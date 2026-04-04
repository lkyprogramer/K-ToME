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

class Phase4ReportRunnerTest {
    @Test
    @Tag("phase4Report")
    fun `phase4 report aggregates currently landed phase4 verification tasks`() {
        val run = Phase4ReportRunner.run()

        assertEquals(5, run.taskCount)
        assertEquals(0, run.failedTaskCount, "phase4Report recorded failed tasks; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected phase4 summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.markdownPath), "Expected phase4 markdown report at ${run.markdownPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val tasks = payload.getValue("tasks").jsonArray
        val taskIds = tasks.map { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content }.toSet()
        val solvabilityTask =
            tasks.first { element -> element.jsonObject.getValue("taskId").jsonPrimitive.content == "solvabilityHarness" }.jsonObject

        assertEquals("P4", payload.getValue("phaseId").jsonPrimitive.content)
        assertEquals("5", payload.getValue("taskCount").jsonPrimitive.content)
        assertEquals("0", payload.getValue("failedTaskCount").jsonPrimitive.content)
        assertEquals("1000", solvabilityTask.getValue("metrics").jsonObject.getValue("distinctSeedCount").jsonPrimitive.content)
        assertEquals(
            setOf("mapgenSmoke", "solvabilityHarness", "bossHarness", "whiteBoxMapgen", "whiteBoxSolvability"),
            taskIds,
        )
    }
}
