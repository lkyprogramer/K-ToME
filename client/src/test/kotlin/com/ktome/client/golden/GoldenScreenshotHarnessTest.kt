package com.ktome.client.golden

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ScreenUtils
import com.ktome.client.GameApp
import com.ktome.client.automationWorld
import com.ktome.client.installReserveTalent
import com.ktome.client.input.CommandSource
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.client.input.InputSource
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.render.TileRenderer
import com.ktome.client.ui.combat.CombatDecisionFrame
import com.ktome.client.ui.combat.CombatDecisionFrameState
import com.ktome.client.ui.combat.CombatDecisionPhase
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.client.ui.layout.ModalFrameLocalState
import com.ktome.core.ai.BossEncounterState
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.item.AffixDef
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.StatModifier
import com.ktome.core.loot.RarityTier
import com.ktome.core.map.Point
import com.ktome.core.mapgen.center
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.world.solvability.SearchActionResult
import com.ktome.core.world.solvability.SearchBindingId
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.ItemFactory
import com.ktome.game.i18n.GameLocale
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opentest4j.TestAbortedException

@Tag("goldenScreenshot")
class GoldenScreenshotHarnessTest {
    @TempDir
    lateinit var tempDir: Path

    private val optPr03ItemBundle = DataLoader().loadItemBundle()
    private val sampleSecretBindingId = SearchBindingId("search.underground_river.crystal_rift")

    @Test
    fun `golden screenshot hashes remain stable for english and chinese formal screens`() {
        val english = captureGoldenSet(GameLocale.EN_US, "en-us")
        val chinese = captureGoldenSet(GameLocale.ZH_CN, "zh-cn")

        assertEquals(
            listOf(
                "10c7931dc33386a9b673866a3c2b44e319855084093bef4459fc22c84bde46d7",
                "fab6098d8a8d56a6158a05d2c0896a7a146f0d0357aebd00d72d26d2be0b936d",
                "15338a89f1cbabb025b947dc46f231be2d839894536f954f39e631f952a6002a",
                "0b4e7496eb699b7907b02ae1b726eae0fbab0e2ea20c8603c2ec072d548dddce",
                "8bf3679e3604e9e790adc5855e1e75b6a2ee6ea42b1250e455f89d27c5c492d3",
                "93752d2697f8db4bdc40ff058ea62ec30836edb4f37770785557cef621d2eae9",
                "5cfeadeb46dc23ce7ed1ff1ed68f072d8b8197d16977729f6fb3bff8b47f87a0",
                "3970263d124125fef4336d74d042acf374d6c185a12565ac4115b957be76cc47",
                "b2ba545cb1bd9bfbff966b4efb0a9532083138e598a2b2ffd52293cd02c0ed40",
                "68151636323f76535c0f3a8b342ff87c71ba206f5dc99850c26ffab1b5e2eb94",
            ),
            english + chinese,
        )
    }

    @Test
    fun `boss warning golden hashes remain stable for english and chinese`() {
        val english = captureBossWarningHash(GameLocale.EN_US, "boss-warning-en")
        val chinese = captureBossWarningHash(GameLocale.ZH_CN, "boss-warning-zh")

        assertEquals(
            listOf(
                "9099d7b6f55a4cbba07e3ebf0f5936e39f345a06819fe2acd41c61540e05e0e4",
                "f8bbf11edc358df2fe2a32d534802f29ba0bce50d493561189b2c3e88a13d955",
            ),
            listOf(english, chinese),
        )
    }

    @Test
    fun `route midpoint rogue golden hashes remain stable for english and chinese`() {
        val english = captureRouteMidpointSet(GameLocale.EN_US, "route-mid-rogue-en")
        val chinese = captureRouteMidpointSet(GameLocale.ZH_CN, "route-mid-rogue-zh")

        assertEquals(
            listOf(
                "96f220fca1388bb96065feac1dcb9b913a0d1c99d56c593e9dd11bea43e2278e",
                "9afaa35eeed39bfa9f9f58f6ee3b7af8e0a331539176648a9f554a39c3465393",
                "8af028a537d8dbd3a9dbd9601f52bd14818f3c63db68f69ee6e195eac38d2d46",
                "e8698aa57a4245b31d414a5fc271099a3dfa045da6ee983f362ccb68814491d6",
                "763191e759524085b44456409d1d33f838e8d76fcf31c245d179ed19062c9f3a",
                "0c5f8a16351f0b88180d51f08bd208de92813720cef32f66293260255b835dd8",
            ),
            english + chinese,
        )
    }

    @Test
    fun `golden map path snapshot keeps frontstage readability cues available`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260331L, zoneId = "greenwood_fringe", playerProfessionId = "rogue"),
                saveManager = SaveManager(tempDir.resolve("golden-frontstage-snapshot")),
            )

        assertFalse(session.perform(PlayerCommand.Search))

        val snapshot = session.renderSnapshot()
        assertTrue(snapshot.uiState.frontstageReadability.terrainHighlights.size <= 2)
        assertTrue(snapshot.uiState.frontstageReadability.recentActionCues.any { cue -> cue.message.key == "log.search.no_target" })
    }

    @Test
    fun `gameplay log emphasis hashes remain stable for english and chinese`() {
        val english =
            listOf(
                captureFormalLogHash(GameLocale.EN_US, "log-tone-formal-en"),
                captureRouteMidpointLogHash(GameLocale.EN_US, "log-tone-route-en"),
                captureBossWarningLogHash(GameLocale.EN_US, "log-tone-boss-en"),
            )
        val chinese =
            listOf(
                captureFormalLogHash(GameLocale.ZH_CN, "log-tone-formal-zh"),
                captureRouteMidpointLogHash(GameLocale.ZH_CN, "log-tone-route-zh"),
                captureBossWarningLogHash(GameLocale.ZH_CN, "log-tone-boss-zh"),
            )

        assertEquals(
            listOf(
                "dfb98064a0fa150e697ccf32ada9cd6526b87603dc9679f18ada9d22b24fa9af",
                "ad961a761d871fb817a1e53fd5e655e1d09be0aff27af087a0e98d079fb60316",
                "e6f013d595d43277ed08575ed591e7741f769b208cfa6f7d311ea5a0ed36a17f",
                "4ba641356d30ac1bcf7ceb9528d722d4ebe60145e87bb580a3c13034c9949098",
                "216504e492abdebeaf797e3a8972e0f7a9702a8a7da0fdbd8058b01026b68e04",
                "13dbba5c925db8d65816a6acad81f228a0391d3c5a373afde45d56da11af0cb3",
            ),
            english + chinese,
        )
    }

    @Test
    fun `outcome recap golden hashes remain stable for english and chinese`() {
        val english = captureOutcomeSet(GameLocale.EN_US, "outcome-en")
        val chinese = captureOutcomeSet(GameLocale.ZH_CN, "outcome-zh")

        assertEquals(
            listOf(
                "450e7caf5c08a15a48c92829f8bdfbacd38b2f5291fb61f4696ec0d325e53633",
                "2089b6e6299b466ee98e8b97534d41e434e256211b8774fbad788b8eb847dad5",
                "1b2502c2e7fd673dde69ef26ed55b91ba246883c2a10d3a470e74a9c7853647a",
                "d896340bd8187b1d68422dc5623f565d0d7a409eed9fd568bcb57d12459aee23",
            ),
            english + chinese,
        )
    }

    @Test
    fun `sample pack golden hash remains stable for filesystem backed content`() {
        val hash = captureSamplePackRuntimeHash()

        assertEquals("612be70495ad1e82991ce612dce0fb29e74af392c8e97488136004408d043bf6", hash)
    }

    @Test
    fun `phase4 uiux pr03 item and ground loot golden hashes remain stable for english and chinese`() {
        val english = capturePhase4UiuxPr03Set(GameLocale.EN_US, "phase4-uiux-pr03-items-en")
        val chinese = capturePhase4UiuxPr03Set(GameLocale.ZH_CN, "phase4-uiux-pr03-items-zh")

        assertEquals(
            listOf(
                "48141a05055719cbc96f347e169a40da0f93fd501e50047259ed32c3b6ee8376",
                "24344425196986d58cf6fa22f7ee4a6578eabe9f84420a4c6feafb805cabb6f0",
                "7e8f3320fec8decd80e1fb8e60305f91a7e56dc9bf5cad4ea21fc603b50f433e",
                "948460e945d46f0cb1d9ad44406fbc81d58f7d159288a3ab4f22b2cc796bcd3f",
                "e4c4f2ce523cdb5fb9904dfb5bcbf9d66bf3ade5cd7c5af2c9bc588be2522d31",
                "05f0cb802735a1854d3da0e59e6139d2800b9097b0afbdc0ef954398fcad28dd",
                "597ad5b3b902810f72d9a331f17073b93006fb2ee741510ef6f2295657cf3d1a",
                "9b8daff754a4830712f95a600b063e523b01c7daeb73db4bc81bd127465a7f23",
                "256d8ab0880ba035e9c4d1d2c5575fd486406d11493df43df4bcfa1debcf889c",
                "7edec6f96e64829b1d97cdece2f2435e132e3efc5b50b0bf91c4847046457481",
            ),
            english + chinese,
        )
    }

    @Test
    fun `phase4 uiux pr05 telegraph and combat decision hashes remain stable`() {
        val hashes =
            listOf(
                "phase4-uiux-pr05-telegraph-triple-surface",
                "phase4-uiux-pr05-combat-action",
                "phase4-uiux-pr05-combat-method",
                "phase4-uiux-pr05-combat-target",
                "phase4-uiux-pr05-combat-disabled-resource",
                "phase4-uiux-pr05-combat-illegal-target",
            ).zip(capturePhase4UiuxPr05Set()).toMap()

        assertEquals(
            mapOf(
                "phase4-uiux-pr05-telegraph-triple-surface" to "129d42db2bdc12f2d955c3551671749624341f74c1a58a4204f11c804c05cce0",
                "phase4-uiux-pr05-combat-action" to "16d6af8e3644ac4e39c23e6b83ee11601a378105cd5698fa7081d16a38cb9a1d",
                "phase4-uiux-pr05-combat-method" to "a18d019199db3e26e658b55698b7c63c93bc42e89c8fe85396a0910ecfd84228",
                "phase4-uiux-pr05-combat-target" to "e7f74536e4677cb8712062a045972d6042f23153529edf95c775dcc857f1d36d",
                "phase4-uiux-pr05-combat-disabled-resource" to "16d6af8e3644ac4e39c23e6b83ee11601a378105cd5698fa7081d16a38cb9a1d",
                "phase4-uiux-pr05-combat-illegal-target" to "e39d965b07d822bec08a06dc66ab540f0618a8b654b5e04ea6dc96b394b30648",
            ),
            hashes,
        )
    }

    private fun captureGoldenSet(
        locale: GameLocale,
        saveFolderName: String,
    ): List<String> =
        captureGameplaySet(
            locale = locale,
            saveFolderName = saveFolderName,
            defaultConfig =
                FoundationGameConfig(
                    seed = 20260318L,
                    zoneId = "shattered_outpost",
                    playerProfessionId = "vanguard",
                ),
            includeMenu = true,
            includeLoadout = true,
        )

    private fun captureRouteMidpointSet(
        locale: GameLocale,
        saveFolderName: String,
    ): List<String> =
        captureGameplaySet(
            locale = locale,
            saveFolderName = saveFolderName,
            defaultConfig =
                FoundationGameConfig(
                    seed = 20260316L,
                    zoneId = "deep_iron_pit",
                    playerProfessionId = "rogue",
                    zoneRoute = FOUNDATION_ZONE_ROUTE,
                    routeIndex = 2,
                ),
        )

    private fun captureFormalLogHash(
        locale: GameLocale,
        saveFolderName: String,
    ): String =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve(saveFolderName)),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260318L,
                            zoneId = "shattered_outpost",
                            playerProfessionId = "vanguard",
                        ),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = locale,
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for formal log golden capture." }
                installReserveTalent(session, "charge", fixtureLabel = "golden loadout fixture")
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                captureGameplayLogHash(session) { repeat(2) { app.render() } }
            } finally {
                app.dispose()
            }
        }

    private fun captureRouteMidpointLogHash(
        locale: GameLocale,
        saveFolderName: String,
    ): String =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve(saveFolderName)),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260316L,
                            zoneId = "deep_iron_pit",
                            playerProfessionId = "rogue",
                            zoneRoute = FOUNDATION_ZONE_ROUTE,
                            routeIndex = 2,
                        ),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = locale,
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for route log golden capture." }
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                captureGameplayLogHash(session) { repeat(2) { app.render() } }
            } finally {
                app.dispose()
            }
        }

    private fun captureGameplaySet(
        locale: GameLocale,
        saveFolderName: String,
        defaultConfig: FoundationGameConfig,
        includeMenu: Boolean = false,
        includeLoadout: Boolean = false,
    ): List<String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve(saveFolderName)),
                    defaultConfig = defaultConfig,
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = locale,
                )

            try {
                app.create()
                val hashes = mutableListOf<String>()
                if (includeMenu) {
                    hashes += captureHash { repeat(2) { app.render() } }
                }

                app.startNewGame()
                if (includeLoadout) {
                    installReserveTalent(
                        requireNotNull(app.activeSessionOrNull()) { "Expected an active session for loadout golden capture." },
                        "charge",
                        fixtureLabel = "golden loadout fixture",
                    )
                }
                hashes +=
                    captureHash {
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        repeat(2) { app.render() }
                    }
                hashes +=
                    captureHash {
                        overlaySource.overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 0)
                        repeat(2) { app.render() }
                    }
                if (includeLoadout) {
                    hashes +=
                        captureHash {
                            overlaySource.overlayState = OverlayState(mode = UiMode.LOADOUT_EDIT, loadoutSlotSelection = 1, loadoutReserveSelection = 0)
                            repeat(2) { app.render() }
                        }
                }
                hashes +=
                    captureHash {
                        overlaySource.overlayState = OverlayState(mode = UiMode.INSPECT)
                        repeat(2) { app.render() }
                    }
                hashes
            } finally {
                app.dispose()
            }
        }

    private fun captureBossWarningHash(
        locale: GameLocale,
        saveFolderName: String,
    ): String =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve(saveFolderName)),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260317L,
                            zoneId = "grey_gate_depths",
                            playerProfessionId = "templar",
                        ),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = locale,
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected an active session for overlay golden capture." }
                val stairsDown = requireNotNull(automationStairPoint(session, StairDirection.DOWN)) { "Expected a downstairs entry for boss-warning golden capture." }
                automationMovePlayerTo(session, stairsDown)
                app.render()
                check(session.perform(PlayerCommand.Descend)) { "Failed to descend into the boss floor for locale ${locale.id}." }
                app.render()

                val bossId = requireNotNull(automationBossEntity(session)) { "Expected a live boss entity for overlay golden capture." }
                val bossPoint = requireNotNull(automationWorld(session).get<Position>(bossId)) { "Expected boss position for overlay golden capture." }.toPoint()
                automationMovePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
                prepareBossTelegraphFixture(session, bossId)
                app.render()

                val snapshot = waitForBossTelegraph(session, app)
                assertTrue(snapshot.overlays.any { overlay -> overlay.id.startsWith("boss-warning:") })
                triggerTemplarHealFeedback(session)
                val combinedSnapshot = session.renderSnapshot()
                assertTrue(
                    combinedSnapshot.combatFeedbackEvents.any { event ->
                        event.type == com.ktome.core.snapshot.CombatFeedbackTypeSnapshot.HEAL &&
                            event.targetEntityId == session.playerId.value
                    },
                )
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                return@withLwjgl3Context captureHash { repeat(2) { app.render() } }
            } finally {
                app.dispose()
            }
        }

    private fun captureBossWarningLogHash(
        locale: GameLocale,
        saveFolderName: String,
    ): String =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve(saveFolderName)),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260317L,
                            zoneId = "grey_gate_depths",
                            playerProfessionId = "templar",
                        ),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = locale,
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for boss log golden capture." }
                val stairsDown = requireNotNull(automationStairPoint(session, StairDirection.DOWN)) { "Expected downstairs entry for boss log golden capture." }
                automationMovePlayerTo(session, stairsDown)
                app.render()
                check(session.perform(PlayerCommand.Descend)) { "Failed to descend into boss floor for locale ${locale.id}." }
                app.render()

                val bossId = requireNotNull(automationBossEntity(session)) { "Expected a live boss entity for boss log golden capture." }
                val bossPoint = requireNotNull(automationWorld(session).get<Position>(bossId)) { "Expected boss position for boss log golden capture." }.toPoint()
                automationMovePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
                prepareBossTelegraphFixture(session, bossId)
                waitForBossTelegraph(session, app)
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                captureGameplayLogHash(session) { repeat(2) { app.render() } }
            } finally {
                app.dispose()
            }
        }

    private fun captureOutcomeSet(
        locale: GameLocale,
        saveFolderName: String,
    ): List<String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve(saveFolderName)),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260322L,
                            zoneId = "shattered_outpost",
                            playerProfessionId = "vanguard",
                        ),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = locale,
                )

            try {
                app.create()
                val defeatHash =
                    run {
                        app.startNewGame()
                        val defeatSession = requireNotNull(app.activeSessionOrNull()) { "Expected an active session for defeat golden capture." }
                        automationForceDefeatPlayer(defeatSession)
                        app.showOutcome(defeatSession)
                        captureHash { repeat(2) { app.render() } }
                    }
                val victoryHash =
                    run {
                        app.startNewGame()
                        val victorySession = requireNotNull(app.activeSessionOrNull()) { "Expected an active session for victory golden capture." }
                        val stairsDown = requireNotNull(automationStairPoint(victorySession, StairDirection.DOWN)) { "Expected downstairs for victory golden capture." }
                        automationMovePlayerTo(victorySession, stairsDown)
                        app.render()
                        check(victorySession.perform(PlayerCommand.Descend)) { "Failed to descend for victory golden capture in locale ${locale.id}." }
                        app.render()
                        val bossId = requireNotNull(automationEntityByTemplateId(victorySession, "bandit.captain")) { "Expected bandit captain for victory golden capture." }
                        val bossPoint = requireNotNull(automationWorld(victorySession).get<Position>(bossId)) { "Expected boss position for victory golden capture." }.toPoint()
                        requireNotNull(automationWorld(victorySession).get<com.ktome.core.ecs.Health>(bossId)).current = 1
                        val attackOrigin = if (bossPoint.x > 0) Point(bossPoint.x - 1, bossPoint.y) else Point(bossPoint.x + 1, bossPoint.y)
                        automationMovePlayerTo(victorySession, attackOrigin)
                        app.render()
                        check(victorySession.perform(PlayerCommand.Move(bossPoint - attackOrigin))) { "Failed to finish boss for victory golden capture in locale ${locale.id}." }
                        app.showOutcome(victorySession)
                        captureHash { repeat(2) { app.render() } }
                    }
                listOf(defeatHash, victoryHash)
            } finally {
                app.dispose()
            }
        }

    private fun captureSamplePackRuntimeHash(): String =
        withLwjgl3Context(width = 1280, height = 800) {
            val selection = samplePackSelection()
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("sample-pack-golden")),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260316L,
                            zoneId = "underground_river",
                            playerProfessionId = "arcanist",
                        ),
                    contentPackSelection = selection,
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = GameLocale.EN_US,
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active sample-pack session for golden capture." }
                val inventorySelection = claimSamplePackReward(session)
                overlaySource.overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = inventorySelection)
                captureHash {
                    repeat(2) { app.render() }
                }
            } finally {
                app.dispose()
            }
        }

    private fun capturePhase4UiuxPr03Set(
        locale: GameLocale,
        saveFolderName: String,
    ): List<String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve(saveFolderName)),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260409L,
                            zoneId = "underground_river",
                            playerProfessionId = "rogue",
                        ),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = locale,
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for phase4 UI/UX PR-03 golden capture." }
                val selections = installOptPr03InspectFixtures(session)
                val inventoryHashes =
                    selections.map { inventorySelection ->
                        captureHash {
                            overlaySource.overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = inventorySelection)
                            repeat(2) { app.render() }
                        }
                    }
                installOptPr03GroundLootFixtures(session)
                inventoryHashes +
                    captureHash {
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        repeat(2) { app.render() }
                    }
            } finally {
                app.dispose()
            }
        }

    private fun capturePhase4UiuxPr05Set(): List<String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("phase4-uiux-pr05-combat-decision")),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260412L,
                            zoneId = "grey_gate_depths",
                            playerProfessionId = "templar",
                        ),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = GameLocale.ZH_CN,
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for phase4 UI/UX PR-05 golden capture." }
                val stairsDown = requireNotNull(automationStairPoint(session, StairDirection.DOWN)) { "Expected downstairs entry for PR-05 golden capture." }
                automationMovePlayerTo(session, stairsDown)
                app.render()
                check(session.perform(PlayerCommand.Descend)) { "Failed to descend into the boss floor for PR-05 golden capture." }
                app.render()

                val bossId = requireNotNull(automationBossEntity(session)) { "Expected a live boss entity for PR-05 golden capture." }
                val bossPoint = requireNotNull(automationWorld(session).get<Position>(bossId)) { "Expected boss position for PR-05 golden capture." }.toPoint()
                automationMovePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
                prepareBossTelegraphFixture(session, bossId)
                waitForBossTelegraph(session, app)

                val snapshot = session.renderSnapshot()
                val actionSlot = requireNotNull(snapshot.uiState.talents.firstOrNull()) { "Expected a combat action for PR-05 golden capture." }.slot
                val actionId = "talent:$actionSlot"
                val targetPoint = snapshot.uiState.targetablePositions.firstOrNull()?.let { point -> Point(point.x, point.y) } ?: bossPoint
                val illegalPoint = Point(0, 0).takeUnless { point -> point == targetPoint } ?: Point(snapshot.metadata.width - 1, snapshot.metadata.height - 1)
                val targetState =
                    CombatDecisionFrameState(
                        phase = CombatDecisionPhase.TARGET,
                        selectedActionId = actionId,
                        selectedMethodId = "default",
                        skippedMethod = true,
                    )

                val hashes = mutableListOf<String>()
                hashes +=
                    captureHash {
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        repeat(2) { app.render() }
                    }
                hashes +=
                    captureHash {
                        overlaySource.overlayState = combatDecisionOverlay(CombatDecisionFrame.initialState)
                        repeat(2) { app.render() }
                    }
                hashes +=
                    captureHash {
                        overlaySource.overlayState =
                            combatDecisionOverlay(
                                CombatDecisionFrameState(
                                    phase = CombatDecisionPhase.METHOD,
                                    selectedActionId = actionId,
                                    selectedMethodId = null,
                                ),
                            )
                        repeat(2) { app.render() }
                    }
                hashes +=
                    captureHash {
                        overlaySource.overlayState = combatDecisionOverlay(targetState, targetPoint)
                        repeat(2) { app.render() }
                    }

                requireNotNull(
                    requireNotNull(automationWorld(session).get<ResourcePools>(session.playerId)) {
                        "Expected player resource pools for PR-05 low-resource golden capture."
                    }.pool(ResourceType.POSITIVE_ENERGY),
                ) {
                    "Expected positive energy pool for PR-05 low-resource golden capture."
                }.current = 0
                hashes +=
                    captureHash {
                        overlaySource.overlayState = combatDecisionOverlay(CombatDecisionFrame.initialState)
                        repeat(2) { app.render() }
                    }
                hashes +=
                    captureHash {
                        overlaySource.overlayState = combatDecisionOverlay(targetState, illegalPoint)
                        repeat(2) { app.render() }
                    }
                hashes
            } finally {
                app.dispose()
            }
        }

    private fun combatDecisionOverlay(
        state: CombatDecisionFrameState,
        cursor: Point? = null,
    ): OverlayState =
        OverlayState(
            mode = UiMode.TARGETING,
            targetingCursor = cursor,
            modalFrames =
                listOf(
                    ModalFrame(
                        kind = ModalFrameKind.COMBAT_DECISION,
                        localState =
                            ModalFrameLocalState(
                                targetingCursor = cursor,
                                combatDecisionState = state,
                            ),
                    ),
                ),
        )

    private fun captureHash(render: () -> Unit): String {
        render()
        Gdx.gl.glFinish()
        val pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        return try {
            pixmapHash(pixmap)
        } finally {
            pixmap.dispose()
        }
    }

    private fun captureGameplayLogHash(
        snapshotSource: FoundationGameSession,
        render: () -> Unit,
    ): String {
        val snapshot = snapshotSource.renderSnapshot()
        val layout = TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, cellWidth = 32f, cellHeight = 32f)
        return captureHash(
            x = layout.logX.toInt(),
            y = layout.cardY.toInt(),
            width = layout.logWidth.toInt(),
            height = layout.cardHeight.toInt(),
            render = render,
        )
    }

    private fun captureHash(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        render: () -> Unit,
    ): String {
        render()
        Gdx.gl.glFinish()
        val pixmap = ScreenUtils.getFrameBufferPixmap(x, y, width, height)
        return try {
            pixmapHash(pixmap)
        } finally {
            pixmap.dispose()
        }
    }

    private fun pixmapHash(pixmap: Pixmap): String {
        val bytes = ByteArray(pixmap.width * pixmap.height * 4)
        val buffer = pixmap.pixels
        buffer.rewind()
        buffer.get(bytes)
        buffer.rewind()
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun triggerTemplarHealFeedback(session: FoundationGameSession) {
        val world = automationWorld(session)
        val playerHealth = requireNotNull(world.get<Health>(session.playerId))
        playerHealth.current = (playerHealth.max / 2).coerceAtLeast(1)
        val positiveEnergyPool =
            requireNotNull(requireNotNull(world.get<ResourcePools>(session.playerId)).pool(ResourceType.POSITIVE_ENERGY)) {
                "Expected templar positive energy pool for boss warning golden."
            }
        positiveEnergyPool.current = positiveEnergyPool.max
        val holyLightSlot = requireNotNull(session.talentSlots().firstOrNull { slot -> slot.talentId == "holy_light" }) {
            "Expected holy_light slot for boss warning golden."
        }.slot
        check(session.perform(PlayerCommand.UseTalent(slot = holyLightSlot))) { "Expected holy_light to consume a turn during boss warning golden capture." }
    }

    private fun installOptPr03InspectFixtures(session: FoundationGameSession): List<Int> =
        prependInventoryFixtureItems(
            session,
            listOf(
                buildAffixItem(baseId = "long_sword", affixId = "briarhook"),
                buildAffixItem(baseId = "bandit_trophy", affixId = "floodtouched"),
                buildSpecialItem(templateId = "unique.thornpath_crook"),
                buildSpecialItem(templateId = "artifact.heartroot_gambit"),
            ),
        )

    private fun installOptPr03GroundLootFixtures(session: FoundationGameSession) {
        val world = automationWorld(session)
        val metadata = session.renderSnapshot().metadata
        val dropPoint = Point(metadata.playerX, metadata.playerY)
        val factory = ItemFactory()
        val items =
            buildList {
                add(buildAffixItem(baseId = "long_sword", affixId = "briarhook"))
                add(buildSpecialItem(templateId = "unique.thornpath_crook"))
                repeat(8) {
                    add(buildAffixItem(baseId = "bandit_trophy", affixId = "floodtouched"))
                }
            }
        items.forEach { item -> factory.createGroundItem(world, item, dropPoint) }
    }

    private fun prependInventoryFixtureItems(
        session: FoundationGameSession,
        items: List<ItemInstance>,
    ): List<Int> {
        val world = automationWorld(session)
        val inventory = requireNotNull(world.get<com.ktome.core.item.Inventory>(session.playerId))
        val createdIds = items.map { item -> ItemFactory().createCarriedItem(world = world, item = item) }
        inventory.itemIds.addAll(0, createdIds)
        return createdIds.indices.toList()
    }

    private fun addInventoryFixtureItem(
        session: FoundationGameSession,
        item: ItemInstance,
    ): Int {
        val world = automationWorld(session)
        val inventory = requireNotNull(world.get<com.ktome.core.item.Inventory>(session.playerId))
        val index = inventory.itemIds.size
        inventory.itemIds += ItemFactory().createCarriedItem(world = world, item = item)
        return index
    }

    private fun buildAffixItem(
        baseId: String,
        affixId: String,
    ): ItemInstance {
        val base = requireNotNull(optPr03ItemBundle.baseItems.firstOrNull { item -> item.id == baseId }) { "Unknown base item '$baseId'." }
        val affix = requireNotNull(optPr03ItemBundle.affixes.firstOrNull { candidate -> candidate.id == affixId }) { "Unknown affix '$affixId'." }
        return ItemInstance(
            baseId = base.id,
            name = base.name,
            type = base.type,
            slot = base.slot,
            glyph = base.glyph,
            colorHex = base.colorHex,
            quality = RarityTier.MAGIC,
            affixes = listOf(affix),
            stats = base.baseStats + affix.statModifiers,
            effect = base.effect,
            resourceTypeId = base.resourceTypeId,
            magnitude = base.magnitude,
            passive = affix.passive ?: base.passive,
        )
    }

    private fun buildSpecialItem(
        templateId: String,
    ): ItemInstance {
        val template = requireNotNull(optPr03ItemBundle.specialTemplate(templateId)) { "Unknown special template '$templateId'." }
        val base = requireNotNull(optPr03ItemBundle.baseItems.firstOrNull { item -> item.id == template.itemId }) {
            "Unknown special item base '${template.itemId}'."
        }
        val material = template.fixedMaterialId?.let { materialId ->
            requireNotNull(optPr03ItemBundle.materials.firstOrNull { candidate -> candidate.id == materialId }) {
                "Unknown material '$materialId' for '$templateId'."
            }
        }
        val affixes =
            template.fixedAffixIds.map { affixId ->
                requireNotNull(optPr03ItemBundle.affixes.firstOrNull { candidate -> candidate.id == affixId }) {
                    "Unknown affix '$affixId' for '$templateId'."
                }
            }
        val stats =
            listOf(base.baseStats, material?.statModifiers ?: StatModifier.ZERO)
                .plus(affixes.map(AffixDef::statModifiers))
                .fold(StatModifier.ZERO) { acc, modifier -> acc + modifier }
        return ItemInstance(
            baseId = base.id,
            name = base.name,
            type = base.type,
            slot = base.slot,
            glyph = base.glyph,
            colorHex = base.colorHex,
            quality = RarityTier.RARE,
            materialId = material?.id,
            materialName = material?.name,
            affixes = affixes,
            stats = stats,
            effect = base.effect,
            resourceTypeId = base.resourceTypeId,
            magnitude = base.magnitude,
            passive = base.passive,
            specialTemplateId = template.id,
        )
    }

    private fun <T> withLwjgl3Context(
        width: Int,
        height: Int,
        block: () -> T,
    ): T {
        var result: Result<T>? = null
        val configuration =
            Lwjgl3ApplicationConfiguration().apply {
                setInitialVisible(false)
                disableAudio(true)
                setHdpiMode(HdpiMode.Pixels)
                setWindowedMode(width, height)
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
                    "Skipping LWJGL3 screenshot golden because the window backend is unavailable in this environment.",
                    exception,
                )
            }
            throw exception
        }

        return requireNotNull(result) {
            "LWJGL3 golden capture did not produce a result."
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

    private fun samplePackSelection(): ContentPackSelection {
        val repoRoot = Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()
        return ContentPackSelection.of(repoRoot.resolve("examples/content-packs/sample.flooded_relics"))
    }

    private fun claimSamplePackReward(session: FoundationGameSession): Int {
        clearMonsters(session)
        session.automationMovePlayerTo(requireNotNull(session.automationInteractablePoint("crystal_cache_chest")))
        check(session.perform(PlayerCommand.Interact)) { "Failed to claim underground_river primer interactable for sample-pack golden capture." }

        val searchPoint = requireNotNull(session.automationSearchPointForBinding(sampleSecretBindingId))
        val bindingSearchResult =
            session.automationSearchState()
                .firstOrNull { entry -> entry.bindingId == sampleSecretBindingId }
                ?.result
        if (bindingSearchResult != SearchActionResult.REVEALED) {
            session.automationMovePlayerTo(searchPoint)
            check(session.perform(PlayerCommand.Search)) { "Search command was rejected during sample-pack golden capture." }
        }
        check(
            session.automationSearchState().any { entry ->
                entry.bindingId == sampleSecretBindingId && entry.result == SearchActionResult.REVEALED
            },
        ) {
            "Expected sample-pack hidden entrance to be REVEALED. " +
                "tags=${session.automationDiscoveryTags()} searchState=${session.automationSearchState()}"
        }

        session.automationMovePlayerTo(requireNotNull(session.automationHiddenEntrancePointForBinding(sampleSecretBindingId)))
        check(session.perform(PlayerCommand.Interact)) { "Failed to enter sample-pack secret zone." }

        session.automationMovePlayerTo(requireNotNull(session.automationSecretRewardPointForBinding(sampleSecretBindingId)))
        check(session.perform(PlayerCommand.Interact)) { "Failed to claim sample-pack reward." }
        assertEquals("sample_flooded_relics.zone.flooded_reliquary.name", session.renderSnapshot().metadata.zoneNameKey)

        val rewardEntry =
            requireNotNull(
                session.renderSnapshot().uiState.inventory.firstOrNull { entry ->
                    entry.item.nameKey.startsWith("sample_flooded_relics.item.")
                },
            ) {
                "Expected sample-pack reward to be visible in the live inventory snapshot."
            }
        return rewardEntry.index
    }

    private fun clearMonsters(session: FoundationGameSession) {
        val world = automationWorld(session)
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
    }

    private fun propByType(
        session: FoundationGameSession,
        propTypeId: String,
    ): PropRenderSnapshot? = session.renderSnapshot().props.firstOrNull { prop -> prop.propTypeId == propTypeId }

}

private object NoOpInputSource : InputSource {
    override fun isKeyJustPressed(keycode: Int): Boolean = false

    override fun isKeyPressed(keycode: Int): Boolean = false
}

private class MutableOverlayCommandSource : CommandSource {
    var overlayState: OverlayState = OverlayState(mode = UiMode.MAP)

    override fun nextCommand(snapshot: RenderSnapshot) = null

    override fun overlayState(): OverlayState = overlayState

    override fun isMapMode(): Boolean = overlayState.mode == UiMode.MAP
}

private fun automationMovePlayerTo(
    session: FoundationGameSession,
    point: Point,
) {
    invokeSessionInternal(session, "automationMovePlayerTo", arrayOf(Point::class.java), point)
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
    world.get<com.ktome.core.talent.EffectTracker>(bossId)?.effects?.removeIf { effect ->
        effect.schemaId == "war_cry_empower"
    }
    world.remove<com.ktome.core.ai.PendingTelegraphState>(bossId)
    world.get<com.ktome.core.talent.CooldownState>(bossId)?.remainingByTalentId?.apply {
        this["war_cry"] = 0
        this["power_strike"] = 99
        this["charge"] = 99
    }
}

private fun automationForceDefeatPlayer(session: FoundationGameSession) {
    invokeSessionInternal(session, "automationForceDefeatPlayer")
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
