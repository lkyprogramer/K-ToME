package com.ktome.client.screen

import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FoundationViewportSupportTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `syncViewport updates world size when snapshot dimensions change`() {
        val initialSnapshot =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("viewport-left")),
            ).renderSnapshot()
        val largerSnapshot =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "greenwood_fringe", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("viewport-right")),
            ).renderSnapshot()

        val viewport = FitViewport(FoundationViewportSupport.worldWidth(initialSnapshot), FoundationViewportSupport.worldHeight(initialSnapshot))

        assertTrue(FoundationViewportSupport.syncViewport(viewport, largerSnapshot, 1280, 720))
        assertEquals(FoundationViewportSupport.worldWidth(largerSnapshot), viewport.worldWidth)
        assertEquals(FoundationViewportSupport.worldHeight(largerSnapshot), viewport.worldHeight)
        assertFalse(FoundationViewportSupport.syncViewport(viewport, largerSnapshot, 1280, 720))
    }
}
