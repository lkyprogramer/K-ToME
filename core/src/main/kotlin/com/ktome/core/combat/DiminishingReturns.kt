package com.ktome.core.combat

object DiminishingReturns {
    const val DR_EVASION_C: Double = 150.0
    const val DR_CRIT_C: Double = 200.0
    const val DR_CAST_SPEED_C: Double = 100.0
    const val DR_HP_REGEN_C: Double = 80.0

    fun hyperbolic(
        rawValue: Double,
        halfValueConstant: Double,
    ): Double {
        require(halfValueConstant > 0.0) { "Half-value constant must be positive." }
        if (rawValue <= 0.0) {
            return 0.0
        }
        return rawValue * halfValueConstant / (rawValue + halfValueConstant)
    }

    fun effectiveEvasion(rawEvasion: Int): Double = hyperbolic(rawEvasion.toDouble(), DR_EVASION_C)

    fun effectiveCritRating(rawCritRating: Int): Double = hyperbolic(rawCritRating.toDouble(), DR_CRIT_C)

    fun effectiveCastSpeed(rawCastSpeed: Int): Double = hyperbolic(rawCastSpeed.toDouble(), DR_CAST_SPEED_C)

    fun effectiveHpRegen(rawHpRegen: Double): Double = hyperbolic(rawHpRegen, DR_HP_REGEN_C)

    fun adjustedCooldownTurns(
        baseCooldown: Int,
        effectiveCastSpeed: Double,
    ): Int {
        require(baseCooldown >= 0) { "Base cooldown must not be negative." }
        if (baseCooldown == 0) {
            return 0
        }
        val hasteMultiplier = 1.0 + (effectiveCastSpeed.coerceAtLeast(0.0) / 100.0)
        return kotlin.math.ceil(baseCooldown / hasteMultiplier).toInt().coerceAtLeast(1)
    }

    fun marginalValue(
        rawValue: Double,
        halfValueConstant: Double,
    ): Double = hyperbolic(rawValue + 1.0, halfValueConstant) - hyperbolic(rawValue, halfValueConstant)
}
