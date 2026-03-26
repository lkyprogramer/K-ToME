package com.ktome.client.screen

import com.ktome.client.GameApp
import com.ktome.game.OutcomeSummary

internal object OutcomeSummaryPresenter {
    fun bodyLines(
        app: GameApp,
        summary: OutcomeSummary,
        isVictory: Boolean,
    ): List<String> =
        buildList {
            add(
                if (isVictory) {
                    app.text("ui.victory.floors_cleared", "current" to summary.floorReached, "max" to summary.maxFloor)
                } else {
                    app.text("ui.game_over.floor_reached", "current" to summary.floorReached, "max" to summary.maxFloor)
                },
            )
            add(app.text("ui.summary.zone", "zone" to app.text(summary.zoneNameKey)))
            add(app.text("ui.summary.turns_taken", "turns" to summary.turns))
            add(app.text("ui.summary.headless_turn_equivalent", "turns" to summary.headlessTurnEquivalent))
            add(app.text("ui.summary.final_level", "level" to summary.playerLevel))
            add(app.text("ui.summary.shards", "amount" to summary.shardBalance))
            if (summary.zonePath.isNotEmpty()) {
                add(app.text("ui.summary.route_path", "path" to summary.zonePath.joinToString(" -> ")))
            }
            add(app.text("ui.summary.final_hp", "current" to summary.finalHpCurrent, "max" to summary.finalHpMax))
            add(
                app.text(
                    "ui.summary.final_resource",
                    "resource" to app.text(summary.finalResourceLabelKey),
                    "current" to summary.finalResourceCurrent,
                    "max" to summary.finalResourceMax,
                ),
            )
            if (!isVictory) {
                add(
                    summary.killerNameKey?.let { killerNameKey ->
                        app.text("ui.summary.killed_by", "killer" to app.text(killerNameKey))
                    } ?: app.text("ui.summary.death_reason", "reason" to app.text(summary.outcomeReasonKey)),
                )
            }
            if (summary.lastEvents.isNotEmpty()) {
                add(app.text("ui.summary.last_events"))
                summary.lastEvents.forEach { event -> add("- ${app.text(event)}") }
            }
        }
}
