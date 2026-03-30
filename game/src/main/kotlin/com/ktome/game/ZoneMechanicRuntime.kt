package com.ktome.game

import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.model.MonsterTemplate
import java.util.Random

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

internal object ZoneMechanicRuntime {
    private const val DEFAULT_PATROL_MAX_HOSTILES: Int = 4
    private const val DEFAULT_PATROL_WAVE_LIMIT: Int = 3
    private const val DEFAULT_PATROL_INTERVAL_TURNS: Int = 20
    private const val DEFAULT_FURNACE_INTERVAL_TURNS: Int = 30
    private const val DEFAULT_FURNACE_DAMAGE_PER_TICK: Int = 6
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
