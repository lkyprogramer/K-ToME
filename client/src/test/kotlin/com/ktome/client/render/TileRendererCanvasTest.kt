package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.ManifestLogSink
import com.ktome.client.assets.ManifestPrefixRule
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.ui.chrome.ChromeFrameBounds
import com.ktome.client.ui.chrome.ChromeFramePainter
import com.ktome.client.ui.chrome.ChromeSurfaceKind
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.input.ValidationOverlayPanelState
import com.ktome.client.render.layout.DemoSlotGridLayout
import com.ktome.client.render.layout.GameShellBounds
import com.ktome.client.ui.combat.CombatAffordanceResourceKeys
import com.ktome.client.ui.combat.CombatDecisionFrame
import com.ktome.client.ui.combat.CombatDecisionFrameState
import com.ktome.client.ui.combat.CombatDecisionPhase
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.client.ui.layout.ModalFrameLocalState
import com.ktome.client.ui.layout.PaneFocusAnchor
import com.ktome.client.ui.status.StatusHudRenderer
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.BossVariantRenderSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.CombatFeedbackSnapshot
import com.ktome.core.snapshot.CombatFeedbackTypeSnapshot
import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.EquipmentSlotSnapshot
import com.ktome.core.snapshot.FrontstageActionCategorySnapshot
import com.ktome.core.snapshot.FrontstageActionCueSnapshot
import com.ktome.core.snapshot.FrontstageActionPrioritySnapshot
import com.ktome.core.snapshot.FrontstageReadabilitySnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.InscriptionSlotSnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RewardPresentationEntrySnapshot
import com.ktome.core.snapshot.RewardPresentationSourceSnapshot
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot
import com.ktome.core.snapshot.TalentNodeStateSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.snapshot.TalentTreeNodeSnapshot
import com.ktome.core.snapshot.TalentTreeSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.validation.ValidationPhase4Guide
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationSummarySnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileRendererCanvasTest {
    @Test
    fun `render canvas honors manifest footprint dimensions`() {
        val canvas = RecordingTileCanvas()

        val summary =
            TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val actorDraw = canvas.assetDraws.single { draw -> draw.asset.resolvedKey == "actor.vanguard" }
        assertEquals(64f, actorDraw.width)
        assertEquals(32f, actorDraw.height)
    }

    @Test
    fun `render canvas places shell rail and right panel inside their bounds`() {
        val canvas = RecordingTileCanvas()
        val snapshot = sampleSnapshot(height = 6)
        val layout = TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, 32f, 32f)

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val navIcon = canvas.assetDraws.first { draw -> draw.asset.resolvedKey == DarkUiChromeVisualKeys.SHELL_NAV_COMPASS }
        val rightDivider = canvas.assetDraws.first { draw -> draw.asset.resolvedKey == DarkUiChromeVisualKeys.SHELL_RIGHT_SECTION_DIVIDER }
        assertTrue(navIcon.x >= layout.shell.leftRailBounds.x)
        assertTrue(navIcon.x + navIcon.width <= layout.shell.leftRailBounds.right)
        assertTrue(rightDivider.x >= layout.shell.rightPanelBounds.x)
        assertTrue(rightDivider.x + rightDivider.width <= layout.shell.rightPanelBounds.right)
    }

    @Test
    fun `render canvas draws focus ring around selected map pane`() {
        val canvas = RecordingTileCanvas()
        val snapshot = sampleSnapshot(height = 6)
        val layout = TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, 32f, 32f)

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP, paneFocusAnchor = PaneFocusAnchor.CONTEXT),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val focusColor = Color.valueOf("1CB7C8")
        val focusBounds = layout.demoShell.bottomDeck.logDeck
        assertTrue(
            canvas.rectDraws.any { rect ->
                rect.x == focusBounds.x &&
                    rect.y == focusBounds.y &&
                    rect.color == focusColor
            },
        )
    }

    @Test
    fun `render model exposes ui message and presentation cards`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.MAP, uiMessageKey = "ui.message.force-switch.shop"),
            )

        assertTrue(model.messageLines.any { line -> line.text == "Shop took over the current view." })
        assertTrue(model.messageLines.any { line -> line.text == "No new log entries." })
        assertTrue(model.messageLines.any { line -> line.text == "Actions, combat, and event records will appear here." })
        assertEquals("Log unavailable.", model.logPresentation.fallbackText)
        assertFalse(model.playerCard.name.isBlank())
        assertEquals("No available actions.", model.actionPanel.emptyStateText)
        assertTrue(model.targetCard.emptyStateText.isNotBlank())
    }

    @Test
    fun `render canvas rejects mismatched snapshot and model map dimensions`() {
        val snapshot =
            sampleSnapshot(
                width = 2,
                height = 1,
                cells =
                    listOf(
                        MapCellSnapshot(
                            x = 2,
                            y = 0,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.test.ground_01",
                        ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )
        }
    }

    @Test
    fun `render model map dimensions use snapshot metadata when map cells are sparse`() {
        val snapshot =
            sampleSnapshot(
                width = 3,
                height = 3,
                cells =
                    listOf(
                        MapCellSnapshot(
                            x = 0,
                            y = 0,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.test.ground_01",
                        ),
                    ),
            )

        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.MAP),
            )
        val diagnostics =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )

        assertEquals(TileMapDimensions(3, 3), model.mapDimensions)
        assertEquals(TileMapDimensions(3, 3), diagnostics.viewport.identity.mapDimensions)
    }

    @Test
    fun `render canvas passes validation inspect projection into viewport resolver`() {
        val cursor = com.ktome.core.map.Point(2, 2)
        val diagnostics =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 5, height = 5),
                overlayState =
                    OverlayState(
                        mode = UiMode.VALIDATION,
                        validationPanel = validationPanel(cursor),
                    ),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )

        assertEquals(TileViewportFocusMode.INSPECT, diagnostics.viewport.identity.focusMode)
        assertEquals(cursor, diagnostics.viewport.state.lastFocusTile)
        assertEquals(TileTooltipSource.INSPECT_CURSOR, diagnostics.overlayFrame.overlayModel.selectedTooltipSource)
        assertEquals(cursor, (diagnostics.overlayFrame.overlayModel.selectedTooltip?.anchor?.source as TileOverlayAnchor.WorldTile).tile)
    }

    @Test
    fun `render model localizes validation save block feedback`() {
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.MAP, uiMessageKey = "ui.message.save.blocked-in-validation"),
            )

        assertTrue(model.messageLines.any { line -> line.text == "Cannot save while validation mode is active." })
    }

    @Test
    fun `render model shows item detail as a distinct inventory frame`() {
        val item =
            ItemRenderSnapshot(
                baseItemId = "long_sword",
                nameKey = "item.long_sword.name",
                typeId = "WEAPON",
                descKey = "item.long_sword.desc",
            )
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        inventory =
                            listOf(
                                InventoryEntrySnapshot(
                                    index = 0,
                                    item = item,
                                ),
                            ),
                    ),
                overlayState =
                    OverlayState(
                        mode = UiMode.INVENTORY,
                        modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY), ModalFrame(ModalFrameKind.ITEM_DETAIL)),
                        inventorySelection = 0,
                    ),
            )

        assertEquals("Item Detail", model.sidebar.title)
        assertTrue(model.sidebar.rows.any { row -> row.text.contains("Long Sword") })
        assertTrue(model.sidebar.rows.any { row -> row.text == "E use, X/C compare stub, Backspace back, Esc close all." })
    }

    @Test
    fun `phase4 uiux pr04 item and explain descriptions use DescriptionPresenter keyword rendering`() {
        val item =
            ItemRenderSnapshot(
                baseItemId = "long_sword",
                nameKey = "item.long_sword.name",
                typeId = "WEAPON",
                iconKey = "item.short_sword.icon",
                descKey = "item.long_sword.desc",
                passiveDescriptions = listOf(RenderTextTokenSnapshot("affix.of_precision.desc")),
            )
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val inventoryModel =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        inventory =
                            listOf(
                                InventoryEntrySnapshot(
                                    index = 0,
                                    item = item,
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 0),
            )
        val inspectModel =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        cells =
                            listOf(
                                MapCellSnapshot(
                                    x = 0,
                                    y = 0,
                                    visibility = CellVisibilitySnapshot.VISIBLE,
                                    terrainTypeId = "floor",
                                    terrainVisualKey = "tileset.test.ground_01",
                                    items = listOf(item),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 0), explainPaneOpen = true),
            )

        assertTrue(inventoryModel.sidebar.rows.any { row -> row.text.contains("Marked") })
        assertTrue(inventoryModel.sidebar.rows.none { row -> row.text.contains("[[marked]]") })
        assertTrue(inspectModel.sidebar.rows.any { row -> row.text.contains("Marked") })
        assertEquals(1, inspectModel.sidebar.rows.count { row -> row.text.startsWith("Marked:") })
    }

    @Test
    fun `inspect sidebar includes telegraph warnings for the cursor cell`() {
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        overlays =
                            listOf(
                                OverlayRenderSnapshot(
                                    id = "telegraph:test",
                                    visualKey = "missing_visual",
                                    previewTurns = 1,
                                    dangerLevel = 3,
                                    shape = OverlayShapeSnapshot.SINGLE_TILE,
                                    sourceAbilityId = "telegraph.test",
                                    cells = listOf(GridPointSnapshot(0, 0)),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 0)),
            )

        assertTrue(model.sidebar.rows.any { row -> row.text.contains("1t High telegraph.test") })
    }

    @Test
    fun `target card and log prefix reuse the same telegraph presentation`() {
        val warning =
            OverlayRenderSnapshot(
                id = "telegraph:test",
                visualKey = "missing_visual",
                previewTurns = 1,
                dangerLevel = 3,
                shape = OverlayShapeSnapshot.SINGLE_TILE,
                sourceAbilityId = "telegraph.test",
                cells = listOf(GridPointSnapshot(0, 0)),
                warningMessage = RenderTextTokenSnapshot("log.boss.enrage"),
            )
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        overlays = listOf(warning),
                        logEvents = listOf(RenderLogEventSnapshot(RenderTextTokenSnapshot("log.warning.telegraph"))),
                    ),
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 0)),
            )

        assertTrue(model.targetCard.lines.any { line -> line.contains("High risk") && line.contains("telegraph.test") })
        assertTrue(model.messageLines.any { line -> line.text.startsWith("[1t High]") })
        assertTrue(model.messageLines.any { line -> line.icon?.resolvedKey == "missing_visual" })
    }

    @Test
    fun `warning log prefix follows overlay order instead of presentation sort order`() {
        val lowWarning =
            OverlayRenderSnapshot(
                id = "telegraph:b",
                visualKey = "missing_visual",
                previewTurns = 2,
                dangerLevel = 1,
                shape = OverlayShapeSnapshot.SINGLE_TILE,
                sourceAbilityId = "telegraph.low",
                cells = listOf(GridPointSnapshot(0, 0)),
                warningMessage = RenderTextTokenSnapshot("log.warning.telegraph"),
            )
        val highWarning =
            OverlayRenderSnapshot(
                id = "telegraph:a",
                visualKey = CombatAffordanceResourceKeys.TARGET_ICON,
                previewTurns = 1,
                dangerLevel = 3,
                shape = OverlayShapeSnapshot.SINGLE_TILE,
                sourceAbilityId = "telegraph.high",
                cells = listOf(GridPointSnapshot(1, 0)),
                warningMessage = RenderTextTokenSnapshot("log.warning.telegraph"),
            )
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        overlays = listOf(lowWarning, highWarning),
                        logEvents =
                            listOf(
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.warning.telegraph")),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.warning.telegraph")),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertTrue(model.messageLines[0].text.startsWith("[2t Low]"))
        assertEquals("missing_visual", model.messageLines[0].icon?.resolvedKey)
        assertTrue(model.messageLines[1].text.startsWith("[1t High]"))
        assertEquals(CombatAffordanceResourceKeys.TARGET_ICON, model.messageLines[1].icon?.resolvedKey)
    }

    @Test
    fun `combat decision panel consumes formal phase icons in sidebar and action panel`() {
        val state =
            OverlayState(
                mode = UiMode.TARGETING,
                modalFrames =
                    listOf(
                        ModalFrame(
                            kind = ModalFrameKind.COMBAT_DECISION,
                            localState =
                                ModalFrameLocalState(
                                    combatDecisionState = CombatDecisionFrame.initialState,
                                ),
                        ),
                    ),
            )
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        talents =
                            listOf(
                                TalentSlotSnapshot(
                                    slot = 1,
                                    talentId = "power_strike",
                                    nameKey = "talent.vanguard.power_strike.name",
                                    iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                                    level = 1,
                                    maxLevel = 5,
                                    resourceCost = 3,
                                    resourceLabelKey = "ui.hud.stamina.short",
                                    range = 1,
                                    minRange = 0,
                                    currentCooldown = 0,
                                    maxCooldown = 3,
                                    requiresTarget = true,
                                ),
                            ),
                    ),
                overlayState = state,
        )

        assertEquals("Combat Decision", model.sidebar.title)
        assertEquals("Choose Action", model.sidebar.rows.first().text)
        assertEquals(CombatAffordanceResourceKeys.ACTION_ICON, model.sidebar.rows.first().icon?.resolvedKey)
        assertTrue(model.actionPanel.entries.any { entry -> entry.icon?.resolvedKey == CombatAffordanceResourceKeys.ACTION_ICON })

        val canvas = RecordingTileCanvas()
        val summary =
            TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    talents =
                        listOf(
                            TalentSlotSnapshot(
                                slot = 1,
                                talentId = "power_strike",
                                nameKey = "talent.vanguard.power_strike.name",
                                iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                                level = 1,
                                maxLevel = 5,
                                resourceCost = 3,
                                resourceLabelKey = "ui.hud.stamina.short",
                                range = 1,
                                minRange = 0,
                                currentCooldown = 0,
                                maxCooldown = 3,
                                requiresTarget = true,
                            ),
                        ),
                ),
            overlayState = state,
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )
        assertTrue(canvas.assetDraws.any { draw -> draw.asset.resolvedKey == CombatAffordanceResourceKeys.ACTION_ICON })
    }

    @Test
    fun `dark uiux pr02 draws panel slot modal and hud assets`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    talents =
                        listOf(
                            TalentSlotSnapshot(
                                slot = 1,
                                talentId = "power_strike",
                                nameKey = "talent.vanguard.power_strike.name",
                                iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                                level = 1,
                                maxLevel = 5,
                                resourceCost = 3,
                                resourceLabelKey = "ui.hud.stamina.short",
                                range = 1,
                                minRange = 0,
                                currentCooldown = 0,
                                maxCooldown = 3,
                                requiresTarget = true,
                            ),
                        ),
                ),
            overlayState = OverlayState(mode = UiMode.INVENTORY, modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY))),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val drawnKeys = canvas.assetDraws.map { draw -> draw.asset.resolvedKey }
        listOf(
            DarkUiChromeVisualKeys.PANEL_BODY,
            DarkUiChromeVisualKeys.PANEL_CORNER_TL,
            DarkUiChromeVisualKeys.PANEL_CORNER_TR,
            DarkUiChromeVisualKeys.PANEL_CORNER_BL,
            DarkUiChromeVisualKeys.PANEL_CORNER_BR,
            DarkUiChromeVisualKeys.PANEL_EDGE_TOP,
            DarkUiChromeVisualKeys.PANEL_EDGE_RIGHT,
            DarkUiChromeVisualKeys.PANEL_EDGE_BOTTOM,
            DarkUiChromeVisualKeys.PANEL_EDGE_LEFT,
        ).forEach { key -> assertTrue(drawnKeys.contains(key), "$key missing from $drawnKeys") }
        assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.SLOT_EMPTY), drawnKeys.toString())
        assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.MODAL_BODY), drawnKeys.toString())
        assertTrue(drawnKeys.count { key -> key.startsWith("ui.hud.") } >= 2, drawnKeys.toString())
    }

    @Test
    fun `dark uiux pr02 keeps zh shell text inside chrome content bounds`() {
        val canvas = RecordingTileCanvas()
        val snapshot =
            sampleSnapshot(
                width = 18,
                height = 17,
                logEvents =
                    listOf(
                        RenderLogEventSnapshot(
                            RenderTextTokenSnapshot(
                                key = "log.zone.mechanic_hint",
                                arguments =
                                    listOf(
                                        RenderTextArgumentSnapshot(
                                            name = "hint",
                                            value = "如果在 Boss 线外拖得太久，这层会持续有巡逻增援补进来。",
                                        ),
                                    ),
                            ),
                        ),
                    ),
            )

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.ZH_CN),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
            shellWorldWidth = 1024f,
            shellWorldHeight = 768f,
        )

        val layout =
            TileRenderer.layoutMetrics(
                mapWidth = snapshot.metadata.width,
                mapHeight = snapshot.metadata.height,
                cellWidth = 32f,
                cellHeight = 32f,
                shellWorldWidth = 1024f,
                shellWorldHeight = 768f,
            )
        val inset = ChromeFramePainter.contentInsets(ChromeSurfaceKind.Panel).left
        val leftRight = layout.shell.leftRailBounds.right - inset
        val rightRight = layout.shell.rightPanelBounds.right - inset
        val logRight = layout.logX + layout.logWidth - inset
        val focusRight = layout.focusX + layout.focusWidth - inset

        canvas.textDraws
            .filter { draw ->
                draw.y >= layout.shell.leftRailBounds.y &&
                    draw.x >= layout.shell.leftRailBounds.x &&
                    draw.x < layout.shell.leftRailBounds.right
            }
            .forEach { draw -> assertTextEndsBefore(draw, leftRight) }
        canvas.textDraws
            .filter { draw ->
                draw.y >= layout.shell.rightPanelBounds.y &&
                    draw.x >= layout.shell.rightPanelBounds.x &&
                    draw.x < layout.shell.rightPanelBounds.right
            }
            .forEach { draw -> assertTextEndsBefore(draw, rightRight) }
        canvas.textDraws
            .filter { draw ->
                draw.y >= layout.cardY &&
                    draw.y < layout.cardY + layout.cardHeight &&
                    draw.x >= layout.logX &&
                    draw.x < layout.logX + layout.logWidth
            }
            .forEach { draw -> assertTextEndsBefore(draw, logRight) }
        canvas.textDraws
            .filter { draw ->
                draw.y >= layout.cardY &&
                    draw.y < layout.cardY + layout.cardHeight &&
                    draw.x >= layout.focusX &&
                    draw.x < layout.focusX + layout.focusWidth
            }
            .forEach { draw -> assertTextEndsBefore(draw, focusRight) }
    }

    @Test
    fun `dark uiux pr02-1 draws shell owner keys inside their consumer regions`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val layout = TileRenderer.layoutMetrics(mapWidth = 1, mapHeight = 1, cellWidth = 32f, cellHeight = 32f)
        DarkUiChromeVisualKeys.pr02_1DemoShellOwnerKeys.forEach { key ->
            assertTrue(canvas.assetDraws.any { draw -> draw.asset.resolvedKey == key }, "$key missing")
        }
        assertAssetOrder(
            canvas,
            DarkUiChromeVisualKeys.SHELL_MAP_STAGE_FRAME,
            DarkUiChromeVisualKeys.SHELL_MAP_STAGE_BACKDROP,
            "tileset.test.ground_01",
            "actor.vanguard",
        )
        canvas.assetDraws
            .filter { draw -> draw.asset.resolvedKey.startsWith("ui.shell.") }
            .forEach { draw ->
                val expected = expectedShellRegion(layout, draw.asset.resolvedKey)
                assertAssetInside(draw, expected)
            }
    }

    @Test
    fun `dark uiux pr02-1 draws right panel slots and hero crest scaffold`() {
        val canvas = RecordingTileCanvas()
        val weapon =
            ItemRenderSnapshot(
                baseItemId = "short_sword",
                nameKey = "item.short_sword.name",
                typeId = "WEAPON",
                iconKey = "item.short_sword.icon",
            )
        val snapshot =
            sampleSnapshot(
                equipment = listOf(EquipmentSlotSnapshot(slotId = "WEAPON", item = weapon)),
                inscriptions =
                    listOf(
                        InscriptionSlotSnapshot(
                            hotkey = 5,
                            inscriptionId = "phase_door",
                            nameKey = "inscription.phase_door.name",
                            descKey = "inscription.phase_door.desc",
                            iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                            categoryId = "MOVEMENT",
                            cooldownRemaining = 0,
                            maxCooldown = 10,
                        ),
                    ),
            )
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val layout = TileRenderer.layoutMetrics(mapWidth = 1, mapHeight = 1, cellWidth = 32f, cellHeight = 32f)
        val right = layout.demoShell.rightPanelLayout
        listOf(right.equipmentSlots, right.backpackSlots).forEach { grid ->
            assertTrue(grid.slotSide >= 42f)
            grid.slotBounds.forEach { slotBounds ->
                assertRightPanelSlotDraw(canvas, slotBounds)
            }
        }
        assertTrue(right.inscriptionSlots.slotSide >= 42f)
        right.inscriptionSlots.slotBounds.forEachIndexed { index, slotBounds ->
            assertRightPanelSlotDraw(canvas, inscriptionRowSlotBounds(right.inscriptions, right.inscriptionSlots, index, slotBounds))
        }
        assertTrue(right.equipment.y >= right.inscriptions.top)
        assertTrue(right.inscriptions.y >= right.backpack.top)
        assertTrue(right.backpack.y >= right.operationHints.top)
        assertEquals(2, right.equipmentSlots.columns)
        assertEquals(5, right.equipmentSlots.rows)
        assertEquals(9, right.equipmentSlots.slotBounds.size)
        assertEquals(2, right.inscriptionSlots.columns)
        assertEquals(4, right.inscriptionSlots.rows)
        assertEquals(8, right.inscriptionSlots.slotBounds.size)
        assertEquals(4, right.backpackSlots.columns)
        assertEquals(2, right.backpackSlots.rows)
        assertEquals(8, model.shell.demo.backpackSlots.size)
        assertEquals(9, model.shell.demo.equipmentSlots.size)
        assertEquals((5..12).map { hotkey -> hotkey.toString() }, model.shell.demo.inscriptionSlots.map { slot -> slot.label })
        assertTrue(model.shell.demo.inscriptionSlots.drop(4).all { slot -> slot.detail == null })
        assertTrue(model.shell.demo.heroSummaryLines.any { line -> line.contains("ATK") })
        assertTrue(model.shell.demo.heroSummaryLines.any { line -> line.contains("DEF") })
        assertTrue(canvas.textDraws.none { draw -> draw.text == "Ground" || draw.text == "Ground Items" })
        assertTrue(canvas.textDraws.any { draw -> draw.text.startsWith("5.") && draw.text.contains("Phase") && draw.x >= right.inscriptions.x && draw.x <= right.inscriptions.right })
        assertTrue(model.shell.demo.operationHints.any { hint -> hint.contains("5-8") })

        val crest = canvas.assetDraws.single { draw -> draw.asset.resolvedKey == DarkUiChromeVisualKeys.SHELL_HERO_CREST_PLACEHOLDER }
        assertTrue(crest.width >= 96f)
        assertTrue(crest.height >= 96f)
        assertAssetInside(crest, layout.demoShell.bottomDeck.heroCard)
    }

    @Test
    fun `combat decision target cursor marks illegal hover without relying on toast`() {
        val targetState =
            CombatDecisionFrameState(
                phase = CombatDecisionPhase.TARGET,
                selectedActionId = "talent:1",
                selectedMethodId = "default",
                skippedMethod = true,
            )
        val overlayState =
            OverlayState(
                mode = UiMode.TARGETING,
                targetingCursor = com.ktome.core.map.Point(0, 1),
                modalFrames =
                    listOf(
                        ModalFrame(
                            kind = ModalFrameKind.COMBAT_DECISION,
                            localState =
                                ModalFrameLocalState(
                                    targetingCursor = com.ktome.core.map.Point(0, 1),
                                    combatDecisionState = targetState,
                                ),
                        ),
                    ),
            )
        val snapshot =
            sampleSnapshot(
                height = 2,
                cells =
                    listOf(
                        MapCellSnapshot(
                            x = 0,
                            y = 0,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.test.ground_01",
                        ),
                        MapCellSnapshot(
                            x = 0,
                            y = 1,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.test.ground_01",
                        ),
                    ),
                talents =
                    listOf(
                        TalentSlotSnapshot(
                            slot = 1,
                            talentId = "power_strike",
                            nameKey = "talent.vanguard.power_strike.name",
                            iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                            level = 1,
                            maxLevel = 5,
                            resourceCost = 3,
                            resourceLabelKey = "ui.hud.stamina.short",
                            range = 1,
                            minRange = 0,
                            currentCooldown = 0,
                            maxCooldown = 3,
                            requiresTarget = true,
                        ),
                    ),
                targetablePositions = listOf(GridPointSnapshot(0, 0)),
            )

        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = overlayState,
            )
        assertEquals(TileTargetCursorState.ILLEGAL, model.targetCursorState)

        val canvas = RecordingTileCanvas()
        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = overlayState,
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )
        val invalidCursorColor = UiDesignTokens.color.telegraph.high.color().toString()
        assertTrue(canvas.rectDraws.any { draw -> draw.color.toString() == invalidCursorColor })
    }

    @Test
    fun `combat decision target cursor stays legal for free cursor inscriptions`() {
        val targetState =
            CombatDecisionFrameState(
                phase = CombatDecisionPhase.TARGET,
                selectedActionId = "inscription:5",
                selectedMethodId = "default",
                skippedMethod = true,
            )
        val overlayState =
            OverlayState(
                mode = UiMode.TARGETING,
                targetingCursor = com.ktome.core.map.Point(0, 1),
                modalFrames =
                    listOf(
                        ModalFrame(
                            kind = ModalFrameKind.COMBAT_DECISION,
                            localState =
                                ModalFrameLocalState(
                                    targetingCursor = com.ktome.core.map.Point(0, 1),
                                    combatDecisionState = targetState,
                                ),
                        ),
                    ),
            )
        val snapshot =
            sampleSnapshot(
                height = 2,
                cells =
                    listOf(
                        MapCellSnapshot(
                            x = 0,
                            y = 0,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.test.ground_01",
                        ),
                        MapCellSnapshot(
                            x = 0,
                            y = 1,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.test.ground_01",
                        ),
                    ),
                inscriptions =
                    listOf(
                        InscriptionSlotSnapshot(
                            hotkey = 5,
                            inscriptionId = "phase_door",
                            nameKey = "inscription.phase_door.name",
                            descKey = "inscription.phase_door.desc",
                            iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                            categoryId = "MOVEMENT",
                            cooldownRemaining = 0,
                            maxCooldown = 10,
                            requiresTarget = true,
                        ),
                    ),
                targetablePositions = emptyList(),
            )

        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = overlayState,
            )

        assertEquals(TileTargetCursorState.LEGAL, model.targetCursorState)
    }

    @Test
    fun `inspect target card falls back to terrain and empty tile copy`() {
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        height = 2,
                        cells =
                            listOf(
                                MapCellSnapshot(
                                    x = 0,
                                    y = 0,
                                    visibility = CellVisibilitySnapshot.VISIBLE,
                                    terrainTypeId = "floor",
                                    terrainVisualKey = "tileset.test.ground_01",
                                ),
                                MapCellSnapshot(
                                    x = 0,
                                    y = 1,
                                    visibility = CellVisibilitySnapshot.VISIBLE,
                                    terrainTypeId = "floor",
                                    terrainVisualKey = "tileset.test.ground_01",
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 1)),
            )

        assertFalse(model.targetCard.isEmpty)
        assertTrue(model.targetCard.lines.any { line -> line == "Move the cursor to an enemy, terrain, dropped item, or warning tile for details." })
    }

    @Test
    fun `render canvas draws dungeon fog for explored and hidden cells`() {
        val canvas = RecordingTileCanvas()

        val summary =
            TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    width = 3,
                    height = 1,
                    cells =
                        listOf(
                            MapCellSnapshot(
                                x = 0,
                                y = 0,
                                visibility = CellVisibilitySnapshot.VISIBLE,
                                terrainTypeId = "floor",
                                terrainVisualKey = "tileset.test.ground_01",
                            ),
                            MapCellSnapshot(
                                x = 1,
                                y = 0,
                                visibility = CellVisibilitySnapshot.EXPLORED,
                                terrainTypeId = "floor",
                                terrainVisualKey = "tileset.test.ground_01",
                            ),
                            MapCellSnapshot(
                                x = 2,
                                y = 0,
                                visibility = CellVisibilitySnapshot.HIDDEN,
                                terrainTypeId = "floor",
                                terrainVisualKey = "tileset.test.ground_01",
                            ),
                        ),
                ),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val explored = summary.viewport.tileRect(com.ktome.core.map.Point(1, 0))
        val hidden = summary.viewport.tileRect(com.ktome.core.map.Point(2, 0))
        val cellRects =
            canvas.rectDraws.filter { draw ->
                draw.width == 32f && draw.height == 32f
            }

        assertTrue(cellRects.any { draw -> draw.x == explored.x.toFloat() && draw.y == explored.y.toFloat() && draw.color.a == 0.42f })
        assertTrue(cellRects.any { draw -> draw.x == hidden.x.toFloat() && draw.y == hidden.y.toFloat() && draw.color.a == 0.50f })
    }

    @Test
    fun `render canvas draws ground loot marker with count badge and rarity glyph`() {
        val canvas = RecordingTileCanvas()
        val rareItem =
            ItemRenderSnapshot(
                baseItemId = "short_sword",
                nameKey = "item.short_sword.name",
                typeId = "WEAPON",
                iconKey = "item.short_sword.icon",
                qualityTierId = "RARE",
            )

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    cells =
                        listOf(
                            MapCellSnapshot(
                                x = 0,
                                y = 0,
                                visibility = CellVisibilitySnapshot.VISIBLE,
                                terrainTypeId = "floor",
                                terrainVisualKey = "tileset.test.ground_01",
                                items = List(10) { rareItem },
                            ),
                        ),
                ),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(canvas.assetDraws.any { draw -> draw.asset.resolvedKey == "item.short_sword.icon" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "9+" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "\u25C6\u25C6" })
    }

    @Test
    fun `render canvas keeps hud gauges clear of the title line`() {
        val canvas = RecordingTileCanvas()
        val snapshot = sampleSnapshot(width = 18, height = 17)

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val titleDraw =
            canvas.textDraws.first { draw ->
                draw.text == "Hero"
            }
        val titleBounds =
            TileRenderer.textApproximationBounds(
                style = titleDraw.style,
                text = titleDraw.text,
                x = titleDraw.x,
                y = titleDraw.y,
            )
        val titleTextBottom = titleDraw.y - titleBounds[3]
        val gaugeBackgrounds = canvas.rectDraws.filter { draw -> draw.color == UiDesignTokens.color.bar.background.color() }
        val topGauge = requireNotNull(gaugeBackgrounds.maxByOrNull { draw -> draw.y })

        assertTrue(topGauge.y + topGauge.height <= titleTextBottom)
        assertTrue(titleTextBottom - (topGauge.y + topGauge.height) >= 4f)
    }

    @Test
    fun `render canvas keeps chinese hotbar labels inside their slot cards`() {
        val canvas = RecordingTileCanvas()
        val snapshot =
            sampleSnapshot(
                width = 18,
                height = 17,
                talents =
                    listOf(
                        TalentSlotSnapshot(
                            slot = 1,
                            talentId = "power_strike",
                            nameKey = "talent.vanguard.power_strike.name",
                            level = 1,
                            maxLevel = 5,
                            resourceCost = 8,
                            resourceLabelKey = "ui.hud.stamina.short",
                            range = 1,
                            minRange = 0,
                            currentCooldown = 0,
                            maxCooldown = 3,
                            requiresTarget = false,
                        ),
                        TalentSlotSnapshot(
                            slot = 2,
                            talentId = "shield_bash",
                            nameKey = "talent.vanguard.shield_bash.name",
                            level = 1,
                            maxLevel = 5,
                            resourceCost = 10,
                            resourceLabelKey = "ui.hud.stamina.short",
                            range = 1,
                            minRange = 0,
                            currentCooldown = 0,
                            maxCooldown = 3,
                            requiresTarget = false,
                        ),
                        TalentSlotSnapshot(
                            slot = 3,
                            talentId = "guard_stance",
                            nameKey = "talent.vanguard.guard_stance.name",
                            level = 1,
                            maxLevel = 5,
                            resourceCost = 8,
                            resourceLabelKey = "ui.hud.stamina.short",
                            range = 0,
                            minRange = 0,
                            currentCooldown = 0,
                            maxCooldown = 3,
                            requiresTarget = false,
                        ),
                        TalentSlotSnapshot(
                            slot = 4,
                            talentId = "charge",
                            nameKey = "talent.vanguard.charge.name",
                            level = 1,
                            maxLevel = 5,
                            resourceCost = 12,
                            resourceLabelKey = "ui.hud.stamina.short",
                            range = 5,
                            minRange = 1,
                            currentCooldown = 0,
                            maxCooldown = 3,
                            requiresTarget = true,
                        ),
                    ),
            )

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.ZH_CN),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val layout = TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, 32f, 32f)
        val guardLabel = canvas.textDraws.single { draw -> draw.text.startsWith("格挡") }
        val guardBounds =
            TileRenderer.textApproximationBounds(
                style = guardLabel.style,
                text = guardLabel.text,
                x = guardLabel.x,
                y = guardLabel.y,
            )
        val guardSlot = layout.demoShell.bottomDeck.actionSlotBounds[2]

        assertTrue(guardLabel.x + guardBounds[2] <= guardSlot.right - 4f)
    }

    @Test
    fun `render canvas keeps long chinese route hints readable in bottom log`() {
        val canvas = RecordingTileCanvas()
        val snapshot =
            sampleSnapshot(
                width = 18,
                height = 17,
                logEvents =
                    listOf(
                        RenderLogEventSnapshot(
                            RenderTextTokenSnapshot(
                                key = "log.zone.mechanic_hint",
                                arguments =
                                    listOf(
                                        RenderTextArgumentSnapshot(
                                            name = "hint",
                                            value = "如果在 Boss 线外拖得太久，这层会持续有巡逻增援补进来。",
                                        ),
                                    ),
                            ),
                        ),
                    ),
            )

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.ZH_CN),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val layout = TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, 32f, 32f)
        val routeHintDraws =
            canvas.textDraws.filter { draw ->
                draw.x >= layout.logX &&
                    draw.x < layout.logX + layout.logWidth &&
                    draw.y >= layout.cardY &&
                    draw.y < layout.cardY + layout.cardHeight
            }

        assertTrue(routeHintDraws.any { draw -> draw.text.contains("路线提示") })
        assertTrue(routeHintDraws.none { draw -> draw.text.contains("…") })
        val routeHintText = routeHintDraws.joinToString(separator = "") { draw -> draw.text }
        assertTrue(routeHintText.contains("持续有巡逻增援补进来"), routeHintText)
        assertTrue(
            routeHintDraws.all { draw ->
                val bounds = TileRenderer.textApproximationBounds(draw.style, draw.text, draw.x, draw.y)
                draw.x + bounds[2] <= layout.logX + layout.logWidth + 0.5f
            },
        )
    }

    @Test
    fun `render canvas derives boss variant tint from visual manifest metadata`() {
        val canvas = RecordingTileCanvas()
        val baseSnapshot = sampleSnapshot(width = 2, height = 1)
        val snapshot =
            baseSnapshot.copy(
                metadata = baseSnapshot.metadata.copy(width = 2, height = 1),
                mapCells =
                    listOf(
                        MapCellSnapshot(
                            x = 0,
                            y = 0,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.test.ground_01",
                        ),
                        MapCellSnapshot(
                            x = 1,
                            y = 0,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.test.ground_01",
                        ),
                    ),
                actors =
                    baseSnapshot.actors +
                        ActorRenderSnapshot(
                            entityId = 2,
                            x = 1,
                            y = 0,
                            visualKey = "actor.vanguard",
                            nameKey = "boss.test.name",
                            isPlayer = false,
                            roleKind = ActorRoleKindSnapshot.BOSS,
                            bossVariant =
                                BossVariantRenderSnapshot(
                                    variantId = "boss.variant.molten_glass",
                                    nameKey = "boss.variant.molten_glass.name",
                                    visualTintKey = "vfx.boss.variant.molten_glass",
                                ),
                        ),
            )

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val actorDraw = canvas.assetDraws.single { draw -> draw.asset.resolvedKey == "actor.vanguard" && draw.tintColorHex != null }
        assertEquals("#FF7A3C", actorDraw.tintColorHex)
    }

    @Test
    fun `render canvas keeps three gauge stack clear of the title line for dual resource classes`() {
        val canvas = RecordingTileCanvas()
        val baseSnapshot = sampleSnapshot()
        val snapshot =
            baseSnapshot.copy(
                uiState =
                    baseSnapshot.uiState.copy(
                        playerStatus =
                            baseSnapshot.uiState.playerStatus.copy(
                                currentResource = 11,
                                maxResource = 20,
                                resourceLabelKey = "ui.hud.mana.short",
                                resourceTypeId = "MANA",
                                secondaryResourceCurrent = 7,
                                secondaryResourceMax = 12,
                                secondaryResourceLabelKey = "ui.hud.equilibrium.short",
                                secondaryResourceTypeId = "EQUILIBRIUM",
                                secondaryResourceStableMin = 3,
                                secondaryResourceStableMax = 9,
                            ),
                    ),
            )

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val titleDraw = canvas.textDraws.first { draw -> draw.text == "Hero" }
        val gaugeBackgrounds = canvas.rectDraws.filter { draw -> draw.color == UiDesignTokens.color.bar.background.color() }
        val sortedGaugeBackgrounds = gaugeBackgrounds.sortedBy { draw -> draw.y }
        val titleBounds =
            TileRenderer.textApproximationBounds(
                style = titleDraw.style,
                text = titleDraw.text,
                x = titleDraw.x,
                y = titleDraw.y,
            )
        val titleTextBottom = titleDraw.y - titleBounds[3]

        assertEquals(2, sortedGaugeBackgrounds.size)
        assertTrue(sortedGaugeBackgrounds.zipWithNext().all { (lower, upper) -> upper.y >= lower.y + lower.height + 2f })
        assertTrue(sortedGaugeBackgrounds.last().y + sortedGaugeBackgrounds.last().height <= titleTextBottom)
        assertTrue(titleTextBottom - (sortedGaugeBackgrounds.last().y + sortedGaugeBackgrounds.last().height) >= 4f)
    }

    @Test
    fun `render canvas shows frontstage focus card and recent reward detail text in map mode`() {
        val canvas = RecordingTileCanvas()
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val baseSnapshot = sampleSnapshot(width = 11, height = 20)
        val snapshot =
            baseSnapshot.copy(
                uiState =
                    baseSnapshot.uiState.copy(
                        recentRewards =
                            listOf(
                                RewardPresentationEntrySnapshot(
                                    source = RewardPresentationSourceSnapshot.SECRET_ZONE,
                                    sourceLabelKey = "ui.reward.source.secret_zone",
                                    itemDisplayName = RenderTextTokenSnapshot("tile.floor.name"),
                                    detailText =
                                        RenderTextTokenSnapshot(
                                            "ui.inspect.passive.hp_regen_turn",
                                            listOf(RenderTextArgumentSnapshot(name = "amount", value = "2")),
                                        ),
                                ),
                            ),
                        frontstageReadability =
                            FrontstageReadabilitySnapshot(
                                terrainHighlights = listOf(RenderTextTokenSnapshot("ui.hud.frontstage.terrain.water")),
                                recentActionCues =
                                    listOf(
                                        FrontstageActionCueSnapshot(
                                            category = FrontstageActionCategorySnapshot.SEARCH,
                                            priority = FrontstageActionPrioritySnapshot.MEDIUM,
                                            stableKey = "search:no_target",
                                            message = RenderTextTokenSnapshot("log.search.no_target"),
                                        ),
                                        FrontstageActionCueSnapshot(
                                            category = FrontstageActionCategorySnapshot.SECRET,
                                            priority = FrontstageActionPrioritySnapshot.CRITICAL,
                                            stableKey = "secret:enter:test",
                                            message = RenderTextTokenSnapshot("ui.hud.frontstage.terrain.oil"),
                                        ),
                                        FrontstageActionCueSnapshot(
                                            category = FrontstageActionCategorySnapshot.PASSIVE,
                                            priority = FrontstageActionPrioritySnapshot.LOW,
                                            stableKey = "passive:test",
                                            message = RenderTextTokenSnapshot("ui.hud.frontstage.terrain.ice"),
                                        ),
                                    ),
                            ),
                    ),
            )

        TileRenderer.renderToCanvas(
            localizer = localizer,
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.MAP),
            )
        assertEquals(
            TileTextTone.GREEN,
            model.sidebar.rows.first { row -> row.text.contains(localizer.text("log.search.no_target")) }.tone,
        )
        assertEquals(
            TileTextTone.GOLD,
            model.sidebar.rows.first { row -> row.text.contains(localizer.text("ui.hud.frontstage.terrain.oil")) }.tone,
        )
        assertEquals(
            TileTextTone.LIGHT_GRAY,
            model.sidebar.rows.first { row -> row.text.contains(localizer.text("ui.hud.frontstage.terrain.ice")) }.tone,
        )
    }

    @Test
    fun `responsive layout keeps sidebar compact on small maps`() {
        val metrics = TileRenderer.layoutMetrics(mapWidth = 11, mapHeight = 10, cellWidth = 32f, cellHeight = 32f)

        assertTrue(metrics.sidebarWidth <= 420f)
        assertEquals(1280f, metrics.worldWidth)
        assertTrue(metrics.shell.mapInnerPadding.left > 0)
    }

    @Test
    fun `responsive layout keeps bottom panels inside world width`() {
        val metrics = TileRenderer.layoutMetrics(mapWidth = 9, mapHeight = 10, cellWidth = 32f, cellHeight = 32f)

        assertTrue(metrics.logWidth >= 180f)
        assertTrue(metrics.focusX + metrics.focusWidth <= metrics.demoShell.bottomDeck.bounds.right + 0.5f)
    }

    @Test
    fun `render canvas keeps map dominant regions separate at 1024 by 768 breakpoint`() {
        val canvas = RecordingTileCanvas()
        val metrics =
            TileRenderer.layoutMetrics(
                mapWidth = 18,
                mapHeight = 17,
                cellWidth = 32f,
                cellHeight = 32f,
                shellWorldWidth = 1024f,
                shellWorldHeight = 768f,
            )

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(width = 18, height = 17),
            overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 0)),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
            shellWorldWidth = 1024f,
            shellWorldHeight = 768f,
        )

        assertTrue(metrics.worldWidth <= 1024f)
        assertTrue(metrics.worldHeight <= 768f)
        assertTrue(metrics.cardY + metrics.cardHeight <= metrics.mapOffsetY)
        assertTrue(metrics.infoX + metrics.infoWidth <= metrics.focusX)
        assertEquals(0f, metrics.footerHintBounds.width)
        assertTrue(metrics.focusX + metrics.focusWidth <= metrics.logX)
        assertTrue(metrics.sidebarX >= metrics.shell.mapBounds.right + metrics.panelGap)
        val focusRingRects =
            canvas.rectDraws.filter { draw ->
                draw.x >= metrics.shell.mapBounds.x &&
                    draw.y >= metrics.mapOffsetY &&
                    draw.x + draw.width <= metrics.shell.mapBounds.right &&
                    draw.y + draw.height <= metrics.worldHeight &&
                    ((draw.width == 32f && draw.height == 2f) || (draw.width == 2f && draw.height == 32f))
            }
        assertEquals(4, focusRingRects.size)
    }

    @Test
    fun `responsive layout expands log panel across available center space`() {
        val metrics = TileRenderer.layoutMetrics(mapWidth = 24, mapHeight = 10, cellWidth = 32f, cellHeight = 32f)

        assertEquals(metrics.demoShell.bottomDeck.logDeck.x, metrics.logX)
        assertEquals(metrics.demoShell.bottomDeck.logDeck.width, metrics.logWidth)
        assertTrue(metrics.focusX + metrics.focusWidth <= metrics.logX)
        assertTrue(metrics.logX + metrics.logWidth <= metrics.demoShell.bottomDeck.bounds.right + 0.5f)
        assertTrue(metrics.logWidth >= 180f)
    }

    @Test
    fun recordsExplicitLayerFlushBoundaries() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertEquals(
            listOf(
                TileLayerFlushReason.BACKGROUND,
                TileLayerFlushReason.SHELL_OUTER_FRAME,
                TileLayerFlushReason.MAP_STAGE_FRAME,
                TileLayerFlushReason.MAP_TERRAIN_BASE,
                TileLayerFlushReason.MAP_PROPS_AND_DECALS,
                TileLayerFlushReason.MAP_SPRITE_OVERLAYS_AND_TELEGRAPHS,
                TileLayerFlushReason.MAP_ACTORS,
                TileLayerFlushReason.MAP_PLAYER_INDICATOR,
                TileLayerFlushReason.MAP_GROUND_LOOT_MARKERS,
                TileLayerFlushReason.MAP_FOG_VEILS,
                TileLayerFlushReason.MAP_ACTIVE_CURSOR,
                TileLayerFlushReason.MAP_COMBAT_FEEDBACK,
                TileLayerFlushReason.MAP_WARM_OVERLAY,
                TileLayerFlushReason.SHELL_NAV_RAIL,
                TileLayerFlushReason.SHELL_RIGHT_PANEL,
                TileLayerFlushReason.SHELL_BOTTOM_HERO,
                TileLayerFlushReason.SHELL_BOTTOM_ACTION_DECK,
                TileLayerFlushReason.SHELL_BOTTOM_LOG_DECK,
                TileLayerFlushReason.OVERLAY_PASSIVE_TOOLTIP,
                TileLayerFlushReason.OVERLAY_TOAST,
                TileLayerFlushReason.OVERLAY_MODAL_BACKDROP,
                TileLayerFlushReason.OVERLAY_MODAL,
                TileLayerFlushReason.OVERLAY_MODAL_EXPLICIT_TOOLTIP,
                TileLayerFlushReason.DEBUG_HINTS,
            ),
            canvas.flushes,
        )
    }

    @Test
    fun overlayFrameDoesNotCarryRawOverlayState() {
        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val fieldNames = summary.overlayFrame::class.java.declaredFields.map { it.name }.toSet()
        assertFalse("overlayState" in fieldNames)
        assertFalse("modalFrames" in fieldNames)
        assertFalse("projection" in fieldNames)
        assertFalse("viewport" in fieldNames)
    }

    @Test
    fun renderDiagnosticsDoesNotExposeAggregateFrameState() {
        val diagnostics =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val fieldNames = diagnostics::class.java.declaredFields.map { it.name }.toSet()
        assertEquals(setOf("viewport", "overlayFrame"), fieldNames)
        assertFalse("projection" in fieldNames)
        assertFalse("layerPlan" in fieldNames)
        assertFalse("model" in fieldNames)
    }

    @Test
    fun shellFrameCarriesPaneFocusFactAndTextLayoutOnly() {
        val fieldNames = ShellRenderFrame::class.java.declaredFields.map { it.name }.toSet()

        assertEquals(setOf("model", "layout", "textLayout", "paneFocusAnchor"), fieldNames)
    }

    @Test
    fun viewportUsesShellMapBounds() {
        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 90, height = 56),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )
        val layout = TileRenderer.layoutMetrics(90, 56, 32f, 32f)

        assertEquals(layout.shell.cellAlignedMapBounds, summary.viewport.mapBounds)
        assertTrue(summary.viewport.visibleRange.columns < 90)
    }

    @Test
    fun overlayDrawsAboveShellAndBottomHud() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(),
            overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 0)),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(canvas.flushes.indexOf(TileLayerFlushReason.SHELL_BOTTOM_LOG_DECK) < canvas.flushes.indexOf(TileLayerFlushReason.OVERLAY_PASSIVE_TOOLTIP))
    }

    @Test
    fun modalUsesModalSafeBounds() {
        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.INVENTORY, modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY))),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val modal = requireNotNull(summary.overlayFrame.overlayModel.activeModal)
        assertTrue(modal.bounds.x >= summary.overlayFrame.modalSafeBounds.left)
        assertTrue(modal.bounds.right <= summary.overlayFrame.modalSafeBounds.right)
        assertTrue(modal.bounds.y >= summary.overlayFrame.modalSafeBounds.bottom)
        assertTrue(modal.bounds.top <= summary.overlayFrame.modalSafeBounds.top)
    }

    @Test
    fun tooltipAvoidsBottomLogReservedBounds() {
        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 0)),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )
        val tooltip = requireNotNull(summary.overlayFrame.overlayModel.selectedTooltip)

        assertTrue(tooltip.placedRect.y >= summary.overlayFrame.bottomLogReservedBounds.top)
    }

    @Test
    fun `render canvas keeps tooltip text inside chrome content bounds`() {
        val canvas = RecordingTileCanvas()
        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 0)),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )
        val tooltip = requireNotNull(summary.overlayFrame.overlayModel.selectedTooltip)
        val rect = tooltip.placedRect
        val content =
            ChromeFramePainter.contentBounds(
                ChromeFrameBounds(
                    x = rect.x.toFloat(),
                    y = rect.y.toFloat(),
                    width = rect.width.toFloat(),
                    height = rect.height.toFloat(),
                ),
                ChromeSurfaceKind.Tooltip,
            )
        val tooltipText = (listOf(tooltip.titleLine.text) + tooltip.bodyLines.map { line -> line.text }).toSet()
        val tooltipDraws =
            canvas.textDraws.filter { draw ->
                draw.text in tooltipText &&
                    draw.x >= rect.x &&
                    draw.x <= rect.right &&
                    draw.y >= rect.y &&
                    draw.y <= rect.top
            }

        assertTrue(tooltipDraws.isNotEmpty())
        tooltipDraws.forEach { draw ->
            val bounds = TileRenderer.textApproximationBounds(draw.style, draw.text, draw.x, draw.y)
            assertTrue(draw.x >= content.x - 0.5f, "${draw.text} starts before tooltip content")
            assertTrue(draw.x + bounds[2] <= content.right + 0.5f, "${draw.text} exceeds tooltip content")
            assertTrue(draw.y <= content.top + 0.5f, "${draw.text} starts above tooltip content")
            assertTrue(draw.y - bounds[3] >= content.y - 0.5f, "${draw.text} descends below tooltip content")
        }
    }

    @Test
    fun `render model surfaces talent use and targeting hints in map mode`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        talents =
                            listOf(
                                TalentSlotSnapshot(
                                    slot = 1,
                                    talentId = "fireball",
                                    nameKey = "talent.arcanist.fireball.name",
                                    level = 1,
                                    maxLevel = 5,
                                    resourceCost = 12,
                                    resourceLabelKey = "ui.hud.mana.short",
                                    resourceTypeId = "MANA",
                                    range = 4,
                                    minRange = 1,
                                    currentCooldown = 0,
                                    maxCooldown = 4,
                                    requiresTarget = true,
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertTrue(model.sidebar.rows.any { row -> row.text == "1-4 use talent" })
        assertTrue(model.sidebar.rows.any { row -> row.text == "L edit loadout" })
        assertTrue(model.sidebar.rows.any { row -> row.text == "Targeted talent: move cursor, Enter confirm" })
        assertTrue(model.hud.hotbar.single().resourceText.contains("AIM 4"))
        assertTrue(model.sidebar.rows.any { row -> row.text == "A ruined border outpost and the opening stretch of the run." })
    }

    @Test
    fun `render model exposes secondary resource as a dedicated gauge`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val baseSnapshot = sampleSnapshot()
        val snapshot =
            baseSnapshot.copy(
                uiState =
                    baseSnapshot.uiState.copy(
                        playerStatus =
                            baseSnapshot.uiState.playerStatus.copy(
                                currentResource = 11,
                                maxResource = 20,
                                resourceLabelKey = "ui.hud.mana.short",
                                resourceTypeId = "MANA",
                                secondaryResourceCurrent = 7,
                                secondaryResourceMax = 12,
                                secondaryResourceLabelKey = "ui.hud.equilibrium.short",
                                secondaryResourceTypeId = "EQUILIBRIUM",
                                secondaryResourceStableMin = 3,
                                secondaryResourceStableMax = 9,
                            ),
                    ),
            )

        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertNotNull(model.hud.secondaryResourceGauge)
        assertEquals("EQL", model.hud.secondaryResourceGauge!!.label)
        assertEquals("EQUILIBRIUM", model.hud.secondaryResourceGauge!!.resourceTypeId)
        assertEquals(3, model.hud.secondaryResourceGauge!!.stableMin)
        assertEquals(9, model.hud.secondaryResourceGauge!!.stableMax)
        assertFalse(model.hud.summaryText.contains("EQL"))
    }

    @Test
    fun `shell model keeps hp resource and xp in bottom hud instead of right panel`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertEquals("EXPERIENCE", model.hud.experienceGauge.resourceTypeId)
        assertTrue(model.shell.leftRail.rows.any { row -> row.text == "Critical Hint" })
        assertTrue(model.shell.rightPanel.rows.any { row -> row.text == "HP, resource, and XP are owned by bottom HUD." })
        assertFalse(model.shell.rightPanel.rows.any { row -> row.text.startsWith("HP ") })
        assertFalse(model.shell.rightPanel.rows.any { row -> row.text.startsWith("STA ") })
        assertFalse(model.shell.rightPanel.rows.any { row -> row.text.contains(model.hud.experienceGauge.summary) })
    }

    @Test
    fun `shell right panel keeps ground equipment inscriptions and backpack sections in order`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val item =
            ItemRenderSnapshot(
                baseItemId = "short_sword",
                nameKey = "item.short_sword.name",
                typeId = "WEAPON",
                iconKey = "item.short_sword.icon",
            )
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        cells =
                            listOf(
                                MapCellSnapshot(
                                    x = 0,
                                    y = 0,
                                    visibility = CellVisibilitySnapshot.VISIBLE,
                                    terrainTypeId = "floor",
                                    terrainVisualKey = "tileset.test.ground_01",
                                    items = listOf(item),
                                ),
                            ),
                        inscriptions =
                            listOf(
                                InscriptionSlotSnapshot(
                                    hotkey = 5,
                                    inscriptionId = "phase_door",
                                    nameKey = "inscription.phase_door.name",
                                    descKey = "inscription.phase_door.desc",
                                    iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                                    categoryId = "MOVEMENT",
                                    cooldownRemaining = 0,
                                    maxCooldown = 10,
                                ),
                            ),
                        equipment = listOf(EquipmentSlotSnapshot(slotId = "WEAPON", item = item)),
                        inventory = listOf(InventoryEntrySnapshot(index = 0, item = item)),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        val rowTexts = model.shell.rightPanel.rows.map(TileTextRow::text)
        val equipmentIndex = rowTexts.indexOf("Equipment")
        val inscriptionIndex = rowTexts.indexOf("Inscriptions")
        val backpackIndex = rowTexts.indexOf("Backpack: 1 items")

        assertTrue(rowTexts.none { row -> row == "Ground" })
        assertTrue(equipmentIndex >= 0)
        assertTrue(inscriptionIndex > equipmentIndex)
        assertTrue(backpackIndex > inscriptionIndex)
        assertTrue(rowTexts.any { row -> row.contains("Short Sword") })
        assertTrue(rowTexts.any { row -> row == "5. Phase Door" })
    }

    @Test
    fun `shell quest summary consumes latest objective log token when available`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        logEvents =
                            listOf(
                                RenderLogEventSnapshot(
                                    RenderTextTokenSnapshot(
                                        key = "log.objective.progress",
                                        arguments =
                                            listOf(
                                                RenderTextArgumentSnapshot(name = "objective", valueKey = "objective.shattered_outpost_breach.name"),
                                                RenderTextArgumentSnapshot(name = "step", valueKey = "objective.shattered_outpost_breach.step.inner_breach"),
                                            ),
                                    ),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertTrue(
            model.shell.leftRail.rows.any { row ->
                row.text == "Objective updated: Breach The Outpost. You break into the outpost's inner yard."
            },
        )
        assertFalse(model.shell.leftRail.rows.any { row -> row.text == "No active quest summary." })
    }

    @Test
    fun `zone description only appears in map sidebar`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val mapModel =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.MAP),
            )
        val inventoryModel =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState = OverlayState(mode = UiMode.INVENTORY),
            )

        assertTrue(mapModel.sidebar.rows.any { row -> row.text == "A ruined border outpost and the opening stretch of the run." })
        assertFalse(inventoryModel.sidebar.rows.any { row -> row.text == "A ruined border outpost and the opening stretch of the run." })
    }

    @Test
    fun `render model highlights high signal log families with distinct tones`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        logEvents =
                            listOf(
                                RenderLogEventSnapshot(
                                    RenderTextTokenSnapshot(
                                        key = "log.zone.enter",
                                        arguments =
                                            listOf(
                                                RenderTextArgumentSnapshot(name = "zone", valueKey = "zone.shattered_outpost.name"),
                                                RenderTextArgumentSnapshot(name = "desc", valueKey = "zone.shattered_outpost.desc"),
                                            ),
                                    ),
                                ),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.passive.damage_bonus_vs_tag")),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.talent.damage_resisted")),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.talent.damage_vulnerable")),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.level_up")),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.boss.enrage")),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertEquals(
            listOf(
                TileTextTone.CYAN,
                TileTextTone.GREEN,
                TileTextTone.BLUE,
                TileTextTone.RED,
                TileTextTone.GOLD,
                TileTextTone.RED,
            ),
            model.messageLines.map { line -> line.tone },
        )
    }

    @Test
    fun `render model surfaces typed combat feedback for tile overlays`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        combatFeedbackEvents =
                            listOf(
                                CombatFeedbackSnapshot(
                                    targetEntityId = 2,
                                    sourceEntityId = 1,
                                    x = 0,
                                    y = 0,
                                    type = CombatFeedbackTypeSnapshot.DAMAGE,
                                    amount = 24,
                                    damageTypeId = "FIRE",
                                    critical = true,
                                ),
                                CombatFeedbackSnapshot(
                                    targetEntityId = 2,
                                    x = 0,
                                    y = 0,
                                    type = CombatFeedbackTypeSnapshot.STATUS_APPLIED,
                                    statusNameKey = "status.stun",
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertEquals(2, model.combatFeedback.size)
        assertEquals("24!", model.combatFeedback.first().text)
        assertEquals(TileTextTone.GOLD, model.combatFeedback.first().tone)
        assertEquals("+Stunned", model.combatFeedback.last().text)
        assertEquals(1, model.combatFeedback.last().stackIndex)
    }

    @Test
    fun `render canvas offsets combat feedback away from telegraph cells`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    width = 2,
                    overlays =
                        listOf(
                            OverlayRenderSnapshot(
                                id = "telegraph:test",
                                visualKey = "missing_visual",
                                previewTurns = 1,
                                dangerLevel = 2,
                                shape = OverlayShapeSnapshot.SINGLE_TILE,
                                sourceAbilityId = "telegraph.test",
                                cells = listOf(GridPointSnapshot(0, 0)),
                            ),
                        ),
                    combatFeedbackEvents =
                        listOf(
                            CombatFeedbackSnapshot(
                                targetEntityId = 2,
                                sourceEntityId = 1,
                                x = 0,
                                y = 0,
                                type = CombatFeedbackTypeSnapshot.DAMAGE,
                                amount = 24,
                            ),
                        ),
                ),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val feedbackDraw = canvas.textDraws.first { draw -> draw.text == "24" }
        val layout = TileRenderer.layoutMetrics(mapWidth = 2, mapHeight = sampleSnapshot().metadata.height, cellWidth = 32f, cellHeight = 32f)
        assertTrue(feedbackDraw.x > layout.shell.mapBounds.x + 32f)
    }

    @Test
    fun `render model keeps player status turns visible in hud badges`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        playerStatusEffects =
                            listOf(
                                StatusEffectRenderSnapshot(
                                    typeId = "ARMOR_BREAK",
                                    remainingTurns = 3,
                                    nameKey = "status.armor_break",
                                    iconKey = "missing_visual",
                                    stackCount = 2,
                                    stackCap = 3,
                                    category = StatusEffectCategorySnapshot.DEBUFF,
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertEquals(1, model.hud.statusIcons.size)
        assertEquals("2/3 3t", model.hud.statusIcons.single().badgeText)
    }

    @Test
    fun `status badge colors remain distinct for buffs and debuffs`() {
        assertEquals(Color.valueOf("7FE0A0"), StatusHudRenderer.badgeColor(StatusEffectCategorySnapshot.BUFF))
        assertEquals(Color.valueOf("FF9A8D"), StatusHudRenderer.badgeColor(StatusEffectCategorySnapshot.DEBUFF))
    }

    @Test
    fun `loadout edit sidebar shows active slots reserve talents and equip controls`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        talents =
                            listOf(
                                TalentSlotSnapshot(
                                    slot = 1,
                                    talentId = "power_strike",
                                    nameKey = "talent.vanguard.power_strike.name",
                                    level = 1,
                                    maxLevel = 5,
                                    resourceCost = 8,
                                    resourceLabelKey = "ui.hud.stamina.short",
                                    resourceTypeId = "STAMINA",
                                    range = 1,
                                    minRange = 0,
                                    currentCooldown = 0,
                                    maxCooldown = 3,
                                    requiresTarget = false,
                                ),
                            ),
                        reserveTalents =
                            listOf(
                                TalentReserveSnapshot(
                                    talentId = "charge",
                                    nameKey = "talent.vanguard.charge.name",
                                    level = 1,
                                    maxLevel = 5,
                                    resourceCost = 10,
                                    resourceLabelKey = "ui.hud.stamina.short",
                                    resourceTypeId = "STAMINA",
                                    range = 3,
                                    minRange = 1,
                                    currentCooldown = 0,
                                    maxCooldown = 4,
                                    requiresTarget = true,
                                    descKey = "talent.vanguard.charge.desc",
                                    descriptionModel =
                                        DescriptionModelSnapshot(
                                            templateKey = "talent.vanguard.charge.desc",
                                            placeholders =
                                                mapOf(
                                                    "minRange" to DescriptionValueSnapshot.IntValue(1),
                                                    "range" to DescriptionValueSnapshot.IntValue(3),
                                                    "damagePercent" to DescriptionValueSnapshot.IntValue(120),
                                                ),
                                            keywords = listOf("damage", "stun"),
                                        ),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.LOADOUT_EDIT, loadoutSlotSelection = 1, loadoutReserveSelection = 0),
            )

        assertEquals("Loadout", model.sidebar.title)
        assertTrue(model.sidebar.rows.any { row -> row.text == "Active Slots" })
        assertTrue(model.sidebar.rows.any { row -> row.text == "Reserve Talents" })
        assertTrue(model.sidebar.rows.any { row -> row.text.contains("Charge 1/5") })
        assertTrue(model.sidebar.rows.any { row -> row.text.contains("Rush a foe from 1 to 3 tiles away for 120% Damage.") })
        assertTrue(model.sidebar.rows.any { row -> row.text == "1-4 choose slot  W/X move reserve  E equip  F close" })
    }

    @Test
    fun `tile talent sidebar uses shared talent presentation lines and resolves icons`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        talentTrees =
                            listOf(
                                TalentTreeSnapshot(
                                    treeId = "vanguard_arms",
                                    treeOwnerId = "vanguard",
                                    nameKey = "talent_tree.vanguard_arms.name",
                                    descKey = "talent_tree.vanguard_arms.desc",
                                    iconKey = "item.short_sword.icon",
                                    nodes =
                                        listOf(
                                            TalentTreeNodeSnapshot(
                                                talentId = "charge",
                                                treeId = "vanguard_arms",
                                                treeOwnerId = "vanguard",
                                                nameKey = "talent.vanguard.charge.name",
                                                descKey = "talent.vanguard.charge.desc",
                                                iconKey = "item.short_sword.icon",
                                                state = TalentNodeStateSnapshot.LEARNABLE,
                                                rank = 0,
                                                maxRank = 5,
                                                unlockLevel = 1,
                                                resourceCost = 8,
                                                resourceLabelKey = "ui.hud.stamina.short",
                                                range = 3,
                                                minRange = 1,
                                                currentCooldown = 0,
                                                maxCooldown = 4,
                                                requiresTarget = true,
                                            ),
                                            TalentTreeNodeSnapshot(
                                                talentId = "war_cry",
                                                treeId = "vanguard_arms",
                                                treeOwnerId = "vanguard",
                                                nameKey = "talent.vanguard.war_cry.name",
                                                descKey = "talent.vanguard.war_cry.desc",
                                                iconKey = "item.short_sword.icon",
                                                state = TalentNodeStateSnapshot.LOCKED,
                                                rank = 0,
                                                maxRank = 5,
                                                unlockLevel = 2,
                                                resourceCost = 12,
                                                resourceLabelKey = "ui.hud.stamina.short",
                                                range = 0,
                                                minRange = 0,
                                                currentCooldown = 0,
                                                maxCooldown = 6,
                                                requiresTarget = false,
                                            ),
                                        ),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.TALENT_ASSIGN, talentTreeSelection = 0, talentTreePreviewExpanded = false),
            )

        val treeRow = model.sidebar.rows.first { row -> row.text == "Arms" }
        val selectedNodeRow = model.sidebar.rows.first { row -> row.text == "[+] Charge 0/5" }
        val lockedNodeRow = model.sidebar.rows.first { row -> row.text == "[x] War Cry 0/5" }

        assertEquals(TileTextTone.GOLD, treeRow.tone)
        assertNotNull(treeRow.icon)
        assertEquals(TileTextTone.CYAN, selectedNodeRow.tone)
        assertTrue(selectedNodeRow.selected)
        assertNotNull(selectedNodeRow.icon)
        assertEquals(TileTextTone.GRAY, lockedNodeRow.tone)
    }

    @Test
    fun `loadout edit sidebar renders breakpoint preview rows with secondary gray tone`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        reserveTalents =
                            listOf(
                                TalentReserveSnapshot(
                                    talentId = "charge",
                                    nameKey = "talent.vanguard.charge.name",
                                    level = 2,
                                    committedLevel = 1,
                                    maxLevel = 5,
                                    resourceCost = 10,
                                    resourceLabelKey = "ui.hud.stamina.short",
                                    resourceTypeId = "STAMINA",
                                    range = 5,
                                    minRange = 1,
                                    currentCooldown = 0,
                                    maxCooldown = 4,
                                    requiresTarget = true,
                                    descKey = "talent.vanguard.charge.desc",
                                    descriptionModel =
                                        DescriptionModelSnapshot(
                                            templateKey = "talent.vanguard.charge.desc",
                                            placeholders =
                                                mapOf(
                                                    "minRange" to DescriptionValueSnapshot.IntValue(1),
                                                    "range" to DescriptionValueSnapshot.IntValue(5),
                                                    "damagePercent" to DescriptionValueSnapshot.IntValue(130),
                                                ),
                                        ),
                                    nextBreakpointPreview =
                                        TalentBreakpointPreviewSnapshot(
                                            atRank = 5,
                                            model =
                                                DescriptionModelSnapshot(
                                                    templateKey = "talent.breakpoint.apply_status",
                                                    placeholders =
                                                        mapOf(
                                                            "statusDuration" to DescriptionValueSnapshot.IntValue(2),
                                                            "statusId" to
                                                                DescriptionValueSnapshot.StatusValue(
                                                                    statusId = "STUN",
                                                                    nameKey = "status.stun",
                                                                ),
                                                        ),
                                                ),
                                        ),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.LOADOUT_EDIT, loadoutReserveSelection = 0),
            )

        val breakpointHeader = model.sidebar.rows.first { row -> row.text == "Next breakpoint: rank 5." }
        val breakpointEffect = model.sidebar.rows.first { row -> row.text.contains("new status effect for 2 turns") }

        assertEquals(TileTextTone.GRAY, breakpointHeader.tone)
        assertEquals(TileTextTone.GRAY, breakpointEffect.tone)
    }

    @Test
    fun `inspect sidebar uses composed item display name`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val item =
            ItemRenderSnapshot(
                baseItemId = "short_sword",
                nameKey = "item.short_sword.name",
                displayName =
                    RenderTextTokenSnapshot(
                        key = "item.display.composed",
                        arguments =
                            listOf(
                                RenderTextArgumentSnapshot(
                                    name = "quality",
                                    valueToken =
                                        RenderTextTokenSnapshot(
                                            key = "item.display.part.quality",
                                            arguments = listOf(RenderTextArgumentSnapshot(name = "quality", valueKey = "item.quality.rare")),
                                        ),
                                ),
                                RenderTextArgumentSnapshot(name = "prefix1", value = ""),
                                RenderTextArgumentSnapshot(name = "prefix2", value = ""),
                                RenderTextArgumentSnapshot(
                                    name = "material",
                                    valueToken =
                                        RenderTextTokenSnapshot(
                                            key = "item.display.part.material",
                                            arguments = listOf(RenderTextArgumentSnapshot(name = "material", valueKey = "material.mithril.name")),
                                        ),
                                ),
                                RenderTextArgumentSnapshot(name = "suffix1", value = ""),
                                RenderTextArgumentSnapshot(name = "suffix2", value = ""),
                                RenderTextArgumentSnapshot(name = "base", valueKey = "item.short_sword.name"),
                            ),
                ),
                typeId = "WEAPON",
                iconKey = "item.short_sword.icon",
                qualityTierId = "RARE",
            )
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        cells =
                            listOf(
                                MapCellSnapshot(
                                    x = 0,
                                    y = 0,
                                    visibility = CellVisibilitySnapshot.VISIBLE,
                                    terrainTypeId = "floor",
                                    terrainVisualKey = "tileset.test.ground_01",
                                    items = listOf(item),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 0)),
            )

        assertTrue(model.sidebar.rows.any { row -> row.text == "\u25C6\u25C6 Rare Mithril Short Sword" })
    }

    @Test
    fun `inventory sidebar localizes off hand slot and monster tag labels in chinese`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.ZH_CN)
        val item =
            ItemRenderSnapshot(
                baseItemId = "bandit_trophy",
                nameKey = "item.bandit_trophy.name",
                typeId = "ARMOR",
                slotId = "OFF_HAND",
                passiveDescriptions =
                    listOf(
                        RenderTextTokenSnapshot(
                            key = "ui.inspect.passive.damage_vs_tag",
                            arguments =
                                listOf(
                                    RenderTextArgumentSnapshot(name = "amount", value = "10"),
                                    RenderTextArgumentSnapshot(name = "tag", valueKey = "monster.tag.undead"),
                                ),
                        ),
                    ),
            )

        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        inventory =
                            listOf(
                                InventoryEntrySnapshot(
                                    index = 0,
                                    item = item,
                                    equippedSlotId = "OFF_HAND",
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 0),
            )

        val sidebarTexts = model.sidebar.rows.map(TileTextRow::text)
        assertTrue(sidebarTexts.contains("1. 强盗战利徽记 [副手]"))
        assertTrue(sidebarTexts.contains("槽位 副手"))
        assertTrue(sidebarTexts.contains("对亡灵额外造成 +10% 伤害"))
        assertFalse(sidebarTexts.any { text -> "OFF_HAND" in text })
        assertFalse(sidebarTexts.any { text -> "undead" in text })
    }

    private fun assertTextEndsBefore(
        draw: RecordingTileCanvas.TextDraw,
        rightEdge: Float,
    ) {
        val bounds = TileRenderer.textApproximationBounds(draw.style, draw.text, draw.x, draw.y)
        assertTrue(draw.x + bounds[2] <= rightEdge + 0.5f, "${draw.text} exceeds $rightEdge")
    }

    private fun assertAssetInside(
        draw: RecordingTileCanvas.AssetDraw,
        bounds: GameShellBounds,
    ) {
        assertTrue(draw.x >= bounds.x - 0.5f, "${draw.asset.resolvedKey} x=${draw.x} escapes $bounds")
        assertTrue(draw.y >= bounds.y - 0.5f, "${draw.asset.resolvedKey} y=${draw.y} escapes $bounds")
        assertTrue(draw.x + draw.width <= bounds.right + 0.5f, "${draw.asset.resolvedKey} right=${draw.x + draw.width} escapes $bounds")
        assertTrue(draw.y + draw.height <= bounds.top + 0.5f, "${draw.asset.resolvedKey} top=${draw.y + draw.height} escapes $bounds")
    }

    private fun assertRightPanelSlotDraw(
        canvas: RecordingTileCanvas,
        slotBounds: GameShellBounds,
    ) {
        assertTrue(
            canvas.assetDraws.any { draw ->
                draw.asset.resolvedKey in setOf(DarkUiChromeVisualKeys.SLOT_EMPTY, DarkUiChromeVisualKeys.SLOT_EQUIPPED) &&
                    draw.x == slotBounds.x &&
                    draw.y == slotBounds.y &&
                    draw.width == slotBounds.width &&
                    draw.height == slotBounds.height
            },
            "missing right-panel slot draw at $slotBounds",
        )
    }

    private fun inscriptionRowSlotBounds(
        sectionBounds: GameShellBounds,
        grid: DemoSlotGridLayout,
        index: Int,
        slotBounds: GameShellBounds,
    ): GameShellBounds {
        val columnGap = 10f
        val rowWidth = ((sectionBounds.width - 24f - columnGap) / 2f).coerceAtLeast(grid.slotSide + 48f)
        val leftX = sectionBounds.x + 12f
        val rightX = sectionBounds.right - 12f - rowWidth
        val rowX = if (index % 2 == 0) leftX else rightX
        return GameShellBounds(
            x = rowX + 6f,
            y = slotBounds.y,
            width = slotBounds.width,
            height = slotBounds.height,
        )
    }

    private fun assertAssetOrder(
        canvas: RecordingTileCanvas,
        vararg keys: String,
    ) {
        val indices = keys.map { key -> canvas.assetDraws.indexOfFirst { draw -> draw.asset.resolvedKey == key } }
        assertTrue(indices.all { index -> index >= 0 }, "Missing draw in order check: ${keys.toList()} -> $indices")
        assertEquals(indices.sorted(), indices, "Unexpected draw order for ${keys.toList()}")
    }

    private fun expectedShellRegion(
        layout: TileLayoutMetrics,
        key: String,
    ): GameShellBounds =
        when (key) {
            DarkUiChromeVisualKeys.SHELL_OUTER_FRAME -> layout.demoShell.outerFrame
            DarkUiChromeVisualKeys.SHELL_MAP_STAGE_FRAME -> layout.demoShell.mapStage
            DarkUiChromeVisualKeys.SHELL_MAP_STAGE_BACKDROP -> layout.demoShell.mapStage
            DarkUiChromeVisualKeys.SHELL_NAV_RAIL_FRAME,
            DarkUiChromeVisualKeys.SHELL_NAV_BUTTON_ACTIVE,
            DarkUiChromeVisualKeys.SHELL_NAV_COMPASS,
            DarkUiChromeVisualKeys.SHELL_NAV_BAG,
            DarkUiChromeVisualKeys.SHELL_NAV_SCROLL,
            DarkUiChromeVisualKeys.SHELL_NAV_BOOK,
            DarkUiChromeVisualKeys.SHELL_NAV_GEAR -> layout.demoShell.navRail
            DarkUiChromeVisualKeys.SHELL_RIGHT_SECTION_DIVIDER -> layout.demoShell.rightPanel
            DarkUiChromeVisualKeys.SHELL_HERO_CARD_FRAME,
            DarkUiChromeVisualKeys.SHELL_HERO_CREST_PLACEHOLDER -> layout.demoShell.bottomDeck.heroCard
            DarkUiChromeVisualKeys.SHELL_ACTION_DECK_FRAME -> layout.demoShell.bottomDeck.actionDeck
            DarkUiChromeVisualKeys.SHELL_COMMAND_HINT_PLATE -> layout.demoShell.rightPanelLayout.operationHints
            DarkUiChromeVisualKeys.SHELL_LOG_DECK_FRAME -> layout.demoShell.bottomDeck.logDeck
            else -> error("Unexpected shell key $key")
        }

    private fun sampleResolver(): VisualManifestResolver =
        VisualManifestResolver(
            manifest =
                VisualManifest(
                    manifestVersion = 1,
                    styleTag = "test-style",
                    fallbackKey = "missing_visual",
                    entries =
                        listOf(
                            VisualManifestEntry(
                                key = "missing_visual",
                                category = "debug",
                                rawOutputPath = "debug/missing_visual.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "tileset.test.ground_01",
                                category = "tile_ground",
                                rawOutputPath = "phase2/p2-b/tileset_ruins_ground_01.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "actor.vanguard",
                                category = "actor_sprite",
                                rawOutputPath = "phase2/p2-b/actor_vanguard.png",
                                footprint = "2x1",
                            ),
                            VisualManifestEntry(
                                key = "item.short_sword.icon",
                                category = "item_icon",
                                rawOutputPath = "phase2/p2-b/icon_item_short_sword.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "ui.empty.inventory.icon",
                                category = "icon",
                                rawOutputPath = "phase4/uiux_pr03/ui_empty_inventory_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "ui.empty.shop.icon",
                                category = "icon",
                                rawOutputPath = "phase4/uiux_pr03/ui_empty_shop_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "ui.empty.inspect.icon",
                                category = "icon",
                                rawOutputPath = "phase4/uiux_pr03/ui_empty_inspect_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "ui.card.shop.header.icon",
                                category = "icon",
                                rawOutputPath = "phase4/uiux_pr03/ui_card_shop_header_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "ui.card.reward.header.icon",
                                category = "icon",
                                rawOutputPath = "phase4/uiux_pr03/ui_card_reward_header_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = CombatAffordanceResourceKeys.ACTION_ICON,
                                category = "icon",
                                rawOutputPath = "dark-v1/ui/ui_combat_action_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = CombatAffordanceResourceKeys.METHOD_ICON,
                                category = "icon",
                                rawOutputPath = "dark-v1/ui/ui_combat_method_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = CombatAffordanceResourceKeys.TARGET_ICON,
                                category = "icon",
                                rawOutputPath = "dark-v1/ui/ui_combat_target_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = CombatAffordanceResourceKeys.LOCK_ICON,
                                category = "icon",
                                rawOutputPath = "dark-v1/ui/ui_combat_lock_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = CombatAffordanceResourceKeys.INVALID_ICON,
                                category = "icon",
                                rawOutputPath = "dark-v1/ui/ui_combat_invalid_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "vfx.boss.variant.molten_glass",
                                category = "vfx_overlay",
                                rawOutputPath = "phase4/pr06/boss_variant_molten_glass.png",
                                footprint = "1x1",
                                tintColorHex = "#FF7A3C",
                            ),
                        ) +
                            DarkUiChromeVisualKeys.pr02Round1OwnerKeys
                                .plus(DarkUiChromeVisualKeys.pr02_1DemoShellOwnerKeys)
                                .filterNot { key ->
                                    key in
                                        setOf(
                                            CombatAffordanceResourceKeys.ACTION_ICON,
                                            CombatAffordanceResourceKeys.METHOD_ICON,
                                            CombatAffordanceResourceKeys.TARGET_ICON,
                                            CombatAffordanceResourceKeys.LOCK_ICON,
                                            CombatAffordanceResourceKeys.INVALID_ICON,
                                        )
                                }.map(::darkUiManifestEntry),
                    prefixRules = listOf(ManifestPrefixRule(prefix = "icon.", targetKey = "missing_visual")),
                ),
            logSink = ManifestLogSink { error("Unexpected manifest fallback: $it") },
        )

    private fun darkUiManifestEntry(key: String): VisualManifestEntry =
        VisualManifestEntry(
            key = key,
            category =
                if (key.startsWith("ui.frame.") || key.contains(".frame") || key.contains(".plate") || key.contains(".divider")) {
                    "ui_frame"
                } else {
                    "icon"
                },
            rawOutputPath = "dark-v1/ui/${key.replace('.', '_')}.png",
            footprint = "ui",
        )

    private fun sampleSnapshot(
        width: Int = 1,
        height: Int = 1,
        cells: List<MapCellSnapshot>? = null,
        overlays: List<OverlayRenderSnapshot> = emptyList(),
        playerStatusEffects: List<StatusEffectRenderSnapshot> = emptyList(),
        talents: List<TalentSlotSnapshot> = emptyList(),
        inscriptions: List<InscriptionSlotSnapshot> = emptyList(),
        equipment: List<EquipmentSlotSnapshot> = emptyList(),
        reserveTalents: List<TalentReserveSnapshot> = emptyList(),
        talentTrees: List<TalentTreeSnapshot> = emptyList(),
        inventory: List<InventoryEntrySnapshot> = emptyList(),
        targetablePositions: List<GridPointSnapshot> = listOf(GridPointSnapshot(0, 0)),
        logEvents: List<RenderLogEventSnapshot> = emptyList(),
        combatFeedbackEvents: List<CombatFeedbackSnapshot> = emptyList(),
    ): RenderSnapshot =
        RenderSnapshot(
            metadata =
                RenderMetadataSnapshot(
                    revision = 1,
                    zoneId = "shattered_outpost",
                    zoneNameKey = "zone.shattered_outpost.name",
                    zoneDescKey = "zone.shattered_outpost.desc",
                    currentFloor = 1,
                    maxFloor = 2,
                    width = width,
                    height = height,
                    playerX = 0,
                    playerY = 0,
                    zoneVisualKey = "zone.shattered_outpost.visual",
                    zoneAudioProfile = "audio.zone.shattered_outpost",
                    tilesetKey = "tileset.test",
                    ambientProfile = "ambient.shattered_outpost",
                ),
            mapCells =
                cells ?:
                    (0 until width).flatMap { x ->
                        (0 until height).map { y ->
                            MapCellSnapshot(
                                x = x,
                                y = y,
                                visibility = CellVisibilitySnapshot.VISIBLE,
                                terrainTypeId = "floor",
                                terrainVisualKey = "tileset.test.ground_01",
                            )
                        }
                    },
            overlays = overlays,
            actors =
                listOf(
                    ActorRenderSnapshot(
                        entityId = 1,
                        x = 0,
                        y = 0,
                        visualKey = "actor.vanguard",
                        nameKey = "actor.player.name",
                        isPlayer = true,
                        roleKind = ActorRoleKindSnapshot.PLAYER,
                        currentHp = 24,
                        maxHp = 24,
                        attack = 7,
                        defense = 5,
                        accuracy = 6,
                        evasion = 4,
                        speed = 100,
                        strength = 12,
                        dexterity = 8,
                        constitution = 11,
                        willpower = 7,
                        statusEffects = playerStatusEffects,
                    ),
                ),
            uiState =
                RenderUiStateSnapshot(
                    playerStatus =
                        PlayerStatusSnapshot(
                            currentHp = 24,
                            maxHp = 24,
                            currentResource = 12,
                            maxResource = 12,
                            resourceLabelKey = "ui.hud.stamina.short",
                            resourceTypeId = "STAMINA",
                            level = 1,
                            currentExperience = 0,
                            nextLevelRequirement = 12,
                            statPoints = 0,
                            talentPoints = 0,
                            attack = 7,
                            defense = 5,
                            accuracy = 6,
                            evasion = 4,
                            speed = 100,
                    ),
                    equipment = equipment,
                    talents = talents,
                    inscriptions = inscriptions,
                    reserveTalents = reserveTalents,
                    talentTrees = talentTrees,
                    inventory = inventory,
                    targetablePositions = targetablePositions,
                ),
            logEvents = logEvents,
            combatFeedbackEvents = combatFeedbackEvents,
        )

    private fun validationPanel(cursor: com.ktome.core.map.Point): ValidationOverlayPanelState =
        ValidationOverlayPanelState(
            summary =
                ValidationSummarySnapshot(
                    preset = ValidationPreset.CUSTOM,
                    seed = 1L,
                    seedCorpus = listOf(1L),
                    zoneId = "shattered_outpost",
                    floor = 1,
                    activePackIds = emptyList(),
                    bossVariantModeId = "none",
                    preferredBossVariantId = null,
                    lastResult = null,
                ),
            zoneNameKey = "zone.shattered_outpost.name",
            inspectCursor = cursor,
            phase4Guide =
                ValidationPhase4Guide(
                    targetLabelKeys = emptyList(),
                    quickPathLabelKeys = emptyList(),
                    evidenceLabelKeys = emptyList(),
                ),
            scenarioContext = null,
            sections = emptyList(),
        )
}

private class RecordingTileCanvas : TileCanvas {
    data class AssetDraw(
        val asset: ResolvedVisualAsset,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val alpha: Float,
        val tintColorHex: String?,
    )

    data class RectDraw(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val color: Color,
    )

    data class TextDraw(
        val style: TileTextStyle,
        val text: String,
        val x: Float,
        val y: Float,
        val color: Color,
    )

    val assetDraws = mutableListOf<AssetDraw>()
    val rectDraws = mutableListOf<RectDraw>()
    val textDraws = mutableListOf<TextDraw>()
    val flushes = mutableListOf<TileLayerFlushReason>()

    override fun drawRect(draw: TileRectDraw) {
        rectDraws +=
            RectDraw(
                draw.bounds.x,
                draw.bounds.y,
                draw.bounds.width,
                draw.bounds.height,
                Color(draw.color),
            )
    }

    override fun drawAsset(draw: TileAssetDraw) {
        assetDraws +=
            AssetDraw(
                draw.asset,
                draw.bounds.x,
                draw.bounds.y,
                draw.bounds.width,
                draw.bounds.height,
                draw.alpha,
                draw.tintColorHex,
            )
    }

    override fun drawText(draw: TileTextDraw) {
        textDraws +=
            TextDraw(
                draw.style,
                draw.text,
                draw.position.x,
                draw.position.y,
                Color(draw.color),
            )
    }

    override fun flushLayer(reason: TileLayerFlushReason) {
        flushes += reason
    }
}
