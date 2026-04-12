package com.ktome.tools.loot

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class WhiteBoxLootRunnerTest {
    @Test
    @Tag("whiteBoxLoot")
    fun `white-box loot writes standard reports and surfaces same-zone local identity guardrails`() {
        val run = WhiteBoxLootRunner.run()

        assertEquals(6, run.caseCount)
        assertEquals(2, run.failedAssertions, "whiteBoxLoot should now surface the two same-zone local identity guardrail failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.casesPath), "Expected case report at ${run.casesPath}")
        assertTrue(Files.exists(run.reportPath), "Expected markdown report at ${run.reportPath}")
        assertTrue(Files.isDirectory(run.summaryPath.parent.resolve("artifacts")), "Expected artifacts directory beside ${run.summaryPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        val aggregates = payload.getValue("aggregates").jsonArray
        val corpusAggregate =
            aggregates.first { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus" }.jsonObject
        val corpusMetrics = corpusAggregate.getValue("metrics").jsonObject
        val aggregateRuleIds =
            corpusAggregate
                .getValue("assertions")
                .jsonArray
                .map { assertion -> assertion.jsonObject.getValue("ruleId").jsonPrimitive.content }
                .toSet()

        assertEquals("loot", payload.getValue("domainId").jsonPrimitive.content)
        assertEquals("FAIL", payload.getValue("verdict").jsonPrimitive.content)
        assertEquals("6", summary.getValue("caseCount").jsonPrimitive.content)
        assertEquals("2", summary.getValue("failedAssertions").jsonPrimitive.content)
        assertTrue(aggregates.any { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus" })
        assertTrue(
            setOf(
                "lootProfileBaseItemOverlapMatrix",
                "lootProfileAverageBaseItemOverlap",
                "lootProfileMaxBaseItemOverlap",
                "lootProfileDistinctBaseItemCount",
                "sameZoneSecretVsCadenceMaxOverlap",
                "sameZoneSecretVsRewardMaxOverlap",
                "sameZoneSecretVsCadencePairs",
                "sameZoneSecretVsRewardPairs",
                "localIdentityFailurePairs",
                "affixPassiveCoverage",
                "affixPassiveKinds",
                "uniqueArtifactMeaningfulSwapRate",
            ).all(corpusMetrics::containsKey),
        )
        assertTrue(corpusMetrics.getValue("lootProfileBaseItemOverlapMatrix").jsonObject.isNotEmpty())
        assertTrue(corpusMetrics.getValue("sameZoneSecretVsCadencePairs").jsonArray.isNotEmpty())
        assertTrue(corpusMetrics.getValue("sameZoneSecretVsRewardPairs").jsonArray.isNotEmpty())
        assertTrue(corpusMetrics.getValue("affixPassiveKinds").jsonArray.isNotEmpty())
        assertTrue(corpusMetrics.getValue("lootProfileMaxBaseItemOverlap").jsonPrimitive.content.toDouble() < 0.95)
        assertTrue(
            setOf(
                "loot.aggregate.overlap_below_threshold",
                "loot.aggregate.max_overlap_sanity",
                "loot.aggregate.same_zone_secret_cadence_guardrail",
                "loot.aggregate.same_zone_secret_reward_guardrail",
                "loot.aggregate.passive_coverage",
            ).all(aggregateRuleIds::contains),
        )
        assertEquals(6, Files.readAllLines(run.casesPath).count { line -> line.isNotBlank() })
    }
}
