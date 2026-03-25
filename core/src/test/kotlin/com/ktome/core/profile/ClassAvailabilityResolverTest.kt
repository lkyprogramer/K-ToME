package com.ktome.core.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClassAvailabilityResolverTest {
    @Test
    fun `player creation maps dev unlocked classes to unavailable`() {
        assertEquals(
            ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE,
            ClassAvailabilityResolver.resolve(
                unlockState = ClassUnlockState.DEV_UNLOCKED,
                context = AvailabilityContext.PLAYER_CREATION,
            ),
        )
    }

    @Test
    fun `dev lab and white box map dev unlocked classes to playable`() {
        AvailabilityContext.entries
            .filter { context -> context != AvailabilityContext.PLAYER_CREATION }
            .forEach { context ->
                assertEquals(
                    ClassPlayabilityState.PLAYABLE,
                    ClassAvailabilityResolver.resolve(
                        unlockState = ClassUnlockState.DEV_UNLOCKED,
                        context = context,
                    ),
                )
            }
    }

    @Test
    fun `locked classes stay locked in every context`() {
        AvailabilityContext.entries.forEach { context ->
            assertEquals(
                ClassPlayabilityState.LOCKED,
                ClassAvailabilityResolver.resolve(
                    unlockState = ClassUnlockState.LOCKED,
                    context = context,
                ),
            )
        }
    }
}
