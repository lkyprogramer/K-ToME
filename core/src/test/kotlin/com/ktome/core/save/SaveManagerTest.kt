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
}
