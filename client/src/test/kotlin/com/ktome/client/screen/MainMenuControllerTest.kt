package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.InputSource
import com.ktome.core.profession.ProfessionTier
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.core.profile.ClassUnlockState
import com.ktome.game.ProfessionSelectionOption
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
                professionSelections = playableSelections(),
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
                professionSelections = playableSelections(),
            )

        val changed = controller.pollAction(hasSave = true)
        assertEquals("templar", changed.selectedProfessionId)
        assertTrue(changed.professionChanged)
        assertFalse(changed.rejected)
    }

    @Test
    fun `start new game is rejected for locked or unavailable professions`() {
        val lockedInput = QueueInputSource(Keys.ENTER)
        val lockedController =
            MainMenuController(
                input = lockedInput,
                availableProfessionIds = listOf("vanguard", "arcanist", "rogue"),
                initialProfessionId = "arcanist",
                professionSelections = mixedSelections(),
            )

        val lockedResult = lockedController.pollAction(hasSave = false)
        assertEquals(null, lockedResult.action)
        assertEquals("arcanist", lockedResult.selectedProfessionId)
        assertTrue(lockedResult.rejected)

        val unavailableInput = QueueInputSource(Keys.ENTER)
        val unavailableController =
            MainMenuController(
                input = unavailableInput,
                availableProfessionIds = listOf("vanguard", "arcanist", "rogue"),
                initialProfessionId = "rogue",
                professionSelections = mixedSelections(),
            )

        val unavailableResult = unavailableController.pollAction(hasSave = false)
        assertEquals(null, unavailableResult.action)
        assertEquals("rogue", unavailableResult.selectedProfessionId)
        assertTrue(unavailableResult.rejected)
    }

    private fun playableSelections(): List<ProfessionSelectionOption> =
        listOf(
            selection("vanguard", ClassPlayabilityState.PLAYABLE),
            selection("arcanist", ClassPlayabilityState.PLAYABLE),
            selection("rogue", ClassPlayabilityState.PLAYABLE),
            selection("templar", ClassPlayabilityState.PLAYABLE),
        )

    private fun mixedSelections(): List<ProfessionSelectionOption> =
        listOf(
            selection("vanguard", ClassPlayabilityState.PLAYABLE),
            selection("arcanist", ClassPlayabilityState.LOCKED),
            selection("rogue", ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE),
        )

    private fun selection(
        id: String,
        playabilityState: ClassPlayabilityState,
    ): ProfessionSelectionOption =
        ProfessionSelectionOption(
            id = id,
            tier = ProfessionTier.BASE,
            unlockState = ClassUnlockState.RELEASE_UNLOCKED,
            playabilityState = playabilityState,
        )
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
