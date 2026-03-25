package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.game.PlayerCommand
import com.ktome.game.PlayerResourceView
import com.ktome.game.PlayerStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SoloClearLabSupportBotTest {
    private val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))

    @Test
    fun `boss observation window waits when warning is visible and boss is adjacent`() {
        val command =
            pendingBossTelegraphObservationCommand(
                scenario = SoloClearScenario.BOSS,
                observation = observation(visibleBossPositions = listOf(Point(2, 1))),
                sawBossWarning = true,
                sawTalentTelegraph = false,
                waitedTurns = 0,
            )

        assertEquals(PlayerCommand.Wait, command)
    }

    @Test
    fun `boss observation window does not wait after telegraph appears or budget is spent`() {
        val observation = observation(visibleBossPositions = listOf(Point(2, 1)))

        assertNull(
            pendingBossTelegraphObservationCommand(
                scenario = SoloClearScenario.BOSS,
                observation = observation,
                sawBossWarning = true,
                sawTalentTelegraph = true,
                waitedTurns = 0,
            ),
        )
        assertNull(
            pendingBossTelegraphObservationCommand(
                scenario = SoloClearScenario.BOSS,
                observation = observation,
                sawBossWarning = true,
                sawTalentTelegraph = false,
                waitedTurns = SOLO_CLEAR_BOSS_TELEGRAPH_WAIT_TURNS,
            ),
        )
    }

    @Test
    fun `boss observation window does not stall when boss is not adjacent or hp is unsafe`() {
        assertNull(
            pendingBossTelegraphObservationCommand(
                scenario = SoloClearScenario.BOSS,
                observation = observation(visibleBossPositions = listOf(Point(4, 1))),
                sawBossWarning = true,
                sawTalentTelegraph = false,
                waitedTurns = 0,
            ),
        )
        assertNull(
            pendingBossTelegraphObservationCommand(
                scenario = SoloClearScenario.BOSS,
                observation =
                    observation(
                        visibleBossPositions = listOf(Point(2, 1)),
                        playerStatus = healthyStatus(currentHp = 11, maxHp = 24),
                    ),
                sawBossWarning = true,
                sawTalentTelegraph = false,
                waitedTurns = 0,
            ),
        )
    }

    @Test
    fun `boss observation window does nothing outside boss scenario`() {
        val command =
            pendingBossTelegraphObservationCommand(
                scenario = SoloClearScenario.MOB_PACK,
                observation = observation(visibleBossPositions = listOf(Point(2, 1))),
                sawBossWarning = true,
                sawTalentTelegraph = false,
                waitedTurns = 0,
            )

        assertNull(command)
    }

    private fun observation(
        visibleBossPositions: List<Point> = emptyList(),
        playerStatus: PlayerStatus = healthyStatus(),
    ): RunObservation =
        RunObservation(
            floor = 1,
            turnIndex = 0,
            playerStatus = playerStatus,
            playerResource = PlayerResourceView(current = 40, max = 100, typeId = "HATE"),
            playerPosition = Point(1, 1),
            map = map,
            visibleTiles = map.floorPoints().toSet(),
            exploredTiles = map.floorPoints().toSet(),
            visibleHostilePositions = visibleBossPositions,
            visibleBossPositions = visibleBossPositions,
            visibleBlockingPositions = emptySet(),
            visibleGroundItemPositions = emptyList(),
            visibleInteractables = emptyList(),
            knownDownstairsPositions = emptyList(),
            inventoryItems = emptyList(),
            talentSlots = emptyList(),
            reserveTalents = emptyList(),
            canAscend = false,
            canDescend = false,
            runOutcome = RunOutcome.InProgress,
            messageLogTail = emptyList(),
            eventTail = emptyList(),
        )

    private fun healthyStatus(
        currentHp: Int = 24,
        maxHp: Int = 24,
    ): PlayerStatus =
        PlayerStatus(
            currentHp = currentHp,
            maxHp = maxHp,
            level = 10,
            currentExperience = 0,
            nextLevelRequirement = 100,
            statPoints = 0,
            talentPoints = 0,
            attack = 12,
            defense = 8,
            accuracy = 10,
            evasion = 6,
            speed = 100,
        )
}
