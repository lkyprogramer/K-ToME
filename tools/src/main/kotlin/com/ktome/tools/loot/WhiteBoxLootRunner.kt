package com.ktome.tools.loot

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.game.data.DataLoader
import com.ktome.tools.phase4.Phase4OwnerBaselineRegistry
import com.ktome.tools.phase4.Phase4OwnerMetricTargets
import com.ktome.tools.phase4.requiredMetric
import com.ktome.tools.mapgen.phase4HarnessHeader
import com.ktome.tools.verification.EvaluationEntry
import com.ktome.tools.verification.EvaluationEntryStatus
import com.ktome.tools.verification.EvaluationResult
import com.ktome.tools.verification.EvaluationVerdict
import com.ktome.tools.verification.VerificationBaseline
import com.ktome.tools.verification.VerificationCacheSupport
import com.ktome.tools.whitebox.WhiteBoxDomainWriteRequest
import com.ktome.tools.whitebox.WhiteBoxReportWriter
import com.ktome.tools.whitebox.toVerificationReportHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class WhiteBoxLootRun(
    val caseCount: Int,
    val failedAssertions: Int,
    val summaryPath: Path,
    val casesPath: Path,
    val reportPath: Path,
)

private data class LootPreflightArtifacts(
    val summary: LootPreflightSummary,
) {
    val culpritPairs: List<LootPreflightPairSummary> = summary.culpritPairs.sortedBy(LootPreflightPairSummary::pairId)
    val culpritPairIds: Set<String> = culpritPairs.mapTo(linkedSetOf(), LootPreflightPairSummary::pairId)
    val culpritReasons: List<String> = culpritPairs.flatMap(LootPreflightPairSummary::culpritReasons).distinct().sorted()
    val culpritReasonCounts: Map<String, Int> =
        culpritPairs
            .flatMap(LootPreflightPairSummary::culpritReasons)
            .groupingBy { it }
            .eachCount()
            .toSortedMap()
}

object WhiteBoxLootRunner {
    const val HARNESS_ID: String = "whiteBoxLoot"
    private const val DOMAIN_ID: String = "loot"
    private const val CORPUS_ID: String = "P4_PR05_LOOT_WHITEBOX"
    private const val WHITEBOX_EVALUATION_CACHE_VERSION: String = "uvr-pr05-whitebox-loot-eval-v3"
    private val json: Json = Json { prettyPrint = true; explicitNulls = false }

    fun run(): WhiteBoxLootRun {
        val repoRoot = VerificationCacheSupport.repoRoot()
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val cacheDirs = VerificationCacheSupport.cacheDirs(domainId = DOMAIN_ID, repoRoot = repoRoot)
        val preflightSummaryPath = LootBalanceLabRunner.lootPreflightSummaryPath(repoRoot)
        val baselinePath = repoRoot.resolve(Phase4OwnerBaselineRegistry.lootBaselinePath())
        val baseline = VerificationBaseline.read(baselinePath)
        val loader = DataLoader(LOOT_REPORT_LOCALE)
        val schemaCatalog = loader.loadSchemaCatalog()
        val itemBundle = loader.loadItemBundle()
        val dynamicPoolCoverageSummary = computeDynamicPoolCoverage(schemaCatalog.lootProfiles)
        val specialTierPassiveFamilyDuplicateSummary = computeSpecialTierPassiveFamilyDuplicateSummary(itemBundle)

        val kernelRun =
            if (reuseHarnessOutputs()) {
                LootBalanceLabRunner.readKernelRun() ?: LootLabKernel.execute().kernelRun
            } else {
                LootLabKernel.execute().kernelRun
            }
        val strictAwareProfileOverlapSummary =
            kernelRun.profileOverlapSummary.withStrictPairCeilings(strictSecretProfileMaxOverlapTargets(baseline))
        val overlapSummaryJson = strictAwareProfileOverlapSummary.toJson()
        val preflightArtifacts = loadPreflightArtifacts(preflightSummaryPath.parent)
        val corpusAggregateMetrics =
            corpusMetrics(
                kernelRun = kernelRun,
                profileOverlapSummary = strictAwareProfileOverlapSummary,
                overlapSummaryJson = overlapSummaryJson,
                preflightArtifacts = preflightArtifacts,
                dynamicPoolCoverageSummary = dynamicPoolCoverageSummary,
                specialTierPassiveFamilyDuplicateSummary = specialTierPassiveFamilyDuplicateSummary,
            )
        val evaluationFingerprint =
            VerificationCacheSupport.sha256(
                WHITEBOX_EVALUATION_CACHE_VERSION,
                VerificationCacheSupport.sha256Json(corpusAggregateMetrics),
                if (Files.isRegularFile(preflightSummaryPath)) VerificationCacheSupport.sha256Files(listOf(preflightSummaryPath)) else "missing-preflight",
                if (Files.isRegularFile(baselinePath)) VerificationCacheSupport.sha256Files(listOf(baselinePath)) else "missing-baseline",
            )
        val evaluationCacheDir = cacheDirs.evaluationDir.resolve("$WHITEBOX_EVALUATION_CACHE_VERSION-$evaluationFingerprint")
        val cachedSummaryPath = evaluationCacheDir.resolve("whitebox-loot-summary.json")
        val cachedCasesPath = evaluationCacheDir.resolve("whitebox-loot-cases.jsonl")
        val cachedReportPath = evaluationCacheDir.resolve("whitebox-loot-report.md")
        if (Files.isRegularFile(cachedSummaryPath) && Files.isRegularFile(cachedCasesPath) && Files.isRegularFile(cachedReportPath)) {
            VerificationCacheSupport.clearDirectory(outputDir)
            VerificationCacheSupport.copyDirectoryContents(evaluationCacheDir, outputDir)
            return cachedRun(outputDir.resolve("whitebox-loot-summary.json"), outputDir.resolve("whitebox-loot-cases.jsonl"), outputDir.resolve("whitebox-loot-report.md"))
        }
        val header =
            phase4HarnessHeader(harnessId = HARNESS_ID, seedList = kernelRun.matrixSeeds, locale = LOOT_REPORT_LOCALE.id)
                .toVerificationReportHeader(corpusId = CORPUS_ID)
        val corpus =
            WhiteBoxCorpusSpec(
                corpusId = CORPUS_ID,
                description = "Six fixed PR-05 loot matrices with 10000 rolls each, sharing the same generator and trace contract as lootBalanceLab.",
                sampleCount = kernelRun.matrices.size,
            )
        val caseReports =
            kernelRun.matrices.map { matrix ->
                val joinKey =
                    WhiteBoxJoinKey(
                        seed = matrix.spec.seedBase,
                        zoneId = matrix.spec.zoneId,
                        scenarioId = matrix.spec.id,
                    )
                val assertions = caseAssertions(matrix)
                val artifacts =
                    if (
                        WhiteBoxReportWriter.shouldWriteArtifacts(
                            retentionPolicy = ArtifactRetentionPolicy.ALL,
                            joinKey = joinKey,
                            assertions = assertions,
                        )
                    ) {
                        writeArtifacts(outputDir = outputDir, joinKey = joinKey, matrix = matrix)
                    } else {
                        emptyList()
                    }
                WhiteBoxCaseReport(
                    joinKey = joinKey,
                    facts = caseFacts(matrix),
                    fingerprints =
                        mapOf(
                            "matrixId" to matrix.spec.id,
                            "zoneSource" to "${matrix.spec.zoneId}:${matrix.spec.sourceTier.name}",
                            "magicFind" to matrix.spec.magicFind.toString(),
                        ),
                    assertions = assertions,
                    artifacts = artifacts,
                )
            }
        val aggregates =
            listOf(
                WhiteBoxAggregateReport(
                    groupId = "corpus",
                    sampleCount = kernelRun.matrices.size,
                    metrics = corpusAggregateMetrics,
                    assertions =
                        corpusAssertions(
                            kernelRun = kernelRun,
                            profileOverlapSummary = strictAwareProfileOverlapSummary,
                            overlapSummaryJson = overlapSummaryJson,
                            preflightArtifacts = preflightArtifacts,
                        ),
                ),
            )
        val result =
            WhiteBoxReportWriter.write(
                WhiteBoxDomainWriteRequest(
                    domainId = DOMAIN_ID,
                    outputDir = outputDir,
                    header = header,
                    corpus = corpus,
                    cases = caseReports,
                    aggregates = aggregates,
                    retentionPolicy = ArtifactRetentionPolicy.ALL,
                ),
            )
        val ownerEvaluation =
            buildOwnerEvaluation(
                profileOverlapSummary = strictAwareProfileOverlapSummary,
                baseline = baseline,
                preflightArtifacts = preflightArtifacts,
                corpusAggregateMetrics = corpusAggregateMetrics,
                dynamicPoolCoverageSummary = dynamicPoolCoverageSummary,
            )
        decorateSummary(
            summaryPath = result.summaryPath,
            repoRoot = repoRoot,
            preflightSummaryPath = preflightSummaryPath,
            baselinePath = baselinePath,
            evaluationFingerprint = evaluationFingerprint,
            ownerEvaluation = ownerEvaluation,
            preflightArtifacts = preflightArtifacts,
        )
        VerificationCacheSupport.clearDirectory(evaluationCacheDir)
        VerificationCacheSupport.copyDirectoryContents(outputDir, evaluationCacheDir)
        return WhiteBoxLootRun(
            caseCount = caseReports.size,
            failedAssertions = result.failedAssertions,
            summaryPath = result.summaryPath,
            casesPath = result.casesPath,
            reportPath = result.reportPath,
        )
    }

    private fun reuseHarnessOutputs(): Boolean = System.getProperty("ktome.phase4.reuseHarnessOutputs") == "true"

    private fun caseAssertions(matrix: LootMatrixResult): List<WhiteBoxAssertionResult> =
        listOf(
            WhiteBoxAssertionResult(
                ruleId = "loot.case.magic_rate_drift",
                passed = matrix.magicRateDrift <= 0.05,
                message = "MAGIC drift stays within 5 percentage points of the PR-05 formula expectation.",
                context = buildJsonObject { put("magicRateDrift", matrix.magicRateDrift) },
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.case.rare_rate_drift",
                passed = matrix.rareRateDrift <= 0.05,
                message = "RARE drift stays within 5 percentage points of the PR-05 formula expectation.",
                context = buildJsonObject { put("rareRateDrift", matrix.rareRateDrift) },
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.case.special_rate_relative_error",
                passed =
                    matrix.uniqueRelativeError <= 0.25 &&
                        matrix.artifactRelativeError <= 0.25,
                message = "UNIQUE / ARTIFACT relative error stays within the PR-05 tolerance.",
                context =
                    buildJsonObject {
                        put("uniqueRelativeError", matrix.uniqueRelativeError)
                        put("artifactRelativeError", matrix.artifactRelativeError)
                    },
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.case.affix_budget_deviation",
                passed =
                    matrix.affixBudgetAverageDeviation <= 0.05 &&
                        matrix.affixBudgetP95Deviation <= 0.12,
                message = "Affix budget deviation stays inside the PR-05 average/P95 guardrails.",
                context =
                    buildJsonObject {
                        put("averageDeviation", matrix.affixBudgetAverageDeviation)
                        put("p95Deviation", matrix.affixBudgetP95Deviation)
                    },
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.case.allowed_sources_only",
                passed =
                    when (matrix.spec.sourceTier) {
                        com.ktome.core.loot.SourceTier.NORMAL -> matrix.uniqueRate == 0.0 && matrix.artifactRate == 0.0
                        com.ktome.core.loot.SourceTier.CHEST -> !matrix.spec.allowsArtifact() || matrix.artifactRate > 0.0
                        else -> true
                    },
                message = "Special tiers only appear on allowed sources for this matrix.",
                context =
                    buildJsonObject {
                        put("sourceTier", matrix.spec.sourceTier.name)
                        put("sourceDescriptor", matrix.spec.sourceDescriptor)
                        put("uniqueRate", matrix.uniqueRate)
                        put("artifactRate", matrix.artifactRate)
                    },
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.case.cast_speed_dr",
                passed = matrix.castSpeedSamples.all { sample -> sample.effectiveCastSpeed <= sample.rawCastSpeedRating.toDouble() },
                message = "castSpeed samples never exceed the raw rating after DR.",
                context =
                    buildJsonObject {
                        put("sampleCount", matrix.castSpeedSamples.size)
                        put("p50", matrix.castSpeedPostDrP50)
                        put("p95", matrix.castSpeedPostDrP95)
                    },
            ),
        )

    private fun corpusAssertions(
        kernelRun: LootKernelRun,
        profileOverlapSummary: LootProfileOverlapSummary,
        overlapSummaryJson: JsonObject,
        preflightArtifacts: LootPreflightArtifacts,
    ): List<WhiteBoxAssertionResult> =
        listOf(
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.template_pool_thresholds",
                passed = kernelRun.specialPoolSummary.passesThresholds,
                message = "Template pool satisfies OPT PR-03 counts, zone coverage, and source-tier thresholds.",
                context = kernelRun.specialPoolSummary.toJson(),
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.magic_find_clamp",
                passed = kernelRun.clampComparison.withinTolerance,
                message = "magicFind=1.50 stays distributionally aligned with magicFind=1.00 after clamp.",
                context = kernelRun.clampComparison.toJson(),
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.overlap_below_threshold",
                passed = profileOverlapSummary.averageOverlap < 0.30,
                message = "Average loot-profile base item overlap stays below the OPT PR-04 exit threshold.",
                context = overlapSummaryJson,
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.max_overlap_sanity",
                passed = profileOverlapSummary.maxOverlap < 0.95,
                message = "No loot-profile base item pool remains a near-total subset of another profile.",
                context = overlapSummaryJson,
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.same_zone_secret_cadence_guardrail",
                passed = profileOverlapSummary.sameZoneSecretVsCadenceMaxOverlap <= SAME_ZONE_SECRET_CADENCE_MAX_OVERLAP_TARGET,
                message = "Same-zone secret versus cadence overlap stays below the V2OPT PR-01 local identity guardrail.",
                context = overlapSummaryJson,
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.same_zone_secret_reward_guardrail",
                passed = profileOverlapSummary.sameZoneSecretVsRewardMaxOverlap <= SAME_ZONE_SECRET_REWARD_MAX_OVERLAP_TARGET,
                message = "Same-zone secret versus reward overlap stays below the V2OPT PR-01 local identity guardrail.",
                context = overlapSummaryJson,
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.strict_pair_guardrail",
                passed = profileOverlapSummary.strictLocalIdentityViolations.isEmpty(),
                message = "PR-03 strict pair guardrails hold for abyssal/deep-iron secret identities.",
                context =
                    buildJsonObject {
                        putJsonArray("strictLocalIdentityViolations") {
                            profileOverlapSummary.strictLocalIdentityViolations.forEach { violation ->
                                add(violation.toJson())
                            }
                        }
                    },
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.passive_coverage",
                passed = kernelRun.passiveCoverageSummary.coverageRatio >= 0.80,
                message = "Affix passive coverage stays at or above the OPT PR-03 threshold.",
                context = kernelRun.passiveCoverageSummary.toJson(),
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.preflight_culprit_alignment",
                passed = (profileOverlapSummary.localIdentityFailurePairs.toSet() - preflightArtifacts.culpritPairIds).isEmpty(),
                message = "verifyLootPreflight culprit pairs cover every same-zone local identity failure surfaced by whiteBoxLoot.",
                context =
                    buildJsonObject {
                        putJsonArray("localIdentityFailurePairs") {
                            profileOverlapSummary.localIdentityFailurePairs.sorted().forEach { pairId -> add(JsonPrimitive(pairId)) }
                        }
                        putJsonArray("preflightCulpritPairIds") {
                            preflightArtifacts.culpritPairIds.sorted().forEach { pairId -> add(JsonPrimitive(pairId)) }
                        }
                        putJsonArray("missingPairIds") {
                            (profileOverlapSummary.localIdentityFailurePairs.toSet() - preflightArtifacts.culpritPairIds)
                                .sorted()
                                .forEach { pairId -> add(JsonPrimitive(pairId)) }
                        }
                        putJsonObject("preflightCulpritReasonCounts") {
                            preflightArtifacts.culpritReasonCounts.forEach { (reason, count) -> put(reason, count) }
                        }
                    },
            ),
        )

    private fun caseFacts(matrix: LootMatrixResult): JsonObject =
        buildJsonObject {
            put("sourceLevel", matrix.spec.sourceLevel)
            put("sourceTier", matrix.spec.sourceTier.name)
            put("sourceDescriptor", matrix.spec.sourceDescriptor)
            put("zoneId", matrix.spec.zoneId)
            put("playerLevel", matrix.spec.playerLevel)
            put("magicFind", matrix.spec.magicFind)
            put("rollCount", matrix.totalRolls)
            putJsonObject("rarityTierDistribution") {
                matrix.rarityTierDistribution.forEach { (tier, rate) -> put(tier, rate) }
            }
            putJsonObject("affixBudgetDeviation") {
                put("average", matrix.affixBudgetAverageDeviation)
                put("p95", matrix.affixBudgetP95Deviation)
            }
            put("specialTierEligibilityRate", matrix.specialTierEligibilityRate)
            put("rarePityActivations", matrix.rarePityActivations)
            put("uniquePityActivations", matrix.uniquePityActivations)
            put("castSpeedPostDrP50", matrix.castSpeedPostDrP50)
            put("castSpeedPostDrP95", matrix.castSpeedPostDrP95)
            put("uniqueArtifactOutcomeCount", matrix.uniqueArtifactOutcomeCount)
            put("meaningfulUniqueArtifactSwapCount", matrix.meaningfulUniqueArtifactSwapCount)
            put("uniqueArtifactMeaningfulSwapRate", matrix.meaningfulUniqueArtifactSwapRate)
        }

    private fun corpusMetrics(
        kernelRun: LootKernelRun,
        profileOverlapSummary: LootProfileOverlapSummary,
        overlapSummaryJson: JsonObject,
        preflightArtifacts: LootPreflightArtifacts,
        dynamicPoolCoverageSummary: DynamicPoolCoverageSummary,
        specialTierPassiveFamilyDuplicateSummary: SpecialTierPassiveFamilyDuplicateSummary,
    ): JsonObject =
        buildJsonObject {
            put("matrixCount", kernelRun.matrices.size)
            put("totalRolls", kernelRun.totalRolls)
            put("failedExpectationCount", kernelRun.failedExpectationCount)
            put("maxMagicRateDrift", kernelRun.matrices.maxOfOrNull(LootMatrixResult::magicRateDrift) ?: 0.0)
            put("maxRareRateDrift", kernelRun.matrices.maxOfOrNull(LootMatrixResult::rareRateDrift) ?: 0.0)
            put("maxUniqueRelativeError", kernelRun.matrices.maxOfOrNull(LootMatrixResult::uniqueRelativeError) ?: 0.0)
            put("maxArtifactRelativeError", kernelRun.matrices.maxOfOrNull(LootMatrixResult::artifactRelativeError) ?: 0.0)
            put("rarePityActivations", kernelRun.matrices.sumOf(LootMatrixResult::rarePityActivations))
            put("uniquePityActivations", kernelRun.matrices.sumOf(LootMatrixResult::uniquePityActivations))
            put("clampMaxDistributionDelta", kernelRun.clampComparison.maxDistributionDelta)
            put("templatePoolThresholdsPassed", kernelRun.specialPoolSummary.passesThresholds)
            put("affixCount", kernelRun.specialPoolSummary.affixCount)
            put("uniqueTemplateCount", kernelRun.specialPoolSummary.uniqueTemplateCount)
            put("artifactTemplateCount", kernelRun.specialPoolSummary.artifactTemplateCount)
            put("totalCount", kernelRun.specialPoolSummary.totalCount)
            put("lootProfileAverageBaseItemOverlap", profileOverlapSummary.averageOverlap)
            put("lootProfileMaxBaseItemOverlap", profileOverlapSummary.maxOverlap)
            put("lootProfileDistinctBaseItemCount", profileOverlapSummary.distinctBaseItemCount)
            put("lootProfileBaseItemOverlapMatrix", overlapSummaryJson.getValue("matrix"))
            put("sameZoneSecretVsCadenceMaxOverlap", profileOverlapSummary.sameZoneSecretVsCadenceMaxOverlap)
            put("sameZoneSecretVsRewardMaxOverlap", profileOverlapSummary.sameZoneSecretVsRewardMaxOverlap)
            put("sameZoneSecretVsCadencePairs", overlapSummaryJson.getValue("sameZoneSecretVsCadencePairs"))
            put("sameZoneSecretVsRewardPairs", overlapSummaryJson.getValue("sameZoneSecretVsRewardPairs"))
            put("localIdentityFailurePairs", overlapSummaryJson.getValue("localIdentityFailurePairs"))
            put("strictLocalIdentityViolationCount", profileOverlapSummary.strictLocalIdentityViolations.size)
            putJsonArray("strictLocalIdentityViolations") {
                profileOverlapSummary.strictLocalIdentityViolations.forEach { violation ->
                    add(violation.toJson())
                }
            }
            putJsonArray("secretProfileIdentitySummaries") {
                profileOverlapSummary.secretProfileIdentitySummaries.forEach { summary ->
                    add(summary.toJson())
                }
            }
            put("preflightProfileCount", preflightArtifacts.summary.profileCount)
            put("preflightPairCount", preflightArtifacts.summary.pairCount)
            put("preflightCulpritPairCount", preflightArtifacts.summary.culpritPairCount)
            putJsonArray("preflightCulpritReasons") {
                preflightArtifacts.culpritReasons.forEach { reason -> add(JsonPrimitive(reason)) }
            }
            putJsonObject("preflightCulpritReasonCounts") {
                preflightArtifacts.culpritReasonCounts.forEach { (reason, count) -> put(reason, count) }
            }
            putJsonArray("preflightCulpritPairs") {
                preflightArtifacts.culpritPairs.forEach { pair -> add(pair.toJson()) }
            }
            put("affixPassiveCoverage", kernelRun.passiveCoverageSummary.coverageRatio)
            putJsonArray("affixPassiveKinds") {
                kernelRun.passiveCoverageSummary.passiveKinds.sorted().forEach { passiveKind -> add(kotlinx.serialization.json.JsonPrimitive(passiveKind)) }
            }
            put("dynamicPoolCoverage", dynamicPoolCoverageSummary.dynamicPoolCoverage)
            putJsonArray("dynamicPoolTargetProfiles") {
                dynamicPoolCoverageSummary.targetProfiles.forEach { summary -> add(summary.toJson()) }
            }
            put("uniqueArtifactOutcomeCount", kernelRun.uniqueArtifactOutcomeCount)
            put("meaningfulUniqueArtifactSwapCount", kernelRun.meaningfulUniqueArtifactSwapCount)
            put("uniqueArtifactMeaningfulSwapRate", kernelRun.uniqueArtifactMeaningfulSwapRate)
            put("specialTierPassiveFamilyDuplicateCount", specialTierPassiveFamilyDuplicateSummary.duplicateFamilyCount)
            put("specialTierPassiveFamilyDuplicateSummary", specialTierPassiveFamilyDuplicateSummary.toJson())
        }

    private fun writeArtifacts(
        outputDir: Path,
        joinKey: WhiteBoxJoinKey,
        matrix: LootMatrixResult,
    ): List<com.ktome.core.harness.whitebox.WhiteBoxArtifact> =
        listOf(
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "rarity-table",
                kind = "rarity_table",
                fileName = "rarity-table.md",
                summary = "Observed versus expected rarity distribution.",
                content = LootLabKernel.renderRarityTable(matrix),
                tags = listOf("rarity", "distribution"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "affix-cost-breakdown",
                kind = "affix_cost_breakdown",
                fileName = "affix-cost-breakdown.md",
                summary = "Affix budget deviation plus cost histogram.",
                content = LootLabKernel.renderAffixBreakdown(matrix),
                tags = listOf("affix", "budget"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "special-tier-trace",
                kind = "special_tier_trace",
                fileName = "special-tier-trace.md",
                summary = "Eligibility and upgrade samples from the shared PR-05 trace.",
                content = LootLabKernel.renderSpecialTrace(matrix),
                tags = listOf("special", "trace"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "pity-timeline",
                kind = "pity_timeline",
                fileName = "pity-timeline.md",
                summary = "Rare and special pity activation timeline.",
                content = LootLabKernel.renderPityTimeline(matrix),
                tags = listOf("pity"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "cast-speed-dr-table",
                kind = "cast_speed_dr",
                fileName = "cast-speed-dr.md",
                summary = "Raw cast speed versus post-DR output.",
                content = LootLabKernel.renderCastSpeedTable(matrix),
                tags = listOf("cast_speed", "dr"),
            ),
        )

    private fun cachedRun(
        summaryPath: Path,
        casesPath: Path,
        reportPath: Path,
    ): WhiteBoxLootRun {
        val payload = json.parseToJsonElement(Files.readString(summaryPath)).jsonObject
        val rewritten =
            buildJsonObject {
                payload.forEach { (key, value) ->
                    if (key == "evaluationCache") {
                        putJsonObject("evaluationCache") {
                            value.jsonObject.forEach { (cacheKey, cacheValue) ->
                                put(cacheKey, if (cacheKey == "cacheStatus") JsonPrimitive("HIT") else cacheValue)
                            }
                        }
                    } else {
                        put(key, value)
                    }
                }
            }
        Files.writeString(summaryPath, json.encodeToString(JsonElement.serializer(), rewritten))
        val summary = rewritten.getValue("summary").jsonObject
        return WhiteBoxLootRun(
            caseCount = summary.getValue("caseCount").jsonPrimitive.content.toInt(),
            failedAssertions = summary.getValue("failedAssertions").jsonPrimitive.content.toInt(),
            summaryPath = summaryPath,
            casesPath = casesPath,
            reportPath = reportPath,
        )
    }

    private fun decorateSummary(
        summaryPath: Path,
        repoRoot: Path,
        preflightSummaryPath: Path,
        baselinePath: Path,
        evaluationFingerprint: String,
        ownerEvaluation: EvaluationResult,
        preflightArtifacts: LootPreflightArtifacts,
    ) {
        val payload = json.parseToJsonElement(Files.readString(summaryPath)).jsonObject
        val decorated =
            buildJsonObject {
                payload.forEach { (key, value) -> put(key, value) }
                put(
                    "ownerEvaluation",
                    json.parseToJsonElement(json.encodeToString(EvaluationResult.serializer(), ownerEvaluation)),
                )
                putJsonObject("evaluationCache") {
                    put("contractVersion", WHITEBOX_EVALUATION_CACHE_VERSION)
                    put("inputFingerprint", evaluationFingerprint)
                    put("cacheStatus", "MISS")
                    put("preflightSummaryPath", VerificationCacheSupport.relativeToRepo(preflightSummaryPath, repoRoot))
                    put("preflightCulpritPairCount", preflightArtifacts.summary.culpritPairCount)
                    put("baselinePath", VerificationCacheSupport.relativeToRepo(baselinePath, repoRoot))
                }
            }
        Files.writeString(summaryPath, json.encodeToString(JsonElement.serializer(), decorated))
    }

    private fun buildOwnerEvaluation(
        profileOverlapSummary: LootProfileOverlapSummary,
        baseline: VerificationBaseline,
        preflightArtifacts: LootPreflightArtifacts,
        corpusAggregateMetrics: JsonObject,
        dynamicPoolCoverageSummary: DynamicPoolCoverageSummary,
    ): EvaluationResult {
        val cadenceMetricId = "sameZoneSecretVsCadenceMaxOverlap"
        val rewardMetricId = "sameZoneSecretVsRewardMaxOverlap"
        val dynamicPoolMetricId = "dynamicPoolCoverage"
        val specialTierDuplicateMetricId = "specialTierPassiveFamilyDuplicateCount"
        val overlapSummaryJson = profileOverlapSummary.toJson()
        val strictViolationBreakdown =
            profileOverlapSummary.strictLocalIdentityViolations.splitByLocalIdentityPairType()
        val cadenceStrictViolations = strictViolationBreakdown.cadenceViolations
        val rewardStrictViolations = strictViolationBreakdown.rewardViolations
        val localRewardEvaluation =
            buildLocalRewardIdentityEvaluation(
            baseline = baseline,
            strictViolations = profileOverlapSummary.strictLocalIdentityViolations,
            cadenceInput =
                LocalRewardIdentityMetricEvaluationInput(
                    metricId = cadenceMetricId,
                    overlap = profileOverlapSummary.sameZoneSecretVsCadenceMaxOverlap,
                    currentValueElement =
                        buildJsonObject {
                            put("maxOverlap", profileOverlapSummary.sameZoneSecretVsCadenceMaxOverlap)
                            put("pairs", overlapSummaryJson.getValue("sameZoneSecretVsCadencePairs"))
                            put("localIdentityFailurePairs", overlapSummaryJson.getValue("localIdentityFailurePairs"))
                            putJsonArray("strictLocalIdentityViolations") {
                                cadenceStrictViolations.forEach { violation -> add(violation.toJson()) }
                            }
                            putJsonArray("preflightCulpritPairs") {
                                preflightArtifacts.culpritPairs.forEach { pair -> add(pair.toJson()) }
                            }
                            putJsonArray("preflightCulpritReasons") {
                                preflightArtifacts.culpritReasons.forEach { reason -> add(JsonPrimitive(reason)) }
                            }
                        },
                    pairCount = profileOverlapSummary.sameZoneSecretVsCadencePairs.size,
                    includeOverlapFormula = true,
                    preflightCulpritCount = preflightArtifacts.summary.culpritPairCount,
                ),
            rewardInput =
                LocalRewardIdentityMetricEvaluationInput(
                    metricId = rewardMetricId,
                    overlap = profileOverlapSummary.sameZoneSecretVsRewardMaxOverlap,
                    currentValueElement =
                        buildJsonObject {
                            put("maxOverlap", profileOverlapSummary.sameZoneSecretVsRewardMaxOverlap)
                            put("pairs", overlapSummaryJson.getValue("sameZoneSecretVsRewardPairs"))
                            put("localIdentityFailurePairs", overlapSummaryJson.getValue("localIdentityFailurePairs"))
                            putJsonArray("strictLocalIdentityViolations") {
                                rewardStrictViolations.forEach { violation -> add(violation.toJson()) }
                            }
                            putJsonArray("preflightCulpritPairs") {
                                preflightArtifacts.culpritPairs.forEach { pair -> add(pair.toJson()) }
                            }
                            putJsonArray("preflightCulpritReasons") {
                                preflightArtifacts.culpritReasons.forEach { reason -> add(JsonPrimitive(reason)) }
                            }
                        },
                    pairCount = profileOverlapSummary.sameZoneSecretVsRewardPairs.size,
                    failurePairCount = profileOverlapSummary.localIdentityFailurePairs.size,
                    preflightCulpritCount = preflightArtifacts.summary.culpritPairCount,
                ),
            detailsByMetricId =
                mapOf(
                    cadenceMetricId to corpusAggregateMetrics,
                    rewardMetricId to corpusAggregateMetrics,
                ),
        )
        val dynamicPoolRange = baseline.requiredMetric(dynamicPoolMetricId)
        val dynamicPoolStatus =
            if (Phase4OwnerMetricTargets.passes(dynamicPoolRange, dynamicPoolCoverageSummary.dynamicPoolCoverage)) {
                EvaluationEntryStatus.PASS
            } else {
                EvaluationEntryStatus.UNEXPECTED_REGRESSION
            }
        val specialTierDuplicateRange = baseline.requiredMetric(specialTierDuplicateMetricId)
        val specialTierPassiveFamilyDuplicateSummary = corpusAggregateMetrics.getValue("specialTierPassiveFamilyDuplicateSummary")
        val specialTierPassiveFamilyDuplicateCount =
            corpusAggregateMetrics.getValue("specialTierPassiveFamilyDuplicateCount").jsonPrimitive.content.toInt()
        val uniqueArtifactMeaningfulSwapRate = corpusAggregateMetrics.getValue("uniqueArtifactMeaningfulSwapRate").jsonPrimitive.content.toDouble()
        val dynamicPoolEntry =
            EvaluationEntry(
                metricId = dynamicPoolMetricId,
                status = dynamicPoolStatus,
                currentValue =
                    buildJsonObject {
                        put("rate", dynamicPoolCoverageSummary.dynamicPoolCoverage)
                        putJsonArray("dynamicPoolTargetProfiles") {
                            dynamicPoolCoverageSummary.targetProfiles.forEach { summary -> add(summary.toJson()) }
                        }
                        put("specialTierPassiveFamilyDuplicateSummary", specialTierPassiveFamilyDuplicateSummary)
                        put("uniqueArtifactMeaningfulSwapRate", corpusAggregateMetrics.getValue("uniqueArtifactMeaningfulSwapRate"))
                    },
                currentValueText =
                    "${formatPercent(dynamicPoolCoverageSummary.dynamicPoolCoverage)} " +
                        "(${dynamicPoolCoverageSummary.dynamicProfileCount}/${dynamicPoolCoverageSummary.targetProfiles.size})",
                targetText = Phase4OwnerMetricTargets.targetText(dynamicPoolMetricId, dynamicPoolRange),
                note =
                    "duplicateFamilies=${specialTierPassiveFamilyDuplicateSummary.jsonObject.getValue("duplicateFamilyCount").jsonPrimitive.content}; " +
                        "meaningfulSwap=${formatPercent(uniqueArtifactMeaningfulSwapRate)}",
                details = corpusAggregateMetrics,
            )
        val specialTierDuplicateEntry =
            EvaluationEntry(
                metricId = specialTierDuplicateMetricId,
                status =
                    if (Phase4OwnerMetricTargets.passes(specialTierDuplicateRange, specialTierPassiveFamilyDuplicateCount.toDouble())) {
                        EvaluationEntryStatus.PASS
                    } else {
                        EvaluationEntryStatus.UNEXPECTED_REGRESSION
                    },
                currentValue =
                    buildJsonObject {
                        put("count", specialTierPassiveFamilyDuplicateCount)
                        put("specialTierPassiveFamilyDuplicateSummary", specialTierPassiveFamilyDuplicateSummary)
                        put("uniqueArtifactMeaningfulSwapRate", corpusAggregateMetrics.getValue("uniqueArtifactMeaningfulSwapRate"))
                    },
                currentValueText = specialTierPassiveFamilyDuplicateCount.toString(),
                targetText = Phase4OwnerMetricTargets.targetText(specialTierDuplicateMetricId, specialTierDuplicateRange),
                note =
                    "duplicatedZones=${specialTierPassiveFamilyDuplicateSummary.jsonObject.getValue("duplicatedZoneCount").jsonPrimitive.content}; " +
                        "meaningfulSwap=${formatPercent(uniqueArtifactMeaningfulSwapRate)}",
                details = corpusAggregateMetrics,
            )
        val entries = localRewardEvaluation.entries + dynamicPoolEntry + specialTierDuplicateEntry
        val unexpectedRegressionCount = entries.count { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }
        return localRewardEvaluation.copy(
            verdict = if (unexpectedRegressionCount > 0) EvaluationVerdict.FAIL else EvaluationVerdict.PASS,
            passCount = entries.count { entry -> entry.status == EvaluationEntryStatus.PASS },
            approvedDebtCount = entries.count { entry -> entry.status == EvaluationEntryStatus.APPROVED_DEBT },
            expectedFailureCount = entries.count { entry -> entry.status == EvaluationEntryStatus.EXPECTED_FAILURE },
            unexpectedRegressionCount = unexpectedRegressionCount,
            improvedDebtCount = entries.count { entry -> entry.status == EvaluationEntryStatus.IMPROVEMENT },
            entries = entries,
        )
    }

    private fun loadPreflightArtifacts(reportDir: Path): LootPreflightArtifacts =
        LootPreflightArtifacts(
            summary =
                requireNotNull(LootPreflightRunner.readSummary(reportDir)) {
                    "Missing loot preflight summary under $reportDir. Run verifyLootPreflight before whiteBoxLoot."
                },
        )

    private fun LootPreflightPairSummary.toJson(): JsonObject =
        json.parseToJsonElement(json.encodeToString(LootPreflightPairSummary.serializer(), this)).jsonObject

private fun reportDir(): Path {
    val configured = System.getProperty("ktome.phase4.whitebox.loot.reportDir")
    return if (configured.isNullOrBlank()) {
        Path.of("tools", "build", "reports", "phase4", "whitebox", DOMAIN_ID)
    } else {
        Path.of(configured)
    }
}

private fun formatPercent(value: Double): String = String.format(java.util.Locale.US, "%.1f%%", value * 100.0)
}
