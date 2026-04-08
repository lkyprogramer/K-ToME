package com.ktome.client

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ScreenUtils
import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.client.audio.AudioCueSink
import com.ktome.client.audio.AudioSinkBindings
import com.ktome.client.audio.AudioSinkBindingsFactory
import com.ktome.client.audio.BackgroundAudioSink
import com.ktome.client.audio.GdxAudioCueSink
import com.ktome.client.audio.GdxBackgroundAudioSink
import com.ktome.client.audio.NoOpAudioCueSink
import com.ktome.client.audio.NoOpBackgroundAudioSink
import com.ktome.client.audio.AudioRouter
import com.ktome.client.input.AudioRouterAwareCommandSource
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputHandler
import com.ktome.client.input.InputHandlerCommandSource
import com.ktome.client.input.InputSource
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.render.TileRenderer
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.map.Point
import com.ktome.core.ai.PendingTelegraphState
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.EffectTracker
import com.ktome.core.ai.BossEncounterState
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.client.screen.MainMenuScreen
import com.ktome.client.screen.MainMenuTextSnapshot
import com.ktome.core.save.AssetVersionContract
import com.ktome.core.save.SaveManager
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.harness.HarnessReportWriter
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.Localizer
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.math.abs
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opentest4j.TestAbortedException

class ClientSmokeHarnessTest {
    private val clientAssets = ClientAssetBundleLoader.load()

    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("clientSmoke")
    fun `client smoke covers new game and continue lifecycle`() {
        val saveManager = SaveManager(tempDir.resolve("client-smoke-save"))
        val newGameSource = SmokeCommandSource()
        val continueSource = SmokeCommandSource()
        val reports =
            listOf(
                runNewGameSmoke(
                    name = "new-game-default-zh",
                    saveManager = saveManager,
                    smokeSource = newGameSource,
                    defaultConfig = FoundationGameConfig(seed = 20260312L),
                    menuInput = ScriptedInputSource(Keys.ENTER),
                    expectedLocale = GameLocale.ZH_CN,
                    expectedMenuSubtitle = "主菜单",
                    expectedMenuLanguage = "语言：简体中文",
                    expectedPlayerName = "英雄",
                    expectedPlayerRole = "玩家",
                    expectedHudToken = "层",
                    expectedZoneDescription = "废弃边境前哨，短局的起点。",
                    expectedLoadoutTitle = "技能装配",
                    expectedInventoryTitle = "背包",
                    expectedInspectTitle = "检视",
                    expectedLogLine = "你进入了地牢。",
                ),
                runNewGameSmoke(
                    name = "new-game-arcanist-shattered-outpost-default-zh",
                    saveManager = saveManager,
                    smokeSource = SmokeCommandSource(),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260318L,
                            zoneId = "shattered_outpost",
                            playerProfessionId = "arcanist",
                        ),
                    menuInput = ScriptedInputSource(Keys.ENTER),
                    expectedLocale = GameLocale.ZH_CN,
                    expectedMenuSubtitle = "主菜单",
                    expectedMenuLanguage = "语言：简体中文",
                    expectedPlayerName = "英雄",
                    expectedPlayerRole = "玩家",
                    expectedHudToken = "层",
                    expectedZoneDescription = "废弃边境前哨，短局的起点。",
                    expectedLoadoutTitle = "技能装配",
                    expectedInventoryTitle = "背包",
                    expectedInspectTitle = "检视",
                    expectedLogLine = "你进入了地牢。",
                ),
                runNewGameSmoke(
                    name = "new-game-toggle-en",
                    saveManager = saveManager,
                    smokeSource = SmokeCommandSource(),
                    defaultConfig = FoundationGameConfig(seed = 20260312L),
                    menuInput = ScriptedInputSource(Keys.L, Keys.ENTER),
                    expectedLocale = GameLocale.EN_US,
                    expectedMenuSubtitle = "Main Menu",
                    expectedMenuLanguage = "Language: English",
                    expectedPlayerName = "Hero",
                    expectedPlayerRole = "Player",
                    expectedHudToken = "FL",
                    expectedZoneDescription = "A ruined border outpost and the opening stretch of the run.",
                    expectedLoadoutTitle = "Loadout",
                    expectedInventoryTitle = "Inventory",
                    expectedInspectTitle = "Inspect",
                    expectedLogLine = "You enter the dungeon.",
                ),
                runNewGameSmoke(
                    name = "new-game-rogue-greenwood-zh",
                    saveManager = saveManager,
                    smokeSource = SmokeCommandSource(),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260316L,
                            zoneId = "greenwood_fringe",
                            playerProfessionId = "rogue",
                        ),
                    menuInput = ScriptedInputSource(Keys.ENTER),
                    expectedLocale = GameLocale.ZH_CN,
                    expectedMenuSubtitle = "主菜单",
                    expectedMenuLanguage = "语言：简体中文",
                    expectedPlayerName = "英雄",
                    expectedPlayerRole = "玩家",
                    expectedHudToken = "层",
                    expectedZoneDescription = "有巡逻压力与视野遮挡的林地边缘。",
                    expectedLoadoutTitle = "技能装配",
                    expectedInventoryTitle = "背包",
                    expectedInspectTitle = "检视",
                    expectedLogLine = "你进入了地牢。",
                ),
                runNewGameSmoke(
                    name = "new-game-templar-grey-gate-en",
                    saveManager = saveManager,
                    smokeSource = SmokeCommandSource(),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260317L,
                            zoneId = "grey_gate_depths",
                            playerProfessionId = "templar",
                        ),
                    menuInput = ScriptedInputSource(Keys.L, Keys.ENTER),
                    expectedLocale = GameLocale.EN_US,
                    expectedMenuSubtitle = "Main Menu",
                    expectedMenuLanguage = "Language: English",
                    expectedPlayerName = "Hero",
                    expectedPlayerRole = "Player",
                    expectedHudToken = "FL",
                    expectedZoneDescription = "The sealed descent leading to the short-run boss.",
                    expectedLoadoutTitle = "Loadout",
                    expectedInventoryTitle = "Inventory",
                    expectedInspectTitle = "Inspect",
                    expectedLogLine = "You enter the dungeon.",
                ),
                runContinueSmoke(
                    name = "continue-zh-after-en-save",
                    saveManager = saveManager,
                    smokeSource = continueSource,
                    savedConfig = FoundationGameConfig(seed = 20260313L),
                    menuInput = ScriptedInputSource(Keys.DOWN, Keys.ENTER),
                    expectedLocale = GameLocale.ZH_CN,
                    expectedZoneId = "shattered_outpost",
                    expectedProfessionId = "vanguard",
                    expectedMenuSubtitle = "主菜单",
                    expectedMenuLanguage = "语言：简体中文",
                    expectedPlayerName = "英雄",
                    expectedPlayerRole = "玩家",
                    expectedHudToken = "层",
                    expectedZoneDescription = "废弃边境前哨，短局的起点。",
                    expectedLoadoutTitle = "技能装配",
                    expectedInventoryTitle = "背包",
                    expectedInspectTitle = "检视",
                    expectedLogLine = "游戏已加载。",
                ),
                runContinueSmoke(
                    name = "continue-rogue-deep-iron-pit-en",
                    saveManager = saveManager,
                    smokeSource = SmokeCommandSource(),
                    savedConfig =
                        FoundationGameConfig(
                            seed = 20260316L,
                            zoneId = "deep_iron_pit",
                            playerProfessionId = "rogue",
                            zoneRoute = FOUNDATION_ZONE_ROUTE,
                            routeIndex = 2,
                        ),
                    menuInput = ScriptedInputSource(Keys.L, Keys.DOWN, Keys.ENTER),
                    expectedLocale = GameLocale.EN_US,
                    expectedZoneId = "deep_iron_pit",
                    expectedProfessionId = "rogue",
                    expectedMenuSubtitle = "Main Menu",
                    expectedMenuLanguage = "Language: English",
                    expectedPlayerName = "Hero",
                    expectedPlayerRole = "Player",
                    expectedHudToken = "FL",
                    expectedZoneDescription = "An industrial mine choked with heat and metal.",
                    expectedLoadoutTitle = "Loadout",
                    expectedInventoryTitle = "Inventory",
                    expectedInspectTitle = "Inspect",
                    expectedLogLine = "Game loaded.",
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

    @Test
    @Tag("clientSmoke")
    fun `shop overlay renders refresh stock service label`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260331L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "vanguard",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = SaveManager(tempDir.resolve("client-shop-refresh-save")),
            )
        automationMovePlayerTo(session, interactablePoint(session, "merchant_stall"))
        assertTrue(session.perform(PlayerCommand.Interact))

        val localizer = session.localizer()
        val capture = captureOverlay(localizer, session.renderSnapshot(), OverlayState(mode = UiMode.SHOP))

        assertTrue(capture.rows.any { row -> row.contains(localizer.text("shop.service.refresh_stock.name")) })
        assertTrue(capture.rows.any { row -> row == localizer.text("ui.controls.shop.close_hint") })
    }

    @Test
    @Tag("clientSmoke")
    fun `inventory overlay renders explicit close hint`() {
        val session =
            GameModule.newFoundationSession(
                saveManager = SaveManager(tempDir.resolve("client-inventory-close-hint-save")),
            )

        val localizer = session.localizer()
        val capture = captureOverlay(localizer, session.renderSnapshot(), OverlayState(mode = UiMode.INVENTORY))

        assertTrue(capture.rows.any { row -> row == localizer.text("ui.controls.inventory.close_hint") })
    }

    @Test
    @Tag("clientSmoke")
    fun `world map overlay labels route rewards`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260331L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "vanguard",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = SaveManager(tempDir.resolve("client-route-reward-save")),
        )
        automationMovePlayerTo(session, requireNotNull(automationStairPoint(session, StairDirection.DOWN)))
        assertTrue(session.perform(PlayerCommand.Descend))
        automationMovePlayerTo(session, requireNotNull(automationStairPoint(session, StairDirection.DOWN)))
        assertTrue(session.perform(PlayerCommand.Descend))

        val localizer = session.localizer()
        val capture = captureOverlay(localizer, session.renderSnapshot(), OverlayState(mode = UiMode.WORLD_MAP))
        val guaranteedRewardLabelPrefix = localizer.text("ui.world_map.guaranteed_reward", "rewards" to "TEST").substringBefore("TEST")
        val milestoneRewardLabelPrefix = localizer.text("ui.world_map.milestone_reward", "reward" to "TEST").substringBefore("TEST")

        assertTrue(capture.rows.any { row -> row.startsWith(guaranteedRewardLabelPrefix) })
        assertTrue(capture.rows.any { row -> row.startsWith(milestoneRewardLabelPrefix) })
    }

    @Test
    @Tag("clientSmoke")
    fun `late run reliquary shop overlay renders reliquary post name`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260331L,
                        zoneId = "abyssal_temple",
                        playerProfessionId = "templar",
                        zoneRoute =
                            listOf(
                                "shattered_outpost",
                                "greenwood_fringe",
                                "deep_iron_pit",
                                "grey_gate_depths",
                                "underground_river",
                                "abyssal_temple",
                            ),
                        routeIndex = 5,
                    ),
                saveManager = SaveManager(tempDir.resolve("client-reliquary-shop-save")),
            )
        automationMovePlayerTo(session, interactablePoint(session, "temple_ward_reliquary"))
        assertTrue(session.perform(PlayerCommand.Interact))

        val localizer = session.localizer()
        val capture = captureOverlay(localizer, session.renderSnapshot(), OverlayState(mode = UiMode.SHOP))

        assertTrue(capture.rows.any { row -> row == localizer.text("shop.abyssal_reliquary_post.name") })
        assertTrue(capture.rows.any { row -> row == localizer.text("ui.shop.hint.reliquary_stock") })
        assertTrue(capture.rows.any { row -> row.contains(localizer.text("shop.service.refresh_stock.name")) })
        assertTrue(capture.rows.any { row -> row.contains(localizer.text("ui.shop.tag.reliquary")) })
        assertTrue(capture.rows.any { row -> row.contains(localizer.text("ui.shop.tag.once")) })
    }

    @Test
    @Tag("clientSmoke")
    fun `client smoke renders cadence reward source label in sidebar`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260331L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = SaveManager(tempDir.resolve("client-cadence-reward-sidebar-save")),
            )
        automationMovePlayerTo(session, requireNotNull(automationStairPoint(session, StairDirection.DOWN)))
        assertTrue(session.perform(PlayerCommand.Descend))

        val localizer = session.localizer()
        val capture = captureOverlay(localizer, session.renderSnapshot(), OverlayState(mode = UiMode.MAP))

        assertTrue(capture.rows.any { row -> row.contains(localizer.text("ui.reward.source.cadence")) })
        assertEquals(1, capture.rows.count { row -> row == localizer.text("ui.sidebar.recent_rewards") })
    }

    @Test
    @Tag("clientSmoke")
    fun `client smoke covers render enabled tile path`() {
        withLwjgl3Gdx(enableAudio = false) {
            val smokeSource = SmokeCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("client-smoke-render-save")),
                    defaultConfig = FoundationGameConfig(seed = 20260312L),
                    menuInputSourceFactory = { ScriptedInputSource(Keys.ENTER) },
                    gameCommandSourceFactory = { smokeSource },
                    outcomeInputSourceFactory = { ScriptedInputSource() },
                    renderEnabled = true,
                    assetVersionProvider = { AssetVersionContract.CURRENT },
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(awaitActiveSession(app)) { "Render-enabled smoke did not create a session." }
                installProfessionReserveTalent(session)
                val capture = captureRenderedUiPath(app, smokeSource)

                assertRenderPath(capture, "Render-enabled smoke")
            } finally {
                app.dispose()
            }
        }
    }

    @Test
    @Tag("clientSmoke")
    fun `client smoke covers audio enabled formal path`() {
        withLwjgl3Gdx(enableAudio = true) {
            val smokeSource = SmokeCommandSource(overlayInput = ScriptedInputSource(Keys.L, Keys.F, Keys.I, Keys.DOWN, Keys.F, Keys.X, Keys.F))
            val audioHarness = RecordingAudioHarness.withGdxDelegates()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("client-smoke-audio-save")),
                    defaultConfig = FoundationGameConfig(seed = 20260312L),
                    menuInputSourceFactory = { ScriptedInputSource(Keys.ENTER) },
                    gameCommandSourceFactory = { smokeSource },
                    outcomeInputSourceFactory = { ScriptedInputSource() },
                    renderEnabled = true,
                    assetVersionProvider = { AssetVersionContract.CURRENT },
                    audioSinkBindingsFactory = audioHarness.factory(),
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(awaitActiveSession(app)) { "Audio-enabled render smoke did not create a session." }
                installProfessionReserveTalent(session)
                val capture = captureRenderedUiPath(app, smokeSource)

                assertRenderPath(capture, "Audio-enabled render smoke")
                assertTrue(
                    audioHarness.backgroundTransitions.containsAll(listOf("audio.music.menu", "audio.zone.shattered_outpost")),
                    "Expected menu and zone background tracks, got ${audioHarness.backgroundTransitions}.",
                )
                assertTrue(
                    audioHarness.cueEvents.count { it == "audio.ui.confirm" } >= 2,
                    "Expected at least two confirm cues, got ${audioHarness.cueEvents}.",
                )
                assertTrue(
                    audioHarness.cueEvents.count { it == "audio.ui.cancel" } >= 1,
                    "Expected at least one cancel cue, got ${audioHarness.cueEvents}.",
                )
                assertTrue(
                    audioHarness.cueEvents.contains("audio.ui.hover"),
                    "Expected hover cue while navigating overlays, got ${audioHarness.cueEvents}.",
                )
            } finally {
                app.dispose()
            }
        }
    }

    @Test
    @Tag("clientSmoke")
    fun `client smoke covers audio enabled boss warning path`() {
        withLwjgl3Gdx(enableAudio = true) {
            val audioHarness = RecordingAudioHarness.withGdxDelegates()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("client-smoke-boss-audio-save")),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260317L,
                            zoneId = "grey_gate_depths",
                            playerProfessionId = "templar",
                        ),
                    menuInputSourceFactory = { ScriptedInputSource() },
                    gameCommandSourceFactory = { PassiveCommandSource() },
                    outcomeInputSourceFactory = { ScriptedInputSource() },
                    renderEnabled = true,
                    assetVersionProvider = { AssetVersionContract.CURRENT },
                    audioSinkBindingsFactory = audioHarness.factory(),
                )
            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for boss warning smoke." }
                val stairsDown = requireNotNull(automationStairPoint(session, StairDirection.DOWN))
                automationMovePlayerTo(session, stairsDown)
                app.render()
                check(session.perform(PlayerCommand.Descend)) { "Failed to descend into boss floor." }
                app.render()

                val bossId = requireNotNull(automationBossEntity(session))
                val bossPoint = requireNotNull(automationWorld(session).get<Position>(bossId)).toPoint()
                automationMovePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
                prepareBossTelegraphFixture(session, bossId)
                app.render()

                val snapshot = waitForBossTelegraph(session, app)
                assertTrue(
                    snapshot.overlays.any { overlay -> overlay.id.startsWith("boss-warning:") },
                    "Expected visible boss warning overlay, got ${snapshot.overlays.map { it.id }}.",
                )
                assertTrue(
                    audioHarness.cueEvents.contains("audio.boss.warning"),
                    "Expected boss warning cue, got ${audioHarness.cueEvents}.",
                )
                triggerTemplarHealFeedback(session)
                app.render()
                val combinedSnapshot = session.renderSnapshot()
                assertTrue(
                    combinedSnapshot.overlays.any { overlay ->
                        overlay.id.startsWith("telegraph:") || overlay.id.startsWith("boss-warning:")
                    },
                    "Expected boss telegraph overlay to remain visible while feedback is rendered.",
                )
                assertTrue(
                    combinedSnapshot.combatFeedbackEvents.any { event ->
                        event.type == com.ktome.core.snapshot.CombatFeedbackTypeSnapshot.HEAL &&
                            event.targetEntityId == session.playerId.value
                    },
                    "Expected typed combat feedback while boss telegraph is visible.",
                )
            } finally {
                app.dispose()
            }
        }
    }

    private fun runNewGameSmoke(
        name: String,
        saveManager: SaveManager,
        smokeSource: SmokeCommandSource,
        defaultConfig: FoundationGameConfig,
        menuInput: ScriptedInputSource,
        expectedLocale: GameLocale,
        expectedMenuSubtitle: String,
        expectedMenuLanguage: String,
        expectedPlayerName: String,
        expectedPlayerRole: String,
        expectedHudToken: String,
        expectedZoneDescription: String,
        expectedLoadoutTitle: String,
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
                    gameCommandSourceFactory = { smokeSource },
                    outcomeInputSourceFactory = { ScriptedInputSource() },
                    renderEnabled = false,
                    assetVersionProvider = { AssetVersionContract.CURRENT },
                )
            app.create()
            val menuSnapshot = captureMenuSnapshot(app, expectedLocale)
            val initialSession = awaitActiveSession(app)
            initialSession?.let(::installProfessionReserveTalent)
            val initialUi = initialSession?.let { session -> captureUiSnapshot(app, smokeSource, session) }
            val session = initialSession?.let { awaitStableGameplaySession(app, smokeSource) }
            val finalSnapshot = session?.renderSnapshot()
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
                        initialUi?.playerName == expectedPlayerName &&
                        initialUi?.playerRole == expectedPlayerRole &&
                        initialUi?.hudLine?.contains(expectedHudToken) == true &&
                        initialUi?.zoneDescription == expectedZoneDescription &&
                        initialUi?.inspectHasKeyStats == true &&
                        initialUi?.enteredLoadout == true &&
                        initialUi?.enteredInventory == true &&
                        initialUi?.enteredInspect == true &&
                        initialUi?.loadoutTitle == expectedLoadoutTitle &&
                        initialUi?.loadoutHasReserveTalent == true &&
                        initialUi?.inventoryTitle == expectedInventoryTitle &&
                        initialUi?.inspectTitle == expectedInspectTitle &&
                        initialUi?.firstMessage == expectedLogLine &&
                        smokeSource.consumedCommands > 0,
                screenName = app.screen?.javaClass?.simpleName ?: "None",
                localeId = localeId,
                zoneId = zoneId,
                professionId = professionId,
                floorReached = session?.currentFloor(),
                turns = session?.currentTurnCount(),
                menuSubtitle = menuSnapshot.subtitle,
                menuLanguage = menuSnapshot.language,
                issuedCommands = smokeSource.issuedCommands,
                consumedCommands = smokeSource.consumedCommands,
                playerName = initialUi?.playerName,
                playerRole = initialUi?.playerRole,
                hudLine = initialUi?.hudLine,
                zoneDescription = initialUi?.zoneDescription,
                loadoutTitle = initialUi?.loadoutTitle,
                inventoryTitle = initialUi?.inventoryTitle,
                inspectTitle = initialUi?.inspectTitle,
                firstMessage = initialUi?.firstMessage,
                failureReason =
                    if (initialSession == null) {
                        "Session was not created from main menu."
                    } else if (session == null) {
                        "Session did not remain active through the smoke gameplay window."
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
                    } else if (initialUi?.playerName != expectedPlayerName) {
                        "Expected player name $expectedPlayerName, got ${initialUi?.playerName}."
                    } else if (initialUi?.playerRole != expectedPlayerRole) {
                        "Expected player role $expectedPlayerRole, got ${initialUi?.playerRole}."
                    } else if (initialUi?.hudLine?.contains(expectedHudToken) != true) {
                        "Expected HUD line to contain $expectedHudToken, got ${initialUi?.hudLine}."
                    } else if (initialUi?.zoneDescription != expectedZoneDescription) {
                        "Expected zone description $expectedZoneDescription, got ${initialUi?.zoneDescription}."
                    } else if (initialUi?.inspectHasKeyStats != true) {
                        "Inspect sidebar is missing key stat overview."
                    } else if (initialUi?.enteredLoadout != true) {
                        "Loadout overlay was not entered through the input path."
                    } else if (initialUi?.enteredInventory != true) {
                        "Inventory overlay was not entered through the input path."
                    } else if (initialUi?.enteredInspect != true) {
                        "Inspect overlay was not entered through the input path."
                    } else if (initialUi?.loadoutTitle != expectedLoadoutTitle) {
                        "Expected loadout title $expectedLoadoutTitle, got ${initialUi?.loadoutTitle}."
                    } else if (initialUi?.loadoutHasReserveTalent != true) {
                        "Loadout overlay did not render any reserve talent row."
                    } else if (initialUi?.inventoryTitle != expectedInventoryTitle) {
                        "Expected inventory title $expectedInventoryTitle, got ${initialUi?.inventoryTitle}."
                    } else if (initialUi?.inspectTitle != expectedInspectTitle) {
                        "Expected inspect title $expectedInspectTitle, got ${initialUi?.inspectTitle}."
                    } else if (initialUi?.firstMessage != expectedLogLine) {
                        "Expected first log line $expectedLogLine, got ${initialUi?.firstMessage}."
                    } else if (smokeSource.consumedCommands <= 0) {
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
        smokeSource: SmokeCommandSource,
        savedConfig: FoundationGameConfig,
        menuInput: ScriptedInputSource,
        expectedLocale: GameLocale,
        expectedZoneId: String,
        expectedProfessionId: String,
        expectedMenuSubtitle: String,
        expectedMenuLanguage: String,
        expectedPlayerName: String,
        expectedPlayerRole: String,
        expectedHudToken: String,
        expectedZoneDescription: String,
        expectedLoadoutTitle: String,
        expectedInventoryTitle: String,
        expectedInspectTitle: String,
        expectedLogLine: String,
    ): ClientSmokeReport {
        val session = GameModule.newFoundationSession(savedConfig, saveManager, GameLocale.EN_US)
        repeat(8) {
            session.perform(PlayerCommand.Wait)
        }
        check(session.perform(PlayerCommand.SaveGame)) { "Failed to create save for continue smoke." }

        return withHeadlessGdx {
            val app =
                GameApp(
                    saveManager = saveManager,
                    defaultConfig = savedConfig,
                    menuInputSourceFactory = { menuInput },
                    gameCommandSourceFactory = { smokeSource },
                    outcomeInputSourceFactory = { ScriptedInputSource() },
                    renderEnabled = false,
                    assetVersionProvider = { AssetVersionContract.CURRENT },
                )
            app.create()
            val menuSnapshot = captureMenuSnapshot(app, expectedLocale)
            val initialLoaded = awaitActiveSession(app)
            initialLoaded?.let(::installProfessionReserveTalent)
            val initialUi = initialLoaded?.let { loadedSession -> captureUiSnapshot(app, smokeSource, loadedSession) }
            val loaded = initialLoaded?.let { awaitStableGameplaySession(app, smokeSource) }
            val localeId = loaded?.localizer()?.locale?.id
            val finalSnapshot = loaded?.renderSnapshot()
            val zoneId = finalSnapshot?.metadata?.zoneId
            val professionId = loaded?.config?.playerProfessionId
            ClientSmokeReport(
                name = name,
                success =
                    loaded != null &&
                        localeId == expectedLocale.id &&
                        zoneId == expectedZoneId &&
                        professionId == expectedProfessionId &&
                        menuSnapshot.subtitle == expectedMenuSubtitle &&
                        menuSnapshot.language == expectedMenuLanguage &&
                        initialUi?.playerName == expectedPlayerName &&
                        initialUi?.playerRole == expectedPlayerRole &&
                        initialUi?.hudLine?.contains(expectedHudToken) == true &&
                        initialUi?.zoneDescription == expectedZoneDescription &&
                        initialUi?.inspectHasKeyStats == true &&
                        initialUi?.enteredLoadout == true &&
                        initialUi?.enteredInventory == true &&
                        initialUi?.enteredInspect == true &&
                        initialUi?.loadoutTitle == expectedLoadoutTitle &&
                        initialUi?.loadoutHasReserveTalent == true &&
                        initialUi?.inventoryTitle == expectedInventoryTitle &&
                        initialUi?.inspectTitle == expectedInspectTitle &&
                        initialUi?.firstMessage == expectedLogLine &&
                        smokeSource.consumedCommands > 0,
                screenName = app.screen?.javaClass?.simpleName ?: "None",
                localeId = localeId,
                zoneId = zoneId,
                professionId = professionId,
                floorReached = loaded?.currentFloor(),
                turns = loaded?.currentTurnCount(),
                menuSubtitle = menuSnapshot.subtitle,
                menuLanguage = menuSnapshot.language,
                issuedCommands = smokeSource.issuedCommands,
                consumedCommands = smokeSource.consumedCommands,
                playerName = initialUi?.playerName,
                playerRole = initialUi?.playerRole,
                hudLine = initialUi?.hudLine,
                zoneDescription = initialUi?.zoneDescription,
                loadoutTitle = initialUi?.loadoutTitle,
                inventoryTitle = initialUi?.inventoryTitle,
                inspectTitle = initialUi?.inspectTitle,
                firstMessage = initialUi?.firstMessage,
                failureReason =
                    if (initialLoaded == null) {
                        "Continue did not load a session."
                    } else if (loaded == null) {
                        "Continue session did not remain active through the smoke gameplay window."
                    } else if (localeId != expectedLocale.id) {
                        "Expected locale ${expectedLocale.id}, got $localeId."
                    } else if (zoneId != expectedZoneId) {
                        "Expected zone $expectedZoneId, got $zoneId."
                    } else if (professionId != expectedProfessionId) {
                        "Expected profession $expectedProfessionId, got $professionId."
                    } else if (menuSnapshot.subtitle != expectedMenuSubtitle) {
                        "Expected menu subtitle $expectedMenuSubtitle, got ${menuSnapshot.subtitle}."
                    } else if (menuSnapshot.language != expectedMenuLanguage) {
                        "Expected menu language label $expectedMenuLanguage, got ${menuSnapshot.language}."
                    } else if (initialUi?.playerName != expectedPlayerName) {
                        "Expected player name $expectedPlayerName, got ${initialUi?.playerName}."
                    } else if (initialUi?.playerRole != expectedPlayerRole) {
                        "Expected player role $expectedPlayerRole, got ${initialUi?.playerRole}."
                    } else if (initialUi?.hudLine?.contains(expectedHudToken) != true) {
                        "Expected HUD line to contain $expectedHudToken, got ${initialUi?.hudLine}."
                    } else if (initialUi?.zoneDescription != expectedZoneDescription) {
                        "Expected zone description $expectedZoneDescription, got ${initialUi?.zoneDescription}."
                    } else if (initialUi?.inspectHasKeyStats != true) {
                        "Inspect sidebar is missing key stat overview."
                    } else if (initialUi?.enteredLoadout != true) {
                        "Loadout overlay was not entered through the input path."
                    } else if (initialUi?.enteredInventory != true) {
                        "Inventory overlay was not entered through the input path."
                    } else if (initialUi?.enteredInspect != true) {
                        "Inspect overlay was not entered through the input path."
                    } else if (initialUi?.loadoutTitle != expectedLoadoutTitle) {
                        "Expected loadout title $expectedLoadoutTitle, got ${initialUi?.loadoutTitle}."
                    } else if (initialUi?.loadoutHasReserveTalent != true) {
                        "Loadout overlay did not render any reserve talent row."
                    } else if (initialUi?.inventoryTitle != expectedInventoryTitle) {
                        "Expected inventory title $expectedInventoryTitle, got ${initialUi?.inventoryTitle}."
                    } else if (initialUi?.inspectTitle != expectedInspectTitle) {
                        "Expected inspect title $expectedInspectTitle, got ${initialUi?.inspectTitle}."
                    } else if (initialUi?.firstMessage != expectedLogLine) {
                        "Expected first log line $expectedLogLine, got ${initialUi?.firstMessage}."
                    } else if (smokeSource.consumedCommands <= 0) {
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

    private fun captureRenderedUiPath(
        app: GameApp,
        smokeSource: SmokeCommandSource,
        frameBudget: Int = 180,
    ): RenderPathCapture {
        var mapHash: String? = null
        var loadoutHash: String? = null
        var inventoryHash: String? = null
        var inspectHash: String? = null

        for (frame in 0 until frameBudget) {
            app.render()
            if (app.activeSessionOrNull() == null) {
                continue
            }
            when (smokeSource.overlayState().mode) {
                UiMode.MAP -> if (mapHash == null) mapHash = frameBufferHash()
                UiMode.LOADOUT_EDIT -> if (loadoutHash == null) loadoutHash = frameBufferHash()
                UiMode.INVENTORY -> if (inventoryHash == null) inventoryHash = frameBufferHash()
                UiMode.INSPECT -> if (inspectHash == null) inspectHash = frameBufferHash()
                else -> Unit
            }
            if (
                mapHash != null &&
                loadoutHash != null &&
                inventoryHash != null &&
                inspectHash != null &&
                smokeSource.entered(UiMode.LOADOUT_EDIT) &&
                smokeSource.entered(UiMode.INVENTORY) &&
                smokeSource.entered(UiMode.INSPECT)
            ) {
                break
            }
        }

        return RenderPathCapture(
            mapHash = mapHash,
            loadoutHash = loadoutHash,
            inventoryHash = inventoryHash,
            inspectHash = inspectHash,
            enteredLoadout = smokeSource.entered(UiMode.LOADOUT_EDIT),
            enteredInventory = smokeSource.entered(UiMode.INVENTORY),
            enteredInspect = smokeSource.entered(UiMode.INSPECT),
        )
    }

    private fun awaitStableGameplaySession(
        app: GameApp,
        smokeSource: SmokeCommandSource,
        frameBudget: Int = 40,
        minimumConsumedCommands: Int = 12,
    ): FoundationGameSession? {
        repeat(frameBudget) {
            val session = app.activeSessionOrNull() ?: return null
            if (smokeSource.consumedCommands >= minimumConsumedCommands && smokeSource.overlayState().mode == UiMode.MAP) {
                return session
            }
            app.render()
        }
        val session = app.activeSessionOrNull() ?: return null
        return session.takeIf {
            smokeSource.consumedCommands >= minimumConsumedCommands &&
                smokeSource.overlayState().mode == UiMode.MAP
        }
    }

    private fun assertRenderPath(
        capture: RenderPathCapture,
        label: String,
    ) {
        assertTrue(capture.enteredLoadout, "$label never entered loadout mode.")
        assertTrue(capture.enteredInventory, "$label never entered inventory mode.")
        assertTrue(capture.enteredInspect, "$label never entered inspect mode.")
        assertTrue(capture.mapHash != null, "$label did not capture a map frame.")
        assertTrue(capture.loadoutHash != null, "$label did not capture a loadout frame.")
        assertTrue(capture.inventoryHash != null, "$label did not capture an inventory frame.")
        assertTrue(capture.inspectHash != null, "$label did not capture an inspect frame.")
        assertEquals(
            4,
            setOf(capture.mapHash, capture.loadoutHash, capture.inventoryHash, capture.inspectHash).size,
            "$label map/loadout/inventory/inspect frames should differ.",
        )
    }

    private fun <T> withLwjgl3Gdx(
        enableAudio: Boolean,
        block: () -> T,
    ): T {
        var result: Result<T>? = null
        val configuration =
            Lwjgl3ApplicationConfiguration().apply {
                setInitialVisible(false)
                if (!enableAudio) {
                    disableAudio(true)
                }
                setHdpiMode(HdpiMode.Pixels)
                setWindowedMode(1280, 800)
                setForegroundFPS(60)
                setIdleFPS(60)
                setPauseWhenLostFocus(false)
                setPauseWhenMinimized(false)
            }

        try {
            Lwjgl3Application(
                object : ApplicationAdapter() {
                    override fun create() {
                        result = runCatching(block)
                        Gdx.app.exit()
                    }
                },
                configuration,
            )
        } catch (exception: RuntimeException) {
            if (isUnavailableLwjglBackend(exception)) {
                throw TestAbortedException(
                    "Skipping render-enabled client smoke because LWJGL3 backend is unavailable in this environment.",
                    exception,
                )
            }
            throw exception
        }

        return requireNotNull(result) {
            "LWJGL3 client smoke did not produce a result."
        }.getOrThrow()
    }

    private fun isUnavailableLwjglBackend(exception: Throwable): Boolean {
        val messages =
            generateSequence<Throwable>(exception) { current -> current.cause }
                .mapNotNull(Throwable::message)
                .joinToString(separator = "\n")
        val stackTrace =
            generateSequence<Throwable>(exception) { current -> current.cause }
                .flatMap { throwable -> throwable.stackTrace.asSequence() }
                .joinToString(separator = "\n") { element -> "${element.className}.${element.methodName}" }
        return listOf(
            "Unable to initialize GLFW",
            "Couldn't create window",
            "Unable to initialize OpenAL",
            "Audio device",
        ).any(messages::contains) ||
            listOf(
                "org.lwjgl.glfw.GLFW.glfwGetMonitorPos",
                "com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.toLwjgl3Monitor",
            ).any(stackTrace::contains)
    }

    private fun frameBufferHash(): String {
        Gdx.gl.glFinish()
        val pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        return try {
            val bytes = ByteArray(pixmap.width * pixmap.height * 4)
            val buffer = pixmap.pixels
            buffer.rewind()
            buffer.get(bytes)
            buffer.rewind()
            MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
        } finally {
            pixmap.dispose()
        }
    }

    private fun waitForBossTelegraph(
        session: FoundationGameSession,
        app: GameApp,
        maxTurns: Int = 4,
    ): RenderSnapshot {
        repeat(maxTurns) { index ->
            val snapshot = session.renderSnapshot()
            val hasTelegraph = snapshot.overlays.any { overlay -> overlay.id.startsWith("telegraph:") }
            if (hasTelegraph) {
                return snapshot
            }
            check(session.perform(PlayerCommand.Wait)) { "Failed to advance boss warning turn at step ${index + 1}." }
            app.render()
        }
        return session.renderSnapshot()
    }

    private fun triggerTemplarHealFeedback(session: FoundationGameSession) {
        val world = automationWorld(session)
        val playerHealth = requireNotNull(world.get<Health>(session.playerId))
        playerHealth.current = (playerHealth.max / 2).coerceAtLeast(1)
        val positiveEnergyPool =
            requireNotNull(requireNotNull(world.get<ResourcePools>(session.playerId)).pool(ResourceType.POSITIVE_ENERGY)) {
                "Expected templar positive energy pool for client smoke feedback."
            }
        positiveEnergyPool.current = positiveEnergyPool.max
        val holyLightSlot = requireNotNull(session.talentSlots().firstOrNull { slot -> slot.talentId == "holy_light" }) {
            "Expected holy_light slot for client smoke feedback."
        }.slot
        check(session.perform(PlayerCommand.UseTalent(slot = holyLightSlot))) { "Expected holy_light to consume a turn during boss warning smoke." }
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
        val zoneDescription: String? = null,
        val loadoutTitle: String? = null,
        val inventoryTitle: String? = null,
        val inspectTitle: String? = null,
        val firstMessage: String? = null,
        val failureReason: String? = null,
    )

    private data class RenderPathCapture(
        val mapHash: String?,
        val loadoutHash: String?,
        val inventoryHash: String?,
        val inspectHash: String?,
        val enteredLoadout: Boolean,
        val enteredInventory: Boolean,
        val enteredInspect: Boolean,
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
        app: GameApp,
        smokeSource: SmokeCommandSource,
        session: FoundationGameSession,
    ): ClientUiSnapshot {
        val localizer = app.localizer()
        val snapshot = session.renderSnapshot()
        val mapModel = TileRenderer.buildRenderModel(localizer, clientAssets.visualResolver, snapshot, OverlayState(mode = UiMode.MAP))
        var loadoutCapture: OverlayCapture? = null
        var inventoryCapture: OverlayCapture? = null
        var inspectCapture: OverlayCapture? = null
        repeat(16) {
            if (loadoutCapture != null && inventoryCapture != null && inspectCapture != null) {
                return@repeat
            }
            app.render()
            val activeSession = app.activeSessionOrNull() ?: return@repeat
            val overlayState = smokeSource.overlayState()
            when (overlayState.mode) {
                UiMode.LOADOUT_EDIT -> {
                    if (loadoutCapture == null) {
                        loadoutCapture = captureOverlay(localizer, activeSession.renderSnapshot(), overlayState)
                    }
                }

                UiMode.INVENTORY -> {
                    if (inventoryCapture == null) {
                        inventoryCapture = captureOverlay(localizer, activeSession.renderSnapshot(), overlayState)
                    }
                }

                UiMode.INSPECT -> {
                    if (inspectCapture == null) {
                        inspectCapture = captureOverlay(localizer, activeSession.renderSnapshot(), overlayState)
                    }
                }

                else -> Unit
            }
        }
        val player = requireNotNull(snapshot.actors.singleOrNull { actor -> actor.isPlayer }) {
            "Expected a single player actor in render snapshot."
        }
        val reserveTalentName =
            snapshot.uiState.reserveTalents.firstOrNull()?.nameKey?.let(localizer::text)
        val inspectLines = inspectCapture?.rows.orEmpty()
        val playerName = localizer.text(player.nameKey)
        val playerRole = inspectLines.firstOrNull { line -> line == localizer.text("actor.player.role") }
        val zoneDescription =
            snapshot.metadata.zoneDescKey
                ?.let(localizer::text)
                ?.takeIf { desc -> mapModel.sidebar.rows.any { row -> row.text == desc } }
        val inspectHasKeyStats =
            inspectLines.any { line -> line.contains(localizer.text("ui.hud.accuracy.short")) && line.contains(localizer.text("ui.hud.evasion.short")) } &&
                inspectLines.any { line -> line.contains(localizer.text("ui.stat.str")) && line.contains(localizer.text("ui.stat.dex")) } &&
                inspectLines.any { line -> line.contains(localizer.text("ui.stat.con")) && line.contains(localizer.text("ui.stat.wil")) } &&
                inspectLines.any { line -> line.contains(localizer.text("ui.hud.speed.short")) }
        return ClientUiSnapshot(
            hudLine = mapModel.hud.summaryText,
            loadoutTitle = loadoutCapture?.title,
            inventoryTitle = inventoryCapture?.title,
            inspectTitle = inspectCapture?.title,
            firstMessage = mapModel.messageLines.firstOrNull()?.text,
            playerName = playerName,
            playerRole = playerRole,
            zoneDescription = zoneDescription,
            inspectHasKeyStats = inspectHasKeyStats,
            loadoutHasReserveTalent = reserveTalentName != null && loadoutCapture?.rows.orEmpty().any { row -> row.contains(reserveTalentName) },
            enteredLoadout = smokeSource.entered(UiMode.LOADOUT_EDIT),
            enteredInventory = smokeSource.entered(UiMode.INVENTORY),
            enteredInspect = smokeSource.entered(UiMode.INSPECT),
        )
    }

    private fun captureOverlay(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): OverlayCapture {
        val model = TileRenderer.buildRenderModel(localizer, clientAssets.visualResolver, snapshot, overlayState)
        return OverlayCapture(
            title = model.sidebar.title,
            rows = model.sidebar.rows.map { row -> row.text },
        )
    }

    private data class OverlayCapture(
        val title: String,
        val rows: List<String>,
    )

    private data class ClientUiSnapshot(
        val hudLine: String,
        val zoneDescription: String?,
        val loadoutTitle: String?,
        val inventoryTitle: String?,
        val inspectTitle: String?,
        val firstMessage: String?,
        val playerName: String,
        val playerRole: String?,
        val inspectHasKeyStats: Boolean,
        val loadoutHasReserveTalent: Boolean,
        val enteredLoadout: Boolean,
        val enteredInventory: Boolean,
        val enteredInspect: Boolean,
    )
}

private fun findOpenAdjacentPoint(
    session: FoundationGameSession,
    center: Point,
): Point {
    val world = automationWorld(session)
    val occupied =
        world.entitiesWith(Position::class, BlocksMovement::class)
            .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
            .toSet()

    return Point.ALL_DIRECTIONS
        .asSequence()
        .map { delta -> center + delta }
        .firstOrNull { point ->
            session.map.isInBounds(point.x, point.y) &&
                !session.map[point].blocksMovement &&
                point !in occupied
        } ?: error("No open adjacent point around $center.")
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
        zoneDescription?.let { put("zoneDescription", it) }
        loadoutTitle?.let { put("loadoutTitle", it) }
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

    override fun onCommandResult(
        previousSnapshot: RenderSnapshot,
        currentSnapshot: RenderSnapshot,
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

private class SmokeCommandSource(
    private val botSource: BotCommandSource = BotCommandSource(),
    overlayInput: ScriptedInputSource = ScriptedInputSource(Keys.L, Keys.F, Keys.I, Keys.F, Keys.X, Keys.F),
) : CommandSource, AudioRouterAwareCommandSource {
    private val uiSource =
        InputHandlerCommandSource(
            inputHandler = InputHandler(overlayInput),
            inputSource = overlayInput,
        )
    private val enteredModes = linkedSetOf(UiMode.MAP)
    private var lastUiCommand: Boolean = false

    val issuedCommands: Int
        get() = botSource.issuedCommands

    val consumedCommands: Int
        get() = botSource.consumedCommands

    override var audioRouter: AudioRouter?
        get() = uiSource.audioRouter
        set(value) {
            uiSource.audioRouter = value
        }

    fun entered(mode: UiMode): Boolean = mode in enteredModes

    override fun nextCommand(snapshot: RenderSnapshot): PlayerCommand? {
        val command = uiSource.nextCommand(snapshot)
        enteredModes += uiSource.overlayState().mode
        if (command != null || !uiSource.isMapMode()) {
            lastUiCommand = true
            return command
        }
        lastUiCommand = false
        return botSource.nextCommand(snapshot)
    }

    override fun onCommandResult(
        previousSnapshot: RenderSnapshot,
        currentSnapshot: RenderSnapshot,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        if (lastUiCommand) {
            uiSource.onCommandResult(previousSnapshot, currentSnapshot, command, consumed)
        } else {
            botSource.onCommandResult(previousSnapshot, currentSnapshot, command, consumed)
        }
    }

    override fun overlayState(): OverlayState = uiSource.overlayState()

    override fun isMapMode(): Boolean = uiSource.isMapMode()

    override fun shouldReturnToMenu(): Boolean = uiSource.shouldReturnToMenu()

    override fun onReturnToMenu() {
        uiSource.onReturnToMenu()
    }

    override fun onSnapshotUpdated(previous: RenderSnapshot?, current: RenderSnapshot) {
        uiSource.onSnapshotUpdated(previous, current)
    }
}

private class PassiveCommandSource : CommandSource, AudioRouterAwareCommandSource {
    override var audioRouter: AudioRouter? = null

    override fun nextCommand(snapshot: RenderSnapshot): PlayerCommand? = null

    override fun overlayState(): OverlayState = OverlayState(mode = UiMode.MAP)

    override fun isMapMode(): Boolean = true

    override fun onSnapshotUpdated(previous: RenderSnapshot?, current: RenderSnapshot) {
        audioRouter?.onSnapshotUpdated(previous, current)
    }
}

private class RecordingAudioHarness(
    private val delegateCueSink: AudioCueSink,
    private val delegateBackgroundSink: BackgroundAudioSink,
    private val disposeDelegates: () -> Unit = {},
) {
    val cueEvents = mutableListOf<String>()
    val backgroundTransitions = mutableListOf<String>()
    private var lastBackgroundKey: String? = null

    fun factory(): AudioSinkBindingsFactory =
        AudioSinkBindingsFactory { renderEnabled ->
            if (renderEnabled) {
                AudioSinkBindings(
                    cueSink =
                        AudioCueSink { cue ->
                            cueEvents += cue.resolvedKey
                            delegateCueSink.emit(cue)
                        },
                    backgroundSink =
                        BackgroundAudioSink { cue ->
                            val resolvedKey = cue?.resolvedKey
                            if (resolvedKey != lastBackgroundKey) {
                                lastBackgroundKey = resolvedKey
                                resolvedKey?.let(backgroundTransitions::add)
                            }
                            delegateBackgroundSink.transitionTo(cue)
                        },
                    dispose = disposeDelegates,
                )
            } else {
                AudioSinkBindings(
                    cueSink = NoOpAudioCueSink,
                    backgroundSink = NoOpBackgroundAudioSink,
                )
            }
        }

    companion object {
        fun withGdxDelegates(): RecordingAudioHarness =
            RecordingAudioHarness(
                delegateCueSink = GdxAudioCueSink,
                delegateBackgroundSink = GdxBackgroundAudioSink,
                disposeDelegates = {
                    GdxBackgroundAudioSink.dispose()
                    GdxAudioCueSink.dispose()
                },
            )
    }
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

private fun automationMovePlayerTo(
    session: FoundationGameSession,
    point: Point,
) {
    invokeSessionInternal(session, "automationMovePlayerTo", arrayOf(Point::class.java), point)
}

private fun interactablePoint(
    session: FoundationGameSession,
    interactableId: String,
): Point {
    val world = automationWorld(session)
    val entityId =
        requireNotNull(
            world.entitiesWith(Position::class, Interactable::class)
                .firstOrNull { candidate -> world.get<Interactable>(candidate)?.id == interactableId },
        )
    return requireNotNull(world.get<Position>(entityId)).toPoint()
}

private fun automationStairPoint(
    session: FoundationGameSession,
    direction: StairDirection,
): Point? =
    invokeSessionInternal(session, "automationStairPoint", arrayOf(StairDirection::class.java), direction) as Point?

private fun automationEntityByTemplateId(
    session: FoundationGameSession,
    templateId: String,
): EntityId? =
    invokeSessionInternal(session, "automationEntityByTemplateId", arrayOf(String::class.java), templateId) as EntityId?

private fun automationBossEntity(session: FoundationGameSession): EntityId? {
    val world = automationWorld(session)
    return world.entitiesWith(Position::class, BossEncounterState::class, Health::class)
        .firstOrNull { entityId -> (world.get<Health>(entityId)?.current ?: 0) > 0 }
}

private fun prepareBossTelegraphFixture(
    session: FoundationGameSession,
    bossId: EntityId,
) {
    val world = automationWorld(session)
    world.get<EffectTracker>(bossId)?.effects?.removeIf { effect -> effect.schemaId == "war_cry_empower" }
    world.remove<PendingTelegraphState>(bossId)
    world.get<CooldownState>(bossId)?.remainingByTalentId?.apply {
        this["war_cry"] = 0
        this["power_strike"] = 99
        this["charge"] = 99
    }
}

private fun invokeSessionInternal(
    session: FoundationGameSession,
    methodName: String,
    parameterTypes: Array<Class<*>> = emptyArray(),
    vararg args: Any?,
): Any? {
    val methods = session.javaClass.methods
    val matchingMethods =
        methods.filter { method ->
            method.name == methodName ||
                method.name.startsWith("${methodName}-") ||
                method.name.startsWith("${methodName}\$")
        }
    val method =
        matchingMethods.firstOrNull()
            ?: error(
            "No internal helper matched $methodName(${parameterTypes.joinToString { it.simpleName }}) on ${session.javaClass.name}. " +
                "Candidates=${methods.map { it.name }.filter { it.startsWith(methodName) || it.contains(methodName) }}",
        )
    method.isAccessible = true
    return method.invoke(session, *args)
}
