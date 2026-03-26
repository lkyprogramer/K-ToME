package com.ktome.game.objective

import com.ktome.core.world.ObjectiveState

internal object ObjectiveRuntimeEvaluator {
    fun onProgressRecorded(currentState: ObjectiveState): ObjectiveState =
        when (currentState) {
            ObjectiveState.LOCKED -> ObjectiveState.LOCKED
            ObjectiveState.AVAILABLE -> ObjectiveState.IN_PROGRESS

            ObjectiveState.IN_PROGRESS,
            ObjectiveState.COMPLETED,
            -> currentState
        }

    fun isSatisfied(
        rule: ObjectiveCompletionRule,
        currentState: ObjectiveState,
        trigger: ObjectiveCompletionTrigger,
        floorReached: Int,
        maxFloor: Int,
    ): Boolean {
        val finalFloorReached = floorReached >= maxFloor
        return when (rule) {
            ObjectiveCompletionRule.DEFEAT_ZONE_BOSS -> trigger == ObjectiveCompletionTrigger.BOSS_DEFEAT
            ObjectiveCompletionRule.EXPLORE_FLOOR_PAIR ->
                trigger == ObjectiveCompletionTrigger.ZONE_EXIT &&
                    finalFloorReached &&
                    currentState in setOf(ObjectiveState.IN_PROGRESS, ObjectiveState.COMPLETED)
            ObjectiveCompletionRule.SECURE_FORGE_PATH ->
                finalFloorReached &&
                    trigger in TERMINAL_TRIGGERS &&
                    currentState in setOf(ObjectiveState.IN_PROGRESS, ObjectiveState.COMPLETED)
        }
    }

    private val TERMINAL_TRIGGERS = setOf(ObjectiveCompletionTrigger.ZONE_EXIT, ObjectiveCompletionTrigger.BOSS_DEFEAT)
}
