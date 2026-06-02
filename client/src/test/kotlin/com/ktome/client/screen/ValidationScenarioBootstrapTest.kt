package com.ktome.client.screen

import com.ktome.client.validation.ValidationScenarioPresentationSpec
import com.ktome.client.validation.ValidationScenarioStartupMode
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.validation.ValidationOverlaySection
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationScenarioStartupSurface
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidationScenarioBootstrapTest {
    @Test
    fun `valid system property starts direct scenario validation session options`() {
        val result =
            ValidationScenarioBootstrap.resolve(
                propertyProvider =
                    mapOf(
                        ValidationScenarioBootstrap.SCENARIO_PROPERTY to "phase4-v4-pr00-selftest",
                        ValidationScenarioBootstrap.WHITEBOX_ROOT_PROPERTY to "build/whitebox/phase4-v4-pr00-selftest",
                        ValidationScenarioBootstrap.EVIDENCE_DIR_PROPERTY to "build/whitebox/phase4-v4-pr00-selftest/evidence",
                        ValidationScenarioBootstrap.MANUAL_RECORD_PROPERTY to
                            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr00-selftest.md",
                        ValidationScenarioBootstrap.APP_HASH_PROPERTY to "abc123",
                    )::get,
                samplePackSelection = ContentPackSelection.EMPTY,
            ) as ValidationScenarioBootstrapResult.Start

        assertEquals(GameLocale.ZH_CN, result.scenario.runtime.locale)
        assertEquals(ValidationScenarioId("phase4-v4-pr00-selftest"), result.options.scenarioId)
        assertEquals(ValidationPreset.MAPGEN_DIFF, result.options.preset)
        assertEquals(2026042430L, result.options.foundationConfig.seed)
        assertEquals(-1, result.options.scenarioRouteIndex)
        assertEquals(
            "build/whitebox/phase4-v4-pr00-selftest/expected-evidence.json",
            result.options.scenarioEvidenceSummary?.expectedEvidencePath,
        )
        assertEquals(
            "build/whitebox/phase4-v4-pr00-selftest/cua-runbook.md",
            result.options.scenarioEvidenceSummary?.runbookPath,
        )
        assertEquals("abc123", result.options.scenarioEvidenceSummary?.appExecutableSha256)
    }

    @Test
    fun `valid startup surface system property maps to typed scenario startup surface`() {
        val result =
            ValidationScenarioBootstrap.resolve(
                propertyProvider =
                    mapOf(
                        ValidationScenarioBootstrap.SCENARIO_PROPERTY to "dark-uiux-pr08-director-mine-map-stage",
                        ValidationScenarioBootstrap.WHITEBOX_ROOT_PROPERTY to "build/whitebox/dark-uiux-pr08-director-mine-map-stage",
                        ValidationScenarioBootstrap.STARTUP_SURFACE_PROPERTY to "evidence-summary",
                    )::get,
                samplePackSelection = ContentPackSelection.EMPTY,
            ) as ValidationScenarioBootstrapResult.Start

        assertEquals(
            ValidationScenarioStartupSurface.EVIDENCE_SUMMARY,
            result.options.scenarioStartupSurface,
        )
    }

    @Test
    fun `invalid startup surface system property fails fast`() {
        val result =
            ValidationScenarioBootstrap.resolve(
                propertyProvider =
                    mapOf(
                        ValidationScenarioBootstrap.SCENARIO_PROPERTY to "dark-uiux-pr08-director-mine-map-stage",
                        ValidationScenarioBootstrap.WHITEBOX_ROOT_PROPERTY to "build/whitebox/dark-uiux-pr08-director-mine-map-stage",
                        ValidationScenarioBootstrap.STARTUP_SURFACE_PROPERTY to "prepare-shop-surface",
                    )::get,
                samplePackSelection = ContentPackSelection.EMPTY,
            ) as ValidationScenarioBootstrapResult.Error
        val payload =
            validationScenarioErrorState(
                result,
                LocalizationBundle.load().translator(GameLocale.EN_US),
            ).payload.renderPlainText()

        assertEquals(ValidationScenarioBootstrapErrorCode.INVALID_PHASE4_V4_STARTUP_SURFACE, result.errorCode)
        assertTrue(payload.contains("validationErrorCode: INVALID_PHASE4_V4_STARTUP_SURFACE"))
        assertTrue(payload.contains("ktome.validation.startupSurface=prepare-shop-surface"))
    }

    @Test
    fun `unknown scenario creates copyable fail-fast error payload`() {
        val result =
            ValidationScenarioBootstrap.resolve(
                propertyProvider =
                    mapOf(
                        ValidationScenarioBootstrap.SCENARIO_PROPERTY to "missing-scenario",
                        ValidationScenarioBootstrap.WHITEBOX_ROOT_PROPERTY to "build/whitebox/missing",
                        ValidationScenarioBootstrap.EVIDENCE_DIR_PROPERTY to "build/whitebox/missing/evidence",
                        ValidationScenarioBootstrap.MANUAL_RECORD_PROPERTY to "docs/review/phase4/v4-pr/manual-records/missing.md",
                    )::get,
            ) as ValidationScenarioBootstrapResult.Error
        val errorState =
            validationScenarioErrorState(
                result,
                LocalizationBundle.load().translator(GameLocale.EN_US),
            )
        val payload = errorState.payload.renderPlainText()

        assertTrue(payload.contains("scenarioId: missing-scenario"))
        assertTrue(payload.contains("validationErrorCode: UNKNOWN_PHASE4_V4_SCENARIO"))
        assertTrue(payload.contains("knownScenarioIds: phase4-v4-pr00-selftest"))
        assertTrue(payload.contains("manualRecordPath: docs/review/phase4/v4-pr/manual-records/missing.md"))
        assertTrue(payload.contains("expectedEvidencePath: build/whitebox/missing/expected-evidence.json"))
        assertEquals("validation.phase4.v4.error.copy_detail", errorState.copyDetailLabelKey)
    }

    @Test
    fun `registered scenario missing presentation creates copyable fail-fast error payload`() {
        val result =
            ValidationScenarioBootstrap.resolve(
                propertyProvider =
                    mapOf(
                        ValidationScenarioBootstrap.SCENARIO_PROPERTY to "phase4-v4-pr00-selftest",
                        ValidationScenarioBootstrap.WHITEBOX_ROOT_PROPERTY to "build/whitebox/phase4-v4-pr00-selftest",
                        ValidationScenarioBootstrap.MANUAL_RECORD_PROPERTY to
                            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr00-selftest.md",
                    )::get,
                presentationProvider = { null },
            ) as ValidationScenarioBootstrapResult.Error
        val payload =
            validationScenarioErrorState(
                result,
                LocalizationBundle.load().translator(GameLocale.EN_US),
            ).payload.renderPlainText()

        assertTrue(payload.contains("scenarioId: phase4-v4-pr00-selftest"))
        assertTrue(payload.contains("validationErrorCode: MISSING_PHASE4_V4_SCENARIO_PRESENTATION"))
        assertTrue(payload.contains("startupErrorDetail: Missing validation scenario presentation spec"))
        assertTrue(payload.contains("expectedEvidencePath: build/whitebox/phase4-v4-pr00-selftest/expected-evidence.json"))
    }

    @Test
    fun `registered setup-only scenario creates copyable startup mode error payload`() {
        val scenarioId = ValidationScenarioId("phase4-v4-pr00-selftest")
        val result =
            ValidationScenarioBootstrap.resolve(
                propertyProvider =
                    mapOf(
                        ValidationScenarioBootstrap.SCENARIO_PROPERTY to scenarioId.value,
                        ValidationScenarioBootstrap.EVIDENCE_DIR_PROPERTY to
                            "build/whitebox/phase4-v4-pr00-selftest/evidence",
                    )::get,
                presentationProvider = {
                    ValidationScenarioPresentationSpec(
                        id = scenarioId,
                        titleKey = "validation.phase4.v4.phase4-v4-pr00-selftest.title",
                        startupMode = ValidationScenarioStartupMode.VALIDATION_SETUP,
                        initialOverlaySection = ValidationOverlaySection.PHASE4_V4_FAST,
                    )
                },
            ) as ValidationScenarioBootstrapResult.Error
        val payload =
            validationScenarioErrorState(
                result,
                LocalizationBundle.load().translator(GameLocale.EN_US),
            ).payload.renderPlainText()

        assertTrue(payload.contains("scenarioId: phase4-v4-pr00-selftest"))
        assertTrue(payload.contains("validationErrorCode: INVALID_PHASE4_V4_SCENARIO_STARTUP_MODE"))
        assertTrue(payload.contains("startupErrorDetail: Validation scenario phase4-v4-pr00-selftest declares startupMode=VALIDATION_SETUP"))
        assertTrue(payload.contains("expectedEvidencePath: build/whitebox/phase4-v4-pr00-selftest/expected-evidence.json"))
    }

    @Test
    fun `evidence dir alone resolves scenario evidence paths from parent root`() {
        val result =
            ValidationScenarioBootstrap.resolve(
                propertyProvider =
                    mapOf(
                        ValidationScenarioBootstrap.SCENARIO_PROPERTY to "phase4-v4-pr00-selftest",
                        ValidationScenarioBootstrap.EVIDENCE_DIR_PROPERTY to
                            "build/whitebox/phase4-v4-pr00-selftest/evidence/",
                    )::get,
                samplePackSelection = ContentPackSelection.EMPTY,
            ) as ValidationScenarioBootstrapResult.Start

        assertEquals(
            "build/whitebox/phase4-v4-pr00-selftest",
            result.options.scenarioEvidenceSummary?.whiteboxRoot,
        )
        assertEquals(
            "build/whitebox/phase4-v4-pr00-selftest/evidence",
            result.options.scenarioEvidenceSummary?.evidenceDir,
        )
        assertEquals(
            "build/whitebox/phase4-v4-pr00-selftest/expected-evidence.json",
            result.options.scenarioEvidenceSummary?.expectedEvidencePath,
        )
    }
}
