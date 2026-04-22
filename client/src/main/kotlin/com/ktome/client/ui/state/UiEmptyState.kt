package com.ktome.client.ui.state

import com.ktome.client.ui.card.ModalCardAction
import com.ktome.core.snapshot.RenderTextTokenSnapshot

internal data class UiEmptyState(
    val title: RenderTextTokenSnapshot,
    val detail: RenderTextTokenSnapshot,
    val primaryCta: ModalCardAction? = null,
) {
    init {
        require(title.key.isNotBlank()) { "UiEmptyState.title must not be blank." }
        require(detail.key.isNotBlank()) { "UiEmptyState.detail must not be blank." }
    }

    companion object {
        fun inventory(): UiEmptyState =
            UiEmptyState(
                title = RenderTextTokenSnapshot("ui.empty.inventory.title"),
                detail = RenderTextTokenSnapshot("ui.empty.inventory.detail"),
            )

        fun shop(): UiEmptyState =
            UiEmptyState(
                title = RenderTextTokenSnapshot("ui.empty.shop.title"),
                detail = RenderTextTokenSnapshot("ui.empty.shop.detail"),
            )

        fun inspect(): UiEmptyState =
            UiEmptyState(
                title = RenderTextTokenSnapshot("ui.empty.inspect.title"),
                detail = RenderTextTokenSnapshot("ui.empty.inspect.detail"),
            )

        fun log(): UiEmptyState =
            UiEmptyState(
                title = RenderTextTokenSnapshot("ui.empty.log.title"),
                detail = RenderTextTokenSnapshot("ui.empty.log.detail"),
            )
    }
}
