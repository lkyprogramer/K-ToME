package com.ktome.core.combat

import com.ktome.core.random.RandomSource

data class CritRoll(
    val effectiveCritChance: Double,
    val roll: Double,
    val isCritical: Boolean,
)

object CritFormula {
    const val BASE_CRIT_RATE: Double = 0.05
    const val MAX_CRIT_RATE: Double = 0.50
    const val BASE_CRIT_MULTIPLIER: Double = 1.5

    fun effectiveCritChance(
        baseCritRate: Double,
        critBonus: Double = 0.0,
        critResistance: Double = 0.0,
    ): Double = (baseCritRate + critBonus - critResistance).coerceIn(0.0, MAX_CRIT_RATE)

    fun rollCrit(
        random: RandomSource,
        baseCritRate: Double,
        critBonus: Double = 0.0,
        critResistance: Double = 0.0,
    ): CritRoll {
        val effectiveChance = effectiveCritChance(baseCritRate, critBonus, critResistance)
        val roll = random.nextDouble()
        return CritRoll(
            effectiveCritChance = effectiveChance,
            roll = roll,
            isCritical = roll < effectiveChance,
        )
    }
}
