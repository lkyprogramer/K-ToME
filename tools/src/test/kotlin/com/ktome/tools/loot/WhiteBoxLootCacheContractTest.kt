package com.ktome.tools.loot

import com.ktome.tools.phase4.Phase4OwnerBaselineRegistry
import com.ktome.tools.phase4.Phase4OwnerBaselineTestSupport
import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertTimeout
import org.junit.jupiter.api.io.TempDir

@Tag("lootVerificationCacheContract")
class WhiteBoxLootCacheContractTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `white-box loot rejects missing kernel when fallback is disabled`() {
        val config =
            WhiteBoxLootRunConfig(
                repoRoot = tempDir.resolve("repo"),
                lootReportDir = tempDir.resolve("missing-loot-reports"),
                outputDir = tempDir.resolve("whitebox-loot"),
                preflightSummaryPath = tempDir.resolve("loot-preflight").resolve(LootPreflightRunner.SUMMARY_FILE_NAME),
                baselinePath = tempDir.resolve("phase4-loot-baseline.json"),
                allowKernelFallback = false,
            )

        val error = assertThrows<IllegalStateException> { WhiteBoxLootRunner.run(config) }

        assertTrue(error.message.orEmpty().contains("Missing loot kernel"))
        assertTrue(error.message.orEmpty().contains(":tools:lootBalanceLab"))
    }

    @Test
    fun `loot baseline-only rerun re-evaluates within ten seconds without rerunning loot kernel`() {
        val originalLootReportDir = System.getProperty("ktome.phase4.loot.reportDir")
        val originalWhiteBoxReportDir = System.getProperty("ktome.phase4.whitebox.loot.reportDir")
        val originalPreflightReportDir = System.getProperty("ktome.phase4.loot.preflight.reportDir")
        val originalReuse = System.getProperty("ktome.phase4.reuseHarnessOutputs")
        val originalBaselineOverride = System.getProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot")
        val originalAllowFallback = System.getProperty("ktome.phase4.whitebox.loot.allowKernelFallback")
        val repoRoot = VerificationCacheSupport.repoRoot()
        val cacheDirs = VerificationCacheSupport.cacheDirs(domainId = "loot", repoRoot = repoRoot)
        val baselineCopy = tempDir.resolve("phase4-loot-baseline.json")
        Files.copy(repoRoot.resolve(Phase4OwnerBaselineRegistry.LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH), baselineCopy)
        VerificationCacheSupport.clearDirectory(cacheDirs.kernelDir)
        VerificationCacheSupport.clearDirectory(cacheDirs.evaluationDir)
        try {
            val effectivePreflightReportDir = tempDir.resolve("loot-preflight").toString()
            val effectiveLootReportDir = tempDir.resolve("loot-reports").toString()
            val effectiveWhiteBoxReportDir = tempDir.resolve("whitebox-loot").toString()
            System.setProperty("ktome.phase4.loot.preflight.reportDir", effectivePreflightReportDir)
            System.setProperty("ktome.phase4.loot.reportDir", effectiveLootReportDir)
            System.setProperty("ktome.phase4.whitebox.loot.reportDir", effectiveWhiteBoxReportDir)
            System.setProperty("ktome.phase4.reuseHarnessOutputs", "true")
            System.setProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot", baselineCopy.toString())
            System.setProperty("ktome.phase4.whitebox.loot.allowKernelFallback", "true")

            LootPreflightRunner.run()
            val lootRun = LootBalanceLabRunner.run()
            WhiteBoxLootRunner.run()
            val lootSummaryTimestamp = Files.getLastModifiedTime(lootRun.summaryPath)

            Phase4OwnerBaselineTestSupport.stampBaselineMetadata(baselineCopy, marker = "baseline-only-rerun")

            val rerun =
                assertTimeout(Duration.ofSeconds(10)) {
                    WhiteBoxLootRunner.run()
                }
            val payload = Json.parseToJsonElement(Files.readString(rerun.summaryPath)).jsonObject
            val evaluationCache = payload.getValue("evaluationCache").jsonObject

            assertEquals("MISS", evaluationCache.getValue("cacheStatus").jsonPrimitive.content)
            assertEquals("PASS", payload.getValue("verdict").jsonPrimitive.content)
            assertEquals("PRODUCER_ARTIFACT", payload.getValue("kernelSource").jsonPrimitive.content)
            assertEquals(lootSummaryTimestamp, Files.getLastModifiedTime(lootRun.summaryPath))
        } finally {
            restoreProperty("ktome.phase4.loot.reportDir", originalLootReportDir)
            restoreProperty("ktome.phase4.whitebox.loot.reportDir", originalWhiteBoxReportDir)
            restoreProperty("ktome.phase4.loot.preflight.reportDir", originalPreflightReportDir)
            restoreProperty("ktome.phase4.reuseHarnessOutputs", originalReuse)
            restoreProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot", originalBaselineOverride)
            restoreProperty("ktome.phase4.whitebox.loot.allowKernelFallback", originalAllowFallback)
        }
    }

    private fun restoreProperty(
        key: String,
        value: String?,
    ) {
        if (value == null) {
            System.clearProperty(key)
        } else {
            System.setProperty(key, value)
        }
    }
}
