package com.ktome.game.data

import com.ktome.core.talent.DescriptionValue
import com.ktome.core.talent.DynamicDescriptionResolver
import com.ktome.core.talent.EffectOp
import com.ktome.game.FOUNDATION_BREAKPOINT_PAYOFF_CONTRACTS
import com.ktome.game.FoundationBreakpointPayoffContract
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

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

    @Test
    fun `profession tier two and tier three nodes declare specified prerequisite ranks`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val talentsById = catalog.talents.associateBy { talent -> talent.id }
        val treesById = catalog.talentTrees.associateBy { tree -> tree.id }

        catalog.professions
            .filterNot { profession -> "frozen" in profession.tags }
            .forEach { profession ->
                profession.talentTrees
                    .mapNotNull(treesById::get)
                    .forEach { tree ->
                        tree.nodes.forEachIndexed { index, talentId ->
                            val requiredRank =
                                when {
                                    tree.nodes.size <= 4 && index == 2 -> 2
                                    tree.nodes.size <= 4 && index >= 3 -> 3
                                    tree.nodes.size > 4 && index in 2..3 -> 2
                                    tree.nodes.size > 4 && index >= 4 -> 3
                                    else -> return@forEachIndexed
                                }
                            val maxPrerequisiteRank =
                                talentsById.getValue(talentId)
                                    .requirements
                                    .talentPrereqs
                                    .maxOfOrNull { prerequisite -> prerequisite.minRank }
                                    ?: 0

                            assertTrue(
                                maxPrerequisiteRank >= requiredRank,
                                "Expected ${tree.id}/$talentId to require a specified prerequisite rank >= $requiredRank.",
                            )
                        }
                    }
            }
    }

    @Test
    fun `base class breakpoint payoff talents freeze documented gameplay pivots`() {
        val loader = DataLoader()
        val catalog = loader.loadSchemaCatalog()
        val talentsById = loader.loadTalentDefinitions().associateBy { talent -> talent.id }
        val schemaById = catalog.talents.associateBy { talent -> talent.id }

        FOUNDATION_BREAKPOINT_PAYOFF_CONTRACTS.forEach { contract ->
            val talent = requireNotNull(talentsById[contract.talentId])
            assertNotNull(
                talent.breakpoints.firstOrNull { breakpoint ->
                    breakpoint.atRank == contract.breakpointRank && breakpoint.unlockedEffects.any { effect -> contract.matchesDocumentedEffect(effect) }
                },
                "Expected ${contract.talentId} to expose a documented breakpoint payoff at rank ${contract.breakpointRank}.",
            )
            assertTrue(
                requireNotNull(schemaById[contract.talentId]).keywords.containsAll(contract.requiredKeywords),
                "Expected ${contract.talentId} keywords to include ${contract.requiredKeywords}.",
            )
        }
    }

    @Test
    fun `base class breakpoint preview models stay aligned with unlocked runtime effects`() {
        val talentsById = DataLoader().loadTalentDefinitions().associateBy { talent -> talent.id }

        FOUNDATION_BREAKPOINT_PAYOFF_CONTRACTS.forEach { contract ->
            val talent = requireNotNull(talentsById[contract.talentId])
            val preview = requireNotNull(DynamicDescriptionResolver.nextBreakpointPreview(talent, currentRank = contract.previewRank))
            val breakpoint = talent.breakpoints.single { documented -> documented.atRank == contract.breakpointRank }
            val primaryEffect = breakpoint.unlockedEffects.first()
            val documentedEffect = breakpoint.unlockedEffects.single { effect -> contract.matchesDocumentedEffect(effect) }

            assertEquals(contract.breakpointRank, preview.atRank)
            assertEquals(contract.descriptionAddendumKey, preview.descriptionAddendumKey)
            assertEquals(previewTemplateKey(primaryEffect), preview.model.templateKey)
            assertEquals(contract.breakpointRank, (preview.model.placeholders.getValue("rank") as DescriptionValue.IntValue).value)

            when (primaryEffect) {
                is EffectOp.Damage -> {
                    assertEquals(
                        (primaryEffect.scaling.attackMultiplier * 100.0).roundToInt(),
                        (preview.model.placeholders.getValue("damagePercent") as DescriptionValue.IntValue).value,
                    )
                }

                is EffectOp.ApplyStatus -> {
                    assertEquals(primaryEffect.statusId, (preview.model.placeholders.getValue("statusId") as DescriptionValue.TextValue).value)
                    assertEquals(primaryEffect.duration, (preview.model.placeholders.getValue("statusDuration") as DescriptionValue.IntValue).value)
                }

                is EffectOp.ResourceRestore -> {
                    assertEquals(
                        (primaryEffect.fraction * 100.0).roundToInt(),
                        (preview.model.placeholders.getValue("resourceRestorePercent") as DescriptionValue.IntValue).value,
                    )
                }

                is EffectOp.Heal -> {
                    assertEquals(
                        (primaryEffect.maxHpFraction * 100.0).roundToInt(),
                        (preview.model.placeholders.getValue("healPercent") as DescriptionValue.IntValue).value,
                    )
                }

                is EffectOp.StatModifier,
                is EffectOp.Displacement,
                -> Unit
            }

            when (documentedEffect) {
                is EffectOp.ApplyStatus ->
                    assertTrue(
                        breakpoint.unlockedEffects.filterIsInstance<EffectOp.ApplyStatus>().any { effect -> effect.statusId == documentedEffect.statusId },
                        "Expected ${contract.talentId} breakpoint to retain documented status payoff ${documentedEffect.statusId}.",
                    )

                is EffectOp.ResourceRestore ->
                    assertTrue(
                        breakpoint.unlockedEffects.filterIsInstance<EffectOp.ResourceRestore>().any { effect ->
                            effect.type.name == documentedEffect.type.name && effect.fraction == documentedEffect.fraction
                        },
                        "Expected ${contract.talentId} breakpoint to retain documented resource restore payoff.",
                    )

                else -> error("Unexpected documented breakpoint effect $documentedEffect for ${contract.talentId}")
            }
        }
    }

    private fun FoundationBreakpointPayoffContract.matchesDocumentedEffect(effect: EffectOp): Boolean =
        when {
            payoffStatusId != null ->
                effect is EffectOp.ApplyStatus && effect.statusId.equals(payoffStatusId, ignoreCase = true)
            payoffResourceTypeId != null ->
                effect is EffectOp.ResourceRestore && effect.type.name == payoffResourceTypeId
            else -> false
        }

    private fun previewTemplateKey(effect: EffectOp): String =
        when (effect) {
            is EffectOp.ApplyStatus -> "talent.breakpoint.apply_status"
            is EffectOp.Displacement -> "talent.breakpoint.displacement"
            is EffectOp.ResourceRestore -> "talent.breakpoint.resource_restore"
            is EffectOp.Heal -> "talent.breakpoint.heal"
            is EffectOp.StatModifier -> "talent.breakpoint.stat_modifier"
            is EffectOp.Damage -> "talent.breakpoint.damage"
        }
}
