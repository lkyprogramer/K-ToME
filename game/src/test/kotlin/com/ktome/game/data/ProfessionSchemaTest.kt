package com.ktome.game.data

import com.ktome.game.GameModule
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
        val inscriptionIds = catalog.inscriptions.map { it.id }.toSet()

        assertEquals(
            setOf("vanguard", "arcanist", "rogue", "templar", "berserker", "spellblade", "shadowblade", "warden"),
            catalog.professions.map { it.id }.toSet(),
        )
        catalog.professions.forEach { profession ->
            assertEquals("profession.${profession.id}.name", profession.nameKey)
            assertEquals("profession.${profession.id}.desc", profession.descKey)
            assertEquals("profession.${profession.id}.resource_hint", profession.resourceHintKey)
            assertTrue(profession.iconKey.startsWith("icon.profession."))
            assertTrue(profession.baseStats.str + profession.baseStats.dex + profession.baseStats.con + profession.baseStats.wil > 0)
            assertTrue(profession.statGrowth.str + profession.statGrowth.dex + profession.statGrowth.con + profession.statGrowth.wil > 0)
            assertTrue(profession.resourceProfiles.isNotEmpty())
            assertTrue(profession.resourceProfiles.size <= 2)
            assertTrue(profession.resourceProfiles.any { profile -> profile.axis == profession.primarySpendAxis })
            assertTrue(profession.soloContract.offenseTags.isNotEmpty())
            assertTrue(profession.soloContract.defenseTags.isNotEmpty())
            assertTrue(profession.soloContract.mobilityTags.isNotEmpty())
            assertTrue(profession.soloContract.aoeAnswerTags.isNotEmpty())
            assertTrue(profession.soloContract.bossAnswerTags.isNotEmpty())
            assertTrue(profession.soloContract.panicAnswerTags.isNotEmpty())
            profession.talentTrees.forEach { treeId -> assertTrue(treeIds.contains(treeId), "Unknown talent tree $treeId") }
            profession.startingTalents.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown starter talent $talentId") }
            profession.startingKit.forEach { itemId -> assertTrue(itemIds.contains(itemId), "Unknown starter item $itemId") }
            profession.startingInscriptions.forEach { inscriptionId ->
                assertTrue(inscriptionIds.contains(inscriptionId), "Unknown starter inscription $inscriptionId")
            }
        }
        listOf("vanguard", "arcanist", "rogue", "templar").forEach { professionId ->
            val profession = catalog.professions.first { it.id == professionId }
            val nodeCount =
                profession.talentTrees.sumOf { treeId ->
                    requireNotNull(catalog.talentTrees.firstOrNull { tree -> tree.id == treeId }) { "Missing talent tree $treeId" }.nodes.size
                }
            assertTrue(nodeCount >= 16, "Base profession '$professionId' must expose at least 16 formal talents, actual=$nodeCount")
        }
        assertEquals(2, catalog.professions.first { it.id == "spellblade" }.resourceProfiles.size)
        assertEquals(
            12,
            catalog.talentTrees.first { tree -> tree.id == "berserker_wrath" }.nodes.size +
                catalog.talentTrees.first { tree -> tree.id == "berserker_ruin" }.nodes.size +
                catalog.talentTrees.first { tree -> tree.id == "berserker_bloodwar" }.nodes.size,
        )
        assertEquals(
            12,
            catalog.talentTrees.first { tree -> tree.id == "spellblade_enchanted_blade" }.nodes.size +
                catalog.talentTrees.first { tree -> tree.id == "spellblade_elemental_flux" }.nodes.size +
                catalog.talentTrees.first { tree -> tree.id == "spellblade_battle_spell" }.nodes.size,
        )
        assertEquals(0, catalog.talentTrees.first { tree -> tree.id == "shadowblade_assassination_plus" }.nodes.size)
        assertEquals(0, catalog.talentTrees.first { tree -> tree.id == "warden_nature_guard" }.nodes.size)
        assertEquals("DEV_UNLOCKED", catalog.professions.first { it.id == "berserker" }.initialUnlockState.name)
        assertEquals("LOCKED", catalog.professions.first { it.id == "shadowblade" }.initialUnlockState.name)
        assertEquals("icon.profession.berserker", catalog.professions.first { it.id == "berserker" }.iconKey)
        assertEquals("icon.profession.spellblade", catalog.professions.first { it.id == "spellblade" }.iconKey)
        assertEquals("icon.profession.rogue", catalog.professions.first { it.id == "shadowblade" }.iconKey)
        assertEquals("icon.profession.templar", catalog.professions.first { it.id == "warden" }.iconKey)
        assertEquals(
            listOf("long_sword", "basic_shield", "chain_mail", "healing_potion"),
            catalog.professions.first { it.id == "vanguard" }.startingKit,
        )
        assertEquals(
            listOf("arcane_staff", "apprentice_robe", "mana_potion"),
            catalog.professions.first { it.id == "arcanist" }.startingKit,
        )
        assertEquals(
            listOf("healing_light", "phase_door"),
            catalog.professions.first { it.id == "rogue" }.startingInscriptions,
        )
    }

    @Test
    fun `player creation profession options expose canonical profession icons`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val schemaIconByProfessionId =
            catalog.professions.associate { profession -> profession.id to profession.iconKey }
        val creationState = GameModule.playerCreationState()

        creationState.professionOptions.forEach { option ->
            assertEquals(schemaIconByProfessionId.getValue(option.id), option.iconKey)
            assertTrue(option.iconKey.startsWith("icon.profession."))
        }
    }
}
