package com.ktome.tools.phase4

import com.ktome.tools.verification.BaselineMode
import com.ktome.tools.verification.EvaluationEntry
import com.ktome.tools.verification.EvaluationEntryStatus
import com.ktome.tools.verification.EvaluationResult
import com.ktome.tools.verification.EvaluationVerdict
import com.ktome.tools.verification.KernelResult
import com.ktome.tools.verification.RenderResult
import com.ktome.tools.verification.ReportAggregationInput
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ReportPhase4Run(
    val inputCount: Int,
    val ownerMetricCount: Int,
    val unexpectedRegressionCount: Int,
    val approvedDebtCount: Int,
    val improvedDebtCount: Int,
    val domainCacheHitRate: Double,
    val artifactReuseRate: Double,
    val slowestDomain: String? = null,
    val summaryPath: Path,
    val markdownPath: Path,
    val comparisonPath: Path? = null,
)

@Serializable
private data class ReportPhase4Aggregate(
    val phaseId: String,
    val generatedAt: String,
    val buildId: String? = null,
    val locale: String? = null,
    val phaseVerdict: String,
    val inputCount: Int,
    val failedTaskCount: Int,
    val ownerMetricCount: Int,
    val unexpectedRegressionCount: Int,
    val approvedDebtCount: Int,
    val expectedFailureCount: Int,
    val improvedDebtCount: Int,
    val domainCacheHitRate: Double,
    val slowestDomain: String? = null,
    val topInvalidationReasons: List<String>,
    val artifactReuseRate: Double,
    val metricCatalog: List<Phase4MetricCatalogEntry>,
    val inputs: List<ReportAggregationInput>,
    val ownerMetrics: List<ReportPhase4OwnerMetric>,
    val legacyComparison: ReportPhase4LegacyComparison? = null,
)

@Serializable
private data class ReportPhase4OwnerMetric(
    val metricId: String,
    val sourceTaskId: String,
    val evaluationId: String,
    val baselineMode: BaselineMode,
    val status: EvaluationEntryStatus,
    val currentValue: JsonElement,
    val currentValueText: String,
    val target: String,
    val note: String? = null,
)

@Serializable
private data class ReportPhase4LegacyComparison(
    val comparedAt: String,
    val legacySummaryPath: String,
    val metricCount: Int,
    val mismatchCount: Int,
    val mismatches: List<ReportPhase4LegacyMismatch>,
)

@Serializable
private data class ReportPhase4LegacyMismatch(
    val metricId: String,
    val field: String,
    val expectedValue: String,
    val actualValue: String,
)

private val reportPhase4Json: Json =
    Json {
        prettyPrint = true
        explicitNulls = false
    }

object ReportPhase4Runner {
    private const val SUMMARY_FILE: String = "report-phase4-summary.json"
    private const val MARKDOWN_FILE: String = "report-phase4-summary.md"
    private const val LEGACY_COMPARISON_FILE: String = "report-phase4-legacy-comparison.json"

    fun run(compareLegacy: Boolean = false): ReportPhase4Run {
        val materialization = Phase4AggregationInputRunner.materialize()
        val repoRoot = repoRoot()
        val outputDir = reportDir()
        Files.createDirectories(outputDir)

        val aggregationInputs = materialization.inputs
        val sourcePathByTaskId = aggregationInputs.associate { input -> input.sourceTaskId to input.kernelResult.sourcePath }
        val metricCatalog = Phase4MetricCatalog.entries(sourcePathByTaskId)
        val ownerMetrics = buildOwnerMetrics(aggregationInputs = aggregationInputs)
        val evaluationResults = aggregationInputs.flatMap(ReportAggregationInput::evaluationResults)
        val failedTaskCount =
            aggregationInputs.count { input ->
                input.evaluationResults.any { evaluation ->
                    evaluation.evaluationId.endsWith(".taskStatus") && evaluation.verdict == EvaluationVerdict.FAIL
                }
            }
        val unexpectedRegressionCount = evaluationResults.sumOf(EvaluationResult::unexpectedRegressionCount)
        val approvedDebtCount = evaluationResults.sumOf(EvaluationResult::approvedDebtCount)
        val expectedFailureCount = evaluationResults.sumOf(EvaluationResult::expectedFailureCount)
        val improvedDebtCount = evaluationResults.sumOf(EvaluationResult::improvedDebtCount)
        val legacyComparison =
            if (compareLegacy) {
                compareAgainstLegacy(
                    summaryPath = requireLegacySummaryPath(),
                    repoRoot = repoRoot,
                    ownerMetrics = ownerMetrics,
                )
            } else {
                null
            }
        val aggregate =
            ReportPhase4Aggregate(
                phaseId = "P4",
                generatedAt = Instant.now().toString(),
                buildId = aggregationInputs.firstNotNullOfOrNull { input -> input.kernelResult.buildId },
                locale = aggregationInputs.firstNotNullOfOrNull { input -> input.kernelResult.locale },
                phaseVerdict =
                    when {
                        unexpectedRegressionCount > 0 -> "FAIL"
                        approvedDebtCount > 0 || expectedFailureCount > 0 -> "PASS_WITH_DEBT"
                        else -> "PASS"
                    },
                inputCount = aggregationInputs.size,
                failedTaskCount = failedTaskCount,
                ownerMetricCount = ownerMetrics.size,
                unexpectedRegressionCount = unexpectedRegressionCount,
                approvedDebtCount = approvedDebtCount,
                expectedFailureCount = expectedFailureCount,
                improvedDebtCount = improvedDebtCount,
                domainCacheHitRate = materialization.summary.domainCacheHitRate,
                slowestDomain = materialization.summary.slowestDomain,
                topInvalidationReasons = materialization.summary.topInvalidationReasons,
                artifactReuseRate = materialization.summary.artifactReuseRate,
                metricCatalog = metricCatalog,
                inputs = aggregationInputs,
                ownerMetrics = ownerMetrics,
                legacyComparison = legacyComparison,
            )
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        val markdownPath = outputDir.resolve(MARKDOWN_FILE)
        Files.writeString(summaryPath, reportPhase4Json.encodeToString(ReportPhase4Aggregate.serializer(), aggregate))
        Files.writeString(markdownPath, renderMarkdown(aggregate))
        val comparisonPath =
            legacyComparison?.let { comparison ->
                outputDir.resolve(LEGACY_COMPARISON_FILE).also { path ->
                    Files.writeString(
                        path,
                        reportPhase4Json.encodeToString(ReportPhase4LegacyComparison.serializer(), comparison),
                    )
                }
            }
        require(legacyComparison?.mismatchCount ?: 0 == 0) {
            "reportPhase4 legacy comparison found ${legacyComparison?.mismatchCount ?: 0} mismatches; inspect ${comparisonPath ?: outputDir.resolve(LEGACY_COMPARISON_FILE)}."
        }
        return ReportPhase4Run(
            inputCount = aggregate.inputCount,
            ownerMetricCount = aggregate.ownerMetricCount,
            unexpectedRegressionCount = aggregate.unexpectedRegressionCount,
            approvedDebtCount = aggregate.approvedDebtCount,
            improvedDebtCount = aggregate.improvedDebtCount,
            domainCacheHitRate = aggregate.domainCacheHitRate,
            artifactReuseRate = aggregate.artifactReuseRate,
            slowestDomain = aggregate.slowestDomain,
            summaryPath = summaryPath,
            markdownPath = markdownPath,
            comparisonPath = comparisonPath,
        )
    }

    private fun buildOwnerMetrics(aggregationInputs: List<ReportAggregationInput>): List<ReportPhase4OwnerMetric> {
        val entriesByMetricId =
            aggregationInputs
                .flatMap(ReportAggregationInput::evaluationResults)
                .flatMap { evaluation -> evaluation.entries.map { entry -> evaluation to entry } }
                .filterNot { (_, entry) -> entry.metricId.startsWith("task:") }
                .associateBy({ (_, entry) -> entry.metricId }, { (evaluation, entry) -> evaluation to entry })
        return Phase4MetricCatalog.specs.map { spec ->
            val (evaluation, entry) =
                checkNotNull(entriesByMetricId[spec.id]) {
                    "Missing evaluation entry for reportPhase4 owner metric ${spec.id}."
                }
            ReportPhase4OwnerMetric(
                metricId = spec.id,
                sourceTaskId = spec.ownerTaskId,
                evaluationId = evaluation.evaluationId,
                baselineMode = evaluation.mode,
                status = entry.status,
                currentValue = entry.currentValue,
                currentValueText = entry.currentValueText,
                target = entry.targetText ?: spec.targetText,
                note = entry.note,
            )
        }
    }

    private fun compareAgainstLegacy(
        summaryPath: Path,
        repoRoot: Path,
        ownerMetrics: List<ReportPhase4OwnerMetric>,
    ): ReportPhase4LegacyComparison {
        val payload = reportPhase4Json.parseToJsonElement(Files.readString(summaryPath)).jsonObject
        val legacyMetricsById =
            payload.jsonObjectArray("experienceMetrics").associateBy { metric ->
                metric.getValue("metricId").jsonPrimitive.content
            }
        val mismatches = mutableListOf<ReportPhase4LegacyMismatch>()
        ownerMetrics.forEach { metric ->
            val legacyMetric =
                legacyMetricsById[metric.metricId]
                    ?: run {
                        mismatches +=
                            ReportPhase4LegacyMismatch(
                                metricId = metric.metricId,
                                field = "presence",
                                expectedValue = "present",
                                actualValue = "missing",
                            )
                        return@forEach
                    }
            compareField(
                mismatches = mismatches,
                metricId = metric.metricId,
                field = "sourceTaskId",
                expectedValue = legacyMetric.getValue("sourceTaskId").jsonPrimitive.content,
                actualValue = metric.sourceTaskId,
            )
            compareField(
                mismatches = mismatches,
                metricId = metric.metricId,
                field = "currentValue",
                expectedValue = legacyMetric.getValue("currentValue").toString(),
                actualValue = metric.currentValue.toString(),
            )
            compareField(
                mismatches = mismatches,
                metricId = metric.metricId,
                field = "currentValueText",
                expectedValue = legacyMetric.getValue("currentValueText").jsonPrimitive.content,
                actualValue = metric.currentValueText,
            )
            compareField(
                mismatches = mismatches,
                metricId = metric.metricId,
                field = "target",
                expectedValue = legacyMetric.getValue("target").jsonPrimitive.content,
                actualValue = metric.target,
            )
            compareField(
                mismatches = mismatches,
                metricId = metric.metricId,
                field = "status",
                expectedValue = legacyMetric.getValue("status").jsonPrimitive.content,
                actualValue = normalizeLegacyStatus(metric.status),
            )
        }
        val extraLegacyMetricIds = legacyMetricsById.keys - ownerMetrics.map(ReportPhase4OwnerMetric::metricId).toSet()
        extraLegacyMetricIds.sorted().forEach { metricId ->
            mismatches +=
                ReportPhase4LegacyMismatch(
                    metricId = metricId,
                    field = "presence",
                    expectedValue = "absent",
                    actualValue = "unexpected legacy metric",
                )
        }
        return ReportPhase4LegacyComparison(
            comparedAt = Instant.now().toString(),
            legacySummaryPath = repoRoot.relativize(summaryPath.toAbsolutePath().normalize()).toString().replace('\\', '/'),
            metricCount = ownerMetrics.size,
            mismatchCount = mismatches.size,
            mismatches = mismatches,
        )
    }

    private fun compareField(
        mismatches: MutableList<ReportPhase4LegacyMismatch>,
        metricId: String,
        field: String,
        expectedValue: String,
        actualValue: String,
    ) {
        if (expectedValue != actualValue) {
            mismatches +=
                ReportPhase4LegacyMismatch(
                    metricId = metricId,
                    field = field,
                    expectedValue = expectedValue,
                    actualValue = actualValue,
                )
        }
    }

    private fun normalizeLegacyStatus(status: EvaluationEntryStatus): String =
        when (status) {
            EvaluationEntryStatus.UNEXPECTED_REGRESSION -> "FAIL"
            else -> "PASS"
        }

    private fun renderMarkdown(report: ReportPhase4Aggregate): String =
        buildString {
            appendLine("# reportPhase4")
            appendLine()
            appendLine("- generatedAt: `${report.generatedAt}`")
            report.buildId?.let { buildId -> appendLine("- buildId: `${buildId}`") }
            report.locale?.let { locale -> appendLine("- locale: `${locale}`") }
            appendLine("- phaseVerdict: `${report.phaseVerdict}`")
            appendLine("- inputCount: `${report.inputCount}`")
            appendLine("- failedTaskCount: `${report.failedTaskCount}`")
            appendLine("- ownerMetricCount: `${report.ownerMetricCount}`")
            appendLine("- unexpectedRegressionCount: `${report.unexpectedRegressionCount}`")
            appendLine("- approvedDebtCount: `${report.approvedDebtCount}`")
            appendLine("- expectedFailureCount: `${report.expectedFailureCount}`")
            appendLine("- improvedDebtCount: `${report.improvedDebtCount}`")
            appendLine("- domainCacheHitRate: `${formatRate(report.domainCacheHitRate)}`")
            appendLine("- artifactReuseRate: `${formatRate(report.artifactReuseRate)}`")
            report.slowestDomain?.let { slowestDomain -> appendLine("- slowestDomain: `${slowestDomain}`") }
            if (report.topInvalidationReasons.isNotEmpty()) {
                appendLine("- topInvalidationReasons: `${report.topInvalidationReasons.joinToString()}`")
            }
            appendLine()
            appendLine("## Owner Metrics")
            appendLine("| metricId | source | mode | current | target | status |")
            appendLine("| --- | --- | --- | --- | --- | --- |")
            report.ownerMetrics.forEach { metric ->
                appendLine(
                    "| `${metric.metricId}` | `${metric.sourceTaskId}` | `${metric.baselineMode}` | ${metric.currentValueText} | `${metric.target}` | `${metric.status}` |",
                )
            }
            val notedMetrics = report.ownerMetrics.filter { metric -> metric.note != null }
            if (notedMetrics.isNotEmpty()) {
                appendLine()
                notedMetrics.forEach { metric ->
                    appendLine("- `${metric.metricId}` note: ${metric.note}")
                }
            }
            appendLine()
            appendLine("## Inputs")
            report.inputs.forEach { input ->
                appendLine("### `${input.sourceTaskId}`")
                appendLine("- sourcePath: `${input.kernelResult.sourcePath}`")
                appendLine("- status: `${input.kernelResult.status}`")
                if (input.evaluationResults.isEmpty()) {
                    appendLine("- evaluations: none")
                } else {
                    appendLine("- evaluations:")
                    input.evaluationResults.forEach { evaluation ->
                        appendLine(
                            "  - `${evaluation.evaluationId}` -> verdict=${evaluation.verdict}, regressions=${evaluation.unexpectedRegressionCount}, approvedDebt=${evaluation.approvedDebtCount}, improvements=${evaluation.improvedDebtCount}",
                        )
                    }
                }
                input.renderResult?.metadata?.takeIf(JsonObject::isNotEmpty)?.let { metadata ->
                    appendLine("- cacheStatus: `${metadata.stringValue("cacheStatus") ?: "unknown"}`")
                    appendLine("- artifactReused: `${metadata["artifactReused"]?.jsonPrimitive?.content ?: "false"}`")
                    metadata["invalidationReason"]?.jsonPrimitive?.content?.let { reason ->
                        appendLine("- invalidationReason: `${reason}`")
                    }
                    metadata["evaluationDurationMillis"]?.jsonPrimitive?.content?.let { durationMillis ->
                        appendLine("- evaluationDurationMillis: `${durationMillis}`")
                    }
                }
            }
            report.legacyComparison?.let { comparison ->
                appendLine()
                appendLine("## Legacy Comparison")
                appendLine("- legacySummaryPath: `${comparison.legacySummaryPath}`")
                appendLine("- metricCount: `${comparison.metricCount}`")
                appendLine("- mismatchCount: `${comparison.mismatchCount}`")
                comparison.mismatches.forEach { mismatch ->
                    appendLine("- `${mismatch.metricId}` `${mismatch.field}` expected `${mismatch.expectedValue}` but got `${mismatch.actualValue}`")
                }
            }
        }

    private fun requireLegacySummaryPath(): Path {
        val summaryPath = legacyReportDir().resolve("phase4-summary.json")
        require(Files.exists(summaryPath)) {
            "Missing legacy phase4 summary at $summaryPath. Run phase4Report or phase4ReportOnly before compareLegacy=true."
        }
        return summaryPath
    }

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.aggregate.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "verification", "phase4")
        } else {
            Path.of(configured)
        }
    }

    private fun legacyReportDir(): Path {
        val configured = System.getProperty("ktome.phase4.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4")
        } else {
            Path.of(configured)
        }
    }

private fun repoRoot(): Path {
        val configured = System.getProperty("ktome.repo.root")
        return if (configured.isNullOrBlank()) Path.of(".").toAbsolutePath().normalize() else Path.of(configured).toAbsolutePath().normalize()
    }
}

private fun JsonObject.stringValue(key: String): String? = get(key)?.jsonPrimitive?.content

private fun JsonObject.jsonObjectArray(key: String): List<JsonObject> = getValue(key).jsonArray.map(JsonElement::jsonObject)

private fun formatRate(value: Double): String = String.format(java.util.Locale.US, "%.3f", value)
