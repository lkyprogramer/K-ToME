package com.ktome.core.harness.whitebox

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ContractVersionStamp(
    val contractId: String,
    val version: String,
) {
    init {
        require(contractId.isNotBlank()) { "ContractVersionStamp.contractId must not be blank." }
        require(version.isNotBlank()) { "ContractVersionStamp.version must not be blank." }
    }
}

@Serializable
data class VerificationReportHeader(
    val harnessId: String,
    val phaseId: String,
    val buildId: String,
    val locale: String,
    val corpusId: String,
    val timestamp: String,
    val activePackIds: List<String>,
    val activePackManifestVersions: Map<String, String>,
    val contractVersions: List<ContractVersionStamp>,
    val seedList: List<Long> = emptyList(),
) {
    init {
        require(harnessId.isNotBlank()) { "VerificationReportHeader.harnessId must not be blank." }
        require(phaseId.isNotBlank()) { "VerificationReportHeader.phaseId must not be blank." }
        require(buildId.isNotBlank()) { "VerificationReportHeader.buildId must not be blank." }
        require(locale.isNotBlank()) { "VerificationReportHeader.locale must not be blank." }
        require(corpusId.isNotBlank()) { "VerificationReportHeader.corpusId must not be blank." }
        require(timestamp.isNotBlank()) { "VerificationReportHeader.timestamp must not be blank." }
        require(activePackIds.all(String::isNotBlank)) { "VerificationReportHeader.activePackIds must not contain blank ids." }
        require(activePackManifestVersions.keys.all(String::isNotBlank)) {
            "VerificationReportHeader.activePackManifestVersions must not contain blank pack ids."
        }
        require(activePackManifestVersions.values.all(String::isNotBlank)) {
            "VerificationReportHeader.activePackManifestVersions must not contain blank versions."
        }
        require(contractVersions.all { stamp -> stamp.contractId.isNotBlank() && stamp.version.isNotBlank() }) {
            "VerificationReportHeader.contractVersions must not contain blank values."
        }
    }
}

@Serializable
data class WhiteBoxJoinKey(
    val seed: Long? = null,
    val zoneId: String? = null,
    val floorIndex: Int? = null,
    val scenarioId: String? = null,
) {
    init {
        require(zoneId == null || zoneId.isNotBlank()) { "WhiteBoxJoinKey.zoneId must not be blank when present." }
        require(floorIndex == null || floorIndex > 0) { "WhiteBoxJoinKey.floorIndex must be positive when present." }
        require(scenarioId == null || scenarioId.isNotBlank()) { "WhiteBoxJoinKey.scenarioId must not be blank when present." }
        require(seed != null || zoneId != null || floorIndex != null || scenarioId != null) {
            "WhiteBoxJoinKey must declare at least one coordinate."
        }
    }
}

@Serializable
data class WhiteBoxAssertionResult(
    val ruleId: String,
    val passed: Boolean,
    val severity: String = "ERROR",
    val message: String,
    val context: JsonObject = JsonObject(emptyMap()),
) {
    init {
        require(ruleId.isNotBlank()) { "WhiteBoxAssertionResult.ruleId must not be blank." }
        require(severity.isNotBlank()) { "WhiteBoxAssertionResult.severity must not be blank." }
        require(message.isNotBlank()) { "WhiteBoxAssertionResult.message must not be blank." }
    }
}

@Serializable
data class WhiteBoxArtifact(
    val artifactId: String,
    val kind: String,
    val format: String,
    val relativePath: String,
    val summary: String? = null,
    val tags: List<String> = emptyList(),
) {
    init {
        require(artifactId.isNotBlank()) { "WhiteBoxArtifact.artifactId must not be blank." }
        require(kind.isNotBlank()) { "WhiteBoxArtifact.kind must not be blank." }
        require(format.isNotBlank()) { "WhiteBoxArtifact.format must not be blank." }
        require(relativePath.isNotBlank()) { "WhiteBoxArtifact.relativePath must not be blank." }
        require(summary == null || summary.isNotBlank()) { "WhiteBoxArtifact.summary must not be blank when present." }
        require(tags.all(String::isNotBlank)) { "WhiteBoxArtifact.tags must not contain blank values." }
    }
}

@Serializable
data class WhiteBoxCaseReport(
    val joinKey: WhiteBoxJoinKey,
    val facts: JsonObject,
    val fingerprints: Map<String, String>,
    val assertions: List<WhiteBoxAssertionResult>,
    val artifacts: List<WhiteBoxArtifact>,
) {
    init {
        require(fingerprints.keys.all(String::isNotBlank)) { "WhiteBoxCaseReport.fingerprints must not contain blank keys." }
        require(fingerprints.values.all(String::isNotBlank)) { "WhiteBoxCaseReport.fingerprints must not contain blank values." }
    }
}

@Serializable
data class WhiteBoxAggregateReport(
    val groupId: String,
    val sampleCount: Int,
    val metrics: JsonObject,
    val assertions: List<WhiteBoxAssertionResult>,
) {
    init {
        require(groupId.isNotBlank()) { "WhiteBoxAggregateReport.groupId must not be blank." }
        require(sampleCount >= 0) { "WhiteBoxAggregateReport.sampleCount must not be negative." }
    }
}

@Serializable
data class WhiteBoxCorpusSpec(
    val corpusId: String,
    val description: String,
    val sampleCount: Int,
) {
    init {
        require(corpusId.isNotBlank()) { "WhiteBoxCorpusSpec.corpusId must not be blank." }
        require(description.isNotBlank()) { "WhiteBoxCorpusSpec.description must not be blank." }
        require(sampleCount >= 0) { "WhiteBoxCorpusSpec.sampleCount must not be negative." }
    }
}

@Serializable
enum class ArtifactRetentionPolicy {
    ALL,
    FAILURES_PLUS_SAMPLES,
    SUMMARY_ONLY,
}

fun interface WhiteBoxCaseRule<T> {
    fun verify(case: T): List<WhiteBoxAssertionResult>
}

fun interface WhiteBoxAggregateRule<T> {
    fun verify(cases: List<T>): List<WhiteBoxAssertionResult>
}
