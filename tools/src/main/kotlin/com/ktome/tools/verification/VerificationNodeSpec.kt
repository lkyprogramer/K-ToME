package com.ktome.tools.verification

data class VerificationNodeSpec(
    val nodeId: String,
    val description: String,
    val workloadClass: VerificationWorkloadClass,
    val tier: VerificationTier,
    val nodeKind: VerificationNodeKind,
    val dependsOn: Set<String> = emptySet(),
    val selectedClasses: List<String> = emptyList(),
    val selectedTags: List<String> = emptyList(),
)
