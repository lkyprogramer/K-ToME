package com.ktome.tools.phase4

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class ReportPhase4RunnerTest {
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

        assertTrue(terrainInput.getValue("evaluationResults").jsonArray.size >= 3)
        assertEquals("RELATIVE_BASELINE", terrainMetric.getValue("baselineMode").jsonPrimitive.content)
        assertEquals("BUDGET_THRESHOLD", lootMetric.getValue("baselineMode").jsonPrimitive.content)
        assertEquals("PASS", lootMetric.getValue("status").jsonPrimitive.content)
        assertTrue(payload.containsKey("metricCatalog"))
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
}
