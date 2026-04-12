package com.ktome.tools.verification

data class BaselinePolicySpec(
    val mode: BaselineMode,
    val baselinePath: String? = null,
)
