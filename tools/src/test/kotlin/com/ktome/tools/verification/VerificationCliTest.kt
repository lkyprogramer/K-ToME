package com.ktome.tools.verification

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class VerificationCliTest {
    private val json: Json =
        Json {
            prettyPrint = true
            explicitNulls = false
        }

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `report mode rebuilds summary from existing raw result without copying producer payload`() {
        val sourceDir =
            writeReportSourceArtifact(
                tempDir.resolve("source").createDirectories(),
                nodeId = "contractLint.staticGraph",
            )
        val outputDir = tempDir.resolve("report")

        VerificationCli.main(
            arrayOf(
                "report",
                "--domain",
                "contractLint",
                "--tier",
                VerificationTier.PREFLIGHT.name,
                "--output-dir",
                outputDir.toString(),
                "--cache-status",
                "REPORT_ONLY_REBUILD",
                "--artifact-input",
                sourceDir.toString(),
            ),
        )

        val summary =
            json.decodeFromString<VerificationSummary>(
                outputDir.resolve("summary.json").readText(),
            )
        val metadata =
            json.decodeFromString<VerificationMetadata>(
                outputDir.resolve("metadata.json").readText(),
            )

        assertEquals("contractLint", summary.domainId)
        assertEquals("PASS", summary.verdict)
        assertEquals("source-snapshot", summary.snapshotHash)
        assertEquals("REPORT_ONLY_REBUILD", summary.cacheStatus)
        assertEquals(true, summary.reportOnly)
        assertEquals(sourceDir.toString(), metadata.sourceArtifactDir)
        assertFalse(outputDir.resolve("raw-result.json").exists())
        assertFalse(outputDir.resolve("metadata.json").readText().contains("generatedAt"))
    }

    @Test
    fun `report mode rejects multiple artifact inputs`() {
        val sourceA =
            writeReportSourceArtifact(
                tempDir.resolve("source-a").createDirectories(),
                nodeId = "contractLint.staticGraph",
            )
        val sourceB =
            writeReportSourceArtifact(
                tempDir.resolve("source-b").createDirectories(),
                nodeId = "contractLint.staticGraph",
            )

        val exception =
            assertThrows(IllegalStateException::class.java) {
                VerificationCli.main(
                    arrayOf(
                        "report",
                        "--domain",
                        "contractLint",
                        "--tier",
                        VerificationTier.PREFLIGHT.name,
                        "--output-dir",
                        tempDir.resolve("report-multi").toString(),
                        "--cache-status",
                        "REPORT_ONLY_REBUILD",
                        "--artifact-input",
                        sourceA.toString(),
                        "--artifact-input",
                        sourceB.toString(),
                    ),
                )
            }

        assertTrue(exception.message!!.contains("exactly one --artifact-input"))
    }

    @Test
    fun `report mode rejects requested node id mismatch`() {
        val sourceDir =
            writeReportSourceArtifact(
                tempDir.resolve("source-mismatch").createDirectories(),
                nodeId = "contractLint.staticGraph",
            )

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VerificationCli.main(
                    arrayOf(
                        "report",
                        "--domain",
                        "contractLint",
                        "--tier",
                        VerificationTier.PREFLIGHT.name,
                        "--node-id",
                        "contractLint.otherNode",
                        "--output-dir",
                        tempDir.resolve("report-mismatch").toString(),
                        "--cache-status",
                        "REPORT_ONLY_REBUILD",
                        "--artifact-input",
                        sourceDir.toString(),
                    ),
                )
            }

        assertTrue(exception.message!!.contains("requested node mismatch"))
    }

    @Test
    fun `legacy adapter accepts explicit node id`() {
        val outputDir = tempDir.resolve("legacy")

        VerificationCli.main(
            arrayOf(
                "legacy-adapter",
                "--domain",
                "demo",
                "--tier",
                VerificationTier.PREFLIGHT.name,
                "--node-id",
                "demo.customNode",
                "--snapshot",
                "snapshot-demo",
                "--output-dir",
                outputDir.toString(),
                "--cache-status",
                "LOCAL_EXECUTION",
                "--select-class",
                VerificationDemoProbeTest::class.java.name,
            ),
        )

        val summary =
            json.decodeFromString<VerificationSummary>(
                outputDir.resolve("summary.json").readText(),
            )

        assertEquals("demo.customNode", summary.nodeId)
        assertEquals("PASS", summary.verdict)
    }

    private fun writeReportSourceArtifact(
        directory: Path,
        nodeId: String,
    ): Path {
        directory.resolve("raw-result.json").writeText(
            json.encodeToString(
                LegacyJUnitRawResult(
                    domainId = "contractLint",
                    tier = VerificationTier.PREFLIGHT.name,
                    nodeId = nodeId,
                    selectedClasses = listOf("com.ktome.tools.lint.ContractLintTest"),
                    selectedTags = emptyList(),
                    totalTests = 3,
                    failedTests = 0,
                    durationMillis = 15,
                    tests =
                        listOf(
                            VerificationTestCaseResult(
                                uniqueId = "[engine:junit-jupiter]/[class:demo]/[method:test]",
                                displayName = "test",
                                status = "SUCCESSFUL",
                            ),
                        ),
                ),
            ),
        )
        directory.resolve("summary.json").writeText(
            json.encodeToString(
                VerificationSummary(
                    domainId = "contractLint",
                    tier = VerificationTier.PREFLIGHT.name,
                    verdict = "PASS",
                    snapshotHash = "source-snapshot",
                    cacheStatus = "LOCAL_EXECUTION",
                    outputPaths =
                        mapOf(
                            "rawResult" to directory.resolve("raw-result.json").toString(),
                            "summary" to directory.resolve("summary.json").toString(),
                            "metadata" to directory.resolve("metadata.json").toString(),
                        ),
                    nodeId = nodeId,
                    totalTests = 3,
                    failedTests = 0,
                    durationMillis = 15,
                    reportOnly = false,
                ),
            ),
        )
        return directory
    }
}
