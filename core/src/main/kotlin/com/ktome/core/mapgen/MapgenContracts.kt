package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class NodeId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "NodeId must not be blank." }
    }

    override fun toString(): String = value
}

@Serializable
@JvmInline
value class RequirementRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "RequirementRef must not be blank." }
    }

    override fun toString(): String = value
}

@Serializable
enum class PathClass {
    CRITICAL_PATH,
    OPTIONAL,
    SECRET,
}

data class MapgenRequest(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val targetWidth: Int,
    val targetHeight: Int,
) {
    init {
        require(zoneId.isNotBlank()) { "MapgenRequest.zoneId must not be blank." }
        require(floorIndex > 0) { "MapgenRequest.floorIndex must be positive." }
        require(targetWidth > 0) { "MapgenRequest.targetWidth must be positive." }
        require(targetHeight > 0) { "MapgenRequest.targetHeight must be positive." }
    }
}

enum class TerrainTag {
    WATER,
    OIL,
    ICE,
}

data class ZoneMapgenProfile(
    val zoneId: String,
    val allowedBiomeFamilies: Set<String>,
    val loopCountRange: IntRange,
    val vaultPool: Set<String>,
    val terrainTagWeights: Map<TerrainTag, Float>,
    val roomTagFilter: Set<String>,
) {
    init {
        require(zoneId.isNotBlank()) { "ZoneMapgenProfile.zoneId must not be blank." }
        require(allowedBiomeFamilies.all(String::isNotBlank)) { "allowedBiomeFamilies must not contain blank ids." }
        require(vaultPool.all(String::isNotBlank)) { "vaultPool must not contain blank ids." }
        require(roomTagFilter.all(String::isNotBlank)) { "roomTagFilter must not contain blank ids." }
        require(loopCountRange.first >= 0 && loopCountRange.last >= loopCountRange.first) {
            "loopCountRange must be a valid non-negative range."
        }
        require(terrainTagWeights.values.all { weight -> weight >= 0f }) {
            "terrainTagWeights must not contain negative weights."
        }
    }
}

data class TopologyNode(
    val id: NodeId,
    val roomDefId: String,
    val pathClass: PathClass,
    val tags: Set<String>,
) {
    init {
        require(roomDefId.isNotBlank()) { "TopologyNode.roomDefId must not be blank." }
        require(tags.all(String::isNotBlank)) { "TopologyNode.tags must not contain blank entries." }
    }
}

data class TopologyEdge(
    val from: NodeId,
    val to: NodeId,
    val isLoop: Boolean = false,
    val requiredKeys: Set<RequirementRef> = emptySet(),
) {
    init {
        require(from != to) { "TopologyEdge must connect two distinct nodes." }
    }
}

data class TopologyGraph(
    val nodes: List<TopologyNode>,
    val edges: List<TopologyEdge>,
    val primaryPathNodeIds: List<NodeId>,
    val optionalLoopCount: Int,
) {
    init {
        require(nodes.isNotEmpty()) { "TopologyGraph.nodes must not be empty." }
        require(primaryPathNodeIds.isNotEmpty()) { "TopologyGraph.primaryPathNodeIds must not be empty." }
        require(optionalLoopCount >= 0) { "TopologyGraph.optionalLoopCount must not be negative." }

        val nodeIds = nodes.map(TopologyNode::id)
        require(nodeIds.distinct().size == nodeIds.size) { "TopologyGraph.nodes must not contain duplicate ids." }
        require(primaryPathNodeIds.all(nodeIds::contains)) {
            "TopologyGraph.primaryPathNodeIds must refer to declared nodes."
        }
        require(
            edges.all { edge ->
                edge.from in nodeIds && edge.to in nodeIds
            },
        ) {
            "TopologyGraph.edges must only reference declared nodes."
        }
    }
}

data class GeneratedEntrance(
    val id: String,
    val fromNodeId: NodeId,
    val targetNodeId: NodeId,
) {
    init {
        require(id.isNotBlank()) { "GeneratedEntrance.id must not be blank." }
        require(fromNodeId != targetNodeId) { "GeneratedEntrance must connect two distinct nodes." }
    }
}

data class GeneratedFloor(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val topology: TopologyGraph,
    val terrainTags: Map<Point, Set<TerrainTag>>,
    val entrances: List<GeneratedEntrance> = emptyList(),
    val map: GameMap,
) {
    init {
        require(zoneId.isNotBlank()) { "GeneratedFloor.zoneId must not be blank." }
        require(floorIndex > 0) { "GeneratedFloor.floorIndex must be positive." }
        require(
            terrainTags.all { (point, tags) ->
                map.isInBounds(point.x, point.y) && !map.blocksMovement(point.x, point.y) && tags.isNotEmpty()
            },
        ) {
            "GeneratedFloor.terrainTags must only target walkable in-bounds tiles and each placement must have at least one tag."
        }
    }

    companion object {
        fun compatibility(
            zoneId: String,
            floorIndex: Int,
            seed: Long,
            map: GameMap,
            terrainTags: Map<Point, Set<TerrainTag>> = emptyMap(),
            topology: TopologyGraph = LinearTopologyProjector.project(map),
            entrances: List<GeneratedEntrance> = emptyList(),
        ): GeneratedFloor =
            GeneratedFloor(
                zoneId = zoneId,
                floorIndex = floorIndex,
                seed = seed,
                topology = topology,
                terrainTags = terrainTags,
                entrances = entrances,
                map = map,
            )
    }
}

interface ZoneMapgenProfileResolver {
    fun resolve(zoneId: String): ZoneMapgenProfile
}

interface TopologyPlanner {
    fun plan(
        profile: ZoneMapgenProfile,
        request: MapgenRequest,
    ): TopologyGraph
}

interface MapgenPipeline {
    fun run(request: MapgenRequest): GeneratedFloor
}

fun TopologyGraph.isPrimaryPathReachable(): Boolean {
    val adjacency =
        buildMap<NodeId, MutableSet<NodeId>> {
            nodes.forEach { node -> put(node.id, linkedSetOf()) }
            edges.forEach { edge ->
                getValue(edge.from).add(edge.to)
                getValue(edge.to).add(edge.from)
            }
        }
    val start = primaryPathNodeIds.firstOrNull() ?: return false
    val queue = ArrayDeque<NodeId>()
    val visited = linkedSetOf<NodeId>()
    queue.addLast(start)
    visited += start
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        adjacency.getValue(current)
            .asSequence()
            .filterNot(visited::contains)
            .forEach { next ->
                visited += next
                queue.addLast(next)
            }
    }
    return primaryPathNodeIds.all(visited::contains)
}
