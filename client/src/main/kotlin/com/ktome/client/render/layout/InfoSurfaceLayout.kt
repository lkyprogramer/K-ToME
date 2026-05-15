package com.ktome.client.render.layout

import com.ktome.client.render.TileLayoutMetrics
import com.ktome.client.ui.token.UiDesignTokens
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
        val worldWidth = request.shellWorldWidth.coerceAtLeast(tokens.fixed.shellMinWorldWidth)
        val worldHeight = request.shellWorldHeight.coerceAtLeast(tokens.fixed.shellMinWorldHeight)
        val panelGap = tokens.spacing.md
        val demoShell =
            DemoShellLayoutSolver.resolve(
                DemoShellLayoutRequest(
                    viewportWidth = worldWidth.roundToInt(),
                    viewportHeight = worldHeight.roundToInt(),
                    cellSize = request.cellWidth.roundToInt().coerceAtLeast(1),
                ),
            )
        val shell =
            GameShellLayout(
                leftRailBounds = demoShell.navRail,
                mapBounds = demoShell.mapStage,
                rightPanelBounds = demoShell.rightPanel,
                bottomHudBounds = demoShell.bottomDeck.bounds,
                shellContentBounds =
                    GameShellBounds(
                        x = 0f,
                        y = demoShell.bottomDeck.bounds.top,
                        width = worldWidth,
                        height = worldHeight - demoShell.bottomDeck.bounds.top,
                    ),
                modalSafeBounds = demoShell.modalSafeBounds,
                bottomLogReservedBounds = demoShell.bottomDeck.logDeck,
                cellAlignedMapBounds = demoShell.mapContentBounds,
                mapInnerPadding = demoShell.mapInnerPadding,
            )
        val bottom = demoShell.bottomDeck
        val firstActionSlot = bottom.actionSlotBounds.first()
        val secondActionSlot = bottom.actionSlotBounds.getOrNull(1)
        val hotbarGap = secondActionSlot?.let { slot -> slot.x - firstActionSlot.right } ?: panelGap
        val zeroWidthFooterHints = GameShellBounds(bottom.actionDeck.right, bottom.actionDeck.y, 0f, bottom.actionDeck.height)
        return TileLayoutMetrics(
            shell = shell,
            demoShell = demoShell,
            mapOffsetY = demoShell.bottomDeck.bounds.top,
            worldWidth = worldWidth,
            worldHeight = worldHeight,
            sidebarX = shell.rightPanelBounds.x + tokens.spacing.sm,
            sidebarWidth = shell.rightPanelBounds.width - tokens.spacing.md,
            bottomInset = bottom.heroCard.x,
            panelGap = panelGap,
            cardY = bottom.heroCard.y,
            cardHeight = bottom.heroCard.height,
            infoX = bottom.heroCard.x,
            infoWidth = bottom.heroCard.width,
            logX = bottom.logDeck.x,
            logWidth = bottom.logDeck.width,
            focusX = bottom.actionDeck.x,
            focusWidth = bottom.actionDeck.width,
            hotbarX = firstActionSlot.x,
            hotbarY = firstActionSlot.y,
            hotbarCardWidth = firstActionSlot.width,
            hotbarCardHeight = firstActionSlot.height,
            hotbarGap = hotbarGap,
            footerHintBounds = zeroWidthFooterHints,
        )
    }
}
