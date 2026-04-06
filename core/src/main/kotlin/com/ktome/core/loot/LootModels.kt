package com.ktome.core.loot

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Serializable
data class LootRollContext(
    val sourceLevel: Int,
    val sourceTier: SourceTier,
    val zoneId: String,
    val playerLevel: Int,
    val magicFindBonus: Float,
    val seed: Long,
) {
    init {
        require(sourceLevel > 0) { "LootRollContext.sourceLevel must be positive." }
        require(zoneId.isNotBlank()) { "LootRollContext.zoneId must not be blank." }
        require(playerLevel > 0) { "LootRollContext.playerLevel must be positive." }
    }
}

@Serializable
enum class SourceTier(
    val itemLevelBonus: Int,
    val rarityBonus: Float,
    val affixBudgetBonus: Int,
) {
    NORMAL(0, 0.00f, 0),
    ELITE(1, 0.15f, 2),
    BOSS(2, 0.40f, 4),
    CHEST(1, 0.10f, 1),
    SECRET_ZONE(1, 0.10f, 1),
}

@Serializable
enum class RarityTier(
    val baseWeight: Int,
    val qualityBonus: Int,
    val baseBudget: Int,
) {
    NORMAL(720, 0, 0),
    MAGIC(220, 1, 6),
    RARE(50, 3, 14),
}

@Serializable
enum class SpecialTier {
    UNIQUE,
    ARTIFACT,
}

@Serializable
data class SpecialTierEligibility(
    val availableSpecialTiers: Set<SpecialTier>,
    val availableTemplateIds: Set<String> = emptySet(),
) {
    init {
        require(availableTemplateIds.none(String::isBlank)) {
            "SpecialTierEligibility.availableTemplateIds must not contain blank ids."
        }
    }
}

@Serializable
data class LootBudget(
    val iLvl: Int,
    val qLvl: Int,
    val rarityTier: RarityTier,
    val rarityScore: Float,
    val affixBudget: Int,
    val specialTierEligibility: SpecialTierEligibility,
) {
    init {
        require(iLvl > 0) { "LootBudget.iLvl must be positive." }
        require(qLvl >= iLvl) { "LootBudget.qLvl must be greater than or equal to iLvl." }
        require(affixBudget >= 0) { "LootBudget.affixBudget must not be negative." }
    }
}

@Serializable
data class PityTracker(
    val rollsSinceLastRare: Int = 0,
    val eligibleSpecialRollsSinceLastUnique: Int = 0,
) {
    init {
        require(rollsSinceLastRare >= 0) { "PityTracker.rollsSinceLastRare must not be negative." }
        require(eligibleSpecialRollsSinceLastUnique >= 0) {
            "PityTracker.eligibleSpecialRollsSinceLastUnique must not be negative."
        }
    }
}

@Serializable
data class RewardDelta(
    val source: String,
    val amount: Int,
) {
    init {
        require(source.isNotBlank()) { "RewardDelta.source must not be blank." }
        require(amount >= 0) { "RewardDelta.amount must not be negative." }
    }
}

@Serializable
data class ThreatDelta(
    val source: String,
    val amount: Int,
) {
    init {
        require(source.isNotBlank()) { "ThreatDelta.source must not be blank." }
        require(amount >= 0) { "ThreatDelta.amount must not be negative." }
    }
}

@Serializable
data class FloorRewardBudget(
    val zoneId: String,
    val floorIndex: Int,
    val baseBudget: Int,
    val rewardDeltas: List<RewardDelta> = emptyList(),
) {
    init {
        require(zoneId.isNotBlank()) { "FloorRewardBudget.zoneId must not be blank." }
        require(floorIndex > 0) { "FloorRewardBudget.floorIndex must be positive." }
        require(baseBudget >= 0) { "FloorRewardBudget.baseBudget must not be negative." }
    }

    val totalBudget: Int
        get() = baseBudget + rewardDeltas.sumOf(RewardDelta::amount)
}

@Serializable
data class EncounterThreatBudget(
    val encounterId: String,
    val baseBudget: Int,
    val threatDeltas: List<ThreatDelta> = emptyList(),
) {
    init {
        require(encounterId.isNotBlank()) { "EncounterThreatBudget.encounterId must not be blank." }
        require(baseBudget >= 0) { "EncounterThreatBudget.baseBudget must not be negative." }
    }

    val totalBudget: Int
        get() = baseBudget + threatDeltas.sumOf(ThreatDelta::amount)
}

@Serializable
data class LootRollResult(
    val context: LootRollContext,
    val budget: LootBudget,
    val rolledRarityTier: RarityTier,
    val upgradedSpecialTier: SpecialTier? = null,
    val specialUpgradeAttempted: Boolean = false,
    val rarePityApplied: Boolean = false,
    val specialPityApplied: Boolean = false,
    val resultingPityTracker: PityTracker = PityTracker(),
) {
    val resolvedRarityTier: RarityTier
        get() = budget.rarityTier
}

fun LootRollContext.toJson(): JsonObject =
    buildJsonObject {
        put("sourceLevel", sourceLevel)
        put("sourceTier", sourceTier.name)
        put("zoneId", zoneId)
        put("playerLevel", playerLevel)
        put("magicFindBonus", magicFindBonus)
        put("seed", seed)
    }

fun SpecialTierEligibility.toJson(): JsonObject =
    buildJsonObject {
        putJsonArray("availableSpecialTiers") {
            availableSpecialTiers.sortedBy(SpecialTier::name).forEach { tier -> add(JsonPrimitive(tier.name)) }
        }
        putJsonArray("availableTemplateIds") {
            availableTemplateIds.sorted().forEach { templateId -> add(JsonPrimitive(templateId)) }
        }
    }

fun LootBudget.toJson(): JsonObject =
    buildJsonObject {
        put("iLvl", iLvl)
        put("qLvl", qLvl)
        put("rarityTier", rarityTier.name)
        put("rarityScore", rarityScore)
        put("affixBudget", affixBudget)
        put("specialTierEligibility", specialTierEligibility.toJson())
    }

fun PityTracker.toJson(): JsonObject =
    buildJsonObject {
        put("rollsSinceLastRare", rollsSinceLastRare)
        put("eligibleSpecialRollsSinceLastUnique", eligibleSpecialRollsSinceLastUnique)
    }

fun FloorRewardBudget.toJson(): JsonObject =
    buildJsonObject {
        put("zoneId", zoneId)
        put("floorIndex", floorIndex)
        put("baseBudget", baseBudget)
        put("totalBudget", totalBudget)
        putJsonArray("rewardDeltas") {
            rewardDeltas.sortedBy(RewardDelta::source).forEach { delta ->
                add(
                    buildJsonObject {
                        put("source", delta.source)
                        put("amount", delta.amount)
                    },
                )
            }
        }
    }

fun EncounterThreatBudget.toJson(): JsonObject =
    buildJsonObject {
        put("encounterId", encounterId)
        put("baseBudget", baseBudget)
        put("totalBudget", totalBudget)
        putJsonArray("threatDeltas") {
            threatDeltas.sortedBy(ThreatDelta::source).forEach { delta ->
                add(
                    buildJsonObject {
                        put("source", delta.source)
                        put("amount", delta.amount)
                    },
                )
            }
        }
    }

fun LootRollResult.toJson(): JsonObject =
    buildJsonObject {
        put("context", context.toJson())
        put("budget", budget.toJson())
        put("rolledRarityTier", rolledRarityTier.name)
        put("resolvedRarityTier", resolvedRarityTier.name)
        put("upgradedSpecialTier", upgradedSpecialTier?.name)
        put("specialUpgradeAttempted", specialUpgradeAttempted)
        put("rarePityApplied", rarePityApplied)
        put("specialPityApplied", specialPityApplied)
        put("resultingPityTracker", resultingPityTracker.toJson())
    }
