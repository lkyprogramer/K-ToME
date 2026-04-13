package com.ktome.tools.whitebox

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.VerificationReportHeader
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxArtifact
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.game.harness.whitebox.SharedWhiteBoxWriteCore
import com.ktome.game.harness.whitebox.SharedWhiteBoxWriteRequest
import com.ktome.game.harness.whitebox.SharedWhiteBoxWriteResult
import java.nio.file.Path

internal data class WhiteBoxDomainWriteRequest(
    val domainId: String,
    val outputDir: Path,
    val header: VerificationReportHeader,
    val corpus: WhiteBoxCorpusSpec,
    val cases: List<WhiteBoxCaseReport>,
    val aggregates: List<WhiteBoxAggregateReport>,
    val retentionPolicy: ArtifactRetentionPolicy = ArtifactRetentionPolicy.ALL,
)

internal data class WhiteBoxWriteResult(
    val summaryPath: Path,
    val casesPath: Path,
    val reportPath: Path,
    val artifactCount: Int,
    val failedAssertions: Int,
)

internal object WhiteBoxReportWriter {
    fun write(request: WhiteBoxDomainWriteRequest): WhiteBoxWriteResult {
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
                        retentionPolicy = request.retentionPolicy,
                    ),
                markdownRenderer = { stats ->
                    WhiteBoxMarkdownRenderer.render(
                        domainId = request.domainId,
                        corpus = request.corpus,
                        cases = request.cases,
                        aggregates = request.aggregates,
                        totalAssertions = stats.totalAssertions,
                        failedAssertions = stats.failedAssertions,
                        artifactCount = stats.artifactCount,
                    )
                },
            )
        return result.toToolsWriteResult()
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

    fun shouldWriteArtifacts(
        retentionPolicy: ArtifactRetentionPolicy,
        joinKey: WhiteBoxJoinKey,
        assertions: List<com.ktome.core.harness.whitebox.WhiteBoxAssertionResult>,
        sampleCaseKeys: Set<WhiteBoxJoinKey> = emptySet(),
    ): Boolean =
        SharedWhiteBoxWriteCore.shouldWriteArtifacts(
            retentionPolicy = retentionPolicy,
            joinKey = joinKey,
            assertions = assertions,
            sampleCaseKeys = sampleCaseKeys,
        )
}

private fun SharedWhiteBoxWriteResult.toToolsWriteResult(): WhiteBoxWriteResult =
    WhiteBoxWriteResult(
        summaryPath = summaryPath,
        casesPath = casesPath,
        reportPath = reportPath,
        artifactCount = artifactCount,
        failedAssertions = failedAssertions,
    )
