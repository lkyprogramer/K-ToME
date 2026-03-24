package com.ktome.core.ai

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BossTraceExportTest {
    @Test
    fun `boss and ai traces export to json`() {
        val bossTrace =
            BossTrace(
                encounterId = "molten_giant",
                actorId = 7,
                fromPhase = "phase_full",
                toPhase = "phase_enraged",
                trigger = "hp_threshold",
                turnId = 42,
                sideEffects = listOf("TELEGRAPH:molten_giant_phase_warning"),
            )
        val decisionTrace =
            AIDecisionTrace(
                actorId = 7,
                profileId = "ai.boss.molten_giant.phase_enraged",
                turnId = 42,
                selectionPolicy = AISelectionPolicy.WEIGHTED_RANDOM,
                evaluatedActions = emptyList(),
                orderedCandidateActionIds = listOf("charge", "power_strike"),
                selectedActionId = "power_strike",
                rngRoll = 0.42,
                reason = "weighted_random",
            )

        val json = Json.encodeToString(bossTrace) + Json.encodeToString(decisionTrace)

        assertTrue(json.contains("phase_enraged"))
        assertTrue(json.contains("power_strike"))
    }
}
