package com.ktome.tools.verification

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
enum class EvaluationVerdict {
    PASS,
    FAIL,
}

@Serializable
enum class EvaluationEntryStatus {
    PASS,
    APPROVED_DEBT,
    EXPECTED_FAILURE,
    UNEXPECTED_REGRESSION,
    IMPROVEMENT,
}

@Serializable
data class EvaluationEntry(
    val metricId: String,
    val status: EvaluationEntryStatus,
    val currentValue: JsonElement,
    val currentValueText: String,
    val targetText: String? = null,
    val note: String? = null,
    val details: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class EvaluationResult(
    val evaluationId: String,
    val domainId: String,
    val mode: BaselineMode,
    val verdict: EvaluationVerdict,
    val baselineId: String? = null,
    val metricDefinitionVersion: String? = null,
    val passCount: Int,
    val approvedDebtCount: Int,
    val expectedFailureCount: Int,
    val unexpectedRegressionCount: Int,
    val improvedDebtCount: Int,
    val entries: List<EvaluationEntry>,
) {
    init {
        require(evaluationId.isNotBlank()) { "EvaluationResult.evaluationId must not be blank." }
        require(domainId.isNotBlank()) { "EvaluationResult.domainId must not be blank." }
    }
}
