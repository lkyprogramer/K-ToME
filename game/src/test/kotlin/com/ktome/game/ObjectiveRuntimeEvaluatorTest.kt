package com.ktome.game

import com.ktome.core.world.ObjectiveState
import com.ktome.game.objective.ObjectiveCompletionRule
import com.ktome.game.objective.ObjectiveCompletionTrigger
import com.ktome.game.objective.ObjectiveRuntimeEvaluator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObjectiveRuntimeEvaluatorTest {
    @Test
    fun `progress recording only promotes available objectives into in progress`() {
        assertEquals(ObjectiveState.LOCKED, ObjectiveRuntimeEvaluator.onProgressRecorded(ObjectiveState.LOCKED))
        assertEquals(ObjectiveState.IN_PROGRESS, ObjectiveRuntimeEvaluator.onProgressRecorded(ObjectiveState.AVAILABLE))
        assertEquals(ObjectiveState.IN_PROGRESS, ObjectiveRuntimeEvaluator.onProgressRecorded(ObjectiveState.IN_PROGRESS))
        assertEquals(ObjectiveState.COMPLETED, ObjectiveRuntimeEvaluator.onProgressRecorded(ObjectiveState.COMPLETED))
    }

    @Test
    fun `explore floor pair requires prior progress before zone exit can complete it`() {
        assertFalse(
            ObjectiveRuntimeEvaluator.isSatisfied(
                rule = ObjectiveCompletionRule.EXPLORE_FLOOR_PAIR,
                currentState = ObjectiveState.AVAILABLE,
                trigger = ObjectiveCompletionTrigger.ZONE_EXIT,
                floorReached = 2,
                maxFloor = 2,
            ),
        )
        assertTrue(
            ObjectiveRuntimeEvaluator.isSatisfied(
                rule = ObjectiveCompletionRule.EXPLORE_FLOOR_PAIR,
                currentState = ObjectiveState.IN_PROGRESS,
                trigger = ObjectiveCompletionTrigger.ZONE_EXIT,
                floorReached = 2,
                maxFloor = 2,
            ),
        )
    }

    @Test
    fun `secure forge path only completes on terminal trigger after progress was started`() {
        assertFalse(
            ObjectiveRuntimeEvaluator.isSatisfied(
                rule = ObjectiveCompletionRule.SECURE_FORGE_PATH,
                currentState = ObjectiveState.AVAILABLE,
                trigger = ObjectiveCompletionTrigger.BOSS_DEFEAT,
                floorReached = 2,
                maxFloor = 2,
            ),
        )
        assertFalse(
            ObjectiveRuntimeEvaluator.isSatisfied(
                rule = ObjectiveCompletionRule.SECURE_FORGE_PATH,
                currentState = ObjectiveState.IN_PROGRESS,
                trigger = ObjectiveCompletionTrigger.PROGRESS_RECORDED,
                floorReached = 2,
                maxFloor = 2,
            ),
        )
        assertTrue(
            ObjectiveRuntimeEvaluator.isSatisfied(
                rule = ObjectiveCompletionRule.SECURE_FORGE_PATH,
                currentState = ObjectiveState.IN_PROGRESS,
                trigger = ObjectiveCompletionTrigger.BOSS_DEFEAT,
                floorReached = 2,
                maxFloor = 2,
            ),
        )
    }
}
