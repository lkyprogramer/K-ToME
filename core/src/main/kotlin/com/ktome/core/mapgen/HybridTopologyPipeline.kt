package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.map.TileType
import com.ktome.core.world.solvability.NodeAnchorId
import kotlin.math.max
import kotlin.math.min
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
        val hiddenSourceAnchorIds = profile.hiddenEntrancePlans.map(HiddenEntrancePlan::sourceAnchorId).distinct().sortedBy(NodeAnchorId::value)
        require(hiddenSourceAnchorIds.size == profile.hiddenEntrancePlans.size) {
            "Each zone mapgen profile must bind at most one hidden entrance plan to a formal hidden anchor family."
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
                    anchorId = primaryAnchorId(roleTag = roleTag, ordinal = index),
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
                    anchorId = optionalAnchorId(index),
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
        val hiddenSourceNodes =
            hiddenSourceAnchorIds.mapIndexed { index, anchorId ->
                val nodeBiomeFamilyId =
                    selectNodeBiomeFamily(
                        selectedFamilies = selectedFamilies,
                        pathClass = PathClass.OPTIONAL,
                        ordinal = primaryNodeCount + optionalNodeCount + index,
                    )
                val candidateTags = resolveCandidateRoomTags(profile = profile, biomeFamilyId = nodeBiomeFamilyId)
                TopologyNode(
                    id = NodeId("hidden-source-$index"),
                    anchorId = anchorId,
                    roomDefId =
                        chooseRoomDefId(
                            biomeFamilyId = nodeBiomeFamilyId,
                            pathClass = PathClass.OPTIONAL,
                            roleTag = "optional",
                            candidateTags = candidateTags,
                            seed = request.seed,
                            salt = "hidden-source-$index",
                        ),
                    pathClass = PathClass.OPTIONAL,
                    tags = buildSet {
                        add("optional")
                        add("hidden_anchor")
                        add(anchorId.value)
                        add("vault_candidate")
                        add("pattern_candidate")
                    },
                    biomeFamilyId = nodeBiomeFamilyId,
                )
            }

        val requirementRefsByAnchorId =
            profile.keyGatePlans
                .groupBy { plan -> plan.grantedByAnchorId }
                .mapValues { (_, plans) -> plans.map(KeyGatePlan::requirementRef).toSet() }

        val primaryNodesWithGrants =
            primaryNodes.map { node ->
                node.copy(grants = requirementRefsByAnchorId[node.anchorId].orEmpty())
            }
        val optionalNodesWithGrants =
            optionalNodes.map { node ->
                node.copy(grants = requirementRefsByAnchorId[node.anchorId].orEmpty())
            }
        val hiddenSourceNodesWithGrants =
            hiddenSourceNodes.map { node ->
                node.copy(grants = requirementRefsByAnchorId[node.anchorId].orEmpty())
            }
        val accessibleNodes = primaryNodesWithGrants + optionalNodesWithGrants + hiddenSourceNodesWithGrants
        val accessibleNodesByAnchorId = accessibleNodes.associateBy(TopologyNode::anchorId)
        profile.keyGatePlans.forEach { plan ->
            val grantNode = requireNotNull(accessibleNodesByAnchorId[plan.grantedByAnchorId]) {
                "KeyGatePlan '${plan.id}' references unknown grantedByAnchorId '${plan.grantedByAnchorId.value}'."
            }
            require(grantNode.pathClass != PathClass.SECRET) {
                "KeyGatePlan '${plan.id}' must not grant a requirement from SECRET anchor '${plan.grantedByAnchorId.value}'."
            }
        }
        val secretNodes =
            profile.hiddenEntrancePlans.mapIndexed { index, plan ->
                require(plan.sourceAnchorId in accessibleNodesByAnchorId) {
                    "HiddenEntrancePlan '${plan.bindingId.value}' references unknown source anchor '${plan.sourceAnchorId.value}'."
                }
                require(plan.targetAnchorId !in accessibleNodesByAnchorId) {
                    "HiddenEntrancePlan '${plan.bindingId.value}' target anchor '${plan.targetAnchorId.value}' must be unique."
                }
                val sourceNode = accessibleNodesByAnchorId.getValue(plan.sourceAnchorId)
                val nodeBiomeFamilyId = sourceNode.biomeFamilyId ?: selectedFamilies.first()
                val candidateTags = resolveCandidateRoomTags(profile = profile, biomeFamilyId = nodeBiomeFamilyId)
                TopologyNode(
                    id = NodeId("secret-$index"),
                    anchorId = plan.targetAnchorId,
                    roomDefId =
                        chooseRoomDefId(
                            biomeFamilyId = nodeBiomeFamilyId,
                            pathClass = PathClass.SECRET,
                            roleTag = "optional",
                            candidateTags = candidateTags,
                            seed = request.seed,
                            salt = "secret-$index",
                        ),
                    pathClass = PathClass.SECRET,
                    tags = buildSet {
                        add("secret")
                        add("hidden_cache")
                    },
                    biomeFamilyId = nodeBiomeFamilyId,
                )
            }
        val nodes = accessibleNodes + secretNodes
        val nodesByAnchorId = nodes.associateBy(TopologyNode::anchorId)

        val baseEdges =
            buildList {
                primaryNodesWithGrants.zipWithNext { left, right ->
                    add(TopologyEdge(from = left.id, to = right.id))
                }
                optionalNodesWithGrants.forEachIndexed { index, node ->
                    val attachIndex = (primaryNodesWithGrants.lastIndex - 1 - index).coerceAtLeast(1)
                    val attachNode = primaryNodesWithGrants[attachIndex]
                    val reconnectNode = primaryNodesWithGrants.last()
                    add(TopologyEdge(from = attachNode.id, to = node.id))
                    add(TopologyEdge(from = node.id, to = reconnectNode.id, isLoop = true))
                }
                hiddenSourceNodesWithGrants.forEach { node ->
                    val placement = hiddenAnchorPlacement(anchorId = node.anchorId, primaryNodes = primaryNodesWithGrants)
                    add(TopologyEdge(from = placement.attachNode.id, to = node.id))
                    add(TopologyEdge(from = node.id, to = placement.reconnectNode.id, isLoop = true))
                }
            }
        val edges =
            applyKeyGatePlans(
                edges = baseEdges,
                primaryPathNodeIds = primaryNodesWithGrants.map(TopologyNode::id),
                nodesByAnchorId = nodesByAnchorId,
                keyGatePlans = profile.keyGatePlans,
            )

        return TopologyGraph(
            nodes = nodes,
            edges = edges,
            primaryPathNodeIds = primaryNodesWithGrants.map(TopologyNode::id),
            optionalLoopCount = edges.count(TopologyEdge::isLoop),
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
                    roleTag in roomDef.tags || "general" in roomDef.tags || (pathClass != PathClass.CRITICAL_PATH && "optional" in roomDef.tags)
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

    private fun applyKeyGatePlans(
        edges: List<TopologyEdge>,
        primaryPathNodeIds: List<NodeId>,
        nodesByAnchorId: Map<NodeAnchorId, TopologyNode>,
        keyGatePlans: List<KeyGatePlan>,
    ): List<TopologyEdge> {
        val plansByEndpoints =
            keyGatePlans.groupBy { plan ->
                val from = requireNotNull(nodesByAnchorId[plan.fromAnchorId]) {
                    "KeyGatePlan '${plan.id}' references unknown fromAnchorId '${plan.fromAnchorId.value}'."
                }
                val to = requireNotNull(nodesByAnchorId[plan.toAnchorId]) {
                    "KeyGatePlan '${plan.id}' references unknown toAnchorId '${plan.toAnchorId.value}'."
                }
                setOf(from.id, to.id)
            }
        val gatedEdges =
            edges.map { edge ->
                val plans = plansByEndpoints[setOf(edge.from, edge.to)].orEmpty()
                if (plans.isEmpty()) {
                    return@map edge
                }
                edge.copy(requiredKeys = edge.requiredKeys + plans.map(KeyGatePlan::requirementRef))
            }
        val primaryPathIndexByNodeId = primaryPathNodeIds.withIndex().associate { (index, nodeId) -> nodeId to index }
        val primaryPathRequirementsByEndpoints =
            gatedEdges
                .filter { edge ->
                    val fromIndex = primaryPathIndexByNodeId[edge.from]
                    val toIndex = primaryPathIndexByNodeId[edge.to]
                    fromIndex != null && toIndex != null && kotlin.math.abs(fromIndex - toIndex) == 1
                }.associate { edge -> setOf(edge.from, edge.to) to edge.requiredKeys }
        val attachPrimaryNodeIdByOptionalNodeId =
            gatedEdges
                .asSequence()
                .filterNot(TopologyEdge::isLoop)
                .mapNotNull { edge ->
                    val fromIndex = primaryPathIndexByNodeId[edge.from]
                    val toIndex = primaryPathIndexByNodeId[edge.to]
                    when {
                        fromIndex != null && toIndex == null -> edge.to to edge.from
                        fromIndex == null && toIndex != null -> edge.from to edge.to
                        else -> null
                    }
                }.toMap()
        return gatedEdges.map { edge ->
            if (!edge.isLoop) {
                return@map edge
            }
            val fromPrimaryIndex = primaryPathIndexByNodeId[edge.from]
            val toPrimaryIndex = primaryPathIndexByNodeId[edge.to]
            val optionalNodeId =
                when {
                    fromPrimaryIndex == null && toPrimaryIndex != null -> edge.from
                    fromPrimaryIndex != null && toPrimaryIndex == null -> edge.to
                    else -> return@map edge
                }
            val reconnectPrimaryNodeId =
                when {
                    fromPrimaryIndex != null -> edge.from
                    toPrimaryIndex != null -> edge.to
                    else -> return@map edge
                }
            val attachPrimaryNodeId = attachPrimaryNodeIdByOptionalNodeId[optionalNodeId] ?: return@map edge
            val attachIndex = primaryPathIndexByNodeId.getValue(attachPrimaryNodeId)
            val reconnectIndex = primaryPathIndexByNodeId.getValue(reconnectPrimaryNodeId)
            val crossedRequirements =
                requirementsAlongPrimarySegment(
                    primaryPathNodeIds = primaryPathNodeIds,
                    startIndex = attachIndex,
                    endIndex = reconnectIndex,
                    primaryPathRequirementsByEndpoints = primaryPathRequirementsByEndpoints,
                )
            if (crossedRequirements.isEmpty()) {
                return@map edge
            }
            edge.copy(requiredKeys = edge.requiredKeys + crossedRequirements)
        }.also { updatedEdges ->
            keyGatePlans.forEach { plan ->
                val from = nodesByAnchorId.getValue(plan.fromAnchorId)
                val to = nodesByAnchorId.getValue(plan.toAnchorId)
                require(updatedEdges.any { edge -> setOf(edge.from, edge.to) == setOf(from.id, to.id) }) {
                    "KeyGatePlan '${plan.id}' could not resolve an edge between '${plan.fromAnchorId.value}' and '${plan.toAnchorId.value}'."
                }
            }
        }
    }

    private fun requirementsAlongPrimarySegment(
        primaryPathNodeIds: List<NodeId>,
        startIndex: Int,
        endIndex: Int,
        primaryPathRequirementsByEndpoints: Map<Set<NodeId>, Set<RequirementRef>>,
    ): Set<RequirementRef> {
        if (startIndex == endIndex) {
            return emptySet()
        }
        val segmentStart = min(startIndex, endIndex)
        val segmentEndExclusive = max(startIndex, endIndex)
        return (segmentStart until segmentEndExclusive)
            .asSequence()
            .flatMap { index ->
                primaryPathRequirementsByEndpoints[setOf(primaryPathNodeIds[index], primaryPathNodeIds[index + 1])]
                    .orEmpty()
                    .asSequence()
            }.toSet()
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
        val roomInstances =
            instantiateRooms(
                topology = topology,
                hiddenEntrancePlans = profile.hiddenEntrancePlans,
                request = request,
            )
        val nodesByAnchorId = topology.nodes.associateBy(TopologyNode::anchorId)
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
        val roomsWithVaultTags = tagVaultRooms(rooms = patternedRooms, vaultPlacements = vaultPlacements)
        topology.edges.forEach { edge ->
            carveCorridor(
                builder = builder,
                from = roomBoundsByNodeId.getValue(edge.from).center,
                to = roomBoundsByNodeId.getValue(edge.to).center,
                random = stableRandom(request.seed, "corridor:${edge.from.value}:${edge.to.value}:${edge.isLoop}"),
            )
        }
        ensureAnchorsWalkable(builder = builder, topology = topology, rooms = roomsWithVaultTags)

        val map =
            builder.build(
                rooms = roomsWithVaultTags.map { room -> Room(room.x, room.y, room.width, room.height) },
                playerStart = Room(roomsWithVaultTags.first().x, roomsWithVaultTags.first().y, roomsWithVaultTags.first().width, roomsWithVaultTags.first().height).center,
            )
        val terrainTags =
            paintTerrainTags(
                map = map,
                profile = profile,
                rooms = roomsWithVaultTags,
                vaultPlacements = vaultPlacements,
                request = request,
            )
        val entrances =
            buildGeneratedEntrances(
                hiddenEntrancePlans = profile.hiddenEntrancePlans,
                nodesByAnchorId = nodesByAnchorId,
                roomsByAnchorId = roomsWithVaultTags.associateBy(RoomInstance::anchorId),
            )
        return GeneratedFloor(
            zoneId = request.zoneId,
            floorIndex = request.floorIndex,
            seed = request.seed,
            topology = topology,
            rooms = roomsWithVaultTags,
            terrainTags = terrainTags,
            biomeFamilyIds = roomsWithVaultTags.mapNotNull(RoomInstance::biomeFamilyId).distinct(),
            vaultPlacements = vaultPlacements,
            entrances = entrances,
            map = map,
        )
    }

    private fun instantiateRooms(
        topology: TopologyGraph,
        hiddenEntrancePlans: List<HiddenEntrancePlan>,
        request: MapgenRequest,
    ): List<RoomInstance> {
        val nodesById = topology.nodes.associateBy(TopologyNode::id)
        val nodesByAnchorId = topology.nodes.associateBy(TopologyNode::anchorId)
        val primaryNodes = topology.primaryPathNodeIds.map(nodesById::getValue)
        val hiddenSourceAnchorIds = hiddenEntrancePlans.map(HiddenEntrancePlan::sourceAnchorId).toSet()
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
                    anchorId = node.anchorId,
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
            .filter { node ->
                node.pathClass != PathClass.SECRET &&
                    node.id !in topology.primaryPathNodeIds &&
                    node.anchorId !in hiddenSourceAnchorIds
            }
            .sortedBy { node -> node.id.value }
            .forEachIndexed { index, node ->
                val anchorRoom = rooms[(rooms.lastIndex - 1 - index).coerceAtLeast(1)]
                rooms +=
                    instantiateOptionalRoom(
                        room = node,
                        anchorRoom = anchorRoom,
                        existingRooms = rooms,
                        request = request,
                        branchSalt = "branch:${node.id.value}",
                        horizontalSalt = null,
                        verticalSpacing = 4,
                    )
            }

        hiddenSourceAnchorIds
            .sortedBy(NodeAnchorId::value)
            .map(nodesByAnchorId::getValue)
            .forEach { node ->
                val placement = hiddenAnchorPlacement(anchorId = node.anchorId, primaryNodes = primaryNodes)
                val anchorRoom = requireNotNull(rooms.firstOrNull { room -> room.nodeId == placement.attachNode.id }) {
                    "Hidden entrance anchor '${node.anchorId.value}' could not resolve its anchor room."
                }
                val verticalSpacing =
                    when (node.anchorId.value) {
                        "hidden.critical.adjacent" -> 5
                        "hidden.goal.adjacent" -> 6
                        else -> 4
                    }
                rooms +=
                    instantiateOptionalRoom(
                        room = node,
                        anchorRoom = anchorRoom,
                        existingRooms = rooms,
                        request = request,
                        branchSalt = "hidden-branch:${node.id.value}",
                        horizontalSalt = "hidden-x:${node.id.value}",
                        verticalSpacing = verticalSpacing,
                    )
            }

        val roomsByAnchorId = rooms.associateBy(RoomInstance::anchorId)
        hiddenEntrancePlans.forEach { plan ->
            val node = requireNotNull(nodesById[topology.nodes.first { candidate -> candidate.anchorId == plan.targetAnchorId }.id]) {
                "HiddenEntrancePlan '${plan.bindingId.value}' target anchor '${plan.targetAnchorId.value}' must resolve to a secret node."
            }
            val sourceRoom = requireNotNull(roomsByAnchorId[plan.entranceAnchorId]) {
                "HiddenEntrancePlan '${plan.bindingId.value}' entrance anchor '${plan.entranceAnchorId.value}' must resolve to an instantiated room."
            }
            val roomDef = roomDefsById.getValue(node.roomDefId)
            val width =
                sampleDimension(
                    roomDef.widthRange,
                    maxValue = (request.targetWidth / 5).coerceAtLeast(roomDef.widthRange.first),
                    random = stableRandom(request.seed, "width:${node.id.value}"),
                )
            val height =
                sampleDimension(
                    roomDef.heightRange,
                    maxValue = (request.targetHeight / 4).coerceAtLeast(roomDef.heightRange.first),
                    random = stableRandom(request.seed, "height:${node.id.value}"),
                )
            val branchUp = stableRandom(request.seed, "secret-branch:${node.id.value}").nextBoolean()
            val horizontalOffset =
                stableRandom(request.seed, "secret-x:${node.id.value}")
                    .nextInt(-max(2, width / 3), max(3, width / 3) + 1)
            val candidateX =
                (sourceRoom.x + (sourceRoom.width / 2) - (width / 2) + horizontalOffset)
                    .coerceIn(2, request.targetWidth - width - 2)
            val candidateY =
                if (branchUp) {
                    sourceRoom.y - height - 8
                } else {
                    sourceRoom.y + sourceRoom.height + 8
                }
            val boundedY = candidateY.coerceIn(2, request.targetHeight - height - 2)
            rooms +=
                adjustPlacement(
                    existingRooms = rooms,
                    candidate =
                        RoomInstance(
                            nodeId = node.id,
                            anchorId = node.anchorId,
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

    private fun buildGeneratedEntrances(
        hiddenEntrancePlans: List<HiddenEntrancePlan>,
        nodesByAnchorId: Map<NodeAnchorId, TopologyNode>,
        roomsByAnchorId: Map<NodeAnchorId, RoomInstance>,
    ): List<GeneratedEntrance> =
        hiddenEntrancePlans.map { plan ->
            val sourceNode = requireNotNull(nodesByAnchorId[plan.sourceAnchorId]) {
                "HiddenEntrancePlan '${plan.bindingId.value}' references unknown source anchor '${plan.sourceAnchorId.value}'."
            }
            val entranceRoom = requireNotNull(roomsByAnchorId[plan.entranceAnchorId]) {
                "HiddenEntrancePlan '${plan.bindingId.value}' entrance anchor '${plan.entranceAnchorId.value}' must resolve to an instantiated room."
            }
            val targetNode = requireNotNull(nodesByAnchorId[plan.targetAnchorId]) {
                "HiddenEntrancePlan '${plan.bindingId.value}' references unknown target anchor '${plan.targetAnchorId.value}'."
            }
            require(entranceRoom.nodeId == sourceNode.id) {
                "HiddenEntrancePlan '${plan.bindingId.value}' entrance anchor '${plan.entranceAnchorId.value}' must resolve to source node '${sourceNode.id.value}'."
            }
            GeneratedEntrance(
                bindingId = plan.bindingId,
                fromNodeId = entranceRoom.nodeId,
                targetNodeId = targetNode.id,
                entranceAnchorId = plan.entranceAnchorId,
                targetAnchorId = plan.targetAnchorId,
                pathClass = plan.pathClass,
                discoveryRule = plan.discoveryRule,
                targetSecretZoneId = plan.targetSecretZoneId,
                resolvedReturnBridgeNodeId = entranceRoom.nodeId,
            )
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

    private fun tagVaultRooms(
        rooms: List<RoomInstance>,
        vaultPlacements: List<VaultPlacement>,
    ): List<RoomInstance> {
        if (vaultPlacements.isEmpty()) {
            return rooms
        }
        val vaultNodeIds = vaultPlacements.map(VaultPlacement::nodeId).toSet()
        return rooms.map { room ->
            if (room.nodeId in vaultNodeIds) {
                room.copy(tags = room.tags + "vault")
            } else {
                room
            }
        }
    }

    private fun instantiateOptionalRoom(
        room: TopologyNode,
        anchorRoom: RoomInstance,
        existingRooms: List<RoomInstance>,
        request: MapgenRequest,
        branchSalt: String,
        horizontalSalt: String?,
        verticalSpacing: Int,
    ): RoomInstance {
        val roomDef = roomDefsById.getValue(room.roomDefId)
        val width =
            sampleDimension(
                roomDef.widthRange,
                maxValue = (request.targetWidth / 5).coerceAtLeast(roomDef.widthRange.first),
                random = stableRandom(request.seed, "width:${room.id.value}"),
            )
        val height =
            sampleDimension(
                roomDef.heightRange,
                maxValue = (request.targetHeight / 4).coerceAtLeast(roomDef.heightRange.first),
                random = stableRandom(request.seed, "height:${room.id.value}"),
            )
        val branchUp = stableRandom(request.seed, branchSalt).nextBoolean()
        val horizontalOffset =
            horizontalSalt
                ?.let { salt ->
                    stableRandom(request.seed, salt).nextInt(-max(2, width / 3), max(3, width / 3) + 1)
                } ?: 0
        val candidateX =
            (anchorRoom.x + (anchorRoom.width / 2) - (width / 2) + horizontalOffset)
                .coerceIn(2, request.targetWidth - width - 2)
        val candidateY =
            if (branchUp) {
                anchorRoom.y - height - verticalSpacing
            } else {
                anchorRoom.y + anchorRoom.height + verticalSpacing
            }
        val boundedY = candidateY.coerceIn(2, request.targetHeight - height - 2)
        return adjustPlacement(
            existingRooms = existingRooms,
            candidate =
                RoomInstance(
                    nodeId = room.id,
                    anchorId = room.anchorId,
                    roomDefId = room.roomDefId,
                    x = candidateX,
                    y = boundedY,
                    width = width,
                    height = height,
                    shape = roomDef.shape,
                    pathClass = room.pathClass,
                    tags = room.tags,
                    biomeFamilyId = room.biomeFamilyId,
                ),
            maxWidth = request.targetWidth,
            maxHeight = request.targetHeight,
        )
    }

    private fun ensureAnchorsWalkable(
        builder: GameMap.Builder,
        topology: TopologyGraph,
        rooms: List<RoomInstance>,
    ) {
        val roomsByNodeId = rooms.associate { room -> room.nodeId to Room(room.x, room.y, room.width, room.height) }
        topology.nodes.forEach { node ->
            val room = roomsByNodeId[node.id] ?: return@forEach
            val center = room.center
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
                TerrainTagPainter.resolveEffectiveWeights(
                    profileWeights = profile.terrainTagWeights,
                    familyWeights = room.biomeFamilyId?.let(biomeFamiliesById::get)?.terrainTagWeights.orEmpty(),
                )
            if (weights.isEmpty()) {
                return@forEachIndexed
            }
            val candidatePoints =
                TerrainTagPainter.paintableRoomPoints(
                    room = room,
                    map = map,
                    excludedPoint = map.playerStart,
                )
            if (candidatePoints.isEmpty()) {
                return@forEachIndexed
            }
            TerrainTagPainter.paintWeightedTags(
                painted = painted,
                candidatePoints = candidatePoints,
                weights = weights,
            ) { tag ->
                stableRandom(request.seed, "terrain:${room.nodeId.value}:${ordinal}:${tag.name}")
            }
            val terrainHints = terrainHintsForPattern(room.patternId)
            terrainHints.forEachIndexed { index, tag ->
                if (candidatePoints.isNotEmpty()) {
                    val point =
                        candidatePoints[
                            (index * max(1, candidatePoints.size / max(1, terrainHints.size))) % candidatePoints.size
                        ]
                    painted.getOrPut(point) { linkedSetOf() }.add(tag)
                }
            }
        }
        vaultPlacements.forEach { vault ->
            val room = roomsByNodeId[vault.nodeId] ?: return@forEach
            val candidatePoints = TerrainTagPainter.paintableRoomPoints(room = room, map = map)
            if (candidatePoints.isEmpty()) {
                return@forEach
            }
            vault.requiredTerrainTags.sortedBy(TerrainTag::ordinal).forEachIndexed { index, tag ->
                val point = candidatePoints[(index * max(1, candidatePoints.size / max(1, vault.requiredTerrainTags.size))) % candidatePoints.size]
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
                if (!room.contains(point)) {
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

    private fun terrainHintsForPattern(patternId: String?): Set<TerrainTag> {
        patternId ?: return emptySet()
        return patternTemplatesById[patternId]
            ?.rows
            ?.flatMap { row -> row.mapNotNull(::terrainTagForGlyph) }
            ?.toSet()
            .orEmpty()
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

private data class HiddenAnchorPlacement(
    val attachNode: TopologyNode,
    val reconnectNode: TopologyNode,
)

private fun hiddenAnchorPlacement(
    anchorId: NodeAnchorId,
    primaryNodes: List<TopologyNode>,
): HiddenAnchorPlacement {
    require(primaryNodes.size >= 3) { "Hybrid topology requires at least three primary nodes for hidden entrance placement." }
    val attachIndex =
        when (anchorId.value) {
            "hidden.branch" -> 1
            "hidden.critical.adjacent" -> primaryNodes.lastIndex / 2
            "hidden.goal.adjacent" -> (primaryNodes.lastIndex - 1).coerceAtLeast(1)
            else -> error("Unsupported hidden entrance anchor '${anchorId.value}'.")
        }
    val reconnectIndex =
        when (anchorId.value) {
            "hidden.critical.adjacent" -> (attachIndex + 1).coerceAtMost(primaryNodes.lastIndex)
            else -> primaryNodes.lastIndex
        }
    return HiddenAnchorPlacement(
        attachNode = primaryNodes[attachIndex],
        reconnectNode = primaryNodes[reconnectIndex],
    )
}

private fun primaryAnchorId(
    roleTag: String,
    ordinal: Int,
): NodeAnchorId =
    when (roleTag) {
        "start" -> NodeAnchorId("critical.start")
        "goal" -> NodeAnchorId("critical.goal")
        "hub" -> NodeAnchorId("critical.hub")
        else -> NodeAnchorId("critical.route.$ordinal")
    }

private fun optionalAnchorId(index: Int): NodeAnchorId = NodeAnchorId("optional.branch.${index + 1}")

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
