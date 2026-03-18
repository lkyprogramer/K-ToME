package com.ktome.game.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocaleServiceTest {
    @Test
    fun `bundle resolves translated text and placeholders`() {
        val bundle = LocalizationBundle.load()

        assertEquals("New Game", bundle.translator(GameLocale.EN_US).text("ui.menu.new_game"))
        assertEquals("新游戏", bundle.translator(GameLocale.ZH_CN).text("ui.menu.new_game"))
        assertEquals("语言：简体中文", bundle.translator(GameLocale.ZH_CN).text("ui.menu.language", "value" to "简体中文"))
    }

    @Test
    fun `locale cycle is stable`() {
        assertEquals(GameLocale.ZH_CN, GameLocale.EN_US.cycle())
        assertEquals(GameLocale.EN_US, GameLocale.ZH_CN.cycle())
    }
}
