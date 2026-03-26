package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.core.snapshot.RouteSelectionSnapshot
import com.ktome.core.world.ObjectiveState
import com.ktome.game.FOUNDATION_ZONE_ID
import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.FOUNDATION_RACE_ID
import com.ktome.game.InventoryItemView
import com.ktome.game.PlayerResourceView
import com.ktome.game.PlayerStatus
import com.ktome.game.TalentReserveView
import com.ktome.game.TalentSlotView

data class ScenarioSpec(
    val name: String,
    val seed: Long,
    val zoneId: String = FOUNDATION_ZONE_ID,
    val professionId: String = FOUNDATION_PROFESSION_ID,
    val raceId: String = FOUNDATION_RACE_ID,
    val zoneRoute: List<String> = listOf(zoneId),
    val routeIndex: Int = 0,
    val corpusId: String = HarnessMetadata.DEFAULT_CORPUS_ID,
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

    data class ReachZoneAtLeastOrTerminal(
        val zoneId: String,
    ) : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean =
            zoneDepth(observation.zoneId) >= zoneDepth(zoneId) ||
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
)

data class ZoneHeadlessMilestone(
    val zoneId: String,
    val turnIndex: Int,
    val headlessTurnEquivalent: Int,
    val deltaTurns: Int,
    val deltaHeadlessTurns: Int,
)

data class CaptainEncounterTraceEntry(
    val turnIndex: Int,
    val headlessTurnEquivalent: Int,
    val floor: Int,
    val playerHp: Int,
    val playerMaxHp: Int,
    val playerResourceCurrent: Int,
    val playerResourceMax: Int,
    val playerResourceTypeId: String,
    val captainHp: Int?,
    val captainMaxHp: Int?,
    val captainDistance: Int?,
    val command: String?,
    val recentMessages: List<String>,
    val recentEvents: List<String>,
)

data class ZoneObjectiveSummary(
    val zoneId: String,
    val questId: String,
    val objectiveId: String,
    val state: ObjectiveState,
    val completionFlagGranted: Boolean,
)

data class ScenarioReport(
    val name: String,
    val seed: Long,
    val zoneId: String = FOUNDATION_ZONE_ID,
    val professionId: String,
    val raceId: String = FOUNDATION_RACE_ID,
    val routeIndex: Int = 0,
    val finalZoneId: String = zoneId,
    val zoneRouteHash: String = zoneId,
    val zonePath: List<String> = listOf(zoneId),
    val success: Boolean,
    val outcome: RunOutcome,
    val floorReached: Int,
    val turns: Int,
    val headlessTurnEquivalent: Int = turns,
    val buildId: String = HarnessMetadata.BUILD_ID,
    val phaseId: String = HarnessMetadata.PHASE_ID,
    val rulesetVersion: String = HarnessMetadata.RULESET_VERSION,
    val traceSchemaVersion: String = HarnessMetadata.TRACE_SCHEMA_VERSION,
    val corpusId: String = HarnessMetadata.DEFAULT_CORPUS_ID,
    val localeId: String = "headless",
    val profileId: String = HarnessMetadata.PROFILE_ID,
    val buildHash: String? = null,
    val goalReached: Boolean,
    val failureReason: String? = null,
    val stuckReason: String? = null,
    val assertionFailures: List<String> = emptyList(),
    val checkpointRoundTripVerified: Boolean = false,
    val commandStats: Map<String, Int> = emptyMap(),
    val zoneHeadlessMilestones: List<ZoneHeadlessMilestone> = emptyList(),
    val zoneObjectiveSummaries: List<ZoneObjectiveSummary> = emptyList(),
    val captainEncounterTrace: List<CaptainEncounterTraceEntry> = emptyList(),
    val lastCommands: List<String> = emptyList(),
    val lastMessages: List<String> = emptyList(),
    val eventTail: List<String> = emptyList(),
) {
    /**
     * Normal defeats should not be treated as harness crashes/stalls by acceptance labs that
     * separately track progression thresholds.
     */
    fun crashedOrStalled(): Boolean =
        stuckReason != null ||
            (failureReason != null && failureReason != "Turn budget exhausted.")
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

    data class FinalZoneAtLeast(
        val zoneId: String,
    ) : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (zoneDepth(report.finalZoneId) >= zoneDepth(zoneId)) {
                null
            } else {
                "Expected final zone >= $zoneId but was ${report.finalZoneId}."
            }
    }
}

internal fun zoneDepth(zoneId: String): Int =
    when (zoneId) {
        "shattered_outpost" -> 0
        "greenwood_fringe" -> 1
        "bandit_camp" -> 2
        "elven_ruins" -> 3
        "deep_iron_pit" -> 4
        "molten_core" -> 5
        "grey_gate_depths" -> 6
        "underground_river" -> 7
        "crystal_cavern" -> 8
        "abyssal_temple" -> 9
        "abyssal_heart" -> 10
        else -> -1
    }

data class RunObservation(
    val zoneId: String = FOUNDATION_ZONE_ID,
    val floor: Int,
    val turnIndex: Int,
    val playerStatus: PlayerStatus,
    val playerResource: PlayerResourceView = PlayerResourceView(current = 0, max = 0, typeId = "STAMINA"),
    val shardBalance: Int = 0,
    val playerPosition: Point,
    val map: GameMap,
    val visibleTiles: Set<Point>,
    val exploredTiles: Set<Point>,
    val visibleHostilePositions: List<Point>,
    val visibleBossPositions: List<Point> = emptyList(),
    val visibleBlockingPositions: Set<Point>,
    val visibleGroundItemPositions: List<Point>,
    val visibleInteractables: List<ObservedInteractable>,
    val knownDownstairsPositions: List<Point>,
    val playerStatusTypeIds: Set<String> = emptySet(),
    val activeRouteSelection: RouteSelectionSnapshot? = null,
    val activeShopId: String? = null,
    val activeShopOffers: List<ObservedShopOffer> = emptyList(),
    val inventoryItems: List<InventoryItemView>,
    val talentSlots: List<TalentSlotView>,
    val reserveTalents: List<TalentReserveView> = emptyList(),
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

data class ObservedShopOffer(
    val index: Int,
    val price: Int,
    val tags: Set<String> = emptySet(),
    val purchasable: Boolean = true,
)
