package com.ktome.game

import com.ktome.core.ai.AIAction
import com.ktome.core.ai.AIActorSnapshot
import com.ktome.core.ai.AIDecision
import com.ktome.core.ai.AIDecisionContext
import com.ktome.core.ai.AITargetSnapshot
import com.ktome.core.combat.CombatResolver
import com.ktome.core.dungeon.DungeonManager
import com.ktome.core.dungeon.FloorState
import com.ktome.core.dungeon.StairDirection
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
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
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
import com.ktome.core.item.ItemDataBundle
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
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.core.random.StatefulRandomSource
import com.ktome.core.run.RunOutcome
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.StatusEffectType
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.talent.TalentResolver
import com.ktome.core.talent.TalentUseResult
import com.ktome.core.turn.TurnActorState
import com.ktome.core.turn.TurnScheduler
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate
import java.nio.file.Files

class FoundationGameSession internal constructor(
    val config: FoundationGameConfig,
    private val content: GameContent,
    private val saveManager: SaveManager,
    private val dungeonManager: DungeonManager<FloorRuntimeState>,
    private var playerSnapshot: PlayerSnapshot,
    initialMessageLog: List<String> = emptyList(),
    private var turnCount: Int = 0,
    private val inventoryManager: InventoryManager = InventoryManager(),
    private val combatRandomSource: RandomSource = defaultCombatRandomSource(config, turnCount),
    private val combatResolver: CombatResolver = CombatResolver(combatRandomSource),
    private val talentRegistry: TalentRegistry = content.talentRegistry,
    private val talentResolver: TalentResolver = TalentResolver(talentRegistry, combatResolver),
    private val sessionRandom: RandomSource = defaultSessionRandomSource(config, turnCount),
) {
    private val messageLog = ArrayDeque<String>()
    private val pendingActions = ArrayDeque<EntityId>()
    private var activeTurnActor: EntityId? = null
    private var runOutcome: RunOutcome = RunOutcome.InProgress
    private var activeFloorState: FloorRuntimeState = dungeonManager.currentState().payload
    private var world: World = SessionSnapshotMapper.restoreWorld(playerSnapshot, activeFloorState)
    private var visibleTiles: Set<Point> = emptySet()
    private var exploredTiles: LinkedHashSet<Point> = activeFloorState.exploredTiles

    init {
        initialMessageLog.forEach(::addMessage)
        refreshFov()
    }

    internal constructor(
        config: FoundationGameConfig,
        map: GameMap,
        world: World,
        playerId: EntityId,
        combatResolver: CombatResolver,
        talentRegistry: TalentRegistry,
        talentResolver: TalentResolver,
        sessionRandom: RandomSource,
        inventoryManager: InventoryManager = InventoryManager(),
    ) : this(
        config = config,
        content = compatibilityContent(talentRegistry),
        saveManager = SaveManager(Files.createTempDirectory("ktome-session-save")),
        dungeonManager = compatibilityDungeonManager(config, map, world, playerId),
        playerSnapshot = SessionSnapshotMapper.capturePlayer(world, playerId),
        initialMessageLog = listOf("You enter the dungeon."),
        inventoryManager = inventoryManager,
        combatRandomSource = UntrackedRandomSource,
        combatResolver = combatResolver,
        talentRegistry = talentRegistry,
        talentResolver = talentResolver,
        sessionRandom = sessionRandom,
    )

    val map: GameMap
        get() = activeFloorState.map

    val playerId: EntityId
        get() = EntityId(playerSnapshot.entity.id)

    fun currentFloor(): Int = dungeonManager.currentFloor

    fun maxFloor(): Int = config.maxFloor

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

    fun isGameOver(): Boolean = runOutcome is RunOutcome.Defeat

    fun isVictory(): Boolean = runOutcome is RunOutcome.Victory

    fun runOutcome(): RunOutcome = runOutcome

    fun runSummary(): RunSummary? =
        if (!runOutcome.isTerminal) {
            null
        } else {
            RunSummary(
                outcome = runOutcome,
                floorReached = currentFloor(),
                maxFloor = config.maxFloor,
                turns = turnCount,
                playerLevel = playerStatus().level,
            )
        }

    fun canAscend(): Boolean = activeFloorState.stairsUp != null && playerPosition() == activeFloorState.stairsUp

    fun canDescend(): Boolean = activeFloorState.stairsDown != null && playerPosition() == activeFloorState.stairsDown

    fun hasPendingStatAllocation(): Boolean = playerStatus().statPoints > 0

    fun hasPendingTalentAllocation(): Boolean = playerStatus().talentPoints > 0

    fun saveOnExit(): Boolean = if (runOutcome.isTerminal) false else persistRun()

    internal fun automationWorld(): World = world

    internal fun automationMovePlayerTo(point: Point) {
        require(map.isInBounds(point.x, point.y)) { "Point $point is outside the current map." }
        requireNotNull(world.get<Position>(playerId)).moveTo(point)
        refreshFov()
    }

    internal fun automationStairPoint(direction: StairDirection): Point? =
        when (direction) {
            StairDirection.UP -> activeFloorState.stairsUp
            StairDirection.DOWN -> activeFloorState.stairsDown
        }

    internal fun automationEntityByTemplateId(templateId: String): EntityId? =
        world.entitiesWith(MonsterTemplateId::class)
            .firstOrNull { entityId -> world.get<MonsterTemplateId>(entityId)?.value == templateId }

    internal fun automationForceDefeatPlayer() {
        handleDeath(playerId, null)
    }

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
        val loadout = world.get<TalentLoadout>(playerId) ?: return emptyList()
        val cooldowns = world.get<CooldownState>(playerId)?.remainingByTalentId.orEmpty()
        return loadout.slotToTalentId.entries.sortedBy { it.key }.mapNotNull { (slot, talentId) ->
            val definition = talentRegistry.get(talentId) ?: return@mapNotNull null
            TalentSlotView(
                slot = slot,
                talentId = talentId,
                name = definition.name,
                level = loadout.levelOf(talentId),
                maxLevel = definition.maxLevel,
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
        if (runOutcome.isTerminal || !world.isAlive(playerId)) {
            return false
        }

        advanceUntilPlayerTurn()
        if (runOutcome.isTerminal || !world.isAlive(playerId) || pendingActions.firstOrNull() != playerId) {
            refreshFov()
            return false
        }

        prepareActorTurn(playerId)
        val resolution = executePlayerCommand(command)
        if (!resolution.accepted) {
            refreshFov()
            return false
        }

        if (resolution.consumesTurn) {
            finishActorTurn(playerId)
            pendingActions.removeFirstOrNull()
            activeTurnActor = null
            turnCount += 1
            advanceUntilPlayerTurn()
            maybePersistCheckpoint(resolution)
        }

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
        if (runOutcome.isTerminal || !world.isAlive(playerId)) {
            return
        }

        val tickResult = TurnScheduler.tick(actorStates())
        applyRemainingEnergy(tickResult.remainingEnergy)
        pendingActions += tickResult.actionQueue
    }

    private fun advanceUntilPlayerTurn() {
        while (!runOutcome.isTerminal && world.isAlive(playerId)) {
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

        if (changed) {
            StatsCalculator.recalculateAndStore(world, actorId)
        }
    }

    private fun executePlayerCommand(command: PlayerCommand): CommandResolution =
        when (command) {
            PlayerCommand.Wait -> {
                addMessage("You wait.")
                CommandResolution.accepted()
            }

            PlayerCommand.PickUp -> {
                val item = itemsOnGroundAt(playerPosition()).firstOrNull()
                if (item == null) {
                    addMessage("There is nothing here to pick up.")
                    CommandResolution.rejected()
                } else {
                    val result = inventoryManager.pickUp(world, playerId, item)
                    addInventoryMessage(result)
                    CommandResolution(result.success, consumesTurn = result.success)
                }
            }

            PlayerCommand.Ascend -> CommandResolution(transitionFloor(StairDirection.UP), consumesTurn = true, persistCheckpointAfterTurn = true)

            PlayerCommand.Descend -> CommandResolution(transitionFloor(StairDirection.DOWN), consumesTurn = true, persistCheckpointAfterTurn = true)

            PlayerCommand.SaveGame -> {
                val saved = persistRun()
                addMessage(if (saved) "Game saved." else "Failed to save game.")
                CommandResolution(accepted = true, consumesTurn = false)
            }

            is PlayerCommand.AssignStat -> {
                val experience = requireNotNull(world.get<Experience>(playerId))
                val stats = requireNotNull(world.get<Stats>(playerId))
                if (experience.unspentStatPoints <= 0) {
                    addMessage("No unspent stat points remain.")
                    CommandResolution.rejected()
                } else {
                    when (command.stat) {
                        PrimaryStat.STR -> stats.str += 1
                        PrimaryStat.DEX -> stats.dex += 1
                        PrimaryStat.CON -> stats.con += 1
                        PrimaryStat.WIL -> stats.wil += 1
                    }
                    experience.unspentStatPoints -= 1
                    StatsCalculator.recalculateAndStore(world, playerId)
                    addMessage("You invest a point into ${command.stat.name}.")
                    CommandResolution(accepted = true, consumesTurn = false)
                }
            }

            is PlayerCommand.AssignTalent -> {
                val experience = requireNotNull(world.get<Experience>(playerId))
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId))
                val talentId = loadout.talentIdAt(command.slot)
                if (experience.unspentTalentPoints <= 0) {
                    addMessage("No unspent talent points remain.")
                    CommandResolution.rejected()
                } else if (talentId == null) {
                    addMessage("No talent is assigned to slot ${command.slot}.")
                    CommandResolution.rejected()
                } else {
                    val definition = requireNotNull(talentRegistry.get(talentId))
                    val currentLevel = loadout.levelOf(talentId)
                    if (currentLevel >= definition.maxLevel) {
                        addMessage("${definition.name} is already at maximum level.")
                        CommandResolution.rejected()
                    } else {
                        loadout.talentLevels[talentId] = currentLevel + 1
                        experience.unspentTalentPoints -= 1
                        addMessage("${definition.name} advances to level ${currentLevel + 1}.")
                        CommandResolution(accepted = true, consumesTurn = false)
                    }
                }
            }

            is PlayerCommand.ActivateInventoryItem -> {
                val itemView = inventoryItems().getOrNull(command.index)
                if (itemView == null) {
                    addMessage("That inventory slot is empty.")
                    CommandResolution.rejected()
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
                            CommandResolution(result.success, consumesTurn = result.success)
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
                            CommandResolution(result.success, consumesTurn = false)
                        }
                    }
                }
            }

            is PlayerCommand.Move -> {
                val from = playerPosition()
                if (!command.delta.isAdjacentTo(Point.ZERO)) {
                    addMessage("You can only move one tile at a time.")
                    CommandResolution.rejected()
                } else {
                    val destination = from + command.delta
                    val blocker = blockerAt(destination)
                    if (blocker != null) {
                        resolveAttack(playerId, blocker)
                        CommandResolution.accepted()
                    } else {
                        val result = MovementRules.attemptMove(map, from, command.delta)
                        if (result.moved) {
                            requireNotNull(world.get<Position>(playerId)).moveTo(result.destination)
                            CommandResolution.accepted()
                        } else {
                            addMessage("You cannot move there.")
                            CommandResolution.rejected()
                        }
                    }
                }
            }

            is PlayerCommand.UseTalent -> {
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId))
                val talentId = loadout.talentIdAt(command.slot)
                if (talentId == null) {
                    addMessage("No talent is assigned to slot ${command.slot}.")
                    CommandResolution.rejected()
                } else {
                    when (val result = talentResolver.resolve(world, map, playerId, talentId, command.target)) {
                        is TalentUseResult.Failure -> {
                            addMessage(result.reason)
                            CommandResolution.rejected()
                        }

                        is TalentUseResult.Success -> {
                            logTalentResult(result.result)
                            handleTalentDeaths(result.result.targets, playerId)
                            CommandResolution.accepted()
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
        if (tryUseMonsterTalent(monsterId)) {
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

    private fun tryUseMonsterTalent(monsterId: EntityId): Boolean {
        val loadout = world.get<TalentLoadout>(monsterId) ?: return false
        val actorPosition = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val targetPosition = playerPosition()
        val prioritizedTalentIds =
            listOfNotNull(
                loadout.slotToTalentId.values.firstOrNull { it == "war_cry" },
                loadout.slotToTalentId.values.firstOrNull { it == "power_strike" },
            )

        prioritizedTalentIds.forEach { talentId ->
            val definition = talentRegistry.get(talentId) ?: return@forEach
            if (talentId == "war_cry" && world.get<EffectTracker>(monsterId)?.has(StatusEffectType.WAR_CRY_BUFF) == true) {
                return@forEach
            }
            val distance = actorPosition.chebyshevDistanceTo(targetPosition)
            if (distance !in definition.minRange..definition.range.coerceAtLeast(definition.minRange)) {
                return@forEach
            }
            val target = if (definition.range > 0) targetPosition else null
            when (val result = talentResolver.resolve(world, map, monsterId, talentId, target)) {
                is TalentUseResult.Failure -> return@forEach
                is TalentUseResult.Success -> {
                    logTalentResult(result.result)
                    handleTalentDeaths(result.result.targets, monsterId)
                    return true
                }
            }
        }

        return false
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
            runOutcome = RunOutcome.Defeat(currentFloor())
            pendingActions.clear()
            activeTurnActor = null
            saveManager.deleteSave()
            addMessage("You die. Game over.")
            return
        }

        val targetName = requireNotNull(world.get<Name>(target)).value
        val isBoss =
            currentFloor() == config.maxFloor &&
                world.get<MonsterTemplateId>(target)?.value == content.bossDefinition.template.id

        addMessage("$targetName dies.")
        val reward = world.get<ExperienceReward>(target)?.value ?: 0
        world.destroyEntity(target)
        if (killer == playerId && reward > 0) {
            gainExperience(reward)
        }
        if (isBoss) {
            runOutcome = RunOutcome.Victory(currentFloor())
            pendingActions.clear()
            activeTurnActor = null
            saveManager.deleteSave()
            addMessage("You defeat $targetName. Victory!")
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

    private fun transitionFloor(direction: StairDirection): Boolean {
        val requiredStair =
            when (direction) {
                StairDirection.UP -> activeFloorState.stairsUp
                StairDirection.DOWN -> activeFloorState.stairsDown
            }
        if (requiredStair == null || playerPosition() != requiredStair) {
            addMessage(
                when (direction) {
                    StairDirection.UP -> "There are no stairs leading up here."
                    StairDirection.DOWN -> "There are no stairs leading down here."
                },
            )
            return false
        }

        syncActiveFloorState()
        val transition = dungeonManager.transition(direction)
        activeFloorState = transition.state.payload
        exploredTiles = activeFloorState.exploredTiles
        playerSnapshot = playerSnapshot.copy(entity = playerSnapshot.entity.copy(position = transition.entryPoint))
        world = SessionSnapshotMapper.restoreWorld(playerSnapshot, activeFloorState)
        pendingActions.clear()
        activeTurnActor = null
        refreshFov()

        addMessage(
            when (direction) {
                StairDirection.UP -> "You ascend to floor ${transition.toFloor}."
                StairDirection.DOWN -> "You descend to floor ${transition.toFloor}."
            },
        )
        return true
    }

    private fun persistRun(): Boolean {
        if (runOutcome.isTerminal) {
            return false
        }

        syncActiveFloorState()
        val floors =
            dungeonManager.knownFloors()
                .sorted()
                .mapNotNull { floor -> dungeonManager.stateOf(floor) }
        return saveManager.save(
            SessionSnapshotMapper.toSaveSnapshot(
                config = config,
                currentFloor = currentFloor(),
                turnCount = turnCount,
                messageLog = messageLog(),
                player = playerSnapshot,
                floors = floors,
                combatRandomState = (combatRandomSource as? StatefulRandomSource)?.snapshotState(),
                sessionRandomState = (sessionRandom as? StatefulRandomSource)?.snapshotState(),
            ),
        )
    }

    private fun maybePersistCheckpoint(resolution: CommandResolution) {
        if (!resolution.persistCheckpointAfterTurn || runOutcome.isTerminal) {
            return
        }

        if (persistRun()) {
            addMessage("Checkpoint saved.")
        }
    }

    private fun syncActiveFloorState() {
        playerSnapshot = SessionSnapshotMapper.capturePlayer(world, playerId)
        val excludedEntities =
            linkedSetOf<EntityId>().apply {
                add(playerId)
                playerSnapshot.carriedEntities.mapTo(this) { snapshot -> EntityId(snapshot.id) }
            }
        activeFloorState =
            SessionSnapshotMapper.captureFloor(
                map = map,
                stairsUp = activeFloorState.stairsUp,
                stairsDown = activeFloorState.stairsDown,
                exploredTiles = exploredTiles,
                world = world,
                excludedEntities = excludedEntities,
            )
        dungeonManager.replaceCurrentState(
            FloorState(
                floor = currentFloor(),
                stairsUp = activeFloorState.stairsUp,
                stairsDown = activeFloorState.stairsDown,
                payload = activeFloorState,
            ),
        )
    }

    private fun handleTalentDeaths(
        targets: List<EntityId>,
        killer: EntityId,
    ) {
        targets
            .distinct()
            .filter { target -> world.isAlive(target) }
            .forEach { target ->
                val health = world.get<Health>(target)
                if (health != null && health.current <= 0) {
                    handleDeath(target, killer)
                }
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

    private data class CommandResolution(
        val accepted: Boolean,
        val consumesTurn: Boolean,
        val persistCheckpointAfterTurn: Boolean = false,
    ) {
        companion object {
            fun accepted(): CommandResolution = CommandResolution(accepted = true, consumesTurn = true)

            fun rejected(): CommandResolution = CommandResolution(accepted = false, consumesTurn = false)
        }
    }

    companion object {
        private const val COMBAT_RANDOM_SALT: Long = 0xC0FFEE
        private const val SESSION_RANDOM_SALT: Long = 0x51A17A

        private fun compatibilityContent(talentRegistry: TalentRegistry): GameContent =
            GameContent(
                talents = emptyList(),
                talentRegistry = talentRegistry,
                monsterCatalog = emptyList(),
                itemBundle = ItemDataBundle(emptyList(), emptyList(), emptyList()),
                bossDefinition =
                    BossDefinition(
                        template =
                            MonsterTemplate(
                                id = "compatibility_boss",
                                name = "Compatibility Boss",
                                glyph = 'B',
                                colorHex = "#FFFFFF",
                                stats = Stats(str = 1, dex = 1, con = 1, wil = 1),
                                baseHp = 1,
                                baseAttack = 1,
                                baseDefense = 0,
                                speed = 100,
                                ai = com.ktome.core.ecs.AIType.CHASE,
                                expReward = 0,
                                spawnFloors = listOf(1),
                                spawnWeight = 1,
                            ),
                        talentLevels = emptyMap(),
                    ),
            )

        private fun compatibilityDungeonManager(
            config: FoundationGameConfig,
            map: GameMap,
            world: World,
            playerId: EntityId,
        ): DungeonManager<FloorRuntimeState> {
            val playerSnapshot = SessionSnapshotMapper.capturePlayer(world, playerId)
            val excludedEntities =
                linkedSetOf<EntityId>().apply {
                    add(playerId)
                    playerSnapshot.carriedEntities.mapTo(this) { snapshot -> EntityId(snapshot.id) }
                }
            val floorState =
                FloorState(
                    floor = config.floor,
                    payload =
                        SessionSnapshotMapper.captureFloor(
                            map = map,
                            stairsUp = null,
                            stairsDown = null,
                            exploredTiles = emptySet(),
                            world = world,
                            excludedEntities = excludedEntities,
                        ),
                )
            return DungeonManager(
                maxFloor = config.maxFloor,
                startFloor = config.floor,
                floorLoader = { requestedFloor ->
                    require(requestedFloor == config.floor) {
                        "Compatibility session only supports the initial floor."
                    }
                    floorState
                },
            ).apply {
                putState(floorState)
            }
        }

        internal fun defaultCombatRandomSource(
            config: FoundationGameConfig,
            turnCount: Int,
        ): StatefulRandomSource = SplitMix64RandomSource.fromSeed(config.seed xor turnCount.toLong() xor COMBAT_RANDOM_SALT)

        internal fun defaultSessionRandomSource(
            config: FoundationGameConfig,
            turnCount: Int,
        ): StatefulRandomSource = SplitMix64RandomSource.fromSeed(config.seed xor turnCount.toLong() xor SESSION_RANDOM_SALT)

        private data object UntrackedRandomSource : RandomSource {
            override fun nextDouble(): Double = 0.0

            override fun nextInt(
                fromInclusive: Int,
                untilExclusive: Int,
            ): Int = fromInclusive
        }
    }
}
