package com.ktome.client.render

import com.ktome.game.contentpack.ContentPackKeyResolutionSummary
import com.ktome.game.contentpack.ContentPackOverlaySummary
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
                            "ui.validation.fold.more" to "+{count} more",
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

    @Test
    fun overlayOpsAreSortedAndBounded() {
        val localizer =
            LocalizationBundle.fromMaps(
                mapOf(
                    GameLocale.EN_US to
                        mapOf(
                            "ui.validation.empty" to "empty",
                            "ui.validation.fold.more" to "+{count} more",
                        ),
                    GameLocale.ZH_CN to emptyMap(),
                ),
            ).translator(GameLocale.EN_US)
        val summaries =
            (10 downTo 1).map { index ->
                ContentPackOverlaySummary(
                    packId = "pack-$index",
                    namespace = "ns-$index",
                    opCounts = mapOf("replace" to 1, "append" to 2),
                )
            }

        val text = ValidationPackSummaryText.overlayOps(localizer, summaries)

        assertEquals(
            "pack-10:append=2/replace=1, pack-1:append=2/replace=1, pack-2:append=2/replace=1, " +
                "pack-3:append=2/replace=1, pack-4:append=2/replace=1, pack-5:append=2/replace=1, " +
                "pack-6:append=2/replace=1, pack-7:append=2/replace=1, +2 more",
            text,
        )
    }
}
