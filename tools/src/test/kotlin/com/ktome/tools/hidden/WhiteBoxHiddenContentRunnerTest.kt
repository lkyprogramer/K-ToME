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
        val corpusAggregate =
            aggregates.first { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus" }.jsonObject
        val aggregateRuleIds =
            corpusAggregate
                .getValue("assertions")
                .jsonArray
                .map { assertion -> assertion.jsonObject.getValue("ruleId").jsonPrimitive.content }
                .toSet()
        val caseRuleIds =
            firstCase
                .getValue("assertions")
                .jsonArray
                .map { assertion -> assertion.jsonObject.getValue("ruleId").jsonPrimitive.content }
                .toSet()
        val artifactIds =
            firstCase
                .getValue("artifacts")
                .jsonArray
                .map { artifact -> artifact.jsonObject.getValue("artifactId").jsonPrimitive.content }
                .toSet()

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
            setOf(
                "hidden-content.aggregate.explicit_search_reveal_present",
                "hidden-content.aggregate.hidden_events_optional_or_secret_only",
                "hidden-content.aggregate.secret_reward_node_present",
                "hidden-content.aggregate.reward_bridge_backed_by_loot_budget",
                "hidden-content.aggregate.search_failure_non_blocking",
                "hidden-content.aggregate.return_bridge_proof_consistency",
            ).all(aggregateRuleIds::contains),
        )
        assertTrue(
            setOf(
                "hidden-content.case.hidden_events_optional_or_secret_only",
                "hidden-content.case.secret_reward_node_present",
                "hidden-content.case.reward_bridge_backed_by_loot_budget",
                "hidden-content.case.search_failure_keeps_mainline_reachable",
                "hidden-content.case.solvability_proof_matches_search_result",
                "hidden-content.case.solvability_proof_covers_return_bridge",
            ).all(caseRuleIds::contains),
        )
        assertEquals(
            setOf("trigger-timeline", "search-action-results", "return-bridge-proof", "reward-bridge-summary"),
            artifactIds,
        )
        assertEquals(500, Files.readAllLines(run.casesPath).count { line -> line.isNotBlank() })
    }
}
