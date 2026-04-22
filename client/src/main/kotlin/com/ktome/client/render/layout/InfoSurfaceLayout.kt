package com.ktome.client.render.layout

import com.ktome.client.render.TileLayoutMetrics
import com.ktome.client.ui.token.UiDesignTokens

internal sealed interface InfoSurfaceLayout {
    data object MapDominant : InfoSurfaceLayout

    data object WideSplit : InfoSurfaceLayout

    data object ModalOverlay : InfoSurfaceLayout
}

internal data class InfoSurfaceLayoutRequest(
    val mapWidth: Int,
    val mapHeight: Int,
    val cellWidth: Float,
    val cellHeight: Float,
    val uiRows: Int,
)

internal object InfoSurfaceLayoutSolver {
    fun resolveMetrics(
        layout: InfoSurfaceLayout,
        request: InfoSurfaceLayoutRequest,
    ): TileLayoutMetrics =
        when (layout) {
            InfoSurfaceLayout.MapDominant -> mapDominantMetrics(request)
            InfoSurfaceLayout.WideSplit,
            InfoSurfaceLayout.ModalOverlay -> throw UnsupportedOperationException("$layout is reserved for a later UI layout PR.")
        }

    private fun mapDominantMetrics(request: InfoSurfaceLayoutRequest): TileLayoutMetrics {
        val tokens = UiDesignTokens
        val mapWidthPx = request.mapWidth * request.cellWidth
        val mapHeightPx = request.mapHeight * request.cellHeight
        val mapOffsetY = request.uiRows * request.cellHeight
        val sidebarGap = tokens.spacing.lg
        val sidebarWidth = (mapWidthPx * 0.55f).coerceIn(340f, 420f)
        val worldWidth = mapWidthPx + sidebarGap + sidebarWidth + tokens.spacing.lg
        val worldHeight = mapHeightPx + mapOffsetY
        val bottomInset = tokens.spacing.md
        val panelGap = tokens.spacing.md
        val hotbarX = bottomInset
        val hotbarY = tokens.spacing.md
        val hotbarCardWidth = 126f
        val hotbarCardHeight = 84f
        val hotbarGap = 14f
        val cardY = hotbarY + hotbarCardHeight + tokens.spacing.md
        val cardHeight = (mapOffsetY - cardY - tokens.spacing.md).coerceAtLeast(96f)
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
}
