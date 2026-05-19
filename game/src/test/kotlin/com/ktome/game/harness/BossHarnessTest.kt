package com.ktome.game.harness

import com.ktome.core.ai.AIDecisionTrace
import com.ktome.core.ai.BossEncounterState
import com.ktome.core.ai.BossTrace
import com.ktome.core.ai.PendingTelegraphState
import com.ktome.core.ecs.BossVariantRuntime
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.EliteMutationLoadout
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.core.loot.EncounterThreatBudget
import com.ktome.core.loot.FloorRewardBudget
import com.ktome.core.map.Point
import com.ktome.core.phase.Phase4ContractVersions
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.CombatFeedbackTypeSnapshot
import com.ktome.game.AbyssalRuntimeKeys
import com.ktome.game.data.DataLoader
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.ZoneTriggerFactIds
import com.ktome.game.elites.BossVariantSelectionMode
import com.ktome.game.interactablePoint
import com.ktome.game.terrainPreferenceImplemented
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.math.max
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

private const val MIN_PHASE_TRANSITION_OBSERVED_RATIO = 1.0
private const val MIN_VARIANT_TRACE_DIVERGENCE_RATIO = 1.0
private const val MIN_VARIANT_ACTION_TRACE_DIVERGENCE_SCORE = 0.25

class BossHarnessTest {
    @TempDir
    lateinit var tempDir: Path

    private val rewardBudgetsByLootProfileId: Map<String, Int> =
        DataLoader().loadSchemaCatalog().lootProfiles.associate { profile -> profile.id to profile.rewardBudget }
    private val bossRegistryMetrics: BossRegistryMetrics = BossRegistryMetrics.load()

    @Test
    @Tag("bossHarness")
    fun `boss harness covers phase three roster with telegraph and trace consistency`() {
        val pairReports =
            listOf(
                runMoltenGiantPair(seed = 20260325L),
                runDungeonLordPair(seed = 20260326L),
                runAbyssalGuardianPair(seed = 20260327L),
            )
        val reports = pairReports.flatMap { pair -> listOf(pair.baseReport, pair.variantReport) }

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "boss-harness",
            payload =
                buildJsonObject {
                    put("scriptVersion", "boss-harness-v5")
                    putJsonArray("reports") { reports.forEach { report -> add(report.toJson()) } }
                    putJsonArray("pairReports") { pairReports.forEach { report -> add(report.toJson()) } }
                },
            markdown =
                buildString {
                    appendLine("# Boss Harness")
                    appendLine("- scriptVersion: boss-harness-v5")
                    reports.forEach { report ->
                        appendLine(
                            "- encounter=${report.encounterId}, template=${report.templateId}, variant=${report.variantId ?: "base"}, " +
                                "seed=${report.seed}, success=${report.success}, phase=${report.phaseId}, telegraph=${report.telegraphKey}, " +
                                "selectedActions=${if (report.selectedActionIds.isEmpty()) "none" else report.selectedActionIds}, " +
                                "threatCost=${report.threatCost}, lootProfileOverride=${report.lootProfileOverride ?: "none"}, " +
                                "preferredTerrain=${if (report.preferredTerrainTags.isEmpty()) "none" else report.preferredTerrainTags}, " +
                                "bossTerrain=${if (report.bossTerrainTags.isEmpty()) "none" else report.bossTerrainTags}, " +
                                "preferenceAvailable=${report.terrainPreferenceAvailable}, preferenceApplied=${report.terrainPreferenceImplemented}, " +
                                "phaseSequence=${report.phaseSequence}, aiTraceHash=${report.aiTraceHash}, bossTraceHash=${report.bossTraceHash}",
                        )
                    }
                    pairReports.forEach { pair ->
                        appendLine(
                            "- pair=${pair.joinKey.scenarioId}, success=${pair.success}, phaseGraphUnchanged=${pair.phaseGraphUnchanged}, " +
                                "structuralDiffCount=${pair.phaseGraphStructuralDiffCount}, inspectReadable=${pair.inspectReadable}, " +
                                "logReadable=${pair.logReadable}, threatLedgerMatched=${pair.threatLedgerMatched}, rewardLedgerMatched=${pair.rewardLedgerMatched}, " +
                                "terrainPreferenceAvailable=${pair.variantReport.terrainPreferenceAvailable}, " +
                                "terrainPreferenceApplied=${pair.variantReport.terrainPreferenceImplemented}",
                        )
                    }
                },
        )

        val whiteBoxOutputDir = whiteBoxSummaryReportDir("ktome.phase4.whitebox.boss.reportDir", "boss")
        val whiteBoxWriteResult =
            WhiteBoxHarnessWriter.write(
                WhiteBoxHarnessWriteRequest(
                    domainId = "boss",
                    outputDir = whiteBoxOutputDir,
                    header =
                        whiteBoxPhase4Header(
                            harnessId = "bossHarness",
                            corpusId = "P4_PR06_BOSS_WHITEBOX",
                            contractVersions =
                                listOf(
                                    "bossVariantOverlay" to Phase4ContractVersions.BOSS_VARIANT_OVERLAY_VERSION.toString(),
                                    "eliteMutationRegistry" to Phase4ContractVersions.ELITE_MUTATION_REGISTRY_VERSION.toString(),
                                    "rewardLedger" to Phase4ContractVersions.REWARD_LEDGER_VERSION.toString(),
                                    "bossEncounterPhaseGraph" to "1",
                                    "visualManifest" to "1",
                                    "audioManifest" to "1",
                                ),
                            seeds = pairReports.flatMap { pair -> listOf(pair.baseReport.seed, pair.variantReport.seed) }.distinct(),
                        ),
                    corpus =
                        WhiteBoxCorpusSpec(
                            corpusId = "P4_PR06_BOSS_WHITEBOX",
                            description = "Base-vs-variant PR-06 boss overlay corpus for the three formal boss encounters.",
                            sampleCount = pairReports.size,
                        ),
                    cases = pairReports.map { pair -> pair.toWhiteBoxCase(whiteBoxOutputDir) },
                    aggregates = bossAggregates(pairReports),
                ),
            )

        assertEquals(0, whiteBoxWriteResult.failedAssertions, "bossHarness left failed white-box assertions in ${whiteBoxWriteResult.summaryPath}")
        assertTrue(reports.all(BossHarnessReport::success), reports.joinToString(separator = "\n") { report -> "${report.templateId}:${report.variantId ?: "base"} ${report.failureReason ?: "unknown"}" })
        assertTrue(pairReports.all(BossHarnessPairReport::success), pairReports.joinToString(separator = "\n") { pair -> "${pair.joinKey.scenarioId}: ${pair.failureReason ?: "unknown"}" })
    }

    @Test
    @Tag("bossHarness")
    fun `boss harness keeps telegraph and combat feedback visible in the same snapshot`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260326L, zoneId = "grey_gate_depths", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("boss-feedback-coexist")),
            )
        descendToBossFloor(session)
        val bossId = requireNotNull(session.automationEntityByTemplateId("cultist.dungeon_lord"))
        val world = session.automationWorld()
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(findOpenPointAtDistance(session, bossPoint, minDistance = 5, maxDistance = 8))

        assertTrue(session.perform(PlayerCommand.Wait))
        requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).current =
            requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).max * 40 / 100
        assertTrue(session.perform(PlayerCommand.Wait))
        val telegraph = requireNotNull(world.get<PendingTelegraphState>(bossId))

        triggerTemplarHealFeedback(session)
        val snapshot = session.renderSnapshot()

        assertTrue(snapshot.overlays.any { overlay -> overlay.id.startsWith("telegraph:") || overlay.id.startsWith("boss-warning:") })
        assertTrue(
            snapshot.combatFeedbackEvents.any { event ->
                event.type == CombatFeedbackTypeSnapshot.HEAL &&
                    event.targetEntityId == session.playerId.value &&
                    (event.amount ?: 0) > 0
            },
        )
        assertTrue(telegraph.sourceAbilityId == "dungeon_lord_phase_warning" || telegraph.telegraphSpecId == "dungeon_lord_phase_warning")
    }

    private fun runMoltenGiantPair(seed: Long): BossHarnessPairReport =
        buildPairReport(
            baseReport = runMoltenGiantHarness(seed = seed, preferredVariantId = null),
            variantReport = runMoltenGiantHarness(seed = seed, preferredVariantId = "boss.variant.molten_glass"),
        )

    private fun runDungeonLordPair(seed: Long): BossHarnessPairReport =
        buildPairReport(
            baseReport = runDungeonLordHarness(seed = seed, preferredVariantId = null),
            variantReport = runDungeonLordHarness(seed = seed, preferredVariantId = "boss.variant.grey_crown"),
        )

    private fun runAbyssalGuardianPair(seed: Long): BossHarnessPairReport =
        buildPairReport(
            baseReport = runAbyssalGuardianHarness(seed = seed, preferredVariantId = null),
            variantReport = runAbyssalGuardianHarness(seed = seed, preferredVariantId = "boss.variant.abyssal_eclipse"),
        )

    private fun runMoltenGiantHarness(
        seed: Long,
        preferredVariantId: String?,
    ): BossHarnessReport {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = seed,
                        zoneId = "deep_iron_pit",
                        playerProfessionId = "vanguard",
                        bossVariantSelectionMode =
                            if (preferredVariantId == null) {
                                BossVariantSelectionMode.DISABLED
                            } else {
                                BossVariantSelectionMode.FORCE_AVAILABLE
                            },
                        preferredBossVariantId = preferredVariantId,
                    ),
                saveManager = SaveManager(tempDir.resolve("boss-molten-$seed-${preferredVariantId ?: "base"}")),
            )
        descendToBossFloor(session)
        val bossId = requireNotNull(session.automationEntityByTemplateId("orc.molten_giant"))
        val world = session.automationWorld()
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(findOpenAdjacentPoint(session, bossPoint))
        if (preferredVariantId != null) {
            session.automationGrantDiscoveryTags(setOf(ZoneTriggerFactIds.OIL_OR_FIRE_SEEN))
        }

        requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).current =
            requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).max * 49 / 100
        assertTrue(session.automationAdvanceBossPhaseForValidation(bossId))
        val phaseTelegraph = world.get<PendingTelegraphState>(bossId)
        waitForPhaseActionTrace(
            session = session,
            bossActorId = bossId.value,
            expectedPhaseId = "phase_enraged",
            expectedActionIds =
                if (preferredVariantId == null) {
                    setOf("earthshaker", "linebreaker", "strike")
                } else {
                    setOf("linebreaker")
                },
        )

        return buildReport(
            session = session,
            bossActorId = bossId.value,
            bossPoint = bossPoint,
            encounterId = "molten_giant_encounter",
            seed = seed,
            zoneId = "deep_iron_pit",
            templateId = "orc.molten_giant",
            telegraph = phaseTelegraph,
            phaseState = world.get<BossEncounterState>(bossId),
            expectedTelegraphKey = if (preferredVariantId == null) "molten_giant_phase_warning" else "molten_glass_phase_override_warning",
            expectedPhaseId = "phase_enraged",
            expectedSelectedActionIds =
                if (preferredVariantId == null) {
                    setOf("earthshaker", "linebreaker", "strike")
                } else {
                    setOf("linebreaker")
                },
            expectedBossTracePhaseId = "phase_enraged",
            expectedBossTraceSideEffect = if (preferredVariantId == null) "TELEGRAPH:molten_giant_phase_warning" else "PHASE_OVERRIDE_TELEGRAPH:molten_glass_phase_override_warning",
            expectedVariantId = preferredVariantId,
        )
    }

    private fun runDungeonLordHarness(
        seed: Long,
        preferredVariantId: String?,
    ): BossHarnessReport {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = seed,
                        zoneId = "grey_gate_depths",
                        playerProfessionId = "templar",
                        bossVariantSelectionMode =
                            if (preferredVariantId == null) {
                                BossVariantSelectionMode.DISABLED
                            } else {
                                BossVariantSelectionMode.FORCE_AVAILABLE
                            },
                        preferredBossVariantId = preferredVariantId,
                    ),
                saveManager = SaveManager(tempDir.resolve("boss-dungeon-$seed-${preferredVariantId ?: "base"}")),
            )
        descendToBossFloor(session)
        val bossId = requireNotNull(session.automationEntityByTemplateId("cultist.dungeon_lord"))
        val world = session.automationWorld()
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(findOpenPointAtDistance(session, bossPoint, minDistance = 5, maxDistance = 8))

        val bossHealth = requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId))
        if (preferredVariantId == "boss.variant.grey_crown") {
            val cooldowns = requireNotNull(world.get<com.ktome.core.talent.CooldownState>(bossId)).remainingByTalentId
            bossHealth.current = bossHealth.max * 3 / 5
            cooldowns.apply {
                this["battlefield_command"] = 99
                this["shadow_bind"] = 99
                this["ritual_break"] = 99
                this["arcane_shield"] = 99
                this["elite_war_call"] = 0
            }
            repeat(4) {
                if (world.get<com.ktome.core.talent.EffectTracker>(bossId)?.effects?.any { effect -> effect.schemaId == "war_cry_empower" } == true) {
                    return@repeat
                }
                assertTrue(session.perform(PlayerCommand.Wait))
            }
            session.automationMovePlayerTo(findOpenPointAtDistance(session, bossPoint, minDistance = 2, maxDistance = 4))
            cooldowns.apply {
                this["battlefield_command"] = 99
                this["shadow_bind"] = 99
                this["ritual_break"] = 0
                this["arcane_shield"] = 99
                this["elite_war_call"] = 99
            }
        } else {
            assertTrue(session.perform(PlayerCommand.Wait))
        }
        bossHealth.current = bossHealth.max * 40 / 100
        assertTrue(session.perform(PlayerCommand.Wait))
        val phaseTelegraph = world.get<PendingTelegraphState>(bossId)
        waitForPhaseActionTrace(
            session = session,
            bossActorId = bossId.value,
            expectedPhaseId = "phase_desperate",
            expectedActionIds =
                if (preferredVariantId == null) {
                    setOf("ritual_break", "arcane_shield", "close_quarters", "press_forward")
                } else {
                    setOf("war_call", "battlefield_command", "arcane_shield")
                },
        )

        return buildReport(
            session = session,
            bossActorId = bossId.value,
            bossPoint = bossPoint,
            encounterId = "dungeon_lord_encounter",
            seed = seed,
            zoneId = "grey_gate_depths",
            templateId = "cultist.dungeon_lord",
            telegraph = phaseTelegraph,
            phaseState = world.get<BossEncounterState>(bossId),
            expectedTelegraphKey = if (preferredVariantId == null) "dungeon_lord_phase_warning" else "grey_crown_phase_override_warning",
            expectedPhaseId = "phase_desperate",
            expectedSelectedActionIds =
                if (preferredVariantId == null) {
                    setOf("ritual_break", "arcane_shield", "close_quarters", "press_forward")
                } else {
                    setOf("war_call", "battlefield_command", "ritual_break", "arcane_shield")
                },
            expectedBossTracePhaseId = "phase_desperate",
            expectedBossTraceSideEffect = if (preferredVariantId == null) "TELEGRAPH:dungeon_lord_phase_warning" else "PHASE_OVERRIDE_TELEGRAPH:grey_crown_phase_override_warning",
            expectedVariantId = preferredVariantId,
        )
    }

    private fun runAbyssalGuardianHarness(
        seed: Long,
        preferredVariantId: String?,
    ): BossHarnessReport {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = seed,
                        zoneId = "abyssal_heart",
                        playerProfessionId = "vanguard",
                        bossVariantSelectionMode =
                            if (preferredVariantId == null) {
                                BossVariantSelectionMode.DISABLED
                            } else {
                                BossVariantSelectionMode.FORCE_AVAILABLE
                            },
                        preferredBossVariantId = preferredVariantId,
                    ),
                saveManager = SaveManager(tempDir.resolve("boss-abyssal-$seed-${preferredVariantId ?: "base"}")),
            )
        session.automationMovePlayerTo(interactablePoint(session, AbyssalRuntimeKeys.Finale.INTERACTABLE_ID))
        assertTrue(session.perform(PlayerCommand.Interact))
        val bossId = requireNotNull(session.automationEntityByTemplateId("abyssal.guardian"))
        val world = session.automationWorld()
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(findOpenAdjacentPoint(session, bossPoint))
        if (preferredVariantId != null) {
            session.automationGrantDiscoveryTags(setOf(ZoneTriggerFactIds.VOID_PRESSURE_ACTIVE))
        }

        assertTrue(session.perform(PlayerCommand.Wait))
        requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).current =
            requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).max * 35 / 100
        assertTrue(session.perform(PlayerCommand.Wait))
        val phaseTelegraph = world.get<PendingTelegraphState>(bossId)
        waitForPhaseActionTrace(
            session = session,
            bossActorId = bossId.value,
            expectedPhaseId = "phase_abyssal",
            expectedActionIds =
                if (preferredVariantId == null) {
                    setOf("void_breach", "abyssal_consecration", "linebreaker", "relentless_strike", "press_abyss")
                } else {
                    setOf("void_breach", "abyssal_consecration", "linebreaker")
                },
        )

        return buildReport(
            session = session,
            bossActorId = bossId.value,
            bossPoint = bossPoint,
            encounterId = "abyssal_guardian_encounter",
            seed = seed,
            zoneId = "abyssal_heart",
            templateId = "abyssal.guardian",
            telegraph = phaseTelegraph,
            phaseState = world.get<BossEncounterState>(bossId),
            expectedTelegraphKey = if (preferredVariantId == null) "abyssal_guardian_phase_warning" else "abyssal_eclipse_phase_override_warning",
            expectedPhaseId = "phase_abyssal",
            expectedSelectedActionIds =
                if (preferredVariantId == null) {
                    setOf("void_breach", "abyssal_consecration", "linebreaker", "relentless_strike", "press_abyss")
                } else {
                    setOf("void_breach", "abyssal_consecration", "linebreaker")
                },
            expectedBossTracePhaseId = "phase_abyssal",
            expectedBossTraceSideEffect = if (preferredVariantId == null) "TELEGRAPH:abyssal_guardian_phase_warning" else "PHASE_OVERRIDE_TELEGRAPH:abyssal_eclipse_phase_override_warning",
            expectedVariantId = preferredVariantId,
        )
    }

    private fun buildReport(
        session: FoundationGameSession,
        bossActorId: Int,
        bossPoint: Point,
        encounterId: String,
        seed: Long,
        zoneId: String,
        templateId: String,
        telegraph: PendingTelegraphState?,
        phaseState: BossEncounterState?,
        expectedTelegraphKey: String,
        expectedPhaseId: String,
        expectedSelectedActionIds: Set<String> = emptySet(),
        expectedBossTracePhaseId: String? = null,
        expectedBossTraceSideEffect: String? = null,
        expectedVariantId: String?,
    ): BossHarnessReport {
        val bossId = EntityId(bossActorId)
        val world = session.automationWorld()
        val aiTraces = session.recentAIDecisionTraces().filter { trace -> trace.actorId == bossActorId }
        val bossTraces = session.recentBossTraces().filter { trace -> trace.actorId == bossActorId }
        val bossJson = Json.encodeToString(bossTraces)
        val decodedAi = Json.decodeFromString<List<AIDecisionTrace>>(Json.encodeToString(aiTraces))
        val decodedBoss = Json.decodeFromString<List<BossTrace>>(bossJson)
        val phaseTransitionTurnId = decodedBoss.lastOrNull { trace -> trace.toPhase == expectedPhaseId }?.turnId
        val phaseScopedAi =
            phaseTransitionTurnId?.let { turnId ->
                decodedAi.filter { trace -> trace.turnId >= turnId }
            } ?: decodedAi
        val aiJson = Json.encodeToString(phaseScopedAi)
        val aiTraceHash = sha256(aiJson)
        val bossTraceHash = sha256(bossJson)
        val traceRoundTripMatches =
            Json.decodeFromString<List<AIDecisionTrace>>(aiJson) == phaseScopedAi &&
                decodedBoss == bossTraces
        val selectedActionIds = phaseScopedAi.mapNotNull(AIDecisionTrace::selectedActionId).distinct().sorted()
        val selectedActionSequence = phaseScopedAi.mapNotNull(AIDecisionTrace::selectedActionId)
        val telegraphMatches =
            telegraph != null &&
                (telegraph.sourceAbilityId == expectedTelegraphKey || telegraph.telegraphSpecId == expectedTelegraphKey)
        val aiTraceRequired = expectedSelectedActionIds.isNotEmpty()
        val bossTraceRequired = expectedBossTracePhaseId != null || expectedBossTraceSideEffect != null
        val expectedActionPresent =
            expectedSelectedActionIds.isEmpty() ||
                phaseScopedAi.any { trace -> trace.selectedActionId in expectedSelectedActionIds }
        val expectedBossPhaseTracePresent =
            expectedBossTracePhaseId == null ||
                decodedBoss.any { trace -> trace.toPhase == expectedBossTracePhaseId }
        val expectedBossSideEffectPresent =
            expectedBossTraceSideEffect == null ||
                decodedBoss.any { trace ->
                    (expectedBossTracePhaseId == null || trace.toPhase == expectedBossTracePhaseId) &&
                        expectedBossTraceSideEffect in trace.sideEffects
                }
        val variantRuntime = world.get<BossVariantRuntime>(bossId)
        val mutationLoadout = world.get<EliteMutationLoadout>(bossId)?.mutationIds?.sorted().orEmpty()
        val preferredTerrainTags = session.automationPreferredTerrainTags(bossId).map { terrainTag -> terrainTag.name }.sorted()
        val bossTerrainTags = session.automationTerrainTagsAt(bossPoint).map { terrainTag -> terrainTag.name }.sorted()
        val adjacentBossTerrainTags = session.automationAdjacentTerrainTagsAt(bossPoint).map { terrainTag -> terrainTag.name }.sorted()
        val terrainPreferenceAvailable =
            preferredTerrainTags.isNotEmpty() &&
                session.automationTerrainTags().values.any { terrainTags -> terrainTags.any { terrainTag -> terrainTag.name in preferredTerrainTags } }
        val terrainPreferenceImplemented =
            terrainPreferenceImplemented(
                preferredTerrainTags = preferredTerrainTags.toSet(),
                directTerrainTags = bossTerrainTags.toSet(),
                adjacentTerrainTags = adjacentBossTerrainTags.toSet(),
            )
        val inspectPoint = world.get<Position>(bossId)?.toPoint() ?: bossPoint
        val inspectActor = session.inspectAt(inspectPoint).actor
        val snapshot = session.renderSnapshot()
        val inspectReadable =
            if (expectedVariantId == null) {
                inspectActor?.bossVariant == null
            } else {
                inspectActor?.bossVariant?.id == expectedVariantId &&
                    inspectActor.mutations.map { mutation -> mutation.id }.sorted() == mutationLoadout
            }
        val logReadable =
            if (expectedVariantId == null) {
                true
            } else {
                snapshot.logEvents.any { event ->
                    event.message.key == "log.boss.variant.applied" &&
                        event.message.arguments.any { argument -> argument.valueKey == "$expectedVariantId.name" }
                }
            }
        val phaseSequence = phaseSequence(decodedBoss, phaseState)
        val phaseTransitionTriggers = decodedBoss.map { trace -> "${trace.fromPhase ?: "START"}->${trace.toPhase}:${trace.trigger}" }.distinct()
        val phaseTransitionObserved = decodedBoss.any { trace -> trace.toPhase == expectedPhaseId }
        val phaseOverrideExpectation = expectedVariantId?.let(bossRegistryMetrics.phaseOverrideExpectationsByVariant::getValue)
        val phaseOverridePhaseIds = phaseState?.phaseOverrideTriggeredPhaseIds?.sorted().orEmpty()
        val phaseOverrideRuntimeTriggered =
            phaseOverrideExpectation?.let { expectation -> expectation.phaseId in phaseOverridePhaseIds } ?: false
        val phaseOverrideTelegraphTriggered =
            phaseOverrideExpectation?.let { expectation ->
                decodedBoss.any { trace ->
                    "PHASE_OVERRIDE_TELEGRAPH:${expectation.telegraphSpecId}" in trace.sideEffects
                }
            } ?: false
        val phaseOverrideActionEmphasisIds =
            decodedBoss
                .asSequence()
                .flatMap { trace -> trace.sideEffects.asSequence() }
                .filter { sideEffect -> sideEffect.startsWith("PHASE_OVERRIDE_ACTION_EMPHASIS:") }
                .flatMap { sideEffect -> sideEffect.removePrefix("PHASE_OVERRIDE_ACTION_EMPHASIS:").splitToSequence(",") }
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
                .toList()
        val phaseOverrideActionEmphasisApplied =
            phaseOverrideExpectation?.let { expectation ->
                expectation.actionEmphasisIds.all { actionId -> actionId in phaseOverrideActionEmphasisIds }
            } ?: false
        val phaseOverrideRuntimeRequirementSatisfied =
            phaseOverrideExpectation == null || phaseOverrideRuntimeTriggered
        val phaseOverrideTelegraphRequirementSatisfied =
            phaseOverrideExpectation == null || phaseOverrideTelegraphTriggered
        val phaseOverrideActionEmphasisRequirementSatisfied =
            phaseOverrideExpectation == null || phaseOverrideActionEmphasisApplied
        val phaseOverrideSkippedReason =
            decodedBoss
                .asSequence()
                .flatMap { trace -> trace.sideEffects.asSequence() }
                .lastOrNull { sideEffect -> sideEffect.startsWith("PHASE_OVERRIDE_SKIPPED:") }
                ?.removePrefix("PHASE_OVERRIDE_SKIPPED:")
        val encounterThreatBudget = session.automationEncounterThreatBudget(bossId)
        val floorRewardBudget = session.automationFloorRewardBudget()
        val variantMatches =
            when {
                expectedVariantId == null -> variantRuntime == null
                else -> variantRuntime?.variantId == expectedVariantId
            }
        val success =
            telegraphMatches &&
                (!aiTraceRequired || phaseScopedAi.isNotEmpty()) &&
                (!bossTraceRequired || decodedBoss.isNotEmpty()) &&
                traceRoundTripMatches &&
                expectedActionPresent &&
                expectedBossPhaseTracePresent &&
                expectedBossSideEffectPresent &&
                variantMatches &&
                phaseOverrideRuntimeRequirementSatisfied &&
                phaseOverrideTelegraphRequirementSatisfied &&
                phaseOverrideActionEmphasisRequirementSatisfied &&
                phaseTransitionObserved &&
                (!terrainPreferenceAvailable || terrainPreferenceImplemented) &&
                inspectReadable &&
                logReadable
        val failureReason =
            when {
                telegraph == null -> "Missing pending telegraph."
                !telegraphMatches -> "Expected telegraph '$expectedTelegraphKey' but got ${telegraph.sourceAbilityId}/${telegraph.telegraphSpecId}."
                aiTraceRequired && phaseScopedAi.isEmpty() -> "Missing AI decision traces after phase transition."
                bossTraceRequired && decodedBoss.isEmpty() -> "Missing boss traces."
                !traceRoundTripMatches -> "Trace payload changed after JSON round trip."
                !expectedActionPresent -> "Missing expected AI action trace after phase transition in ${expectedSelectedActionIds.sorted()}."
                !expectedBossPhaseTracePresent -> "Missing boss phase trace '$expectedBossTracePhaseId'."
                !expectedBossSideEffectPresent -> "Missing boss trace side effect '$expectedBossTraceSideEffect'."
                !variantMatches -> "Expected variant ${expectedVariantId ?: "base"} but got ${variantRuntime?.variantId ?: "base"}."
                !phaseOverrideRuntimeRequirementSatisfied -> "Missing phase override trigger for ${phaseOverrideExpectation?.phaseId}."
                !phaseOverrideTelegraphRequirementSatisfied -> "Missing phase override telegraph for ${phaseOverrideExpectation?.telegraphSpecId}."
                !phaseOverrideActionEmphasisRequirementSatisfied -> "Missing phase override action emphasis for ${phaseOverrideExpectation?.actionEmphasisIds}."
                !phaseTransitionObserved -> "Boss phase transition was not observed in the trace payload."
                terrainPreferenceAvailable && !terrainPreferenceImplemented ->
                    "Boss spawn did not land on preferred terrain or an adjacent tactical tile."
                !inspectReadable -> "Inspect view did not expose the expected variant/mutation metadata."
                !logReadable -> "Render log did not expose the expected boss variant token."
                else -> null
            }
        return BossHarnessReport(
            seed = seed,
            localeId = session.localizer().locale.id,
            zoneId = zoneId,
            templateId = templateId,
            encounterId = encounterId,
            variantId = variantRuntime?.variantId,
            grantedMutations = mutationLoadout,
            threatCost = variantRuntime?.threatCost ?: 0,
            lootProfileOverride = variantRuntime?.lootProfileOverride,
            lootProfileRewardBudget = variantRuntime?.lootProfileOverride?.let(rewardBudgetsByLootProfileId::get),
            actionWeightProfileId = variantRuntime?.actionWeightProfileId,
            visualTintKey = variantRuntime?.visualTintKey,
            renderTintColorHex = null,
            preferredTerrainTags = preferredTerrainTags,
            bossTerrainTags = bossTerrainTags,
            adjacentBossTerrainTags = adjacentBossTerrainTags,
            terrainPreferenceAvailable = terrainPreferenceAvailable,
            terrainPreferenceImplemented = terrainPreferenceImplemented,
            success = success,
            phaseId = phaseState?.currentPhaseId,
            telegraphKey = telegraph?.sourceAbilityId ?: telegraph?.telegraphSpecId,
            phaseSequence = phaseSequence,
            phaseTransitionTriggers = phaseTransitionTriggers,
            phaseTransitionObserved = phaseTransitionObserved,
            phaseOverridePhaseIds = phaseOverridePhaseIds,
            phaseOverrideTelegraphKey = phaseOverrideExpectation?.telegraphSpecId?.takeIf { phaseOverrideTelegraphTriggered },
            phaseOverrideRuntimeTriggered = phaseOverrideRuntimeTriggered,
            phaseOverrideTelegraphTriggered = phaseOverrideTelegraphTriggered,
            phaseOverrideActionEmphasisIds = phaseOverrideActionEmphasisIds,
            phaseOverrideSkippedReason = phaseOverrideSkippedReason,
            expectedSelectedActions = expectedSelectedActionIds.sorted(),
            selectedActionIds = selectedActionIds,
            selectedActionSequence = selectedActionSequence,
            requiredAiTraceCount = if (aiTraceRequired) 1 else 0,
            observedAiTraceCount = phaseScopedAi.size,
            aiTraceCount = phaseScopedAi.size,
            bossTraceCount = decodedBoss.size,
            aiTraceHash = aiTraceHash,
            bossTraceHash = bossTraceHash,
            aiTracePayload = Json.parseToJsonElement(aiJson),
            bossTracePayload = Json.parseToJsonElement(bossJson),
            encounterThreatBudget = encounterThreatBudgetToJson(encounterThreatBudget),
            floorRewardBudgetDelta = floorRewardBudgetToJson(floorRewardBudget),
            inspectReadable = inspectReadable,
            logReadable = logReadable,
            failureReason = failureReason,
        )
    }

    private fun buildPairReport(
        baseReport: BossHarnessReport,
        variantReport: BossHarnessReport,
    ): BossHarnessPairReport {
        val expectedVariantId = requireNotNull(variantReport.variantId) { "Variant harness report must carry a variant id." }
        val basePhaseIds = bossRegistryMetrics.bossVariantBasePhaseIds.getValue(expectedVariantId)
        val phaseGraphUnchanged =
            baseReport.encounterId == variantReport.encounterId &&
                variantReport.phaseOverridePhaseIds.all { phaseId -> phaseId in basePhaseIds }
        val phaseGraphStructuralDiffCount = if (phaseGraphUnchanged) 0 else 1
        val threatLedgerMatched =
            variantReport.encounterThreatBudget.jsonObject.getValue("threatDeltas").toString().contains("bossVariant:$expectedVariantId") &&
                variantReport.encounterThreatBudget.jsonObject.getValue("totalBudget").jsonPrimitive.content.toInt() >=
                variantReport.encounterThreatBudget.jsonObject.getValue("baseBudget").jsonPrimitive.content.toInt() + variantReport.threatCost
        val rewardLedgerEntry =
            variantReport.floorRewardBudgetDelta.jsonObject
                .getValue("rewardDeltas")
                .jsonArray
                .firstOrNull { delta ->
                    delta.jsonObject.getValue("source").jsonPrimitive.content == "bossVariant:$expectedVariantId"
                }?.jsonObject
        val rewardLedgerMatched =
            variantReport.lootProfileOverride == null ||
                (
                    rewardLedgerEntry != null &&
                        rewardLedgerEntry.getValue("amount").jsonPrimitive.content.toInt() == variantReport.lootProfileRewardBudget
                )
        val grantedMutationsRegistered = variantReport.grantedMutations.all { mutationId -> mutationId.startsWith("elite.") }
        val traceDiverged = baseReport.aiTraceHash != variantReport.aiTraceHash
        val selectedActionSequenceDiverged = baseReport.selectedActionSequence != variantReport.selectedActionSequence
        val actionTraceDivergenceScore =
            actionTraceDivergenceScore(
                baseReport.selectedActionSequence,
                variantReport.selectedActionSequence,
            )
        val success =
            baseReport.success &&
                variantReport.success &&
                phaseGraphUnchanged &&
                baseReport.phaseTransitionObserved &&
                variantReport.phaseTransitionObserved &&
                traceDiverged &&
                selectedActionSequenceDiverged &&
                actionTraceDivergenceScore >= MIN_VARIANT_ACTION_TRACE_DIVERGENCE_SCORE &&
                grantedMutationsRegistered &&
                threatLedgerMatched &&
                rewardLedgerMatched &&
                variantReport.phaseOverrideRuntimeTriggered &&
                variantReport.phaseOverrideTelegraphTriggered &&
                variantReport.inspectReadable &&
                variantReport.logReadable
        val failureReason =
            when {
                !baseReport.success -> "Base report failed: ${baseReport.failureReason}"
                !variantReport.success -> "Variant report failed: ${variantReport.failureReason}"
                !phaseGraphUnchanged -> "Boss variant phase override targets a phase outside the base encounter graph."
                !baseReport.phaseTransitionObserved || !variantReport.phaseTransitionObserved -> "Base and variant reports must both observe a boss phase transition."
                !traceDiverged -> "Variant AI trace did not diverge from the base trace."
                !selectedActionSequenceDiverged -> "Variant selected action sequence did not diverge from the base sequence."
                actionTraceDivergenceScore < MIN_VARIANT_ACTION_TRACE_DIVERGENCE_SCORE ->
                    "Variant selected action sequence divergence score $actionTraceDivergenceScore is below $MIN_VARIANT_ACTION_TRACE_DIVERGENCE_SCORE."
                !grantedMutationsRegistered -> "Variant references a non-registry mutation id."
                !threatLedgerMatched -> "Variant threatCost is missing from the encounter ledger."
                !rewardLedgerMatched -> "Variant lootProfileOverride is missing from the reward ledger or uses the wrong rewardBudget amount."
                !variantReport.phaseOverrideRuntimeTriggered -> "Variant phase override runtime trigger was not observed."
                !variantReport.phaseOverrideTelegraphTriggered -> "Variant phase override telegraph was not observed."
                !variantReport.inspectReadable -> "Variant metadata is not inspect-readable."
                !variantReport.logReadable -> "Variant metadata is not log-readable."
                else -> null
            }
        return BossHarnessPairReport(
            joinKey = WhiteBoxJoinKey(scenarioId = "pair:${baseReport.encounterId}:$expectedVariantId"),
            baseReport = baseReport,
            variantReport = variantReport,
            phaseGraphUnchanged = phaseGraphUnchanged,
            phaseGraphStructuralDiffCount = phaseGraphStructuralDiffCount.coerceAtLeast(0),
            traceDiverged = traceDiverged,
            selectedActionSequenceDiverged = selectedActionSequenceDiverged,
            actionTraceDivergenceScore = actionTraceDivergenceScore,
            threatLedgerMatched = threatLedgerMatched,
            rewardLedgerMatched = rewardLedgerMatched,
            phaseOverrideRuntimeTriggered = variantReport.phaseOverrideRuntimeTriggered,
            phaseOverrideTelegraphTriggered = variantReport.phaseOverrideTelegraphTriggered,
            phaseOverrideActionEmphasisIds = variantReport.phaseOverrideActionEmphasisIds,
            inspectReadable = variantReport.inspectReadable,
            logReadable = variantReport.logReadable,
            success = success,
            failureReason = failureReason,
        )
    }

    private fun bossAggregates(pairReports: List<BossHarnessPairReport>): List<WhiteBoxAggregateReport> {
        val phaseTransitionObservedRatio = phaseTransitionObservedRatio(pairReports)
        val variantTraceDivergenceRatio = variantTraceDivergenceRatio(pairReports)
        val minVariantActionTraceDivergenceScore = pairReports.minOf(BossHarnessPairReport::actionTraceDivergenceScore)
        val phaseOverrideSchemaCoverage = bossRegistryMetrics.phaseOverrideSchemaCoverage
        val phaseOverrideRuntimeTriggerCoverage = phaseOverrideRuntimeTriggerCoverage(pairReports)
        val phaseOverrideTelegraphCoverage = phaseOverrideTelegraphCoverage(pairReports)
        val phaseOverrideActionDistinctCountsByVariant = phaseOverrideActionDistinctCountsByVariant(pairReports)
        val phaseOverrideActionDistinctMin = phaseOverrideActionDistinctCountsByVariant.values.minOrNull() ?: 0
        val phaseGraphUnchanged = pairReports.all(BossHarnessPairReport::phaseGraphUnchanged)
        val phaseGraphUnchangedReason =
            if (phaseGraphUnchanged) {
                "data_level_override_only"
            } else {
                "phase_graph_structural_diff"
            }
        return listOf(
            WhiteBoxAggregateReport(
                groupId = "per-encounter",
                sampleCount = pairReports.size,
                metrics =
                    buildJsonObject {
                        put("pairCount", pairReports.size)
                        put("phaseGraphStructuralDiffCount", pairReports.sumOf(BossHarnessPairReport::phaseGraphStructuralDiffCount))
                        put("phaseTransitionObservedRatio", phaseTransitionObservedRatio)
                        put("variantTraceDivergenceRatio", variantTraceDivergenceRatio)
                        put("minVariantActionTraceDivergenceScore", minVariantActionTraceDivergenceScore)
                        put("phaseOverrideSchemaCoverage", phaseOverrideSchemaCoverage)
                        put("phaseOverrideRuntimeTriggerCoverage", phaseOverrideRuntimeTriggerCoverage)
                        put("phaseOverrideTelegraphCoverage", phaseOverrideTelegraphCoverage)
                        put("phaseOverrideActionDistinctCount", phaseOverrideActionDistinctMin)
                        put("phaseOverrideActionDistinctMin", phaseOverrideActionDistinctMin)
                        putJsonObject("phaseOverrideActionDistinctCountsByVariant") {
                            phaseOverrideActionDistinctCountsByVariant.forEach { (variantId, actionCount) ->
                                put(variantId, actionCount)
                            }
                        }
                        put("phaseGraphUnchanged", phaseGraphUnchanged)
                        put("phaseGraphUnchangedReason", phaseGraphUnchangedReason)
                        put("bossVariantStructuralDivergenceNote", "phaseGraphUnchanged=$phaseGraphUnchanged; phaseOverrides target declared base encounter phase ids.")
                        put("variantCount", pairReports.count { pair -> pair.variantReport.variantId != null })
                        put("terrainPreferenceVariantCount", pairReports.count { pair -> pair.variantReport.preferredTerrainTags.isNotEmpty() })
                        put("terrainPreferenceAvailableVariantCount", pairReports.count { pair -> pair.variantReport.terrainPreferenceAvailable })
                        put(
                            "terrainPreferenceImplementedCount",
                            pairReports.count { pair ->
                                pair.variantReport.terrainPreferenceAvailable && pair.variantReport.terrainPreferenceImplemented
                            },
                        )
                    },
                assertions =
                    listOf(
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.base_variant_pairs_present",
                            passed = pairReports.all { pair -> pair.baseReport.variantId == null && pair.variantReport.variantId != null },
                            message = "Each PR-06 boss variant has a base-vs-variant contrast sample.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.phase_graph_diff_zero",
                            passed = pairReports.sumOf(BossHarnessPairReport::phaseGraphStructuralDiffCount) == 0,
                            message = "All boss variant phase overrides target declared base encounter phase ids.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.phase_transition_observed_ratio",
                            passed = phaseTransitionObservedRatio >= MIN_PHASE_TRANSITION_OBSERVED_RATIO,
                            message = "All base-vs-variant samples observe a formal boss phase transition.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.variant_trace_divergence",
                            passed = variantTraceDivergenceRatio >= MIN_VARIANT_TRACE_DIVERGENCE_RATIO,
                            message = "Every boss variant diverges from its base AI trace and selected action sequence.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.preferred_terrain_applied",
                            passed =
                                pairReports.all { pair ->
                                    !pair.variantReport.terrainPreferenceAvailable || pair.variantReport.terrainPreferenceImplemented
                                },
                            message = "Every boss variant with explicit terrain affinity lands on preferred terrain or an adjacent tactical tile.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.phase_override_schema_coverage",
                            passed = phaseOverrideSchemaCoverage >= 1.0,
                            message = "All formal boss variants declare at least one phase override.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.phase_override_runtime_trigger_coverage",
                            passed = phaseOverrideRuntimeTriggerCoverage >= 1.0,
                            message = "All formal boss variant phase overrides are triggered by bossHarness runtime samples.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.phase_override_telegraph_coverage",
                            passed = phaseOverrideTelegraphCoverage >= 1.0,
                            message = "All formal boss variant phase override telegraph specs are observed in runtime trace.",
                        ),
                    ),
            ),
            WhiteBoxAggregateReport(
                groupId = "corpus",
                sampleCount = pairReports.size,
                metrics =
                    buildJsonObject {
                        put("pairCount", pairReports.size)
                        put("threatLedgerMatchedCount", pairReports.count(BossHarnessPairReport::threatLedgerMatched))
                        put("rewardLedgerMatchedCount", pairReports.count(BossHarnessPairReport::rewardLedgerMatched))
                        put("inspectReadableCount", pairReports.count(BossHarnessPairReport::inspectReadable))
                        put("logReadableCount", pairReports.count(BossHarnessPairReport::logReadable))
                        put("eliteMutationDistinctCount", bossRegistryMetrics.eliteMutationDistinctCount)
                        put("eliteMutationValidPairCount", bossRegistryMetrics.eliteMutationValidPairCount)
                        put("bossVariantCount", bossRegistryMetrics.bossVariantCount)
                        put("phaseOverrideVariantCount", bossRegistryMetrics.phaseOverrideVariantCount)
                        put("bossVariantMutationPairwiseDistinct", bossRegistryMetrics.bossVariantMutationPairwiseDistinct)
                        put("bossVariantBasePhaseCountMin", bossRegistryMetrics.bossVariantBasePhaseCounts.values.minOrNull() ?: 0)
                        put("phaseTransitionObservedRatio", phaseTransitionObservedRatio)
                        put("variantTraceDivergenceRatio", variantTraceDivergenceRatio)
                        put("minVariantActionTraceDivergenceScore", minVariantActionTraceDivergenceScore)
                        put("phaseOverrideVariantDenominator", bossRegistryMetrics.phaseOverrideVariantDenominator)
                        put("phaseOverrideSchemaCount", bossRegistryMetrics.phaseOverrideVariantCount)
                        put("phaseOverrideSchemaCoverage", phaseOverrideSchemaCoverage)
                        put("phaseOverrideRuntimeTriggerCount", pairReports.count(BossHarnessPairReport::phaseOverrideRuntimeTriggered))
                        put("phaseOverrideRuntimeTriggerCoverage", phaseOverrideRuntimeTriggerCoverage)
                        put("phaseOverrideTelegraphCount", pairReports.count(BossHarnessPairReport::phaseOverrideTelegraphTriggered))
                        put("phaseOverrideTelegraphCoverage", phaseOverrideTelegraphCoverage)
                        put("phaseOverrideActionDistinctCount", phaseOverrideActionDistinctMin)
                        put("phaseOverrideActionDistinctMin", phaseOverrideActionDistinctMin)
                        putJsonObject("phaseOverrideActionDistinctCountsByVariant") {
                            phaseOverrideActionDistinctCountsByVariant.forEach { (variantId, actionCount) ->
                                put(variantId, actionCount)
                            }
                        }
                        put("phaseGraphUnchanged", phaseGraphUnchanged)
                        put("phaseGraphUnchangedReason", phaseGraphUnchangedReason)
                        put("bossVariantStructuralDivergenceNote", "phaseGraphUnchanged=$phaseGraphUnchanged; phaseOverrides target declared base encounter phase ids.")
                        put("terrainPreferenceVariantCount", pairReports.count { pair -> pair.variantReport.preferredTerrainTags.isNotEmpty() })
                        put("terrainPreferenceAvailableVariantCount", pairReports.count { pair -> pair.variantReport.terrainPreferenceAvailable })
                        put(
                            "terrainPreferenceImplementedCount",
                            pairReports.count { pair ->
                                pair.variantReport.terrainPreferenceAvailable && pair.variantReport.terrainPreferenceImplemented
                            },
                        )
                        put(
                            "terrainPreferenceImplementedRate",
                            pairReports.let { reports ->
                                val variantsWithAvailability = reports.count { pair -> pair.variantReport.terrainPreferenceAvailable }
                                if (variantsWithAvailability == 0) {
                                    0.0
                                } else {
                                    reports.count { pair ->
                                        pair.variantReport.terrainPreferenceAvailable && pair.variantReport.terrainPreferenceImplemented
                                    }.toDouble() / variantsWithAvailability.toDouble()
                                }
                            },
                        )
                        putJsonObject("mutationTierDistribution") {
                            bossRegistryMetrics.mutationTierDistribution.forEach { (tierId, count) ->
                                put(tierId, count)
                            }
                        }
                        putJsonObject("bossVariantMutationSets") {
                            bossRegistryMetrics.bossVariantMutationSets.forEach { (variantId, mutationIds) ->
                                putJsonArray(variantId) {
                                    mutationIds.forEach { mutationId -> add(JsonPrimitive(mutationId)) }
                                }
                            }
                        }
                        putJsonObject("bossVariantPreferredTerrainTags") {
                            bossRegistryMetrics.bossVariantPreferredTerrainTags.forEach { (variantId, terrainTags) ->
                                putJsonArray(variantId) {
                                    terrainTags.forEach { terrainTag -> add(JsonPrimitive(terrainTag)) }
                                }
                            }
                        }
                        putJsonObject("bossVariantBasePhaseCounts") {
                            bossRegistryMetrics.bossVariantBasePhaseCounts.forEach { (variantId, phaseCount) ->
                                put(variantId, phaseCount)
                            }
                        }
                    },
                assertions =
                    listOf(
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.threat_cost_traceable",
                            passed = pairReports.all(BossHarnessPairReport::threatLedgerMatched),
                            message = "Every variant threatCost is traceable in the encounter ledger.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.loot_override_traceable",
                            passed = pairReports.all(BossHarnessPairReport::rewardLedgerMatched),
                            message = "Every variant lootProfileOverride is traceable in the floor reward ledger.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.variant_readability",
                            passed = pairReports.all { pair -> pair.inspectReadable && pair.logReadable },
                            message = "Every variant can be traced from inspect/log/visual cue metadata.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.mutation_count",
                            passed = bossRegistryMetrics.eliteMutationDistinctCount >= 12,
                            message = "Elite mutation registry reaches the OPT PR-02 target count of at least 12.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.valid_pair_count",
                            passed = bossRegistryMetrics.eliteMutationValidPairCount >= 40,
                            message = "Elite mutation registry exposes at least 40 valid non-forbidden pairs.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.tier_balance",
                            passed =
                                bossRegistryMetrics.mutationTierDistribution["MINOR"].orZero() >= 2 &&
                                    bossRegistryMetrics.mutationTierDistribution["MAJOR"].orZero() >= 5 &&
                                    bossRegistryMetrics.mutationTierDistribution["SIGNATURE"].orZero() >= 2,
                            message = "Mutation tier distribution satisfies MINOR >= 2, MAJOR >= 5, SIGNATURE >= 2.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.variant_pairwise_distinct",
                            passed = bossRegistryMetrics.bossVariantMutationPairwiseDistinct,
                            message = "All formal boss variants keep pairwise-distinct mutation combinations.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.variant_base_has_two_phases",
                            passed = bossRegistryMetrics.bossVariantBasePhaseCounts.values.all { phaseCount -> phaseCount >= 2 },
                            message = "Every formal boss variant targets a base encounter with at least two phases.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.phase_transition_observed_ratio",
                            passed = phaseTransitionObservedRatio >= MIN_PHASE_TRANSITION_OBSERVED_RATIO,
                            message = "phaseTransitionObservedRatio reaches the V3 PR-04 owner threshold.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.variant_trace_divergence_threshold",
                            passed = variantTraceDivergenceRatio >= MIN_VARIANT_TRACE_DIVERGENCE_RATIO,
                            message = "variant/base trace divergence reaches the V3 PR-04 owner threshold.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.variant_preferred_terrain_applied",
                            passed =
                                pairReports.all { pair ->
                                    !pair.variantReport.terrainPreferenceAvailable || pair.variantReport.terrainPreferenceImplemented
                                },
                            message = "Boss variants with explicit terrain affinity are realized on preferred terrain or an adjacent tactical tile.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.phase_override_schema_coverage",
                            passed = phaseOverrideSchemaCoverage >= 1.0,
                            message = "phaseOverrideSchemaCoverage reaches the PR-05 owner threshold.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.phase_override_runtime_trigger_coverage",
                            passed = phaseOverrideRuntimeTriggerCoverage >= 1.0,
                            message = "phaseOverrideRuntimeTriggerCoverage reaches the PR-05 owner threshold.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "boss.aggregate.phase_override_telegraph_coverage",
                            passed = phaseOverrideTelegraphCoverage >= 1.0,
                            message = "phaseOverrideTelegraphCoverage reaches the PR-05 owner threshold.",
                        ),
                    ),
            ),
        )
    }

    private fun phaseTransitionObservedRatio(pairReports: List<BossHarnessPairReport>): Double {
        val reports = pairReports.flatMap { pair -> listOf(pair.baseReport, pair.variantReport) }
        if (reports.isEmpty()) {
            return 0.0
        }
        return reports.count(BossHarnessReport::phaseTransitionObserved).toDouble() / reports.size.toDouble()
    }

    private fun variantTraceDivergenceRatio(pairReports: List<BossHarnessPairReport>): Double {
        if (pairReports.isEmpty()) {
            return 0.0
        }
        return pairReports
            .count(BossHarnessPairReport::variantTraceDivergencePassed)
            .toDouble() / pairReports.size.toDouble()
    }

    private fun phaseOverrideRuntimeTriggerCoverage(pairReports: List<BossHarnessPairReport>): Double =
        if (bossRegistryMetrics.phaseOverrideVariantDenominator == 0) {
            0.0
        } else {
            pairReports.count(BossHarnessPairReport::phaseOverrideRuntimeTriggered).toDouble() /
                bossRegistryMetrics.phaseOverrideVariantDenominator.toDouble()
        }

    private fun phaseOverrideTelegraphCoverage(pairReports: List<BossHarnessPairReport>): Double =
        if (bossRegistryMetrics.phaseOverrideVariantDenominator == 0) {
            0.0
        } else {
            pairReports.count(BossHarnessPairReport::phaseOverrideTelegraphTriggered).toDouble() /
                bossRegistryMetrics.phaseOverrideVariantDenominator.toDouble()
        }

    private fun phaseOverrideActionDistinctCountsByVariant(pairReports: List<BossHarnessPairReport>): Map<String, Int> =
        bossRegistryMetrics.phaseOverrideExpectationsByVariant.keys.associateWith { variantId ->
            pairReports
                .filter { pair -> pair.variantReport.variantId == variantId }
                .flatMap(BossHarnessPairReport::phaseOverrideActionEmphasisIds)
                .distinct()
                .size
        }.toSortedMap()

    private fun descendToBossFloor(session: FoundationGameSession) {
        val stairsDown = requireNotNull(session.automationStairPoint(com.ktome.core.dungeon.StairDirection.DOWN))
        session.automationMovePlayerTo(stairsDown)
        assertTrue(session.perform(PlayerCommand.Descend))
    }

    private fun triggerTemplarHealFeedback(session: FoundationGameSession) {
        val world = session.automationWorld()
        val playerHealth = requireNotNull(world.get<com.ktome.core.ecs.Health>(session.playerId))
        playerHealth.current = (playerHealth.max / 2).coerceAtLeast(1)
        val positiveEnergyPool =
            requireNotNull(requireNotNull(world.get<ResourcePools>(session.playerId)).pool(ResourceType.POSITIVE_ENERGY)) {
                "Expected templar positive energy pool for feedback harness."
            }
        positiveEnergyPool.current = positiveEnergyPool.max
        val holyLightSlot = requireNotNull(session.talentSlots().firstOrNull { slot -> slot.talentId == "holy_light" }) {
            "Expected templar holy_light slot for feedback harness."
        }.slot
        assertTrue(session.perform(PlayerCommand.UseTalent(slot = holyLightSlot)))
    }

    private fun findOpenAdjacentPoint(
        session: FoundationGameSession,
        center: Point,
    ): Point {
        val world = session.automationWorld()
        val occupied = world.entitiesWith(Position::class).mapTo(linkedSetOf()) { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
        return Point.ALL_DIRECTIONS
            .map { delta -> center + delta }
            .first { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            }
    }

    private fun findOpenPointAtDistance(
        session: FoundationGameSession,
        center: Point,
        minDistance: Int,
        maxDistance: Int,
    ): Point {
        val world = session.automationWorld()
        val occupied = world.entitiesWith(Position::class).mapTo(linkedSetOf()) { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
        return (0 until session.map.height).asSequence()
            .flatMap { y -> (0 until session.map.width).asSequence().map { x -> Point(x, y) } }
            .filter { point ->
                point.chebyshevDistanceTo(center) in minDistance..maxDistance &&
                    session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            }.sortedWith(compareByDescending<Point> { point -> point.chebyshevDistanceTo(center) }.thenBy(Point::y).thenBy(Point::x))
            .first()
    }

    private fun waitForPhaseActionTrace(
        session: FoundationGameSession,
        bossActorId: Int,
        expectedPhaseId: String,
        expectedActionIds: Set<String>,
    ) {
        repeat(20) {
            val transitionTurnId =
                session
                    .recentBossTraces()
                    .lastOrNull { trace -> trace.actorId == bossActorId && trace.toPhase == expectedPhaseId }
                    ?.turnId
            if (
                transitionTurnId != null &&
                session
                    .recentAIDecisionTraces()
                    .any { trace ->
                        trace.actorId == bossActorId &&
                            trace.turnId >= transitionTurnId &&
                            trace.selectedActionId in expectedActionIds
                    }
            ) {
                return
            }
            if (session.runOutcome().isTerminal) {
                return
            }
            assertTrue(session.perform(PlayerCommand.Wait))
        }
    }

    private fun phaseSequence(
        bossTraces: List<BossTrace>,
        phaseState: BossEncounterState?,
    ): List<String> =
        buildList {
            bossTraces.firstOrNull()?.fromPhase?.let(::add)
            bossTraces.map(BossTrace::toPhase).forEach { phaseId ->
                if (phaseId !in this) {
                    add(phaseId)
                }
            }
            if (isEmpty()) {
                phaseState?.currentPhaseId?.let(::add)
            }
        }

    private fun actionTraceDivergenceScore(
        baseSequence: List<String>,
        variantSequence: List<String>,
    ): Double {
        val denominator = max(baseSequence.size, variantSequence.size)
        if (denominator == 0) {
            return 0.0
        }
        val differenceCount =
            (0 until denominator).count { index ->
                baseSequence.getOrNull(index) != variantSequence.getOrNull(index)
            }
        return differenceCount.toDouble() / denominator.toDouble()
    }

    private fun encounterThreatBudgetToJson(budget: EncounterThreatBudget): JsonElement =
        buildJsonObject {
            put("encounterId", budget.encounterId)
            put("baseBudget", budget.baseBudget)
            put("totalBudget", budget.totalBudget)
            putJsonArray("threatDeltas") {
                budget.threatDeltas.forEach { delta ->
                    add(buildJsonObject {
                        put("source", delta.source)
                        put("amount", delta.amount)
                    })
                }
            }
        }

    private fun floorRewardBudgetToJson(budget: FloorRewardBudget): JsonElement =
        buildJsonObject {
            put("zoneId", budget.zoneId)
            put("floorIndex", budget.floorIndex)
            put("baseBudget", budget.baseBudget)
            put("totalBudget", budget.totalBudget)
            putJsonArray("rewardDeltas") {
                budget.rewardDeltas.forEach { delta ->
                    add(buildJsonObject {
                        put("source", delta.source)
                        put("amount", delta.amount)
                    })
                }
            }
        }
}

private data class BossHarnessReport(
    val seed: Long,
    val localeId: String,
    val zoneId: String,
    val templateId: String,
    val encounterId: String,
    val variantId: String?,
    val grantedMutations: List<String>,
    val threatCost: Int,
    val lootProfileOverride: String?,
    val lootProfileRewardBudget: Int?,
    val actionWeightProfileId: String?,
    val visualTintKey: String?,
    val renderTintColorHex: String?,
    val preferredTerrainTags: List<String>,
    val bossTerrainTags: List<String>,
    val adjacentBossTerrainTags: List<String>,
    val terrainPreferenceAvailable: Boolean,
    val terrainPreferenceImplemented: Boolean,
    val success: Boolean,
    val phaseId: String?,
    val telegraphKey: String?,
    val phaseSequence: List<String>,
    val phaseTransitionTriggers: List<String>,
    val phaseTransitionObserved: Boolean,
    val phaseOverridePhaseIds: List<String>,
    val phaseOverrideTelegraphKey: String?,
    val phaseOverrideRuntimeTriggered: Boolean,
    val phaseOverrideTelegraphTriggered: Boolean,
    val phaseOverrideActionEmphasisIds: List<String>,
    val phaseOverrideSkippedReason: String?,
    val expectedSelectedActions: List<String>,
    val selectedActionIds: List<String>,
    val selectedActionSequence: List<String>,
    val requiredAiTraceCount: Int,
    val observedAiTraceCount: Int,
    val aiTraceCount: Int,
    val bossTraceCount: Int,
    val aiTraceHash: String,
    val bossTraceHash: String,
    val aiTracePayload: JsonElement,
    val bossTracePayload: JsonElement,
    val encounterThreatBudget: JsonElement,
    val floorRewardBudgetDelta: JsonElement,
    val inspectReadable: Boolean,
    val logReadable: Boolean,
    val failureReason: String?,
) {
    fun toJson() =
        buildJsonObject {
            put("seed", seed)
            put("localeId", localeId)
            put("zoneId", zoneId)
            put("templateId", templateId)
            put("encounterId", encounterId)
            variantId?.let { put("variantId", it) }
            put("success", success)
            phaseId?.let { put("phaseId", it) }
            telegraphKey?.let { put("telegraphKey", it) }
            putJsonArray("grantedMutations") { grantedMutations.forEach { mutationId -> add(JsonPrimitive(mutationId)) } }
            putJsonArray("phaseSequence") { phaseSequence.forEach { phase -> add(JsonPrimitive(phase)) } }
            putJsonArray("phaseTransitionTriggers") { phaseTransitionTriggers.forEach { trigger -> add(JsonPrimitive(trigger)) } }
            put("phaseTransitionObserved", phaseTransitionObserved)
            putJsonArray("phaseOverridePhaseIds") { phaseOverridePhaseIds.forEach { phaseId -> add(JsonPrimitive(phaseId)) } }
            phaseOverrideTelegraphKey?.let { put("phaseOverrideTelegraphKey", it) }
            put("phaseOverrideRuntimeTriggered", phaseOverrideRuntimeTriggered)
            put("phaseOverrideTelegraphTriggered", phaseOverrideTelegraphTriggered)
            putJsonArray("phaseOverrideActionEmphasisIds") { phaseOverrideActionEmphasisIds.forEach { actionId -> add(JsonPrimitive(actionId)) } }
            phaseOverrideSkippedReason?.let { put("phaseOverrideSkippedReason", it) }
            putJsonArray("expectedSelectedActions") { expectedSelectedActions.forEach { actionId -> add(JsonPrimitive(actionId)) } }
            putJsonArray("selectedActionIds") { selectedActionIds.forEach { actionId -> add(JsonPrimitive(actionId)) } }
            putJsonArray("selectedActionSequence") { selectedActionSequence.forEach { actionId -> add(JsonPrimitive(actionId)) } }
            put("requiredAiTraceCount", requiredAiTraceCount)
            put("observedAiTraceCount", observedAiTraceCount)
            put("aiTraceCount", aiTraceCount)
            put("bossTraceCount", bossTraceCount)
            put("aiTraceHash", aiTraceHash)
            put("bossTraceHash", bossTraceHash)
            put("threatCost", threatCost)
            lootProfileOverride?.let { put("lootProfileOverride", it) }
            lootProfileRewardBudget?.let { put("lootProfileRewardBudget", it) }
            actionWeightProfileId?.let { put("actionWeightProfileId", it) }
            visualTintKey?.let { put("visualTintKey", it) }
            renderTintColorHex?.let { put("renderTintColorHex", it) }
            putJsonArray("preferredTerrainTags") { preferredTerrainTags.forEach { terrainTag -> add(JsonPrimitive(terrainTag)) } }
            putJsonArray("bossTerrainTags") { bossTerrainTags.forEach { terrainTag -> add(JsonPrimitive(terrainTag)) } }
            putJsonArray("adjacentBossTerrainTags") { adjacentBossTerrainTags.forEach { terrainTag -> add(JsonPrimitive(terrainTag)) } }
            put("terrainPreferenceAvailable", terrainPreferenceAvailable)
            put("terrainPreferenceImplemented", terrainPreferenceImplemented)
            put("encounterThreatBudget", encounterThreatBudget)
            put("floorRewardBudgetDelta", floorRewardBudgetDelta)
            put("inspectReadable", inspectReadable)
            put("logReadable", logReadable)
            put("aiTraces", aiTracePayload)
            put("bossTraces", bossTracePayload)
            failureReason?.let { put("failureReason", it) }
            putJsonArray("checks") {
                add(JsonPrimitive("telegraph"))
                add(JsonPrimitive("phaseTransition"))
                add(JsonPrimitive("phaseOverrideRuntime"))
                add(JsonPrimitive("phaseOverrideTelegraph"))
                add(JsonPrimitive("phaseOverrideActionEmphasis"))
                add(JsonPrimitive("bossTracePayload"))
                add(JsonPrimitive("aiDecisionTracePayload"))
                add(JsonPrimitive("phaseSpecificActionTrace"))
                add(JsonPrimitive("traceHash"))
                add(JsonPrimitive("variantReadability"))
                add(JsonPrimitive("terrainAwarePlacement"))
                add(JsonPrimitive("threatRewardLedger"))
            }
        }
}

private data class BossHarnessPairReport(
    val joinKey: WhiteBoxJoinKey,
    val baseReport: BossHarnessReport,
    val variantReport: BossHarnessReport,
    val phaseGraphUnchanged: Boolean,
    val phaseGraphStructuralDiffCount: Int,
    val traceDiverged: Boolean,
    val selectedActionSequenceDiverged: Boolean,
    val actionTraceDivergenceScore: Double,
    val threatLedgerMatched: Boolean,
    val rewardLedgerMatched: Boolean,
    val phaseOverrideRuntimeTriggered: Boolean,
    val phaseOverrideTelegraphTriggered: Boolean,
    val phaseOverrideActionEmphasisIds: List<String>,
    val inspectReadable: Boolean,
    val logReadable: Boolean,
    val success: Boolean,
    val failureReason: String?,
) {
    fun variantTraceDivergencePassed(): Boolean =
        traceDiverged &&
            selectedActionSequenceDiverged &&
            actionTraceDivergenceScore >= MIN_VARIANT_ACTION_TRACE_DIVERGENCE_SCORE

    fun toJson() =
        buildJsonObject {
            put("joinKey", Json.encodeToJsonElement(WhiteBoxJoinKey.serializer(), joinKey))
            put("base", baseReport.toJson())
            put("variant", variantReport.toJson())
            put("phaseGraphUnchanged", phaseGraphUnchanged)
            put("phaseGraphStructuralDiffCount", phaseGraphStructuralDiffCount)
            put("traceDiverged", traceDiverged)
            put("selectedActionSequenceDiverged", selectedActionSequenceDiverged)
            put("actionTraceDivergenceScore", actionTraceDivergenceScore)
            put("threatLedgerMatched", threatLedgerMatched)
            put("rewardLedgerMatched", rewardLedgerMatched)
            put("phaseOverrideRuntimeTriggered", phaseOverrideRuntimeTriggered)
            put("phaseOverrideTelegraphTriggered", phaseOverrideTelegraphTriggered)
            putJsonArray("phaseOverrideActionEmphasisIds") { phaseOverrideActionEmphasisIds.forEach { actionId -> add(JsonPrimitive(actionId)) } }
            put("inspectReadable", inspectReadable)
            put("logReadable", logReadable)
            put("success", success)
            failureReason?.let { put("failureReason", it) }
        }

    fun toWhiteBoxCase(outputDir: Path): WhiteBoxCaseReport {
        val variantId = requireNotNull(variantReport.variantId)
        val assertions =
            listOf(
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.base_encounter_exists",
                    passed = baseReport.encounterId.isNotBlank(),
                    message = "Base encounter id is present for the pair.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.variant_mutations_registered",
                    passed = variantReport.grantedMutations.all { mutationId -> mutationId.startsWith("elite.") },
                    message = "Variant grantedMutations only reference registered elite mutation ids.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.phase_graph_same_as_base",
                    passed = phaseGraphUnchanged,
                    message = "Variant preserves the base phase graph structure.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.phase_transition_observed",
                    passed = baseReport.phaseTransitionObserved && variantReport.phaseTransitionObserved,
                    message = "Base and variant samples both observe the formal boss phase transition.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.action_weight_overlay_only",
                    passed = variantReport.actionWeightProfileId != null && phaseGraphUnchanged,
                    message = "actionWeightProfileId only shifts weights and does not alter phase structure.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.variant_trace_divergence",
                    passed = variantTraceDivergencePassed(),
                    message = "Variant sample diverges from the base AI trace and selected action sequence.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.threat_cost_in_ledger",
                    passed = threatLedgerMatched,
                    message = "Variant threatCost is represented inside EncounterThreatBudget.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.loot_override_in_ledger",
                    passed = rewardLedgerMatched,
                    message = "Variant lootProfileOverride is represented inside FloorRewardBudget.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.readability_contract",
                    passed = inspectReadable && logReadable && variantReport.telegraphKey != null,
                    message = "Variant remains traceable via telegraph, inspect, and log metadata.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.phase_override_runtime_triggered",
                    passed = phaseOverrideRuntimeTriggered,
                    message = "Variant phase override trigger is observed through boss runtime state.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.phase_override_telegraph_triggered",
                    passed = phaseOverrideTelegraphTriggered,
                    message = "Variant phase override telegraph is represented in boss trace side effects.",
                ),
                WhiteBoxAssertionResult(
                    ruleId = "boss.case.preferred_terrain_applied",
                    passed = !variantReport.terrainPreferenceAvailable || variantReport.terrainPreferenceImplemented,
                    message = "Variant preferred terrain tags are realized by the final boss landing tile or its adjacent tactical tiles.",
                ),
            )
        return WhiteBoxCaseReport(
            joinKey = joinKey,
            facts =
                buildJsonObject {
                    put("baseEncounterId", baseReport.encounterId)
                    put("variantId", variantId)
                    putJsonArray("grantedMutations") { variantReport.grantedMutations.forEach { mutationId -> add(JsonPrimitive(mutationId)) } }
                    put("threatCost", variantReport.threatCost)
                    variantReport.lootProfileOverride?.let { put("lootProfileOverride", it) }
                    variantReport.lootProfileRewardBudget?.let { put("lootProfileRewardBudget", it) }
                    variantReport.actionWeightProfileId?.let { put("actionWeightProfileId", it) }
                    putJsonArray("phaseSequence") { variantReport.phaseSequence.forEach { phase -> add(JsonPrimitive(phase)) } }
                    putJsonArray("phaseTransitionTriggers") { variantReport.phaseTransitionTriggers.forEach { trigger -> add(JsonPrimitive(trigger)) } }
                    put("phaseTransitionObserved", variantReport.phaseTransitionObserved)
                    putJsonArray("phaseOverridePhaseIds") { variantReport.phaseOverridePhaseIds.forEach { phaseId -> add(JsonPrimitive(phaseId)) } }
                    variantReport.phaseOverrideTelegraphKey?.let { put("phaseOverrideTelegraphKey", it) }
                    put("phaseOverrideRuntimeTriggered", phaseOverrideRuntimeTriggered)
                    put("phaseOverrideTelegraphTriggered", phaseOverrideTelegraphTriggered)
                    putJsonArray("phaseOverrideActionEmphasisIds") { phaseOverrideActionEmphasisIds.forEach { actionId -> add(JsonPrimitive(actionId)) } }
                    putJsonArray("selectedActions") { variantReport.selectedActionIds.forEach { actionId -> add(JsonPrimitive(actionId)) } }
                    putJsonArray("selectedActionSequence") { variantReport.selectedActionSequence.forEach { actionId -> add(JsonPrimitive(actionId)) } }
                    put("traceDiverged", traceDiverged)
                    put("selectedActionSequenceDiverged", selectedActionSequenceDiverged)
                    put("actionTraceDivergenceScore", actionTraceDivergenceScore)
                    putJsonArray("preferredTerrainTags") { variantReport.preferredTerrainTags.forEach { terrainTag -> add(JsonPrimitive(terrainTag)) } }
                    putJsonArray("bossTerrainTags") { variantReport.bossTerrainTags.forEach { terrainTag -> add(JsonPrimitive(terrainTag)) } }
                    putJsonArray("adjacentBossTerrainTags") { variantReport.adjacentBossTerrainTags.forEach { terrainTag -> add(JsonPrimitive(terrainTag)) } }
                    put("terrainPreferenceAvailable", variantReport.terrainPreferenceAvailable)
                    put("terrainPreferenceImplemented", variantReport.terrainPreferenceImplemented)
                    put("aiTraceHash", variantReport.aiTraceHash)
                    put("bossTraceHash", variantReport.bossTraceHash)
                    put("encounterThreatBudget", variantReport.encounterThreatBudget)
                    put("floorRewardBudgetDelta", variantReport.floorRewardBudgetDelta)
                },
            fingerprints =
                linkedMapOf(
                    "baseAiTraceHash" to baseReport.aiTraceHash,
                    "variantAiTraceHash" to variantReport.aiTraceHash,
                    "baseBossTraceHash" to baseReport.bossTraceHash,
                    "variantBossTraceHash" to variantReport.bossTraceHash,
                ),
            assertions = assertions,
            artifacts = writeArtifacts(outputDir),
        )
    }

    private fun writeArtifacts(outputDir: Path) =
        listOf(
            WhiteBoxHarnessWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "phase-graph-diff",
                kind = "phase_graph_diff",
                fileName = "phase-graph-diff.md",
                summary = "Base-vs-variant phase graph comparison.",
                content =
                    buildString {
                        appendLine("| side | phaseSequence | transitionTriggers |")
                        appendLine("| --- | --- | --- |")
                        appendLine("| base | ${baseReport.phaseSequence.joinToString()} | ${baseReport.phaseTransitionTriggers.joinToString()} |")
                        appendLine("| variant | ${variantReport.phaseSequence.joinToString()} | ${variantReport.phaseTransitionTriggers.joinToString()} |")
                        appendLine("| structuralDiffCount | $phaseGraphStructuralDiffCount | ${if (phaseGraphUnchanged) "0" else "1+"} |")
                    },
                tags = listOf("phase", "graph"),
            ),
            WhiteBoxHarnessWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "ai-candidates",
                kind = "ai_candidates",
                fileName = "ai-candidates.md",
                summary = "Base-vs-variant AI candidate and selected action comparison.",
                content =
                    buildString {
                        appendLine("| side | selectedActions | aiTraceHash |")
                        appendLine("| --- | --- | --- |")
                        appendLine("| base | ${baseReport.selectedActionSequence.joinToString()} | ${baseReport.aiTraceHash} |")
                        appendLine("| variant | ${variantReport.selectedActionSequence.joinToString()} | ${variantReport.aiTraceHash} |")
                        appendLine("| divergenceScore | $actionTraceDivergenceScore | traceDiverged=$traceDiverged |")
                    },
                tags = listOf("ai", "actions"),
            ),
            WhiteBoxHarnessWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "mutation-source-table",
                kind = "mutation_source_table",
                fileName = "mutation-source-table.md",
                summary = "Mutations granted by the boss variant overlay.",
                content =
                    buildString {
                        appendLine("| variantId | mutationId |")
                        appendLine("| --- | --- |")
                        variantReport.grantedMutations.forEach { mutationId ->
                            appendLine("| ${variantReport.variantId} | $mutationId |")
                        }
                    },
                tags = listOf("mutation", "variant"),
            ),
            WhiteBoxHarnessWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "terrain-placement",
                kind = "terrain_placement",
                fileName = "terrain-placement.md",
                summary = "Boss variant terrain affinity versus realized landing tile.",
                content =
                    buildString {
                        appendLine("| side | preferredTerrainTags | bossTerrainTags | adjacentBossTerrainTags | preferenceImplemented |")
                        appendLine("| --- | --- | --- | --- | --- |")
                        appendLine(
                            "| base | ${baseReport.preferredTerrainTags.joinToString().ifBlank { "-" }} | ${baseReport.bossTerrainTags.joinToString().ifBlank { "-" }} | " +
                                "${baseReport.adjacentBossTerrainTags.joinToString().ifBlank { "-" }} | ${baseReport.terrainPreferenceAvailable}/${baseReport.terrainPreferenceImplemented} |",
                        )
                        appendLine(
                            "| variant | ${variantReport.preferredTerrainTags.joinToString().ifBlank { "-" }} | ${variantReport.bossTerrainTags.joinToString().ifBlank { "-" }} | " +
                                "${variantReport.adjacentBossTerrainTags.joinToString().ifBlank { "-" }} | ${variantReport.terrainPreferenceAvailable}/${variantReport.terrainPreferenceImplemented} |",
                        )
                    },
                tags = listOf("terrain", "placement"),
            ),
            WhiteBoxHarnessWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "ledger-breakdown",
                kind = "ledger_breakdown",
                fileName = "ledger-breakdown.md",
                summary = "Threat and reward ledger deltas for the variant boss.",
                content =
                    buildString {
                        appendLine("## EncounterThreatBudget")
                        appendLine(Json.encodeToString(variantReport.encounterThreatBudget))
                        appendLine()
                        appendLine("## FloorRewardBudget")
                        appendLine(Json.encodeToString(variantReport.floorRewardBudgetDelta))
                    },
                tags = listOf("ledger", "reward", "threat"),
            ),
            WhiteBoxHarnessWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "variant-cue-tint",
                kind = "variant_cue_tint",
                fileName = "variant-cue-tint.md",
                summary = "Variant cue key carried by runtime; tint is resolved client-side from manifest metadata.",
                content =
                    buildString {
                        appendLine("| variantId | visualTintKey |")
                        appendLine("| --- | --- |")
                        appendLine("| ${variantReport.variantId} | ${variantReport.visualTintKey ?: "-"} |")
                    },
                tags = listOf("visual", "tint"),
            ),
            WhiteBoxHarnessWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "phase-override",
                kind = "phase_override",
                fileName = "phase-override.md",
                summary = "PR-05 variant phase override runtime trigger, telegraph, and action emphasis.",
                content =
                    buildString {
                        appendLine("| variantId | phaseOverridePhaseIds | telegraph | runtimeTriggered | telegraphTriggered | actionEmphasisIds |")
                        appendLine("| --- | --- | --- | --- | --- | --- |")
                        appendLine(
                            "| ${variantReport.variantId} | ${variantReport.phaseOverridePhaseIds.joinToString().ifBlank { "-" }} | " +
                                "${variantReport.phaseOverrideTelegraphKey ?: "-"} | $phaseOverrideRuntimeTriggered | " +
                                "$phaseOverrideTelegraphTriggered | ${phaseOverrideActionEmphasisIds.joinToString().ifBlank { "-" }} |",
                        )
                    },
                tags = listOf("phase", "override"),
            ),
        )
}

private data class PhaseOverrideHarnessExpectation(
    val phaseId: String,
    val telegraphSpecId: String,
    val actionEmphasisIds: List<String>,
)

private data class BossRegistryMetrics(
    val eliteMutationDistinctCount: Int,
    val eliteMutationValidPairCount: Int,
    val mutationTierDistribution: Map<String, Int>,
    val bossVariantCount: Int,
    val phaseOverrideVariantDenominator: Int,
    val phaseOverrideVariantCount: Int,
    val phaseOverrideSchemaCoverage: Double,
    val phaseOverrideExpectationsByVariant: Map<String, PhaseOverrideHarnessExpectation>,
    val bossVariantMutationPairwiseDistinct: Boolean,
    val bossVariantMutationSets: Map<String, List<String>>,
    val bossVariantPreferredTerrainTags: Map<String, List<String>>,
    val bossVariantBasePhaseIds: Map<String, List<String>>,
    val bossVariantBasePhaseCounts: Map<String, Int>,
) {
    companion object {
        fun load(): BossRegistryMetrics {
            val schemaCatalog = DataLoader().loadSchemaCatalog()
            val allZoneIds = schemaCatalog.zones.mapTo(linkedSetOf()) { zone -> zone.id }
            val mutations = schemaCatalog.eliteMutations
            val mutationsById = mutations.associateBy { mutation -> mutation.id }
            val bossVariantCount = schemaCatalog.bossVariants.map { variant -> variant.id }.distinct().size
            val phaseOverrideExpectationsByVariant =
                schemaCatalog.bossVariants
                    .mapNotNull { variant ->
                        val override = variant.phaseOverrides.singleOrNull() ?: return@mapNotNull null
                        variant.id to
                            PhaseOverrideHarnessExpectation(
                                phaseId = override.phaseId,
                                telegraphSpecId = override.telegraphSpecId,
                                actionEmphasisIds = override.actionEmphasisIds,
                            )
                    }.toMap()
                    .toSortedMap()
            val bossVariantMutationSets =
                schemaCatalog.bossVariants
                    .associate { variant ->
                        variant.id to variant.grantedMutations.map { mutationRef -> mutationRef.mutationId }.sorted()
                    }.toSortedMap()
            val bossVariantPreferredTerrainTags =
                schemaCatalog.bossVariants
                    .associate { variant ->
                        variant.id to
                            variant.grantedMutations
                                .asSequence()
                                .mapNotNull { mutationRef -> mutationsById[mutationRef.mutationId] }
                                .flatMap { mutation -> mutation.preferredTerrainTags.asSequence() }
                                .map { terrainTag -> terrainTag.name }
                                .distinct()
                                .sorted()
                                .toList()
                    }.toSortedMap()
            val bossVariantBasePhaseCounts =
                schemaCatalog.bossVariants
                    .associate { variant ->
                        variant.id to
                            schemaCatalog.bossEncounters
                                .firstOrNull { encounter -> encounter.id == variant.baseEncounterId }
                                ?.phases
                                .orEmpty()
                                .size
                    }.toSortedMap()
            val bossVariantBasePhaseIds =
                schemaCatalog.bossVariants
                    .associate { variant ->
                        variant.id to
                            schemaCatalog.bossEncounters
                                .firstOrNull { encounter -> encounter.id == variant.baseEncounterId }
                                ?.phases
                                .orEmpty()
                                .map { phase -> phase.id }
                    }.toSortedMap()
            val validPairCount =
                mutations.indices.sumOf { leftIndex ->
                    val left = mutations[leftIndex]
                    mutations
                        .drop(leftIndex + 1)
                        .count { right -> mutationsCanCoexist(left = left, right = right, allZoneIds = allZoneIds) }
                }
            return BossRegistryMetrics(
                eliteMutationDistinctCount = mutations.map { mutation -> mutation.id }.distinct().size,
                eliteMutationValidPairCount = validPairCount,
                mutationTierDistribution =
                    mutations
                        .groupingBy { mutation -> mutation.tier.name }
                        .eachCount()
                        .toSortedMap(),
                bossVariantCount = bossVariantCount,
                phaseOverrideVariantDenominator = bossVariantCount,
                phaseOverrideVariantCount = schemaCatalog.bossVariants.count { variant -> variant.phaseOverrides.isNotEmpty() },
                phaseOverrideSchemaCoverage =
                    if (bossVariantCount == 0) {
                        0.0
                    } else {
                        schemaCatalog.bossVariants.count { variant -> variant.phaseOverrides.isNotEmpty() }.toDouble() / bossVariantCount.toDouble()
                    },
                phaseOverrideExpectationsByVariant = phaseOverrideExpectationsByVariant,
                bossVariantMutationPairwiseDistinct =
                    bossVariantMutationSets.values.distinct().size == bossVariantMutationSets.size,
                bossVariantMutationSets = bossVariantMutationSets,
                bossVariantPreferredTerrainTags = bossVariantPreferredTerrainTags,
                bossVariantBasePhaseIds = bossVariantBasePhaseIds,
                bossVariantBasePhaseCounts = bossVariantBasePhaseCounts,
            )
        }

        private fun mutationsCanCoexist(
            left: com.ktome.game.elites.EliteMutationDef,
            right: com.ktome.game.elites.EliteMutationDef,
            allZoneIds: Set<String>,
        ): Boolean {
            if (right.id in left.incompatibleWith || left.id in right.incompatibleWith) {
                return false
            }
            if (left.tier.name == "SIGNATURE" && right.tier.name == "SIGNATURE") {
                return false
            }
            val sharedZones = resolveAllowedZones(left.allowedZones, allZoneIds).intersect(resolveAllowedZones(right.allowedZones, allZoneIds))
            if (sharedZones.isEmpty()) {
                return false
            }
            return floorRangesOverlap(
                leftMinFloor = left.minFloor,
                leftMaxFloor = left.maxFloor,
                rightMinFloor = right.minFloor,
                rightMaxFloor = right.maxFloor,
            )
        }

        private fun resolveAllowedZones(
            allowedZones: Set<String>,
            allZoneIds: Set<String>,
        ): Set<String> = if (allowedZones.isEmpty()) allZoneIds else allowedZones

        private fun floorRangesOverlap(
            leftMinFloor: Int,
            leftMaxFloor: Int?,
            rightMinFloor: Int,
            rightMaxFloor: Int?,
        ): Boolean {
            val effectiveLeftMax = leftMaxFloor ?: Int.MAX_VALUE
            val effectiveRightMax = rightMaxFloor ?: Int.MAX_VALUE
            return maxOf(leftMinFloor, rightMinFloor) <= minOf(effectiveLeftMax, effectiveRightMax)
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0

private fun sha256(payload: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
