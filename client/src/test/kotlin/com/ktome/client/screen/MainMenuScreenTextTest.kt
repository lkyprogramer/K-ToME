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
        val entryCount = 4
        val lastEntryY = mainMenuLastEntryY(entryCount)

        assertTrue(mainMenuFooterLanguageY(entryCount) + MAIN_MENU_FOOTER_LINE_HEIGHT <= lastEntryY)
        assertTrue(mainMenuFooterControlsY(entryCount) + MAIN_MENU_FOOTER_LINE_HEIGHT <= mainMenuFooterLanguageY(entryCount))
        assertTrue(mainMenuFooterNoticeY(entryCount) + MAIN_MENU_FOOTER_LINE_HEIGHT <= mainMenuFooterControlsY(entryCount))
    }

    @Test
    fun `main menu footer keeps legacy placement when the menu stack is shorter`() {
        val entryCount = 3

        assertEquals(MAIN_MENU_FOOTER_LANGUAGE_DEFAULT_Y, mainMenuFooterLanguageY(entryCount))
        assertEquals(MAIN_MENU_FOOTER_CONTROLS_DEFAULT_Y, mainMenuFooterControlsY(entryCount))
        assertEquals(MAIN_MENU_FOOTER_NOTICE_DEFAULT_Y, mainMenuFooterNoticeY(entryCount))
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

        assertEquals("Status: Ready to start", selectionStateText(enLocalizer, com.ktome.core.profile.ClassPlayabilityState.PLAYABLE))
        assertEquals("状态：锁定", selectionStateText(zhLocalizer, com.ktome.core.profile.ClassPlayabilityState.LOCKED))
    }

    @Test
    fun `discovered unavailable note uses player facing names instead of availability enums`() {
        val zhLocalizer = LocalizationBundle.load().translator(GameLocale.ZH_CN)
        val enLocalizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        val enNote =
            discoveredUnavailableNoteText(
                localizer = enLocalizer,
                noteKey = "ui.menu.profession_discovered_unavailable",
                optionNameKeys = listOf("profession.berserker.name", "profession.spellblade.name"),
            )
        val zhNote =
            discoveredUnavailableNoteText(
                localizer = zhLocalizer,
                noteKey = "ui.menu.profession_discovered_unavailable",
                optionNameKeys = listOf("profession.berserker.name"),
            )

        assertTrue(enNote!!.contains("Berserker"))
        assertTrue(enNote.contains("Berserker, Spellblade"))
        assertFalse(enNote.contains("UNLOCKED_BUT_UNAVAILABLE"))
        assertTrue(zhNote!!.contains("狂战士"))
        assertTrue(
            discoveredUnavailableNoteText(
                localizer = zhLocalizer,
                noteKey = "ui.menu.profession_discovered_unavailable",
                optionNameKeys = listOf("profession.berserker.name", "profession.spellblade.name"),
            )!!.contains("狂战士、咒剑士")
        )
        assertFalse(zhNote.contains("开发验证"))
    }
}
