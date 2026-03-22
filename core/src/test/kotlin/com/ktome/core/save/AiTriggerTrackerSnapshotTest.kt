package com.ktome.core.save

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AiTriggerTrackerSnapshotTest {
    @Test
    fun `ai trigger tracker snapshot accepts distinct consumed and pending ids`() {
        assertDoesNotThrow {
            AiTriggerTrackerSnapshot(
                consumedTriggerIds = listOf("dungeon_lord_opening_war_cry"),
                pendingCombatStartTriggerIds = listOf("bandit_captain_opening_shield_bash"),
                engagedInCombat = true,
            ).validateOrThrow()
        }
    }

    @Test
    fun `ai trigger tracker snapshot rejects blank or duplicate ids`() {
        assertThrows(IllegalArgumentException::class.java) {
            AiTriggerTrackerSnapshot(consumedTriggerIds = listOf("")).validateOrThrow()
        }
        assertThrows(IllegalArgumentException::class.java) {
            AiTriggerTrackerSnapshot(pendingCombatStartTriggerIds = listOf("repeat", "repeat")).validateOrThrow()
        }
    }
}
