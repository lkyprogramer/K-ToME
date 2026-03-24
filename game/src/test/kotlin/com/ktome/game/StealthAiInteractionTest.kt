package com.ktome.game

import com.ktome.core.ai.AIPerceptionState
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusLifecycle
import com.ktome.core.talent.EffectTracker
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.EntityFactory
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class StealthAiInteractionTest {
    @TempDir
    lateinit var tempDir: Path

    private val ratTemplate = DataLoader().loadMonsterCatalog().monsters.first { monster -> monster.id == "beast.rat" }

    @Test
    fun `stealth makes ai chase the last known player position instead of the hidden target`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260324L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("stealth-ai")),
            )
        val world = session.automationWorld()
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)

        val visiblePlayerPoint = session.playerPosition()
        val monsterId =
            EntityFactory().createMonster(
                world = world,
                template = ratTemplate,
                position = findOpenPointAtDistance(session, visiblePlayerPoint, 3),
            )

        assertTrue(session.perform(PlayerCommand.Wait))
        val perception = requireNotNull(world.get<AIPerceptionState>(monsterId))
        assertEquals(visiblePlayerPoint, perception.lastKnownTargetPosition)

        val hiddenDestination = findOpenPointAtDistance(session, visiblePlayerPoint, 5)
        StatusLifecycle.applyEffect(
            requireNotNull(world.get<EffectTracker>(session.playerId)),
            StatusLifecycle.createInstance(
                type = StatusEffectType.STEALTH,
                effectId = "player_stealth",
                duration = 2,
                sourceEntityId = session.playerId,
            ),
        )
        session.automationMovePlayerTo(hiddenDestination)

        val before = requireNotNull(world.get<Position>(monsterId)).toPoint()
        assertTrue(session.perform(PlayerCommand.Wait))
        val after = requireNotNull(world.get<Position>(monsterId)).toPoint()

        assertTrue(after.chebyshevDistanceTo(visiblePlayerPoint) < before.chebyshevDistanceTo(visiblePlayerPoint))
        assertTrue(after.chebyshevDistanceTo(hiddenDestination) >= 1)
        assertEquals(visiblePlayerPoint, perception.lastKnownTargetPosition)
    }

    private fun findOpenPointAtDistance(
        session: FoundationGameSession,
        center: Point,
        distance: Int,
    ): Point {
        val world = session.automationWorld()
        val occupied = world.entitiesWith(Position::class).mapTo(linkedSetOf()) { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
        return session.map.floorPoints()
            .filter { point ->
                point.chebyshevDistanceTo(center) == distance &&
                    point !in occupied
            }
            .sortedWith(compareBy<Point>(Point::y).thenBy(Point::x))
            .first()
    }
}
