package com.ktome.tools.loot

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class LootBalanceLabRunnerTest {
    @Test
    @Tag("lootBalanceLab")
    fun `loot balance lab writes summary and per-roll reports for six fixed matrices`() {
        val run = LootBalanceLabRunner.run()

        assertEquals(6, run.matrixCount)
        assertEquals(60_000, run.totalRolls)
        assertEquals(0, run.failedExpectationCount, "lootBalanceLab recorded threshold failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.rollsPath), "Expected roll report at ${run.rollsPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val rollLines = Files.readAllLines(run.rollsPath)
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val matrices = payload.getValue("matrices").jsonArray
        val clamp = payload.getValue("magicFindClampComparison").jsonObject
        val specialTemplatePool = payload.getValue("specialTemplatePool").jsonObject
        val profileOverlapSummary = payload.getValue("profileOverlapSummary").jsonObject
        val rewardChestMatrix =
            matrices
                .map { element -> element.jsonObject }
                .first { matrix -> matrix.getValue("matrixId").jsonPrimitive.content == "abyssal_reward_chest_mf010" }
        val restoredKernelRun = LootBalanceLabRunner.readKernelRun(run.summaryPath.parent)

        assertEquals("PASS", summary.getValue("verdict").jsonPrimitive.content)
        assertEquals("6", summary.getValue("matrixCount").jsonPrimitive.content)
        assertEquals("60000", summary.getValue("totalRolls").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failedExpectationCount").jsonPrimitive.content)
        assertEquals("en-US", header.getValue("locale").jsonPrimitive.content)
        assertEquals(6, matrices.size)
        assertEquals("true", clamp.getValue("withinTolerance").jsonPrimitive.content)
        assertEquals("SPECIAL_REWARD_CHEST", rewardChestMatrix.getValue("sourceDescriptor").jsonPrimitive.content)
        assertNotEquals("0.0", rewardChestMatrix.getValue("artifactRate").jsonPrimitive.content)
        assertEquals("true", specialTemplatePool.getValue("passesThresholds").jsonPrimitive.content)
        assertTrue(
            specialTemplatePool.getValue("secretZoneArtifactTemplateCount").jsonPrimitive.content.toInt() > 0,
            "PR-05 special pool must expose secret-zone artifact coverage.",
        )
        assertTrue(
            rollLines.any { line -> "\"rawAffixBudgetShortfall\":0" !in line && "\"rawAffixBudgetShortfall\":" in line },
            "Expected at least one non-zero raw affix budget shortfall sample in ${run.rollsPath}",
        )
        assertEquals(
            setOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple"),
            profileOverlapSummary
                .getValue("sameZoneSecretVsCadencePairs")
                .jsonArray
                .map { pair -> pair.jsonObject.getValue("zoneId").jsonPrimitive.content }
                .toSet(),
        )
        assertEquals(
            setOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple"),
            profileOverlapSummary
                .getValue("sameZoneSecretVsRewardPairs")
                .jsonArray
                .map { pair -> pair.jsonObject.getValue("zoneId").jsonPrimitive.content }
                .toSet(),
        )
        assertNotNull(restoredKernelRun)
        assertEquals(6, restoredKernelRun?.matrices?.size)
        assertEquals(60_000, restoredKernelRun?.totalRolls)
        assertEquals(60_000, rollLines.count { line -> line.isNotBlank() })
    }
}
