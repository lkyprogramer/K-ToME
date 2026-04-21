package com.ktome.tools.hidden

import com.ktome.core.map.Point
import com.ktome.game.PlayerCommand
import com.ktome.game.harness.RunBot
import com.ktome.game.harness.RunObservation

internal class OrganicHiddenProbeBotPolicy(
    private val delegate: RunBot,
) : RunBot {
    private val searchedPositionsByFloor = mutableMapOf<Int, MutableSet<Point>>()
    private val searchCountByFloor = mutableMapOf<Int, Int>()
    private var lastAcceptedSearchTurn: Int? = null
    private var lastRejectedSearchKey: RejectedSearchKey? = null

    override fun decide(observation: RunObservation): PlayerCommand? = searchCommand(observation) ?: delegate.decide(observation)

    internal fun searchCommand(observation: RunObservation): PlayerCommand? = if (shouldSearch(observation)) PlayerCommand.Search else null

    internal fun recordAcceptedSearch(observation: RunObservation) {
        searchedPositionsByFloor.getOrPut(observation.floor) { linkedSetOf() } += observation.playerPosition
        searchCountByFloor[observation.floor] = (searchCountByFloor[observation.floor] ?: 0) + 1
        lastAcceptedSearchTurn = observation.turnIndex
        lastRejectedSearchKey = null
    }

    internal fun recordRejectedSearch(observation: RunObservation) {
        lastRejectedSearchKey =
            RejectedSearchKey(
                floor = observation.floor,
                position = observation.playerPosition,
                turnIndex = observation.turnIndex,
            )
    }

    private fun shouldSearch(observation: RunObservation): Boolean {
        if (!observation.searchPromptAvailable) {
            return false
        }
        if (
            lastRejectedSearchKey ==
                RejectedSearchKey(
                    floor = observation.floor,
                    position = observation.playerPosition,
                    turnIndex = observation.turnIndex,
                )
        ) {
            return false
        }
        if (observation.activeRouteSelection != null || observation.activeShopId != null) {
            return false
        }
        val acceptedSearchTurn = lastAcceptedSearchTurn
        if (acceptedSearchTurn != null && observation.turnIndex - acceptedSearchTurn < 4) {
            return false
        }
        if ((searchCountByFloor[observation.floor] ?: 0) >= 6) {
            return false
        }
        if (observation.playerPosition in searchedPositionsByFloor.getOrPut(observation.floor) { linkedSetOf() }) {
            return false
        }
        return true
    }

    private data class RejectedSearchKey(
        val floor: Int,
        val position: Point,
        val turnIndex: Int,
    )
}
