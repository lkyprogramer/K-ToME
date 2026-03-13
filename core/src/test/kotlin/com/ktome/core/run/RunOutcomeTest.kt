package com.ktome.core.run

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RunOutcomeTest {
    @Test
    fun `in progress outcome is not terminal`() {
        assertFalse(RunOutcome.InProgress.isTerminal)
    }

    @Test
    fun `victory and defeat are terminal`() {
        assertTrue(RunOutcome.Victory(floor = 5).isTerminal)
        assertTrue(RunOutcome.Defeat(floor = 3).isTerminal)
    }
}
