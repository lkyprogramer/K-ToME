package com.ktome.game.harness

import com.ktome.core.harness.whitebox.VerificationReportHeader
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxArtifact
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.game.harness.whitebox.SharedWhiteBoxWriteCore
import com.ktome.game.harness.whitebox.SharedWhiteBoxWriteRequest
import com.ktome.game.harness.whitebox.SharedWhiteBoxWriteResult
import java.nio.file.Path

internal data class WhiteBoxHarnessWriteRequest(
    val domainId: String,
    val outputDir: Path,
    val header: VerificationReportHeader,
    val corpus: WhiteBoxCorpusSpec,
    val cases: List<WhiteBoxCaseReport>,
    val aggregates: List<WhiteBoxAggregateReport>,
)

internal data class WhiteBoxHarnessWriteResult(
    val summaryPath: Path,
    val casesPath: Path,
    val reportPath: Path,
    val failedAssertions: Int,
    val artifactCount: Int,
)

internal object WhiteBoxHarnessWriter {
    fun write(request: WhiteBoxHarnessWriteRequest): WhiteBoxHarnessWriteResult {
        val result =
            SharedWhiteBoxWriteCore.write(
                request =
                    SharedWhiteBoxWriteRequest(
                        domainId = request.domainId,
                        outputDir = request.outputDir,
                        header = request.header,
                        corpus = request.corpus,
                        cases = request.cases,
                        aggregates = request.aggregates,
                    ),
                markdownRenderer = { renderMarkdown(request) },
            )
        return result.toHarnessWriteResult()
    }

    fun writeTextArtifact(
        outputDir: Path,
        joinKey: WhiteBoxJoinKey,
        artifactId: String,
        kind: String,
        fileName: String,
        summary: String? = null,
        content: String,
        tags: List<String> = emptyList(),
    ): WhiteBoxArtifact =
        SharedWhiteBoxWriteCore.writeTextArtifact(
            outputDir = outputDir,
            joinKey = joinKey,
            artifactId = artifactId,
            kind = kind,
            fileName = fileName,
            summary = summary,
            content = content,
            tags = tags,
        )

    private fun renderMarkdown(request: WhiteBoxHarnessWriteRequest): String {
        val failedAssertions =
            request.cases.sumOf { report -> report.assertions.count { assertion -> !assertion.passed } } +
                request.aggregates.sumOf { report -> report.assertions.count { assertion -> !assertion.passed } }
        return buildString {
            appendLine("# White-Box ${request.domainId}")
            appendLine("- verdict: ${if (failedAssertions == 0) "PASS" else "FAIL"}")
            appendLine("- corpusId: ${request.corpus.corpusId}")
            appendLine("- caseCount: ${request.cases.size}")
            appendLine("- aggregateCount: ${request.aggregates.size}")
            appendLine("- artifactCount: ${request.cases.sumOf { it.artifacts.size }}")
            appendLine()
            appendLine("## Cases")
            request.cases.forEach { report ->
                appendLine("- joinKey: ${joinKeyLabel(report.joinKey)}")
                report.assertions.forEach { assertion ->
                    appendLine("  - ${if (assertion.passed) "PASS" else "FAIL"} ${assertion.ruleId}: ${assertion.message}")
                }
            }
            appendLine()
            appendLine("## Aggregates")
            request.aggregates.forEach { aggregate ->
                appendLine("- groupId: ${aggregate.groupId} (${aggregate.sampleCount})")
                aggregate.assertions.forEach { assertion ->
                    appendLine("  - ${if (assertion.passed) "PASS" else "FAIL"} ${assertion.ruleId}: ${assertion.message}")
                }
            }
        }
    }
}

private fun SharedWhiteBoxWriteResult.toHarnessWriteResult(): WhiteBoxHarnessWriteResult =
    WhiteBoxHarnessWriteResult(
        summaryPath = summaryPath,
        casesPath = casesPath,
        reportPath = reportPath,
        failedAssertions = failedAssertions,
        artifactCount = artifactCount,
    )

private fun joinKeyLabel(joinKey: WhiteBoxJoinKey): String =
    listOfNotNull(
        joinKey.scenarioId?.let { "scenario=$it" },
        joinKey.zoneId?.let { "zone=$it" },
        joinKey.floorIndex?.let { "floor=$it" },
        joinKey.seed?.let { "seed=$it" },
    ).joinToString(separator = ", ")

internal fun whiteBoxSummaryReportDir(
    propertyKey: String,
    domainId: String,
): Path {
    val configured = System.getProperty(propertyKey)
    return if (configured.isNullOrBlank()) {
        Path.of("tools", "build", "reports", "phase4", "whitebox", domainId)
    } else {
        Path.of(configured)
    }
}

internal fun whiteBoxPhase4Header(
    harnessId: String,
    corpusId: String,
    contractVersions: List<Pair<String, String>>,
    seeds: List<Long>,
): VerificationReportHeader =
    VerificationReportHeader(
        harnessId = harnessId,
        phaseId = "P4",
        buildId = HarnessMetadata.BUILD_ID,
        locale = "en-US",
        corpusId = corpusId,
        timestamp = java.time.Instant.now().toString(),
        activePackIds = emptyList(),
        activePackManifestVersions = emptyMap(),
        contractVersions =
            contractVersions.map { (contractId, version) ->
                com.ktome.core.harness.whitebox.ContractVersionStamp(contractId = contractId, version = version)
            },
        seedList = seeds,
    )

internal fun failedAssertionCount(assertions: List<WhiteBoxAssertionResult>): Int =
    assertions.count { assertion -> !assertion.passed }
