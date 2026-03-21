package com.ktome.game.harness

import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.FOUNDATION_ZONE_ID
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class HeadlessSmokeSuiteTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("headlessSmoke")
    fun `pr headless smoke scenarios are green`() {
        val harness = HeadlessRunHarness(rootDir = tempDir)
        val reports =
            listOf(
                harness.run(
                    ScenarioSpec(
                        name = "vanguard-shattered-outpost",
                        seed = 20260312L,
                        zoneId = FOUNDATION_ZONE_ID,
                        professionId = FOUNDATION_PROFESSION_ID,
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 0,
                        maxTurns = 450,
                        goal = ScenarioGoal.ReachFloor(2),
                        assertions = listOf(ScenarioAssertion.ReachedFloorAtLeast(2), ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                ),
                harness.run(
                    ScenarioSpec(
                        name = "arcanist-shattered-outpost-save-load",
                        seed = 20260313L,
                        zoneId = FOUNDATION_ZONE_ID,
                        professionId = "arcanist",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 0,
                        maxTurns = 450,
                        goal = ScenarioGoal.ReachFloor(2),
                        saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 10, verifyRoundTrip = true),
                        assertions =
                            listOf(
                                ScenarioAssertion.ReachedFloorAtLeast(2),
                                ScenarioAssertion.CheckpointRoundTrip,
                                ScenarioAssertion.NoFailure,
                                ScenarioAssertion.NoStall,
                            ),
                    ),
                ),
                harness.run(
                    ScenarioSpec(
                        name = "rogue-deep-iron-pit",
                        seed = 20260316L,
                        zoneId = "deep_iron_pit",
                        professionId = "rogue",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 2,
                        maxTurns = 600,
                        goal = ScenarioGoal.ReachFloor(2),
                        assertions = listOf(ScenarioAssertion.ReachedFloorAtLeast(2), ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                ),
                harness.run(
                    ScenarioSpec(
                        name = "templar-grey-gate-depths",
                        seed = 20260317L,
                        zoneId = "grey_gate_depths",
                        professionId = "templar",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 3,
                        maxTurns = 600,
                        goal = ScenarioGoal.ReachFloor(2),
                        assertions = listOf(ScenarioAssertion.ReachedFloorAtLeast(2), ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                ),
            )

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "headless-smoke",
            payload =
                buildJsonArray {
                    reports.forEach { report ->
                        add(report.toJson())
                    }
                },
            markdown =
                buildString {
                    appendLine("# Headless Smoke")
                    reports.forEach { report ->
                        appendLine(
                            "- ${report.name}: success=${report.success}, zone=${report.zoneId}, routeIndex=${report.routeIndex}, profession=${report.professionId}, floor=${report.floorReached}, turns=${report.turns}, outcome=${report.outcome}",
                        )
                    }
                },
        )

        assertTrue(reports.all { it.success }, reports.joinToString(separator = "\n") { "${it.name}: ${it.failureReason ?: it.stuckReason ?: it.assertionFailures.joinToString()}" })
    }
}

internal fun ScenarioReport.toJson() =
    buildJsonObject {
        put("name", name)
        put("seed", seed)
        put("zoneId", zoneId)
        put("professionId", professionId)
        put("routeIndex", routeIndex)
        put("success", success)
        put("outcome", outcome.toString())
        put("floorReached", floorReached)
        put("turns", turns)
        put("goalReached", goalReached)
        failureReason?.let { put("failureReason", it) }
        stuckReason?.let { put("stuckReason", it) }
        put("checkpointRoundTripVerified", checkpointRoundTripVerified)
        putJsonArray("lastCommands") { lastCommands.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("lastMessages") { lastMessages.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("eventTail") { eventTail.forEach { add(JsonPrimitive(it)) } }
    }
