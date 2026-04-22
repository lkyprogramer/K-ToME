package com.ktome.client.render

import com.ktome.client.render.layout.InfoSurfaceLayout
import com.ktome.client.render.layout.InfoSurfaceLayoutRequest
import com.ktome.client.render.layout.InfoSurfaceLayoutSolver
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
                    ),
            )

        assertTrue(metrics.worldWidth <= 1024f)
        assertTrue(metrics.worldHeight <= 768f)
        assertPanelsDoNotOverlap(metrics)
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
                    ),
            )

        assertTrue(metrics.worldWidth <= 1280f)
        assertTrue(metrics.worldHeight <= 800f)
        assertPanelsDoNotOverlap(metrics)
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
            )

        assertThrows(UnsupportedOperationException::class.java) {
            InfoSurfaceLayoutSolver.resolveMetrics(InfoSurfaceLayout.WideSplit, request)
        }
        assertThrows(UnsupportedOperationException::class.java) {
            InfoSurfaceLayoutSolver.resolveMetrics(InfoSurfaceLayout.ModalOverlay, request)
        }
    }

    private fun assertPanelsDoNotOverlap(metrics: TileLayoutMetrics) {
        assertTrue(metrics.infoX + metrics.infoWidth <= metrics.logX)
        assertTrue(metrics.logX + metrics.logWidth + metrics.panelGap <= metrics.focusX)
        assertTrue(metrics.focusX + metrics.focusWidth <= metrics.worldWidth - metrics.bottomInset + 0.5f)
        assertTrue(metrics.cardY + metrics.cardHeight <= metrics.mapOffsetY)
    }
}
