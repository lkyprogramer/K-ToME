package com.ktome.tools.hidden

import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class OrganicHiddenProbeRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("organicHiddenProbe")
    fun `organic hidden probe writes probe reports without primer metadata drift`() {
        val originalReportDir = System.getProperty("ktome.phase4.hidden.reportDir")
        val repoRoot = VerificationCacheSupport.repoRoot()
        val cacheDirs = VerificationCacheSupport.cacheDirs(domainId = "organic-hidden", repoRoot = repoRoot)
        val isolatedTestRun = originalReportDir == null
        if (isolatedTestRun) {
            VerificationCacheSupport.clearDirectory(cacheDirs.kernelDir)
        }
        try {
            val effectiveReportDir = originalReportDir ?: tempDir.resolve("hidden-reports").toString()
            System.setProperty("ktome.phase4.hidden.reportDir", effectiveReportDir)
            val coldRun = OrganicHiddenProbeRunner.run()
            val coldPayload = Json.parseToJsonElement(Files.readString(coldRun.summaryPath)).jsonObject
            val run = OrganicHiddenProbeRunner.run()

            assertEquals(528, run.totalCases)
            assertEquals(0, run.runtimeFailureCount, "organicHiddenProbe recorded runtime failures; inspect ${run.summaryPath}")
            assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
            assertTrue(Files.exists(run.eventsPath), "Expected event report at ${run.eventsPath}")
            assertTrue(Files.exists(run.markdownPath), "Expected markdown report at ${run.markdownPath}")

            val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
            val markdown = Files.readString(run.markdownPath)
            val summary = payload.getValue("summary").jsonObject
            val zones = payload.getValue("zones").jsonObject
            val combinations = payload.getValue("combinations").jsonArray
            val zoneDiscoveryDistribution = payload.getValue("zoneDiscoveryDistribution").jsonObject
            val secretZoneDiscoveryDistribution = payload.getValue("secretZoneDiscoveryDistribution").jsonObject
            val notes = payload.getValue("notes").jsonArray
            val coldKernelCache = coldPayload.getValue("kernelCache").jsonObject
            val kernelCache = payload.getValue("kernelCache").jsonObject
            val firstEvent = Json.parseToJsonElement(Files.readAllLines(run.eventsPath).first { line -> line.isNotBlank() }).jsonObject

            if (isolatedTestRun) {
                assertEquals("MISS", coldKernelCache.getValue("cacheStatus").jsonPrimitive.content)
            }
            assertEquals("HIT", kernelCache.getValue("cacheStatus").jsonPrimitive.content)
            assertEquals("false", summary.getValue("scriptedVerification").jsonPrimitive.content)
            assertEquals("0", summary.getValue("primerActionUsedCount").jsonPrimitive.content)
            assertEquals("528", summary.getValue("totalCases").jsonPrimitive.content)
            assertEquals("528", summary.getValue("distinctSeedCount").jsonPrimitive.content)
            assertEquals("12", summary.getValue("comboCount").jsonPrimitive.content)
            assertEquals("11", summary.getValue("seedsPerZoneCombo").jsonPrimitive.content)
            assertEquals("true", summary.getValue("searchPromptRequired").jsonPrimitive.content)
            assertEquals("true", summary.getValue("reactiveSearchOnly").jsonPrimitive.content)
            assertTrue(summary.containsKey("searchActionUseCount"))
            assertTrue(summary.containsKey("searchActionUseRate"))
            assertTrue(summary.containsKey("searchAttemptCount"))
            assertTrue(summary.containsKey("leadDiscoveryRate"))
            assertTrue(summary.containsKey("secretConversionRate"))
            assertTrue(summary.containsKey("secretZoneEntryRate"))
            assertTrue(summary.containsKey("averageFirstHiddenDiscoveryTurn"))
            assertTrue(summary.containsKey("firstHiddenDiscoveryTurnP50"))
            assertTrue(summary.containsKey("firstHiddenDiscoveryTurnP90"))
            assertTrue(summary.containsKey("firstSecretZoneEntryTurnP50"))
            assertTrue(summary.containsKey("firstSecretZoneEntryTurnP90"))
            assertEquals("organic-hidden-probe-bot-v5", summary.getValue("probeBotId").jsonPrimitive.content)
            assertTrue(
                summary.getValue("searchAttemptCount").jsonPrimitive.content.toInt() >= summary.getValue("searchActionUseCount").jsonPrimitive.content.toInt(),
                "organicHiddenProbe search accounting drifted.",
            )
            assertEquals(listOf("vanguard", "arcanist", "rogue", "templar"), summary.getValue("professionIds").jsonArray.map { it.jsonPrimitive.content })
            assertEquals(listOf("human", "elf", "dwarf"), summary.getValue("raceIds").jsonArray.map { it.jsonPrimitive.content })
            assertTrue(notes.any { note -> note.jsonPrimitive.content.contains("RunObservation-visible prompts") })
            assertTrue(notes.any { note -> note.jsonPrimitive.content.contains("4 profession x 3 race") })
            assertTrue(notes.any { note -> note.jsonPrimitive.content.contains("highest-priority clue") })
            assertEquals(setOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple"), zones.keys)
            assertEquals(12, combinations.size)
            assertEquals(zones.keys, zoneDiscoveryDistribution.keys)
            assertTrue(secretZoneDiscoveryDistribution.isNotEmpty())
            assertEquals(44, combinations.first().jsonObject.getValue("caseCount").jsonPrimitive.content.toInt())
            assertTrue(firstEvent.containsKey("professionId"))
            assertTrue(firstEvent.containsKey("raceId"))
            assertEquals(528, Files.readAllLines(run.eventsPath).count { line -> line.isNotBlank() })
            assertTrue(markdown.contains("## Zone Discovery Distribution"))
            assertTrue(markdown.contains("## Secret-Zone Discovery Distribution"))
            assertTrue(markdown.contains("## Combination Breakdown"))
            assertTrue(markdown.contains("searchPromptRequired: `true`"))
            assertTrue(markdown.contains("leadDiscoveryRate"))
            assertTrue(markdown.contains("secretConversionRate"))
            assertTrue(markdown.contains("organicHiddenProbe is a standalone owner artifact"))
        } finally {
            if (originalReportDir == null) {
                System.clearProperty("ktome.phase4.hidden.reportDir")
            } else {
                System.setProperty("ktome.phase4.hidden.reportDir", originalReportDir)
            }
        }
    }

}
