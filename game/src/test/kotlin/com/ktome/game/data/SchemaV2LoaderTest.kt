package com.ktome.game.data

import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SchemaV2LoaderTest {
    @Test
    fun `schema v2 loader reads full content catalog`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()

        assertEquals(setOf("vanguard", "arcanist", "rogue", "templar"), catalog.professions.map { it.id }.toSet())
        assertEquals(12, catalog.talentTrees.size)
        assertEquals(setOf("power_strike", "charge", "shield_bash", "war_cry"), catalog.talents.map { it.id }.toSet())
        assertEquals(setOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths"), catalog.zones.map { it.id }.toSet())
        assertEquals(setOf("normal"), catalog.difficulties.map { it.id }.toSet())
        assertTrue(catalog.visualKeys.contains("actor.vanguard"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.power_strike"))
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
}
