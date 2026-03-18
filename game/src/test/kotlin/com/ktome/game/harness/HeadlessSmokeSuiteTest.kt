package com.ktome.game.harness

import com.ktome.game.FOUNDATION_PROFESSION_ID
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
                        name = "reach-floor-2",
                        seed = 20260312L,
                        professionId = FOUNDATION_PROFESSION_ID,
                        maxTurns = 300,
                        goal = ScenarioGoal.ReachFloor(2),
                        assertions = listOf(ScenarioAssertion.ReachedFloorAtLeast(2), ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                ),
                harness.run(
                    ScenarioSpec(
                        name = "save-load-round-trip",
                        seed = 20260313L,
                        professionId = FOUNDATION_PROFESSION_ID,
                        maxTurns = 450,
                        goal = ScenarioGoal.ReachFloor(2),
                        saveLoadCheckpoint = SaveLoadCheckpoint(floor = 2, continueTurns = 100, verifyRoundTrip = true),
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
                        name = "reach-floor-3",
                        seed = 20260316L,
                        professionId = FOUNDATION_PROFESSION_ID,
                        maxTurns = 1200,
                        goal = ScenarioGoal.ReachFloor(3),
                        assertions = listOf(ScenarioAssertion.ReachedFloorAtLeast(3), ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
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
                        appendLine("- ${report.name}: success=${report.success}, floor=${report.floorReached}, turns=${report.turns}, outcome=${report.outcome}")
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
        put("professionId", professionId)
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
