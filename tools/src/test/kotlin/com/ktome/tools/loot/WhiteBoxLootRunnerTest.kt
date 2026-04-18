package com.ktome.tools.loot

import com.ktome.tools.phase4.Phase4OwnerBaselineRegistry
import com.ktome.tools.phase4.Phase4OwnerBaselineTestSupport
import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.junit.jupiter.api.io.TempDir

@Tag("whiteBoxLoot")
class WhiteBoxLootRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `white-box loot writes standard reports and keeps same-zone local identity guardrails green`() {
        val originalLootReportDir = System.getProperty("ktome.phase4.loot.reportDir")
        val originalWhiteBoxReportDir = System.getProperty("ktome.phase4.whitebox.loot.reportDir")
        val originalPreflightReportDir = System.getProperty("ktome.phase4.loot.preflight.reportDir")
        val originalReuse = System.getProperty("ktome.phase4.reuseHarnessOutputs")
        val repoRoot = VerificationCacheSupport.repoRoot()
        val cacheDirs = VerificationCacheSupport.cacheDirs(domainId = "loot", repoRoot = repoRoot)
        val isolatedTestRun = originalLootReportDir == null && originalWhiteBoxReportDir == null && originalPreflightReportDir == null
        if (isolatedTestRun) {
            VerificationCacheSupport.clearDirectory(cacheDirs.kernelDir)
            VerificationCacheSupport.clearDirectory(cacheDirs.evaluationDir)
        }
        try {
            val effectivePreflightReportDir = originalPreflightReportDir ?: tempDir.resolve("loot-preflight").toString()
            val effectiveLootReportDir = originalLootReportDir ?: tempDir.resolve("loot-reports").toString()
            val effectiveWhiteBoxReportDir = originalWhiteBoxReportDir ?: tempDir.resolve("whitebox-loot").toString()
            System.setProperty("ktome.phase4.loot.preflight.reportDir", effectivePreflightReportDir)
            System.setProperty("ktome.phase4.loot.reportDir", effectiveLootReportDir)
            System.setProperty("ktome.phase4.whitebox.loot.reportDir", effectiveWhiteBoxReportDir)
            System.setProperty("ktome.phase4.reuseHarnessOutputs", "true")
            LootPreflightRunner.run()
            val coldRun = WhiteBoxLootRunner.run()
            val coldPayload = Json.parseToJsonElement(Files.readString(coldRun.summaryPath)).jsonObject
            val run = WhiteBoxLootRunner.run()

            assertEquals(6, run.caseCount)
            assertEquals(0, run.failedAssertions, "whiteBoxLoot should keep same-zone local identity guardrails green; inspect ${run.summaryPath}")
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
            val coldEvaluationCache = coldPayload.getValue("evaluationCache").jsonObject
            val evaluationCache = payload.getValue("evaluationCache").jsonObject
            val ownerEvaluation = payload.getValue("ownerEvaluation").jsonObject

            if (isolatedTestRun) {
                assertEquals("MISS", coldEvaluationCache.getValue("cacheStatus").jsonPrimitive.content)
            }
            assertEquals("HIT", evaluationCache.getValue("cacheStatus").jsonPrimitive.content)
            assertEquals("loot", payload.getValue("domainId").jsonPrimitive.content)
            assertEquals("PASS", payload.getValue("verdict").jsonPrimitive.content)
            assertEquals("6", summary.getValue("caseCount").jsonPrimitive.content)
            assertEquals("0", summary.getValue("failedAssertions").jsonPrimitive.content)
            assertEquals("loot.localRewardIdentity", ownerEvaluation.getValue("evaluationId").jsonPrimitive.content)
            assertTrue(aggregates.any { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus" })
            assertTrue(
                setOf(
                    "lootProfileBaseItemOverlapMatrix",
                    "lootProfileAverageBaseItemOverlap",
                    "lootProfileMaxBaseItemOverlap",
                    "lootProfileDistinctBaseItemCount",
                    "dynamicPoolCoverage",
                    "dynamicPoolTargetProfiles",
                    "sameZoneSecretVsCadenceMaxOverlap",
                    "sameZoneSecretVsRewardMaxOverlap",
                    "sameZoneSecretVsCadencePairs",
                    "sameZoneSecretVsRewardPairs",
                    "localIdentityFailurePairs",
                    "strictLocalIdentityViolationCount",
                    "strictLocalIdentityViolations",
                    "secretProfileIdentitySummaries",
                    "preflightCulpritPairCount",
                    "preflightCulpritReasons",
                    "preflightCulpritPairs",
                    "affixPassiveCoverage",
                    "affixPassiveKinds",
                    "uniqueArtifactMeaningfulSwapRate",
                    "specialTierPassiveFamilyDuplicateSummary",
                ).all(corpusMetrics::containsKey),
            )
            assertEquals(1.0, corpusMetrics.getValue("dynamicPoolCoverage").jsonPrimitive.content.toDouble())
            assertEquals(10, corpusMetrics.getValue("dynamicPoolTargetProfiles").jsonArray.size)
            assertTrue(corpusMetrics.getValue("lootProfileBaseItemOverlapMatrix").jsonObject.isNotEmpty())
            assertTrue(corpusMetrics.getValue("sameZoneSecretVsCadencePairs").jsonArray.isNotEmpty())
            assertTrue(corpusMetrics.getValue("sameZoneSecretVsRewardPairs").jsonArray.isNotEmpty())
            assertEquals(0, corpusMetrics.getValue("strictLocalIdentityViolationCount").jsonPrimitive.content.toInt())
            assertTrue(corpusMetrics.getValue("strictLocalIdentityViolations").jsonArray.isEmpty())
            assertTrue(corpusMetrics.getValue("specialTierPassiveFamilyDuplicateSummary").jsonObject.containsKey("duplicateFamilies"))
            val secretProfileIdentitySummaries = corpusMetrics.getValue("secretProfileIdentitySummaries").jsonArray
            assertEquals(5, secretProfileIdentitySummaries.size)
            assertTrue(secretProfileIdentitySummaries.all { summary -> summary.jsonObject.containsKey("identityAxes") })
            assertTrue(secretProfileIdentitySummaries.all { summary -> summary.jsonObject.containsKey("canonicalZoneId") })
            assertTrue(secretProfileIdentitySummaries.none { summary -> summary.jsonObject.containsKey("zoneId") })
            assertTrue(secretProfileIdentitySummaries.all { summary -> summary.jsonObject.containsKey("rewardStructureKeys") })
            assertTrue(secretProfileIdentitySummaries.any { summary -> summary.jsonObject.getValue("profileId").jsonPrimitive.content == "loot.deep_iron_slag_cache.secret" })
            assertTrue(
                secretProfileIdentitySummaries.any { summary ->
                    summary.jsonObject.getValue("profileId").jsonPrimitive.content == "loot.deep_iron_smuggler_stash.secret" &&
                        summary.jsonObject.getValue("rewardStructureKeys").jsonArray.size >= 3
                },
            )
            val preflightCulpritPairs = corpusMetrics.getValue("preflightCulpritPairs").jsonArray
            val preflightCulpritReasons = corpusMetrics.getValue("preflightCulpritReasons").jsonArray
            assertEquals(corpusMetrics.getValue("preflightCulpritPairCount").jsonPrimitive.content.toInt(), preflightCulpritPairs.size)
            assertEquals(
                preflightCulpritReasons.map { reason -> reason.jsonPrimitive.content }.toSet(),
                preflightCulpritPairs
                    .flatMap { pair -> pair.jsonObject.getValue("culpritReasons").jsonArray }
                    .map { reason -> reason.jsonPrimitive.content }
                    .toSet(),
            )
            assertTrue(corpusMetrics.getValue("affixPassiveKinds").jsonArray.isNotEmpty())
            assertTrue(corpusMetrics.getValue("lootProfileMaxBaseItemOverlap").jsonPrimitive.content.toDouble() < 0.95)
            assertTrue(
                setOf(
                    "loot.aggregate.overlap_below_threshold",
                    "loot.aggregate.max_overlap_sanity",
                    "loot.aggregate.same_zone_secret_cadence_guardrail",
                    "loot.aggregate.same_zone_secret_reward_guardrail",
                    "loot.aggregate.strict_pair_guardrail",
                    "loot.aggregate.passive_coverage",
                    "loot.aggregate.preflight_culprit_alignment",
                ).all(aggregateRuleIds::contains),
            )
            assertEquals(
                corpusMetrics.getValue("localIdentityFailurePairs").jsonArray.map { pair -> pair.jsonPrimitive.content }.toSet(),
                corpusMetrics
                    .getValue("localIdentityFailurePairs")
                    .jsonArray
                    .map { pair -> pair.jsonPrimitive.content }
                    .toSet()
                    .intersect(
                        corpusMetrics
                            .getValue("preflightCulpritPairs")
                            .jsonArray
                            .map { pair -> pair.jsonObject.getValue("pairId").jsonPrimitive.content }
                            .toSet(),
                    ),
            )
            assertEquals(
                corpusMetrics.getValue("preflightCulpritPairCount").jsonPrimitive.content,
                evaluationCache.getValue("preflightCulpritPairCount").jsonPrimitive.content,
            )
            assertEquals(6, Files.readAllLines(run.casesPath).count { line -> line.isNotBlank() })
        } finally {
            if (originalLootReportDir == null) {
                System.clearProperty("ktome.phase4.loot.reportDir")
            } else {
                System.setProperty("ktome.phase4.loot.reportDir", originalLootReportDir)
            }
            if (originalWhiteBoxReportDir == null) {
                System.clearProperty("ktome.phase4.whitebox.loot.reportDir")
            } else {
                System.setProperty("ktome.phase4.whitebox.loot.reportDir", originalWhiteBoxReportDir)
            }
            if (originalPreflightReportDir == null) {
                System.clearProperty("ktome.phase4.loot.preflight.reportDir")
            } else {
                System.setProperty("ktome.phase4.loot.preflight.reportDir", originalPreflightReportDir)
            }
            if (originalReuse == null) {
                System.clearProperty("ktome.phase4.reuseHarnessOutputs")
            } else {
                System.setProperty("ktome.phase4.reuseHarnessOutputs", originalReuse)
            }
        }
    }

    @Test
    fun `loot baseline-only rerun re-evaluates within ten seconds without rerunning loot kernel`() {
        val originalLootReportDir = System.getProperty("ktome.phase4.loot.reportDir")
        val originalWhiteBoxReportDir = System.getProperty("ktome.phase4.whitebox.loot.reportDir")
        val originalPreflightReportDir = System.getProperty("ktome.phase4.loot.preflight.reportDir")
        val originalReuse = System.getProperty("ktome.phase4.reuseHarnessOutputs")
        val originalBaselineOverride = System.getProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot")
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
            assertEquals(lootSummaryTimestamp, Files.getLastModifiedTime(lootRun.summaryPath))
        } finally {
            if (originalLootReportDir == null) {
                System.clearProperty("ktome.phase4.loot.reportDir")
            } else {
                System.setProperty("ktome.phase4.loot.reportDir", originalLootReportDir)
            }
            if (originalWhiteBoxReportDir == null) {
                System.clearProperty("ktome.phase4.whitebox.loot.reportDir")
            } else {
                System.setProperty("ktome.phase4.whitebox.loot.reportDir", originalWhiteBoxReportDir)
            }
            if (originalPreflightReportDir == null) {
                System.clearProperty("ktome.phase4.loot.preflight.reportDir")
            } else {
                System.setProperty("ktome.phase4.loot.preflight.reportDir", originalPreflightReportDir)
            }
            if (originalReuse == null) {
                System.clearProperty("ktome.phase4.reuseHarnessOutputs")
            } else {
                System.setProperty("ktome.phase4.reuseHarnessOutputs", originalReuse)
            }
            if (originalBaselineOverride == null) {
                System.clearProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot")
            } else {
                System.setProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot", originalBaselineOverride)
            }
        }
    }

    @Test
    fun `white-box loot fails fast when strict pair ceiling is tightened below observed overlap`() {
        val originalLootReportDir = System.getProperty("ktome.phase4.loot.reportDir")
        val originalWhiteBoxReportDir = System.getProperty("ktome.phase4.whitebox.loot.reportDir")
        val originalPreflightReportDir = System.getProperty("ktome.phase4.loot.preflight.reportDir")
        val originalReuse = System.getProperty("ktome.phase4.reuseHarnessOutputs")
        val originalBaselineOverride = System.getProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot")
        val repoRoot = VerificationCacheSupport.repoRoot()
        val baselineCopy = tempDir.resolve("phase4-loot-baseline-strict-violation.json")
        Files.copy(repoRoot.resolve(Phase4OwnerBaselineRegistry.LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH), baselineCopy)
        Phase4OwnerBaselineTestSupport.overrideStrictPairCeiling(
            path = baselineCopy,
            secretProfileId = "loot.deep_iron_slag_cache.secret",
            maxValue = 0.19,
        )
        try {
            val effectivePreflightReportDir = tempDir.resolve("loot-preflight").toString()
            val effectiveLootReportDir = tempDir.resolve("loot-reports").toString()
            val effectiveWhiteBoxReportDir = tempDir.resolve("whitebox-loot").toString()
            System.setProperty("ktome.phase4.loot.preflight.reportDir", effectivePreflightReportDir)
            System.setProperty("ktome.phase4.loot.reportDir", effectiveLootReportDir)
            System.setProperty("ktome.phase4.whitebox.loot.reportDir", effectiveWhiteBoxReportDir)
            System.setProperty("ktome.phase4.reuseHarnessOutputs", "true")
            System.setProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot", baselineCopy.toString())

            LootPreflightRunner.run()
            val run = WhiteBoxLootRunner.run()
            val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
            val ownerEvaluation = payload.getValue("ownerEvaluation").jsonObject
            val corpusMetrics =
                payload.getValue("aggregates").jsonArray
                    .first { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == "corpus" }
                    .jsonObject
                    .getValue("metrics")
                    .jsonObject
            val entries = ownerEvaluation.getValue("entries").jsonArray
            val cadenceEntry =
                entries.first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "sameZoneSecretVsCadenceMaxOverlap" }.jsonObject

            assertEquals("FAIL", ownerEvaluation.getValue("verdict").jsonPrimitive.content)
            assertTrue(corpusMetrics.getValue("strictLocalIdentityViolations").jsonArray.isNotEmpty())
            assertEquals("UNEXPECTED_REGRESSION", cadenceEntry.getValue("status").jsonPrimitive.content)
            assertTrue(cadenceEntry.getValue("note").jsonPrimitive.content.contains("deep_iron_pit:loot.deep_iron_slag_cache.secret"))
            assertTrue(cadenceEntry.getValue("note").jsonPrimitive.content.contains("0.190"))
        } finally {
            if (originalLootReportDir == null) {
                System.clearProperty("ktome.phase4.loot.reportDir")
            } else {
                System.setProperty("ktome.phase4.loot.reportDir", originalLootReportDir)
            }
            if (originalWhiteBoxReportDir == null) {
                System.clearProperty("ktome.phase4.whitebox.loot.reportDir")
            } else {
                System.setProperty("ktome.phase4.whitebox.loot.reportDir", originalWhiteBoxReportDir)
            }
            if (originalPreflightReportDir == null) {
                System.clearProperty("ktome.phase4.loot.preflight.reportDir")
            } else {
                System.setProperty("ktome.phase4.loot.preflight.reportDir", originalPreflightReportDir)
            }
            if (originalReuse == null) {
                System.clearProperty("ktome.phase4.reuseHarnessOutputs")
            } else {
                System.setProperty("ktome.phase4.reuseHarnessOutputs", originalReuse)
            }
            if (originalBaselineOverride == null) {
                System.clearProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot")
            } else {
                System.setProperty("ktome.phase4.ownerBaselineOverride.whiteBoxLoot", originalBaselineOverride)
            }
        }
    }

}
