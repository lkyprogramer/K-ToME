package com.ktome.client.ui.state

import com.ktome.client.ui.card.ModalCardAction
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiErrorPayloadTest {
    @Test
    fun `payload preserves inserted context order and build hash suffix`() {
        val payload =
            UiErrorPayload(
                heading = "Manifest error",
                detail = "Visual manifest is invalid.",
                contextKeyValuePairs =
                    listOf(
                        "savePath" to "/tmp/save.json",
                        "reasonCode" to "MANIFEST_MISMATCH",
                        "gameVersion" to "0.4.0-test",
                    ),
                buildHash = "abcdef1",
            ).renderPlainText()

        assertEquals(
            listOf(
                "Manifest error",
                "Visual manifest is invalid.",
                "savePath: /tmp/save.json",
                "reasonCode: MANIFEST_MISMATCH",
                "gameVersion: 0.4.0-test",
                "[ktome/abcdef1]",
            ),
            payload.split("\n"),
        )
        assertFalse(payload.contains("\r"))
    }

    @Test
    fun `recoverable error state exposes required actions`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val state =
            UiErrorState.recoverable(
                localizer = localizer,
                heading = RenderTextTokenSnapshot("ui.error.action.retry"),
                detail = RenderTextTokenSnapshot("ui.error.action.copy-detail"),
                contextKeyValuePairs = listOf("reasonCode" to "snapshot.invalid"),
                buildHash = "abcdef1",
            )

        assertTrue(state.actions.contains(ModalCardAction.RETRY))
        assertTrue(state.actions.contains(ModalCardAction.BACK_TO_MENU))
        assertTrue(state.actions.contains(ModalCardAction.COPY_ERROR_DETAIL))
        assertTrue(state.payload.renderPlainText().endsWith("[ktome/abcdef1]"))
    }
}
