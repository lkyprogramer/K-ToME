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
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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
    val schemaVersion: String,
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
    val sections: JsonObject = JsonObject(emptyMap()),
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
    val details: JsonObject = JsonObject(emptyMap()),
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
    private const val SCHEMA_VERSION: String = "report-phase4-v2"
    private const val SUMMARY_FILE: String = "report-phase4-summary.json"
    private const val MARKDOWN_FILE: String = "report-phase4-summary.md"
private const val LEGACY_COMPARISON_FILE: String = "report-phase4-legacy-comparison.json"
private const val BUILD_IDENTITY_DEBUG_FILE: String = "build-identity-debug.json"
    private val pacingMetricIds: List<String> =
        listOf(
            "avgObjectiveAcquireTurn",
            "avgVisibleHostileTurnCount",
            "avgEnemyTurns",
            "criticalPathCombatFloorSatisfied",
        )

    fun run(compareLegacy: Boolean = false): ReportPhase4Run {
        val materialization = Phase4AggregationInputRunner.materialize()
        val repoRoot = repoRoot()
        val outputDir = reportDir()
        Files.createDirectories(outputDir)

        val aggregationInputs = materialization.inputs
        val sourcePathByTaskId = aggregationInputs.associate { input -> input.sourceTaskId to input.kernelResult.sourcePath }
        val ownerMetrics = buildOwnerMetrics(aggregationInputs = aggregationInputs)
        val sections = buildSections(ownerMetrics = ownerMetrics)
        val metricCatalog =
            Phase4MetricCatalog.entries(
                sourcePathByTaskId = sourcePathByTaskId,
                targetTextByMetricId = ownerMetrics.associate { metric -> metric.metricId to metric.target },
            )
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
                schemaVersion = SCHEMA_VERSION,
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
                sections = sections,
                legacyComparison = legacyComparison,
            )
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        val markdownPath = outputDir.resolve(MARKDOWN_FILE)
        Files.writeString(summaryPath, reportPhase4Json.encodeToString(ReportPhase4Aggregate.serializer(), aggregate))
        Files.writeString(markdownPath, renderMarkdown(aggregate))
        val buildIdentityDebugPath = outputDir.resolve(BUILD_IDENTITY_DEBUG_FILE)
        Files.writeString(
            buildIdentityDebugPath,
            reportPhase4Json.encodeToString(
                JsonElement.serializer(),
                buildIdentityDebugArtifact(aggregationInputs.associateBy(ReportAggregationInput::sourceTaskId)),
            ),
        )
        val legacyComparisonPath = outputDir.resolve(LEGACY_COMPARISON_FILE)
        val comparisonPath =
            legacyComparison?.let { comparison ->
                legacyComparisonPath.also { path ->
                    Files.writeString(
                        path,
                        reportPhase4Json.encodeToString(ReportPhase4LegacyComparison.serializer(), comparison),
                    )
                }
            } ?: run {
                Files.deleteIfExists(legacyComparisonPath)
                null
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
                target = checkNotNull(entry.targetText) { "Missing targetText for reportPhase4 owner metric ${spec.id}." },
                note = entry.note,
                details = entry.details,
            )
        }
    }

    private fun buildSections(ownerMetrics: List<ReportPhase4OwnerMetric>): JsonObject {
        val objectiveMetric = requireOwnerMetric(ownerMetrics, "avgObjectiveAcquireTurn")
        val visibleMetric = requireOwnerMetric(ownerMetrics, "avgVisibleHostileTurnCount")
        val enemyMetric = requireOwnerMetric(ownerMetrics, "avgEnemyTurns")
        val satisfiedMetric = requireOwnerMetric(ownerMetrics, "criticalPathCombatFloorSatisfied")
        val satisfiedValue = satisfiedMetric.currentValue.jsonObject
        val criticalPathZoneIds =
            satisfiedValue.getValue("criticalPathZoneIds").jsonArray.map { zoneId -> zoneId.jsonPrimitive.content }
        val zoneBreakdown = satisfiedValue.getValue("zoneBreakdown").jsonObject
        val zoneSnapshots =
            criticalPathZoneIds.map { zoneId ->
                val zoneState = zoneBreakdown.getValue(zoneId).jsonObject
                CriticalPathZonePacingSnapshot(
                    zoneId = zoneId,
                    avgObjectiveAcquireTurn = zoneState["avgObjectiveAcquireTurn"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                    avgVisibleHostileTurnCount = zoneState.getValue("avgVisibleHostileTurnCount").jsonPrimitive.content.toDouble(),
                    avgEnemyTurns = zoneState.getValue("avgEnemyTurns").jsonPrimitive.content.toDouble(),
                )
            }
        val sampleMissingZoneIds =
            zoneSnapshots
                .filter { snapshot -> snapshot.avgObjectiveAcquireTurn == null }
                .map(CriticalPathZonePacingSnapshot::zoneId)
        val designAudit =
            satisfiedMetric.details
                .getValue("designAudit")
                .jsonArray
                .toCriticalPathDesignAuditSnapshots()
        validateCriticalPathPacingDetails(
            ownerMetrics =
                listOf(
                    objectiveMetric,
                    visibleMetric,
                    enemyMetric,
                    satisfiedMetric,
                ),
            sampleMissingZoneIds = sampleMissingZoneIds,
        )
        return buildJsonObject {
            put(
                "criticalPathPacing",
                CriticalPathPacingEvidence(
                    criticalPathZoneIds = criticalPathZoneIds,
                    zoneSnapshots = zoneSnapshots,
                    zoneBreakdown = zoneBreakdown,
                    designAudit = designAudit,
                    sampleMissingZoneIds = sampleMissingZoneIds,
                ).toSectionJson(),
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
                expectedValue = legacyMetric.getValue("currentValue"),
                actualValue = metric.currentValue,
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
        expectedValue: JsonElement,
        actualValue: JsonElement,
    ) {
        if (!jsonSubsetMatches(expected = expectedValue, actual = actualValue)) {
            mismatches +=
                ReportPhase4LegacyMismatch(
                    metricId = metricId,
                    field = field,
                    expectedValue = expectedValue.toString(),
                    actualValue = actualValue.toString(),
                )
        }
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

    private fun buildIdentityDebugArtifact(
        inputsByTaskId: Map<String, ReportAggregationInput>,
    ): JsonElement {
        val longRunInput = requireInput(inputsByTaskId, "longRunLab")
        val lootInput = requireInput(inputsByTaskId, "whiteBoxLoot")
        val longRunRewardSelections = readLongRunRewardSelections(repoRoot().resolve(longRunInput.kernelResult.sourcePath))
        val rewardRoutingCoverageSummary =
            lootInput.kernelResult.metrics["rewardRoutingCoverageSummary"]
                ?: error("whiteBoxLoot.rewardRoutingCoverageSummary missing from artifact.")
        return buildJsonObject {
            put("rewardSourceSelections", longRunRewardSelections)
            put(
                "topRejectedCapstoneCandidates",
                rewardRoutingCoverageSummary.jsonObject.getValue("topRejectedCapstoneCandidates"),
            )
            put(
                "perProfessionSourceCoverage",
                rewardRoutingCoverageSummary.jsonObject.getValue("professionSourceCoverage"),
            )
        }
    }

    private fun readLongRunRewardSelections(sourcePath: Path): JsonElement {
        if (!Files.isRegularFile(sourcePath)) {
            return buildJsonArray {}
        }
        val payload = reportPhase4Json.parseToJsonElement(Files.readString(sourcePath)).jsonObject
        val rewards =
            payload["reports"]
                ?.jsonArray
                ?.flatMap { report ->
                    report.jsonObject["milestoneRewards"]?.jsonArray.orEmpty()
                }.orEmpty()
        val grouped =
            rewards.groupBy { reward ->
                val payload = reward.jsonObject
                listOf(
                    payload.getValue("rewardSource").jsonPrimitive.content,
                    payload.getValue("sourceId").jsonPrimitive.content,
                    payload.getValue("zoneId").jsonPrimitive.content,
                    payload.getValue("baseItemId").jsonPrimitive.content,
                )
            }
        return buildJsonArray {
            grouped.entries
                .sortedBy { (key, _) -> key.joinToString("|") }
                .forEach { (key, entries) ->
                    add(
                        buildJsonObject {
                            put("rewardSource", key[0])
                            put("sourceId", key[1])
                            put("zoneId", key[2])
                            put("selectedBaseId", key[3])
                            put("count", entries.size)
                        },
                    )
                }
        }
    }

    private fun jsonSubsetMatches(
        expected: JsonElement,
        actual: JsonElement,
    ): Boolean =
        when {
            expected is JsonObject && actual is JsonObject ->
                expected.all { (key, expectedValue) ->
                    actual[key]?.let { actualValue -> jsonSubsetMatches(expectedValue, actualValue) } == true
                }
            expected is kotlinx.serialization.json.JsonArray && actual is kotlinx.serialization.json.JsonArray ->
                expected.size == actual.size &&
                    expected.indices.all { index -> jsonSubsetMatches(expected[index], actual[index]) }
            else -> expected == actual
        }

    private fun normalizeLegacyStatus(status: EvaluationEntryStatus): String =
        when (status) {
            EvaluationEntryStatus.UNEXPECTED_REGRESSION -> "FAIL"
            else -> "PASS"
        }

    private fun renderMarkdown(report: ReportPhase4Aggregate): String =
        buildString {
            val inputsByTaskId = report.inputs.associateBy(ReportAggregationInput::sourceTaskId)
            val scriptedHiddenInput = requireInput(inputsByTaskId, "hiddenContentHarness")
            val organicHiddenInput = requireInput(inputsByTaskId, "organicHiddenProbe")
            val solvabilityWhiteBoxInput = requireInput(inputsByTaskId, "whiteBoxSolvability")
            val lootInput = requireInput(inputsByTaskId, "whiteBoxLoot")
            val scriptedHiddenMetric = requireOwnerMetric(report.ownerMetrics, "scriptedHiddenVerificationRate")
            val leadDiscoveryMetric = requireOwnerMetric(report.ownerMetrics, "leadDiscoveryRate")
            val secretConversionMetric = requireOwnerMetric(report.ownerMetrics, "secretConversionRate")
            val cadenceMetric = requireOwnerMetric(report.ownerMetrics, "sameZoneSecretVsCadenceMaxOverlap")
            val rewardMetric = requireOwnerMetric(report.ownerMetrics, "sameZoneSecretVsRewardMaxOverlap")
            val rewardAuthorityMetric = requireOwnerMetric(report.ownerMetrics, "secretZoneRewardAuthorityViolations")
            val sourceCoverageMetric = requireOwnerMetric(report.ownerMetrics, "professionCapstoneSourceCoverage.reportOnly")
            val objectiveMetric = requireOwnerMetric(report.ownerMetrics, "avgObjectiveAcquireTurn")
            val visibleMetric = requireOwnerMetric(report.ownerMetrics, "avgVisibleHostileTurnCount")
            val enemyMetric = requireOwnerMetric(report.ownerMetrics, "avgEnemyTurns")
            val satisfiedMetric = requireOwnerMetric(report.ownerMetrics, "criticalPathCombatFloorSatisfied")
            val capstoneAdoptionFloorMetric = requireOwnerMetric(report.ownerMetrics, "professionCapstoneAdoptionFloor.reportOnly")
            val nonWeaponFloorMetric = requireOwnerMetric(report.ownerMetrics, "nonWeaponBuildPayoffFloor.reportOnly")
            val criticalPathSection = requireSection(report.sections, "criticalPathPacing")
            val criticalPathZoneIds =
                criticalPathSection.getValue("criticalPathZoneIds").jsonArray.map { zoneId ->
                    zoneId.jsonPrimitive.content
                }
            val criticalPathBreakdown = criticalPathSection.getValue("zoneBreakdown").jsonObject
            val criticalPathDesignAudit = criticalPathSection.getValue("designAudit").jsonArray
            appendLine("# reportPhase4")
            appendLine()
            appendLine("- schemaVersion: `${report.schemaVersion}`")
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
            appendLine("## Scripted vs Organic Hidden")
            appendLine("- sourceTask.scripted: `${scriptedHiddenInput.sourceTaskId}`")
            appendLine("- sourceTask.organic: `${organicHiddenInput.sourceTaskId}`")
            appendLine("- headline owner metrics: `leadDiscoveryRate / secretConversionRate / searchActionUseRate / secretZoneEntryRate`")
            appendLine("- supporting evidence: `firstHiddenDiscoveryTurn* / firstSecretZoneEntryTurn* / zoneDiscoveryDistribution / secretZoneDiscoveryDistribution / failingSecretEntryZoneIds`")
            appendLine("- `scriptedHiddenVerificationRate`: ${scriptedHiddenMetric.currentValueText} / `${scriptedHiddenMetric.status}`")
            appendLine("- `leadDiscoveryRate`: ${leadDiscoveryMetric.currentValueText} / `${leadDiscoveryMetric.status}`")
            appendLine("- `secretConversionRate`: ${secretConversionMetric.currentValueText} / `${secretConversionMetric.status}`")
            appendLine("- scripted primer cases: `${scriptedHiddenInput.kernelResult.metrics.intValue("primerActionUsedCount")}`")
            appendLine("- organic first hidden turn P50/P90: `${formatNullableInt(organicHiddenInput.kernelResult.metrics.optionalIntValue("firstHiddenDiscoveryTurnP50"))} / ${formatNullableInt(organicHiddenInput.kernelResult.metrics.optionalIntValue("firstHiddenDiscoveryTurnP90"))}`")
            appendLine("- organic first secret-zone entry turn P50/P90: `${formatNullableInt(organicHiddenInput.kernelResult.metrics.optionalIntValue("firstSecretZoneEntryTurnP50"))} / ${formatNullableInt(organicHiddenInput.kernelResult.metrics.optionalIntValue("firstSecretZoneEntryTurnP90"))}`")
            appendLine("- organic released matrix: `${organicHiddenInput.kernelResult.metrics.stringList("professionIds").joinToString()} × ${organicHiddenInput.kernelResult.metrics.stringList("raceIds").joinToString()}`")
            appendLine("- organic comboCount / seedsPerZoneCombo: `${organicHiddenInput.kernelResult.metrics.intValue("comboCount")} / ${organicHiddenInput.kernelResult.metrics.intValue("seedsPerZoneCombo")}`")
            appendLine("- organic searchPromptRequired: `${organicHiddenInput.kernelResult.metrics.getValue("searchPromptRequired").jsonPrimitive.content}`")
            appendLine("- organic failing secret-entry zones: `${organicHiddenInput.kernelResult.metrics.stringList("failingSecretEntryZoneIds").joinToString().ifBlank { "none" }}`")
            appendLine("- organic zone discovery distribution:")
            organicHiddenInput.kernelResult.metrics
                .getValue("zoneDiscoveryDistribution")
                .jsonObject
                .toSortedMap()
                .forEach { (zoneId, rate) ->
                    appendLine("  - `$zoneId`: `${formatRate(rate.jsonPrimitive.content.toDouble())}` share of total discoveries")
                }
            val secretZoneDiscoveryDistribution =
                organicHiddenInput.kernelResult.metrics["secretZoneDiscoveryDistribution"]?.jsonObject
            if (secretZoneDiscoveryDistribution.isNullOrEmpty()) {
                appendLine("- organic secret-zone discovery distribution: `n/a`")
            } else {
                appendLine("- organic secret-zone discovery distribution:")
                secretZoneDiscoveryDistribution
                    .toSortedMap()
                    .forEach { (secretZoneId, rate) ->
                        appendLine("  - `$secretZoneId`: `${formatRate(rate.jsonPrimitive.content.toDouble())}` share of total secret-zone entries")
                    }
            }
            appendLine()
            appendLine("## Solvability WhiteBox")
            appendLine("- sourceTask: `${solvabilityWhiteBoxInput.sourceTaskId}`")
            appendLine("- producerMode: `single-task lane-aware artifact with a fail-capable deterministic fixture corpus`")
            appendLine("- `revealSuccessCaseCount`: `${solvabilityWhiteBoxInput.kernelResult.metrics.intValue("revealSuccessCaseCount")}`")
            appendLine("- `revealSuccessCasesWithReveal`: `${solvabilityWhiteBoxInput.kernelResult.metrics.intValue("revealSuccessCasesWithReveal")}`")
            appendLine("- `revealSuccessCasesWithBacktrackProof`: `${solvabilityWhiteBoxInput.kernelResult.metrics.intValue("revealSuccessCasesWithBacktrackProof")}`")
            appendLine("- `revealFailCaseCount`: `${solvabilityWhiteBoxInput.kernelResult.metrics.intValue("revealFailCaseCount")}`")
            appendLine("- `revealFailCasesWithFail`: `${solvabilityWhiteBoxInput.kernelResult.metrics.intValue("revealFailCasesWithFail")}`")
            appendLine("- `revealFailTaxonomy`: `${solvabilityWhiteBoxInput.kernelResult.metrics.stringList("revealFailTaxonomy").joinToString().ifBlank { "none" }}`")
            appendLine()
            appendLine("## Local Reward Identity")
            appendLine("- sourceTask: `${lootInput.sourceTaskId}`")
            appendLine("- `sameZoneSecretVsCadenceMaxOverlap`: ${cadenceMetric.currentValueText} / `${cadenceMetric.status}`")
            appendLine("- `sameZoneSecretVsRewardMaxOverlap`: ${rewardMetric.currentValueText} / `${rewardMetric.status}`")
            appendLine("- `secretZoneRewardAuthorityViolations`: ${rewardAuthorityMetric.currentValueText} / `${rewardAuthorityMetric.status}`")
            appendLine("- `professionCapstoneSourceCoverage.reportOnly`: ${sourceCoverageMetric.currentValueText} / `${sourceCoverageMetric.status}`")
            appendLine("- `localIdentityFailurePairs`: ${lootInput.kernelResult.metrics.stringList("localIdentityFailurePairs").joinToString().ifBlank { "none" }}")
            appendLine("- `strictLocalIdentityViolations`: ${lootInput.kernelResult.metrics.getValue("strictLocalIdentityViolations").jsonArray.joinToString { violation -> violation.jsonObject.getValue("pairId").jsonPrimitive.content }.ifBlank { "none" }}")
            appendLine("- `secretZoneRewardAuthorityViolationIds`: ${lootInput.kernelResult.metrics.getValue("secretZoneRewardAuthorityViolations").jsonArray.joinToString { violation -> violation.jsonObject.getValue("violationId").jsonPrimitive.content }.ifBlank { "none" }}")
            sourceCoverageMetric.note?.let { note -> appendLine("- `professionCapstoneSourceCoverage.reportOnly` note: $note") }
            appendLine("- secret reward identity summaries:")
            lootInput.kernelResult.metrics
                .getValue("secretProfileIdentitySummaries")
                .jsonArray
                .forEach { element ->
                    val summary = element.jsonObject
                    val canonicalZoneId = summary.getValue("canonicalZoneId").jsonPrimitive.content
                    appendLine("  - `${summary.getValue("profileId").jsonPrimitive.content}` (`$canonicalZoneId`)")
                    appendLine("    - rewardStructureKeys: `${summary.getValue("rewardStructureKeys").jsonArray.joinToString { rewardKey -> rewardKey.jsonPrimitive.content }.ifBlank { "none" }}`")
                    appendLine("    - axes: `${summary.getValue("identityAxes").jsonArray.joinToString { axis -> axis.jsonPrimitive.content }}`")
                    appendLine("    - fixedItemIds: `${summary.getValue("fixedItemIds").jsonArray.joinToString { itemId -> itemId.jsonPrimitive.content }.ifBlank { "none" }}`")
                    appendLine("    - typeWeights: `${summary.getValue("typeWeights").jsonObject.entries.joinToString { (typeId, weight) -> "$typeId=${weight.jsonPrimitive.content}" }.ifBlank { "none" }}`")
                    appendLine("    - slotBias: `${summary.getValue("slotBias").jsonObject.entries.joinToString { (slotId, weight) -> "$slotId=${weight.jsonPrimitive.content}" }.ifBlank { "none" }}`")
                    appendLine("    - specialTemplateTagPreference: `${summary.getValue("specialTemplateTagPreference").jsonArray.joinToString { tag -> tag.jsonPrimitive.content }.ifBlank { "none" }}`")
                    appendLine("    - affixTagPreference: `${summary.getValue("affixTagPreference").jsonArray.joinToString { tag -> tag.jsonPrimitive.content }.ifBlank { "none" }}`")
                    appendLine(
                        "    - overlap: cadence=${formatNullableRate(summary["sameZoneCadenceMaxOverlap"]?.jsonPrimitive?.content?.toDoubleOrNull())}, " +
                            "reward=${formatNullableRate(summary["sameZoneRewardMaxOverlap"]?.jsonPrimitive?.content?.toDoubleOrNull())}, " +
                            "strictTarget=${formatNullableRate(summary["strictAllowedMaxOverlap"]?.jsonPrimitive?.content?.toDoubleOrNull())}",
                    )
                    appendLine(
                        "    - strictViolationPairIds: `${summary.getValue("strictViolationPairIds").jsonArray.joinToString { pairId -> pairId.jsonPrimitive.content }.ifBlank { "none" }}`",
                    )
                    appendLine(
                        "    - candidateBaseIds (debug): `${summary.getValue("candidateBaseIds").jsonArray.joinToString { baseId -> baseId.jsonPrimitive.content }.ifBlank { "none" }}`",
                    )
                }
            appendLine()
            appendLine("## Critical Path Pacing")
            appendLine("- sourceTask: `${satisfiedMetric.sourceTaskId}`")
            appendLine("- `avgObjectiveAcquireTurn`: ${objectiveMetric.currentValueText} / `${objectiveMetric.status}`")
            appendLine("- `avgVisibleHostileTurnCount`: ${visibleMetric.currentValueText} / `${visibleMetric.status}`")
            appendLine("- `avgEnemyTurns`: ${enemyMetric.currentValueText} / `${enemyMetric.status}`")
            appendLine("- `criticalPathCombatFloorSatisfied`: ${satisfiedMetric.currentValueText} / `${satisfiedMetric.status}`")
            appendLine("- criticalPathZoneIds: `${criticalPathZoneIds.joinToString()}`")
            appendLine("| zoneId | avgObjectiveAcquireTurn | avgVisibleHostileTurnCount | avgEnemyTurns | satisfied |")
            appendLine("| --- | --- | --- | --- | --- |")
            criticalPathZoneIds.forEach { zoneId ->
                val zoneState = criticalPathBreakdown.getValue(zoneId).jsonObject
                appendLine(
                    "| `$zoneId` | ${formatNullableRate(zoneState["avgObjectiveAcquireTurn"]?.jsonPrimitive?.content?.toDoubleOrNull())} | ${formatRate(zoneState.getValue("avgVisibleHostileTurnCount").jsonPrimitive.content.toDouble())} | ${formatRate(zoneState.getValue("avgEnemyTurns").jsonPrimitive.content.toDouble())} | `${zoneState.getValue("satisfied").jsonPrimitive.content}` |",
                )
            }
            appendLine()
            appendLine("### Critical Path Design Audit")
            appendLine("| zoneId | floorCount | mapSize | worldRole | objectiveSetId | objectiveCompletionRule | mechanicsWithoutDedicatedRuntimeHook |")
            appendLine("| --- | --- | --- | --- | --- | --- | --- |")
            criticalPathDesignAudit.forEach { element ->
                val audit = element.jsonObject
                appendLine(
                    "| `${audit.getValue("zoneId").jsonPrimitive.content}` | `${audit.getValue("floorCount").jsonPrimitive.content}` | `${audit.getValue("mapSize").jsonPrimitive.content}` | `${audit.getValue("worldRole").jsonPrimitive.content}` | `${audit.getValue("objectiveSetId").jsonPrimitive.content}` | `${audit.getValue("objectiveCompletionRule").jsonPrimitive.content}` | `${audit.getValue("mechanicsWithoutDedicatedRuntimeHook").jsonArray.joinToString { mechanic -> mechanic.jsonPrimitive.content }.ifBlank { "none" }}` |",
                )
            }
            appendLine()
            appendLine("## Terminal Build Identity")
            appendLine("- `professionCapstoneAdoptionFloor.reportOnly`: ${capstoneAdoptionFloorMetric.currentValueText} / `${capstoneAdoptionFloorMetric.status}`")
            appendLine("- `nonWeaponBuildPayoffFloor.reportOnly`: ${nonWeaponFloorMetric.currentValueText} / `${nonWeaponFloorMetric.status}`")
            capstoneAdoptionFloorMetric.note?.let { note -> appendLine("- `professionCapstoneAdoptionFloor.reportOnly` note: $note") }
            nonWeaponFloorMetric.note?.let { note -> appendLine("- `nonWeaponBuildPayoffFloor.reportOnly` note: $note") }
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
            "Missing legacy phase4 summary at $summaryPath. Run phase4LegacyReport or phase4LegacyReportOnly before compareLegacy=true."
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

    private fun validateCriticalPathPacingDetails(
        ownerMetrics: List<ReportPhase4OwnerMetric>,
        sampleMissingZoneIds: List<String>,
    ) {
        ownerMetrics.forEach { ownerMetric ->
            val expectedMetricKind =
                if (ownerMetric.metricId == "criticalPathCombatFloorSatisfied") {
                    "ratio"
                } else {
                    "minimum"
                }
            val expectedSampleMissing =
                when (ownerMetric.metricId) {
                    "avgObjectiveAcquireTurn",
                    "criticalPathCombatFloorSatisfied",
                    ->
                        sampleMissingZoneIds.isNotEmpty()

                    else -> false
                }
            val expectedZoneFailures =
                ownerMetric.currentValue.jsonObject
                    .getValue("failingZones")
                    .jsonArray
                    .map { zone -> zone.jsonPrimitive.content }
            require(ownerMetric.details.getValue("sectionRef").jsonPrimitive.content == "criticalPathPacing") {
                "reportPhase4 owner metric ${ownerMetric.metricId} must point at sections.criticalPathPacing."
            }
            require(ownerMetric.details.getValue("metricKind").jsonPrimitive.content == expectedMetricKind) {
                "reportPhase4 owner metric ${ownerMetric.metricId} metricKind drifted from the shared pacing contract."
            }
            require(ownerMetric.details.getValue("zoneFailures") == expectedZoneFailures.toJsonArray()) {
                "reportPhase4 owner metric ${ownerMetric.metricId} zoneFailures drifted from the shared pacing contract."
            }
            require(ownerMetric.details.getValue("sampleMissing").jsonPrimitive.content.toBoolean() == expectedSampleMissing) {
                "reportPhase4 owner metric ${ownerMetric.metricId} sampleMissing drifted from the shared pacing contract."
            }
            if (ownerMetric.metricId == "criticalPathCombatFloorSatisfied") {
                require(ownerMetric.details.containsKey("designAudit")) {
                    "reportPhase4 owner metric ${ownerMetric.metricId} must carry designAudit through shared pacing details."
                }
            }
        }
    }

    private fun requireSection(
        sections: JsonObject,
        sectionKey: String,
    ): JsonObject =
        checkNotNull(sections[sectionKey]?.jsonObject) {
            "Missing reportPhase4 section '$sectionKey'."
        }

}

private fun JsonObject.stringValue(key: String): String? = get(key)?.jsonPrimitive?.content

private fun JsonObject.jsonObjectArray(key: String): List<JsonObject> = getValue(key).jsonArray.map(JsonElement::jsonObject)

private fun formatRate(value: Double): String = String.format(java.util.Locale.US, "%.3f", value)

private fun formatNullableRate(value: Double?): String = value?.let(::formatRate) ?: "n/a"

private fun formatNullableInt(value: Int?): String = value?.toString() ?: "n/a"

private fun JsonObject.intValue(key: String): Int = getValue(key).jsonPrimitive.content.toInt()

private fun JsonObject.optionalIntValue(key: String): Int? = get(key)?.jsonPrimitive?.content?.toIntOrNull()

private fun JsonObject.stringList(key: String): List<String> = getValue(key).jsonArray.map { element -> element.jsonPrimitive.content }

private fun requireInput(
    inputsByTaskId: Map<String, ReportAggregationInput>,
    taskId: String,
): ReportAggregationInput =
    checkNotNull(inputsByTaskId[taskId]) {
        "Missing reportPhase4 input for task '$taskId'."
    }

private fun requireOwnerMetric(
    ownerMetrics: List<ReportPhase4OwnerMetric>,
    metricId: String,
): ReportPhase4OwnerMetric =
    ownerMetrics.firstOrNull { metric -> metric.metricId == metricId }
        ?: error("Missing reportPhase4 owner metric '$metricId'.")
