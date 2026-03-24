package com.ktome.core.status

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StealthBreakTest {
    @Test
    fun `stealth only breaks on actual damage`() {
        val tracker = StatusTracker()
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.STEALTH, "stealth", duration = 3))

        assertTrue(StatusLifecycle.breakOnDamage(tracker, actualDamage = 0).isEmpty())
        assertTrue(tracker.has(StatusEffectType.STEALTH))

        val removed = StatusLifecycle.breakOnDamage(tracker, actualDamage = 7)

        assertEquals(listOf(StatusEffectType.STEALTH), removed.map { effect -> effect.type })
        assertTrue(tracker.activeEffects().isEmpty())
    }
}
