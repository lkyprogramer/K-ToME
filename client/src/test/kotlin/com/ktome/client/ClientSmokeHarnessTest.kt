package com.ktome.client

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputSource
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.render.AsciiRenderer
import com.ktome.core.map.Point
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.client.screen.MainMenuScreen
import com.ktome.client.screen.MainMenuTextSnapshot
import com.ktome.core.save.AssetVersionContract
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.harness.HarnessReportWriter
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.Localizer
import java.nio.file.Path
import kotlin.math.abs
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ClientSmokeHarnessTest {
    private val clientAssets = ClientAssetBundleLoader.load()

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
            val initialUi = initialSession?.let { session -> captureUiSnapshot(app.localizer(), session.renderSnapshot()) }
            repeat(180) { app.render() }
            val session = app.activeSessionOrNull()
            val finalSnapshot = session?.renderSnapshot()
            val finalUi = finalSnapshot?.let { snapshot -> captureUiSnapshot(app.localizer(), snapshot) }
            val localeId = session?.localizer()?.locale?.id
            val zoneId = finalSnapshot?.metadata?.zoneId
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
                        finalUi?.playerName == expectedPlayerName &&
                        finalUi?.playerRole == expectedPlayerRole &&
                        initialUi?.hudLine?.contains(expectedHudToken) == true &&
                        initialUi?.inventoryTitle == expectedInventoryTitle &&
                        initialUi?.inspectTitle == expectedInspectTitle &&
                        initialUi?.firstMessage == expectedLogLine &&
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
                playerName = finalUi?.playerName,
                playerRole = finalUi?.playerRole,
                hudLine = initialUi?.hudLine,
                inventoryTitle = initialUi?.inventoryTitle,
                inspectTitle = initialUi?.inspectTitle,
                firstMessage = initialUi?.firstMessage,
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
                    } else if (finalUi?.playerName != expectedPlayerName) {
                        "Expected player name $expectedPlayerName, got ${finalUi?.playerName}."
                    } else if (finalUi?.playerRole != expectedPlayerRole) {
                        "Expected player role $expectedPlayerRole, got ${finalUi?.playerRole}."
                    } else if (initialUi?.hudLine?.contains(expectedHudToken) != true) {
                        "Expected HUD line to contain $expectedHudToken, got ${initialUi?.hudLine}."
                    } else if (initialUi?.inventoryTitle != expectedInventoryTitle) {
                        "Expected inventory title $expectedInventoryTitle, got ${initialUi?.inventoryTitle}."
                    } else if (initialUi?.inspectTitle != expectedInspectTitle) {
                        "Expected inspect title $expectedInspectTitle, got ${initialUi?.inspectTitle}."
                    } else if (initialUi?.firstMessage != expectedLogLine) {
                        "Expected first log line $expectedLogLine, got ${initialUi?.firstMessage}."
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
            val initialUi = initialLoaded?.let { loadedSession -> captureUiSnapshot(app.localizer(), loadedSession.renderSnapshot()) }
            repeat(180) { app.render() }
            val loaded = app.activeSessionOrNull()
            val localeId = loaded?.localizer()?.locale?.id
            val finalSnapshot = loaded?.renderSnapshot()
            val finalUi = finalSnapshot?.let { snapshot -> captureUiSnapshot(app.localizer(), snapshot) }
            val zoneId = finalSnapshot?.metadata?.zoneId
            val professionId = loaded?.config?.playerProfessionId
            ClientSmokeReport(
                name = name,
                success =
                    loaded != null &&
                        localeId == expectedLocale.id &&
                        menuSnapshot.subtitle == expectedMenuSubtitle &&
                        menuSnapshot.language == expectedMenuLanguage &&
                        finalUi?.playerName == expectedPlayerName &&
                        finalUi?.playerRole == expectedPlayerRole &&
                        initialUi?.hudLine?.contains(expectedHudToken) == true &&
                        initialUi?.inventoryTitle == expectedInventoryTitle &&
                        initialUi?.inspectTitle == expectedInspectTitle &&
                        initialUi?.firstMessage == expectedLogLine &&
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
                playerName = finalUi?.playerName,
                playerRole = finalUi?.playerRole,
                hudLine = initialUi?.hudLine,
                inventoryTitle = initialUi?.inventoryTitle,
                inspectTitle = initialUi?.inspectTitle,
                firstMessage = initialUi?.firstMessage,
                failureReason =
                    if (loaded == null) {
                        "Continue did not load a session."
                    } else if (localeId != expectedLocale.id) {
                        "Expected locale ${expectedLocale.id}, got $localeId."
                    } else if (menuSnapshot.subtitle != expectedMenuSubtitle) {
                        "Expected menu subtitle $expectedMenuSubtitle, got ${menuSnapshot.subtitle}."
                    } else if (menuSnapshot.language != expectedMenuLanguage) {
                        "Expected menu language label $expectedMenuLanguage, got ${menuSnapshot.language}."
                    } else if (finalUi?.playerName != expectedPlayerName) {
                        "Expected player name $expectedPlayerName, got ${finalUi?.playerName}."
                    } else if (finalUi?.playerRole != expectedPlayerRole) {
                        "Expected player role $expectedPlayerRole, got ${finalUi?.playerRole}."
                    } else if (initialUi?.hudLine?.contains(expectedHudToken) != true) {
                        "Expected HUD line to contain $expectedHudToken, got ${initialUi?.hudLine}."
                    } else if (initialUi?.inventoryTitle != expectedInventoryTitle) {
                        "Expected inventory title $expectedInventoryTitle, got ${initialUi?.inventoryTitle}."
                    } else if (initialUi?.inspectTitle != expectedInspectTitle) {
                        "Expected inspect title $expectedInspectTitle, got ${initialUi?.inspectTitle}."
                    } else if (initialUi?.firstMessage != expectedLogLine) {
                        "Expected first log line $expectedLogLine, got ${initialUi?.firstMessage}."
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

    private fun captureUiSnapshot(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): ClientUiSnapshot {
        val hudLine = AsciiRenderer.hudText(localizer, snapshot)
        val inventoryTitle = AsciiRenderer.sidebarTitle(localizer, UiMode.INVENTORY)
        val inspectTitle = AsciiRenderer.sidebarTitle(localizer, UiMode.INSPECT)
        val firstMessage =
            AsciiRenderer.buildRenderModel(localizer, clientAssets.visualResolver, snapshot, OverlayState(mode = UiMode.MAP))
                .messageLines
                .firstOrNull()
        val player = requireNotNull(snapshot.actors.singleOrNull { actor -> actor.isPlayer }) {
            "Expected a single player actor in render snapshot."
        }
        val inspectLines =
            AsciiRenderer.buildRenderModel(
                localizer,
                clientAssets.visualResolver,
                snapshot,
                OverlayState(
                    mode = UiMode.INSPECT,
                    inspectCursor = Point(snapshot.metadata.playerX, snapshot.metadata.playerY),
                ),
            ).sidebarLines.map { line -> line.text }
        val playerName = localizer.text(player.nameKey)
        val playerRole = inspectLines.firstOrNull { line -> line == localizer.text("actor.player.role") }
        return ClientUiSnapshot(
            hudLine = hudLine,
            inventoryTitle = inventoryTitle,
            inspectTitle = inspectTitle,
            firstMessage = firstMessage,
            playerName = playerName,
            playerRole = playerRole,
        )
    }

    private data class ClientUiSnapshot(
        val hudLine: String,
        val inventoryTitle: String,
        val inspectTitle: String,
        val firstMessage: String?,
        val playerName: String,
        val playerRole: String?,
    )
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
) : CommandSource {
    var issuedCommands: Int = 0
        private set
    var consumedCommands: Int = 0
        private set

    override fun nextCommand(snapshot: RenderSnapshot): PlayerCommand? {
        val command = SnapshotSmokeBot.decide(snapshot)
        issuedCommands += 1
        return command
    }

    override fun onCommandResult(snapshot: RenderSnapshot, command: PlayerCommand, consumed: Boolean) {
        if (consumed) {
            consumedCommands += 1
        }
    }

    override fun overlayState(): com.ktome.client.input.OverlayState =
        com.ktome.client.input.OverlayState(mode = com.ktome.client.input.UiMode.MAP)

    override fun isMapMode(): Boolean = true
}

private object SnapshotSmokeBot {
    fun decide(snapshot: RenderSnapshot): PlayerCommand {
        val player = Point(snapshot.metadata.playerX, snapshot.metadata.playerY)
        val playerCell = snapshot.mapCells.firstOrNull { cell -> cell.x == player.x && cell.y == player.y }
        if (playerCell?.items?.isNotEmpty() == true) {
            return PlayerCommand.PickUp
        }
        when (playerCell?.stairDirectionId) {
            "DOWN" -> return PlayerCommand.Descend
            "UP" -> return PlayerCommand.Ascend
        }

        adjacentHostileDelta(player, snapshot)?.let { return PlayerCommand.Move(it) }
        nextStepTowardGoal(player, snapshot)?.let { return PlayerCommand.Move(it) }
        return PlayerCommand.Wait
    }

    private fun adjacentHostileDelta(player: Point, snapshot: RenderSnapshot): Point? =
        snapshot.uiState.targetablePositions
            .asSequence()
            .map { target -> Point(target.x, target.y) }
            .firstOrNull { target ->
                abs(target.x - player.x) <= 1 &&
                    abs(target.y - player.y) <= 1 &&
                    (target.x != player.x || target.y != player.y)
            }?.let { target -> Point(target.x - player.x, target.y - player.y) }

    private fun nextStepTowardGoal(player: Point, snapshot: RenderSnapshot): Point? {
        val cellsByPoint = snapshot.mapCells.associateBy { cell -> Point(cell.x, cell.y) }
        val goals =
            buildSet {
                snapshot.mapCells
                    .filter { cell ->
                        cell.visibility == CellVisibilitySnapshot.VISIBLE &&
                            (cell.items.isNotEmpty() || cell.stairDirectionId != null)
                    }.forEach { cell -> add(Point(cell.x, cell.y)) }
                snapshot.uiState.targetablePositions.forEach { target -> add(Point(target.x, target.y)) }
            }
        if (goals.isEmpty()) {
            return firstExplorableStep(player, cellsByPoint)
        }

        val queue = ArrayDeque<Point>()
        val previous = mutableMapOf<Point, Point?>()
        queue += player
        previous[player] = null
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current != player && current in goals) {
                var cursor = current
                while (previous[cursor] != player && previous[cursor] != null) {
                    cursor = requireNotNull(previous[cursor])
                }
                return Point(cursor.x - player.x, cursor.y - player.y)
            }
            for (neighbor in neighbors(current)) {
                if (neighbor in previous || !isTraversable(neighbor, cellsByPoint, goals)) {
                    continue
                }
                previous[neighbor] = current
                queue += neighbor
            }
        }
        return firstExplorableStep(player, cellsByPoint)
    }

    private fun firstExplorableStep(
        player: Point,
        cellsByPoint: Map<Point, com.ktome.core.snapshot.MapCellSnapshot>,
    ): Point? =
        neighbors(player)
            .firstOrNull { point -> isTraversable(point, cellsByPoint, emptySet()) }
            ?.let { point -> Point(point.x - player.x, point.y - player.y) }

    private fun isTraversable(
        point: Point,
        cellsByPoint: Map<Point, com.ktome.core.snapshot.MapCellSnapshot>,
        goals: Set<Point>,
    ): Boolean {
        val cell = cellsByPoint[point] ?: return false
        if (cell.visibility == CellVisibilitySnapshot.HIDDEN || cell.terrainTypeId == "wall") {
            return false
        }
        return cell.actorEntityId == null || point in goals
    }

    private fun neighbors(point: Point): List<Point> =
        listOf(
            Point(point.x + 1, point.y),
            Point(point.x, point.y + 1),
            Point(point.x - 1, point.y),
            Point(point.x, point.y - 1),
        )
}
