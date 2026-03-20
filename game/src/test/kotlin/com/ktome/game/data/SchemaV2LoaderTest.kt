package com.ktome.game.data

import com.ktome.game.i18n.GameLocale
import com.ktome.core.talent.StatusEffectType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SchemaV2LoaderTest {
    @Test
    fun `schema v2 loader reads full content catalog`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()

        assertEquals(setOf("vanguard", "arcanist", "rogue", "templar"), catalog.professions.map { it.id }.toSet())
        assertEquals(12, catalog.talentTrees.size)
        assertTrue(
            catalog.talents.map { it.id }.toSet().containsAll(
                setOf("power_strike", "charge", "shield_bash", "war_cry", "fireball", "blink"),
            ),
        )
        assertEquals(setOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths"), catalog.zones.map { it.id }.toSet())
        assertEquals(setOf("armory_gate", "supply_crate", "alarm_bonfire"), catalog.interactables.map { it.id }.toSet())
        assertEquals(setOf("shattered_outpost_breach"), catalog.objectiveSets.map { it.id }.toSet())
        assertEquals(4, catalog.objectiveSets.single { it.id == "shattered_outpost_breach" }.placements.size)
        assertEquals(
            setOf("room_center", "boss_entry", "stairs_up", "player_start"),
            catalog.objectiveSets.single { it.id == "shattered_outpost_breach" }.placements.map { it.anchor }.toSet(),
        )
        assertEquals(setOf("normal"), catalog.difficulties.map { it.id }.toSet())
        assertTrue(catalog.visualKeys.contains("actor.vanguard"))
        assertTrue(catalog.visualKeys.contains("talent.arcanist.mana_surge.icon"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.power_strike"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.mana_surge"))
        assertEquals(
            listOf("basic_shield", "mana_potion", "scroll_teleport"),
            catalog.lootProfiles.first { it.id == "loot.foundation.boss" }.itemIds,
        )
        val banditCaptainAi = catalog.aiProfiles.first { it.id == "ai.boss.bandit_captain" }
        assertEquals(listOf("war_cry", "power_strike", "shield_bash"), banditCaptainAi.talentPriority)
        assertEquals(1, banditCaptainAi.skipRules.size)
        assertEquals(StatusEffectType.WAR_CRY_BUFF, banditCaptainAi.skipRules.single().selfHasStatus)
        assertTrue(catalog.arenas.any { it.id == "arena.shattered_outpost.boss" })
    }

    @Test
    fun `runtime projections follow active locale`() {
        val enLoader = DataLoader(GameLocale.EN_US)
        val zhLoader = DataLoader(GameLocale.ZH_CN)

        assertEquals("Rat", enLoader.loadMonsterCatalog().monsters.first { it.id == "beast.rat" }.name)
        assertEquals("老鼠", zhLoader.loadMonsterCatalog().monsters.first { it.id == "beast.rat" }.name)
        assertEquals("Short Sword", enLoader.loadItemBundle().baseItems.first { it.id == "short_sword" }.name)
        assertEquals("短剑", zhLoader.loadItemBundle().baseItems.first { it.id == "short_sword" }.name)
        assertEquals("Power Strike", enLoader.loadTalentDefinitions().first { it.id == "power_strike" }.name)
        assertEquals("猛击", zhLoader.loadTalentDefinitions().first { it.id == "power_strike" }.name)
        assertEquals("Fireball", enLoader.loadTalentDefinitions().first { it.id == "fireball" }.name)
        assertEquals("火球", zhLoader.loadTalentDefinitions().first { it.id == "fireball" }.name)
    }

    @Test
    fun `runtime monster projection keeps schema v2 contract fields available`() {
        val boss =
            DataLoader(GameLocale.EN_US)
                .loadMonsterCatalog()
                .monsters
                .first { monster -> monster.id == "cultist.dungeon_lord" }

        assertEquals("boss", boss.archetype)
        assertEquals("actor.cultist.dungeon_lord", boss.visualKey)
        assertEquals("icon.monster.cultist.dungeon_lord", boss.iconKey)
        assertEquals("audio.monster.cultist.dungeon_lord", boss.audioProfile)
        assertEquals("ai.boss.dungeon_lord", boss.aiProfileId)
        assertEquals("loot.foundation.boss", boss.lootProfileId)
        assertEquals(mapOf("war_cry" to 3, "power_strike" to 4), boss.talentLevels)
    }

    @Test
    fun `bandit captain slice keeps dedicated boss routing ids`() {
        val boss =
            DataLoader(GameLocale.EN_US)
                .loadMonsterCatalog()
                .monsters
                .first { monster -> monster.id == "bandit.captain" }
        val encounter =
            DataLoader(GameLocale.EN_US)
                .loadSchemaCatalog()
                .bossEncounters
                .first { schema -> schema.id == "bandit_captain_encounter" }

        assertEquals("ai.boss.bandit_captain", boss.aiProfileId)
        assertEquals("arena.shattered_outpost.boss", encounter.arenaId)
    }
}
