package com.ktome.tools.mapgen

import java.nio.file.Files
import kotlinx.serialization.json.Json
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
        assertEquals("zh-CN", summary.getValue("header").jsonObject.getValue("locale").jsonPrimitive.content)
        assertEquals("1000", summary.getValue("summary").jsonObject.getValue("totalCases").jsonPrimitive.content)
        assertTrue(summary.getValue("summary").jsonObject.getValue("casesWithBacktrackProof").jsonPrimitive.content.toInt() > 0)
        assertEquals(1000, Files.readAllLines(run.proofsPath).count { line -> line.isNotBlank() })
    }
}

