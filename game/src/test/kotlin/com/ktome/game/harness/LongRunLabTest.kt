package com.ktome.game.harness

import com.ktome.core.run.RunOutcome
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class LongRunLabTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    fun `nightly long run lab meets first pass thresholds`() {
        val harness = HeadlessRunHarness(rootDir = tempDir)
        val reports =
            listOf(
                "vanguard" to 20260312L,
                "arcanist" to 20260313L,
            ).map { (professionId, seed) ->
                harness.run(
                    ScenarioSpec(
                        name = "long-run-$professionId-$seed",
                        seed = seed,
                        professionId = professionId,
                        maxTurns = 900,
                        goal = ScenarioGoal.Victory,
                        assertions = listOf(ScenarioAssertion.Victory, ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                )
            }

        val victories = reports.count { it.outcome is RunOutcome.Victory }
        val crashedOrStalled = reports.filter { it.crashedOrStalled() || it.outcome is RunOutcome.Defeat }
        val summary =
            buildJsonObject {
                put("sliceId", "pr06-minimal-official-slice")
                put("seedCount", reports.size)
                put("victories", victories)
                putJsonArray("reports") {
                    reports.forEach { add(it.toJson()) }
                }
            }

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "long-run-summary",
            payload = summary,
            markdown =
                buildString {
                    appendLine("# Long Run Lab")
                    appendLine("- sliceId: pr06-minimal-official-slice")
                    appendLine("- seeds: ${reports.size}")
                    appendLine("- victories: $victories")
                    reports.forEach { report ->
                        appendLine("- profession=${report.professionId}, seed=${report.seed}, success=${report.success}, floor=${report.floorReached}, turns=${report.turns}, outcome=${report.outcome}")
                    }
                },
        )

        assertTrue(
            crashedOrStalled.isEmpty(),
            crashedOrStalled.joinToString(separator = "\n") { report ->
                "${report.professionId}/${report.seed}: ${report.failureReason ?: report.stuckReason ?: report.outcome}"
            },
        )
        assertTrue(victories == reports.size, "Expected all official-slice long-run probes to end in victory, actual=$victories/${reports.size}")
    }
}
