package com.ktome.core.status

import com.ktome.core.combat.DamageType
import com.ktome.core.item.StatModifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusLifecycleTest {
    @Test
    fun `refresh statuses keep single instance and longest duration`() {
        val tracker = StatusTracker()

        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.STUN,
                effectId = "stun_a",
                duration = 2,
            ),
        )
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.STUN,
                effectId = "stun_b",
                duration = 4,
            ),
        )

        assertEquals(1, tracker.activeEffects().size)
        assertEquals(4, tracker.activeEffects().single().remainingTurns)
        assertEquals(StatusEffectType.STUN.schemaId, tracker.activeEffects().single().schemaId)
    }

    @Test
    fun `armor break caps at three stacks`() {
        val tracker = StatusTracker()

        repeat(4) { index ->
            StatusLifecycle.applyEffect(
                tracker,
                StatusLifecycle.createInstance(
                    type = StatusEffectType.ARMOR_BREAK,
                    effectId = "armor_break_$index",
                    duration = 3,
                    statModifierOverride = StatModifier(defense = -3),
                ),
            )
        }

        val armorBreak = tracker.activeEffects().single()
        assertEquals(3, armorBreak.stackCount)
        assertEquals(3, armorBreak.stackCap)
        assertEquals(-9, armorBreak.effectiveStatModifier().defense)
    }

    @Test
    fun `cleanse prioritizes hard control and skips locked statuses`() {
        val tracker = StatusTracker()
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.CURSE, "curse", duration = 5))
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.STUN, "stun", duration = 2))
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.STEALTH, "stealth", duration = 3))
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.INVULNERABLE, "invuln", duration = 1))

        val removed = StatusLifecycle.cleanse(tracker, maxEffectsRemoved = 2)

        assertEquals(listOf(StatusEffectType.STUN.schemaId, StatusEffectType.CURSE.schemaId), removed.map { effect -> effect.schemaId })
        assertTrue(tracker.has(StatusEffectType.STEALTH))
        assertTrue(tracker.has(StatusEffectType.INVULNERABLE))
    }

    @Test
    fun `freeze and burn mutually exclude each other`() {
        val tracker = StatusTracker()
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.FREEZE, "freeze", duration = 2))

        val result =
            StatusLifecycle.applyEffect(
                tracker,
                StatusLifecycle.createInstance(StatusEffectType.BURN, "burn", duration = 3),
            )

        assertEquals("FREEZE_OVERWRITTEN_BY_BURN", result.interactionId)
        assertEquals(listOf(StatusEffectType.BURN.schemaId), tracker.activeEffects().map { effect -> effect.schemaId })
    }

    @Test
    fun `overcharge boosts lightning damage and is consumed after successful hit`() {
        val tracker = StatusTracker()
        StatusLifecycle.applyEffect(tracker, StatusLifecycle.createInstance(StatusEffectType.OVERCHARGE, "overcharge", duration = 2))

        assertEquals(1.25, StatusLifecycle.incomingDamageMultiplier(tracker, DamageType.LIGHTNING), 0.0001)
        assertEquals(1.0, StatusLifecycle.incomingDamageMultiplier(tracker, DamageType.FIRE), 0.0001)

        val consumed = StatusLifecycle.consumeOnDamage(tracker, DamageType.LIGHTNING, actualDamage = 12)

        assertEquals(1, consumed.size)
        assertEquals(StatusEffectType.OVERCHARGE.schemaId, consumed.single().schemaId)
        assertTrue(tracker.activeEffects().isEmpty())
    }
}
