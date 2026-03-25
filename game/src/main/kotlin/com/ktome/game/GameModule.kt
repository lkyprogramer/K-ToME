package com.ktome.game

import com.ktome.core.dungeon.DungeonManager
import com.ktome.core.dungeon.FloorState
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.get
import com.ktome.core.fov.Shadowcasting
import com.ktome.core.map.BspConfig
import com.ktome.core.map.BspGenerator
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.InvalidSaveException
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.save.PointSnapshot
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.ai.AIActionType
import com.ktome.core.ai.AICondition
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.SchemaCombatProfile
import com.ktome.game.i18n.GameLocale
import com.ktome.game.factory.BossFactory
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate
import com.ktome.game.telegraph.TelegraphRegistry
import com.ktome.game.telegraph.ThreatProfileRegistry
import com.ktome.core.item.ItemGenerator
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.Inventory
import com.ktome.core.item.InventoryManager
import com.ktome.core.profession.ProfessionTier
import com.ktome.core.profession.ReleaseUnlockCondition
import com.ktome.core.profile.AdvancedClassUnlockRule
import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.profile.ClassAvailabilityResolver
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.core.profile.ClassUnlockState
import com.ktome.core.profile.ProfileData
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.race.RaceDef
import java.nio.file.Path
import kotlin.random.Random
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.ZoneSchemaV2

object GameModule {
    private const val DEFAULT_ROUTE_VISIBILITY_RADIUS = 8

    fun availableProfessionIds(
        locale: GameLocale = GameLocale.DEFAULT,
    ): List<String> = DataLoader(locale).loadSchemaCatalog().professions.map(ProfessionSchemaV2::id)

    fun professionSelections(
        locale: GameLocale = GameLocale.DEFAULT,
        profile: ProfileData = ProfileData(),
        context: AvailabilityContext = AvailabilityContext.PLAYER_CREATION,
    ): List<ProfessionSelectionOption> =
        DataLoader(locale)
            .loadSchemaCatalog()
            .professions
            .map { profession ->
                val unlockState = effectiveUnlockState(profession, profile)
                ProfessionSelectionOption(
                    id = profession.id,
                    tier = profession.tier,
                    unlockState = unlockState,
                    playabilityState = ClassAvailabilityResolver.resolve(unlockState = unlockState, context = context),
                )
            }

    fun availableRaceIds(
        locale: GameLocale = GameLocale.DEFAULT,
    ): List<String> = DataLoader(locale).loadSchemaCatalog().races.map(RaceDef::id)

    fun advancedClassUnlockRules(
        locale: GameLocale = GameLocale.DEFAULT,
    ): List<AdvancedClassUnlockRule> =
        DataLoader(locale)
            .loadSchemaCatalog()
            .professions
            .mapNotNull { profession ->
                when (val condition = profession.releaseUnlockCondition) {
                    is ReleaseUnlockCondition.RequireProfessionCleared ->
                        AdvancedClassUnlockRule(
                            classId = profession.id,
                            requiredProfessionId = condition.professionId,
                        )

                    null -> null
                }
            }

    fun newFoundationSession(
        config: FoundationGameConfig = FoundationGameConfig(),
        saveManager: SaveManager = SaveManager(defaultSaveDir()),
        locale: GameLocale = GameLocale.DEFAULT,
        profile: ProfileData = ProfileData(),
        availabilityContext: AvailabilityContext = AvailabilityContext.PLAYER_CREATION,
    ): FoundationGameSession {
        val content = loadContent(locale)
        validateNewSessionConfig(
            config = config,
            schemaCatalog = content.schemaCatalog,
            profile = profile,
            availabilityContext = availabilityContext,
        )
        val profession = resolveProfession(content.schemaCatalog, config.playerProfessionId)
        val race = resolveRace(content.schemaCatalog, config.playerRaceId)
        val playerSnapshot = createInitialPlayerSnapshot(content, profession, race, Point.ZERO)
        val zoneRuntime = buildZoneRuntime(content, config)
        val sessionConfig = zoneRuntime.config
        val dungeonManager = zoneRuntime.dungeonManager
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
            initialMessageLog = zoneRuntime.initialMessages,
            zoneRuntimeFactory = { nextConfig -> buildZoneRuntime(content, nextConfig) },
        )
    }

    fun loadFoundationSession(
        saveManager: SaveManager,
        locale: GameLocale = GameLocale.DEFAULT,
    ): FoundationGameSession? {
        val snapshot = saveManager.load() ?: return null
        val loader = DataLoader(locale)
        val restored = SessionSnapshotMapper.fromSaveSnapshot(snapshot)
        val content = buildContent(loader)
        val schemaCatalog = content.schemaCatalog
        validateLoadedSessionConfig(restored.config, schemaCatalog)
        val profession = resolveProfession(schemaCatalog, restored.config.playerProfessionId)
        resolveRace(schemaCatalog, restored.config.playerRaceId)
        validateLoadedPlayerResourceContract(restored.player, profession)
        val zone = resolveZone(schemaCatalog, restored.config.zoneId)
        val sessionConfig =
            restored.config.copy(
                width = zone.mapSize.width,
                height = zone.mapSize.height,
                maxFloor = zone.floorCount,
            )
        validateLoadedFloorContracts(
            floors = restored.floors,
            content = content,
            zone = zone,
            maxFloor = sessionConfig.maxFloor,
        )
        val dungeonManager =
            DungeonManager(
                maxFloor = sessionConfig.maxFloor,
                startFloor = restored.currentFloor,
                floorLoader = { floor ->
                    restored.floors.firstOrNull { it.floor == floor }
                        ?: generateFloor(content, sessionConfig, zone, floor)
                },
            )
        restored.floors.forEach { floorState ->
            dungeonManager.putState(floorState)
        }
        return FoundationGameSession(
            config = sessionConfig,
            content = content,
            saveManager = saveManager,
            dungeonManager = dungeonManager,
            playerSnapshot = restored.player,
            initialMessageLog = listOf(RenderLogEventSnapshot(RenderTextTokenSnapshot("log.session.loaded"))),
            turnCount = restored.turnCount,
            combatRandomSource =
                restored.combatRandomState?.let(SplitMix64RandomSource::fromState)
                    ?: FoundationGameSession.defaultCombatRandomSource(sessionConfig, restored.turnCount),
            sessionRandom =
                restored.sessionRandomState?.let(SplitMix64RandomSource::fromState)
                    ?: FoundationGameSession.defaultSessionRandomSource(sessionConfig, restored.turnCount),
            restoredPendingActionIds = restored.pendingActionIds,
            restoredActiveTurnActorId = restored.activeTurnActorId,
            zoneRuntimeFactory = { nextConfig -> buildZoneRuntime(content, nextConfig) },
        )
    }

    private fun buildZoneRuntime(
        content: GameContent,
        config: FoundationGameConfig,
    ): ZoneRuntimeBundle {
        val zone = resolveZone(content.schemaCatalog, config.zoneId)
        require(config.floor in 1..zone.floorCount) {
            "Start floor ${config.floor} is outside zone '${zone.id}' range 1..${zone.floorCount}."
        }
        val resolvedConfig =
            config.copy(
                width = zone.mapSize.width,
                height = zone.mapSize.height,
                maxFloor = zone.floorCount,
            )
        val dungeonManager =
            DungeonManager(
                maxFloor = resolvedConfig.maxFloor,
                startFloor = resolvedConfig.floor,
                floorLoader = { floor -> generateFloor(content, resolvedConfig, zone, floor) },
            )
        return ZoneRuntimeBundle(
            config = resolvedConfig,
            dungeonManager = dungeonManager,
            initialMessages = initialMessagesForZone(zone, content.schemaCatalog),
        )
    }

    private fun loadContent(locale: GameLocale): GameContent {
        val loader = DataLoader(locale)
        return buildContent(loader)
    }

    private fun buildContent(loader: DataLoader): GameContent {
        val schemaCatalog = loader.loadSchemaCatalog()
        val talents = loader.loadTalentDefinitions()
        val statusCatalog = loader.loadStatusCatalog()
        return GameContent(
            talents = talents,
            statuses = schemaCatalog.statuses,
            statusCatalog = statusCatalog,
            talentRegistry = com.ktome.core.talent.TalentRegistry().apply { registerAll(talents) },
            races = schemaCatalog.races,
            inscriptions = schemaCatalog.inscriptions,
            monsterCatalog = loader.loadMonsterCatalog().monsters,
            itemBundle = loader.loadItemBundle(),
            bossDefinitions = loader.loadBossDefinitions(),
            schemaCatalog = schemaCatalog,
            localizer = loader.localizer,
            telegraphRegistry = TelegraphRegistry(schemaCatalog.telegraphSpecs.associateBy { spec -> spec.id }),
            threatProfileRegistry = ThreatProfileRegistry(schemaCatalog.threatProfiles.associateBy { profile -> profile.id }),
        ).also(::validateAiProfileContracts)
    }

    private fun initialMessagesForZone(
        zone: ZoneSchemaV2,
        schemaCatalog: SchemaCatalog,
    ): List<RenderLogEventSnapshot> =
        buildList {
            add(RenderLogEventSnapshot(RenderTextTokenSnapshot("log.session.enter_dungeon")))
            add(
                RenderLogEventSnapshot(
                    RenderTextTokenSnapshot(
                        "log.zone.enter",
                        arguments =
                            listOf(
                                RenderTextArgumentSnapshot(name = "zone", valueKey = zone.nameKey),
                                RenderTextArgumentSnapshot(name = "desc", valueKey = zone.descKey),
                            ),
                    ),
                ),
            )
            val objective = schemaCatalog.objectiveSets.firstOrNull { objectiveSet -> objectiveSet.id == zone.objectiveSetId }
            if (objective != null) {
                add(
                    RenderLogEventSnapshot(
                        RenderTextTokenSnapshot(
                            "log.objective.activate",
                            arguments = listOf(RenderTextArgumentSnapshot(name = "objective", valueKey = objective.nameKey)),
                        ),
                    ),
                )
            }
        }

    private fun createInitialPlayerSnapshot(
        content: GameContent,
        profession: ProfessionSchemaV2,
        race: RaceDef,
        position: Point,
    ): com.ktome.core.save.PlayerSnapshot {
        val world = World()
        val playerId =
            EntityFactory().createPlayer(
                world = world,
                position = position,
                talents = resolveStartingTalents(content, profession, race),
                playerName = content.localizer.text("actor.player.name"),
                stats = profession.baseStats.toRuntimeStats(race),
                combatProfile = profession.combatProfile.toRuntimeCombatProfile(race),
            )
        installStarterKit(world, playerId, resolveStarterItems(content, profession))
        StatsCalculator.recalculateAndStore(world, playerId)
        PlayerResourceService.ensureInitialized(world, playerId, profession)
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
        reserveEntityRange(world, config.routeIndex, floor)
        val factory = EntityFactory()
        val itemFactory = ItemFactory()
        val bossFactory = BossFactory(factory)
        val itemGenerator = ItemGenerator(content.itemBundle, com.ktome.core.random.RandomSource.from(Random(floorSeed(config.seed, floor, 0x91F3))))
        val monsterRandom = Random(floorSeed(config.seed, floor, 0x63AF))
        val bossDefinition = if (floor == config.maxFloor) resolveBossDefinition(content, zone) else null
        val stairsUp = if (floor > 1) map.playerStart else null
        val stairsDown =
            when {
                floor < config.maxFloor -> chooseDownstairs(map, stairsUp)
                bossDefinition == null -> chooseDownstairs(map, stairsUp)
                else -> null
            }
        val occupiedPoints =
            linkedSetOf<Point>().apply {
                stairsUp?.let(::add)
                stairsDown?.let(::add)
            }

        stairsUp?.let { point -> createStair(world, point, StairDirection.UP, content.localizer) }
        stairsDown?.let { point -> createStair(world, point, StairDirection.DOWN, content.localizer) }

        val bossPosition =
            if (bossDefinition != null) {
                chooseBossPosition(map, occupiedPoints)
            } else {
                null
            }

        if (bossDefinition != null) {
            val resolvedBossPosition = requireNotNull(bossPosition)
            bossFactory.createBoss(world, bossDefinition, resolvedBossPosition)
            occupiedPoints += resolvedBossPosition
        } else {
            spawnMonsters(
                zone = zone,
                factory = factory,
                world = world,
                map = map,
                floor = floor,
                catalog = resolveZoneMonsterCatalog(content, zone, floor),
                occupiedPoints = occupiedPoints,
                random = monsterRandom,
                desiredCount = zoneMonsterSpawnCount(zone, floor, map.rooms.size),
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
        createObjectiveInteractables(
            world = world,
            map = map,
            floor = floor,
            content = content,
            occupiedPoints = occupiedPoints,
            objectiveSetId = zone.objectiveSetId,
            stairsUp = stairsUp,
            stairsDown = stairsDown,
            bossPosition = bossPosition,
        )

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
        zone: ZoneSchemaV2,
        factory: EntityFactory,
        world: World,
        map: com.ktome.core.map.GameMap,
        floor: Int,
        catalog: List<MonsterTemplate>,
        occupiedPoints: MutableSet<Point>,
        random: Random,
        desiredCount: Int,
    ) {
        val availableTemplates = catalog.filter { floor in it.spawnFloors }.ifEmpty { catalog }
        val roomCandidates = map.rooms.drop(1)
        if (availableTemplates.isEmpty() || roomCandidates.isEmpty()) {
            return
        }
        val maxSpawnSlots = roomCandidates.size + if (allowsRoomPack(zone, floor)) 1 else 0
        val spawnCount = desiredCount.coerceIn(1, maxSpawnSlots)
        val selectedTemplates = selectMonsterTemplates(zone, floor, availableTemplates, spawnCount, random)
        val plannedRooms = plannedMonsterRooms(zone, floor, roomCandidates, selectedTemplates.size, random)

        plannedRooms
            .zip(selectedTemplates)
            .groupBy({ (room, _) -> room }, { (_, template) -> template })
            .forEach { (room, templates) ->
                val spawnPoints = findSpawnPoints(room, map, occupiedPoints, templates.size)
                templates.zip(spawnPoints).forEach { (template, spawnPoint) ->
                    factory.createMonster(
                        world = world,
                        template = template,
                        position = spawnPoint,
                        patrolRoute = if (resolveAiType(template) == AIType.PATROL) buildPatrolRoute(room) else null,
                    )
                    occupiedPoints += spawnPoint
                }
            }
    }

    private fun zoneMonsterSpawnCount(
        zone: ZoneSchemaV2,
        floor: Int,
        roomCount: Int,
    ): Int {
        return when (zone.id) {
            "shattered_outpost" -> if (floor == 1) 3 else 4
            "greenwood_fringe" -> if (floor == 1) 4 else 5
            else -> roomCount.coerceAtMost(4).coerceAtLeast(3)
        }
    }

    private fun selectMonsterTemplates(
        zone: ZoneSchemaV2,
        floor: Int,
        availableTemplates: List<MonsterTemplate>,
        desiredCount: Int,
        random: Random,
    ): List<MonsterTemplate> {
        val uniqueCatalog = availableTemplates.distinctBy(MonsterTemplate::id)
        val selected = encounterAnchorTemplates(zone, floor, uniqueCatalog, random).take(desiredCount).toMutableList()

        while (selected.size < desiredCount) {
            val weightedUniqueCandidate =
                selectWeightedTemplate(
                    candidates = uniqueCatalog,
                    zone = zone,
                    floor = floor,
                    selected = selected,
                    random = random,
                    uniqueOnly = true,
                )
            if (weightedUniqueCandidate != null) {
                selected += weightedUniqueCandidate
                continue
            }
            val weightedDuplicateCandidate =
                selectWeightedTemplate(
                    candidates = availableTemplates,
                    zone = zone,
                    floor = floor,
                    selected = selected,
                    random = random,
                    uniqueOnly = false,
                ) ?: break
            selected += weightedDuplicateCandidate
        }
        selected.shuffle(random)
        return selected
    }

    private fun encounterAnchorTemplates(
        zone: ZoneSchemaV2,
        floor: Int,
        availableTemplates: List<MonsterTemplate>,
        random: Random,
    ): List<MonsterTemplate> {
        val selected = mutableListOf<MonsterTemplate>()
        encounterBehaviorOrder(zone, floor).forEach { behavior ->
            selectWeightedTemplate(
                candidates = availableTemplates.filter { template -> resolveAiType(template) == behavior },
                zone = zone,
                floor = floor,
                selected = selected,
                random = random,
                uniqueOnly = true,
            )?.let(selected::add)
        }
        return selected
    }

    private fun selectWeightedTemplate(
        candidates: List<MonsterTemplate>,
        zone: ZoneSchemaV2,
        floor: Int,
        selected: List<MonsterTemplate>,
        random: Random,
        uniqueOnly: Boolean,
    ): MonsterTemplate? {
        val selectedIds = if (uniqueOnly) selected.mapTo(linkedSetOf(), MonsterTemplate::id) else emptySet()
        val weightedPool =
            candidates
                .asSequence()
                .filter { template -> !uniqueOnly || template.id !in selectedIds }
                .filter { template -> canSelectEncounterTemplate(zone, floor, selected, template) }
                .distinctBy(MonsterTemplate::id)
                .toList()
        if (weightedPool.isEmpty()) {
            return null
        }
        return chooseWeightedTemplate(weightedPool, random)
    }

    private fun encounterBehaviorOrder(
        zone: ZoneSchemaV2,
        floor: Int,
    ): List<AIType> =
        when {
            zone.id == "shattered_outpost" && floor == 1 -> listOf(AIType.CHASE, AIType.PATROL)
            else -> listOf(AIType.CHASE, AIType.KITE, AIType.PATROL)
        }

    private fun canSelectEncounterTemplate(
        zone: ZoneSchemaV2,
        floor: Int,
        selected: List<MonsterTemplate>,
        candidate: MonsterTemplate,
    ): Boolean {
        if (resolveAiType(candidate) != AIType.KITE) {
            return true
        }
        return selected.count { template -> resolveAiType(template) == AIType.KITE } < maxKiteSpawnCount(zone, floor)
    }

    private fun maxKiteSpawnCount(
        zone: ZoneSchemaV2,
        floor: Int,
    ): Int =
        when {
            zone.id == "shattered_outpost" && floor == 1 -> 0
            zone.id == "greenwood_fringe" && floor == 1 -> 1
            else -> Int.MAX_VALUE
        }

    private fun allowsRoomPack(
        zone: ZoneSchemaV2,
        floor: Int,
    ): Boolean =
        when (zone.id) {
            "shattered_outpost" -> floor == 1
            "greenwood_fringe" -> floor <= 2
            else -> false
        }

    private fun plannedMonsterRooms(
        zone: ZoneSchemaV2,
        floor: Int,
        roomCandidates: List<Room>,
        spawnCount: Int,
        random: Random,
    ): List<Room> {
        if (spawnCount <= 0 || roomCandidates.isEmpty()) {
            return emptyList()
        }
        val shuffledRooms = roomCandidates.shuffled(random)
        if (!allowsRoomPack(zone, floor) || spawnCount < 3) {
            return shuffledRooms.take(spawnCount)
        }

        val packRoom = shuffledRooms.firstOrNull { room -> room.width >= 6 && room.height >= 6 } ?: return shuffledRooms.take(spawnCount)
        val planned = mutableListOf(packRoom, packRoom)
        val remainingRooms = shuffledRooms.filterNot { room -> room == packRoom }
        planned += remainingRooms.take((spawnCount - planned.size).coerceAtLeast(0))
        while (planned.size < spawnCount) {
            planned += shuffledRooms[(planned.size - 1).mod(shuffledRooms.size)]
        }
        return planned
    }

    private fun chooseWeightedTemplate(
        templates: List<MonsterTemplate>,
        random: Random,
    ): MonsterTemplate {
        val totalWeight = templates.sumOf(MonsterTemplate::spawnWeight)
        require(totalWeight > 0) { "Monster selection requires a positive spawn weight." }
        var roll = random.nextInt(totalWeight)
        templates.forEach { template ->
            roll -= template.spawnWeight
            if (roll < 0) {
                return template
            }
        }
        return templates.last()
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
            val spawnPoint = findSpawnPoints(room, map, occupiedPoints).first()
            itemFactory.createGroundItem(world, itemGenerator.generate(floor), spawnPoint)
            occupiedPoints += spawnPoint
        }
    }

    private fun findSpawnPoints(
        room: Room,
        map: com.ktome.core.map.GameMap,
        occupiedPoints: Set<Point>,
        count: Int = 1,
    ): List<Point> {
        val reserved = occupiedPoints.toMutableSet()
        val preferred = mutableListOf<Point>()
        preferred += room.center
        preferred += listOf(
            Point(room.center.x + 1, room.center.y),
            Point(room.center.x - 1, room.center.y),
            Point(room.center.x, room.center.y + 1),
            Point(room.center.x, room.center.y - 1),
            Point(room.left + 1, room.top + 1),
            Point(room.right - 1, room.bottom - 1),
            Point(room.left + 1, room.bottom - 1),
            Point(room.right - 1, room.top + 1),
        )
        preferred +=
            (room.left + 1 until room.right).flatMap { x ->
                (room.top + 1 until room.bottom).map { y -> Point(x, y) }
            }

        val result = mutableListOf<Point>()
        preferred.distinct().forEach { point ->
            if (result.size >= count) {
                return@forEach
            }
            if (!room.contains(point) || map[point].blocksMovement || point in reserved) {
                return@forEach
            }
            result += point
            reserved += point
        }
        return result.ifEmpty { listOf(room.center) }.take(count)
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

    private fun createObjectiveInteractables(
        world: World,
        map: com.ktome.core.map.GameMap,
        floor: Int,
        content: GameContent,
        occupiedPoints: MutableSet<Point>,
        objectiveSetId: String?,
        stairsUp: Point?,
        stairsDown: Point?,
        bossPosition: Point?,
    ) {
        val objective =
            content.schemaCatalog.objectiveSets.firstOrNull { objectiveSet ->
                objectiveSet.id == objectiveSetId
            } ?: return
        plannedInteractables(
            objective = objective,
            floor = floor,
            map = map,
            occupiedPoints = occupiedPoints,
            stairsUp = stairsUp,
            stairsDown = stairsDown,
            bossPosition = bossPosition,
        )
            .filter { (interactableId, _) -> interactableId in objective.interactables }
            .forEach { (interactableId, point) ->
                createInteractable(world, interactableId, point, content)
                occupiedPoints += point
            }
    }

    private fun plannedInteractables(
        objective: com.ktome.game.data.schema.ObjectiveSetSchemaV2,
        floor: Int,
        map: com.ktome.core.map.GameMap,
        occupiedPoints: Set<Point>,
        stairsUp: Point?,
        stairsDown: Point?,
        bossPosition: Point?,
    ): List<Pair<String, Point>> {
        val reserved = occupiedPoints.toMutableSet()
        return objective.placements
            .asSequence()
            .filter { placement -> placement.floor == floor }
            .map { placement ->
                placement.interactableId to preferredInteractablePoint(
                    map = map,
                    placement = placement,
                    stairsUp = stairsUp,
                    stairsDown = stairsDown,
                    bossPosition = bossPosition,
                )
            }
            .mapNotNull { (interactableId, preferredPoint) ->
            findObjectiveInteractablePoint(map, preferredPoint, reserved)?.also { point ->
                reserved += point
            }?.let { point -> interactableId to point }
        }.toList()
    }

    private fun preferredInteractablePoint(
        map: com.ktome.core.map.GameMap,
        placement: com.ktome.game.data.schema.ObjectiveInteractablePlacementSchemaV2,
        stairsUp: Point?,
        stairsDown: Point?,
        bossPosition: Point?,
    ): Point {
        val routeSeed = stairsUp ?: map.playerStart
        val anchor =
            when (placement.anchor.lowercase()) {
                "player_start" -> map.playerStart
                "stairs_up" -> stairsUp ?: map.playerStart
                "stairs_down" -> stairsDown ?: fallbackRoomCenter(map)
                "room_center" -> routeVisibleAnchor(map, routeSeed, fallbackRoomCenter(map))
                "boss_entry" -> routeVisibleAnchor(map, routeSeed, bossEntryPoint(map, bossPosition))
                else -> error("Unsupported interactable placement anchor '${placement.anchor}'.")
            }
        return anchor + Point(placement.offset.x, placement.offset.y)
    }

    private fun fallbackRoomCenter(map: com.ktome.core.map.GameMap): Point =
        map.rooms.drop(1)
            .ifEmpty { map.rooms }
            .let { rooms -> rooms[rooms.size / 2].center }

    private fun bossEntryPoint(
        map: com.ktome.core.map.GameMap,
        bossPosition: Point?,
    ): Point {
        val center = bossPosition ?: return fallbackRoomCenter(map)
        return Point.ALL_DIRECTIONS
            .asSequence()
            .map { delta -> center + delta }
            .filter { point ->
                map.isInBounds(point.x, point.y) &&
                    !map[point].blocksMovement
            }
            .minWithOrNull(
                compareBy<Point> { point -> point.chebyshevDistanceTo(map.playerStart) }
                    .thenBy(Point::y)
                    .thenBy(Point::x),
            ) ?: center
    }

    private fun routeVisibleAnchor(
        map: com.ktome.core.map.GameMap,
        routeSeed: Point,
        preferred: Point,
    ): Point {
        val visible =
            Shadowcasting.computeVisible(
                map = map,
                origin = routeSeed,
                radius = DEFAULT_ROUTE_VISIBILITY_RADIUS,
            )
        return visible
            .asSequence()
            .filter { point ->
                map.isInBounds(point.x, point.y) &&
                    !map[point].blocksMovement &&
                    point != routeSeed
            }
            .minWithOrNull(
                compareBy<Point> { point -> point.chebyshevDistanceTo(preferred) }
                    .thenBy { point -> point.chebyshevDistanceTo(routeSeed) }
                    .thenBy(Point::y)
                    .thenBy(Point::x),
            ) ?: preferred
    }

    private fun findObjectiveInteractablePoint(
        map: com.ktome.core.map.GameMap,
        preferred: Point,
        occupiedPoints: Set<Point>,
    ): Point? {
        val candidates =
            sequenceOf(preferred, map.playerStart)
                .plus(Point.ALL_DIRECTIONS.asSequence().map { delta -> preferred + delta })
                .plus(Point.ALL_DIRECTIONS.asSequence().map { delta -> map.playerStart + delta })
                .distinct()
        return candidates.firstOrNull { point ->
            map.isInBounds(point.x, point.y) &&
                !map[point].blocksMovement &&
                point !in occupiedPoints &&
                point != map.playerStart
        }
    }

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

    private fun createInteractable(
        world: World,
        interactableId: String,
        position: Point,
        content: GameContent,
    ) {
        val schema =
            requireNotNull(content.schemaCatalog.interactables.firstOrNull { interactable -> interactable.id == interactableId }) {
                "Unknown interactable '$interactableId'."
            }
        val entityId = world.createEntity()
        world.add(entityId, Position(position.x, position.y))
        world.add(entityId, Interactable(interactableId))
        world.add(
            entityId,
            Glyph(
                when (interactableId) {
                    "armory_gate" -> '+'
                    "alarm_bonfire", "warden_beacon", "slag_valve", "shadow_brazier" -> '^'
                    "mine_furnace" -> '#'
                    "ritual_altar" -> '='
                    else -> '&'
                },
            ),
        )
        world.add(
            entityId,
            DisplayColor(
                when (interactableId) {
                    "armory_gate" -> "#C7B48A"
                    "alarm_bonfire", "warden_beacon" -> "#FF8A3D"
                    "slag_valve" -> "#D66A3D"
                    "shadow_brazier" -> "#8A73C9"
                    "mine_furnace" -> "#C16B3C"
                    "ritual_altar" -> "#6E6786"
                    else -> "#D6C977"
                },
            ),
        )
        world.add(entityId, Name(content.localizer.text(schema.nameKey)))
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

    private fun resolveBossDefinition(
        content: GameContent,
        zone: ZoneSchemaV2,
    ): BossDefinition? {
        val bossEncounterId = zone.bossEncounterId ?: return null
        return requireNotNull(content.bossDefinitions[bossEncounterId]) {
            "Zone '${zone.id}' references unknown boss encounter '$bossEncounterId'."
        }
    }

    private fun resolveStartingTalents(
        content: GameContent,
        profession: ProfessionSchemaV2,
        race: RaceDef? = null,
    ) = resolveStartingTalentIds(content, profession, race).map { talentId ->
        requireNotNull(content.talents.firstOrNull { it.id == talentId }) {
            "Profession '${profession.id}' references unknown starter talent '$talentId'."
        }
    }

    private fun resolveStartingTalentIds(
        content: GameContent,
        profession: ProfessionSchemaV2,
        race: RaceDef? = null,
    ): List<String> = TalentProgression.unlockedTalentIds(content.schemaCatalog, profession, level = 1, race = race)

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
        profile: ProfileData,
        availabilityContext: AvailabilityContext,
    ) {
        routeValidationError(config, schemaCatalog)?.let { message ->
            throw IllegalArgumentException(message)
        }
        require(schemaCatalog.zones.any { it.id == config.zoneId }) {
            "Unknown zone id '${config.zoneId}'. Update FoundationGameConfig to use a formal ZoneSpec id."
        }
        require(schemaCatalog.professions.any { it.id == config.playerProfessionId }) {
            "Unknown profession id '${config.playerProfessionId}'. Update FoundationGameConfig to use a formal ProfessionDef id."
        }
        require(schemaCatalog.races.any { it.id == config.playerRaceId }) {
            "Unknown race id '${config.playerRaceId}'. Update FoundationGameConfig to use a formal RaceDef id."
        }
        val profession = resolveProfession(schemaCatalog, config.playerProfessionId)
        requirePlayableSelection(
            selectionType = "Profession",
            selectionId = profession.id,
            resolvedState =
                ClassAvailabilityResolver.resolve(
                    unlockState = effectiveUnlockState(profession, profile),
                    context = availabilityContext,
                ),
            availabilityContext = availabilityContext,
        )
        val race = resolveRace(schemaCatalog, config.playerRaceId)
        requirePlayableSelection(
            selectionType = "Race",
            selectionId = race.id,
            resolvedState =
                ClassAvailabilityResolver.resolve(
                    unlockState = race.initialUnlockState,
                    context = availabilityContext,
                ),
            availabilityContext = availabilityContext,
        )
    }

    private fun validateLoadedSessionConfig(
        config: FoundationGameConfig,
        schemaCatalog: SchemaCatalog,
    ) {
        routeValidationError(config, schemaCatalog)?.let(::InvalidSaveException)?.let { throw it }
        if (schemaCatalog.zones.none { it.id == config.zoneId }) {
            throw InvalidSaveException("Save references unknown zone id '${config.zoneId}'.")
        }
        if (schemaCatalog.professions.none { it.id == config.playerProfessionId }) {
            throw InvalidSaveException("Save references unknown profession id '${config.playerProfessionId}'.")
        }
        if (schemaCatalog.races.none { it.id == config.playerRaceId }) {
            throw InvalidSaveException("Save references unknown race id '${config.playerRaceId}'.")
        }
        val zone = resolveZone(schemaCatalog, config.zoneId)
        if (config.width != zone.mapSize.width || config.height != zone.mapSize.height) {
            throw InvalidSaveException(
                "Save map size ${config.width}x${config.height} does not match zone '${zone.id}' schema ${zone.mapSize.width}x${zone.mapSize.height}.",
            )
        }
        if (config.maxFloor != zone.floorCount) {
            throw InvalidSaveException(
                "Save maxFloor ${config.maxFloor} does not match zone '${zone.id}' floorCount ${zone.floorCount}.",
            )
        }
        if (config.floor !in 1..zone.floorCount) {
            throw InvalidSaveException(
                "Save current floor ${config.floor} is outside zone '${zone.id}' range 1..${zone.floorCount}.",
            )
        }
    }

    private fun validateLoadedPlayerResourceContract(
        player: PlayerSnapshot,
        profession: ProfessionSchemaV2,
    ) {
        val availablePoolTypes = player.entity.resourcePools.map { pool -> pool.type }.toSet()
        val missingPoolTypes = requiredPlayerResourcePoolTypes(profession).filterNot(availablePoolTypes::contains)
        if (missingPoolTypes.isNotEmpty()) {
            throw InvalidSaveException(
                "Save player entity is missing required resource pools $missingPoolTypes for profession '${profession.id}'.",
            )
        }
    }

    private fun requiredPlayerResourcePoolTypes(profession: ProfessionSchemaV2): Set<String> =
        linkedSetOf<String>().apply {
            add(ResourceType.STAMINA.name)
            profession.resourceProfiles.mapNotNullTo(this) { profile -> profile.resourceType?.name }
        }

    private fun resolveRace(
        schemaCatalog: SchemaCatalog,
        raceId: String,
    ): RaceDef =
        requireNotNull(schemaCatalog.races.firstOrNull { race -> race.id == raceId }) {
            "Unknown race id '$raceId'."
        }

    private fun effectiveUnlockState(
        profession: ProfessionSchemaV2,
        profile: ProfileData,
    ): ClassUnlockState =
        if (profession.id in profile.releaseUnlockedClasses) {
            ClassUnlockState.RELEASE_UNLOCKED
        } else {
            profession.initialUnlockState
        }

    private fun requirePlayableSelection(
        selectionType: String,
        selectionId: String,
        resolvedState: ClassPlayabilityState,
        availabilityContext: AvailabilityContext,
    ) {
        require(resolvedState == ClassPlayabilityState.PLAYABLE) {
            "$selectionType '$selectionId' is $resolvedState in $availabilityContext and cannot start a new session."
        }
    }

    private fun validateAiProfileContracts(content: GameContent) {
        val talentIds = content.talents.map { talent -> talent.id }.toSet()
        val profilesById = content.schemaCatalog.aiProfiles.associateBy { profile -> profile.id }
        val threatProfileIds = content.schemaCatalog.threatProfiles.map { profile -> profile.id }.toSet()
        require(profilesById.size == content.schemaCatalog.aiProfiles.size) {
            "AI profile ids must stay unique."
        }
        require(threatProfileIds.size == content.schemaCatalog.threatProfiles.size) {
            "Threat profile ids must stay unique."
        }
        content.schemaCatalog.aiProfiles.forEach { profile ->
            require(profile.perceptionRange > 0) { "AI profile '${profile.id}' must declare positive perceptionRange." }
            require(profile.actions.isNotEmpty()) { "AI profile '${profile.id}' must declare at least one action." }
            require(profile.actions.distinctBy { action -> action.id }.size == profile.actions.size) {
                "AI profile '${profile.id}' contains duplicate action ids."
            }
            profile.actions.forEach { action ->
                require(action.id.isNotBlank()) { "AI action ids must not be blank in profile '${profile.id}'." }
                if (action.type == AIActionType.USE_ABILITY) {
                    require(!action.abilityId.isNullOrBlank()) {
                        "AI action '${action.id}' in profile '${profile.id}' must declare abilityId when type=USE_ABILITY."
                    }
                    require(action.abilityId in talentIds) {
                        "AI action '${action.id}' in profile '${profile.id}' references unknown talent '${action.abilityId}'."
                    }
                } else {
                    require(action.abilityId == null) {
                        "AI action '${action.id}' in profile '${profile.id}' must not declare abilityId unless type=USE_ABILITY."
                    }
                }
                action.condition?.let { condition ->
                    require(aiConditionDepth(condition) <= 3) {
                        "AI action '${action.id}' in profile '${profile.id}' exceeds AI condition max depth 3."
                    }
                }
            }
        }
        require(content.schemaCatalog.telegraphSpecs.distinctBy { spec -> spec.id }.size == content.schemaCatalog.telegraphSpecs.size) {
            "Telegraph spec ids must stay unique."
        }
        content.schemaCatalog.telegraphSpecs.forEach { spec ->
            require(spec.previewTurns > 0) { "Telegraph spec '${spec.id}' must declare previewTurns > 0." }
            require(spec.threatProfileId in threatProfileIds) {
                "Telegraph spec '${spec.id}' references unknown threat profile '${spec.threatProfileId}'."
            }
            require(spec.counterplayTags.isNotEmpty()) {
                "Telegraph spec '${spec.id}' must declare at least one counterplay tag."
            }
        }
        validateThreatProfileBands(content)
        val monsterById = content.allMonsterTemplates().associateBy(MonsterTemplate::id)
        content.allMonsterTemplates().forEach { template ->
            val profile = requireNotNull(profilesById[template.aiProfileId]) {
                "Monster template '${template.id}' references unknown AI profile '${template.aiProfileId}'."
            }
            profile.actions
                .filter { action -> action.type == AIActionType.USE_ABILITY }
                .forEach { action ->
                    require(template.talentLevels.containsKey(action.abilityId)) {
                        "Monster template '${template.id}' uses AI action '${action.id}' for talent '${action.abilityId}', but that talent is not configured on the monster."
                    }
                }
        }
        content.schemaCatalog.bossEncounters.forEach { encounter ->
            require(monsterById.containsKey(encounter.templateId)) {
                "Boss encounter '${encounter.id}' references unknown monster '${encounter.templateId}'."
            }
            require(encounter.phases.isNotEmpty()) { "Boss encounter '${encounter.id}' must declare at least one phase." }
            val sortedByThreshold =
                encounter.phases.mapNotNull { phase -> phase.hpThreshold }
                    .zipWithNext()
                    .all { (current, next) -> current >= next }
            require(sortedByThreshold) {
                "Boss encounter '${encounter.id}' phases must stay sorted by hpThreshold descending."
            }
            encounter.phases.forEach { phase ->
                require(phase.aiProfileId in profilesById) {
                    "Boss phase '${phase.id}' in encounter '${encounter.id}' references unknown AI profile '${phase.aiProfileId}'."
                }
                phase.onEnter.forEach { event ->
                    event.telegraphSpecId?.let { telegraphId ->
                        require(content.telegraphRegistry.resolve(telegraphId) != null) {
                            "Boss phase '${phase.id}' in encounter '${encounter.id}' references unknown telegraph '$telegraphId'."
                        }
                    }
                }
                val bossProfile = requireNotNull(profilesById[phase.aiProfileId])
                bossProfile.actions
                    .filter { action -> action.type == AIActionType.USE_ABILITY }
                    .forEach { action ->
                        require(monsterById.getValue(encounter.templateId).talentLevels.containsKey(action.abilityId)) {
                            "Boss encounter '${encounter.id}' phase '${phase.id}' uses talent '${action.abilityId}' not configured on '${encounter.templateId}'."
                        }
                    }
            }
        }
    }

    private fun validateThreatProfileBands(content: GameContent) {
        content.schemaCatalog.threatProfiles
            .groupBy { profile -> "${profile.defenderArchetype}:${profile.difficultyId}" }
            .forEach { (groupId, profiles) ->
                val sorted = profiles.sortedBy { profile -> profile.levelBand.min }
                sorted.zipWithNext().forEach { (current, next) ->
                    require(current.levelBand.max < next.levelBand.min) {
                        "Threat profile level bands overlap inside group '$groupId': ${current.id} and ${next.id}."
                    }
                }
            }
    }

    private fun aiConditionDepth(condition: AICondition): Int =
        when (condition) {
            is AICondition.And -> 1 + (condition.conditions.maxOfOrNull(::aiConditionDepth) ?: 0)
            is AICondition.Or -> 1 + (condition.conditions.maxOfOrNull(::aiConditionDepth) ?: 0)
            is AICondition.Not -> 1 + aiConditionDepth(condition.condition)
            else -> 1
        }

    private fun routeValidationError(
        config: FoundationGameConfig,
        schemaCatalog: SchemaCatalog,
    ): String? {
        if (config.zoneRoute.isEmpty()) {
            return "Zone route must not be empty."
        }
        if (config.routeIndex !in config.zoneRoute.indices) {
            return "Route index ${config.routeIndex} must be within zone route indices."
        }
        if (config.zoneRoute[config.routeIndex] != config.zoneId) {
            return "Current zone '${config.zoneId}' must match zoneRoute[${config.routeIndex}]='${config.zoneRoute[config.routeIndex]}'."
        }
        val unknownZoneIds = config.zoneRoute.filterNot { zoneId -> schemaCatalog.zones.any { zone -> zone.id == zoneId } }
        if (unknownZoneIds.isNotEmpty()) {
            return "Zone route contains unknown zone ids: $unknownZoneIds."
        }
        val duplicateZoneIds =
            config.zoneRoute
                .groupingBy { zoneId -> zoneId }
                .eachCount()
                .filterValues { count -> count > 1 }
                .keys
                .toList()
        if (duplicateZoneIds.isNotEmpty()) {
            return "Zone route must not repeat zone ids: $duplicateZoneIds."
        }
        return null
    }

    private fun validateLoadedFloorContracts(
        floors: List<FloorState<FloorRuntimeState>>,
        content: GameContent,
        zone: ZoneSchemaV2,
        maxFloor: Int,
    ) {
        val finalFloor = floors.firstOrNull { floorState -> floorState.floor == maxFloor } ?: return
        val cachedBossTemplateIds =
            finalFloor.payload.entities.mapNotNull { snapshot ->
                snapshot.monsterTemplateId?.let(::normalizeLoadedMonsterTemplateId)
            }.toSet()
        val activeBossTemplateId = content.bossDefinitionForZone(zone.id)?.template?.id

        if (activeBossTemplateId == null) {
            if (finalFloor.stairsDown == null) {
                throw InvalidSaveException(
                    "Save final floor for zone '${zone.id}' is missing the routed exit stair. This save predates the ZoneSpec floor routing contract.",
                )
            }
            if (cachedBossTemplateIds.any(content.bossTemplateIds()::contains)) {
                throw InvalidSaveException(
                    "Save final floor for zone '${zone.id}' still contains a boss from the pre-routing contract. Start a new run.",
                )
            }
            return
        }

        if (finalFloor.stairsDown != null) {
            throw InvalidSaveException(
                "Save final floor for zone '${zone.id}' incorrectly contains an exit stair despite bossEncounterId '$activeBossTemplateId'.",
            )
        }
        if (activeBossTemplateId !in cachedBossTemplateIds) {
            throw InvalidSaveException(
                "Save final floor for zone '${zone.id}' is missing boss template '$activeBossTemplateId'. This save no longer matches the routed boss contract.",
            )
        }
    }

    private fun normalizeLoadedMonsterTemplateId(templateId: String): String =
        when (templateId) {
            "dungeon_lord" -> FOUNDATION_BOSS_TEMPLATE_ID
            else -> templateId
        }

    private fun resolveAiType(template: MonsterTemplate): AIType =
        when (template.aiProfileId) {
            "ai.kite.basic",
            "ai.controller.pressure",
            "ai.controller.shadow_priest",
            ->
                AIType.KITE
            "ai.patrol.basic",
            "ai.skirmisher.flank",
            "ai.elite.huntmaster",
            ->
                AIType.PATROL
            "ai.chase.basic",
            "ai.guard.basic",
            "ai.elite.forge_guard",
            "ai.elite.ashgate_warden",
            "ai.boss.bandit_captain.phase_full",
            "ai.boss.molten_giant.phase_full",
            "ai.boss.molten_giant.phase_enraged",
            "ai.boss.dungeon_lord.phase_full",
            "ai.boss.dungeon_lord.phase_enraged",
            ->
                AIType.CHASE
            else -> template.ai
        }

    private fun reserveEntityRange(
        world: World,
        routeIndex: Int,
        floor: Int,
    ) {
        val reservation = com.ktome.core.ecs.EntityId((routeIndex + 1) * 10_000 + floor * 1_000)
        world.createEntity(reservation)
        world.destroyEntity(reservation)
    }

    private fun com.ktome.game.data.schema.SchemaStats.toRuntimeStats(race: RaceDef) =
        com.ktome.core.ecs.Stats(
            str = str + race.statModifiers.str,
            dex = dex + race.statModifiers.dex,
            con = con + race.statModifiers.con,
            wil = wil + race.statModifiers.wil,
        )

    private fun SchemaCombatProfile.toRuntimeCombatProfile(race: RaceDef) =
        com.ktome.core.ecs.CombatProfile(
            baseAttack = baseAttack,
            baseDefense = baseDefense,
            baseAccuracy = baseAccuracy + race.statModifiers.accuracyDelta,
            baseEvasion = baseEvasion + race.statModifiers.evasionDelta,
            baseSpeed = baseSpeed + race.statModifiers.speedDelta,
            baseHp = baseHp + race.statModifiers.hpDelta,
            baseStamina = baseStamina,
            baseHpRegen = baseHpRegen,
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
            resourceTypeId = resourceTypeId,
            magnitude = magnitude,
            passive = passive,
        )

    private fun defaultSaveDir(): Path = Path.of(System.getProperty("user.home"), ".ktome")
}
