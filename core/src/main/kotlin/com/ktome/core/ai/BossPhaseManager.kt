package com.ktome.core.ai

data class BossPhaseEvaluationContext(
    val healthRatio: Double,
    val encounterTurnCount: Int,
    val activeStatusIds: Set<String> = emptySet(),
)

data class BossPhaseResolution(
    val phase: BossPhaseDef,
    val matchedTriggers: Set<String>,
)

object BossPhaseManager {
    fun resolvePhase(
        encounter: BossEncounter,
        context: BossPhaseEvaluationContext,
        currentPhaseId: String? = null,
        transitionTiming: BossPhaseTransitionTiming? = null,
    ): BossPhaseDef = resolvePhaseResolution(encounter, context, currentPhaseId, transitionTiming).phase

    fun resolvePhaseResolution(
        encounter: BossEncounter,
        context: BossPhaseEvaluationContext,
        currentPhaseId: String? = null,
        transitionTiming: BossPhaseTransitionTiming? = null,
    ): BossPhaseResolution {
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
            } ?: currentPhase
            ?: requireNotNull(
                encounter.phases.firstOrNull { phase ->
                    (transitionTiming == null || phase.transitionTiming == transitionTiming) &&
                        matches(phase, context)
                },
            ) {
                "Boss encounter '${encounter.id}' has no matching phase for hpRatio=${context.healthRatio} turnCount=${context.encounterTurnCount} timing=${transitionTiming ?: "ANY"}."
            }
        return BossPhaseResolution(
            phase = nextPhase,
            matchedTriggers =
                matchedTriggers(
                    phase = nextPhase,
                    context = context,
                    isInitialSelection = currentPhaseId == null && encounter.phases.firstOrNull()?.id == nextPhase.id,
                ),
        )
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
}
