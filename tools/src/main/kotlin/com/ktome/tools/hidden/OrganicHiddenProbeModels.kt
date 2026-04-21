package com.ktome.tools.hidden

import com.ktome.core.harness.HarnessReportHeader
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class OrganicHiddenProbeRun(
    val totalCases: Int,
    val runtimeFailureCount: Int,
    val summaryPath: Path,
    val eventsPath: Path,
    val markdownPath: Path,
)

internal data class OrganicHiddenProbeArtifactPaths(
    val summaryPath: Path,
    val eventsPath: Path,
    val markdownPath: Path,
)

internal data class OrganicHiddenProbeSummaryRequest(
    val results: List<OrganicHiddenProbeCaseResult>,
    val professionIds: List<String>,
    val raceIds: List<String>,
    val seedsPerZoneCombo: Int,
    val perZoneSecretEntryMinRate: Double,
)

internal data class OrganicHiddenProbeArtifactWriteRequest(
    val outputDir: Path,
    val header: HarnessReportHeader,
    val summary: OrganicHiddenProbeSummary,
    val kernelCacheMetadata: JsonObject,
    val shardEventPaths: List<Path>,
    val probeBotId: String,
    val probeTurnBudget: Int,
    val probeMaxFloor: Int,
    val json: kotlinx.serialization.json.Json,
)

internal data class OrganicHiddenProbeCaseSpec(
    val zoneId: String,
    val floorIndex: Int,
    val professionId: String,
    val raceId: String,
    val seed: Long,
)

internal data class OrganicHiddenProbeCaseResult(
    val zoneId: String,
    val floorIndex: Int,
    val professionId: String,
    val raceId: String,
    val seed: Long,
    val turnCount: Int,
    val searchAttemptCount: Int,
    val searchActionUseCount: Int,
    val searchRevealCount: Int,
    val hiddenEventIds: List<String>,
    val secretZoneIds: List<String>,
    val firstHiddenDiscoveryTurn: Int?,
    val firstSecretZoneEntryTurn: Int?,
    val lastCommands: List<String>,
    val finalZoneId: String,
    val finalFloor: Int,
    val runtimeFailure: String? = null,
) {
    val discoveredWithoutPrimer: Boolean
        get() = leadDiscovered

    val leadDiscovered: Boolean
        get() = searchRevealCount > 0 || secretZoneIds.isNotEmpty()

    val secretConverted: Boolean
        get() = searchRevealCount > 0 && secretZoneIds.isNotEmpty()

    val secretZoneEntered: Boolean
        get() = secretZoneIds.isNotEmpty()

    fun toJson(header: HarnessReportHeader): JsonObject =
        buildJsonObject {
            put("buildId", header.buildId)
            put("phaseId", header.phaseId)
            put("locale", header.locale)
            put("contentSchemaVersion", header.contentSchemaVersion)
            put("searchRuleVersion", header.searchRuleVersion)
            put("secretRuleVersion", header.secretRuleVersion)
            put("zoneId", zoneId)
            put("floorIndex", floorIndex)
            put("professionId", professionId)
            put("raceId", raceId)
            put("seed", seed)
            put("turnCount", turnCount)
            put("searchAttemptCount", searchAttemptCount)
            put("searchActionUseCount", searchActionUseCount)
            put("searchRevealCount", searchRevealCount)
            put("discoveredWithoutPrimer", discoveredWithoutPrimer)
            put("secretZoneEntered", secretZoneEntered)
            put("firstHiddenDiscoveryTurn", firstHiddenDiscoveryTurn)
            put("firstSecretZoneEntryTurn", firstSecretZoneEntryTurn)
            put("finalZoneId", finalZoneId)
            put("finalFloor", finalFloor)
            putJsonArray("hiddenEventIds") {
                hiddenEventIds.forEach { hiddenEventId -> add(JsonPrimitive(hiddenEventId)) }
            }
            putJsonArray("secretZoneIds") {
                secretZoneIds.forEach { secretZoneId -> add(JsonPrimitive(secretZoneId)) }
            }
            putJsonArray("lastCommands") {
                lastCommands.forEach { command -> add(JsonPrimitive(command)) }
            }
            runtimeFailure?.let { failure -> put("runtimeFailure", failure) }
        }
}

internal data class OrganicHiddenProbeShardSpec(
    val zoneId: String,
    val floorIndex: Int,
    val professionId: String,
    val raceId: String,
    val cases: List<OrganicHiddenProbeCaseSpec>,
) {
    val shardId: String
        get() = "$zoneId:$floorIndex:$professionId:$raceId"
}

internal data class OrganicHiddenProbeZoneMetrics(
    val caseCount: Int,
    val runsWithSearchActionCount: Int,
    val searchActionUseCount: Int,
    val discoveryWithoutPrimerCount: Int,
    val secretZoneEntryCount: Int,
    val averageFirstHiddenDiscoveryTurn: Double?,
    val averageFirstSecretZoneEntryTurn: Double?,
    val firstHiddenDiscoveryTurnP50: Int?,
    val firstHiddenDiscoveryTurnP90: Int?,
    val firstSecretZoneEntryTurnP50: Int?,
    val firstSecretZoneEntryTurnP90: Int?,
) {
    val searchActionUseRate: Double
        get() = if (caseCount == 0) 0.0 else runsWithSearchActionCount.toDouble() / caseCount.toDouble()

    val leadDiscoveryRate: Double
        get() = if (caseCount == 0) 0.0 else discoveryWithoutPrimerCount.toDouble() / caseCount.toDouble()

    val secretZoneEntryRate: Double
        get() = if (caseCount == 0) 0.0 else secretZoneEntryCount.toDouble() / caseCount.toDouble()

    val secretConversionRate: Double
        get() = if (discoveryWithoutPrimerCount == 0) 0.0 else secretZoneEntryCount.toDouble() / discoveryWithoutPrimerCount.toDouble()
}

internal data class OrganicHiddenProbeCombinationMetrics(
    val professionId: String,
    val raceId: String,
    val caseCount: Int,
    val runsWithSearchActionCount: Int,
    val searchActionUseCount: Int,
    val discoveryWithoutPrimerCount: Int,
    val secretZoneEntryCount: Int,
    val averageFirstHiddenDiscoveryTurn: Double?,
    val averageFirstSecretZoneEntryTurn: Double?,
    val firstHiddenDiscoveryTurnP50: Int?,
    val firstHiddenDiscoveryTurnP90: Int?,
    val firstSecretZoneEntryTurnP50: Int?,
    val firstSecretZoneEntryTurnP90: Int?,
) {
    val searchActionUseRate: Double
        get() = if (caseCount == 0) 0.0 else runsWithSearchActionCount.toDouble() / caseCount.toDouble()

    val leadDiscoveryRate: Double
        get() = if (caseCount == 0) 0.0 else discoveryWithoutPrimerCount.toDouble() / caseCount.toDouble()

    val secretZoneEntryRate: Double
        get() = if (caseCount == 0) 0.0 else secretZoneEntryCount.toDouble() / caseCount.toDouble()

    val secretConversionRate: Double
        get() = if (discoveryWithoutPrimerCount == 0) 0.0 else secretZoneEntryCount.toDouble() / discoveryWithoutPrimerCount.toDouble()
}

internal data class OrganicHiddenProbeSummary(
    val totalCases: Int,
    val distinctSeedCount: Int,
    val runtimeFailureCount: Int,
    val searchAttemptCount: Int,
    val runsWithSearchActionCount: Int,
    val searchActionUseCount: Int,
    val discoveryWithoutPrimerCount: Int,
    val secretZoneEntryCount: Int,
    val averageFirstHiddenDiscoveryTurn: Double?,
    val averageFirstSecretZoneEntryTurn: Double?,
    val firstHiddenDiscoveryTurnP50: Int?,
    val firstHiddenDiscoveryTurnP90: Int?,
    val firstSecretZoneEntryTurnP50: Int?,
    val firstSecretZoneEntryTurnP90: Int?,
    val professionIds: List<String>,
    val raceIds: List<String>,
    val seedsPerZoneCombo: Int,
    val zoneBreakdown: Map<String, OrganicHiddenProbeZoneMetrics>,
    val combinations: List<OrganicHiddenProbeCombinationMetrics>,
    val zoneDiscoveryDistribution: Map<String, Double>,
    val secretZoneDiscoveryDistribution: Map<String, Double>,
    val perZoneSecretEntryMinRate: Double,
    val failingSecretEntryZoneIds: List<String>,
) {
    val searchActionUseRate: Double
        get() = if (totalCases == 0) 0.0 else runsWithSearchActionCount.toDouble() / totalCases.toDouble()

    val leadDiscoveryRate: Double
        get() = if (totalCases == 0) 0.0 else discoveryWithoutPrimerCount.toDouble() / totalCases.toDouble()

    val secretZoneEntryRate: Double
        get() = if (totalCases == 0) 0.0 else secretZoneEntryCount.toDouble() / totalCases.toDouble()

    val secretConversionRate: Double
        get() = if (discoveryWithoutPrimerCount == 0) 0.0 else secretZoneEntryCount.toDouble() / discoveryWithoutPrimerCount.toDouble()
}

internal fun JsonObject.toOrganicHiddenProbeCaseResult(): OrganicHiddenProbeCaseResult =
    OrganicHiddenProbeCaseResult(
        zoneId = getValue("zoneId").jsonPrimitive.content,
        floorIndex = getValue("floorIndex").jsonPrimitive.content.toInt(),
        professionId = getValue("professionId").jsonPrimitive.content,
        raceId = getValue("raceId").jsonPrimitive.content,
        seed = getValue("seed").jsonPrimitive.content.toLong(),
        turnCount = getValue("turnCount").jsonPrimitive.content.toInt(),
        searchAttemptCount = getValue("searchAttemptCount").jsonPrimitive.content.toInt(),
        searchActionUseCount = getValue("searchActionUseCount").jsonPrimitive.content.toInt(),
        searchRevealCount = getValue("searchRevealCount").jsonPrimitive.content.toInt(),
        hiddenEventIds = getValue("hiddenEventIds").jsonArray.map { hiddenEventId -> hiddenEventId.jsonPrimitive.content },
        secretZoneIds = getValue("secretZoneIds").jsonArray.map { secretZoneId -> secretZoneId.jsonPrimitive.content },
        firstHiddenDiscoveryTurn = this["firstHiddenDiscoveryTurn"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
        firstSecretZoneEntryTurn = this["firstSecretZoneEntryTurn"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
        lastCommands = getValue("lastCommands").jsonArray.map { command -> command.jsonPrimitive.content },
        finalZoneId = getValue("finalZoneId").jsonPrimitive.content,
        finalFloor = getValue("finalFloor").jsonPrimitive.content.toInt(),
        runtimeFailure = this["runtimeFailure"]?.jsonPrimitive?.contentOrNull,
    )
