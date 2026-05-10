package com.ktome.client.render

import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.bossVariantModeLabelKey
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.input.ValidationOverlayPanelState
import com.ktome.client.telegraph.TelegraphPresentationModel
import com.ktome.client.telegraph.TelegraphRenderer
import com.ktome.client.telegraph.TelegraphStyle
import com.ktome.client.ui.card.ModalCardModel
import com.ktome.client.ui.combat.CombatAffordanceResourceKeys
import com.ktome.client.ui.combat.CombatDecisionFeedbackKeys
import com.ktome.client.ui.combat.CombatDecisionFrame
import com.ktome.client.ui.combat.CombatDecisionPanel
import com.ktome.client.ui.combat.CombatDecisionPanelModel
import com.ktome.client.ui.combat.CombatDecisionPanelRequest
import com.ktome.client.ui.combat.CombatDecisionValidationFixtures
import com.ktome.client.ui.combat.CombatDecisionValidationPanelRequest
import com.ktome.client.ui.combat.CombatDecisionValidationSurface
import com.ktome.client.ui.hud.ResourceGaugeModel
import com.ktome.client.ui.hud.ResourceHud
import com.ktome.client.ui.inspect.ExplainPaneModel
import com.ktome.client.ui.item.GroundLootMarkerModel
import com.ktome.client.ui.item.GroundLootMarkerPlacement
import com.ktome.client.ui.item.QualityColorTokenId
import com.ktome.client.ui.item.QualityPresentation
import com.ktome.client.ui.item.SpecialAccentTokenId
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.client.ui.panel.ActionPanelEntryModel
import com.ktome.client.ui.panel.ActionPanelModel
import com.ktome.client.ui.panel.LogPresentationModel
import com.ktome.client.ui.panel.LogPresentationModelBuilder
import com.ktome.client.ui.panel.PlayerCardModel
import com.ktome.client.ui.panel.TargetCardModel
import com.ktome.client.ui.state.UiEmptyState
import com.ktome.client.ui.status.StatusHudRenderer
import com.ktome.client.ui.status.StatusHudIconModel
import com.ktome.client.ui.status.StatusIconResolver
import com.ktome.client.ui.talent.DescriptionLine
import com.ktome.client.ui.talent.DescriptionLineKind
import com.ktome.client.ui.talent.DescriptionPresenter
import com.ktome.client.ui.talent.DescriptionSurface
import com.ktome.client.ui.talent.TalentSidebarLine
import com.ktome.client.ui.talent.TalentSidebarLineRole
import com.ktome.client.ui.talent.TalentSidebarPresenter
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.CombatFeedbackSnapshot
import com.ktome.core.snapshot.CombatFeedbackTypeSnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.ItemStatModifierSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RewardPresentationSourceSnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.game.PLAYER_ACTIVE_TALENT_SLOT_COUNT
import com.ktome.game.i18n.Localizer

internal enum class TileTextTone {
    GOLD,
    WHITE,
    LIGHT_GRAY,
    CYAN,
    GRAY,
    GREEN,
    RED,
    BLUE,
    MAGENTA,
}

internal data class TileVisualPlacement(
    val x: Int,
    val y: Int,
    val asset: ResolvedVisualAsset,
    val alpha: Float = 1f,
    val tintColorHex: String? = null,
)

internal data class TileGroundLootMarkerModel(
    val x: Int,
    val y: Int,
    val icon: ResolvedVisualAsset,
    val countBadge: String?,
    val cornerGlyph: String?,
    val rarityTone: TileTextTone,
    val specialAccentTokenId: SpecialAccentTokenId?,
    val placement: GroundLootMarkerPlacement,
)

internal data class TileFogPlacement(
    val x: Int,
    val y: Int,
    val alpha: Float,
)

internal data class TileTextRow(
    val text: String,
    val tone: TileTextTone,
    val icon: ResolvedVisualAsset? = null,
    val selected: Boolean = false,
)

internal enum class TileTargetCursorState {
    LEGAL,
    ILLEGAL,
}

internal data class TileMessageLine(
    val text: String,
    val tone: TileTextTone,
    val icon: ResolvedVisualAsset? = null,
)

internal data class TileCombatFeedbackModel(
    val x: Int,
    val y: Int,
    val text: String,
    val tone: TileTextTone,
    val stackIndex: Int,
    val horizontalOffsetCells: Int = 0,
)

internal data class TileGaugeModel(
    val label: String,
    val current: Int,
    val max: Int,
    val tone: TileTextTone,
    val resourceTypeId: String,
    val stableMin: Int? = null,
    val stableMax: Int? = null,
) {
    val percent: Float =
        if (max <= 0) {
            0f
        } else {
            (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }

    val summary: String = "$label $current/$max"
}

internal data class TileHotbarSlotModel(
    val slot: Int,
    val label: String,
    val icon: ResolvedVisualAsset?,
    val accentIcon: ResolvedVisualAsset?,
    val resourceText: String,
    val cooldownText: String?,
)

internal data class TileHudModel(
    val playerName: String,
    val zoneName: String,
    val floorText: String,
    val hpGauge: TileGaugeModel,
    val resourceGauge: TileGaugeModel,
    val secondaryResourceGauge: TileGaugeModel? = null,
    val experienceGauge: TileGaugeModel,
    val statusIcons: List<StatusHudIconModel>,
    val focusIcon: ResolvedVisualAsset?,
    val focusName: String?,
    val focusLines: List<String>,
    val hotbar: List<TileHotbarSlotModel>,
    val summaryText: String,
)

internal data class TileSidebarModel(
    val title: String,
    val rows: List<TileTextRow>,
)

internal data class TilePanelModel(
    val title: String,
    val rows: List<TileTextRow>,
)

internal data class TileShellModel(
    val leftRail: TilePanelModel,
    val rightPanel: TilePanelModel,
    val footerHints: List<TileTextRow>,
)

internal data class TileRenderModel(
    val terrainTiles: List<TileVisualPlacement>,
    val propTiles: List<TileVisualPlacement>,
    val overlayTiles: List<TileVisualPlacement>,
    val groundLootMarkers: List<TileGroundLootMarkerModel>,
    val actorTiles: List<TileVisualPlacement>,
    val fogTiles: List<TileFogPlacement>,
    val targetCursorState: TileTargetCursorState?,
    val hud: TileHudModel,
    val messageLines: List<TileMessageLine>,
    val logPresentation: LogPresentationModel,
    val playerCard: PlayerCardModel,
    val targetCard: TargetCardModel,
    val actionPanel: ActionPanelModel,
    val combatFeedback: List<TileCombatFeedbackModel>,
    val sidebar: TileSidebarModel,
    val shell: TileShellModel,
    val playerTile: com.ktome.core.map.Point,
    val mapDimensions: TileMapDimensions,
)

internal object TileRenderModelBuilder {
    fun build(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): TileRenderModel {
        val cellByPoint = snapshot.mapCells.associateBy { cell -> point(cell.x, cell.y) }
        val actorById = snapshot.actors.associateBy(ActorRenderSnapshot::entityId)
        val propByPoint = snapshot.props.associateBy { prop -> point(prop.x, prop.y) }
        val player = requireNotNull(snapshot.actors.singleOrNull { actor -> actor.isPlayer }) {
            "Expected a single player actor in render snapshot."
        }
        val playerCell = requireNotNull(cellByPoint[point(snapshot.metadata.playerX, snapshot.metadata.playerY)]) {
            "Expected player cell at (${snapshot.metadata.playerX}, ${snapshot.metadata.playerY}) in render snapshot."
        }
        val terrainTiles =
            snapshot.mapCells
                .filter { cell -> cell.visibility != CellVisibilitySnapshot.HIDDEN }
                .map { cell ->
                    TileVisualPlacement(
                        x = cell.x,
                        y = cell.y,
                        asset = resolveVisual(visualResolver, cell.terrainVisualKey),
                        alpha = if (cell.visibility == CellVisibilitySnapshot.EXPLORED) 0.72f else 1f,
                    )
                }
        val fogTiles =
            snapshot.mapCells.mapNotNull { cell ->
                when (cell.visibility) {
                    CellVisibilitySnapshot.VISIBLE -> null
                    CellVisibilitySnapshot.EXPLORED -> TileFogPlacement(x = cell.x, y = cell.y, alpha = 0.42f)
                    CellVisibilitySnapshot.HIDDEN -> TileFogPlacement(x = cell.x, y = cell.y, alpha = 0.94f)
                }
            }
        val propTiles =
            snapshot.props.map { prop ->
                TileVisualPlacement(
                    x = prop.x,
                    y = prop.y,
                    asset = resolveVisual(visualResolver, prop.visualKey),
                )
            }
        val overlayTiles =
            snapshot.overlays.flatMap { overlay ->
                val asset = resolveVisual(visualResolver, overlay.visualKey)
                overlay.cells.map { cell ->
                    TileVisualPlacement(
                        x = cell.x,
                        y = cell.y,
                        asset = asset,
                        alpha = TelegraphStyle.overlayAlpha(overlay.dangerLevel),
                    )
                }
            }
        val overlayCells = snapshot.overlays.flatMap { overlay -> overlay.cells }.map { cell -> cell.x to cell.y }.toSet()
        val groundLootMarkers =
            snapshot.mapCells.mapNotNull { cell ->
                if (cell.visibility != CellVisibilitySnapshot.VISIBLE || cell.items.isEmpty()) {
                    null
                } else {
                    GroundLootMarkerModel.fromCell(
                        cell = cell,
                        isActorOccupied = cell.actorEntityId != null,
                    )?.toTileMarker(visualResolver)
                }
            }
        val actorTiles =
            snapshot.actors
                .sortedBy { actor -> if (actor.isPlayer) 1 else 0 }
                .map { actor ->
                    TileVisualPlacement(
                        x = actor.x,
                        y = actor.y,
                        asset = resolveVisual(visualResolver, actor.visualKey),
                        tintColorHex = actorTintColorHex(visualResolver, actor),
                    )
                }

        val hud = buildHud(localizer, visualResolver, snapshot, overlayState, player, actorById, cellByPoint)
        val combatDecisionState = overlayState.modalFrames.lastOrNull()?.localState?.combatDecisionState
        val combatPanel =
            combatDecisionState?.let { state ->
                val focusIndex = overlayState.modalFrames.lastOrNull()?.localState?.focusIndex ?: 0
                overlayState.validationCombatDecisionSurface?.let { surface ->
                    CombatDecisionValidationFixtures.panel(
                        CombatDecisionValidationPanelRequest(
                            localizer = localizer,
                            snapshot = snapshot,
                            surface = surface,
                            state = state,
                            focusIndex = focusIndex,
                        ),
                    )
                } ?: CombatDecisionPanel.build(
                    CombatDecisionPanelRequest(
                        localizer = localizer,
                        snapshot = snapshot,
                        state = state,
                        focusIndex = focusIndex,
                        targetCursor = overlayState.targetingCursor ?: overlayState.modalFrames.lastOrNull()?.localState?.targetingCursor,
                        renderText = { token -> renderTextToken(localizer, token) },
                    ),
                )
            }
        val baseMessageLines =
            if (snapshot.logEvents.isEmpty()) {
                emptyLogMessageLines(localizer)
            } else {
                val warningTelegraphs = snapshot.overlays.map(TelegraphPresentationModel::fromOverlay)
                var warningIndex = 0
                snapshot.logEvents.map { event ->
                    val baseText = renderLogEvent(localizer, event)
                    val warningTelegraph =
                        if (event.message.key == "log.warning.telegraph") {
                            warningTelegraphs.getOrNull(warningIndex++)
                        } else {
                            null
                        }
                    TileMessageLine(
                        text =
                            if (warningTelegraph != null) {
                                "${TelegraphRenderer.logPrefix(localizer, warningTelegraph)} $baseText"
                            } else {
                                baseText
                            },
                        tone = warningTelegraph?.let { TelegraphRenderer.tileTone(it.dangerLevel) } ?: messageTone(event.message.key),
                        icon = warningTelegraph?.iconKey?.let { iconKey -> resolveVisual(visualResolver, iconKey) },
                    )
                }
            }
        val uiMessageLine =
            overlayState.uiMessageKey?.let { key ->
                TileMessageLine(text = localizer.text(key), tone = TileTextTone.CYAN)
            }
        val messageLines = baseMessageLines + listOfNotNull(uiMessageLine)

        val sidebar = buildSidebar(localizer, visualResolver, snapshot, overlayState, player, actorById, cellByPoint, propByPoint, playerCell, combatPanel)
        return TileRenderModel(
            terrainTiles = terrainTiles,
            propTiles = propTiles,
            overlayTiles = overlayTiles,
            groundLootMarkers = groundLootMarkers,
            actorTiles = actorTiles,
            fogTiles = fogTiles,
            targetCursorState = combatDecisionTargetCursorState(snapshot, overlayState),
            hud = hud,
            messageLines = messageLines,
            logPresentation =
                LogPresentationModelBuilder.build(localizer, snapshot.logEvents) { token ->
                    renderTextToken(localizer, token)
                },
            playerCard =
                PlayerCardModel(
                    name = hud.playerName,
                    hpSummary = hud.hpGauge.summary,
                    primaryResourceSummary = hud.resourceGauge.summary,
                    secondaryResourceSummary = hud.secondaryResourceGauge?.summary,
                    statusCount = hud.statusIcons.size,
                    emptyStateText = localizer.text("ui.player.empty"),
                ),
            targetCard =
                buildTargetCardModel(
                    localizer = localizer,
                    snapshot = snapshot,
                    overlayState = overlayState,
                    hud = hud,
                    actorById = actorById,
                    cellByPoint = cellByPoint,
                    propByPoint = propByPoint,
                ),
            actionPanel =
                combatPanel?.toActionPanel(visualResolver)
                    ?: ActionPanelModel(
                        entries =
                            hud.hotbar.map { slot ->
                                ActionPanelEntryModel(
                                    hotkey = slot.slot.toString(),
                                    label = slot.label,
                                    enabled = slot.cooldownText == null,
                                    icon = slot.icon,
                                )
                            },
                        emptyStateText = localizer.text("ui.action.empty"),
                    ),
            combatFeedback = buildCombatFeedback(localizer, snapshot.metadata.width, overlayCells, snapshot.combatFeedbackEvents),
            sidebar = sidebar,
            shell = buildShell(localizer, snapshot, hud, sidebar, messageLines, playerCell),
            playerTile = point(player.x, player.y),
            mapDimensions = TileMapDimensions(snapshot.metadata.width, snapshot.metadata.height),
        )
    }

    private fun buildHud(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        player: ActorRenderSnapshot,
        actorById: Map<Int, ActorRenderSnapshot>,
        cellByPoint: Map<com.ktome.core.map.Point, MapCellSnapshot>,
    ): TileHudModel {
        val playerStatus = snapshot.uiState.playerStatus
        val resourceHud = ResourceHud.build(localizer, snapshot)
        val focusActor = focusedActor(snapshot, overlayState, actorById, cellByPoint)
        val frontstageFocus = frontstageFocus(localizer, snapshot, overlayState, focusActor)
        val statusIcons = StatusIconResolver.resolveIcons(visualResolver, player.statusEffects)
        val hotbar =
            snapshot.uiState.talents.map { talent ->
                TileHotbarSlotModel(
                    slot = talent.slot,
                    label = localizer.text(talent.nameKey),
                    icon = talent.iconKey?.let { resolveVisual(visualResolver, it) },
                    accentIcon = talent.damageTypeIconKey?.let { resolveVisual(visualResolver, it) },
                    resourceText = talentUsageSummary(localizer, talent),
                    cooldownText =
                        if (talent.currentCooldown > 0) {
                            localizer.text("ui.sidebar.cooldown.short") + ":" + talent.currentCooldown
                        } else {
                            null
                        },
                )
            }
        val focusName =
            focusActor?.let { actor -> localizer.text(actor.nameKey) }
                ?: frontstageFocus?.first
        val focusLines =
            focusActor?.let { actor ->
                listOf(
                    actorRole(localizer, actor),
                    "${localizer.text("ui.hud.hp.short")} ${actor.currentHp}/${actor.maxHp}",
                    "${localizer.text("ui.hud.attack.short")} ${actor.attack}  ${localizer.text("ui.hud.defense.short")} ${actor.defense}",
                ) +
                    listOfNotNull(
                        actor.bossVariant?.let { variant ->
                            localizer.text("ui.inspect.boss_variant", "variant" to localizer.text(variant.nameKey))
                        },
                    ) +
                    actor.mutations.map { mutation ->
                        mutation.summary?.let { summary ->
                            "${localizer.text(mutation.nameKey)} · ${renderTextToken(localizer, summary)}"
                        } ?: localizer.text("ui.inspect.mutation.line", "mutation" to localizer.text(mutation.nameKey))
                    } +
                    actor.statusEffects.flatMap { effect ->
                        DescriptionPresenter.presentStatusEffectLines(localizer, effect).map(DescriptionLine::text)
                    }
            } ?: frontstageFocus?.second.orEmpty()

        return TileHudModel(
            playerName = localizer.text(player.nameKey),
            zoneName = localizer.text(snapshot.metadata.zoneNameKey),
            floorText = localizer.text("ui.hud.floor.short") + " ${snapshot.metadata.currentFloor}/${snapshot.metadata.maxFloor}",
            hpGauge =
                TileGaugeModel(
                    label = localizer.text("ui.hud.hp.short"),
                    current = playerStatus.currentHp,
                    max = playerStatus.maxHp,
                    tone = TileTextTone.RED,
                    resourceTypeId = "HEALTH",
                ),
            resourceGauge = resourceHud.primaryGauge.toTileGauge(),
            secondaryResourceGauge = resourceHud.secondaryGauge?.toTileGauge(),
            experienceGauge =
                TileGaugeModel(
                    label = localizer.text("ui.hud.xp.short"),
                    current = playerStatus.currentExperience,
                    max = playerStatus.nextLevelRequirement,
                    tone = TileTextTone.GOLD,
                    resourceTypeId = "EXPERIENCE",
                ),
            statusIcons = statusIcons,
            focusIcon = focusActor?.let { actor -> resolveVisual(visualResolver, actor.visualKey) },
            focusName = focusName,
            focusLines = focusLines,
            hotbar = hotbar,
            summaryText = resourceHud.summaryText,
        )
    }

    private fun buildShell(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        hud: TileHudModel,
        sidebar: TileSidebarModel,
        messageLines: List<TileMessageLine>,
        playerCell: MapCellSnapshot,
    ): TileShellModel {
        val status = snapshot.uiState.playerStatus
        val zoneDescription =
            snapshot.metadata.zoneDescKey
                ?.let(localizer::text)
                ?: localizer.text("ui.shell.quest.none")
        val questSummary = questSummaryText(localizer, snapshot)
        val criticalHint =
            messageLines
                .lastOrNull { line -> line.tone == TileTextTone.RED || line.tone == TileTextTone.GOLD || line.tone == TileTextTone.CYAN }
                ?.text
                ?: localizer.text("ui.shell.critical_hint.none")
        val awarenessRows = frontstageSidebarRows(localizer, snapshot).take(6)
        val recentRewardRows =
            snapshot.uiState.recentRewards.asReversed().flatMap { entry ->
                listOf(
                    TileTextRow(
                        recentRewardText(
                            sourceLabel = renderTextToken(localizer, ModalCardModel.rewardPresentationSummary(entry)),
                            itemDisplayName = renderTextToken(localizer, entry.itemDisplayName),
                        ),
                        rewardPresentationTone(entry.source),
                    ),
                ) +
                    ModalCardModel.rewardPresentationDetailLines(entry).map { detailText ->
                        TileTextRow(recentRewardDetailText(renderTextToken(localizer, detailText)), TileTextTone.LIGHT_GRAY)
                    }
            }.take(4)
        val inventoryCount = snapshot.uiState.inventory.size
        val groundRows =
            playerCell.items
                .take(3)
                .map { item ->
                    TileTextRow(renderItemDisplay(localizer, item), itemTone(item))
                }
                .ifEmpty { listOf(TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)) }
        val equipmentRows =
            snapshot.uiState.equipment.map { equipment ->
                val itemName = equipment.item?.let { item -> renderItemDisplay(localizer, item) } ?: "-"
                TileTextRow("${equipmentSlotLabel(localizer, equipment.slotId)}: $itemName", TileTextTone.WHITE)
            }.ifEmpty { listOf(TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)) }
        val inscriptionRows =
            snapshot.uiState.inscriptions
                .map { inscription ->
                    val cooldownSuffix =
                        if (inscription.cooldownRemaining > 0) {
                            " (${inscription.cooldownRemaining})"
                        } else {
                            ""
                        }
                    TileTextRow("${inscription.hotkey}. ${localizer.text(inscription.nameKey)}$cooldownSuffix", TileTextTone.WHITE)
                }
                .ifEmpty { listOf(TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)) }
        val pointRows =
            listOfNotNull(
                status.statPoints.takeIf { points -> points > 0 }?.let { points ->
                    TileTextRow("${localizer.text("ui.hud.stat.short")} $points", TileTextTone.GOLD)
                },
                status.talentPoints.takeIf { points -> points > 0 }?.let { points ->
                    TileTextRow("${localizer.text("ui.hud.talent.short")} $points", TileTextTone.GOLD)
                },
                status.raceTalentPoints.takeIf { points -> points > 0 }?.let { points ->
                    TileTextRow("${localizer.text("ui.hud.race_talent.short")} $points", TileTextTone.GOLD)
                },
            )
        val modeRows =
            if (snapshot.uiState.activeShop != null || sidebar.title != TileRenderer.sidebarTitle(localizer, UiMode.MAP)) {
                listOf(TileTextRow(sidebar.title, TileTextTone.GOLD)) + sidebar.rows.take(5)
            } else {
                emptyList()
            }

        return TileShellModel(
            leftRail =
                TilePanelModel(
                    title = hud.zoneName,
                    rows =
                        listOf(
                            TileTextRow(hud.floorText, TileTextTone.GOLD),
                            TileTextRow(zoneDescription, TileTextTone.LIGHT_GRAY),
                            TileTextRow(localizer.text("ui.shell.quest.summary"), TileTextTone.GOLD),
                            TileTextRow(questSummary, TileTextTone.LIGHT_GRAY),
                            TileTextRow(localizer.text("ui.shell.critical_hint"), TileTextTone.GOLD),
                            TileTextRow(criticalHint, TileTextTone.CYAN),
                        ) +
                            if (awarenessRows.isEmpty()) {
                                emptyList()
                            } else {
                                listOf(TileTextRow(localizer.text("ui.hud.frontstage.title"), TileTextTone.GOLD)) + awarenessRows
                            } +
                            if (recentRewardRows.isEmpty()) {
                                emptyList()
                            } else {
                                listOf(TileTextRow(localizer.text("ui.sidebar.recent_rewards"), TileTextTone.GOLD)) + recentRewardRows
                            },
                ),
            rightPanel =
                TilePanelModel(
                    title = hud.playerName,
                    rows =
                        listOf(
                            TileTextRow(
                                "${localizer.text("ui.hud.level.short")} ${status.level}",
                                TileTextTone.GOLD,
                            ),
                            TileTextRow(localizer.text("ui.sidebar.shards", "value" to snapshot.uiState.shardBalance), TileTextTone.GOLD),
                        ) +
                            pointRows +
                            listOf(TileTextRow(localizer.text("ui.sidebar.ground"), TileTextTone.GOLD)) +
                            groundRows +
                            listOf(TileTextRow(localizer.text("ui.sidebar.equipment"), TileTextTone.GOLD)) +
                            equipmentRows +
                            listOf(TileTextRow(localizer.text("ui.sidebar.inscriptions"), TileTextTone.GOLD)) +
                            inscriptionRows +
                            listOf(
                                TileTextRow(localizer.text("ui.shell.backpack.summary", "count" to inventoryCount), TileTextTone.LIGHT_GRAY),
                                TileTextRow(localizer.text("ui.shell.resources.bottom_owned"), TileTextTone.GRAY),
                            ) +
                            modeRows,
                ),
            footerHints =
                listOf(
                    TileTextRow(localizer.text("ui.controls.map.inventory"), TileTextTone.LIGHT_GRAY),
                    TileTextRow(localizer.text("ui.controls.map.pick_up"), TileTextTone.LIGHT_GRAY),
                    TileTextRow(localizer.text("ui.controls.map.save"), TileTextTone.LIGHT_GRAY),
                ),
        )
    }

    private fun questSummaryText(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): String =
        snapshot.logEvents
            .asReversed()
            .map(RenderLogEventSnapshot::message)
            .firstOrNull { token -> token.key.startsWith("log.objective.") }
            ?.let { token -> renderTextToken(localizer, token) }
            ?: localizer.text("ui.shell.quest.none")

    private fun buildTargetCardModel(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        hud: TileHudModel,
        actorById: Map<Int, ActorRenderSnapshot>,
        cellByPoint: Map<com.ktome.core.map.Point, MapCellSnapshot>,
        propByPoint: Map<com.ktome.core.map.Point, PropRenderSnapshot>,
    ): TargetCardModel {
        val inspectEmptyState = UiEmptyState.inspect()
        val emptyText = renderTextToken(localizer, inspectEmptyState.title)
        val focusPoint =
            when (overlayState.mode) {
                UiMode.INSPECT -> overlayState.inspectCursor
                UiMode.TARGETING -> overlayState.targetingCursor
                UiMode.VALIDATION -> overlayState.validationPanel?.inspectCursor
                else -> null
            } ?: return TargetCardModel(title = hud.focusName, lines = hud.focusLines, emptyStateText = emptyText)
        val cell = cellByPoint[focusPoint] ?: return TargetCardModel(title = null, lines = emptyList(), emptyStateText = emptyText)
        val actor = cell.actorEntityId?.let(actorById::get)
        val overlaysAtFocus =
            snapshot.overlays.filter { overlay ->
                overlay.cells.any { cell -> cell.x == focusPoint.x && cell.y == focusPoint.y }
            }
        val telegraphRows = overlaysAtFocus.flatMap { overlay -> TelegraphRenderer.targetCardRows(localizer, overlay) }
        if (actor != null) {
            return TargetCardModel(
                title = hud.focusName ?: localizer.text(actor.nameKey),
                lines = hud.focusLines + telegraphRows,
                emptyStateText = emptyText,
            )
        }
        val prop = propByPoint[focusPoint]
        val propNameKey = prop?.nameKey
        if (propNameKey != null) {
            return TargetCardModel(
                title = localizer.text(propNameKey),
                lines =
                    listOfNotNull(prop.stateLabelKey?.let(localizer::text)) +
                        DescriptionPresenter.presentInspectObjectLines(localizer, listOfNotNull(prop.descKey?.let(::RenderTextTokenSnapshot)))
                            .map(DescriptionLine::text) +
                        telegraphRows,
                emptyStateText = emptyText,
            )
        }
        if (cell.items.isNotEmpty()) {
            return TargetCardModel(
                title = localizer.text("ui.sidebar.items"),
                lines = cell.items.map { item -> renderItemDisplay(localizer, item) } + telegraphRows,
                emptyStateText = emptyText,
            )
        }
        return TargetCardModel(
            title = terrainName(localizer, cell),
                lines =
                    listOf(
                        localizer.text("ui.inspect.cursor", "x" to focusPoint.x, "y" to focusPoint.y),
                        renderTextToken(localizer, inspectEmptyState.detail),
                    ) + telegraphRows,
            emptyStateText = emptyText,
        )
    }

    private fun buildSidebar(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        player: ActorRenderSnapshot,
        actorById: Map<Int, ActorRenderSnapshot>,
        cellByPoint: Map<com.ktome.core.map.Point, MapCellSnapshot>,
        propByPoint: Map<com.ktome.core.map.Point, PropRenderSnapshot>,
        playerCell: MapCellSnapshot,
        combatPanel: CombatDecisionPanelModel?,
    ): TileSidebarModel {
        val rows = mutableListOf<TileTextRow>()
        val title = TileRenderer.sidebarTitle(localizer, overlayState.mode)

        when (overlayState.mode) {
            UiMode.MAP -> {
                snapshot.metadata.zoneDescKey?.let { descKey ->
                    rows += TileTextRow(localizer.text(descKey), TileTextTone.LIGHT_GRAY)
                }
                rows += TileTextRow(localizer.text("ui.sidebar.shards", "value" to snapshot.uiState.shardBalance), TileTextTone.GOLD)
                if (snapshot.overlays.isNotEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.warnings"), TileTextTone.GOLD)
                    rows += TelegraphRenderer.tileRows(localizer, snapshot)
                }
                val frontstageRows = frontstageSidebarRows(localizer, snapshot)
                if (frontstageRows.isNotEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.frontstage"), TileTextTone.GOLD)
                    rows += frontstageRows
                }
                rows += TileTextRow(localizer.text("ui.sidebar.equipment"), TileTextTone.GOLD)
                snapshot.uiState.equipment.forEach { equipment ->
                    val item = equipment.item
                    val presentation = item?.let(QualityPresentation::from)
                    val itemName = item?.let { renderItemDisplay(localizer, it, requireNotNull(presentation)) } ?: "-"
                    rows +=
                        TileTextRow(
                            text = "${equipmentSlotLabel(localizer, equipment.slotId)}: $itemName",
                            tone = presentation?.let(::itemTone) ?: TileTextTone.WHITE,
                            icon = item?.iconKey?.let { resolveVisual(visualResolver, it) },
                        )
                }
                if (snapshot.uiState.inscriptions.isNotEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.inscriptions"), TileTextTone.GOLD)
                    snapshot.uiState.inscriptions.forEach { inscription ->
                        val cooldownSuffix =
                            if (inscription.cooldownRemaining > 0) {
                                " (${inscription.cooldownRemaining})"
                            } else {
                                ""
                            }
                        rows +=
                            TileTextRow(
                                text = "${inscription.hotkey}. ${localizer.text(inscription.nameKey)}$cooldownSuffix",
                                tone = if (inscription.cooldownRemaining > 0) TileTextTone.GRAY else TileTextTone.WHITE,
                                icon = inscription.iconKey.let { iconKey -> resolveVisual(visualResolver, iconKey) },
                            )
                    }
                }
                rows += TileTextRow(localizer.text("ui.sidebar.items"), TileTextTone.GOLD)
                if (playerCell.items.isEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)
                } else {
                    playerCell.items.forEach { item ->
                        val presentation = QualityPresentation.from(item)
                        rows +=
                            TileTextRow(
                                text = renderItemDisplay(localizer, item, presentation),
                                tone = itemTone(presentation),
                                icon = item.iconKey?.let { resolveVisual(visualResolver, it) },
                        )
                    }
                }
                snapshot.uiState.searchPromptLabelKey?.let { labelKey ->
                    rows += TileTextRow(localizer.text(labelKey), TileTextTone.CYAN)
                }
                rows += TileTextRow(localizer.text("ui.controls.map.inventory"), TileTextTone.LIGHT_GRAY)
                rows += TileTextRow(localizer.text("ui.controls.map.pick_up"), TileTextTone.LIGHT_GRAY)
                rows += TileTextRow(localizer.text("ui.controls.map.save"), TileTextTone.LIGHT_GRAY)
                if (snapshot.uiState.talents.isNotEmpty()) {
                    rows += TileTextRow(localizer.text("ui.controls.map.use_talent"), TileTextTone.LIGHT_GRAY)
                    rows += TileTextRow(localizer.text("ui.controls.map.edit_loadout"), TileTextTone.LIGHT_GRAY)
                }
                if (snapshot.uiState.inscriptions.isNotEmpty()) {
                    rows += TileTextRow(localizer.text("ui.controls.map.use_inscription"), TileTextTone.LIGHT_GRAY)
                }
                if (snapshot.uiState.talents.any(TalentSlotSnapshot::requiresTarget)) {
                    rows += TileTextRow(localizer.text("ui.controls.map.target_talent"), TileTextTone.LIGHT_GRAY)
                }
                if (playerCell.stairDirectionId == "DOWN") {
                    rows += TileTextRow(localizer.text("ui.controls.map.descend"), TileTextTone.LIGHT_GRAY)
                }
                if (playerCell.stairDirectionId == "UP") {
                    rows += TileTextRow(localizer.text("ui.controls.map.ascend"), TileTextTone.LIGHT_GRAY)
                }
                if (snapshot.uiState.playerStatus.talentPoints > 0 || snapshot.uiState.playerStatus.raceTalentPoints > 0) {
                    rows += TileTextRow(localizer.text("ui.controls.map.spend_talent"), TileTextTone.LIGHT_GRAY)
                }
                if (snapshot.uiState.playerStatus.statPoints > 0) {
                    rows += TileTextRow(localizer.text("ui.controls.map.spend_stat"), TileTextTone.LIGHT_GRAY)
                }
            }

            UiMode.SHOP -> {
                val shop = snapshot.uiState.activeShop
                rows += TileTextRow(localizer.text("ui.sidebar.shards", "value" to snapshot.uiState.shardBalance), TileTextTone.GOLD)
                if (shop == null) {
                    rows += emptyStateRows(localizer, visualResolver, UiEmptyState.shop())
                } else {
                    rows += TileTextRow(localizer.text(shop.shopNameKey), TileTextTone.GOLD)
                    shop.hintLabelKeys.forEach { hintLabelKey ->
                        rows += TileTextRow(localizer.text(hintLabelKey), TileTextTone.LIGHT_GRAY)
                    }
                    shop.inscriptionReplacementPrompt?.let { prompt ->
                        rows +=
                            inscriptionReplacementPromptLines(
                                localizer = localizer,
                                prompt = prompt,
                                selectedHotkey = overlayState.inscriptionReplacementHotkeySelection,
                            ).map(::inscriptionReplacementPromptTileRow)
                    }
                }
                if (shop?.inscriptionReplacementPrompt == null) {
                    rows += TileTextRow(localizer.text("ui.controls.shop.close_hint"), TileTextTone.LIGHT_GRAY)
                }
                if (shop != null) {
                    rows += TileTextRow(localizer.text("ui.shop.buy"), if (overlayState.shopFocus == com.ktome.client.input.ShopFocus.BUY) TileTextTone.GOLD else TileTextTone.WHITE)
                    if (shop.offers.isEmpty()) {
                        rows += emptyStateRows(localizer, visualResolver, UiEmptyState.shop())
                    } else {
                        shop.offers.forEach { offer ->
                            val card = ModalCardModel.shopOffer(shop.shopId, offer)
                            rows +=
                                modalCardHeaderRow(
                                    localizer = localizer,
                                    visualResolver = visualResolver,
                                    card = card,
                                    prefix = "${offer.index + 1}.",
                                    tone = if (overlayState.shopFocus == com.ktome.client.input.ShopFocus.BUY && overlayState.shopOfferSelection == offer.index) TileTextTone.CYAN else TileTextTone.WHITE,
                                    selected = overlayState.shopFocus == com.ktome.client.input.ShopFocus.BUY && overlayState.shopOfferSelection == offer.index,
                                )
                        }
                    }
                    rows += TileTextRow(localizer.text("ui.shop.sell"), if (overlayState.shopFocus == com.ktome.client.input.ShopFocus.SELL) TileTextTone.GOLD else TileTextTone.WHITE)
                    if (shop.sellEntries.isEmpty()) {
                        rows += emptyStateRows(localizer, visualResolver, UiEmptyState.shop())
                    } else {
                        shop.sellEntries.forEachIndexed { displayIndex, sellEntry ->
                            val inventoryEntry = snapshot.uiState.inventory.firstOrNull { entry -> entry.index == sellEntry.inventoryIndex }
                            val card =
                                ModalCardModel.shopSellEntry(
                                    shopId = shop.shopId,
                                    displayIndex = displayIndex,
                                    item = inventoryEntry?.item,
                                    price = sellEntry.price,
                                )
                            rows +=
                                modalCardHeaderRow(
                                    localizer = localizer,
                                    visualResolver = visualResolver,
                                    card = card,
                                    prefix = "${displayIndex + 1}.",
                                    tone = if (overlayState.shopFocus == com.ktome.client.input.ShopFocus.SELL && overlayState.inventorySelection == displayIndex) TileTextTone.CYAN else TileTextTone.WHITE,
                                    selected = overlayState.shopFocus == com.ktome.client.input.ShopFocus.SELL && overlayState.inventorySelection == displayIndex,
                                )
                        }
                    }
                    selectedShopDescriptionRows(localizer, shop, snapshot, overlayState).forEach { line ->
                        rows += TileTextRow(line.text, descriptionTone(line))
                    }
                }
                rows += TileTextRow(localizer.text("ui.controls.shop"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.WORLD_MAP -> {
                val routePanel = snapshot.uiState.activeRouteSelection
                if (routePanel == null) {
                    rows += TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)
                } else {
                    rows += TileTextRow(localizer.text("ui.world_map.current_zone", "zone" to localizer.text(routePanel.currentZoneNameKey)), TileTextTone.GOLD)
                    routePanel.options.forEach { option ->
                        val card = RoutePreviewText.modalCardModel(option)
                        rows +=
                            modalCardHeaderRow(
                                localizer = localizer,
                                visualResolver = visualResolver,
                                card = card,
                                prefix = "${option.index + 1}.",
                                tone = if (overlayState.routeSelection == option.index) TileTextTone.CYAN else TileTextTone.WHITE,
                                selected = overlayState.routeSelection == option.index,
                            )
                        card.detailLines.forEach { detailLine ->
                            rows += TileTextRow(renderTextToken(localizer, detailLine), TileTextTone.LIGHT_GRAY)
                        }
                        card.summary?.let { summary ->
                            rows += TileTextRow(renderTextToken(localizer, summary), TileTextTone.LIGHT_GRAY)
                        }
                        card.rewardLines.forEach { rewardLine ->
                            rows += TileTextRow(renderTextToken(localizer, rewardLine), modalCardRewardTone(rewardLine))
                        }
                        RoutePreviewText.traitLine(localizer, option)?.let { traits ->
                            rows += TileTextRow(traits, TileTextTone.BLUE)
                        }
                    }
                }
                rows += TileTextRow(localizer.text("ui.controls.world_map"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.INVENTORY -> {
                if (overlayState.activeModalKind == ModalFrameKind.ITEM_COMPARE) {
                    rows += TileTextRow(localizer.text("ui.modal.item_compare.stub"), TileTextTone.LIGHT_GRAY)
                    rows += TileTextRow(localizer.text("ui.controls.deferred_modal"), TileTextTone.LIGHT_GRAY)
                    return TileSidebarModel(title = localizer.text("ui.sidebar.item_compare"), rows = rows)
                }
                if (overlayState.activeModalKind == ModalFrameKind.ITEM_DETAIL) {
                    val selectedItem = snapshot.uiState.inventory.getOrNull(overlayState.inventorySelection)?.item
                    if (selectedItem == null) {
                        rows += TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)
                    } else {
                        val presentation = QualityPresentation.from(selectedItem)
                        rows +=
                            TileTextRow(
                                text = renderItemDisplay(localizer, selectedItem, presentation),
                                tone = itemTone(presentation),
                                icon = selectedItem.iconKey?.let { resolveVisual(visualResolver, it) },
                            )
                        DescriptionPresenter.presentInventoryItemLines(localizer, selectedItem).forEach { line ->
                            rows += TileTextRow(line.text, descriptionTone(line))
                        }
                        itemDetailLines(localizer, selectedItem).forEach { detail ->
                            rows += TileTextRow(detail, TileTextTone.LIGHT_GRAY)
                        }
                    }
                    rows += TileTextRow(localizer.text("ui.controls.item_detail"), TileTextTone.LIGHT_GRAY)
                    return TileSidebarModel(title = localizer.text("ui.sidebar.item_detail"), rows = rows)
                }
                rows += TileTextRow(localizer.text("ui.controls.inventory.close_hint"), TileTextTone.LIGHT_GRAY)
                snapshot.uiState.inventory.forEach { entry ->
                    val presentation = QualityPresentation.from(entry.item)
                    val label = "${entry.index + 1}. ${renderItemDisplay(localizer, entry.item, presentation)}"
                    val equipped = entry.equippedSlotId?.let { slotId -> " [${equipmentSlotLabel(localizer, slotId)}]" }.orEmpty()
                    rows +=
                        TileTextRow(
                            text = label + equipped,
                            tone = if (entry.index == overlayState.inventorySelection) TileTextTone.CYAN else itemTone(presentation),
                            icon = entry.item.iconKey?.let { resolveVisual(visualResolver, it) },
                            selected = entry.index == overlayState.inventorySelection,
                        )
                }
                if (snapshot.uiState.inventory.isEmpty()) {
                    rows += emptyStateRows(localizer, visualResolver, UiEmptyState.inventory())
                } else {
                    val selectedItem = snapshot.uiState.inventory.getOrNull(overlayState.inventorySelection)?.item
                    selectedItem?.let { item ->
                        DescriptionPresenter.presentInventoryItemLines(localizer, item).forEach { line ->
                            rows += TileTextRow(line.text, descriptionTone(line))
                        }
                        itemDetailLines(localizer, item).forEach { detail ->
                            rows += TileTextRow(detail, TileTextTone.LIGHT_GRAY)
                        }
                    }
                }
                rows += TileTextRow(localizer.text("ui.controls.inventory"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.LOADOUT_EDIT -> {
                val activeTalents = snapshot.uiState.talents.associateBy(TalentSlotSnapshot::slot)
                rows += TileTextRow(localizer.text("ui.sidebar.active_loadout"), TileTextTone.GOLD)
                (1..PLAYER_ACTIVE_TALENT_SLOT_COUNT).forEach { slot ->
                    val talent = activeTalents[slot]
                    rows +=
                        TileTextRow(
                            text = loadoutSlotLabel(localizer, slot, talent),
                            tone = if (overlayState.loadoutSlotSelection == slot) TileTextTone.CYAN else TileTextTone.WHITE,
                            icon = talent?.iconKey?.let { resolveVisual(visualResolver, it) },
                            selected = overlayState.loadoutSlotSelection == slot,
                        )
                }
                rows += TileTextRow(localizer.text("ui.sidebar.reserve_talents"), TileTextTone.GOLD)
                if (snapshot.uiState.reserveTalents.isEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)
                } else {
                    snapshot.uiState.reserveTalents.forEachIndexed { index, talent ->
                        rows +=
                            TileTextRow(
                                text = reserveTalentLabel(localizer, talent),
                                tone = if (overlayState.loadoutReserveSelection == index) TileTextTone.CYAN else TileTextTone.WHITE,
                                icon = talent.iconKey?.let { resolveVisual(visualResolver, it) },
                                selected = overlayState.loadoutReserveSelection == index,
                            )
                    }
                    snapshot.uiState.reserveTalents.getOrNull(overlayState.loadoutReserveSelection)?.let { talent ->
                        DescriptionPresenter.presentReserveTalentLines(localizer, talent).forEach { line ->
                            rows += TileTextRow(line.text, descriptionTone(line))
                        }
                        rows += TileTextRow(talentUsageSummary(localizer, talent), TileTextTone.LIGHT_GRAY)
                    }
                }
                rows += TileTextRow(localizer.text("ui.controls.loadout"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.TARGETING -> {
                if (overlayState.activeModalKind == ModalFrameKind.COMBAT_DECISION) {
                    if (combatPanel != null) {
                        rows +=
                            TileTextRow(
                                text = combatPanel.title,
                                tone = TileTextTone.GOLD,
                                icon = resolveVisual(visualResolver, combatPanel.phaseIconKey),
                            )
                        combatPanel.rows.forEach { row ->
                            rows +=
                                TileTextRow(
                                    text = row.text,
                                    tone =
                                        when {
                                            !row.enabled -> TileTextTone.GRAY
                                            row.danger -> TileTextTone.RED
                                            row.selected -> TileTextTone.CYAN
                                            else -> TileTextTone.WHITE
                                        },
                                    icon = row.iconKey?.let { resolveVisual(visualResolver, it) },
                                    selected = row.selected,
                                )
                        }
                        rows += TileTextRow(localizer.text("ui.controls.combat_decision"), TileTextTone.LIGHT_GRAY)
                    } else {
                        rows += TileTextRow(localizer.text(CombatDecisionFeedbackKeys.NO_AVAILABLE_ACTION), TileTextTone.LIGHT_GRAY)
                    }
                    return TileSidebarModel(title = localizer.text("ui.sidebar.combat_decision"), rows = rows)
                }
                val cursor = overlayState.targetingCursor
                rows += TileTextRow(localizer.text("ui.targeting.slot", "slot" to overlayState.targetingSlot), TileTextTone.WHITE)
                rows +=
                    TileTextRow(
                        localizer.text("ui.targeting.cursor", "x" to (cursor?.x ?: "-"), "y" to (cursor?.y ?: "-")),
                        TileTextTone.WHITE,
                    )
                rows += TileTextRow(localizer.text("ui.controls.targeting"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.INSPECT -> {
                val cursor = overlayState.inspectCursor ?: point(snapshot.metadata.playerX, snapshot.metadata.playerY)
                val cell = requireNotNull(cellByPoint[cursor])
                val actor = cell.actorEntityId?.let(actorById::get)
                val prop = propByPoint[cursor]

                rows += TileTextRow(localizer.text("ui.inspect.cursor", "x" to cursor.x, "y" to cursor.y), TileTextTone.WHITE)
                rows += TileTextRow("${visibilityLabel(localizer, cell.visibility)} ${terrainName(localizer, cell)}", TileTextTone.WHITE)
                cell.terrainOverride?.let { terrainOverride ->
                    val tickDamageTypeId = terrainOverride.tickDamageTypeId
                    rows += TileTextRow(localizer.text("ui.inspect.terrain.rule", "rule" to localizer.text(terrainOverride.ruleNameKey)), TileTextTone.LIGHT_GRAY)
                    rows += TileTextRow(localizer.text("ui.inspect.terrain.turns", "turns" to terrainOverride.remainingTurns), TileTextTone.LIGHT_GRAY)
                    if (terrainOverride.conductsLightning) {
                        rows += TileTextRow(localizer.text("ui.inspect.terrain.conducts_lightning"), TileTextTone.CYAN)
                    }
                    if (terrainOverride.tickDamage > 0 && tickDamageTypeId != null) {
                        rows +=
                            TileTextRow(
                                localizer.text(
                                    "ui.inspect.terrain.tick_damage",
                                    "amount" to terrainOverride.tickDamage,
                                    "damageType" to localizer.text(damageTypeLabelKey(tickDamageTypeId)),
                                ),
                                TileTextTone.RED,
                        )
                    }
                }
                val overlaysAtCursor =
                    snapshot.overlays.filter { overlay ->
                        overlay.cells.any { cell -> cell.x == cursor.x && cell.y == cursor.y }
                    }
                if (overlaysAtCursor.isNotEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.warnings"), TileTextTone.GOLD)
                    rows += TelegraphRenderer.tileRows(localizer, overlaysAtCursor)
                }
                actor?.let { inspected ->
                    rows +=
                        TileTextRow(
                            text = localizer.text(inspected.nameKey),
                            tone = TileTextTone.GOLD,
                            icon = resolveVisual(visualResolver, inspected.visualKey),
                        )
                    rows += TileTextRow(actorRole(localizer, inspected), TileTextTone.WHITE)
                    inspected.bossVariant?.let { variant ->
                        rows +=
                            TileTextRow(
                                text = localizer.text("ui.inspect.boss_variant", "variant" to localizer.text(variant.nameKey)),
                                tone = TileTextTone.MAGENTA,
                                icon = variant.visualTintKey?.let { resolveVisual(visualResolver, it) },
                            )
                    }
                    rows += TileTextRow("${localizer.text("ui.hud.hp.short")} ${inspected.currentHp}/${inspected.maxHp}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.hud.attack.short")} ${inspected.attack}  ${localizer.text("ui.hud.defense.short")} ${inspected.defense}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.hud.accuracy.short")} ${inspected.accuracy}  ${localizer.text("ui.hud.evasion.short")} ${inspected.evasion}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.stat.str")} ${inspected.strength}  ${localizer.text("ui.stat.dex")} ${inspected.dexterity}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.stat.con")} ${inspected.constitution}  ${localizer.text("ui.stat.wil")} ${inspected.willpower}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.hud.speed.short")} ${inspected.speed}", TileTextTone.WHITE)
                    inspected.mutations.forEach { mutation ->
                        rows +=
                            TileTextRow(
                                text = localizer.text("ui.inspect.mutation.line", "mutation" to localizer.text(mutation.nameKey)),
                                tone = TileTextTone.CYAN,
                                icon = resolveVisual(visualResolver, mutation.iconKey),
                            )
                        mutation.summary?.let { summary ->
                            rows += TileTextRow(renderTextToken(localizer, summary), TileTextTone.LIGHT_GRAY)
                        }
                    }
                    inspected.statusEffects.forEach { effect ->
                        DescriptionPresenter.presentStatusEffectLines(localizer, effect).forEach { line ->
                            rows +=
                                TileTextRow(
                                    text = line.text,
                                    tone = descriptionTone(line),
                                    icon = effect.iconKey?.let { resolveVisual(visualResolver, it) },
                                )
                        }
                    }
                }
                if (cell.items.isNotEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.items"), TileTextTone.GOLD)
                    cell.items.forEach { item ->
                        val presentation = QualityPresentation.from(item)
                        rows +=
                            TileTextRow(
                                text = renderItemDisplay(localizer, item, presentation),
                                tone = itemTone(presentation),
                                icon = item.iconKey?.let { resolveVisual(visualResolver, it) },
                            )
                        DescriptionPresenter.presentInspectItemLines(localizer, item).forEach { line ->
                            rows += TileTextRow(line.text, descriptionTone(line))
                        }
                        itemDetailLines(localizer, item).forEach { detail ->
                            rows += TileTextRow(detail, TileTextTone.LIGHT_GRAY)
                        }
                    }
                }
                prop?.nameKey?.let { nameKey ->
                    rows +=
                        TileTextRow(
                            text = localizer.text(nameKey),
                            tone = TileTextTone.GOLD,
                            icon = resolveVisual(visualResolver, prop.visualKey),
                        )
                    prop.stateLabelKey?.let { stateLabelKey ->
                        rows += TileTextRow(localizer.text(stateLabelKey), TileTextTone.CYAN)
                    }
                    DescriptionPresenter
                        .presentInspectObjectLines(
                            localizer,
                            listOfNotNull(prop.descKey?.let(::RenderTextTokenSnapshot)),
                        ).forEach { line ->
                            rows += TileTextRow(line.text, descriptionTone(line))
                    }
                }
                cell.stairDirectionId?.let { directionId ->
                    rows +=
                        TileTextRow(
                            text = stairName(localizer, directionId),
                            tone = TileTextTone.LIGHT_GRAY,
                            icon = resolveVisual(visualResolver, stairVisualKey(directionId)),
                        )
                }
                if (actor == null && cell.items.isEmpty() && cell.stairDirectionId == null && prop == null) {
                    if (cell.visibility == CellVisibilitySnapshot.VISIBLE) {
                        rows += emptyStateRows(localizer, visualResolver, UiEmptyState.inspect())
                    } else {
                        rows +=
                            TileTextRow(
                                text =
                                    when (cell.visibility) {
                                        CellVisibilitySnapshot.VISIBLE -> error("Visible inspect empty state is handled above.")
                                        CellVisibilitySnapshot.EXPLORED -> localizer.text("ui.inspect.explored_not_visible")
                                        CellVisibilitySnapshot.HIDDEN -> localizer.text("ui.inspect.unknown_tile")
                                    },
                                tone = TileTextTone.LIGHT_GRAY,
                        )
                    }
                }
                if (overlayState.explainPaneOpen) {
                    rows += explainPaneRows(
                        localizer = localizer,
                        model =
                            ExplainPaneModel.fromInspectSurface(
                                localizer = localizer,
                                actor = actor,
                                item = cell.items.firstOrNull(),
                                prop = prop,
                                terrainOverride = cell.terrainOverride,
                                overlay = overlaysAtCursor.firstOrNull(),
                            ),
                    )
                }
                rows += TileTextRow(localizer.text("ui.controls.inspect"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.VALIDATION -> {
                rows += validationOverlayRows(localizer, requireNotNull(overlayState.validationPanel))
            }

            UiMode.STAT_ASSIGN -> {
                rows += TileTextRow(localizer.text("ui.sidebar.points", "value" to snapshot.uiState.playerStatus.statPoints), TileTextTone.WHITE)
                rows += TileTextRow("1. ${localizer.text("ui.stat.str")}", TileTextTone.WHITE)
                rows += TileTextRow("2. ${localizer.text("ui.stat.dex")}", TileTextTone.WHITE)
                rows += TileTextRow("3. ${localizer.text("ui.stat.con")}", TileTextTone.WHITE)
                rows += TileTextRow("4. ${localizer.text("ui.stat.wil")}", TileTextTone.WHITE)
            }

            UiMode.TALENT_ASSIGN -> {
                TalentSidebarPresenter.present(localizer, snapshot.uiState, overlayState).forEach { line ->
                    rows +=
                        TileTextRow(
                            text = line.text,
                            tone = talentSidebarTone(line),
                            icon = line.iconKey?.let { resolveVisual(visualResolver, it) },
                            selected = line.selected,
                        )
                }
            }
        }

        if (overlayState.mode in recentRewardPresentationModes && snapshot.uiState.recentRewards.isNotEmpty()) {
            rows += TileTextRow(localizer.text("ui.sidebar.recent_rewards"), TileTextTone.GOLD)
            snapshot.uiState.recentRewards.asReversed().forEach { entry ->
                rows +=
                    TileTextRow(
                        recentRewardText(
                            sourceLabel = renderTextToken(localizer, ModalCardModel.rewardPresentationSummary(entry)),
                            itemDisplayName = renderTextToken(localizer, entry.itemDisplayName),
                        ),
                        rewardPresentationTone(entry.source),
                    )
                ModalCardModel.rewardPresentationDetailLines(entry).forEach { detailText ->
                    rows += TileTextRow(recentRewardDetailText(renderTextToken(localizer, detailText)), TileTextTone.LIGHT_GRAY)
                }
            }
        }

        return TileSidebarModel(title = title, rows = rows)
    }

    private fun actorTintColorHex(
        visualResolver: VisualManifestResolver,
        actor: ActorRenderSnapshot,
    ): String? =
        actor.displayTintColorHex
            ?: actor.bossVariant?.visualTintKey
                ?.let { tintKey -> resolveVisual(visualResolver, tintKey) }
                ?.takeIf { resolved -> resolved.fallbackUsed.not() }
                ?.entry
                ?.tintColorHex

    private fun rewardPresentationTone(source: RewardPresentationSourceSnapshot): TileTextTone =
        when (source) {
            RewardPresentationSourceSnapshot.CADENCE -> TileTextTone.GOLD
            RewardPresentationSourceSnapshot.ROUTE -> TileTextTone.GREEN
            RewardPresentationSourceSnapshot.BOSS -> TileTextTone.RED
            RewardPresentationSourceSnapshot.CACHE -> TileTextTone.CYAN
            RewardPresentationSourceSnapshot.SUPPORT -> TileTextTone.BLUE
            RewardPresentationSourceSnapshot.HIDDEN_EVENT -> TileTextTone.MAGENTA
            RewardPresentationSourceSnapshot.SECRET_ZONE -> TileTextTone.GREEN
        }

    private fun emptyStateRows(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        emptyState: UiEmptyState,
    ): List<TileTextRow> =
        listOf(
            TileTextRow(
                text = renderTextToken(localizer, emptyState.title),
                tone = TileTextTone.GOLD,
                icon = resolveVisual(visualResolver, emptyState.iconKey),
            ),
            TileTextRow(renderTextToken(localizer, emptyState.detail), TileTextTone.LIGHT_GRAY),
        )

    private fun emptyLogMessageLines(localizer: Localizer): List<TileMessageLine> {
        val emptyState = UiEmptyState.log()
        return listOf(
            TileMessageLine(renderTextToken(localizer, emptyState.title), TileTextTone.GOLD),
            TileMessageLine(renderTextToken(localizer, emptyState.detail), TileTextTone.LIGHT_GRAY),
        )
    }

    private fun explainPaneRows(
        localizer: Localizer,
        model: ExplainPaneModel,
    ): List<TileTextRow> =
        buildList {
            add(TileTextRow(renderTextToken(localizer, model.card.title), TileTextTone.GOLD))
            model.keywordChips.forEach { chip -> add(TileTextRow(renderTextToken(localizer, chip), TileTextTone.CYAN)) }
            model.referenceChain.forEach { ref -> add(TileTextRow(renderTextToken(localizer, ref), TileTextTone.GRAY)) }
        }

    private fun modalCardHeaderRow(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        card: ModalCardModel,
        prefix: String,
        tone: TileTextTone,
        selected: Boolean = false,
    ): TileTextRow {
        val title = renderTextToken(localizer, card.title)
        val detailSuffix =
            card.detailLines
                .takeIf { details -> details.isNotEmpty() }
                ?.joinToString(separator = " / ", prefix = " [", postfix = "]") { detail -> renderTextToken(localizer, detail) }
                .orEmpty()
        val summarySuffix = card.summary?.let { summary -> " - ${renderTextToken(localizer, summary)}" }.orEmpty()
        val disabledSuffix = card.disabledReason?.let { reason -> " - ${renderTextToken(localizer, reason)}" }.orEmpty()
        return TileTextRow(
            text = "$prefix $title$detailSuffix$summarySuffix$disabledSuffix",
            tone = tone,
            icon = card.iconKey?.let { iconKey -> resolveVisual(visualResolver, iconKey) },
            selected = selected,
        )
    }

    private fun modalCardRewardTone(line: RenderTextTokenSnapshot): TileTextTone =
        when (line.key) {
            "ui.world_map.milestone_reward" -> TileTextTone.GOLD
            else -> TileTextTone.GREEN
        }

    private fun GroundLootMarkerModel.toTileMarker(visualResolver: VisualManifestResolver): TileGroundLootMarkerModel =
        TileGroundLootMarkerModel(
            x = x,
            y = y,
            icon = resolveVisual(visualResolver, iconKey),
            countBadge = countBadge,
            cornerGlyph = qualityPresentation.cornerGlyph,
            rarityTone = qualityTone(qualityPresentation),
            specialAccentTokenId = qualityPresentation.specialAccentTokenId,
            placement = placement,
        )

    private fun combatDecisionTargetCursorState(
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): TileTargetCursorState? {
        if (overlayState.mode != UiMode.TARGETING || overlayState.activeModalKind != ModalFrameKind.COMBAT_DECISION) {
            return null
        }
        val localState = overlayState.modalFrames.lastOrNull()?.localState ?: return null
        val state = localState.combatDecisionState ?: return null
        if (state.phase != com.ktome.client.ui.combat.CombatDecisionPhase.TARGET) {
            return null
        }
        val cursor = overlayState.targetingCursor ?: localState.targetingCursor ?: return null
        overlayState.validationCombatDecisionSurface?.let { surface ->
            if (surface == CombatDecisionValidationSurface.NO_LEGAL_TARGET) {
                return null
            }
            return if (cursor == CombatDecisionValidationFixtures.legalTargetPoint(snapshot)) {
                TileTargetCursorState.LEGAL
            } else {
                TileTargetCursorState.ILLEGAL
            }
        }
        val action = CombatDecisionFrame.selectedAction(snapshot, state) ?: return TileTargetCursorState.ILLEGAL
        if (action.usesFreeCursorTargeting()) {
            return TileTargetCursorState.LEGAL
        }
        val legalTargets = CombatDecisionFrame.legalTargets(snapshot, action)
        return if (legalTargets.any { target -> target.point == cursor }) {
            TileTargetCursorState.LEGAL
        } else {
            TileTargetCursorState.ILLEGAL
        }
    }

    private fun com.ktome.client.ui.combat.CombatDecisionPanelModel.toActionPanel(visualResolver: VisualManifestResolver): ActionPanelModel =
        ActionPanelModel(
            entries =
                rows.take(4).mapIndexed { index, row ->
                    ActionPanelEntryModel(
                        hotkey = (index + 1).toString(),
                        label = row.text,
                        enabled = row.enabled,
                        icon = row.iconKey?.let { iconKey -> resolveVisual(visualResolver, iconKey) },
                    )
                },
            emptyStateText = title,
        )

    private fun itemTone(item: ItemRenderSnapshot): TileTextTone =
        itemTone(QualityPresentation.from(item))

    private fun itemTone(presentation: QualityPresentation): TileTextTone =
        qualityTone(presentation)

    private fun qualityTone(presentation: QualityPresentation): TileTextTone =
        when (presentation.colorTokenId) {
            QualityColorTokenId.NORMAL -> TileTextTone.WHITE
            QualityColorTokenId.MAGIC -> TileTextTone.BLUE
            QualityColorTokenId.RARE -> TileTextTone.GOLD
        }

    private fun focusedActor(
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        actorById: Map<Int, ActorRenderSnapshot>,
        cellByPoint: Map<com.ktome.core.map.Point, MapCellSnapshot>,
    ): ActorRenderSnapshot? {
        val focusPoint =
            when (overlayState.mode) {
                UiMode.INSPECT -> overlayState.inspectCursor
                UiMode.TARGETING -> overlayState.targetingCursor
                UiMode.VALIDATION -> overlayState.validationPanel?.inspectCursor
                else -> null
            } ?: return null
        val actorId = cellByPoint[focusPoint]?.actorEntityId ?: return null
        return actorById[actorId]
    }

    private fun validationOverlayRows(
        localizer: Localizer,
        panel: ValidationOverlayPanelState,
    ): List<TileTextRow> {
        val rows = mutableListOf<TileTextRow>()
        val summary = panel.summary
        rows += TileTextRow(localizer.text("ui.validation.overlay.summary"), TileTextTone.GOLD)
        rows += TileTextRow(localizer.text("ui.validation.entry.preset", "value" to localizer.text(summary.preset.titleKey)), TileTextTone.WHITE)
        rows += TileTextRow(localizer.text("ui.validation.entry.seed", "value" to summary.seed), TileTextTone.WHITE)
        if (summary.seedCorpus.size > 1) {
            rows +=
                TileTextRow(
                    localizer.text(
                        "ui.validation.entry.seed_corpus",
                        "value" to summary.seedCorpus.joinToString(", "),
                    ),
                    TileTextTone.WHITE,
                )
        }
        rows += TileTextRow(localizer.text("ui.validation.entry.zone", "value" to localizer.text(panel.zoneNameKey)), TileTextTone.WHITE)
        rows += TileTextRow(localizer.text("ui.validation.entry.floor", "value" to summary.floor), TileTextTone.WHITE)
        rows +=
            TileTextRow(
                localizer.text(
                    "ui.validation.active_packs",
                    "value" to ValidationPackSummaryText.activePackIds(localizer, summary.activePackIds),
                ),
                TileTextTone.WHITE,
            )
        rows +=
            TileTextRow(
                localizer.text(
                    "ui.validation.pack.namespaces",
                    "value" to ValidationPackSummaryText.namespaces(localizer, summary.activePackSummaries),
                ),
                TileTextTone.WHITE,
            )
        rows +=
            TileTextRow(
                localizer.text(
                    "ui.validation.pack.overlay_ops",
                    "value" to ValidationPackSummaryText.overlayOps(localizer, summary.activePackSummaries),
                ),
                TileTextTone.WHITE,
            )
        rows +=
            TileTextRow(
                localizer.text(
                    "ui.validation.pack.touched_ids",
                    "value" to ValidationPackSummaryText.touchedContentIds(localizer, summary.touchedContentIds),
                ),
                TileTextTone.WHITE,
            )
        rows +=
            TileTextRow(
                localizer.text(
                    "ui.validation.pack.key_resolution",
                    "visual" to summary.packKeyResolutionSummary.resolvedVisualKeys,
                    "audio" to summary.packKeyResolutionSummary.resolvedAudioKeys,
                    "locale" to summary.packKeyResolutionSummary.resolvedLocaleKeys,
                    "overrides" to summary.packKeyResolutionSummary.overriddenKeys,
                    "warnings" to summary.packKeyResolutionSummary.warningCount,
                ),
                TileTextTone.WHITE,
            )
        rows +=
            TileTextRow(
                ValidationPackSummaryText.keyWarnings(localizer, summary.packKeyResolutionSummary),
                TileTextTone.WHITE,
            )
        summary.packVisibilityComparison?.let { comparison ->
            rows +=
                TileTextRow(
                    localizer.text(
                        "ui.validation.pack.no_pack_state",
                        "active" to ValidationPackSummaryText.activePackIds(localizer, comparison.noPackState.activePackIds),
                        "ops" to ValidationPackSummaryText.overlayOps(localizer, comparison.noPackState.activePackSummaries),
                        "touched" to ValidationPackSummaryText.touchedContentIds(localizer, comparison.noPackState.touchedContentIds),
                    ),
                    TileTextTone.WHITE,
                )
            rows +=
                TileTextRow(
                    localizer.text(
                        "ui.validation.pack.active_sample_state",
                        "active" to ValidationPackSummaryText.activePackIds(localizer, comparison.activeSamplePackState.activePackIds),
                        "ops" to ValidationPackSummaryText.overlayOps(localizer, comparison.activeSamplePackState.activePackSummaries),
                        "touched" to ValidationPackSummaryText.touchedContentIds(localizer, comparison.activeSamplePackState.touchedContentIds),
                    ),
                    TileTextTone.WHITE,
                )
        }
        rows +=
            TileTextRow(
                localizer.text(
                    "ui.validation.entry.boss_variant_mode",
                    "value" to localizer.text(bossVariantModeLabelKey(summary.bossVariantModeId)),
                ),
                TileTextTone.WHITE,
            )
        rows +=
            TileTextRow(
                localizer.text(
                    "ui.validation.entry.preferred_variant",
                    "value" to (summary.preferredBossVariantId ?: localizer.text("ui.validation.none")),
                ),
                TileTextTone.WHITE,
            )
        rows += TileTextRow(localizer.text("ui.inspect.cursor", "x" to panel.inspectCursor.x, "y" to panel.inspectCursor.y), TileTextTone.WHITE)
        rows +=
            TileTextRow(
                localizer.text(
                    "ui.validation.overlay.last_result",
                    "value" to (summary.lastResult?.let { token -> renderTextToken(localizer, token) } ?: localizer.text("ui.validation.none")),
                ),
                TileTextTone.LIGHT_GRAY,
            )
        rows += TileTextRow(localizer.text("ui.validation.phase4.targets"), TileTextTone.GOLD)
        panel.phase4Guide.targetLabelKeys.forEach { labelKey ->
            rows += TileTextRow("  ${localizer.text(labelKey)}", TileTextTone.WHITE)
        }
        rows += TileTextRow(localizer.text("ui.validation.phase4.quick_paths"), TileTextTone.GOLD)
        panel.phase4Guide.quickPathLabelKeys.forEach { labelKey ->
            rows += TileTextRow("  ${localizer.text(labelKey)}", TileTextTone.LIGHT_GRAY)
        }
        rows += TileTextRow(localizer.text("ui.validation.phase4.evidence"), TileTextTone.GOLD)
        panel.phase4Guide.evidenceLabelKeys.forEach { labelKey ->
            rows += TileTextRow("  ${localizer.text(labelKey)}", TileTextTone.LIGHT_GRAY)
        }
        panel.scenarioContext?.requiredEvidenceKeys.orEmpty().forEach { evidencePath ->
            rows += TileTextRow("  $evidencePath", TileTextTone.LIGHT_GRAY)
        }
        panel.summary.scenarioEvidenceSummary?.let { evidenceSummary ->
            rows += TileTextRow(localizer.text("ui.validation.phase4.v4.evidence_summary"), TileTextTone.GOLD)
            validationScenarioEvidenceSummaryLines(localizer, evidenceSummary).forEach { row ->
                rows += TileTextRow("  $row", TileTextTone.LIGHT_GRAY)
            }
        }
        rows += TileTextRow(localizer.text("ui.controls.validation"), TileTextTone.LIGHT_GRAY)
        panel.sections.forEach { section ->
            rows +=
                TileTextRow(
                    text = localizer.text(section.titleKey),
                    tone = if (section.selected) TileTextTone.GOLD else TileTextTone.WHITE,
                    selected = section.selected,
                )
            section.actions.forEach { action ->
                rows +=
                    TileTextRow(
                        text = "  ${localizer.text(action.labelKey)}",
                        tone = if (action.selected) TileTextTone.CYAN else TileTextTone.LIGHT_GRAY,
                        selected = action.selected,
                    )
            }
        }
        return rows
    }

    private fun frontstageFocus(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        focusActor: ActorRenderSnapshot?,
    ): Pair<String, List<String>>? {
        if (overlayState.mode != UiMode.MAP || focusActor != null) {
            return null
        }
        val lines =
            frontstagePresentationEntries(snapshot).map { entry -> renderTextToken(localizer, entry.token) } +
                snapshot.uiState.recentRewards.asReversed().flatMap { entry ->
                    listOf(
                        recentRewardText(
                            sourceLabel = renderTextToken(localizer, ModalCardModel.rewardPresentationSummary(entry)),
                            itemDisplayName = renderTextToken(localizer, entry.itemDisplayName),
                        ),
                    ) +
                        ModalCardModel.rewardPresentationDetailLines(entry).map { detailText ->
                            recentRewardDetailText(renderTextToken(localizer, detailText))
                        }
                }
        if (lines.isEmpty()) {
            return null
        }
        return localizer.text("ui.hud.frontstage.title") to lines
    }

    private fun frontstageSidebarRows(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): List<TileTextRow> =
        frontstagePresentationEntries(snapshot).map { entry ->
            TileTextRow(
                text = renderTextToken(localizer, entry.token),
                tone =
                    when (entry.kind) {
                        FrontstagePresentationKind.MUTATION -> TileTextTone.WHITE
                        FrontstagePresentationKind.TERRAIN -> TileTextTone.CYAN
                        FrontstagePresentationKind.SEARCH -> TileTextTone.GREEN
                        FrontstagePresentationKind.SECRET -> TileTextTone.GOLD
                        FrontstagePresentationKind.PASSIVE -> TileTextTone.LIGHT_GRAY
                    },
            )
        }

    private fun resourceTone(resourceTypeId: String): TileTextTone =
        when (resourceTypeId) {
            "MANA" -> TileTextTone.BLUE
            "ENERGY" -> TileTextTone.CYAN
            "POSITIVE_ENERGY" -> TileTextTone.GOLD
            else -> TileTextTone.GREEN
        }

    private fun messageTone(messageKey: String): TileTextTone =
        when {
            messageKey == "log.talent.damage_resisted" -> TileTextTone.BLUE
            messageKey == "log.talent.damage_vulnerable" -> TileTextTone.RED
            messageKey.startsWith("log.passive.") -> TileTextTone.GREEN
            messageKey.startsWith("log.level_up") -> TileTextTone.GOLD
            messageKey.startsWith("log.reward.") -> TileTextTone.GOLD
            messageKey == "log.zone.enter" -> TileTextTone.CYAN
            messageKey.startsWith("log.boss.") -> TileTextTone.RED
            else -> TileTextTone.WHITE
        }

    private fun buildCombatFeedback(
        localizer: Localizer,
        mapWidth: Int,
        overlayCells: Set<Pair<Int, Int>>,
        feedbackEvents: List<CombatFeedbackSnapshot>,
    ): List<TileCombatFeedbackModel> {
        val stackCounts = mutableMapOf<Pair<Int, Int>, Int>()
        return feedbackEvents.map { event ->
            val key = event.x to event.y
            val stackIndex = stackCounts.getOrDefault(key, 0)
            stackCounts[key] = stackIndex + 1
            val horizontalOffsetCells =
                if (key in overlayCells) {
                    if (event.x >= mapWidth - 1) {
                        -1
                    } else {
                        1
                    }
                } else {
                    0
                }
            TileCombatFeedbackModel(
                x = event.x,
                y = event.y,
                text = combatFeedbackText(localizer, event),
                tone = combatFeedbackTone(event),
                stackIndex = stackIndex,
                horizontalOffsetCells = horizontalOffsetCells,
            )
        }
    }

    private fun combatFeedbackText(
        localizer: Localizer,
        event: CombatFeedbackSnapshot,
    ): String =
        when (event.type) {
            CombatFeedbackTypeSnapshot.DAMAGE -> "${event.amount ?: 0}${if (event.critical) "!" else ""}"
            CombatFeedbackTypeSnapshot.HEAL -> "+${event.amount ?: 0}"
            CombatFeedbackTypeSnapshot.MISS -> localizer.text("ui.combat_feedback.miss")
            CombatFeedbackTypeSnapshot.STATUS_APPLIED -> "+${event.statusNameKey?.let(localizer::text).orEmpty()}"
            CombatFeedbackTypeSnapshot.STATUS_REMOVED -> "-${event.statusNameKey?.let(localizer::text).orEmpty()}"
        }

    private fun combatFeedbackTone(event: CombatFeedbackSnapshot): TileTextTone =
        when (event.type) {
            CombatFeedbackTypeSnapshot.DAMAGE ->
                if (event.critical) {
                    TileTextTone.GOLD
                } else {
                    damageTone(event.damageTypeId)
                }

            CombatFeedbackTypeSnapshot.HEAL -> TileTextTone.GREEN
            CombatFeedbackTypeSnapshot.MISS -> TileTextTone.LIGHT_GRAY
            CombatFeedbackTypeSnapshot.STATUS_APPLIED -> TileTextTone.CYAN
            CombatFeedbackTypeSnapshot.STATUS_REMOVED -> TileTextTone.GRAY
        }

    private fun damageTone(damageTypeId: String?): TileTextTone =
        when (damageTypeId) {
            "FIRE" -> TileTextTone.RED
            "COLD" -> TileTextTone.CYAN
            "LIGHTNING" -> TileTextTone.GOLD
            "HOLY" -> TileTextTone.GOLD
            "SHADOW" -> TileTextTone.MAGENTA
            else -> TileTextTone.WHITE
        }

    private fun ResourceGaugeModel.toTileGauge(): TileGaugeModel =
        TileGaugeModel(
            label = label,
            current = current,
            max = max,
            tone = resourceTone(resourceTypeId),
            resourceTypeId = resourceTypeId,
            stableMin = stableMin,
            stableMax = stableMax,
        )

    private fun descriptionTone(line: DescriptionLine): TileTextTone =
        when (line.kind) {
            DescriptionLineKind.SECONDARY -> TileTextTone.GRAY
            DescriptionLineKind.PRIMARY,
            DescriptionLineKind.KEYWORD,
            DescriptionLineKind.STATE,
            -> TileTextTone.LIGHT_GRAY
        }

    private fun talentSidebarTone(line: TalentSidebarLine): TileTextTone =
        if (line.selected) {
            TileTextTone.CYAN
        } else {
            when (line.role) {
                TalentSidebarLineRole.POINTS,
                TalentSidebarLineRole.ACTION_HINT,
                -> TileTextTone.WHITE

                TalentSidebarLineRole.ACTION_TITLE -> TileTextTone.CYAN
                TalentSidebarLineRole.TREE_HEADER -> TileTextTone.GOLD
                TalentSidebarLineRole.NODE_LOCKED -> TileTextTone.GRAY
                TalentSidebarLineRole.NODE_LEARNABLE -> TileTextTone.GREEN
                TalentSidebarLineRole.NODE_LEARNED_RESERVE -> TileTextTone.WHITE
                TalentSidebarLineRole.NODE_LEARNED_ACTIVE -> TileTextTone.GOLD
                TalentSidebarLineRole.DESCRIPTION_SECONDARY -> TileTextTone.GRAY
                TalentSidebarLineRole.DESCRIPTION_PRIMARY,
                TalentSidebarLineRole.DESCRIPTION_STATE,
                TalentSidebarLineRole.FOOTER,
                -> TileTextTone.LIGHT_GRAY
            }
        }

    private fun resolveVisual(
        visualResolver: VisualManifestResolver,
        key: String,
    ): ResolvedVisualAsset {
        val resolved = visualResolver.resolve(key)
        require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
            "Tile render model requires exact visual key '$key'."
        }
        return resolved
    }

    private fun terrainName(
        localizer: Localizer,
        cell: MapCellSnapshot,
    ): String =
        when (cell.terrainTypeId) {
            "water" -> localizer.text("tile.water.name")
            "oil" -> localizer.text("tile.oil.name")
            "ice" -> localizer.text("tile.ice.name")
            "floor" -> localizer.text("tile.floor.name")
            "wall" -> localizer.text("tile.wall.name")
            else -> localizer.text("tile.unknown.name")
        }

    private fun damageTypeLabelKey(damageTypeId: String): String =
        when (damageTypeId) {
            "PHYSICAL" -> "damage_type.physical.name"
            "FIRE" -> "damage_type.fire.name"
            "COLD" -> "damage_type.cold.name"
            "LIGHTNING" -> "damage_type.lightning.name"
            "HOLY" -> "damage_type.holy.name"
            "SHADOW" -> "damage_type.shadow.name"
            else -> "damage_type.physical.name"
        }

    private fun actorRole(
        localizer: Localizer,
        actor: ActorRenderSnapshot,
    ): String =
        when (actor.roleKind ?: ActorRoleKindSnapshot.GENERIC) {
            ActorRoleKindSnapshot.PLAYER -> localizer.text("actor.player.role")
            ActorRoleKindSnapshot.BOSS -> localizer.text("actor.boss.role")
            ActorRoleKindSnapshot.MONSTER ->
                localizer.text(
                    "actor.monster.role",
                    "ai" to aiLabel(localizer, actor.aiTypeId),
                )
            ActorRoleKindSnapshot.GENERIC -> localizer.text("actor.generic.role")
        }

    private fun aiLabel(
        localizer: Localizer,
        aiTypeId: String?,
    ): String =
        when (aiTypeId) {
            "CHASE" -> localizer.text("ai.chase")
            "KITE" -> localizer.text("ai.kite")
            "PATROL" -> localizer.text("ai.patrol")
            else -> localizer.text("actor.generic.role")
        }

    private fun renderLogEvent(
        localizer: Localizer,
        event: RenderLogEventSnapshot,
    ): String =
        localizer.text(
            event.message.key,
            *event.message.arguments.map { argument -> argument.name to resolveArgument(localizer, argument) }.toTypedArray(),
        )

    private fun resolveArgument(
        localizer: Localizer,
        argument: RenderTextArgumentSnapshot,
    ): String =
        argument.valueToken?.let { token -> renderTextToken(localizer, token) }
            ?: argument.valueKey?.let(localizer::text)
            ?: argument.value.orEmpty()

    private fun renderItemDisplay(
        localizer: Localizer,
        item: ItemRenderSnapshot,
        presentation: QualityPresentation = QualityPresentation.from(item),
    ): String {
        val baseName = item.displayName?.let { token -> renderTextToken(localizer, token) } ?: localizer.text(item.nameKey)
        return presentation.cornerGlyph?.let { glyph -> "$glyph $baseName" } ?: baseName
    }

    private fun talentUsageSummary(
        localizer: Localizer,
        talent: TalentSlotSnapshot,
    ): String = talentUsageSummary(localizer, talent.resourceCost, talent.resourceLabelKey, talent.requiresTarget, talent.range)

    private fun talentUsageSummary(
        localizer: Localizer,
        talent: TalentReserveSnapshot,
    ): String = talentUsageSummary(localizer, talent.resourceCost, talent.resourceLabelKey, talent.requiresTarget, talent.range)

    private fun talentUsageSummary(
        localizer: Localizer,
        resourceCost: Int,
        resourceLabelKey: String,
        requiresTarget: Boolean,
        range: Int,
    ): String {
        val resourceText = "$resourceCost ${localizer.text(resourceLabelKey)}"
        if (!requiresTarget) {
            return resourceText
        }
        return "$resourceText · ${localizer.text("ui.sidebar.target.range", "range" to range)}"
    }

    private fun talentSidebarLabel(
        localizer: Localizer,
        talent: TalentSlotSnapshot,
    ): String {
        val targetSuffix =
            if (talent.requiresTarget) {
                " [${localizer.text("ui.sidebar.target.range", "range" to talent.range)}]"
            } else {
                ""
        }
        return "${talent.slot}. ${localizer.text(talent.nameKey)} ${talentRankLabel(talent.level, talent.committedLevel, talent.maxLevel)}$targetSuffix"
    }

    private fun reserveTalentLabel(
        localizer: Localizer,
        talent: TalentReserveSnapshot,
    ): String {
        val targetSuffix =
            if (talent.requiresTarget) {
                " [${localizer.text("ui.sidebar.target.range", "range" to talent.range)}]"
            } else {
                ""
            }
        return "${localizer.text(talent.nameKey)} ${talentRankLabel(talent.level, talent.committedLevel, talent.maxLevel)}$targetSuffix"
    }

    private fun loadoutSlotLabel(
        localizer: Localizer,
        slot: Int,
        talent: TalentSlotSnapshot?,
    ): String =
        if (talent == null) {
            "$slot. ${localizer.text("ui.sidebar.empty")}"
        } else {
            "$slot. ${localizer.text(talent.nameKey)} ${talentRankLabel(talent.level, talent.committedLevel, talent.maxLevel)}"
        }

    private fun talentRankLabel(
        level: Int,
        committedLevel: Int,
        maxLevel: Int,
    ): String =
        if (level == committedLevel) {
            "$level/$maxLevel"
        } else {
            "$level/$maxLevel (live $committedLevel)"
        }

    private fun renderTextToken(
        localizer: Localizer,
        token: RenderTextTokenSnapshot,
    ): String =
        localizer.text(
            token.key,
            *token.arguments.map { argument -> argument.name to resolveArgument(localizer, argument) }.toTypedArray(),
        )

    private fun itemDetailLines(
        localizer: Localizer,
        item: ItemRenderSnapshot,
    ): List<String> =
        buildList {
            item.slotId?.let { slotId ->
                add(localizer.text("ui.inspect.slot", "slot" to equipmentSlotLabel(localizer, slotId)))
            }
            item.qualityNameKey?.let { qualityNameKey ->
                add(localizer.text("ui.inspect.quality", "quality" to localizer.text(qualityNameKey)))
            }
            item.materialNameKey?.let { materialNameKey ->
                add(localizer.text("ui.inspect.material", "material" to localizer.text(materialNameKey)))
            }
            item.affixNameKeys.forEach { affixNameKey ->
                add(localizer.text("ui.inspect.affix", "affix" to localizer.text(affixNameKey)))
            }
            addAll(statModifierLines(localizer, item.stats))
            when (item.effectTypeId) {
                "HEAL" -> add(localizer.text("ui.inspect.restore_hp", "amount" to item.magnitude))
                "TELEPORT" -> add(localizer.text("ui.inspect.teleport_random"))
                null -> Unit
            }
            if (isEmpty()) {
                add(localizer.text("ui.inspect.no_special_effect"))
            }
        }

    private fun selectedShopDescriptionRows(
        localizer: Localizer,
        shop: com.ktome.core.snapshot.ShopPanelSnapshot,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): List<DescriptionLine> =
        when (overlayState.shopFocus) {
            com.ktome.client.input.ShopFocus.BUY -> {
                val offer = shop.offers.firstOrNull { candidate -> candidate.index == overlayState.shopOfferSelection }
                offer?.let { selected ->
                    val card = ModalCardModel.shopOffer(shop.shopId, selected)
                    DescriptionPresenter.presentModalCardLines(localizer, card, DescriptionSurface.SHOP_ITEM)
                }.orEmpty()
            }

            com.ktome.client.input.ShopFocus.SELL -> {
                val sellEntry = shop.sellEntries.getOrNull(overlayState.inventorySelection)
                val item = sellEntry?.let { entry -> snapshot.uiState.inventory.firstOrNull { inventory -> inventory.index == entry.inventoryIndex }?.item }
                item?.let { selected -> DescriptionPresenter.presentShopItemLines(localizer, selected) }.orEmpty()
            }
        }

    private fun inscriptionReplacementPromptTileRow(line: InscriptionReplacementPromptLine): TileTextRow =
        TileTextRow(
            text = line.text,
            tone =
                if (line.selected) {
                    TileTextTone.CYAN
                } else {
                    when (line.tone) {
                        InscriptionReplacementPromptTone.TITLE -> TileTextTone.GOLD
                        InscriptionReplacementPromptTone.PRIMARY -> TileTextTone.WHITE
                        InscriptionReplacementPromptTone.SECONDARY -> TileTextTone.LIGHT_GRAY
                        InscriptionReplacementPromptTone.WARNING -> TileTextTone.RED
                    }
                },
            selected = line.selected,
        )

    private fun statModifierLines(
        localizer: Localizer,
        modifier: ItemStatModifierSnapshot,
    ): List<String> =
        buildList {
            addModifier(localizer.text("ui.stat.str"), modifier.str)
            addModifier(localizer.text("ui.stat.dex"), modifier.dex)
            addModifier(localizer.text("ui.stat.con"), modifier.con)
            addModifier(localizer.text("ui.stat.wil"), modifier.wil)
            addModifier(localizer.text("ui.hud.attack.short"), modifier.attack)
            addModifier(localizer.text("ui.hud.defense.short"), modifier.defense)
            addModifier(localizer.text("ui.hud.accuracy.short"), modifier.accuracy)
            addModifier(localizer.text("ui.hud.evasion.short"), modifier.evasion)
            addModifier(localizer.text("ui.hud.speed.short"), modifier.speed)
            addModifier(localizer.text("ui.hud.hp.short"), modifier.maxHp)
            addModifier(localizer.text("ui.hud.stamina.short"), modifier.maxStamina)
            addDecimalModifier(localizer.text("ui.inspect.mod.hp_regen"), modifier.hpRegen)
            addDecimalModifier(localizer.text("ui.inspect.mod.stamina_regen"), modifier.staminaRegen)
            addPercentModifier(localizer.text("ui.inspect.mod.crit"), modifier.critChance)
            addPercentModifier(localizer.text("ui.inspect.mod.talent"), modifier.talentPower)
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
            add("$label ${signed((value * 100).toInt())}%")
        }
    }

    private fun stairName(
        localizer: Localizer,
        directionId: String,
    ): String =
        when (directionId) {
            "UP" -> localizer.text("stairs.up.name")
            "DOWN" -> localizer.text("stairs.down.name")
            else -> directionId
        }

    private fun stairVisualKey(directionId: String): String =
        when (directionId) {
            "UP" -> "prop.stairs.up"
            else -> "prop.stairs.down"
        }

    private fun visibilityLabel(
        localizer: Localizer,
        visibility: CellVisibilitySnapshot,
    ): String =
        when (visibility) {
            CellVisibilitySnapshot.VISIBLE -> localizer.text("ui.inspect.visible")
            CellVisibilitySnapshot.EXPLORED -> localizer.text("ui.inspect.explored")
            CellVisibilitySnapshot.HIDDEN -> localizer.text("ui.inspect.hidden")
        }

    private fun point(
        x: Int,
        y: Int,
    ): com.ktome.core.map.Point = com.ktome.core.map.Point(x, y)

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
}
