package com.ktome.game

import com.ktome.core.combat.CombatResolver
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.map.BspConfig
import com.ktome.core.map.BspGenerator
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.random.RandomSource
import com.ktome.core.talent.TalentRegistry
import com.ktome.game.factory.ItemFactory
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.EntityFactory
import com.ktome.game.model.MonsterTemplate
import com.ktome.core.item.ItemGenerator
import com.ktome.core.talent.TalentResolver
import kotlin.random.Random

object GameModule {
    fun newFoundationSession(config: FoundationGameConfig = FoundationGameConfig()): FoundationGameSession {
        val map = BspGenerator(
            seed = config.seed,
            config = BspConfig(width = config.width, height = config.height),
        ).generate()

        val loader = DataLoader()
        val catalog = loader.loadMonsterCatalog()
        val itemBundle = loader.loadItemBundle()
        val talents = loader.loadTalentDefinitions()
        val world = com.ktome.core.ecs.World()
        val factory = EntityFactory()
        val itemFactory = ItemFactory()
        val combatResolver = CombatResolver(RandomSource.from(Random(config.seed xor 0xC0FFEE)))
        val talentRegistry = TalentRegistry().apply { registerAll(talents) }
        val playerId = factory.createPlayer(world, map.playerStart, talents)
        val itemGenerator = ItemGenerator(itemBundle, RandomSource.from(Random(config.seed xor 0x1A2B3C4D)))
        val occupiedPoints = linkedSetOf(map.playerStart)

        spawnMonsters(
            factory = factory,
            world = world,
            map = map,
            config = config,
            catalog = catalog.monsters,
            occupiedPoints = occupiedPoints,
        )
        spawnItems(
            itemFactory = itemFactory,
            itemGenerator = itemGenerator,
            world = world,
            map = map,
            config = config,
            occupiedPoints = occupiedPoints,
        )

        return FoundationGameSession(
            config = config,
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = combatResolver,
            talentRegistry = talentRegistry,
            talentResolver = TalentResolver(talentRegistry, combatResolver),
            sessionRandom = RandomSource.from(Random(config.seed xor 0x51A17A)),
        )
    }

    private fun spawnMonsters(
        factory: EntityFactory,
        world: com.ktome.core.ecs.World,
        map: com.ktome.core.map.GameMap,
        config: FoundationGameConfig,
        catalog: List<MonsterTemplate>,
        occupiedPoints: MutableSet<Point>,
    ) {
        val availableTemplates = catalog.filter { config.floor in it.spawnFloors }
        val behaviorPriority = listOf(AIType.CHASE, AIType.KITE, AIType.PATROL)
        val selectedTemplates = behaviorPriority.mapNotNull { behavior ->
            availableTemplates.firstOrNull { it.ai == behavior }
        }

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

    private fun spawnItems(
        itemFactory: ItemFactory,
        itemGenerator: ItemGenerator,
        world: com.ktome.core.ecs.World,
        map: com.ktome.core.map.GameMap,
        config: FoundationGameConfig,
        occupiedPoints: MutableSet<Point>,
    ) {
        val itemRooms = map.rooms.drop(1).take(4)
        itemRooms.forEach { room ->
            val spawnPoint = findSpawnPoint(room, map, occupiedPoints)
            itemFactory.createGroundItem(world, itemGenerator.generate(config.floor), spawnPoint)
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
