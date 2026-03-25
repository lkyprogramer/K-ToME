package com.ktome.game

import com.ktome.core.ai.AIPerceptionState
import com.ktome.core.ai.BossEncounterState
import com.ktome.core.ai.DangerLevel
import com.ktome.core.ai.PendingTelegraphState
import com.ktome.core.dungeon.FloorState
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.AiTriggerTracker
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Energy
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.ExperienceReward
import com.ktome.core.ecs.Faction
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.PlayerControlled
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.ResistanceProfile
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.effect.AreaEffectEmitter
import com.ktome.core.effect.WorldEffect
import com.ktome.core.inscription.InscriptionCooldownState
import com.ktome.core.inscription.InscriptionLoadout
import com.ktome.core.inscription.InscriptionSlot
import com.ktome.core.item.AffixType
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.Equipment
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.GroundItem
import com.ktome.core.item.Inventory
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemQuality
import com.ktome.core.item.ItemType
import com.ktome.core.item.MaterialDef
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.race.RaceTalentPointBank
import com.ktome.core.resource.EquilibriumAffinity
import com.ktome.core.resource.EquilibriumState
import com.ktome.core.resource.ResourcePoolSnapshot
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.AIBehaviorSnapshot
import com.ktome.core.save.AIPerceptionSnapshot
import com.ktome.core.save.AiTriggerTrackerSnapshot
import com.ktome.core.save.ActiveEffectSnapshot
import com.ktome.core.save.AreaEffectEmitterSnapshot
import com.ktome.core.save.BossEncounterStateSnapshot
import com.ktome.core.save.CombatProfileSnapshot
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.EquipmentSnapshot
import com.ktome.core.save.ExperienceSnapshot
import com.ktome.core.save.FloorSnapshot
import com.ktome.core.save.InscriptionLoadoutSnapshot
import com.ktome.core.save.InscriptionSlotSaveSnapshot
import com.ktome.core.save.InventorySnapshot
import com.ktome.core.save.InvalidSaveException
import com.ktome.core.save.ItemSnapshot
import com.ktome.core.save.MapSnapshot
import com.ktome.core.save.PendingTelegraphSnapshot
import com.ktome.core.save.PatrolRouteSnapshot
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.SaveRestoreException
import com.ktome.core.save.StairSnapshot
import com.ktome.core.save.StatModifierSnapshot
import com.ktome.core.save.TalentAllocationDraftSnapshot
import com.ktome.core.save.StatsSnapshot
import com.ktome.core.save.TalentLoadoutSnapshot
import com.ktome.core.save.WorldEffectSnapshot
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.ActiveEffect
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.TalentAllocationDraft
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.core.status.StatusDefinitions
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusLifecycle
import com.ktome.game.model.MonsterTemplate

private const val HERO_GLYPH: Char = '@'
private const val HERO_COLOR_HEX: String = "#FFD700"
private const val STAIR_COLOR_HEX: String = "#D7E7FF"

internal data class FloorRuntimeState(
    val map: GameMap,
    val stairsUp: Point? = null,
    val stairsDown: Point? = null,
    val exploredTiles: LinkedHashSet<Point> = linkedSetOf(),
    val entities: MutableList<EntitySnapshot> = mutableListOf(),
)

internal data class RestoredRunState(
    val config: FoundationGameConfig,
    val currentFloor: Int,
    val turnCount: Int,
    val player: PlayerSnapshot,
    val floors: List<FloorState<FloorRuntimeState>>,
    val combatRandomState: Long? = null,
    val sessionRandomState: Long? = null,
    val pendingActionIds: List<Int> = emptyList(),
    val activeTurnActorId: Int? = null,
)

internal object SessionSnapshotMapper {
    private val legacyMonsterTemplateIds =
        mapOf(
            "rat" to "beast.rat",
            "bone_archer" to "undead.bone_archer",
            "sentry" to "bandit.sentry",
            "orc" to "orc.raider",
            "dungeon_lord" to FOUNDATION_BOSS_TEMPLATE_ID,
        )

    fun capturePlayer(
        world: World,
        playerId: EntityId,
    ): PlayerSnapshot {
        val carriedIds = carriedEntityIds(world, playerId)
        return PlayerSnapshot(
            entity = captureEntity(world, playerId),
            carriedEntities = carriedIds.map { carriedId -> captureEntity(world, carriedId) },
        )
    }

    fun captureFloor(
        map: GameMap,
        stairsUp: Point?,
        stairsDown: Point?,
        exploredTiles: Set<Point>,
        world: World,
        excludedEntities: Set<EntityId>,
    ): FloorRuntimeState =
        FloorRuntimeState(
            map = map,
            stairsUp = stairsUp,
            stairsDown = stairsDown,
            exploredTiles = linkedSetOf<Point>().apply { addAll(exploredTiles) },
            entities =
                world.entitiesWith()
                    .filter { entityId -> entityId !in excludedEntities }
                    .map { entityId -> captureEntity(world, entityId) }
                    .toMutableList(),
        )

    fun restoreWorld(
        content: GameContent,
        player: PlayerSnapshot,
        floor: FloorRuntimeState,
    ): World {
        val world = World()
        val snapshots = (listOf(player.entity) + player.carriedEntities + floor.entities).sortedBy(EntitySnapshot::id)
        val snapshotById = snapshots.associateBy(EntitySnapshot::id)

        snapshots.forEach { snapshot ->
            world.createEntity(EntityId(snapshot.id))
        }
        snapshots.forEach { snapshot ->
            restoreEntity(world, snapshot, content)
        }
        snapshotById.forEach { (entityId, snapshot) ->
            val entity = EntityId(entityId)
            if (world.get<Stats>(entity) != null && world.get<CombatProfile>(entity) != null) {
                StatsCalculator.recalculateAndStore(world, entity)
            }
            world.get<Health>(entity)?.let { health ->
                snapshot.healthCurrent?.let { health.current = it.coerceIn(0, health.max) }
            }
            val snapshotStamina =
                snapshot.resourcePools
                    .firstOrNull { pool -> pool.type == ResourceType.STAMINA.name }
                    ?.current
            if (snapshotStamina != null) {
                world.get<ResourcePools>(entity)?.pool(ResourceType.STAMINA)?.let { pool ->
                    pool.syncTo(nextCurrent = snapshotStamina, nextMax = pool.max)
                }
            }
        }
        return world
    }

    fun toSaveSnapshot(
        config: FoundationGameConfig,
        currentFloor: Int,
        turnCount: Int,
        player: PlayerSnapshot,
        floors: List<FloorState<FloorRuntimeState>>,
        combatRandomState: Long?,
        sessionRandomState: Long?,
        pendingActionIds: List<Int>,
        activeTurnActorId: Int?,
    ): SaveSnapshot =
        SaveSnapshot(
            timestampEpochMillis = System.currentTimeMillis(),
            worldSeed = config.seed,
            currentZoneId = config.zoneId,
            zoneRoute = config.zoneRoute,
            routeIndex = config.routeIndex,
            floorIndex = currentFloor,
            mapWidth = config.width,
            mapHeight = config.height,
            fovRadius = config.fovRadius,
            messageLogSize = config.messageLogSize,
            playerProfessionId = config.playerProfessionId,
            playerRaceId = config.playerRaceId,
            maxFloor = config.maxFloor,
            turnCount = turnCount,
            player = canonicalizePlayerSnapshot(player),
            combatRandomState = combatRandomState,
            sessionRandomState = sessionRandomState,
            pendingActionIds = pendingActionIds,
            activeTurnActorId = activeTurnActorId,
            floors =
                floors.sortedBy(FloorState<FloorRuntimeState>::floor).map { floorState ->
                    FloorSnapshot(
                        floorIndex = floorState.floor,
                        map =
                            MapSnapshot(
                                rows = floorState.payload.map.asGlyphRows(),
                                playerStart = PointSnapshot.from(floorState.payload.map.playerStart),
                            ),
                        stairsUp = floorState.stairsUp?.let(PointSnapshot::from),
                        stairsDown = floorState.stairsDown?.let(PointSnapshot::from),
                        exploredTiles = floorState.payload.exploredTiles.sortedCanonicalPoints().map(PointSnapshot::from),
                        entities = floorState.payload.entities.map(::canonicalizeEntitySnapshot).sortedBy(EntitySnapshot::id),
                    )
                },
        )

    fun fromSaveSnapshot(snapshot: SaveSnapshot): RestoredRunState =
        RestoredRunState(
            config =
                FoundationGameConfig(
                    width = snapshot.mapWidth,
                    height = snapshot.mapHeight,
                    seed = snapshot.worldSeed,
                    fovRadius = snapshot.fovRadius,
                    floor = snapshot.floorIndex,
                    maxFloor = snapshot.maxFloor,
                    messageLogSize = snapshot.messageLogSize,
                    zoneId = snapshot.currentZoneId,
                    playerProfessionId = snapshot.playerProfessionId,
                    playerRaceId = snapshot.playerRaceId,
                    zoneRoute = snapshot.zoneRoute,
                    routeIndex = snapshot.routeIndex,
                ),
            currentFloor = snapshot.floorIndex,
            turnCount = snapshot.turnCount,
            player = canonicalizePlayerSnapshot(snapshot.player),
            combatRandomState = snapshot.combatRandomState,
            sessionRandomState = snapshot.sessionRandomState,
            pendingActionIds = snapshot.pendingActionIds.toList(),
            activeTurnActorId = snapshot.activeTurnActorId,
            floors =
                snapshot.floors.map { floor ->
                    FloorState(
                        floor = floor.floorIndex,
                        stairsUp = floor.stairsUp?.toPoint(),
                        stairsDown = floor.stairsDown?.toPoint(),
                        payload =
                            FloorRuntimeState(
                                map = GameMap.fromAscii(rows = floor.map.rows, playerStart = floor.map.playerStart.toPoint()),
                                stairsUp = floor.stairsUp?.toPoint(),
                                stairsDown = floor.stairsDown?.toPoint(),
                                exploredTiles = linkedSetOf<Point>().apply { addAll(floor.exploredTiles.map(PointSnapshot::toPoint)) },
                                entities = floor.entities.map(::canonicalizeEntitySnapshot).sortedBy(EntitySnapshot::id).toMutableList(),
                            ),
                    )
                },
        )

    private fun captureEntity(
        world: World,
        entityId: EntityId,
    ): EntitySnapshot {
        val staminaPool = world.get<ResourcePools>(entityId)?.pool(ResourceType.STAMINA)
        return EntitySnapshot(
            id = entityId.value,
            position = world.get<Position>(entityId)?.toPoint()?.let(PointSnapshot::from),
            blocksMovement = world.get<BlocksMovement>(entityId)?.value == true,
            faction = world.get<FactionTag>(entityId)?.value?.name,
            stats = world.get<Stats>(entityId)?.let(::toStatsSnapshot),
            combatProfile = world.get<CombatProfile>(entityId)?.let(::toCombatProfileSnapshot),
            healthCurrent = world.get<Health>(entityId)?.current,
            energyCurrent = world.get<Energy>(entityId)?.current,
            experience = world.get<Experience>(entityId)?.let(::toExperienceSnapshot),
            experienceReward = world.get<ExperienceReward>(entityId)?.value,
            aiBehavior = world.get<AIBehavior>(entityId)?.let(::toAIBehaviorSnapshot),
            monsterTemplateId = world.get<MonsterTemplateId>(entityId)?.value,
            patrolRoute =
                world.get<PatrolRoute>(entityId)?.let { route ->
                    PatrolRouteSnapshot(
                        waypoints = route.waypoints.map(PointSnapshot::from),
                        nextWaypointIndex = route.nextWaypointIndex,
                    )
                },
            inventory =
                world.get<Inventory>(entityId)?.let { inventory ->
                    InventorySnapshot(capacity = inventory.capacity, itemIds = inventory.itemIds.map(EntityId::value))
                },
            equipment =
                world.get<Equipment>(entityId)?.let { equipment ->
                    EquipmentSnapshot(
                        slots = equipment.slots.mapKeys { (slot, _) -> slot.name }.mapValues { (_, itemId) -> itemId.value },
                    )
                },
            cooldowns = world.get<CooldownState>(entityId)?.remainingByTalentId?.toMap(),
            effects = world.get<EffectTracker>(entityId)?.effects?.map(::toActiveEffectSnapshot),
            areaEffectEmitter = world.get<AreaEffectEmitter>(entityId)?.let(::toAreaEffectEmitterSnapshot),
            worldEffect = world.get<WorldEffect>(entityId)?.let(::toWorldEffectSnapshot),
            aiTriggerTracker =
                world.get<AiTriggerTracker>(entityId)?.let { tracker ->
                    AiTriggerTrackerSnapshot(
                        consumedTriggerIds = tracker.consumedTriggerIds.sorted(),
                        pendingCombatStartTriggerIds = tracker.pendingCombatStartTriggerIds.sorted(),
                        engagedInCombat = tracker.engagedInCombat,
                    )
                },
            aiPerception =
                world.get<AIPerceptionState>(entityId)?.let { perception ->
                    AIPerceptionSnapshot(
                        lastKnownTargetPosition = perception.lastKnownTargetPosition?.let(PointSnapshot::from),
                    )
                },
            pendingTelegraph =
                world.get<PendingTelegraphState>(entityId)?.let { telegraph ->
                    PendingTelegraphSnapshot(
                        telegraphSpecId = telegraph.telegraphSpecId,
                        sourceAbilityId = telegraph.sourceAbilityId,
                        remainingTurns = telegraph.remainingTurns,
                        targetPoint = PointSnapshot.from(telegraph.targetPoint),
                        queuedAbilityId = telegraph.queuedAbilityId,
                        dangerLevel = telegraph.resolvedDangerLevel.name,
                    )
                },
            bossEncounterState =
                world.get<BossEncounterState>(entityId)?.let { bossState ->
                    BossEncounterStateSnapshot(
                        encounterId = bossState.encounterId,
                        currentPhaseId = bossState.currentPhaseId,
                        encounterTurnCount = bossState.encounterTurnCount,
                        phaseTurnCount = bossState.phaseTurnCount,
                    )
                },
            resourcePools =
                world.get<ResourcePools>(entityId)?.entries
                    ?.values
                    ?.sortedBy { pool -> pool.type.name }
                    ?.map { pool -> ResourcePoolSnapshot(type = pool.type.name, current = pool.current, max = pool.max) }
                    .orEmpty(),
            equilibriumLastAffinity = world.get<EquilibriumState>(entityId)?.lastResolvedAffinity?.name,
            raceTalentPoints = world.get<RaceTalentPointBank>(entityId)?.unspentPoints,
            inscriptionLoadout =
                world.get<InscriptionLoadout>(entityId)?.let { loadout ->
                    InscriptionLoadoutSnapshot(
                        slots = loadout.slots.map { slot -> InscriptionSlotSaveSnapshot(hotkey = slot.hotkey, inscriptionId = slot.inscriptionId) },
                    )
                },
            inscriptionCooldowns = world.get<InscriptionCooldownState>(entityId)?.remainingByInscriptionId?.toMap(),
            talentLoadout =
                world.get<TalentLoadout>(entityId)?.let { loadout ->
                    TalentLoadoutSnapshot(
                        slotToTalentId = loadout.slotToTalentId.toMap(),
                        talentLevels = loadout.talentLevels.toMap(),
                    )
                },
            talentAllocationDraft =
                world.get<TalentAllocationDraft>(entityId)?.let { draft ->
                    TalentAllocationDraftSnapshot(
                        ownerType = draft.ownerType.name,
                        treeOwnerId = draft.treeOwnerId,
                        pendingRanks =
                            draft.pendingRanks.entries
                                .sortedBy { (talentId, _) -> talentId }
                                .associateTo(linkedMapOf()) { (talentId, rank) -> talentId to rank },
                        previousPendingRanks =
                            draft.previousPendingRanks
                                ?.entries
                                ?.sortedBy { (talentId, _) -> talentId }
                                ?.associateTo(linkedMapOf()) { (talentId, rank) -> talentId to rank },
                    )
                },
            itemState = world.get<ItemInstance>(entityId)?.let(::toItemSnapshot),
            isGroundItem = world.get<GroundItem>(entityId) != null,
            isPlayerControlled = world.get<PlayerControlled>(entityId) != null,
            interactableId = world.get<Interactable>(entityId)?.id,
            stair = world.get<Stair>(entityId)?.let { StairSnapshot(direction = it.direction.name) },
        )
    }

    private fun restoreEntity(
        world: World,
        snapshot: EntitySnapshot,
        content: GameContent,
    ) {
        val entityId = EntityId(snapshot.id)
        snapshot.position?.let { point -> world.add(entityId, Position(point.x, point.y)) }
        if (snapshot.blocksMovement) {
            world.add(entityId, BlocksMovement())
        }
        snapshot.faction?.let { faction ->
            world.add(entityId, FactionTag(parseEnumFromSave<Faction>(value = faction, label = "faction")))
        }
        snapshot.stats?.let { stats -> world.add(entityId, toStats(stats)) }
        snapshot.combatProfile?.let { profile -> world.add(entityId, toCombatProfile(profile)) }
        snapshot.healthCurrent?.let { current -> world.add(entityId, Health(current = current, max = current)) }
        snapshot.energyCurrent?.let { current -> world.add(entityId, Energy(current)) }
        snapshot.experience?.let { experience -> world.add(entityId, toExperience(experience)) }
        snapshot.experienceReward?.let { reward -> world.add(entityId, ExperienceReward(reward)) }
        snapshot.aiBehavior?.let { behavior -> world.add(entityId, toAIBehavior(behavior)) }
        snapshot.monsterTemplateId?.let { templateId ->
            world.add(entityId, MonsterTemplateId(normalizeMonsterTemplateId(templateId)))
        }
        snapshot.patrolRoute?.let { route ->
            world.add(entityId, PatrolRoute(route.waypoints.map(PointSnapshot::toPoint), route.nextWaypointIndex))
        }
        snapshot.inventory?.let { inventory ->
            world.add(
                entityId,
                Inventory(
                    capacity = inventory.capacity,
                    itemIds = inventory.itemIds.map(::EntityId).toMutableList(),
                ),
            )
        }
        snapshot.equipment?.let { equipment ->
            val restoredSlots =
                linkedMapOf<EquipSlot, EntityId>().apply {
                    equipment.slots.forEach { (slotName, itemId) ->
                        put(parseEnumFromSave<EquipSlot>(value = slotName, label = "equipment slot"), EntityId(itemId))
                    }
                }
            world.add(
                entityId,
                Equipment(slots = restoredSlots),
            )
        }
        snapshot.cooldowns?.let { cooldowns ->
            world.add(entityId, CooldownState(cooldowns.toMutableMap()))
        }
        snapshot.effects?.let { effects ->
            world.add(
                entityId,
                EffectTracker(
                    effects = effects.map { effect -> restoreActiveEffect(effect, content) }.toMutableList(),
                    ownerId = entityId,
                ),
            )
        }
        snapshot.areaEffectEmitter?.let { emitter ->
            world.add(entityId, restoreAreaEffectEmitter(emitter, content))
        }
        snapshot.worldEffect?.let { effect ->
            world.add(entityId, restoreWorldEffect(effect, content))
        }
        snapshot.aiTriggerTracker?.let { tracker ->
            world.add(
                entityId,
                AiTriggerTracker(
                    consumedTriggerIds = tracker.consumedTriggerIds.toCollection(linkedSetOf()),
                    pendingCombatStartTriggerIds = tracker.pendingCombatStartTriggerIds.toCollection(linkedSetOf()),
                    engagedInCombat = tracker.engagedInCombat,
                ),
            )
        }
        snapshot.aiPerception?.let { perception ->
            world.add(
                entityId,
                AIPerceptionState(
                    lastKnownTargetPosition = perception.lastKnownTargetPosition?.toPoint(),
                ),
            )
        }
        snapshot.pendingTelegraph?.let { telegraph ->
            world.add(
                entityId,
                PendingTelegraphState(
                    telegraphSpecId = telegraph.telegraphSpecId,
                    sourceAbilityId = telegraph.sourceAbilityId,
                    remainingTurns = telegraph.remainingTurns,
                    targetPoint = telegraph.targetPoint.toPoint(),
                    queuedAbilityId = telegraph.queuedAbilityId,
                    resolvedDangerLevel = parseEnumFromSave<DangerLevel>(telegraph.dangerLevel, "telegraph danger level"),
                ),
            )
        }
        snapshot.bossEncounterState?.let { bossState ->
            world.add(
                entityId,
                BossEncounterState(
                    encounterId = bossState.encounterId,
                    currentPhaseId = bossState.currentPhaseId,
                    encounterTurnCount = bossState.encounterTurnCount,
                    phaseTurnCount = bossState.phaseTurnCount,
                ),
            )
        }
        if (snapshot.resourcePools.isNotEmpty()) {
            world.add(
                entityId,
                ResourcePools(
                    entries =
                        snapshot.resourcePools.associateTo(linkedMapOf()) { pool ->
                            val type = ResourceType.fromId(pool.type)
                            type to com.ktome.core.resource.ResourcePool(type = type, current = pool.current, max = pool.max)
                        }.toMutableMap(),
                ),
            )
        }
        snapshot.equilibriumLastAffinity?.let { affinity ->
            world.add(
                entityId,
                EquilibriumState(
                    lastResolvedAffinity = parseEnumFromSave<EquilibriumAffinity>(affinity, "equilibrium affinity"),
                ),
            )
        }
        snapshot.raceTalentPoints?.let { unspentPoints ->
            world.add(entityId, RaceTalentPointBank(unspentPoints = unspentPoints))
        }
        snapshot.inscriptionLoadout?.let { loadout ->
            world.add(
                entityId,
                InscriptionLoadout(
                    slots =
                        loadout.slots
                            .map { slot -> InscriptionSlot(hotkey = slot.hotkey, inscriptionId = slot.inscriptionId) }
                            .toMutableList(),
                ),
            )
        }
        snapshot.inscriptionCooldowns?.let { cooldowns ->
            world.add(entityId, InscriptionCooldownState(cooldowns.toMutableMap()))
        }
        snapshot.talentLoadout?.let { loadout ->
            world.add(
                entityId,
                TalentLoadout(
                    slotToTalentId = loadout.slotToTalentId.toMutableMap(),
                    talentLevels = loadout.talentLevels.toMutableMap(),
                ),
            )
        }
        snapshot.talentAllocationDraft?.let { draft ->
            world.add(
                entityId,
                TalentAllocationDraft(
                    ownerType = parseEnumFromSave<TalentTreeOwnerType>(value = draft.ownerType, label = "talent draft owner type"),
                    treeOwnerId = draft.treeOwnerId,
                    pendingRanks =
                        draft.pendingRanks.entries
                            .sortedBy { (talentId, _) -> talentId }
                            .associateTo(linkedMapOf()) { (talentId, rank) -> talentId to rank },
                    previousPendingRanks =
                        draft.previousPendingRanks
                            ?.entries
                            ?.sortedBy { (talentId, _) -> talentId }
                            ?.associateTo(linkedMapOf()) { (talentId, rank) -> talentId to rank },
                ),
            )
        }
        snapshot.itemState?.let { itemSnapshot ->
            val item = restoreItemInstance(itemSnapshot, content.itemBundle)
            world.add(entityId, item)
            applyItemPresentation(world, entityId, item)
        }
        if (snapshot.isGroundItem) {
            world.add(entityId, GroundItem)
        }
        if (snapshot.isPlayerControlled) {
            world.add(entityId, PlayerControlled)
            applyPlayerPresentation(world, entityId, content.localizer.text("actor.player.name"))
        }
        snapshot.interactableId?.let { interactableId ->
            world.add(entityId, Interactable(interactableId))
            applyInteractablePresentation(world, entityId, interactableId, content)
        }
        snapshot.stair?.let { stair ->
            val direction = parseEnumFromSave<StairDirection>(value = stair.direction, label = "stair direction")
            world.add(entityId, Stair(direction))
            applyStairPresentation(world, entityId, direction, content.localizer)
        }
        snapshot.monsterTemplateId?.let { templateId ->
            applyMonsterPresentation(world, entityId, resolveMonsterTemplate(content, templateId))
        }
    }

    private fun carriedEntityIds(
        world: World,
        playerId: EntityId,
    ): List<EntityId> {
        val inventoryIds = world.get<Inventory>(playerId)?.itemIds.orEmpty()
        val equippedIds = world.get<Equipment>(playerId)?.slots?.values.orEmpty()
        return (inventoryIds + equippedIds).distinctBy(EntityId::value).sortedBy(EntityId::value)
    }

    private fun canonicalizePlayerSnapshot(snapshot: PlayerSnapshot): PlayerSnapshot =
        PlayerSnapshot(
            entity = canonicalizeEntitySnapshot(snapshot.entity),
            carriedEntities = snapshot.carriedEntities.map(::canonicalizeEntitySnapshot).sortedBy(EntitySnapshot::id),
        )

    private fun canonicalizeEntitySnapshot(snapshot: EntitySnapshot): EntitySnapshot {
        val patrolRoute = snapshot.patrolRoute
        val inventory = snapshot.inventory
        val equipment = snapshot.equipment
        val talentLoadout = snapshot.talentLoadout
        val talentAllocationDraft = snapshot.talentAllocationDraft
        val aiTriggerTracker = snapshot.aiTriggerTracker
        val inscriptionLoadout = snapshot.inscriptionLoadout

        return snapshot.copy(
            position = snapshot.position?.copy(),
            stats = snapshot.stats?.copy(),
            combatProfile = snapshot.combatProfile?.copy(),
            experience = snapshot.experience?.copy(),
            aiBehavior = snapshot.aiBehavior?.copy(),
            patrolRoute =
                patrolRoute?.copy(
                    waypoints = patrolRoute.waypoints.map { point -> point.copy() },
                ),
            inventory = inventory?.copy(itemIds = inventory.itemIds.toList()),
            equipment =
                equipment?.copy(
                    slots = equipment.slots.entries.sortedBy { (slot, _) -> slot }.associateTo(linkedMapOf()) { (slot, itemId) -> slot to itemId },
                ),
            cooldowns = snapshot.cooldowns?.entries?.sortedBy { (talentId, _) -> talentId }?.associateTo(linkedMapOf()) { (talentId, turns) -> talentId to turns },
            effects = snapshot.effects?.map(::copyActiveEffectSnapshot)?.sortedBy(ActiveEffectSnapshot::id),
            areaEffectEmitter = snapshot.areaEffectEmitter?.let(::copyAreaEffectEmitterSnapshot),
            worldEffect = snapshot.worldEffect?.let(::copyWorldEffectSnapshot),
            aiTriggerTracker =
                aiTriggerTracker?.copy(
                    consumedTriggerIds = aiTriggerTracker.consumedTriggerIds.sorted(),
                    pendingCombatStartTriggerIds = aiTriggerTracker.pendingCombatStartTriggerIds.sorted(),
                ),
            aiPerception =
                snapshot.aiPerception?.let { perception ->
                    perception.copy(
                        lastKnownTargetPosition = perception.lastKnownTargetPosition?.copy(),
                    )
                },
            pendingTelegraph =
                snapshot.pendingTelegraph?.let { telegraph ->
                    telegraph.copy(
                        targetPoint = telegraph.targetPoint.copy(),
                    )
                },
            bossEncounterState = snapshot.bossEncounterState?.copy(),
            resourcePools = snapshot.resourcePools.sortedBy(ResourcePoolSnapshot::type),
            inscriptionLoadout =
                inscriptionLoadout?.copy(
                    slots =
                        inscriptionLoadout.slots
                            .sortedBy(InscriptionSlotSaveSnapshot::hotkey)
                            .map { slot -> slot.copy() },
                ),
            inscriptionCooldowns =
                snapshot.inscriptionCooldowns
                    ?.entries
                    ?.sortedBy { (inscriptionId, _) -> inscriptionId }
                    ?.associateTo(linkedMapOf()) { (inscriptionId, turns) -> inscriptionId to turns },
            talentLoadout =
                talentLoadout?.copy(
                    slotToTalentId =
                        talentLoadout.slotToTalentId.entries
                            .sortedBy { (slot, _) -> slot }
                            .associateTo(linkedMapOf()) { (slot, talentId) -> slot to talentId },
                    talentLevels =
                        talentLoadout.talentLevels.entries
                            .sortedBy { (talentId, _) -> talentId }
                            .associateTo(linkedMapOf()) { (talentId, level) -> talentId to level },
                ),
            talentAllocationDraft =
                talentAllocationDraft?.copy(
                    pendingRanks =
                        talentAllocationDraft.pendingRanks.entries
                            .sortedBy { (talentId, _) -> talentId }
                            .associateTo(linkedMapOf()) { (talentId, rank) -> talentId to rank },
                    previousPendingRanks =
                        talentAllocationDraft.previousPendingRanks
                            ?.entries
                            ?.sortedBy { (talentId, _) -> talentId }
                            ?.associateTo(linkedMapOf()) { (talentId, rank) -> talentId to rank },
                ),
            itemState = snapshot.itemState?.let(::canonicalizeItemSnapshot),
            stair = snapshot.stair?.copy(),
        )
    }

    private fun canonicalizeItemSnapshot(snapshot: ItemSnapshot): ItemSnapshot =
        snapshot.copy(
            affixIds = snapshot.affixIds.sorted(),
            stats = snapshot.stats.copy(),
        )

    private fun copyActiveEffectSnapshot(snapshot: ActiveEffectSnapshot): ActiveEffectSnapshot =
        snapshot.copy(
            statModifiers = snapshot.statModifiers.copy(),
        )

    private fun copyAreaEffectEmitterSnapshot(snapshot: AreaEffectEmitterSnapshot): AreaEffectEmitterSnapshot =
        snapshot.copy(
            affectedActorIds = snapshot.affectedActorIds.sorted(),
            effects = snapshot.effects.map(::copyActiveEffectSnapshot).sortedBy(ActiveEffectSnapshot::id),
        )

    private fun copyWorldEffectSnapshot(snapshot: WorldEffectSnapshot): WorldEffectSnapshot =
        snapshot.copy(
            affectedActorIds = snapshot.affectedActorIds.sorted(),
            effects = snapshot.effects.map(::copyActiveEffectSnapshot).sortedBy(ActiveEffectSnapshot::id),
        )

    private fun toStatsSnapshot(stats: Stats): StatsSnapshot =
        StatsSnapshot(
            str = stats.str,
            dex = stats.dex,
            con = stats.con,
            wil = stats.wil,
        )

    private fun toStats(snapshot: StatsSnapshot): Stats =
        Stats(
            str = snapshot.str,
            dex = snapshot.dex,
            con = snapshot.con,
            wil = snapshot.wil,
        )

    private fun toCombatProfileSnapshot(profile: CombatProfile): CombatProfileSnapshot =
        CombatProfileSnapshot(
            baseAttack = profile.baseAttack,
            baseDefense = profile.baseDefense,
            baseAccuracy = profile.baseAccuracy,
            baseEvasion = profile.baseEvasion,
            baseSpeed = profile.baseSpeed,
            baseHp = profile.baseHp,
            baseStamina = profile.baseStamina,
            baseHpRegen = profile.baseHpRegen,
        )

    private fun toCombatProfile(snapshot: CombatProfileSnapshot): CombatProfile =
        CombatProfile(
            baseAttack = snapshot.baseAttack,
            baseDefense = snapshot.baseDefense,
            baseAccuracy = snapshot.baseAccuracy,
            baseEvasion = snapshot.baseEvasion,
            baseSpeed = snapshot.baseSpeed,
            baseHp = snapshot.baseHp,
            baseStamina = snapshot.baseStamina,
            baseHpRegen = snapshot.baseHpRegen,
        )

    private fun toExperienceSnapshot(experience: Experience): ExperienceSnapshot =
        ExperienceSnapshot(
            current = experience.current,
            level = experience.level,
            unspentStatPoints = experience.unspentStatPoints,
            unspentTalentPoints = experience.unspentTalentPoints,
        )

    private fun toExperience(snapshot: ExperienceSnapshot): Experience =
        Experience(
            current = snapshot.current,
            level = snapshot.level,
            unspentStatPoints = snapshot.unspentStatPoints,
            unspentTalentPoints = snapshot.unspentTalentPoints,
        )

    private fun toAIBehaviorSnapshot(behavior: AIBehavior): AIBehaviorSnapshot =
        AIBehaviorSnapshot(
            type = behavior.type.name,
            sightRadius = behavior.sightRadius,
            preferredRangeStart = behavior.preferredRangeStart,
            preferredRangeEnd = behavior.preferredRangeEnd,
        )

    private fun toAIBehavior(snapshot: AIBehaviorSnapshot): AIBehavior =
        AIBehavior(
            type = parseEnumFromSave<AIType>(value = snapshot.type, label = "AI behavior type"),
            sightRadius = snapshot.sightRadius,
            preferredRangeStart = snapshot.preferredRangeStart,
            preferredRangeEnd = snapshot.preferredRangeEnd,
        )

    private fun toActiveEffectSnapshot(effect: ActiveEffect): ActiveEffectSnapshot =
        ActiveEffectSnapshot(
            id = effect.id,
            type = effect.schemaId,
            remainingTurns = effect.remainingTurns,
            statModifiers = toStatModifierSnapshot(effect.statModifiers),
            skipNextDecay = effect.skipNextDecay,
            stackCount = effect.stackCount,
            appliedTurn = effect.appliedTurn,
            sourceEntityId = effect.sourceEntityId?.value,
            magnitude = effect.magnitude,
        )

    private fun toAreaEffectEmitterSnapshot(emitter: AreaEffectEmitter): AreaEffectEmitterSnapshot =
        AreaEffectEmitterSnapshot(
            emitterId = emitter.emitterId,
            sourceEntityId = emitter.sourceEntityId?.value,
            affectedActorIds = emitter.affectedActorIds.map(EntityId::value).sorted(),
            emitterPriority = emitter.emitterPriority,
            effects = emitter.effects.map(::toActiveEffectSnapshot).sortedBy(ActiveEffectSnapshot::id),
        )

    private fun toWorldEffectSnapshot(effect: WorldEffect): WorldEffectSnapshot =
        WorldEffectSnapshot(
            effectId = effect.effectId,
            affectedActorIds = effect.affectedActorIds.map(EntityId::value).sorted(),
            worldPriority = effect.worldPriority,
            effects = effect.effects.map(::toActiveEffectSnapshot).sortedBy(ActiveEffectSnapshot::id),
        )

    private fun restoreActiveEffect(
        snapshot: ActiveEffectSnapshot,
        content: GameContent,
    ): ActiveEffect {
        val definition =
            content.statusCatalog.definitionOrNull(snapshot.type)
                ?: StatusDefinitions.definitionForSchemaId(snapshot.type)
                ?: StatusDefinitions.definitionFor(StatusEffectType.fromSchemaId(snapshot.type))
        return StatusLifecycle.createInstance(
            definition = definition,
            effectId = snapshot.id,
            duration = snapshot.remainingTurns,
            magnitude = snapshot.magnitude,
            sourceEntityId = snapshot.sourceEntityId?.let(::EntityId),
            appliedTurn = snapshot.appliedTurn,
            skipNextDecay = snapshot.skipNextDecay,
            statModifierOverride = toStatModifier(snapshot.statModifiers),
        ).also { effect ->
            effect.stackCount = snapshot.stackCount
        }
    }

    private fun restoreAreaEffectEmitter(
        snapshot: AreaEffectEmitterSnapshot,
        content: GameContent,
    ): AreaEffectEmitter =
        AreaEffectEmitter(
            emitterId = snapshot.emitterId,
            sourceEntityId = snapshot.sourceEntityId?.let(::EntityId),
            affectedActorIds = snapshot.affectedActorIds.map(::EntityId).toSet(),
            emitterPriority = snapshot.emitterPriority,
            effects = snapshot.effects.map { effect -> restoreActiveEffect(effect, content) }.toMutableList(),
        )

    private fun restoreWorldEffect(
        snapshot: WorldEffectSnapshot,
        content: GameContent,
    ): WorldEffect =
        WorldEffect(
            effectId = snapshot.effectId,
            affectedActorIds = snapshot.affectedActorIds.map(::EntityId).toSet(),
            worldPriority = snapshot.worldPriority,
            effects = snapshot.effects.map { effect -> restoreActiveEffect(effect, content) }.toMutableList(),
        )

    private fun toItemSnapshot(item: ItemInstance): ItemSnapshot =
        ItemSnapshot(
            baseId = item.baseId,
            type = item.type.name,
            slot = item.slot?.name,
            quality = item.quality.name,
            materialId = item.materialId,
            affixIds = item.affixes.map { affix -> affix.id },
            stats = toStatModifierSnapshot(item.stats),
            effect = item.effect?.name,
            magnitude = item.magnitude,
        )

    private fun restoreItemInstance(
        snapshot: ItemSnapshot,
        bundle: ItemDataBundle,
    ): ItemInstance {
        val base = resolveBaseItem(bundle, snapshot.baseId)
        val material = snapshot.materialId?.let { materialId -> resolveMaterial(bundle, materialId) }
        val affixes = snapshot.affixIds.map { affixId -> resolveAffix(bundle, affixId) }

        return ItemInstance(
            baseId = snapshot.baseId,
            name = buildItemName(base, material, affixes),
            type = parseEnumFromSave<ItemType>(value = snapshot.type, label = "item type"),
            slot = snapshot.slot?.let { slot -> parseEnumFromSave<EquipSlot>(value = slot, label = "item slot") } ?: base.slot,
            glyph = base.glyph,
            colorHex = base.colorHex,
            quality = parseEnumFromSave<ItemQuality>(value = snapshot.quality, label = "item quality"),
            materialId = snapshot.materialId,
            materialName = material?.name,
            affixes = affixes,
            stats = toStatModifier(snapshot.stats),
            effect =
                snapshot.effect?.let { effect ->
                    parseEnumFromSave<ConsumableEffect>(value = effect, label = "consumable effect")
                } ?: base.effect,
            resourceTypeId = base.resourceTypeId,
            magnitude = snapshot.magnitude,
            passive = base.passive,
        )
    }

    private fun resolveBaseItem(
        bundle: ItemDataBundle,
        baseId: String,
    ): ItemBaseDef =
        requireNotNull(bundle.baseItems.firstOrNull { item -> item.id == baseId }) {
            throw SaveRestoreException("Save references unknown item base '$baseId'.")
        }

    private fun resolveMaterial(
        bundle: ItemDataBundle,
        materialId: String,
    ): MaterialDef =
        requireNotNull(bundle.materials.firstOrNull { material -> material.id == materialId }) {
            throw SaveRestoreException("Save references unknown item material '$materialId'.")
        }

    private fun resolveAffix(
        bundle: ItemDataBundle,
        affixId: String,
    ): com.ktome.core.item.AffixDef =
        requireNotNull(bundle.affixes.firstOrNull { affix -> affix.id == affixId }) {
            throw SaveRestoreException("Save references unknown item affix '$affixId'.")
        }

    private fun buildItemName(
        base: ItemBaseDef,
        material: MaterialDef?,
        affixes: List<com.ktome.core.item.AffixDef>,
    ): String {
        val prefixes = affixes.filter { affix -> affix.type == AffixType.PREFIX }.joinToString(" ") { affix -> affix.name }.trim()
        val suffixes = affixes.filter { affix -> affix.type == AffixType.SUFFIX }.joinToString(" ") { affix -> affix.name }.trim()
        return buildString {
            if (prefixes.isNotBlank()) {
                append(prefixes)
                append(' ')
            }
            material?.name?.let {
                append(it)
                append(' ')
            }
            append(base.name)
            if (suffixes.isNotBlank()) {
                append(' ')
                append(suffixes)
            }
        }
    }

    private fun toStatModifierSnapshot(modifier: StatModifier): StatModifierSnapshot =
        StatModifierSnapshot(
            str = modifier.str,
            dex = modifier.dex,
            con = modifier.con,
            wil = modifier.wil,
            attack = modifier.attack,
            defense = modifier.defense,
            accuracy = modifier.accuracy,
            evasion = modifier.evasion,
            speed = modifier.speed,
            maxHp = modifier.maxHp,
            maxStamina = modifier.maxStamina,
            hpRegen = modifier.hpRegen,
            staminaRegen = modifier.staminaRegen,
            critChance = modifier.critChance,
            talentPower = modifier.talentPower,
            attackMultiplierBonus = modifier.attackMultiplierBonus,
            defenseMultiplierBonus = modifier.defenseMultiplierBonus,
        )

    private fun toStatModifier(snapshot: StatModifierSnapshot): StatModifier =
        StatModifier(
            str = snapshot.str,
            dex = snapshot.dex,
            con = snapshot.con,
            wil = snapshot.wil,
            attack = snapshot.attack,
            defense = snapshot.defense,
            accuracy = snapshot.accuracy,
            evasion = snapshot.evasion,
            speed = snapshot.speed,
            maxHp = snapshot.maxHp,
            maxStamina = snapshot.maxStamina,
            hpRegen = snapshot.hpRegen,
            staminaRegen = snapshot.staminaRegen,
            critChance = snapshot.critChance,
            talentPower = snapshot.talentPower,
            attackMultiplierBonus = snapshot.attackMultiplierBonus,
            defenseMultiplierBonus = snapshot.defenseMultiplierBonus,
        )

    private fun applyPlayerPresentation(
        world: World,
        entityId: EntityId,
        playerName: String,
    ) {
        world.add(entityId, Glyph(HERO_GLYPH))
        world.add(entityId, DisplayColor(HERO_COLOR_HEX))
        world.add(entityId, Name(playerName))
    }

    private fun applyMonsterPresentation(
        world: World,
        entityId: EntityId,
        template: MonsterTemplate,
    ) {
        world.add(entityId, Glyph(template.glyph))
        world.add(entityId, DisplayColor(template.colorHex))
        world.add(entityId, Name(template.name))
        if (template.resistances.isNotEmpty()) {
            world.add(
                entityId,
                ResistanceProfile(
                    values = template.resistances.entries.associateTo(linkedMapOf()) { (type, value) -> type to value },
                ),
            )
        }
    }

    private fun applyItemPresentation(
        world: World,
        entityId: EntityId,
        item: ItemInstance,
    ) {
        world.add(entityId, Glyph(item.glyph))
        world.add(entityId, DisplayColor(item.colorHex))
        world.add(entityId, Name(item.name))
    }

    private fun applyStairPresentation(
        world: World,
        entityId: EntityId,
        direction: StairDirection,
        localizer: com.ktome.game.i18n.Localizer,
    ) {
        world.add(entityId, Glyph(if (direction == StairDirection.DOWN) '>' else '<'))
        world.add(entityId, DisplayColor(STAIR_COLOR_HEX))
        world.add(
            entityId,
            Name(
                when (direction) {
                    StairDirection.DOWN -> localizer.text("stairs.down.name")
                    StairDirection.UP -> localizer.text("stairs.up.name")
                },
            ),
        )
    }

    private fun applyInteractablePresentation(
        world: World,
        entityId: EntityId,
        interactableId: String,
        content: GameContent,
    ) {
        val schema =
            requireNotNull(content.schemaCatalog.interactables.firstOrNull { interactable -> interactable.id == interactableId }) {
                "Unknown interactable '$interactableId'."
            }
        val glyph =
            when (interactableId) {
                "armory_gate" -> '+'
                "alarm_bonfire" -> '^'
                else -> '&'
            }
        val colorHex =
            when (interactableId) {
                "armory_gate" -> "#C7B48A"
                "alarm_bonfire" -> "#FF8A3D"
                else -> "#D6C977"
            }
        world.add(entityId, Glyph(glyph))
        world.add(entityId, DisplayColor(colorHex))
        world.add(entityId, Name(content.localizer.text(schema.nameKey)))
    }

    private fun resolveMonsterTemplate(
        content: GameContent,
        templateId: String,
    ): MonsterTemplate =
        requireNotNull(
            content.allMonsterTemplates().firstOrNull { template ->
                template.id == normalizeMonsterTemplateId(templateId)
            },
        ) {
            throw SaveRestoreException("Save references unknown monster template '$templateId'.")
        }

    private fun normalizeMonsterTemplateId(templateId: String): String = legacyMonsterTemplateIds[templateId] ?: templateId

    private inline fun <reified T : Enum<T>> parseEnumFromSave(
        value: String,
        label: String,
    ): T =
        try {
            enumValueOf<T>(value)
        } catch (exception: IllegalArgumentException) {
            throw SaveRestoreException("Save references unknown $label '$value'.", exception)
        }

    private fun Iterable<Point>.sortedCanonicalPoints(): List<Point> =
        sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
}
