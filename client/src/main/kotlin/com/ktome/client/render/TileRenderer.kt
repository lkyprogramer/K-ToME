package com.ktome.client.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.ktome.client.assets.ClientTextureRepository
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
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.i18n.Localizer
import kotlin.math.roundToInt

internal enum class TileTextStyle {
    TITLE,
    UI,
    SMALL,
}

internal enum class TileLayerFlushReason {
    BACKGROUND,
    MAP_TERRAIN_BASE,
    MAP_PROPS_AND_DECALS,
    MAP_SPRITE_OVERLAYS_AND_TELEGRAPHS,
    MAP_ACTORS,
    MAP_FOG_VEILS,
    MAP_GROUND_LOOT_MARKERS,
    MAP_PLAYER_INDICATOR,
    MAP_TARGETING_HIGHLIGHTS,
    MAP_ACTIVE_CURSOR,
    MAP_COMBAT_FEEDBACK,
    MAP_WARM_OVERLAY,
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

internal data class TileAssetDraw(
    val asset: ResolvedVisualAsset,
    val bounds: TileFloatBounds,
    val alpha: Float = 1f,
    val tintColorHex: String? = null,
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
) {
    drawAsset(
        TileAssetDraw(
            asset = asset,
            bounds = bounds,
            alpha = alpha,
            tintColorHex = tintColorHex,
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
            batch.draw(texture, draw.bounds.x, draw.bounds.y, draw.bounds.width, draw.bounds.height)
            batch.color = previous
            hasPendingDraw = true
        }

        override fun drawText(draw: TileTextDraw) {
            val font =
                when (draw.style) {
                    TileTextStyle.TITLE -> titleFont
                    TileTextStyle.UI -> uiFont
                    TileTextStyle.SMALL -> smallFont
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
            renderWarmMapOverlay(canvas, mapFrame)
            canvas.flushLayer(TileLayerFlushReason.MAP_WARM_OVERLAY)
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
                frame.layerPlan.terrainBase.forEach { placement -> drawPlacement(canvas, placement, viewport) }
                canvas.flushLayer(TileLayerFlushReason.MAP_TERRAIN_BASE)
                frame.layerPlan.propsAndDecals.forEach { placement -> drawPlacement(canvas, placement, viewport) }
                canvas.flushLayer(TileLayerFlushReason.MAP_PROPS_AND_DECALS)
                frame.layerPlan.spriteOverlaysAndTelegraphs.forEach { placement -> drawPlacement(canvas, placement, viewport) }
                canvas.flushLayer(TileLayerFlushReason.MAP_SPRITE_OVERLAYS_AND_TELEGRAPHS)
                frame.layerPlan.actors.forEach { placement -> drawPlacement(canvas, placement, viewport) }
                canvas.flushLayer(TileLayerFlushReason.MAP_ACTORS)
                frame.layerPlan.playerIndicators.forEach { indicator ->
                    if (viewport.containsTile(indicator.tile)) {
                        val rect = viewport.tileRect(indicator.tile)
                        drawRectOutline(
                            canvas = canvas,
                            x = rect.x.toFloat() + 3f,
                            y = rect.y.toFloat() + 3f,
                            width = rect.width - 6f,
                            height = rect.height - 6f,
                            stroke = 2f,
                            color = color("D99A2B", 0.94f),
                        )
                    }
                }
                canvas.flushLayer(TileLayerFlushReason.MAP_PLAYER_INDICATOR)
                drawGroundLootMarkers(canvas, frame.layerPlan.groundLootMarkers, viewport)
                canvas.flushLayer(TileLayerFlushReason.MAP_GROUND_LOOT_MARKERS)
                drawFogOverlays(canvas, frame.layerPlan.fogVeils, viewport)
                canvas.flushLayer(TileLayerFlushReason.MAP_FOG_VEILS)
                drawTargetHighlights(canvas, frame.layerPlan.targetHighlights, viewport)
                canvas.flushLayer(TileLayerFlushReason.MAP_TARGETING_HIGHLIGHTS)
                frame.layerPlan.activeCursor?.let { cursor ->
                    drawCursor(
                        canvas = canvas,
                        tile = cursor.tile,
                        viewport = viewport,
                        color =
                            if (cursor.mode == TileViewportFocusMode.TARGETING) {
                                targetCursorColor(cursor.state)
                            } else {
                                UiDesignTokens.color.focus.ring.color()
                            },
                    )
                }
                canvas.flushLayer(TileLayerFlushReason.MAP_ACTIVE_CURSOR)
                drawCombatFeedback(canvas, frame.layerPlan.combatFeedback, viewport)
                canvas.flushLayer(TileLayerFlushReason.MAP_COMBAT_FEEDBACK)
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

        private fun drawTargetHighlights(
            canvas: TileCanvas,
            highlights: List<TileTargetHighlightModel>,
            viewport: TileMapViewport,
        ) {
            highlights.forEach { highlight ->
                if (!viewport.containsTile(highlight.tile)) {
                    return@forEach
                }
                val rect = viewport.tileRect(highlight.tile)
                val inset = 4f
                val bounds =
                    tileBounds(
                        x = rect.x.toFloat() + inset,
                        y = rect.y.toFloat() + inset,
                        width = (rect.width - inset * 2f).coerceAtLeast(1f),
                        height = (rect.height - inset * 2f).coerceAtLeast(1f),
                    )
                canvas.drawRect(bounds, targetHighlightFill(highlight.state))
                drawRectOutline(
                    canvas = canvas,
                    x = bounds.x,
                    y = bounds.y,
                    width = bounds.width,
                    height = bounds.height,
                    stroke = 1f,
                    color = targetHighlightBorder(highlight.state),
                )
            }
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

        private fun drawPlacement(
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
            val anchorX = rect.x + viewport.cellSize * placement.asset.entry.pivotX.toFloat()
            val anchorY = rect.y + viewport.cellSize * placement.asset.entry.pivotY.toFloat()
            val drawX = anchorX - width * placement.asset.entry.pivotX.toFloat()
            val drawY = anchorY - height * placement.asset.entry.pivotY.toFloat()
            canvas.drawAsset(placement.asset, tileBounds(drawX, drawY, width, height), placement.alpha, placement.tintColorHex)
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
                canvas.drawRect(tileBounds(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat()), color("1A0E04", fog.alpha))
            }
        }

        private fun renderWarmMapOverlay(
            canvas: TileCanvas,
            frame: MapRenderFrame,
        ) {
            drawMapStageEdgeVignette(canvas, frame.layout.demoShell.mapStage)
            drawPlayerWarmLight(canvas, frame.model.playerTile, frame.viewport)
            drawMapStageInnerFeather(canvas, frame.layout.demoShell.mapStage)
        }

        private fun drawMapStageEdgeVignette(
            canvas: TileCanvas,
            bounds: GameShellBounds,
        ) {
            val ringWidth = 5f
            repeat(8) { index ->
                val inset = index * ringWidth
                val alpha = 0.65f * (1f - index / 8f) * 0.22f
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
        ) {
            if (!viewport.containsTile(playerTile)) {
                return
            }
            val rect = viewport.tileRect(playerTile)
            val centerX = rect.x + rect.width / 2f
            val centerY = rect.y + rect.height / 2f
            val radius = viewport.cellSize * 8f
            repeat(8) { index ->
                val t = (8 - index) / 8f
                val halfSide = radius * t
                val alpha = 0.006f + 0.006f * (1f - t)
                val bounds =
                    clippedBounds(
                        x = centerX - halfSide,
                        y = centerY - halfSide,
                        width = halfSide * 2f,
                        height = halfSide * 2f,
                        clip = viewport.mapBounds,
                    ) ?: return@repeat
                canvas.drawRect(bounds, color("D99A2B", alpha))
            }
        }

        private fun drawMapStageInnerFeather(
            canvas: TileCanvas,
            bounds: GameShellBounds,
        ) {
            val thickness = 12f
            val color = color("D99A2B", 0.18f)
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

        internal fun color(
            hex: String,
            alpha: Float = 1f,
        ): Color = Color.valueOf(hex).also { it.a = alpha }
    }
}
