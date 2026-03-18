package com.ktome.game

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.save.SaveManager
import com.ktome.game.i18n.GameLocale
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LocaleSaveReloadSmokeTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `saved session re-renders using active locale on load`() {
        val saveManager = SaveManager(tempDir.resolve("locale-save"))
        val enSession = GameModule.newFoundationSession(FoundationGameConfig(seed = 20260318L), saveManager, GameLocale.EN_US)

        assertTrue(enSession.perform(PlayerCommand.SaveGame))

        val zhSession = GameModule.loadFoundationSession(saveManager, GameLocale.ZH_CN)
        assertNotNull(zhSession)
        val loaded = requireNotNull(zhSession)

        assertEquals(GameLocale.ZH_CN, loaded.localizer().locale)
        assertEquals("英雄", loaded.actorViews().single { it.isPlayer }.name)
        assertTrue(loaded.messageLog().contains("游戏已加载。"))

        val downstairs = requireNotNull(loaded.automationStairPoint(StairDirection.DOWN))
        loaded.automationMovePlayerTo(downstairs)
        assertEquals("下楼梯", loaded.inspectAt(downstairs).stairLabel)
    }

    @Test
    @Tag("headlessSmoke")
    fun `headless smoke keeps locale specific session text stable`() {
        val saveManager = SaveManager(tempDir.resolve("locale-headless-smoke"))
        val enSession = GameModule.newFoundationSession(FoundationGameConfig(seed = 20260319L), saveManager, GameLocale.EN_US)
        val zhSession = GameModule.newFoundationSession(FoundationGameConfig(seed = 20260319L), saveManager, GameLocale.ZH_CN)

        assertEquals("You enter the dungeon.", enSession.messageLog().first())
        assertEquals("你进入了地牢。", zhSession.messageLog().first())
        assertEquals("Hero", enSession.actorViews().single { it.isPlayer }.name)
        assertEquals("英雄", zhSession.actorViews().single { it.isPlayer }.name)
    }
}
