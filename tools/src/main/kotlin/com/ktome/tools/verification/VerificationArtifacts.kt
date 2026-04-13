package com.ktome.tools.verification

import kotlinx.serialization.Serializable

@Serializable
data class VerificationTestCaseResult(
    val uniqueId: String,
    val displayName: String,
    val status: String,
    val errorMessage: String? = null,
)

@Serializable
data class LegacyJUnitRawResult(
    val domainId: String,
    val tier: String,
    val nodeId: String,
    val selectedClasses: List<String>,
    val selectedTags: List<String>,
    val totalTests: Int,
    val failedTests: Int,
    val durationMillis: Long,
    val tests: List<VerificationTestCaseResult>,
)

@Serializable
data class VerificationSummary(
    val domainId: String,
    val tier: String,
    val verdict: String,
    val snapshotHash: String,
    val cacheStatus: String,
    val outputPaths: Map<String, String>,
    val nodeId: String,
    val totalTests: Int,
    val failedTests: Int,
    val durationMillis: Long,
    val reportOnly: Boolean,
)

@Serializable
data class VerificationMetadata(
    val domainId: String,
    val phaseIds: List<String>,
    val workloadClass: String,
    val declaredWorkloadClasses: List<String>,
    val defaultTier: String,
    val selectedTier: String,
    val nodeId: String,
    val selectedNodeWorkloadClass: String,
    val baselineMode: String? = null,
    val cachePolicy: VerificationCachePolicyDescriptor,
    val artifactPolicy: VerificationArtifactPolicyDescriptor,
    val sourceArtifactDir: String? = null,
)

@Serializable
data class VerificationCachePolicyDescriptor(
    val buildCacheEnabled: Boolean,
    val configurationCacheCompatible: Boolean,
    val reuseExistingArtifacts: Boolean,
)

@Serializable
data class VerificationArtifactPolicyDescriptor(
    val rawResultFileName: String,
    val summaryFileName: String,
    val metadataFileName: String,
)
