package com.ktome.client.render

import com.ktome.client.render.layout.RectInt
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileMapViewportTest {
    @Test
    fun centersSmallMapWithinBounds() {
        val viewport = viewport(dimensions = TileMapDimensions(4, 3), bounds = RectInt(10, 20, 320, 256), player = Point(1, 1))

        assertEquals(Point(0, 0), viewport.viewportTopLeft)
        assertEquals(TileVisibleRange(0, 4, 0, 3), viewport.visibleRange)
        assertEquals(96, viewport.innerPaddingX)
        assertEquals(80, viewport.innerPaddingY)
    }

    @Test
    fun centersSmallMapWithInnerPaddingAndZeroTopLeft() {
        val viewport = viewport(dimensions = TileMapDimensions(2, 2), bounds = RectInt(0, 0, 192, 192), player = Point(1, 1))

        assertEquals(Point(0, 0), viewport.viewportTopLeft)
        assertEquals(Point(64, 96), viewport.tileToScreen(Point(0, 0)))
    }

    @Test
    fun keepsTopLeftInsideDeadzone() {
        val first = viewport(player = Point(8, 8))
        val second = viewport(player = Point(9, 8), previous = first.state)

        assertEquals(first.viewportTopLeft, second.viewportTopLeft)
    }

    @Test
    fun keepsDeadzoneHalfOpenForEvenAndOddCells() {
        val even = viewport(bounds = RectInt(0, 0, 320, 320))
        val odd = viewport(bounds = RectInt(0, 0, 352, 352))

        assertEquals(even.horizontal.effectiveDeadzoneCells, even.horizontal.deadzoneEndExclusive - even.horizontal.deadzoneStart)
        assertEquals(odd.horizontal.effectiveDeadzoneCells, odd.horizontal.deadzoneEndExclusive - odd.horizontal.deadzoneStart)
    }

    @Test
    fun keepsThresholdEqualJumpOnDeadzonePath() {
        val first = viewport(player = Point(8, 8))
        val second = viewport(player = Point(8 + first.horizontal.effectiveDeadzoneCells, 8), previous = first.state)

        assertTrue(second.viewportTopLeft.x <= first.viewportTopLeft.x + first.horizontal.effectiveDeadzoneCells)
    }

    @Test
    fun snapsWhenHorizontalJumpExceedsThreshold() {
        val first = viewport(player = Point(8, 8))
        val second = viewport(player = Point(8 + first.horizontal.effectiveDeadzoneCells + 1, 8), previous = first.state)

        assertEquals(centeredTopLeft(second, second.state.lastPlayerTile).x, second.viewportTopLeft.x)
    }

    @Test
    fun snapsWhenVerticalJumpExceedsThreshold() {
        val first = viewport(player = Point(8, 8))
        val second = viewport(player = Point(8, 8 + first.vertical.effectiveDeadzoneCells + 1), previous = first.state)

        assertEquals(centeredTopLeft(second, second.state.lastPlayerTile).y, second.viewportTopLeft.y)
    }

    @Test
    fun keepsDiagonalMoveWhenNeitherAxisExceedsThreshold() {
        val first = viewport(player = Point(8, 8))
        val second =
            viewport(
                player = Point(8 + first.horizontal.effectiveDeadzoneCells, 8 + first.vertical.effectiveDeadzoneCells),
                previous = first.state,
            )

        assertFalse(second.viewportTopLeft == centeredTopLeft(second, second.state.lastPlayerTile))
    }

    @Test
    fun snapsWhenDiagonalAnyAxisExceedsThreshold() {
        val first = viewport(player = Point(8, 8))
        val second =
            viewport(
                player = Point(8 + first.horizontal.effectiveDeadzoneCells + 1, 8 + first.vertical.effectiveDeadzoneCells),
                previous = first.state,
            )

        assertEquals(centeredTopLeft(second, second.state.lastPlayerTile), second.viewportTopLeft)
    }

    @Test
    fun clampsBottomRightEdge() {
        val viewport = viewport(player = Point(59, 39))

        assertEquals(60 - viewport.visibleRange.columns, viewport.viewportTopLeft.x)
        assertEquals(40 - viewport.visibleRange.rows, viewport.viewportTopLeft.y)
    }

    @Test
    fun snapsBackToPlayerAfterInspect() {
        val inspect = viewport(player = Point(4, 4), focus = Point(30, 20), focusMode = TileViewportFocusMode.INSPECT)
        val player = viewport(player = Point(4, 4), focus = Point(4, 4), previous = inspect.state)

        assertEquals(centeredTopLeft(player, Point(4, 4)), player.viewportTopLeft)
        assertEquals(TileViewportFocusMode.PLAYER, player.state.lastFocusMode)
    }

    @Test
    fun snapsPlayerJumpWhileInspectFocusIsActive() {
        val first = viewport(player = Point(4, 4), focus = Point(30, 20), focusMode = TileViewportFocusMode.INSPECT)
        val second =
            viewport(
                player = Point(4 + first.horizontal.effectiveDeadzoneCells + 1, 4),
                focus = Point(30, 20),
                focusMode = TileViewportFocusMode.INSPECT,
                previous = first.state,
            )

        assertEquals(centeredTopLeft(second, second.state.lastPlayerTile), second.viewportTopLeft)
    }

    @Test
    fun snapsPlayerJumpWhileTargetingFocusIsActive() {
        val first = viewport(player = Point(4, 4), focus = Point(30, 20), focusMode = TileViewportFocusMode.TARGETING)
        val second =
            viewport(
                player = Point(4, 4 + first.vertical.effectiveDeadzoneCells + 1),
                focus = Point(30, 20),
                focusMode = TileViewportFocusMode.TARGETING,
                previous = first.state,
            )

        assertEquals(centeredTopLeft(second, second.state.lastPlayerTile), second.viewportTopLeft)
    }

    @Test
    fun producesCellAlignedVisibleRange() {
        val viewport = viewport(bounds = RectInt(3, 5, 319, 255), player = Point(8, 8))

        assertEquals(9, viewport.visibleRange.columns)
        assertEquals(7, viewport.visibleRange.rows)
        assertTrue(viewport.tileToScreen(viewport.viewportTopLeft).x >= 3)
    }

    @Test
    fun usesIntegerTileToScreenForEveryLayer() {
        val viewport = viewport(player = Point(8, 8))

        assertEquals(viewport.tileToScreen(Point(8, 8)), viewport.tileRect(Point(8, 8)).let { Point(it.x, it.y) })
    }

    @Test
    fun changesIdentityWhenCellAlignedMapBoundsChanges() {
        val first = viewport(bounds = RectInt(0, 0, 320, 320))
        val second = viewport(bounds = RectInt(0, 0, 352, 320), previous = first.state)

        assertFalse(first.state.identity == second.state.identity)
        assertEquals(centeredTopLeft(second, second.state.lastFocusTile), second.viewportTopLeft)
    }

    private fun viewport(
        dimensions: TileMapDimensions = TileMapDimensions(60, 40),
        bounds: RectInt = RectInt(0, 0, 320, 320),
        player: Point = Point(8, 8),
        focus: Point = player,
        focusMode: TileViewportFocusMode = TileViewportFocusMode.PLAYER,
        previous: TileMapViewportState? = null,
    ): TileMapViewport =
        TileMapViewport.resolve(
            identity =
                TileMapViewportIdentity(
                    zoneId = "test",
                    currentFloor = 1,
                    mapDimensions = dimensions,
                    cellSize = 32,
                    cellAlignedMapBounds = bounds,
                    focusMode = focusMode,
                ),
            playerTile = player,
            focusTile = focus,
            previousState = previous,
        )

    private fun centeredTopLeft(
        viewport: TileMapViewport,
        focus: Point,
    ): Point =
        Point(
            x = (focus.x - viewport.visibleRange.columns / 2).coerceIn(0, viewport.identity.mapDimensions.columns - viewport.visibleRange.columns),
            y = (focus.y - viewport.visibleRange.rows / 2).coerceIn(0, viewport.identity.mapDimensions.rows - viewport.visibleRange.rows),
        )
}
