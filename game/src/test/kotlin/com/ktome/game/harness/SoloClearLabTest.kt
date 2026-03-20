package com.ktome.game.harness

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SoloClearLabTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("soloClearLab")
    fun `solo clear lab v1 meets the fixed phase2 matrix`() {
        val harness = SoloClearLabHarness(rootDir = tempDir)
        val reports =
            listOf("vanguard", "arcanist", "rogue", "templar")
                .flatMap { professionId ->
                    SoloClearScenario.entries.map { scenario ->
                        harness.run(professionId = professionId, scenario = scenario)
                    }
                }

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "solo-clear-lab",
            payload = harness.reportsToJson(reports),
            markdown = harness.reportsToMarkdown(reports),
        )

        assertTrue(
            reports.all { it.success },
            reports.joinToString(separator = "\n") { report ->
                "${report.professionId}/${report.scenarioId}: ${report.failureReason ?: "unknown failure"}"
            },
        )
    }
}
