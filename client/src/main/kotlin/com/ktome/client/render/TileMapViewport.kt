package com.ktome.client.render

import com.ktome.client.render.layout.RectInt
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.map.Point
import kotlin.math.abs
import kotlin.math.floor

internal data class TileMapDimensions(
    val columns: Int,
    val rows: Int,
) {
    init {
        require(columns > 0) { "Tile map columns must be positive." }
        require(rows > 0) { "Tile map rows must be positive." }
    }
}

internal enum class TileViewportFocusMode {
    PLAYER,
    INSPECT,
    TARGETING,
}

internal data class TileMapViewportIdentity(
    val zoneId: String,
    val currentFloor: Int,
    val mapDimensions: TileMapDimensions,
    val cellSize: Int,
    val cellAlignedMapBounds: RectInt,
    val focusMode: TileViewportFocusMode,
)

internal data class TileVisibleRange(
    val startX: Int,
    val endXExclusive: Int,
    val startY: Int,
    val endYExclusive: Int,
) {
    init {
        require(endXExclusive >= startX) { "Visible range X end must be >= start." }
        require(endYExclusive >= startY) { "Visible range Y end must be >= start." }
    }

    val columns: Int get() = endXExclusive - startX
    val rows: Int get() = endYExclusive - startY

    fun contains(tile: Point): Boolean =
        tile.x in startX until endXExclusive &&
            tile.y in startY until endYExclusive
}

internal data class TileViewportAxisDebug(
    val visibleCells: Int,
    val effectiveDeadzoneCells: Int,
    val deadzoneStart: Int,
    val deadzoneEndExclusive: Int,
)

internal data class TileMapViewportState(
    val identity: TileMapViewportIdentity,
    val viewportTopLeft: Point,
    val visibleRange: TileVisibleRange,
    val lastPlayerTile: Point,
    val lastFocusTile: Point,
    val lastFocusMode: TileViewportFocusMode,
    val horizontal: TileViewportAxisDebug,
    val vertical: TileViewportAxisDebug,
)

internal data class TileMapViewport(
    val identity: TileMapViewportIdentity,
    val mapBounds: RectInt,
    val viewportTopLeft: Point,
    val visibleRange: TileVisibleRange,
    val cellSize: Int,
    val innerPaddingX: Int,
    val innerPaddingY: Int,
    val horizontal: TileViewportAxisDebug,
    val vertical: TileViewportAxisDebug,
    val state: TileMapViewportState,
) {
    fun containsTile(tile: Point): Boolean = visibleRange.contains(tile)

    fun tileToScreen(tile: Point): Point {
        require(containsTile(tile)) { "Tile $tile is outside visible range $visibleRange." }
        val localX = tile.x - viewportTopLeft.x
        val localY = tile.y - viewportTopLeft.y
        return Point(
            x = mapBounds.x + innerPaddingX + localX * cellSize,
            y = mapBounds.y + innerPaddingY + (visibleRange.rows - localY - 1) * cellSize,
        )
    }

    fun tileRect(tile: Point): RectInt {
        val bottomLeft = tileToScreen(tile)
        return RectInt(bottomLeft.x, bottomLeft.y, cellSize, cellSize)
    }

    companion object {
        fun resolve(
            identity: TileMapViewportIdentity,
            playerTile: Point,
            focusTile: Point,
            previousState: TileMapViewportState?,
        ): TileMapViewport {
            val visibleColumns = (identity.cellAlignedMapBounds.width / identity.cellSize).coerceAtLeast(1)
            val visibleRows = (identity.cellAlignedMapBounds.height / identity.cellSize).coerceAtLeast(1)
            val clampedVisibleColumns = minOf(visibleColumns, identity.mapDimensions.columns)
            val clampedVisibleRows = minOf(visibleRows, identity.mapDimensions.rows)
            val horizontalDebug =
                axisDebug(
                    visibleCells = clampedVisibleColumns,
                    minCells = UiDesignTokens.fixed.deadzoneHorizontalMinCells,
                    ratio = UiDesignTokens.fixed.deadzoneHorizontalRatio,
                )
            val verticalDebug =
                axisDebug(
                    visibleCells = clampedVisibleRows,
                    minCells = UiDesignTokens.fixed.deadzoneVerticalMinCells,
                    ratio = UiDesignTokens.fixed.deadzoneVerticalRatio,
                )

            val topLeft =
                if (previousState == null || previousState.identity != identity) {
                    centeredTopLeft(identity.mapDimensions, clampedVisibleColumns, clampedVisibleRows, focusTile)
                } else if (shouldSnapPlayerJump(previousState, playerTile, horizontalDebug, verticalDebug)) {
                    centeredTopLeft(identity.mapDimensions, clampedVisibleColumns, clampedVisibleRows, playerTile)
                } else {
                    deadzoneTopLeft(
                        dimensions = identity.mapDimensions,
                        visibleColumns = clampedVisibleColumns,
                        visibleRows = clampedVisibleRows,
                        previousTopLeft = previousState.viewportTopLeft,
                        focusTile = focusTile,
                        horizontal = horizontalDebug,
                        vertical = verticalDebug,
                    )
                }
            val visibleRange =
                TileVisibleRange(
                    startX = topLeft.x,
                    endXExclusive = topLeft.x + clampedVisibleColumns,
                    startY = topLeft.y,
                    endYExclusive = topLeft.y + clampedVisibleRows,
                )
            val innerPaddingX = ((identity.cellAlignedMapBounds.width - clampedVisibleColumns * identity.cellSize) / 2).coerceAtLeast(0)
            val innerPaddingY = ((identity.cellAlignedMapBounds.height - clampedVisibleRows * identity.cellSize) / 2).coerceAtLeast(0)
            val state =
                TileMapViewportState(
                    identity = identity,
                    viewportTopLeft = topLeft,
                    visibleRange = visibleRange,
                    lastPlayerTile = playerTile,
                    lastFocusTile = focusTile,
                    lastFocusMode = identity.focusMode,
                    horizontal = horizontalDebug,
                    vertical = verticalDebug,
                )
            return TileMapViewport(
                identity = identity,
                mapBounds = identity.cellAlignedMapBounds,
                viewportTopLeft = topLeft,
                visibleRange = visibleRange,
                cellSize = identity.cellSize,
                innerPaddingX = innerPaddingX,
                innerPaddingY = innerPaddingY,
                horizontal = horizontalDebug,
                vertical = verticalDebug,
                state = state,
            )
        }

        private fun axisDebug(
            visibleCells: Int,
            minCells: Int,
            ratio: Float,
        ): TileViewportAxisDebug {
            val cap = maxOf(0, visibleCells - 2)
            val effective = maxOf(minCells, floor(visibleCells * ratio).toInt()).coerceAtMost(cap)
            val centerIndex = visibleCells / 2
            val start =
                if (effective == 0) {
                    centerIndex
                } else {
                    (centerIndex - effective / 2).coerceIn(0, visibleCells - effective)
                }
            return TileViewportAxisDebug(
                visibleCells = visibleCells,
                effectiveDeadzoneCells = effective,
                deadzoneStart = start,
                deadzoneEndExclusive = start + effective,
            )
        }

        private fun shouldSnapPlayerJump(
            previousState: TileMapViewportState,
            playerTile: Point,
            horizontal: TileViewportAxisDebug,
            vertical: TileViewportAxisDebug,
        ): Boolean {
            val dx = playerTile.x - previousState.lastPlayerTile.x
            val dy = playerTile.y - previousState.lastPlayerTile.y
            return abs(dx) > horizontal.effectiveDeadzoneCells || abs(dy) > vertical.effectiveDeadzoneCells
        }

        private fun centeredTopLeft(
            dimensions: TileMapDimensions,
            visibleColumns: Int,
            visibleRows: Int,
            focusTile: Point,
        ): Point =
            Point(
                x = centeredAxisTopLeft(dimensions.columns, visibleColumns, focusTile.x),
                y = centeredAxisTopLeft(dimensions.rows, visibleRows, focusTile.y),
            )

        private fun centeredAxisTopLeft(
            mapCells: Int,
            visibleCells: Int,
            focus: Int,
        ): Int {
            if (mapCells <= visibleCells) {
                return 0
            }
            return (focus - visibleCells / 2).coerceIn(0, mapCells - visibleCells)
        }

        private fun deadzoneTopLeft(
            dimensions: TileMapDimensions,
            visibleColumns: Int,
            visibleRows: Int,
            previousTopLeft: Point,
            focusTile: Point,
            horizontal: TileViewportAxisDebug,
            vertical: TileViewportAxisDebug,
        ): Point =
            Point(
                x =
                    deadzoneAxisTopLeft(
                        mapCells = dimensions.columns,
                        visibleCells = visibleColumns,
                        previousTopLeft = previousTopLeft.x,
                        focus = focusTile.x,
                        debug = horizontal,
                    ),
                y =
                    deadzoneAxisTopLeft(
                        mapCells = dimensions.rows,
                        visibleCells = visibleRows,
                        previousTopLeft = previousTopLeft.y,
                        focus = focusTile.y,
                        debug = vertical,
                    ),
            )

        private fun deadzoneAxisTopLeft(
            mapCells: Int,
            visibleCells: Int,
            previousTopLeft: Int,
            focus: Int,
            debug: TileViewportAxisDebug,
        ): Int {
            if (mapCells <= visibleCells) {
                return 0
            }
            if (debug.effectiveDeadzoneCells == 0) {
                return centeredAxisTopLeft(mapCells, visibleCells, focus)
            }
            val focusLocalIndex = focus - previousTopLeft
            val nextTopLeft =
                when {
                    focusLocalIndex in debug.deadzoneStart until debug.deadzoneEndExclusive -> previousTopLeft
                    focusLocalIndex < debug.deadzoneStart -> focus - debug.deadzoneStart
                    else -> focus - (debug.deadzoneEndExclusive - 1)
                }
            return nextTopLeft.coerceIn(0, mapCells - visibleCells)
        }
    }
}
