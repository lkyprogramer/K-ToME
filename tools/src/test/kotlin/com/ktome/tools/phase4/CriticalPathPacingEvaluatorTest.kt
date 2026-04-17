package com.ktome.tools.phase4

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CriticalPathPacingEvaluatorTest {
    @Test
    fun `missing objective sample becomes unexpected regression with compact details`() {
        val evaluation =
            CriticalPathPacingEvaluator.evaluate(
                longRunMetrics =
                    buildJsonObject {
                        putJsonArray("criticalPathZoneIds") {
                            add(JsonPrimitive("greenwood_fringe"))
                            add(JsonPrimitive("grey_gate_depths"))
                        }
                        putJsonObject("fullRouteZoneTraversalDiagnostics") {
                            putJsonObject("greenwood_fringe") {
                                put("avgObjectiveAcquireTurn", 7.2)
                                put("avgVisibleHostileTurnCount", 14.3)
                                put("avgEnemyTurns", 69.8)
                            }
                            putJsonObject("grey_gate_depths") {
                                put("avgVisibleHostileTurnCount", 8.5)
                                put("avgEnemyTurns", 15.0)
                            }
                        }
                        putJsonObject("criticalPathZoneDesignAudit") {
                            put("greenwood_fringe", designAudit(zoneId = "greenwood_fringe", terrainTagWeights = mapOf("WATER" to 0.2, "ICE" to 0.1)))
                            put("grey_gate_depths", designAudit(zoneId = "grey_gate_depths"))
                        }
                    },
                thresholds =
                    testThresholds(),
            )

        val objectiveEntry = evaluation.entriesByMetricId.getValue("avgObjectiveAcquireTurn")
        val objectiveDetails = objectiveEntry.details()

        assertEquals(listOf("grey_gate_depths"), evaluation.evidence.sampleMissingZoneIds)
        assertEquals("UNEXPECTED_REGRESSION", objectiveEntry.status.name)
        assertEquals(listOf("grey_gate_depths"), objectiveEntry.zoneFailures)
        assertEquals("criticalPathPacing", objectiveDetails.getValue("sectionRef").jsonPrimitive.content)
        assertEquals("minimum", objectiveDetails.getValue("metricKind").jsonPrimitive.content)
        assertEquals("false", evaluation.entriesByMetricId.getValue("avgEnemyTurns").details().getValue("sampleMissing").jsonPrimitive.content)
        assertEquals("true", objectiveDetails.getValue("sampleMissing").jsonPrimitive.content)
    }

    @Test
    fun `missing zone diagnostics become degraded pacing evidence instead of aborting`() {
        val evaluation =
            CriticalPathPacingEvaluator.evaluate(
                longRunMetrics =
                    buildJsonObject {
                        putJsonArray("criticalPathZoneIds") {
                            add(JsonPrimitive("greenwood_fringe"))
                            add(JsonPrimitive("grey_gate_depths"))
                        }
                        putJsonObject("fullRouteZoneTraversalDiagnostics") {
                            putJsonObject("greenwood_fringe") {
                                put("avgObjectiveAcquireTurn", 7.2)
                                put("avgVisibleHostileTurnCount", 14.3)
                                put("avgEnemyTurns", 69.8)
                            }
                        }
                        putJsonObject("criticalPathZoneDesignAudit") {
                            put("greenwood_fringe", designAudit(zoneId = "greenwood_fringe"))
                            put("grey_gate_depths", designAudit(zoneId = "grey_gate_depths"))
                        }
                    },
                thresholds =
                    testThresholds(),
            )

        assertEquals(listOf("grey_gate_depths"), evaluation.evidence.sampleMissingZoneIds)
        assertEquals(listOf("grey_gate_depths"), evaluation.entriesByMetricId.getValue("avgObjectiveAcquireTurn").zoneFailures)
        assertEquals(listOf("grey_gate_depths"), evaluation.entriesByMetricId.getValue("avgVisibleHostileTurnCount").zoneFailures)
        assertEquals(listOf("grey_gate_depths"), evaluation.entriesByMetricId.getValue("avgEnemyTurns").zoneFailures)
    }

    @Test
    fun `missing visible hostile metric fails fast instead of falling back to zero`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                CriticalPathPacingEvaluator.evaluate(
                    longRunMetrics =
                        buildJsonObject {
                            putJsonArray("criticalPathZoneIds") {
                                add(JsonPrimitive("greenwood_fringe"))
                            }
                            putJsonObject("fullRouteZoneTraversalDiagnostics") {
                                putJsonObject("greenwood_fringe") {
                                    put("avgObjectiveAcquireTurn", 7.2)
                                    put("avgEnemyTurns", 69.8)
                                }
                            }
                            putJsonObject("criticalPathZoneDesignAudit") {
                                put("greenwood_fringe", designAudit(zoneId = "greenwood_fringe"))
                            }
                        },
                    thresholds =
                        testThresholds(),
                )
            }

        assertEquals(true, error.message.orEmpty().contains("avgVisibleHostileTurnCount"))
        assertEquals(true, error.message.orEmpty().contains("greenwood_fringe"))
    }

    @Test
    fun `design audit stays ordered and non-gating for pacing verdicts`() {
        val thresholds = testThresholds()
        val evaluation = CriticalPathPacingEvaluator.evaluate(longRunMetrics(), thresholds)
        val mutatedEvaluation =
            CriticalPathPacingEvaluator.evaluate(
                longRunMetrics().withMutatedAuditMapSize(zoneId = "greenwood_fringe", mapSize = "999x999"),
                thresholds,
            )
        val sortedTerrainTagKeys =
            evaluation.evidence.designAudit
                .first { audit -> audit.zoneId == "greenwood_fringe" }
                .toJson()
                .getValue("terrainTagWeights")
                .jsonObject
                .keys
                .toList()

        assertEquals(
            evaluation.evidence.criticalPathZoneIds,
            evaluation.evidence.designAudit.map(CriticalPathZoneDesignAuditSnapshot::zoneId),
        )
        assertEquals(sortedTerrainTagKeys.sorted(), sortedTerrainTagKeys)
        assertEquals(
            evaluation.toEvaluationResult(domainId = "longrun", evaluationId = "longrun.criticalPathPacing").entries.map { entry ->
                listOf(entry.metricId, entry.status.name, entry.currentValue.toString(), entry.currentValueText)
            },
            mutatedEvaluation.toEvaluationResult(domainId = "longrun", evaluationId = "longrun.criticalPathPacing").entries.map { entry ->
                listOf(entry.metricId, entry.status.name, entry.currentValue.toString(), entry.currentValueText)
            },
        )
    }

    @Test
    fun `missing critical path design audit zone fails fast`() {
        val thresholds = testThresholds()
        val error =
            assertThrows(IllegalStateException::class.java) {
                CriticalPathPacingEvaluator.evaluate(
                    longRunMetrics().withRemovedAuditZone(zoneId = "grey_gate_depths"),
                    thresholds,
                )
            }

        assertEquals(true, error.message.orEmpty().contains("grey_gate_depths"))
    }

    private fun longRunMetrics(): JsonObject =
        buildJsonObject {
            putJsonArray("criticalPathZoneIds") {
                add(JsonPrimitive("greenwood_fringe"))
                add(JsonPrimitive("grey_gate_depths"))
            }
            putJsonObject("fullRouteZoneTraversalDiagnostics") {
                putJsonObject("greenwood_fringe") {
                    put("avgObjectiveAcquireTurn", 7.2)
                    put("avgVisibleHostileTurnCount", 14.3)
                    put("avgEnemyTurns", 69.8)
                }
                putJsonObject("grey_gate_depths") {
                    put("avgObjectiveAcquireTurn", 6.1)
                    put("avgVisibleHostileTurnCount", 8.5)
                    put("avgEnemyTurns", 15.0)
                }
            }
            putJsonObject("criticalPathZoneDesignAudit") {
                put("greenwood_fringe", designAudit(zoneId = "greenwood_fringe", terrainTagWeights = mapOf("WATER" to 0.2, "ICE" to 0.1)))
                put("grey_gate_depths", designAudit(zoneId = "grey_gate_depths", terrainTagWeights = mapOf("MUD" to 0.4, "STONE" to 0.6)))
            }
        }

    private fun testThresholds(): CriticalPathPacingThresholds =
        CriticalPathPacingThresholds(
            objectiveFloor = 4.0,
            visibleFloor = 1.0,
            enemyFloor = 1.0,
            satisfiedFloor = 1.0,
            targetTextByMetricId =
                mapOf(
                    "avgObjectiveAcquireTurn" to ">= 4.000",
                    "avgVisibleHostileTurnCount" to ">= 1.000",
                    "avgEnemyTurns" to ">= 1.000",
                    "criticalPathCombatFloorSatisfied" to ">= 1.000",
                ),
        )

    private fun designAudit(
        zoneId: String,
        terrainTagWeights: Map<String, Double> = emptyMap(),
    ): JsonObject =
        buildJsonObject {
            put("floorCount", 2)
            put("mapSize", "70x45")
            put("worldRole", "mandatory")
            put("monsterPoolCount", 6)
            put("elitePoolCount", 2)
            put("objectiveSetId", "${zoneId}_objective")
            put("objectiveCompletionRule", "explore_floor_pair")
            putJsonArray("specialMechanics") {
                add(JsonPrimitive("line_of_sight"))
            }
            putJsonArray("mechanicsWithoutDedicatedRuntimeHook") {
                add(JsonPrimitive("line_of_sight"))
            }
            putJsonArray("objectivePlacements") {
                add(JsonPrimitive("${zoneId}_placement"))
            }
            putJsonObject("terrainTagWeights") {
                terrainTagWeights.forEach { (terrainTag, weight) ->
                    put(terrainTag, weight)
                }
            }
        }

    private fun JsonObject.withMutatedAuditMapSize(
        zoneId: String,
        mapSize: String,
    ): JsonObject =
        buildJsonObject {
            forEach { (key, value) ->
                if (key == "criticalPathZoneDesignAudit") {
                    putJsonObject(key) {
                        value.jsonObject.forEach { (auditZoneId, auditValue) ->
                            put(
                                auditZoneId,
                                if (auditZoneId == zoneId) {
                                    buildJsonObject {
                                        auditValue.jsonObject.forEach { (auditKey, auditEntry) ->
                                            put(auditKey, if (auditKey == "mapSize") JsonPrimitive(mapSize) else auditEntry)
                                        }
                                    }
                                } else {
                                    auditValue
                                },
                            )
                        }
                    }
                } else {
                    put(key, value)
                }
            }
        }

    private fun JsonObject.withRemovedAuditZone(zoneId: String): JsonObject =
        buildJsonObject {
            forEach { (key, value) ->
                if (key == "criticalPathZoneDesignAudit") {
                    putJsonObject(key) {
                        value.jsonObject.forEach { (auditZoneId, auditValue) ->
                            if (auditZoneId != zoneId) {
                                put(auditZoneId, auditValue)
                            }
                        }
                    }
                } else {
                    put(key, value)
                }
            }
        }
}
