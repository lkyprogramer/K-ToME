package com.ktome.tools.hidden

import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class HiddenPreflightRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("verifyHiddenPreflight")
    fun `hidden preflight locks search bindings and secret zone counts`() {
        System.setProperty("ktome.phase4.hidden.preflight.reportDir", tempDir.toString())
        val run = HiddenPreflightRunner.run()
        val summary = Json.parseToJsonElement(run.summaryPath.readText()).jsonObject

        assertTrue(run.hiddenEventCount > 0)
        assertTrue(run.secretZoneCount > 0)
        assertTrue(summary.getValue("searchBindingIds").jsonArray.all { binding -> binding.jsonPrimitive.content.startsWith("search.") })
        assertTrue(summary.getValue("secretZoneIds").jsonArray.isNotEmpty())
    }
}
