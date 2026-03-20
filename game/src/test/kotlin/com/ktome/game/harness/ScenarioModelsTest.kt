package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.game.PlayerStatus
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScenarioModelsTest {
    @Test
    fun `reach floor or terminal accepts victory before target floor`() {
        val goal = ScenarioGoal.ReachFloorOrTerminal(5)

        assertTrue(goal.isSatisfied(observation(floor = 1, runOutcome = RunOutcome.Victory(floor = 1))))
    }

    @Test
    fun `reach floor or terminal rejects defeat before target floor`() {
        val goal = ScenarioGoal.ReachFloorOrTerminal(5)

        assertFalse(goal.isSatisfied(observation(floor = 1, runOutcome = RunOutcome.Defeat(floor = 1))))
    }

    @Test
    fun `reach floor or terminal still accepts reaching target floor while run is in progress`() {
        val goal = ScenarioGoal.ReachFloorOrTerminal(5)

        assertTrue(goal.isSatisfied(observation(floor = 5, runOutcome = RunOutcome.InProgress)))
    }

    @Test
    fun `scenario report does not treat normal defeat without harness errors as crash or stall`() {
        val report =
            report(
                success = false,
                goalReached = false,
                outcome = RunOutcome.Defeat(floor = 2),
            )

        assertFalse(report.crashedOrStalled())
    }

    @Test
    fun `scenario report treats harness failure as crash or stall`() {
        val report =
            report(
                success = false,
                goalReached = false,
                outcome = RunOutcome.Defeat(floor = 2),
                failureReason = "Turn budget exhausted.",
            )

        assertTrue(report.crashedOrStalled())
    }

    private fun observation(
        floor: Int,
        runOutcome: RunOutcome,
    ): RunObservation =
        RunObservation(
            floor = floor,
            turnIndex = 0,
            playerStatus =
                PlayerStatus(
                    currentHp = 10,
                    maxHp = 10,
                    currentStamina = 5,
                    maxStamina = 5,
                    level = 1,
                    currentExperience = 0,
                    nextLevelRequirement = 10,
                    statPoints = 0,
                    talentPoints = 0,
                    attack = 1,
                    defense = 1,
                    accuracy = 1,
                    evasion = 1,
                    speed = 100,
                ),
            playerPosition = Point.ZERO,
            map = GameMap.fromAscii(listOf("@")),
            visibleTiles = setOf(Point.ZERO),
            exploredTiles = setOf(Point.ZERO),
            visibleHostilePositions = emptyList(),
            visibleBlockingPositions = emptySet(),
            visibleGroundItemPositions = emptyList(),
            visibleInteractables = emptyList(),
            knownDownstairsPositions = emptyList(),
            inventoryItems = emptyList(),
            talentSlots = emptyList(),
            canAscend = false,
            canDescend = false,
            runOutcome = runOutcome,
            messageLogTail = emptyList(),
            eventTail = emptyList(),
        )

    private fun report(
        success: Boolean,
        goalReached: Boolean,
        outcome: RunOutcome,
        failureReason: String? = null,
        stuckReason: String? = null,
    ): ScenarioReport =
        ScenarioReport(
            name = "scenario",
            seed = 1L,
            professionId = "profession",
            success = success,
            outcome = outcome,
            floorReached = 2,
            turns = 42,
            goalReached = goalReached,
            failureReason = failureReason,
            stuckReason = stuckReason,
        )
}
