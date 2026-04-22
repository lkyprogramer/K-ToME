package com.ktome.client.screen

import com.ktome.game.i18n.Localizer

internal data class MainMenuSummaryModel(
    val primaryAction: MainMenuPrimaryAction,
    val continueAvailability: ContinueAvailability,
    val buildSummary: List<BuildCapabilityLine>,
    val helpLines: List<String>,
    val localeLabel: String,
)

internal enum class MainMenuPrimaryAction {
    QUICK_START,
    CONTINUE,
    VALIDATION_MODE,
    EXIT_GAME,
}

internal data class BuildCapabilityLine(
    val labelKey: String,
    val valueTextKey: String? = null,
    val disabled: Boolean = false,
)

internal data class MainMenuBuildSummaryTextLine(
    val text: String,
    val disabled: Boolean,
)

internal fun MainMenuSummaryModel.localizedBuildSummary(localizer: Localizer): List<MainMenuBuildSummaryTextLine> =
    buildSummary.map { line ->
        MainMenuBuildSummaryTextLine(
            text = buildCapabilityText(localizer, line),
            disabled = line.disabled,
        )
    }

internal fun buildCapabilityText(
    localizer: Localizer,
    line: BuildCapabilityLine,
): String {
    val label = localizer.text(line.labelKey)
    val value = line.valueTextKey?.let(localizer::text)
    return listOfNotNull(label, value).joinToString(" ")
}
