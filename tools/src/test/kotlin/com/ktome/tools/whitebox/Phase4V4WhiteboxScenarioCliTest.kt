package com.ktome.tools.whitebox

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class Phase4V4WhiteboxScenarioCliTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `missing scenario fails fast with legal ids`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                Phase4V4WhiteboxScenarioCli.run(baseConfig(scenarioId = null))
            }

        assertTrue(exception.message?.contains("Missing -Pktome.whitebox.scenario") == true)
        assertTrue(exception.message?.contains("phase4-v4-pr00-selftest") == true)
        assertTrue(exception.message?.contains("phase4-v4-pr01") == true)
        assertTrue(exception.message?.contains("phase4-v4-pr02") == true)
        assertTrue(exception.message?.contains("phase4-v4-pr03") == true)
        assertTrue(exception.message?.contains("phase4-v4-pr04") == true)
    }

    @Test
    fun `valid scenario generates launch script runbook template evidence plan and executable hash`() {
        val result =
            Phase4V4WhiteboxScenarioCli.run(
                baseConfig(scenarioId = "phase4-v4-pr00-selftest"),
            )
        val paths = result.paths

        assertTrue(Files.isRegularFile(paths.launchScript))
        assertTrue(Files.isRegularFile(paths.runbook))
        assertTrue(Files.isRegularFile(paths.manualRecordTemplate))
        assertTrue(Files.isRegularFile(paths.expectedEvidence))
        assertTrue(Files.isRegularFile(paths.appExecutableSha256))
        assertTrue(Files.isDirectory(paths.runtimeHome))
        assertTrue(Files.isDirectory(paths.evidenceDir))

        val launchScript = paths.launchScript.readText()
        assertTrue(launchScript.contains("JAVA_TOOL_OPTIONS=\"-Duser.home=build/whitebox/phase4-v4-pr00-selftest/runtime-home"))
        assertTrue(launchScript.contains("-Dktome.validation.scenario=phase4-v4-pr00-selftest"))
        assertTrue(launchScript.contains("-Dktome.whitebox.root=build/whitebox/phase4-v4-pr00-selftest"))
        assertTrue(launchScript.contains("-Dktome.whitebox.evidenceDir=build/whitebox/phase4-v4-pr00-selftest/evidence"))
        assertTrue(launchScript.contains("-Dktome.whitebox.manualRecord=docs/review/phase4/v4-pr/manual-records/phase4-v4-pr00-selftest.md"))
        assertTrue(launchScript.contains("APP_EXECUTABLE_SHA256=\"${'$'}REPO_ROOT/build/whitebox/phase4-v4-pr00-selftest/app-executable.sha256\""))
        assertTrue(launchScript.contains("APP_BUNDLE=\"${'$'}REPO_ROOT/client/build/release/K-ToME.app\""))
        assertTrue(launchScript.contains("EXPECTED_HASH=\"$(awk '{print ${'$'}1}' \"${'$'}APP_EXECUTABLE_SHA256\")\""))
        assertTrue(launchScript.contains("BEFORE_PIDS=\"$(pgrep -f \"${'$'}APP_EXECUTABLE\" || true)\""))
        assertTrue(launchScript.contains("-Dktome.whitebox.appHash=${'$'}EXPECTED_HASH"))
        assertTrue(launchScript.contains("env JAVA_TOOL_OPTIONS=\"${'$'}JAVA_TOOL_OPTIONS\" open -n \"${'$'}APP_BUNDLE\""))
        assertTrue(launchScript.contains("CANDIDATE_PIDS=\"$(pgrep -f \"${'$'}APP_EXECUTABLE\" || true)\""))
        assertTrue(launchScript.contains("printf '%s\\n' \"${'$'}APP_PID\" > \"build/whitebox/phase4-v4-pr00-selftest/evidence/app.pid\""))
        assertTrue(launchScript.contains("APP_HASH_MISMATCH"))
        assertTrue(launchScript.contains("APP_LAUNCH_FAILED"))
        assertFalseMachinePath(launchScript)

        val runbook = paths.runbook.readText()
        assertTrue(runbook.contains("## 1. Scenario summary"))
        assertTrue(runbook.contains("## 9. Failure retention"))
        assertTrue(runbook.contains("| Step | Mode | Input | Expected visible result | Evidence file |"))
        assertTrue(runbook.contains("| 5 | Keyboard | Right, Enter | Evidence summary shows expected paths, freshness, and app hash | `evidence/phase4-v4-pr00-evidence-summary.png` |"))
        assertTrue(runbook.contains("docs/review/phase4/v4-pr/manual-records/phase4-v4-pr00-selftest.md"))
        assertFalseMachinePath(runbook)

        val expectedEvidence = paths.expectedEvidence.readText()
        assertTrue(expectedEvidence.contains("\"scenarioId\": \"phase4-v4-pr00-selftest\""))
        assertTrue(expectedEvidence.contains("\"manualRecordPath\": \"docs/review/phase4/v4-pr/manual-records/phase4-v4-pr00-selftest.md\""))
        assertTrue(expectedEvidence.contains("phase4-v4-pr00-scenario-bootstrap.png.metadata.txt"))
        assertTrue(expectedEvidence.contains("phase4-v4-pr00-scenario-bootstrap.png.sha256"))
        assertFalseMachinePath(expectedEvidence)

        assertEquals(result.appHash, paths.appExecutableSha256.readText().substringBefore("  "))
    }

    @Test
    fun `pr01 scenario generates profession tree evidence names from the typed registry`() {
        val result =
            Phase4V4WhiteboxScenarioCli.run(
                baseConfig(scenarioId = "phase4-v4-pr01"),
            )
        val paths = result.paths

        val launchScript = paths.launchScript.readText()
        assertTrue(launchScript.contains("SCENARIO_APP_LOG=\"build/whitebox/phase4-v4-pr01/evidence/phase4-v4-pr01-app.log\""))

        val runbook = paths.runbook.readText()
        assertTrue(runbook.contains("phase4-v4-pr01-talent-tree-start.png"))
        assertTrue(runbook.contains("phase4-v4-pr01-reserve-active-slot.png"))
        assertTrue(runbook.contains("| 5 | Keyboard (initial UI mode: MAP) | F9, Right, Enter, Esc, T, Enter | Capture after the final Enter: ACTIVE_TALENT_SLOT_CHOICE"))
        assertTrue(!runbook.contains("select another active node"))
        assertTrue(!runbook.contains("Move to a tier-3 node"))
        assertTrue(!runbook.contains("arcanist validation restart"))
        assertTrue(runbook.contains("log.talent.rank_up"))
        assertTrue(runbook.contains("docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md"))
        assertFalseMachinePath(runbook)

        val expectedEvidence = paths.expectedEvidence.readText()
        assertTrue(expectedEvidence.contains("\"scenarioId\": \"phase4-v4-pr01\""))
        assertTrue(expectedEvidence.contains("phase4-v4-pr01-app.log"))
        assertTrue(expectedEvidence.contains("log.talent.breakpoint_chosen"))
        assertTrue(expectedEvidence.contains("phase4-v4-pr01-tier3-locked-reason.png.sha256"))
        assertFalseMachinePath(expectedEvidence)
    }

    @Test
    fun `pr03 scenario generates build identity reward evidence names from the typed registry`() {
        val result =
            Phase4V4WhiteboxScenarioCli.run(
                baseConfig(scenarioId = "phase4-v4-pr03"),
            )
        val paths = result.paths

        val launchScript = paths.launchScript.readText()
        assertTrue(launchScript.contains("SCENARIO_APP_LOG=\"build/whitebox/phase4-v4-pr03/evidence/phase4-v4-pr03-app.log\""))
        assertTrue(launchScript.contains("-Dktome.validation.scenario=phase4-v4-pr03"))

        val runbook = paths.runbook.readText()
        assertTrue(runbook.contains("phase4-v4-pr03-arcanist-reward-card.png"))
        assertTrue(runbook.contains("phase4-v4-pr03-arcanist-adopted-nonweapon.png"))
        assertTrue(runbook.contains("phase4-v4-pr03-rogue-offhand-payoff.png"))
        assertTrue(runbook.contains("phase4-v4-pr03-report-no-approved-debt.png"))
        assertTrue(runbook.contains("artifact_briar_heart"))
        assertTrue(runbook.contains("professionCapstoneAdoptionFloor.reportOnly"))
        assertTrue(runbook.contains("docs/review/phase4/v4-pr/manual-records/phase4-v4-pr03-build-identity-reward-adoption.md"))
        assertFalseMachinePath(runbook)

        val expectedEvidence = paths.expectedEvidence.readText()
        assertTrue(expectedEvidence.contains("\"scenarioId\": \"phase4-v4-pr03\""))
        assertTrue(expectedEvidence.contains("phase4-v4-pr03-app.log"))
        assertTrue(expectedEvidence.contains("log.validation.item.pr03_showcase"))
        assertTrue(expectedEvidence.contains("phase4-v4-pr03-report-no-approved-debt.png.sha256"))
        assertFalseMachinePath(expectedEvidence)
    }

    @Test
    fun `pr04 scenario generates hidden search hook evidence names from the typed registry`() {
        val result =
            Phase4V4WhiteboxScenarioCli.run(
                baseConfig(scenarioId = "phase4-v4-pr04"),
            )
        val paths = result.paths

        val launchScript = paths.launchScript.readText()
        assertTrue(launchScript.contains("SCENARIO_APP_LOG=\"build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-app.log\""))
        assertTrue(launchScript.contains("-Dktome.validation.scenario=phase4-v4-pr04"))

        val runbook = paths.runbook.readText()
        assertTrue(runbook.contains("phase4-v4-pr04-deep-iron-search-cue.png"))
        assertTrue(runbook.contains("phase4-v4-pr04-search-result-feedback.png"))
        assertTrue(runbook.contains("phase4-v4-pr04-abyssal-void-pressure.png"))
        assertTrue(runbook.contains("phase4-v4-pr04-zone-hook-triggered.png"))
        assertTrue(runbook.contains("phase4-v4-pr04-priority-no-overlap.png"))
        assertTrue(runbook.contains("search_available"))
        assertTrue(runbook.contains("void_pressure"))
        assertTrue(runbook.contains("docs/review/phase4/v4-pr/manual-records/phase4-v4-pr04-hidden-search-zone-hooks.md"))
        assertFalseMachinePath(runbook)

        val expectedEvidence = paths.expectedEvidence.readText()
        assertTrue(expectedEvidence.contains("\"scenarioId\": \"phase4-v4-pr04\""))
        assertTrue(expectedEvidence.contains("phase4-v4-pr04-app.log"))
        assertTrue(expectedEvidence.contains("log.zone.hook.void_pressure"))
        assertTrue(expectedEvidence.contains("phase4-v4-pr04-priority-no-overlap.png.sha256"))
        assertFalseMachinePath(expectedEvidence)
    }

    @Test
    fun `whitebox materialization catalog stays in parity with scenario registry`() {
        val parity = Phase4V4WhiteboxScenarioMaterializationCatalog.validateRegistryParity()

        assertTrue(
            parity.isValid,
            "missingFromMaterialization=${parity.missingFromMaterialization}, missingFromRegistry=${parity.missingFromRegistry}",
        )
    }

    private fun baseConfig(scenarioId: String?): Phase4V4WhiteboxScenarioCliConfig {
        val repoRoot = tempDir.resolve("repo")
        val appExecutable = repoRoot.resolve("client/build/release/K-ToME.app/Contents/MacOS/K-ToME")
        val scenarioYaml = repoRoot.resolve("tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml")
        Files.createDirectories(appExecutable.parent)
        Files.createDirectories(scenarioYaml.parent)
        appExecutable.writeText("fake packaged app")
        scenarioYaml.writeText(
            """
            |scenarios:
            |  - id: phase4-v4-pr00-selftest
            |  - id: phase4-v4-pr01
            |  - id: phase4-v4-pr02
            |  - id: phase4-v4-pr03
            |  - id: phase4-v4-pr04
            |
            """.trimMargin(),
        )
        return Phase4V4WhiteboxScenarioCliConfig(
            repoRoot = repoRoot,
            scenarioId = scenarioId,
            appExecutable = appExecutable,
            outputRoot = repoRoot.resolve("build/whitebox"),
            scenarioYaml = scenarioYaml,
        )
    }

    private fun assertFalseMachinePath(payload: String) {
        assertTrue(!payload.contains("/" + "Users/"))
        assertTrue(!payload.contains("/" + "tmp/"))
    }
}
