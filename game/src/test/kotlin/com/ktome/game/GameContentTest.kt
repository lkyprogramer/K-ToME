package com.ktome.game

import com.ktome.core.talent.TalentRegistry
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.SchemaCatalog
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameContentTest {
    private val loader = DataLoader()
    private val baseSchemaCatalog = loader.loadSchemaCatalog()
    private val talents = loader.loadTalentDefinitions()

    @Test
    fun `boss variant loot override must resolve to a registered loot profile`() {
        val targetVariant = baseSchemaCatalog.bossVariants.first()
        val ex =
            assertThrows<IllegalArgumentException> {
                newContent(
                    baseSchemaCatalog.copy(
                        bossVariants =
                            baseSchemaCatalog.bossVariants.map { variant ->
                                if (variant.id == targetVariant.id) {
                                    variant.copy(lootProfileOverride = "loot.missing.profile")
                                } else {
                                    variant
                                }
                            },
                    ),
                )
            }

        assertTrue(ex.message.orEmpty().contains("unknown loot profile"))
    }

    @Test
    fun `boss variant action weight profile must stay inside base encounter action ids`() {
        val targetVariant = baseSchemaCatalog.bossVariants.first { variant -> variant.actionWeightProfileId != null }
        val targetProfileId = requireNotNull(targetVariant.actionWeightProfileId)
        val ex =
            assertThrows<IllegalArgumentException> {
                newContent(
                    baseSchemaCatalog.copy(
                        actionWeightProfiles =
                            baseSchemaCatalog.actionWeightProfiles.map { profile ->
                                if (profile.id == targetProfileId) {
                                    profile.copy(actionWeights = profile.actionWeights + ("non_exposed_action" to 1.0))
                                } else {
                                    profile
                                }
                            },
                    ),
                )
            }

        assertTrue(ex.message.orEmpty().contains("unknown base-encounter actions"))
    }

    private fun newContent(schemaCatalog: SchemaCatalog): GameContent =
        GameContent(
            talents = talents,
            statuses = schemaCatalog.statuses,
            statusCatalog = loader.loadStatusCatalog(),
            talentRegistry = TalentRegistry().apply { registerAll(talents) },
            monsterCatalog = loader.loadMonsterCatalog().monsters,
            itemBundle = loader.loadItemBundle(),
            bossDefinitions = loader.loadBossDefinitions(),
            schemaCatalog = schemaCatalog,
            localizer = loader.localizer,
        )
}
