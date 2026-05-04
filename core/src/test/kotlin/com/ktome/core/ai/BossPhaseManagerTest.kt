package com.ktome.core.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun `phase override triggers after base phase threshold matches`() {
        val encounter = phaseOverrideEncounter()

        val resolution =
            BossPhaseManager.resolvePhaseResolution(
                encounter = encounter,
                context =
                    BossPhaseEvaluationContext(
                        healthRatio = 0.49,
                        encounterTurnCount = 2,
                        activeTriggerIds = setOf("boss.trigger.hp_below_50", "zone.trigger.oil_or_fire_seen"),
                    ),
                currentPhaseId = "phase_full",
            )

        assertEquals("phase_enraged", resolution.phase.id)
        assertEquals("molten_glass_phase_override_warning", resolution.phaseOverride?.telegraphSpecId)
        assertTrue("phase_override" in resolution.matchedTriggers)
    }

    @Test
    fun `phase override does not bypass base phase threshold`() {
        val encounter = phaseOverrideEncounter()

        val resolution =
            BossPhaseManager.resolvePhaseResolution(
                encounter = encounter,
                context =
                    BossPhaseEvaluationContext(
                        healthRatio = 0.70,
                        encounterTurnCount = 2,
                        activeTriggerIds = setOf("boss.trigger.hp_below_50", "zone.trigger.oil_or_fire_seen"),
                    ),
                currentPhaseId = "phase_full",
            )

        assertEquals("phase_full", resolution.phase.id)
        assertNull(resolution.phaseOverride)
    }

    @Test
    fun `all any and not trigger expressions evaluate only active trigger ids`() {
        val encounter =
            phaseOverrideEncounter(
                trigger =
                    TriggerExpression.AllOf(
                        listOf(
                            TriggerExpression.AnyOf(
                                listOf(
                                    TriggerExpression.Ref("zone.trigger.oil_or_fire_seen"),
                                    TriggerExpression.Ref("zone.trigger.void_pressure_active"),
                                ),
                            ),
                            TriggerExpression.Not(TriggerExpression.Ref("boss.trigger.silenced")),
                        ),
                    ),
            )

        val matched =
            BossPhaseManager.resolvePhaseResolution(
                encounter = encounter,
                context =
                    BossPhaseEvaluationContext(
                        healthRatio = 0.49,
                        encounterTurnCount = 2,
                        activeTriggerIds = setOf("zone.trigger.void_pressure_active"),
                    ),
                currentPhaseId = "phase_full",
            )
        val skipped =
            BossPhaseManager.resolvePhaseResolution(
                encounter = encounter,
                context =
                    BossPhaseEvaluationContext(
                        healthRatio = 0.49,
                        encounterTurnCount = 2,
                        activeTriggerIds = setOf("zone.trigger.void_pressure_active", "boss.trigger.silenced"),
                    ),
                currentPhaseId = "phase_full",
            )

        assertEquals("phase_enraged", matched.phaseOverride?.phaseId)
        assertNull(skipped.phaseOverride)
        assertEquals("trigger_unmatched", skipped.phaseOverrideSkippedReason)
    }

    @Test
    fun `compound trigger expressions require at least two children`() {
        val ref = TriggerExpression.Ref("boss.trigger.hp_below_50")

        assertThrows<IllegalArgumentException> {
            TriggerExpression.AllOf(listOf(ref))
        }
        assertThrows<IllegalArgumentException> {
            TriggerExpression.AnyOf(listOf(ref))
        }
    }

    @Test
    fun `phase override is one shot per phase`() {
        val encounter = phaseOverrideEncounter()

        val resolution =
            BossPhaseManager.resolvePhaseResolution(
                encounter = encounter,
                context =
                    BossPhaseEvaluationContext(
                        healthRatio = 0.49,
                        encounterTurnCount = 2,
                        activeTriggerIds = setOf("boss.trigger.hp_below_50", "zone.trigger.oil_or_fire_seen"),
                        triggeredPhaseOverridePhaseIds = setOf("phase_enraged"),
                    ),
                currentPhaseId = "phase_enraged",
            )

        assertNull(resolution.phaseOverride)
        assertEquals("already_triggered", resolution.phaseOverrideSkippedReason)
    }

    private fun phaseOverrideEncounter(
        trigger: TriggerExpression =
            TriggerExpression.AllOf(
                listOf(
                    TriggerExpression.Ref("boss.trigger.hp_below_50"),
                    TriggerExpression.Ref("zone.trigger.oil_or_fire_seen"),
                ),
            ),
    ): BossEncounter =
        BossEncounter(
            id = "molten_giant",
            templateId = "orc.molten_giant",
            phases =
                listOf(
                    BossPhaseDef(id = "phase_full", hpThreshold = 1.0, hpEnd = 0.5, aiProfileId = "full"),
                    BossPhaseDef(id = "phase_enraged", hpThreshold = 0.5, hpEnd = 0.0, aiProfileId = "enraged"),
                ),
            phaseOverrides =
                listOf(
                    BossPhaseOverride(
                        phaseId = "phase_enraged",
                        trigger = trigger,
                        telegraphSpecId = "molten_glass_phase_override_warning",
                        actionEmphasisIds = listOf("linebreaker", "earthshaker"),
                        onEnterEventKey = "boss.variant.molten_glass.phase_override.entered",
                    ),
                ),
        )
}
