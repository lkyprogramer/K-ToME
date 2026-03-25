package com.ktome.core.profile

enum class ClassUnlockState {
    LOCKED,
    DEV_UNLOCKED,
    RELEASE_UNLOCKED,
}

enum class AvailabilityContext {
    PLAYER_CREATION,
    DEV_LAB,
    WHITE_BOX,
}

enum class ClassPlayabilityState {
    LOCKED,
    UNLOCKED_BUT_UNAVAILABLE,
    PLAYABLE,
}
