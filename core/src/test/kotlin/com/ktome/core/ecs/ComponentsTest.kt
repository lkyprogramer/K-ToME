package com.ktome.core.ecs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComponentsTest {
    @Test
    fun `ai behavior preferred range and trigger tracker state are typed runtime data`() {
        val behavior = AIBehavior(type = AIType.CHASE, preferredRangeStart = 2, preferredRangeEnd = 4)
        val tracker = AiTriggerTracker()

        tracker.pendingCombatStartTriggerIds += "opening_guard"
        tracker.consumedTriggerIds += "enrage_40"
        tracker.engagedInCombat = true

        assertEquals(2..4, behavior.preferredRange)
        assertEquals(setOf("opening_guard"), tracker.pendingCombatStartTriggerIds)
        assertEquals(setOf("enrage_40"), tracker.consumedTriggerIds)
        assertTrue(tracker.engagedInCombat)
    }
}
