package com.ktome.client.render

import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.input.OverlayState
import com.ktome.client.input.ShopFocus
import com.ktome.client.input.UiMode
import com.ktome.client.input.ValidationOverlayPanelState
import com.ktome.client.telegraph.TelegraphPresentationModel
import com.ktome.client.telegraph.TelegraphRenderer
import com.ktome.client.telegraph.TelegraphStyle
import com.ktome.client.ui.card.ModalCardModel
import com.ktome.client.ui.chrome.ChromeFrameAssets
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
import com.ktome.client.ui.layout.PaneFocusAnchor
import com.ktome.client.ui.panel.ActionPanelEntryModel
import com.ktome.client.ui.panel.ActionPanelModel
import com.ktome.client.ui.panel.LogPresentationModel
import com.ktome.client.ui.panel.LogPresentationModelBuilder
import com.ktome.client.ui.panel.PlayerCardModel
import com.ktome.client.ui.panel.TargetCardModel
import com.ktome.client.ui.state.UiEmptyState
import com.ktome.client.ui.status.StatusHudRenderer
import com.ktome.client.ui.status.StatusHudIconModel
import com.ktome.client.ui.status.StatusHudPresenter
import com.ktome.client.ui.status.StatusIconResolver
import com.ktome.client.ui.talent.ActiveSlotChoiceModalItem
import com.ktome.client.ui.talent.DescriptionLine
import com.ktome.client.ui.talent.DescriptionLineKind
import com.ktome.client.ui.talent.DescriptionPresenter
import com.ktome.client.ui.talent.DescriptionSurface
import com.ktome.client.ui.talent.TalentAssignPanelModel
import com.ktome.client.ui.talent.TalentAssignTreeRowModel
import com.ktome.client.ui.talent.TalentSidebarLine
import com.ktome.client.ui.talent.TalentSidebarLineRole
import com.ktome.client.ui.talent.TalentSidebarPresenter
import com.ktome.client.ui.talent.TalentTreeSelectionIdentity
import com.ktome.client.ui.talent.toTalentTreeSelectionIdentity
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.CombatFeedbackSnapshot
import com.ktome.core.snapshot.CombatFeedbackTypeSnapshot
import com.ktome.core.snapshot.InscriptionReplacementPromptSnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.ItemStatModifierSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RewardPresentationSourceSnapshot
import com.ktome.core.snapshot.RouteSelectionSnapshot
import com.ktome.core.snapshot.ShopPanelSnapshot
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

internal enum class TileTextRowKind {
    DEFAULT,
    SHOP_REPLACEMENT_SLOT,
}

private const val MAX_REPLACEMENT_SLOT_HINT_ROWS = 4
private const val INSCRIPTION_HOTKEY_START = 5

internal data class TileVisualPlacement(
    val x: Int,
    val y: Int,
    val asset: ResolvedVisualAsset,
    val alpha: Float = 1f,
    val tintColorHex: String? = null,
    val drawPriority: Int = 0,
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
    val visibility: CellVisibilitySnapshot,
)

internal enum class TileMapCellMaterialKind {
    FLOOR,
    WALL,
    HAZARD,
}

internal data class TileMapCellMaterialModel(
    val x: Int,
    val y: Int,
    val kind: TileMapCellMaterialKind,
    val visibility: CellVisibilitySnapshot,
    val variant: Int,
    val northOcclusion: Boolean,
    val southOcclusion: Boolean,
    val westOcclusion: Boolean,
    val eastOcclusion: Boolean,
)

internal data class TileTextRow(
    val text: String,
    val tone: TileTextTone,
    val icon: ResolvedVisualAsset? = null,
    val selected: Boolean = false,
    val extraIcons: List<ResolvedVisualAsset> = emptyList(),
    val frame: ResolvedVisualAsset? = null,
    val kind: TileTextRowKind = TileTextRowKind.DEFAULT,
)

internal fun TileTextRow.hasVisualIcons(): Boolean = icon != null || extraIcons.isNotEmpty()

internal fun TileTextRow.visualIconCount(): Int = (if (icon == null) 0 else 1) + extraIcons.size

internal inline fun TileTextRow.forEachVisualIcon(
    limit: Int = Int.MAX_VALUE,
    action: (ResolvedVisualAsset) -> Unit,
) {
    var emitted = 0
    icon?.let { firstIcon ->
        if (emitted < limit) {
            action(firstIcon)
            emitted += 1
        }
    }
    for (extraIcon in extraIcons) {
        if (emitted >= limit) {
            return
        }
        action(extraIcon)
        emitted += 1
    }
}

private fun ActiveSlotChoiceModalItem.renderKey(): String = slot?.toString() ?: hotkeyText

internal enum class TileTalentAssignReferenceChromeSlot(
    val visualKey: String,
) {
    SURFACE_TEXTURE("dark.uiux.pr04.talent_assign.chrome.surface_texture"),
    TOP_EDGE("dark.uiux.pr04.talent_assign.chrome.top_edge"),
    BOTTOM_EDGE("dark.uiux.pr04.talent_assign.chrome.bottom_edge"),
    LEFT_EDGE("dark.uiux.pr04.talent_assign.chrome.left_edge"),
    RIGHT_EDGE("dark.uiux.pr04.talent_assign.chrome.right_edge"),
    CORNER_TOP_LEFT("dark.uiux.pr04.talent_assign.chrome.corner_top_left"),
    CORNER_TOP_RIGHT("dark.uiux.pr04.talent_assign.chrome.corner_top_right"),
    CORNER_BOTTOM_LEFT("dark.uiux.pr04.talent_assign.chrome.corner_bottom_left"),
    CORNER_BOTTOM_RIGHT("dark.uiux.pr04.talent_assign.chrome.corner_bottom_right"),
}

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

internal data class TileTalentAssignPanelRenderModel(
    val panel: TalentAssignPanelModel,
    val sectionIcons: Map<String, ResolvedVisualAsset>,
    val rowIcons: Map<TalentTreeSelectionIdentity, ResolvedVisualAsset>,
    val detailBlockIcons: Map<Int, ResolvedVisualAsset>,
    val activeSlotChoiceItemIcons: Map<String, ResolvedVisualAsset>,
    val referenceChromeAssets: Map<TileTalentAssignReferenceChromeSlot, ResolvedVisualAsset>,
) {
    companion object {
        fun from(
            panel: TalentAssignPanelModel,
            visualResolver: VisualManifestResolver,
        ): TileTalentAssignPanelRenderModel =
            TileTalentAssignPanelRenderModel(
                panel = panel,
                sectionIcons =
                    panel.sections.mapNotNull { section ->
                        section.iconKey?.let { iconKey -> section.treeId to visualResolver.resolve(iconKey) }
                    }.toMap(),
                rowIcons =
                    panel.sections
                        .flatMap { section -> section.rows }
                        .mapNotNull { row ->
                            row.skillIconKey?.let { iconKey -> row.toTalentTreeSelectionIdentity() to visualResolver.resolve(iconKey) }
                        }.toMap(),
                detailBlockIcons =
                    panel.detail?.blocks.orEmpty().mapIndexedNotNull { index, block ->
                        block.iconKey?.let { iconKey -> index to visualResolver.resolve(iconKey) }
                    }.toMap(),
                activeSlotChoiceItemIcons =
                    panel.activeSlotChoiceModal?.items.orEmpty().mapNotNull { item ->
                        item.iconKey?.let { iconKey -> item.renderKey() to visualResolver.resolve(iconKey) }
                    }.toMap(),
                referenceChromeAssets =
                    TileTalentAssignReferenceChromeSlot.entries.associateWith { slot ->
                        visualResolver.resolve(slot.visualKey)
                    },
            )
    }
}

internal data class TilePanelModel(
    val title: String,
    val rows: List<TileTextRow>,
)

internal data class TileShellModel(
    val leftRail: TilePanelModel,
    val rightPanel: TilePanelModel,
    val footerHints: List<TileTextRow>,
    val demo: TileDemoShellModel = TileDemoShellModel.empty(),
)

internal enum class TileDemoNavItemKind {
    COMPASS,
    BAG,
    SCROLL,
    BOOK,
    GEAR,
}

internal enum class TileDemoNavItemState {
    SELECTED,
    IDLE,
}

internal data class TileDemoNavItemModel(
    val kind: TileDemoNavItemKind,
    val label: String,
    val state: TileDemoNavItemState,
)

internal enum class TileDemoSlotState {
    EMPTY,
    FILLED,
}

internal data class TileDemoSlotModel(
    val label: String,
    val detail: String?,
    val icon: ResolvedVisualAsset?,
    val state: TileDemoSlotState,
    val stableId: String? = null,
    val frame: ResolvedVisualAsset? = null,
    val selected: Boolean = false,
    val qualityTierId: String? = null,
    val quantityText: String? = null,
    val tooltipAnchorId: String? = null,
    val visualOnly: Boolean = false,
    val showBadge: Boolean = false,
)

internal data class TileDemoShellModel(
    val navItems: List<TileDemoNavItemModel>,
    val rightEquipmentTitle: String,
    val rightInscriptionsTitle: String,
    val rightBackpackTitle: String,
    val rightOperationHintsTitle: String,
    val equipmentSlots: List<TileDemoSlotModel>,
    val inscriptionSlots: List<TileDemoSlotModel>,
    val backpackSlots: List<TileDemoSlotModel>,
    val backpackPageLabel: String,
    val operationHints: List<String>,
    val heroSummaryLines: List<String>,
    val heroLevelText: String = "",
    val equipmentInventory: EquipmentInventoryPresentation = EquipmentInventoryPresentation.empty(),
    val operationRows: List<TileTextRow> = operationHints.map { hint -> TileTextRow(hint, TileTextTone.LIGHT_GRAY) },
) {
    companion object {
        fun empty(): TileDemoShellModel =
            TileDemoShellModel(
                navItems = emptyList(),
                rightEquipmentTitle = "",
                rightInscriptionsTitle = "",
                rightBackpackTitle = "",
                rightOperationHintsTitle = "",
                equipmentSlots = emptyList(),
                inscriptionSlots = emptyList(),
                backpackSlots = emptyList(),
                backpackPageLabel = "",
            operationHints = emptyList(),
            heroSummaryLines = emptyList(),
            heroLevelText = "",
            equipmentInventory = EquipmentInventoryPresentation.empty(),
            operationRows = emptyList(),
        )
    }
}

internal enum class TileFrontstageSurfaceKind {
    SHOP,
    SHOP_REPLACEMENT,
    ROUTE_SELECTION,
    STAT_ASSIGN,
    REWARD,
}

internal data class TileFrontstageCardModel(
    val title: String,
    val tone: TileTextTone,
    val summary: String? = null,
    val detailRows: List<TileTextRow> = emptyList(),
    val icon: ResolvedVisualAsset? = null,
    val stateIcon: ResolvedVisualAsset? = null,
    val typeIcon: ResolvedVisualAsset? = null,
    val frame: ResolvedVisualAsset? = null,
    val selected: Boolean = false,
    val disabled: Boolean = false,
)

internal data class TileFrontstageColumnModel(
    val title: String,
    val cards: List<TileFrontstageCardModel>,
)

internal data class TileFrontstageSurfaceModel(
    val kind: TileFrontstageSurfaceKind,
    val title: String,
    val eyebrow: String?,
    val summary: String?,
    val columns: List<TileFrontstageColumnModel>,
    val footerRows: List<TileTextRow>,
)

internal enum class TilePanelTooltipAnchorKind {
    EQUIPMENT_SLOT,
    INSCRIPTION_SLOT,
    BACKPACK_SLOT,
}

internal data class TilePanelTooltipModel(
    val anchorKind: TilePanelTooltipAnchorKind,
    val anchorIndex: Int,
    val anchorId: String,
    val titleLine: TileTextLine,
    val bodyLines: List<TileTextLine>,
)

internal data class TileDemoShellAssets(
    val outerFrame: ResolvedVisualAsset,
    val mapStageFrame: ResolvedVisualAsset,
    val mapStageBackdrop: ResolvedVisualAsset,
    val navRailFrame: ResolvedVisualAsset,
    val navButtonActive: ResolvedVisualAsset,
    val heroCardFrame: ResolvedVisualAsset,
    val actionDeckFrame: ResolvedVisualAsset,
    val logDeckFrame: ResolvedVisualAsset,
    val rightSectionDivider: ResolvedVisualAsset,
    val heroCrestPlaceholder: ResolvedVisualAsset,
    val commandHintPlate: ResolvedVisualAsset,
    val navCompass: ResolvedVisualAsset,
    val navBag: ResolvedVisualAsset,
    val navScroll: ResolvedVisualAsset,
    val navBook: ResolvedVisualAsset,
    val navGear: ResolvedVisualAsset,
) {
    fun navIcon(kind: TileDemoNavItemKind): ResolvedVisualAsset =
        when (kind) {
            TileDemoNavItemKind.COMPASS -> navCompass
            TileDemoNavItemKind.BAG -> navBag
            TileDemoNavItemKind.SCROLL -> navScroll
            TileDemoNavItemKind.BOOK -> navBook
            TileDemoNavItemKind.GEAR -> navGear
        }

    companion object {
        fun resolve(visualResolver: VisualManifestResolver): TileDemoShellAssets =
            TileDemoShellAssets(
                outerFrame = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_OUTER_FRAME),
                mapStageFrame = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_MAP_STAGE_FRAME),
                mapStageBackdrop = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_MAP_STAGE_BACKDROP),
                navRailFrame = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_NAV_RAIL_FRAME),
                navButtonActive = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_NAV_BUTTON_ACTIVE),
                heroCardFrame = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_HERO_CARD_FRAME),
                actionDeckFrame = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_ACTION_DECK_FRAME),
                logDeckFrame = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_LOG_DECK_FRAME),
                rightSectionDivider = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_RIGHT_SECTION_DIVIDER),
                heroCrestPlaceholder = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_HERO_CREST_PLACEHOLDER),
                commandHintPlate = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_COMMAND_HINT_PLATE),
                navCompass = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_NAV_COMPASS),
                navBag = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_NAV_BAG),
                navScroll = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_NAV_SCROLL),
                navBook = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_NAV_BOOK),
                navGear = visualResolver.resolve(DarkUiChromeVisualKeys.SHELL_NAV_GEAR),
            )
    }
}

internal data class TileChromeAssets(
    val panelBody: ResolvedVisualAsset,
    val panelFocus: ResolvedVisualAsset,
    val panelCornerTopLeft: ResolvedVisualAsset,
    val panelCornerTopRight: ResolvedVisualAsset,
    val panelCornerBottomLeft: ResolvedVisualAsset,
    val panelCornerBottomRight: ResolvedVisualAsset,
    val panelEdgeTop: ResolvedVisualAsset,
    val panelEdgeRight: ResolvedVisualAsset,
    val panelEdgeBottom: ResolvedVisualAsset,
    val panelEdgeLeft: ResolvedVisualAsset,
    val slotEmpty: ResolvedVisualAsset,
    val slotEquipped: ResolvedVisualAsset,
    val slotSelected: ResolvedVisualAsset,
    val tooltipBody: ResolvedVisualAsset,
    val modalBody: ResolvedVisualAsset,
    val hudHp: ResolvedVisualAsset,
    val hudStamina: ResolvedVisualAsset,
    val hudXp: ResolvedVisualAsset,
    val demoShell: TileDemoShellAssets,
) {
    val frameAssets: ChromeFrameAssets =
        ChromeFrameAssets(
            body = panelBody,
            cornerTopLeft = panelCornerTopLeft,
            cornerTopRight = panelCornerTopRight,
            cornerBottomLeft = panelCornerBottomLeft,
            cornerBottomRight = panelCornerBottomRight,
            edgeTop = panelEdgeTop,
            edgeRight = panelEdgeRight,
            edgeBottom = panelEdgeBottom,
            edgeLeft = panelEdgeLeft,
        )

    fun iconForGauge(gauge: TileGaugeModel): ResolvedVisualAsset? =
        when (gauge.resourceTypeId) {
            "HEALTH" -> hudHp
            "EXPERIENCE" -> hudXp
            "STAMINA" -> hudStamina
            else -> null
        }

    companion object {
        fun resolve(visualResolver: VisualManifestResolver): TileChromeAssets =
            TileChromeAssets(
                panelBody = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_BODY),
                panelFocus = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_FOCUS),
                panelCornerTopLeft = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_CORNER_TL),
                panelCornerTopRight = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_CORNER_TR),
                panelCornerBottomLeft = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_CORNER_BL),
                panelCornerBottomRight = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_CORNER_BR),
                panelEdgeTop = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_EDGE_TOP),
                panelEdgeRight = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_EDGE_RIGHT),
                panelEdgeBottom = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_EDGE_BOTTOM),
                panelEdgeLeft = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.PANEL_EDGE_LEFT),
                slotEmpty = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.SLOT_EMPTY),
                slotEquipped = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.SLOT_EQUIPPED),
                slotSelected = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.SLOT_SELECTED),
                tooltipBody = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.TOOLTIP_BODY),
                modalBody = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.MODAL_BODY),
                hudHp = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.HUD_HP),
                hudStamina = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.HUD_STAMINA),
                hudXp = resolveChromeAsset(visualResolver, DarkUiChromeVisualKeys.HUD_XP),
                demoShell = TileDemoShellAssets.resolve(visualResolver),
            )

        private fun resolveChromeAsset(
            visualResolver: VisualManifestResolver,
            key: String,
        ): ResolvedVisualAsset = visualResolver.resolve(key)
    }
}

internal data class TileRenderModel(
    val terrainTiles: List<TileVisualPlacement>,
    val mapCellMaterials: List<TileMapCellMaterialModel> = emptyList(),
    val propTiles: List<TileVisualPlacement>,
    val overlayTiles: List<TileVisualPlacement>,
    val groundLootMarkers: List<TileGroundLootMarkerModel>,
    val actorTiles: List<TileVisualPlacement>,
    val fogTiles: List<TileFogPlacement>,
    val targetCursorState: TileTargetCursorState?,
    val targetHighlights: List<TileTargetHighlightModel> = emptyList(),
    val hud: TileHudModel,
    val messageLines: List<TileMessageLine>,
    val logPresentation: LogPresentationModel,
    val playerCard: PlayerCardModel,
    val targetCard: TargetCardModel,
    val actionPanel: ActionPanelModel,
    val combatFeedback: List<TileCombatFeedbackModel>,
    val sidebar: TileSidebarModel,
    val talentAssignPanel: TileTalentAssignPanelRenderModel? = null,
    val inventoryWorkbench: InventoryWorkbenchPresentation? = null,
    val shell: TileShellModel,
    val frontstageSurface: TileFrontstageSurfaceModel? = null,
    val panelTooltip: TilePanelTooltipModel? = null,
    val playerTile: com.ktome.core.map.Point,
    val mapDimensions: TileMapDimensions,
    val chromeAssets: TileChromeAssets? = null,
)

internal object TileRenderModelBuilder {
    private const val DEMO_BACKPACK_PAGE_SIZE = 8
    private const val EXPLORED_FOG_ALPHA = 0.42f
    private const val HIDDEN_STAGE_FOG_ALPHA = 0.34f

    fun build(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): TileRenderModel {
        val cellByPoint = snapshot.mapCells.associateBy { cell -> point(cell.x, cell.y) }
        val actorById = snapshot.actors.associateBy(ActorRenderSnapshot::entityId)
        val propByPoint = snapshot.props.associateBy { prop -> point(prop.x, prop.y) }
        val overlaysByPoint = linkedMapOf<com.ktome.core.map.Point, MutableList<OverlayRenderSnapshot>>()
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
        val mapCellMaterials = buildMapCellMaterials(snapshot.mapCells)
        val fogTiles =
            snapshot.mapCells.mapNotNull { cell ->
                when (cell.visibility) {
                    CellVisibilitySnapshot.VISIBLE -> null
                    CellVisibilitySnapshot.EXPLORED -> TileFogPlacement(x = cell.x, y = cell.y, alpha = EXPLORED_FOG_ALPHA, visibility = cell.visibility)
                    CellVisibilitySnapshot.HIDDEN -> TileFogPlacement(x = cell.x, y = cell.y, alpha = HIDDEN_STAGE_FOG_ALPHA, visibility = cell.visibility)
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
                    overlaysByPoint.getOrPut(point(cell.x, cell.y)) { mutableListOf() } += overlay
                    TileVisualPlacement(
                        x = cell.x,
                        y = cell.y,
                        asset = asset,
                        alpha = TelegraphStyle.overlayAlpha(overlay.dangerLevel),
                        drawPriority = overlay.dangerLevel,
                    )
                }
            }
        val overlayCells = overlaysByPoint.keys.map { point -> point.x to point.y }.toSet()
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

        val chromeAssets = TileChromeAssets.resolve(visualResolver)
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

        val talentAssignPanelModel =
            if (overlayState.mode == UiMode.TALENT_ASSIGN) {
                TalentSidebarPresenter.presentPanel(localizer, snapshot.uiState, overlayState)
            } else {
                null
            }
        val sidebar =
            buildSidebar(
                localizer,
                visualResolver,
                snapshot,
                overlayState,
                player,
                actorById,
                cellByPoint,
                propByPoint,
                overlaysByPoint,
                playerCell,
                combatPanel,
                talentAssignPanelModel,
            )
        val shell = buildShell(localizer, visualResolver, snapshot, overlayState, hud, sidebar, messageLines)
        val inventoryWorkbench = buildInventoryWorkbench(localizer, visualResolver, snapshot, overlayState)
        val frontstageSurface = buildFrontstageSurface(localizer, visualResolver, snapshot, overlayState)
        return TileRenderModel(
            terrainTiles = terrainTiles,
            mapCellMaterials = mapCellMaterials,
            propTiles = propTiles,
            overlayTiles = overlayTiles,
            groundLootMarkers = groundLootMarkers,
            actorTiles = actorTiles,
            fogTiles = fogTiles,
            targetCursorState = combatDecisionTargetCursorState(snapshot, overlayState),
            targetHighlights = combatDecisionTargetHighlights(snapshot, overlayState),
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
                    overlaysByPoint = overlaysByPoint,
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
            talentAssignPanel = talentAssignPanelModel?.let { panel -> TileTalentAssignPanelRenderModel.from(panel, visualResolver) },
            inventoryWorkbench = inventoryWorkbench,
            shell = shell,
            frontstageSurface = frontstageSurface,
            panelTooltip = panelTooltip(localizer, snapshot, overlayState, shell),
            playerTile = point(player.x, player.y),
            mapDimensions = TileMapDimensions(snapshot.metadata.width, snapshot.metadata.height),
            chromeAssets = chromeAssets,
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
        val statusIcons =
            StatusHudPresenter.present(
                localizer = localizer,
                icons = StatusIconResolver.resolveIcons(visualResolver, player.statusEffects),
            )
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
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        hud: TileHudModel,
        sidebar: TileSidebarModel,
        messageLines: List<TileMessageLine>,
    ): TileShellModel {
        val status = snapshot.uiState.playerStatus
        val zoneDescription =
            snapshot.metadata.zoneDescKey
                ?.let(localizer::text)
                ?: localizer.text("ui.shell.quest.none")
        val questSummary = questSummaryRow(localizer, visualResolver, snapshot)
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
                            questSummary,
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
                    TileTextRow(localizer.text("ui.controls.map.edit_loadout"), TileTextTone.LIGHT_GRAY),
                    TileTextRow(localizer.text("ui.controls.map.use_talent"), TileTextTone.LIGHT_GRAY),
                    TileTextRow(localizer.text("ui.controls.map.use_inscription"), TileTextTone.LIGHT_GRAY),
                ),
            demo = buildDemoShellModel(localizer, visualResolver, snapshot, overlayState, hud),
        )
    }

    private fun buildDemoShellModel(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        hud: TileHudModel,
    ): TileDemoShellModel {
        val status = snapshot.uiState.playerStatus
        val operationHints = demoOperationHints(localizer, overlayState)
        val effectiveInventorySelection =
            if (overlayState.mode == UiMode.INVENTORY) {
                overlayState.inventorySelection
            } else {
                overlayState.hoveredInventoryIndex ?: overlayState.inventorySelection
            }
        val equipmentInventory =
            EquipmentInventoryPresenter.present(
                EquipmentInventoryPresenterRequest(
                    localizer = localizer,
                    visualResolver = visualResolver,
                    equipment = snapshot.uiState.equipment,
                    inventory = snapshot.uiState.inventory,
                    inventorySelection = effectiveInventorySelection,
                    selectedEquipmentSlotId = overlayState.hoveredEquipmentSlotId,
                ),
            )
        val operationRows = demoOperationRows(localizer, visualResolver, snapshot, overlayState, operationHints)
        return TileDemoShellModel(
            navItems =
                listOf(
                    TileDemoNavItemModel(TileDemoNavItemKind.COMPASS, localizer.text("ui.sidebar.map"), navItemState(TileDemoNavItemKind.COMPASS, overlayState)),
                    TileDemoNavItemModel(TileDemoNavItemKind.BAG, localizer.text("ui.sidebar.inventory"), navItemState(TileDemoNavItemKind.BAG, overlayState)),
                    TileDemoNavItemModel(TileDemoNavItemKind.SCROLL, localizer.text("ui.sidebar.recent_rewards"), navItemState(TileDemoNavItemKind.SCROLL, overlayState)),
                    TileDemoNavItemModel(TileDemoNavItemKind.BOOK, localizer.text("ui.sidebar.improve_talents"), navItemState(TileDemoNavItemKind.BOOK, overlayState)),
                    TileDemoNavItemModel(TileDemoNavItemKind.GEAR, localizer.text("ui.menu.action.validation"), navItemState(TileDemoNavItemKind.GEAR, overlayState)),
                ),
            rightEquipmentTitle = localizer.text("ui.sidebar.equipment"),
            rightInscriptionsTitle = localizer.text("ui.sidebar.inscriptions"),
            rightBackpackTitle = localizer.text("ui.sidebar.inventory"),
            rightOperationHintsTitle =
                when {
                    overlayState.mode == UiMode.VALIDATION -> localizer.text("ui.sidebar.validation")
                    snapshot.uiState.activeShop != null && overlayState.mode == UiMode.SHOP -> localizer.text("ui.sidebar.shop")
                    else -> localizer.text("ui.shell.operation_hints")
                },
            equipmentSlots = equipmentInventory.equipmentSlots.map(::equipmentSlot),
            inscriptionSlots = inscriptionSlotModels(localizer, visualResolver, snapshot, overlayState),
            backpackSlots = equipmentInventory.inventoryGrid.cells.map(::inventorySlot),
            backpackPageLabel = equipmentInventory.inventoryGrid.pageLabel,
            operationHints = operationHints,
            operationRows = operationRows,
            equipmentInventory = equipmentInventory,
            heroSummaryLines =
                listOf(
                    hud.floorText,
                    hud.hpGauge.summary,
                    hud.resourceGauge.summary,
                    "${localizer.text("ui.hud.attack.short")} ${status.attack}",
                    "${localizer.text("ui.hud.defense.short")} ${status.defense}",
                ),
            heroLevelText = "${localizer.text("ui.hud.level.short")} ${status.level}",
        )
    }

    private fun buildInventoryWorkbench(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): InventoryWorkbenchPresentation? {
        if (overlayState.mode != UiMode.INVENTORY || overlayState.activeModalKind != ModalFrameKind.INVENTORY) {
            return null
        }
        return InventoryWorkbenchPresenter.present(
            InventoryWorkbenchPresenterRequest(
                localizer = localizer,
                visualResolver = visualResolver,
                equipment = snapshot.uiState.equipment,
                inventory = snapshot.uiState.inventory,
                selectedEntryId = overlayState.inventorySelection.takeIf { selection -> selection >= 0 },
                focusedCell = overlayState.inventoryFocusedCell,
                hoveredCell = overlayState.hoveredInventoryCell,
                pageIndex = overlayState.inventoryPageIndex,
            ),
        )
    }

    private fun buildMapCellMaterials(cells: List<MapCellSnapshot>): List<TileMapCellMaterialModel> {
        val cellByPoint = cells.associateBy { cell -> point(cell.x, cell.y) }
        return cells
            .filter { cell -> cell.visibility != CellVisibilitySnapshot.HIDDEN }
            .map { cell ->
                val kind = cell.materialKind()
                TileMapCellMaterialModel(
                    x = cell.x,
                    y = cell.y,
                    kind = kind,
                    visibility = cell.visibility,
                    variant = materialVariant(cell.x, cell.y, cell.terrainVisualKey),
                    northOcclusion = kind == TileMapCellMaterialKind.FLOOR && cellByPoint[point(cell.x, cell.y + 1)].isOccludingMaterial(),
                    southOcclusion = kind == TileMapCellMaterialKind.FLOOR && cellByPoint[point(cell.x, cell.y - 1)].isOccludingMaterial(),
                    westOcclusion = kind == TileMapCellMaterialKind.FLOOR && cellByPoint[point(cell.x - 1, cell.y)].isOccludingMaterial(),
                    eastOcclusion = kind == TileMapCellMaterialKind.FLOOR && cellByPoint[point(cell.x + 1, cell.y)].isOccludingMaterial(),
                )
            }
    }

    private fun MapCellSnapshot?.isOccludingMaterial(): Boolean =
        this == null ||
            visibility == CellVisibilitySnapshot.HIDDEN ||
            materialKind() == TileMapCellMaterialKind.WALL

    private fun MapCellSnapshot.materialKind(): TileMapCellMaterialKind =
        when {
            terrainTypeId.contains("wall", ignoreCase = true) ||
                terrainVisualKey.contains("wall", ignoreCase = true) ->
                TileMapCellMaterialKind.WALL
            terrainTypeId.contains("lava", ignoreCase = true) ||
                terrainTypeId.contains("water", ignoreCase = true) ||
                terrainTypeId.contains("acid", ignoreCase = true) ||
                terrainVisualKey.contains("lava", ignoreCase = true) ||
                terrainVisualKey.contains("water", ignoreCase = true) ->
                TileMapCellMaterialKind.HAZARD
            else -> TileMapCellMaterialKind.FLOOR
        }

    private fun materialVariant(
        x: Int,
        y: Int,
        key: String,
    ): Int {
        var hash = 17
        hash = hash * 31 + x
        hash = hash * 31 + y
        key.forEach { char -> hash = hash * 31 + char.code }
        return hash and Int.MAX_VALUE
    }

    private fun buildFrontstageSurface(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): TileFrontstageSurfaceModel? {
        val shop = snapshot.uiState.activeShop
        if (shop != null && overlayState.mode == UiMode.SHOP) {
            return shop.inscriptionReplacementPrompt?.let { prompt ->
                buildShopReplacementFrontstage(localizer, visualResolver, snapshot, overlayState, shop, prompt)
            } ?: buildShopFrontstage(localizer, visualResolver, snapshot, overlayState, shop)
        }
        val routeSelection = snapshot.uiState.activeRouteSelection
        if (routeSelection != null && overlayState.mode == UiMode.WORLD_MAP) {
            return buildRouteFrontstage(localizer, visualResolver, routeSelection, overlayState)
        }
        if (overlayState.mode == UiMode.STAT_ASSIGN && snapshot.uiState.playerStatus.statPoints > 0) {
            return buildStatAssignFrontstage(localizer, snapshot)
        }
        if (
            snapshot.uiState.recentRewards.isNotEmpty() &&
            (overlayState.mode == UiMode.VALIDATION || overlayState.paneFocusAnchor == PaneFocusAnchor.CONTEXT)
        ) {
            return buildRewardFrontstage(localizer, visualResolver, snapshot)
        }
        return null
    }

    private fun buildShopFrontstage(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        shop: ShopPanelSnapshot,
    ): TileFrontstageSurfaceModel {
        val buyCards =
            shop.offers.take(5).map { offer ->
                val card = ModalCardModel.shopOffer(shop.shopId, offer, shardBalance = snapshot.uiState.shardBalance)
                TileFrontstageCardModel(
                    title = renderTextToken(localizer, card.title),
                    tone = if (overlayState.shopFocus == ShopFocus.BUY && overlayState.shopOfferSelection == offer.index) TileTextTone.CYAN else TileTextTone.WHITE,
                    summary = card.costLines.firstOrNull()?.let { token -> renderTextToken(localizer, token) },
                    detailRows =
                        card.detailLines.take(2).map { token ->
                            TileTextRow(renderTextToken(localizer, token), TileTextTone.LIGHT_GRAY)
                        },
                    icon = card.iconKey?.let { key -> resolveVisual(visualResolver, key) },
                    stateIcon = card.stateIconKey?.let { key -> resolveVisual(visualResolver, key) },
                    typeIcon = card.typeIconKey?.let { key -> resolveVisual(visualResolver, key) },
                    frame = card.frameKey?.let { key -> resolveVisual(visualResolver, key) },
                    selected = overlayState.shopFocus == ShopFocus.BUY && overlayState.shopOfferSelection == offer.index,
                )
            }
        val sellCards =
            shop.sellEntries.take(5).mapIndexed { displayIndex, sellEntry ->
                val inventoryEntry = snapshot.uiState.inventory.firstOrNull { entry -> entry.index == sellEntry.inventoryIndex }
                val card =
                    ModalCardModel.shopSellEntry(
                        shopId = shop.shopId,
                        displayIndex = displayIndex,
                        item = inventoryEntry?.item,
                        price = sellEntry.price,
                    )
                TileFrontstageCardModel(
                    title = renderTextToken(localizer, card.title),
                    tone = if (overlayState.shopFocus == ShopFocus.SELL && overlayState.inventorySelection == displayIndex) TileTextTone.CYAN else TileTextTone.WHITE,
                    summary = card.rewardLines.firstOrNull()?.let { token -> renderTextToken(localizer, token) },
                    detailRows =
                        card.detailLines.take(1).map { token ->
                            TileTextRow(renderTextToken(localizer, token), TileTextTone.LIGHT_GRAY)
                        },
                    icon = card.iconKey?.let { key -> resolveVisual(visualResolver, key) },
                    selected = overlayState.shopFocus == ShopFocus.SELL && overlayState.inventorySelection == displayIndex,
                    disabled = inventoryEntry == null,
                )
            }
        val detailCards =
            selectedShopDescriptionRows(localizer, shop, snapshot, overlayState)
                .take(5)
                .mapIndexed { index, line ->
                    TileFrontstageCardModel(
                        title = if (index == 0) line.text else " ",
                        tone = descriptionTone(line),
                        detailRows = if (index == 0) emptyList() else listOf(TileTextRow(line.text, descriptionTone(line))),
                    )
                }
        return TileFrontstageSurfaceModel(
            kind = TileFrontstageSurfaceKind.SHOP,
            title = localizer.text(shop.shopNameKey),
            eyebrow = localizer.text("ui.sidebar.shards", "value" to snapshot.uiState.shardBalance),
            summary = shop.hintLabelKeys.joinToString("  ") { key -> localizer.text(key) }.ifBlank { null },
            columns =
                listOf(
                    TileFrontstageColumnModel(localizer.text("ui.shop.buy"), buyCards),
                    TileFrontstageColumnModel(localizer.text("ui.shop.sell"), sellCards),
                    TileFrontstageColumnModel(localizer.text("ui.frontstage.shop.detail"), detailCards),
                ),
            footerRows = listOf(TileTextRow(localizer.text("ui.controls.shop"), TileTextTone.LIGHT_GRAY)),
        )
    }

    private fun buildShopReplacementFrontstage(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        shop: ShopPanelSnapshot,
        prompt: InscriptionReplacementPromptSnapshot,
    ): TileFrontstageSurfaceModel {
        val candidateCard =
            TileFrontstageCardModel(
                title = localizer.text(prompt.candidate.nameKey),
                tone = TileTextTone.GOLD,
                summary =
                    listOf(
                        localizer.text(prompt.candidate.categoryLabelKey.ifBlank { "ui.sidebar.inscriptions" }),
                        localizer.text("ui.modal.card.cost.shards", "amount" to prompt.price),
                    ).joinToString("  "),
                detailRows =
                    listOfNotNull(
                        prompt.candidate.descKey.takeIf(String::isNotBlank)?.let { key ->
                            TileTextRow(localizer.text(key), TileTextTone.LIGHT_GRAY)
                        },
                    ) +
                        prompt.candidate.effectTagLabelKeys.take(2).map { key ->
                            TileTextRow(localizer.text(key), TileTextTone.CYAN)
                        },
                icon = resolveVisual(visualResolver, prompt.candidate.iconKey),
                selected = true,
            )
        val slotCards =
            prompt.currentSlots.map { slot ->
                val selected = slot.hotkey == overlayState.inscriptionReplacementHotkeySelection
                TileFrontstageCardModel(
                    title = listOfNotNull(slot.hotkey?.toString(), localizer.text(slot.nameKey)).joinToString(". "),
                    tone = if (selected) TileTextTone.CYAN else TileTextTone.WHITE,
                    summary = localizer.text(slot.categoryLabelKey.ifBlank { "ui.sidebar.inscriptions" }),
                    detailRows = slot.effectTagLabelKeys.take(2).map { key -> TileTextRow(localizer.text(key), TileTextTone.LIGHT_GRAY) },
                    icon = resolveVisual(visualResolver, slot.iconKey),
                    selected = selected,
                )
            }
        val routingCards =
            prompt.categoryChanges.map { change ->
                TileFrontstageCardModel(
                    title = localizer.text(change.categoryLabelKey.ifBlank { "ui.sidebar.inscriptions" }),
                    tone = if (change.afterCount > change.limit) TileTextTone.RED else TileTextTone.GOLD,
                    summary = "${change.beforeCount} -> ${change.afterCount} / ${change.limit}",
                    detailRows =
                        listOfNotNull(
                            change.targetHotkey?.let { hotkey ->
                                TileTextRow(localizer.text("ui.frontstage.shop.replace_hotkey", "hotkey" to hotkey), TileTextTone.LIGHT_GRAY)
                            },
                        ),
                )
            } +
                listOfNotNull(
                    prompt.rejectedReasonKey?.let { key ->
                        TileFrontstageCardModel(
                            title = localizer.text(key),
                            tone = TileTextTone.RED,
                        )
                    },
                )
        return TileFrontstageSurfaceModel(
            kind = TileFrontstageSurfaceKind.SHOP_REPLACEMENT,
            title = localizer.text("ui.frontstage.shop.replacement_title"),
            eyebrow = localizer.text(shop.shopNameKey),
            summary = localizer.text("ui.frontstage.shop.replacement_summary"),
            columns =
                listOf(
                    TileFrontstageColumnModel(localizer.text("ui.frontstage.shop.candidate"), listOf(candidateCard)),
                    TileFrontstageColumnModel(localizer.text("ui.frontstage.shop.current_slots"), slotCards),
                    TileFrontstageColumnModel(localizer.text("ui.frontstage.shop.routing"), routingCards),
                ),
            footerRows = listOf(TileTextRow(localizer.text("ui.controls.shop"), TileTextTone.LIGHT_GRAY)),
        )
    }

    private fun buildRouteFrontstage(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        routeSelection: RouteSelectionSnapshot,
        overlayState: OverlayState,
    ): TileFrontstageSurfaceModel {
        val routeCards =
            routeSelection.options.map { option ->
                val card = RoutePreviewText.modalCardModel(option)
                TileFrontstageCardModel(
                    title = renderTextToken(localizer, card.title),
                    tone = if (overlayState.routeSelection == option.index) TileTextTone.CYAN else TileTextTone.WHITE,
                    summary = card.summary?.let { token -> renderTextToken(localizer, token) },
                    detailRows =
                        (
                            card.detailLines.take(2).map { token -> TileTextRow(renderTextToken(localizer, token), TileTextTone.LIGHT_GRAY) } +
                                card.rewardLines.take(2).map { token -> TileTextRow(renderTextToken(localizer, token), modalCardRewardTone(token)) } +
                                listOfNotNull(RoutePreviewText.traitLine(localizer, option)?.let { traits -> TileTextRow(traits, TileTextTone.BLUE) })
                        ).take(4),
                    icon = resolveVisual(visualResolver, DarkUiChromeVisualKeys.SHELL_NAV_COMPASS),
                    selected = overlayState.routeSelection == option.index,
                )
            }
        return TileFrontstageSurfaceModel(
            kind = TileFrontstageSurfaceKind.ROUTE_SELECTION,
            title = localizer.text("ui.frontstage.route.title"),
            eyebrow = localizer.text("ui.world_map.current_zone", "zone" to localizer.text(routeSelection.currentZoneNameKey)),
            summary = localizer.text("ui.frontstage.route.summary"),
            columns = listOf(TileFrontstageColumnModel(localizer.text("ui.frontstage.route.options"), routeCards)),
            footerRows = listOf(TileTextRow(localizer.text("ui.controls.world_map"), TileTextTone.LIGHT_GRAY)),
        )
    }

    private fun buildStatAssignFrontstage(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): TileFrontstageSurfaceModel {
        val status = snapshot.uiState.playerStatus
        val cards =
            listOf(
                TileFrontstageCardModel(
                    title = "1. ${localizer.text("ui.stat.str")}",
                    tone = TileTextTone.GOLD,
                    summary = localizer.text("ui.frontstage.stat.str_summary"),
                    detailRows = listOf(TileTextRow("${localizer.text("ui.hud.attack.short")} ${status.attack}", TileTextTone.WHITE)),
                    selected = true,
                ),
                TileFrontstageCardModel(
                    title = "2. ${localizer.text("ui.stat.dex")}",
                    tone = TileTextTone.WHITE,
                    summary = localizer.text("ui.frontstage.stat.dex_summary"),
                    detailRows =
                        listOf(
                            TileTextRow("${localizer.text("ui.hud.accuracy.short")} ${status.accuracy}", TileTextTone.LIGHT_GRAY),
                            TileTextRow("${localizer.text("ui.hud.evasion.short")} ${status.evasion}", TileTextTone.LIGHT_GRAY),
                        ),
                ),
                TileFrontstageCardModel(
                    title = "3. ${localizer.text("ui.stat.con")}",
                    tone = TileTextTone.WHITE,
                    summary = localizer.text("ui.frontstage.stat.con_summary"),
                    detailRows =
                        listOf(
                            TileTextRow("${localizer.text("ui.hud.hp.short")} ${status.currentHp}/${status.maxHp}", TileTextTone.LIGHT_GRAY),
                            TileTextRow("${localizer.text("ui.hud.defense.short")} ${status.defense}", TileTextTone.LIGHT_GRAY),
                        ),
                ),
                TileFrontstageCardModel(
                    title = "4. ${localizer.text("ui.stat.wil")}",
                    tone = TileTextTone.WHITE,
                    summary = localizer.text("ui.frontstage.stat.wil_summary"),
                    detailRows =
                        listOf(
                            TileTextRow("${localizer.text(status.resourceLabelKey)} ${status.currentResource}/${status.maxResource}", TileTextTone.LIGHT_GRAY),
                            TileTextRow("${localizer.text("ui.hud.speed.short")} ${status.speed}", TileTextTone.LIGHT_GRAY),
                        ),
                ),
            )
        return TileFrontstageSurfaceModel(
            kind = TileFrontstageSurfaceKind.STAT_ASSIGN,
            title = localizer.text("ui.sidebar.assign_stats"),
            eyebrow = localizer.text("ui.sidebar.points", "value" to status.statPoints),
            summary = localizer.text("ui.frontstage.stat.summary"),
            columns = listOf(TileFrontstageColumnModel(localizer.text("ui.frontstage.stat.options"), cards)),
            footerRows = listOf(TileTextRow(localizer.text("ui.controls.stat_assign"), TileTextTone.LIGHT_GRAY)),
        )
    }

    private fun buildRewardFrontstage(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
    ): TileFrontstageSurfaceModel {
        val rewardCards =
            snapshot.uiState.recentRewards.asReversed().take(4).mapIndexed { index, entry ->
                val card = ModalCardModel.rewardPresentation(index, entry)
                TileFrontstageCardModel(
                    title = renderTextToken(localizer, card.title),
                    tone = rewardPresentationTone(entry.source),
                    summary = card.summary?.let { token -> renderTextToken(localizer, token) },
                    detailRows =
                        card.detailLines.take(4).map { token ->
                            TileTextRow(renderTextToken(localizer, token), TileTextTone.LIGHT_GRAY)
                        },
                    icon = card.iconKey?.let { key -> resolveVisual(visualResolver, key) },
                    selected = index == 0,
                )
            }
        return TileFrontstageSurfaceModel(
            kind = TileFrontstageSurfaceKind.REWARD,
            title = localizer.text("ui.frontstage.reward.title"),
            eyebrow = localizer.text("ui.sidebar.recent_rewards"),
            summary = localizer.text("ui.frontstage.reward.summary"),
            columns = listOf(TileFrontstageColumnModel(localizer.text("ui.frontstage.reward.claimed"), rewardCards)),
            footerRows = listOf(TileTextRow(localizer.text("ui.controls.map.inventory"), TileTextTone.LIGHT_GRAY)),
        )
    }

    private fun demoOperationRows(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        fallbackHints: List<String>,
    ): List<TileTextRow> {
        if (overlayState.mode == UiMode.VALIDATION) {
            return overlayState.validationPanel?.let { panel ->
                ValidationOverlaySummaryPresenter.present(
                    localizer = localizer,
                    panel = panel,
                    displayMode = ValidationOverlayDisplayMode.COMPACT,
                    visibleOverlayRows = 28,
                )
            } ?: fallbackHints.map { hint -> TileTextRow(hint, TileTextTone.LIGHT_GRAY) }
        }
        val shop = snapshot.uiState.activeShop
        if (shop == null || overlayState.mode != UiMode.SHOP) {
            return fallbackHints.map { hint -> TileTextRow(hint, TileTextTone.LIGHT_GRAY) }
        }
        val prompt = shop.inscriptionReplacementPrompt
        if (prompt != null) {
            val promptRows =
                inscriptionReplacementPromptLines(
                    localizer = localizer,
                    prompt = prompt,
                    selectedHotkey = overlayState.inscriptionReplacementHotkeySelection,
                ).map { line ->
                    inscriptionReplacementPromptTileRow(visualResolver, line)
                }
            val title = promptRows.firstOrNull()
            val slotRows = promptRows.filter { row -> row.kind == TileTextRowKind.SHOP_REPLACEMENT_SLOT }
            val supportRows = promptRows.drop(1).filter { row -> row.icon == null }
            return listOfNotNull(title) + slotRows.take(MAX_REPLACEMENT_SLOT_HINT_ROWS) + supportRows.take(2)
        }
        val selectedOffer = shop.offers.getOrNull(overlayState.shopOfferSelection) ?: shop.offers.firstOrNull()
        val selectedOfferRow =
            selectedOffer?.let { offer ->
                modalCardHeaderRow(
                    localizer = localizer,
                    visualResolver = visualResolver,
                    card = ModalCardModel.shopOffer(shop.shopId, offer, shardBalance = snapshot.uiState.shardBalance),
                    prefix = "${offer.index + 1}.",
                    tone = TileTextTone.CYAN,
                    selected = true,
                )
            }
        val nextOfferRow =
            shop.offers
                .firstOrNull { offer -> selectedOffer == null || offer.index != selectedOffer.index }
                ?.let { offer ->
                    modalCardHeaderRow(
                        localizer = localizer,
                        visualResolver = visualResolver,
                        card = ModalCardModel.shopOffer(shop.shopId, offer, shardBalance = snapshot.uiState.shardBalance),
                        prefix = "${offer.index + 1}.",
                        tone = TileTextTone.WHITE,
                    )
                }
        return listOf(TileTextRow(localizer.text(shop.shopNameKey), TileTextTone.GOLD)) +
            listOfNotNull(selectedOfferRow, nextOfferRow) +
            listOf(TileTextRow(localizer.text("ui.controls.shop"), TileTextTone.LIGHT_GRAY))
    }

    private fun navItemState(
        kind: TileDemoNavItemKind,
        overlayState: OverlayState,
    ): TileDemoNavItemState {
        val selected =
            when (kind) {
                TileDemoNavItemKind.COMPASS ->
                    overlayState.mode in
                        setOf(
                            UiMode.MAP,
                            UiMode.WORLD_MAP,
                            UiMode.SHOP,
                            UiMode.TARGETING,
                            UiMode.INSPECT,
                            UiMode.STAT_ASSIGN,
                        ) &&
                        overlayState.paneFocusAnchor == PaneFocusAnchor.WORLD

                TileDemoNavItemKind.BAG -> overlayState.mode == UiMode.INVENTORY
                TileDemoNavItemKind.SCROLL -> overlayState.mode == UiMode.MAP && overlayState.paneFocusAnchor == PaneFocusAnchor.CONTEXT
                TileDemoNavItemKind.BOOK -> overlayState.mode in setOf(UiMode.LOADOUT_EDIT, UiMode.TALENT_ASSIGN)
                TileDemoNavItemKind.GEAR -> overlayState.mode == UiMode.VALIDATION
            }
        return if (selected) TileDemoNavItemState.SELECTED else TileDemoNavItemState.IDLE
    }

    private fun equipmentSummaryRows(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): List<String> {
        val bySlot = snapshot.uiState.equipment.associateBy { slot -> slot.slotId }
        return listOf("WEAPON", "OFF_HAND", "ARMOR").map { slotId ->
            val itemName = bySlot[slotId]?.item?.let { item -> renderItemDisplay(localizer, item) } ?: "-"
            "${equipmentSlotLabel(localizer, slotId)}: $itemName"
        }
    }

    private fun inscriptionSlotModels(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): List<TileDemoSlotModel> {
        val byHotkey = snapshot.uiState.inscriptions.associateBy { inscription -> inscription.hotkey }
        return (5..12).map { hotkey ->
            val inscription = byHotkey[hotkey]
            val selected = overlayState.hoveredInscriptionHotkey == hotkey
            when {
                inscription != null ->
                    TileDemoSlotModel(
                        label = hotkey.toString(),
                        detail = localizer.text(inscription.nameKey),
                        icon = resolveVisual(visualResolver, inscription.iconKey),
                        state = TileDemoSlotState.FILLED,
                        stableId = "inscription:$hotkey",
                        selected = selected,
                        tooltipAnchorId = "inscription:$hotkey",
                    )

                hotkey <= 8 ->
                    TileDemoSlotModel(
                        label = hotkey.toString(),
                        detail = localizer.text("ui.shell.inscription.empty"),
                        icon = null,
                        state = TileDemoSlotState.EMPTY,
                        stableId = "inscription:$hotkey",
                        selected = selected,
                        tooltipAnchorId = "inscription:$hotkey",
                    )

                else ->
                    TileDemoSlotModel(
                        label = hotkey.toString(),
                        detail = null,
                        icon = null,
                        state = TileDemoSlotState.EMPTY,
                        stableId = "inscription:$hotkey",
                        selected = selected,
                        tooltipAnchorId = "inscription:$hotkey",
                    )
            }
        }
    }

    private fun equipmentSlot(slot: EquipmentSlotCellModel): TileDemoSlotModel =
        TileDemoSlotModel(
            label = if (slot.visualOnly) "" else slot.label,
            detail = slot.label.takeIf { label -> label.isNotBlank() },
            icon = slot.itemIcon,
            state = if (slot.itemIcon == null) TileDemoSlotState.EMPTY else TileDemoSlotState.FILLED,
            stableId = slot.slotId,
            frame = slot.frame,
            selected = slot.selected,
            qualityTierId = slot.qualityTierId,
            tooltipAnchorId = slot.tooltipAnchorId,
            visualOnly = slot.visualOnly,
        )

    private fun inventorySlot(slot: InventoryGridCellModel): TileDemoSlotModel =
        TileDemoSlotModel(
            label = slot.identityIndex?.plus(1)?.toString().orEmpty(),
            detail = null,
            icon = slot.itemIcon,
            state = if (slot.itemIcon == null) TileDemoSlotState.EMPTY else TileDemoSlotState.FILLED,
            stableId = slot.identityIndex?.let { index -> "inventory:$index" },
            frame = slot.frame,
            selected = slot.selected,
            qualityTierId = slot.qualityTierId,
            quantityText = slot.quantityText,
            tooltipAnchorId = slot.tooltipAnchorId,
            showBadge = slot.identityIndex != null,
        )

    private fun questSummaryRow(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
    ): TileTextRow {
        val objectiveToken =
            snapshot.logEvents
            .asReversed()
            .map(RenderLogEventSnapshot::message)
            .firstOrNull { token -> token.key.startsWith("log.objective.") }
        return TileTextRow(
            text = objectiveToken?.let { token -> renderTextToken(localizer, token) } ?: localizer.text("ui.shell.quest.none"),
            tone = questSummaryTone(objectiveToken),
            icon = QuestSummaryIconResolver.resolve(visualResolver, objectiveToken),
        )
    }

    private fun questSummaryTone(token: RenderTextTokenSnapshot?): TileTextTone =
        when (token?.key) {
            "log.objective.activate" -> TileTextTone.GOLD
            "log.objective.complete" -> TileTextTone.GOLD
            "log.objective.advance" -> TileTextTone.WHITE
            else -> TileTextTone.LIGHT_GRAY
        }

    private fun buildTargetCardModel(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        hud: TileHudModel,
        actorById: Map<Int, ActorRenderSnapshot>,
        cellByPoint: Map<com.ktome.core.map.Point, MapCellSnapshot>,
        propByPoint: Map<com.ktome.core.map.Point, PropRenderSnapshot>,
        overlaysByPoint: Map<com.ktome.core.map.Point, List<OverlayRenderSnapshot>>,
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
        val overlaysAtFocus = overlaysByPoint[focusPoint].orEmpty()
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

    private fun demoOperationHints(
        localizer: Localizer,
        overlayState: OverlayState,
    ): List<String> =
        when (overlayState.mode) {
            UiMode.INVENTORY ->
                when (overlayState.activeModalKind) {
                    ModalFrameKind.ITEM_DETAIL -> listOf(localizer.text("ui.controls.item_detail"))
                    ModalFrameKind.ITEM_COMPARE -> listOf(localizer.text("ui.controls.deferred_modal"))
                    else ->
                        listOf(
                            localizer.text("ui.controls.inventory.close_hint"),
                            localizer.text("ui.controls.inventory"),
                        )
                }

            UiMode.SHOP ->
                listOf(
                    localizer.text("ui.controls.shop.close_hint"),
                    localizer.text("ui.controls.shop"),
                )

            else ->
                listOf(
                    localizer.text("ui.controls.map.inventory"),
                    localizer.text("ui.controls.map.pick_up"),
                    localizer.text("ui.controls.map.save"),
                    localizer.text("ui.controls.map.edit_loadout"),
                    localizer.text("ui.controls.map.use_talent"),
                    localizer.text("ui.controls.map.use_inscription"),
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
        overlaysByPoint: Map<com.ktome.core.map.Point, List<OverlayRenderSnapshot>>,
        playerCell: MapCellSnapshot,
        combatPanel: CombatDecisionPanelModel?,
        talentAssignPanel: TalentAssignPanelModel?,
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
                            icon = item?.let { resolveItemIconVisual(visualResolver, it) },
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
                                icon = resolveItemIconVisual(visualResolver, item),
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
                            ).map { line ->
                                inscriptionReplacementPromptTileRow(visualResolver, line)
                            }
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
                            val card = ModalCardModel.shopOffer(shop.shopId, offer, shardBalance = snapshot.uiState.shardBalance)
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
                    val selectedItem = selectedInventoryItem(snapshot, overlayState)
                    if (selectedItem == null) {
                        rows += TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)
                    } else {
                        val presentation = QualityPresentation.from(selectedItem)
                        rows +=
                            TileTextRow(
                                text = renderItemDisplay(localizer, selectedItem, presentation),
                                tone = itemTone(presentation),
                                icon = resolveItemIconVisual(visualResolver, selectedItem),
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
                val selectedItem = selectedInventoryItem(snapshot, overlayState)
                selectedItem?.let { item ->
                    rows += inventoryItemHeaderRow(localizer, visualResolver, item)
                    DescriptionPresenter.presentInventoryItemLines(localizer, item).forEach { line ->
                        rows += TileTextRow(line.text, descriptionTone(line))
                    }
                    itemDetailLines(localizer, item).forEach { detail ->
                        rows += TileTextRow(detail, TileTextTone.LIGHT_GRAY)
                    }
                    equipmentComparisonLines(localizer, snapshot, item).take(3).forEach { line ->
                        rows += TileTextRow(line.text, line.tone)
                    }
                }
                snapshot.uiState.inventory.forEach { entry ->
                    val presentation = QualityPresentation.from(entry.item)
                    val label = "${entry.index + 1}. ${renderItemDisplay(localizer, entry.item, presentation)}"
                    val equipped = entry.equippedSlotId?.let { slotId -> " [${equipmentSlotLabel(localizer, slotId)}]" }.orEmpty()
                    rows +=
                        TileTextRow(
                            text = label + equipped,
                            tone = if (entry.index == overlayState.inventorySelection) TileTextTone.CYAN else itemTone(presentation),
                            icon = resolveItemIconVisual(visualResolver, entry.item),
                            selected = entry.index == overlayState.inventorySelection,
                        )
                }
                if (snapshot.uiState.inventory.isEmpty()) {
                    rows += emptyStateRows(localizer, visualResolver, UiEmptyState.inventory())
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
                val overlaysAtCursor = overlaysByPoint[cursor].orEmpty()
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
                                icon = resolveItemIconVisual(visualResolver, item),
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
                val panel = talentAssignPanel ?: TalentSidebarPresenter.presentPanel(localizer, snapshot.uiState, overlayState)
                TalentSidebarPresenter.presentFromPanel(panel).forEach { line ->
                    rows +=
                        TileTextRow(
                            text = line.text,
                            tone = talentSidebarTone(line),
                            icon = line.iconKey?.let(visualResolver::resolve),
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
        val iconKeys = listOfNotNull(card.iconKey, card.stateIconKey, card.typeIconKey).distinct()
        return TileTextRow(
            text = "$prefix $title$detailSuffix$summarySuffix$disabledSuffix",
            tone = tone,
            icon = iconKeys.firstOrNull()?.let { iconKey -> resolveVisual(visualResolver, iconKey) },
            selected = selected,
            extraIcons = iconKeys.drop(1).map { iconKey -> resolveVisual(visualResolver, iconKey) },
            frame = card.frameKey?.let { frameKey -> resolveVisual(visualResolver, frameKey) },
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

    private fun combatDecisionTargetHighlights(
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): List<TileTargetHighlightModel> {
        if (overlayState.mode != UiMode.TARGETING || overlayState.activeModalKind != ModalFrameKind.COMBAT_DECISION) {
            return emptyList()
        }
        val localState = overlayState.modalFrames.lastOrNull()?.localState ?: return emptyList()
        val state = localState.combatDecisionState ?: return emptyList()
        if (state.phase != com.ktome.client.ui.combat.CombatDecisionPhase.TARGET) {
            return emptyList()
        }
        val cursor = overlayState.targetingCursor ?: localState.targetingCursor
        overlayState.validationCombatDecisionSurface?.let { surface ->
            if (surface == CombatDecisionValidationSurface.NO_LEGAL_TARGET) {
                return emptyList()
            }
            val legalTarget = CombatDecisionValidationFixtures.legalTargetPoint(snapshot)
            return listOfNotNull(
                TileTargetHighlightModel(legalTarget, TileTargetCursorState.LEGAL),
                cursor
                    ?.takeUnless { tile -> tile == legalTarget }
                    ?.let { tile -> TileTargetHighlightModel(tile, TileTargetCursorState.ILLEGAL) },
            )
        }
        val action = CombatDecisionFrame.selectedAction(snapshot, state)
        if (action == null) {
            return cursor
                ?.let { tile -> listOf(TileTargetHighlightModel(tile, TileTargetCursorState.ILLEGAL)) }
                .orEmpty()
        }
        if (action.usesFreeCursorTargeting()) {
            return cursor
                ?.let { tile -> listOf(TileTargetHighlightModel(tile, TileTargetCursorState.LEGAL)) }
                .orEmpty()
        }
        val legalTargets = CombatDecisionFrame.legalTargets(snapshot, action).map { target -> target.point }.distinct()
        val legalHighlights = legalTargets.map { tile -> TileTargetHighlightModel(tile, TileTargetCursorState.LEGAL) }
        val invalidCursorHighlight =
            cursor
                ?.takeUnless { tile -> tile in legalTargets }
                ?.let { tile -> TileTargetHighlightModel(tile, TileTargetCursorState.ILLEGAL) }
        return legalHighlights + listOfNotNull(invalidCursorHighlight)
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
    ): List<TileTextRow> =
        ValidationOverlaySummaryPresenter.present(localizer, panel)

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

    private fun selectedInventoryItem(
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): ItemRenderSnapshot? {
        val selectedIndex = overlayState.inventorySelection
        return snapshot.uiState.inventory.firstOrNull { entry -> entry.index == selectedIndex }?.item
    }

    private fun inventoryItemHeaderRow(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        item: ItemRenderSnapshot,
    ): TileTextRow {
        val presentation = QualityPresentation.from(item)
        return TileTextRow(
            text = renderItemDisplay(localizer, item, presentation),
            tone = itemTone(presentation),
            icon = resolveItemIconVisual(visualResolver, item),
        )
    }

    private fun panelTooltip(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        shell: TileShellModel,
    ): TilePanelTooltipModel? {
        if (overlayState.mode == UiMode.INVENTORY && overlayState.activeModalKind == ModalFrameKind.INVENTORY) {
            return null
        }
        overlayState.hoveredEquipmentSlotId?.let { slotId ->
            val anchorIndex = typedEquipmentSlotOrder.indexOf(slotId).takeIf { index -> index >= 0 } ?: return@let
            return equipmentTooltip(localizer, snapshot, slotId, anchorIndex)
        }
        overlayState.hoveredInscriptionHotkey?.let { hotkey ->
            val anchorIndex = hotkey - INSCRIPTION_HOTKEY_START
            if (anchorIndex in shell.demo.inscriptionSlots.indices) {
                return inscriptionTooltip(localizer, snapshot, hotkey, anchorIndex)
            }
        }
        val inventoryIndex =
            overlayState.hoveredInventoryIndex
                ?: overlayState.inventorySelection.takeIf {
                    overlayState.mode == UiMode.INVENTORY && overlayState.activeModalKind == ModalFrameKind.INVENTORY
                }
        inventoryIndex?.let { index ->
            val anchorIndex = shell.demo.equipmentInventory.inventoryGrid.cells.indexOfFirst { cell -> cell.identityIndex == index }
            if (anchorIndex >= 0) {
                val item = snapshot.uiState.inventory.firstOrNull { entry -> entry.index == index }?.item ?: return@let
                return itemTooltip(
                    localizer = localizer,
                    snapshot = snapshot,
                    anchorKind = TilePanelTooltipAnchorKind.BACKPACK_SLOT,
                    anchorIndex = anchorIndex,
                    anchorId = "inventory:$index",
                    item = item,
                    includeEquipmentComparison = true,
                )
            }
        }
        return null
    }

    private fun equipmentTooltip(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        slotId: String,
        anchorIndex: Int,
    ): TilePanelTooltipModel {
        val slotLabel = equipmentSlotLabel(localizer, slotId)
        val item = snapshot.uiState.equipment.firstOrNull { slot -> slot.slotId == slotId }?.item
        if (item == null) {
            return TilePanelTooltipModel(
                anchorKind = TilePanelTooltipAnchorKind.EQUIPMENT_SLOT,
                anchorIndex = anchorIndex,
                anchorId = "equipment:$slotId",
                titleLine = TileTextLine(slotLabel, TileTextTone.GOLD),
                bodyLines =
                    listOf(
                        TileTextLine(localizer.text("log.inventory.slot_nothing_equipped", "slot" to slotLabel), TileTextTone.GRAY),
                    ),
            )
        }
        return itemTooltip(
            localizer = localizer,
            snapshot = snapshot,
            anchorKind = TilePanelTooltipAnchorKind.EQUIPMENT_SLOT,
            anchorIndex = anchorIndex,
            anchorId = "equipment:$slotId",
            item = item,
            includeEquipmentComparison = false,
            titleSuffix = slotLabel,
        )
    }

    private fun inscriptionTooltip(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        hotkey: Int,
        anchorIndex: Int,
    ): TilePanelTooltipModel {
        val inscription = snapshot.uiState.inscriptions.firstOrNull { slot -> slot.hotkey == hotkey }
        if (inscription == null) {
            return TilePanelTooltipModel(
                anchorKind = TilePanelTooltipAnchorKind.INSCRIPTION_SLOT,
                anchorIndex = anchorIndex,
                anchorId = "inscription:$hotkey",
                titleLine = TileTextLine("$hotkey. ${localizer.text("ui.shell.inscription.empty")}", TileTextTone.GOLD),
                bodyLines =
                    listOf(
                        TileTextLine(localizer.text("log.inscription.slot_empty", "slot" to hotkey), TileTextTone.GRAY),
                    ),
            )
        }
        val categoryKey = inscription.categoryId.takeIf(String::isNotBlank)?.let { categoryId -> "ui.inscription.category.$categoryId" }
        val bodyLines =
            buildList {
                categoryKey?.let { key ->
                    add(TileTextLine(localizer.text("ui.inscription.detail.category", "category" to localizer.text(key)), TileTextTone.LIGHT_GRAY))
                }
                add(
                    TileTextLine(
                        localizer.text(
                            "ui.inscription.detail.cooldown",
                            "current" to inscription.cooldownRemaining,
                            "max" to inscription.maxCooldown,
                        ),
                        if (inscription.cooldownRemaining > 0) TileTextTone.RED else TileTextTone.LIGHT_GRAY,
                    ),
                )
                add(TileTextLine(localizer.text(inscription.descKey), TileTextTone.LIGHT_GRAY))
                if (inscription.requiresTarget) {
                    add(TileTextLine(localizer.text("ui.inscription.detail.requires_target"), TileTextTone.CYAN))
                }
            }
        return TilePanelTooltipModel(
            anchorKind = TilePanelTooltipAnchorKind.INSCRIPTION_SLOT,
            anchorIndex = anchorIndex,
            anchorId = "inscription:$hotkey",
            titleLine = TileTextLine("$hotkey. ${localizer.text(inscription.nameKey)}", TileTextTone.GOLD),
            bodyLines = bodyLines,
        )
    }

    private fun itemTooltip(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        anchorKind: TilePanelTooltipAnchorKind,
        anchorIndex: Int,
        anchorId: String,
        item: ItemRenderSnapshot,
        includeEquipmentComparison: Boolean,
        titleSuffix: String? = null,
    ): TilePanelTooltipModel {
        val presentation = QualityPresentation.from(item)
        val title =
            listOfNotNull(renderItemDisplay(localizer, item, presentation), titleSuffix?.let { suffix -> "[$suffix]" })
                .joinToString(" ")
        val bodyLines =
            DescriptionPresenter.presentInventoryItemLines(localizer, item).map { line ->
                TileTextLine(line.text, descriptionTone(line))
            } +
                itemDetailLines(localizer, item).map { line -> TileTextLine(line, TileTextTone.LIGHT_GRAY) } +
                if (includeEquipmentComparison) {
                    equipmentComparisonLines(localizer, snapshot, item)
                } else {
                    emptyList()
                }
        return TilePanelTooltipModel(
            anchorKind = anchorKind,
            anchorIndex = anchorIndex,
            anchorId = anchorId,
            titleLine = TileTextLine(title, itemTone(presentation)),
            bodyLines = bodyLines,
        )
    }

    private fun equipmentComparisonLines(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        candidate: ItemRenderSnapshot,
    ): List<TileTextLine> {
        val slotId = candidate.slotId ?: return emptyList()
        val slotLabel = equipmentSlotLabel(localizer, slotId)
        val equipped = snapshot.uiState.equipment.firstOrNull { slot -> slot.slotId == slotId }?.item
        if (equipped == null) {
            return listOf(TileTextLine(localizer.text("ui.inventory.detail.compare.no_equipped", "slot" to slotLabel), TileTextTone.GRAY))
        }
        val header =
            TileTextLine(
                localizer.text("ui.inventory.detail.compare.equipped", "item" to renderItemDisplay(localizer, equipped)),
                TileTextTone.GOLD,
            )
        val deltas = statDeltaLines(localizer, candidate.stats, equipped.stats)
        return listOf(header) +
            if (deltas.isEmpty()) {
                listOf(TileTextLine(localizer.text("ui.inventory.detail.compare.no_delta"), TileTextTone.GRAY))
            } else {
                deltas
            }
    }

    private fun statDeltaLines(
        localizer: Localizer,
        candidate: ItemStatModifierSnapshot,
        equipped: ItemStatModifierSnapshot,
    ): List<TileTextLine> =
        buildList {
            addDelta(localizer.text("ui.stat.str"), candidate.str - equipped.str)
            addDelta(localizer.text("ui.stat.dex"), candidate.dex - equipped.dex)
            addDelta(localizer.text("ui.stat.con"), candidate.con - equipped.con)
            addDelta(localizer.text("ui.stat.wil"), candidate.wil - equipped.wil)
            addDelta(localizer.text("ui.hud.attack.short"), candidate.attack - equipped.attack)
            addDelta(localizer.text("ui.hud.defense.short"), candidate.defense - equipped.defense)
            addDelta(localizer.text("ui.hud.accuracy.short"), candidate.accuracy - equipped.accuracy)
            addDelta(localizer.text("ui.hud.evasion.short"), candidate.evasion - equipped.evasion)
            addDelta(localizer.text("ui.hud.speed.short"), candidate.speed - equipped.speed)
            addDelta(localizer.text("ui.hud.hp.short"), candidate.maxHp - equipped.maxHp)
            addDelta(localizer.text("ui.hud.stamina.short"), candidate.maxStamina - equipped.maxStamina)
            addDecimalDelta(localizer.text("ui.inspect.mod.hp_regen"), candidate.hpRegen - equipped.hpRegen)
            addDecimalDelta(localizer.text("ui.inspect.mod.stamina_regen"), candidate.staminaRegen - equipped.staminaRegen)
            addPercentDelta(localizer.text("ui.inspect.mod.crit"), candidate.critChance - equipped.critChance)
            addPercentDelta(localizer.text("ui.inspect.mod.talent"), candidate.talentPower - equipped.talentPower)
        }

    private fun MutableList<TileTextLine>.addDelta(
        label: String,
        value: Int,
    ) {
        if (value != 0) {
            add(TileTextLine("$label ${signed(value)}", deltaTone(value.toDouble())))
        }
    }

    private fun MutableList<TileTextLine>.addDecimalDelta(
        label: String,
        value: Double,
    ) {
        if (value != 0.0) {
            add(TileTextLine("$label ${signedDecimal(value)}", deltaTone(value)))
        }
    }

    private fun MutableList<TileTextLine>.addPercentDelta(
        label: String,
        value: Double,
    ) {
        if (value != 0.0) {
            add(TileTextLine("$label ${signed((value * 100).toInt())}%", deltaTone(value)))
        }
    }

    private fun deltaTone(value: Double): TileTextTone = if (value > 0.0) TileTextTone.GREEN else TileTextTone.RED

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
                    val card = ModalCardModel.shopOffer(shop.shopId, selected, shardBalance = snapshot.uiState.shardBalance)
                    DescriptionPresenter.presentModalCardLines(localizer, card, DescriptionSurface.SHOP_ITEM)
                }.orEmpty()
            }

            com.ktome.client.input.ShopFocus.SELL -> {
                val sellEntry = shop.sellEntries.getOrNull(overlayState.inventorySelection)
                val item = sellEntry?.let { entry -> snapshot.uiState.inventory.firstOrNull { inventory -> inventory.index == entry.inventoryIndex }?.item }
                item?.let { selected -> DescriptionPresenter.presentShopItemLines(localizer, selected) }.orEmpty()
            }
        }

    private fun inscriptionReplacementPromptTileRow(
        visualResolver: VisualManifestResolver,
        line: InscriptionReplacementPromptLine,
    ): TileTextRow =
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
            icon = line.iconKey?.let { iconKey -> resolveVisual(visualResolver, iconKey) },
            kind =
                if (line.iconKey == DarkUiChromeVisualKeys.SHOP_REPLACEMENT_SLOT_MARKER) {
                    TileTextRowKind.SHOP_REPLACEMENT_SLOT
                } else {
                    TileTextRowKind.DEFAULT
                },
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
