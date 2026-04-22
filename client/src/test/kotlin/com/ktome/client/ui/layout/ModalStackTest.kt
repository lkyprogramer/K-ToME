package com.ktome.client.ui.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModalStackTest {
    @Test
    fun `push pop and clear preserve explicit frame kinds`() {
        val stack = ModalStack()

        stack.push(ModalFrame(ModalFrameKind.INVENTORY))
        stack.push(ModalFrame(ModalFrameKind.ITEM_DETAIL))
        stack.push(ModalFrame(ModalFrameKind.ITEM_COMPARE))

        assertEquals(3, stack.depth)
        assertFalse(stack.canPush())
        assertEquals(ModalFrameKind.ITEM_COMPARE, stack.top()?.kind)

        assertEquals(ModalFrameKind.ITEM_COMPARE, stack.pop()?.kind)
        assertEquals(ModalFrameKind.ITEM_DETAIL, stack.top()?.kind)

        stack.clear()
        assertTrue(stack.isEmpty)
    }

    @Test
    fun `depth overflow fails fast instead of silently flattening`() {
        val stack = ModalStack()
        stack.push(ModalFrame(ModalFrameKind.INVENTORY))
        stack.push(ModalFrame(ModalFrameKind.ITEM_DETAIL))
        stack.push(ModalFrame(ModalFrameKind.ITEM_COMPARE))

        assertThrows(IllegalStateException::class.java) {
            stack.push(ModalFrame(ModalFrameKind.COMBAT_DECISION))
        }
    }

    @Test
    fun `deferred combat decision occupies a real stack layer`() {
        val stack = ModalStack()
        stack.push(ModalFrame(ModalFrameKind.TARGETING))
        stack.push(ModalFrame(ModalFrameKind.COMBAT_DECISION))

        assertEquals(2, stack.depth)
        assertEquals(listOf(ModalFrameKind.TARGETING, ModalFrameKind.COMBAT_DECISION), stack.frames().map(ModalFrame::kind))
    }
}
