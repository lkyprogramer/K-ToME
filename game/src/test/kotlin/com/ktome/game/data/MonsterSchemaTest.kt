package com.ktome.game.data

import com.ktome.core.combat.DamageType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MonsterSchemaTest {
    @Test
    fun `monster skeletons cover required family namespaces and combat contracts`() {
        val schemaCatalog = DataLoader().loadSchemaCatalog()
        val runtimeCatalog = DataLoader().loadMonsterCatalog().monsters.associateBy { monster -> monster.id }
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
                    "cultist.dungeon_lord",
                    "cultist.ember_adept",
                    "cultist.shadow_priest",
                    "cultist.ashgate_warden",
                ),
            ),
        )
        assertEquals(24, schemaCatalog.monsters.size)
        assertEquals(2, schemaCatalog.monsters.count { monster -> "boss" in monster.tags })
        assertEquals(4, schemaCatalog.monsters.count { monster -> "elite" in monster.tags || monster.lootProfileId.endsWith(".elite") })
        assertEquals(18, schemaCatalog.monsters.count { monster -> "boss" !in monster.tags && "elite" !in monster.tags && !monster.lootProfileId.endsWith(".elite") })
        assertTrue(setOf("beast", "bandit", "undead", "orc", "cultist", "goblin").all(familyTags::contains))
        setOf("bandit", "undead", "orc", "cultist", "goblin").forEach { family ->
            assertTrue(
                runtimeCatalog.values
                    .filter { monster -> family in monster.tags }
                    .any { monster -> monster.resistances.values.any { value -> value != 0 } },
                "Expected at least one $family monster with formal elemental resistance data.",
            )
        }
        assertTrue(
            runtimeCatalog.values
                .filter { monster -> "beast" in monster.tags }
                .all { monster -> monster.resistances.isEmpty() },
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
}
