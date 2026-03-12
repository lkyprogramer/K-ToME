package com.ktome.game

import com.ktome.core.combat.CombatResolver
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.map.BspConfig
import com.ktome.core.map.BspGenerator
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.random.RandomSource
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.EntityFactory
import com.ktome.game.model.MonsterTemplate
import kotlin.random.Random

object GameModule {
    fun newFoundationSession(config: FoundationGameConfig = FoundationGameConfig()): FoundationGameSession {
        val map = BspGenerator(
            seed = config.seed,
            config = BspConfig(width = config.width, height = config.height),
        ).generate()

        val loader = DataLoader()
        val catalog = loader.loadMonsterCatalog()
        val world = com.ktome.core.ecs.World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, map.playerStart)

        spawnMonsters(
            factory = factory,
            world = world,
            map = map,
            config = config,
            catalog = catalog.monsters,
        )

        return FoundationGameSession(
            config = config,
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = CombatResolver(RandomSource.from(Random(config.seed xor 0xC0FFEE))),
        )
    }

    private fun spawnMonsters(
        factory: EntityFactory,
        world: com.ktome.core.ecs.World,
        map: com.ktome.core.map.GameMap,
        config: FoundationGameConfig,
        catalog: List<MonsterTemplate>,
    ) {
        val availableTemplates = catalog.filter { config.floor in it.spawnFloors }
        val behaviorPriority = listOf(AIType.CHASE, AIType.KITE, AIType.PATROL)
        val selectedTemplates = behaviorPriority.mapNotNull { behavior ->
            availableTemplates.firstOrNull { it.ai == behavior }
        }
        val occupiedPoints = linkedSetOf(map.playerStart)

        map.rooms.drop(1).zip(selectedTemplates).forEach { (room, template) ->
            val spawnPoint = findSpawnPoint(room, map, occupiedPoints)
            factory.createMonster(
                world = world,
                template = template,
                position = spawnPoint,
                patrolRoute = if (template.ai == AIType.PATROL) buildPatrolRoute(room) else null,
            )
            occupiedPoints += spawnPoint
        }
    }

    private fun findSpawnPoint(
        room: Room,
        map: com.ktome.core.map.GameMap,
        occupiedPoints: Set<Point>,
    ): Point {
        val candidates = sequenceOf(
            room.center,
            Point(room.left + 1, room.top + 1),
            Point(room.right - 1, room.bottom - 1),
            Point(room.left + 1, room.bottom - 1),
            Point(room.right - 1, room.top + 1),
        ).distinct()

        return candidates.firstOrNull { point ->
            !map[point].blocksMovement && point !in occupiedPoints
        } ?: room.center
    }

    private fun buildPatrolRoute(room: Room): PatrolRoute {
        val points = listOf(
            Point(room.left + 1, room.top + 1),
            Point(room.right - 1, room.top + 1),
            Point(room.right - 1, room.bottom - 1),
            Point(room.left + 1, room.bottom - 1),
        ).distinct()
        return PatrolRoute(points)
    }
}
