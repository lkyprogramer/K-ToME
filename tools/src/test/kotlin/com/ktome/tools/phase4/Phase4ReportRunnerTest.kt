package com.ktome.tools.phase4

import java.time.Duration
import java.time.Instant
import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class Phase4ReportRunnerTest {
    @Test
    @Tag("phase4Report")
    fun `phase4 report aggregates currently landed phase4 verification tasks`() {
        val run = Phase4ReportRunner.run()

        assertEquals(12, run.taskCount)
        assertEquals(0, run.failedTaskCount, "phase4Report recorded failed tasks; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected phase4 summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.markdownPath), "Expected phase4 markdown report at ${run.markdownPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val tasks = payload.getValue("tasks").jsonArray
        val experienceMetrics = payload.getValue("experienceMetrics").jsonArray
        val taskIds = tasks.map { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content }.toSet()
        val solvabilityTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "solvabilityHarness" }.jsonObject
        val bossTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "bossHarness" }.jsonObject
        val terrainTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "terrainInteractionBatch" }.jsonObject
        val lootTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "whiteBoxLoot" }.jsonObject
        val hiddenTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "whiteBoxHiddenContent" }.jsonObject
        val whiteBoxContentPackTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "whiteBoxContentPack" }.jsonObject
        val contentPackTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "contentPackHarness" }.jsonObject
        val experienceMetricIds =
            experienceMetrics.map { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content }.toSet()
        val markdown = Files.readString(run.markdownPath)

        assertEquals("P4", payload.getValue("phaseId").jsonPrimitive.content)
        assertEquals("12", payload.getValue("taskCount").jsonPrimitive.content)
        assertEquals("0", payload.getValue("failedTaskCount").jsonPrimitive.content)
        assertEquals("1000", solvabilityTask.getValue("metrics").jsonObject.getValue("distinctSeedCount").jsonPrimitive.content)
        assertTrue(bossTask.getValue("metrics").jsonObject.getValue("whiteBoxSummaryPath").jsonPrimitive.content.contains("whitebox/boss"))
        assertTrue(bossTask.getValue("metrics").jsonObject.containsKey("eliteMutationDistinctCount"))
        assertTrue(bossTask.getValue("metrics").jsonObject.containsKey("eliteMutationValidPairCount"))
        assertTrue(bossTask.getValue("metrics").jsonObject.containsKey("corpusAggregateMetrics"))
        assertTrue(bossTask.getValue("metrics").jsonObject.containsKey("perEncounterAggregateMetrics"))
        assertTrue(terrainTask.getValue("sourcePath").jsonPrimitive.content.contains("whitebox/terrain"))
        assertTrue(terrainTask.getValue("metrics").jsonObject.containsKey("terrainTaggedCombatExposureRate"))
        assertTrue(terrainTask.getValue("metrics").jsonObject.containsKey("terrainCoverageByZone"))
        assertTrue(terrainTask.getValue("metrics").jsonObject.containsKey("corpusAggregateMetrics"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("lootProfileBaseItemOverlapMatrix"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("uniqueArtifactMeaningfulSwapRate"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("corpusAggregateMetrics"))
        assertTrue(hiddenTask.getValue("metrics").jsonObject.containsKey("hiddenTriggerTypeCoverage"))
        assertTrue(hiddenTask.getValue("metrics").jsonObject.containsKey("secretEntranceBindingSet"))
        assertTrue(hiddenTask.getValue("metrics").jsonObject.containsKey("corpusAggregateMetrics"))
        assertEquals("11", contentPackTask.getValue("metrics").jsonObject.getValue("totalCases").jsonPrimitive.content)
        assertTrue(
            contentPackTask.getValue("metrics").jsonObject.getValue("whiteBoxSummaryPath").jsonPrimitive.content.contains("whitebox/content-pack"),
        )
        assertTrue(contentPackTask.getValue("metrics").jsonObject.containsKey("whiteBoxCorpusAggregateMetrics"))
        assertTrue(contentPackTask.getValue("metrics").jsonObject.containsKey("contentPackArtifactTimestamp"))
        assertTrue(contentPackTask.getValue("metrics").jsonObject.containsKey("whiteBoxContentPackArtifactTimestamp"))
        assertEquals(whiteBoxContentPackTask.getValue("buildId").jsonPrimitive.content, contentPackTask.getValue("buildId").jsonPrimitive.content)
        val contentPackTimestamp =
            Instant.parse(contentPackTask.getValue("metrics").jsonObject.getValue("contentPackArtifactTimestamp").jsonPrimitive.content)
        val whiteBoxContentPackTimestamp =
            Instant.parse(contentPackTask.getValue("metrics").jsonObject.getValue("whiteBoxContentPackArtifactTimestamp").jsonPrimitive.content)
        assertTrue(
            Duration.between(contentPackTimestamp, whiteBoxContentPackTimestamp).abs() <= Duration.ofMinutes(30),
            "content-pack artifact timestamps drifted beyond the freshness guard window.",
        )
        assertEquals(10, experienceMetrics.size)
        assertEquals(
            setOf(
                "eliteMutationDistinctCount",
                "eliteMutationValidPairCount",
                "lootProfileBaseItemOverlapMatrix",
                "lootProfileDistinctBaseItemCount",
                "affixPassiveCoverage",
                "hiddenTriggerTypeCoverage",
                "secretEntranceBindingCoverage",
                "terrainTaggedCombatExposureRate",
                "terrainInteractionEncounterRate",
                "uniqueArtifactMeaningfulSwapRate",
            ),
            experienceMetricIds,
        )
        assertTrue(markdown.contains("## 体验度量基线"))
        assertTrue(markdown.contains("uniqueArtifactMeaningfulSwapRate"))
        assertEquals(
            setOf(
                "mapgenSmoke",
                "solvabilityHarness",
                "hiddenContentHarness",
                "contentPackHarness",
                "bossHarness",
                "terrainInteractionBatch",
                "whiteBoxMapgen",
                "whiteBoxSolvability",
                "lootBalanceLab",
                "whiteBoxLoot",
                "whiteBoxHiddenContent",
                "whiteBoxContentPack",
            ),
            taskIds,
        )
    }
}
