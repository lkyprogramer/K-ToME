package com.ktome.game.harness

import com.ktome.core.run.RunOutcome
import com.ktome.game.FOUNDATION_PROFESSION_ID
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
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
        val seeds = (20260312L..20260331L).toList()
        val reports =
            seeds.map { seed ->
                harness.run(
                    ScenarioSpec(
                        name = "long-run-$seed",
                        seed = seed,
                        professionId = FOUNDATION_PROFESSION_ID,
                        maxTurns = 3000,
                        goal = ScenarioGoal.ReachFloorOrTerminal(3),
                        assertions = listOf(ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                )
            }

        val floor3OrBetter = reports.count { it.floorReached >= 3 || (it.outcome.isTerminal && it.outcome !is RunOutcome.Defeat) }
        val crashedOrStalled = reports.filter { !it.success && (it.failureReason != "Turn budget exhausted." || it.stuckReason != null) }
        val summary =
            buildJsonObject {
                put("professionId", FOUNDATION_PROFESSION_ID)
                put("seedCount", seeds.size)
                put("floor3OrBetter", floor3OrBetter)
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
                    appendLine("- profession: $FOUNDATION_PROFESSION_ID")
                    appendLine("- seeds: ${seeds.size}")
                    appendLine("- floor3OrBetter: $floor3OrBetter")
                    reports.forEach { report ->
                        appendLine("- ${report.seed}: success=${report.success}, floor=${report.floorReached}, turns=${report.turns}, outcome=${report.outcome}")
                    }
                },
        )

        assertEquals(0, crashedOrStalled.size, crashedOrStalled.joinToString(separator = "\n") { "${it.seed}: ${it.failureReason ?: it.stuckReason}" })
        assertTrue(floor3OrBetter * 100 >= seeds.size * 70, "Expected at least 70% of runs to reach floor 3 or terminal, actual=$floor3OrBetter/${seeds.size}")
    }
}
