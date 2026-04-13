package com.ktome.tools.verification

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class KernelResult(
    val domainId: String,
    val sourceTaskId: String,
    val sourcePath: String,
    val status: String,
    val buildId: String? = null,
    val locale: String? = null,
    val metrics: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class RenderResult(
    val renderId: String,
    val domainId: String,
    val sourceTaskId: String,
    val reportOnly: Boolean,
    val summaryPath: String,
    val markdownPath: String? = null,
    val artifactInputs: List<String> = emptyList(),
    val metadata: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class ReportAggregationInput(
    val domainId: String,
    val sourceTaskId: String,
    val kernelResult: KernelResult,
    val evaluationResults: List<EvaluationResult> = emptyList(),
    val renderResult: RenderResult? = null,
)
