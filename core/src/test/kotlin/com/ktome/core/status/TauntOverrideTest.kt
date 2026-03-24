package com.ktome.core.status

import com.ktome.core.ecs.EntityId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TauntOverrideTest {
    @Test
    fun `later taunt overrides previous source and records the replacement`() {
        val tracker = StatusTracker()
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.TAUNT,
                effectId = "taunt_first",
                duration = 2,
                sourceEntityId = EntityId(11),
            ),
        )

        val result =
            StatusLifecycle.applyEffect(
                tracker,
                StatusLifecycle.createInstance(
                    type = StatusEffectType.TAUNT,
                    effectId = "taunt_second",
                    duration = 3,
                    sourceEntityId = EntityId(22),
                ),
            )

        assertEquals("TAUNT_OVERRIDE", result.interactionId)
        assertEquals(EntityId(11), result.removed.single().sourceEntityId)
        assertEquals(EntityId(22), tracker.activeEffects().single().sourceEntityId)
    }
}
