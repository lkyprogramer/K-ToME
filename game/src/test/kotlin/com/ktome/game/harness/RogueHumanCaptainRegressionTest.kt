package com.ktome.game.harness

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RogueHumanCaptainRegressionTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    fun `rogue human seed exits shattered outpost without stalling at captain`() {
        val report =
            HeadlessRunHarness(rootDir = tempDir).run(
                ScenarioSpec(
                    name = "rogue-human-captain-regression",
                    seed = 20260350L,
                    professionId = "rogue",
                    raceId = "human",
                    maxTurns = 280,
                    goal = ScenarioGoal.SurviveTurns(240),
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
                append("Expected rogue/human seed 20260350 to clear shattered_outpost. ")
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
