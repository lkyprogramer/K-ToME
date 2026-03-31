package com.ktome.game.harness

import com.ktome.core.ai.AIDecisionTrace
import com.ktome.core.ai.BossEncounterState
import com.ktome.core.ai.BossTrace
import com.ktome.core.ai.PendingTelegraphState
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.CombatFeedbackTypeSnapshot
import com.ktome.game.AbyssalRuntimeKeys
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.interactablePoint
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BossHarnessTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("bossHarness")
    fun `boss harness covers phase three roster with telegraph and trace consistency`() {
        val reports =
            listOf(
                runMoltenGiantHarness(seed = 20260325L),
                runDungeonLordHarness(seed = 20260326L),
                runAbyssalGuardianHarness(seed = 20260327L),
            )

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "boss-harness",
            payload =
                buildJsonObject {
                    put("scriptVersion", "boss-harness-v2")
                    putJsonArray("reports") { reports.forEach { report -> add(report.toJson()) } }
                },
            markdown =
                buildString {
                    appendLine("# Boss Harness")
                    appendLine("- scriptVersion: boss-harness-v2")
                    reports.forEach { report ->
                        appendLine(
                            "- boss=${report.templateId}, zone=${report.zoneId}, seed=${report.seed}, locale=${report.localeId}, success=${report.success}, telegraph=${report.telegraphKey}, phase=${report.phaseId}, expectedActions=${if (report.expectedSelectedActions.isEmpty()) "none" else report.expectedSelectedActions}, selectedActions=${if (report.selectedActionIds.isEmpty()) "none" else report.selectedActionIds}, observedAiTraceCount=${report.observedAiTraceCount}/${report.requiredAiTraceCount}, bossTraceCount=${report.bossTraceCount}, aiTraceHash=${report.aiTraceHash}, bossTraceHash=${report.bossTraceHash}",
                        )
                    }
                },
        )

        assertTrue(
            reports.all { report -> report.success },
            reports.joinToString(separator = "\n") { report -> "${report.templateId}: ${report.failureReason ?: "unknown failure"}" },
        )
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

        assertTrue(
            snapshot.overlays.any { overlay ->
                overlay.id.startsWith("telegraph:") || overlay.id.startsWith("boss-warning:")
            },
        )
        assertTrue(
            snapshot.combatFeedbackEvents.any { event ->
                event.type == CombatFeedbackTypeSnapshot.HEAL &&
                    event.targetEntityId == session.playerId.value &&
                    (event.amount ?: 0) > 0
            },
        )
        assertTrue(telegraph.sourceAbilityId == "dungeon_lord_phase_warning" || telegraph.telegraphSpecId == "dungeon_lord_phase_warning")
    }

    private fun runMoltenGiantHarness(seed: Long): BossHarnessReport {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = seed, zoneId = "deep_iron_pit", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-molten-$seed")),
            )
        descendToBossFloor(session)
        val bossId = requireNotNull(session.automationEntityByTemplateId("orc.molten_giant"))
        val world = session.automationWorld()
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(findOpenAdjacentPoint(session, bossPoint))

        assertTrue(session.perform(PlayerCommand.Wait))
        requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).current =
            requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).max * 49 / 100
        assertTrue(session.perform(PlayerCommand.Wait))
        val phaseTelegraph = world.get<PendingTelegraphState>(bossId)
        waitForPhaseActionTrace(
            session = session,
            bossActorId = bossId.value,
            expectedPhaseId = "phase_enraged",
            expectedActionIds = setOf("earthshaker"),
        )

        return buildReport(
            session = session,
            bossActorId = bossId.value,
            seed = seed,
            zoneId = "deep_iron_pit",
            templateId = "orc.molten_giant",
            telegraph = phaseTelegraph,
            phaseState = world.get<BossEncounterState>(bossId),
            expectedTelegraphKey = "molten_giant_phase_warning",
            expectedPhaseId = "phase_enraged",
            expectedSelectedActionIds = setOf("earthshaker"),
            expectedBossTracePhaseId = "phase_enraged",
            expectedBossTraceSideEffect = "TELEGRAPH:molten_giant_phase_warning",
        )
    }

    private fun runDungeonLordHarness(seed: Long): BossHarnessReport {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = seed, zoneId = "grey_gate_depths", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("boss-dungeon-$seed")),
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
        val phaseTelegraph = world.get<PendingTelegraphState>(bossId)
        waitForPhaseActionTrace(
            session = session,
            bossActorId = bossId.value,
            expectedPhaseId = "phase_desperate",
            expectedActionIds = setOf("arcane_shield"),
        )

        return buildReport(
            session = session,
            bossActorId = bossId.value,
            seed = seed,
            zoneId = "grey_gate_depths",
            templateId = "cultist.dungeon_lord",
            telegraph = phaseTelegraph,
            phaseState = world.get<BossEncounterState>(bossId),
            expectedTelegraphKey = "dungeon_lord_phase_warning",
            expectedPhaseId = "phase_desperate",
            expectedSelectedActionIds = setOf("arcane_shield"),
            expectedBossTracePhaseId = "phase_desperate",
            expectedBossTraceSideEffect = "TELEGRAPH:dungeon_lord_phase_warning",
        )
    }

    private fun runAbyssalGuardianHarness(seed: Long): BossHarnessReport {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = seed, zoneId = "abyssal_heart", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-abyssal-$seed")),
            )
        session.automationMovePlayerTo(interactablePoint(session, AbyssalRuntimeKeys.Finale.INTERACTABLE_ID))
        assertTrue(session.perform(PlayerCommand.Interact))
        val bossId = requireNotNull(session.automationEntityByTemplateId("abyssal.guardian"))
        val world = session.automationWorld()
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(findOpenAdjacentPoint(session, bossPoint))

        assertTrue(session.perform(PlayerCommand.Wait))
        requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).current =
            requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId)).max * 35 / 100
        assertTrue(session.perform(PlayerCommand.Wait))
        val phaseTelegraph = world.get<PendingTelegraphState>(bossId)
        waitForPhaseActionTrace(
            session = session,
            bossActorId = bossId.value,
            expectedPhaseId = "phase_abyssal",
            expectedActionIds = setOf("abyssal_consecration", "press_abyss"),
        )

        return buildReport(
            session = session,
            bossActorId = bossId.value,
            seed = seed,
            zoneId = "abyssal_heart",
            templateId = "abyssal.guardian",
            telegraph = phaseTelegraph,
            phaseState = world.get<BossEncounterState>(bossId),
            expectedTelegraphKey = "abyssal_guardian_phase_warning",
            expectedPhaseId = "phase_abyssal",
            expectedSelectedActionIds = setOf("abyssal_consecration", "press_abyss"),
            expectedBossTracePhaseId = "phase_abyssal",
            expectedBossTraceSideEffect = "TELEGRAPH:abyssal_guardian_phase_warning",
        )
    }

    private fun buildReport(
        session: FoundationGameSession,
        bossActorId: Int,
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
    ): BossHarnessReport {
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
        val success =
            telegraphMatches &&
                phaseState?.currentPhaseId == expectedPhaseId &&
                (!aiTraceRequired || phaseScopedAi.isNotEmpty()) &&
                (!bossTraceRequired || decodedBoss.isNotEmpty()) &&
                traceRoundTripMatches &&
                expectedActionPresent &&
                expectedBossPhaseTracePresent &&
                expectedBossSideEffectPresent
        val failureReason =
            when {
                telegraph == null -> "Missing pending telegraph."
                !telegraphMatches -> "Expected telegraph '$expectedTelegraphKey' but got ${telegraph.sourceAbilityId}/${telegraph.telegraphSpecId}."
                phaseState?.currentPhaseId != expectedPhaseId -> "Expected phase $expectedPhaseId but got ${phaseState?.currentPhaseId}."
                aiTraceRequired && phaseScopedAi.isEmpty() -> "Missing AI decision traces after phase transition."
                bossTraceRequired && decodedBoss.isEmpty() -> "Missing boss traces."
                !traceRoundTripMatches -> "Trace payload changed after JSON round trip."
                !expectedActionPresent -> "Missing expected AI action trace after phase transition in ${expectedSelectedActionIds.sorted()}."
                !expectedBossPhaseTracePresent -> "Missing boss phase trace '$expectedBossTracePhaseId'."
                !expectedBossSideEffectPresent -> "Missing boss trace side effect '$expectedBossTraceSideEffect'."
                else -> null
            }
        return BossHarnessReport(
            seed = seed,
            localeId = session.localizer().locale.id,
            zoneId = zoneId,
            templateId = templateId,
            success = success,
            phaseId = phaseState?.currentPhaseId,
            telegraphKey = telegraph?.sourceAbilityId ?: telegraph?.telegraphSpecId,
            expectedSelectedActions = expectedSelectedActionIds.sorted(),
            selectedActionIds = selectedActionIds,
            requiredAiTraceCount = if (aiTraceRequired) 1 else 0,
            observedAiTraceCount = phaseScopedAi.size,
            aiTraceCount = phaseScopedAi.size,
            bossTraceCount = decodedBoss.size,
            aiTraceHash = aiTraceHash,
            bossTraceHash = bossTraceHash,
            aiTracePayload = Json.parseToJsonElement(aiJson),
            bossTracePayload = Json.parseToJsonElement(bossJson),
            failureReason = failureReason,
        )
    }

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
            }
            .sortedWith(compareByDescending<Point> { point -> point.chebyshevDistanceTo(center) }.thenBy(Point::y).thenBy(Point::x))
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
}

private data class BossHarnessReport(
    val seed: Long,
    val localeId: String,
    val zoneId: String,
    val templateId: String,
    val success: Boolean,
    val phaseId: String?,
    val telegraphKey: String?,
    val expectedSelectedActions: List<String>,
    val selectedActionIds: List<String>,
    val requiredAiTraceCount: Int,
    val observedAiTraceCount: Int,
    val aiTraceCount: Int,
    val bossTraceCount: Int,
    val aiTraceHash: String,
    val bossTraceHash: String,
    val aiTracePayload: kotlinx.serialization.json.JsonElement,
    val bossTracePayload: kotlinx.serialization.json.JsonElement,
    val failureReason: String?,
) {
    fun toJson() =
        buildJsonObject {
            put("seed", seed)
            put("localeId", localeId)
            put("zoneId", zoneId)
            put("templateId", templateId)
            put("success", success)
            phaseId?.let { put("phaseId", it) }
            telegraphKey?.let { put("telegraphKey", it) }
            putJsonArray("expectedSelectedActions") { expectedSelectedActions.forEach { actionId -> add(JsonPrimitive(actionId)) } }
            putJsonArray("selectedActionIds") { selectedActionIds.forEach { actionId -> add(JsonPrimitive(actionId)) } }
            put("requiredAiTraceCount", requiredAiTraceCount)
            put("observedAiTraceCount", observedAiTraceCount)
            put("aiTraceCount", aiTraceCount)
            put("bossTraceCount", bossTraceCount)
            put("aiTraceHash", aiTraceHash)
            put("bossTraceHash", bossTraceHash)
            put("aiTraces", aiTracePayload)
            put("bossTraces", bossTracePayload)
            failureReason?.let { put("failureReason", it) }
            putJsonArray("checks") {
                add(JsonPrimitive("telegraph"))
                add(JsonPrimitive("phaseTransition"))
                add(JsonPrimitive("bossTracePayload"))
                add(JsonPrimitive("aiDecisionTracePayload"))
                add(JsonPrimitive("phaseSpecificActionTrace"))
                add(JsonPrimitive("traceHash"))
            }
        }
}

private fun sha256(payload: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
