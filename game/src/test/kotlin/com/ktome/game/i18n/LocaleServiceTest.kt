package com.ktome.game.i18n

import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocaleServiceTest {
    @Test
    fun `bundle resolves translated text and placeholders`() {
        val bundle = LocalizationBundle.load()

        assertEquals("Quick Start", bundle.translator(GameLocale.EN_US).text("ui.menu.action.quick-start"))
        assertEquals("快速开始", bundle.translator(GameLocale.ZH_CN).text("ui.menu.action.quick-start"))
        assertEquals("语言：简体中文", bundle.translator(GameLocale.ZH_CN).text("ui.menu.language", "value" to "简体中文"))
    }

    @Test
    fun `locale cycle is stable`() {
        assertEquals(GameLocale.ZH_CN, GameLocale.EN_US.cycle())
        assertEquals(GameLocale.EN_US, GameLocale.ZH_CN.cycle())
    }

    @Test
    fun `bundle can be loaded repeatedly from classpath`() {
        repeat(128) {
            val bundle = LocalizationBundle.load()

            assertEquals("快速开始", bundle.translator(GameLocale.ZH_CN).text("ui.menu.action.quick-start"))
        }
    }

    @Test
    fun `ui glyph catalog can be loaded repeatedly from classpath`() {
        repeat(64) {
            val glyphs = UiGlyphCatalog.requiredGlyphs()

            assertTrue(glyphs.contains('新'))
            assertTrue(glyphs.contains('N'))
        }
    }

    @Test
    fun `schema-only data loader path does not require localization bundle`() {
        val catalog =
            DataLoader(
                locale = GameLocale.ZH_CN,
                localizationBundleProvider = { error("Schema-only loading must not initialize localization bundle.") },
            ).loadSchemaCatalog()

        assertTrue(catalog.professions.isNotEmpty())
    }
}
