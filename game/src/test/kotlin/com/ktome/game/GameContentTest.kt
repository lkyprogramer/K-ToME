package com.ktome.game

import com.ktome.core.talent.TalentRegistry
import com.ktome.core.world.solvability.DiscoveryPredicate
import com.ktome.core.world.solvability.DiscoveryPredicateType
import com.ktome.core.world.solvability.DiscoveryRule
import com.ktome.core.world.solvability.NodeAnchorId
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.hidden.HiddenEventRewardPayload
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

    @Test
    fun `secret zone entry rule must stay identical to hidden entrance discovery rule`() {
        val targetSecretZone = baseSchemaCatalog.secretZones.first()
        val ex =
            assertThrows<IllegalArgumentException> {
                newContent(
                    baseSchemaCatalog.copy(
                        secretZones =
                            baseSchemaCatalog.secretZones.map { secretZone ->
                                if (secretZone.id != targetSecretZone.id) {
                                    secretZone
                                } else {
                                    secretZone.copy(
                                        entryRule =
                                            DiscoveryRule(
                                                predicates =
                                                    listOf(
                                                        DiscoveryPredicate(
                                                            type = DiscoveryPredicateType.PERCEPTION_CHECK,
                                                            difficulty = 99,
                                                        ),
                                                    ),
                                            ),
                                    )
                                }
                            },
                    ),
                )
            }

        assertTrue(ex.message.orEmpty().contains("entryRule"))
    }

    @Test
    fun `secret zone entrance anchor must stay identical to hidden entrance anchor`() {
        val targetSecretZone = baseSchemaCatalog.secretZones.first()
        val ex =
            assertThrows<IllegalArgumentException> {
                newContent(
                    baseSchemaCatalog.copy(
                        secretZones =
                            baseSchemaCatalog.secretZones.map { secretZone ->
                                if (secretZone.id != targetSecretZone.id) {
                                    secretZone
                                } else {
                                    secretZone.copy(entranceBindingId = NodeAnchorId("optional.branch.missing"))
                                }
                            },
                    ),
                )
            }

        assertTrue(ex.message.orEmpty().contains("entrance anchor"))
    }

    @Test
    fun `hidden reveal reward must target a registered entrance binding`() {
        val targetEvent =
            baseSchemaCatalog.hiddenEvents.first { hiddenEvent ->
                hiddenEvent.rewards.any { reward -> reward.payload is HiddenEventRewardPayload.RevealSecretZone }
            }
        val ex =
            assertThrows<IllegalArgumentException> {
                newContent(
                    baseSchemaCatalog.copy(
                        hiddenEvents =
                            baseSchemaCatalog.hiddenEvents.map { hiddenEvent ->
                                if (hiddenEvent.id != targetEvent.id) {
                                    hiddenEvent
                                } else {
                                    hiddenEvent.copy(
                                        rewards =
                                            hiddenEvent.rewards.map { reward ->
                                                when (val payload = reward.payload) {
                                                    is HiddenEventRewardPayload.RevealSecretZone ->
                                                        reward.copy(
                                                            payload =
                                                                payload.copy(
                                                                    bindingId = com.ktome.core.world.solvability.SearchBindingId("search.missing.binding"),
                                                                ),
                                                        )

                                                    else -> reward
                                                }
                                            },
                                    )
                                }
                            },
                    ),
                )
            }

        assertTrue(ex.message.orEmpty().contains("unknown search binding"))
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
