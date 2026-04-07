package com.ktome.tools.hidden

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class WhiteBoxHiddenContentRunnerTest {
    @Test
    @Tag("whiteBoxHiddenContent")
    fun `white-box hidden-content writes standard reports and zero failed assertions`() {
        val run = WhiteBoxHiddenContentRunner.run()

        assertEquals(500, run.caseCount)
        assertEquals(0, run.failedAssertions, "whiteBoxHiddenContent recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.casesPath), "Expected case report at ${run.casesPath}")
        assertTrue(Files.exists(run.reportPath), "Expected markdown report at ${run.reportPath}")
        assertTrue(Files.isDirectory(run.summaryPath.parent.resolve("artifacts")), "Expected artifacts directory beside ${run.summaryPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val corpus = payload.getValue("corpus").jsonObject
        val aggregates = payload.getValue("aggregates").jsonArray
        val firstCase =
            Json.parseToJsonElement(Files.readAllLines(run.casesPath).first { line -> line.isNotBlank() }).jsonObject

        assertEquals("hidden-content", payload.getValue("domainId").jsonPrimitive.content)
        assertEquals("PASS", payload.getValue("verdict").jsonPrimitive.content)
        assertEquals("P4_PR07_HIDDEN_CONTENT_WHITEBOX", corpus.getValue("corpusId").jsonPrimitive.content)
        assertEquals("500", summary.getValue("caseCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failedAssertions").jsonPrimitive.content)
        assertEquals("2000", summary.getValue("artifactCount").jsonPrimitive.content)
        assertTrue(
            aggregates.any { aggregate ->
                aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus"
            },
        )
        assertTrue(
            aggregates
                .first { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus" }
                .jsonObject
                .getValue("assertions")
                .jsonArray
                .any { assertion ->
                    assertion.jsonObject.getValue("ruleId").jsonPrimitive.content ==
                        "hidden-content.aggregate.explicit_search_reveal_present"
                },
        )
        assertEquals(4, firstCase.getValue("artifacts").jsonArray.size)
        assertTrue(
            firstCase.getValue("assertions").jsonArray.any { assertion ->
                assertion.jsonObject.getValue("ruleId").jsonPrimitive.content == "hidden-content.case.hidden_events_optional_or_secret_only"
            },
        )
        assertTrue(
            firstCase.getValue("artifacts").jsonArray.any { artifact ->
                artifact.jsonObject.getValue("artifactId").jsonPrimitive.content == "return-bridge-proof"
            },
        )
        assertEquals(500, Files.readAllLines(run.casesPath).count { line -> line.isNotBlank() })
    }
}
