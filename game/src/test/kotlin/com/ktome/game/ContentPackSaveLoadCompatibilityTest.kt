package com.ktome.game

import com.ktome.core.save.InvalidSaveException
import com.ktome.core.save.SaveManager
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.contentpack.ContentPackFixtureCatalog
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ContentPackSaveLoadCompatibilityTest {
    @Test
    fun `save load fails fast when active pack environment changes`() {
        val saveDir = Files.createTempDirectory("ktome-content-pack-save")
        val saveManager = SaveManager(saveDir)
        val selection =
            ContentPackFixtureCatalog.selection(
                activePackRoots = listOf(ContentPackFixtureCatalog.samplePackRoot()),
            )
        try {
            val session =
                GameModule.newFoundationSession(
                    saveManager = saveManager,
                    contentPackSelection = selection,
                )
            session.perform(PlayerCommand.SaveGame)

            assertNotNull(GameModule.loadFoundationSession(saveManager, contentPackSelection = selection))
            assertThrows(InvalidSaveException::class.java) {
                GameModule.loadFoundationSession(saveManager, contentPackSelection = ContentPackSelection.EMPTY)
            }
        } finally {
            saveDir.toFile().deleteRecursively()
        }
    }
}
