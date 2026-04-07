package com.ktome.game.hidden

import com.ktome.core.mapgen.GeneratedEntrance
import com.ktome.core.mapgen.GeneratedFloor
import com.ktome.core.mapgen.MapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.mapgen.NodeId
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.TopologyEdge
import com.ktome.core.mapgen.TopologyGraph
import com.ktome.core.mapgen.TopologyNode

class HiddenContentMapgenPipeline(
    private val delegate: MapgenPipeline,
    private val secretZoneRegistry: SecretZoneRegistry,
) : MapgenPipeline {
    override fun run(request: MapgenRequest): GeneratedFloor {
        val generatedFloor = delegate.run(request)
        val resolvedEntrances =
            generatedFloor.entrances.map { entrance ->
                val secretZone = requireNotNull(secretZoneRegistry.resolveForEntrance(entrance)) {
                    "GeneratedEntrance '${entrance.bindingId.value}' references unknown secret zone '${entrance.targetSecretZoneId.id}'."
                }
                require(secretZone.entranceBindingId == entrance.entranceAnchorId) {
                    "Secret zone '${secretZone.id.id}' must bind to entrance anchor '${entrance.entranceAnchorId.value}', got '${secretZone.entranceBindingId.value}'."
                }
                require(secretZone.entryRule == entrance.discoveryRule) {
                    "Secret zone '${secretZone.id.id}' entryRule must match hidden entrance discoveryRule '${entrance.bindingId.value}'."
                }
                entrance.copy(
                    resolvedReturnBridgeNodeId =
                        resolveReturnBridgeNodeId(
                            generatedFloor = generatedFloor,
                            entrance = entrance,
                            secretZone = secretZone,
                        ),
                )
            }
        return generatedFloor.copy(entrances = resolvedEntrances)
    }

    private fun resolveReturnBridgeNodeId(
        generatedFloor: GeneratedFloor,
        entrance: GeneratedEntrance,
        secretZone: SecretZoneDef,
    ): NodeId {
        val topology = generatedFloor.topology
        val resolved =
            when (secretZone.returnBridgePolicy) {
                ReturnBridgePolicy.NEAREST_OPTIONAL_ANCHOR ->
                    nearestNode(
                        topology = topology,
                        startNodeId = entrance.targetNodeId,
                        predicate = { node -> node.pathClass == PathClass.OPTIONAL },
                    )

                ReturnBridgePolicy.LAST_MAINLINE_BRANCH ->
                    nearestNode(
                        topology = topology,
                        startNodeId = entrance.fromNodeId,
                        predicate = { node -> node.id in topology.primaryPathNodeIds },
                    )

                ReturnBridgePolicy.EXPLICIT_ANCHOR ->
                    topology.nodes.firstOrNull { node -> secretZone.returnBridgeAnchorTag in node.tags }?.id
            }
        val nodeId = requireNotNull(resolved) {
            "Secret zone '${secretZone.id.id}' could not resolve return bridge node for policy '${secretZone.returnBridgePolicy}'."
        }
        val node = requireNotNull(topology.nodes.firstOrNull { candidate -> candidate.id == nodeId }) {
            "Resolved return bridge node '$nodeId' for secret zone '${secretZone.id.id}' is missing from topology."
        }
        require(node.pathClass != PathClass.SECRET) {
            "Secret zone '${secretZone.id.id}' resolved return bridge '$nodeId' into SECRET path class."
        }
        require(canReach(topology = topology, start = nodeId, target = topology.primaryPathNodeIds.last())) {
            "Secret zone '${secretZone.id.id}' return bridge '$nodeId' cannot reach the current floor exit path."
        }
        return nodeId
    }

    private fun nearestNode(
        topology: TopologyGraph,
        startNodeId: NodeId,
        predicate: (TopologyNode) -> Boolean,
    ): NodeId? {
        val adjacency = topology.adjacency()
        val nodesById = topology.nodes.associateBy { node -> node.id }
        val queue = ArrayDeque<NodeId>()
        val visited = linkedSetOf<NodeId>()
        queue += startNodeId
        visited += startNodeId
        while (queue.isNotEmpty()) {
            val nodeId = queue.removeFirst()
            val node = nodesById.getValue(nodeId)
            if (predicate(node)) {
                return node.id
            }
            adjacency.getValue(nodeId)
                .asSequence()
                .filterNot(visited::contains)
                .sortedBy(NodeId::value)
                .forEach { next ->
                    visited += next
                    queue += next
                }
        }
        return null
    }

    private fun canReach(
        topology: TopologyGraph,
        start: NodeId,
        target: NodeId,
    ): Boolean {
        if (start == target) {
            return true
        }
        val adjacency = topology.adjacency()
        val queue = ArrayDeque<NodeId>()
        val visited = linkedSetOf<NodeId>()
        queue += start
        visited += start
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            adjacency.getValue(current)
                .asSequence()
                .filterNot(visited::contains)
                .sortedBy(NodeId::value)
                .forEach { next ->
                    if (next == target) {
                        return true
                    }
                    visited += next
                    queue += next
                }
        }
        return false
    }

    private fun TopologyGraph.adjacency(): Map<NodeId, Set<NodeId>> =
        buildMap {
            nodes.forEach { node -> put(node.id, linkedSetOf()) }
            edges.forEach { edge: TopologyEdge ->
                (getValue(edge.from) as MutableSet<NodeId>).add(edge.to)
                (getValue(edge.to) as MutableSet<NodeId>).add(edge.from)
            }
        }
}
