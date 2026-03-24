package com.ktome.core.combat

object CombatRuleset {
    const val PHASE_ID: String = "P3"
    const val RULESET_VERSION: String = "3.0.0"
    const val TRACE_SCHEMA_VERSION: String = "1"

    fun formulaEnvelope(): TraceEnvelope =
        TraceEnvelope(
            phaseId = PHASE_ID,
            rulesetVersion = RULESET_VERSION,
            traceSchemaVersion = TRACE_SCHEMA_VERSION,
            corpusId = CombatCorpusId.FORMULA,
        )

    fun statusEnvelope(): TraceEnvelope =
        TraceEnvelope(
            phaseId = PHASE_ID,
            rulesetVersion = RULESET_VERSION,
            traceSchemaVersion = TRACE_SCHEMA_VERSION,
            corpusId = CombatCorpusId.STATUS,
        )
}
