package com.ktome.core.world.solvability

import com.ktome.core.mapgen.GeneratedFloor
import com.ktome.core.mapgen.TopologyNode

object SolvabilityGraphBuilder {
    fun build(generatedFloor: GeneratedFloor): SolvabilityGraph {
        val nodes =
            generatedFloor.topology.nodes.map { node ->
                node.toSolvabilityNode()
            }
        val topologyEdges =
            generatedFloor.topology.edges.map { edge ->
                SolvabilityEdge(
                    from = edge.from,
                    to = edge.to,
                    requiredKeys = edge.requiredKeys,
                )
            }
        val entranceEdges =
            generatedFloor.entrances.map { entrance ->
                SolvabilityEdge(
                    from = entrance.fromNodeId,
                    to = entrance.targetNodeId,
                    requiredKeys = emptySet(),
                    discoveryRule = entrance.discoveryRule,
                    searchBindingId = entrance.bindingId,
                    discoveryContextTags = discoveryContextTagsFor(generatedFloor = generatedFloor, entrance = entrance),
                )
            }
        return SolvabilityGraph(
            entryNodeId = generatedFloor.topology.primaryPathNodeIds.first(),
            nodes = nodes,
            edges = topologyEdges + entranceEdges,
        )
    }

    private fun TopologyNode.toSolvabilityNode(): SolvabilityNode =
        SolvabilityNode(
            id = id,
            anchorId = anchorId,
            pathClass = pathClass,
            roomId = roomDefId,
            grants = grants,
        )

    private fun discoveryContextTagsFor(
        generatedFloor: GeneratedFloor,
        entrance: com.ktome.core.mapgen.GeneratedEntrance,
    ): Set<String> =
        requireNotNull(
            generatedFloor.roomForEntrance(entrance),
        ) {
            "GeneratedEntrance '${entrance.bindingId.value}' must resolve to an instantiated entrance room."
        }.tags
}
