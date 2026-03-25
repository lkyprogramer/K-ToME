package com.ktome.core.ai

import com.ktome.core.random.SplitMix64RandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AIProfileResolverTest {
    @Test
    fun `deterministic selection uses order key then action id`() {
        val profile =
            AIProfile(
                id = "deterministic",
                perceptionRange = 8,
                useLastKnownPosition = true,
                defaultBehavior = AIDefaultBehavior.CHASE,
                selectionPolicy = AISelectionPolicy.DETERMINISTIC_PRIORITY,
                actions =
                    listOf(
                        AIAction(id = "zeta", type = AIActionType.WAIT, orderKey = 20),
                        AIAction(id = "alpha", type = AIActionType.WAIT, orderKey = 10),
                        AIAction(id = "beta", type = AIActionType.WAIT, orderKey = 10),
                    ),
            )

        val decision =
            AIProfileResolver.decide(
                profile = profile,
                context = baselineDecisionContext(),
            )

        assertEquals("alpha", decision.selectedAction?.id)
        assertEquals(listOf("alpha", "beta", "zeta"), decision.trace.orderedCandidateActionIds)
    }

    @Test
    fun `weighted selection falls back when all weights are non positive`() {
        val profile =
            AIProfile(
                id = "weighted",
                perceptionRange = 8,
                useLastKnownPosition = true,
                defaultBehavior = AIDefaultBehavior.CHASE,
                selectionPolicy = AISelectionPolicy.WEIGHTED_RANDOM,
                actions =
                    listOf(
                        AIAction(id = "advance", type = AIActionType.MOVE_TOWARD_TARGET, orderKey = 10, weight = 0.0),
                        AIAction(id = "wait", type = AIActionType.WAIT, orderKey = 20, weight = -2.0),
                    ),
            )

        val decision =
            AIProfileResolver.decide(
                profile = profile,
                context = baselineDecisionContext(),
                randomSource = SplitMix64RandomSource.fromSeed(7L),
            )

        assertEquals("advance", decision.selectedAction?.id)
        assertEquals("weight_sum_non_positive_fallback", decision.trace.reason)
    }

    @Test
    fun `conditions filter unusable ability actions`() {
        val profile =
            AIProfile(
                id = "abilities",
                perceptionRange = 8,
                useLastKnownPosition = true,
                defaultBehavior = AIDefaultBehavior.CHASE,
                selectionPolicy = AISelectionPolicy.DETERMINISTIC_PRIORITY,
                actions =
                    listOf(
                        AIAction(
                            id = "power_strike",
                            type = AIActionType.USE_ABILITY,
                            orderKey = 10,
                            abilityId = "power_strike",
                            condition = AICondition.TalentReady("power_strike"),
                        ),
                    ),
            )

        val decision =
            AIProfileResolver.decide(
                profile = profile,
                context = baselineDecisionContext(usableAbilityIds = emptySet()),
            )

        assertNull(decision.selectedAction)
        assertEquals("no_matching_action", decision.trace.reason)
    }

    @Test
    fun `weighted selection records rng roll when a positive weight candidate is chosen`() {
        val profile =
            AIProfile(
                id = "weighted-positive",
                perceptionRange = 8,
                useLastKnownPosition = true,
                defaultBehavior = AIDefaultBehavior.CHASE,
                selectionPolicy = AISelectionPolicy.WEIGHTED_RANDOM,
                actions =
                    listOf(
                        AIAction(id = "advance", type = AIActionType.MOVE_TOWARD_TARGET, orderKey = 10, weight = 1.0),
                        AIAction(id = "retreat", type = AIActionType.RETREAT_FROM_TARGET, orderKey = 20, weight = 3.0),
                    ),
            )

        val decision =
            AIProfileResolver.decide(
                profile = profile,
                context = baselineDecisionContext(),
                randomSource = FixedRandomSource(nextDouble = 0.80),
            )

        assertEquals("retreat", decision.selectedAction?.id)
        assertEquals("weighted_random", decision.trace.reason)
        assertEquals(0.80, decision.trace.rngRoll)
    }

    @Test
    fun `condition evaluator covers target self status and boolean composition`() {
        val context =
            AIProfileDecisionContext(
                actorId = 7,
                turnId = 3,
                selfHpRatio = 0.35,
                targetHpRatio = 0.20,
                targetVisible = true,
                targetDistance = 2,
                selfStatusIds = setOf("guard"),
                targetStatusIds = setOf("marked"),
                usableAbilityIds = setOf("power_strike"),
                currentEncounterTurn = 6,
            )

        assertTrue(AIConditionEvaluator.evaluate(AICondition.TargetVisible, context))
        assertTrue(AIConditionEvaluator.evaluate(AICondition.TargetDistanceLessThan(3), context))
        assertTrue(AIConditionEvaluator.evaluate(AICondition.TargetDistanceAtMost(2), context))
        assertTrue(AIConditionEvaluator.evaluate(AICondition.TargetDistanceBetween(1, 2), context))
        assertTrue(AIConditionEvaluator.evaluate(AICondition.TargetHpBelow(0.25), context))
        assertTrue(AIConditionEvaluator.evaluate(AICondition.HpBelow(0.40), context))
        assertTrue(AIConditionEvaluator.evaluate(AICondition.HasStatus("guard", AIConditionScope.SELF), context))
        assertTrue(AIConditionEvaluator.evaluate(AICondition.HasStatus("marked", AIConditionScope.TARGET), context))
        assertTrue(AIConditionEvaluator.evaluate(AICondition.TalentReady("power_strike"), context))
        assertTrue(AIConditionEvaluator.evaluate(AICondition.TurnCountModulo(divisor = 3, remainder = 0), context))
        assertTrue(
            AIConditionEvaluator.evaluate(
                AICondition.And(listOf(AICondition.TargetVisible, AICondition.HpBelow(0.40))),
                context,
            ),
        )
        assertTrue(
            AIConditionEvaluator.evaluate(
                AICondition.Or(listOf(AICondition.TargetDistanceAtMost(1), AICondition.TargetVisible)),
                context,
            ),
        )
        assertFalse(AIConditionEvaluator.evaluate(AICondition.Not(AICondition.TargetVisible), context))
        assertFalse(AIConditionEvaluator.evaluate(AICondition.TurnCountModulo(divisor = 4, remainder = 1), context))
    }

    private fun baselineDecisionContext(
        usableAbilityIds: Set<String> = setOf("power_strike"),
    ): AIProfileDecisionContext =
        AIProfileDecisionContext(
            actorId = 99,
            turnId = 12,
            selfHpRatio = 1.0,
            targetHpRatio = 1.0,
            targetVisible = true,
            targetDistance = 1,
            usableAbilityIds = usableAbilityIds,
            currentEncounterTurn = 3,
        )

    private class FixedRandomSource(
        private val nextDouble: Double,
    ) : com.ktome.core.random.RandomSource {
        override fun nextDouble(): Double = nextDouble

        override fun nextInt(
            fromInclusive: Int,
            untilExclusive: Int,
        ): Int = fromInclusive
    }
}
