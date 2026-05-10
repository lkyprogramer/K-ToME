package com.ktome.client.render

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameShellLayoutTest {
    @Test
    fun `shell layout exposes left rail map right panel and bottom hud without overlap at 1280 by 800`() {
        val metrics = TileRenderer.layoutMetrics(mapWidth = 24, mapHeight = 18, cellWidth = 32f, cellHeight = 32f)
        val shell = metrics.shell

        assertTrue(metrics.worldWidth <= 1280f)
        assertTrue(metrics.worldHeight <= 800f)
        assertFalse(shell.leftRailBounds.overlaps(shell.mapBounds))
        assertFalse(shell.mapBounds.overlaps(shell.rightPanelBounds))
        assertFalse(shell.bottomHudBounds.overlaps(shell.mapBounds))
        assertTrue(shell.leftRailBounds.x < shell.mapBounds.x)
        assertTrue(shell.mapBounds.x < shell.rightPanelBounds.x)
    }

    @Test
    fun `shell layout keeps minimum breakpoint readable and map centered between rails`() {
        val metrics =
            TileRenderer.layoutMetrics(
                mapWidth = 18,
                mapHeight = 17,
                cellWidth = 32f,
                cellHeight = 32f,
                shellWorldWidth = 1024f,
                shellWorldHeight = 768f,
            )
        val shell = metrics.shell

        assertTrue(metrics.worldWidth <= 1024f)
        assertTrue(metrics.worldHeight <= 768f)
        assertTrue(shell.leftRailBounds.width >= 184f)
        assertTrue(shell.rightPanelBounds.width >= 240f)
        assertTrue(shell.mapBounds.x + shell.mapBounds.width <= shell.rightPanelBounds.x)
        assertTrue(shell.bottomHudBounds.height >= 224f)
    }

    @Test
    fun keepsMapAreaAtLeastHalfShellUsableContentAt1280x800() {
        val metrics = TileRenderer.layoutMetrics(mapWidth = 90, mapHeight = 56, cellWidth = 32f, cellHeight = 32f)
        val shell = metrics.shell
        val mapArea = shell.mapBounds.width * shell.mapBounds.height
        val usableArea = shell.shellContentBounds.width * shell.shellContentBounds.height

        assertTrue(mapArea >= usableArea * 0.5f)
    }

    @Test
    fun computesShellUsableContentAreaExcludingBottomHud() {
        val metrics = TileRenderer.layoutMetrics(mapWidth = 60, mapHeight = 40, cellWidth = 32f, cellHeight = 32f)

        assertEquals(metrics.shell.bottomHudBounds.top, metrics.shell.shellContentBounds.y)
        assertEquals(metrics.worldHeight - metrics.shell.bottomHudBounds.height, metrics.shell.shellContentBounds.height)
    }

    @Test
    fun producesModalSafeBoundsAboveFooterAndBottomLog() {
        val metrics = TileRenderer.layoutMetrics(mapWidth = 60, mapHeight = 40, cellWidth = 32f, cellHeight = 32f)

        assertTrue(metrics.shell.modalSafeBounds.bottom > metrics.shell.bottomHudBounds.top)
        assertTrue(metrics.shell.modalSafeBounds.top <= metrics.worldHeight)
        assertTrue(metrics.shell.bottomLogReservedBounds.top <= metrics.shell.bottomHudBounds.top)
    }

    @Test
    fun `viewport layout matrix locks fixed shell and cell aligned map bounds`() {
        val shellSizes = listOf(1024f to 768f, 1280f to 800f, 1440f to 900f)
        val mapDimensions = listOf(60 to 40, 70 to 45, 90 to 56)

        shellSizes.forEach { (shellWidth, shellHeight) ->
            mapDimensions.forEach { (mapWidth, mapHeight) ->
                val metrics =
                    TileRenderer.layoutMetrics(
                        mapWidth = mapWidth,
                        mapHeight = mapHeight,
                        cellWidth = 32f,
                        cellHeight = 32f,
                        shellWorldWidth = shellWidth,
                        shellWorldHeight = shellHeight,
                    )
                val shell = metrics.shell

                assertEquals(shellWidth, metrics.worldWidth)
                assertEquals(shellHeight, metrics.worldHeight)
                assertEquals(0, shell.cellAlignedMapBounds.width % 32)
                assertEquals(0, shell.cellAlignedMapBounds.height % 32)
                assertTrue(shell.cellAlignedMapBounds.width > 0)
                assertTrue(shell.cellAlignedMapBounds.height > 0)
                assertTrue(shell.cellAlignedMapBounds.width < mapWidth * 32)
                assertTrue(shell.cellAlignedMapBounds.height < mapHeight * 32)
                assertTrue(shell.leftRailBounds.right <= shell.mapBounds.x)
                assertTrue(shell.mapBounds.right <= shell.rightPanelBounds.x)
            }
        }
    }
}
