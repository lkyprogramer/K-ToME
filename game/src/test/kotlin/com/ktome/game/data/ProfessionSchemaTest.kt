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

        assertEquals(
            setOf("vanguard", "arcanist", "rogue", "templar", "berserker", "spellblade", "shadowblade", "warden"),
            catalog.professions.map { it.id }.toSet(),
        )
        catalog.professions.forEach { profession ->
            assertEquals("profession.${profession.id}.name", profession.nameKey)
            assertEquals("profession.${profession.id}.desc", profession.descKey)
            assertTrue(profession.baseStats.str + profession.baseStats.dex + profession.baseStats.con + profession.baseStats.wil > 0)
            assertTrue(profession.statGrowth.str + profession.statGrowth.dex + profession.statGrowth.con + profession.statGrowth.wil > 0)
            assertTrue(profession.resourceProfiles.isNotEmpty())
            assertTrue(profession.resourceProfiles.size <= 2)
            assertTrue(profession.soloContract.offenseTags.isNotEmpty())
            assertTrue(profession.soloContract.defenseTags.isNotEmpty())
            assertTrue(profession.soloContract.mobilityTags.isNotEmpty())
            assertTrue(profession.soloContract.aoeAnswerTags.isNotEmpty())
            assertTrue(profession.soloContract.bossAnswerTags.isNotEmpty())
            assertTrue(profession.soloContract.panicAnswerTags.isNotEmpty())
            profession.talentTrees.forEach { treeId -> assertTrue(treeIds.contains(treeId), "Unknown talent tree $treeId") }
            profession.startingTalents.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown starter talent $talentId") }
            profession.startingKit.forEach { itemId -> assertTrue(itemIds.contains(itemId), "Unknown starter item $itemId") }
        }
        assertEquals(2, catalog.professions.first { it.id == "spellblade" }.resourceProfiles.size)
        assertEquals("DEV_UNLOCKED", catalog.professions.first { it.id == "berserker" }.initialUnlockState.name)
        assertEquals("LOCKED", catalog.professions.first { it.id == "shadowblade" }.initialUnlockState.name)
        assertEquals(
            listOf("long_sword", "basic_shield", "chain_mail", "healing_potion"),
            catalog.professions.first { it.id == "vanguard" }.startingKit,
        )
        assertEquals(
            listOf("arcane_staff", "apprentice_robe", "mana_potion"),
            catalog.professions.first { it.id == "arcanist" }.startingKit,
        )
    }
}
