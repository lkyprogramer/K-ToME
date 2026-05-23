package com.ktome.client.render

import com.ktome.game.contentpack.ContentPackKeyResolutionSummary
import com.ktome.game.contentpack.ContentPackOverlaySummary
import com.ktome.game.i18n.Localizer

internal object ValidationPackSummaryText {
    private const val MAX_INLINE_ITEMS: Int = 8

    fun activePackIds(
        localizer: Localizer,
        activePackIds: List<String>,
    ): String =
        activePackIds
            .sorted()
            .bounded(localizer)
            .ifEmpty { listOf(localizer.text("ui.validation.not_available")) }
            .joinToString(", ")

    fun namespaces(
        localizer: Localizer,
        summaries: List<ContentPackOverlaySummary>,
    ): String =
        summaries
            .map { summary -> "${summary.packId}:${summary.namespace}" }
            .sorted()
            .bounded(localizer)
            .ifEmpty { listOf(localizer.text("ui.validation.empty")) }
            .joinToString(", ")

    fun overlayOps(
        localizer: Localizer,
        summaries: List<ContentPackOverlaySummary>,
    ): String =
        summaries
            .map { summary ->
                val opText =
                    summary.opCounts
                        .filterValues { count -> count > 0 }
                        .toSortedMap()
                        .entries
                        .joinToString("/") { (op, count) -> "$op=$count" }
                val normalizedOpText = opText.ifBlank { localizer.text("ui.validation.empty") }
                "${summary.packId}:$normalizedOpText"
            }
            .sorted()
            .bounded(localizer)
            .ifEmpty { listOf(localizer.text("ui.validation.empty")) }
            .joinToString(", ")

    fun touchedContentIds(
        localizer: Localizer,
        touchedContentIds: List<String>,
    ): String =
        touchedContentIds
            .sorted()
            .bounded(localizer)
            .ifEmpty { listOf(localizer.text("ui.validation.empty")) }
            .joinToString(", ")

    fun keyWarnings(
        localizer: Localizer,
        summary: ContentPackKeyResolutionSummary,
    ): String =
        localizer.text(
            "ui.validation.pack.key_warnings",
            "visual" to warningKeys(localizer, summary.warningVisualKeys),
            "audio" to warningKeys(localizer, summary.warningAudioKeys),
            "locale" to warningKeys(localizer, summary.warningLocaleKeys),
        )

    private fun warningKeys(
        localizer: Localizer,
        keys: List<String>,
    ): String =
        keys
            .sorted()
            .bounded(localizer)
            .ifEmpty { listOf(localizer.text("ui.validation.empty")) }
            .joinToString(", ")

    private fun List<String>.bounded(localizer: Localizer): List<String> {
        if (size <= MAX_INLINE_ITEMS) {
            return this
        }
        return take(MAX_INLINE_ITEMS) + localizer.text("ui.validation.fold.more", "count" to (size - MAX_INLINE_ITEMS))
    }
}
