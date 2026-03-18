package com.ktome.game

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.game.factory.EntityFactory
import com.ktome.game.model.MonsterTemplate
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

internal class FullGameLoopAutomationDriver(
    private val saveManager: SaveManager,
    private val config: FoundationGameConfig = FoundationGameConfig(),
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
        while (session.currentFloor() < targetFloor) {
            val stair = requireNotNull(session.automationStairPoint(StairDirection.DOWN)) {
                "No downstairs on floor ${session.currentFloor()}."
            }
            session.automationMovePlayerTo(stair)
            assertTrue(session.perform(PlayerCommand.Descend))
        }
    }

    fun saveAndRestart(session: FoundationGameSession): FoundationGameSession {
        assertTrue(session.perform(PlayerCommand.SaveGame))
        assertTrue(canContinue())
        return requireNotNull(continueGame()) { "Continue should load the saved game." }
    }

    fun killBossForVictory(session: FoundationGameSession) {
        val world = session.automationWorld()
        val bossId =
            world.entitiesWith(MonsterTemplateId::class, Position::class, Health::class)
                .single { entityId ->
                    requireNotNull(world.get<MonsterTemplateId>(entityId)).value == FOUNDATION_BOSS_TEMPLATE_ID
                }
        requireNotNull(world.get<Health>(bossId)).current = 1
        val bossPosition = requireNotNull(world.get<Position>(bossId)).toPoint()
        val attackOrigin = findOpenAdjacentPoint(session, bossPosition)

        session.automationMovePlayerTo(attackOrigin)
        assertTrue(session.perform(PlayerCommand.Move(bossPosition - attackOrigin)))
        assertTrue(session.isVictory())
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
