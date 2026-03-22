package com.ktome.game

import com.ktome.core.ai.AIAction
import com.ktome.core.ai.AIActorSnapshot
import com.ktome.core.ai.AIDecision
import com.ktome.core.ai.AIDecisionContext
import com.ktome.core.ai.AITargetSnapshot
import com.ktome.core.combat.CombatResolver
import com.ktome.core.combat.DamageType
import com.ktome.core.dungeon.DungeonManager
import com.ktome.core.dungeon.FloorState
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.AiTriggerTracker
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
import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.ResistanceProfile
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.event.DamageDealtEvent
import com.ktome.core.event.EntityDeathEvent
import com.ktome.core.event.ExperienceGainedEvent
import com.ktome.core.event.LevelUpEvent
import com.ktome.core.event.MissEvent
import com.ktome.core.fov.Shadowcasting
import com.ktome.core.item.AffixType
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.Equipment
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.EquippedPassiveSource
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemGenerator
import com.ktome.core.item.Inventory
import com.ktome.core.item.InventoryManager
import com.ktome.core.item.InventoryOperationCode
import com.ktome.core.item.InventoryOperationResult
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemQuality
import com.ktome.core.item.ItemType
import com.ktome.core.item.MaterialDef
import com.ktome.core.item.PassiveEffectResolver
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.movement.MovementRules
import com.ktome.core.progression.ExperienceSystem
import com.ktome.core.random.RandomSource
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.core.random.StatefulRandomSource
import com.ktome.core.resource.ResourceType
import com.ktome.core.resource.StaminaPools
import com.ktome.core.run.RunOutcome
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.EquipmentSlotSnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.ItemStatModifierSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.StatusEffectType
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.talent.TalentFailureCode
import com.ktome.core.talent.DamageMultiplierResolver
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.talent.TalentResolver
import com.ktome.core.talent.TalentUseResult
import com.ktome.core.turn.TurnActorState
import com.ktome.core.turn.TurnScheduler
import com.ktome.game.data.schema.ItemBundleSchemaV2
import com.ktome.game.data.schema.AITriggerConditionKindSchemaV2
import com.ktome.game.data.schema.AITriggerSchemaV2
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.AIProfileSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.TalentSchemaV2
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.i18n.Localizer
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate
import java.nio.file.Files
import kotlin.math.abs

internal data class ZoneRuntimeBundle(
    val config: FoundationGameConfig,
    val dungeonManager: DungeonManager<FloorRuntimeState>,
    val initialMessages: List<RenderLogEventSnapshot>,
)

private const val SUMMARY_EVENT_LIMIT: Int = 5

class FoundationGameSession internal constructor(
    config: FoundationGameConfig,
    private val content: GameContent,
    private val saveManager: SaveManager,
    dungeonManager: DungeonManager<FloorRuntimeState>,
    private var playerSnapshot: PlayerSnapshot,
    initialMessageLog: List<RenderLogEventSnapshot> = emptyList(),
    private var turnCount: Int = 0,
    private val inventoryManager: InventoryManager = InventoryManager(),
    private val combatRandomSource: RandomSource = defaultCombatRandomSource(config, turnCount),
    private val combatResolver: CombatResolver = CombatResolver(combatRandomSource),
    private val talentRegistry: TalentRegistry = content.talentRegistry,
    private val talentResolver: TalentResolver = TalentResolver(talentRegistry, combatResolver),
    private val sessionRandom: RandomSource = defaultSessionRandomSource(config, turnCount),
    private val restoredPendingActionIds: List<Int> = emptyList(),
    private val restoredActiveTurnActorId: Int? = null,
    private val zoneRuntimeFactory: (FoundationGameConfig) -> ZoneRuntimeBundle = { unsupportedZoneConfig ->
        error("Zone transition is not supported for config $unsupportedZoneConfig.")
    },
) {
    private data class LevelUpFeedbackSnapshot(
        val stats: Stats,
        val maxHp: Int,
        val resourceTypeId: String,
        val resourceMax: Int,
    )

    var config: FoundationGameConfig = config
        private set
    private val messageLog = ArrayDeque<SessionLogEntry>()
    private val recentEvents = ArrayDeque<String>()
    private val recentSummaryEvents = ArrayDeque<RenderTextTokenSnapshot>()
    private val pendingActions = ArrayDeque<EntityId>()
    private var activeTurnActor: EntityId? = null
    private var runOutcome: RunOutcome = RunOutcome.InProgress
    private var dungeonManager: DungeonManager<FloorRuntimeState> = dungeonManager
    private var activeFloorState: FloorRuntimeState = dungeonManager.currentState().payload
    private var world: World = SessionSnapshotMapper.restoreWorld(content, playerSnapshot, activeFloorState)
    private var visibleTiles: Set<Point> = emptySet()
    private var exploredTiles: LinkedHashSet<Point> = activeFloorState.exploredTiles
    private var renderSnapshotRevision: Long = 1L
    private var cachedRenderSnapshot: RenderSnapshot? = null
    private var lastPlayerCombatTurn: Int = -1
    private val objectiveProgressTokens = linkedSetOf<String>()
    private var checkpointRequested: Boolean = false
    private var terminalKillerNameKey: String? = null
    private var terminalKillerTemplateId: String? = null
    private val playerBaseResistanceValues: Map<DamageType, Int> =
        world.get<ResistanceProfile>(playerId)?.values?.toMap(linkedMapOf()) ?: emptyMap()

    init {
        talentResolver.damageMultiplierResolver =
            DamageMultiplierResolver { _, attacker, target, damageType, baseMultiplier ->
                resolveDamageMultiplier(
                    attacker = attacker,
                    target = target,
                    damageType = damageType,
                    baseMultiplier = baseMultiplier,
                )
            }
        initialMessageLog.forEach(::addMessage)
        restorePendingTurnState()
        syncUnlockedPlayerTalents()
        ensurePlayerResourcePools()
        syncPlayerResistanceProfile()
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
        initialMessageLog = listOf(RenderLogEventSnapshot(RenderTextTokenSnapshot("log.session.enter_dungeon"))),
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

    fun renderSnapshot(): RenderSnapshot =
        cachedRenderSnapshot ?: buildRenderSnapshot().also { snapshot ->
            cachedRenderSnapshot = snapshot
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

    fun messageLog(): List<String> = messageLog.map(SessionLogEntry::text)

    fun currentTurnCount(): Int = turnCount

    fun recentEventLog(limit: Int = 20): List<String> = recentEvents.takeLast(limit)

    fun isGameOver(): Boolean = runOutcome is RunOutcome.Defeat

    fun isVictory(): Boolean = runOutcome is RunOutcome.Victory

    fun runOutcome(): RunOutcome = runOutcome

    fun runSummary(): RunSummary? =
        if (!runOutcome.isTerminal) {
            null
        } else {
            val health = requireNotNull(world.get<Health>(playerId)) { "Missing Health for $playerId." }
            val resource = resolvePlayerResourceView()
            RunSummary(
                outcome = runOutcome,
                floorReached = currentFloor(),
                maxFloor = config.maxFloor,
                turns = turnCount,
                playerLevel = playerStatus().level,
                zoneNameKey = currentZoneSchema().nameKey,
                outcomeReasonKey = runOutcomeReasonKey(runOutcome),
                killerNameKey = terminalKillerNameKey,
                killerTemplateId = terminalKillerTemplateId,
                finalHpCurrent = health.current.coerceAtLeast(0),
                finalHpMax = health.max,
                finalResourceTypeId = resource.typeId,
                finalResourceLabelKey = resourceLabelKey(resource.typeId),
                finalResourceCurrent = resource.current,
                finalResourceMax = resource.max,
                lastEvents = recentSummaryEvents.toList(),
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

    internal fun automationInteractableTags(interactableId: String): Set<String> =
        interactableSchemaFor(interactableId)?.interactionTags?.toSet().orEmpty()

    internal fun automationForceDefeatPlayer() {
        handleDeath(playerId, null)
    }

    fun playerStatus(): PlayerStatus {
        val health = requireNotNull(world.get<Health>(playerId))
        val experience = requireNotNull(world.get<Experience>(playerId))
        val derivedStats = requireNotNull(world.get<DerivedStats>(playerId))
        return PlayerStatus(
            currentHp = health.current,
            maxHp = health.max,
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

    fun playerResourceView(): PlayerResourceView = resolvePlayerResourceView()

    fun inventoryItems(): List<InventoryItemView> {
        val inventory = world.get<Inventory>(playerId) ?: return emptyList()
        return inventory.itemIds.mapIndexedNotNull { index, itemId ->
            val item = world.get<ItemInstance>(itemId) ?: return@mapIndexedNotNull null
            InventoryItemView(
                index = index,
                name = item.name,
                type = item.type,
                slot = item.slot,
                equippedSlot = inventoryManager.equippedSlotOf(world, playerId, itemId),
                effect = item.effect,
                resourceTypeId = item.resourceTypeId,
                magnitude = item.magnitude,
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
            val schema = talentSchemaFor(talentId)
            val level = loadout.levelOf(talentId).coerceIn(1, definition.maxLevel)
            val schemaResource = schema?.resourceCosts?.entries?.firstOrNull()
            val definitionResource = definition.resolvedResourceCosts().entries.firstOrNull()
            val resourceTypeId = schemaResource?.key ?: definitionResource?.key?.name ?: currentProfessionSchema()?.resourceType ?: ResourceType.STAMINA.name
            val resourceCost = schemaResource?.value ?: definitionResource?.value ?: 0
            val effectiveRange = definition.range + (definition.levelEffects[level]?.rangeBonus ?: 0)
            TalentSlotView(
                slot = slot,
                talentId = talentId,
                name = definition.name,
                level = level,
                maxLevel = definition.maxLevel,
                resourceCost = resourceCost,
                resourceTypeId = resourceTypeId,
                range = effectiveRange,
                minRange = definition.minRange,
                currentCooldown = cooldowns[talentId] ?: 0,
                maxCooldown = definition.cooldown,
                requiresTarget = effectiveRange > 0,
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

    private fun buildRenderSnapshot(): RenderSnapshot {
        ensurePlayerResourcePools()
        val zone = currentZoneSchema()
        val overlays = buildOverlaySnapshots()
        return RenderSnapshot(
            metadata =
                RenderMetadataSnapshot(
                    revision = renderSnapshotRevision,
                    zoneId = zone.id,
                    zoneNameKey = zone.nameKey,
                    currentFloor = currentFloor(),
                    maxFloor = maxFloor(),
                    width = map.width,
                    height = map.height,
                    playerX = playerPosition().x,
                    playerY = playerPosition().y,
                    zoneVisualKey = zone.visualKey,
                    zoneAudioProfile = zone.audioProfile,
                    tilesetKey = zone.tilesetKey,
                    ambientProfile = zone.ambientProfile,
                ),
            mapCells = buildMapCells(zone),
            props = buildPropSnapshots(),
            actors = buildActorSnapshots(),
            overlays = overlays,
            uiState = buildRenderUiState(),
            logEvents = buildVisibleLogEvents(overlays),
        )
    }

    private fun buildVisibleLogEvents(overlays: List<OverlayRenderSnapshot>): List<RenderLogEventSnapshot> =
        messageLog.map(SessionLogEntry::snapshot) +
            overlays.mapNotNull { overlay -> overlay.warningMessage?.let(::RenderLogEventSnapshot) }

    private fun buildMapCells(zone: ZoneSchemaV2): List<MapCellSnapshot> =
        buildList(capacity = map.width * map.height) {
            for (y in 0 until map.height) {
                for (x in 0 until map.width) {
                    val point = Point(x, y)
                    val visibility = cellVisibility(point)
                    val cell =
                        when (visibility) {
                            CellVisibilitySnapshot.HIDDEN ->
                                MapCellSnapshot(
                                    x = x,
                                    y = y,
                                    visibility = visibility,
                                    terrainTypeId = "hidden",
                                    terrainVisualKey = "tile.hidden",
                                )

                            else -> {
                                val tile = map[point]
                                MapCellSnapshot(
                                    x = x,
                                    y = y,
                                    visibility = visibility,
                                    terrainTypeId = terrainTypeId(tile),
                                    terrainVisualKey = terrainVisualKey(zone, tile),
                                    stairDirectionId = stairDirectionAt(point)?.name,
                                    actorEntityId =
                                        if (visibility == CellVisibilitySnapshot.VISIBLE) {
                                            actorAt(point)?.value
                                        } else {
                                            null
                                        },
                                    items =
                                        if (visibility == CellVisibilitySnapshot.VISIBLE) {
                                            itemsOnGroundAt(point).mapNotNull(::toItemRenderSnapshot)
                                        } else {
                                            emptyList()
                                        },
                                )
                            }
                        }
                    add(cell)
                }
            }
        }

    private fun buildPropSnapshots(): List<PropRenderSnapshot> =
        (
            world.entitiesWith(Position::class, Stair::class)
                .mapNotNull { entityId ->
                    val position = requireNotNull(world.get<Position>(entityId)).toPoint()
                    if (cellVisibility(position) == CellVisibilitySnapshot.HIDDEN) {
                        return@mapNotNull null
                    }
                    val stair = requireNotNull(world.get<Stair>(entityId))
                    PropRenderSnapshot(
                        id = "stair:${stair.direction.name.lowercase()}:${entityId.value}",
                        x = position.x,
                        y = position.y,
                        propTypeId = "stairs",
                        stairDirectionId = stair.direction.name,
                        visualKey = stairVisualKey(stair.direction),
                        audioProfile = "audio.interactable.stairs",
                    )
                } +
                world.entitiesWith(Position::class, Interactable::class)
                    .mapNotNull { entityId ->
                        val position = requireNotNull(world.get<Position>(entityId)).toPoint()
                        if (cellVisibility(position) == CellVisibilitySnapshot.HIDDEN) {
                            return@mapNotNull null
                        }
                        val interactable = requireNotNull(world.get<Interactable>(entityId))
                        val schema = interactableSchemaFor(interactable.id) ?: return@mapNotNull null
                        PropRenderSnapshot(
                            id = "interactable:${schema.id}:${entityId.value}",
                            x = position.x,
                            y = position.y,
                            propTypeId = schema.id,
                            visualKey = schema.visualKey,
                            audioProfile = schema.audioProfile,
                        )
                    }
        ).sortedWith(compareBy<PropRenderSnapshot> { it.y }.thenBy { it.x }.thenBy(PropRenderSnapshot::id))

    private fun buildActorSnapshots(): List<ActorRenderSnapshot> =
        world.entitiesWith(Position::class, Health::class, Stats::class, DerivedStats::class, Name::class)
            .mapNotNull { entityId ->
                val position = requireNotNull(world.get<Position>(entityId)).toPoint()
                val health = requireNotNull(world.get<Health>(entityId))
                if (health.current <= 0) {
                    return@mapNotNull null
                }
                if (entityId != playerId && position !in visibleTiles) {
                    return@mapNotNull null
                }

                val stats = requireNotNull(world.get<Stats>(entityId))
                val derived = requireNotNull(world.get<DerivedStats>(entityId))
                val behavior = world.get<AIBehavior>(entityId)
                ActorRenderSnapshot(
                    entityId = entityId.value,
                    x = position.x,
                    y = position.y,
                    visualKey = entityVisualKey(entityId),
                    audioProfile = entityAudioProfile(entityId),
                    nameKey = entityNameKey(entityId),
                    isPlayer = entityId == playerId,
                    roleKind = actorRoleKind(entityId, behavior),
                    aiTypeId = behavior?.type?.name,
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
                    statusEffects = activeStatusEffectSnapshots(entityId),
                )
            }.sortedWith(compareBy<ActorRenderSnapshot> { it.y }.thenBy { it.x }.thenBy(ActorRenderSnapshot::entityId))

    private fun buildOverlaySnapshots(): List<OverlayRenderSnapshot> {
        val bossSchemaByTemplateId = content.schemaCatalog.bossEncounters.associateBy { schema -> schema.bossTemplateId }
        val talentSchemaById = content.schemaCatalog.talents.associateBy { schema -> schema.id }
        return world.entitiesWith(Position::class, Health::class, MonsterTemplateId::class)
            .flatMap { entityId ->
                val health = requireNotNull(world.get<Health>(entityId))
                if (health.current <= 0) {
                    return@flatMap emptyList()
                }
                val templateId = requireNotNull(world.get<MonsterTemplateId>(entityId)).value
                if (templateId !in content.bossTemplateIds()) {
                    return@flatMap emptyList()
                }
                val position = requireNotNull(world.get<Position>(entityId)).toPoint()
                if (position !in visibleTiles) {
                    return@flatMap emptyList()
                }
                val bossSchema = bossSchemaByTemplateId[templateId]
                val behavior = world.get<AIBehavior>(entityId)
                val targetVisible =
                    behavior != null &&
                        playerPosition() in Shadowcasting.computeVisible(map = map, origin = position, radius = behavior.sightRadius)
                buildList {
                    if (targetVisible) {
                        add(
                            OverlayRenderSnapshot(
                                id = "boss-warning:${entityId.value}",
                                visualKey = "vfx.boss.warning.sigil_01",
                                audioProfile = "audio.boss.warning",
                                previewTurns = 1,
                                dangerLevel = 3,
                                shape = OverlayShapeSnapshot.SINGLE_TILE,
                                sourceAbilityId = bossSchema?.id ?: "boss.warning.presence",
                                cells = listOf(GridPointSnapshot(position.x, position.y)),
                                warningMessage =
                                    RenderTextTokenSnapshot(
                                        "log.warning.boss_presence",
                                        listOf(entityArg("boss", entityId)),
                                    ),
                            ),
                        )
                    }

                    nextMonsterTalentIntent(
                        monsterId = entityId,
                        talentSchemaById = talentSchemaById,
                        targetVisible = targetVisible,
                    )?.let { intent ->
                        telegraphOverlayFor(entityId, position, intent)?.let(::add)
                    }
                }
            }.sortedBy(OverlayRenderSnapshot::id)
    }

    private data class MonsterTalentIntent(
        val talentId: String,
        val talentSchema: TalentSchemaV2,
        val target: Point?,
        val triggerId: String? = null,
        val consumesOnce: Boolean = false,
        val postMessage: RenderTextTokenSnapshot? = null,
    )

    private fun nextMonsterTalentIntent(
        monsterId: EntityId,
        talentSchemaById: Map<String, TalentSchemaV2>,
        targetVisible: Boolean,
    ): MonsterTalentIntent? {
        val loadout = world.get<TalentLoadout>(monsterId) ?: return null
        val targetPosition = playerPosition()
        triggeredMonsterTalentIntent(
            monsterId = monsterId,
            loadout = loadout,
            talentSchemaById = talentSchemaById,
            targetVisible = targetVisible,
            targetPosition = targetPosition,
        )?.let { return it }
        val prioritizedTalentIds =
            prioritizedMonsterTalentIds(
                loadout = loadout,
                aiProfile = aiProfileFor(monsterId),
            )

        return prioritizedTalentIds.firstNotNullOfOrNull { talentId ->
            if (shouldSkipMonsterTalent(monsterId = monsterId, talentId = talentId)) {
                return@firstNotNullOfOrNull null
            }
            val definition = talentRegistry.get(talentId) ?: return@firstNotNullOfOrNull null
            val target = if (definition.range > 0) targetPosition else null
            if (talentResolver.canUse(world, map, monsterId, talentId, target) != null) {
                return@firstNotNullOfOrNull null
            }
            val talentSchema = talentSchemaById[talentId] ?: return@firstNotNullOfOrNull null
            MonsterTalentIntent(
                talentId = talentId,
                talentSchema = talentSchema,
                target = target,
            )
        }
    }

    private fun triggeredMonsterTalentIntent(
        monsterId: EntityId,
        loadout: TalentLoadout,
        talentSchemaById: Map<String, TalentSchemaV2>,
        targetVisible: Boolean,
        targetPosition: Point,
    ): MonsterTalentIntent? {
        val aiProfile = aiProfileFor(monsterId) ?: return null
        val tracker = world.get<AiTriggerTracker>(monsterId)
        return aiProfile.triggers.firstNotNullOfOrNull { trigger ->
            if (trigger.once && tracker?.consumedTriggerIds?.contains(trigger.triggerId) == true) {
                return@firstNotNullOfOrNull null
            }
            if (!isMonsterTriggerConditionSatisfied(trigger, monsterId, targetVisible)) {
                return@firstNotNullOfOrNull null
            }
            if (trigger.talentId !in loadout.talentLevels) {
                return@firstNotNullOfOrNull null
            }
            if (shouldSkipMonsterTalent(monsterId = monsterId, talentId = trigger.talentId)) {
                return@firstNotNullOfOrNull null
            }
            val definition = talentRegistry.get(trigger.talentId) ?: return@firstNotNullOfOrNull null
            val target = if (definition.range > 0) targetPosition else null
            if (talentResolver.canUse(world, map, monsterId, trigger.talentId, target) != null) {
                return@firstNotNullOfOrNull null
            }
            val talentSchema = talentSchemaById[trigger.talentId] ?: return@firstNotNullOfOrNull null
            MonsterTalentIntent(
                talentId = trigger.talentId,
                talentSchema = talentSchema,
                target = target,
                triggerId = trigger.triggerId,
                consumesOnce = trigger.once,
                postMessage = triggerPostMessageToken(trigger),
            )
        }
    }

    private fun isMonsterTriggerConditionSatisfied(
        trigger: AITriggerSchemaV2,
        monsterId: EntityId,
        targetVisible: Boolean,
    ): Boolean =
        when (trigger.condition) {
            AITriggerConditionKindSchemaV2.ON_COMBAT_START -> {
                val tracker = world.get<AiTriggerTracker>(monsterId) ?: return false
                targetVisible &&
                    (
                        trigger.triggerId in tracker.pendingCombatStartTriggerIds ||
                            !tracker.engagedInCombat
                    )
            }
            AITriggerConditionKindSchemaV2.HP_BELOW_RATIO -> {
                val health = world.get<Health>(monsterId) ?: return false
                val threshold = trigger.threshold ?: return false
                health.max > 0 && health.current.toDouble() / health.max <= threshold
            }
        }

    private fun triggerPostMessageToken(trigger: AITriggerSchemaV2): RenderTextTokenSnapshot? =
        trigger.postMessageKey?.let { key ->
            RenderTextTokenSnapshot(
                key = key,
                arguments =
                    trigger.postMessageArgs.entries
                        .sortedBy { (name, _) -> name }
                        .map { (name, valueKey) -> keyArg(name, valueKey) },
            )
        }

    private fun prioritizedMonsterTalentIds(
        loadout: TalentLoadout,
        aiProfile: AIProfileSchemaV2?,
    ): List<String> {
        val configured =
            aiProfile
                ?.talentPriority
                ?.mapNotNull { configuredTalentId ->
                    loadout.slotToTalentId.values.firstOrNull { talentId -> talentId == configuredTalentId }
                }
                .orEmpty()
        if (configured.isNotEmpty()) {
            return configured
        }
        return listOfNotNull(
            loadout.slotToTalentId.values.firstOrNull { it == "war_cry" },
            loadout.slotToTalentId.values.firstOrNull { it == "power_strike" },
            loadout.slotToTalentId.values.firstOrNull { it == "charge" },
            loadout.slotToTalentId.values.firstOrNull { it == "shield_bash" },
        )
    }

    private fun shouldSkipMonsterTalent(
        monsterId: EntityId,
        talentId: String,
    ): Boolean =
        aiProfileFor(monsterId)
            ?.skipRules
            ?.any { rule ->
                rule.talentId == talentId &&
                    world.get<EffectTracker>(monsterId)?.has(rule.selfHasStatus) == true
            } == true

    private fun aiProfileFor(monsterId: EntityId): AIProfileSchemaV2? {
        val aiProfileId = monsterAiProfileId(monsterId) ?: return null
        return content.schemaCatalog.aiProfiles.firstOrNull { profile -> profile.id == aiProfileId }
    }

    private fun monsterAiProfileId(monsterId: EntityId): String? {
        val templateId = world.get<MonsterTemplateId>(monsterId)?.value ?: return null
        return content.allMonsterTemplates()
            .firstOrNull { template -> template.id == templateId }
            ?.aiProfileId
    }

    private fun telegraphOverlayFor(
        entityId: EntityId,
        origin: Point,
        intent: MonsterTalentIntent,
    ): OverlayRenderSnapshot? {
        val cells = telegraphCells(origin, intent.target ?: origin, intent.talentSchema)
        if (cells.isEmpty()) {
            return null
        }
        return OverlayRenderSnapshot(
            id = "telegraph:${entityId.value}:${intent.talentId}",
            visualKey = "vfx.telegraph.warning.sigil_01",
            audioProfile = intent.talentSchema.audioProfile,
            previewTurns = 1,
            dangerLevel = if (intent.talentSchema.kind == "ACTIVE") 2 else 1,
            shape = telegraphShape(intent.talentSchema.telegraph),
            sourceAbilityId = intent.talentId,
            cells = cells.map { point -> GridPointSnapshot(point.x, point.y) },
            warningMessage =
                RenderTextTokenSnapshot(
                    "log.warning.telegraph",
                    listOf(
                        entityArg("boss", entityId),
                        talentArg("talent", intent.talentId, fallbackName = intent.talentId),
                    ),
                ),
        )
    }

    private fun telegraphShape(telegraph: String): OverlayShapeSnapshot =
        when (telegraph) {
            "charge_lane" -> OverlayShapeSnapshot.LINE
            "self_buff_aura" -> OverlayShapeSnapshot.RING
            else -> OverlayShapeSnapshot.SINGLE_TILE
        }

    private fun telegraphCells(
        origin: Point,
        target: Point,
        talentSchema: TalentSchemaV2,
    ): List<Point> =
        when (talentSchema.telegraph) {
            "charge_lane" -> lineTowards(origin, target, maxSteps = maxOf(1, talentSchema.range))
            "self_buff_aura" -> auraCells(origin, radius = maxOf(1, talentSchema.areaRadius))
            else -> listOf(projectSingleTarget(origin, target, maxSteps = maxOf(1, talentSchema.range)))
        }

    private fun projectSingleTarget(
        origin: Point,
        target: Point,
        maxSteps: Int,
    ): Point {
        val dx = (target.x - origin.x).coerceIn(-1, 1)
        val dy = (target.y - origin.y).coerceIn(-1, 1)
        if (dx == 0 && dy == 0) {
            return origin
        }
        val distance = minOf(origin.chebyshevDistanceTo(target), maxSteps)
        return Point(origin.x + dx * distance, origin.y + dy * distance)
    }

    private fun lineTowards(
        origin: Point,
        target: Point,
        maxSteps: Int,
    ): List<Point> {
        val dx = (target.x - origin.x).coerceIn(-1, 1)
        val dy = (target.y - origin.y).coerceIn(-1, 1)
        if (dx == 0 && dy == 0) {
            return listOf(origin)
        }
        val stepCount = minOf(maxSteps, maxOf(1, origin.chebyshevDistanceTo(target)))
        return (1..stepCount)
            .map { step -> Point(origin.x + dx * step, origin.y + dy * step) }
            .filter { point -> map.isInBounds(point.x, point.y) }
    }

    private fun auraCells(
        origin: Point,
        radius: Int,
    ): List<Point> =
        buildList {
            for (y in origin.y - radius..origin.y + radius) {
                for (x in origin.x - radius..origin.x + radius) {
                    if (!map.isInBounds(x, y)) {
                        continue
                    }
                    add(Point(x, y))
                }
            }
        }.distinct()

    private fun buildRenderUiState(): RenderUiStateSnapshot =
        RenderUiStateSnapshot(
            playerStatus = buildPlayerStatusSnapshot(),
            equipment = buildEquipmentSnapshots(),
            talents = buildTalentSnapshots(),
            inventory = buildInventoryEntries(),
            targetablePositions = targetableHostilePositions().map { point -> GridPointSnapshot(point.x, point.y) },
        )

    private fun buildPlayerStatusSnapshot(): PlayerStatusSnapshot {
        val status = playerStatus()
        val resource = resolvePlayerResourceView()
        return PlayerStatusSnapshot(
            currentHp = status.currentHp,
            maxHp = status.maxHp,
            currentResource = resource.current,
            maxResource = resource.max,
            resourceLabelKey = resourceLabelKey(resource.typeId),
            resourceTypeId = resource.typeId,
            level = status.level,
            currentExperience = status.currentExperience,
            nextLevelRequirement = status.nextLevelRequirement,
            statPoints = status.statPoints,
            talentPoints = status.talentPoints,
            attack = status.attack,
            defense = status.defense,
            accuracy = status.accuracy,
            evasion = status.evasion,
            speed = status.speed,
        )
    }

    private fun buildEquipmentSnapshots(): List<EquipmentSlotSnapshot> =
        EquipSlot.entries.map { slot ->
            val equippedItem = equippedItemFor(slot)
            EquipmentSlotSnapshot(
                slotId = slot.name,
                item = equippedItem?.let(::toItemRenderSnapshot),
            )
        }

    private fun equippedItemFor(slot: EquipSlot): ItemInstance? =
        world.get<Equipment>(playerId)
            ?.slots
            ?.get(slot)
            ?.let { entityId -> world.get<ItemInstance>(entityId) }

    private fun buildTalentSnapshots(): List<TalentSlotSnapshot> =
        talentSlots().map { slot ->
            val schema = requireNotNull(content.schemaCatalog.talents.firstOrNull { it.id == slot.talentId }) {
                "Unknown talent schema '${slot.talentId}'."
            }
            val resourceTypeId = schema.resourceCosts.keys.firstOrNull() ?: resolvePlayerResourceView().typeId
            TalentSlotSnapshot(
                slot = slot.slot,
                talentId = slot.talentId,
                nameKey = schema.nameKey,
                visualKey = schema.visualKey,
                iconKey = schema.iconKey,
                damageTypeIconKey = schema.damageType?.let(::damageTypeIconKey),
                audioProfile = schema.audioProfile,
                level = slot.level,
                maxLevel = slot.maxLevel,
                resourceCost = schema.resourceCosts[resourceTypeId] ?: slot.resourceCost,
                resourceLabelKey = resourceLabelKey(resourceTypeId),
                resourceTypeId = resourceTypeId,
                range = slot.range,
                minRange = slot.minRange,
                currentCooldown = slot.currentCooldown,
                maxCooldown = slot.maxCooldown,
                requiresTarget = slot.requiresTarget,
            )
        }

    private fun buildInventoryEntries(): List<InventoryEntrySnapshot> =
        inventoryItems().map { itemView ->
            val itemEntityId = world.get<Inventory>(playerId)?.itemIds?.getOrNull(itemView.index)
            val item =
                requireNotNull(itemEntityId?.let { entityId -> world.get<ItemInstance>(entityId) }) {
                    "Missing inventory item at index ${itemView.index}."
                }
            InventoryEntrySnapshot(
                index = itemView.index,
                item = toItemRenderSnapshot(item),
                equippedSlotId = itemView.equippedSlot?.name,
            )
        }

    private fun toItemRenderSnapshot(itemId: EntityId): ItemRenderSnapshot? =
        world.get<ItemInstance>(itemId)?.let(::toItemRenderSnapshot)

    private fun toItemRenderSnapshot(item: ItemInstance): ItemRenderSnapshot {
        val schema = requireNotNull(itemSchemaFor(item)) {
            "Unknown item schema '${item.baseId}'."
        }
        val materialNameKey = item.materialId?.let(::materialSchemaFor)?.nameKey
        val affixNameKeys = item.affixes.mapNotNull { affix -> affixSchemaFor(affix.id)?.nameKey }
        return ItemRenderSnapshot(
            baseItemId = item.baseId,
            nameKey = schema.nameKey,
            displayName = itemDisplayToken(item),
            descKey = schema.descKey,
            typeId = item.type.name,
            visualKey = schema.visualKey,
            iconKey = schema.iconKey,
            audioProfile = schema.audioProfile,
            slotId = item.slot?.name,
            qualityNameKey = qualityLabelKey(item.quality),
            materialNameKey = materialNameKey,
            affixNameKeys = affixNameKeys,
            passiveDescriptions = item.passive?.let(::passiveDescriptionToken)?.let(::listOf).orEmpty(),
            stats = item.stats.toSnapshot(),
            effectTypeId = item.effect?.name,
            magnitude = item.magnitude,
        )
    }

    private fun currentZoneSchema(): ZoneSchemaV2 =
        requireNotNull(content.schemaCatalog.zones.firstOrNull { zone -> zone.id == config.zoneId }) {
            "Unknown zone '${config.zoneId}'."
        }

    private fun currentProfessionSchema(): ProfessionSchemaV2? =
        content.schemaCatalog.professions.firstOrNull { profession -> profession.id == config.playerProfessionId }

    private fun currentObjectiveSetSchema() =
        content.schemaCatalog.objectiveSets.firstOrNull { objectiveSet -> objectiveSet.id == currentZoneSchema().objectiveSetId }

    private fun activeBossEncounterSchema() =
        activeBossDefinition()?.encounterId?.let { encounterId ->
            content.schemaCatalog.bossEncounters.firstOrNull { schema -> schema.id == encounterId }
        }

    private fun interactableSchemaFor(interactableId: String) =
        content.schemaCatalog.interactables.firstOrNull { interactable -> interactable.id == interactableId }

    private fun lootProfile(profileId: String) =
        content.schemaCatalog.lootProfiles.firstOrNull { profile -> profile.id == profileId }

    private fun resolvePlayerResourceView(): PlayerResourceView {
        val schema = currentProfessionSchema()
        val resourceTypeId = schema?.resourceType ?: ResourceType.STAMINA.name
        val pools =
            if (schema != null) {
                PlayerResourceService.sync(world, playerId, schema)
            } else {
                requireNotNull(world.get<com.ktome.core.resource.ResourcePools>(playerId)) {
                    "Missing ResourcePools for '$playerId'."
                }
            }
        val pool =
            requireNotNull(pools.pool(ResourceType.fromId(resourceTypeId))) {
                "Missing resource pool '$resourceTypeId' for '$playerId'."
            }
        return PlayerResourceView(
            current = pool.current,
            max = pool.max,
            typeId = resourceTypeId,
        )
    }

    private fun resourceLabelKey(resourceTypeId: String): String =
        when (resourceTypeId) {
            "MANA" -> "ui.hud.mana.short"
            "ENERGY" -> "ui.hud.energy.short"
            "POSITIVE_ENERGY" -> "ui.hud.positive_energy.short"
            else -> "ui.hud.stamina.short"
        }

    private fun damageTypeLabelKey(damageType: DamageType): String =
        when (damageType) {
            DamageType.PHYSICAL -> "damage_type.physical.name"
            DamageType.FIRE -> "damage_type.fire.name"
            DamageType.COLD -> "damage_type.cold.name"
            DamageType.LIGHTNING -> "damage_type.lightning.name"
            DamageType.HOLY -> "damage_type.holy.name"
            DamageType.SHADOW -> "damage_type.shadow.name"
        }

    private fun damageTypeIconKey(damageTypeId: String): String =
        when (damageTypeId) {
            "PHYSICAL" -> "icon.damage_type.physical"
            "FIRE" -> "icon.damage_type.fire"
            "COLD" -> "icon.damage_type.cold"
            "LIGHTNING" -> "icon.damage_type.lightning"
            "HOLY" -> "icon.damage_type.holy"
            "SHADOW" -> "icon.damage_type.shadow"
            else -> "icon.damage_type.physical"
        }

    private fun entityVisualKey(entityId: EntityId): String =
        when {
            entityId == playerId -> currentProfessionSchema()?.visualKey ?: "actor.player"
            else -> {
                val templateId = world.get<MonsterTemplateId>(entityId)?.value ?: return "actor.unknown"
                content.allMonsterTemplates().firstOrNull { template -> template.id == templateId }?.visualKey ?: "actor.unknown"
            }
        }

    private fun entityAudioProfile(entityId: EntityId): String? =
        when {
            entityId == playerId -> currentProfessionSchema()?.audioProfile
            else -> {
                val templateId = world.get<MonsterTemplateId>(entityId)?.value ?: return null
                content.allMonsterTemplates().firstOrNull { template -> template.id == templateId }?.audioProfile
            }
        }

    private fun entityNameKey(entityId: EntityId): String =
        entityNameKeyOrNull(entityId) ?: "actor.unknown.name"

    private fun entityNameKeyOrNull(entityId: EntityId): String? =
        when {
            entityId == playerId -> "actor.player.name"
            else -> {
                val templateId = world.get<MonsterTemplateId>(entityId)?.value ?: return null
                content.schemaCatalog.monsters.firstOrNull { schema -> schema.id == templateId }?.nameKey
            }
        }

    private fun itemSchemaFor(item: ItemInstance) =
        content.schemaCatalog.itemBundle.items.firstOrNull { schema -> schema.id == item.baseId }

    private fun itemSchemaFor(baseItemId: String) =
        content.schemaCatalog.itemBundle.items.firstOrNull { schema -> schema.id == baseItemId }

    private fun materialSchemaFor(materialId: String) =
        content.schemaCatalog.itemBundle.materials.firstOrNull { schema -> schema.id == materialId }

    private fun affixSchemaFor(affixId: String) =
        content.schemaCatalog.itemBundle.affixes.firstOrNull { schema -> schema.id == affixId }

    private fun itemBaseDef(baseItemId: String): ItemBaseDef? =
        content.itemBundle.baseItems.firstOrNull { item -> item.id == baseItemId }

    private fun officialRewardItem(
        baseId: String,
        fallbackBaseId: String,
    ): ItemInstance {
        val base =
            itemBaseDef(baseId)
                ?: requireNotNull(itemBaseDef(fallbackBaseId)) {
                    "Missing fallback reward item '$fallbackBaseId'."
        }
        return base.toRuntimeItem()
    }

    private fun rewardItemFromProfiles(
        profileIds: List<String>,
        fallbackBaseId: String,
    ): ItemInstance {
        val candidateIds =
            profileIds
                .flatMap { profileId -> lootProfile(profileId)?.itemIds.orEmpty() }
                .distinct()
        val freshCandidateIds = candidateIds.filterNot(currentOwnedItemBaseIds()::contains)
        val selectedBaseId =
            rewardPreferenceOrder().firstOrNull { itemId ->
                itemId in freshCandidateIds && isRewardSuitableForCurrentProfession(itemId)
            }
                ?: fallbackBaseId.takeIf(::isRewardSuitableForCurrentProfession)
                ?: rewardPreferenceOrder().firstOrNull { itemId ->
                    itemId in candidateIds && isRewardSuitableForCurrentProfession(itemId)
                }
                ?: freshCandidateIds.firstOrNull { itemId -> isRewardSuitableForCurrentProfession(itemId) }
                ?: freshCandidateIds.firstOrNull()
                ?: candidateIds.firstOrNull()
                ?: fallbackBaseId
        return officialRewardItem(baseId = selectedBaseId, fallbackBaseId = fallbackBaseId)
    }

    private fun currentOwnedItemBaseIds(): Set<String> {
        val inventory = world.get<Inventory>(playerId) ?: return emptySet()
        return inventory.itemIds
            .mapNotNull { itemId -> world.get<ItemInstance>(itemId)?.baseId }
            .toSet()
    }

    private fun isRewardSuitableForCurrentProfession(baseItemId: String): Boolean {
        val profession = currentProfessionSchema() ?: return true
        val base = itemBaseDef(baseItemId) ?: return false
        if (base.resourceTypeId != null && base.resourceTypeId != profession.resourceType) {
            return false
        }
        val itemSchema = itemSchemaFor(baseItemId)
        if (profession.resourceType != ResourceType.MANA.name && itemSchema?.tags?.contains("arcane") == true) {
            return false
        }
        return true
    }

    private fun rewardPreferenceOrder(): List<String> =
        when (config.playerProfessionId) {
            "vanguard" -> listOf("forgebreaker_pick", "basic_shield", "chain_mail", "war_maul", "healing_potion", "scroll_teleport")
            "arcanist" -> listOf("seal_reliquary", "emerald_charm", "mana_potion", "apprentice_robe", "scroll_teleport", "healing_potion")
            "rogue" -> listOf("bandit_trophy", "hunter_bow", "leather_armor", "energy_tonic", "scroll_teleport", "healing_potion")
            "templar" -> listOf("sanctified_seal", "long_sword", "basic_shield", "chain_mail", "consecrated_oil", "healing_potion")
            else -> listOf("healing_potion", "scroll_teleport")
        }

    private fun grantRewardItem(
        reward: ItemInstance,
        dropPoint: Point,
    ): Boolean {
        val inventory = requireNotNull(world.get<Inventory>(playerId)) { "Missing Inventory for $playerId" }
        if (inventory.itemIds.size < inventory.capacity) {
            inventory.itemIds += ItemFactory().createCarriedItem(world, reward)
            return true
        }
        ItemFactory().createGroundItem(world, reward, dropPoint)
        return false
    }

    private fun supportRewardItem(): ItemInstance =
        rewardItemFromProfiles(
            profileIds = listOf("loot.foundation.elite"),
            fallbackBaseId = "healing_potion",
        )

    private fun zoneRewardItem(
        profileIds: List<String>,
        fallbackBaseId: String,
    ): ItemInstance = rewardItemFromProfiles(profileIds = profileIds, fallbackBaseId = fallbackBaseId)

    private fun activeBossRewardItem(): ItemInstance =
        rewardItemFromProfiles(
            profileIds = activeBossEncounterSchema()?.rewards.orEmpty(),
            fallbackBaseId = "scroll_teleport",
        )

    private fun alertCurrentFloorHostiles(): Int {
        val playerFaction = requireNotNull(world.get<FactionTag>(playerId)).value
        var alerted = 0
        world.entitiesWith(FactionTag::class, AIBehavior::class)
            .filter { entityId -> entityId != playerId && requireNotNull(world.get<FactionTag>(entityId)).value != playerFaction }
            .forEach { entityId ->
                val behavior = requireNotNull(world.get<AIBehavior>(entityId))
                val alertedBehavior =
                    behavior.copy(
                        type = if (behavior.type == AIType.PATROL) AIType.CHASE else behavior.type,
                        sightRadius = maxOf(behavior.sightRadius, 12),
                    )
                if (alertedBehavior != behavior) {
                    world.add(entityId, alertedBehavior)
                    alerted += 1
                }
            }
        return alerted
    }

    private fun restoreArmorySupplies(): Int {
        val resource = resolvePlayerResourceView()
        val pool =
            requireNotNull(world.get<com.ktome.core.resource.ResourcePools>(playerId)?.pool(ResourceType.fromId(resource.typeId))) {
                "Missing resource pool '${resource.typeId}' for '$playerId'."
            }
        val restored =
            if (resource.typeId == ResourceType.STAMINA.name) {
                (pool.max / 3).coerceAtLeast(8)
            } else {
                (pool.max / 4).coerceAtLeast(10)
            }
        val before = pool.current
        pool.restore(restored)
        return pool.current - before
    }

    private fun recordObjectiveProgress(
        token: String,
        stepKey: String,
    ) {
        val objective = currentObjectiveSetSchema() ?: return
        if (!objectiveProgressTokens.add(token)) {
            return
        }
        addMessage(
            "log.objective.progress",
            keyArg("objective", objective.nameKey),
            keyArg("step", stepKey),
        )
    }

    private fun reinforcementTemplateForAlarm(): MonsterTemplate? {
        val preferredIds = listOf("bandit.archer", "bandit.raider", "goblin.scout")
        val allowedIds = currentZoneSchema().monsterPools + currentZoneSchema().elitePools
        return preferredIds
            .filter(allowedIds::contains)
            .asSequence()
            .mapNotNull { templateId -> content.monsterCatalog.firstOrNull { template -> template.id == templateId } }
            .firstOrNull()
    }

    private fun reinforcementSpawnPoint(origin: Point): Point? {
        val occupied = occupiedBlockingTiles(excluding = playerId)
        val preferredCenters =
            map.rooms
                .drop(1)
                .asReversed()
                .map { room -> room.center }
        val candidates =
            preferredCenters.asSequence()
                .plus(
                    map.floorPoints()
                        .sortedWith(
                            compareByDescending<Point> { point -> point.chebyshevDistanceTo(origin) }
                                .thenBy(Point::y)
                                .thenBy(Point::x),
                        )
                        .asSequence(),
                )
                .distinct()
        return candidates.firstOrNull { point ->
            map.isInBounds(point.x, point.y) &&
                !map[point].blocksMovement &&
                point !in occupied &&
                point != playerPosition()
        }
    }

    private fun spawnAlarmReinforcement(origin: Point): EntityId? {
        val template = reinforcementTemplateForAlarm() ?: return null
        val spawnPoint = reinforcementSpawnPoint(origin) ?: return null
        return EntityFactory().createMonster(world = world, template = template, position = spawnPoint)
    }

    private fun cellVisibility(point: Point): CellVisibilitySnapshot =
        when {
            point in visibleTiles -> CellVisibilitySnapshot.VISIBLE
            point in exploredTiles -> CellVisibilitySnapshot.EXPLORED
            else -> CellVisibilitySnapshot.HIDDEN
        }

    private fun terrainVisualKey(
        zone: ZoneSchemaV2,
        tile: com.ktome.core.map.TileType,
    ): String =
        when (tile) {
            com.ktome.core.map.TileType.FLOOR -> "${zone.tilesetKey}.ground_01"
            com.ktome.core.map.TileType.WALL -> "${zone.tilesetKey}.wall_01"
        }

    private fun terrainTypeId(tile: com.ktome.core.map.TileType): String =
        when (tile) {
            com.ktome.core.map.TileType.FLOOR -> "floor"
            com.ktome.core.map.TileType.WALL -> "wall"
        }

    private fun stairVisualKey(direction: StairDirection): String =
        when (direction) {
            StairDirection.UP -> "prop.stairs.up"
            StairDirection.DOWN -> "prop.stairs.down"
        }

    private fun actorRoleKind(
        entityId: EntityId,
        behavior: AIBehavior?,
    ): ActorRoleKindSnapshot =
        when {
            entityId == playerId -> ActorRoleKindSnapshot.PLAYER
            world.get<MonsterTemplateId>(entityId)?.value in content.bossTemplateIds() -> ActorRoleKindSnapshot.BOSS
            behavior != null -> ActorRoleKindSnapshot.MONSTER
            else -> ActorRoleKindSnapshot.GENERIC
        }

    private fun activeStatusEffectSnapshots(entityId: EntityId): List<StatusEffectRenderSnapshot> =
        world.get<EffectTracker>(entityId)
            ?.effects
            ?.filter { effect -> effect.remainingTurns > 0 }
            ?.map { effect ->
                StatusEffectRenderSnapshot(
                    typeId = effect.type.name,
                    remainingTurns = effect.remainingTurns,
                    nameKey = statusEffectNameKey(effect.type),
                    iconKey = statusEffectIconKey(effect.type),
                )
            }.orEmpty()

    private fun statusEffectIconKey(type: StatusEffectType): String =
        when (type) {
            StatusEffectType.STUNNED -> "icon.status.stunned"
            StatusEffectType.ARMOR_BREAK -> "icon.status.armor_break"
            StatusEffectType.WAR_CRY_BUFF -> "icon.status.war_cry_buff"
            StatusEffectType.WAR_CRY_DEBUFF -> "icon.status.war_cry_debuff"
            StatusEffectType.GUARD_STANCE_BUFF -> "icon.status.guard_stance_buff"
            StatusEffectType.ARCANE_SHIELD_BUFF -> "icon.status.arcane_shield_buff"
            StatusEffectType.UNYIELDING_BUFF -> "icon.status.unyielding_buff"
            StatusEffectType.MANA_SURGE_BUFF -> "icon.status.mana_surge_buff"
            StatusEffectType.STEALTH_BUFF -> "icon.skill.rogue.shadowstep"
            StatusEffectType.CURSED -> "icon.status.cursed"
            StatusEffectType.HOLY_SHIELD_BUFF -> "icon.skill.templar.divine_intervention"
            StatusEffectType.DEVOTION_BUFF -> "icon.status.consecration"
            StatusEffectType.HOLY_AURA_BUFF -> "icon.status.consecration"
        }

    private fun stairDirectionAt(point: Point): StairDirection? =
        world.entitiesWith(Position::class, Stair::class)
            .firstOrNull { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == point }
            ?.let { entityId -> requireNotNull(world.get<Stair>(entityId)).direction }

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
            stairDirectionId = stairDirectionAt(point)?.name,
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

        if (StaminaPools.hasPool(world, actorId)) {
            val regen = requireNotNull(world.get<DerivedStats>(actorId)).staminaRegen.toInt().coerceAtLeast(0)
            StaminaPools.restore(world, actorId, regen)
        }
        applyTurnStartPassives(actorId)
        if (actorId == playerId) {
            currentProfessionSchema()?.let { profession ->
                PlayerResourceService.onTurnStart(
                    world = world,
                    playerId = playerId,
                    profession = profession,
                    inCombat = turnCount <= lastPlayerCombatTurn,
                )
            }
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
        if (actorId == playerId) {
            ensurePlayerResourcePools()
        }
    }

    private fun executePlayerCommand(command: PlayerCommand): CommandResolution =
        when (command) {
            PlayerCommand.Wait -> {
                addMessage("log.wait")
                CommandResolution.accepted()
            }

            PlayerCommand.PickUp -> {
                val item = itemsOnGroundAt(playerPosition()).firstOrNull()
                if (item == null) {
                    addMessage("log.nothing_to_pick_up")
                    CommandResolution.rejected()
                } else {
                    val result = inventoryManager.pickUp(world, playerId, item)
                    addInventoryMessage(result)
                    CommandResolution(result.success, consumesTurn = result.success)
                }
            }

            PlayerCommand.Interact -> interactAtPlayerPosition()

            PlayerCommand.Ascend -> CommandResolution(transitionFloor(StairDirection.UP), consumesTurn = true, persistCheckpointAfterTurn = true)

            PlayerCommand.Descend -> CommandResolution(transitionFloor(StairDirection.DOWN), consumesTurn = true, persistCheckpointAfterTurn = true)

            PlayerCommand.SaveGame -> {
                val saved = persistRun()
                addMessage(if (saved) "log.save.success" else "log.save.failure")
                CommandResolution(accepted = true, consumesTurn = false)
            }

            is PlayerCommand.AssignStat -> {
                val experience = requireNotNull(world.get<Experience>(playerId))
                val stats = requireNotNull(world.get<Stats>(playerId))
                if (experience.unspentStatPoints <= 0) {
                    addMessage("log.stat.none")
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
                    ensurePlayerResourcePools()
                    addMessage("log.stat.invest", keyArg("stat", primaryStatLabelKey(command.stat)))
                    CommandResolution(accepted = true, consumesTurn = false)
                }
            }

            is PlayerCommand.AssignTalent -> {
                val experience = requireNotNull(world.get<Experience>(playerId))
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId))
                val talentId = loadout.talentIdAt(command.slot)
                if (experience.unspentTalentPoints <= 0) {
                    addMessage("log.talent.none")
                    CommandResolution.rejected()
                } else if (talentId == null) {
                    addMessage("log.talent.slot_empty", literalArg("slot", command.slot))
                    CommandResolution.rejected()
                } else {
                    val definition = requireNotNull(talentRegistry.get(talentId))
                    val currentLevel = loadout.levelOf(talentId)
                    if (currentLevel >= definition.maxLevel) {
                        addMessage("log.talent.max_level", keyArg("talent", talentNameKey(talentId)))
                        CommandResolution.rejected()
                    } else {
                        loadout.talentLevels[talentId] = currentLevel + 1
                        experience.unspentTalentPoints -= 1
                        addMessage(
                            "log.talent.advance",
                            keyArg("talent", talentNameKey(talentId)),
                            literalArg("level", currentLevel + 1),
                        )
                        CommandResolution(accepted = true, consumesTurn = false)
                    }
                }
            }

            is PlayerCommand.ActivateInventoryItem -> {
                val itemView = inventoryItems().getOrNull(command.index)
                if (itemView == null) {
                    addMessage("log.inventory.slot_empty")
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
                                ensurePlayerResourcePools()
                                syncPlayerResistanceProfile()
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
                    addMessage("log.move.single_step_only")
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
                            addMessage("log.move.cannot_move")
                            CommandResolution.rejected()
                        }
                    }
                }
            }

            is PlayerCommand.UseTalent -> {
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId))
                val talentId = loadout.talentIdAt(command.slot)
                if (talentId == null) {
                    addMessage("log.talent.slot_empty", literalArg("slot", command.slot))
                    CommandResolution.rejected()
                } else {
                    when (val result = talentResolver.resolve(world, map, playerId, talentId, command.target)) {
                        is TalentUseResult.Failure -> {
                            addMessage(talentFailureMessage(result))
                            CommandResolution.rejected()
                        }

                        is TalentUseResult.Success -> {
                            applyTalentResourceReactions(result.result)
                            logTalentResult(result.result)
                            logTriggeredTalentDamagePassives(result.result)
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
        val behavior = world.get<AIBehavior>(monsterId) ?: return
        val position = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val targetPosition = playerPosition()
        val targetVisible = targetPosition in Shadowcasting.computeVisible(map = map, origin = position, radius = behavior.sightRadius)
        val enteredCombatThisTurn = updateMonsterCombatState(monsterId, targetVisible)
        if (world.get<EffectTracker>(monsterId)?.has(StatusEffectType.STUNNED) == true) {
            expireCombatStartTriggers(monsterId, enteredCombatThisTurn)
            return
        }
        if (tryUseMonsterTalent(monsterId, targetVisible)) {
            expireCombatStartTriggers(monsterId, enteredCombatThisTurn)
            return
        }

        val patrolRoute = world.get<PatrolRoute>(monsterId)
        val decision = AIDecision.decide(
            AIDecisionContext(
                map = map,
                actor = AIActorSnapshot(monsterId, position, behavior, patrolRoute),
                target = AITargetSnapshot(playerId, targetPosition),
                occupiedTiles = occupiedBlockingTiles(excluding = monsterId),
                targetVisible = targetVisible,
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
        expireCombatStartTriggers(monsterId, enteredCombatThisTurn)
    }

    private fun updateMonsterCombatState(
        monsterId: EntityId,
        targetVisible: Boolean,
    ): Boolean {
        val tracker = world.get<AiTriggerTracker>(monsterId) ?: return false
        if (!targetVisible) {
            tracker.engagedInCombat = false
            tracker.consumedTriggerIds.clear()
            tracker.pendingCombatStartTriggerIds.clear()
            return false
        }
        if (!tracker.engagedInCombat) {
            tracker.engagedInCombat = true
            tracker.pendingCombatStartTriggerIds.clear()
            tracker.pendingCombatStartTriggerIds +=
                aiProfileFor(monsterId)
                    ?.triggers
                    ?.filter { trigger -> trigger.condition == AITriggerConditionKindSchemaV2.ON_COMBAT_START }
                    ?.map(AITriggerSchemaV2::triggerId)
                    .orEmpty()
            return true
        }
        return false
    }

    private fun expireCombatStartTriggers(
        monsterId: EntityId,
        enteredCombatThisTurn: Boolean,
    ) {
        if (!enteredCombatThisTurn) {
            return
        }
        world.get<AiTriggerTracker>(monsterId)?.pendingCombatStartTriggerIds?.clear()
    }

    private fun inspectActorView(entityId: EntityId): InspectActorView {
        val stats = requireNotNull(world.get<Stats>(entityId))
        val health = requireNotNull(world.get<Health>(entityId))
        val derived = requireNotNull(world.get<DerivedStats>(entityId))
        val behavior = world.get<AIBehavior>(entityId)
        val role =
            when {
                entityId == playerId -> tr("actor.player.role")
                world.get<MonsterTemplateId>(entityId)?.value in content.bossTemplateIds() -> tr("actor.boss.role")
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
            addAll(itemPresentationLines(item))
            addAll(statModifierLines(item.stats))
            when (item.effect) {
                com.ktome.core.item.ConsumableEffect.HEAL -> add(tr("ui.inspect.restore_hp", "amount" to item.magnitude))
                com.ktome.core.item.ConsumableEffect.TELEPORT -> add(tr("ui.inspect.teleport_random"))
                com.ktome.core.item.ConsumableEffect.RESTORE_RESOURCE -> {
                    val resourceLabel = tr(resourceLabelKey(item.resourceTypeId ?: ResourceType.STAMINA.name))
                    add(tr("ui.inspect.restore_resource", "amount" to item.magnitude, "resource" to resourceLabel))
                }
                null -> Unit
            }
            if (isEmpty()) {
                add(tr("ui.inspect.no_special_effect"))
            }
        }

    private fun itemPresentationLines(item: ItemInstance): List<String> =
        buildList {
            add(tr("ui.inspect.quality", "quality" to tr(qualityLabelKey(item.quality))))
            item.materialName?.let { materialName ->
                add(tr("ui.inspect.material", "material" to materialName))
            }
            item.affixes.forEach { affix ->
                add(tr("ui.inspect.affix", "affix" to affix.name))
            }
            item.passive?.let { passive ->
                add(render(passiveDescriptionToken(passive)))
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

    private fun tryUseMonsterTalent(
        monsterId: EntityId,
        targetVisible: Boolean,
    ): Boolean {
        val talentSchemaById = content.schemaCatalog.talents.associateBy(TalentSchemaV2::id)
        val intent =
            nextMonsterTalentIntent(
                monsterId = monsterId,
                talentSchemaById = talentSchemaById,
                targetVisible = targetVisible,
            ) ?: return false
        when (val result = talentResolver.resolve(world, map, monsterId, intent.talentId, intent.target)) {
            is TalentUseResult.Failure -> return false
            is TalentUseResult.Success -> {
                applyTalentResourceReactions(result.result)
                logTalentResult(result.result)
                logTriggeredTalentDamagePassives(result.result)
                consumeMonsterTrigger(monsterId, intent)
                intent.postMessage?.let { token -> addMessage(RenderLogEventSnapshot(token)) }
                handleTalentDeaths(result.result.targets, monsterId)
                return true
            }
        }
    }

    private fun consumeMonsterTrigger(
        monsterId: EntityId,
        intent: MonsterTalentIntent,
    ) {
        val triggerId = intent.triggerId ?: return
        val tracker = world.get<AiTriggerTracker>(monsterId) ?: return
        tracker.pendingCombatStartTriggerIds.remove(triggerId)
        if (!intent.consumesOnce) {
            return
        }
        tracker.consumedTriggerIds.add(triggerId)
    }

    private fun resolveAttack(
        attacker: EntityId,
        target: EntityId,
    ) {
        val targetHealth = requireNotNull(world.get<Health>(target))
        val damageAdjustment = resolveDamageAdjustment(attacker, target, DamageType.PHYSICAL)
        val result =
            combatResolver.resolveMelee(
                world = world,
                attacker = attacker,
                target = target,
                damageType = DamageType.PHYSICAL,
                damageMultiplier = damageAdjustment.multiplier,
            )

        if (!result.hit) {
            logEvent(MissEvent(attacker, target))
            addMessage(
                "log.attack.miss",
                entityArg("attacker", attacker),
                entityArg("target", target),
            )
            return
        }

        targetHealth.current = (targetHealth.current - result.finalDamage).coerceAtLeast(0)
        applyDamageResourceReactions(attacker, target, result.finalDamage)
        logEvent(DamageDealtEvent(attacker, target, result.finalDamage, result.critical))
        logTriggeredDamagePassives(attacker = attacker, sources = damageAdjustment.sources)
        addMessage(
            if (result.critical) {
                "log.attack.crit"
            } else {
                "log.attack.hit"
            },
            entityArg("attacker", attacker),
            entityArg("target", target),
            literalArg("damage", result.finalDamage),
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
        val deathTargetArg = entityArg("target", target)
        val killerSummary = killer?.let(::terminalKillerSummary)

        if (target == playerId) {
            runOutcome = RunOutcome.Defeat(currentFloor())
            terminalKillerNameKey = killerSummary?.nameKey
            terminalKillerTemplateId = killerSummary?.templateId
            pendingActions.clear()
            activeTurnActor = null
            saveManager.deleteSave()
            addMessage("log.player.death")
            return
        }

        val activeBossTemplateId = activeBossDefinition()?.template?.id
        val isBoss =
            currentFloor() == config.maxFloor &&
                activeBossTemplateId != null &&
                world.get<MonsterTemplateId>(target)?.value == activeBossTemplateId

        addMessage("log.entity.death", deathTargetArg)
        val monsterDropArg = entityArg("monster", target)
        val reward = world.get<ExperienceReward>(target)?.value ?: 0
        val deathPoint = world.get<Position>(target)?.toPoint()
        val droppedLoot =
            if (!isBoss && deathPoint != null) {
                lootItemForMonsterDeath(target)
            } else {
                null
            }
        world.destroyEntity(target)
        if (killer == playerId && reward > 0) {
            gainExperience(reward)
        }
        if (deathPoint != null && droppedLoot != null) {
            ItemFactory().createGroundItem(world, droppedLoot, deathPoint)
            addMessage(
                "log.loot.monster_drop_quality",
                monsterDropArg,
                keyArg("quality", qualityLabelKey(droppedLoot.quality)),
                itemDisplayArgument("item", droppedLoot, includeQuality = false),
            )
        }
        if (isBoss) {
            deathPoint?.let { point ->
                val bossReward = activeBossRewardItem()
                val stored = grantRewardItem(bossReward, point)
                val rewardSchema = requireNotNull(itemSchemaFor(bossReward.baseId)) {
                    "Unknown boss reward item '${bossReward.baseId}'."
                }
                addMessage(
                    if (stored) {
                        "log.boss.reward.claimed"
                    } else {
                        "log.boss.reward.dropped"
                    },
                    keyArg("item", rewardSchema.nameKey),
                )
            }
            currentObjectiveSetSchema()?.let { objective ->
                addMessage("log.objective.complete", keyArg("objective", objective.nameKey))
            }
            if (advanceToNextZoneInRoute()) {
                addMessage("log.route.advance", keyArg("zone", currentZoneSchema().nameKey))
            } else {
                runOutcome = RunOutcome.Victory(currentFloor())
                pendingActions.clear()
                activeTurnActor = null
                saveManager.deleteSave()
                addMessage("log.victory", deathTargetArg)
            }
        }
    }

    private data class TerminalKillerSummary(
        val nameKey: String?,
        val templateId: String?,
    )

    private fun terminalKillerSummary(killer: EntityId): TerminalKillerSummary =
        TerminalKillerSummary(
            nameKey = entityNameKeyOrNull(killer),
            templateId = world.get<MonsterTemplateId>(killer)?.value,
        )

    private fun gainExperience(amount: Int) {
        val experience = requireNotNull(world.get<Experience>(playerId))
        val profession = currentProfessionSchema()
        val baseline = captureLevelUpFeedbackSnapshot()
        val result = ExperienceSystem.applyReward(experience = experience, reward = amount)
        var unlockedTalentIds = emptyList<String>()
        if (result.levelsGained > 0) {
            profession?.let { schema ->
                applyLevelGrowth(schema, result.levelsGained)
                unlockedTalentIds = syncUnlockedPlayerTalents()
            }
        }
        ensurePlayerResourcePools()
        applyExperienceRecovery(result)
        val updated = captureLevelUpFeedbackSnapshot()

        logEvent(ExperienceGainedEvent(playerId, amount))
        addMessage("log.xp.gain", literalArg("amount", amount))

        if (result.levelsGained > 0) {
            logEvent(
                LevelUpEvent(
                    entity = playerId,
                    newLevel = experience.level,
                    unspentStatPoints = experience.unspentStatPoints,
                    unspentTalentPoints = experience.unspentTalentPoints,
                ),
            )
            addMessage("log.level_up", literalArg("level", experience.level))
            logLevelUpGrowth(baseline, updated)
            unlockedTalentIds.forEach { talentId ->
                addMessage("log.talent.unlock", keyArg("talent", talentNameKey(talentId)))
            }
        }
    }

    private fun applyExperienceRecovery(result: com.ktome.core.progression.ExperienceGainResult) {
        if (result.shouldRestoreHealthToMax) {
            world.get<Health>(playerId)?.let { health ->
                health.current = health.max
            }
        }
        if (result.shouldRestorePrimaryResourceToMax) {
            val resource = resolvePlayerResourceView()
            val pool =
                requireNotNull(world.get<com.ktome.core.resource.ResourcePools>(playerId)?.pool(ResourceType.fromId(resource.typeId))) {
                    "Missing resource pool '${resource.typeId}' for '$playerId'."
                }
            pool.syncTo(nextCurrent = pool.max, nextMax = pool.max)
        }
    }

    private fun applyLevelGrowth(
        profession: ProfessionSchemaV2,
        levelsGained: Int,
    ) {
        if (levelsGained <= 0) {
            return
        }
        val stats = requireNotNull(world.get<Stats>(playerId)) { "Missing Stats for $playerId." }
        repeat(levelsGained) {
            stats.str += profession.statGrowth.str
            stats.dex += profession.statGrowth.dex
            stats.con += profession.statGrowth.con
            stats.wil += profession.statGrowth.wil
        }
        StatsCalculator.recalculateAndStore(world, playerId)
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
                    StairDirection.UP -> "log.stairs.missing_up"
                    StairDirection.DOWN -> "log.stairs.missing_down"
                },
            )
            return false
        }

        if (direction == StairDirection.DOWN && currentFloor() == config.maxFloor && activeBossDefinition() == null) {
            if (advanceToNextZoneInRoute()) {
                addMessage("log.route.advance", keyArg("zone", currentZoneSchema().nameKey))
                return true
            }
            runOutcome = RunOutcome.Victory(currentFloor())
            pendingActions.clear()
            activeTurnActor = null
            saveManager.deleteSave()
            addMessage("log.victory.escape")
            return true
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
                StairDirection.UP -> "log.stairs.ascend"
                StairDirection.DOWN -> "log.stairs.descend"
            },
            literalArg("floor", transition.toFloor),
        )
        if (direction == StairDirection.DOWN && currentZoneSchema().id == "shattered_outpost" && transition.toFloor == config.maxFloor) {
            recordObjectiveProgress(
                token = "shattered_outpost.inner_breach",
                stepKey = "objective.shattered_outpost_breach.step.inner_breach",
            )
            addMessage("log.objective.advance")
        }
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
        val shouldPersist = resolution.persistCheckpointAfterTurn || checkpointRequested
        checkpointRequested = false
        if (!shouldPersist || runOutcome.isTerminal) {
            return
        }

        if (persistRun()) {
            addMessage("log.checkpoint.saved")
        }
    }

    private fun advanceToNextZoneInRoute(): Boolean {
        val nextRouteIndex = config.routeIndex + 1
        if (nextRouteIndex !in config.zoneRoute.indices) {
            return false
        }

        syncActiveFloorState()
        val nextConfig =
            config.copy(
                floor = 1,
                zoneId = config.zoneRoute[nextRouteIndex],
                routeIndex = nextRouteIndex,
            )
        val nextRuntime = zoneRuntimeFactory(nextConfig)
        config = nextRuntime.config
        dungeonManager = nextRuntime.dungeonManager
        activeFloorState = dungeonManager.currentState().payload
        exploredTiles = activeFloorState.exploredTiles
        objectiveProgressTokens.clear()
        playerSnapshot =
            playerSnapshot.copy(
                entity =
                    playerSnapshot.entity.copy(
                        position = PointSnapshot.from(activeFloorState.map.playerStart),
                    ),
            )
        world = SessionSnapshotMapper.restoreWorld(content, playerSnapshot, activeFloorState)
        syncPlayerResistanceProfile()
        pendingActions.clear()
        activeTurnActor = null
        nextRuntime.initialMessages.forEach(::addMessage)
        checkpointRequested = true
        refreshFov()
        return true
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

    private fun ensurePlayerResourcePools() {
        currentProfessionSchema()?.let { profession ->
            PlayerResourceService.sync(world, playerId, profession)
        }
    }

    private fun syncUnlockedPlayerTalents(
        notify: Boolean = false,
    ): List<String> {
        val profession = currentProfessionSchema() ?: return emptyList()
        val experience = world.get<Experience>(playerId) ?: return emptyList()
        val loadout = world.get<TalentLoadout>(playerId) ?: return emptyList()
        val unlockedTalentIds = TalentProgression.unlockedTalentIds(content.schemaCatalog, profession, experience.level)
        val newlyUnlockedTalentIds = mutableListOf<String>()
        var nextSlot = (loadout.slotToTalentId.keys.maxOrNull() ?: 0) + 1
        unlockedTalentIds.forEach { talentId ->
            loadout.talentLevels.putIfAbsent(talentId, 1)
            if (talentId in loadout.slotToTalentId.values) {
                return@forEach
            }
            loadout.slotToTalentId[nextSlot] = talentId
            nextSlot += 1
            newlyUnlockedTalentIds += talentId
            if (notify) {
                addMessage("log.talent.unlock", keyArg("talent", talentNameKey(talentId)))
            }
        }
        return newlyUnlockedTalentIds
    }

    private fun syncActiveFloorState() {
        ensurePlayerResourcePools()
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

    private fun applyTurnStartPassives(actorId: EntityId) {
        val health = world.get<Health>(actorId) ?: return
        PassiveEffectResolver.equippedPassives(world, actorId).forEach { source ->
            when (val passive = source.passive) {
                is EquipmentPassive.HpRegenPerTurn -> {
                    val before = health.current
                    health.current = (health.current + passive.amount).coerceAtMost(health.max)
                    val restored = health.current - before
                    if (restored > 0 && actorId == playerId) {
                        addMessage(
                            "log.passive.hp_regen",
                            itemDisplayArgument("item", source.item),
                            literalArg("amount", restored),
                        )
                    }
                }

                is EquipmentPassive.DamageTypeBonus,
                is EquipmentPassive.DamageVsTag,
                is EquipmentPassive.ResistanceBonus,
                -> Unit
            }
        }
    }

    private fun syncPlayerResistanceProfile() {
        val combined = playerBaseResistanceValues.toMutableMap()
        PassiveEffectResolver.resistanceBonuses(PassiveEffectResolver.equippedPassives(world, playerId)).forEach { (type, amount) ->
            combined[type] = (combined[type] ?: 0) + amount
        }
        if (combined.isEmpty()) {
            world.remove<ResistanceProfile>(playerId)
        } else {
            world.add(
                playerId,
                ResistanceProfile(
                    values = combined.toMutableMap(),
                ),
            )
        }
    }

    private fun resolveDamageMultiplier(
        attacker: EntityId,
        target: EntityId,
        damageType: DamageType,
        baseMultiplier: Double,
    ): Double = baseMultiplier * resolveDamageAdjustment(attacker, target, damageType).multiplier

    private fun resolveDamageAdjustment(
        attacker: EntityId,
        target: EntityId,
        damageType: DamageType,
    ) = PassiveEffectResolver.resolveDamageAdjustment(
        passives = PassiveEffectResolver.equippedPassives(world, attacker),
        targetTags = targetTagsFor(target),
        damageType = damageType,
    )

    private fun targetTagsFor(target: EntityId): Set<String> {
        val templateId = world.get<MonsterTemplateId>(target)?.value ?: return emptySet()
        return content.allMonsterTemplates()
            .firstOrNull { template -> template.id == templateId }
            ?.tags
            ?.toSet()
            .orEmpty()
    }

    private fun logTriggeredDamagePassives(
        attacker: EntityId,
        sources: List<EquippedPassiveSource>,
    ) {
        if (attacker != playerId || sources.isEmpty()) {
            return
        }
        sources
            .distinctBy { source -> source.item.baseId to source.passive }
            .forEach { source ->
                val itemArgument = itemDisplayArgument("item", source.item)
                when (val passive = source.passive) {
                    is EquipmentPassive.DamageVsTag ->
                        addMessage(
                            "log.passive.damage_bonus_vs_tag",
                            itemArgument,
                            monsterTagArg("tag", passive.tag),
                            literalArg("amount", (passive.bonusPercent * 100).toInt()),
                        )

                    is EquipmentPassive.DamageTypeBonus ->
                        addMessage(
                            "log.passive.damage_bonus_type",
                            itemArgument,
                            keyArg("damageType", damageTypeLabelKey(passive.type)),
                            literalArg("amount", (passive.bonusPercent * 100).toInt()),
                        )

                    is EquipmentPassive.HpRegenPerTurn,
                    is EquipmentPassive.ResistanceBonus,
                    -> Unit
                }
            }
    }

    private fun logTriggeredTalentDamagePassives(result: com.ktome.core.talent.TalentResult) {
        if (result.user != playerId) {
            return
        }
        val triggeredSources =
            result.effects
                .filterIsInstance<com.ktome.core.talent.TalentEffectResult.Damage>()
                .flatMap { effect ->
                    resolveDamageAdjustment(
                        attacker = result.user,
                        target = effect.target,
                        damageType = effect.damageType,
                    ).sources
                }
        logTriggeredDamagePassives(attacker = result.user, sources = triggeredSources)
    }

    private fun applyDamageResourceReactions(
        attacker: EntityId,
        target: EntityId,
        damage: Int,
    ) {
        if (attacker == playerId || target == playerId) {
            lastPlayerCombatTurn = turnCount
        }
        currentProfessionSchema()?.let { profession ->
            if (attacker == playerId) {
                PlayerResourceService.onSuccessfulHit(world, playerId, profession)
            }
            if (target == playerId && damage > 0) {
                PlayerResourceService.onDamageTaken(world, playerId, profession, damage)
            }
        }
    }

    private fun applyTalentResourceReactions(result: com.ktome.core.talent.TalentResult) {
        result.effects.forEach { effect ->
            if (effect is com.ktome.core.talent.TalentEffectResult.Damage && effect.amount > 0) {
                applyDamageResourceReactions(attacker = result.user, target = effect.target, damage = effect.amount)
            }
        }
    }

    private fun talentSchemaFor(talentId: String): TalentSchemaV2? =
        content.schemaCatalog.talents.firstOrNull { schema -> schema.id == talentId }

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

    private fun interactableEntityAt(point: Point): EntityId? =
        world.entitiesWith(Position::class, Interactable::class)
            .sortedBy(EntityId::value)
            .firstOrNull { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == point }

    private fun interactAtPlayerPosition(): CommandResolution {
        val entityId = interactableEntityAt(playerPosition())
        if (entityId == null) {
            addMessage("log.interactable.none")
            return CommandResolution.rejected()
        }

        val interactable = requireNotNull(world.get<Interactable>(entityId))
        val schema = requireNotNull(interactableSchemaFor(interactable.id)) {
            "Unknown interactable '${interactable.id}'."
        }
        val position = requireNotNull(world.get<Position>(entityId)).toPoint()

        when (interactable.id) {
            "supply_crate" -> {
                val reward = officialRewardItem(baseId = "scroll_teleport", fallbackBaseId = "healing_potion")
                ItemFactory().createGroundItem(world, reward, position)
                val rewardSchema = requireNotNull(itemSchemaFor(reward.baseId)) {
                    "Unknown item schema '${reward.baseId}'."
                }
                addMessage(
                    "log.interactable.supply_crate",
                    keyArg("interactable", schema.nameKey),
                    keyArg("item", rewardSchema.nameKey),
                )
            }

            "trail_cache",
            "ore_stash",
            "seal_cache",
            -> {
                val reward =
                    when (interactable.id) {
                        "trail_cache" ->
                            zoneRewardItem(
                                profileIds = listOf("loot.greenwood_fringe.reward", "loot.foundation.common"),
                                fallbackBaseId = "healing_potion",
                            )
                        "ore_stash" ->
                            zoneRewardItem(
                                profileIds = listOf("loot.deep_iron_pit.reward", "loot.foundation.elite"),
                                fallbackBaseId = "stamina_draught",
                            )
                        else ->
                            zoneRewardItem(
                                profileIds = listOf("loot.grey_gate_depths.reward", "loot.foundation.boss"),
                                fallbackBaseId = "scroll_teleport",
                            )
                    }
                ItemFactory().createGroundItem(world, reward, position)
                val rewardSchema = requireNotNull(itemSchemaFor(reward.baseId)) {
                    "Unknown item schema '${reward.baseId}'."
                }
                addMessage(
                    "log.interactable.supply_crate",
                    keyArg("interactable", schema.nameKey),
                    keyArg("item", rewardSchema.nameKey),
                )
                when (interactable.id) {
                    "trail_cache" ->
                        recordObjectiveProgress(
                            token = "greenwood.trail_cache",
                            stepKey = "objective.greenwood_signal_hunt.step.trail_cache_opened",
                        )
                    "ore_stash" ->
                        recordObjectiveProgress(
                            token = "deep_iron_pit.ore_stash",
                            stepKey = "objective.deep_iron_pit_forge_run.step.ore_stash_opened",
                        )
                    "seal_cache" ->
                        recordObjectiveProgress(
                            token = "grey_gate.seal_cache",
                            stepKey = "objective.grey_gate_seal_rite.step.seal_cache_opened",
                        )
                }
            }

            "alarm_bonfire" -> {
                addMessage("log.interactable.alarm_bonfire", keyArg("interactable", schema.nameKey))
                val alerted = alertCurrentFloorHostiles()
                if (alerted > 0) {
                    addMessage("log.interactable.alarm_bonfire.alerted", literalArg("count", alerted))
                }
                spawnAlarmReinforcement(position)?.let { reinforcementId ->
                    addMessage("log.interactable.alarm_bonfire.reinforcement", entityArg("monster", reinforcementId))
                }
                recordObjectiveProgress(
                    token = "shattered_outpost.alarm_triggered",
                    stepKey = "objective.shattered_outpost_breach.step.alarm_triggered",
                )
            }

            "warden_beacon",
            "slag_valve",
            "shadow_brazier",
            -> {
                addMessage("log.interactable.alarm_bonfire", keyArg("interactable", schema.nameKey))
                val alerted = alertCurrentFloorHostiles()
                if (alerted > 0) {
                    addMessage("log.interactable.alarm_bonfire.alerted", literalArg("count", alerted))
                }
                spawnAlarmReinforcement(position)?.let { reinforcementId ->
                    addMessage("log.interactable.alarm_bonfire.reinforcement", entityArg("monster", reinforcementId))
                }
                when (interactable.id) {
                    "warden_beacon" ->
                        recordObjectiveProgress(
                            token = "greenwood.warden_beacon",
                            stepKey = "objective.greenwood_signal_hunt.step.warden_beacon_triggered",
                        )
                    "slag_valve" ->
                        recordObjectiveProgress(
                            token = "deep_iron_pit.slag_valve",
                            stepKey = "objective.deep_iron_pit_forge_run.step.slag_valve_opened",
                        )
                    "shadow_brazier" ->
                        recordObjectiveProgress(
                            token = "grey_gate.shadow_brazier",
                            stepKey = "objective.grey_gate_seal_rite.step.shadow_brazier_lit",
                        )
                }
            }

            "armory_gate" -> {
                addMessage("log.interactable.armory_gate", keyArg("interactable", schema.nameKey))
                val reward = supportRewardItem()
                grantRewardItem(reward, position)
                val rewardSchema = requireNotNull(itemSchemaFor(reward.baseId)) {
                    "Unknown support reward item '${reward.baseId}'."
                }
                addMessage(
                    "log.interactable.armory_gate.reward",
                    keyArg("interactable", schema.nameKey),
                    keyArg("item", rewardSchema.nameKey),
                )
                val restored = restoreArmorySupplies()
                if (restored > 0) {
                    addMessage("log.interactable.armory_gate.resupply", literalArg("amount", restored))
                }
                recordObjectiveProgress(
                    token = "shattered_outpost.armory_opened",
                    stepKey = "objective.shattered_outpost_breach.step.armory_opened",
                )
                addMessage("log.objective.advance")
            }

            "hunter_snare",
            "mine_furnace",
            "ritual_altar",
            -> {
                addMessage("log.interactable.armory_gate", keyArg("interactable", schema.nameKey))
                val reward =
                    when (interactable.id) {
                        "hunter_snare" ->
                            zoneRewardItem(
                                profileIds = listOf("loot.greenwood_fringe.reward", "loot.foundation.elite"),
                                fallbackBaseId = "bandit_trophy",
                            )
                        "mine_furnace" ->
                            zoneRewardItem(
                                profileIds = listOf("loot.deep_iron_pit.reward", "loot.foundation.elite"),
                                fallbackBaseId = "forgebreaker_pick",
                            )
                        else ->
                            zoneRewardItem(
                                profileIds = listOf("loot.grey_gate_depths.reward", "loot.foundation.boss"),
                                fallbackBaseId = "sanctified_seal",
                            )
                    }
                grantRewardItem(reward, position)
                val rewardSchema = requireNotNull(itemSchemaFor(reward.baseId)) {
                    "Unknown support reward item '${reward.baseId}'."
                }
                addMessage(
                    "log.interactable.armory_gate.reward",
                    keyArg("interactable", schema.nameKey),
                    keyArg("item", rewardSchema.nameKey),
                )
                val restored = restoreArmorySupplies()
                if (restored > 0) {
                    addMessage("log.interactable.armory_gate.resupply", literalArg("amount", restored))
                }
                when (interactable.id) {
                    "hunter_snare" ->
                        recordObjectiveProgress(
                            token = "greenwood.hunter_snare",
                            stepKey = "objective.greenwood_signal_hunt.step.hunter_snare_cut",
                        )
                    "mine_furnace" ->
                        recordObjectiveProgress(
                            token = "deep_iron_pit.mine_furnace",
                            stepKey = "objective.deep_iron_pit_forge_run.step.furnace_claimed",
                        )
                    "ritual_altar" ->
                        recordObjectiveProgress(
                            token = "grey_gate.ritual_altar",
                            stepKey = "objective.grey_gate_seal_rite.step.ritual_altar_secured",
                        )
                }
                addMessage("log.objective.advance")
            }

            else -> {
                addMessage("log.interactable.none")
                return CommandResolution.rejected()
            }
        }

        world.destroyEntity(entityId)
        return CommandResolution.accepted()
    }

    private fun addInventoryMessage(result: InventoryOperationResult) {
        addMessage(inventoryMessage(result))
    }

    private fun logTalentResult(result: com.ktome.core.talent.TalentResult) {
        addMessage(
            "log.talent.use",
            entityArg("user", result.user),
            talentArg("talent", result.talentId, result.talentName),
        )
        result.effects.forEach { effect ->
            when (effect) {
                is com.ktome.core.talent.TalentEffectResult.Buff -> {
                    addMessage(
                        when (effect.type) {
                            StatusEffectType.WAR_CRY_BUFF -> "log.talent.target_empowered"
                            StatusEffectType.WAR_CRY_DEBUFF -> "log.talent.target_shaken"
                            else -> "log.talent.target_affected"
                        },
                        entityArg("target", effect.target),
                        literalArg("turns", effect.duration),
                    )
                }

                is com.ktome.core.talent.TalentEffectResult.Damage -> {
                    addMessage(
                        if (effect.crit) {
                            "log.talent.damage_crit"
                        } else {
                            "log.talent.damage"
                        },
                        keyArg("talent", talentNameKey(result.talentId)),
                        entityArg("target", effect.target),
                        literalArg("damage", effect.amount),
                    )
                    if (effect.damageType.isElemental && effect.resistanceValue != 0) {
                        addMessage(
                            if (effect.resistanceValue > 0) {
                                "log.talent.damage_resisted"
                            } else {
                                "log.talent.damage_vulnerable"
                            },
                            entityArg("target", effect.target),
                            keyArg("damageType", damageTypeLabelKey(effect.damageType)),
                            literalArg("amount", abs(effect.resistanceValue)),
                        )
                    }
                }

                is com.ktome.core.talent.TalentEffectResult.Heal -> {
                    addMessage(
                        "log.talent.heal",
                        entityArg("target", effect.target),
                        literalArg("amount", effect.amount),
                    )
                }

                is com.ktome.core.talent.TalentEffectResult.Knockback -> {
                    addMessage("log.talent.knockback", entityArg("target", effect.target))
                }

                is com.ktome.core.talent.TalentEffectResult.Miss -> {
                    addMessage(
                        "log.talent.miss",
                        keyArg("talent", talentNameKey(result.talentId)),
                        entityArg("target", effect.target),
                    )
                }

                is com.ktome.core.talent.TalentEffectResult.Movement -> Unit

                is com.ktome.core.talent.TalentEffectResult.ResourceRestore -> {
                    addMessage(
                        "log.talent.resource_restore",
                        entityArg("target", effect.target),
                        literalArg("amount", effect.amount),
                        literalArg("resource", effect.resourceTypeId),
                    )
                }

                is com.ktome.core.talent.TalentEffectResult.StatusApplied -> {
                    addMessage(
                        when (effect.type) {
                            StatusEffectType.STUNNED -> "log.talent.target_stunned"
                            StatusEffectType.ARMOR_BREAK -> "log.talent.target_armor_broken"
                            else -> "log.talent.target_affected"
                        },
                        entityArg("target", effect.target),
                        literalArg("turns", effect.duration),
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
        invalidateRenderSnapshot()
    }

    private fun addMessage(message: RenderLogEventSnapshot) {
        if (messageLog.size == config.messageLogSize) {
            messageLog.removeFirst()
        }
        messageLog += SessionLogEntry(render(message.message), message)
        if (recentSummaryEvents.size == SUMMARY_EVENT_LIMIT) {
            recentSummaryEvents.removeFirst()
        }
        recentSummaryEvents += message.message
        invalidateRenderSnapshot()
    }

    private fun addMessage(
        key: String,
        vararg arguments: RenderTextArgumentSnapshot,
    ) {
        addMessage(
            RenderLogEventSnapshot(
                message = RenderTextTokenSnapshot(key, arguments.toList()),
            ),
        )
    }

    private fun invalidateRenderSnapshot() {
        renderSnapshotRevision += 1
        cachedRenderSnapshot = null
    }

    private fun tr(
        key: String,
        vararg args: Pair<String, Any?>,
    ): String = content.localizer.text(key, *args)

    private fun render(token: RenderTextTokenSnapshot): String =
        content.localizer.text(
            token.key,
            *token.arguments.map { argument -> argument.name to renderArgument(argument) }.toTypedArray(),
        )

    private fun renderArgument(argument: RenderTextArgumentSnapshot): String =
        argument.valueToken?.let(::render)
            ?: argument.valueKey?.let(content.localizer::text)
            ?: argument.value.orEmpty()

    private fun literalArg(
        name: String,
        value: Any?,
    ): RenderTextArgumentSnapshot =
        RenderTextArgumentSnapshot(
            name = name,
            value = value?.toString().orEmpty(),
        )

    private fun keyArg(
        name: String,
        valueKey: String,
    ): RenderTextArgumentSnapshot =
        RenderTextArgumentSnapshot(
            name = name,
            valueKey = valueKey,
        )

    private fun tokenArg(
        name: String,
        valueToken: RenderTextTokenSnapshot,
    ): RenderTextArgumentSnapshot =
        RenderTextArgumentSnapshot(
            name = name,
            valueToken = valueToken,
        )

    private fun qualityLabelKey(quality: ItemQuality): String =
        when (quality) {
            ItemQuality.COMMON -> "item.quality.common"
            ItemQuality.MAGIC -> "item.quality.magic"
            ItemQuality.RARE -> "item.quality.rare"
        }

    private fun runOutcomeReasonKey(outcome: RunOutcome): String =
        when (outcome) {
            is RunOutcome.Defeat ->
                when (outcome.reason) {
                    "player_died" -> "ui.summary.reason.player_died"
                    else -> outcome.reason
                }

            is RunOutcome.Victory ->
                when (outcome.reason) {
                    "boss_defeated" -> "ui.summary.reason.boss_defeated"
                    else -> outcome.reason
                }

            RunOutcome.InProgress -> "ui.summary.reason.in_progress"
        }

    private fun passiveDescriptionToken(passive: EquipmentPassive): RenderTextTokenSnapshot =
        when (passive) {
            is EquipmentPassive.DamageVsTag ->
                RenderTextTokenSnapshot(
                    "ui.inspect.passive.damage_vs_tag",
                    listOf(
                        literalArg("amount", (passive.bonusPercent * 100).toInt()),
                        monsterTagArg("tag", passive.tag),
                    ),
                )

            is EquipmentPassive.HpRegenPerTurn ->
                RenderTextTokenSnapshot(
                    "ui.inspect.passive.hp_regen_turn",
                    listOf(
                        literalArg("amount", passive.amount),
                    ),
                )

            is EquipmentPassive.DamageTypeBonus ->
                RenderTextTokenSnapshot(
                    "ui.inspect.passive.damage_type_bonus",
                    listOf(
                        literalArg("amount", (passive.bonusPercent * 100).toInt()),
                        keyArg("damageType", damageTypeLabelKey(passive.type)),
                    ),
                )

            is EquipmentPassive.ResistanceBonus ->
                RenderTextTokenSnapshot(
                    "ui.inspect.passive.resistance_bonus",
                    listOf(
                        literalArg("amount", passive.amount),
                        keyArg("damageType", damageTypeLabelKey(passive.damageType)),
                    ),
                )
        }

    private fun monsterTagArg(
        name: String,
        tag: String,
    ): RenderTextArgumentSnapshot =
        monsterTagLabelKeyOrNull(tag)
            ?.let { key -> keyArg(name, key) }
            ?: literalArg(name, tag)

    private fun monsterTagLabelKeyOrNull(tag: String): String? =
        when (tag) {
            "bandit" -> "monster.tag.bandit"
            else -> null
        }

    private fun ItemBaseDef.toRuntimeItem(): ItemInstance =
        ItemInstance(
            baseId = id,
            name = name,
            type = type,
            slot = slot,
            glyph = glyph,
            colorHex = colorHex,
            stats = baseStats.copy(),
            effect = effect,
            resourceTypeId = resourceTypeId,
            magnitude = magnitude,
            passive = passive,
        )

    private fun primaryStatLabelKey(stat: PrimaryStat): String =
        when (stat) {
            PrimaryStat.STR -> "ui.stat.str"
            PrimaryStat.DEX -> "ui.stat.dex"
            PrimaryStat.CON -> "ui.stat.con"
            PrimaryStat.WIL -> "ui.stat.wil"
        }

    private fun captureLevelUpFeedbackSnapshot(): LevelUpFeedbackSnapshot {
        val stats = requireNotNull(world.get<Stats>(playerId)) { "Missing Stats for $playerId." }.copy()
        val health = requireNotNull(world.get<Health>(playerId)) { "Missing Health for $playerId." }
        val resource = resolvePlayerResourceView()
        return LevelUpFeedbackSnapshot(
            stats = stats,
            maxHp = health.max,
            resourceTypeId = resource.typeId,
            resourceMax = resource.max,
        )
    }

    private fun logLevelUpGrowth(
        baseline: LevelUpFeedbackSnapshot,
        updated: LevelUpFeedbackSnapshot,
    ) {
        buildLevelUpStatArguments(baseline.stats, updated.stats)?.let { arguments ->
            addMessage("log.level_up.stats", *arguments.toTypedArray())
        }

        val hpDelta = updated.maxHp - baseline.maxHp
        if (hpDelta > 0) {
            addMessage("log.level_up.hp_max", literalArg("amount", hpDelta))
        }

        val resourceDelta = updated.resourceMax - baseline.resourceMax
        if (resourceDelta > 0) {
            addMessage(
                "log.level_up.resource_max",
                keyArg("resource", resourceLabelKey(updated.resourceTypeId)),
                literalArg("amount", resourceDelta),
            )
        }
    }

    private fun buildLevelUpStatArguments(
        baseline: Stats,
        updated: Stats,
    ): List<RenderTextArgumentSnapshot>? {
        val deltas =
            linkedMapOf(
                PrimaryStat.STR to (updated.str - baseline.str),
                PrimaryStat.DEX to (updated.dex - baseline.dex),
                PrimaryStat.CON to (updated.con - baseline.con),
                PrimaryStat.WIL to (updated.wil - baseline.wil),
            )
        if (deltas.values.none { delta -> delta != 0 }) {
            return null
        }
        val separator = levelUpStatSeparator()
        val orderedStats = deltas.keys.toList()
        return buildList {
            orderedStats.forEachIndexed { index, stat ->
                val delta = deltas.getValue(stat)
                add(levelUpStatLabelArg(stat, delta))
                add(levelUpStatDeltaArg(stat, delta))
                if (index < orderedStats.lastIndex) {
                    val hasCurrent = delta != 0
                    val hasNext = orderedStats.drop(index + 1).any { nextStat -> deltas.getValue(nextStat) != 0 }
                    add(literalArg("sep${index + 1}", if (hasCurrent && hasNext) separator else ""))
                }
            }
        }
    }

    private fun levelUpStatSeparator(): String =
        when (content.localizer.locale) {
            GameLocale.ZH_CN -> "、"
            GameLocale.EN_US -> ", "
        }

    private fun levelUpStatLabelArg(
        stat: PrimaryStat,
        delta: Int,
    ): RenderTextArgumentSnapshot =
        if (delta == 0) {
            literalArg("${stat.name.lowercase()}Label", "")
        } else {
            keyArg("${stat.name.lowercase()}Label", primaryStatLabelKey(stat))
        }

    private fun levelUpStatDeltaArg(
        stat: PrimaryStat,
        delta: Int,
    ): RenderTextArgumentSnapshot =
        literalArg(
            stat.name.lowercase(),
            if (delta == 0) {
                ""
            } else {
                "+$delta"
            },
        )

    private fun itemTypeLabel(type: ItemType): String =
        tr(itemTypeLabelKey(type))

    private fun itemTypeLabelKey(type: ItemType): String =
        when (type) {
            ItemType.WEAPON -> "ui.item.type.weapon"
            ItemType.ARMOR -> "ui.item.type.armor"
            ItemType.CONSUMABLE -> "ui.item.type.consumable"
        }

    private fun equipSlotLabel(slot: EquipSlot): String =
        tr(equipSlotLabelKey(slot))

    private fun equipSlotLabelKey(slot: EquipSlot): String =
        when (slot) {
            EquipSlot.WEAPON -> "ui.sidebar.weapon"
            EquipSlot.OFF_HAND -> "ui.sidebar.off_hand"
            EquipSlot.ARMOR -> "ui.sidebar.armor"
        }

    private fun stairName(direction: StairDirection): String =
        tr(stairNameKey(direction))

    private fun stairNameKey(direction: StairDirection): String =
        when (direction) {
            StairDirection.UP -> "stairs.up.name"
            StairDirection.DOWN -> "stairs.down.name"
        }

    private fun aiLabel(type: com.ktome.core.ecs.AIType): String =
        tr(aiLabelKey(type))

    private fun aiLabelKey(type: com.ktome.core.ecs.AIType): String =
        when (type) {
            com.ktome.core.ecs.AIType.CHASE -> "ai.chase"
            com.ktome.core.ecs.AIType.KITE -> "ai.kite"
            com.ktome.core.ecs.AIType.PATROL -> "ai.patrol"
        }

    private fun statusEffectName(type: StatusEffectType): String =
        tr(statusEffectNameKey(type))

    private fun statusEffectNameKey(type: StatusEffectType): String =
        when (type) {
            StatusEffectType.STUNNED -> "status.stunned"
            StatusEffectType.ARMOR_BREAK -> "status.armor_break"
            StatusEffectType.WAR_CRY_BUFF -> "status.war_cry_buff"
            StatusEffectType.WAR_CRY_DEBUFF -> "status.war_cry_debuff"
            StatusEffectType.GUARD_STANCE_BUFF -> "status.guard_stance_buff"
            StatusEffectType.ARCANE_SHIELD_BUFF -> "status.arcane_shield_buff"
            StatusEffectType.UNYIELDING_BUFF -> "status.unyielding_buff"
            StatusEffectType.MANA_SURGE_BUFF -> "status.mana_surge_buff"
            StatusEffectType.STEALTH_BUFF -> "status.stealth_buff"
            StatusEffectType.CURSED -> "status.cursed"
            StatusEffectType.HOLY_SHIELD_BUFF -> "status.holy_shield_buff"
            StatusEffectType.DEVOTION_BUFF -> "status.devotion_buff"
            StatusEffectType.HOLY_AURA_BUFF -> "status.holy_aura_buff"
        }

    private fun talentFailureMessage(result: TalentUseResult.Failure): RenderLogEventSnapshot =
        when (result.code) {
            TalentFailureCode.UNKNOWN_TALENT ->
                RenderLogEventSnapshot(
                    message = RenderTextTokenSnapshot("log.talent.failure.unknown"),
                )
            TalentFailureCode.UNSUPPORTED_TALENT ->
                RenderLogEventSnapshot(
                    message = RenderTextTokenSnapshot("log.talent.failure.unsupported"),
                )
            TalentFailureCode.COOLDOWN ->
                RenderLogEventSnapshot(
                    message =
                        RenderTextTokenSnapshot(
                            "log.talent.failure.cooldown",
                            listOf(
                                talentArg("talent", result.talentId, result.talentName),
                            ),
                        ),
                )
            TalentFailureCode.NO_STAMINA ->
                RenderLogEventSnapshot(
                    message = RenderTextTokenSnapshot("log.talent.failure.no_stamina"),
                )
            TalentFailureCode.NO_RESOURCE ->
                RenderLogEventSnapshot(
                    message = RenderTextTokenSnapshot("log.talent.failure.no_resource"),
                )
            TalentFailureCode.TARGET_REQUIRED ->
                RenderLogEventSnapshot(
                    message = RenderTextTokenSnapshot("log.talent.failure.target_required"),
                )
            TalentFailureCode.OUT_OF_RANGE ->
                RenderLogEventSnapshot(
                    message = RenderTextTokenSnapshot("log.talent.failure.out_of_range"),
                )
            TalentFailureCode.NO_TARGET ->
                RenderLogEventSnapshot(
                    message = RenderTextTokenSnapshot("log.talent.failure.no_target"),
                )
            TalentFailureCode.NO_CHARGE_PATH ->
                RenderLogEventSnapshot(
                    message = RenderTextTokenSnapshot("log.talent.failure.no_charge_path"),
                )
        }

    private fun inventoryMessage(result: InventoryOperationResult): RenderLogEventSnapshot =
        when (result) {
            is InventoryOperationResult.Success ->
                when (result.code) {
                    InventoryOperationCode.PICK_UP ->
                        inventoryItemMessage("log.inventory.pick_up", result)
                    InventoryOperationCode.EQUIP ->
                        inventoryItemMessage("log.inventory.equip", result)
                    InventoryOperationCode.REMOVE ->
                        inventoryItemMessage("log.inventory.remove", result)
                    InventoryOperationCode.CONSUME_USE ->
                        inventoryItemMessage("log.inventory.consume.use", result)
                    InventoryOperationCode.CONSUME_READ ->
                        inventoryItemMessage("log.inventory.consume.read", result)
                    InventoryOperationCode.DROP ->
                        inventoryItemMessage("log.inventory.drop", result)
                    else -> error("Unsupported inventory success code ${result.code}.")
                }

            is InventoryOperationResult.Failure ->
                when (result.code) {
                    InventoryOperationCode.NOT_ITEM ->
                        RenderLogEventSnapshot(
                            message = RenderTextTokenSnapshot("log.inventory.not_item"),
                        )
                    InventoryOperationCode.NOT_ON_GROUND ->
                        inventoryItemMessage("log.inventory.not_on_ground", result)
                    InventoryOperationCode.PACK_FULL ->
                        RenderLogEventSnapshot(
                            message = RenderTextTokenSnapshot("log.inventory.pack_full"),
                        )
                    InventoryOperationCode.PACK_SLOT_EMPTY ->
                        RenderLogEventSnapshot(
                            message = RenderTextTokenSnapshot("log.inventory.pack_slot_empty"),
                        )
                    InventoryOperationCode.CANNOT_EQUIP ->
                        inventoryItemMessage("log.inventory.cannot_equip", result)
                    InventoryOperationCode.NOTHING_EQUIPPED ->
                        RenderLogEventSnapshot(
                            message =
                                RenderTextTokenSnapshot(
                                    "log.inventory.slot_nothing_equipped",
                                    listOf(
                                        result.slot?.let { slot -> keyArg("slot", equipSlotLabelKey(slot)) } ?: literalArg("slot", "-"),
                                    ),
                                ),
                        )
                    InventoryOperationCode.NOT_CONSUMABLE ->
                        inventoryItemMessage("log.inventory.not_consumable", result)
                    InventoryOperationCode.NO_TELEPORT_DESTINATION ->
                        RenderLogEventSnapshot(
                            message = RenderTextTokenSnapshot("log.inventory.no_teleport_destination"),
                        )
                    InventoryOperationCode.NO_RESOURCE_POOL ->
                        RenderLogEventSnapshot(
                            message = RenderTextTokenSnapshot("log.inventory.no_resource_pool"),
                        )
                    else -> error("Unsupported inventory failure code ${result.code}.")
                }
        }

    private fun inventoryItemMessage(
        key: String,
        result: InventoryOperationResult.Success,
    ): RenderLogEventSnapshot =
        inventoryItemMessage(
            key = key,
            itemBaseId = result.itemBaseId,
            itemName = result.itemName,
            itemQuality = result.itemQuality,
            itemMaterialId = result.itemMaterialId,
            itemAffixIds = result.itemAffixIds,
        )

    private fun inventoryItemMessage(
        key: String,
        result: InventoryOperationResult.Failure,
    ): RenderLogEventSnapshot =
        inventoryItemMessage(
            key = key,
            itemBaseId = result.itemBaseId,
            itemName = result.itemName,
            itemQuality = result.itemQuality,
            itemMaterialId = result.itemMaterialId,
            itemAffixIds = result.itemAffixIds,
        )

    private fun inventoryItemMessage(
        key: String,
        itemBaseId: String?,
        itemName: String?,
        itemQuality: ItemQuality?,
        itemMaterialId: String?,
        itemAffixIds: List<String>,
    ): RenderLogEventSnapshot =
        RenderLogEventSnapshot(
            message =
                RenderTextTokenSnapshot(
                    key,
                    listOf(
                        itemDisplayArgument(
                            name = "item",
                            itemBaseId = itemBaseId,
                            fallbackName = itemName,
                            itemQuality = itemQuality,
                            itemMaterialId = itemMaterialId,
                            itemAffixIds = itemAffixIds,
                        ),
                    ),
                ),
        )

    private fun itemArg(
        name: String,
        itemBaseId: String?,
        fallbackName: String?,
    ): RenderTextArgumentSnapshot =
        itemBaseId
            ?.let(::itemNameKeyOrNull)
            ?.let { key -> keyArg(name, key) }
            ?: literalArg(name, fallbackName.orEmpty())

    private fun itemDisplayArgument(
        name: String,
        item: ItemInstance,
        includeQuality: Boolean = true,
    ): RenderTextArgumentSnapshot =
        itemDisplayArgument(
            name = name,
            itemBaseId = item.baseId,
            fallbackName = item.name,
            itemQuality = item.quality,
            itemMaterialId = item.materialId,
            itemAffixIds = item.affixes.map { affix -> affix.id },
            includeQuality = includeQuality,
        )

    private fun itemDisplayArgument(
        name: String,
        itemBaseId: String?,
        fallbackName: String?,
        itemQuality: ItemQuality?,
        itemMaterialId: String?,
        itemAffixIds: List<String>,
        includeQuality: Boolean = true,
    ): RenderTextArgumentSnapshot =
        itemDisplayToken(
            itemBaseId = itemBaseId,
            itemQuality = itemQuality,
            itemMaterialId = itemMaterialId,
            itemAffixIds = itemAffixIds,
            includeQuality = includeQuality,
        )?.let { token -> tokenArg(name, token) }
            ?: itemArg(name, itemBaseId, fallbackName)

    private fun itemDisplayToken(
        item: ItemInstance,
        includeQuality: Boolean = true,
    ): RenderTextTokenSnapshot? =
        itemDisplayToken(
            itemBaseId = item.baseId,
            itemQuality = item.quality,
            itemMaterialId = item.materialId,
            itemAffixIds = item.affixes.map { affix -> affix.id },
            includeQuality = includeQuality,
        )

    private fun itemDisplayToken(
        itemBaseId: String?,
        itemQuality: ItemQuality?,
        itemMaterialId: String?,
        itemAffixIds: List<String>,
        includeQuality: Boolean,
    ): RenderTextTokenSnapshot? {
        val baseNameKey = itemBaseId?.let(::itemNameKeyOrNull) ?: return null
        val affixSchemas = itemAffixIds.mapNotNull(::affixSchemaFor)
        val prefixes = affixSchemas.filter { schema -> schema.type == AffixType.PREFIX }
        val suffixes = affixSchemas.filter { schema -> schema.type == AffixType.SUFFIX }
        return RenderTextTokenSnapshot(
            key = "item.display.composed",
            arguments =
                listOf(
                    itemDisplayQualityArg("quality", itemQuality, includeQuality),
                    itemDisplayAffixArg("prefix1", prefixes.getOrNull(0)?.nameKey),
                    itemDisplayAffixArg("prefix2", prefixes.getOrNull(1)?.nameKey),
                    itemDisplayMaterialArg("material", itemMaterialId?.let(::materialSchemaFor)?.nameKey),
                    keyArg("base", baseNameKey),
                    itemDisplayAffixArg("suffix1", suffixes.getOrNull(0)?.nameKey, isSuffix = true),
                    itemDisplayAffixArg("suffix2", suffixes.getOrNull(1)?.nameKey, isSuffix = true),
                ),
        )
    }

    private fun itemDisplayQualityArg(
        name: String,
        itemQuality: ItemQuality?,
        includeQuality: Boolean,
    ): RenderTextArgumentSnapshot =
        itemQuality
            ?.takeIf { includeQuality && it != ItemQuality.COMMON }
            ?.let(::qualityLabelKey)
            ?.let { qualityNameKey ->
                tokenArg(
                    name,
                    RenderTextTokenSnapshot(
                        key = "item.display.part.quality",
                        arguments = listOf(keyArg("quality", qualityNameKey)),
                    ),
                )
            }
            ?: literalArg(name, "")

    private fun itemDisplayMaterialArg(
        name: String,
        materialNameKey: String?,
    ): RenderTextArgumentSnapshot =
        materialNameKey
            ?.let { valueKey ->
                tokenArg(
                    name,
                    RenderTextTokenSnapshot(
                        key = "item.display.part.material",
                        arguments = listOf(keyArg("material", valueKey)),
                    ),
                )
            }
            ?: literalArg(name, "")

    private fun itemDisplayAffixArg(
        name: String,
        affixNameKey: String?,
        isSuffix: Boolean = false,
    ): RenderTextArgumentSnapshot =
        affixNameKey
            ?.let { valueKey ->
                tokenArg(
                    name,
                    RenderTextTokenSnapshot(
                        key =
                            if (isSuffix) {
                                "item.display.part.suffix"
                            } else {
                                "item.display.part.prefix"
                            },
                        arguments = listOf(keyArg("affix", valueKey)),
                    ),
                )
            }
            ?: literalArg(name, "")

    private fun itemNameKeyOrNull(baseItemId: String): String? =
        content.schemaCatalog.itemBundle.items.firstOrNull { schema -> schema.id == baseItemId }?.nameKey

    private fun lootItemForMonsterDeath(target: EntityId): ItemInstance? {
        val templateId = world.get<MonsterTemplateId>(target)?.value ?: return null
        val profileId =
            content.allMonsterTemplates()
                .firstOrNull { template -> template.id == templateId }
                ?.lootProfileId
                ?.takeIf(String::isNotBlank)
                ?: return null
        val profile = lootProfile(profileId) ?: return null
        val candidateItems = profile.itemIds.mapNotNull(::itemBaseDef)
        if (candidateItems.isEmpty()) {
            return null
        }
        val floorCandidates = candidateItems.filter { item -> currentFloor() in item.dropFloors }.ifEmpty { candidateItems }
        return ItemGenerator(content.itemBundle, sessionRandom).generate(chooseWeightedLootItem(floorCandidates), currentFloor())
    }

    private fun chooseWeightedLootItem(candidates: List<ItemBaseDef>): ItemBaseDef {
        val totalWeight = candidates.sumOf { item -> item.dropWeight.coerceAtLeast(1) }
        require(totalWeight > 0) { "Loot selection requires a positive total weight." }
        var roll = sessionRandom.nextInt(0, totalWeight)
        candidates.forEach { item ->
            roll -= item.dropWeight.coerceAtLeast(1)
            if (roll < 0) {
                return item
            }
        }
        return candidates.last()
    }

    private fun talentNameKey(talentId: String): String =
        talentNameKeyOrNull(talentId) ?: "log.talent.failure.unknown"

    private fun talentNameKeyOrNull(talentId: String): String? =
        content.schemaCatalog.talents.firstOrNull { schema -> schema.id == talentId }?.nameKey

    private fun talentArg(
        name: String,
        talentId: String?,
        fallbackName: String?,
    ): RenderTextArgumentSnapshot =
        talentId
            ?.let(::talentNameKeyOrNull)
            ?.let { key -> keyArg(name, key) }
            ?: literalArg(name, fallbackName.orEmpty())

    private fun entityArg(
        name: String,
        entityId: EntityId,
    ): RenderTextArgumentSnapshot =
        entityNameKeyOrNull(entityId)
            ?.let { key -> keyArg(name, key) }
            ?: literalArg(name, world.get<Name>(entityId)?.value ?: tr("actor.unknown.name"))

    private fun StatModifier.toSnapshot(): ItemStatModifierSnapshot =
        ItemStatModifierSnapshot(
            str = str,
            dex = dex,
            con = con,
            wil = wil,
            attack = attack,
            defense = defense,
            accuracy = accuracy,
            evasion = evasion,
            speed = speed,
            maxHp = maxHp,
            maxStamina = maxStamina,
            hpRegen = hpRegen,
            staminaRegen = staminaRegen,
            critChance = critChance,
            talentPower = talentPower,
        )

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

    private fun activeBossDefinition(): BossDefinition? = content.bossDefinitionForZone(config.zoneId)

    private data class SessionLogEntry(
        val text: String,
        val snapshot: RenderLogEventSnapshot,
    )

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
                bossDefinitions =
                    mapOf(
                        "compatibility_boss_encounter" to
                            BossDefinition(
                                encounterId = "compatibility_boss_encounter",
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
                    ),
                schemaCatalog =
                    SchemaCatalog(
                        professions = emptyList(),
                        talents = emptyList(),
                        talentTrees = emptyList(),
                        monsters = emptyList(),
                        bossEncounters = emptyList(),
                        zones = emptyList(),
                        interactables = emptyList(),
                        objectiveSets = emptyList(),
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
                        passive = item.passive,
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
