package com.ktome.game

import com.ktome.core.dungeon.DungeonManager
import com.ktome.core.dungeon.FloorState
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.map.BspConfig
import com.ktome.core.map.BspGenerator
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.core.save.SaveManager
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.BossFactory
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.model.MonsterTemplate
import com.ktome.core.item.ItemGenerator
import java.nio.file.Path
import kotlin.random.Random

object GameModule {
    fun newFoundationSession(
        config: FoundationGameConfig = FoundationGameConfig(),
        saveManager: SaveManager = SaveManager(defaultSaveDir()),
    ): FoundationGameSession {
        val content = loadContent()
        val playerSnapshot = createInitialPlayerSnapshot(content, Point.ZERO)
        val dungeonManager =
            DungeonManager(
                maxFloor = config.maxFloor,
                startFloor = config.floor,
                floorLoader = { floor -> generateFloor(content, config, floor) },
            )
        val startFloor = dungeonManager.currentState()
        val startingPlayer = playerSnapshot.copy(entity = playerSnapshot.entity.copy(position = startFloor.payload.map.playerStart))
        return FoundationGameSession(
            config = config,
            content = content,
            saveManager = saveManager,
            dungeonManager = dungeonManager,
            playerSnapshot = startingPlayer,
            initialMessageLog = listOf("You enter the dungeon."),
        )
    }

    fun loadFoundationSession(saveManager: SaveManager): FoundationGameSession? {
        val snapshot = saveManager.load() ?: return null
        val loader = DataLoader()
        val talents = loader.loadTalentDefinitions()
        val restored = SessionSnapshotMapper.fromSaveSnapshot(snapshot)
        val content =
            GameContent(
                talents = talents,
                talentRegistry = com.ktome.core.talent.TalentRegistry().apply { registerAll(talents) },
                monsterCatalog = loader.loadMonsterCatalog().monsters,
                itemBundle = loader.loadItemBundle(),
                bossDefinition = loader.loadBossDefinition(),
            )
        val dungeonManager =
            DungeonManager(
                maxFloor = restored.config.maxFloor,
                startFloor = restored.currentFloor,
                floorLoader = { floor ->
                    restored.floors.firstOrNull { it.floor == floor }
                        ?: generateFloor(content, restored.config, floor)
                },
            )
        restored.floors.forEach { floorState ->
            dungeonManager.putState(floorState)
        }
        return FoundationGameSession(
            config = restored.config,
            content = content,
            saveManager = saveManager,
            dungeonManager = dungeonManager,
            playerSnapshot = restored.player,
            initialMessageLog = restored.messageLog + "Game loaded.",
            turnCount = restored.turnCount,
            combatRandomSource =
                restored.combatRandomState?.let(SplitMix64RandomSource::fromState)
                    ?: FoundationGameSession.defaultCombatRandomSource(restored.config, restored.turnCount),
            sessionRandom =
                restored.sessionRandomState?.let(SplitMix64RandomSource::fromState)
                    ?: FoundationGameSession.defaultSessionRandomSource(restored.config, restored.turnCount),
            restoredPendingActionIds = restored.pendingActionIds,
            restoredActiveTurnActorId = restored.activeTurnActorId,
        )
    }

    private fun loadContent(): GameContent {
        val loader = DataLoader()
        val talents = loader.loadTalentDefinitions()
        return GameContent(
            talents = talents,
            talentRegistry = com.ktome.core.talent.TalentRegistry().apply { registerAll(talents) },
            monsterCatalog = loader.loadMonsterCatalog().monsters,
            itemBundle = loader.loadItemBundle(),
            bossDefinition = loader.loadBossDefinition(),
        )
    }

    private fun createInitialPlayerSnapshot(
        content: GameContent,
        position: Point,
    ): com.ktome.core.save.PlayerSnapshot {
        val world = World()
        val playerId = EntityFactory().createPlayer(world, position, content.talents)
        return SessionSnapshotMapper.capturePlayer(world, playerId)
    }

    private fun generateFloor(
        content: GameContent,
        config: FoundationGameConfig,
        floor: Int,
    ): FloorState<FloorRuntimeState> {
        val map =
            BspGenerator(
                seed = floorSeed(config.seed, floor, 0x44A1),
                config = BspConfig(width = config.width, height = config.height),
            ).generate()
        val world = World()
        reserveEntityRange(world, floor)
        val factory = EntityFactory()
        val itemFactory = ItemFactory()
        val bossFactory = BossFactory(factory)
        val itemGenerator = ItemGenerator(content.itemBundle, com.ktome.core.random.RandomSource.from(Random(floorSeed(config.seed, floor, 0x91F3))))
        val stairsUp = if (floor > 1) map.playerStart else null
        val stairsDown = if (floor < config.maxFloor) chooseDownstairs(map, stairsUp) else null
        val occupiedPoints =
            linkedSetOf<Point>().apply {
                stairsUp?.let(::add)
                stairsDown?.let(::add)
            }

        stairsUp?.let { point -> createStair(world, point, StairDirection.UP) }
        stairsDown?.let { point -> createStair(world, point, StairDirection.DOWN) }

        if (floor == config.maxFloor) {
            val bossPosition = chooseBossPosition(map, occupiedPoints)
            bossFactory.createBoss(world, content.bossDefinition, bossPosition)
            occupiedPoints += bossPosition
        } else {
            spawnMonsters(
                factory = factory,
                world = world,
                map = map,
                floor = floor,
                catalog = content.monsterCatalog,
                occupiedPoints = occupiedPoints,
            )
            spawnItems(
                itemFactory = itemFactory,
                itemGenerator = itemGenerator,
                world = world,
                map = map,
                floor = floor,
                occupiedPoints = occupiedPoints,
            )
        }

        return FloorState(
            floor = floor,
            stairsUp = stairsUp,
            stairsDown = stairsDown,
            payload =
                SessionSnapshotMapper.captureFloor(
                    map = map,
                    stairsUp = stairsUp,
                    stairsDown = stairsDown,
                    exploredTiles = emptySet(),
                    world = world,
                    excludedEntities = emptySet(),
                ),
        )
    }

    private fun spawnMonsters(
        factory: EntityFactory,
        world: World,
        map: com.ktome.core.map.GameMap,
        floor: Int,
        catalog: List<MonsterTemplate>,
        occupiedPoints: MutableSet<Point>,
    ) {
        val availableTemplates = catalog.filter { floor in it.spawnFloors }
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
        world: World,
        map: com.ktome.core.map.GameMap,
        floor: Int,
        occupiedPoints: MutableSet<Point>,
    ) {
        val itemRooms = map.rooms.drop(1).take(4)
        itemRooms.forEach { room ->
            val spawnPoint = findSpawnPoint(room, map, occupiedPoints)
            itemFactory.createGroundItem(world, itemGenerator.generate(floor), spawnPoint)
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

    private fun chooseDownstairs(
        map: com.ktome.core.map.GameMap,
        upstairs: Point?,
    ): Point {
        val candidates = map.rooms.asReversed().map(Room::center) + map.floorPoints()
        return candidates.first { point -> point != upstairs }
    }

    private fun chooseBossPosition(
        map: com.ktome.core.map.GameMap,
        occupiedPoints: Set<Point>,
    ): Point = map.rooms.asReversed().map(Room::center).firstOrNull { it !in occupiedPoints } ?: map.floorPoints().first { it !in occupiedPoints }

    private fun createStair(
        world: World,
        position: Point,
        direction: StairDirection,
    ) {
        val stairId = world.createEntity()
        world.add(stairId, Position(position.x, position.y))
        world.add(stairId, Glyph(if (direction == StairDirection.DOWN) '>' else '<'))
        world.add(stairId, DisplayColor("#D7E7FF"))
        world.add(stairId, Name(if (direction == StairDirection.DOWN) "Downstairs" else "Upstairs"))
        world.add(stairId, Stair(direction))
    }

    private fun floorSeed(
        seed: Long,
        floor: Int,
        salt: Int,
    ): Long = seed xor (floor.toLong() shl 32) xor salt.toLong()

    private fun reserveEntityRange(
        world: World,
        floor: Int,
    ) {
        val reservation = com.ktome.core.ecs.EntityId(floor * 1_000)
        world.createEntity(reservation)
        world.destroyEntity(reservation)
    }

    private fun defaultSaveDir(): Path = Path.of(System.getProperty("user.home"), ".ktome")
}
