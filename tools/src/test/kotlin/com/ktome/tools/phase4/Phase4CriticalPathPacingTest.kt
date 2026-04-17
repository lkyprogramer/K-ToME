package com.ktome.tools.phase4

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class Phase4CriticalPathPacingTest {
    @Test
    fun `missing critical path zone diagnostics fail fast`() {
        val error =
            assertThrows(IllegalStateException::class.java) {
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
                }
                    .toCriticalPathPacingSummary()
            }

        assertTrue(error.message.orEmpty().contains("grey_gate_depths"))
        assertTrue(error.message.orEmpty().contains("fullRouteZoneTraversalDiagnostics"))
    }
}
