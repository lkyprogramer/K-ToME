package com.ktome.tools.mapgen

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
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
    fun `white-box solvability pilot writes standard reports for the OPT PR-05 corpus`() {
        val run = WhiteBoxSolvabilityRunner.run()

        assertEquals(46, run.totalCases)
        assertEquals(0, run.failedAssertions, "whiteBoxSolvability recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.casesPath), "Expected case report at ${run.casesPath}")
        assertTrue(Files.exists(run.reportPath), "Expected markdown report at ${run.reportPath}")
        assertTrue(Files.isDirectory(run.summaryPath.parent.resolve("artifacts")), "Expected artifacts directory beside ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath.parent.resolve(WhiteBoxSolvabilitySuccessLane.LANE_ID).resolve("whitebox-solvability-summary.json")))
        assertTrue(Files.exists(run.summaryPath.parent.resolve(WhiteBoxSolvabilityFailLane.LANE_ID).resolve("whitebox-solvability-summary.json")))

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val corpus = payload.getValue("corpus").jsonObject
        val aggregates = payload.getValue("aggregates").jsonArray

        assertEquals("solvability", payload.getValue("domainId").jsonPrimitive.content)
        assertEquals("PASS", payload.getValue("verdict").jsonPrimitive.content)
        assertEquals("P4_OPT_PR05_SOLVABILITY_WHITEBOX", corpus.getValue("corpusId").jsonPrimitive.content)
        assertEquals("46", summary.getValue("caseCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failedAssertions").jsonPrimitive.content)
        val successAggregate =
            aggregates.first { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "${WhiteBoxSolvabilitySuccessLane.LANE_ID}:corpus" }.jsonObject
        val failAggregate =
            aggregates.first { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "${WhiteBoxSolvabilityFailLane.LANE_ID}:corpus" }.jsonObject
        val successMetrics = successAggregate.getValue("metrics").jsonObject
        val failMetrics = failAggregate.getValue("metrics").jsonObject
        assertTrue(
            successAggregate.getValue("assertions").jsonArray.any { assertion ->
                assertion.jsonObject.getValue("ruleId").jsonPrimitive.content == "solvability.aggregate.reveal_success.corpus_backtrack_coverage"
            },
        )
        assertTrue(
            failAggregate.getValue("assertions").jsonArray.any { assertion ->
                assertion.jsonObject.getValue("ruleId").jsonPrimitive.content == "solvability.aggregate.reveal_fail.corpus_fail_coverage"
            },
        )
        assertTrue(
            successAggregate.getValue("assertions").jsonArray.any { assertion ->
                assertion.jsonObject.getValue("ruleId").jsonPrimitive.content ==
                    "solvability.aggregate.reveal_success.corpus_hidden_anchor_families_resolved"
            },
        )
        val firstCase = Json.parseToJsonElement(Files.readAllLines(run.casesPath).first { line -> line.isNotBlank() }).jsonObject
        assertTrue(successMetrics.containsKey("providedDiscoveryTags"))
        assertTrue(successMetrics.containsKey("requiredHiddenAnchorFamilies"))
        assertTrue(successMetrics.containsKey("observedHiddenAnchorFamilies"))
        assertEquals("40", successAggregate.getValue("sampleCount").jsonPrimitive.content)
        assertEquals("6", failAggregate.getValue("sampleCount").jsonPrimitive.content)
        assertTrue(failMetrics.containsKey("casesWithFail"))
        assertTrue(failMetrics.containsKey("failStateTaxonomy"))
        assertEquals("6", failMetrics.getValue("casesWithFail").jsonPrimitive.content)
        assertEquals("0", failMetrics.getValue("casesWithReveal").jsonPrimitive.content)
        val revealFailZoneFloors =
            Files.readAllLines(run.casesPath)
                .asSequence()
                .filter { line -> line.isNotBlank() }
                .map { line -> Json.parseToJsonElement(line).jsonObject }
                .filter { casePayload -> casePayload.getValue("facts").jsonObject.getValue("laneId").jsonPrimitive.content == WhiteBoxSolvabilityFailLane.LANE_ID }
                .map { casePayload ->
                    casePayload.getValue("facts").jsonObject.let { facts ->
                        facts.getValue("zoneId").jsonPrimitive.content to facts.getValue("floorIndex").jsonPrimitive.int
                    }
                }.toSet()
        assertEquals(
            WhiteBoxSolvabilityFailLane.fixtureZoneFloors().map { fixture -> fixture.zoneId to fixture.floorIndex }.toSet(),
            revealFailZoneFloors,
        )
        assertTrue(firstCase.getValue("facts").jsonObject.containsKey("providedDiscoveryTags"))
        assertTrue(firstCase.getValue("facts").jsonObject.containsKey("laneId"))
        assertTrue(firstCase.getValue("facts").jsonObject.containsKey("hiddenAnchorFamiliesSatisfied"))
        assertTrue(
            firstCase.getValue("assertions").jsonArray.any { assertion ->
                assertion.jsonObject.getValue("ruleId").jsonPrimitive.content ==
                    "solvability.case.hidden_anchor_families_resolved"
            },
        )
        assertTrue(
            firstCase.getValue("assertions").jsonArray.none { assertion ->
                assertion.jsonObject.getValue("ruleId").jsonPrimitive.content == "solvability.case.backtrack_satisfied"
            },
        )
        assertEquals(46, Files.readAllLines(run.casesPath).count { line -> line.isNotBlank() })
    }
}
