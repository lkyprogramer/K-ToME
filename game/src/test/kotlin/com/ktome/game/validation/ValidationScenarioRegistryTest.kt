package com.ktome.game.validation

import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.TalentNodeStateSnapshot
import com.ktome.core.world.solvability.SearchBindingId
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.contentpack.ContentPackFixtureCatalog
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.i18n.GameLocale
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ValidationScenarioRegistryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `pr00 selftest scenario matches fixed phase4 v4 contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))

        assertEquals("PR-00", scenario.prId)
        assertEquals(ValidationPreset.MAPGEN_DIFF, scenario.runtime.preset)
        assertEquals(2026042430L, scenario.runtime.seed)
        assertEquals("vanguard", scenario.runtime.professionId)
        assertEquals("human", scenario.runtime.raceId)
        assertEquals("greenwood_fringe", scenario.runtime.zoneId)
        assertEquals(2, scenario.runtime.floor)
        assertEquals(-1, scenario.runtime.routeIndex)
        assertEquals(ValidationScenarioContentPackMode.NONE, scenario.runtime.contentPackMode)
        assertTrue("evidence/phase4-v4-pr00-scenario-bootstrap.png" in scenario.evidence.requiredEvidenceFiles)
        assertEquals(
            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr00-selftest.md",
            scenario.evidence.manualRecordPath,
        )
        assertEquals(
            scenario.evidence.requiredEvidenceFiles.count { file -> file.endsWith(".png") },
            scenario.evidence.cuaSteps.size,
        )
    }

    @Test
    fun `pr01 profession tree scenario matches fixed phase4 v4 contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr01"))

        assertEquals("PR-01", scenario.prId)
        assertEquals(ValidationPreset.MAPGEN_DIFF, scenario.runtime.preset)
        assertEquals(2026042431L, scenario.runtime.seed)
        assertEquals("vanguard", scenario.runtime.professionId)
        assertEquals("human", scenario.runtime.raceId)
        assertEquals("greenwood_fringe", scenario.runtime.zoneId)
        assertEquals(2, scenario.runtime.floor)
        assertEquals(-1, scenario.runtime.routeIndex)
        assertEquals(ValidationScenarioContentPackMode.NONE, scenario.runtime.contentPackMode)
        assertTrue("evidence/phase4-v4-pr01-talent-tree-start.png" in scenario.evidence.requiredEvidenceFiles)
        assertTrue("evidence/phase4-v4-pr01-reserve-active-slot.png" in scenario.evidence.requiredEvidenceFiles)
        assertTrue("evidence/phase4-v4-pr01-app.log" in scenario.evidence.requiredEvidenceFiles)
        assertEquals(
            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md",
            scenario.evidence.manualRecordPath,
        )
        assertEquals(4, scenario.evidence.requiredEvidenceFiles.count { file -> file.endsWith(".png") })
        assertEquals(
            listOf("log.talent.learned", "log.talent.rank_up", "log.talent.breakpoint_chosen"),
            scenario.evidence.requiredLogEventKeys,
        )
        assertEquals(
            scenario.evidence.requiredEvidenceFiles.filter { file -> file.endsWith(".png") },
            scenario.evidence.cuaSteps.map { step -> step.evidenceFile },
        )
        val runbookText = scenario.evidence.cuaSteps.joinToString("\n") { step -> "${step.mode} ${step.input} ${step.expectedVisibleResult}" }
        listOf("select another", "Select a", "Move to", "arcanist").forEach { vagueTerm ->
            assertFalse(runbookText.contains(vagueTerm), "PR-01 runbook must not contain vague or unrelated term: $vagueTerm")
        }
        val reserveStep = scenario.evidence.cuaSteps.first { step -> step.evidenceFile.endsWith("phase4-v4-pr01-reserve-active-slot.png") }
        assertEquals("Keyboard (initial UI mode: MAP)", reserveStep.mode)
        assertEquals("F9, Right, Enter, Esc, T, Enter", reserveStep.input)
        assertTrue(reserveStep.expectedVisibleResult.contains("ACTIVE_TALENT_SLOT_CHOICE"))
    }

    @Test
    fun `pr02 inscription replacement scenario uses exact keyboard evidence inputs`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr02"))

        assertEquals(
            listOf(
                "F9, Enter",
                "Down, Down, Enter",
                "F9, Right, Enter, Down, Down, Enter",
                "5, Enter",
                "F9, Right, Enter, Down, Down, Enter, 7, Enter",
            ),
            scenario.evidence.cuaSteps.map { step -> step.input },
        )
        val runbookText = scenario.evidence.cuaSteps.joinToString("\n") { step -> "${step.mode} ${step.input} ${step.expectedVisibleResult}" }
        listOf("Select inscription offer", "buy inscription", "5-8, Enter", "retry").forEach { vagueTerm ->
            assertFalse(runbookText.contains(vagueTerm), "PR-02 runbook must use exact key sequences instead of vague term: $vagueTerm")
        }
    }

    @Test
    fun `dark uiux pr02 chrome fit scenario maps to ui manual record`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr02-ui-chrome-sprite-pilot"))

        assertEquals("PR-02", scenario.prId)
        assertEquals(ValidationPreset.LOOT_LAB, scenario.runtime.preset)
        assertEquals(2026051002L, scenario.runtime.seed)
        assertEquals(GameLocale.ZH_CN, scenario.runtime.locale)
        assertEquals("rogue", scenario.runtime.professionId)
        assertEquals("greenwood_fringe", scenario.runtime.zoneId)
        assertEquals(
            listOf(
                "evidence/dark-uiux-pr02-shell-hud-frame-fit.png",
                "evidence/dark-uiux-pr02-inventory-modal-frame-fit.png",
                "evidence/dark-uiux-pr02-validation-overlay-frame-fit.png",
                "evidence/dark-uiux-pr02-runtime-error-loading-fit.png",
                "evidence/dark-uiux-pr02-ui-chrome-sprite-pilot-app.log",
            ),
            scenario.evidence.requiredEvidenceFiles,
        )
        assertEquals("UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md", scenario.evidence.manualRecordPath)
        assertEquals(
            scenario.evidence.requiredEvidenceFiles.filter { file -> file.endsWith(".png") },
            scenario.evidence.cuaSteps.map { step -> step.evidenceFile },
        )
        assertEquals(
            "validation.phase4.v4.dark-uiux-pr02-ui-chrome-sprite-pilot.evidence.summary_note",
            scenario.evidence.scenarioNoteLabelKey,
        )
    }

    @Test
    fun `dark uiux pr02 2 ui demo new scenario maps to parity evidence contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr02-1-demo-shell-foundation"))

        assertEquals("PR-02-2", scenario.prId)
        assertEquals(ValidationPreset.LOOT_LAB, scenario.runtime.preset)
        assertEquals(2026051102L, scenario.runtime.seed)
        assertEquals(GameLocale.ZH_CN, scenario.runtime.locale)
        assertEquals("vanguard", scenario.runtime.professionId)
        assertEquals("shattered_outpost", scenario.runtime.zoneId)
        assertEquals(
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
            scenario.evidence.requiredEvidenceFiles,
        )
        assertEquals("UI/manual-records/ui-demo-new-visual-parity.md", scenario.evidence.manualRecordPath)
        assertEquals(
            scenario.evidence.requiredEvidenceFiles.filter { file -> file.endsWith(".png") },
            scenario.evidence.cuaSteps.map { step -> step.evidenceFile },
        )
        assertEquals(
            "validation.phase4.v4.dark-uiux-pr02-2-ui-demo-new-visual-parity.evidence.summary_note",
            scenario.evidence.scenarioNoteLabelKey,
        )
    }

    @Test
    fun `dark uiux pr03 equipment inventory scenario maps to evidence contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr03-equipment-inventory-items"))

        assertEquals("PR-03", scenario.prId)
        assertEquals(ValidationPreset.LOOT_LAB, scenario.runtime.preset)
        assertEquals(2026050903L, scenario.runtime.seed)
        assertEquals(GameLocale.ZH_CN, scenario.runtime.locale)
        assertEquals("rogue", scenario.runtime.professionId)
        assertEquals("greenwood_fringe", scenario.runtime.zoneId)
        assertEquals(
            listOf(
                "evidence/dark-uiux-pr03-equipment-slots.png",
                "evidence/dark-uiux-pr03-inventory-empty.png",
                "evidence/dark-uiux-pr03-inventory-stacked.png",
                "evidence/dark-uiux-pr03-inscription-shop.png",
                "evidence/dark-uiux-pr03-shop-full-slot-replace.png",
                "evidence/dark-uiux-pr03-app.log",
            ),
            scenario.evidence.requiredEvidenceFiles,
        )
        assertEquals("UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md", scenario.evidence.manualRecordPath)
        assertEquals(listOf("log.validation.item.pr03_showcase"), scenario.evidence.requiredLogEventKeys)
        assertTrue(scenario.evidence.cuaSteps.any { step -> step.expectedVisibleResult.contains("hotkeys 5-8") })
    }

    @Test
    fun `pr03 build identity reward scenario matches fixed phase4 v4 contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr03"))

        assertEquals("PR-03", scenario.prId)
        assertEquals(ValidationPreset.LOOT_LAB, scenario.runtime.preset)
        assertEquals(2026042433L, scenario.runtime.seed)
        assertEquals("arcanist", scenario.runtime.professionId)
        assertEquals("human", scenario.runtime.raceId)
        assertEquals("greenwood_fringe", scenario.runtime.zoneId)
        assertEquals(1, scenario.runtime.floor)
        assertEquals(-1, scenario.runtime.routeIndex)
        assertEquals(ValidationScenarioContentPackMode.NONE, scenario.runtime.contentPackMode)
        assertEquals(
            listOf(
                "evidence/phase4-v4-pr03-arcanist-reward-card.png",
                "evidence/phase4-v4-pr03-arcanist-adopted-nonweapon.png",
                "evidence/phase4-v4-pr03-rogue-offhand-payoff.png",
                "evidence/phase4-v4-pr03-report-no-approved-debt.png",
                "evidence/phase4-v4-pr03-app.log",
            ),
            scenario.evidence.requiredEvidenceFiles,
        )
        assertEquals(
            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr03-build-identity-reward-adoption.md",
            scenario.evidence.manualRecordPath,
        )
        assertEquals(listOf("log.validation.item.pr03_showcase"), scenario.evidence.requiredLogEventKeys)
        assertEquals("validation.phase4.v4.phase4-v4-pr03.evidence.summary_note", scenario.evidence.scenarioNoteLabelKey)
        assertTrue(scenario.evidence.cuaSteps.any { step -> step.expectedVisibleResult.contains("professionCapstoneAdoptionFloor.reportOnly") })
    }

    @Test
    fun `pr04 hidden search zone hook scenario matches fixed phase4 v4 contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr04"))

        assertEquals("PR-04", scenario.prId)
        assertEquals(ValidationPreset.HIDDEN_CONTENT, scenario.runtime.preset)
        assertEquals(2026042434L, scenario.runtime.seed)
        assertEquals("arcanist", scenario.runtime.professionId)
        assertEquals("human", scenario.runtime.raceId)
        assertEquals("deep_iron_pit", scenario.runtime.zoneId)
        assertEquals(1, scenario.runtime.floor)
        assertEquals(-1, scenario.runtime.routeIndex)
        assertEquals(ValidationScenarioContentPackMode.NONE, scenario.runtime.contentPackMode)
        assertEquals(
            listOf(
                "evidence/phase4-v4-pr04-deep-iron-search-cue.png",
                "evidence/phase4-v4-pr04-search-result-feedback.png",
                "evidence/phase4-v4-pr04-abyssal-void-pressure.png",
                "evidence/phase4-v4-pr04-zone-hook-triggered.png",
                "evidence/phase4-v4-pr04-priority-no-overlap.png",
                "evidence/phase4-v4-pr04-app.log",
            ),
            scenario.evidence.requiredEvidenceFiles,
        )
        assertEquals(
            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr04-hidden-search-zone-hooks.md",
            scenario.evidence.manualRecordPath,
        )
        assertEquals(
            listOf(
                "log.search.available",
                "log.search.revealed_tag",
                "log.zone.hook.void_pressure",
                "zone.trigger.void_pressure_active",
            ),
            scenario.evidence.requiredLogEventKeys,
        )
        assertEquals("validation.phase4.v4.phase4-v4-pr04.evidence.summary_note", scenario.evidence.scenarioNoteLabelKey)
    }

    @Test
    fun `pr05 boss variant phase language scenario matches fixed phase4 v4 contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr05"))

        assertEquals("PR-05", scenario.prId)
        assertEquals(ValidationPreset.BOSS_VARIANT, scenario.runtime.preset)
        assertEquals(2026042435L, scenario.runtime.seed)
        assertEquals("vanguard", scenario.runtime.professionId)
        assertEquals("human", scenario.runtime.raceId)
        assertEquals("deep_iron_pit", scenario.runtime.zoneId)
        assertEquals(2, scenario.runtime.floor)
        assertEquals(-1, scenario.runtime.routeIndex)
        assertEquals(ValidationScenarioContentPackMode.NONE, scenario.runtime.contentPackMode)
        assertEquals(
            listOf(
                "evidence/phase4-v4-pr05-molten-glass-warning.png",
                "evidence/phase4-v4-pr05-grey-crown-warning.png",
                "evidence/phase4-v4-pr05-abyssal-eclipse-warning.png",
                "evidence/phase4-v4-pr05-report-coverage.png",
                "evidence/phase4-v4-pr05-app.log",
            ),
            scenario.evidence.requiredEvidenceFiles,
        )
        assertEquals(
            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr05-boss-variant-phase-language.md",
            scenario.evidence.manualRecordPath,
        )
        assertEquals(
            listOf(
                "log.boss.phase_override_entered",
                "boss.variant.molten_glass.phase_override.entered",
                "boss.variant.grey_crown.phase_override.entered",
                "boss.variant.abyssal_eclipse.phase_override.entered",
            ),
            scenario.evidence.requiredLogEventKeys,
        )
        assertEquals("validation.phase4.v4.phase4-v4-pr05.evidence.summary_note", scenario.evidence.scenarioNoteLabelKey)
    }

    @Test
    fun `pr06 route diversity scenario matches fixed phase4 v4 contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr06"))

        assertEquals("PR-06", scenario.prId)
        assertEquals(ValidationPreset.MAPGEN_DIFF, scenario.runtime.preset)
        assertEquals(2026042436L, scenario.runtime.seed)
        assertEquals("rogue", scenario.runtime.professionId)
        assertEquals("human", scenario.runtime.raceId)
        assertEquals("greenwood_fringe", scenario.runtime.zoneId)
        assertEquals(1, scenario.runtime.floor)
        assertEquals(0, scenario.runtime.routeIndex)
        assertEquals(ValidationScenarioContentPackMode.NONE, scenario.runtime.contentPackMode)
        assertEquals(
            listOf(
                "evidence/phase4-v4-pr06-scenario-distribution.png",
                "evidence/phase4-v4-pr06-route-hash-diversity.png",
                "evidence/phase4-v4-pr06-branch-inclusive-routes.png",
                "evidence/phase4-v4-pr06-verifychanged-routing.png",
                "evidence/phase4-v4-pr06-app.log",
            ),
            scenario.evidence.requiredEvidenceFiles,
        )
        assertEquals(
            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr06-long-run-route-diversity.md",
            scenario.evidence.manualRecordPath,
        )
        assertEquals(listOf("log.validation.phase4_v4.action"), scenario.evidence.requiredLogEventKeys)
        assertEquals("validation.phase4.v4.phase4-v4-pr06.evidence.summary_note", scenario.evidence.scenarioNoteLabelKey)
    }

    @Test
    fun `pr07 sample pack visibility scenario matches fixed phase4 v4 contract`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr07"))

        assertEquals("PR-07", scenario.prId)
        assertEquals(ValidationPreset.CONTENT_PACK, scenario.runtime.preset)
        assertEquals(2026042437L, scenario.runtime.seed)
        assertEquals("arcanist", scenario.runtime.professionId)
        assertEquals("human", scenario.runtime.raceId)
        assertEquals("underground_river", scenario.runtime.zoneId)
        assertEquals(1, scenario.runtime.floor)
        assertEquals(-1, scenario.runtime.routeIndex)
        assertEquals(ValidationScenarioContentPackMode.SAMPLE_PACK_ENABLED, scenario.runtime.contentPackMode)
        assertTrue("evidence/phase4-v4-pr07-active-sample-pack-summary.png" in scenario.evidence.requiredEvidenceFiles)
        assertTrue("evidence/phase4-v4-pr07-touched-content-ids.png" in scenario.evidence.requiredEvidenceFiles)
        assertEquals(
            "docs/review/phase4/v4-pr/manual-records/phase4-v4-pr07-sample-pack-add-first-visibility.md",
            scenario.evidence.manualRecordPath,
        )
        assertEquals(listOf("log.validation.phase4_v4.action"), scenario.evidence.requiredLogEventKeys)
        assertEquals("validation.phase4.v4.phase4-v4-pr07.evidence.summary_note", scenario.evidence.scenarioNoteLabelKey)
    }

    @Test
    fun `scenario yaml ids stay in parity with typed registry`() {
        val repoRoot = Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()
        val parity =
            ValidationScenarioRegistry.validateYamlParity(
                repoRoot.resolve("tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml"),
            )

        assertTrue(parity.isValid, "missingFromYaml=${parity.missingFromYaml}, missingFromKotlin=${parity.missingFromKotlin}")
    }

    @Test
    fun `scenario yaml rejects duplicated contract fields`() {
        val yamlPath = tempDir.resolve("phase4-v4-scenarios.yaml")
        java.nio.file.Files.writeString(
            yamlPath,
            """
            |scenarios:
            |  - id: phase4-v4-pr00-selftest
            |    seed: 2026042430
            |
            """.trimMargin(),
        )

        assertThrows(IllegalArgumentException::class.java) {
            ValidationScenarioRegistry.validateYamlParity(yamlPath)
        }
    }

    @Test
    fun `scenario requires four screenshot evidence files`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))

        assertThrows(IllegalArgumentException::class.java) {
            scenario.copy(
                evidence =
                    scenario.evidence.copy(
                        requiredEvidenceFiles =
                            listOf(
                                "evidence/bootstrap.png",
                                "evidence/primary.png",
                                "evidence/secondary.png",
                                "evidence/app.log",
                            ),
                    ),
            )
        }
    }

    @Test
    fun `scenario session options force no profile persistence and expose scenario id`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))
        val options = scenario.toSessionOptions()

        assertEquals(ValidationScenarioId("phase4-v4-pr00-selftest"), options.scenarioId)
        assertEquals(ProfileRunPersistenceMode.NO_OP, options.profileRunPersistenceMode)
        assertEquals(ValidationPreset.MAPGEN_DIFF, options.preset)
        assertEquals(2026042430L, options.foundationConfig.seed)
        assertEquals("vanguard", options.foundationConfig.playerProfessionId)
        assertEquals("human", options.foundationConfig.playerRaceId)
        assertEquals("greenwood_fringe", options.foundationConfig.zoneId)
        assertEquals(2, options.foundationConfig.floor)
        assertEquals(-1, options.scenarioRouteIndex)
        assertEquals(1, options.foundationConfig.routeIndex)
    }

    @Test
    fun `scenario route index is preserved as scenario boundary without overriding runtime route`() {
        val baseScenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))
        val scenario = baseScenario.copy(runtime = baseScenario.runtime.copy(routeIndex = 6))
        val options = scenario.toSessionOptions()

        assertEquals(6, options.scenarioRouteIndex)
        assertEquals(1, options.foundationConfig.routeIndex)
    }

    @Test
    fun `scenario runtime rejects route index below no-corpus sentinel`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))

        assertThrows(IllegalArgumentException::class.java) {
            scenario.runtime.copy(routeIndex = -2)
        }
    }

    @Test
    fun `mapgen diff runtime rejects route diversity forbidden start zones`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))

        assertThrows(IllegalArgumentException::class.java) {
            scenario.runtime.copy(zoneId = "abyssal_temple")
        }
        val options = scenario.toSessionOptions()
        assertThrows(IllegalArgumentException::class.java) {
            options.copy(
                foundationConfig = options.foundationConfig.copy(zoneId = "grey_gate_depths"),
            )
        }
    }

    @Test
    fun `pr01 scenario actions materialize profession tree run choice scenes`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr01"))
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("phase4-v4-pr01-scenario-actions")),
                    options = scenario.toSessionOptions(),
                ),
            )

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )
        assertEquals(listOf(1, 2, 3), session.talentSlots().map { slot -> slot.slot })
        assertFalse(session.talentSlots().any { slot -> slot.slot == 4 })
        assertEquals(1, session.playerStatus().talentPoints)
        assertEquals(TalentNodeStateSnapshot.LEARNABLE, talentTreeNode(session, "war_cry").state)
        assertEquals(TalentNodeStateSnapshot.LOCKED, talentTreeNode(session, "linebreaker").state)

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                    ),
                ),
            ),
        )
        assertEquals(null, session.consumePendingValidationRestartOptions())
        assertEquals(listOf(1, 2, 3, 4), session.talentSlots().map { slot -> slot.slot })
        assertEquals("charge", session.talentSlots().first { slot -> slot.slot == 4 }.talentId)
        val pendingSunderArmor = talentTreeNode(session, "sunder_armor")
        assertEquals(1, pendingSunderArmor.rank)
        assertEquals(0, pendingSunderArmor.committedRank)
        assertTrue(pendingSunderArmor.hasPendingAllocation)

        assertFalse(session.perform(PlayerCommand.ConfirmTalentDraft))
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.talent.active_slot_choice_required" })

        assertTrue(session.perform(PlayerCommand.ConfirmTalentDraftToReserve))
        assertEquals("charge", session.talentSlots().first { slot -> slot.slot == 4 }.talentId)
        assertTrue(session.renderSnapshot().uiState.reserveTalents.any { talent -> talent.talentId == "sunder_armor" })
        assertEquals(0, session.playerStatus().talentPoints)
    }

    @Test
    fun `dark uiux pr04 scenario materializes reference talent assign state`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr04-profession-tree-ui"))
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr04-scenario-actions")),
                    options = scenario.toSessionOptions(),
                ),
            )

        assertEquals("PR-04", scenario.prId)
        assertEquals("UI/manual-records/dark-uiux-pr04-profession-tree-ui.md", scenario.evidence.manualRecordPath)
        assertFalse("evidence/dark-uiux-pr04-active-slot-choice.png" in scenario.evidence.requiredEvidenceFiles)
        assertEquals(
            listOf("client/build/reports/golden/dark-uiux-pr04/dark-uiux-pr04-active-slot-choice.png"),
            scenario.evidence.requiredExternalEvidenceFiles,
        )
        assertTrue(
            scenario.evidence.cuaSteps.any { step ->
                step.evidenceFile == "client/build/reports/golden/dark-uiux-pr04/dark-uiux-pr04-active-slot-choice.png"
            },
        )
        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )

        assertEquals(3, session.playerStatus().talentPoints)
        assertEquals(1, session.playerStatus().raceTalentPoints)
        assertEquals(listOf(1, 2, 3, 4), session.talentSlots().map { slot -> slot.slot })
        assertEquals(
            listOf("power_strike", "charge", "shield_bash", "guard_stance"),
            session.talentSlots().map { slot -> slot.talentId },
        )
        assertEquals(TalentNodeStateSnapshot.LEARNED_ACTIVE, talentTreeNode(session, "power_strike").state)
        assertEquals(TalentNodeStateSnapshot.LEARNABLE, talentTreeNode(session, "sweeping_strike").state)
        assertEquals(TalentNodeStateSnapshot.LEARNED_ACTIVE, talentTreeNode(session, "charge").state)
        assertEquals(TalentNodeStateSnapshot.LEARNED_ACTIVE, talentTreeNode(session, "shield_bash").state)
        assertEquals(TalentNodeStateSnapshot.LEARNED_ACTIVE, talentTreeNode(session, "guard_stance").state)
        assertEquals(TalentNodeStateSnapshot.LEARNED_RESERVE, talentTreeNode(session, "iron_wall").state)
        assertEquals(TalentNodeStateSnapshot.LEARNED_ACTIVE, talentTreeNode(session, "war_cry").state)
        assertEquals(TalentNodeStateSnapshot.LEARNABLE, talentTreeNode(session, "rallying_banner").state)
        assertEquals(TalentNodeStateSnapshot.LEARNED_ACTIVE, talentTreeNode(session, "intimidation").state)
        assertEquals(2, talentTreeNode(session, "power_strike").rank)
        assertEquals(1, talentTreeNode(session, "power_strike").committedRank)
        assertTrue(talentTreeNode(session, "power_strike").hasPendingAllocation)

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                    ),
                ),
            ),
        )
        val slotChoiceRequirement = requireNotNull(session.renderSnapshot().uiState.activeTalentSlotChoiceRequirement)
        assertEquals("sunder_armor", slotChoiceRequirement.candidateTalentId)
        assertEquals("PROFESSION", slotChoiceRequirement.ownerType)
        assertEquals("vanguard", slotChoiceRequirement.treeOwnerId)
    }

    @Test
    fun `pr03 scenario actions materialize build identity reward payoff scenes`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr03"))
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("phase4-v4-pr03-scenario-actions")),
                    options = scenario.toSessionOptions(),
                ),
            )

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )
        assertEquals("unique_deepcurrent_lens", session.currentEquippedBaseItemId(com.ktome.core.item.EquipSlot.OFF_HAND))
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.validation.item.pr03_showcase" })
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.reward.capstone.non_weapon_anchor" })
        val primaryReward = requireNotNull(session.renderSnapshot().uiState.recentRewards.lastOrNull())
        val primaryIdentity = requireNotNull(primaryReward.buildIdentity)
        assertEquals("OFF_HAND", primaryIdentity.slotId)
        assertEquals("ui.reward.slot.off_hand", primaryIdentity.slotLabelKey)
        assertEquals("arcanist", primaryIdentity.professionId)
        assertEquals("profession.arcanist.name", primaryIdentity.professionLabelKey)
        assertEquals("ui.reward.identity.reason.non_weapon_capstone", primaryIdentity.scoreReason.key)

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                    ),
                ),
            ),
        )
        assertEquals("artifact_briar_heart", session.currentEquippedBaseItemId(com.ktome.core.item.EquipSlot.OFF_HAND))
    }

    @Test
    fun `pr04 scenario actions stage deep iron search and abyssal hook surfaces`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr04"))
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("phase4-v4-pr04-scenario-actions")),
                    options = scenario.toSessionOptions(),
                ),
            )

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )
        assertEquals("deep_iron_pit", session.config.zoneId)
        assertNotNull(session.renderSnapshot().uiState.searchPromptLabelKey)
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.search.available" })
        assertTrue(session.perform(PlayerCommand.Search))
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.search.revealed_tag" })

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                    ),
                ),
            ),
        )
        assertEquals("abyssal_temple", session.config.zoneId)
        val secondarySnapshot = session.renderSnapshot()
        val secondaryCueKeys =
            secondarySnapshot.uiState.frontstageReadability.recentActionCues
                .map { cue -> cue.message.key to cue.cueType.name }
        assertTrue(
            "void_pressure" in session.automationTriggeredZoneHookIds(),
            "Expected void pressure runtime hook tag. cues=$secondaryCueKeys tags=${session.automationDiscoveryTags()}",
        )
        assertTrue(
            "zone.trigger.void_pressure_active" in session.automationDiscoveryTags(),
            "Expected void pressure trigger fact tag. cues=$secondaryCueKeys tags=${session.automationDiscoveryTags()}",
        )
        assertTrue(
            ("log.zone.hook.void_pressure" to "ZONE_HOOK_TRIGGERED") in secondaryCueKeys,
            "Expected visible zone hook frontstage cue. cues=$secondaryCueKeys",
        )
    }

    @Test
    fun `pr05 scenario actions rotate boss variant phase override surfaces`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr05"))
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("phase4-v4-pr05-scenario-actions")),
                    options = scenario.toSessionOptions(),
                ),
            )

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )
        assertTrue(
            session.renderSnapshot().overlays.any { overlay -> overlay.sourceAbilityId == "molten_glass_phase_override_warning" },
            "overlays=${session.renderSnapshot().overlays.map { overlay -> overlay.sourceAbilityId }} logs=${session.renderSnapshot().logEvents.map { event -> event.message.key }}",
        )
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "boss.variant.molten_glass.phase_override.entered" })

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                    ),
                ),
            ),
        )
        assertEquals("grey_gate_depths", session.config.zoneId)
        assertTrue(
            session.renderSnapshot().overlays.any { overlay -> overlay.sourceAbilityId == "grey_crown_phase_override_warning" },
            "overlays=${session.renderSnapshot().overlays.map { overlay -> overlay.sourceAbilityId }} logs=${session.renderSnapshot().logEvents.map { event -> event.message.key }}",
        )
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "boss.variant.grey_crown.phase_override.entered" })

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                    ),
                ),
            ),
        )
        assertEquals("abyssal_heart", session.config.zoneId)
        assertTrue(
            session.renderSnapshot().overlays.any { overlay -> overlay.sourceAbilityId == "abyssal_eclipse_phase_override_warning" },
            "overlays=${session.renderSnapshot().overlays.map { overlay -> overlay.sourceAbilityId }} logs=${session.renderSnapshot().logEvents.map { event -> event.message.key }}",
        )
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "boss.variant.abyssal_eclipse.phase_override.entered" })
    }

    @Test
    fun `pr06 scenario actions expose route diversity and routing summaries`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr06"))
        val repoRoot = tempDir.resolve("phase4-v4-pr06-artifacts")
        writePr06ValidationArtifacts(repoRoot)

        withRepoRootProperty(repoRoot) {
            val session =
                GameModule.newValidationSession(
                    ValidationSessionRequest(
                        saveManager = SaveManager(tempDir.resolve("phase4-v4-pr06-scenario-actions")),
                        options = scenario.toSessionOptions(),
                    ),
                )

            assertTrue(
                session.perform(
                    PlayerCommand.Validation(
                        ValidationAction.Phase4V4ScenarioAction(
                            scenarioId = scenario.id,
                            actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                        ),
                    ),
                ),
            )
            val primaryResult = requireNotNull(session.validationSummarySnapshot()?.lastResult?.argumentValue("result"))
            assertTrue(primaryResult.contains("artifactStatus=loaded"), primaryResult)
            assertTrue(primaryResult.contains("full_route=98"), primaryResult)
            assertTrue(primaryResult.contains("branch_inclusive=7"), primaryResult)
            assertTrue(primaryResult.contains("route_probe=5"), primaryResult)
            assertTrue(primaryResult.contains("late_route_probe=4"), primaryResult)
            assertTrue(primaryResult.contains("zoneRouteHashDistribution=2_hashes,max=98/114"), primaryResult)
            assertTrue(primaryResult.contains("topHashShare=0.13<=0.40"), primaryResult)
            assertTrue(primaryResult.contains("branchInclusiveRoutes=1(secret:artifact_secret)"), primaryResult)

            assertTrue(
                session.perform(
                    PlayerCommand.Validation(
                        ValidationAction.Phase4V4ScenarioAction(
                            scenarioId = scenario.id,
                            actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                        ),
                    ),
                ),
            )
            val secondaryResult = requireNotNull(session.validationSummarySnapshot()?.lastResult?.argumentValue("result"))
            assertTrue(secondaryResult.contains("verifyChangedArtifactStatus=loaded"), secondaryResult)
            assertTrue(secondaryResult.contains(":game:longRunLab"), secondaryResult)
            assertTrue(secondaryResult.contains(":tools:scopeCoverageLint"), secondaryResult)
        }
    }

    @Test
    fun `pr07 scenario actions expose sample pack summary and runtime touched ids`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr07"))
        val sampleBindingId = SearchBindingId("sample.flooded_relics.search.flooded_reliquary")
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("phase4-v4-pr07-scenario-actions")),
                    options =
                        scenario.toSessionOptions(
                            samplePackSelection = ContentPackSelection.of(ContentPackFixtureCatalog.samplePackRoot()),
                        ),
                ),
            )

        assertEquals(listOf("sample.flooded_relics"), requireNotNull(session.validationSummarySnapshot()).activePackIds)
        assertTrue(
            requireNotNull(session.validationSummarySnapshot()).activePackSummaries.single().opCounts.getValue("ADD") >= 5,
        )
        assertEquals(0, requireNotNull(session.validationSummarySnapshot()).packKeyResolutionSummary.warningCount)

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )
        val primaryResult = requireNotNull(session.validationSummarySnapshot()?.lastResult?.argumentValue("result"))
        assertTrue(primaryResult.contains("sample_pack_summary_ready"), primaryResult)
        assertTrue(primaryResult.contains("noPackActivePackIds=N/A"), primaryResult)
        val primarySummary = requireNotNull(session.validationSummarySnapshot())
        assertTrue(primarySummary.touchedContentIds.isEmpty())
        val comparison = requireNotNull(primarySummary.packVisibilityComparison)
        assertTrue(comparison.noPackState.activePackIds.isEmpty())
        assertTrue(comparison.noPackState.activePackSummaries.isEmpty())
        assertTrue(comparison.noPackState.touchedContentIds.isEmpty())
        assertEquals(listOf("sample.flooded_relics"), comparison.activeSamplePackState.activePackIds)
        assertTrue(comparison.activeSamplePackState.activePackSummaries.single().opCounts.getValue("ADD") >= 5)

        assertTrue(session.perform(PlayerCommand.DropInventoryItem(session.inventoryItems().first().index)))
        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                    ),
                ),
            ),
        )
        val secondaryResult = requireNotNull(session.validationSummarySnapshot()?.lastResult?.argumentValue("result"))
        assertTrue(secondaryResult.contains(sampleBindingId.value), secondaryResult)

        assertTrue(session.perform(PlayerCommand.Search))
        session.automationMovePlayerTo(requireNotNull(session.automationHiddenEntrancePointForBinding(sampleBindingId)))
        assertTrue(session.perform(PlayerCommand.Interact))
        session.automationMovePlayerTo(requireNotNull(session.automationSecretRewardPointForBinding(sampleBindingId)))
        assertTrue(session.perform(PlayerCommand.Interact))

        val touchedContentIds = requireNotNull(session.validationSummarySnapshot()).touchedContentIds
        assertTrue("sample.flooded_relics.secret_zone.flooded_reliquary" in touchedContentIds, touchedContentIds.toString())
        assertTrue("sample.flooded_relics.hidden_event.flooded_reliquary.reward" in touchedContentIds, touchedContentIds.toString())
        assertTrue("sample.flooded_relics.unique.floodtide_lantern" in touchedContentIds, touchedContentIds.toString())
        assertTrue("sample.flooded_relics.loot.flooded_reliquary.secret" in touchedContentIds, touchedContentIds.toString())
    }

    @Test
    fun `pr06 scenario actions prefer launch materialized route summaries`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr06"))
        val primaryProperty = "artifactStatus=loaded;scenarioTypeDistribution={full_route=12,branch_inclusive=4}"
        val evidenceProperty = "artifactStatus=loaded;verifyChangedArtifactStatus=loaded;verifyChangedTasks=:game:longRunLab"

        withSystemProperty(Phase4V4Pr06WhiteboxProperties.PRIMARY_RESULT, primaryProperty) {
            withSystemProperty(Phase4V4Pr06WhiteboxProperties.EVIDENCE_RESULT, evidenceProperty) {
                val session =
                    GameModule.newValidationSession(
                        ValidationSessionRequest(
                            saveManager = SaveManager(tempDir.resolve("phase4-v4-pr06-launch-summary")),
                            options = scenario.toSessionOptions(),
                        ),
                    )

                assertTrue(
                    session.perform(
                        PlayerCommand.Validation(
                            ValidationAction.Phase4V4ScenarioAction(
                                scenarioId = scenario.id,
                                actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                            ),
                        ),
                    ),
                )
                val primaryResult = requireNotNull(session.validationSummarySnapshot()?.lastResult?.argumentValue("result"))
                assertEquals(primaryProperty, primaryResult)

                assertTrue(
                    session.perform(
                        PlayerCommand.Validation(
                            ValidationAction.Phase4V4ScenarioAction(
                                scenarioId = scenario.id,
                                actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                            ),
                        ),
                    ),
                )
                val secondaryResult = requireNotNull(session.validationSummarySnapshot()?.lastResult?.argumentValue("result"))
                assertTrue(secondaryResult.contains(evidenceProperty), secondaryResult)
                assertTrue(secondaryResult.contains("uiSurface=>clientSmoke+goldenScreenshot"), secondaryResult)
            }
        }
    }


    @Test
    fun `phase4 v4 scenario actions dispatch through validation command path`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))
        val evidenceSummary =
            ValidationScenarioEvidenceSummary(
                whiteboxRoot = "build/whitebox/phase4-v4-pr00-selftest",
                evidenceDir = "build/whitebox/phase4-v4-pr00-selftest/evidence",
                manualRecordPath = scenario.evidence.manualRecordPath,
                expectedEvidencePath = "build/whitebox/phase4-v4-pr00-selftest/expected-evidence.json",
                runbookPath = "build/whitebox/phase4-v4-pr00-selftest/cua-runbook.md",
                appExecutableSha256Path = "build/whitebox/phase4-v4-pr00-selftest/app-executable.sha256",
                appExecutableSha256 = "abc123",
            )
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("phase4-v4-scenario-actions")),
                    options = scenario.toSessionOptions(evidenceSummary = evidenceSummary),
                ),
            )

        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )
        val afterFirst = session.validationSummarySnapshot()?.lastResult
        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                    ),
                ),
            ),
        )

        assertEquals(afterFirst, session.validationSummarySnapshot()?.lastResult)
        assertEquals(null, session.validationSummarySnapshot()?.scenarioEvidenceSummary)
        assertTrue(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.SHOW_EVIDENCE_SUMMARY,
                    ),
                ),
            ),
        )
        assertEquals(evidenceSummary, session.validationSummarySnapshot()?.scenarioEvidenceSummary)
    }

    private fun com.ktome.core.snapshot.RenderTextTokenSnapshot.argumentValue(name: String): String? =
        arguments.firstOrNull { argument -> argument.name == name }?.value

    private fun writePr06ValidationArtifacts(repoRoot: Path) {
        val longRunPath = repoRoot.resolve("build/reports/harness/long-run-full.json")
        Files.createDirectories(longRunPath.parent)
        Files.writeString(longRunPath, """{"buildId":"test","scenarioCount":114}""")
        val reportPath = repoRoot.resolve("tools/build/reports/verification/phase4/report-phase4-summary.json")
        Files.createDirectories(reportPath.parent)
        Files.writeString(
            reportPath,
            """
            |{
            |  "sections": {
            |    "routeDiversity": {
            |      "scenarioTypeDistribution": {
            |        "full_route": 98,
            |        "branch_inclusive": 7,
            |        "route_probe": 5,
            |        "late_route_probe": 4
            |      },
            |      "zoneRouteHashDistribution": {
            |        "artifact_hash": 98,
            |        "secondary_hash": 16
            |      },
            |      "zoneRouteHashDiversity": {
            |        "totalRuns": 114,
            |        "distinctHashes": 2,
            |        "fullRouteIntentDistinctCount": 98,
            |        "actualFullRouteHashDistinctCount": 9,
            |        "topHash": "artifact_hash",
            |        "topHashCount": 13,
            |        "topHashShare": 0.13,
            |        "probeRouteHashSample": [
            |          "artifact_probe_hash"
            |        ]
            |      },
            |      "routeTokenSample": [
            |        "artifact_route>secret:artifact_secret",
            |        "secondary_route"
            |      ],
            |      "probeRouteHashSample": [
            |        "artifact_probe_hash"
            |      ]
            |    }
            |  }
            |}
            """.trimMargin(),
        )
        val verifyChangedPath = repoRoot.resolve("build/verification/verify-changed/verify-changed-plan.json")
        Files.createDirectories(verifyChangedPath.parent)
        Files.writeString(
            verifyChangedPath,
            """
            |{
            |  "requestedTaskPaths": [
            |    ":game:longRunLab",
            |    ":tools:scopeCoverageLint"
            |  ]
            |}
            """.trimMargin(),
        )
    }

    private fun withRepoRootProperty(
        repoRoot: Path,
        block: () -> Unit,
    ) {
        withSystemProperty("ktome.repo.root", repoRoot.toString(), block)
    }

    private fun withSystemProperty(
        name: String,
        value: String,
        block: () -> Unit,
    ) {
        val previous = System.getProperty(name)
        System.setProperty(name, value)
        try {
            block()
        } finally {
            if (previous == null) {
                System.clearProperty(name)
            } else {
                System.setProperty(name, previous)
            }
        }
    }

    private fun talentTreeNode(
        session: FoundationGameSession,
        talentId: String,
    ) = session.renderSnapshot().uiState.talentTrees
        .flatMap { tree -> tree.nodes }
        .first { node -> node.talentId == talentId }
}
