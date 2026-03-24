package com.ktome.core.ai

import kotlinx.serialization.Serializable

@Serializable
enum class AIDefaultBehavior {
    CHASE,
    KITE,
    PATROL,
    WAIT,
}

@Serializable
enum class AISelectionPolicy {
    DETERMINISTIC_PRIORITY,
    WEIGHTED_RANDOM,
}

@Serializable
enum class AIActionType {
    ATTACK_TARGET,
    MOVE_TOWARD_TARGET,
    RETREAT_FROM_TARGET,
    USE_ABILITY,
    WAIT,
}

@Serializable
data class AIAction(
    val id: String,
    val type: AIActionType,
    val orderKey: Int? = null,
    val weight: Double? = null,
    val condition: AICondition? = null,
    val abilityId: String? = null,
)

@Serializable
data class AIProfile(
    val id: String,
    val perceptionRange: Int,
    val useLastKnownPosition: Boolean,
    val defaultBehavior: AIDefaultBehavior,
    val selectionPolicy: AISelectionPolicy,
    val actions: List<AIAction>,
)
