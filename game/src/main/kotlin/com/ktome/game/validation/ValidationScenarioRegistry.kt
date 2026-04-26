package com.ktome.game.validation

import com.ktome.game.i18n.GameLocale
import java.nio.file.Files
import java.nio.file.Path
import org.yaml.snakeyaml.Yaml

object ValidationScenarioRegistry {
    private val scenarios: List<ValidationScenarioDef> =
        listOf(
            ValidationScenarioDef(
                id = ValidationScenarioId("phase4-v4-pr00-selftest"),
                prId = "PR-00",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.MAPGEN_DIFF,
                        seed = 2026042430L,
                        locale = GameLocale.ZH_CN,
                        professionId = "vanguard",
                        raceId = "human",
                        zoneId = "greenwood_fringe",
                        floor = 2,
                        routeIndex = -1,
                        contentPackMode = ValidationScenarioContentPackMode.NONE,
                    ),
                evidence =
                    ValidationScenarioEvidenceSpec(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/phase4-v4-pr00-scenario-bootstrap.png",
                                "evidence/phase4-v4-pr00-primary-scene.png",
                                "evidence/phase4-v4-pr00-secondary-scene.png",
                                "evidence/phase4-v4-pr00-evidence-summary.png",
                                "evidence/app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard",
                                    input = "F9",
                                    expectedVisibleResult = "PHASE4_V4_FAST section is selected",
                                    evidenceFile = "evidence/phase4-v4-pr00-scenario-bootstrap.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard",
                                    input = "Enter",
                                    expectedVisibleResult = "Primary scenario action returns ok",
                                    evidenceFile = "evidence/phase4-v4-pr00-primary-scene.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard",
                                    input = "Right, Enter",
                                    expectedVisibleResult = "Secondary scenario action returns ok",
                                    evidenceFile = "evidence/phase4-v4-pr00-secondary-scene.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard",
                                    input = "Right, Enter",
                                    expectedVisibleResult = "Evidence summary shows expected paths, freshness, and app hash",
                                    evidenceFile = "evidence/phase4-v4-pr00-evidence-summary.png",
                                ),
                            ),
                        manualRecordPath = "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr00-selftest.md",
                    ),
            ),
        )

    fun all(): List<ValidationScenarioDef> = scenarios

    fun knownIds(): List<String> = scenarios.map { scenario -> scenario.id.value }

    fun find(id: ValidationScenarioId): ValidationScenarioDef? =
        scenarios.firstOrNull { scenario -> scenario.id == id }

    fun require(id: ValidationScenarioId): ValidationScenarioDef =
        find(id)
            ?: throw UnknownValidationScenarioException(
                scenarioId = id.value,
                knownScenarioIds = knownIds(),
                startupProperties = emptyMap(),
            )

    fun require(
        scenarioId: String,
        startupProperties: Map<String, String?>,
    ): ValidationScenarioDef =
        find(ValidationScenarioId(scenarioId))
            ?: throw UnknownValidationScenarioException(
                scenarioId = scenarioId,
                knownScenarioIds = knownIds(),
                startupProperties = startupProperties,
            )

    fun validateYamlParity(yamlPath: Path): ValidationScenarioYamlParity {
        val yamlIds = loadYamlScenarioIds(yamlPath)
        val kotlinIds = knownIds()
        return ValidationScenarioYamlParity(
            kotlinIds = kotlinIds,
            yamlIds = yamlIds,
            missingFromYaml = kotlinIds.filterNot(yamlIds::contains),
            missingFromKotlin = yamlIds.filterNot(kotlinIds::contains),
        )
    }

    private fun loadYamlScenarioIds(yamlPath: Path): List<String> {
        require(Files.isRegularFile(yamlPath)) { "Scenario yaml does not exist: $yamlPath" }
        val root = Yaml().load<Map<String, Any?>>(Files.readString(yamlPath))
        val rawScenarios = root["scenarios"] as? List<*>
            ?: throw IllegalArgumentException("Scenario yaml must contain a scenarios list.")
        return rawScenarios.map { rawScenario ->
            val scenario = rawScenario as? Map<*, *>
                ?: throw IllegalArgumentException("Scenario yaml entry must be an object.")
            val unexpectedKeys = scenario.keys.filterNot { key -> key == "id" }
            require(unexpectedKeys.isEmpty()) {
                "Scenario yaml entry may only declare id; unexpected keys: ${unexpectedKeys.joinToString(", ")}."
            }
            val id = scenario["id"] as? String
                ?: throw IllegalArgumentException("Scenario yaml entry must contain an id.")
            id
        }
    }
}

data class ValidationScenarioYamlParity(
    val kotlinIds: List<String>,
    val yamlIds: List<String>,
    val missingFromYaml: List<String>,
    val missingFromKotlin: List<String>,
) {
    val isValid: Boolean
        get() = missingFromYaml.isEmpty() && missingFromKotlin.isEmpty()
}

class UnknownValidationScenarioException(
    val scenarioId: String,
    val knownScenarioIds: List<String>,
    val startupProperties: Map<String, String?>,
) : IllegalArgumentException(
        "Unknown validation scenario '$scenarioId'. Known scenario ids: ${knownScenarioIds.joinToString(", ")}. Startup properties: " +
            startupProperties.entries.joinToString(", ") { (key, value) -> "$key=${value ?: "<unset>"}" },
    )
