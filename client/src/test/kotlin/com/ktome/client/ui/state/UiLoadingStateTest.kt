package com.ktome.client.ui.state

import com.ktome.client.ui.card.ModalCardAction
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiLoadingStateTest {
    @Test
    fun `generic loading state does not allow cancel`() {
        val loading = UiLoadingState.generic()

        assertEquals("ui.loading.generic", loading.message.key)
        assertTrue(loading.showsSpinner)
        assertFalse(loading.allowsCancel)
        assertEquals(null, loading.cancelAction)
    }

    @Test
    fun `cancel availability must match cancel action`() {
        val cancellable = UiLoadingState.cancellable(RenderTextTokenSnapshot("ui.loading.generic"))

        assertTrue(cancellable.allowsCancel)
        assertEquals(ModalCardAction.CANCEL, cancellable.cancelAction)
        assertThrows(IllegalArgumentException::class.java) {
            UiLoadingState(
                message = RenderTextTokenSnapshot("ui.loading.generic"),
                showsSpinner = true,
                allowsCancel = true,
                cancelAction = null,
            )
        }
    }
}
