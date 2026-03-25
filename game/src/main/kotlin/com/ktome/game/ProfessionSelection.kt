package com.ktome.game

import com.ktome.core.profession.ProfessionTier
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.core.profile.ClassUnlockState

data class ProfessionSelectionOption(
    val id: String,
    val tier: ProfessionTier,
    val unlockState: ClassUnlockState,
    val playabilityState: ClassPlayabilityState,
)
