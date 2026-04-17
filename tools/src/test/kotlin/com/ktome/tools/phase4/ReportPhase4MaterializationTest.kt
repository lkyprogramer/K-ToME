package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
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

class ReportPhase4MaterializationTest {
    @Test
    @Tag("reportPhase4")
    fun `reportPhase4 materializes canonical aggregate into configured repo output dir`() {
        val compareLegacy = System.getProperty("ktome.phase4.aggregate.compareLegacy")?.toBooleanStrictOrNull() ?: false
        val outputDir = configuredAggregateReportDir()
        Files.createDirectories(outputDir)
        val staleComparisonPath = outputDir.resolve("report-phase4-legacy-comparison.json")
        if (!compareLegacy) {
            Files.writeString(staleComparisonPath, """{"stale":true}""")
        }
        val startedAt = Instant.now()

        val run = ReportPhase4Runner.run(compareLegacy = compareLegacy)

        assertEquals(outputDir.resolve("report-phase4-summary.json").toAbsolutePath().normalize(), run.summaryPath.toAbsolutePath().normalize())
        assertEquals(outputDir.resolve("report-phase4-summary.md").toAbsolutePath().normalize(), run.markdownPath.toAbsolutePath().normalize())
        assertTrue(Files.exists(run.summaryPath), "Expected canonical summary at ${run.summaryPath}")
        assertTrue(Files.exists(run.markdownPath), "Expected canonical markdown at ${run.markdownPath}")
        assertTrue(
            !Files.getLastModifiedTime(run.summaryPath).toInstant().isBefore(startedAt.minusSeconds(1)),
            "Expected ${run.summaryPath} to be refreshed during this task execution.",
        )

        val payload = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val markdown = Files.readString(run.markdownPath)
        val ownerMetrics = payload.getValue("ownerMetrics").jsonArray

        assertEquals("P4", payload.getValue("phaseId").jsonPrimitive.content)
        assertEquals("14", payload.getValue("inputCount").jsonPrimitive.content)
        assertEquals("13", payload.getValue("ownerMetricCount").jsonPrimitive.content)
        assertEquals(13, ownerMetrics.size)
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "avgObjectiveAcquireTurn" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "avgVisibleHostileTurnCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "avgEnemyTurns" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "criticalPathCombatFloorSatisfied" })
        assertTrue(markdown.contains("## Critical Path Pacing"))
        assertTrue(markdown.contains("criticalPathZoneIds"))
        assertTrue(markdown.contains("criticalPathCombatFloorSatisfied"))

        if (compareLegacy) {
            assertNotNull(run.comparisonPath)
            val comparisonPath = requireNotNull(run.comparisonPath)
            assertTrue(Files.exists(comparisonPath), "Expected canonical parity report at $comparisonPath")
            val comparison = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(comparisonPath)).jsonObject
            assertEquals("0", comparison.getValue("mismatchCount").jsonPrimitive.content)
            assertEquals("13", comparison.getValue("metricCount").jsonPrimitive.content)
        } else {
            assertNull(run.comparisonPath)
            assertFalse(Files.exists(staleComparisonPath), "Canonical report-only run should remove stale legacy comparison artifacts.")
        }
    }

    private fun configuredAggregateReportDir(): Path {
        val configured = System.getProperty("ktome.phase4.aggregate.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "verification", "phase4")
        } else {
            Path.of(configured)
        }
    }
}
