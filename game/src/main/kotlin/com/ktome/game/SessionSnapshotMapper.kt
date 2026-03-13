package com.ktome.game

import com.ktome.core.dungeon.FloorState
import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Energy
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.ExperienceReward
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.PlayerControlled
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.item.Equipment
import com.ktome.core.item.GroundItem
import com.ktome.core.item.Inventory
import com.ktome.core.item.ItemInstance
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.EquipmentSnapshot
import com.ktome.core.save.FloorSnapshot
import com.ktome.core.save.InventorySnapshot
import com.ktome.core.save.MapSnapshot
import com.ktome.core.save.PatrolRouteSnapshot
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.StairSnapshot
import com.ktome.core.save.TalentLoadoutSnapshot
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.ActiveEffect
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.TalentLoadout

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
    val messageLog: List<String>,
    val player: PlayerSnapshot,
    val floors: List<FloorState<FloorRuntimeState>>,
    val combatRandomState: Long? = null,
    val sessionRandomState: Long? = null,
    val pendingActionIds: List<Int> = emptyList(),
    val activeTurnActorId: Int? = null,
)

internal object SessionSnapshotMapper {
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
            restoreEntity(world, snapshot)
        }
        snapshotById.forEach { (entityId, snapshot) ->
            val entity = EntityId(entityId)
            if (world.get<Stats>(entity) != null && world.get<CombatProfile>(entity) != null) {
                StatsCalculator.recalculateAndStore(world, entity)
            }
            world.get<Health>(entity)?.let { health ->
                snapshot.healthCurrent?.let { health.current = it.coerceIn(0, health.max) }
            }
            world.get<Stamina>(entity)?.let { stamina ->
                snapshot.staminaCurrent?.let { stamina.current = it.coerceIn(0, stamina.max) }
            }
        }
        return world
    }

    fun toSaveSnapshot(
        config: FoundationGameConfig,
        currentFloor: Int,
        turnCount: Int,
        messageLog: List<String>,
        player: PlayerSnapshot,
        floors: List<FloorState<FloorRuntimeState>>,
        combatRandomState: Long?,
        sessionRandomState: Long?,
        pendingActionIds: List<Int>,
        activeTurnActorId: Int?,
    ): SaveSnapshot =
        SaveSnapshot(
            timestampEpochMillis = System.currentTimeMillis(),
            seed = config.seed,
            mapWidth = config.width,
            mapHeight = config.height,
            fovRadius = config.fovRadius,
            messageLogSize = config.messageLogSize,
            currentFloor = currentFloor,
            maxFloor = config.maxFloor,
            turnCount = turnCount,
            messageLog = messageLog,
            player = copyPlayerSnapshot(player),
            combatRandomState = combatRandomState,
            sessionRandomState = sessionRandomState,
            pendingActionIds = pendingActionIds,
            activeTurnActorId = activeTurnActorId,
            floors =
                floors.sortedBy(FloorState<FloorRuntimeState>::floor).map { floorState ->
                    FloorSnapshot(
                        floor = floorState.floor,
                        map = MapSnapshot(rows = floorState.payload.map.asGlyphRows(), playerStart = floorState.payload.map.playerStart),
                        stairsUp = floorState.stairsUp,
                        stairsDown = floorState.stairsDown,
                        exploredTiles = floorState.payload.exploredTiles.toList(),
                        entities = floorState.payload.entities.map(::copyEntitySnapshot),
                    )
                },
        )

    fun fromSaveSnapshot(snapshot: SaveSnapshot): RestoredRunState =
        RestoredRunState(
            config =
                FoundationGameConfig(
                    width = snapshot.mapWidth,
                    height = snapshot.mapHeight,
                    seed = snapshot.seed,
                    fovRadius = snapshot.fovRadius,
                    floor = snapshot.currentFloor,
                    maxFloor = snapshot.maxFloor,
                    messageLogSize = snapshot.messageLogSize,
                ),
            currentFloor = snapshot.currentFloor,
            turnCount = snapshot.turnCount,
            messageLog = snapshot.messageLog.toList(),
            player = copyPlayerSnapshot(snapshot.player),
            combatRandomState = snapshot.combatRandomState,
            sessionRandomState = snapshot.sessionRandomState,
            pendingActionIds = snapshot.pendingActionIds.toList(),
            activeTurnActorId = snapshot.activeTurnActorId,
            floors =
                snapshot.floors.map { floor ->
                    FloorState(
                        floor = floor.floor,
                        stairsUp = floor.stairsUp,
                        stairsDown = floor.stairsDown,
                        payload =
                            FloorRuntimeState(
                                map = GameMap.fromAscii(rows = floor.map.rows, playerStart = floor.map.playerStart),
                                stairsUp = floor.stairsUp,
                                stairsDown = floor.stairsDown,
                                exploredTiles = linkedSetOf<Point>().apply { addAll(floor.exploredTiles) },
                                entities = floor.entities.map(::copyEntitySnapshot).toMutableList(),
                            ),
                    )
                },
        )

    private fun captureEntity(
        world: World,
        entityId: EntityId,
    ): EntitySnapshot =
        EntitySnapshot(
            id = entityId.value,
            position = world.get<Position>(entityId)?.toPoint(),
            glyph = world.get<Glyph>(entityId)?.value,
            colorHex = world.get<DisplayColor>(entityId)?.hex,
            name = world.get<Name>(entityId)?.value,
            blocksMovement = world.get<BlocksMovement>(entityId)?.value == true,
            faction = world.get<FactionTag>(entityId)?.value,
            stats = world.get<Stats>(entityId)?.copy(),
            combatProfile = world.get<CombatProfile>(entityId)?.copy(),
            healthCurrent = world.get<Health>(entityId)?.current,
            staminaCurrent = world.get<Stamina>(entityId)?.current,
            energyCurrent = world.get<Energy>(entityId)?.current,
            experience = world.get<Experience>(entityId)?.copy(),
            experienceReward = world.get<ExperienceReward>(entityId)?.value,
            aiBehavior = world.get<AIBehavior>(entityId)?.copy(),
            monsterTemplateId = world.get<MonsterTemplateId>(entityId)?.value,
            patrolRoute =
                world.get<PatrolRoute>(entityId)?.let { route ->
                    PatrolRouteSnapshot(waypoints = route.waypoints.toList(), nextWaypointIndex = route.nextWaypointIndex)
                },
            inventory =
                world.get<Inventory>(entityId)?.let { inventory ->
                    InventorySnapshot(capacity = inventory.capacity, itemIds = inventory.itemIds.map(EntityId::value))
                },
            equipment =
                world.get<Equipment>(entityId)?.let { equipment ->
                    EquipmentSnapshot(slots = equipment.slots.mapValues { (_, itemId) -> itemId.value })
                },
            cooldowns = world.get<com.ktome.core.talent.CooldownState>(entityId)?.remainingByTalentId?.toMap(),
            effects = world.get<EffectTracker>(entityId)?.effects?.map(::copyActiveEffect),
            talentLoadout =
                world.get<TalentLoadout>(entityId)?.let { loadout ->
                    TalentLoadoutSnapshot(
                        slotToTalentId = loadout.slotToTalentId.toMap(),
                        talentLevels = loadout.talentLevels.toMap(),
                    )
                },
            itemInstance = world.get<ItemInstance>(entityId)?.let(::copyItemInstance),
            isGroundItem = world.get<GroundItem>(entityId) != null,
            isPlayerControlled = world.get<PlayerControlled>(entityId) != null,
            stair = world.get<Stair>(entityId)?.let { StairSnapshot(it.direction) },
        )

    private fun restoreEntity(
        world: World,
        snapshot: EntitySnapshot,
    ) {
        val entityId = EntityId(snapshot.id)
        snapshot.position?.let { point -> world.add(entityId, Position(point.x, point.y)) }
        snapshot.glyph?.let { glyph -> world.add(entityId, Glyph(glyph)) }
        snapshot.colorHex?.let { colorHex -> world.add(entityId, DisplayColor(colorHex)) }
        snapshot.name?.let { name -> world.add(entityId, Name(name)) }
        if (snapshot.blocksMovement) {
            world.add(entityId, BlocksMovement())
        }
        snapshot.faction?.let { faction -> world.add(entityId, FactionTag(faction)) }
        snapshot.stats?.let { stats -> world.add(entityId, stats.copy()) }
        snapshot.combatProfile?.let { profile -> world.add(entityId, profile.copy()) }
        snapshot.healthCurrent?.let { current -> world.add(entityId, Health(current = current, max = current)) }
        snapshot.staminaCurrent?.let { current -> world.add(entityId, Stamina(current = current, max = current)) }
        snapshot.energyCurrent?.let { current -> world.add(entityId, Energy(current)) }
        snapshot.experience?.let { experience -> world.add(entityId, experience.copy()) }
        snapshot.experienceReward?.let { reward -> world.add(entityId, ExperienceReward(reward)) }
        snapshot.aiBehavior?.let { behavior -> world.add(entityId, behavior.copy()) }
        snapshot.monsterTemplateId?.let { templateId -> world.add(entityId, MonsterTemplateId(templateId)) }
        snapshot.patrolRoute?.let { route ->
            world.add(entityId, PatrolRoute(route.waypoints.toList(), route.nextWaypointIndex))
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
            world.add(
                entityId,
                Equipment(
                    slots = equipment.slots.mapValuesTo(linkedMapOf()) { (_, itemId) -> EntityId(itemId) },
                ),
            )
        }
        snapshot.cooldowns?.let { cooldowns ->
            world.add(entityId, com.ktome.core.talent.CooldownState(cooldowns.toMutableMap()))
        }
        snapshot.effects?.let { effects ->
            world.add(entityId, EffectTracker(effects.map(::copyActiveEffect).toMutableList()))
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
        snapshot.itemInstance?.let { item -> world.add(entityId, copyItemInstance(item)) }
        if (snapshot.isGroundItem) {
            world.add(entityId, GroundItem)
        }
        if (snapshot.isPlayerControlled) {
            world.add(entityId, PlayerControlled)
        }
        snapshot.stair?.let { stair -> world.add(entityId, Stair(stair.direction)) }
    }

    private fun carriedEntityIds(
        world: World,
        playerId: EntityId,
    ): List<EntityId> {
        val inventoryIds = world.get<Inventory>(playerId)?.itemIds.orEmpty()
        val equippedIds = world.get<Equipment>(playerId)?.slots?.values.orEmpty()
        return (inventoryIds + equippedIds).distinctBy(EntityId::value)
    }

    private fun copyPlayerSnapshot(snapshot: PlayerSnapshot): PlayerSnapshot =
        PlayerSnapshot(
            entity = copyEntitySnapshot(snapshot.entity),
            carriedEntities = snapshot.carriedEntities.map(::copyEntitySnapshot),
        )

    private fun copyEntitySnapshot(snapshot: EntitySnapshot): EntitySnapshot {
        val patrolRoute = snapshot.patrolRoute
        val inventory = snapshot.inventory
        val equipment = snapshot.equipment
        val talentLoadout = snapshot.talentLoadout

        return snapshot.copy(
            position = snapshot.position?.copy(),
            stats = snapshot.stats?.copy(),
            combatProfile = snapshot.combatProfile?.copy(),
            experience = snapshot.experience?.copy(),
            aiBehavior = snapshot.aiBehavior?.copy(),
            patrolRoute = patrolRoute?.copy(waypoints = patrolRoute.waypoints.map(Point::copy)),
            inventory = inventory?.copy(itemIds = inventory.itemIds.toList()),
            equipment = equipment?.copy(slots = linkedMapOf<com.ktome.core.item.EquipSlot, Int>().apply { putAll(equipment.slots) }),
            cooldowns = snapshot.cooldowns?.let { linkedMapOf<String, Int>().apply { putAll(it) } },
            effects = snapshot.effects?.map(::copyActiveEffect),
            talentLoadout =
                talentLoadout?.copy(
                    slotToTalentId = linkedMapOf<Int, String>().apply { putAll(talentLoadout.slotToTalentId) },
                    talentLevels = linkedMapOf<String, Int>().apply { putAll(talentLoadout.talentLevels) },
                ),
            itemInstance = snapshot.itemInstance?.let(::copyItemInstance),
        )
    }

    private fun copyItemInstance(item: ItemInstance): ItemInstance =
        item.copy(
            affixes = item.affixes.map { affix -> affix.copy(statModifiers = affix.statModifiers.copy()) },
            stats = item.stats.copy(),
        )

    private fun copyActiveEffect(effect: ActiveEffect): ActiveEffect =
        effect.copy(
            statModifiers = effect.statModifiers.copy(),
        )
}
