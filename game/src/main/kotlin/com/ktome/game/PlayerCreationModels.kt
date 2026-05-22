package com.ktome.game

import com.ktome.core.profession.ProfessionTier
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.core.profile.ClassUnlockState

sealed interface PlayerCreationOption {
    val id: String
    val displayNameKey: String
    val descriptionKey: String
    val unlockState: ClassUnlockState
    val playabilityState: ClassPlayabilityState
}

data class ProfessionPlayerCreationOption(
    override val id: String,
    override val displayNameKey: String,
    override val descriptionKey: String,
    override val unlockState: ClassUnlockState,
    override val playabilityState: ClassPlayabilityState,
    val tier: ProfessionTier,
    val resourceHintKey: String,
    val iconKey: String = "icon.profession.$id",
) : PlayerCreationOption {
    init {
        require(id.isNotBlank()) { "Profession player creation option id must not be blank." }
        require(displayNameKey.isNotBlank()) { "Profession player creation option '$id' displayNameKey must not be blank." }
        require(descriptionKey.isNotBlank()) { "Profession player creation option '$id' descriptionKey must not be blank." }
        require(resourceHintKey.isNotBlank()) { "Profession player creation option '$id' resourceHintKey must not be blank." }
        require(iconKey.startsWith("icon.profession.")) {
            "Profession player creation option '$id' iconKey must use icon.profession.*."
        }
    }
}

data class RacePlayerCreationOption(
    override val id: String,
    override val displayNameKey: String,
    override val descriptionKey: String,
    override val unlockState: ClassUnlockState,
    override val playabilityState: ClassPlayabilityState,
) : PlayerCreationOption {
    init {
        require(id.isNotBlank()) { "Race player creation option id must not be blank." }
        require(displayNameKey.isNotBlank()) { "Race player creation option '$id' displayNameKey must not be blank." }
        require(descriptionKey.isNotBlank()) { "Race player creation option '$id' descriptionKey must not be blank." }
    }
}

data class PlayerCreationSelection(
    val professionId: String,
    val raceId: String,
)

data class PlayerCreationState(
    val professionOptions: List<ProfessionPlayerCreationOption>,
    val raceOptions: List<RacePlayerCreationOption>,
    val selection: PlayerCreationSelection,
) {
    init {
        require(professionOptions.isNotEmpty()) { "Player creation requires at least one profession option." }
        require(raceOptions.isNotEmpty()) { "Player creation requires at least one race option." }
        require(professionOptions.any { option -> option.id == selection.professionId }) {
            "Unknown selected profession '${selection.professionId}'."
        }
        require(raceOptions.any { option -> option.id == selection.raceId }) {
            "Unknown selected race '${selection.raceId}'."
        }
    }

    fun selectedProfessionOption(): ProfessionPlayerCreationOption =
        requireNotNull(professionOptions.firstOrNull { option -> option.id == selection.professionId }) {
            "Unknown selected profession '${selection.professionId}'."
        }

    fun selectedRaceOption(): RacePlayerCreationOption =
        requireNotNull(raceOptions.firstOrNull { option -> option.id == selection.raceId }) {
            "Unknown selected race '${selection.raceId}'."
        }

    fun canStartNewGame(): Boolean =
        selectedProfessionOption().playabilityState == ClassPlayabilityState.PLAYABLE &&
            selectedRaceOption().playabilityState == ClassPlayabilityState.PLAYABLE
}
