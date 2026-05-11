package com.ktome.client.golden

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
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
import com.ktome.core.profile.ProfileManager
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
import com.ktome.game.validation.ValidationAction
import com.ktome.game.validation.ValidationScenarioActionId
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationScenarioRegistry
import java.nio.file.Files
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
    private val sampleSecretBindingId = SearchBindingId("sample.flooded_relics.search.flooded_reliquary")
    private val darkUiuxPr011GoldenEvidenceLabels =
        listOf(
            "dark-uiux-pr01-1-viewport-deadzone-still",
            "dark-uiux-pr01-1-viewport-deadzone-scroll",
            "dark-uiux-pr01-1-viewport-edge-clamp-top-left",
            "dark-uiux-pr01-1-viewport-edge-clamp-bottom-right",
            "dark-uiux-pr01-1-shell-min-window",
            "dark-uiux-pr01-1-inspect-tooltip-layer",
            "dark-uiux-pr01-1-item-modal-layer",
            "dark-uiux-pr01-1-overlay-conflict-fixture",
            "dark-uiux-pr01-1-targeting-cursor-viewport",
            "dark-uiux-pr01-1-focus-projection-resolution",
            "dark-uiux-pr01-1-foundation-viewport-fixed-world",
            "dark-uiux-pr01-1-map-sublayer-order",
            "dark-uiux-pr01-1-modal-backdrop-stack",
            "dark-uiux-pr01-1-combat-feedback-with-modal",
            "dark-uiux-pr01-1-tooltip-flip-corners",
            "dark-uiux-pr01-1-item-tooltip-vs-modal-parity",
            "dark-uiux-pr01-1-ascii-deletion-scan",
            "dark-uiux-pr01-1-tome-layout-reference",
        )
    private val darkUiuxPr02GoldenEvidenceLabels =
        listOf(
            "dark-uiux-pr02-round1-chrome",
            "dark-uiux-pr02-hud-icons-pilot",
            "dark-uiux-pr02-standalone-screen-chrome",
        )

    @Test
    fun `dark uiux pr01 1 golden evidence labels remain registered`() {
        assertEquals(
            listOf(
                "dark-uiux-pr01-1-viewport-deadzone-still",
                "dark-uiux-pr01-1-viewport-deadzone-scroll",
                "dark-uiux-pr01-1-viewport-edge-clamp-top-left",
                "dark-uiux-pr01-1-viewport-edge-clamp-bottom-right",
                "dark-uiux-pr01-1-shell-min-window",
                "dark-uiux-pr01-1-inspect-tooltip-layer",
                "dark-uiux-pr01-1-item-modal-layer",
                "dark-uiux-pr01-1-overlay-conflict-fixture",
                "dark-uiux-pr01-1-targeting-cursor-viewport",
                "dark-uiux-pr01-1-focus-projection-resolution",
                "dark-uiux-pr01-1-foundation-viewport-fixed-world",
                "dark-uiux-pr01-1-map-sublayer-order",
                "dark-uiux-pr01-1-modal-backdrop-stack",
                "dark-uiux-pr01-1-combat-feedback-with-modal",
                "dark-uiux-pr01-1-tooltip-flip-corners",
                "dark-uiux-pr01-1-item-tooltip-vs-modal-parity",
                "dark-uiux-pr01-1-ascii-deletion-scan",
                "dark-uiux-pr01-1-tome-layout-reference",
            ),
            darkUiuxPr011GoldenEvidenceLabels,
        )
    }

    @Test
    fun `dark uiux pr02 golden evidence labels remain registered`() {
        assertEquals(
            listOf(
                "dark-uiux-pr02-round1-chrome",
                "dark-uiux-pr02-hud-icons-pilot",
                "dark-uiux-pr02-standalone-screen-chrome",
            ),
            darkUiuxPr02GoldenEvidenceLabels,
        )
    }

    @Test
    fun `dark uiux pr01 1 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr011GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr01-1-viewport-deadzone-still" to "65fb4297085993539740db5169ef20f37c0511099a4901bb3b14c35582a40039",
                "dark-uiux-pr01-1-viewport-deadzone-scroll" to "37823e17667971a74bd5dd7258722ce716b3149f34c49260aacf18a3847262f3",
                "dark-uiux-pr01-1-viewport-edge-clamp-top-left" to "0cd272027c51c8676fb4136e3e21ca73a3eb165628e807c4b179d2788e013c56",
                "dark-uiux-pr01-1-viewport-edge-clamp-bottom-right" to "1bd958ed1ebce38c5a145a298a89cc146f2a839fc63f298f276480465d2e3e1b",
                "dark-uiux-pr01-1-inspect-tooltip-layer" to "f5e213557b0f57ce2db84e97528a8c62abb6e5ddb4ad6c01dd52ca04b1644d38",
                "dark-uiux-pr01-1-item-modal-layer" to "8e2b9d827f0009e11c8e691838205e3d8e4278dbad4f914f86489f4d4bfa523b",
                "dark-uiux-pr01-1-overlay-conflict-fixture" to "ebc4ae7d69e0d4f247512660d501405e9cfc6a18be26b618f79bad7900dde9c5",
                "dark-uiux-pr01-1-targeting-cursor-viewport" to "fc32105c7f2c5b4825a6fdf2af34fa717ae4c1a2ad9e58198f6c32398da9015f",
                "dark-uiux-pr01-1-focus-projection-resolution" to "782cb48a6600629c92be1939cc88dc3afcafe6194357c4ddc3d16eb61e603eb6",
                "dark-uiux-pr01-1-foundation-viewport-fixed-world" to "f84f5692e3627ed0d2292541c30442c52b4fc6c6c6cd4f33a26cf3b82a7bb563",
                "dark-uiux-pr01-1-map-sublayer-order" to "f84f5692e3627ed0d2292541c30442c52b4fc6c6c6cd4f33a26cf3b82a7bb563",
                "dark-uiux-pr01-1-modal-backdrop-stack" to "8e2b9d827f0009e11c8e691838205e3d8e4278dbad4f914f86489f4d4bfa523b",
                "dark-uiux-pr01-1-combat-feedback-with-modal" to "2ed96efdc7441d17e1a4066df7aea9148c055d8362b4ad613b928c1d548566e1",
                "dark-uiux-pr01-1-tooltip-flip-corners" to "1e9f50a19c7b7f322bb79d40104ec71636905970686919ae2b66cb4f36a06710",
                "dark-uiux-pr01-1-item-tooltip-vs-modal-parity" to "a4e020b33515e5833c24124605f55bcc4571ac368a34f5663c893d892b7e5d91",
                "dark-uiux-pr01-1-ascii-deletion-scan" to "f84f5692e3627ed0d2292541c30442c52b4fc6c6c6cd4f33a26cf3b82a7bb563",
                "dark-uiux-pr01-1-tome-layout-reference" to "f84f5692e3627ed0d2292541c30442c52b4fc6c6c6cd4f33a26cf3b82a7bb563",
                "dark-uiux-pr01-1-shell-min-window" to "dfcaa2c495a6bb16962bcdd4966d1b1a378a8e9a34d08aa6ed3c99a0ac77c9d1",
            ),
            hashes,
        )
    }

    @Test
    fun `dark uiux pr02 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr02GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr02-round1-chrome" to "34ee37a2d921677f925edfef667760581d7909089732a5046b85a5e88372ebbb",
                "dark-uiux-pr02-hud-icons-pilot" to "9dc97b55cfc4980e6fba1477b7c242ae8405f614adb56b997a011512a1df716f",
                "dark-uiux-pr02-standalone-screen-chrome" to "11cd74c8765a1f8017b72042381480379cbb89989e8b75351df5e0baf663ea8b",
            ),
            hashes,
        )
    }

    @Test
    fun `golden screenshot hashes remain stable for english and chinese formal screens`() {
        val english = captureGoldenSet(GameLocale.EN_US, "en-us")
        val chinese = captureGoldenSet(GameLocale.ZH_CN, "zh-cn")

        assertEquals(
            listOf(
                "1af8c316e586fd46bc9d46805e26913e7866b4ea5eb9b798b433a73a098a5aa2",
                "65fb4297085993539740db5169ef20f37c0511099a4901bb3b14c35582a40039",
                "e3ae85a8d9feb8f1b547d094dbcccef7d2df7e5b6192a58e1e92b0bb55b2a570",
                "4c5672f726dcd3f313570892be75c055cd21b4d931610adefb195a18d7cf1789",
                "09792e4798b3690758caa9250ba27f957dc48d0f451c40efb2c273410e623264",
                "66297e3642fa593664795894ff48e72d74e86c21ee1bd0e3434a1654f0de7dc2",
                "5d4b8ae7f09b933deebdb0c1c9580c36041af84f4ed44c1f9091ed46377cf68e",
                "b5ee53173ec6df3af0256497352d7bc873e3e49167cafa1e3b25128d803b1a9d",
                "0ae5ccdafa0bc96efd7e1ec47e94bfbcbb12b00303f9515397250dc0e6fac30d",
                "36a0185eb83845309c8c34ab95ad236d3e42742020c1dcc7465867aa3e80012a",
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
                "b5c79a284caf7f77f8b0fc2858045a2101b64ff01dc53f90375fe545086cd21c",
                "ed1808127d75e7a165b725b960089107536a745534e9653cc7eb0febe5c06f02",
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
                "7aac49506b9d6437a7b21ca120b689a40fdaa63eed37081ac5ec266b4623535f",
                "9c0653a99300d9ec1861e4d20f3df0dc848ddaec81442e28a983dc2d4b94719b",
                "24a60ba66a5b8e04648f6bdaccdf406f6b538f43d667dcc2299a30feffbb2727",
                "7162654b512285ba053367b437fe80105ad35b076d617ebfbc8056dc3ae1c568",
                "bfb983a6e765563dbbc1dba52c46e34e9cb78f91b0fef92324cd8977850a6665",
                "0b9fb2264c3a08b74c99a2d2d7870840ce0e117fa3e239e8f27af462e04a55fe",
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
                "936f3f83ebd6bdb913ebbfa5d437c1ae5a24adc6a42c7316e6d64b612807a0d2",
                "453f3c9bbcf5d5330794374fbf6f42c859fca5c2ace5c7ef1d415e3be04ae570",
                "e24ff6ff32c20115b1b309bceac8e95aea81266015aae7f2068d7a339326a80d",
                "3620981207434dc720a8b14f9dcdbcd4bcb1ac5569bd567d0bb028f645febc7f",
                "8ab20af8f078188fa05b2c0f62d3e049154a7e5d8b33e5d7a7549f3fd7178172",
                "ed007513bb6fae11c2076b74c8a18a328552b7861fd799e5d0d0911b1ac12593",
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
                "d4a3377e11cbbf220d0f90a4d8fd394a8345db2b0053dd7daf1d862edd0b8ab9",
                "d2c4fcc0f14d7b8eeb5976af7e5899122d41dfce32e8e2d83f2e0248b60ea55e",
                "b01b5396109cd8f53c9eca4ad98cb109191f4d27d4b5f2d938d5af03ed325202",
                "fc4cf8eedfcd8099b4b9d7a6765e05dd700f38fc2baa4281f771f9773589d6c7",
            ),
            english + chinese,
        )
    }

    @Test
    fun `sample pack golden hash remains stable for filesystem backed content`() {
        val hash = captureSamplePackRuntimeHash()

        assertEquals("f6190eb4f33841765696afbfbcd817a170aaabce8c1731407c7b93a85966f8db", hash)
    }

    @Test
    fun `phase4 uiux pr03 item and ground loot golden hashes remain stable for english and chinese`() {
        val english = capturePhase4UiuxPr03Set(GameLocale.EN_US, "phase4-uiux-pr03-items-en")
        val chinese = capturePhase4UiuxPr03Set(GameLocale.ZH_CN, "phase4-uiux-pr03-items-zh")

        assertEquals(
            listOf(
                "5d459baa95ae0bdea46f3a034e6963c07263e1eabaadeab15886e6f265a0af18",
                "0fd3b5fd21b2d05b6a570c4f3754ed04bbaa62e00d00bd7fa7f4908fe43e6e29",
                "17b19227113463c94083322a1e335c7b139e6e00c0f9bafeae8ed829d5ce86c1",
                "22d7062cb4fd1966c288d5fdf41c1cfae5d598fdef19d023d24002abffc34cbd",
                "028b79c554af7c169b5af4eec9b181365d65337db6e72413baf71a5ccc2c9741",
                "29c2f46fd2918a0664b6163a610ce07750befd5e70e05708a2c303f32cd1e61d",
                "8680e40151ec71e8ae44f35eed2d88813ac91251cb35dd4bac3ad273e1614cc8",
                "7bf249237ac6984b6be36cfae22f9d9be6b80c5b9a452381f7c63fee1eb34b86",
                "37503976c2589636684f09387707bb9d9a7e2ed5a511321db24ecb43809edfb4",
                "1ff703876e5d5abe22d281af9cad7ec31cf34dcf11496898f67f48f44391a3bd",
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
                "phase4-uiux-pr05-telegraph-triple-surface" to "3ff4074dfdca85bfcc75422dba960bcec780d0b0950a07316b55a74a23fd7392",
                "phase4-uiux-pr05-combat-action" to "cf4f88bad443a310164e5496e42732e30a641a0c5fd6f3d220e0ff25c231d75b",
                "phase4-uiux-pr05-combat-method" to "3cbbcaf70db48238b3a6caae42708884c98b23cea1d7b9a18c67650e19dcb71a",
                "phase4-uiux-pr05-combat-target" to "253e1089a041331a800ced8118da046d933cdeb2e4b3e8e1c039b50f0dd42009",
                "phase4-uiux-pr05-combat-disabled-resource" to "cf4f88bad443a310164e5496e42732e30a641a0c5fd6f3d220e0ff25c231d75b",
                "phase4-uiux-pr05-combat-illegal-target" to "620fbb773d3f60c8ce024a998d57f210df369d013438c50d189fd9639a122c2f",
            ),
            hashes,
        )
    }

    @Test
    fun `phase4 v4 pr05 boss variant phase warning hashes remain stable`() {
        val hashes =
            listOf(
                "phase4-v4-pr05-molten-glass-phase-override-warning",
                "phase4-v4-pr05-grey-crown-phase-override-warning",
                "phase4-v4-pr05-abyssal-eclipse-phase-override-warning",
            ).zip(capturePhase4V4Pr05BossVariantWarningSet()).toMap()

        assertEquals(
            mapOf(
                "phase4-v4-pr05-molten-glass-phase-override-warning" to "b826e8d58d21de20f91e82bdf1000c33bd8421d0797d30d2741f301c9ae48fcc",
                "phase4-v4-pr05-grey-crown-phase-override-warning" to "12efe117206485bcd68f158bbc62dd2bcb1ed3fa7b7cf1ecbe81894ce528e6fe",
                "phase4-v4-pr05-abyssal-eclipse-phase-override-warning" to "e9ced9c39d1f68c1ebd89e16cfc99f47e0510b719f97dbe6e249f3c93de94e59",
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

    private fun captureDarkUiuxPr011GoldenEvidence(): Map<String, String> {
        val hashes = linkedMapOf<String, String>()
        hashes += captureDarkUiuxPr011StandardEvidence()
        hashes += captureDarkUiuxPr011MinWindowEvidence()
        writeDarkUiuxPr011EvidenceIndex(hashes)
        return hashes
    }

    private fun captureDarkUiuxPr02GoldenEvidence(): Map<String, String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr02-golden")),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260413L,
                            zoneId = "shattered_outpost",
                            playerProfessionId = "vanguard",
                        ),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = GameLocale.EN_US,
                )
            val hashes = linkedMapOf<String, String>()

            try {
                app.create()
                hashes["dark-uiux-pr02-standalone-screen-chrome"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr02-standalone-screen-chrome",
                        evidenceDir = darkUiuxPr02GoldenDir(),
                    ) {
                        repeat(2) { app.render() }
                    }
                app.startNewGame()
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                hashes["dark-uiux-pr02-round1-chrome"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr02-round1-chrome",
                        evidenceDir = darkUiuxPr02GoldenDir(),
                    ) {
                        repeat(2) { app.render() }
                    }
                hashes["dark-uiux-pr02-hud-icons-pilot"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr02-hud-icons-pilot",
                        evidenceDir = darkUiuxPr02GoldenDir(),
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.INVENTORY)
                        repeat(2) { app.render() }
                    }
                writeDarkUiuxPr02EvidenceIndex(hashes)
                hashes
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr011StandardEvidence(): Map<String, String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr01-1-golden")),
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
                    initialLocale = GameLocale.EN_US,
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for PR-01-1 golden evidence." }
                val initial = session.renderSnapshot().metadata
                val player = Point(initial.playerX, initial.playerY)
                val focusTile = Point((player.x + 1).coerceAtMost(initial.width - 1), player.y)
                val bottomRight = Point(initial.width - 1, initial.height - 1)
                val itemSelection = addInventoryFixtureItem(session, buildAffixItem(baseId = "long_sword", affixId = "briarhook"))
                val hashes = linkedMapOf<String, String>()

                fun capture(
                    label: String,
                    writeSideBySide: Boolean = false,
                    render: () -> Unit,
                ) {
                    hashes[label] = captureGoldenArtifact(label = label, writeSideBySide = writeSideBySide, render = render)
                }

                capture("dark-uiux-pr01-1-viewport-deadzone-still") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                    repeat(2) { app.render() }
                }
                automationMovePlayerTo(session, Point((player.x + 8).coerceAtMost(initial.width - 1), player.y))
                capture("dark-uiux-pr01-1-viewport-deadzone-scroll") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                    repeat(2) { app.render() }
                }
                automationMovePlayerTo(session, Point.ZERO)
                capture("dark-uiux-pr01-1-viewport-edge-clamp-top-left") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                    repeat(2) { app.render() }
                }
                automationMovePlayerTo(session, bottomRight)
                capture("dark-uiux-pr01-1-viewport-edge-clamp-bottom-right") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                    repeat(2) { app.render() }
                }
                automationMovePlayerTo(session, player)
                capture("dark-uiux-pr01-1-inspect-tooltip-layer") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = focusTile)
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-item-modal-layer") {
                    overlaySource.overlayState =
                        OverlayState(
                            mode = UiMode.INVENTORY,
                            inventorySelection = itemSelection,
                            modalFrames = listOf(ModalFrame(ModalFrameKind.ITEM_DETAIL)),
                        )
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-overlay-conflict-fixture") {
                    overlaySource.overlayState =
                        OverlayState(
                            mode = UiMode.INSPECT,
                            inspectCursor = focusTile,
                            modalFrames = listOf(ModalFrame(ModalFrameKind.ITEM_DETAIL)),
                        )
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-targeting-cursor-viewport") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.TARGETING, targetingCursor = focusTile)
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-focus-projection-resolution") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = focusTile, explainPaneOpen = true)
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-foundation-viewport-fixed-world") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-map-sublayer-order") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-modal-backdrop-stack") {
                    overlaySource.overlayState =
                        OverlayState(
                            mode = UiMode.INVENTORY,
                            inventorySelection = itemSelection,
                            modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY), ModalFrame(ModalFrameKind.ITEM_DETAIL)),
                        )
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-combat-feedback-with-modal") {
                    overlaySource.overlayState = combatDecisionOverlay(CombatDecisionFrame.initialState, focusTile)
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-tooltip-flip-corners") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = bottomRight)
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-item-tooltip-vs-modal-parity") {
                    overlaySource.overlayState =
                        OverlayState(
                            mode = UiMode.INVENTORY,
                            inventorySelection = itemSelection,
                            modalFrames = listOf(ModalFrame(ModalFrameKind.ITEM_DETAIL), ModalFrame(ModalFrameKind.ITEM_COMPARE)),
                        )
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-ascii-deletion-scan") {
                    overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                    repeat(2) { app.render() }
                }
                capture("dark-uiux-pr01-1-tome-layout-reference", writeSideBySide = true) {
                    overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                    repeat(2) { app.render() }
                }
                hashes
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr011MinWindowEvidence(): Map<String, String> =
        withLwjgl3Context(width = 1024, height = 768) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr01-1-min-window-golden")),
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
                    initialLocale = GameLocale.EN_US,
                )

            try {
                app.create()
                app.startNewGame()
                val hash =
                    captureGoldenArtifact(label = "dark-uiux-pr01-1-shell-min-window") {
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        repeat(2) { app.render() }
                    }
                mapOf("dark-uiux-pr01-1-shell-min-window" to hash)
            } finally {
                app.dispose()
            }
        }

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
                    validationSaveManager = SaveManager(tempDir.resolve("$saveFolderName-validation-save")),
                    defaultConfig = defaultConfig,
                    profileManager = ProfileManager(tempDir.resolve("$saveFolderName-profile")),
                    validationProfileManager = ProfileManager(tempDir.resolve("$saveFolderName-validation-profile")),
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

    private fun capturePhase4V4Pr05BossVariantWarningSet(): List<String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr05"))
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("phase4-v4-pr05-boss-variant-warning/save")),
                    validationSaveManager = SaveManager(tempDir.resolve("phase4-v4-pr05-boss-variant-warning/validation-save")),
                    profileManager = com.ktome.core.profile.ProfileManager(tempDir.resolve("phase4-v4-pr05-boss-variant-warning/profile")),
                    validationProfileManager = com.ktome.core.profile.ProfileManager(tempDir.resolve("phase4-v4-pr05-boss-variant-warning/validation-profile")),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = scenario.runtime.locale,
                )

            try {
                app.create()
                app.startValidationSession(scenario.toSessionOptions())
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active PR-05 validation session for boss variant warning capture." }
                listOf(
                    capturePhase4V4Pr05BossVariantWarningHash(
                        app = app,
                        session = session,
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                        expectedOverlaySourceAbilityId = "molten_glass_phase_override_warning",
                        expectedLogKey = "boss.variant.molten_glass.phase_override.entered",
                    ),
                    capturePhase4V4Pr05BossVariantWarningHash(
                        app = app,
                        session = session,
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                        expectedOverlaySourceAbilityId = "grey_crown_phase_override_warning",
                        expectedLogKey = "boss.variant.grey_crown.phase_override.entered",
                    ),
                    capturePhase4V4Pr05BossVariantWarningHash(
                        app = app,
                        session = session,
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                        expectedOverlaySourceAbilityId = "abyssal_eclipse_phase_override_warning",
                        expectedLogKey = "boss.variant.abyssal_eclipse.phase_override.entered",
                    ),
                )
            } finally {
                app.dispose()
            }
        }

    private fun capturePhase4V4Pr05BossVariantWarningHash(
        app: GameApp,
        session: FoundationGameSession,
        scenarioId: ValidationScenarioId,
        actionId: ValidationScenarioActionId,
        expectedOverlaySourceAbilityId: String,
        expectedLogKey: String,
    ): String {
        check(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenarioId,
                        actionId = actionId,
                    ),
                ),
            ),
        ) {
            "Failed to prepare PR-05 boss variant validation scene '$expectedOverlaySourceAbilityId'."
        }
        val snapshot = session.renderSnapshot()
        check(snapshot.overlays.any { overlay -> overlay.sourceAbilityId == expectedOverlaySourceAbilityId }) {
            "Expected overlay '$expectedOverlaySourceAbilityId'; overlays=${snapshot.overlays.map { overlay -> overlay.sourceAbilityId }}."
        }
        check(snapshot.logEvents.any { event -> event.message.key == expectedLogKey }) {
            "Expected log '$expectedLogKey'; logs=${snapshot.logEvents.map { event -> event.message.key }}."
        }
        return captureHash {
            repeat(2) {
                app.render()
            }
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

    private fun captureGoldenArtifact(
        label: String,
        evidenceDir: Path = darkUiuxPr011GoldenDir(),
        writeSideBySide: Boolean = false,
        render: () -> Unit,
    ): String {
        render()
        Gdx.gl.glFinish()
        val pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        return try {
            val hash = pixmapHash(pixmap)
            Files.createDirectories(evidenceDir)
            PixmapIO.writePNG(FileHandle(evidenceDir.resolve("$label.png").toFile()), pixmap)
            if (writeSideBySide) {
                writeDarkUiuxPr011SideBySide(label, pixmap)
            }
            hash
        } finally {
            pixmap.dispose()
        }
    }

    private fun writeDarkUiuxPr011SideBySide(
        label: String,
        captured: Pixmap,
    ) {
        val reference = Pixmap(FileHandle(repoRootPath().resolve("UI/UI-demo.png").toFile()))
        val combined = Pixmap(2560, 800, Pixmap.Format.RGBA8888)
        try {
            combined.setColor(Color.BLACK)
            combined.fill()
            combined.drawPixmap(reference, 0, 0, reference.width, reference.height, 0, 0, 1280, 800)
            combined.drawPixmap(captured, 0, 0, captured.width, captured.height, 1280, 0, 1280, 800)
            PixmapIO.writePNG(FileHandle(darkUiuxPr011GoldenDir().resolve("$label-side-by-side.png").toFile()), combined)
        } finally {
            reference.dispose()
            combined.dispose()
        }
    }

    private fun writeDarkUiuxPr011EvidenceIndex(hashes: Map<String, String>) {
        val evidenceDir = darkUiuxPr011GoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact")
                hashes.forEach { (label, hash) ->
                    appendLine("$label\t$hash\tclient/build/reports/golden/dark-uiux-pr01-1/$label.png")
                }
                appendLine(
                    "dark-uiux-pr01-1-tome-layout-reference-side-by-side\tN/A\t" +
                        "client/build/reports/golden/dark-uiux-pr01-1/dark-uiux-pr01-1-tome-layout-reference-side-by-side.png",
                )
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun writeDarkUiuxPr02EvidenceIndex(hashes: Map<String, String>) {
        val evidenceDir = darkUiuxPr02GoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact")
                hashes.forEach { (label, hash) ->
                    appendLine("$label\t$hash\tclient/build/reports/golden/dark-uiux-pr02/$label.png")
                }
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun darkUiuxPr011GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr01-1")

    private fun darkUiuxPr02GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr02")

    private fun repoRootPath(): Path =
        Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()

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
