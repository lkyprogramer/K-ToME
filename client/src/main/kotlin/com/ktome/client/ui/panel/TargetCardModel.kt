package com.ktome.client.ui.panel

internal data class TargetCardModel(
    val title: String?,
    val lines: List<String>,
    val emptyStateText: String,
) {
    val isEmpty: Boolean = title == null && lines.isEmpty()
}
