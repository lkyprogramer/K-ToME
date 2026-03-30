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
                progressStageNameKey = "ui.summary.stage.early",
                zonePath = listOf("shattered_outpost", "greenwood_fringe"),
                zonePathNameKeys = listOf("zone.shattered_outpost.name", "zone.greenwood_fringe.name"),
                defeatedBossNameKeys = listOf("boss.bandit.captain.name"),
                claimedRouteRewardNameKeys = listOf("item.healing_potion.name"),
                outcomeReasonKey = "ui.summary.reason.player_died",
                failureSummaryKey = "ui.summary.failure_recap.early",
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
        assertTrue(lines.any { line -> line.contains("Run stage") && line.contains("Opening push") })
        assertTrue(lines.any { line -> line.contains("Route") && line.contains("Shattered Outpost -> Greenwood Fringe") })
        assertTrue(lines.any { line -> line.contains("Bosses defeated") && line.contains("Bandit Captain") })
        assertTrue(lines.any { line -> line.contains("Route rewards secured") && line.contains("Healing Potion") })
        assertTrue(lines.any { line -> line.contains("Killed by") && line.contains("Bandit Captain") })
        assertTrue(lines.any { line -> line.contains("Final state") && line.contains("STA 8/60") })
        assertTrue(lines.any { line -> line.contains("run broke during the Opening push") })
        assertFalse(lines.any { line -> line.contains("headless", ignoreCase = true) })
        assertFalse(lines.any { line -> line.contains("shattered_outpost") })
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
                progressStageNameKey = "ui.summary.stage.late",
                zonePath = listOf("deep_iron_pit", "grey_gate_depths"),
                zonePathNameKeys = listOf("zone.deep_iron_pit.name", "zone.grey_gate_depths.name"),
                defeatedBossNameKeys = listOf("boss.orc.molten_giant.name", "boss.cultist.dungeon_lord.name"),
                claimedRouteRewardNameKeys = listOf("item.basic_shield.name", "item.forgebreaker_pick.name"),
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
        assertTrue(lines.any { line -> line.contains("Bosses defeated") && line.contains("Molten Giant") && line.contains("Dungeon Lord") })
        assertTrue(lines.any { line -> line.contains("Route rewards secured") && line.contains("Basic Shield") })
        assertTrue(lines.any { line -> line.contains("secured the Late-route push") })
    }

    @Test
    fun `presenter localizes route and list separators`() {
        val app = GameApp(renderEnabled = false, initialLocale = GameLocale.ZH_CN)
        val summary =
            OutcomeSummary(
                outcome = RunOutcome.Victory(floor = 4),
                floorReached = 4,
                maxFloor = 4,
                turns = 102,
                playerLevel = 6,
                zoneNameKey = "zone.grey_gate_depths.name",
                progressStageNameKey = "ui.summary.stage.late",
                zonePath = listOf("deep_iron_pit", "grey_gate_depths"),
                zonePathNameKeys = listOf("zone.deep_iron_pit.name", "zone.grey_gate_depths.name"),
                defeatedBossNameKeys = listOf("boss.orc.molten_giant.name", "boss.cultist.dungeon_lord.name"),
                claimedRouteRewardNameKeys = listOf("item.basic_shield.name", "item.forgebreaker_pick.name"),
                outcomeReasonKey = "ui.summary.reason.boss_defeated",
                killerNameKey = null,
                killerTemplateId = null,
                finalHpCurrent = 41,
                finalHpMax = 96,
                finalResourceTypeId = "MANA",
                finalResourceLabelKey = "ui.hud.mana.short",
                finalResourceCurrent = 18,
                finalResourceMax = 42,
                lastEvents = emptyList(),
            )

        val lines = OutcomeSummaryPresenter.bodyLines(app, summary, isVictory = true)
        val routeLine = lines.first { line -> line.startsWith("路线：") }
        val bossesLine = lines.first { line -> line.startsWith("已击败 Boss：") }
        val rewardsLine = lines.first { line -> line.startsWith("已拿到的路线奖励：") }

        assertTrue(routeLine.contains(" → "))
        assertTrue(bossesLine.contains("、"))
        assertTrue(rewardsLine.contains("、"))
    }
}
