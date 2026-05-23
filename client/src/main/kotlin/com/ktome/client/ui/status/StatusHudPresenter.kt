package com.ktome.client.ui.status

import com.ktome.game.i18n.Localizer

internal object StatusHudPresenter {
    const val MAX_VISIBLE_STATUS_ICONS: Int = 6

    fun present(
        localizer: Localizer,
        icons: List<StatusHudIconModel>,
    ): List<StatusHudIconModel> {
        if (icons.size <= MAX_VISIBLE_STATUS_ICONS) {
            return icons
        }
        val visibleIcons = icons.take(MAX_VISIBLE_STATUS_ICONS - 1)
        val hiddenIcons = icons.drop(MAX_VISIBLE_STATUS_ICONS - 1)
        val hiddenCount = hiddenIcons.size
        val foldSource = hiddenIcons.first()
        val foldSummary = localizer.text("ui.status.fold.summary", "count" to hiddenCount)
        val hiddenLabels =
            hiddenIcons.joinToString(", ") { icon ->
                icon.presentation.nameKey?.let(localizer::text) ?: icon.presentation.typeId
            }
        return visibleIcons +
            foldSource.copy(
                isFoldBadge = true,
                hiddenPresentations = hiddenIcons.map(StatusHudIconModel::presentation),
                foldSummary = foldSummary,
                foldInteraction =
                    StatusHudFoldInteractionModel(
                        interactive = false,
                        hint = localizer.text("ui.status.fold.non_interactive_hint"),
                        detailTitle = localizer.text("ui.status.fold.detail.title", "count" to hiddenCount),
                        detailBody = localizer.text("ui.status.fold.detail.body", "statuses" to hiddenLabels),
                    ),
            )
    }
}
