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

class WhiteBoxMapgenHarnessTest {
    @Test
    @Tag("whiteBoxMapgen")
    fun `white-box mapgen pilot writes standard reports for the PR-03 corpus`() {
        val run = WhiteBoxMapgenRunner.run()

        assertEquals(40, run.totalCases)
        assertEquals(0, run.failedAssertions, "whiteBoxMapgen recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.casesPath), "Expected case report at ${run.casesPath}")
        assertTrue(Files.exists(run.reportPath), "Expected markdown report at ${run.reportPath}")
        assertTrue(Files.isDirectory(run.summaryPath.parent.resolve("artifacts")), "Expected artifacts directory beside ${run.summaryPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val corpus = payload.getValue("corpus").jsonObject
        val aggregates = payload.getValue("aggregates").jsonArray

        assertEquals("mapgen", payload.getValue("domainId").jsonPrimitive.content)
        assertEquals("PASS", payload.getValue("verdict").jsonPrimitive.content)
        assertEquals("P4_PR03_MAPGEN_WHITEBOX", corpus.getValue("corpusId").jsonPrimitive.content)
        assertEquals("40", summary.getValue("caseCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failedAssertions").jsonPrimitive.content)
        assertTrue(aggregates.any { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus" })
        assertTrue(
            aggregates.any { aggregate ->
                val metrics = aggregate.jsonObject.getValue("metrics").jsonObject
                "distinctEntranceLayoutCount" in metrics
            },
        )
        assertEquals(40, Files.readAllLines(run.casesPath).count { line -> line.isNotBlank() })
    }
}
