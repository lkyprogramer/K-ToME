package com.ktome.tools.whitebox

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.VerificationReportHeader
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxArtifact
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

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
    private val prettyJson: Json = Json { prettyPrint = true }
    private val lineJson: Json = Json { prettyPrint = false }

    fun write(request: WhiteBoxDomainWriteRequest): WhiteBoxWriteResult {
        Files.createDirectories(request.outputDir)

        val summaryPath = request.outputDir.resolve("whitebox-${request.domainId}-summary.json")
        val casesPath = request.outputDir.resolve("whitebox-${request.domainId}-cases.jsonl")
        val reportPath = request.outputDir.resolve("whitebox-${request.domainId}-report.md")
        val totalAssertions =
            request.cases.sumOf { report -> report.assertions.size } +
                request.aggregates.sumOf { report -> report.assertions.size }
        val failedAssertions =
            request.cases.sumOf { report -> report.assertions.count { assertion -> !assertion.passed } } +
                request.aggregates.sumOf { report -> report.assertions.count { assertion -> !assertion.passed } }
        val passedAssertions = totalAssertions - failedAssertions
        val artifactCount = request.cases.sumOf { report -> report.artifacts.size }
        val failedCaseCount = request.cases.count { report -> report.assertions.any { assertion -> !assertion.passed } }
        val failedAggregateCount = request.aggregates.count { report -> report.assertions.any { assertion -> !assertion.passed } }
        val firstFailedJoinKey =
            request.cases.firstOrNull { report -> report.assertions.any { assertion -> !assertion.passed } }?.joinKey
        Files.writeString(
            summaryPath,
            prettyJson.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(
                    request = request,
                    totalAssertions = totalAssertions,
                    passedAssertions = passedAssertions,
                    failedAssertions = failedAssertions,
                    artifactCount = artifactCount,
                    failedCaseCount = failedCaseCount,
                    failedAggregateCount = failedAggregateCount,
                    firstFailedJoinKey = firstFailedJoinKey,
                ),
            ),
        )
        Files.writeString(
            casesPath,
            request.cases.joinToString(separator = "\n") { report ->
                lineJson.encodeToString(WhiteBoxCaseReport.serializer(), report)
            } + "\n",
        )
        Files.writeString(
            reportPath,
            WhiteBoxMarkdownRenderer.render(
                domainId = request.domainId,
                corpus = request.corpus,
                cases = request.cases,
                aggregates = request.aggregates,
                totalAssertions = totalAssertions,
                failedAssertions = failedAssertions,
                artifactCount = artifactCount,
            ),
        )
        return WhiteBoxWriteResult(
            summaryPath = summaryPath,
            casesPath = casesPath,
            reportPath = reportPath,
            artifactCount = artifactCount,
            failedAssertions = failedAssertions,
        )
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
    ): WhiteBoxArtifact {
        val artifactDir = outputDir.resolve("artifacts").resolve(caseDirName(joinKey))
        Files.createDirectories(artifactDir)
        val artifactPath = artifactDir.resolve(fileName)
        Files.writeString(artifactPath, content)
        val relativePath = outputDir.relativize(artifactPath).toString().replace('\\', '/')
        return WhiteBoxArtifact(
            artifactId = artifactId,
            kind = kind,
            format = fileName.substringAfterLast('.', missingDelimiterValue = "txt"),
            relativePath = relativePath,
            summary = summary,
            tags = tags,
        )
    }

    fun shouldWriteArtifacts(
        retentionPolicy: ArtifactRetentionPolicy,
        joinKey: WhiteBoxJoinKey,
        assertions: List<com.ktome.core.harness.whitebox.WhiteBoxAssertionResult>,
        sampleCaseKeys: Set<WhiteBoxJoinKey> = emptySet(),
    ): Boolean =
        when (retentionPolicy) {
            ArtifactRetentionPolicy.ALL -> true
            ArtifactRetentionPolicy.FAILURES_PLUS_SAMPLES ->
                assertions.any { assertion -> !assertion.passed } || joinKey in sampleCaseKeys
            ArtifactRetentionPolicy.SUMMARY_ONLY -> false
        }

    private fun buildSummaryPayload(
        request: WhiteBoxDomainWriteRequest,
        totalAssertions: Int,
        passedAssertions: Int,
        failedAssertions: Int,
        artifactCount: Int,
        failedCaseCount: Int,
        failedAggregateCount: Int,
        firstFailedJoinKey: WhiteBoxJoinKey?,
    ): JsonObject =
        buildJsonObject {
            put("domainId", request.domainId)
            put("verdict", if (failedAssertions == 0) "PASS" else "FAIL")
            put("failedCaseCount", failedCaseCount)
            put("failedAggregateCount", failedAggregateCount)
            put(
                "firstFailedJoinKey",
                firstFailedJoinKey?.let { joinKey ->
                    prettyJson.encodeToJsonElement(WhiteBoxJoinKey.serializer(), joinKey)
                } ?: JsonNull,
            )
            put("header", prettyJson.encodeToJsonElement(VerificationReportHeader.serializer(), request.header))
            put("corpus", prettyJson.encodeToJsonElement(WhiteBoxCorpusSpec.serializer(), request.corpus))
            putJsonObject("summary") {
                put("caseCount", request.cases.size)
                put("aggregateCount", request.aggregates.size)
                put("totalAssertions", totalAssertions)
                put("passedAssertions", passedAssertions)
                put("failedAssertions", failedAssertions)
                put("caseFailureCount", failedCaseCount)
                put("aggregateFailureCount", failedAggregateCount)
                put("artifactCount", artifactCount)
                put("retentionPolicy", request.retentionPolicy.name)
            }
            putJsonArray("aggregates") {
                request.aggregates.forEach { aggregate ->
                    add(prettyJson.encodeToJsonElement(WhiteBoxAggregateReport.serializer(), aggregate))
                }
            }
        }

    private fun caseDirName(joinKey: WhiteBoxJoinKey): String =
        buildList {
            joinKey.scenarioId?.let { scenarioId -> add("scenario-${sanitize(scenarioId)}") }
            joinKey.zoneId?.let { zoneId -> add("zone-${sanitize(zoneId)}") }
            joinKey.floorIndex?.let { floorIndex -> add("floor-${floorIndex}") }
            joinKey.seed?.let { seed -> add("seed-${seed}") }
        }.joinToString(separator = "__").ifBlank { "case" }

    private fun sanitize(value: String): String =
        value.map { ch ->
            when {
                ch.isLetterOrDigit() -> ch
                ch == '.' || ch == '-' || ch == '_' -> ch
                else -> '_'
            }
        }.joinToString(separator = "")
}
