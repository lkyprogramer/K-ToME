package com.ktome.game.harness

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SoloClearLabV2Test {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("soloClearLab")
    fun `solo clear lab v2 meets the fixed phase3 matrix`() {
        val harness = SoloClearLabHarness(rootDir = tempDir)
        val reports =
            SOLO_CLEAR_PROFESSIONS
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

        val bossReports = reports.filter { it.scenarioId == SoloClearScenario.BOSS.name.lowercase() }
        assertTrue(
            bossReports.all { it.sawBossWarning },
            bossReports.joinToString(separator = "\n") { report ->
                "${report.professionId}/${report.scenarioId}: warning=${report.sawBossWarning}, telegraph=${report.sawTalentTelegraph}"
            },
        )
        assertTrue(
            reports.all { it.success },
            reports.joinToString(separator = "\n") { report ->
                "${report.professionId}/${report.scenarioId}: ${report.failureReason ?: "unknown failure"}"
            },
        )
    }
}
