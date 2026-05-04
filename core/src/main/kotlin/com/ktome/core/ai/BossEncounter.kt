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
sealed interface TriggerExpression {
    @Serializable
    data class Ref(val triggerId: String) : TriggerExpression {
        init {
            require(triggerId.isNotBlank()) { "TriggerExpression.Ref.triggerId must not be blank." }
        }
    }

    @Serializable
    data class AllOf(val children: List<TriggerExpression>) : TriggerExpression {
        init {
            require(children.size >= 2) { "TriggerExpression.AllOf must declare at least two children." }
        }
    }

    @Serializable
    data class AnyOf(val children: List<TriggerExpression>) : TriggerExpression {
        init {
            require(children.size >= 2) { "TriggerExpression.AnyOf must declare at least two children." }
        }
    }

    @Serializable
    data class Not(val child: TriggerExpression) : TriggerExpression
}

fun TriggerExpression.referenceIds(): Set<String> =
    when (this) {
        is TriggerExpression.Ref -> setOf(triggerId)
        is TriggerExpression.AllOf -> children.flatMapTo(linkedSetOf()) { child -> child.referenceIds() }
        is TriggerExpression.AnyOf -> children.flatMapTo(linkedSetOf()) { child -> child.referenceIds() }
        is TriggerExpression.Not -> child.referenceIds()
    }

@Serializable
data class BossPhaseOverride(
    val phaseId: String,
    val trigger: TriggerExpression,
    val telegraphSpecId: String,
    val actionEmphasisIds: List<String>,
    val onEnterEventKey: String,
) {
    init {
        require(phaseId.isNotBlank()) { "BossPhaseOverride.phaseId must not be blank." }
        require(telegraphSpecId.isNotBlank()) { "BossPhaseOverride.telegraphSpecId must not be blank." }
        require(actionEmphasisIds.isNotEmpty()) { "BossPhaseOverride.actionEmphasisIds must not be empty." }
        require(actionEmphasisIds.all(String::isNotBlank)) { "BossPhaseOverride.actionEmphasisIds must not contain blanks." }
        require(actionEmphasisIds.distinct().size == actionEmphasisIds.size) {
            "BossPhaseOverride.actionEmphasisIds must not contain duplicates."
        }
        require(onEnterEventKey.isNotBlank()) { "BossPhaseOverride.onEnterEventKey must not be blank." }
    }
}

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
    val phaseOverrides: List<BossPhaseOverride> = emptyList(),
) {
    init {
        require(phaseOverrides.map(BossPhaseOverride::phaseId).distinct().size == phaseOverrides.size) {
            "BossEncounter.phaseOverrides must not contain duplicate phase ids."
        }
    }
}

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
