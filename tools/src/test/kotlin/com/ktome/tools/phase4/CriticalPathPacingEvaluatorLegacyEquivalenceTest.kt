package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CriticalPathPacingEvaluatorLegacyEquivalenceTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("reportPhase4Fixture")
    fun `shared evaluator keeps pacing parity with legacy report projection`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir, includeLegacySummary = true)

        Phase4ReportFixtureTestSupport.withFixtureProperties(
            repoRoot = fixtureRepoRoot,
            aggregateReportDir = tempDir.resolve("aggregate-pacing-parity"),
        ) {
            val run = ReportPhase4Runner.run(compareLegacy = true)
            val canonicalOwnerMetrics =
                Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
                    .getValue("ownerMetrics")
                    .jsonArray
                    .filter { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content in pacingMetricIds }
                    .associateBy { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content }
            val legacyMetrics =
                Phase4ReportFixtureTestSupport.json.parseToJsonElement(
                    Files.readString(fixtureRepoRoot.resolve("tools/build/reports/phase4/phase4-summary.json")),
                ).jsonObject
                    .getValue("experienceMetrics")
                    .jsonArray
                    .filter { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content in pacingMetricIds }
                    .associateBy { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content }

            assertEquals(pacingMetricIds.toSet(), canonicalOwnerMetrics.keys)
            assertEquals(pacingMetricIds.toSet(), legacyMetrics.keys)
            pacingMetricIds.forEach { metricId ->
                val canonicalMetric = canonicalOwnerMetrics.getValue(metricId).jsonObject
                val legacyMetric = legacyMetrics.getValue(metricId).jsonObject

                assertEquals(legacyMetric.getValue("sourceTaskId"), canonicalMetric.getValue("sourceTaskId"))
                assertEquals(legacyMetric.getValue("currentValue"), canonicalMetric.getValue("currentValue"))
                assertEquals(legacyMetric.getValue("currentValueText"), canonicalMetric.getValue("currentValueText"))
                assertEquals(legacyMetric.getValue("target"), canonicalMetric.getValue("target"))
                assertEquals(
                    legacyMetric.getValue("status").jsonPrimitive.content,
                    normalizeCanonicalStatus(canonicalMetric.getValue("status").jsonPrimitive.content),
                )
            }
        }
    }

    private fun normalizeCanonicalStatus(status: String): String =
        if (status == "UNEXPECTED_REGRESSION") {
            "FAIL"
        } else {
            "PASS"
        }

    private companion object {
        val pacingMetricIds: List<String> =
            listOf(
                "avgObjectiveAcquireTurn",
                "avgVisibleHostileTurnCount",
                "avgEnemyTurns",
                "criticalPathCombatFloorSatisfied",
            )
    }
}
