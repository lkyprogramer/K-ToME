package com.ktome.core.talent

import com.ktome.core.combat.CombatResolver
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar
import com.ktome.core.stats.StatsCalculator

sealed interface TalentUseResult {
    data class Success(
        val result: TalentResult,
    ) : TalentUseResult

    data class Failure(
        val reason: String,
    ) : TalentUseResult
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
}

class TalentResolver(
    private val registry: TalentRegistry,
    private val combatResolver: CombatResolver,
) {
    private val supportedTalentIds = setOf("power_strike", "charge", "shield_bash", "war_cry")

    private data class DamageResolution(
        val hit: Boolean,
        val finalDamage: Int = 0,
        val critical: Boolean = false,
    )

    fun canUse(
        world: World,
        map: GameMap,
        user: EntityId,
        talentId: String,
        target: Point?,
    ): String? {
        val definition = registry.get(talentId) ?: return "Unknown talent."
        if (talentId !in supportedTalentIds) {
            return "Talent is not supported yet."
        }
        val stamina = requireNotNull(world.get<Stamina>(user)) { "Missing Stamina for $user" }
        val cooldowns = requireNotNull(world.get<CooldownState>(user)) { "Missing CooldownState for $user" }

        if (cooldowns.remainingByTalentId[talentId]?.let { it > 0 } == true) {
            return "${definition.name} is still cooling down."
        }
        if (stamina.current < definition.staminaCost) {
            return "Not enough stamina."
        }
        if (definition.range == 0) {
            return null
        }

        val userPosition = requireNotNull(world.get<Position>(user)).toPoint()
        val targetPoint = target ?: return "A target is required."
        val distance = userPosition.chebyshevDistanceTo(targetPoint)
        if (distance > definition.range || distance < definition.minRange) {
            return "Target is out of range."
        }

        val targetEntity = hostileTargetAt(world, user, targetPoint) ?: return "No valid target."
        if (definition.id == "charge" && chargeDestination(world, map, userPosition, targetPoint, targetEntity) == null) {
            return "No path to charge target."
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
        val failureReason = canUse(world, map, user, talentId, target)
        if (failureReason != null) {
            return TalentUseResult.Failure(failureReason)
        }

        val definition = requireNotNull(registry.get(talentId))
        val loadout = requireNotNull(world.get<TalentLoadout>(user)) { "Missing TalentLoadout for $user" }
        val stamina = requireNotNull(world.get<Stamina>(user)) { "Missing Stamina for $user" }
        val cooldowns = requireNotNull(world.get<CooldownState>(user)) { "Missing CooldownState for $user" }
        val level = loadout.levelOf(talentId).coerceIn(1, definition.maxLevel)
        val effect = requireNotNull(definition.levelEffects[level]) { "Missing level effect for $talentId level $level" }
        val effects = mutableListOf<TalentEffectResult>()
        val targets = linkedSetOf<EntityId>()

        if (talentId !in supportedTalentIds) {
            return TalentUseResult.Failure("Unsupported talent: $talentId")
        }

        stamina.current -= definition.staminaCost
        cooldowns.remainingByTalentId[talentId] = definition.cooldown

        when (talentId) {
            "power_strike" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, effect.damageMultiplier, effects)
                if (damageResult.hit) {
                    if (effect.knockback > 0) {
                        knockback(world, map, user, targetEntity, effect.knockback)?.let(effects::add)
                    }
                    if (effect.armorBreakDuration > 0) {
                        applyEffect(
                            world = world,
                            target = targetEntity,
                            effect =
                                ActiveEffect(
                                    id = "power_strike_armor_break",
                                    name = "Armor Break",
                                    type = StatusEffectType.ARMOR_BREAK,
                                    remainingTurns = effect.armorBreakDuration,
                                    statModifiers = StatModifier(defense = -3),
                                ),
                        )
                        effects += TalentEffectResult.StatusApplied(targetEntity, StatusEffectType.ARMOR_BREAK, effect.armorBreakDuration)
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
                val damageResult = resolveDamage(world, user, targetEntity, effect.damageMultiplier, effects)
                if (damageResult.hit && effect.stunDuration > 0) {
                    applyEffect(
                        world = world,
                        target = targetEntity,
                        effect =
                            ActiveEffect(
                                id = "charge_stun",
                                name = "Stunned",
                                type = StatusEffectType.STUNNED,
                                remainingTurns = effect.stunDuration,
                            ),
                    )
                    effects += TalentEffectResult.StatusApplied(targetEntity, StatusEffectType.STUNNED, effect.stunDuration)
                }
            }

            "shield_bash" -> {
                val targetEntity = requireNotNull(hostileTargetAt(world, user, requireNotNull(target)))
                targets += targetEntity
                val damageResult = resolveDamage(world, user, targetEntity, effect.damageMultiplier, effects)
                if (damageResult.hit) {
                    if (effect.stunDuration > 0) {
                        applyEffect(
                            world = world,
                            target = targetEntity,
                            effect =
                                ActiveEffect(
                                    id = "shield_bash_stun",
                                    name = "Stunned",
                                    type = StatusEffectType.STUNNED,
                                    remainingTurns = effect.stunDuration,
                                ),
                        )
                        effects += TalentEffectResult.StatusApplied(targetEntity, StatusEffectType.STUNNED, effect.stunDuration)
                    }
                    if (effect.knockback > 0) {
                        knockback(world, map, user, targetEntity, effect.knockback)?.let(effects::add)
                    }
                }
            }

            "war_cry" -> {
                applyEffect(
                    world = world,
                    target = user,
                    effect =
                        ActiveEffect(
                            id = "war_cry_buff",
                            name = "War Cry",
                            type = StatusEffectType.WAR_CRY_BUFF,
                            remainingTurns = effect.buffDuration,
                            statModifiers = StatModifier(attackMultiplierBonus = effect.buffMagnitude),
                            skipNextDecay = true,
                        ),
                )
                effects += TalentEffectResult.Buff(user, StatusEffectType.WAR_CRY_BUFF, effect.buffDuration, effect.buffMagnitude)
                targets += user

                if (effect.debuffDuration > 0 && effect.debuffMagnitude > 0.0) {
                    val origin = requireNotNull(world.get<Position>(user)).toPoint()
                    hostileTargetsWithin(world, user, origin, definition.areaRadius).forEach { enemy ->
                        applyEffect(
                            world = world,
                            target = enemy,
                            effect =
                                ActiveEffect(
                                    id = "war_cry_debuff",
                                    name = "Shaken",
                                    type = StatusEffectType.WAR_CRY_DEBUFF,
                                    remainingTurns = effect.debuffDuration,
                                    statModifiers = StatModifier(defenseMultiplierBonus = -effect.debuffMagnitude),
                                ),
                        )
                        effects += TalentEffectResult.Buff(enemy, StatusEffectType.WAR_CRY_DEBUFF, effect.debuffDuration, effect.debuffMagnitude)
                        targets += enemy
                    }
                }
            }

            else -> return TalentUseResult.Failure("Unsupported talent: $talentId")
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

    private fun resolveDamage(
        world: World,
        attacker: EntityId,
        target: EntityId,
        damageMultiplier: Double,
        effects: MutableList<TalentEffectResult>,
    ): DamageResolution {
        val result = combatResolver.resolveMelee(world, attacker, target, damageMultiplier)
        if (!result.hit) {
            effects += TalentEffectResult.Miss(target)
            return DamageResolution(hit = false)
        }

        val health = requireNotNull(world.get<Health>(target)) { "Missing Health for $target" }
        health.current = (health.current - result.finalDamage).coerceAtLeast(0)
        effects += TalentEffectResult.Damage(target, result.finalDamage, result.critical)
        return DamageResolution(
            hit = true,
            finalDamage = result.finalDamage,
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
}
