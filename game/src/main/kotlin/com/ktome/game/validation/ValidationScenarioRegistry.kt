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
            ValidationScenarioDef(
                id = ValidationScenarioId("dark-uiux-pr02-ui-chrome-sprite-pilot"),
                prId = "PR-02",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.LOOT_LAB,
                        seed = 2026051002L,
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
                                "evidence/dark-uiux-pr02-shell-hud-frame-fit.png",
                                "evidence/dark-uiux-pr02-inventory-modal-frame-fit.png",
                                "evidence/dark-uiux-pr02-validation-overlay-frame-fit.png",
                                "evidence/dark-uiux-pr02-runtime-error-loading-fit.png",
                                "evidence/dark-uiux-pr02-ui-chrome-sprite-pilot-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial validation session)",
                                    input = "Capture launch window",
                                    expectedVisibleResult = "Shell, left/right rails, log card, focus card, hotbar, and footer text are inside PR-02 chrome frame content bounds.",
                                    evidenceFile = "evidence/dark-uiux-pr02-shell-hud-frame-fit.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "I",
                                    expectedVisibleResult = "Inventory modal and slot chrome use PR-02 frame assets, and modal text remains inside the content inset.",
                                    evidenceFile = "evidence/dark-uiux-pr02-inventory-modal-frame-fit.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9",
                                    expectedVisibleResult = "Validation overlay text and action chrome remain bounded; overlay does not cover bottom HUD text.",
                                    evidenceFile = "evidence/dark-uiux-pr02-validation-overlay-frame-fit.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (supplemental packaged run)",
                                    input = "Launch normal app or runtime error/loading surface",
                                    expectedVisibleResult = "Standalone loading/error or equivalent runtime surface uses PR-02 frame inset slots without text on chrome borders.",
                                    evidenceFile = "evidence/dark-uiux-pr02-runtime-error-loading-fit.png",
                                ),
                            ),
                        manualRecordPath = "UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md",
                        scenarioNoteLabelKey = "validation.phase4.v4.dark-uiux-pr02-ui-chrome-sprite-pilot.evidence.summary_note",
                    ),
            ),
            ValidationScenarioDef(
                id = ValidationScenarioId("phase4-v4-pr03"),
                prId = "PR-03",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.LOOT_LAB,
                        seed = 2026042433L,
                        locale = GameLocale.ZH_CN,
                        professionId = "arcanist",
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
                                "evidence/phase4-v4-pr03-arcanist-reward-card.png",
                                "evidence/phase4-v4-pr03-arcanist-adopted-nonweapon.png",
                                "evidence/phase4-v4-pr03-rogue-offhand-payoff.png",
                                "evidence/phase4-v4-pr03-report-no-approved-debt.png",
                                "evidence/phase4-v4-pr03-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter",
                                    expectedVisibleResult = "Arcanist reward card uses existing capstone or non-weapon payoff item and explains slot, profession identity, and score reason.",
                                    evidenceFile = "evidence/phase4-v4-pr03-arcanist-reward-card.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "I",
                                    expectedVisibleResult = "Equipment or inventory surface shows the adopted arcanist non-weapon payoff instead of default vanguard armor.",
                                    evidenceFile = "evidence/phase4-v4-pr03-arcanist-adopted-nonweapon.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter",
                                    expectedVisibleResult = "Secondary scene materializes a rogue OFF_HAND payoff such as artifact_briar_heart through the game validation action.",
                                    evidenceFile = "evidence/phase4-v4-pr03-rogue-offhand-payoff.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Right, Enter",
                                    expectedVisibleResult = "Evidence summary confirms professionCapstoneAdoptionFloor.reportOnly and nonWeaponBuildPayoffFloor.reportOnly are no longer approved debt.",
                                    evidenceFile = "evidence/phase4-v4-pr03-report-no-approved-debt.png",
                                ),
                            ),
                        manualRecordPath = "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr03-build-identity-reward-adoption.md",
                        requiredLogEventKeys =
                            listOf(
                                "log.validation.item.pr03_showcase",
                            ),
                        scenarioNoteLabelKey = "validation.phase4.v4.phase4-v4-pr03.evidence.summary_note",
                    ),
            ),
            ValidationScenarioDef(
                id = ValidationScenarioId("phase4-v4-pr04"),
                prId = "PR-04",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.HIDDEN_CONTENT,
                        seed = 2026042434L,
                        locale = GameLocale.ZH_CN,
                        professionId = "arcanist",
                        raceId = "human",
                        zoneId = "deep_iron_pit",
                        floor = 1,
                        routeIndex = -1,
                        contentPackMode = ValidationScenarioContentPackMode.NONE,
                    ),
                evidence =
                    ValidationScenarioEvidenceSpec(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/phase4-v4-pr04-deep-iron-search-cue.png",
                                "evidence/phase4-v4-pr04-search-result-feedback.png",
                                "evidence/phase4-v4-pr04-abyssal-void-pressure.png",
                                "evidence/phase4-v4-pr04-zone-hook-triggered.png",
                                "evidence/phase4-v4-pr04-priority-no-overlap.png",
                                "evidence/phase4-v4-pr04-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter",
                                    expectedVisibleResult = "Player is positioned at the deep_iron_pit slag Search cue; search_available frontstage cue is visible.",
                                    evidenceFile = "evidence/phase4-v4-pr04-deep-iron-search-cue.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "S",
                                    expectedVisibleResult = "Formal Search action produces clue/cache/stash feedback and keeps the hidden entry on the typed snapshot path.",
                                    evidenceFile = "evidence/phase4-v4-pr04-search-result-feedback.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter",
                                    expectedVisibleResult = "Secondary scene switches to abyssal_temple near the ward objective and emits void_pressure runtime warning.",
                                    evidenceFile = "evidence/phase4-v4-pr04-abyssal-void-pressure.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter",
                                    expectedVisibleResult = "zone_hook_triggered cue is visible from a real runtime hook, not an overlay-only label.",
                                    evidenceFile = "evidence/phase4-v4-pr04-zone-hook-triggered.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Right, Enter",
                                    expectedVisibleResult = "Evidence summary lists hidden Search cue, Search result, zone hook, and priority no-overlap evidence.",
                                    evidenceFile = "evidence/phase4-v4-pr04-priority-no-overlap.png",
                                ),
                            ),
                        manualRecordPath = "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr04-hidden-search-zone-hooks.md",
                        requiredLogEventKeys =
                            listOf(
                                "log.search.available",
                                "log.search.revealed_tag",
                                "log.zone.hook.void_pressure",
                                "zone.trigger.void_pressure_active",
                            ),
                        scenarioNoteLabelKey = "validation.phase4.v4.phase4-v4-pr04.evidence.summary_note",
                    ),
            ),
            ValidationScenarioDef(
                id = ValidationScenarioId("phase4-v4-pr05"),
                prId = "PR-05",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.BOSS_VARIANT,
                        seed = 2026042435L,
                        locale = GameLocale.ZH_CN,
                        professionId = "vanguard",
                        raceId = "human",
                        zoneId = "deep_iron_pit",
                        floor = 2,
                        routeIndex = -1,
                        contentPackMode = ValidationScenarioContentPackMode.NONE,
                    ),
                evidence =
                    ValidationScenarioEvidenceSpec(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/phase4-v4-pr05-molten-glass-warning.png",
                                "evidence/phase4-v4-pr05-grey-crown-warning.png",
                                "evidence/phase4-v4-pr05-abyssal-eclipse-warning.png",
                                "evidence/phase4-v4-pr05-report-coverage.png",
                                "evidence/phase4-v4-pr05-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter",
                                    expectedVisibleResult = "Primary scene materializes molten_glass, triggers molten_glass_phase_override_warning, and logs phase override entry.",
                                    evidenceFile = "evidence/phase4-v4-pr05-molten-glass-warning.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter",
                                    expectedVisibleResult = "Secondary scene first materializes grey_crown and shows grey_crown_phase_override_warning with battlefield_command or ritual_break emphasis.",
                                    evidenceFile = "evidence/phase4-v4-pr05-grey-crown-warning.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter",
                                    expectedVisibleResult = "Secondary scene next materializes abyssal_eclipse and shows abyssal_eclipse_phase_override_warning with void_breach or abyssal_consecration emphasis.",
                                    evidenceFile = "evidence/phase4-v4-pr05-abyssal-eclipse-warning.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Right, Enter",
                                    expectedVisibleResult = "Evidence summary lists bossVariantPhaseOverride coverage metrics and phaseGraphUnchangedReason=data_level_override_only.",
                                    evidenceFile = "evidence/phase4-v4-pr05-report-coverage.png",
                                ),
                            ),
                        manualRecordPath = "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr05-boss-variant-phase-language.md",
                        requiredLogEventKeys =
                            listOf(
                                "log.boss.phase_override_entered",
                                "boss.variant.molten_glass.phase_override.entered",
                                "boss.variant.grey_crown.phase_override.entered",
                                "boss.variant.abyssal_eclipse.phase_override.entered",
                            ),
                        scenarioNoteLabelKey = "validation.phase4.v4.phase4-v4-pr05.evidence.summary_note",
                    ),
            ),
            ValidationScenarioDef(
                id = ValidationScenarioId("phase4-v4-pr06"),
                prId = "PR-06",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.MAPGEN_DIFF,
                        seed = 2026042436L,
                        locale = GameLocale.ZH_CN,
                        professionId = "rogue",
                        raceId = "human",
                        zoneId = "greenwood_fringe",
                        floor = 1,
                        routeIndex = 0,
                        contentPackMode = ValidationScenarioContentPackMode.NONE,
                    ),
                evidence =
                    ValidationScenarioEvidenceSpec(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/phase4-v4-pr06-scenario-distribution.png",
                                "evidence/phase4-v4-pr06-route-hash-diversity.png",
                                "evidence/phase4-v4-pr06-branch-inclusive-routes.png",
                                "evidence/phase4-v4-pr06-verifychanged-routing.png",
                                "evidence/phase4-v4-pr06-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter",
                                    expectedVisibleResult = "Route diversity summary shows scenarioTypeDistribution with full_route=12, branch_inclusive=4, route_probe=2, and late_route_probe=2.",
                                    evidenceFile = "evidence/phase4-v4-pr06-scenario-distribution.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter",
                                    expectedVisibleResult = "Route diversity summary shows zoneRouteHashDistribution and zoneRouteHashDiversity.topHashShare <= 40%.",
                                    evidenceFile = "evidence/phase4-v4-pr06-route-hash-diversity.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter",
                                    expectedVisibleResult = "Route diversity summary shows grouped full-route and branch-inclusive routeToken samples with distinct mandatory and secret combinations.",
                                    evidenceFile = "evidence/phase4-v4-pr06-branch-inclusive-routes.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter",
                                    expectedVisibleResult = "VerifyChanged routing summary shows route-hash, loot, and item owner surfaces include :game:longRunLab while TalentSidebarPresenter stays presentation-only.",
                                    evidenceFile = "evidence/phase4-v4-pr06-verifychanged-routing.png",
                                ),
                            ),
                        manualRecordPath = "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr06-long-run-route-diversity.md",
                        requiredLogEventKeys =
                            listOf(
                                "log.validation.phase4_v4.action",
                            ),
                        scenarioNoteLabelKey = "validation.phase4.v4.phase4-v4-pr06.evidence.summary_note",
                    ),
            ),
            ValidationScenarioDef(
                id = ValidationScenarioId("phase4-v4-pr07"),
                prId = "PR-07",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.CONTENT_PACK,
                        seed = 2026042437L,
                        locale = GameLocale.ZH_CN,
                        professionId = "arcanist",
                        raceId = "human",
                        zoneId = "underground_river",
                        floor = 1,
                        routeIndex = -1,
                        contentPackMode = ValidationScenarioContentPackMode.SAMPLE_PACK_ENABLED,
                    ),
                evidence =
                    ValidationScenarioEvidenceSpec(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/phase4-v4-pr07-active-sample-pack-summary.png",
                                "evidence/phase4-v4-pr07-no-pack-empty-state.png",
                                "evidence/phase4-v4-pr07-sample-secret-touch.png",
                                "evidence/phase4-v4-pr07-touched-content-ids.png",
                                "evidence/phase4-v4-pr07-key-resolution-warning.png",
                                "evidence/phase4-v4-pr07-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9",
                                    expectedVisibleResult = "Validation overlay shows active pack id, namespace, ADD-only op summary, touched content ids empty state, and key resolution status.",
                                    evidenceFile = "evidence/phase4-v4-pr07-active-sample-pack-summary.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter",
                                    expectedVisibleResult = "prepare-primary-scene result exposes no-pack empty state and active sample pack summary from validation snapshot data.",
                                    evidenceFile = "evidence/phase4-v4-pr07-no-pack-empty-state.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter, R, Enter",
                                    expectedVisibleResult = "prepare-secondary-scene places the player on sample.flooded_relics.search.flooded_reliquary; formal R/Enter Search/Interact touches the sample secret content.",
                                    evidenceFile = "evidence/phase4-v4-pr07-sample-secret-touch.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9",
                                    expectedVisibleResult = "Overlay touched content ids contain sample.flooded_relics namespaced secret, hidden event, or item ids after runtime interaction.",
                                    evidenceFile = "evidence/phase4-v4-pr07-touched-content-ids.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Right, Enter",
                                    expectedVisibleResult = "Evidence summary lists pack-local key resolution status as UI-only warning surface; runtime rules remain unchanged.",
                                    evidenceFile = "evidence/phase4-v4-pr07-key-resolution-warning.png",
                                ),
                            ),
                        manualRecordPath = "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr07-sample-pack-add-first-visibility.md",
                        requiredLogEventKeys =
                            listOf(
                                "log.validation.phase4_v4.action",
                            ),
                        scenarioNoteLabelKey = "validation.phase4.v4.phase4-v4-pr07.evidence.summary_note",
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
