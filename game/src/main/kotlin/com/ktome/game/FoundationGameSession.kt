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
import com.ktome.core.ecs.Stair
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
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.Inventory
import com.ktome.core.item.InventoryManager
import com.ktome.core.item.InventoryOperationCode
import com.ktome.core.item.InventoryOperationResult
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemType
import com.ktome.core.item.MaterialDef
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.movement.MovementRules
import com.ktome.core.progression.ExperienceSystem
import com.ktome.core.random.RandomSource
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.core.random.StatefulRandomSource
import com.ktome.core.run.RunOutcome
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.StatusEffectType
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.talent.TalentFailureCode
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.talent.TalentResolver
import com.ktome.core.talent.TalentUseResult
import com.ktome.core.turn.TurnActorState
import com.ktome.core.turn.TurnScheduler
import com.ktome.game.data.schema.ItemBundleSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.i18n.Localizer
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
    private val restoredPendingActionIds: List<Int> = emptyList(),
    private val restoredActiveTurnActorId: Int? = null,
) {
    private val messageLog = ArrayDeque<String>()
    private val recentEvents = ArrayDeque<String>()
    private val pendingActions = ArrayDeque<EntityId>()
    private var activeTurnActor: EntityId? = null
    private var runOutcome: RunOutcome = RunOutcome.InProgress
    private var activeFloorState: FloorRuntimeState = dungeonManager.currentState().payload
    private var world: World = SessionSnapshotMapper.restoreWorld(content, playerSnapshot, activeFloorState)
    private var visibleTiles: Set<Point> = emptySet()
    private var exploredTiles: LinkedHashSet<Point> = activeFloorState.exploredTiles

    init {
        initialMessageLog.forEach(::addMessage)
        restorePendingTurnState()
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
        content = compatibilityContent(talentRegistry = talentRegistry, world = world, currentFloor = config.floor),
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
        restoredPendingActionIds = emptyList(),
        restoredActiveTurnActorId = null,
    )

    val map: GameMap
        get() = activeFloorState.map

    val playerId: EntityId
        get() = EntityId(playerSnapshot.entity.id)

    fun currentFloor(): Int = dungeonManager.currentFloor

    fun maxFloor(): Int = config.maxFloor

    fun localizer(): Localizer = content.localizer

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

    fun currentTurnCount(): Int = turnCount

    fun recentEventLog(limit: Int = 20): List<String> = recentEvents.takeLast(limit)

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
                range = definition.range,
                minRange = definition.minRange,
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

    fun inspectAt(point: Point): InspectView {
        require(map.isInBounds(point.x, point.y)) { "Point $point is outside the current map." }

        val visibility =
            when {
                point in visibleTiles -> TileVisibility.VISIBLE
                point in exploredTiles -> TileVisibility.EXPLORED
                else -> TileVisibility.HIDDEN
            }

        if (visibility == TileVisibility.HIDDEN) {
            return InspectView(point = point, visibility = visibility, terrainName = tr("tile.unknown.name"))
        }

        return InspectView(
            point = point,
            visibility = visibility,
            terrainName = tileName(map[point]),
            actor =
                if (visibility == TileVisibility.VISIBLE) {
                    actorAt(point)?.let(::inspectActorView)
                } else {
                    null
                },
            items =
                if (visibility == TileVisibility.VISIBLE) {
                    itemsOnGroundAt(point).mapNotNull { itemId -> world.get<ItemInstance>(itemId)?.let(::inspectItemView) }
                } else {
                    emptyList()
                },
            stairLabel = stairLabelAt(point),
        )
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
                addMessage(tr("log.wait"))
                CommandResolution.accepted()
            }

            PlayerCommand.PickUp -> {
                val item = itemsOnGroundAt(playerPosition()).firstOrNull()
                if (item == null) {
                    addMessage(tr("log.nothing_to_pick_up"))
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
                addMessage(if (saved) tr("log.save.success") else tr("log.save.failure"))
                CommandResolution(accepted = true, consumesTurn = false)
            }

            is PlayerCommand.AssignStat -> {
                val experience = requireNotNull(world.get<Experience>(playerId))
                val stats = requireNotNull(world.get<Stats>(playerId))
                if (experience.unspentStatPoints <= 0) {
                    addMessage(tr("log.stat.none"))
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
                    addMessage(tr("log.stat.invest", "stat" to primaryStatLabel(command.stat)))
                    CommandResolution(accepted = true, consumesTurn = false)
                }
            }

            is PlayerCommand.AssignTalent -> {
                val experience = requireNotNull(world.get<Experience>(playerId))
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId))
                val talentId = loadout.talentIdAt(command.slot)
                if (experience.unspentTalentPoints <= 0) {
                    addMessage(tr("log.talent.none"))
                    CommandResolution.rejected()
                } else if (talentId == null) {
                    addMessage(tr("log.talent.slot_empty", "slot" to command.slot))
                    CommandResolution.rejected()
                } else {
                    val definition = requireNotNull(talentRegistry.get(talentId))
                    val currentLevel = loadout.levelOf(talentId)
                    if (currentLevel >= definition.maxLevel) {
                        addMessage(tr("log.talent.max_level", "talent" to definition.name))
                        CommandResolution.rejected()
                    } else {
                        loadout.talentLevels[talentId] = currentLevel + 1
                        experience.unspentTalentPoints -= 1
                        addMessage(tr("log.talent.advance", "talent" to definition.name, "level" to (currentLevel + 1)))
                        CommandResolution(accepted = true, consumesTurn = false)
                    }
                }
            }

            is PlayerCommand.ActivateInventoryItem -> {
                val itemView = inventoryItems().getOrNull(command.index)
                if (itemView == null) {
                    addMessage(tr("log.inventory.slot_empty"))
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
                    addMessage(tr("log.move.single_step_only"))
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
                            addMessage(tr("log.move.cannot_move"))
                            CommandResolution.rejected()
                        }
                    }
                }
            }

            is PlayerCommand.UseTalent -> {
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId))
                val talentId = loadout.talentIdAt(command.slot)
                if (talentId == null) {
                    addMessage(tr("log.talent.slot_empty", "slot" to command.slot))
                    CommandResolution.rejected()
                } else {
                    when (val result = talentResolver.resolve(world, map, playerId, talentId, command.target)) {
                        is TalentUseResult.Failure -> {
                            addMessage(localizeTalentFailure(result))
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

    private fun inspectActorView(entityId: EntityId): InspectActorView {
        val stats = requireNotNull(world.get<Stats>(entityId))
        val health = requireNotNull(world.get<Health>(entityId))
        val derived = requireNotNull(world.get<DerivedStats>(entityId))
        val behavior = world.get<AIBehavior>(entityId)
        val role =
            when {
                entityId == playerId -> tr("actor.player.role")
                world.get<MonsterTemplateId>(entityId)?.value == FOUNDATION_BOSS_TEMPLATE_ID -> tr("actor.boss.role")
                behavior != null -> tr("actor.monster.role", "ai" to aiLabel(behavior.type))
                else -> tr("actor.generic.role")
            }

        return InspectActorView(
            name = requireNotNull(world.get<Name>(entityId)).value,
            role = role,
            currentHp = health.current,
            maxHp = health.max,
            attack = derived.attack,
            defense = derived.defense,
            accuracy = derived.accuracy,
            evasion = derived.evasion,
            speed = derived.speed,
            strength = stats.str,
            dexterity = stats.dex,
            constitution = stats.con,
            willpower = stats.wil,
            statusEffects = activeStatusEffects(entityId),
        )
    }

    private fun inspectItemView(item: ItemInstance): InspectItemView {
        return InspectItemView(
            name = item.name,
            typeLabel = itemTypeLabel(item.type),
            details = itemDetailLines(item),
        )
    }

    private fun itemDetailLines(item: ItemInstance): List<String> =
        buildList {
            item.slot?.let { add(tr("ui.inspect.slot", "slot" to equipSlotLabel(it))) }
            addAll(statModifierLines(item.stats))
            when (item.effect) {
                com.ktome.core.item.ConsumableEffect.HEAL -> add(tr("ui.inspect.restore_hp", "amount" to item.magnitude))
                com.ktome.core.item.ConsumableEffect.TELEPORT -> add(tr("ui.inspect.teleport_random"))
                null -> Unit
            }
            if (isEmpty()) {
                add(tr("ui.inspect.no_special_effect"))
            }
        }

    private fun statModifierLines(modifier: StatModifier): List<String> =
        buildList {
            addModifier(tr("ui.stat.str"), modifier.str)
            addModifier(tr("ui.stat.dex"), modifier.dex)
            addModifier(tr("ui.stat.con"), modifier.con)
            addModifier(tr("ui.stat.wil"), modifier.wil)
            addModifier(tr("ui.hud.attack.short"), modifier.attack)
            addModifier(tr("ui.hud.defense.short"), modifier.defense)
            addModifier(tr("ui.hud.accuracy.short"), modifier.accuracy)
            addModifier(tr("ui.hud.evasion.short"), modifier.evasion)
            addModifier(tr("ui.hud.speed.short"), modifier.speed)
            addModifier(tr("ui.hud.hp.short"), modifier.maxHp)
            addModifier(tr("ui.hud.stamina.short"), modifier.maxStamina)
            addDecimalModifier(tr("ui.inspect.mod.hp_regen"), modifier.hpRegen)
            addDecimalModifier(tr("ui.inspect.mod.stamina_regen"), modifier.staminaRegen)
            addPercentModifier(tr("ui.inspect.mod.crit"), modifier.critChance)
            addPercentModifier(tr("ui.inspect.mod.talent"), modifier.talentPower)
        }

    private fun MutableList<String>.addModifier(
        label: String,
        value: Int,
    ) {
        if (value != 0) {
            add("$label ${signed(value)}")
        }
    }

    private fun MutableList<String>.addDecimalModifier(
        label: String,
        value: Double,
    ) {
        if (value != 0.0) {
            add("$label ${signedDecimal(value)}")
        }
    }

    private fun MutableList<String>.addPercentModifier(
        label: String,
        value: Double,
    ) {
        if (value != 0.0) {
            val percent = (value * 100).toInt()
            add("$label ${signed(percent)}%")
        }
    }

    private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

    private fun signedDecimal(value: Double): String {
        val normalized =
            if (value % 1.0 == 0.0) {
                value.toInt().toString()
            } else {
                "%.1f".format(value)
            }
        return if (value > 0) "+$normalized" else normalized
    }

    private fun activeStatusEffects(entityId: EntityId): List<String> =
        world.get<EffectTracker>(entityId)
            ?.effects
            ?.filter { effect -> effect.remainingTurns > 0 }
            ?.map { effect -> tr("ui.inspect.effect.turns", "name" to statusEffectName(effect.type), "turns" to effect.remainingTurns) }
            .orEmpty()

    private fun tileName(tile: com.ktome.core.map.TileType): String =
        when (tile) {
            com.ktome.core.map.TileType.FLOOR -> tr("tile.floor.name")
            com.ktome.core.map.TileType.WALL -> tr("tile.wall.name")
        }

    private fun actorAt(point: Point): EntityId? =
        world.entitiesWith(Position::class, Health::class, Name::class)
            .firstOrNull { entityId ->
                requireNotNull(world.get<Position>(entityId)).toPoint() == point &&
                    requireNotNull(world.get<Health>(entityId)).current > 0
            }

    private fun stairLabelAt(point: Point): String? =
        world.entitiesWith(Position::class, Stair::class)
            .firstOrNull { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == point }
            ?.let { entityId -> stairName(requireNotNull(world.get<Stair>(entityId)).direction) }

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
            addMessage(tr("log.attack.miss", "attacker" to attackerName, "target" to targetName))
            return
        }

        targetHealth.current = (targetHealth.current - result.finalDamage).coerceAtLeast(0)
        logEvent(DamageDealtEvent(attacker, target, result.finalDamage, result.critical))
        addMessage(
            if (result.critical) {
                tr("log.attack.crit", "attacker" to attackerName, "target" to targetName, "damage" to result.finalDamage)
            } else {
                tr("log.attack.hit", "attacker" to attackerName, "target" to targetName, "damage" to result.finalDamage)
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
            addMessage(tr("log.player.death"))
            return
        }

        val targetName = requireNotNull(world.get<Name>(target)).value
        val isBoss =
            currentFloor() == config.maxFloor &&
                world.get<MonsterTemplateId>(target)?.value == content.bossDefinition.template.id

        addMessage(tr("log.entity.death", "target" to targetName))
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
            addMessage(tr("log.victory", "target" to targetName))
        }
    }

    private fun gainExperience(amount: Int) {
        val experience = requireNotNull(world.get<Experience>(playerId))
        val health = requireNotNull(world.get<Health>(playerId))
        val stamina = requireNotNull(world.get<Stamina>(playerId))
        val result = ExperienceSystem.applyReward(experience = experience, health = health, stamina = stamina, reward = amount)

        logEvent(ExperienceGainedEvent(playerId, amount))
        addMessage(tr("log.xp.gain", "amount" to amount))

        if (result.levelsGained > 0) {
            logEvent(
                LevelUpEvent(
                    entity = playerId,
                    newLevel = experience.level,
                    unspentStatPoints = experience.unspentStatPoints,
                    unspentTalentPoints = experience.unspentTalentPoints,
                ),
            )
            addMessage(tr("log.level_up", "level" to experience.level))
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
                    StairDirection.UP -> tr("log.stairs.missing_up")
                    StairDirection.DOWN -> tr("log.stairs.missing_down")
                },
            )
            return false
        }

        syncActiveFloorState()
        val transition = dungeonManager.transition(direction)
        activeFloorState = transition.state.payload
        exploredTiles = activeFloorState.exploredTiles
        playerSnapshot = playerSnapshot.copy(entity = playerSnapshot.entity.copy(position = PointSnapshot.from(transition.entryPoint)))
        world = SessionSnapshotMapper.restoreWorld(content, playerSnapshot, activeFloorState)
        pendingActions.clear()
        activeTurnActor = null
        refreshFov()

        addMessage(
            when (direction) {
                StairDirection.UP -> tr("log.stairs.ascend", "floor" to transition.toFloor)
                StairDirection.DOWN -> tr("log.stairs.descend", "floor" to transition.toFloor)
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
                player = playerSnapshot,
                floors = floors,
                combatRandomState = (combatRandomSource as? StatefulRandomSource)?.snapshotState(),
                sessionRandomState = (sessionRandom as? StatefulRandomSource)?.snapshotState(),
                pendingActionIds = pendingActions.map(EntityId::value),
                activeTurnActorId = activeTurnActor?.value,
            ),
        )
    }

    private fun maybePersistCheckpoint(resolution: CommandResolution) {
        if (!resolution.persistCheckpointAfterTurn || runOutcome.isTerminal) {
            return
        }

        if (persistRun()) {
            addMessage(tr("log.checkpoint.saved"))
        }
    }

    private fun restorePendingTurnState() {
        pendingActions.clear()
        restoredPendingActionIds
            .map(::EntityId)
            .filter(world::isAlive)
            .forEach(pendingActions::addLast)
        activeTurnActor =
            restoredActiveTurnActorId
                ?.let(::EntityId)
                ?.takeIf(world::isAlive)
                ?.takeIf { actorId -> actorId in pendingActions }
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
        addMessage(localizeInventoryMessage(result))
    }

    private fun logTalentResult(result: com.ktome.core.talent.TalentResult) {
        val userName = requireNotNull(world.get<Name>(result.user)).value
        addMessage(tr("log.talent.use", "user" to userName, "talent" to result.talentName))
        result.effects.forEach { effect ->
            when (effect) {
                is com.ktome.core.talent.TalentEffectResult.Buff -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage(
                        when (effect.type) {
                            StatusEffectType.WAR_CRY_BUFF -> tr("log.talent.target_empowered", "target" to targetName, "turns" to effect.duration)
                            StatusEffectType.WAR_CRY_DEBUFF -> tr("log.talent.target_shaken", "target" to targetName, "turns" to effect.duration)
                            else -> tr("log.talent.target_affected", "target" to targetName)
                        },
                    )
                }

                is com.ktome.core.talent.TalentEffectResult.Damage -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage(
                        if (effect.crit) {
                            tr("log.talent.damage_crit", "talent" to result.talentName, "target" to targetName, "damage" to effect.amount)
                        } else {
                            tr("log.talent.damage", "talent" to result.talentName, "target" to targetName, "damage" to effect.amount)
                        },
                    )
                }

                is com.ktome.core.talent.TalentEffectResult.Knockback -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage(tr("log.talent.knockback", "target" to targetName))
                }

                is com.ktome.core.talent.TalentEffectResult.Miss -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage(tr("log.talent.miss", "talent" to result.talentName, "target" to targetName))
                }

                is com.ktome.core.talent.TalentEffectResult.Movement -> Unit

                is com.ktome.core.talent.TalentEffectResult.StatusApplied -> {
                    val targetName = requireNotNull(world.get<Name>(effect.target)).value
                    addMessage(
                        when (effect.type) {
                            StatusEffectType.STUNNED -> tr("log.talent.target_stunned", "target" to targetName, "turns" to effect.duration)
                            StatusEffectType.ARMOR_BREAK -> tr("log.talent.target_armor_broken", "target" to targetName, "turns" to effect.duration)
                            else -> tr("log.talent.target_affected", "target" to targetName)
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

    private fun tr(
        key: String,
        vararg args: Pair<String, Any?>,
    ): String = content.localizer.text(key, *args)

    private fun primaryStatLabel(stat: PrimaryStat): String =
        when (stat) {
            PrimaryStat.STR -> tr("ui.stat.str")
            PrimaryStat.DEX -> tr("ui.stat.dex")
            PrimaryStat.CON -> tr("ui.stat.con")
            PrimaryStat.WIL -> tr("ui.stat.wil")
        }

    private fun itemTypeLabel(type: ItemType): String =
        when (type) {
            ItemType.WEAPON -> tr("ui.item.type.weapon")
            ItemType.ARMOR -> tr("ui.item.type.armor")
            ItemType.CONSUMABLE -> tr("ui.item.type.consumable")
        }

    private fun equipSlotLabel(slot: EquipSlot): String =
        when (slot) {
            EquipSlot.WEAPON -> tr("ui.sidebar.weapon")
            EquipSlot.ARMOR -> tr("ui.sidebar.armor")
        }

    private fun stairName(direction: StairDirection): String =
        when (direction) {
            StairDirection.UP -> tr("stairs.up.name")
            StairDirection.DOWN -> tr("stairs.down.name")
        }

    private fun aiLabel(type: com.ktome.core.ecs.AIType): String =
        when (type) {
            com.ktome.core.ecs.AIType.CHASE -> tr("ai.chase")
            com.ktome.core.ecs.AIType.KITE -> tr("ai.kite")
            com.ktome.core.ecs.AIType.PATROL -> tr("ai.patrol")
        }

    private fun statusEffectName(type: StatusEffectType): String =
        when (type) {
            StatusEffectType.STUNNED -> tr("status.stunned")
            StatusEffectType.ARMOR_BREAK -> tr("status.armor_break")
            StatusEffectType.WAR_CRY_BUFF -> tr("status.war_cry_buff")
            StatusEffectType.WAR_CRY_DEBUFF -> tr("status.war_cry_debuff")
        }

    private fun localizeTalentFailure(result: TalentUseResult.Failure): String =
        when (result.code) {
            TalentFailureCode.UNKNOWN_TALENT -> tr("log.talent.failure.unknown")
            TalentFailureCode.UNSUPPORTED_TALENT -> tr("log.talent.failure.unsupported")
            TalentFailureCode.COOLDOWN ->
                tr(
                    "log.talent.failure.cooldown",
                    "talent" to (result.talentName ?: tr("log.talent.failure.unknown")),
                )
            TalentFailureCode.NO_STAMINA -> tr("log.talent.failure.no_stamina")
            TalentFailureCode.TARGET_REQUIRED -> tr("log.talent.failure.target_required")
            TalentFailureCode.OUT_OF_RANGE -> tr("log.talent.failure.out_of_range")
            TalentFailureCode.NO_TARGET -> tr("log.talent.failure.no_target")
            TalentFailureCode.NO_CHARGE_PATH -> tr("log.talent.failure.no_charge_path")
        }

    private fun localizeInventoryMessage(result: InventoryOperationResult): String =
        when (result) {
            is InventoryOperationResult.Success ->
                when (result.code) {
                    InventoryOperationCode.PICK_UP -> tr("log.inventory.pick_up", "item" to result.itemName)
                    InventoryOperationCode.EQUIP -> tr("log.inventory.equip", "item" to result.itemName)
                    InventoryOperationCode.REMOVE -> tr("log.inventory.remove", "item" to result.itemName)
                    InventoryOperationCode.CONSUME_USE -> tr("log.inventory.consume.use", "item" to result.itemName)
                    InventoryOperationCode.CONSUME_READ -> tr("log.inventory.consume.read", "item" to result.itemName)
                    InventoryOperationCode.DROP -> tr("log.inventory.drop", "item" to result.itemName)
                    else -> result.message
                }

            is InventoryOperationResult.Failure ->
                when (result.code) {
                    InventoryOperationCode.NOT_ITEM -> tr("log.inventory.not_item")
                    InventoryOperationCode.NOT_ON_GROUND -> tr("log.inventory.not_on_ground", "item" to result.itemName)
                    InventoryOperationCode.PACK_FULL -> tr("log.inventory.pack_full")
                    InventoryOperationCode.PACK_SLOT_EMPTY -> tr("log.inventory.pack_slot_empty")
                    InventoryOperationCode.CANNOT_EQUIP -> tr("log.inventory.cannot_equip", "item" to result.itemName)
                    InventoryOperationCode.NOTHING_EQUIPPED ->
                        tr("log.inventory.slot_nothing_equipped", "slot" to (result.slot?.let(::equipSlotLabel) ?: "-"))
                    InventoryOperationCode.NOT_CONSUMABLE -> tr("log.inventory.not_consumable", "item" to result.itemName)
                    InventoryOperationCode.NO_TELEPORT_DESTINATION -> tr("log.inventory.no_teleport_destination")
                    else -> result.message
                }
        }

    private fun logEvent(event: Any) {
        if (recentEvents.size == 20) {
            recentEvents.removeFirst()
        }
        recentEvents += summarizeEvent(event)
    }

    private fun summarizeEvent(event: Any): String =
        when (event) {
            is DamageDealtEvent -> "damage:${event.attacker.value}->${event.target.value}:${event.damage}${if (event.crit) ":crit" else ""}"
            is MissEvent -> "miss:${event.attacker.value}->${event.target.value}"
            is EntityDeathEvent -> "death:${event.entity.value}:${event.killer?.value ?: "none"}"
            is ExperienceGainedEvent -> "xp:${event.entity.value}:${event.amount}"
            is LevelUpEvent -> "level:${event.entity.value}:${event.newLevel}"
            else -> event::class.simpleName ?: "UnknownEvent"
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

        private fun compatibilityContent(
            talentRegistry: TalentRegistry,
            world: World,
            currentFloor: Int,
        ): GameContent =
            GameContent(
                talents = emptyList(),
                talentRegistry = talentRegistry,
                monsterCatalog = compatibilityMonsterCatalog(world, currentFloor),
                itemBundle = compatibilityItemBundle(world, currentFloor),
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
                schemaCatalog =
                    SchemaCatalog(
                        professions = emptyList(),
                        talents = emptyList(),
                        talentTrees = emptyList(),
                        monsters = emptyList(),
                        bossEncounters = emptyList(),
                        zones = emptyList(),
                        difficulties = emptyList(),
                        itemBundle = ItemBundleSchemaV2(materials = emptyList(), affixes = emptyList(), items = emptyList()),
                        lootProfiles = emptyList(),
                        tilesets = emptyList(),
                        aiProfiles = emptyList(),
                        arenas = emptyList(),
                        ambientProfiles = emptyList(),
                        visualKeys = emptySet(),
                        audioProfiles = emptySet(),
                    ),
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            )

        private fun compatibilityMonsterCatalog(
            world: World,
            currentFloor: Int,
        ): List<MonsterTemplate> =
            world.entitiesWith(MonsterTemplateId::class)
                .mapNotNull { entityId ->
                    val templateId = world.get<MonsterTemplateId>(entityId)?.value ?: return@mapNotNull null
                    val profile = world.get<com.ktome.core.ecs.CombatProfile>(entityId)
                    MonsterTemplate(
                        id = templateId,
                        name = world.get<Name>(entityId)?.value ?: templateId,
                        glyph = world.get<Glyph>(entityId)?.value ?: 'M',
                        colorHex = world.get<DisplayColor>(entityId)?.hex ?: "#AAAAAA",
                        stats = world.get<Stats>(entityId)?.copy() ?: Stats(str = 1, dex = 1, con = 1, wil = 1),
                        baseHp = profile?.baseHp ?: world.get<Health>(entityId)?.max ?: 1,
                        baseAttack = profile?.baseAttack ?: 1,
                        baseDefense = profile?.baseDefense ?: 0,
                        speed = profile?.baseSpeed ?: 100,
                        ai = world.get<AIBehavior>(entityId)?.type ?: com.ktome.core.ecs.AIType.CHASE,
                        expReward = world.get<ExperienceReward>(entityId)?.value ?: 0,
                        spawnFloors = listOf(currentFloor),
                        spawnWeight = 1,
                    )
                }.distinctBy(MonsterTemplate::id)

        private fun compatibilityItemBundle(
            world: World,
            currentFloor: Int,
        ): ItemDataBundle {
            val items =
                world.entitiesWith(ItemInstance::class)
                    .mapNotNull { entityId -> world.get<ItemInstance>(entityId) }

            val baseItems =
                items.distinctBy(ItemInstance::baseId).map { item ->
                    ItemBaseDef(
                        id = item.baseId,
                        name = item.name,
                        type = item.type,
                        slot = item.slot,
                        glyph = item.glyph,
                        colorHex = item.colorHex,
                        baseStats = item.stats.copy(),
                        allowedMaterials = item.materialId?.let(::listOf).orEmpty(),
                        dropFloors = listOf(currentFloor),
                        dropWeight = 1,
                        effect = item.effect,
                        magnitude = item.magnitude,
                    )
                }

            val materials =
                items.mapNotNull { item ->
                    item.materialId?.let { materialId ->
                        MaterialDef(
                            id = materialId,
                            name = item.materialName ?: compatibilityLabel(materialId),
                            minFloor = 1,
                        )
                    }
                }.distinctBy(MaterialDef::id)

            val affixes =
                items.flatMap(ItemInstance::affixes)
                    .map { affix -> affix.copy(statModifiers = affix.statModifiers.copy()) }
                    .distinctBy { affix -> affix.id }

            return ItemDataBundle(
                baseItems = baseItems,
                materials = materials,
                affixes = affixes,
            )
        }

        private fun compatibilityLabel(id: String): String =
            id.split('_', '-')
                .filter(String::isNotBlank)
                .joinToString(" ") { segment ->
                    segment.lowercase().replaceFirstChar { char ->
                        if (char.isLowerCase()) {
                            char.titlecase()
                        } else {
                            char.toString()
                        }
                    }
                }

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
