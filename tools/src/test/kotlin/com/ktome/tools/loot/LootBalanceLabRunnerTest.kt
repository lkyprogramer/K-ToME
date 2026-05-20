package com.ktome.tools.loot

import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
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
            val firstRun = LootBalanceLabRunner.run()
            val firstPayload = Json.parseToJsonElement(Files.readString(firstRun.summaryPath)).jsonObject
            val (run, payload) =
                if (isolatedTestRun) {
                    val warmRun = LootBalanceLabRunner.run()
                    val warmPayload = Json.parseToJsonElement(Files.readString(warmRun.summaryPath)).jsonObject
                    val coldKernelCache = firstPayload.getValue("kernelCache").jsonObject
                    val warmKernelCache = warmPayload.getValue("kernelCache").jsonObject

                    assertEquals("MISS", coldKernelCache.getValue("cacheStatus").jsonPrimitive.content)
                    assertEquals("HIT", warmKernelCache.getValue("cacheStatus").jsonPrimitive.content)
                    warmRun to warmPayload
                } else {
                    val kernelCache = firstPayload.getValue("kernelCache").jsonObject
                    val cacheStatus = kernelCache.getValue("cacheStatus").jsonPrimitive.content
                    assertTrue(
                        cacheStatus == "MISS" || cacheStatus == "HIT",
                        "Expected loot kernel cache status to be MISS or HIT, but was $cacheStatus.",
                    )
                    firstRun to firstPayload
                }

            assertEquals(6, run.matrixCount)
            assertEquals(60_000, run.totalRolls)
            assertEquals(0, run.failedExpectationCount, "lootBalanceLab recorded threshold failures; inspect ${run.summaryPath}")
            assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
            assertTrue(Files.exists(run.rollsPath), "Expected roll report at ${run.rollsPath}")

            val rollLineStats = lootRollLineStats(run.rollsPath)
            val header = payload.getValue("header").jsonObject
            val summary = payload.getValue("summary").jsonObject
            val matrices = payload.getValue("matrices").jsonArray
            val clamp = payload.getValue("magicFindClampComparison").jsonObject
            val specialTemplatePool = payload.getValue("specialTemplatePool").jsonObject
            val profileOverlapSummary = payload.getValue("profileOverlapSummary").jsonObject
            val kernelCache = payload.getValue("kernelCache").jsonObject
            val shardRollPaths =
                kernelCache
                    .getValue("shardRollPaths")
                    .jsonArray
                    .map { element -> repoRoot.resolve(element.jsonPrimitive.content) }
            val rewardChestMatrix =
                matrices
                    .map { element -> element.jsonObject }
                    .first { matrix -> matrix.getValue("matrixId").jsonPrimitive.content == "abyssal_reward_chest_mf010" }
            val restoredKernelRun = LootBalanceLabRunner.readKernelRun(run.summaryPath.parent)

            assertEquals("60", kernelCache.getValue("shardCount").jsonPrimitive.content)
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
                rollLineStats.hasNonZeroBudgetShortfall,
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
            assertEquals(60_000, rollLineStats.nonBlankCount)
            assertTrue(shardRollPaths.all(Files::isRegularFile))
            val shardRollLineCounts = shardRollPaths.associateWith(::countNonBlankLines)
            assertTrue(shardRollLineCounts.values.all { count -> count == 1_000 })
            assertEquals(60_000, shardRollLineCounts.values.sum())

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

    @Test
    @Tag("lootBalanceLab")
    fun `readKernelRun ignores cached payloads with stale contract version`() {
        val repoRoot = tempDir.resolve("stale-contract-repo")
        val reportDir = tempDir.resolve("loot-reports-stale-contract")
        val summaryPath = reportDir.resolve("loot-balance-summary.json")
        val mergedKernelPath =
            VerificationCacheSupport.cacheDirs(domainId = "loot", repoRoot = repoRoot)
                .kernelDir
                .resolve("merged")
                .resolve("loot-kernel-merged.json")

        writeStaleKernelPayload(summaryPath)
        writeStaleKernelPayload(mergedKernelPath)

        assertEquals(null, LootBalanceLabRunner.readKernelRun(reportDir = reportDir, repoRoot = repoRoot))
    }

    private fun writeStaleKernelPayload(path: Path) {
        VerificationCacheSupport.writeJson(
            path,
            buildJsonObject {
                putJsonObject("kernelCache") {
                    put("contractVersion", "uvr-pr05-loot-kernel-v5")
                }
            },
        )
    }

    private data class LootRollLineStats(
        val nonBlankCount: Int,
        val hasNonZeroBudgetShortfall: Boolean,
    )

    private fun lootRollLineStats(path: Path): LootRollLineStats {
        var nonBlankCount = 0
        var hasNonZeroBudgetShortfall = false
        Files.newBufferedReader(path).useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    nonBlankCount += 1
                    if ("\"rawAffixBudgetShortfall\":0" !in line && "\"rawAffixBudgetShortfall\":" in line) {
                        hasNonZeroBudgetShortfall = true
                    }
                }
            }
        }
        return LootRollLineStats(
            nonBlankCount = nonBlankCount,
            hasNonZeroBudgetShortfall = hasNonZeroBudgetShortfall,
        )
    }

    private fun countNonBlankLines(path: Path): Int =
        Files.newBufferedReader(path).useLines { lines -> lines.count(String::isNotBlank) }
}
