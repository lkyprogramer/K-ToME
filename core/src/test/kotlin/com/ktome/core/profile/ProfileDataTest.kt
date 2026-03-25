package com.ktome.core.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfileDataTest {
    @Test
    fun `profile codec round trips release unlocks and run history`() {
        val profile =
            ProfileData(
                releaseUnlockedClasses = setOf("berserker"),
                runHistory =
                    listOf(
                        RunSummary(
                            seed = 1L,
                            finishedAtEpochMillis = 2L,
                            classId = "vanguard",
                            raceId = "human",
                            finalZoneId = "abyssal_heart",
                            turnCount = 345,
                            headlessTurnEquivalent = 345,
                            zoneRouteHash = "route",
                            buildHash = "build",
                            rulesetVersion = "phase3",
                            victory = true,
                        ),
                    ),
            )

        val restored = ProfileCodec().decode(ProfileCodec().encode(profile))

        assertEquals(profile, restored)
    }

    @Test
    fun `default profile starts with no release unlocks`() {
        val profile = ProfileData()

        assertTrue(profile.releaseUnlockedClasses.isEmpty())
        assertTrue(profile.runHistory.isEmpty())
    }
}
