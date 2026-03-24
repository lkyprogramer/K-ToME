package com.ktome.core.ai

import com.ktome.core.ecs.EntityId
import com.ktome.core.map.Point
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusLifecycle
import com.ktome.core.talent.EffectTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StealthTauntHandlerTest {
    @Test
    fun `taunt source overrides fallback target while source stays alive`() {
        val fallbackTargetId = EntityId(1)
        val tauntSourceId = EntityId(2)
        val tracker = EffectTracker(ownerId = EntityId(9))
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.TAUNT,
                effectId = "forced_taunt",
                duration = 3,
                sourceEntityId = tauntSourceId,
            ),
        )

        val resolved =
            StealthTauntHandler.resolveTargetId(
                effectTracker = tracker,
                fallbackTargetId = fallbackTargetId,
                isAlive = { entityId -> entityId == tauntSourceId },
            )

        assertEquals(tauntSourceId, resolved)
    }

    @Test
    fun `stealth target is treated as not visible even inside visible tiles`() {
        val tracker = EffectTracker(ownerId = EntityId(4))
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.STEALTH,
                effectId = "hidden_target",
                duration = 2,
                sourceEntityId = EntityId(4),
            ),
        )

        val visible = StealthTauntHandler.isTargetVisible(tracker, Point(3, 3), setOf(Point(3, 3)))

        assertFalse(visible)
    }

    @Test
    fun `reaching last known position clears it and falls through`() {
        val perception = AIPerceptionState(lastKnownTargetPosition = Point(5, 5))

        val beforeArrival =
            StealthTauntHandler.consumeLastKnownTargetPosition(
                perception = perception,
                currentPosition = Point(4, 5),
                useLastKnownPosition = true,
            )
        val afterArrival =
            StealthTauntHandler.consumeLastKnownTargetPosition(
                perception = perception,
                currentPosition = Point(5, 5),
                useLastKnownPosition = true,
            )

        assertEquals(Point(5, 5), beforeArrival)
        assertNull(afterArrival)
        assertNull(perception.lastKnownTargetPosition)
    }

    @Test
    fun `visible target refreshes last known position`() {
        val perception = AIPerceptionState()

        StealthTauntHandler.rememberLastKnownTargetPosition(
            perception = perception,
            targetVisible = true,
            targetPosition = Point(7, 2),
        )

        assertTrue(perception.lastKnownTargetPosition == Point(7, 2))
    }
}
