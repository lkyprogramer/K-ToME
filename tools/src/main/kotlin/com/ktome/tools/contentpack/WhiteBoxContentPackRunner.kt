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
import kotlinx.serialization.json.putJsonObject

data class WhiteBoxContentPackRun(
    val caseCount: Int,
    val failedAssertions: Int,
    val summaryPath: Path,
    val casesPath: Path,
    val reportPath: Path,
)

object WhiteBoxContentPackRunner {
    const val HARNESS_ID: String = "whiteBoxContentPack"
    private const val DOMAIN_ID: String = "whiteBoxContentPack"
    private const val CORPUS_ID: String = "P4_PR08_CONTENT_PACK_WHITEBOX"

    fun run(): WhiteBoxContentPackRun {
        val kernelRun = ContentPackHarnessRunner.executeKernel()
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val caseReports =
            kernelRun.results.map { result ->
                val joinKey =
                    WhiteBoxJoinKey(
                        seed = result.seedList.firstOrNull(),
                        scenarioId = result.fixtureId,
                    )
                WhiteBoxCaseReport(
                    joinKey = joinKey,
                    facts =
                        buildJsonObject {
                            put("fixtureId", result.fixtureId)
                            put("packId", result.packId)
                            putJsonArray("activePackIds") { result.activePackIds.forEach { packId -> add(kotlinx.serialization.json.JsonPrimitive(packId)) } }
                            putJsonObject("activePackManifestVersions") {
                                result.activePackManifestVersions.forEach { (packId, version) -> put(packId, version) }
                            }
                            putJsonArray("diagnosticCodes") { result.diagnosticCodes.forEach { code -> add(kotlinx.serialization.json.JsonPrimitive(code)) } }
                            putJsonArray("resolvedOrder") { result.resolvedOrder.forEach { packId -> add(kotlinx.serialization.json.JsonPrimitive(packId)) } }
                            putJsonArray("overlayOps") { result.overlayOps.forEach { op -> add(kotlinx.serialization.json.JsonPrimitive(op)) } }
                            put("headlessRunSucceeded", result.headlessRunSucceeded)
                            put("fallbackToBaseVerified", result.fallbackToBaseVerified)
                            put("registryEntryPresent", result.registryEntryPresent)
                        },
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
                            description = "PR-08 content-pack harness cases covering runtime ADD/REPLACE and structured failure fixtures for APPEND, DENY, dependency, version, namespace, and precedence diagnostics.",
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
                                    },
                                assertions =
                                    listOf(
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.expected_failures_accounted_for",
                                            passed = kernelRun.analysis.summary.diagnosticMismatchCount == 0,
                                            message = "All failure fixtures emit the exact structured diagnostics expected by PR-08.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.runtime_add_replace_path",
                                            passed = kernelRun.analysis.summary.successfulRuntimeCaseCount >= 2,
                                            message = "Runtime ADD and whole-entry REPLACE both pass through the formal loader path.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.key_resolution_exact",
                                            passed =
                                                kernelRun.analysis.summary.localeResolutionFailureCount == 0 &&
                                                    kernelRun.analysis.summary.visualResolutionFailureCount == 0 &&
                                                    kernelRun.analysis.summary.audioResolutionFailureCount == 0,
                                            message = "Pack-local i18n / visual / audio keys resolve exactly without fallback or prefix rescue.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.precedence_verified",
                                            passed = kernelRun.analysis.summary.precedenceFailureCount == 0,
                                            message = "Dual-pack precedence and expected non-ADD overlay ops remain explainable.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.headless_run_stable",
                                            passed = kernelRun.analysis.summary.headlessRunFailureCount == 0,
                                            message = "Fixed-seed headless start/save/reload passes for runtime-success fixtures.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.base_fallback_verified",
                                            passed = kernelRun.analysis.summary.fallbackFailureCount == 0,
                                            message = "Disabling the pack falls back to the base registry and asset manifests.",
                                        ),
                                        WhiteBoxAssertionResult(
                                            ruleId = "content-pack.aggregate.no_unexpected_failures",
                                            passed = aggregateFailures.isEmpty(),
                                            message = "No aggregate regressions remain after the PR-08 fixture sweep.",
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
                message = "Harness result matches the expected runtime or failure outcome for this fixture.",
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
                message = "Fallback behavior stays aligned with the base registry and manifests when the pack is disabled.",
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
                artifactId = "merge-trace",
                kind = "merge_trace",
                fileName = "merge-trace.md",
                summary = "Resolved order, active pack versions, registry presence, and exact key resolution facts.",
                content =
                    buildString {
                        appendLine("# Merge Trace")
                        appendLine()
                        appendLine("fixtureId: ${result.fixtureId}")
                        appendLine("packId: ${result.packId}")
                        appendLine("activePackIds: ${result.activePackIds}")
                        appendLine("resolvedOrder: ${result.resolvedOrder}")
                        appendLine("overlayOps: ${result.overlayOps}")
                        appendLine("registryEntryPresent: ${result.registryEntryPresent}")
                        appendLine("registryEntryNameKey: ${result.registryEntryNameKey}")
                        appendLine("registryEntryVisualKey: ${result.registryEntryVisualKey}")
                        appendLine("registryEntryAudioProfile: ${result.registryEntryAudioProfile}")
                        appendLine("localeKeysResolved: ${result.localeKeysResolved}")
                        appendLine("visualKeysResolved: ${result.visualKeysResolved}")
                        appendLine("audioKeysResolved: ${result.audioKeysResolved}")
                    },
                tags = listOf("merge", "content-pack"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "precedence-matrix",
                kind = "precedence_matrix",
                fileName = "precedence-matrix.md",
                summary = "Pack order and precedence verification for the fixture.",
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
                summary = "Structured diagnostics and failure reasons for the fixture.",
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
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "dependency-version-namespace-table",
                kind = "failure_table",
                fileName = "dependency-version-namespace-table.md",
                summary = "Dependency, version, namespace, and fallback state for the fixture.",
                content =
                    buildString {
                        appendLine("# Dependency / Version / Namespace")
                        appendLine()
                        appendLine("activePackManifestVersions: ${result.activePackManifestVersions}")
                        appendLine("seedList: ${result.seedList}")
                        appendLine("headlessRunSucceeded: ${result.headlessRunSucceeded}")
                        appendLine("fallbackToBaseVerified: ${result.fallbackToBaseVerified}")
                    },
                tags = listOf("dependency", "version", "namespace"),
            ),
        )

    private fun reportDir(): Path =
        Path.of(
            System.getProperty("ktome.phase4.whitebox.contentPack.reportDir")
                ?: repoRoot().resolve("tools/build/reports/phase4/whitebox/content-pack").toString(),
        )
}
