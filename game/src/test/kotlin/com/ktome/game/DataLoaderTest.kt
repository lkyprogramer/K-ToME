package com.ktome.game

import com.ktome.core.ecs.AIType
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
}
