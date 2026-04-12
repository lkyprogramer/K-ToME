package com.ktome.tools.hidden

import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.harness.RunObservation
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class OrganicHiddenProbeRunnerTest {
    @Test
    @Tag("organicHiddenProbe")
    fun `organic hidden probe writes probe reports without primer metadata drift`() {
        val run = OrganicHiddenProbeRunner.run()

        assertEquals(500, run.totalCases)
        assertEquals(0, run.runtimeFailureCount, "organicHiddenProbe recorded runtime failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.eventsPath), "Expected event report at ${run.eventsPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val zones = payload.getValue("zones").jsonObject
        val notes = payload.getValue("notes").jsonArray

        assertEquals("false", summary.getValue("scriptedVerification").jsonPrimitive.content)
        assertEquals("0", summary.getValue("primerActionUsedCount").jsonPrimitive.content)
        assertEquals("500", summary.getValue("totalCases").jsonPrimitive.content)
        assertEquals("500", summary.getValue("distinctSeedCount").jsonPrimitive.content)
        assertTrue(summary.containsKey("searchActionUseCount"))
        assertTrue(summary.containsKey("searchActionUseRate"))
        assertTrue(summary.containsKey("searchAttemptCount"))
        assertTrue(summary.containsKey("organicHiddenDiscoveryRate"))
        assertTrue(summary.containsKey("secretZoneEntryRate"))
        assertTrue(summary.containsKey("averageFirstHiddenDiscoveryTurn"))
        assertEquals("organic-hidden-probe-bot-v4", summary.getValue("probeBotId").jsonPrimitive.content)
        assertTrue(
            summary.getValue("searchAttemptCount").jsonPrimitive.content.toInt() >= summary.getValue("searchActionUseCount").jsonPrimitive.content.toInt(),
            "organicHiddenProbe search accounting drifted.",
        )
        assertTrue(notes.any { note -> note.jsonPrimitive.content.contains("RunObservation-visible prompts") })
        assertEquals(setOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple"), zones.keys)
        assertEquals(500, Files.readAllLines(run.eventsPath).count { line -> line.isNotBlank() })
    }

    @Test
    fun `rejected search only suppresses the current observation`() {
        val bot = OrganicHiddenProbeBot(delegate = SmokeBot())
        val observation = searchableObservation(turnIndex = 10)

        assertEquals(PlayerCommand.Search, bot.searchCommand(observation))
        bot.recordRejectedSearch(observation)

        assertNull(bot.searchCommand(observation))
        assertEquals(PlayerCommand.Search, bot.searchCommand(observation.copy(turnIndex = observation.turnIndex + 1)))
    }

    @Test
    fun `accepted search consumes cooldown and searched position budget`() {
        val bot = OrganicHiddenProbeBot(delegate = SmokeBot())
        val observation = searchableObservation(turnIndex = 10)

        assertEquals(PlayerCommand.Search, bot.searchCommand(observation))
        bot.recordAcceptedSearch(observation)

        assertNull(bot.searchCommand(observation.copy(turnIndex = observation.turnIndex + 1, playerPosition = observation.playerPosition.copy(x = observation.playerPosition.x + 1))))
        assertNull(bot.searchCommand(observation.copy(turnIndex = observation.turnIndex + 4)))
        assertEquals(
            PlayerCommand.Search,
            bot.searchCommand(observation.copy(turnIndex = observation.turnIndex + 4, playerPosition = observation.playerPosition.copy(x = observation.playerPosition.x + 1))),
        )
    }

    private fun searchableObservation(turnIndex: Int): RunObservation {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260312L, playerProfessionId = FOUNDATION_PROFESSION_ID),
            )
        return RunObservationCapture.capture(session, turnIndex)
            .copy(
                searchPromptAvailable = true,
                activeRouteSelection = null,
                activeShopId = null,
                canDescend = false,
                visibleBossPositions = emptyList(),
                visibleHostilePositions = emptyList(),
            )
    }
}
