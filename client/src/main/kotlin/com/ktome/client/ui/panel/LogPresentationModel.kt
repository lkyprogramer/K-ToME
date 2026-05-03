package com.ktome.client.ui.panel

import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.game.i18n.Localizer

internal enum class LogPresentationCategory {
    COMBAT,
    REWARD,
    STATUS,
    EXPLORATION,
    SYSTEM,
}

internal enum class LogPresentationImportance {
    NORMAL,
    HIGH,
    CRITICAL,
}

internal data class LogPresentationEntry(
    val text: String,
    val category: LogPresentationCategory,
    val importance: LogPresentationImportance,
)

internal data class LogPresentationModel(
    val entries: List<LogPresentationEntry>,
    val emptyStateText: String,
    val fallbackText: String,
) {
    val isEmpty: Boolean = entries.isEmpty()
}

internal object LogPresentationModelBuilder {
    fun build(
        localizer: Localizer,
        events: List<RenderLogEventSnapshot>,
        renderText: (com.ktome.core.snapshot.RenderTextTokenSnapshot) -> String,
    ): LogPresentationModel =
        LogPresentationModel(
            entries =
                events.map { event ->
                    val key = event.message.key
                    LogPresentationEntry(
                        text = renderText(event.message),
                        category = categoryFor(key),
                        importance = importanceFor(key),
                    )
                },
            emptyStateText = localizer.text("ui.log.empty"),
            fallbackText = localizer.text("ui.log.fallback"),
        )

    private fun categoryFor(key: String): LogPresentationCategory =
        when {
            key.startsWith("log.talent.") || key.startsWith("log.boss.") -> LogPresentationCategory.COMBAT
            key.startsWith("log.reward.") || key.startsWith("log.level_up") -> LogPresentationCategory.REWARD
            key.startsWith("log.status.") || key.startsWith("log.passive.") -> LogPresentationCategory.STATUS
            key.startsWith("log.zone.") || key.startsWith("log.route.") || key.startsWith("zone.trigger.") -> LogPresentationCategory.EXPLORATION
            else -> LogPresentationCategory.SYSTEM
        }

    private fun importanceFor(key: String): LogPresentationImportance =
        when {
            key.startsWith("log.boss.") -> LogPresentationImportance.CRITICAL
            key.startsWith("log.talent.failure.") || key.startsWith("log.reward.") -> LogPresentationImportance.HIGH
            else -> LogPresentationImportance.NORMAL
        }
}
