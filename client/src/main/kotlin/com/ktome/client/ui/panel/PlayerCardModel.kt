package com.ktome.client.ui.panel

internal data class PlayerCardModel(
    val name: String,
    val hpSummary: String,
    val primaryResourceSummary: String,
    val secondaryResourceSummary: String? = null,
    val statusCount: Int = 0,
    val emptyStateText: String,
)
