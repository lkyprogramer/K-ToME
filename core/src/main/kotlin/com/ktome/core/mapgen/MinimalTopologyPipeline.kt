package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.map.TileType

class MinimalTopologyPlanner : TopologyPlanner {
    override fun plan(
        profile: ZoneMapgenProfile,
        request: MapgenRequest,
    ): TopologyGraph {
        val hasOptionalLoop = profile.loopCountRange.last > 0
        val primaryNodes =
            listOf(
                TopologyNode(
                    id = NodeId("start"),
                    roomDefId = "planner.start",
                    pathClass = PathClass.CRITICAL_PATH,
                    tags = setOf("start") + profile.allowedBiomeFamilies,
                ),
                TopologyNode(
                    id = NodeId("mid"),
                    roomDefId = "planner.mid",
                    pathClass = PathClass.CRITICAL_PATH,
                    tags = setOf("hub") + profile.roomTagFilter.take(1),
                ),
                TopologyNode(
                    id = NodeId("goal"),
                    roomDefId = "planner.goal",
                    pathClass = PathClass.CRITICAL_PATH,
                    tags = setOf("goal"),
                ),
            )
        val optionalBranch =
            if (hasOptionalLoop) {
                listOf(
                    TopologyNode(
                        id = NodeId("optional"),
                        roomDefId = "planner.optional",
                        pathClass = PathClass.OPTIONAL,
                        tags = setOf("optional") + profile.roomTagFilter.take(2),
                    ),
                )
            } else {
                emptyList()
            }
        val nodes = primaryNodes + optionalBranch
        val edges =
            buildList {
                add(TopologyEdge(from = NodeId("start"), to = NodeId("mid")))
                add(TopologyEdge(from = NodeId("mid"), to = NodeId("goal")))
                if (optionalBranch.isNotEmpty()) {
                    add(TopologyEdge(from = NodeId("mid"), to = NodeId("optional")))
                    add(TopologyEdge(from = NodeId("optional"), to = NodeId("goal"), isLoop = true))
                }
            }
        return TopologyGraph(
            nodes = nodes,
            edges = edges,
            primaryPathNodeIds = primaryNodes.map(TopologyNode::id),
            optionalLoopCount = if (hasOptionalLoop) 1 else 0,
        )
    }
}

class MinimalTopologyMapgenPipeline(
    private val profileResolver: ZoneMapgenProfileResolver,
    private val planner: TopologyPlanner = MinimalTopologyPlanner(),
) : MapgenPipeline {
    override fun run(request: MapgenRequest): GeneratedFloor {
        val profile = profileResolver.resolve(request.zoneId)
        val topology = planner.plan(profile = profile, request = request)
        val map = materialize(topology = topology, request = request)
        return GeneratedFloor(
            zoneId = request.zoneId,
            floorIndex = request.floorIndex,
            seed = request.seed,
            topology = topology,
            terrainTags = TerrainTagPainter.paint(map = map, profile = profile, seed = request.seed),
            entrances = emptyList(),
            map = map,
        )
    }

    private fun materialize(
        topology: TopologyGraph,
        request: MapgenRequest,
    ): GameMap {
        val builder = GameMap.Builder(request.targetWidth, request.targetHeight)
        val criticalIds = topology.primaryPathNodeIds
        val roomWidth = (request.targetWidth / 8).coerceIn(6, 10)
        val roomHeight = (request.targetHeight / 5).coerceIn(6, 9)
        val horizontalGap = roomWidth + 4
        val startX = 2
        val baseY = (request.targetHeight / 2) - (roomHeight / 2)
        val roomsById = linkedMapOf<NodeId, Room>()

        criticalIds.forEachIndexed { index, nodeId ->
            roomsById[nodeId] = Room(startX + (index * horizontalGap), baseY, roomWidth, roomHeight)
        }
        topology.nodes
            .filter { node -> node.pathClass != PathClass.CRITICAL_PATH }
            .forEachIndexed { index, node ->
                val branchBase = roomsById.getValue(NodeId("mid"))
                roomsById[node.id] =
                    Room(
                        x = branchBase.x,
                        y = (branchBase.y + roomHeight + 4 + (index * (roomHeight + 2))).coerceAtMost(request.targetHeight - roomHeight - 2),
                        width = roomWidth,
                        height = roomHeight,
                    )
            }

        roomsById.values.forEach(builder::carveRoom)
        topology.edges.forEach { edge ->
            carveCorridor(builder = builder, from = roomsById.getValue(edge.from).center, to = roomsById.getValue(edge.to).center)
        }
        return builder.build(
            rooms = roomsById.values.toList(),
            playerStart = roomsById.getValue(criticalIds.first()).center,
        )
    }

    private fun carveCorridor(
        builder: GameMap.Builder,
        from: Point,
        to: Point,
    ) {
        val horizontalRange = if (from.x <= to.x) from.x..to.x else to.x..from.x
        horizontalRange.forEach { x -> builder.setTile(Point(x, from.y), TileType.FLOOR) }
        val verticalRange = if (from.y <= to.y) from.y..to.y else to.y..from.y
        verticalRange.forEach { y -> builder.setTile(Point(to.x, y), TileType.FLOOR) }
    }
}
