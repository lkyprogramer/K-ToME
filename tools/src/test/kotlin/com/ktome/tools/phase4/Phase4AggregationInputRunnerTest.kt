package com.ktome.tools.phase4

import com.ktome.tools.verification.VerificationCacheSupport
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
            val lootPayload =
                Json.parseToJsonElement(Files.readString(warmRun.inputDir.resolve("whiteBoxLoot.json"))).jsonObject
            val lootEvaluationIds =
                lootPayload
                    .getValue("evaluationResults")
                    .jsonArray
                    .map { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content }

            assertTrue(evaluations.any { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "terrain.aggregateRelativeBaseline" })
            assertTrue(evaluations.any { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "terrain.perZoneLowerBound" })
            assertTrue(lootEvaluationIds.contains("loot.localRewardIdentity"))
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

    @Test
    @Tag("reportPhase4")
    @Tag("phase4AggregationInput")
    fun `longrun owner baseline change only regenerates longrun aggregation input`() {
        val repoRoot = VerificationCacheSupport.repoRoot()
        val originalReportDir = System.getProperty("ktome.phase4.aggregate.reportDir")
        val originalBaselineOverride = System.getProperty("ktome.phase4.ownerBaselineOverride.longRunLab")
        val tempReportDir = Files.createTempDirectory("ktome-phase4-aggregation-baseline-test")
        val baselineCopy = tempReportDir.resolve("phase4-terminal-build-baseline.json")
        Files.copy(repoRoot.resolve(Phase4OwnerBaselineRegistry.TERMINAL_BUILD_BASELINE_RELATIVE_PATH), baselineCopy)
        try {
            System.setProperty("ktome.phase4.aggregate.reportDir", tempReportDir.toString())
            System.setProperty("ktome.phase4.ownerBaselineOverride.longRunLab", baselineCopy.toString())

            val firstRun = Phase4AggregationInputRunner.materialize()
            val firstLongRunPayload =
                Json.parseToJsonElement(Files.readString(firstRun.inputDir.resolve("longRunLab.json"))).jsonObject
            val firstLongRunMetadata = firstLongRunPayload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject
            val firstLongRunFingerprint = firstLongRunMetadata.getValue("sourceArtifactFingerprint").jsonPrimitive.content

            Phase4OwnerBaselineTestSupport.stampBaselineMetadata(baselineCopy, marker = "report-only-longrun-baseline")

            val secondRun = Phase4AggregationInputRunner.materialize()
            val secondLongRunPayload =
                Json.parseToJsonElement(Files.readString(secondRun.inputDir.resolve("longRunLab.json"))).jsonObject
            val secondLongRunMetadata = secondLongRunPayload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject
            val secondLootPayload =
                Json.parseToJsonElement(Files.readString(secondRun.inputDir.resolve("whiteBoxLoot.json"))).jsonObject
            val secondLootMetadata = secondLootPayload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject

            assertEquals(14, secondRun.summary.inputCount)
            assertEquals(13, secondRun.summary.reusedInputCount)
            assertEquals(1, secondRun.summary.regeneratedInputCount)
            assertEquals("MISS", secondLongRunMetadata.getValue("cacheStatus").jsonPrimitive.content)
            assertEquals("baseline-changed", secondLongRunMetadata.getValue("invalidationReason").jsonPrimitive.content)
            assertEquals(firstLongRunFingerprint, secondLongRunMetadata.getValue("sourceArtifactFingerprint").jsonPrimitive.content)
            assertEquals("HIT", secondLootMetadata.getValue("cacheStatus").jsonPrimitive.content)
            assertEquals("true", secondLootMetadata.getValue("artifactReused").jsonPrimitive.content)
        } finally {
            if (originalReportDir == null) {
                System.clearProperty("ktome.phase4.aggregate.reportDir")
            } else {
                System.setProperty("ktome.phase4.aggregate.reportDir", originalReportDir)
            }
            if (originalBaselineOverride == null) {
                System.clearProperty("ktome.phase4.ownerBaselineOverride.longRunLab")
            } else {
                System.setProperty("ktome.phase4.ownerBaselineOverride.longRunLab", originalBaselineOverride)
            }
        }
    }

}
