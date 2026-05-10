package com.ktome.client.render

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
import com.ktome.client.render.layout.InfoSurfaceLayout
import com.ktome.client.render.layout.InfoSurfaceLayoutRequest
import com.ktome.client.render.layout.InfoSurfaceLayoutSolver
import com.ktome.client.render.layout.RectInt
import com.ktome.client.ui.layout.PaneFocusAnchor
import com.ktome.client.ui.status.StatusHudRenderer
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.map.Point
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.i18n.Localizer
import kotlin.math.roundToInt

internal enum class TileTextStyle {
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
    MAP_ACTIVE_CURSOR,
    MAP_COMBAT_FEEDBACK,
    SHELL_PANES,
    BOTTOM_HUD,
    OVERLAY_PASSIVE_TOOLTIP,
    OVERLAY_TOAST,
    OVERLAY_MODAL_BACKDROP,
    OVERLAY_MODAL,
    OVERLAY_MODAL_EXPLICIT_TOOLTIP,
    DEBUG_HINTS,
}

internal interface TileCanvas {
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
        alpha: Float = 1f,
        tintColorHex: String? = null,
    )

    fun drawText(
        style: TileTextStyle,
        text: String,
        x: Float,
        y: Float,
        color: Color,
    )

    fun flushLayer(reason: TileLayerFlushReason)
}

internal object NoOpTileCanvas : TileCanvas {
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
        tintColorHex: String?,
    ) = Unit

    override fun drawText(
        style: TileTextStyle,
        text: String,
        x: Float,
        y: Float,
        color: Color,
    ) = Unit

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
)

class TileRenderer(
    private val localizer: Localizer,
    private val visualResolver: VisualManifestResolver,
    private val textureRepository: ClientTextureRepository,
    private val cellWidth: Float = 32f,
    private val cellHeight: Float = 32f,
) : Disposable {
    private val uiFont = KtomeFonts.createUiFont(size = 28)
    private val smallFont = KtomeFonts.createUiFont(size = 24)
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
        ).viewport.state
    }

    override fun dispose() {
        uiFont.dispose()
        smallFont.dispose()
        whitePixel.dispose()
    }

    private inner class GdxTileCanvas(
        private val batch: SpriteBatch,
    ) : TileCanvas {
        private var hasPendingDraw = false

        override fun drawRect(
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            color: Color,
        ) {
            val previous = Color(batch.color)
            batch.color = color
            batch.draw(whitePixel, x, y, width, height)
            batch.color = previous
            hasPendingDraw = true
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
            val texture = textureRepository.textureFor(asset)
            val previous = Color(batch.color)
            batch.color =
                tintColorHex
                    ?.let { tint -> Color.valueOf(tint.removePrefix("#")).also { color -> color.a = alpha } }
                    ?: Color(1f, 1f, 1f, alpha)
            batch.draw(texture, x, y, width, height)
            batch.color = previous
            hasPendingDraw = true
        }

        override fun drawText(
            style: TileTextStyle,
            text: String,
            x: Float,
            y: Float,
            color: Color,
        ) {
            val font = if (style == TileTextStyle.UI) uiFont else smallFont
            font.color = color
            font.draw(batch, text, x, y)
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
                        modalSafeBounds = layout.shell.modalSafeBounds,
                        bottomLogReservedBounds = layout.shell.bottomLogReservedBounds,
                    ),
                )
            val overlayFrame =
                OverlayRenderFrame(
                    overlayModel = overlayModel,
                    shellContentBounds = layout.shell.shellContentBounds,
                    modalSafeBounds = layout.shell.modalSafeBounds,
                    bottomLogReservedBounds = layout.shell.bottomLogReservedBounds,
                    textMetrics = TileTextMetrics,
                )

            canvas.drawRect(
                x = 0f,
                y = 0f,
                width = layout.worldWidth,
                height = layout.worldHeight,
                color = UiDesignTokens.color.surface.base.color(),
            )
            canvas.flushLayer(TileLayerFlushReason.BACKGROUND)
            canvas.drawRect(
                x = layout.shell.mapBounds.x,
                y = layout.shell.mapBounds.y,
                width = layout.shell.mapBounds.width,
                height = layout.shell.mapBounds.height,
                color = UiDesignTokens.color.surface.base.color(),
            )
            TileMapRenderer.render(canvas, MapRenderFrame(model, layerPlan, layout, viewport))
            TileShellRenderer.render(canvas, buildShellRenderFrame(model, layout, overlayState))
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

        private fun buildShellTextLayout(
            model: TileRenderModel,
            layout: TileLayoutMetrics,
        ): ShellTextLayout =
            ShellTextLayout(
                playerName =
                    TileTextMetrics.truncateTextToWidth(
                        model.playerCard.name.ifBlank { model.playerCard.emptyStateText },
                        layout.infoWidth - 24f,
                        TileTextStyle.UI,
                    ),
                summaryLines =
                    packSummaryLines(
                        summaryText = model.hud.summaryText,
                        maxChars = TileTextMetrics.maxCharsForWidth(layout.focusWidth - 24f, TileTextStyle.SMALL),
                        maxLines = 3,
                    ),
                targetTitle =
                    model.targetCard.title?.let { title ->
                        TileTextMetrics.truncateTextToWidth(title, layout.focusWidth - 66f, TileTextStyle.SMALL)
                    },
                targetLines =
                    model.targetCard.lines.map { line ->
                        TileTextMetrics.truncateTextToWidth(line, layout.focusWidth - 24f, TileTextStyle.SMALL)
                    },
                messageLines = buildMessageTextLayout(model, layout),
                leftRail =
                    buildPanelTextLayout(
                        panel = model.shell.leftRail,
                        width = layout.shell.leftRailBounds.width - 24f,
                        maxRows = ((layout.shell.leftRailBounds.height - 54f) / 30f).toInt().coerceAtLeast(0),
                    ),
                rightPanel =
                    buildPanelTextLayout(
                        panel = model.shell.rightPanel,
                        width = layout.shell.rightPanelBounds.width - 24f,
                        maxRows = ((layout.shell.rightPanelBounds.height - 54f) / 28f).toInt().coerceAtLeast(0),
                    ),
                footerHints =
                    TileTextMetrics.truncateTextToWidth(
                        model.shell.footerHints.joinToString("  ") { hint -> hint.text },
                        layout.worldWidth - layout.bottomInset * 2f,
                        TileTextStyle.SMALL,
                    ),
                hotbar = buildHotbarTextLayout(model, layout),
            )

        private fun buildMessageTextLayout(
            model: TileRenderModel,
            layout: TileLayoutMetrics,
        ): List<TileMessageLine> {
            val maxChars = TileTextMetrics.maxCharsForWidth(layout.logWidth - 24f, TileTextStyle.SMALL)
            val overlayMessages =
                if (model.messageLines.size > model.logPresentation.entries.size) {
                    model.messageLines.drop(model.logPresentation.entries.size)
                } else {
                    emptyList()
                }
            val displayLines = model.messageLines.take(model.logPresentation.entries.size) + overlayMessages
            return displayLines.flatMap { line -> wrapMessageLine(line, maxChars) }.takeLast(messageRows)
        }

        private fun wrapMessageLine(
            line: TileMessageLine,
            maxChars: Int,
        ): List<TileMessageLine> {
            val firstLineMaxChars = (maxChars - if (line.icon == null) 0 else 3).coerceAtLeast(1)
            return TileTextMetrics.wrapText(line.text, firstLineMaxChars, maxChars.coerceAtLeast(1)).mapIndexed { index, text ->
                line.copy(
                    text = TileTextMetrics.truncateText(text, if (index == 0 && line.icon != null) firstLineMaxChars else maxChars),
                    icon = line.icon.takeIf { index == 0 },
                )
            }
        }

        private fun buildPanelTextLayout(
            panel: TilePanelModel,
            width: Float,
            maxRows: Int,
        ): ShellPanelTextLayout {
            val titleMaxChars = TileTextMetrics.maxCharsForWidth(width, TileTextStyle.UI)
            val bodyMaxChars = TileTextMetrics.maxCharsForWidth(width, TileTextStyle.SMALL)
            return ShellPanelTextLayout(
                title = TileTextMetrics.truncateText(panel.title, titleMaxChars),
                rows =
                    panel.rows.take(maxRows).map { row ->
                        val style = if (row.tone == TileTextTone.GOLD && row.icon == null) TileTextStyle.UI else TileTextStyle.SMALL
                        ShellPanelRowTextLayout(
                            text = TileTextMetrics.truncateText(row.text, bodyMaxChars - if (row.icon == null) 0 else 3),
                            tone = row.tone,
                            icon = row.icon,
                            selected = row.selected,
                            style = style,
                        )
                    },
            )
        }

        private fun buildHotbarTextLayout(
            model: TileRenderModel,
            layout: TileLayoutMetrics,
        ): List<ShellHotbarTextLayout> {
            val maxWidth = layout.hotbarCardWidth - 64f
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

        private object TileShellRenderer {
            fun render(
                canvas: TileCanvas,
                frame: ShellRenderFrame,
            ) {
                val layout = frame.layout
                val model = frame.model
                canvas.drawRect(
                    x = layout.shell.bottomHudBounds.x,
                    y = layout.shell.bottomHudBounds.y,
                    width = layout.shell.bottomHudBounds.width,
                    height = layout.shell.bottomHudBounds.height,
                    color = UiDesignTokens.color.surface.raised.color(),
                )
                canvas.drawRect(
                    x = layout.shell.leftRailBounds.x,
                    y = layout.shell.leftRailBounds.y,
                    width = layout.shell.leftRailBounds.width,
                    height = layout.shell.leftRailBounds.height,
                    color = UiDesignTokens.color.surface.overlay.color(),
                )
                canvas.drawRect(
                    x = layout.shell.rightPanelBounds.x,
                    y = layout.shell.rightPanelBounds.y,
                    width = layout.shell.rightPanelBounds.width,
                    height = layout.shell.rightPanelBounds.height,
                    color = UiDesignTokens.color.surface.overlay.color(),
                )
                canvas.flushLayer(TileLayerFlushReason.SHELL_PANES)
                drawPaneFocusRing(canvas, frame.paneFocusAnchor, layout)
                drawLeftRail(canvas, frame.textLayout.leftRail, layout)
                drawHud(canvas, model, frame.textLayout, layout)
                drawMessages(canvas, frame.textLayout.messageLines, layout)
                drawRightPanel(canvas, frame.textLayout.rightPanel, layout)
                drawFooterHints(canvas, frame.textLayout.footerHints, layout)
                canvas.flushLayer(TileLayerFlushReason.BOTTOM_HUD)
            }
        }

        internal fun worldWidth(
            snapshot: RenderSnapshot,
            cellWidth: Float = 32f,
            cellHeight: Float = 32f,
        ): Float = UiDesignTokens.fixed.shellPreferredWorldWidth

        internal fun worldHeight(
            snapshot: RenderSnapshot,
            cellWidth: Float = 32f,
            cellHeight: Float = 32f,
        ): Float = UiDesignTokens.fixed.shellPreferredWorldHeight

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
                drawFogOverlays(canvas, frame.layerPlan.fogVeils, viewport)
                canvas.flushLayer(TileLayerFlushReason.MAP_FOG_VEILS)
                drawGroundLootMarkers(canvas, frame.layerPlan.groundLootMarkers, viewport)
                canvas.flushLayer(TileLayerFlushReason.MAP_GROUND_LOOT_MARKERS)
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
                            color = UiDesignTokens.color.quality.rare.color(),
                        )
                    }
                }
                canvas.flushLayer(TileLayerFlushReason.MAP_PLAYER_INDICATOR)
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
                    drawTooltip(canvas, model.selectedTooltip)
                }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_PASSIVE_TOOLTIP)
                model.toast?.let { toast ->
                    canvas.drawRect(
                        frame.shellContentBounds.x + 16f,
                        frame.shellContentBounds.top - 48f,
                        360f,
                        34f,
                        UiDesignTokens.color.surface.overlay.color(),
                    )
                    canvas.drawText(TileTextStyle.SMALL, toast.line.text, frame.shellContentBounds.x + 28f, frame.shellContentBounds.top - 26f, tone(toast.line.tone))
                }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_TOAST)
                model.modalBackdrop?.let { backdrop ->
                    canvas.drawRect(
                        backdrop.bounds.x,
                        backdrop.bounds.y,
                        backdrop.bounds.width,
                        backdrop.bounds.height,
                        backdrop.color.color().also { color -> color.a = backdrop.alpha },
                    )
                }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_MODAL_BACKDROP)
                model.activeModal?.let { modal -> drawModal(canvas, modal) }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_MODAL)
                if (model.activeModal != null && model.selectedTooltip != null) {
                    drawTooltip(canvas, model.selectedTooltip)
                }
                canvas.flushLayer(TileLayerFlushReason.OVERLAY_MODAL_EXPLICIT_TOOLTIP)
                canvas.flushLayer(TileLayerFlushReason.DEBUG_HINTS)
            }
        }

        private fun drawTooltip(
            canvas: TileCanvas,
            tooltip: TileTooltipModel,
        ) {
            val rect = tooltip.placedRect
            canvas.drawRect(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat(), UiDesignTokens.color.surface.overlay.color())
            canvas.drawRect(rect.x.toFloat(), (rect.top - 3).toFloat(), rect.width.toFloat(), 3f, UiDesignTokens.color.focus.ring.color())
            canvas.drawText(TileTextStyle.SMALL, tooltip.titleLine.text, rect.x + 8f, rect.top - 10f, tone(tooltip.titleLine.tone))
            tooltip.bodyLines.take(TILE_TOOLTIP_BODY_LINE_LIMIT).forEachIndexed { index, line ->
                canvas.drawText(TileTextStyle.SMALL, line.text, rect.x + 8f, rect.top - 36f - index * 22f, tone(line.tone))
            }
        }

        private fun drawModal(
            canvas: TileCanvas,
            modal: TileModalModel,
        ) {
            val bounds = modal.bounds
            canvas.drawRect(bounds.x.toFloat(), bounds.y.toFloat(), bounds.width.toFloat(), bounds.height.toFloat(), UiDesignTokens.color.surface.raised.color())
            canvas.drawRect(bounds.x.toFloat(), (bounds.top - 4).toFloat(), bounds.width.toFloat(), 4f, UiDesignTokens.color.quality.rare.color())
            canvas.drawText(TileTextStyle.UI, modal.titleLine.text, bounds.x + 18f, bounds.top - 18f, tone(modal.titleLine.tone))
            modal.bodyLines.take(10).forEachIndexed { index, line ->
                canvas.drawText(TileTextStyle.SMALL, line.text, bounds.x + 18f, bounds.top - 56f - index * 24f, tone(line.tone))
            }
            modal.footerHintLines.take(2).forEachIndexed { index, line ->
                canvas.drawText(TileTextStyle.SMALL, line.text, bounds.x + 18f, bounds.y + 24f + index * 22f, tone(line.tone))
            }
        }

        private fun drawHud(
            canvas: TileCanvas,
            model: TileRenderModel,
            textLayout: ShellTextLayout,
            layout: TileLayoutMetrics,
        ) {
            val hud = model.hud
            val actionPanel = model.actionPanel
            val textTopY = layout.cardY + layout.cardHeight - 10f
            val smallLineHeight = 26f
            val focusNameY = layout.cardY + 72f
            val focusBodyTopY = layout.cardY + 40f

            canvas.drawRect(layout.infoX, layout.cardY, layout.infoWidth, layout.cardHeight, UiDesignTokens.color.surface.raised.color())
            canvas.drawRect(layout.logX, layout.cardY, layout.logWidth, layout.cardHeight, UiDesignTokens.color.surface.overlay.color())
            canvas.drawRect(layout.focusX, layout.cardY, layout.focusWidth, layout.cardHeight, UiDesignTokens.color.surface.raised.color())

            canvas.drawText(
                TileTextStyle.UI,
                textLayout.playerName,
                layout.infoX + 12f,
                textTopY,
                tone(TileTextTone.GOLD),
            )
            val gaugeHeight = 11f
            val gaugeGap = 3f
            val gauges = listOfNotNull(hud.experienceGauge, hud.secondaryResourceGauge, hud.resourceGauge, hud.hpGauge)
            gauges.forEachIndexed { index, gauge ->
                val gaugeY = layout.cardY + 8f + index * (gaugeHeight + gaugeGap)
                drawGauge(canvas, gauge, layout.infoX + 12f, gaugeY, layout.infoWidth - 24f, gaugeHeight)
            }

            textLayout.summaryLines.forEachIndexed { index, line ->
                canvas.drawText(
                    TileTextStyle.SMALL,
                    line,
                    layout.focusX + 12f,
                    textTopY - index * smallLineHeight,
                    tone(TileTextTone.LIGHT_GRAY),
                )
            }

            var statusX = layout.focusX + 12f
            hud.statusIcons.forEach { icon ->
                canvas.drawRect(
                    statusX - 1f,
                    textTopY - 95f,
                    28f,
                    28f,
                    StatusHudRenderer.accentColor(icon.category),
                )
                canvas.drawAsset(icon.asset, statusX, textTopY - 94f, 26f, 26f)
                canvas.drawText(
                    TileTextStyle.SMALL,
                    icon.badgeText,
                    statusX,
                    textTopY - 100f,
                    StatusHudRenderer.badgeColor(icon.category),
                )
                statusX += 34f
            }

            hud.focusIcon?.let { icon ->
                canvas.drawAsset(icon, layout.focusX + 12f, layout.cardY + 38f, 32f, 32f)
            }
            textLayout.targetTitle?.let { name ->
                canvas.drawText(
                    TileTextStyle.SMALL,
                    name,
                    layout.focusX + 54f,
                    focusNameY,
                    tone(TileTextTone.GOLD),
                )
            }
            textLayout.targetLines.forEachIndexed { index, line ->
                canvas.drawText(
                    TileTextStyle.SMALL,
                    line,
                    layout.focusX + 12f,
                    focusBodyTopY - index * smallLineHeight,
                    tone(TileTextTone.LIGHT_GRAY),
                )
            }

            if (actionPanel.isEmpty) {
                return
            }
            actionPanel.entries.forEachIndexed { index, entry ->
                val slot = hud.hotbar.getOrNull(index)
                val hotbarText = textLayout.hotbar.getOrNull(index) ?: return@forEachIndexed
                val x = layout.hotbarX + index * (layout.hotbarCardWidth + layout.hotbarGap)
                canvas.drawRect(x, layout.hotbarY, layout.hotbarCardWidth, layout.hotbarCardHeight, UiDesignTokens.color.slot.filled.color())
                (entry.icon ?: slot?.icon)?.let { icon -> canvas.drawAsset(icon, x + 10f, layout.hotbarY + 14f, 38f, 38f) }
                slot?.accentIcon?.let { icon -> canvas.drawAsset(icon, x + 35f, layout.hotbarY + 42f, 16f, 16f) }
                canvas.drawText(TileTextStyle.SMALL, hotbarText.hotkey, x + 8f, layout.hotbarY + layout.hotbarCardHeight - 10f, tone(TileTextTone.GOLD))
                canvas.drawText(
                    TileTextStyle.SMALL,
                    hotbarText.label,
                    x + 62f,
                    layout.hotbarY + layout.hotbarCardHeight - 18f,
                    tone(TileTextTone.WHITE),
                )
                canvas.drawText(
                    TileTextStyle.SMALL,
                    hotbarText.detail,
                    x + 62f,
                    layout.hotbarY + 28f,
                    tone(hotbarText.detailTone),
                )
            }
        }

        private fun drawMessages(
            canvas: TileCanvas,
            messageLines: List<TileMessageLine>,
            layout: TileLayoutMetrics,
        ) {
            val topY = layout.cardY + layout.cardHeight - 18f
            messageLines.forEachIndexed { index, line ->
                val iconOffset =
                    line.icon?.let { icon ->
                        canvas.drawAsset(icon, layout.logX + 12f, topY - index * 26f - 16f, 18f, 18f)
                        24f
                    } ?: 0f
                canvas.drawText(
                    TileTextStyle.SMALL,
                    line.text,
                    layout.logX + 12f + iconOffset,
                    topY - index * 26f,
                    tone(line.tone),
                )
            }
        }

        private fun targetCursorColor(state: TileTargetCursorState?): Color =
            when (state) {
                TileTargetCursorState.ILLEGAL -> UiDesignTokens.color.telegraph.high.color()
                TileTargetCursorState.LEGAL,
                null,
                -> UiDesignTokens.color.quality.rare.color()
            }

        private fun drawPaneFocusRing(
            canvas: TileCanvas,
            paneFocusAnchor: PaneFocusAnchor?,
            layout: TileLayoutMetrics,
        ) {
            if (paneFocusAnchor == null) {
                return
            }
            val stroke = UiDesignTokens.stroke.medium
            val color = UiDesignTokens.color.focus.ring.color()
            when (paneFocusAnchor) {
                PaneFocusAnchor.WORLD ->
                    drawRectOutline(
                        canvas = canvas,
                        x = layout.shell.mapBounds.x,
                        y = layout.shell.mapBounds.y,
                        width = layout.shell.mapBounds.width,
                        height = layout.shell.mapBounds.height,
                        stroke = stroke,
                        color = color,
                    )

                PaneFocusAnchor.CONTEXT ->
                    drawRectOutline(
                        canvas = canvas,
                        x = layout.logX,
                        y = layout.cardY,
                        width = layout.logWidth,
                        height = layout.cardHeight,
                        stroke = stroke,
                        color = color,
                    )

                PaneFocusAnchor.CHARACTER_ACTION ->
                    drawRectOutline(
                        canvas = canvas,
                        x = layout.focusX,
                        y = layout.cardY,
                        width = layout.focusWidth,
                        height = layout.cardHeight,
                        stroke = stroke,
                        color = color,
                    )
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
            canvas.drawRect(x, y, width, stroke, color)
            canvas.drawRect(x, y + height - stroke, width, stroke, color)
            canvas.drawRect(x, y, stroke, height, color)
            canvas.drawRect(x + width - stroke, y, stroke, height, color)
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
                canvas.drawRect(iconX - 2f, iconY - 2f, iconSize + 4f, iconSize + 4f, color("05070A", 0.72f))
                canvas.drawRect(iconX - 2f, iconY - 2f, iconSize + 4f, 3f, tone(marker.rarityTone))
                marker.specialAccentTokenId?.let { accent ->
                    canvas.drawRect(iconX - 2f, iconY + iconSize - 1f, iconSize + 4f, 3f, specialAccentColor(accent))
                }
                canvas.drawAsset(marker.icon, iconX, iconY, iconSize, iconSize)
                marker.cornerGlyph?.let { glyph ->
                    canvas.drawText(TileTextStyle.SMALL, glyph, iconX - 1f, iconY + iconSize + 13f, tone(marker.rarityTone))
                }
                marker.countBadge?.let { badge ->
                    val badgeWidth = if (badge == "9+") 27f else 20f
                    canvas.drawRect(iconX + iconSize - badgeWidth + 3f, iconY - 4f, badgeWidth, 18f, UiDesignTokens.color.surface.baseDim.color())
                    canvas.drawText(TileTextStyle.SMALL, badge, iconX + iconSize - badgeWidth + 6f, iconY + 11f, tone(TileTextTone.WHITE))
                }
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
                canvas.drawRect(worldX - 2f, worldY - 16f, backgroundWidth, 18f, UiDesignTokens.color.surface.baseDim.color())
                canvas.drawText(
                    TileTextStyle.SMALL,
                    feedback.text,
                    worldX,
                    worldY - 2f,
                    tone(feedback.tone),
                )
            }
        }

        private fun drawLeftRail(
            canvas: TileCanvas,
            panel: ShellPanelTextLayout,
            layout: TileLayoutMetrics,
        ) {
            val bounds = layout.shell.leftRailBounds
            drawPanelRows(
                canvas = canvas,
                panel = panel,
                x = bounds.x + 12f,
                topY = bounds.top - 16f,
                maxRows = ((bounds.height - 54f) / 30f).toInt().coerceAtLeast(0),
            )
        }

        private fun drawRightPanel(
            canvas: TileCanvas,
            panel: ShellPanelTextLayout,
            layout: TileLayoutMetrics,
        ) {
            val bounds = layout.shell.rightPanelBounds
            drawPanelRows(
                canvas = canvas,
                panel = panel,
                x = bounds.x + 12f,
                topY = bounds.top - 16f,
                maxRows = ((bounds.height - 54f) / 28f).toInt().coerceAtLeast(0),
            )
        }

        private fun drawPanelRows(
            canvas: TileCanvas,
            panel: ShellPanelTextLayout,
            x: Float,
            topY: Float,
            maxRows: Int,
        ) {
            canvas.drawText(TileTextStyle.UI, panel.title, x, topY, tone(TileTextTone.GOLD))
            panel.rows.take(maxRows).forEachIndexed { index, row ->
                val baseline = topY - 34f - index * 28f
                canvas.drawText(
                    style = row.style,
                    text = row.text,
                    x = x + if (row.icon == null) 0f else 28f,
                    y = baseline,
                    color = tone(if (row.selected) TileTextTone.CYAN else row.tone),
                )
                row.icon?.let { icon ->
                    canvas.drawAsset(icon, x, baseline - 18f, 18f, 18f, alpha = if (row.selected) 1f else 0.95f)
                }
            }
        }

        private fun drawFooterHints(
            canvas: TileCanvas,
            hints: String,
            layout: TileLayoutMetrics,
        ) {
            val y = UiDesignTokens.spacing.lg
            canvas.drawText(TileTextStyle.SMALL, hints, layout.bottomInset, y, tone(TileTextTone.LIGHT_GRAY))
        }

        private fun drawGauge(
            canvas: TileCanvas,
            gauge: TileGaugeModel,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
        ) {
            canvas.drawRect(x, y, width, height, UiDesignTokens.color.bar.background.color())
            canvas.drawRect(x + 2f, y + 2f, (width - 4f) * gauge.percent, height - 4f, gaugeFillColor(gauge))
            canvas.drawText(TileTextStyle.SMALL, gauge.summary, x + 6f, y + height - 3f, tone(TileTextTone.WHITE))
        }

        private fun gaugeFillColor(gauge: TileGaugeModel): Color =
            when (gauge.resourceTypeId) {
                "HEALTH" -> UiDesignTokens.color.bar.hp.color()
                "EXPERIENCE" -> UiDesignTokens.color.bar.experience.color()
                else ->
                    if (gauge.stableMin != null || gauge.stableMax != null) {
                        UiDesignTokens.color.bar.secondaryResource.color()
                    } else {
                        UiDesignTokens.color.bar.resource.color()
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
            canvas.drawAsset(placement.asset, drawX, drawY, width, height, placement.alpha, placement.tintColorHex)
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
                canvas.drawRect(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat(), color("05070A", fog.alpha))
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
            val widthPerCharacter = TileTextMetrics.approximateCharWidth(style).roundToInt()
            val height = TileTextMetrics.approximateLineHeight(style).roundToInt()
            return intArrayOf(
                x.roundToInt(),
                y.roundToInt(),
                maxOf(1, text.length * widthPerCharacter),
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
            maxChars: Int,
            maxLines: Int,
        ): List<String> {
            if (summaryText.isBlank()) {
                return emptyList()
            }
            val segments = summaryText.split("  ").filter { it.isNotBlank() }
            if (segments.isEmpty()) {
                return listOf(TileTextMetrics.truncateText(summaryText, maxChars))
            }
            val lines = mutableListOf<String>()
            var current = ""
            segments.forEach { rawSegment ->
                val segment = TileTextMetrics.truncateText(rawSegment, maxChars)
                val candidate = if (current.isBlank()) segment else "$current  $segment"
                if (candidate.length <= maxChars) {
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
                maxLines <= 1 -> listOf(TileTextMetrics.truncateText(lines.joinToString("  "), maxChars))
                else -> lines.take(maxLines - 1) + TileTextMetrics.truncateText(lines.drop(maxLines - 1).joinToString("  "), maxChars)
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
