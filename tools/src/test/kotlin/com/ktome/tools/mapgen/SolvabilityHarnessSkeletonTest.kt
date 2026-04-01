package com.ktome.tools.mapgen

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class SolvabilityHarnessSkeletonTest {
    @Test
    @Tag("solvabilityHarness")
    fun `solvability harness alias is frozen pending pr03`() {
        val run = SolvabilityHarnessSkeleton.run()

        assertEquals("PENDING_PR03", run.status)
        assertTrue(Files.exists(run.reportPath), "Expected solvability harness skeleton report at ${run.reportPath}")
        val payload = Json.parseToJsonElement(Files.readString(run.reportPath)).jsonObject
        assertEquals("zh-CN", payload.getValue("header").jsonObject.getValue("locale").jsonPrimitive.content)
    }
}
