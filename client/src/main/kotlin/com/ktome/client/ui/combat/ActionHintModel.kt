package com.ktome.client.ui.combat

import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot

internal enum class ActionAvailability {
    AVAILABLE,
    DISABLED,
    UNKNOWN,
}

internal data class LegalTargetSummary(
    val count: Int?,
    val missingReason: RenderTextTokenSnapshot? = null,
)

internal data class TelegraphLinkageHint(
    val telegraphId: String,
    val dangerLevel: Int,
    val previewTurnsRemaining: Int,
)

internal data class ActionHintModel(
    val availability: ActionAvailability,
    val resourceCosts: List<RenderTextTokenSnapshot>,
    val cooldownTurns: Int?,
    val rangeSummary: RenderTextTokenSnapshot?,
    val legalTargetSummary: LegalTargetSummary,
    val disabledReason: RenderTextTokenSnapshot?,
    val telegraphLinkage: TelegraphLinkageHint?,
    val missingFactReason: RenderTextTokenSnapshot? = null,
)

internal object ActionHintModelBuilder {
    private val missingFactToken = RenderTextTokenSnapshot("ui.combat.fact.missing")

    fun build(
        snapshot: RenderSnapshot,
        actionId: String,
    ): ActionHintModel {
        val action =
            CombatDecisionFrame
                .availableActions(snapshot)
                .firstOrNull { candidate -> candidate.id == actionId }
                ?: return missingActionHint()
        return build(snapshot, action)
    }

    fun build(
        snapshot: RenderSnapshot,
        action: CombatActionOption,
    ): ActionHintModel {
        val disabledReason = CombatDecisionFrame.disabledReasonKey(snapshot, action)?.let(::RenderTextTokenSnapshot)
        val telegraphLinkage =
            snapshot.overlays
                .firstOrNull { overlay -> overlay.sourceAbilityId == action.sourceAbilityId }
                ?.let { overlay ->
                    TelegraphLinkageHint(
                        telegraphId = overlay.id,
                        dangerLevel = overlay.dangerLevel,
                        previewTurnsRemaining = overlay.previewTurns,
                    )
                }
        val missingTelegraphLinkage =
            if (telegraphLinkage == null && snapshot.overlays.isNotEmpty()) {
                missingFactToken
            } else {
                null
            }
        val legalTargetSummary =
            if (action.requiresTarget) {
                LegalTargetSummary(count = null, missingReason = missingFactToken)
            } else {
                LegalTargetSummary(count = 1)
            }
        val resourceCosts =
            if (action.resourceCost != null && action.resourceLabelKey != null) {
                listOf(
                    RenderTextTokenSnapshot(
                        key = "ui.combat.cost.line",
                        arguments =
                            listOf(
                                RenderTextArgumentSnapshot(name = "amount", value = action.resourceCost.toString()),
                                RenderTextArgumentSnapshot(name = "resource", valueKey = action.resourceLabelKey),
                            ),
                    ),
                )
            } else {
                emptyList()
            }
        val rangeSummary =
            if (action.minRange != null && action.range != null) {
                RenderTextTokenSnapshot(
                    key = "ui.combat.range.line",
                    arguments =
                        listOf(
                            RenderTextArgumentSnapshot(name = "min", value = action.minRange.toString()),
                            RenderTextArgumentSnapshot(name = "max", value = action.range.toString()),
                        ),
                )
            } else {
                null
            }
        val missingActionFact =
            if (
                action.resourceCost == null ||
                action.resourceLabelKey == null ||
                action.minRange == null ||
                action.range == null
            ) {
                missingFactToken
            } else {
                null
            }

        return ActionHintModel(
            availability = if (disabledReason == null) ActionAvailability.AVAILABLE else ActionAvailability.DISABLED,
            resourceCosts = resourceCosts,
            cooldownTurns = action.cooldownTurns,
            rangeSummary = rangeSummary,
            legalTargetSummary = legalTargetSummary,
            disabledReason = disabledReason,
            telegraphLinkage = telegraphLinkage,
            missingFactReason = missingActionFact ?: missingTelegraphLinkage,
        )
    }

    private fun missingActionHint(): ActionHintModel =
        ActionHintModel(
            availability = ActionAvailability.UNKNOWN,
            resourceCosts = emptyList(),
            cooldownTurns = null,
            rangeSummary = null,
            legalTargetSummary = LegalTargetSummary(count = null, missingReason = missingFactToken),
            disabledReason = RenderTextTokenSnapshot(CombatDecisionFeedbackKeys.UNKNOWN_ACTION),
            telegraphLinkage = null,
            missingFactReason = missingFactToken,
        )
}
