package com.ktome.game.harness

import com.ktome.core.harness.whitebox.VerificationReportHeader
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxArtifact
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

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
    private val prettyJson: Json = Json { prettyPrint = true }
    private val lineJson: Json = Json { prettyPrint = false }

    fun write(request: WhiteBoxHarnessWriteRequest): WhiteBoxHarnessWriteResult {
        Files.createDirectories(request.outputDir)
        val summaryPath = request.outputDir.resolve("whitebox-${request.domainId}-summary.json")
        val casesPath = request.outputDir.resolve("whitebox-${request.domainId}-cases.jsonl")
        val reportPath = request.outputDir.resolve("whitebox-${request.domainId}-report.md")
        val totalAssertions = request.cases.sumOf { it.assertions.size } + request.aggregates.sumOf { it.assertions.size }
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
                        put("artifactCount", artifactCount)
                        put("caseFailureCount", failedCaseCount)
                        put("aggregateFailureCount", failedAggregateCount)
                        put("retentionPolicy", "ALL")
                    }
                    putJsonArray("aggregates") {
                        request.aggregates.forEach { aggregate ->
                            add(prettyJson.encodeToJsonElement(WhiteBoxAggregateReport.serializer(), aggregate))
                        }
                    }
                },
            ),
        )
        Files.writeString(
            casesPath,
            request.cases.joinToString(separator = "\n") { report ->
                lineJson.encodeToString(WhiteBoxCaseReport.serializer(), report)
            } + "\n",
        )
        Files.writeString(reportPath, renderMarkdown(request))
        return WhiteBoxHarnessWriteResult(
            summaryPath = summaryPath,
            casesPath = casesPath,
            reportPath = reportPath,
            failedAssertions = failedAssertions,
            artifactCount = artifactCount,
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

    private fun joinKeyLabel(joinKey: WhiteBoxJoinKey): String =
        listOfNotNull(
            joinKey.scenarioId?.let { "scenario=$it" },
            joinKey.zoneId?.let { "zone=$it" },
            joinKey.floorIndex?.let { "floor=$it" },
            joinKey.seed?.let { "seed=$it" },
        ).joinToString(separator = ", ")

    private fun caseDirName(joinKey: WhiteBoxJoinKey): String =
        buildList {
            joinKey.scenarioId?.let { scenarioId -> add("scenario-${sanitize(scenarioId)}") }
            joinKey.zoneId?.let { zoneId -> add("zone-${sanitize(zoneId)}") }
            joinKey.floorIndex?.let { floorIndex -> add("floor-$floorIndex") }
            joinKey.seed?.let { seed -> add("seed-$seed") }
        }.joinToString(separator = "__").ifBlank { "case" }

    private fun sanitize(value: String): String =
        value.map { ch ->
            when {
                ch.isLetterOrDigit() -> ch
                ch == '.' || ch == '-' || ch == '_' || ch == ':' -> ch
                else -> '_'
            }
        }.joinToString(separator = "")
}

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
