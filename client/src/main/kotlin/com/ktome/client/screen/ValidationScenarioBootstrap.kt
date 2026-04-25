package com.ktome.client.screen

import com.ktome.client.build.BuildInfo
import com.ktome.client.ui.card.ModalCardAction
import com.ktome.client.ui.state.UiErrorPayload
import com.ktome.client.ui.state.UiErrorState
import com.ktome.client.validation.ValidationScenarioPresentationCatalog
import com.ktome.client.validation.ValidationScenarioPresentationSpec
import com.ktome.client.validation.ValidationScenarioStartupMode
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.i18n.Localizer
import com.ktome.game.validation.UnknownValidationScenarioException
import com.ktome.game.validation.ValidationScenarioDef
import com.ktome.game.validation.ValidationScenarioEvidenceSummary
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationScenarioRegistry
import com.ktome.game.validation.ValidationSessionOptions
import java.nio.file.Path

internal enum class ValidationScenarioBootstrapErrorCode(
    val payloadValue: String,
) {
    UNKNOWN_PHASE4_V4_SCENARIO("UNKNOWN_PHASE4_V4_SCENARIO"),
    MISSING_PHASE4_V4_SCENARIO_PRESENTATION("MISSING_PHASE4_V4_SCENARIO_PRESENTATION"),
    INVALID_PHASE4_V4_SCENARIO_STARTUP_MODE("INVALID_PHASE4_V4_SCENARIO_STARTUP_MODE"),
}

internal sealed interface ValidationScenarioBootstrapResult {
    data object NotRequested : ValidationScenarioBootstrapResult

    data class Start(
        val scenario: ValidationScenarioDef,
        val options: ValidationSessionOptions,
    ) : ValidationScenarioBootstrapResult

    data class Error(
        val scenarioId: String,
        val knownScenarioIds: List<String>,
        val startupProperties: Map<String, String?>,
        val manualRecordPath: String?,
        val expectedEvidencePath: String?,
        val errorCode: ValidationScenarioBootstrapErrorCode =
            ValidationScenarioBootstrapErrorCode.UNKNOWN_PHASE4_V4_SCENARIO,
        val detailMessage: String? = null,
    ) : ValidationScenarioBootstrapResult
}

internal object ValidationScenarioBootstrap {
    const val SCENARIO_PROPERTY: String = "ktome.validation.scenario"
    const val WHITEBOX_ROOT_PROPERTY: String = "ktome.whitebox.root"
    const val EVIDENCE_DIR_PROPERTY: String = "ktome.whitebox.evidenceDir"
    const val MANUAL_RECORD_PROPERTY: String = "ktome.whitebox.manualRecord"
    const val APP_HASH_PROPERTY: String = "ktome.whitebox.appHash"

    fun resolve(
        propertyProvider: (String) -> String? = System::getProperty,
        samplePackSelection: ContentPackSelection = ContentPackSelection.EMPTY,
        presentationProvider: (ValidationScenarioId) -> ValidationScenarioPresentationSpec? =
            ValidationScenarioPresentationCatalog::find,
    ): ValidationScenarioBootstrapResult {
        val scenarioId = propertyProvider(SCENARIO_PROPERTY)?.takeIf(String::isNotBlank)
            ?: return ValidationScenarioBootstrapResult.NotRequested
        val startupProperties =
            mapOf(
                SCENARIO_PROPERTY to scenarioId,
                WHITEBOX_ROOT_PROPERTY to propertyProvider(WHITEBOX_ROOT_PROPERTY),
                EVIDENCE_DIR_PROPERTY to propertyProvider(EVIDENCE_DIR_PROPERTY),
                MANUAL_RECORD_PROPERTY to propertyProvider(MANUAL_RECORD_PROPERTY),
                APP_HASH_PROPERTY to propertyProvider(APP_HASH_PROPERTY),
            )
        return try {
            val scenario = ValidationScenarioRegistry.require(scenarioId, startupProperties)
            val presentation =
                presentationProvider(scenario.id)
                    ?: return errorResult(
                        scenarioId = scenario.id.value,
                        startupProperties = startupProperties,
                        errorCode = ValidationScenarioBootstrapErrorCode.MISSING_PHASE4_V4_SCENARIO_PRESENTATION,
                        detailMessage = "Missing validation scenario presentation spec for ${scenario.id.value}.",
                    )
            if (presentation.startupMode != ValidationScenarioStartupMode.DIRECT_VALIDATION_SESSION) {
                return errorResult(
                    scenarioId = scenario.id.value,
                    startupProperties = startupProperties,
                    errorCode = ValidationScenarioBootstrapErrorCode.INVALID_PHASE4_V4_SCENARIO_STARTUP_MODE,
                    detailMessage =
                        "Validation scenario ${scenario.id.value} declares startupMode=${presentation.startupMode} " +
                            "and cannot be started directly.",
                )
            }
            ValidationScenarioBootstrapResult.Start(
                scenario = scenario,
                options =
                    scenario.toSessionOptions(
                        samplePackSelection = samplePackSelection,
                        evidenceSummary = scenarioEvidenceSummary(scenario, startupProperties),
                    ),
            )
        } catch (exception: UnknownValidationScenarioException) {
            errorResult(
                scenarioId = exception.scenarioId,
                startupProperties = exception.startupProperties,
                errorCode = ValidationScenarioBootstrapErrorCode.UNKNOWN_PHASE4_V4_SCENARIO,
            )
        }
    }

    private fun errorResult(
        scenarioId: String,
        startupProperties: Map<String, String?>,
        errorCode: ValidationScenarioBootstrapErrorCode,
        detailMessage: String? = null,
    ): ValidationScenarioBootstrapResult.Error =
        ValidationScenarioBootstrapResult.Error(
            scenarioId = scenarioId,
            knownScenarioIds = ValidationScenarioRegistry.knownIds(),
            startupProperties = startupProperties,
            manualRecordPath = startupProperties[MANUAL_RECORD_PROPERTY],
            expectedEvidencePath = expectedEvidencePath(startupProperties),
            errorCode = errorCode,
            detailMessage = detailMessage,
        )

    private fun scenarioEvidenceSummary(
        scenario: ValidationScenarioDef,
        startupProperties: Map<String, String?>,
    ): ValidationScenarioEvidenceSummary? {
        val paths = resolveWhiteboxPaths(startupProperties) ?: return null
        return ValidationScenarioEvidenceSummary(
            whiteboxRoot = paths.whiteboxRoot,
            evidenceDir = paths.evidenceDir,
            manualRecordPath = startupProperties[MANUAL_RECORD_PROPERTY]?.takeIf(String::isNotBlank)
                ?: scenario.evidence.manualRecordPath,
            expectedEvidencePath = paths.expectedEvidencePath,
            runbookPath = paths.runbookPath,
            appExecutableSha256Path = paths.appExecutableSha256Path,
            appExecutableSha256 = startupProperties[APP_HASH_PROPERTY]?.takeIf(String::isNotBlank),
        )
    }

    private fun expectedEvidencePath(startupProperties: Map<String, String?>): String? =
        resolveWhiteboxPaths(startupProperties)?.expectedEvidencePath

    private fun resolveWhiteboxPaths(startupProperties: Map<String, String?>): StartupWhiteboxPaths? {
        val evidenceDir =
            startupProperties[EVIDENCE_DIR_PROPERTY]
                ?.takeIf(String::isNotBlank)
                ?.let { path -> Path.of(path).normalize() }
        val whiteboxRoot =
            startupProperties[WHITEBOX_ROOT_PROPERTY]
                ?.takeIf(String::isNotBlank)
                ?.let { path -> Path.of(path).normalize() }
                ?: evidenceDir?.whiteboxRootFromEvidenceDir()
                ?: return null
        val resolvedEvidenceDir = evidenceDir ?: whiteboxRoot.resolve("evidence").normalize()
        return StartupWhiteboxPaths(
            whiteboxRoot = whiteboxRoot.portablePath(),
            evidenceDir = resolvedEvidenceDir.portablePath(),
            expectedEvidencePath = whiteboxRoot.resolve("expected-evidence.json").portablePath(),
            runbookPath = whiteboxRoot.resolve("cua-runbook.md").portablePath(),
            appExecutableSha256Path = whiteboxRoot.resolve("app-executable.sha256").portablePath(),
        )
    }

    private fun Path.whiteboxRootFromEvidenceDir(): Path =
        if (fileName?.toString() == "evidence") {
            parent ?: this
        } else {
            this
        }

    private fun Path.portablePath(): String = normalize().toString().replace('\\', '/')

    private data class StartupWhiteboxPaths(
        val whiteboxRoot: String,
        val evidenceDir: String,
        val expectedEvidencePath: String,
        val runbookPath: String,
        val appExecutableSha256Path: String,
    )
}

internal fun validationScenarioErrorState(
    error: ValidationScenarioBootstrapResult.Error,
    localizer: Localizer,
): UiErrorState {
    val isUnknownScenario = error.errorCode == ValidationScenarioBootstrapErrorCode.UNKNOWN_PHASE4_V4_SCENARIO
    val heading =
        RenderTextTokenSnapshot(
            key =
                if (isUnknownScenario) {
                    "validation.phase4.v4.error.unknown_scenario.title"
                } else {
                    "validation.phase4.v4.error.startup_contract.title"
                },
        )
    val detail =
        RenderTextTokenSnapshot(
            key =
                if (isUnknownScenario) {
                    "validation.phase4.v4.error.unknown_scenario.body"
                } else {
                    "validation.phase4.v4.error.startup_contract.body"
                },
            arguments =
                listOf(
                    RenderTextArgumentSnapshot(name = "scenarioId", value = error.scenarioId),
                    RenderTextArgumentSnapshot(name = "knownScenarioIds", value = error.knownScenarioIds.joinToString(", ")),
                    RenderTextArgumentSnapshot(name = "validationErrorCode", value = error.errorCode.payloadValue),
                ),
        )
    val context =
        listOf(
            "scenarioId" to error.scenarioId,
            "knownScenarioIds" to error.knownScenarioIds.joinToString(", "),
            "validationErrorCode" to error.errorCode.payloadValue,
            "startupErrorDetail" to error.detailMessage.orEmpty(),
            "manualRecordPath" to error.manualRecordPath.orEmpty(),
            "expectedEvidencePath" to error.expectedEvidencePath.orEmpty(),
            "startupProperties" to
                error.startupProperties.entries.joinToString("; ") { (key, value) ->
                    "$key=${value ?: "<unset>"}"
                },
        )
    return UiErrorState(
        heading = heading,
        detail = detail,
        actions =
            listOf(
                ModalCardAction.RETRY,
                ModalCardAction.BACK_TO_MENU,
                ModalCardAction.COPY_ERROR_DETAIL,
            ),
        copyDetailLabelKey = "validation.phase4.v4.error.copy_detail",
        payload =
            UiErrorPayload(
                heading = localizer.text(heading.key),
                detail =
                    localizer.text(
                        detail.key,
                        "scenarioId" to error.scenarioId,
                        "knownScenarioIds" to error.knownScenarioIds.joinToString(", "),
                        "validationErrorCode" to error.errorCode.payloadValue,
                    ),
                contextKeyValuePairs = context,
                buildHash = BuildInfo.shortHash,
            ),
    )
}
