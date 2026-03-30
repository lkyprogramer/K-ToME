package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.InputSource
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.game.PlayerCreationSelection
import com.ktome.game.PlayerCreationState
import com.ktome.game.ProfessionPlayerCreationOption
import com.ktome.game.RacePlayerCreationOption
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
                playerCreationState = playableState(),
            )

        input.push(Keys.RIGHT)
        val changed = controller.pollAction(hasSave = false)
        assertEquals("arcanist", changed.selection.professionId)
        assertTrue(changed.professionChanged)
        assertTrue(changed.selectionChanged)
        assertEquals(PlayerCreationFocus.PROFESSION, changed.focusedAxis)
        assertEquals(null, changed.action)

        input.push(Keys.ENTER)
        val started = controller.pollAction(hasSave = false)
        assertEquals(MainMenuAction.StartNewGame, started.action)
        assertEquals("arcanist", started.selection.professionId)
        assertEquals("human", started.selection.raceId)
    }

    @Test
    fun `profession selection wraps backwards from first entry`() {
        val input = QueueInputSource(Keys.LEFT)
        val controller =
            MainMenuController(
                input = input,
                playerCreationState = playableState(),
            )

        val changed = controller.pollAction(hasSave = true)
        assertEquals("templar", changed.selection.professionId)
        assertTrue(changed.professionChanged)
        assertFalse(changed.rejected)
    }

    @Test
    fun `q and e cycle races and start action keeps selected race`() {
        val input = QueueInputSource()
        val controller =
            MainMenuController(
                input = input,
                playerCreationState =
                    playableState().copy(
                        professionOptions = playableState().professionOptions.take(2),
                    ),
            )

        input.push(Keys.E)
        val changed = controller.pollAction(hasSave = false)
        assertEquals("elf", changed.selection.raceId)
        assertTrue(changed.raceChanged)
        assertTrue(changed.selectionChanged)
        assertEquals(PlayerCreationFocus.RACE, changed.focusedAxis)
        assertEquals(null, changed.action)

        input.push(Keys.ENTER)
        val started = controller.pollAction(hasSave = false)
        assertEquals(MainMenuAction.StartNewGame, started.action)
        assertEquals("elf", started.selection.raceId)
    }

    @Test
    fun `default player creation carousel skips locked and unavailable options`() {
        val professionInput = QueueInputSource(Keys.RIGHT)
        val professionController =
            MainMenuController(
                input = professionInput,
                playerCreationState =
                    PlayerCreationState(
                        professionOptions = carouselProfessionOptions(),
                        raceOptions = playableState().raceOptions.take(2),
                        selection = PlayerCreationSelection(professionId = "rogue", raceId = "human"),
                    ),
            )

        assertEquals("vanguard", professionController.currentSelection().professionId)
        val professionResult = professionController.pollAction(hasSave = false)
        assertEquals("templar", professionResult.selection.professionId)
        assertTrue(professionResult.professionChanged)
        assertFalse(professionResult.rejected)

        val raceInput = QueueInputSource(Keys.E)
        val raceController =
            MainMenuController(
                input = raceInput,
                playerCreationState =
                    PlayerCreationState(
                        professionOptions = playableState().professionOptions.take(2),
                        raceOptions = carouselRaceOptions(),
                        selection = PlayerCreationSelection(professionId = "vanguard", raceId = "seer"),
                    ),
            )

        assertEquals("human", raceController.currentSelection().raceId)
        val raceResult = raceController.pollAction(hasSave = false)
        assertEquals("dwarf", raceResult.selection.raceId)
        assertTrue(raceResult.raceChanged)
        assertFalse(raceResult.rejected)
    }

    private fun playableState(): PlayerCreationState =
        PlayerCreationState(
            professionOptions =
                listOf(
                    professionOption("vanguard", ClassPlayabilityState.PLAYABLE),
                    professionOption("arcanist", ClassPlayabilityState.PLAYABLE),
                    professionOption("rogue", ClassPlayabilityState.PLAYABLE),
                    professionOption("templar", ClassPlayabilityState.PLAYABLE),
                ),
            raceOptions =
                listOf(
                    raceOption("human", ClassPlayabilityState.PLAYABLE),
                    raceOption("elf", ClassPlayabilityState.PLAYABLE),
                    raceOption("dwarf", ClassPlayabilityState.PLAYABLE),
                ),
            selection = PlayerCreationSelection(professionId = "vanguard", raceId = "human"),
        )

    private fun carouselProfessionOptions(): List<ProfessionPlayerCreationOption> =
        listOf(
            professionOption("vanguard", ClassPlayabilityState.PLAYABLE),
            professionOption("arcanist", ClassPlayabilityState.LOCKED),
            professionOption("rogue", ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE),
            professionOption("templar", ClassPlayabilityState.PLAYABLE),
        )

    private fun carouselRaceOptions(): List<RacePlayerCreationOption> =
        listOf(
            raceOption("human", ClassPlayabilityState.PLAYABLE),
            raceOption("orc", ClassPlayabilityState.LOCKED),
            raceOption("seer", ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE),
            raceOption("dwarf", ClassPlayabilityState.PLAYABLE),
        )

    private fun professionOption(
        id: String,
        playabilityState: ClassPlayabilityState,
    ): ProfessionPlayerCreationOption =
        ProfessionPlayerCreationOption(
            id = id,
            displayNameKey = "profession.$id.name",
            descriptionKey = "profession.$id.desc",
            unlockState = com.ktome.core.profile.ClassUnlockState.RELEASE_UNLOCKED,
            playabilityState = playabilityState,
            tier = com.ktome.core.profession.ProfessionTier.BASE,
            resourceHintKey = "profession.$id.resource_hint",
        )

    private fun raceOption(
        id: String,
        playabilityState: ClassPlayabilityState,
    ): RacePlayerCreationOption =
        RacePlayerCreationOption(
            id = id,
            displayNameKey = "race.$id.name",
            descriptionKey = "race.$id.desc",
            unlockState = com.ktome.core.profile.ClassUnlockState.RELEASE_UNLOCKED,
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
