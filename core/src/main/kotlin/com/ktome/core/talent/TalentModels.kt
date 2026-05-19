package com.ktome.core.talent

import com.ktome.core.combat.ApplicationPolicy
import com.ktome.core.combat.DamageType
import com.ktome.core.combat.SaveDimension
import com.ktome.core.ecs.EntityId
import com.ktome.core.item.PassiveEffect
import com.ktome.core.item.StatModifier
import com.ktome.core.resource.EquilibriumAffinity
import com.ktome.core.resource.ResourceType
import com.ktome.core.status.StatusInstance
import com.ktome.core.status.StatusTracker

data class TalentDef(
    val id: String,
    val nameKey: String,
    val descriptionTemplateKey: String,
    val iconKey: String? = null,
    val visualKey: String? = null,
    val audioProfile: String? = null,
    val maxRank: Int = 5,
    val tier: Int = 1,
    val category: TalentCategory = TalentCategory.ACTIVE,
    val damageType: DamageType = DamageType.PHYSICAL,
    val powerDimension: SaveDimension? = null,
    val resourceCosts: List<ResourceCost> = emptyList(),
    val cooldown: Int,
    val actionCost: ActionCost = ActionCost.STANDARD,
    val targetingDef: TalentTargeting = TalentTargeting.self(),
    val levelEffects: Map<Int, TalentLevelEffect>,
    val prerequisites: List<TalentPrerequisite> = emptyList(),
    val breakpoints: List<TalentBreakpoint> = emptyList(),
    val keywords: List<String> = emptyList(),
    val aiHints: TalentAiHints? = null,
    val telegraphRef: String? = null,
    val equilibriumAffinity: EquilibriumAffinity = EquilibriumAffinity.NEUTRAL,
    val callbacks: List<String> = emptyList(),
    val treeId: String = "",
    val unlockLevel: Int = 1,
) {
    val maxLevel: Int
        get() = maxRank

    val range: Int
        get() = targetingDef.range

    val minRange: Int
        get() = targetingDef.minRange

    val areaRadius: Int
        get() = targetingDef.areaRadius

    fun resolvedResourceCosts(): Map<ResourceType, Int> =
        resourceCosts
            .filter { cost -> cost.amount > 0 }
            .associateTo(linkedMapOf()) { cost -> cost.type to cost.amount }

    fun levelEffect(rank: Int): TalentLevelEffect =
        requireNotNull(levelEffects[rank]) { "Missing level effect for talent '$id' rank $rank." }

    fun nextBreakpoint(afterRank: Int): TalentBreakpoint? =
        breakpoints
            .sortedBy(TalentBreakpoint::atRank)
            .firstOrNull { breakpoint -> breakpoint.atRank > afterRank }

    constructor(
        id: String,
        name: String,
        description: String,
        resourceCosts: Map<ResourceType, Int> = emptyMap(),
        cooldown: Int,
        range: Int = 0,
        minRange: Int = 0,
        areaRadius: Int = 0,
        levelEffects: Map<Int, TalentLevelEffect>,
        damageType: DamageType = DamageType.PHYSICAL,
        powerDimension: SaveDimension? = null,
    ) : this(
        id = id,
        nameKey = name,
        descriptionTemplateKey = description,
        maxRank = levelEffects.keys.maxOrNull() ?: 1,
        damageType = damageType,
        powerDimension = powerDimension,
        resourceCosts = resourceCosts.map { (type, amount) -> ResourceCost(type = type, amount = amount) },
        cooldown = cooldown,
        targetingDef =
            when {
                range == 0 && areaRadius == 0 -> TalentTargeting.self()
                range == 0 && areaRadius > 0 ->
                    TalentTargeting(
                        type = TalentTargetingType.RADIUS_SELF,
                        areaRadius = areaRadius,
                    )

                areaRadius > 0 ->
                    TalentTargeting(
                        type = TalentTargetingType.RADIUS_TARGET,
                        range = range,
                        minRange = minRange,
                        areaRadius = areaRadius,
                    )

                else ->
                    TalentTargeting(
                        type = TalentTargetingType.SINGLE_TARGET,
                        range = range,
                        minRange = minRange,
                    )
            },
        levelEffects = levelEffects,
    )
}

enum class TalentCategory {
    ACTIVE,
    PASSIVE,
    SUSTAINED,
}

enum class ActionCost {
    INSTANT,
    QUICK,
    STANDARD,
    HEAVY,
}

enum class TalentTargetingType {
    SELF,
    SINGLE_TARGET,
    LINE,
    CONE,
    RADIUS_SELF,
    RADIUS_TARGET,
    GROUND_TARGET,
    CROSS,
}

data class TalentTargeting(
    val type: TalentTargetingType,
    val range: Int = 0,
    val minRange: Int = 0,
    val areaRadius: Int = 0,
    val requiresLineOfSight: Boolean = true,
    val friendlyFire: Boolean = false,
) {
    companion object {
        fun self(): TalentTargeting = TalentTargeting(type = TalentTargetingType.SELF)
    }
}

data class ResourceCost(
    val type: ResourceType,
    val amount: Int,
)

data class TalentPrerequisite(
    val talentId: String,
    val minRank: Int,
)

enum class TalentRole {
    OFFENSE,
    DEFENSE,
    HEAL,
    CONTROL,
    MOBILITY,
    UTILITY,
}

data class TalentAiHints(
    val role: TalentRole,
    val preferredRange: IntRange? = null,
    val isSustainToggle: Boolean = false,
)

data class TalentBreakpoint(
    val atRank: Int,
    val unlockedEffects: List<EffectOp> = emptyList(),
    val descriptionAddendumKey: String? = null,
)

data class TalentNode(
    val talentId: String,
    val row: Int,
    val col: Int,
    val prerequisites: List<TalentPrerequisite> = emptyList(),
)

data class TalentLevelEffect(
    val damageMultiplier: Double = 1.0,
    val knockback: Int = 0,
    val rangeBonus: Int = 0,
    val healFraction: Double = 0.0,
    val resourceRestoreFraction: Double = 0.0,
    val associatedEffects: List<AssociatedStatusEffect> = emptyList(),
    val cleanseEffect: CleanseEffect? = null,
    val effectOps: List<EffectOp> = emptyList(),
    val passiveEffects: List<PassiveEffect> = emptyList(),
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
    val statusId: String,
    val trigger: EffectTrigger,
    val targetScope: EffectTargetScope,
    val applicationPolicy: ApplicationPolicy,
    val saveDimension: SaveDimension? = null,
    val duration: Int = 0,
    val magnitude: Double = 0.0,
) {
    constructor(
        effectId: String,
        effectType: com.ktome.core.status.StatusEffectType,
        trigger: EffectTrigger,
        targetScope: EffectTargetScope,
        applicationPolicy: ApplicationPolicy,
        saveDimension: SaveDimension? = null,
        duration: Int = 0,
        magnitude: Double = 0.0,
    ) : this(
        effectId = effectId,
        statusId = effectType.schemaId,
        trigger = trigger,
        targetScope = targetScope,
        applicationPolicy = applicationPolicy,
        saveDimension = saveDimension,
        duration = duration,
        magnitude = magnitude,
    )
}

data class CleanseEffect(
    val effectId: String = "cleanse",
    val trigger: EffectTrigger = EffectTrigger.ON_CAST,
    val targetScope: EffectTargetScope = EffectTargetScope.SELF,
    val applicationPolicy: ApplicationPolicy = ApplicationPolicy.INSTANT_ACTION,
    val maxEffectsRemoved: Int = 1,
)

data class ScalingDef(
    val attackMultiplier: Double = 0.0,
    val spellMultiplier: Double = 0.0,
    val statId: String? = null,
)

enum class DisplacementType {
    PUSH,
    PULL,
    DASH,
    TELEPORT,
}

sealed interface EffectOp {
    data class Damage(
        val damageType: DamageType? = null,
        val baseAmount: IntRange = 0..0,
        val scaling: ScalingDef = ScalingDef(),
    ) : EffectOp

    data class Heal(
        val baseAmount: IntRange = 0..0,
        val scaling: ScalingDef = ScalingDef(),
        val maxHpFraction: Double = 0.0,
    ) : EffectOp

    data class ApplyStatus(
        val statusId: String,
        val duration: Int,
        val applicationPolicy: ApplicationPolicy,
        val trigger: EffectTrigger = EffectTrigger.ON_CAST,
        val targetScope: EffectTargetScope = EffectTargetScope.SELF,
        val saveDimension: SaveDimension? = null,
        val magnitude: Double = 0.0,
    ) : EffectOp

    data class ResourceRestore(
        val type: ResourceType,
        val amount: Int = 0,
        val fraction: Double = 0.0,
    ) : EffectOp

    data class Displacement(
        val type: DisplacementType,
        val distance: Int,
        val targetScope: EffectTargetScope = EffectTargetScope.PRIMARY_TARGET,
    ) : EffectOp

    data class StatModifier(
        val modifier: com.ktome.core.item.StatModifier,
        val duration: Int,
        val targetScope: EffectTargetScope = EffectTargetScope.SELF,
    ) : EffectOp
}

data class TalentLoadout(
    val slotToTalentId: MutableMap<Int, String> = linkedMapOf(),
    val talentLevels: MutableMap<String, Int> = linkedMapOf(),
) {
    fun talentIdAt(slot: Int): String? = slotToTalentId[slot]

    fun levelOf(talentId: String): Int = talentLevels[talentId] ?: 0

    fun effectiveLevels(draft: TalentAllocationDraft?): Map<String, Int> =
        if (draft == null) {
            talentLevels.toMap(linkedMapOf())
        } else {
            TalentAllocationPlanner.effectiveRanks(liveRanks = talentLevels, draft = draft)
        }
}

data class CooldownState(
    val remainingByTalentId: MutableMap<String, Int> = linkedMapOf(),
)

typealias EffectTracker = StatusTracker

typealias ActiveEffect = StatusInstance

data class TalentStatusApplication(
    val statusId: String,
    val engineTypeId: String,
    val duration: Int,
)

data class TalentStatusResult(
    val target: EntityId,
    val statusId: String,
    val duration: Int,
    val category: String,
)
