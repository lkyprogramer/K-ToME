package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.map.Room
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
        val painted = seededTags.mapValuesTo(linkedMapOf()) { (_, tags) -> linkedSetOf<TerrainTag>().apply { addAll(tags) } }
        if (rooms.isEmpty()) {
            return seededTags
        }
        rooms.forEachIndexed { index, room ->
            val familyWeights = biomeFamilies[room.nodeId]?.terrainTagWeights.orEmpty()
            val effectiveWeights =
                linkedMapOf<TerrainTag, Float>().apply {
                    TerrainTag.entries.sortedBy(TerrainTag::ordinal).forEach { tag ->
                        val resolvedWeight = profile.terrainTagWeights[tag] ?: familyWeights[tag] ?: 0f
                        if (resolvedWeight > 0f) {
                            put(tag, resolvedWeight)
                        }
                    }
                }
            if (effectiveWeights.isEmpty()) {
                return@forEachIndexed
            }
            val candidatePoints =
                roomFloorPoints(room = room, map = map)
                    .asSequence()
                    .filterNot { point -> point == map.playerStart }
                    .toList()
            if (candidatePoints.isEmpty()) {
                return@forEachIndexed
            }
            val totalWeight = effectiveWeights.values.sum()
            effectiveWeights.forEach { (tag, weight) ->
                val ratio = weight / totalWeight
                val targetCount = maxOf(1, (candidatePoints.size * ratio * 0.18f).roundToInt())
                val random = Random(seed xor room.nodeId.value.hashCode().toLong() xor ((index + 1L) shl 24) xor tag.ordinal.toLong())
                repeat(targetCount) {
                    val point = candidatePoints[random.nextInt(candidatePoints.size)]
                    painted.getOrPut(point) { linkedSetOf() }.add(tag)
                }
            }
        }
        return painted.mapValues { (_, tags) -> tags.toSet() }
    }

    private fun roomFloorPoints(
        room: RoomInstance,
        map: GameMap,
    ): List<Point> {
        val points = mutableListOf<Point>()
        for (y in room.y until room.y + room.height) {
            for (x in room.x until room.x + room.width) {
                if (!map.blocksMovement(x, y)) {
                    points += Point(x, y)
                }
            }
        }
        return points.sortedWith(compareBy<Point>(Point::y).thenBy(Point::x))
    }
}
