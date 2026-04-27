package com.ktome.tools.lint

import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("contractLint")
class Phase2ContentCoverageTest {
    private val catalog = DataLoader().loadSchemaCatalog()
    private val assets = ClientAssetBundleLoader.load()
    private val phase2RouteZoneIds: List<String> =
        listOf(
            "shattered_outpost",
            "greenwood_fringe",
            "deep_iron_pit",
            "grey_gate_depths",
        )
    private val expectedObjectiveSetByZone =
        linkedMapOf(
            "shattered_outpost" to "shattered_outpost_breach",
            "greenwood_fringe" to "greenwood_signal_hunt",
            "deep_iron_pit" to "deep_iron_pit_forge_run",
            "grey_gate_depths" to "grey_gate_seal_rite",
        )

    @Test
    fun `phase2 content floor covers four professions route zones and minimum content matrix`() {
        val professionById = catalog.professions.associateBy { it.id }
        val talentById = catalog.talents.associateBy { it.id }
        val treeIds = catalog.talentTrees.map { it.id }.toSet()
        val zoneById = catalog.zones.associateBy { it.id }
        val objectiveIds = catalog.objectiveSets.map { it.id }.toSet()
        val bossIds = catalog.bossEncounters.map { it.id }.toSet()

        assertEquals(
            setOf("vanguard", "arcanist", "rogue", "templar", "berserker", "spellblade", "shadowblade", "warden"),
            professionById.keys,
        )
        assertTrue(zoneById.keys.containsAll(FOUNDATION_ZONE_ROUTE), "Foundation route zones must remain present in the Phase 3 catalog.")
        assertTrue(catalog.monsters.size >= 24, "Expected at least 24 monsters, got ${catalog.monsters.size}.")
        assertTrue(catalog.itemBundle.items.size >= 24, "Expected at least 24 items, got ${catalog.itemBundle.items.size}.")
        assertTrue(catalog.itemBundle.items.count { item -> "weapon" in item.tags } >= 6, "Expected at least 6 weapons.")
        assertTrue(catalog.itemBundle.items.count { item -> "armor" in item.tags } >= 6, "Expected at least 6 armor items.")
        assertTrue(catalog.itemBundle.items.count { item -> "accessory" in item.tags } >= 4, "Expected at least 4 accessories.")
        assertTrue(catalog.itemBundle.items.count { item -> "consumable" in item.tags } >= 6, "Expected at least 6 consumables.")
        assertTrue(
            catalog.itemBundle.items.count { item -> "reward" in item.tags || "quest" in item.tags } >= 2,
            "Expected at least 2 quest/boss/reward items.",
        )

        professionById.values.forEach { profession ->
            assertTrue(profession.talentTrees.size >= 3, "Profession ${profession.id} must expose at least 3 trees.")
            if ("frozen" !in profession.tags) {
                assertEquals(3, profession.startingTalents.size, "Profession ${profession.id} must expose exactly 3 starting talents.")
                val ownedNonStarterTalents =
                    catalog.talents.filter { talent ->
                        talent.treeId in profession.talentTrees && talent.id !in profession.startingTalents
                    }
                assertTrue(
                    ownedNonStarterTalents.isNotEmpty(),
                    "Profession ${profession.id} must expose at least one non-starter talent for explicit run choices.",
                )
            } else {
                assertTrue(profession.startingTalents.isEmpty(), "Frozen profession ${profession.id} must keep starter runtime frozen.")
            }
            assertTrue(profession.startingKit.isNotEmpty(), "Profession ${profession.id} must expose starting kit.")
            assertTrue(profession.resourceProfiles.isNotEmpty(), "Profession ${profession.id} must expose at least one resource profile.")
            assertTrue(profession.soloContract.offenseTags.isNotEmpty(), "Profession ${profession.id} must expose offense tags.")
            assertTrue(profession.soloContract.defenseTags.isNotEmpty(), "Profession ${profession.id} must expose defense tags.")
            assertTrue(profession.soloContract.mobilityTags.isNotEmpty(), "Profession ${profession.id} must expose mobility tags.")
            assertTrue(profession.soloContract.aoeAnswerTags.isNotEmpty(), "Profession ${profession.id} must expose aoe answer tags.")
            assertTrue(profession.soloContract.bossAnswerTags.isNotEmpty(), "Profession ${profession.id} must expose boss answer tags.")
            assertTrue(profession.soloContract.panicAnswerTags.isNotEmpty(), "Profession ${profession.id} must expose panic answer tags.")
            assertTrue(
                profession.resourceType in profession.startingResources.keys,
                "Profession ${profession.id} must initialize its primary resource ${profession.resourceType}.",
            )
            assertTrue(
                profession.resourceType in profession.resourceCaps.keys,
                "Profession ${profession.id} must cap its primary resource ${profession.resourceType}.",
            )
            profession.talentTrees.forEach { treeId ->
                assertTrue(treeId in treeIds, "Profession ${profession.id} references unknown tree $treeId.")
            }
            profession.startingTalents.forEach { talentId ->
                val talent = requireNotNull(talentById[talentId]) { "Profession ${profession.id} references unknown talent $talentId." }
                assertTrue(
                    talent.treeId in profession.talentTrees,
                    "Profession ${profession.id} starting talent $talentId must belong to one of its trees.",
                )
            }
        }

        assertEquals(
            phase2RouteZoneIds,
            catalog.zones.map { it.id }.filter(phase2RouteZoneIds::contains),
            "Phase 2 route zones should stay aligned in their frozen order within the expanded Phase 3 catalog.",
        )
        assertEquals(
            phase2RouteZoneIds,
            expectedObjectiveSetByZone.keys.toList(),
            "The frozen Phase 2 route objective map must stay aligned with the Phase 2 route zones.",
        )
        phase2RouteZoneIds.forEach { zoneId ->
            val zone = requireNotNull(zoneById[zoneId]) { "Missing route zone $zoneId." }
            val expectedObjectiveId = requireNotNull(expectedObjectiveSetByZone[zoneId]) { "Missing expected objective binding for $zoneId." }
            assertEquals(expectedObjectiveId, zone.objectiveSetId, "Route zone ${zone.id} must stay bound to the official objective set.")
            assertTrue(zone.objectiveSetId in objectiveIds, "Route zone ${zone.id} references unknown objective set ${zone.objectiveSetId}.")
            assertTrue(zone.monsterPools.isNotEmpty(), "Route zone ${zone.id} must define monster pools.")
            assertTrue(zone.elitePools.isNotEmpty(), "Route zone ${zone.id} must define elite pools.")
            if (zone.id == "shattered_outpost" || zone.id == "grey_gate_depths") {
                assertTrue(zone.bossEncounterId != null, "Boss route zone ${zone.id} must define boss encounter.")
                assertTrue(zone.bossEncounterId in bossIds, "Zone ${zone.id} references unknown boss encounter ${zone.bossEncounterId}.")
            }
        }

        expectedObjectiveSetByZone.values.forEach { objectiveId ->
            val objective = requireNotNull(catalog.objectiveSets.find { it.id == objectiveId }) { "Missing route objective set $objectiveId." }
            assertTrue(objective.interactables.size >= 3, "Objective ${objective.id} must expose at least 3 interactables.")
            assertTrue(objective.placements.size >= 3, "Objective ${objective.id} must expose at least 3 placements.")
        }
    }

    @Test
    fun `phase2 route critical formal path keys resolve exactly`() {
        val visualKeys =
            setOf(
                "actor.vanguard",
                "actor.arcanist",
                "actor.rogue",
                "actor.templar",
                "portrait.vanguard",
                "portrait.arcanist",
                "portrait.rogue",
                "portrait.templar",
                "icon.profession.vanguard",
                "icon.profession.arcanist",
                "icon.profession.rogue",
                "icon.profession.templar",
                "actor.cultist.dungeon_lord",
                "zone.shattered_outpost.visual",
                "zone.greenwood_fringe.visual",
                "zone.deep_iron_pit.visual",
                "zone.grey_gate_depths.visual",
                "zone.shattered_outpost.icon",
                "zone.greenwood_fringe.icon",
                "zone.deep_iron_pit.icon",
                "zone.grey_gate_depths.icon",
                "prop.armory_gate",
                "prop.mine_furnace",
                "prop.ritual_altar",
                "prop.stairs.down",
                "prop.stairs.up",
                "prop.supply_crate",
                "prop.alarm_bonfire",
                "item.basic_shield.icon",
                "item.healing_potion.icon",
                "item.mana_potion.icon",
                "item.arcane_staff.icon",
                "item.battle_axe.icon",
                "item.leather_armor.icon",
                "item.long_sword.icon",
                "item.plate_armor.icon",
                "item.scroll_teleport.icon",
                "icon.quest.armory_key",
                "icon.quest.seal_key",
                "boss.cultist.dungeon_lord.visual",
                "boss.cultist.dungeon_lord.icon",
                "icon.tree.vanguard_arms",
                "icon.tree.vanguard_shield",
                "icon.tree.vanguard_warcry",
                "icon.tree.arcanist_arcane",
                "icon.tree.arcanist_flame",
                "icon.tree.arcanist_frost",
                "icon.tree.rogue_agility",
                "icon.tree.rogue_assassination",
                "icon.tree.rogue_subtlety",
                "icon.tree.templar_grace",
                "icon.tree.templar_smite",
                "icon.tree.templar_faith",
                "tree.vanguard_arms",
                "tree.vanguard_shield",
                "tree.vanguard_warcry",
                "tree.arcanist_arcane",
                "tree.arcanist_flame",
                "tree.arcanist_frost",
                "tree.rogue_agility",
                "tree.rogue_assassination",
                "tree.templar_grace",
                "tree.templar_smite",
                "tree.rogue_subtlety",
                "tree.templar_faith",
                "icon.skill.vanguard.power_strike",
                "icon.skill.arcanist.blink",
                "icon.skill.rogue.shadowstep",
                "icon.skill.templar.divine_intervention",
                "icon.monster.bandit.captain",
                "icon.monster.bandit.sentry",
                "icon.monster.beast.rat",
                "icon.monster.cultist.dungeon_lord",
                "icon.monster.orc.raider",
                "icon.monster.undead.bone_archer",
                "icon.damage_type.fire",
                "icon.damage_type.shadow",
                "vfx.boss.warning.sigil_01",
                "vfx.zone.effect.ward_seal_01",
            )
        val audioKeys =
            setOf(
                "audio.profession.vanguard",
                "audio.profession.arcanist",
                "audio.profession.rogue",
                "audio.profession.templar",
                "audio.tree.vanguard_arms",
                "audio.tree.arcanist_arcane",
                "audio.tree.rogue_subtlety",
                "audio.tree.templar_faith",
                "ambient.shattered_outpost",
                "ambient.greenwood_fringe",
                "ambient.deep_iron_pit",
                "ambient.grey_gate_depths",
                "audio.zone.shattered_outpost",
                "audio.zone.greenwood_fringe",
                "audio.zone.deep_iron_pit",
                "audio.zone.grey_gate_depths",
                "audio.boss.warning",
                "audio.boss.bandit_captain",
                "audio.boss.cultist.dungeon_lord",
                "audio.interactable.open",
                "audio.interactable.stairs",
                "audio.ui.confirm",
                "audio.ui.cancel",
                "audio.ui.hover",
                "audio.ui.level_up",
                "audio.ui.talent_unlock",
                "audio.objective.progress",
                "audio.route.transition",
                "audio.route.complete",
                "audio.talent.power_strike",
                "audio.talent.fireball",
                "audio.talent.shadowstep",
                "audio.talent.divine_intervention",
                "audio.item.basic_shield",
                "audio.item.healing_potion",
                "audio.item.mana_potion",
                "audio.item.arcane_staff",
                "audio.item.battle_axe",
                "audio.item.leather_armor",
                "audio.item.long_sword",
                "audio.item.plate_armor",
                "audio.item.scroll_teleport",
            )

        visualKeys.forEach(::assertExactVisualKey)
        audioKeys.forEach(::assertExactAudioKey)
    }

    private fun assertExactVisualKey(key: String) {
        val resolved = assets.visualResolver.resolve(key)
        assertFalse(resolved.fallbackUsed, "Visual key $key should not resolve via fallback.")
        assertFalse(resolved.matchedByPrefix, "Visual key $key should not resolve via prefix fallback.")
    }

    private fun assertExactAudioKey(key: String) {
        val resolved = assets.audioResolver.resolve(key)
        assertFalse(resolved.fallbackUsed, "Audio key $key should not resolve via fallback.")
        assertFalse(resolved.matchedByPrefix, "Audio key $key should not resolve via prefix fallback.")
    }
}
