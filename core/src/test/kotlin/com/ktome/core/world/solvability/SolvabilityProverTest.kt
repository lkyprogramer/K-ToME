package com.ktome.core.world.solvability

import com.ktome.core.mapgen.NodeId
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.RequirementRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SolvabilityProverTest {
    @Test
    fun `proof supports optional backtracking to unlock a critical gate`() {
        val optionalKey = RequirementRef("KEY_ITEM:greenwood_final_path_key")
        val graph =
            SolvabilityGraph(
                entryNodeId = NodeId("start"),
                nodes =
                    listOf(
                        SolvabilityNode(NodeId("start"), NodeAnchorId("critical.start"), PathClass.CRITICAL_PATH, "room.start", emptySet()),
                        SolvabilityNode(NodeId("hub"), NodeAnchorId("critical.hub"), PathClass.CRITICAL_PATH, "room.hub", emptySet()),
                        SolvabilityNode(NodeId("optional"), NodeAnchorId("optional.branch.1"), PathClass.OPTIONAL, "room.optional", setOf(optionalKey)),
                        SolvabilityNode(NodeId("goal"), NodeAnchorId("critical.goal"), PathClass.CRITICAL_PATH, "room.goal", emptySet()),
                    ),
                edges =
                    listOf(
                        SolvabilityEdge(NodeId("start"), NodeId("hub"), emptySet()),
                        SolvabilityEdge(NodeId("hub"), NodeId("optional"), emptySet()),
                        SolvabilityEdge(NodeId("hub"), NodeId("goal"), setOf(optionalKey)),
                    ),
            )

        val proof = SolvabilityProver.prove(graph = graph, perceptionScore = PerceptionScore(baseMentalPower = 12))

        assertTrue(proof.criticalPathReachable)
        assertEquals(listOf("start", "hub", "optional", "goal"), proof.visitedNodes.map { nodeId -> nodeId.value })
        assertEquals(listOf(optionalKey), proof.acquiredKeys)
        assertEquals(1, proof.optionalPathCount)
        assertEquals(0, proof.secretPathCount)
    }

    @Test
    fun `failed hidden entrance reveal does not block the critical path`() {
        val bindingId = SearchBindingId("search.underground_river.crystal_rift")
        val graph =
            SolvabilityGraph(
                entryNodeId = NodeId("start"),
                nodes =
                    listOf(
                        SolvabilityNode(NodeId("start"), NodeAnchorId("critical.start"), PathClass.CRITICAL_PATH, "room.start", emptySet()),
                        SolvabilityNode(NodeId("goal"), NodeAnchorId("critical.goal"), PathClass.CRITICAL_PATH, "room.goal", emptySet()),
                        SolvabilityNode(NodeId("secret"), NodeAnchorId("secret.crystal_rift"), PathClass.SECRET, "room.secret", emptySet()),
                    ),
                edges =
                    listOf(
                        SolvabilityEdge(NodeId("start"), NodeId("goal"), emptySet()),
                        SolvabilityEdge(
                            from = NodeId("start"),
                            to = NodeId("secret"),
                            requiredKeys = emptySet(),
                            discoveryRule =
                                DiscoveryRule(
                                    predicates = listOf(DiscoveryPredicate(type = DiscoveryPredicateType.PERCEPTION_CHECK, difficulty = 16)),
                                ),
                            searchBindingId = bindingId,
                        ),
                    ),
            )

        val proof = SolvabilityProver.prove(graph = graph, perceptionScore = PerceptionScore(baseMentalPower = 12))

        assertTrue(proof.criticalPathReachable)
        assertFalse(proof.visitedNodes.any { nodeId -> nodeId.value == "secret" })
        assertEquals(1, proof.searchActionCount)
        assertEquals(0, proof.searchRevealCount)
        assertEquals(1, proof.searchFailCount)
        assertEquals("FAILED_CHECK", proof.searchStates.single().result.name)
    }

    @Test
    fun `repeated frontier passes do not reroll the same search binding`() {
        val bindingId = SearchBindingId("search.greenwood.hidden_cache")
        val graph =
            SolvabilityGraph(
                entryNodeId = NodeId("start"),
                nodes =
                    listOf(
                        SolvabilityNode(NodeId("start"), NodeAnchorId("critical.start"), PathClass.CRITICAL_PATH, "room.start", emptySet()),
                        SolvabilityNode(NodeId("goal"), NodeAnchorId("critical.goal"), PathClass.CRITICAL_PATH, "room.goal", emptySet()),
                        SolvabilityNode(NodeId("secret"), NodeAnchorId("secret.greenwood.hidden_cache"), PathClass.SECRET, "room.secret", emptySet()),
                    ),
                edges =
                    listOf(
                        SolvabilityEdge(NodeId("start"), NodeId("goal"), emptySet()),
                        SolvabilityEdge(
                            from = NodeId("goal"),
                            to = NodeId("secret"),
                            requiredKeys = emptySet(),
                            discoveryRule =
                                DiscoveryRule(
                                    predicates = listOf(DiscoveryPredicate(type = DiscoveryPredicateType.PERCEPTION_CHECK, difficulty = 8)),
                                ),
                            searchBindingId = bindingId,
                        ),
                    ),
            )

        val proof = SolvabilityProver.prove(graph = graph, perceptionScore = PerceptionScore(baseMentalPower = 12))

        assertEquals(1, proof.searchActionCount)
        assertEquals(1, proof.searchRevealCount)
        assertEquals(0, proof.searchFailCount)
        assertEquals(1, proof.searchStates.size)
    }

    @Test
    fun `required tag discovery uses edge context tags in solvability proof`() {
        val bindingId = SearchBindingId("search.tagged.hidden_cache")
        val graph =
            SolvabilityGraph(
                entryNodeId = NodeId("start"),
                nodes =
                    listOf(
                        SolvabilityNode(NodeId("start"), NodeAnchorId("critical.start"), PathClass.CRITICAL_PATH, "room.start", emptySet()),
                        SolvabilityNode(NodeId("secret"), NodeAnchorId("secret.tagged.hidden_cache"), PathClass.SECRET, "room.secret", emptySet()),
                    ),
                edges =
                    listOf(
                        SolvabilityEdge(
                            from = NodeId("start"),
                            to = NodeId("secret"),
                            requiredKeys = emptySet(),
                            discoveryRule =
                                DiscoveryRule(
                                    predicates = listOf(DiscoveryPredicate(type = DiscoveryPredicateType.REQUIRED_TAG, requiredTag = "hidden_cache")),
                                ),
                            searchBindingId = bindingId,
                            discoveryContextTags = setOf("hidden_cache", "optional"),
                        ),
                    ),
            )

        val proof = SolvabilityProver.prove(graph = graph, perceptionScore = PerceptionScore(baseMentalPower = 0))

        assertTrue(proof.visitedNodes.any { nodeId -> nodeId.value == "secret" })
        assertEquals(1, proof.searchActionCount)
        assertEquals(1, proof.searchRevealCount)
        assertEquals(0, proof.searchFailCount)
        assertEquals("REVEALED", proof.searchStates.single().result.name)
    }
}
