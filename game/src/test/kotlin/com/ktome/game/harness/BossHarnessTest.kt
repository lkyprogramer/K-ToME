package com.ktome.game.harness

import com.ktome.core.ai.AIDecisionTrace
import com.ktome.core.ai.BossEncounterState
import com.ktome.core.ai.BossTrace
import com.ktome.core.ai.PendingTelegraphState
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
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
    fun `boss harness covers two bosses with telegraph and trace consistency`() {
        val reports =
            listOf(
                runBanditCaptainHarness(seed = 20260324L),
                runMoltenGiantHarness(seed = 20260325L),
            )

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "boss-harness",
            payload =
                buildJsonObject {
                    put("scriptVersion", "boss-harness-v1")
                    putJsonArray("reports") { reports.forEach { report -> add(report.toJson()) } }
                },
            markdown =
                buildString {
                    appendLine("# Boss Harness")
                    appendLine("- scriptVersion: boss-harness-v1")
                    reports.forEach { report ->
                        appendLine(
                            "- boss=${report.templateId}, zone=${report.zoneId}, seed=${report.seed}, locale=${report.localeId}, success=${report.success}, telegraph=${report.telegraphKey}, phase=${report.phaseId}, aiTraceCount=${report.aiTraceCount}, bossTraceCount=${report.bossTraceCount}, aiTraceHash=${report.aiTraceHash}, bossTraceHash=${report.bossTraceHash}",
                        )
                    }
                },
        )

        assertTrue(
            reports.all { report -> report.success },
            reports.joinToString(separator = "\n") { report -> "${report.templateId}: ${report.failureReason ?: "unknown failure"}" },
        )
    }

    private fun runBanditCaptainHarness(seed: Long): BossHarnessReport {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = seed, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-bandit-$seed")),
            )
        descendToBossFloor(session)
        val bossId = requireNotNull(session.automationEntityByTemplateId("bandit.captain"))
        val world = session.automationWorld()
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(findOpenAdjacentPoint(session, bossPoint))
        requireNotNull(world.get<com.ktome.core.talent.CooldownState>(bossId)).remainingByTalentId.apply {
            this["shield_bash"] = 0
            this["power_strike"] = 99
            this["charge"] = 99
        }

        var telegraph: PendingTelegraphState? = null
        for (attempt in 0 until 6) {
            assertTrue(session.perform(PlayerCommand.Wait))
            val candidate = world.get<PendingTelegraphState>(bossId)
            if (candidate?.sourceAbilityId == "shield_bash") {
                telegraph = candidate
                break
            }
        }

        return buildReport(
            session = session,
            seed = seed,
            zoneId = "shattered_outpost",
            templateId = "bandit.captain",
            telegraph = telegraph,
            phaseState = world.get<BossEncounterState>(bossId),
            expectedTelegraphKey = "shield_bash",
            expectedPhaseId = "phase_full",
            expectedSelectedActionId = "shield_bash",
        )
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

        return buildReport(
            session = session,
            seed = seed,
            zoneId = "deep_iron_pit",
            templateId = "orc.molten_giant",
            telegraph = world.get<PendingTelegraphState>(bossId),
            phaseState = world.get<BossEncounterState>(bossId),
            expectedTelegraphKey = "molten_giant_phase_warning",
            expectedPhaseId = "phase_enraged",
            expectedBossTracePhaseId = "phase_enraged",
            expectedBossTraceSideEffect = "TELEGRAPH:molten_giant_phase_warning",
        )
    }

    private fun buildReport(
        session: FoundationGameSession,
        seed: Long,
        zoneId: String,
        templateId: String,
        telegraph: PendingTelegraphState?,
        phaseState: BossEncounterState?,
        expectedTelegraphKey: String,
        expectedPhaseId: String,
        expectedSelectedActionId: String? = null,
        expectedBossTracePhaseId: String? = null,
        expectedBossTraceSideEffect: String? = null,
    ): BossHarnessReport {
        val aiTraces = session.recentAIDecisionTraces()
        val bossTraces = session.recentBossTraces()
        val aiJson = Json.encodeToString(aiTraces)
        val bossJson = Json.encodeToString(bossTraces)
        val decodedAi = Json.decodeFromString<List<AIDecisionTrace>>(aiJson)
        val decodedBoss = Json.decodeFromString<List<BossTrace>>(bossJson)
        val aiTraceHash = sha256(aiJson)
        val bossTraceHash = sha256(bossJson)
        val traceRoundTripMatches = decodedAi == aiTraces && decodedBoss == bossTraces
        val telegraphMatches =
            telegraph != null &&
                (telegraph.sourceAbilityId == expectedTelegraphKey || telegraph.telegraphSpecId == expectedTelegraphKey)
        val aiTraceRequired = expectedSelectedActionId != null
        val bossTraceRequired = expectedBossTracePhaseId != null || expectedBossTraceSideEffect != null
        val expectedActionPresent =
            expectedSelectedActionId == null ||
                decodedAi.any { trace -> trace.selectedActionId == expectedSelectedActionId }
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
                (!aiTraceRequired || decodedAi.isNotEmpty()) &&
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
                aiTraceRequired && decodedAi.isEmpty() -> "Missing AI decision traces."
                bossTraceRequired && decodedBoss.isEmpty() -> "Missing boss traces."
                !traceRoundTripMatches -> "Trace payload changed after JSON round trip."
                !expectedActionPresent -> "Missing expected AI action trace '$expectedSelectedActionId'."
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
            aiTraceCount = decodedAi.size,
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
}

private data class BossHarnessReport(
    val seed: Long,
    val localeId: String,
    val zoneId: String,
    val templateId: String,
    val success: Boolean,
    val phaseId: String?,
    val telegraphKey: String?,
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
            put("aiTraceCount", aiTraceCount)
            put("bossTraceCount", bossTraceCount)
            put("aiTraceHash", aiTraceHash)
            put("bossTraceHash", bossTraceHash)
            put("aiTraces", aiTracePayload)
            put("bossTraces", bossTracePayload)
            failureReason?.let { put("failureReason", it) }
            putJsonArray("checks") {
                add(JsonPrimitive("telegraph"))
                add(JsonPrimitive("bossTracePayload"))
                add(JsonPrimitive("aiDecisionTracePayload"))
                add(JsonPrimitive("traceHash"))
            }
        }
}

private fun sha256(payload: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
