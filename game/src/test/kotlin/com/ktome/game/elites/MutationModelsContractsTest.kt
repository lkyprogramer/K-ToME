package com.ktome.game.elites

import com.ktome.core.ai.AIAction
import com.ktome.core.ai.AIProfile
import com.ktome.core.ai.BossPhaseDef
import com.ktome.core.ai.BossPhaseOverride
import com.ktome.core.ai.TelegraphSpec
import com.ktome.core.ai.TriggerExpression
import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MutationModelsContractsTest {
    private val catalog = DataLoader().loadSchemaCatalog()
    private val aiProfilesById = catalog.aiProfiles.associateBy(AIProfile::id)
    private val targetVariant = catalog.bossVariants.first { variant -> variant.id == "boss.variant.molten_glass" }
    private val targetEncounter = catalog.bossEncounters.first { encounter -> encounter.id == targetVariant.baseEncounterId }
    private val targetOverride = targetVariant.phaseOverrides.single()
    private val phaseIds = targetEncounter.phases.mapTo(linkedSetOf(), BossPhaseDef::id)
    private val telegraphIds = catalog.telegraphSpecs.mapTo(linkedSetOf(), TelegraphSpec::id)
    private val allowedActionIds =
        targetEncounter.phases
            .flatMap { phase -> aiProfilesById.getValue(phase.aiProfileId).actions.map(AIAction::id) }
            .toSet()

    @Test
    fun `phase override reference validator rejects broken variant contracts`() {
        val invalidCases =
            listOf(
                "unknown phase" to targetVariant.withPhaseOverride(targetOverride.copy(phaseId = "phase_missing")),
                "unknown telegraph" to targetVariant.withPhaseOverride(targetOverride.copy(telegraphSpecId = "missing_phase_override_warning")),
                "unknown base-encounter actions" to
                    targetVariant.withPhaseOverride(targetOverride.copy(actionEmphasisIds = listOf("linebreaker", "missing_action"))),
                "unknown trigger facts" to
                    targetVariant.withPhaseOverride(targetOverride.copy(trigger = TriggerExpression.Ref("zone.trigger.missing"))),
                "must be boss.variant.molten_glass.phase_override.entered" to
                    targetVariant.withPhaseOverride(targetOverride.copy(onEnterEventKey = "boss.variant.grey_crown.phase_override.entered")),
                "must use 'boss.variant.<slug>'" to
                    targetVariant.copy(id = "boss.variant.MoltenGlass"),
            )

        invalidCases.forEach { (expectedMessage, invalidVariant) ->
            val ex =
                assertThrows<IllegalArgumentException> {
                    BossVariantPhaseOverrideContracts.validateReferences(
                        variant = invalidVariant,
                        phaseIds = phaseIds,
                        telegraphIds = telegraphIds,
                        allowedActionIds = allowedActionIds,
                    )
                }

            assertTrue(
                ex.message.orEmpty().contains(expectedMessage),
                "Expected '$expectedMessage' in '${ex.message}'.",
            )
        }
    }

    @Test
    fun `phase override reference validator accepts the shipped boss variant contracts`() {
        catalog.bossVariants.forEach { variant ->
            val encounter = catalog.bossEncounters.first { candidate -> candidate.id == variant.baseEncounterId }
            val allowedActions =
                encounter.phases
                    .flatMap { phase -> aiProfilesById.getValue(phase.aiProfileId).actions.map(AIAction::id) }
                    .toSet()

            BossVariantPhaseOverrideContracts.validateReferences(
                variant = variant,
                phaseIds = encounter.phases.mapTo(linkedSetOf(), BossPhaseDef::id),
                telegraphIds = telegraphIds,
                allowedActionIds = allowedActions,
            )
        }
    }

    private fun BossVariantDef.withPhaseOverride(override: BossPhaseOverride): BossVariantDef =
        copy(phaseOverrides = listOf(override))
}
