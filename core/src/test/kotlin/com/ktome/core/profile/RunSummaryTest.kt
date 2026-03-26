package com.ktome.core.profile

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RunSummaryTest {
    @Test
    fun `victory summaries reject defeat reason`() {
        assertThrows(IllegalArgumentException::class.java) {
            RunSummary(
                seed = 1L,
                finishedAtEpochMillis = 2L,
                classId = "vanguard",
                raceId = "human",
                finalZoneId = "abyssal_heart",
                turnCount = 100,
                headlessTurnEquivalent = 140,
                zoneRouteHash = "shattered_outpost>greenwood_fringe>abyssal_heart",
                buildHash = "vanguard#human",
                rulesetVersion = "phase3",
                victory = true,
                defeatReason = "player_died",
            )
        }
    }
}
