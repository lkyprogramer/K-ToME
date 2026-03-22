package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.InputSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MainMenuControllerTest {
    @Test
    fun `left and right cycle professions and start action keeps selected profession`() {
        val input = QueueInputSource()
        val controller =
            MainMenuController(
                input = input,
                availableProfessionIds = listOf("vanguard", "arcanist", "rogue", "templar"),
                initialProfessionId = "vanguard",
            )

        input.push(Keys.RIGHT)
        val changed = controller.pollAction(hasSave = false)
        assertEquals("arcanist", changed.selectedProfessionId)
        assertTrue(changed.professionChanged)
        assertTrue(changed.selectionChanged)
        assertEquals(null, changed.action)

        input.push(Keys.ENTER)
        val started = controller.pollAction(hasSave = false)
        assertEquals(MainMenuAction.StartNewGame, started.action)
        assertEquals("arcanist", started.selectedProfessionId)
    }

    @Test
    fun `profession selection wraps backwards from first entry`() {
        val input = QueueInputSource(Keys.LEFT)
        val controller =
            MainMenuController(
                input = input,
                availableProfessionIds = listOf("vanguard", "arcanist", "rogue", "templar"),
                initialProfessionId = "vanguard",
            )

        val changed = controller.pollAction(hasSave = true)
        assertEquals("templar", changed.selectedProfessionId)
        assertTrue(changed.professionChanged)
        assertFalse(changed.rejected)
    }
}

private class QueueInputSource(
    vararg keys: Int,
) : InputSource {
    private val queue = ArrayDeque<Int>().apply { keys.forEach(::addLast) }

    fun push(
        vararg keys: Int,
    ) {
        keys.forEach(queue::addLast)
    }

    override fun isKeyJustPressed(keycode: Int): Boolean =
        if (queue.firstOrNull() == keycode) {
            queue.removeFirst()
            true
        } else {
            false
        }

    override fun isKeyPressed(keycode: Int): Boolean = false
}
