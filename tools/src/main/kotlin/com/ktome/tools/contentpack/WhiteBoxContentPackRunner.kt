package com.ktome.tools.contentpack

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxArtifact
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.game.contentpack.repoRoot
import com.ktome.tools.whitebox.WhiteBoxDomainWriteRequest
import com.ktome.tools.whitebox.WhiteBoxReportWriter
import com.ktome.tools.whitebox.toVerificationReportHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class WhiteBoxContentPackRun(
    val caseCount: Int,
    val failedAssertions: Int,
    val summaryPath: Path,
    val casesPath: Path,
    val reportPath: Path,
)

object WhiteBoxContentPackRunner {
    const val HARNESS_ID: String = "whiteBoxContentPack"
    private const val DOMAIN_ID: String = "content-pack"
    private const val CORPUS_ID: String = "P4_PR09_SAMPLE_CONTENT_PACK_WHITEBOX"

    fun run(): WhiteBoxContentPackRun {
        val kernelRun = ContentPackHarnessRunner.executeKernel()
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        deleteLegacyArtifacts(outputDir)
        val caseReports =
            kernelRun.results.map { result ->
                val joinKey =
                    WhiteBoxJoinKey(
                        seed = result.seedList.firstOrNull(),
                        scenarioId = result.fixtureId,
                    )
                WhiteBoxCaseReport(
                    joinKey = joinKey,
                    facts = result.toFactsJson(),
                    fingerprints =
                        mapOf(
                            "fixtureId" to result.fixtureId,
                            "packId" to (result.packId ?: "none"),
                            "diagnosticCodes" to result.diagnosticCodes.sorted().joinToString(separator = ",").ifBlank { "none" },
                        ),
                    assertions = caseAssertions(result),
                    artifacts = writeArtifacts(outputDir = outputDir, joinKey = joinKey, result = result),
                )
            }
        val aggregateFailures = kernelRun.analysis.aggregateFailures
        val result =
            WhiteBoxReportWriter.write(
                WhiteBoxDomainWriteRequest(
                    domainId = DOMAIN_ID,
                    outputDir = outputDir,
                    header = kernelRun.header.toVerificationReportHeader(corpusId = CORPUS_ID).copy(harnessId = HARNESS_ID),
                    corpus =
                        WhiteBoxCorpusSpec(
                            corpusId = CORPUS_ID,
                            description =
                                "PR-09 content-pack white-box corpus covering the official sample pack, disabled fallback, precedence fixture, structured failure fixtures, merged asset keys, resource contract checks, and fixed-seed sample reward traces.",
                            sampleCount = kernelRun.results.size,
                        ),
                    cases = caseReports,
                    aggregates =
                        listOf(
                            WhiteBoxAggregateReport(
                                groupId = "corpus",
                                sampleCount = kernelRun.results.size,
                                metrics =
                                    buildJsonObject {
                                        put("successfulRuntimeCaseCount", kernelRun.analysis.summary.successfulRuntimeCaseCount)
                                        put("expectedFailureCaseCount", kernelRun.analysis.summary.expectedFailureCaseCount)
                                        put("diagnosticMismatchCount", kernelRun.analysis.summary.diagnosticMismatchCount)
                                        put("localeResolutionFailureCount", kernelRun.analysis.summary.localeResolutionFailureCount)
                                        put("visualResolutionFailureCount", kernelRun.analysis.summary.visualResolutionFailureCount)
                                        put("audioResolutionFailureCount", kernelRun.analysis.summary.audioResolutionFailureCount)
                                        put("headlessRunFailureCount", kernelRun.analysis.summary.headlessRunFailureCount)
                                        put("fallbackFailureCount", kernelRun.analysis.summary.fallbackFailureCount)
                                        put("precedenceFailureCount", kernelRun.analysis.summary.precedenceFailureCount)
                                        put("resourceContractFailureCount", kernelRun.analysis.summary.resourceContractFailureCount)
                                        put("generatedTemplateFailureCount", kernelRun.analysis.summary.generatedTemplateFailureCount)
                                        put("legacyLootProfileSchemaRejectCount", kernelRun.analysis.summary.legacyLootProfileSchemaRejectCount)
                                        putJsonArray("legacyLootProfileSchemaRejectSummaries") {
                                            kernelRun.analysis.summary.legacyLootProfileSchemaRejectSummaries.forEach { summary ->
                                                add(summary.toJson())
                                            }
                                        }
                                    },
                                assertions =
                                    listOf(
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.expected_failures_accounted_for",
                                            passed = kernelRun.analysis.summary.diagnosticMismatchCount == 0,
                                            message = "All PR-09 failure fixtures emit the exact structured diagnostics documented by the content-pack contract.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.runtime_sample_pack_path",
                                            passed = kernelRun.analysis.summary.successfulRuntimeCaseCount >= 4,
                                            message =
                                                "Official sample pack, disabled fallback, precedence fixture, and split-bias fixture all execute through the formal loader path.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.key_resolution_exact",
                                            passed =
                                                kernelRun.analysis.summary.localeResolutionFailureCount == 0 &&
                                                    kernelRun.analysis.summary.visualResolutionFailureCount == 0 &&
                                                    kernelRun.analysis.summary.audioResolutionFailureCount == 0,
                                            message = "Merged pack-local i18n / visual / audio keys resolve exactly without fallback or prefix rescue.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.resource_contract_verified",
                                            passed = kernelRun.analysis.summary.resourceContractFailureCount == 0,
                                            message = "PR-09 sample-pack plans, manifests, reports, and pack-root files stay aligned.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.generated_template_present",
                                            passed = kernelRun.analysis.summary.generatedTemplateFailureCount == 0,
                                            message = "Fixed-seed sample reward traces still reach sample_flooded_relics special templates.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.precedence_verified",
                                            passed = kernelRun.analysis.summary.precedenceFailureCount == 0,
                                            message = "Dual-pack precedence and expected non-ADD overlay ops remain explainable.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.legacy_loot_profile_schema_rejected",
                                            passed = kernelRun.analysis.summary.legacyLootProfileSchemaRejectCount == 1,
                                            message = "Legacy V2 loot profiles are rejected by the runtime loader with the dedicated schema mismatch diagnostic.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.headless_run_stable",
                                            passed = kernelRun.analysis.summary.headlessRunFailureCount == 0,
                                            message = "Fixed-seed headless start/save/reload passes for runtime-success fixtures.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.base_fallback_verified",
                                            passed = kernelRun.analysis.summary.fallbackFailureCount == 0,
                                            message = "Disabling the official sample pack falls back to the base underground river secret zone.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.no_unexpected_failures",
                                            passed = aggregateFailures.isEmpty(),
                                            message = "No aggregate regressions remain after the PR-09 content-pack sweep.",
                                        ),
                                    ),
                            ),
                        ),
                    retentionPolicy = ArtifactRetentionPolicy.ALL,
                ),
            )
        return WhiteBoxContentPackRun(
            caseCount = caseReports.size,
            failedAssertions = result.failedAssertions,
            summaryPath = result.summaryPath,
            casesPath = result.casesPath,
            reportPath = result.reportPath,
        )
    }

    private fun caseAssertions(result: ContentPackCaseResult): List<WhiteBoxAssertionResult> =
        listOf(
            WhiteBoxAssertionResult(
                ruleId = "content-pack.case.expected_outcome_matched",
                passed = result.success,
                message = "Harness result matches the expected runtime or failure outcome for this PR-09 fixture.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "content-pack.case.precedence_verified",
                passed = result.precedenceVerified,
                message = "Resolved pack order and non-ADD overlay ops match the fixture contract.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "content-pack.case.key_resolution_exact",
                passed =
                    result.localeResolutionFailureCount == 0 &&
                        result.visualResolutionFailureCount == 0 &&
                        result.audioResolutionFailureCount == 0,
                message = "Pack-local locale and asset keys resolve exactly for this fixture.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "content-pack.case.headless_run_stable",
                passed = result.seedList.isEmpty() || result.headlessRunSucceeded,
                message = "Headless start/save/reload succeeds for fixtures that declare deterministic seeds.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "content-pack.case.base_fallback_verified",
                passed = !result.failureReasons.contains("case.base_fallback_not_verified"),
                message = "Fallback behavior stays aligned with the base secret-zone contract when the pack is disabled.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "content-pack.case.resource_contract_verified",
                passed = result.resourceContractVerified,
                message = "Sample-pack plan/manifests/files/reports stay internally consistent for this fixture.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "content-pack.case.generated_template_present",
                passed =
                    result.generatedSpecialTemplateIds.isEmpty() ||
                        result.generatedSpecialTemplateIds.any { templateId -> templateId.startsWith("sample.flooded_relics.") },
                message = "Sample reward trace reaches at least one sample_flooded_relics special template when requested.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "content-pack.case.diagnostics_structured",
                passed = !result.failureReasons.contains("case.diagnostic_code_mismatch"),
                message = "Failure fixtures emit the documented structured diagnostic codes.",
            ),
        )

    private fun writeArtifacts(
        outputDir: Path,
        joinKey: WhiteBoxJoinKey,
        result: ContentPackCaseResult,
    ): List<WhiteBoxArtifact> =
        listOf(
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "pack-manifest-resolve",
                kind = "pack_manifest_resolve",
                fileName = "pack-manifest-resolve.md",
                summary = "Resolved packs plus merged secret-zone / hidden-event / loot / special-template facts.",
                content =
                    buildString {
                        appendLine("# Pack Manifest Resolve")
                        appendLine()
                        appendLine("fixtureId: ${result.fixtureId}")
                        appendLine("packId: ${result.packId}")
                        appendLine("activePackIds: ${result.activePackIds}")
                        appendLine("activePackManifestVersions: ${result.activePackManifestVersions}")
                        appendLine("resolvedOrder: ${result.resolvedOrder}")
                        appendLine("overlayOps: ${result.overlayOps}")
                        appendLine("secretZonePresent: ${result.secretZonePresent}")
                        appendLine("secretZoneNameKey: ${result.secretZoneNameKey}")
                        appendLine("secretZoneVisualKey: ${result.secretZoneVisualKey}")
                        appendLine("secretZoneAudioProfile: ${result.secretZoneAudioProfile}")
                        appendLine("hiddenEventPresent: ${result.hiddenEventPresent}")
                        appendLine("hiddenEventLootProfileId: ${result.hiddenEventLootProfileId}")
                        appendLine("lootProfilePresent: ${result.lootProfilePresent}")
                        appendLine("lootProfileRewardBudget: ${result.lootProfileRewardBudget}")
                        appendLine("lootProfilePoolStrategy: ${result.lootProfilePoolStrategy}")
                        appendLine("lootProfileItemIds: ${result.lootProfileItemIds}")
                        appendLine("lootProfileItemTagFilter: ${result.lootProfileItemTagFilter}")
                        appendLine("lootProfileExcludeIds: ${result.lootProfileExcludeIds}")
                        appendLine("lootProfileTypeWeights: ${result.lootProfileTypeWeights}")
                        appendLine("lootProfileSlotBias: ${result.lootProfileSlotBias}")
                        appendLine("lootProfileSpecialTemplateTagPreference: ${result.lootProfileSpecialTemplateTagPreference}")
                        appendLine("lootProfileAffixTagPreference: ${result.lootProfileAffixTagPreference}")
                        appendLine("specialTemplateIds: ${result.specialTemplateIds}")
                    },
                tags = listOf("manifest", "merge", "content-pack"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "merged-key-summary",
                kind = "merged_key_summary",
                fileName = "merged-key-summary.md",
                summary = "Merged locale/visual/audio key resolution and resource contract details.",
                content =
                    buildString {
                        appendLine("# Merged Key Summary")
                        appendLine()
                        appendLine("localeKeysResolved: ${result.localeKeysResolved}")
                        appendLine("visualKeysResolved: ${result.visualKeysResolved}")
                        appendLine("audioKeysResolved: ${result.audioKeysResolved}")
                        appendLine("localeResolutionFailureCount: ${result.localeResolutionFailureCount}")
                        appendLine("visualResolutionFailureCount: ${result.visualResolutionFailureCount}")
                        appendLine("audioResolutionFailureCount: ${result.audioResolutionFailureCount}")
                        appendLine("resourceContractVerified: ${result.resourceContractVerified}")
                        appendLine("resourceContractDetails: ${result.resourceContractDetails}")
                    },
                tags = listOf("keys", "manifest", "assets"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "headless-run-summary",
                kind = "headless_run_summary",
                fileName = "headless-run-summary.md",
                summary = "Fixed-seed save/load summary and generated sample reward template ids.",
                content =
                    buildString {
                        appendLine("# Headless Run Summary")
                        appendLine()
                        appendLine("seedList: ${result.seedList}")
                        appendLine("headlessRunSucceeded: ${result.headlessRunSucceeded}")
                        appendLine("generatedSpecialTemplateIds: ${result.generatedSpecialTemplateIds}")
                        appendLine("fallbackToBaseVerified: ${result.fallbackToBaseVerified}")
                    },
                tags = listOf("headless", "seed", "reward"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "precedence-matrix",
                kind = "precedence_matrix",
                fileName = "precedence-matrix.md",
                summary = "Pack order, overlay op chain, and precedence verdict for the fixture.",
                content =
                    buildString {
                        appendLine("# Precedence Matrix")
                        appendLine()
                        appendLine("fixtureId: ${result.fixtureId}")
                        appendLine("resolvedOrder: ${result.resolvedOrder}")
                        appendLine("overlayOps: ${result.overlayOps}")
                        appendLine("precedenceVerified: ${result.precedenceVerified}")
                    },
                tags = listOf("precedence", "matrix"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "lint-diagnostics",
                kind = "lint_diagnostics",
                fileName = "lint-diagnostics.md",
                summary = "Expected failure codes, actual diagnostics, and case-level failure reasons.",
                content =
                    buildString {
                        appendLine("# Lint Diagnostics")
                        appendLine()
                        appendLine("expectedFailureCodes: ${result.expectedFailureCodes}")
                        appendLine("diagnosticCodes: ${result.diagnosticCodes}")
                        appendLine("diagnostics: ${result.diagnostics}")
                        appendLine("failureReasons: ${result.failureReasons}")
                    },
                tags = listOf("diagnostics", "lint"),
            ),
        )

    private fun reportDir(): Path =
        Path.of(
            System.getProperty("ktome.phase4.whitebox.contentPack.reportDir")
                ?: repoRoot().resolve("tools/build/reports/phase4/whitebox/content-pack").toString(),
        )

    private fun deleteLegacyArtifacts(outputDir: Path) {
        listOf(
            "whitebox-whiteBoxContentPack-summary.json",
            "whitebox-whiteBoxContentPack-cases.jsonl",
            "whitebox-whiteBoxContentPack-report.md",
        ).forEach { fileName ->
            Files.deleteIfExists(outputDir.resolve(fileName))
        }
    }
}
