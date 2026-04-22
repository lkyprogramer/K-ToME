package com.ktome.client.ui.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaneFocusControllerTest {
    @Test
    fun `tab cycles map anchors in both directions`() {
        val controller = PaneFocusController()

        assertEquals(PaneFocusAnchor.CONTEXT, controller.move(1))
        assertEquals(PaneFocusAnchor.CHARACTER_ACTION, controller.move(1))
        assertEquals(PaneFocusAnchor.WORLD, controller.move(1))
        assertEquals(PaneFocusAnchor.CHARACTER_ACTION, controller.move(-1))
    }

    @Test
    fun `modal close restores the anchor that opened it`() {
        val controller = PaneFocusController()
        controller.move(1)

        controller.onModalOpened()
        controller.move(1)
        controller.onModalClosed()

        assertEquals(PaneFocusAnchor.CONTEXT, controller.currentAnchor)
    }

    @Test
    fun `passive takeover resets map anchor and clears suspended restore`() {
        val controller = PaneFocusController()
        controller.move(1)
        controller.onModalOpened()
        controller.onPassiveTakeover()
        controller.onModalClosed()

        assertEquals(PaneFocusAnchor.WORLD, controller.currentAnchor)
    }
}
