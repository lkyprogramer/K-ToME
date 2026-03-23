package com.ktome.core.combat

import kotlinx.serialization.Serializable

@Serializable
enum class CombatCorpusId {
    FORMULA,
    STATUS,
    INTEGRATION,
    LONG_RUN,
}

@Serializable
data class TraceEnvelope(
    val phaseId: String,
    val rulesetVersion: String,
    val traceSchemaVersion: String,
    val corpusId: CombatCorpusId,
)
