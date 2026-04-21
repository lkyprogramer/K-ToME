package com.ktome.tools.hidden

import com.ktome.core.map.Point
import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.harness.RunObservation
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OrganicHiddenProbeBotPolicyTest {
    @Test
    fun `rejected search only suppresses the current observation`() {
        val bot = OrganicHiddenProbeBotPolicy(delegate = SmokeBot())
        val observation = searchableObservation(turnIndex = 10)

        assertEquals(PlayerCommand.Search, bot.searchCommand(observation))
        bot.recordRejectedSearch(observation)

        assertNull(bot.searchCommand(observation))
        assertEquals(PlayerCommand.Search, bot.searchCommand(observation.copy(turnIndex = observation.turnIndex + 1)))
    }

    @Test
    fun `accepted search consumes cooldown and searched position budget`() {
        val bot = OrganicHiddenProbeBotPolicy(delegate = SmokeBot())
        val observation = searchableObservation(turnIndex = 10)

        assertEquals(PlayerCommand.Search, bot.searchCommand(observation))
        bot.recordAcceptedSearch(observation)

        assertNull(bot.searchCommand(observation.copy(turnIndex = observation.turnIndex + 1, playerPosition = observation.playerPosition.copy(x = observation.playerPosition.x + 1))))
        assertNull(bot.searchCommand(observation.copy(turnIndex = observation.turnIndex + 4)))
        assertEquals(
            PlayerCommand.Search,
            bot.searchCommand(observation.copy(turnIndex = observation.turnIndex + 4, playerPosition = observation.playerPosition.copy(x = observation.playerPosition.x + 1))),
        )
    }

    @Test
    fun `visible threats and descent do not suppress prompt-driven search`() {
        val bot = OrganicHiddenProbeBotPolicy(delegate = SmokeBot())
        val observation =
            searchableObservation(turnIndex = 10).copy(
                canDescend = true,
                visibleBossPositions = listOf(Point(3, 3)),
                visibleHostilePositions = listOf(Point(4, 4)),
            )

        assertEquals(PlayerCommand.Search, bot.searchCommand(observation))
    }

    private fun searchableObservation(turnIndex: Int): RunObservation {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260312L, playerProfessionId = FOUNDATION_PROFESSION_ID),
            )
        return RunObservationCapture.capture(session, turnIndex)
            .copy(
                searchPromptAvailable = true,
                activeRouteSelection = null,
                activeShopId = null,
                canDescend = false,
                visibleBossPositions = emptyList(),
                visibleHostilePositions = emptyList(),
            )
    }
}
