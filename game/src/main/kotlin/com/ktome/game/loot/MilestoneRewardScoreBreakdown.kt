package com.ktome.game.loot

data class MilestoneRewardScoreBreakdown(
    val baseScore: Int = 0,
    val professionCapstoneBonus: Int = 0,
    val nonWeaponPayoffBonus: Int = 0,
    val wrongProfessionCapstonePenalty: Int = 0,
    val slotRotationBonus: Int = 0,
    val duplicateSlotPenalty: Int = 0,
    val terminalIdentityBonus: Int = 0,
    val lateCommonPenalty: Int = 0,
) {
    val positiveBonusBeforeCap: Int
        get() =
            professionCapstoneBonus +
                nonWeaponPayoffBonus +
                slotRotationBonus +
                terminalIdentityBonus

    val positiveBonusCap: Int
        get() = ceilRatio(baseScore, 12, 10)

    val positiveBonusAfterCap: Int
        get() = positiveBonusBeforeCap.coerceAtMost(positiveBonusCap)

    val totalScore: Int
        get() =
            baseScore +
                positiveBonusAfterCap -
                wrongProfessionCapstonePenalty -
                duplicateSlotPenalty -
                lateCommonPenalty
}

internal fun ceilRatio(
    value: Int,
    numerator: Int,
    denominator: Int,
): Int =
    if (value <= 0) {
        0
    } else {
        (value * numerator + denominator - 1) / denominator
    }
