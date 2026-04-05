package com.ktome.tools.whitebox

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.ContractVersionStamp
import com.ktome.core.harness.whitebox.VerificationReportHeader
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class WhiteBoxReportWriterTest {
    private val json: Json = Json { prettyPrint = true }

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `writer emits standard four-part report contract`() {
        val outputDir = tempDir.resolve("whitebox")
        val joinKey = WhiteBoxJoinKey(seed = 123L, zoneId = "greenwood_fringe", floorIndex = 1)
        val artifact =
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "base-map",
                kind = "map",
                fileName = "base-map.txt",
                summary = "Raw ASCII map.",
                content = "#####\n#...#\n#####",
            )
        val request =
            WhiteBoxDomainWriteRequest(
                domainId = "mapgen",
                outputDir = outputDir,
                header =
                    VerificationReportHeader(
                        harnessId = "whiteBoxMapgen",
                        phaseId = "P4",
                        buildId = "test-build",
                        locale = "zh-CN",
                        corpusId = "P4_PR03_MAPGEN_WHITEBOX",
                        timestamp = "2026-04-04T00:00:00Z",
                        activePackIds = listOf("ktome.base"),
                        activePackManifestVersions = mapOf("ktome.base" to "1.0.0"),
                        contractVersions = listOf(ContractVersionStamp(contractId = "contentSchema", version = "4")),
                        seedList = listOf(123L),
                    ),
                corpus =
                    WhiteBoxCorpusSpec(
                        corpusId = "P4_PR03_MAPGEN_WHITEBOX",
                        description = "Test corpus.",
                        sampleCount = 1,
                    ),
                cases =
                    listOf(
                        WhiteBoxCaseReport(
                            joinKey = joinKey,
                            facts =
                                buildJsonObject {
                                    put("criticalPathReachable", true)
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
                            artifacts = listOf(artifact),
                        ),
                    ),
                aggregates =
                    listOf(
                        WhiteBoxAggregateReport(
                            groupId = "corpus",
                            sampleCount = 1,
                            metrics =
                                buildJsonObject {
                                    put("differenceCategoryCount", 3)
                                },
                            assertions =
                                listOf(
                                    WhiteBoxAssertionResult(
                                        ruleId = "mapgen.aggregate.difference_categories",
                                        passed = true,
                                        message = "Difference threshold satisfied.",
                                    ),
                                ),
                        ),
                    ),
            )

        val result = WhiteBoxReportWriter.write(request)
        val summaryPayload = json.parseToJsonElement(Files.readString(result.summaryPath)).jsonObject

        assertTrue(Files.exists(result.summaryPath))
        assertTrue(Files.exists(result.casesPath))
        assertTrue(Files.exists(result.reportPath))
        assertTrue(Files.exists(outputDir.resolve(artifact.relativePath)))
        assertEquals("mapgen", summaryPayload.getValue("domainId").jsonPrimitive.content)
        assertEquals("PASS", summaryPayload.getValue("verdict").jsonPrimitive.content)
        assertEquals("0", summaryPayload.getValue("failedCaseCount").jsonPrimitive.content)
        assertEquals("0", summaryPayload.getValue("failedAggregateCount").jsonPrimitive.content)
        assertEquals("1", summaryPayload.getValue("summary").jsonObject.getValue("caseCount").jsonPrimitive.content)
        assertEquals(1, summaryPayload.getValue("aggregates").jsonArray.size)
        assertEquals(1, result.artifactCount)
        assertEquals(0, result.failedAssertions)
        assertEquals(1, Files.readAllLines(result.casesPath).count { line -> line.isNotBlank() })
        val markdown = Files.readString(result.reportPath)
        assertTrue(markdown.contains("## Artifact Samples"))
        assertTrue(markdown.contains(artifact.relativePath))
    }

    @Test
    fun `retention policy failures plus samples keeps only failing or sampled cases`() {
        val joinKey = WhiteBoxJoinKey(seed = 123L, zoneId = "greenwood_fringe", floorIndex = 1)
        val failingAssertions =
            listOf(
                WhiteBoxAssertionResult(
                    ruleId = "mapgen.case.execution_success",
                    passed = false,
                    message = "Execution failed.",
                ),
            )

        assertTrue(
            WhiteBoxReportWriter.shouldWriteArtifacts(
                retentionPolicy = ArtifactRetentionPolicy.FAILURES_PLUS_SAMPLES,
                joinKey = joinKey,
                assertions = failingAssertions,
            ),
        )
        assertTrue(
            WhiteBoxReportWriter.shouldWriteArtifacts(
                retentionPolicy = ArtifactRetentionPolicy.FAILURES_PLUS_SAMPLES,
                joinKey = joinKey,
                assertions = emptyList(),
                sampleCaseKeys = setOf(joinKey),
            ),
        )
        assertFalse(
            WhiteBoxReportWriter.shouldWriteArtifacts(
                retentionPolicy = ArtifactRetentionPolicy.SUMMARY_ONLY,
                joinKey = joinKey,
                assertions = failingAssertions,
            ),
        )
    }
}
