package com.ktome.game.loot

data class MilestoneRewardScoreBreakdown(
    val poolWeightScore: Int = 0,
    val freshBonus: Int = 0,
    val buildMatchScore: Int = 0,
    val exactProfessionScore: Int = 0,
    val professionCapstoneScore: Int = 0,
    val nonWeaponAnchorScore: Int = 0,
    val preferredRewardSourceScore: Int = 0,
    val routeBiasScore: Int = 0,
    val rewardBiasScore: Int = 0,
    val antiCollapsePenalty: Int = 0,
) {
    val totalScore: Int
        get() =
            poolWeightScore +
                freshBonus +
                buildMatchScore +
                exactProfessionScore +
                professionCapstoneScore +
                nonWeaponAnchorScore +
                preferredRewardSourceScore +
                routeBiasScore +
                rewardBiasScore -
                antiCollapsePenalty
}
