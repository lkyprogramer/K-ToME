package com.ktome.core.status

import com.ktome.core.item.StatModifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArmorBreakCapTest {
    @Test
    fun `armor break uses a cross source global cap of three stacks`() {
        val tracker = StatusTracker()

        repeat(4) { index ->
            StatusLifecycle.applyEffect(
                tracker,
                StatusLifecycle.createInstance(
                    type = StatusEffectType.ARMOR_BREAK,
                    effectId = "armor_break_$index",
                    duration = 3,
                    sourceEntityId = com.ktome.core.ecs.EntityId(index + 1),
                    statModifierOverride = StatModifier(defense = -3),
                ),
            )
        }

        val armorBreak = tracker.activeEffects().single()
        assertEquals(3, armorBreak.stackCount)
        assertEquals(3, armorBreak.stackCap)
        assertEquals(-9, armorBreak.effectiveStatModifier().defense)
    }
}
