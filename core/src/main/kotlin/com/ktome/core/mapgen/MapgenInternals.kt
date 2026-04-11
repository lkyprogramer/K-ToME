package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.world.solvability.NodeAnchorId
import kotlin.math.roundToInt
import kotlin.random.Random

internal const val LEGACY_COMPATIBILITY_BIOME_FAMILY_ID: String = "legacy.compatibility"

internal object LinearTopologyProjector {
    fun project(map: GameMap): TopologyGraph {
        if (map.rooms.isEmpty()) {
            val syntheticId = NodeId("legacy-start")
            return TopologyGraph(
                nodes =
                    listOf(
                        TopologyNode(
                            id = syntheticId,
                            anchorId = NodeAnchorId("legacy.start"),
                            roomDefId = "legacy.synthetic.start",
                            pathClass = PathClass.CRITICAL_PATH,
                            tags = setOf("legacy", "synthetic", "start"),
                            biomeFamilyId = LEGACY_COMPATIBILITY_BIOME_FAMILY_ID,
                        ),
                    ),
                edges = emptyList(),
                primaryPathNodeIds = listOf(syntheticId),
                optionalLoopCount = 0,
            )
        }
        val nodes =
            map.rooms.mapIndexed { index, room ->
                TopologyNode(
                    id = NodeId("room-${index.toString().padStart(2, '0')}"),
                    anchorId = NodeAnchorId("legacy.room.${index.toString().padStart(2, '0')}"),
                    roomDefId = "bsp.rect.standard",
                    pathClass = PathClass.CRITICAL_PATH,
                    tags = setOf("bsp", "room", if (index == 0) "start" else "primary"),
                    biomeFamilyId = LEGACY_COMPATIBILITY_BIOME_FAMILY_ID,
                )
            }
        require(nodes.isNotEmpty()) { "BSP-backed projection requires at least one room." }
        val edges =
            nodes.zipWithNext { left, right ->
                TopologyEdge(from = left.id, to = right.id)
            }
        return TopologyGraph(
            nodes = nodes,
            edges = edges,
            primaryPathNodeIds = nodes.map(TopologyNode::id),
            optionalLoopCount = 0,
        )
    }
}

internal object CompatibilityRoomProjector {
    fun project(
        map: GameMap,
        topology: TopologyGraph,
    ): List<RoomInstance> {
        if (map.rooms.isEmpty()) {
            val syntheticRoom = syntheticRoom(map)
            return topology.primaryPathNodeIds.take(1).map { nodeId ->
                RoomInstance(
                    nodeId = nodeId,
                    anchorId = NodeAnchorId("legacy.synthetic.room"),
                    roomDefId = "legacy.synthetic.room",
                    x = syntheticRoom.x,
                    y = syntheticRoom.y,
                    width = syntheticRoom.width,
                    height = syntheticRoom.height,
                    shape = RoomShape.RECT,
                    tags = setOf("legacy", "synthetic", "room"),
                    pathClass = PathClass.CRITICAL_PATH,
                    biomeFamilyId = LEGACY_COMPATIBILITY_BIOME_FAMILY_ID,
                )
            }
        }
        val topologyNodeById = topology.nodes.associateBy(TopologyNode::id)
        return map.rooms.mapIndexed { index, room ->
            val nodeId =
                topology.nodes.getOrNull(index)?.id
                    ?: NodeId("room-${index.toString().padStart(2, '0')}")
            val node = topologyNodeById[nodeId]
            RoomInstance(
                nodeId = nodeId,
                anchorId = node?.anchorId ?: NodeAnchorId("legacy.room.${index.toString().padStart(2, '0')}"),
                roomDefId = node?.roomDefId ?: "legacy.room",
                x = room.x,
                y = room.y,
                width = room.width,
                height = room.height,
                shape = RoomShape.RECT,
                pathClass = node?.pathClass ?: PathClass.CRITICAL_PATH,
                tags = node?.tags ?: setOf("legacy", "room"),
                biomeFamilyId = LEGACY_COMPATIBILITY_BIOME_FAMILY_ID,
            )
        }
    }

    private fun syntheticRoom(map: GameMap): Room {
        val floorPoints = map.floorPoints()
        val minX = floorPoints.minOfOrNull(Point::x) ?: map.playerStart.x
        val maxX = floorPoints.maxOfOrNull(Point::x) ?: map.playerStart.x
        val minY = floorPoints.minOfOrNull(Point::y) ?: map.playerStart.y
        val maxY = floorPoints.maxOfOrNull(Point::y) ?: map.playerStart.y
        return Room(
            x = minX,
            y = minY,
            width = (maxX - minX + 1).coerceAtLeast(1),
            height = (maxY - minY + 1).coerceAtLeast(1),
        )
    }
}

internal object TerrainTagPainter {
    private const val TERRAIN_DENSITY_SCALE: Float = 0.64f

    fun paint(
        map: GameMap,
        profile: ZoneMapgenProfile,
        seed: Long,
    ): Map<Point, Set<TerrainTag>> {
        return paint(
            map = map,
            profile = profile,
            seed = seed,
            rooms = CompatibilityRoomProjector.project(map = map, topology = LinearTopologyProjector.project(map)),
            biomeFamilies = emptyMap(),
            seededTags = emptyMap(),
        )
    }

    fun paint(
        map: GameMap,
        profile: ZoneMapgenProfile,
        seed: Long,
        rooms: List<RoomInstance>,
        biomeFamilies: Map<NodeId, BiomeFamilyDef>,
        seededTags: Map<Point, Set<TerrainTag>>,
    ): Map<Point, Set<TerrainTag>> {
        val painted: MutableMap<Point, MutableSet<TerrainTag>> =
            seededTags.mapValuesTo(linkedMapOf()) { (_, tags) -> linkedSetOf<TerrainTag>().apply { addAll(tags) } }
        if (rooms.isEmpty()) {
            return seededTags
        }
        rooms.forEachIndexed { index, room ->
            val effectiveWeights =
                resolveEffectiveWeights(
                    profileWeights = profile.terrainTagWeights,
                    familyWeights = biomeFamilies[room.nodeId]?.terrainTagWeights.orEmpty(),
                )
            if (effectiveWeights.isEmpty()) {
                return@forEachIndexed
            }
            paintWeightedTags(
                painted = painted,
                candidatePoints = paintableRoomPoints(room = room, map = map, excludedPoint = map.playerStart),
                weights = effectiveWeights,
            ) { tag ->
                Random(seed xor room.nodeId.value.hashCode().toLong() xor ((index + 1L) shl 24) xor tag.ordinal.toLong())
            }
        }
        return painted.mapValues { (_, tags) -> tags.toSet() }
    }

    fun resolveEffectiveWeights(
        profileWeights: Map<TerrainTag, Float>,
        familyWeights: Map<TerrainTag, Float>,
    ): Map<TerrainTag, Float> =
        linkedMapOf<TerrainTag, Float>().apply {
            TerrainTag.entries.sortedBy(TerrainTag::ordinal).forEach { tag ->
                val resolvedWeight = profileWeights[tag] ?: familyWeights[tag] ?: 0f
                if (resolvedWeight > 0f) {
                    put(tag, resolvedWeight)
                }
            }
        }

    fun paintWeightedTags(
        painted: MutableMap<Point, MutableSet<TerrainTag>>,
        candidatePoints: List<Point>,
        weights: Map<TerrainTag, Float>,
        randomForTag: (TerrainTag) -> Random,
    ) {
        if (candidatePoints.isEmpty()) {
            return
        }
        weights.entries
            .sortedBy { (tag, _) -> tag.ordinal }
            .forEach { (tag, weight) ->
                if (weight <= 0f) {
                    return@forEach
                }
                val targetCount = terrainTargetCount(candidatePoints.size, weight)
                val random = randomForTag(tag)
                repeat(targetCount) {
                    val point = candidatePoints[random.nextInt(candidatePoints.size)]
                    painted.getOrPut(point) { linkedSetOf() }.add(tag)
                }
            }
    }

    fun paintableRoomPoints(
        room: RoomInstance,
        map: GameMap,
        excludedPoint: Point? = null,
    ): List<Point> {
        val points = mutableListOf<Point>()
        for (y in room.y until room.y + room.height) {
            for (x in room.x until room.x + room.width) {
                val point = Point(x, y)
                if (map.isInBounds(x, y) && !map.blocksMovement(x, y) && point != excludedPoint) {
                    points += point
                }
            }
        }
        return points.sortedWith(compareBy<Point>(Point::y).thenBy(Point::x))
    }

    private fun terrainTargetCount(
        candidatePointCount: Int,
        weight: Float,
    ): Int =
        maxOf(
            1,
            minOf(candidatePointCount, (candidatePointCount * weight * TERRAIN_DENSITY_SCALE).roundToInt()),
        )
}
