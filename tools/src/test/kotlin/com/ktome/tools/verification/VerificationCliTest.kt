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
import org.junit.jupiter.api.Assertions.assertNotNull
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

    @Test
    fun `legacy adapter fails the task when selected junit classes fail`() {
        val outputDir = tempDir.resolve("legacy-failing")

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VerificationCli.main(
                    arrayOf(
                        "legacy-adapter",
                        "--domain",
                        "demo",
                        "--tier",
                        VerificationTier.PREFLIGHT.name,
                        "--node-id",
                        "demo.failingNode",
                        "--snapshot",
                        "snapshot-failing",
                        "--output-dir",
                        outputDir.toString(),
                        "--cache-status",
                        "LOCAL_EXECUTION",
                        "--select-class",
                        VerificationFailingProbeFixture::class.java.name,
                    ),
                )
            }

        val summary =
            json.decodeFromString<VerificationSummary>(
                outputDir.resolve("summary.json").readText(),
            )

        assertNotNull(exception.message)
        assertTrue(exception.message!!.contains("failing tests"))
        assertEquals("FAIL", summary.verdict)
        assertEquals(1, summary.failedTests)
    }

    @Test
    fun `report mode fails when source artifact captured failing junit classes`() {
        val sourceDir =
            writeReportSourceArtifact(
                tempDir.resolve("source-failing").createDirectories(),
                nodeId = "contractLint.staticGraph",
                failedTests = 1,
                verdict = "FAIL",
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
                        "--output-dir",
                        tempDir.resolve("report-failing").toString(),
                        "--cache-status",
                        "REPORT_ONLY_REBUILD",
                        "--artifact-input",
                        sourceDir.toString(),
                    ),
                )
            }

        assertTrue(exception.message!!.contains("failing tests"))
    }

    @Test
    fun `plan changed writes task list and impact summary`() {
        val outputDir = tempDir.resolve("changed-plan")

        VerificationCli.main(
            arrayOf(
                "plan-changed",
                "--output-dir",
                outputDir.toString(),
                "--changed-file",
                "game/src/main/resources/data/loot/index.yaml",
                "--changed-file",
                "game/src/main/kotlin/com/ktome/game/data/DataLoader.kt",
            ),
        )

        val plan =
            json.decodeFromString<VerificationImpactPlan>(
                outputDir.resolve("verify-changed-plan.json").readText(),
            )
        val taskPaths = outputDir.resolve("task-paths.txt").readText().lineSequence().filter(String::isNotBlank).toSet()
        val preflightTaskPaths =
            outputDir.resolve("preflight-task-paths.txt").readText().lineSequence().filter(String::isNotBlank).toSet()

        assertTrue(plan.impactedDomains.any { impact -> impact.domainId == "loot" })
        assertTrue(plan.impactedDomains.any { impact -> impact.domainId == "hidden" })
        assertTrue(taskPaths.contains(":tools:verifyLootPreflight"))
        assertTrue(taskPaths.contains(":tools:lootBalanceLab"))
        assertTrue(preflightTaskPaths.contains(":tools:verifyLootPreflight"))
        assertTrue(preflightTaskPaths.contains(":tools:verifyHiddenPreflight"))
        assertFalse(preflightTaskPaths.contains(":tools:lootBalanceLab"))
        assertFalse(taskPaths.contains(":tools:phase4ReportOnly"))
        assertFalse(taskPaths.contains(":tools:phase4LegacyReport"))
        assertFalse(taskPaths.contains(":tools:phase4LegacyReportOnly"))
    }

    private fun writeReportSourceArtifact(
        directory: Path,
        nodeId: String,
        failedTests: Int = 0,
        verdict: String = if (failedTests == 0) "PASS" else "FAIL",
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
                    failedTests = failedTests,
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
                    verdict = verdict,
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
                    failedTests = failedTests,
                    durationMillis = 15,
                    reportOnly = false,
                ),
            ),
        )
        return directory
    }
}
