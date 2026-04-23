package com.ktome.client.ui.panel

import com.ktome.client.assets.ResolvedVisualAsset

internal data class ActionPanelEntryModel(
    val hotkey: String,
    val label: String,
    val enabled: Boolean,
    val icon: ResolvedVisualAsset? = null,
)

internal data class ActionPanelModel(
    val entries: List<ActionPanelEntryModel>,
    val emptyStateText: String,
) {
    val isEmpty: Boolean = entries.isEmpty()
}
