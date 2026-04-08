package com.ktome.tools.contentpack

import com.ktome.client.assets.ManifestLoadException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContentPackHarnessFailureClassificationTest {
    @Test
    fun `manifest load failures are converted into structured content-pack diagnostics`() {
        val failure =
            ContentPackHarnessRunner.classifyFailure(
                ManifestLoadException("Visual manifest is invalid: /tmp/fixture.visual-manifest.json"),
            )

        assertEquals(listOf("content-pack.resource.visual-manifest-invalid"), failure.diagnosticCodes)
        assertEquals(listOf("Visual manifest is invalid: /tmp/fixture.visual-manifest.json"), failure.diagnostics)
    }

    @Test
    fun `wrapped manifest load failures still keep structured asset diagnostics`() {
        val failure =
            ContentPackHarnessRunner.classifyFailure(
                IllegalStateException(
                    "wrapper",
                    ManifestLoadException("Audio manifest is invalid: /tmp/fixture.audio-manifest.json"),
                ),
            )

        assertEquals(listOf("content-pack.resource.audio-manifest-invalid"), failure.diagnosticCodes)
        assertEquals(listOf("Audio manifest is invalid: /tmp/fixture.audio-manifest.json"), failure.diagnostics)
    }

    @Test
    fun `locale parsing failures are converted into structured content-pack diagnostics`() {
        val exception = IllegalStateException("Unexpected JSON token")
        exception.stackTrace =
            arrayOf(
                StackTraceElement(
                    "com.ktome.game.contentpack.ContentPackResources",
                    "parseLocaleBundle",
                    "ContentPackResources.kt",
                    42,
                ),
            )

        val failure = ContentPackHarnessRunner.classifyFailure(exception)

        assertEquals(listOf("content-pack.resource.locale-invalid"), failure.diagnosticCodes)
    }

    @Test
    fun `wrapped locale parsing failures still keep structured locale diagnostics`() {
        val localeFailure = IllegalArgumentException("Missing key")
        localeFailure.stackTrace =
            arrayOf(
                StackTraceElement(
                    "com.ktome.game.i18n.LocalizationBundle",
                    "parseBundle",
                    "Localization.kt",
                    58,
                ),
            )

        val failure = ContentPackHarnessRunner.classifyFailure(RuntimeException("wrapper", localeFailure))

        assertEquals(listOf("content-pack.resource.locale-invalid"), failure.diagnosticCodes)
        assertEquals(listOf("IllegalArgumentException: Missing key"), failure.diagnostics)
    }
}
