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
            ),
            treeIds,
        )

        catalog.talentTrees.forEach { tree ->
            assertTrue(professionIds.contains(tree.professionId), "Unknown profession ${tree.professionId}")
            assertTrue(tree.layout.isNotBlank())
            tree.nodes.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown tree node $talentId") }
        }

        catalog.talents.forEach { talent ->
            assertTrue(treeIds.contains(talent.treeId), "Unknown tree ${talent.treeId}")
            assertTrue(talent.nameKey.startsWith("talent."))
            assertTrue(talent.nameKey.endsWith(".name"))
            assertTrue(talent.descKey.endsWith(".desc"))
            assertTrue(talent.telegraph.isNotBlank())
            assertTrue(talent.callbacks.isNotEmpty() || talent.callbacks.isEmpty())
            talent.requirements.talentPrereqs.forEach { prereq ->
                assertTrue(talentIds.contains(prereq.talentId), "Unknown prerequisite ${prereq.talentId}")
            }
        }
    }
}
