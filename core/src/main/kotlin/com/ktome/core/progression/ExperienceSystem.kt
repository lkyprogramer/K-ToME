package com.ktome.core.progression

import com.ktome.core.ecs.Experience

data class ExperienceGainResult(
    val experience: Experience,
    val levelsGained: Int,
    val gainedStatPoints: Int,
    val gainedTalentPoints: Int,
    val shouldRestoreHealthToMax: Boolean,
    val shouldRestorePrimaryResourceToMax: Boolean,
)

object ExperienceSystem {
    fun nextLevelExp(level: Int): Int = level * 100 + 50

    fun applyReward(
        experience: Experience,
        reward: Int,
    ): ExperienceGainResult {
        require(reward >= 0) { "reward must be non-negative." }

        var totalLevelsGained = 0
        var totalStatPoints = 0
        var totalTalentPoints = 0
        var restored = false

        experience.current += reward
        while (experience.level < 20 && experience.current >= nextLevelExp(experience.level)) {
            experience.current -= nextLevelExp(experience.level)
            experience.level += 1
            experience.unspentStatPoints += 2
            totalLevelsGained += 1
            totalStatPoints += 2

            if (experience.level % 2 == 1) {
                experience.unspentTalentPoints += 1
                totalTalentPoints += 1
            }
        }

        restored = totalLevelsGained > 0
        return ExperienceGainResult(
            experience = experience,
            levelsGained = totalLevelsGained,
            gainedStatPoints = totalStatPoints,
            gainedTalentPoints = totalTalentPoints,
            shouldRestoreHealthToMax = restored,
            shouldRestorePrimaryResourceToMax = restored,
        )
    }
}
