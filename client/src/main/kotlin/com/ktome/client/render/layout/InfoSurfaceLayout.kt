package com.ktome.client.render.layout

import com.ktome.client.render.TileLayoutMetrics
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.game.PLAYER_ACTIVE_TALENT_SLOT_COUNT

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
        val mapOffsetY = tokens.fixed.shellBottomHudHeight.coerceAtLeast(request.uiRows * request.cellHeight)
        val panelGap = tokens.spacing.md
        val leftRailWidth =
            if (mapWidthPx >= 700f) {
                208f
            } else {
                tokens.fixed.shellLeftRailMinWidth
            }
        val rightPanelWidth =
            if (mapWidthPx >= 700f) {
                268f
            } else {
                tokens.fixed.shellRightPanelMinWidth
            }
        val worldWidth = leftRailWidth + panelGap + mapWidthPx + panelGap + rightPanelWidth
        val worldHeight = mapHeightPx + mapOffsetY
        val bottomInset = tokens.spacing.md
        val shell =
            GameShellLayout(
                leftRailBounds =
                    GameShellBounds(
                        x = 0f,
                        y = mapOffsetY,
                        width = leftRailWidth,
                        height = mapHeightPx,
                    ),
                mapBounds =
                    GameShellBounds(
                        x = leftRailWidth + panelGap,
                        y = mapOffsetY,
                        width = mapWidthPx,
                        height = mapHeightPx,
                    ),
                rightPanelBounds =
                    GameShellBounds(
                        x = leftRailWidth + panelGap + mapWidthPx + panelGap,
                        y = mapOffsetY,
                        width = rightPanelWidth,
                        height = mapHeightPx,
                    ),
                bottomHudBounds =
                    GameShellBounds(
                        x = 0f,
                        y = 0f,
                        width = worldWidth,
                        height = mapOffsetY,
                    ),
            )
        val hotbarX = bottomInset
        val hotbarY = tokens.spacing.xl + tokens.spacing.sm
        val hotbarGap = 12f
        val availableHotbarWidth =
            (worldWidth - bottomInset * 2 - hotbarGap * (PLAYER_ACTIVE_TALENT_SLOT_COUNT - 1)).coerceAtLeast(0f)
        val hotbarCardWidth = (availableHotbarWidth / PLAYER_ACTIVE_TALENT_SLOT_COUNT).coerceAtMost(156f)
        val hotbarCardHeight = 72f
        val cardY = hotbarY + hotbarCardHeight + tokens.spacing.md
        val cardHeight = (mapOffsetY - cardY - tokens.spacing.md).coerceAtLeast(88f)
        val panelWidth = worldWidth - bottomInset * 2
        val minLogWidth = 180f
        var infoWidth = (panelWidth * 0.28f).coerceIn(260f, 360f)
        var focusWidth = (panelWidth * 0.17f).coerceIn(280f, 340f)
        val infoX = bottomInset
        var focusX = bottomInset + panelWidth - focusWidth
        var availableLogWidth = focusX - panelGap - (infoX + infoWidth)
        if (availableLogWidth < minLogWidth) {
            var deficit = minLogWidth - availableLogWidth
            val infoShrink = minOf((infoWidth - 260f).coerceAtLeast(0f), deficit)
            infoWidth -= infoShrink
            deficit -= infoShrink
            val focusShrink = minOf((focusWidth - 250f).coerceAtLeast(0f), deficit)
            focusWidth -= focusShrink
            focusX = bottomInset + panelWidth - focusWidth
            availableLogWidth = focusX - panelGap - (infoX + infoWidth)
        }
        if (availableLogWidth < minLogWidth) {
            val extraFocusShrink = minOf((focusWidth - 150f).coerceAtLeast(0f), minLogWidth - availableLogWidth)
            focusWidth -= extraFocusShrink
            focusX = bottomInset + panelWidth - focusWidth
            availableLogWidth = focusX - panelGap - (infoX + infoWidth)
        }
        val logX = infoX + infoWidth + panelGap
        val logWidth = (focusX - panelGap - logX).coerceAtLeast(minLogWidth)
        return TileLayoutMetrics(
            shell = shell,
            mapOffsetY = mapOffsetY,
            worldWidth = worldWidth,
            worldHeight = worldHeight,
            sidebarX = shell.rightPanelBounds.x + tokens.spacing.sm,
            sidebarWidth = shell.rightPanelBounds.width - tokens.spacing.md,
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
