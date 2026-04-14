package com.ktome.tools.loot

import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LootBalanceLabRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("lootBalanceLab")
    fun `loot balance lab writes summary and per-roll reports for six fixed matrices`() {
        val originalReportDir = System.getProperty("ktome.phase4.loot.reportDir")
        val repoRoot = VerificationCacheSupport.repoRoot()
        val cacheDirs = VerificationCacheSupport.cacheDirs(domainId = "loot", repoRoot = repoRoot)
        val isolatedTestRun = originalReportDir == null
        if (isolatedTestRun) {
            VerificationCacheSupport.clearDirectory(cacheDirs.kernelDir)
        }
        try {
            val effectiveReportDir = originalReportDir ?: tempDir.resolve("loot-reports").toString()
            System.setProperty("ktome.phase4.loot.reportDir", effectiveReportDir)
            val coldRun = LootBalanceLabRunner.run()
            val coldPayload = Json.parseToJsonElement(Files.readString(coldRun.summaryPath)).jsonObject
            val warmRun = LootBalanceLabRunner.run()

            assertEquals(6, warmRun.matrixCount)
            assertEquals(60_000, warmRun.totalRolls)
            assertEquals(0, warmRun.failedExpectationCount, "lootBalanceLab recorded threshold failures; inspect ${warmRun.summaryPath}")
            assertTrue(Files.exists(warmRun.summaryPath), "Expected summary report at ${warmRun.summaryPath}")
            assertTrue(Files.exists(warmRun.rollsPath), "Expected roll report at ${warmRun.rollsPath}")

            val payload = Json.parseToJsonElement(Files.readString(warmRun.summaryPath)).jsonObject
            val rollLines = Files.readAllLines(warmRun.rollsPath)
            val header = payload.getValue("header").jsonObject
            val summary = payload.getValue("summary").jsonObject
            val matrices = payload.getValue("matrices").jsonArray
            val clamp = payload.getValue("magicFindClampComparison").jsonObject
            val specialTemplatePool = payload.getValue("specialTemplatePool").jsonObject
            val profileOverlapSummary = payload.getValue("profileOverlapSummary").jsonObject
            val coldKernelCache = coldPayload.getValue("kernelCache").jsonObject
            val warmKernelCache = payload.getValue("kernelCache").jsonObject
            val shardRollPaths =
                warmKernelCache
                    .getValue("shardRollPaths")
                    .jsonArray
                    .map { element -> repoRoot.resolve(element.jsonPrimitive.content) }
            val rewardChestMatrix =
                matrices
                    .map { element -> element.jsonObject }
                    .first { matrix -> matrix.getValue("matrixId").jsonPrimitive.content == "abyssal_reward_chest_mf010" }
            val restoredKernelRun = LootBalanceLabRunner.readKernelRun(warmRun.summaryPath.parent)

            if (isolatedTestRun) {
                assertEquals("MISS", coldKernelCache.getValue("cacheStatus").jsonPrimitive.content)
            }
            assertEquals("HIT", warmKernelCache.getValue("cacheStatus").jsonPrimitive.content)
            assertEquals("60", warmKernelCache.getValue("shardCount").jsonPrimitive.content)
            assertEquals(60, shardRollPaths.size)
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
                "Expected at least one non-zero raw affix budget shortfall sample in ${warmRun.rollsPath}",
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
            assertTrue(shardRollPaths.all(Files::isRegularFile))
            assertTrue(shardRollPaths.all { path -> Files.readAllLines(path).count(String::isNotBlank) == 1_000 })
            assertEquals(60_000, shardRollPaths.sumOf { path -> Files.readAllLines(path).count(String::isNotBlank) })

            val shardPayloads =
                shardRollPaths
                    .map { shardRollPath ->
                        Json.parseToJsonElement(Files.readString(shardRollPath.parent.resolve("kernel.json"))).jsonObject
                    }.groupBy { payload -> payload.getValue("matrixId").jsonPrimitive.content }
            shardPayloads.values.forEach { payloads ->
                val orderedPayloads = payloads.sortedBy { payload -> payload.getValue("rollStartInclusive").jsonPrimitive.int }
                assertEquals("0", orderedPayloads.first().getValue("startingPityTracker").jsonObject.getValue("rollsSinceLastRare").jsonPrimitive.content)
                assertEquals(
                    "0",
                    orderedPayloads.first().getValue("startingPityTracker").jsonObject.getValue("eligibleSpecialRollsSinceLastUnique").jsonPrimitive.content,
                )
                orderedPayloads.zipWithNext { previous, current ->
                    assertEquals(
                        previous.getValue("resultingPityTracker").jsonObject,
                        current.getValue("startingPityTracker").jsonObject,
                        "Adjacent loot shards must preserve pity tracker continuity across shard boundaries.",
                    )
                }
            }
        } finally {
            if (originalReportDir == null) {
                System.clearProperty("ktome.phase4.loot.reportDir")
            } else {
                System.setProperty("ktome.phase4.loot.reportDir", originalReportDir)
            }
        }
    }
}
