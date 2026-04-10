package com.ktome.tools.loot

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.tools.mapgen.phase4HarnessHeader
import com.ktome.tools.whitebox.WhiteBoxDomainWriteRequest
import com.ktome.tools.whitebox.WhiteBoxReportWriter
import com.ktome.tools.whitebox.toVerificationReportHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
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

object WhiteBoxLootRunner {
    const val HARNESS_ID: String = "whiteBoxLoot"
    private const val DOMAIN_ID: String = "loot"
    private const val CORPUS_ID: String = "P4_PR05_LOOT_WHITEBOX"

    fun run(): WhiteBoxLootRun {
        val outputDir = reportDir()
        Files.createDirectories(outputDir)

        val kernelRun = LootLabKernel.execute()
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
                    metrics = corpusMetrics(kernelRun),
                    assertions = corpusAssertions(kernelRun),
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
        return WhiteBoxLootRun(
            caseCount = caseReports.size,
            failedAssertions = result.failedAssertions,
            summaryPath = result.summaryPath,
            casesPath = result.casesPath,
            reportPath = result.reportPath,
        )
    }

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

    private fun corpusAssertions(kernelRun: LootKernelRun): List<WhiteBoxAssertionResult> =
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
                passed = kernelRun.profileOverlapSummary.averageOverlap < 0.30,
                message = "Average loot-profile base item overlap stays below the OPT PR-04 exit threshold.",
                context = kernelRun.profileOverlapSummary.toJson(),
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.max_overlap_sanity",
                passed = kernelRun.profileOverlapSummary.maxOverlap < 0.95,
                message = "No loot-profile base item pool remains a near-total subset of another profile.",
                context = kernelRun.profileOverlapSummary.toJson(),
            ),
            WhiteBoxAssertionResult(
                ruleId = "loot.aggregate.passive_coverage",
                passed = kernelRun.passiveCoverageSummary.coverageRatio >= 0.80,
                message = "Affix passive coverage stays at or above the OPT PR-03 threshold.",
                context = kernelRun.passiveCoverageSummary.toJson(),
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

    private fun corpusMetrics(kernelRun: LootKernelRun): JsonObject =
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
            put("lootProfileAverageBaseItemOverlap", kernelRun.profileOverlapSummary.averageOverlap)
            put("lootProfileMaxBaseItemOverlap", kernelRun.profileOverlapSummary.maxOverlap)
            put("lootProfileDistinctBaseItemCount", kernelRun.profileOverlapSummary.distinctBaseItemCount)
            put("lootProfileBaseItemOverlapMatrix", kernelRun.profileOverlapSummary.toJson().getValue("matrix"))
            put("affixPassiveCoverage", kernelRun.passiveCoverageSummary.coverageRatio)
            putJsonArray("affixPassiveKinds") {
                kernelRun.passiveCoverageSummary.passiveKinds.sorted().forEach { passiveKind -> add(kotlinx.serialization.json.JsonPrimitive(passiveKind)) }
            }
            put("uniqueArtifactOutcomeCount", kernelRun.uniqueArtifactOutcomeCount)
            put("meaningfulUniqueArtifactSwapCount", kernelRun.meaningfulUniqueArtifactSwapCount)
            put("uniqueArtifactMeaningfulSwapRate", kernelRun.uniqueArtifactMeaningfulSwapRate)
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

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.whitebox.loot.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4", "whitebox", DOMAIN_ID)
        } else {
            Path.of(configured)
        }
    }
}
