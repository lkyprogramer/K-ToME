package com.ktome.tools.verification

import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

const val VERIFICATION_BASELINE_SCHEMA_VERSION: Int = 1

private val verificationBaselineJson: Json =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        explicitNulls = false
    }

@Serializable
data class VerificationBaseline(
    val schemaVersion: Int,
    val baselineId: String,
    val domainId: String,
    val mode: BaselineMode,
    val metricDefinitionVersion: String,
    val approvedDebtKeys: List<String> = emptyList(),
    val expectedFailureCodes: List<String> = emptyList(),
    val ceilings: List<VerificationBaselineCeiling> = emptyList(),
    val expectedMetricRanges: List<VerificationExpectedMetricRange> = emptyList(),
    val sourceArtifactPath: String? = null,
    val sourceBuildId: String? = null,
    val sourceGeneratedAt: String? = null,
    val notes: List<String> = emptyList(),
    val metadata: JsonObject = JsonObject(emptyMap()),
) {
    init {
        require(schemaVersion == VERIFICATION_BASELINE_SCHEMA_VERSION) {
            "Unsupported verification baseline schemaVersion=$schemaVersion for $baselineId; expected $VERIFICATION_BASELINE_SCHEMA_VERSION."
        }
        require(baselineId.isNotBlank()) { "VerificationBaseline.baselineId must not be blank." }
        require(domainId.isNotBlank()) { "VerificationBaseline.domainId must not be blank." }
        require(metricDefinitionVersion.isNotBlank()) {
            "VerificationBaseline($baselineId).metricDefinitionVersion must not be blank."
        }
    }

    fun approvedDebtKeySet(): Set<String> = approvedDebtKeys.toSortedSet()

    fun expectedFailureCodeSet(): Set<String> = expectedFailureCodes.toSortedSet()

    fun ceiling(key: String): VerificationBaselineCeiling? = ceilings.firstOrNull { ceiling -> ceiling.key == key }

    fun expectedMetricRange(metricId: String): VerificationExpectedMetricRange? =
        expectedMetricRanges.firstOrNull { range -> range.metricId == metricId }

    companion object {
        fun read(path: Path): VerificationBaseline {
            val payload = verificationBaselineJson.parseToJsonElement(path.readText()).jsonObject
            require("schemaVersion" in payload) {
                "Verification baseline $path must declare explicit schemaVersion=$VERIFICATION_BASELINE_SCHEMA_VERSION."
            }
            return verificationBaselineJson.decodeFromJsonElement<VerificationBaseline>(payload)
        }
    }
}

@Serializable
data class VerificationBaselineCeiling(
    val key: String,
    val maxValue: Double,
    val minValue: Double? = null,
    val notes: String? = null,
) {
    init {
        require(key.isNotBlank()) { "VerificationBaselineCeiling.key must not be blank." }
        minValue?.let { minimum ->
            require(minimum <= maxValue) {
                "VerificationBaselineCeiling($key) minValue=$minimum must not exceed maxValue=$maxValue."
            }
        }
    }
}

@Serializable
data class VerificationExpectedMetricRange(
    val metricId: String,
    val baselineValue: Double? = null,
    val minValue: Double? = null,
    val minInclusive: Boolean = true,
    val maxValue: Double? = null,
    val maxInclusive: Boolean = true,
    val targetRelativeIncrease: Double? = null,
    val targetRelativeDecrease: Double? = null,
    val numerator: Int? = null,
    val denominator: Int? = null,
    val numeratorLabel: String? = null,
    val denominatorLabel: String? = null,
    val sourceValueText: String? = null,
    val normalizedFormula: String? = null,
    val notes: String? = null,
    val metadata: JsonObject = JsonObject(emptyMap()),
) {
    init {
        require(metricId.isNotBlank()) { "VerificationExpectedMetricRange.metricId must not be blank." }
        minValue?.let { minimum ->
            maxValue?.let { maximum ->
                require(minimum <= maximum) {
                    "VerificationExpectedMetricRange($metricId) minValue=$minimum must not exceed maxValue=$maximum."
                }
            }
        }
        targetRelativeIncrease?.let { value ->
            require(value >= 0.0) {
                "VerificationExpectedMetricRange($metricId) targetRelativeIncrease must be >= 0.0, found $value."
            }
        }
        targetRelativeDecrease?.let { value ->
            require(value >= 0.0) {
                "VerificationExpectedMetricRange($metricId) targetRelativeDecrease must be >= 0.0, found $value."
            }
        }
        require(targetRelativeIncrease == null || targetRelativeDecrease == null) {
            "VerificationExpectedMetricRange($metricId) must not declare both targetRelativeIncrease and targetRelativeDecrease."
        }
        require(minInclusive || minimumAcceptedValue() != null) {
            "VerificationExpectedMetricRange($metricId) cannot set minInclusive=false without a minimum bound."
        }
        require(maxInclusive || maximumAcceptedValue() != null) {
            "VerificationExpectedMetricRange($metricId) cannot set maxInclusive=false without a maximum bound."
        }
    }

    fun minimumAcceptedValue(): Double? =
        when {
            minValue != null -> minValue
            baselineValue != null && targetRelativeIncrease != null -> baselineValue * (1.0 + targetRelativeIncrease)
            else -> null
        }

    fun maximumAcceptedValue(): Double? =
        when {
            maxValue != null -> maxValue
            baselineValue != null && targetRelativeDecrease != null -> baselineValue * (1.0 - targetRelativeDecrease)
            else -> null
        }

    fun minimumBoundOperator(): String = if (minInclusive) ">=" else ">"

    fun maximumBoundOperator(): String = if (maxInclusive) "<=" else "<"

    fun passesMinimumBound(actualValue: Double): Boolean {
        val minimumAcceptedValue = minimumAcceptedValue() ?: return true
        return if (minInclusive) actualValue >= minimumAcceptedValue else actualValue > minimumAcceptedValue
    }

    fun passesMaximumBound(actualValue: Double): Boolean {
        val maximumAcceptedValue = maximumAcceptedValue() ?: return true
        return if (maxInclusive) actualValue <= maximumAcceptedValue else actualValue < maximumAcceptedValue
    }
}
