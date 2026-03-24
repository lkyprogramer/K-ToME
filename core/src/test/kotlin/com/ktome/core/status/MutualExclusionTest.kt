package com.ktome.core.status

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MutualExclusionTest {
    @Test
    fun `freeze is removed when burn is applied later`() {
        val tracker = StatusTracker()
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.FREEZE, "freeze", duration = 2))

        val result =
            StatusLifecycle.applyEffect(
                tracker,
                StatusLifecycle.createInstance(StatusEffectType.BURN, "burn", duration = 3),
            )

        assertEquals("FREEZE_OVERWRITTEN_BY_BURN", result.interactionId)
        assertEquals(listOf(StatusEffectType.BURN), tracker.activeEffects().map { effect -> effect.type })
    }
}
