package com.ktome.client.ui.state

import com.ktome.client.ui.card.ModalCardAction
import com.ktome.core.snapshot.RenderTextTokenSnapshot

internal data class UiLoadingState(
    val message: RenderTextTokenSnapshot,
    val showsSpinner: Boolean,
    val allowsCancel: Boolean,
    val cancelAction: ModalCardAction? = null,
) {
    init {
        require(message.key.isNotBlank()) { "UiLoadingState.message must not be blank." }
        require(allowsCancel == (cancelAction == ModalCardAction.CANCEL)) {
            "UiLoadingState allowsCancel must match ModalCardAction.CANCEL."
        }
    }

    companion object {
        val cancelLabelToken: RenderTextTokenSnapshot = RenderTextTokenSnapshot("ui.loading.cancel")

        fun generic(): UiLoadingState =
            UiLoadingState(
                message = RenderTextTokenSnapshot("ui.loading.generic"),
                showsSpinner = true,
                allowsCancel = false,
            )

        fun cancellable(message: RenderTextTokenSnapshot = RenderTextTokenSnapshot("ui.loading.generic")): UiLoadingState =
            UiLoadingState(
                message = message,
                showsSpinner = true,
                allowsCancel = true,
                cancelAction = ModalCardAction.CANCEL,
            )
    }
}
