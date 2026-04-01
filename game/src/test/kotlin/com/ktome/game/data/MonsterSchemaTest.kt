package com.ktome.game.data

import com.ktome.core.combat.DamageType
import com.ktome.game.routeVisibleCommonMonsters
import com.ktome.game.routeVisibleEliteOrBossMonsters
import com.ktome.game.routeVisibleMonsters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MonsterSchemaTest {
    @Test
    fun `monster skeletons cover required family namespaces and combat contracts`() {
        val schemaCatalog = DataLoader().loadSchemaCatalog()
        val runtimeCatalog = DataLoader().loadMonsterCatalog().monsters.associateBy { monster -> monster.id }
        val monsterSchemasById = schemaCatalog.monsters.associateBy { monster -> monster.id }
        val aiProfilesById = schemaCatalog.aiProfiles.associateBy { profile -> profile.id }
        val monsterIds = schemaCatalog.monsters.map { it.id }.toSet()
        val familyTags = schemaCatalog.monsters.flatMap { it.tags }.toSet()

        assertTrue(
            monsterIds.containsAll(
                setOf(
                    "beast.rat",
                    "beast.rat_scavenger",
                    "beast.thorn_stalker",
                    "goblin.scout",
                    "goblin.scrapper",
                    "bandit.raider",
                    "bandit.archer",
                    "bandit.captain",
                    "bandit.trapper",
                    "bandit.wild_huntmaster",
                    "undead.restless_skeleton",
                    "undead.bone_guard",
                    "undead.bone_archer",
                    "undead.moss_archer",
                    "undead.ash_wraith",
                    "undead.chain_thrall",
                    "bandit.sentry",
                    "orc.raider",
                    "orc.miner",
                    "orc.forge_guard",
                    "orc.molten_giant",
                    "cultist.dungeon_lord",
                    "cultist.ember_adept",
                    "cultist.shadow_priest",
                    "cultist.ashgate_warden",
                    "bandit.cutthroat",
                    "warded_ruin.relic_guard",
                    "forge.slag_tender",
                    "crystal.shardling",
                    "river.hook_lurker",
                    "abyssal.ward_breaker",
                    "abyssal.eclipsed_seraph",
                    "abyssal.guardian",
                ),
            ),
        )
        assertTrue(schemaCatalog.monsters.size >= 60, "Phase 3 monster roster floor must reach at least 60 templates.")
        assertEquals(4, schemaCatalog.monsters.count { monster -> "boss" in monster.tags })
        assertTrue(schemaCatalog.monsters.count { monster -> "elite" in monster.tags || monster.lootProfileId.endsWith(".elite") } >= 11)
        assertTrue(schemaCatalog.monsters.count { monster -> "boss" !in monster.tags && "elite" !in monster.tags && !monster.lootProfileId.endsWith(".elite") } >= 45)
        assertTrue(schemaCatalog.monsters.count { monster -> monster.talents.isNotEmpty() } >= 16)
        val guardProfileTighteningAssignments =
            schemaCatalog.monsters
                .filter { monster ->
                    monster.id in
                        setOf(
                            "goblin.scrapper",
                            "orc.miner",
                            "undead.chain_thrall",
                            "warded_ruin.vault_watcher",
                            "forge.slag_tender",
                            "river.undertow_brute",
                        )
                }.associate { monster -> monster.id to monster.aiProfileId }
        assertEquals(
            mapOf(
                "goblin.scrapper" to "ai.guard.basic",
                "orc.miner" to "ai.forge.guard",
                "undead.chain_thrall" to "ai.warded_ruin.sentinel",
                "warded_ruin.vault_watcher" to "ai.warded_ruin.sentinel",
                "forge.slag_tender" to "ai.forge.channeler",
                "river.undertow_brute" to "ai.river.lurker",
            ),
            guardProfileTighteningAssignments,
            "PR-20 guard-profile tightening drifted away from the frozen monster/profile mapping.",
        )
        assertTrue(
            guardProfileTighteningAssignments
                .filterKeys { monsterId -> monsterId != "goblin.scrapper" }
                .values
                .none { profileId -> profileId == "ai.guard.basic" },
            "PR-20 only permits goblin.scrapper to remain on ai.guard.basic as the explicit tutorial exception.",
        )
        val goblinScrapper = requireNotNull(monsterSchemasById["goblin.scrapper"]) { "Missing goblin.scrapper schema." }
        assertTrue(
            goblinScrapper.spawnFloors.maxOrNull() ?: 0 <= 2,
            "goblin.scrapper may keep ai.guard.basic only while it stays on the early/tutorial transition floors.",
        )
        val guardProfileTighteningTalentAssignments =
            mapOf(
                "orc.miner" to setOf("earthshaker", "linebreaker"),
                "undead.chain_thrall" to setOf("sanctuary", "ritual_break"),
                "warded_ruin.vault_watcher" to setOf("sanctuary", "ritual_break"),
                "forge.slag_tender" to setOf("cinder_burst", "inferno_orb"),
                "river.undertow_brute" to setOf("shadow_bind"),
            )
        guardProfileTighteningTalentAssignments.forEach { (monsterId, expectedTalentIds) ->
            val monster = requireNotNull(monsterSchemasById[monsterId]) { "Missing monster schema for '$monsterId'." }
            assertEquals(
                expectedTalentIds,
                monster.talents.keys,
                "PR-20 frozen talent hook drifted for '$monsterId'.",
            )
            val profileId = guardProfileTighteningAssignments.getValue(monsterId)
            val abilityIds =
                requireNotNull(aiProfilesById[profileId]) {
                    "Missing AI profile '$profileId'."
                }.actions
                    .mapNotNull { action -> action.abilityId }
                    .toSet()
            assertEquals(
                expectedTalentIds,
                abilityIds,
                "PR-20 profile '$profileId' drifted away from the frozen monster talent contract for '$monsterId'.",
            )
        }
        val guardBasicMonsterIds =
            schemaCatalog.monsters
                .filter { monster -> monster.aiProfileId == "ai.guard.basic" }
                .map { monster -> monster.id }
                .toSet()
        assertEquals(
            setOf("goblin.scrapper"),
            guardBasicMonsterIds,
            "ai.guard.basic must stay limited to the explicit tutorial exception after PR-20.",
        )
        val basicProfileMonsterIds =
            schemaCatalog.monsters
                .filter { monster -> monster.aiProfileId in setOf("ai.chase.basic", "ai.kite.basic", "ai.patrol.basic") }
                .map { monster -> monster.id }
                .toSet()
        assertTrue(
            basicProfileMonsterIds.size <= 4,
            "Basic AI tail must stay at <= 4 templates, actual=$basicProfileMonsterIds",
        )
        assertEquals(
            setOf("beast.rat", "beast.rat_scavenger", "goblin.scout", "undead.restless_skeleton"),
            basicProfileMonsterIds,
            "Remaining basic AI templates must stay on tutorial or low-threat filler roles only.",
        )
        assertTrue(setOf("beast", "bandit", "undead", "orc", "cultist", "goblin", "warded_ruin", "forge", "crystal", "river", "abyssal").all(familyTags::contains))
        setOf("bandit", "undead", "orc", "cultist", "goblin", "warded_ruin", "forge", "crystal", "river", "abyssal").forEach { family ->
            assertTrue(
                runtimeCatalog.values
                    .filter { monster -> family in monster.tags }
                    .any { monster -> monster.resistances.values.any { value -> value != 0 } },
                "Expected at least one $family monster with formal elemental resistance data.",
            )
        }
        val beasts = runtimeCatalog.values.filter { monster -> "beast" in monster.tags }
        val beastResistanceCarriers = beasts.filter { monster -> monster.resistances.values.any { value -> value != 0 } }
        assertTrue(beastResistanceCarriers.size <= 1, "Beast family should stay on a low/zero resistance baseline in Phase 2.")
        assertTrue(
            beastResistanceCarriers.all { monster ->
                monster.resistances.entries.count { (_, value) -> value != 0 } <= 1 &&
                    monster.resistances.values.all { value -> value in -10..10 }
            },
            "Any beast resistance deviation should remain a single mild signal in Phase 2.",
        )
        schemaCatalog.monsters.forEach { monster ->
            val runtimeMonster = requireNotNull(runtimeCatalog[monster.id]) { "Missing runtime monster projection for ${monster.id}" }
            assertTrue(monster.nameKey.startsWith("monster."))
            assertTrue(monster.nameKey.endsWith(".name"))
            assertTrue(monster.archetype.isNotBlank())
            assertTrue(monster.aiProfileId.isNotBlank())
            assertTrue(monster.lootProfileId.isNotBlank())
            assertTrue(monster.baseAccuracy > 0)
            assertTrue(monster.baseEvasion >= 0)
            assertEquals(
                monster.resistances.toSortedMap(),
                runtimeMonster.resistances.mapKeys { (type, _) -> type.name }.toSortedMap(),
                "Runtime resistance projection drifted for ${monster.id}.",
            )
            assertTrue(runtimeMonster.resistances.values.all { value -> value in -25..25 })
            assertTrue(
                runtimeMonster.resistances.entries.count { (type, value) -> type.isElemental && value != 0 } <= 2,
                "Monster ${monster.id} exceeds the Stage B non-zero resistance limit.",
            )
            assertTrue(
                runtimeMonster.resistances.keys.none { type -> type == DamageType.PHYSICAL },
                "Monster ${monster.id} should not define PHYSICAL resistance in Stage B.",
            )
            monster.talents.keys.forEach { talentId ->
                assertTrue(schemaCatalog.talents.any { it.id == talentId }, "Unknown monster talent $talentId")
            }
        }
    }

    @Test
    fun `route zones expose minimum resistance coverage across common special and elite samples`() {
        val loader = DataLoader()
        val schemaCatalog = loader.loadSchemaCatalog()
        val zoneCatalog = schemaCatalog.zones.associateBy { zone -> zone.id }
        val runtimeCatalog = loader.loadMonsterCatalog().monsters.associateBy { monster -> monster.id }
        val bossTemplateIdsByEncounterId = schemaCatalog.bossEncounters.associate { encounter -> encounter.id to encounter.templateId }
        val specialArchetypes = setOf("artillery", "controller")
        val signatureCarrierIdsByZone =
            mapOf(
                "shattered_outpost" to setOf("goblin.scrapper"),
            )

        listOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths").forEach { zoneId ->
            val zone = requireNotNull(zoneCatalog[zoneId]) { "Missing zone schema for $zoneId." }
            val commonPool = routeVisibleCommonMonsters(zone, runtimeCatalog)
            val eliteOrBossPool = routeVisibleEliteOrBossMonsters(zone, runtimeCatalog, bossTemplateIdsByEncounterId)
            val routePool = routeVisibleMonsters(zone, runtimeCatalog, bossTemplateIdsByEncounterId)
            val signatureCarrierIds = signatureCarrierIdsByZone[zoneId].orEmpty()

            assertTrue(
                routePool.count { monster -> monster.resistances.values.any { value -> value != 0 } } >= 2,
                "Zone $zoneId should expose at least 2 route-visible monsters with non-zero resistances.",
            )
            assertTrue(
                commonPool.any { monster ->
                    monster.archetype !in specialArchetypes &&
                        monster.resistances.values.any { value -> value != 0 }
                },
                "Zone $zoneId should keep at least one common frontline resistance sample.",
            )
            if (zoneId != "greenwood_fringe") {
                assertTrue(
                    routePool.any { monster ->
                        (monster.id in signatureCarrierIds || monster.archetype in specialArchetypes) &&
                            monster.resistances.values.any { value -> value != 0 }
                    },
                    "Zone $zoneId should keep at least one signature or ranged/caster resistance sample.",
                )
            }
            assertTrue(
                eliteOrBossPool.any { monster -> monster.resistances.values.any { value -> value != 0 } },
                "Zone $zoneId should keep at least one elite or boss resistance sample.",
            )
        }
    }

    @Test
    fun `route resistance coverage keeps fire shadow samples and holy tag vulnerability off the resistance baseline`() {
        val loader = DataLoader()
        val schemaCatalog = loader.loadSchemaCatalog()
        val zoneCatalog = schemaCatalog.zones
        val runtimeCatalog = loader.loadMonsterCatalog().monsters.associateBy { monster -> monster.id }
        val bossTemplateIdsByEncounterId = schemaCatalog.bossEncounters.associate { encounter -> encounter.id to encounter.templateId }
        val routePool =
            zoneCatalog
                .flatMap { zone -> routeVisibleMonsters(zone, runtimeCatalog, bossTemplateIdsByEncounterId) }
                .distinctBy { monster -> monster.id }

        assertTrue(
            routePool.any { monster -> (monster.resistances[DamageType.FIRE] ?: 0) > 0 },
            "Expected at least one route-visible monster with positive FIRE resistance.",
        )
        assertTrue(
            routePool.any { monster -> (monster.resistances[DamageType.FIRE] ?: 0) < 0 },
            "Expected at least one route-visible monster vulnerable to FIRE.",
        )
        assertTrue(
            routePool.any { monster -> (monster.resistances[DamageType.SHADOW] ?: 0) > 0 },
            "Expected at least one route-visible monster with positive SHADOW resistance.",
        )
        assertTrue(
            routePool.any { monster -> "undead" in monster.tags || "demon" in monster.tags },
            "Expected route-visible undead or demon monsters for HOLY tag bonus coverage.",
        )
        assertTrue(
            routePool.none { monster ->
                ("undead" in monster.tags || "demon" in monster.tags) &&
                    (monster.resistances[DamageType.HOLY] ?: 0) < 0
            },
            "HOLY vulnerability should come from the tag multiplier path, not default negative HOLY resistance.",
        )
    }
}
