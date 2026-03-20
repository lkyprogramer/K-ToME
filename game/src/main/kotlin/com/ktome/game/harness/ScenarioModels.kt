package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.game.FOUNDATION_ZONE_ID
import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.InventoryItemView
import com.ktome.game.PlayerResourceView
import com.ktome.game.PlayerStatus
import com.ktome.game.TalentSlotView

data class ScenarioSpec(
    val name: String,
    val seed: Long,
    val zoneId: String = FOUNDATION_ZONE_ID,
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

    data object Victory : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean = observation.runOutcome is RunOutcome.Victory
    }

    data class ReachFloorOrTerminal(
        val floor: Int,
    ) : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean =
            observation.floor >= floor ||
                (observation.runOutcome.isTerminal && observation.runOutcome !is RunOutcome.Defeat)
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
    val zoneId: String = FOUNDATION_ZONE_ID,
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
) {
    /**
     * Normal defeats should not be treated as harness crashes/stalls by acceptance labs that
     * separately track progression thresholds.
     */
    fun crashedOrStalled(): Boolean = failureReason != null || stuckReason != null
}

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

    data object Victory : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.outcome is RunOutcome.Victory) null else "Expected victory but got ${report.outcome}."
    }
}

data class RunObservation(
    val floor: Int,
    val turnIndex: Int,
    val playerStatus: PlayerStatus,
    val playerResource: PlayerResourceView = PlayerResourceView(current = 0, max = 0, typeId = "STAMINA"),
    val playerPosition: Point,
    val map: GameMap,
    val visibleTiles: Set<Point>,
    val exploredTiles: Set<Point>,
    val visibleHostilePositions: List<Point>,
    val visibleBlockingPositions: Set<Point>,
    val visibleGroundItemPositions: List<Point>,
    val visibleInteractables: List<ObservedInteractable>,
    val knownDownstairsPositions: List<Point>,
    val inventoryItems: List<InventoryItemView>,
    val talentSlots: List<TalentSlotView>,
    val canAscend: Boolean,
    val canDescend: Boolean,
    val runOutcome: RunOutcome,
    val messageLogTail: List<String>,
    val eventTail: List<String>,
)

data class ObservedInteractable(
    val id: String,
    val position: Point,
    val interactionTags: Set<String> = emptySet(),
)
