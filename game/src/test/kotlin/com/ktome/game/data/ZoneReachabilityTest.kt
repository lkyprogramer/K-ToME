package com.ktome.game.data

import com.ktome.game.i18n.GameLocale
import java.util.ArrayDeque
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZoneReachabilityTest {
    @Test
    fun `world graph reaches all mandatory and optional zones from start`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        val graph = catalog.worldGraph
        val visited = linkedSetOf<String>()
        val queue = ArrayDeque<String>()
        queue += graph.startZoneId

        while (queue.isNotEmpty()) {
            val zoneId = queue.removeFirst()
            if (!visited.add(zoneId)) {
                continue
            }
            graph.outgoingConnections(zoneId)
                .map { connection -> graph.destinationFor(zoneId, connection) }
                .filterNot(visited::contains)
                .forEach(queue::addLast)
        }

        assertEquals(catalog.zones.map { zone -> zone.id }.toSet(), visited)
        assertTrue(setOf("bandit_camp", "elven_ruins", "molten_core", "crystal_cavern").all(visited::contains))
    }
}
