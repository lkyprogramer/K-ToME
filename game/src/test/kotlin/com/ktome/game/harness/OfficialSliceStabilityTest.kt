package com.ktome.game.harness

import com.ktome.core.run.RunOutcome
import com.ktome.core.save.SaveManager
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

private const val OFFICIAL_SLICE_STABILITY_SCRIPT_VERSION: String = "official-slice-stability-v2"
private const val OFFICIAL_SLICE_MIN_TURNS: Int = 40
private const val OFFICIAL_SLICE_MAX_TURNS: Int = 450
private const val OFFICIAL_SLICE_MAX_FLOOR2_SPLIT: Int = 260
private const val OFFICIAL_SLICE_MAX_BOSS_ENCOUNTER_TURNS: Int = 125

class OfficialSliceStabilityTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    @Tag("bossHarness")
    fun `official shattered outpost slice remains within first pass stability band`() {
        val reports =
            listOf(
                OfficialSliceSpec(professionId = "vanguard", seed = 20260312L),
                OfficialSliceSpec(professionId = "arcanist", seed = 20260313L),
            ).map { spec -> runOfficialSlice(spec) }

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "official-slice-stability",
            payload =
                buildJsonObject {
                    put("scriptVersion", OFFICIAL_SLICE_STABILITY_SCRIPT_VERSION)
                    put("zoneId", "shattered_outpost")
                    putJsonArray("reports") {
                        reports.forEach { report -> add(report.toJson()) }
                    }
                },
            markdown =
                buildString {
                    appendLine("# Official Slice Stability")
                    appendLine("- scriptVersion: $OFFICIAL_SLICE_STABILITY_SCRIPT_VERSION")
                    appendLine("- zoneId: shattered_outpost")
                    appendLine("- maxTurns: $OFFICIAL_SLICE_MAX_TURNS")
                    appendLine("- floor2SplitMax: $OFFICIAL_SLICE_MAX_FLOOR2_SPLIT")
                    appendLine("- bossEncounterTurnsMax: $OFFICIAL_SLICE_MAX_BOSS_ENCOUNTER_TURNS")
                    reports.forEach { report ->
                        appendLine(
                            "- profession=${report.professionId}, seed=${report.seed}, success=${report.success}, turns=${report.turns}, floor2Turn=${report.floor2Turn}, bossWarningTurn=${report.bossWarningTurn}, bossEncounterTurns=${report.bossEncounterTurns}, routeHash=${report.routeHash}, outcome=${report.outcome}",
                        )
                    }
                },
        )

        assertTrue(
            reports.all(OfficialSliceStabilityReport::success),
            reports.joinToString(separator = "\n") { report ->
                "${report.professionId}/${report.seed}: ${report.failureReason ?: "unknown failure"}"
            },
        )
    }

    private fun runOfficialSlice(spec: OfficialSliceSpec): OfficialSliceStabilityReport {
        val saveManager = SaveManager(tempDir.resolve("${spec.professionId}-${spec.seed}"))
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = spec.seed,
                        zoneId = "shattered_outpost",
                        playerProfessionId = spec.professionId,
                    ),
                saveManager = saveManager,
            )
        val bot = SmokeBot()
        val commandTrace = mutableListOf<String>()
        val routeTrace = mutableListOf(routeStep(session))
        var turns = 0
        var floor2Turn: Int? = null
        var bossWarningTurn: Int? = null
        var failureReason: String? = null

        while (turns < OFFICIAL_SLICE_MAX_TURNS && session.config.zoneId == "shattered_outpost" && !session.runOutcome().isTerminal) {
            val snapshot = session.renderSnapshot()
            if (floor2Turn == null && session.currentFloor() >= 2) {
                floor2Turn = turns
            }
            if (bossWarningTurn == null && snapshot.overlays.any { overlay -> overlay.id.startsWith("boss-warning:") }) {
                bossWarningTurn = turns
            }

            val observation = RunObservationCapture.capture(session, turns)
            val command = bot.decide(observation)
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

        if (floor2Turn == null && session.currentFloor() >= 2) {
            floor2Turn = turns
        }
        if (bossWarningTurn == null && session.renderSnapshot().overlays.any { overlay -> overlay.id.startsWith("boss-warning:") }) {
            bossWarningTurn = turns
        }

        val bossEncounterTurns = bossWarningTurn?.let { warningTurn -> turns - warningTurn }
        val reasons = mutableListOf<String>()
        failureReason?.let(reasons::add)
        val sliceCleared = session.config.zoneId != "shattered_outpost"
        if (!sliceCleared && session.runOutcome() !is RunOutcome.Victory) {
            reasons += "Expected slice clear into greenwood or terminal victory but got zone=${session.config.zoneId}, outcome=${session.runOutcome()}."
        }
        if (turns < OFFICIAL_SLICE_MIN_TURNS) {
            reasons += "Slice finished too quickly: turns=$turns < $OFFICIAL_SLICE_MIN_TURNS."
        }
        if (floor2Turn == null) {
            reasons += "Failed to record floor 2 split."
        } else if (floor2Turn > OFFICIAL_SLICE_MAX_FLOOR2_SPLIT) {
            reasons += "Floor 2 split too slow: floor2Turn=$floor2Turn > $OFFICIAL_SLICE_MAX_FLOOR2_SPLIT."
        }
        if (bossWarningTurn == null) {
            reasons += "Boss warning never became visible."
        }
        if (bossEncounterTurns == null) {
            reasons += "Boss encounter duration could not be derived."
        } else if (bossEncounterTurns > OFFICIAL_SLICE_MAX_BOSS_ENCOUNTER_TURNS) {
            reasons += "Boss encounter too slow: bossEncounterTurns=$bossEncounterTurns > $OFFICIAL_SLICE_MAX_BOSS_ENCOUNTER_TURNS."
        }
        if (sliceCleared) {
            if (session.config.zoneId != "greenwood_fringe") {
                reasons += "Expected shattered_outpost slice to advance into greenwood_fringe but got ${session.config.zoneId}."
            }
            if (session.currentFloor() != 1) {
                reasons += "Expected slice checkpoint to restart on floor 1 after route advance, got floor=${session.currentFloor()}."
            }
        }

        return OfficialSliceStabilityReport(
            professionId = spec.professionId,
            seed = spec.seed,
            localeId = "headless",
            zoneId = "shattered_outpost",
            scriptVersion = OFFICIAL_SLICE_STABILITY_SCRIPT_VERSION,
            success = reasons.isEmpty(),
            outcome = session.runOutcome().toString(),
            turns = turns,
            floor2Turn = floor2Turn,
            bossWarningTurn = bossWarningTurn,
            bossEncounterTurns = bossEncounterTurns,
            routeHash = sha256(routeTrace.joinToString(separator = "|")),
            commandTraceHash = sha256(commandTrace.joinToString(separator = "|")),
            commandTrace = commandTrace,
            failureReason = reasons.firstOrNull(),
        )
    }

    private fun routeStep(session: com.ktome.game.FoundationGameSession): String {
        val position = session.playerPosition()
        return "${session.currentFloor()}@${position.x},${position.y}"
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

private data class OfficialSliceSpec(
    val professionId: String,
    val seed: Long,
)

private data class OfficialSliceStabilityReport(
    val professionId: String,
    val seed: Long,
    val localeId: String,
    val zoneId: String,
    val scriptVersion: String,
    val success: Boolean,
    val outcome: String,
    val turns: Int,
    val floor2Turn: Int?,
    val bossWarningTurn: Int?,
    val bossEncounterTurns: Int?,
    val routeHash: String,
    val commandTraceHash: String,
    val commandTrace: List<String>,
    val failureReason: String?,
) {
    fun toJson() =
        buildJsonObject {
            put("professionId", professionId)
            put("seed", seed)
            put("localeId", localeId)
            put("zoneId", zoneId)
            put("scriptVersion", scriptVersion)
            put("success", success)
            put("outcome", outcome)
            put("turns", turns)
            floor2Turn?.let { put("floor2Turn", it) }
            bossWarningTurn?.let { put("bossWarningTurn", it) }
            bossEncounterTurns?.let { put("bossEncounterTurns", it) }
            put("routeHash", routeHash)
            put("commandTraceHash", commandTraceHash)
            failureReason?.let { put("failureReason", it) }
            putJsonArray("commandTrace") { commandTrace.forEach { add(JsonPrimitive(it)) } }
        }
}
