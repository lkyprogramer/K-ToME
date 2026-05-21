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
import com.ktome.client.assets.ShopOfferTagTokens
import com.ktome.client.automationWorld
import com.ktome.client.installReserveTalent
import com.ktome.client.input.CommandSource
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.client.input.InputSource
import com.ktome.client.input.OverlayState
import com.ktome.client.input.ShopFocus
import com.ktome.client.input.UiMode
import com.ktome.client.render.InventoryWorkbenchCellCoordinate
import com.ktome.client.render.InventoryWorkbenchGrid
import com.ktome.client.render.InventoryWorkbenchStacks
import com.ktome.client.render.TileLayoutMetrics
import com.ktome.client.render.TileRenderer
import com.ktome.client.render.layout.GameShellBounds
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
import kotlin.math.roundToInt
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

    private data class GoldenArtifactCrop(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    )

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
    private val darkUiuxPr02_1GoldenEvidenceLabels =
        listOf(
            "ui-demo-new-parity-1672x941",
            "ui-demo-new-parity-1280x800",
            "ui-demo-new-right-panel-grid",
            "ui-demo-new-bottom-deck-no-command-hints",
            "ui-demo-new-inventory-page-1",
            "ui-demo-new-inventory-page-2",
            "ui-demo-new-nav-rail-crop",
            "ui-demo-new-map-stage-crop",
        )
    private val darkUiuxPr03GoldenEvidenceLabels =
        listOf(
            "dark-uiux-pr03-equipment-slots",
            "dark-uiux-pr03-inventory-empty",
            "dark-uiux-pr03-inventory-stacked",
            "dark-uiux-pr03-inscription-shop",
            "dark-uiux-pr03-shop-full-slot-replace",
        )
    private val darkUiuxPr04GoldenEvidenceLabels =
        listOf(
            "dark-uiux-pr04-talent-assign-panel-start",
            "dark-uiux-pr04-active-slot-choice",
        )
    private val darkUiuxPr05GoldenEvidenceLabels =
        listOf(
            "dark-uiux-pr05-map-layer-stack",
            "dark-uiux-pr05-actor-boss-telegraph",
        )
    private val darkUiuxPr05_1GoldenEvidenceLabels =
        listOf(
            "dark-uiux-pr05-1-inventory-workbench",
            "dark-uiux-pr05-1-inventory-compare",
            "dark-uiux-pr05-1-inventory-pagination",
            "dark-uiux-pr05-1-inventory-min-window",
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
    fun `dark uiux pr02 1 golden evidence labels remain registered`() {
        assertEquals(
            listOf(
                "ui-demo-new-parity-1672x941",
                "ui-demo-new-parity-1280x800",
                "ui-demo-new-right-panel-grid",
                "ui-demo-new-bottom-deck-no-command-hints",
                "ui-demo-new-inventory-page-1",
                "ui-demo-new-inventory-page-2",
                "ui-demo-new-nav-rail-crop",
                "ui-demo-new-map-stage-crop",
            ),
            darkUiuxPr02_1GoldenEvidenceLabels,
        )
    }

    @Test
    fun `dark uiux pr03 golden evidence labels remain registered`() {
        assertEquals(
            listOf(
                "dark-uiux-pr03-equipment-slots",
                "dark-uiux-pr03-inventory-empty",
                "dark-uiux-pr03-inventory-stacked",
                "dark-uiux-pr03-inscription-shop",
                "dark-uiux-pr03-shop-full-slot-replace",
            ),
            darkUiuxPr03GoldenEvidenceLabels,
        )
    }

    @Test
    fun `dark uiux pr04 golden evidence labels remain registered`() {
        assertEquals(
            listOf(
                "dark-uiux-pr04-talent-assign-panel-start",
                "dark-uiux-pr04-active-slot-choice",
            ),
            darkUiuxPr04GoldenEvidenceLabels,
        )
    }

    @Test
    fun `dark uiux pr05 golden evidence labels remain registered`() {
        assertEquals(
            listOf(
                "dark-uiux-pr05-map-layer-stack",
                "dark-uiux-pr05-actor-boss-telegraph",
            ),
            darkUiuxPr05GoldenEvidenceLabels,
        )
    }

    @Test
    fun `dark uiux pr05 1 golden evidence labels remain registered`() {
        assertEquals(
            listOf(
                "dark-uiux-pr05-1-inventory-workbench",
                "dark-uiux-pr05-1-inventory-compare",
                "dark-uiux-pr05-1-inventory-pagination",
                "dark-uiux-pr05-1-inventory-min-window",
            ),
            darkUiuxPr05_1GoldenEvidenceLabels,
        )
    }

    @Test
    fun `dark uiux pr01 1 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr011GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr01-1-viewport-deadzone-still" to "d1663670cec01a697ecebb377ef07e7acf8bf036514e69b934a45ed6087f94ac",
                "dark-uiux-pr01-1-viewport-deadzone-scroll" to "933e669bd96773355bc58732b34fddf66d0ca1795cc35a413964608b352ab226",
                "dark-uiux-pr01-1-viewport-edge-clamp-top-left" to "94dcedf53c74ffa4028b65ca505f8983c66042ffa7f7a55eded60ba4b3fd1309",
                "dark-uiux-pr01-1-viewport-edge-clamp-bottom-right" to "0b52b046f25b79e884e784bb6123c6ae842655b22ec5f335c002b2d861445b0d",
                "dark-uiux-pr01-1-inspect-tooltip-layer" to "19aa22cc4736a22dab701493d3cde5b8789ca428ad8dacb8d810aff86c28a17b",
                "dark-uiux-pr01-1-item-modal-layer" to "6a87265f7edec921d6f5108005125e99acb3121e2bd5ab26ca6193a872806469",
                "dark-uiux-pr01-1-overlay-conflict-fixture" to "55a0d1e8377641571bbdd0404db7e25f76c4824adef55b1294b07f2fe6913251",
                "dark-uiux-pr01-1-targeting-cursor-viewport" to "c16f3286501b810ad8da352114fdeb96d16ad61a7b28a629a4be6690ce533098",
                "dark-uiux-pr01-1-focus-projection-resolution" to "19aa22cc4736a22dab701493d3cde5b8789ca428ad8dacb8d810aff86c28a17b",
                "dark-uiux-pr01-1-foundation-viewport-fixed-world" to "ac62f98c007d137bf46e82c8b6913a364dabeaac86a876f74e4bd7cb49339f45",
                "dark-uiux-pr01-1-map-sublayer-order" to "ac62f98c007d137bf46e82c8b6913a364dabeaac86a876f74e4bd7cb49339f45",
                "dark-uiux-pr01-1-modal-backdrop-stack" to "6a87265f7edec921d6f5108005125e99acb3121e2bd5ab26ca6193a872806469",
                "dark-uiux-pr01-1-combat-feedback-with-modal" to "25c6783829c2ab3a611f1458138723556fbd60bee757593d83c3457ee1cd1bcb",
                "dark-uiux-pr01-1-tooltip-flip-corners" to "30cb5aed6b06c865eb294838f3b86cddbd9cddc4c726a901f6f8cbb429ecfc72",
                "dark-uiux-pr01-1-item-tooltip-vs-modal-parity" to "65e435c12d77261665feb5480f5a429ef75245a1f569ae969f38ef6f276017fb",
                "dark-uiux-pr01-1-ascii-deletion-scan" to "ac62f98c007d137bf46e82c8b6913a364dabeaac86a876f74e4bd7cb49339f45",
                "dark-uiux-pr01-1-tome-layout-reference" to "ac62f98c007d137bf46e82c8b6913a364dabeaac86a876f74e4bd7cb49339f45",
                "dark-uiux-pr01-1-shell-min-window" to "089d73d73f3c6d78e4976db9b1c0327c9d9d29f1c616c7206db00c357b9b72bb",
            ),
            hashes,
        )
    }

    @Test
    fun `dark uiux pr02 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr02GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr02-round1-chrome" to "d6da14059a379e5cf463b1c7497f07f95e3719ab343426590df77827eea3ba5c",
                "dark-uiux-pr02-hud-icons-pilot" to "464a750cd40009bdf6c9c7f82c69e3ad62b207340e36208be8bc161084d3408a",
                "dark-uiux-pr02-standalone-screen-chrome" to "11cd74c8765a1f8017b72042381480379cbb89989e8b75351df5e0baf663ea8b",
            ),
            hashes,
        )
    }

    @Test
    fun `dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr02_1GoldenEvidence()

        assertEquals(
            mapOf(
                "ui-demo-new-parity-1672x941" to "340e61c8eb3a65e838d4b1a47b6d923aeca15d2608eee189381b9c1d346e2c07",
                "ui-demo-new-parity-1280x800" to "44af6cccabcb1d4349055afc1d4b4c648e1bdbf93248d3f6b9550c999e8c6076",
                "ui-demo-new-right-panel-grid" to "97694b6d053b199a4ac1cdaf9d2cffd1f2af230e872f31fb0a6dc596ec045b4d",
                "ui-demo-new-bottom-deck-no-command-hints" to "7d6e8294a344b3e3855e3fa74f3b77c96a8b5b1d55bbc249fd6807377053743a",
                "ui-demo-new-inventory-page-1" to "5e245777a23ed90626e4747cbbf04dd429a375b3da9a45c45265c4f54a5bd767",
                "ui-demo-new-inventory-page-2" to "f3acb92f2bd0dbfba6350265413b466f99de809f888777d0fb19939c2a7d39a7",
                "ui-demo-new-nav-rail-crop" to "c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03",
                "ui-demo-new-map-stage-crop" to "8ff35a68fa746ad2b38ad165110030e0ae694f8c30b589bbd3ac63454bb5022c",
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
                "d1663670cec01a697ecebb377ef07e7acf8bf036514e69b934a45ed6087f94ac",
                "6d66afe88b2c9b06719653563b231ad42d75672e6a44f4611731877d1188f1bb",
                "5398319ee53e99f7b99588ac4bffef0224545ebc2d5113381313bf6b4def5204",
                "6c93b038bac7e1f7dca170a885dec607219de06975c1431e6cc9d952e2f601a5",
                "f45cfa16a929281374a59033fcdfc24a135646f8bec5d111dc635177ce86d3ff",
                "3fa600120c5b401bf998db7d7e0e58a164e21ec9a533dee3625441f93b8c0ef4",
                "6e6ab16592b6b57e69d6c23fbe1939ef31b4913f0a808787827d507032e6bd18",
                "b6905d6031e25d76c62146ed7ea1f37ea07ae25af00d358eb6dc80ab5ca39791",
                "951f08437d9adcf0ee05844a8df7714c8bffa220d1db2866a83ecc801fd4ed8b",
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
                "9e2bb81775c9ca461baf31a139202df9ea9be34ead8ff11eeaa05c8159fab077",
                "addc0b2b927cb6c8c0b137925339aa537441ff801c467efc9cdbb725136d8b41",
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
                "2e747f096139305f222487d9e0b806e2110d3e6752eff5ef951366e8c242cd82",
                "f79b3dc841fbf6a0b57f2012d56a7390ce6a104a12f83a0080f97ea74d27afe4",
                "923995822732a418f418f90a3fbe4094a8c3ae09671f3707e7b57093db4b31d0",
                "462d1dba4258bf6bf71013eafb627c023d94a9c9414ff750e11b381a1c3cdce9",
                "ad84dda690753f48a60b40d712147bcee9056f1127c2434094616f267193aad8",
                "1ef7db225a116c4dbc018f9f9938496d5e21106ca96777e2efc859d9661d9a6b",
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
                "2f02361e4c8eabd645e04a2ced04afee0d724b3a4d633dad858026e77c94de07",
                "a6dfc8e7e4ac1eaf228a5e1b2ce7d66a4c59befb4b4100814ddee148edfef27b",
                "a876965c00bd78356ddd8044fa1540278b44211f2d96e0c7d6cfaffa3861fe5e",
                "eedfaa1e2e204bd1ad360a4dbab71a0867908f49f0a6c5ea31b0311c00630563",
                "0f44f22ecca8ea2d33c3baa3253455bdd59f92e1714503b11fcbdd9eb5896636",
                "47374c02e39b746d5e3a3f22f24418160416069fe1ba7fb143d4e262ac6aadfe",
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
                "8e8e0fe6b9de4c157952c3a194c1dbef52c27d38fdadbbb08d8e3a12b62b2a10",
                "b01b5396109cd8f53c9eca4ad98cb109191f4d27d4b5f2d938d5af03ed325202",
                "204c71f95a93ef96199ff2aa7033ba373e015826707c7a06d8d2a1a94069893b",
            ),
            english + chinese,
        )
    }

    @Test
    fun `sample pack golden hash remains stable for filesystem backed content`() {
        val hash = captureSamplePackRuntimeHash()

        assertEquals("78d6df625c0dfc024fe1510e4f4feb410968a808477fe9ad445b928fe81a151b", hash)
    }

    @Test
    fun `phase4 uiux pr03 item and ground loot golden hashes remain stable for english and chinese`() {
        val english = capturePhase4UiuxPr03Set(GameLocale.EN_US, "phase4-uiux-pr03-items-en")
        val chinese = capturePhase4UiuxPr03Set(GameLocale.ZH_CN, "phase4-uiux-pr03-items-zh")

        assertEquals(
            listOf(
                "63f73a07d92b527852539661b4e3388df64f9c618397aaa76c700ef295c45b17",
                "8272ba138ab8c14734775be5e9342c66f0a17d94968f0db51ea8cc4f8579c7da",
                "81cbf8f14df818dee9ee0ddcea6d791b9ecaa6465d6bb1e7d565faa2cf6a1a0f",
                "f90ce47716244d4b265f29c4ea031f6a3d958389550787f79fdb89dc8cd6602a",
                "9bfa1678c71f5ce1ed5d9219e745dea2a1a0839b4c869f6f8298370b02691aef",
                "13e45f8177d7155970d4829c78088137448228083b61dfd056579396cd46efe4",
                "a6ab3209738af9c2f629eadeb3b099edd4808c21a5b949464a6f8b17006b55da",
                "0912688a9dba9c784b6ef65870736433bf27a632b25c6198bff4367507958d58",
                "a3fe75d99ec402c78aa571d199c246d2ed817060e38b917b8a21bec54d9a2784",
                "d3fa2829f56ff061a0fdf9fcf9a3019886321a659d1ad9b5b5ce56067c3b6cf8",
            ),
            english + chinese,
        )
    }

    @Test
    fun `dark uiux pr03 equipment inventory and shop evidence hashes remain stable`() {
        val hashes = captureDarkUiuxPr03GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr03-equipment-slots" to "2c928166f3d87736bf8a7370fcd4e5f0e0ba442d9a41f3dcad2dbfde326430ca",
                "dark-uiux-pr03-inventory-empty" to "33c34425a3318460972d8829763c644ef9f22eca2fe845cb62e7a03ec4fba3ef",
                "dark-uiux-pr03-inventory-stacked" to "6bfc775be7f1700488ad2df23509bde43ce6c046cc3ac92da5ffea750df6a85e",
                "dark-uiux-pr03-inscription-shop" to "1d7e2bc54e1ff526a789bba13d3bf1efe9293e3bf1608a92735631136465471f",
                "dark-uiux-pr03-shop-full-slot-replace" to "faeb990d01e122b1dd6506ad0942d90ce6676893b59f390f7919527af4f8f3e9",
            ),
            hashes,
        )
    }

    @Test
    fun `dark uiux pr04 talent assign golden evidence writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr04GoldenEvidence()

        assertEquals(darkUiuxPr04GoldenEvidenceLabels, hashes.keys.toList())
        assertTrue(hashes.values.all { hash -> hash.matches(Regex("[a-f0-9]{64}")) })
        assertTrue(
            hashes["dark-uiux-pr04-talent-assign-panel-start"] != hashes["dark-uiux-pr04-active-slot-choice"],
            "PR04 active slot choice evidence must render the slot-choice modal, not duplicate the panel-start capture.",
        )
    }

    @Test
    fun `dark uiux pr05 map actor portrait golden evidence writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr05GoldenEvidence()

        assertEquals(darkUiuxPr05GoldenEvidenceLabels, hashes.keys.toList())
        assertTrue(hashes.values.all { hash -> hash.matches(Regex("[a-f0-9]{64}")) })
        assertTrue(
            hashes["dark-uiux-pr05-map-layer-stack"] != hashes["dark-uiux-pr05-actor-boss-telegraph"],
            "PR05 boss telegraph evidence must not duplicate the ordinary map layer stack capture.",
        )
    }

    @Test
    fun `dark uiux pr05 1 inventory workbench golden evidence writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr05_1GoldenEvidence()

        assertEquals(darkUiuxPr05_1GoldenEvidenceLabels, hashes.keys.toList())
        assertTrue(hashes.values.all { hash -> hash.matches(Regex("[a-f0-9]{64}")) })
        assertTrue(
            hashes["dark-uiux-pr05-1-inventory-workbench"] != hashes["dark-uiux-pr05-1-inventory-min-window"],
            "PR05-1 min-window evidence must be an independent compact viewport capture.",
        )
    }

    @Test
    fun `phase4 uiux pr05 telegraph and combat decision hashes remain stable`() {
        val labels =
            listOf(
                "phase4-uiux-pr05-telegraph-triple-surface",
                "phase4-uiux-pr05-combat-action",
                "phase4-uiux-pr05-combat-method",
                "phase4-uiux-pr05-combat-target",
                "phase4-uiux-pr05-combat-disabled-resource",
                "phase4-uiux-pr05-combat-illegal-target",
            )
        val hashes =
            labels.zip(capturePhase4UiuxPr05Set()).toMap()
        writePhase4UiuxPr05EvidenceIndex(hashes)

        assertEquals(
            mapOf(
                "phase4-uiux-pr05-telegraph-triple-surface" to "20697ade71cf36730cc55b8a3dc66fdca70b813116a9f3bbd6e37978831d480d",
                "phase4-uiux-pr05-combat-action" to "d7dd2091cf88a42b21084ac6133556319ac5e87c7fbbcbcb3582938162fa3c51",
                "phase4-uiux-pr05-combat-method" to "eb1356b564ea1e543ad4e5929a881ee6651e3ab1fa2ec5fd6a70313c954bceba",
                "phase4-uiux-pr05-combat-target" to "7547bbda9a24446e4665fcce5dd4f9fe4b41e3b548ebfca03d18a09e7a0b3e5d",
                "phase4-uiux-pr05-combat-disabled-resource" to "d7dd2091cf88a42b21084ac6133556319ac5e87c7fbbcbcb3582938162fa3c51",
                "phase4-uiux-pr05-combat-illegal-target" to "01eca8da44674318ebee3546be153c99502c60100aabf304e85bb42a825b1490",
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
                "phase4-v4-pr05-molten-glass-phase-override-warning" to "f72af717173251321e827dcc85bbec3b3352e6f0dbb8d692ffe9fb11734c3653",
                "phase4-v4-pr05-grey-crown-phase-override-warning" to "8abe9c0cf1ca8a104cf1f71d4a24fbe4d82d3ba081467f746fa5645eedf7742b",
                "phase4-v4-pr05-abyssal-eclipse-phase-override-warning" to "8a2698e90e710da7450b8af0ee4eec6ede4ae8b63d505f274d74377100e37308",
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

    private fun captureDarkUiuxPr02_1GoldenEvidence(): Map<String, String> {
        val hashes = linkedMapOf<String, String>()
        hashes +=
            captureDarkUiuxPr02_1GoldenEvidenceWindow(width = 1672, height = 941) { app, overlaySource ->
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                linkedMapOf(
                    "ui-demo-new-parity-1672x941" to
                        captureGoldenArtifact(
                            label = "ui-demo-new-parity-1672x941",
                            evidenceDir = darkUiuxPr02_1GoldenDir(),
                            flipY = true,
                        ) {
                            repeat(2) { app.render() }
                        },
                )
            }
        hashes +=
            captureDarkUiuxPr02_1GoldenEvidenceWindow(width = 1280, height = 800) { app, overlaySource ->
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for PR-02-2 golden evidence." }
                val layout = currentTileLayout(session)
                val rightPanelCrop = goldenCrop(layout.demoShell.rightPanel, layout)
                val bottomDeckCrop = goldenCrop(layout.demoShell.bottomDeck.bounds, layout)
                val navRailCrop = goldenCrop(layout.demoShell.navRail, layout)
                val mapStageCrop = goldenCrop(layout.demoShell.mapStage, layout)
                val windowHashes = linkedMapOf<String, String>()
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                windowHashes["ui-demo-new-parity-1280x800"] =
                    captureGoldenArtifact(
                        label = "ui-demo-new-parity-1280x800",
                        evidenceDir = darkUiuxPr02_1GoldenDir(),
                        flipY = true,
                    ) {
                        repeat(2) { app.render() }
                    }
                windowHashes["ui-demo-new-right-panel-grid"] =
                    captureGoldenArtifact(
                        label = "ui-demo-new-right-panel-grid",
                        evidenceDir = darkUiuxPr02_1GoldenDir(),
                        crop = rightPanelCrop,
                        flipY = true,
                    ) {
                        repeat(2) { app.render() }
                    }
                windowHashes["ui-demo-new-bottom-deck-no-command-hints"] =
                    captureGoldenArtifact(
                        label = "ui-demo-new-bottom-deck-no-command-hints",
                        evidenceDir = darkUiuxPr02_1GoldenDir(),
                        crop = bottomDeckCrop,
                        flipY = true,
                    ) {
                        repeat(2) { app.render() }
                    }
                windowHashes["ui-demo-new-nav-rail-crop"] =
                    captureGoldenArtifact(
                        label = "ui-demo-new-nav-rail-crop",
                        evidenceDir = darkUiuxPr02_1GoldenDir(),
                        crop = navRailCrop,
                        flipY = true,
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        repeat(2) { app.render() }
                    }
                windowHashes["ui-demo-new-map-stage-crop"] =
                    captureGoldenArtifact(
                        label = "ui-demo-new-map-stage-crop",
                        evidenceDir = darkUiuxPr02_1GoldenDir(),
                        crop = mapStageCrop,
                        flipY = true,
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        repeat(2) { app.render() }
                    }
                windowHashes
            }
        hashes +=
            captureDarkUiuxPr02_1GoldenEvidenceWindow(width = 1280, height = 800) { app, overlaySource ->
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for PR-02-2 pagination evidence." }
                installPr02_1PaginationFixtures(session)
                val layout = currentTileLayout(session)
                val rightPanelCrop = goldenCrop(layout.demoShell.rightPanel, layout)
                val pageHashes = linkedMapOf<String, String>()
                pageHashes["ui-demo-new-inventory-page-1"] =
                    captureGoldenArtifact(
                        label = "ui-demo-new-inventory-page-1",
                        evidenceDir = darkUiuxPr02_1GoldenDir(),
                        crop = rightPanelCrop,
                        flipY = true,
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.INVENTORY)
                        repeat(2) { app.render() }
                    }
                pageHashes["ui-demo-new-inventory-page-2"] =
                    captureGoldenArtifact(
                        label = "ui-demo-new-inventory-page-2",
                        evidenceDir = darkUiuxPr02_1GoldenDir(),
                        crop = rightPanelCrop,
                        flipY = true,
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 8)
                        repeat(2) { app.render() }
                    }
                pageHashes
            }
        writeDarkUiuxPr02_1EvidenceIndex(hashes)
        return hashes
    }

    private fun captureDarkUiuxPr02_1GoldenEvidenceWindow(
        width: Int,
        height: Int,
        capture: (GameApp, MutableOverlayCommandSource) -> Map<String, String>,
    ): Map<String, String> =
        withLwjgl3Context(width = width, height = height) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr02-1-golden")),
                    validationSaveManager = SaveManager(tempDir.resolve("dark-uiux-pr02-1-validation-golden-$width-$height")),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = GameLocale.ZH_CN,
                )

            try {
                app.create()
                val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr02-1-demo-shell-foundation"))
                app.startValidationSession(scenario.toSessionOptions(ContentPackSelection.EMPTY))
                requireNotNull(app.activeSessionOrNull()) { "Expected active validation session for PR-02-2 golden evidence." }
                capture(app, overlaySource)
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr03GoldenEvidence(): Map<String, String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr03-golden")),
                    validationSaveManager = SaveManager(tempDir.resolve("dark-uiux-pr03-validation-golden")),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = GameLocale.ZH_CN,
                )
            val hashes = linkedMapOf<String, String>()

            try {
                app.create()
                val equipmentScenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr03-equipment-inventory-items"))
                app.startValidationSession(equipmentScenario.toSessionOptions(ContentPackSelection.EMPTY))
                val equipmentSession = requireNotNull(app.activeSessionOrNull()) { "Expected active PR03 validation session." }
                val equipmentPanelCrop = goldenCrop(currentTileLayout(equipmentSession).demoShell.rightPanel, currentTileLayout(equipmentSession))
                hashes["dark-uiux-pr03-equipment-slots"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr03-equipment-slots",
                        evidenceDir = darkUiuxPr03GoldenDir(),
                        crop = equipmentPanelCrop,
                        flipY = true,
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        repeat(2) { app.render() }
                    }

                clearInventoryFixture(equipmentSession)
                hashes["dark-uiux-pr03-inventory-empty"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr03-inventory-empty",
                        evidenceDir = darkUiuxPr03GoldenDir(),
                        crop = equipmentPanelCrop,
                        flipY = true,
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.INVENTORY)
                        repeat(2) { app.render() }
                    }

                installPr03StackedInventoryFixtures(equipmentSession)
                hashes["dark-uiux-pr03-inventory-stacked"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr03-inventory-stacked",
                        evidenceDir = darkUiuxPr03GoldenDir(),
                        crop = equipmentPanelCrop,
                        flipY = true,
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 1)
                        repeat(2) { app.render() }
                    }

                val shopScenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr02"))
                app.startValidationSession(shopScenario.toSessionOptions())
                val shopSession = requireNotNull(app.activeSessionOrNull()) { "Expected active shop validation session." }
                check(
                    shopSession.perform(
                        PlayerCommand.Validation(
                            ValidationAction.Phase4V4ScenarioAction(
                                scenarioId = shopScenario.id,
                                actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                            ),
                        ),
                    ),
                ) {
                    "Failed to prepare PR02 shop scene for PR03 golden shop evidence."
                }
                requireNotNull(shopSession.renderSnapshot().uiState.activeShop) { "Expected active shop for PR03 golden shop evidence." }
                hashes["dark-uiux-pr03-inscription-shop"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr03-inscription-shop",
                        evidenceDir = darkUiuxPr03GoldenDir(),
                        flipY = true,
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.SHOP, shopFocus = ShopFocus.BUY)
                        repeat(2) { app.render() }
                    }
                check(
                    shopSession.perform(
                        PlayerCommand.Validation(
                            ValidationAction.Phase4V4ScenarioAction(
                                scenarioId = shopScenario.id,
                                actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                            ),
                        ),
                    ),
                ) {
                    "Failed to prepare PR02 replacement scene for PR03 golden replacement evidence."
                }
                val replacementShop = requireNotNull(shopSession.renderSnapshot().uiState.activeShop) {
                    "Expected active replacement shop for PR03 golden replacement evidence."
                }
                val replacementOffer =
                    requireNotNull(
                        replacementShop.offers.firstOrNull { offer ->
                            ShopOfferTagTokens.INSCRIPTION in offer.tagLabelKeys ||
                                "INSCRIPTION" in offer.tags ||
                                offer.labelKey.startsWith("inscription.")
                        },
                    ) {
                        "Expected inscription offer for PR03 golden replacement evidence; offers=${replacementShop.offers}."
                    }
                shopSession.perform(
                    PlayerCommand.BuyShopOffer(
                        index = replacementOffer.index,
                        offerFingerprint = replacementOffer.offerFingerprint,
                    ),
                )
                requireNotNull(shopSession.renderSnapshot().uiState.activeShop?.inscriptionReplacementPrompt) {
                    "Expected replacement prompt for PR03 golden replacement evidence."
                }
                hashes["dark-uiux-pr03-shop-full-slot-replace"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr03-shop-full-slot-replace",
                        evidenceDir = darkUiuxPr03GoldenDir(),
                        flipY = true,
                    ) {
                        overlaySource.overlayState =
                            OverlayState(
                                mode = UiMode.SHOP,
                                shopFocus = ShopFocus.BUY,
                                shopOfferSelection = 0,
                                inscriptionReplacementHotkeySelection = 5,
                            )
                        repeat(2) { app.render() }
                    }
                writeDarkUiuxPr03EvidenceIndex(hashes)
                hashes
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr04GoldenEvidence(): Map<String, String> =
        withLwjgl3Context(width = 1280, height = 840) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr04-golden")),
                    validationSaveManager = SaveManager(tempDir.resolve("dark-uiux-pr04-validation-golden")),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = GameLocale.ZH_CN,
                )
            val hashes = linkedMapOf<String, String>()

            try {
                app.create()
                val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr04-profession-tree-ui"))
                app.startValidationSession(scenario.toSessionOptions(ContentPackSelection.EMPTY))
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active PR04 validation session." }
                check(
                    session.perform(
                        PlayerCommand.Validation(
                            ValidationAction.Phase4V4ScenarioAction(
                                scenarioId = scenario.id,
                                actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                            ),
                        ),
                    ),
                ) {
                    "Failed to prepare PR04 reference talent-assign scene."
                }
                hashes["dark-uiux-pr04-talent-assign-panel-start"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr04-talent-assign-panel-start",
                        evidenceDir = darkUiuxPr04GoldenDir(),
                        flipY = true,
                    ) {
                        overlaySource.overlayState =
                            OverlayState(
                                mode = UiMode.TALENT_ASSIGN,
                                talentTreeSelection = 1,
                                modalFrames = listOf(ModalFrame(ModalFrameKind.TALENT_ASSIGN)),
                            )
                        repeat(2) { app.render() }
                    }
                check(
                    session.perform(
                        PlayerCommand.Validation(
                            ValidationAction.Phase4V4ScenarioAction(
                                scenarioId = scenario.id,
                                actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                            ),
                        ),
                    ),
                ) {
                    "Failed to prepare PR04 active-slot-choice scene."
                }
                requireNotNull(session.renderSnapshot().uiState.activeTalentSlotChoiceRequirement) {
                    "Expected upstream active talent slot choice requirement for PR04 golden evidence."
                }
                hashes["dark-uiux-pr04-active-slot-choice"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr04-active-slot-choice",
                        evidenceDir = darkUiuxPr04GoldenDir(),
                        flipY = true,
                    ) {
                        overlaySource.overlayState =
                            OverlayState(
                                mode = UiMode.TALENT_ASSIGN,
                                talentTreeSelection = 1,
                                modalFrames =
                                    listOf(
                                        ModalFrame(ModalFrameKind.TALENT_ASSIGN),
                                        ModalFrame(ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE),
                                    ),
                            )
                        repeat(2) { app.render() }
                    }
                writeDarkUiuxPr04EvidenceIndex(hashes)
                hashes
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr05GoldenEvidence(): Map<String, String> {
        val hashes = linkedMapOf<String, String>()
        hashes += captureDarkUiuxPr05MapLayerStack()
        hashes += captureDarkUiuxPr05ActorBossTelegraph()
        writeDarkUiuxPr05EvidenceIndex(hashes)
        return hashes
    }

    private fun captureDarkUiuxPr05_1GoldenEvidence(): Map<String, String> {
        val hashes = linkedMapOf<String, String>()
        hashes += captureDarkUiuxPr05_1WorkbenchEvidence()
        hashes += captureDarkUiuxPr05_1StandardEvidence()
        hashes += captureDarkUiuxPr05_1MinWindowEvidence()
        writeDarkUiuxPr05_1EvidenceIndex(hashes)
        return hashes
    }

    private fun captureDarkUiuxPr05_1WorkbenchEvidence(): Map<String, String> =
        withLwjgl3Context(width = 1672, height = 941) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr05-1-inventory-workbench")),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260521L,
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
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active PR05-1 inventory workbench session." }
                seedDarkUiuxPr05_1InventoryFixture(session)
                linkedMapOf(
                    "dark-uiux-pr05-1-inventory-workbench" to
                        captureGoldenArtifact(
                            label = "dark-uiux-pr05-1-inventory-workbench",
                            evidenceDir = darkUiuxPr05_1GoldenDir(),
                        ) {
                            overlaySource.overlayState =
                                OverlayState(
                                    mode = UiMode.INVENTORY,
                                    inventorySelection = 0,
                                    inventoryFocusedCell = InventoryWorkbenchCellCoordinate.ORIGIN,
                                    modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY)),
                                )
                            repeat(2) { app.render() }
                        },
                )
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr05_1StandardEvidence(): Map<String, String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr05-1-inventory-standard")),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260521L,
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
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active PR05-1 inventory workbench session." }
                val compareSelection = seedDarkUiuxPr05_1InventoryFixture(session)
                val hashes = linkedMapOf<String, String>()
                hashes["dark-uiux-pr05-1-inventory-compare"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr05-1-inventory-compare",
                        evidenceDir = darkUiuxPr05_1GoldenDir(),
                    ) {
                        overlaySource.overlayState =
                            OverlayState(
                                mode = UiMode.INVENTORY,
                                inventorySelection = compareSelection,
                                inventoryFocusedCell = InventoryWorkbenchCellCoordinate(column = 1, row = 0),
                                modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY)),
                            )
                        repeat(2) { app.render() }
                    }
                hashes["dark-uiux-pr05-1-inventory-pagination"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr05-1-inventory-pagination",
                        evidenceDir = darkUiuxPr05_1GoldenDir(),
                    ) {
                        overlaySource.overlayState =
                            OverlayState(
                                mode = UiMode.INVENTORY,
                                inventorySelection = 31,
                                inventoryPageIndex = 1,
                                inventoryFocusedCell = InventoryWorkbenchCellCoordinate(column = 0, row = 0),
                                modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY)),
                            )
                        repeat(2) { app.render() }
                    }
                hashes
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr05_1MinWindowEvidence(): Map<String, String> =
        withLwjgl3Context(width = 1024, height = 768) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr05-1-inventory-min-window")),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = 20260521L,
                            zoneId = "shattered_outpost",
                            playerProfessionId = "vanguard",
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
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active PR05-1 min-window session." }
                seedDarkUiuxPr05_1InventoryFixture(session, potionCount = 6, firstAffixBaseId = "hunter_bow")
                linkedMapOf(
                    "dark-uiux-pr05-1-inventory-min-window" to
                        captureGoldenArtifact(
                            label = "dark-uiux-pr05-1-inventory-min-window",
                            evidenceDir = darkUiuxPr05_1GoldenDir(),
                        ) {
                            overlaySource.overlayState =
                                OverlayState(
                                    mode = UiMode.INVENTORY,
                                    inventorySelection = 0,
                                    inventoryFocusedCell = InventoryWorkbenchCellCoordinate.ORIGIN,
                                    modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY)),
                                )
                            repeat(2) { app.render() }
                        },
                )
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr05MapLayerStack(): Map<String, String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr05-map-layer-stack"))
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr05-map-layer-stack")),
                    validationSaveManager = SaveManager(tempDir.resolve("dark-uiux-pr05-map-layer-stack-validation")),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = GameLocale.ZH_CN,
                )

            try {
                app.create()
                app.startValidationSession(scenario.toSessionOptions())
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active PR05 map layer stack session." }
                val propPoint =
                    session.automationInteractablePoint("supply_crate")
                        ?: requireNotNull(automationStairPoint(session, StairDirection.DOWN)) {
                            "Expected a prop or downstairs entry for PR05 map layer stack evidence."
                        }
                automationMovePlayerTo(session, findOpenAdjacentPoint(session, propPoint))
                installOptPr03GroundLootFixtures(session)
                invalidateGoldenFixtureSnapshot(session)
                val layout = currentTileLayout(session)
                val mapStageCrop = goldenCrop(layout.demoShell.mapStage, layout)
                linkedMapOf(
                    "dark-uiux-pr05-map-layer-stack" to
                        captureGoldenArtifact(
                            label = "dark-uiux-pr05-map-layer-stack",
                            evidenceDir = darkUiuxPr05GoldenDir(),
                            crop = mapStageCrop,
                            flipY = true,
                        ) {
                            overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                            repeat(2) { app.render() }
                        },
                )
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr05ActorBossTelegraph(): Map<String, String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("dark-uiux-pr05-actor-boss-telegraph"))
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr05-actor-boss-telegraph")),
                    validationSaveManager = SaveManager(tempDir.resolve("dark-uiux-pr05-actor-boss-telegraph-validation")),
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = GameLocale.ZH_CN,
                )

            try {
                app.create()
                app.startValidationSession(scenario.toSessionOptions())
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active PR05 boss telegraph session." }
                check(
                    session.perform(
                        PlayerCommand.Validation(
                            ValidationAction.Phase4V4ScenarioAction(
                                scenarioId = scenario.id,
                                actionId = ValidationScenarioActionId.PREPARE_PRIMARY_SCENE,
                            ),
                        ),
                    ),
                ) {
                    "Failed to prepare PR05 boss telegraph validation scene."
                }
                app.render()

                val bossId = requireNotNull(automationBossEntity(session)) { "Expected a live boss entity for PR05 boss telegraph evidence." }
                val bossPoint = requireNotNull(automationWorld(session).get<Position>(bossId)) {
                    "Expected boss position for PR05 boss telegraph evidence."
                }.toPoint()
                automationMovePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
                prepareBossTelegraphFixture(session, bossId)
                val snapshot = waitForBossTelegraph(session, app)
                assertTrue(snapshot.overlays.any { overlay -> overlay.id.startsWith("boss-warning:") })
                val layout = currentTileLayout(session)
                val mapStageCrop = goldenCrop(layout.demoShell.mapStage, layout)
                linkedMapOf(
                    "dark-uiux-pr05-actor-boss-telegraph" to
                        captureGoldenArtifact(
                            label = "dark-uiux-pr05-actor-boss-telegraph",
                            evidenceDir = darkUiuxPr05GoldenDir(),
                            crop = mapStageCrop,
                            flipY = true,
                        ) {
                            overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                            repeat(2) { app.render() }
                        },
                )
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
                    captureGoldenArtifact(
                        label = "phase4-uiux-pr05-telegraph-triple-surface",
                        evidenceDir = phase4UiuxPr05GoldenDir(),
                    ) {
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        repeat(2) { app.render() }
                    }
                hashes +=
                    captureGoldenArtifact(
                        label = "phase4-uiux-pr05-combat-action",
                        evidenceDir = phase4UiuxPr05GoldenDir(),
                    ) {
                        overlaySource.overlayState = combatDecisionOverlay(CombatDecisionFrame.initialState)
                        repeat(2) { app.render() }
                    }
                hashes +=
                    captureGoldenArtifact(
                        label = "phase4-uiux-pr05-combat-method",
                        evidenceDir = phase4UiuxPr05GoldenDir(),
                    ) {
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
                    captureGoldenArtifact(
                        label = "phase4-uiux-pr05-combat-target",
                        evidenceDir = phase4UiuxPr05GoldenDir(),
                    ) {
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
                    captureGoldenArtifact(
                        label = "phase4-uiux-pr05-combat-disabled-resource",
                        evidenceDir = phase4UiuxPr05GoldenDir(),
                    ) {
                        overlaySource.overlayState = combatDecisionOverlay(CombatDecisionFrame.initialState)
                        repeat(2) { app.render() }
                    }
                hashes +=
                    captureGoldenArtifact(
                        label = "phase4-uiux-pr05-combat-illegal-target",
                        evidenceDir = phase4UiuxPr05GoldenDir(),
                    ) {
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
        crop: GoldenArtifactCrop? = null,
        flipY: Boolean = false,
        render: () -> Unit,
    ): String {
        render()
        Gdx.gl.glFinish()
        val captured =
            if (crop == null) {
                ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
            } else {
                ScreenUtils.getFrameBufferPixmap(crop.x, crop.y, crop.width, crop.height)
            }
        val pixmap = if (flipY) flipPixmapY(captured) else captured
        return try {
            val hash = pixmapHash(pixmap)
            Files.createDirectories(evidenceDir)
            PixmapIO.writePNG(FileHandle(evidenceDir.resolve("$label.png").toFile()), pixmap)
            if (writeSideBySide && crop == null) {
                writeDarkUiuxPr011SideBySide(label, pixmap)
            }
            hash
        } finally {
            if (pixmap !== captured) {
                pixmap.dispose()
            }
            captured.dispose()
        }
    }

    private fun flipPixmapY(source: Pixmap): Pixmap {
        val flipped = Pixmap(source.width, source.height, source.format)
        try {
            for (y in 0 until source.height) {
                flipped.drawPixmap(source, 0, y, source.width, 1, 0, source.height - y - 1, source.width, 1)
            }
            return flipped
        } catch (error: Throwable) {
            flipped.dispose()
            throw error
        }
    }

    private fun writeDarkUiuxPr011SideBySide(
        label: String,
        captured: Pixmap,
    ) {
        val reference = Pixmap(FileHandle(repoRootPath().resolve("UI/UI-demo-new.png").toFile()))
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

    private fun writeDarkUiuxPr02_1EvidenceIndex(hashes: Map<String, String>) {
        val evidenceDir = darkUiuxPr02_1GoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact")
                hashes.forEach { (label, hash) ->
                    appendLine("$label\t$hash\tclient/build/reports/golden/dark-uiux-pr02-1/$label.png")
                }
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun writeDarkUiuxPr03EvidenceIndex(hashes: Map<String, String>) {
        val evidenceDir = darkUiuxPr03GoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact")
                hashes.forEach { (label, hash) ->
                    appendLine("$label\t$hash\tclient/build/reports/golden/dark-uiux-pr03/$label.png")
                }
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun writeDarkUiuxPr04EvidenceIndex(hashes: Map<String, String>) {
        val evidenceDir = darkUiuxPr04GoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact")
                hashes.forEach { (label, hash) ->
                    appendLine("$label\t$hash\tclient/build/reports/golden/dark-uiux-pr04/$label.png")
                }
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun writeDarkUiuxPr05EvidenceIndex(hashes: Map<String, String>) {
        val evidenceDir = darkUiuxPr05GoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact")
                hashes.forEach { (label, hash) ->
                    appendLine("$label\t$hash\tclient/build/reports/golden/dark-uiux-pr05/$label.png")
                }
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun writeDarkUiuxPr05_1EvidenceIndex(hashes: Map<String, String>) {
        val evidenceDir = darkUiuxPr05_1GoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact")
                hashes.forEach { (label, hash) ->
                    appendLine("$label\t$hash\tclient/build/reports/golden/dark-uiux-pr05-1/$label.png")
                }
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun writePhase4UiuxPr05EvidenceIndex(hashes: Map<String, String>) {
        val evidenceDir = phase4UiuxPr05GoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact")
                hashes.forEach { (label, hash) ->
                    appendLine("$label\t$hash\tclient/build/reports/golden/phase4-uiux-pr05/$label.png")
                }
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun darkUiuxPr011GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr01-1")

    private fun darkUiuxPr02GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr02")

    private fun darkUiuxPr02_1GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr02-1")

    private fun darkUiuxPr03GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr03")

    private fun darkUiuxPr04GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr04")

    private fun darkUiuxPr05GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr05")

    private fun seedDarkUiuxPr05_1InventoryFixture(
        session: FoundationGameSession,
        potionCount: Int = 8,
        firstAffixBaseId: String = "long_sword",
    ): Int {
        clearInventoryFixture(session)
        repeat(potionCount) {
            addInventoryFixtureItem(session, buildBaseItem("healing_potion"))
        }
        val firstAffixSelection = addInventoryFixtureItem(session, buildAffixItem(baseId = firstAffixBaseId, affixId = "briarhook"))
        addInventoryFixtureItem(session, buildBaseItem("chain_mail"))
        addInventoryFixtureItem(session, buildBaseItem("scroll_teleport"))
        addInventoryFixtureItem(session, buildBaseItem("mana_potion"))
        addInventoryFixtureItem(session, buildBaseItem("seal_reliquary"))
        addInventoryFixtureItem(session, buildBaseItem("artifact_deepcurrent_crown"))
        addInventoryFixtureItem(session, buildBaseItem("bandit_trophy"))
        repeat(18) { index ->
            addInventoryFixtureItem(
                session,
                buildAffixItem(
                    baseId = if (index % 2 == 0) "hunter_bow" else "emerald_charm",
                    affixId = if (index % 3 == 0) "briarhook" else "starforged",
                ),
            )
        }
        invalidateGoldenFixtureSnapshot(session)
        val workbenchGroups = InventoryWorkbenchStacks.groups(session.renderSnapshot().uiState.inventory)
        assertTrue(
            workbenchGroups.drop(InventoryWorkbenchGrid.PAGE_SIZE).isNotEmpty(),
            "PR05-1 pagination golden must capture a populated second workbench page.",
        )
        return firstAffixSelection
    }

    private fun darkUiuxPr05_1GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr05-1")

    private fun phase4UiuxPr05GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/phase4-uiux-pr05")

    private fun repoRootPath(): Path =
        Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()

    private fun currentTileLayout(session: FoundationGameSession): TileLayoutMetrics {
        val snapshot = session.renderSnapshot()
        return TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, cellWidth = 32f, cellHeight = 32f)
    }

    private fun goldenCrop(
        bounds: GameShellBounds,
        layout: TileLayoutMetrics,
    ): GoldenArtifactCrop {
        val scaleX = Gdx.graphics.backBufferWidth.toFloat() / layout.worldWidth
        val scaleY = Gdx.graphics.backBufferHeight.toFloat() / layout.worldHeight
        val x = (bounds.x * scaleX).roundToInt().coerceIn(0, Gdx.graphics.backBufferWidth - 1)
        val y = (bounds.y * scaleY).roundToInt().coerceIn(0, Gdx.graphics.backBufferHeight - 1)
        val width = (bounds.width * scaleX).roundToInt().coerceAtLeast(1).coerceAtMost(Gdx.graphics.backBufferWidth - x)
        val height = (bounds.height * scaleY).roundToInt().coerceAtLeast(1).coerceAtMost(Gdx.graphics.backBufferHeight - y)
        return GoldenArtifactCrop(x = x, y = y, width = width, height = height)
    }

    private fun installPr02_1PaginationFixtures(session: FoundationGameSession) {
        addInventoryFixtureItem(session, buildAffixItem(baseId = "bandit_trophy", affixId = "floodtouched"))
        addInventoryFixtureItem(session, buildAffixItem(baseId = "emerald_charm", affixId = "starforged"))
        invalidateGoldenFixtureSnapshot(session)
    }

    private fun clearInventoryFixture(session: FoundationGameSession) {
        val world = automationWorld(session)
        val inventory = requireNotNull(world.get<com.ktome.core.item.Inventory>(session.playerId))
        inventory.itemIds.clear()
        invalidateGoldenFixtureSnapshot(session)
    }

    private fun installPr03StackedInventoryFixtures(session: FoundationGameSession) {
        addInventoryFixtureItem(session, buildAffixItem(baseId = "long_sword", affixId = "briarhook"))
        addInventoryFixtureItem(session, buildAffixItem(baseId = "long_sword", affixId = "briarhook"))
        addInventoryFixtureItem(session, buildAffixItem(baseId = "emerald_charm", affixId = "starforged"))
        invalidateGoldenFixtureSnapshot(session)
    }

    private fun invalidateGoldenFixtureSnapshot(session: FoundationGameSession) {
        val method = FoundationGameSession::class.java.getDeclaredMethod("invalidateRenderSnapshot")
        method.isAccessible = true
        method.invoke(session)
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

    private fun buildBaseItem(baseId: String): ItemInstance {
        val base = requireNotNull(optPr03ItemBundle.baseItems.firstOrNull { item -> item.id == baseId }) { "Unknown base item '$baseId'." }
        return ItemInstance(
            baseId = base.id,
            name = base.name,
            type = base.type,
            slot = base.slot,
            glyph = base.glyph,
            colorHex = base.colorHex,
            quality = RarityTier.NORMAL,
            stats = base.baseStats,
            effect = base.effect,
            resourceTypeId = base.resourceTypeId,
            magnitude = base.magnitude,
            passive = base.passive,
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
                setWindowPosition(0, 0)
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
