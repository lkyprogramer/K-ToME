package com.ktome.client.screen

import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.core.profile.ClassUnlockState
import com.ktome.core.profession.ProfessionTier
import com.ktome.game.PlayerCreationSelection
import com.ktome.game.PlayerCreationState
import com.ktome.game.ProfessionPlayerCreationOption
import com.ktome.game.RacePlayerCreationOption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainMenuFocusPolicyTest {
    @Test
    fun `quick start owns initial focus when no save exists`() {
        val entries = entries(ContinueAvailability.Absent)

        assertEquals(0, MainMenuFocusPolicy.initialIndex(entries, ContinueAvailability.Absent))
    }

    @Test
    fun `continue owns initial focus when save is available`() {
        val entries = entries(ContinueAvailability.Available)

        assertEquals(1, MainMenuFocusPolicy.initialIndex(entries, ContinueAvailability.Available))
    }

    @Test
    fun `unavailable save falls back to quick start instead of disabled continue`() {
        val unavailable =
            ContinueAvailability.Unavailable(
                reasonCode = ContinueUnavailableReasonCode.CORRUPTED,
                savePath = "/tmp/run-save.json",
            )
        val entries = entries(unavailable)

        assertEquals(0, MainMenuFocusPolicy.initialIndex(entries, unavailable))
    }

    @Test
    fun `validation mode receives focus when quick start is disabled`() {
        val entries = MainMenuController(playerCreationState = lockedState()).entries(ContinueAvailability.Absent)

        assertEquals(2, MainMenuFocusPolicy.initialIndex(entries, ContinueAvailability.Absent))
    }

    @Test
    fun `primary action follows validation fallback when quick start is disabled`() {
        val entries = MainMenuController(playerCreationState = lockedState()).entries(ContinueAvailability.Absent)

        assertEquals(
            MainMenuPrimaryAction.VALIDATION_MODE,
            MainMenuFocusPolicy.primaryAction(entries, ContinueAvailability.Absent),
        )
    }

    private fun entries(continueAvailability: ContinueAvailability): List<MainMenuController.MenuEntry> =
        MainMenuController(playerCreationState = playableState()).entries(continueAvailability)

    private fun playableState(): PlayerCreationState =
        PlayerCreationState(
            professionOptions =
                listOf(
                    ProfessionPlayerCreationOption(
                        id = "vanguard",
                        displayNameKey = "profession.vanguard.name",
                        descriptionKey = "profession.vanguard.desc",
                        unlockState = ClassUnlockState.RELEASE_UNLOCKED,
                        playabilityState = ClassPlayabilityState.PLAYABLE,
                        tier = ProfessionTier.BASE,
                        resourceHintKey = "profession.vanguard.resource_hint",
                    ),
                ),
            raceOptions =
                listOf(
                    RacePlayerCreationOption(
                        id = "human",
                        displayNameKey = "race.human.name",
                        descriptionKey = "race.human.desc",
                        unlockState = ClassUnlockState.RELEASE_UNLOCKED,
                        playabilityState = ClassPlayabilityState.PLAYABLE,
                    ),
                ),
            selection = PlayerCreationSelection(professionId = "vanguard", raceId = "human"),
        )

    private fun lockedState(): PlayerCreationState =
        PlayerCreationState(
            professionOptions =
                listOf(
                    ProfessionPlayerCreationOption(
                        id = "vanguard",
                        displayNameKey = "profession.vanguard.name",
                        descriptionKey = "profession.vanguard.desc",
                        unlockState = ClassUnlockState.LOCKED,
                        playabilityState = ClassPlayabilityState.LOCKED,
                        tier = ProfessionTier.BASE,
                        resourceHintKey = "profession.vanguard.resource_hint",
                    ),
                ),
            raceOptions =
                listOf(
                    RacePlayerCreationOption(
                        id = "human",
                        displayNameKey = "race.human.name",
                        descriptionKey = "race.human.desc",
                        unlockState = ClassUnlockState.LOCKED,
                        playabilityState = ClassPlayabilityState.LOCKED,
                    ),
                ),
            selection = PlayerCreationSelection(professionId = "vanguard", raceId = "human"),
        )
}
