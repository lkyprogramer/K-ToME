package com.ktome.core.talent

import com.ktome.core.combat.ApplicationPolicy
import com.ktome.core.combat.CombatResolver
import com.ktome.core.combat.DamageType
import com.ktome.core.combat.SaveDimension
import com.ktome.core.combat.StatusApplicationRequest
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

    private companion object {
        val BUFF_LIKE_EFFECT_TYPES =
            setOf(
                StatusEffectType.WAR_CRY_BUFF,
                StatusEffectType.WAR_CRY_DEBUFF,
                StatusEffectType.GUARD_STANCE_BUFF,
                StatusEffectType.ARCANE_SHIELD_BUFF,
                StatusEffectType.UNYIELDING_BUFF,
                StatusEffectType.MANA_SURGE_BUFF,
                StatusEffectType.STEALTH_BUFF,
                StatusEffectType.HOLY_SHIELD_BUFF,
                StatusEffectType.DEVOTION_BUFF,
                StatusEffectType.HOLY_AURA_BUFF,
            )
        val NEGATIVE_EFFECT_TYPES =
            setOf(
                StatusEffectType.STUNNED,
                StatusEffectType.ARMOR_BREAK,
                StatusEffectType.WAR_CRY_DEBUFF,
                StatusEffectType.CURSED,
            )
    }

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
                val damageResult =
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                if (damageResult.hit && effect.knockback > 0) {
                    knockback(world, map, user, targetEntity, effect.knockback)?.let(effects::add)
                }
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_HIT,
                    primaryTarget = targetEntity,
                    areaTargets = listOf(targetEntity),
                    hitSucceeded = damageResult.hit,
                    effects = effects,
                )
            }

            "charge" -> {
                val targetPoint = requireNotNull(target)
                val targetEntity = requireNotNull(hostileTargetAt(world, user, targetPoint))
                targets += targetEntity
                val from = requireNotNull(world.get<Position>(user)).toPoint()
                val destination = requireNotNull(chargeDestination(world, map, from, targetPoint, targetEntity))
                requireNotNull(world.get<Position>(user)).moveTo(destination)
                effects += TalentEffectResult.Movement(user, from, destination)
                val damageResult =
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_HIT,
                    primaryTarget = targetEntity,
                    areaTargets = listOf(targetEntity),
                    hitSucceeded = damageResult.hit,
                    effects = effects,
                )
            }

            "shield_bash" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult =
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                if (damageResult.hit && effect.knockback > 0) {
                    knockback(world, map, user, targetEntity, effect.knockback)?.let(effects::add)
                }
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_HIT,
                    primaryTarget = targetEntity,
                    areaTargets = listOf(targetEntity),
                    hitSucceeded = damageResult.hit,
                    effects = effects,
                )
            }

            "war_cry" -> {
                val nearbyTargets = hostileTargetsWithin(world, user, requireNotNull(world.get<Position>(user)).toPoint(), definition.areaRadius)
                targets += user
                targets += nearbyTargets
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    areaTargets = nearbyTargets,
                    effects = effects,
                )
            }

            "sweeping_strike" -> {
                val center = requireNotNull(target)
                val hitTargets = hostileTargetsWithin(world, user, center, definition.areaRadius).ifEmpty {
                    listOfNotNull(hostileTargetAt(world, user, center))
                }
                hitTargets.forEach { targetEntity ->
                    targets += targetEntity
                    val damageResult =
                        resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                    if (damageResult.hit && effect.knockback > 0) {
                        knockback(world, map, user, targetEntity, effect.knockback)?.let(effects::add)
                    }
                }
            }

            "sunder_armor" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult =
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_HIT,
                    primaryTarget = targetEntity,
                    areaTargets = listOf(targetEntity),
                    hitSucceeded = damageResult.hit,
                    effects = effects,
                )
            }

            "guard_stance" -> {
                targets += user
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
            }

            "intimidation" -> {
                val nearbyTargets = hostileTargetsWithin(world, user, requireNotNull(world.get<Position>(user)).toPoint(), definition.areaRadius)
                targets += nearbyTargets
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    areaTargets = nearbyTargets,
                    effects = effects,
                )
            }

            "unyielding" -> {
                targets += user
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
            }

            "fireball" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
            }

            "flame_wall" -> {
                val center = requireNotNull(target)
                hostileTargetsWithin(world, user, center, definition.areaRadius).forEach { targetEntity ->
                    targets += targetEntity
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                }
            }

            "ice_bolt" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult =
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_HIT,
                    primaryTarget = targetEntity,
                    areaTargets = listOf(targetEntity),
                    hitSucceeded = damageResult.hit,
                    effects = effects,
                )
            }

            "frost_nova" -> {
                val origin = requireNotNull(world.get<Position>(user)).toPoint()
                hostileTargetsWithin(world, user, origin, definition.areaRadius).forEach { targetEntity ->
                    targets += targetEntity
                    val damageResult =
                        resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                    applyConfiguredEffects(
                        world = world,
                        user = user,
                        definition = definition,
                        effect = effect,
                        trigger = EffectTrigger.ON_HIT,
                        primaryTarget = targetEntity,
                        areaTargets = listOf(targetEntity),
                        hitSucceeded = damageResult.hit,
                        effects = effects,
                    )
                }
            }

            "ice_prison" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult =
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_HIT,
                    primaryTarget = targetEntity,
                    areaTargets = listOf(targetEntity),
                    hitSucceeded = damageResult.hit,
                    effects = effects,
                )
            }

            "blink" -> {
                val from = requireNotNull(world.get<Position>(user)).toPoint()
                val destination = requireNotNull(blinkDestination(world, map, from, requireNotNull(target), user))
                requireNotNull(world.get<Position>(user)).moveTo(destination)
                effects += TalentEffectResult.Movement(user, from, destination)
                targets += user
            }

            "arcane_shield" -> {
                targets += user
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
            }

            "mana_surge" -> {
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
                restoreResource(world, user, ResourceType.MANA, effect, effects)
                targets += user
            }

            "backstab" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
            }

            "poison_blade" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult =
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_HIT,
                    primaryTarget = targetEntity,
                    areaTargets = listOf(targetEntity),
                    hitSucceeded = damageResult.hit,
                    effects = effects,
                )
            }

            "stealth" -> {
                targets += user
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
            }

            "smoke_bomb" -> {
                val nearbyTargets = hostileTargetsWithin(world, user, requireNotNull(world.get<Position>(user)).toPoint(), definition.areaRadius)
                targets += nearbyTargets
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    areaTargets = nearbyTargets,
                    effects = effects,
                )
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
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
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
                resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
            }

            "deathblow" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult =
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                if (damageResult.hit) {
                    restoreResource(world, user, ResourceType.ENERGY, effect, effects)
                }
            }

            "holy_strike" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
            }

            "judgment_hammer" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult =
                    resolveDamage(world, user, targetEntity, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_HIT,
                    primaryTarget = targetEntity,
                    areaTargets = listOf(targetEntity),
                    hitSucceeded = damageResult.hit,
                    effects = effects,
                )
            }

            "holy_light" -> {
                healTarget(world, user, effect, effects)
                targets += user
            }

            "holy_shield" -> {
                targets += user
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
            }

            "devotion" -> {
                targets += user
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
            }

            "holy_aura" -> {
                targets += user
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
                val origin = requireNotNull(world.get<Position>(user)).toPoint()
                hostileTargetsWithin(world, user, origin, definition.areaRadius).forEach { enemy ->
                    targets += enemy
                    resolveDamage(world, user, enemy, definition.damageType, effect.damageMultiplier, effects, abilityId = definition.id)
                }
            }

            "purify" -> {
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
                healTarget(world, user, effect, effects)
                targets += user
            }

            "divine_intervention" -> {
                healTarget(world, user, effect, effects)
                targets += user
                applyConfiguredEffects(
                    world = world,
                    user = user,
                    definition = definition,
                    effect = effect,
                    trigger = EffectTrigger.ON_CAST,
                    effects = effects,
                )
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

    private fun applyConfiguredEffects(
        world: World,
        user: EntityId,
        definition: TalentDef,
        effect: TalentLevelEffect,
        trigger: EffectTrigger,
        primaryTarget: EntityId? = null,
        areaTargets: List<EntityId> = emptyList(),
        hitSucceeded: Boolean = false,
        effects: MutableList<TalentEffectResult>,
    ) {
        effect.associatedEffects
            .asSequence()
            .filter { spec -> spec.trigger == trigger }
            .forEach { spec ->
                configuredTargets(user, primaryTarget, areaTargets, spec.targetScope).forEach { target ->
                    applyConfiguredStatusEffect(
                        world = world,
                        user = user,
                        target = target,
                        definition = definition,
                        spec = spec,
                        hitSucceeded = hitSucceeded,
                        effects = effects,
                    )
                }
            }

        effect.cleanseEffect
            ?.takeIf { spec -> spec.trigger == trigger }
            ?.let { spec ->
                configuredTargets(user, primaryTarget, areaTargets, spec.targetScope).forEach { target ->
                    applyConfiguredCleanse(
                        world = world,
                        user = user,
                        target = target,
                        spec = spec,
                    )
                }
            }
    }

    private fun configuredTargets(
        user: EntityId,
        primaryTarget: EntityId?,
        areaTargets: List<EntityId>,
        targetScope: EffectTargetScope,
    ): List<EntityId> {
        val resolved = linkedSetOf<EntityId>()
        when (targetScope) {
            EffectTargetScope.SELF -> resolved += user
            EffectTargetScope.PRIMARY_TARGET -> primaryTarget?.let(resolved::add)
            EffectTargetScope.HOSTILES_IN_RADIUS -> resolved += areaTargets
        }
        return resolved.toList()
    }

    private fun applyConfiguredStatusEffect(
        world: World,
        user: EntityId,
        target: EntityId,
        definition: TalentDef,
        spec: AssociatedStatusEffect,
        hitSucceeded: Boolean,
        effects: MutableList<TalentEffectResult>,
    ) {
        if (spec.duration <= 0) {
            return
        }
        check(spec.applicationPolicy != ApplicationPolicy.TAG_AUTO) {
            "TAG_AUTO associated effects are not supported by TalentResolver yet."
        }
        val request =
            StatusApplicationRequest(
                statusId = spec.effectId,
                duration = spec.duration,
                applicationPolicy = spec.applicationPolicy,
                saveDimension = resolveSaveDimension(spec, definition),
            )
        val resolution =
            combatResolver.resolveStatusApplication(
                world = world,
                attacker = user,
                target = target,
                request = request,
                hitSucceeded = hitSucceeded,
            )
        if (!resolution.applied) {
            return
        }
        applyEffect(world, target, buildActiveEffect(spec))
        effects += buildTalentEffectResult(spec, target)
    }

    private fun applyConfiguredCleanse(
        world: World,
        user: EntityId,
        target: EntityId,
        spec: CleanseEffect,
    ) {
        check(spec.applicationPolicy != ApplicationPolicy.TAG_AUTO) {
            "TAG_AUTO cleanse effects are not supported by TalentResolver yet."
        }
        val request =
            StatusApplicationRequest(
                statusId = spec.effectId,
                duration = 0,
                applicationPolicy = spec.applicationPolicy,
            )
        val resolution =
            combatResolver.resolveStatusApplication(
                world = world,
                attacker = user,
                target = target,
                request = request,
            )
        if (!resolution.applied) {
            return
        }
        clearNegativeEffects(
            world = world,
            target = target,
            maxEffectsRemoved = spec.maxEffectsRemoved,
        )
    }

    private fun resolveSaveDimension(
        spec: AssociatedStatusEffect,
        definition: TalentDef,
    ): SaveDimension? =
        spec.saveDimension
            ?: definition.powerDimension
            ?: if (spec.applicationPolicy.requiresSave()) {
                error("Talent ${definition.id} effect ${spec.effectId} requires saveDimension for ${spec.applicationPolicy}.")
            } else {
                null
            }

    private fun buildActiveEffect(spec: AssociatedStatusEffect): ActiveEffect =
        ActiveEffect(
            id = spec.effectId,
            name = effectDisplayName(spec.effectType),
            type = spec.effectType,
            remainingTurns = spec.duration,
            statModifiers = effectStatModifiers(spec),
            skipNextDecay = spec.targetScope == EffectTargetScope.SELF,
        )

    private fun effectDisplayName(type: StatusEffectType): String =
        when (type) {
            StatusEffectType.STUNNED -> "Stunned"
            StatusEffectType.ARMOR_BREAK -> "Armor Break"
            StatusEffectType.WAR_CRY_BUFF -> "War Cry"
            StatusEffectType.WAR_CRY_DEBUFF -> "Shaken"
            StatusEffectType.GUARD_STANCE_BUFF -> "Guard Stance"
            StatusEffectType.ARCANE_SHIELD_BUFF -> "Arcane Shield"
            StatusEffectType.UNYIELDING_BUFF -> "Unyielding"
            StatusEffectType.MANA_SURGE_BUFF -> "Mana Surge"
            StatusEffectType.STEALTH_BUFF -> "Stealth"
            StatusEffectType.CURSED -> "Cursed"
            StatusEffectType.HOLY_SHIELD_BUFF -> "Holy Shield"
            StatusEffectType.DEVOTION_BUFF -> "Devotion"
            StatusEffectType.HOLY_AURA_BUFF -> "Holy Aura"
        }

    private fun effectStatModifiers(spec: AssociatedStatusEffect): StatModifier =
        when (spec.effectType) {
            StatusEffectType.STUNNED -> StatModifier()
            StatusEffectType.ARMOR_BREAK -> StatModifier(defense = -3)
            StatusEffectType.WAR_CRY_BUFF -> StatModifier(attackMultiplierBonus = spec.magnitude)
            StatusEffectType.WAR_CRY_DEBUFF -> StatModifier(defenseMultiplierBonus = -spec.magnitude)
            StatusEffectType.GUARD_STANCE_BUFF -> StatModifier(defenseMultiplierBonus = spec.magnitude)
            StatusEffectType.ARCANE_SHIELD_BUFF -> StatModifier(defenseMultiplierBonus = spec.magnitude)
            StatusEffectType.UNYIELDING_BUFF -> StatModifier(defenseMultiplierBonus = spec.magnitude)
            StatusEffectType.MANA_SURGE_BUFF -> StatModifier(talentPower = spec.magnitude)
            StatusEffectType.STEALTH_BUFF ->
                StatModifier(
                    evasion = maxOf(2, (spec.magnitude * 20).toInt()),
                    speed = maxOf(2, (spec.magnitude * 10).toInt()),
                )
            StatusEffectType.CURSED ->
                StatModifier(
                    attackMultiplierBonus = -spec.magnitude,
                    defenseMultiplierBonus = -spec.magnitude,
                )
            StatusEffectType.HOLY_SHIELD_BUFF -> StatModifier(defenseMultiplierBonus = spec.magnitude)
            StatusEffectType.DEVOTION_BUFF ->
                StatModifier(
                    attackMultiplierBonus = spec.magnitude,
                    accuracy = maxOf(1, (spec.magnitude * 10).toInt()),
                )
            StatusEffectType.HOLY_AURA_BUFF -> StatModifier(defenseMultiplierBonus = spec.magnitude)
        }

    private fun buildTalentEffectResult(
        spec: AssociatedStatusEffect,
        target: EntityId,
    ): TalentEffectResult =
        if (spec.effectType in BUFF_LIKE_EFFECT_TYPES) {
            TalentEffectResult.Buff(target, spec.effectType, spec.duration, spec.magnitude)
        } else {
            TalentEffectResult.StatusApplied(target, spec.effectType, spec.duration)
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
        val amount = maxOf(8, (pool.max * effect.resourceRestoreFraction).toInt().coerceAtLeast(0))
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
        val amount = maxOf(10, (health.max * effect.healFraction).toInt().coerceAtLeast(0))
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
        abilityId: String,
    ): DamageResolution {
        val effectiveMultiplier =
            damageMultiplierResolver.resolve(
                world = world,
                attacker = attacker,
                target = target,
                damageType = damageType,
                baseMultiplier = damageMultiplier,
            )
        val result =
            combatResolver.resolveMelee(
                world = world,
                attacker = attacker,
                target = target,
                damageType = damageType,
                damageMultiplier = effectiveMultiplier,
                abilityId = abilityId,
            )
        if (!result.hit) {
            effects += TalentEffectResult.Miss(target)
            return DamageResolution(hit = false)
        }

        val damage = requireNotNull(result.damage) { "Missing DamageResult for successful hit." }
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
        maxEffectsRemoved: Int,
    ): Int {
        if (maxEffectsRemoved <= 0) {
            return 0
        }
        val tracker = world.get<EffectTracker>(target) ?: return 0
        val removable =
            tracker.effects
                .filter { effect -> effect.type in NEGATIVE_EFFECT_TYPES }
                .sortedWith(
                    compareBy<ActiveEffect>(
                        { negativeEffectPriority(it.type) },
                        { -it.remainingTurns },
                        { it.id },
                    ),
                ).take(maxEffectsRemoved)
        if (removable.isEmpty()) {
            return 0
        }
        tracker.effects.removeAll(removable.toSet())
        StatsCalculator.recalculateAndStore(world, target)
        return removable.size
    }

    private fun negativeEffectPriority(type: StatusEffectType): Int =
        when (type) {
            StatusEffectType.STUNNED -> 0
            StatusEffectType.CURSED -> 1
            StatusEffectType.ARMOR_BREAK -> 2
            StatusEffectType.WAR_CRY_DEBUFF -> 3
            else -> Int.MAX_VALUE
        }
}
