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
            val zhHint = resourceHintText(zhLocalizer, "profession.$professionId.resource_hint")
            val enHint = resourceHintText(enLocalizer, "profession.$professionId.resource_hint")

            assertFalse(zhHint.startsWith("!!"))
            assertFalse(enHint.startsWith("!!"))
        }
        assertTrue(resourceHintText(zhLocalizer, "profession.rogue.resource_hint").contains("能量"))
        assertTrue(resourceHintText(enLocalizer, "profession.templar.resource_hint").contains("POSITIVE"))
    }

    @Test
    fun `main menu footer spacing stays below the class entry stack`() {
        val lastEntryY = mainMenuClassEntryY(2)

        assertTrue(MAIN_MENU_FOOTER_LANGUAGE_Y + MAIN_MENU_FOOTER_LINE_HEIGHT <= lastEntryY)
        assertTrue(MAIN_MENU_FOOTER_CONTROLS_Y + MAIN_MENU_FOOTER_LINE_HEIGHT <= MAIN_MENU_FOOTER_LANGUAGE_Y)
        assertTrue(MAIN_MENU_FOOTER_NOTICE_Y + MAIN_MENU_FOOTER_LINE_HEIGHT <= MAIN_MENU_FOOTER_CONTROLS_Y)
    }

    @Test
    fun `selection labels keep profession and race independent`() {
        val zhLocalizer = LocalizationBundle.load().translator(GameLocale.ZH_CN)
        val enLocalizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        assertEquals("Class: Vanguard", selectionLabel(enLocalizer, "ui.menu.profession", "profession.vanguard.name"))
        assertEquals("Race: Human", selectionLabel(enLocalizer, "ui.menu.race", "race.human.name"))
        assertEquals("职业：战卫", selectionLabel(zhLocalizer, "ui.menu.profession", "profession.vanguard.name"))
        assertEquals("种族：矮人", selectionLabel(zhLocalizer, "ui.menu.race", "race.dwarf.name"))
    }

    @Test
    fun `selection state text resolves through generic availability keys`() {
        val zhLocalizer = LocalizationBundle.load().translator(GameLocale.ZH_CN)
        val enLocalizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        assertEquals("Status: PLAYABLE", selectionStateText(enLocalizer, com.ktome.core.profile.ClassPlayabilityState.PLAYABLE))
        assertEquals("状态：锁定", selectionStateText(zhLocalizer, com.ktome.core.profile.ClassPlayabilityState.LOCKED))
    }
}
