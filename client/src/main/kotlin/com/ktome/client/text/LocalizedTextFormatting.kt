package com.ktome.client.text

import com.ktome.game.i18n.Localizer

internal object LocalizedTextSeparator {
    const val INLINE: String = "ui.common.separator.inline"
    const val LIST: String = "ui.common.separator.list"
    const val PATH: String = "ui.common.separator.path"
}

internal fun Localizer.joinLocalizedValues(
    separatorKey: String,
    values: Iterable<String>,
): String = values.joinToString(separator = text(separatorKey))

internal fun Localizer.joinLocalizedKeys(
    separatorKey: String,
    textKeys: Iterable<String>,
): String = joinLocalizedValues(separatorKey, textKeys.map(::text))
