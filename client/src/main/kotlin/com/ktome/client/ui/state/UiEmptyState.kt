package com.ktome.client.ui.state

import com.ktome.client.ui.UiCompanionVisualKeys
import com.ktome.client.ui.card.ModalCardAction
import com.ktome.core.snapshot.RenderTextTokenSnapshot

internal data class UiEmptyState(
    val title: RenderTextTokenSnapshot,
    val detail: RenderTextTokenSnapshot,
    val iconKey: String,
    val primaryCta: ModalCardAction? = null,
) {
    init {
        require(title.key.isNotBlank()) { "UiEmptyState.title must not be blank." }
        require(detail.key.isNotBlank()) { "UiEmptyState.detail must not be blank." }
        require(iconKey.isNotBlank()) { "UiEmptyState.iconKey must not be blank." }
    }

    companion object {
        fun inventory(): UiEmptyState =
            UiEmptyState(
                title = RenderTextTokenSnapshot("ui.empty.inventory.title"),
                detail = RenderTextTokenSnapshot("ui.empty.inventory.detail"),
                iconKey = UiCompanionVisualKeys.EMPTY_INVENTORY,
            )

        fun shop(): UiEmptyState =
            UiEmptyState(
                title = RenderTextTokenSnapshot("ui.empty.shop.title"),
                detail = RenderTextTokenSnapshot("ui.empty.shop.detail"),
                iconKey = UiCompanionVisualKeys.EMPTY_SHOP,
            )

        fun inspect(): UiEmptyState =
            UiEmptyState(
                title = RenderTextTokenSnapshot("ui.empty.inspect.title"),
                detail = RenderTextTokenSnapshot("ui.empty.inspect.detail"),
                iconKey = UiCompanionVisualKeys.EMPTY_INSPECT,
            )

        fun log(): UiEmptyState =
            UiEmptyState(
                title = RenderTextTokenSnapshot("ui.empty.log.title"),
                detail = RenderTextTokenSnapshot("ui.empty.log.detail"),
                iconKey = UiCompanionVisualKeys.EMPTY_INSPECT,
            )
    }
}
