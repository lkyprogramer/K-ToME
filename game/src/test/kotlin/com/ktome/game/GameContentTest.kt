package com.ktome.game

import com.ktome.core.ai.AIActionType
import com.ktome.core.ai.AICondition
import com.ktome.core.ai.TriggerExpression
import com.ktome.core.ai.referenceIds
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.world.solvability.DiscoveryPredicate
import com.ktome.core.world.solvability.DiscoveryPredicateType
import com.ktome.core.world.solvability.DiscoveryRule
import com.ktome.core.world.solvability.NodeAnchorId
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.elites.BossVariantDef
import com.ktome.game.hidden.HiddenEventRewardPayload
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `elite mutation granted talents must resolve to registered talents`() {
        val targetMutation = baseSchemaCatalog.eliteMutations.first { mutation -> mutation.grantedTalents.isNotEmpty() }
        val ex =
            assertThrows<IllegalArgumentException> {
                newContent(
                    baseSchemaCatalog.copy(
                        eliteMutations =
                            baseSchemaCatalog.eliteMutations.map { mutation ->
                                if (mutation.id == targetMutation.id) {
                                    mutation.copy(
                                        grantedTalents = listOf(com.ktome.game.elites.TalentGrantRef("elite.missing.talent")),
                                    )
                                } else {
                                    mutation
                                }
                            },
                    ),
                )
            }

        assertTrue(ex.message.orEmpty().contains("unknown granted talent"))
    }

    @Test
    fun `elite mutation ai profile overlay must resolve to registered profiles`() {
        val targetMutation = baseSchemaCatalog.eliteMutations.first { mutation -> mutation.aiProfileOverlay != null }
        val ex =
            assertThrows<IllegalArgumentException> {
                newContent(
                    baseSchemaCatalog.copy(
                        eliteMutations =
                            baseSchemaCatalog.eliteMutations.map { mutation ->
                                if (mutation.id == targetMutation.id) {
                                    mutation.copy(aiProfileOverlay = "ai.elite.missing_overlay")
                                } else {
                                    mutation
                                }
                            },
                    ),
                )
            }

        assertTrue(ex.message.orEmpty().contains("unknown AI profile overlay"))
    }

    @Test
    fun `elite mutation aura status must resolve to registered status schema`() {
        val targetMutation = baseSchemaCatalog.eliteMutations.first { mutation -> mutation.auraStatusId != null }
        val ex =
            assertThrows<IllegalArgumentException> {
                newContent(
                    baseSchemaCatalog.copy(
                        eliteMutations =
                            baseSchemaCatalog.eliteMutations.map { mutation ->
                                if (mutation.id == targetMutation.id) {
                                    mutation.copy(auraStatusId = "STATUS_MISSING")
                                } else {
                                    mutation
                                }
                            },
                    ),
                )
            }

        assertTrue(ex.message.orEmpty().contains("unknown aura status"))
    }

    @Test
    fun `frostbound and tidebound overlays keep close-range cold control windows`() {
        val frostboundProfile = baseSchemaCatalog.aiProfiles.first { profile -> profile.id == "ai.elite.frostbound" }
        val tideboundProfile = baseSchemaCatalog.aiProfiles.first { profile -> profile.id == "ai.elite.tidebound" }
        val retreat = frostboundProfile.actions.first { action -> action.id == "retreat" }
        val frostNova = frostboundProfile.actions.first { action -> action.id == "frost_nova" }
        val frostNovaCondition = frostNova.condition as? AICondition.And
        val glacialSeal = tideboundProfile.actions.first { action -> action.id == "glacial_seal" }
        val glacialSealCondition = glacialSeal.condition as? AICondition.And

        assertEquals(AIActionType.RETREAT_FROM_TARGET, retreat.type)
        assertEquals(AICondition.TargetDistanceLessThan(distance = 2), retreat.condition)
        assertEquals(AIActionType.USE_ABILITY, frostNova.type)
        assertEquals("elite_frost_nova", frostNova.abilityId)
        assertEquals(
            AICondition.TargetDistanceBetween(minDistance = 1, maxDistance = 2),
            frostNovaCondition?.conditions?.firstOrNull(),
        )
        assertEquals(
            AICondition.TalentReady(talentId = "elite_frost_nova"),
            frostNovaCondition?.conditions?.getOrNull(1),
        )
        assertEquals(AIActionType.USE_ABILITY, glacialSeal.type)
        assertEquals("glacial_seal", glacialSeal.abilityId)
        assertEquals(
            AICondition.TargetDistanceBetween(minDistance = 1, maxDistance = 6),
            glacialSealCondition?.conditions?.firstOrNull(),
        )
        assertEquals(
            AICondition.TalentReady(talentId = "glacial_seal"),
            glacialSealCondition?.conditions?.getOrNull(1),
        )
    }

    @Test
    fun `grey crown action weight profile keeps commander emphasis readable in data`() {
        val variant = baseSchemaCatalog.bossVariants.first { bossVariant -> bossVariant.id == "boss.variant.grey_crown" }
        val profile =
            baseSchemaCatalog.actionWeightProfiles.first { actionWeightProfile ->
                actionWeightProfile.id == requireNotNull(variant.actionWeightProfileId)
            }

        assertEquals(72.0, profile.actionWeights["battlefield_command"])
        assertEquals(70.0, profile.actionWeights["arcane_shield"])
        assertEquals(58.0, profile.actionWeights["ritual_break"])
        assertEquals(10.0, profile.actionWeights["shadow_bind"])
        assertEquals(4.0, profile.actionWeights["close_quarters"])
        assertEquals(3.0, profile.actionWeights["press_forward"])
        assertEquals(
            setOf("battlefield_command", "shadow_bind", "ritual_break", "arcane_shield", "close_quarters", "press_forward"),
            profile.actionWeights.keys,
        )
    }

    @Test
    fun `boss variants declare strict phase override language`() {
        val overridesByVariant =
            baseSchemaCatalog.bossVariants.associate { variant ->
                variant.id to variant.phaseOverrides
            }

        assertEquals(
            setOf("boss.variant.molten_glass", "boss.variant.grey_crown", "boss.variant.abyssal_eclipse"),
            overridesByVariant.keys,
        )
        assertEquals("phase_enraged", overridesByVariant.getValue("boss.variant.molten_glass").single().phaseId)
        assertEquals("molten_glass_phase_override_warning", overridesByVariant.getValue("boss.variant.molten_glass").single().telegraphSpecId)
        assertEquals(listOf("linebreaker", "earthshaker"), overridesByVariant.getValue("boss.variant.molten_glass").single().actionEmphasisIds)
        assertEquals("phase_desperate", overridesByVariant.getValue("boss.variant.grey_crown").single().phaseId)
        assertEquals("grey_crown_phase_override_warning", overridesByVariant.getValue("boss.variant.grey_crown").single().telegraphSpecId)
        assertEquals(listOf("battlefield_command", "ritual_break"), overridesByVariant.getValue("boss.variant.grey_crown").single().actionEmphasisIds)
        assertEquals("phase_abyssal", overridesByVariant.getValue("boss.variant.abyssal_eclipse").single().phaseId)
        assertEquals("abyssal_eclipse_phase_override_warning", overridesByVariant.getValue("boss.variant.abyssal_eclipse").single().telegraphSpecId)
        assertEquals(listOf("void_breach", "abyssal_consecration"), overridesByVariant.getValue("boss.variant.abyssal_eclipse").single().actionEmphasisIds)
        assertTriggerRefs(
            overridesByVariant.getValue("boss.variant.molten_glass").single().trigger,
            setOf("boss.trigger.hp_below_50", "zone.trigger.oil_or_fire_seen"),
        )
        assertTriggerRefs(
            overridesByVariant.getValue("boss.variant.grey_crown").single().trigger,
            setOf("boss.trigger.hp_below_45", "boss.trigger.war_caller_active"),
        )
        assertTriggerRefs(
            overridesByVariant.getValue("boss.variant.abyssal_eclipse").single().trigger,
            setOf("boss.trigger.hp_below_40", "zone.trigger.void_pressure_active"),
        )
    }

    @Test
    fun `boss variant phase override references fail fast during content load`() {
        val targetVariant = baseSchemaCatalog.bossVariants.first { variant -> variant.id == "boss.variant.molten_glass" }
        val targetOverride = targetVariant.phaseOverrides.single()
        val invalidCases =
            listOf(
                "unknown phase" to targetOverride.copy(phaseId = "phase_missing"),
                "unknown trigger facts" to targetOverride.copy(trigger = TriggerExpression.Ref("zone.trigger.missing")),
                "unknown telegraph" to targetOverride.copy(telegraphSpecId = "missing_phase_override_warning"),
                "unknown base-encounter actions" to targetOverride.copy(actionEmphasisIds = listOf("linebreaker", "missing_action")),
                "must be boss.variant.molten_glass.phase_override.entered" to
                    targetOverride.copy(onEnterEventKey = "boss.variant.grey_crown.phase_override.entered"),
            )

        invalidCases.forEach { (expectedMessage, invalidOverride) ->
            val ex =
                assertThrows<IllegalArgumentException> {
                    newContent(
                        baseSchemaCatalog.copy(
                            bossVariants =
                                baseSchemaCatalog.bossVariants.map { variant ->
                                    if (variant.id == targetVariant.id) {
                                        variant.withPhaseOverride(invalidOverride)
                                    } else {
                                        variant
                                    }
                                },
                        ),
                    )
                }

            assertTrue(
                ex.message.orEmpty().contains(expectedMessage),
                "Expected '$expectedMessage' in '${ex.message}'.",
            )
        }
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
        ).also(GameContent::validateEliteMutationContracts)

    private fun assertTriggerRefs(
        trigger: TriggerExpression,
        expectedRefs: Set<String>,
    ) {
        assertEquals(expectedRefs, trigger.referenceIds())
    }

    private fun BossVariantDef.withPhaseOverride(override: com.ktome.core.ai.BossPhaseOverride): BossVariantDef =
        copy(phaseOverrides = listOf(override))
}
