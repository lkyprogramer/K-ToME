package com.ktome.core.save

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.map.Point
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SaveManagerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `save and load round trips snapshot`() {
        val manager = SaveManager(tempDir)
        val snapshot = sampleSnapshot()

        assertTrue(manager.save(snapshot))
        assertTrue(manager.hasSave())
        assertTrue(manager.hasSaveFile())
        assertEquals(snapshot, manager.load())
    }

    @Test
    fun `delete removes persisted save`() {
        val manager = SaveManager(tempDir)
        manager.save(sampleSnapshot())

        manager.deleteSave()

        assertFalse(manager.hasSave())
        assertFalse(manager.hasSaveFile())
        assertNull(manager.load())
    }

    @Test
    fun `load rejects mismatched version`() {
        val manager = SaveManager(tempDir)
        tempDir.createDirectories()
        manager.savePath().writeText(
            """
            {
              "version": 999,
              "timestampEpochMillis": 1,
              "seed": 42,
              "mapWidth": 80,
              "mapHeight": 50,
              "fovRadius": 8,
              "messageLogSize": 8,
              "currentFloor": 1,
              "maxFloor": 5,
              "turnCount": 7,
              "messageLog": [],
              "player": { "entity": { "id": 1 } },
              "floors": [
                {
                  "floor": 1,
                  "map": {
                    "rows": ["..."],
                    "playerStart": { "x": 0, "y": 0 }
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        assertFalse(manager.hasSave())
        assertNull(manager.load())
    }

    @Test
    fun `malformed file does not count as a save`() {
        val manager = SaveManager(tempDir)
        tempDir.createDirectories()
        manager.savePath().writeText("{ definitely not json")

        assertFalse(manager.hasSave())
        assertTrue(manager.hasSaveFile())
        assertNull(manager.load())
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
                    carriedEntities =
                        listOf(
                            EntitySnapshot(
                                id = 2,
                                name = "Travel Ration",
                                itemInstance =
                                    com.ktome.core.item.ItemInstance(
                                        baseId = "ration",
                                        name = "Travel Ration",
                                        type = com.ktome.core.item.ItemType.CONSUMABLE,
                                        glyph = '!',
                                        colorHex = "#FFFFFF",
                                    ),
                            ),
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
                                    colorHex = "#FFFFFF",
                                    name = "Downstairs",
                                    stair = StairSnapshot(StairDirection.DOWN),
                                ),
                            ),
                    ),
                ),
        )
}
