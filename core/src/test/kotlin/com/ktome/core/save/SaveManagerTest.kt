package com.ktome.core.save

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class SaveManagerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `save and load round trips snapshot`() {
        val manager = SaveManager(tempDir)
        val snapshot = SaveFixtures.resourceHeavyScene()

        assertTrue(manager.save(snapshot))
        assertTrue(manager.hasSave())
        assertTrue(manager.hasSaveFile())
        assertEquals(snapshot, manager.load())
    }

    @Test
    fun `delete removes persisted save`() {
        val manager = SaveManager(tempDir)
        manager.save(SaveFixtures.emptyScene())

        manager.deleteSave()

        assertFalse(manager.hasSave())
        assertFalse(manager.hasSaveFile())
        assertNull(manager.load())
    }

    @Test
    fun `legacy save is rejected with explicit failure`() {
        val manager = SaveManager(tempDir)
        tempDir.createDirectories()
        manager.savePath().writeText("""{"version":999,"timestampEpochMillis":1}""")

        assertFalse(manager.hasSave())
        assertTrue(manager.hasSaveFile())
        assertThrows(LegacySaveFormatException::class.java) {
            manager.load()
        }
    }

    @Test
    fun `malformed file does not count as a save`() {
        val manager = SaveManager(tempDir)
        tempDir.createDirectories()
        manager.savePath().writeText("{ definitely not json")

        assertFalse(manager.hasSave())
        assertTrue(manager.hasSaveFile())
        assertThrows(MalformedSaveException::class.java) {
            manager.load()
        }
    }

    @Test
    fun `missing pr06 save field is rejected instead of defaulting`() {
        val manager = SaveManager(tempDir)
        val codec = SaveCodec()
        val json = Json { prettyPrint = true }
        val encoded = codec.encode(SaveFixtures.resourceHeavyScene())
        val root = json.parseToJsonElement(encoded).jsonObject
        val corrupted = JsonObject(root.filterKeys { key -> key != "headlessTurnEquivalent" })
        manager.savePath().parent.createDirectories()
        manager.savePath().writeText(json.encodeToString(JsonObject.serializer(), corrupted))

        assertThrows(InvalidSaveException::class.java) {
            manager.load()
        }
    }

    @Test
    fun `missing nested world progress fields are rejected instead of defaulting`() {
        val manager = SaveManager(tempDir)
        val codec = SaveCodec()
        val json = Json { prettyPrint = true }
        val encoded = codec.encode(SaveFixtures.resourceHeavyScene())
        val root = json.parseToJsonElement(encoded).jsonObject
        val worldProgress = root.getValue("worldProgress").jsonObject
        val corruptedWorldProgress = JsonObject(worldProgress.filterKeys { key -> key != "claimedRouteRewards" })
        val corrupted =
            JsonObject(
                root.toMutableMap().apply {
                    put("worldProgress", corruptedWorldProgress)
                },
            )
        manager.savePath().parent.createDirectories()
        manager.savePath().writeText(json.encodeToString(JsonObject.serializer(), corrupted))

        assertThrows(InvalidSaveException::class.java) {
            manager.load()
        }
    }
}
