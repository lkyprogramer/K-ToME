package com.ktome.tools.phase4

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class Phase4CriticalPathPacingTest {
    @Test
    fun `missing critical path zone diagnostics materialize as failed pacing evidence`() {
        val summary =
            buildJsonObject {
                put(
                    "criticalPathZoneIds",
                    buildJsonArray {
                        add(JsonPrimitive("greenwood_fringe"))
                        add(JsonPrimitive("grey_gate_depths"))
                    },
                )
                putJsonObject("fullRouteZoneTraversalDiagnostics") {
                    putJsonObject("greenwood_fringe") {
                        put("avgObjectiveAcquireTurn", 7.2)
                        put("avgVisibleHostileTurnCount", 14.3)
                        put("avgEnemyTurns", 69.8)
                    }
                }
            }.toCriticalPathPacingSummary()

        val missingZone = summary.zonesById.getValue("grey_gate_depths")

        assertNull(missingZone.avgObjectiveAcquireTurn)
        assertEquals(0.0, missingZone.avgVisibleHostileTurnCount)
        assertEquals(0.0, missingZone.avgEnemyTurns)
        assertEquals(listOf("grey_gate_depths"), summary.failingObjectiveZones(target = 4.0))
        assertEquals(listOf("grey_gate_depths"), summary.failingVisibleHostileZones(target = 1.0))
        assertEquals(listOf("grey_gate_depths"), summary.failingEnemyTurnZones(target = 1.0))

        val breakdown =
            summary.perZoneBreakdownJson(
                objectiveAcquireFloor = 4.0,
                visibleHostileFloor = 1.0,
                enemyTurnFloor = 1.0,
            )

        assertEquals(
            JsonPrimitive(false),
            breakdown.getValue("grey_gate_depths").jsonObject.getValue("satisfied"),
        )
    }
}
