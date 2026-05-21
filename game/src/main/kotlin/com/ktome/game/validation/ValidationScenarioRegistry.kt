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
                id = ValidationScenarioId("dark-uiux-pr04-profession-tree-ui"),
                prId = "PR-04",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.MAPGEN_DIFF,
                        seed = 2026051604L,
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
                                "evidence/dark-uiux-pr04-talent-assign-panel-start.png",
                                "evidence/dark-uiux-pr04-talent-assign-min-window-log-visible.png",
                                "evidence/dark-uiux-pr04-right-companion-coexistence.png",
                                "evidence/dark-uiux-pr04-profession-tree-ui-app.log",
                            ),
                        requiredExternalEvidenceFiles =
                            listOf(
                                "client/build/reports/golden/dark-uiux-pr04/dark-uiux-pr04-active-slot-choice.png",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter, Esc, T",
                                    expectedVisibleResult = "Talent Assign panel matches the PR04 reference structure with three expanded vanguard sections, reference state markers, skill icons, current detail, next preview, and bottom legend.",
                                    evidenceFile = "evidence/dark-uiux-pr04-talent-assign-panel-start.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Automated golden or packaged keyboard",
                                    input = "Golden PR04 active-slot fixture; packaged F9, Right, Enter, Esc, T, Enter when local key routing is reliable",
                                    expectedVisibleResult = "ACTIVE_TALENT_SLOT_CHOICE uses the same dark PR04 panel chrome and shows four replacement slots, one reserve row, and Esc cancel; active-slot evidence must not duplicate the panel-start capture.",
                                    evidenceFile = "client/build/reports/golden/dark-uiux-pr04/dark-uiux-pr04-active-slot-choice.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (manual resize from Talent Assign state)",
                                    input = "Resize to minimum supported viewport",
                                    expectedVisibleResult = "Talent Assign compact viewport keeps the selected row, current detail, footer actions, and bottom legend readable without internal overlap.",
                                    evidenceFile = "evidence/dark-uiux-pr04-talent-assign-min-window-log-visible.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (after leaving Talent Assign)",
                                    input = "Esc from Talent Assign, capture shell",
                                    expectedVisibleResult = "PR03 right companion equipment, inventory, status surface, shell hotbar, and bottom log are restored after leaving Talent Assign.",
                                    evidenceFile = "evidence/dark-uiux-pr04-right-companion-coexistence.png",
                                ),
                            ),
                        manualRecordPath = "UI/manual-records/dark-uiux-pr04-profession-tree-ui.md",
                    ),
            ),
            pr0401PassiveScenario(
                id = "dark-uiux-pr04-01-static-passive-detail",
                seed = 202605170401L,
                professionId = "vanguard",
                setup =
                    ValidationScenarioTalentSetupSpec(
                        targetTalentId = "unyielding",
                        initialFocusedTalentId = "unyielding",
                        playerLevel = 5,
                        prerequisiteRanks = mapOf("intimidation" to 2),
                        setUnspentTalentPoints = 1,
                        previewExpanded = false,
                    ),
                requiredEvidenceFiles =
                    listOf(
                        "evidence/passive-static-preview-collapsed-after-toggle.png",
                        "evidence/passive-detail-static.png",
                        "evidence/passive-static-next-preview.png",
                        "evidence/passive-static-after-learn-no-slot-modal.png",
                        "evidence/passive-static-app.log",
                    ),
                cuaSteps =
                    listOf(
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (initial UI mode: MAP; setup focuses unyielding)",
                            input = "F9, Enter, Esc, T",
                            expectedVisibleResult = "`unyielding` row is focused; right detail shows `Passive`, rank `0`, rank 1 max HP, defense, physical resistance, and no active-slot modal.",
                            evidenceFile = "evidence/passive-detail-static.png",
                            expectedFocusedTalentId = "unyielding",
                            typedAssertions =
                                listOf(
                                    FocusedTalentAssertion(talentId = "unyielding", category = "PASSIVE", rank = 0, state = "LEARNABLE"),
                                    PassiveLineAssertion(lineKind = "STAT_MODIFIER", statId = "maxHp", value = "+10", orderIndex = 0),
                                    PassiveLineAssertion(lineKind = "STAT_MODIFIER", statId = "defense", value = "+1", orderIndex = 1),
                                    PassiveLineAssertion(lineKind = "RESISTANCE_BONUS", damageType = "PHYSICAL", value = "+1", orderIndex = 2),
                                ),
                            localizedVisibleAssertions =
                                listOf(
                                    LocalizedTextAssertion(
                                        locale = "ZH_CN",
                                        key = "talent.vanguard.unyielding.name",
                                        visibleTextPolicy = "title-visible",
                                        evidenceFile = "evidence/passive-detail-static.png",
                                    ),
                                ),
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "P",
                            expectedVisibleResult = "Next preview is expanded and lists rank 1 to rank 2 deltas for max HP, defense and physical resistance.",
                            evidenceFile = "evidence/passive-static-next-preview.png",
                            expectedFocusedTalentId = "unyielding",
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "P",
                            expectedVisibleResult = "Preview returns to collapsed state after a prior preview expansion; Talent Assign remains focused on `unyielding` and PASSIVE actions/footer remain free of `R` shortcut affordance.",
                            evidenceFile = "evidence/passive-static-preview-collapsed-after-toggle.png",
                            expectedFocusedTalentId = "unyielding",
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "Enter",
                            expectedVisibleResult = "Learning `unyielding` does not open `ACTIVE_TALENT_SLOT_CHOICE`; learned detail still renders PASSIVE current effects.",
                            evidenceFile = "evidence/passive-static-after-learn-no-slot-modal.png",
                            expectedFocusedTalentId = "unyielding",
                        ),
                    ),
                requiredLogEventKeys = listOf("log.validation.phase4_v4.action", "log.talent.learned"),
                forbiddenLogFragments =
                    listOf("PlayerCommand.RespecTalentTree", "RespecTalentTree", "ConfirmTalentDraftToReserve", "EquipTalentToSlot", "ACTIVE_TALENT_SLOT_CHOICE"),
            ),
            pr0401PassiveScenario(
                id = "dark-uiux-pr04-01-trigger-passive-detail",
                seed = 202605170402L,
                professionId = "arcanist",
                setup =
                    ValidationScenarioTalentSetupSpec(
                        targetTalentId = "mana_surge",
                        initialFocusedTalentId = "mana_surge",
                        playerLevel = 5,
                        prerequisiteRanks = mapOf("arcane_shield" to 2, "blink" to 2),
                        setUnspentTalentPoints = 1,
                        previewExpanded = false,
                    ),
                requiredEvidenceFiles =
                    listOf(
                        "evidence/passive-trigger-panel-entry.png",
                        "evidence/passive-detail-trigger.png",
                        "evidence/passive-cast-speed-effective-detail.png",
                        "evidence/passive-trigger-next-preview.png",
                        "evidence/passive-trigger-after-learn-no-slot-modal.png",
                        "evidence/passive-trigger-app.log",
                    ),
                cuaSteps =
                    listOf(
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (initial UI mode: MAP; setup focuses mana_surge)",
                            input = "F9, Enter, Esc, T",
                            expectedVisibleResult = "`mana_surge` row is focused; right detail shows `Passive`, rank `0`, on-kill MANA restore and FIRE / COLD / LIGHTNING damage lines.",
                            evidenceFile = "evidence/passive-detail-trigger.png",
                            expectedFocusedTalentId = "mana_surge",
                            typedAssertions =
                                listOf(
                                    FocusedTalentAssertion(talentId = "mana_surge", category = "PASSIVE", rank = 0, state = "LEARNABLE"),
                                    PassiveLineAssertion(lineKind = "ON_KILL_RESOURCE_RESTORE", resourceType = "MANA", value = "+3", orderIndex = 0),
                                    PassiveLineAssertion(lineKind = "DAMAGE_TYPE_BONUS", damageType = "FIRE", value = "+3%", orderIndex = 1),
                                    PassiveLineAssertion(lineKind = "DAMAGE_TYPE_BONUS", damageType = "COLD", value = "+3%", orderIndex = 2),
                                    PassiveLineAssertion(lineKind = "DAMAGE_TYPE_BONUS", damageType = "LIGHTNING", value = "+3%", orderIndex = 3),
                                ),
                            localizedVisibleAssertions =
                                listOf(
                                    LocalizedTextAssertion(
                                        locale = "ZH_CN",
                                        key = "talent.arcanist.mana_surge.name",
                                        visibleTextPolicy = "title-visible",
                                        evidenceFile = "evidence/passive-detail-trigger.png",
                                    ),
                                ),
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "P",
                            expectedVisibleResult = "Next preview is expanded and lists MANA restore plus FIRE / COLD / LIGHTNING damage deltas in `DamageType` enum order.",
                            evidenceFile = "evidence/passive-trigger-next-preview.png",
                            expectedFocusedTalentId = "mana_surge",
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "P",
                            expectedVisibleResult = "Preview returns to collapsed state; Talent Assign remains focused on `mana_surge` and no active-slot modal is open.",
                            evidenceFile = "evidence/passive-trigger-panel-entry.png",
                            expectedFocusedTalentId = "mana_surge",
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same tree after trigger preview)",
                            input = "Down",
                            expectedVisibleResult = "`arcane_overload` row is focused; right detail renders cast speed as the §2.5 effective decimal value, not the raw rating.",
                            evidenceFile = "evidence/passive-cast-speed-effective-detail.png",
                            expectedFocusedTalentId = "arcane_overload",
                            typedAssertions =
                                listOf(
                                    FocusedTalentAssertion(talentId = "arcane_overload", category = "PASSIVE", rank = 0, state = "LEARNABLE"),
                                    PassiveLineAssertion(lineKind = "STAT_MODIFIER", statId = "castSpeedRating", value = "+1.0", orderIndex = 0),
                                ),
                            localizedVisibleAssertions =
                                listOf(
                                    LocalizedTextAssertion(
                                        locale = "ZH_CN",
                                        key = "talent.arcanist.arcane_overload.name",
                                        visibleTextPolicy = "title-visible",
                                        evidenceFile = "evidence/passive-cast-speed-effective-detail.png",
                                    ),
                                ),
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (return to trigger row)",
                            input = "Up, Enter",
                            expectedVisibleResult = "Learning `mana_surge` does not open `ACTIVE_TALENT_SLOT_CHOICE`; detail remains a PASSIVE rule summary.",
                            evidenceFile = "evidence/passive-trigger-after-learn-no-slot-modal.png",
                            expectedFocusedTalentId = "mana_surge",
                        ),
                    ),
                requiredLogEventKeys = listOf("log.validation.phase4_v4.action", "log.talent.learned"),
                forbiddenLogFragments =
                    listOf("PlayerCommand.RespecTalentTree", "RespecTalentTree", "ConfirmTalentDraftToReserve", "EquipTalentToSlot", "ACTIVE_TALENT_SLOT_CHOICE"),
            ),
            pr0401PassiveScenario(
                id = "dark-uiux-pr04-01-passive-action-suppression",
                seed = 202605170403L,
                professionId = "vanguard",
                setup =
                    ValidationScenarioTalentSetupSpec(
                        targetTalentId = "bulwark_march",
                        initialFocusedTalentId = "bulwark_march",
                        playerLevel = 4,
                        prerequisiteRanks = mapOf("guard_stance" to 3),
                        setUnspentTalentPoints = 1,
                        previewExpanded = false,
                    ),
                requiredEvidenceFiles =
                    listOf(
                        "evidence/passive-conditional-detail-before-r.png",
                        "evidence/passive-action-preview-expanded.png",
                        "evidence/passive-no-active-slot-modal.png",
                        "evidence/passive-action-log-no-reserve.png",
                        "evidence/passive-action-suppression-app.log",
                    ),
                cuaSteps =
                    listOf(
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (initial UI mode: MAP; setup focuses bulwark_march)",
                            input = "F9, Enter, Esc, T",
                            expectedVisibleResult = "`bulwark_march` row is focused; detail shows `SELF_HAS_STATUS=GUARD_STANCE_BUFF`, defense/speed bonus, and `TAUNT` damage payoff.",
                            evidenceFile = "evidence/passive-conditional-detail-before-r.png",
                            expectedFocusedTalentId = "bulwark_march",
                            typedAssertions =
                                listOf(
                                    FocusedTalentAssertion(talentId = "bulwark_march", category = "PASSIVE", rank = 0, state = "LEARNABLE"),
                                    PassiveLineAssertion(lineKind = "CONDITIONAL_STAT_BONUS", condition = "SELF_HAS_STATUS", statusId = "GUARD_STANCE_BUFF", statId = "defense", value = "+2", orderIndex = 0),
                                    PassiveLineAssertion(lineKind = "CONDITIONAL_STAT_BONUS", condition = "SELF_HAS_STATUS", statusId = "GUARD_STANCE_BUFF", statId = "speed", value = "+1", orderIndex = 1),
                                    PassiveLineAssertion(lineKind = "DAMAGE_VS_STATUS", statusId = "TAUNT", value = "+4%", orderIndex = 2),
                                ),
                            localizedVisibleAssertions =
                                listOf(
                                    LocalizedTextAssertion(
                                        locale = "ZH_CN",
                                        key = "talent.vanguard.bulwark_march.name",
                                        visibleTextPolicy = "title-visible",
                                        evidenceFile = "evidence/passive-conditional-detail-before-r.png",
                                    ),
                                ),
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "P",
                            expectedVisibleResult = "Next preview is expanded and lists guarded defense, speed and `TAUNT` damage deltas.",
                            evidenceFile = "evidence/passive-action-preview-expanded.png",
                            expectedFocusedTalentId = "bulwark_march",
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "R",
                            expectedVisibleResult = "Pressing `R` on PASSIVE does not emit `PlayerCommand.RespecTalentTree`, does not open `ACTIVE_TALENT_SLOT_CHOICE`, does not show footer `RESERVE`, and does not emit reserve/slot command.",
                            evidenceFile = "evidence/passive-no-active-slot-modal.png",
                            expectedFocusedTalentId = "bulwark_march",
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "Esc",
                            expectedVisibleResult = "Returning from Talent Assign restores shell/log state without respec, reserve confirmation, slot replacement or rollback error text.",
                            evidenceFile = "evidence/passive-action-log-no-reserve.png",
                        ),
                    ),
                requiredLogEventKeys = listOf("log.validation.phase4_v4.action"),
                forbiddenLogFragments =
                    listOf(
                        "PlayerCommand.RespecTalentTree",
                        "RespecTalentTree",
                        "ConfirmTalentDraftToReserve",
                        "EquipTalentToSlot",
                        "ACTIVE_TALENT_SLOT_CHOICE",
                        "log.talent.draft_rollback",
                    ),
            ),
            pr0401PassiveScenario(
                id = "dark-uiux-pr04-01-effective-hp-regen-detail",
                seed = 202605170404L,
                professionId = "berserker",
                setup =
                    ValidationScenarioTalentSetupSpec(
                        targetTalentId = "pain_fuel",
                        initialFocusedTalentId = "pain_fuel",
                        playerLevel = 4,
                        prerequisiteRanks = mapOf("kill_frenzy" to 2),
                        setUnspentTalentPoints = 1,
                        previewExpanded = false,
                    ),
                requiredEvidenceFiles =
                    listOf(
                        "evidence/passive-hp-regen-effective-detail.png",
                        "evidence/passive-hp-regen-effective-preview.png",
                        "evidence/passive-hp-regen-preview-collapsed.png",
                        "evidence/passive-hp-regen-after-learn-no-slot-modal.png",
                        "evidence/passive-hp-regen-app.log",
                    ),
                cuaSteps =
                    listOf(
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (initial UI mode: MAP; setup focuses pain_fuel)",
                            input = "F9, Enter, Esc, T",
                            expectedVisibleResult = "`pain_fuel` row is focused; right detail renders hp regen as the §2.5 effective decimal value and does not open an active-slot modal.",
                            evidenceFile = "evidence/passive-hp-regen-effective-detail.png",
                            expectedFocusedTalentId = "pain_fuel",
                            typedAssertions =
                                listOf(
                                    FocusedTalentAssertion(talentId = "pain_fuel", category = "PASSIVE", rank = 0, state = "LEARNABLE"),
                                    PassiveLineAssertion(lineKind = "STAT_MODIFIER", statId = "hpRegen", value = "+0.4", orderIndex = 0),
                                    PassiveLineAssertion(lineKind = "STAT_MODIFIER", statId = "attackMultiplierBonus", value = "+5%", orderIndex = 1),
                                ),
                            localizedVisibleAssertions =
                                listOf(
                                    LocalizedTextAssertion(
                                        locale = "ZH_CN",
                                        key = "talent.berserker.pain_fuel.name",
                                        visibleTextPolicy = "title-visible",
                                        evidenceFile = "evidence/passive-hp-regen-effective-detail.png",
                                    ),
                                ),
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "P",
                            expectedVisibleResult = "Next preview is expanded and lists hp regen plus attack damage deltas using typed passive detail rows.",
                            evidenceFile = "evidence/passive-hp-regen-effective-preview.png",
                            expectedFocusedTalentId = "pain_fuel",
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "P",
                            expectedVisibleResult = "Preview returns to collapsed state after a prior preview expansion; Talent Assign remains focused on `pain_fuel`.",
                            evidenceFile = "evidence/passive-hp-regen-preview-collapsed.png",
                            expectedFocusedTalentId = "pain_fuel",
                        ),
                        ValidationScenarioEvidenceStep(
                            mode = "Keyboard (same focused row)",
                            input = "Enter",
                            expectedVisibleResult = "Learning `pain_fuel` does not open `ACTIVE_TALENT_SLOT_CHOICE`; learned detail remains PASSIVE.",
                            evidenceFile = "evidence/passive-hp-regen-after-learn-no-slot-modal.png",
                            expectedFocusedTalentId = "pain_fuel",
                        ),
                    ),
                requiredLogEventKeys = listOf("log.validation.phase4_v4.action", "log.talent.learned"),
                forbiddenLogFragments =
                    listOf("PlayerCommand.RespecTalentTree", "RespecTalentTree", "ConfirmTalentDraftToReserve", "EquipTalentToSlot", "ACTIVE_TALENT_SLOT_CHOICE"),
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
                id = ValidationScenarioId("dark-uiux-pr02-1-demo-shell-foundation"),
                prId = "PR-02-2",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.LOOT_LAB,
                        seed = 2026051102L,
                        locale = GameLocale.ZH_CN,
                        professionId = "vanguard",
                        raceId = "human",
                        zoneId = "shattered_outpost",
                        floor = 1,
                        routeIndex = -1,
                        contentPackMode = ValidationScenarioContentPackMode.NONE,
                    ),
                evidence =
                    ValidationScenarioEvidenceSpec(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/ui-demo-new-parity-1672x941.png",
                                "evidence/ui-demo-new-parity-1280x800.png",
                                "evidence/ui-demo-new-right-panel-grid.png",
                                "evidence/ui-demo-new-bottom-deck-no-command-hints.png",
                                "evidence/ui-demo-new-inventory-page-1.png",
                                "evidence/ui-demo-new-inventory-page-2.png",
                                "evidence/ui-demo-new-nav-rail-crop.png",
                                "evidence/ui-demo-new-map-stage-crop.png",
                                "evidence/ui-demo-new-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial validation session)",
                                    input = "Capture launch window at 1672x941",
                                    expectedVisibleResult = "UI-demo-new parity shell shows warm dungeon map stage, vanguard actor, ruin tiles, right panel, and bottom hero/action/log deck.",
                                    evidenceFile = "evidence/ui-demo-new-parity-1672x941.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (responsive validation session)",
                                    input = "Capture launch window at 1280x800",
                                    expectedVisibleResult = "Responsive shell keeps the same visual hierarchy, warm palette, and no bottom command hint plate.",
                                    evidenceFile = "evidence/ui-demo-new-parity-1280x800.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "Capture right panel crop",
                                    expectedVisibleResult = "Right panel is ordered equipment, inscriptions, backpack, operation hints with 9 equipment sockets, 5-12 inscription rows, and 4x2 backpack grid.",
                                    evidenceFile = "evidence/ui-demo-new-right-panel-grid.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (supporting crop)",
                                    input = "Capture bottom deck crop",
                                    expectedVisibleResult = "Bottom deck contains hero card, four-slot action deck, and log deck only; command hints are not visible in the bottom region.",
                                    evidenceFile = "evidence/ui-demo-new-bottom-deck-no-command-hints.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "I",
                                    expectedVisibleResult = "Pagination fixture page 1 shows eight fixed slots and a 1/N pager without moving command hints into the bottom deck; the launch parity screenshot remains single-page.",
                                    evidenceFile = "evidence/ui-demo-new-inventory-page-1.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (inventory UI mode)",
                                    input = "PgDn",
                                    expectedVisibleResult = "Backpack page 2 starts at the next page first slot and the modal state remains inside modalSafeBounds.",
                                    evidenceFile = "evidence/ui-demo-new-inventory-page-2.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (supporting crop)",
                                    input = "Capture nav rail crop",
                                    expectedVisibleResult = "Five ui.shell.nav.* icons are visually distinct and the selected state remains bounded in the rail.",
                                    evidenceFile = "evidence/ui-demo-new-nav-rail-crop.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (supporting crop)",
                                    input = "Capture map stage crop",
                                    expectedVisibleResult = "Map stage uses dark-v1 ruin floor/wall, vanguard actor, down-stairs prop, warm fog, and restrained amber edge feather.",
                                    evidenceFile = "evidence/ui-demo-new-map-stage-crop.png",
                                ),
                            ),
                        manualRecordPath = "UI/manual-records/ui-demo-new-visual-parity.md",
                        scenarioNoteLabelKey = "validation.phase4.v4.dark-uiux-pr02-2-ui-demo-new-visual-parity.evidence.summary_note",
                    ),
            ),
            ValidationScenarioDef(
                id = ValidationScenarioId("dark-uiux-pr03-equipment-inventory-items"),
                prId = "PR-03",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.LOOT_LAB,
                        seed = 2026050903L,
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
                                "evidence/dark-uiux-pr03-equipment-slots.png",
                                "evidence/dark-uiux-pr03-inventory-empty.png",
                                "evidence/dark-uiux-pr03-inventory-stacked.png",
                                "evidence/dark-uiux-pr03-inscription-shop.png",
                                "evidence/dark-uiux-pr03-shop-full-slot-replace.png",
                                "evidence/dark-uiux-pr03-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "Capture right equipment panel",
                                    expectedVisibleResult = "WEAPON, OFF_HAND, ARMOR, and ACCESSORY are real typed cells; remaining sockets are visual-only and no ground loot section appears in the right panel.",
                                    evidenceFile = "evidence/dark-uiux-pr03-equipment-slots.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (validation action: emptyInventorySurface)",
                                    input = "Open inventory after empty-inventory setup",
                                    expectedVisibleResult = "Inventory grid shows the dark empty state and does not render placeholder items.",
                                    evidenceFile = "evidence/dark-uiux-pr03-inventory-empty.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (inventory UI mode)",
                                    input = "Move selection across repeated item cells",
                                    expectedVisibleResult = "Repeated healing_potion entries remain separate stable cells; quality frame and badge anchor do not change hitbox identity.",
                                    evidenceFile = "evidence/dark-uiux-pr03-inventory-stacked.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (shop UI mode)",
                                    input = "Navigate buy and sell columns",
                                    expectedVisibleResult = "Shop offers show dark price/affordability markers and inscription marker without encoding purchase rules in the image.",
                                    evidenceFile = "evidence/dark-uiux-pr03-inscription-shop.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (shop replacement prompt)",
                                    input = "Try 1-4, then 5-8, Enter, and Esc",
                                    expectedVisibleResult = "Only hotkeys 5-8 select replacement slots; Enter submits selected hotkey and Esc/Backspace cancel without pre-confirm shard loss.",
                                    evidenceFile = "evidence/dark-uiux-pr03-shop-full-slot-replace.png",
                                ),
                            ),
                        manualRecordPath = "UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md",
                        requiredLogEventKeys =
                            listOf(
                                "log.validation.item.pr03_showcase",
                            ),
                        scenarioNoteLabelKey = "validation.phase4.v4.dark-uiux-pr03-equipment-inventory-items.evidence.summary_note",
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
                id = ValidationScenarioId("dark-uiux-pr05-map-layer-stack"),
                prId = "PR-05",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.CUSTOM,
                        seed = 202605090501L,
                        locale = GameLocale.ZH_CN,
                        professionId = "arcanist",
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
                                "evidence/dark-uiux-pr05-map-layer-stack-primary.png",
                                "evidence/dark-uiux-pr05-map-layer-stack-prop-marker.png",
                                "evidence/dark-uiux-pr05-map-layer-stack-loot-marker.png",
                                "evidence/dark-uiux-pr05-map-layer-stack-report.png",
                                "evidence/dark-uiux-pr05-map-layer-stack-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "Open scenario",
                                    expectedVisibleResult = "Dark PR-05 ground and wall tiles, player actor, and map props render without old black-grid fallback.",
                                    evidenceFile = "evidence/dark-uiux-pr05-map-layer-stack-primary.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "Move focus to a visible interactable prop",
                                    expectedVisibleResult = "Interactable prop art remains readable above the dark ground and below actor sprites.",
                                    evidenceFile = "evidence/dark-uiux-pr05-map-layer-stack-prop-marker.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "Move focus to a loot marker",
                                    expectedVisibleResult = "Ground loot marker and tile content remain readable without covering the actor sprite.",
                                    evidenceFile = "evidence/dark-uiux-pr05-map-layer-stack-loot-marker.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "Open evidence summary",
                                    expectedVisibleResult = "Evidence summary lists PR-05 map layer stack checks and artifact paths.",
                                    evidenceFile = "evidence/dark-uiux-pr05-map-layer-stack-report.png",
                                ),
                            ),
                        manualRecordPath = "UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md",
                        scenarioNoteLabelKey = "validation.phase4.v4.dark-uiux-pr05-map-layer-stack.evidence.summary_note",
                    ),
            ),
            ValidationScenarioDef(
                id = ValidationScenarioId("dark-uiux-pr05-actor-boss-telegraph"),
                prId = "PR-05",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.BOSS_VARIANT,
                        seed = 202605090502L,
                        locale = GameLocale.ZH_CN,
                        professionId = "templar",
                        raceId = "human",
                        zoneId = "grey_gate_depths",
                        floor = 2,
                        routeIndex = -1,
                        contentPackMode = ValidationScenarioContentPackMode.NONE,
                    ),
                evidence =
                    ValidationScenarioEvidenceSpec(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/dark-uiux-pr05-actor-boss-telegraph-primary.png",
                                "evidence/dark-uiux-pr05-actor-boss-telegraph-grey-crown.png",
                                "evidence/dark-uiux-pr05-actor-boss-telegraph-abyssal-eclipse.png",
                                "evidence/dark-uiux-pr05-actor-boss-telegraph-report.png",
                                "evidence/dark-uiux-pr05-actor-boss-telegraph-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Enter",
                                    expectedVisibleResult = "Boss actor, ordinary VFX, and molten_glass boss warning remain readable in the dark PR-05 map surface.",
                                    evidenceFile = "evidence/dark-uiux-pr05-actor-boss-telegraph-primary.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter",
                                    expectedVisibleResult = "grey_crown warning overlaps the boss actor without hiding actor silhouette or warning ring.",
                                    evidenceFile = "evidence/dark-uiux-pr05-actor-boss-telegraph-grey-crown.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Enter",
                                    expectedVisibleResult = "abyssal_eclipse warning and ordinary VFX remain separable from the boss actor on dark tiles.",
                                    evidenceFile = "evidence/dark-uiux-pr05-actor-boss-telegraph-abyssal-eclipse.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "F9, Right, Right, Enter",
                                    expectedVisibleResult = "Evidence summary lists the PR-05 boss telegraph screenshots and phase override coverage.",
                                    evidenceFile = "evidence/dark-uiux-pr05-actor-boss-telegraph-report.png",
                                ),
                            ),
                        manualRecordPath = "UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md",
                        requiredLogEventKeys =
                            listOf(
                                "log.boss.phase_override_entered",
                                "boss.variant.molten_glass.phase_override.entered",
                                "boss.variant.grey_crown.phase_override.entered",
                                "boss.variant.abyssal_eclipse.phase_override.entered",
                            ),
                        scenarioNoteLabelKey = "validation.phase4.v4.dark-uiux-pr05-actor-boss-telegraph.evidence.summary_note",
                    ),
            ),
            ValidationScenarioDef(
                id = ValidationScenarioId("dark-uiux-pr05-1-inventory-page-workbench"),
                prId = "PR-05-1",
                runtime =
                    ValidationScenarioRuntimeSpec(
                        preset = ValidationPreset.LOOT_LAB,
                        seed = 20260521L,
                        locale = GameLocale.ZH_CN,
                        professionId = "vanguard",
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
                                "evidence/dark-uiux-pr05-1-inventory-workbench-open.png",
                                "evidence/dark-uiux-pr05-1-inventory-compare-selection.png",
                                "evidence/dark-uiux-pr05-1-inventory-consumable-selection.png",
                                "evidence/dark-uiux-pr05-1-inventory-empty-cell-selection.png",
                                "evidence/dark-uiux-pr05-1-inventory-pagination-page-two.png",
                                "evidence/dark-uiux-pr05-1-inventory-min-window-1024x768.png",
                                "evidence/dark-uiux-pr05-1-inventory-escape-return-map.png",
                                "evidence/dark-uiux-pr05-1-inventory-page-workbench-app.log",
                            ),
                        cuaSteps =
                            listOf(
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (initial UI mode: MAP)",
                                    input = "I",
                                    expectedVisibleResult = "Full-page inventory workbench opens with 9-slot visual equipment area, 6x4 backpack grid, selected item detail, typed compare/action rows, footer hints, and low-light shell context.",
                                    evidenceFile = "evidence/dark-uiux-pr05-1-inventory-workbench-open.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (inventory UI mode)",
                                    input = "Right, Enter",
                                    expectedVisibleResult = "Committed selection moves to an equippable item and the detail pane shows the current equipment compare cue from typed item data.",
                                    evidenceFile = "evidence/dark-uiux-pr05-1-inventory-compare-selection.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (inventory UI mode)",
                                    input = "Right, Right, Enter",
                                    expectedVisibleResult = "Committed selection moves to a consumable or material-like item without changing the workbench layout or showing fake equip-only compare rows.",
                                    evidenceFile = "evidence/dark-uiux-pr05-1-inventory-consumable-selection.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (inventory UI mode)",
                                    input = "PgDn",
                                    expectedVisibleResult = "Empty cell selection keeps the 6x4 grid stable and shows an empty selection/action state without placeholder item text.",
                                    evidenceFile = "evidence/dark-uiux-pr05-1-inventory-empty-cell-selection.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (inventory UI mode)",
                                    input = "Left, Left, Left, Enter",
                                    expectedVisibleResult = "Inventory remains on page 2 with a committed item selection while footer/page/capacity text stays visible.",
                                    evidenceFile = "evidence/dark-uiux-pr05-1-inventory-pagination-page-two.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Manual resize",
                                    input = "Resize packaged window to 1024x768",
                                    expectedVisibleResult = "All three workbench columns, 6x4 grid, selected detail, and footer remain readable without major overlap.",
                                    evidenceFile = "evidence/dark-uiux-pr05-1-inventory-min-window-1024x768.png",
                                ),
                                ValidationScenarioEvidenceStep(
                                    mode = "Keyboard (inventory UI mode)",
                                    input = "Esc",
                                    expectedVisibleResult = "Inventory workbench closes and returns to the in-run map shell.",
                                    evidenceFile = "evidence/dark-uiux-pr05-1-inventory-escape-return-map.png",
                                ),
                            ),
                        manualRecordPath = "UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md",
                        scenarioNoteLabelKey = "validation.phase4.v4.dark-uiux-pr05-1-inventory-page-workbench.evidence.summary_note",
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

    private fun pr0401PassiveScenario(
        id: String,
        seed: Long,
        professionId: String,
        setup: ValidationScenarioTalentSetupSpec,
        requiredEvidenceFiles: List<String>,
        cuaSteps: List<ValidationScenarioEvidenceStep>,
        requiredLogEventKeys: List<String>,
        forbiddenLogFragments: List<String>,
    ): ValidationScenarioDef =
        ValidationScenarioDef(
            id = ValidationScenarioId(id),
            prId = "PR-04-01",
            runtime =
                ValidationScenarioRuntimeSpec(
                    preset = ValidationPreset.MAPGEN_DIFF,
                    seed = seed,
                    locale = GameLocale.ZH_CN,
                    professionId = professionId,
                    raceId = "human",
                    zoneId = "greenwood_fringe",
                    floor = 2,
                    routeIndex = -1,
                    contentPackMode = ValidationScenarioContentPackMode.NONE,
                ),
            evidence =
                ValidationScenarioEvidenceSpec(
                    requiredEvidenceFiles = requiredEvidenceFiles,
                    cuaSteps = cuaSteps,
                    manualRecordPath = "UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md",
                    requiredLogEventKeys = requiredLogEventKeys,
                    forbiddenLogFragments = forbiddenLogFragments,
                ),
            talentSetup = setup,
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
