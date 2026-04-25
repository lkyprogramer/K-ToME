package com.ktome.game.validation

import com.ktome.core.save.SaveManager
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ValidationScenarioRegistryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `pr00 selftest scenario matches fixed phase4 v4 contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))

        assertEquals("PR-00", scenario.prId)
        assertEquals(ValidationPreset.MAPGEN_DIFF, scenario.runtime.preset)
        assertEquals(2026042430L, scenario.runtime.seed)
        assertEquals("vanguard", scenario.runtime.professionId)
        assertEquals("human", scenario.runtime.raceId)
        assertEquals("greenwood_fringe", scenario.runtime.zoneId)
        assertEquals(2, scenario.runtime.floor)
        assertEquals(-1, scenario.runtime.routeIndex)
        assertEquals(ValidationScenarioContentPackMode.NONE, scenario.runtime.contentPackMode)
        assertTrue("evidence/phase4-v4-pr00-scenario-bootstrap.png" in scenario.evidence.requiredEvidenceFiles)
        assertEquals(
            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr00-selftest.md",
            scenario.evidence.manualRecordPath,
        )
        assertEquals(
            scenario.evidence.requiredEvidenceFiles.count { file -> file.endsWith(".png") },
            scenario.evidence.cuaSteps.size,
        )
    }

    @Test
    fun `scenario yaml ids stay in parity with typed registry`() {
        val repoRoot = Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()
        val parity =
            ValidationScenarioRegistry.validateYamlParity(
                repoRoot.resolve("tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml"),
            )

        assertTrue(parity.isValid, "missingFromYaml=${parity.missingFromYaml}, missingFromKotlin=${parity.missingFromKotlin}")
    }

    @Test
    fun `scenario yaml rejects duplicated contract fields`() {
        val yamlPath = tempDir.resolve("phase4-v4-scenarios.yaml")
        java.nio.file.Files.writeString(
            yamlPath,
            """
            |scenarios:
            |  - id: phase4-v4-pr00-selftest
            |    seed: 2026042430
            |
            """.trimMargin(),
        )

        assertThrows(IllegalArgumentException::class.java) {
            ValidationScenarioRegistry.validateYamlParity(yamlPath)
        }
    }

    @Test
    fun `scenario requires four screenshot evidence files`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))

        assertThrows(IllegalArgumentException::class.java) {
            scenario.copy(
                evidence =
                    scenario.evidence.copy(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/bootstrap.png",
                                "evidence/primary.png",
                                "evidence/secondary.png",
                                "evidence/app.log",
                            ),
                    ),
            )
        }
    }

    @Test
    fun `scenario session options force no profile persistence and expose scenario id`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))
        val options = scenario.toSessionOptions()

        assertEquals(ValidationScenarioId("phase4-v4-pr00-selftest"), options.scenarioId)
        assertEquals(ProfileRunPersistenceMode.NO_OP, options.profileRunPersistenceMode)
        assertEquals(ValidationPreset.MAPGEN_DIFF, options.preset)
        assertEquals(2026042430L, options.foundationConfig.seed)
        assertEquals("vanguard", options.foundationConfig.playerProfessionId)
        assertEquals("human", options.foundationConfig.playerRaceId)
        assertEquals("greenwood_fringe", options.foundationConfig.zoneId)
        assertEquals(2, options.foundationConfig.floor)
        assertEquals(-1, options.scenarioRouteIndex)
        assertEquals(1, options.foundationConfig.routeIndex)
    }

    @Test
    fun `scenario route index is preserved as scenario boundary without overriding runtime route`() {
        val baseScenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))
        val scenario = baseScenario.copy(runtime = baseScenario.runtime.copy(routeIndex = 6))
        val options = scenario.toSessionOptions()

        assertEquals(6, options.scenarioRouteIndex)
        assertEquals(1, options.foundationConfig.routeIndex)
    }

    @Test
    fun `scenario runtime rejects route index below no-corpus sentinel`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))

        assertThrows(IllegalArgumentException::class.java) {
            scenario.runtime.copy(routeIndex = -2)
        }
    }

    @Test
    fun `phase4 v4 scenario actions dispatch through validation command path`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))
        val evidenceSummary =
            ValidationScenarioEvidenceSummary(
                whiteboxRoot = "build/whitebox/phase4-v4-pr00-selftest",
                evidenceDir = "build/whitebox/phase4-v4-pr00-selftest/evidence",
                manualRecordPath = scenario.evidence.manualRecordPath,
                expectedEvidencePath = "build/whitebox/phase4-v4-pr00-selftest/expected-evidence.json",
                runbookPath = "build/whitebox/phase4-v4-pr00-selftest/cua-runbook.md",
                appExecutableSha256Path = "build/whitebox/phase4-v4-pr00-selftest/app-executable.sha256",
                appExecutableSha256 = "abc123",
            )
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("phase4-v4-scenario-actions")),
                    options = scenario.toSessionOptions(evidenceSummary = evidenceSummary),
                ),
            )

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )
        val afterFirst = session.validationSummarySnapshot()?.lastResult
        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )

        assertEquals(afterFirst, session.validationSummarySnapshot()?.lastResult)
        assertEquals(null, session.validationSummarySnapshot()?.scenarioEvidenceSummary)
        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.SHOW_EVIDENCE_SUMMARY,
                    ),
                ),
            ),
        )
        assertEquals(evidenceSummary, session.validationSummarySnapshot()?.scenarioEvidenceSummary)
    }
}
