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
        val changed = controller.pollAction(ContinueAvailability.Absent)
        assertEquals("arcanist", changed.selection.professionId)
        assertTrue(changed.professionChanged)
        assertTrue(changed.selectionChanged)
        assertEquals(PlayerCreationFocus.PROFESSION, changed.focusedAxis)
        assertEquals(null, changed.action)

        input.push(Keys.ENTER)
        val started = controller.pollAction(ContinueAvailability.Absent)
        assertEquals(MainMenuAction.QuickStart, started.action)
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

        val changed = controller.pollAction(ContinueAvailability.Available)
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
        val changed = controller.pollAction(ContinueAvailability.Absent)
        assertEquals("elf", changed.selection.raceId)
        assertTrue(changed.raceChanged)
        assertTrue(changed.selectionChanged)
        assertEquals(PlayerCreationFocus.RACE, changed.focusedAxis)
        assertEquals(null, changed.action)

        input.push(Keys.ENTER)
        val started = controller.pollAction(ContinueAvailability.Absent)
        assertEquals(MainMenuAction.QuickStart, started.action)
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
        val professionResult = professionController.pollAction(ContinueAvailability.Absent)
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
        val raceResult = raceController.pollAction(ContinueAvailability.Absent)
        assertEquals("dwarf", raceResult.selection.raceId)
        assertTrue(raceResult.raceChanged)
        assertFalse(raceResult.rejected)
    }

    @Test
    fun `main menu exposes validation mode as the fourth entry`() {
        val input = QueueInputSource()
        val controller =
            MainMenuController(
                input = input,
                playerCreationState = playableState(),
            )

        assertEquals(4, controller.entries(ContinueAvailability.Available).size)
        input.push(Keys.DOWN)
        controller.pollAction(ContinueAvailability.Available)
        input.push(Keys.DOWN)
        controller.pollAction(ContinueAvailability.Available)
        input.push(Keys.ENTER)
        val result = controller.pollAction(ContinueAvailability.Available)

        assertEquals(MainMenuAction.ValidationMode, result.action)
    }

    @Test
    fun `available save makes continue the initial focus`() {
        val input = QueueInputSource(Keys.ENTER)
        val controller =
            MainMenuController(
                input = input,
                playerCreationState = playableState(),
                initialContinueAvailability = ContinueAvailability.Available,
            )

        val result = controller.pollAction(ContinueAvailability.Available)

        assertEquals(MainMenuAction.Continue, result.action)
    }

    @Test
    fun `unavailable save keeps quick start as initial focus and disables continue`() {
        val unavailable =
            ContinueAvailability.Unavailable(
                reasonCode = ContinueUnavailableReasonCode.SCHEMA_MISMATCH,
                savePath = "/tmp/run-save.json",
            )
        val input = QueueInputSource()
        val controller =
            MainMenuController(
                input = input,
                playerCreationState = playableState(),
                initialContinueAvailability = unavailable,
            )

        val entries = controller.entries(unavailable)

        assertEquals(0, controller.selectedIndex())
        assertFalse(entries.single { entry -> entry.action == MainMenuAction.Continue }.enabled)
        assertTrue(entries.single { entry -> entry.action == MainMenuAction.Continue }.focusable)
    }

    @Test
    fun `unavailable save exposes copy error detail shortcut`() {
        val unavailable =
            ContinueAvailability.Unavailable(
                reasonCode = ContinueUnavailableReasonCode.CORRUPTED,
                savePath = "/tmp/run-save.json",
            )
        val input = QueueInputSource(Keys.C)
        val controller =
            MainMenuController(
                input = input,
                playerCreationState = playableState(),
                initialContinueAvailability = unavailable,
            )

        val result = controller.pollAction(unavailable)

        assertEquals(MainMenuAction.CopyContinueErrorDetail, result.action)
        assertFalse(result.rejected)
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
