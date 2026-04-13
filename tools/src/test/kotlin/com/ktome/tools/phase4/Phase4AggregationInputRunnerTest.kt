package com.ktome.tools.phase4

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class Phase4AggregationInputRunnerTest {
    @Test
    @Tag("reportPhase4")
    @Tag("phase4AggregationInput")
    fun `phase4 aggregation inputs are cached and reusable across warm runs`() {
        val originalReportDir = System.getProperty("ktome.phase4.aggregate.reportDir")
        val tempReportDir = Files.createTempDirectory("ktome-phase4-aggregation-test")
        try {
            System.setProperty("ktome.phase4.aggregate.reportDir", tempReportDir.toString())

            val coldRun = Phase4AggregationInputRunner.materialize()
            val warmRun = Phase4AggregationInputRunner.materialize()

            assertEquals(14, coldRun.summary.inputCount)
            assertEquals(14, warmRun.summary.inputCount)
            assertEquals(0, coldRun.summary.reusedInputCount)
            assertEquals(14, warmRun.summary.reusedInputCount)
            assertEquals(14, warmRun.inputs.size)
            assertTrue(Files.exists(coldRun.summaryPath))
            assertTrue(Files.exists(warmRun.inputDir.resolve("terrainInteractionBatch.json")))

            val payload =
                Json.parseToJsonElement(Files.readString(warmRun.inputDir.resolve("terrainInteractionBatch.json"))).jsonObject
            val evaluations = payload.getValue("evaluationResults").jsonArray
            val renderMetadata = payload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject

            assertTrue(evaluations.any { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "terrain.aggregateRelativeBaseline" })
            assertTrue(evaluations.any { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "terrain.perZoneLowerBound" })
            assertTrue(renderMetadata.containsKey("baselineFingerprints"))
            assertEquals("HIT", renderMetadata.getValue("cacheStatus").jsonPrimitive.content)
            assertEquals("true", renderMetadata.getValue("artifactReused").jsonPrimitive.content)
        } finally {
            if (originalReportDir == null) {
                System.clearProperty("ktome.phase4.aggregate.reportDir")
            } else {
                System.setProperty("ktome.phase4.aggregate.reportDir", originalReportDir)
            }
        }
    }
}
