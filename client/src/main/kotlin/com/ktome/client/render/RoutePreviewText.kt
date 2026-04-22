package com.ktome.client.render

import com.ktome.client.text.LocalizedTextSeparator
import com.ktome.client.text.joinLocalizedKeys
import com.ktome.client.text.joinLocalizedValues
import com.ktome.client.ui.card.ModalCardModel
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
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

    fun guaranteedRewardLine(
        localizer: Localizer,
        option: RouteOptionSnapshot,
    ): String? =
        option.guaranteedRewardItemNameKeys
            .takeIf(List<String>::isNotEmpty)
            ?.let { rewardNameKeys -> localizer.joinLocalizedKeys(LocalizedTextSeparator.LIST, rewardNameKeys) }
            ?.let { rewards -> localizer.text("ui.world_map.guaranteed_reward", "rewards" to rewards) }

    fun milestoneRewardLine(
        localizer: Localizer,
        option: RouteOptionSnapshot,
    ): String? =
        option.milestoneRewardLabelKey?.let { rewardLabelKey ->
            localizer.text("ui.world_map.milestone_reward", "reward" to localizer.text(rewardLabelKey))
        }

    fun modalCardModel(option: RouteOptionSnapshot): ModalCardModel =
        ModalCardModel.routePreview(
            stableKey = "route:${option.routeId}",
            title = RenderTextTokenSnapshot(option.destinationZoneNameKey),
            iconKey = null,
            summary = routeSummaryToken(option),
            detailLines =
                buildList {
                    option.destinationZoneDescKey?.let { descKey -> add(RenderTextTokenSnapshot(descKey)) }
                    option.mechanicHintKey?.let { hintKey -> add(RenderTextTokenSnapshot("ui.world_map.route_note", listOf(RenderTextArgumentSnapshot(name = "hint", valueKey = hintKey)))) }
                },
            rewardLines =
                buildList {
                    option.guaranteedRewardItemNameKeys.forEach { itemNameKey ->
                        add(
                            RenderTextTokenSnapshot(
                                "ui.world_map.guaranteed_reward",
                                listOf(RenderTextArgumentSnapshot(name = "rewards", valueKey = itemNameKey)),
                            ),
                        )
                    }
                    option.milestoneRewardLabelKey?.let { rewardKey ->
                        add(
                            RenderTextTokenSnapshot(
                                "ui.world_map.milestone_reward",
                                listOf(RenderTextArgumentSnapshot(name = "reward", valueKey = rewardKey)),
                            ),
                        )
                    }
                },
        )

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

    private fun routeSummaryToken(option: RouteOptionSnapshot): RenderTextTokenSnapshot =
        if (option.recommendedLevelMin == option.recommendedLevelMax) {
            RenderTextTokenSnapshot(
                key = "ui.world_map.route_card.summary.single",
                arguments =
                    listOf(
                        RenderTextArgumentSnapshot(name = "level", value = option.recommendedLevelMin.toString()),
                        RenderTextArgumentSnapshot(name = "shards", value = option.shardReward.toString()),
                    ),
            )
        } else {
            RenderTextTokenSnapshot(
                key = "ui.world_map.route_card.summary.range",
                arguments =
                    listOf(
                        RenderTextArgumentSnapshot(name = "min", value = option.recommendedLevelMin.toString()),
                        RenderTextArgumentSnapshot(name = "max", value = option.recommendedLevelMax.toString()),
                        RenderTextArgumentSnapshot(name = "shards", value = option.shardReward.toString()),
                    ),
            )
        }
}
