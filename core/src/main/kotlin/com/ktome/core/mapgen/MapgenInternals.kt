package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import kotlin.math.roundToInt
import kotlin.random.Random

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

internal object TerrainTagPainter {
    fun paint(
        map: GameMap,
        profile: ZoneMapgenProfile,
        seed: Long,
    ): Map<Point, Set<TerrainTag>> {
        if (profile.terrainTagWeights.isEmpty()) {
            return emptyMap()
        }
        val totalWeight = profile.terrainTagWeights.values.sum()
        if (totalWeight <= 0f) {
            return emptyMap()
        }

        val candidatePoints =
            map.floorPoints()
                .asSequence()
                .filterNot { point -> point == map.playerStart }
                .sortedWith(compareBy<Point>(Point::y).thenBy(Point::x))
                .toList()
        if (candidatePoints.isEmpty()) {
            return emptyMap()
        }

        val painted = linkedMapOf<Point, MutableSet<TerrainTag>>()
        profile.terrainTagWeights.entries
            .sortedBy { (tag, _) -> tag.ordinal }
            .forEachIndexed { index, (tag, weight) ->
                if (weight <= 0f) {
                    return@forEachIndexed
                }
                val ratio = weight / totalWeight
                val targetCount = maxOf(1, (candidatePoints.size * ratio * 0.08f).roundToInt())
                val random = Random(seed xor ((index + 1L) shl 32) xor tag.ordinal.toLong())
                repeat(targetCount) {
                    val point = candidatePoints[random.nextInt(candidatePoints.size)]
                    painted.getOrPut(point) { linkedSetOf() }.add(tag)
                }
            }
        return painted.mapValues { (_, tags) -> tags.toSet() }
    }
}
