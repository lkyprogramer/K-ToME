package com.ktome.core.world.solvability

import com.ktome.core.ecs.EntityId
import com.ktome.core.mapgen.NodeId
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.RequirementRef
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class NodeAnchorId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "NodeAnchorId must not be blank." }
    }

    override fun toString(): String = value
}

@Serializable
@JvmInline
value class SearchBindingId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "SearchBindingId must not be blank." }
    }

    override fun toString(): String = value
}

@Serializable
@JvmInline
value class RegistryId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "RegistryId must not be blank." }
    }

    override fun toString(): String = value
}

@Serializable
data class ContentRef(
    val registry: RegistryId,
    val id: String,
) {
    init {
        require(id.isNotBlank()) { "ContentRef.id must not be blank." }
    }
}

@Serializable
enum class KeyType {
    KEY_ITEM,
    SWITCH,
    BOSS_SIGIL,
    QUEST_FLAG,
    PERCEPTION_REVEAL,
}

@Serializable
enum class DiscoveryPredicateType {
    PERCEPTION_CHECK,
    REQUIRED_TAG,
}

@Serializable
enum class RuleCombinator {
    AND,
    OR,
}

@Serializable
data class DiscoveryPredicate(
    val type: DiscoveryPredicateType,
    val difficulty: Int? = null,
    val requiredTag: String? = null,
) {
    init {
        require(difficulty == null || difficulty >= 0) { "DiscoveryPredicate.difficulty must not be negative." }
        require(requiredTag == null || requiredTag.isNotBlank()) {
            "DiscoveryPredicate.requiredTag must not be blank when present."
        }
        when (type) {
            DiscoveryPredicateType.PERCEPTION_CHECK -> requireNotNull(difficulty) {
                "PERCEPTION_CHECK predicates must declare difficulty."
            }

            DiscoveryPredicateType.REQUIRED_TAG -> requireNotNull(requiredTag) {
                "REQUIRED_TAG predicates must declare requiredTag."
            }
        }
    }
}

@Serializable
data class DiscoveryRule(
    val combinator: RuleCombinator = RuleCombinator.AND,
    val predicates: List<DiscoveryPredicate>,
) {
    init {
        require(predicates.isNotEmpty()) { "DiscoveryRule.predicates must not be empty." }
    }

    fun evaluate(
        perceptionScore: PerceptionScore,
        providedTags: Set<String> = emptySet(),
    ): Boolean {
        val results =
            predicates.map { predicate ->
                when (predicate.type) {
                    DiscoveryPredicateType.PERCEPTION_CHECK -> perceptionScore.total >= requireNotNull(predicate.difficulty)
                    DiscoveryPredicateType.REQUIRED_TAG -> requireNotNull(predicate.requiredTag) in providedTags
                }
            }
        return when (combinator) {
            RuleCombinator.AND -> results.all { matched -> matched }
            RuleCombinator.OR -> results.any { matched -> matched }
        }
    }

    fun perceptionDifficulty(): Int? =
        predicates.firstOrNull { predicate -> predicate.type == DiscoveryPredicateType.PERCEPTION_CHECK }?.difficulty
}

@Serializable
data class PerceptionScore(
    val baseMentalPower: Int,
    val equipmentBonus: Int = 0,
    val buffBonus: Int = 0,
    val passiveBonus: Int = 0,
) {
    val total: Int
        get() = baseMentalPower + equipmentBonus + buffBonus + passiveBonus
}

@Serializable
data class SearchAction(
    val bindingId: SearchBindingId,
    val actorId: EntityId,
)

@Serializable
enum class SearchActionResult {
    REVEALED,
    FAILED_CHECK,
    NO_TARGET,
    ALREADY_RESOLVED,
}

@Serializable
data class ResolvedEntranceBinding(
    val searchBindingId: SearchBindingId,
    val entranceAnchorId: NodeAnchorId,
    val resolvedTargetNodeId: NodeId,
) 

@Serializable
data class SearchStateEntry(
    val bindingId: SearchBindingId,
    val result: SearchActionResult,
)

fun Iterable<SearchStateEntry>.revealedBindingIds(): Set<SearchBindingId> =
    filterTo(linkedSetOf()) { entry -> entry.result == SearchActionResult.REVEALED }
        .mapTo(linkedSetOf()) { entry -> entry.bindingId }

data class SolvabilityNode(
    val id: NodeId,
    val anchorId: NodeAnchorId,
    val pathClass: PathClass,
    val roomId: String,
    val grants: Set<RequirementRef>,
)

data class SolvabilityEdge(
    val from: NodeId,
    val to: NodeId,
    val requiredKeys: Set<RequirementRef>,
    val discoveryRule: DiscoveryRule? = null,
    val searchBindingId: SearchBindingId? = null,
    val discoveryContextTags: Set<String> = emptySet(),
) {
    init {
        require((discoveryRule == null) == (searchBindingId == null)) {
            "SolvabilityEdge discoveryRule and searchBindingId must be declared together."
        }
        require(discoveryContextTags.all(String::isNotBlank)) {
            "SolvabilityEdge.discoveryContextTags must not contain blank tags."
        }
        require(discoveryRule != null || discoveryContextTags.isEmpty()) {
            "SolvabilityEdge.discoveryContextTags require a discoveryRule."
        }
    }
}

data class SolvabilityGraph(
    val entryNodeId: NodeId,
    val nodes: List<SolvabilityNode>,
    val edges: List<SolvabilityEdge>,
) {
    init {
        require(nodes.isNotEmpty()) { "SolvabilityGraph.nodes must not be empty." }
        val nodeIds = nodes.map(SolvabilityNode::id)
        require(nodeIds.distinct().size == nodeIds.size) { "SolvabilityGraph.nodes must not contain duplicate ids." }
        require(entryNodeId in nodeIds) { "SolvabilityGraph.entryNodeId must reference a declared node." }
        require(edges.all { edge -> edge.from in nodeIds && edge.to in nodeIds }) {
            "SolvabilityGraph.edges must only reference declared nodes."
        }
    }
}

data class SolvabilityProof(
    val criticalPathReachable: Boolean,
    val acquiredKeys: List<RequirementRef>,
    val unresolvedRequirements: List<RequirementRef>,
    val visitedNodes: List<NodeId>,
    val optionalPathCount: Int,
    val secretPathCount: Int,
    val totalReachableNodes: Int,
    val reachabilityRatio: Float,
    val searchActionCount: Int,
    val searchRevealCount: Int,
    val searchFailCount: Int,
    val searchStates: List<SearchStateEntry>,
)
