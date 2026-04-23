package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.ManifestLogSink
import com.ktome.client.assets.ManifestPrefixRule
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.client.ui.layout.PaneFocusAnchor
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.BossVariantRenderSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.CombatFeedbackSnapshot
import com.ktome.core.snapshot.CombatFeedbackTypeSnapshot
import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.FrontstageActionCategorySnapshot
import com.ktome.core.snapshot.FrontstageActionCueSnapshot
import com.ktome.core.snapshot.FrontstageActionPrioritySnapshot
import com.ktome.core.snapshot.FrontstageReadabilitySnapshot
import com.ktome.core.snapshot.GridPointSnapshot
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
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileRendererCanvasTest {
    @Test
    fun `render canvas honors manifest footprint dimensions`() {
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

        val actorDraw = canvas.assetDraws.single { draw -> draw.asset.resolvedKey == "actor.vanguard" }
        assertEquals(64f, actorDraw.width)
        assertEquals(32f, actorDraw.height)
    }

    @Test
    fun `render canvas places sidebar inside upper right sidebar panel`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(height = 6),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val titleDraw = canvas.textDraws.first { draw -> draw.text == "Ground" }
        assertTrue(titleDraw.y > TileRenderer.uiRows * 32f)
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

        val focusColor = UiDesignTokens.color.focus.ring.color()
        assertTrue(
            canvas.rectDraws.any { rect ->
                rect.x == layout.logX &&
                    rect.y == layout.cardY &&
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

        val mapOffsetY = TileRenderer.uiRows * 32f
        val cellRects =
            canvas.rectDraws.filter { draw ->
                draw.y == mapOffsetY && draw.width == 32f && draw.height == 32f
            }

        assertTrue(cellRects.any { draw -> draw.x == 32f && draw.color.a == 0.42f })
        assertTrue(cellRects.any { draw -> draw.x == 64f && draw.color.a == 0.94f })
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

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(),
            overlayState = OverlayState(mode = UiMode.MAP),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val subtitleDraw = requireNotNull(canvas.textDraws.getOrNull(1))
        val gaugeBackgrounds = canvas.rectDraws.filter { draw -> draw.height == 14f }
        val topGauge = requireNotNull(gaugeBackgrounds.maxByOrNull { draw -> draw.y })

        assertTrue(topGauge.y + topGauge.height <= subtitleDraw.y)
        assertTrue(subtitleDraw.y - (topGauge.y + topGauge.height) >= 4f)
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

        val subtitleDraw = requireNotNull(canvas.textDraws.getOrNull(1))
        val gaugeBackgrounds = canvas.rectDraws.filter { draw -> draw.height == 14f }
        val sortedGaugeBackgrounds = gaugeBackgrounds.sortedBy { draw -> draw.y }

        assertEquals(3, sortedGaugeBackgrounds.size)
        assertTrue(sortedGaugeBackgrounds.zipWithNext().all { (lower, upper) -> upper.y >= lower.y + lower.height + 4f })
        assertTrue(sortedGaugeBackgrounds.last().y + sortedGaugeBackgrounds.last().height <= subtitleDraw.y)
        assertTrue(subtitleDraw.y - (sortedGaugeBackgrounds.last().y + sortedGaugeBackgrounds.last().height) >= 4f)
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

        assertTrue(canvas.textDraws.any { draw -> draw.text == localizer.text("ui.hud.frontstage.title") })
        assertTrue(canvas.textDraws.any { draw -> draw.text.contains("Water lane") })
        assertTrue(canvas.textDraws.any { draw -> draw.text.contains("Restore 2 HP") })

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
        val legacyWorldWidth = (11 + TileRenderer.sidebarColumns) * 32f

        assertTrue(metrics.sidebarWidth <= 420f)
        assertTrue(metrics.worldWidth < legacyWorldWidth)
    }

    @Test
    fun `responsive layout keeps bottom panels inside world width`() {
        val metrics = TileRenderer.layoutMetrics(mapWidth = 9, mapHeight = 10, cellWidth = 32f, cellHeight = 32f)

        assertTrue(metrics.logWidth >= 180f)
        assertTrue(metrics.focusX + metrics.focusWidth <= metrics.worldWidth - metrics.bottomInset + 0.5f)
    }

    @Test
    fun `render canvas keeps map dominant regions separate at 1024 by 768 breakpoint`() {
        val canvas = RecordingTileCanvas()
        val metrics = TileRenderer.layoutMetrics(mapWidth = 18, mapHeight = 17, cellWidth = 32f, cellHeight = 32f)

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot = sampleSnapshot(width = 18, height = 17),
            overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = com.ktome.core.map.Point(0, 0)),
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        assertTrue(metrics.worldWidth <= 1024f)
        assertTrue(metrics.worldHeight <= 768f)
        assertTrue(metrics.cardY + metrics.cardHeight <= metrics.mapOffsetY)
        assertTrue(metrics.infoX + metrics.infoWidth <= metrics.logX)
        assertTrue(metrics.logX + metrics.logWidth + metrics.panelGap <= metrics.focusX)
        assertTrue(metrics.sidebarX >= 18 * 32f + metrics.panelGap)
        val mapWidthPx = 18 * 32f
        val focusRingRects =
            canvas.rectDraws.filter { draw ->
                draw.x >= 0f &&
                    draw.y >= metrics.mapOffsetY &&
                    draw.x + draw.width <= mapWidthPx &&
                    draw.y + draw.height <= metrics.worldHeight &&
                    ((draw.width == 32f && draw.height == 2f) || (draw.width == 2f && draw.height == 32f))
            }
        assertEquals(4, focusRingRects.size)
    }

    @Test
    fun `responsive layout leaves spacer between info and log on wide maps`() {
        val metrics = TileRenderer.layoutMetrics(mapWidth = 24, mapHeight = 10, cellWidth = 32f, cellHeight = 32f)

        assertTrue(metrics.logX > metrics.infoX + metrics.infoWidth + 40f)
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
        assertTrue(feedbackDraw.x > 32f)
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
    fun `render canvas uses distinct status tones for buffs and debuffs`() {
        val canvas = RecordingTileCanvas()

        TileRenderer.renderToCanvas(
            localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
            visualResolver = sampleResolver(),
            snapshot =
                sampleSnapshot(
                    playerStatusEffects =
                        listOf(
                            StatusEffectRenderSnapshot(
                                typeId = "HASTE",
                                remainingTurns = 2,
                                nameKey = "status.haste",
                                iconKey = "missing_visual",
                                category = StatusEffectCategorySnapshot.BUFF,
                            ),
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
            canvas = canvas,
            cellWidth = 32f,
            cellHeight = 32f,
        )

        val buffBadge = canvas.textDraws.first { draw -> draw.text == "2t" }
        val debuffBadge = canvas.textDraws.first { draw -> draw.text == "2/3 3t" }

        assertEquals(Color.valueOf("7FE0A0"), buffBadge.color)
        assertEquals(Color.valueOf("FF9A8D"), debuffBadge.color)
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
                                key = "vfx.boss.variant.molten_glass",
                                category = "vfx_overlay",
                                rawOutputPath = "phase4/pr06/boss_variant_molten_glass.png",
                                footprint = "1x1",
                                asciiColorHex = "#FF7A3C",
                            ),
                        ),
                    prefixRules = listOf(ManifestPrefixRule(prefix = "icon.", targetKey = "missing_visual")),
                ),
            logSink = ManifestLogSink { error("Unexpected manifest fallback: $it") },
        )

    private fun sampleSnapshot(
        width: Int = 1,
        height: Int = 1,
        cells: List<MapCellSnapshot>? = null,
        overlays: List<OverlayRenderSnapshot> = emptyList(),
        playerStatusEffects: List<StatusEffectRenderSnapshot> = emptyList(),
        talents: List<TalentSlotSnapshot> = emptyList(),
        reserveTalents: List<TalentReserveSnapshot> = emptyList(),
        inventory: List<InventoryEntrySnapshot> = emptyList(),
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
                    listOf(
                        MapCellSnapshot(
                            x = 0,
                            y = 0,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.test.ground_01",
                        ),
                    ),
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
                    equipment = emptyList(),
                    talents = talents,
                    reserveTalents = reserveTalents,
                    inventory = inventory,
                    targetablePositions = listOf(GridPointSnapshot(0, 0)),
                ),
            logEvents = logEvents,
            combatFeedbackEvents = combatFeedbackEvents,
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

    override fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
    ) {
        rectDraws += RectDraw(x, y, width, height, Color(color))
    }

    override fun drawAsset(
        asset: ResolvedVisualAsset,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        alpha: Float,
        tintColorHex: String?,
    ) {
        assetDraws += AssetDraw(asset, x, y, width, height, alpha, tintColorHex)
    }

    override fun drawText(
        style: TileTextStyle,
        text: String,
        x: Float,
        y: Float,
        color: Color,
    ) {
        textDraws += TextDraw(style, text, x, y, Color(color))
    }
}
