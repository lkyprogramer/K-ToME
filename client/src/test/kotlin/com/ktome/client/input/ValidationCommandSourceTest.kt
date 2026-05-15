package com.ktome.client.input

import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.client.validation.ValidationScenarioPresentationCatalog
import com.ktome.game.GameModule
import com.ktome.game.validation.ValidationAction
import com.ktome.game.validation.ValidationOverlaySection
import com.ktome.game.validation.ValidationScenarioActionId
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationScenarioRegistry
import com.ktome.game.validation.ValidationSessionRequest
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ValidationCommandSourceTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `phase4 v4 fast section only appears in scenario sessions`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))
        val scenarioSession =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("scenario-command-source")),
                    options = scenario.toSessionOptions(),
                ),
            )
        val normalSession =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("normal-command-source")),
                ),
            )

        val scenarioPanel =
            requireNotNull(
                enrichValidationOverlayState(
                    scenarioSession,
                    OverlayState(
                        mode = UiMode.VALIDATION,
                        validationCursor = ValidationOverlayCursor(selectedSection = ValidationOverlaySection.PHASE4_V4_FAST),
                    ),
                ).validationPanel,
            )
        val normalPanel =
            requireNotNull(
                enrichValidationOverlayState(
                    normalSession,
                    OverlayState(mode = UiMode.VALIDATION, validationCursor = ValidationOverlayCursor()),
                ).validationPanel,
        )

        assertTrue(scenarioPanel.sections.any { section -> section.titleKey == ValidationOverlaySection.PHASE4_V4_FAST.titleKey })
        assertFalse(normalPanel.sections.any { section -> section.titleKey == ValidationOverlaySection.PHASE4_V4_FAST.titleKey })
        val scenarioContext = requireNotNull(scenarioPanel.scenarioContext)
        assertEquals(ValidationScenarioPresentationCatalog.require(scenario.id).titleKey, scenarioContext.titleKey)
        assertTrue("evidence/phase4-v4-pr00-scenario-bootstrap.png" in scenarioContext.requiredEvidenceKeys)
        assertNull(normalPanel.scenarioContext)
    }

    @Test
    fun `descriptor plan cache keys by scenario scope`() {
        val normalScope =
            ValidationOverlayDescriptorScope(
                preset = com.ktome.game.validation.ValidationPreset.MAPGEN_DIFF,
                restartMode = ValidationOverlayRestartMode.SAME_PRESET_ONLY,
            )
        val scenarioScope = normalScope.copy(scenarioId = ValidationScenarioId("phase4-v4-pr00-selftest"))

        val firstNormalPlan = ValidationOverlayDescriptorPlanCache.plan(normalScope)
        val secondNormalPlan = ValidationOverlayDescriptorPlanCache.plan(normalScope)
        val scenarioPlan = ValidationOverlayDescriptorPlanCache.plan(scenarioScope)

        assertSame(firstNormalPlan, secondNormalPlan)
        assertFalse(ValidationOverlaySection.PHASE4_V4_FAST in firstNormalPlan.sections)
        assertTrue(ValidationOverlaySection.PHASE4_V4_FAST in scenarioPlan.sections)
    }

    @Test
    fun `scenario presentation catalog stays in parity with registry`() {
        val parity = ValidationScenarioPresentationCatalog.validateRegistryParity()

        assertTrue(
            parity.isValid,
            "missingFromPresentation=${parity.missingFromPresentation}, missingFromRegistry=${parity.missingFromRegistry}",
        )
    }

    @Test
    fun `dark uiux pr02 1 scenario exposes presentation title key`() {
        val scenarioId = ValidationScenarioId("dark-uiux-pr02-1-demo-shell-foundation")

        assertEquals(
            "validation.phase4.v4.dark-uiux-pr02-1-demo-shell-foundation.title",
            ValidationScenarioPresentationCatalog.require(scenarioId).titleKey,
        )
    }

    @Test
    fun `dark uiux pr03 scenario exposes presentation title key`() {
        val scenarioId = ValidationScenarioId("dark-uiux-pr03-equipment-inventory-items")

        assertEquals(
            "validation.phase4.v4.dark-uiux-pr03-equipment-inventory-items.title",
            ValidationScenarioPresentationCatalog.require(scenarioId).titleKey,
        )
    }

    @Test
    fun `phase4 v4 fast section emits typed scenario actions`() {
        val scenarioId = ValidationScenarioId("phase4-v4-pr00-selftest")
        val action =
            validationOverlayAction(
                ValidationOverlaySelection(
                    preset = com.ktome.game.validation.ValidationPreset.MAPGEN_DIFF,
                    restartNextSeedEnabled = false,
                    scenarioId = scenarioId,
                    section = ValidationOverlaySection.PHASE4_V4_FAST,
                    index = 0,
                    inspectCursor = Point.ZERO,
                ),
            )

        assertEquals(
            ValidationAction.Phase4V4ScenarioAction(
                scenarioId = scenarioId,
                actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
            ),
            action,
        )
    }
}
