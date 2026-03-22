package com.ktome.client.screen

import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
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
}
