package com.ktome.game

import com.ktome.core.save.SaveManager
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FullGameLoopAutomationTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `scripted automation covers route transition save continue victory and permadeath`() {
        val saveManager = SaveManager(tempDir.resolve("automation-save-arcanist"))
        val driver =
            FullGameLoopAutomationDriver(
                saveManager = saveManager,
                config =
                    FoundationGameConfig(
                        seed = 20260313L,
                        zoneId = FOUNDATION_ZONE_ROUTE.first(),
                        playerProfessionId = "arcanist",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 0,
                    ),
            )

        val newGame = driver.newGame()
        assertEquals(1, newGame.currentFloor())
        assertEquals("shattered_outpost", newGame.config.zoneId)
        assertEquals(0, newGame.config.routeIndex)

        driver.advanceUntilZone(newGame, "greenwood_fringe")
        assertEquals("greenwood_fringe", newGame.config.zoneId)
        assertEquals(1, newGame.config.routeIndex)
        assertEquals(1, newGame.currentFloor())
        assertTrue(saveManager.hasSave(), "Route transitions should create an auto-save before the restart step.")

        val continued = driver.saveAndRestart(newGame)
        assertEquals("greenwood_fringe", continued.config.zoneId)
        assertEquals(1, continued.config.routeIndex)
        assertEquals(1, continued.currentFloor())

        driver.completeRunForVictory(continued)
        assertTrue(continued.isVictory())
        assertFalse(saveManager.hasSave(), "Victory should clear the single save slot.")

        val secondRun = driver.newGame()
        assertEquals(1, secondRun.currentFloor())
        assertTrue(secondRun.perform(PlayerCommand.SaveGame))
        assertTrue(saveManager.hasSave(), "Manual save should create a continue slot before the death check.")

        driver.forceGameOver(secondRun)
        assertTrue(secondRun.isGameOver())
        assertFalse(saveManager.hasSave(), "Permadeath must remove the save slot.")
        assertNull(GameModule.loadFoundationSession(saveManager), "Continue should be unavailable after death.")
    }

    @Test
    fun `scripted automation covers templar late route checkpoint continue on boss floor`() {
        val saveManager = SaveManager(tempDir.resolve("automation-save-templar-grey-gate"))
        val driver =
            FullGameLoopAutomationDriver(
                saveManager = saveManager,
                config =
                    FoundationGameConfig(
                        seed = 20260317L,
                        zoneId = "grey_gate_depths",
                        playerProfessionId = "templar",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 3,
                    ),
            )

        val session = driver.newGame()
        assertEquals("grey_gate_depths", session.config.zoneId)
        assertEquals("templar", session.config.playerProfessionId)
        assertEquals(3, session.config.routeIndex)

        driver.descendToFloor(session, targetFloor = 2)
        assertEquals(2, session.currentFloor())

        val continued = driver.saveAndRestart(session)
        assertEquals("grey_gate_depths", continued.config.zoneId)
        assertEquals("templar", continued.config.playerProfessionId)
        assertEquals(3, continued.config.routeIndex)
        assertEquals(2, continued.currentFloor())
    }
}
