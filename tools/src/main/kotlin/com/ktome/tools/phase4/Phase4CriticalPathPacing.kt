package com.ktome.tools.phase4

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

internal data class CriticalPathZonePacingSnapshot(
    val zoneId: String,
    val avgObjectiveAcquireTurn: Double?,
    val avgVisibleHostileTurnCount: Double,
    val avgEnemyTurns: Double,
)

internal data class CriticalPathPacingSummary(
    val zoneIds: List<String>,
    val zonesById: Map<String, CriticalPathZonePacingSnapshot>,
) {
    init {
        require(zoneIds.isNotEmpty()) { "CriticalPathPacingSummary.zoneIds must not be empty." }
        require(zoneIds.all(zonesById::containsKey)) {
            "CriticalPathPacingSummary.zonesById must cover all criticalPathZoneIds."
        }
    }

    fun minimumObjectiveAcquireTurn(): Double? =
        zoneIds
            .map { zoneId -> zonesById.getValue(zoneId).avgObjectiveAcquireTurn }
            .takeIf { objectiveTurns -> objectiveTurns.all { objectiveTurn -> objectiveTurn != null } }
            ?.let { objectiveTurns -> objectiveTurns.map { objectiveTurn -> requireNotNull(objectiveTurn) } }
            ?.minOrNull()

    fun minimumVisibleHostileTurnCount(): Double =
        zoneIds.minOf { zoneId -> zonesById.getValue(zoneId).avgVisibleHostileTurnCount }

    fun minimumEnemyTurns(): Double =
        zoneIds.minOf { zoneId -> zonesById.getValue(zoneId).avgEnemyTurns }

    fun failingObjectiveZones(target: Double): List<String> =
        zoneIds.filter { zoneId ->
            val current = zonesById.getValue(zoneId).avgObjectiveAcquireTurn
            current == null || current < target
        }

    fun failingVisibleHostileZones(target: Double): List<String> =
        zoneIds.filter { zoneId -> zonesById.getValue(zoneId).avgVisibleHostileTurnCount < target }

    fun failingEnemyTurnZones(target: Double): List<String> =
        zoneIds.filter { zoneId -> zonesById.getValue(zoneId).avgEnemyTurns < target }

    fun satisfiedZoneIds(
        objectiveAcquireFloor: Double,
        visibleHostileFloor: Double,
        enemyTurnFloor: Double,
    ): List<String> =
        zoneIds.filter { zoneId ->
            val zone = zonesById.getValue(zoneId)
            val objectiveSatisfied = zone.avgObjectiveAcquireTurn != null && zone.avgObjectiveAcquireTurn >= objectiveAcquireFloor
            objectiveSatisfied &&
                zone.avgVisibleHostileTurnCount >= visibleHostileFloor &&
                zone.avgEnemyTurns >= enemyTurnFloor
        }

    fun satisfiedRatio(
        objectiveAcquireFloor: Double,
        visibleHostileFloor: Double,
        enemyTurnFloor: Double,
    ): Double =
        if (zoneIds.isEmpty()) {
            0.0
        } else {
            satisfiedZoneIds(
                objectiveAcquireFloor = objectiveAcquireFloor,
                visibleHostileFloor = visibleHostileFloor,
                enemyTurnFloor = enemyTurnFloor,
            ).size.toDouble() / zoneIds.size.toDouble()
        }

    fun perZoneBreakdownJson(
        objectiveAcquireFloor: Double,
        visibleHostileFloor: Double,
        enemyTurnFloor: Double,
    ): JsonObject =
        buildJsonObject {
            zoneIds.forEach { zoneId ->
                val zone = zonesById.getValue(zoneId)
                putJsonObject(zoneId) {
                    zone.avgObjectiveAcquireTurn?.let { put("avgObjectiveAcquireTurn", it) }
                    put("avgVisibleHostileTurnCount", zone.avgVisibleHostileTurnCount)
                    put("avgEnemyTurns", zone.avgEnemyTurns)
                    put(
                        "satisfied",
                        zone.avgObjectiveAcquireTurn != null &&
                            zone.avgObjectiveAcquireTurn >= objectiveAcquireFloor &&
                            zone.avgVisibleHostileTurnCount >= visibleHostileFloor &&
                            zone.avgEnemyTurns >= enemyTurnFloor,
                    )
                }
            }
        }
}

internal data class CriticalPathPacingEvaluationSnapshot(
    val zoneIds: List<String>,
    val objectiveFloor: Double,
    val visibleHostileFloor: Double,
    val enemyTurnFloor: Double,
    val objectiveMinimum: Double?,
    val visibleMinimum: Double,
    val enemyMinimum: Double,
    val objectiveFailures: List<String>,
    val visibleFailures: List<String>,
    val enemyFailures: List<String>,
    val satisfiedZoneIds: List<String>,
    val satisfiedRatio: Double,
    val satisfiedFailures: List<String>,
    val zoneBreakdown: JsonObject,
) {
    fun note(format: (Double) -> String): String =
        "criticalPathZoneIds=${zoneIds.joinToString()}, " +
            "objectiveFloor=${format(objectiveFloor)}, visibleFloor=${format(visibleHostileFloor)}, enemyFloor=${format(enemyTurnFloor)}"

    fun objectiveMetricValue(): JsonObject =
        buildJsonObject {
            objectiveMinimum?.let { put("minimum", it) }
            put("target", objectiveFloor)
            put("criticalPathZoneIds", zoneIds.toJsonArray())
            put("failingZones", objectiveFailures.toJsonArray())
        }

    fun visibleMetricValue(): JsonObject =
        buildJsonObject {
            put("minimum", visibleMinimum)
            put("target", visibleHostileFloor)
            put("criticalPathZoneIds", zoneIds.toJsonArray())
            put("failingZones", visibleFailures.toJsonArray())
        }

    fun enemyMetricValue(): JsonObject =
        buildJsonObject {
            put("minimum", enemyMinimum)
            put("target", enemyTurnFloor)
            put("criticalPathZoneIds", zoneIds.toJsonArray())
            put("failingZones", enemyFailures.toJsonArray())
        }

    fun satisfiedMetricValue(): JsonObject =
        buildJsonObject {
            put("rate", satisfiedRatio)
            put("satisfiedZoneCount", satisfiedZoneIds.size)
            put("criticalPathZoneCount", zoneIds.size)
            put("criticalPathZoneIds", zoneIds.toJsonArray())
            put("failingZones", satisfiedFailures.toJsonArray())
            put("zoneBreakdown", zoneBreakdown)
        }
}

internal fun CriticalPathPacingSummary.evaluate(
    objectiveAcquireFloor: Double,
    visibleHostileFloor: Double,
    enemyTurnFloor: Double,
): CriticalPathPacingEvaluationSnapshot {
    val objectiveFailures = failingObjectiveZones(objectiveAcquireFloor)
    val visibleFailures = failingVisibleHostileZones(visibleHostileFloor)
    val enemyFailures = failingEnemyTurnZones(enemyTurnFloor)
    val satisfiedZoneIds =
        satisfiedZoneIds(
            objectiveAcquireFloor = objectiveAcquireFloor,
            visibleHostileFloor = visibleHostileFloor,
            enemyTurnFloor = enemyTurnFloor,
        )
    val satisfiedRatio =
        satisfiedRatio(
            objectiveAcquireFloor = objectiveAcquireFloor,
            visibleHostileFloor = visibleHostileFloor,
            enemyTurnFloor = enemyTurnFloor,
        )
    return CriticalPathPacingEvaluationSnapshot(
        zoneIds = zoneIds,
        objectiveFloor = objectiveAcquireFloor,
        visibleHostileFloor = visibleHostileFloor,
        enemyTurnFloor = enemyTurnFloor,
        objectiveMinimum = minimumObjectiveAcquireTurn(),
        visibleMinimum = minimumVisibleHostileTurnCount(),
        enemyMinimum = minimumEnemyTurns(),
        objectiveFailures = objectiveFailures,
        visibleFailures = visibleFailures,
        enemyFailures = enemyFailures,
        satisfiedZoneIds = satisfiedZoneIds,
        satisfiedRatio = satisfiedRatio,
        satisfiedFailures = zoneIds.filterNot(satisfiedZoneIds::contains),
        zoneBreakdown =
            perZoneBreakdownJson(
                objectiveAcquireFloor = objectiveAcquireFloor,
                visibleHostileFloor = visibleHostileFloor,
                enemyTurnFloor = enemyTurnFloor,
            ),
    )
}

internal fun JsonObject.toCriticalPathPacingSummary(): CriticalPathPacingSummary {
    val zoneIds = getValue("criticalPathZoneIds").jsonArray.map { zoneId -> zoneId.jsonPrimitive.content }
    val diagnostics = getValue("fullRouteZoneTraversalDiagnostics").jsonObject
    return CriticalPathPacingSummary(
        zoneIds = zoneIds,
        zonesById =
            zoneIds.associateWith { zoneId ->
                diagnostics[zoneId]?.jsonObject?.toCriticalPathZonePacingSnapshot(zoneId)
                    ?: missingCriticalPathZonePacingSnapshot(zoneId)
            },
    )
}

private fun JsonObject.toCriticalPathZonePacingSnapshot(zoneId: String): CriticalPathZonePacingSnapshot =
    CriticalPathZonePacingSnapshot(
        zoneId = zoneId,
        avgObjectiveAcquireTurn = this["avgObjectiveAcquireTurn"]?.jsonPrimitive?.content?.toDoubleOrNull(),
        avgVisibleHostileTurnCount = this["avgVisibleHostileTurnCount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
        avgEnemyTurns = this["avgEnemyTurns"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
    )

private fun missingCriticalPathZonePacingSnapshot(zoneId: String): CriticalPathZonePacingSnapshot =
    CriticalPathZonePacingSnapshot(
        zoneId = zoneId,
        avgObjectiveAcquireTurn = null,
        avgVisibleHostileTurnCount = 0.0,
        avgEnemyTurns = 0.0,
    )

internal fun List<String>.toJsonArray(): JsonArray =
    buildJsonArray {
        this@toJsonArray.forEach { value -> add(JsonPrimitive(value)) }
    }
