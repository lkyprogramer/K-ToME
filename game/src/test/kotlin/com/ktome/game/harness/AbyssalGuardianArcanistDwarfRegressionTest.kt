package com.ktome.game.harness

import com.ktome.game.FOUNDATION_ZONE_ROUTE
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AbyssalGuardianArcanistDwarfRegressionTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `arcanist dwarf late route sample reaches the late route slice within headless gate after abyssal guardian split`() {
        val report =
            HeadlessRunHarness(rootDir = tempDir).run(
                ScenarioSpec(
                    name = "abyssal-guardian-arcanist-dwarf-regression",
                    seed = 20260342L,
                    zoneId = "underground_river",
                    professionId = "arcanist",
                    raceId = "dwarf",
                    zoneRoute = FOUNDATION_ZONE_ROUTE,
                    routeIndex = 4,
                    corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
                    maxTurns = 1800,
                    goal = ScenarioGoal.ReachTerminal,
                ),
            )

        assertTrue(
            report.finalZoneId in setOf("abyssal_temple", "abyssal_heart"),
            "Expected final zone to stay in the late-route slice, actual=${report.finalZoneId}",
        )
        assertTrue(
            !report.crashedOrStalled(),
            "Expected deterministic late-route sample without crash/stall, failure=${report.failureReason}, stuck=${report.stuckReason}, outcome=${report.outcome}",
        )
        assertTrue(
            report.headlessTurnEquivalent <= 3000,
            "Expected headlessTurnEquivalent <= 3000, actual=${report.headlessTurnEquivalent}",
        )
    }
}
