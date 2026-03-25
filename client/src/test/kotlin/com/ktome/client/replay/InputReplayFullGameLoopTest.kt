package com.ktome.client.replay

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.InputHandler
import com.ktome.client.screen.MainMenuAction
import com.ktome.client.screen.MainMenuController
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar
import com.ktome.core.run.RunOutcome
import com.ktome.core.save.SaveManager
import com.ktome.game.FOUNDATION_BOSS_TEMPLATE_ID
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.factory.EntityFactory
import com.ktome.game.model.MonsterTemplate
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class InputReplayFullGameLoopTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `replay path exercises menu continue victory and game over through key bindings`() {
        val saveManager = SaveManager(tempDir.resolve("replay-save"))
        val menuInput = ReplayInputSource()
        val menu =
            MainMenuController(
                input = menuInput,
                availableProfessionIds = GameModule.availableProfessionIds(),
                availableRaceIds = GameModule.availablePlayerCreationRaceIds(),
                initialProfessionId = "vanguard",
                initialRaceId = "human",
            )

        assertFalse(saveManager.hasSave())
        val newGameAction = pressMenu(menu, menuInput, hasSave = false, justPressed = setOf(Keys.ENTER))
        assertEquals(MainMenuAction.StartNewGame, newGameAction)

        val session = GameModule.newFoundationSession(FoundationGameConfig(zoneId = "grey_gate_depths"), saveManager)
        val inputHandler = InputHandler(ReplayInputSource())
        descendToFloor(session, inputHandler, targetFloor = 2)
        assertEquals(2, session.currentFloor())

        assertTrue(runCommand(session, inputHandler, justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S)))
        assertTrue(saveManager.hasSave())

        val continueSelection = pressMenu(menu, menuInput, hasSave = true, justPressed = setOf(Keys.DOWN))
        assertNull(continueSelection)
        val continueAction = pressMenu(menu, menuInput, hasSave = true, justPressed = setOf(Keys.ENTER))
        assertEquals(MainMenuAction.ContinueGame, continueAction)

        val continued = GameModule.loadFoundationSession(saveManager)
        assertNotNull(continued)
        val continuedSession = requireNotNull(continued)
        val continuedInput = InputHandler(ReplayInputSource())
        assertEquals(2, continuedSession.currentFloor())

        killBossByReplay(continuedSession, continuedInput)
        assertTrue(continuedSession.runOutcome() is RunOutcome.Victory)
        assertFalse(saveManager.hasSave())

        val secondNewGameAction = pressMenu(menu, menuInput, hasSave = false, justPressed = setOf(Keys.UP))
        assertNull(secondNewGameAction)
        val wrapAroundAction = pressMenu(menu, menuInput, hasSave = false, justPressed = setOf(Keys.ENTER))
        assertEquals(MainMenuAction.StartNewGame, wrapAroundAction)

        val secondRun = GameModule.newFoundationSession(FoundationGameConfig(), saveManager)
        val secondInput = InputHandler(ReplayInputSource())
        forceGameOverByReplay(secondRun, secondInput)
        assertTrue(secondRun.runOutcome() is RunOutcome.Defeat)
        assertFalse(saveManager.hasSave())

        val blockedContinueSelect = pressMenu(menu, menuInput, hasSave = false, justPressed = setOf(Keys.DOWN))
        assertNull(blockedContinueSelect)
        val blockedContinueConfirm = pressMenu(menu, menuInput, hasSave = false, justPressed = setOf(Keys.ENTER))
        assertNull(blockedContinueConfirm)
        assertNull(GameModule.loadFoundationSession(saveManager))
    }

    private fun descendToFloor(
        session: FoundationGameSession,
        inputHandler: InputHandler,
        targetFloor: Int,
    ) {
        while (session.currentFloor() < targetFloor) {
            clearRegularMonsters(session)
            val stair = requireNotNull(stairPoint(session, StairDirection.DOWN))
            replayMovePath(session, inputHandler, stair)
            assertTrue(
                runCommand(
                    session = session,
                    inputHandler = inputHandler,
                    justPressed = setOf(Keys.PERIOD),
                    pressed = setOf(Keys.SHIFT_LEFT, Keys.PERIOD),
                ),
            )
        }
    }

    private fun killBossByReplay(
        session: FoundationGameSession,
        inputHandler: InputHandler,
    ) {
        val world = runtimeWorld(session)
        val bossId =
            world.entitiesWith(MonsterTemplateId::class, Position::class, Health::class)
                .single { entityId ->
                    requireNotNull(world.get<MonsterTemplateId>(entityId)).value == FOUNDATION_BOSS_TEMPLATE_ID
                }
        world.remove<AIBehavior>(bossId)
        requireNotNull(world.get<Health>(bossId)).current = 1
        val bossPosition = requireNotNull(world.get<Position>(bossId)).toPoint()
        val attackOrigin = findOpenAdjacentPoint(session, bossPosition)

        replayMovePath(session, inputHandler, attackOrigin)
        val delta = bossPosition - attackOrigin
        assertTrue(runCommand(session, inputHandler, justPressed = setOf(keyForDelta(delta))))
    }

    private fun forceGameOverByReplay(
        session: FoundationGameSession,
        inputHandler: InputHandler,
    ) {
        val world = runtimeWorld(session)
        val playerPosition = session.playerPosition()
        EntityFactory().createMonster(
            world = world,
            template =
                MonsterTemplate(
                    id = "replay_killer",
                    name = "Replay Killer",
                    glyph = 'K',
                    colorHex = "#FF4444",
                    stats = com.ktome.core.ecs.Stats(str = 12, dex = 1, con = 1, wil = 1),
                    baseHp = 10,
                    baseAttack = 12,
                    baseDefense = 0,
                    speed = 90,
                    ai = AIType.CHASE,
                    expReward = 0,
                    spawnFloors = listOf(1),
                    spawnWeight = 1,
                ),
            position = findOpenAdjacentPoint(session, playerPosition),
        )
        requireNotNull(world.get<Health>(session.playerId)).current = 1

        repeat(4) {
            if (session.runOutcome() !is RunOutcome.Defeat) {
                assertTrue(runCommand(session, inputHandler, justPressed = setOf(Keys.SPACE)))
            }
        }
    }

    private fun replayMovePath(
        session: FoundationGameSession,
        inputHandler: InputHandler,
        destination: Point,
    ) {
        while (session.playerPosition() != destination) {
            drainMandatoryAllocations(session, inputHandler)
            val path =
                AStar.findPath(
                    map = session.map,
                    start = session.playerPosition(),
                    goal = destination,
                    blocked = blockingPoints(session) - destination,
                )
            require(path.size >= 2) { "No path from ${session.playerPosition()} to $destination." }
            val delta = path[1] - path[0]
            val consumed = runCommand(session, inputHandler, justPressed = setOf(keyForDelta(delta)))
            assertTrue(
                consumed,
                "Replay move failed from ${path[0]} to ${path[1]} toward $destination; player=${session.playerPosition()}; messages=${session.messageLog().takeLast(4)}",
            )
        }
    }

    private fun drainMandatoryAllocations(
        session: FoundationGameSession,
        inputHandler: InputHandler,
    ) {
        while (session.hasPendingStatAllocation()) {
            assertTrue(runCommand(session, inputHandler, justPressed = setOf(Keys.NUM_1)))
        }
    }

    private fun runCommand(
        session: FoundationGameSession,
        inputHandler: InputHandler,
        justPressed: Set<Int>,
        pressed: Set<Int> = justPressed,
    ): Boolean {
        val replayInput = extractReplayInput(inputHandler)
        replayInput.frame(justPressed = justPressed, pressed = pressed)
        val previousSnapshot = session.renderSnapshot()
        val command = inputHandler.pollCommand(previousSnapshot)
        val consumed = command?.let(session::perform) ?: false
        if (command != null) {
            val currentSnapshot = session.renderSnapshot()
            inputHandler.onCommandResult(currentSnapshot, command, consumed)
        }
        replayInput.clear()
        return consumed
    }

    private fun pressMenu(
        controller: MainMenuController,
        input: ReplayInputSource,
        hasSave: Boolean,
        justPressed: Set<Int>,
        pressed: Set<Int> = justPressed,
    ): MainMenuAction? {
        input.frame(justPressed = justPressed, pressed = pressed)
        val action = controller.pollAction(hasSave).action
        input.clear()
        return action
    }

    private fun stairPoint(
        session: FoundationGameSession,
        direction: StairDirection,
    ): Point? {
        val world = runtimeWorld(session)
        return world.entitiesWith(Position::class, Stair::class)
            .firstOrNull { entityId -> requireNotNull(world.get<Stair>(entityId)).direction == direction }
            ?.let { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
    }

    private fun clearRegularMonsters(session: FoundationGameSession) {
        val world = runtimeWorld(session)
        world.entitiesWith(MonsterTemplateId::class)
            .filter { entityId ->
                requireNotNull(world.get<MonsterTemplateId>(entityId)).value != FOUNDATION_BOSS_TEMPLATE_ID
            }
            .forEach(world::destroyEntity)
    }

    private fun blockingPoints(session: FoundationGameSession): Set<Point> {
        val world = runtimeWorld(session)
        return world.entitiesWith(Position::class, BlocksMovement::class)
            .filter { entityId -> entityId != session.playerId }
            .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
            .toSet()
    }

    private fun findOpenAdjacentPoint(
        session: FoundationGameSession,
        center: Point,
    ): Point {
        val occupied = blockingPoints(session)
        return Point.ALL_DIRECTIONS
            .map { delta -> center + delta }
            .firstOrNull { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            } ?: error("No open adjacent point around $center.")
    }

    private fun keyForDelta(delta: Point): Int =
        when (delta) {
            Point(0, -1) -> Keys.UP
            Point(1, -1) -> Keys.E
            Point(1, 0) -> Keys.RIGHT
            Point(1, 1) -> Keys.C
            Point(0, 1) -> Keys.DOWN
            Point(-1, 1) -> Keys.Z
            Point(-1, 0) -> Keys.LEFT
            Point(-1, -1) -> Keys.Q
            else -> error("Unsupported movement delta $delta.")
        }

    private fun runtimeWorld(session: FoundationGameSession): World {
        val field = FoundationGameSession::class.java.getDeclaredField("world")
        field.isAccessible = true
        return field.get(session) as World
    }

    private fun extractReplayInput(inputHandler: InputHandler): ReplayInputSource {
        val field = InputHandler::class.java.getDeclaredField("input")
        field.isAccessible = true
        return field.get(inputHandler) as ReplayInputSource
    }
}
