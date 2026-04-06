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
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
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
                color = color("0B0D12"),
            )
            canvas.drawRect(
                x = 0f,
                y = layout.mapOffsetY,
                width = mapWidth * cellWidth,
                height = mapHeight * cellHeight,
                color = color("06080D"),
            )
            canvas.drawRect(
                x = 0f,
                y = 0f,
                width = layout.worldWidth,
                height = layout.mapOffsetY,
                color = color("11131A"),
            )
            canvas.drawRect(
                x = layout.sidebarX - 10f,
                y = layout.mapOffsetY,
                width = layout.sidebarWidth + 20f,
                height = mapHeight * cellHeight,
                color = color("101016", 0.93f),
            )

            TileLayerComposer.compose(model).forEach { placement ->
                drawPlacement(canvas, placement, mapHeight, layout.mapOffsetY, cellWidth, cellHeight)
            }
            drawFogOverlays(canvas, model.fogTiles, mapHeight, layout.mapOffsetY, cellWidth, cellHeight)
            drawCombatFeedback(canvas, model.combatFeedback, mapHeight, layout.mapOffsetY, cellWidth, cellHeight)

            model.targetCursor?.let { cursor ->
                drawCursor(canvas, cursor.x, cursor.y, mapHeight, layout.mapOffsetY, cellWidth, cellHeight, color("FFA500"))
            }
            model.inspectCursor?.let { cursor ->
                drawCursor(canvas, cursor.x, cursor.y, mapHeight, layout.mapOffsetY, cellWidth, cellHeight, color("33CCDD"))
            }

            drawHud(canvas, model.hud, layout)
            drawMessages(canvas, model.messageLines, layout)
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
                UiMode.STAT_ASSIGN -> localizer.text("ui.sidebar.assign_stats")
                UiMode.TALENT_ASSIGN -> localizer.text("ui.sidebar.improve_talents")
            }

        private fun drawHud(
            canvas: TileCanvas,
            hud: TileHudModel,
            layout: TileLayoutMetrics,
        ) {
            val textTopY = layout.cardY + layout.cardHeight - 16f
            val smallLineHeight = 26f
            val focusNameY = layout.cardY + 72f
            val focusBodyTopY = layout.cardY + 40f
            val summaryMaxChars = maxCharsForWidth(layout.focusWidth - 24f, TileTextStyle.SMALL)
            val summaryLines = packSummaryLines(hud.summaryText, summaryMaxChars, maxLines = 3)

            canvas.drawRect(layout.infoX, layout.cardY, layout.infoWidth, layout.cardHeight, color("171B24", 0.96f))
            canvas.drawRect(layout.logX, layout.cardY, layout.logWidth, layout.cardHeight, color("151922", 0.94f))
            canvas.drawRect(layout.focusX, layout.cardY, layout.focusWidth, layout.cardHeight, color("171B24", 0.96f))

            canvas.drawText(
                TileTextStyle.UI,
                truncateTextToWidth(hud.playerName, layout.infoWidth - 24f, TileTextStyle.UI),
                layout.infoX + 12f,
                textTopY,
                Color.GOLD,
            )
            canvas.drawText(
                TileTextStyle.SMALL,
                truncateTextToWidth("${hud.zoneName}  ${hud.floorText}", layout.infoWidth - 24f, TileTextStyle.SMALL),
                layout.infoX + 12f,
                textTopY - smallLineHeight,
                Color.LIGHT_GRAY,
            )
            val gaugeHeight = 14f
            val gaugeGap = 4f
            val secondaryGaugeY = layout.cardY + 4f
            val resourceGaugeY = secondaryGaugeY + gaugeHeight + gaugeGap
            val hpGaugeY = resourceGaugeY + gaugeHeight + gaugeGap
            drawGauge(canvas, hud.hpGauge, layout.infoX + 12f, hpGaugeY, layout.infoWidth - 24f, gaugeHeight)
            drawGauge(canvas, hud.resourceGauge, layout.infoX + 12f, resourceGaugeY, layout.infoWidth - 24f, gaugeHeight)
            hud.secondaryResourceGauge?.let { gauge ->
                drawGauge(canvas, gauge, layout.infoX + 12f, secondaryGaugeY, layout.infoWidth - 24f, gaugeHeight)
            }

            summaryLines.forEachIndexed { index, line ->
                canvas.drawText(
                    TileTextStyle.SMALL,
                    line,
                    layout.focusX + 12f,
                    textTopY - index * smallLineHeight,
                    Color.LIGHT_GRAY,
                )
            }

            var statusX = layout.focusX + 12f
            hud.statusIcons.forEach { icon ->
                canvas.drawRect(
                    statusX - 1f,
                    textTopY - 95f,
                    28f,
                    28f,
                    statusAccentColor(icon.category),
                )
                canvas.drawAsset(icon.asset, statusX, textTopY - 94f, 26f, 26f)
                canvas.drawText(
                    TileTextStyle.SMALL,
                    icon.badgeText,
                    statusX,
                    textTopY - 100f,
                    statusBadgeColor(icon.category),
                )
                statusX += 34f
            }

            hud.focusIcon?.let { icon ->
                canvas.drawAsset(icon, layout.focusX + 12f, layout.cardY + 38f, 32f, 32f)
            }
            hud.focusName?.let { name ->
                canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateTextToWidth(name, layout.focusWidth - 66f, TileTextStyle.SMALL),
                    layout.focusX + 54f,
                    focusNameY,
                    Color.GOLD,
                )
            }
            hud.focusLines.forEachIndexed { index, line ->
                canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateTextToWidth(line, layout.focusWidth - 24f, TileTextStyle.SMALL),
                    layout.focusX + 12f,
                    focusBodyTopY - index * smallLineHeight,
                    Color.LIGHT_GRAY,
                )
            }

            hud.hotbar.forEachIndexed { index, slot ->
                val x = layout.hotbarX + index * (layout.hotbarCardWidth + layout.hotbarGap)
                canvas.drawRect(x, layout.hotbarY, layout.hotbarCardWidth, layout.hotbarCardHeight, color("1A1D26"))
                slot.icon?.let { icon -> canvas.drawAsset(icon, x + 10f, layout.hotbarY + 18f, 44f, 44f) }
                slot.accentIcon?.let { icon -> canvas.drawAsset(icon, x + 40f, layout.hotbarY + 48f, 16f, 16f) }
                canvas.drawText(TileTextStyle.SMALL, slot.slot.toString(), x + 8f, layout.hotbarY + 74f, Color.GOLD)
                canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateTextToWidth(slot.label, layout.hotbarCardWidth - 64f, TileTextStyle.SMALL),
                    x + 62f,
                    layout.hotbarY + 66f,
                    Color.WHITE,
                )
                slot.cooldownText?.let { cooldown ->
                    canvas.drawText(
                        TileTextStyle.SMALL,
                        truncateTextToWidth(cooldown, layout.hotbarCardWidth - 64f, TileTextStyle.SMALL),
                        x + 62f,
                        layout.hotbarY + 34f,
                        Color.SALMON,
                    )
                } ?: canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateTextToWidth(slot.resourceText, layout.hotbarCardWidth - 64f, TileTextStyle.SMALL),
                    x + 62f,
                    layout.hotbarY + 34f,
                    Color.LIGHT_GRAY,
                )
            }
        }

        private fun drawMessages(
            canvas: TileCanvas,
            messageLines: List<TileMessageLine>,
            layout: TileLayoutMetrics,
        ) {
            val topY = layout.cardY + layout.cardHeight - 18f
            val maxChars = maxCharsForWidth(layout.logWidth - 24f, TileTextStyle.SMALL)
            messageLines.takeLast(messageRows).forEachIndexed { index, line ->
                canvas.drawText(
                    TileTextStyle.SMALL,
                    truncateText(line.text, maxChars),
                    layout.logX + 12f,
                    topY - index * 26f,
                    tone(line.tone),
                )
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
                canvas.drawRect(worldX - 2f, worldY - 16f, backgroundWidth, 18f, color("0B0D12", 0.82f))
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
                Color.GOLD,
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
            canvas.drawRect(x, y, width, height, color("232630"))
            canvas.drawRect(x + 2f, y + 2f, (width - 4f) * gauge.percent, height - 4f, tone(gauge.tone))
            canvas.drawText(TileTextStyle.SMALL, gauge.summary, x + 6f, y + height - 3f, Color.WHITE)
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
            canvas.drawRect(worldX, worldY, cellWidth, 2f, color)
            canvas.drawRect(worldX, worldY + cellHeight - 2f, cellWidth, 2f, color)
            canvas.drawRect(worldX, worldY, 2f, cellHeight, color)
            canvas.drawRect(worldX + cellWidth - 2f, worldY, 2f, cellHeight, color)
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
            val mapWidthPx = mapWidth * cellWidth
            val mapHeightPx = mapHeight * cellHeight
            val mapOffsetY = uiRows * cellHeight
            val sidebarGap = 18f
            val sidebarWidth = (mapWidthPx * 0.55f).coerceIn(340f, 420f)
            val worldWidth = mapWidthPx + sidebarGap + sidebarWidth + 18f
            val worldHeight = mapHeightPx + mapOffsetY
            val bottomInset = 12f
            val panelGap = 12f
            val hotbarX = bottomInset
            val hotbarY = 12f
            val hotbarCardWidth = 126f
            val hotbarCardHeight = 84f
            val hotbarGap = 14f
            val cardY = hotbarY + hotbarCardHeight + 12f
            val cardHeight = (mapOffsetY - cardY - 12f).coerceAtLeast(96f)
            val panelWidth = worldWidth - bottomInset * 2
            val preferredLogWidth = (panelWidth * 0.19f).coerceIn(300f, 420f)
            val minLogWidth = 180f
            var infoWidth = (panelWidth * 0.28f).coerceIn(360f, 480f)
            var focusWidth = (panelWidth * 0.17f).coerceIn(250f, 340f)
            val infoX = bottomInset
            var focusX = bottomInset + panelWidth - focusWidth
            var availableLogWidth = focusX - panelGap - (infoX + infoWidth)
            if (availableLogWidth < minLogWidth) {
                var deficit = minLogWidth - availableLogWidth
                val focusShrink = minOf((focusWidth - 170f).coerceAtLeast(0f), deficit * 0.55f)
                focusWidth -= focusShrink
                deficit -= focusShrink
                val infoShrink = minOf((infoWidth - 260f).coerceAtLeast(0f), deficit)
                infoWidth -= infoShrink
                focusX = bottomInset + panelWidth - focusWidth
                availableLogWidth = focusX - panelGap - (infoX + infoWidth)
            }
            if (availableLogWidth < minLogWidth) {
                val extraFocusShrink = minOf((focusWidth - 150f).coerceAtLeast(0f), minLogWidth - availableLogWidth)
                focusWidth -= extraFocusShrink
                focusX = bottomInset + panelWidth - focusWidth
                availableLogWidth = focusX - panelGap - (infoX + infoWidth)
            }
            val logWidth = minOf(preferredLogWidth, availableLogWidth.coerceAtLeast(minLogWidth))
            val logX = focusX - panelGap - logWidth
            return TileLayoutMetrics(
                mapOffsetY = mapOffsetY,
                worldWidth = worldWidth,
                worldHeight = worldHeight,
                sidebarX = mapWidthPx + sidebarGap,
                sidebarWidth = sidebarWidth,
                bottomInset = bottomInset,
                panelGap = panelGap,
                cardY = cardY,
                cardHeight = cardHeight,
                infoX = infoX,
                infoWidth = infoWidth,
                logX = logX,
                logWidth = logWidth,
                focusX = focusX,
                focusWidth = focusWidth,
                hotbarX = hotbarX,
                hotbarY = hotbarY,
                hotbarCardWidth = hotbarCardWidth,
                hotbarCardHeight = hotbarCardHeight,
                hotbarGap = hotbarGap,
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

        private fun truncateTextToWidth(
            text: String,
            maxWidth: Float,
            style: TileTextStyle,
        ): String = truncateText(text, maxCharsForWidth(maxWidth, style))

        private fun maxCharsForWidth(
            maxWidth: Float,
            style: TileTextStyle,
        ): Int = (maxWidth / approximateCharWidth(style)).toInt().coerceAtLeast(4)

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
            if (maxChars <= 0 || text.length <= maxChars) {
                return text
            }
            return text.take((maxChars - 1).coerceAtLeast(1)) + "…"
        }

        private fun tone(tone: TileTextTone): Color =
            when (tone) {
                TileTextTone.GOLD -> color("CCAA33")
                TileTextTone.WHITE -> color("DDDDDD")
                TileTextTone.LIGHT_GRAY -> color("AAAAAA")
                TileTextTone.CYAN -> color("33CCDD")
                TileTextTone.GRAY -> color("777777")
                TileTextTone.GREEN -> color("59C173")
                TileTextTone.RED -> color("D95959")
                TileTextTone.BLUE -> color("5C90D2")
                TileTextTone.MAGENTA -> color("7B1FA2")
            }

        private fun statusAccentColor(category: StatusEffectCategorySnapshot): Color =
            when (category) {
                StatusEffectCategorySnapshot.BUFF -> color("1F6A3B", 0.78f)
                StatusEffectCategorySnapshot.DEBUFF -> color("7A2B25", 0.80f)
                StatusEffectCategorySnapshot.NEUTRAL -> color("3A4353", 0.72f)
            }

        private fun statusBadgeColor(category: StatusEffectCategorySnapshot): Color =
            when (category) {
                StatusEffectCategorySnapshot.BUFF -> color("7FE0A0")
                StatusEffectCategorySnapshot.DEBUFF -> color("FF9A8D")
                StatusEffectCategorySnapshot.NEUTRAL -> Color.LIGHT_GRAY
            }

        internal fun color(
            hex: String,
            alpha: Float = 1f,
        ): Color = Color.valueOf(hex).also { it.a = alpha }
    }
}
