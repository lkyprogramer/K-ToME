package com.ktome.tools.whitebox

import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal object WhiteBoxMarkdownRenderer {
    private val json: Json = Json { prettyPrint = true }

    fun render(
        domainId: String,
        corpus: WhiteBoxCorpusSpec,
        cases: List<WhiteBoxCaseReport>,
        aggregates: List<WhiteBoxAggregateReport>,
        totalAssertions: Int,
        failedAssertions: Int,
        artifactCount: Int,
    ): String =
        buildString {
            appendLine("# White-Box ${domainId.replaceFirstChar(Char::titlecase)}")
            appendLine()
            appendLine("- corpusId: `${corpus.corpusId}`")
            appendLine("- sampleCount: `${corpus.sampleCount}`")
            appendLine("- caseCount: `${cases.size}`")
            appendLine("- aggregateCount: `${aggregates.size}`")
            appendLine("- totalAssertions: `${totalAssertions}`")
            appendLine("- failedAssertions: `${failedAssertions}`")
            appendLine("- artifactCount: `${artifactCount}`")
            appendLine()

            appendLine("## Aggregate Summary")
            if (aggregates.isEmpty()) {
                appendLine("- none")
            } else {
                aggregates.forEach { aggregate ->
                    appendLine("### `${aggregate.groupId}`")
                    appendLine("- sampleCount: `${aggregate.sampleCount}`")
                    val failedRules = aggregate.assertions.filterNot { assertion -> assertion.passed }
                    appendLine("- failedRules: `${failedRules.size}`")
                    appendLine("```json")
                    appendLine(json.encodeToString(JsonObject.serializer(), aggregate.metrics))
                    appendLine("```")
                }
            }
            appendLine()

            appendLine("## Artifact Samples")
            val artifactSamples =
                cases.asSequence()
                    .flatMap { report -> report.artifacts.asSequence().map { artifact -> report.joinKey.render() to artifact } }
                    .take(10)
                    .toList()
            if (artifactSamples.isEmpty()) {
                appendLine("- none")
            } else {
                artifactSamples.forEach { (joinKey, artifact) ->
                    appendLine("- `${joinKey}` -> `${artifact.relativePath}` (`${artifact.artifactId}`)")
                }
            }
            appendLine()

            val failedCases = cases.filter { report -> report.assertions.any { assertion -> !assertion.passed } }
            appendLine("## Failed Cases")
            if (failedCases.isEmpty()) {
                appendLine("- none")
            } else {
                failedCases.forEach { report ->
                    appendLine("- `${report.joinKey.render()}`")
                    report.assertions.filterNot { assertion -> assertion.passed }
                        .forEach { assertion ->
                            appendLine("  - `${assertion.ruleId}`: ${assertion.message}")
                        }
                    report.artifacts.forEach { artifact ->
                        appendLine("  - artifact `${artifact.artifactId}`: `${artifact.relativePath}`")
                    }
                }
            }
        }

    private fun WhiteBoxJoinKey.render(): String {
        val parts = buildList {
            scenarioId?.let { value -> add("scenario=$value") }
            zoneId?.let { value -> add("zone=$value") }
            floorIndex?.let { value -> add("floor=$value") }
            seed?.let { value -> add("seed=$value") }
        }
        return parts.joinToString(separator = ", ")
    }
}
