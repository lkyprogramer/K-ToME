package com.ktome.tools.loot

import com.ktome.game.data.schema.LootProfileLocalIdentityCategory
import com.ktome.tools.phase4.Phase4OwnerBaselineRegistry
import com.ktome.tools.phase4.Phase4OwnerMetricTargets
import com.ktome.tools.phase4.requiredMetric
import com.ktome.tools.verification.EvaluationEntryStatus
import com.ktome.tools.verification.EvaluationResult
import com.ktome.tools.verification.VerificationBaseline
import com.ktome.tools.verification.VerificationBaselineComparator
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

internal data class LootStrictLocalIdentityViolationBreakdown(
    val cadenceViolations: List<LootStrictLocalIdentityViolation>,
    val rewardViolations: List<LootStrictLocalIdentityViolation>,
)

internal data class LocalRewardIdentityMetricEvaluationInput(
    val metricId: String,
    val overlap: Double,
    val currentValueElement: JsonObject,
    val pairCount: Int,
    val failurePairCount: Int? = null,
    val includeOverlapFormula: Boolean = false,
    val preflightCulpritCount: Int? = null,
)

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

internal fun Iterable<LootStrictLocalIdentityViolation>.splitByLocalIdentityPairType(): LootStrictLocalIdentityViolationBreakdown {
    val cadenceViolations = mutableListOf<LootStrictLocalIdentityViolation>()
    val rewardViolations = mutableListOf<LootStrictLocalIdentityViolation>()
    for (violation in this) {
        when (violation.pairType) {
            SECRET_VS_CADENCE_PAIR_TYPE -> cadenceViolations += violation
            SECRET_VS_REWARD_PAIR_TYPE -> rewardViolations += violation
        }
    }
    return LootStrictLocalIdentityViolationBreakdown(
        cadenceViolations = cadenceViolations,
        rewardViolations = rewardViolations,
    )
}

internal fun localIdentityPairTypeForComparedCategory(comparedCategory: LootProfileLocalIdentityCategory): String =
    when (comparedCategory) {
        LootProfileLocalIdentityCategory.CADENCE -> SECRET_VS_CADENCE_PAIR_TYPE
        LootProfileLocalIdentityCategory.REWARD -> SECRET_VS_REWARD_PAIR_TYPE
        else -> error("Unsupported local identity compared category '${comparedCategory.token}'.")
    }

internal fun localIdentityPairType(
    leftCategory: LootProfileLocalIdentityCategory,
    rightCategory: LootProfileLocalIdentityCategory,
): String? =
    when {
        leftCategory == LootProfileLocalIdentityCategory.SECRET && rightCategory == LootProfileLocalIdentityCategory.CADENCE -> SECRET_VS_CADENCE_PAIR_TYPE
        rightCategory == LootProfileLocalIdentityCategory.SECRET && leftCategory == LootProfileLocalIdentityCategory.CADENCE -> SECRET_VS_CADENCE_PAIR_TYPE
        leftCategory == LootProfileLocalIdentityCategory.SECRET && rightCategory == LootProfileLocalIdentityCategory.REWARD -> SECRET_VS_REWARD_PAIR_TYPE
        rightCategory == LootProfileLocalIdentityCategory.SECRET && leftCategory == LootProfileLocalIdentityCategory.REWARD -> SECRET_VS_REWARD_PAIR_TYPE
        else -> null
    }

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
    val violationBreakdown = violations.splitByLocalIdentityPairType()
    val cadenceViolations = violationBreakdown.cadenceViolations
    val rewardViolations = violationBreakdown.rewardViolations
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

internal fun buildLocalRewardIdentityEvaluation(
    baseline: VerificationBaseline,
    strictViolations: List<LootStrictLocalIdentityViolation>,
    cadenceInput: LocalRewardIdentityMetricEvaluationInput,
    rewardInput: LocalRewardIdentityMetricEvaluationInput,
    detailsByMetricId: Map<String, JsonObject>,
): EvaluationResult {
    val cadenceRange = baseline.requiredMetric(cadenceInput.metricId)
    val rewardRange = baseline.requiredMetric(rewardInput.metricId)
    val strictViolationBreakdown = strictViolations.splitByLocalIdentityPairType()
    val strictViolationsByMetricId =
        mapOf(
            cadenceInput.metricId to strictViolationBreakdown.cadenceViolations,
            rewardInput.metricId to strictViolationBreakdown.rewardViolations,
        )
    val result =
        VerificationBaselineComparator.compareBudgetThreshold(
            domainId = "loot",
            evaluationId = "loot.localRewardIdentity",
            baseline = baseline.copy(expectedMetricRanges = listOf(cadenceRange, rewardRange)),
            actualMetrics =
                mapOf(
                    cadenceInput.metricId to cadenceInput.overlap,
                    rewardInput.metricId to rewardInput.overlap,
                ),
            currentValueTexts =
                mapOf(
                    cadenceInput.metricId to
                        formatStrictAwareLocalIdentityCurrentValue(
                            cadenceInput.overlap,
                            strictViolationsByMetricId.getValue(cadenceInput.metricId),
                        ),
                    rewardInput.metricId to
                        formatStrictAwareLocalIdentityCurrentValue(
                            rewardInput.overlap,
                            strictViolationsByMetricId.getValue(rewardInput.metricId),
                        ),
                ),
            currentValueElements =
                mapOf(
                    cadenceInput.metricId to cadenceInput.currentValueElement,
                    rewardInput.metricId to rewardInput.currentValueElement,
                ),
            detailsByMetricId = detailsByMetricId,
        )
    val strictAwareResult =
        result.withStrictLocalIdentityViolations(
            cadenceMetricId = cadenceInput.metricId,
            rewardMetricId = rewardInput.metricId,
            violations = strictViolations,
        )
    return strictAwareResult.copy(
        entries =
            strictAwareResult.entries.map { entry ->
                when (entry.metricId) {
                    cadenceInput.metricId ->
                        entry.copy(
                            targetText = Phase4OwnerMetricTargets.targetText(entry.metricId, cadenceRange),
                            note =
                                appendEvaluationNote(
                                    existingNote = entry.note,
                                    detail =
                                        localIdentityEvaluationDetail(
                                            pairCount = cadenceInput.pairCount,
                                            strictViolationCount = strictViolationsByMetricId.getValue(cadenceInput.metricId).size,
                                            includeOverlapFormula = cadenceInput.includeOverlapFormula,
                                            preflightCulpritCount = cadenceInput.preflightCulpritCount,
                                        ),
                                ),
                        )

                    rewardInput.metricId ->
                        entry.copy(
                            targetText = Phase4OwnerMetricTargets.targetText(entry.metricId, rewardRange),
                            note =
                                appendEvaluationNote(
                                    existingNote = entry.note,
                                    detail =
                                        localIdentityEvaluationDetail(
                                            pairCount = rewardInput.pairCount,
                                            failurePairCount = rewardInput.failurePairCount,
                                            strictViolationCount = strictViolationsByMetricId.getValue(rewardInput.metricId).size,
                                            includeOverlapFormula = rewardInput.includeOverlapFormula,
                                            preflightCulpritCount = rewardInput.preflightCulpritCount,
                                        ),
                                ),
                        )

                    else -> entry
                }
            },
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

internal fun localIdentityEvaluationDetail(
    pairCount: Int,
    strictViolationCount: Int,
    failurePairCount: Int? = null,
    includeOverlapFormula: Boolean = false,
    preflightCulpritCount: Int? = null,
): String {
    val counters =
        buildList {
            add("pairCount=$pairCount")
            failurePairCount?.let { count -> add("failurePairs=$count") }
            add("strictViolations=$strictViolationCount")
        }
    val extras =
        buildList {
            if (includeOverlapFormula) {
                add("overlap = |A ∩ B| / min(|A|, |B|)")
            }
            preflightCulpritCount?.let { count -> add("preflightCulprits=$count") }
        }
    return listOf(counters.joinToString(", "), extras.takeIf { it.isNotEmpty() }?.joinToString("; "))
        .filterNotNull()
        .joinToString("; ")
}

internal fun appendEvaluationNote(
    existingNote: String?,
    detail: String,
): String = listOfNotNull(existingNote, detail.takeIf(String::isNotBlank)).joinToString(separator = "; ")

private fun formatStrictLocalIdentityViolation(violation: LootStrictLocalIdentityViolation): String =
    "${violation.pairId}=${formatLocalIdentityRatio(violation.overlap)}>${formatLocalIdentityRatio(violation.allowedMaxOverlap)}"

internal fun formatLocalIdentityRatio(value: Double): String = String.format(java.util.Locale.US, "%.3f", value)
