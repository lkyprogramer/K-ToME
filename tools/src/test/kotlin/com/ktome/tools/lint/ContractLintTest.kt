package com.ktome.tools.lint

import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("contractLint")
class ContractLintTest {
    @Test
    fun `schema v2 contracts resolve all mandatory cross references`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val professionIds = catalog.professions.map { it.id }.toSet()
        val talentIds = catalog.talents.map { it.id }.toSet()
        val treeIds = catalog.talentTrees.map { it.id }.toSet()
        val monsterIds = catalog.monsters.map { it.id }.toSet()
        val bossIds = catalog.bossEncounters.map { it.id }.toSet()
        val itemIds = catalog.itemBundle.items.map { it.id }.toSet()
        val lootIds = catalog.lootProfiles.map { it.id }.toSet()
        val tilesetIds = catalog.tilesets.map { it.id }.toSet()
        val aiIds = catalog.aiProfiles.map { it.id }.toSet()
        val arenaIds = catalog.arenas.map { it.id }.toSet()
        val ambientIds = catalog.ambientProfiles.map { it.id }.toSet()

        catalog.professions.forEach { profession ->
            assertEquals(2, profession.schemaVersion)
            assertEquals("profession.${profession.id}.name", profession.nameKey)
            assertEquals("profession.${profession.id}.desc", profession.descKey)
            assertTrue(catalog.visualKeys.contains(profession.visualKey), "Unknown visual key ${profession.visualKey}")
            assertTrue(catalog.visualKeys.contains(profession.iconKey), "Unknown icon key ${profession.iconKey}")
            assertTrue(catalog.audioProfiles.contains(profession.audioProfile), "Unknown audio profile ${profession.audioProfile}")
            profession.talentTrees.forEach { treeId -> assertTrue(treeIds.contains(treeId), "Unknown profession tree $treeId") }
            profession.startingTalents.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown starter talent $talentId") }
            profession.startingKit.forEach { itemId -> assertTrue(itemIds.contains(itemId), "Unknown starter item $itemId") }
        }

        catalog.talentTrees.forEach { tree ->
            assertEquals(2, tree.schemaVersion)
            assertTrue(professionIds.contains(tree.professionId), "Unknown talent tree profession ${tree.professionId}")
            assertEquals("talent_tree.${tree.id}.name", tree.nameKey)
            assertEquals("talent_tree.${tree.id}.desc", tree.descKey)
            assertTrue(catalog.visualKeys.contains(tree.visualKey), "Unknown visual key ${tree.visualKey}")
            assertTrue(catalog.visualKeys.contains(tree.iconKey), "Unknown icon key ${tree.iconKey}")
            assertTrue(catalog.audioProfiles.contains(tree.audioProfile), "Unknown audio profile ${tree.audioProfile}")
            tree.nodes.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown tree node $talentId") }
        }

        val treeById = catalog.talentTrees.associateBy { it.id }
        catalog.talents.forEach { talent ->
            assertEquals(2, talent.schemaVersion)
            assertTrue(treeIds.contains(talent.treeId), "Unknown talent tree ${talent.treeId}")
            val tree = requireNotNull(treeById[talent.treeId]) { "Unknown tree ${talent.treeId}" }
            assertEquals("talent.${tree.professionId}.${talent.id}.name", talent.nameKey)
            assertEquals("talent.${tree.professionId}.${talent.id}.desc", talent.descKey)
            assertTrue(catalog.visualKeys.contains(talent.visualKey), "Unknown visual key ${talent.visualKey}")
            assertTrue(catalog.visualKeys.contains(talent.iconKey), "Unknown icon key ${talent.iconKey}")
            assertTrue(catalog.audioProfiles.contains(talent.audioProfile), "Unknown audio profile ${talent.audioProfile}")
            talent.requirements.talentPrereqs.forEach { prereq ->
                assertTrue(talentIds.contains(prereq.talentId), "Unknown talent prerequisite ${prereq.talentId}")
            }
        }

        catalog.monsters.forEach { monster ->
            assertEquals(2, monster.schemaVersion)
            assertTrue(monster.id.contains('.'), "Monster id must use family namespace: ${monster.id}")
            assertTrue(monster.nameKey.startsWith("monster.${monster.id}."), "Monster key must follow monster.<family>.<id> namespace: ${monster.nameKey}")
            assertTrue(catalog.visualKeys.contains(monster.visualKey), "Unknown visual key ${monster.visualKey}")
            assertTrue(catalog.visualKeys.contains(monster.iconKey), "Unknown icon key ${monster.iconKey}")
            assertTrue(catalog.audioProfiles.contains(monster.audioProfile), "Unknown audio profile ${monster.audioProfile}")
            assertTrue(aiIds.contains(monster.aiProfileId), "Unknown AI profile ${monster.aiProfileId}")
            assertTrue(lootIds.contains(monster.lootProfileId), "Unknown loot profile ${monster.lootProfileId}")
            monster.talents.keys.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown monster talent $talentId") }
        }

        catalog.bossEncounters.forEach { boss ->
            assertEquals(2, boss.schemaVersion)
            assertEquals("boss.${boss.bossTemplateId}.name", boss.nameKey)
            assertEquals("boss.${boss.bossTemplateId}.desc", boss.descKey)
            assertTrue(catalog.visualKeys.contains(boss.visualKey), "Unknown visual key ${boss.visualKey}")
            assertTrue(catalog.visualKeys.contains(boss.iconKey), "Unknown icon key ${boss.iconKey}")
            assertTrue(catalog.audioProfiles.contains(boss.audioProfile), "Unknown audio profile ${boss.audioProfile}")
            assertTrue(monsterIds.contains(boss.bossTemplateId), "Unknown boss template ${boss.bossTemplateId}")
            assertTrue(arenaIds.contains(boss.arenaId), "Unknown arena ${boss.arenaId}")
            boss.rewards.forEach { rewardId -> assertTrue(lootIds.contains(rewardId), "Unknown boss reward $rewardId") }
        }

        catalog.zones.forEach { zone ->
            assertEquals(2, zone.schemaVersion)
            assertEquals("zone.${zone.id}.name", zone.nameKey)
            assertEquals("zone.${zone.id}.desc", zone.descKey)
            assertTrue(zone.floorCount > 0, "Zone floorCount must stay positive for ${zone.id}")
            assertTrue(zone.mapSize.width > 0 && zone.mapSize.height > 0, "Zone mapSize must stay positive for ${zone.id}")
            assertTrue(catalog.visualKeys.contains(zone.visualKey), "Unknown visual key ${zone.visualKey}")
            assertTrue(catalog.visualKeys.contains(zone.iconKey), "Unknown icon key ${zone.iconKey}")
            assertTrue(catalog.audioProfiles.contains(zone.audioProfile), "Unknown audio profile ${zone.audioProfile}")
            assertTrue(tilesetIds.contains(zone.tilesetKey), "Unknown tileset ${zone.tilesetKey}")
            assertTrue(ambientIds.contains(zone.ambientProfile), "Unknown ambient profile ${zone.ambientProfile}")
            zone.monsterPools.forEach { monsterId -> assertTrue(monsterIds.contains(monsterId), "Unknown zone monster $monsterId") }
            zone.elitePools.forEach { monsterId -> assertTrue(monsterIds.contains(monsterId), "Unknown zone elite $monsterId") }
            zone.bossEncounterId?.let { bossId -> assertTrue(bossIds.contains(bossId), "Unknown boss encounter $bossId") }
        }

        catalog.itemBundle.materials.forEach { material ->
            assertEquals(2, material.schemaVersion)
            assertEquals("material.${material.id.lowercase()}.name", material.nameKey)
            assertEquals("material.${material.id.lowercase()}.desc", material.descKey)
            assertTrue(catalog.visualKeys.contains(material.visualKey), "Unknown visual key ${material.visualKey}")
            assertTrue(catalog.visualKeys.contains(material.iconKey), "Unknown icon key ${material.iconKey}")
            assertTrue(catalog.audioProfiles.contains(material.audioProfile), "Unknown audio profile ${material.audioProfile}")
        }
        catalog.itemBundle.affixes.forEach { affix ->
            assertEquals(2, affix.schemaVersion)
            assertEquals("affix.${affix.id}.name", affix.nameKey)
            assertEquals("affix.${affix.id}.desc", affix.descKey)
            assertTrue(catalog.visualKeys.contains(affix.visualKey), "Unknown visual key ${affix.visualKey}")
            assertTrue(catalog.visualKeys.contains(affix.iconKey), "Unknown icon key ${affix.iconKey}")
            assertTrue(catalog.audioProfiles.contains(affix.audioProfile), "Unknown audio profile ${affix.audioProfile}")
        }
        catalog.itemBundle.items.forEach { item ->
            assertEquals(2, item.schemaVersion)
            assertEquals("item.${item.id}.name", item.nameKey)
            assertEquals("item.${item.id}.desc", item.descKey)
            assertTrue(catalog.visualKeys.contains(item.visualKey), "Unknown visual key ${item.visualKey}")
            assertTrue(catalog.visualKeys.contains(item.iconKey), "Unknown icon key ${item.iconKey}")
            assertTrue(catalog.audioProfiles.contains(item.audioProfile), "Unknown audio profile ${item.audioProfile}")
            item.materials.forEach { materialId -> assertTrue(catalog.itemBundle.materials.any { it.id == materialId }, "Unknown material $materialId") }
        }

        catalog.difficulties.forEach { difficulty ->
            assertEquals(2, difficulty.schemaVersion)
            assertEquals("difficulty.${difficulty.id}.name", difficulty.nameKey)
            assertEquals("difficulty.${difficulty.id}.desc", difficulty.descKey)
            assertTrue(catalog.visualKeys.contains(difficulty.visualKey), "Unknown visual key ${difficulty.visualKey}")
            assertTrue(catalog.visualKeys.contains(difficulty.iconKey), "Unknown icon key ${difficulty.iconKey}")
            assertTrue(catalog.audioProfiles.contains(difficulty.audioProfile), "Unknown audio profile ${difficulty.audioProfile}")
        }
    }

    @Test
    fun `phase2 skeleton ids and family namespaces stay frozen`() {
        val catalog = DataLoader().loadSchemaCatalog()

        assertEquals(setOf("vanguard", "arcanist", "rogue", "templar"), catalog.professions.map { it.id }.toSet())
        assertEquals(setOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths"), catalog.zones.map { it.id }.toSet())
        assertEquals(
            mapOf(
                "shattered_outpost" to 2,
                "greenwood_fringe" to 2,
                "deep_iron_pit" to 2,
                "grey_gate_depths" to 2,
            ),
            catalog.zones.associate { zone -> zone.id to zone.floorCount },
        )
        assertEquals(
            mapOf(
                "shattered_outpost" to null,
                "greenwood_fringe" to null,
                "deep_iron_pit" to null,
                "grey_gate_depths" to "dungeon_lord_encounter",
            ),
            catalog.zones.associate { zone -> zone.id to zone.bossEncounterId },
        )
        assertEquals(setOf("normal"), catalog.difficulties.map { it.id }.toSet())
        assertEquals(
            setOf("beast.rat", "undead.bone_archer", "bandit.sentry", "orc.raider", "cultist.dungeon_lord"),
            catalog.monsters.map { it.id }.toSet(),
        )

        val uniqueNameKeys =
            buildList {
                addAll(catalog.professions.map { it.nameKey })
                addAll(catalog.talentTrees.map { it.nameKey })
                addAll(catalog.talents.map { it.nameKey })
                addAll(catalog.monsters.map { it.nameKey })
                addAll(catalog.bossEncounters.map { it.nameKey })
                addAll(catalog.zones.map { it.nameKey })
                addAll(catalog.difficulties.map { it.nameKey })
                addAll(catalog.itemBundle.materials.map { it.nameKey })
                addAll(catalog.itemBundle.affixes.map { it.nameKey })
                addAll(catalog.itemBundle.items.map { it.nameKey })
            }
        assertEquals(uniqueNameKeys.size, uniqueNameKeys.toSet().size, "nameKey values must stay unique.")

        val uniqueIds =
            buildList {
                addAll(catalog.professions.map { "profession:${it.id}" })
                addAll(catalog.talentTrees.map { "talent_tree:${it.id}" })
                addAll(catalog.talents.map { "talent:${it.id}" })
                addAll(catalog.monsters.map { "monster:${it.id}" })
                addAll(catalog.bossEncounters.map { "boss:${it.id}" })
                addAll(catalog.zones.map { "zone:${it.id}" })
                addAll(catalog.difficulties.map { "difficulty:${it.id}" })
                addAll(catalog.itemBundle.materials.map { "material:${it.id}" })
                addAll(catalog.itemBundle.affixes.map { "affix:${it.id}" })
                addAll(catalog.itemBundle.items.map { "item:${it.id}" })
            }
        assertEquals(uniqueIds.size, uniqueIds.toSet().size, "Object ids must stay unique inside their namespaces.")
    }
}
