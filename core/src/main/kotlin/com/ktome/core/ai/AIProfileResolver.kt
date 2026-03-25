package com.ktome.core.ai

import com.ktome.core.random.RandomSource

data class AIProfileDecisionContext(
    val actorId: Int,
    val turnId: Int,
    val selfHpRatio: Double,
    val targetHpRatio: Double? = null,
    val targetVisible: Boolean,
    val targetDistance: Int? = null,
    val selfStatusIds: Set<String> = emptySet(),
    val targetStatusIds: Set<String> = emptySet(),
    val usableAbilityIds: Set<String> = emptySet(),
    val currentEncounterTurn: Int = 0,
)

data class AIProfileDecision(
    val selectedAction: AIAction?,
    val trace: AIDecisionTrace,
)

object AIProfileResolver {
    fun decide(
        profile: AIProfile,
        context: AIProfileDecisionContext,
        randomSource: RandomSource? = null,
    ): AIProfileDecision {
        val orderedActions = profile.actions.sortedWith(compareBy<AIAction> { it.orderKey ?: Int.MAX_VALUE }.thenBy(AIAction::id))
        val evaluatedActions =
            orderedActions.map { action ->
                AIEvaluatedActionTrace(
                    actionId = action.id,
                    actionType = action.type,
                    orderKey = action.orderKey,
                    weight = action.weight,
                    abilityId = action.abilityId,
                    conditionMatched = matches(action, context),
                )
            }
        val candidates = orderedActions.filter { action -> matches(action, context) }
        if (candidates.isEmpty()) {
            return AIProfileDecision(
                selectedAction = null,
                trace =
                    AIDecisionTrace(
                        actorId = context.actorId,
                        profileId = profile.id,
                        turnId = context.turnId,
                        selectionPolicy = profile.selectionPolicy,
                        evaluatedActions = evaluatedActions,
                        orderedCandidateActionIds = emptyList(),
                        reason = "no_matching_action",
                    ),
            )
        }

        return when (profile.selectionPolicy) {
            AISelectionPolicy.DETERMINISTIC_PRIORITY -> {
                val selected = candidates.first()
                AIProfileDecision(
                    selectedAction = selected,
                    trace =
                        AIDecisionTrace(
                            actorId = context.actorId,
                            profileId = profile.id,
                            turnId = context.turnId,
                            selectionPolicy = profile.selectionPolicy,
                            evaluatedActions = evaluatedActions,
                            orderedCandidateActionIds = candidates.map(AIAction::id),
                            selectedActionId = selected.id,
                            reason = "deterministic_priority",
                        ),
                )
            }

            AISelectionPolicy.WEIGHTED_RANDOM -> weighted(profile, context, candidates, evaluatedActions, randomSource)
        }
    }

    private fun weighted(
        profile: AIProfile,
        context: AIProfileDecisionContext,
        candidates: List<AIAction>,
        evaluatedActions: List<AIEvaluatedActionTrace>,
        randomSource: RandomSource?,
    ): AIProfileDecision {
        val weightedCandidates = candidates.map { action -> action to (action.weight ?: 1.0) }
        val totalWeight = weightedCandidates.sumOf { (_, weight) -> weight.coerceAtLeast(0.0) }
        if (totalWeight <= 0.0) {
            val fallback = candidates.first()
            return AIProfileDecision(
                selectedAction = fallback,
                trace =
                    AIDecisionTrace(
                        actorId = context.actorId,
                        profileId = profile.id,
                        turnId = context.turnId,
                        selectionPolicy = profile.selectionPolicy,
                        evaluatedActions = evaluatedActions,
                        orderedCandidateActionIds = candidates.map(AIAction::id),
                        selectedActionId = fallback.id,
                        reason = "weight_sum_non_positive_fallback",
                    ),
            )
        }

        val rngRoll = requireNotNull(randomSource) {
            "RandomSource is required when selectionPolicy=WEIGHTED_RANDOM."
        }.nextDouble()
        var cursor = 0.0
        val selected =
            weightedCandidates.firstOrNull { (_, weight) ->
                cursor += weight.coerceAtLeast(0.0) / totalWeight
                rngRoll < cursor
            }?.first ?: candidates.last()

        return AIProfileDecision(
            selectedAction = selected,
            trace =
                AIDecisionTrace(
                    actorId = context.actorId,
                    profileId = profile.id,
                    turnId = context.turnId,
                    selectionPolicy = profile.selectionPolicy,
                    evaluatedActions = evaluatedActions,
                    orderedCandidateActionIds = candidates.map(AIAction::id),
                    selectedActionId = selected.id,
                    rngRoll = rngRoll,
                    reason = "weighted_random",
                ),
        )
    }

    private fun matches(
        action: AIAction,
        context: AIProfileDecisionContext,
    ): Boolean {
        if (action.type == AIActionType.USE_ABILITY) {
            val abilityId = action.abilityId ?: return false
            if (abilityId !in context.usableAbilityIds) {
                return false
            }
        }
        return action.condition?.let { condition -> AIConditionEvaluator.evaluate(condition, context) } ?: true
    }
}

object AIConditionEvaluator {
    fun evaluate(
        condition: AICondition,
        context: AIProfileDecisionContext,
    ): Boolean =
        when (condition) {
            AICondition.TargetVisible -> context.targetVisible
            is AICondition.TargetDistanceLessThan -> context.targetDistance?.let { distance -> distance < condition.distance } == true
            is AICondition.TargetDistanceAtMost -> context.targetDistance?.let { distance -> distance <= condition.distance } == true
            is AICondition.TargetDistanceBetween ->
                context.targetDistance?.let { distance -> distance in condition.minDistance..condition.maxDistance } == true
            is AICondition.TargetHpBelow -> context.targetHpRatio?.let { ratio -> ratio <= condition.threshold } == true
            is AICondition.HpBelow -> context.selfHpRatio <= condition.threshold
            is AICondition.HasStatus ->
                when (condition.scope) {
                    AIConditionScope.SELF -> condition.statusId in context.selfStatusIds
                    AIConditionScope.TARGET -> condition.statusId in context.targetStatusIds
                }
            is AICondition.TalentReady -> condition.talentId in context.usableAbilityIds
            is AICondition.TurnCountModulo ->
                condition.divisor > 0 && context.currentEncounterTurn % condition.divisor == condition.remainder
            is AICondition.And -> condition.conditions.all { nested -> evaluate(nested, context) }
            is AICondition.Or -> condition.conditions.any { nested -> evaluate(nested, context) }
            is AICondition.Not -> !evaluate(condition.condition, context)
        }
}
