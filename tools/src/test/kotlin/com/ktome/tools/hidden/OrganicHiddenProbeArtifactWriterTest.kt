package com.ktome.tools.hidden

import com.ktome.tools.mapgen.phase4HarnessHeader
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class OrganicHiddenProbeArtifactWriterTest {
    private companion object {
        const val PROBE_BOT_ID: String = "organic-hidden-probe-bot-v5"
        const val PROBE_TURN_BUDGET: Int = 48
        const val PROBE_MAX_FLOOR: Int = 1
    }

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private val json = Json { prettyPrint = true }
    private val header = phase4HarnessHeader(harnessId = "organicHiddenProbe", seedList = listOf(1L))

    @Test
    fun `artifact writer emits stable summary fields and markdown headings`() {
        val shardPath = tempDir.resolve("events.jsonl")
        Files.writeString(shardPath, "{\"seed\":1}\n")
        val summary = validSummary()

        val paths =
            OrganicHiddenProbeArtifactWriter.writeSummaryArtifacts(
                OrganicHiddenProbeArtifactWriteRequest(
                    outputDir = tempDir,
                    header = header,
                    summary = summary,
                    kernelCacheMetadata = buildJsonObject { put("cacheStatus", "MISS") },
                    shardEventPaths = listOf(shardPath),
                    probeBotId = PROBE_BOT_ID,
                    probeTurnBudget = PROBE_TURN_BUDGET,
                    probeMaxFloor = PROBE_MAX_FLOOR,
                    json = json,
                ),
            )
        val payload = Json.parseToJsonElement(Files.readString(paths.summaryPath)).jsonObject
        val markdown = Files.readString(paths.markdownPath)

        assertEquals(1, payload.getValue("summary").jsonObject.getValue("totalCases").jsonPrimitive.content.toInt())
        assertTrue(payload.getValue("summary").jsonObject.containsKey("leadDiscoveryRate"))
        assertTrue(payload.getValue("summary").jsonObject.containsKey("secretConversionRate"))
        assertEquals(PROBE_BOT_ID, payload.getValue("summary").jsonObject.getValue("probeBotId").jsonPrimitive.content)
        assertEquals(PROBE_TURN_BUDGET, payload.getValue("summary").jsonObject.getValue("probeTurnBudget").jsonPrimitive.content.toInt())
        assertEquals(PROBE_MAX_FLOOR, payload.getValue("summary").jsonObject.getValue("probeMaxFloor").jsonPrimitive.content.toInt())
        assertTrue(payload.containsKey("zones"))
        assertTrue(payload.containsKey("combinations"))
        assertTrue(markdown.contains("## Zone Discovery Distribution"))
        assertTrue(markdown.contains("## Combination Breakdown"))
    }

    @Test
    fun `artifact writer fails fast when required summary sections are missing`() {
        val invalidSummary = validSummary().copy(combinations = emptyList())

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                OrganicHiddenProbeArtifactWriter.writeSummaryArtifacts(
                    OrganicHiddenProbeArtifactWriteRequest(
                        outputDir = tempDir,
                        header = header,
                        summary = invalidSummary,
                        kernelCacheMetadata = buildJsonObject { put("cacheStatus", "MISS") },
                        shardEventPaths = emptyList(),
                        probeBotId = PROBE_BOT_ID,
                        probeTurnBudget = PROBE_TURN_BUDGET,
                        probeMaxFloor = PROBE_MAX_FLOOR,
                        json = json,
                    ),
                )
            }

        assertTrue(error.message.orEmpty().contains("combination breakdown"))
    }

    private fun validSummary(): OrganicHiddenProbeSummary =
        OrganicHiddenProbeSummary(
            totalCases = 1,
            distinctSeedCount = 1,
            runtimeFailureCount = 0,
            searchAttemptCount = 1,
            runsWithSearchActionCount = 1,
            searchActionUseCount = 1,
            runsWithSearchPromptCount = 1,
            searchPromptVisibleCount = 1,
            searchRevealCount = 1,
            discoveryWithoutPrimerCount = 1,
            secretZoneEntryCount = 1,
            averageFirstHiddenDiscoveryTurn = 5.0,
            averageFirstSecretZoneEntryTurn = 7.0,
            firstHiddenDiscoveryTurnP50 = 5,
            firstHiddenDiscoveryTurnP90 = 5,
            firstSecretZoneEntryTurnP50 = 7,
            firstSecretZoneEntryTurnP90 = 7,
            professionIds = listOf("vanguard"),
            raceIds = listOf("human"),
            seedsPerZoneCombo = 1,
            zoneBreakdown =
                mapOf(
                    "greenwood_fringe" to
                        OrganicHiddenProbeZoneMetrics(
                            caseCount = 1,
                            runsWithSearchActionCount = 1,
                            searchActionUseCount = 1,
                            runsWithSearchPromptCount = 1,
                            searchPromptVisibleCount = 1,
                            searchRevealCount = 1,
                            slagCueEligibleRoomCount = 0,
                            slagCueCandidateCount = 0,
                            discoveryWithoutPrimerCount = 1,
                            secretZoneEntryCount = 1,
                            averageFirstHiddenDiscoveryTurn = 5.0,
                            averageFirstSecretZoneEntryTurn = 7.0,
                            firstHiddenDiscoveryTurnP50 = 5,
                            firstHiddenDiscoveryTurnP90 = 5,
                            firstSecretZoneEntryTurnP50 = 7,
                            firstSecretZoneEntryTurnP90 = 7,
                        ),
                ),
            combinations =
                listOf(
                    OrganicHiddenProbeCombinationMetrics(
                        professionId = "vanguard",
                        raceId = "human",
                        caseCount = 1,
                        runsWithSearchActionCount = 1,
                        searchActionUseCount = 1,
                        runsWithSearchPromptCount = 1,
                        searchPromptVisibleCount = 1,
                        searchRevealCount = 1,
                        slagCueEligibleRoomCount = 0,
                        slagCueCandidateCount = 0,
                        discoveryWithoutPrimerCount = 1,
                        secretZoneEntryCount = 1,
                        averageFirstHiddenDiscoveryTurn = 5.0,
                        averageFirstSecretZoneEntryTurn = 7.0,
                        firstHiddenDiscoveryTurnP50 = 5,
                        firstHiddenDiscoveryTurnP90 = 5,
                        firstSecretZoneEntryTurnP50 = 7,
                        firstSecretZoneEntryTurnP90 = 7,
                    ),
                ),
            zoneDiscoveryDistribution = mapOf("greenwood_fringe" to 1.0),
            secretZoneDiscoveryDistribution = mapOf("secret.greenwood" to 1.0),
            topZoneLeadShare = 0.0,
            topZoneLeadShareZoneId = null,
            topZoneLeadShareDenominator = 0,
            zoneSearchPromptVisibility = 1.0,
            perZoneSecretConversionFloorReportOnly = mapOf("greenwood_fringe" to 1.0),
            secretZoneSearchConversionFloorReportOnly = mapOf("greenwood_fringe" to 1.0),
            perZoneSearchUseFloorReportOnly = mapOf("greenwood_fringe" to 1.0),
            slagCueDensityPerEligibleRoomReportOnly = emptyMap(),
            perZoneSecretEntryMinRate = 0.05,
            failingSecretEntryZoneIds = emptyList(),
        )
}
