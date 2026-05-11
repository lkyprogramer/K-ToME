package com.ktome.client.screen

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.ManifestLogSink
import com.ktome.client.assets.ManifestPrefixRule
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.ui.token.UiDesignTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StandaloneScreenLayoutTest {
    @Test
    fun `main menu standalone layout reserves action stack secondary panel disabled detail and footer`() {
        val layout = DarkStandaloneScreenLayout.mainMenu()

        assertTrue(layout.primaryActionStack.right <= layout.secondaryPanel.x)
        assertTrue(layout.disabledDetailArea.y >= layout.footerHelp.top)
        assertTrue(layout.disabledDetailArea.top <= layout.primaryActionStack.y)
        assertTrue(layout.secondaryPanel.top <= layout.header.y)
        assertDoesNotOverlap(layout.primaryActionStack, layout.secondaryPanel)
        assertDoesNotOverlap(layout.disabledDetailArea, layout.footerHelp)
    }

    @Test
    fun `validation setup layout keeps list and active pack summary above footer`() {
        val layout = DarkStandaloneScreenLayout.validationSetup()
        val entryCount = 15
        val placements = DarkStandaloneScreenLayout.validationEntryPlacements(entryCount)
        val columnCount = placements.map { placement -> placement.x }.distinct().size

        assertEquals(entryCount, placements.size)
        assertEquals(2, columnCount)
        assertTrue(layout.primaryActionStack.y >= layout.footerHelp.top)
        assertTrue(layout.primaryActionStack.top <= layout.disabledDetailArea.y)
        assertTrue(layout.disabledDetailArea.top <= layout.secondaryPanel.y)
        assertTrue(DarkStandaloneScreenLayout.validationFooterControlsBaselineY < layout.footerHelp.top)
        assertTrue(DarkStandaloneScreenLayout.validationFooterControlsBaselineY > layout.footerHelp.y)
        placements.forEach { placement ->
            assertTrue(placement.x >= layout.primaryActionStack.x)
            assertTrue(placement.x < layout.primaryActionStack.right)
            assertTrue(placement.baselineY >= layout.primaryActionStack.y + DarkStandaloneScreenLayout.validationEntryMinStepY)
            assertTrue(placement.baselineY <= layout.primaryActionStack.top)
        }
        placements
            .groupBy { placement -> placement.x }
            .values
            .forEach { columnPlacements ->
                columnPlacements.zipWithNext().forEach { (current, next) ->
                    assertTrue(current.baselineY - next.baselineY >= DarkStandaloneScreenLayout.validationEntryMinStepY)
                }
            }
        assertDoesNotOverlap(layout.primaryActionStack, layout.disabledDetailArea)
        assertDoesNotOverlap(layout.disabledDetailArea, layout.secondaryPanel)
        assertDoesNotOverlap(layout.primaryActionStack, layout.footerHelp)
    }

    @Test
    fun `outcome body baselines are capped inside the primary action stack`() {
        val layout = DarkStandaloneScreenLayout.outcome()
        val baselines = DarkStandaloneScreenLayout.outcomeBodyLineBaselines(lineCount = 20)

        assertEquals(DarkStandaloneScreenLayout.outcomeBodyLineCapacity(), baselines.size)
        assertTrue(baselines.isNotEmpty())
        baselines.forEach { baseline ->
            assertTrue(baseline <= layout.primaryActionStack.top)
            assertTrue(baseline >= layout.primaryActionStack.y + DarkStandaloneScreenLayout.outcomeBodyStepY)
        }
        baselines.zipWithNext().forEach { (current, next) ->
            assertEquals(DarkStandaloneScreenLayout.outcomeBodyStepY, current - next)
        }
    }

    @Test
    fun `talent tones match the art bible contract consumed by later talent UI`() {
        val talent = UiDesignTokens.color.talent

        assertEquals("#59616C", talent.locked.hexString())
        assertEquals("#1CB7C8", talent.learnable.hexString())
        assertEquals("#D99A2B", talent.reserve.hexString())
        assertEquals("#52C989", talent.active.hexString())
    }

    @Test
    fun `dark uiux pr02 standalone chrome consumes manifest keys`() {
        val chromeAssets =
            StandaloneChromeAssets.resolve(
                visualResolver = standaloneChromeResolver(),
                screenMarkerKey = DarkUiChromeVisualKeys.SCREEN_VALIDATION_BADGE,
            )
        val sink = RecordingStandaloneChromeSink()

        StandaloneScreenChrome.drawToSink(
            request =
                StandaloneChromeRequest(
                    layout = DarkStandaloneScreenLayout.validationSetup(),
                    detailAreaMode = StandaloneDetailAreaMode.HIDDEN,
                    chromeAssets = chromeAssets,
                ),
            sink = sink,
        )

        val drawnKeys = sink.assetDraws.map { draw -> draw.asset.resolvedKey }
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
        assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.CONTROL_BACK), drawnKeys.toString())
        assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.CONTROL_CONFIRM), drawnKeys.toString())
        assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.CONTROL_COPY), drawnKeys.toString())
        assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.SCREEN_VALIDATION_BADGE), drawnKeys.toString())
        assertFalse(chromeAssets.panelBody.fallbackUsed)
    }

    @Test
    fun `runtime status layout keeps chrome panels inside the game viewport`() {
        val layout = DarkStandaloneScreenLayout.runtimeStatus(worldWidth = 1280f, worldHeight = 800f)

        listOf(layout.header, layout.primaryActionStack, layout.footerHelp).forEach { bounds ->
            assertTrue(bounds.x >= layout.background.x)
            assertTrue(bounds.right <= layout.background.right)
            assertTrue(bounds.y >= layout.background.y)
            assertTrue(bounds.top <= layout.background.top)
        }
        assertDoesNotOverlap(layout.header, layout.primaryActionStack)
        assertDoesNotOverlap(layout.primaryActionStack, layout.footerHelp)
    }

    @Test
    fun `dark uiux pr02 runtime loading and error chrome consumes manifest markers`() {
        val resolver = standaloneChromeResolver()
        val layout = DarkStandaloneScreenLayout.runtimeStatus(worldWidth = 1280f, worldHeight = 800f)

        listOf(
            DarkUiChromeVisualKeys.SCREEN_LOADING_MARKER,
            DarkUiChromeVisualKeys.SCREEN_ERROR_MARKER,
        ).forEach { markerKey ->
            val sink = RecordingStandaloneChromeSink()

            StandaloneScreenChrome.drawToSink(
                request =
                    StandaloneChromeRequest(
                        layout = layout,
                        detailAreaMode = StandaloneDetailAreaMode.HIDDEN,
                        chromeAssets =
                            StandaloneChromeAssets.resolve(
                                visualResolver = resolver,
                                screenMarkerKey = markerKey,
                            ),
                    ),
                sink = sink,
            )

            val drawnKeys = sink.assetDraws.map { draw -> draw.asset.resolvedKey }
            assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.PANEL_BODY), drawnKeys.toString())
            assertTrue(drawnKeys.contains(markerKey), drawnKeys.toString())
            assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.CONTROL_BACK), drawnKeys.toString())
            assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.CONTROL_CONFIRM), drawnKeys.toString())
            assertTrue(drawnKeys.contains(DarkUiChromeVisualKeys.CONTROL_COPY), drawnKeys.toString())
        }
    }

    @Test
    fun `dark uiux pr02 chrome uses fallback asset when panel body missing`() {
        val chromeAssets =
            StandaloneChromeAssets.resolve(
                visualResolver = standaloneChromeResolver(excludedKeys = setOf(DarkUiChromeVisualKeys.PANEL_BODY)),
            )
        val sink = RecordingStandaloneChromeSink()

        StandaloneScreenChrome.drawToSink(
            request =
                StandaloneChromeRequest(
                    layout = DarkStandaloneScreenLayout.mainMenu(),
                    detailAreaMode = StandaloneDetailAreaMode.HIDDEN,
                    chromeAssets = chromeAssets,
                ),
            sink = sink,
        )

        assertTrue(chromeAssets.panelBody.fallbackUsed)
        assertEquals("missing_visual", chromeAssets.panelBody.resolvedKey)
        assertTrue(sink.assetDraws.any { draw -> draw.asset.requestedKey == DarkUiChromeVisualKeys.PANEL_BODY })
    }

    private fun assertDoesNotOverlap(
        first: ScreenPanelBounds,
        second: ScreenPanelBounds,
    ) {
        assertTrue(
            first.x >= second.right ||
                first.right <= second.x ||
                first.y >= second.top ||
                first.top <= second.y,
            "Expected $first not to overlap $second.",
        )
    }

    private fun standaloneChromeResolver(excludedKeys: Set<String> = emptySet()): VisualManifestResolver =
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
                        ) +
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
                                DarkUiChromeVisualKeys.CONTROL_BACK,
                                DarkUiChromeVisualKeys.CONTROL_CONFIRM,
                                DarkUiChromeVisualKeys.CONTROL_COPY,
                                DarkUiChromeVisualKeys.SCREEN_VALIDATION_BADGE,
                                DarkUiChromeVisualKeys.SCREEN_ERROR_MARKER,
                                DarkUiChromeVisualKeys.SCREEN_LOADING_MARKER,
                            ).filterNot(excludedKeys::contains).map { key ->
                                VisualManifestEntry(
                                    key = key,
                                    category = if (key.startsWith("ui.frame.")) "ui_frame" else "icon",
                                    rawOutputPath = "dark-v1/ui/${key.replace('.', '_')}.png",
                                    footprint = "ui",
                                )
                            },
                    prefixRules = listOf(ManifestPrefixRule(prefix = "ui.", targetKey = "missing_visual")),
                ),
            logSink = ManifestLogSink { },
        )
}

private class RecordingStandaloneChromeSink : StandaloneChromeDrawSink {
    val assetDraws = mutableListOf<StandaloneChromeAssetDraw>()

    override fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
    ) = Unit

    override fun drawAsset(
        asset: ResolvedVisualAsset,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        alpha: Float,
    ) {
        assetDraws += StandaloneChromeAssetDraw(asset, x, y, width, height, alpha)
    }
}
