package com.ktome.core.status

import com.ktome.core.combat.ApplicationPolicy
import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.effect.ActorEffect
import com.ktome.core.effect.AreaEffectEmitter
import com.ktome.core.effect.PersistentEffect
import com.ktome.core.effect.WorldEffect
import com.ktome.core.item.StatModifier
import kotlin.math.roundToInt

enum class EffectCategory {
    BUFF,
    DEBUFF,
    NEUTRAL,
}

enum class EffectCarrierKind {
    ACTOR,
    AREA,
    WORLD,
}

enum class StackingRule {
    REFRESH_DURATION,
    INDEPENDENT_STACKS,
    CAPPED_STACKS,
    KEEP_STRONGEST,
    LATEST_OVERRIDES,
    UNIQUE,
}

enum class ReplacePolicy {
    REFRESH_DURATION,
    KEEP_STRONGEST,
    REPLACE_EXISTING,
}

enum class RemoteRemovalPolicy {
    ACTOR_CLEANSE_REMOVABLE,
    ACTOR_CLEANSE_IMMUNE,
}

enum class StatusTickTiming {
    NONE,
    TURN_START,
}

object StatusTickPriority {
    const val DEFAULT: Int = 300
    const val DOT: Int = 320
}

enum class StatusEffectType(
    val schemaId: String,
    val category: EffectCategory,
    val dispellable: Boolean = true,
    val introducedPhase: String = "P2",
) {
    CUSTOM("CUSTOM", EffectCategory.NEUTRAL, introducedPhase = "P3"),
    STUN("STUN", EffectCategory.DEBUFF, introducedPhase = "P1"),
    ARMOR_BREAK("ARMOR_BREAK", EffectCategory.DEBUFF, introducedPhase = "P1"),
    GUARD("GUARD", EffectCategory.BUFF),
    MARKED("MARKED", EffectCategory.DEBUFF),
    ROOT("ROOT", EffectCategory.DEBUFF),
    SILENCE("SILENCE", EffectCategory.DEBUFF),
    BLEED("BLEED", EffectCategory.DEBUFF),
    BURN("BURN", EffectCategory.DEBUFF),
    SHIELD("SHIELD", EffectCategory.BUFF),
    REGEN("REGEN", EffectCategory.BUFF),
    HASTE("HASTE", EffectCategory.BUFF),
    SLOW("SLOW", EffectCategory.DEBUFF),
    FREEZE("FREEZE", EffectCategory.DEBUFF),
    POISON("POISON", EffectCategory.DEBUFF),
    BANE("BANE", EffectCategory.DEBUFF, introducedPhase = "P3"),
    CURSE("CURSE", EffectCategory.DEBUFF, introducedPhase = "P3"),
    WEAKEN("WEAKEN", EffectCategory.DEBUFF, introducedPhase = "P3"),
    OVERCHARGE("OVERCHARGE", EffectCategory.DEBUFF, introducedPhase = "P3"),
    INVULNERABLE("INVULNERABLE", EffectCategory.BUFF, dispellable = false, introducedPhase = "P3"),
    STEALTH("STEALTH", EffectCategory.BUFF, dispellable = false, introducedPhase = "P3"),
    TAUNT("TAUNT", EffectCategory.DEBUFF, introducedPhase = "P3"),
    GUARD_STANCE_BUFF("GUARD_STANCE_BUFF", EffectCategory.BUFF),
    ARCANE_SHIELD_BUFF("ARCANE_SHIELD_BUFF", EffectCategory.BUFF),
    UNYIELDING_BUFF("UNYIELDING_BUFF", EffectCategory.BUFF),
    MANA_SURGE_BUFF("MANA_SURGE_BUFF", EffectCategory.BUFF),
    HOLY_SHIELD_BUFF("HOLY_SHIELD_BUFF", EffectCategory.BUFF),
    DEVOTION_BUFF("DEVOTION_BUFF", EffectCategory.BUFF),
    HOLY_AURA_BUFF("HOLY_AURA_BUFF", EffectCategory.BUFF),
    ;

    companion object {
        fun fromSchemaId(id: String): StatusEffectType =
            when (id) {
                "STUN", "STUNNED" -> STUN
                "CURSE", "CURSED" -> CURSE
                "STEALTH", "STEALTH_BUFF" -> STEALTH
                else -> values().firstOrNull { type -> type.schemaId == id } ?: runCatching { valueOf(id) }.getOrDefault(CUSTOM)
            }
    }
}

data class StatusEffectDef(
    val id: String,
    val type: StatusEffectType,
    val category: EffectCategory = type.category,
    val nameKey: String,
    val iconKey: String? = null,
    val stackingRule: StackingRule,
    val stackCap: Int = 1,
    val replacePolicy: ReplacePolicy = ReplacePolicy.REFRESH_DURATION,
    val uniquenessKey: String? = null,
    val exclusiveGroup: String? = null,
    val sourceScopedUnique: Boolean = false,
    val dispellable: Boolean = type.dispellable,
    val remoteRemovalPolicy: RemoteRemovalPolicy = RemoteRemovalPolicy.ACTOR_CLEANSE_REMOVABLE,
    val tickTiming: StatusTickTiming = StatusTickTiming.NONE,
    val tickPriority: Int = StatusTickPriority.DEFAULT,
    val tickDamageType: DamageType? = null,
    val tickDamage: Int = 0,
    val statModifier: StatModifier = StatModifier.ZERO,
    val carrierKind: EffectCarrierKind = EffectCarrierKind.ACTOR,
    val breaksOnActualDamage: Boolean = false,
    val consumedOnDamageType: DamageType? = null,
    val consumedDamageMultiplier: Double = 1.0,
)

data class StatusInstance(
    val id: String,
    val type: StatusEffectType,
    var remainingTurns: Int,
    var statModifiers: StatModifier = StatModifier.ZERO,
    var skipNextDecay: Boolean = false,
    val nameKey: String? = null,
    val iconKey: String? = null,
    val category: EffectCategory = type.category,
    val schemaId: String = type.schemaId,
    var stackCount: Int = 1,
    val stackCap: Int = 1,
    val stackingRule: StackingRule = StackingRule.REFRESH_DURATION,
    val replacePolicy: ReplacePolicy = ReplacePolicy.REFRESH_DURATION,
    val uniquenessKey: String? = null,
    val exclusiveGroup: String? = null,
    val sourceScopedUnique: Boolean = false,
    val dispellable: Boolean = type.dispellable,
    val remoteRemovalPolicy: RemoteRemovalPolicy = RemoteRemovalPolicy.ACTOR_CLEANSE_REMOVABLE,
    val tickTiming: StatusTickTiming = StatusTickTiming.NONE,
    val tickPriority: Int = StatusTickPriority.DEFAULT,
    val tickDamageType: DamageType? = null,
    val tickDamage: Int = 0,
    val sourceEntityId: EntityId? = null,
    val carrierKind: EffectCarrierKind = EffectCarrierKind.ACTOR,
    val appliedTurn: Int = 0,
    val applicationPolicy: ApplicationPolicy? = null,
    var magnitude: Double = 0.0,
    val breaksOnActualDamage: Boolean = false,
    val consumedOnDamageType: DamageType? = null,
    val consumedDamageMultiplier: Double = 1.0,
) {
    fun effectiveStatModifier(): StatModifier =
        if (stackingRule == StackingRule.CAPPED_STACKS) {
            statModifiers.scaled(stackCount)
        } else {
            statModifiers
        }

    fun isActive(): Boolean = remainingTurns > 0

    fun isDebuff(): Boolean = category == EffectCategory.DEBUFF

    fun strengthScore(): Double = magnitude * 1000.0 + remainingTurns
}

typealias StatusTracker = ActorEffect

data class CleansePolicy(
    val priorityOrder: CleanseOrder = CleanseOrder.HARD_CONTROL_THEN_LONGEST,
    val canCleanseTypes: Set<StatusEffectType>? = null,
    val excludeTypes: Set<StatusEffectType> = emptySet(),
) {
    companion object {
        val DEFAULT: CleansePolicy = CleansePolicy()
    }
}

enum class CleanseOrder {
    HARD_CONTROL_THEN_LONGEST,
    LONGEST_REMAINING,
    MOST_RECENT,
    HIGHEST_MAGNITUDE,
}

data class StatusChangeResult(
    val applied: Boolean,
    val added: List<StatusInstance> = emptyList(),
    val removed: List<StatusInstance> = emptyList(),
    val refreshed: List<StatusInstance> = emptyList(),
    val interactionId: String? = null,
)

data class CarrierDueEffect(
    val carrierKind: EffectCarrierKind,
    val effect: StatusInstance,
    val sourceEntityId: EntityId? = null,
    val sourceKey: String,
    val orderKey: String,
)

object StatusDefinitions {
    private val definitions: Map<StatusEffectType, StatusEffectDef> =
        linkedMapOf(
            StatusEffectType.STUN to
                def(
                    type = StatusEffectType.STUN,
                    nameKey = "status.stun",
                    iconKey = "icon.status.stunned",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
            StatusEffectType.ARMOR_BREAK to
                def(
                    type = StatusEffectType.ARMOR_BREAK,
                    nameKey = "status.armor_break",
                    iconKey = "icon.status.armor_break",
                    stackingRule = StackingRule.CAPPED_STACKS,
                    stackCap = 3,
                    statModifier = StatModifier(defense = -3),
                ),
            StatusEffectType.GUARD to
                def(
                    type = StatusEffectType.GUARD,
                    nameKey = "status.guard",
                    iconKey = "icon.status.guard_stance_buff",
                    stackingRule = StackingRule.KEEP_STRONGEST,
                ),
            StatusEffectType.MARKED to
                def(
                    type = StatusEffectType.MARKED,
                    nameKey = "status.marked",
                    iconKey = "icon.status.war_cry_debuff",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
            StatusEffectType.ROOT to
                def(
                    type = StatusEffectType.ROOT,
                    nameKey = "status.root",
                    iconKey = "icon.status.stunned",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
            StatusEffectType.SILENCE to
                def(
                    type = StatusEffectType.SILENCE,
                    nameKey = "status.silence",
                    iconKey = "icon.status.mana_surge_buff",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
            StatusEffectType.BLEED to
                def(
                    type = StatusEffectType.BLEED,
                    nameKey = "status.bleed",
                    iconKey = "icon.status.cursed",
                    stackingRule = StackingRule.INDEPENDENT_STACKS,
                    tickTiming = StatusTickTiming.TURN_START,
                    tickPriority = StatusTickPriority.DOT,
                    tickDamageType = DamageType.PHYSICAL,
                    tickDamage = 3,
                ),
            StatusEffectType.BURN to
                def(
                    type = StatusEffectType.BURN,
                    nameKey = "status.burn",
                    iconKey = "icon.status.cursed",
                    stackingRule = StackingRule.INDEPENDENT_STACKS,
                    exclusiveGroup = "element.fire_cold",
                    tickTiming = StatusTickTiming.TURN_START,
                    tickPriority = StatusTickPriority.DOT,
                    tickDamageType = DamageType.FIRE,
                    tickDamage = 4,
                ),
            StatusEffectType.SHIELD to
                def(
                    type = StatusEffectType.SHIELD,
                    nameKey = "status.shield",
                    iconKey = "icon.skill.templar.divine_intervention",
                    stackingRule = StackingRule.KEEP_STRONGEST,
                ),
            StatusEffectType.REGEN to
                def(
                    type = StatusEffectType.REGEN,
                    nameKey = "status.regen",
                    iconKey = "icon.status.unyielding_buff",
                    stackingRule = StackingRule.KEEP_STRONGEST,
                ),
            StatusEffectType.HASTE to
                def(
                    type = StatusEffectType.HASTE,
                    nameKey = "status.haste",
                    iconKey = "icon.status.war_cry_buff",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
            StatusEffectType.SLOW to
                def(
                    type = StatusEffectType.SLOW,
                    nameKey = "status.slow",
                    iconKey = "icon.status.war_cry_debuff",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
            StatusEffectType.FREEZE to
                def(
                    type = StatusEffectType.FREEZE,
                    nameKey = "status.freeze",
                    iconKey = "icon.status.stunned",
                    stackingRule = StackingRule.REFRESH_DURATION,
                    exclusiveGroup = "element.fire_cold",
                ),
            StatusEffectType.POISON to
                def(
                    type = StatusEffectType.POISON,
                    nameKey = "status.poison",
                    iconKey = "icon.status.cursed",
                    stackingRule = StackingRule.INDEPENDENT_STACKS,
                    tickTiming = StatusTickTiming.TURN_START,
                    tickPriority = StatusTickPriority.DOT,
                    tickDamageType = DamageType.SHADOW,
                    tickDamage = 3,
                ),
            StatusEffectType.BANE to
                def(
                    type = StatusEffectType.BANE,
                    nameKey = "status.bane",
                    iconKey = "icon.status.war_cry_debuff",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
            StatusEffectType.CURSE to
                def(
                    type = StatusEffectType.CURSE,
                    nameKey = "status.curse",
                    iconKey = "icon.status.cursed",
                    stackingRule = StackingRule.REFRESH_DURATION,
                    statModifier = StatModifier(attackMultiplierBonus = -0.15, defenseMultiplierBonus = -0.15),
                ),
            StatusEffectType.WEAKEN to
                def(
                    type = StatusEffectType.WEAKEN,
                    nameKey = "status.weaken",
                    iconKey = "icon.status.war_cry_debuff",
                    stackingRule = StackingRule.REFRESH_DURATION,
                    statModifier = StatModifier(attackMultiplierBonus = -0.15),
                ),
            StatusEffectType.OVERCHARGE to
                def(
                    type = StatusEffectType.OVERCHARGE,
                    nameKey = "status.overcharge",
                    iconKey = "icon.status.mana_surge_buff",
                    stackingRule = StackingRule.REFRESH_DURATION,
                    consumedOnDamageType = DamageType.LIGHTNING,
                    consumedDamageMultiplier = 1.25,
                ),
            StatusEffectType.INVULNERABLE to
                def(
                    type = StatusEffectType.INVULNERABLE,
                    nameKey = "status.invulnerable",
                    iconKey = "icon.skill.templar.divine_intervention",
                    stackingRule = StackingRule.KEEP_STRONGEST,
                    dispellable = false,
                ),
            StatusEffectType.STEALTH to
                def(
                    type = StatusEffectType.STEALTH,
                    nameKey = "status.stealth",
                    iconKey = "icon.skill.rogue.shadowstep",
                    stackingRule = StackingRule.REFRESH_DURATION,
                    dispellable = false,
                    breaksOnActualDamage = true,
                ),
            StatusEffectType.TAUNT to
                def(
                    type = StatusEffectType.TAUNT,
                    nameKey = "status.taunt",
                    iconKey = "icon.status.war_cry_debuff",
                    stackingRule = StackingRule.LATEST_OVERRIDES,
                ),
            StatusEffectType.GUARD_STANCE_BUFF to
                def(
                    type = StatusEffectType.GUARD_STANCE_BUFF,
                    nameKey = "status.guard_stance_buff",
                    iconKey = "icon.status.guard_stance_buff",
                    stackingRule = StackingRule.KEEP_STRONGEST,
                ),
            StatusEffectType.ARCANE_SHIELD_BUFF to
                def(
                    type = StatusEffectType.ARCANE_SHIELD_BUFF,
                    nameKey = "status.arcane_shield_buff",
                    iconKey = "icon.status.arcane_shield_buff",
                    stackingRule = StackingRule.KEEP_STRONGEST,
                ),
            StatusEffectType.UNYIELDING_BUFF to
                def(
                    type = StatusEffectType.UNYIELDING_BUFF,
                    nameKey = "status.unyielding_buff",
                    iconKey = "icon.status.unyielding_buff",
                    stackingRule = StackingRule.KEEP_STRONGEST,
                ),
            StatusEffectType.MANA_SURGE_BUFF to
                def(
                    type = StatusEffectType.MANA_SURGE_BUFF,
                    nameKey = "status.mana_surge_buff",
                    iconKey = "icon.status.mana_surge_buff",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
            StatusEffectType.HOLY_SHIELD_BUFF to
                def(
                    type = StatusEffectType.HOLY_SHIELD_BUFF,
                    nameKey = "status.holy_shield_buff",
                    iconKey = "icon.skill.templar.divine_intervention",
                    stackingRule = StackingRule.KEEP_STRONGEST,
                ),
            StatusEffectType.DEVOTION_BUFF to
                def(
                    type = StatusEffectType.DEVOTION_BUFF,
                    nameKey = "status.devotion_buff",
                    iconKey = "icon.status.consecration",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
            StatusEffectType.HOLY_AURA_BUFF to
                def(
                    type = StatusEffectType.HOLY_AURA_BUFF,
                    nameKey = "status.holy_aura_buff",
                    iconKey = "icon.status.consecration",
                    stackingRule = StackingRule.REFRESH_DURATION,
                ),
        )

    fun definitionFor(type: StatusEffectType): StatusEffectDef =
        requireNotNull(definitions[type]) { "Missing status definition for ${type.name}." }

    fun definitionForSchemaId(schemaId: String): StatusEffectDef? =
        definitions.values.firstOrNull { definition -> definition.type.schemaId == schemaId }

    fun nameKey(type: StatusEffectType): String = definitionFor(type).nameKey

    fun iconKey(type: StatusEffectType): String? = definitionFor(type).iconKey

    private fun def(
        type: StatusEffectType,
        nameKey: String,
        iconKey: String?,
        category: EffectCategory = type.category,
        stackingRule: StackingRule,
        stackCap: Int = 1,
        replacePolicy: ReplacePolicy = ReplacePolicy.REFRESH_DURATION,
        uniquenessKey: String? = null,
        exclusiveGroup: String? = null,
        sourceScopedUnique: Boolean = false,
        dispellable: Boolean = type.dispellable,
        remoteRemovalPolicy: RemoteRemovalPolicy = RemoteRemovalPolicy.ACTOR_CLEANSE_REMOVABLE,
        tickTiming: StatusTickTiming = StatusTickTiming.NONE,
        tickPriority: Int = StatusTickPriority.DEFAULT,
        tickDamageType: DamageType? = null,
        tickDamage: Int = 0,
        statModifier: StatModifier = StatModifier.ZERO,
        carrierKind: EffectCarrierKind = EffectCarrierKind.ACTOR,
        breaksOnActualDamage: Boolean = false,
        consumedOnDamageType: DamageType? = null,
        consumedDamageMultiplier: Double = 1.0,
    ): StatusEffectDef =
        StatusEffectDef(
            id = type.schemaId,
            type = type,
            category = category,
            nameKey = nameKey,
            iconKey = iconKey,
            stackingRule = stackingRule,
            stackCap = stackCap,
            replacePolicy = replacePolicy,
            uniquenessKey = uniquenessKey,
            exclusiveGroup = exclusiveGroup,
            sourceScopedUnique = sourceScopedUnique,
            dispellable = dispellable,
            remoteRemovalPolicy = remoteRemovalPolicy,
            tickTiming = tickTiming,
            tickPriority = tickPriority,
            tickDamageType = tickDamageType,
            tickDamage = tickDamage,
            statModifier = statModifier,
            carrierKind = carrierKind,
            breaksOnActualDamage = breaksOnActualDamage,
            consumedOnDamageType = consumedOnDamageType,
            consumedDamageMultiplier = consumedDamageMultiplier,
        )
}

object StatusLifecycle {
    fun createInstance(
        definition: StatusEffectDef,
        effectId: String,
        duration: Int,
        magnitude: Double = 0.0,
        sourceEntityId: EntityId? = null,
        appliedTurn: Int = 0,
        skipNextDecay: Boolean = false,
        applicationPolicy: ApplicationPolicy? = null,
        statModifierOverride: StatModifier? = null,
        tickDamageOverride: Int? = null,
    ): StatusInstance =
        StatusInstance(
            id = effectId,
            type = definition.type,
            remainingTurns = duration,
            statModifiers = statModifierOverride ?: definition.statModifier,
            skipNextDecay = skipNextDecay,
            nameKey = definition.nameKey,
            iconKey = definition.iconKey,
            category = definition.category,
            schemaId = definition.id,
            stackCount = 1,
            stackCap = definition.stackCap,
            stackingRule = definition.stackingRule,
            replacePolicy = definition.replacePolicy,
            uniquenessKey = definition.uniquenessKey,
            exclusiveGroup = definition.exclusiveGroup,
            sourceScopedUnique = definition.sourceScopedUnique,
            dispellable = definition.dispellable,
            remoteRemovalPolicy = definition.remoteRemovalPolicy,
            tickTiming = definition.tickTiming,
            tickPriority = definition.tickPriority,
            tickDamageType = definition.tickDamageType,
            tickDamage = tickDamageOverride ?: definition.tickDamage,
            sourceEntityId = sourceEntityId,
            carrierKind = definition.carrierKind,
            appliedTurn = appliedTurn,
            applicationPolicy = applicationPolicy,
            magnitude = magnitude,
            breaksOnActualDamage = definition.breaksOnActualDamage,
            consumedOnDamageType = definition.consumedOnDamageType,
            consumedDamageMultiplier = definition.consumedDamageMultiplier,
        )

    fun createInstance(
        type: StatusEffectType,
        effectId: String,
        duration: Int,
        magnitude: Double = 0.0,
        sourceEntityId: EntityId? = null,
        appliedTurn: Int = 0,
        skipNextDecay: Boolean = false,
        applicationPolicy: ApplicationPolicy? = null,
        statModifierOverride: StatModifier? = null,
        tickDamageOverride: Int? = null,
    ): StatusInstance {
        val definition = StatusDefinitions.definitionFor(type)
        return createInstance(
            definition = definition.copy(id = type.schemaId, statModifier = statModifierOverride ?: defaultStatModifier(type, magnitude)),
            effectId = effectId,
            duration = duration,
            magnitude = magnitude,
            sourceEntityId = sourceEntityId,
            appliedTurn = appliedTurn,
            skipNextDecay = skipNextDecay,
            applicationPolicy = applicationPolicy,
            statModifierOverride = statModifierOverride ?: defaultStatModifier(type, magnitude),
            tickDamageOverride = tickDamageOverride,
        )
    }

    fun applyEffect(
        tracker: StatusTracker,
        incoming: StatusInstance,
    ): StatusChangeResult {
        val removed = mutableListOf<StatusInstance>()
        val refreshed = mutableListOf<StatusInstance>()
        val interactionId = applyMutualExclusion(tracker, incoming, removed)

        val updated =
            when (incoming.stackingRule) {
                StackingRule.INDEPENDENT_STACKS -> {
                    tracker.effects += incoming
                    true
                }

                StackingRule.CAPPED_STACKS -> {
                    val existing = tracker.effects.firstOrNull { effect -> effect.isActive() && effect.schemaId == incoming.schemaId }
                    if (existing == null) {
                        tracker.effects += incoming
                    } else {
                        existing.stackCount = (existing.stackCount + incoming.stackCount).coerceAtMost(existing.stackCap)
                        existing.remainingTurns = maxOf(existing.remainingTurns, incoming.remainingTurns)
                        refreshed += existing
                    }
                    true
                }

                StackingRule.KEEP_STRONGEST -> applyKeepStrongest(tracker, incoming, refreshed)
                StackingRule.LATEST_OVERRIDES -> {
                    val replaced = tracker.effects.filter { effect -> effect.isActive() && effect.schemaId == incoming.schemaId }
                    tracker.effects.removeAll(replaced.toSet())
                    removed += replaced
                    tracker.effects += incoming
                    true
                }

                StackingRule.UNIQUE -> applyUnique(tracker, incoming, refreshed)
                StackingRule.REFRESH_DURATION -> applyRefresh(tracker, incoming, refreshed)
            }

        return StatusChangeResult(
            applied = updated,
            added = if (updated && incoming in tracker.effects) listOf(incoming) else emptyList(),
            removed = removed,
            refreshed = refreshed,
            interactionId =
                when {
                    interactionId != null -> interactionId
                    incoming.type == StatusEffectType.TAUNT && removed.any { effect -> effect.schemaId == StatusEffectType.TAUNT.schemaId } ->
                        "TAUNT_OVERRIDE"
                    else -> null
                },
        )
    }

    fun applyEffect(
        world: World,
        target: EntityId,
        incoming: StatusInstance,
    ): StatusChangeResult {
        val tracker = world.get<StatusTracker>(target) ?: StatusTracker(ownerId = target).also { tracker -> world.add(target, tracker) }
        return applyEffect(tracker, incoming)
    }

    fun decayEndOfTurn(tracker: StatusTracker): Boolean {
        return decayEndOfTurn(tracker.effects)
    }

    fun decayEndOfTurn(carrier: PersistentEffect): Boolean = decayEndOfTurn(carrier.effects)

    fun decayEndOfTurn(effects: MutableList<StatusInstance>): Boolean {
        var changed = false
        val iterator = effects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            if (effect.skipNextDecay) {
                effect.skipNextDecay = false
                continue
            }
            effect.remainingTurns -= 1
            if (effect.remainingTurns <= 0) {
                iterator.remove()
                changed = true
            }
        }
        return changed
    }

    fun cleanse(
        tracker: StatusTracker,
        maxEffectsRemoved: Int,
        policy: CleansePolicy = CleansePolicy.DEFAULT,
    ): List<StatusInstance> {
        if (maxEffectsRemoved <= 0) {
            return emptyList()
        }
        val removable =
            tracker.activeEffects()
                .filter { effect ->
                    effect.isDebuff() &&
                        effect.dispellable &&
                        effect.remoteRemovalPolicy == RemoteRemovalPolicy.ACTOR_CLEANSE_REMOVABLE &&
                        (policy.canCleanseTypes == null || policy.canCleanseTypes.any { type -> type.schemaId == effect.schemaId }) &&
                        policy.excludeTypes.none { type -> type.schemaId == effect.schemaId }
                }.sortedWith(cleanseComparator(policy.priorityOrder))
                .take(maxEffectsRemoved)
        if (removable.isEmpty()) {
            return emptyList()
        }
        tracker.effects.removeAll(removable.toSet())
        return removable
    }

    fun incomingDamageMultiplier(
        tracker: StatusTracker,
        damageType: DamageType,
    ): Double =
        tracker.activeEffects()
            .filter { effect -> effect.consumedOnDamageType == damageType }
            .maxOfOrNull(StatusInstance::consumedDamageMultiplier)
            ?: 1.0

    fun consumeOnDamage(
        tracker: StatusTracker,
        damageType: DamageType,
        actualDamage: Int,
    ): List<StatusInstance> {
        if (actualDamage <= 0) {
            return emptyList()
        }
        val consumed =
            tracker.activeEffects()
                .filter { effect -> effect.consumedOnDamageType == damageType }
        if (consumed.isEmpty()) {
            return emptyList()
        }
        tracker.effects.removeAll(consumed.toSet())
        return consumed
    }

    fun breakOnDamage(
        tracker: StatusTracker,
        actualDamage: Int,
    ): List<StatusInstance> {
        if (actualDamage <= 0) {
            return emptyList()
        }
        val removed = tracker.activeEffects().filter(StatusInstance::breaksOnActualDamage)
        if (removed.isEmpty()) {
            return emptyList()
        }
        tracker.effects.removeAll(removed.toSet())
        return removed
    }

    fun hasInvulnerable(tracker: StatusTracker): Boolean =
        tracker.activeEffects().any { effect -> effect.schemaId == StatusEffectType.INVULNERABLE.schemaId }

    private fun applyRefresh(
        tracker: StatusTracker,
        incoming: StatusInstance,
        refreshed: MutableList<StatusInstance>,
    ): Boolean {
        val existing = tracker.effects.firstOrNull { effect -> effect.isActive() && effect.schemaId == incoming.schemaId }
        if (existing == null) {
            tracker.effects += incoming
            return true
        }
        existing.remainingTurns = maxOf(existing.remainingTurns, incoming.remainingTurns)
        if (incoming.strengthScore() >= existing.strengthScore()) {
            existing.statModifiers = incoming.statModifiers
            existing.magnitude = incoming.magnitude
        }
        refreshed += existing
        return true
    }

    private fun applyKeepStrongest(
        tracker: StatusTracker,
        incoming: StatusInstance,
        refreshed: MutableList<StatusInstance>,
    ): Boolean {
        val existing = tracker.effects.firstOrNull { effect -> effect.isActive() && effect.schemaId == incoming.schemaId }
        if (existing == null) {
            tracker.effects += incoming
            return true
        }
        if (incoming.strengthScore() >= existing.strengthScore()) {
            existing.remainingTurns = incoming.remainingTurns
            existing.statModifiers = incoming.statModifiers
            existing.magnitude = incoming.magnitude
        } else {
            existing.remainingTurns = maxOf(existing.remainingTurns, incoming.remainingTurns)
        }
        refreshed += existing
        return true
    }

    private fun applyUnique(
        tracker: StatusTracker,
        incoming: StatusInstance,
        refreshed: MutableList<StatusInstance>,
    ): Boolean {
        val existing =
            tracker.effects.firstOrNull { effect ->
                effect.isActive() &&
                    effect.uniquenessKey == incoming.uniquenessKey &&
                    (!incoming.sourceScopedUnique || effect.sourceEntityId == incoming.sourceEntityId)
            }
        if (existing == null) {
            tracker.effects += incoming
            return true
        }
        return when (incoming.replacePolicy) {
            ReplacePolicy.REPLACE_EXISTING -> {
                tracker.effects.remove(existing)
                tracker.effects += incoming
                true
            }

            ReplacePolicy.KEEP_STRONGEST -> {
                if (incoming.strengthScore() >= existing.strengthScore()) {
                    existing.remainingTurns = incoming.remainingTurns
                    existing.statModifiers = incoming.statModifiers
                    existing.magnitude = incoming.magnitude
                } else {
                    existing.remainingTurns = maxOf(existing.remainingTurns, incoming.remainingTurns)
                }
                refreshed += existing
                true
            }

            ReplacePolicy.REFRESH_DURATION -> {
                existing.remainingTurns = maxOf(existing.remainingTurns, incoming.remainingTurns)
                if (incoming.strengthScore() >= existing.strengthScore()) {
                    existing.statModifiers = incoming.statModifiers
                    existing.magnitude = incoming.magnitude
                }
                refreshed += existing
                true
            }
        }
    }

    private fun applyMutualExclusion(
        tracker: StatusTracker,
        incoming: StatusInstance,
        removed: MutableList<StatusInstance>,
    ): String? {
        val conflictingSchemaId =
            when (incoming.type) {
                StatusEffectType.BURN -> StatusEffectType.FREEZE.schemaId
                StatusEffectType.FREEZE -> StatusEffectType.BURN.schemaId
                else -> null
            } ?: return null
        val conflicts = tracker.activeEffects().filter { effect -> effect.schemaId == conflictingSchemaId }
        if (conflicts.isEmpty()) {
            return null
        }
        tracker.effects.removeAll(conflicts.toSet())
        removed += conflicts
        return when (incoming.type) {
            StatusEffectType.BURN -> "FREEZE_OVERWRITTEN_BY_BURN"
            StatusEffectType.FREEZE -> "BURN_OVERWRITTEN_BY_FREEZE"
            else -> null
        }
    }

    private fun cleanseComparator(order: CleanseOrder): Comparator<StatusInstance> =
        when (order) {
            CleanseOrder.HARD_CONTROL_THEN_LONGEST ->
                compareBy<StatusInstance>(
                    { hardControlPriority(it) },
                    { -it.remainingTurns },
                    { it.appliedTurn },
                    { it.id },
                )

            CleanseOrder.LONGEST_REMAINING ->
                compareBy<StatusInstance>(
                    { -it.remainingTurns },
                    { it.appliedTurn },
                    { it.id },
                )

            CleanseOrder.MOST_RECENT ->
                compareByDescending<StatusInstance> { it.appliedTurn }.thenBy(StatusInstance::id)

            CleanseOrder.HIGHEST_MAGNITUDE ->
                compareByDescending<StatusInstance> { it.magnitude }.thenByDescending(StatusInstance::remainingTurns).thenBy(StatusInstance::id)
        }

    private fun hardControlPriority(effect: StatusInstance): Int =
        when (effect.type) {
            StatusEffectType.STUN,
            StatusEffectType.ROOT,
            -> 0

            else -> 1
        }

    private fun defaultStatModifier(
        type: StatusEffectType,
        magnitude: Double,
    ): StatModifier =
        when (type) {
            StatusEffectType.CUSTOM,
            StatusEffectType.STUN,
            StatusEffectType.OVERCHARGE,
            StatusEffectType.BLEED,
            StatusEffectType.BURN,
            StatusEffectType.POISON,
            StatusEffectType.ROOT,
            StatusEffectType.SILENCE,
            StatusEffectType.FREEZE,
            StatusEffectType.MARKED,
            StatusEffectType.TAUNT,
            StatusEffectType.INVULNERABLE,
            -> StatModifier.ZERO

            StatusEffectType.ARMOR_BREAK -> StatModifier(defense = -3)
            StatusEffectType.GUARD,
            StatusEffectType.GUARD_STANCE_BUFF,
            StatusEffectType.ARCANE_SHIELD_BUFF,
            StatusEffectType.HOLY_SHIELD_BUFF,
            StatusEffectType.HOLY_AURA_BUFF,
            -> StatModifier(defenseMultiplierBonus = magnitude)

            StatusEffectType.REGEN,
            StatusEffectType.UNYIELDING_BUFF,
            -> StatModifier(hpRegen = magnitude)

            StatusEffectType.HASTE ->
                StatModifier(
                    speed = maxOf(2, (magnitude * 10).roundToInt()),
                )

            StatusEffectType.SLOW ->
                StatModifier(
                    speed = -maxOf(2, (magnitude * 10).roundToInt()),
                )

            StatusEffectType.SHIELD -> StatModifier.ZERO
            StatusEffectType.BANE -> StatModifier(accuracy = -maxOf(1, (magnitude * 10).roundToInt()))
            StatusEffectType.CURSE ->
                StatModifier(
                    attackMultiplierBonus = -magnitude,
                    defenseMultiplierBonus = -magnitude,
                )

            StatusEffectType.WEAKEN -> StatModifier(attackMultiplierBonus = -magnitude)
            StatusEffectType.STEALTH ->
                StatModifier(
                    evasion = maxOf(2, (magnitude * 20).roundToInt()),
                    speed = maxOf(2, (magnitude * 10).roundToInt()),
                )

            StatusEffectType.MANA_SURGE_BUFF -> StatModifier(talentPower = magnitude)
            StatusEffectType.DEVOTION_BUFF ->
                StatModifier(
                    attackMultiplierBonus = magnitude,
                    accuracy = maxOf(1, (magnitude * 10).roundToInt()),
                )
        }
}

object StatusTickResolver {
    fun dueEffects(
        world: World,
        actorId: EntityId,
    ): List<CarrierDueEffect> {
        val actorEffects =
            world.get<StatusTracker>(actorId)
                ?.activeEffects()
                ?.filter { effect -> effect.tickTiming == StatusTickTiming.TURN_START }
                ?.sortedWith(compareBy<StatusInstance>({ it.tickPriority }, { it.appliedTurn }, { it.id }))
                ?.map { effect ->
                    CarrierDueEffect(
                        carrierKind = EffectCarrierKind.ACTOR,
                        effect = effect,
                        sourceEntityId = effect.sourceEntityId,
                        sourceKey = actorId.value.toString(),
                        orderKey = "%03d:%08d:%s".format(effect.tickPriority, effect.appliedTurn, effect.id),
                    )
                }.orEmpty()

        val areaEffects =
            world.entitiesWith(AreaEffectEmitter::class)
                .mapNotNull { entityId ->
                    world.get<AreaEffectEmitter>(entityId)?.takeIf { emitter -> actorId in emitter.affectedActorIds }?.let { emitter ->
                        entityId to emitter
                    }
                }.sortedWith(
                    compareBy<Pair<EntityId, AreaEffectEmitter>>(
                        { (_, emitter) -> emitter.emitterPriority },
                        { (_, emitter) -> emitter.sourceEntityId?.value ?: Int.MAX_VALUE },
                        { (_, emitter) -> emitter.emitterId },
                    ),
                ).flatMap { (_, emitter) ->
                    emitter.effects
                        .filter { effect -> effect.isActive() && effect.tickTiming == StatusTickTiming.TURN_START }
                        .sortedWith(compareBy<StatusInstance>({ it.tickPriority }, { it.appliedTurn }, { it.id }))
                        .map { effect ->
                            CarrierDueEffect(
                                carrierKind = EffectCarrierKind.AREA,
                                effect = effect,
                                sourceEntityId = emitter.sourceEntityId,
                                sourceKey = emitter.emitterId,
                                orderKey =
                                    "%03d:%08d:%s".format(
                                        emitter.emitterPriority,
                                        emitter.sourceEntityId?.value ?: Int.MAX_VALUE,
                                        emitter.emitterId,
                                    ),
                            )
                        }
                }

        val worldEffects =
            world.entitiesWith(WorldEffect::class)
                .mapNotNull { entityId ->
                    world.get<WorldEffect>(entityId)?.takeIf { effect -> actorId in effect.affectedActorIds }?.let { effect ->
                        entityId to effect
                    }
                }.sortedWith(
                    compareBy<Pair<EntityId, WorldEffect>>(
                        { (_, effect) -> effect.worldPriority },
                        { (_, effect) -> effect.effectId },
                    ),
                ).flatMap { (_, worldEffect) ->
                    worldEffect.effects
                        .filter { effect -> effect.isActive() && effect.tickTiming == StatusTickTiming.TURN_START }
                        .sortedWith(compareBy<StatusInstance>({ it.tickPriority }, { it.appliedTurn }, { it.id }))
                        .map { effect ->
                            CarrierDueEffect(
                                carrierKind = EffectCarrierKind.WORLD,
                                effect = effect,
                                sourceEntityId = null,
                                sourceKey = worldEffect.effectId,
                                orderKey = "%03d:%s".format(worldEffect.worldPriority, worldEffect.effectId),
                            )
                        }
                }

        return actorEffects + areaEffects + worldEffects
    }
}

private fun StatModifier.scaled(multiplier: Int): StatModifier =
    if (multiplier <= 1) {
        this
    } else {
        StatModifier(
            str = str * multiplier,
            dex = dex * multiplier,
            con = con * multiplier,
            wil = wil * multiplier,
            attack = attack * multiplier,
            defense = defense * multiplier,
            accuracy = accuracy * multiplier,
            evasion = evasion * multiplier,
            speed = speed * multiplier,
            maxHp = maxHp * multiplier,
            maxStamina = maxStamina * multiplier,
            hpRegen = hpRegen * multiplier,
            staminaRegen = staminaRegen * multiplier,
            critChance = critChance * multiplier,
            talentPower = talentPower * multiplier,
            attackMultiplierBonus = attackMultiplierBonus * multiplier,
            defenseMultiplierBonus = defenseMultiplierBonus * multiplier,
        )
    }
