package com.ktome.core.combat

import kotlin.math.exp

object HitFormula {
    const val HIT_SIGMOID_K: Double = 0.04
    const val HIT_SIGMOID_M: Double = -10.0
    const val MIN_HIT_CHANCE: Double = 0.05
    const val MAX_HIT_CHANCE: Double = 0.95

    fun linearHitChance(
        accuracy: Int,
        evasion: Int,
    ): Double = linearHitChance(accuracy.toDouble(), evasion.toDouble())

    fun linearHitChance(
        accuracy: Double,
        evasion: Double,
    ): Double = (0.85 + (accuracy - evasion) * 0.01).coerceIn(MIN_HIT_CHANCE, MAX_HIT_CHANCE)

    fun sigmoidHitChance(
        accuracy: Int,
        evasion: Int,
    ): Double = sigmoidHitChance(accuracy.toDouble(), evasion.toDouble())

    fun sigmoidHitChance(
        accuracy: Int,
        evasion: Double,
    ): Double = sigmoidHitChance(accuracy.toDouble(), evasion)

    fun sigmoidHitChance(
        accuracy: Double,
        evasion: Double,
    ): Double {
        val delta = accuracy - evasion
        val sigmoid = 1.0 / (1.0 + exp(-HIT_SIGMOID_K * (delta - HIT_SIGMOID_M)))
        return (MIN_HIT_CHANCE + (MAX_HIT_CHANCE - MIN_HIT_CHANCE) * sigmoid)
            .coerceIn(MIN_HIT_CHANCE, MAX_HIT_CHANCE)
    }
}
