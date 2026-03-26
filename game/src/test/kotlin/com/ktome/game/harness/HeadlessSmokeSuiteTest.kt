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
                        saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 10),
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
        put("raceId", raceId)
        put("routeIndex", routeIndex)
        put("finalZoneId", finalZoneId)
        put("zoneRouteHash", zoneRouteHash)
        put("buildId", buildId)
        put("phaseId", phaseId)
        put("rulesetVersion", rulesetVersion)
        put("traceSchemaVersion", traceSchemaVersion)
        put("corpusId", corpusId)
        put("localeId", localeId)
        put("profileId", profileId)
        buildHash?.let { put("buildHash", it) }
        put("success", success)
        put("outcome", outcome.toString())
        put("floorReached", floorReached)
        put("turns", turns)
        put("headlessTurnEquivalent", headlessTurnEquivalent)
        put("goalReached", goalReached)
        failureReason?.let { put("failureReason", it) }
        stuckReason?.let { put("stuckReason", it) }
        put("checkpointRoundTripVerified", checkpointRoundTripVerified)
        putJsonArray("zonePath") { zonePath.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("zoneHeadlessMilestones") {
            zoneHeadlessMilestones.forEach { milestone ->
                add(
                    buildJsonObject {
                        put("zoneId", milestone.zoneId)
                        put("turnIndex", milestone.turnIndex)
                        put("headlessTurnEquivalent", milestone.headlessTurnEquivalent)
                        put("deltaTurns", milestone.deltaTurns)
                        put("deltaHeadlessTurns", milestone.deltaHeadlessTurns)
                    },
                )
            }
        }
        putJsonArray("zoneObjectiveSummaries") {
            zoneObjectiveSummaries.forEach { summary ->
                add(
                    buildJsonObject {
                        put("zoneId", summary.zoneId)
                        put("questId", summary.questId)
                        put("objectiveId", summary.objectiveId)
                        put("state", summary.state.name)
                        put("completionFlagGranted", summary.completionFlagGranted)
                    },
                )
            }
        }
        putJsonArray("captainEncounterTrace") {
            captainEncounterTrace.forEach { entry ->
                add(
                    buildJsonObject {
                        put("turnIndex", entry.turnIndex)
                        put("headlessTurnEquivalent", entry.headlessTurnEquivalent)
                        put("floor", entry.floor)
                        put("playerHp", entry.playerHp)
                        put("playerMaxHp", entry.playerMaxHp)
                        put("playerResourceCurrent", entry.playerResourceCurrent)
                        put("playerResourceMax", entry.playerResourceMax)
                        put("playerResourceTypeId", entry.playerResourceTypeId)
                        entry.captainHp?.let { put("captainHp", it) }
                        entry.captainMaxHp?.let { put("captainMaxHp", it) }
                        entry.captainDistance?.let { put("captainDistance", it) }
                        entry.command?.let { put("command", it) }
                        putJsonArray("recentMessages") { entry.recentMessages.forEach { add(JsonPrimitive(it)) } }
                        putJsonArray("recentEvents") { entry.recentEvents.forEach { add(JsonPrimitive(it)) } }
                    },
                )
            }
        }
        putJsonArray("lastCommands") { lastCommands.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("lastMessages") { lastMessages.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("eventTail") { eventTail.forEach { add(JsonPrimitive(it)) } }
    }
