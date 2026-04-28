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
            ValidationScenarioDef(
                id = ValidationScenarioId("phase4-v4-pr01"),
                prId = "PR-01",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.MAPGEN_DIFF,
                        seed = 2026042431L,
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
                                "evidence/phase4-v4-pr01-talent-tree-start.png",
                                "evidence/phase4-v4-pr01-learnable-confirm.png",
                                "evidence/phase4-v4-pr01-tier3-locked-reason.png",
                                "evidence/phase4-v4-pr01-reserve-active-slot.png",
                                "evidence/phase4-v4-pr01-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter, Esc, T",
                                    expectedVisibleResult = "Capture after T: Talent tree shows three vanguard starter talents, one empty active slot, learnable non-starter nodes, and locked higher-tier nodes",
                                    evidenceFile = "evidence/phase4-v4-pr01-talent-tree-start.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: TALENT_ASSIGN from talent-tree-start)",
                                    input = "Down, Enter, Enter",
                                    expectedVisibleResult = "Capture after the second Enter: Charge consumes one point after confirm and binds to the fourth active slot",
                                    evidenceFile = "evidence/phase4-v4-pr01-learnable-confirm.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: TALENT_ASSIGN from learnable-confirm)",
                                    input = "Down, Down, Down",
                                    expectedVisibleResult = "Capture after the third Down: Linebreaker remains visible with concrete lock reasons",
                                    evidenceFile = "evidence/phase4-v4-pr01-tier3-locked-reason.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter, Esc, T, Enter",
                                    expectedVisibleResult = "Capture after the final Enter: ACTIVE_TALENT_SLOT_CHOICE is visible with 1-4 replacement options, R reserve, and Esc cancel; Charge remains in slot 4",
                                    evidenceFile = "evidence/phase4-v4-pr01-reserve-active-slot.png",
                                ),
                            ),
                        manualRecordPath = "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md",
                        requiredLogEventKeys =
                            listOf(
                                "log.talent.learned",
                                "log.talent.rank_up",
                                "log.talent.breakpoint_chosen",
                            ),
                    ),
            ),
            ValidationScenarioDef(
                id = ValidationScenarioId("phase4-v4-pr02"),
                prId = "PR-02",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.LOOT_LAB,
                        seed = 2026042432L,
                        locale = GameLocale.ZH_CN,
                        professionId = "rogue",
                        raceId = "human",
                        zoneId = "greenwood_fringe",
                        floor = 1,
                        routeIndex = -1,
                        contentPackMode = ValidationScenarioContentPackMode.NONE,
                    ),
                evidence =
                    ValidationScenarioEvidenceSpec(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/phase4-v4-pr02-start-inscriptions.png",
                                "evidence/phase4-v4-pr02-install-third-slot.png",
                                "evidence/phase4-v4-pr02-replacement-modal.png",
                                "evidence/phase4-v4-pr02-replace-keep-hotkey.png",
                                "evidence/phase4-v4-pr02-reject-no-shard-loss.png",
                                "evidence/phase4-v4-pr02-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter",
                                    expectedVisibleResult = "Rogue starts with exactly two inscriptions and a nearby shop with purchasable inscription offers.",
                                    evidenceFile = "evidence/phase4-v4-pr02-start-inscriptions.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: SHOP)",
                                    input = "Down, Down, Enter",
                                    expectedVisibleResult = "A third inscription installs directly, spends shards once, and keeps hotkeys consecutive from 5.",
                                    evidenceFile = "evidence/phase4-v4-pr02-install-third-slot.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter, Down, Down, Enter",
                                    expectedVisibleResult = "Four filled slots open the replacement prompt with candidate, current slots, category deltas, price, and hotkeys 5-8.",
                                    evidenceFile = "evidence/phase4-v4-pr02-replacement-modal.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: SHOP replacement prompt)",
                                    input = "5, Enter",
                                    expectedVisibleResult = "Replacement succeeds, keeps the selected hotkey, writes log.shop.inscription.replaced, and plays equip-changed audio.",
                                    evidenceFile = "evidence/phase4-v4-pr02-replace-keep-hotkey.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter, Down, Down, Enter, 7, Enter",
                                    expectedVisibleResult = "Rejected replacement leaves shards and loadout unchanged.",
                                    evidenceFile = "evidence/phase4-v4-pr02-reject-no-shard-loss.png",
                                ),
                            ),
                        manualRecordPath = "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr02-inscription-shop-replacement.md",
                        requiredLogEventKeys =
                            listOf(
                                "shop.purchase.requires_replacement_target",
                                "log.shop.inscription.replaced",
                            ),
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
