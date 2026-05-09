package com.ktome.client.render

import org.junit.jupiter.api.Assertions.assertFalse
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
        val metrics = TileRenderer.layoutMetrics(mapWidth = 18, mapHeight = 17, cellWidth = 32f, cellHeight = 32f)
        val shell = metrics.shell

        assertTrue(metrics.worldWidth <= 1024f)
        assertTrue(metrics.worldHeight <= 768f)
        assertTrue(shell.leftRailBounds.width >= 184f)
        assertTrue(shell.rightPanelBounds.width >= 240f)
        assertTrue(shell.mapBounds.x + shell.mapBounds.width <= shell.rightPanelBounds.x)
        assertTrue(shell.bottomHudBounds.height >= 224f)
    }
}
