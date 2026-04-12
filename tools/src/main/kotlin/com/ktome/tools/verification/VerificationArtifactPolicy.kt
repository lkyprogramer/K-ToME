package com.ktome.tools.verification

data class VerificationArtifactPolicy(
    val rawResultFileName: String = "raw-result.json",
    val summaryFileName: String = "summary.json",
    val metadataFileName: String = "metadata.json",
)
