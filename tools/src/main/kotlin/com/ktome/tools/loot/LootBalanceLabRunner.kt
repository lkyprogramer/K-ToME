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
import com.ktome.game.data.schema.LootProfileSchemaV3
import com.ktome.game.i18n.GameLocale
import com.ktome.game.loot.LootProfileCandidatePoolResolver
import com.ktome.game.mapgen.SchemaZoneRewardProfileResolver
import com.ktome.tools.mapgen.phase4HarnessHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
)

internal data class LootCastSpeedSample(
    val rollIndex: Int,
    val rawCastSpeedRating: Int,
    val effectiveCastSpeed: Double,
    val finalTier: String,
    val specialTemplateId: String?,
)

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
            put("sourceLevel", spec.sourceLevel)
            put("sourceTier", spec.sourceTier.name)
            put("sourceDescriptor", spec.sourceDescriptor)
            put("zoneId", spec.zoneId)
            put("playerLevel", spec.playerLevel)
            put("magicFind", spec.magicFind)
            put("effectiveMagicFind", spec.magicFind.coerceIn(0.0f, 1.0f))
            put("rollCount", totalRolls)
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
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("averageOverlap", averageOverlap)
            put("maxOverlap", maxOverlap)
            put("distinctBaseItemCount", distinctBaseItemCount)
            putJsonObject("matrix") {
                overlapMatrix.toSortedMap().forEach { (profileId, row) ->
                    putJsonObject(profileId) {
                        row.toSortedMap().forEach { (candidateId, overlap) ->
                            put(candidateId, overlap)
                        }
                    }
                }
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

private const val ROLLS_PER_MATRIX: Int = 10_000
private const val MAGIC_RARE_ABSOLUTE_DRIFT_TOLERANCE: Double = 0.05
private const val SPECIAL_RELATIVE_ERROR_TOLERANCE: Double = 0.25
private const val AFFIX_BUDGET_AVERAGE_TOLERANCE: Double = 0.05
private const val AFFIX_BUDGET_P95_TOLERANCE: Double = 0.12
private const val CLAMP_DISTRIBUTION_TOLERANCE: Double = 0.02
internal val LOOT_REPORT_LOCALE: GameLocale = GameLocale.EN_US
private const val EQUIPMENT_PASSIVE_KIND_COUNT: Int = 9

internal object LootLabKernel {
    private val json: Json = Json { prettyPrint = true }
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

    fun execute(onRoll: (LootRollSample) -> Unit = {}): LootKernelRun {
        val loader = DataLoader(LOOT_REPORT_LOCALE)
        val schemaCatalog = loader.loadSchemaCatalog()
        val itemBundle = loader.loadItemBundle()
        val rewardResolver = SchemaZoneRewardProfileResolver(schemaCatalog.zones, schemaCatalog.zoneRewardProfiles)
        val rarePassiveUniverseBySlot = buildRarePassiveUniverseBySlot(itemBundle)
        val matrices =
            matrixSpecs.map { spec ->
                executeMatrix(
                    spec = spec,
                    zoneRewardProfile = rewardResolver.resolve(spec.zoneId),
                    itemBundleLoader = { itemBundle },
                    rarePassiveUniverseBySlot = rarePassiveUniverseBySlot,
                    onRoll = onRoll,
                )
            }
        val specialPoolSummary = summarizeSpecialPool(itemBundleLoader = { itemBundle })
        val clampComparison = compareClampBoundary(matrices)
        return LootKernelRun(
            matrices = matrices,
            specialPoolSummary = specialPoolSummary,
            clampComparison = clampComparison,
            matrixSeeds = matrixSpecs.map(LootMatrixSpec::seedBase),
            profileOverlapSummary = summarizeLootProfileOverlap(schemaCatalog.lootProfiles, itemBundle),
            passiveCoverageSummary = summarizeAffixPassiveCoverage(itemBundle),
        )
    }

    private fun executeMatrix(
        spec: LootMatrixSpec,
        zoneRewardProfile: ZoneRewardProfile,
        itemBundleLoader: () -> com.ktome.core.item.ItemDataBundle,
        rarePassiveUniverseBySlot: Map<EquipSlot, Set<String>>,
        onRoll: (LootRollSample) -> Unit,
    ): LootMatrixResult {
        val finalTierCounts = linkedMapOf("NORMAL" to 0, "MAGIC" to 0, "RARE" to 0, "UNIQUE" to 0, "ARTIFACT" to 0)
        val baseRarityCounts = linkedMapOf("NORMAL" to 0, "MAGIC" to 0, "RARE" to 0)
        val affixDeviationRatios = mutableListOf<Double>()
        val castSpeedValues = mutableListOf<Double>()
        val affixCostHistogram = linkedMapOf<String, Int>()
        val affixIdCounts = linkedMapOf<String, Int>()
        val pityTimeline = mutableListOf<LootPityEvent>()
        val castSpeedSamples = mutableListOf<LootCastSpeedSample>()
        val sampleRolls = mutableListOf<LootRollSample>()
        var availableSpecialTiers: Set<SpecialTier> = emptySet()
        var eligibleCount = 0
        var uniqueCount = 0
        var artifactCount = 0
        var uniqueArtifactOutcomeCount = 0
        var meaningfulUniqueArtifactSwapCount = 0
        var rarePityActivations = 0
        var uniquePityActivations = 0
        var pityTracker = com.ktome.core.loot.PityTracker()

        repeat(spec.rollCount) { rollIndex ->
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
            availableSpecialTiers =
                availableSpecialTiers + generated.rollResult.budget.specialTierEligibility.availableSpecialTiers
            pityTracker = generated.rollResult.resultingPityTracker
            onRoll(sample)

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
                sampleRolls.size < 8 ||
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

        val expected =
            expectedDistribution(
                spec = spec,
                zoneRewardProfile = zoneRewardProfile,
                eligibleCount = eligibleCount,
                totalRolls = spec.rollCount,
                availableSpecialTiers = availableSpecialTiers,
            )
        val rarityDistribution = finalTierCounts.mapValues { (_, count) -> count.toDouble() / spec.rollCount.toDouble() }
        val baseDistribution = baseRarityCounts.mapValues { (_, count) -> count.toDouble() / spec.rollCount.toDouble() }
        val magicRateDrift = abs(rarityDistribution.getValue("MAGIC") - expected.magicRate)
        val rareRateDrift = abs(rarityDistribution.getValue("RARE") - expected.rareRate)
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
            specialTierEligibilityRate = eligibleCount.toDouble() / spec.rollCount.toDouble(),
            rarePityActivations = rarePityActivations,
            uniquePityActivations = uniquePityActivations,
            castSpeedPostDrP50 = castSpeedPostDrP50,
            castSpeedPostDrP95 = castSpeedPostDrP95,
            magicRateDrift = magicRateDrift,
            rareRateDrift = rareRateDrift,
            uniqueRelativeError = uniqueRelativeError,
            artifactRelativeError = artifactRelativeError,
            failedExpectationCount = failedExpectationCount,
            affixCostHistogram = affixCostHistogram.toSortedMap(),
            topAffixIds = affixIdCounts.entries.sortedByDescending(Map.Entry<String, Int>::value).take(8).associate { it.key to it.value },
            pityTimeline = pityTimeline.take(20),
            castSpeedSamples = castSpeedSamples.sortedByDescending(LootCastSpeedSample::rawCastSpeedRating).take(10),
            sampleRolls = sampleRolls.take(16),
            uniqueArtifactOutcomeCount = uniqueArtifactOutcomeCount,
            meaningfulUniqueArtifactSwapCount = meaningfulUniqueArtifactSwapCount,
            meaningfulUniqueArtifactSwapRate =
                if (uniqueArtifactOutcomeCount == 0) {
                    0.0
                } else {
                    meaningfulUniqueArtifactSwapCount.toDouble() / uniqueArtifactOutcomeCount.toDouble()
                },
        )
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
        profiles: List<LootProfileSchemaV3>,
        itemBundle: ItemDataBundle,
    ): LootProfileOverlapSummary {
        val resolver = LootProfileCandidatePoolResolver(itemBundle)
        val candidateBaseIdsByProfileId =
            profiles.associate { profile ->
                profile.id to resolver.resolve(profile).allCandidateBaseIds
            }
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
        val distinctBaseItemCount =
            candidateBaseIdsByProfileId.values
                .flatMapTo(linkedSetOf()) { candidateBaseIds -> candidateBaseIds }
                .size
        return LootProfileOverlapSummary(
            overlapMatrix = overlapMatrix,
            averageOverlap = overlapValues.averageOrZero(),
            maxOverlap = overlapValues.maxOrNull() ?: 0.0,
            distinctBaseItemCount = distinctBaseItemCount,
        )
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
    private val compactJson: Json = Json { prettyPrint = false }

    fun run(): LootBalanceLabRun {
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val rollsPath = outputDir.resolve(ROLLS_FILE)
        Files.deleteIfExists(rollsPath)
        Files.createFile(rollsPath)

        val kernelRun =
            Files.newBufferedWriter(rollsPath).use { writer ->
                LootLabKernel.execute { sample ->
                    writer.appendLine(compactJson.encodeToString(JsonElement.serializer(), sample.toJson()))
                }
            }
        val header = phase4HarnessHeader(harnessId = HARNESS_ID, seedList = kernelRun.matrixSeeds, locale = LOOT_REPORT_LOCALE.id)
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        Files.writeString(summaryPath, json.encodeToString(JsonElement.serializer(), buildSummaryPayload(header, kernelRun)))
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
    ): JsonObject =
        buildJsonObject {
            put("header", header.toJson())
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
            putJsonArray("matrices") {
                kernelRun.matrices.forEach { matrix -> add(matrix.toJson()) }
            }
        }

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.loot.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4", "loot")
        } else {
            Path.of(configured)
        }
    }
}
