package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ReportPhase4RunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("reportPhase4")
    fun `reportPhase4 builds artifact only aggregate and optional legacy comparison`() {
        val compareLegacy = System.getProperty("ktome.phase4.aggregate.compareLegacy")?.toBooleanStrictOrNull() ?: false

        val run = ReportPhase4Runner.run(compareLegacy = compareLegacy)

        assertTrue(Files.exists(run.summaryPath), "Expected reportPhase4 summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.markdownPath), "Expected reportPhase4 markdown report at ${run.markdownPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val inputs = payload.getValue("inputs").jsonArray
        val ownerMetrics = payload.getValue("ownerMetrics").jsonArray
        val metricCatalog = payload.getValue("metricCatalog").jsonArray

        assertEquals("P4", payload.getValue("phaseId").jsonPrimitive.content)
        assertEquals("14", payload.getValue("inputCount").jsonPrimitive.content)
        assertEquals("9", payload.getValue("ownerMetricCount").jsonPrimitive.content)
        assertEquals("0", payload.getValue("unexpectedRegressionCount").jsonPrimitive.content)
        assertEquals("0", payload.getValue("approvedDebtCount").jsonPrimitive.content)
        assertEquals("0", payload.getValue("improvedDebtCount").jsonPrimitive.content)
        assertTrue(payload.containsKey("domainCacheHitRate"))
        assertTrue(payload.containsKey("artifactReuseRate"))
        assertTrue(payload.containsKey("topInvalidationReasons"))
        assertEquals(14, inputs.size)
        assertEquals(9, ownerMetrics.size)

        val terrainInput =
            inputs.first { input -> input.jsonObject.getValue("sourceTaskId").jsonPrimitive.content == "terrainInteractionBatch" }.jsonObject
        val terrainMetric =
            ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "terrainInteractionEncounterRate.aggregate" }.jsonObject
        val lootMetric =
            ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "sameZoneSecretVsCadenceMaxOverlap" }.jsonObject
        val lootCatalogMetric =
            metricCatalog.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "sameZoneSecretVsCadenceMaxOverlap" }.jsonObject

        assertTrue(terrainInput.getValue("evaluationResults").jsonArray.size >= 3)
        assertEquals("RELATIVE_BASELINE", terrainMetric.getValue("baselineMode").jsonPrimitive.content)
        assertEquals("BUDGET_THRESHOLD", lootMetric.getValue("baselineMode").jsonPrimitive.content)
        assertEquals("PASS", lootMetric.getValue("status").jsonPrimitive.content)
        assertEquals("< 0.750", lootMetric.getValue("target").jsonPrimitive.content)
        assertEquals(lootMetric.getValue("target").jsonPrimitive.content, lootCatalogMetric.getValue("target").jsonPrimitive.content)
        assertTrue(terrainInput.getValue("renderResult").jsonObject.getValue("metadata").jsonObject.containsKey("cacheStatus"))
        assertTrue(terrainInput.getValue("renderResult").jsonObject.getValue("metadata").jsonObject.containsKey("sourceArtifactFingerprint"))

        if (compareLegacy) {
            assertNotNull(run.comparisonPath)
            val comparison = Json.parseToJsonElement(Files.readString(run.comparisonPath!!)).jsonObject
            assertEquals("0", comparison.getValue("mismatchCount").jsonPrimitive.content)
            assertEquals("9", comparison.getValue("metricCount").jsonPrimitive.content)
        } else {
            assertNull(run.comparisonPath)
        }
    }

    @Test
    @Tag("reportPhase4")
    fun `reportPhase4 removes stale legacy comparison artifact on canonical runs`() {
        val originalReportDir = System.getProperty("ktome.phase4.aggregate.reportDir")
        val staleComparisonPath = tempDir.resolve("report-phase4-legacy-comparison.json")
        Files.writeString(staleComparisonPath, """{"stale":true}""")

        try {
            System.setProperty("ktome.phase4.aggregate.reportDir", tempDir.toString())

            val run = ReportPhase4Runner.run(compareLegacy = false)

            assertTrue(Files.exists(run.summaryPath))
            assertFalse(Files.exists(staleComparisonPath), "Canonical phase4Report run should delete stale parity artifacts from the default output directory.")
            assertNull(run.comparisonPath)
        } finally {
            if (originalReportDir == null) {
                System.clearProperty("ktome.phase4.aggregate.reportDir")
            } else {
                System.setProperty("ktome.phase4.aggregate.reportDir", originalReportDir)
            }
        }
    }
}
