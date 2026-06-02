package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.DarkUiChromeTestKeys
import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.render.layout.GameShellBounds
import com.ktome.client.ui.chrome.ChromeFramePainter
import com.ktome.client.ui.panel.ActionPanelEntryModel
import com.ktome.client.ui.panel.ActionPanelModel
import com.ktome.client.ui.panel.LogPresentationModel
import com.ktome.client.ui.panel.PlayerCardModel
import com.ktome.client.ui.panel.TargetCardModel
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DemoShellRendererTest {
    @Test
    fun `shell keys bind only to their consumer regions and draw in the fixed shell order`() {
        val canvas = RecordingCanvas()
        val frame = sampleFrame()

        DemoShellRenderer.renderOuterFrame(canvas, frame)
        DemoShellRenderer.renderMapStageFrame(canvas, frame)
        DemoShellRenderer.renderShell(canvas, frame)

        DarkUiChromeTestKeys.pr02_1DemoShellOwnerKeys.forEach { key ->
            assertTrue(canvas.assetDraws.any { draw -> draw.key == key }, "$key was not drawn")
        }
        canvas.assetDraws
            .filter { draw -> draw.key.startsWith("ui.shell.") }
            .forEach { draw ->
                val expectedRegion = expectedRegion(frame, draw.key)
                assertTrue(expectedRegion.contains(draw), "${draw.key} escaped ${expectedRegion}: $draw")
            }

        assertIncreasing(
            canvas,
            DarkUiChromeVisualKeys.SHELL_OUTER_FRAME,
            DarkUiChromeVisualKeys.SHELL_MAP_STAGE_FRAME,
            SHELL_MAP_STAGE_BACKDROP,
            DarkUiChromeVisualKeys.SHELL_NAV_RAIL_FRAME,
            DarkUiChromeVisualKeys.SHELL_RIGHT_SECTION_DIVIDER,
            DarkUiChromeVisualKeys.SHELL_COMMAND_HINT_PLATE,
            DarkUiChromeVisualKeys.SHELL_HERO_CARD_FRAME,
            DarkUiChromeVisualKeys.SHELL_ACTION_DECK_FRAME,
            DarkUiChromeVisualKeys.SHELL_LOG_DECK_FRAME,
        )

        val equipmentBounds = frame.layout.demoShell.rightPanelLayout.equipment
        val operationBounds = frame.layout.demoShell.rightPanelLayout.operationHints
        val bottomBounds = frame.layout.demoShell.bottomDeck.bounds
        assertTrue(canvas.assetDraws.any { draw -> draw.key == SHELL_MAP_STAGE_BACKDROP && frame.layout.demoShell.mapStage.contains(draw) })
        assertTrue(canvas.textDraws.none { draw -> draw.text == "Ground" }, "Ground loot section must not be rendered in UI-demo-new shell.")
        val equipmentSlotDraws =
            canvas.assetDraws
                .filter { draw -> draw.key == DarkUiChromeVisualKeys.SLOT_EMPTY || draw.key == DarkUiChromeVisualKeys.SLOT_EQUIPPED }
                .filter { draw -> equipmentBounds.contains(draw) }
        assertTrue(equipmentSlotDraws.size >= 9, "Expected icon-first equipment sockets in the top right panel.")
        assertTrue(canvas.textDraws.any { draw -> draw.text == "Ctrl+S" && operationBounds.contains(draw) }, "Right operation hints must keep save shortcut.")
        assertTrue(canvas.textDraws.any { draw -> draw.text == "Save" && operationBounds.contains(draw) }, "Right operation hints must keep save label.")
        assertTrue(canvas.textDraws.any { draw -> draw.text == "5-8" && operationBounds.contains(draw) }, "Right operation hints must keep inscription shortcut.")
        assertTrue(canvas.textDraws.any { draw -> draw.text == "Use rune" && operationBounds.contains(draw) }, "Right operation hints must keep inscription label.")
        assertTrue(canvas.textDraws.none { draw -> draw.text.contains("Ctrl+S") && bottomBounds.contains(draw) }, "Bottom deck must not render command hints.")
        val logBody = logBodyRegion(frame.layout.demoShell.bottomDeck.logDeck)
        assertTrue(canvas.rectDraws.any { draw -> logBody.covers(draw) }, "Log deck must cover the decorative body with one continuous surface.")
        assertTrue(
            canvas.rectDraws.any { draw -> logBody.contains(draw) && draw.width == 3f && draw.height > logBody.height * 0.68f },
            "Log deck should keep a narrow signal rail for scanability without turning every log line into a card.",
        )
        assertFalse(
            canvas.rectDraws.any { draw -> logBody.contains(draw) && draw.height in 17f..19f && draw.width >= logBody.width - 8f },
            "Log deck must not split content into stacked row plates.",
        )
    }

    @Test
    fun `visual only empty equipment sockets stay quiet instead of drawing fake gold glyphs`() {
        val canvas = RecordingCanvas()
        val frame = sampleFrame(visualOnlyEquipmentIndexes = setOf(4, 5, 6, 7, 8))

        DemoShellRenderer.renderShell(canvas, frame)

        val visualOnlySlots = frame.layout.demoShell.rightPanelLayout.equipmentSlots.slotBounds.drop(4)
        val intrusiveGlyphs =
            canvas.rectDraws.filter { draw ->
                visualOnlySlots.any { slotBounds ->
                    val centralRegion =
                        GameShellBounds(
                            x = slotBounds.x + slotBounds.width * 0.16f,
                            y = slotBounds.y + slotBounds.height * 0.16f,
                            width = slotBounds.width * 0.68f,
                            height = slotBounds.height * 0.68f,
                        )
                    centralRegion.contains(draw) &&
                        draw.alpha >= 0.55f &&
                        (draw.width <= 6f || draw.height <= 6f || draw.width < slotBounds.width * 0.70f)
                }
            }
        assertTrue(
            intrusiveGlyphs.isEmpty(),
            "Visual-only empty equipment sockets must not render high-contrast pseudo-glyph strokes: $intrusiveGlyphs",
        )
        val quietInteriorDraws =
            canvas.rectDraws.count { draw ->
                visualOnlySlots.any { slotBounds ->
                    slotBounds.contains(draw) && draw.alpha in 0.12f..0.46f
                }
            }
        assertTrue(
            quietInteriorDraws >= visualOnlySlots.size * 6,
            "Visual-only empty equipment sockets should still keep low-contrast socket depth.",
        )
    }

    @Test
    fun `right panel sections use readable forged wells behind sockets and utility rows`() {
        val canvas = RecordingCanvas()
        val frame = sampleFrame(visualOnlyEquipmentIndexes = setOf(4, 5, 6, 7, 8))

        DemoShellRenderer.renderShell(canvas, frame)

        val right = frame.layout.demoShell.rightPanelLayout
        val sectionWells =
            listOf(right.equipment, right.inscriptions, right.backpack, right.operationHints)
                .count { section ->
                    canvas.rectDraws.any { draw ->
                        section.contains(draw) &&
                            draw.width >= section.width * 0.78f &&
                            draw.height >= section.height * 0.48f &&
                            draw.alpha in 0.28f..0.38f
                    }
                }
        assertEquals(
            4,
            sectionWells,
            "Every right-panel section should have a readable dark material well instead of receding into flat black.",
        )

        val socketWells =
            right.equipmentSlots.slotBounds.count { slotBounds ->
                canvas.rectDraws.any { draw ->
                    draw.x <= slotBounds.x - 3f &&
                        draw.y <= slotBounds.y - 3f &&
                        draw.x + draw.width >= slotBounds.right + 3f &&
                        draw.y + draw.height >= slotBounds.top + 3f &&
                        draw.alpha in 0.30f..0.40f
                }
            }
        assertTrue(
            socketWells >= right.equipmentSlots.slotBounds.size - 1,
            "Equipment sockets should sit in visible forged wells so empty paper-doll targets remain intentional at packaged scale.",
        )
    }

    @Test
    fun `empty action reserve socket stays below equipped action hierarchy`() {
        val canvas = RecordingCanvas()
        val frame = sampleFrame(actionEntryCount = 3)

        DemoShellRenderer.renderShell(canvas, frame)

        val actionSlots = frame.layout.demoShell.bottomDeck.actionSlotBounds
        val equippedActionSlots = actionSlots.take(3)
        val emptyReserveSlot = actionSlots[3]
        val reserveAsset =
            canvas.assetDraws.singleOrNull { draw ->
                draw.key == DarkUiChromeVisualKeys.SLOT_EMPTY && emptyReserveSlot.contains(draw)
            }
        assertTrue(reserveAsset != null, "Expected the fourth action slot to render as an empty reserve socket.")
        assertTrue(
            reserveAsset!!.alpha <= 0.40f,
            "Empty action reserve socket must stay visually quieter than equipped actions: $reserveAsset",
        )
        equippedActionSlots.forEach { slotBounds ->
            assertTrue(
                canvas.assetDraws.any { draw ->
                    draw.key == DarkUiChromeVisualKeys.SLOT_EQUIPPED &&
                        slotBounds.contains(draw) &&
                        draw.alpha >= 0.86f
                },
                "Equipped action socket should remain high-priority in $slotBounds.",
            )
        }
        assertFalse(
            canvas.rectDraws.any { draw ->
                emptyReserveSlot.contains(draw) &&
                    draw.width == 1f &&
                    draw.height > emptyReserveSlot.height * 0.18f &&
                    draw.x > emptyReserveSlot.x + emptyReserveSlot.width * 0.42f &&
                    draw.x < emptyReserveSlot.x + emptyReserveSlot.width * 0.58f
            },
            "Empty action reserve socket must not draw the central command connector.",
        )
    }

    @Test
    fun `hero gauges sit in individual forged troughs instead of flat color strips`() {
        val canvas = RecordingCanvas()
        val frame = sampleFrame()

        DemoShellRenderer.renderShell(canvas, frame)

        val heroContent = panelBodyRegion(frame.layout.demoShell.bottomDeck.heroCard)
        val gaugeBackgrounds =
            canvas.rectDraws
                .filter { draw ->
                    heroContent.contains(draw) &&
                        draw.width >= heroContent.width * 0.34f &&
                        draw.height in 17f..20f &&
                        draw.alpha in 0.86f..0.90f
                }
                .sortedBy { draw -> draw.y }

        assertEquals(2, gaugeBackgrounds.size, "The hero card should render HP and resource gauges.")
        val forgedTroughs =
            gaugeBackgrounds.count { gauge ->
                canvas.rectDraws.any { draw ->
                    heroContent.contains(draw) &&
                        draw.alpha in 0.32f..0.38f &&
                        draw.x <= gauge.x - 2f &&
                        draw.x + draw.width >= gauge.x + gauge.width + 2f &&
                        draw.y <= gauge.y - 1f &&
                        draw.y + draw.height >= gauge.y + gauge.height + 1f
                }
            }
        assertEquals(
            gaugeBackgrounds.size,
            forgedTroughs,
            "Each hero gauge should sit inside its own forged trough so HP/resource read as authored UI, not loose flat bars.",
        )
    }

    @Test
    fun `bottom hud renders a shared command spine across hero action and log`() {
        val canvas = RecordingCanvas()
        val frame = sampleFrame(actionEntryCount = 3)

        DemoShellRenderer.renderShell(canvas, frame)

        val bottom = frame.layout.demoShell.bottomDeck
        val hero = bottom.heroCard
        val log = bottom.logDeck
        val expectedWidth = log.right - hero.x
        val commandSpines =
            canvas.rectDraws.filter { draw ->
                draw.x <= hero.x + 28f &&
                    draw.x + draw.width >= log.right - 28f &&
                    draw.width >= expectedWidth * 0.88f &&
                    draw.height in 12f..24f &&
                    draw.y >= hero.y + 8f &&
                    draw.y <= hero.y + 32f &&
                    draw.alpha >= 0.20f
            }
        assertTrue(
            commandSpines.isNotEmpty(),
            "Bottom HUD should have one shared command spine tying hero, action deck, and log together.",
        )
    }

    @Test
    fun `bottom hud masks inter panel seams with forged lock plates`() {
        val canvas = RecordingCanvas()
        val frame = sampleFrame(actionEntryCount = 3)

        DemoShellRenderer.renderShell(canvas, frame)

        val bottom = frame.layout.demoShell.bottomDeck
        val seamCenters =
            listOf(
                (bottom.heroCard.right + bottom.actionDeck.x) / 2f,
                (bottom.actionDeck.right + bottom.logDeck.x) / 2f,
            )
        val seamLocks =
            seamCenters.count { centerX ->
                canvas.rectDraws.any { draw ->
                    draw.x <= centerX - 16f &&
                        draw.x + draw.width >= centerX + 16f &&
                        draw.height >= bottom.heroCard.height * 0.70f &&
                        draw.y >= bottom.heroCard.y + 10f &&
                        draw.y + draw.height <= bottom.heroCard.top - 10f &&
                        draw.alpha in 0.28f..0.38f
                }
            }
        assertEquals(
            seamCenters.size,
            seamLocks,
            "Bottom HUD seams should be masked by forged lock plates so hero, action, and log read as one console instead of adjacent cards.",
        )
    }

    @Test
    fun `bottom hud subdues individual panel frames under the shared console shell`() {
        val canvas = RecordingCanvas()
        val frame = sampleFrame(actionEntryCount = 3)

        DemoShellRenderer.renderShell(canvas, frame)

        val bottomFrameKeys =
            setOf(
                DarkUiChromeVisualKeys.SHELL_HERO_CARD_FRAME,
                DarkUiChromeVisualKeys.SHELL_ACTION_DECK_FRAME,
                DarkUiChromeVisualKeys.SHELL_LOG_DECK_FRAME,
            )
        val heavyChildFrames =
            canvas.assetDraws.filter { draw ->
                draw.key in bottomFrameKeys &&
                    frame.layout.demoShell.bottomDeck.bounds.contains(draw) &&
                    draw.alpha > 0.78f
            }
        assertTrue(
            heavyChildFrames.isEmpty(),
            "Bottom HUD child frames should sit below the shared console shell instead of dominating as three separate cards: $heavyChildFrames",
        )
    }

    @Test
    fun `hero card renders a tall heraldic material pillar behind the crest`() {
        val canvas = RecordingCanvas()
        val frame = sampleFrame()

        DemoShellRenderer.renderShell(canvas, frame)

        val heroContent = panelBodyRegion(frame.layout.demoShell.bottomDeck.heroCard)
        val pillarCandidates =
            canvas.rectDraws.filter { draw ->
                heroContent.contains(draw) &&
                    draw.x <= heroContent.x + heroContent.width * 0.12f &&
                    draw.width in (heroContent.width * 0.26f)..(heroContent.width * 0.46f) &&
                    draw.height >= heroContent.height * 0.76f &&
                    draw.alpha >= 0.44f
            }
        assertTrue(
            pillarCandidates.isNotEmpty(),
            "Hero card should frame the crest with a tall heraldic material pillar, not only a small backing plate.",
        )
        assertTrue(
            canvas.assetDraws.any { draw ->
                draw.key == DarkUiChromeVisualKeys.SHELL_HERO_CREST_PLACEHOLDER &&
                    draw.x < heroContent.x + heroContent.width * 0.42f &&
                    heroContent.contains(draw)
            },
            "Hero crest should remain anchored inside the heraldic pillar region.",
        )
    }

    private fun sampleFrame(
        visualOnlyEquipmentIndexes: Set<Int> = emptySet(),
        actionEntryCount: Int = 4,
    ): ShellRenderFrame {
        val layout = TileRenderer.layoutMetrics(mapWidth = 24, mapHeight = 18, cellWidth = 32f, cellHeight = 32f)
        val visualResolver = sampleResolver()
        val chromeAssets = TileChromeAssets.resolve(visualResolver)
        val demoModel =
            TileDemoShellModel(
                navItems =
                    listOf(
                        TileDemoNavItemModel(TileDemoNavItemKind.COMPASS, "Map", TileDemoNavItemState.SELECTED),
                        TileDemoNavItemModel(TileDemoNavItemKind.BAG, "Bag", TileDemoNavItemState.IDLE),
                        TileDemoNavItemModel(TileDemoNavItemKind.SCROLL, "Log", TileDemoNavItemState.IDLE),
                        TileDemoNavItemModel(TileDemoNavItemKind.BOOK, "Talent", TileDemoNavItemState.IDLE),
                        TileDemoNavItemModel(TileDemoNavItemKind.GEAR, "Settings", TileDemoNavItemState.IDLE),
                    ),
                rightEquipmentTitle = "Equipment",
                rightInscriptionsTitle = "Inscriptions",
                rightBackpackTitle = "Backpack",
                rightOperationHintsTitle = "Operations",
                equipmentSlots = fixedSlots(9, visualOnlyIndexes = visualOnlyEquipmentIndexes),
                inscriptionSlots = fixedSlots(8, firstLabel = 5),
                backpackSlots = fixedSlots(8),
                backpackPageLabel = "1/2  PgUp/PgDn",
                operationHints = listOf("I Inventory", "G Pick up", "Ctrl+S Save", "L Loadout", "5-8 Use rune"),
                heroSummaryLines = listOf("F1", "HP 30/30", "STA 10/10", "ATK 5", "DEF 2"),
            )
        val model =
            TileRenderModel(
                terrainTiles = emptyList(),
                propTiles = emptyList(),
                overlayTiles = emptyList(),
                groundLootMarkers = emptyList(),
                actorTiles = emptyList(),
                fogTiles = emptyList(),
                targetCursorState = null,
                hud =
                    TileHudModel(
                        playerName = "Hero",
                        zoneName = "Zone",
                        floorText = "F1",
                        hpGauge = TileGaugeModel("HP", 30, 30, TileTextTone.RED, "HEALTH"),
                        resourceGauge = TileGaugeModel("STA", 10, 10, TileTextTone.GREEN, "STAMINA"),
                        experienceGauge = TileGaugeModel("XP", 1, 10, TileTextTone.GOLD, "EXPERIENCE"),
                        statusIcons = emptyList(),
                        focusIcon = null,
                        focusName = null,
                        focusLines = emptyList(),
                        hotbar = emptyList(),
                        summaryText = "STA 10/10",
                    ),
                messageLines = listOf(TileMessageLine("A recent event is readable.", TileTextTone.WHITE)),
                logPresentation = LogPresentationModel(entries = emptyList(), emptyStateText = "", fallbackText = ""),
                playerCard = PlayerCardModel("Hero", "HP 30/30", "STA 10/10", emptyStateText = ""),
                targetCard = TargetCardModel(title = null, lines = emptyList(), emptyStateText = ""),
                actionPanel =
                    ActionPanelModel(
                        entries =
                            (1..actionEntryCount).map { index ->
                                ActionPanelEntryModel(hotkey = index.toString(), label = "Skill $index", enabled = true)
                            },
                        emptyStateText = "",
                    ),
                combatFeedback = emptyList(),
                sidebar = TileSidebarModel("Ground", emptyList()),
                shell =
                    TileShellModel(
                        leftRail = TilePanelModel("Left", emptyList()),
                        rightPanel = TilePanelModel("Right", emptyList()),
                        footerHints = listOf(TileTextRow("I Inventory", TileTextTone.LIGHT_GRAY)),
                        demo = demoModel,
                    ),
                playerTile = Point(0, 0),
                mapDimensions = TileMapDimensions(1, 1),
                chromeAssets = chromeAssets,
            )
        return ShellRenderFrame(
            model = model,
            layout = layout,
            textLayout =
                ShellTextLayout(
                    playerName = "Hero",
                    summaryLines = listOf("STA 10/10"),
                    targetTitle = null,
                    targetLines = emptyList(),
                    messageLines = listOf(TileMessageLine("A recent event is readable.", TileTextTone.WHITE)),
                    leftRail = ShellPanelTextLayout("Left", emptyList()),
                    rightPanel = ShellPanelTextLayout("Right", emptyList()),
                    footerHints = "I Inventory  G Pick up  Ctrl+S Save",
                    hotbar =
                        (1..actionEntryCount).map { index ->
                            ShellHotbarTextLayout(index.toString(), "Skill $index", "Ready", TileTextTone.LIGHT_GRAY)
                        },
                ),
            paneFocusAnchor = null,
        )
    }

    private fun fixedSlots(
        count: Int,
        firstLabel: Int = 1,
        visualOnlyIndexes: Set<Int> = emptySet(),
    ): List<TileDemoSlotModel> =
        List(count) { index ->
            TileDemoSlotModel(
                label = (firstLabel + index).toString(),
                detail = null,
                icon = null,
                state = if (index == 0) TileDemoSlotState.FILLED else TileDemoSlotState.EMPTY,
                visualOnly = index in visualOnlyIndexes,
            )
        }

    private fun sampleResolver(): VisualManifestResolver {
        val keys =
            listOf("missing_visual", SHELL_MAP_STAGE_BACKDROP) +
                DarkUiChromeTestKeys.pr02Round1OwnerKeys +
                DarkUiChromeTestKeys.pr02_1DemoShellOwnerKeys
        return VisualManifestResolver(
            VisualManifest(
                manifestVersion = 1,
                styleTag = "test",
                fallbackKey = "missing_visual",
                entries = keys.distinct().map(::manifestEntry),
            ),
        )
    }

    private fun manifestEntry(key: String): VisualManifestEntry =
        VisualManifestEntry(
            key = key,
            category = if (key.startsWith("ui.frame.") || key.contains(".frame") || key.contains(".plate") || key.contains(".divider")) "ui_frame" else "icon",
            rawOutputPath = "dark-v1/ui/${key.replace('.', '_')}.png",
            footprint = "ui",
        )

    private fun expectedRegion(
        frame: ShellRenderFrame,
        key: String,
    ): GameShellBounds =
        when (key) {
            DarkUiChromeVisualKeys.SHELL_OUTER_FRAME -> frame.layout.demoShell.outerFrame
            DarkUiChromeVisualKeys.SHELL_MAP_STAGE_FRAME -> frame.layout.demoShell.mapStage
            SHELL_MAP_STAGE_BACKDROP -> frame.layout.demoShell.mapStage
            DarkUiChromeVisualKeys.SHELL_NAV_RAIL_FRAME,
            DarkUiChromeVisualKeys.SHELL_NAV_BUTTON_ACTIVE,
            DarkUiChromeVisualKeys.SHELL_NAV_COMPASS,
            DarkUiChromeVisualKeys.SHELL_NAV_BAG,
            DarkUiChromeVisualKeys.SHELL_NAV_SCROLL,
            DarkUiChromeVisualKeys.SHELL_NAV_BOOK,
            DarkUiChromeVisualKeys.SHELL_NAV_GEAR -> frame.layout.demoShell.navRail
            DarkUiChromeVisualKeys.SHELL_RIGHT_SECTION_DIVIDER -> frame.layout.demoShell.rightPanel
            DarkUiChromeVisualKeys.SHELL_HERO_CARD_FRAME,
            DarkUiChromeVisualKeys.SHELL_HERO_CREST_PLACEHOLDER -> frame.layout.demoShell.bottomDeck.heroCard
            DarkUiChromeVisualKeys.SHELL_ACTION_DECK_FRAME -> frame.layout.demoShell.bottomDeck.actionDeck
            DarkUiChromeVisualKeys.SHELL_COMMAND_HINT_PLATE -> frame.layout.demoShell.rightPanelLayout.operationHints
            DarkUiChromeVisualKeys.SHELL_LOG_DECK_FRAME -> frame.layout.demoShell.bottomDeck.logDeck
            else -> error("Unexpected shell key $key")
        }

    private fun assertIncreasing(
        canvas: RecordingCanvas,
        vararg keys: String,
    ) {
        val indices = keys.map { key -> canvas.assetDraws.indexOfFirst { draw -> draw.key == key } }
        assertTrue(indices.all { index -> index >= 0 }, indices.toString())
        assertEquals(indices.sorted(), indices)
    }

    private fun GameShellBounds.contains(draw: AssetDraw): Boolean =
        draw.x >= x - 0.5f &&
            draw.y >= y - 0.5f &&
            draw.x + draw.width <= right + 0.5f &&
            draw.y + draw.height <= top + 0.5f

    private fun GameShellBounds.contains(draw: TextDraw): Boolean =
        draw.x >= x - 0.5f &&
            draw.y >= y - 0.5f &&
            draw.x <= right + 0.5f &&
            draw.y <= top + 0.5f

    private fun GameShellBounds.contains(draw: RectDraw): Boolean =
        draw.x >= x - 0.5f &&
            draw.y >= y - 0.5f &&
            draw.x + draw.width <= right + 0.5f &&
            draw.y + draw.height <= top + 0.5f

    private fun GameShellBounds.covers(draw: RectDraw): Boolean =
        draw.x <= x + 0.5f &&
            draw.y <= y + 0.5f &&
            draw.x + draw.width >= right - 0.5f &&
            draw.y + draw.height >= top - 0.5f

    private fun logBodyRegion(bounds: GameShellBounds): GameShellBounds {
        return panelBodyRegion(bounds)
    }

    private fun panelBodyRegion(bounds: GameShellBounds): GameShellBounds {
        val edge = ChromeFramePainter.frameEdgeSize
        return GameShellBounds(
            x = bounds.x + edge,
            y = bounds.y + edge,
            width = bounds.width - edge * 2f,
            height = bounds.height - edge * 2f,
        )
    }

    private data class AssetDraw(
        val key: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val alpha: Float,
    )

    private data class RectDraw(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val alpha: Float,
    )

    private data class TextDraw(
        val text: String,
        val x: Float,
        val y: Float,
    )

    private class RecordingCanvas : TileCanvas {
        val assetDraws = mutableListOf<AssetDraw>()
        val rectDraws = mutableListOf<RectDraw>()
        val textDraws = mutableListOf<TextDraw>()

        override fun drawRect(draw: TileRectDraw) {
            rectDraws += RectDraw(draw.bounds.x, draw.bounds.y, draw.bounds.width, draw.bounds.height, draw.color.a)
        }

        override fun drawAsset(draw: TileAssetDraw) {
            assetDraws += AssetDraw(draw.asset.resolvedKey, draw.bounds.x, draw.bounds.y, draw.bounds.width, draw.bounds.height, draw.alpha)
        }

        override fun drawText(draw: TileTextDraw) {
            textDraws += TextDraw(draw.text, draw.position.x, draw.position.y)
        }

        override fun flushLayer(reason: TileLayerFlushReason) = Unit
    }

    private companion object {
        const val SHELL_MAP_STAGE_BACKDROP: String = "ui.shell.map_stage.backdrop"
    }
}
