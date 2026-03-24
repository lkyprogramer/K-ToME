package com.ktome.game

import com.ktome.core.ai.AIPerceptionState
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
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

class TauntAiInteractionTest {
    @TempDir
    lateinit var tempDir: Path

    private val ratTemplate = DataLoader().loadMonsterCatalog().monsters.first { monster -> monster.id == "beast.rat" }

    @Test
    fun `taunt makes ai move toward the taunt source instead of the player`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260324L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("taunt-ai")),
            )
        val world = session.automationWorld()
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)

        val playerPoint = session.playerPosition()
        val monsterPosition = findOpenPointAtDistance(session, playerPoint, 3)
        val monsterId =
            EntityFactory().createMonster(
                world = world,
                template = ratTemplate,
                position = monsterPosition,
            )
        val tauntSourcePoint = findOpenPointAtDistance(session, monsterPosition, 3, exclude = setOf(playerPoint))
        val tauntSourceId = installTauntSource(world, tauntSourcePoint)

        StatusLifecycle.applyEffect(
            requireNotNull(world.get<EffectTracker>(monsterId)),
            StatusLifecycle.createInstance(
                type = StatusEffectType.TAUNT,
                effectId = "forced_taunt",
                duration = 2,
                sourceEntityId = tauntSourceId,
            ),
        )

        val before = requireNotNull(world.get<Position>(monsterId)).toPoint()
        assertTrue(session.perform(PlayerCommand.Wait))
        val after = requireNotNull(world.get<Position>(monsterId)).toPoint()

        assertTrue(after.chebyshevDistanceTo(tauntSourcePoint) < before.chebyshevDistanceTo(tauntSourcePoint))
        assertTrue(after.chebyshevDistanceTo(playerPoint) >= before.chebyshevDistanceTo(playerPoint))
    }

    @Test
    fun `taunted ai does not see through stealth on a non player target`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260324L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("taunt-stealth-ai")),
            )
        val world = session.automationWorld()
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)

        val playerPoint = session.playerPosition()
        val monsterPosition = findOpenPointAtDistance(session, playerPoint, 3)
        val monsterId =
            EntityFactory().createMonster(
                world = world,
                template = ratTemplate,
                position = monsterPosition,
            )
        val tauntSourcePoint = findOpenPointAtDistance(session, monsterPosition, 3, exclude = setOf(playerPoint))
        val tauntSourceId = installTauntSource(world, tauntSourcePoint)

        StatusLifecycle.applyEffect(
            requireNotNull(world.get<EffectTracker>(monsterId)),
            StatusLifecycle.createInstance(
                type = StatusEffectType.TAUNT,
                effectId = "forced_taunt_hidden_target",
                duration = 3,
                sourceEntityId = tauntSourceId,
            ),
        )

        assertTrue(session.perform(PlayerCommand.Wait))
        val perception = requireNotNull(world.get<AIPerceptionState>(monsterId))
        assertEquals(tauntSourcePoint, perception.lastKnownTargetPosition)

        val hiddenDestination = findOpenPointAtDistance(session, tauntSourcePoint, 4, exclude = setOf(playerPoint))
        StatusLifecycle.applyEffect(
            requireNotNull(world.get<EffectTracker>(tauntSourceId)),
            StatusLifecycle.createInstance(
                type = StatusEffectType.STEALTH,
                effectId = "taunt_source_stealth",
                duration = 2,
                sourceEntityId = tauntSourceId,
            ),
        )
        requireNotNull(world.get<Position>(tauntSourceId)).moveTo(hiddenDestination)

        val before = requireNotNull(world.get<Position>(monsterId)).toPoint()
        assertTrue(session.perform(PlayerCommand.Wait))
        val after = requireNotNull(world.get<Position>(monsterId)).toPoint()

        assertEquals(tauntSourcePoint, perception.lastKnownTargetPosition)
        assertTrue(after.chebyshevDistanceTo(tauntSourcePoint) < before.chebyshevDistanceTo(tauntSourcePoint))
    }

    private fun installTauntSource(
        world: World,
        point: Point,
    ): com.ktome.core.ecs.EntityId =
        world.createEntity().also { entityId ->
            world.add(entityId, Position(point.x, point.y))
            world.add(entityId, Health(current = 30, max = 30))
            world.add(entityId, Name("Taunt Totem"))
            world.add(entityId, EffectTracker())
        }

    private fun findOpenPointAtDistance(
        session: FoundationGameSession,
        center: Point,
        distance: Int,
        exclude: Set<Point> = emptySet(),
    ): Point {
        val world = session.automationWorld()
        val occupied = world.entitiesWith(Position::class).mapTo(linkedSetOf()) { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
        return session.map.floorPoints()
            .filter { point ->
                point.chebyshevDistanceTo(center) == distance &&
                    point !in occupied &&
                    point !in exclude
            }
            .sortedWith(compareBy<Point>(Point::y).thenBy(Point::x))
            .first()
    }
}
