package com.ktome.client.render

import com.ktome.client.render.layout.InfoSurfaceLayout
import com.ktome.client.render.layout.InfoSurfaceLayoutRequest
import com.ktome.client.render.layout.InfoSurfaceLayoutSolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InfoSurfaceLayoutTest {
    @Test
    fun `map dominant layout fits 1024 by 768 breakpoint without panel overlap`() {
        val metrics =
            InfoSurfaceLayoutSolver.resolveMetrics(
                layout = InfoSurfaceLayout.MapDominant,
                request =
                    InfoSurfaceLayoutRequest(
                        mapWidth = 18,
                        mapHeight = 17,
                        cellWidth = 32f,
                        cellHeight = 32f,
                        uiRows = TileRenderer.uiRows,
                        shellWorldWidth = 1024f,
                        shellWorldHeight = 768f,
                    ),
            )

        assertTrue(metrics.worldWidth <= 1024f)
        assertTrue(metrics.worldHeight <= 768f)
        assertPanelsDoNotOverlap(metrics)
        assertBottomDeckContract(metrics, minDeckHeight = 164f, minDeckRatio = 0.20f, minHeroWidth = 200f)
    }

    @Test
    fun `map dominant layout fits 1280 by 800 breakpoint without panel overlap`() {
        val metrics =
            InfoSurfaceLayoutSolver.resolveMetrics(
                layout = InfoSurfaceLayout.MapDominant,
                request =
                    InfoSurfaceLayoutRequest(
                        mapWidth = 24,
                        mapHeight = 18,
                        cellWidth = 32f,
                        cellHeight = 32f,
                        uiRows = TileRenderer.uiRows,
                        shellWorldWidth = 1280f,
                        shellWorldHeight = 800f,
                    ),
            )

        assertTrue(metrics.worldWidth <= 1280f)
        assertTrue(metrics.worldHeight <= 800f)
        assertPanelsDoNotOverlap(metrics)
        assertBottomDeckContract(metrics, minDeckHeight = 180f, minDeckRatio = 0.22f, minHeroWidth = 208f)
    }

    @Test
    fun `map dominant layout keeps demo aspect bottom deck proportions`() {
        val metrics =
            InfoSurfaceLayoutSolver.resolveMetrics(
                layout = InfoSurfaceLayout.MapDominant,
                request =
                    InfoSurfaceLayoutRequest(
                        mapWidth = 32,
                        mapHeight = 20,
                        cellWidth = 32f,
                        cellHeight = 32f,
                        uiRows = TileRenderer.uiRows,
                        shellWorldWidth = 1672f,
                        shellWorldHeight = 941f,
                    ),
            )

        assertTrue(metrics.worldWidth <= 1672f)
        assertTrue(metrics.worldHeight <= 941f)
        assertPanelsDoNotOverlap(metrics)
        assertBottomDeckContract(metrics, minDeckHeight = 208f, minDeckRatio = 0.22f, minHeroWidth = 280f)
    }

    @Test
    fun `wide split and modal overlay are reserved layout contracts`() {
        val request =
            InfoSurfaceLayoutRequest(
                mapWidth = 18,
                mapHeight = 17,
                cellWidth = 32f,
                cellHeight = 32f,
                uiRows = TileRenderer.uiRows,
                shellWorldWidth = 1280f,
                shellWorldHeight = 800f,
            )

        assertThrows(UnsupportedOperationException::class.java) {
            InfoSurfaceLayoutSolver.resolveMetrics(InfoSurfaceLayout.WideSplit, request)
        }
        assertThrows(UnsupportedOperationException::class.java) {
            InfoSurfaceLayoutSolver.resolveMetrics(InfoSurfaceLayout.ModalOverlay, request)
        }
    }

    private fun assertPanelsDoNotOverlap(metrics: TileLayoutMetrics) {
        assertTrue(metrics.infoX + metrics.infoWidth <= metrics.focusX)
        assertTrue(metrics.focusX + metrics.focusWidth <= metrics.footerHintBounds.x)
        assertTrue(metrics.footerHintBounds.right <= metrics.logX)
        assertTrue(metrics.logX + metrics.logWidth <= metrics.demoShell.bottomDeck.bounds.right + 0.5f)
        assertTrue(metrics.cardY + metrics.cardHeight <= metrics.mapOffsetY)
    }

    private fun assertBottomDeckContract(
        metrics: TileLayoutMetrics,
        minDeckHeight: Float,
        minDeckRatio: Float,
        minHeroWidth: Float,
    ) {
        val bottom = metrics.demoShell.bottomDeck
        assertTrue(bottom.bounds.height >= minDeckHeight)
        assertTrue(bottom.bounds.height / metrics.worldHeight >= minDeckRatio)
        assertTrue(bottom.heroCard.width >= minHeroWidth)
        assertEquals(0f, bottom.bounds.x)
        assertTrue(bottom.bounds.right <= metrics.shell.rightPanelBounds.x)
        assertTrue(bottom.actionSlotBounds.isNotEmpty())
        bottom.actionSlotBounds.forEach { slot ->
            assertTrue(slot.width >= 68f)
            assertTrue(slot.height >= 68f)
            assertTrue(slot.x >= bottom.actionDeck.x)
            assertTrue(slot.right <= bottom.actionDeck.right)
        }
        assertTrue(bottom.actionSlotBounds.size >= 4)
        assertTrue(bottom.actionDeck.right <= bottom.logDeck.x)
        assertTrue(bottom.logDeck.right <= bottom.bounds.right)
    }
}
