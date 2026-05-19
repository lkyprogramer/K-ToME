package com.ktome.game.harness

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemType
import com.ktome.core.loot.RarityTier
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.RouteSelectionSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.snapshot.TalentTreeNodeSnapshot
import com.ktome.core.snapshot.TalentTreeSnapshot
import com.ktome.game.FoundationGameSession
import com.ktome.game.InventoryItemView
import com.ktome.game.PlayerResourceView
import com.ktome.game.PlayerStatus
import com.ktome.game.PlayerCommand
import com.ktome.game.SecondaryPlayerResourceView
import com.ktome.game.TalentReserveView
import com.ktome.game.TalentSlotView
import com.ktome.game.TalentTreeNodeView
import com.ktome.game.TalentTreeView
import com.ktome.game.validation.ValidationAction
import com.ktome.core.talent.TalentTreeOwnerType
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal class StallDetector(
    private val maxRepeats: Int = 20,
) {
    private var lastSignature: String? = null
    private var repeatCount: Int = 0

    fun observe(observation: RunObservation): String? {
        val signature = observation.signature()
        if (signature == lastSignature) {
            repeatCount += 1
        } else {
            lastSignature = signature
            repeatCount = 1
        }
        return if (repeatCount >= maxRepeats) {
            "Repeated state $signature for $repeatCount observations."
        } else {
            null
        }
    }

    fun reset() {
        lastSignature = null
        repeatCount = 0
    }
}

object RunObservationCapture {
    fun capture(
        session: FoundationGameSession,
        turnIndex: Int,
    ): RunObservation {
        val snapshot = session.renderSnapshot()
        val actors = snapshot.actors
        val uiState = snapshot.uiState
        val playerStatusSnapshot = uiState.playerStatus
        val mapCells = snapshot.mapCells
        val visibleTiles = mapCells.asSequence()
            .filter { cell -> cell.visibility == CellVisibilitySnapshot.VISIBLE }
            .map { cell -> Point(cell.x, cell.y) }
            .toSet()
        val exploredTiles = mapCells.asSequence()
            .filter { cell -> cell.visibility != CellVisibilitySnapshot.HIDDEN }
            .map { cell -> Point(cell.x, cell.y) }
            .toSet()
        val knownDownstairsPositions =
            mapCells
                .asSequence()
                .filter { cell -> cell.visibility != CellVisibilitySnapshot.HIDDEN && cell.stairDirectionId == StairDirection.DOWN.name }
                .map { cell -> Point(cell.x, cell.y) }
                .distinct()
                .sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
                .toList()

        val visibleGroundItemPositions =
            mapCells
                .asSequence()
                .filter { cell -> cell.visibility == CellVisibilitySnapshot.VISIBLE && cell.items.isNotEmpty() }
                .map { cell -> Point(cell.x, cell.y) }
                .distinct()
                .sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
                .toList()
        val interactableTagsByType = linkedMapOf<String, Set<String>>()
        val visibleInteractables =
            snapshot.props
                .asSequence()
                .filter { prop -> prop.propTypeId != "stairs" }
                .map { prop ->
                    ObservedInteractable(
                        id = prop.propTypeId,
                        position = Point(prop.x, prop.y),
                        interactionTags =
                            interactableTagsByType.getOrPut(prop.propTypeId) {
                                session.automationInteractableTags(prop.propTypeId)
                            },
                    )
                }
                .distinctBy { interactable -> interactable.id to interactable.position }
                .sortedWith(compareBy<ObservedInteractable> { it.position.y }.thenBy { it.position.x })
                .toList()

        val visibleBlockingPositions =
            actors
                .filterNot { it.isPlayer }
                .map { actor -> Point(actor.x, actor.y) }
                .toSet()
        val visibleBossPositions =
            actors
                .asSequence()
                .filter { actor -> !actor.isPlayer && actor.roleKind == ActorRoleKindSnapshot.BOSS }
                .map { actor -> Point(actor.x, actor.y) }
                .distinct()
                .sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
                .toList()
        val playerActor = actors.firstOrNull { actor -> actor.isPlayer }
        val activeShop = uiState.activeShop

        return RunObservation(
            zoneId = snapshot.metadata.zoneId,
            floor = session.currentFloor(),
            turnIndex = turnIndex,
            playerStatus = playerStatusSnapshot.toPlayerStatus(),
            playerResource = playerStatusSnapshot.toPlayerResourceView(),
            shardBalance = uiState.shardBalance,
            playerPosition = session.playerPosition(),
            map = session.map,
            visibleTiles = visibleTiles,
            exploredTiles = exploredTiles,
            visibleHostilePositions = uiState.targetablePositions.map { point -> Point(point.x, point.y) },
            visibleBossPositions = visibleBossPositions,
            visibleBlockingPositions = visibleBlockingPositions,
            visibleGroundItemPositions = visibleGroundItemPositions,
            visibleInteractables = visibleInteractables,
            knownDownstairsPositions = knownDownstairsPositions,
            searchPromptAvailable = uiState.searchPromptLabelKey != null,
            playerStatusTypeIds = playerActor?.statusEffects?.mapTo(linkedSetOf()) { status -> status.typeId }.orEmpty(),
            activeRouteSelection = uiState.activeRouteSelection,
            activeShopId = activeShop?.shopId,
            activeShopOffers =
                activeShop
                    ?.offers
                    ?.map { offer ->
                        ObservedShopOffer(
                            index = offer.index,
                            offerFingerprint = offer.offerFingerprint,
                            price = offer.price,
                            tags = offer.tags.toSet(),
                            purchasable = session.automationCanPurchaseShopOffer(offer.index),
                        )
                    }.orEmpty(),
            activeInscriptionReplacementPrompt =
                activeShop?.inscriptionReplacementPrompt?.let { prompt ->
                    ObservedInscriptionReplacementPrompt(
                        offerIndex = prompt.offerIndex,
                        offerFingerprint = prompt.offerFingerprint,
                        candidateInscriptionId = prompt.candidate.inscriptionId,
                        candidateCategoryId = prompt.candidate.categoryId,
                        upgradeFromInscriptionId = prompt.candidate.upgradeFromInscriptionId,
                        categoryLimit = prompt.categoryChanges.firstOrNull()?.limit ?: Int.MAX_VALUE,
                        currentSlots =
                            prompt.currentSlots.mapNotNull { slot ->
                                slot.hotkey?.let { hotkey ->
                                    ObservedInscriptionReplacementSlot(
                                        hotkey = hotkey,
                                        inscriptionId = slot.inscriptionId,
                                        categoryId = slot.categoryId,
                                    )
                                }
                            },
                    )
                },
            inventoryItems = uiState.inventory.map(::toInventoryItemView),
            inscriptions =
                uiState.inscriptions.map { inscription ->
                    ObservedInscription(
                        hotkey = inscription.hotkey,
                        inscriptionId = inscription.inscriptionId,
                        cooldownRemaining = inscription.cooldownRemaining,
                        requiresTarget = inscription.requiresTarget,
                    )
                },
            talentSlots = uiState.talents.map(::toTalentSlotView),
            reserveTalents = uiState.reserveTalents.map(::toTalentReserveView),
            talentTrees = uiState.talentTrees.map(::toTalentTreeView),
            canAscend = session.canAscend(),
            canDescend = session.canDescend(),
            runOutcome = session.runOutcome(),
            messageLogTail = session.messageLog().takeLast(12),
            eventTail = session.recentEventLog(12),
        )
    }
}

private fun PlayerStatusSnapshot.toPlayerStatus(): PlayerStatus =
    PlayerStatus(
        currentHp = currentHp,
        maxHp = maxHp,
        level = level,
        currentExperience = currentExperience,
        nextLevelRequirement = nextLevelRequirement,
        statPoints = statPoints,
        talentPoints = talentPoints,
        raceTalentPoints = raceTalentPoints,
        attack = attack,
        defense = defense,
        accuracy = accuracy,
        evasion = evasion,
        speed = speed,
        castSpeedRating = castSpeedRating,
        effectiveCastSpeed = effectiveCastSpeed,
    )

private fun PlayerStatusSnapshot.toPlayerResourceView(): PlayerResourceView =
    PlayerResourceView(
        current = currentResource,
        max = maxResource,
        typeId = resourceTypeId,
        stableMin = resourceStableMin,
        stableMax = resourceStableMax,
        secondary =
            secondaryResourceView(
                current = secondaryResourceCurrent,
                max = secondaryResourceMax,
                typeId = secondaryResourceTypeId,
                stableMin = secondaryResourceStableMin,
                stableMax = secondaryResourceStableMax,
            ),
    )

private fun secondaryResourceView(
    current: Int?,
    max: Int?,
    typeId: String?,
    stableMin: Int?,
    stableMax: Int?,
): SecondaryPlayerResourceView? {
    if (current == null || max == null || typeId == null) {
        return null
    }
    return SecondaryPlayerResourceView(
        current = current,
        max = max,
        typeId = typeId,
        stableMin = stableMin,
        stableMax = stableMax,
    )
}

private fun toInventoryItemView(entry: InventoryEntrySnapshot): InventoryItemView {
    val item = entry.item
    return InventoryItemView(
        index = entry.index,
        name = item.displayName?.key ?: item.nameKey,
        baseItemId = item.baseItemId,
        specialTemplateId = item.specialTemplateId,
        type = ItemType.valueOf(item.typeId),
        slot = item.slotId?.let(EquipSlot::valueOf),
        equippedSlot = entry.equippedSlotId?.let(EquipSlot::valueOf),
        quality = RarityTier.valueOf(item.qualityTierId),
        affixIds = item.affixIds,
        effect = item.effectTypeId?.let(ConsumableEffect::valueOf),
        resourceTypeId = item.resourceTypeId,
        magnitude = item.magnitude,
    )
}

private fun toTalentSlotView(snapshot: TalentSlotSnapshot): TalentSlotView =
    TalentSlotView(
        slot = snapshot.slot,
        talentId = snapshot.talentId,
        name = snapshot.nameKey,
        descKey = snapshot.descKey,
        ownerType = TalentTreeOwnerType.valueOf(snapshot.ownerType),
        level = snapshot.level,
        committedLevel = snapshot.committedLevel,
        maxLevel = snapshot.maxLevel,
        resourceCost = snapshot.resourceCost,
        resourceTypeId = snapshot.resourceTypeId,
        range = snapshot.range,
        minRange = snapshot.minRange,
        currentCooldown = snapshot.currentCooldown,
        maxCooldown = snapshot.maxCooldown,
        requiresTarget = snapshot.requiresTarget,
        descriptionModel = null,
        nextBreakpointPreview = snapshot.nextBreakpointPreview,
        hasPendingAllocation = snapshot.hasPendingAllocation,
    )

private fun toTalentReserveView(snapshot: TalentReserveSnapshot): TalentReserveView =
    TalentReserveView(
        talentId = snapshot.talentId,
        name = snapshot.nameKey,
        descKey = snapshot.descKey,
        ownerType = TalentTreeOwnerType.valueOf(snapshot.ownerType),
        level = snapshot.level,
        committedLevel = snapshot.committedLevel,
        maxLevel = snapshot.maxLevel,
        resourceCost = snapshot.resourceCost,
        resourceTypeId = snapshot.resourceTypeId,
        range = snapshot.range,
        minRange = snapshot.minRange,
        currentCooldown = snapshot.currentCooldown,
        maxCooldown = snapshot.maxCooldown,
        requiresTarget = snapshot.requiresTarget,
        descriptionModel = null,
        nextBreakpointPreview = snapshot.nextBreakpointPreview,
        hasPendingAllocation = snapshot.hasPendingAllocation,
    )

private fun toTalentTreeView(snapshot: TalentTreeSnapshot): TalentTreeView =
    TalentTreeView(
        treeId = snapshot.treeId,
        ownerType = TalentTreeOwnerType.valueOf(snapshot.ownerType),
        treeOwnerId = snapshot.treeOwnerId,
        nameKey = snapshot.nameKey,
        nodes = snapshot.nodes.map(::toTalentTreeNodeView),
    )

private fun toTalentTreeNodeView(snapshot: TalentTreeNodeSnapshot): TalentTreeNodeView =
    TalentTreeNodeView(
        talentId = snapshot.talentId,
        treeId = snapshot.treeId,
        ownerType = TalentTreeOwnerType.valueOf(snapshot.ownerType),
        treeOwnerId = snapshot.treeOwnerId,
        nameKey = snapshot.nameKey,
        category = snapshot.category,
        state = snapshot.state,
        level = snapshot.rank,
        committedLevel = snapshot.committedRank,
        maxLevel = snapshot.maxRank,
        unlockLevel = snapshot.unlockLevel,
        hasPendingAllocation = snapshot.hasPendingAllocation,
    )

fun RunObservation.signature(): String =
    buildString {
        append(floor)
        append('|')
        append(playerPosition.x)
        append(',')
        append(playerPosition.y)
        append('|')
        append(playerStatus.currentHp)
        append('/')
        append(playerStatus.maxHp)
        append('|')
        append(playerResource.typeId)
        append(':')
        append(playerResource.current)
        append('/')
        append(playerResource.max)
        append('|')
        append(playerStatus.level)
        append('|')
        append(playerStatus.attack)
        append('/')
        append(playerStatus.defense)
        append('/')
        append(playerStatus.speed)
        append('|')
        append(
            talentSlots.joinToString(separator = ",") { slot ->
                "${slot.slot}:${slot.talentId}:${slot.level}:${slot.committedLevel}:${slot.hasPendingAllocation}:${slot.currentCooldown}"
            },
        )
        append('|')
        append(
            reserveTalents.joinToString(separator = ",") { talent ->
                "${talent.talentId}:${talent.level}:${talent.committedLevel}:${talent.hasPendingAllocation}:${talent.currentCooldown}"
            },
        )
        append('|')
        append(
            talentTrees.joinToString(separator = ",") { tree ->
                tree.nodes.joinToString(separator = ";") { node ->
                    "${tree.treeId}:${node.talentId}:${node.state}:${node.level}:${node.committedLevel}:${node.hasPendingAllocation}"
                }
            },
        )
        append('|')
        append(inventoryItems.size)
        append('|')
        append(visibleHostilePositions.size)
    }

fun stepToward(
    observation: RunObservation,
    target: Point,
): Point? =
    AStar.findPath(
        map = observation.map,
        start = observation.playerPosition,
        goal = target,
        blocked = observation.visibleBlockingPositions - target,
    ).getOrNull(1)

fun Point.deltaFrom(origin: Point): Point =
    Point(
        x = (x - origin.x).coerceIn(-1, 1),
        y = (y - origin.y).coerceIn(-1, 1),
    )

fun routeProgressCommand(
    session: FoundationGameSession,
    observation: RunObservation,
): PlayerCommand? =
    routeProgressCommand(
        session = session,
        observation = observation,
        pendingObjectivePoint = session.automationPendingObjectiveInteractablePoint(),
    )

fun routeProgressCommandWithoutObjectiveHook(
    session: FoundationGameSession,
    observation: RunObservation,
): PlayerCommand? {
    return routeProgressCommand(
        session = session,
        observation = observation,
        pendingObjectivePoint = null,
    )
}

private fun routeProgressCommand(
    session: FoundationGameSession,
    observation: RunObservation,
    pendingObjectivePoint: Point?,
): PlayerCommand? {
    if (observation.activeShopId != null) {
        return null
    }

    observation.activeRouteSelection?.let { routeSelection ->
        return PlayerCommand.SelectRoute(preferredRouteIndex(routeSelection))
    }

    val occupiedTiles by lazy {
        session.automationWorld()
            .entitiesWith(Position::class, BlocksMovement::class)
            .filter { entityId -> entityId != session.playerId }
            .map { entityId -> requireNotNull(session.automationWorld().get<Position>(entityId)).toPoint() }
            .toSet()
    }
    val hazardTiles by lazy { session.automationZoneHazardPoints() - observation.playerPosition }

    val canPursueObjectiveHook =
        pendingObjectivePoint != null &&
            observation.visibleBossPositions.isEmpty() &&
            observation.visibleHostilePositions.none { hostile ->
                hostile.chebyshevDistanceTo(observation.playerPosition) <= 2
            }
    if (canPursueObjectiveHook) {
        val objectivePoint = requireNotNull(pendingObjectivePoint)
        if (objectivePoint == observation.playerPosition) {
            return PlayerCommand.Interact
        }
        val path =
            AStar.findPath(
                map = session.map,
                start = observation.playerPosition,
                goal = objectivePoint,
                blocked = (occupiedTiles + hazardTiles) - objectivePoint,
            )
        path.getOrNull(1)?.let { nextStep ->
            return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
        }
    }

    if (observation.canDescend) {
        return PlayerCommand.Descend
    }

    val closeVisibleHostile =
        observation.visibleHostilePositions.any { hostile ->
            hostile.chebyshevDistanceTo(observation.playerPosition) <= 3
        }
    if (observation.playerResource.typeId == "ENERGY" && closeVisibleHostile) {
        return null
    }

    val stairsDown = session.automationStairPoint(StairDirection.DOWN)
    if (stairsDown != null) {
        if (stairsDown == observation.playerPosition) {
            return PlayerCommand.Descend
        }
        val path =
            AStar.findPath(
                map = session.map,
                start = observation.playerPosition,
                goal = stairsDown,
                blocked = (occupiedTiles + hazardTiles) - stairsDown,
            )
        val nextStep = path.getOrNull(1) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    val bossPoint = session.automationBossPoint()
    if (bossPoint != null && observation.visibleBossPositions.isEmpty()) {
        val path =
            AStar.findPath(
                map = session.map,
                start = observation.playerPosition,
                goal = bossPoint,
                // Offscreen boss pursuit must not freeze on transient hazard telegraphs,
                // otherwise the harness can orbit outside the arena without ever reacquiring the boss.
                blocked = occupiedTiles - bossPoint,
            )
        val nextStep = path.getOrNull(1) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }
    return null
}

internal fun preferredRouteIndex(routeSelection: RouteSelectionSnapshot): Int {
    val mainlineOption =
        routeSelection.options.firstOrNull { option ->
            !option.isReturnPath && option.destinationZoneId !in OPTIONAL_ROUTE_ZONE_IDS
        }
    if (mainlineOption != null) {
        return mainlineOption.index
    }

    val branchOption = routeSelection.options.firstOrNull { option -> !option.isReturnPath }
    return branchOption?.index ?: routeSelection.options.firstOrNull()?.index ?: 0
}

fun PlayerCommand.commandName(): String = this::class.simpleName ?: "UnknownCommand"

fun renderCommand(command: PlayerCommand): String =
    when (command) {
        is PlayerCommand.Move -> "Move(${command.delta.x},${command.delta.y})"
        is PlayerCommand.UseTalent ->
            command.target?.let { target -> "UseTalent(${command.slot},${target.x},${target.y})" } ?: "UseTalent(${command.slot})"
        is PlayerCommand.UseInscription ->
            command.target?.let { target -> "UseInscription(${command.hotkey},${target.x},${target.y})" } ?: "UseInscription(${command.hotkey})"
        is PlayerCommand.ActivateInventoryItem -> "ActivateInventoryItem(${command.index})"
        is PlayerCommand.DropInventoryItem -> "DropInventoryItem(${command.index})"
        is PlayerCommand.BuyShopOffer -> "BuyShopOffer(${command.index})"
        is PlayerCommand.SellInventoryItem -> "SellInventoryItem(${command.index})"
        is PlayerCommand.SelectRoute -> "SelectRoute(${command.index})"
        is PlayerCommand.EquipTalentToSlot -> "EquipTalentToSlot(${command.slot},${command.talentId})"
        is PlayerCommand.AssignStat -> "AssignStat(${command.stat.name})"
        is PlayerCommand.AssignTalent -> "AssignTalent(${command.talentId})"
        is PlayerCommand.RespecTalentTree -> "RespecTalentTree(${command.ownerType},${command.treeOwnerId})"
        is PlayerCommand.ConfirmTalentDraftReplacingSlot -> "ConfirmTalentDraftReplacingSlot(${command.slot})"
        is PlayerCommand.Validation -> "Validation(${command.action})"
        else -> command.commandName()
    }

fun PlayerCommand.consumesTurn(): Boolean =
    when (this) {
        PlayerCommand.Wait,
        is PlayerCommand.Move,
        PlayerCommand.Interact,
        PlayerCommand.Search,
        PlayerCommand.PickUp,
        PlayerCommand.Ascend,
        PlayerCommand.Descend,
        is PlayerCommand.UseTalent,
        is PlayerCommand.UseInscription,
        is PlayerCommand.ActivateInventoryItem,
        -> true

        PlayerCommand.CloseShop,
        PlayerCommand.CancelInscriptionReplacementPurchase,
        is PlayerCommand.DropInventoryItem,
        is PlayerCommand.BuyShopOffer,
        is PlayerCommand.SellInventoryItem,
        is PlayerCommand.SelectRoute,
        is PlayerCommand.EquipTalentToSlot,
        is PlayerCommand.AssignStat,
        is PlayerCommand.AssignTalent,
        is PlayerCommand.RespecTalentTree,
        PlayerCommand.ConfirmTalentDraft,
        PlayerCommand.ConfirmTalentDraftToReserve,
        is PlayerCommand.ConfirmTalentDraftReplacingSlot,
        PlayerCommand.RollbackTalentDraft,
        PlayerCommand.SaveGame,
        -> false

        is PlayerCommand.Validation ->
            when (this.action) {
                ValidationAction.ExecuteSearch -> true
                else -> false
            }
    }

object HarnessReportWriter {
    fun reportDir(): Path {
        val configured = System.getProperty("ktome.harness.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("build", "reports", "harness")
        } else {
            Path.of(configured)
        }
    }

    fun writeJsonAndMarkdown(
        fileStem: String,
        payload: JsonElement,
        markdown: String,
    ) {
        val dir = reportDir()
        val jsonPath = dir.resolve("$fileStem.json")
        val markdownPath = dir.resolve("$fileStem.md")
        Files.createDirectories(jsonPath.parent)
        Files.createDirectories(markdownPath.parent)
        Files.writeString(jsonPath, Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), payload))
        Files.writeString(markdownPath, markdown)
    }
}
