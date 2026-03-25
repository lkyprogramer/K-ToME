package com.ktome.core.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdvancedClassUnlockTest {
    @Test
    fun `victory appends run history and unlocks matching advanced class`() {
        val summary =
            RunSummary(
                seed = 20260325L,
                finishedAtEpochMillis = 1L,
                classId = "vanguard",
                raceId = "human",
                finalZoneId = "abyssal_heart",
                turnCount = 1200,
                headlessTurnEquivalent = 1200,
                zoneRouteHash = "route-hash",
                buildHash = "build-hash",
                rulesetVersion = "phase3",
                victory = true,
            )

        val updated =
            ProfileProgression.appendRun(
                profile = ProfileData(),
                summary = summary,
                unlockRules = listOf(AdvancedClassUnlockRule(classId = "berserker", requiredProfessionId = "vanguard")),
            )

        assertEquals(listOf(summary), updated.runHistory)
        assertEquals(setOf("berserker"), updated.releaseUnlockedClasses)
    }

    @Test
    fun `defeat does not unlock advanced class`() {
        val updated =
            ProfileProgression.appendRun(
                profile = ProfileData(),
                summary =
                    RunSummary(
                        seed = 1L,
                        finishedAtEpochMillis = 1L,
                        classId = "arcanist",
                        raceId = "elf",
                        finalZoneId = "deep_iron_pit",
                        turnCount = 400,
                        headlessTurnEquivalent = 400,
                        zoneRouteHash = "route-hash",
                        buildHash = "build-hash",
                        rulesetVersion = "phase3",
                        victory = false,
                        defeatReason = "killed_by_boss",
                    ),
                unlockRules = listOf(AdvancedClassUnlockRule(classId = "spellblade", requiredProfessionId = "arcanist")),
            )

        assertTrue(updated.releaseUnlockedClasses.isEmpty())
    }
}
