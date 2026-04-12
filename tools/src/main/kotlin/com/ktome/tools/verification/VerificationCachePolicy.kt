package com.ktome.tools.verification

data class VerificationCachePolicy(
    val buildCacheEnabled: Boolean,
    val configurationCacheCompatible: Boolean,
    val reuseExistingArtifacts: Boolean = true,
)
