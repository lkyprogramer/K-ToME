package com.ktome.core.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WorldGraphTest {
    @Test
    fun `bidirectional connection resolves both directions`() {
        val graph =
            WorldGraph(
                startZoneId = "shattered_outpost",
                connections =
                    listOf(
                        ZoneConnection(
                            id = "route.shattered_outpost.greenwood_fringe",
                            fromZoneId = "shattered_outpost",
                            toZoneId = "greenwood_fringe",
                            isBidirectional = true,
                        ),
                    ),
            )

        val connection = graph.outgoingConnections("greenwood_fringe").single()

        assertEquals("shattered_outpost", graph.destinationFor("greenwood_fringe", connection))
        assertEquals("greenwood_fringe", graph.destinationFor("shattered_outpost", connection))
    }

    @Test
    fun `reverse traversal on one way edge is rejected`() {
        val graph =
            WorldGraph(
                startZoneId = "shattered_outpost",
                connections =
                    listOf(
                        ZoneConnection(
                            id = "route.abyssal_temple.abyssal_heart",
                            fromZoneId = "abyssal_temple",
                            toZoneId = "abyssal_heart",
                            isBidirectional = false,
                        ),
                    ),
            )
        val connection = graph.connections.single()

        assertThrows(IllegalArgumentException::class.java) {
            graph.destinationFor("abyssal_heart", connection)
        }
    }
}
