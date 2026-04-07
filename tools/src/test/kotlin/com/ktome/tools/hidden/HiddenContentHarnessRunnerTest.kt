package com.ktome.tools.hidden

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class HiddenContentHarnessRunnerTest {
    @Test
    @Tag("hiddenContentHarness")
    fun `hidden-content harness writes fixed reports and preserves join-key facts`() {
        val run = HiddenContentHarnessRunner.run()

        assertEquals(500, run.totalCases)
        assertEquals(0, run.failureCount, "hiddenContentHarness recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.eventsPath), "Expected event report at ${run.eventsPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val zones = payload.getValue("zones").jsonObject
        val firstEvent =
            Json.parseToJsonElement(Files.readAllLines(run.eventsPath).first { line -> line.isNotBlank() }).jsonObject

        assertEquals("500", summary.getValue("totalCases").jsonPrimitive.content)
        assertEquals("500", summary.getValue("distinctSeedCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("caseFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("aggregateFailureCount").jsonPrimitive.content)
        assertTrue(summary.getValue("hiddenEventTriggerCount").jsonPrimitive.content.toInt() > 0)
        assertTrue(summary.getValue("hiddenEventTriggerRate").jsonPrimitive.content.toDouble() >= MIN_HIDDEN_EVENT_TRIGGER_RATE)
        assertTrue(summary.getValue("secretZoneDiscoveryCount").jsonPrimitive.content.toInt() > 0)
        assertTrue(summary.getValue("secretZoneDiscoveryRate").jsonPrimitive.content.toDouble() >= MIN_SECRET_ZONE_DISCOVERY_RATE)
        assertTrue(summary.getValue("explicitSearchRevealCount").jsonPrimitive.content.toInt() > 0)
        assertEquals("0", summary.getValue("zeroHiddenEventZoneCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("zeroSecretZoneZoneCount").jsonPrimitive.content)
        assertEquals(setOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple"), zones.keys)
        assertTrue(firstEvent.getValue("searchBindingId").jsonPrimitive.content.startsWith("search."))
        assertTrue(firstEvent.getValue("entranceBindingId").jsonPrimitive.content.startsWith("optional."))
        assertTrue(firstEvent.containsKey("resolvedReturnBridgeNodeId"))
        assertTrue(firstEvent.containsKey("triggerType"))
        assertTrue(firstEvent.containsKey("rewardBudgetSources"))
        assertTrue(firstEvent.containsKey("caseFailureReasons"))
        assertNotNull(payload["aggregateFailures"]?.jsonArray)
        assertEquals(500, Files.readAllLines(run.eventsPath).count { line -> line.isNotBlank() })
    }
}
