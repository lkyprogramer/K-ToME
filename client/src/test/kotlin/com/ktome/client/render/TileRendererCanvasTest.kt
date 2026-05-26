package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.client.assets.DarkUiChromeTestKeys
import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.ManifestLogSink
import com.ktome.client.assets.ManifestPrefixRule
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.ShopOfferTagTokens
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.ui.chrome.ChromeFrameBounds
import com.ktome.client.ui.chrome.ChromeFramePainter
import com.ktome.client.ui.chrome.ChromeSurfaceKind
import com.ktome.client.input.OverlayState
import com.ktome.client.input.ShopFocus
import com.ktome.client.input.UiMode
import com.ktome.client.input.ValidationOverlayActionState
import com.ktome.client.input.ValidationOverlayPanelState
import com.ktome.client.input.ValidationOverlaySectionState
import com.ktome.client.render.layout.DemoSlotGridLayout
import com.ktome.client.render.layout.GameShellBounds
import com.ktome.client.render.layout.InventoryWorkbenchLayoutRequest
import com.ktome.client.render.layout.InventoryWorkbenchLayoutSolver
import com.ktome.client.ui.combat.CombatAffordanceResourceKeys
import com.ktome.client.ui.combat.CombatDecisionFrame
import com.ktome.client.ui.combat.CombatDecisionFrameState
import com.ktome.client.ui.combat.CombatDecisionPhase
import com.ktome.client.ui.UiCompanionVisualKeys
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
import com.ktome.core.snapshot.InscriptionReplacementCategoryChangeSnapshot
import com.ktome.core.snapshot.InscriptionReplacementEntrySnapshot
import com.ktome.core.snapshot.InscriptionReplacementPromptSnapshot
import com.ktome.core.snapshot.InscriptionSlotSnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.ItemStatModifierSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RewardPresentationEntrySnapshot
import com.ktome.core.snapshot.RewardPresentationSourceSnapshot
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.ShopOfferSnapshot
import com.ktome.core.snapshot.ShopPanelSnapshot
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot
import com.ktome.core.snapshot.TalentNodeStateSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.snapshot.TalentTreeNodeSnapshot
import com.ktome.core.snapshot.TalentTreeSnapshot
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.game.GameModule
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.validation.ValidationPhase4Guide
import com.ktome.game.validation.ValidationOverlaySection
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationScenarioRegistry
import com.ktome.game.validation.ValidationSessionRequest
import com.ktome.game.validation.ValidationSummarySnapshot
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.math.abs

class TileRendererCanvasTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `dark uiux demo map stage keeps visible room mass centered`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr02-1-demo-shell-foundation"))
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr02-1-composition")),
                    options = scenario.toSessionOptions(),
                ),
            )
        val snapshot = session.renderSnapshot()

        val diagnostics =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = ClientAssetBundleLoader.load(logSink = ManifestLogSink { }).visualResolver,
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = NoOpTileCanvas,
                cellWidth = 32f,
                cellHeight = 32f,
                shellWorldWidth = 1280f,
                shellWorldHeight = 800f,
            )
        val visibleRects =
            snapshot.mapCells
                .filter { cell -> cell.visibility == CellVisibilitySnapshot.VISIBLE }
                .filter { cell -> diagnostics.viewport.containsTile(Point(cell.x, cell.y)) }
                .map { cell -> diagnostics.viewport.tileRect(Point(cell.x, cell.y)) }
        val visibleLeft = visibleRects.minOf { rect -> rect.x }
        val visibleRight = visibleRects.maxOf { rect -> rect.x + rect.width }
        val visibleCenter = (visibleLeft + visibleRight) / 2
        val mapCenter = diagnostics.viewport.mapBounds.x + diagnostics.viewport.mapBounds.width / 2
        val maxCenterDrift = diagnostics.viewport.cellSize * 2

        assertTrue(
            abs(visibleCenter - mapCenter) <= maxCenterDrift,
            "PR02-1 map stage composition drifted too far from center: visibleCenter=$visibleCenter, mapCenter=$mapCenter, maxCenterDrift=$maxCenterDrift, visibleLeft=$visibleLeft, visibleRight=$visibleRight, mapBounds=${diagnostics.viewport.mapBounds}.",
        )
    }

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
    fun `render canvas grounds actor sprites with compact floor contact shadows`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(width = 5, height = 5, playerX = 2, playerY = 2),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val actorDraw = canvas.assetDraws.single { draw -> draw.asset.resolvedKey == "actor.vanguard" }
        val actorCenterX = actorDraw.x + actorDraw.width / 2f
        val actorFootY = actorDraw.y + actorDraw.height * 0.80f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 44f..46f &&
                    draw.height in 5f..7f &&
                    draw.color.a.isNear(0.18f) &&
                    draw.contains(actorCenterX, actorFootY)
            },
            "actor sprites should receive a compact dark floor contact shadow so they feel grounded in the dungeon tile, not pasted over the map",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 31f..34f &&
                    draw.height in 1f..3f &&
                    draw.color.a.isNear(0.055f) &&
                    draw.contains(actorCenterX, actorFootY)
            },
            "actor contact shadows should include a faint worn-stone edge so the grounding reads as material interaction rather than a flat black badge",
        )
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
    fun `demo nav rail reads as icon first forged command rail`() {
        val canvas = RecordingTileCanvas()
        val snapshot = sampleSnapshot(height = 6)

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val layout = TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, 32f, 32f)
        val navRail = layout.demoShell.navRail
        val buttonBounds = DemoNavRailButtonLayout.resolve(navRail, itemCount = 5)
        val selectedBounds = buttonBounds.first()
        val navIcons =
            listOf(
                DarkUiChromeVisualKeys.SHELL_NAV_COMPASS,
                DarkUiChromeVisualKeys.SHELL_NAV_BAG,
                DarkUiChromeVisualKeys.SHELL_NAV_SCROLL,
                DarkUiChromeVisualKeys.SHELL_NAV_BOOK,
                DarkUiChromeVisualKeys.SHELL_NAV_GEAR,
            ).map { key -> canvas.assetDraws.single { draw -> draw.asset.resolvedKey == key } }

        assertTrue(
            navIcons.all { draw -> draw.width >= 31f && draw.height >= 31f },
            "left nav icons should be large enough to read as primary icon-first controls at first glance",
        )

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 18f..30f &&
                    draw.height > navRail.height * 0.72f &&
                    draw.color.a.isNear(0.32f) &&
                    draw.x >= navRail.x &&
                    draw.x + draw.width <= navRail.right &&
                    draw.y >= navRail.y &&
                    draw.y + draw.height <= navRail.top
            },
            "nav rail should have a dark forged backbone so the icons do not look pasted onto an empty vertical strip",
        )

        val shelfBars =
            canvas.rectDraws.filter { draw ->
                draw.width > selectedBounds.width &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.145f) &&
                    draw.x >= navRail.x &&
                    draw.x + draw.width <= navRail.right &&
                    draw.y >= navRail.y &&
                    draw.y + draw.height <= navRail.top
            }
        assertTrue(
            shelfBars.size >= buttonBounds.size,
            "each nav icon should sit on a subtle forged shelf so the rail reads as authored equipment UI, not isolated buttons",
        )

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > selectedBounds.width &&
                    draw.height > selectedBounds.height &&
                    draw.color.a.isNear(0.08f) &&
                    draw.contains(selectedBounds.x + selectedBounds.width / 2f, selectedBounds.y + selectedBounds.height / 2f)
            },
            "selected nav item should gain a restrained cyan halo that anchors the active pane without adding text labels",
        )
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
    fun `inventory root frame shows selected item detail and comparison without entering detail frame`() {
        val equipped =
            ItemRenderSnapshot(
                baseItemId = "worn_sword",
                nameKey = "item.long_sword.name",
                typeId = "WEAPON",
                slotId = "WEAPON",
                stats = ItemStatModifierSnapshot(attack = 1, defense = 1),
            )
        val candidate =
            ItemRenderSnapshot(
                baseItemId = "hunter_bow",
                nameKey = "item.hunter_bow.name",
                typeId = "WEAPON",
                slotId = "WEAPON",
                stats = ItemStatModifierSnapshot(attack = 4),
            )
        val overlayState =
            OverlayState(
                mode = UiMode.INVENTORY,
                modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY)),
                inventorySelection = 3,
                hoveredInventoryCell = InventoryWorkbenchCellCoordinate(column = 2, row = 0),
            )
        val snapshot =
            sampleSnapshot(
                equipment = listOf(EquipmentSlotSnapshot(slotId = "WEAPON", item = equipped)),
                inventory = listOf(InventoryEntrySnapshot(index = 3, item = candidate)),
            )
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = overlayState,
            )
        val diagnostics =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = overlayState,
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )

        assertEquals("Inventory", model.sidebar.title)
        assertTrue(model.sidebar.rows.take(6).any { row -> row.text.contains("Hunter Bow") })
        assertTrue(model.sidebar.rows.any { row -> row.text == "Equipped: Long Sword" })
        assertTrue(model.sidebar.rows.any { row -> row.text == "ATK +3" && row.tone == TileTextTone.GREEN })
        assertTrue(model.sidebar.rows.any { row -> row.text == "DEF -1" && row.tone == TileTextTone.RED })
        val workbench = requireNotNull(model.inventoryWorkbench)
        assertEquals(6, workbench.grid.columns)
        assertEquals(4, workbench.grid.rows)
        assertTrue(workbench.selectedItemTitle.contains("Hunter Bow"))
        assertTrue(workbench.compareRows.any { row -> row.statId == "attack" && row.deltaValue == "+3" && row.tone == TileTextTone.GREEN })
        assertTrue(workbench.compareRows.any { row -> row.statId == "defense" && row.deltaValue == "-1" && row.tone == TileTextTone.RED })
        assertEquals(InventoryWorkbenchCellCoordinate(column = 2, row = 0), workbench.grid.hoveredCell)
        assertTrue(workbench.grid.cells.first { cell -> cell.coordinate == InventoryWorkbenchCellCoordinate(column = 2, row = 0) }.hovered)
        val activeModal = requireNotNull(diagnostics.overlayFrame.overlayModel.activeModal)
        assertNotNull(activeModal.inventoryWorkbench)
        assertNull(diagnostics.overlayFrame.overlayModel.selectedTooltip)
    }

    @Test
    fun `compact inventory workbench clamps detail rows and keeps action footer keys visible`() {
        val candidate =
            ItemRenderSnapshot(
                baseItemId = "hunter_bow",
                nameKey = "item.hunter_bow.name",
                typeId = "WEAPON",
                iconKey = "item.hunter_bow.icon",
                slotId = "WEAPON",
                qualityTierId = "RARE",
                qualityNameKey = "item.quality.rare",
                affixNameKeys =
                    listOf(
                        "affix.sharp.name",
                        "affix.sturdy.name",
                        "affix.swift.name",
                        "affix.of_strength.name",
                    ),
                stats = ItemStatModifierSnapshot(attack = 4, defense = 3, accuracy = 2),
            )
        val snapshot =
            sampleSnapshot(
                inventory = listOf(InventoryEntrySnapshot(index = 0, item = candidate)),
            )
        val overlayState =
            OverlayState(
                mode = UiMode.INVENTORY,
                modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY)),
                inventorySelection = 0,
            )
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = overlayState,
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
            shellWorldWidth = 1024f,
            shellWorldHeight = 768f,
        )

        val layout =
            InventoryWorkbenchLayoutSolver.resolve(
                InventoryWorkbenchLayoutRequest(viewportWidth = 1024, viewportHeight = 768),
            )
        val detailTexts =
            canvas.textDraws
                .filter { draw -> draw.x >= layout.detailColumn.x && draw.x <= layout.detailColumn.right }
                .map(RecordingTileCanvas.TextDraw::text)
        assertTrue(detailTexts.contains("Slot Weapon"))
        assertTrue(detailTexts.contains("Quality Rare"))
        assertTrue(detailTexts.contains("Affix Sharp"))
        assertTrue(detailTexts.contains("Affix Sturdy"))
        assertTrue(detailTexts.contains("Affix Swift"))
        assertFalse(detailTexts.contains("Affix of Strength"))

        val actionKeyTexts =
            canvas.textDraws
                .filter { draw -> draw.x >= layout.detailColumn.x && draw.x <= layout.detailColumn.right && draw.y >= layout.detailColumn.y && draw.y <= layout.detailColumn.top }
                .map(RecordingTileCanvas.TextDraw::text)
        listOf("Enter", "E", "D", "Esc").forEach { keyText ->
            assertTrue(actionKeyTexts.contains(keyText), "Missing compact action key $keyText")
        }

        val footerKeyTexts =
            canvas.textDraws
                .filter { draw -> draw.x >= layout.footer.x && draw.x <= layout.footer.right && draw.y >= layout.footer.y && draw.y <= layout.footer.top }
                .map(RecordingTileCanvas.TextDraw::text)
        listOf("Arrows", "Enter", "E", "D", "PgUp/PgDn", "Esc").forEach { keyText ->
            assertTrue(footerKeyTexts.contains(keyText), "Missing compact footer key $keyText")
        }
    }

    @Test
    fun `right panel equipment and inscription hover produce detail tooltip models`() {
        val weapon =
            ItemRenderSnapshot(
                baseItemId = "hunter_bow",
                nameKey = "item.hunter_bow.name",
                typeId = "WEAPON",
                slotId = "WEAPON",
                stats = ItemStatModifierSnapshot(attack = 3),
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
                            cooldownRemaining = 2,
                            maxCooldown = 12,
                            requiresTarget = true,
                        ),
                    ),
            )

        val equipmentDiagnostics =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.MAP, hoveredEquipmentSlotId = "WEAPON"),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )
        val equipmentTooltip = requireNotNull(equipmentDiagnostics.overlayFrame.overlayModel.selectedTooltip)
        assertTrue(equipmentTooltip.titleLine.text.contains("Hunter Bow"))
        assertTrue(equipmentTooltip.bodyLines.any { line -> line.text == "ATK +3" })

        val inscriptionDiagnostics =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.MAP, hoveredInscriptionHotkey = 5),
                canvas = RecordingTileCanvas(),
                cellWidth = 32f,
                cellHeight = 32f,
            )
        val inscriptionTooltip = requireNotNull(inscriptionDiagnostics.overlayFrame.overlayModel.selectedTooltip)
        assertTrue(inscriptionTooltip.titleLine.text.contains("Phase Door"))
        assertTrue(inscriptionTooltip.bodyLines.any { line -> line.text == "Cooldown: 2/12" && line.tone == TileTextTone.RED })
        assertTrue(inscriptionTooltip.bodyLines.any { line -> line.text == "Requires target" })
    }

    @Test
    fun `demo shell inventory operations show drop shortcut`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
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
                                    item =
                                        ItemRenderSnapshot(
                                            baseItemId = "long_sword",
                                            nameKey = "item.long_sword.name",
                                            typeId = "WEAPON",
                                        ),
                                ),
                            ),
                    ),
                overlayState =
                    OverlayState(
                        mode = UiMode.INVENTORY,
                        modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY)),
                        inventorySelection = 0,
                    ),
            )

        assertTrue(model.shell.demo.operationHints.any { hint -> hint == localizer.text("ui.controls.inventory") })
        assertTrue(model.shell.demo.operationRows.any { row -> row.text.contains("D drop") })
    }

    @Test
    fun `demo shell validation operations expose selected action list`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(),
                overlayState =
                    OverlayState(
                        mode = UiMode.VALIDATION,
                        validationPanel = validationPanelWithActions(com.ktome.core.map.Point(1, 1)),
                    ),
            )
        val operationText = model.shell.demo.operationRows.joinToString("\n") { row -> row.text }

        assertEquals(localizer.text("ui.sidebar.validation"), model.shell.demo.rightOperationHintsTitle)
        assertTrue(operationText.contains(localizer.text("ui.controls.validation")), operationText)
        assertTrue(operationText.contains(localizer.text("ui.validation.action.phase4_v4.show-evidence-summary")), operationText)
        assertFalse(operationText.contains(localizer.text("ui.controls.map.inventory")), operationText)
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
        DarkUiChromeTestKeys.pr02_1DemoShellOwnerKeys.forEach { key ->
            assertTrue(canvas.assetDraws.any { draw -> draw.asset.resolvedKey == key }, "$key missing")
        }
        assertAssetOrder(
            canvas,
            DarkUiChromeVisualKeys.SHELL_MAP_STAGE_FRAME,
            DarkUiChromeVisualKeys.SHELL_MAP_STAGE_BACKDROP,
            "tileset.test.ground_01",
            "actor.vanguard",
        )
        val backdrop = canvas.assetDraws.single { draw -> draw.asset.resolvedKey == DarkUiChromeVisualKeys.SHELL_MAP_STAGE_BACKDROP }
        assertTrue(backdrop.alpha >= 0.9f, "map stage backdrop must carry visible dungeon fog instead of being black-scrimmed away")
        canvas.assetDraws
            .filter { draw -> draw.asset.resolvedKey.startsWith("ui.shell.") }
            .forEach { draw ->
                val expected = expectedShellRegion(layout, draw.asset.resolvedKey)
                assertAssetInside(draw, expected)
            }
    }

    @Test
    fun `render canvas layers irregular darkness into map stage backdrop`() {
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

        val stage = summary.viewport.mapBounds

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > stage.width * 0.22f &&
                    draw.height > stage.height * 0.24f &&
                    draw.color.a.isNear(0.138f) &&
                    draw.x < stage.x + stage.width * 0.24f &&
                    draw.y > stage.y + stage.height * 0.50f
            },
            "map stage backdrop should include an upper-left irregular vault veil so hidden darkness reads as layered dungeon depth rather than a flat rectangular scrim",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > stage.width * 0.20f &&
                    draw.height > stage.height * 0.18f &&
                    draw.color.a.isNear(0.126f) &&
                    draw.x > stage.x + stage.width * 0.52f &&
                    draw.y < stage.y + stage.height * 0.36f
            },
            "map stage backdrop should include a lower-right irregular void veil so the playable room is surrounded by asymmetric dungeon darkness instead of a balanced rectangular stage",
        )
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
        val rightPanelSlotDraws =
            canvas.assetDraws.filter { draw ->
                draw.asset.resolvedKey in setOf(
                    DarkUiChromeVisualKeys.SLOT_EMPTY,
                    DarkUiChromeVisualKeys.SLOT_EQUIPPED,
                    DarkUiChromeVisualKeys.SLOT_SELECTED,
                ) && draw.x >= right.equipment.x
            }
        assertTrue(rightPanelSlotDraws.isNotEmpty())
        assertTrue(rightPanelSlotDraws.all { draw -> draw.alpha >= 0.94f }, "right panel slots must keep readable material frames")
        val visualSocketGlyphs =
            canvas.rectDraws.count { draw ->
                draw.color.a.isNear(0.72f) &&
                    draw.x >= right.equipment.x &&
                    draw.x + draw.width <= right.equipment.right &&
                    draw.y >= right.equipment.y &&
                    draw.y + draw.height <= right.equipment.top
            }
        assertTrue(visualSocketGlyphs >= 5, "visual-only equipment sockets should read as forged placeholders instead of blank holes")
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.34f) &&
                    draw.width > right.equipment.width * 0.48f &&
                    draw.height > right.equipment.height * 0.54f &&
                    draw.x >= right.equipment.x + 24f &&
                    draw.x + draw.width <= right.equipment.right - 24f &&
                    draw.y >= right.equipment.y &&
                    draw.y + draw.height <= right.equipment.top
            },
            "equipment section should use a broad forged rig backdrop so sockets read as one character panel, not floating icons",
        )
        assertTrue(right.inscriptionSlots.slotSide >= 42f)
        right.inscriptionSlots.slotBounds.forEachIndexed { index, slotBounds ->
            assertRightPanelSlotDraw(canvas, inscriptionRowSlotBounds(right.inscriptions, right.inscriptionSlots, index, slotBounds))
        }
        assertTrue(
            canvas.rectDraws.count { draw ->
                draw.color.a.isNear(0.50f) &&
                    draw.width == 3f &&
                    draw.height >= right.inscriptionSlots.slotSide &&
                    draw.x >= right.inscriptions.x &&
                    draw.x + draw.width <= right.inscriptions.right &&
                    draw.y >= right.inscriptions.y &&
                    draw.y + draw.height <= right.inscriptions.top
            } >= 4,
            "inscription rows should keep a compact forged rail so the two-column list scans as deliberate equipment UI",
        )
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
        val operationFullRows =
            canvas.rectDraws.count { draw ->
                draw.color.a.isNear(0.72f) &&
                    draw.width > right.operationHints.width * 0.70f &&
                    draw.height in 14f..16f &&
                    draw.x >= right.operationHints.x &&
                    draw.x + draw.width <= right.operationHints.right &&
                    draw.y >= right.operationHints.y &&
                    draw.y + draw.height <= right.operationHints.top
            }
        assertEquals(
            0,
            operationFullRows,
            "operation hints should render as a compact command matrix, not oversized full-width text rows",
        )
        val operationKeyChips =
            canvas.rectDraws.count { draw ->
                draw.color.a.isNear(0.46f) &&
                    draw.width in 30f..96f &&
                    draw.height in 12f..15f &&
                    draw.x >= right.operationHints.x &&
                    draw.x + draw.width <= right.operationHints.right &&
                    draw.y >= right.operationHints.y &&
                    draw.y + draw.height <= right.operationHints.top
            }
        assertTrue(operationKeyChips >= 4, "operation hints should expose shortcuts as low-emphasis key chips for scanability")
        assertTrue(
            canvas.textDraws.any { draw ->
                draw.text == "Ctrl+S" &&
                    draw.color.a <= 0.78f &&
                    draw.x >= right.operationHints.x &&
                    draw.x <= right.operationHints.right
            },
            "operation hint shortcut text should stay readable without using full-strength gold emphasis",
        )
        assertFalse(
            canvas.textDraws.any { draw -> draw.text.contains("Ctrl+S save") && draw.x >= right.operationHints.x && draw.x <= right.operationHints.right },
            "operation hint labels should be split from shortcuts so command text does not dominate the panel",
        )

        val crest = canvas.assetDraws.single { draw -> draw.asset.resolvedKey == DarkUiChromeVisualKeys.SHELL_HERO_CREST_PLACEHOLDER }
        assertTrue(crest.width >= 96f)
        assertTrue(crest.height >= 96f)
        assertAssetInside(crest, layout.demoShell.bottomDeck.heroCard)
    }

    @Test
    fun `right panel empty slots read as hollow equipment sockets`() {
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

        val right = TileRenderer.layoutMetrics(mapWidth = 1, mapHeight = 1, cellWidth = 32f, cellHeight = 32f).demoShell.rightPanelLayout
        val inscriptionSlots =
            right.inscriptionSlots.slotBounds.mapIndexed { index, slotBounds ->
                inscriptionRowSlotBounds(right.inscriptions, right.inscriptionSlots, index, slotBounds)
            }
        val emptySlotBounds = right.equipmentSlots.slotBounds + inscriptionSlots + right.backpackSlots.slotBounds
        val hollowCenters =
            emptySlotBounds.count { slotBounds ->
                canvas.rectDraws.any { draw ->
                    (draw.color.a.isNear(0.31f) || draw.color.a.isNear(0.43f)) &&
                        draw.width >= slotBounds.width * 0.48f &&
                        draw.width <= slotBounds.width * 0.72f &&
                        draw.height >= slotBounds.height * 0.48f &&
                        draw.height <= slotBounds.height * 0.72f &&
                        draw.x >= slotBounds.x + slotBounds.width * 0.14f &&
                        draw.x + draw.width <= slotBounds.right - slotBounds.width * 0.14f &&
                        draw.y >= slotBounds.y + slotBounds.height * 0.14f &&
                        draw.y + draw.height <= slotBounds.top - slotBounds.height * 0.14f
                }
            }

        assertTrue(
            hollowCenters >= emptySlotBounds.size - 1,
            "empty right-panel slots should have a visible hollow socket center so they read as intentional equipment targets, not flat black placeholders",
        )
    }

    @Test
    fun `right panel equipment rig connects sockets with forged rail structure`() {
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
        val equipment = layout.demoShell.rightPanelLayout.equipment

        val verticalHarnessRails =
            canvas.rectDraws.filter { draw ->
                draw.width in 2f..4f &&
                    draw.height > equipment.height * 0.52f &&
                    draw.color.a.isNear(0.118f) &&
                    draw.x >= equipment.x &&
                    draw.x + draw.width <= equipment.right &&
                    draw.y >= equipment.y &&
                    draw.y + draw.height <= equipment.top
            }
        assertTrue(
            verticalHarnessRails.size >= 2,
            "equipment sockets should sit on paired forged rails so the section reads as one paper-doll rig, not isolated floating icons",
        )

        val rowTieBars =
            canvas.rectDraws.filter { draw ->
                draw.width > equipment.width * 0.24f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.086f) &&
                    draw.x >= equipment.x &&
                    draw.x + draw.width <= equipment.right &&
                    draw.y >= equipment.y &&
                    draw.y + draw.height <= equipment.top
            }
        assertTrue(
            rowTieBars.size >= 3,
            "equipment rig should use restrained horizontal tie bars to connect slot rows without turning the panel into a bright grid",
        )

        val harnessRivets =
            canvas.rectDraws.filter { draw ->
                draw.width in 4f..6f &&
                    draw.height in 4f..6f &&
                    draw.color.a.isNear(0.132f) &&
                    draw.x >= equipment.x &&
                    draw.x + draw.width <= equipment.right &&
                    draw.y >= equipment.y &&
                    draw.y + draw.height <= equipment.top
            }
        assertTrue(
            harnessRivets.size >= 6,
            "equipment rig rails should have small worn rivets so the large right-panel equipment surface gains hand-authored metal detail",
        )
    }

    @Test
    fun `right panel equipment rig reads as an armored paper doll scaffold`() {
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

        val equipment = TileRenderer.layoutMetrics(mapWidth = 1, mapHeight = 1, cellWidth = 32f, cellHeight = 32f).demoShell.rightPanelLayout.equipment

        val torsoShadow =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.168f) &&
                    draw.width in 38f..54f &&
                    draw.height > equipment.height * 0.20f &&
                    draw.x >= equipment.x + equipment.width * 0.38f &&
                    draw.x + draw.width <= equipment.right - equipment.width * 0.38f &&
                    draw.y >= equipment.y + equipment.height * 0.14f &&
                    draw.y + draw.height <= equipment.top - equipment.height * 0.12f
            }
        assertTrue(
            torsoShadow.isNotEmpty(),
            "equipment sockets should sit over a restrained central armored torso shadow so the upper right panel reads as a paper-doll scaffold, not icons floating on black",
        )

        val shoulderMantles =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.154f) &&
                    draw.width > equipment.width * 0.36f &&
                    draw.height in 18f..26f &&
                    draw.x >= equipment.x + equipment.width * 0.18f &&
                    draw.x + draw.width <= equipment.right - equipment.width * 0.18f &&
                    draw.y >= equipment.y + equipment.height * 0.52f &&
                    draw.y + draw.height <= equipment.top - 12f
            }
        assertTrue(
            shoulderMantles.isNotEmpty(),
            "equipment rig should include a broad shoulder mantle behind the top socket rows so the section gains a readable armor silhouette at first glance",
        )

        val sideArmorPlates =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.142f) &&
                    draw.width in 14f..22f &&
                    draw.height > equipment.height * 0.24f &&
                    draw.x >= equipment.x + 10f &&
                    draw.x + draw.width <= equipment.right - 10f &&
                    draw.y >= equipment.y + equipment.height * 0.16f &&
                    draw.y + draw.height <= equipment.top - equipment.height * 0.12f
            }
        assertTrue(
            sideArmorPlates.size >= 2,
            "equipment rig should add paired side armor plates so the large equipment section has authored side mass instead of empty black gutters",
        )
    }

    @Test
    fun `right panel large surfaces carry restrained charcoal material skin`() {
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
        val right = layout.demoShell.rightPanelLayout

        val equipmentCharcoalGrain =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.052f) &&
                    draw.width > right.equipment.width * 0.70f &&
                    draw.height in 2f..4f &&
                    draw.x >= right.equipment.x + 10f &&
                    draw.x + draw.width <= right.equipment.right - 10f &&
                    draw.y >= right.equipment.y + 10f &&
                    draw.y + draw.height <= right.equipment.top - 8f
            }
        assertTrue(
            equipmentCharcoalGrain.size >= 2,
            "equipment section body should carry broad, low-contrast charcoal grain so the large black area reads as material instead of a flat void",
        )

        val inscriptionOldLeatherStriations =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.046f) &&
                    draw.width in 2f..4f &&
                    draw.height > right.inscriptions.height * 0.40f &&
                    draw.x >= right.inscriptions.x + 8f &&
                    draw.x + draw.width <= right.inscriptions.right - 8f &&
                    draw.y >= right.inscriptions.y + 8f &&
                    draw.y + draw.height <= right.inscriptions.top - 8f
            }
        assertTrue(
            inscriptionOldLeatherStriations.size >= 2,
            "inscription section should include subdued vertical old-leather striations so the row rack sits on a crafted backing, not a plain black panel",
        )

        val backpackSootFlecks =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.038f) &&
                    draw.width in 3f..6f &&
                    draw.height in 3f..6f &&
                    draw.x >= right.backpack.x + 8f &&
                    draw.x + draw.width <= right.backpack.right - 8f &&
                    draw.y >= right.backpack.y + 8f &&
                    draw.y + draw.height <= right.backpack.top - 8f
            }
        assertTrue(
            backpackSootFlecks.size >= 4,
            "backpack tray body should have tiny soot and worn-stone flecks so empty inventory space still reads as authored dark fantasy material",
        )
    }

    @Test
    fun `right panel inscriptions and backpack read as forged utility racks`() {
        val canvas = RecordingTileCanvas()
        val weapon =
            ItemRenderSnapshot(
                baseItemId = "short_sword",
                nameKey = "item.short_sword.name",
                typeId = "WEAPON",
                iconKey = "item.short_sword.icon",
            )
        val potion =
            ItemRenderSnapshot(
                baseItemId = "healing_potion",
                nameKey = "item.healing_potion.name",
                typeId = "CONSUMABLE",
                iconKey = "item.short_sword.icon",
            )
        val snapshot =
            sampleSnapshot(
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
                        InscriptionSlotSnapshot(
                            hotkey = 6,
                            inscriptionId = "healing_light",
                            nameKey = "inscription.healing_light.name",
                            descKey = "inscription.healing_light.desc",
                            iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                            categoryId = "HEALING",
                            cooldownRemaining = 0,
                            maxCooldown = 10,
                        ),
                    ),
                inventory =
                    listOf(
                        InventoryEntrySnapshot(index = 0, item = weapon),
                        InventoryEntrySnapshot(index = 1, item = potion),
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

        val layout = TileRenderer.layoutMetrics(mapWidth = 1, mapHeight = 1, cellWidth = 32f, cellHeight = 32f)
        val right = layout.demoShell.rightPanelLayout

        val inscriptionLedgerSpines =
            canvas.rectDraws.filter { draw ->
                draw.width in 2f..5f &&
                    draw.height > right.inscriptionSlots.slotSide * 3.0f &&
                    draw.color.a.isNear(0.112f) &&
                    draw.x >= right.inscriptions.x &&
                    draw.x + draw.width <= right.inscriptions.right &&
                    draw.y >= right.inscriptions.y &&
                    draw.y + draw.height <= right.inscriptions.top
            }
        assertTrue(
            inscriptionLedgerSpines.size >= 2,
            "inscription rows should sit on column ledger spines so the section reads as a forged rune rack, not detached list rows",
        )

        val inscriptionRivets =
            canvas.rectDraws.filter { draw ->
                draw.width in 4f..6f &&
                    draw.height in 4f..6f &&
                    draw.color.a.isNear(0.128f) &&
                    draw.x >= right.inscriptions.x &&
                    draw.x + draw.width <= right.inscriptions.right &&
                    draw.y >= right.inscriptions.y &&
                    draw.y + draw.height <= right.inscriptions.top
            }
        assertTrue(
            inscriptionRivets.size >= 8,
            "inscription ledger spines should have worn rivets so the dense hotkey rows gain hand-authored metal detail",
        )

        val backpackShelves =
            canvas.rectDraws.filter { draw ->
                draw.width > right.backpack.width * 0.68f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.092f) &&
                    draw.x >= right.backpack.x &&
                    draw.x + draw.width <= right.backpack.right &&
                    draw.y >= right.backpack.y &&
                    draw.y + draw.height <= right.backpack.top
            }
        assertTrue(
            backpackShelves.size >= 2,
            "backpack slots should rest on restrained shelf rails instead of floating as a loose icon grid",
        )

        val backpackStraps =
            canvas.rectDraws.filter { draw ->
                draw.width in 2f..4f &&
                    draw.height > right.backpackSlots.slotSide * 1.55f &&
                    draw.color.a.isNear(0.118f) &&
                    draw.x >= right.backpack.x &&
                    draw.x + draw.width <= right.backpack.right &&
                    draw.y >= right.backpack.y &&
                    draw.y + draw.height <= right.backpack.top
            }
        assertTrue(
            backpackStraps.size >= 2,
            "backpack utility slots should be tied together by subtle leather/iron straps so the section reads as one pack tray",
        )
    }

    @Test
    fun `operation command matrix keeps caption hierarchy and non overlapping key chips`() {
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
        val operationBounds = layout.demoShell.rightPanelLayout.operationHints
        val commandKeyDraws =
            canvas.textDraws.filter { draw ->
                draw.text in setOf("i", "g", "Ctrl+S", "L", "1-4", "5-8") &&
                    draw.x >= operationBounds.x &&
                    draw.x <= operationBounds.right &&
                    draw.y >= operationBounds.y &&
                    draw.y <= operationBounds.top
            }

        val operationDrawSummary =
            canvas.textDraws
                .filter { draw ->
                    draw.x >= operationBounds.x &&
                        draw.x <= operationBounds.right &&
                        draw.y >= operationBounds.y &&
                        draw.y <= operationBounds.top
                }.joinToString(" | ") { draw -> "${draw.text}@${draw.x},${draw.y}/${draw.style.name}" }
        assertTrue(
            commandKeyDraws.size >= 5,
            "operation command matrix must keep the core keyboard shortcuts visible: $operationDrawSummary",
        )
        assertTrue(
            commandKeyDraws.all { draw -> draw.style == TileTextStyle.CAPTION },
            "operation shortcuts should render at caption hierarchy so the hint plate stays secondary to equipment, inscriptions, and backpack",
        )

        val keyChips =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.46f) &&
                    draw.width in 30f..96f &&
                    draw.height in 12f..15f &&
                    draw.x >= operationBounds.x &&
                    draw.x + draw.width <= operationBounds.right &&
                    draw.y >= operationBounds.y &&
                    draw.y + draw.height <= operationBounds.top
            }
        assertTrue(keyChips.size >= 5, "operation key chips should remain visible after caption-level compaction")
        keyChips.forEachIndexed { index, chip ->
            keyChips.drop(index + 1).forEach { other ->
                val horizontalOverlap = chip.x < other.x + other.width && other.x < chip.x + chip.width
                val verticalOverlap = chip.y < other.y + other.height && other.y < chip.y + chip.height
                assertFalse(
                    horizontalOverlap && verticalOverlap,
                    "operation key chips should not overlap; overlapping chips make the right-panel hint plate read as cramped engineering text",
                )
            }
        }
    }

    @Test
    fun `right operation hints sit on one forged command dock`() {
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
        val operationBounds = layout.demoShell.rightPanelLayout.operationHints
        val dockBackplates =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.156f) &&
                    draw.width > operationBounds.width * 0.76f &&
                    draw.height > operationBounds.height * 0.34f &&
                    draw.x >= operationBounds.x &&
                    draw.x + draw.width <= operationBounds.right &&
                    draw.y >= operationBounds.y &&
                    draw.y + draw.height <= operationBounds.top
            }
        assertTrue(
            dockBackplates.isNotEmpty(),
            "right operation hints should sit on one shared dark command dock instead of reading as loose text chips",
        )

        val forgedColumnRails =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.096f) &&
                    draw.width in 2f..4f &&
                    draw.height > operationBounds.height * 0.40f &&
                    draw.x >= operationBounds.x &&
                    draw.x + draw.width <= operationBounds.right &&
                    draw.y >= operationBounds.y &&
                    draw.y + draw.height <= operationBounds.top
            }
        assertTrue(
            forgedColumnRails.isNotEmpty(),
            "two-column operation shortcuts should be tied together by a forged vertical command rail",
        )

        val keyplateRivets =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.124f) &&
                    draw.width in 4f..6f &&
                    draw.height in 4f..6f &&
                    draw.x >= operationBounds.x &&
                    draw.x + draw.width <= operationBounds.right &&
                    draw.y >= operationBounds.y &&
                    draw.y + draw.height <= operationBounds.top
            }
        assertTrue(
            keyplateRivets.size >= 8,
            "operation keyplates should have worn rivet anchors so the hint area reads as a deliberate utility rack",
        )
    }

    @Test
    fun `dark uiux pr03 presents typed equipment inventory identity and fallback icons`() {
        val canvas = RecordingTileCanvas()
        val weapon =
            ItemRenderSnapshot(
                baseItemId = "hunter_bow",
                nameKey = "item.hunter_bow.name",
                typeId = "WEAPON",
                slotId = "WEAPON",
                iconKey = "item.hunter_bow.icon",
                qualityTierId = "MAGIC",
            )
        val accessory =
            ItemRenderSnapshot(
                baseItemId = "emerald_charm",
                nameKey = "item.emerald_charm.name",
                typeId = "ARMOR",
                slotId = "ACCESSORY",
                iconKey = "item.emerald_charm.icon",
                qualityTierId = "RARE",
            )
        val missingIcon =
            ItemRenderSnapshot(
                baseItemId = "debug_missing",
                nameKey = "item.debug_missing.name",
                typeId = "CONSUMABLE",
                visualKey = "item.missing.debug.visual",
                iconKey = "item.missing.debug.icon",
                qualityTierId = "RARE",
            )
        val snapshot =
            sampleSnapshot(
                equipment =
                    listOf(
                        EquipmentSlotSnapshot(slotId = "WEAPON", item = weapon),
                        EquipmentSlotSnapshot(slotId = "ACCESSORY", item = accessory),
                    ),
                inventory =
                    listOf(
                        InventoryEntrySnapshot(index = 3, item = weapon),
                        InventoryEntrySnapshot(index = 7, item = missingIcon),
                    ),
            )

        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(ManifestLogSink { }),
                snapshot = snapshot,
                overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 7),
            )
        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(ManifestLogSink { }),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 7),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val equipment = model.shell.demo.equipmentInventory.equipmentSlots
        assertEquals(listOf("WEAPON", "OFF_HAND", "ARMOR", "ACCESSORY"), equipment.take(4).map(EquipmentSlotCellModel::slotId))
        assertTrue(equipment.drop(4).all(EquipmentSlotCellModel::visualOnly))
        val inventory = model.shell.demo.equipmentInventory.inventoryGrid
        assertEquals(7, inventory.selectedInventoryIndex)
        assertEquals(listOf(3, 7), inventory.cells.take(2).map(InventoryGridCellModel::identityIndex))
        assertEquals(UiCompanionVisualKeys.EMPTY_INVENTORY, inventory.cells[1].itemIcon?.resolvedKey)
        assertEquals("inventory:7", inventory.cells[1].tooltipAnchorId)
        assertTrue(canvas.assetDraws.any { draw -> draw.asset.resolvedKey == DarkUiChromeVisualKeys.SLOT_SELECTED })
        assertTrue(canvas.assetDraws.any { draw -> draw.asset.resolvedKey == UiCompanionVisualKeys.EMPTY_INVENTORY })
        assertTrue(canvas.textDraws.none { draw -> draw.text == "Weapon" || draw.text == "Accessory" })
    }

    @Test
    fun `dark uiux pr03 draws shop offer frame price and type markers`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    width = 24,
                    height = 18,
                    shardBalance = 25,
                    activeShop = sampleShop(),
                ),
            overlayState = OverlayState(mode = UiMode.SHOP, shopFocus = ShopFocus.BUY, shopOfferSelection = 0),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val drawnKeys = canvas.assetDraws.map { draw -> draw.asset.resolvedKey }
        assertTrue(DarkUiChromeVisualKeys.SHOP_OFFER_FRAME in drawnKeys, drawnKeys.toString())
        assertTrue(DarkUiChromeVisualKeys.SHOP_PRICE_AFFORDABLE in drawnKeys, drawnKeys.toString())
        assertTrue(DarkUiChromeVisualKeys.SHOP_INSCRIPTION_MARKER in drawnKeys, drawnKeys.toString())
    }

    @Test
    fun `dark uiux pr03 draws replacement slot marker in shop prompt`() {
        val canvas = RecordingTileCanvas()
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val snapshot =
            sampleSnapshot(
                width = 24,
                height = 18,
                shardBalance = 25,
                activeShop = sampleShop(inscriptionReplacementPrompt = sampleReplacementPrompt(currentSlotCount = 4)),
            )
        val overlayState =
            OverlayState(
                mode = UiMode.SHOP,
                shopFocus = ShopFocus.BUY,
                shopOfferSelection = 0,
                inscriptionReplacementHotkeySelection = 5,
            )
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = overlayState,
            )

        TileRenderer.renderToCanvas(
            localizer = localizer,
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = overlayState,
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val slotRows =
            model.shell.demo.operationRows.filter { row ->
                row.kind == TileTextRowKind.SHOP_REPLACEMENT_SLOT
            }
        assertEquals(4, slotRows.size)
        assertTrue(
            canvas.assetDraws.count { draw -> draw.asset.resolvedKey == DarkUiChromeVisualKeys.SHOP_REPLACEMENT_SLOT_MARKER } >= 4,
            canvas.assetDraws.map { draw -> draw.asset.resolvedKey }.toString(),
        )
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
        assertTrue(model.targetHighlights.any { highlight -> highlight.tile == com.ktome.core.map.Point(0, 0) && highlight.state == TileTargetCursorState.LEGAL })
        assertTrue(model.targetHighlights.any { highlight -> highlight.tile == com.ktome.core.map.Point(0, 1) && highlight.state == TileTargetCursorState.ILLEGAL })

        val canvas = RecordingTileCanvas()
        val diagnostics =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = snapshot,
                overlayState = overlayState,
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )
        assertNull(diagnostics.overlayFrame.overlayModel.activeModal)
        assertNull(diagnostics.overlayFrame.overlayModel.modalBackdrop)
        assertTrue(canvas.flushes.contains(TileLayerFlushReason.MAP_TARGETING_HIGHLIGHTS))
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

        assertTrue(cellRects.any { draw -> draw.x == explored.x.toFloat() && draw.y == explored.y.toFloat() && draw.color.a.isNear(0.70f) })
        assertTrue(cellRects.any { draw -> draw.x == hidden.x.toFloat() && draw.y == hidden.y.toFloat() && draw.color.a.isNear(0.98f) })
    }

    @Test
    fun `render canvas anchors visible dungeon room with torch fixtures`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    width = 3,
                    height = 2,
                    cells =
                        (0 until 3).flatMap { x ->
                            (0 until 2).map { y ->
                                MapCellSnapshot(
                                    x = x,
                                    y = y,
                                    visibility = CellVisibilitySnapshot.VISIBLE,
                                    terrainTypeId = if (y == 1) "wall" else "floor",
                                    terrainVisualKey = "tileset.test.ground_01",
                                )
                            }
                        },
                ),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(
            canvas.rectDraws.any { draw -> draw.width == 4f && draw.height == 12f && draw.color.a == 0.62f },
            "visible wall/floor contact should draw torch flames as a stable dungeon focal point",
        )
    }

    @Test
    fun `render canvas keeps torch focal points sparse so dark stage pressure remains`() {
        val canvas = RecordingTileCanvas()
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = 6, playerY = 4),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val flameCores =
            canvas.rectDraws.filter { draw ->
                draw.width == 4f && draw.height == 12f && draw.color.a.isNear(0.62f)
            }

        assertTrue(
            flameCores.size in 3..4,
            "large rooms should keep torch flames to a few authored focal points; too many flames turn local firelight into a map-wide amber wash",
        )
    }

    @Test
    fun `render canvas keeps torch tile glow from becoming map-wide amber grid wash`() {
        val canvas = RecordingTileCanvas()
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = 6, playerY = 4),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val amberTileGlowCells =
            canvas.rectDraws.filter { draw ->
                draw.width == 30f &&
                    draw.height == 30f &&
                    draw.color.a > 0.03f &&
                    draw.color.r > 0.70f &&
                    draw.color.g > 0.28f &&
                    draw.color.b < 0.24f
            }

        assertTrue(
            amberTileGlowCells.size <= 96,
            "torch tile glow should stay local; too many high-alpha 30px amber cells turn authored torch light into a map-wide grid wash, count=${amberTileGlowCells.size}",
        )
    }

    @Test
    fun `render canvas joins adjacent visible walls into continuous masonry mass`() {
        val canvas = RecordingTileCanvas()
        val cells =
            (0 until 7).flatMap { x ->
                (0 until 6).map { y ->
                    val wall = x == 0 || x == 6 || y == 0 || y == 5
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(width = 7, height = 6, cells = cells, playerX = 3, playerY = 3),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 150f &&
                    draw.height in 12f..14f &&
                    draw.color.a.isNear(0.30f)
            },
            "adjacent visible wall tiles should merge into a continuous horizontal masonry mass, not only repeat per-cell wall relief",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 12f..14f &&
                    draw.height > 90f &&
                    draw.color.a.isNear(0.26f)
            },
            "side walls should also read as continuous vertical thickness so room boundaries feel architectural rather than tiled",
        )
    }

    @Test
    fun `render canvas frames narrow visible passages as authored dungeon thresholds`() {
        val canvas = RecordingTileCanvas()
        val cells =
            (0 until 7).flatMap { x ->
                (0 until 7).map { y ->
                    val floor = (x in 2..4 && y in 3..5) || (x == 3 && y in 1..2)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (floor) "floor" else "wall",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 7, height = 7, cells = cells, playerX = 3, playerY = 4),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val throat = summary.viewport.tileRect(com.ktome.core.map.Point(3, 2))
        val throatCenterX = throat.x + throat.width / 2f
        val throatCenterY = throat.y + throat.height / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 7f..9f &&
                    draw.height > 44f &&
                    draw.color.a.isNear(0.38f) &&
                    draw.contains(throat.x + 4f, throatCenterY)
            },
            "single-width visible passages should gain a continuous dark side jamb so doorways read as authored architecture, not a cut-out in a rectangle",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 20f..28f &&
                    draw.height in 8f..12f &&
                    draw.color.a.isNear(0.31f) &&
                    draw.contains(throatCenterX, throat.y + throat.height - 4f)
            },
            "room-to-corridor transitions should add a compact worn threshold cap instead of only repeating per-tile floor art",
        )
    }

    @Test
    fun `render canvas caps corridor mouths with broken lintel stones`() {
        val canvas = RecordingTileCanvas()
        val cells =
            (0 until 7).flatMap { x ->
                (0 until 7).map { y ->
                    val floor = (x in 2..4 && y in 3..5) || (x == 3 && y in 1..2)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (floor) "floor" else "wall",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 7, height = 7, cells = cells, playerX = 3, playerY = 4),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val throat = summary.viewport.tileRect(com.ktome.core.map.Point(3, 2))
        val throatCenterX = throat.x + throat.width / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 56f..61f &&
                    draw.height in 11f..15f &&
                    draw.color.a.isNear(0.205f) &&
                    draw.contains(throatCenterX, throat.y + throat.height - 8f)
            },
            "corridor mouths should gain a broken lintel stone wider than one tile so room openings read as carved dungeon architecture",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 46f..49f &&
                    draw.height in 16f..19f &&
                    draw.color.a.isNear(0.287f) &&
                    draw.contains(throatCenterX, throat.y + throat.height - 10f)
            },
            "corridor mouths should receive a recessed throat shadow so the opening reads carved into thick wall mass instead of a flat tile seam",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 31f..36f &&
                    draw.height in 3f..5f &&
                    draw.color.a.isNear(0.132f) &&
                    draw.contains(throatCenterX, throat.y + throat.height - 4f)
            },
            "corridor mouth lintels should include a warm worn lip tying the passage threshold into the surrounding wall material",
        )
    }

    @Test
    fun `render canvas chips corridor mouth edges with rubble teeth`() {
        val canvas = RecordingTileCanvas()
        val cells =
            (0 until 7).flatMap { x ->
                (0 until 7).map { y ->
                    val floor = (x in 2..4 && y in 3..5) || (x == 3 && y in 1..2)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (floor) "floor" else "wall",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 7, height = 7, cells = cells, playerX = 3, playerY = 4),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val throat = summary.viewport.tileRect(com.ktome.core.map.Point(3, 2))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 4f..6f &&
                    draw.height in 12f..15f &&
                    draw.color.a.isNear(0.263f) &&
                    draw.contains(throat.x + 6f, throat.y + throat.height - 11f)
            },
            "corridor mouth edges should add compact dark rubble teeth so the aperture reads chipped out of stone rather than masked by a smooth rectangle",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 4f..6f &&
                    draw.height in 9f..12f &&
                    draw.color.a.isNear(0.241f) &&
                    draw.contains(throat.x + throat.width - 6f, throat.y + throat.height - 9f)
            },
            "corridor mouth edges should use uneven rubble teeth on the opposite jamb so the doorway silhouette stays asymmetric",
        )
        val wornPinChips =
            canvas.rectDraws.filter { draw ->
                draw.width in 6f..9f &&
                    draw.height in 1f..3f &&
                    draw.color.a.isNear(0.149f)
            }
        assertTrue(
            wornPinChips.size >= 2,
            "corridor mouth rubble should include restrained worn stone pin chips so the broken aperture remains material instead of a flat dark bite",
        )
    }

    @Test
    fun `render canvas gives narrow passage throats asymmetric shadow bites`() {
        val canvas = RecordingTileCanvas()
        val cells =
            (0 until 7).flatMap { x ->
                (0 until 7).map { y ->
                    val floor = (x in 2..4 && y in 3..5) || (x == 3 && y in 1..2)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (floor) "floor" else "wall",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 7, height = 7, cells = cells, playerX = 3, playerY = 4),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val throat = summary.viewport.tileRect(com.ktome.core.map.Point(3, 2))
        val throatCenterX = throat.x + throat.width / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 6f..8f &&
                    draw.height in 17f..20f &&
                    draw.color.a.isNear(0.236f) &&
                    draw.contains(throat.x + 12f, throat.y + throat.height * 0.44f)
            },
            "narrow passage throats should have an offset dark bite on one jamb so the opening reads carved through thick masonry, not a symmetric rectangular slot",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 5f..7f &&
                    draw.height in 13f..16f &&
                    draw.color.a.isNear(0.214f) &&
                    draw.contains(throat.x + throat.width - 12f, throat.y + throat.height * 0.68f)
            },
            "narrow passage throats should have a second shorter shadow bite on the opposite jamb to break the doorway silhouette",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 8f..11f &&
                    draw.height in 1f..3f &&
                    draw.color.a.isNear(0.121f) &&
                    draw.contains(throatCenterX, throat.y + throat.height * 0.30f)
            },
            "asymmetric throat shadows should include a small worn stone nick so the doorway pressure remains material, not just a flat black mask",
        )
    }

    @Test
    fun `render canvas gives horizontal passage throats asymmetric shadow bites`() {
        val canvas = RecordingTileCanvas()
        val cells =
            (0 until 7).flatMap { x ->
                (0 until 7).map { y ->
                    val floor = (x in 3..5 && y in 2..4) || (x in 1..2 && y == 3)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (floor) "floor" else "wall",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 7, height = 7, cells = cells, playerX = 4, playerY = 3),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val throat = summary.viewport.tileRect(com.ktome.core.map.Point(2, 3))
        val throatCenterY = throat.y + throat.height / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 17f..20f &&
                    draw.height in 6f..8f &&
                    draw.color.a.isNear(0.236f) &&
                    draw.contains(throat.x + throat.width * 0.44f, throat.y + 12f)
            },
            "horizontal passage throats should have an offset dark bite on one lintel so side openings read carved through thick masonry, not a symmetric rectangular slot",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 13f..16f &&
                    draw.height in 5f..7f &&
                    draw.color.a.isNear(0.214f) &&
                    draw.contains(throat.x + throat.width * 0.68f, throat.y + throat.height - 12f)
            },
            "horizontal passage throats should have a second shorter shadow bite on the opposing lintel to break the doorway silhouette",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 1f..3f &&
                    draw.height in 8f..11f &&
                    draw.color.a.isNear(0.121f) &&
                    draw.contains(throat.x + throat.width * 0.30f, throatCenterY)
            },
            "horizontal throat shadows should include a small worn stone nick so the doorway pressure remains material, not just a flat black mask",
        )
    }

    @Test
    fun `render canvas keeps authored tile art clear beneath atmosphere washes`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    width = 12,
                    height = 8,
                    cells =
                        (0 until 12).flatMap { x ->
                            (0 until 8).map { y ->
                                MapCellSnapshot(
                                    x = x,
                                    y = y,
                                    visibility = CellVisibilitySnapshot.VISIBLE,
                                    terrainTypeId = "floor",
                                    terrainVisualKey = "tileset.test.ground_01",
                                )
                            }
                        },
                ),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val terrainDraw = canvas.assetDraws.first { draw -> draw.asset.entry.category == "tile_ground" }
        assertTrue(
            terrainDraw.alpha >= 0.98f,
            "authored ground tile art should remain the dominant read; atmosphere tuning must not lower terrain asset opacity",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 360f &&
                    draw.height > 230f &&
                    draw.color.a.isNear(0.082f)
            },
            "large visible rooms should use a lighter foundation glaze so the generated stone tile texture remains visible at runtime size",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 300f &&
                    draw.height > 180f &&
                    draw.color.a.isNear(0.045f)
            },
            "large visible rooms should keep painterly atmosphere below the authored floor detail instead of burying cracks and stone grain",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 8f..10f &&
                    draw.height > 190f &&
                    draw.color.a.isNear(0.039f)
            },
            "internal grid dissolve should be restrained enough that players read hand-painted stone first and tile structure second",
        )
    }

    @Test
    fun `render canvas adds authored dungeon scars and stains to visible floors`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    width = 12,
                    height = 8,
                    cells =
                        (0 until 12).flatMap { x ->
                            (0 until 8).map { y ->
                                MapCellSnapshot(
                                    x = x,
                                    y = y,
                                    visibility = CellVisibilitySnapshot.VISIBLE,
                                    terrainTypeId = "floor",
                                    terrainVisualKey = "tileset.test.ground_01",
                                )
                            }
                        },
                ),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(
            canvas.rectDraws.any { draw -> draw.width == 18f && draw.height == 3f && draw.color.a.isNear(0.30f) },
            "visible room floors should include broken slab scars that disrupt the repeated grid read",
        )
        assertTrue(
            canvas.rectDraws.any { draw -> draw.width == 17f && draw.height == 8f && draw.color.a.isNear(0.28f) },
            "visible room floors should include strong story stains, not only neutral stone tiling",
        )
        assertTrue(
            canvas.rectDraws.any { draw -> draw.width > 150f && draw.height > 120f && draw.color.a.isNear(0.46f) },
            "large visible rooms should receive collapsed dark corners so the map does not read as a perfect rectangle",
        )
        assertTrue(
            canvas.rectDraws.any { draw -> draw.width > 300f && draw.height > 180f && draw.color.a.isNear(0.045f) },
            "large visible rooms should receive a restrained painterly wash without burying the authored tile art",
        )
        assertTrue(
            canvas.rectDraws.any { draw -> draw.width > 360f && draw.height > 230f && draw.color.a.isNear(0.082f) },
            "large visible rooms should glaze the base layer lightly so the authored tile art remains the primary read",
        )
        assertTrue(
            canvas.rectDraws.any { draw -> draw.width > 110f && draw.height > 14f && draw.color.a.isNear(0.10f) },
            "large visible rooms should include cross-cell fracture marks, not only per-tile noise",
        )
        assertTrue(
            canvas.rectDraws.any { draw -> draw.width > 96f && draw.height > 54f && draw.color.a.isNear(0.066f) },
            "large visible rooms should include broad uneven slab plates so the floor reads as stonework, not a flat wash",
        )
        assertTrue(
            canvas.rectDraws.any { draw -> draw.width == 5f && draw.height > 190f && draw.color.a.isNear(0.034f) },
            "large visible rooms should cover black cell joints with broader stone mortar bands instead of debug-like grid cuts",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 8f..10f &&
                    draw.height > 190f &&
                    draw.color.a.isNear(0.039f)
            },
            "large visible rooms should dissolve internal black grid joints with wider low-alpha stone grout before adding authored slab accents",
        )
        val jointPlugs =
            canvas.rectDraws.filter { draw ->
                draw.width in 11f..15f &&
                    draw.height in 10f..14f &&
                    draw.color.a.isNear(0.072f)
            }
        assertTrue(
            jointPlugs.size >= 4,
            "large visible rooms should patch selected grid intersections with irregular stone plugs so long black lattice crossings stop reading as a uniform overlay",
        )
        val seamErosionPanels =
            canvas.rectDraws.filter { draw ->
                draw.width > 80f &&
                    draw.height in 16f..28f &&
                    draw.color.a.isNear(0.080f)
            }
        assertTrue(
            seamErosionPanels.size >= 3,
            "large visible rooms should erode long internal grid seams with off-grid stone plates so the eye reads broken slabs before tile lattice",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 170f &&
                    draw.height > 78f &&
                    draw.color.a.isNear(0.072f)
            },
            "large visible rooms should place director-scale stone slab fields across multiple cells so the interior stops reading as individual square tiles",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 185f &&
                    draw.height in 7f..13f &&
                    draw.color.a.isNear(0.14f)
            },
            "large visible rooms should place cross-cell seam breaks over internal joints so the grid reads as stone construction rather than debug lines",
        )
    }

    @Test
    fun `render canvas overlays asymmetric room center veils to suppress grid rhythm`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    width = 12,
                    height = 8,
                    cells =
                        (0 until 12).flatMap { x ->
                            (0 until 8).map { y ->
                                MapCellSnapshot(
                                    x = x,
                                    y = y,
                                    visibility = CellVisibilitySnapshot.VISIBLE,
                                    terrainTypeId = "floor",
                                    terrainVisualKey = "tileset.test.ground_01",
                                )
                            }
                        },
                ),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 220f &&
                    draw.height in 24f..28f &&
                    draw.color.a.isNear(0.086f)
            },
            "large visible rooms should cover the center with asymmetric stone veils so the grid is no longer the dominant room rhythm",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 200f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.052f)
            },
            "room-center stone veils should carry a thin worn lip so the broad overlay reads as material wear, not a flat wash",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 145f..160f &&
                    draw.height in 38f..44f &&
                    draw.color.a.isNear(0.074f)
            },
            "visible room centers should include a broad off-grid shadow terrace that interrupts the repeated tile lattice",
        )
    }

    @Test
    fun `render canvas masks long floor lattice with irregular stone islands`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val lowerIslandFloor = summary.viewport.tileRect(com.ktome.core.map.Point(5, 4))
        val upperIslandFloor = summary.viewport.tileRect(com.ktome.core.map.Point(7, 2))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 128f..134f &&
                    draw.height in 45f..49f &&
                    draw.color.a.isNear(0.083f) &&
                    draw.contains(lowerIslandFloor.x + lowerIslandFloor.width * 0.68f, lowerIslandFloor.y + lowerIslandFloor.height * 0.58f)
            },
            "large visible rooms should mask long floor lattice with off-grid stone islands that cross both row and column seams",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 116f..121f &&
                    draw.height in 42f..45f &&
                    draw.color.a.isNear(0.091f) &&
                    draw.contains(upperIslandFloor.x + upperIslandFloor.width * 0.60f, upperIslandFloor.y + upperIslandFloor.height * 0.38f)
            },
            "upper visible-room floor should receive a darker broken stone island so the eye does not follow a continuous checkerboard row",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                    draw.width in 86f..91f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.057f) &&
                    draw.contains(lowerIslandFloor.x + lowerIslandFloor.width * 0.72f, lowerIslandFloor.y + lowerIslandFloor.height * 0.68f)
            },
            "irregular stone islands should include a narrow worn lip so the overlay reads as authored stone material, not a flat veil",
        )
    }

    @Test
    fun `render canvas sharpens room center with high relief stone facets`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val centerFloor = summary.viewport.tileRect(playerTile)

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 96f..100f &&
                    draw.height in 18f..21f &&
                    draw.color.a.isNear(0.118f) &&
                    draw.contains(centerFloor.x + centerFloor.width * 0.70f, centerFloor.y + centerFloor.height * 0.58f)
            },
            "visible room center should get a higher-relief off-grid stone facet so the focal floor reads as sharp masonry instead of smoky tile lattice",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 3f..5f &&
                    draw.height in 17f..20f &&
                    draw.color.a.isNear(0.124f) &&
                    draw.contains(centerFloor.x + centerFloor.width * 0.54f, centerFloor.y + centerFloor.height * 0.50f)
            },
            "high-relief stone facets should include a short dark undercut that restores local contrast without drawing a full grid line",
        )
    }

    @Test
    fun `render canvas cuts warm map haze with a cool tactical clarity plane`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val centerFloor = summary.viewport.tileRect(playerTile)
        val tacticalClarityCandidates =
            canvas.rectDraws
                .filter { draw -> draw.color.a.isNear(0.153f) || draw.color.a.isNear(0.161f) || draw.color.a.isNear(0.061f) }
                .joinToString { draw -> "${draw.width}x${draw.height}@${draw.x},${draw.y}/a=${draw.color.a}" }

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 150f..162f &&
                    draw.height in 35f..39f &&
                    draw.color.a.isNear(0.153f) &&
                    draw.contains(centerFloor.x + centerFloor.width * 0.28f, centerFloor.y + centerFloor.height * 0.46f)
            },
            "visible room focus should cut the warm map haze with a cool tactical clarity plane so the center reads as stone, not amber fog; center=$centerFloor; candidates=$tacticalClarityCandidates",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 119f..123f &&
                    draw.height in 3f..5f &&
                    draw.color.a.isNear(0.161f) &&
                    draw.contains(centerFloor.x + centerFloor.width * 0.62f, centerFloor.y + centerFloor.height * 0.90f)
            },
            "tactical clarity should add a compact dark undercut that sharpens the focal lane without restoring a full grid line",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 78f..84f &&
                    draw.height in 1f..3f &&
                    draw.color.a.isNear(0.061f) &&
                    draw.contains(centerFloor.x + centerFloor.width * 0.54f, centerFloor.y + centerFloor.height * 1.22f)
            },
            "tactical clarity should keep a restrained worn lip so the sharper center remains dungeon material instead of a UI overlay",
        )
    }

    @Test
    fun `render canvas etches focal stone cutlines into visible room center`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val centerFloor = summary.viewport.tileRect(playerTile)
        val focalCutlineCandidates =
            canvas.rectDraws
                .filter { draw -> draw.color.a.isNear(0.137f) || draw.color.a.isNear(0.128f) || draw.color.a.isNear(0.076f) }
                .joinToString { draw -> "${draw.width}x${draw.height}@${draw.x},${draw.y}/a=${draw.color.a}" }

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 87f..90f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.137f) &&
                    draw.contains(centerFloor.x + centerFloor.width * 0.72f, centerFloor.y + centerFloor.height * 1.08f)
            },
            "visible room center should receive a short dark focal cutline so the stone floor reads sharper than a soft fog plane; center=$centerFloor candidates=$focalCutlineCandidates",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 2f..4f &&
                    draw.height in 32f..36f &&
                    draw.color.a.isNear(0.128f) &&
                    draw.contains(centerFloor.x + centerFloor.width * 2.02f, centerFloor.y + centerFloor.height * 0.86f)
            },
            "focal stone cutlines should include a compact vertical bite that breaks the remaining square-grid read near the player lane",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 60f..64f &&
                    draw.height in 1f..3f &&
                    draw.color.a.isNear(0.076f) &&
                    draw.contains(centerFloor.x + centerFloor.width * 1.40f, centerFloor.y + centerFloor.height * 1.58f)
            },
            "focal stone cutlines should keep a cool worn lip so the sharpened center remains material, not UI chrome",
        )
    }

    @Test
    fun `render canvas adds fine aggregate stone grain to visible floor cells`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val centerFloor = summary.viewport.tileRect(playerTile)
        fun isInsideCenterFloor(
            x: Float,
            y: Float,
        ): Boolean =
            x >= centerFloor.x &&
                x <= centerFloor.x + centerFloor.width &&
                y >= centerFloor.y &&
                y <= centerFloor.y + centerFloor.height

        val fineDarkPits =
            canvas.rectDraws.filter { draw ->
                draw.width == 2f &&
                    draw.height == 2f &&
                    (draw.color.a.isNear(0.036f) || draw.color.a.isNear(0.030f)) &&
                    isInsideCenterFloor(draw.x + 1f, draw.y + 1f)
            }
        val warmAggregateFlecks =
            canvas.rectDraws.filter { draw ->
                draw.width in 5f..7f &&
                    draw.height == 1f &&
                    draw.color.a.isNear(0.026f) &&
                    isInsideCenterFloor(draw.x + draw.width / 2f, draw.y)
            }

        assertTrue(
            fineDarkPits.size >= 2,
            "visible floor cells should carry multiple tiny dark aggregate pits so hand-painted stone density is visible at runtime size",
        )
        assertTrue(
            warmAggregateFlecks.isNotEmpty(),
            "visible floor cells should include restrained warm aggregate flecks so stone grain reads as authored material, not only flat haze",
        )
    }

    @Test
    fun `render canvas sharpens visible floor cells with fracture kernels`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val centerFloor = summary.viewport.tileRect(playerTile)
        fun isInsideCenterFloor(
            x: Float,
            y: Float,
        ): Boolean =
            x >= centerFloor.x &&
                x <= centerFloor.x + centerFloor.width &&
                y >= centerFloor.y &&
                y <= centerFloor.y + centerFloor.height

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 9f..12f &&
                    draw.height in 1f..2f &&
                    draw.color.a.isNear(0.054f) &&
                    isInsideCenterFloor(draw.x + draw.width / 2f, draw.y)
            },
            "visible floor cells should carry compact dark fracture kernels so the tile body reads as sharpened stone rather than fog-softened texture",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 1f..2f &&
                    draw.height in 6f..9f &&
                    draw.color.a.isNear(0.050f) &&
                    isInsideCenterFloor(draw.x, draw.y + draw.height / 2f)
            },
            "visible floor fracture kernels should include a short vertical cut to break repeated same-direction cracks",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 4f..6f &&
                    draw.height in 1f..2f &&
                    draw.color.a.isNear(0.032f) &&
                    isInsideCenterFloor(draw.x + draw.width / 2f, draw.y)
            },
            "visible floor fracture kernels should keep a restrained worn edge so the sharper cracks remain material, not UI noise",
        )
    }

    @Test
    fun `render canvas adds resource-scale chipped stone clusters to visible floor cells`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val centerFloor = summary.viewport.tileRect(playerTile)
        fun isInsideCenterFloor(
            x: Float,
            y: Float,
        ): Boolean =
            x >= centerFloor.x &&
                x <= centerFloor.x + centerFloor.width &&
                y >= centerFloor.y &&
                y <= centerFloor.y + centerFloor.height

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 3f..5f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.089f) &&
                    isInsideCenterFloor(draw.x + draw.width / 2f, draw.y + draw.height / 2f)
            },
            "visible floor cells should include compact dark chipped-stone sockets that read at 32px runtime size",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 2f..4f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.059f) &&
                    isInsideCenterFloor(draw.x + draw.width / 2f, draw.y + draw.height / 2f)
            },
            "visible floor chipped clusters should include cold stone flecks so material density is authored instead of monochrome dirt",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 6f..9f &&
                    draw.height in 1f..2f &&
                    draw.color.a.isNear(0.047f) &&
                    isInsideCenterFloor(draw.x + draw.width / 2f, draw.y)
            },
            "visible floor chipped clusters should keep a restrained worn lip so added detail reads as stone edge wear, not random noise",
        )
    }

    @Test
    fun `render canvas lays broad mortar veils over long room lattice seams`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
        )

        val centerFloor = summary.viewport.tileRect(playerTile)
        val horizontalVeilTarget = centerFloor.x + centerFloor.width / 2f to centerFloor.y + centerFloor.height * 1.24f
        val verticalVeilTarget = centerFloor.x + centerFloor.width * 0.56f to centerFloor.y + centerFloor.height / 2f
        val wornLipTarget = centerFloor.x + centerFloor.width * 0.62f to centerFloor.y + centerFloor.height * 1.65f
        val horizontalVeilCandidates =
            canvas.rectDraws
                .filter { draw -> draw.color.a.isNear(0.062f) || draw.color.a.isNear(0.056f) || draw.color.a.isNear(0.048f) }
                .map { draw -> "x=${draw.x}, y=${draw.y}, w=${draw.width}, h=${draw.height}, a=${draw.color.a}" }
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 280f &&
                    draw.height in 10f..13f &&
                    draw.color.a.isNear(0.062f) &&
                    draw.contains(horizontalVeilTarget.first, horizontalVeilTarget.second)
            },
            "large visible rooms should lay a broad stone mortar veil across long horizontal seams so the center no longer reads as continuous debug lattice; target=$horizontalVeilTarget candidates=$horizontalVeilCandidates",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 11f..14f &&
                    draw.height > 145f &&
                    draw.color.a.isNear(0.056f) &&
                    draw.contains(verticalVeilTarget.first, verticalVeilTarget.second)
            },
            "large visible rooms should lay a broad off-grid vertical mortar veil so long column seams break into stone construction",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 210f &&
                    draw.height in 1f..3f &&
                    draw.color.a.isNear(0.048f) &&
                    draw.contains(wornLipTarget.first, wornLipTarget.second)
            },
            "broad mortar veils should keep a restrained worn lip so the seam mask reads as material wear rather than a translucent rectangle",
        )
    }

    @Test
    fun `render canvas lays heavy broken slab caps across visible floor lattice`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(2, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val horizontalCapFloor = summary.viewport.tileRect(com.ktome.core.map.Point(5, 4))
        val verticalCapFloor = summary.viewport.tileRect(com.ktome.core.map.Point(7, 3))
        val playerRect = summary.viewport.tileRect(playerTile)
        val playerCenterX = playerRect.x + playerRect.width / 2f
        val playerCenterY = playerRect.y + playerRect.height / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 148f..152f &&
                    draw.height in 14f..17f &&
                    draw.color.a.isNear(0.104f) &&
                    draw.contains(horizontalCapFloor.x + horizontalCapFloor.width * 0.55f, horizontalCapFloor.y + horizontalCapFloor.height * 0.55f)
            },
            "visible floor lattice should be interrupted by a heavy off-grid horizontal slab cap so the room reads as authored stonework instead of a repeated tile grid",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 15f..18f &&
                    draw.height in 118f..121f &&
                    draw.color.a.isNear(0.101f) &&
                    draw.contains(verticalCapFloor.x + verticalCapFloor.width * 0.50f, verticalCapFloor.y + verticalCapFloor.height * 0.48f)
            },
            "visible floor lattice should include a tall broken vertical slab cap that interrupts continuous column seams",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 88f..94f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.063f) &&
                    draw.contains(horizontalCapFloor.x + horizontalCapFloor.width * 0.42f, horizontalCapFloor.y + horizontalCapFloor.height * 0.62f)
            },
            "heavy slab caps should carry a narrow worn lip so the lattice break reads as stone edge detail rather than a flat overlay",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                (draw.color.a.isNear(0.104f) || draw.color.a.isNear(0.101f)) &&
                    draw.contains(playerCenterX, playerCenterY)
            },
            "heavy slab caps must stay off the player focal center",
        )
    }

    @Test
    fun `render canvas gives player and torch compact local light pools`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(4, 3)
        val torchTile = com.ktome.core.map.Point(4, 5)
        val cells =
            (0 until 9).flatMap { x ->
                (0 until 8).map { y ->
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (x == torchTile.x && y == torchTile.y) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 9, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val playerRect = summary.viewport.tileRect(playerTile)
        val playerCenterX = playerRect.x + playerRect.width / 2f
        val playerCenterY = playerRect.y + playerRect.height / 2f
        val torchRect = summary.viewport.tileRect(torchTile)
        val torchCenterX = torchRect.x + torchRect.width / 2f
        val torchCenterY = torchRect.y + torchRect.height / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 116f..120f &&
                    draw.height in 82f..86f &&
                    draw.color.a.isNear(0.080f) &&
                    draw.contains(playerCenterX, playerCenterY)
            },
            "player focus should use a compact warm lantern pool instead of only a map-wide square wash",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 80f..84f &&
                    draw.height in 52f..54f &&
                    draw.color.a.isNear(0.066f) &&
                    draw.contains(torchCenterX, torchCenterY)
            },
            "torch fixtures should cast a tight local warm pool so firelight reads as authored focal light rather than a broad amber rectangle",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width == 30f &&
                    draw.height == 30f &&
                    draw.color.a.isNear(0.18f) &&
                    draw.contains(playerCenterX, playerCenterY)
            },
            "player tile should retain a brighter local core so the hero remains the map focal point",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 89f..92f &&
                    draw.height in 42f..45f &&
                    draw.color.a.isNear(0.064f) &&
                    draw.contains(playerCenterX, playerCenterY)
            },
            "player focal light should veil nearby grid seams with warm stone so the hero pool reads as material, not only glow over lattice",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 68f..71f &&
                    draw.height in 3f..5f &&
                    draw.color.a.isNear(0.070f) &&
                    draw.x < playerCenterX &&
                    draw.x + draw.width > playerCenterX
            },
            "player focal light should carry a short worn lip that breaks the nearest seam under the lantern pool",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 89f..92f &&
                    draw.height in 42f..45f &&
                    draw.color.a.isNear(0.048f) &&
                    draw.contains(torchCenterX, torchCenterY)
            },
            "torch pools should also drop out local grid seams so wall lights feel embedded in stone rather than pasted on top",
        )
    }

    @Test
    fun `render canvas gives player tile forged focal brackets`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(4, 3)
        val cells =
            (0 until 9).flatMap { x ->
                (0 until 8).map { y ->
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 9, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val playerRect = summary.viewport.tileRect(playerTile)
        val cornerTargets =
            listOf(
                playerRect.x + 5f to playerRect.y + 5f,
                playerRect.x + playerRect.width - 5f to playerRect.y + 5f,
                playerRect.x + 5f to playerRect.y + playerRect.height - 5f,
                playerRect.x + playerRect.width - 5f to playerRect.y + playerRect.height - 5f,
            )
        val litCornerCount =
            cornerTargets.count { (targetX, targetY) ->
                canvas.rectDraws.any { draw ->
                    draw.color.a.isNear(0.615f) &&
                        draw.width in 2f..9f &&
                        draw.height in 2f..9f &&
                        draw.contains(targetX, targetY)
                }
            }

        assertEquals(
            4,
            litCornerCount,
            "player indicator should add forged warm corner brackets so the hero tile reads as the first-glance focal anchor, not only a flat square outline",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.315f) &&
                    draw.width in 15f..20f &&
                    draw.height in 2f..4f &&
                    draw.contains(playerRect.x + playerRect.width / 2f, playerRect.y + 7f)
            },
            "player indicator should keep a compact dark underfoot shelf that grounds the focal frame without covering the actor sprite",
        )
    }

    @Test
    fun `render canvas carves cool focal clarity under player light`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(4, 3)
        val cells =
            (0 until 9).flatMap { x ->
                (0 until 8).map { y ->
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 9, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val playerRect = summary.viewport.tileRect(playerTile)
        val playerCenterX = playerRect.x + playerRect.width / 2f
        val playerCenterY = playerRect.y + playerRect.height / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 77f..80f &&
                    draw.height in 7f..9f &&
                    draw.color.a.isNear(0.117f) &&
                    draw.contains(playerCenterX, playerCenterY - playerRect.height * 0.54f)
            },
            "player focal light should carve a compact cool undercut below the warm pool so the hero area reads as clear stone instead of amber fog over a grid",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 44f..47f &&
                    draw.height in 1f..3f &&
                    draw.color.a.isNear(0.067f) &&
                    draw.contains(playerCenterX + playerRect.width * 0.18f, playerCenterY + playerRect.height * 0.43f)
            },
            "player focal clarity should keep a small worn-stone lip so the cool cut reads as dungeon material rather than a UI shadow",
        )
    }

    @Test
    fun `render canvas casts depth shadows from visible room into hidden stage`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(4, 4)
        val cells =
            (0 until 9).flatMap { x ->
                (0 until 9).map { y ->
                    val visibleRoom = x in 2..6 && y in 2..6
                    val wall = visibleRoom && (x == 2 || x == 6 || y == 2 || y == 6)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = if (visibleRoom) CellVisibilitySnapshot.VISIBLE else CellVisibilitySnapshot.HIDDEN,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 9, height = 9, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val topWall = summary.viewport.tileRect(com.ktome.core.map.Point(4, 6))
        val rightWall = summary.viewport.tileRect(com.ktome.core.map.Point(6, 4))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 34f..38f &&
                    draw.height in 20f..24f &&
                    draw.color.a.isNear(0.36f) &&
                    draw.contains(topWall.x + topWall.width / 2f, topWall.y + topWall.height + 6f)
            },
            "visible room walls should cast a soft shadow into hidden tiles so the room silhouette does not read as a flat rectangle on a grid",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 20f..24f &&
                    draw.height in 34f..38f &&
                    draw.color.a.isNear(0.32f) &&
                    draw.contains(rightWall.x + rightWall.width + 6f, rightWall.y + rightWall.height / 2f)
            },
            "side walls should also push darkness into hidden stage space to break the uniform background grid",
        )
    }

    @Test
    fun `render canvas suppresses broad hidden stage grid outside visible room`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 11).flatMap { x ->
                (0 until 9).map { y ->
                    val visibleRoom = x in 4..6 && y in 3..5
                    val wall = visibleRoom && (x == 4 || x == 6 || y == 3 || y == 5)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = if (visibleRoom) CellVisibilitySnapshot.VISIBLE else CellVisibilitySnapshot.HIDDEN,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 11, height = 9, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val roomCenter = summary.viewport.tileRect(playerTile)
        val roomCenterX = roomCenter.x + roomCenter.width / 2f
        val roomCenterY = roomCenter.y + roomCenter.height / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 90f &&
                    draw.height > 230f &&
                    draw.color.a.isNear(0.41f) &&
                    draw.contains(roomCenter.x - 80f, roomCenterY)
            },
            "hidden stage beside the visible room should receive a broad dark veil so backdrop grid does not compete with the authored room silhouette",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.41f) && draw.contains(roomCenterX, roomCenterY)
            },
            "hidden-stage suppression must not cover the visible room focal area",
        )
        val deepPocketDraws = canvas.rectDraws.filter { draw -> draw.color.a.isNear(0.29f) }
        assertTrue(
            deepPocketDraws.any { draw ->
                draw.width > 240f &&
                    draw.height > 120f &&
                    draw.x + draw.width < roomCenterX - 48f &&
                    draw.y < roomCenterY
            },
            "hidden stage should add an irregular deep pocket outside the lower-left room edge so the authored room emerges from darkness instead of a visible rectangular grid",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.29f) && draw.contains(roomCenterX, roomCenterY)
            },
            "deep hidden-stage pockets must stay outside the visible room focal area",
        )
        val shoulderPocketDraws = canvas.rectDraws.filter { draw -> draw.color.a.isNear(0.255f) }
        assertTrue(
            shoulderPocketDraws.any { draw ->
                draw.width > 170f &&
                    draw.height > 42f &&
                    draw.x + draw.width < roomCenterX - 48f &&
                    draw.y < roomCenterY &&
                    draw.y + draw.height > roomCenterY
            },
            "hidden stage should add a second asymmetric deep pocket along the left room shoulder so darkness does not read as one uniform rectangular veil",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.255f) && draw.contains(roomCenterX, roomCenterY)
            },
            "upper hidden-stage pockets must stay outside the visible room focal area",
        )
        val bottomBasinDraws = canvas.rectDraws.filter { draw -> draw.color.a.isNear(0.245f) }
        assertTrue(
            bottomBasinDraws.any { draw ->
                draw.width > 300f &&
                    draw.height > 70f &&
                    draw.x < roomCenterX &&
                    draw.x + draw.width > roomCenterX &&
                    draw.y + draw.height < roomCenterY - 48f
            },
            "hidden stage below the visible room should receive a broad void basin so the lower backdrop recedes as darkness instead of a regular stage grid",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.245f) && draw.contains(roomCenterX, roomCenterY)
            },
            "bottom hidden-stage basin must stay outside the visible room focal area",
        )
        val upperLeftVaultDraws = canvas.rectDraws.filter { draw -> draw.color.a.isNear(0.268f) }
        assertTrue(
            upperLeftVaultDraws.any { draw ->
                draw.width > 155f &&
                    draw.height > 78f &&
                    draw.x + draw.width < roomCenterX - 28f &&
                    draw.y > roomCenterY
            },
            "hidden stage should add an upper-left vault pocket so the room perimeter emerges from layered darkness instead of a rectangular backdrop veil",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.268f) && draw.contains(roomCenterX, roomCenterY)
            },
            "upper-left hidden-stage vault pockets must stay outside the visible room focal area",
        )
        val rightShoulderVoidDraws = canvas.rectDraws.filter { draw -> draw.color.a.isNear(0.222f) }
        assertTrue(
            rightShoulderVoidDraws.any { draw ->
                draw.width > 150f &&
                    draw.height > 42f &&
                    draw.x > roomCenterX + 48f &&
                    draw.y < roomCenterY &&
                    draw.y + draw.height > roomCenterY
            },
            "hidden stage should add an asymmetric right-side void shoulder so the visible room is framed by non-rectangular darkness rather than a flat stage grid",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.222f) && draw.contains(roomCenterX, roomCenterY)
            },
            "right-side hidden-stage void shoulders must stay outside the visible room focal area",
        )
    }

    @Test
    fun `render canvas breaks hidden stage grid with staggered void cascades`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 11).flatMap { x ->
                (0 until 9).map { y ->
                    val visibleRoom = x in 4..6 && y in 3..5
                    val wall = visibleRoom && (x == 4 || x == 6 || y == 3 || y == 5)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = if (visibleRoom) CellVisibilitySnapshot.VISIBLE else CellVisibilitySnapshot.HIDDEN,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 11, height = 9, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val roomCenter = summary.viewport.tileRect(playerTile)
        val roomCenterX = roomCenter.x + roomCenter.width / 2f
        val roomCenterY = roomCenter.y + roomCenter.height / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 120f &&
                    draw.height > 70f &&
                    draw.color.a.isNear(0.232f) &&
                    draw.x + draw.width < roomCenterX - 36f &&
                    draw.contains(roomCenter.x - 96f, roomCenterY)
            },
            "left hidden stage should receive a staggered deep void cascade so the backdrop stops reading as a regular rectangular grid",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 190f &&
                    draw.height > 45f &&
                    draw.color.a.isNear(0.218f) &&
                    draw.y + draw.height < roomCenterY - 32f &&
                    draw.contains(roomCenterX - 72f, roomCenter.y - 52f)
            },
            "lower hidden stage should receive a cross-axis void shelf that interrupts the long bottom grid lanes without touching the room focus",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                (draw.color.a.isNear(0.232f) || draw.color.a.isNear(0.218f)) &&
                    draw.contains(roomCenterX, roomCenterY)
            },
            "staggered hidden-stage cascades must stay outside the playable focal center",
        )
    }

    @Test
    fun `render canvas keeps hidden stage secondary veils cool neutral instead of moss green`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 11).flatMap { x ->
                (0 until 9).map { y ->
                    val visibleRoom = x in 4..6 && y in 3..5
                    val wall = visibleRoom && (x == 4 || x == 6 || y == 3 || y == 5)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = if (visibleRoom) CellVisibilitySnapshot.VISIBLE else CellVisibilitySnapshot.HIDDEN,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(width = 11, height = 9, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val mossTintVeils =
            canvas.rectDraws.filter { draw ->
                draw.color.r.isNear(11f / 255f) &&
                    draw.color.g.isNear(14f / 255f) &&
                    draw.color.b.isNear(11f / 255f)
            }
        assertTrue(
            mossTintVeils.isEmpty(),
            "hidden-stage secondary veils should not use moss-green #0B0E0B curtains; dark void pressure must stay cool neutral",
        )

        val coolNeutralVeils =
            canvas.rectDraws.filter { draw ->
                draw.color.r.isNear(5f / 255f) &&
                    draw.color.g.isNear(6f / 255f) &&
                    draw.color.b.isNear(7f / 255f)
            }
        assertTrue(
            coolNeutralVeils.size >= 5,
            "hidden-stage secondary veils should be reauthored as cool-neutral #050607 layers instead of deleting the darkness pass",
        )
    }

    @Test
    fun `render canvas anchors far hidden stage with neutral void ballast`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 11).flatMap { x ->
                (0 until 9).map { y ->
                    val visibleRoom = x in 4..6 && y in 3..5
                    val wall = visibleRoom && (x == 4 || x == 6 || y == 3 || y == 5)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = if (visibleRoom) CellVisibilitySnapshot.VISIBLE else CellVisibilitySnapshot.HIDDEN,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 11, height = 9, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val roomCenter = summary.viewport.tileRect(playerTile)
        val roomCenterX = roomCenter.x + roomCenter.width / 2f
        val roomCenterY = roomCenter.y + roomCenter.height / 2f
        fun RecordingTileCanvas.RectDraw.isNeutralVoidBallast(alpha: Float): Boolean =
            color.r.isNear(1f / 255f) &&
                color.g.isNear(1f / 255f) &&
                color.b.isNear(1f / 255f) &&
                color.a.isNear(alpha)

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.isNeutralVoidBallast(0.318f) &&
                    draw.width > 150f &&
                    draw.height > 250f &&
                    draw.x + draw.width < roomCenterX - 38f &&
                    draw.y < roomCenterY &&
                    draw.y + draw.height > roomCenterY
            },
            "far-left hidden stage should receive a high-opacity neutral void ballast so greenish stage fog recedes behind the authored room",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.isNeutralVoidBallast(0.302f) &&
                    draw.width > 260f &&
                    draw.height > 80f &&
                    draw.x < roomCenterX &&
                    draw.x + draw.width > roomCenterX &&
                    draw.y + draw.height < roomCenterY - 40f
            },
            "lower hidden stage should receive a broad neutral void ballast so bottom grid lanes fall behind the visible room silhouette",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                (draw.isNeutralVoidBallast(0.318f) || draw.isNeutralVoidBallast(0.302f)) &&
                    draw.contains(roomCenterX, roomCenterY)
            },
            "neutral void ballast must not cover the playable focal center",
        )
    }

    @Test
    fun `render canvas collars visible room with asymmetric void pressure`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 11).flatMap { x ->
                (0 until 9).map { y ->
                    val visibleRoom = x in 4..6 && y in 3..5
                    val wall = visibleRoom && (x == 4 || x == 6 || y == 3 || y == 5)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = if (visibleRoom) CellVisibilitySnapshot.VISIBLE else CellVisibilitySnapshot.HIDDEN,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 11, height = 9, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val leftWall = summary.viewport.tileRect(com.ktome.core.map.Point(4, 4))
        val upperWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 3))
        val playerRect = summary.viewport.tileRect(playerTile)
        val playerCenterX = playerRect.x + playerRect.width / 2f
        val playerCenterY = playerRect.y + playerRect.height / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 32f..35f &&
                    draw.height in 49f..52f &&
                    draw.color.a.isNear(0.205f) &&
                    draw.contains(leftWall.x - 18f, leftWall.y + leftWall.height * 0.52f)
            },
            "hidden stage should press a compact dark collar against the visible room edge so the room reads carved from darkness instead of floating over a visible grid",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 77f..81f &&
                    draw.height in 13f..16f &&
                    draw.color.a.isNear(0.188f) &&
                    draw.contains(upperWall.x + upperWall.width * 0.50f, upperWall.y + upperWall.height + 8f)
            },
            "upper room perimeter should get an asymmetric void crown that breaks the straight hidden-stage lattice near the doorway silhouette",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 1f..3f &&
                    draw.height in 42f..45f &&
                    draw.color.a.isNear(0.066f) &&
                    draw.contains(leftWall.x - 5f, leftWall.y + leftWall.height * 0.56f)
            },
            "dark collars should keep a restrained worn-stone lip at the aperture so the edge reads as masonry pressure rather than a flat black mask",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                (draw.color.a.isNear(0.205f) || draw.color.a.isNear(0.188f)) &&
                    draw.contains(playerCenterX, playerCenterY)
            },
            "void collars must remain outside the playable focal center",
        )
    }

    @Test
    fun `render canvas feathers hidden darkness into room silhouette with ragged fog teeth`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 11).flatMap { x ->
                (0 until 9).map { y ->
                    val visibleRoom = x in 4..6 && y in 3..5
                    val wall = visibleRoom && (x == 4 || x == 6 || y == 3 || y == 5)
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = if (visibleRoom) CellVisibilitySnapshot.VISIBLE else CellVisibilitySnapshot.HIDDEN,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 11, height = 9, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val leftWall = summary.viewport.tileRect(com.ktome.core.map.Point(4, 4))
        val rightWall = summary.viewport.tileRect(com.ktome.core.map.Point(6, 4))
        val roomCenter = summary.viewport.tileRect(playerTile)
        val roomCenterX = roomCenter.x + roomCenter.width / 2f
        val roomCenterY = roomCenter.y + roomCenter.height / 2f

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 16f..19f &&
                    draw.height in 58f..61f &&
                    draw.color.a.isNear(0.196f) &&
                    draw.contains(leftWall.x - 6f, leftWall.y + leftWall.height * 0.62f)
            },
            "hidden darkness should send a narrow ragged tooth into the left room edge so the silhouette does not stay a clean rectangular cutout",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 11f..14f &&
                    draw.height in 45f..48f &&
                    draw.color.a.isNear(0.172f) &&
                    draw.contains(rightWall.x + rightWall.width + 5f, rightWall.y + rightWall.height * 0.45f)
            },
            "right room edge should receive a different narrow fog tooth so darkness frames the room asymmetrically rather than mirroring one rectangle",
        )
        assertFalse(
            canvas.rectDraws.any { draw ->
                (draw.color.a.isNear(0.196f) || draw.color.a.isNear(0.172f)) &&
                    draw.contains(roomCenterX, roomCenterY)
            },
            "ragged hidden-stage fog teeth must feather only the room boundary, not cover the playable center",
        )
    }

    @Test
    fun `render canvas chips visible room corners into broken dungeon silhouette`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 10).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 9 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 10, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val screenLowerLeft = summary.viewport.tileRect(com.ktome.core.map.Point(0, 7))
        val screenUpperRight = summary.viewport.tileRect(com.ktome.core.map.Point(9, 0))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 52f..58f &&
                    draw.height in 46f..54f &&
                    draw.color.a.isNear(0.43f) &&
                    draw.contains(screenLowerLeft.x + 18f, screenLowerLeft.y + 18f)
            },
            "visible room outer corners should be chipped with dark broken masonry so the room does not remain a perfect rectangular block",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 19f..22f &&
                    draw.height in 5f..7f &&
                    draw.color.a.isNear(0.13f) &&
                    draw.contains(screenUpperRight.x + screenUpperRight.width - 18f, screenUpperRight.y + screenUpperRight.height - 8f)
            },
            "corner breakup should include a small worn stone lip so the darkness reads as broken wall material rather than a flat mask",
        )
    }

    @Test
    fun `render canvas breaks straight room edges with jagged aperture bites`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val screenLeftWall = summary.viewport.tileRect(com.ktome.core.map.Point(0, 4))
        val screenRightWall = summary.viewport.tileRect(com.ktome.core.map.Point(11, 3))
        val screenUpperWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 0))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 28f..31f &&
                    draw.height in 48f..51f &&
                    draw.color.a.isNear(0.36f) &&
                    draw.contains(screenLeftWall.x + 10f, screenLeftWall.y + 20f)
            },
            "visible room left edge should have a jagged dark bite so the room no longer reads as a perfect rectangular block",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 30f..33f &&
                    draw.height in 46f..49f &&
                    draw.color.a.isNear(0.355f) &&
                    draw.contains(screenRightWall.x + screenRightWall.width - 10f, screenRightWall.y + 18f)
            },
            "visible room right edge should receive an offset bite so silhouette breakup is asymmetric, not just four dark corners",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 76f..80f &&
                    draw.height in 25f..28f &&
                    draw.color.a.isNear(0.305f) &&
                    draw.contains(screenUpperWall.x + screenUpperWall.width / 2f, screenUpperWall.y + screenUpperWall.height - 8f)
            },
            "visible room upper wall should include a broad chipped crown bite that interrupts the long straight top edge",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 28f..32f &&
                    draw.height in 4f..7f &&
                    draw.color.a.isNear(0.112f) &&
                    draw.contains(screenLeftWall.x + 22f, screenLeftWall.y + 28f)
            },
            "jagged aperture bites should include a small worn stone lip so the darkness reads as broken masonry rather than a flat mask",
        )
    }

    @Test
    fun `render canvas cools visible room edges with asymmetric fog pressure`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val rightInterior = summary.viewport.tileRect(com.ktome.core.map.Point(10, 3))
        val lowerLeftInterior = summary.viewport.tileRect(com.ktome.core.map.Point(2, 6))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 76f..79f &&
                    draw.height in 185f..189f &&
                    draw.color.a.isNear(0.104f) &&
                    draw.contains(rightInterior.x + 16f, rightInterior.y + 16f)
            },
            "visible room right edge should carry a cool fog pressure veil so the torch field falls back into dungeon darkness instead of staying evenly lit",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 151f..153f &&
                    draw.height in 44f..47f &&
                    draw.color.a.isNear(0.092f) &&
                    draw.contains(lowerLeftInterior.x + 16f, lowerLeftInterior.y + 16f)
            },
            "visible room lower-left floor should receive a broad asymmetric dark basin so the room lighting reads directional, not like a flat warm wash",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 98f..101f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.050f) &&
                    draw.contains(lowerLeftInterior.x + 38f, lowerLeftInterior.y + 17f)
            },
            "cool fog pressure should keep a restrained worn-stone lip so the dark basin still reads as material over stone, not a UI shadow",
        )
    }

    @Test
    fun `render canvas lays staggered stone slabs across visible room floor`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val centerFloor = summary.viewport.tileRect(com.ktome.core.map.Point(5, 4))
        val upperFloor = summary.viewport.tileRect(com.ktome.core.map.Point(7, 2))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 70f..74f &&
                    draw.height in 22f..25f &&
                    draw.color.a.isNear(0.096f) &&
                    draw.contains(centerFloor.x + centerFloor.width / 2f, centerFloor.y + centerFloor.height / 2f)
            },
            "visible room floor should include staggered wide stone slabs so the main room no longer reads as uniform single-cell checkerboard",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 48f..52f &&
                    draw.height in 4f..6f &&
                    draw.color.a.isNear(0.118f) &&
                    draw.contains(upperFloor.x + upperFloor.width / 2f, upperFloor.y + upperFloor.height * 0.82f)
            },
            "staggered slabs should add short dark mortar cuts that interrupt long grid lines without covering the floor art",
        )
    }

    @Test
    fun `render canvas breaks uniform room grid joints with chipped mortar caps`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val verticalJointFloor = summary.viewport.tileRect(com.ktome.core.map.Point(4, 3))
        val horizontalJointFloor = summary.viewport.tileRect(com.ktome.core.map.Point(4, 4))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 9f..11f &&
                    draw.height in 20f..23f &&
                    draw.color.a.isNear(0.112f) &&
                    draw.contains(verticalJointFloor.x + verticalJointFloor.width - 1f, verticalJointFloor.y + verticalJointFloor.height * 0.30f)
            },
            "visible room floor should bridge selected internal vertical seams with stone-colored caps so grid lines stop reading as perfectly regular",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 22f..24f &&
                    draw.height in 9f..11f &&
                    draw.color.a.isNear(0.098f) &&
                    draw.contains(horizontalJointFloor.x + horizontalJointFloor.width * 0.52f, horizontalJointFloor.y + horizontalJointFloor.height - 2f)
            },
            "visible room floor should include chipped horizontal mortar caps that interrupt long row seams without hiding floor tile readability",
        )
    }

    @Test
    fun `render canvas adds localized grime and broken stone detail to visible room floor`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val centerFloor = summary.viewport.tileRect(com.ktome.core.map.Point(4, 3))
        val lowerFloor = summary.viewport.tileRect(com.ktome.core.map.Point(8, 5))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 46f..50f &&
                    draw.height in 15f..19f &&
                    draw.color.a.isNear(0.142f) &&
                    draw.contains(centerFloor.x + centerFloor.width * 0.55f, centerFloor.y + centerFloor.height * 0.60f)
            },
            "visible room floor should include localized dark grime and chipped stone scars instead of only uniform grid overlays",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 24f..28f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.086f) &&
                    draw.contains(lowerFloor.x + lowerFloor.width * 0.42f, lowerFloor.y + lowerFloor.height * 0.54f)
            },
            "localized broken stone detail should include small worn highlights so floor damage reads as material texture rather than a flat stain",
        )
        val microDebrisDraws =
            canvas.rectDraws.filter { draw ->
                draw.width in 3f..6f &&
                    draw.height in 2f..5f &&
                    draw.color.a.isNear(0.072f)
            }
        assertTrue(
            microDebrisDraws.size >= 5,
            "visible room floor should scatter multiple small chipped-stone debris marks so the ground gains hand-authored detail density instead of only broad stains",
        )
        val wornChipHighlights =
            canvas.rectDraws.filter { draw ->
                draw.width in 2f..5f &&
                    draw.height in 1f..3f &&
                    draw.color.a.isNear(0.058f)
            }
        assertTrue(
            wornChipHighlights.size >= 4,
            "micro debris should include short warm worn edges so chips read as stone fragments rather than random dark pixels",
        )
        val hairlineEtches =
            canvas.rectDraws.filter { draw ->
                draw.width in 9f..15f &&
                    draw.height == 1f &&
                    draw.color.a.isNear(0.041f)
            }
        assertTrue(
            hairlineEtches.size >= 24,
            "visible room floor should carry fine low-alpha hairline etches so individual cells gain hand-cut stone grain at runtime size",
        )
        val pittedStoneSpecks =
            canvas.rectDraws.filter { draw ->
                draw.width in 2f..3f &&
                    draw.height in 2f..3f &&
                    draw.color.a.isNear(0.046f)
            }
        assertTrue(
            pittedStoneSpecks.size >= 10,
            "visible room floor should include a field of tiny pitted stone specks so the surface gains dense hand-worked grain at runtime size",
        )
        val shortCutMarks =
            canvas.rectDraws.filter { draw ->
                draw.width in 6f..10f &&
                    draw.height in 1f..2f &&
                    draw.color.a.isNear(0.052f)
            }
        assertTrue(
            shortCutMarks.size >= 6,
            "visible room floor should add short off-grid cut marks so fine detail breaks the remaining regular lattice without hiding actors or loot",
        )
    }

    @Test
    fun `render canvas scatters rubble along visible wall floor contacts`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val screenUpperWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 0))
        val screenLeftWall = summary.viewport.tileRect(com.ktome.core.map.Point(0, 4))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 35f..39f &&
                    draw.height in 9f..12f &&
                    draw.color.a.isNear(0.185f) &&
                    draw.contains(screenUpperWall.x + screenUpperWall.width / 2f, screenUpperWall.y + screenUpperWall.height - 8f)
            },
            "long visible wall contacts should include localized dark rubble clusters so floor-wall edges stop reading as clean grid lines",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 7f..10f &&
                    draw.height in 4f..7f &&
                    draw.color.a.isNear(0.145f) &&
                    draw.contains(screenLeftWall.x + screenLeftWall.width - 7f, screenLeftWall.y + screenLeftWall.height * 0.58f)
            },
            "wall-foot rubble should include small warm stone chips that tie the wall mass back into the floor material",
        )
    }

    @Test
    fun `render canvas packs gritty contact occlusion under visible wall feet`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val upperInteriorFloor = summary.viewport.tileRect(com.ktome.core.map.Point(5, 1))
        val lowerInteriorFloor = summary.viewport.tileRect(com.ktome.core.map.Point(5, 6))
        val rightInteriorFloor = summary.viewport.tileRect(com.ktome.core.map.Point(10, 4))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 229f..235f &&
                    draw.height in 5f..7f &&
                    draw.color.a.isNear(0.133f) &&
                    draw.contains(upperInteriorFloor.x + upperInteriorFloor.width / 2f, upperInteriorFloor.y + 5f)
            },
            "visible wall feet should press a broken dark contact band into the adjacent floor so wall mass reads as settled stone instead of a clean outline",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 6f..8f &&
                    draw.height in 83f..87f &&
                    draw.color.a.isNear(0.107f) &&
                    draw.contains(rightInteriorFloor.x + rightInteriorFloor.width - 4f, rightInteriorFloor.y + rightInteriorFloor.height / 2f)
            },
            "side wall feet should add a narrow gritty occlusion run on the adjacent floor so vertical walls keep their weight at room scale",
        )
        val grittyContactChips =
            canvas.rectDraws.filter { draw ->
                draw.width in 3f..6f &&
                    draw.height in 3f..5f &&
                    draw.color.a.isNear(0.097f) &&
                    (
                        draw.contains(upperInteriorFloor.x + upperInteriorFloor.width / 2f, upperInteriorFloor.y + 9f) ||
                            draw.contains(lowerInteriorFloor.x + upperInteriorFloor.width / 2f, lowerInteriorFloor.y + lowerInteriorFloor.height - 9f)
                    )
            }
        assertTrue(
            grittyContactChips.size >= 2,
            "wall-floor contacts should include small deterministic grit chips so the edge reads as authored masonry texture rather than a flat dark strip",
        )
    }

    @Test
    fun `render canvas raises visible wall runs with crown and interior face`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val screenUpperWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 0))
        val screenLowerWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 7))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 318f..326f &&
                    draw.height in 17f..20f &&
                    draw.color.a.isNear(0.235f) &&
                    draw.contains(screenUpperWall.x + screenUpperWall.width / 2f, screenUpperWall.y + screenUpperWall.height - 8f)
            },
            "long visible wall runs should gain a continuous raised crown so perimeter walls read as thick masonry rather than flat tile edges",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 294f..306f &&
                    draw.height in 5f..7f &&
                    draw.color.a.isNear(0.155f) &&
                    draw.contains(screenLowerWall.x + screenLowerWall.width / 2f, screenLowerWall.y + 9f)
            },
            "wall runs should include a warm worn interior face that separates floor from wall without depending only on the dark grid line",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 335f &&
                    draw.height in 20f..24f &&
                    draw.color.a.isNear(0.278f) &&
                    draw.contains(screenUpperWall.x + screenUpperWall.width / 2f, screenUpperWall.y + screenUpperWall.height - 11f)
            },
            "long visible wall runs should cast a deeper underface shadow so walls read as raised masonry mass rather than a thin tile border",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width > 310f &&
                    draw.height in 2f..5f &&
                    draw.color.a.isNear(0.092f) &&
                    draw.contains(screenUpperWall.x + screenUpperWall.width / 2f, screenUpperWall.y + screenUpperWall.height - 17f)
            },
            "raised wall faces should carry a subtle worn lip highlight that supports authored stone thickness without flattening into a grid line",
        )
    }

    @Test
    fun `render canvas deepens visible wall height with interior shadow terraces`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val upperInteriorFloor = summary.viewport.tileRect(com.ktome.core.map.Point(5, 1))
        val lowerInteriorFloor = summary.viewport.tileRect(com.ktome.core.map.Point(5, 6))
        val rightInteriorFloor = summary.viewport.tileRect(com.ktome.core.map.Point(10, 4))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 310f..314f &&
                    draw.height in 11f..13f &&
                    draw.color.a.isNear(0.127f) &&
                    draw.contains(upperInteriorFloor.x + upperInteriorFloor.width / 2f, upperInteriorFloor.y + 7f)
            },
            "raised top wall runs should cast a broad interior floor shadow terrace so the wall reads as elevated masonry rather than a flat outline",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 273f..277f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.069f) &&
                    draw.contains(lowerInteriorFloor.x + lowerInteriorFloor.width / 2f, lowerInteriorFloor.y + lowerInteriorFloor.height - 8f)
            },
            "raised lower wall runs should include a narrow worn-stone ledge on the adjacent floor to sell the high-low transition without adding a grid line",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 11f..13f &&
                    draw.height in 179f..183f &&
                    draw.color.a.isNear(0.113f) &&
                    draw.contains(rightInteriorFloor.x + rightInteriorFloor.width - 7f, rightInteriorFloor.y + rightInteriorFloor.height / 2f)
            },
            "raised side wall runs should project a slim interior occlusion strip into the floor so side walls read as thick stone mass",
        )
    }

    @Test
    fun `render canvas articulates visible wall crowns with uneven cap blocks`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val screenUpperWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 0))
        val screenRightWall = summary.viewport.tileRect(com.ktome.core.map.Point(11, 4))
        val screenUpperLeftCorner = summary.viewport.tileRect(com.ktome.core.map.Point(0, 0))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 20f..23f &&
                    draw.height in 7f..9f &&
                    draw.color.a.isNear(0.168f) &&
                    draw.contains(screenUpperWall.x + screenUpperWall.width * 0.52f, screenUpperWall.y + screenUpperWall.height - 13f)
            },
            "visible wall crowns should break into uneven cap-stone blocks so long walls stop reading as flat continuous strips",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 6f..8f &&
                    draw.height in 19f..22f &&
                    draw.color.a.isNear(0.154f) &&
                    draw.contains(screenRightWall.x + 7f, screenRightWall.y + screenRightWall.height * 0.54f)
            },
            "visible side walls should gain small masonry notches that imply stone thickness instead of a repeated one-tile column",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 25f..29f &&
                    draw.height in 25f..29f &&
                    draw.color.a.isNear(0.215f) &&
                    draw.contains(screenUpperLeftCorner.x + screenUpperLeftCorner.width - 8f, screenUpperLeftCorner.y + screenUpperLeftCorner.height - 8f)
            },
            "visible wall corners should gain dark masonry return blocks so room corners read as thick buttresses rather than thin tile intersections",
        )
    }

    @Test
    fun `render canvas lays granular masonry courses along visible wall faces`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val screenUpperWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 0))
        val screenRightWall = summary.viewport.tileRect(com.ktome.core.map.Point(11, 4))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 14f..17f &&
                    draw.height in 4f..6f &&
                    draw.color.a.isNear(0.132f) &&
                    draw.contains(screenUpperWall.x + 13f, screenUpperWall.y + screenUpperWall.height - 19f)
            },
            "visible wall faces should include small masonry course stones so long walls gain hand-built brick density instead of only broad bands",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 4f..6f &&
                    draw.height in 14f..17f &&
                    draw.color.a.isNear(0.126f) &&
                    draw.contains(screenRightWall.x + 8f, screenRightWall.y + screenRightWall.height * 0.55f)
            },
            "side wall faces should carry vertical masonry course stones so columns do not read as repeated flat wall tiles",
        )
        val secondaryMortarTicks =
            canvas.rectDraws.filter { draw ->
                draw.width in 2f..4f &&
                    draw.height in 8f..12f &&
                    draw.color.a.isNear(0.118f)
            }
        assertTrue(
            secondaryMortarTicks.size >= 5,
            "visible wall masonry should include multiple secondary dark mortar ticks so wall faces gain dense hand-built stone grain instead of sparse course markers",
        )
        val warmWornFlecks =
            canvas.rectDraws.filter { draw ->
                draw.width in 7f..11f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.086f)
            }
        assertTrue(
            warmWornFlecks.size >= 4,
            "visible wall masonry should include short warm worn flecks so dense wall grain reads as chipped stone rather than random dark notches",
        )
    }

    @Test
    fun `render canvas adds chipped micro joints across visible wall faces`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val screenUpperWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 0))
        val screenRightWall = summary.viewport.tileRect(com.ktome.core.map.Point(11, 4))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 3f..5f &&
                    draw.height in 3f..5f &&
                    draw.color.a.isNear(0.104f) &&
                    draw.contains(screenUpperWall.x + 13f, screenUpperWall.y + screenUpperWall.height - 13f)
            },
            "visible wall faces should carry compact chipped dark joints so raised blocks read as hand-cut stones rather than smooth smoky bands",
        )
        val warmPinFlecks =
            canvas.rectDraws.filter { draw ->
                draw.width in 5f..7f &&
                    draw.height in 1f..3f &&
                    draw.color.a.isNear(0.073f)
            }
        assertTrue(
            warmPinFlecks.size >= 6,
            "visible wall masonry should add restrained warm pin flecks across long runs so wall faces gain authored worn-stone density without becoming noisy",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 1f..3f &&
                    draw.height in 6f..8f &&
                    draw.color.a.isNear(0.096f) &&
                    draw.contains(screenRightWall.x + 13f, screenRightWall.y + 17f)
            },
            "side wall faces should include narrow vertical chipped joints so thick side boundaries keep the same masonry grain as horizontal walls",
        )
    }

    @Test
    fun `render canvas layers offset block faces inside visible wall masonry runs`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val screenUpperWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 0))
        val screenRightWall = summary.viewport.tileRect(com.ktome.core.map.Point(11, 4))

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 56f..62f &&
                    draw.height in 8f..10f &&
                    draw.color.a.isNear(0.147f) &&
                    draw.contains(screenUpperWall.x + screenUpperWall.width * 0.52f, screenUpperWall.y + screenUpperWall.height - 24f)
            },
            "visible wall faces should include offset raised stone plates so long walls read as stacked masonry blocks rather than a single fog-softened strip",
        )
        val darkMortarReturns =
            canvas.rectDraws.filter { draw ->
                draw.width in 2f..4f &&
                    draw.height in 10f..14f &&
                    draw.color.a.isNear(0.109f)
            }
        assertTrue(
            darkMortarReturns.size >= 3,
            "offset wall blocks should carry dark mortar returns so the raised plates have authored joints instead of floating as flat highlights",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 7f..10f &&
                    draw.height in 39f..44f &&
                    draw.color.a.isNear(0.141f) &&
                    draw.contains(screenRightWall.x + 8f, screenRightWall.y + screenRightWall.height * 0.52f)
            },
            "side visible wall faces should gain stacked vertical stone plates so side walls keep the same block-built relief as horizontal runs",
        )
    }

    @Test
    fun `render canvas breaks long visible wall silhouette with heavy capstone slabs`() {
        val canvas = RecordingTileCanvas()
        val playerTile = com.ktome.core.map.Point(5, 4)
        val cells =
            (0 until 12).flatMap { x ->
                (0 until 8).map { y ->
                    val wall = x == 0 || x == 11 || y == 0 || y == 7
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = if (wall) "wall" else "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    )
                }
            }

        val summary =
            TileRenderer.renderToCanvas(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(width = 12, height = 8, cells = cells, playerX = playerTile.x, playerY = playerTile.y),
                overlayState = OverlayState(mode = UiMode.MAP),
                canvas = canvas,
                cellWidth = 32f,
                cellHeight = 32f,
            )

        val screenUpperWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 0))
        val screenLowerWall = summary.viewport.tileRect(com.ktome.core.map.Point(5, 7))
        val capstoneCandidates =
            canvas.rectDraws
                .filter { draw -> draw.color.a.isNear(0.183f) || draw.color.a.isNear(0.121f) || draw.color.a.isNear(0.157f) }
                .joinToString { draw -> "${draw.width}x${draw.height}@${draw.x},${draw.y}/a=${draw.color.a}" }

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 79f..83f &&
                    draw.height in 11f..13f &&
                    draw.color.a.isNear(0.183f) &&
                    draw.contains(screenUpperWall.x + screenUpperWall.width * 0.58f, screenUpperWall.y + screenUpperWall.height - 26f)
            },
            "long visible wall crowns should gain a heavy broken capstone slab so the wall silhouette reads hand-built instead of a smooth smoky strip; candidates=$capstoneCandidates",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 50f..54f &&
                    draw.height in 2f..4f &&
                    draw.color.a.isNear(0.121f) &&
                    draw.contains(screenUpperWall.x + screenUpperWall.width * 0.92f, screenUpperWall.y + screenUpperWall.height - 20f)
            },
            "heavy capstone slabs should carry a short worn lip highlight so they read as stone faces rather than another dark veil",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.width in 2f..4f &&
                    draw.height in 16f..18f &&
                    draw.color.a.isNear(0.157f) &&
                    draw.contains(screenLowerWall.x + screenLowerWall.width * 1.70f, screenLowerWall.y + 23f)
            },
            "lower visible wall crowns should include a dark vertical cleft that breaks the straight cap band into individual stones",
        )
    }

    @Test
    fun `render canvas bleeds terrain tiles to suppress hard checkerboard seams`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(width = 2, height = 1),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val terrainDraw = canvas.assetDraws.first { draw -> draw.asset.entry.category == "tile_ground" }
        assertTrue(terrainDraw.width >= 35f)
        assertTrue(terrainDraw.height >= 35f)
        assertTrue(terrainDraw.alpha >= 0.96f)
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
    fun drawsGroundLootMarkerAboveTerrainAndBelowBlockingActorBadge() {
        val canvas = RecordingTileCanvas()
        val rareItem =
            ItemRenderSnapshot(
                baseItemId = "short_sword",
                nameKey = "item.short_sword.name",
                typeId = "WEAPON",
                iconKey = "item.short_sword.icon",
                qualityTierId = "RARE",
            )
        val base =
            sampleSnapshot(
                width = 2,
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
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.forest_edge.ground_01",
                            actorEntityId = 2,
                            items = listOf(rareItem),
                        ),
                    ),
            )
        val snapshot =
            base.copy(
                actors =
                    base.actors +
                        ActorRenderSnapshot(
                            entityId = 2,
                            x = 1,
                            y = 0,
                            visualKey = "actor.arcanist",
                            nameKey = "profession.arcanist.name",
                            isPlayer = false,
                            roleKind = ActorRoleKindSnapshot.GENERIC,
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

        val terrainIndex = canvas.assetDraws.indexOfFirst { draw -> draw.asset.resolvedKey == "tileset.forest_edge.ground_01" }
        val actorIndex = canvas.assetDraws.indexOfFirst { draw -> draw.asset.resolvedKey == "actor.arcanist" }
        val lootMarkerIndex = canvas.assetDraws.indexOfFirst { draw -> draw.asset.resolvedKey == "item.short_sword.icon" }
        assertTrue(terrainIndex in 0 until actorIndex)
        assertTrue(actorIndex in 0 until lootMarkerIndex)
        assertTrue(canvas.flushes.indexOf(TileLayerFlushReason.MAP_ACTORS) < canvas.flushes.indexOf(TileLayerFlushReason.MAP_GROUND_LOOT_MARKERS))
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

        assertTrue(gaugeBackgrounds.all { draw -> draw.height >= 18f })
        assertTrue(
            canvas.rectDraws.any { draw -> draw.width > 110f && draw.height >= 58f && draw.color.a.isNear(0.62f) },
            "hero card should use a dedicated dark gauge well so HP/resource bars read as intentional UI, not loose strips over art",
        )
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
        assertEquals("CAPTION", guardLabel.style.name, "bottom action labels should use caption hierarchy instead of full body text")
        val guardBounds =
            TileRenderer.textApproximationBounds(
                style = guardLabel.style,
                text = guardLabel.text,
                x = guardLabel.x,
                y = guardLabel.y,
            )
        val guardSlot = layout.demoShell.bottomDeck.actionSlotBounds[2]

        assertTrue(guardLabel.x + guardBounds[2] <= guardSlot.right - 4f)
        val actionDeck = layout.demoShell.bottomDeck.actionDeck
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.64f) &&
                    draw.width > actionDeck.width * 0.80f &&
                    draw.height > actionDeck.height * 0.58f &&
                    draw.x >= actionDeck.x &&
                    draw.x + draw.width <= actionDeck.right &&
                    draw.y >= actionDeck.y &&
                    draw.y + draw.height <= actionDeck.top
            },
            "action deck should use one continuous dark tray so the hotbar reads as a deliberate control surface",
        )
        val actionSocketWells =
            layout.demoShell.bottomDeck.actionSlotBounds.count { slotBounds ->
                canvas.rectDraws.any { draw ->
                    draw.color.a.isNear(0.52f) &&
                        draw.width >= slotBounds.width - 10f &&
                        draw.height > slotBounds.height * 0.68f &&
                        draw.x >= slotBounds.x + 3f &&
                        draw.x + draw.width <= slotBounds.right - 3f &&
                        draw.y >= slotBounds.y + 2f &&
                        draw.y + draw.height <= slotBounds.top - 2f
                }
            }
        assertTrue(
            actionSocketWells >= 3,
            "filled action slots should sit in forged socket wells instead of floating as separate icon and label plates",
        )
    }

    @Test
    fun `bottom action deck reads as one forged command console`() {
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
        val actionDeck = layout.demoShell.bottomDeck.actionDeck
        val filledSlots = layout.demoShell.bottomDeck.actionSlotBounds.take(4)
        val commandSpan = filledSlots.last().right - filledSlots.first().x

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.168f) &&
                    draw.width > commandSpan * 0.88f &&
                    draw.height in 26f..34f &&
                    draw.x >= actionDeck.x &&
                    draw.x + draw.width <= actionDeck.right &&
                    draw.y >= actionDeck.y &&
                    draw.y + draw.height <= actionDeck.top
            },
            "action labels should sit on a shared command plinth so the bottom deck reads as one control console, not separate captions",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.112f) &&
                    draw.width > commandSpan * 0.82f &&
                    draw.height in 3f..5f &&
                    draw.x >= actionDeck.x &&
                    draw.x + draw.width <= actionDeck.right &&
                    draw.y >= actionDeck.y &&
                    draw.y + draw.height <= actionDeck.top
            },
            "action sockets should be tied together by a restrained forged rail instead of floating above independent label plates",
        )
        val railRivets =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.136f) &&
                    draw.width in 4f..6f &&
                    draw.height in 4f..6f &&
                    draw.x >= actionDeck.x &&
                    draw.x + draw.width <= actionDeck.right &&
                    draw.y >= actionDeck.y &&
                    draw.y + draw.height <= actionDeck.top
            }
        assertTrue(
            railRivets.size >= filledSlots.size,
            "each action socket should register on the shared command rail with a small worn rivet",
        )
    }

    @Test
    fun `bottom hud panels sit on one forged foundation rail`() {
        val canvas = RecordingTileCanvas()
        val snapshot = sampleSnapshot(width = 18, height = 17)

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.ZH_CN),
            visualResolver = sampleResolver(),
            snapshot = snapshot,
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val bottom = TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, 32f, 32f).demoShell.bottomDeck
        val hudSpan = bottom.logDeck.right - bottom.heroCard.x

        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.144f) &&
                    draw.width > hudSpan * 0.96f &&
                    draw.height > bottom.bounds.height * 0.78f &&
                    draw.x <= bottom.heroCard.x &&
                    draw.x + draw.width >= bottom.logDeck.right &&
                    draw.y >= bottom.bounds.y &&
                    draw.y + draw.height <= bottom.bounds.top
            },
            "hero, action, and log panels should sit on one shared dark foundation slab instead of reading as three disconnected bottom cards",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.118f) &&
                    draw.width > hudSpan * 0.94f &&
                    draw.height in 3f..5f &&
                    draw.x <= bottom.heroCard.x + 2f &&
                    draw.x + draw.width >= bottom.logDeck.right - 2f &&
                    draw.y >= bottom.heroCard.top - 8f &&
                    draw.y <= bottom.heroCard.top
            },
            "bottom HUD needs a continuous forged top rail tying hero, action, and log surfaces into one control shelf",
        )
        val connectorPosts =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.126f) &&
                    draw.width in 4f..6f &&
                    draw.height > bottom.heroCard.height * 0.62f &&
                    draw.y >= bottom.heroCard.y + 10f &&
                    draw.y + draw.height <= bottom.heroCard.top - 10f
            }
        assertTrue(
            connectorPosts.size >= 2,
            "gaps between hero/action/log panels should have restrained vertical connector posts so the bottom HUD reads as one forged assembly",
        )
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
        assertTrue(
            routeHintDraws.all { draw -> draw.style.name == "CAPTION" },
            "bottom log message rows should use caption hierarchy so the log remains secondary to the map and actions",
        )
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
    fun `bottom log renders compact ledger and scroll density cues`() {
        val canvas = RecordingTileCanvas()
        val snapshot =
            sampleSnapshot(
                width = 18,
                height = 17,
                logEvents =
                    listOf(
                        logEvent("log.zone.enter", "zone" to "破碎前哨", "desc" to "泥泞里还有未灭的哨火。"),
                        logEvent("log.objective.activate", "objective" to "稳住前哨交互点"),
                        logEvent("log.reward.route.claimed", "zone" to "渡口", "item" to "铁壁之印"),
                        logEvent("log.passive.hp_regen", "item" to "治愈之印", "amount" to "2"),
                        logEvent("log.boss.enrage", "source" to "哨兵队长"),
                        logEvent("log.zone.mechanic_hint", "hint" to "如果在 Boss 线外拖得太久，这层会持续有巡逻增援补进来"),
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
        val log = layout.demoShell.bottomDeck.logDeck
        val logContent =
            ChromeFramePainter.contentBounds(
                ChromeFrameBounds(log.x, log.y, log.width, log.height),
                ChromeSurfaceKind.Panel,
            )
        val logTextDraws =
            canvas.textDraws.filter { draw ->
                draw.x >= log.x &&
                    draw.x < log.right &&
                    draw.y >= log.y &&
                    draw.y < log.top
            }
        assertTrue(logTextDraws.size >= 4, "test fixture should render enough bottom log rows to exercise density cues")

        val ledgerTicks =
            canvas.rectDraws.filter { draw ->
                draw.color.a.isNear(0.118f) &&
                    draw.width in 5f..9f &&
                    draw.height in 1f..3f &&
                    draw.x >= logContent.x + 4f &&
                    draw.x <= logContent.x + 18f &&
                    draw.y >= logContent.y + 8f &&
                    draw.y <= logContent.top - 8f
            }
        assertTrue(
            ledgerTicks.size >= minOf(4, logTextDraws.size),
            "bottom log should render compact ledger ticks so dense wrapped events read as an event stream instead of a flat text block",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.28f) &&
                    draw.width in 1f..3f &&
                    draw.height > logContent.height * 0.58f &&
                    draw.x >= logContent.right - 8f &&
                    draw.x <= logContent.right - 2f
            },
            "bottom log should include a subdued scroll-density spine on the right edge",
        )
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.a.isNear(0.092f) &&
                    draw.width in 3f..5f &&
                    draw.height in 20f..34f &&
                    draw.x >= logContent.right - 9f &&
                    draw.x <= logContent.right - 1f &&
                    draw.y >= logContent.y + 10f &&
                    draw.y <= logContent.y + 22f
            },
            "bottom log should show a restrained lower thumb marker so recent entries feel anchored to a scrollable ledger",
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
    fun keepsBossTelegraphReadableWhenActorOccupiesCell() {
        val canvas = RecordingTileCanvas()
        val base =
            sampleSnapshot(
                width = 2,
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
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.forest_edge.ground_01",
                            actorEntityId = 2,
                        ),
                    ),
                overlays =
                    listOf(
                        OverlayRenderSnapshot(
                            id = "boss:warning",
                            visualKey = "vfx.boss.warning.sigil_01",
                            previewTurns = 1,
                            dangerLevel = 3,
                            shape = OverlayShapeSnapshot.SINGLE_TILE,
                            sourceAbilityId = "boss_warning",
                            cells = listOf(GridPointSnapshot(1, 0)),
                        ),
                        OverlayRenderSnapshot(
                            id = "ordinary:vfx",
                            visualKey = "vfx.zone.effect.void_pressure_01",
                            previewTurns = 1,
                            dangerLevel = 1,
                            shape = OverlayShapeSnapshot.SINGLE_TILE,
                            sourceAbilityId = "zone_pressure",
                            cells = listOf(GridPointSnapshot(1, 0)),
                        ),
                    ),
            )
        val snapshot =
            base.copy(
                actors =
                    base.actors +
                        ActorRenderSnapshot(
                            entityId = 2,
                            x = 1,
                            y = 0,
                            visualKey = "actor.boss.ashgate_warden",
                            nameKey = "boss.ashgate_warden.name",
                            isPlayer = false,
                            roleKind = ActorRoleKindSnapshot.BOSS,
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

        val ordinaryIndex = canvas.assetDraws.indexOfFirst { draw -> draw.asset.resolvedKey == "vfx.zone.effect.void_pressure_01" }
        val bossWarningIndex = canvas.assetDraws.indexOfFirst { draw -> draw.asset.resolvedKey == "vfx.boss.warning.sigil_01" }
        val bossActorIndex = canvas.assetDraws.indexOfFirst { draw -> draw.asset.resolvedKey == "actor.boss.ashgate_warden" }
        assertTrue(ordinaryIndex in 0 until bossWarningIndex)
        assertTrue(bossWarningIndex in 0 until bossActorIndex)
        assertTrue(canvas.flushes.indexOf(TileLayerFlushReason.MAP_SPRITE_OVERLAYS_AND_TELEGRAPHS) < canvas.flushes.indexOf(TileLayerFlushReason.MAP_ACTORS))
    }

    @Test
    fun rendersPr05InteractablePropsWithDarkManifestEntries() {
        val canvas = RecordingTileCanvas()
        val snapshot =
            sampleSnapshot(width = 2).copy(
                props =
                    listOf(
                        PropRenderSnapshot(
                            id = "stairs:up:test",
                            x = 0,
                            y = 0,
                            propTypeId = "stairs",
                            stairDirectionId = "UP",
                            visualKey = "prop.stairs.up",
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

        val propDraw = canvas.assetDraws.single { draw -> draw.asset.resolvedKey == "prop.stairs.up" }
        assertEquals("dark-v1/props/prop_stairs_up.png", propDraw.asset.entry.rawOutputPath)
        assertFalse(propDraw.asset.fallbackUsed)
    }

    @Test
    fun `render canvas gives authored props visible atmosphere`() {
        val canvas = RecordingTileCanvas()
        val snapshot =
            sampleSnapshot().copy(
                props =
                    listOf(
                        PropRenderSnapshot(
                            id = "interactable:alarm_bonfire:test",
                            x = 0,
                            y = 0,
                            propTypeId = "alarm_bonfire",
                            visualKey = "prop.alarm_bonfire",
                        ),
                        PropRenderSnapshot(
                            id = "interactable:ritual_altar:test",
                            x = 0,
                            y = 0,
                            propTypeId = "ritual_altar",
                            visualKey = "prop.ritual_altar",
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

        assertTrue(
            canvas.rectDraws.any { draw -> draw.width == 30f && draw.height == 30f && draw.color.a.isNear(0.28f) },
            "authored bonfire props should cast a readable warm pool before the prop sprite is drawn",
        )
        assertTrue(
            canvas.rectDraws.any { draw -> draw.width == 20f && draw.height == 18f && draw.color.a.isNear(0.13f) },
            "authored ritual props should add a low red occult glow instead of reading as flat floor art",
        )
        assertTrue(canvas.flushes.indexOf(TileLayerFlushReason.MAP_PROP_ATMOSPHERE) < canvas.flushes.indexOf(TileLayerFlushReason.MAP_PROPS_AND_DECALS))
    }

    @Test
    fun keepsPr05ActorSpritesReadableOnDarkMap() {
        val canvas = RecordingTileCanvas()
        val base = sampleSnapshot()
        val snapshot =
            base.copy(
                actors =
                    base.actors +
                        ActorRenderSnapshot(
                            entityId = 2,
                            x = 0,
                            y = 0,
                            visualKey = "actor.arcanist",
                            nameKey = "profession.arcanist.name",
                            isPlayer = false,
                            roleKind = ActorRoleKindSnapshot.GENERIC,
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

        val actorDraw = canvas.assetDraws.single { draw -> draw.asset.resolvedKey == "actor.arcanist" }
        assertEquals("dark-v1/actors/actor_arcanist.png", actorDraw.asset.entry.rawOutputPath)
        assertFalse(actorDraw.asset.fallbackUsed)
        assertTrue(actorDraw.width >= 32f)
        assertTrue(actorDraw.height >= 32f)
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
                TileLayerFlushReason.MAP_CELL_MATERIAL,
                TileLayerFlushReason.MAP_PROP_ATMOSPHERE,
                TileLayerFlushReason.MAP_PROPS_AND_DECALS,
                TileLayerFlushReason.MAP_SPRITE_OVERLAYS_AND_TELEGRAPHS,
                TileLayerFlushReason.MAP_ACTORS,
                TileLayerFlushReason.MAP_PLAYER_INDICATOR,
                TileLayerFlushReason.MAP_GROUND_LOOT_ATMOSPHERE,
                TileLayerFlushReason.MAP_GROUND_LOOT_MARKERS,
                TileLayerFlushReason.MAP_FOG_VEILS,
                TileLayerFlushReason.MAP_TARGETING_HIGHLIGHTS,
                TileLayerFlushReason.MAP_ACTIVE_CURSOR,
                TileLayerFlushReason.MAP_COMBAT_FEEDBACK,
                TileLayerFlushReason.MAP_WARM_OVERLAY,
                TileLayerFlushReason.MAP_FRONTSTAGE_SURFACE,
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
                overlayState = OverlayState(mode = UiMode.INVENTORY, modalFrames = listOf(ModalFrame(ModalFrameKind.ITEM_DETAIL))),
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
    fun shellQuestSummaryCarriesGenericQuestMarkerForObjectiveLog() {
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
                row.text == "Objective updated: Breach The Outpost. You break into the outpost's inner yard." &&
                    row.icon?.requestedKey == "icon.quest.objective_marker"
            },
        )
        assertFalse(model.shell.leftRail.rows.any { row -> row.text == "No tracked quest in this zone." })
    }

    @Test
    fun shellQuestSummaryUsesObjectiveTokenToneBudget() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        mapOf(
            "log.objective.activate" to TileTextTone.GOLD,
            "log.objective.progress" to TileTextTone.LIGHT_GRAY,
            "log.objective.advance" to TileTextTone.WHITE,
            "log.objective.complete" to TileTextTone.GOLD,
        ).forEach { (tokenKey, expectedTone) ->
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
                                            key = tokenKey,
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

            val objectiveRow =
                model.shell.leftRail.rows.single { row -> row.icon?.requestedKey == "icon.quest.objective_marker" }
            assertEquals(expectedTone, objectiveRow.tone, tokenKey)
        }
    }

    @Test
    fun shellQuestSummaryRejectsUnknownObjectiveTextKeyForIcon() {
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
                                        key = "log.objective.unregistered_stage",
                                        arguments =
                                            listOf(
                                                RenderTextArgumentSnapshot(name = "objective", value = "Unknown objective"),
                                            ),
                                    ),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        val objectiveRow =
            model.shell.leftRail.rows.single { row -> row.text.contains("log.objective.unregistered_stage") }
        assertNull(objectiveRow.icon)
        assertEquals(TileTextTone.LIGHT_GRAY, objectiveRow.tone)
    }

    @Test
    fun shellQuestSummaryDoesNotUseQuestIconForEmptyState() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            TileRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(logEvents = emptyList()),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        val emptyQuestRow =
            model.shell.leftRail.rows.single { row -> row.text == "No tracked quest in this zone." }
        assertNull(emptyQuestRow.icon)
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
                                    iconKey = "icon.status.armor_break",
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

        val treeRow = model.sidebar.rows.first { row -> row.text == "Arms  2/2" }
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
    fun darkUiuxPr04TalentAssignPanelMatchesReferenceStructure() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(talentTrees = listOf(pr04TalentTree())),
            overlayState = pr04TalentAssignOverlay(),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(canvas.textDraws.any { draw -> draw.text == "Talent Assignment" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "Profession Talent Points: 0" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "Arms" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "2/2" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "[+]" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "Charge" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "0/5" })
        assertTrue(canvas.textDraws.any { draw -> draw.text.startsWith("Current Rank Detail") })
        assertTrue(
            canvas.textDraws.any { draw -> draw.text.startsWith("Next Rank") },
            canvas.textDraws.joinToString(" | ") { draw -> draw.text },
        )
        assertTrue(canvas.textDraws.any { draw -> draw.text == "Up/Down" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "select" })
    }

    @Test
    fun darkUiuxPr04DrawsStateMarkerSkillIconNameRankAndSelectedRow() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(talentTrees = listOf(pr04TalentTree())),
            overlayState = pr04TalentAssignOverlay(),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(canvas.textDraws.any { draw -> draw.text == "[+]" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "Charge" })
        assertTrue(canvas.textDraws.any { draw -> draw.text == "0/5" })
        assertTrue(canvas.assetDraws.any { draw -> draw.asset.resolvedKey == "item.short_sword.icon" })
        assertTrue(
            canvas.rectDraws.any { draw ->
                draw.color.r > 0.10f &&
                    draw.color.g > 0.65f &&
                    draw.color.b > 0.70f &&
                    draw.color.a in 0.17f..0.19f
            },
        )
    }

    @Test
    fun darkUiuxPr04KeepsRightDetailAboveNextPreview() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(talentTrees = listOf(pr04TalentTree())),
            overlayState = pr04TalentAssignOverlay(),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val current = canvas.textDraws.first { draw -> draw.text.startsWith("Current Rank Detail") }
        val next = canvas.textDraws.firstOrNull { draw -> draw.text.startsWith("Next Rank") }
        assertNotNull(next, canvas.textDraws.joinToString(" | ") { draw -> draw.text })
        assertTrue(current.y > requireNotNull(next).y)
    }

    @Test
    fun darkUiuxPr04SkillIconFallbackRendersWithoutCrashing() {
        val fallbackMessages = mutableListOf<String>()
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(ManifestLogSink { message -> fallbackMessages += message }),
            snapshot =
                sampleSnapshot(
                    talentTrees =
                        listOf(
                            pr04TalentTree(
                                primaryTalentId = "custom_missing_pr04",
                                nodeIconKey = "icon.skill.missing_pr04",
                            ),
                        ),
                ),
            overlayState = pr04TalentAssignOverlay(),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(canvas.assetDraws.any { draw -> draw.asset.requestedKey == "icon.skill.missing_pr04" && draw.asset.fallbackUsed })
        assertTrue(fallbackMessages.any { message -> message.contains("icon.skill.missing_pr04") })
    }

    @Test
    fun darkUiuxPr04TalentAssignPanelDoesNotCoverRightCompanionOrBottomLog() {
        val model =
            TileRenderer.buildRenderModel(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                snapshot = sampleSnapshot(talentTrees = listOf(pr04TalentTree())),
                overlayState = pr04TalentAssignOverlay(),
            )

        assertNotNull(model.talentAssignPanel)
        assertTrue(model.shell.rightPanel.rows.isNotEmpty())
        assertEquals("No log entries.", model.logPresentation.emptyStateText)
    }

    @Test
    fun darkUiuxPr04OverflowsListWithoutLayoutShift() {
        val panel =
            TileRenderer
                .buildRenderModel(
                    localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                    visualResolver = sampleResolver(),
                    snapshot =
                        sampleSnapshot(
                            talentTrees =
                                listOf(
                                    pr04TalentTree(treeId = "vanguard_arms"),
                                    pr04TalentTree(treeId = "vanguard_guard"),
                                    pr04TalentTree(treeId = "vanguard_warcry"),
                                ),
                        ),
                    overlayState = pr04TalentAssignOverlay(),
                ).talentAssignPanel
                ?.panel

        assertEquals(listOf("2/2", "2/2", "2/2"), requireNotNull(panel).sections.map { section -> section.nodeCountText })
        assertTrue(panel.sections.all { section -> section.scroll.verticalOffset == 0 })
    }

    @Test
    fun darkUiuxPr04ScrollsFocusedTalentRowIntoVisibleList() {
        val canvas = RecordingTileCanvas()
        val trees =
            (0 until 12).map { index ->
                pr04TalentTree(
                    treeId = "vanguard_tree_$index",
                    primaryTalentId = if (index == 11) "unyielding" else "charge",
                )
            }

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(talentTrees = trees),
            overlayState = pr04TalentAssignOverlay(talentTreeSelection = 22),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(
            canvas.textDraws.any { draw -> draw.text == "Unyielding" },
            canvas.textDraws.joinToString(" | ") { draw -> draw.text },
        )
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

    private fun sampleResolver(logSink: ManifestLogSink = ManifestLogSink { error("Unexpected manifest fallback: $it") }): VisualManifestResolver =
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
                                key = "tileset.forest_edge.ground_01",
                                category = "tile_ground",
                                rawOutputPath = "dark-v1/tiles/tileset_forest_edge_ground_01.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "actor.vanguard",
                                category = "actor_sprite",
                                rawOutputPath = "phase2/p2-b/actor_vanguard.png",
                                footprint = "2x1",
                            ),
                            VisualManifestEntry(
                                key = "actor.arcanist",
                                category = "actor_sprite",
                                rawOutputPath = "dark-v1/actors/actor_arcanist.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "actor.boss.ashgate_warden",
                                category = "actor_sprite",
                                rawOutputPath = "dark-v1/actors/actor_boss_ashgate_warden.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "prop.stairs.up",
                                category = "prop_interactable",
                                rawOutputPath = "dark-v1/props/prop_stairs_up.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "prop.alarm_bonfire",
                                category = "prop_interactable",
                                rawOutputPath = "dark-v1/props/prop_alarm_bonfire.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "prop.supply_crate",
                                category = "prop_interactable",
                                rawOutputPath = "dark-v1/props/prop_supply_crate.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "prop.ritual_altar",
                                category = "prop_interactable",
                                rawOutputPath = "dark-v1/props/prop_ritual_altar.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "item.short_sword.icon",
                                category = "item_icon",
                                rawOutputPath = "phase2/p2-b/icon_item_short_sword.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "item.hunter_bow.icon",
                                category = "icon_item",
                                rawOutputPath = "dark-v1/items/item_hunter_bow_icon.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "item.emerald_charm.icon",
                                category = "icon_item",
                                rawOutputPath = "dark-v1/items/item_emerald_charm_icon.png",
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
                                key = "icon.quest.objective_marker",
                                category = "icon_quest",
                                rawOutputPath = "dark-v1/icons/icon_quest_objective_marker.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "icon.status.armor_break",
                                category = "icon_status",
                                rawOutputPath = "dark-v1/icons/icon_status_armor_break.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "vfx.boss.variant.molten_glass",
                                category = "vfx_overlay",
                                rawOutputPath = "phase4/pr06/boss_variant_molten_glass.png",
                                footprint = "1x1",
                                tintColorHex = "#FF7A3C",
                            ),
                            VisualManifestEntry(
                                key = "vfx.zone.effect.void_pressure_01",
                                category = "tile_decal",
                                rawOutputPath = "dark-v1/vfx/vfx_zone_effect_void_pressure_01.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "vfx.boss.warning.sigil_01",
                                category = "vfx_plate",
                                rawOutputPath = "dark-v1/vfx/vfx_boss_warning_sigil_01.png",
                                footprint = "overlay",
                            ),
                        ) + pr04ReferenceIconKeys().map { key ->
                            VisualManifestEntry(
                                key = key,
                                category = "icon",
                                rawOutputPath = "phase4/uiux_pr04/${key.replace('.', '_')}.png",
                                footprint = "ui",
                                tags = listOf("reference-crop"),
                            )
                        } + pr04ReferenceChromeKeys().map { key ->
                            VisualManifestEntry(
                                key = key,
                                category = "ui_frame",
                                rawOutputPath = "phase4/uiux_pr04/${key.replace('.', '_')}.png",
                                footprint = "ui",
                                tags = listOf("reference-crop", "chrome"),
                            )
                        } +
                            DarkUiChromeTestKeys.pr02Round1OwnerKeys
                                .plus(DarkUiChromeTestKeys.pr02_1DemoShellOwnerKeys)
                                .plus(
                                    listOf(
                                        DarkUiChromeVisualKeys.SHOP_PRICE_AFFORDABLE,
                                        DarkUiChromeVisualKeys.SHOP_PRICE_UNAFFORDABLE,
                                        DarkUiChromeVisualKeys.SHOP_INSCRIPTION_MARKER,
                                        DarkUiChromeVisualKeys.SHOP_REPLACEMENT_SLOT_MARKER,
                                        DarkUiChromeVisualKeys.SHOP_OFFER_FRAME,
                                    ),
                                )
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
            logSink = logSink,
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

    private fun pr04ReferenceIconKeys(): List<String> =
        listOf(
            "power_strike",
            "sweeping_strike",
            "linebreaker",
            "earthshaker",
            "charge",
            "sunder_armor",
            "shield_bash",
            "taunt",
            "guard_stance",
            "iron_wall",
            "bulwark_march",
            "war_cry",
            "rallying_banner",
            "battlefield_command",
            "intimidation",
            "unyielding",
        ).map { talentId -> "dark.uiux.pr04.talent.vanguard.$talentId.icon" } +
            "dark.uiux.pr04.talent.vanguard.sweeping_strike.hero"

    private fun pr04ReferenceChromeKeys(): List<String> =
        TileTalentAssignReferenceChromeSlot.entries.map(TileTalentAssignReferenceChromeSlot::visualKey)

    private fun sampleShop(inscriptionReplacementPrompt: InscriptionReplacementPromptSnapshot? = null): ShopPanelSnapshot =
        ShopPanelSnapshot(
            shopId = "test_shop",
            shopNameKey = "shop.greenwood_supply_post.name",
            offers =
                listOf(
                    ShopOfferSnapshot(
                        index = 0,
                        labelKey = "inscription.phase_door.name",
                        price = 12,
                        offerFingerprint = "offer-inscription",
                        tags = listOf("INSCRIPTION"),
                        tagLabelKeys = listOf(ShopOfferTagTokens.INSCRIPTION),
                    ),
                    ShopOfferSnapshot(
                        index = 1,
                        labelKey = "item.short_sword.name",
                        price = 40,
                        offerFingerprint = "offer-sword",
                    ),
                ),
            inscriptionReplacementPrompt = inscriptionReplacementPrompt,
        )

    private fun sampleReplacementPrompt(currentSlotCount: Int = 1): InscriptionReplacementPromptSnapshot =
        InscriptionReplacementPromptSnapshot(
            offerIndex = 0,
            offerFingerprint = "offer-inscription",
            candidate =
                InscriptionReplacementEntrySnapshot(
                    hotkey = null,
                    inscriptionId = "phase_door_plus",
                    nameKey = "inscription.phase_door.name",
                    descKey = "inscription.phase_door.desc",
                    iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                    categoryId = "MOVEMENT",
                    cooldownRemaining = 0,
                    maxCooldown = 8,
                    upgradeFromInscriptionId = "phase_door",
                ),
            currentSlots = sampleCurrentReplacementSlots().take(currentSlotCount),
            categoryChanges =
                listOf(
                    InscriptionReplacementCategoryChangeSnapshot(
                        targetHotkey = 5,
                        categoryId = "MOVEMENT",
                        beforeCount = 1,
                        afterCount = 1,
                        limit = 2,
                    ),
                ),
            price = 12,
        )

    private fun sampleCurrentReplacementSlots(): List<InscriptionReplacementEntrySnapshot> =
        listOf(
            sampleCurrentReplacementSlot(hotkey = 5, inscriptionId = "phase_door", nameKey = "inscription.phase_door.name"),
            sampleCurrentReplacementSlot(hotkey = 6, inscriptionId = "healing_light", nameKey = "inscription.healing_light.name"),
            sampleCurrentReplacementSlot(hotkey = 7, inscriptionId = "iron_shield", nameKey = "inscription.iron_shield.name"),
            sampleCurrentReplacementSlot(hotkey = 8, inscriptionId = "purge", nameKey = "inscription.purge.name"),
        )

    private fun sampleCurrentReplacementSlot(
        hotkey: Int,
        inscriptionId: String,
        nameKey: String,
    ): InscriptionReplacementEntrySnapshot =
        InscriptionReplacementEntrySnapshot(
            hotkey = hotkey,
            inscriptionId = inscriptionId,
            nameKey = nameKey,
            descKey = "inscription.phase_door.desc",
            iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
            categoryId = "MOVEMENT",
            cooldownRemaining = 0,
            maxCooldown = 10,
        )

    private fun pr04TalentAssignOverlay(talentTreeSelection: Int = 0): OverlayState =
        OverlayState(
            mode = UiMode.TALENT_ASSIGN,
            talentTreeSelection = talentTreeSelection,
            modalFrames = listOf(ModalFrame(ModalFrameKind.TALENT_ASSIGN)),
        )

    private fun pr04TalentTree(
        treeId: String = "vanguard_arms",
        primaryTalentId: String = "charge",
        nodeIconKey: String = "item.short_sword.icon",
        treeIconKey: String = "item.short_sword.icon",
    ): TalentTreeSnapshot =
        TalentTreeSnapshot(
            treeId = treeId,
            treeOwnerId = "vanguard",
            nameKey = "talent_tree.vanguard_arms.name",
            descKey = "talent_tree.vanguard_arms.desc",
            iconKey = treeIconKey,
            nodes =
                listOf(
                    TalentTreeNodeSnapshot(
                        talentId = primaryTalentId,
                        treeId = treeId,
                        treeOwnerId = "vanguard",
                        nameKey = "talent.vanguard.$primaryTalentId.name",
                        descKey = "talent.vanguard.charge.desc",
                        iconKey = nodeIconKey,
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
                        treeId = treeId,
                        treeOwnerId = "vanguard",
                        nameKey = "talent.vanguard.war_cry.name",
                        descKey = "talent.vanguard.war_cry.desc",
                        iconKey = nodeIconKey,
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
        )

    private fun sampleSnapshot(
        width: Int = 1,
        height: Int = 1,
        cells: List<MapCellSnapshot>? = null,
        playerX: Int = 0,
        playerY: Int = 0,
        overlays: List<OverlayRenderSnapshot> = emptyList(),
        playerStatusEffects: List<StatusEffectRenderSnapshot> = emptyList(),
        talents: List<TalentSlotSnapshot> = emptyList(),
        inscriptions: List<InscriptionSlotSnapshot> = emptyList(),
        equipment: List<EquipmentSlotSnapshot> = emptyList(),
        reserveTalents: List<TalentReserveSnapshot> = emptyList(),
        talentTrees: List<TalentTreeSnapshot> = emptyList(),
        inventory: List<InventoryEntrySnapshot> = emptyList(),
        shardBalance: Int = 0,
        activeShop: ShopPanelSnapshot? = null,
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
                    playerX = playerX,
                    playerY = playerY,
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
                        x = playerX,
                        y = playerY,
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
                    shardBalance = shardBalance,
                    activeShop = activeShop,
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

    private fun validationPanelWithActions(cursor: com.ktome.core.map.Point): ValidationOverlayPanelState =
        validationPanel(cursor).copy(
            sections =
                listOf(
                    ValidationOverlaySectionState(
                        titleKey = ValidationOverlaySection.PHASE4_V4_FAST.titleKey,
                        selected = true,
                        actions =
                            listOf(
                                ValidationOverlayActionState(
                                    labelKey = "ui.validation.action.phase4_v4.show-evidence-summary",
                                    selected = true,
                                ),
                                ValidationOverlayActionState(
                                    labelKey = "ui.validation.action.phase4_v4.prepare-shop-surface",
                                    selected = false,
                                ),
                            ),
                    ),
                ),
        )

    private fun logEvent(
        key: String,
        vararg arguments: Pair<String, String>,
    ): RenderLogEventSnapshot =
        RenderLogEventSnapshot(
            RenderTextTokenSnapshot(
                key = key,
                arguments = arguments.map { (name, value) -> RenderTextArgumentSnapshot(name = name, value = value) },
            ),
        )
}

private fun Float.isNear(expected: Float): Boolean = abs(this - expected) < 0.0001f

private fun RecordingTileCanvas.RectDraw.contains(
    x: Float,
    y: Float,
): Boolean = x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height

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
