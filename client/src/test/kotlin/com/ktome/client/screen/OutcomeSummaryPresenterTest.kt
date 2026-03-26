package com.ktome.client.screen

import com.ktome.client.GameApp
import com.ktome.core.run.RunOutcome
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.OutcomeSummary
import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OutcomeSummaryPresenterTest {
    @Test
    fun `defeat presenter renders killer zone resource and last events from typed summary`() {
        val app = GameApp(renderEnabled = false, initialLocale = GameLocale.EN_US)
        val summary =
            OutcomeSummary(
                outcome = RunOutcome.Defeat(floor = 2),
                floorReached = 2,
                maxFloor = 4,
                turns = 87,
                playerLevel = 5,
                zoneNameKey = "zone.shattered_outpost.name",
                outcomeReasonKey = "ui.summary.reason.player_died",
                killerNameKey = "monster.bandit.captain.name",
                killerTemplateId = "bandit.captain",
                finalHpCurrent = 0,
                finalHpMax = 72,
                finalResourceTypeId = "STAMINA",
                finalResourceLabelKey = "ui.hud.stamina.short",
                finalResourceCurrent = 8,
                finalResourceMax = 60,
                lastEvents = listOf(RenderTextTokenSnapshot("log.player.death")),
            )

        val lines = OutcomeSummaryPresenter.bodyLines(app, summary, isVictory = false)

        assertTrue(lines.any { line -> line.contains("Final zone") && line.contains("Shattered Outpost") })
        assertTrue(lines.any { line -> line.contains("Killed by") && line.contains("Bandit Captain") })
        assertTrue(lines.any { line -> line.contains("Final STA") && line.contains("8/60") })
        assertTrue(lines.any { line -> line.contains("Game over") || line.contains("You die") })
    }

    @Test
    fun `victory presenter omits defeat specific line`() {
        val app = GameApp(renderEnabled = false, initialLocale = GameLocale.EN_US)
        val summary =
            OutcomeSummary(
                outcome = RunOutcome.Victory(floor = 4),
                floorReached = 4,
                maxFloor = 4,
                turns = 102,
                playerLevel = 6,
                zoneNameKey = "zone.grey_gate_depths.name",
                outcomeReasonKey = "ui.summary.reason.boss_defeated",
                killerNameKey = null,
                killerTemplateId = null,
                finalHpCurrent = 41,
                finalHpMax = 96,
                finalResourceTypeId = "MANA",
                finalResourceLabelKey = "ui.hud.mana.short",
                finalResourceCurrent = 18,
                finalResourceMax = 42,
                lastEvents = listOf(RenderTextTokenSnapshot("log.victory.escape")),
            )

        val lines = OutcomeSummaryPresenter.bodyLines(app, summary, isVictory = true)

        assertFalse(lines.any { line -> line.contains("Killed by") || line.contains("Death reason") })
        assertTrue(lines.any { line -> line.contains("Grey Gate Depths") })
        assertTrue(lines.any { line -> line.contains("Victory") || line.contains("secure the exit") })
    }
}
