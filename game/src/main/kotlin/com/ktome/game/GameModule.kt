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
import com.ktome.core.ecs.get
import com.ktome.core.map.BspConfig
import com.ktome.core.map.BspGenerator
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.core.save.InvalidSaveException
import com.ktome.core.save.SaveManager
import com.ktome.core.save.PointSnapshot
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.i18n.GameLocale
import com.ktome.game.factory.BossFactory
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.model.MonsterTemplate
import com.ktome.core.item.ItemGenerator
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.Inventory
import com.ktome.core.item.InventoryManager
import java.nio.file.Path
import kotlin.random.Random
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.ZoneSchemaV2

object GameModule {
    fun newFoundationSession(
        config: FoundationGameConfig = FoundationGameConfig(),
        saveManager: SaveManager = SaveManager(defaultSaveDir()),
        locale: GameLocale = GameLocale.EN_US,
    ): FoundationGameSession {
        val content = loadContent(locale)
        validateNewSessionConfig(config, content.schemaCatalog)
        val profession = resolveProfession(content.schemaCatalog, config.playerProfessionId)
        val zone = resolveZone(content.schemaCatalog, config.zoneId)
        val sessionConfig = config.copy(width = zone.mapSize.width, height = zone.mapSize.height)
        val playerSnapshot = createInitialPlayerSnapshot(content, profession, Point.ZERO)
        val dungeonManager =
            DungeonManager(
                maxFloor = sessionConfig.maxFloor,
                startFloor = sessionConfig.floor,
                floorLoader = { floor -> generateFloor(content, sessionConfig, zone, floor) },
            )
        val startFloor = dungeonManager.currentState()
        val startingPlayer =
            playerSnapshot.copy(
                entity = playerSnapshot.entity.copy(position = PointSnapshot.from(startFloor.payload.map.playerStart)),
            )
        return FoundationGameSession(
            config = sessionConfig,
            content = content,
            saveManager = saveManager,
            dungeonManager = dungeonManager,
            playerSnapshot = startingPlayer,
            initialMessageLog = listOf(content.localizer.text("log.session.enter_dungeon")),
        )
    }

    fun loadFoundationSession(
        saveManager: SaveManager,
        locale: GameLocale = GameLocale.EN_US,
    ): FoundationGameSession? {
        val snapshot = saveManager.load() ?: return null
        val loader = DataLoader(locale)
        val schemaCatalog = loader.loadSchemaCatalog()
        val talents = loader.loadTalentDefinitions()
        val restored = SessionSnapshotMapper.fromSaveSnapshot(snapshot)
        val content =
            GameContent(
                talents = talents,
                talentRegistry = com.ktome.core.talent.TalentRegistry().apply { registerAll(talents) },
                monsterCatalog = loader.loadMonsterCatalog().monsters,
                itemBundle = loader.loadItemBundle(),
                bossDefinition = loader.loadBossDefinition(),
                schemaCatalog = schemaCatalog,
                localizer = loader.localizer,
            )
        validateLoadedSessionConfig(restored.config, schemaCatalog)
        val zone = resolveZone(schemaCatalog, restored.config.zoneId)
        val dungeonManager =
            DungeonManager(
                maxFloor = restored.config.maxFloor,
                startFloor = restored.currentFloor,
                floorLoader = { floor ->
                    restored.floors.firstOrNull { it.floor == floor }
                        ?: generateFloor(content, restored.config, zone, floor)
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
            initialMessageLog = listOf(content.localizer.text("log.session.loaded")),
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

    private fun loadContent(locale: GameLocale): GameContent {
        val loader = DataLoader(locale)
        val schemaCatalog = loader.loadSchemaCatalog()
        val talents = loader.loadTalentDefinitions()
        return GameContent(
            talents = talents,
            talentRegistry = com.ktome.core.talent.TalentRegistry().apply { registerAll(talents) },
            monsterCatalog = loader.loadMonsterCatalog().monsters,
            itemBundle = loader.loadItemBundle(),
            bossDefinition = loader.loadBossDefinition(),
            schemaCatalog = schemaCatalog,
            localizer = loader.localizer,
        )
    }

    private fun createInitialPlayerSnapshot(
        content: GameContent,
        profession: ProfessionSchemaV2,
        position: Point,
    ): com.ktome.core.save.PlayerSnapshot {
        val world = World()
        val playerId =
            EntityFactory().createPlayer(
                world = world,
                position = position,
                talents = resolveStartingTalents(content, profession),
                playerName = content.localizer.text("actor.player.name"),
                stats = profession.baseStats.toRuntimeStats(),
            )
        installStarterKit(world, playerId, resolveStarterItems(content, profession))
        return SessionSnapshotMapper.capturePlayer(world, playerId)
    }

    private fun generateFloor(
        content: GameContent,
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
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

        stairsUp?.let { point -> createStair(world, point, StairDirection.UP, content.localizer) }
        stairsDown?.let { point -> createStair(world, point, StairDirection.DOWN, content.localizer) }

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
                catalog = resolveZoneMonsterCatalog(content, zone, floor),
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
        val availableTemplates = catalog.filter { floor in it.spawnFloors }.ifEmpty { catalog }
        val behaviorPriority = listOf(AIType.CHASE, AIType.KITE, AIType.PATROL)
        val selectedTemplates = behaviorPriority.mapNotNull { behavior ->
            availableTemplates.firstOrNull { resolveAiType(it) == behavior }
        }

        map.rooms.drop(1).zip(selectedTemplates).forEach { (room, template) ->
            val spawnPoint = findSpawnPoint(room, map, occupiedPoints)
            factory.createMonster(
                world = world,
                template = template,
                position = spawnPoint,
                patrolRoute = if (resolveAiType(template) == AIType.PATROL) buildPatrolRoute(room) else null,
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
        localizer: com.ktome.game.i18n.Localizer,
    ) {
        val stairId = world.createEntity()
        world.add(stairId, Position(position.x, position.y))
        world.add(stairId, Glyph(if (direction == StairDirection.DOWN) '>' else '<'))
        world.add(stairId, DisplayColor("#D7E7FF"))
        world.add(
            stairId,
            Name(
                when (direction) {
                    StairDirection.DOWN -> localizer.text("stairs.down.name")
                    StairDirection.UP -> localizer.text("stairs.up.name")
                },
            ),
        )
        world.add(stairId, Stair(direction))
    }

    private fun resolveProfession(
        schemaCatalog: SchemaCatalog,
        professionId: String,
    ): ProfessionSchemaV2 =
        requireNotNull(schemaCatalog.professions.firstOrNull { it.id == professionId }) {
            "Unknown profession id '$professionId'."
        }

    private fun resolveZone(
        schemaCatalog: SchemaCatalog,
        zoneId: String,
    ): ZoneSchemaV2 =
        requireNotNull(schemaCatalog.zones.firstOrNull { it.id == zoneId }) {
            "Unknown zone id '$zoneId'."
        }

    private fun resolveStartingTalents(
        content: GameContent,
        profession: ProfessionSchemaV2,
    ) = resolveStartingTalentIds(content, profession).map { talentId ->
        requireNotNull(content.talents.firstOrNull { it.id == talentId }) {
            "Profession '${profession.id}' references unknown starter talent '$talentId'."
        }
    }

    private fun resolveStartingTalentIds(
        content: GameContent,
        profession: ProfessionSchemaV2,
    ): List<String> {
        if (profession.startingTalents.isNotEmpty()) {
            return profession.startingTalents.distinct()
        }
        val treeIds = profession.talentTrees.toSet()
        val treeNodeIds =
            content.schemaCatalog.talentTrees
                .filter { tree -> tree.id in treeIds }
                .flatMap { tree -> tree.nodes }
        if (treeNodeIds.isNotEmpty()) {
            return treeNodeIds.distinct()
        }
        return content.schemaCatalog.talents
            .filter { talent -> talent.treeId in treeIds }
            .map { talent -> talent.id }
            .distinct()
    }

    private fun resolveStarterItems(
        content: GameContent,
        profession: ProfessionSchemaV2,
    ): List<ItemBaseDef> =
        profession.startingKit.map { itemId ->
            requireNotNull(content.itemBundle.baseItems.firstOrNull { item -> item.id == itemId }) {
                "Profession '${profession.id}' references unknown starter item '$itemId'."
            }
        }

    private fun installStarterKit(
        world: World,
        playerId: com.ktome.core.ecs.EntityId,
        starterItems: List<ItemBaseDef>,
    ) {
        if (starterItems.isEmpty()) {
            return
        }
        val itemFactory = ItemFactory()
        val inventoryManager = InventoryManager()
        val inventory = requireNotNull(world.get<Inventory>(playerId)) { "Missing Inventory for $playerId" }
        val equippedSlots = linkedSetOf<com.ktome.core.item.EquipSlot>()

        starterItems.forEach { starterItem ->
            inventory.itemIds += itemFactory.createCarriedItem(world, starterItem.toStarterItem())
        }

        inventory.itemIds.forEachIndexed { index, itemId ->
            val slot = world.get<ItemInstance>(itemId)?.slot ?: return@forEachIndexed
            if (slot !in equippedSlots && inventoryManager.equip(world, playerId, index).success) {
                equippedSlots += slot
            }
        }
    }

    private fun resolveZoneMonsterCatalog(
        content: GameContent,
        zone: ZoneSchemaV2,
        floor: Int,
    ): List<MonsterTemplate> {
        val catalogById = content.monsterCatalog.associateBy(MonsterTemplate::id)
        val allowedIds =
            linkedSetOf<String>().apply {
                addAll(zone.monsterPools)
                if (floor > 1) {
                    addAll(zone.elitePools)
                }
            }
        val scopedCatalog =
            if (allowedIds.isEmpty()) {
                content.monsterCatalog
            } else {
                allowedIds.map { monsterId ->
                    requireNotNull(catalogById[monsterId]) {
                        "Zone '${zone.id}' references unknown monster '$monsterId'."
                    }
                }
            }
        return scopedCatalog.filter { monster -> floor in monster.spawnFloors }.ifEmpty { scopedCatalog }
    }

    private fun floorSeed(
        seed: Long,
        floor: Int,
        salt: Int,
    ): Long = seed xor (floor.toLong() shl 32) xor salt.toLong()

    private fun validateNewSessionConfig(
        config: FoundationGameConfig,
        schemaCatalog: SchemaCatalog,
    ) {
        require(schemaCatalog.zones.any { it.id == config.zoneId }) {
            "Unknown zone id '${config.zoneId}'. Update FoundationGameConfig to use a formal ZoneSpec id."
        }
        require(schemaCatalog.professions.any { it.id == config.playerProfessionId }) {
            "Unknown profession id '${config.playerProfessionId}'. Update FoundationGameConfig to use a formal ProfessionDef id."
        }
    }

    private fun validateLoadedSessionConfig(
        config: FoundationGameConfig,
        schemaCatalog: SchemaCatalog,
    ) {
        if (schemaCatalog.zones.none { it.id == config.zoneId }) {
            throw InvalidSaveException("Save references unknown zone id '${config.zoneId}'.")
        }
        if (schemaCatalog.professions.none { it.id == config.playerProfessionId }) {
            throw InvalidSaveException("Save references unknown profession id '${config.playerProfessionId}'.")
        }
    }

    private fun resolveAiType(template: MonsterTemplate): AIType =
        when (template.aiProfileId) {
            "ai.kite.basic" -> AIType.KITE
            "ai.patrol.basic" -> AIType.PATROL
            "ai.chase.basic", "ai.boss.dungeon_lord" -> AIType.CHASE
            else -> template.ai
        }

    private fun reserveEntityRange(
        world: World,
        floor: Int,
    ) {
        val reservation = com.ktome.core.ecs.EntityId(floor * 1_000)
        world.createEntity(reservation)
        world.destroyEntity(reservation)
    }

    private fun com.ktome.game.data.schema.SchemaStats.toRuntimeStats() =
        com.ktome.core.ecs.Stats(
            str = str,
            dex = dex,
            con = con,
            wil = wil,
        )

    private fun ItemBaseDef.toStarterItem() =
        ItemInstance(
            baseId = id,
            name = name,
            type = type,
            slot = slot,
            glyph = glyph,
            colorHex = colorHex,
            stats = baseStats.copy(),
            effect = effect,
            magnitude = magnitude,
        )

    private fun defaultSaveDir(): Path = Path.of(System.getProperty("user.home"), ".ktome")
}
