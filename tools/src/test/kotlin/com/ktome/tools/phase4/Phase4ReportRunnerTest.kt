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

        assertEquals(14, run.taskCount)
        assertEquals(0, run.failedTaskCount, "phase4Report should stay green once the landed owner guardrails are repaired; inspect ${run.summaryPath}")
        assertEquals(0, run.failedExperienceMetricCount, "phase4Report should not report residual failing owner metrics after the repair; inspect ${run.summaryPath}")
        assertEquals(run.failedTaskCount + run.failedExperienceMetricCount, run.failedGateCount)
        assertTrue(Files.exists(run.summaryPath), "Expected phase4 summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.markdownPath), "Expected phase4 markdown report at ${run.markdownPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val tasks = payload.getValue("tasks").jsonArray
        val metricCatalog = payload.getValue("metricCatalog").jsonArray
        val experienceMetrics = payload.getValue("experienceMetrics").jsonArray
        val taskIds = tasks.map { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content }.toSet()
        val solvabilityTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "solvabilityHarness" }.jsonObject
        val hiddenHarnessTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "hiddenContentHarness" }.jsonObject
        val organicHiddenTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "organicHiddenProbe" }.jsonObject
        val longRunTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "longRunLab" }.jsonObject
        val terrainTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "terrainInteractionBatch" }.jsonObject
        val lootTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "whiteBoxLoot" }.jsonObject
        val whiteBoxContentPackTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "whiteBoxContentPack" }.jsonObject
        val contentPackTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "contentPackHarness" }.jsonObject
        val experienceMetricIds =
            experienceMetrics.map { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content }.toSet()
        val metricCatalogIds =
            metricCatalog.map { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content }.toSet()
        val terrainAggregateMetric =
            experienceMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "terrainInteractionEncounterRate.aggregate" }.jsonObject
        val terrainLowerBoundMetric =
            experienceMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "terrainInteractionEncounterRate.per_zone_lower_bound" }.jsonObject
        val markdown = Files.readString(run.markdownPath)

        assertEquals("P4", payload.getValue("phaseId").jsonPrimitive.content)
        assertEquals("14", payload.getValue("taskCount").jsonPrimitive.content)
        assertEquals("0", payload.getValue("failedTaskCount").jsonPrimitive.content)
        assertEquals("1000", solvabilityTask.getValue("metrics").jsonObject.getValue("distinctSeedCount").jsonPrimitive.content)
        assertTrue(solvabilityTask.getValue("metrics").jsonObject.containsKey("providedDiscoveryTags"))
        assertTrue(solvabilityTask.getValue("metrics").jsonObject.containsKey("requiredHiddenAnchorFamilies"))
        assertTrue(solvabilityTask.getValue("metrics").jsonObject.containsKey("observedHiddenAnchorFamilies"))
        assertEquals("true", hiddenHarnessTask.getValue("metrics").jsonObject.getValue("scriptedVerification").jsonPrimitive.content)
        assertTrue(hiddenHarnessTask.getValue("metrics").jsonObject.containsKey("primerActionUsedCount"))
        assertEquals("false", organicHiddenTask.getValue("metrics").jsonObject.getValue("scriptedVerification").jsonPrimitive.content)
        assertTrue(organicHiddenTask.getValue("metrics").jsonObject.containsKey("organicHiddenDiscoveryRate"))
        assertTrue(organicHiddenTask.getValue("metrics").jsonObject.containsKey("searchActionUseRate"))
        assertTrue(longRunTask.getValue("metrics").jsonObject.containsKey("terminalWeaponBaseDiversity"))
        assertTrue(longRunTask.getValue("metrics").jsonObject.containsKey("crossProfessionTopWeaponDominance"))
        assertTrue(longRunTask.getValue("metrics").jsonObject.containsKey("professionAlignedWeaponAdoptionRate"))
        assertTrue(longRunTask.getValue("metrics").jsonObject.containsKey("professionTerminalWeaponDistribution"))
        assertTrue(longRunTask.getValue("metrics").jsonObject.containsKey("professionTopWeaponBaseIds"))
        assertTrue(longRunTask.getValue("metrics").jsonObject.containsKey("professionTopWeaponSemanticTags"))
        assertTrue(terrainTask.getValue("sourcePath").jsonPrimitive.content.contains("whitebox/terrain"))
        assertTrue(terrainTask.getValue("metrics").jsonObject.containsKey("combatSampledZoneIds"))
        assertTrue(terrainTask.getValue("metrics").jsonObject.containsKey("combatSampledZoneExclusionNotes"))
        assertTrue(terrainTask.getValue("metrics").jsonObject.containsKey("perZoneEncounterLowerBoundTarget"))
        assertTrue(terrainTask.getValue("metrics").jsonObject.containsKey("perZoneEncounterFailures"))
        assertTrue(tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "whiteBoxMapgen" }.jsonObject.getValue("metrics").jsonObject.containsKey("requiredHiddenAnchorFamilies"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("lootProfileBaseItemOverlapMatrix"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("sameZoneSecretVsCadenceMaxOverlap"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("sameZoneSecretVsRewardMaxOverlap"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("localIdentityFailurePairs"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("preflightCulpritPairCount"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("preflightCulpritReasons"))
        assertTrue(lootTask.getValue("metrics").jsonObject.containsKey("preflightCulpritPairs"))
        assertEquals("PASS", lootTask.getValue("status").jsonPrimitive.content)
        assertEquals("0", lootTask.getValue("metrics").jsonObject.getValue("failedAssertions").jsonPrimitive.content)
        assertEquals("13", contentPackTask.getValue("metrics").jsonObject.getValue("totalCases").jsonPrimitive.content)
        assertEquals("1", contentPackTask.getValue("metrics").jsonObject.getValue("legacyLootProfileSchemaRejectCount").jsonPrimitive.content)
        assertEquals(
            "loot.foundation.common",
            contentPackTask
                .getValue("metrics")
                .jsonObject
                .getValue("legacyLootProfileSchemaRejectSummaries")
                .jsonArray
                .single()
                .jsonObject
                .getValue("targetProfileId")
                .jsonPrimitive
                .content,
        )
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
        assertEquals(9, experienceMetrics.size)
        assertEquals(9, metricCatalog.size)
        assertEquals(
            setOf(
                "scriptedHiddenVerificationRate",
                "organicHiddenDiscoveryRate",
                "sameZoneSecretVsCadenceMaxOverlap",
                "sameZoneSecretVsRewardMaxOverlap",
                "terminalWeaponBaseDiversity",
                "crossProfessionTopWeaponDominance",
                "professionAlignedWeaponAdoptionRate",
                "terrainInteractionEncounterRate.aggregate",
                "terrainInteractionEncounterRate.per_zone_lower_bound",
            ),
            experienceMetricIds,
        )
        assertEquals(experienceMetricIds, metricCatalogIds)
        assertTrue(markdown.contains("## 指标 Owner 表"))
        assertTrue(markdown.contains("## Local Reward Identity"))
        assertTrue(markdown.contains("## Terminal Build Identity"))
        assertTrue(markdown.contains("## Scripted vs Organic Hidden"))
        assertTrue(markdown.contains("## Terrain Combat Sample Contract"))
        assertTrue(markdown.contains("- sourceTask: `whiteBoxLoot`"))
        assertTrue(markdown.contains("- sourceTask: `longRunLab`"))
        assertTrue(markdown.contains("- sourceTask.scripted: `hiddenContentHarness`"))
        assertTrue(markdown.contains("- sourceTask.organic: `organicHiddenProbe`"))
        assertTrue(markdown.contains("- sourceTask: `terrainInteractionBatch`"))
        assertTrue(markdown.contains("legacyLootProfileSchemaRejectSummaries"))
        assertTrue(markdown.contains("loot.foundation.common"))
        assertTrue(terrainAggregateMetric.getValue("currentValue").jsonObject.containsKey("baseline"))
        assertTrue(terrainAggregateMetric.getValue("currentValue").jsonObject.containsKey("relativeIncrease"))
        assertTrue(terrainAggregateMetric.getValue("currentValue").jsonObject.containsKey("targetRate"))
        assertTrue(terrainLowerBoundMetric.getValue("currentValue").jsonObject.containsKey("failureZones"))
        assertTrue(terrainLowerBoundMetric.getValue("currentValue").jsonObject.containsKey("combatSampledZoneIds"))
        assertTrue(terrainAggregateMetric.getValue("note").jsonPrimitive.content.contains("baseline="))
        assertTrue(terrainLowerBoundMetric.getValue("note").jsonPrimitive.content.contains("combatSampledZoneIds"))
        val terminalDiversityMetric =
            experienceMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "terminalWeaponBaseDiversity" }.jsonObject
        val topWeaponMetric =
            experienceMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "crossProfessionTopWeaponDominance" }.jsonObject
        val alignedWeaponMetric =
            experienceMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionAlignedWeaponAdoptionRate" }.jsonObject
        val organicHiddenMetric =
            experienceMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "organicHiddenDiscoveryRate" }.jsonObject
        assertTrue(terminalDiversityMetric.getValue("note").jsonPrimitive.content.contains("terminalBases="))
        assertTrue(terminalDiversityMetric.getValue("note").jsonPrimitive.content.contains("topWeaponSemantics="))
        assertTrue(topWeaponMetric.getValue("note").jsonPrimitive.content.contains("topWeaponBaseId="))
        assertTrue(alignedWeaponMetric.getValue("note").jsonPrimitive.content.contains("alignedSamples="))
        assertTrue(alignedWeaponMetric.getValue("note").jsonPrimitive.content.contains("topWeaponSemantics="))
        assertTrue(organicHiddenMetric.getValue("note").jsonPrimitive.content.contains("observationOnly=true"))
        assertEquals(
            setOf(
                "mapgenSmoke",
                "solvabilityHarness",
                "hiddenContentHarness",
                "organicHiddenProbe",
                "contentPackHarness",
                "bossHarness",
                "longRunLab",
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

    @Test
    fun `failed gate count includes failed experience metrics even when tasks pass`() {
        assertEquals(0, countFailedStatuses(listOf("PASS", "PASS")))
        assertEquals(1, countFailedStatuses(listOf("PASS", "FAIL", "PASS")))
    }
}
