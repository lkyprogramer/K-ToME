package com.ktome.client.validation

import com.ktome.game.validation.ValidationOverlaySection
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationScenarioRegistry

internal enum class ValidationScenarioStartupMode {
    VALIDATION_SETUP,
    DIRECT_VALIDATION_SESSION,
}

internal data class ValidationScenarioPresentationSpec(
    val id: ValidationScenarioId,
    val titleKey: String,
    val startupMode: ValidationScenarioStartupMode,
    val initialOverlaySection: ValidationOverlaySection,
) {
    init {
        require(titleKey.isNotBlank()) { "Validation scenario presentation ${id.value} must declare titleKey." }
    }
}

internal object ValidationScenarioPresentationCatalog {
    private val specs: List<ValidationScenarioPresentationSpec> =
        listOf(
            ValidationScenarioPresentationSpec(
                id = ValidationScenarioId("phase4-v4-pr00-selftest"),
                titleKey = "validation.phase4.v4.phase4-v4-pr00-selftest.title",
                startupMode = ValidationScenarioStartupMode.DIRECT_VALIDATION_SESSION,
                initialOverlaySection = ValidationOverlaySection.PHASE4_V4_FAST,
            ),
            ValidationScenarioPresentationSpec(
                id = ValidationScenarioId("phase4-v4-pr01"),
                titleKey = "validation.phase4.v4.phase4-v4-pr01.title",
                startupMode = ValidationScenarioStartupMode.DIRECT_VALIDATION_SESSION,
                initialOverlaySection = ValidationOverlaySection.PHASE4_V4_FAST,
            ),
            ValidationScenarioPresentationSpec(
                id = ValidationScenarioId("phase4-v4-pr02"),
                titleKey = "validation.phase4.v4.phase4-v4-pr02.title",
                startupMode = ValidationScenarioStartupMode.DIRECT_VALIDATION_SESSION,
                initialOverlaySection = ValidationOverlaySection.PHASE4_V4_FAST,
            ),
            ValidationScenarioPresentationSpec(
                id = ValidationScenarioId("phase4-v4-pr03"),
                titleKey = "validation.phase4.v4.phase4-v4-pr03.title",
                startupMode = ValidationScenarioStartupMode.DIRECT_VALIDATION_SESSION,
                initialOverlaySection = ValidationOverlaySection.PHASE4_V4_FAST,
            ),
            ValidationScenarioPresentationSpec(
                id = ValidationScenarioId("phase4-v4-pr04"),
                titleKey = "validation.phase4.v4.phase4-v4-pr04.title",
                startupMode = ValidationScenarioStartupMode.DIRECT_VALIDATION_SESSION,
                initialOverlaySection = ValidationOverlaySection.PHASE4_V4_FAST,
            ),
            ValidationScenarioPresentationSpec(
                id = ValidationScenarioId("phase4-v4-pr05"),
                titleKey = "validation.phase4.v4.phase4-v4-pr05.title",
                startupMode = ValidationScenarioStartupMode.DIRECT_VALIDATION_SESSION,
                initialOverlaySection = ValidationOverlaySection.PHASE4_V4_FAST,
            ),
        )
    private val specsById: Map<ValidationScenarioId, ValidationScenarioPresentationSpec> = specs.associateBy { spec -> spec.id }

    fun find(id: ValidationScenarioId): ValidationScenarioPresentationSpec? = specsById[id]

    fun require(id: ValidationScenarioId): ValidationScenarioPresentationSpec =
        find(id) ?: error("Missing validation scenario presentation spec for ${id.value}.")

    fun validateRegistryParity(): ValidationScenarioPresentationParity {
        val registryIds = ValidationScenarioRegistry.knownIds()
        val presentationIds = specs.map { spec -> spec.id.value }
        return ValidationScenarioPresentationParity(
            registryIds = registryIds,
            presentationIds = presentationIds,
            missingFromPresentation = registryIds.filterNot(presentationIds::contains),
            missingFromRegistry = presentationIds.filterNot(registryIds::contains),
        )
    }
}

internal data class ValidationScenarioPresentationParity(
    val registryIds: List<String>,
    val presentationIds: List<String>,
    val missingFromPresentation: List<String>,
    val missingFromRegistry: List<String>,
) {
    val isValid: Boolean
        get() = missingFromPresentation.isEmpty() && missingFromRegistry.isEmpty()
}

internal fun validationScenarioRequiredEvidenceKeys(scenarioId: ValidationScenarioId?): List<String> {
    scenarioId ?: return emptyList()
    return ValidationScenarioRegistry.find(scenarioId)?.evidence?.requiredEvidenceFiles.orEmpty()
}
