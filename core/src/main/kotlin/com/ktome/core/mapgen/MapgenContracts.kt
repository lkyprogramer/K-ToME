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

enum class RoomShape {
    RECT,
    L_SHAPE,
    ROUND,
    IRREGULAR,
}

data class RoomDef(
    val id: String,
    val shape: RoomShape,
    val widthRange: IntRange,
    val heightRange: IntRange,
    val tags: Set<String>,
) {
    init {
        require(id.isNotBlank()) { "RoomDef.id must not be blank." }
        require(widthRange.first > 0 && widthRange.last >= widthRange.first) {
            "RoomDef.widthRange must be a valid positive range."
        }
        require(heightRange.first > 0 && heightRange.last >= heightRange.first) {
            "RoomDef.heightRange must be a valid positive range."
        }
        require(tags.all(String::isNotBlank)) { "RoomDef.tags must not contain blank entries." }
    }
}

data class PatternRoomDef(
    val id: String,
    val baseRoomId: String,
    val patternId: String,
    val requiredTags: Set<String>,
    val spawnWeight: Int,
) {
    init {
        require(id.isNotBlank()) { "PatternRoomDef.id must not be blank." }
        require(baseRoomId.isNotBlank()) { "PatternRoomDef.baseRoomId must not be blank." }
        require(patternId.isNotBlank()) { "PatternRoomDef.patternId must not be blank." }
        require(requiredTags.all(String::isNotBlank)) { "PatternRoomDef.requiredTags must not contain blank entries." }
        require(spawnWeight > 0) { "PatternRoomDef.spawnWeight must be positive." }
    }
}

data class VaultDef(
    val id: String,
    val templateId: String,
    val pathClass: PathClass,
    val threatBudget: Int,
    val rewardBudget: Int,
    val allowOnBiomeFamilies: Set<String>,
    val requiredTerrainTags: Set<TerrainTag>,
) {
    init {
        require(id.isNotBlank()) { "VaultDef.id must not be blank." }
        require(templateId.isNotBlank()) { "VaultDef.templateId must not be blank." }
        require(allowOnBiomeFamilies.isNotEmpty()) { "VaultDef.allowOnBiomeFamilies must not be empty." }
        require(threatBudget >= 0) { "VaultDef.threatBudget must not be negative." }
        require(rewardBudget >= 0) { "VaultDef.rewardBudget must not be negative." }
        require(allowOnBiomeFamilies.all(String::isNotBlank)) {
            "VaultDef.allowOnBiomeFamilies must not contain blank ids."
        }
        require(pathClass != PathClass.CRITICAL_PATH || rewardBudget == 0) {
            "CRITICAL_PATH vaults must keep rewardBudget at 0."
        }
    }
}

data class BiomeFamilyDef(
    val id: String,
    val primaryTileSet: String,
    val secondaryTileSet: String?,
    val terrainTagWeights: Map<TerrainTag, Float>,
    val allowedRoomTags: Set<String>,
) {
    init {
        require(id.isNotBlank()) { "BiomeFamilyDef.id must not be blank." }
        require(primaryTileSet.isNotBlank()) { "BiomeFamilyDef.primaryTileSet must not be blank." }
        require(secondaryTileSet == null || secondaryTileSet.isNotBlank()) {
            "BiomeFamilyDef.secondaryTileSet must not be blank when present."
        }
        require(terrainTagWeights.values.all { weight -> weight >= 0f }) {
            "BiomeFamilyDef.terrainTagWeights must not contain negative weights."
        }
        require(allowedRoomTags.isNotEmpty()) { "BiomeFamilyDef.allowedRoomTags must not be empty." }
        require(allowedRoomTags.all(String::isNotBlank)) {
            "BiomeFamilyDef.allowedRoomTags must not contain blank entries."
        }
    }
}

data class ZoneMapgenProfile(
    val zoneId: String,
    val id: String = zoneId,
    val allowedBiomeFamilies: Set<String>,
    val loopCountRange: IntRange,
    val vaultPool: Set<String>,
    val terrainTagWeights: Map<TerrainTag, Float>,
    val roomTagFilter: Set<String>,
) {
    init {
        require(zoneId.isNotBlank()) { "ZoneMapgenProfile.zoneId must not be blank." }
        require(id.isNotBlank()) { "ZoneMapgenProfile.id must not be blank." }
        require(allowedBiomeFamilies.isNotEmpty()) { "ZoneMapgenProfile.allowedBiomeFamilies must not be empty." }
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

data class ZoneRewardProfile(
    val id: String,
    val zoneId: String,
    val rarityBonus: Float,
    val qualityBonus: Int,
    val baseRewardBudget: Int,
) {
    init {
        require(id.isNotBlank()) { "ZoneRewardProfile.id must not be blank." }
        require(zoneId.isNotBlank()) { "ZoneRewardProfile.zoneId must not be blank." }
        require(rarityBonus >= 0f) { "ZoneRewardProfile.rarityBonus must not be negative." }
        require(qualityBonus >= 0) { "ZoneRewardProfile.qualityBonus must not be negative." }
        require(baseRewardBudget >= 0) { "ZoneRewardProfile.baseRewardBudget must not be negative." }
    }
}

data class TopologyNode(
    val id: NodeId,
    val roomDefId: String,
    val pathClass: PathClass,
    val tags: Set<String>,
    val biomeFamilyId: String? = null,
) {
    init {
        require(roomDefId.isNotBlank()) { "TopologyNode.roomDefId must not be blank." }
        require(tags.all(String::isNotBlank)) { "TopologyNode.tags must not contain blank entries." }
        require(biomeFamilyId == null || biomeFamilyId.isNotBlank()) {
            "TopologyNode.biomeFamilyId must not be blank when present."
        }
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

data class RoomInstance(
    val nodeId: NodeId,
    val roomDefId: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val shape: RoomShape = RoomShape.RECT,
    val pathClass: PathClass = PathClass.CRITICAL_PATH,
    val tags: Set<String> = emptySet(),
    val patternId: String? = null,
    val biomeFamilyId: String? = null,
) {
    init {
        require(roomDefId.isNotBlank()) { "RoomInstance.roomDefId must not be blank." }
        require(width > 0) { "RoomInstance.width must be positive." }
        require(height > 0) { "RoomInstance.height must be positive." }
        require(tags.all(String::isNotBlank)) { "RoomInstance.tags must not contain blank entries." }
        require(patternId == null || patternId.isNotBlank()) { "RoomInstance.patternId must not be blank when present." }
        require(biomeFamilyId == null || biomeFamilyId.isNotBlank()) {
            "RoomInstance.biomeFamilyId must not be blank when present."
        }
    }
}

data class VaultPlacement(
    val vaultId: String,
    val nodeId: NodeId,
    val roomDefId: String,
    val pathClass: PathClass,
    val biomeFamilyId: String,
    val threatBudget: Int,
    val rewardBudget: Int,
    val requiredTerrainTags: Set<TerrainTag>,
) {
    init {
        require(vaultId.isNotBlank()) { "VaultPlacement.vaultId must not be blank." }
        require(roomDefId.isNotBlank()) { "VaultPlacement.roomDefId must not be blank." }
        require(biomeFamilyId.isNotBlank()) { "VaultPlacement.biomeFamilyId must not be blank." }
        require(threatBudget >= 0) { "VaultPlacement.threatBudget must not be negative." }
        require(rewardBudget >= 0) { "VaultPlacement.rewardBudget must not be negative." }
    }
}

data class GeneratedFloor(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val topology: TopologyGraph,
    val rooms: List<RoomInstance> = emptyList(),
    val terrainTags: Map<Point, Set<TerrainTag>>,
    val biomeFamilyIds: List<String> = emptyList(),
    val vaultPlacements: List<VaultPlacement> = emptyList(),
    val entrances: List<GeneratedEntrance> = emptyList(),
    val map: GameMap,
) {
    init {
        val topologyNodeIds = topology.nodes.map(TopologyNode::id).toSet()
        require(zoneId.isNotBlank()) { "GeneratedFloor.zoneId must not be blank." }
        require(floorIndex > 0) { "GeneratedFloor.floorIndex must be positive." }
        require(biomeFamilyIds.all(String::isNotBlank)) { "GeneratedFloor.biomeFamilyIds must not contain blank ids." }
        require(biomeFamilyIds.distinct().size <= 2) {
            "GeneratedFloor.biomeFamilyIds must not contain more than two distinct biome families."
        }
        require(
            terrainTags.all { (point, tags) ->
                map.isInBounds(point.x, point.y) && !map.blocksMovement(point.x, point.y) && tags.isNotEmpty()
            },
        ) {
            "GeneratedFloor.terrainTags must only target walkable in-bounds tiles and each placement must have at least one tag."
        }
        require(rooms.map(RoomInstance::nodeId).toSet().size == rooms.size) {
            "GeneratedFloor.rooms must not contain duplicate node ids."
        }
        require(rooms.all { room -> room.nodeId in topologyNodeIds }) {
            "GeneratedFloor.rooms must reference declared topology nodes."
        }
        require(rooms.all { room -> map.isInBounds(room.x, room.y) && map.isInBounds(room.x + room.width - 1, room.y + room.height - 1) }) {
            "GeneratedFloor.rooms must remain inside map bounds."
        }
        require(vaultPlacements.all { placement -> placement.nodeId in topologyNodeIds }) {
            "GeneratedFloor.vaultPlacements must reference declared topology nodes."
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
            rooms: List<RoomInstance> = compatibilityRooms(map = map, topology = topology),
            biomeFamilyIds: List<String> = emptyList(),
            vaultPlacements: List<VaultPlacement> = emptyList(),
            entrances: List<GeneratedEntrance> = emptyList(),
        ): GeneratedFloor =
            GeneratedFloor(
                zoneId = zoneId,
                floorIndex = floorIndex,
                seed = seed,
                topology = topology,
                rooms = rooms,
                terrainTags = terrainTags,
                biomeFamilyIds = biomeFamilyIds,
                vaultPlacements = vaultPlacements,
                entrances = entrances,
                map = map,
            )

        private fun compatibilityRooms(
            map: GameMap,
            topology: TopologyGraph,
        ): List<RoomInstance> = CompatibilityRoomProjector.project(map = map, topology = topology)
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

fun TopologyGraph.loopEdgeCount(): Int = edges.count(TopologyEdge::isLoop)

fun TopologyGraph.loopEdgeRatio(): Double =
    if (edges.isEmpty()) {
        0.0
    } else {
        loopEdgeCount().toDouble() / edges.size.toDouble()
    }
