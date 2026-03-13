package com.ktome.game

import com.ktome.core.ecs.AIType
import com.ktome.core.item.ItemType
import com.ktome.game.data.DataLoader
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataLoaderTest {
    private val loader = DataLoader()

    @Test
    fun `loads monster catalog from bundled yaml`() {
        val catalog = loader.loadMonsterCatalog()

        assertEquals(4, catalog.monsters.size)
        assertTrue(catalog.monsters.any { it.ai == AIType.CHASE })
        assertTrue(catalog.monsters.any { it.ai == AIType.KITE })
        assertTrue(catalog.monsters.any { it.ai == AIType.PATROL })
    }

    @Test
    fun `loads monster catalog from input stream`() {
        val yaml = """
            monsters:
              - id: scout
                name: Scout
                glyph: "s"
                color: "#FFFFFF"
                stats: { str: 2, dex: 4, con: 3, wil: 1 }
                baseHp: 9
                baseAttack: 2
                baseDefense: 1
                speed: 105
                ai: CHASE
                expReward: 8
                spawnFloors: [1]
                spawnWeight: 5
        """.trimIndent()

        val catalog = loader.loadMonsterCatalog(ByteArrayInputStream(yaml.toByteArray()))

        assertEquals("scout", catalog.monsters.single().id)
        assertEquals(105, catalog.monsters.single().speed)
    }

    @Test
    fun `loads bundled item data`() {
        val bundle = loader.loadItemBundle()

        assertTrue(bundle.baseItems.any { it.type == ItemType.WEAPON })
        assertTrue(bundle.baseItems.any { it.type == ItemType.CONSUMABLE })
        assertTrue(bundle.materials.any { it.id == "STEEL" })
        assertTrue(bundle.affixes.any { it.id == "sharp" })
    }

    @Test
    fun `loads bundled talent definitions`() {
        val talents = loader.loadTalentDefinitions()

        assertEquals(4, talents.size)
        assertTrue(talents.any { it.id == "power_strike" })
        assertTrue(talents.any { it.id == "war_cry" && it.range == 0 })
    }

    @Test
    fun `loads bundled boss definition`() {
        val boss = loader.loadBossDefinition()

        assertEquals("dungeon_lord", boss.template.id)
        assertEquals(4, boss.talentLevels["power_strike"])
        assertEquals(3, boss.talentLevels["war_cry"])
    }
}
