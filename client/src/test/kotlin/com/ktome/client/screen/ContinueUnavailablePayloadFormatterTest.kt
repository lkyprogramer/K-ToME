package com.ktome.client.screen

import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContinueUnavailablePayloadFormatterTest {
    @Test
    fun `payload keeps fixed context order fixed line endings and omits stack traces`() {
        val enLocalizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val payload =
            ContinueUnavailablePayloadFormatter.format(
                localizer = enLocalizer,
                unavailable =
                    ContinueAvailability.Unavailable(
                        reasonCode = ContinueUnavailableReasonCode.UNKNOWN,
                        savePath = "/tmp/run-save.json",
                        throwableClass = "java.io.EOFException",
                        throwableMessage = "x".repeat(230),
                    ),
                buildHash = "abcdef1",
                gameVersion = "0.4.0-test",
            ).renderPlainText()

        val lines = payload.split("\n")
        assertEquals("Continue Game Unavailable", lines[0])
        assertEquals("Unclassified save issue. Copy error detail and report it.", lines[1])
        assertEquals("savePath: /tmp/run-save.json", lines[2])
        assertEquals("reasonCode: UNKNOWN", lines[3])
        assertEquals("gameVersion: 0.4.0-test", lines[4])
        assertEquals("throwableClass: java.io.EOFException", lines[5])
        assertTrue(lines[6].startsWith("throwableMessage: "))
        assertTrue(lines[6].endsWith("..."))
        assertEquals("[ktome/abcdef1]", lines[7])
        assertFalse(payload.contains("\r"))
        assertFalse(payload.contains("at com."))
    }
}
