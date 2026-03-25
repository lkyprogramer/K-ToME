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
import com.ktome.core.status.CleansePolicy
import com.ktome.core.status.EffectCategory
import com.ktome.core.status.StatusCatalog
import com.ktome.core.status.StatusDefinitions
import com.ktome.core.status.StatusEffectDef
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusLifecycle

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

private fun StatModifier.scaledOrFixed(magnitude: Double): StatModifier =
    if (magnitude == 0.0) {
        this
    } else {
        StatModifier(
            str = (str * magnitude).toInt(),
            dex = (dex * magnitude).toInt(),
            con = (con * magnitude).toInt(),
            wil = (wil * magnitude).toInt(),
            attack = (attack * magnitude).toInt(),
            defense = (defense * magnitude).toInt(),
            accuracy = (accuracy * magnitude).toInt(),
            evasion = (evasion * magnitude).toInt(),
            speed = (speed * magnitude).toInt(),
            maxHp = (maxHp * magnitude).toInt(),
            maxStamina = (maxStamina * magnitude).toInt(),
            hpRegen = hpRegen * magnitude,
            staminaRegen = staminaRegen * magnitude,
            critChance = critChance * magnitude,
            talentPower = talentPower * magnitude,
            attackMultiplierBonus = attackMultiplierBonus * magnitude,
            defenseMultiplierBonus = defenseMultiplierBonus * magnitude,
        )
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
        val stealthBroken: Boolean = false,
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
        val statusId: String = type.schemaId,
        val duration: Int,
        val interactionId: String? = null,
        val previousSource: EntityId? = null,
    ) : TalentEffectResult

    data class Buff(
        val target: EntityId,
        val type: StatusEffectType,
        val statusId: String = type.schemaId,
        val duration: Int,
        val magnitude: Double,
        val interactionId: String? = null,
        val previousSource: EntityId? = null,
    ) : TalentEffectResult

    data class StatusCleanse(
        val target: EntityId,
        val removed: List<StatusEffectType>,
        val removedStatusIds: List<String> = removed.map(StatusEffectType::schemaId),
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
    private val statusCatalog: StatusCatalog = StatusCatalog.EMPTY,
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
                talentName = definition.nameKey,
                talentId = talentId,
            )
        }
        val cooldowns = requireNotNull(world.get<CooldownState>(user)) { "Missing CooldownState for $user" }

        if (cooldowns.remainingByTalentId[talentId]?.let { it > 0 } == true) {
            return TalentUseResult.Failure(
                code = TalentFailureCode.COOLDOWN,
                reason = "${definition.id} is still cooling down.",
                talentName = definition.nameKey,
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
                talentName = definition.nameKey,
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
                    talentName = definition.nameKey,
                    talentId = talentId,
                )
        val distance = userPosition.chebyshevDistanceTo(targetPoint)
        if (distance > range || distance < definition.minRange) {
            return TalentUseResult.Failure(
                code = TalentFailureCode.OUT_OF_RANGE,
                reason = "Target is out of range.",
                talentName = definition.nameKey,
                talentId = talentId,
            )
        }

        if (definition.id == "blink" || definition.id == "roll") {
            if (blinkDestination(world, map, userPosition, targetPoint, user) == null) {
                return TalentUseResult.Failure(
                    code = TalentFailureCode.NO_TARGET,
                    reason = "No valid movement destination.",
                    talentName = definition.nameKey,
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
                    talentName = definition.nameKey,
                    talentId = talentId,
                )
        if (definition.id == "charge" && chargeDestination(world, map, userPosition, targetPoint, targetEntity) == null) {
            return TalentUseResult.Failure(
                code = TalentFailureCode.NO_CHARGE_PATH,
                reason = "No path to charge target.",
                talentName = definition.nameKey,
                talentId = talentId,
            )
        }
        if (definition.id == "shadowstep" && shadowstepDestination(world, map, userPosition, targetPoint, targetEntity) == null) {
            return TalentUseResult.Failure(
                    code = TalentFailureCode.NO_TARGET,
                    reason = "No valid shadowstep landing point.",
                    talentName = definition.nameKey,
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
                talentName = definition.nameKey,
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
                restoreConfiguredResource(world, user, definition, effect, effects)
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
                    restoreConfiguredResource(world, user, definition, effect, effects)
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
                    talentName = definition.nameKey,
                    talentId = talentId,
                )
        }

        StatsCalculator.recalculateAndStore(world, user)
        return TalentUseResult.Success(
            TalentResult(
                talentId = definition.id,
                talentName = definition.nameKey,
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
                        effects = effects,
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
                statusId = spec.statusId,
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
        val changeResult = applyEffect(world, target, buildActiveEffect(spec, user))
        effects += buildTalentEffectResult(spec, target, changeResult)
    }

    private fun applyConfiguredCleanse(
        world: World,
        user: EntityId,
        target: EntityId,
        spec: CleanseEffect,
        effects: MutableList<TalentEffectResult>,
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
        val removed = clearNegativeEffects(
            world = world,
            target = target,
            maxEffectsRemoved = spec.maxEffectsRemoved,
        )
        if (removed.isNotEmpty()) {
            effects +=
                TalentEffectResult.StatusCleanse(
                    target = target,
                    removed = removed.map { effect -> effect.type },
                    removedStatusIds = removed.map { effect -> effect.schemaId },
                )
        }
    }

    private fun resolveSaveDimension(
        spec: AssociatedStatusEffect,
        definition: TalentDef,
    ): SaveDimension? =
        spec.saveDimension
            ?: definition.powerDimension
            ?: if (spec.applicationPolicy.requiresSave()) {
                error("Talent ${definition.id} status ${spec.statusId} requires saveDimension for ${spec.applicationPolicy}.")
            } else {
                null
            }

    private fun buildActiveEffect(
        spec: AssociatedStatusEffect,
        sourceEntityId: EntityId,
    ): ActiveEffect {
        val statusDefinition = statusCatalog.definitionFor(spec.statusId)
        return StatusLifecycle.createInstance(
            definition = statusDefinition,
            effectId = spec.effectId,
            duration = spec.duration,
            magnitude = spec.magnitude,
            sourceEntityId = sourceEntityId,
            skipNextDecay = spec.targetScope == EffectTargetScope.SELF,
            applicationPolicy = spec.applicationPolicy,
            statModifierOverride = effectStatModifiers(spec, statusDefinition),
        )
    }

    private fun effectStatModifiers(
        spec: AssociatedStatusEffect,
        statusDefinition: StatusEffectDef,
    ): StatModifier =
        when (statusDefinition.type) {
            StatusEffectType.CUSTOM -> statusDefinition.statModifier.scaledOrFixed(spec.magnitude)
            StatusEffectType.GUARD,
            StatusEffectType.SHIELD,
            StatusEffectType.REGEN,
            StatusEffectType.HASTE,
            StatusEffectType.SLOW,
            StatusEffectType.BANE,
            StatusEffectType.WEAKEN,
            StatusEffectType.OVERCHARGE,
            StatusEffectType.INVULNERABLE,
            StatusEffectType.STUN,
            -> StatModifier()
            StatusEffectType.ARMOR_BREAK -> StatModifier(defense = -3)
            StatusEffectType.GUARD_STANCE_BUFF -> StatModifier(defenseMultiplierBonus = spec.magnitude)
            StatusEffectType.ARCANE_SHIELD_BUFF -> StatModifier(defenseMultiplierBonus = spec.magnitude)
            StatusEffectType.UNYIELDING_BUFF -> StatModifier(defenseMultiplierBonus = spec.magnitude)
            StatusEffectType.MANA_SURGE_BUFF -> StatModifier(talentPower = spec.magnitude)
            StatusEffectType.STEALTH ->
                StatModifier(
                    evasion = maxOf(2, (spec.magnitude * 20).toInt()),
                    speed = maxOf(2, (spec.magnitude * 10).toInt()),
                )
            StatusEffectType.CURSE ->
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
            StatusEffectType.MARKED,
            StatusEffectType.ROOT,
            StatusEffectType.SILENCE,
            StatusEffectType.BLEED,
            StatusEffectType.BURN,
            StatusEffectType.FREEZE,
            StatusEffectType.POISON,
            StatusEffectType.TAUNT,
            -> StatModifier.ZERO
        }

    private fun buildTalentEffectResult(
        spec: AssociatedStatusEffect,
        target: EntityId,
        changeResult: com.ktome.core.status.StatusChangeResult,
    ): TalentEffectResult {
        val statusDefinition = statusCatalog.definitionFor(spec.statusId)
        return if (statusDefinition.category == EffectCategory.BUFF) {
            TalentEffectResult.Buff(
                target = target,
                type = statusDefinition.type,
                statusId = statusDefinition.id,
                duration = spec.duration,
                magnitude = spec.magnitude,
                interactionId = changeResult.interactionId,
                previousSource = previousOverrideSource(changeResult),
            )
        } else {
            TalentEffectResult.StatusApplied(
                target = target,
                type = statusDefinition.type,
                statusId = statusDefinition.id,
                duration = spec.duration,
                interactionId = changeResult.interactionId,
                previousSource = previousOverrideSource(changeResult),
            )
        }
    }

    private fun restoreConfiguredResource(
        world: World,
        user: EntityId,
        definition: TalentDef,
        effect: TalentLevelEffect,
        effects: MutableList<TalentEffectResult>,
    ) {
        val resourceType =
            effect.effectOps
                .filterIsInstance<EffectOp.ResourceRestore>()
                .firstOrNull()
                ?.type
                ?: definition.resourceCosts.firstOrNull()?.type
                ?: return
        restoreResource(world, user, resourceType, effect, effects)
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
                stealthBroken = StatusEffectType.STEALTH in result.removedStatusTypes,
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
    ): com.ktome.core.status.StatusChangeResult {
        val tracker = world.get<EffectTracker>(target) ?: EffectTracker(ownerId = target).also { world.add(target, it) }
        val result = StatusLifecycle.applyEffect(tracker, effect)
        StatsCalculator.recalculateAndStore(world, target)
        return result
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
        var bestDestination: Point? = null
        var bestPathLength: Int? = null

        Point.ALL_DIRECTIONS
            .map { direction -> targetPoint + direction }
            .filter { destination ->
                destination != from &&
                    map.isInBounds(destination.x, destination.y) &&
                    !map[destination].blocksMovement &&
                    destination !in blockedTiles
            }.forEach { destination ->
                val path = AStar.findPath(map = map, start = from, goal = destination, blocked = blockedTiles)
                if (path.isEmpty()) {
                    return@forEach
                }
                val pathLength = path.size
                val shouldReplace =
                    bestDestination == null ||
                        requireNotNull(bestPathLength) > pathLength ||
                        (
                            requireNotNull(bestPathLength) == pathLength &&
                                comparePointOrder(destination, requireNotNull(bestDestination)) < 0
                        )
                if (shouldReplace) {
                    bestDestination = destination
                    bestPathLength = pathLength
                }
            }

        return bestDestination
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

        var bestDestination: Point? = null
        candidates.forEach { destination ->
            val shouldReplace =
                bestDestination == null ||
                    compareDistanceThenPointOrder(
                        left = destination,
                        right = requireNotNull(bestDestination),
                        distanceFrom = targetPoint,
                    ) < 0
            if (shouldReplace) {
                bestDestination = destination
            }
        }
        return bestDestination
    }

    private fun shadowstepDestination(
        world: World,
        map: GameMap,
        from: Point,
        targetPoint: Point,
        targetEntity: EntityId,
    ): Point? {
        val blockedTiles = blockedTiles(world, excluding = setOf(targetEntity))
        var bestDestination: Point? = null
        Point.ALL_DIRECTIONS
            .map { direction -> targetPoint + direction }
            .filter { destination ->
                destination != from &&
                    map.isInBounds(destination.x, destination.y) &&
                    !map[destination].blocksMovement &&
                    destination !in blockedTiles
            }.forEach { destination ->
                val shouldReplace =
                    bestDestination == null ||
                        compareDistanceThenPointOrder(
                            left = destination,
                            right = requireNotNull(bestDestination),
                            distanceFrom = from,
                        ) < 0
                if (shouldReplace) {
                    bestDestination = destination
                }
            }
        return bestDestination
    }

    private fun compareDistanceThenPointOrder(
        left: Point,
        right: Point,
        distanceFrom: Point,
    ): Int {
        val distanceCompare = left.chebyshevDistanceTo(distanceFrom).compareTo(right.chebyshevDistanceTo(distanceFrom))
        if (distanceCompare != 0) {
            return distanceCompare
        }
        return comparePointOrder(left, right)
    }

    private fun comparePointOrder(
        left: Point,
        right: Point,
    ): Int {
        val yCompare = left.y.compareTo(right.y)
        if (yCompare != 0) {
            return yCompare
        }
        return left.x.compareTo(right.x)
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
    ): List<ActiveEffect> {
        if (maxEffectsRemoved <= 0) {
            return emptyList()
        }
        val tracker = world.get<EffectTracker>(target) ?: return emptyList()
        val removable =
            StatusLifecycle.cleanse(
                tracker = tracker,
                maxEffectsRemoved = maxEffectsRemoved,
                policy = CleansePolicy.DEFAULT,
            )
        if (removable.isEmpty()) {
            return emptyList()
        }
        StatsCalculator.recalculateAndStore(world, target)
        return removable
    }

    private fun previousOverrideSource(changeResult: com.ktome.core.status.StatusChangeResult): EntityId? =
        if (changeResult.interactionId == "TAUNT_OVERRIDE") {
            changeResult.removed.firstOrNull { effect -> effect.type == StatusEffectType.TAUNT }?.sourceEntityId
        } else {
            null
        }
}
