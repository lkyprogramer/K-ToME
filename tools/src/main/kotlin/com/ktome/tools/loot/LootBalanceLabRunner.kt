package com.ktome.tools.loot

import com.ktome.core.harness.toJson
import com.ktome.core.item.AffixDef
import com.ktome.core.item.AffixEquipType
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.GeneratedItemRoll
import com.ktome.core.item.ItemGenerator
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemType
import com.ktome.core.item.StatModifier
import com.ktome.core.loot.LootRollContext
import com.ktome.core.loot.RarityTier
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier
import com.ktome.core.loot.SpecialTierEligibility
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.phase.Phase4ContractVersions
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.LootProfileLocalIdentityCategory
import com.ktome.game.data.schema.LootProfileSchemaV3
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.hidden.HiddenEventRewardPayload
import com.ktome.game.i18n.GameLocale
import com.ktome.game.loot.LootProfileCandidatePoolResolver
import com.ktome.game.mapgen.SchemaZoneRewardProfileResolver
import com.ktome.tools.mapgen.phase4HarnessHeader
import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class LootBalanceLabRun(
    val matrixCount: Int,
    val totalRolls: Int,
    val failedExpectationCount: Int,
    val summaryPath: Path,
    val rollsPath: Path,
)

internal data class LootMatrixSpec(
    val id: String,
    val zoneId: String,
    val sourceLevel: Int,
    val sourceTier: SourceTier,
    val sourceDescriptor: String = sourceTier.name,
    val playerLevel: Int,
    val magicFind: Float,
    val specialTierOverride: Set<SpecialTier>? = null,
    val rollCount: Int = ROLLS_PER_MATRIX,
    val seedBase: Long,
) {
    fun allowsArtifact(): Boolean =
        specialTierOverride?.contains(SpecialTier.ARTIFACT) ?: (sourceTier == SourceTier.BOSS || sourceTier == SourceTier.SECRET_ZONE)
}

internal data class LootRollSample(
    val matrixId: String,
    val rollIndex: Int,
    val seed: Long,
    val sourceLevel: Int,
    val sourceTier: SourceTier,
    val zoneId: String,
    val playerLevel: Int,
    val magicFind: Float,
    val effectiveMagicFind: Float,
    val rolledRarityTier: RarityTier,
    val resolvedRarityTier: RarityTier,
    val finalTier: String,
    val specialTier: SpecialTier?,
    val specialTemplateId: String?,
    val generatedBaseItemId: String,
    val itemType: ItemType,
    val equipSlot: EquipSlot?,
    val specialOrBasePassiveSignatures: Set<String>,
    val affixPassiveSignatures: Set<String>,
    val meaningfulUniqueArtifactSwap: Boolean?,
    val specialTierEligibilityCount: Int,
    val rarePityApplied: Boolean,
    val specialPityApplied: Boolean,
    val affixBudget: Int,
    val affixBudgetTarget: Int,
    val affixBudgetConsumed: Int,
    val affixBudgetDeviationRatio: Double?,
    val rawAffixBudgetShortfall: Int,
    val rawCastSpeedRating: Int,
    val effectiveCastSpeed: Double,
    val affixCostBreakdown: List<Pair<String, Int>>,
    val previousRarePity: Int,
    val previousSpecialPity: Int,
    val resultingRarePity: Int,
    val resultingSpecialPity: Int,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("matrixId", matrixId)
            put("rollIndex", rollIndex)
            put("seed", seed)
            put("sourceLevel", sourceLevel)
            put("sourceTier", sourceTier.name)
            put("zoneId", zoneId)
            put("playerLevel", playerLevel)
            put("magicFind", magicFind)
            put("effectiveMagicFind", effectiveMagicFind)
            put("rolledRarityTier", rolledRarityTier.name)
            put("resolvedRarityTier", resolvedRarityTier.name)
            put("finalTier", finalTier)
            put("specialTier", specialTier?.name)
            put("specialTemplateId", specialTemplateId)
            put("generatedBaseItemId", generatedBaseItemId)
            put("itemType", itemType.name)
            put("equipSlot", equipSlot?.name)
            putJsonArray("specialOrBasePassiveSignatures") {
                specialOrBasePassiveSignatures.sorted().forEach { signature -> add(JsonPrimitive(signature)) }
            }
            putJsonArray("affixPassiveSignatures") {
                affixPassiveSignatures.sorted().forEach { signature -> add(JsonPrimitive(signature)) }
            }
            put("meaningfulUniqueArtifactSwap", meaningfulUniqueArtifactSwap)
            put("specialTierEligibilityCount", specialTierEligibilityCount)
            put("rarePityApplied", rarePityApplied)
            put("specialPityApplied", specialPityApplied)
            put("affixBudget", affixBudget)
            put("affixBudgetTarget", affixBudgetTarget)
            put("affixBudgetConsumed", affixBudgetConsumed)
            put("affixBudgetDeviationRatio", affixBudgetDeviationRatio)
            put("rawAffixBudgetShortfall", rawAffixBudgetShortfall)
            put("rawCastSpeedRating", rawCastSpeedRating)
            put("effectiveCastSpeed", effectiveCastSpeed)
            putJsonArray("affixCostBreakdown") {
                affixCostBreakdown.forEach { (affixId, cost) ->
                    add(
                        buildJsonObject {
                            put("affixId", affixId)
                            put("cost", cost)
                        },
                    )
                }
            }
            putJsonObject("pityBefore") {
                put("rollsSinceLastRare", previousRarePity)
                put("eligibleSpecialRollsSinceLastUnique", previousSpecialPity)
            }
            putJsonObject("pityAfter") {
                put("rollsSinceLastRare", resultingRarePity)
                put("eligibleSpecialRollsSinceLastUnique", resultingSpecialPity)
            }
        }
}

internal data class LootPityEvent(
    val rollIndex: Int,
    val kind: String,
    val finalTier: String,
    val specialTemplateId: String?,
    val beforeValue: Int,
    val afterValue: Int,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("rollIndex", rollIndex)
            put("kind", kind)
            put("finalTier", finalTier)
            put("specialTemplateId", specialTemplateId)
            put("beforeValue", beforeValue)
            put("afterValue", afterValue)
        }
}

internal data class LootCastSpeedSample(
    val rollIndex: Int,
    val rawCastSpeedRating: Int,
    val effectiveCastSpeed: Double,
    val finalTier: String,
    val specialTemplateId: String?,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("rollIndex", rollIndex)
            put("rawCastSpeedRating", rawCastSpeedRating)
            put("effectiveCastSpeed", effectiveCastSpeed)
            put("finalTier", finalTier)
            put("specialTemplateId", specialTemplateId)
        }
}

internal data class LootExpectation(
    val finalRates: Map<String, Double>,
    val magicRate: Double,
    val rareRate: Double,
    val uniqueRate: Double,
    val artifactRate: Double,
    val noUpgradeRate: Double,
)

internal data class LootMatrixResult(
    val spec: LootMatrixSpec,
    val totalRolls: Int,
    val availableSpecialTiers: Set<SpecialTier>,
    val rarityTierDistribution: Map<String, Double>,
    val baseRarityDistribution: Map<String, Double>,
    val expectedDistribution: LootExpectation,
    val affixBudgetAverageDeviation: Double,
    val affixBudgetP95Deviation: Double,
    val uniqueRate: Double,
    val artifactRate: Double,
    val specialTierEligibilityRate: Double,
    val rarePityActivations: Int,
    val uniquePityActivations: Int,
    val castSpeedPostDrP50: Double,
    val castSpeedPostDrP95: Double,
    val magicRateDrift: Double,
    val rareRateDrift: Double,
    val uniqueRelativeError: Double,
    val artifactRelativeError: Double,
    val failedExpectationCount: Int,
    val affixCostHistogram: Map<String, Int>,
    val topAffixIds: Map<String, Int>,
    val pityTimeline: List<LootPityEvent>,
    val castSpeedSamples: List<LootCastSpeedSample>,
    val sampleRolls: List<LootRollSample>,
    val uniqueArtifactOutcomeCount: Int,
    val meaningfulUniqueArtifactSwapCount: Int,
    val meaningfulUniqueArtifactSwapRate: Double,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("matrixId", spec.id)
            put("seedBase", spec.seedBase)
            put("sourceLevel", spec.sourceLevel)
            put("sourceTier", spec.sourceTier.name)
            put("sourceDescriptor", spec.sourceDescriptor)
            put("zoneId", spec.zoneId)
            put("playerLevel", spec.playerLevel)
            put("magicFind", spec.magicFind)
            put("effectiveMagicFind", spec.magicFind.coerceIn(0.0f, 1.0f))
            put("rollCount", totalRolls)
            putJsonArray("availableSpecialTiers") {
                availableSpecialTiers.map(SpecialTier::name).sorted().forEach { tier -> add(JsonPrimitive(tier)) }
            }
            putJsonObject("rarityTierDistribution") {
                rarityTierDistribution.forEach { (tier, rate) -> put(tier, rate) }
            }
            putJsonObject("baseRarityDistribution") {
                baseRarityDistribution.forEach { (tier, rate) -> put(tier, rate) }
            }
            putJsonObject("expectedDistribution") {
                expectedDistribution.finalRates.forEach { (tier, rate) -> put(tier, rate) }
                put("noUpgradeRate", expectedDistribution.noUpgradeRate)
            }
            putJsonObject("affixBudgetDeviation") {
                put("average", affixBudgetAverageDeviation)
                put("p95", affixBudgetP95Deviation)
            }
            put("uniqueRate", uniqueRate)
            put("artifactRate", artifactRate)
            put("specialTierEligibilityRate", specialTierEligibilityRate)
            put("rarePityActivations", rarePityActivations)
            put("uniquePityActivations", uniquePityActivations)
            put("castSpeedPostDrP50", castSpeedPostDrP50)
            put("castSpeedPostDrP95", castSpeedPostDrP95)
            putJsonObject("distributionError") {
                put("magicRateDrift", magicRateDrift)
                put("rareRateDrift", rareRateDrift)
                put("uniqueRelativeError", uniqueRelativeError)
                put("artifactRelativeError", artifactRelativeError)
            }
            put("failedExpectationCount", failedExpectationCount)
            putJsonObject("affixCostHistogram") {
                affixCostHistogram.forEach { (band, count) -> put(band, count) }
            }
            putJsonObject("topAffixIds") {
                topAffixIds.forEach { (affixId, count) -> put(affixId, count) }
            }
            putJsonArray("pityTimeline") {
                pityTimeline.forEach { event -> add(event.toJson()) }
            }
            putJsonArray("castSpeedSamples") {
                castSpeedSamples.forEach { sample -> add(sample.toJson()) }
            }
            putJsonArray("sampleRolls") {
                sampleRolls.forEach { sample -> add(sample.toJson()) }
            }
            put("uniqueArtifactOutcomeCount", uniqueArtifactOutcomeCount)
            put("meaningfulUniqueArtifactSwapCount", meaningfulUniqueArtifactSwapCount)
            put("meaningfulUniqueArtifactSwapRate", meaningfulUniqueArtifactSwapRate)
        }
}

internal data class LootProfileOverlapSummary(
    val overlapMatrix: Map<String, Map<String, Double>>,
    val averageOverlap: Double,
    val maxOverlap: Double,
    val distinctBaseItemCount: Int,
    val sameZoneSecretVsCadencePairs: List<LootLocalOverlapPairSummary>,
    val sameZoneSecretVsRewardPairs: List<LootLocalOverlapPairSummary>,
    val localIdentityFailurePairs: List<String>,
    val strictLocalIdentityViolations: List<LootStrictLocalIdentityViolation>,
    val secretProfileIdentitySummaries: List<LootSecretProfileIdentitySummary>,
) {
    val sameZoneSecretVsCadenceMaxOverlap: Double
        get() = sameZoneSecretVsCadencePairs.maxOfOrNull(LootLocalOverlapPairSummary::overlap) ?: 0.0

    val sameZoneSecretVsRewardMaxOverlap: Double
        get() = sameZoneSecretVsRewardPairs.maxOfOrNull(LootLocalOverlapPairSummary::overlap) ?: 0.0

    fun toJson(): JsonObject =
        buildJsonObject {
            put("averageOverlap", averageOverlap)
            put("maxOverlap", maxOverlap)
            put("distinctBaseItemCount", distinctBaseItemCount)
            put("sameZoneSecretVsCadenceMaxOverlap", sameZoneSecretVsCadenceMaxOverlap)
            put("sameZoneSecretVsRewardMaxOverlap", sameZoneSecretVsRewardMaxOverlap)
            putJsonObject("matrix") {
                overlapMatrix.toSortedMap().forEach { (profileId, row) ->
                    putJsonObject(profileId) {
                        row.toSortedMap().forEach { (candidateId, overlap) ->
                            put(candidateId, overlap)
                        }
                    }
                }
            }
            putJsonArray("sameZoneSecretVsCadencePairs") {
                sameZoneSecretVsCadencePairs.sortedBy(LootLocalOverlapPairSummary::pairId).forEach { pair ->
                    add(pair.toJson())
                }
            }
            putJsonArray("sameZoneSecretVsRewardPairs") {
                sameZoneSecretVsRewardPairs.sortedBy(LootLocalOverlapPairSummary::pairId).forEach { pair ->
                    add(pair.toJson())
                }
            }
            putJsonArray("localIdentityFailurePairs") {
                localIdentityFailurePairs.sorted().forEach { pairId -> add(JsonPrimitive(pairId)) }
            }
            putJsonArray("strictLocalIdentityViolations") {
                strictLocalIdentityViolations.sortedBy(LootStrictLocalIdentityViolation::pairId).forEach { violation ->
                    add(violation.toJson())
                }
            }
            putJsonArray("secretProfileIdentitySummaries") {
                secretProfileIdentitySummaries.sortedBy(LootSecretProfileIdentitySummary::profileId).forEach { summary ->
                    add(summary.toJson())
                }
            }
        }
}

internal data class LootLocalOverlapPairSummary(
    val zoneId: String,
    val pairType: String,
    val secretProfileId: String,
    val comparedProfileId: String,
    val overlap: Double,
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
        }
}

/**
 * Canonical owner-artifact shape for the Phase 4 local reward identity report.
 * The long-term authority doc refers to this surface as `SecretRewardIdentitySummary`.
 * `poolStrategy` is retained as explanatory debug metadata and is not a separate metric contract.
 */
internal data class LootSecretProfileIdentitySummary(
    val profileId: String,
    val canonicalZoneId: String,
    val poolStrategy: String,
    val identityAxes: List<String>,
    val rewardStructureKeys: List<String>,
    val fixedItemIds: List<String>,
    val candidateBaseIds: List<String>,
    val typeWeights: Map<String, Int>,
    val slotBias: Map<String, Int>,
    val specialTemplateTagPreference: List<String>,
    val affixTagPreference: List<String>,
    val sameZoneCadenceMaxOverlap: Double?,
    val sameZoneRewardMaxOverlap: Double?,
    val strictAllowedMaxOverlap: Double?,
    val strictViolationPairIds: List<String>,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("profileId", profileId)
            put("canonicalZoneId", canonicalZoneId)
            put("poolStrategy", poolStrategy)
            putJsonArray("identityAxes") {
                identityAxes.forEach { axis -> add(JsonPrimitive(axis)) }
            }
            putJsonArray("rewardStructureKeys") {
                rewardStructureKeys.forEach { rewardKey -> add(JsonPrimitive(rewardKey)) }
            }
            putJsonArray("fixedItemIds") {
                fixedItemIds.forEach { itemId -> add(JsonPrimitive(itemId)) }
            }
            putJsonArray("candidateBaseIds") {
                candidateBaseIds.forEach { baseId -> add(JsonPrimitive(baseId)) }
            }
            putJsonObject("typeWeights") {
                typeWeights.toSortedMap().forEach { (typeId, weight) -> put(typeId, weight) }
            }
            putJsonObject("slotBias") {
                slotBias.toSortedMap().forEach { (slotId, weight) -> put(slotId, weight) }
            }
            putJsonArray("specialTemplateTagPreference") {
                specialTemplateTagPreference.forEach { tag -> add(JsonPrimitive(tag)) }
            }
            putJsonArray("affixTagPreference") {
                affixTagPreference.forEach { tag -> add(JsonPrimitive(tag)) }
            }
            put("sameZoneCadenceMaxOverlap", sameZoneCadenceMaxOverlap)
            put("sameZoneRewardMaxOverlap", sameZoneRewardMaxOverlap)
            put("strictAllowedMaxOverlap", strictAllowedMaxOverlap)
            putJsonArray("strictViolationPairIds") {
                strictViolationPairIds.forEach { pairId -> add(JsonPrimitive(pairId)) }
            }
        }
}

internal data class LootPassiveCoverageSummary(
    val passiveKinds: Set<String>,
    val coverageRatio: Double,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("coverageRatio", coverageRatio)
            putJsonArray("passiveKinds") {
                passiveKinds.sorted().forEach { passiveKind -> add(JsonPrimitive(passiveKind)) }
            }
        }
}

internal data class LootSpecialPoolSummary(
    val affixCount: Int,
    val uniqueTemplateCount: Int,
    val artifactTemplateCount: Int,
    val totalCount: Int,
    val targetZoneCoverageCount: Int,
    val buildArchetypeCount: Int,
    val bossOnlyArtifactTemplateCount: Int,
    val chestOnlyArtifactTemplateCount: Int,
    val secretZoneArtifactTemplateCount: Int,
    val passesThresholds: Boolean,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("affixCount", affixCount)
            put("uniqueTemplateCount", uniqueTemplateCount)
            put("artifactTemplateCount", artifactTemplateCount)
            put("totalCount", totalCount)
            put("targetZoneCoverageCount", targetZoneCoverageCount)
            put("buildArchetypeCount", buildArchetypeCount)
            put("bossOnlyArtifactTemplateCount", bossOnlyArtifactTemplateCount)
            put("chestOnlyArtifactTemplateCount", chestOnlyArtifactTemplateCount)
            put("secretZoneArtifactTemplateCount", secretZoneArtifactTemplateCount)
            put("passesThresholds", passesThresholds)
        }
}

internal data class LootClampComparison(
    val referenceMatrixId: String,
    val overflowMatrixId: String,
    val tierDelta: Map<String, Double>,
    val maxDistributionDelta: Double,
    val withinTolerance: Boolean,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("referenceMatrixId", referenceMatrixId)
            put("overflowMatrixId", overflowMatrixId)
            put("maxDistributionDelta", maxDistributionDelta)
            put("withinTolerance", withinTolerance)
            putJsonObject("tierDelta") {
                tierDelta.forEach { (tier, delta) -> put(tier, delta) }
            }
        }
}

internal data class LootKernelRun(
    val matrices: List<LootMatrixResult>,
    val specialPoolSummary: LootSpecialPoolSummary,
    val clampComparison: LootClampComparison,
    val matrixSeeds: List<Long>,
    val profileOverlapSummary: LootProfileOverlapSummary,
    val passiveCoverageSummary: LootPassiveCoverageSummary,
) {
    val totalRolls: Int
        get() = matrices.sumOf(LootMatrixResult::totalRolls)

    val failedExpectationCount: Int
        get() = matrices.sumOf(LootMatrixResult::failedExpectationCount) + if (specialPoolSummary.passesThresholds && clampComparison.withinTolerance) 0 else 1

    val uniqueArtifactOutcomeCount: Int
        get() = matrices.sumOf(LootMatrixResult::uniqueArtifactOutcomeCount)

    val meaningfulUniqueArtifactSwapCount: Int
        get() = matrices.sumOf(LootMatrixResult::meaningfulUniqueArtifactSwapCount)

    val uniqueArtifactMeaningfulSwapRate: Double
        get() =
            if (uniqueArtifactOutcomeCount == 0) {
                0.0
            } else {
                meaningfulUniqueArtifactSwapCount.toDouble() / uniqueArtifactOutcomeCount.toDouble()
            }
}

private data class LootMatrixShardSpec(
    val matrixId: String,
    val rollStartInclusive: Int,
    val rollCount: Int,
) {
    val shardId: String
        get() = "$matrixId:${rollStartInclusive}-${rollStartInclusive + rollCount - 1}"
}

private data class LootMatrixKernelShard(
    val shardId: String,
    val matrixId: String,
    val rollStartInclusive: Int,
    val rollCount: Int,
    val startingPityTracker: com.ktome.core.loot.PityTracker,
    val resultingPityTracker: com.ktome.core.loot.PityTracker,
    val availableSpecialTiers: Set<SpecialTier>,
    val finalTierCounts: Map<String, Int>,
    val baseRarityCounts: Map<String, Int>,
    val affixDeviationRatios: List<Double>,
    val castSpeedValues: List<Double>,
    val affixCostHistogram: Map<String, Int>,
    val affixIdCounts: Map<String, Int>,
    val pityTimeline: List<LootPityEvent>,
    val castSpeedSamples: List<LootCastSpeedSample>,
    val sampleRolls: List<LootRollSample>,
    val eligibleCount: Int,
    val uniqueCount: Int,
    val artifactCount: Int,
    val uniqueArtifactOutcomeCount: Int,
    val meaningfulUniqueArtifactSwapCount: Int,
    val rarePityActivations: Int,
    val uniquePityActivations: Int,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("shardId", shardId)
            put("matrixId", matrixId)
            put("rollStartInclusive", rollStartInclusive)
            put("rollCount", rollCount)
            putJsonObject("startingPityTracker") {
                put("rollsSinceLastRare", startingPityTracker.rollsSinceLastRare)
                put("eligibleSpecialRollsSinceLastUnique", startingPityTracker.eligibleSpecialRollsSinceLastUnique)
            }
            putJsonObject("resultingPityTracker") {
                put("rollsSinceLastRare", resultingPityTracker.rollsSinceLastRare)
                put("eligibleSpecialRollsSinceLastUnique", resultingPityTracker.eligibleSpecialRollsSinceLastUnique)
            }
            putJsonArray("availableSpecialTiers") {
                availableSpecialTiers.map(SpecialTier::name).sorted().forEach { tier -> add(JsonPrimitive(tier)) }
            }
            putJsonObject("finalTierCounts") {
                finalTierCounts.toSortedMap().forEach { (tier, count) -> put(tier, count) }
            }
            putJsonObject("baseRarityCounts") {
                baseRarityCounts.toSortedMap().forEach { (tier, count) -> put(tier, count) }
            }
            putJsonArray("affixDeviationRatios") {
                affixDeviationRatios.forEach { ratio -> add(JsonPrimitive(ratio)) }
            }
            putJsonArray("castSpeedValues") {
                castSpeedValues.forEach { value -> add(JsonPrimitive(value)) }
            }
            putJsonObject("affixCostHistogram") {
                affixCostHistogram.toSortedMap().forEach { (cost, count) -> put(cost, count) }
            }
            putJsonObject("affixIdCounts") {
                affixIdCounts.toSortedMap().forEach { (affixId, count) -> put(affixId, count) }
            }
            putJsonArray("pityTimeline") {
                pityTimeline.forEach { event -> add(event.toJson()) }
            }
            putJsonArray("castSpeedSamples") {
                castSpeedSamples.forEach { sample -> add(sample.toJson()) }
            }
            putJsonArray("sampleRolls") {
                sampleRolls.forEach { sample -> add(sample.toJson()) }
            }
            put("eligibleCount", eligibleCount)
            put("uniqueCount", uniqueCount)
            put("artifactCount", artifactCount)
            put("uniqueArtifactOutcomeCount", uniqueArtifactOutcomeCount)
            put("meaningfulUniqueArtifactSwapCount", meaningfulUniqueArtifactSwapCount)
            put("rarePityActivations", rarePityActivations)
            put("uniquePityActivations", uniquePityActivations)
        }
}

private data class LootMatrixShardExecution(
    val shard: LootMatrixKernelShard,
    val rolls: List<LootRollSample>,
)

internal data class LootKernelExecution(
    val kernelRun: LootKernelRun,
    val inputFingerprint: String,
    val cacheStatus: String,
    val reusedShardCount: Int,
    val shardCount: Int,
    val mergedKernelPath: Path,
    val shardRollPaths: List<Path>,
) {
    fun cacheMetadata(repoRoot: Path): JsonObject =
        buildJsonObject {
            put("contractVersion", LOOT_KERNEL_CACHE_VERSION)
            put("inputFingerprint", inputFingerprint)
            put("cacheStatus", cacheStatus)
            put("reusedShardCount", reusedShardCount)
            put("shardCount", shardCount)
            put("kernelReuseSource", VerificationCacheSupport.relativeToRepo(mergedKernelPath, repoRoot))
            putJsonArray("shardRollPaths") {
                shardRollPaths.forEach { shardRollPath ->
                    add(JsonPrimitive(VerificationCacheSupport.relativeToRepo(shardRollPath, repoRoot)))
                }
            }
        }
}

private const val ROLLS_PER_MATRIX: Int = 10_000
private const val ROLLS_PER_SHARD: Int = 1_000
private const val MAGIC_RARE_ABSOLUTE_DRIFT_TOLERANCE: Double = 0.05
private const val SPECIAL_RELATIVE_ERROR_TOLERANCE: Double = 0.25
private const val AFFIX_BUDGET_AVERAGE_TOLERANCE: Double = 0.05
private const val AFFIX_BUDGET_P95_TOLERANCE: Double = 0.12
private const val CLAMP_DISTRIBUTION_TOLERANCE: Double = 0.02
private const val LOOT_KERNEL_CACHE_VERSION: String = "uvr-pr05-loot-kernel-v6"
internal val LOOT_REPORT_LOCALE: GameLocale = GameLocale.EN_US
private const val EQUIPMENT_PASSIVE_KIND_COUNT: Int = 9

internal object LootLabKernel {
    private val json: Json = Json { prettyPrint = true }
    private val compactJson: Json = Json { prettyPrint = false; explicitNulls = false }
    private val matrixSpecs: List<LootMatrixSpec> =
        listOf(
            LootMatrixSpec(
                id = "greenwood_normal_mf000",
                zoneId = "greenwood_fringe",
                sourceLevel = 2,
                sourceTier = SourceTier.NORMAL,
                playerLevel = 2,
                magicFind = 0.00f,
                seedBase = 20260405010000L,
            ),
            LootMatrixSpec(
                id = "deep_iron_elite_mf015",
                zoneId = "deep_iron_pit",
                sourceLevel = 4,
                sourceTier = SourceTier.ELITE,
                playerLevel = 4,
                magicFind = 0.15f,
                seedBase = 20260405020000L,
            ),
            LootMatrixSpec(
                id = "river_boss_mf025",
                zoneId = "underground_river",
                sourceLevel = 6,
                sourceTier = SourceTier.BOSS,
                playerLevel = 6,
                magicFind = 0.25f,
                seedBase = 20260405030000L,
            ),
            LootMatrixSpec(
                id = "abyssal_reward_chest_mf010",
                zoneId = "abyssal_temple",
                sourceLevel = 7,
                sourceTier = SourceTier.CHEST,
                sourceDescriptor = "SPECIAL_REWARD_CHEST",
                playerLevel = 7,
                magicFind = 0.10f,
                specialTierOverride = setOf(SpecialTier.UNIQUE, SpecialTier.ARTIFACT),
                seedBase = 20260405040000L,
            ),
            LootMatrixSpec(
                id = "abyssal_boss_mf100",
                zoneId = "abyssal_temple",
                sourceLevel = 7,
                sourceTier = SourceTier.BOSS,
                playerLevel = 7,
                magicFind = 1.00f,
                seedBase = 20260405050000L,
            ),
            LootMatrixSpec(
                id = "abyssal_boss_mf150",
                zoneId = "abyssal_temple",
                sourceLevel = 7,
                sourceTier = SourceTier.BOSS,
                playerLevel = 7,
                magicFind = 1.50f,
                seedBase = 20260405060000L,
            ),
        )

    fun execute(): LootKernelExecution {
        val repoRoot = VerificationCacheSupport.repoRoot()
        val cacheDirs = VerificationCacheSupport.cacheDirs(domainId = "loot", repoRoot = repoRoot)
        val inputFingerprint = VerificationCacheSupport.sha256Files(lootKernelFingerprintInputs(repoRoot))
        val kernelRoot = VerificationCacheSupport.ensureDirectory(cacheDirs.kernelDir.resolve(inputFingerprint))
        val mergedKernelPath =
            VerificationCacheSupport.ensureDirectory(cacheDirs.kernelDir.resolve("merged"))
                .resolve("loot-kernel-merged.json")
        val loader = DataLoader(LOOT_REPORT_LOCALE)
        val schemaCatalog = loader.loadSchemaCatalog()
        val itemBundle = loader.loadItemBundle()
        val strictPairCeilings = loadStrictSecretProfileMaxOverlapTargets(repoRoot)
        val rewardResolver = SchemaZoneRewardProfileResolver(schemaCatalog.zones, schemaCatalog.zoneRewardProfiles)
        val rarePassiveUniverseBySlot = buildRarePassiveUniverseBySlot(itemBundle)
        var reusedShardCount = 0
        val shardRollPaths = mutableListOf<Path>()
        val matrices =
            matrixSpecs.map { spec ->
                var currentPityTracker = com.ktome.core.loot.PityTracker()
                val shards =
                    matrixShardSpecs(spec).map { shardSpec ->
                        val shardDir = VerificationCacheSupport.ensureDirectory(kernelRoot.resolve(spec.id).resolve(shardSpec.shardId))
                        val shardPayloadPath = shardDir.resolve("kernel.json")
                        val shardRollPath = shardDir.resolve("rolls.jsonl")
                        shardRollPaths.add(shardRollPath)
                        val cachedShard =
                            if (Files.isRegularFile(shardPayloadPath) && Files.isRegularFile(shardRollPath)) {
                                readShardPayload(shardPayloadPath)
                            } else {
                                null
                            }
                        val shard =
                            if (cachedShard != null && cachedShard.startingPityTracker == currentPityTracker) {
                                reusedShardCount += 1
                                cachedShard
                            } else {
                                val shardExecution =
                                    executeMatrixShard(
                                        spec = spec,
                                        shardSpec = shardSpec,
                                        zoneRewardProfile = rewardResolver.resolve(spec.zoneId),
                                        itemBundleLoader = { itemBundle },
                                        rarePassiveUniverseBySlot = rarePassiveUniverseBySlot,
                                        startingPityTracker = currentPityTracker,
                                    )
                                writeShardPayload(
                                    shardPayloadPath = shardPayloadPath,
                                    shardRollPath = shardRollPath,
                                    shard = shardExecution.shard,
                                    rolls = shardExecution.rolls,
                                )
                                shardExecution.shard
                            }
                        currentPityTracker = shard.resultingPityTracker
                        shard
                    }
                mergeMatrixShards(
                    spec = spec,
                    shards = shards,
                    zoneRewardProfile = rewardResolver.resolve(spec.zoneId),
                )
            }
        val specialPoolSummary = summarizeSpecialPool(itemBundleLoader = { itemBundle })
        val clampComparison = compareClampBoundary(matrices)
        val kernelRun =
            LootKernelRun(
                matrices = matrices,
                specialPoolSummary = specialPoolSummary,
                clampComparison = clampComparison,
                matrixSeeds = matrixSpecs.map(LootMatrixSpec::seedBase),
                profileOverlapSummary =
                    summarizeLootProfileOverlap(
                        schemaCatalog = schemaCatalog,
                        profiles = schemaCatalog.lootProfiles,
                        itemBundle = itemBundle,
                        strictPairCeilings = strictPairCeilings,
                    ),
                passiveCoverageSummary = summarizeAffixPassiveCoverage(itemBundle),
            )
        val header = phase4HarnessHeader(harnessId = LootBalanceLabRunner.HARNESS_ID, seedList = kernelRun.matrixSeeds, locale = LOOT_REPORT_LOCALE.id)
        Files.writeString(
            mergedKernelPath,
            VerificationCacheSupport.json.encodeToString(
                JsonElement.serializer(),
                buildMergedKernelPayload(
                    header = header,
                    kernelRun = kernelRun,
                    cacheMetadata =
                        buildJsonObject {
                            put("contractVersion", LOOT_KERNEL_CACHE_VERSION)
                            put("inputFingerprint", inputFingerprint)
                            put("cacheStatus", if (reusedShardCount == shardRollPaths.size) "HIT" else "MISS")
                            put("reusedShardCount", reusedShardCount)
                            put("shardCount", shardRollPaths.size)
                        },
                ),
            ),
        )
        return LootKernelExecution(
            kernelRun = kernelRun,
            inputFingerprint = inputFingerprint,
            cacheStatus = if (reusedShardCount == shardRollPaths.size) "HIT" else "MISS",
            reusedShardCount = reusedShardCount,
            shardCount = shardRollPaths.size,
            mergedKernelPath = mergedKernelPath,
            shardRollPaths = shardRollPaths.sortedBy(Path::toString),
        )
    }

    private fun executeMatrixShard(
        spec: LootMatrixSpec,
        shardSpec: LootMatrixShardSpec,
        zoneRewardProfile: ZoneRewardProfile,
        itemBundleLoader: () -> com.ktome.core.item.ItemDataBundle,
        rarePassiveUniverseBySlot: Map<EquipSlot, Set<String>>,
        startingPityTracker: com.ktome.core.loot.PityTracker,
    ): LootMatrixShardExecution {
        val finalTierCounts = linkedMapOf("NORMAL" to 0, "MAGIC" to 0, "RARE" to 0, "UNIQUE" to 0, "ARTIFACT" to 0)
        val baseRarityCounts = linkedMapOf("NORMAL" to 0, "MAGIC" to 0, "RARE" to 0)
        val affixDeviationRatios = mutableListOf<Double>()
        val castSpeedValues = mutableListOf<Double>()
        val affixCostHistogram = linkedMapOf<String, Int>()
        val affixIdCounts = linkedMapOf<String, Int>()
        val pityTimeline = mutableListOf<LootPityEvent>()
        val castSpeedSamples = mutableListOf<LootCastSpeedSample>()
        val sampleRolls = mutableListOf<LootRollSample>()
        val allRolls = mutableListOf<LootRollSample>()
        var availableSpecialTiers: Set<SpecialTier> = emptySet()
        var eligibleCount = 0
        var uniqueCount = 0
        var artifactCount = 0
        var uniqueArtifactOutcomeCount = 0
        var meaningfulUniqueArtifactSwapCount = 0
        var rarePityActivations = 0
        var uniquePityActivations = 0
        var pityTracker = startingPityTracker

        repeat(shardSpec.rollCount) { shardIndex ->
            val rollIndex = shardSpec.rollStartInclusive + shardIndex
            val seed = spec.seedBase + rollIndex
            val context =
                LootRollContext(
                    sourceLevel = spec.sourceLevel,
                    sourceTier = spec.sourceTier,
                    zoneId = spec.zoneId,
                    playerLevel = spec.playerLevel,
                    magicFindBonus = spec.magicFind,
                    seed = seed,
                )
            val generated =
                ItemGenerator(
                    bundle = itemBundleLoader(),
                    random = SplitMix64RandomSource.fromSeed(seed),
                ).rollAndGenerate(
                    context = context,
                    zoneRewardProfile = zoneRewardProfile,
                    pityTracker = pityTracker,
                    specialTierEligibility =
                        spec.specialTierOverride?.let { allowedSpecialTiers ->
                            SpecialTierEligibility(availableSpecialTiers = allowedSpecialTiers)
                        },
                )
            val sample =
                buildRollSample(
                    spec = spec,
                    rollIndex = rollIndex,
                    generated = generated,
                    previousPity = pityTracker,
                    rarePassiveUniverseBySlot = rarePassiveUniverseBySlot,
                )
            allRolls += sample
            availableSpecialTiers =
                availableSpecialTiers + generated.rollResult.budget.specialTierEligibility.availableSpecialTiers
            pityTracker = generated.rollResult.resultingPityTracker
            baseRarityCounts.compute(sample.rolledRarityTier.name) { _, count -> (count ?: 0) + 1 }
            finalTierCounts.compute(sample.finalTier) { _, count -> (count ?: 0) + 1 }
            if (sample.specialTierEligibilityCount > 0) {
                eligibleCount += 1
            }
            if (sample.finalTier == SpecialTier.UNIQUE.name) {
                uniqueCount += 1
            }
            if (sample.finalTier == SpecialTier.ARTIFACT.name) {
                artifactCount += 1
            }
            if (sample.finalTier == SpecialTier.UNIQUE.name || sample.finalTier == SpecialTier.ARTIFACT.name) {
                uniqueArtifactOutcomeCount += 1
                if (sample.meaningfulUniqueArtifactSwap == true) {
                    meaningfulUniqueArtifactSwapCount += 1
                }
            }
            if (sample.rarePityApplied) {
                rarePityActivations += 1
                pityTimeline +=
                    LootPityEvent(
                        rollIndex = rollIndex,
                        kind = "rare",
                        finalTier = sample.finalTier,
                        specialTemplateId = sample.specialTemplateId,
                        beforeValue = sample.previousRarePity,
                        afterValue = sample.resultingRarePity,
                    )
            }
            if (sample.specialPityApplied) {
                uniquePityActivations += 1
                pityTimeline +=
                    LootPityEvent(
                        rollIndex = rollIndex,
                        kind = "special",
                        finalTier = sample.finalTier,
                        specialTemplateId = sample.specialTemplateId,
                        beforeValue = sample.previousSpecialPity,
                        afterValue = sample.resultingSpecialPity,
                    )
            }
            sample.affixBudgetDeviationRatio?.let(affixDeviationRatios::add)
            castSpeedValues += sample.effectiveCastSpeed
            if (sample.rawCastSpeedRating > 0) {
                castSpeedSamples +=
                    LootCastSpeedSample(
                        rollIndex = rollIndex,
                        rawCastSpeedRating = sample.rawCastSpeedRating,
                        effectiveCastSpeed = sample.effectiveCastSpeed,
                        finalTier = sample.finalTier,
                        specialTemplateId = sample.specialTemplateId,
                    )
            }
            sample.affixCostBreakdown.forEach { (affixId, cost) ->
                affixCostHistogram.compute(cost.toString()) { _, count -> (count ?: 0) + 1 }
                affixIdCounts.compute(affixId) { _, count -> (count ?: 0) + 1 }
            }
            if (
                rollIndex < 8 ||
                sample.specialTier != null ||
                sample.rarePityApplied ||
                sample.specialPityApplied ||
                sample.rawCastSpeedRating > 0
            ) {
                if (sampleRolls.none { existing -> existing.rollIndex == sample.rollIndex }) {
                    sampleRolls += sample
                }
            }
        }

        return LootMatrixShardExecution(
            shard =
                LootMatrixKernelShard(
                    shardId = shardSpec.shardId,
                    matrixId = spec.id,
                    rollStartInclusive = shardSpec.rollStartInclusive,
                    rollCount = shardSpec.rollCount,
                    startingPityTracker = startingPityTracker,
                    resultingPityTracker = pityTracker,
                    availableSpecialTiers = availableSpecialTiers,
                    finalTierCounts = finalTierCounts.toSortedMap(),
                    baseRarityCounts = baseRarityCounts.toSortedMap(),
                    affixDeviationRatios = affixDeviationRatios.toList(),
                    castSpeedValues = castSpeedValues.toList(),
                    affixCostHistogram = affixCostHistogram.toSortedMap(),
                    affixIdCounts = affixIdCounts.toSortedMap(),
                    pityTimeline = pityTimeline.sortedBy(LootPityEvent::rollIndex),
                    castSpeedSamples = castSpeedSamples.sortedByDescending(LootCastSpeedSample::rawCastSpeedRating),
                    sampleRolls = sampleRolls.sortedBy(LootRollSample::rollIndex),
                    eligibleCount = eligibleCount,
                    uniqueCount = uniqueCount,
                    artifactCount = artifactCount,
                    uniqueArtifactOutcomeCount = uniqueArtifactOutcomeCount,
                    meaningfulUniqueArtifactSwapCount = meaningfulUniqueArtifactSwapCount,
                    rarePityActivations = rarePityActivations,
                    uniquePityActivations = uniquePityActivations,
                ),
            rolls = allRolls.sortedBy(LootRollSample::rollIndex),
        )
    }

    private fun mergeMatrixShards(
        spec: LootMatrixSpec,
        shards: List<LootMatrixKernelShard>,
        zoneRewardProfile: ZoneRewardProfile,
    ): LootMatrixResult {
        val availableSpecialTiers = shards.flatMapTo(linkedSetOf(), LootMatrixKernelShard::availableSpecialTiers)
        val finalTierCounts = mergeIntMaps(shards.map(LootMatrixKernelShard::finalTierCounts))
        val baseRarityCounts = mergeIntMaps(shards.map(LootMatrixKernelShard::baseRarityCounts))
        val affixDeviationRatios = shards.flatMap(LootMatrixKernelShard::affixDeviationRatios)
        val castSpeedValues = shards.flatMap(LootMatrixKernelShard::castSpeedValues)
        val affixCostHistogram = mergeIntMaps(shards.map(LootMatrixKernelShard::affixCostHistogram)).toSortedMap()
        val affixIdCounts = mergeIntMaps(shards.map(LootMatrixKernelShard::affixIdCounts))
        val pityTimeline = shards.flatMap(LootMatrixKernelShard::pityTimeline).sortedBy(LootPityEvent::rollIndex)
        val castSpeedSamples =
            shards
                .flatMap(LootMatrixKernelShard::castSpeedSamples)
                .sortedByDescending(LootCastSpeedSample::rawCastSpeedRating)
        val sampleRolls =
            shards
                .flatMap(LootMatrixKernelShard::sampleRolls)
                .sortedBy(LootRollSample::rollIndex)
                .distinctBy(LootRollSample::rollIndex)
        val expected =
            expectedDistribution(
                spec = spec,
                zoneRewardProfile = zoneRewardProfile,
                eligibleCount = shards.sumOf(LootMatrixKernelShard::eligibleCount),
                totalRolls = spec.rollCount,
                availableSpecialTiers = availableSpecialTiers,
            )
        val rarityDistribution = finalTierCounts.mapValues { (_, count) -> count.toDouble() / spec.rollCount.toDouble() }
        val baseDistribution = baseRarityCounts.mapValues { (_, count) -> count.toDouble() / spec.rollCount.toDouble() }
        val magicRateDrift = abs(rarityDistribution.getValue("MAGIC") - expected.magicRate)
        val rareRateDrift = abs(rarityDistribution.getValue("RARE") - expected.rareRate)
        val uniqueCount = shards.sumOf(LootMatrixKernelShard::uniqueCount)
        val artifactCount = shards.sumOf(LootMatrixKernelShard::artifactCount)
        val uniqueRate = uniqueCount.toDouble() / spec.rollCount.toDouble()
        val artifactRate = artifactCount.toDouble() / spec.rollCount.toDouble()
        val uniqueRelativeError = relativeError(actual = uniqueRate, expected = expected.uniqueRate)
        val artifactRelativeError = relativeError(actual = artifactRate, expected = expected.artifactRate)
        val affixBudgetAverageDeviation = affixDeviationRatios.averageOrZero()
        val affixBudgetP95Deviation = percentile(affixDeviationRatios, 0.95)
        val castSpeedPostDrP50 = percentile(castSpeedValues, 0.50)
        val castSpeedPostDrP95 = percentile(castSpeedValues, 0.95)
        var failedExpectationCount = 0
        if (magicRateDrift > MAGIC_RARE_ABSOLUTE_DRIFT_TOLERANCE) {
            failedExpectationCount += 1
        }
        if (rareRateDrift > MAGIC_RARE_ABSOLUTE_DRIFT_TOLERANCE) {
            failedExpectationCount += 1
        }
        if (expected.uniqueRate > 0.0 && uniqueRelativeError > SPECIAL_RELATIVE_ERROR_TOLERANCE) {
            failedExpectationCount += 1
        }
        if (expected.artifactRate > 0.0 && artifactRelativeError > SPECIAL_RELATIVE_ERROR_TOLERANCE) {
            failedExpectationCount += 1
        }
        if (affixBudgetAverageDeviation > AFFIX_BUDGET_AVERAGE_TOLERANCE) {
            failedExpectationCount += 1
        }
        if (affixBudgetP95Deviation > AFFIX_BUDGET_P95_TOLERANCE) {
            failedExpectationCount += 1
        }
        if (spec.sourceTier == SourceTier.NORMAL && (uniqueCount > 0 || artifactCount > 0)) {
            failedExpectationCount += 1
        }
        if (!spec.allowsArtifact() && artifactCount > 0) {
            failedExpectationCount += 1
        }

        return LootMatrixResult(
            spec = spec,
            totalRolls = spec.rollCount,
            availableSpecialTiers = availableSpecialTiers,
            rarityTierDistribution = rarityDistribution,
            baseRarityDistribution = baseDistribution,
            expectedDistribution = expected,
            affixBudgetAverageDeviation = affixBudgetAverageDeviation,
            affixBudgetP95Deviation = affixBudgetP95Deviation,
            uniqueRate = uniqueRate,
            artifactRate = artifactRate,
            specialTierEligibilityRate = shards.sumOf(LootMatrixKernelShard::eligibleCount).toDouble() / spec.rollCount.toDouble(),
            rarePityActivations = shards.sumOf(LootMatrixKernelShard::rarePityActivations),
            uniquePityActivations = shards.sumOf(LootMatrixKernelShard::uniquePityActivations),
            castSpeedPostDrP50 = castSpeedPostDrP50,
            castSpeedPostDrP95 = castSpeedPostDrP95,
            magicRateDrift = magicRateDrift,
            rareRateDrift = rareRateDrift,
            uniqueRelativeError = uniqueRelativeError,
            artifactRelativeError = artifactRelativeError,
            failedExpectationCount = failedExpectationCount,
            affixCostHistogram = affixCostHistogram,
            topAffixIds = affixIdCounts.entries.sortedByDescending(Map.Entry<String, Int>::value).take(8).associate { it.key to it.value },
            pityTimeline = pityTimeline.take(20),
            castSpeedSamples = castSpeedSamples.take(10),
            sampleRolls = sampleRolls.take(16),
            uniqueArtifactOutcomeCount = shards.sumOf(LootMatrixKernelShard::uniqueArtifactOutcomeCount),
            meaningfulUniqueArtifactSwapCount = shards.sumOf(LootMatrixKernelShard::meaningfulUniqueArtifactSwapCount),
            meaningfulUniqueArtifactSwapRate =
                if (shards.sumOf(LootMatrixKernelShard::uniqueArtifactOutcomeCount) == 0) {
                    0.0
                } else {
                    shards.sumOf(LootMatrixKernelShard::meaningfulUniqueArtifactSwapCount).toDouble() /
                        shards.sumOf(LootMatrixKernelShard::uniqueArtifactOutcomeCount).toDouble()
                },
        )
    }

    private fun matrixShardSpecs(spec: LootMatrixSpec): List<LootMatrixShardSpec> =
        (0 until spec.rollCount step ROLLS_PER_SHARD).map { rollStartInclusive ->
            LootMatrixShardSpec(
                matrixId = spec.id,
                rollStartInclusive = rollStartInclusive,
                rollCount = minOf(ROLLS_PER_SHARD, spec.rollCount - rollStartInclusive),
            )
        }

    private fun readShardPayload(path: Path): LootMatrixKernelShard =
        VerificationCacheSupport.json.parseToJsonElement(Files.readString(path)).jsonObject.toLootMatrixKernelShard()

    private fun writeShardPayload(
        shardPayloadPath: Path,
        shardRollPath: Path,
        shard: LootMatrixKernelShard,
        rolls: List<LootRollSample>,
    ) {
        Files.createDirectories(shardPayloadPath.parent)
        Files.writeString(
            shardPayloadPath,
            VerificationCacheSupport.json.encodeToString(JsonElement.serializer(), shard.toJson()),
        )
        Files.writeString(
            shardRollPath,
            rolls
                .sortedBy(LootRollSample::rollIndex)
                .joinToString(separator = "\n") { sample ->
                    compactJson.encodeToString(JsonElement.serializer(), sample.toJson())
                } + "\n",
        )
    }

    private fun buildMergedKernelPayload(
        header: com.ktome.core.harness.HarnessReportHeader,
        kernelRun: LootKernelRun,
        cacheMetadata: JsonObject,
    ): JsonObject =
        buildJsonObject {
            put("header", header.toJson())
            put("kernelCache", cacheMetadata)
            putJsonObject("summary") {
                put("matrixCount", kernelRun.matrices.size)
                put("totalRolls", kernelRun.totalRolls)
                put("failedExpectationCount", kernelRun.failedExpectationCount)
                put("magicRareAbsoluteDriftTolerance", MAGIC_RARE_ABSOLUTE_DRIFT_TOLERANCE)
                put("specialRelativeErrorTolerance", SPECIAL_RELATIVE_ERROR_TOLERANCE)
                put("affixBudgetAverageTolerance", AFFIX_BUDGET_AVERAGE_TOLERANCE)
                put("affixBudgetP95Tolerance", AFFIX_BUDGET_P95_TOLERANCE)
                put("clampDistributionTolerance", CLAMP_DISTRIBUTION_TOLERANCE)
                put("rarePityActivations", kernelRun.matrices.sumOf(LootMatrixResult::rarePityActivations))
                put("uniquePityActivations", kernelRun.matrices.sumOf(LootMatrixResult::uniquePityActivations))
                put("maxMagicRateDrift", kernelRun.matrices.maxOfOrNull(LootMatrixResult::magicRateDrift) ?: 0.0)
                put("maxRareRateDrift", kernelRun.matrices.maxOfOrNull(LootMatrixResult::rareRateDrift) ?: 0.0)
                put("maxUniqueRelativeError", kernelRun.matrices.maxOfOrNull(LootMatrixResult::uniqueRelativeError) ?: 0.0)
                put("maxArtifactRelativeError", kernelRun.matrices.maxOfOrNull(LootMatrixResult::artifactRelativeError) ?: 0.0)
                put("verdict", if (kernelRun.failedExpectationCount == 0) "PASS" else "FAIL")
            }
            put("specialTemplatePool", kernelRun.specialPoolSummary.toJson())
            put("magicFindClampComparison", kernelRun.clampComparison.toJson())
            put("profileOverlapSummary", kernelRun.profileOverlapSummary.toJson())
            put("passiveCoverageSummary", kernelRun.passiveCoverageSummary.toJson())
            putJsonArray("matrices") {
                kernelRun.matrices.forEach { matrix -> add(matrix.toJson()) }
            }
        }

    private fun lootKernelFingerprintInputs(repoRoot: Path): List<Path> =
        listOf(
            repoRoot.resolve("core/src/main/kotlin/com/ktome/core"),
            repoRoot.resolve("tools/src/main/kotlin/com/ktome/tools/loot"),
            repoRoot.resolve("game/src/main/kotlin/com/ktome/game/data"),
            repoRoot.resolve("game/src/main/kotlin/com/ktome/game/loot"),
            repoRoot.resolve("game/src/main/kotlin/com/ktome/game/mapgen"),
            repoRoot.resolve("game/src/main/resources/data/events"),
            repoRoot.resolve("game/src/main/resources/data/items"),
            repoRoot.resolve("game/src/main/resources/data/loot"),
            repoRoot.resolve("game/src/main/resources/data/secret-zones"),
            repoRoot.resolve("game/src/main/resources/data/world"),
        )

    private fun mergeIntMaps(maps: List<Map<String, Int>>): Map<String, Int> =
        buildMap {
            maps.forEach { currentMap ->
                currentMap.forEach { (key, count) ->
                    put(key, (get(key) ?: 0) + count)
                }
            }
        }

    private fun buildRollSample(
        spec: LootMatrixSpec,
        rollIndex: Int,
        generated: GeneratedItemRoll,
        previousPity: com.ktome.core.loot.PityTracker,
        rarePassiveUniverseBySlot: Map<EquipSlot, Set<String>>,
    ): LootRollSample {
        val finalTier = generated.trace.specialTier?.name ?: generated.rollResult.resolvedRarityTier.name
        val specialOrBasePassiveSignatures = generated.item.passive?.let(::passiveSignature)?.let(::setOf).orEmpty()
        val affixPassiveSignatures =
            generated.item.affixes
                .mapNotNull(AffixDef::passive)
                .mapTo(linkedSetOf(), ::passiveSignature)
        val affixBudgetDeviationRatio =
            generated.rollResult.resolvedRarityTier
                .takeIf { rarityTier ->
                    rarityTier in setOf(RarityTier.MAGIC, RarityTier.RARE) &&
                        generated.trace.specialTier == null &&
                        generated.item.type != com.ktome.core.item.ItemType.CONSUMABLE
                }
                ?.let {
                    if (generated.trace.affixBudgetTarget == 0) {
                        0.0
                    } else {
                        generated.trace.affixBudgetDeviation.toDouble() / generated.trace.affixBudgetTarget.toDouble()
                    }
                }
        return LootRollSample(
            matrixId = spec.id,
            rollIndex = rollIndex,
            seed = generated.rollResult.context.seed,
            sourceLevel = spec.sourceLevel,
            sourceTier = spec.sourceTier,
            zoneId = spec.zoneId,
            playerLevel = spec.playerLevel,
            magicFind = spec.magicFind,
            effectiveMagicFind = spec.magicFind.coerceIn(0.0f, 1.0f),
            rolledRarityTier = generated.rollResult.rolledRarityTier,
            resolvedRarityTier = generated.rollResult.resolvedRarityTier,
            finalTier = finalTier,
            specialTier = generated.trace.specialTier,
            specialTemplateId = generated.trace.specialTemplateId,
            generatedBaseItemId = generated.trace.generatedBaseItemId,
            itemType = generated.item.type,
            equipSlot = generated.item.slot,
            specialOrBasePassiveSignatures = specialOrBasePassiveSignatures,
            affixPassiveSignatures = affixPassiveSignatures,
            meaningfulUniqueArtifactSwap =
                generated.trace.specialTier
                    ?.takeIf { specialTier -> specialTier == SpecialTier.UNIQUE || specialTier == SpecialTier.ARTIFACT }
                    ?.let {
                        isMeaningfulUniqueArtifactSwap(
                            item = generated.item,
                            rarePassiveUniverseBySlot = rarePassiveUniverseBySlot,
                            specialOrBasePassiveSignatures = specialOrBasePassiveSignatures,
                            affixPassiveSignatures = affixPassiveSignatures,
                        )
                    },
            specialTierEligibilityCount = generated.rollResult.budget.specialTierEligibility.availableTemplateIds.size,
            rarePityApplied = generated.rollResult.rarePityApplied,
            specialPityApplied = generated.rollResult.specialPityApplied,
            affixBudget = generated.rollResult.budget.affixBudget,
            affixBudgetTarget = generated.trace.affixBudgetTarget,
            affixBudgetConsumed = generated.trace.affixBudgetConsumed,
            affixBudgetDeviationRatio = affixBudgetDeviationRatio,
            rawAffixBudgetShortfall = generated.trace.rawAffixBudgetShortfall,
            rawCastSpeedRating = generated.trace.rawCastSpeedRating,
            effectiveCastSpeed = generated.trace.effectiveCastSpeed,
            affixCostBreakdown = generated.trace.affixCostBreakdown.map { cost -> cost.affixId to cost.cost },
            previousRarePity = previousPity.rollsSinceLastRare,
            previousSpecialPity = previousPity.eligibleSpecialRollsSinceLastUnique,
            resultingRarePity = generated.trace.resultingPityTracker.rollsSinceLastRare,
            resultingSpecialPity = generated.trace.resultingPityTracker.eligibleSpecialRollsSinceLastUnique,
        )
    }

    private fun expectedDistribution(
        spec: LootMatrixSpec,
        zoneRewardProfile: ZoneRewardProfile,
        eligibleCount: Int,
        totalRolls: Int,
        availableSpecialTiers: Set<SpecialTier>,
    ): LootExpectation {
        val rarityScore = spec.sourceTier.rarityBonus + zoneRewardProfile.rarityBonus + spec.magicFind.coerceIn(0.0f, 1.0f) * 0.50f
        val normalWeight = RarityTier.NORMAL.baseWeight
        val magicWeight = scaledWeight(RarityTier.MAGIC.baseWeight, 1.0f + rarityScore * 0.50f)
        val rareWeight = scaledWeight(RarityTier.RARE.baseWeight, 1.0f + rarityScore)
        val baseTotal = normalWeight + magicWeight + rareWeight
        val baseNormalRate = normalWeight.toDouble() / baseTotal.toDouble()
        val baseMagicRate = magicWeight.toDouble() / baseTotal.toDouble()
        val baseRareRate = rareWeight.toDouble() / baseTotal.toDouble()
        val specialEligible = eligibleCount == totalRolls && spec.sourceTier != SourceTier.NORMAL
        val uniqueWeight = if (specialEligible && SpecialTier.UNIQUE in availableSpecialTiers) 2 else 0
        val artifactWeight = if (specialEligible && SpecialTier.ARTIFACT in availableSpecialTiers) 1 else 0
        val noUpgradeWeight = if (uniqueWeight > 0 || artifactWeight > 0) 39 else 0
        val totalSpecialWeight = uniqueWeight + artifactWeight + noUpgradeWeight
        val noUpgradeRate =
            if (totalSpecialWeight == 0) {
                1.0
            } else {
                noUpgradeWeight.toDouble() / totalSpecialWeight.toDouble()
            }
        val uniqueRate =
            if (totalSpecialWeight == 0) {
                0.0
            } else {
                uniqueWeight.toDouble() / totalSpecialWeight.toDouble()
            }
        val artifactRate =
            if (totalSpecialWeight == 0) {
                0.0
            } else {
                artifactWeight.toDouble() / totalSpecialWeight.toDouble()
            }
        val finalRates =
            linkedMapOf(
                "NORMAL" to baseNormalRate * noUpgradeRate,
                "MAGIC" to baseMagicRate * noUpgradeRate,
                "RARE" to baseRareRate * noUpgradeRate,
                "UNIQUE" to uniqueRate,
                "ARTIFACT" to artifactRate,
            )
        return LootExpectation(
            finalRates = finalRates,
            magicRate = finalRates.getValue("MAGIC"),
            rareRate = finalRates.getValue("RARE"),
            uniqueRate = uniqueRate,
            artifactRate = artifactRate,
            noUpgradeRate = noUpgradeRate,
        )
    }

    private fun summarizeSpecialPool(itemBundleLoader: () -> com.ktome.core.item.ItemDataBundle): LootSpecialPoolSummary {
        val bundle = itemBundleLoader()
        val targetZones = setOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple")
        val buildArchetypes = setOf("vanguard", "rogue", "arcanist", "templar")
        val affixCount = bundle.affixes.size
        val uniqueTemplates = bundle.specialTemplates.filter { template -> template.specialTier == SpecialTier.UNIQUE }
        val artifactTemplates = bundle.specialTemplates.filter { template -> template.specialTier == SpecialTier.ARTIFACT }
        val totalCount = affixCount + uniqueTemplates.size + artifactTemplates.size
        val targetZoneCoverage = bundle.specialTemplates.flatMapTo(linkedSetOf()) { template -> template.allowedZones }.intersect(targetZones).size
        val buildArchetypeCount = bundle.specialTemplates.flatMapTo(linkedSetOf()) { template -> template.tags.intersect(buildArchetypes) }.size
        val bossOnlyArtifactTemplateCount = artifactTemplates.count { template -> template.allowedSourceTiers == setOf(SourceTier.BOSS) }
        val chestOnlyArtifactTemplateCount = artifactTemplates.count { template -> template.allowedSourceTiers == setOf(SourceTier.CHEST) }
        val secretZoneArtifactTemplateCount =
            artifactTemplates.count { template -> SourceTier.SECRET_ZONE in template.allowedSourceTiers }
        return LootSpecialPoolSummary(
            affixCount = affixCount,
            uniqueTemplateCount = uniqueTemplates.size,
            artifactTemplateCount = artifactTemplates.size,
            totalCount = totalCount,
            targetZoneCoverageCount = targetZoneCoverage,
            buildArchetypeCount = buildArchetypeCount,
            bossOnlyArtifactTemplateCount = bossOnlyArtifactTemplateCount,
            chestOnlyArtifactTemplateCount = chestOnlyArtifactTemplateCount,
            secretZoneArtifactTemplateCount = secretZoneArtifactTemplateCount,
            passesThresholds =
                affixCount >= 75 &&
                    uniqueTemplates.size >= 20 &&
                    artifactTemplates.size >= 8 &&
                    totalCount >= 103 &&
                    targetZoneCoverage == targetZones.size &&
                    buildArchetypeCount >= 2 &&
                    bossOnlyArtifactTemplateCount > 0 &&
                    chestOnlyArtifactTemplateCount > 0 &&
                    secretZoneArtifactTemplateCount > 0,
        )
    }

    private fun summarizeLootProfileOverlap(
        schemaCatalog: SchemaCatalog,
        profiles: List<LootProfileSchemaV3>,
        itemBundle: ItemDataBundle,
        strictPairCeilings: Map<String, Double>,
    ): LootProfileOverlapSummary {
        val resolver = LootProfileCandidatePoolResolver(itemBundle)
        val candidateBaseIdsByProfileId =
            profiles.associate { profile ->
                profile.id to resolver.resolve(profile).allCandidateBaseIds
            }
        val rewardStructureKeysByProfileId = buildSecretRewardStructureKeysByProfileId(schemaCatalog)
        val overlapMatrix =
            profiles.associate { profile ->
                val leftItems = candidateBaseIdsByProfileId.getValue(profile.id)
                profile.id to
                    profiles
                        .filterNot { candidate -> candidate.id == profile.id }
                        .associate { candidate ->
                            val rightItems = candidateBaseIdsByProfileId.getValue(candidate.id)
                            val denominator = minOf(leftItems.size, rightItems.size).coerceAtLeast(1)
                            candidate.id to leftItems.intersect(rightItems).size.toDouble() / denominator.toDouble()
                        }
            }
        val overlapValues = overlapMatrix.values.flatMap { row -> row.values }
        val metadataByProfileId = profiles.associate { profile -> profile.id to profile.localIdentityMetadata() }
        val sameZoneSecretVsCadencePairs =
            buildLocalOverlapPairs(
                overlapMatrix = overlapMatrix,
                metadataByProfileId = metadataByProfileId,
                comparedCategory = LootProfileLocalIdentityCategory.CADENCE,
            )
        val sameZoneSecretVsRewardPairs =
            buildLocalOverlapPairs(
                overlapMatrix = overlapMatrix,
                metadataByProfileId = metadataByProfileId,
                comparedCategory = LootProfileLocalIdentityCategory.REWARD,
            )
        validateLocalIdentityPairCoverage(
            metadataByProfileId = metadataByProfileId,
            pairs = sameZoneSecretVsCadencePairs,
            comparedCategory = LootProfileLocalIdentityCategory.CADENCE,
        )
        validateLocalIdentityPairCoverage(
            metadataByProfileId = metadataByProfileId,
            pairs = sameZoneSecretVsRewardPairs,
            comparedCategory = LootProfileLocalIdentityCategory.REWARD,
        )
        val distinctBaseItemCount =
            candidateBaseIdsByProfileId.values
                .flatMapTo(linkedSetOf()) { candidateBaseIds -> candidateBaseIds }
                .size
        val strictLocalIdentityViolations =
            (sameZoneSecretVsCadencePairs + sameZoneSecretVsRewardPairs)
                .mapNotNull { pair -> pair.toStrictLocalIdentityViolationOrNull(strictPairCeilings) }
                .distinctBy(LootStrictLocalIdentityViolation::pairId)
                .sortedBy(LootStrictLocalIdentityViolation::pairId)
        return LootProfileOverlapSummary(
            overlapMatrix = overlapMatrix,
            averageOverlap = overlapValues.averageOrZero(),
            maxOverlap = overlapValues.maxOrNull() ?: 0.0,
            distinctBaseItemCount = distinctBaseItemCount,
            sameZoneSecretVsCadencePairs = sameZoneSecretVsCadencePairs,
            sameZoneSecretVsRewardPairs = sameZoneSecretVsRewardPairs,
            localIdentityFailurePairs =
                (
                    sameZoneSecretVsCadencePairs
                        .filter { pair -> pair.exceedsGlobalLocalIdentityGuardrail() }
                        .map(LootLocalOverlapPairSummary::pairId) +
                        sameZoneSecretVsRewardPairs
                            .filter { pair -> pair.exceedsGlobalLocalIdentityGuardrail() }
                            .map(LootLocalOverlapPairSummary::pairId)
                ).distinct(),
            strictLocalIdentityViolations = strictLocalIdentityViolations,
            secretProfileIdentitySummaries =
                buildSecretProfileIdentitySummaries(
                    profiles = profiles,
                    metadataByProfileId = metadataByProfileId,
                    candidateBaseIdsByProfileId = candidateBaseIdsByProfileId,
                    rewardStructureKeysByProfileId = rewardStructureKeysByProfileId,
                    cadencePairsBySecretProfileId = sameZoneSecretVsCadencePairs.groupBy(LootLocalOverlapPairSummary::secretProfileId),
                    rewardPairsBySecretProfileId = sameZoneSecretVsRewardPairs.groupBy(LootLocalOverlapPairSummary::secretProfileId),
                    strictViolationsBySecretProfileId = strictLocalIdentityViolations.groupBy(LootStrictLocalIdentityViolation::secretProfileId),
                    strictPairCeilings = strictPairCeilings,
                ),
        )
    }

    private fun buildLocalOverlapPairs(
        overlapMatrix: Map<String, Map<String, Double>>,
        metadataByProfileId: Map<String, LootProfileLocalIdentityMetadata>,
        comparedCategory: LootProfileLocalIdentityCategory,
    ): List<LootLocalOverlapPairSummary> =
        overlapMatrix.flatMap { (profileId, row) ->
            val profileMetadata = metadataByProfileId.getValue(profileId)
            if (profileMetadata.category != LootProfileLocalIdentityCategory.SECRET || profileMetadata.canonicalZoneId == null) {
                emptyList()
            } else {
                row.mapNotNull { (candidateId, overlap) ->
                    val candidateMetadata = metadataByProfileId.getValue(candidateId)
                    if (candidateMetadata.category != comparedCategory || candidateMetadata.canonicalZoneId != profileMetadata.canonicalZoneId) {
                        null
                    } else {
                        LootLocalOverlapPairSummary(
                            zoneId = requireNotNull(profileMetadata.canonicalZoneId),
                            pairType = localIdentityPairTypeForComparedCategory(comparedCategory),
                            secretProfileId = profileId,
                            comparedProfileId = candidateId,
                            overlap = overlap,
                        )
                    }
                }
            }
        }

    private fun buildSecretProfileIdentitySummaries(
        profiles: List<LootProfileSchemaV3>,
        metadataByProfileId: Map<String, LootProfileLocalIdentityMetadata>,
        candidateBaseIdsByProfileId: Map<String, Set<String>>,
        rewardStructureKeysByProfileId: Map<String, List<String>>,
        cadencePairsBySecretProfileId: Map<String, List<LootLocalOverlapPairSummary>>,
        rewardPairsBySecretProfileId: Map<String, List<LootLocalOverlapPairSummary>>,
        strictViolationsBySecretProfileId: Map<String, List<LootStrictLocalIdentityViolation>>,
        strictPairCeilings: Map<String, Double>,
    ): List<LootSecretProfileIdentitySummary> =
        profiles
            .filter { profile -> metadataByProfileId.getValue(profile.id).category == LootProfileLocalIdentityCategory.SECRET }
            .map { profile ->
                val metadata = metadataByProfileId.getValue(profile.id)
                val cadencePairs = cadencePairsBySecretProfileId[profile.id].orEmpty()
                val rewardPairs = rewardPairsBySecretProfileId[profile.id].orEmpty()
                val strictViolations = strictViolationsBySecretProfileId[profile.id].orEmpty()
                val rewardStructureKeys = rewardStructureKeysByProfileId[profile.id].orEmpty()
                LootSecretProfileIdentitySummary(
                    profileId = profile.id,
                    canonicalZoneId = requireNotNull(metadata.canonicalZoneId) {
                        "Secret loot profile '${profile.id}' must resolve canonicalZoneId for identity summary."
                    },
                    poolStrategy = profile.poolStrategy.name,
                    identityAxes = profile.identityAxes(rewardStructureKeys),
                    rewardStructureKeys = rewardStructureKeys,
                    fixedItemIds = profile.itemIds.sorted(),
                    candidateBaseIds = candidateBaseIdsByProfileId.getValue(profile.id).sorted(),
                    typeWeights = profile.typeWeights.entries.associate { (type, weight) -> type.name to weight },
                    slotBias = profile.slotBias.entries.associate { (slot, weight) -> slot.name to weight },
                    specialTemplateTagPreference = profile.specialTemplateTagPreference.sorted(),
                    affixTagPreference = profile.affixTagPreference.sorted(),
                    sameZoneCadenceMaxOverlap = cadencePairs.maxOfOrNull(LootLocalOverlapPairSummary::overlap),
                    sameZoneRewardMaxOverlap = rewardPairs.maxOfOrNull(LootLocalOverlapPairSummary::overlap),
                    strictAllowedMaxOverlap = strictSecretProfileMaxOverlapTarget(profile.id, strictPairCeilings),
                    strictViolationPairIds = strictViolations.map(LootStrictLocalIdentityViolation::pairId).sorted(),
                )
            }
            .sortedBy(LootSecretProfileIdentitySummary::profileId)

    private fun LootProfileSchemaV3.identityAxes(rewardStructureKeys: List<String>): List<String> =
        buildList {
            if (itemIds.isNotEmpty()) add("fixed_item_ids")
            if (typeWeights.isNotEmpty()) add("type_weights")
            if (slotBias.isNotEmpty()) add("slot_bias")
            if (specialTemplateTagPreference.isNotEmpty()) add("special_template_bias")
            if (affixTagPreference.isNotEmpty()) add("affix_bias")
            if (rewardStructureKeys.any { rewardKey -> rewardKey != "LOOT_PROFILE" } || rewardStructureKeys.distinct().size > 1) {
                add("reward_structure")
            }
        }

    private fun buildSecretRewardStructureKeysByProfileId(schemaCatalog: SchemaCatalog): Map<String, List<String>> {
        val rewardKeysByProfileId =
            schemaCatalog.hiddenEvents
                .flatMap { hiddenEvent ->
                    val rewardKeys = hiddenEvent.rewards.map { reward -> reward.key.name }
                    hiddenEvent.rewards.mapNotNull { reward ->
                        val lootProfilePayload = reward.payload as? HiddenEventRewardPayload.LootProfile ?: return@mapNotNull null
                        lootProfilePayload.lootProfileRef.id to rewardKeys
                    }
                }.groupBy(
                    keySelector = Pair<String, List<String>>::first,
                    valueTransform = Pair<String, List<String>>::second,
                ).mapValues { (_, rewardKeyGroups) ->
                    rewardKeyGroups
                        .flatten()
                        .distinct()
                        .sorted()
                }
        return schemaCatalog.secretZones.associate { secretZone ->
            secretZone.rewardProfileId.id to rewardKeysByProfileId[secretZone.rewardProfileId.id].orEmpty()
        }
    }

    private fun validateLocalIdentityPairCoverage(
        metadataByProfileId: Map<String, LootProfileLocalIdentityMetadata>,
        pairs: List<LootLocalOverlapPairSummary>,
        comparedCategory: LootProfileLocalIdentityCategory,
    ) {
        val secretZoneIds =
            metadataByProfileId.values
                .asSequence()
                .filter { metadata -> metadata.category == LootProfileLocalIdentityCategory.SECRET }
                .mapNotNull(LootProfileLocalIdentityMetadata::canonicalZoneId)
                .toSet()
        val pairedZoneIds = pairs.mapTo(linkedSetOf(), LootLocalOverlapPairSummary::zoneId)
        val missingZoneIds = secretZoneIds - pairedZoneIds
        require(missingZoneIds.isEmpty()) {
            "Local identity metric lost same-zone ${localIdentityPairTypeForComparedCategory(comparedCategory)} coverage for zones ${missingZoneIds.sorted()}."
        }
    }

    private fun summarizeAffixPassiveCoverage(bundle: ItemDataBundle): LootPassiveCoverageSummary {
        val passiveKinds =
            bundle.affixes
                .mapNotNull(AffixDef::passive)
                .mapTo(linkedSetOf(), ::passiveKind)
        return LootPassiveCoverageSummary(
            passiveKinds = passiveKinds,
            coverageRatio = passiveKinds.size.toDouble() / EQUIPMENT_PASSIVE_KIND_COUNT.toDouble(),
        )
    }

    private fun buildRarePassiveUniverseBySlot(bundle: ItemDataBundle): Map<EquipSlot, Set<String>> {
        val specialItemIds = bundle.specialTemplates.mapTo(linkedSetOf()) { template -> template.itemId }
        val affixPassiveSignaturesByEquipType =
            bundle.affixes
                .asSequence()
                .filter { affix -> affix.passive != null }
                .groupBy(AffixDef::equipType)
                .mapValues { (_, affixes) ->
                    affixes.mapNotNull(AffixDef::passive).mapTo(linkedSetOf(), ::passiveSignature)
                }
        return bundle.baseItems
            .asSequence()
            .filterNot { baseItem -> baseItem.id in specialItemIds }
            .filter { baseItem -> baseItem.slot != null }
            .groupBy { baseItem -> requireNotNull(baseItem.slot) }
            .mapValues { (_, baseItems) ->
                buildSet {
                    baseItems.mapNotNull(ItemBaseDef::passive).mapTo(this, ::passiveSignature)
                    baseItems
                        .mapNotNull(::equipTypeForBase)
                        .flatMapTo(this) { equipType -> affixPassiveSignaturesByEquipType[equipType].orEmpty() }
                }
            }
    }

    private fun equipTypeForBase(baseItem: ItemBaseDef): AffixEquipType? =
        when (baseItem.type) {
            ItemType.WEAPON -> AffixEquipType.WEAPON
            ItemType.ARMOR -> AffixEquipType.ARMOR
            ItemType.CONSUMABLE -> null
        }

    private fun isMeaningfulUniqueArtifactSwap(
        item: ItemInstance,
        rarePassiveUniverseBySlot: Map<EquipSlot, Set<String>>,
        specialOrBasePassiveSignatures: Set<String>,
        affixPassiveSignatures: Set<String>,
    ): Boolean {
        val equipSlot = item.slot ?: return false
        val specialPassiveUniverse = specialOrBasePassiveSignatures + affixPassiveSignatures
        if (specialPassiveUniverse.isEmpty()) {
            return false
        }
        val rareUniverse = rarePassiveUniverseBySlot[equipSlot].orEmpty()
        return !specialPassiveUniverse.all(rareUniverse::contains)
    }

    private fun compareClampBoundary(matrices: List<LootMatrixResult>): LootClampComparison {
        val reference = requireNotNull(matrices.firstOrNull { matrix -> matrix.spec.id == "abyssal_boss_mf100" })
        val overflow = requireNotNull(matrices.firstOrNull { matrix -> matrix.spec.id == "abyssal_boss_mf150" })
        val tierDelta =
            reference.rarityTierDistribution.keys.associateWith { tier ->
                abs(reference.rarityTierDistribution.getValue(tier) - overflow.rarityTierDistribution.getValue(tier))
            }
        val maxDistributionDelta = tierDelta.values.maxOrNull() ?: 0.0
        return LootClampComparison(
            referenceMatrixId = reference.spec.id,
            overflowMatrixId = overflow.spec.id,
            tierDelta = tierDelta.toSortedMap(),
            maxDistributionDelta = maxDistributionDelta,
            withinTolerance = maxDistributionDelta <= CLAMP_DISTRIBUTION_TOLERANCE,
        )
    }

    internal fun renderRarityTable(matrix: LootMatrixResult): String =
        buildString {
            appendLine("| Tier | Observed | Expected |")
            appendLine("| --- | ---: | ---: |")
            matrix.rarityTierDistribution.forEach { (tier, observed) ->
                val expected = matrix.expectedDistribution.finalRates.getValue(tier)
                appendLine("| $tier | ${"%.4f".format(observed)} | ${"%.4f".format(expected)} |")
            }
        }

    internal fun renderAffixBreakdown(matrix: LootMatrixResult): String =
        buildString {
            appendLine("| Metric | Value |")
            appendLine("| --- | ---: |")
            appendLine("| averageDeviation | ${"%.4f".format(matrix.affixBudgetAverageDeviation)} |")
            appendLine("| p95Deviation | ${"%.4f".format(matrix.affixBudgetP95Deviation)} |")
            appendLine()
            appendLine("| Cost | Count |")
            appendLine("| --- | ---: |")
            matrix.affixCostHistogram.forEach { (cost, count) ->
                appendLine("| $cost | $count |")
            }
            appendLine()
            appendLine("| Affix | Count |")
            appendLine("| --- | ---: |")
            matrix.topAffixIds.forEach { (affixId, count) ->
                appendLine("| $affixId | $count |")
            }
        }

    internal fun renderSpecialTrace(matrix: LootMatrixResult): String =
        buildString {
            appendLine("| Roll | Final Tier | Template | Eligible Templates | Rare Pity | Special Pity |")
            appendLine("| --- | --- | --- | ---: | --- | --- |")
            matrix.sampleRolls.forEach { sample ->
                appendLine(
                    "| ${sample.rollIndex} | ${sample.finalTier} | ${sample.specialTemplateId ?: "-"} | " +
                        "${sample.specialTierEligibilityCount} | ${sample.rarePityApplied} | ${sample.specialPityApplied} |",
                )
            }
        }

    internal fun renderPityTimeline(matrix: LootMatrixResult): String =
        buildString {
            appendLine("| Roll | Kind | Final Tier | Template | Before | After |")
            appendLine("| --- | --- | --- | --- | ---: | ---: |")
            matrix.pityTimeline.forEach { event ->
                appendLine(
                    "| ${event.rollIndex} | ${event.kind} | ${event.finalTier} | ${event.specialTemplateId ?: "-"} | " +
                        "${event.beforeValue} | ${event.afterValue} |",
                )
            }
        }

    internal fun renderCastSpeedTable(matrix: LootMatrixResult): String =
        buildString {
            appendLine("| Metric | Value |")
            appendLine("| --- | ---: |")
            appendLine("| p50 | ${"%.4f".format(matrix.castSpeedPostDrP50)} |")
            appendLine("| p95 | ${"%.4f".format(matrix.castSpeedPostDrP95)} |")
            appendLine()
            appendLine("| Roll | Raw | Effective | Final Tier | Template |")
            appendLine("| --- | ---: | ---: | --- | --- |")
            matrix.castSpeedSamples.forEach { sample ->
                appendLine(
                    "| ${sample.rollIndex} | ${sample.rawCastSpeedRating} | ${"%.4f".format(sample.effectiveCastSpeed)} | " +
                        "${sample.finalTier} | ${sample.specialTemplateId ?: "-"} |",
                )
            }
        }

    private fun scaledWeight(
        baseWeight: Int,
        multiplier: Float,
    ): Int = (baseWeight.toFloat() * multiplier).roundToInt().coerceAtLeast(1)

    private fun relativeError(
        actual: Double,
        expected: Double,
    ): Double =
        when {
            expected == 0.0 && actual == 0.0 -> 0.0
            expected == 0.0 -> 1.0
            else -> abs(actual - expected) / expected
        }

    private fun percentile(
        values: List<Double>,
        quantile: Double,
    ): Double {
        if (values.isEmpty()) {
            return 0.0
        }
        val sorted = values.sorted()
        val index = ((sorted.size - 1) * quantile).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

    private fun passiveKind(passive: EquipmentPassive): String =
        when (passive) {
            is EquipmentPassive.OnHitStatusProc -> "OnHitStatusProc"
            is EquipmentPassive.OnKillResourceRestore -> "OnKillResourceRestore"
            is EquipmentPassive.ConditionalStatBonus -> "ConditionalStatBonus"
            is EquipmentPassive.TerrainAffinityBonus -> "TerrainAffinityBonus"
            is EquipmentPassive.DamageVsTag -> "DamageVsTag"
            is EquipmentPassive.DamageVsStatus -> "DamageVsStatus"
            is EquipmentPassive.HpRegenPerTurn -> "HpRegenPerTurn"
            is EquipmentPassive.DamageTypeBonus -> "DamageTypeBonus"
            is EquipmentPassive.ResistanceBonus -> "ResistanceBonus"
        }

    private fun passiveSignature(passive: EquipmentPassive): String =
        when (passive) {
            is EquipmentPassive.OnHitStatusProc ->
                "OnHitStatusProc:${passive.statusId}:${passive.chance}:${passive.duration}:${passive.magnitude}"

            is EquipmentPassive.OnKillResourceRestore ->
                "OnKillResourceRestore:${passive.resourceType.name}:${passive.amount}"

            is EquipmentPassive.ConditionalStatBonus ->
                "ConditionalStatBonus:${passive.condition.name}:${passive.statusId ?: "-"}:${statModifierSignature(passive.statModifier)}"

            is EquipmentPassive.TerrainAffinityBonus ->
                "TerrainAffinityBonus:${passive.terrainTag.name}:${statModifierSignature(passive.statModifier)}"

            is EquipmentPassive.DamageVsTag -> "DamageVsTag:${passive.tag}:${passive.bonusPercent}"
            is EquipmentPassive.DamageVsStatus -> "DamageVsStatus:${passive.statusId}:${passive.bonusPercent}"
            is EquipmentPassive.HpRegenPerTurn -> "HpRegenPerTurn:${passive.amount}"
            is EquipmentPassive.DamageTypeBonus -> "DamageTypeBonus:${passive.type.name}:${passive.bonusPercent}"
            is EquipmentPassive.ResistanceBonus -> "ResistanceBonus:${passive.damageType.name}:${passive.amount}"
        }

    private fun statModifierSignature(modifier: StatModifier): String =
        listOf(
            modifier.str,
            modifier.dex,
            modifier.con,
            modifier.wil,
            modifier.attack,
            modifier.defense,
            modifier.accuracy,
            modifier.evasion,
            modifier.speed,
            modifier.castSpeedRating,
            modifier.maxHp,
            modifier.maxStamina,
            modifier.hpRegen,
            modifier.staminaRegen,
            modifier.critChance,
            modifier.talentPower,
            modifier.attackMultiplierBonus,
            modifier.defenseMultiplierBonus,
        ).joinToString(":")
}

object LootBalanceLabRunner {
    const val HARNESS_ID: String = "lootBalanceLab"
    private const val SUMMARY_FILE: String = "loot-balance-summary.json"
    private const val ROLLS_FILE: String = "loot-balance-rolls.jsonl"
    private val json: Json = Json { prettyPrint = true }

    fun run(): LootBalanceLabRun {
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val rollsPath = outputDir.resolve(ROLLS_FILE)
        val repoRoot = VerificationCacheSupport.repoRoot()
        val kernelExecution = LootLabKernel.execute()
        VerificationCacheSupport.mergeJsonlFiles(targetPath = rollsPath, sourcePaths = kernelExecution.shardRollPaths)
        val kernelRun = kernelExecution.kernelRun
        val header = phase4HarnessHeader(harnessId = HARNESS_ID, seedList = kernelRun.matrixSeeds, locale = LOOT_REPORT_LOCALE.id)
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        Files.writeString(
            summaryPath,
            json.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(
                    header = header,
                    kernelRun = kernelRun,
                    kernelCacheMetadata = kernelExecution.cacheMetadata(repoRoot),
                ),
            ),
        )
        return LootBalanceLabRun(
            matrixCount = kernelRun.matrices.size,
            totalRolls = kernelRun.totalRolls,
            failedExpectationCount = kernelRun.failedExpectationCount,
            summaryPath = summaryPath,
            rollsPath = rollsPath,
        )
    }

    private fun buildSummaryPayload(
        header: com.ktome.core.harness.HarnessReportHeader,
        kernelRun: LootKernelRun,
        kernelCacheMetadata: JsonObject,
    ): JsonObject =
        buildJsonObject {
            put("header", header.toJson())
            put("kernelCache", kernelCacheMetadata)
            putJsonObject("summary") {
                put("matrixCount", kernelRun.matrices.size)
                put("totalRolls", kernelRun.totalRolls)
                put("failedExpectationCount", kernelRun.failedExpectationCount)
                put("magicRareAbsoluteDriftTolerance", MAGIC_RARE_ABSOLUTE_DRIFT_TOLERANCE)
                put("specialRelativeErrorTolerance", SPECIAL_RELATIVE_ERROR_TOLERANCE)
                put("affixBudgetAverageTolerance", AFFIX_BUDGET_AVERAGE_TOLERANCE)
                put("affixBudgetP95Tolerance", AFFIX_BUDGET_P95_TOLERANCE)
                put("clampDistributionTolerance", CLAMP_DISTRIBUTION_TOLERANCE)
                put("rarePityActivations", kernelRun.matrices.sumOf(LootMatrixResult::rarePityActivations))
                put("uniquePityActivations", kernelRun.matrices.sumOf(LootMatrixResult::uniquePityActivations))
                put("maxMagicRateDrift", kernelRun.matrices.maxOfOrNull(LootMatrixResult::magicRateDrift) ?: 0.0)
                put("maxRareRateDrift", kernelRun.matrices.maxOfOrNull(LootMatrixResult::rareRateDrift) ?: 0.0)
                put("maxUniqueRelativeError", kernelRun.matrices.maxOfOrNull(LootMatrixResult::uniqueRelativeError) ?: 0.0)
                put("maxArtifactRelativeError", kernelRun.matrices.maxOfOrNull(LootMatrixResult::artifactRelativeError) ?: 0.0)
                put("verdict", if (kernelRun.failedExpectationCount == 0) "PASS" else "FAIL")
            }
            put("specialTemplatePool", kernelRun.specialPoolSummary.toJson())
            put("magicFindClampComparison", kernelRun.clampComparison.toJson())
            put("profileOverlapSummary", kernelRun.profileOverlapSummary.toJson())
            put("passiveCoverageSummary", kernelRun.passiveCoverageSummary.toJson())
            putJsonArray("matrices") {
                kernelRun.matrices.forEach { matrix -> add(matrix.toJson()) }
            }
        }

    internal fun readKernelRun(reportDir: Path = reportDir()): LootKernelRun? {
        val summaryPath = reportDir.resolve(SUMMARY_FILE)
        if (Files.isRegularFile(summaryPath)) {
            val payload = json.parseToJsonElement(Files.readString(summaryPath)).jsonObject
            if (payload.hasCurrentKernelContractVersion()) {
                return payload.toLootKernelRun()
            }
        }
        val repoRoot = VerificationCacheSupport.repoRoot()
        val cacheDirs = VerificationCacheSupport.cacheDirs("loot", repoRoot)
        val mergedKernelPath = cacheDirs.kernelDir.resolve("merged").resolve("loot-kernel-merged.json")
        if (!Files.isRegularFile(mergedKernelPath)) {
            return null
        }
        val payload = json.parseToJsonElement(Files.readString(mergedKernelPath)).jsonObject
        return if (payload.hasCurrentKernelContractVersion()) payload.toLootKernelRun() else null
    }

    internal fun lootPreflightReportDir(repoRoot: Path = VerificationCacheSupport.repoRoot()): Path {
        val configured = System.getProperty("ktome.phase4.loot.preflight.reportDir")
        return if (configured.isNullOrBlank()) {
            repoRoot.resolve("tools/build/reports/verification/loot/preflight")
        } else {
            Path.of(configured)
        }
    }

    internal fun lootPreflightSummaryPath(repoRoot: Path = VerificationCacheSupport.repoRoot()): Path =
        lootPreflightReportDir(repoRoot).resolve(LootPreflightRunner.SUMMARY_FILE_NAME)

    private fun JsonObject.toLootKernelRun(): LootKernelRun {
        val matrices =
            getValue("matrices").jsonArray.map { matrix ->
                matrix.jsonObject.toLootMatrixResult()
            }
        val profileOverlapSummary = getValue("profileOverlapSummary").jsonObject.toLootProfileOverlapSummary()
        val passiveCoverageSummary = getValue("passiveCoverageSummary").jsonObject.toLootPassiveCoverageSummary()
        val matrixSeeds = getValue("header").jsonObject.getValue("seedList").jsonArray.map { seed -> seed.jsonPrimitive.content.toLong() }
        return LootKernelRun(
            matrices = matrices,
            specialPoolSummary = getValue("specialTemplatePool").jsonObject.toLootSpecialPoolSummary(),
            clampComparison = getValue("magicFindClampComparison").jsonObject.toLootClampComparison(),
            matrixSeeds = matrixSeeds,
            profileOverlapSummary = profileOverlapSummary,
            passiveCoverageSummary = passiveCoverageSummary,
        )
    }

    private fun JsonObject.hasCurrentKernelContractVersion(): Boolean =
        this["kernelCache"]
            ?.jsonObject
            ?.get("contractVersion")
            ?.jsonPrimitive
            ?.contentOrNull == LOOT_KERNEL_CACHE_VERSION

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.loot.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4", "loot")
        } else {
            Path.of(configured)
        }
    }
}

private fun JsonObject.toLootMatrixResult(): LootMatrixResult {
    val expectedDistribution = getValue("expectedDistribution").jsonObject.toLootExpectation()
    return LootMatrixResult(
        spec =
            LootMatrixSpec(
                id = stringValue("matrixId"),
                zoneId = stringValue("zoneId"),
                sourceLevel = intValue("sourceLevel"),
                sourceTier = SourceTier.valueOf(stringValue("sourceTier")),
                sourceDescriptor = stringValue("sourceDescriptor"),
                playerLevel = intValue("playerLevel"),
                magicFind = floatValue("magicFind"),
                specialTierOverride =
                    getValue("availableSpecialTiers").jsonArray
                        .map { tier -> SpecialTier.valueOf(tier.jsonPrimitive.content) }
                        .toSet(),
                rollCount = intValue("rollCount"),
                seedBase = longValue("seedBase"),
            ),
        totalRolls = intValue("rollCount"),
        availableSpecialTiers =
            getValue("availableSpecialTiers").jsonArray
                .map { tier -> SpecialTier.valueOf(tier.jsonPrimitive.content) }
                .toSet(),
        rarityTierDistribution = getValue("rarityTierDistribution").jsonObject.toDoubleMap(),
        baseRarityDistribution = getValue("baseRarityDistribution").jsonObject.toDoubleMap(),
        expectedDistribution = expectedDistribution,
        affixBudgetAverageDeviation = getValue("affixBudgetDeviation").jsonObject.doubleValue("average"),
        affixBudgetP95Deviation = getValue("affixBudgetDeviation").jsonObject.doubleValue("p95"),
        uniqueRate = doubleValue("uniqueRate"),
        artifactRate = doubleValue("artifactRate"),
        specialTierEligibilityRate = doubleValue("specialTierEligibilityRate"),
        rarePityActivations = intValue("rarePityActivations"),
        uniquePityActivations = intValue("uniquePityActivations"),
        castSpeedPostDrP50 = doubleValue("castSpeedPostDrP50"),
        castSpeedPostDrP95 = doubleValue("castSpeedPostDrP95"),
        magicRateDrift = getValue("distributionError").jsonObject.doubleValue("magicRateDrift"),
        rareRateDrift = getValue("distributionError").jsonObject.doubleValue("rareRateDrift"),
        uniqueRelativeError = getValue("distributionError").jsonObject.doubleValue("uniqueRelativeError"),
        artifactRelativeError = getValue("distributionError").jsonObject.doubleValue("artifactRelativeError"),
        failedExpectationCount = intValue("failedExpectationCount"),
        affixCostHistogram = getValue("affixCostHistogram").jsonObject.toIntMap(),
        topAffixIds = getValue("topAffixIds").jsonObject.toIntMap(),
        pityTimeline = getValue("pityTimeline").jsonArray.map { event -> event.jsonObject.toLootPityEvent() },
        castSpeedSamples = getValue("castSpeedSamples").jsonArray.map { sample -> sample.jsonObject.toLootCastSpeedSample() },
        sampleRolls = getValue("sampleRolls").jsonArray.map { sample -> sample.jsonObject.toLootRollSample() },
        uniqueArtifactOutcomeCount = intValue("uniqueArtifactOutcomeCount"),
        meaningfulUniqueArtifactSwapCount = intValue("meaningfulUniqueArtifactSwapCount"),
        meaningfulUniqueArtifactSwapRate = doubleValue("meaningfulUniqueArtifactSwapRate"),
    )
}

private fun JsonObject.toLootExpectation(): LootExpectation {
    val finalRates = toDoubleMap().filterKeys { key -> key != "noUpgradeRate" }
    val uniqueRate = finalRates["UNIQUE"] ?: 0.0
    val artifactRate = finalRates["ARTIFACT"] ?: 0.0
    return LootExpectation(
        finalRates = finalRates,
        magicRate = finalRates["MAGIC"] ?: 0.0,
        rareRate = finalRates["RARE"] ?: 0.0,
        uniqueRate = uniqueRate,
        artifactRate = artifactRate,
        noUpgradeRate = doubleValue("noUpgradeRate"),
    )
}

private fun JsonObject.toLootPityEvent(): LootPityEvent =
    LootPityEvent(
        rollIndex = intValue("rollIndex"),
        kind = stringValue("kind"),
        finalTier = stringValue("finalTier"),
        specialTemplateId = nullableString("specialTemplateId"),
        beforeValue = intValue("beforeValue"),
        afterValue = intValue("afterValue"),
    )

private fun JsonObject.toLootCastSpeedSample(): LootCastSpeedSample =
    LootCastSpeedSample(
        rollIndex = intValue("rollIndex"),
        rawCastSpeedRating = intValue("rawCastSpeedRating"),
        effectiveCastSpeed = doubleValue("effectiveCastSpeed"),
        finalTier = stringValue("finalTier"),
        specialTemplateId = nullableString("specialTemplateId"),
    )

private fun JsonObject.toLootRollSample(): LootRollSample =
    LootRollSample(
        matrixId = stringValue("matrixId"),
        rollIndex = intValue("rollIndex"),
        seed = longValue("seed"),
        sourceLevel = intValue("sourceLevel"),
        sourceTier = SourceTier.valueOf(stringValue("sourceTier")),
        zoneId = stringValue("zoneId"),
        playerLevel = intValue("playerLevel"),
        magicFind = floatValue("magicFind"),
        effectiveMagicFind = floatValue("effectiveMagicFind"),
        rolledRarityTier = RarityTier.valueOf(stringValue("rolledRarityTier")),
        resolvedRarityTier = RarityTier.valueOf(stringValue("resolvedRarityTier")),
        finalTier = stringValue("finalTier"),
        specialTier = nullableString("specialTier")?.let(SpecialTier::valueOf),
        specialTemplateId = nullableString("specialTemplateId"),
        generatedBaseItemId = stringValue("generatedBaseItemId"),
        itemType = ItemType.valueOf(stringValue("itemType")),
        equipSlot = nullableString("equipSlot")?.let(EquipSlot::valueOf),
        specialOrBasePassiveSignatures = getValue("specialOrBasePassiveSignatures").jsonArray.map { it.jsonPrimitive.content }.toSet(),
        affixPassiveSignatures = getValue("affixPassiveSignatures").jsonArray.map { it.jsonPrimitive.content }.toSet(),
        meaningfulUniqueArtifactSwap = nullableBoolean("meaningfulUniqueArtifactSwap"),
        specialTierEligibilityCount = intValue("specialTierEligibilityCount"),
        rarePityApplied = booleanValue("rarePityApplied"),
        specialPityApplied = booleanValue("specialPityApplied"),
        affixBudget = intValue("affixBudget"),
        affixBudgetTarget = intValue("affixBudgetTarget"),
        affixBudgetConsumed = intValue("affixBudgetConsumed"),
        affixBudgetDeviationRatio = nullableDouble("affixBudgetDeviationRatio"),
        rawAffixBudgetShortfall = intValue("rawAffixBudgetShortfall"),
        rawCastSpeedRating = intValue("rawCastSpeedRating"),
        effectiveCastSpeed = doubleValue("effectiveCastSpeed"),
        affixCostBreakdown =
            getValue("affixCostBreakdown").jsonArray.map { entry ->
                entry.jsonObject.stringValue("affixId") to entry.jsonObject.intValue("cost")
            },
        previousRarePity = getValue("pityBefore").jsonObject.intValue("rollsSinceLastRare"),
        previousSpecialPity = getValue("pityBefore").jsonObject.intValue("eligibleSpecialRollsSinceLastUnique"),
        resultingRarePity = getValue("pityAfter").jsonObject.intValue("rollsSinceLastRare"),
        resultingSpecialPity = getValue("pityAfter").jsonObject.intValue("eligibleSpecialRollsSinceLastUnique"),
    )

private fun JsonObject.toLootMatrixKernelShard(): LootMatrixKernelShard =
    LootMatrixKernelShard(
        shardId = stringValue("shardId"),
        matrixId = stringValue("matrixId"),
        rollStartInclusive = intValue("rollStartInclusive"),
        rollCount = intValue("rollCount"),
        startingPityTracker =
            getValue("startingPityTracker").jsonObject.let { pity ->
                com.ktome.core.loot.PityTracker(
                    rollsSinceLastRare = pity.intValue("rollsSinceLastRare"),
                    eligibleSpecialRollsSinceLastUnique = pity.intValue("eligibleSpecialRollsSinceLastUnique"),
                )
            },
        resultingPityTracker =
            getValue("resultingPityTracker").jsonObject.let { pity ->
                com.ktome.core.loot.PityTracker(
                    rollsSinceLastRare = pity.intValue("rollsSinceLastRare"),
                    eligibleSpecialRollsSinceLastUnique = pity.intValue("eligibleSpecialRollsSinceLastUnique"),
                )
            },
        availableSpecialTiers = getValue("availableSpecialTiers").jsonArray.map { tier -> SpecialTier.valueOf(tier.jsonPrimitive.content) }.toSet(),
        finalTierCounts = getValue("finalTierCounts").jsonObject.toIntMap(),
        baseRarityCounts = getValue("baseRarityCounts").jsonObject.toIntMap(),
        affixDeviationRatios = getValue("affixDeviationRatios").jsonArray.map { ratio -> ratio.jsonPrimitive.content.toDouble() },
        castSpeedValues = getValue("castSpeedValues").jsonArray.map { value -> value.jsonPrimitive.content.toDouble() },
        affixCostHistogram = getValue("affixCostHistogram").jsonObject.toIntMap(),
        affixIdCounts = getValue("affixIdCounts").jsonObject.toIntMap(),
        pityTimeline = getValue("pityTimeline").jsonArray.map { event -> event.jsonObject.toLootPityEvent() },
        castSpeedSamples = getValue("castSpeedSamples").jsonArray.map { sample -> sample.jsonObject.toLootCastSpeedSample() },
        sampleRolls = getValue("sampleRolls").jsonArray.map { sample -> sample.jsonObject.toLootRollSample() },
        eligibleCount = intValue("eligibleCount"),
        uniqueCount = intValue("uniqueCount"),
        artifactCount = intValue("artifactCount"),
        uniqueArtifactOutcomeCount = intValue("uniqueArtifactOutcomeCount"),
        meaningfulUniqueArtifactSwapCount = intValue("meaningfulUniqueArtifactSwapCount"),
        rarePityActivations = intValue("rarePityActivations"),
        uniquePityActivations = intValue("uniquePityActivations"),
    )

private fun JsonObject.toLootProfileOverlapSummary(): LootProfileOverlapSummary =
    LootProfileOverlapSummary(
        overlapMatrix =
            getValue("matrix").jsonObject.mapValues { (_, row) ->
                row.jsonObject.toDoubleMap()
            },
        averageOverlap = doubleValue("averageOverlap"),
        maxOverlap = doubleValue("maxOverlap"),
        distinctBaseItemCount = intValue("distinctBaseItemCount"),
        sameZoneSecretVsCadencePairs =
            getValue("sameZoneSecretVsCadencePairs").jsonArray.map { pair ->
                pair.jsonObject.toLootLocalOverlapPairSummary()
            },
        sameZoneSecretVsRewardPairs =
            getValue("sameZoneSecretVsRewardPairs").jsonArray.map { pair ->
                pair.jsonObject.toLootLocalOverlapPairSummary()
            },
        localIdentityFailurePairs = getValue("localIdentityFailurePairs").jsonArray.map { pair -> pair.jsonPrimitive.content },
        strictLocalIdentityViolations =
            this["strictLocalIdentityViolations"]?.jsonArray?.map { violation ->
                violation.jsonObject.toLootStrictLocalIdentityViolation()
            }.orEmpty(),
        secretProfileIdentitySummaries =
            this["secretProfileIdentitySummaries"]?.jsonArray?.map { summary ->
                summary.jsonObject.toLootSecretProfileIdentitySummary()
            }.orEmpty(),
    )

private fun JsonObject.toLootLocalOverlapPairSummary(): LootLocalOverlapPairSummary =
    LootLocalOverlapPairSummary(
        zoneId = stringValue("zoneId"),
        pairType = stringValue("pairType"),
        secretProfileId = stringValue("secretProfileId"),
        comparedProfileId = stringValue("comparedProfileId"),
        overlap = doubleValue("overlap"),
    )

private fun JsonObject.toLootSecretProfileIdentitySummary(): LootSecretProfileIdentitySummary =
    LootSecretProfileIdentitySummary(
        profileId = stringValue("profileId"),
        canonicalZoneId = stringValue("canonicalZoneId"),
        poolStrategy = stringValue("poolStrategy"),
        identityAxes = getValue("identityAxes").jsonArray.map { axis -> axis.jsonPrimitive.content },
        rewardStructureKeys = this["rewardStructureKeys"]?.jsonArray?.map { rewardKey -> rewardKey.jsonPrimitive.content }.orEmpty(),
        fixedItemIds = getValue("fixedItemIds").jsonArray.map { itemId -> itemId.jsonPrimitive.content },
        candidateBaseIds = getValue("candidateBaseIds").jsonArray.map { baseId -> baseId.jsonPrimitive.content },
        typeWeights = getValue("typeWeights").jsonObject.toIntMap(),
        slotBias = getValue("slotBias").jsonObject.toIntMap(),
        specialTemplateTagPreference = getValue("specialTemplateTagPreference").jsonArray.map { tag -> tag.jsonPrimitive.content },
        affixTagPreference = getValue("affixTagPreference").jsonArray.map { tag -> tag.jsonPrimitive.content },
        sameZoneCadenceMaxOverlap = this["sameZoneCadenceMaxOverlap"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
        sameZoneRewardMaxOverlap = this["sameZoneRewardMaxOverlap"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
        strictAllowedMaxOverlap = this["strictAllowedMaxOverlap"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
        strictViolationPairIds = getValue("strictViolationPairIds").jsonArray.map { pairId -> pairId.jsonPrimitive.content },
    )

private fun JsonObject.toLootPassiveCoverageSummary(): LootPassiveCoverageSummary =
    LootPassiveCoverageSummary(
        passiveKinds = getValue("passiveKinds").jsonArray.map { passive -> passive.jsonPrimitive.content }.toSet(),
        coverageRatio = doubleValue("coverageRatio"),
    )

private fun JsonObject.toLootSpecialPoolSummary(): LootSpecialPoolSummary =
    LootSpecialPoolSummary(
        affixCount = intValue("affixCount"),
        uniqueTemplateCount = intValue("uniqueTemplateCount"),
        artifactTemplateCount = intValue("artifactTemplateCount"),
        totalCount = intValue("totalCount"),
        targetZoneCoverageCount = intValue("targetZoneCoverageCount"),
        buildArchetypeCount = intValue("buildArchetypeCount"),
        bossOnlyArtifactTemplateCount = intValue("bossOnlyArtifactTemplateCount"),
        chestOnlyArtifactTemplateCount = intValue("chestOnlyArtifactTemplateCount"),
        secretZoneArtifactTemplateCount = intValue("secretZoneArtifactTemplateCount"),
        passesThresholds = booleanValue("passesThresholds"),
    )

private fun JsonObject.toLootClampComparison(): LootClampComparison =
    LootClampComparison(
        referenceMatrixId = stringValue("referenceMatrixId"),
        overflowMatrixId = stringValue("overflowMatrixId"),
        tierDelta = getValue("tierDelta").jsonObject.toDoubleMap(),
        maxDistributionDelta = doubleValue("maxDistributionDelta"),
        withinTolerance = booleanValue("withinTolerance"),
    )

private fun JsonObject.toDoubleMap(): Map<String, Double> = entries.associate { (key, value) -> key to value.jsonPrimitive.content.toDouble() }

private fun JsonObject.toIntMap(): Map<String, Int> = entries.associate { (key, value) -> key to value.jsonPrimitive.content.toInt() }

private fun JsonObject.intValue(key: String): Int = getValue(key).jsonPrimitive.content.toInt()

private fun JsonObject.longValue(key: String): Long = getValue(key).jsonPrimitive.content.toLong()

private fun JsonObject.floatValue(key: String): Float = getValue(key).jsonPrimitive.content.toFloat()

private fun JsonObject.doubleValue(key: String): Double = getValue(key).jsonPrimitive.content.toDouble()

private fun JsonObject.stringValue(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.booleanValue(key: String): Boolean = getValue(key).jsonPrimitive.content.toBooleanStrict()

private fun JsonObject.nullableString(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

private fun JsonObject.nullableDouble(key: String): Double? = this[key]?.jsonPrimitive?.contentOrNull?.toDouble()

private fun JsonObject.nullableBoolean(key: String): Boolean? = this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrict()
