package com.ktome.client.render

import com.ktome.client.text.LocalizedTextSeparator
import com.ktome.client.text.joinLocalizedKeys
import com.ktome.client.text.joinLocalizedValues
import com.ktome.core.snapshot.RouteOptionSnapshot
import com.ktome.game.i18n.Localizer

internal object RoutePreviewText {
    fun summaryLine(
        localizer: Localizer,
        option: RouteOptionSnapshot,
    ): String =
        buildList {
            add(recommendedLevelText(localizer, option))
            add(localizer.text("ui.world_map.shard_reward", "amount" to option.shardReward))
            if (option.isReturnPath) {
                add(localizer.text("ui.world_map.return_path"))
            }
        }.let { summaryParts -> localizer.joinLocalizedValues(LocalizedTextSeparator.INLINE, summaryParts) }

    fun traitLine(
        localizer: Localizer,
        option: RouteOptionSnapshot,
    ): String? =
        option.rescueHintLabelKeys
            .takeIf(List<String>::isNotEmpty)
            ?.let { labelKeys -> localizer.joinLocalizedKeys(LocalizedTextSeparator.INLINE, labelKeys) }
            ?.let { traits -> localizer.text("ui.world_map.route_traits", "traits" to traits) }

    fun mechanicLine(
        localizer: Localizer,
        option: RouteOptionSnapshot,
    ): String? =
        option.mechanicHintKey?.let { hintKey ->
            localizer.text("ui.world_map.route_note", "hint" to localizer.text(hintKey))
        }

    fun rewardLine(
        localizer: Localizer,
        option: RouteOptionSnapshot,
    ): String? =
        option.rewardItemNameKeys
            .takeIf(List<String>::isNotEmpty)
            ?.let { rewardNameKeys -> localizer.joinLocalizedKeys(LocalizedTextSeparator.LIST, rewardNameKeys) }

    private fun recommendedLevelText(
        localizer: Localizer,
        option: RouteOptionSnapshot,
    ): String =
        if (option.recommendedLevelMin == option.recommendedLevelMax) {
            localizer.text("ui.world_map.recommended_level.single", "level" to option.recommendedLevelMin)
        } else {
            localizer.text(
                "ui.world_map.recommended_level.range",
                "min" to option.recommendedLevelMin,
                "max" to option.recommendedLevelMax,
            )
        }
}
