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
                "dark-uiux-pr01-1-viewport-deadzone-still" to "467827562a0d3aafda229d853d0df9f28a3ff9914a37efc75a8dc70f80cd456a",
                "dark-uiux-pr01-1-viewport-deadzone-scroll" to "b91540f498ed10d3de26e4352ebc83e5f154cc8f886208f1a427b8fb21c42cc5",
                "dark-uiux-pr01-1-viewport-edge-clamp-top-left" to "01e4fb39f53b4c081e7bbcb64802668423fbfe75823b291eed2ce3c1336b9c33",
                "dark-uiux-pr01-1-viewport-edge-clamp-bottom-right" to "67aedb2d4c38293091ebbc1c242f279dc8cb330301fb478b937153a7434a8606",
                "dark-uiux-pr01-1-inspect-tooltip-layer" to "6e6747aa98ab2429df2edf61936dba7dd45a046440579860eeee3f3848ea9233",
                "dark-uiux-pr01-1-item-modal-layer" to "8c9d0c4b5b74ce83d7f82f7d34830669fbda99f94a25f26850590c7264ad1e30",
                "dark-uiux-pr01-1-overlay-conflict-fixture" to "dfb9853b82c31901928e4677e6f5021c1ba1af6849196adfbc92a85eb2734663",
                "dark-uiux-pr01-1-targeting-cursor-viewport" to "d3d2295910efa8f38ab4cac6c0b85eadb8acba8786c4edffc33bb29075fb848f",
                "dark-uiux-pr01-1-focus-projection-resolution" to "6e6747aa98ab2429df2edf61936dba7dd45a046440579860eeee3f3848ea9233",
                "dark-uiux-pr01-1-foundation-viewport-fixed-world" to "46ba50d1743e61fe426f2a9db07b5ee37d60571a44dea7b8c1c60390a1b887bd",
                "dark-uiux-pr01-1-map-sublayer-order" to "46ba50d1743e61fe426f2a9db07b5ee37d60571a44dea7b8c1c60390a1b887bd",
                "dark-uiux-pr01-1-modal-backdrop-stack" to "8c9d0c4b5b74ce83d7f82f7d34830669fbda99f94a25f26850590c7264ad1e30",
                "dark-uiux-pr01-1-combat-feedback-with-modal" to "69202c82a3e0dee4b4ff42131f5f8690fe5c2a22dafbab8f5aba03416ad889f9",
                "dark-uiux-pr01-1-tooltip-flip-corners" to "dae5b65c02a988f9ff1c44a753d22b077d7d42cad57ada9d89732316b20ef9ff",
                "dark-uiux-pr01-1-item-tooltip-vs-modal-parity" to "8ce89272e7f9f49a65b8be5eff1e0a98e779f6f08042f4d701345b201555eb3e",
                "dark-uiux-pr01-1-ascii-deletion-scan" to "46ba50d1743e61fe426f2a9db07b5ee37d60571a44dea7b8c1c60390a1b887bd",
                "dark-uiux-pr01-1-tome-layout-reference" to "46ba50d1743e61fe426f2a9db07b5ee37d60571a44dea7b8c1c60390a1b887bd",
                "dark-uiux-pr01-1-shell-min-window" to "020b63c680bb1ae954a2311a6ad8d9a4f3aa2ed281a14143fdd01145aebf7b91",
            ),
            hashes,
        )
    }

    @Test
    fun `dark uiux pr02 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr02GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr02-round1-chrome" to "b060821dfd2a02c0c08e1e07f5dae095f3f5279de514f9e453a8e7076c5b8792",
                "dark-uiux-pr02-hud-icons-pilot" to "a5c573f01a46f4ef0851ee9c017b3252d8a94213b651607dd85efdb60c86d383",
                "dark-uiux-pr02-standalone-screen-chrome" to "9e674c394244f5b7d11a0df1e08bb60c8c9712b696bdef2a195e137b8bc39646",
            ),
            hashes,
        )
    }

    @Test
    fun `dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr02_1GoldenEvidence()

        assertEquals(
            mapOf(
                "ui-demo-new-parity-1672x941" to "f6f94cbb3a90df90e309d6e8cbefecf02f0d43c990e323ff5a1537188911aa78",
                "ui-demo-new-parity-1280x800" to "4eab125f30a3ad2200d34c67ae17e1e7525bb5bf603e06702551363743dda532",
                "ui-demo-new-right-panel-grid" to "c8bcf38d64bfeb2648b5d26535dea2f7ee4220e024860846fb0159d3f544b5d7",
                "ui-demo-new-bottom-deck-no-command-hints" to "fbb282fdd9e32821e3bd7b4d08e9df76b70f32d59e8c5bd625171ed72d646b4f",
                "ui-demo-new-inventory-page-1" to "e98b0311720183d954faa2f60731c332a5750bb10f1ffc358d9411c6b17ede8b",
                "ui-demo-new-inventory-page-2" to "5acc29cdcb60cba7bd6225a13618a58c70b91c9117e36121b0cc431008515a9c",
                "ui-demo-new-nav-rail-crop" to "c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6",
                "ui-demo-new-map-stage-crop" to "070f834b41a5db14c103a46d23d47da2662afc33400c0fda75c761ef5a9fa84d",
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
                "1ba55ad83fa2cce9f127d1745b9cc79d3bf67e42fb8f3824f9f902392cacd61f",
                "467827562a0d3aafda229d853d0df9f28a3ff9914a37efc75a8dc70f80cd456a",
                "9e0801dd67fe5009d8c049a27ae872958e0953f09e4f6da8d11742ec12d3cace",
                "e3101a84c100a5ef9a74b56dc3307d6d98b99219e3b58ef3e7e4d948535ac430",
                "d341d681d709b20d9993d4d52501e0b57b410e485542ffa6cb11c4e3f5916a9c",
                "97642f3d02e22551633362b8377553df60166f1b06d308be01f3fbb7de37b47e",
                "20d5ff90e08b727b2e762731bacce8b612a1672b624e3712eb6d02f94404b80b",
                "7cf14f2724e9e291d212f618fb2ca01ab4e9b56da3eefff7950e682070ca5053",
                "6b4e608cc377aaeff005a07dd740c04ca6fdb6efa96d2e7368a5e0794d694d1f",
                "4ab357f9879121686398eced95f19fa6f808d5d09f433b0c8515fa489b167776",
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
                "585340b2ca2bf9578e92cd6f7ca9a510d278d70fa9a05ffaafd8ee4f8327cbcf",
                "8fedfb6c12a58fcf5cbdfd0989216de3832b8c5c70260692cdc6f07453580131",
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
                "bc017e7681f51a163050027ea5a80b8ee23d2008a6a806a7c37e5e1be9a85228",
                "a0a90d26b32de723da7118bc7857c34a820b0dff516d3fe499bd35b624c0fa68",
                "d63b5bf3de56190beedb382b389d0b0899ffacef83aa44e63354d0e2399eaac7",
                "97bca3a80a3b7a25719b45b104bcf0b64c04cb84ad8c9356ab7c639548628008",
                "1c7a1607147767f8dd9533cf417a8716629ac8606a694c743c2f7bbf2ed375fb",
                "90be55da8432ac0e434ee7d4c1ea90ae874b0b5b6528fe93be7f56981027f862",
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
                "8b986cbd5a2f042fd26cebb413c94a96e8f91a68dbe86e39601d6f1f2d3b5f4a",
                "4c740d201a32a13832b9dc30ff473f826179ffc48ab27a26af524895b92af65d",
                "48ce3cf2d04370ed6099757d14a6c41f69fb00d5ac3c6ff98e42df908e409e68",
                "af7b2031b199951b6c0f61151160f32a6e192384f2a307e50f18ba2f50773df4",
                "fe2f15d27353d4310e58f82b4b74d90759461966541beddb24ad439fc054a1dd",
                "2c17eb63800543f4299d16a2fded5a30712f4928be645febd7c0b120d9212a2b",
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
                "db5304ab71ac33580aadee58d2515825bac27e96d3536a15fc39c1b014531e53",
                "a9f0d945e69c3c0944e90254555e84f0c37ebfb403b0494eead8620e697dcfd4",
                "466dbd732272ce2798d017c41d20b8748943ac6e69f2ad6ff711b8c5c387b983",
                "5336bab71f17b759d701bc052ceb64ca7cd3dc740e58039076e688384c1f692c",
            ),
            english + chinese,
        )
    }

    @Test
    fun `sample pack golden hash remains stable for filesystem backed content`() {
        val hash = captureSamplePackRuntimeHash()

        assertEquals("73151a4fabc21435659bf6b13b5a7d851eee99159dafec5623779d40b7910782", hash)
    }

    @Test
    fun `phase4 uiux pr03 item and ground loot golden hashes remain stable for english and chinese`() {
        val english = capturePhase4UiuxPr03Set(GameLocale.EN_US, "phase4-uiux-pr03-items-en")
        val chinese = capturePhase4UiuxPr03Set(GameLocale.ZH_CN, "phase4-uiux-pr03-items-zh")

        assertEquals(
            listOf(
                "3149ebb0a557675b658281cfa0069b8b071bacea4371665c3f02a6d6a28feb68",
                "6e20f4bab45e66acb8d8747ca8174a7bd221b9928ba868261b700292dfcd6d58",
                "aec2524b6c08de1fd33d0105d481d2d9e78b20722f4267729bc0c8f3c9cd56df",
                "930ecd075e30124f56d80600e3e976bcae4fb10311f7c63a84b5c0a587bd5d55",
                "23015521eb72dd5af559155d02cf03d24c06a8cc59c98d5cd8cb63f85320c364",
                "5ccd125518ee815c307c336e51ef5e1681951f4eb5d863242f8a8d311575ce51",
                "dc4bb0537f1d0978f867e88eb971fca7f56314ad0569b816ca32b1b91742d297",
                "3bcf2fa32d30f48d0664ac56c62915f331d4bf0a51084bc0d152be6938c6c180",
                "70c2386a1e8782f7220c97c70e79656fb4c13c2bf74fe2a7c152aa3f593eb727",
                "43cb3cae266285b58b26bd7aff1ab567c7c9505eb7f37a41bdd72a020c7b87b7",
            ),
            english + chinese,
        )
    }

    @Test
    fun `dark uiux pr03 equipment inventory and shop evidence hashes remain stable`() {
        val hashes = captureDarkUiuxPr03GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr03-equipment-slots" to "8b5de22035be2042b4a95f92062e7a07f85fcc01fdec43a54b7b82f21675ec7b",
                "dark-uiux-pr03-inventory-empty" to "8595d2fa14c996388f19381d5f36c02b63059f22191112c583a63f7815c7b6f0",
                "dark-uiux-pr03-inventory-stacked" to "452de482ad35234d81e093082ebeb59ea3f344c36716b686c9ab11fa8bfd4d62",
                "dark-uiux-pr03-inscription-shop" to "99dc4e1ebb8b79991946af6ff2daa0f9298dd9524a5e575f3b2343c55a54a0da",
                "dark-uiux-pr03-shop-full-slot-replace" to "f4ab6e4aa0584babeb8898c0c629a46ab4f668447690e11396d14980cdc47ae8",
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
                "phase4-uiux-pr05-telegraph-triple-surface" to "fa1b440201e1b416606819562dcfdc00fbbadf0dc6568565efb1fd75e1b784f5",
                "phase4-uiux-pr05-combat-action" to "218cad9865955a2f55b582080c5e0b54c7628eb48d861969f2387cf9c517c0ca",
                "phase4-uiux-pr05-combat-method" to "351a4b9deabd9753f719d2fc1a0c9b509cd3a5d10f37c4d55905ac99573a6d79",
                "phase4-uiux-pr05-combat-target" to "0e5fcaa4586563049b0dac29b2893fbf2dfe93b853c752fd305c232f10fbe83e",
                "phase4-uiux-pr05-combat-disabled-resource" to "218cad9865955a2f55b582080c5e0b54c7628eb48d861969f2387cf9c517c0ca",
                "phase4-uiux-pr05-combat-illegal-target" to "c6f6fa4ef149d1dbcd88e47a05635d616365b3e7c02cec7ef003a5ad63cf7a96",
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
                "phase4-v4-pr05-molten-glass-phase-override-warning" to "2cd8bf6fe75535d6c009c2622a1ed2ab1a4eb0103b37c6e0e72cbfd2fe81c95f",
                "phase4-v4-pr05-grey-crown-phase-override-warning" to "2cd8bf6fe75535d6c009c2622a1ed2ab1a4eb0103b37c6e0e72cbfd2fe81c95f",
                "phase4-v4-pr05-abyssal-eclipse-phase-override-warning" to "2cd8bf6fe75535d6c009c2622a1ed2ab1a4eb0103b37c6e0e72cbfd2fe81c95f",
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
        val captured = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        val pixmap = flipPixmapY(captured)
        return try {
            pixmapHash(pixmap)
        } finally {
            pixmap.dispose()
            captured.dispose()
        }
    }

    private fun captureGoldenArtifact(
        label: String,
        evidenceDir: Path = darkUiuxPr011GoldenDir(),
        writeSideBySide: Boolean = false,
        crop: GoldenArtifactCrop? = null,
        flipY: Boolean = true,
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
        val captured = ScreenUtils.getFrameBufferPixmap(x, y, width, height)
        val pixmap = flipPixmapY(captured)
        return try {
            pixmapHash(pixmap)
        } finally {
            pixmap.dispose()
            captured.dispose()
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
