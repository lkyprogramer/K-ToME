package com.ktome.tools.verification

import java.util.Locale
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object VerificationBaselineComparator {
    fun compareStrictZeroFailure(
        domainId: String,
        evaluationId: String,
        metricId: String,
        observedFailureCount: Int,
        currentValue: JsonElement = JsonPrimitive(observedFailureCount),
        currentValueText: String = observedFailureCount.toString(),
        targetText: String = "0",
        note: String? = null,
        details: JsonObject = JsonObject(emptyMap()),
    ): EvaluationResult {
        val status =
            if (observedFailureCount == 0) {
                EvaluationEntryStatus.PASS
            } else {
                EvaluationEntryStatus.UNEXPECTED_REGRESSION
            }
        return buildResult(
            domainId = domainId,
            evaluationId = evaluationId,
            mode = BaselineMode.STRICT_ZERO_FAILURE,
            entries =
                listOf(
                    EvaluationEntry(
                        metricId = metricId,
                        status = status,
                        currentValue = currentValue,
                        currentValueText = currentValueText,
                        targetText = targetText,
                        note = note,
                        details = details,
                    ),
                ),
        )
    }

    fun compareApprovedDebtSet(
        domainId: String,
        evaluationId: String,
        baseline: VerificationBaseline,
        actualDebtKeys: Set<String>,
        actualDebtValues: Map<String, Double> = emptyMap(),
        currentValueTexts: Map<String, String> = emptyMap(),
        currentValueElements: Map<String, JsonElement> = emptyMap(),
        detailsByMetricId: Map<String, JsonObject> = emptyMap(),
    ): EvaluationResult {
        require(baseline.mode == BaselineMode.APPROVED_DEBT_SET) {
            "VerificationBaselineComparator.compareApprovedDebtSet requires APPROVED_DEBT_SET baseline, found ${baseline.mode}."
        }
        val approvedKeys = baseline.approvedDebtKeySet()
        val entries = mutableListOf<EvaluationEntry>()
        actualDebtKeys.sorted().forEach { debtKey ->
            val actualValue = actualDebtValues[debtKey]
            val ceiling = baseline.ceiling(debtKey)
            val status =
                when {
                    debtKey !in approvedKeys -> EvaluationEntryStatus.UNEXPECTED_REGRESSION
                    actualValue != null && exceedsCeiling(actualValue, ceiling) -> EvaluationEntryStatus.UNEXPECTED_REGRESSION
                    else -> EvaluationEntryStatus.APPROVED_DEBT
                }
            entries +=
                EvaluationEntry(
                    metricId = debtKey,
                    status = status,
                    currentValue = currentValueElements[debtKey] ?: actualValue?.let(::JsonPrimitive) ?: JsonPrimitive("present"),
                    currentValueText = currentValueTexts[debtKey] ?: actualValue?.let(::formatValue) ?: "present",
                    targetText = renderCeilingTarget(ceiling),
                    details = detailsByMetricId[debtKey] ?: JsonObject(emptyMap()),
                )
        }
        (approvedKeys - actualDebtKeys).sorted().forEach { debtKey ->
            entries +=
                EvaluationEntry(
                    metricId = debtKey,
                    status = EvaluationEntryStatus.IMPROVEMENT,
                    currentValue = JsonPrimitive("cleared"),
                    currentValueText = "cleared",
                    targetText = renderCeilingTarget(baseline.ceiling(debtKey)),
                    details = detailsByMetricId[debtKey] ?: JsonObject(emptyMap()),
                )
        }
        return buildResult(
            domainId = domainId,
            evaluationId = evaluationId,
            mode = baseline.mode,
            baselineId = baseline.baselineId,
            metricDefinitionVersion = baseline.metricDefinitionVersion,
            entries = entries,
        )
    }

    fun compareExpectedFailureCodeSet(
        domainId: String,
        evaluationId: String,
        baseline: VerificationBaseline,
        actualFailureCodes: Set<String>,
        detailsByMetricId: Map<String, JsonObject> = emptyMap(),
    ): EvaluationResult {
        require(baseline.mode == BaselineMode.EXPECTED_FAILURE_CODE_SET) {
            "VerificationBaselineComparator.compareExpectedFailureCodeSet requires EXPECTED_FAILURE_CODE_SET baseline, found ${baseline.mode}."
        }
        val expectedCodes = baseline.expectedFailureCodeSet()
        val entries = mutableListOf<EvaluationEntry>()
        actualFailureCodes.sorted().forEach { failureCode ->
            val status =
                if (failureCode in expectedCodes) {
                    EvaluationEntryStatus.EXPECTED_FAILURE
                } else {
                    EvaluationEntryStatus.UNEXPECTED_REGRESSION
                }
            entries +=
                EvaluationEntry(
                    metricId = failureCode,
                    status = status,
                    currentValue = JsonPrimitive(failureCode),
                    currentValueText = failureCode,
                    targetText = "expected failure code",
                    details = detailsByMetricId[failureCode] ?: JsonObject(emptyMap()),
                )
        }
        (expectedCodes - actualFailureCodes).sorted().forEach { failureCode ->
            entries +=
                EvaluationEntry(
                    metricId = failureCode,
                    status = EvaluationEntryStatus.IMPROVEMENT,
                    currentValue = JsonPrimitive("cleared"),
                    currentValueText = "cleared",
                    targetText = "expected failure code",
                    details = detailsByMetricId[failureCode] ?: JsonObject(emptyMap()),
                )
        }
        return buildResult(
            domainId = domainId,
            evaluationId = evaluationId,
            mode = baseline.mode,
            baselineId = baseline.baselineId,
            metricDefinitionVersion = baseline.metricDefinitionVersion,
            entries = entries,
        )
    }

    fun compareRelativeBaseline(
        domainId: String,
        evaluationId: String,
        baseline: VerificationBaseline,
        actualMetrics: Map<String, Double>,
        currentValueTexts: Map<String, String> = emptyMap(),
        currentValueElements: Map<String, JsonElement> = emptyMap(),
        detailsByMetricId: Map<String, JsonObject> = emptyMap(),
    ): EvaluationResult {
        require(baseline.mode == BaselineMode.RELATIVE_BASELINE) {
            "VerificationBaselineComparator.compareRelativeBaseline requires RELATIVE_BASELINE baseline, found ${baseline.mode}."
        }
        val entries =
            baseline.expectedMetricRanges
                .sortedBy(VerificationExpectedMetricRange::metricId)
                .map { range ->
                    val actualValue = actualMetrics[range.metricId]
                    val status = budgetRangeStatus(actualValue, range)
                    EvaluationEntry(
                        metricId = range.metricId,
                        status = status,
                        currentValue = currentValueElements[range.metricId] ?: actualValue?.let(::JsonPrimitive) ?: JsonNull,
                        currentValueText = currentValueTexts[range.metricId] ?: actualValue?.let(::formatValue) ?: "missing",
                        targetText = renderExpectedRangeTarget(range),
                        details =
                            detailsByMetricId[range.metricId]
                                ?: buildJsonObject {
                                    range.baselineValue?.let { baselineValue -> put("baselineValue", baselineValue) }
                                    range.targetRelativeIncrease?.let { relative -> put("targetRelativeIncrease", relative) }
                                    range.targetRelativeDecrease?.let { relative -> put("targetRelativeDecrease", relative) }
                                },
                    )
                }
        return buildResult(
            domainId = domainId,
            evaluationId = evaluationId,
            mode = baseline.mode,
            baselineId = baseline.baselineId,
            metricDefinitionVersion = baseline.metricDefinitionVersion,
            entries = entries,
        )
    }

    fun compareBudgetThreshold(
        domainId: String,
        evaluationId: String,
        baseline: VerificationBaseline,
        actualMetrics: Map<String, Double>,
        currentValueTexts: Map<String, String> = emptyMap(),
        currentValueElements: Map<String, JsonElement> = emptyMap(),
        detailsByMetricId: Map<String, JsonObject> = emptyMap(),
    ): EvaluationResult {
        require(baseline.mode == BaselineMode.BUDGET_THRESHOLD) {
            "VerificationBaselineComparator.compareBudgetThreshold requires BUDGET_THRESHOLD baseline, found ${baseline.mode}."
        }
        val entries = mutableListOf<EvaluationEntry>()
        baseline.expectedMetricRanges
            .sortedBy(VerificationExpectedMetricRange::metricId)
            .forEach { range ->
                val actualValue = actualMetrics[range.metricId]
                entries +=
                    EvaluationEntry(
                        metricId = range.metricId,
                        status = budgetRangeStatus(actualValue, range),
                        currentValue = currentValueElements[range.metricId] ?: actualValue?.let(::JsonPrimitive) ?: JsonNull,
                        currentValueText = currentValueTexts[range.metricId] ?: actualValue?.let(::formatValue) ?: "missing",
                        targetText = renderExpectedRangeTarget(range),
                        details = detailsByMetricId[range.metricId] ?: JsonObject(emptyMap()),
                    )
            }
        baseline.ceilings
            .sortedBy(VerificationBaselineCeiling::key)
            .filterNot { ceiling -> baseline.expectedMetricRange(ceiling.key) != null }
            .forEach { ceiling ->
                val actualValue = actualMetrics[ceiling.key]
                entries +=
                    EvaluationEntry(
                        metricId = ceiling.key,
                        status =
                            if (actualValue == null || exceedsCeiling(actualValue, ceiling)) {
                                EvaluationEntryStatus.UNEXPECTED_REGRESSION
                            } else {
                                EvaluationEntryStatus.PASS
                            },
                        currentValue = currentValueElements[ceiling.key] ?: actualValue?.let(::JsonPrimitive) ?: JsonNull,
                        currentValueText = currentValueTexts[ceiling.key] ?: actualValue?.let(::formatValue) ?: "missing",
                        targetText = renderCeilingTarget(ceiling),
                        details = detailsByMetricId[ceiling.key] ?: JsonObject(emptyMap()),
                    )
            }
        return buildResult(
            domainId = domainId,
            evaluationId = evaluationId,
            mode = baseline.mode,
            baselineId = baseline.baselineId,
            metricDefinitionVersion = baseline.metricDefinitionVersion,
            entries = entries,
        )
    }

    private fun buildResult(
        domainId: String,
        evaluationId: String,
        mode: BaselineMode,
        entries: List<EvaluationEntry>,
        baselineId: String? = null,
        metricDefinitionVersion: String? = null,
    ): EvaluationResult =
        EvaluationResult(
            evaluationId = evaluationId,
            domainId = domainId,
            mode = mode,
            verdict =
                if (entries.any { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }) {
                    EvaluationVerdict.FAIL
                } else {
                    EvaluationVerdict.PASS
                },
            baselineId = baselineId,
            metricDefinitionVersion = metricDefinitionVersion,
            passCount = entries.count { entry -> entry.status == EvaluationEntryStatus.PASS },
            approvedDebtCount = entries.count { entry -> entry.status == EvaluationEntryStatus.APPROVED_DEBT },
            expectedFailureCount = entries.count { entry -> entry.status == EvaluationEntryStatus.EXPECTED_FAILURE },
            unexpectedRegressionCount = entries.count { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION },
            improvedDebtCount = entries.count { entry -> entry.status == EvaluationEntryStatus.IMPROVEMENT },
            entries = entries,
        )

    private fun budgetRangeStatus(
        actualValue: Double?,
        range: VerificationExpectedMetricRange,
    ): EvaluationEntryStatus {
        if (actualValue == null) {
            return EvaluationEntryStatus.UNEXPECTED_REGRESSION
        }
        if (!range.passesMinimumBound(actualValue)) {
            return EvaluationEntryStatus.UNEXPECTED_REGRESSION
        }
        if (!range.passesMaximumBound(actualValue)) {
            return EvaluationEntryStatus.UNEXPECTED_REGRESSION
        }
        return EvaluationEntryStatus.PASS
    }

    private fun exceedsCeiling(
        actualValue: Double,
        ceiling: VerificationBaselineCeiling?,
    ): Boolean {
        if (ceiling == null) {
            return false
        }
        ceiling.minValue?.let { minimum ->
            if (actualValue < minimum) {
                return true
            }
        }
        return actualValue > ceiling.maxValue
    }

    private fun renderCeilingTarget(ceiling: VerificationBaselineCeiling?): String? {
        ceiling ?: return null
        return when (val minimum = ceiling.minValue) {
            null -> "<= ${formatValue(ceiling.maxValue)}"
            else -> "${formatValue(minimum)} .. ${formatValue(ceiling.maxValue)}"
        }
    }

    private fun renderExpectedRangeTarget(range: VerificationExpectedMetricRange): String =
        when {
            range.minValue != null && range.maxValue != null ->
                "${range.minimumBoundOperator()} ${formatValue(range.minValue)} .. ${range.maximumBoundOperator()} ${formatValue(range.maxValue)}"
            range.minimumAcceptedValue() != null && range.maximumAcceptedValue() != null ->
                "${range.minimumBoundOperator()} ${formatValue(range.minimumAcceptedValue()!!)} .. ${range.maximumBoundOperator()} ${formatValue(range.maximumAcceptedValue()!!)}"
            range.minimumAcceptedValue() != null -> "${range.minimumBoundOperator()} ${formatValue(range.minimumAcceptedValue()!!)}"
            range.maximumAcceptedValue() != null -> "${range.maximumBoundOperator()} ${formatValue(range.maximumAcceptedValue()!!)}"
            else -> "defined by baseline"
        }

    private fun formatValue(value: Double): String =
        if (value == value.toInt().toDouble()) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.6f", value)
        }
}
