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
    private const val LEVEL_CAP: Int = 20
    private const val STAT_POINTS_PER_LEVEL: Int = 2

    fun nextLevelExp(level: Int): Int = level * 100 + 50

    fun statPointsGrantedForLevel(level: Int): Int {
        require(level in 2..LEVEL_CAP) { "Stat point grant is only defined for levels 2..$LEVEL_CAP, got $level." }
        return STAT_POINTS_PER_LEVEL
    }

    fun talentPointsGrantedForLevel(level: Int): Int {
        require(level in 2..LEVEL_CAP) { "Talent point grant is only defined for levels 2..$LEVEL_CAP, got $level." }
        return 1
    }

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
        while (experience.level < LEVEL_CAP && experience.current >= nextLevelExp(experience.level)) {
            experience.current -= nextLevelExp(experience.level)
            experience.level += 1
            val grantedStatPoints = statPointsGrantedForLevel(experience.level)
            experience.unspentStatPoints += grantedStatPoints
            totalLevelsGained += 1
            totalStatPoints += grantedStatPoints

            val grantedTalentPoints = talentPointsGrantedForLevel(experience.level)
            experience.unspentTalentPoints += grantedTalentPoints
            totalTalentPoints += grantedTalentPoints
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
