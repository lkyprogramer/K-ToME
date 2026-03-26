package com.ktome.game.data

import com.ktome.core.economy.ShardEconomy
import com.ktome.core.economy.ShopNode
import com.ktome.core.item.AffixEquipType
import com.ktome.core.item.AffixType
import com.ktome.game.ZoneMechanicRuntime
import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZoneContentCoverageTest {
    @Test
    fun `optional zones carry unique content and no mainline boss`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        val objectivesById = catalog.objectiveSets.associateBy { objective -> objective.id }
        val optionalZones = catalog.zones.filter { zone -> zone.worldRole == "optional" }

        assertEquals(setOf("bandit_camp", "elven_ruins", "molten_core", "crystal_cavern"), optionalZones.map { zone -> zone.id }.toSet())
        optionalZones.forEach { zone ->
            assertNotNull(zone.uniqueContentTag, "Optional zone '${zone.id}' must expose uniqueContentTag.")
            assertTrue(zone.bossEncounterId == null, "Optional zone '${zone.id}' must not own a mainline boss encounter.")
            val objective = requireNotNull(zone.objectiveSetId?.let(objectivesById::get)) {
                "Optional zone '${zone.id}' must bind to a real objective set."
            }
            assertNotNull(objective.linkedQuestId, "Optional zone '${zone.id}' must bind objective runtime to a quest-backed state.")
            assertNotNull(objective.questObjectiveId, "Optional zone '${zone.id}' must bind objective runtime to a quest objective id.")
            assertTrue(objective.interactables.isNotEmpty(), "Optional zone '${zone.id}' must expose at least one interactable hook.")
            assertTrue(objective.placements.isNotEmpty(), "Optional zone '${zone.id}' must place its objective hook into the runtime map.")
            assertTrue(
                ZoneMechanicRuntime.uniqueContentRewardProfiles(zone.uniqueContentTag).isNotEmpty(),
                "Optional zone '${zone.id}' must route uniqueContentTag into a real reward/runtime bias.",
            )
        }
    }

    @Test
    fun `late route zones expose runtime objective hooks and mechanic hints`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        val objectivesById = catalog.objectiveSets.associateBy { objective -> objective.id }

        listOf("underground_river", "abyssal_temple", "abyssal_heart").forEach { zoneId ->
            val zone = requireNotNull(catalog.zones.firstOrNull { candidate -> candidate.id == zoneId })
            val objective = requireNotNull(zone.objectiveSetId?.let(objectivesById::get)) {
                "Late route zone '$zoneId' must bind to a real objective set."
            }
            assertTrue(objective.interactables.isNotEmpty(), "Late route zone '$zoneId' must expose at least one objective interactable.")
            assertTrue(objective.placements.isNotEmpty(), "Late route zone '$zoneId' must place its objective interactable into the map.")
            assertNotNull(ZoneMechanicRuntime.introHintKey(zone), "Late route zone '$zoneId' must expose a mechanic intro hint.")
        }
    }

    @Test
    fun `shop rescue policies satisfy checkpoint affordability contract`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        val shops = catalog.shopNodes.associateBy { shop -> shop.id }

        val greenwood = requireNotNull(shops["greenwood_supply_post"])
        assertGuaranteedTagsPresent(greenwood)
        val greenwoodAffordable =
            ShardEconomy.mandatoryAffordableOffers(
                offers = greenwood.inventory,
                balance = greenwood.rescuePolicy.affordability.expectedShardBudgetByCheckpoint,
                requiredTags = greenwood.rescuePolicy.affordability.requiredAffordableTags,
            )
        assertTrue(greenwoodAffordable.size >= greenwood.rescuePolicy.affordability.mandatoryAffordableItemCount)
        assertTrue(greenwoodAffordable.any { offer -> "RECOVERY" in offer.tags || "PROTECTION" in offer.tags })

        val deepIron = requireNotNull(shops["deep_iron_pit_waystation"])
        assertGuaranteedTagsPresent(deepIron)
        val deepIronAffordable =
            ShardEconomy.mandatoryAffordableOffers(
                offers = deepIron.inventory,
                balance = deepIron.rescuePolicy.affordability.expectedShardBudgetByCheckpoint,
                requiredTags = deepIron.rescuePolicy.affordability.requiredAffordableTags,
            )
        assertTrue(deepIronAffordable.size >= deepIron.rescuePolicy.affordability.mandatoryAffordableItemCount)
        val affordableTags = deepIronAffordable.flatMapTo(linkedSetOf()) { offer -> offer.tags }
        assertTrue("MOVEMENT" in affordableTags)
        assertTrue("CLEANSING" in affordableTags || "PROTECTION" in affordableTags)
    }

    @Test
    fun `world graph route rewards stay aligned to route connections`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        val connectionIds = catalog.worldGraph.connections.map { connection -> connection.id }.toSet()
        val rewardsById = catalog.routeRewards.associateBy { reward -> reward.routeId }

        assertEquals(connectionIds, rewardsById.keys, "Route rewards must stay 1:1 with authoritative world graph connections.")
    }

    @Test
    fun `affix catalog freezes v1 budget and split`() {
        val itemBundle = DataLoader(GameLocale.EN_US).loadItemBundle()

        assertEquals(40, itemBundle.affixes.size)
        assertEquals(
            12,
            itemBundle.affixes.count { affix -> affix.equipType == AffixEquipType.WEAPON && affix.type == AffixType.PREFIX },
        )
        assertEquals(
            10,
            itemBundle.affixes.count { affix -> affix.equipType == AffixEquipType.WEAPON && affix.type == AffixType.SUFFIX },
        )
        assertEquals(
            10,
            itemBundle.affixes.count { affix -> affix.equipType == AffixEquipType.ARMOR && affix.type == AffixType.PREFIX },
        )
        assertEquals(
            8,
            itemBundle.affixes.count { affix -> affix.equipType == AffixEquipType.ARMOR && affix.type == AffixType.SUFFIX },
        )
    }

    private fun assertGuaranteedTagsPresent(shop: ShopNode) {
        shop.rescuePolicy.guaranteedTags.forEach { tag ->
            assertTrue(
                shop.inventory.any { offer -> tag in offer.tags },
                "Shop '${shop.id}' is missing guaranteed rescue tag '$tag' in raw inventory.",
            )
        }
    }
}
