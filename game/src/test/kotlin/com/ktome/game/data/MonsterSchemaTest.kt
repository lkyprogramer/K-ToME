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
                    "goblin.scout",
                    "bandit.raider",
                    "bandit.archer",
                    "bandit.captain",
                    "undead.restless_skeleton",
                    "undead.bone_guard",
                    "undead.bone_archer",
                    "bandit.sentry",
                    "orc.raider",
                    "cultist.dungeon_lord",
                ),
            ),
        )
        assertTrue(setOf("beast", "bandit", "undead", "orc", "cultist", "goblin").all(familyTags::contains))
        catalog.monsters.forEach { monster ->
            assertTrue(monster.nameKey.startsWith("monster."))
            assertTrue(monster.nameKey.endsWith(".name"))
            assertTrue(monster.archetype.isNotBlank())
            assertTrue(monster.aiProfileId.isNotBlank())
            assertTrue(monster.lootProfileId.isNotBlank())
            monster.talents.keys.forEach { talentId ->
                assertTrue(catalog.talents.any { it.id == talentId }, "Unknown monster talent $talentId")
            }
        }
    }
}
