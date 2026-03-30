package com.ktome.client.screen

import com.ktome.client.GameApp
import com.ktome.client.text.LocalizedTextSeparator
import com.ktome.client.text.joinLocalizedKeys
import com.ktome.game.OutcomeSummary

internal object OutcomeSummaryPresenter {
    private const val SUMMARY_EVENT_LIMIT: Int = 2

    fun bodyLines(
        app: GameApp,
        summary: OutcomeSummary,
        isVictory: Boolean,
    ): List<String> {
        val zoneName = app.text(summary.zoneNameKey)
        val stageName = app.text(summary.progressStageNameKey)
        val routePath = summary.zonePathNameKeys.takeIf(List<String>::isNotEmpty)?.let { nameKeys -> renderNameKeyPath(app, nameKeys) }
        val defeatedBosses = renderNameKeyList(app, summary.defeatedBossNameKeys)
        val routeRewards = renderNameKeyList(app, summary.claimedRouteRewardNameKeys)
        val finalResourceLabel = app.text(summary.finalResourceLabelKey)
        return buildList {
            add(
                if (isVictory) {
                    app.text("ui.victory.floors_cleared", "current" to summary.floorReached, "max" to summary.maxFloor)
                } else {
                    app.text("ui.game_over.floor_reached", "current" to summary.floorReached, "max" to summary.maxFloor)
                },
            )
            add(app.text("ui.summary.zone", "zone" to zoneName))
            add(app.text("ui.summary.run_stage", "stage" to stageName))
            add(app.text("ui.summary.turns_taken", "turns" to summary.turns))
            add(app.text("ui.summary.final_level", "level" to summary.playerLevel))
            add(app.text("ui.summary.shards", "amount" to summary.shardBalance))
            routePath?.let { path ->
                add(app.text("ui.summary.route_path", "path" to path))
            }
            add(
                app.text(
                    "ui.summary.bosses_defeated",
                    "bosses" to defeatedBosses,
                ),
            )
            add(
                app.text(
                    "ui.summary.route_rewards_secured",
                    "rewards" to routeRewards,
                )
            )
            add(
                app.text(
                    "ui.summary.final_state",
                    "hpCurrent" to summary.finalHpCurrent,
                    "hpMax" to summary.finalHpMax,
                    "resource" to finalResourceLabel,
                    "resourceCurrent" to summary.finalResourceCurrent,
                    "resourceMax" to summary.finalResourceMax,
                ),
            )
            if (isVictory) {
                add(
                    app.text(
                        "ui.summary.victory_recap",
                        "stage" to stageName,
                        "zone" to zoneName,
                    ),
                )
            } else {
                add(
                    summary.killerNameKey?.let { killerNameKey ->
                        app.text("ui.summary.killed_by", "killer" to app.text(killerNameKey))
                    } ?: app.text("ui.summary.death_reason", "reason" to app.text(summary.outcomeReasonKey)),
                )
                summary.failureSummaryKey?.let { failureSummaryKey ->
                    add(
                        app.text(
                            failureSummaryKey,
                            "stage" to stageName,
                            "zone" to zoneName,
                        ),
                    )
                }
            }
            if (summary.lastEvents.isNotEmpty()) {
                add(app.text("ui.summary.last_events"))
                summary.lastEvents.takeLast(SUMMARY_EVENT_LIMIT).forEach { event -> add("- ${app.text(event)}") }
            }
        }
    }

    private fun renderNameKeyPath(
        app: GameApp,
        nameKeys: List<String>,
    ): String = app.localizer().joinLocalizedKeys(LocalizedTextSeparator.PATH, nameKeys)

    private fun renderNameKeyList(
        app: GameApp,
        nameKeys: List<String>,
    ): String =
        if (nameKeys.isEmpty()) {
            app.text("ui.summary.none")
        } else {
            app.localizer().joinLocalizedKeys(LocalizedTextSeparator.LIST, nameKeys)
        }
}
