package com.ktome.game

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.game.factory.EntityFactory
import com.ktome.game.harness.RunObservation
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import com.ktome.game.harness.consumesTurn
import com.ktome.game.model.MonsterTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal class FullGameLoopAutomationDriver(
    private val saveManager: SaveManager,
    private val config: FoundationGameConfig = FoundationGameConfig(zoneId = "shattered_outpost"),
) {
    private val bot = SmokeBot()

    fun canContinue(): Boolean = saveManager.hasSave()

    fun newGame(): FoundationGameSession {
        saveManager.deleteSave()
        return GameModule.newFoundationSession(config = config, saveManager = saveManager).also(::stabilizeAutomationSession)
    }

    fun continueGame(): FoundationGameSession? = GameModule.loadFoundationSession(saveManager)

    fun descendToFloor(
        session: FoundationGameSession,
        targetFloor: Int,
    ) {
        val result =
            driveUntil(session, maxTurns = 700) { observation ->
                observation.floor >= targetFloor
            }
        assertTrue(
            session.currentFloor() >= targetFloor,
            "Failed to reach floor $targetFloor, last messages=${result.observation.messageLogTail}",
        )
    }

    fun advanceUntilZone(
        session: FoundationGameSession,
        targetZoneId: String,
    ) {
        val result =
            driveUntil(session, maxTurns = 2200) {
                session.config.zoneId == targetZoneId || it.runOutcome.isTerminal
            }
        assertTrue(
            session.config.zoneId == targetZoneId,
            "Failed to reach zone $targetZoneId, last messages=${result.observation.messageLogTail}",
        )
    }

    fun saveAndRestart(session: FoundationGameSession): FoundationGameSession {
        val persistedPlayerState = capturePlayerState(session)
        assertTrue(session.perform(PlayerCommand.SaveGame))
        assertTrue(canContinue())
        val continued = requireNotNull(continueGame()) { "Continue should load the saved game." }
        assertEquals(
            persistedPlayerState,
            capturePlayerState(continued),
            "Continue should preserve the saved player state exactly.",
        )
        return continued
    }

    fun killBossForVictory(session: FoundationGameSession) {
        val result = driveUntil(session, maxTurns = 900) { observation -> observation.runOutcome.isTerminal }
        assertTrue(session.isVictory(), "Expected real victory path, last messages=${result.observation.messageLogTail}")
        assertFalse(canContinue())
    }

    fun completeRunToRouteTerminal(session: FoundationGameSession) {
        val result = driveUntil(session, maxTurns = 3200) { observation -> observation.runOutcome.isTerminal }
        assertTrue(session.runOutcome().isTerminal, "Expected route terminal outcome, last messages=${result.observation.messageLogTail}")
        assertTrue(session.config.zoneId == FOUNDATION_ZONE_ROUTE.last(), "Expected terminal route to reach final zone, last messages=${result.observation.messageLogTail}")
        assertTrue(session.currentFloor() >= 2, "Expected terminal route to reach final floor coverage, last messages=${result.observation.messageLogTail}")
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

    private fun driveUntil(
        session: FoundationGameSession,
        maxTurns: Int,
        stopWhen: (RunObservation) -> Boolean,
    ): DriveResult {
        var turnCount = 0
        var totalRejectedCommands = 0
        var observation = RunObservationCapture.capture(session, turnCount)
        while (turnCount < maxTurns && !stopWhen(observation)) {
            val command = requireNotNull(bot.decide(observation)) { "Bot failed to provide a command." }
            if (!session.perform(command)) {
                totalRejectedCommands += 1
                assertTrue(totalRejectedCommands <= 20, "Command rejected during automation: $command")
                observation = RunObservationCapture.capture(session, turnCount)
                continue
            }
            if (command.consumesTurn()) {
                turnCount += 1
            }
            observation = RunObservationCapture.capture(session, turnCount)
        }
        return DriveResult(
            turnCount = turnCount,
            observation = observation,
            totalRejectedCommands = totalRejectedCommands,
        )
    }

    private data class DriveResult(
        val turnCount: Int,
        val observation: RunObservation,
        val totalRejectedCommands: Int,
    )

    private data class ResourcePoolState(
        val current: Int,
        val max: Int,
    )

    private data class PlayerStateSnapshot(
        val position: Point,
        val stats: Stats,
        val combatProfile: CombatProfile?,
        val derivedStats: DerivedStats?,
        val health: Health?,
        val resourcePools: Map<ResourceType, ResourcePoolState>,
    )

    private fun stabilizeAutomationSession(session: FoundationGameSession) {
        val world = session.automationWorld()
        requireNotNull(world.get<Stats>(session.playerId)).apply {
            str = maxOf(str, 18)
            dex = maxOf(dex, 40)
            con = maxOf(con, 18)
            wil = maxOf(wil, 20)
        }
        world.get<CombatProfile>(session.playerId)?.let { profile ->
            world.add(
                session.playerId,
                profile.copy(
                    baseAttack = maxOf(profile.baseAttack, 16),
                    baseDefense = maxOf(profile.baseDefense, 10),
                    baseAccuracy = maxOf(profile.baseAccuracy, 120),
                    baseHp = maxOf(profile.baseHp, 220),
                    baseStamina = maxOf(profile.baseStamina, 120),
                ),
            )
        }
        StatsCalculator.recalculateAndStore(world, session.playerId)
        world.get<Health>(session.playerId)?.let { health -> health.current = health.max }
        world.get<ResourcePools>(session.playerId)?.entries?.values?.forEach { pool -> pool.current = pool.max }
    }

    private fun capturePlayerState(session: FoundationGameSession): PlayerStateSnapshot {
        val world = session.automationWorld()
        val playerId = session.playerId
        return PlayerStateSnapshot(
            position = requireNotNull(world.get<Position>(playerId)).toPoint(),
            stats = requireNotNull(world.get<Stats>(playerId)).copy(),
            combatProfile = world.get<CombatProfile>(playerId)?.copy(),
            derivedStats = world.get<DerivedStats>(playerId)?.copy(),
            health = world.get<Health>(playerId)?.copy(),
            resourcePools =
                world.get<ResourcePools>(playerId)?.entries
                    ?.mapValues { (_, pool) -> ResourcePoolState(current = pool.current, max = pool.max) }
                    ?.toMap()
                    ?: emptyMap(),
        )
    }
}
