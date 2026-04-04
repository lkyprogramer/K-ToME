package com.ktome.core.world.solvability

import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.RequirementRef
import kotlin.math.roundToInt

object SolvabilityProver {
    fun prove(
        graph: SolvabilityGraph,
        perceptionScore: PerceptionScore,
        providedTags: Set<String> = emptySet(),
    ): SolvabilityProof {
        val nodeById = graph.nodes.associateBy(SolvabilityNode::id)
        val adjacency =
            buildMap {
                graph.nodes.forEach { node -> put(node.id, mutableListOf<SolvabilityEdge>()) }
                graph.edges.forEach { edge ->
                    getValue(edge.from).add(edge)
                    getValue(edge.to).add(edge)
                }
            }
        val visited = linkedSetOf(graph.entryNodeId)
        val acquiredKeys = linkedSetOf<RequirementRef>()
        val searchStates = linkedMapOf<SearchBindingId, SearchStateEntry>()
        var searchActionCount = 0
        var searchRevealCount = 0
        var searchFailCount = 0

        var progress = true
        while (progress) {
            progress = false

            visited.toList().forEach { nodeId ->
                nodeById.getValue(nodeId).grants.forEach { requirement ->
                    if (acquiredKeys.add(requirement)) {
                        progress = true
                    }
                }
            }

            visited.toList().forEach { nodeId ->
                adjacency.getValue(nodeId).forEach { edge ->
                    val nextNodeId = if (edge.from == nodeId) edge.to else edge.from
                    if (nextNodeId in visited) {
                        return@forEach
                    }
                    if (!acquiredKeys.containsAll(edge.requiredKeys)) {
                        return@forEach
                    }
                    val canTraverse =
                        if (edge.discoveryRule == null) {
                            true
                        } else {
                            val bindingId = requireNotNull(edge.searchBindingId)
                            val cached = searchStates[bindingId]
                            val resolved =
                                if (cached != null) {
                                    cached
                                } else {
                                    searchActionCount += 1
                                    val result =
                                        if (
                                            edge.discoveryRule.evaluate(
                                                perceptionScore = perceptionScore,
                                                providedTags = edge.discoveryContextTags + providedTags,
                                            )
                                        ) {
                                            searchRevealCount += 1
                                            SearchActionResult.REVEALED
                                        } else {
                                            searchFailCount += 1
                                            SearchActionResult.FAILED_CHECK
                                        }
                                    SearchStateEntry(bindingId = bindingId, result = result).also { entry ->
                                        searchStates[bindingId] = entry
                                    }
                                }
                            resolved.result == SearchActionResult.REVEALED
                        }
                    if (!canTraverse) {
                        return@forEach
                    }
                    if (visited.add(nextNodeId)) {
                        progress = true
                    }
                }
            }
        }

        val unresolvedRequirements =
            graph.edges
                .asSequence()
                .filter { edge ->
                    val fromCritical = nodeById.getValue(edge.from).pathClass == PathClass.CRITICAL_PATH
                    val toCritical = nodeById.getValue(edge.to).pathClass == PathClass.CRITICAL_PATH
                    fromCritical || toCritical
                }.flatMap { edge ->
                    edge.requiredKeys.asSequence().filterNot(acquiredKeys::contains)
                }.distinct()
                .sortedBy(RequirementRef::value)
                .toList()
        val optionalPathCount = visited.count { nodeId -> nodeById.getValue(nodeId).pathClass == PathClass.OPTIONAL }
        val secretPathCount = visited.count { nodeId -> nodeById.getValue(nodeId).pathClass == PathClass.SECRET }
        val totalReachableNodes = visited.size
        val reachabilityRatio =
            if (graph.nodes.isEmpty()) {
                0f
            } else {
                ((visited.size.toDouble() / graph.nodes.size.toDouble()) * 1000.0).roundToInt() / 1000f
            }
        val criticalPathReachable =
            graph.nodes
                .filter { node -> node.pathClass == PathClass.CRITICAL_PATH }
                .all { node -> node.id in visited }

        return SolvabilityProof(
            criticalPathReachable = criticalPathReachable,
            acquiredKeys = acquiredKeys.toList(),
            unresolvedRequirements = unresolvedRequirements,
            visitedNodes = visited.toList(),
            optionalPathCount = optionalPathCount,
            secretPathCount = secretPathCount,
            totalReachableNodes = totalReachableNodes,
            reachabilityRatio = reachabilityRatio,
            searchActionCount = searchActionCount,
            searchRevealCount = searchRevealCount,
            searchFailCount = searchFailCount,
            searchStates = searchStates.values.toList(),
        )
    }
}
