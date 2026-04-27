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
import com.ktome.client.render.layout.InfoSurfaceLayout
import com.ktome.client.render.layout.InfoSurfaceLayoutRequest
import com.ktome.client.render.layout.InfoSurfaceLayoutSolver
import com.ktome.client.ui.layout.PaneFocusAnchor
import com.ktome.client.ui.panel.LogPresentationModel
import com.ktome.client.ui.status.StatusHudRenderer
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.i18n.Localizer
import kotlin.math.roundToInt

internal enum class TileTextStyle {
    UI,
    SMALL,
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
}

internal data class TileLayoutMetrics(
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

    fun render(
        batch: SpriteBatch,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ) {
        renderToCanvas(
            localizer = localizer,
            visualResolver = visualResolver,
            snapshot = snapshot,
            overlayState = overlayState,
            canvas = GdxTileCanvas(batch),
            cellWidth = cellWidth,
            cellHeight = cellHeight,
        )
    }

    override fun dispose() {
        uiFont.dispose()
        smallFont.dispose()
        whitePixel.dispose()
    }

    private inner class GdxTileCanvas(
        private val batch: SpriteBatch,
    ) : TileCanvas {
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
        ) {
            val model = buildRenderModel(localizer, visualResolver, snapshot, overlayState)
            val mapWidth = snapshot.metadata.width
            val mapHeight = snapshot.metadata.height
            val layout = layoutMetrics(mapWidth, mapHeight, cellWidth, cellHeight)

            canvas.drawRect(
                x = 0f,
                y = 0f,
                width = layout.worldWidth,
                height = layout.worldHeight,
                color = UiDesignTokens.color.surface.base.color(),
            )
            canvas.drawRect(
                x = 0f,
                y = layout.mapOffsetY,
                width = mapWidth * cellWidth,
                height = mapHeight * cellHeight,
                color = UiDesignTokens.color.surface.base.color(),
            )
            canvas.drawRect(
                x = 0f,
                y = 0f,
                width = layout.worldWidth,
                height = layout.mapOffsetY,
                color = UiDesignTokens.color.surface.raised.color(),
            )
            canvas.drawRect(
                x = layout.sidebarX - 10f,
                y = layout.mapOffsetY,
                width = layout.sidebarWidth + 20f,
                height = mapHeight * cellHeight,
                color = UiDesignTokens.color.surface.overlay.color(),
            )

            TileLayerComposer.compose(model).forEach { placement ->
                drawPlacement(canvas, placement, mapHeight, layout.mapOffsetY, cellWidth, cellHeight)
            }
            drawGroundLootMarkers(canvas, model.groundLootMarkers, mapHeight, layout.mapOffsetY, cellWidth, cellHeight)
            drawFogOverlays(canvas, model.fogTiles, mapHeight, layout.mapOffsetY, cellWidth, cellHeight)
            drawCombatFeedback(canvas, model.combatFeedback, mapHeight, layout.mapOffsetY, cellWidth, cellHeight)

            model.targetCursor?.let { cursor ->
                drawCursor(
                    canvas,
                    cursor.x,
                    cursor.y,
                    mapHeight,
                    layout.mapOffsetY,
                    cellWidth,
                    cellHeight,
                    targetCursorColor(model.targetCursorState),
                )
            }
            model.inspectCursor?.let { cursor ->
                drawCursor(canvas, cursor.x, cursor.y, mapHeight, layout.mapOffsetY, cellWidth, cellHeight, UiDesignTokens.color.focus.ring.color())
            }

            drawPaneFocusRing(canvas, overlayState, layout, mapWidth, mapHeight, cellWidth, cellHeight)
            drawHud(canvas, model, layout)
            drawMessages(canvas, model.logPresentation, model.messageLines, layout)
            drawSidebar(canvas, model.sidebar, layout, mapHeight, cellHeight)
        }

        internal fun worldWidth(
            snapshot: RenderSnapshot,
            cellWidth: Float = 32f,
            cellHeight: Float = 32f,
        ): Float = layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, cellWidth, cellHeight).worldWidth

        internal fun worldHeight(
            snapshot: RenderSnapshot,
            cellWidth: Float = 32f,
            cellHeight: Float = 32f,
        ): Float = layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, cellWidth, cellHeight).worldHeight

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

        private fun drawHud(
            canvas: TileCanvas,
            model: TileRenderModel,
            layout: TileLayoutMetrics,
        ) {
            val hud = model.hud
            val playerCard = model.playerCard
            val targetCard = model.targetCard
            val actionPanel = model.actionPanel
            val textTopY = layout.cardY + layout.cardHeight - 10f
            val smallLineHeight = 26f
            val focusNameY = layout.cardY + 72f
            val focusBodyTopY = layout.cardY + 40f
            val summaryMaxChars = maxCharsForWidth(layout.focusWidth - 24f, TileTextStyle.SMALL)
            val summaryLines = packSummaryLines(hud.summaryText, summaryMaxChars, maxLines = 3)

            canvas.drawRect(layout.infoX, layout.cardY, layout.infoWidth, layout.cardHeight, UiDesignTokens.color.surface.raised.color())
            canvas.drawRect(layout.logX, layout.cardY, layout.logWidth, layout.cardHeight, UiDesignTokens.color.surface.overlay.color())
            canvas.drawRect(layout.focusX, layout.cardY, layout.focusWidth, layout.cardHeight, UiDesignTokens.color.surface.raised.color())

            canvas.drawText(
                TileTextStyle.UI,
                truncateTextToWidth(playerCard.name.ifBlank { playerCard.emptyStateText }, layout.infoWidth - 24f, TileTextStyle.UI),
                layout.infoX + 12f,
                textTopY,
                tone(TileTextTone.GOLD),
            )
            canvas.drawText(
                TileTextStyle.SMALL,
                truncateTextToWidth("${hud.zoneName}  ${hud.floorText}", layout.infoWidth - 24f, TileTextStyle.SMALL),
                layout.infoX + 12f,
                textTopY - smallLineHeight,
                tone(TileTextTone.LIGHT_GRAY),
            )
            val gaugeHeight = 14f
            val gaugeGap = 4f
            val gauges = listOfNotNull(hud.secondaryResourceGauge, hud.resourceGauge, hud.hpGauge)
            gauges.forEachIndexed { index, gauge ->
                val gaugeY = layout.cardY + 8f + index * (gaugeHeight + gaugeGap)
                drawGauge(canvas, gauge, layout.infoX + 12f, gaugeY, layout.infoWidth - 24f, gaugeHeight)
            }

            summaryLines.forEachIndexed { index, line ->
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
            targetCard.title?.let { name ->
                canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateTextToWidth(name, layout.focusWidth - 66f, TileTextStyle.SMALL),
                    layout.focusX + 54f,
                    focusNameY,
                    tone(TileTextTone.GOLD),
                )
            }
            targetCard.lines.forEachIndexed { index, line ->
                canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateTextToWidth(line, layout.focusWidth - 24f, TileTextStyle.SMALL),
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
                val x = layout.hotbarX + index * (layout.hotbarCardWidth + layout.hotbarGap)
                canvas.drawRect(x, layout.hotbarY, layout.hotbarCardWidth, layout.hotbarCardHeight, UiDesignTokens.color.surface.raised.color())
                (entry.icon ?: slot?.icon)?.let { icon -> canvas.drawAsset(icon, x + 10f, layout.hotbarY + 18f, 44f, 44f) }
                slot?.accentIcon?.let { icon -> canvas.drawAsset(icon, x + 40f, layout.hotbarY + 48f, 16f, 16f) }
                canvas.drawText(TileTextStyle.SMALL, entry.hotkey, x + 8f, layout.hotbarY + 74f, tone(TileTextTone.GOLD))
                canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateTextToWidth(entry.label, layout.hotbarCardWidth - 64f, TileTextStyle.SMALL),
                    x + 62f,
                    layout.hotbarY + 66f,
                    tone(TileTextTone.WHITE),
                )
                slot?.cooldownText?.let { cooldown ->
                    canvas.drawText(
                        TileTextStyle.SMALL,
                        truncateTextToWidth(cooldown, layout.hotbarCardWidth - 64f, TileTextStyle.SMALL),
                        x + 62f,
                        layout.hotbarY + 34f,
                        tone(TileTextTone.RED),
                    )
                } ?: canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateTextToWidth(slot?.resourceText.orEmpty(), layout.hotbarCardWidth - 64f, TileTextStyle.SMALL),
                    x + 62f,
                    layout.hotbarY + 34f,
                    tone(TileTextTone.LIGHT_GRAY),
                )
            }
        }

        private fun drawMessages(
            canvas: TileCanvas,
            logPresentation: LogPresentationModel,
            messageLines: List<TileMessageLine>,
            layout: TileLayoutMetrics,
        ) {
            val topY = layout.cardY + layout.cardHeight - 18f
            val maxChars = maxCharsForWidth(layout.logWidth - 24f, TileTextStyle.SMALL)
            val overlayMessages =
                if (messageLines.size > logPresentation.entries.size) {
                    messageLines.drop(logPresentation.entries.size)
                } else {
                    emptyList()
                }
            val displayLines = messageLines.take(logPresentation.entries.size) + overlayMessages
            val wrappedLines = displayLines.flatMap { line -> wrapMessageLine(line, maxChars) }
            wrappedLines.takeLast(messageRows).forEachIndexed { index, line ->
                val iconOffset =
                    line.icon?.let { icon ->
                        canvas.drawAsset(icon, layout.logX + 12f, topY - index * 26f - 16f, 18f, 18f)
                        24f
                    } ?: 0f
                canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateText(line.text, (maxChars - if (line.icon == null) 0 else 3).coerceAtLeast(0)),
                    layout.logX + 12f + iconOffset,
                    topY - index * 26f,
                    tone(line.tone),
                )
            }
        }

        private fun wrapMessageLine(
            line: TileMessageLine,
            maxChars: Int,
        ): List<TileMessageLine> {
            val firstLineMaxChars = (maxChars - if (line.icon == null) 0 else 3).coerceAtLeast(1)
            return wrapText(line.text, firstLineMaxChars, maxChars.coerceAtLeast(1)).mapIndexed { index, text ->
                line.copy(text = text, icon = line.icon.takeIf { index == 0 })
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
            overlayState: OverlayState,
            layout: TileLayoutMetrics,
            mapWidth: Int,
            mapHeight: Int,
            cellWidth: Float,
            cellHeight: Float,
        ) {
            if (overlayState.mode != UiMode.MAP) {
                return
            }
            val stroke = UiDesignTokens.stroke.medium
            val color = UiDesignTokens.color.focus.ring.color()
            when (overlayState.paneFocusAnchor) {
                PaneFocusAnchor.WORLD ->
                    drawRectOutline(
                        canvas = canvas,
                        x = 0f,
                        y = layout.mapOffsetY,
                        width = mapWidth * cellWidth,
                        height = mapHeight * cellHeight,
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
            mapHeight: Int,
            mapOffsetY: Float,
            cellWidth: Float,
            cellHeight: Float,
        ) {
            markers.forEach { marker ->
                val cellLeft = marker.x * cellWidth
                val cellBottom = mapOffsetY + (mapHeight - marker.y - 1) * cellHeight
                val actorCorner = marker.placement == com.ktome.client.ui.item.GroundLootMarkerPlacement.ACTOR_CORNER
                val iconSize = if (actorCorner) cellWidth * 0.52f else cellWidth * 0.72f
                val iconX =
                    if (actorCorner) {
                        cellLeft + cellWidth - iconSize - 2f
                    } else {
                        cellLeft + (cellWidth - iconSize) / 2f
                    }
                val iconY =
                    if (actorCorner) {
                        cellBottom + cellHeight - iconSize - 2f
                    } else {
                        cellBottom + (cellHeight - iconSize) / 2f
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
            mapHeight: Int,
            mapOffsetY: Float,
            cellWidth: Float,
            cellHeight: Float,
        ) {
            combatFeedback.forEach { feedback ->
                val worldX = feedback.x * cellWidth + 4f + feedback.horizontalOffsetCells * (cellWidth + 6f)
                val worldY = mapOffsetY + (mapHeight - feedback.y - 1) * cellHeight + cellHeight - 2f + feedback.stackIndex * 15f
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

        private fun drawSidebar(
            canvas: TileCanvas,
            sidebar: TileSidebarModel,
            layout: TileLayoutMetrics,
            mapHeight: Int,
            cellHeight: Float,
        ) {
            val titleY = layout.mapOffsetY + mapHeight * cellHeight - 16f
            val lineHeight = 32f
            val maxRows = (((mapHeight * cellHeight) - 56f) / lineHeight).toInt().coerceAtLeast(0)
            val titleMaxChars = maxCharsForWidth(layout.sidebarWidth - 12f, TileTextStyle.UI)
            val bodyMaxChars = maxCharsForWidth(layout.sidebarWidth - 40f, TileTextStyle.SMALL)
            canvas.drawText(
                TileTextStyle.UI,
                truncateText(sidebar.title, titleMaxChars),
                layout.sidebarX,
                titleY,
                tone(TileTextTone.GOLD),
            )
            sidebar.rows.take(maxRows).forEachIndexed { index, row ->
                val baseline = titleY - 32f - index * lineHeight
                row.icon?.let { icon ->
                    canvas.drawAsset(icon, layout.sidebarX, baseline - 18f, 20f, 20f, alpha = if (row.selected) 1f else 0.95f)
                }
                canvas.drawText(
                    style = if (row.tone == TileTextTone.GOLD && row.icon == null) TileTextStyle.UI else TileTextStyle.SMALL,
                    text = truncateText(row.text, bodyMaxChars - if (row.icon == null) 0 else 3),
                    x = layout.sidebarX + if (row.icon == null) 0f else 30f,
                    y = baseline,
                    color = tone(if (row.selected) TileTextTone.CYAN else row.tone),
                )
            }
        }

        private fun drawGauge(
            canvas: TileCanvas,
            gauge: TileGaugeModel,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
        ) {
            canvas.drawRect(x, y, width, height, UiDesignTokens.color.surface.overlay.color())
            canvas.drawRect(x + 2f, y + 2f, (width - 4f) * gauge.percent, height - 4f, tone(gauge.tone))
            canvas.drawText(TileTextStyle.SMALL, gauge.summary, x + 6f, y + height - 3f, tone(TileTextTone.WHITE))
        }

        private fun drawCursor(
            canvas: TileCanvas,
            x: Int,
            y: Int,
            mapHeight: Int,
            mapOffsetY: Float,
            cellWidth: Float,
            cellHeight: Float,
            color: Color,
        ) {
            val worldX = x * cellWidth
            val worldY = mapOffsetY + (mapHeight - y - 1) * cellHeight
            drawRectOutline(canvas, worldX, worldY, cellWidth, cellHeight, 2f, color)
        }

        private fun drawPlacement(
            canvas: TileCanvas,
            placement: TileVisualPlacement,
            mapHeight: Int,
            mapOffsetY: Float,
            cellWidth: Float,
            cellHeight: Float,
        ) {
            val footprint = footprintDimensions(placement.asset.entry.footprint)
            val width = cellWidth * footprint.first
            val height = cellHeight * footprint.second
            val anchorX = placement.x * cellWidth + cellWidth * placement.asset.entry.pivotX.toFloat()
            val cellBottom = mapOffsetY + (mapHeight - placement.y - 1) * cellHeight
            val anchorY = cellBottom + cellHeight * placement.asset.entry.pivotY.toFloat()
            val drawX = anchorX - width * placement.asset.entry.pivotX.toFloat()
            val drawY = anchorY - height * placement.asset.entry.pivotY.toFloat()
            canvas.drawAsset(placement.asset, drawX, drawY, width, height, placement.alpha, placement.tintColorHex)
        }

        private fun drawFogOverlays(
            canvas: TileCanvas,
            fogTiles: List<TileFogPlacement>,
            mapHeight: Int,
            mapOffsetY: Float,
            cellWidth: Float,
            cellHeight: Float,
        ) {
            fogTiles.forEach { fog ->
                val worldX = fog.x * cellWidth
                val worldY = mapOffsetY + (mapHeight - fog.y - 1) * cellHeight
                canvas.drawRect(worldX, worldY, cellWidth, cellHeight, color("05070A", fog.alpha))
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
            val widthPerCharacter = approximateCharWidth(style).roundToInt()
            val height = approximateLineHeight(style).roundToInt()
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
                return listOf(truncateText(summaryText, maxChars))
            }
            val lines = mutableListOf<String>()
            var current = ""
            segments.forEach { rawSegment ->
                val segment = truncateText(rawSegment, maxChars)
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
                maxLines <= 1 -> listOf(truncateText(lines.joinToString("  "), maxChars))
                else -> lines.take(maxLines - 1) + truncateText(lines.drop(maxLines - 1).joinToString("  "), maxChars)
            }
        }

        private fun wrapText(
            text: String,
            firstLineMaxChars: Int,
            continuationMaxChars: Int,
        ): List<String> {
            val lines = mutableListOf<String>()
            var remaining = text
            var maxChars = firstLineMaxChars.coerceAtLeast(1)
            while (remaining.length > maxChars) {
                val splitIndex = wrapSplitIndex(remaining, maxChars)
                lines += remaining.take(splitIndex).trimEnd()
                remaining = remaining.drop(splitIndex).trimStart()
                maxChars = continuationMaxChars.coerceAtLeast(1)
            }
            if (remaining.isNotBlank()) {
                lines += remaining
            }
            return lines.ifEmpty { listOf("") }
        }

        private fun wrapSplitIndex(
            text: String,
            maxChars: Int,
        ): Int {
            val searchEnd = (maxChars + 1).coerceAtMost(text.length)
            val whitespaceSplit = text.take(searchEnd).indexOfLast(Char::isWhitespace)
            return if (whitespaceSplit > 0) {
                whitespaceSplit
            } else {
                maxChars.coerceAtLeast(1)
            }
        }

        private fun truncateTextToWidth(
            text: String,
            maxWidth: Float,
            style: TileTextStyle,
        ): String = truncateText(text, maxCharsForWidth(maxWidth, style))

        private fun maxCharsForWidth(
            maxWidth: Float,
            style: TileTextStyle,
        ): Int = (maxWidth / approximateCharWidth(style)).toInt().coerceAtLeast(1)

        private fun approximateCharWidth(style: TileTextStyle): Float =
            when (style) {
                TileTextStyle.UI -> 18f
                TileTextStyle.SMALL -> 16f
            }

        private fun approximateLineHeight(style: TileTextStyle): Float =
            when (style) {
                TileTextStyle.UI -> 30f
                TileTextStyle.SMALL -> 24f
            }

        private fun truncateText(
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
