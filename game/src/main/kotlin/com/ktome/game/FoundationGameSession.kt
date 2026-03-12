package com.ktome.game

import com.ktome.core.ai.AIAction
import com.ktome.core.ai.AIActorSnapshot
import com.ktome.core.ai.AIDecision
import com.ktome.core.ai.AIDecisionContext
import com.ktome.core.ai.AITargetSnapshot
import com.ktome.core.combat.CombatResolver
import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Energy
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.ExperienceReward
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.event.DamageDealtEvent
import com.ktome.core.event.EntityDeathEvent
import com.ktome.core.event.ExperienceGainedEvent
import com.ktome.core.event.LevelUpEvent
import com.ktome.core.event.MissEvent
import com.ktome.core.fov.Shadowcasting
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.movement.MovementRules
import com.ktome.core.progression.ExperienceSystem
import com.ktome.core.turn.TurnActorState
import com.ktome.core.turn.TurnScheduler

class FoundationGameSession internal constructor(
    val config: FoundationGameConfig,
    val map: GameMap,
    private val world: World,
    val playerId: EntityId,
    private val combatResolver: CombatResolver,
) {
    private var visibleTiles: Set<Point> = emptySet()
    private val exploredTiles = linkedSetOf<Point>()
    private val messageLog = ArrayDeque<String>()
    private val pendingActions = ArrayDeque<EntityId>()
    private var gameOver = false

    init {
        addMessage("You enter the dungeon.")
        refreshFov()
    }

    fun playerPosition(): Point = requireNotNull(world.get<Position>(playerId)).toPoint()

    fun playerGlyph(): Char = world.get<Glyph>(playerId)?.value ?: '@'

    fun visibleTiles(): Set<Point> = visibleTiles.toSet()

    fun exploredTiles(): Set<Point> = exploredTiles.toSet()

    fun actorViews(): List<ActorView> =
        world.entitiesWith(Position::class, Glyph::class, DisplayColor::class, Name::class)
            .mapNotNull { entityId ->
                val position = requireNotNull(world.get<Position>(entityId)).toPoint()
                if (entityId != playerId && position !in visibleTiles) {
                    return@mapNotNull null
                }

                ActorView(
                    entityId = entityId,
                    position = position,
                    glyph = requireNotNull(world.get<Glyph>(entityId)).value,
                    colorHex = requireNotNull(world.get<DisplayColor>(entityId)).hex,
                    name = requireNotNull(world.get<Name>(entityId)).value,
                    isPlayer = entityId == playerId,
                )
            }

    fun messageLog(): List<String> = messageLog.toList()

    fun isGameOver(): Boolean = gameOver

    fun playerStatus(): PlayerStatus {
        val health = requireNotNull(world.get<Health>(playerId))
        val experience = requireNotNull(world.get<Experience>(playerId))
        return PlayerStatus(
            currentHp = health.current,
            maxHp = health.max,
            level = experience.level,
            currentExperience = experience.current,
            nextLevelRequirement = ExperienceSystem.nextLevelExp(experience.level),
            statPoints = experience.unspentStatPoints,
            talentPoints = experience.unspentTalentPoints,
        )
    }

    fun movePlayer(delta: Point): Boolean = perform(PlayerCommand.Move(delta))

    fun perform(command: PlayerCommand): Boolean {
        if (gameOver || !world.isAlive(playerId)) {
            return false
        }

        advanceUntilPlayerTurn()
        if (gameOver || !world.isAlive(playerId) || pendingActions.firstOrNull() != playerId) {
            refreshFov()
            return false
        }

        if (!isExecutable(command)) {
            return false
        }

        pendingActions.removeFirst()
        executePlayerCommand(command)
        advanceUntilPlayerTurn()
        refreshFov()
        return true
    }

    private fun actorStates(): List<TurnActorState> =
        world.entitiesWith(Position::class, Energy::class, DerivedStats::class, Health::class)
            .filter { entityId -> requireNotNull(world.get<Health>(entityId)).current > 0 }
            .map { entityId ->
                TurnActorState(
                    entityId = entityId,
                    speed = requireNotNull(world.get<DerivedStats>(entityId)).speed,
                    energy = requireNotNull(world.get<Energy>(entityId)).current,
                )
            }

    private fun applyRemainingEnergy(remainingEnergy: Map<EntityId, Int>) {
        remainingEnergy.forEach { (entityId, energy) ->
            world.get<Energy>(entityId)?.current = energy
        }
    }

    private fun scheduleNextActions() {
        if (gameOver || !world.isAlive(playerId)) {
            return
        }

        val tickResult = TurnScheduler.tick(actorStates())
        applyRemainingEnergy(tickResult.remainingEnergy)
        pendingActions += tickResult.actionQueue
    }

    private fun advanceUntilPlayerTurn() {
        while (!gameOver && world.isAlive(playerId)) {
            if (pendingActions.isEmpty()) {
                scheduleNextActions()
            }

            val nextActor = pendingActions.firstOrNull() ?: break
            if (nextActor == playerId) {
                break
            }

            pendingActions.removeFirst()
            if (!world.isAlive(nextActor)) {
                continue
            }
            executeMonsterTurn(nextActor)
        }
    }

    private fun isExecutable(command: PlayerCommand): Boolean =
        when (command) {
            PlayerCommand.Wait -> true
            is PlayerCommand.Move -> {
                val from = playerPosition()
                val moveResult = MovementRules.attemptMove(map, from, command.delta)
                if (moveResult.moved) {
                    true
                } else {
                    canBumpAttack(from, command.delta)
                }
            }
        }

    private fun executePlayerCommand(command: PlayerCommand) {
        when (command) {
            PlayerCommand.Wait -> addMessage("You wait.")
            is PlayerCommand.Move -> {
                val from = playerPosition()
                val destination = from + command.delta
                val blocker = blockerAt(destination).takeIf { command.delta.isAdjacentTo(Point.ZERO) }
                if (blocker != null) {
                    resolveAttack(playerId, blocker)
                } else {
                    val result = MovementRules.attemptMove(map, from, command.delta)
                    if (result.moved) {
                        requireNotNull(world.get<Position>(playerId)).moveTo(result.destination)
                    }
                }
            }
        }
    }

    private fun executeMonsterTurn(monsterId: EntityId) {
        if (!world.isAlive(playerId)) {
            return
        }

        val behavior = world.get<AIBehavior>(monsterId) ?: return
        val position = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val patrolRoute = world.get<PatrolRoute>(monsterId)
        val decision = AIDecision.decide(
            AIDecisionContext(
                map = map,
                actor = AIActorSnapshot(
                    entityId = monsterId,
                    position = position,
                    behavior = behavior,
                    patrolRoute = patrolRoute,
                ),
                target = AITargetSnapshot(playerId, playerPosition()),
                occupiedTiles = occupiedBlockingTiles(excluding = monsterId),
                targetVisible = playerPosition() in Shadowcasting.computeVisible(
                    map = map,
                    origin = position,
                    radius = behavior.sightRadius,
                ),
            ),
        )

        decision.nextPatrolIndex?.let { nextIndex ->
            patrolRoute?.nextWaypointIndex = nextIndex
        }

        when (val action = decision.action) {
            is AIAction.Attack -> resolveAttack(monsterId, action.target)
            is AIAction.Move -> {
                if (blockerAt(action.destination) == null) {
                    requireNotNull(world.get<Position>(monsterId)).moveTo(action.destination)
                }
            }
            AIAction.Wait -> Unit
        }
    }

    private fun resolveAttack(
        attacker: EntityId,
        target: EntityId,
    ) {
        val attackerName = requireNotNull(world.get<Name>(attacker)).value
        val targetName = requireNotNull(world.get<Name>(target)).value
        val targetHealth = requireNotNull(world.get<Health>(target))
        val result = combatResolver.resolveMelee(world, attacker, target)

        if (!result.hit) {
            logEvent(MissEvent(attacker, target))
            addMessage("$attackerName misses $targetName.")
            return
        }

        targetHealth.current = (targetHealth.current - result.finalDamage).coerceAtLeast(0)
        logEvent(DamageDealtEvent(attacker, target, result.finalDamage, result.critical))
        addMessage(
            if (result.critical) {
                "$attackerName critically hits $targetName for ${result.finalDamage} damage."
            } else {
                "$attackerName hits $targetName for ${result.finalDamage} damage."
            },
        )

        if (result.targetKilled) {
            handleDeath(target, attacker)
        }
    }

    private fun handleDeath(
        target: EntityId,
        killer: EntityId?,
    ) {
        logEvent(EntityDeathEvent(target, killer))

        if (target == playerId) {
            gameOver = true
            addMessage("You die. Game over.")
            return
        }

        val targetName = requireNotNull(world.get<Name>(target)).value
        addMessage("$targetName dies.")

        val reward = world.get<ExperienceReward>(target)?.value ?: 0
        world.destroyEntity(target)
        if (killer == playerId && reward > 0) {
            gainExperience(reward)
        }
    }

    private fun gainExperience(amount: Int) {
        val experience = requireNotNull(world.get<Experience>(playerId))
        val health = requireNotNull(world.get<Health>(playerId))
        val result = ExperienceSystem.applyReward(
            experience = experience,
            health = health,
            reward = amount,
        )

        logEvent(ExperienceGainedEvent(playerId, amount))
        addMessage("You gain $amount experience.")

        if (result.levelsGained > 0) {
            logEvent(
                LevelUpEvent(
                    entity = playerId,
                    newLevel = experience.level,
                    unspentStatPoints = experience.unspentStatPoints,
                    unspentTalentPoints = experience.unspentTalentPoints,
                ),
            )
            addMessage("You advance to level ${experience.level}.")
        }
    }

    private fun occupiedBlockingTiles(excluding: EntityId? = null): Set<Point> =
        world.entitiesWith(Position::class, BlocksMovement::class)
            .filter { entityId -> entityId != excluding && world.get<BlocksMovement>(entityId)?.value == true }
            .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
            .toSet()

    private fun blockerAt(point: Point): EntityId? =
        world.entitiesWith(Position::class, BlocksMovement::class)
            .firstOrNull { entityId ->
                world.get<BlocksMovement>(entityId)?.value == true &&
                    requireNotNull(world.get<Position>(entityId)).toPoint() == point
            }

    private fun canBumpAttack(
        from: Point,
        delta: Point,
    ): Boolean = delta.isAdjacentTo(Point.ZERO) && blockerAt(from + delta) != null

    private fun refreshFov() {
        visibleTiles = Shadowcasting.computeVisible(
            map = map,
            origin = playerPosition(),
            radius = config.fovRadius,
        )
        exploredTiles += visibleTiles
    }

    private fun addMessage(message: String) {
        if (messageLog.size == config.messageLogSize) {
            messageLog.removeFirst()
        }
        messageLog += message
    }

    private fun logEvent(event: Any) {
        @Suppress("UNUSED_VARIABLE")
        val ignored = event
    }
}
