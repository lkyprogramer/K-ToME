package com.ktome.game.harness

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class TemplarHumanCaptainRegressionTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    fun `templar human seed exits shattered outpost without dying to captain`() {
        val report =
            HeadlessRunHarness(rootDir = tempDir).run(
                ScenarioSpec(
                    name = "templar-human-captain-regression",
                    seed = 20260360L,
                    professionId = "templar",
                    raceId = "human",
                    maxTurns = 240,
                    goal = ScenarioGoal.SurviveTurns(220),
                    assertions =
                        listOf(
                            ScenarioAssertion.NoFailure,
                            ScenarioAssertion.NoStall,
                            ScenarioAssertion.FinalZoneAtLeast("greenwood_fringe"),
                        ),
                ),
            )

        assertTrue(
            report.success,
            buildString {
                append("Expected templar/human seed 20260360 to clear shattered_outpost. ")
                append("finalZone=${report.finalZoneId}, outcome=${report.outcome}, turns=${report.turns}, headless=${report.headlessTurnEquivalent}")
                if (report.assertionFailures.isNotEmpty()) {
                    append(", assertions=${report.assertionFailures}")
                }
                if (report.failureReason != null) {
                    append(", failure=${report.failureReason}")
                }
                if (report.stuckReason != null) {
                    append(", stuck=${report.stuckReason}")
                }
                if (report.captainEncounterTrace.isNotEmpty()) {
                    append(", captainTail=${report.captainEncounterTrace.takeLast(4)}")
                }
                if (report.zoneHeadlessMilestones.isNotEmpty()) {
                    append(", milestones=${report.zoneHeadlessMilestones}")
                }
                if (report.lastCommands.isNotEmpty()) {
                    append(", lastCommands=${report.lastCommands}")
                }
                if (report.commandStats.isNotEmpty()) {
                    append(", commandStats=${report.commandStats}")
                }
                if (report.lastMessages.isNotEmpty()) {
                    append(", lastMessages=${report.lastMessages}")
                }
                if (report.eventTail.isNotEmpty()) {
                    append(", eventTail=${report.eventTail}")
                }
            },
        )
    }
}
