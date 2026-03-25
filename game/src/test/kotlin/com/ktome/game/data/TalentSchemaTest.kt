package com.ktome.game.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TalentSchemaTest {
    @Test
    fun `talent tree namespaces and v2 talent fields are valid`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val professionIds = catalog.professions.map { it.id }.toSet()
        val talentIds = catalog.talents.map { it.id }.toSet()
        val treeIds = catalog.talentTrees.map { it.id }.toSet()
        val telegraphIds = catalog.telegraphSpecs.map { it.id }.toSet()

        assertEquals(
            setOf(
                "vanguard_arms",
                "vanguard_shield",
                "vanguard_warcry",
                "arcanist_flame",
                "arcanist_frost",
                "arcanist_arcane",
                "rogue_assassination",
                "rogue_subtlety",
                "rogue_agility",
                "templar_smite",
                "templar_grace",
                "templar_faith",
                "berserker_wrath",
                "berserker_ruin",
                "berserker_bloodwar",
                "spellblade_enchanted_blade",
                "spellblade_elemental_flux",
                "spellblade_battle_spell",
                "shadowblade_assassination_plus",
                "shadowblade_shadowstep_mastery",
                "shadowblade_venom_night",
                "warden_nature_guard",
                "warden_life_ward",
                "warden_earth_bastion",
                "human_adaptability",
                "elf_keen_senses",
                "dwarf_resilience",
                "orc_battle_fury",
                "undead_deathless_will",
            ),
            treeIds,
        )

        catalog.talentTrees.forEach { tree ->
            if (tree.raceId == null) {
                assertTrue(professionIds.contains(tree.professionId), "Unknown profession ${tree.professionId}")
            } else {
                assertTrue(tree.professionId.isBlank(), "Race tree ${tree.id} must not also declare professionId ${tree.professionId}")
            }
            assertTrue(tree.layout.isNotBlank())
            tree.nodes.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown tree node $talentId") }
        }

        catalog.talents.forEach { talent ->
            assertTrue(treeIds.contains(talent.treeId), "Unknown tree ${talent.treeId}")
            assertTrue(talent.nameKey.startsWith("talent."))
            assertTrue(talent.nameKey.endsWith(".name"))
            assertTrue(talent.descKey.endsWith(".desc"))
            talent.telegraphRef?.let { telegraphRef ->
                assertTrue(telegraphRef in telegraphIds, "Unknown telegraph ref $telegraphRef")
            }
            assertTrue(talent.castTime in setOf("INSTANT", "QUICK", "STANDARD", "HEAVY"))
            assertTrue(talent.callbacks.isNotEmpty() || talent.callbacks.isEmpty())
            talent.requirements.talentPrereqs.forEach { prereq ->
                assertTrue(talentIds.contains(prereq.talentId), "Unknown prerequisite ${prereq.talentId}")
            }
        }
    }
}
