package com.ktome.game.harness

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RouteProgressCommandTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `route progress targets boss on final floor when no downstairs exist`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "grey_gate_depths", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-route-progress")),
            )

        val stairsDown = requireNotNull(session.automationStairPoint(StairDirection.DOWN))
        session.automationMovePlayerTo(stairsDown)
        assertTrue(session.perform(PlayerCommand.Descend))

        val bossPoint = requireNotNull(session.automationBossPoint())
        val observation = RunObservationCapture.capture(session, turnIndex = 0)
        val command = requireNotNull(routeProgressCommand(session, observation))

        assertTrue(command is PlayerCommand.Move)
        val destination = observation.playerPosition + (command as PlayerCommand.Move).delta
        assertCloser(destination, observation.playerPosition, bossPoint)
    }

    @Test
    fun `route progress yields to combat bot once boss is visible`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "grey_gate_depths", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-visible-route-progress")),
            )

        val stairsDown = requireNotNull(session.automationStairPoint(StairDirection.DOWN))
        session.automationMovePlayerTo(stairsDown)
        assertTrue(session.perform(PlayerCommand.Descend))

        val bossPoint = requireNotNull(session.automationBossPoint())
        session.automationMovePlayerTo(findOpenAdjacentPoint(session, bossPoint))
        val observation = RunObservationCapture.capture(session, turnIndex = 0)

        assertTrue(observation.visibleBossPositions.isNotEmpty())
        assertTrue(routeProgressCommand(session, observation) == null)
    }

    @Test
    fun `route progress still pursues objective hook when only distant hostiles are visible`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260318L,
                        zoneId = "bandit_camp",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "bandit_camp"),
                        routeIndex = 2,
                    ),
                saveManager = SaveManager(tempDir.resolve("objective-hook-route-progress")),
            )

        val objectivePoint = requireNotNull(session.automationPendingObjectiveInteractablePoint())
        val observation = RunObservationCapture.capture(session, turnIndex = 0)
        val farHostile = objectivePoint + Point(4, 0)
        val command =
            requireNotNull(
                routeProgressCommand(
                    session,
                    observation.copy(visibleHostilePositions = listOf(farHostile)),
                ),
            )

        assertTrue(command is PlayerCommand.Move || command == PlayerCommand.Interact)
        if (command is PlayerCommand.Move) {
            val destination = observation.playerPosition + command.delta
            assertCloser(destination, observation.playerPosition, objectivePoint)
        }
    }

    @Test
    fun `energy route progress yields to combat bot when a visible hostile is close`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260318L,
                        zoneId = "bandit_camp",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "bandit_camp"),
                        routeIndex = 2,
                    ),
                saveManager = SaveManager(tempDir.resolve("close-hostile-route-progress")),
            )

        val observation = RunObservationCapture.capture(session, turnIndex = 0)
        val closeHostile = observation.playerPosition + Point(2, 0)

        assertTrue(routeProgressCommand(session, observation.copy(visibleHostilePositions = listOf(closeHostile))) == null)
    }

    @Test
    fun `mana route progress yields to combat bot when a visible hostile is close`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260318L,
                        zoneId = "bandit_camp",
                        playerProfessionId = "arcanist",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "bandit_camp"),
                        routeIndex = 2,
                    ),
                saveManager = SaveManager(tempDir.resolve("close-hostile-route-progress-mana")),
            )

        val observation = RunObservationCapture.capture(session, turnIndex = 0)
        val closeHostile = observation.playerPosition + Point(2, 0)

        assertTrue(routeProgressCommand(session, observation.copy(visibleHostilePositions = listOf(closeHostile))) == null)
    }

    @Test
    fun `non energy route progress keeps hard route push when a visible hostile is close`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260318L,
                        zoneId = "bandit_camp",
                        playerProfessionId = "vanguard",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "bandit_camp"),
                        routeIndex = 2,
                    ),
                saveManager = SaveManager(tempDir.resolve("close-hostile-route-progress-non-energy")),
            )

        val observation = RunObservationCapture.capture(session, turnIndex = 0)
        val closeHostile = observation.playerPosition + Point(2, 0)

        assertTrue(routeProgressCommand(session, observation.copy(visibleHostilePositions = listOf(closeHostile))) != null)
    }

    @Test
    fun `route progress without objective hook does not auto interact pending objective`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260318L,
                        zoneId = "bandit_camp",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "bandit_camp"),
                        routeIndex = 2,
                    ),
                saveManager = SaveManager(tempDir.resolve("objective-hook-disabled-route-progress")),
            )

        val objectivePoint = requireNotNull(session.automationPendingObjectiveInteractablePoint())
        session.automationMovePlayerTo(objectivePoint)
        val observation = RunObservationCapture.capture(session, turnIndex = 0)

        assertTrue(routeProgressCommand(session, observation) == PlayerCommand.Interact)
        assertTrue(routeProgressCommandWithoutObjectiveHook(session, observation) != PlayerCommand.Interact)
    }

    @Test
    fun `terrain exposure fast frame matches full observation route command pilot set`() {
        val pilotCases =
            listOf(
                "greenwood_fringe" to 20260409010000L,
                "deep_iron_pit" to 20260409011000L,
                "underground_river" to 20260409012000L,
                "crystal_cavern" to 20260409013000L,
            )
        val fullCommands =
            pilotCases.map { (zoneId, seed) ->
                val session =
                    GameModule.newFoundationSession(
                        config = FoundationGameConfig(seed = seed, zoneId = zoneId, playerProfessionId = "arcanist"),
                        saveManager = SaveManager(tempDir.resolve("terrain-fast-frame-route-$zoneId-$seed")),
                    )
                val observation = RunObservationCapture.capture(session, turnIndex = 0)
                val frame = TerrainExposureFastFrame.capture(session)
                val fullCommand = routeProgressCommandWithoutObjectiveHook(session, observation)
                val fastCommand = routeProgressCommandWithoutObjectiveHook(session, frame)

                assertEquals(fullCommand, fastCommand, "Fast frame command drifted for $zoneId/$seed.")
                fullCommand
            }

        assertTrue(fullCommands.any { command -> command is PlayerCommand.Move })
    }

    @Test
    fun `terrain exposure fast frame falls back when route progress cannot prove a move`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260318L,
                        zoneId = "bandit_camp",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "bandit_camp"),
                        routeIndex = 2,
                    ),
                saveManager = SaveManager(tempDir.resolve("terrain-fast-frame-close-hostile")),
            )
        val observation = RunObservationCapture.capture(session, turnIndex = 0)
        val closeHostile = observation.playerPosition + Point(2, 0)
        val frame =
            TerrainExposureFastFrame
                .fromObservation(observation)
                .copy(visibleHostilePositions = listOf(closeHostile))

        assertEquals(
            routeProgressCommandWithoutObjectiveHook(session, observation.copy(visibleHostilePositions = listOf(closeHostile))),
            routeProgressCommandWithoutObjectiveHook(session, frame),
        )
        assertEquals(null, routeProgressCommandWithoutObjectiveHook(session, frame))
    }

    @Test
    fun `route progress pivots to boss path on final floor after terminal objective is in progress`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "deep_iron_pit", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("deep-iron-in-progress-route-progress")),
            )

        val oreStash = requireNotNull(session.automationInteractablePoint("ore_stash"))
        session.automationMovePlayerTo(oreStash)
        assertTrue(session.perform(PlayerCommand.Interact))

        val stairsDown = requireNotNull(session.automationStairPoint(StairDirection.DOWN))
        session.automationMovePlayerTo(stairsDown)
        assertTrue(session.perform(PlayerCommand.Descend))

        assertTrue(session.automationPendingObjectiveInteractablePoint() == null)

        val bossPoint = requireNotNull(session.automationBossPoint())
        val observation = RunObservationCapture.capture(session, turnIndex = 0)
        val command = requireNotNull(routeProgressCommand(session, observation))

        assertTrue(command is PlayerCommand.Move)
        val destination = observation.playerPosition + (command as PlayerCommand.Move).delta
        assertCloser(destination, observation.playerPosition, bossPoint)
    }

    private fun assertCloser(
        destination: Point,
        origin: Point,
        target: Point,
    ) {
        assertTrue(
            destination.chebyshevDistanceTo(target) < origin.chebyshevDistanceTo(target),
            "Expected $destination to move closer to $target from $origin.",
        )
    }

    private fun findOpenAdjacentPoint(
        session: com.ktome.game.FoundationGameSession,
        center: Point,
    ): Point {
        val occupied =
            session.automationWorld()
                .entitiesWith(com.ktome.core.ecs.Position::class, com.ktome.core.ecs.BlocksMovement::class)
                .map { entityId ->
                    val position = requireNotNull(session.automationWorld().get<com.ktome.core.ecs.Position>(entityId))
                    Point(position.x, position.y)
                }.toSet()
        return Point.ALL_DIRECTIONS
            .map { delta -> center + delta }
            .first { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map.blocksMovement(point.x, point.y) &&
                    point !in occupied
            }
    }
}
