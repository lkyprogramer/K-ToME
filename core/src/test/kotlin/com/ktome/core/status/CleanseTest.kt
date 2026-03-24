package com.ktome.core.status

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CleanseTest {
    @Test
    fun `cleanse prioritizes hard control then longest remaining and skips locked effects`() {
        val tracker = StatusTracker()
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.CURSE, "curse", duration = 5))
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.STUN, "stun", duration = 2))
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.ROOT, "root", duration = 4))
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.STEALTH, "stealth", duration = 3))

        val removed = StatusLifecycle.cleanse(tracker, maxEffectsRemoved = 2)

        assertEquals(listOf(StatusEffectType.ROOT, StatusEffectType.STUN), removed.map { effect -> effect.type })
        assertTrue(tracker.has(StatusEffectType.CURSE))
        assertTrue(tracker.has(StatusEffectType.STEALTH))
    }
}
