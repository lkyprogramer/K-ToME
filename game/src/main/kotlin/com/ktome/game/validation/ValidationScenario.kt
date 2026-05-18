package com.ktome.game.validation

import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.i18n.GameLocale

enum class ValidationOverlaySection(
    val titleKey: String,
) {
    RESTART(titleKey = "ui.validation.section.restart"),
    PR05_COMBAT(titleKey = "ui.validation.section.pr05_combat"),
    TRAVEL(titleKey = "ui.validation.section.travel"),
    RECOVERY(titleKey = "ui.validation.section.recovery"),
    ENCOUNTER(titleKey = "ui.validation.section.encounter"),
    TERRAIN(titleKey = "ui.validation.section.terrain"),
    REWARD_AND_ITEM(titleKey = "ui.validation.section.reward_and_item"),
    DISCOVERY(titleKey = "ui.validation.section.discovery"),
    PHASE4_V4_FAST(titleKey = "ui.validation.section.phase4_v4_fast"),
}

data class ValidationScenarioId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Validation scenario id must not be blank." }
    }

    override fun toString(): String = value
}

data class ValidationScenarioEvidenceStep(
    val mode: String,
    val input: String,
    val expectedVisibleResult: String,
    val evidenceFile: String,
) {
    init {
        require(mode.isNotBlank()) { "Validation scenario evidence step mode must not be blank." }
        require(input.isNotBlank()) { "Validation scenario evidence step input must not be blank." }
        require(expectedVisibleResult.isNotBlank()) { "Validation scenario evidence step expected result must not be blank." }
        require(evidenceFile.isNotBlank()) { "Validation scenario evidence step file must not be blank." }
    }
}

data class ValidationScenarioEvidenceSummary(
    val whiteboxRoot: String,
    val evidenceDir: String,
    val manualRecordPath: String,
    val expectedEvidencePath: String,
    val runbookPath: String,
    val appExecutableSha256Path: String,
    val appExecutableSha256: String?,
    val requiredLogEventKeys: List<String> = emptyList(),
    val producerFreshnessLabelKey: String = "validation.phase4.v4.evidence.producer_freshness.not_applicable",
    val scenarioNoteLabelKey: String? = null,
) {
    init {
        require(whiteboxRoot.isNotBlank()) { "Validation scenario evidence summary must declare whiteboxRoot." }
        require(evidenceDir.isNotBlank()) { "Validation scenario evidence summary must declare evidenceDir." }
        require(manualRecordPath.isNotBlank()) { "Validation scenario evidence summary must declare manualRecordPath." }
        require(expectedEvidencePath.isNotBlank()) { "Validation scenario evidence summary must declare expectedEvidencePath." }
        require(runbookPath.isNotBlank()) { "Validation scenario evidence summary must declare runbookPath." }
        require(appExecutableSha256Path.isNotBlank()) { "Validation scenario evidence summary must declare appExecutableSha256Path." }
        require(requiredLogEventKeys.all(String::isNotBlank)) {
            "Validation scenario evidence summary requiredLogEventKeys must not contain blank entries."
        }
        require(producerFreshnessLabelKey.isNotBlank()) { "Validation scenario evidence summary must declare producerFreshnessLabelKey." }
        require(scenarioNoteLabelKey == null || scenarioNoteLabelKey.isNotBlank()) {
            "Validation scenario evidence summary scenarioNoteLabelKey must not be blank."
        }
    }
}

enum class ValidationScenarioContentPackMode {
    NONE,
    SAMPLE_PACK_ENABLED,
}

enum class ValidationScenarioActionId(
    val value: String,
) {
    PREPARE_PRIMARY_SCENE("prepare-primary-scene"),
    PREPARE_SECONDARY_SCENE("prepare-secondary-scene"),
    SHOW_EVIDENCE_SUMMARY("show-evidence-summary"),
    RESET_SCENARIO("reset-scenario"),
    ;
}

data class ValidationScenarioDef(
    val id: ValidationScenarioId,
    val prId: String,
    val runtime: ValidationScenarioRuntimeSpec,
    val evidence: ValidationScenarioEvidenceSpec,
) {
    init {
        require(prId.isNotBlank()) { "Validation scenario ${id.value} must declare prId." }
    }

    fun toSessionOptions(
        samplePackSelection: ContentPackSelection = ContentPackSelection.EMPTY,
        evidenceSummary: ValidationScenarioEvidenceSummary? = null,
    ): ValidationSessionOptions {
        val contentSelection =
            when (runtime.contentPackMode) {
                ValidationScenarioContentPackMode.NONE -> ContentPackSelection.EMPTY
                ValidationScenarioContentPackMode.SAMPLE_PACK_ENABLED -> samplePackSelection
            }
        val baseOptions = validationSessionOptionsForPreset(runtime.preset, contentSelection)
        val route = scenarioZoneRoute()
        val runtimeRouteIndex = route.indexOf(runtime.zoneId).takeIf { index -> index >= 0 } ?: 0
        return baseOptions.copy(
            foundationConfig =
                baseOptions.foundationConfig.copy(
                    seed = runtime.seed,
                    playerProfessionId = runtime.professionId,
                    playerRaceId = runtime.raceId,
                    zoneId = runtime.zoneId,
                    floor = runtime.floor,
                    zoneRoute = route,
                    routeIndex = runtimeRouteIndex,
                ),
            seedCorpus = listOf(runtime.seed),
            contentPackSelection = contentSelection,
            profileRunPersistenceMode = ProfileRunPersistenceMode.NO_OP,
            scenarioId = id,
            scenarioRouteIndex = runtime.routeIndex,
            scenarioEvidenceSummary = evidenceSummary,
        )
    }

    private fun scenarioZoneRoute(): List<String> =
        if (runtime.zoneId in FOUNDATION_ZONE_ROUTE) {
            FOUNDATION_ZONE_ROUTE
        } else {
            listOf(runtime.zoneId)
        }
}

data class ValidationScenarioRuntimeSpec(
    val preset: ValidationPreset,
    val seed: Long,
    val locale: GameLocale,
    val professionId: String,
    val raceId: String,
    val zoneId: String,
    val floor: Int,
    val routeIndex: Int,
    val contentPackMode: ValidationScenarioContentPackMode,
) {
    init {
        require(professionId.isNotBlank()) { "Validation scenario runtime must declare professionId." }
        require(raceId.isNotBlank()) { "Validation scenario runtime must declare raceId." }
        require(zoneId.isNotBlank()) { "Validation scenario runtime must declare zoneId." }
        require(floor > 0) { "Validation scenario runtime must declare a positive floor." }
        require(routeIndex >= -1) { "Validation scenario runtime routeIndex must be -1 or greater." }
        validatePresetStartZone(preset = preset, zoneId = zoneId)
    }
}

data class ValidationScenarioEvidenceSpec(
    val requiredEvidenceFiles: List<String>,
    val requiredExternalEvidenceFiles: List<String> = emptyList(),
    val cuaSteps: List<ValidationScenarioEvidenceStep>,
    val manualRecordPath: String,
    val requiredLogEventKeys: List<String> = emptyList(),
    val scenarioNoteLabelKey: String? = null,
) {
    val allRequiredEvidenceFiles: List<String>
        get() = requiredEvidenceFiles + requiredExternalEvidenceFiles

    init {
        require(allRequiredEvidenceFiles.count { evidenceFile -> evidenceFile.endsWith(".png") } >= 4) {
            "Validation scenario evidence must require at least four screenshot evidence files."
        }
        require(requiredEvidenceFiles.distinct().size == requiredEvidenceFiles.size) {
            "Validation scenario evidence must not repeat evidence file names."
        }
        require(requiredExternalEvidenceFiles.distinct().size == requiredExternalEvidenceFiles.size) {
            "Validation scenario evidence must not repeat external evidence file names."
        }
        require(allRequiredEvidenceFiles.distinct().size == allRequiredEvidenceFiles.size) {
            "Validation scenario evidence must not repeat evidence file names across packaged and external evidence."
        }
        require(cuaSteps.isNotEmpty()) { "Validation scenario evidence must declare CUA steps." }
        require(cuaSteps.all { step -> step.evidenceFile in allRequiredEvidenceFiles }) {
            "Validation scenario evidence CUA steps must reference required evidence files."
        }
        require(manualRecordPath.isNotBlank()) { "Validation scenario evidence must declare manualRecordPath." }
        require(requiredLogEventKeys.all(String::isNotBlank)) {
            "Validation scenario evidence requiredLogEventKeys must not contain blank entries."
        }
        require(scenarioNoteLabelKey == null || scenarioNoteLabelKey.isNotBlank()) {
            "Validation scenario evidence scenarioNoteLabelKey must not be blank."
        }
    }
}
