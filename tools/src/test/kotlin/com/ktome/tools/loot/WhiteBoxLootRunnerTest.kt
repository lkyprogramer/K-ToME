package com.ktome.tools.loot

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class WhiteBoxLootRunnerTest {
    @Test
    @Tag("whiteBoxLoot")
    fun `white-box loot writes standard reports and zero failed assertions`() {
        val run = WhiteBoxLootRunner.run()

        assertEquals(6, run.caseCount)
        assertEquals(0, run.failedAssertions, "whiteBoxLoot recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.casesPath), "Expected case report at ${run.casesPath}")
        assertTrue(Files.exists(run.reportPath), "Expected markdown report at ${run.reportPath}")
        assertTrue(Files.isDirectory(run.summaryPath.parent.resolve("artifacts")), "Expected artifacts directory beside ${run.summaryPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val aggregates = payload.getValue("aggregates").jsonArray

        assertEquals("loot", payload.getValue("domainId").jsonPrimitive.content)
        assertEquals("PASS", payload.getValue("verdict").jsonPrimitive.content)
        assertEquals("6", summary.getValue("caseCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failedAssertions").jsonPrimitive.content)
        assertTrue(aggregates.any { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus" })
        assertEquals(6, Files.readAllLines(run.casesPath).count { line -> line.isNotBlank() })
    }
}
