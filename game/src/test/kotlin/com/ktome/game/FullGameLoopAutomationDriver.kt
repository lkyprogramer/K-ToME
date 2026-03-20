package com.ktome.game

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.game.factory.EntityFactory
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import com.ktome.game.harness.consumesTurn
import com.ktome.game.model.MonsterTemplate
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal class FullGameLoopAutomationDriver(
    private val saveManager: SaveManager,
    private val config: FoundationGameConfig = FoundationGameConfig(zoneId = "shattered_outpost"),
) {
    fun canContinue(): Boolean = saveManager.hasSave()

    fun newGame(): FoundationGameSession {
        saveManager.deleteSave()
        return GameModule.newFoundationSession(config = config, saveManager = saveManager)
    }

    fun continueGame(): FoundationGameSession? = GameModule.loadFoundationSession(saveManager)

    fun descendToFloor(
        session: FoundationGameSession,
        targetFloor: Int,
    ) {
        val bot = SmokeBot()
        var turnCount = 0
        var observation = RunObservationCapture.capture(session, turnCount)
        while (session.currentFloor() < targetFloor && !observation.runOutcome.isTerminal && turnCount < 700) {
            val command = requireNotNull(bot.decide(observation)) { "Bot failed to provide a command before floor $targetFloor." }
            assertTrue(session.perform(command), "Command rejected while descending to floor $targetFloor: $command")
            if (command.consumesTurn()) {
                turnCount += 1
            }
            observation = RunObservationCapture.capture(session, turnCount)
        }
        assertTrue(session.currentFloor() >= targetFloor, "Failed to reach floor $targetFloor, last messages=${observation.messageLogTail}")
    }

    fun saveAndRestart(session: FoundationGameSession): FoundationGameSession {
        assertTrue(session.perform(PlayerCommand.SaveGame))
        assertTrue(canContinue())
        return requireNotNull(continueGame()) { "Continue should load the saved game." }
    }

    fun killBossForVictory(session: FoundationGameSession) {
        val bot = SmokeBot()
        var turnCount = 0
        var observation = RunObservationCapture.capture(session, turnCount)
        while (turnCount < 900 && !observation.runOutcome.isTerminal) {
            val command = requireNotNull(bot.decide(observation)) { "Bot failed to provide a command." }
            assertTrue(session.perform(command), "Command rejected during victory automation: $command")
            if (command.consumesTurn()) {
                turnCount += 1
            }
            observation = RunObservationCapture.capture(session, turnCount)
        }
        assertTrue(session.isVictory(), "Expected real victory path, last messages=${observation.messageLogTail}")
        assertFalse(canContinue())
    }

    fun forceGameOver(session: FoundationGameSession) {
        val world = session.automationWorld()
        val playerPosition = session.playerPosition()
        val killerPosition = findOpenAdjacentPoint(session, playerPosition)
        EntityFactory().createMonster(
            world = world,
            template =
                MonsterTemplate(
                    id = "automation_killer",
                    name = "Automation Killer",
                    glyph = 'K',
                    colorHex = "#FF4444",
                    stats = com.ktome.core.ecs.Stats(str = 12, dex = 1, con = 1, wil = 1),
                    baseHp = 10,
                    baseAttack = 12,
                    baseDefense = 0,
                    speed = 90,
                    ai = AIType.CHASE,
                    expReward = 0,
                    spawnFloors = listOf(session.currentFloor()),
                    spawnWeight = 1,
                ),
            position = killerPosition,
        )
        requireNotNull(world.get<Health>(session.playerId)).current = 1

        repeat(4) {
            if (!session.isGameOver()) {
                assertTrue(session.perform(PlayerCommand.Wait))
            }
        }
        assertTrue(session.isGameOver())
        assertFalse(canContinue())
    }

    private fun findOpenAdjacentPoint(
        session: FoundationGameSession,
        center: Point,
    ): Point {
        val world = session.automationWorld()
        val occupied =
            world.entitiesWith(Position::class, BlocksMovement::class)
                .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
                .toSet()

        return Point.ALL_DIRECTIONS
            .map { delta -> center + delta }
            .firstOrNull { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            } ?: error("No open adjacent point around $center.")
    }
}
