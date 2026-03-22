package com.ktome.core.talent

import com.ktome.core.combat.CombatResolver
import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.stats.StatsCalculator

sealed interface TalentUseResult {
    data class Success(
        val result: TalentResult,
    ) : TalentUseResult

    data class Failure(
        val code: TalentFailureCode,
        val reason: String,
        val talentName: String? = null,
        val talentId: String? = null,
    ) : TalentUseResult
}

fun interface DamageMultiplierResolver {
    fun resolve(
        world: World,
        attacker: EntityId,
        target: EntityId,
        damageType: DamageType,
        baseMultiplier: Double,
    ): Double
}

enum class TalentFailureCode {
    UNKNOWN_TALENT,
    UNSUPPORTED_TALENT,
    COOLDOWN,
    NO_STAMINA,
    NO_RESOURCE,
    TARGET_REQUIRED,
    OUT_OF_RANGE,
    NO_TARGET,
    NO_CHARGE_PATH,
}

data class TalentResult(
    val talentId: String,
    val talentName: String,
    val user: EntityId,
    val targets: List<EntityId>,
    val effects: List<TalentEffectResult>,
)

sealed interface TalentEffectResult {
    data class Damage(
        val target: EntityId,
        val amount: Int,
        val crit: Boolean,
        val damageType: DamageType = DamageType.PHYSICAL,
        val resistanceValue: Int = 0,
    ) : TalentEffectResult

    data class Heal(
        val target: EntityId,
        val amount: Int,
    ) : TalentEffectResult

    data class Miss(
        val target: EntityId,
    ) : TalentEffectResult

    data class Knockback(
        val target: EntityId,
        val from: Point,
        val to: Point,
    ) : TalentEffectResult

    data class StatusApplied(
        val target: EntityId,
        val type: StatusEffectType,
        val duration: Int,
    ) : TalentEffectResult

    data class Buff(
        val target: EntityId,
        val type: StatusEffectType,
        val duration: Int,
        val magnitude: Double,
    ) : TalentEffectResult

    data class Movement(
        val entity: EntityId,
        val from: Point,
        val to: Point,
    ) : TalentEffectResult

    data class ResourceRestore(
        val target: EntityId,
        val resourceTypeId: String,
        val amount: Int,
    ) : TalentEffectResult
}

class TalentResolver(
    private val registry: TalentRegistry,
    private val combatResolver: CombatResolver,
) {
    var damageMultiplierResolver: DamageMultiplierResolver =
        DamageMultiplierResolver { _, _, _, _, baseMultiplier -> baseMultiplier }

    private val supportedTalentIds =
        setOf(
            "power_strike",
            "charge",
            "shield_bash",
            "war_cry",
            "sweeping_strike",
            "sunder_armor",
            "guard_stance",
            "intimidation",
            "unyielding",
            "fireball",
            "flame_wall",
            "ice_bolt",
            "frost_nova",
            "ice_prison",
            "arcane_shield",
            "blink",
            "mana_surge",
            "backstab",
            "poison_blade",
            "stealth",
            "smoke_bomb",
            "roll",
            "blade_flurry",
            "shadowstep",
            "deathblow",
            "holy_strike",
            "judgment_hammer",
            "holy_light",
            "holy_shield",
            "devotion",
            "holy_aura",
            "purify",
            "divine_intervention",
        )

    private data class DamageResolution(
        val hit: Boolean,
        val finalDamage: Int = 0,
        val critical: Boolean = false,
    )

    private fun effectiveRange(
        definition: TalentDef,
        level: Int,
    ): Int = definition.range + (definition.levelEffects[level]?.rangeBonus ?: 0)

    fun canUse(
        world: World,
        map: GameMap,
        user: EntityId,
        talentId: String,
        target: Point?,
    ): String? = canUseFailure(world, map, user, talentId, target)?.reason

    private fun canUseFailure(
        world: World,
        map: GameMap,
        user: EntityId,
        talentId: String,
        target: Point?,
    ): TalentUseResult.Failure? {
        val definition =
            registry.get(talentId)
                ?: return TalentUseResult.Failure(
                    code = TalentFailureCode.UNKNOWN_TALENT,
                    reason = "Unknown talent.",
                )
        if (talentId !in supportedTalentIds) {
            return TalentUseResult.Failure(
                code = TalentFailureCode.UNSUPPORTED_TALENT,
                reason = "Talent is not supported yet.",
                talentName = definition.name,
                talentId = talentId,
            )
        }
        val cooldowns = requireNotNull(world.get<CooldownState>(user)) { "Missing CooldownState for $user" }

        if (cooldowns.remainingByTalentId[talentId]?.let { it > 0 } == true) {
            return TalentUseResult.Failure(
                code = TalentFailureCode.COOLDOWN,
                reason = "${definition.name} is still cooling down.",
                talentName = definition.name,
                talentId = talentId,
            )
        }
        val loadout = requireNotNull(world.get<TalentLoadout>(user)) { "Missing TalentLoadout for $user" }
        val level = loadout.levelOf(talentId).coerceIn(1, definition.maxLevel)
        val resourceFailure = insufficientResourceFailure(world, user, definition)
        if (resourceFailure != null) {
            return TalentUseResult.Failure(
                code = resourceFailure,
                reason =
                    if (resourceFailure == TalentFailureCode.NO_STAMINA) {
                        "Not enough stamina."
                    } else {
                        "Not enough resource."
                    },
                talentName = definition.name,
                talentId = talentId,
            )
        }
        val range = effectiveRange(definition, level)
        if (range == 0) {
            return null
        }

        val userPosition = requireNotNull(world.get<Position>(user)).toPoint()
        val targetPoint =
            target
                ?: return TalentUseResult.Failure(
                    code = TalentFailureCode.TARGET_REQUIRED,
                    reason = "A target is required.",
                    talentName = definition.name,
                    talentId = talentId,
                )
        val distance = userPosition.chebyshevDistanceTo(targetPoint)
        if (distance > range || distance < definition.minRange) {
            return TalentUseResult.Failure(
                code = TalentFailureCode.OUT_OF_RANGE,
                reason = "Target is out of range.",
                talentName = definition.name,
                talentId = talentId,
            )
        }

        if (definition.id == "blink" || definition.id == "roll") {
            if (blinkDestination(world, map, userPosition, targetPoint, user) == null) {
                return TalentUseResult.Failure(
                    code = TalentFailureCode.NO_TARGET,
                    reason = "No valid movement destination.",
                    talentName = definition.name,
                    talentId = talentId,
                )
            }
            return null
        }

        val targetEntity =
            hostileTargetAt(world, user, targetPoint)
                ?: return TalentUseResult.Failure(
                    code = TalentFailureCode.NO_TARGET,
                    reason = "No valid target.",
                    talentName = definition.name,
                    talentId = talentId,
                )
        if (definition.id == "charge" && chargeDestination(world, map, userPosition, targetPoint, targetEntity) == null) {
            return TalentUseResult.Failure(
                code = TalentFailureCode.NO_CHARGE_PATH,
                reason = "No path to charge target.",
                talentName = definition.name,
                talentId = talentId,
            )
        }
        if (definition.id == "shadowstep" && shadowstepDestination(world, map, userPosition, targetPoint, targetEntity) == null) {
            return TalentUseResult.Failure(
                code = TalentFailureCode.NO_TARGET,
                reason = "No valid shadowstep landing point.",
                talentName = definition.name,
                talentId = talentId,
            )
        }

        return null
    }

    fun resolve(
        world: World,
        map: GameMap,
        user: EntityId,
        talentId: String,
        target: Point?,
    ): TalentUseResult {
        val failure = canUseFailure(world, map, user, talentId, target)
        if (failure != null) {
            return failure
        }

        val definition = requireNotNull(registry.get(talentId))
        val loadout = requireNotNull(world.get<TalentLoadout>(user)) { "Missing TalentLoadout for $user" }
        val cooldowns = requireNotNull(world.get<CooldownState>(user)) { "Missing CooldownState for $user" }
        val level = loadout.levelOf(talentId).coerceIn(1, definition.maxLevel)
        val effect = requireNotNull(definition.levelEffects[level]) { "Missing level effect for $talentId level $level" }
        val effects = mutableListOf<TalentEffectResult>()
        val targets = linkedSetOf<EntityId>()

        if (talentId !in supportedTalentIds) {
            return TalentUseResult.Failure(
                code = TalentFailureCode.UNSUPPORTED_TALENT,
                reason = "Talent is not supported yet.",
                talentName = definition.name,
                talentId = talentId,
            )
        }

        spendResources(world, user, definition)
        cooldowns.remainingByTalentId[talentId] = definition.cooldown

        when (talentId) {
            "power_strike" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                if (damageResult.hit) {
                    if (effect.knockback > 0) {
                        knockback(world, map, user, targetEntity, effect.knockback)?.let(effects::add)
                    }
                    if (effect.armorBreakDuration > 0) {
                        applyArmorBreak(world, targetEntity, effect.armorBreakDuration, effects)
                    }
                }
            }

            "charge" -> {
                val targetPoint = requireNotNull(target)
                val targetEntity = requireNotNull(hostileTargetAt(world, user, targetPoint))
                targets += targetEntity
                val from = requireNotNull(world.get<Position>(user)).toPoint()
                val destination = requireNotNull(chargeDestination(world, map, from, targetPoint, targetEntity))
                requireNotNull(world.get<Position>(user)).moveTo(destination)
                effects += TalentEffectResult.Movement(user, from, destination)
                val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                if (damageResult.hit && effect.stunDuration > 0) {
                    applyStun(world, targetEntity, effect.stunDuration, effects, effectId = "charge_stun")
                }
            }

            "shield_bash" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                if (damageResult.hit) {
                    if (effect.stunDuration > 0) {
                        applyStun(world, targetEntity, effect.stunDuration, effects, effectId = "shield_bash_stun")
                    }
                    if (effect.knockback > 0) {
                        knockback(world, map, user, targetEntity, effect.knockback)?.let(effects::add)
                    }
                }
            }

            "war_cry" -> {
                applyWarCry(world, user, definition.areaRadius, effect, effects, targets)
            }

            "sweeping_strike" -> {
                val center = requireNotNull(target)
                val hitTargets = hostileTargetsWithin(world, user, center, definition.areaRadius).ifEmpty {
                    listOfNotNull(hostileTargetAt(world, user, center))
                }
                hitTargets.forEach { targetEntity ->
                    targets += targetEntity
                    val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                    if (damageResult.hit && effect.knockback > 0) {
                        knockback(world, map, user, targetEntity, effect.knockback)?.let(effects::add)
                    }
                }
            }

            "sunder_armor" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                if (damageResult.hit && effect.armorBreakDuration > 0) {
                    applyArmorBreak(world, targetEntity, effect.armorBreakDuration, effects, effectId = "sunder_armor_break")
                }
            }

            "guard_stance" -> {
                applySelfBuff(
                    world = world,
                    target = user,
                    effectId = "guard_stance_buff",
                    name = "Guard Stance",
                    type = StatusEffectType.GUARD_STANCE_BUFF,
                    duration = effect.buffDuration,
                    magnitude = effect.buffMagnitude,
                    statModifiers = StatModifier(defenseMultiplierBonus = effect.buffMagnitude),
                    effects = effects,
                )
                targets += user
            }

            "intimidation" -> {
                val origin = requireNotNull(world.get<Position>(user)).toPoint()
                hostileTargetsWithin(world, user, origin, definition.areaRadius).forEach { enemy ->
                    applyDebuff(
                        world = world,
                        target = enemy,
                        effectId = "intimidation_debuff",
                        name = "Intimidated",
                        type = StatusEffectType.WAR_CRY_DEBUFF,
                        duration = effect.debuffDuration,
                        magnitude = effect.debuffMagnitude,
                        effects = effects,
                    )
                    targets += enemy
                }
            }

            "unyielding" -> {
                applySelfBuff(
                    world = world,
                    target = user,
                    effectId = "unyielding_buff",
                    name = "Unyielding",
                    type = StatusEffectType.UNYIELDING_BUFF,
                    duration = effect.buffDuration,
                    magnitude = effect.buffMagnitude,
                    statModifiers = StatModifier(defenseMultiplierBonus = effect.buffMagnitude),
                    effects = effects,
                )
                targets += user
            }

            "fireball" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
            }

            "flame_wall" -> {
                val center = requireNotNull(target)
                hostileTargetsWithin(world, user, center, definition.areaRadius).forEach { targetEntity ->
                    targets += targetEntity
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                }
            }

            "ice_bolt" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                if (damageResult.hit && effect.stunDuration > 0) {
                    applyStun(world, targetEntity, effect.stunDuration, effects, effectId = "ice_bolt_stun")
                }
            }

            "frost_nova" -> {
                val origin = requireNotNull(world.get<Position>(user)).toPoint()
                hostileTargetsWithin(world, user, origin, definition.areaRadius).forEach { targetEntity ->
                    targets += targetEntity
                    val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                    if (damageResult.hit && effect.stunDuration > 0) {
                        applyStun(world, targetEntity, effect.stunDuration, effects, effectId = "frost_nova_stun")
                    }
                }
            }

            "ice_prison" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                if (damageResult.hit && effect.stunDuration > 0) {
                    applyStun(world, targetEntity, effect.stunDuration, effects, effectId = "ice_prison_stun")
                }
            }

            "blink" -> {
                val from = requireNotNull(world.get<Position>(user)).toPoint()
                val destination = requireNotNull(blinkDestination(world, map, from, requireNotNull(target), user))
                requireNotNull(world.get<Position>(user)).moveTo(destination)
                effects += TalentEffectResult.Movement(user, from, destination)
                targets += user
            }

            "arcane_shield" -> {
                applySelfBuff(
                    world = world,
                    target = user,
                    effectId = "arcane_shield_buff",
                    name = "Arcane Shield",
                    type = StatusEffectType.ARCANE_SHIELD_BUFF,
                    duration = effect.buffDuration,
                    magnitude = effect.buffMagnitude,
                    statModifiers = StatModifier(defenseMultiplierBonus = effect.buffMagnitude),
                    effects = effects,
                )
                targets += user
            }

            "mana_surge" -> {
                applySelfBuff(
                    world = world,
                    target = user,
                    effectId = "mana_surge_buff",
                    name = "Mana Surge",
                    type = StatusEffectType.MANA_SURGE_BUFF,
                    duration = effect.buffDuration,
                    magnitude = effect.buffMagnitude,
                    statModifiers = StatModifier(talentPower = effect.buffMagnitude),
                    effects = effects,
                )
                restoreResource(world, user, ResourceType.MANA, effect, effects)
                targets += user
            }

            "backstab" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
            }

            "poison_blade" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                if (damageResult.hit && effect.debuffDuration > 0) {
                    applyCurse(world, targetEntity, effect.debuffDuration, effect.debuffMagnitude, effects, effectId = "poison_blade_curse")
                }
            }

            "stealth" -> {
                applySelfBuff(
                    world = world,
                    target = user,
                    effectId = "stealth_buff",
                    name = "Stealth",
                    type = StatusEffectType.STEALTH_BUFF,
                    duration = effect.buffDuration,
                    magnitude = effect.buffMagnitude,
                    statModifiers =
                        StatModifier(
                            evasion = maxOf(2, (effect.buffMagnitude * 20).toInt()),
                            speed = maxOf(2, (effect.buffMagnitude * 10).toInt()),
                        ),
                    effects = effects,
                )
                targets += user
            }

            "smoke_bomb" -> {
                val origin = requireNotNull(world.get<Position>(user)).toPoint()
                hostileTargetsWithin(world, user, origin, definition.areaRadius).forEach { enemy ->
                    applyCurse(world, enemy, effect.debuffDuration, effect.debuffMagnitude, effects, effectId = "smoke_bomb_curse")
                    targets += enemy
                }
            }

            "roll" -> {
                val from = requireNotNull(world.get<Position>(user)).toPoint()
                val destination = requireNotNull(blinkDestination(world, map, from, requireNotNull(target), user))
                requireNotNull(world.get<Position>(user)).moveTo(destination)
                effects += TalentEffectResult.Movement(user, from, destination)
                targets += user
            }

            "blade_flurry" -> {
                val center = requireNotNull(target)
                val hitTargets = hostileTargetsWithin(world, user, center, definition.areaRadius).ifEmpty {
                    listOfNotNull(hostileTargetAt(world, user, center))
                }
                hitTargets.forEach { targetEntity ->
                    targets += targetEntity
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                }
            }

            "shadowstep" -> {
                val targetPoint = requireNotNull(target)
                val targetEntity = requireNotNull(hostileTargetAt(world, user, targetPoint))
                val from = requireNotNull(world.get<Position>(user)).toPoint()
                val destination = requireNotNull(shadowstepDestination(world, map, from, targetPoint, targetEntity))
                requireNotNull(world.get<Position>(user)).moveTo(destination)
                effects += TalentEffectResult.Movement(user, from, destination)
                targets += user
                targets += targetEntity
                resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
            }

            "deathblow" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                if (damageResult.hit) {
                    restoreResource(world, user, ResourceType.ENERGY, effect, effects)
                }
            }

            "holy_strike" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
            }

            "judgment_hammer" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects)
                if (damageResult.hit && effect.stunDuration > 0) {
                    applyStun(world, targetEntity, effect.stunDuration, effects, effectId = "judgment_hammer_stun")
                }
            }

            "holy_light" -> {
                healTarget(world, user, effect, effects)
                targets += user
            }

            "holy_shield" -> {
                applySelfBuff(
                    world = world,
                    target = user,
                    effectId = "holy_shield_buff",
                    name = "Holy Shield",
                    type = StatusEffectType.HOLY_SHIELD_BUFF,
                    duration = effect.buffDuration,
                    magnitude = effect.buffMagnitude,
                    statModifiers = StatModifier(defenseMultiplierBonus = effect.buffMagnitude),
                    effects = effects,
                )
                targets += user
            }

            "devotion" -> {
                applySelfBuff(
                    world = world,
                    target = user,
                    effectId = "devotion_buff",
                    name = "Devotion",
                    type = StatusEffectType.DEVOTION_BUFF,
                    duration = effect.buffDuration,
                    magnitude = effect.buffMagnitude,
                    statModifiers =
                        StatModifier(
                            attackMultiplierBonus = effect.buffMagnitude,
                            accuracy = maxOf(1, (effect.buffMagnitude * 10).toInt()),
                        ),
                    effects = effects,
                )
                targets += user
            }

            "holy_aura" -> {
                applySelfBuff(
                    world = world,
                    target = user,
                    effectId = "holy_aura_buff",
                    name = "Holy Aura",
                    type = StatusEffectType.HOLY_AURA_BUFF,
                    duration = effect.buffDuration,
                    magnitude = effect.buffMagnitude,
                    statModifiers = StatModifier(defenseMultiplierBonus = effect.buffMagnitude),
                    effects = effects,
                )
                targets += user
                val origin = requireNotNull(world.get<Position>(user)).toPoint()
                hostileTargetsWithin(world, user, origin, definition.areaRadius).forEach { enemy ->
                    targets += enemy
                    resolveDamage(world, user, enemy, definition.damageType, effect.damageMultiplier, effects)
                }
            }

            "purify" -> {
                clearNegativeEffects(world, user)
                healTarget(world, user, effect, effects)
                targets += user
            }

            "divine_intervention" -> {
                healTarget(world, user, effect, effects)
                applySelfBuff(
                    world = world,
                    target = user,
                    effectId = "divine_intervention_holy_shield",
                    name = "Divine Intervention",
                    type = StatusEffectType.HOLY_SHIELD_BUFF,
                    duration = effect.buffDuration,
                    magnitude = effect.buffMagnitude,
                    statModifiers = StatModifier(defenseMultiplierBonus = effect.buffMagnitude),
                    effects = effects,
                )
                targets += user
            }

            else ->
                return TalentUseResult.Failure(
                    code = TalentFailureCode.UNSUPPORTED_TALENT,
                    reason = "Talent is not supported yet.",
                    talentName = definition.name,
                    talentId = talentId,
                )
        }

        StatsCalculator.recalculateAndStore(world, user)
        return TalentUseResult.Success(
            TalentResult(
                talentId = definition.id,
                talentName = definition.name,
                user = user,
                targets = targets.toList(),
                effects = effects,
            ),
        )
    }

    private fun applyArmorBreak(
        world: World,
        target: EntityId,
        duration: Int,
        effects: MutableList<TalentEffectResult>,
        effectId: String = "power_strike_armor_break",
    ) {
        applyEffect(
            world = world,
            target = target,
            effect =
                ActiveEffect(
                    id = effectId,
                    name = "Armor Break",
                    type = StatusEffectType.ARMOR_BREAK,
                    remainingTurns = duration,
                    statModifiers = StatModifier(defense = -3),
                ),
        )
        effects += TalentEffectResult.StatusApplied(target, StatusEffectType.ARMOR_BREAK, duration)
    }

    private fun applyStun(
        world: World,
        target: EntityId,
        duration: Int,
        effects: MutableList<TalentEffectResult>,
        effectId: String,
    ) {
        applyEffect(
            world = world,
            target = target,
            effect =
                ActiveEffect(
                    id = effectId,
                    name = "Stunned",
                    type = StatusEffectType.STUNNED,
                    remainingTurns = duration,
                ),
        )
        effects += TalentEffectResult.StatusApplied(target, StatusEffectType.STUNNED, duration)
    }

    private fun applySelfBuff(
        world: World,
        target: EntityId,
        effectId: String,
        name: String,
        type: StatusEffectType,
        duration: Int,
        magnitude: Double,
        statModifiers: StatModifier,
        effects: MutableList<TalentEffectResult>,
    ) {
        applyEffect(
            world = world,
            target = target,
            effect =
                ActiveEffect(
                    id = effectId,
                    name = name,
                    type = type,
                    remainingTurns = duration,
                    statModifiers = statModifiers,
                    skipNextDecay = true,
                ),
        )
        effects += TalentEffectResult.Buff(target, type, duration, magnitude)
    }

    private fun applyDebuff(
        world: World,
        target: EntityId,
        effectId: String,
        name: String,
        type: StatusEffectType,
        duration: Int,
        magnitude: Double,
        effects: MutableList<TalentEffectResult>,
    ) {
        applyEffect(
            world = world,
            target = target,
            effect =
                ActiveEffect(
                    id = effectId,
                    name = name,
                    type = type,
                    remainingTurns = duration,
                    statModifiers = StatModifier(defenseMultiplierBonus = -magnitude),
                ),
        )
        effects += TalentEffectResult.Buff(target, type, duration, magnitude)
    }

    private fun applyCurse(
        world: World,
        target: EntityId,
        duration: Int,
        magnitude: Double,
        effects: MutableList<TalentEffectResult>,
        effectId: String,
    ) {
        applyEffect(
            world = world,
            target = target,
            effect =
                ActiveEffect(
                    id = effectId,
                    name = "Cursed",
                    type = StatusEffectType.CURSED,
                    remainingTurns = duration,
                    statModifiers =
                        StatModifier(
                            attackMultiplierBonus = -magnitude,
                            defenseMultiplierBonus = -magnitude,
                        ),
                ),
        )
        effects += TalentEffectResult.StatusApplied(target, StatusEffectType.CURSED, duration)
    }

    private fun applyWarCry(
        world: World,
        user: EntityId,
        areaRadius: Int,
        effect: TalentLevelEffect,
        effects: MutableList<TalentEffectResult>,
        targets: LinkedHashSet<EntityId>,
    ) {
        applySelfBuff(
            world = world,
            target = user,
            effectId = "war_cry_buff",
            name = "War Cry",
            type = StatusEffectType.WAR_CRY_BUFF,
            duration = effect.buffDuration,
            magnitude = effect.buffMagnitude,
            statModifiers = StatModifier(attackMultiplierBonus = effect.buffMagnitude),
            effects = effects,
        )
        targets += user

        if (effect.debuffDuration > 0 && effect.debuffMagnitude > 0.0) {
            val origin = requireNotNull(world.get<Position>(user)).toPoint()
            hostileTargetsWithin(world, user, origin, areaRadius).forEach { enemy ->
                applyDebuff(
                    world = world,
                    target = enemy,
                    effectId = "war_cry_debuff",
                    name = "Shaken",
                    type = StatusEffectType.WAR_CRY_DEBUFF,
                    duration = effect.debuffDuration,
                    magnitude = effect.debuffMagnitude,
                    effects = effects,
                )
                targets += enemy
            }
        }
    }

    private fun restoreResource(
        world: World,
        user: EntityId,
        resourceType: ResourceType,
        effect: TalentLevelEffect,
        effects: MutableList<TalentEffectResult>,
    ) {
        val pools = world.get<ResourcePools>(user) ?: return
        val pool = pools.pool(resourceType) ?: return
        val before = pool.current
        val amount = maxOf(8, (pool.max * effect.buffMagnitude).toInt().coerceAtLeast(0))
        pool.restore(amount)
        val restored = pool.current - before
        if (restored > 0) {
            effects += TalentEffectResult.ResourceRestore(user, resourceType.name, restored)
        }
    }

    private fun healTarget(
        world: World,
        target: EntityId,
        effect: TalentLevelEffect,
        effects: MutableList<TalentEffectResult>,
    ) {
        val health = world.get<Health>(target) ?: return
        val before = health.current
        val amount = maxOf(10, (health.max * effect.buffMagnitude).toInt().coerceAtLeast(0))
        health.current = (health.current + amount).coerceAtMost(health.max)
        val restored = health.current - before
        if (restored > 0) {
            effects += TalentEffectResult.Heal(target, restored)
        }
    }

    private fun insufficientResourceFailure(
        world: World,
        user: EntityId,
        definition: TalentDef,
    ): TalentFailureCode? {
        for ((type, cost) in definition.resolvedResourceCosts()) {
            if (resourceAmount(world, user, type) >= cost) {
                continue
            }
            return if (type == ResourceType.STAMINA) {
                TalentFailureCode.NO_STAMINA
            } else {
                TalentFailureCode.NO_RESOURCE
            }
        }
        return null
    }

    private fun spendResources(
        world: World,
        user: EntityId,
        definition: TalentDef,
    ) {
        definition.resolvedResourceCosts().forEach { (type, cost) ->
            val pool =
                requireNotNull(world.get<ResourcePools>(user)?.pool(type)) {
                    "Missing ResourcePool '$type' for $user"
                }
            pool.spend(cost)
        }
    }

    private fun resourceAmount(
        world: World,
        user: EntityId,
        type: ResourceType,
    ): Int = world.get<ResourcePools>(user)?.pool(type)?.current ?: 0

    private fun resolveDamage(
        world: World,
        attacker: EntityId,
        target: EntityId,
        damageType: DamageType,
        damageMultiplier: Double,
        effects: MutableList<TalentEffectResult>,
    ): DamageResolution {
        val effectiveMultiplier =
            damageMultiplierResolver.resolve(
                world = world,
                attacker = attacker,
                target = target,
                damageType = damageType,
                baseMultiplier = damageMultiplier,
            )
        val result = combatResolver.resolveMelee(world, attacker, target, damageType, effectiveMultiplier)
        if (!result.hit) {
            effects += TalentEffectResult.Miss(target)
            return DamageResolution(hit = false)
        }

        val health = requireNotNull(world.get<Health>(target)) { "Missing Health for $target" }
        val damage = requireNotNull(result.damage) { "Missing DamageResult for successful hit." }
        health.current = (health.current - damage.finalDamage).coerceAtLeast(0)
        effects +=
            TalentEffectResult.Damage(
                target = target,
                amount = damage.finalDamage,
                crit = result.critical,
                damageType = damage.type,
                resistanceValue = damage.resistanceValue,
            )
        return DamageResolution(
            hit = true,
            finalDamage = damage.finalDamage,
            critical = result.critical,
        )
    }

    private fun applyEffect(
        world: World,
        target: EntityId,
        effect: ActiveEffect,
    ) {
        val tracker = world.get<EffectTracker>(target) ?: EffectTracker().also { world.add(target, it) }
        tracker.effects.removeAll { it.type == effect.type }
        tracker.effects += effect
        StatsCalculator.recalculateAndStore(world, target)
    }

    private fun hostileTargetAt(
        world: World,
        user: EntityId,
        point: Point,
    ): EntityId? {
        val userFaction = requireNotNull(world.get<FactionTag>(user)) { "Missing FactionTag for $user" }.value
        return world.entitiesWith(Position::class, FactionTag::class, Health::class)
            .firstOrNull { entityId ->
                entityId != user &&
                    requireNotNull(world.get<Health>(entityId)).current > 0 &&
                    requireNotNull(world.get<Position>(entityId)).toPoint() == point &&
                    requireNotNull(world.get<FactionTag>(entityId)).value != userFaction
            }
    }

    private fun hostileTargetsWithin(
        world: World,
        user: EntityId,
        origin: Point,
        radius: Int,
    ): List<EntityId> {
        val userFaction = requireNotNull(world.get<FactionTag>(user)) { "Missing FactionTag for $user" }.value
        return world.entitiesWith(Position::class, FactionTag::class, Health::class)
            .filter { entityId ->
                entityId != user &&
                    requireNotNull(world.get<Health>(entityId)).current > 0 &&
                    requireNotNull(world.get<FactionTag>(entityId)).value != userFaction &&
                    requireNotNull(world.get<Position>(entityId)).toPoint().chebyshevDistanceTo(origin) <= radius
            }
    }

    private fun chargeDestination(
        world: World,
        map: GameMap,
        from: Point,
        targetPoint: Point,
        targetEntity: EntityId,
    ): Point? {
        val blockedTiles = blockedTiles(world, excluding = setOf(targetEntity))

        return Point.ALL_DIRECTIONS
            .map { direction -> targetPoint + direction }
            .filter { destination ->
                destination != from &&
                    map.isInBounds(destination.x, destination.y) &&
                    !map[destination].blocksMovement &&
                    destination !in blockedTiles
            }
            .mapNotNull { destination ->
                val path = AStar.findPath(map = map, start = from, goal = destination, blocked = blockedTiles)
                destination.takeIf { path.isNotEmpty() }?.let { reachableDestination ->
                    reachableDestination to path.size
                }
            }
            .minWithOrNull(compareBy<Pair<Point, Int>> { it.second }.thenBy { it.first.y }.thenBy { it.first.x })
            ?.first
    }

    private fun blinkDestination(
        world: World,
        map: GameMap,
        from: Point,
        targetPoint: Point,
        user: EntityId,
    ): Point? {
        val blockedTiles = blockedTiles(world, excluding = setOf(user))
        val candidates =
            sequenceOf(targetPoint)
                .plus(Point.ALL_DIRECTIONS.asSequence().map { direction -> targetPoint + direction })
                .distinct()
                .filter { destination ->
                    destination != from &&
                        map.isInBounds(destination.x, destination.y) &&
                        !map[destination].blocksMovement &&
                        destination !in blockedTiles
                }

        return candidates.minWithOrNull(compareBy<Point> { it.chebyshevDistanceTo(targetPoint) }.thenBy(Point::y).thenBy(Point::x))
    }

    private fun shadowstepDestination(
        world: World,
        map: GameMap,
        from: Point,
        targetPoint: Point,
        targetEntity: EntityId,
    ): Point? {
        val blockedTiles = blockedTiles(world, excluding = setOf(targetEntity))
        return Point.ALL_DIRECTIONS
            .map { direction -> targetPoint + direction }
            .filter { destination ->
                destination != from &&
                    map.isInBounds(destination.x, destination.y) &&
                    !map[destination].blocksMovement &&
                    destination !in blockedTiles
            }.minWithOrNull(compareBy<Point> { it.chebyshevDistanceTo(from) }.thenBy(Point::y).thenBy(Point::x))
    }

    private fun knockback(
        world: World,
        map: GameMap,
        user: EntityId,
        target: EntityId,
        distance: Int,
    ): TalentEffectResult.Knockback? {
        val userPoint = requireNotNull(world.get<Position>(user)).toPoint()
        val targetPosition = requireNotNull(world.get<Position>(target))
        val from = targetPosition.toPoint()
        val direction = Point((from.x - userPoint.x).coerceIn(-1, 1), (from.y - userPoint.y).coerceIn(-1, 1))
        var destination = from
        repeat(distance) {
            val next = destination + direction
            val blocked =
                !map.isInBounds(next.x, next.y) ||
                    map[next].blocksMovement ||
                    world.entitiesWith(Position::class, BlocksMovement::class)
                        .any { entityId -> entityId != target && requireNotNull(world.get<Position>(entityId)).toPoint() == next }
            if (blocked) {
                return@repeat
            }
            destination = next
        }

        if (destination == from) {
            return null
        }

        targetPosition.moveTo(destination)
        return TalentEffectResult.Knockback(target, from, destination)
    }

    private fun blockedTiles(
        world: World,
        excluding: Set<EntityId>,
    ): Set<Point> =
        world.entitiesWith(Position::class, BlocksMovement::class)
            .filter { entityId -> entityId !in excluding }
            .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
            .toSet()

    private fun clearNegativeEffects(
        world: World,
        target: EntityId,
    ) {
        val tracker = world.get<EffectTracker>(target) ?: return
        val removed =
            tracker.effects.removeAll { effect ->
                effect.type in
                    setOf(
                        StatusEffectType.STUNNED,
                        StatusEffectType.ARMOR_BREAK,
                        StatusEffectType.WAR_CRY_DEBUFF,
                        StatusEffectType.CURSED,
                    )
            }
        if (removed) {
            StatsCalculator.recalculateAndStore(world, target)
        }
    }
}
