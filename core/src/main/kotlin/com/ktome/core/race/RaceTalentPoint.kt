package com.ktome.core.race

data class RaceTalentPointBank(
    var unspentPoints: Int = 0,
)

object RaceTalentPointProgression {
    private const val LEVEL_INTERVAL: Int = 4

    fun totalGrantedByLevel(level: Int): Int {
        require(level >= 1) { "Level must be >= 1." }
        return level / LEVEL_INTERVAL
    }

    fun deltaForLevelRange(
        previousLevel: Int,
        nextLevel: Int,
    ): Int {
        require(nextLevel >= previousLevel) { "nextLevel must be >= previousLevel." }
        return totalGrantedByLevel(nextLevel) - totalGrantedByLevel(previousLevel)
    }
}
