package com.ktome.game.harness.whitebox

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.VerificationReportHeader
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxArtifact
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class SharedWhiteBoxWriteRequest(
    val domainId: String,
    val outputDir: Path,
    val header: VerificationReportHeader,
    val corpus: WhiteBoxCorpusSpec,
    val cases: List<WhiteBoxCaseReport>,
    val aggregates: List<WhiteBoxAggregateReport>,
    val retentionPolicy: ArtifactRetentionPolicy = ArtifactRetentionPolicy.ALL,
)

data class SharedWhiteBoxWriteStats(
    val totalAssertions: Int,
    val passedAssertions: Int,
    val failedAssertions: Int,
    val artifactCount: Int,
    val failedCaseCount: Int,
    val failedAggregateCount: Int,
    val firstFailedJoinKey: WhiteBoxJoinKey?,
)

data class SharedWhiteBoxWriteResult(
    val summaryPath: Path,
    val casesPath: Path,
    val reportPath: Path,
    val artifactCount: Int,
    val failedAssertions: Int,
)

object SharedWhiteBoxWriteCore {
    private val uploadUnsafePathCharacters: Set<Char> = setOf('"', ':', '<', '>', '|', '*', '?', '\r', '\n')
    private val prettyJson: Json = Json { prettyPrint = true }
    private val lineJson: Json = Json { prettyPrint = false }

    fun write(
        request: SharedWhiteBoxWriteRequest,
        markdownRenderer: (SharedWhiteBoxWriteStats) -> String,
    ): SharedWhiteBoxWriteResult {
        Files.createDirectories(request.outputDir)

        val summaryPath = request.outputDir.resolve("whitebox-${request.domainId}-summary.json")
        val casesPath = request.outputDir.resolve("whitebox-${request.domainId}-cases.jsonl")
        val reportPath = request.outputDir.resolve("whitebox-${request.domainId}-report.md")
        val stats = summarize(request)

        Files.writeString(
            summaryPath,
            prettyJson.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(request = request, stats = stats),
            ),
        )
        Files.writeString(
            casesPath,
            request.cases.joinToString(separator = "\n") { report ->
                lineJson.encodeToString(WhiteBoxCaseReport.serializer(), report)
            } + "\n",
        )
        Files.writeString(reportPath, markdownRenderer(stats))

        return SharedWhiteBoxWriteResult(
            summaryPath = summaryPath,
            casesPath = casesPath,
            reportPath = reportPath,
            artifactCount = stats.artifactCount,
            failedAssertions = stats.failedAssertions,
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
        val artifactsRoot = outputDir.resolve("artifacts")
        deleteUploadUnsafeArtifactDirectories(artifactsRoot)
        val artifactDir = artifactsRoot.resolve(caseDirName(joinKey))
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
        assertions: List<WhiteBoxAssertionResult>,
        sampleCaseKeys: Set<WhiteBoxJoinKey> = emptySet(),
    ): Boolean =
        when (retentionPolicy) {
            ArtifactRetentionPolicy.ALL -> true
            ArtifactRetentionPolicy.FAILURES_PLUS_SAMPLES ->
                assertions.any { assertion -> !assertion.passed } || joinKey in sampleCaseKeys
            ArtifactRetentionPolicy.SUMMARY_ONLY -> false
        }

    private fun summarize(request: SharedWhiteBoxWriteRequest): SharedWhiteBoxWriteStats {
        val totalAssertions =
            request.cases.sumOf { report -> report.assertions.size } +
                request.aggregates.sumOf { report -> report.assertions.size }
        val failedAssertions =
            request.cases.sumOf { report -> report.assertions.count { assertion -> !assertion.passed } } +
                request.aggregates.sumOf { report -> report.assertions.count { assertion -> !assertion.passed } }
        val failedCaseCount = request.cases.count { report -> report.assertions.any { assertion -> !assertion.passed } }
        val failedAggregateCount =
            request.aggregates.count { report -> report.assertions.any { assertion -> !assertion.passed } }
        return SharedWhiteBoxWriteStats(
            totalAssertions = totalAssertions,
            passedAssertions = totalAssertions - failedAssertions,
            failedAssertions = failedAssertions,
            artifactCount = request.cases.sumOf { report -> report.artifacts.size },
            failedCaseCount = failedCaseCount,
            failedAggregateCount = failedAggregateCount,
            firstFailedJoinKey =
                request.cases.firstOrNull { report -> report.assertions.any { assertion -> !assertion.passed } }?.joinKey,
        )
    }

    private fun buildSummaryPayload(
        request: SharedWhiteBoxWriteRequest,
        stats: SharedWhiteBoxWriteStats,
    ): JsonObject =
        buildJsonObject {
            put("domainId", request.domainId)
            put("verdict", if (stats.failedAssertions == 0) "PASS" else "FAIL")
            put("failedCaseCount", stats.failedCaseCount)
            put("failedAggregateCount", stats.failedAggregateCount)
            put(
                "firstFailedJoinKey",
                stats.firstFailedJoinKey?.let { joinKey ->
                    prettyJson.encodeToJsonElement(WhiteBoxJoinKey.serializer(), joinKey)
                } ?: JsonNull,
            )
            put("header", prettyJson.encodeToJsonElement(VerificationReportHeader.serializer(), request.header))
            put("corpus", prettyJson.encodeToJsonElement(WhiteBoxCorpusSpec.serializer(), request.corpus))
            putJsonObject("summary") {
                put("caseCount", request.cases.size)
                put("aggregateCount", request.aggregates.size)
                put("totalAssertions", stats.totalAssertions)
                put("passedAssertions", stats.passedAssertions)
                put("failedAssertions", stats.failedAssertions)
                put("caseFailureCount", stats.failedCaseCount)
                put("aggregateFailureCount", stats.failedAggregateCount)
                put("artifactCount", stats.artifactCount)
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

    private fun deleteUploadUnsafeArtifactDirectories(artifactsRoot: Path) {
        if (!Files.isDirectory(artifactsRoot)) return
        Files.list(artifactsRoot).use { paths ->
            paths
                .filter { path -> path.fileName.toString().any(uploadUnsafePathCharacters::contains) }
                .forEach(::deleteRecursively)
        }
    }

    private fun deleteRecursively(path: Path) {
        Files.walk(path).use { paths ->
            paths
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        }
    }
}
