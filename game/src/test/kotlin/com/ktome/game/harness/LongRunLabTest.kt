package com.ktome.game.harness

import com.ktome.core.run.RunOutcome
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class LongRunLabTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    fun `nightly long run lab meets first pass thresholds`() {
        val harness = HeadlessRunHarness(rootDir = tempDir)
        val officialSliceReports =
            listOf(
                "vanguard" to 20260312L,
                "arcanist" to 20260313L,
            ).map { (professionId, seed) ->
                harness.run(
                    ScenarioSpec(
                        name = "long-run-$professionId-$seed",
                        seed = seed,
                        professionId = professionId,
                        maxTurns = 900,
                        goal = ScenarioGoal.Victory,
                        assertions = listOf(ScenarioAssertion.Victory, ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                )
            }
        val routeCoverageReports =
            listOf(
                ScenarioSpec(
                    name = "long-run-rogue-deep-iron-pit-route-probe",
                    seed = 20260316L,
                    zoneId = "deep_iron_pit",
                    professionId = "rogue",
                    zoneRoute = FOUNDATION_ZONE_ROUTE,
                    routeIndex = 2,
                    maxTurns = 900,
                    goal = ScenarioGoal.ReachFloor(2),
                    saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 40, verifyRoundTrip = true),
                    assertions =
                        listOf(
                            ScenarioAssertion.ReachedFloorAtLeast(2),
                            ScenarioAssertion.CheckpointRoundTrip,
                            ScenarioAssertion.NoFailure,
                            ScenarioAssertion.NoStall,
                        ),
                ),
                ScenarioSpec(
                    name = "long-run-templar-grey-gate-depths-route-probe",
                    seed = 20260317L,
                    zoneId = "grey_gate_depths",
                    professionId = "templar",
                    zoneRoute = FOUNDATION_ZONE_ROUTE,
                    routeIndex = 3,
                    maxTurns = 900,
                    goal = ScenarioGoal.ReachFloor(2),
                    saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 40, verifyRoundTrip = true),
                    assertions =
                        listOf(
                            ScenarioAssertion.ReachedFloorAtLeast(2),
                            ScenarioAssertion.CheckpointRoundTrip,
                            ScenarioAssertion.NoFailure,
                            ScenarioAssertion.NoStall,
                        ),
                ),
            ).map(harness::run)
        val reports = officialSliceReports + routeCoverageReports

        val officialVictories = officialSliceReports.count { it.outcome is RunOutcome.Victory }
        val failingReports = reports.filterNot(ScenarioReport::success)
        val summary =
            buildJsonObject {
                put("sliceId", "phase2-short-run-route-coverage-v1")
                put("seedCount", reports.size)
                put("officialSliceCount", officialSliceReports.size)
                put("routeProbeCount", routeCoverageReports.size)
                put("officialVictories", officialVictories)
                put("routeCoverageSuccesses", routeCoverageReports.count(ScenarioReport::success))
                putJsonArray("reports") {
                    reports.forEach { add(it.toJson()) }
                }
            }

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "long-run-summary",
            payload = summary,
            markdown =
                buildString {
                    appendLine("# Long Run Lab")
                    appendLine("- sliceId: phase2-short-run-route-coverage-v1")
                    appendLine("- seeds: ${reports.size}")
                    appendLine("- officialVictories: $officialVictories/${officialSliceReports.size}")
                    appendLine("- routeCoverageSuccesses: ${routeCoverageReports.count(ScenarioReport::success)}/${routeCoverageReports.size}")
                    reports.forEach { report ->
                        appendLine(
                            "- profession=${report.professionId}, seed=${report.seed}, zone=${report.zoneId}, routeIndex=${report.routeIndex}, success=${report.success}, floor=${report.floorReached}, turns=${report.turns}, outcome=${report.outcome}",
                        )
                    }
                },
        )

        assertTrue(
            failingReports.isEmpty(),
            failingReports.joinToString(separator = "\n") { report ->
                val tail = (report.assertionFailures + listOfNotNull(report.failureReason, report.stuckReason)).joinToString()
                "${report.professionId}/${report.seed}/${report.zoneId}: ${tail.ifBlank { report.outcome.toString() }}"
            },
        )
        assertTrue(
            officialVictories == officialSliceReports.size,
            "Expected all official-slice long-run probes to end in victory, actual=$officialVictories/${officialSliceReports.size}",
        )
    }
}
