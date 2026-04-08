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

class WhiteBoxContentPackRunnerTest {
    @Test
    @Tag("whiteBoxContentPack")
    fun `white-box content-pack writes standard reports and zero failed assertions`() {
        val run = WhiteBoxContentPackRunner.run()

        assertEquals(11, run.caseCount)
        assertEquals(0, run.failedAssertions, "whiteBoxContentPack recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.casesPath), "Expected cases report at ${run.casesPath}")
        assertTrue(Files.exists(run.reportPath), "Expected markdown report at ${run.reportPath}")
        assertTrue(Files.isDirectory(run.summaryPath.parent.resolve("artifacts")), "Expected artifacts directory beside ${run.summaryPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val corpus = payload.getValue("corpus").jsonObject
        val firstCase =
            Json.parseToJsonElement(Files.readAllLines(run.casesPath).first { line -> line.isNotBlank() }).jsonObject
        val artifactIds =
            firstCase.getValue("artifacts").jsonArray.map { artifact -> artifact.jsonObject.getValue("artifactId").jsonPrimitive.content }.toSet()

        assertEquals("whiteBoxContentPack", payload.getValue("domainId").jsonPrimitive.content)
        assertEquals("PASS", payload.getValue("verdict").jsonPrimitive.content)
        assertEquals("P4_PR09_SAMPLE_CONTENT_PACK_WHITEBOX", corpus.getValue("corpusId").jsonPrimitive.content)
        assertEquals("11", summary.getValue("caseCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failedAssertions").jsonPrimitive.content)
        assertEquals(
            setOf(
                "pack-manifest-resolve",
                "merged-key-summary",
                "headless-run-summary",
                "precedence-matrix",
                "lint-diagnostics",
            ),
            artifactIds,
        )
    }
}
