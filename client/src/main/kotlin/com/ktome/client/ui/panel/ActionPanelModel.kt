package com.ktome.client.ui.panel

internal data class ActionPanelEntryModel(
    val hotkey: String,
    val label: String,
    val enabled: Boolean,
)

internal data class ActionPanelModel(
    val entries: List<ActionPanelEntryModel>,
    val emptyStateText: String,
) {
    val isEmpty: Boolean = entries.isEmpty()
}
