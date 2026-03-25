package com.ktome.core.ai

import com.ktome.core.map.Point

data class AIPerceptionState(
    var lastKnownTargetPosition: Point? = null,
)

data class PendingTelegraphState(
    val telegraphSpecId: String,
    val sourceAbilityId: String,
    var remainingTurns: Int,
    val targetPoint: Point,
    val queuedAbilityId: String? = null,
    val resolvedDangerLevel: DangerLevel,
)

data class BossEncounterState(
    val encounterId: String,
    var currentPhaseId: String? = null,
    var encounterTurnCount: Int = 0,
    var phaseTurnCount: Int = 0,
)
