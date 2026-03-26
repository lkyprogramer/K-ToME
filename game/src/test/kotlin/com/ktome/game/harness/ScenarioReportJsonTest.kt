package com.ktome.game.harness

import com.ktome.core.run.RunOutcome
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScenarioReportJsonTest {
    @Test
    fun `scenario report json includes zone headless milestones and captain trace`() {
        val json =
            ScenarioReport(
                name = "long-run-full-templar-human",
                seed = 20260360L,
                professionId = "templar",
                raceId = "human",
                success = false,
                outcome = RunOutcome.Defeat(floor = 2),
                floorReached = 2,
                turns = 153,
                headlessTurnEquivalent = 371,
                corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
                localeId = "en-US",
                buildHash = "templar#human#build",
                goalReached = true,
                zonePath = listOf("shattered_outpost"),
                zoneHeadlessMilestones =
                    listOf(
                        ZoneHeadlessMilestone(
                            zoneId = "shattered_outpost",
                            turnIndex = 0,
                            headlessTurnEquivalent = 0,
                            deltaTurns = 0,
                            deltaHeadlessTurns = 0,
                        ),
                    ),
                zoneObjectiveSummaries =
                    listOf(
                        ZoneObjectiveSummary(
                            zoneId = "shattered_outpost",
                            questId = "quest.shattered_outpost",
                            objectiveId = "breach",
                            state = com.ktome.core.world.ObjectiveState.COMPLETED,
                            completionFlagGranted = true,
                        ),
                    ),
                captainEncounterTrace =
                    listOf(
                        CaptainEncounterTraceEntry(
                            turnIndex = 153,
                            headlessTurnEquivalent = 371,
                            floor = 2,
                            playerHp = 0,
                            playerMaxHp = 60,
                            playerResourceCurrent = 5,
                            playerResourceMax = 20,
                            playerResourceTypeId = "POSITIVE_ENERGY",
                            captainHp = 18,
                            captainMaxHp = 38,
                            captainDistance = 1,
                            command = "Move",
                            recentMessages = listOf("强盗队长 命中 英雄，造成 30 点伤害。"),
                            recentEvents = listOf("damage:12002->1:30"),
                        ),
                    ),
            ).toJson().jsonObject

        val milestones = json.requiredArray("zoneHeadlessMilestones")
        assertEquals(1, milestones.size)
        assertEquals("shattered_outpost", milestones.single().jsonObject.requiredString("zoneId"))
        assertEquals(HarnessMetadata.BUILD_ID, json.requiredString("buildId"))
        assertEquals(HarnessMetadata.PHASE_ID, json.requiredString("phaseId"))
        assertEquals(HarnessMetadata.LONG_RUN_FULL_CORPUS_ID, json.requiredString("corpusId"))
        assertEquals("templar#human#build", json.requiredString("buildHash"))
        val objectives = json.requiredArray("zoneObjectiveSummaries")
        assertEquals(1, objectives.size)
        assertEquals("quest.shattered_outpost", objectives.single().jsonObject.requiredString("questId"))
        assertEquals("COMPLETED", objectives.single().jsonObject.requiredString("state"))

        val captainTrace = json.requiredArray("captainEncounterTrace")
        assertEquals(1, captainTrace.size)
        val traceEntry = captainTrace.single().jsonObject
        assertEquals("POSITIVE_ENERGY", traceEntry.requiredString("playerResourceTypeId"))
        assertEquals("Move", traceEntry.requiredString("command"))
        assertTrue(traceEntry.requiredArray("recentMessages").isNotEmpty())
        assertTrue(traceEntry.requiredArray("recentEvents").isNotEmpty())
    }

    private fun JsonObject.requiredString(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.requiredArray(key: String): JsonArray = getValue(key).jsonArray
}
