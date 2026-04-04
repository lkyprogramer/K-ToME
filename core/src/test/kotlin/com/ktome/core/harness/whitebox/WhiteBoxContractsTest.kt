package com.ktome.core.harness.whitebox

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WhiteBoxContractsTest {
    private val json: Json = Json { prettyPrint = true }

    @Test
    fun `verification report header serializes stable corpus metadata`() {
        val header =
            VerificationReportHeader(
                harnessId = "whiteBoxMapgen",
                phaseId = "P4",
                buildId = "test-build",
                locale = "zh-CN",
                corpusId = "P4_PR03_MAPGEN_WHITEBOX",
                timestamp = "2026-04-04T00:00:00Z",
                activePackIds = listOf("ktome.base"),
                activePackManifestVersions = mapOf("ktome.base" to "1.0.0"),
                contractVersions =
                    listOf(
                        ContractVersionStamp(contractId = "contentSchema", version = "4"),
                        ContractVersionStamp(contractId = "searchRule", version = "2"),
                    ),
                seedList = listOf(101L, 202L),
            )

        val payload = json.encodeToJsonElement(VerificationReportHeader.serializer(), header).jsonObject

        assertEquals("whiteBoxMapgen", payload.getValue("harnessId").jsonPrimitive.content)
        assertEquals("P4_PR03_MAPGEN_WHITEBOX", payload.getValue("corpusId").jsonPrimitive.content)
        assertEquals(2, payload.getValue("contractVersions").jsonArray.size)
        assertEquals("101", payload.getValue("seedList").jsonArray.first().jsonPrimitive.content)
    }

    @Test
    fun `white-box case report keeps join key facts fingerprints and artifacts`() {
        val report =
            WhiteBoxCaseReport(
                joinKey = WhiteBoxJoinKey(seed = 123L, zoneId = "greenwood_fringe", floorIndex = 1),
                facts =
                    buildJsonObject {
                        put("criticalPathReachable", true)
                        put("patternRoomCount", 1)
                    },
                fingerprints = mapOf("topology" to "fingerprint-1"),
                assertions =
                    listOf(
                        WhiteBoxAssertionResult(
                            ruleId = "mapgen.case.primary_path_reachable",
                            passed = true,
                            message = "Critical path remains reachable.",
                        ),
                    ),
                artifacts =
                    listOf(
                        WhiteBoxArtifact(
                            artifactId = "base-map",
                            kind = "map",
                            format = "txt",
                            relativePath = "artifacts/zone-greenwood_fringe__floor-1__seed-123/base-map.txt",
                            summary = "Raw ASCII map.",
                        ),
                    ),
            )

        val payload = json.encodeToJsonElement(WhiteBoxCaseReport.serializer(), report).jsonObject

        assertEquals("greenwood_fringe", payload.getValue("joinKey").jsonObject.getValue("zoneId").jsonPrimitive.content)
        assertEquals("fingerprint-1", payload.getValue("fingerprints").jsonObject.getValue("topology").jsonPrimitive.content)
        assertTrue(payload.getValue("artifacts").jsonArray.isNotEmpty())
        assertEquals("base-map", payload.getValue("artifacts").jsonArray.first().jsonObject.getValue("artifactId").jsonPrimitive.content)
    }
}
