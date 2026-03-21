package com.ktome.game.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MonsterSchemaTest {
    @Test
    fun `monster skeletons cover required family namespaces and combat contracts`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val monsterIds = catalog.monsters.map { it.id }.toSet()
        val familyTags = catalog.monsters.flatMap { it.tags }.toSet()

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
                    "cultist.dungeon_lord",
                    "cultist.ember_adept",
                    "cultist.shadow_priest",
                    "cultist.ashgate_warden",
                ),
            ),
        )
        assertEquals(24, catalog.monsters.size)
        assertEquals(2, catalog.monsters.count { monster -> "boss" in monster.tags })
        assertEquals(4, catalog.monsters.count { monster -> "elite" in monster.tags || monster.lootProfileId.endsWith(".elite") })
        assertEquals(18, catalog.monsters.count { monster -> "boss" !in monster.tags && "elite" !in monster.tags && !monster.lootProfileId.endsWith(".elite") })
        assertTrue(setOf("beast", "bandit", "undead", "orc", "cultist", "goblin").all(familyTags::contains))
        catalog.monsters.forEach { monster ->
            assertTrue(monster.nameKey.startsWith("monster."))
            assertTrue(monster.nameKey.endsWith(".name"))
            assertTrue(monster.archetype.isNotBlank())
            assertTrue(monster.aiProfileId.isNotBlank())
            assertTrue(monster.lootProfileId.isNotBlank())
            assertTrue(monster.baseAccuracy > 0)
            assertTrue(monster.baseEvasion >= 0)
            monster.talents.keys.forEach { talentId ->
                assertTrue(catalog.talents.any { it.id == talentId }, "Unknown monster talent $talentId")
            }
        }
    }
}
