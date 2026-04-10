package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.world.solvability.ContentRef
import com.ktome.core.world.solvability.DiscoveryRule
import com.ktome.core.world.solvability.KeyType
import com.ktome.core.world.solvability.NodeAnchorId
import com.ktome.core.world.solvability.ResolvedEntranceBinding
import com.ktome.core.world.solvability.SearchBindingId
import kotlinx.serialization.Serializable
import kotlin.math.max

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

val ALLOWED_VAULT_REQUIRED_TERRAIN_TAGS: Set<TerrainTag> =
    setOf(
        TerrainTag.WATER,
        TerrainTag.OIL,
        TerrainTag.ICE,
    )

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
        require(requiredTerrainTags.all(ALLOWED_VAULT_REQUIRED_TERRAIN_TAGS::contains)) {
            "VaultDef.requiredTerrainTags must stay within the supported vault terrain contract."
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
    val keyGatePlans: List<KeyGatePlan> = emptyList(),
    val hiddenEntrancePlans: List<HiddenEntrancePlan> = emptyList(),
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

data class KeyGatePlan(
    val id: String,
    val fromAnchorId: NodeAnchorId,
    val toAnchorId: NodeAnchorId,
    val grantedByAnchorId: NodeAnchorId,
    val keyType: KeyType,
    val keyId: String,
) {
    init {
        require(id.isNotBlank()) { "KeyGatePlan.id must not be blank." }
        require(keyId.isNotBlank()) { "KeyGatePlan.keyId must not be blank." }
    }

    val requirementRef: RequirementRef
        get() = RequirementRef("${keyType.name}:$keyId")
}

val FORMAL_HIDDEN_ENTRANCE_ANCHOR_IDS: Set<NodeAnchorId> =
    linkedSetOf(
        NodeAnchorId("hidden.branch"),
        NodeAnchorId("hidden.critical.adjacent"),
        NodeAnchorId("hidden.goal.adjacent"),
    )

fun NodeAnchorId.isFormalHiddenEntranceAnchor(): Boolean = this in FORMAL_HIDDEN_ENTRANCE_ANCHOR_IDS

data class HiddenEntrancePlan(
    val bindingId: SearchBindingId,
    val sourceAnchorId: NodeAnchorId,
    val entranceAnchorId: NodeAnchorId,
    val targetAnchorId: NodeAnchorId,
    val targetSecretZoneId: ContentRef,
    val discoveryRule: DiscoveryRule,
    val pathClass: PathClass = PathClass.SECRET,
) {
    init {
        require(pathClass == PathClass.SECRET) {
            "HiddenEntrancePlan.pathClass must remain SECRET in PR-03."
        }
        require(sourceAnchorId == entranceAnchorId) {
            "HiddenEntrancePlan.entranceAnchorId must match sourceAnchorId in PR-03."
        }
        require(sourceAnchorId.isFormalHiddenEntranceAnchor()) {
            "HiddenEntrancePlan.sourceAnchorId must use a formal hidden entrance anchor family."
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
    val anchorId: NodeAnchorId,
    val roomDefId: String,
    val pathClass: PathClass,
    val tags: Set<String>,
    val grants: Set<RequirementRef> = emptySet(),
    val biomeFamilyId: String? = null,
) {
    init {
        require(roomDefId.isNotBlank()) { "TopologyNode.roomDefId must not be blank." }
        require(tags.all(String::isNotBlank)) { "TopologyNode.tags must not contain blank entries." }
        require(grants.all { grant -> grant.value.isNotBlank() }) {
            "TopologyNode.grants must not contain blank requirement refs."
        }
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
    val bindingId: SearchBindingId,
    val fromNodeId: NodeId,
    val targetNodeId: NodeId,
    val entranceAnchorId: NodeAnchorId,
    val targetAnchorId: NodeAnchorId,
    val pathClass: PathClass,
    val discoveryRule: DiscoveryRule,
    val targetSecretZoneId: ContentRef,
    val resolvedReturnBridgeNodeId: NodeId = fromNodeId,
) {
    init {
        require(fromNodeId != targetNodeId) { "GeneratedEntrance must connect two distinct nodes." }
        require(pathClass == PathClass.SECRET) { "GeneratedEntrance.pathClass must remain SECRET." }
    }
}

data class RoomInstance(
    val nodeId: NodeId,
    val anchorId: NodeAnchorId,
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

val RoomInstance.center: Point
    get() = Point(x + (width / 2), y + (height / 2))

fun RoomInstance.contains(point: Point): Boolean =
    when (shape) {
        RoomShape.RECT -> point.x in x until x + width && point.y in y until y + height
        RoomShape.L_SHAPE -> {
            val splitX = x + max(2, width / 2)
            val inVerticalArm = point.x in x until splitX && point.y in y until y + height
            val inHorizontalArm = point.x in x until x + width && point.y in y + height / 2 until y + height
            inVerticalArm || inHorizontalArm
        }

        RoomShape.ROUND -> {
            val centerX = x + width / 2.0
            val centerY = y + height / 2.0
            val radiusX = max(1.5, width / 2.2)
            val radiusY = max(1.5, height / 2.2)
            val normalizedX = ((point.x + 0.5) - centerX) / radiusX
            val normalizedY = ((point.y + 0.5) - centerY) / radiusY
            (normalizedX * normalizedX) + (normalizedY * normalizedY) <= 1.0
        }

        RoomShape.IRREGULAR -> {
            point.x in x until x + width &&
                point.y in y until y + height &&
                point !in listOf(
                    Point(x + 1, y + 1),
                    Point(x + width - 2, y + 1),
                    Point(x + 1, y + height - 2),
                    Point(x + width - 2, y + height - 2),
                )
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
        val topologyAnchorIds = topology.nodes.map(TopologyNode::anchorId).toSet()
        val roomAnchorIds = rooms.map(RoomInstance::anchorId).toSet()
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
        require(entrances.all { entrance -> entrance.fromNodeId in topologyNodeIds && entrance.targetNodeId in topologyNodeIds }) {
            "GeneratedFloor.entrances must reference declared topology nodes."
        }
        require(entrances.all { entrance -> entrance.entranceAnchorId in roomAnchorIds }) {
            "GeneratedFloor.entrances must bind to an instantiated room anchor."
        }
        require(entrances.all { entrance -> entrance.targetAnchorId in topologyAnchorIds }) {
            "GeneratedFloor.entrances must reference declared topology anchors."
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

    fun resolvedEntranceBindings(): List<ResolvedEntranceBinding> =
        entrances.map { entrance ->
            ResolvedEntranceBinding(
                searchBindingId = entrance.bindingId,
                entranceAnchorId = entrance.entranceAnchorId,
                resolvedTargetNodeId = entrance.targetNodeId,
            )
        }

    fun roomAt(point: Point): RoomInstance? = rooms.firstOrNull { room -> room.contains(point) }

    fun roomByAnchor(anchorId: NodeAnchorId): RoomInstance? {
        val matches = rooms.filter { room -> room.anchorId == anchorId }
        require(matches.size <= 1) { "GeneratedFloor.roomByAnchor must not resolve duplicate room anchors '$anchorId'." }
        return matches.firstOrNull()
    }

    fun entranceByBinding(bindingId: SearchBindingId): GeneratedEntrance? {
        val matches = entrances.filter { entrance -> entrance.bindingId == bindingId }
        require(matches.size <= 1) { "GeneratedFloor.entranceByBinding must not resolve duplicate binding ids '$bindingId'." }
        return matches.firstOrNull()
    }

    fun roomForEntrance(entrance: GeneratedEntrance): RoomInstance? = roomByAnchor(entrance.entranceAnchorId)
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
