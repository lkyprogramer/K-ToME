package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.InputSource
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.game.PlayerCreationSelection
import com.ktome.game.PlayerCreationState
import com.ktome.game.ProfessionPlayerCreationOption
import com.ktome.game.RacePlayerCreationOption
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.validationSessionOptionsForPreset
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidationSetupControllerTest {
    @Test
    fun `sample pack toggle swaps formal content pack selection`() {
        val input = ValidationQueueInputSource()
        val controller = ValidationSetupController(input = input, context = setupContext())

        repeat(10) {
            input.push(Keys.DOWN)
            controller.pollAction()
        }
        input.push(Keys.RIGHT)
        controller.pollAction()

        val options = controller.currentOptions()

        assertTrue(options.contentPackSelection.activePackRoots.isNotEmpty())
        assertEquals(ValidationPreset.MAPGEN_DIFF, options.preset)
    }

    @Test
    fun `content pack preset enables sample pack by default`() {
        val input = ValidationQueueInputSource()
        val controller = ValidationSetupController(input = input, context = setupContext())

        repeat(6) {
            input.push(Keys.RIGHT)
            controller.pollAction()
        }

        val options = controller.currentOptions()

        assertEquals(ValidationPreset.CONTENT_PACK, options.preset)
        assertEquals(setupContext().samplePackSelection, options.contentPackSelection)
        assertTrue(options.contentPackSelection.activePackRoots.isNotEmpty())
    }

    @Test
    fun `start action returns typed validation session options`() {
        val input = ValidationQueueInputSource()
        val controller = ValidationSetupController(input = input, context = setupContext())

        repeat(11) {
            input.push(Keys.DOWN)
            controller.pollAction()
        }
        input.push(Keys.ENTER)

        val result = controller.pollAction()

        val action = result.action as ValidationSetupAction.StartSession
        assertEquals(ValidationPreset.MAPGEN_DIFF, action.options.preset)
        assertEquals("greenwood_fringe", action.options.foundationConfig.zoneId)
    }

    private fun setupContext(): ValidationSetupContext =
        ValidationSetupContext(
            initialOptions = validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF),
            playerCreationState = playableState(),
            zones =
                listOf(
                    ValidationZoneOption("shattered_outpost", "zone.shattered_outpost.name", 2),
                    ValidationZoneOption("greenwood_fringe", "zone.greenwood_fringe.name", 3),
                ),
            bossVariantIds = listOf("boss.variant.grey_crown"),
            samplePackSelection = ContentPackSelection.of(Path.of("/tmp/sample.flooded_relics")),
            continueEnabled = true,
        )

    private fun playableState(): PlayerCreationState =
        PlayerCreationState(
            professionOptions =
                listOf(
                    professionOption("vanguard", ClassPlayabilityState.PLAYABLE),
                    professionOption("rogue", ClassPlayabilityState.PLAYABLE),
                ),
            raceOptions =
                listOf(
                    raceOption("human", ClassPlayabilityState.PLAYABLE),
                    raceOption("elf", ClassPlayabilityState.PLAYABLE),
                ),
            selection = PlayerCreationSelection(professionId = "vanguard", raceId = "human"),
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

private class ValidationQueueInputSource(
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
