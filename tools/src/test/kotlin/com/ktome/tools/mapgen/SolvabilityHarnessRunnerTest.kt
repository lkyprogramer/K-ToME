package com.ktome.tools.mapgen

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class SolvabilityHarnessRunnerTest {
    @Test
    @Tag("solvabilityHarness")
    fun `solvability harness writes summary and proof reports for the full fixed seed corpus`() {
        val run = SolvabilityHarnessRunner.run()

        assertEquals(1000, run.totalCases)
        assertEquals(0, run.failureCount)
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.proofsPath), "Expected proof report at ${run.proofsPath}")
        val summary = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val header = summary.getValue("header").jsonObject
        val summaryMetrics = summary.getValue("summary").jsonObject
        val firstProof =
            Json.parseToJsonElement(Files.readAllLines(run.proofsPath).first { line -> line.isNotBlank() }).jsonObject
        assertEquals("zh-CN", header.getValue("locale").jsonPrimitive.content)
        assertEquals("1000", summaryMetrics.getValue("totalCases").jsonPrimitive.content)
        assertEquals("1000", summaryMetrics.getValue("distinctSeedCount").jsonPrimitive.content)
        assertTrue(summaryMetrics.getValue("casesWithBacktrackProof").jsonPrimitive.content.toInt() > 0)
        assertTrue(summaryMetrics.getValue("providedDiscoveryTagCount").jsonPrimitive.content.toInt() > 0)
        assertEquals("0", summaryMetrics.getValue("hiddenAnchorFamilyFailureCount").jsonPrimitive.content)
        assertTrue(summaryMetrics.getValue("requiredHiddenAnchorFamilies").jsonArray.isNotEmpty())
        assertTrue(summaryMetrics.getValue("observedHiddenAnchorFamilies").jsonArray.isNotEmpty())
        assertTrue(firstProof.containsKey("providedDiscoveryTags"))
        assertTrue(firstProof.containsKey("hiddenAnchorFamiliesSatisfied"))
        assertTrue(firstProof.containsKey("requiredHiddenAnchorFamilies"))
        assertTrue(firstProof.containsKey("observedHiddenAnchorFamilies"))
        assertEquals(1000, header.getValue("seedList").jsonArray.map { element -> element.jsonPrimitive.content }.distinct().size)
        assertEquals(1000, Files.readAllLines(run.proofsPath).count { line -> line.isNotBlank() })
    }
}
