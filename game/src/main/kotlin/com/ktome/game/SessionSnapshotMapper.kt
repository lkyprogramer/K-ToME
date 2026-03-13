package com.ktome.game

import com.ktome.core.dungeon.FloorState
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.AIBehavior
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
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.PlayerControlled
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
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
import com.ktome.core.save.AIBehaviorSnapshot
import com.ktome.core.save.ActiveEffectSnapshot
import com.ktome.core.save.CombatProfileSnapshot
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.EquipmentSnapshot
import com.ktome.core.save.ExperienceSnapshot
import com.ktome.core.save.FloorSnapshot
import com.ktome.core.save.InventorySnapshot
import com.ktome.core.save.InvalidSaveException
import com.ktome.core.save.ItemSnapshot
import com.ktome.core.save.MapSnapshot
import com.ktome.core.save.PatrolRouteSnapshot
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.SaveRestoreException
import com.ktome.core.save.StairSnapshot
import com.ktome.core.save.StatModifierSnapshot
import com.ktome.core.save.StatsSnapshot
import com.ktome.core.save.TalentLoadoutSnapshot
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.ActiveEffect
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.StatusEffectType
import com.ktome.core.talent.TalentLoadout
import com.ktome.game.model.MonsterTemplate

private const val HERO_GLYPH: Char = '@'
private const val HERO_COLOR_HEX: String = "#FFD700"
private const val HERO_NAME: String = "Hero"
private const val FOUNDATION_ZONE_ID: String = "foundation_dungeon"
private const val FOUNDATION_PROFESSION_ID: String = "foundation_hero"
private const val DOWNSTAIRS_NAME: String = "Downstairs"
private const val UPSTAIRS_NAME: String = "Upstairs"
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
            currentZoneId = FOUNDATION_ZONE_ID,
            floorIndex = currentFloor,
            mapWidth = config.width,
            mapHeight = config.height,
            fovRadius = config.fovRadius,
            messageLogSize = config.messageLogSize,
            playerProfessionId = FOUNDATION_PROFESSION_ID,
            maxFloor = config.maxFloor,
            turnCount = turnCount,
            player = copyPlayerSnapshot(player),
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
                        exploredTiles = floorState.payload.exploredTiles.map(PointSnapshot::from),
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
                    seed = snapshot.worldSeed,
                    fovRadius = snapshot.fovRadius,
                    floor = snapshot.floorIndex,
                    maxFloor = snapshot.maxFloor,
                    messageLogSize = snapshot.messageLogSize,
                ),
            currentFloor = snapshot.floorIndex,
            turnCount = snapshot.turnCount,
            player = copyPlayerSnapshot(snapshot.player),
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
            position = world.get<Position>(entityId)?.toPoint()?.let(PointSnapshot::from),
            blocksMovement = world.get<BlocksMovement>(entityId)?.value == true,
            faction = world.get<FactionTag>(entityId)?.value?.name,
            stats = world.get<Stats>(entityId)?.let(::toStatsSnapshot),
            combatProfile = world.get<CombatProfile>(entityId)?.let(::toCombatProfileSnapshot),
            healthCurrent = world.get<Health>(entityId)?.current,
            staminaCurrent = world.get<Stamina>(entityId)?.current,
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
            talentLoadout =
                world.get<TalentLoadout>(entityId)?.let { loadout ->
                    TalentLoadoutSnapshot(
                        slotToTalentId = loadout.slotToTalentId.toMap(),
                        talentLevels = loadout.talentLevels.toMap(),
                    )
                },
            itemState = world.get<ItemInstance>(entityId)?.let(::toItemSnapshot),
            isGroundItem = world.get<GroundItem>(entityId) != null,
            isPlayerControlled = world.get<PlayerControlled>(entityId) != null,
            stair = world.get<Stair>(entityId)?.let { StairSnapshot(direction = it.direction.name) },
        )

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
        snapshot.faction?.let { faction -> world.add(entityId, FactionTag(Faction.valueOf(faction))) }
        snapshot.stats?.let { stats -> world.add(entityId, toStats(stats)) }
        snapshot.combatProfile?.let { profile -> world.add(entityId, toCombatProfile(profile)) }
        snapshot.healthCurrent?.let { current -> world.add(entityId, Health(current = current, max = current)) }
        snapshot.staminaCurrent?.let { current -> world.add(entityId, Stamina(current = current, max = current)) }
        snapshot.energyCurrent?.let { current -> world.add(entityId, Energy(current)) }
        snapshot.experience?.let { experience -> world.add(entityId, toExperience(experience)) }
        snapshot.experienceReward?.let { reward -> world.add(entityId, ExperienceReward(reward)) }
        snapshot.aiBehavior?.let { behavior -> world.add(entityId, toAIBehavior(behavior)) }
        snapshot.monsterTemplateId?.let { templateId -> world.add(entityId, MonsterTemplateId(templateId)) }
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
                        put(EquipSlot.valueOf(slotName), EntityId(itemId))
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
            world.add(entityId, EffectTracker(effects.map(::restoreActiveEffect).toMutableList()))
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
            applyPlayerPresentation(world, entityId)
        }
        snapshot.stair?.let { stair ->
            val direction = StairDirection.valueOf(stair.direction)
            world.add(entityId, Stair(direction))
            applyStairPresentation(world, entityId, direction)
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
            patrolRoute =
                patrolRoute?.copy(
                    waypoints = patrolRoute.waypoints.map { point -> point.copy() },
                ),
            inventory = inventory?.copy(itemIds = inventory.itemIds.toList()),
            equipment =
                equipment?.copy(
                    slots = linkedMapOf<String, Int>().apply { putAll(equipment.slots) },
                ),
            cooldowns = snapshot.cooldowns?.let { linkedMapOf<String, Int>().apply { putAll(it) } },
            effects = snapshot.effects?.map(::copyActiveEffectSnapshot),
            talentLoadout =
                talentLoadout?.copy(
                    slotToTalentId = linkedMapOf<Int, String>().apply { putAll(talentLoadout.slotToTalentId) },
                    talentLevels = linkedMapOf<String, Int>().apply { putAll(talentLoadout.talentLevels) },
                ),
            itemState = snapshot.itemState?.let(::copyItemSnapshot),
            stair = snapshot.stair?.copy(),
        )
    }

    private fun copyItemSnapshot(snapshot: ItemSnapshot): ItemSnapshot =
        snapshot.copy(
            affixIds = snapshot.affixIds.toList(),
            stats = snapshot.stats.copy(),
        )

    private fun copyActiveEffectSnapshot(snapshot: ActiveEffectSnapshot): ActiveEffectSnapshot =
        snapshot.copy(
            statModifiers = snapshot.statModifiers.copy(),
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
            type = com.ktome.core.ecs.AIType.valueOf(snapshot.type),
            sightRadius = snapshot.sightRadius,
            preferredRangeStart = snapshot.preferredRangeStart,
            preferredRangeEnd = snapshot.preferredRangeEnd,
        )

    private fun toActiveEffectSnapshot(effect: ActiveEffect): ActiveEffectSnapshot =
        ActiveEffectSnapshot(
            id = effect.id,
            type = effect.type.name,
            remainingTurns = effect.remainingTurns,
            statModifiers = toStatModifierSnapshot(effect.statModifiers),
            skipNextDecay = effect.skipNextDecay,
        )

    private fun restoreActiveEffect(snapshot: ActiveEffectSnapshot): ActiveEffect {
        val type = StatusEffectType.valueOf(snapshot.type)
        return ActiveEffect(
            id = snapshot.id,
            name = effectDisplayName(type),
            type = type,
            remainingTurns = snapshot.remainingTurns,
            statModifiers = toStatModifier(snapshot.statModifiers),
            skipNextDecay = snapshot.skipNextDecay,
        )
    }

    private fun effectDisplayName(type: StatusEffectType): String =
        when (type) {
            StatusEffectType.STUNNED -> "Stunned"
            StatusEffectType.ARMOR_BREAK -> "Armor Break"
            StatusEffectType.WAR_CRY_BUFF -> "War Cry"
            StatusEffectType.WAR_CRY_DEBUFF -> "Shaken"
        }

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
            type = ItemType.valueOf(snapshot.type),
            slot = snapshot.slot?.let(EquipSlot::valueOf) ?: base.slot,
            glyph = base.glyph,
            colorHex = base.colorHex,
            quality = ItemQuality.valueOf(snapshot.quality),
            materialId = snapshot.materialId,
            materialName = material?.name,
            affixes = affixes,
            stats = toStatModifier(snapshot.stats),
            effect = snapshot.effect?.let(ConsumableEffect::valueOf) ?: base.effect,
            magnitude = snapshot.magnitude,
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
    ) {
        world.add(entityId, Glyph(HERO_GLYPH))
        world.add(entityId, DisplayColor(HERO_COLOR_HEX))
        world.add(entityId, Name(HERO_NAME))
    }

    private fun applyMonsterPresentation(
        world: World,
        entityId: EntityId,
        template: MonsterTemplate,
    ) {
        world.add(entityId, Glyph(template.glyph))
        world.add(entityId, DisplayColor(template.colorHex))
        world.add(entityId, Name(template.name))
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
    ) {
        world.add(entityId, Glyph(if (direction == StairDirection.DOWN) '>' else '<'))
        world.add(entityId, DisplayColor(STAIR_COLOR_HEX))
        world.add(entityId, Name(if (direction == StairDirection.DOWN) DOWNSTAIRS_NAME else UPSTAIRS_NAME))
    }

    private fun resolveMonsterTemplate(
        content: GameContent,
        templateId: String,
    ): MonsterTemplate =
        requireNotNull((content.monsterCatalog + content.bossDefinition.template).firstOrNull { template -> template.id == templateId }) {
            throw SaveRestoreException("Save references unknown monster template '$templateId'.")
        }
}
