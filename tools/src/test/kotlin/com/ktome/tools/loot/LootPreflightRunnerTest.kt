package com.ktome.tools.loot

import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LootPreflightRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("verifyLootPreflight")
    fun `loot preflight writes culprit diff friendly structure`() {
        System.setProperty("ktome.phase4.loot.preflight.reportDir", tempDir.toString())
        val run = LootPreflightRunner.run()
        val json = Json.parseToJsonElement(run.detailsPath.readText()).jsonArray

        assertTrue(run.profileCount > 0)
        assertTrue(run.pairCount > 0)
        assertTrue(json.isNotEmpty())
        val firstPair = json.first().jsonObject
        assertTrue(firstPair.containsKey("sharedBaseIds"))
        assertTrue(firstPair.containsKey("leftOnlyBaseIds"))
        assertTrue(firstPair.containsKey("rightOnlyBaseIds"))
        val sourceBreakdown = firstPair.getValue("explicitVsTagMatched").jsonObject
        assertTrue(sourceBreakdown.containsKey("leftExplicitOnlyBaseIds"))
        assertTrue(sourceBreakdown.containsKey("rightTagMatchedOnlyBaseIds"))
        // Ensure culprit reasons stay serialized as stable strings for quick diff review.
        firstPair.getValue("culpritReasons").jsonArray.forEach { culpritReason ->
            culpritReason.jsonPrimitive.content
        }
    }
}
