package com.ktome.core.status

import com.ktome.core.combat.DamageType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OverchargeConsumptionTest {
    @Test
    fun `overcharge only boosts and consumes on successful lightning damage`() {
        val tracker = StatusTracker()
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.OVERCHARGE, "overcharge", duration = 2))

        assertEquals(1.25, StatusLifecycle.incomingDamageMultiplier(tracker, DamageType.LIGHTNING), 0.0001)
        assertEquals(1.0, StatusLifecycle.incomingDamageMultiplier(tracker, DamageType.FIRE), 0.0001)
        assertTrue(StatusLifecycle.consumeOnDamage(tracker, DamageType.FIRE, actualDamage = 12).isEmpty())
        assertTrue(tracker.has(StatusEffectType.OVERCHARGE))
        assertTrue(StatusLifecycle.consumeOnDamage(tracker, DamageType.LIGHTNING, actualDamage = 0).isEmpty())
        assertTrue(tracker.has(StatusEffectType.OVERCHARGE))

        val removed = StatusLifecycle.consumeOnDamage(tracker, DamageType.LIGHTNING, actualDamage = 9)

        assertEquals(listOf(StatusEffectType.OVERCHARGE), removed.map { effect -> effect.type })
        assertTrue(tracker.activeEffects().isEmpty())
    }
}
