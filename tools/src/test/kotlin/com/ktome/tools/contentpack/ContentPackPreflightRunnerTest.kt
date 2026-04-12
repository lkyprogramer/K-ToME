package com.ktome.tools.contentpack

import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ContentPackPreflightRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("verifyContentPackPreflight")
    fun `content pack preflight keeps static resolver and loader diagnostics structured`() {
        System.setProperty("ktome.phase4.contentPack.preflight.reportDir", tempDir.toString())
        val run = ContentPackPreflightRunner.run()
        val cases =
            Json.parseToJsonElement(run.summaryPath.readText())
                .jsonObject
                .getValue("cases")
                .jsonArray
                .map { element -> element.jsonObject }
        val officialSamplePack = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "official_sample_pack" }
        val namespaceCollision = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "namespace_collision" }

        assertEquals(7, run.caseCount)
        assertEquals(0, run.failureCount)
        assertTrue(officialSamplePack.getValue("resolvedOrder").jsonArray.isNotEmpty())
        assertTrue(
            namespaceCollision.getValue("diagnosticCodes").jsonArray.any { diagnostic ->
                diagnostic.jsonPrimitive.content == "content-pack.namespace.collision"
            },
        )
    }
}
