package com.ktome.game

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.model.MonsterTemplate
import kotlin.math.abs

internal data class PatrolPressureRuntimeState(
    val spawnTemplateIds: List<String>,
    val maxHostiles: Int,
    val waveLimit: Int,
    val checkIntervalTurns: Int,
    val spawnSeed: Long,
    var nextCheckHeadlessTurn: Int,
    var wavesSpawned: Int = 0,
    var floorHintShown: Boolean = false,
)

internal data class AmbushLaneTriggerState(
    val triggerId: String,
    val spawnTemplateIds: List<String>,
    val spawnPoints: List<Point>,
)

internal enum class FurnacePressurePhase {
    IDLE,
    TELEGRAPH,
    ACTIVE,
}

internal data class FurnacePressureRuntimeState(
    val hazardCells: List<Point>,
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
    var nextCycleTurn: Int,
    var phase: FurnacePressurePhase = FurnacePressurePhase.IDLE,
    var phaseTurnsRemaining: Int = 0,
)

internal data class RiverCurrentRuntimeState(
    val laneCells: List<Point>,
    val approachCells: List<Point>,
    val safeCells: List<Point>,
    val pushDx: Int,
    val pushDy: Int,
)

internal enum class CrystalShardPhase {
    IDLE,
    TELEGRAPH,
    ACTIVE,
}

internal data class CrystalShardRuntimeState(
    val hazardCells: List<Point>,
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
    var nextCycleTurn: Int,
    var phase: CrystalShardPhase = CrystalShardPhase.IDLE,
    var phaseTurnsRemaining: Int = 0,
)

internal enum class VoidPressurePhase {
    IDLE,
    TELEGRAPH,
    ACTIVE,
}

internal data class AbyssalTemplePressureRuntimeState(
    val laneCells: List<Point>,
    val corridorCells: List<Point>,
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
    val suppressionTurnsOnStabilize: Int,
    var nextCycleTurn: Int,
    var phase: VoidPressurePhase = VoidPressurePhase.IDLE,
    var phaseTurnsRemaining: Int = 0,
    var suppressionTurnsRemaining: Int = 0,
)

internal data class VoidEruptionRuntimeState(
    val hazardCells: List<Point>,
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
    val stabilizedTurnsOnFocus: Int,
    var nextCycleTurn: Int,
    var phase: VoidPressurePhase = VoidPressurePhase.IDLE,
    var phaseTurnsRemaining: Int = 0,
    var stabilizedTurnsRemaining: Int = 0,
)

private data class PatrolPressureSpec(
    val spawnTemplateIds: List<String>,
    val maxHostiles: Int,
    val waveLimit: Int,
    val checkIntervalTurns: Int,
    val spawnSeed: Long,
)

private data class AmbushLaneSpec(
    val triggerId: String,
    val triggerPoint: Point,
    val spawnTemplateIds: List<String>,
    val spawnPoints: List<Point>,
)

private enum class CorridorOrientation {
    HORIZONTAL,
    VERTICAL,
}

private data class AmbushTriggerCandidate(
    val triggerPoint: Point,
    val orientation: CorridorOrientation,
    val spawnPoints: List<Point>,
)

private data class FurnacePressureSpec(
    val hazardCells: List<Point>,
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
)

private data class RiverCurrentSpec(
    val laneCells: List<Point>,
    val approachCells: List<Point>,
    val safeCells: List<Point>,
    val pushDx: Int,
    val pushDy: Int,
)

private data class CrystalShardSpec(
    val hazardCells: List<Point>,
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
)

private data class AbyssalTemplePressureSpec(
    val laneCells: List<Point>,
    val corridorCells: List<Point>,
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
    val suppressionTurnsOnStabilize: Int,
)

private data class VoidEruptionSpec(
    val hazardCells: List<Point>,
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
    val stabilizedTurnsOnFocus: Int,
)

internal object ZoneMechanicRuntime {
    private const val DEFAULT_PATROL_MAX_HOSTILES: Int = 4
    private const val DEFAULT_PATROL_WAVE_LIMIT: Int = 3
    private const val DEFAULT_PATROL_INTERVAL_TURNS: Int = 20
    private const val DEFAULT_FURNACE_INTERVAL_TURNS: Int = 30
    private const val DEFAULT_FURNACE_DAMAGE_PER_TICK: Int = 6
    private const val DEFAULT_CRYSTAL_INTERVAL_TURNS: Int = 8
    private const val DEFAULT_CRYSTAL_DAMAGE_PER_TICK: Int = 6
    private const val DEFAULT_VOID_PRESSURE_INTERVAL_TURNS: Int = 6
    private const val DEFAULT_VOID_PRESSURE_DAMAGE_PER_TICK: Int = 7
    private const val DEFAULT_VOID_PRESSURE_SUPPRESSION_TURNS: Int = 4
    private const val DEFAULT_VOID_ERUPTION_INTERVAL_TURNS: Int = 5
    private const val DEFAULT_VOID_ERUPTION_DAMAGE_PER_TICK: Int = 5
    private const val DEFAULT_FINALE_STABILIZED_TURNS: Int = 6
    private val FURNACE_ANCHOR_INTERACTABLE_IDS: Set<String> =
        setOf(
            "mine_furnace",
            "slag_valve",
            "molten_pressure_valve",
        )

    fun introHintKey(zone: ZoneSchemaV2): String? =
        when {
            "ambush_lane" in zone.specialMechanics -> "zone.mechanic_hint.ambush_lane"
            "furnace_pressure" in zone.specialMechanics -> "zone.mechanic_hint.furnace_pressure"
            "lore_cache" in zone.specialMechanics -> "zone.mechanic_hint.lore_cache"
            "ore_cart" in zone.specialMechanics -> "zone.mechanic_hint.ore_cart"
            "lava_pockets" in zone.specialMechanics -> "zone.mechanic_hint.lava_pockets"
            "sealed_gate" in zone.specialMechanics -> "zone.mechanic_hint.sealed_gate"
            "currents" in zone.specialMechanics -> "zone.mechanic_hint.currents"
            "crystal_shards" in zone.specialMechanics -> "zone.mechanic_hint.crystal_shards"
            "abyssal_ward" in zone.specialMechanics -> "zone.mechanic_hint.abyssal_ward"
            "finale" in zone.specialMechanics -> "zone.mechanic_hint.finale"
            zone.environmentTheme == "tutorial_ruins" -> "zone.mechanic_hint.tutorial_ruins"
            zone.environmentTheme == "forest_patrol" -> "zone.mechanic_hint.forest_patrol"
            zone.environmentTheme == "mine_forge" -> "zone.mechanic_hint.mine_forge"
            zone.environmentTheme == "sealed_depths" -> "zone.mechanic_hint.sealed_depths"
            else -> null
        }

    fun uniqueContentRewardProfiles(uniqueContentTag: String?): List<String> =
        when (uniqueContentTag) {
            "optional.bandit_camp.cache" -> listOf("loot.greenwood_fringe.reward", "loot.foundation.elite")
            "optional.elven_ruins.relic" -> listOf("loot.grey_gate_depths.reward", "loot.foundation.boss")
            "optional.molten_core.relic" -> listOf("loot.deep_iron_pit.reward", "loot.foundation.elite")
            "optional.crystal_cavern.node" -> listOf("loot.grey_gate_depths.reward", "loot.foundation.elite")
            else -> emptyList()
        }

    fun uniqueContentFallbackBaseId(uniqueContentTag: String?): String =
        when (uniqueContentTag) {
            "optional.bandit_camp.cache" -> "bandit_trophy"
            "optional.elven_ruins.relic" -> "seal_reliquary"
            "optional.molten_core.relic" -> "forgebreaker_pick"
            "optional.crystal_cavern.node" -> "emerald_charm"
            else -> "healing_potion"
        }

    fun installFloorRuntime(
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
        floor: Int,
        map: GameMap,
        world: World,
        occupiedPoints: Set<Point>,
        catalog: List<MonsterTemplate>,
    ) {
        buildPatrolPressureSpec(config = config, zone = zone, floor = floor, catalog = catalog)?.let { spec ->
            val entityId = world.createEntity()
            world.add(
                entityId,
                PatrolPressureRuntimeState(
                    spawnTemplateIds = spec.spawnTemplateIds,
                    maxHostiles = spec.maxHostiles,
                    waveLimit = spec.waveLimit,
                    checkIntervalTurns = spec.checkIntervalTurns,
                    spawnSeed = spec.spawnSeed,
                    nextCheckHeadlessTurn = spec.checkIntervalTurns,
                    floorHintShown = false,
                ),
            )
        }

        buildAmbushLaneSpecs(
            config = config,
            zone = zone,
            floor = floor,
            map = map,
            occupiedPoints = occupiedPoints,
            catalog = catalog,
        ).forEach { spec ->
            val entityId = world.createEntity()
            world.add(entityId, Position(spec.triggerPoint.x, spec.triggerPoint.y))
            world.add(
                entityId,
                AmbushLaneTriggerState(
                    triggerId = spec.triggerId,
                    spawnTemplateIds = spec.spawnTemplateIds,
                    spawnPoints = spec.spawnPoints,
                ),
            )
        }

        buildFurnacePressureSpec(
            zone = zone,
            floor = floor,
            map = map,
            world = world,
        )?.let { spec ->
            val entityId = world.createEntity()
            world.add(
                entityId,
                FurnacePressureRuntimeState(
                    hazardCells = spec.hazardCells,
                    cycleIntervalTurns = spec.cycleIntervalTurns,
                    telegraphTurns = spec.telegraphTurns,
                    activeTurns = spec.activeTurns,
                    damagePerTick = spec.damagePerTick,
                    nextCycleTurn = spec.cycleIntervalTurns,
                ),
            )
        }

        buildRiverCurrentSpec(
            config = config,
            zone = zone,
            floor = floor,
            map = map,
            world = world,
        )?.let { spec ->
            val entityId = world.createEntity()
            world.add(
                entityId,
                RiverCurrentRuntimeState(
                    laneCells = spec.laneCells,
                    approachCells = spec.approachCells,
                    safeCells = spec.safeCells,
                    pushDx = spec.pushDx,
                    pushDy = spec.pushDy,
                ),
            )
        }

        buildCrystalShardSpec(
            config = config,
            zone = zone,
            floor = floor,
            map = map,
            world = world,
        )?.let { spec ->
            val entityId = world.createEntity()
            world.add(
                entityId,
                CrystalShardRuntimeState(
                    hazardCells = spec.hazardCells,
                    cycleIntervalTurns = spec.cycleIntervalTurns,
                    telegraphTurns = spec.telegraphTurns,
                    activeTurns = spec.activeTurns,
                    damagePerTick = spec.damagePerTick,
                    nextCycleTurn = spec.cycleIntervalTurns,
                ),
            )
        }

        buildAbyssalTemplePressureSpec(
            config = config,
            zone = zone,
            floor = floor,
            map = map,
            world = world,
        )?.let { spec ->
            val entityId = world.createEntity()
            world.add(
                entityId,
                AbyssalTemplePressureRuntimeState(
                    laneCells = spec.laneCells,
                    corridorCells = spec.corridorCells,
                    cycleIntervalTurns = spec.cycleIntervalTurns,
                    telegraphTurns = spec.telegraphTurns,
                    activeTurns = spec.activeTurns,
                    damagePerTick = spec.damagePerTick,
                    suppressionTurnsOnStabilize = spec.suppressionTurnsOnStabilize,
                    nextCycleTurn = spec.cycleIntervalTurns,
                ),
            )
        }

        buildVoidEruptionSpec(
            config = config,
            zone = zone,
            floor = floor,
            map = map,
            world = world,
        )?.let { spec ->
            val entityId = world.createEntity()
            world.add(
                entityId,
                VoidEruptionRuntimeState(
                    hazardCells = spec.hazardCells,
                    cycleIntervalTurns = spec.cycleIntervalTurns,
                    telegraphTurns = spec.telegraphTurns,
                    activeTurns = spec.activeTurns,
                    damagePerTick = spec.damagePerTick,
                    stabilizedTurnsOnFocus = spec.stabilizedTurnsOnFocus,
                    nextCycleTurn = spec.cycleIntervalTurns,
                ),
            )
        }
    }

    fun orderedPoints(
        seed: Long,
        points: Collection<Point>,
    ): List<Point> =
        points
            .distinct()
            .sortedWith(
                compareBy<Point> { point -> stableHash(seed, point) }
                    .thenBy(Point::y)
                    .thenBy(Point::x),
            )

    private fun buildPatrolPressureSpec(
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
        floor: Int,
        catalog: List<MonsterTemplate>,
    ): PatrolPressureSpec? {
        if ("patrol_pressure" !in zone.specialMechanics) {
            return null
        }
        if (floor == zone.floorCount && zone.bossEncounterId != null) {
            return null
        }
        val templateIds =
            selectTemplates(
                catalog = patrolCandidateTemplates(catalog),
                count = 1,
                seed = mechanicSeed(config = config, zone = zone, floor = floor, salt = 0x5131),
            ).map(MonsterTemplate::id)
        if (templateIds.isEmpty()) {
            return null
        }
        return PatrolPressureSpec(
            spawnTemplateIds = templateIds,
            maxHostiles = DEFAULT_PATROL_MAX_HOSTILES,
            waveLimit = DEFAULT_PATROL_WAVE_LIMIT,
            checkIntervalTurns = DEFAULT_PATROL_INTERVAL_TURNS,
            spawnSeed = mechanicSeed(config = config, zone = zone, floor = floor, salt = 0x5117),
        )
    }

    private fun patrolCandidateTemplates(catalog: List<MonsterTemplate>): List<MonsterTemplate> {
        val nonElite =
            catalog.filterNot { template ->
                "boss" in template.tags || "elite" in template.tags || template.lootProfileId.endsWith(".elite")
            }
        val preferred =
            nonElite.filter { template ->
                template.archetype == "skirmisher" ||
                    template.archetype == "scout" ||
                    template.archetype == "sentinel"
            }
        return preferred
            .ifEmpty { nonElite }
            .ifEmpty { catalog }
            .sortedWith(
                compareBy<MonsterTemplate> { template ->
                    when (template.archetype) {
                        "scout",
                        "skirmisher",
                        -> 0
                        "sentinel" -> 1
                        "artillery" -> 2
                        else -> 3
                    }
                }.thenBy(MonsterTemplate::id),
            )
    }

    private fun buildAmbushLaneSpecs(
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
        floor: Int,
        map: GameMap,
        occupiedPoints: Set<Point>,
        catalog: List<MonsterTemplate>,
    ): List<AmbushLaneSpec> {
        if ("ambush_lane" !in zone.specialMechanics) {
            return emptyList()
        }
        val candidates =
            orderedCandidates(
                candidates = ambushTriggerCandidates(map = map, occupiedPoints = occupiedPoints),
                seed = mechanicSeed(config = config, zone = zone, floor = floor, salt = 0xA813),
            )
        if (candidates.isEmpty()) {
            return emptyList()
        }
        val triggerCount = if (candidates.size >= 2) 2 else 1
        return candidates.take(triggerCount).mapIndexedNotNull { index, candidate ->
            val templates =
                selectTemplates(
                    catalog = ambushCandidateTemplates(catalog),
                    count = candidate.spawnPoints.size,
                    seed = mechanicSeed(config = config, zone = zone, floor = floor, salt = 0xA900 + index),
                ).map(MonsterTemplate::id)
            if (templates.isEmpty()) {
                null
            } else {
                AmbushLaneSpec(
                    triggerId = "ambush_lane:$floor:$index",
                    triggerPoint = candidate.triggerPoint,
                    spawnTemplateIds = templates,
                    spawnPoints = candidate.spawnPoints.take(templates.size),
                )
            }
        }
    }

    private fun ambushCandidateTemplates(catalog: List<MonsterTemplate>): List<MonsterTemplate> {
        val regulars =
            catalog.filterNot { template ->
                "boss" in template.tags || "elite" in template.tags || template.lootProfileId.endsWith(".elite")
            }
        val preferred =
            regulars.filter { template ->
                template.archetype == "skirmisher" ||
                    template.archetype == "scout" ||
                    template.archetype == "artillery"
            }
        return preferred
            .ifEmpty { regulars }
            .ifEmpty { catalog }
            .sortedWith(
                compareBy<MonsterTemplate> { template ->
                    when (template.archetype) {
                        "skirmisher",
                        "scout",
                        -> 0
                        "artillery" -> 1
                        "sentinel" -> 2
                        else -> 3
                    }
                }.thenBy(MonsterTemplate::id),
            )
    }

    private fun ambushTriggerCandidates(
        map: GameMap,
        occupiedPoints: Set<Point>,
    ): List<AmbushTriggerCandidate> =
        map.floorPoints()
            .asSequence()
            .mapNotNull { point ->
                val orientation = corridorOrientation(map, point) ?: return@mapNotNull null
                if (occupiedPoints.any { occupied -> occupied.chebyshevDistanceTo(point) <= 3 }) {
                    return@mapNotNull null
                }
                if (point.chebyshevDistanceTo(map.playerStart) <= 5) {
                    return@mapNotNull null
                }
                val spawnPoints =
                    when (orientation) {
                        CorridorOrientation.HORIZONTAL ->
                            listOfNotNull(
                                findPocketPoint(map, point, Point(0, -1), Point(1, 0)),
                                findPocketPoint(map, point, Point(0, 1), Point(1, 0)),
                            )

                        CorridorOrientation.VERTICAL ->
                            listOfNotNull(
                                findPocketPoint(map, point, Point(-1, 0), Point(0, 1)),
                                findPocketPoint(map, point, Point(1, 0), Point(0, 1)),
                            )
                    }.distinct()
                if (spawnPoints.size < 2) {
                    return@mapNotNull null
                }
                AmbushTriggerCandidate(
                    triggerPoint = point,
                    orientation = orientation,
                    spawnPoints = spawnPoints,
                )
            }.toList()

    private fun corridorOrientation(
        map: GameMap,
        point: Point,
    ): CorridorOrientation? {
        if (!isPassable(map, point)) {
            return null
        }
        val north = isPassable(map, point + Point(0, -1))
        val south = isPassable(map, point + Point(0, 1))
        val west = isPassable(map, point + Point(-1, 0))
        val east = isPassable(map, point + Point(1, 0))
        return when {
            north && south && !west && !east -> CorridorOrientation.VERTICAL
            west && east && !north && !south -> CorridorOrientation.HORIZONTAL
            else -> null
        }
    }

    private fun findPocketPoint(
        map: GameMap,
        origin: Point,
        primaryDirection: Point,
        lateralDirection: Point,
    ): Point? {
        var best: Point? = null
        for (depth in 1..4) {
            val base = origin + Point(primaryDirection.x * depth, primaryDirection.y * depth)
            val candidates =
                listOf(
                    base,
                    base + lateralDirection,
                    base - lateralDirection,
                )
            val passable =
                candidates
                    .filter { point -> isPassable(map, point) }
                    .sortedWith(compareBy<Point> { it.chebyshevDistanceTo(origin) }.thenBy(Point::y).thenBy(Point::x))
            if (passable.isNotEmpty()) {
                best = passable.last()
            }
        }
        return best?.takeIf { candidate -> candidate.chebyshevDistanceTo(origin) >= 2 }
    }

    private fun buildFurnacePressureSpec(
        zone: ZoneSchemaV2,
        floor: Int,
        map: GameMap,
        world: World,
    ): FurnacePressureSpec? {
        if ("furnace_pressure" !in zone.specialMechanics) {
            return null
        }
        val anchorCells =
            world.entitiesWith(Position::class, Interactable::class)
                .filter { entityId -> world.get<Interactable>(entityId)?.id in FURNACE_ANCHOR_INTERACTABLE_IDS }
                .mapNotNull { entityId -> world.get<Position>(entityId)?.toPoint() }
        val hazardCells =
            when {
                anchorCells.isNotEmpty() -> furnaceCellsAroundAnchors(map, anchorCells, zone, floor)
                else -> furnaceFallbackCells(map, zone, floor)
            }
        if (hazardCells.isEmpty()) {
            return null
        }
        return FurnacePressureSpec(
            hazardCells = hazardCells,
            cycleIntervalTurns = DEFAULT_FURNACE_INTERVAL_TURNS,
            telegraphTurns = 1,
            activeTurns = 2,
            damagePerTick = maxOf(DEFAULT_FURNACE_DAMAGE_PER_TICK, zone.recommendedLevel.min),
        )
    }

    private fun buildRiverCurrentSpec(
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
        floor: Int,
        map: GameMap,
        world: World,
    ): RiverCurrentSpec? {
        if ("currents" !in zone.specialMechanics) {
            return null
        }
        val routeStart = map.playerStart
        val routeEnd = riverRouteExitPoint(world = world, map = map, routeStart = routeStart)
        val anchor = interactablePoint(world, RiverCrystalRuntimeKeys.River.INTERACTABLE_ID) ?: midpoint(routeStart, routeEnd)
        // Current pressure should read as "sideways drag" relative to the player's approach to the ferry anchor.
        // Using the full stair-to-stair delta can align the push with the objective path and make the anchor unreachable.
        val horizontalBand = abs(anchor.y - routeStart.y) >= abs(anchor.x - routeStart.x)
        val laneCells =
            orderedPoints(
                seed = mechanicSeed(config = config, zone = zone, floor = floor, salt = 0xC011),
                points =
                    routeBandCells(
                        map = map,
                        anchor = anchor,
                        routeStart = routeStart,
                        routeEnd = routeEnd,
                        horizontalBand = horizontalBand,
                    ),
            )
        if (laneCells.isEmpty()) {
            return null
        }
        val pathToAnchor = routePath(map = map, start = routeStart, goal = anchor)
        val pathToExit = routePath(map = map, start = anchor, goal = routeEnd)
        val safeCells =
            riverSafeCells(
                laneCells = laneCells,
                pathToAnchor = pathToAnchor,
                pathToExit = pathToExit,
                anchor = anchor,
                horizontalBand = horizontalBand,
            )
        val approachCells =
            riverApproachCells(
                safeCells = safeCells,
                pathToAnchor = pathToAnchor,
            )
        val pushDx =
            if (horizontalBand) {
                nonZeroSign(routeEnd.x - routeStart.x, fallback = seededDirection(config, zone, floor, salt = 0xC0DE))
            } else {
                0
            }
        val pushDy =
            if (horizontalBand) {
                0
            } else {
                nonZeroSign(routeEnd.y - routeStart.y, fallback = seededDirection(config, zone, floor, salt = 0xCAFE))
            }
        val normalizedSafeCells = safeCells.ifEmpty { laneCells.take(1) }
        val normalizedApproachCells = approachCells.ifEmpty { normalizedSafeCells.take(1) }
        return RiverCurrentSpec(
            laneCells = laneCells,
            approachCells = normalizedApproachCells,
            safeCells = normalizedSafeCells,
            pushDx = pushDx,
            pushDy = pushDy,
        )
    }

    private fun buildCrystalShardSpec(
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
        floor: Int,
        map: GameMap,
        world: World,
    ): CrystalShardSpec? {
        if ("crystal_shards" !in zone.specialMechanics) {
            return null
        }
        val nodePoint = interactablePoint(world, RiverCrystalRuntimeKeys.Crystal.INTERACTABLE_ID)
        val anchor = nodePoint ?: fallbackRouteAnchor(map)
        val roomAnchors =
            map.rooms
                .map(RoomLike::fromRoom)
                .sortedWith(
                    compareByDescending<RoomLike> { room -> room.center.chebyshevDistanceTo(anchor) }
                        .thenBy { room -> room.center.y }
                        .thenBy { room -> room.center.x },
                ).take(2)
                .map(RoomLike::center)
        val candidates =
            orderedPoints(
                seed = mechanicSeed(config = config, zone = zone, floor = floor, salt = 0x5344),
                points =
                    crystalHazardCells(
                        map = map,
                        anchors = listOf(anchor) + roomAnchors,
                    ),
            )
        if (candidates.isEmpty()) {
            return null
        }
        return CrystalShardSpec(
            hazardCells = candidates.take(14),
            cycleIntervalTurns = DEFAULT_CRYSTAL_INTERVAL_TURNS,
            telegraphTurns = 1,
            activeTurns = 2,
            damagePerTick = maxOf(DEFAULT_CRYSTAL_DAMAGE_PER_TICK, zone.recommendedLevel.min / 2),
        )
    }

    private fun buildAbyssalTemplePressureSpec(
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
        floor: Int,
        map: GameMap,
        world: World,
    ): AbyssalTemplePressureSpec? {
        if ("void_pressure" !in zone.specialMechanics) {
            return null
        }
        val routeStart = map.playerStart
        val routeEnd = riverRouteExitPoint(world = world, map = map, routeStart = routeStart)
        val reliquaryPoint = interactablePoint(world, AbyssalRuntimeKeys.Temple.INTERACTABLE_ID)
        val anchor =
            reliquaryPoint
                ?.takeIf { point -> isPassable(map, point) }
                ?: fallbackRouteAnchor(map)
        val horizontalBand = abs(anchor.y - routeStart.y) >= abs(anchor.x - routeStart.x)
        val laneCandidates =
            routeBandCells(
                map = map,
                anchor = anchor,
                routeStart = routeStart,
                routeEnd = routeEnd,
                horizontalBand = horizontalBand,
            ).ifEmpty {
                templeFallbackCells(map = map, anchor = anchor)
            }
        val laneCells =
            orderedPoints(
                seed = mechanicSeed(config = config, zone = zone, floor = floor, salt = 0xAB17),
                points = laneCandidates,
            )
        if (laneCells.isEmpty()) {
            return null
        }
        val laneSet = laneCells.toSet()
        val corridorCells =
            ((routePath(map = map, start = routeStart, goal = anchor) + routePath(map = map, start = anchor, goal = routeEnd)).distinct())
                .filter(laneSet::contains)
                .ifEmpty {
                    laneCells
                        .sortedWith(
                            compareBy<Point> { point -> point.chebyshevDistanceTo(anchor) }
                                .thenBy(Point::y)
                                .thenBy(Point::x),
                        ).take(4)
                }
        return AbyssalTemplePressureSpec(
            laneCells = laneCells,
            corridorCells = corridorCells,
            cycleIntervalTurns = DEFAULT_VOID_PRESSURE_INTERVAL_TURNS,
            telegraphTurns = 1,
            activeTurns = 2,
            damagePerTick = maxOf(DEFAULT_VOID_PRESSURE_DAMAGE_PER_TICK, zone.recommendedLevel.min / 2),
            suppressionTurnsOnStabilize = DEFAULT_VOID_PRESSURE_SUPPRESSION_TURNS,
        )
    }

    private fun buildVoidEruptionSpec(
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
        floor: Int,
        map: GameMap,
        world: World,
    ): VoidEruptionSpec? {
        if ("void_eruption" !in zone.specialMechanics) {
            return null
        }
        val focusPoint =
            interactablePoint(world, AbyssalRuntimeKeys.Finale.INTERACTABLE_ID)
                ?.takeIf { point -> isPassable(map, point) }
                ?: fallbackRouteAnchor(map)
        val roomAnchors =
            map.rooms
                .map(RoomLike::fromRoom)
                .sortedWith(
                    compareByDescending<RoomLike> { room -> room.center.chebyshevDistanceTo(map.playerStart) }
                        .thenBy { room -> room.center.y }
                        .thenBy { room -> room.center.x },
                ).take(2)
                .map(RoomLike::center)
        val candidates =
            orderedPoints(
                seed = mechanicSeed(config = config, zone = zone, floor = floor, salt = 0xF117),
                points =
                    crystalHazardCells(
                        map = map,
                        anchors = listOf(focusPoint) + roomAnchors,
                    ).filterNot { point -> point.chebyshevDistanceTo(focusPoint) <= 1 }
                        .ifEmpty {
                            voidEruptionFallbackCells(map = map, focusPoint = focusPoint)
                        },
            )
        if (candidates.isEmpty()) {
            return null
        }
        return VoidEruptionSpec(
            hazardCells = candidates.take(10),
            cycleIntervalTurns = DEFAULT_VOID_ERUPTION_INTERVAL_TURNS,
            telegraphTurns = 1,
            activeTurns = 1,
            damagePerTick = maxOf(DEFAULT_VOID_ERUPTION_DAMAGE_PER_TICK, zone.recommendedLevel.min / 3),
            stabilizedTurnsOnFocus = DEFAULT_FINALE_STABILIZED_TURNS,
        )
    }

    private fun furnaceCellsAroundAnchors(
        map: GameMap,
        anchors: List<Point>,
        zone: ZoneSchemaV2,
        floor: Int,
    ): List<Point> {
        val candidates =
            anchors.flatMap { anchor ->
                buildList {
                    add(anchor)
                    Point.ALL_DIRECTIONS.forEach { delta ->
                        add(anchor + delta)
                    }
                }
            }.filter { point -> isPassable(map, point) }
            .distinct()
        if (candidates.isEmpty()) {
            return emptyList()
        }
        val seed = zone.id.hashCode().toLong() xor floor.toLong()
        return orderedPoints(seed = seed, points = candidates).take(8)
    }

    private fun furnaceFallbackCells(
        map: GameMap,
        zone: ZoneSchemaV2,
        floor: Int,
    ): List<Point> {
        val rooms = map.rooms.drop(1).ifEmpty { map.rooms }
        val anchors = rooms.sortedBy { room -> room.center.chebyshevDistanceTo(map.playerStart) }.take(2).map(RoomLike::fromRoom)
        val cells =
            anchors.flatMap { room ->
                buildList {
                    add(room.center)
                    Point.CARDINAL_DIRECTIONS.forEach { delta -> add(room.center + delta) }
                }
            }.filter { point -> isPassable(map, point) }
            .distinct()
        val seed = zone.id.hashCode().toLong() xor (floor.toLong() shl 8) xor 0xFACE
        return orderedPoints(seed = seed, points = cells).take(6)
    }

    private fun stairPoint(
        world: World,
        direction: StairDirection,
    ): Point? =
        world.entitiesWith(Position::class, Stair::class)
            .firstOrNull { entityId -> world.get<Stair>(entityId)?.direction == direction }
            ?.let { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }

    private fun riverRouteExitPoint(
        world: World,
        map: GameMap,
        routeStart: Point,
    ): Point =
        stairPoint(world = world, direction = StairDirection.DOWN)
            ?: stairPoint(world = world, direction = StairDirection.UP)?.takeIf { point -> point != routeStart }
            ?: fallbackRouteAnchor(map)

    private fun interactablePoint(
        world: World,
        interactableId: String,
    ): Point? =
        world.entitiesWith(Position::class, Interactable::class)
            .firstOrNull { entityId -> world.get<Interactable>(entityId)?.id == interactableId }
            ?.let { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }

    private fun routeBandCells(
        map: GameMap,
        anchor: Point,
        routeStart: Point,
        routeEnd: Point,
        horizontalBand: Boolean,
    ): List<Point> {
        val blockedOrigins = setOf(routeStart, routeEnd, map.playerStart)
        return map.floorPoints()
            .filter { point ->
                isPassable(map, point) &&
                    point !in blockedOrigins &&
                    blockedOrigins.none { origin -> origin.chebyshevDistanceTo(point) <= 2 } &&
                    if (horizontalBand) {
                        abs(point.y - anchor.y) <= 1
                    } else {
                        abs(point.x - anchor.x) <= 1
                    }
            }.ifEmpty {
                riverFallbackCells(map, anchor, horizontalBand)
            }
    }

    private fun riverFallbackCells(
        map: GameMap,
        anchor: Point,
        horizontalBand: Boolean,
    ): List<Point> =
        buildList {
            for (offset in -4..4) {
                val point =
                    if (horizontalBand) {
                        Point(anchor.x + offset, anchor.y)
                    } else {
                        Point(anchor.x, anchor.y + offset)
                    }
                if (isPassable(map, point)) {
                    add(point)
                }
            }
        }

    private fun templeFallbackCells(
        map: GameMap,
        anchor: Point,
    ): List<Point> =
        buildList {
            add(anchor)
            Point.CARDINAL_DIRECTIONS.forEach { delta ->
                add(anchor + delta)
                add(anchor + Point(delta.x * 2, delta.y * 2))
            }
        }.filter { point -> isPassable(map, point) }

    private fun voidEruptionFallbackCells(
        map: GameMap,
        focusPoint: Point,
    ): List<Point> =
        buildList {
            Point.CARDINAL_DIRECTIONS.forEach { delta ->
                add(focusPoint + Point(delta.x * 2, delta.y * 2))
                add(focusPoint + Point(delta.x * 3, delta.y * 3))
            }
            listOf(
                Point(-1, -1),
                Point(1, -1),
                Point(-1, 1),
                Point(1, 1),
            ).forEach { delta ->
                add(focusPoint + Point(delta.x * 2, delta.y * 2))
            }
        }.filter { point -> isPassable(map, point) }

    private fun riverSafeCells(
        laneCells: List<Point>,
        pathToAnchor: List<Point>,
        pathToExit: List<Point>,
        anchor: Point,
        horizontalBand: Boolean,
    ): List<Point> {
        val laneSet = laneCells.toSet()
        val routedCells =
            (pathToAnchor + pathToExit)
                .filter(laneSet::contains)
                .distinct()
        if (routedCells.isNotEmpty()) {
            return routedCells
        }
        return laneCells.filter { point ->
            if (horizontalBand) {
                abs(point.x - anchor.x) <= 1
            } else {
                abs(point.y - anchor.y) <= 1
            }
        }.ifEmpty {
            laneCells
                .sortedWith(
                    compareBy<Point> { point -> point.chebyshevDistanceTo(anchor) }
                        .thenBy(Point::y)
                        .thenBy(Point::x),
                ).take(3)
        }
    }

    private fun riverApproachCells(
        safeCells: List<Point>,
        pathToAnchor: List<Point>,
    ): List<Point> {
        val safeSet = safeCells.toSet()
        val routedApproach =
            pathToAnchor
                .filter(safeSet::contains)
                .distinct()
        if (routedApproach.isNotEmpty()) {
            return routedApproach
        }
        if (safeCells.isEmpty()) {
            return emptyList()
        }
        return safeCells.take((safeCells.size / 2).coerceAtLeast(1))
    }

    private fun routePath(
        map: GameMap,
        start: Point,
        goal: Point,
    ): List<Point> =
        AStar.findPath(
            map = map,
            start = start,
            goal = goal,
            blocked = emptySet(),
        )

    private fun crystalHazardCells(
        map: GameMap,
        anchors: List<Point>,
    ): List<Point> =
        anchors
            .flatMap { anchor ->
                buildList {
                    add(anchor)
                    Point.CARDINAL_DIRECTIONS.forEach { delta ->
                        add(anchor + delta)
                    }
                    Point.CARDINAL_DIRECTIONS.forEach { delta ->
                        add(anchor + Point(delta.x * 2, delta.y * 2))
                    }
                }
            }.filter { point ->
                isPassable(map, point) &&
                    point.chebyshevDistanceTo(map.playerStart) > 2
            }.distinct()

    private fun fallbackRouteAnchor(map: GameMap): Point =
        map.rooms
            .sortedWith(compareBy<com.ktome.core.map.Room> { room -> room.center.chebyshevDistanceTo(map.playerStart) }.thenBy { room -> room.center.y }.thenBy { room -> room.center.x })
            .drop(1)
            .firstOrNull()
            ?.center
            ?: map.playerStart

    private fun midpoint(
        start: Point,
        end: Point,
    ): Point = Point(x = (start.x + end.x) / 2, y = (start.y + end.y) / 2)

    private fun seededDirection(
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
        floor: Int,
        salt: Int,
    ): Int = if ((mechanicSeed(config = config, zone = zone, floor = floor, salt = salt) and 1L) == 0L) 1 else -1

    private fun nonZeroSign(
        value: Int,
        fallback: Int,
    ): Int =
        when {
            value > 0 -> 1
            value < 0 -> -1
            else -> fallback
        }

    private fun isPassable(
        map: GameMap,
        point: Point,
    ): Boolean =
        map.isInBounds(point.x, point.y) &&
            !map[point].blocksMovement

    private fun selectTemplates(
        catalog: List<MonsterTemplate>,
        count: Int,
        seed: Long,
    ): List<MonsterTemplate> {
        if (count <= 0 || catalog.isEmpty()) {
            return emptyList()
        }
        return catalog
            .distinctBy(MonsterTemplate::id)
            .sortedWith(
                compareBy<MonsterTemplate> { template -> stableHash(seed, template.id) }
                    .thenBy(MonsterTemplate::id),
            ).take(count)
    }

    private fun orderedCandidates(
        candidates: List<AmbushTriggerCandidate>,
        seed: Long,
    ): List<AmbushTriggerCandidate> =
        candidates.sortedWith(
            compareBy<AmbushTriggerCandidate> { candidate -> stableHash(seed, candidate.triggerPoint) }
                .thenBy { candidate -> candidate.triggerPoint.y }
                .thenBy { candidate -> candidate.triggerPoint.x },
        )

    private fun mechanicSeed(
        config: FoundationGameConfig,
        zone: ZoneSchemaV2,
        floor: Int,
        salt: Int,
    ): Long = config.seed xor zone.id.hashCode().toLong() xor (floor.toLong() shl 32) xor salt.toLong()

    private fun stableHash(
        seed: Long,
        point: Point,
    ): Long {
        var value = seed xor (point.x.toLong() shl 32) xor point.y.toLong()
        value = value xor (value ushr 33)
        value *= -0xae502812aa7333L
        value = value xor (value ushr 33)
        value *= -0x3b314601e57a13adL
        return value xor (value ushr 33)
    }

    private fun stableHash(
        seed: Long,
        value: String,
    ): Long {
        var hash = seed
        value.forEach { ch ->
            hash = (hash * 131) xor ch.code.toLong()
            hash = hash xor (hash ushr 33)
        }
        hash *= -0xae502812aa7333L
        hash = hash xor (hash ushr 33)
        hash *= -0x3b314601e57a13adL
        return hash xor (hash ushr 33)
    }

    private data class RoomLike(
        val center: Point,
    ) {
        companion object {
            fun fromRoom(room: com.ktome.core.map.Room): RoomLike = RoomLike(center = room.center)
        }
    }
}
