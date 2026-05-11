package com.ktome.client.render.layout

import com.ktome.client.render.TileLayoutMetrics
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.game.PLAYER_ACTIVE_TALENT_SLOT_COUNT
import kotlin.math.floor
import kotlin.math.roundToInt

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
    val shellWorldWidth: Float = UiDesignTokens.fixed.shellPreferredWorldWidth,
    val shellWorldHeight: Float = UiDesignTokens.fixed.shellPreferredWorldHeight,
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
        require(request.cellWidth.roundToInt() == request.cellHeight.roundToInt()) {
            "Tile shell layout requires square cells."
        }
        val cellSize = request.cellWidth.roundToInt().coerceAtLeast(1)
        val worldWidth = request.shellWorldWidth.coerceAtLeast(tokens.fixed.shellMinWorldWidth)
        val worldHeight = request.shellWorldHeight.coerceAtLeast(tokens.fixed.shellMinWorldHeight)
        val mapOffsetY = tokens.fixed.shellBottomHudHeight.coerceAtLeast(request.uiRows * request.cellHeight)
        val panelGap = tokens.spacing.md
        val leftRailWidth = tokens.fixed.shellLeftRailMinWidth
        val rightPanelWidth = tokens.fixed.shellRightPanelMinWidth
        val mapWidthPx = (worldWidth - leftRailWidth - rightPanelWidth - panelGap * 2f).coerceAtLeast(request.cellWidth)
        val mapHeightPx = (worldHeight - mapOffsetY).coerceAtLeast(request.cellHeight)
        val rawMapX = leftRailWidth + panelGap
        val rawMapY = mapOffsetY
        val alignedMapWidth = floor(mapWidthPx / cellSize.toFloat()).toInt().coerceAtLeast(1) * cellSize
        val alignedMapHeight = floor(mapHeightPx / cellSize.toFloat()).toInt().coerceAtLeast(1) * cellSize
        val alignedMapX = (rawMapX + floor((mapWidthPx - alignedMapWidth) / 2f)).roundToInt()
        val alignedMapY = (rawMapY + floor((mapHeightPx - alignedMapHeight) / 2f)).roundToInt()
        val visibleColumns = minOf(request.mapWidth, alignedMapWidth / cellSize).coerceAtLeast(1)
        val visibleRows = minOf(request.mapHeight, alignedMapHeight / cellSize).coerceAtLeast(1)
        val innerPaddingX = ((alignedMapWidth - visibleColumns * cellSize) / 2).coerceAtLeast(0)
        val innerPaddingY = ((alignedMapHeight - visibleRows * cellSize) / 2).coerceAtLeast(0)
        val cellAlignedMapBounds = RectInt(alignedMapX, alignedMapY, alignedMapWidth, alignedMapHeight)
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
                        x = rawMapX,
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
                shellContentBounds =
                    GameShellBounds(
                        x = 0f,
                        y = mapOffsetY,
                        width = worldWidth,
                        height = mapHeightPx,
                    ),
                modalSafeBounds =
                    ModalSafeBounds(
                        left = bottomInset.roundToInt(),
                        right = (worldWidth - bottomInset).roundToInt(),
                        top = (worldHeight - tokens.spacing.md).roundToInt(),
                        bottom = (mapOffsetY + tokens.spacing.md).roundToInt(),
                    ),
                bottomLogReservedBounds =
                    GameShellBounds(
                        x = 0f,
                        y = tokens.spacing.lg,
                        width = worldWidth,
                        height = (tokens.fixed.shellBottomHudHeight * 0.42f).coerceAtLeast(84f),
                    ),
                cellAlignedMapBounds = cellAlignedMapBounds,
                mapInnerPadding =
                    InsetsInt(
                        left = innerPaddingX,
                        right = innerPaddingX,
                        top = innerPaddingY,
                        bottom = innerPaddingY,
                    ),
            )
        val hotbarX = bottomInset
        val hotbarY = tokens.spacing.xl + tokens.spacing.sm
        val hotbarGap = 12f
        val availableHotbarWidth =
            (worldWidth - bottomInset * 2 - hotbarGap * (PLAYER_ACTIVE_TALENT_SLOT_COUNT - 1)).coerceAtLeast(0f)
        val hotbarCardWidth = (availableHotbarWidth / PLAYER_ACTIVE_TALENT_SLOT_COUNT).coerceAtMost(156f)
        val hotbarCardHeight = 84f
        val cardY = hotbarY + hotbarCardHeight + tokens.spacing.md
        val cardHeight = (mapOffsetY - cardY - tokens.spacing.md).coerceAtLeast(96f)
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
        val hotbarTotalWidth = hotbarCardWidth * PLAYER_ACTIVE_TALENT_SLOT_COUNT + hotbarGap * (PLAYER_ACTIVE_TALENT_SLOT_COUNT - 1)
        val footerHintX = hotbarX + hotbarTotalWidth + hotbarGap
        val sideFooterWidth = worldWidth - bottomInset - footerHintX
        val footerHintBounds =
            if (sideFooterWidth >= 200f) {
                GameShellBounds(
                    x = footerHintX,
                    y = hotbarY,
                    width = sideFooterWidth,
                    height = hotbarCardHeight,
                )
            } else {
                GameShellBounds(
                    x = bottomInset,
                    y = cardY - tokens.spacing.sm - 28f,
                    width = panelWidth,
                    height = 28f,
                )
            }
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
            footerHintBounds = footerHintBounds,
        )
    }
}
