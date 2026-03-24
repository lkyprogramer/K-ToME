package com.ktome.game

import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.game.data.schema.TalentSchemaV2
import com.ktome.game.data.schema.TalentTargetingSchemaV2
import com.ktome.game.data.schema.TalentTreeSchemaV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TalentTreeOwnerResolverTest {
    private val resolver =
        TalentTreeOwnerResolver(
            mapOf(
                "profession_tree" to
                    TalentTreeSchemaV2(
                        id = "profession_tree",
                        professionId = "vanguard",
                        nameKey = "talent_tree.profession_tree.name",
                        descKey = "talent_tree.profession_tree.desc",
                        visualKey = "tree.profession",
                        iconKey = "tree.profession.icon",
                        audioProfile = "audio.tree.profession",
                        schemaVersion = 2,
                        tags = emptyList(),
                        layout = "grid",
                        nodes = listOf("power_strike"),
                    ),
                "race_tree" to
                    TalentTreeSchemaV2(
                        id = "race_tree",
                        professionId = "",
                        raceId = "shalore",
                        nameKey = "talent_tree.race_tree.name",
                        descKey = "talent_tree.race_tree.desc",
                        visualKey = "tree.race",
                        iconKey = "tree.race.icon",
                        audioProfile = "audio.tree.race",
                        schemaVersion = 2,
                        tags = emptyList(),
                        layout = "grid",
                        nodes = listOf("race_blessing"),
                    ),
            ),
        )

    @Test
    fun `resolver returns profession owner for profession tree talents`() {
        val owner = resolver.ownerForTalent(talentSchema("power_strike", "profession_tree"))

        assertEquals(TalentTreeOwnerRef(TalentTreeOwnerType.PROFESSION, "vanguard"), owner)
    }

    @Test
    fun `resolver returns race owner for race tree talents`() {
        val owner = resolver.ownerForTalent(talentSchema("race_blessing", "race_tree"))

        assertEquals(TalentTreeOwnerRef(TalentTreeOwnerType.RACE, "shalore"), owner)
    }

    @Test
    fun `owner inference returns null for mixed sets that require explicit tree selection`() {
        val owner =
            resolver.ownerForTalents(
                listOf(
                    talentSchema("power_strike", "profession_tree"),
                    talentSchema("race_blessing", "race_tree"),
                ),
            )

        assertNull(owner)
    }

    private fun talentSchema(
        id: String,
        treeId: String,
    ): TalentSchemaV2 =
        TalentSchemaV2(
            id = id,
            nameKey = "talent.$id.name",
            descKey = "talent.$id.desc",
            visualKey = "talent.$id.visual",
            iconKey = "talent.$id.icon",
            audioProfile = "audio.$id",
            schemaVersion = 2,
            tags = emptyList(),
            maxPoints = 5,
            tier = 1,
            category = "ACTIVE",
            damageType = null,
            powerDimension = null,
            kind = "ATTACK",
            cooldown = 5,
            castTime = "STANDARD",
            targeting = TalentTargetingSchemaV2(type = "SELF", range = 0, minRange = 0, areaRadius = 0),
            resourceCosts = emptyList(),
            unlockLevel = 1,
            requirements = com.ktome.game.data.schema.TalentRequirementsSchemaV2(),
            levelEffects = linkedMapOf(1 to com.ktome.game.data.schema.TalentLevelEffectSchemaV2()),
            breakpoints = emptyList(),
            keywords = emptyList(),
            callbacks = emptyList(),
            telegraphRef = null,
            aiHints = null,
            treeId = treeId,
        )
}
