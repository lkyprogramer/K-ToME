package com.ktome.game

import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.ExperienceReward
import com.ktome.core.ecs.Faction
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.game.factory.EntityFactory
import com.ktome.game.model.MonsterTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class EntityFactoryTest {
    private val factory = EntityFactory()

    @Test
    fun `createPlayer installs expected player components`() {
        val world = World()

        val playerId = factory.createPlayer(world, Point(4, 5))

        assertEquals(Point(4, 5), requireNotNull(world.get<Position>(playerId)).toPoint())
        assertEquals(Faction.PLAYER, requireNotNull(world.get<FactionTag>(playerId)).value)
        assertEquals("Hero", requireNotNull(world.get<Name>(playerId)).value)
        assertNotNull(world.get<Health>(playerId))
        assertNotNull(world.get<Experience>(playerId))
        assertEquals("#FFD700", requireNotNull(world.get<DisplayColor>(playerId)).hex)
    }

    @Test
    fun `createMonster installs combat ai and reward components`() {
        val world = World()
        val template = MonsterTemplate(
            id = "sentry",
            name = "Sentry",
            glyph = 'g',
            colorHex = "#00FF00",
            stats = com.ktome.core.ecs.Stats(str = 6, dex = 5, con = 6, wil = 1),
            baseHp = 12,
            baseAttack = 4,
            baseDefense = 2,
            speed = 95,
            ai = com.ktome.core.ecs.AIType.PATROL,
            expReward = 18,
            spawnFloors = listOf(1),
            spawnWeight = 4,
        )

        val monsterId = factory.createMonster(
            world = world,
            template = template,
            position = Point(6, 7),
            patrolRoute = PatrolRoute(listOf(Point(6, 7), Point(7, 7))),
        )

        assertEquals(Point(6, 7), requireNotNull(world.get<Position>(monsterId)).toPoint())
        assertEquals(Faction.MONSTER, requireNotNull(world.get<FactionTag>(monsterId)).value)
        assertEquals(18, requireNotNull(world.get<ExperienceReward>(monsterId)).value)
        assertEquals("Sentry", requireNotNull(world.get<Name>(monsterId)).value)
        assertEquals(com.ktome.core.ecs.AIType.PATROL, requireNotNull(world.get<AIBehavior>(monsterId)).type)
        assertEquals(0, requireNotNull(world.get<PatrolRoute>(monsterId)).nextWaypointIndex)
    }
}
