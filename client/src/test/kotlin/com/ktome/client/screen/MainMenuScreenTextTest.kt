package com.ktome.client.screen

import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `main menu help lines stay between summary and footer`() {
        val footerTopY = mainMenuFooterLanguageY(entryCount = 4) + MAIN_MENU_FOOTER_LINE_HEIGHT

        assertTrue(mainMenuHelpLineY(0) + MAIN_MENU_FOOTER_LINE_HEIGHT <= MAIN_MENU_PLAYER_CREATION_SECTION_BOTTOM_Y)
        assertTrue(mainMenuHelpLineY(1) + MAIN_MENU_FOOTER_LINE_HEIGHT <= mainMenuHelpLineY(0))
        assertTrue(mainMenuHelpLineY(1) - MAIN_MENU_FOOTER_LINE_HEIGHT >= footerTopY)
        assertTrue(MAIN_MENU_BUILD_SUMMARY_X + MAIN_MENU_HELP_MAX_WIDTH <= menuWidth)
    }

    @Test
    fun `main menu help copy remains compact for the right column`() {
        val enLocalizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val zhLocalizer = LocalizationBundle.load().translator(GameLocale.ZH_CN)

        val helpKeys = listOf("ui.menu.help.primary-keys", "ui.menu.help.safe-start")
        assertTrue(helpKeys.all { key -> enLocalizer.text(key).length <= 82 })
        assertTrue(helpKeys.all { key -> zhLocalizer.text(key).length <= 48 })
        assertTrue(enLocalizer.text("ui.menu.help.primary-keys").contains("Backspace back"))
        assertTrue(zhLocalizer.text("ui.menu.help.primary-keys").contains("Backspace 后退"))
    }

    @Test
    fun `build summary leaves enough width at the minimum menu breakpoint`() {
        val minimumMenuWidth = 960f

        assertTrue(minimumMenuWidth - MAIN_MENU_BUILD_SUMMARY_X >= 380f)
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
    fun `build capability lines resolve through menu summary keys`() {
        val enLocalizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        val text =
            buildCapabilityText(
                enLocalizer,
                BuildCapabilityLine("ui.menu.build.class-roster.label", "ui.menu.build.class-roster.value"),
            )

        assertEquals("Classes: 4 foundation classes playable", text)
    }

    @Test
    fun `main menu controls describe locale toggle instead of loadout`() {
        val enLocalizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val zhLocalizer = LocalizationBundle.load().translator(GameLocale.ZH_CN)

        assertTrue(enLocalizer.text("ui.menu.controls").contains("L switch language"))
        assertFalse(enLocalizer.text("ui.menu.controls").contains("loadout"))
        assertTrue(zhLocalizer.text("ui.menu.controls").contains("L 切换语言"))
        assertFalse(zhLocalizer.text("ui.menu.controls").contains("装备"))
    }

    @Test
    fun `footer notice prefers reason-specific unavailable copy`() {
        val snapshot =
            textSnapshot(
                continueDisabledReason = "Save file could not be read.",
                continueDisabledDetail = "The save cannot be loaded. Copy error detail if you need to report it.",
            )

        assertEquals("Save file could not be read.", snapshot.footerNotice)
    }

    @Test
    fun `footer notice keeps explicit menu notice above unavailable copy`() {
        val snapshot =
            textSnapshot(
                continueDisabledReason = "Save file could not be read.",
                continueDisabledDetail = "The save cannot be loaded. Copy error detail if you need to report it.",
                notice = "Continue error detail copied.",
            )

        assertEquals("Continue error detail copied.", snapshot.footerNotice)
        assertNull(textSnapshot().footerNotice)
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

    private fun textSnapshot(
        continueDisabledReason: String? = null,
        continueDisabledDetail: String? = null,
        notice: String? = null,
    ): MainMenuTextSnapshot =
        MainMenuTextSnapshot(
            title = "",
            subtitle = "",
            profession = "",
            professionState = "",
            professionDescription = "",
            professionResourceHint = "",
            race = "",
            raceState = "",
            raceDescription = "",
            entries = emptyList(),
            buildSummary = emptyList(),
            helpLines = emptyList(),
            continueDisabledReason = continueDisabledReason,
            continueDisabledDetail = continueDisabledDetail,
            language = "",
            controls = "",
            notice = notice,
        )
}
