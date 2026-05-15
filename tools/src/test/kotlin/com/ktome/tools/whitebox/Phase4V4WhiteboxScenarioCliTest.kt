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
        assertTrue(exception.message?.contains("dark-uiux-pr02-ui-chrome-sprite-pilot") == true)
        assertTrue(exception.message?.contains("dark-uiux-pr03-equipment-inventory-items") == true)
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
        assertTrue(launchScript.contains("EXTRA_JAVA_TOOL_OPTIONS=\"\""))
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
    fun `dark uiux pr02 scenario generates chrome fit evidence names from the typed registry`() {
        val result =
            Phase4V4WhiteboxScenarioCli.run(
                baseConfig(scenarioId = "dark-uiux-pr02-ui-chrome-sprite-pilot"),
            )
        val paths = result.paths

        val launchScript = paths.launchScript.readText()
        assertTrue(launchScript.contains("SCENARIO_APP_LOG=\"build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-ui-chrome-sprite-pilot-app.log\""))
        assertTrue(launchScript.contains("-Dktome.validation.scenario=dark-uiux-pr02-ui-chrome-sprite-pilot"))
        assertTrue(launchScript.contains("-Dktome.whitebox.manualRecord=UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md"))

        val runbook = paths.runbook.readText()
        assertTrue(runbook.contains("dark-uiux-pr02-shell-hud-frame-fit.png"))
        assertTrue(runbook.contains("dark-uiux-pr02-inventory-modal-frame-fit.png"))
        assertTrue(runbook.contains("dark-uiux-pr02-validation-overlay-frame-fit.png"))
        assertTrue(runbook.contains("dark-uiux-pr02-runtime-error-loading-fit.png"))
        assertTrue(runbook.contains("UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md"))
        assertTrue(runbook.contains("PR-02 chrome frame content bounds"))
        assertFalseMachinePath(runbook)

        val expectedEvidence = paths.expectedEvidence.readText()
        assertTrue(expectedEvidence.contains("\"scenarioId\": \"dark-uiux-pr02-ui-chrome-sprite-pilot\""))
        assertTrue(expectedEvidence.contains("dark-uiux-pr02-ui-chrome-sprite-pilot-app.log"))
        assertTrue(expectedEvidence.contains("dark-uiux-pr02-shell-hud-frame-fit.png.sha256"))
        assertFalseMachinePath(expectedEvidence)
    }

    @Test
    fun `dark uiux pr02 2 scenario generates ui demo new evidence names from the typed registry`() {
        val result =
            Phase4V4WhiteboxScenarioCli.run(
                baseConfig(scenarioId = "dark-uiux-pr02-1-demo-shell-foundation"),
            )
        val paths = result.paths

        val launchScript = paths.launchScript.readText()
        assertTrue(launchScript.contains("SCENARIO_APP_LOG=\"build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-app.log\""))
        assertTrue(launchScript.contains("-Dktome.validation.scenario=dark-uiux-pr02-1-demo-shell-foundation"))
        assertTrue(launchScript.contains("-Dktome.whitebox.root=build/whitebox/dark-uiux-pr02-1-demo-shell-foundation"))
        assertTrue(launchScript.contains("-Dktome.whitebox.evidenceDir=build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence"))
        assertTrue(launchScript.contains("-Dktome.whitebox.manualRecord=UI/manual-records/ui-demo-new-visual-parity.md"))

        val runbook = paths.runbook.readText()
        assertTrue(runbook.contains("- locale: `zh-CN`"))
        assertTrue(runbook.contains("- window: `1672x941`"))
        listOf(
            "ui-demo-new-parity-1672x941.png",
            "ui-demo-new-parity-1280x800.png",
            "ui-demo-new-right-panel-grid.png",
            "ui-demo-new-bottom-deck-no-command-hints.png",
            "ui-demo-new-inventory-page-1.png",
            "ui-demo-new-inventory-page-2.png",
            "ui-demo-new-nav-rail-crop.png",
            "ui-demo-new-map-stage-crop.png",
        ).forEach { evidenceName ->
            assertTrue(runbook.contains(evidenceName), evidenceName)
            assertTrue(runbook.contains("scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --out build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/$evidenceName"), evidenceName)
        }
        assertTrue(runbook.contains("UI/manual-records/ui-demo-new-visual-parity.md"))
        assertFalseMachinePath(runbook)

        val expectedEvidence = paths.expectedEvidence.readText()
        assertTrue(expectedEvidence.contains("\"scenarioId\": \"dark-uiux-pr02-1-demo-shell-foundation\""))
        assertTrue(expectedEvidence.contains("ui-demo-new-app.log"))
        assertTrue(expectedEvidence.contains("ui-demo-new-parity-1672x941.png.sha256"))
        assertTrue(expectedEvidence.contains("ui-demo-new-map-stage-crop.png.sha256"))
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
    fun `dark uiux pr03 scenario generates equipment inventory evidence names from the typed registry`() {
        val result =
            Phase4V4WhiteboxScenarioCli.run(
                baseConfig(scenarioId = "dark-uiux-pr03-equipment-inventory-items"),
            )
        val paths = result.paths

        val launchScript = paths.launchScript.readText()
        assertTrue(launchScript.contains("SCENARIO_APP_LOG=\"build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-app.log\""))
        assertTrue(launchScript.contains("-Dktome.validation.scenario=dark-uiux-pr03-equipment-inventory-items"))
        assertTrue(launchScript.contains("-Dktome.whitebox.manualRecord=UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md"))

        val runbook = paths.runbook.readText()
        assertTrue(runbook.contains("- window: `1280x800`"))
        assertTrue(runbook.contains("dark-uiux-pr03-equipment-slots.png"))
        assertTrue(runbook.contains("dark-uiux-pr03-inventory-empty.png"))
        assertTrue(runbook.contains("dark-uiux-pr03-inventory-stacked.png"))
        assertTrue(runbook.contains("dark-uiux-pr03-inscription-shop.png"))
        assertTrue(runbook.contains("dark-uiux-pr03-shop-full-slot-replace.png"))
        assertTrue(runbook.contains("hotkeys 5-8"))
        assertTrue(runbook.contains("UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md"))
        assertFalseMachinePath(runbook)

        val expectedEvidence = paths.expectedEvidence.readText()
        assertTrue(expectedEvidence.contains("\"scenarioId\": \"dark-uiux-pr03-equipment-inventory-items\""))
        assertTrue(expectedEvidence.contains("dark-uiux-pr03-app.log"))
        assertTrue(expectedEvidence.contains("log.validation.item.pr03_showcase"))
        assertTrue(expectedEvidence.contains("dark-uiux-pr03-shop-full-slot-replace.png.sha256"))
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
    fun `pr05 scenario generates boss variant phase override evidence names from the typed registry`() {
        val result =
            Phase4V4WhiteboxScenarioCli.run(
                baseConfig(scenarioId = "phase4-v4-pr05"),
            )
        val paths = result.paths

        val launchScript = paths.launchScript.readText()
        assertTrue(launchScript.contains("SCENARIO_APP_LOG=\"build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-app.log\""))
        assertTrue(launchScript.contains("-Dktome.validation.scenario=phase4-v4-pr05"))

        val runbook = paths.runbook.readText()
        assertTrue(runbook.contains("phase4-v4-pr05-molten-glass-warning.png"))
        assertTrue(runbook.contains("phase4-v4-pr05-grey-crown-warning.png"))
        assertTrue(runbook.contains("phase4-v4-pr05-abyssal-eclipse-warning.png"))
        assertTrue(runbook.contains("phase4-v4-pr05-report-coverage.png"))
        assertTrue(runbook.contains("boss.variant.molten_glass.phase_override.entered"))
        assertTrue(runbook.contains("phaseGraphUnchangedReason=data_level_override_only"))
        assertTrue(runbook.contains("docs/review/phase4/v4-pr/manual-records/phase4-v4-pr05-boss-variant-phase-language.md"))
        assertFalseMachinePath(runbook)

        val expectedEvidence = paths.expectedEvidence.readText()
        assertTrue(expectedEvidence.contains("\"scenarioId\": \"phase4-v4-pr05\""))
        assertTrue(expectedEvidence.contains("phase4-v4-pr05-app.log"))
        assertTrue(expectedEvidence.contains("log.boss.phase_override_entered"))
        assertTrue(expectedEvidence.contains("phase4-v4-pr05-report-coverage.png.sha256"))
        assertFalseMachinePath(expectedEvidence)
    }

    @Test
    fun `pr06 scenario generates route diversity evidence names from the typed registry`() {
        val config = baseConfig(scenarioId = "phase4-v4-pr06")
        writePr06Artifacts(config.repoRoot)
        val result =
            Phase4V4WhiteboxScenarioCli.run(
                config,
            )
        val paths = result.paths

        val launchScript = paths.launchScript.readText()
        assertTrue(launchScript.contains("SCENARIO_APP_LOG=\"build/whitebox/phase4-v4-pr06/evidence/phase4-v4-pr06-app.log\""))
        assertTrue(launchScript.contains("-Dktome.validation.scenario=phase4-v4-pr06"))
        assertTrue(launchScript.contains("-Dktome.repo.root=${'$'}REPO_ROOT"))
        assertTrue(launchScript.contains("PR06_PRIMARY_RESULT='artifactStatus=loaded;producerArtifactStatus=loaded;scenarioTypeDistribution={full_route=12,branch_inclusive=4,route_probe=2,late_route_probe=2}"))
        assertTrue(launchScript.contains("zoneRouteHashDistribution=2_hashes,max=4/5"))
        assertTrue(launchScript.contains("branchInclusiveRoutes=1(secret:greenwood_hidden_cache)"))
        assertTrue(launchScript.contains("-Dktome.phase4.v4.pr06.primaryResult=${'$'}PR06_PRIMARY_RESULT"))
        assertTrue(launchScript.contains("-Dktome.phase4.v4.pr06.evidenceResult=${'$'}PR06_EVIDENCE_RESULT"))
        assertTrue(launchScript.contains("verifyChangedTasks=2_tasks(:game:longRunLab|:tools:scopeCoverageLint)"))

        val runbook = paths.runbook.readText()
        assertTrue(runbook.contains("phase4-v4-pr06-scenario-distribution.png"))
        assertTrue(runbook.contains("phase4-v4-pr06-route-hash-diversity.png"))
        assertTrue(runbook.contains("phase4-v4-pr06-branch-inclusive-routes.png"))
        assertTrue(runbook.contains("phase4-v4-pr06-verifychanged-routing.png"))
        assertTrue(runbook.contains("log.validation.phase4_v4.action"))
        assertTrue(runbook.contains("docs/review/phase4/v4-pr/manual-records/phase4-v4-pr06-long-run-route-diversity.md"))
        assertFalseMachinePath(runbook)

        val expectedEvidence = paths.expectedEvidence.readText()
        assertTrue(expectedEvidence.contains("\"scenarioId\": \"phase4-v4-pr06\""))
        assertTrue(expectedEvidence.contains("phase4-v4-pr06-app.log"))
        assertTrue(expectedEvidence.contains("phase4-v4-pr06-verifychanged-routing.png.sha256"))
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
            |  - id: dark-uiux-pr02-ui-chrome-sprite-pilot
            |  - id: dark-uiux-pr02-1-demo-shell-foundation
            |  - id: dark-uiux-pr03-equipment-inventory-items
            |  - id: phase4-v4-pr03
            |  - id: phase4-v4-pr04
            |  - id: phase4-v4-pr05
            |  - id: phase4-v4-pr06
            |  - id: phase4-v4-pr07
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

    private fun writePr06Artifacts(repoRoot: Path) {
        val longRunPath = repoRoot.resolve("build/reports/harness/long-run-full.json")
        Files.createDirectories(longRunPath.parent)
        longRunPath.writeText("""{"scenarioTypeDistribution":{"full_route":12}}""")
        val reportPath = repoRoot.resolve("tools/build/reports/verification/phase4/report-phase4-summary.json")
        Files.createDirectories(reportPath.parent)
        reportPath.writeText(
            """
            |{
            |  "sections": {
            |    "routeDiversity": {
            |      "scenarioTypeDistribution": {
            |        "full_route": 12,
            |        "branch_inclusive": 4,
            |        "route_probe": 2,
            |        "late_route_probe": 2
            |      },
            |      "zoneRouteHashDistribution": {
            |        "hash_a": 4,
            |        "hash_b": 1
            |      },
            |      "zoneRouteHashDiversity": {
            |        "fullRouteIntentDistinctCount": 12,
            |        "actualFullRouteHashDistinctCount": 7,
            |        "topHashShare": 0.25
            |      },
            |      "routeTokenSample": [
            |        "greenwood_fringe>secret:greenwood_hidden_cache",
            |        "deep_iron_pit>grey_gate_depths"
            |      ]
            |    }
            |  }
            |}
            """.trimMargin(),
        )
        val verifyChangedPath = repoRoot.resolve("build/verification/verify-changed/verify-changed-plan.json")
        Files.createDirectories(verifyChangedPath.parent)
        verifyChangedPath.writeText(
            """
            |{
            |  "requestedTaskPaths": [
            |    ":game:longRunLab",
            |    ":tools:scopeCoverageLint"
            |  ]
            |}
            """.trimMargin(),
        )
    }
}
