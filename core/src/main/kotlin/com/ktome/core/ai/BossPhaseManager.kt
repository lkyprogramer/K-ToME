package com.ktome.core.ai

data class BossPhaseEvaluationContext(
    val healthRatio: Double,
    val encounterTurnCount: Int,
    val activeStatusIds: Set<String> = emptySet(),
    val activeTriggerIds: Set<String> = emptySet(),
    val triggeredPhaseOverridePhaseIds: Set<String> = emptySet(),
)

data class BossPhaseResolution(
    val phase: BossPhaseDef,
    val matchedTriggers: Set<String>,
    val phaseOverride: BossPhaseOverride? = null,
    val phaseOverrideSkippedReason: String? = null,
)

object BossPhaseManager {
    fun resolvePhase(
        encounter: BossEncounter,
        context: BossPhaseEvaluationContext,
        currentPhaseId: String? = null,
        transitionTiming: BossPhaseTransitionTiming? = null,
    ): BossPhaseDef = resolvePhaseResolution(encounter, context, currentPhaseId, transitionTiming).phase

    fun resolvePhaseResolutionOrNull(
        encounter: BossEncounter,
        context: BossPhaseEvaluationContext,
        currentPhaseId: String? = null,
        transitionTiming: BossPhaseTransitionTiming? = null,
    ): BossPhaseResolution? {
        val currentPhase =
            currentPhaseId?.let { phaseId ->
                requireNotNull(encounter.phases.firstOrNull { phase -> phase.id == phaseId }) {
                    "Boss encounter '${encounter.id}' is missing current phase '$phaseId'."
                }
            }
        val nextPhase =
            encounter.phases.firstOrNull { phase ->
                phase.id != currentPhaseId &&
                    (transitionTiming == null || phase.transitionTiming == transitionTiming) &&
                    matches(phase, context)
            } ?: currentPhase?.takeIf { phase ->
                transitionTiming == null || phase.transitionTiming == transitionTiming
            }
            ?: encounter.phases.firstOrNull { phase ->
                (transitionTiming == null || phase.transitionTiming == transitionTiming) &&
                    matches(phase, context)
            }
            ?: return null
        val overrideMatch = resolvePhaseOverride(encounter, nextPhase, context)
        return BossPhaseResolution(
            phase = nextPhase,
            matchedTriggers =
                matchedTriggers(
                    phase = nextPhase,
                    context = context,
                    isInitialSelection = currentPhaseId == null && encounter.phases.firstOrNull()?.id == nextPhase.id,
                ) + listOfNotNull(overrideMatch.override?.let { "phase_override" }),
            phaseOverride = overrideMatch.override,
            phaseOverrideSkippedReason = overrideMatch.skippedReason,
        )
    }

    fun resolvePhaseResolution(
        encounter: BossEncounter,
        context: BossPhaseEvaluationContext,
        currentPhaseId: String? = null,
        transitionTiming: BossPhaseTransitionTiming? = null,
    ): BossPhaseResolution =
        requireNotNull(resolvePhaseResolutionOrNull(encounter, context, currentPhaseId, transitionTiming)) {
            "Boss encounter '${encounter.id}' has no matching phase for hpRatio=${context.healthRatio} turnCount=${context.encounterTurnCount} timing=${transitionTiming ?: "ANY"}."
        }

    private fun matches(
        phase: BossPhaseDef,
        context: BossPhaseEvaluationContext,
    ): Boolean {
        val hpMatches =
            when {
                phase.hpThreshold == null && phase.hpEnd == null -> true
                phase.hpThreshold != null && phase.hpEnd != null ->
                    context.healthRatio <= phase.hpThreshold &&
                        (
                            context.healthRatio > phase.hpEnd ||
                                (phase.hpEnd <= 0.0 && context.healthRatio >= 0.0)
                        )
                phase.hpThreshold != null -> context.healthRatio <= phase.hpThreshold
                else -> context.healthRatio > (phase.hpEnd ?: 0.0)
            }
        val turnMatches = phase.turnCount?.let { threshold -> context.encounterTurnCount >= threshold } ?: true
        val statusMatches = phase.requiredStatus?.let(context.activeStatusIds::contains) ?: true
        return hpMatches && turnMatches && statusMatches
    }

    private fun matchedTriggers(
        phase: BossPhaseDef,
        context: BossPhaseEvaluationContext,
        isInitialSelection: Boolean,
    ): Set<String> {
        if (isInitialSelection) {
            return linkedSetOf("initial_phase")
        }
        return linkedSetOf<String>().apply {
            if ((phase.hpThreshold != null || phase.hpEnd != null) && matchesHp(phase, context)) {
                add("hp_threshold")
            }
            if (phase.turnCount != null && context.encounterTurnCount >= phase.turnCount) {
                add("turn_count")
            }
            if (phase.requiredStatus != null && phase.requiredStatus in context.activeStatusIds) {
                add("required_status")
            }
        }
    }

    private fun resolvePhaseOverride(
        encounter: BossEncounter,
        phase: BossPhaseDef,
        context: BossPhaseEvaluationContext,
    ): PhaseOverrideMatch {
        val override = encounter.phaseOverrides.firstOrNull { candidate -> candidate.phaseId == phase.id } ?: return PhaseOverrideMatch()
        if (phase.id in context.triggeredPhaseOverridePhaseIds) {
            return PhaseOverrideMatch(skippedReason = "already_triggered")
        }
        return if (override.trigger.matches(context.activeTriggerIds)) {
            PhaseOverrideMatch(override = override)
        } else {
            PhaseOverrideMatch(skippedReason = "trigger_unmatched")
        }
    }

    private fun TriggerExpression.matches(activeTriggerIds: Set<String>): Boolean =
        when (this) {
            is TriggerExpression.Ref -> triggerId in activeTriggerIds
            is TriggerExpression.AllOf -> children.all { child -> child.matches(activeTriggerIds) }
            is TriggerExpression.AnyOf -> children.any { child -> child.matches(activeTriggerIds) }
            is TriggerExpression.Not -> !child.matches(activeTriggerIds)
        }

    private fun matchesHp(
        phase: BossPhaseDef,
        context: BossPhaseEvaluationContext,
    ): Boolean =
        when {
            phase.hpThreshold == null && phase.hpEnd == null -> true
            phase.hpThreshold != null && phase.hpEnd != null ->
                context.healthRatio <= phase.hpThreshold &&
                    (
                        context.healthRatio > phase.hpEnd ||
                            (phase.hpEnd <= 0.0 && context.healthRatio >= 0.0)
                    )
            phase.hpThreshold != null -> context.healthRatio <= phase.hpThreshold
            else -> context.healthRatio > (phase.hpEnd ?: 0.0)
        }

    private data class PhaseOverrideMatch(
        val override: BossPhaseOverride? = null,
        val skippedReason: String? = null,
    )
}
