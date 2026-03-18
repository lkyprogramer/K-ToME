package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.InventoryItemView
import com.ktome.game.PlayerStatus
import com.ktome.game.TalentSlotView

data class ScenarioSpec(
    val name: String,
    val seed: Long,
    val professionId: String = FOUNDATION_PROFESSION_ID,
    val maxTurns: Int,
    val goal: ScenarioGoal,
    val saveLoadCheckpoint: SaveLoadCheckpoint? = null,
    val assertions: List<ScenarioAssertion> = emptyList(),
)

sealed interface ScenarioGoal {
    fun isSatisfied(observation: RunObservation): Boolean

    data class ReachFloor(
        val floor: Int,
    ) : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean = observation.floor >= floor
    }

    data object ReachTerminal : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean = observation.runOutcome.isTerminal
    }

    data class ReachFloorOrTerminal(
        val floor: Int,
    ) : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean =
            observation.floor >= floor || observation.runOutcome.isTerminal
    }

    data class SurviveTurns(
        val turns: Int,
    ) : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean = observation.turnIndex >= turns
    }
}

data class SaveLoadCheckpoint(
    val floor: Int,
    val continueTurns: Int,
    val verifyRoundTrip: Boolean = true,
)

data class ScenarioReport(
    val name: String,
    val seed: Long,
    val professionId: String,
    val success: Boolean,
    val outcome: RunOutcome,
    val floorReached: Int,
    val turns: Int,
    val goalReached: Boolean,
    val failureReason: String? = null,
    val stuckReason: String? = null,
    val assertionFailures: List<String> = emptyList(),
    val checkpointRoundTripVerified: Boolean = false,
    val commandStats: Map<String, Int> = emptyMap(),
    val lastCommands: List<String> = emptyList(),
    val lastMessages: List<String> = emptyList(),
    val eventTail: List<String> = emptyList(),
)

sealed interface ScenarioAssertion {
    fun verify(report: ScenarioReport): String?

    data class ReachedFloorAtLeast(
        val floor: Int,
    ) : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.floorReached >= floor) null else "Expected floor >= $floor but was ${report.floorReached}."
    }

    data object NoStall : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.stuckReason == null) null else "Run stalled: ${report.stuckReason}"
    }

    data object NoFailure : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.failureReason == null) null else "Run failed: ${report.failureReason}"
    }

    data object CheckpointRoundTrip : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.checkpointRoundTripVerified) null else "Checkpoint round-trip was not verified."
    }
}

data class RunObservation(
    val floor: Int,
    val turnIndex: Int,
    val playerStatus: PlayerStatus,
    val playerPosition: Point,
    val map: GameMap,
    val visibleTiles: Set<Point>,
    val exploredTiles: Set<Point>,
    val visibleHostilePositions: List<Point>,
    val visibleBlockingPositions: Set<Point>,
    val visibleGroundItemPositions: List<Point>,
    val knownDownstairsPositions: List<Point>,
    val inventoryItems: List<InventoryItemView>,
    val talentSlots: List<TalentSlotView>,
    val canAscend: Boolean,
    val canDescend: Boolean,
    val runOutcome: RunOutcome,
    val messageLogTail: List<String>,
    val eventTail: List<String>,
)
