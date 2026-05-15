package com.ktome.client.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.ktome.client.assets.ClientTextureRepository
import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.render.TileTextMetrics
import com.ktome.client.render.TileTextStyle
import com.ktome.client.ui.chrome.ChromeFrameAssetDraw
import com.ktome.client.ui.chrome.ChromeFrameAssets
import com.ktome.client.ui.chrome.ChromeFrameBounds
import com.ktome.client.ui.chrome.ChromeFrameDrawRequest
import com.ktome.client.ui.chrome.ChromeFrameDrawSink
import com.ktome.client.ui.chrome.ChromeFramePainter
import com.ktome.client.ui.chrome.ChromeFrameRectDraw
import com.ktome.client.ui.chrome.ChromeSurfaceKind
import com.ktome.client.ui.token.UiDesignTokens

internal data class ScreenPanelBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = x + width
    val top: Float get() = y + height
}

internal data class ScreenTextSlot(
    val bounds: ScreenPanelBounds,
    val maxLines: Int,
    val maxChars: Int,
)

internal data class StandaloneScreenLayout(
    val background: ScreenPanelBounds,
    val header: ScreenPanelBounds,
    val primaryActionStack: ScreenPanelBounds,
    val secondaryPanel: ScreenPanelBounds,
    val disabledDetailArea: ScreenPanelBounds,
    val footerHelp: ScreenPanelBounds,
)

internal data class ValidationSetupEntryPlacement(
    val x: Float,
    val baselineY: Float,
    val maxChars: Int,
)

internal enum class StandaloneDetailAreaMode {
    HIDDEN,
    VISIBLE,
}

internal data class StandaloneChromeRequest(
    val layout: StandaloneScreenLayout,
    val detailAreaMode: StandaloneDetailAreaMode,
    val chromeAssets: StandaloneChromeAssets? = null,
)

internal data class StandaloneChromeAssets(
    val panelBody: ResolvedVisualAsset,
    val panelCornerTopLeft: ResolvedVisualAsset,
    val panelCornerTopRight: ResolvedVisualAsset,
    val panelCornerBottomLeft: ResolvedVisualAsset,
    val panelCornerBottomRight: ResolvedVisualAsset,
    val panelEdgeTop: ResolvedVisualAsset,
    val panelEdgeRight: ResolvedVisualAsset,
    val panelEdgeBottom: ResolvedVisualAsset,
    val panelEdgeLeft: ResolvedVisualAsset,
    val backIcon: ResolvedVisualAsset,
    val confirmIcon: ResolvedVisualAsset,
    val copyIcon: ResolvedVisualAsset,
    val screenMarker: ResolvedVisualAsset? = null,
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

    companion object {
        fun resolve(
            visualResolver: VisualManifestResolver,
            screenMarkerKey: String? = null,
        ): StandaloneChromeAssets =
            StandaloneChromeAssets(
                panelBody = visualResolver.resolve(DarkUiChromeVisualKeys.PANEL_BODY),
                panelCornerTopLeft = visualResolver.resolve(DarkUiChromeVisualKeys.PANEL_CORNER_TL),
                panelCornerTopRight = visualResolver.resolve(DarkUiChromeVisualKeys.PANEL_CORNER_TR),
                panelCornerBottomLeft = visualResolver.resolve(DarkUiChromeVisualKeys.PANEL_CORNER_BL),
                panelCornerBottomRight = visualResolver.resolve(DarkUiChromeVisualKeys.PANEL_CORNER_BR),
                panelEdgeTop = visualResolver.resolve(DarkUiChromeVisualKeys.PANEL_EDGE_TOP),
                panelEdgeRight = visualResolver.resolve(DarkUiChromeVisualKeys.PANEL_EDGE_RIGHT),
                panelEdgeBottom = visualResolver.resolve(DarkUiChromeVisualKeys.PANEL_EDGE_BOTTOM),
                panelEdgeLeft = visualResolver.resolve(DarkUiChromeVisualKeys.PANEL_EDGE_LEFT),
                backIcon = visualResolver.resolve(DarkUiChromeVisualKeys.CONTROL_BACK),
                confirmIcon = visualResolver.resolve(DarkUiChromeVisualKeys.CONTROL_CONFIRM),
                copyIcon = visualResolver.resolve(DarkUiChromeVisualKeys.CONTROL_COPY),
                screenMarker = screenMarkerKey?.let(visualResolver::resolve),
            )
    }
}

internal data class StandaloneChromeAssetDraw(
    val asset: ResolvedVisualAsset,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val alpha: Float,
)

internal interface StandaloneChromeDrawSink {
    fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
    )

    fun drawAsset(
        asset: ResolvedVisualAsset,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        alpha: Float,
    )
}

internal class StandaloneScreenChrome(
    private val textureRepository: ClientTextureRepository? = null,
) : Disposable {
    private val whitePixel: Texture = solidTexture()

    fun draw(
        batch: SpriteBatch,
        request: StandaloneChromeRequest,
    ) {
        drawToSink(
            request = request,
            sink =
                object : StandaloneChromeDrawSink {
                    override fun drawRect(
                        x: Float,
                        y: Float,
                        width: Float,
                        height: Float,
                        color: Color,
                    ) {
                        drawRect(batch, x, y, width, height, color)
                    }

                    override fun drawAsset(
                        asset: ResolvedVisualAsset,
                        x: Float,
                        y: Float,
                        width: Float,
                        height: Float,
                        alpha: Float,
                    ) {
                        val texture = textureRepository?.textureFor(asset)
                        if (texture != null) {
                            batch.color = Color(1f, 1f, 1f, alpha)
                            batch.draw(texture, x, y, width, height)
                        }
                    }
                },
        )
        batch.color = Color.WHITE
    }

    override fun dispose() {
        whitePixel.dispose()
    }

    private fun drawRect(
        batch: SpriteBatch,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
    ) {
        batch.color = color
        batch.draw(whitePixel, x, y, width, height)
    }

    companion object {
        internal fun drawToSink(
            request: StandaloneChromeRequest,
            sink: StandaloneChromeDrawSink,
        ) {
            val layout = request.layout
            drawPanel(
                sink = sink,
                bounds = layout.header,
                fill = UiDesignTokens.color.surface.overlay.color(),
                border = UiDesignTokens.color.border.strong.color(),
                chromeAssets = request.chromeAssets,
                surfaceKind = ChromeSurfaceKind.Panel,
            )
            drawPanel(
                sink = sink,
                bounds = layout.primaryActionStack,
                fill = UiDesignTokens.color.surface.raised.color(),
                border = UiDesignTokens.color.border.subtle.color(),
                chromeAssets = request.chromeAssets,
                surfaceKind = ChromeSurfaceKind.Panel,
            )
            if (!layout.secondaryPanel.sameBounds(layout.primaryActionStack)) {
                drawPanel(
                    sink = sink,
                    bounds = layout.secondaryPanel,
                    fill = UiDesignTokens.color.surface.raised.color(),
                    border = UiDesignTokens.color.border.subtle.color(),
                    chromeAssets = request.chromeAssets,
                    surfaceKind = if (layout.secondaryPanel.height <= 60f) ChromeSurfaceKind.FooterHint else ChromeSurfaceKind.Panel,
                )
            }
            if (request.detailAreaMode == StandaloneDetailAreaMode.VISIBLE) {
                drawPanel(
                    sink = sink,
                    bounds = layout.disabledDetailArea,
                    fill = UiDesignTokens.color.surface.baseDim.color(),
                    border = UiDesignTokens.color.status.badge.turns.color(),
                    chromeAssets = request.chromeAssets,
                    surfaceKind = ChromeSurfaceKind.FooterHint,
                )
            }
            drawPanel(
                sink = sink,
                bounds = layout.footerHelp,
                fill = UiDesignTokens.color.surface.baseDim.color(),
                border = UiDesignTokens.color.border.subtle.color(),
                chromeAssets = request.chromeAssets,
                surfaceKind = ChromeSurfaceKind.FooterHint,
            )
            request.chromeAssets?.let { chrome ->
                chrome.screenMarker?.let { marker ->
                    sink.drawAsset(marker, layout.header.right - 44f, layout.header.top - 44f, 28f, 28f, 0.92f)
                }
                sink.drawAsset(chrome.backIcon, layout.footerHelp.right - 114f, layout.footerHelp.y + 10f, 22f, 22f, 0.9f)
                sink.drawAsset(chrome.confirmIcon, layout.footerHelp.right - 78f, layout.footerHelp.y + 10f, 22f, 22f, 0.9f)
                sink.drawAsset(chrome.copyIcon, layout.footerHelp.right - 42f, layout.footerHelp.y + 10f, 22f, 22f, 0.9f)
            }
        }

        private fun drawPanel(
            sink: StandaloneChromeDrawSink,
            bounds: ScreenPanelBounds,
            fill: Color,
            border: Color,
            chromeAssets: StandaloneChromeAssets?,
            surfaceKind: ChromeSurfaceKind,
        ) {
            val frameBounds = bounds.toChromeFrameBounds()
            if (chromeAssets == null) {
                sink.drawRect(bounds.x, bounds.y, bounds.width, bounds.height, fill)
                drawOutline(sink, bounds, UiDesignTokens.stroke.thin, border)
                val content = ChromeFramePainter.contentBounds(frameBounds, surfaceKind).toScreenPanelBounds()
                sink.drawRect(content.x, content.y, content.width, content.height, chromeContentScrim(0.62f))
                return
            }
            ChromeFramePainter.drawFrame(
                sink = sink.asChromeFrameSink(),
                request =
                    ChromeFrameDrawRequest(
                        bounds = frameBounds,
                        assets = chromeAssets.frameAssets,
                        fillColor = fill,
                        borderColor = border,
                    ),
            )
            val content = ChromeFramePainter.contentBounds(frameBounds, surfaceKind).toScreenPanelBounds()
            sink.drawRect(content.x, content.y, content.width, content.height, chromeContentScrim(0.62f))
        }

        private fun drawOutline(
            sink: StandaloneChromeDrawSink,
            bounds: ScreenPanelBounds,
            stroke: Float,
            color: Color,
        ) {
            sink.drawRect(bounds.x, bounds.y, bounds.width, stroke, color)
            sink.drawRect(bounds.x, bounds.top - stroke, bounds.width, stroke, color)
            sink.drawRect(bounds.x, bounds.y, stroke, bounds.height, color)
            sink.drawRect(bounds.right - stroke, bounds.y, stroke, bounds.height, color)
        }

        private fun chromeContentScrim(alpha: Float): Color =
            Color.valueOf("05070A").also { color -> color.a = alpha }
    }

    private fun solidTexture(): Texture =
        Pixmap(1, 1, Pixmap.Format.RGBA8888).let { pixmap ->
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            Texture(pixmap).also { texture ->
                texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
                pixmap.dispose()
            }
        }
}

internal object DarkStandaloneScreenLayout {
    const val width: Float = 960f
    const val height: Float = 540f
    const val marginX: Float = 80f
    const val headerTopY: Float = 472f
    const val footerBaselineY: Float = 42f
    const val validationEntryMinStepY: Float = 36f
    const val validationFooterControlsBaselineY: Float = 54f
    const val outcomeBodyTopPaddingY: Float = 18f
    const val outcomeBodyStepY: Float = 26f

    private const val VALIDATION_ENTRY_TOP_PADDING_Y = 10f
    private const val VALIDATION_ENTRY_COLUMN_GAP = 24f
    private const val VALIDATION_ENTRY_MAX_STEP_Y = 42f
    private const val VALIDATION_ENTRY_SINGLE_COLUMN_MAX_CHARS = 72
    private const val VALIDATION_ENTRY_TWO_COLUMN_MAX_CHARS = 34
    private const val VALIDATION_ENTRY_THREE_COLUMN_MAX_CHARS = 22

    fun mainMenu(): StandaloneScreenLayout {
        val tokens = UiDesignTokens.fixed
        return StandaloneScreenLayout(
            background = ScreenPanelBounds(0f, 0f, tokens.standaloneWidth, tokens.standaloneHeight),
            header = ScreenPanelBounds(marginX, 380f, 800f, 112f),
            primaryActionStack = ScreenPanelBounds(marginX, 126f, tokens.actionStackWidth, 248f),
            secondaryPanel = ScreenPanelBounds(438f, 126f, 442f, 248f),
            disabledDetailArea = ScreenPanelBounds(marginX, 82f, 800f, 36f),
            footerHelp = ScreenPanelBounds(marginX, 28f, 800f, 44f),
        )
    }

    fun validationSetup(): StandaloneScreenLayout {
        val tokens = UiDesignTokens.fixed
        return StandaloneScreenLayout(
            background = ScreenPanelBounds(0f, 0f, tokens.standaloneWidth, tokens.standaloneHeight),
            header = ScreenPanelBounds(marginX, 402f, 800f, 90f),
            primaryActionStack = ScreenPanelBounds(marginX, 74f, tokens.validationListWidth, 252f),
            secondaryPanel = ScreenPanelBounds(marginX, 350f, tokens.validationListWidth, 50f),
            disabledDetailArea = ScreenPanelBounds(marginX, 328f, tokens.validationListWidth, 18f),
            footerHelp = ScreenPanelBounds(marginX, 28f, 800f, 44f),
        )
    }

    fun runtimeStatus(
        worldWidth: Float,
        worldHeight: Float,
    ): StandaloneScreenLayout {
        val tokens = UiDesignTokens.fixed
        val panelWidth =
            tokens.standaloneContentWidth.coerceAtMost(
                (worldWidth - marginX * 2f).coerceAtLeast(tokens.actionStackWidth),
            )
        val panelX = ((worldWidth - panelWidth) / 2f).coerceAtLeast(32f)
        val bodyY = (worldHeight - 360f).coerceAtLeast(136f)
        return StandaloneScreenLayout(
            background = ScreenPanelBounds(0f, 0f, worldWidth, worldHeight),
            header = ScreenPanelBounds(panelX, (worldHeight - 184f).coerceAtLeast(320f), panelWidth, 96f),
            primaryActionStack = ScreenPanelBounds(panelX, bodyY, panelWidth, 144f),
            secondaryPanel = ScreenPanelBounds(panelX, bodyY, panelWidth, 144f),
            disabledDetailArea = ScreenPanelBounds(panelX, 108f, panelWidth, 36f),
            footerHelp = ScreenPanelBounds(panelX, 44f, panelWidth, 52f),
        )
    }

    fun outcome(): StandaloneScreenLayout =
        StandaloneScreenLayout(
            background = ScreenPanelBounds(0f, 0f, width, height),
            header = ScreenPanelBounds(marginX, 408f, 800f, 84f),
            primaryActionStack = ScreenPanelBounds(marginX, 112f, 800f, 284f),
            secondaryPanel = ScreenPanelBounds(marginX, 112f, 800f, 284f),
            disabledDetailArea = ScreenPanelBounds(marginX, 72f, 800f, 34f),
            footerHelp = ScreenPanelBounds(marginX, 28f, 800f, 44f),
        )

    fun validationEntryPlacements(entryCount: Int): List<ValidationSetupEntryPlacement> {
        val layout = validationSetup()
        val entriesContent = layout.primaryActionStack.insetForChromeFrame()
        val columnCount =
            when {
                entryCount <= validationSingleColumnCapacity() -> 1
                entryCount <= validationTwoColumnCapacity() -> 2
                else -> 3
            }
        val rowCount = ((entryCount + columnCount - 1) / columnCount).coerceAtLeast(1)
        val columnWidth =
            (entriesContent.width - VALIDATION_ENTRY_COLUMN_GAP * (columnCount - 1)) / columnCount
        val entryStepY = validationEntryRowStep(rowCount)
        val maxChars =
            when (columnCount) {
                1 -> VALIDATION_ENTRY_SINGLE_COLUMN_MAX_CHARS
                2 -> VALIDATION_ENTRY_TWO_COLUMN_MAX_CHARS
                else -> VALIDATION_ENTRY_THREE_COLUMN_MAX_CHARS
            }

        return List(entryCount) { index ->
            val column = index / rowCount
            val row = index % rowCount
            ValidationSetupEntryPlacement(
                x = entriesContent.x + column * (columnWidth + VALIDATION_ENTRY_COLUMN_GAP),
                baselineY = entriesContent.top - VALIDATION_ENTRY_TOP_PADDING_Y - row * entryStepY,
                maxChars = maxChars,
            )
        }
    }

    fun validationEntryRowStep(rowCount: Int): Float {
        val layout = validationSetup()
        val entriesContent = layout.primaryActionStack.insetForChromeFrame()
        val availableHeight = entriesContent.height - VALIDATION_ENTRY_TOP_PADDING_Y * 2f
        return (availableHeight / rowCount.coerceAtLeast(1)).coerceIn(validationEntryMinStepY, VALIDATION_ENTRY_MAX_STEP_Y)
    }

    fun outcomeBodyLineBaselines(lineCount: Int): List<Float> {
        val layout = outcome()
        val bodyContent = layout.primaryActionStack.insetForChromeFrame()
        return List(lineCount.coerceAtMost(outcomeBodyLineCapacity())) { index ->
            bodyContent.top - outcomeBodyTopPaddingY - index * outcomeBodyStepY
        }
    }

    fun outcomeBodyLineCapacity(): Int {
        val layout = outcome()
        val bodyContent = layout.primaryActionStack.insetForChromeFrame()
        val availableHeight = bodyContent.height - outcomeBodyTopPaddingY
        return (availableHeight / outcomeBodyStepY).toInt().coerceAtLeast(1)
    }

    fun truncate(
        text: String,
        maxChars: Int,
    ): String {
        if (maxChars <= 0) {
            return ""
        }
        if (text.length <= maxChars) {
            return text
        }
        if (maxChars == 1) {
            return "…"
        }
        return text.take(maxChars - 1) + "…"
    }

    private fun validationSingleColumnCapacity(): Int {
        val layout = validationSetup()
        val entriesContent = layout.primaryActionStack.insetForChromeFrame()
        val availableHeight = entriesContent.height - VALIDATION_ENTRY_TOP_PADDING_Y * 2f
        return (availableHeight / validationEntryMinStepY).toInt().coerceAtLeast(1)
    }

    private fun validationTwoColumnCapacity(): Int = validationSingleColumnCapacity() * 2
}

internal fun ScreenPanelBounds.insetForChromeFrame(kind: ChromeSurfaceKind = ChromeSurfaceKind.Panel): ScreenPanelBounds =
    ChromeFramePainter.contentBounds(toChromeFrameBounds(), kind).toScreenPanelBounds()

internal fun ScreenPanelBounds.maxChars(): Int = (width / 14f).toInt().coerceAtLeast(1)

internal fun ScreenPanelBounds.fitText(
    text: String,
    style: TileTextStyle,
): String = TileTextMetrics.truncateTextToWidth(text, safeTextWidth(), style)

internal fun ScreenPanelBounds.wrapText(
    text: String,
    style: TileTextStyle,
    maxLines: Int,
): List<String> {
    val safeWidth = safeTextWidth()
    val lines = TileTextMetrics.wrapTextToWidth(text, safeWidth, style)
    if (lines.size <= maxLines) {
        return lines
    }
    if (maxLines <= 1) {
        return listOf(TileTextMetrics.truncateTextToWidth(lines.joinToString(" "), safeWidth, style))
    }
    return lines.take(maxLines - 1) +
        TileTextMetrics.truncateTextToWidth(lines.drop(maxLines - 1).joinToString(" "), safeWidth, style)
}

private fun ScreenPanelBounds.safeTextWidth(): Float = (width - UiDesignTokens.spacing.xs * 2f).coerceAtLeast(1f)

private fun ScreenPanelBounds.toChromeFrameBounds(): ChromeFrameBounds =
    ChromeFrameBounds(x = x, y = y, width = width, height = height)

private fun ChromeFrameBounds.toScreenPanelBounds(): ScreenPanelBounds =
    ScreenPanelBounds(x = x, y = y, width = width, height = height)

private fun StandaloneChromeDrawSink.asChromeFrameSink(): ChromeFrameDrawSink =
    object : ChromeFrameDrawSink {
        override fun drawRect(draw: ChromeFrameRectDraw) {
            val bounds = draw.bounds
            this@asChromeFrameSink.drawRect(bounds.x, bounds.y, bounds.width, bounds.height, draw.color)
        }

        override fun drawAsset(draw: ChromeFrameAssetDraw) {
            val bounds = draw.bounds
            this@asChromeFrameSink.drawAsset(draw.asset, bounds.x, bounds.y, bounds.width, bounds.height, draw.alpha)
        }
    }

private fun ScreenPanelBounds.sameBounds(other: ScreenPanelBounds): Boolean =
    x == other.x &&
        y == other.y &&
        width == other.width &&
        height == other.height
