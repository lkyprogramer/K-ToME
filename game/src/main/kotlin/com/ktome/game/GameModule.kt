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
import com.ktome.core.economy.ShardEconomy
import com.ktome.core.economy.ShopInventoryState
import com.ktome.core.economy.ShopNode
import com.ktome.core.fov.Shadowcasting
import com.ktome.core.item.AffixSelectionContext
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.pathfinding.AStar
import com.ktome.core.mapgen.HybridTopologyMapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.phase.PackId
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
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.SchemaCombatProfile
import com.ktome.game.i18n.GameLocale
import com.ktome.game.elites.BossVariantSelectionMode
import com.ktome.game.elites.EncounterDecorationService
import com.ktome.game.elites.SpawnDecorationRequest
import com.ktome.game.factory.BossFactory
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.mapgen.SchemaMapgenContentCatalogFactory
import com.ktome.game.mapgen.SchemaZoneMapgenProfileResolver
import com.ktome.game.mapgen.SchemaZoneRewardProfileResolver
import com.ktome.game.validation.ValidationPaths
import com.ktome.game.validation.ValidationSessionRequest
import com.ktome.game.validation.ValidationSessionOptions
import com.ktome.game.validation.loadPersistedValidationSessionOptions
import com.ktome.core.mapgen.BspBackedMapgenPipeline
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate
import com.ktome.game.model.isEliteEncounterTemplate
import com.ktome.game.telegraph.TelegraphRegistry
import com.ktome.game.telegraph.ThreatProfileRegistry
import com.ktome.core.item.ItemGenerator
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.Inventory
import com.ktome.core.item.InventoryManager
import com.ktome.core.profession.ReleaseUnlockCondition
import com.ktome.core.profile.AdvancedClassUnlockRule
import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.profile.ClassAvailabilityResolver
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.core.profile.ClassUnlockState
import com.ktome.core.profile.ProfileData
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.race.RaceDef
import com.ktome.core.world.ObjectiveState
import com.ktome.core.world.WorldProgressDef
import java.nio.file.Path
import kotlin.random.Random
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.ZoneSchemaV2

object GameModule {
    private const val DEFAULT_ROUTE_VISIBILITY_RADIUS = 8

    fun playerCreationState(
        locale: GameLocale = GameLocale.DEFAULT,
        profile: ProfileData = ProfileData(),
        previousSelection: PlayerCreationSelection? = null,
        context: AvailabilityContext = AvailabilityContext.PLAYER_CREATION,
    ): PlayerCreationState {
        val schemaCatalog = DataLoader(locale).loadSchemaCatalog()
        val professionOptions =
            schemaCatalog.professions.map { profession ->
                val unlockState = effectiveUnlockState(profession, profile)
                ProfessionPlayerCreationOption(
                    id = profession.id,
                    displayNameKey = profession.nameKey,
                    descriptionKey = profession.descKey,
                    unlockState = unlockState,
                    playabilityState = ClassAvailabilityResolver.resolve(unlockState = unlockState, context = context),
                    tier = profession.tier,
                    resourceHintKey = profession.resourceHintKey,
                )
            }
        val raceOptions =
            schemaCatalog.races.map { race ->
                RacePlayerCreationOption(
                    id = race.id,
                    displayNameKey = race.nameKey,
                    descriptionKey = race.descKey,
                    unlockState = race.initialUnlockState,
                    playabilityState =
                        ClassAvailabilityResolver.resolve(
                            unlockState = race.initialUnlockState,
                            context = context,
                        ),
                )
            }
        return PlayerCreationState(
            professionOptions = professionOptions,
            raceOptions = raceOptions,
            selection =
                resolvePlayerCreationSelection(
                    previousSelection = previousSelection,
                    professionOptions = professionOptions,
                    raceOptions = raceOptions,
                ),
        )
    }

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
        contentPackSelection: ContentPackSelection = ContentPackSelection.EMPTY,
    ): FoundationGameSession =
        createNewSession(
            config = config,
            saveManager = saveManager,
            locale = locale,
            profile = profile,
            availabilityContext = availabilityContext,
            contentPackSelection = contentPackSelection,
            validationSessionOptions = null,
        )

    fun newValidationSession(
        request: ValidationSessionRequest = ValidationSessionRequest(),
    ): FoundationGameSession =
        createNewSession(
            config = request.options.foundationConfig,
            saveManager = request.saveManager,
            locale = request.locale,
            profile = request.profile,
            availabilityContext = AvailabilityContext.WHITE_BOX,
            contentPackSelection = request.options.contentPackSelection,
            validationSessionOptions = request.options,
        )

    private fun createNewSession(
        config: FoundationGameConfig,
        saveManager: SaveManager,
        locale: GameLocale,
        profile: ProfileData,
        availabilityContext: AvailabilityContext,
        contentPackSelection: ContentPackSelection,
        validationSessionOptions: ValidationSessionOptions?,
    ): FoundationGameSession {
        val content = loadContent(locale = locale, contentPackSelection = contentPackSelection)
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
            worldProgress = initialWorldProgress(content.schemaCatalog),
            shopStates = content.schemaCatalog.shopNodes.associateByTo(linkedMapOf(), ShopNode::id) { shop ->
                ShopInventoryState(shopId = shop.id)
            },
            zoneRuntimeFactory = { nextConfig -> buildZoneRuntime(content, nextConfig) },
            validationSessionOptions = validationSessionOptions,
        )
    }

    fun loadFoundationSession(
        saveManager: SaveManager,
        locale: GameLocale = GameLocale.DEFAULT,
        contentPackSelection: ContentPackSelection = ContentPackSelection.EMPTY,
    ): FoundationGameSession? =
        loadSession(
            saveManager = saveManager,
            locale = locale,
            contentPackSelection = contentPackSelection,
            validationSessionOptions = null,
        )

    fun loadValidationSession(
        saveManager: SaveManager,
        locale: GameLocale = GameLocale.DEFAULT,
        validationSessionOptions: ValidationSessionOptions = ValidationSessionOptions(),
        contentPackSelection: ContentPackSelection = ContentPackSelection.EMPTY,
    ): FoundationGameSession? {
        val persistedOptions = loadPersistedValidationSessionOptions(saveManager)
        val resolvedOptions = persistedOptions ?: validationSessionOptions
        val resolvedContentPackSelection =
            persistedOptions?.contentPackSelection
                ?: resolvedOptions.contentPackSelection.takeUnless { selection -> selection.isEmpty }
                ?: contentPackSelection
        return loadValidationSessionResolved(
            saveManager = saveManager,
            locale = locale,
            resolvedOptions = resolvedOptions,
            contentPackSelection = resolvedContentPackSelection,
        )
    }

    fun loadValidationSessionResolved(
        saveManager: SaveManager,
        locale: GameLocale = GameLocale.DEFAULT,
        resolvedOptions: ValidationSessionOptions,
        contentPackSelection: ContentPackSelection = resolvedOptions.contentPackSelection,
    ): FoundationGameSession? =
        loadSession(
            saveManager = saveManager,
            locale = locale,
            contentPackSelection = contentPackSelection,
            validationSessionOptions = resolvedOptions,
        )

    private fun loadSession(
        saveManager: SaveManager,
        locale: GameLocale,
        contentPackSelection: ContentPackSelection,
        validationSessionOptions: ValidationSessionOptions?,
    ): FoundationGameSession? {
        val snapshot = saveManager.load() ?: return null
        val loader = DataLoader(locale = locale, packSelection = contentPackSelection)
        val content = buildContent(loader)
        validateLoadedPackEnvironment(snapshot = snapshot, content = content)
        val restored = SessionSnapshotMapper.fromSaveSnapshot(snapshot, mapgenPipeline = content.mapgenPipeline)
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
            headlessTurnEquivalent = restored.headlessTurnEquivalent,
            worldProgress = restored.worldProgress,
            shardBalance = restored.shardBalance,
            shopStates = restored.shopStates.associateByTo(linkedMapOf(), ShopInventoryState::shopId),
            cadenceRewardCount = restored.cadenceRewardCount,
            restoredPityTracker = restored.pityTracker,
            restoredMilestoneRewardSummaries = restored.milestoneRewards,
            combatRandomSource =
                restored.combatRandomState?.let(SplitMix64RandomSource::fromState)
                    ?: FoundationGameSession.defaultCombatRandomSource(sessionConfig, restored.turnCount),
            sessionRandom =
                restored.sessionRandomState?.let(SplitMix64RandomSource::fromState)
                    ?: FoundationGameSession.defaultSessionRandomSource(sessionConfig, restored.turnCount),
            restoredPendingActionIds = restored.pendingActionIds,
            restoredActiveTurnActorId = restored.activeTurnActorId,
            zoneRuntimeFactory = { nextConfig -> buildZoneRuntime(content, nextConfig) },
            validationSessionOptions = validationSessionOptions,
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

    private fun loadContent(
        locale: GameLocale,
        contentPackSelection: ContentPackSelection,
    ): GameContent {
        val loader = DataLoader(locale = locale, packSelection = contentPackSelection)
        return buildContent(loader)
    }

    private fun buildContent(loader: DataLoader): GameContent {
        val schemaCatalog = loader.loadSchemaCatalog()
        val talents = loader.loadTalentDefinitions()
        val statusCatalog = loader.loadStatusCatalog()
        val mapgenContentCatalog = SchemaMapgenContentCatalogFactory.from(schemaCatalog)
        val zoneMapgenProfileResolver =
            SchemaZoneMapgenProfileResolver(
                zones = schemaCatalog.zones,
                profiles = schemaCatalog.zoneMapgenProfiles,
            )
        val zoneRewardProfileResolver =
            SchemaZoneRewardProfileResolver(
                zones = schemaCatalog.zones,
                profiles = schemaCatalog.zoneRewardProfiles,
            )
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
            activePackIds = loader.activePackIds,
            activePackManifestVersions = loader.activePackManifestVersions,
            telegraphRegistry = TelegraphRegistry(schemaCatalog.telegraphSpecs.associateBy { spec -> spec.id }),
            threatProfileRegistry = ThreatProfileRegistry(schemaCatalog.threatProfiles.associateBy { profile -> profile.id }),
            zoneMapgenProfileResolver = zoneMapgenProfileResolver,
            zoneRewardProfileResolver = zoneRewardProfileResolver,
            mapgenContentCatalog = mapgenContentCatalog,
            mapgenPipeline =
                RoutedMapgenPipeline(
                    zones = schemaCatalog.zones,
                    migratedZonePipeline =
                        HybridTopologyMapgenPipeline(
                            profileResolver = zoneMapgenProfileResolver,
                            contentCatalog = mapgenContentCatalog,
                        ),
                    compatibilityPipeline = BspBackedMapgenPipeline(profileResolver = zoneMapgenProfileResolver),
                ),
        ).also { content ->
            content.validateEliteMutationContracts()
            validateAiProfileContracts(content)
            validateWorldStructureContracts(content)
        }
    }

    private fun validateLoadedPackEnvironment(
        snapshot: com.ktome.core.save.SaveSnapshot,
        content: GameContent,
    ) {
        val expectedPackIds = snapshot.activePackIds
        val actualPackIds = content.activePackIds
        if (expectedPackIds != actualPackIds) {
            throw InvalidSaveException(
                "Save expects activePackIds=${expectedPackIds.map(PackId::value)}, " +
                    "but loader resolved ${actualPackIds.map(PackId::value)}. Start a new run.",
            )
        }
        if (snapshot.activePackManifestVersions != content.activePackManifestVersions) {
            throw InvalidSaveException(
                "Save expects activePackManifestVersions=${
                    snapshot.activePackManifestVersions.mapKeys { (packId, _) -> packId.value }
                }, but loader resolved ${
                    content.activePackManifestVersions.mapKeys { (packId, _) -> packId.value }
                }. Start a new run.",
            )
        }
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
            ZoneMechanicRuntime.introHintKey(zone)?.let { hintKey ->
                add(
                    RenderLogEventSnapshot(
                        RenderTextTokenSnapshot(
                            "log.zone.mechanic_hint",
                            arguments = listOf(RenderTextArgumentSnapshot(name = "hint", valueKey = hintKey)),
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
        val generatedFloor =
            content.mapgenPipeline.run(
                MapgenRequest(
                    zoneId = zone.id,
                    floorIndex = floor,
                    seed = floorSeed(config.seed, floor, 0x44A1),
                    targetWidth = config.width,
                    targetHeight = config.height,
                ),
            )
        val map = generatedFloor.map
        val world = World()
        reserveEntityRange(world, config.routeIndex, floor)
        val factory = EntityFactory()
        val itemFactory = ItemFactory()
        val bossFactory = BossFactory(factory)
        val encounterDecorationService = EncounterDecorationService(content)
        val itemGenerator = ItemGenerator(content.itemBundle, com.ktome.core.random.RandomSource.from(Random(floorSeed(config.seed, floor, 0x91F3))))
        val profession = resolveProfession(content.schemaCatalog, config.playerProfessionId)
        val affixBuildContext = professionAffixBuildContext(content.schemaCatalog, profession)
        val monsterRandom = Random(floorSeed(config.seed, floor, 0x63AF))
        val bossDefinition = if (floor == config.maxFloor) resolveBossDefinition(content, zone) else null
        val zoneMonsterCatalog = resolveZoneMonsterCatalog(content, zone, floor)
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
                val decoration =
                    encounterDecorationService.selectDecoration(
                        request =
                            SpawnDecorationRequest(
                                zoneId = zone.id,
                                floorIndex = floor,
                                template = bossDefinition.template,
                                bossEncounterId = bossDefinition.encounter.id,
                                preferredBossVariantId = config.preferredBossVariantId,
                                bossVariantSelectionMode = config.bossVariantSelectionMode,
                            ),
                        nextIndex = { bound -> monsterRandom.nextInt(bound) },
                    )
                val resolvedBossPosition =
                    chooseBossPosition(
                        map = map,
                        occupiedPoints = occupiedPoints,
                        terrainTagsByPoint = generatedFloor.terrainTags,
                        preferredTerrainTags = decoration.preferredTerrainTags,
                    )
                val bossId = bossFactory.createBoss(world, bossDefinition, resolvedBossPosition)
                encounterDecorationService.applyDecoration(world = world, entityId = bossId, decoration = decoration)
                StatsCalculator.recalculateAndStore(world, bossId)
                occupiedPoints += resolvedBossPosition
                resolvedBossPosition
            } else {
                spawnMonsters(
                    zone = zone,
                    encounterDecorationService = encounterDecorationService,
                    factory = factory,
                    world = world,
                    map = map,
                    generatedFloor = generatedFloor,
                    floor = floor,
                    catalog = zoneMonsterCatalog,
                    occupiedPoints = occupiedPoints,
                    random = monsterRandom,
                    desiredCount = zoneMonsterSpawnCount(zone, floor, map.rooms.size),
                )
                spawnItems(
                    itemFactory = itemFactory,
                    itemGenerator = itemGenerator,
                    affixContext = affixBuildContext,
                    world = world,
                    map = map,
                    zone = zone,
                    zoneRewardProfile = content.zoneRewardProfileResolver.resolve(zone.id),
                    floor = floor,
                    seed = config.seed,
                    occupiedPoints = occupiedPoints,
                )
                null
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
        createShopInteractable(
            world = world,
            map = map,
            floor = floor,
            zone = zone,
            content = content,
            occupiedPoints = occupiedPoints,
            stairsUp = stairsUp,
        )
        ZoneMechanicRuntime.installFloorRuntime(
            config = config,
            zone = zone,
            floor = floor,
            map = map,
            world = world,
            occupiedPoints = occupiedPoints,
            catalog = zoneMonsterCatalog,
        )

        return FloorState(
            floor = floor,
            stairsUp = stairsUp,
            stairsDown = stairsDown,
            payload =
                SessionSnapshotMapper.captureFloor(
                    generatedFloor = generatedFloor,
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
        encounterDecorationService: EncounterDecorationService,
        factory: EntityFactory,
        world: World,
        map: com.ktome.core.map.GameMap,
        generatedFloor: com.ktome.core.mapgen.GeneratedFloor,
        floor: Int,
        catalog: List<MonsterTemplate>,
        occupiedPoints: MutableSet<Point>,
        random: Random,
        desiredCount: Int,
    ) {
        if (desiredCount <= 0) {
            return
        }
        val availableTemplates = catalog.filter { floor in it.spawnFloors }.ifEmpty { catalog }
        val roomCandidates = map.rooms.drop(1)
        if (availableTemplates.isEmpty() || roomCandidates.isEmpty()) {
            return
        }
        val maxSpawnSlots = roomCandidates.size + if (allowsRoomPack(zone, floor)) 1 else 0
        val spawnCount = desiredCount.coerceIn(1, maxSpawnSlots)
        val selectedTemplates = selectMonsterTemplates(zone, floor, availableTemplates, spawnCount, random)
        val plannedRooms = plannedMonsterRooms(zone, floor, roomCandidates, selectedTemplates.size, random)
        val encounterPlans =
            selectedTemplates.map { template ->
                val forceEliteMutationEligibility =
                    template.id in zone.elitePools &&
                        !template.isEliteEncounterTemplate()
                template to
                    encounterDecorationService.selectDecoration(
                        request =
                            SpawnDecorationRequest(
                                zoneId = zone.id,
                                floorIndex = floor,
                                template = template,
                                forceEliteMutationEligibility = forceEliteMutationEligibility,
                            ),
                        nextIndex = { bound -> random.nextInt(bound) },
                    )
            }

        encounterPlans.forEachIndexed { index, (template, decoration) ->
            val fallbackRoom = plannedRooms[index]
            val placement =
                chooseTerrainAwareRoomPlacement(
                    roomCandidates = roomCandidates,
                    fallbackRoom = fallbackRoom,
                    map = map,
                    occupiedPoints = occupiedPoints,
                    terrainTagsByPoint = generatedFloor.terrainTags,
                    preferredTerrainTags = decoration.preferredTerrainTags,
                )
            val monsterId =
                factory.createMonster(
                    world = world,
                    template = template,
                    position = placement.spawnPoint,
                    patrolRoute = if (resolveAiType(template) == AIType.PATROL) buildPatrolRoute(room = placement.room, map = map) else null,
                )
            encounterDecorationService.applyDecoration(world = world, entityId = monsterId, decoration = decoration)
            StatsCalculator.recalculateAndStore(world, monsterId)
            occupiedPoints += placement.spawnPoint
        }
    }

    private fun zoneMonsterSpawnCount(
        zone: ZoneSchemaV2,
        floor: Int,
        roomCount: Int,
    ): Int {
        return when (zone.id) {
            "shattered_outpost" -> 2
            "greenwood_fringe" -> 2
            "deep_iron_pit" -> 1
            "grey_gate_depths" -> 2
            "underground_river" -> 2
            "abyssal_temple" -> 1
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
        guaranteedEncounterTemplate(zone, floor, availableTemplates, random)?.let(selected::add)
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

    private fun guaranteedEncounterTemplate(
        zone: ZoneSchemaV2,
        floor: Int,
        availableTemplates: List<MonsterTemplate>,
        random: Random,
    ): MonsterTemplate? {
        if (zone.mapgenProfileId == null) {
            return null
        }
        val elitePoolIds = zone.elitePools.toSet()
        val eliteCandidates = availableTemplates.filter { template -> template.id in elitePoolIds }
        if (eliteCandidates.isEmpty()) {
            return null
        }
        return selectWeightedTemplate(
            candidates = eliteCandidates,
            zone = zone,
            floor = floor,
            selected = emptyList(),
            random = random,
            uniqueOnly = true,
        )
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
            zone.id == "greenwood_fringe" -> 0
            zone.id == "deep_iron_pit" -> 1
            zone.id == "grey_gate_depths" -> 1
            zone.id == "underground_river" -> 1
            zone.id == "abyssal_temple" -> 1
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
        affixContext: AffixSelectionContext,
        world: World,
        map: com.ktome.core.map.GameMap,
        zone: ZoneSchemaV2,
        zoneRewardProfile: ZoneRewardProfile,
        floor: Int,
        seed: Long,
        occupiedPoints: MutableSet<Point>,
    ) {
        val itemRooms = map.rooms.drop(1).take(4)
        itemRooms.forEach { room ->
            val spawnPoint = findSpawnPoints(room, map, occupiedPoints).first()
            itemFactory.createGroundItem(
                world,
                itemGenerator.generate(
                    context =
                        com.ktome.core.loot.LootRollContext(
                            sourceLevel = zone.recommendedLevel.max,
                            sourceTier = com.ktome.core.loot.SourceTier.NORMAL,
                            zoneId = zone.id,
                            playerLevel = zone.recommendedLevel.max,
                            magicFindBonus = 0.0f,
                            seed = floorSeed(seed, floor, 0x91F3 + room.center.x + room.center.y),
                        ),
                    zoneRewardProfile = zoneRewardProfile,
                    affixContext = affixContext,
                ),
                spawnPoint,
            )
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

    private fun buildPatrolRoute(
        room: Room,
        map: com.ktome.core.map.GameMap,
    ): PatrolRoute = PatrolRoute(buildPatrolWaypoints(room = room, map = map))

    private fun chooseDownstairs(
        map: com.ktome.core.map.GameMap,
        upstairs: Point?,
    ): Point {
        val reachable = reachablePassablePoints(map = map, start = upstairs ?: map.playerStart)
        val candidates = map.rooms.asReversed().map(Room::center).filter(reachable::contains) + map.floorPoints().filter(reachable::contains)
        return candidates.first { point -> point != upstairs }
    }

    private fun chooseBossPosition(
        map: com.ktome.core.map.GameMap,
        occupiedPoints: Set<Point>,
        terrainTagsByPoint: Map<Point, Set<com.ktome.core.mapgen.TerrainTag>>,
        preferredTerrainTags: Set<com.ktome.core.mapgen.TerrainTag>,
    ): Point {
        val reachable = reachablePassablePoints(map = map, start = map.playerStart)
        val candidates =
            (
                map.rooms.asReversed().map(Room::center).filter { point -> point !in occupiedPoints && point in reachable } +
                    map.floorPoints().filter { point -> point !in occupiedPoints && point in reachable }
            ).distinct()
        val fallbackPoint = candidates.first()
        return chooseTerrainAwarePoint(
            candidatePoints = candidates,
            fallbackPoint = fallbackPoint,
            map = map,
            occupiedPoints = occupiedPoints,
            terrainTagsByPoint = terrainTagsByPoint,
            preferredTerrainTags = preferredTerrainTags,
        ).first
    }

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
        val visibleRoomCenterAnchor = routeVisibleAnchor(map, routeSeed, fallbackRoomCenter(map))
        val roomCenterAnchor =
            if (stairsDown != null && !pathExists(map = map, start = visibleRoomCenterAnchor, goal = stairsDown)) {
                connectedRoomCenterAnchor(map = map, routeSeed = routeSeed, routeTarget = stairsDown)
            } else {
                visibleRoomCenterAnchor
            }
        val anchor =
            when (placement.anchor.lowercase()) {
                "player_start" -> map.playerStart
                "stairs_up" -> stairsUp ?: map.playerStart
                "stairs_down" -> stairsDown ?: fallbackRoomCenter(map)
                "room_center" -> roomCenterAnchor
                "boss_entry" -> routeVisibleAnchor(map, routeSeed, bossEntryPoint(map, bossPosition))
                else -> error("Unsupported interactable placement anchor '${placement.anchor}'.")
            }
        return anchor + Point(placement.offset.x, placement.offset.y)
    }

    private fun connectedRoomCenterAnchor(
        map: com.ktome.core.map.GameMap,
        routeSeed: Point,
        routeTarget: Point?,
    ): Point {
        val fallback = fallbackRoomCenter(map)
        routeTarget?.let { target ->
            val route = AStar.findPath(map = map, start = routeSeed, goal = target, blocked = emptySet())
            val interior = route.drop(1).dropLast(1)
            if (interior.isNotEmpty()) {
                return interior[interior.size / 2]
            }
        }
        val focusPoint = routeTarget?.let { target -> Point((routeSeed.x + target.x) / 2, (routeSeed.y + target.y) / 2) } ?: fallback
        return map.rooms
            .asSequence()
            .map(Room::center)
            .filter { point -> point != routeSeed && !map[point].blocksMovement }
            .filter { point -> pathExists(map = map, start = routeSeed, goal = point) }
            .filter { point -> routeTarget == null || pathExists(map = map, start = point, goal = routeTarget) }
            .minWithOrNull(
                compareBy<Point> { point -> point.chebyshevDistanceTo(focusPoint) }
                    .thenBy { point -> point.chebyshevDistanceTo(routeSeed) }
                    .thenBy(Point::y)
                    .thenBy(Point::x),
            ) ?: fallback
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

    private fun pathExists(
        map: com.ktome.core.map.GameMap,
        start: Point,
        goal: Point,
    ): Boolean =
        AStar.findPath(
            map = map,
            start = start,
            goal = goal,
            blocked = emptySet(),
        ).isNotEmpty()

    private fun reachablePassablePoints(
        map: com.ktome.core.map.GameMap,
        start: Point,
    ): Set<Point> {
        if (map[start].blocksMovement) {
            return emptySet()
        }
        val visited = linkedSetOf(start)
        val queue = ArrayDeque<Point>()
        queue += start
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            Point.CARDINAL_DIRECTIONS
                .asSequence()
                .map { delta -> current + delta }
                .filter { point ->
                    map.isInBounds(point.x, point.y) &&
                        !map[point].blocksMovement &&
                        point !in visited
                }.sortedWith(compareBy<Point>(Point::y).thenBy(Point::x))
                .forEach { point ->
                    visited += point
                    queue += point
                }
        }
        return visited
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
                    "merchant_stall" -> '$'
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
                    "merchant_stall" -> "#F2D16B"
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

    private fun createShopInteractable(
        world: World,
        map: com.ktome.core.map.GameMap,
        floor: Int,
        zone: ZoneSchemaV2,
        content: GameContent,
        occupiedPoints: MutableSet<Point>,
        stairsUp: Point?,
    ) {
        val shopNodeId = zone.shopNodeId ?: return
        val placementFloor =
            when (shopNodeId) {
                "greenwood_supply_post" -> 1
                "deep_iron_pit_waystation" -> maxOf(1, zone.floorCount / 2)
                else -> 1
            }
        if (floor != placementFloor) {
            return
        }
        val preferredPoint = routeVisibleAnchor(map, stairsUp ?: map.playerStart, fallbackRoomCenter(map))
        val shopPoint = findObjectiveInteractablePoint(map, preferredPoint, occupiedPoints) ?: return
        createInteractable(world, "merchant_stall", shopPoint, content)
        occupiedPoints += shopPoint
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
                if (floor > 1 || zoneAllowsEarlyTerrainEliteUptake(zone.id)) {
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

    private fun zoneAllowsEarlyTerrainEliteUptake(zoneId: String): Boolean =
        zoneId in
            setOf(
                "deep_iron_pit",
                "greenwood_fringe",
                "underground_river",
                "crystal_cavern",
            )

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

    private fun resolvePlayerCreationSelection(
        previousSelection: PlayerCreationSelection?,
        professionOptions: List<ProfessionPlayerCreationOption>,
        raceOptions: List<RacePlayerCreationOption>,
    ): PlayerCreationSelection =
        PlayerCreationSelection(
            professionId = resolveSelectedOptionId(previousSelection?.professionId, professionOptions),
            raceId = resolveSelectedOptionId(previousSelection?.raceId, raceOptions),
        )

    private fun resolveSelectedOptionId(
        previousId: String?,
        options: List<PlayerCreationOption>,
    ): String =
        previousId
            ?.let { optionId -> options.firstOrNull { option -> option.id == optionId } }
            ?.takeIf { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }
            ?.id
            ?: options.firstOrNull { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }?.id
            ?: options.first().id

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

    private fun validateWorldStructureContracts(content: GameContent) {
        val schemaCatalog = content.schemaCatalog
        val zonesById = schemaCatalog.zones.associateBy(ZoneSchemaV2::id)
        val shopsById = schemaCatalog.shopNodes.associateBy(ShopNode::id)
        val objectivesById = schemaCatalog.objectiveSets.associateBy { objective -> objective.id }
        val questsById = schemaCatalog.questProgressions.associateBy { quest -> quest.questId }
        val worldConnectionsById = schemaCatalog.worldGraph.connections.associateBy { connection -> connection.id }
        require(shopsById.size == schemaCatalog.shopNodes.size) {
            "Shop ids must stay unique."
        }
        val routeRewardsById = schemaCatalog.routeRewards.associateBy { reward -> reward.routeId }
        require(routeRewardsById.size == schemaCatalog.routeRewards.size) {
            "Route reward ids must stay unique."
        }
        require(routeRewardsById.keys == worldConnectionsById.keys) {
            val missingRewards = worldConnectionsById.keys - routeRewardsById.keys
            val orphanRewards = routeRewardsById.keys - worldConnectionsById.keys
            "Route rewards must match world graph connections. Missing=$missingRewards orphaned=$orphanRewards"
        }

        schemaCatalog.questProgressions.forEach { quest ->
            require(quest.objectiveStates.isEmpty() || quest.objectiveStates.values.any { state -> state != ObjectiveState.COMPLETED }) {
                "Quest '${quest.questId}' must not start fully completed in seed data."
            }
        }

        schemaCatalog.objectiveSets.forEach { objective ->
            objective.linkedQuestId?.let { questId ->
                val quest = requireNotNull(questsById[questId]) {
                    "Objective set '${objective.id}' references unknown quest '$questId'."
                }
                val questObjectiveId = requireNotNull(objective.questObjectiveId) {
                    "Objective set '${objective.id}' must define questObjectiveId when linkedQuestId is present."
                }
                require(questObjectiveId in quest.objectiveStates) {
                    "Objective set '${objective.id}' references unknown quest objective '$questObjectiveId' in quest '$questId'."
                }
            }
        }

        schemaCatalog.shopNodes.forEach { shop ->
            require(shop.zoneId in zonesById) {
                "Shop '${shop.id}' references unknown zone '${shop.zoneId}'."
            }
            shop.rescuePolicy.guaranteedTags.forEach { tag ->
                require(shop.inventory.any { offer -> tag in offer.tags }) {
                    "Shop '${shop.id}' is missing guaranteed rescue tag '$tag' in inventory."
                }
            }
            val affordableOffers =
                ShardEconomy.mandatoryAffordableOffers(
                    offers = shop.inventory,
                    balance = shop.rescuePolicy.affordability.expectedShardBudgetByCheckpoint,
                    requiredTags = shop.rescuePolicy.affordability.requiredAffordableTags,
                )
            require(affordableOffers.size >= shop.rescuePolicy.affordability.mandatoryAffordableItemCount) {
                "Shop '${shop.id}' fails rescue affordability contract at checkpoint '${shop.rescuePolicy.affordability.checkpointId}'."
            }
        }

        val interactablesById = schemaCatalog.interactables.associateBy { interactable -> interactable.id }
        schemaCatalog.zones.forEach { zone ->
            val objective =
                zone.objectiveSetId?.let { objectiveSetId ->
                    require(objectiveSetId in objectivesById) {
                        "Zone '${zone.id}' references unknown objective set '$objectiveSetId'."
                    }
                    requireNotNull(objectivesById[objectiveSetId]) {
                        "Zone '${zone.id}' references unknown objective set '$objectiveSetId'."
                    }
                }

            require(zone.worldRole != "optional" || objective != null) {
                "Optional zone '${zone.id}' must define objectiveSetId."
            }
            if (zone.worldRole == "optional") {
                require(objective != null && objective.interactables.isNotEmpty() && objective.placements.isNotEmpty()) {
                    "Optional zone '${zone.id}' must define a non-empty objective/interactable hook."
                }
            }
            if (zone.id in setOf("underground_river", "abyssal_temple", "abyssal_heart")) {
                require(objective != null && objective.interactables.isNotEmpty() && objective.placements.isNotEmpty()) {
                    "Late zone '${zone.id}' must expose at least one runtime objective hook."
                }
            }
            objective?.let { objectiveSet ->
                require(objectiveSet.linkedQuestId != null && objectiveSet.questObjectiveId != null) {
                    "Zone '${zone.id}' objective '${objectiveSet.id}' must bind to quest-backed runtime state."
                }
                objectiveSet.placements.forEach { placement ->
                    require(placement.interactableId in objectiveSet.interactables) {
                        "Objective '${objectiveSet.id}' placement '${placement.interactableId}' must be declared in interactables."
                    }
                    require(placement.floor in 1..zone.floorCount) {
                        "Objective '${objectiveSet.id}' placement floor ${placement.floor} exceeds zone '${zone.id}' floorCount ${zone.floorCount}."
                    }
                }
                objectiveSet.interactables.forEach { interactableId ->
                    val interactable = requireNotNull(interactablesById[interactableId]) {
                        "Objective '${objectiveSet.id}' references unknown interactable '$interactableId'."
                    }
                    interactable.shopNodeId?.let { shopNodeId ->
                        val shop = requireNotNull(shopsById[shopNodeId]) {
                            "Interactable '$interactableId' references unknown shop '$shopNodeId'."
                        }
                        require(shop.zoneId == zone.id) {
                            "Interactable '$interactableId' in zone '${zone.id}' references shop '$shopNodeId', but the shop belongs to zone '${shop.zoneId}'."
                        }
                    }
                }
            }
            zone.shopNodeId?.let { shopNodeId ->
                val shop = requireNotNull(shopsById[shopNodeId]) {
                    "Zone '${zone.id}' references unknown shop '$shopNodeId'."
                }
                require(shop.zoneId == zone.id) {
                    "Zone '${zone.id}' references shop '$shopNodeId', but the shop belongs to zone '${shop.zoneId}'."
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
        val graph = schemaCatalog.worldGraph
        config.zoneRoute.zipWithNext().forEach { (fromZoneId, toZoneId) ->
            val canTraverse =
                graph.outgoingConnections(fromZoneId).any { connection ->
                    graph.destinationFor(fromZoneId, connection) == toZoneId
                }
            if (!canTraverse) {
                return "Zone route contains an unreachable edge: $fromZoneId -> $toZoneId."
            }
        }
        return null
    }

    private fun initialWorldProgress(schemaCatalog: SchemaCatalog): WorldProgressDef {
        val questStates = schemaCatalog.questProgressions.associateBy { quest -> quest.questId }
        return WorldProgressDef(
            questStates = questStates,
        )
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

internal fun buildPatrolWaypoints(
    room: Room,
    map: com.ktome.core.map.GameMap,
): List<Point> {
    val perimeterPoints =
        listOf(
            Point(room.left + 1, room.top + 1),
            Point(room.right - 1, room.top + 1),
            Point(room.right - 1, room.bottom - 1),
            Point(room.left + 1, room.bottom - 1),
        ).filter { point ->
            room.contains(point) &&
                map.isInBounds(point.x, point.y) &&
                !map[point].blocksMovement
        }
    if (perimeterPoints.isNotEmpty()) {
        return perimeterPoints.distinct()
    }
    return buildList {
        for (y in room.top + 1 until room.bottom) {
            for (x in room.left + 1 until room.right) {
                val point = Point(x, y)
                if (map.isInBounds(x, y) && !map[point].blocksMovement) {
                    add(point)
                }
            }
        }
    }.distinct().ifEmpty { listOf(room.center) }
}
