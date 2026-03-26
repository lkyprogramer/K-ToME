package com.ktome.core.world

import kotlinx.serialization.Serializable

@Serializable
data class WorldGraph(
    val startZoneId: String,
    val connections: List<ZoneConnection>,
) {
    init {
        require(startZoneId.isNotBlank()) { "WorldGraph.startZoneId must not be blank." }
        require(connections.distinctBy(ZoneConnection::id).size == connections.size) {
            "WorldGraph connections must not contain duplicate ids."
        }
    }

    fun outgoingConnections(zoneId: String): List<ZoneConnection> =
        connections.filter { connection ->
            connection.fromZoneId == zoneId || (connection.isBidirectional && connection.toZoneId == zoneId)
        }

    fun destinationFor(
        zoneId: String,
        connection: ZoneConnection,
    ): String =
        when (zoneId) {
            connection.fromZoneId -> connection.toZoneId
            connection.toZoneId ->
                require(connection.isBidirectional) {
                    "Zone '$zoneId' cannot traverse one-way connection '${connection.id}' in reverse."
                }.let { connection.fromZoneId }

            else -> error("Zone '$zoneId' is not part of connection '${connection.id}'.")
        }
}

@Serializable
data class ZoneConnection(
    val id: String,
    val fromZoneId: String,
    val toZoneId: String,
    val isBidirectional: Boolean = true,
    val gate: GateCondition = GateCondition(),
) {
    init {
        require(id.isNotBlank()) { "ZoneConnection.id must not be blank." }
        require(fromZoneId.isNotBlank()) { "ZoneConnection.fromZoneId must not be blank." }
        require(toZoneId.isNotBlank()) { "ZoneConnection.toZoneId must not be blank." }
        require(fromZoneId != toZoneId) { "ZoneConnection '$id' must connect two distinct zones." }
    }
}
