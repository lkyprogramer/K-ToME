package com.ktome.client

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.FloorSnapshot
import com.ktome.core.save.MapSnapshot
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.StairSnapshot
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GameAppLifecycleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `refresh availability reflects loadable save`() {
        val saveManager = SaveManager(tempDir.resolve("lifecycle-save"))
        saveManager.save(sampleSnapshot())
        val coordinator = LifecycleCoordinator(saveManager)

        assertTrue(coordinator.refreshContinueAvailability())
        assertTrue(coordinator.cachedContinueAvailability())
    }

    @Test
    fun `start new session only deletes save after creation`() {
        val saveManager = SaveManager(tempDir.resolve("new-game-save"))
        saveManager.save(sampleSnapshot())
        val coordinator = LifecycleCoordinator(saveManager)

        assertThrows(IllegalStateException::class.java) {
            coordinator.startNewSession<Any> { throw IllegalStateException("Initialization failed") }
        }
        assertTrue(saveManager.savePath().exists())

        coordinator.startNewSession { Any() }
        assertFalse(saveManager.savePath().exists())
    }

    @Test
    fun `continue session clears availability when loader fails`() {
        val saveManager = SaveManager(tempDir.resolve("continue-save"))
        saveManager.save(sampleSnapshot())
        val coordinator = LifecycleCoordinator(saveManager)
        coordinator.refreshContinueAvailability()
        assertTrue(coordinator.cachedContinueAvailability())

        val session = coordinator.continueSession<Any> { null }
        assertNull(session)
        assertFalse(coordinator.cachedContinueAvailability())
    }

    @Test
    fun `legacy save surfaces an explicit notice`() {
        val saveManager = SaveManager(tempDir.resolve("legacy-save"))
        Files.createDirectories(saveManager.savePath().parent)
        saveManager.savePath().writeText("""{"version":2,"timestampEpochMillis":123}""")
        val coordinator = LifecycleCoordinator(saveManager)

        assertFalse(coordinator.refreshContinueAvailability())
        assertTrue(coordinator.consumeNotice()?.contains("Legacy saves") == true)
    }
}

private fun sampleSnapshot(): SaveSnapshot =
    SaveSnapshot(
        timestampEpochMillis = 123L,
        worldSeed = 20260312L,
        currentZoneId = "foundation_dungeon",
        floorIndex = 2,
        mapWidth = 80,
        mapHeight = 50,
        fovRadius = 8,
        messageLogSize = 8,
        playerProfessionId = "foundation_hero",
        maxFloor = 5,
        turnCount = 18,
        player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = PointSnapshot(4, 5),
                        blocksMovement = true,
                        faction = "PLAYER",
                        isPlayerControlled = true,
                    ),
            ),
        floors =
            listOf(
                FloorSnapshot(
                    floorIndex = 1,
                    map = MapSnapshot(rows = listOf("....."), playerStart = PointSnapshot(0, 0)),
                    stairsDown = PointSnapshot(4, 0),
                ),
                FloorSnapshot(
                    floorIndex = 2,
                    map = MapSnapshot(rows = listOf("....."), playerStart = PointSnapshot(1, 0)),
                    stairsUp = PointSnapshot(0, 0),
                    stairsDown = PointSnapshot(4, 0),
                    exploredTiles = listOf(PointSnapshot(0, 0), PointSnapshot(1, 0)),
                    entities =
                        listOf(
                            EntitySnapshot(
                                id = 9,
                                position = PointSnapshot(4, 0),
                                stair = StairSnapshot(StairDirection.DOWN.name),
                            ),
                        ),
                ),
            ),
    )
