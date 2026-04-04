package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.map.TileType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class HybridTopologyPlanner(
    private val roomDefsById: Map<String, RoomDef>,
    private val biomeFamiliesById: Map<String, BiomeFamilyDef>,
) : TopologyPlanner {
    private val biomeMatchTagsById: Map<String, Set<String>> = biomeFamiliesById.keys.associateWith(::resolveBiomeFamilyMatchTags)
    private val knownBiomeThemeTags: Set<String> = biomeMatchTagsById.values.flatten().toSet()
    private val themedTagsByRoomDefId: Map<String, Set<String>> =
        roomDefsById.mapValues { (_, roomDef) ->
            roomDef.tags.intersect(knownBiomeThemeTags)
        }

    override fun plan(
        profile: ZoneMapgenProfile,
        request: MapgenRequest,
    ): TopologyGraph {
        val selectedFamilies = selectBiomeFamilies(profile.allowedBiomeFamilies, request.seed)
        val loopCount = sampleLoopCount(profile.loopCountRange, request.seed).coerceAtMost(2)
        val primaryNodeCount = if (request.targetWidth >= 80) 5 else 4
        val optionalNodeCount =
            when {
                loopCount > 0 -> loopCount
                profile.vaultPool.isNotEmpty() -> 1
                else -> 0
            }

        val primaryNodes =
            (0 until primaryNodeCount).map { index ->
                val roleTag =
                    when (index) {
                        0 -> "start"
                        primaryNodeCount - 1 -> "goal"
                        primaryNodeCount / 2 -> "hub"
                        else -> "route"
                    }
                val nodeBiomeFamilyId =
                    selectNodeBiomeFamily(
                        selectedFamilies = selectedFamilies,
                        pathClass = PathClass.CRITICAL_PATH,
                        ordinal = index,
                    )
                val candidateTags = resolveCandidateRoomTags(profile = profile, biomeFamilyId = nodeBiomeFamilyId)
                TopologyNode(
                    id = NodeId("primary-$index"),
                    roomDefId =
                        chooseRoomDefId(
                            biomeFamilyId = nodeBiomeFamilyId,
                            pathClass = PathClass.CRITICAL_PATH,
                            roleTag = roleTag,
                            candidateTags = candidateTags,
                            seed = request.seed,
                            salt = "$index",
                        ),
                    pathClass = PathClass.CRITICAL_PATH,
                    tags = buildSet {
                        add(roleTag)
                        add("critical")
                        if (roleTag == "hub") {
                            add("pattern_candidate")
                        }
                    },
                    biomeFamilyId = nodeBiomeFamilyId,
                )
            }

        val optionalNodes =
            (0 until optionalNodeCount).map { index ->
                val nodeBiomeFamilyId =
                    selectNodeBiomeFamily(
                        selectedFamilies = selectedFamilies,
                        pathClass = PathClass.OPTIONAL,
                        ordinal = primaryNodeCount + index,
                    )
                val candidateTags = resolveCandidateRoomTags(profile = profile, biomeFamilyId = nodeBiomeFamilyId)
                TopologyNode(
                    id = NodeId("optional-$index"),
                    roomDefId =
                        chooseRoomDefId(
                            biomeFamilyId = nodeBiomeFamilyId,
                            pathClass = PathClass.OPTIONAL,
                            roleTag = "optional",
                            candidateTags = candidateTags,
                            seed = request.seed,
                            salt = "optional-$index",
                        ),
                    pathClass = PathClass.OPTIONAL,
                    tags = buildSet {
                        add("optional")
                        add("vault_candidate")
                        add("pattern_candidate")
                    },
                    biomeFamilyId = nodeBiomeFamilyId,
                )
            }

        val edges =
            buildList {
                primaryNodes.zipWithNext { left, right ->
                    add(TopologyEdge(from = left.id, to = right.id))
                }
                optionalNodes.forEachIndexed { index, node ->
                    val attachIndex = (primaryNodes.lastIndex - 1 - index).coerceAtLeast(1)
                    val attachNode = primaryNodes[attachIndex]
                    val reconnectNode = primaryNodes.last()
                    add(TopologyEdge(from = attachNode.id, to = node.id))
                    add(TopologyEdge(from = node.id, to = reconnectNode.id, isLoop = true))
                }
            }

        return TopologyGraph(
            nodes = primaryNodes + optionalNodes,
            edges = edges,
            primaryPathNodeIds = primaryNodes.map(TopologyNode::id),
            optionalLoopCount = optionalNodes.size,
        )
    }

    private fun resolveCandidateRoomTags(
        profile: ZoneMapgenProfile,
        biomeFamilyId: String,
    ): Set<String> {
        val familyTags = biomeFamiliesById[biomeFamilyId]?.allowedRoomTags.orEmpty()
        require(familyTags.isNotEmpty()) {
            "HybridTopologyPlanner could not resolve room tags for biome family '$biomeFamilyId'."
        }
        if (profile.roomTagFilter.isEmpty()) {
            return familyTags
        }
        return familyTags.intersect(profile.roomTagFilter).also { filteredTags ->
            require(filteredTags.isNotEmpty()) {
                "Zone mapgen profile '${profile.id}' roomTagFilter ${profile.roomTagFilter} does not overlap biome family '$biomeFamilyId'."
            }
        }
    }

    private fun chooseRoomDefId(
        biomeFamilyId: String,
        pathClass: PathClass,
        roleTag: String,
        candidateTags: Set<String>,
        seed: Long,
        salt: String,
    ): String {
        val roleCandidates =
            roomDefsById.values
                .filter { roomDef ->
                    roleTag in roomDef.tags || "general" in roomDef.tags || (pathClass == PathClass.OPTIONAL && "optional" in roomDef.tags)
                }
                .filter { roomDef -> roomDefMatchesBiomeFamily(roomDef = roomDef, biomeFamilyId = biomeFamilyId) }
                .filter { roomDef -> roomDef.tags.any(candidateTags::contains) }
                .sortedBy(RoomDef::id)
        require(roleCandidates.isNotEmpty()) {
            "HybridTopologyPlanner found no RoomDef for role '$roleTag', biomeFamily '$biomeFamilyId', pathClass '$pathClass', and candidate tags ${candidateTags.sorted()}."
        }
        return roleCandidates[stableRandom(seed = seed, salt = "$roleTag:$salt").nextInt(roleCandidates.size)].id
    }

    private fun roomDefMatchesBiomeFamily(
        roomDef: RoomDef,
        biomeFamilyId: String,
    ): Boolean {
        val themedTags = themedTagsByRoomDefId.getValue(roomDef.id)
        if (themedTags.isEmpty()) {
            return true
        }
        val acceptedTags = biomeMatchTagsById[biomeFamilyId] ?: resolveBiomeFamilyMatchTags(biomeFamilyId)
        return themedTags.any(acceptedTags::contains)
    }

    private fun resolveBiomeFamilyMatchTags(biomeFamilyId: String): Set<String> =
        if (biomeFamilyId.startsWith("fallback.")) {
            setOf(biomeFamilyId, biomeFamilyId.removePrefix("fallback."))
        } else {
            setOf(biomeFamilyId)
        }
}

class HybridTopologyMapgenPipeline(
    private val profileResolver: ZoneMapgenProfileResolver,
    private val roomDefsById: Map<String, RoomDef>,
    private val patternTemplatesById: Map<String, PatternTemplateDef>,
    private val patternRooms: List<PatternRoomDef>,
    private val vaultTemplatesById: Map<String, VaultTemplateDef>,
    private val vaultDefsById: Map<String, VaultDef>,
    private val biomeFamiliesById: Map<String, BiomeFamilyDef>,
    private val planner: TopologyPlanner = HybridTopologyPlanner(roomDefsById = roomDefsById, biomeFamiliesById = biomeFamiliesById),
) : MapgenPipeline {
    constructor(
        profileResolver: ZoneMapgenProfileResolver,
        contentCatalog: MapgenContentCatalog,
    ) : this(
        profileResolver = profileResolver,
        roomDefsById = contentCatalog.roomDefs.associateBy(RoomDef::id),
        patternTemplatesById = contentCatalog.patternTemplates,
        patternRooms = contentCatalog.patternRooms,
        vaultTemplatesById = contentCatalog.vaultTemplates,
        vaultDefsById = contentCatalog.vaultDefs.associateBy(VaultDef::id),
        biomeFamiliesById = contentCatalog.biomeFamilies.associateBy(BiomeFamilyDef::id),
    )

    constructor(
        profileResolver: ZoneMapgenProfileResolver,
        roomDefs: List<RoomDef>,
        patternTemplates: List<PatternTemplateDef>,
        patternRooms: List<PatternRoomDef>,
        vaultTemplates: List<VaultTemplateDef>,
        vaults: List<VaultDef>,
        biomeFamilies: List<BiomeFamilyDef>,
    ) : this(
        profileResolver = profileResolver,
        contentCatalog =
            MapgenContentCatalog(
                roomDefs = roomDefs,
                patternRooms = patternRooms,
                patternTemplates = patternTemplates.associateBy(PatternTemplateDef::id),
                vaultDefs = vaults,
                vaultTemplates = vaultTemplates.associateBy(VaultTemplateDef::id),
                biomeFamilies = biomeFamilies,
            ),
    )

    override fun run(request: MapgenRequest): GeneratedFloor {
        val profile = profileResolver.resolve(request.zoneId)
        val topology = planner.plan(profile = profile, request = request)
        val roomInstances = instantiateRooms(topology = topology, request = request)
        val builder = GameMap.Builder(request.targetWidth, request.targetHeight)
        roomInstances.forEach { room -> carveRoomShape(builder = builder, room = room, shape = roomDefsById.getValue(room.roomDefId).shape) }
        val roomBoundsByNodeId = roomInstances.associate { room -> room.nodeId to Room(room.x, room.y, room.width, room.height) }
        topology.edges.forEach { edge ->
            carveCorridor(
                builder = builder,
                from = roomBoundsByNodeId.getValue(edge.from).center,
                to = roomBoundsByNodeId.getValue(edge.to).center,
                random = stableRandom(request.seed, "corridor:${edge.from.value}:${edge.to.value}:${edge.isLoop}"),
            )
        }

        val patternedRooms = applyPatternRooms(builder = builder, topology = topology, rooms = roomInstances, request = request)
        val vaultPlacements = applyVaults(builder = builder, topology = topology, rooms = patternedRooms, profile = profile, request = request)
        topology.edges.forEach { edge ->
            carveCorridor(
                builder = builder,
                from = roomBoundsByNodeId.getValue(edge.from).center,
                to = roomBoundsByNodeId.getValue(edge.to).center,
                random = stableRandom(request.seed, "corridor:${edge.from.value}:${edge.to.value}:${edge.isLoop}"),
            )
        }
        ensureAnchorsWalkable(builder = builder, topology = topology, rooms = patternedRooms)

        val map =
            builder.build(
                rooms = patternedRooms.map { room -> Room(room.x, room.y, room.width, room.height) },
                playerStart = Room(patternedRooms.first().x, patternedRooms.first().y, patternedRooms.first().width, patternedRooms.first().height).center,
            )
        val terrainTags =
            paintTerrainTags(
                map = map,
                profile = profile,
                rooms = patternedRooms,
                vaultPlacements = vaultPlacements,
                request = request,
            )
        return GeneratedFloor(
            zoneId = request.zoneId,
            floorIndex = request.floorIndex,
            seed = request.seed,
            topology = topology,
            rooms = patternedRooms,
            terrainTags = terrainTags,
            biomeFamilyIds = patternedRooms.mapNotNull(RoomInstance::biomeFamilyId).distinct(),
            vaultPlacements = vaultPlacements,
            entrances = emptyList(),
            map = map,
        )
    }

    private fun instantiateRooms(
        topology: TopologyGraph,
        request: MapgenRequest,
    ): List<RoomInstance> {
        val nodesById = topology.nodes.associateBy(TopologyNode::id)
        val primaryNodes = topology.primaryPathNodeIds.map(nodesById::getValue)
        val primaryCount = primaryNodes.size
        val segmentWidth = max(12, (request.targetWidth - 8) / primaryCount)
        val baseY = ((request.targetHeight / 2) - 5).coerceAtLeast(3)
        val rooms = mutableListOf<RoomInstance>()

        primaryNodes.forEachIndexed { index, node ->
            val roomDef = roomDefsById.getValue(node.roomDefId)
            val width = sampleDimension(roomDef.widthRange, maxValue = segmentWidth - 2, random = stableRandom(request.seed, "width:${node.id.value}"))
            val height = sampleDimension(roomDef.heightRange, maxValue = (request.targetHeight / 3).coerceAtLeast(roomDef.heightRange.first), random = stableRandom(request.seed, "height:${node.id.value}"))
            val x = (2 + (index * segmentWidth) + ((segmentWidth - width) / 2)).coerceIn(1, request.targetWidth - width - 2)
            val yJitter = stableRandom(request.seed, "y:${node.id.value}").nextInt(-2, 3)
            val y = (baseY + yJitter).coerceIn(2, request.targetHeight - height - 2)
            rooms +=
                RoomInstance(
                    nodeId = node.id,
                    roomDefId = node.roomDefId,
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    shape = roomDef.shape,
                    pathClass = node.pathClass,
                    tags = node.tags,
                    biomeFamilyId = node.biomeFamilyId,
                )
        }

        topology.nodes
            .filterNot { node -> node.id in topology.primaryPathNodeIds }
            .sortedBy { node -> node.id.value }
            .forEachIndexed { index, node ->
                val anchorRoom = rooms[(rooms.lastIndex - 1 - index).coerceAtLeast(1)]
                val roomDef = roomDefsById.getValue(node.roomDefId)
                val width = sampleDimension(roomDef.widthRange, maxValue = (request.targetWidth / 5).coerceAtLeast(roomDef.widthRange.first), random = stableRandom(request.seed, "width:${node.id.value}"))
                val height = sampleDimension(roomDef.heightRange, maxValue = (request.targetHeight / 4).coerceAtLeast(roomDef.heightRange.first), random = stableRandom(request.seed, "height:${node.id.value}"))
                val branchUp = stableRandom(request.seed, "branch:${node.id.value}").nextBoolean()
                val candidateX = (anchorRoom.x + (anchorRoom.width / 2) - (width / 2)).coerceIn(2, request.targetWidth - width - 2)
                val candidateY =
                    if (branchUp) {
                        anchorRoom.y - height - 4
                    } else {
                        anchorRoom.y + anchorRoom.height + 4
                    }
                val boundedY = candidateY.coerceIn(2, request.targetHeight - height - 2)
                rooms +=
                    adjustPlacement(
                        existingRooms = rooms,
                        candidate =
                            RoomInstance(
                                nodeId = node.id,
                                roomDefId = node.roomDefId,
                                x = candidateX,
                                y = boundedY,
                                width = width,
                                height = height,
                                shape = roomDef.shape,
                                pathClass = node.pathClass,
                                tags = node.tags,
                                biomeFamilyId = node.biomeFamilyId,
                            ),
                        maxWidth = request.targetWidth,
                        maxHeight = request.targetHeight,
                    )
            }
        return rooms
    }

    private fun applyPatternRooms(
        builder: GameMap.Builder,
        topology: TopologyGraph,
        rooms: List<RoomInstance>,
        request: MapgenRequest,
    ): List<RoomInstance> {
        val nodesById = topology.nodes.associateBy(TopologyNode::id)
        val mutableRooms = rooms.toMutableList()
        val eligibleIndices =
            mutableRooms.indices.filter { index ->
                val room = mutableRooms[index]
                val node = nodesById.getValue(room.nodeId)
                "pattern_candidate" in node.tags && "vault_candidate" !in node.tags
            }
        var assigned = false
        eligibleIndices.forEach { index ->
            val room = mutableRooms[index]
            val node = nodesById.getValue(room.nodeId)
            val eligiblePatterns = eligiblePatternsFor(room = room, node = node)
            if (eligiblePatterns.isEmpty()) {
                return@forEach
            }
            val shouldAssign =
                stableRandom(request.seed, "pattern:${room.nodeId.value}").nextInt(100) < 65 ||
                    (!assigned && index == eligibleIndices.last())
            if (!shouldAssign) {
                return@forEach
            }
            val chosen =
                weightedPick(
                    candidates = eligiblePatterns,
                    weightOf = PatternRoomDef::spawnWeight,
                    random = stableRandom(request.seed, "pattern-pick:${room.nodeId.value}"),
                )
            applyPatternTemplate(builder = builder, room = room, patternId = chosen.patternId)
            mutableRooms[index] = room.copy(patternId = chosen.patternId)
            assigned = true
        }
        return mutableRooms
    }

    private fun applyVaults(
        builder: GameMap.Builder,
        topology: TopologyGraph,
        rooms: List<RoomInstance>,
        profile: ZoneMapgenProfile,
        request: MapgenRequest,
    ): List<VaultPlacement> {
        val nodesById = topology.nodes.associateBy(TopologyNode::id)
        return rooms
            .asSequence()
            .filter { room -> nodesById.getValue(room.nodeId).pathClass == PathClass.OPTIONAL }
            .mapNotNull { room ->
                val roomBiomeFamilyId = room.biomeFamilyId ?: return@mapNotNull null
                val candidates =
                    profile.vaultPool
                        .mapNotNull(vaultDefsById::get)
                        .filter { vault ->
                            vault.pathClass == PathClass.OPTIONAL &&
                                roomBiomeFamilyId in vault.allowOnBiomeFamilies
                        }
                        .sortedBy(VaultDef::id)
                if (candidates.isEmpty()) {
                    return@mapNotNull null
                }
                val chosen = candidates[stableRandom(request.seed, "vault:${room.nodeId.value}").nextInt(candidates.size)]
                applyVaultTemplate(builder = builder, room = room, templateId = chosen.templateId)
                VaultPlacement(
                    vaultId = chosen.id,
                    nodeId = room.nodeId,
                    roomDefId = room.roomDefId,
                    pathClass = chosen.pathClass,
                    biomeFamilyId = roomBiomeFamilyId,
                    threatBudget = chosen.threatBudget,
                    rewardBudget = chosen.rewardBudget,
                    requiredTerrainTags = chosen.requiredTerrainTags,
                )
            }.toList()
    }

    private fun ensureAnchorsWalkable(
        builder: GameMap.Builder,
        topology: TopologyGraph,
        rooms: List<RoomInstance>,
    ) {
        val roomsByNodeId = rooms.associate { room -> room.nodeId to Room(room.x, room.y, room.width, room.height) }
        topology.nodes.forEach { node ->
            val center = roomsByNodeId.getValue(node.id).center
            builder.setTile(center, TileType.FLOOR)
            listOf(
                Point(center.x + 1, center.y),
                Point(center.x - 1, center.y),
                Point(center.x, center.y + 1),
                Point(center.x, center.y - 1),
            ).forEach { point ->
                if (point.x > 0 && point.y > 0) {
                    builder.setTile(point, TileType.FLOOR)
                }
            }
        }
    }

    private fun paintTerrainTags(
        map: GameMap,
        profile: ZoneMapgenProfile,
        rooms: List<RoomInstance>,
        vaultPlacements: List<VaultPlacement>,
        request: MapgenRequest,
    ): Map<Point, Set<TerrainTag>> {
        val painted = linkedMapOf<Point, MutableSet<TerrainTag>>()
        val roomsByNodeId = rooms.associateBy(RoomInstance::nodeId)
        rooms.forEachIndexed { ordinal, room ->
            val weights =
                linkedMapOf<TerrainTag, Float>().apply {
                    room.biomeFamilyId?.let { familyId ->
                        biomeFamiliesById[familyId]?.terrainTagWeights?.forEach { (tag, weight) ->
                            this[tag] = weight
                        }
                    }
                    profile.terrainTagWeights.forEach { (tag, weight) ->
                        this[tag] = weight
                    }
                }
            if (weights.isEmpty()) {
                return@forEachIndexed
            }
            val candidatePoints = floorPointsInRoom(room = room, map = map).filterNot { point -> point == map.playerStart }
            val totalWeight = weights.values.sum()
            if (candidatePoints.isEmpty() || totalWeight <= 0f) {
                return@forEachIndexed
            }
            val sortedCandidatePoints = candidatePoints.sortedWith(compareBy<Point>(Point::y).thenBy(Point::x))
            weights.entries
                .sortedBy { (tag, _) -> tag.ordinal }
                .forEach { (tag, weight) ->
                    if (weight <= 0f) {
                        return@forEach
                    }
                    val ratio = weight / totalWeight
                    val targetCount = max(1, (candidatePoints.size * ratio * 0.14f).roundToInt())
                    val random = stableRandom(request.seed, "terrain:${room.nodeId.value}:${ordinal}:${tag.name}")
                    repeat(targetCount) {
                        val point = candidatePoints[random.nextInt(candidatePoints.size)]
                        painted.getOrPut(point) { linkedSetOf() }.add(tag)
                    }
            }
            val terrainHints = terrainHintsForPattern(room.patternId)
            terrainHints.forEachIndexed { index, tag ->
                if (sortedCandidatePoints.isNotEmpty()) {
                    val point =
                        sortedCandidatePoints[
                            (index * max(1, sortedCandidatePoints.size / max(1, terrainHints.size))) % sortedCandidatePoints.size
                        ]
                    painted.getOrPut(point) { linkedSetOf() }.add(tag)
                }
            }
        }
        vaultPlacements.forEach { vault ->
            val room = roomsByNodeId[vault.nodeId] ?: return@forEach
            val candidatePoints = floorPointsInRoom(room = room, map = map)
            if (candidatePoints.isEmpty()) {
                return@forEach
            }
            val sortedPoints = candidatePoints.sortedWith(compareBy<Point>(Point::y).thenBy(Point::x))
            vault.requiredTerrainTags.sortedBy(TerrainTag::ordinal).forEachIndexed { index, tag ->
                val point = sortedPoints[(index * max(1, sortedPoints.size / max(1, vault.requiredTerrainTags.size))) % sortedPoints.size]
                painted.getOrPut(point) { linkedSetOf() }.add(tag)
            }
        }
        return painted.mapValues { (_, tags) -> tags.toSet() }
    }

    private fun eligiblePatternsFor(
        room: RoomInstance,
        node: TopologyNode,
    ): List<PatternRoomDef> {
        val roomTags =
            buildSet {
                addAll(roomDefsById.getValue(room.roomDefId).tags)
                addAll(node.tags)
                room.biomeFamilyId?.let { familyId ->
                    addAll(biomeFamiliesById[familyId]?.allowedRoomTags.orEmpty())
                }
            }
        return patternRooms
            .filter { pattern ->
                pattern.baseRoomId == room.roomDefId && pattern.requiredTags.all(roomTags::contains)
            }.sortedBy(PatternRoomDef::id)
    }

    private fun applyPatternTemplate(
        builder: GameMap.Builder,
        room: RoomInstance,
        patternId: String,
    ) {
        val template = requireNotNull(patternTemplatesById[patternId]) { "Missing pattern template '$patternId'." }
        applyTemplateRows(builder = builder, room = room, rows = template.rows)
    }

    private fun applyVaultTemplate(
        builder: GameMap.Builder,
        room: RoomInstance,
        templateId: String,
    ) {
        val template = requireNotNull(vaultTemplatesById[templateId]) { "Missing vault template '$templateId'." }
        applyTemplateRows(builder = builder, room = room, rows = template.rows)
    }

    private fun applyTemplateRows(
        builder: GameMap.Builder,
        room: RoomInstance,
        rows: List<String>,
    ) {
        val innerWidth = max(1, room.width - 2)
        val innerHeight = max(1, room.height - 2)
        for (dy in 0 until innerHeight) {
            for (dx in 0 until innerWidth) {
                val point = Point(room.x + 1 + dx, room.y + 1 + dy)
                if (!roomContainsPoint(room = room, point = point)) {
                    continue
                }
                val glyph = templateGlyph(rows = rows, x = dx, y = dy, width = innerWidth, height = innerHeight)
                when (glyph) {
                    '.' -> builder.setTile(point, TileType.FLOOR)
                    '#', '~', 'o', 'O', '*' -> builder.setTile(point, TileType.WALL)
                    else -> error("Unsupported mapgen template glyph '$glyph'.")
                }
            }
        }
    }

    private fun carveRoomShape(
        builder: GameMap.Builder,
        room: RoomInstance,
        shape: RoomShape,
    ) {
        when (shape) {
            RoomShape.RECT -> {
                carveRect(builder = builder, room = room)
            }

            RoomShape.L_SHAPE -> {
                val splitX = room.x + max(2, room.width / 2)
                for (y in room.y..room.y + room.height - 1) {
                    for (x in room.x until splitX) {
                        builder.setTile(Point(x, y), TileType.FLOOR)
                    }
                }
                for (y in room.y + room.height / 2..room.y + room.height - 1) {
                    for (x in room.x..room.x + room.width - 1) {
                        builder.setTile(Point(x, y), TileType.FLOOR)
                    }
                }
            }

            RoomShape.ROUND -> {
                val centerX = room.x + room.width / 2.0
                val centerY = room.y + room.height / 2.0
                val radiusX = max(1.5, room.width / 2.2)
                val radiusY = max(1.5, room.height / 2.2)
                for (y in room.y..room.y + room.height - 1) {
                    for (x in room.x..room.x + room.width - 1) {
                        val normalizedX = ((x + 0.5) - centerX) / radiusX
                        val normalizedY = ((y + 0.5) - centerY) / radiusY
                        if ((normalizedX * normalizedX) + (normalizedY * normalizedY) <= 1.0) {
                            builder.setTile(Point(x, y), TileType.FLOOR)
                        }
                    }
                }
            }

            RoomShape.IRREGULAR -> {
                carveRect(builder = builder, room = room)
                listOf(
                    Point(room.x + 1, room.y + 1),
                    Point(room.x + room.width - 2, room.y + 1),
                    Point(room.x + 1, room.y + room.height - 2),
                    Point(room.x + room.width - 2, room.y + room.height - 2),
                ).forEach { point -> builder.setTile(point, TileType.WALL) }
            }
        }
    }

    private fun carveRect(
        builder: GameMap.Builder,
        room: RoomInstance,
    ) {
        for (y in room.y..room.y + room.height - 1) {
            for (x in room.x..room.x + room.width - 1) {
                builder.setTile(Point(x, y), TileType.FLOOR)
            }
        }
    }

    private fun carveCorridor(
        builder: GameMap.Builder,
        from: Point,
        to: Point,
        random: Random,
    ) {
        if (random.nextBoolean()) {
            carveHorizontal(builder = builder, startX = from.x, endX = to.x, y = from.y)
            carveVertical(builder = builder, startY = from.y, endY = to.y, x = to.x)
        } else {
            carveVertical(builder = builder, startY = from.y, endY = to.y, x = from.x)
            carveHorizontal(builder = builder, startX = from.x, endX = to.x, y = to.y)
        }
    }

    private fun carveHorizontal(
        builder: GameMap.Builder,
        startX: Int,
        endX: Int,
        y: Int,
    ) {
        val range = if (startX <= endX) startX..endX else endX..startX
        range.forEach { x -> builder.setTile(Point(x, y), TileType.FLOOR) }
    }

    private fun carveVertical(
        builder: GameMap.Builder,
        startY: Int,
        endY: Int,
        x: Int,
    ) {
        val range = if (startY <= endY) startY..endY else endY..startY
        range.forEach { y -> builder.setTile(Point(x, y), TileType.FLOOR) }
    }

    private fun floorPointsInRoom(
        room: RoomInstance,
        map: GameMap,
    ): List<Point> =
        buildList {
            for (y in room.y..room.y + room.height - 1) {
                for (x in room.x..room.x + room.width - 1) {
                    val point = Point(x, y)
                    if (map.isInBounds(x, y) && !map.blocksMovement(x, y)) {
                        add(point)
                    }
                }
            }
        }

    private fun terrainHintsForPattern(patternId: String?): Set<TerrainTag> {
        patternId ?: return emptySet()
        return patternTemplatesById[patternId]
            ?.rows
            ?.flatMap { row -> row.mapNotNull(::terrainTagForGlyph) }
            ?.toSet()
            .orEmpty()
    }
}

private fun roomContainsPoint(
    room: RoomInstance,
    point: Point,
): Boolean =
    when (room.shape) {
        RoomShape.RECT -> point.x in room.x until room.x + room.width && point.y in room.y until room.y + room.height
        RoomShape.L_SHAPE -> {
            val splitX = room.x + max(2, room.width / 2)
            val inVerticalArm = point.x in room.x until splitX && point.y in room.y until room.y + room.height
            val inHorizontalArm = point.x in room.x until room.x + room.width && point.y in room.y + room.height / 2 until room.y + room.height
            inVerticalArm || inHorizontalArm
        }

        RoomShape.ROUND -> {
            val centerX = room.x + room.width / 2.0
            val centerY = room.y + room.height / 2.0
            val radiusX = max(1.5, room.width / 2.2)
            val radiusY = max(1.5, room.height / 2.2)
            val normalizedX = ((point.x + 0.5) - centerX) / radiusX
            val normalizedY = ((point.y + 0.5) - centerY) / radiusY
            (normalizedX * normalizedX) + (normalizedY * normalizedY) <= 1.0
        }

        RoomShape.IRREGULAR -> {
            point.x in room.x until room.x + room.width &&
                point.y in room.y until room.y + room.height &&
                point !in listOf(
                    Point(room.x + 1, room.y + 1),
                    Point(room.x + room.width - 2, room.y + 1),
                    Point(room.x + 1, room.y + room.height - 2),
                    Point(room.x + room.width - 2, room.y + room.height - 2),
                )
        }
    }

private fun templateGlyph(
    rows: List<String>,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
): Char {
    val rowIndex = scaledTemplateIndex(position = y, actualSize = height, templateSize = rows.size)
    val columnIndex = scaledTemplateIndex(position = x, actualSize = width, templateSize = rows.first().length)
    return rows[rowIndex][columnIndex]
}

private fun scaledTemplateIndex(
    position: Int,
    actualSize: Int,
    templateSize: Int,
): Int =
    if (actualSize <= 1 || templateSize <= 1) {
        0
    } else {
        (position * (templateSize - 1) / (actualSize - 1)).coerceIn(0, templateSize - 1)
    }

private fun selectBiomeFamilies(
    allowedBiomeFamilies: Set<String>,
    seed: Long,
): List<String> {
    val sorted = allowedBiomeFamilies.sorted()
    if (sorted.size <= 2) {
        return sorted
    }
    val random = stableRandom(seed, "biome-selection")
    val firstIndex = random.nextInt(sorted.size)
    val remaining = sorted.filterIndexed { index, _ -> index != firstIndex }
    val second = remaining[random.nextInt(remaining.size)]
    return listOf(sorted[firstIndex], second).sorted()
}

private fun sampleLoopCount(
    range: IntRange,
    seed: Long,
): Int {
    if (range.first == range.last) {
        return range.first
    }
    return stableRandom(seed, "loop-count").nextInt(range.first, range.last + 1)
}

private fun sampleDimension(
    range: IntRange,
    maxValue: Int,
    random: Random,
): Int {
    val boundedMax = min(range.last, maxValue)
    return if (boundedMax <= range.first) {
        boundedMax.coerceAtLeast(range.first)
    } else {
        random.nextInt(range.first, boundedMax + 1)
    }
}

private fun selectNodeBiomeFamily(
    selectedFamilies: List<String>,
    pathClass: PathClass,
    ordinal: Int,
): String {
    require(selectedFamilies.isNotEmpty()) { "Hybrid topology nodes require at least one biome family." }
    return when {
        selectedFamilies.size == 1 -> selectedFamilies.first()
        else -> selectedFamilies[ordinal % selectedFamilies.size]
    }
}

private fun adjustPlacement(
    existingRooms: List<RoomInstance>,
    candidate: RoomInstance,
    maxWidth: Int,
    maxHeight: Int,
): RoomInstance {
    val room = Room(candidate.x, candidate.y, candidate.width, candidate.height)
    if (existingRooms.none { existing -> room.intersects(Room(existing.x, existing.y, existing.width, existing.height)) }) {
        return candidate
    }
    val shift = candidate.height + 3
    val shiftedDown =
        candidate.copy(
            y = (candidate.y + shift).coerceIn(2, maxHeight - candidate.height - 2),
            x = candidate.x.coerceIn(2, maxWidth - candidate.width - 2),
        )
    val shiftedDownRoom = Room(shiftedDown.x, shiftedDown.y, shiftedDown.width, shiftedDown.height)
    if (existingRooms.none { existing -> shiftedDownRoom.intersects(Room(existing.x, existing.y, existing.width, existing.height)) }) {
        return shiftedDown
    }
    return candidate.copy(x = (candidate.x + candidate.width + 4).coerceIn(2, maxWidth - candidate.width - 2))
}

private fun terrainTagForGlyph(glyph: Char): TerrainTag? =
    when (glyph) {
        '~' -> TerrainTag.WATER
        'o', 'O' -> TerrainTag.OIL
        '*' -> TerrainTag.ICE
        else -> null
    }

private fun stableRandom(
    seed: Long,
    salt: String,
): Random {
    var mixed = seed
    salt.forEach { character ->
        mixed = (mixed * 6364136223846793005L) xor character.code.toLong()
    }
    return Random(mixed)
}

private fun <T> weightedPick(
    candidates: List<T>,
    weightOf: (T) -> Int,
    random: Random,
): T {
    require(candidates.isNotEmpty()) { "weightedPick requires at least one candidate." }
    val totalWeight = candidates.sumOf(weightOf)
    require(totalWeight > 0) { "weightedPick requires a positive total weight." }
    var remaining = random.nextInt(totalWeight)
    candidates.forEach { candidate ->
        remaining -= weightOf(candidate)
        if (remaining < 0) {
            return candidate
        }
    }
    return candidates.last()
}
