package com.ktome.tools.contentpack

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class ContentPackHarnessRunnerTest {
    @Test
    @Tag("contentPackHarness")
    fun `content-pack harness writes fixed reports and validates runtime plus failure fixtures`() {
        val run = ContentPackHarnessRunner.run()

        assertEquals(11, run.totalCases)
        assertEquals(0, run.failureCount, "contentPackHarness recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.runsPath), "Expected runs report at ${run.runsPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val cases = payload.getValue("cases").jsonArray
        val replaceCase =
            cases.first { element -> element.jsonObject.getValue("fixtureId").jsonPrimitive.content == "replace_precedence" }.jsonObject

        assertEquals("PASS", summary.getValue("verdict").jsonPrimitive.content)
        assertEquals("11", summary.getValue("totalCases").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failureCount").jsonPrimitive.content)
        assertEquals("3", summary.getValue("successfulRuntimeCaseCount").jsonPrimitive.content)
        assertEquals("8", summary.getValue("expectedFailureCaseCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("diagnosticMismatchCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("localeResolutionFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("visualResolutionFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("audioResolutionFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("precedenceFailureCount").jsonPrimitive.content)
        assertEquals(
            listOf("valid_add_pack", "disabled_pack_fallback", "replace_precedence"),
            cases.take(3).map { element -> element.jsonObject.getValue("fixtureId").jsonPrimitive.content },
        )
        assertEquals(
            listOf("fixture.add_monster", "fixture.replace_monster"),
            replaceCase.getValue("resolvedOrder").jsonArray.map { value -> value.jsonPrimitive.content },
        )
        assertEquals(listOf("REPLACE"), replaceCase.getValue("overlayOps").jsonArray.map { value -> value.jsonPrimitive.content }.filter { op -> op != "ADD" })
        assertEquals(11, Files.readAllLines(run.runsPath).count { line -> line.isNotBlank() })
    }
}
