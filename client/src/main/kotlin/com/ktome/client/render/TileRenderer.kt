package com.ktome.client.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.ktome.client.assets.ClientTextureRepository
import com.ktome.client.assets.DarkUiMapVisualKeys
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.render.layout.GameShellBounds
import com.ktome.client.render.layout.GameShellLayout
import com.ktome.client.render.layout.DemoShellLayout
import com.ktome.client.render.layout.InfoSurfaceLayout
import com.ktome.client.render.layout.InfoSurfaceLayoutRequest
import com.ktome.client.render.layout.InfoSurfaceLayoutSolver
import com.ktome.client.render.layout.RectInt
import com.ktome.client.ui.chrome.ChromeFrameAssetDraw
import com.ktome.client.ui.chrome.ChromeFrameAssets
import com.ktome.client.ui.chrome.ChromeFrameBounds
import com.ktome.client.ui.chrome.ChromeFrameDrawRequest
import com.ktome.client.ui.chrome.ChromeFrameDrawSink
import com.ktome.client.ui.chrome.ChromeFramePainter
import com.ktome.client.ui.chrome.ChromeFrameRectDraw
import com.ktome.client.ui.chrome.ChromeSurfaceKind
import com.ktome.client.ui.layout.PaneFocusAnchor
import com.ktome.client.ui.talent.ActiveSlotChoiceModalItem
import com.ktome.client.ui.talent.ActiveSlotChoiceModalItemKind
import com.ktome.client.ui.talent.TalentAssignFooterHintModel
import com.ktome.client.ui.talent.TalentAssignListViewportModel
import com.ktome.client.ui.talent.TalentAssignListViewportRequest
import com.ktome.client.ui.talent.TalentAssignPanelModel
import com.ktome.client.ui.talent.TalentAssignPanelLayoutRequest
import com.ktome.client.ui.talent.TalentAssignPanelLayoutSolver
import com.ktome.client.ui.talent.TalentAssignSectionModel
import com.ktome.client.ui.talent.TalentAssignTreeRowModel
import com.ktome.client.ui.talent.TalentDetailBlock
import com.ktome.client.ui.talent.TalentDetailBlockKind
import com.ktome.client.ui.talent.TalentLegendItemKind
import com.ktome.client.ui.talent.TalentPreviewToneToken
import com.ktome.client.ui.talent.TalentTreeNodeToneToken
import com.ktome.client.ui.talent.toTalentTreeSelectionIdentity
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.map.Point
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.i18n.Localizer
import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class TileTextStyle {
    TITLE,
    UI,
    SMALL,
    CAPTION,
}

internal enum class TileLayerFlushReason {
    BACKGROUND,
    MAP_TERRAIN_BASE,
    MAP_CELL_MATERIAL,
    MAP_PROP_ATMOSPHERE,
    MAP_PROPS_AND_DECALS,
    MAP_SPRITE_OVERLAYS_AND_TELEGRAPHS,
    MAP_ACTORS,
    MAP_FOG_VEILS,
    MAP_GROUND_LOOT_ATMOSPHERE,
    MAP_GROUND_LOOT_MARKERS,
    MAP_PLAYER_INDICATOR,
    MAP_TARGETING_HIGHLIGHTS,
    MAP_ACTIVE_CURSOR,
    MAP_COMBAT_FEEDBACK,
    MAP_ROOM_COMPOSITOR,
    MAP_FRONTSTAGE_SURFACE,
    SHELL_OUTER_FRAME,
    MAP_STAGE_FRAME,
    SHELL_NAV_RAIL,
    SHELL_RIGHT_PANEL,
    SHELL_BOTTOM_HERO,
    SHELL_BOTTOM_ACTION_DECK,
    SHELL_BOTTOM_COMMAND_HINTS,
    SHELL_BOTTOM_LOG_DECK,
    SHELL_PANES,
    BOTTOM_HUD,
    OVERLAY_PASSIVE_TOOLTIP,
    OVERLAY_TOAST,
    OVERLAY_MODAL_BACKDROP,
    OVERLAY_MODAL,
    OVERLAY_MODAL_EXPLICIT_TOOLTIP,
    DEBUG_HINTS,
}

internal data class TileFloatBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float
        get() = x + width

    val top: Float
        get() = y + height
}

internal data class TileTextPosition(
    val x: Float,
    val y: Float,
)

internal data class TileRectDraw(
    val bounds: TileFloatBounds,
    val color: Color,
)

internal data class TileAssetSourceRegion(
    val leftRatio: Float,
    val bottomRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float,
)

internal data class TileAssetDraw(
    val asset: ResolvedVisualAsset,
    val bounds: TileFloatBounds,
    val alpha: Float = 1f,
    val tintColorHex: String? = null,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val sourceRegion: TileAssetSourceRegion? = null,
)

internal data class TileTextDraw(
    val style: TileTextStyle,
    val text: String,
    val position: TileTextPosition,
    val color: Color,
)

internal fun tileBounds(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
): TileFloatBounds =
    TileFloatBounds(
        x = x,
        y = y,
        width = width,
        height = height,
    )

internal fun tilePosition(
    x: Float,
    y: Float,
): TileTextPosition =
    TileTextPosition(
        x = x,
        y = y,
    )

internal interface TileCanvas {
    fun drawRect(draw: TileRectDraw)

    fun drawAsset(draw: TileAssetDraw)

    fun drawText(draw: TileTextDraw)

    fun flushLayer(reason: TileLayerFlushReason)
}

internal fun TileCanvas.drawRect(
    bounds: TileFloatBounds,
    color: Color,
) {
    drawRect(
        TileRectDraw(
            bounds = bounds,
            color = color,
        ),
    )
}

internal fun TileCanvas.drawAsset(
    asset: ResolvedVisualAsset,
    bounds: TileFloatBounds,
    alpha: Float = 1f,
    tintColorHex: String? = null,
    flipX: Boolean = false,
    flipY: Boolean = false,
) {
    drawAsset(
        TileAssetDraw(
            asset = asset,
            bounds = bounds,
            alpha = alpha,
            tintColorHex = tintColorHex,
            flipX = flipX,
            flipY = flipY,
        ),
    )
}

internal fun TileCanvas.drawText(
    style: TileTextStyle,
    text: String,
    position: TileTextPosition,
    color: Color,
) {
    drawText(
        TileTextDraw(
            style = style,
            text = text,
            position = position,
            color = color,
        ),
    )
}

internal object NoOpTileCanvas : TileCanvas {
    override fun drawRect(draw: TileRectDraw) = Unit

    override fun drawAsset(draw: TileAssetDraw) = Unit

    override fun drawText(draw: TileTextDraw) = Unit

    override fun flushLayer(reason: TileLayerFlushReason) = Unit
}

internal data class MapRenderFrame(
    val model: TileRenderModel,
    val layerPlan: TileMapLayerPlan,
    val layout: TileLayoutMetrics,
    val viewport: TileMapViewport,
)

internal data class ShellRenderFrame(
    val model: TileRenderModel,
    val layout: TileLayoutMetrics,
    val textLayout: ShellTextLayout,
    val paneFocusAnchor: PaneFocusAnchor?,
)

internal data class OverlayRenderFrame(
    val overlayModel: TileOverlayModel,
    val shellContentBounds: GameShellBounds,
    val modalSafeBounds: com.ktome.client.render.layout.ModalSafeBounds,
    val bottomLogReservedBounds: com.ktome.client.render.layout.GameShellBounds,
    val textMetrics: TileTextMetrics,
    val chromeAssets: TileChromeAssets? = null,
)

internal data class ShellTextLayout(
    val playerName: String,
    val summaryLines: List<String>,
    val targetTitle: String?,
    val targetLines: List<String>,
    val messageLines: List<TileMessageLine>,
    val leftRail: ShellPanelTextLayout,
    val rightPanel: ShellPanelTextLayout,
    val footerHints: String,
    val hotbar: List<ShellHotbarTextLayout>,
)

internal data class ShellPanelTextLayout(
    val title: String,
    val rows: List<ShellPanelRowTextLayout>,
)

internal data class ShellPanelRowTextLayout(
    val text: String,
    val tone: TileTextTone,
    val icon: ResolvedVisualAsset?,
    val extraIcons: List<ResolvedVisualAsset>,
    val frame: ResolvedVisualAsset?,
    val selected: Boolean,
    val style: TileTextStyle,
)

internal data class ShellHotbarTextLayout(
    val hotkey: String,
    val label: String,
    val detail: String,
    val detailTone: TileTextTone,
)

internal data class TileRenderDiagnostics(
    val viewport: TileMapViewport,
    val overlayFrame: OverlayRenderFrame,
)

internal data class TileLayoutMetrics(
    val shell: GameShellLayout,
    val demoShell: DemoShellLayout,
    val mapOffsetY: Float,
    val worldWidth: Float,
    val worldHeight: Float,
    val sidebarX: Float,
    val sidebarWidth: Float,
    val bottomInset: Float,
    val panelGap: Float,
    val cardY: Float,
    val cardHeight: Float,
    val infoX: Float,
    val infoWidth: Float,
    val logX: Float,
    val logWidth: Float,
    val focusX: Float,
    val focusWidth: Float,
    val hotbarX: Float,
    val hotbarY: Float,
    val hotbarCardWidth: Float,
    val hotbarCardHeight: Float,
    val hotbarGap: Float,
    val footerHintBounds: GameShellBounds,
)

class TileRenderer(
    private val localizer: Localizer,
    private val visualResolver: VisualManifestResolver,
    private val textureRepository: ClientTextureRepository,
    private val cellWidth: Float = 32f,
    private val cellHeight: Float = 32f,
) : Disposable {
    private val uiFont = KtomeFonts.createUiFont(size = UiDesignTokens.typography.ui)
    private val smallFont = KtomeFonts.createUiFont(size = UiDesignTokens.typography.body)
    private val captionFont = KtomeFonts.createUiFont(size = UiDesignTokens.typography.caption)
    private val titleFont = KtomeFonts.createUiFont(size = UiDesignTokens.typography.title)
    private val whitePixel = solidTexture()
    private var viewportState: TileMapViewportState? = null

    fun render(
        batch: SpriteBatch,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ) {
        viewportState =
            renderToCanvas(
            localizer = localizer,
            visualResolver = visualResolver,
            snapshot = snapshot,
            overlayState = overlayState,
            canvas = GdxTileCanvas(batch),
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            previousViewportState = viewportState,
            shellWorldWidth = Companion.worldWidth(snapshot, cellWidth, cellHeight),
            shellWorldHeight = Companion.worldHeight(snapshot, cellWidth, cellHeight),
        ).viewport.state
    }

    override fun dispose() {
        titleFont.dispose()
        uiFont.dispose()
        smallFont.dispose()
        captionFont.dispose()
        whitePixel.dispose()
    }

    private inner class GdxTileCanvas(
        private val batch: SpriteBatch,
    ) : TileCanvas {
        private var hasPendingDraw = false

        override fun drawRect(draw: TileRectDraw) {
            val previous = Color(batch.color)
            batch.color = draw.color
            batch.draw(whitePixel, draw.bounds.x, draw.bounds.y, draw.bounds.width, draw.bounds.height)
            batch.color = previous
            hasPendingDraw = true
        }

        override fun drawAsset(draw: TileAssetDraw) {
            val texture = textureRepository.textureFor(draw.asset)
            val previous = Color(batch.color)
            batch.color =
                draw.tintColorHex
                    ?.let { tint -> Color.valueOf(tint.removePrefix("#")).also { color -> color.a = draw.alpha } }
                    ?: Color(1f, 1f, 1f, draw.alpha)
            val source = draw.sourceRegion?.toTextureSource(texture)
            if (source != null || draw.flipX || draw.flipY) {
                val sourceRect =
                    source ?: TextureSourceRect(
                        x = 0,
                        y = 0,
                        width = texture.width,
                        height = texture.height,
                    )
                batch.draw(
                    texture,
                    draw.bounds.x,
                    draw.bounds.y,
                    draw.bounds.width,
                    draw.bounds.height,
                    sourceRect.x,
                    sourceRect.y,
                    sourceRect.width,
                    sourceRect.height,
                    draw.flipX,
                    draw.flipY,
                )
            } else {
                batch.draw(texture, draw.bounds.x, draw.bounds.y, draw.bounds.width, draw.bounds.height)
            }
            batch.color = previous
            hasPendingDraw = true
        }

        private fun TileAssetSourceRegion.toTextureSource(texture: Texture): TextureSourceRect {
            val sourceX = (leftRatio.coerceIn(0f, 1f) * texture.width).roundToInt().coerceAtMost(texture.width - 1)
            val sourceY =
                ((1f - bottomRatio.coerceIn(0f, 1f) - heightRatio.coerceIn(0.01f, 1f)) * texture.height)
                    .roundToInt()
                    .coerceAtLeast(0)
                    .coerceAtMost(texture.height - 1)
            val sourceWidth =
                (widthRatio.coerceIn(0.01f, 1f) * texture.width)
                    .roundToInt()
                    .coerceAtLeast(1)
                    .coerceAtMost(texture.width - sourceX)
            val sourceHeight =
                (heightRatio.coerceIn(0.01f, 1f) * texture.height)
                    .roundToInt()
                    .coerceAtLeast(1)
                    .coerceAtMost(texture.height - sourceY)
            return TextureSourceRect(
                x = sourceX,
                y = sourceY,
                width = sourceWidth,
                height = sourceHeight,
            )
        }

        override fun drawText(draw: TileTextDraw) {
            val font =
                when (draw.style) {
                    TileTextStyle.TITLE -> titleFont
                    TileTextStyle.UI -> uiFont
                    TileTextStyle.SMALL -> smallFont
                    TileTextStyle.CAPTION -> captionFont
                }
            font.color = draw.color
            font.draw(batch, draw.text, draw.position.x, draw.position.y)
            hasPendingDraw = true
        }

        override fun flushLayer(reason: TileLayerFlushReason) {
            if (!hasPendingDraw) {
                return
            }
            batch.flush()
            hasPendingDraw = false
        }
    }

    private data class TextureSourceRect(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    )

    private fun solidTexture(): Texture =
        Pixmap(1, 1, Pixmap.Format.RGBA8888).let { pixmap ->
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            Texture(pixmap).also { texture ->
                texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
                pixmap.dispose()
            }
        }

    companion object {
        const val messageRows: Int = 5
        const val uiRows: Int = 7
        const val sidebarColumns: Int = 28
        private const val SHELL_PANEL_TITLE_OFFSET = 26f
        private const val SHELL_PANEL_ROW_HEIGHT = 21f
        private const val SHELL_SMALL_LINE_HEIGHT = 19f
        private val TALENT_ASSIGN_BODY_STYLE = TileTextStyle.SMALL

        internal fun buildRenderModel(
            localizer: Localizer,
            visualResolver: VisualManifestResolver,
            snapshot: RenderSnapshot,
            overlayState: OverlayState,
        ): TileRenderModel = TileRenderModelBuilder.build(localizer, visualResolver, snapshot, overlayState)

        internal fun renderHeadless(
            localizer: Localizer,
            visualResolver: VisualManifestResolver,
            snapshot: RenderSnapshot,
            overlayState: OverlayState,
            cellWidth: Float = 32f,
            cellHeight: Float = 32f,
        ) {
            renderToCanvas(
                localizer = localizer,
                visualResolver = visualResolver,
                snapshot = snapshot,
                overlayState = overlayState,
                canvas = NoOpTileCanvas,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
            )
        }

        internal fun renderToCanvas(
            localizer: Localizer,
            visualResolver: VisualManifestResolver,
            snapshot: RenderSnapshot,
            overlayState: OverlayState,
            canvas: TileCanvas,
            cellWidth: Float = 32f,
            cellHeight: Float = 32f,
            previousViewportState: TileMapViewportState? = null,
            shellWorldWidth: Float = UiDesignTokens.fixed.shellPreferredWorldWidth,
            shellWorldHeight: Float = UiDesignTokens.fixed.shellPreferredWorldHeight,
        ): TileRenderDiagnostics {
            val model = buildRenderModel(localizer, visualResolver, snapshot, overlayState)
            val mapWidth = snapshot.metadata.width
            val mapHeight = snapshot.metadata.height
            val snapshotDimensions = TileMapDimensions(mapWidth, mapHeight)
            require(model.mapDimensions == snapshotDimensions) {
                "Render model dimensions ${model.mapDimensions} must match snapshot dimensions $snapshotDimensions."
            }
            val layout = layoutMetrics(mapWidth, mapHeight, cellWidth, cellHeight, shellWorldWidth, shellWorldHeight)
            val projection =
                TileViewportFocusProjection.resolve(
                    TileViewportFocusProjectionRequest(
                        playerTile = model.playerTile,
                        overlayState = overlayState,
                        validationInspectProjection = validationInspectProjection(overlayState),
                    ),
                )
            val viewport =
                TileMapViewport.resolve(
                    identity =
                        TileMapViewportIdentity(
                            zoneId = snapshot.metadata.zoneId,
                            currentFloor = snapshot.metadata.currentFloor,
                            mapDimensions = TileMapDimensions(snapshot.metadata.width, snapshot.metadata.height),
                            cellSize = cellWidth.roundToInt(),
                            cellAlignedMapBounds = layout.shell.cellAlignedMapBounds,
                            focusMode = projection.resolvedMode,
                        ),
                    playerTile = model.playerTile,
                    focusTile = projection.resolvedFocusTile,
                    previousState = previousViewportState,
                )
            val layerPlan = TileLayerComposer.compose(model, projection)
            val overlayModel =
                TileOverlayModelBuilder.build(
                    TileOverlayModelRequest(
                        renderModel = model,
                        overlayState = overlayState,
                        projection = projection,
                        anchorResolver = FrameTileOverlayAnchorResolver(viewport),
                        shellContentBounds = layout.shell.shellContentBounds,
                        viewportBounds = gameBounds(0f, 0f, layout.worldWidth, layout.worldHeight),
                        modalSafeBounds = layout.shell.modalSafeBounds,
                        bottomLogReservedBounds = layout.shell.bottomLogReservedBounds,
                        explicitModalTooltip = panelTooltip(model, layout),
                    ),
                )
            val overlayFrame =
                OverlayRenderFrame(
                    overlayModel = overlayModel,
                    shellContentBounds = layout.shell.shellContentBounds,
                    modalSafeBounds = layout.shell.modalSafeBounds,
                    bottomLogReservedBounds = layout.shell.bottomLogReservedBounds,
                    textMetrics = TileTextMetrics,
                    chromeAssets = model.chromeAssets,
                )

            val shellFrame = buildShellRenderFrame(model, layout, overlayState)
            canvas.drawRect(tileBounds(0f, 0f, layout.worldWidth, layout.worldHeight), UiDesignTokens.color.surface.base.color())
            canvas.flushLayer(TileLayerFlushReason.BACKGROUND)
            DemoShellRenderer.renderOuterFrame(canvas, shellFrame)
            canvas.flushLayer(TileLayerFlushReason.SHELL_OUTER_FRAME)
            DemoShellRenderer.renderMapStageFrame(canvas, shellFrame)
            canvas.flushLayer(TileLayerFlushReason.MAP_STAGE_FRAME)
            val mapFrame = MapRenderFrame(model, layerPlan, layout, viewport)
            TileMapRenderer.render(canvas, mapFrame)
            DemoShellRenderer.renderFrontstageSurface(canvas, shellFrame)
            canvas.flushLayer(TileLayerFlushReason.MAP_FRONTSTAGE_SURFACE)
            DemoShellRenderer.renderShell(canvas, shellFrame)
            renderOverlayFrame(canvas, overlayFrame)
            return TileRenderDiagnostics(
                viewport = viewport,
                overlayFrame = overlayFrame,
            )
        }

        internal fun renderOverlayFrame(
            canvas: TileCanvas,
            frame: OverlayRenderFrame,
        ) {
            TileOverlayRenderer.render(canvas, frame)
        }

        private fun buildShellRenderFrame(
            model: TileRenderModel,
            layout: TileLayoutMetrics,
            overlayState: OverlayState,
        ): ShellRenderFrame =
            ShellRenderFrame(
                model = model,
                layout = layout,
                textLayout = buildShellTextLayout(model, layout),
                paneFocusAnchor = overlayState.paneFocusAnchor.takeIf { overlayState.mode == UiMode.MAP },
            )

        private fun validationInspectProjection(overlayState: OverlayState): ValidationInspectProjection? {
            if (overlayState.mode != UiMode.VALIDATION) {
                return null
            }
            return overlayState.validationPanel?.inspectCursor?.let { cursor ->
                ValidationInspectProjection(
                    cursor = cursor,
                    reason = ValidationProjectionReason.MANUAL_VALIDATION_PROBE,
                )
            }
        }

        private fun panelTooltip(
            model: TileRenderModel,
            layout: TileLayoutMetrics,
        ): TileTooltipModel? {
            val tooltip = model.panelTooltip ?: return null
            val anchorBounds =
                when (tooltip.anchorKind) {
                    TilePanelTooltipAnchorKind.EQUIPMENT_SLOT ->
                        layout.demoShell.rightPanelLayout.equipmentSlots.slotBounds.getOrNull(tooltip.anchorIndex)
                    TilePanelTooltipAnchorKind.INSCRIPTION_SLOT ->
                        layout.demoShell.rightPanelLayout.inscriptionSlots.slotBounds.getOrNull(tooltip.anchorIndex)
                    TilePanelTooltipAnchorKind.BACKPACK_SLOT ->
                        layout.demoShell.rightPanelLayout.backpackSlots.slotBounds.getOrNull(tooltip.anchorIndex)
                } ?: return null
            val rect = anchorBounds.toRectInt()
            return TileTooltipModel(
                anchor =
                    ResolvedTileOverlayAnchor(
                        source = TileOverlayAnchor.PanelSlot(rect, tooltip.anchorId),
                        bounds = rect,
                        coordinateAuthority = TileOverlayCoordinateAuthority.SHELL_LAYOUT,
                    ),
                titleLine = tooltip.titleLine,
                bodyLines = tooltip.bodyLines,
                placedRect = rect,
            )
        }

        private fun buildShellTextLayout(
            model: TileRenderModel,
            layout: TileLayoutMetrics,
        ): ShellTextLayout {
            val leftContent = chromeContentBounds(layout.shell.leftRailBounds, ChromeSurfaceKind.Panel)
            val rightContent = chromeContentBounds(layout.shell.rightPanelBounds, ChromeSurfaceKind.Panel)
            val infoContent = chromeContentBounds(gameBounds(layout.infoX, layout.cardY, layout.infoWidth, layout.cardHeight), ChromeSurfaceKind.Panel)
            val focusContent = chromeContentBounds(gameBounds(layout.focusX, layout.cardY, layout.focusWidth, layout.cardHeight), ChromeSurfaceKind.Panel)
            return ShellTextLayout(
                playerName =
                    TileTextMetrics.truncateTextToWidth(
                        model.playerCard.name.ifBlank { model.playerCard.emptyStateText },
                        infoContent.width,
                        TileTextStyle.UI,
                    ),
                summaryLines =
                    packSummaryLines(
                        summaryText = model.hud.summaryText,
                        maxWidth = safeTextWidth(focusContent.width),
                        maxLines = 2,
                    ),
                targetTitle =
                    model.targetCard.title?.let { title ->
                        TileTextMetrics.truncateTextToWidth(title, safeTextWidth(focusContent.width - 42f), TileTextStyle.SMALL)
                    },
                targetLines =
                    model.targetCard.lines.map { line ->
                        TileTextMetrics.truncateTextToWidth(line, safeTextWidth(focusContent.width), TileTextStyle.SMALL)
                    },
                messageLines = buildMessageTextLayout(model, layout),
                leftRail =
                    buildPanelTextLayout(
                        panel = model.shell.leftRail,
                        width = safeTextWidth(leftContent.width),
                        maxRows = ((leftContent.height - SHELL_PANEL_TITLE_OFFSET) / SHELL_PANEL_ROW_HEIGHT).toInt().coerceAtLeast(0),
                    ),
                rightPanel =
                    buildPanelTextLayout(
                        panel = model.shell.rightPanel,
                        width = safeTextWidth(rightContent.width),
                        maxRows = ((rightContent.height - SHELL_PANEL_TITLE_OFFSET) / SHELL_PANEL_ROW_HEIGHT).toInt().coerceAtLeast(0),
                    ),
                footerHints =
                    TileTextMetrics.truncateTextToWidth(
                        model.shell.footerHints.joinToString("  ") { hint -> hint.text },
                        safeTextWidth(layout.footerHintBounds.width),
                        TileTextStyle.SMALL,
                ),
                hotbar = buildHotbarTextLayout(model, layout),
            )
        }

        private fun buildMessageTextLayout(
            model: TileRenderModel,
            layout: TileLayoutMetrics,
        ): List<TileMessageLine> {
            val logContent = chromeContentBounds(gameBounds(layout.logX, layout.cardY, layout.logWidth, layout.cardHeight), ChromeSurfaceKind.Panel)
            val maxRows = maxMessageRows(logContent)
            val overlayMessages =
                if (model.messageLines.size > model.logPresentation.entries.size) {
                    model.messageLines.drop(model.logPresentation.entries.size)
                } else {
                    emptyList()
                }
            val displayLines = model.messageLines.take(model.logPresentation.entries.size) + overlayMessages
            return displayLines.flatMap { line -> wrapMessageLine(line, safeTextWidth(logContent.width)) }.takeLast(maxRows)
        }

        private fun wrapMessageLine(
            line: TileMessageLine,
            maxWidth: Float,
        ): List<TileMessageLine> {
            val firstLineMaxWidth = (maxWidth - if (line.icon == null) 0f else 24f).coerceAtLeast(1f)
            return TileTextMetrics.wrapTextToWidth(line.text, firstLineMaxWidth, TileTextStyle.SMALL).flatMapIndexed { index, text ->
                if (index == 0) {
                    listOf(text)
                } else {
                    TileTextMetrics.wrapTextToWidth(text, maxWidth, TileTextStyle.SMALL)
                }
            }.mapIndexed { index, text ->
                val lineWidth = if (index == 0 && line.icon != null) firstLineMaxWidth else maxWidth
                line.copy(
                    text = TileTextMetrics.truncateTextToWidth(text, lineWidth, TileTextStyle.SMALL),
                    icon = line.icon.takeIf { index == 0 },
                )
            }
        }

        private fun buildPanelTextLayout(
            panel: TilePanelModel,
            width: Float,
            maxRows: Int,
        ): ShellPanelTextLayout {
            return ShellPanelTextLayout(
                title = TileTextMetrics.truncateTextToWidth(panel.title, width, TileTextStyle.UI),
                rows =
                    panel.rows.take(maxRows).map { row ->
                        val iconCount = row.visualIconCount()
                        val style = if (row.tone == TileTextTone.GOLD && iconCount == 0) TileTextStyle.UI else TileTextStyle.SMALL
                        val rowWidth = width - if (iconCount == 0) 0f else 20f * iconCount.coerceAtMost(3)
                        ShellPanelRowTextLayout(
                            text = TileTextMetrics.truncateTextToWidth(row.text, rowWidth.coerceAtLeast(1f), style),
                            tone = row.tone,
                            icon = row.icon,
                            extraIcons = row.extraIcons,
                            frame = row.frame,
                            selected = row.selected,
                            style = style,
                        )
                    },
            )
        }

        private fun maxMessageRows(bounds: GameShellBounds): Int =
            ((bounds.height - 8f) / SHELL_SMALL_LINE_HEIGHT).toInt().coerceIn(1, messageRows)

        private fun buildHotbarTextLayout(
            model: TileRenderModel,
            layout: TileLayoutMetrics,
        ): List<ShellHotbarTextLayout> {
            val maxWidth = (layout.hotbarCardWidth - 16f).coerceAtLeast(1f)
            return model.actionPanel.entries.mapIndexed { index, entry ->
                val slot = model.hud.hotbar.getOrNull(index)
                val cooldown = slot?.cooldownText
                ShellHotbarTextLayout(
                    hotkey = entry.hotkey,
                    label = TileTextMetrics.truncateTextToWidth(entry.label, maxWidth, TileTextStyle.SMALL),
                    detail =
                        TileTextMetrics.truncateTextToWidth(
                            cooldown ?: slot?.resourceText.orEmpty(),
                            maxWidth,
                            TileTextStyle.SMALL,
                        ),
                    detailTone = if (cooldown == null) TileTextTone.LIGHT_GRAY else TileTextTone.RED,
                )
            }
        }

        private fun safeTextWidth(width: Float): Float = (width - UiDesignTokens.spacing.xs * 2f).coerceAtLeast(1f)

        internal fun worldWidth(
            snapshot: RenderSnapshot,
            cellWidth: Float = 32f,
            cellHeight: Float = 32f,
        ): Float = UiDesignTokens.fixed.shellPreferredWorldWidth

        internal fun worldHeight(
            snapshot: RenderSnapshot,
            cellWidth: Float = 32f,
            cellHeight: Float = 32f,
        ): Float {
            val preferredWidth = UiDesignTokens.fixed.shellPreferredWorldWidth
            val preferredHeight = UiDesignTokens.fixed.shellPreferredWorldHeight
            val graphics = Gdx.graphics ?: return preferredHeight
            val screenWidth = graphics.width.takeIf { width -> width > 0 } ?: return preferredHeight
            val screenHeight = graphics.height.takeIf { height -> height > 0 } ?: return preferredHeight
            val aspectMatchedHeight = preferredWidth * screenHeight.toFloat() / screenWidth.toFloat()
            return maxOf(preferredHeight, aspectMatchedHeight)
        }

        internal fun sidebarTitle(
            localizer: Localizer,
            mode: UiMode,
        ): String =
            when (mode) {
                UiMode.MAP -> localizer.text("ui.sidebar.ground")
                UiMode.SHOP -> localizer.text("ui.sidebar.shop")
                UiMode.WORLD_MAP -> localizer.text("ui.sidebar.world_map")
                UiMode.INVENTORY -> localizer.text("ui.sidebar.inventory")
                UiMode.LOADOUT_EDIT -> localizer.text("ui.sidebar.loadout")
                UiMode.TARGETING -> localizer.text("ui.sidebar.targeting")
                UiMode.INSPECT -> localizer.text("ui.sidebar.inspect")
                UiMode.VALIDATION -> localizer.text("ui.sidebar.validation")
                UiMode.STAT_ASSIGN -> localizer.text("ui.sidebar.assign_stats")
                UiMode.TALENT_ASSIGN -> localizer.text("ui.sidebar.improve_talents")
            }

        private object TileMapRenderer {
            fun render(
                canvas: TileCanvas,
                frame: MapRenderFrame,
            ) {
                val viewport = frame.viewport
                val compositorStrategy = frame.model.roomPresentationPlan.compositorStrategy
                val usesRoomArtPlateGroundMaterial = compositorStrategy.usesRoomArtPlateGroundMaterial
                val usesRoomArtPlateInteractionGrammar = compositorStrategy.usesRoomArtPlateInteractionGrammar
                val (groundTerrain, upperTerrain) =
                    frame.layerPlan.terrainBase.partition { placement -> placement.asset.entry.category == "tile_ground" }
                if (!usesRoomArtPlateGroundMaterial) {
                    groundTerrain.forEach { placement -> drawPlacement(canvas, placement, viewport) }
                }
                upperTerrain.forEach { placement ->
                    val alphaScale =
                        if (
                            compositorStrategy == RoomCompositorStrategy.TOPOLOGY_RISK_HYBRID_PRESENTATION &&
                            placement.asset.entry.category == "tile_wall"
                        ) {
                            if (!shouldDrawTopologyRiskWallTerrainAnchor(placement, frame.model.roomPresentationPlan.artPlate)) {
                                return@forEach
                            }
                            0.60f
                        } else {
                            1f
                        }
                    drawPlacement(canvas, placement, viewport, alphaScale = alphaScale)
                }
                canvas.flushLayer(TileLayerFlushReason.MAP_TERRAIN_BASE)
                drawVisibleRoomFoundationGlaze(canvas, frame)
                frame.layerPlan.cellMaterials
                    .filterNot { material -> usesRoomArtPlateGroundMaterial && material.kind == TileMapCellMaterialKind.FLOOR }
                    .forEach { material -> drawCellMaterial(canvas, material, viewport) }
                canvas.flushLayer(TileLayerFlushReason.MAP_CELL_MATERIAL)
                renderRoomCompositor(canvas, frame)
                canvas.flushLayer(TileLayerFlushReason.MAP_ROOM_COMPOSITOR)
                drawPropAtmosphere(canvas, frame.layerPlan.propsAndDecals, viewport)
                canvas.flushLayer(TileLayerFlushReason.MAP_PROP_ATMOSPHERE)
                frame.layerPlan.propsAndDecals.forEach { placement -> drawPlacement(canvas, placement, viewport) }
                canvas.flushLayer(TileLayerFlushReason.MAP_PROPS_AND_DECALS)
                if (usesRoomArtPlateInteractionGrammar) {
                    frame.layerPlan.spriteOverlaysAndTelegraphs.forEach { placement ->
                        drawRoomArtPlateSpriteOverlay(canvas, placement, viewport)
                    }
                } else {
                    frame.layerPlan.spriteOverlaysAndTelegraphs.forEach { placement -> drawPlacement(canvas, placement, viewport) }
                }
                canvas.flushLayer(TileLayerFlushReason.MAP_SPRITE_OVERLAYS_AND_TELEGRAPHS)
                frame.layerPlan.actors.forEach { placement -> drawActorGroundingShadow(canvas, placement, viewport) }
                frame.layerPlan.actors.forEach { placement -> drawPlacement(canvas, placement, viewport) }
                canvas.flushLayer(TileLayerFlushReason.MAP_ACTORS)
                if (usesRoomArtPlateInteractionGrammar) {
                    drawRoomArtPlatePlayerIndicators(canvas, frame.layerPlan.playerIndicators, viewport)
                } else {
                    drawPlayerIndicators(canvas, frame.layerPlan.playerIndicators, viewport)
                }
                canvas.flushLayer(TileLayerFlushReason.MAP_PLAYER_INDICATOR)
                if (usesRoomArtPlateInteractionGrammar) {
                    drawRoomArtPlateGroundLootAtmosphere(canvas, frame.layerPlan.groundLootMarkers, viewport)
                } else {
                    drawGroundLootAtmosphere(canvas, frame.layerPlan.groundLootMarkers, viewport)
                }
                canvas.flushLayer(TileLayerFlushReason.MAP_GROUND_LOOT_ATMOSPHERE)
                if (usesRoomArtPlateInteractionGrammar) {
                    drawRoomArtPlateGroundLootMarkers(canvas, frame.layerPlan.groundLootMarkers, viewport)
                } else {
                    drawGroundLootMarkers(canvas, frame.layerPlan.groundLootMarkers, viewport)
                }
                canvas.flushLayer(TileLayerFlushReason.MAP_GROUND_LOOT_MARKERS)
                if (usesRoomArtPlateInteractionGrammar) {
                    drawRoomArtPlateFogOverlays(canvas, frame)
                } else {
                    drawFogOverlays(canvas, frame.layerPlan.fogVeils, viewport)
                }
                if (usesRoomArtPlateInteractionGrammar) {
                    drawRoomArtPlateApertureShoulders(canvas, frame)
                }
                drawHiddenStageApertureMasonry(canvas, frame, visibleRoomClip(frame) ?: viewport.mapBounds)
                canvas.flushLayer(TileLayerFlushReason.MAP_FOG_VEILS)
                drawTargetHighlights(canvas, frame)
                canvas.flushLayer(TileLayerFlushReason.MAP_TARGETING_HIGHLIGHTS)
                frame.layerPlan.activeCursor?.let { cursor ->
                    val cursorColor =
                        if (cursor.mode == TileViewportFocusMode.TARGETING) {
                            targetCursorColor(cursor.state)
                        } else {
                            UiDesignTokens.color.focus.ring.color()
                        }
                    if (usesRoomArtPlateInteractionGrammar) {
                        drawRoomArtPlateCursor(canvas, cursor.tile, viewport, cursorColor)
                    } else {
                        drawCursor(canvas, cursor.tile, viewport, cursorColor)
                    }
                }
                canvas.flushLayer(TileLayerFlushReason.MAP_ACTIVE_CURSOR)
                drawCombatFeedback(canvas, frame.layerPlan.combatFeedback, viewport)
                canvas.flushLayer(TileLayerFlushReason.MAP_COMBAT_FEEDBACK)
            }

            private fun shouldDrawTopologyRiskWallTerrainAnchor(
                placement: TileVisualPlacement,
                artPlate: RoomArtPlateModel?,
            ): Boolean {
                val visiblePoints = artPlate?.topology?.shape?.visiblePoints ?: return true
                val point = Point(placement.x, placement.y)
                val openNeighborCount =
                    if (point in visiblePoints) {
                        listOf(
                            Point(point.x, point.y + 1),
                            Point(point.x, point.y - 1),
                            Point(point.x - 1, point.y),
                            Point(point.x + 1, point.y),
                        ).count { neighbor -> neighbor !in visiblePoints }
                    } else {
                        0
                    }
                val hash = (point.x * 31 + point.y * 17).mod(9)
                return hash == 0 || openNeighborCount >= 2 && hash == 1
            }

            private fun renderRoomCompositor(
                canvas: TileCanvas,
                frame: MapRenderFrame,
            ) {
                when (frame.model.roomPresentationPlan.compositorStrategy) {
                    RoomCompositorStrategy.LEGACY_TILE_DECORATION -> renderLegacyRoomDecoration(canvas, frame)
                    RoomCompositorStrategy.ART_PLATE_PRESENTATION -> {
                        RoomArtPlateRenderer.render(canvas, frame)
                        renderRoomArtPlateReadability(canvas, frame)
                    }
                    RoomCompositorStrategy.TOPOLOGY_RISK_HYBRID_PRESENTATION -> {
                        renderLegacyRoomDecoration(canvas, frame)
                        RoomArtPlateRenderer.render(canvas, frame)
                    }
                }
            }

            private fun renderLegacyRoomDecoration(
                canvas: TileCanvas,
                frame: MapRenderFrame,
            ) {
                drawVisibleRoomFloorUnifier(canvas, frame)
                drawVisibleRoomAtmosphere(canvas, frame)
                drawVisibleRoomApertureHierarchy(canvas, frame)
                renderWarmMapOverlay(canvas, frame)
            }

            private fun renderRoomArtPlateReadability(
                canvas: TileCanvas,
                frame: MapRenderFrame,
            ) {
                drawRoomArtPlateTargetTopologyHints(canvas, frame)
                visibleRoomClip(frame)?.let { roomClip -> drawRoomArtPlateEdgeFeather(canvas, frame, roomClip) }
            }
        }

        private object TileOverlayRenderer {
            fun render(
                canvas: TileCanvas,
                frame: OverlayRenderFrame,
            ) {
                val model = frame.overlayModel
                if (model.activeModal == null && model.selectedTooltip != null) {
                    drawTooltip(canvas, model.selectedTooltip, frame.chromeAssets)
                }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_PASSIVE_TOOLTIP)
                model.toast?.let { toast ->
                    canvas.drawRect(
                        tileBounds(frame.shellContentBounds.x + 16f, frame.shellContentBounds.top - 48f, 360f, 34f),
                        UiDesignTokens.color.surface.overlay.color(),
                    )
                    canvas.drawText(
                        TileTextStyle.SMALL,
                        toast.line.text,
                        tilePosition(frame.shellContentBounds.x + 28f, frame.shellContentBounds.top - 26f),
                        tone(toast.line.tone),
                    )
                }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_TOAST)
                model.modalBackdrop?.let { backdrop ->
                    canvas.drawRect(
                        tileBounds(backdrop.bounds.x, backdrop.bounds.y, backdrop.bounds.width, backdrop.bounds.height),
                        backdrop.color.color().also { color -> color.a = backdrop.alpha },
                    )
                }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_MODAL_BACKDROP)
                model.activeModal?.let { modal -> drawModal(canvas, modal, frame.chromeAssets) }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_MODAL)
                if (model.activeModal != null && model.selectedTooltip != null) {
                    drawTooltip(canvas, model.selectedTooltip, frame.chromeAssets)
                }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_MODAL_EXPLICIT_TOOLTIP)
                canvas.flushLayer(TileLayerFlushReason.DEBUG_HINTS)
            }
        }

        private fun drawTooltip(
            canvas: TileCanvas,
            tooltip: TileTooltipModel,
            chromeAssets: TileChromeAssets?,
        ) {
            val rect = tooltip.placedRect
            canvas.drawRect(
                tileBounds(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat()),
                UiDesignTokens.color.surface.overlay.color(),
            )
            chromeAssets?.let { chrome ->
                drawChromeFrame(
                    canvas = canvas,
                    assets = chrome.frameAssets.copy(body = chrome.tooltipBody),
                    bounds = gameBounds(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat()),
                    fillColor = UiDesignTokens.color.surface.overlay.color(),
                    borderColor = UiDesignTokens.color.focus.ring.color(),
                    alpha = 0.78f,
                )
            }
            canvas.drawRect(tileBounds(rect.x.toFloat(), (rect.top - 3).toFloat(), rect.width.toFloat(), 3f), UiDesignTokens.color.focus.ring.color())
            val content = chromeContentBounds(gameBounds(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat()), ChromeSurfaceKind.Tooltip)
            drawContentScrim(canvas, content, alpha = 0.76f)
            val lineHeight = SHELL_SMALL_LINE_HEIGHT
            val titleBaseline = content.top - 4f
            if (titleBaseline - lineHeight >= content.y) {
                canvas.drawText(TileTextStyle.SMALL, tooltip.titleLine.text, tilePosition(content.x, titleBaseline), tone(tooltip.titleLine.tone))
            }
            tooltip.bodyLines.take(TILE_TOOLTIP_BODY_LINE_LIMIT).forEachIndexed { index, line ->
                val baseline = titleBaseline - lineHeight * (index + 1)
                if (baseline - lineHeight < content.y) {
                    return@forEachIndexed
                }
                val fitted = TileTextMetrics.truncateTextToWidth(line.text, content.width, TileTextStyle.SMALL)
                canvas.drawText(TileTextStyle.SMALL, fitted, tilePosition(content.x, baseline), tone(line.tone))
            }
        }

        private fun drawModal(
            canvas: TileCanvas,
            modal: TileModalModel,
            chromeAssets: TileChromeAssets?,
        ) {
            modal.talentAssignPanel?.let { panel ->
                drawTalentAssignModal(canvas, modal, panel, chromeAssets)
                return
            }
            modal.inventoryWorkbench?.let { workbench ->
                drawInventoryWorkbenchModal(canvas, modal, workbench, chromeAssets)
                return
            }
            val bounds = modal.bounds
            canvas.drawRect(
                tileBounds(bounds.x.toFloat(), bounds.y.toFloat(), bounds.width.toFloat(), bounds.height.toFloat()),
                UiDesignTokens.color.surface.raised.color(),
            )
            chromeAssets?.let { chrome ->
                drawChromeFrame(
                    canvas = canvas,
                    assets = chrome.frameAssets.copy(body = chrome.modalBody),
                    bounds = gameBounds(bounds.x.toFloat(), bounds.y.toFloat(), bounds.width.toFloat(), bounds.height.toFloat()),
                    fillColor = UiDesignTokens.color.surface.raised.color(),
                    borderColor = UiDesignTokens.color.quality.rare.color(),
                    alpha = 0.82f,
                )
            }
            canvas.drawRect(tileBounds(bounds.x.toFloat(), (bounds.top - 4).toFloat(), bounds.width.toFloat(), 4f), UiDesignTokens.color.quality.rare.color())
            val content = chromeContentBounds(gameBounds(bounds.x.toFloat(), bounds.y.toFloat(), bounds.width.toFloat(), bounds.height.toFloat()), ChromeSurfaceKind.Modal)
            drawContentScrim(canvas, content, alpha = 0.76f)
            canvas.drawText(TileTextStyle.UI, modal.titleLine.text, tilePosition(content.x, content.top - 4f), tone(modal.titleLine.tone))
            modal.bodyLines.take(10).forEachIndexed { index, line ->
                val fitted = TileTextMetrics.truncateTextToWidth(line.text, content.width, TileTextStyle.SMALL)
                canvas.drawText(TileTextStyle.SMALL, fitted, tilePosition(content.x, content.top - 42f - index * 24f), tone(line.tone))
            }
            modal.footerHintLines.take(2).forEachIndexed { index, line ->
                val fitted = TileTextMetrics.truncateTextToWidth(line.text, content.width, TileTextStyle.SMALL)
                canvas.drawText(TileTextStyle.SMALL, fitted, tilePosition(content.x, content.y + 12f + index * 22f), tone(line.tone))
            }
        }

        private fun drawInventoryWorkbenchModal(
            canvas: TileCanvas,
            modal: TileModalModel,
            workbench: InventoryWorkbenchPresentation,
            chromeAssets: TileChromeAssets?,
        ) {
            val layout = requireNotNull(modal.inventoryWorkbenchLayout) { "Inventory workbench modal requires resolved layout." }
            canvas.drawRect(
                tileBounds(layout.root.x, layout.root.y, layout.root.width, layout.root.height),
                color("05070A", 0.94f),
            )
            chromeAssets?.let { chrome ->
                drawChromeFrame(
                    canvas = canvas,
                    assets = chrome.frameAssets.copy(body = chrome.modalBody),
                    bounds = layout.root,
                    fillColor = color("05070A", 0.94f),
                    borderColor = UiDesignTokens.color.quality.rare.color(),
                    alpha = 0.90f,
                )
            }
            drawInventoryWorkbenchColumn(canvas, layout.equipmentColumn)
            drawInventoryWorkbenchColumn(canvas, layout.backpackColumn)
            drawInventoryWorkbenchColumn(canvas, layout.detailColumn)
            drawFittedText(canvas, workbench.title, layout.content.x, layout.root.top - 24f, layout.content.width, TileTextStyle.UI, tone(TileTextTone.GOLD))
            drawInventoryWorkbenchEquipment(canvas, workbench, layout.equipmentColumn, layout.equipmentSlotBounds)
            drawInventoryWorkbenchGrid(canvas, workbench, layout.backpackColumn, layout.backpackCellBounds)
            drawInventoryWorkbenchDetail(canvas, workbench, layout.detailColumn, layout.detailMaxLines)
            drawInventoryWorkbenchFooter(canvas, workbench, layout.footer)
        }

        private fun drawInventoryWorkbenchColumn(
            canvas: TileCanvas,
            bounds: GameShellBounds,
        ) {
            canvas.drawRect(tileBounds(bounds.x, bounds.y, bounds.width, bounds.height), color("080A0E", 0.82f))
            canvas.drawRect(tileBounds(bounds.x, bounds.top - 2f, bounds.width, 2f), color("B89B68", 0.46f))
            canvas.drawRect(tileBounds(bounds.x, bounds.y, 1f, bounds.height), color("5D4A31", 0.42f))
            canvas.drawRect(tileBounds(bounds.right - 1f, bounds.y, 1f, bounds.height), color("5D4A31", 0.36f))
        }

        private fun drawInventoryWorkbenchEquipment(
            canvas: TileCanvas,
            workbench: InventoryWorkbenchPresentation,
            column: GameShellBounds,
            slotBounds: List<GameShellBounds>,
        ) {
            drawFittedText(canvas, workbench.equipmentTitle, column.x + 14f, column.top - 24f, column.width - 28f, TileTextStyle.SMALL, tone(TileTextTone.GOLD))
            workbench.equipmentSockets.zip(slotBounds).forEach { (slot, bounds) ->
                val alpha = if (slot.enabled) 1f else 0.38f
                canvas.drawAsset(slot.frame, tileBounds(bounds.x, bounds.y, bounds.width, bounds.height), alpha)
                slot.itemIcon?.let { icon ->
                    canvas.drawAsset(icon, tileBounds(bounds.x + 7f, bounds.y + 7f, bounds.width - 14f, bounds.height - 14f), alpha)
                }
                if (slot.selected || slot.targetCue) {
                    drawThinRect(canvas, bounds, UiDesignTokens.color.focus.ring.color(), 2f)
                }
                if (slot.visualOnly) {
                    canvas.drawRect(tileBounds(bounds.x + bounds.width * 0.42f, bounds.y + bounds.height * 0.42f, bounds.width * 0.16f, bounds.height * 0.16f), color("6B5A42", 0.42f))
                }
                if (!slot.visualOnly && slot.label.isNotBlank()) {
                    drawFittedText(canvas, slot.label, bounds.x, bounds.y - 8f, bounds.width, TileTextStyle.SMALL, tone(TileTextTone.LIGHT_GRAY))
                }
            }
        }

        private fun drawInventoryWorkbenchGrid(
            canvas: TileCanvas,
            workbench: InventoryWorkbenchPresentation,
            column: GameShellBounds,
            cellBounds: List<GameShellBounds>,
        ) {
            drawFittedText(canvas, workbench.backpackTitle, column.x + 14f, column.top - 24f, column.width * 0.52f, TileTextStyle.SMALL, tone(TileTextTone.GOLD))
            drawFittedText(canvas, workbench.grid.capacityText, column.x + column.width * 0.52f, column.top - 24f, column.width * 0.42f, TileTextStyle.SMALL, tone(TileTextTone.LIGHT_GRAY))
            workbench.grid.cells.zip(cellBounds).forEach { (cell, bounds) ->
                canvas.drawAsset(cell.frame, tileBounds(bounds.x, bounds.y, bounds.width, bounds.height), if (cell.empty) 0.70f else 1f)
                cell.itemIcon?.let { icon ->
                    val iconPadding =
                        when {
                            bounds.width >= 64f -> 8f
                            bounds.width >= 58f -> 6f
                            else -> 5f
                        }
                    canvas.drawAsset(icon, tileBounds(bounds.x + iconPadding, bounds.y + iconPadding, bounds.width - iconPadding * 2f, bounds.height - iconPadding * 2f))
                }
                if (cell.focused) {
                    drawThinRect(canvas, bounds, UiDesignTokens.color.focus.ring.color(), 2f)
                }
                if (cell.hovered && !cell.focused) {
                    drawThinRect(canvas, bounds, color("1CB7C8", 0.50f), 1f)
                }
                cell.quantityText?.let { quantity ->
                    val badgeWidth = (quantity.length * 8 + 12).toFloat()
                    canvas.drawRect(tileBounds(bounds.right - badgeWidth, bounds.y + 2f, badgeWidth, 18f), color("05070A", 0.88f))
                    drawFittedText(canvas, quantity, bounds.right - badgeWidth + 5f, bounds.y + 15f, badgeWidth - 8f, TileTextStyle.SMALL, tone(TileTextTone.GOLD))
                }
            }
            drawFittedText(canvas, workbench.grid.pageLabel, column.x + 14f, column.y + 22f, column.width - 28f, TileTextStyle.SMALL, tone(TileTextTone.LIGHT_GRAY))
        }

        private fun drawInventoryWorkbenchDetail(
            canvas: TileCanvas,
            workbench: InventoryWorkbenchPresentation,
            column: GameShellBounds,
            detailMaxLines: Int,
        ) {
            drawFittedText(canvas, workbench.detailTitle, column.x + 14f, column.top - 24f, column.width - 28f, TileTextStyle.SMALL, tone(TileTextTone.GOLD))
            val iconSize = 54f
            workbench.selectedItemIcon?.let { icon ->
                canvas.drawRect(tileBounds(column.x + 14f, column.top - 92f, iconSize, iconSize), color("05070A", 0.82f))
                canvas.drawAsset(icon, tileBounds(column.x + 20f, column.top - 86f, iconSize - 12f, iconSize - 12f))
            }
            val titleX = if (workbench.selectedItemIcon == null) column.x + 14f else column.x + 82f
            drawFittedText(canvas, workbench.selectedItemTitle, titleX, column.top - 56f, column.right - titleX - 14f, TileTextStyle.UI, tone(workbench.selectedItemTone))
            var baseline = column.top - 112f
            workbench.detailRows.take(detailMaxLines).forEach { row ->
                drawFittedText(canvas, row.value, column.x + 14f, baseline, column.width - 28f, TileTextStyle.SMALL, tone(row.tone))
                baseline -= 22f
            }
            val actionY = column.y + 86f
            val actionsTitleBaseline = actionY + 48f
            val compareLineHeight = 22f
            baseline -= 8f
            drawFittedText(canvas, workbench.compareTitle, column.x + 14f, baseline, column.width - 28f, TileTextStyle.SMALL, tone(TileTextTone.GOLD))
            baseline -= 24f
            val fittingCompareRows = ((baseline - actionsTitleBaseline - compareLineHeight) / compareLineHeight).toInt().coerceIn(0, 5)
            workbench.compareRows.take(fittingCompareRows).forEach { row ->
                drawFittedText(canvas, row.value, column.x + 14f, baseline, column.width - 28f, TileTextStyle.SMALL, tone(row.tone))
                baseline -= compareLineHeight
            }
            drawFittedText(canvas, workbench.actionsTitle, column.x + 14f, actionsTitleBaseline, column.width - 28f, TileTextStyle.SMALL, tone(TileTextTone.GOLD))
            var actionX = column.x + 14f
            var actionRowY = actionY
            val rowStartX = actionX
            val rowRight = column.right - 14f
            workbench.actions.forEach { action ->
                val keyWidth = (action.shortcutText.length * 8 + 22).toFloat().coerceAtLeast(34f)
                val preferredLabelWidth = 96f
                var labelWidth = minOf(preferredLabelWidth, rowRight - actionX - keyWidth - 8f).coerceAtLeast(34f)
                if (actionX + keyWidth + 6f + labelWidth > rowRight && actionX > rowStartX) {
                    actionX = rowStartX
                    actionRowY -= 29f
                    labelWidth = minOf(preferredLabelWidth, rowRight - actionX - keyWidth - 8f).coerceAtLeast(34f)
                }
                val actionTone = if (action.enabled) tone(TileTextTone.WHITE) else tone(TileTextTone.GRAY)
                canvas.drawRect(tileBounds(actionX, actionRowY, keyWidth, 24f), color("111820", if (action.enabled) 0.88f else 0.42f))
                drawFittedText(canvas, action.shortcutText, actionX + 8f, actionRowY + 16f, keyWidth - 16f, TileTextStyle.SMALL, actionTone)
                drawFittedText(canvas, action.label, actionX + keyWidth + 6f, actionRowY + 16f, labelWidth, TileTextStyle.SMALL, actionTone)
                actionX += keyWidth + labelWidth + 16f
            }
        }

        private fun drawInventoryWorkbenchFooter(
            canvas: TileCanvas,
            workbench: InventoryWorkbenchPresentation,
            footer: GameShellBounds,
        ) {
            canvas.drawRect(tileBounds(footer.x, footer.y, footer.width, footer.height), color("07090B", 0.86f))
            var cursorX = footer.x + 14f
            val baseline = footer.y + 31f
            val hintGap = 14f
            val footerBudget = footer.width - 28f
            val fullHintWidths =
                workbench.footerHints.map { hint ->
                    val keyWidth = (TileTextMetrics.approximateTextWidth(hint.keyText, TileTextStyle.SMALL) + 20f).coerceAtLeast(34f)
                    val labelWidth = (TileTextMetrics.approximateTextWidth(hint.label, TileTextStyle.SMALL) + 18f).coerceIn(58f, 116f)
                    keyWidth + labelWidth
                }
            val compact = fullHintWidths.sum() + hintGap * (workbench.footerHints.size - 1).coerceAtLeast(0) > footerBudget
            for (hint in workbench.footerHints) {
                val keyWidth = (TileTextMetrics.approximateTextWidth(hint.keyText, TileTextStyle.SMALL) + 20f).coerceAtLeast(34f)
                val labelWidth =
                    if (compact) {
                        0f
                    } else {
                        (TileTextMetrics.approximateTextWidth(hint.label, TileTextStyle.SMALL) + 18f).coerceIn(58f, 116f)
                    }
                if (cursorX + keyWidth + labelWidth > footer.right - 14f) {
                    break
                }
                canvas.drawRect(tileBounds(cursorX, baseline - 18f, keyWidth, 25f), color("111820", 0.86f))
                drawFittedText(canvas, hint.keyText, cursorX + 8f, baseline, keyWidth - 16f, TileTextStyle.SMALL, tone(TileTextTone.WHITE))
                if (!compact) {
                    drawFittedText(canvas, hint.label, cursorX + keyWidth + 8f, baseline, labelWidth, TileTextStyle.SMALL, tone(TileTextTone.LIGHT_GRAY))
                }
                cursorX += keyWidth + labelWidth + hintGap
            }
        }

        private fun drawTalentAssignModal(
            canvas: TileCanvas,
            modal: TileModalModel,
            renderModel: TileTalentAssignPanelRenderModel,
            chromeAssets: TileChromeAssets?,
        ) {
            val bounds = modal.bounds
            val visualBounds = modal.visualBounds ?: bounds
            val panel = renderModel.panel
            canvas.drawRect(
                tileBounds(
                    visualBounds.x.toFloat(),
                    visualBounds.y.toFloat(),
                    visualBounds.width.toFloat(),
                    visualBounds.height.toFloat(),
                ),
                talentAssignPanelFill(),
            )
            chromeAssets?.let { chrome ->
                drawChromeFrame(
                    canvas = canvas,
                    assets = chrome.frameAssets.copy(body = chrome.modalBody),
                    bounds =
                        gameBounds(
                            visualBounds.x.toFloat(),
                            visualBounds.y.toFloat(),
                            visualBounds.width.toFloat(),
                            visualBounds.height.toFloat(),
                        ),
                    fillColor = talentAssignPanelFill(),
                    borderColor = talentAssignFrameGold(),
                    alpha = 0.92f,
                )
            }
            val layout =
                TalentAssignPanelLayoutSolver.resolve(
                    TalentAssignPanelLayoutRequest(
                        ChromeFrameBounds(
                            x = bounds.x.toFloat(),
                            y = bounds.y.toFloat(),
                            width = bounds.width.toFloat(),
                            height = bounds.height.toFloat(),
                        ),
                    ),
                )
            val content = layout.modal.contentBounds.toGameShellBounds()
            drawContentScrim(canvas, content, alpha = 0.93f)
            drawTalentAssignReferenceChrome(
                canvas = canvas,
                bounds =
                    gameBounds(
                        visualBounds.x.toFloat(),
                        visualBounds.y.toFloat(),
                        visualBounds.width.toFloat(),
                        visualBounds.height.toFloat(),
                    ),
                content = content,
                referenceChromeAssets = renderModel.referenceChromeAssets,
            )

            canvas.drawText(
                TileTextStyle.TITLE,
                panel.header.title,
                tilePosition(layout.header.textX, layout.header.titleBaseline),
                talentAssignEmberGold(),
            )
            val pointLine = listOfNotNull(panel.header.professionPointText, panel.header.racePointText).joinToString("     ")
            drawFittedText(
                canvas,
                pointLine,
                layout.header.textX,
                layout.header.pointsBaseline,
                layout.header.pointsWidth,
                TileTextStyle.UI,
                talentAssignBodyText(),
            )

            val hasReferenceSurface = renderModel.referenceChromeAssets.containsKey(TileTalentAssignReferenceChromeSlot.SURFACE_TEXTURE)
            val leftBodyAlpha = if (hasReferenceSurface) 0.66f else 0.58f
            val rightBodyAlpha = if (hasReferenceSurface) 0.12f else 0.52f
            canvas.drawRect(layout.body.list.bounds.toTileBounds(), color("050607", leftBodyAlpha))
            canvas.drawRect(layout.body.right.columnBounds.toTileBounds(), color("050607", rightBodyAlpha))
            if (hasReferenceSurface) {
                canvas.drawRect(tileBounds(content.x, content.y, content.width, 34f), color("3B322A", 0.12f))
            }
            val listEntries = talentAssignListEntries(panel)
            val listViewport = talentAssignListViewport(listEntries, layout.body.list.visibleSlots)
            drawTalentAssignScrollBar(
                canvas = canvas,
                x = layout.body.scrollbar.bounds.x,
                top = layout.body.scrollbar.bounds.top,
                bottom = layout.body.scrollbar.bounds.y,
                viewport = listViewport,
            )
            canvas.drawRect(tileBounds(layout.body.dividerX, layout.body.list.bounds.y, 1f, layout.body.list.bounds.height), color("4E3D29", 0.62f))

            drawTalentAssignList(
                canvas = canvas,
                renderModel = renderModel,
                entries = listEntries,
                viewport = listViewport,
                x = layout.body.list.bounds.x,
                top = layout.body.list.bounds.top,
                width = layout.body.list.bounds.width,
                bottom = layout.body.list.bounds.y,
            )
            drawTalentAssignDetail(
                canvas = canvas,
                renderModel = renderModel,
                x = layout.body.right.detailBounds.x,
                top = layout.body.right.detailBounds.top,
                width = layout.body.right.detailBounds.width,
                bottom = layout.body.right.detailBounds.y,
            )
            drawTalentAssignFooter(canvas, renderModel, layout.footer.bounds.x, layout.footer.baseline, layout.footer.bounds.width)
            panel.activeSlotChoiceModal?.let {
                drawActiveSlotChoiceModal(
                    canvas = canvas,
                    renderModel = renderModel,
                    x = layout.body.right.activeSlotChoiceBounds.x,
                    y = layout.body.right.activeSlotChoiceBounds.y,
                    width = layout.body.right.activeSlotChoiceBounds.width,
                )
            }
        }

        private fun drawTalentAssignReferenceChrome(
            canvas: TileCanvas,
            bounds: GameShellBounds,
            content: GameShellBounds,
            referenceChromeAssets: Map<TileTalentAssignReferenceChromeSlot, ResolvedVisualAsset>,
        ) {
            val hasReferenceChrome =
                TileTalentAssignReferenceChromeSlot.entries.all { slot -> referenceChromeAssets.containsKey(slot) }
            val outerAlpha = if (hasReferenceChrome) 0.22f else 0.48f
            val verticalAlpha = if (hasReferenceChrome) 0.18f else 0.40f
            val separatorAlpha = if (hasReferenceChrome) 0.20f else 0.42f
            referenceChromeAssets[TileTalentAssignReferenceChromeSlot.SURFACE_TEXTURE]?.let { asset ->
                canvas.drawAsset(asset, tileBounds(bounds.x, bounds.y, bounds.width, bounds.height), 1f)
            }
            canvas.drawRect(tileBounds(bounds.x + 4f, bounds.top - 6f, bounds.width - 8f, 1f), color("C0A36C", outerAlpha))
            canvas.drawRect(tileBounds(bounds.x + 4f, bounds.y + 5f, bounds.width - 8f, 1f), color("C0A36C", outerAlpha))
            canvas.drawRect(tileBounds(bounds.x + 5f, bounds.y + 6f, 1f, bounds.height - 12f), color("A58B61", verticalAlpha))
            canvas.drawRect(tileBounds(bounds.right - 6f, bounds.y + 6f, 1f, bounds.height - 12f), color("A58B61", verticalAlpha))
            canvas.drawRect(tileBounds(content.x - 4f, content.top - 50f, content.width + 8f, 1f), color("6D4520", separatorAlpha))
            if (hasReferenceChrome) {
                drawTalentAssignReferenceChromeAssets(canvas, bounds, referenceChromeAssets)
            } else {
                drawTalentAssignCornerOrnament(canvas, bounds.x + 11f, bounds.top - 11f, 1f, -1f)
                drawTalentAssignCornerOrnament(canvas, bounds.right - 11f, bounds.top - 11f, -1f, -1f)
                drawTalentAssignCornerOrnament(canvas, bounds.x + 11f, bounds.y + 11f, 1f, 1f)
                drawTalentAssignCornerOrnament(canvas, bounds.right - 11f, bounds.y + 11f, -1f, 1f)
            }
        }

        private fun drawTalentAssignReferenceChromeAssets(
            canvas: TileCanvas,
            bounds: GameShellBounds,
            referenceChromeAssets: Map<TileTalentAssignReferenceChromeSlot, ResolvedVisualAsset>,
        ) {
            referenceChromeAssets[TileTalentAssignReferenceChromeSlot.TOP_EDGE]?.let { asset ->
                canvas.drawAsset(asset, tileBounds(bounds.x, bounds.top - 16f, bounds.width, 16f), 0.96f)
            }
            referenceChromeAssets[TileTalentAssignReferenceChromeSlot.BOTTOM_EDGE]?.let { asset ->
                canvas.drawAsset(asset, tileBounds(bounds.x, bounds.y, bounds.width, 16f), 0.96f)
            }
            referenceChromeAssets[TileTalentAssignReferenceChromeSlot.LEFT_EDGE]?.let { asset ->
                canvas.drawAsset(asset, tileBounds(bounds.x, bounds.y, 18f, bounds.height), 0.96f)
            }
            referenceChromeAssets[TileTalentAssignReferenceChromeSlot.RIGHT_EDGE]?.let { asset ->
                canvas.drawAsset(asset, tileBounds(bounds.right - 18f, bounds.y, 18f, bounds.height), 0.96f)
            }
            referenceChromeAssets[TileTalentAssignReferenceChromeSlot.CORNER_TOP_LEFT]?.let { asset ->
                canvas.drawAsset(asset, tileBounds(bounds.x, bounds.top - 72f, 54f, 72f), 0.98f)
            }
            referenceChromeAssets[TileTalentAssignReferenceChromeSlot.CORNER_TOP_RIGHT]?.let { asset ->
                canvas.drawAsset(asset, tileBounds(bounds.right - 72f, bounds.top - 72f, 72f, 72f), 0.98f)
            }
            referenceChromeAssets[TileTalentAssignReferenceChromeSlot.CORNER_BOTTOM_LEFT]?.let { asset ->
                canvas.drawAsset(asset, tileBounds(bounds.x, bounds.y, 44f, 64f), 0.98f)
            }
            referenceChromeAssets[TileTalentAssignReferenceChromeSlot.CORNER_BOTTOM_RIGHT]?.let { asset ->
                canvas.drawAsset(asset, tileBounds(bounds.right - 72f, bounds.y, 72f, 64f), 0.98f)
            }
        }

        private fun drawTalentAssignCornerOrnament(
            canvas: TileCanvas,
            anchorX: Float,
            anchorY: Float,
            xDirection: Float,
            yDirection: Float,
        ) {
            val bright = color("BFA77A", 0.64f)
            val shadow = color("4E3B2B", 0.64f)
            val horizontalY = anchorY + yDirection * 8f
            val verticalX = anchorX + xDirection * 8f
            canvas.drawRect(tileBounds(minOf(anchorX, anchorX + xDirection * 30f), horizontalY, 30f, 1f), bright)
            canvas.drawRect(tileBounds(verticalX, minOf(anchorY, anchorY + yDirection * 30f), 1f, 30f), bright)
            canvas.drawRect(tileBounds(minOf(anchorX + xDirection * 8f, anchorX + xDirection * 42f), anchorY, 34f, 1f), shadow)
            canvas.drawRect(tileBounds(anchorX, minOf(anchorY + yDirection * 8f, anchorY + yDirection * 42f), 1f, 34f), shadow)
            canvas.drawRect(tileBounds(minOf(anchorX + xDirection * 18f, anchorX + xDirection * 44f), anchorY + yDirection * 18f, 26f, 1f), bright)
            canvas.drawRect(tileBounds(anchorX + xDirection * 18f, minOf(anchorY + yDirection * 18f, anchorY + yDirection * 44f), 1f, 26f), bright)
        }

        private sealed interface TalentAssignListEntry {
            data class SectionHeader(val section: TalentAssignSectionModel) : TalentAssignListEntry

            data class Row(val row: TalentAssignTreeRowModel) : TalentAssignListEntry
        }

        private fun talentAssignListViewport(
            entries: List<TalentAssignListEntry>,
            visibleSlots: Int,
        ): TalentAssignListViewportModel {
            val focusedIndex =
                entries.indexOfFirst { entry ->
                    entry is TalentAssignListEntry.Row && entry.row.focused
                }
            return TalentAssignPanelLayoutSolver.resolveListViewport(
                TalentAssignListViewportRequest(
                    totalSlots = entries.size,
                    focusedIndex = focusedIndex.takeIf { index -> index >= 0 },
                    visibleSlots = visibleSlots,
                ),
            )
        }

        private fun talentAssignListEntries(panel: TalentAssignPanelModel): List<TalentAssignListEntry> =
            buildList {
                panel.sections.forEach { section ->
                    add(TalentAssignListEntry.SectionHeader(section))
                    section.rows.forEach { row -> add(TalentAssignListEntry.Row(row)) }
                }
            }

        private fun drawTalentAssignList(
            canvas: TileCanvas,
            renderModel: TileTalentAssignPanelRenderModel,
            entries: List<TalentAssignListEntry>,
            viewport: TalentAssignListViewportModel,
            x: Float,
            top: Float,
            width: Float,
            bottom: Float,
        ) {
            var baseline = top
            val headerHeight = 30f
            entries.subList(viewport.firstVisibleIndex, viewport.endExclusiveIndex).forEach { entry ->
                if (baseline < bottom + TalentAssignPanelLayoutSolver.rowStep) {
                    return
                }
                when (entry) {
                    is TalentAssignListEntry.SectionHeader -> {
                        val section = entry.section
                        canvas.drawRect(tileBounds(x, baseline - 24f, width - 6f, headerHeight), color("2C1A0D", 0.74f))
                        canvas.drawRect(tileBounds(x, baseline - 24f, width - 6f, 1f), color("8A5A24", 0.42f))
                        canvas.drawRect(tileBounds(x, baseline + 6f, width - 6f, 1f), color("8A5A24", 0.38f))
                        drawFittedText(
                            canvas = canvas,
                            text = section.displayName,
                            x = x + 28f,
                            baseline = baseline,
                            maxWidth = width - 92f,
                            style = TALENT_ASSIGN_BODY_STYLE,
                            color = talentAssignEmberGold(),
                        )
                        drawFittedText(
                            canvas = canvas,
                            text = section.nodeCountText,
                            x = x + width - 86f,
                            baseline = baseline,
                            maxWidth = 58f,
                            style = TALENT_ASSIGN_BODY_STYLE,
                            color = talentAssignEmberGold(),
                        )
                    }

                    is TalentAssignListEntry.Row -> {
                        val row = entry.row
                        if (row.focused) {
                            canvas.drawRect(tileBounds(x, baseline - 21f, width - 6f, 28f), color("1CB7C8", 0.18f))
                            canvas.drawRect(tileBounds(x, baseline - 21f, 3f, 28f), color("1CB7C8", 0.88f))
                        }
                        val markerColor = talentTone(row.toneToken)
                        val markerX = x + 50f
                        val connectorX = x + 96f
                        val iconX = x + 140f
                        val rowTextX = iconX + 54f
                        drawFittedText(canvas, row.stateMarkerText, markerX, baseline, 42f, TALENT_ASSIGN_BODY_STYLE, markerColor)
                        if (row.connectorPrefix.isNotBlank()) {
                            val connectorColor = color("8A8173", 0.86f)
                            canvas.drawRect(tileBounds(connectorX + 7f, baseline - 20f, 1f, 28f), connectorColor)
                            canvas.drawRect(tileBounds(connectorX + 7f, baseline - 5f, 29f, 1f), connectorColor)
                            drawFittedText(canvas, row.connectorPrefix, connectorX, baseline, 40f, TALENT_ASSIGN_BODY_STYLE, talentAssignBodyText())
                        }
                        renderModel.rowIcons[row.toTalentTreeSelectionIdentity()]?.let { icon ->
                            if (icon.isPr04ReferenceCrop()) {
                                canvas.drawAsset(icon, tileBounds(iconX - 3f, baseline - 24f, 31f, 31f))
                            } else {
                                canvas.drawRect(tileBounds(iconX - 3f, baseline - 24f, 31f, 31f), color("07090B", 0.90f))
                                canvas.drawRect(tileBounds(iconX - 3f, baseline - 24f, 31f, 1f), color("B9A77E", 0.62f))
                                canvas.drawRect(tileBounds(iconX - 3f, baseline + 6f, 31f, 1f), color("B9A77E", 0.52f))
                                canvas.drawRect(tileBounds(iconX - 3f, baseline - 24f, 1f, 31f), color("B9A77E", 0.48f))
                                canvas.drawRect(tileBounds(iconX + 27f, baseline - 24f, 1f, 31f), color("B9A77E", 0.48f))
                                canvas.drawAsset(icon, tileBounds(iconX + 1f, baseline - 20f, 23f, 23f))
                            }
                        }
                        drawFittedText(
                            canvas = canvas,
                            text = row.displayName,
                            x = rowTextX,
                            baseline = baseline,
                            maxWidth = (width - (rowTextX - x) - 116f).coerceAtLeast(40f),
                            style = TALENT_ASSIGN_BODY_STYLE,
                            color = talentAssignRowNameColor(row.toneToken, row.focused),
                        )
                        drawFittedText(
                            canvas = canvas,
                            text = row.rankText,
                            x = x + width - 84f,
                            baseline = baseline,
                            maxWidth = 64f,
                            style = TALENT_ASSIGN_BODY_STYLE,
                            color = talentAssignBodyText(),
                        )
                        if (row.pendingOverlay && row.focused) {
                            canvas.drawRect(tileBounds(x + width - 9f, baseline - 14f, 5f, 14f), UiDesignTokens.color.quality.rare.color())
                        }
                    }
                }
                baseline -= TalentAssignPanelLayoutSolver.rowStep
            }
        }

        private fun drawTalentAssignScrollBar(
            canvas: TileCanvas,
            x: Float,
            top: Float,
            bottom: Float,
            viewport: TalentAssignListViewportModel,
        ) {
            val height = top - bottom
            val railWidth = 14f
            val arrowHeight = 20f
            val trackTop = top - arrowHeight
            val trackBottom = bottom + arrowHeight
            val trackHeight = (trackTop - trackBottom).coerceAtLeast(1f)
            val activeAlpha = if (viewport.hasOverflow) 1f else 0.46f
            canvas.drawRect(tileBounds(x, bottom, railWidth, height), color("080604", 0.86f))
            canvas.drawRect(tileBounds(x, top - 1f, railWidth, 1f), color("B89B68", 0.48f * activeAlpha))
            canvas.drawRect(tileBounds(x, bottom, railWidth, 1f), color("B89B68", 0.44f * activeAlpha))
            canvas.drawRect(tileBounds(x, bottom, 1f, height), color("8A6B42", 0.52f * activeAlpha))
            canvas.drawRect(tileBounds(x + railWidth - 1f, bottom, 1f, height), color("8A6B42", 0.48f * activeAlpha))
            canvas.drawRect(tileBounds(x + 2f, trackBottom, railWidth - 4f, trackHeight), color("6D5435", 0.26f * activeAlpha))
            canvas.drawRect(tileBounds(x + 4f, top - 8f, 6f, 1f), color("D0B585", 0.58f * activeAlpha))
            canvas.drawRect(tileBounds(x + 5f, top - 9f, 4f, 1f), color("D0B585", 0.58f * activeAlpha))
            canvas.drawRect(tileBounds(x + 6f, top - 10f, 2f, 1f), color("D0B585", 0.58f * activeAlpha))
            canvas.drawRect(tileBounds(x + 4f, bottom + 7f, 6f, 1f), color("D0B585", 0.54f * activeAlpha))
            canvas.drawRect(tileBounds(x + 5f, bottom + 8f, 4f, 1f), color("D0B585", 0.54f * activeAlpha))
            canvas.drawRect(tileBounds(x + 6f, bottom + 9f, 2f, 1f), color("D0B585", 0.54f * activeAlpha))
            val visibleFraction = viewport.visibleSlots.toFloat() / viewport.totalSlots.toFloat().coerceAtLeast(1f)
            val thumbHeight =
                if (viewport.hasOverflow) {
                    (trackHeight * visibleFraction).coerceIn(42f, trackHeight)
                } else {
                    (trackHeight * 0.84f).coerceAtMost(trackHeight)
                }
            val scrollFraction =
                if (viewport.maxFirstVisibleIndex == 0) {
                    0f
                } else {
                    viewport.firstVisibleIndex.toFloat() / viewport.maxFirstVisibleIndex.toFloat()
                }
            val thumbTop = trackTop - thumbHeight - (trackHeight - thumbHeight) * scrollFraction
            canvas.drawRect(tileBounds(x + 3f, thumbTop, railWidth - 6f, thumbHeight), color("A98D67", 0.56f * activeAlpha))
            canvas.drawRect(tileBounds(x + 4f, thumbTop + 3f, railWidth - 8f, thumbHeight - 6f), color("D3B37A", 0.12f * activeAlpha))
        }

        private fun drawTalentAssignDetail(
            canvas: TileCanvas,
            renderModel: TileTalentAssignPanelRenderModel,
            x: Float,
            top: Float,
            width: Float,
            bottom: Float,
        ) {
            val blocks = renderModel.panel.detail?.blocks.orEmpty()
            val heroIndex = blocks.indexOfFirst { block -> block.kind == TalentDetailBlockKind.HERO_ICON }
            val header = blocks.firstOrNull { block -> block.kind == TalentDetailBlockKind.HEADER }
            val rankAndCost = blocks.firstOrNull { block -> block.kind == TalentDetailBlockKind.RANK_AND_COST }
            val prerequisites =
                blocks.filter { block ->
                    block.kind == TalentDetailBlockKind.PREREQUISITE || block.kind == TalentDetailBlockKind.PREREQUISITE_FAILED
            }
            var baseline = top
            val heroFrameSize = 114f
            val heroIconSize = 94f
            val heroInset = (heroFrameSize - heroIconSize) / 2f
            val detailTextX = x + heroFrameSize + 26f
            if (heroIndex >= 0) {
                renderModel.detailBlockIcons[heroIndex]?.let { icon ->
                    if (icon.isPr04ReferenceCrop()) {
                        canvas.drawAsset(icon, tileBounds(x, baseline - heroFrameSize + 4f, heroFrameSize, heroFrameSize))
                    } else {
                        canvas.drawRect(tileBounds(x, baseline - heroFrameSize + 4f, heroFrameSize, heroFrameSize), color("07090B", 0.86f))
                        canvas.drawRect(tileBounds(x, baseline + 3f, heroFrameSize, 1f), color("9B7A4A", 0.70f))
                        canvas.drawRect(tileBounds(x, baseline - heroFrameSize + 4f, heroFrameSize, 1f), color("B9A77E", 0.58f))
                        canvas.drawRect(tileBounds(x, baseline - heroFrameSize + 4f, 1f, heroFrameSize), color("B9A77E", 0.52f))
                        canvas.drawRect(tileBounds(x + heroFrameSize - 1f, baseline - heroFrameSize + 4f, 1f, heroFrameSize), color("B9A77E", 0.52f))
                        canvas.drawRect(tileBounds(x + 8f, baseline - heroFrameSize + 12f, heroFrameSize - 16f, 1f), color("D1C29A", 0.36f))
                        canvas.drawRect(tileBounds(x + 8f, baseline - 4f, heroFrameSize - 16f, 1f), color("D1C29A", 0.32f))
                        canvas.drawAsset(icon, tileBounds(x + heroInset, baseline - heroFrameSize + 4f + heroInset, heroIconSize, heroIconSize))
                    }
                }
            }
            header?.let { block ->
                drawFittedText(canvas, block.primaryText, detailTextX, baseline - 2f, width - (detailTextX - x), TileTextStyle.TITLE, talentAssignEmberGold())
                block.secondaryText?.let { text ->
                    val chipX = detailTextX + TileTextMetrics.approximateTextWidth(block.primaryText, TileTextStyle.TITLE) + 36f
                    canvas.drawRect(tileBounds(chipX, baseline - 24f, 56f, 24f), color("092D35", 0.38f))
                    canvas.drawRect(tileBounds(chipX, baseline - 24f, 56f, 1f), color("1CB7C8", 0.68f))
                    canvas.drawRect(tileBounds(chipX, baseline - 1f, 56f, 1f), color("1CB7C8", 0.58f))
                    canvas.drawRect(tileBounds(chipX, baseline - 24f, 1f, 24f), color("1CB7C8", 0.58f))
                    canvas.drawRect(tileBounds(chipX + 55f, baseline - 24f, 1f, 24f), color("1CB7C8", 0.58f))
                    drawFittedText(canvas, text, chipX + 8f, baseline - 7f, 44f, TileTextStyle.SMALL, UiDesignTokens.color.focus.ring.color())
                }
            }
            baseline -= 30f
            rankAndCost?.let { block ->
                drawFittedText(canvas, block.primaryText, detailTextX, baseline, width - (detailTextX - x), TALENT_ASSIGN_BODY_STYLE, talentAssignBodyText())
                baseline -= 22f
                block.secondaryText?.let { text ->
                    drawFittedText(canvas, text, detailTextX, baseline, width - (detailTextX - x), TALENT_ASSIGN_BODY_STYLE, talentAssignBodyText())
                    baseline -= 22f
                }
                block.bodyLines.forEach { line ->
                    val text = line.label?.let { label -> "$label:  ${line.value}" } ?: line.value
                    drawFittedText(canvas, text, detailTextX, baseline, width - (detailTextX - x), TALENT_ASSIGN_BODY_STYLE, previewTone(line.toneToken))
                    baseline -= 20f
                }
            }
            prerequisites.forEach { block ->
                drawFittedText(canvas, block.primaryText, detailTextX, baseline, width - (detailTextX - x), TALENT_ASSIGN_BODY_STYLE, previewTone(block.toneToken))
                baseline -= 20f
                block.bodyLines.take(2).forEach { line ->
                    val text = line.label?.let { label -> "$label:  ${line.value}" } ?: line.value
                    drawFittedText(canvas, text, detailTextX, baseline, width - (detailTextX - x), TALENT_ASSIGN_BODY_STYLE, previewTone(line.toneToken))
                    baseline -= 18f
                }
            }
            val headerReservedHeight = ((top - bottom) * 0.27f).coerceIn(132f, 178f)
            baseline = minOf(baseline - 12f, top - headerReservedHeight)
            drawTalentAssignDivider(canvas, x, baseline + 8f, width)
            blocks
                .filter { block ->
                    block.kind == TalentDetailBlockKind.CURRENT_RANK_DETAIL ||
                        block.kind == TalentDetailBlockKind.NEXT_RANK_PREVIEW
                }.forEach { block ->
                if (baseline < bottom) {
                    return
                }
                if (block.primaryText.isNotBlank()) {
                    drawFittedText(canvas, block.primaryText, x, baseline, width, TALENT_ASSIGN_BODY_STYLE, talentAssignEmberGold())
                    baseline -= 24f
                }
                block.secondaryText?.let { text ->
                    drawFittedText(canvas, text, x, baseline, width, TALENT_ASSIGN_BODY_STYLE, talentAssignBodyText())
                    baseline -= 22f
                }
                block.bodyLines.forEach { line ->
                    if (baseline < bottom) {
                        return
                    }
                    val text = line.label?.let { label -> "$label:  ${line.value}" } ?: line.value
                    drawFittedText(canvas, text, x + 10f, baseline, width - 10f, TALENT_ASSIGN_BODY_STYLE, previewTone(line.toneToken))
                    baseline -=
                        if (block.kind == TalentDetailBlockKind.CURRENT_RANK_DETAIL) {
                            21f
                        } else {
                            20f
                        }
                }
                baseline -=
                    if (block.kind == TalentDetailBlockKind.CURRENT_RANK_DETAIL) {
                        18f
                    } else {
                        12f
                    }
                drawTalentAssignDivider(canvas, x, baseline + 7f, width)
                baseline -= 8f
            }
            blocks.firstOrNull { block -> block.kind == TalentDetailBlockKind.ACTIONS }?.let { block ->
                drawTalentAssignActions(canvas, block, x, minOf(baseline, bottom + 50f), width)
            }
        }

        private fun ResolvedVisualAsset.isPr04ReferenceCrop(): Boolean =
            entry.tags.contains("reference-crop")

        private fun drawTalentAssignDivider(
            canvas: TileCanvas,
            x: Float,
            y: Float,
            width: Float,
        ) {
            canvas.drawRect(tileBounds(x, y, width, 1f), color("8A5A24", 0.48f))
        }

        private fun drawTalentAssignActions(
            canvas: TileCanvas,
            block: TalentDetailBlock,
            x: Float,
            y: Float,
            width: Float,
        ) {
            var cursorX = x
            for (line in block.bodyLines) {
                val key = line.label ?: continue
                val label = line.value
                val keyWidth = (TileTextMetrics.approximateTextWidth(key, TALENT_ASSIGN_BODY_STYLE) + 16f).coerceIn(46f, 76f)
                val labelWidth = TileTextMetrics.approximateTextWidth(label, TALENT_ASSIGN_BODY_STYLE).coerceAtMost(112f)
                canvas.drawRect(tileBounds(cursorX, y - 18f, keyWidth, 26f), color("17100A", 0.88f))
                canvas.drawRect(tileBounds(cursorX, y + 7f, keyWidth, 1f), color("9B7A4A", 0.72f))
                drawFittedText(canvas, key, cursorX + 8f, y, keyWidth - 16f, TALENT_ASSIGN_BODY_STYLE, tone(TileTextTone.WHITE))
                drawFittedText(canvas, label, cursorX + keyWidth + 10f, y, labelWidth, TALENT_ASSIGN_BODY_STYLE, talentAssignBodyText())
                cursorX += keyWidth + labelWidth + 42f
                if (cursorX > x + width - 90f) {
                    return
                }
            }
        }

        private fun drawTalentAssignFooter(
            canvas: TileCanvas,
            renderModel: TileTalentAssignPanelRenderModel,
            x: Float,
            baseline: Float,
            width: Float,
        ) {
            drawTalentAssignFooterHelp(canvas, renderModel.panel.footerHints, x, baseline, width * 0.50f)
            var cursorX = x + width * 0.60f
            val maxX = x + width
            renderModel.panel.legend.items
                .filter { item -> item.kind == TalentLegendItemKind.STATE_TONE }
                .forEach { item ->
                    val marker = item.markerText.orEmpty()
                    val markerColor = item.toneToken?.let(::talentTone) ?: tone(TileTextTone.LIGHT_GRAY)
                    val markerWidth = TileTextMetrics.approximateTextWidth(marker, TALENT_ASSIGN_BODY_STYLE)
                    val labelWidth = TileTextMetrics.approximateTextWidth(item.label, TALENT_ASSIGN_BODY_STYLE).coerceAtMost(92f)
                    if (cursorX + markerWidth + labelWidth > maxX) {
                        return
                    }
                    drawFittedText(canvas, marker, cursorX, baseline, markerWidth + 2f, TALENT_ASSIGN_BODY_STYLE, markerColor)
                    cursorX += markerWidth + 8f
                    drawFittedText(canvas, item.label, cursorX, baseline, labelWidth, TALENT_ASSIGN_BODY_STYLE, markerColor)
                    cursorX += labelWidth + 44f
                }
        }

        private fun drawTalentAssignFooterHelp(
            canvas: TileCanvas,
            footerHints: List<TalentAssignFooterHintModel>,
            x: Float,
            baseline: Float,
            width: Float,
        ) {
            if (footerHints.isEmpty()) {
                return
            }
            var cursorX = x
            val maxX = x + width
            for (hint in footerHints) {
                val key = hint.keyText
                val label = hint.labelText
                val keyWidth = (TileTextMetrics.approximateTextWidth(key, TALENT_ASSIGN_BODY_STYLE) + 16f).coerceIn(38f, 76f)
                val labelWidth = TileTextMetrics.approximateTextWidth(label, TALENT_ASSIGN_BODY_STYLE).coerceAtMost(118f)
                val totalWidth = keyWidth + if (label.isBlank()) 0f else 9f + labelWidth
                if (cursorX + totalWidth > maxX) {
                    return
                }
                canvas.drawRect(tileBounds(cursorX, baseline - 18f, keyWidth, 26f), color("17100A", 0.88f))
                canvas.drawRect(tileBounds(cursorX, baseline + 7f, keyWidth, 1f), color("9B7A4A", 0.72f))
                drawFittedText(canvas, key, cursorX + 8f, baseline, keyWidth - 16f, TALENT_ASSIGN_BODY_STYLE, tone(TileTextTone.WHITE))
                if (label.isNotBlank()) {
                    drawFittedText(canvas, label, cursorX + keyWidth + 9f, baseline, labelWidth, TALENT_ASSIGN_BODY_STYLE, talentAssignBodyText())
                }
                cursorX += totalWidth + 42f
            }
        }

        private fun drawActiveSlotChoiceModal(
            canvas: TileCanvas,
            renderModel: TileTalentAssignPanelRenderModel,
            x: Float,
            y: Float,
            width: Float,
        ) {
            val modalModel = renderModel.panel.activeSlotChoiceModal ?: return
            val modalHeight = 190f
            canvas.drawRect(tileBounds(x, y, width, modalHeight), color("05070A", 0.94f))
            canvas.drawRect(tileBounds(x, y + modalHeight - 3f, width, 3f), UiDesignTokens.color.quality.rare.color())
            drawFittedText(canvas, modalModel.title, x + 12f, y + modalHeight - 20f, width - 24f, TileTextStyle.SMALL, UiDesignTokens.color.quality.rare.color())
            var baseline = y + modalHeight - 48f
            modalModel.items.forEach { item ->
                val focused = item.focused
                if (focused) {
                    canvas.drawRect(tileBounds(x + 8f, baseline - 16f, width - 16f, 22f), color("1CB7C8", 0.18f))
                }
                val tone =
                    when (item.kind) {
                        ActiveSlotChoiceModalItemKind.RESERVE_ACTION -> UiDesignTokens.color.quality.rare.color()
                        ActiveSlotChoiceModalItemKind.SLOT_REPLACE_TARGET -> UiDesignTokens.color.focus.ring.color()
                        ActiveSlotChoiceModalItemKind.SLOT_FILLED -> tone(TileTextTone.WHITE)
                        ActiveSlotChoiceModalItemKind.SLOT_EMPTY -> tone(TileTextTone.GRAY)
                    }
                drawFittedText(canvas, item.hotkeyText, x + 14f, baseline, 24f, TileTextStyle.SMALL, tone)
                renderModel.activeSlotChoiceItemIcons[item.renderKey()]?.let { icon ->
                    canvas.drawAsset(icon, tileBounds(x + 42f, baseline - 17f, 20f, 20f))
                }
                val text = "${item.primaryLabel}${item.secondaryLabel?.let { secondary -> "  $secondary" }.orEmpty()}"
                drawFittedText(canvas, text, x + 68f, baseline, width - 80f, TileTextStyle.SMALL, tone)
                baseline -= 23f
            }
            drawFittedText(canvas, modalModel.cancelHintText, x + 12f, y + 16f, width - 24f, TileTextStyle.SMALL, tone(TileTextTone.LIGHT_GRAY))
        }

        private fun drawFittedText(
            canvas: TileCanvas,
            text: String,
            x: Float,
            baseline: Float,
            maxWidth: Float,
            style: TileTextStyle,
            color: Color,
        ) {
            val fitted = TileTextMetrics.truncateTextToWidth(text, maxWidth.coerceAtLeast(1f), style)
            canvas.drawText(style, fitted, tilePosition(x, baseline), color)
        }

        private fun ActiveSlotChoiceModalItem.renderKey(): String = slot?.toString() ?: hotkeyText

        private fun targetCursorColor(state: TileTargetCursorState?): Color =
            when (state) {
                TileTargetCursorState.ILLEGAL -> UiDesignTokens.color.telegraph.high.color()
                TileTargetCursorState.LEGAL,
                null,
                -> UiDesignTokens.color.quality.rare.color()
            }

        private fun targetHighlightFill(state: TileTargetCursorState): Color =
            when (state) {
                TileTargetCursorState.ILLEGAL -> color("5F1616", 0.24f)
                TileTargetCursorState.LEGAL -> color("0C515A", 0.20f)
            }

        private fun targetHighlightBorder(state: TileTargetCursorState): Color =
            when (state) {
                TileTargetCursorState.ILLEGAL -> color("D4524D", 0.72f)
                TileTargetCursorState.LEGAL -> color("1CB7C8", 0.62f)
            }

        private fun drawChromeFrame(
            canvas: TileCanvas,
            assets: ChromeFrameAssets,
            bounds: GameShellBounds,
            fillColor: Color,
            borderColor: Color,
            alpha: Float = 0.86f,
        ) {
            ChromeFramePainter.drawFrame(
                sink = canvas.asChromeFrameSink(),
                request =
                    ChromeFrameDrawRequest(
                        bounds = bounds.toChromeFrameBounds(),
                        assets = assets,
                        fillColor = fillColor,
                        borderColor = borderColor,
                        alpha = alpha,
                    ),
            )
        }

        private fun drawContentScrim(
            canvas: TileCanvas,
            bounds: GameShellBounds,
            alpha: Float,
        ) {
            canvas.drawRect(tileBounds(bounds.x, bounds.y, bounds.width, bounds.height), color("1A0E04", alpha))
        }

        private fun drawThinRect(
            canvas: TileCanvas,
            bounds: GameShellBounds,
            color: Color,
            thickness: Float,
        ) {
            canvas.drawRect(tileBounds(bounds.x, bounds.y, bounds.width, thickness), color)
            canvas.drawRect(tileBounds(bounds.x, bounds.top - thickness, bounds.width, thickness), color)
            canvas.drawRect(tileBounds(bounds.x, bounds.y, thickness, bounds.height), color)
            canvas.drawRect(tileBounds(bounds.right - thickness, bounds.y, thickness, bounds.height), color)
        }

        private fun chromeContentBounds(
            bounds: GameShellBounds,
            kind: ChromeSurfaceKind,
        ): GameShellBounds =
            ChromeFramePainter.contentBounds(bounds.toChromeFrameBounds(), kind).toGameShellBounds()

        private fun gameBounds(
            x: Float,
            y: Float,
            width: Float,
            height: Float,
        ): GameShellBounds = GameShellBounds(x = x, y = y, width = width, height = height)

        private fun GameShellBounds.toChromeFrameBounds(): ChromeFrameBounds =
            ChromeFrameBounds(x = x, y = y, width = width, height = height)

        private fun ChromeFrameBounds.toGameShellBounds(): GameShellBounds =
            GameShellBounds(x = x, y = y, width = width, height = height)

        private fun ChromeFrameBounds.toTileBounds(): TileFloatBounds =
            tileBounds(x, y, width, height)

        private fun TileCanvas.asChromeFrameSink(): ChromeFrameDrawSink =
            object : ChromeFrameDrawSink {
                override fun drawRect(draw: ChromeFrameRectDraw) {
                    val bounds = draw.bounds
                    this@asChromeFrameSink.drawRect(tileBounds(bounds.x, bounds.y, bounds.width, bounds.height), draw.color)
                }

                override fun drawAsset(draw: ChromeFrameAssetDraw) {
                    val bounds = draw.bounds
                    this@asChromeFrameSink.drawAsset(draw.asset, tileBounds(bounds.x, bounds.y, bounds.width, bounds.height), draw.alpha)
                }
            }

        private fun drawRectOutline(
            canvas: TileCanvas,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            stroke: Float,
            color: Color,
        ) {
            canvas.drawRect(tileBounds(x, y, width, stroke), color)
            canvas.drawRect(tileBounds(x, y + height - stroke, width, stroke), color)
            canvas.drawRect(tileBounds(x, y, stroke, height), color)
            canvas.drawRect(tileBounds(x + width - stroke, y, stroke, height), color)
        }

        private fun drawPlayerIndicators(
            canvas: TileCanvas,
            indicators: List<TilePlayerIndicatorModel>,
            viewport: TileMapViewport,
        ) {
            indicators.forEach { indicator ->
                if (viewport.containsTile(indicator.tile)) {
                    val rect = viewport.tileRect(indicator.tile)
                    val x = rect.x.toFloat()
                    val y = rect.y.toFloat()
                    val width = rect.width.toFloat()
                    val height = rect.height.toFloat()
                    canvas.drawRect(tileBounds(x + 8f, y + 6f, 17f, 3f), color("050604", 0.315f))
                    drawRectOutline(
                        canvas = canvas,
                        x = x + 3f,
                        y = y + 3f,
                        width = rect.width - 6f,
                        height = rect.height - 6f,
                        stroke = 2f,
                        color = color("D99A2B", 0.94f),
                    )
                    val bracket = color("FFE18A", 0.615f)
                    canvas.drawRect(tileBounds(x + 4f, y + 4f, 8f, 2f), bracket)
                    canvas.drawRect(tileBounds(x + 4f, y + 4f, 2f, 8f), bracket)
                    canvas.drawRect(tileBounds(x + width - 12f, y + 4f, 8f, 2f), bracket)
                    canvas.drawRect(tileBounds(x + width - 6f, y + 4f, 2f, 8f), bracket)
                    canvas.drawRect(tileBounds(x + 4f, y + height - 6f, 8f, 2f), bracket)
                    canvas.drawRect(tileBounds(x + 4f, y + height - 12f, 2f, 8f), bracket)
                    canvas.drawRect(tileBounds(x + width - 12f, y + height - 6f, 8f, 2f), bracket)
                    canvas.drawRect(tileBounds(x + width - 6f, y + height - 12f, 2f, 8f), bracket)
                }
            }
        }

        private fun drawRoomArtPlatePlayerIndicators(
            canvas: TileCanvas,
            indicators: List<TilePlayerIndicatorModel>,
            viewport: TileMapViewport,
        ) {
            indicators.forEach { indicator ->
                if (!viewport.containsTile(indicator.tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(indicator.tile)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val width = rect.width.toFloat()
                val height = rect.height.toFloat()
                canvas.drawRect(tileBounds(x + 8f, y + 5f, width - 16f, 3f), color("050604", 0.22f))
                canvas.drawRect(tileBounds(x + 11f, y + 8f, width - 22f, 2f), color("D99A2B", 0.25f))
                drawArtPlateCornerMarks(
                    canvas = canvas,
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    mark = color("FFE18A", 0.58f),
                    inset = 5f,
                    length = 8f,
                    stroke = 1.5f,
                )
            }
        }

        private fun drawGroundLootMarkers(
            canvas: TileCanvas,
            markers: List<TileGroundLootMarkerModel>,
            viewport: TileMapViewport,
        ) {
            markers.forEach { marker ->
                val tile = Point(marker.x, marker.y)
                if (!viewport.containsTile(tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(tile)
                val cellLeft = rect.x.toFloat()
                val cellBottom = rect.y.toFloat()
                val actorCorner = marker.placement == com.ktome.client.ui.item.GroundLootMarkerPlacement.ACTOR_CORNER
                val iconSize = if (actorCorner) viewport.cellSize * 0.52f else viewport.cellSize * 0.72f
                val iconX =
                    if (actorCorner) {
                        cellLeft + viewport.cellSize - iconSize - 2f
                    } else {
                        cellLeft + (viewport.cellSize - iconSize) / 2f
                    }
                val iconY =
                    if (actorCorner) {
                        cellBottom + viewport.cellSize - iconSize - 2f
                    } else {
                        cellBottom + (viewport.cellSize - iconSize) / 2f
                    }
                canvas.drawRect(tileBounds(iconX - 2f, iconY - 2f, iconSize + 4f, iconSize + 4f), color("1A0E04", 0.72f))
                canvas.drawRect(tileBounds(iconX - 2f, iconY - 2f, iconSize + 4f, 3f), tone(marker.rarityTone))
                marker.specialAccentTokenId?.let { accent ->
                    canvas.drawRect(tileBounds(iconX - 2f, iconY + iconSize - 1f, iconSize + 4f, 3f), specialAccentColor(accent))
                }
                canvas.drawAsset(marker.icon, tileBounds(iconX, iconY, iconSize, iconSize))
                marker.cornerGlyph?.let { glyph ->
                    canvas.drawText(TileTextStyle.SMALL, glyph, tilePosition(iconX - 1f, iconY + iconSize + 13f), tone(marker.rarityTone))
                }
                marker.countBadge?.let { badge ->
                    val badgeWidth = if (badge == "9+") 27f else 20f
                    canvas.drawRect(tileBounds(iconX + iconSize - badgeWidth + 3f, iconY - 4f, badgeWidth, 18f), UiDesignTokens.color.surface.baseDim.color())
                    canvas.drawText(TileTextStyle.SMALL, badge, tilePosition(iconX + iconSize - badgeWidth + 6f, iconY + 11f), tone(TileTextTone.WHITE))
                }
            }
        }

        private fun drawRoomArtPlateGroundLootMarkers(
            canvas: TileCanvas,
            markers: List<TileGroundLootMarkerModel>,
            viewport: TileMapViewport,
        ) {
            markers.forEach { marker ->
                val tile = Point(marker.x, marker.y)
                if (!viewport.containsTile(tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(tile)
                val cellLeft = rect.x.toFloat()
                val cellBottom = rect.y.toFloat()
                val actorCorner = marker.placement == com.ktome.client.ui.item.GroundLootMarkerPlacement.ACTOR_CORNER
                val iconSize = if (actorCorner) viewport.cellSize * 0.46f else viewport.cellSize * 0.62f
                val iconX =
                    if (actorCorner) {
                        cellLeft + viewport.cellSize - iconSize - 3f
                    } else {
                        cellLeft + (viewport.cellSize - iconSize) / 2f
                    }
                val iconY =
                    if (actorCorner) {
                        cellBottom + viewport.cellSize - iconSize - 3f
                    } else {
                        cellBottom + (viewport.cellSize - iconSize) / 2f
                    }
                val railColor = withAlpha(tone(marker.rarityTone), 0.58f)
                val shelfAlpha = if (actorCorner) 0.18f else 0.24f
                canvas.drawRect(tileBounds(iconX + 2f, iconY - 3f, iconSize - 4f, 3f), color("050604", shelfAlpha))
                canvas.drawRect(tileBounds(iconX + 1f, iconY + iconSize + 1f, iconSize - 2f, 2f), railColor)
                marker.specialAccentTokenId?.let { accent ->
                    canvas.drawRect(tileBounds(iconX + iconSize - 6f, iconY + 2f, 2f, iconSize - 4f), withAlpha(specialAccentColor(accent), 0.52f))
                }
                canvas.drawAsset(marker.icon, tileBounds(iconX, iconY, iconSize, iconSize))
                marker.cornerGlyph?.let { glyph ->
                    canvas.drawText(TileTextStyle.SMALL, glyph, tilePosition(iconX - 1f, iconY + iconSize + 13f), railColor)
                }
                marker.countBadge?.let { badge ->
                    val badgeWidth = if (badge == "9+") 27f else 20f
                    canvas.drawRect(tileBounds(iconX + iconSize - badgeWidth + 4f, iconY - 4f, badgeWidth, 16f), color("050604", 0.64f))
                    canvas.drawText(TileTextStyle.SMALL, badge, tilePosition(iconX + iconSize - badgeWidth + 7f, iconY + 10f), tone(TileTextTone.WHITE))
                }
            }
        }

        private fun drawTargetHighlights(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val viewport = frame.viewport
            if (frame.model.roomPresentationPlan.compositorStrategy.usesRoomArtPlateInteractionGrammar) {
                drawArtPlateTargetHighlights(canvas, frame.layerPlan.targetHighlights, viewport)
                return
            }
            frame.layerPlan.targetHighlights.forEach { highlight ->
                if (!viewport.containsTile(highlight.tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(highlight.tile)
                drawLegacyTargetHighlight(canvas, rect, highlight.state)
            }
        }

        private fun drawLegacyTargetHighlight(
            canvas: TileCanvas,
            rect: RectInt,
            state: TileTargetCursorState,
        ) {
            val inset = 4f
            val bounds =
                tileBounds(
                    x = rect.x.toFloat() + inset,
                    y = rect.y.toFloat() + inset,
                    width = (rect.width - inset * 2f).coerceAtLeast(1f),
                    height = (rect.height - inset * 2f).coerceAtLeast(1f),
                )
            canvas.drawRect(bounds, targetHighlightFill(state))
            drawRectOutline(
                canvas = canvas,
                x = bounds.x,
                y = bounds.y,
                width = bounds.width,
                height = bounds.height,
                stroke = 1f,
                color = targetHighlightBorder(state),
            )
        }

        private fun drawArtPlateTargetHighlight(
            canvas: TileCanvas,
            rect: RectInt,
            state: TileTargetCursorState,
        ) {
            val x = rect.x.toFloat()
            val y = rect.y.toFloat()
            val width = rect.width.toFloat()
            val height = rect.height.toFloat()
            val veilInset = 7f
            canvas.drawRect(
                tileBounds(
                    x = x + veilInset,
                    y = y + veilInset,
                    width = (width - veilInset * 2f).coerceAtLeast(1f),
                    height = (height - veilInset * 2f).coerceAtLeast(1f),
                ),
                targetHighlightArtPlateVeil(state),
            )
            val mark = targetHighlightArtPlateMark(state)
            val length = (minOf(width, height) * 0.28f).coerceIn(7f, 10f)
            val stroke = 1.5f
            val inset = 4f
            val left = x + inset
            val right = x + width - inset
            val bottom = y + inset
            val top = y + height - inset
            canvas.drawRect(tileBounds(left, bottom, length, stroke), mark)
            canvas.drawRect(tileBounds(left, bottom, stroke, length), mark)
            canvas.drawRect(tileBounds(right - length, bottom, length, stroke), mark)
            canvas.drawRect(tileBounds(right - stroke, bottom, stroke, length), mark)
            canvas.drawRect(tileBounds(left, top - stroke, length, stroke), mark)
            canvas.drawRect(tileBounds(left, top - length, stroke, length), mark)
            canvas.drawRect(tileBounds(right - length, top - stroke, length, stroke), mark)
            canvas.drawRect(tileBounds(right - stroke, top - length, stroke, length), mark)
        }

        private fun drawArtPlateTargetHighlights(
            canvas: TileCanvas,
            highlights: List<TileTargetHighlightModel>,
            viewport: TileMapViewport,
        ) {
            val visibleHighlights = highlights.filter { highlight -> viewport.containsTile(highlight.tile) }
            val legalTiles =
                visibleHighlights
                    .asSequence()
                    .filter { highlight -> highlight.state == TileTargetCursorState.LEGAL }
                    .map { highlight -> highlight.tile }
                    .toSet()
            if (legalTiles.isNotEmpty()) {
                drawArtPlateLegalTargetBoundaryMarks(canvas, legalTiles, viewport)
            }
            visibleHighlights
                .filterNot { highlight -> highlight.state == TileTargetCursorState.LEGAL }
                .forEach { highlight ->
                    drawArtPlateTargetHighlight(canvas, viewport.tileRect(highlight.tile), highlight.state)
                }
        }

        private fun drawArtPlateLegalTargetBoundaryMarks(
            canvas: TileCanvas,
            legalTiles: Set<Point>,
            viewport: TileMapViewport,
        ) {
            val mark = withAlpha(targetHighlightArtPlateMark(TileTargetCursorState.LEGAL), 0.31f)
            legalTiles.forEach { tile ->
                val rect = viewport.tileRect(tile)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val width = rect.width.toFloat()
                val height = rect.height.toFloat()
                val centerX = x + width / 2f
                val centerY = y + height / 2f
                val edgeLength = (minOf(width, height) * 0.30f).coerceIn(8f, 11f)
                val stroke = 1.5f
                val inset = 4f
                if (Point(tile.x, tile.y - 1) !in legalTiles) {
                    canvas.drawRect(tileBounds(centerX - edgeLength / 2f, y + inset, edgeLength, stroke), mark)
                }
                if (Point(tile.x, tile.y + 1) !in legalTiles) {
                    canvas.drawRect(tileBounds(centerX - edgeLength / 2f, y + height - inset - stroke, edgeLength, stroke), mark)
                }
                if (Point(tile.x - 1, tile.y) !in legalTiles) {
                    canvas.drawRect(tileBounds(x + inset, centerY - edgeLength / 2f, stroke, edgeLength), mark)
                }
                if (Point(tile.x + 1, tile.y) !in legalTiles) {
                    canvas.drawRect(tileBounds(x + width - inset - stroke, centerY - edgeLength / 2f, stroke, edgeLength), mark)
                }
            }
        }

        private fun targetHighlightArtPlateVeil(state: TileTargetCursorState): Color =
            when (state) {
                TileTargetCursorState.ILLEGAL -> color("5F1616", 0.10f)
                TileTargetCursorState.LEGAL -> color("0C515A", 0.060f)
            }

        private fun targetHighlightArtPlateMark(state: TileTargetCursorState): Color =
            when (state) {
                TileTargetCursorState.ILLEGAL -> color("D4524D", 0.46f)
                TileTargetCursorState.LEGAL -> color("1CB7C8", 0.34f)
            }

        private fun drawCombatFeedback(
            canvas: TileCanvas,
            combatFeedback: List<TileCombatFeedbackModel>,
            viewport: TileMapViewport,
        ) {
            combatFeedback.forEach { feedback ->
                val tile = Point(feedback.x, feedback.y)
                if (!viewport.containsTile(tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(tile)
                val worldX = rect.x + 4f + feedback.horizontalOffsetCells * (viewport.cellSize + 6f)
                val worldY = rect.y + viewport.cellSize - 2f + feedback.stackIndex * 15f
                val backgroundWidth = (feedback.text.length * 11f).coerceAtLeast(28f)
                canvas.drawRect(tileBounds(worldX - 2f, worldY - 16f, backgroundWidth, 18f), UiDesignTokens.color.surface.baseDim.color())
                canvas.drawText(
                    TileTextStyle.SMALL,
                    feedback.text,
                    tilePosition(worldX, worldY - 2f),
                    tone(feedback.tone),
                )
            }
        }

        private fun drawCursor(
            canvas: TileCanvas,
            tile: Point,
            viewport: TileMapViewport,
            color: Color,
        ) {
            if (!viewport.containsTile(tile)) {
                return
            }
            val rect = viewport.tileRect(tile)
            drawRectOutline(canvas, rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat(), 2f, color)
        }

        private fun drawRoomArtPlateCursor(
            canvas: TileCanvas,
            tile: Point,
            viewport: TileMapViewport,
            color: Color,
        ) {
            if (!viewport.containsTile(tile)) {
                return
            }
            val rect = viewport.tileRect(tile)
            val x = rect.x.toFloat()
            val y = rect.y.toFloat()
            val width = rect.width.toFloat()
            val height = rect.height.toFloat()
            val mark = withAlpha(color, 0.62f)
            drawArtPlateCornerMarks(
                canvas = canvas,
                x = x,
                y = y,
                width = width,
                height = height,
                mark = mark,
                inset = 4f,
                length = 9f,
                stroke = 1.5f,
            )
            canvas.drawRect(tileBounds(x + width * 0.34f, y + 4f, width * 0.32f, 2f), withAlpha(color, 0.35f))
        }

        private fun drawArtPlateCornerMarks(
            canvas: TileCanvas,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            mark: Color,
            inset: Float,
            length: Float,
            stroke: Float,
        ) {
            val left = x + inset
            val right = x + width - inset
            val bottom = y + inset
            val top = y + height - inset
            canvas.drawRect(tileBounds(left, bottom, length, stroke), mark)
            canvas.drawRect(tileBounds(left, bottom, stroke, length), mark)
            canvas.drawRect(tileBounds(right - length, bottom, length, stroke), mark)
            canvas.drawRect(tileBounds(right - stroke, bottom, stroke, length), mark)
            canvas.drawRect(tileBounds(left, top - stroke, length, stroke), mark)
            canvas.drawRect(tileBounds(left, top - length, stroke, length), mark)
            canvas.drawRect(tileBounds(right - length, top - stroke, length, stroke), mark)
            canvas.drawRect(tileBounds(right - stroke, top - length, stroke, length), mark)
        }

        private fun drawPlacement(
            canvas: TileCanvas,
            placement: TileVisualPlacement,
            viewport: TileMapViewport,
            alphaScale: Float = 1f,
        ) {
            val tile = Point(placement.x, placement.y)
            if (!viewport.containsTile(tile)) {
                return
            }
            val rect = viewport.tileRect(tile)
            val footprint = footprintDimensions(placement.asset.entry.footprint)
            val width = viewport.cellSize * footprint.first
            val height = viewport.cellSize * footprint.second
            val anchorX = rect.x + viewport.cellSize * placement.asset.entry.pivotX.toFloat()
            val anchorY = rect.y + viewport.cellSize * placement.asset.entry.pivotY.toFloat()
            val drawX = anchorX - width * placement.asset.entry.pivotX.toFloat()
            val drawY = anchorY - height * placement.asset.entry.pivotY.toFloat()
            val terrainCategory = placement.asset.entry.category
            val isTerrain = terrainCategory.startsWith("tile_")
            val terrainBleed =
                when {
                    terrainCategory == "tile_ground" -> 0f
                    isTerrain -> 1.2f
                    else -> 0f
                }
            val alpha =
                when {
                    terrainCategory == "tile_ground" -> placement.alpha
                    isTerrain -> placement.alpha * 0.96f
                    else -> placement.alpha
                }
            canvas.drawAsset(
                placement.asset,
                tileBounds(drawX - terrainBleed, drawY - terrainBleed, width + terrainBleed * 2f, height + terrainBleed * 2f),
                alpha * alphaScale,
                placement.tintColorHex,
                placement.flipX,
                placement.flipY,
            )
        }

        private fun drawRoomArtPlateSpriteOverlay(
            canvas: TileCanvas,
            placement: TileVisualPlacement,
            viewport: TileMapViewport,
        ) {
            val tile = Point(placement.x, placement.y)
            if (!viewport.containsTile(tile)) {
                return
            }
            val rect = viewport.tileRect(tile)
            val alphaScale =
                when {
                    placement.drawPriority >= 3 -> 0.52f
                    placement.drawPriority == 2 -> 0.44f
                    else -> 0.34f
                }
            drawPlacement(canvas, placement, viewport, alphaScale = alphaScale)
            drawRoomArtPlateOverlayMarkers(canvas, rect, placement.drawPriority)
        }

        private fun drawRoomArtPlateOverlayMarkers(
            canvas: TileCanvas,
            rect: RectInt,
            dangerLevel: Int,
        ) {
            val x = rect.x.toFloat()
            val y = rect.y.toFloat()
            val size = minOf(rect.width, rect.height).toFloat()
            val inset = 5f
            val stroke = if (dangerLevel >= 3) 2f else 1.5f
            val longMark = (size * 0.36f).coerceIn(9f, 13f)
            val shortMark = (size * 0.22f).coerceIn(6f, 9f)
            val mark = roomArtPlateOverlayMarkColor(dangerLevel)
            val veil = roomArtPlateOverlayVeilColor(dangerLevel)

            canvas.drawRect(
                tileBounds(x + inset + 3f, y + inset + 3f, size - inset * 2f - 6f, size - inset * 2f - 6f),
                veil,
            )
            canvas.drawRect(tileBounds(x + inset, y + inset, longMark, stroke), mark)
            canvas.drawRect(tileBounds(x + inset, y + inset, stroke, shortMark), mark)
            canvas.drawRect(tileBounds(x + size - inset - longMark, y + inset, longMark, stroke), mark)
            canvas.drawRect(tileBounds(x + size - inset - stroke, y + inset, stroke, shortMark), mark)
            canvas.drawRect(tileBounds(x + inset, y + size - inset - stroke, longMark, stroke), mark)
            canvas.drawRect(tileBounds(x + inset, y + size - inset - shortMark, stroke, shortMark), mark)
            canvas.drawRect(tileBounds(x + size - inset - longMark, y + size - inset - stroke, longMark, stroke), mark)
            canvas.drawRect(tileBounds(x + size - inset - stroke, y + size - inset - shortMark, stroke, shortMark), mark)

            if (dangerLevel >= 3) {
                canvas.drawRect(tileBounds(x + size * 0.28f, y + size * 0.50f, size * 0.44f, 1.5f), mark)
                canvas.drawRect(tileBounds(x + size * 0.50f, y + size * 0.28f, 1.5f, size * 0.44f), mark)
            }
        }

        private fun roomArtPlateOverlayMarkColor(dangerLevel: Int): Color =
            when {
                dangerLevel >= 3 -> color("D66A4D", 0.58f)
                dangerLevel == 2 -> color("D9A24F", 0.46f)
                else -> color("50B2A8", 0.34f)
            }

        private fun roomArtPlateOverlayVeilColor(dangerLevel: Int): Color =
            when {
                dangerLevel >= 3 -> color("7A1D15", 0.072f)
                dangerLevel == 2 -> color("6A4514", 0.055f)
                else -> color("0E3B39", 0.040f)
            }

        private fun drawActorGroundingShadow(
            canvas: TileCanvas,
            placement: TileVisualPlacement,
            viewport: TileMapViewport,
        ) {
            val tile = Point(placement.x, placement.y)
            if (!viewport.containsTile(tile)) {
                return
            }
            val rect = viewport.tileRect(tile)
            val footprint = footprintDimensions(placement.asset.entry.footprint)
            val width = viewport.cellSize * footprint.first
            val height = viewport.cellSize * footprint.second
            val pivotX = placement.asset.entry.pivotX.toFloat()
            val pivotY = placement.asset.entry.pivotY.toFloat()
            val anchorX = rect.x + viewport.cellSize * pivotX
            val anchorY = rect.y + viewport.cellSize * pivotY
            val drawX = anchorX - width * pivotX
            val drawY = anchorY - height * pivotY
            val centerX = drawX + width / 2f
            val footY = drawY + height * 0.80f
            val shadowWidth = (width * 0.70f).coerceIn(viewport.cellSize * 0.58f, viewport.cellSize * 1.55f)
            val shadowHeight = (viewport.cellSize * 0.18f).coerceAtLeast(5f)
            canvas.drawRect(
                tileBounds(centerX - shadowWidth / 2f, footY - shadowHeight / 2f, shadowWidth, shadowHeight),
                color("050604", 0.18f),
            )
            canvas.drawRect(
                tileBounds(centerX - shadowWidth * 0.36f, footY - 1f, shadowWidth * 0.72f, 2f),
                color("8A6A35", 0.055f),
            )
        }

        private fun drawPropAtmosphere(
            canvas: TileCanvas,
            placements: List<TileVisualPlacement>,
            viewport: TileMapViewport,
        ) {
            placements.forEach { placement ->
                val tile = Point(placement.x, placement.y)
                if (!viewport.containsTile(tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(tile)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val size = rect.width.toFloat()
                val key = placement.asset.resolvedKey
                if (key == "prop.alarm_bonfire") {
                    drawTileGlow(canvas, tile, viewport, radius = 5, maxAlpha = 0.28f)
                    canvas.drawRect(tileBounds(x + 6f, y + 3f, size - 12f, 5f), color("050604", 0.34f))
                    canvas.drawRect(tileBounds(x + 10f, y + 8f, size - 20f, 12f), color("E28A2B", 0.18f))
                } else if (key == "prop.supply_crate") {
                    canvas.drawRect(tileBounds(x + 3f, y + 3f, size - 6f, 7f), color("050604", 0.28f))
                    canvas.drawRect(tileBounds(x + 5f, y + 7f, size - 10f, size - 14f), color("D99A2B", 0.10f))
                    canvas.drawRect(tileBounds(x + 8f, y + size - 10f, size - 16f, 2f), color("FFE18A", 0.11f))
                } else if (key == "prop.ritual_altar" || key == "prop.mine_furnace") {
                    drawTileGlow(canvas, tile, viewport, radius = 4, maxAlpha = 0.16f)
                    canvas.drawRect(tileBounds(x + 3f, y + 2f, size - 6f, 6f), color("050604", 0.34f))
                    canvas.drawRect(tileBounds(x + 6f, y + 7f, size - 12f, size - 14f), color("6E1310", 0.13f))
                    canvas.drawRect(tileBounds(x + 10f, y + size - 10f, size - 20f, 2f), color("D99A2B", 0.12f))
                }
            }
        }

        private fun drawGroundLootAtmosphere(
            canvas: TileCanvas,
            markers: List<TileGroundLootMarkerModel>,
            viewport: TileMapViewport,
        ) {
            markers.forEach { marker ->
                val tile = Point(marker.x, marker.y)
                if (!viewport.containsTile(tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(tile)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val size = rect.width.toFloat()
                canvas.drawRect(tileBounds(x + 5f, y + 6f, size - 10f, size - 12f), color("D99A2B", 0.10f))
                canvas.drawRect(tileBounds(x + 8f, y + 5f, size - 16f, 3f), color("050604", 0.30f))
            }
        }

        private fun drawRoomArtPlateGroundLootAtmosphere(
            canvas: TileCanvas,
            markers: List<TileGroundLootMarkerModel>,
            viewport: TileMapViewport,
        ) {
            markers.forEach { marker ->
                val tile = Point(marker.x, marker.y)
                if (!viewport.containsTile(tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(tile)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val size = rect.width.toFloat()
                val actorCorner = marker.placement == com.ktome.client.ui.item.GroundLootMarkerPlacement.ACTOR_CORNER
                if (actorCorner) {
                    canvas.drawRect(tileBounds(x + size - 17f, y + size - 9f, 10f, 2f), color("050604", 0.16f))
                } else {
                    canvas.drawRect(tileBounds(x + 10f, y + 8f, size - 20f, 3f), color("050604", 0.18f))
                    canvas.drawRect(tileBounds(x + 13f, y + 12f, size - 26f, 5f), color("D99A2B", 0.045f))
                }
            }
        }

        private fun drawRoomArtPlateTargetTopologyHints(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val targetPoints =
                frame.layerPlan.targetHighlights
                    .asSequence()
                    .filter { highlight ->
                        highlight.state == TileTargetCursorState.LEGAL &&
                            frame.viewport.containsTile(highlight.tile)
                    }.map { highlight -> highlight.tile }
                    .toSet()
            if (targetPoints.isEmpty()) {
                return
            }
            val cellSize = frame.viewport.cellSize.toFloat()
            val connectorColor = color("A89460", 0.074f)
            targetPoints.forEach { tile ->
                val rect = frame.viewport.tileRect(tile)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                if (Point(tile.x + 1, tile.y) in targetPoints) {
                    canvas.drawRect(tileBounds(x + cellSize - 2f, y + cellSize / 2f - 1f, 4f, 2f), connectorColor)
                }
                if (Point(tile.x, tile.y + 1) in targetPoints) {
                    canvas.drawRect(tileBounds(x + cellSize / 2f - 1f, y + cellSize - 2f, 2f, 4f), connectorColor)
                }
            }
        }

        private fun drawRoomArtPlateEdgeFeather(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            roomClip: RectInt,
        ) {
            val depth = (frame.viewport.cellSize.toFloat() * 0.16f).coerceIn(5f, 14f)
            val left = roomClip.x.toFloat()
            val right = roomClip.right.toFloat()
            val bottom = roomClip.y.toFloat()
            val top = roomClip.top.toFloat()
            val width = roomClip.width.toFloat()
            val height = roomClip.height.toFloat()
            val edge = color("050604", 0.082f)
            canvas.drawRect(tileBounds(left, bottom, width, depth), edge)
            canvas.drawRect(tileBounds(left, top - depth, width, depth), edge)
            canvas.drawRect(tileBounds(left, bottom, depth, height), edge)
            canvas.drawRect(tileBounds(right - depth, bottom, depth, height), edge)
        }

        private fun drawCellMaterial(
            canvas: TileCanvas,
            material: TileMapCellMaterialModel,
            viewport: TileMapViewport,
        ) {
            val tile = Point(material.x, material.y)
            if (!viewport.containsTile(tile)) {
                return
            }
            val rect = viewport.tileRect(tile)
            val alphaScale = if (material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.EXPLORED) 0.62f else 1f
            val x = rect.x.toFloat()
            val y = rect.y.toFloat()
            val size = viewport.cellSize.toFloat()
            when (material.kind) {
                TileMapCellMaterialKind.WALL -> drawWallMaterial(canvas, material, x, y, size, alphaScale)
                TileMapCellMaterialKind.FLOOR -> drawFloorMaterial(canvas, material, x, y, size, alphaScale)
                TileMapCellMaterialKind.HAZARD -> drawHazardMaterial(canvas, material, x, y, size, alphaScale)
            }
        }

        private fun drawFloorMaterial(
            canvas: TileCanvas,
            material: TileMapCellMaterialModel,
            x: Float,
            y: Float,
            size: Float,
            alphaScale: Float,
        ) {
            canvas.drawRect(tileBounds(x - 1.5f, y - 1.5f, size + 3f, size + 3f), color("2C3A33", 0.012f * alphaScale))
            canvas.drawRect(tileBounds(x + 3f, y + 3f, size - 6f, size - 6f), color("3D493F", 0.006f * alphaScale))
            if (material.northOcclusion) {
                canvas.drawRect(tileBounds(x, y + size - 7f, size, 7f), color("050604", 0.080f * alphaScale))
            }
            if (material.southOcclusion) {
                canvas.drawRect(tileBounds(x, y, size, 6f), color("050604", 0.070f * alphaScale))
            }
            if (material.westOcclusion) {
                canvas.drawRect(tileBounds(x, y, 6f, size), color("050604", 0.052f * alphaScale))
            }
            if (material.eastOcclusion) {
                canvas.drawRect(tileBounds(x + size - 6f, y, 6f, size), color("050604", 0.052f * alphaScale))
            }
        }

        private fun drawWallMaterial(
            canvas: TileCanvas,
            material: TileMapCellMaterialModel,
            x: Float,
            y: Float,
            size: Float,
            alphaScale: Float,
        ) {
            val variant = material.variant
            canvas.drawRect(tileBounds(x + 1f, y + 1f, size - 2f, size - 2f), color("050604", 0.010f * alphaScale))
            when (variant % 4) {
                0 -> canvas.drawRect(tileBounds(x + 2f, y + 2f, size - 4f, size - 4f), color("050604", 0.006f * alphaScale))
                1 -> canvas.drawRect(tileBounds(x + 2f, y + 2f, size - 4f, size - 4f), color("8A6A35", 0.006f * alphaScale))
                2 -> canvas.drawRect(tileBounds(x + 3f, y + 3f, size - 6f, size - 6f), color("1C2B24", 0.006f * alphaScale))
            }
            canvas.drawRect(tileBounds(x + 2f, y + size - 6f, size - 4f, 4f), color("C49B61", 0.035f * alphaScale))
            canvas.drawRect(tileBounds(x + 2f, y + 2f, size - 4f, 5f), color("050604", 0.055f * alphaScale))
            canvas.drawRect(tileBounds(x + 3f, y + size - 10f, size - 6f, 2f), color("FFE18A", 0.014f * alphaScale))
            canvas.drawRect(tileBounds(x + 2f, y + size * 0.48f, size - 4f, 1f), color("6D4520", 0.025f * alphaScale))
            if (variant % 2 == 0) {
                val seamX = x + 8f + (variant % 11).toFloat()
                canvas.drawRect(tileBounds(seamX, y + 5f, 1f, size - 10f), color("050604", 0.030f * alphaScale))
            } else {
                val chipX = x + 5f + (variant % 13).toFloat()
                val chipY = y + 8f + ((variant / 13) % 11).toFloat()
                canvas.drawRect(tileBounds(chipX, chipY, 6f, 2f), color("B8873E", 0.065f * alphaScale))
            }
            if (variant % 17 == 0) {
                val emberX = x + size * 0.42f
                val emberY = y + size * 0.48f
                canvas.drawRect(tileBounds(emberX - 9f, emberY - 7f, 22f, 18f), color("C66A21", 0.090f * alphaScale))
                canvas.drawRect(tileBounds(emberX - 2f, emberY - 1f, 5f, 8f), color("F3B34A", 0.22f * alphaScale))
                canvas.drawRect(tileBounds(emberX - 1f, emberY + 1f, 3f, 4f), color("FFE18A", 0.14f * alphaScale))
            }
        }

        private fun drawHazardMaterial(
            canvas: TileCanvas,
            material: TileMapCellMaterialModel,
            x: Float,
            y: Float,
            size: Float,
            alphaScale: Float,
        ) {
            val pulseX = x + 4f + (material.variant % 12)
            canvas.drawRect(tileBounds(x + 1f, y + 1f, size - 2f, size - 2f), color("1CB7C8", 0.06f * alphaScale))
            canvas.drawRect(tileBounds(pulseX, y + 6f, 8f, size - 12f), color("1CB7C8", 0.12f * alphaScale))
        }

        private fun drawFogOverlays(
            canvas: TileCanvas,
            fogTiles: List<TileFogPlacement>,
            viewport: TileMapViewport,
        ) {
            fogTiles.forEach { fog ->
                val tile = Point(fog.x, fog.y)
                if (!viewport.containsTile(tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(tile)
                canvas.drawRect(tileBounds(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat()), fogOverlayColor(fog))
            }
        }

        private fun drawRoomArtPlateFogOverlays(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val visibleFog =
                frame.layerPlan.fogVeils.filter { fog ->
                    frame.viewport.containsTile(Point(fog.x, fog.y))
                }
            val (exploredFog, hardOcclusionFog) =
                visibleFog.partition { fog -> fog.visibility == CellVisibilitySnapshot.EXPLORED }
            exploredFog.toRoomArtPlateFogComponents().forEach { component ->
                drawRoomArtPlateExploredFogComponent(canvas, frame, component)
            }
            val fogRuns =
                hardOcclusionFog.toRoomArtPlateFogRuns()
                    .mergeVerticalRoomArtPlateFogRuns()
            if (fogRuns.isEmpty()) {
                return
            }
            fogRuns.forEach { run ->
                drawRoomArtPlateFogRun(canvas, frame, run)
            }
        }

        private fun drawRoomArtPlateExploredFogComponent(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            component: List<TileFogPlacement>,
        ) {
            if (component.isEmpty()) {
                return
            }
            val rects = component.map { fog -> frame.viewport.tileRect(Point(fog.x, fog.y)) }
            val left = rects.minOf { rect -> rect.x }.toFloat()
            val right = rects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = rects.minOf { rect -> rect.y }.toFloat()
            val top = rects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val cellSize = frame.viewport.cellSize.toFloat()
            val horizontalBleed = cellSize * 0.24f
            val verticalBleed = cellSize * 0.22f
            drawClippedRect(
                canvas = canvas,
                x = left - horizontalBleed,
                y = bottom - verticalBleed,
                width = right - left + horizontalBleed * 2f,
                height = top - bottom + verticalBleed * 2f,
                clip = frame.viewport.mapBounds,
                color = roomArtPlateExploredFogOverlayColor(component.first().alpha),
            )
            drawClippedRect(
                canvas,
                left - horizontalBleed * 0.24f,
                top - cellSize * 0.14f,
                right - left + horizontalBleed * 0.48f,
                cellSize * 0.09f,
                frame.viewport.mapBounds,
                color("6F5A39", 0.026f),
            )
            drawClippedRect(
                canvas,
                left + cellSize * 0.18f,
                bottom - verticalBleed * 0.30f,
                (right - left - cellSize * 0.36f).coerceAtLeast(cellSize * 0.50f),
                cellSize * 0.07f,
                frame.viewport.mapBounds,
                color("020303", 0.044f),
            )
        }

        private fun drawRoomArtPlateFogRun(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            run: RoomArtPlateFogRun,
        ) {
            val first = frame.viewport.tileRect(Point(run.startX, run.startY))
            val last = frame.viewport.tileRect(Point(run.endX, run.endY))
            val left = minOf(first.x, last.x).toFloat()
            val right = maxOf(first.x + first.width, last.x + last.width).toFloat()
            val bottom = minOf(first.y, last.y).toFloat()
            val top = maxOf(first.y + first.height, last.y + last.height).toFloat()
            val cellSize = frame.viewport.cellSize.toFloat()
            val horizontalBleed =
                when (run.visibility) {
                    CellVisibilitySnapshot.HIDDEN -> cellSize * 0.10f
                    CellVisibilitySnapshot.EXPLORED -> cellSize * 0.20f
                    CellVisibilitySnapshot.VISIBLE -> cellSize * 0.14f
                }
            val verticalBleed =
                when (run.visibility) {
                    CellVisibilitySnapshot.HIDDEN -> cellSize * 0.08f
                    CellVisibilitySnapshot.EXPLORED -> cellSize * 0.18f
                    CellVisibilitySnapshot.VISIBLE -> cellSize * 0.12f
                }
            drawClippedRect(
                canvas = canvas,
                x = left - horizontalBleed,
                y = bottom - verticalBleed,
                width = right - left + horizontalBleed * 2f,
                height = top - bottom + verticalBleed * 2f,
                clip = frame.viewport.mapBounds,
                color = roomArtPlateFogOverlayColor(run),
            )
            if (run.visibility == CellVisibilitySnapshot.EXPLORED) {
                drawClippedRect(
                    canvas,
                    left - horizontalBleed * 0.38f,
                    top - cellSize * 0.13f,
                    right - left + horizontalBleed * 0.76f,
                    cellSize * 0.10f,
                    frame.viewport.mapBounds,
                    color("6F5A39", 0.030f),
                )
                drawClippedRect(
                    canvas,
                    left + cellSize * 0.18f,
                    bottom - verticalBleed * 0.36f,
                    (right - left - cellSize * 0.36f).coerceAtLeast(cellSize * 0.50f),
                    cellSize * 0.08f,
                    frame.viewport.mapBounds,
                    color("020303", 0.052f),
                )
            }
        }

        private data class RoomArtPlateFogRun(
            val visibility: CellVisibilitySnapshot,
            val alpha: Float,
            val startX: Int,
            val endX: Int,
            val startY: Int,
            val endY: Int,
        )

        private fun List<TileFogPlacement>.toRoomArtPlateFogComponents(): List<List<TileFogPlacement>> {
            val fogByPoint = associateBy { fog -> Point(fog.x, fog.y) }
            val visited = mutableSetOf<Point>()
            val components = mutableListOf<List<TileFogPlacement>>()
            sortedWith(compareBy<TileFogPlacement> { fog -> fog.y }.thenBy { fog -> fog.x }).forEach { fog ->
                val start = Point(fog.x, fog.y)
                if (!visited.add(start)) {
                    return@forEach
                }
                val queue = ArrayDeque<Point>()
                val component = mutableListOf<TileFogPlacement>()
                queue.add(start)
                while (queue.isNotEmpty()) {
                    val point = queue.removeFirst()
                    val current = fogByPoint[point] ?: continue
                    component += current
                    listOf(
                        Point(point.x - 1, point.y),
                        Point(point.x + 1, point.y),
                        Point(point.x, point.y - 1),
                        Point(point.x, point.y + 1),
                    ).forEach { neighbor ->
                        if (neighbor in fogByPoint && visited.add(neighbor)) {
                            queue.add(neighbor)
                        }
                    }
                }
                components += component
            }
            return components
        }

        private fun List<TileFogPlacement>.toRoomArtPlateFogRuns(): List<RoomArtPlateFogRun> =
            groupBy { fog -> Triple(fog.visibility, fog.alpha, fog.y) }
                .flatMap { (key, rowFogTiles) ->
                    rowFogTiles
                        .sortedBy(TileFogPlacement::x)
                        .fold(mutableListOf<RoomArtPlateFogRun>()) { runs, fog ->
                            val current = runs.lastOrNull()
                            if (current != null && current.endX + 1 == fog.x) {
                                runs[runs.lastIndex] = current.copy(endX = fog.x)
                            } else {
                                runs +=
                                    RoomArtPlateFogRun(
                                        visibility = key.first,
                                        alpha = key.second,
                                        startX = fog.x,
                                        endX = fog.x,
                                        startY = key.third,
                                        endY = key.third,
                                    )
                            }
                            runs
                        }
                }

        private fun List<RoomArtPlateFogRun>.mergeVerticalRoomArtPlateFogRuns(): List<RoomArtPlateFogRun> =
            sortedWith(
                compareBy<RoomArtPlateFogRun> { run -> run.visibility.name }
                    .thenBy { run -> run.alpha }
                    .thenBy { run -> run.startX }
                    .thenBy { run -> run.endX }
                    .thenBy { run -> run.startY },
            ).fold(mutableListOf<RoomArtPlateFogRun>()) { merged, run ->
                val current = merged.lastOrNull()
                if (
                    current != null &&
                    current.visibility == run.visibility &&
                    current.alpha == run.alpha &&
                    current.startX == run.startX &&
                    current.endX == run.endX &&
                    current.endY + 1 == run.startY
                ) {
                    merged[merged.lastIndex] = current.copy(endY = run.endY)
                } else {
                    merged += run
                }
                merged
            }

        private fun roomArtPlateFogOverlayColor(run: RoomArtPlateFogRun): Color =
            when (run.visibility) {
                CellVisibilitySnapshot.HIDDEN -> color("050604", (run.alpha + 0.54f).coerceAtMost(0.92f))
                CellVisibilitySnapshot.EXPLORED -> roomArtPlateExploredFogOverlayColor(run.alpha)
                CellVisibilitySnapshot.VISIBLE -> color("1A0E04", run.alpha * 0.22f)
            }

        private fun roomArtPlateExploredFogOverlayColor(alpha: Float): Color =
            color("090706", (alpha * 0.78f).coerceIn(0.24f, 0.38f))

        private fun fogOverlayColor(fog: TileFogPlacement): Color =
            when (fog.visibility) {
                com.ktome.core.snapshot.CellVisibilitySnapshot.HIDDEN -> color("050604", (fog.alpha + 0.64f).coerceAtMost(0.99f))
                com.ktome.core.snapshot.CellVisibilitySnapshot.EXPLORED -> color("090706", (fog.alpha + 0.28f).coerceAtMost(0.82f))
                com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE -> color("1A0E04", fog.alpha * 0.30f)
            }

        private fun drawRoomArtPlateApertureShoulders(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val visiblePoints = visibleMaterialPoints(frame)
            if (visiblePoints.isEmpty()) {
                return
            }
            val viewport = frame.viewport
            val cellSize = viewport.cellSize.toFloat()
            val shoulderDepth = (cellSize * 0.40f).coerceIn(8f, 14f)
            val lipDepth = 2f
            val spanInset = (cellSize * 0.20f).coerceIn(5f, 8f)
            frame.layerPlan.fogVeils.forEach { fog ->
                val tile = Point(fog.x, fog.y)
                if (!viewport.containsTile(tile)) {
                    return@forEach
                }
                val alpha =
                    when (fog.visibility) {
                        CellVisibilitySnapshot.HIDDEN -> 0.074f
                        CellVisibilitySnapshot.EXPLORED -> 0.052f
                        CellVisibilitySnapshot.VISIBLE -> return@forEach
                    }
                val rect = viewport.tileRect(tile)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val size = rect.width.toFloat()
                val shoulder = color("111711", alpha)
                val lip = color("6F5A39", alpha * 0.18f)
                if (Point(tile.x - 1, tile.y) in visiblePoints) {
                    canvas.drawRect(tileBounds(x, y + spanInset, shoulderDepth, size - spanInset * 2f), shoulder)
                    canvas.drawRect(tileBounds(x + shoulderDepth - lipDepth, y + spanInset + 3f, lipDepth, size - spanInset * 2f - 6f), lip)
                }
                if (Point(tile.x + 1, tile.y) in visiblePoints) {
                    canvas.drawRect(tileBounds(x + size - shoulderDepth, y + spanInset, shoulderDepth, size - spanInset * 2f), shoulder)
                    canvas.drawRect(tileBounds(x + size - shoulderDepth, y + spanInset + 3f, lipDepth, size - spanInset * 2f - 6f), lip)
                }
                if (Point(tile.x, tile.y - 1) in visiblePoints) {
                    canvas.drawRect(tileBounds(x + spanInset, y, size - spanInset * 2f, shoulderDepth), shoulder)
                    canvas.drawRect(tileBounds(x + spanInset + 3f, y + shoulderDepth - lipDepth, size - spanInset * 2f - 6f, lipDepth), lip)
                }
                if (Point(tile.x, tile.y + 1) in visiblePoints) {
                    canvas.drawRect(tileBounds(x + spanInset, y + size - shoulderDepth, size - spanInset * 2f, shoulderDepth), shoulder)
                    canvas.drawRect(tileBounds(x + spanInset + 3f, y + size - shoulderDepth, size - spanInset * 2f - 6f, lipDepth), lip)
                }
            }
        }

        private fun drawVisibleRoomFoundationGlaze(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val visibleMaterials =
                frame.model.mapCellMaterials
                    .filter { material ->
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            frame.viewport.containsTile(Point(material.x, material.y))
                    }
            if (visibleMaterials.isEmpty()) {
                return
            }
            val rects = visibleMaterials.map { material -> frame.viewport.tileRect(Point(material.x, material.y)) }
            val left = rects.minOf { rect -> rect.x }.toFloat()
            val right = rects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = rects.minOf { rect -> rect.y }.toFloat()
            val top = rects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val width = right - left
            val height = top - bottom
            val cellSize = frame.viewport.cellSize.toFloat()
            if (width < cellSize * 4f || height < cellSize * 4f) {
                return
            }
            canvas.drawRect(tileBounds(left, bottom, width, height), color("26362F", 0.040f))
            canvas.drawRect(
                tileBounds(left + cellSize * 1.15f, bottom + cellSize * 1.05f, width - cellSize * 2.30f, height - cellSize * 2.10f),
                color("8A7654", 0.007f),
            )
            val rows = (height / cellSize).toInt()
            val columns = (width / cellSize).toInt()
            val availableRows = (rows - 2).coerceAtLeast(1)
            (2 until columns step 3).forEach { column ->
                val segmentCount = if (column % 5 == 0) 2 else 1
                repeat(segmentCount) { segment ->
                    val startRow = 1 + (column * 2 + segment * 5) % availableRows
                    val seamY = bottom + startRow * cellSize + cellSize * 0.20f
                    val seamHeight =
                        (cellSize * (1.25f + ((column + segment) % 3) * 0.34f))
                            .coerceAtMost(top - seamY - cellSize * 0.35f)
                    if (seamHeight > cellSize * 0.35f) {
                        val alpha = if ((column + segment) % 2 == 0) 0.007f else 0.005f
                        canvas.drawRect(
                            tileBounds(left + column * cellSize - 0.5f, seamY, 1f, seamHeight),
                            color("7B7457", alpha),
                        )
                    }
                }
            }
            val availableColumns = (columns - 2).coerceAtLeast(1)
            (2 until rows step 3).forEach { row ->
                val segmentCount = if (row % 4 == 0) 2 else 1
                repeat(segmentCount) { segment ->
                    val startColumn = 1 + (row * 3 + segment * 4) % availableColumns
                    val seamX = left + startColumn * cellSize + cellSize * 0.18f
                    val seamWidth =
                        (cellSize * (1.35f + ((row + segment) % 4) * 0.30f))
                            .coerceAtMost(right - seamX - cellSize * 0.35f)
                    if (seamWidth > cellSize * 0.35f) {
                        val alpha = if ((row + segment) % 2 == 0) 0.006f else 0.004f
                        canvas.drawRect(
                            tileBounds(seamX, bottom + row * cellSize - 0.5f, seamWidth, 1f),
                            color("7B7457", alpha),
                        )
                    }
                }
            }
        }

        private fun drawVisibleRoomFloorUnifier(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorMaterials =
                frame.model.mapCellMaterials
                    .filter { material ->
                        material.kind == TileMapCellMaterialKind.FLOOR &&
                            material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            frame.viewport.containsTile(Point(material.x, material.y))
                    }
            if (floorMaterials.isEmpty()) {
                return
            }
            val rects = floorMaterials.map { material -> frame.viewport.tileRect(Point(material.x, material.y)) }
            val left = rects.minOf { rect -> rect.x }.toFloat()
            val right = rects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = rects.minOf { rect -> rect.y }.toFloat()
            val top = rects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val width = right - left
            val height = top - bottom
            val cellSize = frame.viewport.cellSize.toFloat()
            if (width < cellSize * 7f || height < cellSize * 5f) {
                return
            }
            canvas.drawRect(
                tileBounds(left + cellSize * 1.10f, bottom + cellSize * 0.82f, width - cellSize * 2.20f, height - cellSize * 1.64f),
                color("2C3A33", 0.158f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 2.35f, bottom + cellSize * 1.72f, width - cellSize * 4.70f, height - cellSize * 3.15f),
                color("6F674C", 0.060f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.35f, bottom + height * 0.40f, width * 0.34f, 3f),
                color("B69B6B", 0.040f),
            )
            drawVisibleRoomBrokenMortarCaps(canvas, frame)
        }

        private fun drawVisibleRoomApertureHierarchy(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorMaterials =
                frame.model.mapCellMaterials
                    .filter { material ->
                        material.kind == TileMapCellMaterialKind.FLOOR &&
                            material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            frame.viewport.containsTile(Point(material.x, material.y))
                    }
            if (floorMaterials.isEmpty()) {
                return
            }
            val rects = floorMaterials.map { material -> frame.viewport.tileRect(Point(material.x, material.y)) }
            val left = rects.minOf { rect -> rect.x }.toFloat()
            val right = rects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = rects.minOf { rect -> rect.y }.toFloat()
            val top = rects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val width = right - left
            val height = top - bottom
            val cellSize = frame.viewport.cellSize.toFloat()
            if (width < cellSize * 7f || height < cellSize * 5f) {
                return
            }

            val crownShadowHeight = (cellSize * 1.24f).coerceAtMost(height * 0.24f)
            canvas.drawRect(
                tileBounds(left + cellSize * 0.20f, top - crownShadowHeight - cellSize * 0.15f, width - cellSize * 0.40f, crownShadowHeight),
                color("050604", 0.148f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.58f, bottom + height * 0.22f, width * 0.34f, height * 0.56f),
                color("07100D", 0.109f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 0.42f, bottom + cellSize * 0.42f, width * 0.38f, height * 0.20f),
                color("050604", 0.116f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.24f, bottom + height * 0.24f, width * 0.48f, height * 0.48f),
                color("334036", 0.086f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.30f, bottom + height * 0.44f, width * 0.34f, 4f),
                color("A8905E", 0.052f),
            )
        }

        private fun drawVisibleRoomFloorSeamUnderpaint(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            floorMaterials: List<TileMapCellMaterialModel>,
        ) {
            val viewport = frame.viewport
            val floorPoints = floorMaterials.map { material -> Point(material.x, material.y) }.toSet()
            if (floorPoints.isEmpty()) {
                return
            }
            val minFloorX = floorPoints.minOf { point -> point.x }
            val maxFloorX = floorPoints.maxOf { point -> point.x }
            val minFloorY = floorPoints.minOf { point -> point.y }
            val maxFloorY = floorPoints.maxOf { point -> point.y }
            val floorColumns = maxFloorX - minFloorX + 1
            val floorRows = maxFloorY - minFloorY + 1

            floorMaterials.forEach { material ->
                val point = Point(material.x, material.y)
                val rect = viewport.tileRect(point)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val size = rect.width.toFloat()
                val localX = point.x - minFloorX
                val localY = point.y - minFloorY
                val interiorSeam =
                    localX in 1 until (floorColumns - 1).coerceAtLeast(1) &&
                        localY in 1 until (floorRows - 1).coerceAtLeast(1)
                if (!interiorSeam) {
                    return@forEach
                }
                val seamPattern = localX * 13 + localY * 17 + material.variant
                if (Point(point.x + 1, point.y) in floorPoints && seamPattern % 5 != 1 && seamPattern % 5 != 4) {
                    val seamHeight = size * (0.62f + (seamPattern % 4) * 0.060f)
                    val seamY = y + (size - seamHeight) * 0.50f + (seamPattern % 3 - 1) * 1.1f
                    canvas.drawRect(tileBounds(x + size - 4f, seamY, 8f, seamHeight), color("545E4C", 0.026f))
                    if (seamPattern % 4 == 0) {
                        canvas.drawRect(tileBounds(x + size - 1f, seamY + seamHeight * 0.34f, 2f, seamHeight * 0.24f), color("050604", 0.014f))
                    }
                }
                if (Point(point.x, point.y - 1) in floorPoints && seamPattern % 6 != 0 && seamPattern % 6 != 3) {
                    val seamWidth = size * (0.70f + (seamPattern % 3) * 0.060f)
                    val seamX = x + (size - seamWidth) * 0.46f + (seamPattern % 4 - 1) * 0.8f
                    canvas.drawRect(tileBounds(seamX, y + size - 4f, seamWidth, 8f), color("56604C", 0.024f))
                    if (seamPattern % 5 == 2) {
                        canvas.drawRect(tileBounds(seamX + seamWidth * 0.44f, y + size - 1f, seamWidth * 0.24f, 2f), color("A8905E", 0.014f))
                    }
                }
            }
        }

        private fun renderWarmMapOverlay(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val visibleMaterialPoints = visibleMaterialPoints(frame)
            val visibleClip = visibleRoomClip(frame) ?: frame.viewport.mapBounds
            drawMapStageStoneTexture(canvas, frame.layout.demoShell.mapStage)
            drawMapStageShadowVeil(canvas, frame.layout.demoShell.mapStage)
            drawHiddenStageGridSuppression(canvas, frame, visibleClip)
            drawVisibleWallRelief(canvas, frame)
            drawVisibleWallMassBands(canvas, frame)
            drawVisibleWallRaisedFaces(canvas, frame)
            drawVisibleWallCrownBlocks(canvas, frame)
            drawVisibleWallMasonryCourses(canvas, frame)
            drawVisibleWallFootRubble(canvas, frame)
            drawPr08WallFamilyReliefRepaint(canvas, frame)
            drawVisiblePassageThresholds(canvas, frame)
            drawVisibleRoomCornerBreakup(canvas, frame)
            drawVisibleRoomSilhouettePressure(canvas, frame, visibleClip)
            drawVisibleRoomBoundaryCompression(canvas, frame)
            drawVisibleRoomAsymmetricEdgeMass(canvas, frame)
            drawVisibleRoomMacroStructuralPlates(canvas, frame)
            drawVisibleRoomRuntimeCornerApertureShelves(canvas, frame)
            drawVisibleRoomOuterShadows(canvas, frame, visibleMaterialPoints)
            drawMapStageEdgeVignette(canvas, frame.layout.demoShell.mapStage)
            drawVisibleRoomContactShadows(canvas, frame)
            drawVisibleRoomStoryDecals(canvas, frame)
            if (frame.model.roomPresentationPlan.compositorStrategy != RoomCompositorStrategy.TOPOLOGY_RISK_HYBRID_PRESENTATION) {
                drawVisibleRoomMaterialBreakupAsset(canvas, frame)
            }
            drawTorchLightBlooms(canvas, frame, visibleMaterialPoints, visibleClip)
            drawTorchFixtures(canvas, frame)
            drawPlayerWarmLight(canvas, frame.model.playerTile, frame.viewport, visibleMaterialPoints, visibleClip)
            drawMapStageInnerFeather(canvas, frame.layout.demoShell.mapStage)
        }

        private fun drawHiddenStageGridSuppression(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            visibleClip: RectInt,
        ) {
            val mapBounds = frame.viewport.mapBounds
            val cellSize = frame.viewport.cellSize.toFloat()
            val leftGap = visibleClip.x - mapBounds.x
            if (leftGap > cellSize * 1.5f) {
                drawClippedRect(canvas, mapBounds.x.toFloat(), mapBounds.y.toFloat(), leftGap.toFloat(), mapBounds.height.toFloat(), mapBounds, color("05070A", 0.41f))
                drawClippedRect(canvas, visibleClip.x - cellSize * 1.9f, mapBounds.y.toFloat(), cellSize * 1.9f, mapBounds.height.toFloat(), mapBounds, color("05070A", 0.24f))
            }
            val rightGap = mapBounds.right - visibleClip.right
            if (rightGap > cellSize * 1.5f) {
                drawClippedRect(canvas, visibleClip.right.toFloat(), mapBounds.y.toFloat(), rightGap.toFloat(), mapBounds.height.toFloat(), mapBounds, color("05070A", 0.39f))
                drawClippedRect(canvas, visibleClip.right.toFloat(), mapBounds.y.toFloat(), cellSize * 1.9f, mapBounds.height.toFloat(), mapBounds, color("05070A", 0.23f))
            }
            val bottomGap = visibleClip.y - mapBounds.y
            if (bottomGap > cellSize * 1.5f) {
                drawClippedRect(canvas, mapBounds.x.toFloat(), mapBounds.y.toFloat(), mapBounds.width.toFloat(), bottomGap.toFloat(), mapBounds, color("05070A", 0.35f))
                drawClippedRect(canvas, mapBounds.x.toFloat(), visibleClip.y - cellSize * 1.35f, mapBounds.width.toFloat(), cellSize * 1.35f, mapBounds, color("05070A", 0.18f))
            }
            val topGap = mapBounds.top - visibleClip.top
            if (topGap > cellSize * 1.5f) {
                drawClippedRect(canvas, mapBounds.x.toFloat(), visibleClip.top.toFloat(), mapBounds.width.toFloat(), topGap.toFloat(), mapBounds, color("05070A", 0.36f))
                drawClippedRect(canvas, mapBounds.x.toFloat(), visibleClip.top.toFloat(), mapBounds.width.toFloat(), cellSize * 1.35f, mapBounds, color("05070A", 0.18f))
            }
            if (leftGap > cellSize * 3f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x - leftGap * 0.50f,
                    visibleClip.y + visibleClip.height * 0.11f,
                    leftGap * 0.36f,
                    visibleClip.height * 0.78f,
                    mapBounds,
                    color("010202", 0.232f),
                )
                drawClippedRect(
                    canvas,
                    visibleClip.x - leftGap * 0.44f,
                    visibleClip.y + visibleClip.height * 0.46f,
                    leftGap * 0.22f,
                    cellSize * 0.28f,
                    mapBounds,
                    color("6F5A39", 0.052f),
                )
            }
            if (bottomGap > cellSize * 2.5f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x - leftGap * 0.36f,
                    visibleClip.y - bottomGap * 0.33f,
                    visibleClip.width + leftGap * 0.58f,
                    bottomGap * 0.29f,
                    mapBounds,
                    color("010202", 0.218f),
                )
                drawClippedRect(
                    canvas,
                    visibleClip.x - leftGap * 0.24f,
                    visibleClip.y - bottomGap * 0.17f,
                    visibleClip.width * 0.42f,
                    cellSize * 0.12f,
                    mapBounds,
                    color("5D5440", 0.032f),
                )
            }
            if (leftGap > cellSize * 2.5f && bottomGap > cellSize * 2f) {
                drawClippedRect(canvas, mapBounds.x + cellSize * 0.65f, mapBounds.y + bottomGap * 0.18f, leftGap * 0.74f, bottomGap * 0.62f, mapBounds, color("020302", 0.29f))
                drawClippedRect(canvas, mapBounds.x + cellSize * 1.18f, visibleClip.y - cellSize * 1.05f, leftGap * 0.52f, cellSize * 0.68f, mapBounds, color("050607", 0.14f))
            }
            if (leftGap > cellSize * 2.5f) {
                drawClippedRect(canvas, mapBounds.x + leftGap * 0.12f, visibleClip.y + visibleClip.height * 0.43f, leftGap * 0.58f, cellSize * 1.46f, mapBounds, color("020303", 0.255f))
                drawClippedRect(canvas, visibleClip.x - leftGap * 0.36f, visibleClip.y + visibleClip.height * 0.55f, leftGap * 0.24f, cellSize * 0.44f, mapBounds, color("050607", 0.12f))
                drawClippedRect(canvas, visibleClip.x - cellSize * 0.70f, visibleClip.y + visibleClip.height * 0.31f, cellSize * 1.05f, cellSize * 1.58f, mapBounds, color("020303", 0.205f))
                drawClippedRect(canvas, visibleClip.x - 6f, visibleClip.y + visibleClip.height * 0.33f, 2f, cellSize * 1.36f, mapBounds, color("6F5A39", 0.066f))
            }
            if (rightGap > cellSize * 2.5f) {
                drawClippedRect(canvas, visibleClip.right + rightGap * 0.08f, visibleClip.y + visibleClip.height * 0.36f, rightGap * 0.62f, cellSize * 1.54f, mapBounds, color("020303", 0.222f))
                drawClippedRect(canvas, visibleClip.right + rightGap * 0.20f, visibleClip.y + visibleClip.height * 0.50f, rightGap * 0.32f, cellSize * 0.32f, mapBounds, color("6F5A39", 0.070f))
                drawClippedRect(canvas, visibleClip.right - cellSize * 0.18f, visibleClip.y + visibleClip.height * 0.47f, cellSize * 0.88f, cellSize * 1.20f, mapBounds, color("020303", 0.166f))
            }
            if (bottomGap > cellSize * 2.5f) {
                drawClippedRect(canvas, mapBounds.x + mapBounds.width * 0.22f, mapBounds.y + bottomGap * 0.14f, mapBounds.width * 0.52f, bottomGap * 0.46f, mapBounds, color("020303", 0.245f))
                drawClippedRect(canvas, visibleClip.x - cellSize * 0.72f, visibleClip.y - bottomGap * 0.32f, visibleClip.width + cellSize * 1.36f, cellSize * 0.58f, mapBounds, color("050607", 0.105f))
                drawClippedRect(canvas, visibleClip.x + visibleClip.width * 0.22f, visibleClip.y - cellSize * 0.58f, visibleClip.width * 0.62f, cellSize * 0.44f, mapBounds, color("020303", 0.176f))
                drawClippedRect(canvas, visibleClip.x + visibleClip.width * 0.16f, visibleClip.y - cellSize * 0.46f, visibleClip.width * 0.84f, cellSize * 0.44f, mapBounds, color("020303", 0.188f))
            }
            if (leftGap > cellSize * 3f && topGap > cellSize * 2f) {
                drawClippedRect(
                    canvas,
                    mapBounds.x + leftGap * 0.08f,
                    visibleClip.top + topGap * 0.10f,
                    leftGap * 0.78f,
                    topGap * 0.90f,
                    mapBounds,
                    color("020303", 0.268f),
                )
                drawClippedRect(
                    canvas,
                    mapBounds.x + leftGap * 0.42f,
                    visibleClip.top + topGap * 0.42f,
                    leftGap * 0.34f,
                    cellSize * 0.30f,
                    mapBounds,
                    color("6F5A39", 0.078f),
                )
                drawClippedRect(canvas, mapBounds.x + cellSize * 1.2f, visibleClip.top + cellSize * 0.7f, leftGap * 0.62f, topGap * 0.45f, mapBounds, color("050607", 0.18f))
            }
            if (rightGap > cellSize * 3f && bottomGap > cellSize * 2f) {
                drawClippedRect(canvas, visibleClip.right + cellSize * 0.8f, mapBounds.y + bottomGap * 0.20f, rightGap * 0.58f, bottomGap * 0.48f, mapBounds, color("050607", 0.16f))
            }
            if (topGap > cellSize * 2.5f) {
                drawClippedRect(canvas, visibleClip.x + visibleClip.width * 0.24f, visibleClip.top + 2f, visibleClip.width * 0.84f, cellSize * 0.44f, mapBounds, color("020303", 0.188f))
                drawClippedRect(canvas, visibleClip.x + visibleClip.width * 0.42f, visibleClip.top + 8f, visibleClip.width * 0.42f, 2f, mapBounds, color("6F5A39", 0.058f))
            }
            if (leftGap > cellSize * 2f) {
                drawClippedRect(canvas, visibleClip.x - cellSize * 0.38f, visibleClip.y + cellSize * 0.45f, cellSize * 0.55f, cellSize * 1.85f, mapBounds, color("020303", 0.196f))
                drawClippedRect(canvas, visibleClip.x - cellSize * 0.20f, visibleClip.y + cellSize * 2.05f, cellSize * 0.36f, cellSize * 1.12f, mapBounds, color("050607", 0.152f))
                drawClippedRect(canvas, visibleClip.x - cellSize * 0.30f, visibleClip.y + cellSize * 1.26f, cellSize * 0.48f, 3f, mapBounds, color("6F5A39", 0.072f))
            }
            if (rightGap > cellSize * 2f) {
                drawClippedRect(canvas, visibleClip.right - cellSize * 0.07f, visibleClip.y + cellSize * 0.92f, cellSize * 0.39f, cellSize * 1.45f, mapBounds, color("020303", 0.172f))
                drawClippedRect(canvas, visibleClip.right - cellSize * 0.18f, visibleClip.y + cellSize * 2.24f, cellSize * 0.54f, cellSize * 1.06f, mapBounds, color("050607", 0.138f))
                drawClippedRect(canvas, visibleClip.right - cellSize * 0.11f, visibleClip.y + cellSize * 1.58f, cellSize * 0.40f, 3f, mapBounds, color("6F5A39", 0.066f))
            }
            if (leftGap > cellSize * 3f) {
                drawClippedRect(
                    canvas,
                    mapBounds.x + leftGap * 0.04f,
                    mapBounds.y + mapBounds.height * 0.16f,
                    leftGap * 0.46f,
                    mapBounds.height * 0.70f,
                    mapBounds,
                    color("010101", 0.318f),
                )
            }
            if (bottomGap > cellSize * 2.5f) {
                drawClippedRect(
                    canvas,
                    mapBounds.x + mapBounds.width * 0.18f,
                    mapBounds.y + bottomGap * 0.10f,
                    mapBounds.width * 0.58f,
                    bottomGap * 0.44f,
                    mapBounds,
                    color("010101", 0.302f),
                )
            }
        }

        private fun drawHiddenStageApertureMasonry(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            visibleClip: RectInt,
        ) {
            val mapBounds = frame.viewport.mapBounds
            val cellSize = frame.viewport.cellSize.toFloat()
            if (visibleClip.width < cellSize * 2.5f || visibleClip.height < cellSize * 2.5f) {
                return
            }
            val leftGap = visibleClip.x - mapBounds.x
            val rightGap = mapBounds.right - visibleClip.right
            val bottomGap = visibleClip.y - mapBounds.y
            val topGap = mapBounds.top - visibleClip.top
            if (leftGap > cellSize * 1.5f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 2.22f,
                    visibleClip.y + visibleClip.height * 0.08f,
                    cellSize * 1.62f,
                    visibleClip.height * 0.88f,
                    mapBounds,
                    color("15201C", 0.132f),
                )
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 0.78f,
                    visibleClip.y + visibleClip.height * 0.25f,
                    3f,
                    visibleClip.height * 0.46f,
                    mapBounds,
                    color("85704A", 0.096f),
                )
            }
            if (topGap > cellSize * 1.5f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 0.42f,
                    visibleClip.top + cellSize * 0.24f,
                    visibleClip.width + cellSize * 1.62f,
                    cellSize * 0.84f,
                    mapBounds,
                    color("1B1710", 0.158f),
                )
                drawClippedRect(
                    canvas,
                    visibleClip.x + visibleClip.width * 0.18f,
                    visibleClip.top + cellSize * 0.34f,
                    visibleClip.width * 0.64f,
                    3f,
                    mapBounds,
                    color("8A7654", 0.088f),
                )
            }
            if (rightGap > cellSize * 1.5f) {
                drawClippedRect(
                    canvas,
                    visibleClip.right + cellSize * 0.46f,
                    visibleClip.y + visibleClip.height * 0.18f,
                    cellSize * 1.34f,
                    visibleClip.height * 0.70f,
                    mapBounds,
                    color("111B18", 0.112f),
                )
            }
            if (bottomGap > cellSize * 1.5f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x + visibleClip.width * 0.05f,
                    visibleClip.y - cellSize * 1.04f,
                    visibleClip.width * 0.76f,
                    cellSize * 0.52f,
                    mapBounds,
                    color("101713", 0.118f),
                )
            }
            drawDirectorScaleApertureDepth(
                canvas,
                frame,
                visibleClip,
                leftGap.toFloat(),
                rightGap.toFloat(),
                bottomGap.toFloat(),
                topGap.toFloat(),
            )
        }

        private fun drawDirectorScaleApertureDepth(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            visibleClip: RectInt,
            leftGap: Float,
            rightGap: Float,
            bottomGap: Float,
            topGap: Float,
        ) {
            val mapBounds = frame.viewport.mapBounds
            val cellSize = frame.viewport.cellSize.toFloat()
            val roomWidth = visibleClip.width.toFloat()
            val roomHeight = visibleClip.height.toFloat()
            if (roomWidth < cellSize * 2.5f || roomHeight < cellSize * 2.5f) {
                return
            }
            val playerCenterX =
                if (frame.viewport.containsTile(frame.model.playerTile)) {
                    val playerRect = frame.viewport.tileRect(frame.model.playerTile)
                    playerRect.x + playerRect.width / 2f
                } else {
                    visibleClip.x + roomWidth * 0.50f
                }
            val topLipWidth = (roomWidth * 0.50f).coerceAtMost(cellSize * 7.2f).coerceAtLeast(cellSize * 1.55f)
            val topLipMinX = visibleClip.x + cellSize * 0.50f
            val topLipMaxX = visibleClip.right - topLipWidth - cellSize * 0.40f
            val topLipX =
                if (topLipMaxX > topLipMinX) {
                    (playerCenterX - topLipWidth * 0.46f).coerceIn(topLipMinX, topLipMaxX)
                } else {
                    visibleClip.x + roomWidth * 0.18f
                }
            val bottomLipWidth = (roomWidth * 0.40f).coerceAtMost(cellSize * 5.8f).coerceAtLeast(cellSize * 1.20f)
            val bottomLipMinX = visibleClip.x + cellSize * 0.44f
            val bottomLipMaxX = visibleClip.right - bottomLipWidth - cellSize * 0.48f
            val bottomLipX =
                if (bottomLipMaxX > bottomLipMinX) {
                    (playerCenterX - bottomLipWidth * 0.48f).coerceIn(bottomLipMinX, bottomLipMaxX)
                } else {
                    visibleClip.x + roomWidth * 0.26f
                }

            if (topGap > cellSize * 1.35f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 1.05f,
                    visibleClip.top + cellSize * 0.12f,
                    roomWidth + cellSize * 2.18f,
                    cellSize * 0.72f,
                    mapBounds,
                    color("050604", 0.472f),
                )
                drawClippedRect(
                    canvas,
                    topLipX,
                    visibleClip.top + cellSize * 0.76f,
                    topLipWidth,
                    3f,
                    mapBounds,
                    color("A8905E", 0.178f),
                )
            }
            if (bottomGap > cellSize * 1.35f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 0.94f,
                    visibleClip.y - cellSize * 0.90f,
                    roomWidth + cellSize * 2.02f,
                    cellSize * 0.62f,
                    mapBounds,
                    color("050604", 0.438f),
                )
                drawClippedRect(
                    canvas,
                    bottomLipX,
                    visibleClip.y - cellSize * 0.33f,
                    bottomLipWidth,
                    3f,
                    mapBounds,
                    color("A8905E", 0.146f),
                )
            }
            if (leftGap > cellSize * 1.65f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 0.96f,
                    visibleClip.y + roomHeight * 0.15f,
                    cellSize * 0.78f,
                    roomHeight * 0.64f,
                    mapBounds,
                    color("050604", 0.416f),
                )
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 0.24f,
                    visibleClip.y + roomHeight * 0.30f,
                    3f,
                    roomHeight * 0.40f,
                    mapBounds,
                    color("A8905E", 0.154f),
                )
            }
            if (rightGap > cellSize * 1.65f) {
                drawClippedRect(
                    canvas,
                    visibleClip.right + cellSize * 0.18f,
                    visibleClip.y + roomHeight * 0.19f,
                    cellSize * 0.70f,
                    roomHeight * 0.55f,
                    mapBounds,
                    color("050604", 0.392f),
                )
                drawClippedRect(
                    canvas,
                    visibleClip.right + cellSize * 0.16f,
                    visibleClip.y + roomHeight * 0.44f,
                    3f,
                    roomHeight * 0.22f,
                    mapBounds,
                    color("7D6A49", 0.078f),
                )
            }
            drawDirectorScaleApertureCornerShelves(canvas, frame, visibleClip, leftGap, rightGap, bottomGap, topGap)
        }

        private fun drawDirectorScaleApertureCornerShelves(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            visibleClip: RectInt,
            leftGap: Float,
            rightGap: Float,
            bottomGap: Float,
            topGap: Float,
        ) {
            val mapBounds = frame.viewport.mapBounds
            val cellSize = frame.viewport.cellSize.toFloat()
            val roomWidth = visibleClip.width.toFloat()
            val roomHeight = visibleClip.height.toFloat()
            if (roomWidth < cellSize * 4.8f || roomHeight < cellSize * 4f) {
                return
            }
            if (leftGap > cellSize * 1.65f && topGap > cellSize * 1.05f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 1.18f,
                    visibleClip.top + cellSize * 0.10f,
                    roomWidth * 0.54f,
                    cellSize * 0.58f,
                    mapBounds,
                    color("050604", 0.344f),
                )
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 0.88f,
                    visibleClip.top + cellSize * 0.48f,
                    roomWidth * 0.32f,
                    3f,
                    mapBounds,
                    color("8A7654", 0.118f),
                )
                drawClippedRect(
                    canvas,
                    visibleClip.x - cellSize * 0.70f,
                    visibleClip.y + roomHeight * 0.23f,
                    cellSize * 0.52f,
                    roomHeight * 0.60f,
                    mapBounds,
                    color("050604", 0.286f),
                )
            }
            if (rightGap > cellSize * 1.20f && bottomGap > cellSize * 1.05f) {
                drawClippedRect(
                    canvas,
                    visibleClip.x + roomWidth * 0.42f,
                    visibleClip.y - cellSize * 0.54f,
                    roomWidth * 0.52f,
                    cellSize * 0.48f,
                    mapBounds,
                    color("050604", 0.318f),
                )
                drawClippedRect(
                    canvas,
                    visibleClip.x + roomWidth * 0.58f,
                    visibleClip.y - cellSize * 0.18f,
                    roomWidth * 0.24f,
                    3f,
                    mapBounds,
                    color("7D6A49", 0.094f),
                )
            }
        }

        private fun drawVisibleRoomAtmosphere(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val visibleMaterials =
                frame.model.mapCellMaterials
                    .filter { material ->
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            frame.viewport.containsTile(Point(material.x, material.y))
                    }
            if (visibleMaterials.isEmpty()) {
                return
            }
            val rects = visibleMaterials.map { material -> frame.viewport.tileRect(Point(material.x, material.y)) }
            val left = rects.minOf { rect -> rect.x }.toFloat()
            val right = rects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = rects.minOf { rect -> rect.y }.toFloat()
            val top = rects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val width = right - left
            val height = top - bottom
            val cellSize = frame.viewport.cellSize.toFloat()
            repeat(6) { index ->
                val inset = index * 10f
                val alpha = 0.006f * (1f - index / 6f)
                canvas.drawRect(
                    tileBounds(
                        left - inset,
                        bottom - inset,
                        width + inset * 2f,
                        height + inset * 2f,
                    ),
                    color("D99A2B", alpha),
                )
            }
            canvas.drawRect(
                tileBounds(left + cellSize * 0.42f, bottom + cellSize * 0.36f, width - cellSize * 0.84f, height - cellSize * 0.72f),
                color("2F3B32", 0.022f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 1.25f, bottom + cellSize * 1.05f, width - cellSize * 2.50f, height - cellSize * 2.10f),
                color("B8873E", 0.004f),
            )
            drawVisibleRoomSlabVariation(canvas, left, bottom, width, height, cellSize)
            drawVisibleRoomGridDissolve(canvas, left, bottom, width, height, cellSize)
            drawVisibleRoomStaggeredStoneRhythm(canvas, left, bottom, width, height, cellSize)
            drawVisibleRoomCrossCellSlabFields(canvas, left, bottom, width, height, cellSize)
            drawVisibleRoomScaleMaterialFields(canvas, frame, left, bottom, width, height, cellSize)
            drawVisibleRoomLocalizedStoneDamage(canvas, left, bottom, width, height, cellSize)
            drawVisibleRoomSilhouetteBreakup(canvas, left, bottom, width, height, cellSize)
            drawVisibleRoomPainterlyBreakup(canvas, left, bottom, width, height, cellSize)
            drawVisibleRoomTacticalClarityPlane(canvas, left, bottom, width, height, cellSize)
            if (width >= cellSize * 8f && height >= cellSize * 6f) {
                canvas.drawRect(
                    tileBounds(left + width - cellSize * 3.05f, bottom + cellSize * 1.08f, cellSize * 2.42f, height - cellSize * 2.16f),
                    color("071110", 0.104f),
                )
                canvas.drawRect(
                    tileBounds(left + width - cellSize * 2.35f, bottom + height * 0.35f, cellSize * 1.64f, cellSize * 0.30f),
                    color("2F3B32", 0.076f),
                )
                canvas.drawRect(
                    tileBounds(left + cellSize * 1.12f, bottom + cellSize * 0.72f, cellSize * 4.75f, cellSize * 1.42f),
                    color("050604", 0.092f),
                )
                canvas.drawRect(
                    tileBounds(left + cellSize * 1.52f, bottom + cellSize * 1.52f, cellSize * 3.10f, 3f),
                    color("6F5A39", 0.050f),
                )
            }
        }

        private fun drawVisibleRoomMaterialBreakupAsset(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val asset = frame.model.roomMaterialBreakup ?: return
            val viewport = frame.viewport
            val floorRects =
                visibleFloorPoints(frame)
                    .asSequence()
                    .filter(viewport::containsTile)
                    .map(viewport::tileRect)
                    .toList()
            if (floorRects.isEmpty()) {
                return
            }
            val left = floorRects.minOf { rect -> rect.x }.toFloat()
            val right = floorRects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = floorRects.minOf { rect -> rect.y }.toFloat()
            val top = floorRects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val width = right - left
            val height = top - bottom
            val cellSize = viewport.cellSize.toFloat()
            if (width < cellSize * 6f || height < cellSize * 4f) {
                return
            }

            val driftSeed =
                abs(
                    left.roundToInt() * 23 +
                        bottom.roundToInt() * 29 +
                        width.roundToInt() * 31 +
                        height.roundToInt() * 37 +
                        floorRects.size * 41,
                )
            val driftX = ((driftSeed % 5) - 2) * cellSize * 0.045f
            val driftY = (((driftSeed / 5) % 5) - 2) * cellSize * 0.035f
            val insetX = (cellSize * 0.62f).coerceAtMost(width * 0.11f)
            val insetY = (cellSize * 0.52f).coerceAtMost(height * 0.13f)
            canvas.drawAsset(
                asset,
                tileBounds(
                    left + insetX + driftX,
                    bottom + insetY + driftY,
                    width - insetX * 2f,
                    height - insetY * 2f,
                ),
                alpha = 0.78f,
            )
        }

        private fun drawVisibleRoomSlabVariation(
            canvas: TileCanvas,
            left: Float,
            bottom: Float,
            width: Float,
            height: Float,
            cellSize: Float,
        ) {
            if (width < cellSize * 8f || height < cellSize * 6f) {
                return
            }
            canvas.drawRect(
                tileBounds(left + cellSize * 1.20f, bottom + cellSize * 1.28f, cellSize * 3.85f, cellSize * 1.86f),
                color("3E4D42", 0.066f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 1.36f, bottom + cellSize * 2.95f, cellSize * 3.20f, 2f),
                color("8A8468", 0.052f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 4.80f, bottom + cellSize * 1.06f, cellSize * 3.15f, cellSize * 2.08f),
                color("2B3A33", 0.060f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 4.92f, bottom + cellSize * 1.22f, 2f, cellSize * 1.70f),
                color("A8925F", 0.040f),
            )
            canvas.drawRect(
                tileBounds(left + width - cellSize * 4.70f, bottom + cellSize * 1.54f, cellSize * 3.35f, cellSize * 1.62f),
                color("4B422E", 0.054f),
            )
            canvas.drawRect(
                tileBounds(left + width - cellSize * 4.28f, bottom + cellSize * 1.70f, cellSize * 2.10f, 2f),
                color("050604", 0.038f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 2.24f, bottom + height * 0.52f, cellSize * 3.70f, cellSize * 1.48f),
                color("1E2F2A", 0.058f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 2.58f, bottom + height * 0.56f, cellSize * 2.20f, 2f),
                color("7B8669", 0.046f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.53f, bottom + height * 0.51f, cellSize * 3.92f, cellSize * 1.72f),
                color("4F553F", 0.052f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.57f, bottom + height * 0.54f, cellSize * 2.74f, 2f),
                color("B69B6B", 0.040f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.36f, bottom + height - cellSize * 3.05f, cellSize * 4.12f, cellSize * 1.56f),
                color("25342E", 0.054f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.39f, bottom + height - cellSize * 2.70f, cellSize * 2.88f, 2f),
                color("8A8468", 0.038f),
            )
        }

        private fun drawVisibleRoomGridDissolve(
            canvas: TileCanvas,
            left: Float,
            bottom: Float,
            width: Float,
            height: Float,
            cellSize: Float,
        ) {
            if (width < cellSize * 6f || height < cellSize * 6f) {
                return
            }
            val columns = (width / cellSize).toInt()
            val rows = (height / cellSize).toInt()
            val jointPlugAnchors =
                listOf(
                    Pair(2, 2),
                    Pair(4, 3),
                    Pair(7, 2),
                    Pair(9, 4),
                    Pair(5, 5),
                )
            jointPlugAnchors.forEachIndexed { index, anchor ->
                val column = anchor.first
                val row = anchor.second
                if (column < columns && row < rows) {
                    val plugX = left + column * cellSize - 6f + if (index % 2 == 0) 0f else 1.5f
                    val plugY = bottom + row * cellSize - 6f + if (index % 3 == 0) 1f else 0f
                    canvas.drawRect(tileBounds(plugX, plugY, 13f, 12f), color("687058", 0.072f))
                    canvas.drawRect(tileBounds(plugX + 2f, plugY + 8f, 8f, 2f), color("050604", 0.096f))
                }
            }
            canvas.drawRect(
                tileBounds(left + cellSize * 1.65f, bottom + cellSize * 2.18f, cellSize * 3.45f, 22f),
                color("485446", 0.080f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 1.92f, bottom + cellSize * 2.50f, cellSize * 2.38f, 2f),
                color("A8905E", 0.062f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 5.35f, bottom + cellSize * 3.72f, cellSize * 3.10f, 20f),
                color("485446", 0.080f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 5.74f, bottom + cellSize * 3.94f, cellSize * 2.18f, 2f),
                color("050604", 0.088f),
            )
            canvas.drawRect(
                tileBounds(left + width - cellSize * 4.90f, bottom + height - cellSize * 2.68f, cellSize * 3.15f, 18f),
                color("485446", 0.080f),
            )
            canvas.drawRect(
                tileBounds(left + width - cellSize * 4.56f, bottom + height - cellSize * 2.42f, cellSize * 2.20f, 2f),
                color("A8905E", 0.056f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 2.35f, bottom + height * 0.43f, width - cellSize * 4.35f, cellSize * 0.82f),
                color("56614A", 0.086f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 2.85f, bottom + height * 0.48f, width - cellSize * 5.15f, 3f),
                color("B69B6B", 0.052f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.20f, bottom + height * 0.32f, cellSize * 4.80f, cellSize * 1.28f),
                color("050604", 0.074f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.58f, bottom + height * 0.58f, cellSize * 4.30f, cellSize * 1.12f),
                color("2B3A33", 0.076f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 1.10f, bottom + height * 0.52f, width - cellSize * 2.20f, cellSize * 0.36f),
                color("424D3F", 0.062f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.46f, bottom + cellSize * 0.86f, cellSize * 0.40f, height - cellSize * 1.92f),
                color("3D493F", 0.056f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 1.65f, bottom + height * 0.58f, width - cellSize * 3.30f, 2f),
                color("A8905E", 0.048f),
            )
        }

        private fun drawVisibleRoomBrokenMortarCaps(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val viewport = frame.viewport
            val floorMaterials =
                frame.model.mapCellMaterials
                    .filter { material ->
                        material.kind == TileMapCellMaterialKind.FLOOR &&
                            material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            viewport.containsTile(Point(material.x, material.y))
                    }
            if (floorMaterials.isEmpty()) {
                return
            }
            val floorPoints = floorMaterials.map { material -> Point(material.x, material.y) }.toSet()
            val minFloorX = floorPoints.minOf { point -> point.x }
            val maxFloorX = floorPoints.maxOf { point -> point.x }
            val minFloorY = floorPoints.minOf { point -> point.y }
            val maxFloorY = floorPoints.maxOf { point -> point.y }
            val floorColumns = maxFloorX - minFloorX + 1
            val floorRows = maxFloorY - minFloorY + 1

            floorMaterials.forEach { material ->
                val point = Point(material.x, material.y)
                val rect = viewport.tileRect(point)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val size = rect.width.toFloat()
                val localX = point.x - minFloorX
                val localY = point.y - minFloorY
                val seamPattern = localX * 7 + localY * 11
                val seamVariant = material.variant + seamPattern
                val interiorSeam =
                    localX in 1 until (floorColumns - 1).coerceAtLeast(1) &&
                        localY in 1 until (floorRows - 1).coerceAtLeast(1)
                if (Point(point.x + 1, point.y) in floorPoints && (point.x + point.y) % 7 == 0) {
                    canvas.drawRect(tileBounds(x + size - 5f, y + size * 0.18f, 10f, size * 0.66f), color("626A50", 0.046f))
                    canvas.drawRect(tileBounds(x + size - 2f, y + size * 0.50f, 4f, 9f), color("050604", 0.040f))
                }
                val verticalWeave = seamPattern % 5
                if (Point(point.x + 1, point.y) in floorPoints && interiorSeam && (verticalWeave == 0 || verticalWeave == 2)) {
                    val capWidth = if (seamVariant % 3 == 0) 21f else 19f
                    val capHeight = size * (0.86f + (seamVariant % 2) * 0.10f)
                    val capX = x + size - capWidth * 0.50f + (seamVariant % 3 - 1) * 0.8f
                    val capY = y + size * 0.02f + (seamVariant % 3) * 1.2f
                    canvas.drawRect(tileBounds(capX, capY, capWidth, capHeight), color("65705B", 0.052f))
                    canvas.drawRect(tileBounds(capX + capWidth * 0.58f, capY + capHeight * 0.36f, 4f, 12f), color("050604", 0.036f))
                    canvas.drawRect(tileBounds(capX + 3f, capY + capHeight * 0.70f, capWidth - 6f, 2f), color("A8905E", 0.024f))
                }
                if (Point(point.x, point.y - 1) in floorPoints && (point.x * 3 + point.y) % 8 == 0) {
                    canvas.drawRect(tileBounds(x + size * 0.16f, y + size - 5f, size * 0.72f, 10f), color("5A6048", 0.040f))
                    canvas.drawRect(tileBounds(x + size * 0.48f, y + size - 2f, 9f, 4f), color("B69B6B", 0.026f))
                }
                val horizontalWeave = (localX * 5 + localY * 7) % 6
                if (Point(point.x, point.y - 1) in floorPoints && interiorSeam && (horizontalWeave == 1 || horizontalWeave == 4)) {
                    val capWidth = size * (1.08f + (seamVariant % 2) * 0.18f)
                    val capHeight = if (seamVariant % 3 == 1) 18f else 16f
                    val capX = x + size * 0.08f + (seamVariant % 4 - 1) * 1.2f
                    val capY = y + size - capHeight * 0.50f + (seamVariant % 3 - 1) * 0.9f
                    canvas.drawRect(tileBounds(capX, capY, capWidth, capHeight), color("626A50", 0.048f))
                    canvas.drawRect(tileBounds(capX + capWidth * 0.42f, capY + capHeight * 0.58f, 12f, 4f), color("050604", 0.034f))
                    canvas.drawRect(tileBounds(capX + capWidth * 0.18f, capY + 3f, capWidth * 0.58f, 2f), color("B69B6B", 0.022f))
                }
            }
        }

        private fun drawVisibleRoomStaggeredStoneRhythm(
            canvas: TileCanvas,
            left: Float,
            bottom: Float,
            width: Float,
            height: Float,
            cellSize: Float,
        ) {
            if (width < cellSize * 8f || height < cellSize * 6f) {
                return
            }
            val slabWidth = cellSize * 2.25f
            val slabHeight = cellSize * 0.72f
            val darkCutHeight = cellSize * 0.16f
            val slabColor = color("485446", 0.096f)
            val mutedSlabColor = color("26342E", 0.078f)
            val mortarCut = color("050604", 0.118f)
            val wornEdge = color("A8905E", 0.054f)

            fun drawSlab(
                x: Float,
                y: Float,
                muted: Boolean,
            ) {
                canvas.drawRect(tileBounds(x, y, slabWidth, slabHeight), if (muted) mutedSlabColor else slabColor)
                canvas.drawRect(tileBounds(x + cellSize * 0.20f, y + slabHeight - darkCutHeight, cellSize * 1.58f, darkCutHeight), mortarCut)
                canvas.drawRect(tileBounds(x + cellSize * 0.46f, y + cellSize * 0.18f, cellSize * 1.30f, 1.5f), wornEdge)
            }

            drawSlab(left + cellSize * 4.40f, bottom + cellSize * 2.95f, muted = false)
            drawSlab(left + cellSize * 2.05f, bottom + cellSize * 3.86f, muted = true)
            drawSlab(left + cellSize * 6.78f, bottom + cellSize * 5.18f, muted = false)
            drawSlab(left + width - cellSize * 4.25f, bottom + cellSize * 1.70f, muted = true)
            drawSlab(left + width - cellSize * 5.55f, bottom + height - cellSize * 2.38f, muted = false)
            canvas.drawRect(
                tileBounds(left + cellSize * 3.18f, bottom + cellSize * 3.30f, cellSize * 4.70f, cellSize * 0.48f),
                color("65705B", 0.104f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 7.24f, bottom + cellSize * 1.44f, cellSize * 0.52f, cellSize * 3.74f),
                color("4F5A4B", 0.101f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 3.70f, bottom + cellSize * 3.58f, cellSize * 2.86f, 2.5f),
                color("C49B61", 0.063f),
            )
            canvas.drawRect(tileBounds(left + cellSize * 1.42f, bottom + height * 0.52f, width - cellSize * 2.84f, 3f), color("6F6B50", 0.042f))
            canvas.drawRect(tileBounds(left + cellSize * 2.58f, bottom + height * 0.66f, width - cellSize * 4.90f, 3f), color("050604", 0.072f))
        }

        private fun drawVisibleRoomLocalizedStoneDamage(
            canvas: TileCanvas,
            left: Float,
            bottom: Float,
            width: Float,
            height: Float,
            cellSize: Float,
        ) {
            if (width < cellSize * 8f || height < cellSize * 6f) {
                return
            }
            canvas.drawRect(
                tileBounds(left + cellSize * 4.05f, bottom + cellSize * 4.20f, cellSize * 1.50f, cellSize * 0.52f),
                color("070806", 0.142f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 4.34f, bottom + cellSize * 4.38f, cellSize * 0.96f, 2.5f),
                color("9B8055", 0.082f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 8.20f, bottom + cellSize * 2.48f, cellSize * 0.82f, 3f),
                color("B28A5A", 0.086f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 8.42f, bottom + cellSize * 2.30f, cellSize * 0.54f, cellSize * 0.20f),
                color("080706", 0.122f),
            )
            canvas.drawRect(
                tileBounds(left + width - cellSize * 4.15f, bottom + height - cellSize * 3.22f, cellSize * 1.08f, cellSize * 0.34f),
                color("4A1B13", 0.096f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 2.70f, bottom + height - cellSize * 4.05f, cellSize * 0.72f, cellSize * 0.18f),
                color("0C1C17", 0.118f),
            )
            val debrisAnchors =
                listOf(
                    Pair(1.58f, 1.92f),
                    Pair(5.72f, 1.54f),
                    Pair(9.42f, 5.02f),
                )
            debrisAnchors.forEachIndexed { index, anchor ->
                val chipX = left + cellSize * anchor.first
                val chipY = bottom + cellSize * anchor.second
                val chipWidth = if (index % 2 == 0) 5f else 4f
                val chipHeight = if (index % 3 == 0) 3f else 4f
                canvas.drawRect(tileBounds(chipX, chipY, chipWidth, chipHeight), color("050604", 0.060f))
                canvas.drawRect(tileBounds(chipX + 1.5f, chipY + chipHeight + 1f, chipWidth - 1f, 1.5f), color("A8905E", 0.046f))
            }
            val pittedStoneAnchors =
                listOf(
                    Pair(1.95f, 2.26f),
                    Pair(3.16f, 1.68f),
                    Pair(4.48f, 5.08f),
                    Pair(8.68f, 2.18f),
                )
            pittedStoneAnchors.forEachIndexed { index, anchor ->
                val pitX = left + cellSize * anchor.first
                val pitY = bottom + cellSize * anchor.second
                val pitSize = if (index % 3 == 0) 3f else 2f
                canvas.drawRect(tileBounds(pitX, pitY, pitSize, pitSize), color("050604", 0.038f))
            }
            val shortCutAnchors =
                listOf(
                    Pair(2.18f, 3.12f),
                    Pair(4.86f, 1.96f),
                    Pair(8.38f, 2.92f),
                )
            shortCutAnchors.forEachIndexed { index, anchor ->
                val cutX = left + cellSize * anchor.first
                val cutY = bottom + cellSize * anchor.second
                val cutWidth = if (index % 2 == 0) 9f else 7f
                canvas.drawRect(tileBounds(cutX, cutY, cutWidth, 1.5f), color("B69B6B", 0.040f))
            }
        }

        private fun drawVisibleRoomCrossCellSlabFields(
            canvas: TileCanvas,
            left: Float,
            bottom: Float,
            width: Float,
            height: Float,
            cellSize: Float,
        ) {
            if (width < cellSize * 8f || height < cellSize * 6f) {
                return
            }
            canvas.drawRect(
                tileBounds(left + cellSize * 2.05f, bottom + cellSize * 1.58f, cellSize * 5.68f, cellSize * 2.62f),
                color("344137", 0.118f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 2.42f, bottom + cellSize * 2.18f, cellSize * 4.72f, 8f),
                color("B69B6B", 0.068f),
            )
            canvas.drawRect(
                tileBounds(left + width - cellSize * 7.10f, bottom + height - cellSize * 3.48f, cellSize * 5.86f, cellSize * 2.30f),
                color("242D27", 0.104f),
            )
            canvas.drawRect(
                tileBounds(left + width - cellSize * 6.72f, bottom + height - cellSize * 2.42f, cellSize * 5.10f, 9f),
                color("7F6F50", 0.062f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.29f, bottom + height * 0.48f, cellSize * 6.10f, 10f),
                color("050604", 0.140f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.33f, bottom + height * 0.48f + 6f, cellSize * 4.24f, 2f),
                color("A8905E", 0.050f),
            )
        }

        private fun drawVisibleRoomScaleMaterialFields(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            left: Float,
            bottom: Float,
            width: Float,
            height: Float,
            cellSize: Float,
        ) {
            if (width < cellSize * 8f || height < cellSize * 6f) {
                return
            }
            val floorPoints = visibleFloorPoints(frame)
            if (floorPoints.isEmpty()) {
                return
            }
            val minFloorX = floorPoints.minOf { point -> point.x }
            val maxFloorX = floorPoints.maxOf { point -> point.x }
            val minFloorY = floorPoints.minOf { point -> point.y }
            val maxFloorY = floorPoints.maxOf { point -> point.y }
            val phase =
                abs(
                    minFloorX * 31 +
                        minFloorY * 37 +
                        maxFloorX * 41 +
                        maxFloorY * 43 +
                        frame.model.playerTile.x * 7 +
                        frame.model.playerTile.y * 11,
                ) % 5
            val drift = (phase - 2) * cellSize * 0.07f

            canvas.drawRect(
                tileBounds(left + width * 0.06f + drift, bottom + height * 0.12f, width * 0.62f, height * 0.38f),
                color("2F473A", 0.128f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.15f + drift * 0.35f, bottom + height * 0.19f, width * 0.40f, 5f),
                color("8A8468", 0.074f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.48f - drift, bottom + height * 0.50f, width * 0.42f, height * 0.32f),
                color("111D19", 0.116f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.57f - drift * 0.40f, bottom + height * 0.61f, width * 0.24f, 4f),
                color("5F6C57", 0.080f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.35f + drift * 0.50f, bottom + height * 0.34f, width * 0.30f, height * 0.26f),
                color("050604", 0.110f),
            )
        }

        private fun drawVisibleRoomSilhouetteBreakup(
            canvas: TileCanvas,
            left: Float,
            bottom: Float,
            width: Float,
            height: Float,
            cellSize: Float,
        ) {
            if (width < cellSize * 8f || height < cellSize * 6f) {
                return
            }
            val dark = color("050604", 0.24f)
            val softDark = color("050604", 0.17f)
            val stone = color("2B2F27", 0.10f)
            canvas.drawRect(tileBounds(left, bottom + height - cellSize * 4.12f, cellSize * 4.82f, cellSize * 3.82f), dark)
            canvas.drawRect(tileBounds(left + cellSize * 0.72f, bottom + height - cellSize * 4.26f, cellSize * 2.70f, cellSize * 0.96f), stone)
            canvas.drawRect(tileBounds(left + width - cellSize * 5.04f, bottom + height - cellSize * 4.06f, cellSize * 4.70f, cellSize * 3.76f), dark)
            canvas.drawRect(tileBounds(left + width - cellSize * 3.88f, bottom + height - cellSize * 4.46f, cellSize * 2.14f, cellSize * 0.96f), stone)
            canvas.drawRect(tileBounds(left, bottom + cellSize * 0.08f, cellSize * 3.65f, cellSize * 3.22f), softDark)
            canvas.drawRect(tileBounds(left + width - cellSize * 3.70f, bottom + cellSize * 0.02f, cellSize * 3.42f, cellSize * 3.36f), softDark)
            canvas.drawRect(tileBounds(left, bottom + height * 0.36f, cellSize * 3.15f, cellSize * 2.60f), color("050604", 0.18f))
            canvas.drawRect(tileBounds(left + width - cellSize * 3.08f, bottom + height * 0.46f, cellSize * 2.78f, cellSize * 2.82f), color("050604", 0.18f))
            canvas.drawRect(tileBounds(left + width * 0.37f, bottom, cellSize * 3.92f, cellSize * 1.35f), color("050604", 0.12f))
            canvas.drawRect(tileBounds(left + width * 0.42f, bottom + height - cellSize * 1.36f, cellSize * 3.16f, cellSize * 1.02f), color("050604", 0.12f))
        }

        private fun drawVisibleRoomPainterlyBreakup(
            canvas: TileCanvas,
            left: Float,
            bottom: Float,
            width: Float,
            height: Float,
            cellSize: Float,
        ) {
            if (width < cellSize * 8f || height < cellSize * 6f) {
                return
            }
            canvas.drawRect(
                tileBounds(left + cellSize * 1.05f, bottom + cellSize * 0.90f, width - cellSize * 2.10f, height - cellSize * 1.95f),
                color("14221E", 0.045f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 3.18f, bottom + cellSize * 2.34f, cellSize * 4.10f, cellSize * 1.46f),
                color("39483D", 0.083f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 3.52f, bottom + cellSize * 3.62f, cellSize * 2.78f, 3f),
                color("A8905E", 0.057f),
            )
            canvas.drawRect(
                tileBounds(left + width - cellSize * 5.96f, bottom + cellSize * 4.76f, cellSize * 3.72f, cellSize * 1.34f),
                color("111B18", 0.091f),
            )
            canvas.drawRect(
                tileBounds(left + width - cellSize * 5.40f, bottom + cellSize * 5.42f, cellSize * 2.60f, 3f),
                color("050604", 0.087f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.18f, bottom + height * 0.38f, cellSize * 4.25f, cellSize * 1.82f),
                color("050604", 0.052f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.25f, bottom + height * 0.44f, cellSize * 2.75f, cellSize * 0.22f),
                color("8A7654", 0.060f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.52f, bottom + height * 0.42f, cellSize * 3.68f, cellSize * 1.42f),
                color("2B2F27", 0.055f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.58f, bottom + height * 0.48f, cellSize * 2.10f, cellSize * 0.24f),
                color("B69B6B", 0.045f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.22f, bottom + height * 0.60f, cellSize * 3.62f, cellSize * 0.48f),
                color("050604", 0.100f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.27f, bottom + height * 0.64f, cellSize * 2.46f, cellSize * 0.12f),
                color("B69B6B", 0.052f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.58f, bottom + height * 0.24f, cellSize * 2.78f, cellSize * 1.22f),
                color("183529", 0.050f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.32f, bottom + height * 0.34f, cellSize * 2.98f, cellSize * 1.16f),
                color("050604", 0.050f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.35f, bottom + height * 0.39f, cellSize * 2.12f, cellSize * 0.28f),
                color("7B8669", 0.038f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.66f, bottom + height * 0.63f, cellSize * 1.92f, cellSize * 0.78f),
                color("4B0B08", 0.085f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.70f, bottom + height * 0.68f, cellSize * 1.18f, cellSize * 0.22f),
                color("6E1310", 0.065f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.42f, bottom + height * 0.42f, cellSize * 3.06f, cellSize * 0.60f),
                color("6F745C", 0.118f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.46f, bottom + height * 0.415f, 4f, cellSize * 0.58f),
                color("050604", 0.124f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.47f, bottom + height * 0.485f, cellSize * 2.24f, 2.5f),
                color("C49B61", 0.066f),
            )
        }

        private fun drawVisibleRoomTacticalClarityPlane(
            canvas: TileCanvas,
            left: Float,
            bottom: Float,
            width: Float,
            height: Float,
            cellSize: Float,
        ) {
            if (width < cellSize * 8f || height < cellSize * 6f) {
                return
            }
            canvas.drawRect(
                tileBounds(left + width * 0.34f, bottom + height * 0.425f, cellSize * 4.90f, cellSize * 1.16f),
                color("07100D", 0.085f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.38f, bottom + height * 0.485f, cellSize * 3.80f, 4f),
                color("050604", 0.095f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.43f, bottom + height * 0.525f, cellSize * 2.55f, 2f),
                color("A8905E", 0.045f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.45f, bottom + height * 0.51f, cellSize * 2.75f, 3f),
                color("050604", 0.086f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.585f, bottom + height * 0.465f, 3f, cellSize * 1.06f),
                color("050604", 0.080f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.50f, bottom + height * 0.57f, cellSize * 1.95f, 2f),
                color("8EA38E", 0.054f),
            )
        }

        private fun drawVisibleWallRelief(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorPoints = visibleFloorPoints(frame)
            if (floorPoints.isEmpty()) {
                return
            }
            frame.model.mapCellMaterials
                .asSequence()
                .filter { material ->
                    material.kind == TileMapCellMaterialKind.WALL &&
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                        frame.viewport.containsTile(Point(material.x, material.y)) &&
                        hasAdjacentVisibleFloor(material, floorPoints)
                }.forEach { material ->
                    val rect = frame.viewport.tileRect(Point(material.x, material.y))
                    val x = rect.x.toFloat()
                    val y = rect.y.toFloat()
                    val size = rect.width.toFloat()
                    val floorNorth = Point(material.x, material.y + 1) in floorPoints
                    val floorSouth = Point(material.x, material.y - 1) in floorPoints
                    val floorWest = Point(material.x - 1, material.y) in floorPoints
                    val floorEast = Point(material.x + 1, material.y) in floorPoints
                    canvas.drawRect(tileBounds(x + 2f, y + size - 6f, size - 4f, 3f), color("B69B6B", 0.13f))
                    canvas.drawRect(tileBounds(x + 2f, y + 2f, size - 4f, 5f), color("050604", 0.24f))
                    if (floorSouth) {
                        canvas.drawRect(tileBounds(x + 2f, y, size - 4f, 9f), color("050604", 0.22f))
                        canvas.drawRect(tileBounds(x + 5f, y + 8f, size - 10f, 2f), color("6F5A39", 0.18f))
                    }
                    if (floorNorth) {
                        canvas.drawRect(tileBounds(x + 2f, y + size - 9f, size - 4f, 7f), color("D0A35A", 0.08f))
                    }
                    if (floorWest) {
                        canvas.drawRect(tileBounds(x, y + 2f, 7f, size - 4f), color("050604", 0.18f))
                        canvas.drawRect(tileBounds(x + 7f, y + 5f, 2f, size - 10f), color("6F5A39", 0.12f))
                    }
                    if (floorEast) {
                        canvas.drawRect(tileBounds(x + size - 7f, y + 2f, 7f, size - 4f), color("050604", 0.18f))
                        canvas.drawRect(tileBounds(x + size - 9f, y + 5f, 2f, size - 10f), color("6F5A39", 0.12f))
                    }
                }
        }

        private fun drawVisibleWallMassBands(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorPoints = visibleFloorPoints(frame)
            if (floorPoints.isEmpty()) {
                return
            }
            val viewport = frame.viewport
            val walls =
                frame.model.mapCellMaterials
                    .filter { material ->
                        material.kind == TileMapCellMaterialKind.WALL &&
                            material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            viewport.containsTile(Point(material.x, material.y)) &&
                            hasAdjacentVisibleFloor(material, floorPoints)
                    }
            if (walls.isEmpty()) {
                return
            }
            val cellSize = viewport.cellSize.toFloat()
            val wallPoints = walls.map { material -> Point(material.x, material.y) }.toSet()

            fun drawHorizontalRun(
                row: Int,
                start: Int,
                end: Int,
            ) {
                if (end - start + 1 < 3) {
                    return
                }
                val runPoints = (start..end).map { x -> Point(x, row) }.filter { point -> point in wallPoints }
                val floorSouth = runPoints.any { point -> Point(point.x, point.y - 1) in floorPoints }
                val floorNorth = runPoints.any { point -> Point(point.x, point.y + 1) in floorPoints }
                if (!floorSouth && !floorNorth) {
                    return
                }
                val firstRect = viewport.tileRect(Point(start, row))
                val lastRect = viewport.tileRect(Point(end, row))
                val left = firstRect.x.toFloat()
                val width = lastRect.x + lastRect.width - firstRect.x.toFloat()
                if (floorSouth) {
                    val y = firstRect.y.toFloat()
                    canvas.drawRect(tileBounds(left + 2f, y, width - 4f, 13f), color("050604", 0.30f))
                    canvas.drawRect(tileBounds(left + 8f, y + 9f, width - 16f, 3f), color("6F5A39", 0.18f))
                    canvas.drawRect(tileBounds(left + cellSize * 0.45f, y + 4f, width - cellSize * 0.90f, 4f), color("1E1710", 0.22f))
                }
                if (floorNorth) {
                    val y = firstRect.y + cellSize - 13f
                    canvas.drawRect(tileBounds(left + 2f, y, width - 4f, 13f), color("050604", 0.30f))
                    canvas.drawRect(tileBounds(left + 8f, y + 2f, width - 16f, 3f), color("7A6040", 0.17f))
                    canvas.drawRect(tileBounds(left + cellSize * 0.42f, y + 7f, width - cellSize * 0.84f, 4f), color("1A120C", 0.20f))
                }
            }

            fun drawVerticalRun(
                column: Int,
                start: Int,
                end: Int,
            ) {
                if (end - start + 1 < 3) {
                    return
                }
                val runPoints = (start..end).map { y -> Point(column, y) }.filter { point -> point in wallPoints }
                val floorWest = runPoints.any { point -> Point(point.x - 1, point.y) in floorPoints }
                val floorEast = runPoints.any { point -> Point(point.x + 1, point.y) in floorPoints }
                if (!floorWest && !floorEast) {
                    return
                }
                val firstRect = viewport.tileRect(Point(column, start))
                val lastRect = viewport.tileRect(Point(column, end))
                val bottom = minOf(firstRect.y, lastRect.y).toFloat()
                val top = maxOf(firstRect.y + firstRect.height, lastRect.y + lastRect.height).toFloat()
                val height = top - bottom
                if (floorEast) {
                    val x = firstRect.x + cellSize - 13f
                    canvas.drawRect(tileBounds(x, bottom + 2f, 13f, height - 4f), color("050604", 0.26f))
                    canvas.drawRect(tileBounds(x + 2f, bottom + 8f, 3f, height - 16f), color("6F5A39", 0.16f))
                    canvas.drawRect(tileBounds(x + 7f, bottom + cellSize * 0.42f, 4f, height - cellSize * 0.84f), color("1A120C", 0.18f))
                }
                if (floorWest) {
                    val x = firstRect.x.toFloat()
                    canvas.drawRect(tileBounds(x, bottom + 2f, 13f, height - 4f), color("050604", 0.26f))
                    canvas.drawRect(tileBounds(x + 8f, bottom + 8f, 3f, height - 16f), color("7A6040", 0.15f))
                    canvas.drawRect(tileBounds(x + 2f, bottom + cellSize * 0.42f, 4f, height - cellSize * 0.84f), color("1A120C", 0.18f))
                }
            }

            walls.groupBy { material -> material.y }.forEach { (row, rowWalls) ->
                val sorted = rowWalls.map { material -> material.x }.sorted()
                var start = sorted.first()
                var previous = start
                sorted.drop(1).forEach { x ->
                    if (x == previous + 1) {
                        previous = x
                    } else {
                        drawHorizontalRun(row, start, previous)
                        start = x
                        previous = x
                    }
                }
                drawHorizontalRun(row, start, previous)
            }
            walls.groupBy { material -> material.x }.forEach { (column, columnWalls) ->
                val sorted = columnWalls.map { material -> material.y }.sorted()
                var start = sorted.first()
                var previous = start
                sorted.drop(1).forEach { y ->
                    if (y == previous + 1) {
                        previous = y
                    } else {
                        drawVerticalRun(column, start, previous)
                        start = y
                        previous = y
                    }
                }
                drawVerticalRun(column, start, previous)
            }
        }

        private fun drawVisibleWallRaisedFaces(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorPoints = visibleFloorPoints(frame)
            if (floorPoints.isEmpty()) {
                return
            }
            val viewport = frame.viewport
            val walls =
                frame.model.mapCellMaterials
                    .filter { material ->
                        material.kind == TileMapCellMaterialKind.WALL &&
                            material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            viewport.containsTile(Point(material.x, material.y))
                    }
            if (walls.isEmpty()) {
                return
            }
            val cellSize = viewport.cellSize.toFloat()
            val wallPoints = walls.map { material -> Point(material.x, material.y) }.toSet()

            fun drawHorizontalRun(
                row: Int,
                start: Int,
                end: Int,
            ) {
                val runLength = end - start + 1
                if (runLength < 4) {
                    return
                }
                val runPoints = (start..end).map { x -> Point(x, row) }.filter { point -> point in wallPoints }
                val floorSouth = runPoints.any { point -> Point(point.x, point.y - 1) in floorPoints }
                val floorNorth = runPoints.any { point -> Point(point.x, point.y + 1) in floorPoints }
                if (!floorSouth && !floorNorth) {
                    return
                }
                val firstRect = viewport.tileRect(Point(start, row))
                val lastRect = viewport.tileRect(Point(end, row))
                val left = firstRect.x.toFloat()
                val width = lastRect.x + lastRect.width - firstRect.x.toFloat()
                if (floorNorth) {
                    val shadowY = firstRect.y + cellSize - 24f
                    canvas.drawRect(
                        tileBounds(left + cellSize * 0.72f, shadowY, width - cellSize * 1.44f, 22f),
                        color("050604", 0.278f),
                    )
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.04f, shadowY + 6f, width - cellSize * 2.08f, 3f),
                        color("A8905E", 0.092f),
                    )
                    val y = firstRect.y + cellSize - 19f
                    canvas.drawRect(
                        tileBounds(left + cellSize, y, width - cellSize * 2f, 18f),
                        color("050604", 0.235f),
                    )
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.22f, y + 3f, width - cellSize * 2.44f, 4f),
                        color("A8905E", 0.115f),
                    )
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.48f, y + 10f, width - cellSize * 2.96f, 4f),
                        color("1D1710", 0.170f),
                    )
                    val floorRect = viewport.tileRect(Point(start, row + 1))
                    val floorY = floorRect.y.toFloat()
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.12f, floorY + 1f, width - cellSize * 2.24f, 12f),
                        color("050604", 0.127f),
                    )
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.70f, floorY + 10f, width - cellSize * 3.40f, 3f),
                        color("2F3B32", 0.069f),
                    )
                    if (runLength >= 7) {
                        val blockY = firstRect.y + cellSize - 29f
                        canvas.drawRect(tileBounds(left + cellSize * 4.42f, blockY, cellSize * 1.88f, 9f), color("8A7654", 0.147f))
                        canvas.drawRect(tileBounds(left + cellSize * 6.68f, blockY + 2f, cellSize * 1.46f, 7f), color("6F5A39", 0.136f))
                        canvas.drawRect(tileBounds(left + cellSize * 5.03f, blockY + 8f, 3f, 12f), color("050604", 0.109f))
                        canvas.drawRect(tileBounds(left + cellSize * 6.32f, blockY + 1f, 3f, 11f), color("050604", 0.109f))
                        canvas.drawRect(tileBounds(left + width - cellSize * 3.08f, blockY + 6f, 3f, 12f), color("050604", 0.109f))
                    }
                    if (runLength >= 8) {
                        val capY = firstRect.y + cellSize - 31f
                        canvas.drawRect(tileBounds(left + cellSize * 4.58f, capY, cellSize * 2.52f, 12f), color("2A3028", 0.183f))
                        canvas.drawRect(tileBounds(left + cellSize * 4.92f, capY + 8f, cellSize * 1.62f, 3f), color("A8905E", 0.121f))
                        canvas.drawRect(tileBounds(left + cellSize * 6.18f, capY + 2f, 3f, 17f), color("050604", 0.157f))
                    }
                }
                if (floorSouth) {
                    val shadowY = firstRect.y + 2f
                    canvas.drawRect(
                        tileBounds(left + cellSize * 0.72f, shadowY, width - cellSize * 1.44f, 22f),
                        color("050604", 0.260f),
                    )
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.06f, shadowY + 4f, width - cellSize * 2.12f, 3f),
                        color("A8905E", 0.078f),
                    )
                    val y = firstRect.y.toFloat()
                    canvas.drawRect(
                        tileBounds(left + cellSize, y, width - cellSize * 2f, 18f),
                        color("050604", 0.220f),
                    )
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.30f, y + 7f, width - cellSize * 2.60f, 6f),
                        color("6F5A39", 0.155f),
                    )
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.52f, y + 13f, width - cellSize * 3.04f, 3f),
                        color("050604", 0.150f),
                    )
                    val floorRect = viewport.tileRect(Point(start, row - 1))
                    val floorY = floorRect.y.toFloat()
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.12f, floorY + cellSize - 13f, width - cellSize * 2.24f, 12f),
                        color("050604", 0.119f),
                    )
                    canvas.drawRect(
                        tileBounds(left + cellSize * 1.70f, floorY + cellSize - 8f, width - cellSize * 3.40f, 3f),
                        color("6F5A39", 0.069f),
                    )
                    if (runLength >= 7) {
                        val blockY = firstRect.y + 13f
                        canvas.drawRect(tileBounds(left + cellSize * 3.24f, blockY, cellSize * 1.72f, 9f), color("7A6040", 0.141f))
                        canvas.drawRect(tileBounds(left + cellSize * 5.72f, blockY + 1f, cellSize * 1.58f, 8f), color("8A7654", 0.132f))
                        canvas.drawRect(tileBounds(left + cellSize * 4.86f, blockY - 3f, 3f, 12f), color("050604", 0.109f))
                        canvas.drawRect(tileBounds(left + cellSize * 7.18f, blockY + 5f, 3f, 11f), color("050604", 0.109f))
                    }
                    if (runLength >= 8) {
                        val capY = firstRect.y + 15f
                        canvas.drawRect(tileBounds(left + cellSize * 4.76f, capY, cellSize * 2.40f, 11f), color("2A3028", 0.176f))
                        canvas.drawRect(tileBounds(left + cellSize * 5.10f, capY + 2f, cellSize * 1.48f, 3f), color("8A7654", 0.114f))
                        canvas.drawRect(tileBounds(left + cellSize * 6.68f, capY - 1f, 3f, 17f), color("050604", 0.157f))
                    }
                }
            }

            fun drawVerticalRun(
                column: Int,
                start: Int,
                end: Int,
            ) {
                val runLength = end - start + 1
                if (runLength < 4) {
                    return
                }
                val runPoints = (start..end).map { y -> Point(column, y) }.filter { point -> point in wallPoints }
                val floorWest = runPoints.any { point -> Point(point.x - 1, point.y) in floorPoints }
                val floorEast = runPoints.any { point -> Point(point.x + 1, point.y) in floorPoints }
                if (!floorWest && !floorEast) {
                    return
                }
                val firstRect = viewport.tileRect(Point(column, start))
                val lastRect = viewport.tileRect(Point(column, end))
                val bottom = minOf(firstRect.y, lastRect.y).toFloat()
                val top = maxOf(firstRect.y + firstRect.height, lastRect.y + lastRect.height).toFloat()
                val height = top - bottom
                if (floorEast) {
                    val x = firstRect.x + cellSize - 19f
                    canvas.drawRect(tileBounds(x, bottom + cellSize, 18f, height - cellSize * 2f), color("050604", 0.205f))
                    canvas.drawRect(tileBounds(x + 3f, bottom + cellSize * 1.22f, 4f, height - cellSize * 2.44f), color("A8905E", 0.095f))
                    val floorRect = viewport.tileRect(Point(column + 1, start))
                    val floorX = floorRect.x.toFloat()
                    canvas.drawRect(tileBounds(floorX + 1f, bottom + cellSize * 1.18f, 12f, height - cellSize * 2.36f), color("050604", 0.113f))
                    canvas.drawRect(tileBounds(floorX + 9f, bottom + cellSize * 1.58f, 3f, height - cellSize * 3.16f), color("2F3B32", 0.071f))
                    if (runLength >= 6) {
                        val plateX = firstRect.x + cellSize - 14f
                        canvas.drawRect(tileBounds(plateX, bottom + cellSize * 2.18f, 9f, cellSize * 1.28f), color("7A6040", 0.141f))
                        canvas.drawRect(tileBounds(plateX - 2f, bottom + cellSize * 3.72f, 8f, cellSize * 1.08f), color("8A7654", 0.124f))
                        canvas.drawRect(tileBounds(plateX - 1f, bottom + cellSize * 3.02f, 3f, 12f), color("050604", 0.109f))
                    }
                }
                if (floorWest) {
                    val x = firstRect.x.toFloat()
                    canvas.drawRect(tileBounds(x, bottom + cellSize, 18f, height - cellSize * 2f), color("050604", 0.205f))
                    canvas.drawRect(tileBounds(x + 11f, bottom + cellSize * 1.22f, 4f, height - cellSize * 2.44f), color("6F5A39", 0.095f))
                    val floorRect = viewport.tileRect(Point(column - 1, start))
                    val floorX = floorRect.x + cellSize - 12f
                    canvas.drawRect(tileBounds(floorX, bottom + cellSize * 1.18f, 12f, height - cellSize * 2.36f), color("050604", 0.113f))
                    canvas.drawRect(tileBounds(floorX, bottom + cellSize * 1.58f, 3f, height - cellSize * 3.16f), color("6F5A39", 0.071f))
                    if (runLength >= 6) {
                        val plateX = firstRect.x + 5f
                        canvas.drawRect(tileBounds(plateX, bottom + cellSize * 3.36f, 9f, cellSize * 1.31f), color("8A7654", 0.141f))
                        canvas.drawRect(tileBounds(plateX + 2f, bottom + cellSize * 5.05f, 8f, cellSize * 1.06f), color("6F5A39", 0.119f))
                        canvas.drawRect(tileBounds(plateX + 8f, bottom + cellSize * 3.90f, 3f, 12f), color("050604", 0.109f))
                    }
                }
            }

            walls.groupBy { material -> material.y }.forEach { (row, rowWalls) ->
                val sorted = rowWalls.map { material -> material.x }.sorted()
                var start = sorted.first()
                var previous = start
                sorted.drop(1).forEach { x ->
                    if (x == previous + 1) {
                        previous = x
                    } else {
                        drawHorizontalRun(row, start, previous)
                        start = x
                        previous = x
                    }
                }
                drawHorizontalRun(row, start, previous)
            }
            walls.groupBy { material -> material.x }.forEach { (column, columnWalls) ->
                val sorted = columnWalls.map { material -> material.y }.sorted()
                var start = sorted.first()
                var previous = start
                sorted.drop(1).forEach { y ->
                    if (y == previous + 1) {
                        previous = y
                    } else {
                        drawVerticalRun(column, start, previous)
                        start = y
                        previous = y
                    }
                }
                drawVerticalRun(column, start, previous)
            }
        }

        private fun drawVisibleWallCrownBlocks(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorPoints = visibleFloorPoints(frame)
            if (floorPoints.isEmpty()) {
                return
            }
            val viewport = frame.viewport
            val wallMaterials =
                frame.model.mapCellMaterials
                    .filter { material ->
                        material.kind == TileMapCellMaterialKind.WALL &&
                            material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            viewport.containsTile(Point(material.x, material.y))
                    }
            val wallPoints = wallMaterials.map { material -> Point(material.x, material.y) }.toSet()
            wallMaterials
                .asSequence()
                .filter { material -> hasAdjacentVisibleFloor(material, floorPoints) }
                .forEach { material ->
                    val point = Point(material.x, material.y)
                    val rect = viewport.tileRect(point)
                    val x = rect.x.toFloat()
                    val y = rect.y.toFloat()
                    val size = rect.width.toFloat()
                    val floorNorth = Point(point.x, point.y + 1) in floorPoints
                    val floorSouth = Point(point.x, point.y - 1) in floorPoints
                    val floorWest = Point(point.x - 1, point.y) in floorPoints
                    val floorEast = Point(point.x + 1, point.y) in floorPoints
                    if (floorNorth && (point.x + point.y) % 3 == 2) {
                        canvas.drawRect(tileBounds(x + 5f, y + size - 17f, 22f, 8f), color("A8905E", 0.168f))
                        canvas.drawRect(tileBounds(x + size - 7f, y + size - 19f, 4f, 13f), color("050604", 0.158f))
                    }
                    if (floorSouth && (point.x * 2 + point.y) % 5 == 1) {
                        canvas.drawRect(tileBounds(x + 4f, y + 7f, 21f, 8f), color("6F5A39", 0.154f))
                        canvas.drawRect(tileBounds(x + 8f, y + 3f, 13f, 4f), color("050604", 0.150f))
                    }
                    if (floorWest && (point.x + point.y) % 5 == 0) {
                        canvas.drawRect(tileBounds(x + 3f, y + size * 0.20f, 7f, 21f), color("050604", 0.154f))
                        canvas.drawRect(tileBounds(x + 8f, y + size * 0.31f, 3f, 13f), color("A8905E", 0.080f))
                    }
                    if (floorEast && (point.x * 2 + point.y) % 5 == 0) {
                        canvas.drawRect(tileBounds(x + size - 10f, y + size * 0.18f, 7f, 21f), color("050604", 0.154f))
                        canvas.drawRect(tileBounds(x + size - 11f, y + size * 0.42f, 3f, 12f), color("6F5A39", 0.078f))
                    }
                }
            wallMaterials.forEach { material ->
                val point = Point(material.x, material.y)
                val rect = viewport.tileRect(point)
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val size = rect.width.toFloat()
                if (Point(point.x + 1, point.y) in wallPoints &&
                    Point(point.x, point.y + 1) in wallPoints &&
                    Point(point.x + 1, point.y + 1) in floorPoints
                ) {
                    canvas.drawRect(tileBounds(x + size - 27f, y + size - 27f, 27f, 27f), color("050604", 0.215f))
                    canvas.drawRect(tileBounds(x + size - 20f, y + size - 7f, 14f, 3f), color("8A7654", 0.105f))
                }
                if (Point(point.x - 1, point.y) in wallPoints &&
                    Point(point.x, point.y + 1) in wallPoints &&
                    Point(point.x - 1, point.y + 1) in floorPoints
                ) {
                    canvas.drawRect(tileBounds(x, y + size - 27f, 27f, 27f), color("050604", 0.215f))
                    canvas.drawRect(tileBounds(x + 6f, y + size - 7f, 14f, 3f), color("8A7654", 0.105f))
                }
                if (Point(point.x + 1, point.y) in wallPoints &&
                    Point(point.x, point.y - 1) in wallPoints &&
                    Point(point.x + 1, point.y - 1) in floorPoints
                ) {
                    canvas.drawRect(tileBounds(x + size - 27f, y, 27f, 27f), color("050604", 0.215f))
                    canvas.drawRect(tileBounds(x + size - 20f, y + 4f, 14f, 3f), color("6F5A39", 0.098f))
                }
                if (Point(point.x - 1, point.y) in wallPoints &&
                    Point(point.x, point.y - 1) in wallPoints &&
                    Point(point.x - 1, point.y - 1) in floorPoints
                ) {
                    canvas.drawRect(tileBounds(x, y, 27f, 27f), color("050604", 0.215f))
                    canvas.drawRect(tileBounds(x + 6f, y + 4f, 14f, 3f), color("6F5A39", 0.098f))
                }
            }
        }

        private fun drawVisibleWallMasonryCourses(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorPoints = visibleFloorPoints(frame)
            if (floorPoints.isEmpty()) {
                return
            }
            val viewport = frame.viewport
            frame.model.mapCellMaterials
                .asSequence()
                .filter { material ->
                    material.kind == TileMapCellMaterialKind.WALL &&
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                        viewport.containsTile(Point(material.x, material.y)) &&
                        hasAdjacentVisibleFloor(material, floorPoints)
                }.forEach { material ->
                    val point = Point(material.x, material.y)
                    val rect = viewport.tileRect(point)
                    val x = rect.x.toFloat()
                    val y = rect.y.toFloat()
                    val size = rect.width.toFloat()
                    val floorNorth = Point(point.x, point.y + 1) in floorPoints
                    val floorSouth = Point(point.x, point.y - 1) in floorPoints
                    val floorWest = Point(point.x - 1, point.y) in floorPoints
                    val floorEast = Point(point.x + 1, point.y) in floorPoints
                    if (floorNorth && (point.x * 3 + point.y) % 4 == 3) {
                        canvas.drawRect(tileBounds(x + 5f, y + size - 22f, 16f, 5f), color("8A7654", 0.132f))
                        canvas.drawRect(tileBounds(x + 22f, y + size - 23f, 2f, 7f), color("050604", 0.122f))
                    }
                    if (floorNorth && (point.x + point.y) % 2 == 0) {
                        val tickX = x + 7f + ((point.x * 5 + point.y * 3) % 11).toFloat()
                        canvas.drawRect(tileBounds(tickX, y + size - 19f, 3f, 10f), color("050604", 0.118f))
                    }
                    if (floorNorth && (point.x + point.y) % 3 == 1) {
                        val fleckX = x + 9f + ((point.x * 2 + point.y) % 8).toFloat()
                        canvas.drawRect(tileBounds(fleckX, y + size - 11f, 9f, 3f), color("8A7654", 0.086f))
                    }
                    if (floorNorth && (point.x + point.y) % 2 == 1) {
                        val chipX = x + 11f + ((point.x + point.y) % 3).toFloat()
                        canvas.drawRect(tileBounds(chipX, y + size - 15f, 4f, 4f), color("050604", 0.104f))
                        canvas.drawRect(tileBounds(chipX + 7f, y + size - 8f, 6f, 2f), color("B69B6B", 0.073f))
                    }
                    if (floorSouth && (point.x + point.y * 2) % 4 == 2) {
                        canvas.drawRect(tileBounds(x + 7f, y + 16f, 15f, 5f), color("6F5A39", 0.122f))
                        canvas.drawRect(tileBounds(x + 4f, y + 15f, 2f, 7f), color("050604", 0.116f))
                    }
                    if (floorSouth && (point.x + point.y) % 2 == 1) {
                        val tickX = x + 8f + ((point.x * 3 + point.y * 4) % 10).toFloat()
                        canvas.drawRect(tileBounds(tickX, y + 11f, 3f, 10f), color("050604", 0.118f))
                    }
                    if (floorSouth && (point.x + point.y) % 3 == 2) {
                        val fleckX = x + 6f + ((point.x * 4 + point.y) % 9).toFloat()
                        canvas.drawRect(tileBounds(fleckX, y + 20f, 9f, 3f), color("8A7654", 0.086f))
                    }
                    if (floorSouth && (point.x + point.y) % 2 == 0) {
                        val chipX = x + 9f + ((point.x * 2 + point.y) % 4).toFloat()
                        canvas.drawRect(tileBounds(chipX, y + 13f, 4f, 4f), color("050604", 0.104f))
                        canvas.drawRect(tileBounds(chipX + 6f, y + 22f, 6f, 2f), color("B69B6B", 0.073f))
                    }
                    if (floorWest && (point.x + point.y) % 3 == 0) {
                        canvas.drawRect(tileBounds(x + 5f, y + size * 0.34f, 5f, 16f), color("7A6040", 0.126f))
                        canvas.drawRect(tileBounds(x + 4f, y + size * 0.34f + 17f, 7f, 2f), color("050604", 0.112f))
                    }
                    if (floorWest && (point.x * 2 + point.y) % 2 == 0) {
                        val tickY = y + 7f + ((point.x * 5 + point.y * 3) % 12).toFloat()
                        canvas.drawRect(tileBounds(x + 9f, tickY, 10f, 3f), color("050604", 0.112f))
                    }
                    if (floorWest && (point.x + point.y) % 3 == 0) {
                        val chipY = y + 11f + ((point.x * 5 + point.y * 2) % 6).toFloat()
                        canvas.drawRect(tileBounds(x + 12f, chipY, 2f, 7f), color("050604", 0.096f))
                    }
                    if (floorEast && (point.x * 2 + point.y) % 3 == 1) {
                        canvas.drawRect(tileBounds(x + size - 10f, y + size * 0.30f, 5f, 15f), color("8A7654", 0.118f))
                        canvas.drawRect(tileBounds(x + size - 11f, y + size * 0.30f - 2f, 7f, 2f), color("050604", 0.110f))
                    }
                    if (floorEast && (point.x + point.y * 2) % 2 == 1) {
                        val tickY = y + 8f + ((point.x * 3 + point.y * 5) % 11).toFloat()
                        canvas.drawRect(tileBounds(x + size - 18f, tickY, 10f, 3f), color("050604", 0.112f))
                    }
                    if (floorEast && (point.x + point.y) % 3 == 1) {
                        val chipY = y + 10f + ((point.x * 4 + point.y * 3) % 7).toFloat()
                        canvas.drawRect(tileBounds(x + size - 15f, chipY, 2f, 7f), color("050604", 0.096f))
                    }
                }
        }

        private fun drawVisibleWallFootRubble(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorPoints = visibleFloorPoints(frame)
            if (floorPoints.isEmpty()) {
                return
            }
            val viewport = frame.viewport
            val walls =
                frame.model.mapCellMaterials
                    .filter { material ->
                        material.kind == TileMapCellMaterialKind.WALL &&
                            material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            viewport.containsTile(Point(material.x, material.y)) &&
                            hasAdjacentVisibleFloor(material, floorPoints)
                    }
            if (walls.isEmpty()) {
                return
            }
            val cellSize = viewport.cellSize.toFloat()

            fun drawHorizontalRun(
                row: Int,
                start: Int,
                end: Int,
            ) {
                if (end - start + 1 < 5) {
                    return
                }
                val runPoints = (start..end).map { x -> Point(x, row) }
                val floorSouth = runPoints.any { point -> Point(point.x, point.y - 1) in floorPoints }
                val floorNorth = runPoints.any { point -> Point(point.x, point.y + 1) in floorPoints }
                val firstRect = viewport.tileRect(Point(start, row))
                val lastRect = viewport.tileRect(Point(end, row))
                val left = firstRect.x.toFloat()
                val width = lastRect.x + lastRect.width - firstRect.x.toFloat()
                val clusterWidth = cellSize * 1.16f
                if (floorNorth) {
                    val y = firstRect.y + cellSize - 11f
                    val floorY = viewport.tileRect(Point(start, row + 1)).y.toFloat()
                    canvas.drawRect(tileBounds(left + cellSize * 1.38f, floorY + 2f, width - cellSize * 2.75f, 6f), color("050604", 0.133f))
                    canvas.drawRect(tileBounds(left + cellSize * 4.46f, floorY + 7f, 5f, 4f), color("314035", 0.097f))
                    canvas.drawRect(tileBounds(left + cellSize * 5.42f, floorY + 7f, 5f, 4f), color("314035", 0.097f))
                    canvas.drawRect(tileBounds(left + cellSize * 7.84f, floorY + 8f, 4f, 4f), color("050604", 0.097f))
                    canvas.drawRect(tileBounds(left + cellSize * 3.88f, y, clusterWidth, 10f), color("050604", 0.185f))
                    canvas.drawRect(tileBounds(left + cellSize * 4.12f, y + 3f, 9f, 5f), color("6F5A39", 0.145f))
                    canvas.drawRect(tileBounds(left + width - cellSize * 3.42f, y + 1f, clusterWidth * 0.82f, 9f), color("050604", 0.150f))
                }
                if (floorSouth) {
                    val y = firstRect.y + 1f
                    val floorY = viewport.tileRect(Point(start, row - 1)).y.toFloat()
                    canvas.drawRect(tileBounds(left + cellSize * 1.64f, floorY + cellSize - 8f, width - cellSize * 2.90f, 6f), color("050604", 0.122f))
                    canvas.drawRect(tileBounds(left + cellSize * 4.46f, floorY + cellSize - 11f, 5f, 4f), color("314035", 0.097f))
                    canvas.drawRect(tileBounds(left + cellSize * 5.42f, floorY + cellSize - 11f, 5f, 4f), color("314035", 0.097f))
                    canvas.drawRect(tileBounds(left + cellSize * 7.66f, floorY + cellSize - 12f, 4f, 4f), color("050604", 0.097f))
                    canvas.drawRect(tileBounds(left + cellSize * 2.70f, y, clusterWidth, 10f), color("050604", 0.172f))
                    canvas.drawRect(tileBounds(left + cellSize * 2.96f, y + 4f, 8f, 5f), color("7A6040", 0.132f))
                    canvas.drawRect(tileBounds(left + width - cellSize * 4.85f, y + 1f, clusterWidth * 0.92f, 9f), color("050604", 0.145f))
                }
            }

            fun drawVerticalRun(
                column: Int,
                start: Int,
                end: Int,
            ) {
                if (end - start + 1 < 5) {
                    return
                }
                val runPoints = (start..end).map { y -> Point(column, y) }
                val floorWest = runPoints.any { point -> Point(point.x - 1, point.y) in floorPoints }
                val floorEast = runPoints.any { point -> Point(point.x + 1, point.y) in floorPoints }
                val firstRect = viewport.tileRect(Point(column, start))
                val lastRect = viewport.tileRect(Point(column, end))
                val bottom = minOf(firstRect.y, lastRect.y).toFloat()
                val height = maxOf(firstRect.y + firstRect.height, lastRect.y + lastRect.height) - bottom
                val contactHeight = height - cellSize * 3.34f
                val contactTop = bottom + cellSize * 1.68f
                if (floorWest) {
                    canvas.drawRect(tileBounds(firstRect.x - 8f, contactTop, 7f, contactHeight), color("050604", 0.107f))
                    canvas.drawRect(tileBounds(firstRect.x - 11f, contactTop + cellSize * 1.14f, 4f, 4f), color("314035", 0.097f))
                }
                if (floorEast) {
                    canvas.drawRect(tileBounds(firstRect.x + cellSize + 1f, contactTop + 3f, 7f, contactHeight * 0.92f), color("050604", 0.107f))
                    canvas.drawRect(tileBounds(firstRect.x + cellSize + 8f, contactTop + cellSize * 1.32f, 4f, 4f), color("314035", 0.097f))
                }
            }

            fun drawWallChip(material: TileMapCellMaterialModel) {
                if ((material.x + material.y) % 4 != 0) {
                    return
                }
                val rect = viewport.tileRect(Point(material.x, material.y))
                val x = rect.x.toFloat()
                val y = rect.y.toFloat()
                val size = rect.width.toFloat()
                val floorNorth = Point(material.x, material.y + 1) in floorPoints
                val floorSouth = Point(material.x, material.y - 1) in floorPoints
                val floorWest = Point(material.x - 1, material.y) in floorPoints
                val floorEast = Point(material.x + 1, material.y) in floorPoints
                if (floorEast) {
                    canvas.drawRect(tileBounds(x + size - 9f, y + size * 0.50f, 8f, 5f), color("B69B6B", 0.145f))
                    canvas.drawRect(tileBounds(x + size - 13f, y + size * 0.31f, 10f, 7f), color("050604", 0.145f))
                }
                if (floorWest) {
                    canvas.drawRect(tileBounds(x + 1f, y + size * 0.45f, 8f, 5f), color("7A6040", 0.132f))
                    canvas.drawRect(tileBounds(x + 3f, y + size * 0.64f, 11f, 7f), color("050604", 0.135f))
                }
                if (floorNorth) {
                    canvas.drawRect(tileBounds(x + size * 0.24f, y + size - 9f, 11f, 5f), color("A8905E", 0.128f))
                }
                if (floorSouth) {
                    canvas.drawRect(tileBounds(x + size * 0.48f, y + 3f, 10f, 5f), color("6F5A39", 0.122f))
                }
            }

            walls.groupBy { material -> material.y }.forEach { (row, rowWalls) ->
                val sorted = rowWalls.map { material -> material.x }.sorted()
                var start = sorted.first()
                var previous = start
                sorted.drop(1).forEach { x ->
                    if (x == previous + 1) {
                        previous = x
                    } else {
                        drawHorizontalRun(row, start, previous)
                        start = x
                        previous = x
                    }
                }
                drawHorizontalRun(row, start, previous)
            }
            walls.groupBy { material -> material.x }.forEach { (column, columnWalls) ->
                val sorted = columnWalls.map { material -> material.y }.sorted()
                var start = sorted.first()
                var previous = start
                sorted.drop(1).forEach { y ->
                    if (y == previous + 1) {
                        previous = y
                    } else {
                        drawVerticalRun(column, start, previous)
                        start = y
                        previous = y
                    }
                }
                drawVerticalRun(column, start, previous)
            }
            walls.forEach(::drawWallChip)
        }

        private fun drawPr08WallFamilyReliefRepaint(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            if (frame.model.roomPresentationPlan.compositorStrategy == RoomCompositorStrategy.TOPOLOGY_RISK_HYBRID_PRESENTATION) {
                return
            }
            frame.layerPlan.terrainBase
                .asSequence()
                .filter { placement ->
                    placement.alpha >= 0.99f &&
                        placement.asset.entry.category == "tile_wall" &&
                        placement.asset.resolvedKey.isPr08RuinsWallFamily()
                }.forEach { placement ->
                    drawPlacement(canvas, placement, frame.viewport, alphaScale = 0.46f)
                }
        }

        private fun String.isPr08RuinsWallFamily(): Boolean =
            this == DarkUiMapVisualKeys.RUINS_WALL || startsWith("${DarkUiMapVisualKeys.RUINS_WALL}.")

        private fun drawVisiblePassageThresholds(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorPoints = visibleFloorPoints(frame)
            if (floorPoints.isEmpty()) {
                return
            }
            val viewport = frame.viewport
            frame.model.mapCellMaterials
                .asSequence()
                .filter { material ->
                    material.kind == TileMapCellMaterialKind.FLOOR &&
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                        viewport.containsTile(Point(material.x, material.y))
                }.forEach { material ->
                    val northFloor = Point(material.x, material.y + 1) in floorPoints
                    val southFloor = Point(material.x, material.y - 1) in floorPoints
                    val westFloor = Point(material.x - 1, material.y) in floorPoints
                    val eastFloor = Point(material.x + 1, material.y) in floorPoints
                    val wideNorth =
                        northFloor &&
                            Point(material.x - 1, material.y + 1) in floorPoints &&
                            Point(material.x + 1, material.y + 1) in floorPoints
                    val wideSouth =
                        southFloor &&
                            Point(material.x - 1, material.y - 1) in floorPoints &&
                            Point(material.x + 1, material.y - 1) in floorPoints
                    val wideWest =
                        westFloor &&
                            Point(material.x - 1, material.y - 1) in floorPoints &&
                            Point(material.x - 1, material.y + 1) in floorPoints
                    val wideEast =
                        eastFloor &&
                            Point(material.x + 1, material.y - 1) in floorPoints &&
                            Point(material.x + 1, material.y + 1) in floorPoints
                    val verticalPassage = material.westOcclusion && material.eastOcclusion && (northFloor || southFloor)
                    val horizontalPassage = material.northOcclusion && material.southOcclusion && (westFloor || eastFloor)
                    if (!verticalPassage && !horizontalPassage) {
                        return@forEach
                    }
                    val rect = viewport.tileRect(Point(material.x, material.y))
                    val x = rect.x.toFloat()
                    val y = rect.y.toFloat()
                    val size = rect.width.toFloat()
                    if (verticalPassage) {
                        drawClippedRect(canvas, x, y - 8f, 8f, size + 16f, viewport.mapBounds, color("050604", 0.38f))
                        drawClippedRect(canvas, x + size - 8f, y - 8f, 8f, size + 16f, viewport.mapBounds, color("050604", 0.38f))
                        drawClippedRect(canvas, x + 7f, y - 3f, 2f, size + 6f, viewport.mapBounds, color("7A6040", 0.20f))
                        drawClippedRect(canvas, x + size - 9f, y - 3f, 2f, size + 6f, viewport.mapBounds, color("7A6040", 0.20f))
                        drawClippedRect(canvas, x + 9f, y + size * 0.22f, 7f, 19f, viewport.mapBounds, color("050604", 0.236f))
                        drawClippedRect(canvas, x + size - 15f, y + size * 0.54f, 6f, 15f, viewport.mapBounds, color("050604", 0.214f))
                        drawClippedRect(canvas, x + size * 0.36f, y + size * 0.30f, 10f, 2f, viewport.mapBounds, color("8A7654", 0.121f))
                        if (northFloor) {
                            if (wideNorth) {
                                drawClippedRect(canvas, x - size * 0.42f, y + size - 13f, size * 1.84f, 13f, viewport.mapBounds, color("050604", 0.205f))
                                drawClippedRect(canvas, x - size * 0.24f, y + size - 18f, size * 1.48f, 18f, viewport.mapBounds, color("050604", 0.287f))
                            }
                            drawClippedRect(canvas, x + 4f, y + size - 10f, size - 8f, 10f, viewport.mapBounds, color("050604", 0.31f))
                            drawClippedRect(canvas, x + 8f, y + size - 5f, size - 16f, 2f, viewport.mapBounds, color("B69B6B", 0.19f))
                            if (wideNorth) {
                                drawClippedRect(canvas, x - size * 0.02f, y + size - 5f, size * 1.04f, 4f, viewport.mapBounds, color("A8905E", 0.132f))
                                drawClippedRect(canvas, x + size * 0.72f, y + size - 12f, 8f, 9f, viewport.mapBounds, color("1A120C", 0.132f))
                                drawClippedRect(canvas, x + 4f, y + size - 17f, 5f, 14f, viewport.mapBounds, color("050604", 0.263f))
                                drawClippedRect(canvas, x + size - 9f, y + size - 15f, 5f, 11f, viewport.mapBounds, color("050604", 0.241f))
                                drawClippedRect(canvas, x + size * 0.34f, y + size - 4f, 8f, 2f, viewport.mapBounds, color("B69B6B", 0.149f))
                                drawClippedRect(canvas, x + size * 0.58f, y + size - 7f, 7f, 2f, viewport.mapBounds, color("8A7654", 0.149f))
                                drawClippedRect(canvas, x - size * 0.81f, y + size + 2f, size * 2.62f, 18f, viewport.mapBounds, color("050604", 0.232f))
                                drawClippedRect(canvas, x - size * 0.48f, y + size + 9f, size * 1.64f, 4f, viewport.mapBounds, color("8A7654", 0.148f))
                                drawClippedRect(canvas, x - size * 0.58f, y + size + 7f, 11f, 26f, viewport.mapBounds, color("050604", 0.168f))
                                drawClippedRect(canvas, x + size * 1.24f, y + size + 10f, 11f, 24f, viewport.mapBounds, color("050604", 0.168f))
                            }
                        }
                        if (southFloor) {
                            if (wideSouth) {
                                drawClippedRect(canvas, x - size * 0.42f, y, size * 1.84f, 13f, viewport.mapBounds, color("050604", 0.205f))
                                drawClippedRect(canvas, x - size * 0.24f, y, size * 1.48f, 18f, viewport.mapBounds, color("050604", 0.287f))
                            }
                            drawClippedRect(canvas, x + 4f, y, size - 8f, 10f, viewport.mapBounds, color("050604", 0.31f))
                            drawClippedRect(canvas, x + 8f, y + 4f, size - 16f, 2f, viewport.mapBounds, color("B69B6B", 0.17f))
                            if (wideSouth) {
                                drawClippedRect(canvas, x - size * 0.02f, y + 1f, size * 1.04f, 4f, viewport.mapBounds, color("A8905E", 0.132f))
                                drawClippedRect(canvas, x + size * 0.08f, y + 3f, 8f, 9f, viewport.mapBounds, color("1A120C", 0.132f))
                                drawClippedRect(canvas, x + 4f, y + 3f, 5f, 11f, viewport.mapBounds, color("050604", 0.241f))
                                drawClippedRect(canvas, x + size - 9f, y + 2f, 5f, 14f, viewport.mapBounds, color("050604", 0.263f))
                                drawClippedRect(canvas, x + size * 0.30f, y + 5f, 8f, 2f, viewport.mapBounds, color("B69B6B", 0.149f))
                                drawClippedRect(canvas, x + size * 0.57f, y + 8f, 7f, 2f, viewport.mapBounds, color("8A7654", 0.149f))
                                drawClippedRect(canvas, x - size * 0.76f, y - 20f, size * 2.56f, 18f, viewport.mapBounds, color("050604", 0.232f))
                                drawClippedRect(canvas, x - size * 0.36f, y - 13f, size * 1.58f, 4f, viewport.mapBounds, color("8A7654", 0.148f))
                                drawClippedRect(canvas, x - size * 0.54f, y - 35f, 11f, 24f, viewport.mapBounds, color("050604", 0.168f))
                                drawClippedRect(canvas, x + size * 1.20f, y - 34f, 11f, 26f, viewport.mapBounds, color("050604", 0.168f))
                            }
                        }
                    }
                    if (horizontalPassage) {
                        drawClippedRect(canvas, x - 8f, y, size + 16f, 8f, viewport.mapBounds, color("050604", 0.38f))
                        drawClippedRect(canvas, x - 8f, y + size - 8f, size + 16f, 8f, viewport.mapBounds, color("050604", 0.38f))
                        drawClippedRect(canvas, x - 3f, y + 7f, size + 6f, 2f, viewport.mapBounds, color("7A6040", 0.20f))
                        drawClippedRect(canvas, x - 3f, y + size - 9f, size + 6f, 2f, viewport.mapBounds, color("7A6040", 0.20f))
                        drawClippedRect(canvas, x + size * 0.22f, y + 9f, 19f, 7f, viewport.mapBounds, color("050604", 0.236f))
                        drawClippedRect(canvas, x + size * 0.54f, y + size - 15f, 15f, 6f, viewport.mapBounds, color("050604", 0.214f))
                        drawClippedRect(canvas, x + size * 0.30f, y + size * 0.36f, 2f, 10f, viewport.mapBounds, color("8A7654", 0.121f))
                        if (eastFloor) {
                            if (wideEast) {
                                drawClippedRect(canvas, x + size - 13f, y - size * 0.42f, 13f, size * 1.84f, viewport.mapBounds, color("050604", 0.205f))
                                drawClippedRect(canvas, x + size - 18f, y - size * 0.24f, 18f, size * 1.48f, viewport.mapBounds, color("050604", 0.287f))
                            }
                            drawClippedRect(canvas, x + size - 10f, y + 4f, 10f, size - 8f, viewport.mapBounds, color("050604", 0.31f))
                            drawClippedRect(canvas, x + size - 5f, y + 8f, 2f, size - 16f, viewport.mapBounds, color("B69B6B", 0.19f))
                            if (wideEast) {
                                drawClippedRect(canvas, x + size - 5f, y - size * 0.02f, 4f, size * 1.04f, viewport.mapBounds, color("A8905E", 0.132f))
                                drawClippedRect(canvas, x + size - 12f, y + size * 0.72f, 9f, 8f, viewport.mapBounds, color("1A120C", 0.132f))
                                drawClippedRect(canvas, x + size - 17f, y + 4f, 14f, 5f, viewport.mapBounds, color("050604", 0.263f))
                                drawClippedRect(canvas, x + size - 15f, y + size - 9f, 11f, 5f, viewport.mapBounds, color("050604", 0.241f))
                                drawClippedRect(canvas, x + size - 4f, y + size * 0.34f, 2f, 8f, viewport.mapBounds, color("B69B6B", 0.149f))
                                drawClippedRect(canvas, x + size - 7f, y + size * 0.58f, 2f, 7f, viewport.mapBounds, color("8A7654", 0.149f))
                                drawClippedRect(canvas, x + size + 2f, y - size * 0.81f, 18f, size * 2.62f, viewport.mapBounds, color("050604", 0.232f))
                                drawClippedRect(canvas, x + size + 9f, y - size * 0.48f, 4f, size * 1.64f, viewport.mapBounds, color("8A7654", 0.148f))
                                drawClippedRect(canvas, x + size + 7f, y - size * 0.58f, 26f, 11f, viewport.mapBounds, color("050604", 0.168f))
                                drawClippedRect(canvas, x + size + 10f, y + size * 1.24f, 24f, 11f, viewport.mapBounds, color("050604", 0.168f))
                            }
                        }
                        if (westFloor) {
                            if (wideWest) {
                                drawClippedRect(canvas, x, y - size * 0.42f, 13f, size * 1.84f, viewport.mapBounds, color("050604", 0.205f))
                                drawClippedRect(canvas, x, y - size * 0.24f, 18f, size * 1.48f, viewport.mapBounds, color("050604", 0.287f))
                            }
                            drawClippedRect(canvas, x, y + 4f, 10f, size - 8f, viewport.mapBounds, color("050604", 0.31f))
                            drawClippedRect(canvas, x + 4f, y + 8f, 2f, size - 16f, viewport.mapBounds, color("B69B6B", 0.17f))
                            if (wideWest) {
                                drawClippedRect(canvas, x + 1f, y - size * 0.02f, 4f, size * 1.04f, viewport.mapBounds, color("A8905E", 0.132f))
                                drawClippedRect(canvas, x + 3f, y + size * 0.08f, 9f, 8f, viewport.mapBounds, color("1A120C", 0.132f))
                                drawClippedRect(canvas, x + 3f, y + 4f, 11f, 5f, viewport.mapBounds, color("050604", 0.241f))
                                drawClippedRect(canvas, x + 2f, y + size - 9f, 14f, 5f, viewport.mapBounds, color("050604", 0.263f))
                                drawClippedRect(canvas, x + 5f, y + size * 0.30f, 2f, 8f, viewport.mapBounds, color("B69B6B", 0.149f))
                                drawClippedRect(canvas, x + 8f, y + size * 0.57f, 2f, 7f, viewport.mapBounds, color("8A7654", 0.149f))
                                drawClippedRect(canvas, x - 20f, y - size * 0.76f, 18f, size * 2.56f, viewport.mapBounds, color("050604", 0.232f))
                                drawClippedRect(canvas, x - 13f, y - size * 0.36f, 4f, size * 1.58f, viewport.mapBounds, color("8A7654", 0.148f))
                                drawClippedRect(canvas, x - 35f, y - size * 0.54f, 24f, 11f, viewport.mapBounds, color("050604", 0.168f))
                                drawClippedRect(canvas, x - 34f, y + size * 1.20f, 26f, 11f, viewport.mapBounds, color("050604", 0.168f))
                            }
                        }
                    }
            }
        }

        private fun drawVisibleRoomCornerBreakup(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val clip = visibleRoomClip(frame) ?: return
            val cellSize = frame.viewport.cellSize.toFloat()
            if (clip.width < cellSize * 5f || clip.height < cellSize * 5f) {
                return
            }
            val left = clip.x.toFloat()
            val bottom = clip.y.toFloat()
            val right = clip.right.toFloat()
            val top = clip.top.toFloat()
            val width = right - left
            val height = top - bottom
            val chipWidth = cellSize * 1.72f
            val chipHeight = cellSize * 1.56f
            val lipWidth = cellSize * 0.64f
            val lipHeight = cellSize * 0.20f
            val dark = color("050604", 0.43f)
            val softDark = color("050604", 0.31f)
            val stoneLip = color("6F5A39", 0.13f)

            drawClippedRect(canvas, left, bottom, chipWidth, chipHeight, frame.viewport.mapBounds, dark)
            drawClippedRect(canvas, left + cellSize * 0.55f, bottom + chipHeight - lipHeight, lipWidth, lipHeight, frame.viewport.mapBounds, stoneLip)
            drawClippedRect(canvas, left, bottom + cellSize * 0.94f, cellSize * 0.44f, cellSize * 1.06f, frame.viewport.mapBounds, softDark)

            drawClippedRect(canvas, right - chipWidth, bottom, chipWidth, chipHeight, frame.viewport.mapBounds, color("050604", 0.39f))
            drawClippedRect(canvas, right - cellSize * 1.18f, bottom + chipHeight - lipHeight, lipWidth, lipHeight, frame.viewport.mapBounds, color("6F5A39", 0.12f))
            drawClippedRect(canvas, right - cellSize * 0.44f, bottom + cellSize * 0.86f, cellSize * 0.44f, cellSize * 1.12f, frame.viewport.mapBounds, softDark)

            drawClippedRect(canvas, left, top - chipHeight, chipWidth, chipHeight, frame.viewport.mapBounds, color("050604", 0.40f))
            drawClippedRect(canvas, left + cellSize * 0.58f, top - cellSize * 0.30f, lipWidth, lipHeight, frame.viewport.mapBounds, color("6F5A39", 0.12f))
            drawClippedRect(canvas, left, top - cellSize * 1.78f, cellSize * 0.42f, cellSize * 1.08f, frame.viewport.mapBounds, softDark)

            drawClippedRect(canvas, right - chipWidth, top - chipHeight, chipWidth, chipHeight, frame.viewport.mapBounds, color("050604", 0.43f))
            drawClippedRect(canvas, right - cellSize * 1.18f, top - cellSize * 0.30f, lipWidth, lipHeight, frame.viewport.mapBounds, stoneLip)
            drawClippedRect(canvas, right - cellSize * 0.48f, top - cellSize * 1.78f, cellSize * 0.48f, cellSize * 1.08f, frame.viewport.mapBounds, softDark)

            drawClippedRect(canvas, left, bottom + height * 0.38f, cellSize * 0.92f, cellSize * 1.55f, frame.viewport.mapBounds, color("050604", 0.36f))
            drawClippedRect(canvas, left + cellSize * 0.62f, bottom + height * 0.47f, cellSize * 0.92f, cellSize * 0.18f, frame.viewport.mapBounds, color("6F5A39", 0.112f))
            drawClippedRect(canvas, right - cellSize * 0.98f, bottom + height * 0.51f, cellSize * 0.98f, cellSize * 1.48f, frame.viewport.mapBounds, color("050604", 0.355f))
            drawClippedRect(canvas, right - cellSize * 1.44f, bottom + height * 0.62f, cellSize * 0.74f, cellSize * 0.16f, frame.viewport.mapBounds, color("6F5A39", 0.102f))
            drawClippedRect(canvas, left + width * 0.39f, top - cellSize * 0.82f, cellSize * 2.45f, cellSize * 0.82f, frame.viewport.mapBounds, color("050604", 0.305f))
            drawClippedRect(canvas, left + width * 0.44f, top - cellSize * 0.18f, cellSize * 1.34f, cellSize * 0.14f, frame.viewport.mapBounds, color("8A7654", 0.092f))
        }

        private fun drawVisibleRoomSilhouettePressure(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            clip: RectInt,
        ) {
            val cellSize = frame.viewport.cellSize.toFloat()
            val width = clip.width.toFloat()
            val height = clip.height.toFloat()
            if (width < cellSize * 5.5f || height < cellSize * 4.2f) {
                return
            }

            val left = clip.x.toFloat()
            val right = clip.right.toFloat()
            val bottom = clip.y.toFloat()
            val top = clip.top.toFloat()
            val playerCenterX =
                if (frame.viewport.containsTile(frame.model.playerTile)) {
                    val playerRect = frame.viewport.tileRect(frame.model.playerTile)
                    playerRect.x + playerRect.width / 2f
                } else {
                    left + width * 0.52f
                }
            val topBiteWidth = (width * 0.34f).coerceAtMost(cellSize * 5.4f).coerceAtLeast(cellSize * 2.4f)
            val topBiteMinX = left + cellSize * 0.86f
            val topBiteMaxX = right - topBiteWidth - cellSize * 0.62f
            val topBiteX =
                if (topBiteMaxX > topBiteMinX) {
                    (playerCenterX - topBiteWidth * 0.34f).coerceIn(topBiteMinX, topBiteMaxX)
                } else {
                    left + width * 0.24f
                }
            drawClippedRect(
                canvas,
                topBiteX,
                top - cellSize * 1.38f,
                topBiteWidth,
                cellSize * 1.02f,
                frame.viewport.mapBounds,
                color("050604", 0.480f),
            )
            drawClippedRect(
                canvas,
                topBiteX + topBiteWidth * 0.14f,
                top - cellSize * 0.32f,
                topBiteWidth * 0.52f,
                3f,
                frame.viewport.mapBounds,
                color("A8905E", 0.126f),
            )

            val lowerBiteWidth = (width * 0.28f).coerceAtMost(cellSize * 5.2f).coerceAtLeast(cellSize * 2.0f)
            val lowerBiteMinX = left + width * 0.38f
            val lowerBiteMaxX = right - lowerBiteWidth - cellSize * 0.56f
            val lowerBiteX =
                if (lowerBiteMaxX > lowerBiteMinX) {
                    (playerCenterX + lowerBiteWidth * 0.42f).coerceIn(lowerBiteMinX, lowerBiteMaxX)
                } else {
                    left + width * 0.48f
                }
            drawClippedRect(
                canvas,
                lowerBiteX,
                bottom + cellSize * 0.08f,
                lowerBiteWidth,
                cellSize * 0.82f,
                frame.viewport.mapBounds,
                color("050604", 0.400f),
            )
            drawClippedRect(
                canvas,
                lowerBiteX + lowerBiteWidth * 0.38f,
                bottom + cellSize * 0.44f,
                lowerBiteWidth * 0.34f,
                3f,
                frame.viewport.mapBounds,
                color("8A7654", 0.086f),
            )

            drawClippedRect(
                canvas,
                right - cellSize * 1.02f,
                bottom + height * 0.30f,
                cellSize * 1.02f,
                height * 0.43f,
                frame.viewport.mapBounds,
                color("050604", 0.360f),
            )
            drawClippedRect(
                canvas,
                right - cellSize * 1.10f,
                bottom + height * 0.49f,
                cellSize * 0.58f,
                3f,
                frame.viewport.mapBounds,
                color("A8905E", 0.092f),
            )
        }

        private fun drawVisibleRoomBoundaryCompression(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorRects =
                visibleFloorPoints(frame)
                    .asSequence()
                    .filter(frame.viewport::containsTile)
                    .map(frame.viewport::tileRect)
                    .toList()
            if (floorRects.isEmpty()) {
                return
            }
            val left = floorRects.minOf { rect -> rect.x }.toFloat()
            val right = floorRects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = floorRects.minOf { rect -> rect.y }.toFloat()
            val top = floorRects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val width = right - left
            val height = top - bottom
            val cellSize = frame.viewport.cellSize.toFloat()
            if (width < cellSize * 7f || height < cellSize * 5f) {
                return
            }

            canvas.drawRect(
                tileBounds(left + cellSize * 0.62f, top - cellSize * 0.46f, width - cellSize * 1.24f, cellSize * 0.44f),
                color("050604", 0.180f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 1.18f, bottom + cellSize * 0.10f, width - cellSize * 2.36f, cellSize * 0.32f),
                color("1D1710", 0.140f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 0.12f, bottom + cellSize * 0.92f, cellSize * 0.40f, height - cellSize * 1.84f),
                color("050604", 0.150f),
            )
            canvas.drawRect(
                tileBounds(right - cellSize * 0.52f, bottom + cellSize * 0.96f, cellSize * 0.40f, height - cellSize * 1.92f),
                color("050604", 0.142f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 0.34f, top - cellSize * 0.96f, cellSize * 1.12f, cellSize * 0.84f),
                color("050604", 0.210f),
            )
            canvas.drawRect(
                tileBounds(right - cellSize * 1.46f, top - cellSize * 0.90f, cellSize * 1.08f, cellSize * 0.78f),
                color("050604", 0.210f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 0.62f, top - cellSize * 0.32f, width * 0.30f, 2.5f),
                color("8A7654", 0.090f),
            )
            canvas.drawRect(
                tileBounds(right - width * 0.34f, top - cellSize * 0.36f, width * 0.26f, 2f),
                color("6F5A39", 0.080f),
            )
        }

        private fun drawVisibleRoomAsymmetricEdgeMass(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorRects =
                visibleFloorPoints(frame)
                    .asSequence()
                    .filter(frame.viewport::containsTile)
                    .map(frame.viewport::tileRect)
                    .toList()
            if (floorRects.isEmpty()) {
                return
            }
            val left = floorRects.minOf { rect -> rect.x }.toFloat()
            val right = floorRects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = floorRects.minOf { rect -> rect.y }.toFloat()
            val top = floorRects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val width = right - left
            val height = top - bottom
            val cellSize = frame.viewport.cellSize.toFloat()
            if (width < cellSize * 7f || height < cellSize * 5f) {
                return
            }

            canvas.drawRect(
                tileBounds(left + cellSize * 0.72f, top - cellSize * 1.34f, cellSize * 3.20f, cellSize * 1.00f),
                color("050604", 0.320f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 1.02f, top - cellSize * 0.72f, cellSize * 1.98f, 3f),
                color("8A7654", 0.138f),
            )
            canvas.drawRect(
                tileBounds(right - cellSize * 4.36f, bottom + cellSize * 0.48f, cellSize * 3.10f, cellSize * 0.85f),
                color("1A120C", 0.300f),
            )
            canvas.drawRect(
                tileBounds(right - cellSize * 3.68f, bottom + cellSize * 1.16f, cellSize * 1.64f, 2.5f),
                color("A8905E", 0.116f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 0.44f, bottom + height * 0.32f, cellSize * 1.30f, cellSize * 2.50f),
                color("050604", 0.300f),
            )
            canvas.drawRect(
                tileBounds(left + cellSize * 1.42f, bottom + height * 0.49f, 3f, cellSize * 1.32f),
                color("6F5A39", 0.104f),
            )
            canvas.drawRect(
                tileBounds(right - cellSize * 1.36f, bottom + height * 0.40f, cellSize * 1.08f, cellSize * 2.35f),
                color("050604", 0.270f),
            )
            canvas.drawRect(
                tileBounds(right - cellSize * 1.50f, bottom + height * 0.60f, 3f, cellSize * 1.04f),
                color("7D6A49", 0.092f),
            )
        }

        private fun drawVisibleRoomMacroStructuralPlates(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val floorRects =
                visibleFloorPoints(frame)
                    .asSequence()
                    .filter(frame.viewport::containsTile)
                    .map(frame.viewport::tileRect)
                    .toList()
            if (floorRects.isEmpty()) {
                return
            }
            val left = floorRects.minOf { rect -> rect.x }.toFloat()
            val right = floorRects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = floorRects.minOf { rect -> rect.y }.toFloat()
            val top = floorRects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val width = right - left
            val height = top - bottom
            val cellSize = frame.viewport.cellSize.toFloat()
            if (width < cellSize * 6f || height < cellSize * 5f) {
                return
            }

            drawClippedRect(
                canvas,
                left + cellSize * 0.62f,
                top - cellSize * 1.08f,
                width * 0.54f,
                cellSize * 0.90f,
                frame.viewport.mapBounds,
                color("050604", 0.365f),
            )
            drawClippedRect(
                canvas,
                left + cellSize * 1.08f,
                top - cellSize * 0.55f,
                width * 0.30f,
                3f,
                frame.viewport.mapBounds,
                color("8A7654", 0.142f),
            )
            drawClippedRect(
                canvas,
                left + width * 0.52f,
                bottom + cellSize * 0.08f,
                width * 0.40f,
                cellSize * 0.72f,
                frame.viewport.mapBounds,
                color("1A120C", 0.305f),
            )
            drawClippedRect(
                canvas,
                left + cellSize * 0.24f,
                bottom + height * 0.28f,
                cellSize * 0.74f,
                height * 0.46f,
                frame.viewport.mapBounds,
                color("050604", 0.285f),
            )
            drawClippedRect(
                canvas,
                right - cellSize * 0.92f,
                bottom + height * 0.44f,
                cellSize * 0.70f,
                height * 0.34f,
                frame.viewport.mapBounds,
                color("050604", 0.258f),
            )
        }

        private fun drawVisibleRoomRuntimeCornerApertureShelves(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            val cellSize = frame.viewport.cellSize.toFloat()
            if (cellSize < 40f) {
                return
            }
            val materialRects =
                frame.model.mapCellMaterials
                    .asSequence()
                    .filter { material ->
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            frame.viewport.containsTile(Point(material.x, material.y))
                    }.map { material -> frame.viewport.tileRect(Point(material.x, material.y)) }
                    .toList()
            if (materialRects.isEmpty()) {
                return
            }
            val left = materialRects.minOf { rect -> rect.x }.toFloat()
            val right = materialRects.maxOf { rect -> rect.x + rect.width }.toFloat()
            val bottom = materialRects.minOf { rect -> rect.y }.toFloat()
            val top = materialRects.maxOf { rect -> rect.y + rect.height }.toFloat()
            val width = right - left
            val height = top - bottom
            if (width < cellSize * 5f || height < cellSize * 4f) {
                return
            }

            drawClippedRect(
                canvas,
                left + cellSize * 0.04f,
                top - cellSize * 0.58f,
                width * 0.52f,
                cellSize * 0.54f,
                frame.viewport.mapBounds,
                color("050604", 0.332f),
            )
            drawClippedRect(
                canvas,
                left + cellSize * 0.48f,
                top - cellSize * 0.21f,
                width * 0.24f,
                3f,
                frame.viewport.mapBounds,
                color("8A7654", 0.126f),
            )
            drawClippedRect(
                canvas,
                left + cellSize * 0.08f,
                bottom + height * 0.28f,
                cellSize * 0.42f,
                height * 0.44f,
                frame.viewport.mapBounds,
                color("050604", 0.248f),
            )
            drawClippedRect(
                canvas,
                left + width * 0.42f,
                bottom + cellSize * 0.10f,
                width * 0.50f,
                cellSize * 0.50f,
                frame.viewport.mapBounds,
                color("050604", 0.296f),
            )
            drawClippedRect(
                canvas,
                left + width * 0.56f,
                bottom + cellSize * 0.42f,
                width * 0.26f,
                3f,
                frame.viewport.mapBounds,
                color("7D6A49", 0.102f),
            )
        }

        private fun drawVisibleRoomOuterShadows(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            visibleMaterialPoints: Set<Point>,
        ) {
            if (visibleMaterialPoints.isEmpty()) {
                return
            }
            val viewport = frame.viewport
            val cellSize = viewport.cellSize.toFloat()
            val horizontalSpread = cellSize * 0.70f
            val verticalSpread = cellSize * 0.70f
            frame.model.mapCellMaterials
                .asSequence()
                .filter { material ->
                    material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                        viewport.containsTile(Point(material.x, material.y))
                }.forEach { material ->
                    val point = Point(material.x, material.y)
                    val rect = viewport.tileRect(point)
                    val x = rect.x.toFloat()
                    val y = rect.y.toFloat()
                    val size = rect.width.toFloat()
                    if (Point(point.x, point.y + 1) !in visibleMaterialPoints) {
                        drawClippedRect(canvas, x - 2f, y + size - 4f, size + 4f, verticalSpread, viewport.mapBounds, color("050604", 0.36f))
                        drawClippedRect(canvas, x + 4f, y + size - 3f, size - 8f, 3f, viewport.mapBounds, color("6F5A39", 0.12f))
                    }
                    if (Point(point.x, point.y - 1) !in visibleMaterialPoints) {
                        drawClippedRect(canvas, x - 2f, y - verticalSpread + 4f, size + 4f, verticalSpread, viewport.mapBounds, color("050604", 0.34f))
                        drawClippedRect(canvas, x + 4f, y, size - 8f, 3f, viewport.mapBounds, color("6F5A39", 0.10f))
                    }
                    if (Point(point.x - 1, point.y) !in visibleMaterialPoints) {
                        drawClippedRect(canvas, x - horizontalSpread + 4f, y - 2f, horizontalSpread, size + 4f, viewport.mapBounds, color("050604", 0.32f))
                        drawClippedRect(canvas, x, y + 4f, 3f, size - 8f, viewport.mapBounds, color("6F5A39", 0.10f))
                    }
                    if (Point(point.x + 1, point.y) !in visibleMaterialPoints) {
                        drawClippedRect(canvas, x + size - 4f, y - 2f, horizontalSpread, size + 4f, viewport.mapBounds, color("050604", 0.32f))
                        drawClippedRect(canvas, x + size - 3f, y + 4f, 3f, size - 8f, viewport.mapBounds, color("6F5A39", 0.10f))
                    }
                }
        }

        private fun drawVisibleRoomContactShadows(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            frame.model.mapCellMaterials
                .asSequence()
                .filter { material ->
                    material.kind == TileMapCellMaterialKind.FLOOR &&
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                        frame.viewport.containsTile(Point(material.x, material.y))
                }.forEach { material ->
                    val rect = frame.viewport.tileRect(Point(material.x, material.y))
                    val x = rect.x.toFloat()
                    val y = rect.y.toFloat()
                    val size = rect.width.toFloat()
                    val variant = material.variant
                    if (material.northOcclusion) {
                        canvas.drawRect(tileBounds(x, y + size - 10f, size, 10f), color("050604", 0.20f))
                        canvas.drawRect(tileBounds(x + 4f, y + size - 13f, size - 8f, 3f), color("2D1808", 0.11f))
                    }
                    if (material.southOcclusion) {
                        canvas.drawRect(tileBounds(x, y, size, 8f), color("050604", 0.14f))
                    }
                    if (material.westOcclusion) {
                        canvas.drawRect(tileBounds(x, y, 8f, size), color("050604", 0.12f))
                    }
                    if (material.eastOcclusion) {
                        canvas.drawRect(tileBounds(x + size - 8f, y, 8f, size), color("050604", 0.12f))
                    }
                    if (material.northOcclusion && variant % 2 == 0) {
                        canvas.drawRect(tileBounds(x + 2f, y + size - 18f, size * 0.72f, 16f), color("050604", 0.30f))
                        canvas.drawRect(tileBounds(x + 8f, y + size - 14f, size * 0.42f, 4f), color("5D5440", 0.24f))
                    }
                    if (material.southOcclusion && variant % 5 == 0) {
                        canvas.drawRect(tileBounds(x + size * 0.22f, y + 2f, size * 0.64f, 13f), color("050604", 0.26f))
                        canvas.drawRect(tileBounds(x + size * 0.34f, y + 8f, size * 0.32f, 3f), color("7B8669", 0.16f))
                    }
                    if (material.westOcclusion && variant % 7 == 0) {
                        canvas.drawRect(tileBounds(x + 2f, y + size * 0.20f, 15f, size * 0.64f), color("050604", 0.27f))
                        canvas.drawRect(tileBounds(x + 7f, y + size * 0.42f, 5f, size * 0.22f), color("6F5A39", 0.17f))
                    }
                    if (material.eastOcclusion && variant % 11 == 0) {
                        canvas.drawRect(tileBounds(x + size - 17f, y + size * 0.18f, 15f, size * 0.66f), color("050604", 0.27f))
                        canvas.drawRect(tileBounds(x + size - 13f, y + size * 0.48f, 5f, size * 0.22f), color("6F5A39", 0.17f))
                    }
                    if ((material.northOcclusion || material.southOcclusion || material.westOcclusion || material.eastOcclusion) && variant % 3 == 0) {
                        val rubbleX = x + 4f + (variant % 6).toFloat()
                        val rubbleY = y + 5f + ((variant / 7) % 8).toFloat()
                        canvas.drawRect(tileBounds(rubbleX, rubbleY, 22f, 12f), color("050604", 0.24f))
                        canvas.drawRect(tileBounds(rubbleX + 3f, rubbleY + 3f, 8f, 5f), color("6F5A39", 0.22f))
                        canvas.drawRect(tileBounds(rubbleX + 13f, rubbleY + 6f, 6f, 4f), color("B69B6B", 0.12f))
                    }
                    if ((material.northOcclusion && material.westOcclusion) || (material.northOcclusion && material.eastOcclusion)) {
                        val cornerX = if (material.westOcclusion) x + 2f else x + size - 17f
                        canvas.drawRect(tileBounds(cornerX, y + size - 18f, 15f, 15f), color("050604", 0.30f))
                    }
                }
        }

        private fun drawVisibleRoomStoryDecals(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            frame.model.mapCellMaterials
                .asSequence()
                .filter { material ->
                    material.kind == TileMapCellMaterialKind.FLOOR &&
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                        frame.viewport.containsTile(Point(material.x, material.y))
                }.forEach { material ->
                    val rect = frame.viewport.tileRect(Point(material.x, material.y))
                    val x = rect.x.toFloat()
                    val y = rect.y.toFloat()
                    val variant = material.variant
                    if (variant % 23 == 0) {
                        canvas.drawRect(tileBounds(x + 7f, y + 11f, 16f, 2f), color("050604", 0.28f))
                        canvas.drawRect(tileBounds(x + 19f, y + 8f, 2f, 9f), color("050604", 0.22f))
                        canvas.drawRect(tileBounds(x + 10f, y + 15f, 11f, 1f), color("C49B61", 0.12f))
                    }
                    if (variant % 9 == 0) {
                        canvas.drawRect(tileBounds(x + 6f, y + 8f, 18f, 3f), color("050604", 0.30f))
                        canvas.drawRect(tileBounds(x + 9f, y + 13f, 13f, 2f), color("7C7152", 0.24f))
                        canvas.drawRect(tileBounds(x + 18f, y + 6f, 2f, 11f), color("050604", 0.22f))
                    }
                    if (variant % 15 == 0) {
                        canvas.drawRect(tileBounds(x + 8f, y + 9f, 17f, 8f), color("4B0B08", 0.28f))
                        canvas.drawRect(tileBounds(x + 12f, y + 16f, 12f, 4f), color("6E1310", 0.18f))
                        canvas.drawRect(tileBounds(x + 20f, y + 7f, 4f, 3f), color("260504", 0.18f))
                    }
                    if (variant % 31 == 0) {
                        canvas.drawRect(tileBounds(x + 8f, y + 7f, 13f, 6f), color("40110D", 0.22f))
                        canvas.drawRect(tileBounds(x + 15f, y + 12f, 7f, 4f), color("5C1A10", 0.14f))
                    }
                    if (variant % 37 == 0) {
                        canvas.drawRect(tileBounds(x + 7f, y + 19f, 5f, 3f), color("B69B6B", 0.18f))
                        canvas.drawRect(tileBounds(x + 15f, y + 16f, 4f, 3f), color("6F5A39", 0.16f))
                        canvas.drawRect(tileBounds(x + 21f, y + 21f, 3f, 2f), color("C49B61", 0.10f))
                    }
                    if (variant % 41 == 0) {
                        canvas.drawRect(tileBounds(x + 5f, y + 6f, 6f, 5f), color("5D5440", 0.30f))
                        canvas.drawRect(tileBounds(x + 13f, y + 10f, 5f, 4f), color("3E3528", 0.28f))
                        canvas.drawRect(tileBounds(x + 20f, y + 8f, 4f, 5f), color("B69B6B", 0.14f))
                        canvas.drawRect(tileBounds(x + 9f, y + 17f, 11f, 2f), color("050604", 0.20f))
                    }
                }
        }

        private fun drawMapStageShadowVeil(
            canvas: TileCanvas,
            bounds: GameShellBounds,
        ) {
            val left = bounds.x + 28f
            val bottom = bounds.y + 28f
            val width = (bounds.width - 56f).coerceAtLeast(0f)
            val height = (bounds.height - 56f).coerceAtLeast(0f)
            if (width <= 0f || height <= 0f) {
                return
            }
            canvas.drawRect(tileBounds(left, bottom, width, height), color("050604", 0.20f))
            canvas.drawRect(
                tileBounds(left + width * 0.28f, bottom + height * 0.20f, width * 0.52f, height * 0.62f),
                color("1E160B", 0.045f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.05f, bottom + height * 0.58f, width * 0.34f, height * 0.30f),
                color("020303", 0.138f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.62f, bottom + height * 0.14f, width * 0.28f, height * 0.24f),
                color("020303", 0.126f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.58f, bottom + height * 0.32f, width * 0.22f, height * 0.12f),
                color("050604", 0.074f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.12f, bottom + height * 0.69f, width * 0.19f, height * 0.018f),
                color("6F5A39", 0.048f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.70f, bottom + height * 0.29f, width * 0.16f, height * 0.018f),
                color("6F5A39", 0.044f),
            )
            canvas.drawRect(
                tileBounds(left + width * 0.24f, bottom + height * 0.49f, width * 0.18f, height * 0.17f),
                color("050604", 0.082f),
            )
        }

        private fun drawMapStageStoneTexture(
            canvas: TileCanvas,
            bounds: GameShellBounds,
        ) {
            val tileSize = 96f
            val left = bounds.x + 28f
            val bottom = bounds.y + 28f
            val right = bounds.right - 28f
            val top = bounds.top - 28f
            var row = 0
            var y = bottom
            while (y < top) {
                var column = 0
                var x = left
                while (x < right) {
                    val width = minOf(tileSize, right - x)
                    val height = minOf(tileSize, top - y)
                    val variant = row * 17 + column * 31
                    val fillAlpha = if ((row + column) % 3 == 0) 0.012f else 0.008f
                    canvas.drawRect(tileBounds(x + 1f, y + 1f, width - 2f, height - 2f), color("5D4A31", fillAlpha))
                    if (variant % 5 == 0) {
                        canvas.drawRect(tileBounds(x + 10f, y + height - 12f, (width - 20f).coerceAtLeast(2f), 1f), color("B8873E", 0.012f))
                    }
                    if (variant % 7 == 0) {
                        canvas.drawRect(tileBounds(x + width - 16f, y + 10f, 1f, (height - 20f).coerceAtLeast(2f)), color("050604", 0.016f))
                    }
                    if (variant % 11 == 0) {
                        canvas.drawRect(tileBounds(x + 18f, y + 22f, 20f, 1f), color("B69B6B", 0.016f))
                        canvas.drawRect(tileBounds(x + 28f, y + 14f, 1f, 16f), color("050604", 0.016f))
                    }
                    x += tileSize
                    column += 1
                }
                y += tileSize
                row += 1
            }
        }

        private fun drawMapStageEdgeVignette(
            canvas: TileCanvas,
            bounds: GameShellBounds,
        ) {
            val ringWidth = 5f
            repeat(8) { index ->
                val inset = index * ringWidth
                val alpha = 0.65f * (1f - index / 8f) * 0.34f
                val width = (bounds.width - inset * 2f).coerceAtLeast(0f)
                val height = (bounds.height - inset * 2f).coerceAtLeast(0f)
                if (width <= 0f || height <= 0f) {
                    return@repeat
                }
                val left = bounds.x + inset
                val bottom = bounds.y + inset
                val right = left + width - ringWidth
                val top = bottom + height - ringWidth
                canvas.drawRect(tileBounds(left, bottom, ringWidth, height), color("1A0E04", alpha))
                canvas.drawRect(tileBounds(right, bottom, ringWidth, height), color("1A0E04", alpha))
                canvas.drawRect(tileBounds(left, bottom, width, ringWidth), color("1A0E04", alpha))
                canvas.drawRect(tileBounds(left, top, width, ringWidth), color("1A0E04", alpha))
            }
        }

        private fun drawPlayerWarmLight(
            canvas: TileCanvas,
            playerTile: Point,
            viewport: TileMapViewport,
            visibleMaterialPoints: Set<Point>,
            clip: RectInt,
        ) {
            if (!viewport.containsTile(playerTile)) {
                return
            }
            val rect = viewport.tileRect(playerTile)
            val centerX = rect.x + rect.width / 2f
            val centerY = rect.y + rect.height / 2f
            val cellSize = viewport.cellSize.toFloat()
            drawFocalWarmStoneDropout(canvas, centerX, centerY, cellSize, clip, alphaScale = 1f)
            drawClippedCenteredRect(canvas, centerX, centerY, cellSize * 6.20f, cellSize * 4.60f, clip, color("D99A2B", 0.012f))
            drawClippedCenteredRect(canvas, centerX, centerY, cellSize * 4.85f, cellSize * 3.45f, clip, color("E28A2B", 0.024f))
            drawClippedCenteredRect(canvas, centerX, centerY, cellSize * 3.70f, cellSize * 2.625f, clip, color("D99A2B", 0.058f))
            drawVisibleTileGlow(canvas, playerTile, viewport, visibleMaterialPoints, radius = 3, maxAlpha = 0.13f)
        }

        private fun drawTorchLightBlooms(
            canvas: TileCanvas,
            frame: MapRenderFrame,
            visibleMaterialPoints: Set<Point>,
            clip: RectInt,
        ) {
            visibleTorchWallTiles(frame)
                .forEachIndexed { index, tile ->
                    val maxAlpha = if (index < 4) 0.080f else 0.052f
                    drawVisibleTileGlow(canvas, tile, frame.viewport, visibleMaterialPoints, radius = 3, maxAlpha = maxAlpha)
                    drawTorchWarmPool(canvas, tile, frame.viewport, clip, maxAlpha = if (index < 4) 0.046f else 0.032f)
                }
        }

        private fun drawTorchWarmPool(
            canvas: TileCanvas,
            tile: Point,
            viewport: TileMapViewport,
            clip: RectInt,
            maxAlpha: Float,
        ) {
            if (!viewport.containsTile(tile)) {
                return
            }
            val rect = viewport.tileRect(tile)
            val centerX = rect.x + rect.width / 2f
            val centerY = rect.y + rect.height / 2f
            val cellSize = viewport.cellSize.toFloat()
            drawFocalWarmStoneDropout(canvas, centerX, centerY, cellSize, clip, alphaScale = 0.75f)
            drawClippedCenteredRect(canvas, centerX, centerY, cellSize * 4.20f, cellSize * 2.55f, clip, color("D99A2B", maxAlpha * 0.24f))
            drawClippedCenteredRect(canvas, centerX, centerY, cellSize * 3.25f, cellSize * 2.00f, clip, color("E28A2B", maxAlpha * 0.42f))
            drawClippedCenteredRect(canvas, centerX, centerY, cellSize * 2.55f, cellSize * 1.65f, clip, color("D99A2B", maxAlpha))
        }

        private fun drawFocalWarmStoneDropout(
            canvas: TileCanvas,
            centerX: Float,
            centerY: Float,
            cellSize: Float,
            clip: RectInt,
            alphaScale: Float,
        ) {
            drawClippedCenteredRect(
                canvas,
                centerX - cellSize * 0.10f,
                centerY + cellSize * 0.02f,
                cellSize * 2.82f,
                cellSize * 1.36f,
                clip,
                color("6E6348", 0.064f * alphaScale),
            )
            drawClippedCenteredRect(
                canvas,
                centerX + cellSize * 0.18f,
                centerY - cellSize * 0.28f,
                cellSize * 2.18f,
                4f,
                clip,
                color("C49B61", 0.070f * alphaScale),
            )
            drawClippedCenteredRect(
                canvas,
                centerX - cellSize * 0.52f,
                centerY + cellSize * 0.30f,
                4f,
                cellSize * 1.18f,
                clip,
                color("050604", 0.070f * alphaScale),
            )
            drawClippedCenteredRect(
                canvas,
                centerX - cellSize * 0.18f,
                centerY - cellSize * 0.54f,
                cellSize * 2.46f,
                8f,
                clip,
                color("050607", 0.117f * alphaScale),
            )
            drawClippedCenteredRect(
                canvas,
                centerX + cellSize * 0.20f,
                centerY + cellSize * 0.43f,
                cellSize * 1.42f,
                2f,
                clip,
                color("8A8468", 0.067f * alphaScale),
            )
        }

        private fun drawTorchFixtures(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            visibleTorchWallTiles(frame).forEach { tile ->
                val rect = frame.viewport.tileRect(tile)
                val centerX = rect.x.toFloat() + rect.width * 0.50f
                val centerY = rect.y.toFloat() + rect.height * 0.50f
                canvas.drawRect(tileBounds(centerX - 9f, centerY - 7f, 18f, 16f), color("C66A21", 0.12f))
                canvas.drawRect(tileBounds(centerX - 7f, centerY - 2f, 14f, 4f), color("050604", 0.58f))
                canvas.drawRect(tileBounds(centerX - 2f, centerY - 6f, 4f, 12f), color("E28A2B", 0.62f))
                canvas.drawRect(tileBounds(centerX - 1f, centerY - 3f, 2f, 7f), color("FFE18A", 0.72f))
            }
        }

        private fun visibleTorchWallTiles(frame: MapRenderFrame): List<Point> {
            val visibleFloorPoints = visibleFloorPoints(frame)
            if (visibleFloorPoints.isEmpty()) {
                return emptyList()
            }
            val wallCandidates =
                frame.model.mapCellMaterials
                    .asSequence()
                    .filter { material ->
                        material.kind == TileMapCellMaterialKind.WALL &&
                            material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            frame.viewport.containsTile(Point(material.x, material.y)) &&
                            hasAdjacentVisibleFloor(material, visibleFloorPoints)
                    }.toList()
            val preferred = wallCandidates.filter { material -> material.variant % 5 == 0 || material.variant % 7 == 0 || material.variant % 17 == 0 }
            return (preferred.ifEmpty { wallCandidates })
                .sortedWith(
                    compareBy<TileMapCellMaterialModel> { material ->
                        abs(material.x - frame.model.playerTile.x) + abs(material.y - frame.model.playerTile.y)
                    }.thenBy { material -> material.variant % 97 }
                        .thenBy { material -> material.y }
                        .thenBy { material -> material.x },
                ).take(4)
                .map { material -> Point(material.x, material.y) }
        }

        private fun visibleFloorPoints(frame: MapRenderFrame): Set<Point> =
            frame.model.mapCellMaterials
                .asSequence()
                .filter { material ->
                    material.kind == TileMapCellMaterialKind.FLOOR &&
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE
                }.map { material -> Point(material.x, material.y) }
                .toSet()

        private fun visibleMaterialPoints(frame: MapRenderFrame): Set<Point> =
            frame.model.mapCellMaterials
                .asSequence()
                .filter { material ->
                    material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                        frame.viewport.containsTile(Point(material.x, material.y))
                }.map { material -> Point(material.x, material.y) }
                .toSet()

        private fun visibleRoomClip(frame: MapRenderFrame): RectInt? {
            val rects =
                frame.model.mapCellMaterials
                    .asSequence()
                    .filter { material ->
                        material.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.VISIBLE &&
                            frame.viewport.containsTile(Point(material.x, material.y))
                    }.map { material -> frame.viewport.tileRect(Point(material.x, material.y)) }
                    .toList()
            if (rects.isEmpty()) {
                return null
            }
            val left = rects.minOf { rect -> rect.x }
            val right = rects.maxOf { rect -> rect.x + rect.width }
            val bottom = rects.minOf { rect -> rect.y }
            val top = rects.maxOf { rect -> rect.y + rect.height }
            return RectInt(left, bottom, right - left, top - bottom)
        }

        private fun hasAdjacentVisibleFloor(
            material: TileMapCellMaterialModel,
            floorPoints: Set<Point>,
        ): Boolean =
            Point(material.x, material.y + 1) in floorPoints ||
                Point(material.x, material.y - 1) in floorPoints ||
                Point(material.x - 1, material.y) in floorPoints ||
                Point(material.x + 1, material.y) in floorPoints

        private fun drawTileGlow(
            canvas: TileCanvas,
            center: Point,
            viewport: TileMapViewport,
            radius: Int,
            maxAlpha: Float,
        ) {
            drawTileGlowInTiles(canvas, center, viewport, allowedTiles = null, radius = radius, maxAlpha = maxAlpha)
        }

        private fun drawVisibleTileGlow(
            canvas: TileCanvas,
            center: Point,
            viewport: TileMapViewport,
            visibleMaterialPoints: Set<Point>,
            radius: Int,
            maxAlpha: Float,
        ) {
            drawTileGlowInTiles(canvas, center, viewport, allowedTiles = visibleMaterialPoints, radius = radius, maxAlpha = maxAlpha)
        }

        private fun drawTileGlowInTiles(
            canvas: TileCanvas,
            center: Point,
            viewport: TileMapViewport,
            allowedTiles: Set<Point>?,
            radius: Int,
            maxAlpha: Float,
        ) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val distance = maxOf(abs(dx), abs(dy))
                    if (distance > radius) {
                        continue
                    }
                    val tile = Point(center.x + dx, center.y + dy)
                    if (!viewport.containsTile(tile)) {
                        continue
                    }
                    if (allowedTiles != null && tile !in allowedTiles) {
                        continue
                    }
                    val t = 1f - distance.toFloat() / (radius + 1).toFloat()
                    val rect = viewport.tileRect(tile)
                    canvas.drawRect(
                        tileBounds(rect.x + 1f, rect.y + 1f, rect.width - 2f, rect.height - 2f),
                        color("E28A2B", maxAlpha * t * t),
                    )
                }
            }
        }

        private fun drawMapStageInnerFeather(
            canvas: TileCanvas,
            bounds: GameShellBounds,
        ) {
            val thickness = 12f
            val color = color("8A6A35", 0.12f)
            canvas.drawRect(tileBounds(bounds.x, bounds.y, thickness, bounds.height), color)
            canvas.drawRect(tileBounds(bounds.right - thickness, bounds.y, thickness, bounds.height), color)
            canvas.drawRect(tileBounds(bounds.x, bounds.y, bounds.width, thickness), color)
            canvas.drawRect(tileBounds(bounds.x, bounds.top - thickness, bounds.width, thickness), color)
        }

        private fun clippedBounds(
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            clip: RectInt,
        ): TileFloatBounds? {
            val clippedX = x.coerceAtLeast(clip.x.toFloat())
            val clippedY = y.coerceAtLeast(clip.y.toFloat())
            val clippedRight = (x + width).coerceAtMost(clip.right.toFloat())
            val clippedTop = (y + height).coerceAtMost(clip.top.toFloat())
            val clippedWidth = clippedRight - clippedX
            val clippedHeight = clippedTop - clippedY
            return if (clippedWidth <= 0f || clippedHeight <= 0f) {
                null
            } else {
                tileBounds(clippedX, clippedY, clippedWidth, clippedHeight)
            }
        }

        private fun drawClippedCenteredRect(
            canvas: TileCanvas,
            centerX: Float,
            centerY: Float,
            width: Float,
            height: Float,
            clip: RectInt,
            color: Color,
        ) {
            val bounds =
                clippedBounds(
                    x = centerX - width / 2f,
                    y = centerY - height / 2f,
                    width = width,
                    height = height,
                    clip = clip,
            ) ?: return
            canvas.drawRect(bounds, color)
        }

        private fun drawClippedRect(
            canvas: TileCanvas,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            clip: RectInt,
            color: Color,
        ) {
            val bounds = clippedBounds(x = x, y = y, width = width, height = height, clip = clip) ?: return
            canvas.drawRect(bounds, color)
        }

        internal fun footprintDimensions(footprint: String): Pair<Float, Float> =
            when (footprint) {
                "overlay", "ui" -> 1f to 1f
                else -> {
                    val parts = footprint.split("x")
                    require(parts.size == 2) { "Unsupported footprint '$footprint'." }
                    val width = parts[0].toFloatOrNull()
                    val height = parts[1].toFloatOrNull()
                    require(width != null && height != null && width > 0f && height > 0f) {
                        "Unsupported footprint '$footprint'."
                    }
                    width to height
                }
            }

        internal fun textApproximationBounds(
            style: TileTextStyle,
            text: String,
            x: Float,
            y: Float,
        ): IntArray {
            val height = TileTextMetrics.approximateLineHeight(style).roundToInt()
            return intArrayOf(
                x.roundToInt(),
                y.roundToInt(),
                maxOf(1, TileTextMetrics.approximateTextWidth(text, style).roundToInt()),
                height,
            )
        }

        internal fun layoutMetrics(
            mapWidth: Int,
            mapHeight: Int,
            cellWidth: Float,
            cellHeight: Float,
            shellWorldWidth: Float = UiDesignTokens.fixed.shellPreferredWorldWidth,
            shellWorldHeight: Float = UiDesignTokens.fixed.shellPreferredWorldHeight,
        ): TileLayoutMetrics {
            return InfoSurfaceLayoutSolver.resolveMetrics(
                layout = InfoSurfaceLayout.MapDominant,
                request =
                    InfoSurfaceLayoutRequest(
                        mapWidth = mapWidth,
                        mapHeight = mapHeight,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        uiRows = uiRows,
                        shellWorldWidth = shellWorldWidth,
                        shellWorldHeight = shellWorldHeight,
                    ),
            )
        }

        private fun packSummaryLines(
            summaryText: String,
            maxWidth: Float,
            maxLines: Int,
        ): List<String> {
            if (summaryText.isBlank()) {
                return emptyList()
            }
            val segments = summaryText.split("  ").filter { it.isNotBlank() }
            if (segments.isEmpty()) {
                return listOf(TileTextMetrics.truncateTextToWidth(summaryText, maxWidth, TileTextStyle.SMALL))
            }
            val lines = mutableListOf<String>()
            var current = ""
            segments.forEach { rawSegment ->
                val segment = TileTextMetrics.truncateTextToWidth(rawSegment, maxWidth, TileTextStyle.SMALL)
                val candidate = if (current.isBlank()) segment else "$current  $segment"
                if (TileTextMetrics.approximateTextWidth(candidate, TileTextStyle.SMALL) <= maxWidth) {
                    current = candidate
                } else {
                    if (current.isNotBlank()) {
                        lines += current
                    } else {
                        lines += segment
                    }
                    current = segment
                }
            }
            if (current.isNotBlank()) {
                lines += current
            }
            return when {
                lines.size <= maxLines -> lines
                maxLines <= 1 -> listOf(TileTextMetrics.truncateTextToWidth(lines.joinToString("  "), maxWidth, TileTextStyle.SMALL))
                else ->
                    lines.take(maxLines - 1) +
                        TileTextMetrics.truncateTextToWidth(lines.drop(maxLines - 1).joinToString("  "), maxWidth, TileTextStyle.SMALL)
            }
        }

        private fun tone(tone: TileTextTone): Color =
            when (tone) {
                TileTextTone.GOLD -> UiDesignTokens.color.quality.rare.color()
                TileTextTone.WHITE -> UiDesignTokens.color.text.primary.color()
                TileTextTone.LIGHT_GRAY -> UiDesignTokens.color.text.secondary.color()
                TileTextTone.CYAN -> UiDesignTokens.color.focus.ring.color()
                TileTextTone.GRAY -> UiDesignTokens.color.text.disabled.color()
                TileTextTone.GREEN -> UiDesignTokens.color.status.badge.stack.color()
                TileTextTone.RED -> UiDesignTokens.color.status.badge.turns.color()
                TileTextTone.BLUE -> UiDesignTokens.color.quality.magic.color()
                TileTextTone.MAGENTA -> UiDesignTokens.color.telegraph.lethal.color()
            }

        private fun talentTone(token: TalentTreeNodeToneToken): Color =
            when (token) {
                TalentTreeNodeToneToken.TALENT_LOCKED -> UiDesignTokens.color.talent.locked.color()
                TalentTreeNodeToneToken.TALENT_LEARNABLE -> UiDesignTokens.color.talent.learnable.color()
                TalentTreeNodeToneToken.TALENT_RESERVE -> UiDesignTokens.color.talent.reserve.color()
                TalentTreeNodeToneToken.TALENT_ACTIVE -> UiDesignTokens.color.talent.active.color()
            }

        private fun talentAssignRowNameColor(
            token: TalentTreeNodeToneToken,
            focused: Boolean,
        ): Color =
            when {
                focused -> talentTone(token)
                token == TalentTreeNodeToneToken.TALENT_LOCKED -> tone(TileTextTone.GRAY)
                token == TalentTreeNodeToneToken.TALENT_LEARNABLE -> talentTone(token)
                else -> talentAssignBodyText()
            }

        private fun talentAssignPanelFill(): Color = color("090604", 0.98f)

        private fun talentAssignFrameGold(): Color = color("9F7B3C", 0.86f)

        private fun talentAssignEmberGold(): Color = color("F0A34A")

        private fun talentAssignBodyText(): Color = color("C9C2B4")

        private fun previewTone(token: TalentPreviewToneToken): Color =
            when (token) {
                TalentPreviewToneToken.PRIMARY -> tone(TileTextTone.WHITE)
                TalentPreviewToneToken.SECONDARY -> talentAssignBodyText()
                TalentPreviewToneToken.POSITIVE -> color("48BFE3")
                TalentPreviewToneToken.WARNING -> color("D99A2B")
                TalentPreviewToneToken.LOCKED -> color("59616C")
            }

        private fun specialAccentColor(accent: com.ktome.client.ui.item.SpecialAccentTokenId): Color =
            when (accent) {
                com.ktome.client.ui.item.SpecialAccentTokenId.UNIQUE -> UiDesignTokens.color.accent.unique.color()
                com.ktome.client.ui.item.SpecialAccentTokenId.ARTIFACT -> UiDesignTokens.color.accent.artifact.color()
            }

        private fun withAlpha(
            source: Color,
            alpha: Float,
        ): Color = Color(source).also { color -> color.a = alpha }

        internal fun color(
            hex: String,
            alpha: Float = 1f,
        ): Color = Color.valueOf(hex).also { it.a = alpha }
    }
}
