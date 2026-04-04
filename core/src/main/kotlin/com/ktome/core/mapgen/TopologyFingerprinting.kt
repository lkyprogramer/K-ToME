package com.ktome.core.mapgen

import com.ktome.core.map.Point
import com.ktome.core.phase.Phase4ContractVersions
import java.security.MessageDigest

object TopologyFingerprinting {
    const val VERSION: Int = Phase4ContractVersions.TOPOLOGY_FINGERPRINT_VERSION

    fun fingerprint(topology: TopologyGraph): String =
        sha256(
            buildString {
                append("v=")
                append(VERSION)
                append("|nodes=")
                append(
                    topology.nodes
                        .sortedBy { node -> node.id.value }
                        .joinToString(separator = ";") { node ->
                            "${node.id.value}:${node.anchorId.value}:${node.roomDefId}:${node.pathClass.name}:${node.biomeFamilyId ?: "-"}:${node.tags.sorted().joinToString(",")}:${node.grants.map(RequirementRef::value).sorted().joinToString(",")}"
                        },
                )
                append("|edges=")
                append(
                    topology.edges
                        .sortedWith(compareBy<TopologyEdge> { it.from.value }.thenBy { it.to.value }.thenBy(TopologyEdge::isLoop))
                        .joinToString(separator = ";") { edge ->
                            "${edge.from.value}>${edge.to.value}:${edge.isLoop}:${edge.requiredKeys.map(RequirementRef::value).sorted().joinToString(",")}"
                        },
                )
                append("|primary=")
                append(topology.primaryPathNodeIds.joinToString(separator = ">") { nodeId -> nodeId.value })
                append("|loops=")
                append(topology.optionalLoopCount)
            },
        )

    fun terrainTagHash(terrainTags: Map<Point, Set<TerrainTag>>): String =
        sha256(
            buildString {
                append("v=")
                append(VERSION)
                append("|terrain=")
                append(
                    terrainTags.entries
                        .sortedWith(compareBy<Map.Entry<Point, Set<TerrainTag>>> { it.key.y }.thenBy { it.key.x })
                        .joinToString(separator = ";") { (point, tags) ->
                            "${point.x},${point.y}:${tags.map(TerrainTag::name).sorted().joinToString(",")}"
                        },
                )
            },
        )

    private fun sha256(payload: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
