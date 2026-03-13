package com.ktome.client

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.map.Point
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.FloorSnapshot
import com.ktome.core.save.MapSnapshot
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.StairSnapshot
import java.nio.file.Path
import kotlin.io.path.exists
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
}

private fun sampleSnapshot(): SaveSnapshot =
    SaveSnapshot(
        timestampEpochMillis = 123L,
        seed = 20260312L,
        mapWidth = 80,
        mapHeight = 50,
        fovRadius = 8,
        messageLogSize = 8,
        currentFloor = 2,
        maxFloor = 5,
        turnCount = 18,
        messageLog = listOf("You descend the stairs."),
        player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = Point(4, 5),
                        name = "Hero",
                        isPlayerControlled = true,
                    ),
            ),
        floors =
            listOf(
                FloorSnapshot(
                    floor = 1,
                    map = MapSnapshot(rows = listOf("....."), playerStart = Point(0, 0)),
                    stairsDown = Point(4, 0),
                ),
                FloorSnapshot(
                    floor = 2,
                    map = MapSnapshot(rows = listOf("....."), playerStart = Point(1, 0)),
                    stairsUp = Point(0, 0),
                    stairsDown = Point(4, 0),
                    exploredTiles = listOf(Point(0, 0), Point(1, 0)),
                    entities =
                        listOf(
                            EntitySnapshot(
                                id = 9,
                                position = Point(4, 0),
                                glyph = '>',
                                name = "Downstairs",
                                stair = StairSnapshot(StairDirection.DOWN),
                            ),
                        ),
                ),
            ),
    )
