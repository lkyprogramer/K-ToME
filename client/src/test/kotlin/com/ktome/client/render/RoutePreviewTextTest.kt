package com.ktome.client.render

import com.ktome.client.ui.card.ModalCardAction
import com.ktome.core.snapshot.RouteOptionSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoutePreviewTextTest {
    @Test
    fun `route preview text renders player facing level trait and mechanic hints`() {
        val bundle = LocalizationBundle.load()
        val enLocalizer = bundle.translator(GameLocale.EN_US)
        val zhLocalizer = bundle.translator(GameLocale.ZH_CN)
        val option =
            RouteOptionSnapshot(
                index = 0,
                routeId = "route.greenwood_fringe.bandit_camp",
                destinationZoneId = "bandit_camp",
                destinationZoneNameKey = "zone.bandit_camp.name",
                destinationZoneDescKey = "zone.bandit_camp.desc",
                recommendedLevelMin = 3,
                recommendedLevelMax = 5,
                shardReward = 20,
                rescueHintLabelKeys = listOf("ui.world_map.route_trait.movement", "ui.world_map.route_trait.cleansing"),
                mechanicHintKey = "zone.mechanic_hint.ambush_lane",
            )

        val summary = RoutePreviewText.summaryLine(enLocalizer, option)
        val traits = RoutePreviewText.traitLine(enLocalizer, option)
        val mechanic = RoutePreviewText.mechanicLine(enLocalizer, option)
        val zhSummary = RoutePreviewText.summaryLine(zhLocalizer, option)
        val zhTraits = RoutePreviewText.traitLine(zhLocalizer, option)
        val guaranteedRewardLine =
            RoutePreviewText.guaranteedRewardLine(
                enLocalizer,
                option.copy(guaranteedRewardItemNameKeys = listOf("item.healing_potion.name", "item.bandit_trophy.name")),
            )
        val milestoneRewardLine =
            RoutePreviewText.milestoneRewardLine(
                enLocalizer,
                option.copy(milestoneRewardLabelKey = "ui.world_map.milestone_reward.affix"),
            )

        assertTrue(summary.contains("Recommended level 3-5"))
        assertTrue(summary.contains("Shards +20"))
        assertFalse(summary.contains("lv3_5"))
        assertTrue(guaranteedRewardLine!!.contains("Guaranteed utility"))
        assertTrue(milestoneRewardLine!!.contains("Milestone reward"))
        assertTrue(traits!!.contains("Route traits"))
        assertTrue(traits.contains("Mobility"))
        assertTrue(traits.contains("Cleanse"))
        assertFalse(traits.contains("MOVEMENT"))
        assertFalse(traits.contains("CLEANSING"))
        assertTrue(mechanic!!.contains("Route note"))
        assertTrue(mechanic.contains("ambush net"))
        assertTrue(zhSummary.contains("推荐等级 3-5 · 碎晶 +20"))
        assertTrue(zhTraits!!.contains("位移 · 净化"))

        val card = RoutePreviewText.modalCardModel(option.copy(guaranteedRewardItemNameKeys = listOf("item.healing_potion.name")))
        assertEquals("route:route.greenwood_fringe.bandit_camp", card.stableKey)
        assertEquals("zone.bandit_camp.name", card.title.key)
        assertEquals(null, card.iconKey)
        assertEquals(ModalCardAction.ENTER_ROUTE, card.primaryAction)
        assertEquals(ModalCardAction.CANCEL, card.secondaryAction)
        assertEquals("ui.world_map.route_card.summary.range", card.summary?.key)
    }
}
