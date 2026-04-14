package com.ktome.tools.loot

import com.ktome.tools.phase4.Phase4OwnerBaselineRegistry
import com.ktome.tools.verification.EvaluationEntryStatus
import com.ktome.tools.verification.EvaluationResult
import com.ktome.tools.verification.VerificationBaseline
import com.ktome.tools.verification.EvaluationVerdict
import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal const val SAME_ZONE_SECRET_CADENCE_MAX_OVERLAP_TARGET: Double = 0.50
internal const val SAME_ZONE_SECRET_REWARD_MAX_OVERLAP_TARGET: Double = 0.50
internal const val SECRET_VS_CADENCE_PAIR_TYPE: String = "secret_vs_cadence"
internal const val SECRET_VS_REWARD_PAIR_TYPE: String = "secret_vs_reward"
private const val STRICT_PAIR_CEILINGS_METADATA_KEY: String = "strictPairCeilings"

private val DEFAULT_STRICT_SECRET_PROFILE_MAX_OVERLAP_TARGETS: Map<String, Double> =
    mapOf(
        "loot.abyssal_temple_warded_archive.secret" to 0.35,
        "loot.deep_iron_slag_cache.secret" to 0.40,
        "loot.deep_iron_smuggler_stash.secret" to 0.40,
    )

internal fun loadStrictSecretProfileMaxOverlapTargets(repoRoot: Path = VerificationCacheSupport.repoRoot()): Map<String, Double> =
    strictSecretProfileMaxOverlapTargets(
        VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.lootBaselinePath())),
    )

internal fun strictSecretProfileMaxOverlapTargets(baseline: VerificationBaseline): Map<String, Double> {
    val configured =
        baseline.metadata[STRICT_PAIR_CEILINGS_METADATA_KEY]
            ?.jsonArray
            ?.associate { entry ->
                val payload = entry.jsonObject
                payload.getValue("secretProfileId").jsonPrimitive.content to payload.getValue("maxValue").jsonPrimitive.content.toDouble()
            }.orEmpty()
    return if (configured.isEmpty()) DEFAULT_STRICT_SECRET_PROFILE_MAX_OVERLAP_TARGETS else configured.toSortedMap()
}

internal fun strictSecretProfileMaxOverlapTarget(
    secretProfileId: String,
    strictPairCeilings: Map<String, Double> = DEFAULT_STRICT_SECRET_PROFILE_MAX_OVERLAP_TARGETS,
): Double? = strictPairCeilings[secretProfileId]

internal data class LootStrictLocalIdentityViolation(
    val zoneId: String,
    val pairType: String,
    val secretProfileId: String,
    val comparedProfileId: String,
    val overlap: Double,
    val allowedMaxOverlap: Double,
) {
    val pairId: String
        get() = "$zoneId:$secretProfileId->$comparedProfileId"

    fun toJson(): JsonObject =
        buildJsonObject {
            put("pairId", pairId)
            put("zoneId", zoneId)
            put("pairType", pairType)
            put("secretProfileId", secretProfileId)
            put("comparedProfileId", comparedProfileId)
            put("overlap", overlap)
            put("allowedMaxOverlap", allowedMaxOverlap)
        }
}

internal fun exceedsGlobalLocalIdentityGuardrail(
    pairType: String?,
    overlap: Double,
): Boolean =
    when (pairType) {
        SECRET_VS_CADENCE_PAIR_TYPE -> overlap > SAME_ZONE_SECRET_CADENCE_MAX_OVERLAP_TARGET
        SECRET_VS_REWARD_PAIR_TYPE -> overlap > SAME_ZONE_SECRET_REWARD_MAX_OVERLAP_TARGET
        else -> false
    }

internal fun LootLocalOverlapPairSummary.exceedsGlobalLocalIdentityGuardrail(): Boolean =
    exceedsGlobalLocalIdentityGuardrail(pairType = pairType, overlap = overlap)

internal fun LootLocalOverlapPairSummary.toStrictLocalIdentityViolationOrNull(
    strictPairCeilings: Map<String, Double>,
): LootStrictLocalIdentityViolation? {
    val allowedMaxOverlap = strictPairCeilings[secretProfileId] ?: return null
    if (overlap <= allowedMaxOverlap) {
        return null
    }
    return LootStrictLocalIdentityViolation(
        zoneId = zoneId,
        pairType = pairType,
        secretProfileId = secretProfileId,
        comparedProfileId = comparedProfileId,
        overlap = overlap,
        allowedMaxOverlap = allowedMaxOverlap,
    )
}

internal fun JsonObject.toLootStrictLocalIdentityViolation(): LootStrictLocalIdentityViolation =
    LootStrictLocalIdentityViolation(
        zoneId = getValue("zoneId").jsonPrimitive.content,
        pairType = getValue("pairType").jsonPrimitive.content,
        secretProfileId = getValue("secretProfileId").jsonPrimitive.content,
        comparedProfileId = getValue("comparedProfileId").jsonPrimitive.content,
        overlap = getValue("overlap").jsonPrimitive.content.toDouble(),
        allowedMaxOverlap = getValue("allowedMaxOverlap").jsonPrimitive.content.toDouble(),
    )

internal fun LootProfileOverlapSummary.withStrictPairCeilings(
    strictPairCeilings: Map<String, Double>,
): LootProfileOverlapSummary {
    val violations =
        (sameZoneSecretVsCadencePairs + sameZoneSecretVsRewardPairs)
            .mapNotNull { pair -> pair.toStrictLocalIdentityViolationOrNull(strictPairCeilings) }
            .distinctBy(LootStrictLocalIdentityViolation::pairId)
            .sortedBy(LootStrictLocalIdentityViolation::pairId)
    val violationsBySecretProfileId = violations.groupBy(LootStrictLocalIdentityViolation::secretProfileId)
    return copy(
        strictLocalIdentityViolations = violations,
        secretProfileIdentitySummaries =
            secretProfileIdentitySummaries.map { summary ->
                summary.copy(
                    strictAllowedMaxOverlap = strictSecretProfileMaxOverlapTarget(summary.profileId, strictPairCeilings),
                    strictViolationPairIds =
                        violationsBySecretProfileId[summary.profileId]
                            .orEmpty()
                            .map(LootStrictLocalIdentityViolation::pairId)
                            .sorted(),
                )
            },
    )
}

internal fun EvaluationResult.withStrictLocalIdentityViolations(
    cadenceMetricId: String,
    rewardMetricId: String,
    violations: Iterable<LootStrictLocalIdentityViolation>,
): EvaluationResult {
    val violationList = violations.toList()
    val cadenceViolations = violationList.filter { violation -> violation.pairType == SECRET_VS_CADENCE_PAIR_TYPE }
    val rewardViolations = violationList.filter { violation -> violation.pairType == SECRET_VS_REWARD_PAIR_TYPE }
    val updatedEntries =
        entries.map { entry ->
            when {
                entry.metricId == cadenceMetricId && cadenceViolations.isNotEmpty() ->
                    entry.copy(
                        status = EvaluationEntryStatus.UNEXPECTED_REGRESSION,
                        note =
                            "strictViolations=${cadenceViolations.joinToString(separator = ", ") { violation -> formatStrictLocalIdentityViolation(violation) }}",
                    )

                entry.metricId == rewardMetricId && rewardViolations.isNotEmpty() ->
                    entry.copy(
                        status = EvaluationEntryStatus.UNEXPECTED_REGRESSION,
                        note =
                            "strictViolations=${rewardViolations.joinToString(separator = ", ") { violation -> formatStrictLocalIdentityViolation(violation) }}",
                    )

                else -> entry
            }
        }
    val unexpectedRegressionCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }
    return copy(
        verdict = if (unexpectedRegressionCount > 0) EvaluationVerdict.FAIL else EvaluationVerdict.PASS,
        passCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.PASS },
        approvedDebtCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.APPROVED_DEBT },
        expectedFailureCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.EXPECTED_FAILURE },
        unexpectedRegressionCount = unexpectedRegressionCount,
        improvedDebtCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.IMPROVEMENT },
        entries = updatedEntries,
    )
}

internal fun formatStrictAwareLocalIdentityCurrentValue(
    overlap: Double,
    violations: List<LootStrictLocalIdentityViolation>,
): String {
    val baseValue = formatLocalIdentityRatio(overlap)
    if (violations.isEmpty()) {
        return baseValue
    }
    val firstViolation = violations.first()
    val detail =
        "${firstViolation.secretProfileId}=${formatLocalIdentityRatio(firstViolation.overlap)} > " +
            formatLocalIdentityRatio(firstViolation.allowedMaxOverlap)
    return if (violations.size == 1) {
        "$baseValue (strict: $detail)"
    } else {
        "$baseValue (strict: $detail, +${violations.size - 1} more)"
    }
}

private fun formatStrictLocalIdentityViolation(violation: LootStrictLocalIdentityViolation): String =
    "${violation.pairId}=${formatLocalIdentityRatio(violation.overlap)}>${formatLocalIdentityRatio(violation.allowedMaxOverlap)}"

internal fun formatLocalIdentityRatio(value: Double): String = String.format(java.util.Locale.US, "%.3f", value)
