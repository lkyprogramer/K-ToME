package com.ktome.client.render

import com.ktome.client.render.layout.GameShellBounds
import com.ktome.client.render.layout.RectInt
import com.ktome.client.ui.token.UiDesignTokens
import kotlin.math.roundToInt

internal const val TILE_TOOLTIP_BODY_LINE_LIMIT: Int = 5

internal object TileTooltipPlacementSolver {
    fun resolve(
        anchor: ResolvedTileOverlayAnchor,
        bodyLineCount: Int,
        shellContentBounds: GameShellBounds,
        bottomLogReservedBounds: GameShellBounds,
    ): RectInt {
        val padding = UiDesignTokens.fixed.tooltipPadding.roundToInt()
        val margin = UiDesignTokens.fixed.tooltipFlipMargin.roundToInt()
        val width = UiDesignTokens.fixed.tooltipMaxWidth.roundToInt()
        val visibleBodyLineCount = bodyLineCount.coerceIn(0, TILE_TOOLTIP_BODY_LINE_LIMIT)
        val height =
            minOf(
                UiDesignTokens.fixed.tooltipMaxHeight.roundToInt(),
                padding * 2 + 24 + visibleBodyLineCount * 22,
            ).coerceAtLeast(52)
        val bounds = anchor.bounds
        val minX = shellContentBounds.x.roundToInt()
        val maxX = shellContentBounds.right.roundToInt()
        val minY = maxOf(shellContentBounds.y.roundToInt(), bottomLogReservedBounds.top.roundToInt())
        val maxY = shellContentBounds.top.roundToInt()
        val candidates =
            listOf(
                RectInt(bounds.right + margin, bounds.y, width, height),
                RectInt(bounds.x, bounds.y - margin - height, width, height),
                RectInt(bounds.x - margin - width, bounds.y, width, height),
                RectInt(bounds.x, bounds.top + margin, width, height),
            )
        val fittingCandidate = candidates.firstOrNull { rect ->
            rect.x >= minX && rect.right <= maxX && rect.y >= minY && rect.top <= maxY
        }
        if (fittingCandidate != null) {
            return fittingCandidate
        }

        val clampedWidth = minOf(width, maxX - minX).coerceAtLeast(1)
        val clampedHeight = minOf(height, maxY - minY).coerceAtLeast(1)
        return RectInt(
            x = (bounds.right + margin).coerceIn(minX, maxX - clampedWidth),
            y = bounds.y.coerceIn(minY, maxY - clampedHeight),
            width = clampedWidth,
            height = clampedHeight,
        )
    }
}
