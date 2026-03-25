package com.ktome.core.profile

object ClassAvailabilityResolver {
    fun resolve(
        unlockState: ClassUnlockState,
        context: AvailabilityContext,
    ): ClassPlayabilityState =
        when (context) {
            AvailabilityContext.PLAYER_CREATION ->
                when (unlockState) {
                    ClassUnlockState.LOCKED -> ClassPlayabilityState.LOCKED
                    ClassUnlockState.DEV_UNLOCKED -> ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE
                    ClassUnlockState.RELEASE_UNLOCKED -> ClassPlayabilityState.PLAYABLE
                }

            AvailabilityContext.DEV_LAB,
            AvailabilityContext.WHITE_BOX,
            ->
                when (unlockState) {
                    ClassUnlockState.LOCKED -> ClassPlayabilityState.LOCKED
                    ClassUnlockState.DEV_UNLOCKED,
                    ClassUnlockState.RELEASE_UNLOCKED,
                    -> ClassPlayabilityState.PLAYABLE
                }
        }
}
