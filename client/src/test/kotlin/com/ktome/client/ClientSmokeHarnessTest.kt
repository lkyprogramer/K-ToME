package com.ktome.client

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputSource
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.harness.HarnessReportWriter
import com.ktome.game.harness.RunBot
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import com.ktome.core.save.AssetVersionContract
import com.ktome.core.save.SaveManager
import com.ktome.game.PlayerCommand
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ClientSmokeHarnessTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("clientSmoke")
    fun `client smoke covers new game and continue lifecycle`() {
        val saveManager = SaveManager(tempDir.resolve("client-smoke-save"))
        val newGameSource = BotCommandSource()
        val continueSource = BotCommandSource()
        val reports =
            listOf(
                runNewGameSmoke(saveManager, newGameSource),
                runContinueSmoke(saveManager, continueSource),
            )

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "client-smoke",
            payload =
                buildJsonArray {
                    reports.forEach { report ->
                        add(report.toJson())
                    }
                },
            markdown =
                buildString {
                    appendLine("# Client Smoke")
                    reports.forEach { report ->
                        appendLine("- ${report.name}: success=${report.success}, screen=${report.screenName}, floor=${report.floorReached}, turns=${report.turns}, consumed=${report.consumedCommands}")
                    }
                },
        )

        assertTrue(reports.all { it.success }, reports.joinToString(separator = "\n") { "${it.name}: ${it.failureReason}" })
    }

    private fun runNewGameSmoke(
        saveManager: SaveManager,
        botSource: BotCommandSource,
    ): ClientSmokeReport =
        withHeadlessGdx {
            val menuInput = ScriptedInputSource(Keys.ENTER)
            val app =
                GameApp(
                    saveManager = saveManager,
                    defaultConfig = FoundationGameConfig(seed = 20260312L),
                    menuInputSourceFactory = { menuInput },
                    gameCommandSourceFactory = { botSource },
                    outcomeInputSourceFactory = { ScriptedInputSource() },
                    renderEnabled = false,
                    assetVersionProvider = { AssetVersionContract.CURRENT },
                )
            app.create()
            repeat(180) { app.render() }
            val session = app.activeSessionOrNull()
            ClientSmokeReport(
                name = "new-game",
                success = session != null && botSource.consumedCommands > 0 && (session.currentTurnCount() > 0 || session.currentFloor() > 1),
                screenName = app.screen?.javaClass?.simpleName ?: "None",
                floorReached = session?.currentFloor(),
                turns = session?.currentTurnCount(),
                issuedCommands = botSource.issuedCommands,
                consumedCommands = botSource.consumedCommands,
                failureReason =
                    if (session == null) {
                        "Session was not created from main menu."
                    } else if (botSource.consumedCommands <= 0) {
                        "Bot did not consume any command."
                    } else {
                        null
                    },
            ).also {
                app.dispose()
            }
        }

    private fun runContinueSmoke(
        saveManager: SaveManager,
        botSource: BotCommandSource,
    ): ClientSmokeReport {
        val session = GameModule.newFoundationSession(FoundationGameConfig(seed = 20260313L), saveManager)
        repeat(8) {
            session.perform(PlayerCommand.Wait)
        }
        check(session.perform(PlayerCommand.SaveGame)) { "Failed to create save for continue smoke." }

        return withHeadlessGdx {
            val menuInput = ScriptedInputSource(Keys.DOWN, Keys.ENTER)
            val app =
                GameApp(
                    saveManager = saveManager,
                    defaultConfig = FoundationGameConfig(seed = 20260313L),
                    menuInputSourceFactory = { menuInput },
                    gameCommandSourceFactory = { botSource },
                    outcomeInputSourceFactory = { ScriptedInputSource() },
                    renderEnabled = false,
                    assetVersionProvider = { AssetVersionContract.CURRENT },
                )
            app.create()
            repeat(180) { app.render() }
            val loaded = app.activeSessionOrNull()
            ClientSmokeReport(
                name = "continue-game",
                success = loaded != null && botSource.consumedCommands > 0 && loaded.currentTurnCount() >= session.currentTurnCount(),
                screenName = app.screen?.javaClass?.simpleName ?: "None",
                floorReached = loaded?.currentFloor(),
                turns = loaded?.currentTurnCount(),
                issuedCommands = botSource.issuedCommands,
                consumedCommands = botSource.consumedCommands,
                failureReason =
                    if (loaded == null) {
                        "Continue did not load a session."
                    } else if (botSource.consumedCommands <= 0) {
                        "Bot did not consume any command after continue."
                    } else {
                        null
                    },
            ).also {
                app.dispose()
            }
        }
    }

    private fun <T> withHeadlessGdx(block: () -> T): T {
        val backend = HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())
        return try {
            block()
        } finally {
            backend.exit()
        }
    }

    internal data class ClientSmokeReport(
        val name: String,
        val success: Boolean,
        val screenName: String,
        val floorReached: Int?,
        val turns: Int?,
        val issuedCommands: Int,
        val consumedCommands: Int,
        val failureReason: String? = null,
    )
}

private fun ClientSmokeHarnessTest.ClientSmokeReport.toJson() =
    buildJsonObject {
        put("name", name)
        put("success", success)
        put("screenName", screenName)
        floorReached?.let { put("floorReached", it) }
        turns?.let { put("turns", it) }
        put("issuedCommands", issuedCommands)
        put("consumedCommands", consumedCommands)
        failureReason?.let { put("failureReason", it) }
    }

private class ScriptedInputSource(
    vararg keys: Int,
) : InputSource {
    private val queue = ArrayDeque<Int>().apply { keys.forEach(::addLast) }

    override fun isKeyJustPressed(keycode: Int): Boolean =
        if (queue.firstOrNull() == keycode) {
            queue.removeFirst()
            true
        } else {
            false
        }

    override fun isKeyPressed(keycode: Int): Boolean = false
}

private class BotCommandSource(
    private val bot: RunBot = SmokeBot(),
) : CommandSource {
    var issuedCommands: Int = 0
        private set
    var consumedCommands: Int = 0
        private set

    override fun nextCommand(session: FoundationGameSession): PlayerCommand? {
        val command = bot.decide(RunObservationCapture.capture(session, session.currentTurnCount()))
        issuedCommands += 1
        return command
    }

    override fun onCommandResult(
        session: FoundationGameSession,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        if (consumed) {
            consumedCommands += 1
        }
    }

    override fun overlayState(): com.ktome.client.input.OverlayState =
        com.ktome.client.input.OverlayState(mode = com.ktome.client.input.UiMode.MAP)

    override fun isMapMode(): Boolean = true
}
