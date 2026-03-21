package com.ktome.core.combat

data class DamageResult(
    val type: DamageType,
    val rawDamage: Int,
    val reducedDamage: Int,
    val finalDamage: Int,
    val resistanceValue: Int = 0,
)

data class CombatResult(
    val hit: Boolean,
    val crit: Boolean,
    val damage: DamageResult? = null,
    val targetKilled: Boolean = false,
) {
    val rawDamage: Int
        get() = damage?.rawDamage ?: 0

    val reducedDamage: Int
        get() = damage?.reducedDamage ?: 0

    val finalDamage: Int
        get() = damage?.finalDamage ?: 0

    val critical: Boolean
        get() = crit
}
