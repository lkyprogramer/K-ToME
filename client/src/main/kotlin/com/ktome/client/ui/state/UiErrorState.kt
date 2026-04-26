package com.ktome.client.ui.state

import com.ktome.client.build.BuildInfo
import com.ktome.client.ui.card.ModalCardAction
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.i18n.Localizer

internal data class UiErrorPayload(
    val heading: String,
    val detail: String,
    val contextKeyValuePairs: List<Pair<String, String>>,
    val buildHash: String,
) {
    fun renderPlainText(): String =
        (
            listOf(heading, detail) +
                contextKeyValuePairs.map { (key, value) -> "$key: $value" } +
                "[ktome/$buildHash]"
        ).joinToString("\n")
}

internal data class UiErrorState(
    val heading: RenderTextTokenSnapshot,
    val detail: RenderTextTokenSnapshot,
    val actions: List<ModalCardAction>,
    val copyDetailLabelKey: String = ModalCardAction.COPY_ERROR_DETAIL.labelKey,
    val payload: UiErrorPayload,
) {
    init {
        require(heading.key.isNotBlank()) { "UiErrorState.heading must not be blank." }
        require(detail.key.isNotBlank()) { "UiErrorState.detail must not be blank." }
        require(actions.containsAll(defaultActions)) {
            "UiErrorState must expose Retry, Back To Menu, and Copy Error Detail actions."
        }
        require(copyDetailLabelKey.isNotBlank()) { "UiErrorState.copyDetailLabelKey must not be blank." }
    }

    companion object {
        private val defaultActions =
            listOf(
                ModalCardAction.RETRY,
                ModalCardAction.BACK_TO_MENU,
                ModalCardAction.COPY_ERROR_DETAIL,
            )

        fun recoverable(
            localizer: Localizer,
            heading: RenderTextTokenSnapshot,
            detail: RenderTextTokenSnapshot,
            contextKeyValuePairs: List<Pair<String, String>>,
            buildHash: String = BuildInfo.shortHash,
        ): UiErrorState =
            UiErrorState(
                heading = heading,
                detail = detail,
                actions = defaultActions,
                payload =
                    UiErrorPayload(
                        heading = renderToken(localizer, heading),
                        detail = renderToken(localizer, detail),
                        contextKeyValuePairs = contextKeyValuePairs,
                        buildHash = buildHash,
                    ),
            )

        private fun renderToken(
            localizer: Localizer,
            token: RenderTextTokenSnapshot,
        ): String =
            localizer.text(
                token.key,
                *token.arguments.map { argument -> argument.name to resolveArgument(localizer, argument) }.toTypedArray(),
            )

        private fun resolveArgument(
            localizer: Localizer,
            argument: RenderTextArgumentSnapshot,
        ): String =
            argument.valueToken?.let { token -> renderToken(localizer, token) }
                ?: argument.valueKey?.let(localizer::text)
                ?: argument.value.orEmpty()
    }
}
