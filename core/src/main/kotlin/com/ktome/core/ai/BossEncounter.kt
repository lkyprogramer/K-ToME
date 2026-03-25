package com.ktome.core.ai

import kotlinx.serialization.Serializable

@Serializable
enum class BossPhaseTransitionTiming {
    START_OF_TURN,
    ALLOW_FATAL_TRANSITION,
}

@Serializable
enum class BossPhaseEventType {
    TELEGRAPH,
    CLEAR_STATUSES,
    INVULNERABLE,
    EMIT_EVENT,
}

@Serializable
data class BossPhaseEvent(
    val type: BossPhaseEventType,
    val telegraphSpecId: String? = null,
    val invulnerableTurns: Int? = null,
    val messageKey: String? = null,
)

@Serializable
data class BossPhaseDef(
    val id: String,
    val hpThreshold: Double? = null,
    val hpEnd: Double? = null,
    val turnCount: Int? = null,
    val requiredStatus: String? = null,
    val aiProfileId: String,
    val onEnter: List<BossPhaseEvent> = emptyList(),
    val resetAiPhaseState: Boolean = false,
    val transitionTiming: BossPhaseTransitionTiming = BossPhaseTransitionTiming.START_OF_TURN,
)

@Serializable
data class BossEncounter(
    val id: String,
    val templateId: String,
    val phases: List<BossPhaseDef>,
)

@Serializable
data class BossTrace(
    val encounterId: String,
    val actorId: Int,
    val fromPhase: String?,
    val toPhase: String,
    val trigger: String,
    val turnId: Int,
    val sideEffects: List<String>,
)
