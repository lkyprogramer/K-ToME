package com.ktome.tools.whitebox

import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationScenarioRegistry

internal data class Phase4V4WhiteboxScenarioMaterializationSpec(
    val id: ValidationScenarioId,
    val windowWidth: Int,
    val windowHeight: Int,
) {
    init {
        require(windowWidth > 0 && windowHeight > 0) {
            "Phase4 v4 whitebox materialization ${id.value} must declare a positive window size."
        }
    }
}

internal object Phase4V4WhiteboxScenarioMaterializationCatalog {
    private val specs: List<Phase4V4WhiteboxScenarioMaterializationSpec> =
        listOf(
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("phase4-v4-pr00-selftest"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("phase4-v4-pr01"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("dark-uiux-pr04-profession-tree-ui"),
                windowWidth = 1280,
                windowHeight = 840,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("dark-uiux-pr04-01-static-passive-detail"),
                windowWidth = 1280,
                windowHeight = 840,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("dark-uiux-pr04-01-trigger-passive-detail"),
                windowWidth = 1280,
                windowHeight = 840,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("dark-uiux-pr04-01-passive-action-suppression"),
                windowWidth = 1280,
                windowHeight = 840,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("dark-uiux-pr04-01-effective-hp-regen-detail"),
                windowWidth = 1280,
                windowHeight = 840,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("phase4-v4-pr02"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("dark-uiux-pr02-ui-chrome-sprite-pilot"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("dark-uiux-pr02-1-demo-shell-foundation"),
                windowWidth = 1672,
                windowHeight = 941,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("dark-uiux-pr03-equipment-inventory-items"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("phase4-v4-pr03"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("phase4-v4-pr04"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("phase4-v4-pr05"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("phase4-v4-pr06"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
            Phase4V4WhiteboxScenarioMaterializationSpec(
                id = ValidationScenarioId("phase4-v4-pr07"),
                windowWidth = 1280,
                windowHeight = 800,
            ),
        )
    private val specsById: Map<ValidationScenarioId, Phase4V4WhiteboxScenarioMaterializationSpec> = specs.associateBy { spec -> spec.id }

    fun require(id: ValidationScenarioId): Phase4V4WhiteboxScenarioMaterializationSpec =
        specsById[id] ?: error("Missing Phase4 v4 whitebox materialization spec for ${id.value}.")

    fun validateRegistryParity(): Phase4V4WhiteboxScenarioMaterializationParity {
        val registryIds = ValidationScenarioRegistry.knownIds()
        val materializationIds = specs.map { spec -> spec.id.value }
        return Phase4V4WhiteboxScenarioMaterializationParity(
            registryIds = registryIds,
            materializationIds = materializationIds,
            missingFromMaterialization = registryIds.filterNot(materializationIds::contains),
            missingFromRegistry = materializationIds.filterNot(registryIds::contains),
        )
    }
}

internal data class Phase4V4WhiteboxScenarioMaterializationParity(
    val registryIds: List<String>,
    val materializationIds: List<String>,
    val missingFromMaterialization: List<String>,
    val missingFromRegistry: List<String>,
) {
    val isValid: Boolean
        get() = missingFromMaterialization.isEmpty() && missingFromRegistry.isEmpty()
}
