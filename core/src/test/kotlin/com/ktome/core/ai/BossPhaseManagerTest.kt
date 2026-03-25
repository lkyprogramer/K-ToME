package com.ktome.core.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BossPhaseManagerTest {
    @Test
    fun `hp threshold picks the first matching descending phase`() {
        val encounter =
            BossEncounter(
                id = "molten_giant",
                templateId = "orc.molten_giant",
                phases =
                    listOf(
                        BossPhaseDef(id = "phase_full", hpThreshold = 1.0, hpEnd = 0.5, aiProfileId = "full"),
                        BossPhaseDef(id = "phase_enraged", hpThreshold = 0.5, hpEnd = 0.0, aiProfileId = "enraged"),
                    ),
            )

        val phase =
            BossPhaseManager.resolvePhase(
                encounter = encounter,
                context = BossPhaseEvaluationContext(healthRatio = 0.50, encounterTurnCount = 4),
            )

        assertEquals("phase_enraged", phase.id)
    }

    @Test
    fun `turn and status gates participate in phase selection`() {
        val encounter =
            BossEncounter(
                id = "dungeon_lord",
                templateId = "cultist.dungeon_lord",
                phases =
                    listOf(
                        BossPhaseDef(id = "phase_full", hpThreshold = 1.0, hpEnd = 0.7, aiProfileId = "full"),
                        BossPhaseDef(
                            id = "phase_locked",
                            hpThreshold = 0.7,
                            hpEnd = 0.0,
                            turnCount = 3,
                            requiredStatus = "enrage",
                            aiProfileId = "locked",
                        ),
                    ),
            )

        val phase =
            BossPhaseManager.resolvePhase(
                encounter = encounter,
                context =
                    BossPhaseEvaluationContext(
                        healthRatio = 0.6,
                        encounterTurnCount = 5,
                        activeStatusIds = setOf("enrage"),
                    ),
            )

        assertEquals("phase_locked", phase.id)
    }

    @Test
    fun `allow fatal transition returns null when no fatal phase matches`() {
        val encounter =
            BossEncounter(
                id = "bandit_captain",
                templateId = "bandit.captain",
                phases =
                    listOf(
                        BossPhaseDef(id = "phase_full", hpThreshold = 1.0, hpEnd = 0.0, aiProfileId = "full"),
                    ),
            )

        val resolution =
            BossPhaseManager.resolvePhaseResolutionOrNull(
                encounter = encounter,
                context = BossPhaseEvaluationContext(healthRatio = 0.0, encounterTurnCount = 0),
                currentPhaseId = "phase_full",
                transitionTiming = BossPhaseTransitionTiming.ALLOW_FATAL_TRANSITION,
            )

        assertNull(resolution)
    }
}
