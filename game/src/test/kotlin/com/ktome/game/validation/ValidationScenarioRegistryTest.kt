package com.ktome.game.validation

import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.TalentNodeStateSnapshot
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
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

    private fun talentTreeNode(
        session: FoundationGameSession,
        talentId: String,
    ) = session.renderSnapshot().uiState.talentTrees
        .flatMap { tree -> tree.nodes }
        .first { node -> node.talentId == talentId }
}
