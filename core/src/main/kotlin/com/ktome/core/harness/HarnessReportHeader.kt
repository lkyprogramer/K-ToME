package com.ktome.core.harness

import com.ktome.core.phase.PackId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Serializable
data class HarnessReportHeader(
    val harnessId: String,
    val phaseId: String,
    val buildId: String,
    val locale: String,
    val contentSchemaVersion: Int,
    val topologyFingerprintVersion: Int,
    val rewardLedgerVersion: Int,
    val lootFormulaVersion: Int,
    val specialTierEligibilityVersion: Int,
    val searchRuleVersion: Int,
    val secretRuleVersion: Int,
    val overlayContractVersion: Int,
    val activePackIds: List<PackId>,
    val activePackManifestVersions: Map<PackId, String>,
    val timestamp: String,
    val seedList: List<Long>,
)

fun HarnessReportHeader.toJson(): JsonObject =
    buildJsonObject {
        put("harnessId", harnessId)
        put("phaseId", phaseId)
        put("buildId", buildId)
        put("locale", locale)
        put("contentSchemaVersion", contentSchemaVersion)
        put("topologyFingerprintVersion", topologyFingerprintVersion)
        put("rewardLedgerVersion", rewardLedgerVersion)
        put("lootFormulaVersion", lootFormulaVersion)
        put("specialTierEligibilityVersion", specialTierEligibilityVersion)
        put("searchRuleVersion", searchRuleVersion)
        put("secretRuleVersion", secretRuleVersion)
        put("overlayContractVersion", overlayContractVersion)
        put("timestamp", timestamp)
        putJsonArray("activePackIds") { activePackIds.forEach { packId -> add(JsonPrimitive(packId.value)) } }
        putJsonObject("activePackManifestVersions") {
            activePackManifestVersions.toSortedMap(compareBy { packId -> packId.value })
                .forEach { (packId, version) -> put(packId.value, version) }
        }
        putJsonArray("seedList") { seedList.forEach { seed -> add(JsonPrimitive(seed)) } }
    }
