package com.ktome.core.combat

import com.ktome.core.status.StatusEffectType

data class DamagePacket(
    val request: DamageRequest,
    val hitConfirmed: Boolean,
    val critical: Boolean,
    val damageType: DamageType,
    val rawDamage: Int,
    val reducedDamage: Int,
    val finalDamage: Int,
    val remainingHpAfterApply: Int,
    val mitigationValue: Int = 0,
    val preReductionAbsorbed: Int = 0,
    val penetrationContribution: Int = 0,
    val postReductionModifier: Int = 0,
)

data class DamageResult(
    val type: DamageType,
    val rawDamage: Int,
    val reducedDamage: Int,
    val finalDamage: Int,
    val resistanceValue: Int = 0,
    val preReductionAbsorbed: Int = 0,
    val penetrationContribution: Int = 0,
)

data class CombatResult(
    val hit: Boolean,
    val crit: Boolean,
    val packet: DamagePacket? = null,
    val damage: DamageResult? = null,
    val targetKilled: Boolean = false,
    val trace: CombatResolutionTrace? = null,
    val envelope: TraceEnvelope? = null,
    val statusApplication: StatusApplicationResolution? = null,
    val terrainInteraction: ElementInteractionResolution? = null,
    val removedStatusTypes: Set<StatusEffectType> = emptySet(),
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

data class StatusTickResult(
    val damage: DamageResult,
    val targetKilled: Boolean,
    val trace: CombatResolutionTrace,
    val envelope: TraceEnvelope,
)
