package com.ktome.game.harness

import com.ktome.core.save.SaveManager
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val ZONE_CHAIN_SMOKE_SCRIPT_VERSION: String = "zone-chain-smoke-v2"
private const val ZONE_CHAIN_SMOKE_MAX_TURNS: Int = 3200

@Tag("headlessSmoke")
class ZoneChainSmokeTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `official phase2 route reaches final zone floor coverage`() {
        val reports =
            listOf(
                ZoneChainSpec(professionId = "arcanist", seed = 20260313L),
            ).map(::runZoneChain)

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "zone-chain-smoke",
            payload =
                buildJsonObject {
                    put("scriptVersion", ZONE_CHAIN_SMOKE_SCRIPT_VERSION)
                    putJsonArray("zoneRoute") { FOUNDATION_ZONE_ROUTE.forEach { zoneId -> add(JsonPrimitive(zoneId)) } }
                    putJsonArray("reports") { reports.forEach { report -> add(report.toJson()) } }
                },
            markdown =
                buildString {
                    appendLine("# Zone Chain Smoke")
                    appendLine("- scriptVersion: $ZONE_CHAIN_SMOKE_SCRIPT_VERSION")
                    appendLine("- zoneRoute: ${FOUNDATION_ZONE_ROUTE.joinToString(" -> ")}")
                    reports.forEach { report ->
                        appendLine(
                            "- profession=${report.professionId}, seed=${report.seed}, success=${report.success}, turns=${report.turns}, finalZone=${report.finalZoneId}, finalFloor=${report.finalFloor}, routeHash=${report.routeHash}, outcome=${report.outcome}",
                        )
                    }
                },
        )

        assertTrue(
            reports.all(ZoneChainSmokeReport::success),
            reports.joinToString(separator = "\n") { report ->
                "${report.professionId}/${report.seed}: ${report.failureReason ?: "unknown failure"}"
            },
        )
    }

    private fun runZoneChain(spec: ZoneChainSpec): ZoneChainSmokeReport {
        val saveManager = SaveManager(tempDir.resolve("${spec.professionId}-${spec.seed}"))
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = spec.seed,
                        zoneId = FOUNDATION_ZONE_ROUTE.first(),
                        playerProfessionId = spec.professionId,
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 0,
                    ),
                saveManager = saveManager,
            )
        val bot = SmokeBot()
        val commandTrace = mutableListOf<String>()
        val routeTrace = mutableListOf(routeStep(session))
        var turns = 0
        var failureReason: String? = null

        while (turns < ZONE_CHAIN_SMOKE_MAX_TURNS && !session.runOutcome().isTerminal) {
            val observation = RunObservationCapture.capture(session, turns)
            val command: PlayerCommand? =
                routeProgressCommand(session, observation)
                    ?: bot.decide(observation)
            if (command == null) {
                failureReason = "SmokeBot returned no command."
                break
            }
            val renderedCommand = renderCommand(command)
            commandTrace += renderedCommand
            if (!session.perform(command)) {
                failureReason = "Command rejected: $renderedCommand"
                break
            }
            if (command.consumesTurn()) {
                turns += 1
                routeTrace += routeStep(session)
            }
        }

        val visitedZones =
            routeTrace
                .map { entry -> entry.substringBefore(':') }
                .distinct()
        val reasons = mutableListOf<String>()
        failureReason?.let(reasons::add)
        if (session.config.routeIndex != FOUNDATION_ZONE_ROUTE.lastIndex) {
            reasons += "Expected final routeIndex ${FOUNDATION_ZONE_ROUTE.lastIndex} but was ${session.config.routeIndex}."
        }
        if (session.config.zoneId != FOUNDATION_ZONE_ROUTE.last()) {
            reasons += "Expected final zone ${FOUNDATION_ZONE_ROUTE.last()} but was ${session.config.zoneId}."
        }
        if (session.currentFloor() < 1) {
            reasons += "Expected to reach the first playable floor in final zone but was floor ${session.currentFloor()}."
        }
        if (visitedZones != FOUNDATION_ZONE_ROUTE) {
            reasons += "Expected visited zones ${FOUNDATION_ZONE_ROUTE.joinToString(" -> ")}, got ${visitedZones.joinToString(" -> ")}."
        }

        return ZoneChainSmokeReport(
            professionId = spec.professionId,
            seed = spec.seed,
            scriptVersion = ZONE_CHAIN_SMOKE_SCRIPT_VERSION,
            zoneRoute = FOUNDATION_ZONE_ROUTE,
            success = reasons.isEmpty(),
            outcome = session.runOutcome().toString(),
            turns = turns,
            finalZoneId = session.config.zoneId,
            finalFloor = session.currentFloor(),
            routeIndex = session.config.routeIndex,
            routeHash = sha256(routeTrace.joinToString(separator = "|")),
            commandTraceHash = sha256(commandTrace.joinToString(separator = "|")),
            commandTrace = commandTrace,
            failureReason = reasons.firstOrNull(),
        )
    }

    private fun routeStep(session: com.ktome.game.FoundationGameSession): String {
        val position = session.playerPosition()
        return "${session.config.zoneId}:${session.config.routeIndex}:${session.currentFloor()}@${position.x},${position.y}"
    }

    private fun renderCommand(command: PlayerCommand): String =
        when (command) {
            is PlayerCommand.Move -> "Move(${command.delta.x},${command.delta.y})"
            PlayerCommand.Wait -> "Wait"
            PlayerCommand.PickUp -> "PickUp"
            PlayerCommand.Interact -> "Interact"
            PlayerCommand.Ascend -> "Ascend"
            PlayerCommand.Descend -> "Descend"
            PlayerCommand.SaveGame -> "SaveGame"
            PlayerCommand.CloseShop -> "CloseShop"
            is PlayerCommand.BuyShopOffer -> "BuyShopOffer(${command.index})"
            is PlayerCommand.SellInventoryItem -> "SellInventoryItem(${command.index})"
            is PlayerCommand.DropInventoryItem -> "DropInventoryItem(${command.index})"
            is PlayerCommand.SelectRoute -> "SelectRoute(${command.index})"
            is PlayerCommand.ActivateInventoryItem -> "ActivateInventoryItem(${command.index})"
            is PlayerCommand.UseInscription -> "UseInscription(${command.hotkey})"
            is PlayerCommand.UseTalent ->
                command.target?.let { target -> "UseTalent(${command.slot},${target.x},${target.y})" } ?: "UseTalent(${command.slot})"
            is PlayerCommand.EquipTalentToSlot -> "EquipTalentToSlot(${command.slot},${command.talentId})"
            is PlayerCommand.AssignStat -> "AssignStat(${command.stat})"
            is PlayerCommand.AssignTalent -> "AssignTalent(${command.talentId})"
            PlayerCommand.ConfirmTalentDraft -> "ConfirmTalentDraft"
            PlayerCommand.RollbackTalentDraft -> "RollbackTalentDraft"
            is PlayerCommand.RespecTalentTree -> "RespecTalentTree(${command.ownerType},${command.treeOwnerId})"
        }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

}

private data class ZoneChainSpec(
    val professionId: String,
    val seed: Long,
)

private data class ZoneChainSmokeReport(
    val professionId: String,
    val seed: Long,
    val scriptVersion: String,
    val zoneRoute: List<String>,
    val success: Boolean,
    val outcome: String,
    val turns: Int,
    val finalZoneId: String,
    val finalFloor: Int,
    val routeIndex: Int,
    val routeHash: String,
    val commandTraceHash: String,
    val commandTrace: List<String>,
    val failureReason: String?,
) {
    fun toJson() =
        buildJsonObject {
            put("professionId", professionId)
            put("seed", seed)
            put("scriptVersion", scriptVersion)
            put("success", success)
            put("outcome", outcome)
            put("turns", turns)
            put("finalZoneId", finalZoneId)
            put("finalFloor", finalFloor)
            put("routeIndex", routeIndex)
            put("routeHash", routeHash)
            put("commandTraceHash", commandTraceHash)
            failureReason?.let { put("failureReason", it) }
            putJsonArray("zoneRoute") { zoneRoute.forEach { zoneId -> add(JsonPrimitive(zoneId)) } }
            putJsonArray("commandTrace") { commandTrace.forEach { command -> add(JsonPrimitive(command)) } }
        }
}
