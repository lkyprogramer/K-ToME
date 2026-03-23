package com.ktome.core.talent

import com.ktome.core.combat.ApplicationPolicy
import com.ktome.core.combat.DamageType
import com.ktome.core.combat.SaveDimension
import com.ktome.core.item.StatModifier
import com.ktome.core.resource.ResourceType

data class TalentDef(
    val id: String,
    val name: String,
    val description: String,
    val maxLevel: Int = 5,
    val damageType: DamageType = DamageType.PHYSICAL,
    val powerDimension: SaveDimension? = null,
    val resourceCosts: Map<ResourceType, Int> = emptyMap(),
    val cooldown: Int,
    val range: Int,
    val minRange: Int = 0,
    val areaRadius: Int = 0,
    val levelEffects: Map<Int, TalentLevelEffect>,
) {
    fun resolvedResourceCosts(): Map<ResourceType, Int> = resourceCosts.filterValues { cost -> cost > 0 }
}

data class TalentLevelEffect(
    val damageMultiplier: Double = 1.0,
    val knockback: Int = 0,
    val rangeBonus: Int = 0,
    val healFraction: Double = 0.0,
    val resourceRestoreFraction: Double = 0.0,
    val associatedEffects: List<AssociatedStatusEffect> = emptyList(),
    val cleanseEffect: CleanseEffect? = null,
)

enum class EffectTrigger {
    ON_CAST,
    ON_HIT,
}

enum class EffectTargetScope {
    SELF,
    PRIMARY_TARGET,
    HOSTILES_IN_RADIUS,
}

data class AssociatedStatusEffect(
    val effectId: String,
    val effectType: StatusEffectType,
    val trigger: EffectTrigger,
    val targetScope: EffectTargetScope,
    val applicationPolicy: ApplicationPolicy,
    val saveDimension: SaveDimension? = null,
    val duration: Int = 0,
    val magnitude: Double = 0.0,
)

data class CleanseEffect(
    val effectId: String = "cleanse",
    val trigger: EffectTrigger = EffectTrigger.ON_CAST,
    val targetScope: EffectTargetScope = EffectTargetScope.SELF,
    val applicationPolicy: ApplicationPolicy = ApplicationPolicy.INSTANT_ACTION,
    val maxEffectsRemoved: Int = 1,
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
    GUARD_STANCE_BUFF,
    ARCANE_SHIELD_BUFF,
    UNYIELDING_BUFF,
    MANA_SURGE_BUFF,
    STEALTH_BUFF,
    CURSED,
    HOLY_SHIELD_BUFF,
    DEVOTION_BUFF,
    HOLY_AURA_BUFF,
    ;

    companion object {
        fun fromSchemaId(id: String): StatusEffectType =
            when (id) {
                "STUN", "STUNNED" -> STUNNED
                "CURSE", "CURSED" -> CURSED
                else -> valueOf(id)
            }
    }
}

data class ActiveEffect(
    val id: String,
    val name: String,
    val type: StatusEffectType,
    var remainingTurns: Int,
    val statModifiers: StatModifier = StatModifier(),
    var skipNextDecay: Boolean = false,
)

data class EffectTracker(
    val effects: MutableList<ActiveEffect> = mutableListOf(),
) {
    fun has(type: StatusEffectType): Boolean = effects.any { it.type == type && it.remainingTurns > 0 }
}
