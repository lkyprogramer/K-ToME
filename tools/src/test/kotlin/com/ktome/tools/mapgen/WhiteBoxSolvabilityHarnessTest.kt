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

class WhiteBoxSolvabilityHarnessTest {
    @Test
    @Tag("whiteBoxSolvability")
    fun `white-box solvability pilot writes standard reports for the PR-03 corpus`() {
        val run = WhiteBoxSolvabilityRunner.run()

        assertEquals(40, run.totalCases)
        assertEquals(0, run.failedAssertions, "whiteBoxSolvability recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.casesPath), "Expected case report at ${run.casesPath}")
        assertTrue(Files.exists(run.reportPath), "Expected markdown report at ${run.reportPath}")
        assertTrue(Files.isDirectory(run.summaryPath.parent.resolve("artifacts")), "Expected artifacts directory beside ${run.summaryPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val corpus = payload.getValue("corpus").jsonObject
        val aggregates = payload.getValue("aggregates").jsonArray

        assertEquals("solvability", payload.getValue("domainId").jsonPrimitive.content)
        assertEquals("PASS", payload.getValue("verdict").jsonPrimitive.content)
        assertEquals("P4_PR03_SOLVABILITY_WHITEBOX", corpus.getValue("corpusId").jsonPrimitive.content)
        assertEquals("40", summary.getValue("caseCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failedAssertions").jsonPrimitive.content)
        val corpusAggregate =
            aggregates.first { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus" }.jsonObject
        assertTrue(
            corpusAggregate.getValue("assertions").jsonArray.any { assertion ->
                assertion.jsonObject.getValue("ruleId").jsonPrimitive.content == "solvability.aggregate.corpus_backtrack_coverage"
            },
        )
        val firstCase = Json.parseToJsonElement(Files.readAllLines(run.casesPath).first { line -> line.isNotBlank() }).jsonObject
        assertTrue(
            firstCase.getValue("assertions").jsonArray.none { assertion ->
                assertion.jsonObject.getValue("ruleId").jsonPrimitive.content == "solvability.case.backtrack_satisfied"
            },
        )
        assertEquals(40, Files.readAllLines(run.casesPath).count { line -> line.isNotBlank() })
    }
}
