package com.ktome.core.talent

import com.ktome.core.item.StatModifier

data class TalentDef(
    val id: String,
    val name: String,
    val description: String,
    val maxLevel: Int = 5,
    val staminaCost: Int,
    val cooldown: Int,
    val range: Int,
    val minRange: Int = 0,
    val areaRadius: Int = 0,
    val levelEffects: Map<Int, TalentLevelEffect>,
)

data class TalentLevelEffect(
    val damageMultiplier: Double = 1.0,
    val knockback: Int = 0,
    val stunDuration: Int = 0,
    val armorBreakDuration: Int = 0,
    val buffDuration: Int = 0,
    val buffMagnitude: Double = 0.0,
    val debuffMagnitude: Double = 0.0,
    val debuffDuration: Int = 0,
)

data class TalentLoadout(
    val slotToTalentId: MutableMap<Int, String> = linkedMapOf(),
    val talentLevels: MutableMap<String, Int> = linkedMapOf(),
) {
    fun talentIdAt(slot: Int): String? = slotToTalentId[slot]

    fun levelOf(talentId: String): Int = talentLevels[talentId] ?: 1
}

data class CooldownState(
    val remainingByTalentId: MutableMap<String, Int> = linkedMapOf(),
)

enum class StatusEffectType {
    STUNNED,
    ARMOR_BREAK,
    WAR_CRY_BUFF,
    WAR_CRY_DEBUFF,
}

data class ActiveEffect(
    val id: String,
    val name: String,
    val type: StatusEffectType,
    var remainingTurns: Int,
    val statModifiers: StatModifier = StatModifier(),
)

data class EffectTracker(
    val effects: MutableList<ActiveEffect> = mutableListOf(),
) {
    fun has(type: StatusEffectType): Boolean = effects.any { it.type == type && it.remainingTurns > 0 }
}

