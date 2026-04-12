package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.game.PlayerResourceView
import com.ktome.game.PlayerStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HeadlessRunHarnessDiagnosticsTest {
    private val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))

    @Test
    fun `distinct visible threat count does not double count bosses already present in hostile list`() {
        val observation =
            observation(
                visibleHostilePositions = listOf(Point(3, 1), Point(4, 1)),
                visibleBossPositions = listOf(Point(4, 1)),
            )

        assertEquals(2, distinctVisibleThreatCount(observation))
    }

    @Test
    fun `objective progress zone ids include transition target on zone entry`() {
        assertEquals(
            listOf("greenwood_fringe", "deep_iron_pit"),
            objectiveProgressZoneIds(previousZoneId = "greenwood_fringe", currentZoneId = "deep_iron_pit"),
        )
        assertEquals(
            listOf("greenwood_fringe"),
            objectiveProgressZoneIds(previousZoneId = "greenwood_fringe", currentZoneId = "greenwood_fringe"),
        )
    }

    private fun observation(
        visibleHostilePositions: List<Point> = emptyList(),
        visibleBossPositions: List<Point> = emptyList(),
    ): RunObservation =
        RunObservation(
            zoneId = "shattered_outpost",
            floor = 1,
            turnIndex = 0,
            playerStatus =
                PlayerStatus(
                    currentHp = 20,
                    maxHp = 20,
                    level = 1,
                    currentExperience = 0,
                    nextLevelRequirement = 10,
                    statPoints = 0,
                    talentPoints = 0,
                    attack = 6,
                    defense = 4,
                    accuracy = 5,
                    evasion = 3,
                    speed = 100,
                ),
            playerResource = PlayerResourceView(current = 6, max = 20, typeId = "MANA"),
            playerPosition = Point(1, 1),
            map = map,
            visibleTiles = setOf(Point(1, 1)),
            exploredTiles = setOf(Point(1, 1)),
            visibleHostilePositions = visibleHostilePositions,
            visibleBossPositions = visibleBossPositions,
            visibleBlockingPositions = emptySet(),
            visibleGroundItemPositions = emptyList(),
            visibleInteractables = emptyList(),
            knownDownstairsPositions = emptyList(),
            inventoryItems = emptyList(),
            talentSlots = emptyList(),
            canAscend = false,
            canDescend = false,
            runOutcome = RunOutcome.InProgress,
            messageLogTail = emptyList(),
            eventTail = emptyList(),
        )
}
