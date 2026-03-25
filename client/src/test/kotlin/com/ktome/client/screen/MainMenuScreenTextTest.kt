package com.ktome.client.screen

import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MainMenuScreenTextTest {
    @Test
    fun `profession resource hints resolve for every foundation profession`() {
        val zhLocalizer = LocalizationBundle.load().translator(GameLocale.ZH_CN)
        val enLocalizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        listOf("vanguard", "arcanist", "rogue", "templar").forEach { professionId ->
            val zhHint = professionResourceHint(zhLocalizer, professionId)
            val enHint = professionResourceHint(enLocalizer, professionId)

            assertFalse(zhHint.startsWith("!!"))
            assertFalse(enHint.startsWith("!!"))
        }
        assertTrue(professionResourceHint(zhLocalizer, "rogue").contains("能量"))
        assertTrue(professionResourceHint(enLocalizer, "templar").contains("POSITIVE"))
    }

    @Test
    fun `main menu footer spacing stays below the class entry stack`() {
        val lastEntryY = mainMenuClassEntryY(2)

        assertTrue(MAIN_MENU_FOOTER_LANGUAGE_Y + MAIN_MENU_FOOTER_LINE_HEIGHT <= lastEntryY)
        assertTrue(MAIN_MENU_FOOTER_CONTROLS_Y + MAIN_MENU_FOOTER_LINE_HEIGHT <= MAIN_MENU_FOOTER_LANGUAGE_Y)
        assertTrue(MAIN_MENU_FOOTER_NOTICE_Y + MAIN_MENU_FOOTER_LINE_HEIGHT <= MAIN_MENU_FOOTER_CONTROLS_Y)
    }

    @Test
    fun `profession selection label includes current race`() {
        val zhLocalizer = LocalizationBundle.load().translator(GameLocale.ZH_CN)
        val enLocalizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        assertEquals("Class: Vanguard  Race: Human", professionSelectionLabel(enLocalizer, "vanguard", "human"))
        assertEquals("职业：战卫  种族：矮人", professionSelectionLabel(zhLocalizer, "vanguard", "dwarf"))
    }
}
