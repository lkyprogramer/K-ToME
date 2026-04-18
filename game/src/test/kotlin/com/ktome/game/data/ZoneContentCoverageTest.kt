package com.ktome.game.data

import com.ktome.core.economy.ShardEconomy
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.economy.ShopNode
import com.ktome.core.item.AffixEquipType
import com.ktome.core.item.AffixType
import com.ktome.core.loot.AffixCostBand
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier
import com.ktome.game.ZoneMechanicRuntime
import com.ktome.game.data.schema.RewardRoutingGrantMode
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
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
        val rewardRoutingKeys =
            catalog.rewardRoutingEntries.mapTo(linkedSetOf()) { entry ->
                Triple(entry.zoneId, entry.interactableId, entry.grantMode)
            }

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
                objective.interactables.any { interactableId ->
                    Triple(zone.id, interactableId, RewardRoutingGrantMode.GROUND_CACHE) in rewardRoutingKeys ||
                        Triple(zone.id, interactableId, RewardRoutingGrantMode.SUPPORT_GRANT) in rewardRoutingKeys
                },
                "Optional zone '${zone.id}' must route its objective interactables into reward routing authority.",
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
    fun `optional and late phase three zones carry unique roster anchors instead of mirrored shells`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        val zonesById = catalog.zones.associateBy { zone -> zone.id }
        val uniqueRosterExpectations =
            listOf(
                Triple("bandit_camp", "greenwood_fringe", setOf("bandit.cutthroat", "bandit.banner_guard", "bandit.cache_overseer")),
                Triple("elven_ruins", "greenwood_fringe", setOf("warded_ruin.relic_guard", "warded_ruin.cleanse_adept", "warded_ruin.ward_lancer")),
                Triple("molten_core", "deep_iron_pit", setOf("forge.slag_tender", "forge.heat_channeler", "forge.crucible_knight")),
                Triple("crystal_cavern", "underground_river", setOf("crystal.shardling", "crystal.prism_weaver", "crystal.resonant_colossus")),
                Triple("underground_river", "grey_gate_depths", setOf("river.hook_lurker", "river.tide_mender", "river.current_reaver")),
                Triple("abyssal_temple", "underground_river", setOf("abyssal.ward_breaker", "abyssal.void_preacher", "abyssal.eclipsed_seraph")),
                Triple("abyssal_heart", "abyssal_temple", setOf("abyssal_guardian_encounter")),
            )

        uniqueRosterExpectations.forEach { (zoneId, baselineZoneId, expectedAnchorIds) ->
            val zone = requireNotNull(zonesById[zoneId]) { "Missing zone '$zoneId'." }
            val baseline = requireNotNull(zonesById[baselineZoneId]) { "Missing baseline zone '$baselineZoneId'." }
            val rosterIds = (zone.monsterPools + zone.elitePools + listOfNotNull(zone.bossEncounterId)).toSet()
            val baselineIds = (baseline.monsterPools + baseline.elitePools + listOfNotNull(baseline.bossEncounterId)).toSet()

            assertTrue(
                rosterIds.any(expectedAnchorIds::contains),
                "Zone '$zoneId' must expose at least one PR-09 unique roster anchor from $expectedAnchorIds.",
            )
            assertTrue(
                (rosterIds - baselineIds).isNotEmpty(),
                "Zone '$zoneId' must keep at least one roster entry not shared with '$baselineZoneId'.",
            )
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
    fun `affix catalog and special templates satisfy pr05 budget coverage contracts`() {
        val itemBundle = DataLoader(GameLocale.EN_US).loadItemBundle()
        val specialTemplatesByTier = itemBundle.specialTemplates.groupBy { template -> template.specialTier }
        val targetZones = setOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple")
        val buildArchetypes = setOf("vanguard", "rogue", "arcanist", "templar")
        val artifactTemplates = specialTemplatesByTier.getValue(SpecialTier.ARTIFACT)

        assertEquals(84, itemBundle.affixes.size)
        assertTrue(
            itemBundle.affixes.all { affix ->
                affix.cost in setOf(
                    AffixCostBand.TRIVIAL.cost,
                    AffixCostBand.MINOR.cost,
                    AffixCostBand.MEDIUM.cost,
                    AffixCostBand.MAJOR.cost,
                    AffixCostBand.SIGNATURE.cost,
                )
            },
            "PR-05 affix cost must stay inside the frozen cost bands 1/3/6/10/14.",
        )
        assertTrue(itemBundle.affixes.all { affix -> affix.affixFamily.isNotBlank() })
        assertTrue(
            itemBundle.affixes.filter { affix -> affix.cost == AffixCostBand.TRIVIAL.cost }.none { affix ->
                affix.statModifiers.castSpeedRating > 0
            },
            "TRIVIAL affixes must not carry castSpeedRating.",
        )
        assertTrue(
            itemBundle.affixes.any { affix -> affix.equipType == AffixEquipType.WEAPON && affix.type == AffixType.PREFIX },
        )
        assertTrue(
            itemBundle.affixes.any { affix -> affix.equipType == AffixEquipType.WEAPON && affix.type == AffixType.SUFFIX },
        )
        assertTrue(
            itemBundle.affixes.any { affix -> affix.equipType == AffixEquipType.ARMOR && affix.type == AffixType.PREFIX },
        )
        assertTrue(
            itemBundle.affixes.any { affix -> affix.equipType == AffixEquipType.ARMOR && affix.type == AffixType.SUFFIX },
        )
        assertTrue(specialTemplatesByTier.getValue(SpecialTier.UNIQUE).size >= 20)
        assertTrue(artifactTemplates.size >= 8)
        assertEquals(
            setOf(
                "ConditionalStatBonus",
                "DamageTypeBonus",
                "DamageVsStatus",
                "DamageVsTag",
                "HpRegenPerTurn",
                "OnHitStatusProc",
                "OnKillResourceRestore",
                "ResistanceBonus",
                "TerrainAffinityBonus",
            ),
            itemBundle.affixes.mapNotNull { affix -> affix.passive?.let { passive -> passive::class.simpleName } }.toSet(),
        )
        assertEquals(
            targetZones,
            itemBundle.specialTemplates.flatMapTo(linkedSetOf()) { template -> template.allowedZones }.intersect(targetZones),
            "PR-05 special templates must cover the four target zones.",
        )
        assertTrue(
            itemBundle.specialTemplates.flatMapTo(linkedSetOf()) { template -> template.tags.intersect(buildArchetypes) }.size >= 2,
            "PR-05 special templates must cover at least two build archetypes.",
        )
        assertTrue(
            artifactTemplates.any { template -> template.allowedSourceTiers == setOf(SourceTier.BOSS) },
            "PR-05 artifacts must include a boss-only template lane.",
        )
        assertTrue(
            artifactTemplates.any { template -> template.allowedSourceTiers == setOf(SourceTier.CHEST) },
            "PR-05 artifacts must include a chest-only template lane.",
        )
        assertTrue(
            artifactTemplates.any { template -> SourceTier.SECRET_ZONE in template.allowedSourceTiers },
            "PR-05 artifacts must include secret-zone coverage.",
        )
        assertTrue(
            artifactTemplates.map { template -> template.tags }.toSet().size >= 3,
            "Artifact templates must not collapse into one homogeneous semantic pool.",
        )
        val itemIds = itemBundle.baseItems.map { item -> item.id }.toSet()
        itemBundle.specialTemplates.forEach { template ->
            assertTrue(template.itemId in itemIds, "Special template '${template.id}' must resolve to a real item id.")
            assertNotNull(itemBundle.specialTemplateForItemId(template.itemId))
        }
    }

    @Test
    fun `damage versus tag items stay backed by localized monster tag labels`() {
        val bundle = LocalizationBundle.load()
        val english = bundle.translator(GameLocale.EN_US)
        val chinese = bundle.translator(GameLocale.ZH_CN)
        val itemBundle = DataLoader(GameLocale.EN_US).loadItemBundle()
        val passiveTags =
            (
                itemBundle.baseItems.mapNotNull { item -> (item.passive as? EquipmentPassive.DamageVsTag)?.tag } +
                    itemBundle.affixes.mapNotNull { affix -> (affix.passive as? EquipmentPassive.DamageVsTag)?.tag }
            ).toSet()

        assertEquals(setOf("bandit", "undead", "orc", "cultist", "forge", "river", "crystal", "abyssal"), passiveTags)
        passiveTags.forEach { tag ->
            val key = "monster.tag.$tag"
            assertTrue(english.text(key) != "!!$key!!", "English bundle is missing monster tag key '$key'.")
            assertTrue(chinese.text(key) != "!!$key!!", "Chinese bundle is missing monster tag key '$key'.")
        }
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
