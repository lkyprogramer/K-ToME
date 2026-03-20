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
    fun `scripted automation covers save continue victory and permadeath`() {
        listOf(
            "vanguard" to 20260312L,
            "arcanist" to 20260313L,
        ).forEach { (professionId, seed) ->
            val saveManager = SaveManager(tempDir.resolve("automation-save-$professionId"))
            val driver =
                FullGameLoopAutomationDriver(
                    saveManager = saveManager,
                    config = FoundationGameConfig(seed = seed, zoneId = "shattered_outpost", playerProfessionId = professionId),
                )

            val newGame = driver.newGame()
            assertEquals(1, newGame.currentFloor())

            driver.descendToFloor(newGame, 2)
            assertEquals(2, newGame.currentFloor())
            assertTrue(saveManager.hasSave(), "Floor transitions should create an auto-save before the restart step.")

            val continued = driver.saveAndRestart(newGame)
            assertEquals(2, continued.currentFloor())

            driver.killBossForVictory(continued)
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
    }
}
