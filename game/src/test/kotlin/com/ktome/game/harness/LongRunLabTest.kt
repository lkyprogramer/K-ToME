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
import kotlinx.serialization.json.putJsonObject

class LongRunLabTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    fun `nightly long run lab meets first pass thresholds`() {
        val harness = HeadlessRunHarness(rootDir = tempDir)
        val officialSliceReports =
            listOf(
                "arcanist" to 20260313L,
            ).map { (professionId, seed) ->
                harness.run(
                    ScenarioSpec(
                        name = "long-run-$professionId-$seed",
                        seed = seed,
                        zoneId = FOUNDATION_ZONE_ROUTE.first(),
                        professionId = professionId,
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 0,
                        corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
                        maxTurns = 1800,
                        goal = ScenarioGoal.ReachTerminal,
                        saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 40),
                        assertions =
                            listOf(
                                ScenarioAssertion.CheckpointRoundTrip,
                                ScenarioAssertion.NoFailure,
                                ScenarioAssertion.NoStall,
                            ),
                    ),
                )
            }
        val advancedSmokeReports =
            listOf(
                "berserker" to 20260318L,
                "spellblade" to 20260319L,
            ).map { (professionId, seed) ->
                harness.run(
                    ScenarioSpec(
                        name = "long-run-advanced-$professionId-$seed",
                        seed = seed,
                        professionId = professionId,
                        zoneId = "abyssal_temple",
                        zoneRoute = listOf("abyssal_temple", "abyssal_heart"),
                        routeIndex = 0,
                        corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
                        maxTurns = 1600,
                        goal = ScenarioGoal.ReachZoneAtLeastOrTerminal("abyssal_heart"),
                        assertions = listOf(ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                )
            }
        val branchSmokeReports =
            listOf(
                "rogue" to 20260320L,
            ).map { (professionId, seed) ->
                harness.run(
                    ScenarioSpec(
                        name = "long-run-branch-$professionId-$seed",
                        seed = seed,
                        zoneId = FOUNDATION_ZONE_ROUTE.first(),
                        professionId = professionId,
                        zoneRoute =
                            listOf(
                                "shattered_outpost",
                                "greenwood_fringe",
                                "bandit_camp",
                                "greenwood_fringe",
                                "deep_iron_pit",
                                "grey_gate_depths",
                                "underground_river",
                                "abyssal_temple",
                                "abyssal_heart",
                            ),
                        routeIndex = 0,
                        corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
                        maxTurns = 2400,
                        goal = ScenarioGoal.ReachTerminal,
                        assertions = listOf(ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
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
                    corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
                    maxTurns = 900,
                    goal = ScenarioGoal.ReachFloor(2),
                    saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 40),
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
                    corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
                    maxTurns = 900,
                    goal = ScenarioGoal.ReachFloor(2),
                    saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 40),
                    assertions =
                        listOf(
                            ScenarioAssertion.ReachedFloorAtLeast(2),
                            ScenarioAssertion.CheckpointRoundTrip,
                            ScenarioAssertion.NoFailure,
                            ScenarioAssertion.NoStall,
                        ),
                    ),
            ).map(harness::run)
        val reports = officialSliceReports + advancedSmokeReports + branchSmokeReports + routeCoverageReports
        val nonVictoryReports = reports.filter { report -> report.outcome !is RunOutcome.Victory }
        val deathDistribution = nonVictoryReports.groupingBy(ScenarioReport::finalZoneId).eachCount().toSortedMap()
        val routeHashDistribution = reports.groupingBy(ScenarioReport::zoneRouteHash).eachCount().toSortedMap()
        val averageTurns = if (reports.isEmpty()) 0.0 else reports.map(ScenarioReport::turns).average()
        val averageHeadlessTurns = if (reports.isEmpty()) 0.0 else reports.map(ScenarioReport::headlessTurnEquivalent).average()

        val officialTerminalCount = officialSliceReports.count(ScenarioReport::success)
        val advancedGoalCount = advancedSmokeReports.count(ScenarioReport::success)
        val failingReports = reports.filterNot(ScenarioReport::success)
        val summary =
            buildJsonObject {
                put("sliceId", "phase3-pr07-long-run-smoke-v2")
                put("buildId", HarnessMetadata.BUILD_ID)
                put("phaseId", HarnessMetadata.PHASE_ID)
                put("rulesetVersion", HarnessMetadata.RULESET_VERSION)
                put("traceSchemaVersion", HarnessMetadata.TRACE_SCHEMA_VERSION)
                put("corpusId", HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID)
                put("profileId", HarnessMetadata.PROFILE_ID)
                put("localeId", reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed")
                put("seedCount", reports.size)
                put("officialSliceCount", officialSliceReports.size)
                put("advancedSmokeCount", advancedSmokeReports.size)
                put("branchSmokeCount", branchSmokeReports.size)
                put("routeProbeCount", routeCoverageReports.size)
                put("officialTerminalCount", officialTerminalCount)
                put("advancedGoalCount", advancedGoalCount)
                put("branchSmokeSuccesses", branchSmokeReports.count(ScenarioReport::success))
                put("routeCoverageSuccesses", routeCoverageReports.count(ScenarioReport::success))
                put("averageTurns", averageTurns)
                put("averageHeadlessTurns", averageHeadlessTurns)
                putJsonObject("deathDistribution") {
                    deathDistribution.forEach { (zoneId, count) -> put(zoneId, count) }
                }
                putJsonObject("zoneRouteHashDistribution") {
                    routeHashDistribution.forEach { (routeHash, count) -> put(routeHash, count) }
                }
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
                    appendLine("- sliceId: phase3-pr07-long-run-smoke-v2")
                    appendLine("- buildId: ${HarnessMetadata.BUILD_ID}")
                    appendLine("- phaseId: ${HarnessMetadata.PHASE_ID}")
                    appendLine("- rulesetVersion: ${HarnessMetadata.RULESET_VERSION}")
                    appendLine("- traceSchemaVersion: ${HarnessMetadata.TRACE_SCHEMA_VERSION}")
                    appendLine("- corpusId: ${HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID}")
                    appendLine("- profileId: ${HarnessMetadata.PROFILE_ID}")
                    appendLine("- localeId: ${reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed"}")
                    appendLine("- seeds: ${reports.size}")
                    appendLine("- officialTerminalCount: $officialTerminalCount/${officialSliceReports.size}")
                    appendLine("- advancedGoalCount: $advancedGoalCount/${advancedSmokeReports.size}")
                    appendLine("- branchSmokeSuccesses: ${branchSmokeReports.count(ScenarioReport::success)}/${branchSmokeReports.size}")
                    appendLine("- routeCoverageSuccesses: ${routeCoverageReports.count(ScenarioReport::success)}/${routeCoverageReports.size}")
                    appendLine("- averageTurns: $averageTurns")
                    appendLine("- averageHeadlessTurns: $averageHeadlessTurns")
                    appendLine("- deathDistribution: ${if (deathDistribution.isEmpty()) "none" else deathDistribution}")
                    appendLine("- zoneRouteHashDistribution: ${if (routeHashDistribution.isEmpty()) "none" else routeHashDistribution}")
                    reports.forEach { report ->
                        appendLine(
                            "- profession=${report.professionId}, race=${report.raceId}, seed=${report.seed}, zone=${report.zoneId}, routeIndex=${report.routeIndex}, success=${report.success}, floor=${report.floorReached}, turns=${report.turns}, headless=${report.headlessTurnEquivalent}, finalZone=${report.finalZoneId}, routeHash=${report.zoneRouteHash}, buildHash=${report.buildHash ?: "unknown"}, outcome=${report.outcome}",
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
            officialTerminalCount == officialSliceReports.size,
            "Expected all official-slice long-run probes to reach a terminal state, actual=$officialTerminalCount/${officialSliceReports.size}",
        )
        assertTrue(
            advancedGoalCount == advancedSmokeReports.size,
            "Expected advanced-class smoke probes to satisfy the late-route goal without harness failures, actual=$advancedGoalCount/${advancedSmokeReports.size}",
        )
        assertTrue(
            routeHashDistribution.size >= 2,
            "Expected smoke long-run lab to exercise at least two distinct route hashes, actual=$routeHashDistribution",
        )
        assertTrue(
            branchSmokeReports.all(ScenarioReport::success),
            "Expected optional-branch smoke probes to complete without harness failures.",
        )
    }
}
