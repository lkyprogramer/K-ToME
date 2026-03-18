package com.ktome.client

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputSource
import com.ktome.client.input.UiMode
import com.ktome.client.render.AsciiRenderer
import com.ktome.client.screen.MainMenuScreen
import com.ktome.client.screen.MainMenuTextSnapshot
import com.ktome.core.save.AssetVersionContract
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.harness.HarnessReportWriter
import com.ktome.game.harness.RunBot
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import com.ktome.game.i18n.GameLocale
import java.nio.file.Path
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

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
                runNewGameSmoke(
                    name = "new-game-en",
                    saveManager = saveManager,
                    botSource = newGameSource,
                    defaultConfig = FoundationGameConfig(seed = 20260312L),
                    menuInput = ScriptedInputSource(Keys.ENTER),
                    expectedLocale = GameLocale.EN_US,
                    expectedMenuSubtitle = "Main Menu",
                    expectedMenuLanguage = "Language: English",
                    expectedPlayerName = "Hero",
                    expectedPlayerRole = "Player",
                    expectedHudToken = "FL",
                    expectedInventoryTitle = "Inventory",
                    expectedInspectTitle = "Inspect",
                    expectedLogLine = "You enter the dungeon.",
                ),
                runNewGameSmoke(
                    name = "new-game-arcanist-greenwood-en",
                    saveManager = saveManager,
                    botSource = BotCommandSource(),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260318L,
                            zoneId = "greenwood_fringe",
                            playerProfessionId = "arcanist",
                        ),
                    menuInput = ScriptedInputSource(Keys.ENTER),
                    expectedLocale = GameLocale.EN_US,
                    expectedMenuSubtitle = "Main Menu",
                    expectedMenuLanguage = "Language: English",
                    expectedPlayerName = "Hero",
                    expectedPlayerRole = "Player",
                    expectedHudToken = "FL",
                    expectedInventoryTitle = "Inventory",
                    expectedInspectTitle = "Inspect",
                    expectedLogLine = "You enter the dungeon.",
                ),
                runNewGameSmoke(
                    name = "new-game-zh",
                    saveManager = saveManager,
                    botSource = BotCommandSource(),
                    defaultConfig = FoundationGameConfig(seed = 20260312L),
                    menuInput = ScriptedInputSource(Keys.L, Keys.ENTER),
                    expectedLocale = GameLocale.ZH_CN,
                    expectedMenuSubtitle = "主菜单",
                    expectedMenuLanguage = "语言：简体中文",
                    expectedPlayerName = "英雄",
                    expectedPlayerRole = "玩家",
                    expectedHudToken = "层",
                    expectedInventoryTitle = "背包",
                    expectedInspectTitle = "检视",
                    expectedLogLine = "你进入了地牢。",
                ),
                runContinueSmoke(
                    name = "continue-zh-after-en-save",
                    saveManager = saveManager,
                    botSource = continueSource,
                    menuInput = ScriptedInputSource(Keys.L, Keys.DOWN, Keys.ENTER),
                    expectedLocale = GameLocale.ZH_CN,
                    expectedMenuSubtitle = "主菜单",
                    expectedMenuLanguage = "语言：简体中文",
                    expectedPlayerName = "英雄",
                    expectedPlayerRole = "玩家",
                    expectedHudToken = "层",
                    expectedInventoryTitle = "背包",
                    expectedInspectTitle = "检视",
                    expectedLogLine = "游戏已加载。",
                ),
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
                        appendLine("- ${report.name}: success=${report.success}, zone=${report.zoneId}, profession=${report.professionId}, screen=${report.screenName}, floor=${report.floorReached}, turns=${report.turns}, consumed=${report.consumedCommands}")
                    }
                },
        )

        assertTrue(reports.all { it.success }, reports.joinToString(separator = "\n") { "${it.name}: ${it.failureReason}" })
    }

    private fun runNewGameSmoke(
        name: String,
        saveManager: SaveManager,
        botSource: BotCommandSource,
        defaultConfig: FoundationGameConfig,
        menuInput: ScriptedInputSource,
        expectedLocale: GameLocale,
        expectedMenuSubtitle: String,
        expectedMenuLanguage: String,
        expectedPlayerName: String,
        expectedPlayerRole: String,
        expectedHudToken: String,
        expectedInventoryTitle: String,
        expectedInspectTitle: String,
        expectedLogLine: String,
    ): ClientSmokeReport =
        withHeadlessGdx {
            val app =
                GameApp(
                    saveManager = saveManager,
                    defaultConfig = defaultConfig,
                    menuInputSourceFactory = { menuInput },
                    gameCommandSourceFactory = { botSource },
                    outcomeInputSourceFactory = { ScriptedInputSource() },
                    renderEnabled = false,
                    assetVersionProvider = { AssetVersionContract.CURRENT },
                )
            app.create()
            val menuSnapshot = captureMenuSnapshot(app, expectedLocale)
            val initialSession = awaitActiveSession(app)
            val initialHudLine = initialSession?.let { AsciiRenderer.hudText(it.localizer(), it) }
            val initialInventoryTitle = initialSession?.let { AsciiRenderer.sidebarTitle(it.localizer(), UiMode.INVENTORY) }
            val initialInspectTitle = initialSession?.let { AsciiRenderer.sidebarTitle(it.localizer(), UiMode.INSPECT) }
            val initialMessage = initialSession?.messageLog()?.firstOrNull()
            repeat(180) { app.render() }
            val session = app.activeSessionOrNull()
            val playerName = session?.actorViews()?.singleOrNull { it.isPlayer }?.name
            val localeId = session?.localizer()?.locale?.id
            val playerRole = session?.inspectAt(session.playerPosition())?.actor?.role
            val zoneId = session?.config?.zoneId
            val professionId = session?.config?.playerProfessionId
            ClientSmokeReport(
                name = name,
                success =
                    session != null &&
                        zoneId == defaultConfig.zoneId &&
                        professionId == defaultConfig.playerProfessionId &&
                        localeId == expectedLocale.id &&
                        menuSnapshot.subtitle == expectedMenuSubtitle &&
                        menuSnapshot.language == expectedMenuLanguage &&
                        playerName == expectedPlayerName &&
                        playerRole == expectedPlayerRole &&
                        initialHudLine?.contains(expectedHudToken) == true &&
                        initialInventoryTitle == expectedInventoryTitle &&
                        initialInspectTitle == expectedInspectTitle &&
                        initialMessage == expectedLogLine &&
                        botSource.consumedCommands > 0 &&
                        (session.currentTurnCount() > 0 || session.currentFloor() > 1),
                screenName = app.screen?.javaClass?.simpleName ?: "None",
                localeId = localeId,
                zoneId = zoneId,
                professionId = professionId,
                floorReached = session?.currentFloor(),
                turns = session?.currentTurnCount(),
                menuSubtitle = menuSnapshot.subtitle,
                menuLanguage = menuSnapshot.language,
                issuedCommands = botSource.issuedCommands,
                consumedCommands = botSource.consumedCommands,
                playerName = playerName,
                playerRole = playerRole,
                hudLine = initialHudLine,
                inventoryTitle = initialInventoryTitle,
                inspectTitle = initialInspectTitle,
                firstMessage = initialMessage,
                failureReason =
                    if (session == null) {
                        "Session was not created from main menu."
                    } else if (zoneId != defaultConfig.zoneId) {
                        "Expected zone ${defaultConfig.zoneId}, got $zoneId."
                    } else if (professionId != defaultConfig.playerProfessionId) {
                        "Expected profession ${defaultConfig.playerProfessionId}, got $professionId."
                    } else if (localeId != expectedLocale.id) {
                        "Expected locale ${expectedLocale.id}, got $localeId."
                    } else if (menuSnapshot.subtitle != expectedMenuSubtitle) {
                        "Expected menu subtitle $expectedMenuSubtitle, got ${menuSnapshot.subtitle}."
                    } else if (menuSnapshot.language != expectedMenuLanguage) {
                        "Expected menu language label $expectedMenuLanguage, got ${menuSnapshot.language}."
                    } else if (playerName != expectedPlayerName) {
                        "Expected player name $expectedPlayerName, got $playerName."
                    } else if (playerRole != expectedPlayerRole) {
                        "Expected player role $expectedPlayerRole, got $playerRole."
                    } else if (initialHudLine?.contains(expectedHudToken) != true) {
                        "Expected HUD line to contain $expectedHudToken, got $initialHudLine."
                    } else if (initialInventoryTitle != expectedInventoryTitle) {
                        "Expected inventory title $expectedInventoryTitle, got $initialInventoryTitle."
                    } else if (initialInspectTitle != expectedInspectTitle) {
                        "Expected inspect title $expectedInspectTitle, got $initialInspectTitle."
                    } else if (initialMessage != expectedLogLine) {
                        "Expected first log line $expectedLogLine, got $initialMessage."
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
        name: String,
        saveManager: SaveManager,
        botSource: BotCommandSource,
        menuInput: ScriptedInputSource,
        expectedLocale: GameLocale,
        expectedMenuSubtitle: String,
        expectedMenuLanguage: String,
        expectedPlayerName: String,
        expectedPlayerRole: String,
        expectedHudToken: String,
        expectedInventoryTitle: String,
        expectedInspectTitle: String,
        expectedLogLine: String,
    ): ClientSmokeReport {
        val session = GameModule.newFoundationSession(FoundationGameConfig(seed = 20260313L), saveManager, GameLocale.EN_US)
        repeat(8) {
            session.perform(PlayerCommand.Wait)
        }
        check(session.perform(PlayerCommand.SaveGame)) { "Failed to create save for continue smoke." }

        return withHeadlessGdx {
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
            val menuSnapshot = captureMenuSnapshot(app, expectedLocale)
            val initialLoaded = awaitActiveSession(app)
            val initialHudLine = initialLoaded?.let { AsciiRenderer.hudText(it.localizer(), it) }
            val initialInventoryTitle = initialLoaded?.let { AsciiRenderer.sidebarTitle(it.localizer(), UiMode.INVENTORY) }
            val initialInspectTitle = initialLoaded?.let { AsciiRenderer.sidebarTitle(it.localizer(), UiMode.INSPECT) }
            val initialMessage = initialLoaded?.messageLog()?.firstOrNull()
            repeat(180) { app.render() }
            val loaded = app.activeSessionOrNull()
            val localeId = loaded?.localizer()?.locale?.id
            val playerName = loaded?.actorViews()?.singleOrNull { it.isPlayer }?.name
            val playerRole = loaded?.inspectAt(loaded.playerPosition())?.actor?.role
            val zoneId = loaded?.config?.zoneId
            val professionId = loaded?.config?.playerProfessionId
            ClientSmokeReport(
                name = name,
                success =
                    loaded != null &&
                        localeId == expectedLocale.id &&
                        menuSnapshot.subtitle == expectedMenuSubtitle &&
                        menuSnapshot.language == expectedMenuLanguage &&
                        playerName == expectedPlayerName &&
                        playerRole == expectedPlayerRole &&
                        initialHudLine?.contains(expectedHudToken) == true &&
                        initialInventoryTitle == expectedInventoryTitle &&
                        initialInspectTitle == expectedInspectTitle &&
                        initialMessage == expectedLogLine &&
                        botSource.consumedCommands > 0 &&
                        loaded.currentTurnCount() >= session.currentTurnCount(),
                screenName = app.screen?.javaClass?.simpleName ?: "None",
                localeId = localeId,
                zoneId = zoneId,
                professionId = professionId,
                floorReached = loaded?.currentFloor(),
                turns = loaded?.currentTurnCount(),
                menuSubtitle = menuSnapshot.subtitle,
                menuLanguage = menuSnapshot.language,
                issuedCommands = botSource.issuedCommands,
                consumedCommands = botSource.consumedCommands,
                playerName = playerName,
                playerRole = playerRole,
                hudLine = initialHudLine,
                inventoryTitle = initialInventoryTitle,
                inspectTitle = initialInspectTitle,
                firstMessage = initialMessage,
                failureReason =
                    if (loaded == null) {
                        "Continue did not load a session."
                    } else if (localeId != expectedLocale.id) {
                        "Expected locale ${expectedLocale.id}, got $localeId."
                    } else if (menuSnapshot.subtitle != expectedMenuSubtitle) {
                        "Expected menu subtitle $expectedMenuSubtitle, got ${menuSnapshot.subtitle}."
                    } else if (menuSnapshot.language != expectedMenuLanguage) {
                        "Expected menu language label $expectedMenuLanguage, got ${menuSnapshot.language}."
                    } else if (playerName != expectedPlayerName) {
                        "Expected player name $expectedPlayerName, got $playerName."
                    } else if (playerRole != expectedPlayerRole) {
                        "Expected player role $expectedPlayerRole, got $playerRole."
                    } else if (initialHudLine?.contains(expectedHudToken) != true) {
                        "Expected HUD line to contain $expectedHudToken, got $initialHudLine."
                    } else if (initialInventoryTitle != expectedInventoryTitle) {
                        "Expected inventory title $expectedInventoryTitle, got $initialInventoryTitle."
                    } else if (initialInspectTitle != expectedInspectTitle) {
                        "Expected inspect title $expectedInspectTitle, got $initialInspectTitle."
                    } else if (initialMessage != expectedLogLine) {
                        "Expected first log line $expectedLogLine, got $initialMessage."
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
        val localeId: String?,
        val zoneId: String? = null,
        val professionId: String? = null,
        val floorReached: Int?,
        val turns: Int?,
        val menuSubtitle: String? = null,
        val menuLanguage: String? = null,
        val issuedCommands: Int,
        val consumedCommands: Int,
        val playerName: String? = null,
        val playerRole: String? = null,
        val hudLine: String? = null,
        val inventoryTitle: String? = null,
        val inspectTitle: String? = null,
        val firstMessage: String? = null,
        val failureReason: String? = null,
    )

    private fun captureMenuSnapshot(
        app: GameApp,
        expectedLocale: GameLocale,
    ): MainMenuTextSnapshot {
        repeat(4) {
            val menu = app.screen as? MainMenuScreen
            if (menu != null && app.currentLocale() == expectedLocale) {
                return menu.textSnapshot()
            }
            app.render()
        }
        error("Failed to capture main menu snapshot for locale ${expectedLocale.id}.")
    }

    private fun awaitActiveSession(
        app: GameApp,
        frameBudget: Int = 8,
    ): FoundationGameSession? {
        repeat(frameBudget) {
            app.activeSessionOrNull()?.let { session -> return session }
            app.render()
        }
        return app.activeSessionOrNull()
    }
}

private fun ClientSmokeHarnessTest.ClientSmokeReport.toJson() =
    buildJsonObject {
        put("name", name)
        put("success", success)
        put("screenName", screenName)
        localeId?.let { put("localeId", it) }
        zoneId?.let { put("zoneId", it) }
        professionId?.let { put("professionId", it) }
        floorReached?.let { put("floorReached", it) }
        turns?.let { put("turns", it) }
        menuSubtitle?.let { put("menuSubtitle", it) }
        menuLanguage?.let { put("menuLanguage", it) }
        put("issuedCommands", issuedCommands)
        put("consumedCommands", consumedCommands)
        playerName?.let { put("playerName", it) }
        playerRole?.let { put("playerRole", it) }
        hudLine?.let { put("hudLine", it) }
        inventoryTitle?.let { put("inventoryTitle", it) }
        inspectTitle?.let { put("inspectTitle", it) }
        firstMessage?.let { put("firstMessage", it) }
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
