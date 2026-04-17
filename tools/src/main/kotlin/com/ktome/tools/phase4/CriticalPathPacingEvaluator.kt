package com.ktome.tools.phase4

import com.ktome.tools.verification.BaselineMode
import com.ktome.tools.verification.EvaluationEntry
import com.ktome.tools.verification.EvaluationEntryStatus
import com.ktome.tools.verification.EvaluationResult
import com.ktome.tools.verification.EvaluationVerdict
import com.ktome.tools.verification.VerificationBaseline
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal enum class CriticalPathPacingMetricKind(
    val serializedValue: String,
) {
    MINIMUM(serializedValue = "minimum"),
    RATIO(serializedValue = "ratio"),
}

internal data class CriticalPathPacingThresholds(
    val objectiveFloor: Double,
    val visibleFloor: Double,
    val enemyFloor: Double,
    val satisfiedFloor: Double,
    val targetTextByMetricId: Map<String, String> = emptyMap(),
) {
    companion object {
        fun fromBaseline(baseline: VerificationBaseline): CriticalPathPacingThresholds {
            val objectiveMetricId = "avgObjectiveAcquireTurn"
            val visibleMetricId = "avgVisibleHostileTurnCount"
            val enemyMetricId = "avgEnemyTurns"
            val satisfiedMetricId = "criticalPathCombatFloorSatisfied"
            return CriticalPathPacingThresholds(
                objectiveFloor =
                    checkNotNull(baseline.requiredMetric(objectiveMetricId).minimumAcceptedValue()) {
                        "Critical-path pacing baseline must define minValue for $objectiveMetricId."
                    },
                visibleFloor =
                    checkNotNull(baseline.requiredMetric(visibleMetricId).minimumAcceptedValue()) {
                        "Critical-path pacing baseline must define minValue for $visibleMetricId."
                    },
                enemyFloor =
                    checkNotNull(baseline.requiredMetric(enemyMetricId).minimumAcceptedValue()) {
                        "Critical-path pacing baseline must define minValue for $enemyMetricId."
                    },
                satisfiedFloor =
                    checkNotNull(baseline.requiredMetric(satisfiedMetricId).minimumAcceptedValue()) {
                        "Critical-path pacing baseline must define minValue for $satisfiedMetricId."
                    },
                targetTextByMetricId =
                    mapOf(
                        objectiveMetricId to Phase4OwnerMetricTargets.targetText(objectiveMetricId, baseline.requiredMetric(objectiveMetricId)),
                        visibleMetricId to Phase4OwnerMetricTargets.targetText(visibleMetricId, baseline.requiredMetric(visibleMetricId)),
                        enemyMetricId to Phase4OwnerMetricTargets.targetText(enemyMetricId, baseline.requiredMetric(enemyMetricId)),
                        satisfiedMetricId to Phase4OwnerMetricTargets.targetText(satisfiedMetricId, baseline.requiredMetric(satisfiedMetricId)),
                    ),
            )
        }
    }
}

internal data class CriticalPathPacingEvidence(
    val criticalPathZoneIds: List<String>,
    val zoneSnapshots: List<CriticalPathZonePacingSnapshot>,
    val zoneBreakdown: JsonObject,
    val designAudit: List<CriticalPathZoneDesignAuditSnapshot>,
    val sampleMissingZoneIds: List<String>,
) {
    fun toSectionJson(): JsonObject =
        buildJsonObject {
            put("criticalPathZoneIds", criticalPathZoneIds.toJsonArray())
            put("zoneBreakdown", zoneBreakdown)
            put("sampleMissingZoneIds", sampleMissingZoneIds.toJsonArray())
            put(
                "zoneSnapshots",
                kotlinx.serialization.json.buildJsonArray {
                    zoneSnapshots.forEach { snapshot ->
                        add(
                            buildJsonObject {
                                put("zoneId", snapshot.zoneId)
                                snapshot.avgObjectiveAcquireTurn?.let { value -> put("avgObjectiveAcquireTurn", value) }
                                put("avgVisibleHostileTurnCount", snapshot.avgVisibleHostileTurnCount)
                                put("avgEnemyTurns", snapshot.avgEnemyTurns)
                            },
                        )
                    }
                },
            )
            put(
                "designAudit",
                kotlinx.serialization.json.buildJsonArray {
                    designAudit.forEach { audit -> add(audit.toJson()) }
                },
            )
        }
}

internal data class CriticalPathPacingMetricResult(
    val metricId: String,
    val metricKind: CriticalPathPacingMetricKind,
    val status: EvaluationEntryStatus,
    val currentValue: JsonElement,
    val currentValueText: String,
    val zoneFailures: List<String>,
    val sampleMissing: Boolean,
) {
    fun details(): JsonObject =
        buildJsonObject {
            put("sectionRef", "criticalPathPacing")
            put("metricKind", metricKind.serializedValue)
            put("zoneFailures", zoneFailures.toJsonArray())
            put("sampleMissing", sampleMissing)
        }
}

internal data class CriticalPathPacingLegacyMetricProjection(
    val metricId: String,
    val sourceTaskId: String,
    val currentValue: JsonElement,
    val currentValueText: String,
    val target: String,
    val status: String,
    val note: String,
)

internal data class CriticalPathPacingEvaluation(
    val entriesByMetricId: Map<String, CriticalPathPacingMetricResult>,
    val evidence: CriticalPathPacingEvidence,
    val thresholds: CriticalPathPacingThresholds,
    val verdict: EvaluationVerdict,
    val passCount: Int,
    val unexpectedRegressionCount: Int,
) {
    val note: String =
        "criticalPathZoneIds=${evidence.criticalPathZoneIds.joinToString()}, " +
            "objectiveFloor=${formatRequiredDecimal(thresholds.objectiveFloor)}, " +
            "visibleFloor=${formatRequiredDecimal(thresholds.visibleFloor)}, " +
            "enemyFloor=${formatRequiredDecimal(thresholds.enemyFloor)}"

    fun toEvaluationResult(
        domainId: String,
        evaluationId: String,
    ): EvaluationResult =
        EvaluationResult(
            evaluationId = evaluationId,
            domainId = domainId,
            mode = BaselineMode.BUDGET_THRESHOLD,
            verdict = verdict,
            passCount = passCount,
            approvedDebtCount = 0,
            expectedFailureCount = 0,
            unexpectedRegressionCount = unexpectedRegressionCount,
            improvedDebtCount = 0,
            entries =
                orderedMetricIds.map { metricId ->
                    val result = entriesByMetricId.getValue(metricId)
                    EvaluationEntry(
                        metricId = metricId,
                        status = result.status,
                        currentValue = result.currentValue,
                        currentValueText = result.currentValueText,
                        targetText = legacyTargetText(metricId),
                        note = note,
                        details = result.details(),
                    )
                },
        )

    fun toExperienceMetrics(taskId: String): List<CriticalPathPacingLegacyMetricProjection> =
        orderedMetricIds.map { metricId ->
            val result = entriesByMetricId.getValue(metricId)
            CriticalPathPacingLegacyMetricProjection(
                metricId = metricId,
                sourceTaskId = taskId,
                currentValue = result.currentValue,
                currentValueText = result.currentValueText,
                target = legacyTargetText(metricId),
                status =
                    if (result.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION) {
                        "FAIL"
                    } else {
                        "PASS"
                    },
                note = note,
            )
        }

    companion object {
        private val orderedMetricIds: List<String> =
            listOf(
                "avgObjectiveAcquireTurn",
                "avgVisibleHostileTurnCount",
                "avgEnemyTurns",
                "criticalPathCombatFloorSatisfied",
            )
    }

    private fun legacyTargetText(metricId: String): String =
        checkNotNull(thresholds.targetTextByMetricId[metricId]) {
            "CriticalPathPacingThresholds.targetTextByMetricId missing target text for '$metricId'."
        }

    private fun targetFloor(metricId: String): Double =
        when (metricId) {
            "avgObjectiveAcquireTurn" -> thresholds.objectiveFloor
            "avgVisibleHostileTurnCount" -> thresholds.visibleFloor
            "avgEnemyTurns" -> thresholds.enemyFloor
            else -> error("Unsupported critical-path pacing floor metric '$metricId'.")
        }
}

internal object CriticalPathPacingEvaluator {
    fun evaluate(
        longRunMetrics: JsonObject,
        thresholds: CriticalPathPacingThresholds,
    ): CriticalPathPacingEvaluation {
        val pacingSummary = longRunMetrics.toCriticalPathPacingSummary()
        val objectiveFailures = pacingSummary.failingObjectiveZones(thresholds.objectiveFloor)
        val visibleFailures = pacingSummary.failingVisibleHostileZones(thresholds.visibleFloor)
        val enemyFailures = pacingSummary.failingEnemyTurnZones(thresholds.enemyFloor)
        val satisfiedZoneIds =
            pacingSummary.satisfiedZoneIds(
                objectiveAcquireFloor = thresholds.objectiveFloor,
                visibleHostileFloor = thresholds.visibleFloor,
                enemyTurnFloor = thresholds.enemyFloor,
            )
        val satisfiedFailures = pacingSummary.zoneIds.filterNot(satisfiedZoneIds::contains)
        val evidence =
            CriticalPathPacingEvidence(
                criticalPathZoneIds = pacingSummary.zoneIds,
                zoneSnapshots = pacingSummary.zoneSnapshotsInOrder(),
                zoneBreakdown =
                    pacingSummary.perZoneBreakdownJson(
                        objectiveAcquireFloor = thresholds.objectiveFloor,
                        visibleHostileFloor = thresholds.visibleFloor,
                        enemyTurnFloor = thresholds.enemyFloor,
                    ),
                designAudit = longRunMetrics.toCriticalPathDesignAuditSnapshots(pacingSummary.zoneIds),
                sampleMissingZoneIds =
                    pacingSummary.zoneSnapshotsInOrder()
                        .filter { snapshot -> snapshot.avgObjectiveAcquireTurn == null }
                        .map(CriticalPathZonePacingSnapshot::zoneId),
            )
        val objectiveMinimum = pacingSummary.minimumObjectiveAcquireTurn()
        val visibleMinimum = pacingSummary.minimumVisibleHostileTurnCount()
        val enemyMinimum = pacingSummary.minimumEnemyTurns()
        val satisfiedRatio =
            pacingSummary.satisfiedRatio(
                objectiveAcquireFloor = thresholds.objectiveFloor,
                visibleHostileFloor = thresholds.visibleFloor,
                enemyTurnFloor = thresholds.enemyFloor,
            )

        val entries =
            linkedMapOf(
                "avgObjectiveAcquireTurn" to
                    CriticalPathPacingMetricResult(
                        metricId = "avgObjectiveAcquireTurn",
                        metricKind = CriticalPathPacingMetricKind.MINIMUM,
                        status =
                            if (objectiveFailures.isEmpty()) {
                                EvaluationEntryStatus.PASS
                            } else {
                                EvaluationEntryStatus.UNEXPECTED_REGRESSION
                            },
                        currentValue =
                            buildJsonObject {
                                objectiveMinimum?.let { value -> put("minimum", value) }
                                put("target", thresholds.objectiveFloor)
                                put("criticalPathZoneIds", pacingSummary.zoneIds.toJsonArray())
                                put("failingZones", objectiveFailures.toJsonArray())
                            },
                        currentValueText =
                            if (objectiveFailures.isEmpty()) {
                                "min=${formatDecimal(objectiveMinimum)} all critical-path zones >= ${formatDecimal(thresholds.objectiveFloor)}"
                            } else {
                                "min=${formatDecimal(objectiveMinimum)} failed=${objectiveFailures.joinToString()} target>=${formatDecimal(thresholds.objectiveFloor)}"
                            },
                        zoneFailures = objectiveFailures,
                        sampleMissing = evidence.sampleMissingZoneIds.isNotEmpty(),
                    ),
                "avgVisibleHostileTurnCount" to
                    CriticalPathPacingMetricResult(
                        metricId = "avgVisibleHostileTurnCount",
                        metricKind = CriticalPathPacingMetricKind.MINIMUM,
                        status =
                            if (visibleFailures.isEmpty()) {
                                EvaluationEntryStatus.PASS
                            } else {
                                EvaluationEntryStatus.UNEXPECTED_REGRESSION
                            },
                        currentValue =
                            buildJsonObject {
                                put("minimum", visibleMinimum)
                                put("target", thresholds.visibleFloor)
                                put("criticalPathZoneIds", pacingSummary.zoneIds.toJsonArray())
                                put("failingZones", visibleFailures.toJsonArray())
                            },
                        currentValueText =
                            if (visibleFailures.isEmpty()) {
                                "min=${formatDecimal(visibleMinimum)} all critical-path zones >= ${formatDecimal(thresholds.visibleFloor)}"
                            } else {
                                "min=${formatDecimal(visibleMinimum)} failed=${visibleFailures.joinToString()} target>=${formatDecimal(thresholds.visibleFloor)}"
                            },
                        zoneFailures = visibleFailures,
                        sampleMissing = false,
                    ),
                "avgEnemyTurns" to
                    CriticalPathPacingMetricResult(
                        metricId = "avgEnemyTurns",
                        metricKind = CriticalPathPacingMetricKind.MINIMUM,
                        status =
                            if (enemyFailures.isEmpty()) {
                                EvaluationEntryStatus.PASS
                            } else {
                                EvaluationEntryStatus.UNEXPECTED_REGRESSION
                            },
                        currentValue =
                            buildJsonObject {
                                put("minimum", enemyMinimum)
                                put("target", thresholds.enemyFloor)
                                put("criticalPathZoneIds", pacingSummary.zoneIds.toJsonArray())
                                put("failingZones", enemyFailures.toJsonArray())
                            },
                        currentValueText =
                            if (enemyFailures.isEmpty()) {
                                "min=${formatDecimal(enemyMinimum)} all critical-path zones >= ${formatDecimal(thresholds.enemyFloor)}"
                            } else {
                                "min=${formatDecimal(enemyMinimum)} failed=${enemyFailures.joinToString()} target>=${formatDecimal(thresholds.enemyFloor)}"
                            },
                        zoneFailures = enemyFailures,
                        sampleMissing = false,
                    ),
                "criticalPathCombatFloorSatisfied" to
                    CriticalPathPacingMetricResult(
                        metricId = "criticalPathCombatFloorSatisfied",
                        metricKind = CriticalPathPacingMetricKind.RATIO,
                        status =
                            if (satisfiedRatio >= thresholds.satisfiedFloor) {
                                EvaluationEntryStatus.PASS
                            } else {
                                EvaluationEntryStatus.UNEXPECTED_REGRESSION
                            },
                        currentValue =
                            buildJsonObject {
                                put("rate", satisfiedRatio)
                                put("satisfiedZoneCount", satisfiedZoneIds.size)
                                put("criticalPathZoneCount", pacingSummary.zoneIds.size)
                                put("criticalPathZoneIds", pacingSummary.zoneIds.toJsonArray())
                                put("failingZones", satisfiedFailures.toJsonArray())
                                put("zoneBreakdown", evidence.zoneBreakdown)
                            },
                        currentValueText =
                            "${formatPercent(satisfiedRatio)} (${satisfiedZoneIds.size}/${pacingSummary.zoneIds.size}), " +
                                "failed=${satisfiedFailures.joinToString().ifBlank { "none" }}",
                        zoneFailures = satisfiedFailures,
                        sampleMissing = evidence.sampleMissingZoneIds.isNotEmpty(),
                    ),
            )
        val unexpectedRegressionCount = entries.values.count { result -> result.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }
        return CriticalPathPacingEvaluation(
            entriesByMetricId = entries,
            evidence = evidence,
            thresholds = thresholds,
            verdict = if (unexpectedRegressionCount > 0) EvaluationVerdict.FAIL else EvaluationVerdict.PASS,
            passCount = entries.size - unexpectedRegressionCount,
            unexpectedRegressionCount = unexpectedRegressionCount,
        )
    }
}

private fun formatPercent(value: Double): String = String.format(java.util.Locale.US, "%.1f%%", value * 100.0)

private fun formatDecimal(value: Double?): String = value?.let(::formatRequiredDecimal) ?: "n/a"

private fun formatRequiredDecimal(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)
