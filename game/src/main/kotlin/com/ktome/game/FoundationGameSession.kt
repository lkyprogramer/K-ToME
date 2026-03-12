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
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.event.DamageDealtEvent
import com.ktome.core.event.EntityDeathEvent
import com.ktome.core.event.ExperienceGainedEvent
import com.ktome.core.event.LevelUpEvent
import com.ktome.core.event.MissEvent
import com.ktome.core.fov.Shadowcasting
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.Inventory
import com.ktome.core.item.InventoryManager
import com.ktome.core.item.InventoryOperationResult
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemType
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.movement.MovementRules
import com.ktome.core.progression.ExperienceSystem
import com.ktome.core.random.RandomSource
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.StatusEffectType
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.talent.TalentResolver
import com.ktome.core.talent.TalentUseResult
import com.ktome.core.turn.TurnActorState
import com.ktome.core.turn.TurnScheduler

class FoundationGameSession internal constructor(
    val config: FoundationGameConfig,
    val map: GameMap,
    private val world: World,
    val playerId: EntityId,
    private val combatResolver: CombatResolver,
    private val talentRegistry: TalentRegistry,
    private val talentResolver: TalentResolver,
    private val sessionRandom: RandomSource,
    private val inventoryManager: InventoryManager = InventoryManager(),
) {
    private var visibleTiles: Set<Point> = emptySet()
    private val exploredTiles = linkedSetOf<Point>()
    private val messageLog = ArrayDeque<String>()
    private val pendingActions = ArrayDeque<EntityId>()
    private var activeTurnActor: EntityId? = null
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
        val stamina = requireNotNull(world.get<Stamina>(playerId))
        val experience = requireNotNull(world.get<Experience>(playerId))
        val derivedStats = requireNotNull(world.get<DerivedStats>(playerId))
        return PlayerStatus(
            currentHp = health.current,
            maxHp = health.max,
            currentStamina = stamina.current,
            maxStamina = stamina.max,
            level = experience.level,
            currentExperience = experience.current,
            nextLevelRequirement = ExperienceSystem.nextLevelExp(experience.level),
            statPoints = experience.unspentStatPoints,
            talentPoints = experience.unspentTalentPoints,
            attack = derivedStats.attack,
            defense = derivedStats.defense,
            accuracy = derivedStats.accuracy,
            evasion = derivedStats.evasion,
            speed = derivedStats.speed,
        )
    }

    fun inventoryItems(): List<InventoryItemView> {
        val inventory = world.get<Inventory>(playerId) ?: return emptyList()
        return inventory.itemIds.mapIndexedNotNull { index, itemId ->
            val item = world.get<ItemInstance>(itemId) ?: return@mapIndexedNotNull null
            InventoryItemView(
                index = index,
                name = item.name,
                type = item.type,
                equippedSlot = inventoryManager.equippedSlotOf(world, playerId, itemId),
            )
        }
    }

    fun equipmentSlots(): List<EquipmentSlotView> =
        EquipSlot.entries.map { slot ->
            EquipmentSlotView(
                slot = slot,
                itemName =
                    inventoryItems()
                        .firstOrNull { item -> item.equippedSlot == slot }
                        ?.name,
            )
        }

    fun talentSlots(): List<TalentSlotView> {
        val loadout = world.get<com.ktome.core.talent.TalentLoadout>(playerId) ?: return emptyList()
        val cooldowns = world.get<CooldownState>(playerId)?.remainingByTalentId.orEmpty()
        return loadout.slotToTalentId.entries.sortedBy { it.key }.mapNotNull { (slot, talentId) ->
            val definition = talentRegistry.get(talentId) ?: return@mapNotNull null
            TalentSlotView(
                slot = slot,
                talentId = talentId,
                name = definition.name,
                staminaCost = definition.staminaCost,
                currentCooldown = cooldowns[talentId] ?: 0,
                maxCooldown = definition.cooldown,
                requiresTarget = definition.range > 0,
            )
        }
    }

    fun itemNamesAtPlayerPosition(): List<String> =
        itemsOnGroundAt(playerPosition()).mapNotNull { itemId -> world.get<ItemInstance>(itemId)?.name }

    fun targetableHostilePositions(): List<Point> {
        val playerFaction = requireNotNull(world.get<FactionTag>(playerId)).value
        return world.entitiesWith(Position::class, Health::class, FactionTag::class)
            .filter { entityId ->
                entityId != playerId &&
                    requireNotNull(world.get<Health>(entityId)).current > 0 &&
                    requireNotNull(world.get<FactionTag>(entityId)).value != playerFaction
            }
            .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
            .filter { point -> point in visibleTiles }
            .sortedWith(compareBy<Point> { it.chebyshevDistanceTo(playerPosition()) }.thenBy(Point::y).thenBy(Point::x))
    }

    fun perform(command: PlayerCommand): Boolean {
        if (gameOver || !world.isAlive(playerId)) {
            return false
        }

        advanceUntilPlayerTurn()
        if (gameOver || !world.isAlive(playerId) || pendingActions.firstOrNull() != playerId) {
            refreshFov()
            return false
        }

        prepareActorTurn(playerId)
        val consumed = executePlayerCommand(command)
        if (!consumed) {
            refreshFov()
            return false
        }

        finishActorTurn(playerId)
        pendingActions.removeFirstOrNull()
        activeTurnActor = null
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
            if (!world.isAlive(nextActor)) {
                pendingActions.removeFirst()
                activeTurnActor = null
                continue
            }

            prepareActorTurn(nextActor)
            if (nextActor == playerId) {
                break
            }

            pendingActions.removeFirst()
            executeMonsterTurn(nextActor)
            finishActorTurn(nextActor)
            activeTurnActor = null
        }
    }

    private fun prepareActorTurn(actorId: EntityId) {
        if (activeTurnActor == actorId) {
            return
        }

        world.get<CooldownState>(actorId)?.let { cooldowns ->
            cooldowns.remainingByTalentId.keys.toList().forEach { talentId ->
                val remaining = (cooldowns.remainingByTalentId[talentId] ?: 0) - 1
                if (remaining <= 0) {
                    cooldowns.remainingByTalentId.remove(talentId)
                } else {
                    cooldowns.remainingByTalentId[talentId] = remaining
                }
            }
        }

        world.get<Stamina>(actorId)?.let { stamina ->
            val regen = requireNotNull(world.get<DerivedStats>(actorId)).staminaRegen.toInt().coerceAtLeast(0)
            stamina.current = (stamina.current + regen).coerceAtMost(stamina.max)
        }

        activeTurnActor = actorId
    }

    private fun finishActorTurn(actorId: EntityId) {
        val tracker = world.get<EffectTracker>(actorId) ?: return
        var changed = false
        val iterator = tracker.effects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            effect.remainingTurns -= 1
            if (effect.remainingTurns <= 0) {
                iterator.remove()
                changed = true
            }
        }

        if (changed) {
            StatsCalculator.recalculateAndStore(world, actorId)
        }
    }

    private fun executePlayerCommand(command: PlayerCommand): Boolean =
        when (command) {
            PlayerCommand.Wait -> {
                addMessage("You wait.")
                true
            }

            PlayerCommand.PickUp -> {
                val item = itemsOnGroundAt(playerPosition()).firstOrNull()
                if (item == null) {
                    addMessage("There is nothing here to pick up.")
                    false
                } else {
                    val result = inventoryManager.pickUp(world, playerId, item)
                    addInventoryMessage(result)
                    result.success
                }
            }

            is PlayerCommand.ActivateInventoryItem -> {
                val itemView = inventoryItems().getOrNull(command.index)
                if (itemView == null) {
                    addMessage("That inventory slot is empty.")
                    false
                } else {
                    when (itemView.type) {
                        ItemType.CONSUMABLE -> {
                            val itemId = requireNotNull(world.get<Inventory>(playerId)).itemIds[command.index]
                            val item = requireNotNull(world.get<ItemInstance>(itemId))
                            val teleportDestination =
                                if (item.effect == com.ktome.core.item.ConsumableEffect.TELEPORT) {
                                    randomTeleportDestination()
                                } else {
                                    null
                                }
                            val result = inventoryManager.useConsumable(world, playerId, command.index, teleportDestination)
                            addInventoryMessage(result)
                            result.success
                        }

                        ItemType.WEAPON,
                        ItemType.ARMOR,
                        -> {
                            val result =
                                if (itemView.equippedSlot != null) {
                                    inventoryManager.unequip(world, playerId, itemView.equippedSlot)
                                } else {
                                    inventoryManager.equip(world, playerId, command.index)
                                }
                            if (result.success) {
                                StatsCalculator.recalculateAndStore(world, playerId)
                            }
                            addInventoryMessage(result)
                            result.success
                        }
                    }
                }
            }

            is PlayerCommand.Move -> {
                val from = playerPosition()
                if (!command.delta.isAdjacentTo(Point.ZERO)) {
                    addMessage("You can only move one tile at a time.")
                    false
                } else {
                    val destination = from + command.delta
                    val blocker = blockerAt(destination)
                    if (blocker != null) {
                        resolveAttack(playerId, blocker)
                        true
                    } else {
                        val result = MovementRules.attemptMove(map, from, command.delta)
                        if (result.moved) {
                            requireNotNull(world.get<Position>(playerId)).moveTo(result.destination)
                            true
                        } else {
                            addMessage("You cannot move there.")
                            false
                        }
                    }
                }
            }

            is PlayerCommand.UseTalent -> {
                val loadout = requireNotNull(world.get<com.ktome.core.talent.TalentLoadout>(playerId))
                val talentId = loadout.talentIdAt(command.slot)
                if (talentId == null) {
                    addMessage("No talent is assigned to slot ${command.slot}.")
                    false
                } else {
                    when (val result = talentResolver.resolve(world, map, playerId, talentId, command.target)) {
                        is TalentUseResult.Failure -> {
                            addMessage(result.reason)
                            false
                        }

                        is TalentUseResult.Success -> {
                            logTalentResult(result.result)
                            result.result.targets
                                .distinct()
                                .filter { target -> target != playerId && world.isAlive(target) }
                                .forEach { target ->
                                    val health = world.get<Health>(target)
                                    if (health != null && health.current <= 0) {
                                        handleDeath(target, playerId)
                                    }
                                }
                            true
                        }
                    }
                }
            }
        }

    private fun executeMonsterTurn(monsterId: EntityId) {
        if (!world.isAlive(playerId)) {
            return
        }
        if (world.get<EffectTracker>(monsterId)?.has(StatusEffectType.STUNNED) == true) {
            return
        }

        val behavior = world.get<AIBehavior>(monsterId) ?: return
        val position = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val patrolRoute = world.get<PatrolRoute>(monsterId)
        val decision = AIDecision.decide(
            AIDecisionContext(
                map = map,
                actor = AIActorSnapshot(monsterId, position, behavior, patrolRoute),
                target = AITargetSnapshot(playerId, playerPosition()),
                occupiedTiles = occupiedBlockingTiles(excluding = monsterId),
                targetVisible = playerPosition() in Shadowcasting.computeVisible(map = map, origin = position, radius = behavior.sightRadius),
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
        val stamina = requireNotNull(world.get<Stamina>(playerId))
        val result = ExperienceSystem.applyReward(experience = experience, health = health, stamina = stamina, reward = amount)

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

    private fun itemsOnGroundAt(point: Point): List<EntityId> =
        world.entitiesWith(Position::class, ItemInstance::class)
            .filter { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == point }

    private fun addInventoryMessage(result: InventoryOperationResult) {
        addMessage(result.message)
    }

    private fun logTalentResult(result: com.ktome.core.talent.TalentResult) {
        val userName = requireNotNull(world.get<Name>(result.user)).value
        addMessage("$userName uses ${result.talentName}.")
        result.effects.forEach { effect ->
            when (effect) {
                is com.ktome.core.talent.TalentEffectResult.Buff -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage(
                        when (effect.type) {
                            StatusEffectType.WAR_CRY_BUFF -> "$targetName is empowered for ${effect.duration} turns."
                            StatusEffectType.WAR_CRY_DEBUFF -> "$targetName is shaken for ${effect.duration} turns."
                            else -> "$targetName is affected."
                        },
                    )
                }

                is com.ktome.core.talent.TalentEffectResult.Damage -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage(
                        if (effect.crit) {
                            "${result.talentName} critically hits $targetName for ${effect.amount} damage."
                        } else {
                            "${result.talentName} hits $targetName for ${effect.amount} damage."
                        },
                    )
                }

                is com.ktome.core.talent.TalentEffectResult.Knockback -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage("$targetName is knocked back.")
                }

                is com.ktome.core.talent.TalentEffectResult.Miss -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage("${result.talentName} misses $targetName.")
                }

                is com.ktome.core.talent.TalentEffectResult.Movement -> Unit

                is com.ktome.core.talent.TalentEffectResult.StatusApplied -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage(
                        when (effect.type) {
                            StatusEffectType.STUNNED -> "$targetName is stunned for ${effect.duration} turns."
                            StatusEffectType.ARMOR_BREAK -> "$targetName's armor is broken for ${effect.duration} turns."
                            else -> "$targetName is affected."
                        },
                    )
                }
            }
        }
    }

    private fun randomTeleportDestination(): Point {
        val occupied = occupiedBlockingTiles(excluding = playerId)
        val candidates = map.floorPoints().filter { point -> point !in occupied }
        if (candidates.isEmpty()) {
            return playerPosition()
        }
        return candidates[sessionRandom.nextInt(0, candidates.size)]
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

    private fun refreshFov() {
        visibleTiles =
            Shadowcasting.computeVisible(
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
