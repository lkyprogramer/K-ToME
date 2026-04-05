package com.ktome.core.loot

import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.random.RandomSource
import kotlin.math.roundToInt

private const val MAX_MAGIC_FIND_BONUS: Float = 1.0f
private const val MAGIC_FIND_RARITY_MULTIPLIER: Float = 0.50f
private const val MAGIC_RARITY_SCORE_MULTIPLIER: Float = 0.50f
private const val RARE_PITY_THRESHOLD: Int = 20
private const val RARE_PITY_MULTIPLIER: Float = 2.0f
private const val SPECIAL_PITY_THRESHOLD: Int = 50
private const val UNIQUE_PITY_MULTIPLIER: Float = 3.0f

class LootBudgetResolver(
    private val random: RandomSource,
) {
    fun roll(
        context: LootRollContext,
        zoneRewardProfile: ZoneRewardProfile,
        specialTierEligibility: SpecialTierEligibility = defaultSpecialTierEligibility(context.sourceTier),
        pityTracker: PityTracker = PityTracker(),
        minimumRarityTier: RarityTier? = null,
    ): LootRollResult {
        require(zoneRewardProfile.zoneId == context.zoneId) {
            "ZoneRewardProfile '${zoneRewardProfile.id}' belongs to '${zoneRewardProfile.zoneId}', but context requested '${context.zoneId}'."
        }

        val iLvl = resolveItemLevel(context)
        val rarityScore = resolveRarityScore(context, zoneRewardProfile)
        val rarePityApplied = pityTracker.rollsSinceLastRare >= RARE_PITY_THRESHOLD
        val rolledRarityTier = rollBaseRarity(rarityScore = rarityScore, rarePityApplied = rarePityApplied)
        val resolvedRarityTier = minimumRarityTier?.takeIf { tier -> tier.ordinal > rolledRarityTier.ordinal } ?: rolledRarityTier

        val budget =
            LootBudget(
                iLvl = iLvl,
                qLvl = resolveQualityLevel(iLvl = iLvl, rarityTier = resolvedRarityTier, zoneRewardProfile = zoneRewardProfile),
                rarityTier = resolvedRarityTier,
                rarityScore = rarityScore,
                affixBudget = resolveAffixBudget(iLvl = iLvl, rarityTier = resolvedRarityTier, zoneRewardProfile = zoneRewardProfile, sourceTier = context.sourceTier),
                specialTierEligibility = specialTierEligibility,
            )

        val specialPityApplied =
            specialTierEligibility.availableSpecialTiers.isNotEmpty() &&
                pityTracker.eligibleSpecialRollsSinceLastUnique >= SPECIAL_PITY_THRESHOLD
        val upgradedSpecialTier =
            rollSpecialTier(
                specialTierEligibility = specialTierEligibility,
                specialPityApplied = specialPityApplied,
            )
        val resultingPityTracker =
            nextPityTracker(
                previous = pityTracker,
                resolvedRarityTier = resolvedRarityTier,
                specialTierEligibility = specialTierEligibility,
                upgradedSpecialTier = upgradedSpecialTier,
            )

        return LootRollResult(
            context = context,
            budget = budget,
            rolledRarityTier = rolledRarityTier,
            upgradedSpecialTier = upgradedSpecialTier,
            specialUpgradeAttempted = specialTierEligibility.availableSpecialTiers.isNotEmpty(),
            rarePityApplied = rarePityApplied,
            specialPityApplied = specialPityApplied,
            resultingPityTracker = resultingPityTracker,
        )
    }

    private fun resolveItemLevel(context: LootRollContext): Int {
        val upwardVariance = random.nextInt(fromInclusive = 0, untilExclusive = 2)
        val unclampedLevel = context.sourceLevel + context.sourceTier.itemLevelBonus + upwardVariance
        return unclampedLevel.coerceIn(1, context.playerLevel + 3)
    }

    private fun resolveRarityScore(
        context: LootRollContext,
        zoneRewardProfile: ZoneRewardProfile,
    ): Float {
        val effectiveMagicFind = context.magicFindBonus.coerceIn(0.0f, MAX_MAGIC_FIND_BONUS)
        return context.sourceTier.rarityBonus +
            zoneRewardProfile.rarityBonus +
            effectiveMagicFind * MAGIC_FIND_RARITY_MULTIPLIER
    }

    private fun resolveQualityLevel(
        iLvl: Int,
        rarityTier: RarityTier,
        zoneRewardProfile: ZoneRewardProfile,
    ): Int {
        val raw = iLvl + rarityTier.qualityBonus + zoneRewardProfile.qualityBonus
        return raw.coerceIn(iLvl, iLvl + 6)
    }

    private fun resolveAffixBudget(
        iLvl: Int,
        rarityTier: RarityTier,
        zoneRewardProfile: ZoneRewardProfile,
        sourceTier: SourceTier,
    ): Int {
        val qLvl = resolveQualityLevel(iLvl = iLvl, rarityTier = rarityTier, zoneRewardProfile = zoneRewardProfile)
        return qLvl * 2 + rarityTier.baseBudget + sourceTier.affixBudgetBonus
    }

    private fun rollBaseRarity(
        rarityScore: Float,
        rarePityApplied: Boolean,
    ): RarityTier {
        val normalWeight = RarityTier.NORMAL.baseWeight
        val magicWeight = scaledWeight(RarityTier.MAGIC.baseWeight, 1.0f + rarityScore * MAGIC_RARITY_SCORE_MULTIPLIER)
        val rareWeightBase = scaledWeight(RarityTier.RARE.baseWeight, 1.0f + rarityScore)
        val rareWeight =
            if (rarePityApplied) {
                scaledWeight(rareWeightBase, RARE_PITY_MULTIPLIER)
            } else {
                rareWeightBase
            }

        var roll = random.nextInt(fromInclusive = 0, untilExclusive = normalWeight + magicWeight + rareWeight)
        roll -= normalWeight
        if (roll < 0) {
            return RarityTier.NORMAL
        }
        roll -= magicWeight
        if (roll < 0) {
            return RarityTier.MAGIC
        }
        return RarityTier.RARE
    }

    private fun rollSpecialTier(
        specialTierEligibility: SpecialTierEligibility,
        specialPityApplied: Boolean,
    ): SpecialTier? {
        if (specialTierEligibility.availableSpecialTiers.isEmpty()) {
            return null
        }
        if (specialTierEligibility.availableTemplateIds.isEmpty()) {
            return null
        }

        val uniqueWeight =
            if (SpecialTier.UNIQUE in specialTierEligibility.availableSpecialTiers) {
                if (specialPityApplied) {
                    scaledWeight(baseWeight = 1, multiplier = UNIQUE_PITY_MULTIPLIER)
                } else {
                    1
                }
            } else {
                0
            }
        val artifactWeight =
            if (SpecialTier.ARTIFACT in specialTierEligibility.availableSpecialTiers) {
                1
            } else {
                0
            }
        val totalWeight = uniqueWeight + artifactWeight
        if (totalWeight == 0) {
            return null
        }

        val roll = random.nextInt(fromInclusive = 0, untilExclusive = totalWeight)
        return if (roll < uniqueWeight) {
            SpecialTier.UNIQUE
        } else {
            SpecialTier.ARTIFACT
        }
    }

    private fun nextPityTracker(
        previous: PityTracker,
        resolvedRarityTier: RarityTier,
        specialTierEligibility: SpecialTierEligibility,
        upgradedSpecialTier: SpecialTier?,
    ): PityTracker {
        val nextRareCounter =
            if (resolvedRarityTier == RarityTier.RARE) {
                0
            } else {
                previous.rollsSinceLastRare + 1
            }
        val nextSpecialCounter =
            when {
                SpecialTier.UNIQUE == upgradedSpecialTier || SpecialTier.ARTIFACT == upgradedSpecialTier -> 0
                specialTierEligibility.availableSpecialTiers.isNotEmpty() -> previous.eligibleSpecialRollsSinceLastUnique + 1
                else -> previous.eligibleSpecialRollsSinceLastUnique
            }
        return PityTracker(
            rollsSinceLastRare = nextRareCounter,
            eligibleSpecialRollsSinceLastUnique = nextSpecialCounter,
        )
    }

    private fun scaledWeight(
        baseWeight: Int,
        multiplier: Float,
    ): Int = (baseWeight.toFloat() * multiplier).roundToInt().coerceAtLeast(1)

    companion object {
        fun defaultSpecialTierEligibility(sourceTier: SourceTier): SpecialTierEligibility =
            when (sourceTier) {
                SourceTier.NORMAL -> SpecialTierEligibility(availableSpecialTiers = emptySet())
                SourceTier.ELITE -> SpecialTierEligibility(availableSpecialTiers = setOf(SpecialTier.UNIQUE))
                SourceTier.BOSS -> SpecialTierEligibility(availableSpecialTiers = setOf(SpecialTier.UNIQUE, SpecialTier.ARTIFACT))
                SourceTier.CHEST -> SpecialTierEligibility(availableSpecialTiers = setOf(SpecialTier.UNIQUE))
            }
    }
}
