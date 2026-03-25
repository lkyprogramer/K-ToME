package com.ktome.core.ai

import kotlinx.serialization.Serializable

@Serializable
data class AIEvaluatedActionTrace(
    val actionId: String,
    val actionType: AIActionType,
    val orderKey: Int? = null,
    val weight: Double? = null,
    val abilityId: String? = null,
    val conditionMatched: Boolean,
)

@Serializable
data class AIDecisionTrace(
    val actorId: Int,
    val profileId: String,
    val turnId: Int,
    val selectionPolicy: AISelectionPolicy,
    val evaluatedActions: List<AIEvaluatedActionTrace>,
    val orderedCandidateActionIds: List<String>,
    val selectedActionId: String? = null,
    val rngRoll: Double? = null,
    val reason: String? = null,
)
