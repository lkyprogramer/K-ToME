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
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal data class CriticalPathZonePacingSnapshot(
    val zoneId: String,
    val avgObjectiveAcquireTurn: Double?,
    val avgVisibleHostileTurnCount: Double,
    val avgEnemyTurns: Double,
)

internal data class CriticalPathZoneDesignAuditSnapshot(
    val zoneId: String,
    val floorCount: Int,
    val mapSize: String,
    val worldRole: String,
    val monsterPoolCount: Int,
    val elitePoolCount: Int,
    val bossEncounterId: String?,
    val objectiveSetId: String,
    val objectiveCompletionRule: String,
    val specialMechanics: List<String>,
    val allMechanicTerms: List<String>,
    val runtimeHookIds: List<String>,
    val flavorOnlyMechanics: List<String>,
    val mechanicTermsPartitioned: Boolean,
    val mechanicsWithoutDedicatedRuntimeHook: List<String>,
    val objectivePlacements: List<String>,
    val terrainTagWeights: Map<String, Double>,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("zoneId", zoneId)
            put("floorCount", floorCount)
            put("mapSize", mapSize)
            put("worldRole", worldRole)
            put("monsterPoolCount", monsterPoolCount)
            put("elitePoolCount", elitePoolCount)
            bossEncounterId?.let { value -> put("bossEncounterId", value) }
            put("objectiveSetId", objectiveSetId)
            put("objectiveCompletionRule", objectiveCompletionRule)
            putJsonArray("specialMechanics") {
                specialMechanics.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
            }
            putJsonArray("allMechanicTerms") {
                allMechanicTerms.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
            }
            putJsonArray("runtimeHookIds") {
                runtimeHookIds.forEach { hookId -> add(JsonPrimitive(hookId)) }
            }
            putJsonArray("flavorOnlyMechanics") {
                flavorOnlyMechanics.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
            }
            put("mechanicTermsPartitioned", mechanicTermsPartitioned)
            putJsonArray("mechanicsWithoutDedicatedRuntimeHook") {
                mechanicsWithoutDedicatedRuntimeHook.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
            }
            putJsonArray("objectivePlacements") {
                objectivePlacements.forEach { placement -> add(JsonPrimitive(placement)) }
            }
            putJsonObject("terrainTagWeights") {
                terrainTagWeights.toSortedMap().forEach { (terrainTag, weight) ->
                    put(terrainTag, weight)
                }
            }
        }
}

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

    fun zoneSnapshotsInOrder(): List<CriticalPathZonePacingSnapshot> =
        zoneIds.map { zoneId -> zonesById.getValue(zoneId) }

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
        satisfiedZoneIds(
            objectiveAcquireFloor = objectiveAcquireFloor,
            visibleHostileFloor = visibleHostileFloor,
            enemyTurnFloor = enemyTurnFloor,
        ).size.toDouble() / zoneIds.size.toDouble()

    fun perZoneBreakdownJson(
        objectiveAcquireFloor: Double,
        visibleHostileFloor: Double,
        enemyTurnFloor: Double,
    ): JsonObject =
        buildJsonObject {
            zoneIds.forEach { zoneId ->
                val zone = zonesById.getValue(zoneId)
                putJsonObject(zoneId) {
                    zone.avgObjectiveAcquireTurn?.let { value -> put("avgObjectiveAcquireTurn", value) }
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

internal fun JsonObject.toCriticalPathDesignAuditSnapshots(criticalPathZoneIds: List<String>): List<CriticalPathZoneDesignAuditSnapshot> {
    val payload = getValue("criticalPathZoneDesignAudit").jsonObject
    val extraZoneIds = payload.keys - criticalPathZoneIds.toSet()
    require(extraZoneIds.isEmpty()) {
        "criticalPathZoneDesignAudit declared non-critical zones: ${extraZoneIds.sorted().joinToString()}."
    }
    return criticalPathZoneIds.map { zoneId ->
        payload[zoneId]?.jsonObject?.toCriticalPathZoneDesignAuditSnapshot(zoneId)
            ?: error("criticalPathZoneDesignAudit missing entry for critical-path zone '$zoneId'.")
    }
}

internal fun JsonArray.toCriticalPathDesignAuditSnapshots(): List<CriticalPathZoneDesignAuditSnapshot> =
    map { element ->
        val payload = element.jsonObject
        payload.toCriticalPathZoneDesignAuditSnapshot(
            zoneId = payload.getValue("zoneId").jsonPrimitive.content,
        )
    }

private fun JsonObject.toCriticalPathZonePacingSnapshot(zoneId: String): CriticalPathZonePacingSnapshot =
    CriticalPathZonePacingSnapshot(
        zoneId = zoneId,
        avgObjectiveAcquireTurn = this["avgObjectiveAcquireTurn"]?.jsonPrimitive?.content?.toDoubleOrNull(),
        avgVisibleHostileTurnCount =
            requiredDouble(
                key = "avgVisibleHostileTurnCount",
                zoneId = zoneId,
            ),
        avgEnemyTurns =
            requiredDouble(
                key = "avgEnemyTurns",
                zoneId = zoneId,
            ),
    )

private fun JsonObject.toCriticalPathZoneDesignAuditSnapshot(zoneId: String): CriticalPathZoneDesignAuditSnapshot =
    CriticalPathZoneDesignAuditSnapshot(
        zoneId = zoneId,
        floorCount = getValue("floorCount").jsonPrimitive.content.toInt(),
        mapSize = getValue("mapSize").jsonPrimitive.content,
        worldRole = getValue("worldRole").jsonPrimitive.content,
        monsterPoolCount = getValue("monsterPoolCount").jsonPrimitive.content.toInt(),
        elitePoolCount = getValue("elitePoolCount").jsonPrimitive.content.toInt(),
        bossEncounterId = this["bossEncounterId"]?.jsonPrimitive?.content,
        objectiveSetId = getValue("objectiveSetId").jsonPrimitive.content,
        objectiveCompletionRule = getValue("objectiveCompletionRule").jsonPrimitive.content,
        specialMechanics = stringList("specialMechanics"),
        allMechanicTerms = optionalStringList("allMechanicTerms") ?: stringList("specialMechanics"),
        runtimeHookIds = optionalStringList("runtimeHookIds").orEmpty(),
        flavorOnlyMechanics =
            optionalStringList("flavorOnlyMechanics")
                ?: stringList("mechanicsWithoutDedicatedRuntimeHook"),
        mechanicTermsPartitioned =
            this["mechanicTermsPartitioned"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                ?: true,
        mechanicsWithoutDedicatedRuntimeHook =
            optionalStringList("mechanicsWithoutDedicatedRuntimeHook")
                ?: optionalStringList("flavorOnlyMechanics")
                ?: emptyList(),
        objectivePlacements = stringList("objectivePlacements"),
        terrainTagWeights =
            getValue("terrainTagWeights").jsonObject.entries
                .sortedBy(Map.Entry<String, kotlinx.serialization.json.JsonElement>::key)
                .associate { (terrainTag, weight) ->
                    terrainTag to weight.jsonPrimitive.content.toDouble()
                },
    )

private fun JsonObject.stringList(key: String): List<String> =
    getValue(key).jsonArray.map { element -> element.jsonPrimitive.content }

private fun JsonObject.optionalStringList(key: String): List<String>? =
    this[key]?.jsonArray?.map { element -> element.jsonPrimitive.content }

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

private fun JsonObject.requiredDouble(
    key: String,
    zoneId: String,
): Double {
    val rawValue =
        this[key]?.jsonPrimitive?.content?.toDoubleOrNull()
    requireNotNull(rawValue) {
        "$key must be a numeric value for critical-path zone '$zoneId'."
    }
    return rawValue
}
