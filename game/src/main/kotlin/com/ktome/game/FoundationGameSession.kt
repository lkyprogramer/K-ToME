package com.ktome.game

import com.ktome.core.ai.AIAction
import com.ktome.core.ai.AIActionType
import com.ktome.core.ai.AIDecisionTrace
import com.ktome.core.ai.AIDefaultBehavior
import com.ktome.core.ai.AIPathCommand
import com.ktome.core.ai.AIPathing
import com.ktome.core.ai.AIPathingActorSnapshot
import com.ktome.core.ai.AIPathingContext
import com.ktome.core.ai.AIPathingResult
import com.ktome.core.ai.AIPathingTargetSnapshot
import com.ktome.core.ai.AIPerceptionState
import com.ktome.core.ai.AIProfileDecisionContext
import com.ktome.core.ai.AIProfileResolver
import com.ktome.core.ai.BossEncounterState
import com.ktome.core.ai.BossPhaseEventType
import com.ktome.core.ai.BossPhaseEvaluationContext
import com.ktome.core.ai.BossPhaseManager
import com.ktome.core.ai.BossPhaseResolution
import com.ktome.core.ai.BossPhaseTransitionTiming
import com.ktome.core.ai.BossTrace
import com.ktome.core.ai.DangerLevel
import com.ktome.core.ai.PendingTelegraphState
import com.ktome.core.ai.StealthTauntHandler
import com.ktome.core.ai.ThreatRatingResolver
import com.ktome.core.combat.CombatRuleset
import com.ktome.core.combat.CombatResolver
import com.ktome.core.combat.DamageFormula
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
import com.ktome.core.effect.AreaEffectEmitter
import com.ktome.core.effect.WorldEffect
import com.ktome.core.event.DamageDealtEvent
import com.ktome.core.event.EntityDeathEvent
import com.ktome.core.event.ExperienceGainedEvent
import com.ktome.core.event.LevelUpEvent
import com.ktome.core.event.MissEvent
import com.ktome.core.event.StatusAppliedEvent
import com.ktome.core.event.StatusCleanseEvent
import com.ktome.core.event.StatusInteractionEvent
import com.ktome.core.event.StatusTickEvent
import com.ktome.core.event.StealthBrokenEvent
import com.ktome.core.event.TauntOverrideEvent
import com.ktome.core.fov.Shadowcasting
import com.ktome.core.inscription.InscriptionCategory
import com.ktome.core.inscription.InscriptionCooldownState
import com.ktome.core.inscription.InscriptionDef
import com.ktome.core.inscription.InscriptionEffect
import com.ktome.core.inscription.InscriptionLoadout
import com.ktome.core.inscription.InscriptionManager
import com.ktome.core.item.AffixSelectionContext
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
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.item.PassiveDamageAdjustment
import com.ktome.core.item.PassiveEffectResolver
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.movement.MovementRules
import com.ktome.core.progression.ExperienceSystem
import com.ktome.core.profile.RunSummary as ProfileRunSummary
import com.ktome.core.profile.MilestoneRewardSummary
import com.ktome.core.race.RaceTalentPointBank
import com.ktome.core.race.RaceTalentPointProgression
import com.ktome.core.random.RandomSource
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.core.random.StatefulRandomSource
import com.ktome.core.resource.EquilibriumAffinity
import com.ktome.core.resource.EquilibriumState
import com.ktome.core.resource.ResourceAxis
import com.ktome.core.resource.ResourceType
import com.ktome.core.resource.StaminaPools
import com.ktome.core.run.RunOutcome
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.economy.ShopInventoryState
import com.ktome.core.economy.ShardEconomy
import com.ktome.core.economy.ShopNode
import com.ktome.core.economy.ShopOffer
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.EquipmentSlotSnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.InscriptionSlotSnapshot
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
import com.ktome.core.snapshot.RouteOptionSnapshot
import com.ktome.core.snapshot.RouteSelectionSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.ShopOfferSnapshot
import com.ktome.core.snapshot.ShopPanelSnapshot
import com.ktome.core.snapshot.ShopSellEntrySnapshot
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.status.EffectCategory
import com.ktome.core.status.EffectCarrierKind
import com.ktome.core.status.StatusDefinitions
import com.ktome.core.status.StatusLifecycle
import com.ktome.core.status.StatusTickResolver
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.DamageMultiplierResolver
import com.ktome.core.talent.DescriptionModel
import com.ktome.core.talent.DescriptionValue
import com.ktome.core.talent.DynamicDescriptionResolver
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.RespecManager
import com.ktome.core.status.StatusEffectType
import com.ktome.core.talent.RollbackManager
import com.ktome.core.talent.TalentAllocationDraft
import com.ktome.core.talent.TalentAllocationPlanner
import com.ktome.core.talent.TalentFailureCode
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.talent.TalentPrerequisiteValidator
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.talent.TalentResolver
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.core.talent.TalentUseResult
import com.ktome.core.turn.TurnActorState
import com.ktome.core.turn.TurnScheduler
import com.ktome.core.world.WorldProgressDef
import com.ktome.core.world.ObjectiveState
import com.ktome.core.world.QuestProgress
import com.ktome.core.world.RewardClaimPolicy
import com.ktome.core.world.RouteReward
import com.ktome.core.world.ZoneConnection
import com.ktome.game.data.schema.ItemBundleSchemaV2
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.SchemaLevelRange
import com.ktome.game.data.schema.SchemaMapSize
import com.ktome.game.data.schema.TalentLevelEffectSchemaV2
import com.ktome.game.data.schema.TalentSchemaV2
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.i18n.GameLocale
import com.ktome.game.data.schema.InteractableSchemaV2
import com.ktome.game.objective.ObjectiveCompletionRule
import com.ktome.game.objective.ObjectiveCompletionTrigger
import com.ktome.game.objective.ObjectiveRuntimeEvaluator
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.i18n.Localizer
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate
import java.nio.file.Files
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class ZoneRuntimeBundle(
    val config: FoundationGameConfig,
    val dungeonManager: DungeonManager<FloorRuntimeState>,
    val initialMessages: List<RenderLogEventSnapshot>,
)

private const val SUMMARY_EVENT_LIMIT: Int = 5
private const val AI_TRACE_LIMIT: Int = 64

class FoundationGameSession internal constructor(
    config: FoundationGameConfig,
    private val content: GameContent,
    private val saveManager: SaveManager,
    dungeonManager: DungeonManager<FloorRuntimeState>,
    private var playerSnapshot: PlayerSnapshot,
    initialMessageLog: List<RenderLogEventSnapshot> = emptyList(),
    private var turnCount: Int = 0,
    private var headlessTurnEquivalent: Int = 0,
    private var worldProgress: WorldProgressDef = WorldProgressDef(),
    private var shardBalance: Int = 0,
    private var shopStates: MutableMap<String, ShopInventoryState> = linkedMapOf(),
    restoredMilestoneRewardSummaries: List<MilestoneRewardSummary> = emptyList(),
    private val inventoryManager: InventoryManager = InventoryManager(),
    private val combatRandomSource: RandomSource = defaultCombatRandomSource(config, turnCount),
    private val combatResolver: CombatResolver = CombatResolver(combatRandomSource),
    private val talentRegistry: TalentRegistry = content.talentRegistry,
    private val talentResolver: TalentResolver = TalentResolver(talentRegistry, combatResolver, content.statusCatalog),
    private val sessionRandom: RandomSource = defaultSessionRandomSource(config, turnCount),
    private val restoredPendingActionIds: List<Int> = emptyList(),
    private val restoredActiveTurnActorId: Int? = null,
    private val zoneRuntimeFactory: (FoundationGameConfig) -> ZoneRuntimeBundle = { unsupportedZoneConfig ->
        error("Zone transition is not supported for config $unsupportedZoneConfig.")
    },
    private val isolatedZoneSlice: Boolean = false,
) {
    private val talentTreeOwnerResolver =
        TalentTreeOwnerResolver(content.schemaCatalog.talentTrees.associateBy { tree -> tree.id })

    private data class LevelUpFeedbackSnapshot(
        val stats: Stats,
        val maxHp: Int,
        val resourceTypeId: String,
        val resourceMax: Int,
    )

    private data class TalentUiDetails(
        val talentId: String,
        val name: String,
        val descKey: String?,
        val level: Int,
        val committedLevel: Int,
        val maxLevel: Int,
        val resourceCost: Int,
        val resourceTypeId: String,
        val range: Int,
        val minRange: Int,
        val currentCooldown: Int,
        val maxCooldown: Int,
        val requiresTarget: Boolean,
        val descriptionModel: DescriptionModel,
        val nextBreakpointPreview: TalentBreakpointPreviewSnapshot?,
        val hasPendingAllocation: Boolean,
    )

    private data class BossPhaseTurnUpdate(
        val profile: com.ktome.core.ai.AIProfile?,
        val phaseChanged: Boolean,
    )

    private data class RouteAdvanceOption(
        val connection: ZoneConnection,
        val destinationZoneId: String,
        val reward: RouteReward? = null,
    )

    private data class RewardGenerationContext(
        val rewardSource: MilestoneRewardSource,
        val sourceId: String,
        val floor: Int,
        val qualityFloor: ItemQuality,
        val minAffixCount: Int,
        val routeBiasTags: Set<String> = emptySet(),
        val reservedSlots: Set<EquipSlot> = emptySet(),
        val occupiedSlots: Set<EquipSlot> = emptySet(),
        val replacementSlot: EquipSlot? = null,
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
    private val recentAiDecisionTraces = ArrayDeque<AIDecisionTrace>()
    private val recentBossTraces = ArrayDeque<BossTrace>()
    private val recordedMilestoneRewardSummaries = restoredMilestoneRewardSummaries.toMutableList()
    private val respecManager: RespecManager = RespecManager()
    private var activeShopId: String? = null
    private var pendingRouteSelection: List<RouteAdvanceOption> = emptyList()
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
        StatsCalculator.recalculateAndStore(world, playerId)
        ensurePlayerResourcePools()
        ensurePlayerInscriptions()
        syncPlayerResistanceProfile()
        restorePendingZoneAdvanceIfNeeded()
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
        content = compatibilityContent(config = config, talentRegistry = talentRegistry, world = world, currentFloor = config.floor),
        saveManager = SaveManager(Files.createTempDirectory("ktome-session-save")),
        dungeonManager = compatibilityDungeonManager(config, map, world, playerId),
        playerSnapshot = SessionSnapshotMapper.capturePlayer(world, playerId),
        initialMessageLog = listOf(RenderLogEventSnapshot(RenderTextTokenSnapshot("log.session.enter_dungeon"))),
        worldProgress = WorldProgressDef(),
        headlessTurnEquivalent = 0,
        shardBalance = 0,
        shopStates = linkedMapOf(),
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

    fun worldProgress(): WorldProgressDef = worldProgress

    fun currentShardBalance(): Int = shardBalance

    fun shopStates(): List<ShopInventoryState> = shopStates.values.sortedBy(ShopInventoryState::shopId)

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

    fun currentHeadlessTurnEquivalent(): Int = headlessTurnEquivalent

    fun currentBuildHash(): String {
        val equipmentHash =
            EquipSlot.entries.joinToString(separator = "|") { slot ->
                val equipped = equippedItemFor(slot)
                if (equipped == null) {
                    "${slot.name}:-"
                } else {
                    "${slot.name}:${equipped.baseId}:${equipped.materialId ?: "-"}:${equipped.affixes.joinToString(separator = "+", transform = com.ktome.core.item.AffixDef::id)}"
                }
            }
        val talentHash =
            talentSlots()
                .sortedBy { slot -> slot.slot }
                .joinToString(separator = "|") { slot -> "${slot.slot}:${slot.talentId}:${slot.level}" }
        val inscriptionHash =
            buildInscriptionSnapshots()
                .sortedBy(InscriptionSlotSnapshot::hotkey)
                .joinToString(separator = "|") { slot -> "${slot.hotkey}:${slot.inscriptionId}" }
        return listOf(
            config.playerProfessionId,
            config.playerRaceId,
            equipmentHash,
            talentHash,
            inscriptionHash,
        ).joinToString(separator = "#")
    }

    fun milestoneRewardSummaries(): List<MilestoneRewardSummary> =
        recordedMilestoneRewardSummaries.map(::finalizeMilestoneRewardSummary)

    private fun persistedMilestoneRewardSummaries(): List<MilestoneRewardSummary> = recordedMilestoneRewardSummaries.toList()

    fun recentEventLog(limit: Int = 20): List<String> = recentEvents.takeLast(limit)

    fun isGameOver(): Boolean = runOutcome is RunOutcome.Defeat

    fun isVictory(): Boolean = runOutcome is RunOutcome.Victory

    fun runOutcome(): RunOutcome = runOutcome

    fun outcomeSummary(): OutcomeSummary? =
        if (!runOutcome.isTerminal) {
            null
        } else {
            val health = requireNotNull(world.get<Health>(playerId)) { "Missing Health for $playerId." }
            val resource = resolvePlayerResourceView()
            val progressStageNameKey = outcomeProgressStageNameKey()
            OutcomeSummary(
                outcome = runOutcome,
                floorReached = currentFloor(),
                maxFloor = config.maxFloor,
                turns = turnCount,
                headlessTurnEquivalent = headlessTurnEquivalent,
                playerLevel = playerStatus().level,
                zoneNameKey = currentZoneSchema().nameKey,
                progressStageNameKey = progressStageNameKey,
                zonePath = config.zoneRoute,
                zonePathNameKeys = config.zoneRoute.mapNotNull(::zoneNameKeyFor),
                shardBalance = shardBalance,
                defeatedBossIds = worldProgress.defeatedBossIds.sorted(),
                defeatedBossNameKeys = defeatedBossNameKeys(),
                claimedRouteRewardNameKeys = claimedRouteRewardNameKeys(),
                outcomeReasonKey = runOutcomeReasonKey(runOutcome),
                failureSummaryKey = outcomeFailureSummaryKey(progressStageNameKey),
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

    fun profileRunSummary(finishedAtEpochMillis: Long): ProfileRunSummary? =
        if (!runOutcome.isTerminal) {
            null
        } else {
            ProfileRunSummary(
                seed = config.seed,
                finishedAtEpochMillis = finishedAtEpochMillis,
                classId = config.playerProfessionId,
                raceId = config.playerRaceId,
                finalZoneId = config.zoneId,
                turnCount = turnCount,
                headlessTurnEquivalent = headlessTurnEquivalent,
                zoneRouteHash = zoneRouteHash(config.zoneRoute),
                zonePath = config.zoneRoute,
                defeatedBossIds = worldProgress.defeatedBossIds.sorted(),
                claimedRouteRewardIds = worldProgress.claimedRouteRewards.sorted(),
                shardBalance = shardBalance,
                buildHash = currentBuildHash(),
                milestoneRewards = milestoneRewardSummaries(),
                rulesetVersion = CombatRuleset.RULESET_VERSION,
                victory = isVictory(),
                defeatReason = if (isVictory()) null else runOutcome.toString(),
            )
        }

    fun canAscend(): Boolean = activeFloorState.stairsUp != null && playerPosition() == activeFloorState.stairsUp

    fun canDescend(): Boolean = activeFloorState.stairsDown != null && playerPosition() == activeFloorState.stairsDown

    fun hasPendingStatAllocation(): Boolean = playerStatus().statPoints > 0

    fun hasPendingTalentAllocation(): Boolean =
        playerStatus().talentPoints > 0 ||
            playerStatus().raceTalentPoints > 0 ||
            world.get<TalentLoadout>(playerId)?.let { loadout ->
                TalentAllocationPlanner.hasPendingChanges(liveRanks = loadout.talentLevels, draft = talentDraft())
            } == true

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

    internal fun automationBossPoint(): Point? =
        world.entitiesWith(Position::class, BossEncounterState::class, Health::class)
            .firstOrNull { entityId -> (world.get<Health>(entityId)?.current ?: 0) > 0 }
            ?.let { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }

    internal fun automationPendingObjectiveInteractablePoint(): Point? {
        val objective = currentObjectiveSetSchema() ?: return null
        val state = currentObjectiveStateEntry()?.second ?: return null
        if (state != ObjectiveState.AVAILABLE) {
            return null
        }
        val currentFloorInteractableIds =
            objective.placements
                .asSequence()
                .filter { placement -> placement.floor == currentFloor() }
                .map { placement -> placement.interactableId }
                .toSet()
        if (currentFloorInteractableIds.isEmpty()) {
            return null
        }
        return world.entitiesWith(Position::class, Interactable::class)
            .asSequence()
            .filter { entityId -> world.get<Interactable>(entityId)?.id in currentFloorInteractableIds }
            .mapNotNull { entityId -> world.get<Position>(entityId)?.toPoint() }
            .minWithOrNull(compareBy<Point> { point -> point.chebyshevDistanceTo(playerPosition()) }.thenBy(Point::y).thenBy(Point::x))
    }

    internal fun automationInteractableTags(interactableId: String): Set<String> =
        interactableSchemaFor(interactableId)?.interactionTags?.toSet().orEmpty()

    internal fun automationCanPurchaseShopOffer(index: Int): Boolean =
        availableShopOffers().getOrNull(index)?.let(::canPurchaseShopOffer) == true

    internal fun automationForceDefeatPlayer() {
        handleDeath(playerId, null)
    }

    private fun talentDraft(): TalentAllocationDraft? = world.get(playerId)

    private fun setTalentDraft(draft: TalentAllocationDraft?) {
        if (draft == null) {
            world.remove<TalentAllocationDraft>(playerId)
        } else {
            world.add(playerId, draft)
        }
    }

    private fun storeNormalizedTalentDraft(
        loadout: TalentLoadout,
        draft: TalentAllocationDraft?,
    ) {
        setTalentDraft(TalentAllocationPlanner.normalize(liveRanks = loadout.talentLevels, draft = draft))
    }

    private fun minimumTalentRanks(
        loadout: TalentLoadout,
        owner: TalentTreeOwnerRef? = null,
    ): Map<String, Int> =
        scopedTalentRanks(loadout = loadout, owner = owner).keys.associateWith { 1 }

    private fun effectiveTalentRanks(loadout: TalentLoadout): Map<String, Int> =
        TalentAllocationPlanner.effectiveRanks(liveRanks = loadout.talentLevels, draft = talentDraft())

    private fun talentAllocationPreview(
        loadout: TalentLoadout,
        availablePoints: Int,
        owner: TalentTreeOwnerRef? = talentDraftOwner(),
    ) = TalentAllocationPlanner.preview(
        liveRanks = scopedTalentRanks(loadout = loadout, owner = owner),
        minimumRanks = minimumTalentRanks(loadout, owner),
        availablePoints = availablePoints,
        draft = talentDraft(),
    )

    private fun remainingTalentPoints(
        loadout: TalentLoadout,
        availablePoints: Int,
        owner: TalentTreeOwnerRef? = talentDraftOwner(),
    ): Int = talentAllocationPreview(loadout, availablePoints, owner).remainingPoints

    private fun talentDraftOwner(): TalentTreeOwnerRef? =
        talentDraft()?.let { draft ->
            TalentTreeOwnerRef(
                ownerType = draft.ownerType,
                treeOwnerId = draft.treeOwnerId,
            )
        }

    private fun availableTalentPointsForOwner(
        ownerType: TalentTreeOwnerType,
        experience: Experience = requireNotNull(world.get<Experience>(playerId)),
    ): Int =
        when (ownerType) {
            TalentTreeOwnerType.PROFESSION -> experience.unspentTalentPoints
            TalentTreeOwnerType.RACE -> world.get<RaceTalentPointBank>(playerId)?.unspentPoints ?: 0
        }

    private fun storeRemainingTalentPointsForOwner(
        ownerType: TalentTreeOwnerType,
        remainingPoints: Int,
        experience: Experience = requireNotNull(world.get<Experience>(playerId)),
    ) {
        when (ownerType) {
            TalentTreeOwnerType.PROFESSION -> experience.unspentTalentPoints = remainingPoints
            TalentTreeOwnerType.RACE -> {
                val bank = world.get<RaceTalentPointBank>(playerId) ?: RaceTalentPointBank().also { world.add(playerId, it) }
                bank.unspentPoints = remainingPoints
            }
        }
    }

    private fun canOpenTalentAllocation(): Boolean {
        val loadout = world.get<TalentLoadout>(playerId) ?: return false
        return loadout.talentLevels.isNotEmpty()
    }

    private fun isPlayerInCombat(): Boolean = turnCount <= lastPlayerCombatTurn

    fun playerStatus(): PlayerStatus {
        val health = requireNotNull(world.get<Health>(playerId))
        val experience = requireNotNull(world.get<Experience>(playerId))
        val derivedStats = requireNotNull(world.get<DerivedStats>(playerId))
        val loadout = world.get<TalentLoadout>(playerId)
        val raceTalentBank = world.get<RaceTalentPointBank>(playerId)
        val draftOwner = talentDraftOwner()
        val professionTalentPoints =
            if (loadout != null && draftOwner?.ownerType == TalentTreeOwnerType.PROFESSION) {
                remainingTalentPoints(loadout, experience.unspentTalentPoints, draftOwner)
            } else {
                experience.unspentTalentPoints
            }
        val raceTalentPoints =
            if (loadout != null && draftOwner?.ownerType == TalentTreeOwnerType.RACE) {
                remainingTalentPoints(loadout, raceTalentBank?.unspentPoints ?: 0, draftOwner)
            } else {
                raceTalentBank?.unspentPoints ?: 0
            }
        return PlayerStatus(
            currentHp = health.current,
            maxHp = health.max,
            level = experience.level,
            currentExperience = experience.current,
            nextLevelRequirement = ExperienceSystem.nextLevelExp(experience.level),
            statPoints = experience.unspentStatPoints,
            talentPoints = professionTalentPoints,
            raceTalentPoints = raceTalentPoints,
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
        return activeTalentMappings(loadout).mapNotNull { (slot, talentId) ->
            val details = playerTalentDetails(loadout, talentId, cooldowns) ?: return@mapNotNull null
            TalentSlotView(
                slot = slot,
                talentId = details.talentId,
                name = details.name,
                descKey = details.descKey,
                level = details.level,
                committedLevel = details.committedLevel,
                maxLevel = details.maxLevel,
                resourceCost = details.resourceCost,
                resourceTypeId = details.resourceTypeId,
                range = details.range,
                minRange = details.minRange,
                currentCooldown = details.currentCooldown,
                maxCooldown = details.maxCooldown,
                requiresTarget = details.requiresTarget,
                descriptionModel = details.descriptionModel,
                nextBreakpointPreview = details.nextBreakpointPreview,
                hasPendingAllocation = details.hasPendingAllocation,
            )
        }
    }

    fun reserveTalentSlots(): List<TalentReserveView> {
        val loadout = world.get<TalentLoadout>(playerId) ?: return emptyList()
        val cooldowns = world.get<CooldownState>(playerId)?.remainingByTalentId.orEmpty()
        return reserveTalentIds(loadout).mapNotNull { talentId ->
            val details = playerTalentDetails(loadout, talentId, cooldowns) ?: return@mapNotNull null
            TalentReserveView(
                talentId = details.talentId,
                name = details.name,
                descKey = details.descKey,
                level = details.level,
                committedLevel = details.committedLevel,
                maxLevel = details.maxLevel,
                resourceCost = details.resourceCost,
                resourceTypeId = details.resourceTypeId,
                range = details.range,
                minRange = details.minRange,
                currentCooldown = details.currentCooldown,
                maxCooldown = details.maxCooldown,
                requiresTarget = details.requiresTarget,
                descriptionModel = details.descriptionModel,
                nextBreakpointPreview = details.nextBreakpointPreview,
                hasPendingAllocation = details.hasPendingAllocation,
            )
        }
    }

    private fun activeTalentMappings(loadout: TalentLoadout): List<Pair<Int, String>> =
        (1..PLAYER_ACTIVE_TALENT_SLOT_COUNT).mapNotNull { slot ->
            loadout.talentIdAt(slot)?.let { talentId -> slot to talentId }
        }

    private fun reserveTalentIds(loadout: TalentLoadout): List<String> {
        val activeTalentIds = activeTalentMappings(loadout).mapTo(linkedSetOf()) { (_, talentId) -> talentId }
        return orderedUnlockedTalentIds(loadout).filterNot(activeTalentIds::contains)
    }

    private fun orderedUnlockedTalentIds(loadout: TalentLoadout): List<String> {
        val ordered = linkedSetOf<String>()
        val profession = currentProfessionSchema()
        val experience = world.get<Experience>(playerId)
        if (profession != null && experience != null) {
            TalentProgression.unlockedTalentIds(content.schemaCatalog, profession, experience.level)
                .filterTo(ordered) { talentId -> talentId in loadout.talentLevels }
        }
        loadout.talentLevels.keys.forEach(ordered::add)
        return ordered.toList()
    }

    private fun playerTalentDetails(
        loadout: TalentLoadout,
        talentId: String,
        cooldowns: Map<String, Int>,
    ): TalentUiDetails? {
        val definition = talentRegistry.get(talentId) ?: return null
        val schema = talentSchemaFor(talentId)
        val committedLevel = loadout.levelOf(talentId).coerceIn(1, definition.maxLevel)
        val level = (effectiveTalentRanks(loadout)[talentId] ?: committedLevel).coerceIn(1, definition.maxLevel)
        val schemaResource = schema?.resourceCosts?.firstOrNull()
        val definitionResource = definition.resolvedResourceCosts().entries.firstOrNull()
        val resourceTypeId = schemaResource?.axis ?: definitionResource?.key?.name ?: currentProfessionSchema()?.resourceType ?: ResourceType.STAMINA.name
        val resourceCost = schemaResource?.amount ?: definitionResource?.value ?: 0
        val effectiveRange = definition.range + (definition.levelEffects[level]?.rangeBonus ?: 0)
        val descriptionModel = DynamicDescriptionResolver.resolve(definition, com.ktome.core.talent.DescriptionContext(currentRank = committedLevel, previewRank = level))
        return TalentUiDetails(
            talentId = talentId,
            name = tr(definition.nameKey),
            descKey = definition.descriptionTemplateKey,
            level = level,
            committedLevel = committedLevel,
            maxLevel = definition.maxLevel,
            resourceCost = resourceCost,
            resourceTypeId = resourceTypeId,
            range = effectiveRange,
            minRange = definition.minRange,
            currentCooldown = cooldowns[talentId] ?: 0,
            maxCooldown = definition.cooldown,
            requiresTarget = effectiveRange > 0,
            descriptionModel = descriptionModel,
            nextBreakpointPreview = DynamicDescriptionResolver.nextBreakpointPreview(definition, level)?.toSnapshot(),
            hasPendingAllocation = level != committedLevel,
        )
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
                    zoneDescKey = zone.descKey,
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
        return world.entitiesWith(Position::class, Health::class, MonsterTemplateId::class)
            .flatMap { entityId ->
                val health = requireNotNull(world.get<Health>(entityId))
                if (health.current <= 0) {
                    return@flatMap emptyList()
                }
                val templateId = requireNotNull(world.get<MonsterTemplateId>(entityId)).value
                val position = requireNotNull(world.get<Position>(entityId)).toPoint()
                buildList {
                    if (templateId in content.bossTemplateIds() && position in visibleTiles) {
                        val behavior = world.get<AIBehavior>(entityId)
                        val perceptionRange = monsterPerceptionRange(entityId, behavior)
                        val effectiveTargetId = effectiveTargetIdFor(entityId)
                        val targetVisible = targetVisibleFor(entityId, effectiveTargetId, position, perceptionRange)
                        if (targetVisible) {
                            add(
                                OverlayRenderSnapshot(
                                    id = "boss-warning:${entityId.value}",
                                    visualKey = "vfx.boss.warning.sigil_01",
                                    audioProfile = "audio.boss.warning",
                                    previewTurns = 1,
                                    dangerLevel = DangerLevel.HIGH.overlaySeverity,
                                    shape = OverlayShapeSnapshot.SINGLE_TILE,
                                    sourceAbilityId = bossDefinitionByTemplateId(templateId)?.encounterId ?: "boss.warning.presence",
                                    cells = listOf(GridPointSnapshot(position.x, position.y)),
                                    warningMessage =
                                        RenderTextTokenSnapshot(
                                            "log.warning.boss_presence",
                                            listOf(entityArg("boss", entityId)),
                                        ),
                                ),
                            )
                        }
                    }
                    world.get<PendingTelegraphState>(entityId)?.let { pending ->
                        overlayForPendingTelegraph(entityId, position, pending)?.let(::add)
                    }
                }
            }.sortedBy(OverlayRenderSnapshot::id)
    }

    private fun overlayForPendingTelegraph(
        entityId: EntityId,
        origin: Point,
        pending: PendingTelegraphState,
    ): OverlayRenderSnapshot? {
        if (origin !in visibleTiles) {
            return null
        }
        val telegraphSpec = content.telegraphRegistry.resolve(pending.telegraphSpecId) ?: return null
        val talentSchema = pending.queuedAbilityId?.let(::talentSchemaFor)
        val cells = telegraphCells(origin, pending.targetPoint, talentSchema, telegraphSpec)
        if (cells.isEmpty()) {
            return null
        }
        return OverlayRenderSnapshot(
            id = "telegraph:${entityId.value}:${pending.telegraphSpecId}:${pending.sourceAbilityId}",
            visualKey = "vfx.telegraph.warning.sigil_01",
            audioProfile = talentSchema?.audioProfile ?: "audio.boss.warning",
            previewTurns = pending.remainingTurns,
            dangerLevel = pending.resolvedDangerLevel.overlaySeverity,
            shape = telegraphShape(telegraphSpec.shape),
            sourceAbilityId = pending.sourceAbilityId,
            cells = cells.map { point -> GridPointSnapshot(point.x, point.y) },
            warningMessage =
                if (pending.queuedAbilityId != null) {
                    RenderTextTokenSnapshot(
                        "log.warning.telegraph",
                        listOf(
                            entityArg("boss", entityId),
                            talentArg("talent", pending.sourceAbilityId, fallbackName = pending.sourceAbilityId),
                        ),
                    )
                } else {
                    RenderTextTokenSnapshot(
                        "log.warning.boss_presence",
                        listOf(entityArg("boss", entityId)),
                    )
                },
        )
    }

    private fun bossDefinitionByTemplateId(templateId: String): BossDefinition? =
        content.bossDefinitions.values.firstOrNull { definition -> definition.template.id == templateId }

    private fun activeAiProfileFor(monsterId: EntityId): com.ktome.core.ai.AIProfile? {
        val bossState = world.get<BossEncounterState>(monsterId)
        if (bossState != null) {
            val encounter = bossEncounterFor(monsterId) ?: return null
            val activePhase =
                bossState.currentPhaseId?.let { phaseId ->
                    encounter.phases.firstOrNull { phase -> phase.id == phaseId }
                } ?: encounter.phases.firstOrNull()
            return content.aiProfile(activePhase?.aiProfileId)
        }
        return content.aiProfile(monsterAiProfileId(monsterId))
    }

    private fun bossEncounterFor(monsterId: EntityId): com.ktome.core.ai.BossEncounter? {
        val templateId = world.get<MonsterTemplateId>(monsterId)?.value ?: return null
        return bossDefinitionByTemplateId(templateId)?.encounter
    }

    private fun monsterAiProfileId(monsterId: EntityId): String? {
        val templateId = world.get<MonsterTemplateId>(monsterId)?.value ?: return null
        return content.allMonsterTemplates()
            .firstOrNull { template -> template.id == templateId }
            ?.aiProfileId
    }

    private fun telegraphShape(shape: com.ktome.core.ai.TelegraphShape): OverlayShapeSnapshot =
        when (shape) {
            com.ktome.core.ai.TelegraphShape.LINE -> OverlayShapeSnapshot.LINE
            com.ktome.core.ai.TelegraphShape.CIRCLE -> OverlayShapeSnapshot.RING
            com.ktome.core.ai.TelegraphShape.CONE -> OverlayShapeSnapshot.CONE
        }

    private fun telegraphCells(
        origin: Point,
        target: Point,
        talentSchema: TalentSchemaV2?,
        telegraphSpec: com.ktome.core.ai.TelegraphSpec,
    ): List<Point> =
        when (telegraphSpec.shape) {
            com.ktome.core.ai.TelegraphShape.LINE ->
                lineTowards(
                    origin,
                    target,
                    maxSteps = telegraphSpec.length ?: maxOf(1, talentSchema?.targeting?.range ?: origin.chebyshevDistanceTo(target)),
                )
            com.ktome.core.ai.TelegraphShape.CIRCLE -> {
                val center =
                    when (talentSchema?.targeting?.type) {
                        "SELF",
                        "RADIUS_SELF",
                        null,
                        -> origin
                        else -> target
                    }
                auraCells(center, radius = telegraphSpec.radius ?: talentSchema?.targeting?.areaRadius ?: 0)
            }
            com.ktome.core.ai.TelegraphShape.CONE ->
                coneCells(
                    origin = origin,
                    target = target,
                    length = telegraphSpec.length ?: maxOf(1, talentSchema?.targeting?.range ?: 1),
                    angle = telegraphSpec.angle ?: 90,
                )
        }

    private fun effectiveTargetIdFor(monsterId: EntityId): EntityId =
        StealthTauntHandler.resolveTargetId(
            effectTracker = world.get<EffectTracker>(monsterId),
            fallbackTargetId = playerId,
            isAlive = world::isAlive,
        )

    private fun monsterPerceptionRange(
        monsterId: EntityId,
        behavior: AIBehavior?,
    ): Int = activeAiProfileFor(monsterId)?.perceptionRange ?: behavior?.sightRadius ?: config.fovRadius

    private fun targetVisibleFor(
        monsterId: EntityId,
        targetId: EntityId,
        origin: Point,
        perceptionRange: Int,
    ): Boolean {
        val targetPosition = world.get<Position>(targetId)?.toPoint() ?: return false
        return StealthTauntHandler.isTargetVisible(
            effectTracker = world.get<EffectTracker>(targetId),
            targetPosition = targetPosition,
            visibleTiles = Shadowcasting.computeVisible(map = map, origin = origin, radius = perceptionRange),
        )
    }

    private fun resolveTalentTreeOwner(talentSchema: TalentSchemaV2): TalentTreeOwnerRef? =
        talentTreeOwnerResolver.ownerForTalent(talentSchema)

    private fun scopedTalentRanks(
        loadout: TalentLoadout,
        owner: TalentTreeOwnerRef? = null,
    ): Map<String, Int> =
        if (owner == null) {
            loadout.talentLevels.toMap(linkedMapOf())
        } else {
            loadout.talentLevels
                .filterKeys { talentId ->
                    talentSchemaFor(talentId)
                        ?.let(::resolveTalentTreeOwner) == owner
                }.toMap(linkedMapOf())
        }

    private fun ensureDraftOwner(
        existingDraft: TalentAllocationDraft?,
        owner: TalentTreeOwnerRef,
    ): Boolean =
        existingDraft == null ||
            (existingDraft.ownerType == owner.ownerType && existingDraft.treeOwnerId == owner.treeOwnerId)

    private fun applyTalentDraft(
        loadout: TalentLoadout,
        currentDraft: TalentAllocationDraft?,
        owner: TalentTreeOwnerRef,
        talentId: String,
        nextLevel: Int,
    ): TalentAllocationDraft =
        TalentAllocationPlanner.applyRankIncrease(
            draft = currentDraft,
            ownerType = owner.ownerType,
            treeOwnerId = owner.treeOwnerId,
            talentId = talentId,
            nextRank = nextLevel,
        )

    private fun rejectTalentOwnerConflict(messageKey: String): CommandResolution {
        addMessage(messageKey)
        return CommandResolution.rejected()
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
        val stepCount = maxOf(1, maxSteps)
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

    private fun coneCells(
        origin: Point,
        target: Point,
        length: Int,
        angle: Int,
    ): List<Point> {
        val dx = (target.x - origin.x).coerceIn(-1, 1)
        val dy = (target.y - origin.y).coerceIn(-1, 1)
        if (dx == 0 && dy == 0) {
            return listOf(origin)
        }
        val normalizedAngle = angle.coerceAtLeast(30)
        val lateralReach = maxOf(1, (normalizedAngle / 45))
        return buildList {
            for (step in 1..maxOf(1, length)) {
                val center = Point(origin.x + dx * step, origin.y + dy * step)
                for (offset in -lateralReach..lateralReach) {
                    val point =
                        if (dx != 0 && dy != 0) {
                            Point(center.x + offset, center.y - offset)
                        } else if (dx != 0) {
                            Point(center.x, center.y + offset)
                        } else {
                            Point(center.x + offset, center.y)
                        }
                    if (map.isInBounds(point.x, point.y)) {
                        add(point)
                    }
                }
            }
        }.distinct()
    }

    private val DangerLevel.overlaySeverity: Int
        get() =
            when (this) {
                DangerLevel.LOW -> 1
                DangerLevel.MODERATE -> 2
                DangerLevel.HIGH -> 3
                DangerLevel.LETHAL -> 4
            }

    private fun AIType.toDefaultBehavior(): AIDefaultBehavior =
        when (this) {
            AIType.CHASE -> AIDefaultBehavior.CHASE
            AIType.KITE -> AIDefaultBehavior.KITE
            AIType.PATROL -> AIDefaultBehavior.PATROL
        }

    private fun buildRenderUiState(): RenderUiStateSnapshot =
        RenderUiStateSnapshot(
            playerStatus = buildPlayerStatusSnapshot(),
            equipment = buildEquipmentSnapshots(),
            talents = buildTalentSnapshots(),
            reserveTalents = buildReserveTalentSnapshots(),
            inscriptions = buildInscriptionSnapshots(),
            inventory = buildInventoryEntries(),
            targetablePositions = targetableHostilePositions().map { point -> GridPointSnapshot(point.x, point.y) },
            shardBalance = shardBalance,
            activeShop = buildShopPanelSnapshot(),
            activeRouteSelection = buildRouteSelectionSnapshot(),
        )

    private fun buildShopPanelSnapshot(): ShopPanelSnapshot? {
        val shop = currentShopNode() ?: return null
        if (activeShopId == null) {
            return null
        }
        return ShopPanelSnapshot(
            shopId = shop.id,
            shopNameKey = shop.nameKey,
            offers =
                availableShopOffers().mapIndexed { index, offer ->
                    ShopOfferSnapshot(
                        index = index,
                        labelKey = shopOfferLabelKey(offer),
                        price = offer.price,
                        tags = offer.tags.sorted(),
                    )
                },
            sellEntries =
                inventoryItems()
                    .map { itemView ->
                        ShopSellEntrySnapshot(
                            inventoryIndex = itemView.index,
                            price = sellValueForInventoryIndex(itemView.index),
                        )
                    },
        )
    }

    private fun buildRouteSelectionSnapshot(): RouteSelectionSnapshot? {
        if (pendingRouteSelection.isEmpty()) {
            return null
        }
        return RouteSelectionSnapshot(
            currentZoneNameKey = currentZoneSchema().nameKey,
            options =
                pendingRouteSelection.mapIndexed { index, option ->
                    val destinationZone = zoneSchemaFor(option.destinationZoneId)
                    RouteOptionSnapshot(
                        index = index,
                        routeId = option.connection.id,
                        destinationZoneId = destinationZone.id,
                        destinationZoneNameKey = destinationZone.nameKey,
                        destinationZoneDescKey = destinationZone.descKey,
                        recommendedLevelMin = destinationZone.recommendedLevel.min,
                        recommendedLevelMax = destinationZone.recommendedLevel.max,
                        shardReward = option.reward?.shardReward ?: 0,
                        rewardItemNameKeys =
                            option.reward
                                ?.guaranteedUtilityDropIds
                                ?.mapNotNull { baseId -> itemSchemaFor(baseId)?.nameKey }
                                .orEmpty(),
                        rescueHintLabelKeys = routeRescueHintLabelKeys(option.reward?.rescueTags.orEmpty()),
                        mechanicHintKey = ZoneMechanicRuntime.introHintKey(destinationZone),
                        isReturnPath = option.destinationZoneId == config.zoneRoute.getOrNull(config.routeIndex - 1),
                    )
                },
        )
    }

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
            resourceStableMin = resource.stableMin,
            resourceStableMax = resource.stableMax,
            secondaryResourceCurrent = resource.secondary?.current,
            secondaryResourceMax = resource.secondary?.max,
            secondaryResourceLabelKey = resource.secondary?.typeId?.let(::resourceLabelKey),
            secondaryResourceTypeId = resource.secondary?.typeId,
            secondaryResourceStableMin = resource.secondary?.stableMin,
            secondaryResourceStableMax = resource.secondary?.stableMax,
            level = status.level,
            currentExperience = status.currentExperience,
            nextLevelRequirement = status.nextLevelRequirement,
            statPoints = status.statPoints,
            talentPoints = status.talentPoints,
            raceTalentPoints = status.raceTalentPoints,
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
            val owner = requireNotNull(resolveTalentTreeOwner(schema)) { "Unknown talent tree owner for '${slot.talentId}'." }
            val resourceTypeId = schema.resourceCosts.firstOrNull()?.axis ?: resolvePlayerResourceView().typeId
            TalentSlotSnapshot(
                slot = slot.slot,
                talentId = slot.talentId,
                ownerType = owner.ownerType.name,
                treeOwnerId = owner.treeOwnerId,
                nameKey = schema.nameKey,
                visualKey = schema.visualKey,
                iconKey = schema.iconKey,
                damageTypeIconKey = schema.damageType?.let(::damageTypeIconKey),
                audioProfile = schema.audioProfile,
                level = slot.level,
                maxLevel = slot.maxLevel,
                resourceCost = schema.resourceCosts.firstOrNull { cost -> cost.axis == resourceTypeId }?.amount ?: slot.resourceCost,
                resourceLabelKey = resourceLabelKey(resourceTypeId),
                resourceTypeId = resourceTypeId,
                range = slot.range,
                minRange = slot.minRange,
                currentCooldown = slot.currentCooldown,
                maxCooldown = slot.maxCooldown,
                requiresTarget = slot.requiresTarget,
                descKey = slot.descKey,
                committedLevel = slot.committedLevel,
                descriptionModel = slot.descriptionModel?.toSnapshot(),
                nextBreakpointPreview = slot.nextBreakpointPreview,
                isMaxRank = slot.level >= slot.maxLevel,
                hasPendingAllocation = slot.hasPendingAllocation,
            )
        }

    private fun buildReserveTalentSnapshots(): List<TalentReserveSnapshot> =
        reserveTalentSlots().map { talent ->
            val schema = requireNotNull(content.schemaCatalog.talents.firstOrNull { it.id == talent.talentId }) {
                "Unknown talent schema '${talent.talentId}'."
            }
            val owner = requireNotNull(resolveTalentTreeOwner(schema)) { "Unknown talent tree owner for '${talent.talentId}'." }
            val resourceTypeId = schema.resourceCosts.firstOrNull()?.axis ?: resolvePlayerResourceView().typeId
            TalentReserveSnapshot(
                talentId = talent.talentId,
                ownerType = owner.ownerType.name,
                treeOwnerId = owner.treeOwnerId,
                nameKey = schema.nameKey,
                visualKey = schema.visualKey,
                iconKey = schema.iconKey,
                damageTypeIconKey = schema.damageType?.let(::damageTypeIconKey),
                audioProfile = schema.audioProfile,
                level = talent.level,
                maxLevel = talent.maxLevel,
                resourceCost = schema.resourceCosts.firstOrNull { cost -> cost.axis == resourceTypeId }?.amount ?: talent.resourceCost,
                resourceLabelKey = resourceLabelKey(resourceTypeId),
                resourceTypeId = resourceTypeId,
                range = talent.range,
                minRange = talent.minRange,
                currentCooldown = talent.currentCooldown,
                maxCooldown = talent.maxCooldown,
                requiresTarget = talent.requiresTarget,
                descKey = talent.descKey,
                committedLevel = talent.committedLevel,
                descriptionModel = talent.descriptionModel?.toSnapshot(),
                nextBreakpointPreview = talent.nextBreakpointPreview,
                isMaxRank = talent.level >= talent.maxLevel,
                hasPendingAllocation = talent.hasPendingAllocation,
            )
        }

    private fun buildInscriptionSnapshots(): List<InscriptionSlotSnapshot> {
        val loadout = world.get<InscriptionLoadout>(playerId) ?: return emptyList()
        val cooldowns = world.get<InscriptionCooldownState>(playerId)
        val defsById = content.inscriptions.associateBy(InscriptionDef::id)
        return loadout.slots.mapNotNull { slot ->
            val definition = defsById[slot.inscriptionId] ?: return@mapNotNull null
            InscriptionSlotSnapshot(
                hotkey = slot.hotkey,
                inscriptionId = definition.id,
                nameKey = definition.nameKey,
                descKey = definition.descKey,
                iconKey = definition.iconKey,
                categoryId = definition.category.name,
                cooldownRemaining = cooldowns?.remainingByInscriptionId?.get(definition.id) ?: 0,
                maxCooldown = definition.cooldown,
                requiresTarget = (definition.effect as? InscriptionEffect.Teleport)?.controlled == true,
            )
        }
    }

    private fun DescriptionModel.toSnapshot(): DescriptionModelSnapshot =
        DescriptionModelSnapshot(
            templateKey = templateKey,
            placeholders =
                placeholders.mapValues { (name, value) ->
                    when (value) {
                        is DescriptionValue.BooleanValue -> DescriptionValueSnapshot.BooleanValue(value.value)
                        is DescriptionValue.DecimalValue -> DescriptionValueSnapshot.DecimalValue(value.value)
                        is DescriptionValue.IntValue -> DescriptionValueSnapshot.IntValue(value.value)
                        is DescriptionValue.TextValue ->
                            if (name == "statusId") {
                                DescriptionValueSnapshot.StatusValue(
                                    statusId = value.value,
                                    nameKey = statusNameKey(value.value),
                                )
                            } else {
                                DescriptionValueSnapshot.TextValue(value.value)
                            }
                    }
                },
            keywords = keywords,
        )

    private fun com.ktome.core.talent.TalentBreakpointPreview.toSnapshot(): TalentBreakpointPreviewSnapshot =
        TalentBreakpointPreviewSnapshot(
            atRank = atRank,
            descriptionAddendumKey = descriptionAddendumKey,
            model = model.toSnapshot(),
        )

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

    private fun zoneSchemaFor(zoneId: String): ZoneSchemaV2 =
        requireNotNull(content.schemaCatalog.zones.firstOrNull { zone -> zone.id == zoneId }) {
            "Unknown zone '$zoneId'."
        }

    private fun currentProfessionSchema(): ProfessionSchemaV2? =
        content.schemaCatalog.professions.firstOrNull { profession -> profession.id == config.playerProfessionId }

    private fun currentRaceSchema() =
        content.schemaCatalog.races.firstOrNull { race -> race.id == config.playerRaceId }

    private fun currentShopNode(): ShopNode? =
        activeShopId?.let { shopId ->
            content.schemaCatalog.shopNodes.firstOrNull { shop -> shop.id == shopId }
        }

    private fun configuredShopNode(): ShopNode? =
        currentZoneSchema().shopNodeId?.let { shopId ->
            content.schemaCatalog.shopNodes.firstOrNull { shop -> shop.id == shopId }
        }

    private fun currentObjectiveSetSchema() =
        content.schemaCatalog.objectiveSets.firstOrNull { objectiveSet -> objectiveSet.id == currentZoneSchema().objectiveSetId }

    private fun currentQuestProgressTemplate(): QuestProgress? =
        currentObjectiveSetSchema()?.linkedQuestId?.let { questId ->
            worldProgress.questStates[questId]
                ?: content.schemaCatalog.questProgressions.firstOrNull { quest -> quest.questId == questId }
        }

    private fun currentObjectiveCompletionRule(): ObjectiveCompletionRule? =
        currentObjectiveSetSchema()?.let { objective -> ObjectiveCompletionRule.fromSchemaId(objective.completionRule) }

    private fun currentQuestObjectiveId(): String? = currentObjectiveSetSchema()?.questObjectiveId

    private fun currentObjectiveStateEntry(): Pair<QuestProgress, ObjectiveState>? {
        val objective = currentObjectiveSetSchema() ?: return null
        val quest = currentQuestProgressTemplate() ?: return null
        val objectiveId = requireNotNull(objective.questObjectiveId) {
            "Objective set '${objective.id}' must define questObjectiveId when linkedQuestId is present."
        }
        val currentState = requireNotNull(quest.objectiveStates[objectiveId]) {
            "Quest '${quest.questId}' is missing objective '$objectiveId' for objective set '${objective.id}'."
        }
        return quest to currentState
    }

    private fun updateCurrentObjectiveState(transform: (ObjectiveState) -> ObjectiveState): ObjectiveState? {
        val objective = currentObjectiveSetSchema() ?: return null
        val quest = currentQuestProgressTemplate() ?: return null
        val objectiveId = requireNotNull(objective.questObjectiveId) {
            "Objective set '${objective.id}' must define questObjectiveId when linkedQuestId is present."
        }
        val currentState = requireNotNull(quest.objectiveStates[objectiveId]) {
            "Quest '${quest.questId}' is missing objective '$objectiveId' for objective set '${objective.id}'."
        }
        val nextState = transform(currentState)
        if (nextState == currentState) {
            return currentState
        }
        val updatedQuest =
            quest.copy(
                objectiveStates = quest.objectiveStates + (objectiveId to nextState),
            )
        worldProgress = worldProgress.withQuestProgress(quest.questId, updatedQuest)
        return nextState
    }

    private fun completeCurrentZoneQuest(trigger: ObjectiveCompletionTrigger): Boolean {
        val objective = currentObjectiveSetSchema() ?: return false
        val rule = currentObjectiveCompletionRule() ?: return false
        val floorReached = currentFloor()
        val maxFloor = config.maxFloor
        val objectiveStateEntry = currentObjectiveStateEntry() ?: return false
        val currentState = objectiveStateEntry.second
        val satisfied =
            ObjectiveRuntimeEvaluator.isSatisfied(
                rule = rule,
                currentState = currentState,
                trigger = trigger,
                floorReached = floorReached,
                maxFloor = maxFloor,
            )
        if (!satisfied) {
            return false
        }
        val (quest, state) = objectiveStateEntry
        if (state == ObjectiveState.COMPLETED) {
            return false
        }
        val objectiveId = requireNotNull(objective.questObjectiveId)
        val completedQuest =
            quest.copy(
                objectiveStates = quest.objectiveStates + (objectiveId to ObjectiveState.COMPLETED),
            )
        val progressedWorld = worldProgress.withQuestProgress(quest.questId, completedQuest)
        worldProgress =
            if (progressedWorld.isQuestCompleted(quest.questId)) {
                progressedWorld.copy(
                    worldFlags = progressedWorld.worldFlags + completedQuest.completionFlags,
                )
            } else {
                progressedWorld
            }
        return true
    }

    private fun plannedNextZoneId(): String? = config.zoneRoute.getOrNull(config.routeIndex + 1)

    private fun activeShopState(): ShopInventoryState? =
        currentShopNode()?.let { shop -> shopStates[shop.id] }

    private fun availableShopOffers(): List<ShopOffer> {
        val shop = currentShopNode() ?: return emptyList()
        val purchasedIds = shopStates[shop.id]?.purchasedOfferIds.orEmpty()
        return shop.inventory.filterNot { offer -> offer.id in purchasedIds }
    }

    private fun routeRewardFor(routeId: String): RouteReward? =
        content.schemaCatalog.routeRewards.firstOrNull { reward -> reward.routeId == routeId }

    private fun plannedRouteAdvanceOption(): RouteAdvanceOption? {
        val nextZoneId = plannedNextZoneId() ?: return null
        return content.schemaCatalog.worldGraph.outgoingConnections(config.zoneId)
            .firstOrNull { connection ->
                content.schemaCatalog.worldGraph.destinationFor(config.zoneId, connection) == nextZoneId
            }?.let { connection ->
                RouteAdvanceOption(
                    connection = connection,
                    destinationZoneId = nextZoneId,
                    reward = routeRewardFor(connection.id),
                )
            }
    }

    private fun availableRouteAdvanceOptions(): List<RouteAdvanceOption> {
        val immediatePreviousZoneId = config.zoneRoute.getOrNull(config.routeIndex - 1)
        return content.schemaCatalog.worldGraph.outgoingConnections(config.zoneId)
            .map { connection ->
                RouteAdvanceOption(
                    connection = connection,
                    destinationZoneId = content.schemaCatalog.worldGraph.destinationFor(config.zoneId, connection),
                    reward = routeRewardFor(connection.id),
                )
            }.filter { option ->
                worldProgress.satisfies(option.connection.gate)
            }.filter { option ->
                option.destinationZoneId != config.zoneId
            }.sortedWith(
                compareBy<RouteAdvanceOption> { option -> if (option.destinationZoneId == immediatePreviousZoneId) 1 else 0 }
                    .thenBy { option -> zoneSchemaFor(option.destinationZoneId).recommendedLevel.min }
                    .thenBy { option -> option.destinationZoneId },
            )
    }

    private fun claimRouteReward(
        option: RouteAdvanceOption,
        claimPolicy: RewardClaimPolicy,
        dropPoint: Point?,
    ) {
        val reward = option.reward ?: return
        if (reward.claimPolicy != claimPolicy || reward.routeId in worldProgress.claimedRouteRewards) {
            return
        }
        val guaranteedRewards =
            reward.guaranteedUtilityDropIds.mapNotNull { baseId ->
                itemBaseDef(baseId)?.toRuntimeItem()
            }
        val reservedSlots = guaranteedRewards.mapNotNull(ItemInstance::slot).toSet()
        val milestoneReward =
            routeMilestoneRewardItem(
                reward = reward,
                reservedSlots = reservedSlots,
                occupiedSlots = currentEquippedSlots(),
            )
        val hasSufficientInventoryCapacity =
            dropPoint == null ||
                hasInventoryCapacityFor(guaranteedRewards.size + if (milestoneReward != null) 1 else 0)
        if (!hasSufficientInventoryCapacity) {
            return
        }
        grantShards(reward.shardReward)
        dropPoint?.let { point ->
            guaranteedRewards.forEach { rewardItem ->
                grantRewardItem(rewardItem, point)
            }
            milestoneReward?.let { rewardItem ->
                grantRewardItem(rewardItem, point)
                recordMilestoneReward(
                    rewardSource = MilestoneRewardSource.ROUTE,
                    sourceId = reward.routeId,
                    reward = rewardItem,
                )
            }
        }
        worldProgress = worldProgress.withClaimedRouteReward(reward.routeId)
    }

    private fun hasInventoryCapacityFor(itemCount: Int): Boolean {
        if (itemCount <= 0) {
            return true
        }
        val inventory = requireNotNull(world.get<Inventory>(playerId)) { "Missing Inventory for $playerId" }
        val availableSlots = inventory.capacity - inventory.itemIds.size
        return availableSlots >= itemCount
    }

    private fun shopOfferLabelKey(offer: ShopOffer): String =
        when {
            offer.itemBaseId != null -> {
                val itemBaseId = requireNotNull(offer.itemBaseId)
                requireNotNull(itemSchemaFor(itemBaseId)) { "Unknown shop item '$itemBaseId'." }.nameKey
            }
            offer.inscriptionId != null ->
                requireNotNull(content.inscriptions.firstOrNull { inscription -> inscription.id == offer.inscriptionId }) {
                    "Unknown shop inscription '${offer.inscriptionId}'."
                }.nameKey
            else -> error("Shop offer '${offer.id}' is missing both itemBaseId and inscriptionId.")
        }

    private fun activeBossEncounterSchema() =
        activeBossDefinition()?.encounterId?.let { encounterId ->
            content.schemaCatalog.bossEncounters.firstOrNull { schema -> schema.id == encounterId }
        }

    private fun zoneNameKeyFor(zoneId: String): String? =
        content.schemaCatalog.zones.firstOrNull { zone -> zone.id == zoneId }?.nameKey

    private fun bossNameKeyFor(templateId: String): String? =
        bossDefinitionByTemplateId(templateId)?.nameKey
            ?: content.schemaCatalog.monsters.firstOrNull { schema -> schema.id == templateId }?.nameKey

    private fun defeatedBossNameKeys(): List<String> {
        val defeatedBossIds = worldProgress.defeatedBossIds
        return linkedSetOf<String>().apply {
            config.zoneRoute.forEach { zoneId ->
                content
                    .bossDefinitionForZone(zoneId)
                    ?.takeIf { definition -> definition.template.id in defeatedBossIds }
                    ?.nameKey
                    ?.let(::add)
            }
            defeatedBossIds.sorted().forEach { bossId ->
                bossNameKeyFor(bossId)?.let(::add)
            }
        }.toList()
    }

    private fun claimedRouteRewardNameKeys(): List<String> {
        val routeRewardNameKeys =
            worldProgress.claimedRouteRewards
                .sorted()
                .flatMap { routeId -> routeRewardFor(routeId)?.guaranteedUtilityDropIds.orEmpty() }
                .mapNotNull { baseItemId -> itemSchemaFor(baseItemId) }
                .map { schema -> schema.nameKey }
        val routeMilestoneNameKeys =
            milestoneRewardSummaries()
                .asSequence()
                .filter { reward -> reward.rewardSource == MilestoneRewardSource.ROUTE }
                .mapNotNull { reward -> itemSchemaFor(reward.baseItemId)?.nameKey }
                .toList()
        return (routeRewardNameKeys + routeMilestoneNameKeys).distinct()
    }

    private fun outcomeProgressStageNameKey(): String {
        val zone = currentZoneSchema()
        return when {
            zone.id == "abyssal_temple" || zone.id == "abyssal_heart" -> "ui.summary.stage.finale"
            zone.recommendedLevel.max <= 6 -> "ui.summary.stage.early"
            zone.recommendedLevel.max <= 10 -> "ui.summary.stage.mid"
            else -> "ui.summary.stage.late"
        }
    }

    private fun outcomeFailureSummaryKey(progressStageNameKey: String): String? =
        when (runOutcome) {
            is RunOutcome.Defeat ->
                when (progressStageNameKey) {
                    "ui.summary.stage.early" -> "ui.summary.failure_recap.early"
                    "ui.summary.stage.mid" -> "ui.summary.failure_recap.mid"
                    "ui.summary.stage.late" -> "ui.summary.failure_recap.late"
                    else -> "ui.summary.failure_recap.finale"
                }

            else -> null
        }

    private fun interactableSchemaFor(interactableId: String) =
        content.schemaCatalog.interactables.firstOrNull { interactable -> interactable.id == interactableId }

    private fun lootProfile(profileId: String) =
        content.schemaCatalog.lootProfiles.firstOrNull { profile -> profile.id == profileId }

    private fun resolvePlayerResourceView(): PlayerResourceView {
        val schema = currentProfessionSchema()
        val primaryResourceType =
            schema?.primarySpendAxis?.asResourceTypeOrNull()
                ?: ResourceType.STAMINA
        val primaryProfile =
            schema?.let { profession ->
                profession.resourceProfile(profession.primarySpendAxis)
            }
        val secondaryResourceType =
            schema?.stateAxis
                ?.asResourceTypeOrNull()
                ?.takeIf { type -> type != primaryResourceType }
        val secondaryProfile =
            schema?.let { profession ->
                profession.stateAxis
                    ?.takeIf { axis -> axis != profession.primarySpendAxis }
                    ?.let(profession::resourceProfile)
            }
        val pools =
            if (schema != null) {
                PlayerResourceService.sync(world, playerId, schema)
            } else {
                requireNotNull(world.get<com.ktome.core.resource.ResourcePools>(playerId)) {
                    "Missing ResourcePools for '$playerId'."
                }
            }
        val pool =
            requireNotNull(pools.pool(primaryResourceType)) {
                "Missing resource pool '${primaryResourceType.name}' for '$playerId'."
            }
        return PlayerResourceView(
            current = pool.current,
            max = pool.max,
            typeId = primaryResourceType.name,
            stableMin = primaryProfile?.stableMin,
            stableMax = primaryProfile?.stableMax,
            secondary =
                secondaryResourceType
                    ?.let(pools::pool)
                    ?.let { secondary ->
                        SecondaryPlayerResourceView(
                            current = secondary.current,
                            max = secondary.max,
                            typeId = secondary.type.name,
                            stableMin = secondaryProfile?.stableMin,
                            stableMax = secondaryProfile?.stableMax,
                        )
                    },
        )
    }

    private fun resourceLabelKey(resourceTypeId: String): String =
        when (resourceTypeId) {
            "MANA" -> "ui.hud.mana.short"
            "ENERGY" -> "ui.hud.energy.short"
            "POSITIVE_ENERGY" -> "ui.hud.positive_energy.short"
            "HATE" -> "ui.hud.hate.short"
            "EQUILIBRIUM" -> "ui.hud.equilibrium.short"
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

    private fun milestoneRewardItemFromProfiles(
        profileIds: List<String>,
        fallbackBaseId: String,
        rewardContext: RewardGenerationContext,
    ): ItemInstance {
        val candidateIds =
            profileIds
                .flatMap { profileId -> lootProfile(profileId)?.itemIds.orEmpty() }
                .distinct()
        val effectiveFallbackBaseId = normalizeMilestoneFallbackBaseId(fallbackBaseId, rewardContext)
        val selectedBaseId =
            rankMilestoneRewardCandidateIds(candidateIds, rewardContext).firstOrNull()
                ?: effectiveFallbackBaseId
        val base =
            itemBaseDef(selectedBaseId)
                ?: requireNotNull(itemBaseDef(effectiveFallbackBaseId)) {
                    "Missing fallback milestone reward item '$effectiveFallbackBaseId'."
                }
        return ItemGenerator(content.itemBundle, sessionRandom).generate(
            base = base,
            floor = rewardContext.floor,
            affixContext = milestoneAffixContext(rewardContext),
        )
    }

    private fun currentOwnedItemBaseIds(): Set<String> {
        val inventory = world.get<Inventory>(playerId) ?: return emptySet()
        return inventory.itemIds
            .mapNotNull { itemId -> world.get<ItemInstance>(itemId)?.baseId }
            .toSet()
    }

    private fun currentEquippedSlots(): Set<EquipSlot> =
        world.get<Equipment>(playerId)?.slots?.keys?.toSet() ?: emptySet()

    private fun equippedBaseItemIdFor(slot: EquipSlot): String? = equippedItemFor(slot)?.baseId

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

    private fun rankMilestoneRewardCandidateIds(
        candidateIds: List<String>,
        rewardContext: RewardGenerationContext,
    ): List<String> {
        val strictCandidates = strictMilestoneRewardCandidateIds(candidateIds, rewardContext)
        if (strictCandidates.isNotEmpty() || rewardContext.replacementSlot != null) {
            return strictCandidates
        }
        val replacementSlot = selectMilestoneReplacementSlot(candidateIds, rewardContext) ?: return emptyList()
        return strictMilestoneRewardCandidateIds(candidateIds, rewardContext.copy(replacementSlot = replacementSlot))
    }

    private fun strictMilestoneRewardCandidateIds(
        candidateIds: List<String>,
        rewardContext: RewardGenerationContext,
    ): List<String> =
        candidateIds
            .distinct()
            .filter { baseItemId -> isMilestoneRewardBaseAllowed(baseItemId, rewardContext) }
            .sortedWith(
                compareByDescending<String> { baseItemId -> milestoneRewardBaseScore(baseItemId, rewardContext) }
                    .thenBy { baseItemId -> rewardPreferenceOrder().indexOf(baseItemId).takeIf { it >= 0 } ?: Int.MAX_VALUE }
                    .thenByDescending { baseItemId -> itemBaseDef(baseItemId)?.dropWeight ?: 0 }
                    .thenBy { it },
            )

    private fun selectMilestoneReplacementSlot(
        candidateIds: List<String>,
        rewardContext: RewardGenerationContext,
    ): EquipSlot? =
        MILESTONE_REPLACEMENT_SLOT_PRIORITY.firstOrNull { candidateSlot ->
            candidateSlot in rewardContext.occupiedSlots &&
                candidateSlot !in rewardContext.reservedSlots &&
                strictMilestoneRewardCandidateIds(
                    candidateIds = candidateIds,
                    rewardContext = rewardContext.copy(replacementSlot = candidateSlot),
                ).isNotEmpty()
        }

    private fun isMilestoneRewardBaseAllowed(
        baseItemId: String,
        rewardContext: RewardGenerationContext,
    ): Boolean {
        val base = itemBaseDef(baseItemId) ?: return false
        val slot = base.slot ?: return false
        if (base.type == ItemType.CONSUMABLE || base.id in DETERMINISTIC_RESCUE_UTILITY_BASE_IDS) {
            return false
        }
        if (!isRewardSuitableForCurrentProfession(baseItemId)) {
            return false
        }
        if (slot in rewardContext.reservedSlots) {
            return false
        }
        return slot !in rewardContext.occupiedSlots || slot == rewardContext.replacementSlot
    }

    private fun milestoneRewardBaseScore(
        baseItemId: String,
        rewardContext: RewardGenerationContext,
    ): Int {
        val base = itemBaseDef(baseItemId) ?: return Int.MIN_VALUE
        val baseTags = rewardBaseTags(base)
        val buildContext = currentAffixBuildContext()
        val freshBonus = if (baseItemId !in currentOwnedItemBaseIds()) 120 else 0
        val buildMatchScore = baseTags.count(buildContext.buildTags::contains) * 10
        val routeBiasScore = baseTags.count(rewardContext.routeBiasTags::contains) * 6
        val rewardBiasScore = baseTags.count(rewardSourceBiasTags(rewardContext.rewardSource)::contains) * 3
        return freshBonus + buildMatchScore + routeBiasScore + rewardBiasScore + (base.dropWeight.coerceAtLeast(1))
    }

    private fun rewardBaseTags(base: ItemBaseDef): Set<String> =
        linkedSetOf<String>().apply {
            addAll(base.tags.map(String::lowercase))
            add(base.type.name.lowercase())
            base.slot?.name?.lowercase()?.let(::add)
            base.resourceTypeId?.lowercase()?.let(::add)
            if (base.baseStats.attack > 0) {
                add("offense")
            }
            if (base.baseStats.defense > 0 || base.baseStats.maxHp > 0) {
                addAll(listOf("protection", "defense"))
            }
            if (base.baseStats.evasion > 0 || base.baseStats.speed > 0) {
                add("mobility")
            }
            if (base.baseStats.talentPower > 0.0 || base.baseStats.wil > 0) {
                add("spell")
            }
            when (val passive = base.passive) {
                is EquipmentPassive.DamageTypeBonus -> {
                    add(passive.type.name.lowercase())
                    add("offense")
                }
                is EquipmentPassive.ResistanceBonus -> {
                    add(passive.damageType.name.lowercase())
                    addAll(listOf("protection", "resistance"))
                }
                is EquipmentPassive.HpRegenPerTurn -> addAll(listOf("sustain", "life", "regeneration"))
                is EquipmentPassive.DamageVsTag -> add("offense")
                null -> Unit
            }
        }

    private fun normalizeMilestoneFallbackBaseId(
        fallbackBaseId: String,
        rewardContext: RewardGenerationContext,
    ): String =
        selectMilestoneFallbackBaseId(
            preferredBaseIds = listOf(fallbackBaseId) + rewardPreferenceOrder(),
            rewardContext = rewardContext,
        )

    private fun defaultMilestoneFallbackBaseId(rewardContext: RewardGenerationContext): String =
        selectMilestoneFallbackBaseId(
            preferredBaseIds = rewardPreferenceOrder(),
            rewardContext = rewardContext,
        )

    private fun selectMilestoneFallbackBaseId(
        preferredBaseIds: List<String>,
        rewardContext: RewardGenerationContext,
    ): String {
        rankMilestoneRewardCandidateIds(preferredBaseIds, rewardContext).firstOrNull()?.let { fallbackBaseId ->
            return fallbackBaseId
        }
        return requireNotNull(
            rankMilestoneRewardCandidateIds(
                candidateIds = content.itemBundle.baseItems.map(ItemBaseDef::id),
                rewardContext = rewardContext,
            ).firstOrNull(),
        ) {
            "No legal milestone reward base available for ${rewardContext.rewardSource}:${rewardContext.sourceId}."
        }
    }

    private fun milestoneAffixContext(rewardContext: RewardGenerationContext): AffixSelectionContext {
        val buildContext = currentAffixBuildContext()
        return buildContext.copy(
            rewardSource = rewardContext.rewardSource,
            qualityFloor = rewardContext.qualityFloor,
            minAffixCount = rewardContext.minAffixCount,
            routeBiasTags = rewardContext.routeBiasTags,
        )
    }

    private fun rewardPreferenceOrder(): List<String> =
        when (config.playerProfessionId) {
            "vanguard" -> listOf("abyssal_heartstone", "forgebreaker_pick", "basic_shield", "chain_mail", "war_maul", "healing_potion", "scroll_teleport")
            "arcanist" -> listOf("abyssal_heartstone", "seal_reliquary", "emerald_charm", "mana_potion", "apprentice_robe", "scroll_teleport", "healing_potion")
            "rogue" -> listOf("abyssal_heartstone", "bandit_trophy", "hunter_bow", "leather_armor", "energy_tonic", "scroll_teleport", "healing_potion")
            "templar" -> listOf("abyssal_heartstone", "sanctified_seal", "long_sword", "basic_shield", "chain_mail", "consecrated_oil", "healing_potion")
            else -> listOf("healing_potion", "scroll_teleport")
        }

    private fun rewardSourceBiasTags(source: MilestoneRewardSource): Set<String> =
        when (source) {
            MilestoneRewardSource.ROUTE -> setOf("reward", "route")
            MilestoneRewardSource.BOSS -> setOf("reward", "boss", "elite")
            MilestoneRewardSource.CACHE -> setOf("reward", "cache")
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

    private fun zoneRewardItem(
        profileIds: List<String>,
        fallbackBaseId: String,
        rewardContext: RewardGenerationContext,
    ): ItemInstance =
        milestoneRewardItemFromProfiles(
            profileIds = profileIds,
            fallbackBaseId = fallbackBaseId,
            rewardContext = rewardContext,
        )

    private fun activeBossRewardItem(): ItemInstance =
        milestoneRewardItemFromProfiles(
            profileIds = activeBossEncounterSchema()?.rewards.orEmpty(),
            fallbackBaseId = defaultMilestoneFallbackBaseId(
                RewardGenerationContext(
                    rewardSource = MilestoneRewardSource.BOSS,
                    sourceId = activeBossEncounterSchema()?.id ?: currentZoneSchema().id,
                    floor = itemFloorForRecommendedLevel(currentZoneSchema().recommendedLevel.max),
                    qualityFloor = ItemQuality.RARE,
                    minAffixCount =
                        if (activeBossEncounterSchema()?.tags?.contains("finale") == true) {
                            3
                        } else {
                            2
                        },
                    occupiedSlots = currentEquippedSlots(),
                ),
            ),
            rewardContext =
                RewardGenerationContext(
                    rewardSource = MilestoneRewardSource.BOSS,
                    sourceId = activeBossEncounterSchema()?.id ?: currentZoneSchema().id,
                    floor = itemFloorForRecommendedLevel(currentZoneSchema().recommendedLevel.max),
                    qualityFloor = ItemQuality.RARE,
                    minAffixCount =
                        if (activeBossEncounterSchema()?.tags?.contains("finale") == true) {
                            3
                        } else {
                            2
                        },
                    occupiedSlots = currentEquippedSlots(),
                ),
        )

    private fun routeMilestoneRewardItem(
        reward: RouteReward,
        reservedSlots: Set<EquipSlot>,
        occupiedSlots: Set<EquipSlot>,
    ): ItemInstance? {
        if (reward.milestoneRewardProfileIds.isEmpty()) {
            return null
        }
        return milestoneRewardItemFromProfiles(
            profileIds = reward.milestoneRewardProfileIds,
            fallbackBaseId =
                normalizeMilestoneFallbackBaseId(
                    fallbackBaseId = reward.guaranteedUtilityDropIds.firstOrNull().orEmpty(),
                    rewardContext =
                        RewardGenerationContext(
                            rewardSource = MilestoneRewardSource.ROUTE,
                            sourceId = reward.routeId,
                            floor = itemFloorForLevelBand(reward.levelBandRef),
                            qualityFloor = ItemQuality.MAGIC,
                            minAffixCount = 1,
                            routeBiasTags = routeRewardBiasTags(reward.rescueTags),
                            reservedSlots = reservedSlots,
                            occupiedSlots = occupiedSlots,
                        ),
                ),
            rewardContext =
                RewardGenerationContext(
                    rewardSource = MilestoneRewardSource.ROUTE,
                    sourceId = reward.routeId,
                    floor = itemFloorForLevelBand(reward.levelBandRef),
                    qualityFloor = ItemQuality.MAGIC,
                    minAffixCount = 1,
                    routeBiasTags = routeRewardBiasTags(reward.rescueTags),
                    reservedSlots = reservedSlots,
                    occupiedSlots = occupiedSlots,
                ),
        )
    }

    private fun recordMilestoneReward(
        rewardSource: MilestoneRewardSource,
        sourceId: String,
        reward: ItemInstance,
    ) {
        val equipSlot = requireNotNull(reward.slot) {
            "Milestone reward '${reward.baseId}' from $rewardSource:$sourceId must be equippable."
        }
        recordedMilestoneRewardSummaries +=
            MilestoneRewardSummary(
                rewardSource = rewardSource,
                sourceId = sourceId,
                zoneId = config.zoneId,
                baseItemId = reward.baseId,
                equipSlot = equipSlot,
                qualityTier = reward.quality,
                buildHashAtGrant = currentBuildHash(),
                affixIds = reward.affixes.map(com.ktome.core.item.AffixDef::id),
                equippedBaseItemIdBeforeReward = equippedBaseItemIdFor(equipSlot),
            )
    }

    private fun finalizeMilestoneRewardSummary(summary: MilestoneRewardSummary): MilestoneRewardSummary {
        val equippedBaseItemIdAtRunEnd = equippedBaseItemIdFor(summary.equipSlot)
        return summary.copy(
            equippedBaseItemIdAtRunEnd = equippedBaseItemIdAtRunEnd,
            adoptedInFinalBuild = equippedBaseItemIdAtRunEnd == summary.baseItemId,
        )
    }

    private fun itemFloorForLevelBand(levelBandRef: String): Int {
        val normalized = levelBandRef.removePrefix("lv")
        val maxLevel = normalized.substringAfter('_', normalized).toIntOrNull() ?: 1
        return itemFloorForRecommendedLevel(maxLevel)
    }

    private fun itemFloorForRecommendedLevel(maxLevel: Int): Int =
        ((maxLevel - 1) / 3 + 1).coerceIn(1, 5)

    private data class ObjectiveProgressSpec(
        val token: String,
        val stepKey: String,
    )

    private data class RewardSpec(
        val profileIds: List<String>,
        val fallbackBaseId: String,
    )

    private fun objectiveProgressSpecFor(interactableId: String): ObjectiveProgressSpec? =
        when (interactableId) {
            "trail_cache" -> ObjectiveProgressSpec("greenwood.trail_cache", "objective.greenwood_signal_hunt.step.trail_cache_opened")
            "ore_stash" -> ObjectiveProgressSpec("deep_iron_pit.ore_stash", "objective.deep_iron_pit_forge_run.step.ore_stash_opened")
            "seal_cache" -> ObjectiveProgressSpec("grey_gate.seal_cache", "objective.grey_gate_seal_rite.step.seal_cache_opened")
            "alarm_bonfire" -> ObjectiveProgressSpec("shattered_outpost.alarm_triggered", "objective.shattered_outpost_breach.step.alarm_triggered")
            "warden_beacon" -> ObjectiveProgressSpec("greenwood.warden_beacon", "objective.greenwood_signal_hunt.step.warden_beacon_triggered")
            "slag_valve" -> ObjectiveProgressSpec("deep_iron_pit.slag_valve", "objective.deep_iron_pit_forge_run.step.slag_valve_opened")
            "shadow_brazier" -> ObjectiveProgressSpec("grey_gate.shadow_brazier", "objective.grey_gate_seal_rite.step.shadow_brazier_lit")
            "armory_gate" -> ObjectiveProgressSpec("shattered_outpost.armory_opened", "objective.shattered_outpost_breach.step.armory_opened")
            "hunter_snare" -> ObjectiveProgressSpec("greenwood.hunter_snare", "objective.greenwood_signal_hunt.step.hunter_snare_cut")
            "mine_furnace" -> ObjectiveProgressSpec("deep_iron_pit.mine_furnace", "objective.deep_iron_pit_forge_run.step.furnace_claimed")
            "ritual_altar" -> ObjectiveProgressSpec("grey_gate.ritual_altar", "objective.grey_gate_seal_rite.step.ritual_altar_secured")
            "bandit_cache" -> ObjectiveProgressSpec("bandit_camp.cache_raided", "objective.bandit_camp_cache_raid.step.cache_raided")
            "elven_wardstone" -> ObjectiveProgressSpec("elven_ruins.wardstone_claimed", "objective.elven_ruins_relic_ward.step.wardstone_claimed")
            "molten_pressure_valve" -> ObjectiveProgressSpec("molten_core.pressure_stabilized", "objective.molten_core_pressure.step.pressure_stabilized")
            "crystal_resonance_node" -> ObjectiveProgressSpec("crystal_cavern.node_attuned", "objective.crystal_cavern_resonance.step.node_attuned")
            "river_ferry_anchor" -> ObjectiveProgressSpec("underground_river.ferry_anchor_secured", "objective.underground_river_crossing.step.ferry_anchor_secured")
            "temple_ward_reliquary" -> ObjectiveProgressSpec("abyssal_temple.ward_reliquary_claimed", "objective.abyssal_temple_sanctum.step.ward_reliquary_claimed")
            "heart_ward_focus" -> ObjectiveProgressSpec("abyssal_heart.ward_stabilized", "objective.abyssal_heart_finale.step.ward_stabilized")
            else -> null
        }

    private fun optionalZoneRewardSpec(): RewardSpec =
        RewardSpec(
            profileIds = ZoneMechanicRuntime.uniqueContentRewardProfiles(currentZoneSchema().uniqueContentTag),
            fallbackBaseId = ZoneMechanicRuntime.uniqueContentFallbackBaseId(currentZoneSchema().uniqueContentTag),
        )

    private fun ZoneSchemaV2.rewardBiasTags(): Set<String> =
        linkedSetOf<String>().apply {
            uniqueContentTag
                ?.split('.', '_', '-')
                ?.map(String::trim)
                ?.filter(String::isNotBlank)
                ?.map(String::lowercase)
                ?.forEach(::add)
            specialMechanics.map(String::lowercase).forEach(::add)
            environmentTheme.takeIf(String::isNotBlank)?.lowercase()?.let(::add)
        }

    private fun groundRewardSpecFor(interactableId: String): RewardSpec? =
        when (interactableId) {
            "supply_crate" -> RewardSpec(profileIds = emptyList(), fallbackBaseId = "scroll_teleport")
            "trail_cache" -> RewardSpec(listOf("loot.greenwood_fringe.reward", "loot.foundation.common"), "healing_potion")
            "ore_stash" -> RewardSpec(listOf("loot.deep_iron_pit.reward", "loot.foundation.elite"), "stamina_draught")
            "seal_cache" -> RewardSpec(listOf("loot.grey_gate_depths.reward", "loot.foundation.boss"), "scroll_teleport")
            "bandit_cache" -> optionalZoneRewardSpec()
            else -> null
        }

    private fun supportRewardSpecFor(interactableId: String): RewardSpec? =
        when (interactableId) {
            "armory_gate" -> RewardSpec(listOf("loot.foundation.elite"), "healing_potion")
            "hunter_snare" -> RewardSpec(listOf("loot.greenwood_fringe.reward", "loot.foundation.elite"), "bandit_trophy")
            "mine_furnace" -> RewardSpec(listOf("loot.deep_iron_pit.reward", "loot.foundation.elite"), "forgebreaker_pick")
            "ritual_altar" -> RewardSpec(listOf("loot.grey_gate_depths.reward", "loot.foundation.boss"), "sanctified_seal")
            "elven_wardstone" -> optionalZoneRewardSpec()
            "molten_pressure_valve" -> optionalZoneRewardSpec()
            "crystal_resonance_node" -> optionalZoneRewardSpec()
            "river_ferry_anchor" -> RewardSpec(listOf("loot.foundation.common", "loot.grey_gate_depths.reward"), "scroll_teleport")
            "temple_ward_reliquary" -> RewardSpec(listOf("loot.foundation.boss", "loot.grey_gate_depths.reward"), "sanctified_seal")
            "heart_ward_focus" -> RewardSpec(listOf("loot.foundation.boss"), "abyssal_heartstone")
            else -> null
        }

    private fun groundRewardItemFor(
        interactableId: String,
        spec: RewardSpec,
    ): ItemInstance =
        if (spec.profileIds.isEmpty()) {
            officialRewardItem(baseId = spec.fallbackBaseId, fallbackBaseId = "healing_potion")
        } else {
            zoneRewardItem(
                profileIds = spec.profileIds,
                fallbackBaseId = spec.fallbackBaseId,
                rewardContext =
                    RewardGenerationContext(
                        rewardSource = MilestoneRewardSource.CACHE,
                        sourceId = interactableId,
                        floor = itemFloorForRecommendedLevel(currentZoneSchema().recommendedLevel.max),
                        qualityFloor = ItemQuality.MAGIC,
                        minAffixCount = 1,
                        routeBiasTags = routeRewardBiasTags(currentZoneSchema().rewardBiasTags()),
                        occupiedSlots = currentEquippedSlots(),
                    ),
            )
        }

    private fun dropGroundRewardFromInteractable(
        schema: InteractableSchemaV2,
        position: Point,
        spec: RewardSpec,
    ) {
        val reward = groundRewardItemFor(schema.id, spec)
        ItemFactory().createGroundItem(world, reward, position)
        if (spec.profileIds.isNotEmpty()) {
            recordMilestoneReward(
                rewardSource = MilestoneRewardSource.CACHE,
                sourceId = schema.id,
                reward = reward,
            )
        }
        val rewardSchema = requireNotNull(itemSchemaFor(reward.baseId)) {
            "Unknown item schema '${reward.baseId}'."
        }
        addMessage(
            "log.interactable.supply_crate",
            keyArg("interactable", schema.nameKey),
            keyArg("item", rewardSchema.nameKey),
        )
    }

    private fun grantSupportRewardFromInteractable(
        schema: InteractableSchemaV2,
        position: Point,
        spec: RewardSpec,
    ) {
        val reward =
            zoneRewardItem(
                profileIds = spec.profileIds,
                fallbackBaseId = spec.fallbackBaseId,
                rewardContext =
                    RewardGenerationContext(
                        rewardSource = MilestoneRewardSource.CACHE,
                        sourceId = schema.id,
                        floor = itemFloorForRecommendedLevel(currentZoneSchema().recommendedLevel.max),
                        qualityFloor = ItemQuality.MAGIC,
                        minAffixCount = 1,
                        routeBiasTags = routeRewardBiasTags(currentZoneSchema().rewardBiasTags()),
                        occupiedSlots = currentEquippedSlots(),
                    ),
            )
        grantRewardItem(reward, position)
        if (spec.profileIds.isNotEmpty()) {
            recordMilestoneReward(
                rewardSource = MilestoneRewardSource.CACHE,
                sourceId = schema.id,
                reward = reward,
            )
        }
        val rewardSchema = requireNotNull(itemSchemaFor(reward.baseId)) {
            "Unknown support reward item '${reward.baseId}'."
        }
        addMessage("log.interactable.support", keyArg("interactable", schema.nameKey))
        addMessage(
            "log.interactable.support.reward",
            keyArg("interactable", schema.nameKey),
            keyArg("item", rewardSchema.nameKey),
        )
        val restored = restoreArmorySupplies()
        if (restored > 0) {
            addMessage("log.interactable.support.resupply", literalArg("amount", restored))
        }
    }

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
        if (objective.linkedQuestId != null) {
            updateCurrentObjectiveState(ObjectiveRuntimeEvaluator::onProgressRecorded)
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
            ?.activeEffects()
            ?.map { effect ->
                val statusSchema = content.statusSchemaFor(effect.schemaId)
                StatusEffectRenderSnapshot(
                    typeId = effect.schemaId,
                    remainingTurns = effect.remainingTurns,
                    nameKey = effect.nameKey ?: statusSchema?.nameKey ?: statusEffectNameKey(effect.type),
                    iconKey = effect.iconKey ?: statusSchema?.iconKey ?: StatusDefinitions.iconKey(effect.type),
                    stackCount = effect.stackCount,
                    stackCap = effect.stackCap.takeIf { cap -> cap > 1 },
                    category = effect.category.toSnapshotCategory(),
                )
            }.orEmpty()

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

        if (!prepareActorTurn(playerId)) {
            refreshFov()
            return false
        }
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
            headlessTurnEquivalent += 1
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

            if (!prepareActorTurn(nextActor)) {
                pendingActions.removeFirst()
                activeTurnActor = null
                continue
            }
            if (nextActor == playerId) {
                break
            }

            pendingActions.removeFirst()
            executeMonsterTurn(nextActor)
            finishActorTurn(nextActor)
            activeTurnActor = null
            headlessTurnEquivalent += 1
        }
    }

    private fun prepareActorTurn(actorId: EntityId): Boolean {
        if (activeTurnActor == actorId) {
            return world.isAlive(actorId)
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

        if (!applyTurnStartStatusEffects(actorId)) {
            activeTurnActor = null
            return false
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
            world.get<InscriptionCooldownState>(playerId)?.let(InscriptionManager::tickCooldowns)
        }

        activeTurnActor = actorId
        return world.isAlive(actorId)
    }

    private fun finishActorTurn(actorId: EntityId) {
        val tracker = world.get<EffectTracker>(actorId)
        val changed = tracker?.let(StatusLifecycle::decayEndOfTurn) == true
        decayAreaEffectEmitters(actorId)
        decayWorldEffects(actorId)

        if (changed) {
            StatsCalculator.recalculateAndStore(world, actorId)
        }
        if (actorId == playerId) {
            ensurePlayerResourcePools()
        }
    }

    private fun applyTurnStartStatusEffects(actorId: EntityId): Boolean {
        val dueEffects = StatusTickResolver.dueEffects(world, actorId)
        if (dueEffects.isEmpty()) {
            return world.isAlive(actorId)
        }
        var pendingDeathKiller: EntityId? = null

        listOf(
            EffectCarrierKind.ACTOR,
            EffectCarrierKind.AREA,
            EffectCarrierKind.WORLD,
        ).forEach { carrierKind ->
            dueEffects
                .filter { dueEffect -> dueEffect.carrierKind == carrierKind }
                .forEach { dueEffect ->
                    val killer = applyTurnStartStatusTick(actorId, dueEffect)
                    if (pendingDeathKiller == null) {
                        pendingDeathKiller = killer
                    }
                }
            if (pendingDeathKiller != null) {
                handleDeath(actorId, pendingDeathKiller)
                return false
            }
        }

        if (world.isAlive(actorId)) {
            StatsCalculator.recalculateAndStore(world, actorId)
        }
        return world.isAlive(actorId)
    }

    private fun applyTurnStartStatusTick(
        actorId: EntityId,
        dueEffect: com.ktome.core.status.CarrierDueEffect,
    ): EntityId? {
        val tracker = world.get<EffectTracker>(actorId)
        if (tracker != null && StatusLifecycle.hasInvulnerable(tracker)) {
            return null
        }

        val damageType = dueEffect.effect.tickDamageType ?: return null
        val rawDamage = dueEffect.effect.tickDamage
        if (rawDamage <= 0) {
            return null
        }

        val result =
            combatResolver.resolveStatusTick(
                world = world,
                source = dueEffect.sourceEntityId,
                target = actorId,
                statusType = dueEffect.effect.type,
                damageType = damageType,
                rawDamage = rawDamage,
                turn = turnCount,
                traceId = "status-tick:${dueEffect.effect.id}:${actorId.value}",
            )
        val finalDamage = result.damage.finalDamage

        logEvent(DamageDealtEvent(dueEffect.sourceEntityId ?: actorId, actorId, finalDamage, crit = false))
        logEvent(
            StatusTickEvent(
                target = actorId,
                statusType = dueEffect.effect.type,
                statusId = dueEffect.effect.schemaId,
                damage = finalDamage,
                carrierKind = dueEffect.carrierKind,
            ),
        )
        addMessage(
            "log.status.tick",
            keyArg("status", dueEffect.effect.nameKey ?: statusEffectNameKey(dueEffect.effect.type)),
            entityArg("target", actorId),
            literalArg("damage", finalDamage),
        )
        tracker?.let { activeTracker ->
            val removed = StatusLifecycle.breakOnDamage(activeTracker, finalDamage)
            if (removed.any { effect -> effect.type == StatusEffectType.STEALTH }) {
                logStealthBroken(actorId, finalDamage)
            }
        }

        return if (result.targetKilled) dueEffect.sourceEntityId ?: actorId else null
    }

    private fun decayAreaEffectEmitters(actorId: EntityId) {
        world.entitiesWith(AreaEffectEmitter::class).forEach { entityId ->
            val emitter = world.get<AreaEffectEmitter>(entityId) ?: return@forEach
            if (actorId in emitter.affectedActorIds) {
                StatusLifecycle.decayEndOfTurn(emitter)
            }
        }
    }

    private fun decayWorldEffects(actorId: EntityId) {
        world.entitiesWith(WorldEffect::class).forEach { entityId ->
            val effect = world.get<WorldEffect>(entityId) ?: return@forEach
            if (actorId in effect.affectedActorIds) {
                StatusLifecycle.decayEndOfTurn(effect)
            }
        }
    }

    private fun executePlayerCommand(command: PlayerCommand): CommandResolution {
        if (pendingRouteSelection.isNotEmpty() && command !is PlayerCommand.SelectRoute && command != PlayerCommand.SaveGame) {
            addMessage("log.route.selection.pending")
            return CommandResolution.rejected()
        }
        if (
            activeShopId != null &&
            command !is PlayerCommand.CloseShop &&
            command !is PlayerCommand.BuyShopOffer &&
            command !is PlayerCommand.SellInventoryItem &&
            command != PlayerCommand.SaveGame
        ) {
            addMessage("log.shop.pending")
            return CommandResolution.rejected()
        }

        return when (command) {
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

            PlayerCommand.Ascend -> resolveStairCommand(StairDirection.UP)

            PlayerCommand.Descend -> resolveStairCommand(StairDirection.DOWN)

            PlayerCommand.SaveGame -> {
                val saved = persistRun()
                addMessage(if (saved) "log.save.success" else "log.save.failure")
                CommandResolution(accepted = true, consumesTurn = false)
            }

            PlayerCommand.CloseShop -> {
                if (activeShopId == null) {
                    CommandResolution.rejected()
                } else {
                    activeShopId = null
                    addMessage("log.shop.close")
                    CommandResolution(accepted = true, consumesTurn = false)
                }
            }

            is PlayerCommand.BuyShopOffer -> buyShopOffer(command.index)

            is PlayerCommand.SellInventoryItem -> sellInventoryItem(command.index)

            is PlayerCommand.SelectRoute -> selectRoute(command.index)

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
                val talentId = command.talentId
                val talentSchema = talentSchemaFor(talentId)
                if (talentId !in loadout.talentLevels) {
                    addMessage("log.loadout.not_unlocked", keyArg("talent", talentNameKey(talentId)))
                    CommandResolution.rejected()
                } else if (talentSchema == null) {
                    rejectTalentOwnerConflict("log.talent.owner_unresolved")
                } else {
                    val owner = resolveTalentTreeOwner(talentSchema)
                    val existingDraft = talentDraft()
                    if (owner == null) {
                        rejectTalentOwnerConflict("log.talent.owner_unresolved")
                    } else if (!ensureDraftOwner(existingDraft, owner)) {
                        rejectTalentOwnerConflict("log.talent.owner_conflict")
                    } else {
                        val remainingPoints = remainingTalentPoints(loadout, availableTalentPointsForOwner(owner.ownerType, experience), owner)
                        if (remainingPoints <= 0) {
                            addMessage("log.talent.none")
                            CommandResolution.rejected()
                        } else {
                            val definition = requireNotNull(talentRegistry.get(talentId))
                            val currentLevel = effectiveTalentRanks(loadout)[talentId] ?: loadout.levelOf(talentId)
                            if (currentLevel >= definition.maxLevel) {
                                addMessage("log.talent.max_level", keyArg("talent", talentNameKey(talentId)))
                                CommandResolution.rejected()
                            } else {
                                val nextLevel = currentLevel + 1
                                val nextDraft =
                                    applyTalentDraft(
                                        loadout = loadout,
                                        currentDraft = existingDraft,
                                        owner = owner,
                                        talentId = talentId,
                                        nextLevel = nextLevel,
                                    )
                                val candidateRanks = TalentAllocationPlanner.effectiveRanks(loadout.talentLevels, nextDraft)
                                val missingPrerequisites = TalentPrerequisiteValidator.missingPrerequisites(definition.prerequisites, candidateRanks)
                                if (missingPrerequisites.isNotEmpty()) {
                                    addMessage(
                                        "log.talent.requirement_missing",
                                        keyArg("talent", talentNameKey(talentId)),
                                        keyArg("required", talentNameKey(missingPrerequisites.first().talentId)),
                                        literalArg("rank", missingPrerequisites.first().minRank),
                                    )
                                    CommandResolution.rejected()
                                } else {
                                    storeNormalizedTalentDraft(loadout, nextDraft)
                                    addMessage(
                                        "log.talent.preview_advance",
                                        keyArg("talent", talentNameKey(talentId)),
                                        literalArg("level", nextLevel),
                                    )
                                    CommandResolution(accepted = true, consumesTurn = false)
                                }
                            }
                        }
                    }
                }
            }

            PlayerCommand.ConfirmTalentDraft -> {
                val draft = talentDraft()
                val experience = requireNotNull(world.get<Experience>(playerId))
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId))
                if (draft == null) {
                    CommandResolution.rejected()
                } else if (isPlayerInCombat()) {
                    addMessage("log.talent.draft_confirm_blocked")
                    CommandResolution.rejected()
                } else {
                    val owner = TalentTreeOwnerRef(draft.ownerType, draft.treeOwnerId)
                    val preview = talentAllocationPreview(loadout, availableTalentPointsForOwner(owner.ownerType, experience), owner)
                    draft.pendingRanks.forEach { (talentId, rank) ->
                        loadout.talentLevels[talentId] = rank
                    }
                    storeRemainingTalentPointsForOwner(owner.ownerType, preview.remainingPoints, experience)
                    setTalentDraft(null)
                    canonicalizePlayerLoadout(loadout)
                    addMessage("log.talent.draft_confirmed")
                    CommandResolution(accepted = true, consumesTurn = false)
                }
            }

            PlayerCommand.RollbackTalentDraft -> {
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId))
                val draft = talentDraft()
                if (draft == null || draft.previousPendingRanks == null) {
                    CommandResolution.rejected()
                } else {
                    storeNormalizedTalentDraft(loadout, RollbackManager.rollback(draft))
                    addMessage("log.talent.draft_rollback")
                    CommandResolution(accepted = true, consumesTurn = false)
                }
            }

            is PlayerCommand.RespecTalentTree -> {
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId))
                if (isPlayerInCombat()) {
                    addMessage("log.talent.respec_blocked")
                    CommandResolution.rejected()
                } else {
                    val owner = TalentTreeOwnerRef(command.ownerType, command.treeOwnerId)
                    val liveRanks = scopedTalentRanks(loadout = loadout, owner = owner)
                    if (liveRanks.isEmpty()) {
                        rejectTalentOwnerConflict("log.talent.owner_unresolved")
                    } else {
                        val respecDraft =
                            respecManager.createDraft(
                                ownerType = owner.ownerType,
                                treeOwnerId = owner.treeOwnerId,
                                liveRanks = liveRanks,
                                minimumRanks = minimumTalentRanks(loadout = loadout, owner = owner),
                            )
                        storeNormalizedTalentDraft(loadout, respecDraft)
                        addMessage("log.talent.respec")
                        CommandResolution(accepted = true, consumesTurn = false)
                    }
                }
            }

            is PlayerCommand.EquipTalentToSlot -> {
                val loadout = requireNotNull(world.get<TalentLoadout>(playerId)) { "Missing TalentLoadout for $playerId." }
                syncUnlockedPlayerTalents()
                if (command.slot !in 1..PLAYER_ACTIVE_TALENT_SLOT_COUNT) {
                    addMessage("log.loadout.invalid_slot", literalArg("slot", command.slot))
                    CommandResolution.rejected()
                } else if (command.talentId !in loadout.talentLevels) {
                    addMessage("log.loadout.not_unlocked", keyArg("talent", talentNameKey(command.talentId)))
                    CommandResolution.rejected()
                } else {
                    val equippedBefore = loadout.talentIdAt(command.slot)
                    val currentlyEquippedSlot =
                        activeTalentMappings(loadout)
                            .firstOrNull { (_, talentId) -> talentId == command.talentId }
                            ?.first
                    if (equippedBefore == command.talentId) {
                        CommandResolution(accepted = true, consumesTurn = false)
                    } else {
                        if (currentlyEquippedSlot != null) {
                            loadout.slotToTalentId.remove(currentlyEquippedSlot)
                        }
                        loadout.slotToTalentId[command.slot] = command.talentId
                        canonicalizePlayerLoadout(loadout)
                        addMessage(
                            "log.loadout.equip",
                            keyArg("talent", talentNameKey(command.talentId)),
                            literalArg("slot", command.slot),
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
                            if (result.result.hasConfirmedResolutionSuccess()) {
                                recordSuccessfulPlayerAffinity(talentId)
                            }
                            logTalentResult(result.result)
                            logTriggeredTalentDamagePassives(result.result)
                            handleTalentDeaths(result.result.targets, playerId)
                            CommandResolution.accepted()
                        }
                    }
                }
            }

            is PlayerCommand.UseInscription -> {
                val used = useInscription(command.hotkey, command.target)
                CommandResolution(accepted = used, consumesTurn = used)
            }
        }
    }

    private fun resolveStairCommand(direction: StairDirection): CommandResolution {
        val transition = transitionFloor(direction)
        return when (transition) {
            TransitionOutcome.REJECTED -> CommandResolution.rejected()
            TransitionOutcome.OPENED_ROUTE_SELECTION -> CommandResolution(accepted = true, consumesTurn = false)
            TransitionOutcome.TRANSITIONED -> CommandResolution(accepted = true, consumesTurn = true, persistCheckpointAfterTurn = true)
        }
    }

    private fun buyShopOffer(index: Int): CommandResolution {
        val shop = currentShopNode() ?: return CommandResolution.rejected()
        val offer = availableShopOffers().getOrNull(index)
        if (offer == null) {
            addMessage("log.shop.offer_missing")
            return CommandResolution.rejected()
        }
        if (!ShardEconomy.canAfford(shardBalance, offer.price)) {
            addMessage("log.shop.cannot_afford", literalArg("price", offer.price))
            return CommandResolution.rejected()
        }

        when {
            offer.itemBaseId != null -> {
                val itemBaseId = requireNotNull(offer.itemBaseId)
                val rewardItem =
                    requireNotNull(itemBaseDef(itemBaseId)) { "Unknown shop item '$itemBaseId'." }
                        .toRuntimeItem()
                val stored = grantRewardItem(rewardItem, playerPosition())
                shardBalance -= offer.price
                markShopOfferPurchased(shop.id, offer.id)
                addMessage(
                    if (stored) "log.shop.buy.item" else "log.shop.buy.item_dropped",
                    keyArg("shop", shop.nameKey),
                    keyArg("item", requireNotNull(itemSchemaFor(itemBaseId)).nameKey),
                    literalArg("price", offer.price),
                )
                return CommandResolution(accepted = true, consumesTurn = false)
            }

            offer.inscriptionId != null -> {
                ensurePlayerInscriptions()
                val loadout = world.get<InscriptionLoadout>(playerId) ?: return CommandResolution.rejected()
                val definition =
                    requireNotNull(content.inscriptions.firstOrNull { inscription -> inscription.id == offer.inscriptionId }) {
                        "Unknown shop inscription '${offer.inscriptionId}'."
                    }
                val equippedDefinitions =
                    loadout.slots.mapNotNull { slot ->
                        content.inscriptions.firstOrNull { inscription -> inscription.id == slot.inscriptionId }
                    }
                if (!InscriptionManager.canEquip(loadout, equippedDefinitions, definition)) {
                    addMessage("log.shop.inscription_slots_full", keyArg("inscription", definition.nameKey))
                    return CommandResolution.rejected()
                }
                InscriptionManager.equip(loadout, equippedDefinitions, definition)
                shardBalance -= offer.price
                markShopOfferPurchased(shop.id, offer.id)
                addMessage(
                    "log.shop.buy.inscription",
                    keyArg("shop", shop.nameKey),
                    keyArg("inscription", definition.nameKey),
                    literalArg("price", offer.price),
                )
                return CommandResolution(accepted = true, consumesTurn = false)
            }

            else -> return CommandResolution.rejected()
        }
    }

    private fun canPurchaseShopOffer(offer: ShopOffer): Boolean {
        if (!ShardEconomy.canAfford(shardBalance, offer.price)) {
            return false
        }
        return when {
            offer.itemBaseId != null -> true
            offer.inscriptionId != null -> {
                ensurePlayerInscriptions()
                val loadout = world.get<InscriptionLoadout>(playerId) ?: return false
                val definition =
                    requireNotNull(content.inscriptions.firstOrNull { inscription -> inscription.id == offer.inscriptionId }) {
                        "Unknown shop inscription '${offer.inscriptionId}'."
                    }
                val equippedDefinitions =
                    loadout.slots.mapNotNull { slot ->
                        content.inscriptions.firstOrNull { inscription -> inscription.id == slot.inscriptionId }
                    }
                InscriptionManager.canEquip(loadout, equippedDefinitions, definition)
            }
            else -> false
        }
    }

    private fun sellInventoryItem(index: Int): CommandResolution {
        val shop = currentShopNode() ?: return CommandResolution.rejected()
        val inventory = world.get<Inventory>(playerId) ?: return CommandResolution.rejected()
        val itemId = inventory.itemIds.getOrNull(index)
        if (itemId == null) {
            addMessage("log.inventory.slot_empty")
            return CommandResolution.rejected()
        }
        val item = requireNotNull(world.get<ItemInstance>(itemId)) { "Missing inventory item '$itemId'." }
        equippedSlotForItem(itemId)?.let { slot ->
            world.get<Equipment>(playerId)?.slots?.remove(slot)
        }
        inventory.itemIds.removeAt(index)
        world.destroyEntity(itemId)
        val sellValue = sellValueForItem(item)
        shardBalance += sellValue
        StatsCalculator.recalculateAndStore(world, playerId)
        ensurePlayerResourcePools()
        syncPlayerResistanceProfile()
        addMessage(
            "log.shop.sell",
            keyArg("shop", shop.nameKey),
            keyArg("item", requireNotNull(itemSchemaFor(item.baseId)).nameKey),
            literalArg("price", sellValue),
        )
        return CommandResolution(accepted = true, consumesTurn = false)
    }

    private fun selectRoute(index: Int): CommandResolution {
        val option = pendingRouteSelection.getOrNull(index)
        if (option == null) {
            addMessage("log.route.selection.invalid")
            return CommandResolution.rejected()
        }
        pendingRouteSelection = emptyList()
        completeZoneTransition(option)
        addMessage(
            "log.route.selection.confirmed",
            keyArg("zone", zoneSchemaFor(option.destinationZoneId).nameKey),
        )
        return CommandResolution(accepted = true, consumesTurn = false, persistCheckpointAfterTurn = true)
    }

    private fun markShopOfferPurchased(
        shopId: String,
        offerId: String,
    ) {
        val state = shopStates[shopId] ?: ShopInventoryState(shopId = shopId)
        shopStates[shopId] = state.copy(purchasedOfferIds = state.purchasedOfferIds + offerId)
    }

    private fun equippedSlotForItem(itemId: EntityId): EquipSlot? =
        world.get<Equipment>(playerId)
            ?.slots
            ?.entries
            ?.firstOrNull { (_, equippedItemId) -> equippedItemId == itemId }
            ?.key

    private fun sellValueForInventoryIndex(index: Int): Int {
        val itemId = world.get<Inventory>(playerId)?.itemIds?.getOrNull(index)
            ?: return 0
        val item = world.get<ItemInstance>(itemId) ?: return 0
        return sellValueForItem(item)
    }

    private fun sellValueForItem(item: ItemInstance): Int {
        val basePrice =
            when (item.type) {
                ItemType.WEAPON -> 32
                ItemType.ARMOR -> 30
                ItemType.CONSUMABLE -> 18
            }
        val qualityPrice =
            when (item.quality) {
                ItemQuality.COMMON -> 0
                ItemQuality.MAGIC -> 14
                ItemQuality.RARE -> 30
            }
        val affixPrice = item.affixes.sumOf { affix -> affix.tier * 8 }
        val magnitudePrice = (item.magnitude / 4).coerceAtLeast(0)
        val materialPrice = item.materialId?.let(::materialSchemaFor)?.minFloor?.times(3) ?: 0
        return ShardEconomy.sellValue(basePrice + qualityPrice + affixPrice + magnitudePrice + materialPrice)
    }

    private fun grantShards(amount: Int) {
        if (amount <= 0) {
            return
        }
        shardBalance += amount
        addMessage("log.shard.gain", literalArg("amount", amount))
    }

    private fun shardRewardForKill(
        isBoss: Boolean,
    ): Int =
        if (isBoss) {
            12 + currentZoneSchema().recommendedLevel.min * 2
        } else {
            maxOf(3, currentFloor() + currentZoneSchema().recommendedLevel.min / 2)
        }

    private fun useInscription(
        hotkey: Int,
        target: Point? = null,
    ): Boolean {
        ensurePlayerInscriptions()
        val loadout = world.get<InscriptionLoadout>(playerId) ?: return false
        val slot =
            loadout.slots.firstOrNull { inscriptionSlot -> inscriptionSlot.hotkey == hotkey }
                ?: run {
                    addMessage("log.inscription.slot_empty", literalArg("slot", hotkey))
                    return false
                }
        val definition =
            content.inscriptions.firstOrNull { inscription -> inscription.id == slot.inscriptionId }
                ?: run {
                    addMessage("log.inscription.missing", literalArg("slot", hotkey))
                    return false
                }
        val cooldowns = world.get<InscriptionCooldownState>(playerId) ?: InscriptionCooldownState().also { world.add(playerId, it) }
        if (InscriptionManager.isOnCooldown(cooldowns, definition.id)) {
            addMessage(
                "log.inscription.cooldown",
                keyArg("inscription", definition.nameKey),
                literalArg("turns", cooldowns.remainingByInscriptionId[definition.id] ?: 0),
            )
            return false
        }
        if (!applyInscriptionEffect(definition, target)) {
            return false
        }
        InscriptionManager.startCooldown(cooldowns, definition)
        addMessage("log.inscription.use", keyArg("inscription", definition.nameKey))
        return true
    }

    private fun applyInscriptionEffect(
        definition: InscriptionDef,
        target: Point? = null,
    ): Boolean =
        when (val effect = definition.effect) {
            is InscriptionEffect.Heal -> {
                val health = requireNotNull(world.get<Health>(playerId)) { "Missing Health for $playerId." }
                val amount =
                    maxOf(
                        effect.amount,
                        (health.max * effect.percentMax).roundToInt(),
                    ).coerceAtLeast(0)
                val before = health.current
                health.current = (health.current + amount).coerceAtMost(health.max)
                addMessage("log.inscription.heal", literalArg("amount", health.current - before))
                true
            }

            is InscriptionEffect.Teleport -> {
                val destination =
                    if (effect.controlled) {
                        resolveControlledTeleportDestination(target = target, maxRange = effect.range)
                            ?: run {
                                addMessage("log.inscription.no_teleport_destination")
                                return false
                            }
                    } else {
                        randomTeleportDestination(maxRange = effect.range)
                    }
                requireNotNull(world.get<Position>(playerId)).moveTo(destination)
                refreshFov()
                addMessage("log.inscription.teleport")
                true
            }

            is InscriptionEffect.Shield -> {
                StatusLifecycle.applyEffect(
                    world,
                    playerId,
                    StatusLifecycle.createInstance(
                        type = StatusEffectType.HOLY_SHIELD_BUFF,
                        effectId = "inscription:${definition.id}:$turnCount",
                        duration = effect.duration,
                        magnitude = (effect.amount.toDouble() / 100.0).coerceAtLeast(0.1),
                        sourceEntityId = playerId,
                        appliedTurn = turnCount,
                    ),
                )
                StatsCalculator.recalculateAndStore(world, playerId)
                syncPlayerResistanceProfile()
                addMessage("log.inscription.shield", literalArg("turns", effect.duration))
                true
            }

            is InscriptionEffect.Cleanse -> {
                val tracker = world.get<EffectTracker>(playerId)
                val removed = tracker?.let { activeTracker -> StatusLifecycle.cleanse(activeTracker, effect.count) }.orEmpty()
                if (effect.alsoHeal > 0) {
                    world.get<Health>(playerId)?.let { health ->
                        health.current = (health.current + effect.alsoHeal).coerceAtMost(health.max)
                    }
                }
                if (removed.isNotEmpty()) {
                    addMessage("log.inscription.cleanse", literalArg("count", removed.size))
                }
                StatsCalculator.recalculateAndStore(world, playerId)
                true
            }

            is InscriptionEffect.DamageBoost -> {
                StatusLifecycle.applyEffect(
                    world,
                    playerId,
                    StatusLifecycle.createInstance(
                        type = StatusEffectType.MANA_SURGE_BUFF,
                        effectId = "inscription:${definition.id}:$turnCount",
                        duration = effect.duration,
                        magnitude = (effect.multiplier - 1.0).coerceAtLeast(0.0),
                        sourceEntityId = playerId,
                        appliedTurn = turnCount,
                    ),
                )
                StatsCalculator.recalculateAndStore(world, playerId)
                addMessage("log.inscription.buff", literalArg("turns", effect.duration))
                true
            }
        }

    private fun executeMonsterTurn(monsterId: EntityId) {
        if (!world.isAlive(playerId)) {
            return
        }
        val behavior = world.get<AIBehavior>(monsterId) ?: return
        val position = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val phaseUpdate = updateBossPhaseIfNeeded(monsterId, BossPhaseTransitionTiming.START_OF_TURN)
        if (phaseUpdate.phaseChanged && world.get<PendingTelegraphState>(monsterId) != null) {
            return
        }
        if (world.get<EffectTracker>(monsterId)?.has(StatusEffectType.STUN) == true) {
            return
        }
        if (advancePendingTelegraph(monsterId)) {
            return
        }
        val effectiveTargetId = effectiveTargetIdFor(monsterId)
        val targetPosition = world.get<Position>(effectiveTargetId)?.toPoint() ?: playerPosition()
        val profile = phaseUpdate.profile ?: activeAiProfileFor(monsterId)
        val perceptionRange = profile?.perceptionRange ?: behavior.sightRadius
        val targetVisible = targetVisibleFor(monsterId, effectiveTargetId, position, perceptionRange)
        syncLastKnownTargetPosition(monsterId, targetPosition, targetVisible)

        val selectedAction =
            profile?.let { activeProfile ->
                val decision =
                    AIProfileResolver.decide(
                        profile = activeProfile,
                        context =
                            AIProfileDecisionContext(
                                actorId = monsterId.value,
                                turnId = turnCount,
                                selfHpRatio = healthRatio(monsterId),
                                targetHpRatio = healthRatioOrNull(effectiveTargetId),
                                targetVisible = targetVisible,
                                targetDistance = position.chebyshevDistanceTo(targetPosition),
                                selfStatusIds = activeStatusIds(monsterId),
                                targetStatusIds = activeStatusIds(effectiveTargetId),
                                usableAbilityIds = usableAbilityIdsFor(monsterId, effectiveTargetId, targetVisible, targetPosition),
                                currentEncounterTurn = world.get<BossEncounterState>(monsterId)?.encounterTurnCount ?: 0,
                            ),
                        randomSource = sessionRandom,
                    )
                recordAiDecisionTrace(decision.trace)
                decision.selectedAction
            }

        if (selectedAction != null && executeSelectedAiAction(monsterId, selectedAction, effectiveTargetId, targetPosition, targetVisible)) {
            return
        }

        executeDefaultBehavior(monsterId, behavior, effectiveTargetId, targetPosition, targetVisible)
    }

    private fun healthRatio(entityId: EntityId): Double {
        val health = requireNotNull(world.get<Health>(entityId)) { "Missing Health for '$entityId'." }
        return if (health.max <= 0) 0.0 else health.current.coerceAtLeast(0).toDouble() / health.max.toDouble()
    }

    private fun healthRatioOrNull(entityId: EntityId): Double? =
        world.get<Health>(entityId)?.let { health ->
            if (health.max <= 0) {
                0.0
            } else {
                health.current.coerceAtLeast(0).toDouble() / health.max.toDouble()
            }
        }

    private fun activeStatusIds(entityId: EntityId): Set<String> =
        world.get<EffectTracker>(entityId)
            ?.activeEffects()
            ?.mapTo(linkedSetOf()) { effect -> effect.schemaId }
            .orEmpty()

    private fun syncLastKnownTargetPosition(
        monsterId: EntityId,
        targetPosition: Point,
        targetVisible: Boolean,
    ) = StealthTauntHandler.rememberLastKnownTargetPosition(world.get<AIPerceptionState>(monsterId), targetVisible, targetPosition)

    private fun updateBossPhaseIfNeeded(
        monsterId: EntityId,
        transitionTiming: BossPhaseTransitionTiming,
        advanceTurnCounter: Boolean = true,
    ): BossPhaseTurnUpdate {
        val bossState =
            world.get<BossEncounterState>(monsterId)
                ?: return BossPhaseTurnUpdate(profile = null, phaseChanged = false)
        val encounter =
            bossEncounterFor(monsterId)
                ?: return BossPhaseTurnUpdate(profile = null, phaseChanged = false)
        if (advanceTurnCounter) {
            bossState.encounterTurnCount += 1
        }
        val resolution =
            if (transitionTiming == BossPhaseTransitionTiming.ALLOW_FATAL_TRANSITION) {
                BossPhaseManager.resolvePhaseResolutionOrNull(
                    encounter = encounter,
                    context =
                        BossPhaseEvaluationContext(
                            healthRatio = healthRatio(monsterId),
                            encounterTurnCount = bossState.encounterTurnCount,
                            activeStatusIds = activeStatusIds(monsterId),
                        ),
                    currentPhaseId = bossState.currentPhaseId,
                    transitionTiming = transitionTiming,
                ) ?: return BossPhaseTurnUpdate(profile = activeAiProfileFor(monsterId), phaseChanged = false)
            } else {
                BossPhaseManager.resolvePhaseResolution(
                    encounter = encounter,
                    context =
                        BossPhaseEvaluationContext(
                            healthRatio = healthRatio(monsterId),
                            encounterTurnCount = bossState.encounterTurnCount,
                            activeStatusIds = activeStatusIds(monsterId),
                        ),
                    currentPhaseId = bossState.currentPhaseId,
                    transitionTiming = transitionTiming,
                )
            }
        val nextPhase = resolution.phase
        if (bossState.currentPhaseId != nextPhase.id) {
            applyBossPhaseEnter(monsterId, bossState, encounter, resolution)
            return BossPhaseTurnUpdate(
                profile = content.aiProfile(nextPhase.aiProfileId),
                phaseChanged = true,
            )
        } else {
            bossState.phaseTurnCount += 1
        }
        return BossPhaseTurnUpdate(
            profile = content.aiProfile(nextPhase.aiProfileId),
            phaseChanged = false,
        )
    }

    private fun applyBossPhaseEnter(
        monsterId: EntityId,
        bossState: BossEncounterState,
        encounter: com.ktome.core.ai.BossEncounter,
        resolution: BossPhaseResolution,
    ) {
        val nextPhase = resolution.phase
        val previousPhaseId = bossState.currentPhaseId
        bossState.currentPhaseId = nextPhase.id
        bossState.phaseTurnCount = 0
        val sideEffects = mutableListOf<String>()
        if (world.get<PendingTelegraphState>(monsterId) != null) {
            world.remove<PendingTelegraphState>(monsterId)
            sideEffects += "CLEAR_PENDING_TELEGRAPH"
        }
        if (nextPhase.resetAiPhaseState) {
            clearBossRuntimeState(monsterId)
            sideEffects += "RESET_AI_PHASE_STATE"
        }
        nextPhase.onEnter.forEach { event ->
            when (event.type) {
                BossPhaseEventType.TELEGRAPH -> {
                    val telegraphSpecId = requireNotNull(event.telegraphSpecId) {
                        "Boss phase '${nextPhase.id}' TELEGRAPH event must declare telegraphSpecId."
                    }
                    val spec = content.telegraphRegistry.require(telegraphSpecId)
                    world.remove<PendingTelegraphState>(monsterId)
                    world.add(
                        monsterId,
                        PendingTelegraphState(
                            telegraphSpecId = telegraphSpecId,
                            sourceAbilityId = telegraphSpecId,
                            remainingTurns = spec.previewTurns,
                            targetPoint = bossPhaseTelegraphTargetPoint(monsterId),
                            queuedAbilityId = null,
                            resolvedDangerLevel = spec.dangerLevel,
                        ),
                    )
                    sideEffects += "TELEGRAPH:$telegraphSpecId"
                }

                BossPhaseEventType.CLEAR_STATUSES -> {
                    world.get<EffectTracker>(monsterId)?.effects?.clear()
                    sideEffects += "CLEAR_STATUSES"
                }

                BossPhaseEventType.INVULNERABLE -> {
                    event.invulnerableTurns?.let { turns ->
                        grantBossInvulnerable(monsterId, turns)
                        sideEffects += "INVULNERABLE:$turns"
                    }
                }

                BossPhaseEventType.EMIT_EVENT -> {
                    event.messageKey?.let { messageKey ->
                        addMessage(messageKey, entityArg("source", monsterId))
                        sideEffects += "EMIT_EVENT:$messageKey"
                    }
                }
            }
        }
        recordBossTrace(
            BossTrace(
                encounterId = encounter.id,
                actorId = monsterId.value,
                fromPhase = previousPhaseId,
                toPhase = nextPhase.id,
                trigger = resolution.matchedTriggers.joinToString(separator = "+").ifBlank { "phase_match" },
                turnId = turnCount,
                sideEffects = sideEffects,
            ),
        )
    }

    private fun bossPhaseTelegraphTargetPoint(monsterId: EntityId): Point {
        val origin = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val effectiveTargetId = effectiveTargetIdFor(monsterId)
        val behavior = world.get<AIBehavior>(monsterId)
        val perceptionRange = activeAiProfileFor(monsterId)?.perceptionRange ?: behavior?.sightRadius ?: config.fovRadius
        val targetVisible = targetVisibleFor(monsterId, effectiveTargetId, origin, perceptionRange)
        return when {
            targetVisible -> world.get<Position>(effectiveTargetId)?.toPoint()
            else -> world.get<AIPerceptionState>(monsterId)?.lastKnownTargetPosition
        } ?: origin
    }

    private fun clearBossRuntimeState(monsterId: EntityId) {
        world.get<AIPerceptionState>(monsterId)?.lastKnownTargetPosition = null
        world.get<AiTriggerTracker>(monsterId)?.apply {
            engagedInCombat = false
            pendingCombatStartTriggerIds.clear()
        }
    }

    private fun grantBossInvulnerable(
        monsterId: EntityId,
        turns: Int,
    ) {
        val tracker = world.get<EffectTracker>(monsterId) ?: return
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.INVULNERABLE,
                effectId = "boss_phase_invulnerable_${monsterId.value}_${turnCount}",
                duration = turns,
                sourceEntityId = monsterId,
            ),
        )
    }

    private fun advancePendingTelegraph(monsterId: EntityId): Boolean {
        val pending = world.get<PendingTelegraphState>(monsterId) ?: return false
        if (pending.remainingTurns > 1) {
            pending.remainingTurns -= 1
            return true
        }

        world.remove<PendingTelegraphState>(monsterId)
        val queuedAbilityId = pending.queuedAbilityId ?: return true
        val target = talentRegistry.get(queuedAbilityId)?.range?.takeIf { range -> range > 0 }?.let { pending.targetPoint }
        when (val result = talentResolver.resolve(world, map, monsterId, queuedAbilityId, target)) {
            is TalentUseResult.Failure -> return true
            is TalentUseResult.Success -> {
                applyTalentResourceReactions(result.result)
                logTalentResult(result.result)
                logTriggeredTalentDamagePassives(result.result)
                handleTalentDeaths(result.result.targets, monsterId)
                return true
            }
        }
    }

    internal fun recentAIDecisionTraces(): List<AIDecisionTrace> = recentAiDecisionTraces.toList()

    internal fun recentBossTraces(): List<BossTrace> = recentBossTraces.toList()

    private fun recordAiDecisionTrace(trace: AIDecisionTrace) {
        recentAiDecisionTraces += trace
        while (recentAiDecisionTraces.size > AI_TRACE_LIMIT) {
            recentAiDecisionTraces.removeFirst()
        }
    }

    private fun recordBossTrace(trace: BossTrace) {
        recentBossTraces += trace
        while (recentBossTraces.size > AI_TRACE_LIMIT) {
            recentBossTraces.removeFirst()
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
            ?.activeEffects()
            ?.map { effect ->
                tr(
                    "ui.inspect.effect.turns",
                    "name" to (statusEffectName(effect.type) + statusStackSuffix(effect.stackCount, effect.stackCap)),
                    "turns" to effect.remainingTurns,
                )
            }
            .orEmpty()

    private fun statusStackSuffix(
        stackCount: Int,
        stackCap: Int,
    ): String =
        when {
            stackCount <= 1 -> ""
            stackCap > 1 -> " x$stackCount/$stackCap"
            else -> " x$stackCount"
        }

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

    private fun usableAbilityIdsFor(
        monsterId: EntityId,
        targetId: EntityId,
        targetVisible: Boolean,
        targetPosition: Point,
    ): Set<String> {
        val loadout = world.get<TalentLoadout>(monsterId) ?: return emptySet()
        return loadout.talentLevels.keys.filterTo(linkedSetOf()) { talentId ->
            val definition = talentRegistry.get(talentId) ?: return@filterTo false
            val targetPoint = abilityTargetPoint(definition, targetVisible, targetPosition)
            talentResolver.canUse(world, map, monsterId, talentId, targetPoint) == null &&
                (definition.range == 0 || targetVisible || targetId != playerId)
        }
    }

    private fun abilityTargetPoint(
        definition: com.ktome.core.talent.TalentDef,
        targetVisible: Boolean,
        targetPosition: Point,
    ): Point? =
        when {
            definition.range <= 0 -> null
            !targetVisible -> null
            else -> targetPosition
        }

    private fun executeSelectedAiAction(
        monsterId: EntityId,
        selectedAction: com.ktome.core.ai.AIAction,
        targetId: EntityId,
        targetPosition: Point,
        targetVisible: Boolean,
    ): Boolean {
        return when (selectedAction.type) {
            AIActionType.ATTACK_TARGET -> {
                val monsterPosition = requireNotNull(world.get<Position>(monsterId)).toPoint()
                if (
                    !targetVisible ||
                    !world.isAlive(targetId) ||
                    !monsterPosition.isAdjacentTo(targetPosition)
                ) {
                    false
                } else {
                    resolveAttack(monsterId, targetId)
                    true
                }
            }

            AIActionType.MOVE_TOWARD_TARGET -> {
                if (!targetVisible) {
                    false
                } else {
                    val behavior = requireNotNull(world.get<AIBehavior>(monsterId))
                    applyPathingResult(
                        monsterId = monsterId,
                        result =
                            AIPathing.moveToward(
                                pathingContext(
                                    monsterId = monsterId,
                                    targetId = targetId,
                                    targetPosition = targetPosition,
                                    behavior = behavior,
                                    targetVisible = true,
                                ),
                            ),
                    )
                    true
                }
            }

            AIActionType.RETREAT_FROM_TARGET -> {
                if (!targetVisible) {
                    false
                } else {
                    val behavior = requireNotNull(world.get<AIBehavior>(monsterId))
                    val retreat = AIPathing.retreatStep(pathingContext(monsterId, targetId, targetPosition, behavior, targetVisible = true))
                    retreat?.let { destination ->
                        applyPathingResult(monsterId, AIPathingResult(AIPathCommand.Move(destination)))
                        true
                    } ?: false
                }
            }

            AIActionType.USE_ABILITY -> {
                val abilityId = requireNotNull(selectedAction.abilityId) {
                    "AI action '${selectedAction.id}' must declare abilityId."
                }
                val definition = talentRegistry.get(abilityId) ?: return false
                val targetPoint = abilityTargetPoint(definition, targetVisible, targetPosition)
                val talentSchema = talentSchemaFor(abilityId) ?: return false
                val telegraphRef = talentSchema.telegraphRef
                if (telegraphRef != null) {
                    scheduleTelegraphedAbility(monsterId, abilityId, targetPoint ?: requireNotNull(world.get<Position>(monsterId)).toPoint(), talentSchema)
                } else {
                    when (val result = talentResolver.resolve(world, map, monsterId, abilityId, targetPoint)) {
                        is TalentUseResult.Failure -> return false
                        is TalentUseResult.Success -> {
                            applyTalentResourceReactions(result.result)
                            logTalentResult(result.result)
                            logTriggeredTalentDamagePassives(result.result)
                            handleTalentDeaths(result.result.targets, monsterId)
                        }
                    }
                }
                true
            }

            AIActionType.WAIT -> true
        }
    }

    private fun scheduleTelegraphedAbility(
        monsterId: EntityId,
        abilityId: String,
        targetPoint: Point,
        talentSchema: TalentSchemaV2,
    ) {
        val telegraphSpec = content.telegraphSpecFor(talentSchema.telegraphRef) ?: return
        val threatProfile = content.threatProfileRegistry.require(telegraphSpec.threatProfileId)
        val loadout = world.get<TalentLoadout>(monsterId)
        val rank = loadout?.talentLevels?.get(abilityId) ?: 1
        val levelEffect =
            talentSchema.levelEffects[rank]
                ?: talentSchema.levelEffects.entries.maxByOrNull { (resolvedRank, _) -> resolvedRank }?.value
                ?: TalentLevelEffectSchemaV2()
        val assessment =
            ThreatRatingResolver.assess(
                telegraphSpec = telegraphSpec,
                threatProfile = threatProfile,
                baseAttack = world.get<DerivedStats>(monsterId)?.attack ?: 0,
                damageMultiplier = levelEffect.damageMultiplier,
                damageType = talentSchema.damageType?.let(DamageType::valueOf),
            )
        world.remove<PendingTelegraphState>(monsterId)
        world.add(
            monsterId,
            PendingTelegraphState(
                telegraphSpecId = telegraphSpec.id,
                sourceAbilityId = abilityId,
                remainingTurns = assessment.previewTurns,
                targetPoint = targetPoint,
                queuedAbilityId = abilityId,
                resolvedDangerLevel = assessment.dangerLevel,
            ),
        )
    }

    private fun executeDefaultBehavior(
        monsterId: EntityId,
        behavior: AIBehavior,
        targetId: EntityId,
        targetPosition: Point,
        targetVisible: Boolean,
    ) {
        val position = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val profile = activeAiProfileFor(monsterId)
        val perception = world.get<AIPerceptionState>(monsterId)
        val lastKnownTarget =
            StealthTauntHandler.consumeLastKnownTargetPosition(
                perception = perception,
                currentPosition = position,
                useLastKnownPosition = profile?.useLastKnownPosition == true,
            )
        if (!targetVisible && lastKnownTarget != null) {
            applyPathingResult(
                monsterId = monsterId,
                result =
                    AIPathing.moveToward(
                        pathingContext(
                            monsterId = monsterId,
                            targetId = targetId,
                            targetPosition = lastKnownTarget,
                            behavior = behavior,
                            targetVisible = true,
                        ),
                    ),
            )
            return
        }

        val result =
            when (profile?.defaultBehavior ?: behavior.type.toDefaultBehavior()) {
                AIDefaultBehavior.CHASE ->
                    AIPathing.chase(pathingContext(monsterId, targetId, targetPosition, behavior, targetVisible))
                AIDefaultBehavior.KITE ->
                    AIPathing.kite(pathingContext(monsterId, targetId, targetPosition, behavior, targetVisible))
                AIDefaultBehavior.PATROL ->
                    AIPathing.patrol(pathingContext(monsterId, targetId, targetPosition, behavior, targetVisible))
                AIDefaultBehavior.WAIT -> AIPathingResult(AIPathCommand.Wait)
            }
        applyPathingResult(monsterId, result)
    }

    private fun pathingContext(
        monsterId: EntityId,
        targetId: EntityId,
        targetPosition: Point,
        behavior: AIBehavior,
        targetVisible: Boolean,
    ): AIPathingContext =
        AIPathingContext(
            map = map,
            actor =
                AIPathingActorSnapshot(
                    entityId = monsterId,
                    position = requireNotNull(world.get<Position>(monsterId)).toPoint(),
                    behavior = behavior,
                    patrolRoute = world.get<PatrolRoute>(monsterId),
                ),
            target = AIPathingTargetSnapshot(entityId = targetId, position = targetPosition),
            occupiedTiles = occupiedBlockingTiles(excluding = monsterId),
            targetVisible = targetVisible,
        )

    private fun applyPathingResult(
        monsterId: EntityId,
        result: AIPathingResult,
    ) {
        result.nextPatrolIndex?.let { nextIndex ->
            world.get<PatrolRoute>(monsterId)?.nextWaypointIndex = nextIndex
        }
        when (val command = result.command) {
            is AIPathCommand.Attack -> resolveAttack(monsterId, command.target)
            is AIPathCommand.Move -> {
                if (blockerAt(command.destination) == null) {
                    requireNotNull(world.get<Position>(monsterId)).moveTo(command.destination)
                }
            }
            AIPathCommand.Wait -> Unit
        }
    }

    private fun resolveAttack(
        attacker: EntityId,
        target: EntityId,
    ) {
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

        applyDamageResourceReactions(attacker, target, result.finalDamage)
        if (attacker == playerId) {
            recordSuccessfulPlayerAffinity(EquilibriumAffinity.PHYSICAL)
        }
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
        if (StatusEffectType.STEALTH in result.removedStatusTypes) {
            logStealthBroken(target, result.finalDamage)
        }

        if (result.targetKilled) {
            handleDeath(target, attacker)
        }
    }

    private fun handleDeath(
        target: EntityId,
        killer: EntityId?,
    ) {
        if (target != playerId && tryApplyBossFatalTransition(target)) {
            return
        }
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

        if (killer == playerId) {
            currentProfessionSchema()?.let { profession ->
                PlayerResourceService.onKill(world, playerId, profession)
            }
        }

        val activeBossTemplateId = activeBossDefinition()?.template?.id
        val defeatedBossTemplateId = world.get<MonsterTemplateId>(target)?.value
        val isBoss =
            currentFloor() == config.maxFloor &&
                activeBossTemplateId != null &&
                defeatedBossTemplateId == activeBossTemplateId

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
        if (killer == playerId) {
            grantShards(shardRewardForKill(isBoss = isBoss))
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
            defeatedBossTemplateId?.let { bossId ->
                worldProgress = worldProgress.withDefeatedBoss(bossId)
            }
            val objectiveCompleted = completeCurrentZoneQuest(ObjectiveCompletionTrigger.BOSS_DEFEAT)
            deathPoint?.let { point ->
                val bossReward = activeBossRewardItem()
                val stored = grantRewardItem(bossReward, point)
                recordMilestoneReward(
                    rewardSource = MilestoneRewardSource.BOSS,
                    sourceId = activeBossEncounterSchema()?.id ?: currentZoneSchema().id,
                    reward = bossReward,
                )
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
            if (objectiveCompleted) {
                currentObjectiveSetSchema()?.let { objective ->
                    addMessage("log.objective.complete", keyArg("objective", objective.nameKey))
                }
            }
            when (beginZoneAdvance()) {
                TransitionOutcome.REJECTED -> Unit
                TransitionOutcome.OPENED_ROUTE_SELECTION -> Unit
                TransitionOutcome.TRANSITIONED -> {
                    if (runOutcome.isTerminal) {
                        addMessage("log.victory", deathTargetArg)
                    } else {
                        addMessage("log.route.advance", keyArg("zone", currentZoneSchema().nameKey))
                    }
                }
            }
        }
    }

    private fun tryApplyBossFatalTransition(target: EntityId): Boolean {
        val bossState = world.get<BossEncounterState>(target) ?: return false
        val health = world.get<Health>(target) ?: return false
        if (health.current > 0) {
            return false
        }

        val phaseUpdate =
            updateBossPhaseIfNeeded(
                monsterId = target,
                transitionTiming = BossPhaseTransitionTiming.ALLOW_FATAL_TRANSITION,
                advanceTurnCounter = false,
            )
        if (!phaseUpdate.phaseChanged) {
            return false
        }

        health.current = maxOf(health.current, 1)
        bossState.phaseTurnCount = 0
        return true
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
        val previousLevel = experience.level
        val baseline = captureLevelUpFeedbackSnapshot()
        val result = ExperienceSystem.applyReward(experience = experience, reward = amount)
        if (result.levelsGained > 0) {
            val raceTalentDelta =
                RaceTalentPointProgression.deltaForLevelRange(
                    previousLevel = previousLevel,
                    nextLevel = experience.level,
                )
            if (raceTalentDelta > 0) {
                val bank = world.get<RaceTalentPointBank>(playerId) ?: RaceTalentPointBank().also { world.add(playerId, it) }
                bank.unspentPoints += raceTalentDelta
            }
        }
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

    private fun transitionFloor(direction: StairDirection): TransitionOutcome {
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
            return TransitionOutcome.REJECTED
        }

        if (direction == StairDirection.DOWN && currentFloor() == config.maxFloor && activeBossDefinition() == null) {
            val objectiveCompleted = completeCurrentZoneQuest(ObjectiveCompletionTrigger.ZONE_EXIT)
            if (objectiveCompleted) {
                currentObjectiveSetSchema()?.let { objective ->
                    addMessage("log.objective.complete", keyArg("objective", objective.nameKey))
                }
            }
            return when (beginZoneAdvance()) {
                TransitionOutcome.REJECTED -> TransitionOutcome.REJECTED
                TransitionOutcome.OPENED_ROUTE_SELECTION -> TransitionOutcome.OPENED_ROUTE_SELECTION
                TransitionOutcome.TRANSITIONED -> {
                    if (runOutcome.isTerminal) {
                        addMessage("log.victory.escape")
                    } else {
                        addMessage("log.route.advance", keyArg("zone", currentZoneSchema().nameKey))
                    }
                    TransitionOutcome.TRANSITIONED
                }
            }
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
        return TransitionOutcome.TRANSITIONED
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
        val serializedPendingActionIds =
            buildList {
                activeTurnActor
                    ?.takeIf { actorId -> actorId !in pendingActions }
                    ?.let { actorId -> add(actorId.value) }
                addAll(pendingActions.map(EntityId::value))
            }.distinct()
        return saveManager.save(
            SessionSnapshotMapper.toSaveSnapshot(
                config = config,
                currentFloor = currentFloor(),
                turnCount = turnCount,
                headlessTurnEquivalent = headlessTurnEquivalent,
                player = playerSnapshot,
                floors = floors,
                worldProgress = worldProgress,
                shardBalance = shardBalance,
                shopStates = shopStates(),
                combatRandomState = (combatRandomSource as? StatefulRandomSource)?.snapshotState(),
                sessionRandomState = (sessionRandom as? StatefulRandomSource)?.snapshotState(),
                milestoneRewards = persistedMilestoneRewardSummaries(),
                pendingActionIds = serializedPendingActionIds,
                activeTurnActorId = activeTurnActor?.value?.takeIf { actorId -> actorId in serializedPendingActionIds },
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

    private fun beginZoneAdvance(): TransitionOutcome {
        val plannedOption = plannedRouteAdvanceOption()
        if (plannedOption != null) {
            if (!worldProgress.satisfies(plannedOption.connection.gate)) {
                addMessage("log.route.blocked")
                return TransitionOutcome.REJECTED
            }
            completeZoneTransition(plannedOption)
            return TransitionOutcome.TRANSITIONED
        }
        if ("finale" in currentZoneSchema().tags) {
            finalizeVictory()
            return TransitionOutcome.TRANSITIONED
        }
        if (isolatedZoneSlice) {
            finalizeVictory()
            return TransitionOutcome.TRANSITIONED
        }

        val availableOptions = availableRouteAdvanceOptions()
        if (availableOptions.isEmpty()) {
            finalizeVictory()
            return TransitionOutcome.TRANSITIONED
        }
        if (availableOptions.size == 1) {
            completeZoneTransition(availableOptions.single())
            return TransitionOutcome.TRANSITIONED
        }
        pendingRouteSelection = availableOptions
        addMessage("log.route.selection.open", literalArg("count", availableOptions.size))
        return TransitionOutcome.OPENED_ROUTE_SELECTION
    }

    private fun completeZoneTransition(option: RouteAdvanceOption) {
        pendingRouteSelection = emptyList()
        worldProgress = worldProgress.withUnlockedRoute(option.connection.id)
        claimRouteReward(option, claimPolicy = RewardClaimPolicy.ON_ROUTE_UNLOCK, dropPoint = playerPosition())
        claimRouteReward(option, claimPolicy = RewardClaimPolicy.ON_FIRST_ROUTE_CLEAR, dropPoint = playerPosition())

        syncActiveFloorState()
        val nextRouteIndex = config.routeIndex + 1
        val nextRoute =
            if (plannedNextZoneId() == option.destinationZoneId) {
                config.zoneRoute
            } else {
                config.zoneRoute.take(config.routeIndex + 1) + option.destinationZoneId
            }
        val nextConfig =
            config.copy(
                floor = 1,
                zoneId = option.destinationZoneId,
                zoneRoute = nextRoute,
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
        activeShopId = null
        nextRuntime.initialMessages.forEach(::addMessage)
        checkpointRequested = true
        refreshFov()
    }

    private fun finalizeVictory() {
        runOutcome = RunOutcome.Victory(currentFloor())
        pendingRouteSelection = emptyList()
        pendingActions.clear()
        activeTurnActor = null
        saveManager.deleteSave()
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

    private fun restorePendingZoneAdvanceIfNeeded() {
        if (isolatedZoneSlice || runOutcome.isTerminal || currentFloor() != config.maxFloor || activeFloorState.stairsDown != null) {
            return
        }
        val activeBoss = activeBossDefinition() ?: return
        val bossAlive =
            world.entitiesWith(MonsterTemplateId::class, Health::class)
                .any { entityId ->
                    world.get<MonsterTemplateId>(entityId)?.value == activeBoss.template.id &&
                        (world.get<Health>(entityId)?.current ?: 0) > 0
                }
        if (!bossAlive) {
            pendingRouteSelection = availableRouteAdvanceOptions()
        }
    }

    private fun ensurePlayerResourcePools() {
        currentProfessionSchema()?.let { profession ->
            PlayerResourceService.sync(world, playerId, profession)
        }
    }

    private fun ensurePlayerInscriptions() {
        val loadout = world.get<InscriptionLoadout>(playerId) ?: InscriptionLoadout().also { world.add(playerId, it) }
        if (world.get<InscriptionCooldownState>(playerId) == null) {
            world.add(playerId, InscriptionCooldownState())
        }
        if (loadout.slots.isNotEmpty()) {
            return
        }
        val defaultsById = content.inscriptions.associateBy(InscriptionDef::id)
        listOf("healing_light", "phase_door", "iron_shield", "purge")
            .mapNotNull(defaultsById::get)
            .forEach { definition ->
                val equippedDefinitions = loadout.slots.mapNotNull { slot -> defaultsById[slot.inscriptionId] }
                InscriptionManager.equip(loadout, equippedDefinitions, definition)
            }
    }

    private fun recordSuccessfulPlayerAffinity(talentId: String) {
        val profession = currentProfessionSchema() ?: return
        val affinity = talentRegistry.get(talentId)?.equilibriumAffinity ?: return
        recordSuccessfulPlayerAffinity(affinity)
    }

    private fun recordSuccessfulPlayerAffinity(affinity: EquilibriumAffinity) {
        currentProfessionSchema()?.let { profession ->
            PlayerResourceService.recordSuccessfulAffinity(
                world = world,
                playerId = playerId,
                profession = profession,
                affinity = affinity,
            )
        }
    }

    private fun syncUnlockedPlayerTalents(
        notify: Boolean = false,
    ): List<String> {
        val profession = currentProfessionSchema() ?: return emptyList()
        val race = currentRaceSchema()
        val experience = world.get<Experience>(playerId) ?: return emptyList()
        val loadout = world.get<TalentLoadout>(playerId) ?: return emptyList()
        val unlockedTalentIds = TalentProgression.unlockedTalentIds(content.schemaCatalog, profession, experience.level, race)
        val newlyUnlockedTalentIds = mutableListOf<String>()
        unlockedTalentIds.forEach { talentId ->
            val wasUnlocked = talentId in loadout.talentLevels
            loadout.talentLevels.putIfAbsent(talentId, 1)
            if (!wasUnlocked) {
                newlyUnlockedTalentIds += talentId
            }
        }
        canonicalizePlayerLoadout(loadout)
        if (notify) {
            newlyUnlockedTalentIds.forEach { talentId ->
                addMessage("log.talent.unlock", keyArg("talent", talentNameKey(talentId)))
            }
        }
        return newlyUnlockedTalentIds
    }

    private fun canonicalizePlayerLoadout(loadout: TalentLoadout) {
        val activeTalentIds = linkedSetOf<String>()
        (1..PLAYER_ACTIVE_TALENT_SLOT_COUNT).forEach { slot ->
            loadout.talentIdAt(slot)
                ?.takeIf { talentId -> talentId in loadout.talentLevels }
                ?.takeUnless(activeTalentIds::contains)
                ?.let(activeTalentIds::add)
        }
        orderedUnlockedTalentIds(loadout).forEach { talentId ->
            if (activeTalentIds.size >= PLAYER_ACTIVE_TALENT_SLOT_COUNT) {
                return@forEach
            }
            activeTalentIds += talentId
        }
        loadout.slotToTalentId.clear()
        activeTalentIds.forEachIndexed { index, talentId ->
            loadout.slotToTalentId[index + 1] = talentId
        }
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
    ): PassiveDamageAdjustment {
        val targetTags = targetTagsFor(target)
        val passiveAdjustment =
            PassiveEffectResolver.resolveDamageAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, attacker),
                targetTags = targetTags,
                damageType = damageType,
            )
        val holyTagMultiplier = DamageFormula.tagDamageMultiplier(damageType, targetTags)
        val professionMultiplier = professionDamageMultiplier(attacker = attacker, target = target, damageType = damageType)
        return passiveAdjustment.copy(multiplier = passiveAdjustment.multiplier * holyTagMultiplier * professionMultiplier)
    }

    private fun professionDamageMultiplier(
        attacker: EntityId,
        target: EntityId,
        damageType: DamageType,
    ): Double {
        if (attacker != playerId && target != playerId) {
            return 1.0
        }
        val profession = currentProfessionSchema() ?: return 1.0
        return hateDamageMultiplier(attacker = attacker, target = target, profession = profession) *
            equilibriumDamageMultiplier(attacker = attacker, damageType = damageType, profession = profession)
    }

    private fun hateDamageMultiplier(
        attacker: EntityId,
        target: EntityId,
        profession: ProfessionSchemaV2,
    ): Double {
        if (profession.primarySpendAxis != ResourceAxis.HATE && profession.stateAxis != ResourceAxis.HATE) {
            return 1.0
        }
        val hate = world.get<com.ktome.core.resource.ResourcePools>(playerId)?.pool(ResourceType.HATE)?.current ?: return 1.0
        val outgoing =
            when {
                attacker != playerId -> 1.0
                hate >= 80 -> 1.20
                hate >= 60 -> 1.12
                hate >= 30 -> 1.05
                else -> 1.0
            }
        val incomingRisk =
            when {
                target != playerId -> 1.0
                hate >= 80 -> 1.12
                hate >= 60 -> 1.06
                else -> 1.0
            }
        return outgoing * incomingRisk
    }

    private fun equilibriumDamageMultiplier(
        attacker: EntityId,
        damageType: DamageType,
        profession: ProfessionSchemaV2,
    ): Double {
        if (attacker != playerId || profession.stateAxis != ResourceAxis.EQUILIBRIUM) {
            return 1.0
        }
        val profile = profession.resourceProfile(ResourceAxis.EQUILIBRIUM) ?: return 1.0
        val stableMin = profile.stableMin ?: return 1.0
        val stableMax = profile.stableMax ?: return 1.0
        val current = world.get<com.ktome.core.resource.ResourcePools>(playerId)?.pool(ResourceType.EQUILIBRIUM)?.current ?: return 1.0
        if (current in stableMin..stableMax) {
            return 1.0
        }
        val pressure =
            when {
                current < stableMin -> (stableMin - current).coerceAtMost(30)
                else -> (current - stableMax).coerceAtMost(30)
            }
        val favoredMultiplier = 1.0 + pressure / 100.0
        val penalizedMultiplier = (1.0 - pressure / 150.0).coerceAtLeast(0.8)
        return when {
            current < stableMin && damageType == DamageType.PHYSICAL -> favoredMultiplier
            current < stableMin -> penalizedMultiplier
            current > stableMax && damageType != DamageType.PHYSICAL -> favoredMultiplier
            else -> penalizedMultiplier
        }
    }

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
                val before = resolvePlayerResourceView()
                PlayerResourceService.onSuccessfulHit(world, playerId, profession)
                logPlayerResourceRestore(before)
            }
            if (target == playerId && damage > 0) {
                val before = resolvePlayerResourceView()
                PlayerResourceService.onDamageTaken(world, playerId, profession, damage)
                logPlayerResourceRestore(before)
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

    private fun com.ktome.core.talent.TalentResult.hasConfirmedResolutionSuccess(): Boolean =
        effects.any { effect -> effect !is com.ktome.core.talent.TalentEffectResult.Miss }

    private fun logPlayerResourceRestore(before: PlayerResourceView) {
        val after = resolvePlayerResourceView()
        if (after.typeId != before.typeId || after.current <= before.current) {
            return
        }
        addMessage(
            "log.talent.resource_restore",
            entityArg("target", playerId),
            literalArg("amount", after.current - before.current),
            keyArg("resource", resourceLabelKey(after.typeId)),
        )
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
            "merchant_stall" -> {
                val shop = configuredShopNode()
                if (shop == null) {
                    addMessage("log.interactable.none")
                    return CommandResolution.rejected()
                }
                activeShopId = shop.id
                addMessage("log.shop.open", keyArg("shop", shop.nameKey))
                return CommandResolution(accepted = true, consumesTurn = false)
            }

            in setOf("supply_crate", "trail_cache", "ore_stash", "seal_cache", "bandit_cache") -> {
                val rewardSpec = requireNotNull(groundRewardSpecFor(interactable.id)) {
                    "Missing ground reward spec for interactable '${interactable.id}'."
                }
                dropGroundRewardFromInteractable(schema, position, rewardSpec)
                objectiveProgressSpecFor(interactable.id)?.let { progress ->
                    recordObjectiveProgress(token = progress.token, stepKey = progress.stepKey)
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
                objectiveProgressSpecFor(interactable.id)?.let { progress ->
                    recordObjectiveProgress(token = progress.token, stepKey = progress.stepKey)
                }
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
                objectiveProgressSpecFor(interactable.id)?.let { progress ->
                    recordObjectiveProgress(token = progress.token, stepKey = progress.stepKey)
                }
            }

            in setOf(
                "armory_gate",
                "hunter_snare",
                "mine_furnace",
                "ritual_altar",
                "elven_wardstone",
                "molten_pressure_valve",
                "crystal_resonance_node",
                "river_ferry_anchor",
                "temple_ward_reliquary",
                "heart_ward_focus",
            ) -> {
                val rewardSpec = requireNotNull(supportRewardSpecFor(interactable.id)) {
                    "Missing support reward spec for interactable '${interactable.id}'."
                }
                grantSupportRewardFromInteractable(schema, position, rewardSpec)
                objectiveProgressSpecFor(interactable.id)?.let { progress ->
                    recordObjectiveProgress(token = progress.token, stepKey = progress.stepKey)
                }
                if (interactable.id in setOf("armory_gate", "hunter_snare", "mine_furnace", "ritual_altar")) {
                    addMessage("log.objective.advance")
                }
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
                    logEvent(
                        StatusAppliedEvent(
                            target = effect.target,
                            statusType = effect.type,
                            statusId = effect.statusId,
                            source = result.user,
                            remainingTurns = effect.duration,
                        ),
                    )
                    addMessage(
                        when (effect.statusId) {
                            "war_cry_empower" -> "log.talent.target_empowered"
                            "war_cry_shaken" -> "log.talent.target_shaken"
                            else -> "log.talent.target_affected"
                        },
                        entityArg("target", effect.target),
                        literalArg("turns", effect.duration),
                    )
                    effect.interactionId?.let { interactionId ->
                        logStatusInteraction(effect.target, effect.type, effect.statusId, interactionId, result.user, effect.previousSource)
                    }
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
                    if (effect.stealthBroken) {
                        logStealthBroken(effect.target, effect.amount)
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
                        keyArg("resource", resourceLabelKey(effect.resourceTypeId)),
                    )
                }

                is com.ktome.core.talent.TalentEffectResult.StatusCleanse -> {
                    effect.removed.forEachIndexed { index, removedType ->
                        logEvent(
                            StatusCleanseEvent(
                                target = effect.target,
                                statusType = removedType,
                                statusId = effect.removedStatusIds.getOrElse(index) { removedType.schemaId },
                            ),
                        )
                    }
                    if (effect.removed.isNotEmpty()) {
                        addMessage(
                            "log.status.cleanse",
                            entityArg("target", effect.target),
                            literalArg("count", effect.removed.size),
                        )
                    }
                }

                is com.ktome.core.talent.TalentEffectResult.StatusApplied -> {
                    logEvent(
                        StatusAppliedEvent(
                            target = effect.target,
                            statusType = effect.type,
                            statusId = effect.statusId,
                            source = result.user,
                            remainingTurns = effect.duration,
                        ),
                    )
                    addMessage(
                        when (effect.statusId) {
                            StatusEffectType.STUN.schemaId -> "log.talent.target_stunned"
                            StatusEffectType.ARMOR_BREAK.schemaId -> "log.talent.target_armor_broken"
                            else -> "log.talent.target_affected"
                        },
                        entityArg("target", effect.target),
                        literalArg("turns", effect.duration),
                    )
                    effect.interactionId?.let { interactionId ->
                        logStatusInteraction(effect.target, effect.type, effect.statusId, interactionId, result.user, effect.previousSource)
                    }
                }
            }
        }
    }

    private fun logStatusInteraction(
        target: EntityId,
        type: StatusEffectType,
        statusId: String,
        interactionId: String,
        source: EntityId,
        previousSource: EntityId?,
    ) {
        when (interactionId) {
            "TAUNT_OVERRIDE" -> {
                logEvent(
                    TauntOverrideEvent(
                        target = target,
                        statusType = type,
                        statusId = statusId,
                        previousSource = previousSource,
                        newSource = source,
                    ),
                )
                addMessage(
                    "log.status.taunt_override",
                    entityArg("target", target),
                    entityArg("source", source),
                    previousSource?.let { entityArg("previous", it) } ?: literalArg("previous", tr("actor.unknown.name")),
                )
            }

            else -> {
                logEvent(
                    StatusInteractionEvent(
                        target = target,
                        statusType = type,
                        statusId = statusId,
                        interactionId = interactionId,
                    ),
                )
                addStatusInteractionMessage(target, interactionId)
            }
        }
    }

    private fun addStatusInteractionMessage(
        target: EntityId,
        interactionId: String,
    ) {
        when (interactionId) {
            "FREEZE_OVERWRITTEN_BY_BURN" ->
                addMessage(
                    "log.status.freeze_overwritten_by_burn",
                    entityArg("target", target),
                )

            "BURN_OVERWRITTEN_BY_FREEZE" ->
                addMessage(
                    "log.status.burn_overwritten_by_freeze",
                    entityArg("target", target),
                )
        }
    }

    private fun logStealthBroken(
        target: EntityId,
        damage: Int,
    ) {
        logEvent(StealthBrokenEvent(target, damage = damage))
        addMessage(
            "log.status.stealth_broken",
            entityArg("target", target),
            literalArg("damage", damage),
        )
    }

    private fun randomTeleportDestination(maxRange: Int? = null): Point {
        val occupied = occupiedBlockingTiles(excluding = playerId)
        val origin = playerPosition()
        val candidates =
            map.floorPoints().filter { point ->
                point !in occupied &&
                    !map[point].blocksMovement &&
                    (maxRange == null || origin.chebyshevDistanceTo(point) <= maxRange)
            }
        if (candidates.isEmpty()) {
            return origin
        }
        return candidates[sessionRandom.nextInt(0, candidates.size)]
    }

    private fun resolveControlledTeleportDestination(
        target: Point?,
        maxRange: Int,
    ): Point? {
        val destination = target ?: return null
        if (!map.isInBounds(destination.x, destination.y)) {
            return null
        }
        if (playerPosition().chebyshevDistanceTo(destination) > maxRange) {
            return null
        }
        if (map[destination].blocksMovement) {
            return null
        }
        if (destination in occupiedBlockingTiles(excluding = playerId)) {
            return null
        }
        return destination
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
        if (shouldIncludeInRunSummary(message.message)) {
            if (recentSummaryEvents.size == SUMMARY_EVENT_LIMIT) {
                recentSummaryEvents.removeFirst()
            }
            recentSummaryEvents += message.message
        }
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

    private fun shouldIncludeInRunSummary(message: RenderTextTokenSnapshot): Boolean = message.key != "log.zone.enter"

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
        content.statusSchemaFor(type.schemaId)?.nameKey
            ?: if (type == StatusEffectType.CUSTOM) {
                "status.custom"
            } else {
                StatusDefinitions.nameKey(type)
            }

    private fun statusNameKey(statusId: String): String =
        content.statusSchemaFor(statusId)?.nameKey
            ?: content.statusCatalog.definitionOrNull(statusId)?.nameKey
            ?: if (StatusEffectType.fromSchemaId(statusId) == StatusEffectType.CUSTOM) {
                "status.custom"
            } else {
                statusEffectNameKey(StatusEffectType.fromSchemaId(statusId))
            }

    private fun EffectCategory.toSnapshotCategory(): StatusEffectCategorySnapshot =
        when (this) {
            EffectCategory.BUFF -> StatusEffectCategorySnapshot.BUFF
            EffectCategory.DEBUFF -> StatusEffectCategorySnapshot.DEBUFF
            EffectCategory.NEUTRAL -> StatusEffectCategorySnapshot.NEUTRAL
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
        return ItemGenerator(content.itemBundle, sessionRandom).generate(
            base = chooseWeightedLootItem(floorCandidates),
            floor = currentFloor(),
            affixContext = currentAffixBuildContext(),
        )
    }

    private fun currentAffixBuildContext(): AffixSelectionContext {
        val profession = currentProfessionSchema() ?: return AffixSelectionContext()
        val loadout = world.get<TalentLoadout>(playerId)
        val unlockedTalentIds = loadout?.let(::orderedUnlockedTalentIds)?.toSet() ?: profession.startingTalents.toSet()
        return professionAffixBuildContext(
            schemaCatalog = content.schemaCatalog,
            profession = profession,
            unlockedTalentIds = unlockedTalentIds,
        )
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
            is StatusAppliedEvent -> "status_apply:${event.target.value}:${event.statusId}:${event.remainingTurns}"
            is StatusCleanseEvent -> "status_cleanse:${event.target.value}:${event.statusId}:${event.reason}"
            is StatusTickEvent -> "status_tick:${event.target.value}:${event.statusId}:${event.damage}:${event.carrierKind.name}"
            is StatusInteractionEvent -> "status_interaction:${event.target.value}:${event.statusId}:${event.interactionId}"
            is TauntOverrideEvent -> "taunt_override:${event.target.value}:${event.statusId}:${event.newSource?.value ?: "none"}"
            is StealthBrokenEvent -> "stealth_break:${event.target.value}:${event.statusId}:${event.damage}"
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

    private enum class TransitionOutcome {
        REJECTED,
        OPENED_ROUTE_SELECTION,
        TRANSITIONED,
    }

    companion object {
        private const val COMBAT_RANDOM_SALT: Long = 0xC0FFEE
        private const val SESSION_RANDOM_SALT: Long = 0x51A17A

        private fun compatibilityContent(
            config: FoundationGameConfig,
            talentRegistry: TalentRegistry,
            world: World,
            currentFloor: Int,
        ): GameContent {
            val monsterCatalog = compatibilityMonsterCatalog(world, currentFloor)
            val compatibilityBossEncounterId =
                world.entitiesWith(BossEncounterState::class)
                    .firstOrNull()
                    ?.let { "compatibility_boss_encounter" }
            return GameContent(
                talents = emptyList(),
                statuses = emptyList(),
                statusCatalog = com.ktome.core.status.StatusCatalog.EMPTY,
                talentRegistry = talentRegistry,
                monsterCatalog = monsterCatalog,
                itemBundle = compatibilityItemBundle(world, currentFloor),
                bossDefinitions =
                    mapOf(
                        "compatibility_boss_encounter" to
                            BossDefinition(
                                encounterId = "compatibility_boss_encounter",
                                encounter =
                                    com.ktome.core.ai.BossEncounter(
                                        id = "compatibility_boss_encounter",
                                        templateId = "compatibility_boss",
                                        phases = emptyList(),
                                    ),
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
                                nameKey = "boss.compatibility_boss.name",
                                descKey = "boss.compatibility_boss.desc",
                                visualKey = "boss.cultist.dungeon_lord.visual",
                                iconKey = "boss.cultist.dungeon_lord.icon",
                                audioProfile = "audio.boss.warning",
                            ),
                    ),
                schemaCatalog =
                    SchemaCatalog(
                        professions = emptyList(),
                        statuses = emptyList(),
                        talents = emptyList(),
                        talentTrees = emptyList(),
                        monsters = emptyList(),
                        bossEncounters = emptyList(),
                        telegraphSpecs = emptyList(),
                        threatProfiles = emptyList(),
                        zones =
                            listOf(
                                ZoneSchemaV2(
                                    id = config.zoneId,
                                    nameKey = "zone.${config.zoneId}.name",
                                    descKey = "zone.${config.zoneId}.desc",
                                    visualKey = "zone.${config.zoneId}.visual",
                                    iconKey = "zone.${config.zoneId}.icon",
                                    audioProfile = "audio.zone.${config.zoneId}",
                                    schemaVersion = 2,
                                    tags = listOf("zone", "compatibility"),
                                    biome = "compatibility",
                                    floorCount = config.maxFloor,
                                    mapSize = SchemaMapSize(width = config.width, height = config.height),
                                    recommendedLevel = SchemaLevelRange(min = 1, max = maxOf(1, currentFloor)),
                                    environmentTheme = "compatibility",
                                    specialMechanics = emptyList(),
                                    tilesetKey = "tileset.compatibility",
                                    ambientProfile = "ambient.compatibility",
                                    worldRole = "compatibility",
                                    monsterPools = monsterCatalog.map(MonsterTemplate::id),
                                    elitePools = emptyList(),
                                    bossEncounterId = compatibilityBossEncounterId,
                                    objectiveSetId = null,
                                ),
                            ),
                        interactables = emptyList(),
                        objectiveSets = emptyList(),
                        difficulties = emptyList(),
                        itemBundle = ItemBundleSchemaV2(materials = emptyList(), affixes = emptyList(), items = emptyList()),
                        lootProfiles = emptyList(),
                        races = emptyList(),
                        inscriptions = emptyList(),
                        tilesets = emptyList(),
                        aiProfiles = emptyList(),
                        arenas = emptyList(),
                        ambientProfiles = emptyList(),
                        visualKeys = emptySet(),
                        audioProfiles = emptySet(),
                    ),
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                telegraphRegistry = com.ktome.game.telegraph.TelegraphRegistry(emptyMap()),
                threatProfileRegistry = com.ktome.game.telegraph.ThreatProfileRegistry(emptyMap()),
            )
        }

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

        private val DETERMINISTIC_RESCUE_UTILITY_BASE_IDS: Set<String> =
            setOf(
                "healing_potion",
                "scroll_teleport",
                "mana_potion",
                "stamina_draught",
                "energy_tonic",
                "consecrated_oil",
            )

        private val MILESTONE_REPLACEMENT_SLOT_PRIORITY: List<EquipSlot> =
            listOf(EquipSlot.OFF_HAND, EquipSlot.ARMOR, EquipSlot.WEAPON)

        private data object UntrackedRandomSource : RandomSource {
            override fun nextDouble(): Double = 0.0

            override fun nextInt(
                fromInclusive: Int,
                untilExclusive: Int,
            ): Int = fromInclusive
        }
    }

}

internal fun routeRescueHintLabelKeys(rescueTags: Set<String>): List<String> =
    rescueTags
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .sortedBy(String::uppercase)
        .mapNotNull(::routeRescueHintLabelKey)
        .distinct()
        .toList()

internal fun routeRescueHintLabelKey(rescueTag: String): String? =
    when (rescueTag.trim().uppercase()) {
        "MOVEMENT" -> "ui.world_map.route_trait.movement"
        "RECOVERY" -> "ui.world_map.route_trait.recovery"
        "PROTECTION" -> "ui.world_map.route_trait.protection"
        "CLEANSING" -> "ui.world_map.route_trait.cleansing"
        "ARCANE" -> "ui.world_map.route_trait.arcane"
        "FIRE" -> "ui.world_map.route_trait.fire"
        else -> null
    }
