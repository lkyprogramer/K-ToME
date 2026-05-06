package com.ktome.client.render

import com.ktome.game.contentpack.ContentPackKeyResolutionSummary
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValidationPackSummaryTextTest {
    @Test
    fun `key warnings preserve visual audio and locale keys separately`() {
        val localizer =
            LocalizationBundle.fromMaps(
                mapOf(
                    GameLocale.EN_US to
                        mapOf(
                            "ui.validation.empty" to "empty",
                            "ui.validation.pack.key_warnings" to "Pack key warnings: visual={visual}, audio={audio}, locale={locale}",
                        ),
                    GameLocale.ZH_CN to emptyMap(),
                ),
            ).translator(GameLocale.EN_US)

        val text =
            ValidationPackSummaryText.keyWarnings(
                localizer,
                ContentPackKeyResolutionSummary(
                    warningVisualKeys = listOf("visual.missing"),
                    warningAudioKeys = listOf("audio.missing"),
                    warningLocaleKeys = listOf("locale.missing"),
                ),
            )

        assertEquals("Pack key warnings: visual=visual.missing, audio=audio.missing, locale=locale.missing", text)
    }
}
