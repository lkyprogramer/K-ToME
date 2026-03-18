package com.ktome.game.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfessionSchemaTest {
    @Test
    fun `profession skeletons and starter contracts are fixed`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val treeIds = catalog.talentTrees.map { it.id }.toSet()
        val talentIds = catalog.talents.map { it.id }.toSet()
        val itemIds = catalog.itemBundle.items.map { it.id }.toSet()

        assertEquals(setOf("vanguard", "arcanist", "rogue", "templar"), catalog.professions.map { it.id }.toSet())
        catalog.professions.forEach { profession ->
            assertEquals("profession.${profession.id}.name", profession.nameKey)
            assertEquals("profession.${profession.id}.desc", profession.descKey)
            assertTrue(profession.baseStats.str + profession.baseStats.dex + profession.baseStats.con + profession.baseStats.wil > 0)
            assertTrue(profession.statGrowth.str + profession.statGrowth.dex + profession.statGrowth.con + profession.statGrowth.wil > 0)
            assertTrue(profession.startingResources.isNotEmpty())
            assertTrue(profession.soloContract.isNotBlank())
            profession.talentTrees.forEach { treeId -> assertTrue(treeIds.contains(treeId), "Unknown talent tree $treeId") }
            profession.startingTalents.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown starter talent $talentId") }
            profession.startingKit.forEach { itemId -> assertTrue(itemIds.contains(itemId), "Unknown starter item $itemId") }
        }
    }
}
