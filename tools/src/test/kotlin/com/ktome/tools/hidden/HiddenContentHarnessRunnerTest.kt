package com.ktome.tools.hidden

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class HiddenContentHarnessRunnerTest {
    @Test
    @Tag("hiddenContentHarness")
    fun `hidden-content harness writes fixed reports and preserves join-key facts`() {
        val run = HiddenContentHarnessRunner.run()

        assertEquals(625, run.totalCases)
        assertEquals(0, run.failureCount, "hiddenContentHarness recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.eventsPath), "Expected event report at ${run.eventsPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val zones = payload.getValue("zones").jsonObject
        val firstEvent =
            Json.parseToJsonElement(Files.readAllLines(run.eventsPath).first { line -> line.isNotBlank() }).jsonObject
        val restoredKernelRun = HiddenContentHarnessRunner.loadKernelRun(run.summaryPath.parent)

        assertEquals("625", summary.getValue("totalCases").jsonPrimitive.content)
        assertEquals("625", summary.getValue("distinctSeedCount").jsonPrimitive.content)
        assertEquals("true", summary.getValue("scriptedVerification").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("caseFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("aggregateFailureCount").jsonPrimitive.content)
        assertEquals("true", summary.getValue("greenwoodFringeSearchDrivenPathPresent").jsonPrimitive.content)
        assertEquals("14", summary.getValue("hiddenEventRegistryCount").jsonPrimitive.content)
        assertEquals("6", summary.getValue("secretZoneRegistryCount").jsonPrimitive.content)
        assertTrue(summary.getValue("hiddenEventTriggerCount").jsonPrimitive.content.toInt() > 0)
        assertTrue(summary.getValue("hiddenEventTriggerRate").jsonPrimitive.content.toDouble() >= MIN_HIDDEN_EVENT_TRIGGER_RATE)
        assertTrue(summary.getValue("secretZoneDiscoveryCount").jsonPrimitive.content.toInt() > 0)
        assertTrue(summary.getValue("secretZoneDiscoveryRate").jsonPrimitive.content.toDouble() >= MIN_SECRET_ZONE_DISCOVERY_RATE)
        assertTrue(summary.getValue("explicitSearchRevealCount").jsonPrimitive.content.toInt() > 0)
        assertTrue(summary.getValue("primerActionUsedCount").jsonPrimitive.content.toInt() > 0)
        assertTrue(summary.getValue("primerFreeCaseCount").jsonPrimitive.content.toInt() > 0)
        assertEquals(
            summary.getValue("secretZoneDiscoveryCount").jsonPrimitive.content,
            summary.getValue("frontstageSecretCueExpectedCount").jsonPrimitive.content,
        )
        assertEquals("1.0", summary.getValue("frontstageCueExpiryParity").jsonPrimitive.content)
        assertEquals("7", summary.getValue("frontstageCueExpiryProbePassedCount").jsonPrimitive.content)
        assertEquals("7", summary.getValue("frontstageCueExpiryProbeTotalCount").jsonPrimitive.content)
        assertEquals(
            setOf("CRITICAL_COMBAT", "HIGH_COMBAT", "HIGH_HIDDEN", "CRITICAL", "HIGH", "MEDIUM", "LOW"),
            summary.getValue("frontstageCueExpiryProbePriorities").jsonArray.map { priority -> priority.jsonPrimitive.content }.toSet(),
        )
        assertTrue(summary.getValue("frontstageSearchCueVisibilityRate").jsonPrimitive.content.toDouble() >= 0.9)
        assertEquals("1.0", summary.getValue("zoneSearchPromptVisibility").jsonPrimitive.content)
        assertEquals("4", summary.getValue("zoneSearchPromptVisibleZoneCount").jsonPrimitive.content)
        assertEquals("4", summary.getValue("zoneSearchPromptExpectedZoneCount").jsonPrimitive.content)
        assertTrue(summary.getValue("zoneSearchPromptMissingZoneIds").jsonArray.isEmpty())
        assertEquals("1.0", summary.getValue("zoneHookCoverage").jsonPrimitive.content)
        assertEquals("5", summary.getValue("zoneHookTriggeredCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("zeroHiddenEventZoneCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("zeroSecretZoneZoneCount").jsonPrimitive.content)
        assertEquals(setOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple"), zones.keys)
        assertTrue(firstEvent.getValue("searchBindingId").jsonPrimitive.content.startsWith("search."))
        assertTrue(firstEvent.getValue("primerActionId").jsonPrimitive.content.isNotBlank())
        assertTrue(firstEvent.containsKey("primerActionUsed"))
        assertTrue(firstEvent.getValue("entranceBindingId").jsonPrimitive.content.startsWith("hidden."))
        assertTrue(firstEvent.containsKey("resolvedReturnBridgeNodeId"))
        assertTrue(firstEvent.containsKey("triggerType"))
        assertTrue(firstEvent.containsKey("explicitSearchReveal"))
        assertTrue(firstEvent.containsKey("searchPromptVisibleBeforeSearch"))
        assertTrue(firstEvent.containsKey("rewardBudgetSources"))
        assertTrue(firstEvent.containsKey("caseFailureReasons"))
        assertTrue(firstEvent.containsKey("frontstageActionCueTypes"))
        assertNotNull(payload["aggregateFailures"]?.jsonArray)
        assertNotNull(restoredKernelRun)
        assertEquals(625, restoredKernelRun?.results?.size)
        assertEquals(625, restoredKernelRun?.header?.seedList?.size)
        assertEquals(625, Files.readAllLines(run.eventsPath).count { line -> line.isNotBlank() })

        val malformedDir = run.summaryPath.parent.resolve("malformed-frontstage-evidence")
        Files.createDirectories(malformedDir)
        Files.copy(run.summaryPath, malformedDir.resolve("hidden-content-summary.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        val malformedEvent =
            buildJsonObject {
                firstEvent.forEach { (key, value) ->
                    when (key) {
                        "frontstageActionCueCategories" -> put(key, buildJsonArray { add(JsonPrimitive("SEARCH")) })
                        "frontstageActionCuePriorities" -> put(key, buildJsonArray { add(JsonPrimitive("MEDIUM")) })
                        "frontstageActionCueStableKeys" -> put(key, buildJsonArray { add(JsonPrimitive("search:no_target")) })
                        "frontstageActionCueMessageKeys" -> put(key, buildJsonArray { })
                        "frontstageActionCueTypes" -> put(key, buildJsonArray { add(JsonPrimitive("GENERIC")) })
                        else -> put(key, value)
                    }
                }
            }
        Files.writeString(malformedDir.resolve("hidden-content-events.jsonl"), malformedEvent.toString() + "\n")
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                HiddenContentHarnessRunner.loadKernelRun(malformedDir)
            }
        assertTrue(error.message.orEmpty().contains("Frontstage action cue evidence columns"))
    }

    @Test
    fun `frontstage secret visibility denominator only counts entered secret zones`() {
        val outputDir = Files.createTempDirectory("frontstage-secret-visibility-rate")
        val revealOnly =
            hiddenContentCaseResult(
                secretZoneEntered = false,
                logKeys = listOf("log.hidden.secret_zone.revealed"),
                frontstageActionCues =
                    listOf(
                        HiddenFrontstageActionCueEvidence(
                            category = "SECRET",
                            priority = "CRITICAL",
                            stableKey = "secret:reveal:greenwood_hidden_cache",
                            messageKey = "log.hidden.secret_zone.revealed",
                        ),
                    ),
            )
        val enteredSecret =
            hiddenContentCaseResult(
                secretZoneEntered = true,
                logKeys = listOf("log.hidden.secret_zone.enter"),
                frontstageActionCues =
                    listOf(
                        HiddenFrontstageActionCueEvidence(
                            category = "SECRET",
                            priority = "CRITICAL",
                            stableKey = "secret:enter:greenwood_hidden_cache",
                            messageKey = "log.hidden.secret_zone.enter",
                        ),
                    ),
            )

        val metrics = frontstageCueContractMetrics(results = listOf(revealOnly, enteredSecret), outputDir = outputDir)

        assertEquals(1, metrics.secretCueExpectedCount)
        assertEquals(1, metrics.secretCueVisibleCount)
        assertEquals(1.0, metrics.secretCueVisibilityRate)
    }

    private fun hiddenContentCaseResult(
        secretZoneEntered: Boolean,
        logKeys: List<String>,
        frontstageActionCues: List<HiddenFrontstageActionCueEvidence>,
    ): HiddenContentCaseResult =
        HiddenContentCaseResult(
            zoneId = "greenwood_fringe",
            floorIndex = 1,
            seed = 20260416L,
            searchBindingId = "search.greenwood.hidden_cache",
            primerActionId = "primer.greenwood.hidden_cache",
            primerActionUsed = true,
            entranceBindingId = "hidden.greenwood.hidden_cache",
            resolvedReturnBridgeNodeId = "main.0",
            searchActionResult = "REVEALED",
            explicitSearchReveal = true,
            searchPromptVisibleBeforeSearch = false,
            triggerType = "SEARCH_ACTION",
            hiddenEventIds = emptyList(),
            triggerTypes = emptyList(),
            triggerPathClasses = emptyList(),
            optionalOnlyTriggerPathClasses = emptyList(),
            secretZoneId = "greenwood_hidden_cache",
            secretZoneEntered = secretZoneEntered,
            secretRewardNodePresent = secretZoneEntered,
            criticalPathReachable = true,
            searchFailureKeepsMainlineReachable = true,
            returnedToMainline = true,
            returnedRoomNodeId = "main.0",
            returnedPoint = "0,0",
            expectedReturnPoint = "0,0",
            returnBridgeMatchesResolvedNodeId = true,
            proofSearchActionResult = "REVEALED",
            solvabilityProofMatchesSearchAction = true,
            solvabilityProofCoversReturnBridge = true,
            rewardSources = emptyList(),
            rewardBudgetSources = emptyList(),
            expectedRewardBudgetSources = emptyList(),
            threatBudgetSources = emptyList(),
            expectedThreatBudgetSources = emptyList(),
            logKeys = logKeys,
            frontstageActionCues = frontstageActionCues,
        )
}
