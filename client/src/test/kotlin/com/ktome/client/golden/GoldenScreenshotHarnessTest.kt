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
import com.ktome.client.assets.DarkUiMapVisualKeys
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
import javax.imageio.ImageIO
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

    private data class Pr08GeneralizationProbe(
        val labelSuffix: String,
        val seed: Long,
        val floor: Int,
        val professionId: String,
        val probeIntent: String,
        val zoneId: String = "shattered_outpost",
    ) {
        val label: String = "dark-uiux-pr08-generalization-$labelSuffix"
    }

    private data class Pr08VisibleTopology(
        val tilesetKey: String,
        val visibleMaterialCells: Int,
        val boundsWidth: Int,
        val boundsHeight: Int,
        val fillRatioPermille: Int,
        val playerOffsetX: Int,
        val playerOffsetY: Int,
    ) {
        val signature: String =
            "$tilesetKey:${boundsWidth}x$boundsHeight:$fillRatioPermille:${playerOffsetX}_$playerOffsetY:$visibleMaterialCells"
    }

    private data class Pr08GeneralizationProbeEvidence(
        val probe: Pr08GeneralizationProbe,
        val hash: String,
        val topology: Pr08VisibleTopology,
    )

    private data class Pr08TopologyMaskProbe(
        val label: String,
        val columns: Int,
        val rows: Int,
        val visibleCells: Set<Pair<Int, Int>>,
        val cropBands: List<Pr08TopologyMaskBand>,
    )

    private data class Pr08TopologyMaskBand(
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
    private val darkUiuxPr08DirectorEvidenceLabels =
        listOf(
            "dark-uiux-pr08-director-parity-1672x941",
            "dark-uiux-pr08-director-map-stage-crop",
            "dark-uiux-pr08-director-right-panel-crop",
            "dark-uiux-pr08-director-bottom-deck-crop",
            "dark-uiux-pr08-director-forest-map-stage-crop",
            "dark-uiux-pr08-director-mine-map-stage-crop",
            "dark-uiux-pr08-director-shadow-depths-map-stage-crop",
            "dark-uiux-pr08-director-telegraph-combat-crop",
        )
    private val darkUiuxPr08GeneralizationProbes =
        listOf(
            Pr08GeneralizationProbe(
                labelSuffix = "ruins-proof-baseline",
                seed = 2026051102L,
                floor = 1,
                professionId = "vanguard",
                probeIntent = "fixed proof crop baseline",
            ),
            Pr08GeneralizationProbe(
                labelSuffix = "ruins-seed-2026060801",
                seed = 2026060801L,
                floor = 1,
                professionId = "vanguard",
                probeIntent = "compact or near-start room variance",
            ),
            Pr08GeneralizationProbe(
                labelSuffix = "ruins-seed-2026060802",
                seed = 2026060802L,
                floor = 1,
                professionId = "rogue",
                probeIntent = "wide room or lateral bounds variance",
            ),
            Pr08GeneralizationProbe(
                labelSuffix = "ruins-seed-2026060803",
                seed = 2026060803L,
                floor = 1,
                professionId = "arcanist",
                probeIntent = "tall room or vertical bounds variance",
            ),
            Pr08GeneralizationProbe(
                labelSuffix = "ruins-seed-2026060804-floor2",
                seed = 2026060804L,
                floor = 2,
                professionId = "vanguard",
                probeIntent = "corridor-heavy visible-region variance",
            ),
            Pr08GeneralizationProbe(
                labelSuffix = "ruins-seed-2026060805-floor2",
                seed = 2026060805L,
                floor = 2,
                professionId = "templar",
                probeIntent = "offset or L-like visible-region variance",
            ),
            Pr08GeneralizationProbe(
                labelSuffix = "ruins-seed-2026060806",
                seed = 2026060806L,
                floor = 1,
                professionId = "berserker",
                probeIntent = "mixed room and corridor visibility variance",
            ),
            Pr08GeneralizationProbe(
                labelSuffix = "ruins-seed-2026060807-floor2",
                seed = 2026060807L,
                floor = 2,
                professionId = "rogue",
                probeIntent = "alternate procedural room-shape variance",
            ),
        )
    private val darkUiuxPr08GeneralizationEvidenceLabels =
        darkUiuxPr08GeneralizationProbes.map { probe -> probe.label }
    private val darkUiuxPr08TopologyMaskEvidenceLabels =
        listOf("dark-uiux-pr08-topology-source-mask-board")

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
    fun `dark uiux pr08 director evidence labels remain registered`() {
        assertEquals(
            listOf(
                "dark-uiux-pr08-director-parity-1672x941",
                "dark-uiux-pr08-director-map-stage-crop",
                "dark-uiux-pr08-director-right-panel-crop",
                "dark-uiux-pr08-director-bottom-deck-crop",
                "dark-uiux-pr08-director-forest-map-stage-crop",
                "dark-uiux-pr08-director-mine-map-stage-crop",
                "dark-uiux-pr08-director-shadow-depths-map-stage-crop",
                "dark-uiux-pr08-director-telegraph-combat-crop",
            ),
            darkUiuxPr08DirectorEvidenceLabels,
        )
    }

    @Test
    fun `dark uiux pr08 generalization evidence labels remain registered`() {
        assertEquals(
            listOf(
                "dark-uiux-pr08-generalization-ruins-proof-baseline",
                "dark-uiux-pr08-generalization-ruins-seed-2026060801",
                "dark-uiux-pr08-generalization-ruins-seed-2026060802",
                "dark-uiux-pr08-generalization-ruins-seed-2026060803",
                "dark-uiux-pr08-generalization-ruins-seed-2026060804-floor2",
                "dark-uiux-pr08-generalization-ruins-seed-2026060805-floor2",
                "dark-uiux-pr08-generalization-ruins-seed-2026060806",
                "dark-uiux-pr08-generalization-ruins-seed-2026060807-floor2",
            ),
            darkUiuxPr08GeneralizationEvidenceLabels,
        )
    }

    @Test
    fun `dark uiux pr01 1 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr011GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr01-1-viewport-deadzone-still" to "e7e918eefd1a86a3d910af6f6655dda8c44da945c834580e806ec806e92ab774",
                "dark-uiux-pr01-1-viewport-deadzone-scroll" to "df7ca6f611649d5835b8285a14c7f73a9bc6d8481b6629e41788c6c717433904",
                "dark-uiux-pr01-1-viewport-edge-clamp-top-left" to "7571f5f4af22f87d4dbdb769ab61266191d7ad4767f724bb42fa77f091ac4781",
                "dark-uiux-pr01-1-viewport-edge-clamp-bottom-right" to "545301677c1cc3ce14701a59aec8db45c9e76aab227f5bab78bf6cdf0481d270",
                "dark-uiux-pr01-1-inspect-tooltip-layer" to "14d5d015bd847dd9469da0e9a557549b0b4bab8ddf5f9346d5229e92fed52d34",
                "dark-uiux-pr01-1-item-modal-layer" to "b6f5ae4dcf588a6fd107af4481366184721f306670b93d2934c66ce94a468776",
                "dark-uiux-pr01-1-overlay-conflict-fixture" to "ac8b60437db1bbedb18c89b65ad66f0e268d02f21e597e78b660b96628168dd4",
                "dark-uiux-pr01-1-targeting-cursor-viewport" to "1f0c8117277e427e9eb1c3db17e3ad99c3df200c70fbe33e2c2e4b375861c449",
                "dark-uiux-pr01-1-focus-projection-resolution" to "14d5d015bd847dd9469da0e9a557549b0b4bab8ddf5f9346d5229e92fed52d34",
                "dark-uiux-pr01-1-foundation-viewport-fixed-world" to "866f4531e36a653d6b94f25be3bbc462a3017e4a89142593022f6eed26471f7c",
                "dark-uiux-pr01-1-map-sublayer-order" to "866f4531e36a653d6b94f25be3bbc462a3017e4a89142593022f6eed26471f7c",
                "dark-uiux-pr01-1-modal-backdrop-stack" to "b6f5ae4dcf588a6fd107af4481366184721f306670b93d2934c66ce94a468776",
                "dark-uiux-pr01-1-combat-feedback-with-modal" to "4882eedb3c6a8b5e423cfe334a91fdad9bc00948596099c463912a36a81b221e",
                "dark-uiux-pr01-1-tooltip-flip-corners" to "6bc4b3e11fcc8901c14edf6e846571502d2a0060f3358c196fbc94288b365957",
                "dark-uiux-pr01-1-item-tooltip-vs-modal-parity" to "1aa25035ae6fabcf5f6117f609cb76dbcaf7319afef2ca56dd498640b8632e8a",
                "dark-uiux-pr01-1-ascii-deletion-scan" to "866f4531e36a653d6b94f25be3bbc462a3017e4a89142593022f6eed26471f7c",
                "dark-uiux-pr01-1-tome-layout-reference" to "866f4531e36a653d6b94f25be3bbc462a3017e4a89142593022f6eed26471f7c",
                "dark-uiux-pr01-1-shell-min-window" to "fe30057f59ee4e2e803532a04437cb91f5327bfbb1e957d00cbd59039e47aea7",
            ),
            hashes,
        )
    }

    @Test
    fun `dark uiux pr02 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr02GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr02-round1-chrome" to "dcf3509f880ffe282a7fba2796679409d9f1b33720cb681592aa2164f80b8685",
                "dark-uiux-pr02-hud-icons-pilot" to "65e3376a2a8989d54332d055a1ebc631353df9a9a3399e4a2866c04b1a6e0af9",
                "dark-uiux-pr02-standalone-screen-chrome" to "d02fa07bfd23b82a96751efe4817acf0be83726cf780f90feb6e16f1fb7aac61",
            ),
            hashes,
        )
    }

    @Test
    fun `dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr02_1GoldenEvidence()

        assertEquals(
            mapOf(
                "ui-demo-new-parity-1672x941" to "e7f65d0f2f205e238909ec1cd9f54387a9eaf365912e1da5710c9df9e72ed936",
                "ui-demo-new-parity-1280x800" to "eadc93bf5970b986ccef19a5617ce8ecaad997f3ad67d109ff5211c529687d7e",
                "ui-demo-new-right-panel-grid" to "201645bf13f9824fd95289063b92ea8adc722f9d0f344aca75902a2e99c26c47",
                "ui-demo-new-bottom-deck-no-command-hints" to "a416b96d21059212a6a7682f8163f0f065dcac3b545bad28b064814f8120cdfd",
                "ui-demo-new-inventory-page-1" to "8a8f9a409dce5316abdacef86a4107fdadfd84e75e53d6458fba4bb45fd2fb5d",
                "ui-demo-new-inventory-page-2" to "9f52610441461f31ea0f2cd1ed4d0dbca9d98b1725f7d448b09dc4dbde51dbb5",
                "ui-demo-new-nav-rail-crop" to "0b80fba591e20e244dc1b71b6f241729f78d57fa5c89f93150f114536cb5f692",
                "ui-demo-new-map-stage-crop" to "84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676",
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
                "8773ba4fcca15646e2b94e176ecd637b201bdea5840903a23f30ba29d7cce50b",
                "e7e918eefd1a86a3d910af6f6655dda8c44da945c834580e806ec806e92ab774",
                "da5721244d2c6f3f7dc98dce6b39ddd89f2a1e95db02d5e6c20ef6a4075105d3",
                "35d02e91ed0f4b6a7cacfdca68ef48e22c0dc47ae6917ae52ea8d77800c52c4d",
                "719af6ef63e778f6a1a3105287744c6bdf9ebea90472e9733e23253d5070adb4",
                "713fbe61999468209a3d5b580453b23fd53da64268e53419865ac66301309b7d",
                "9a25ce2e1379af0c7a04aff198956e442b89c935fcda58c8f2568d6e98a1ceae",
                "2a10e71edf7d5c7f064b152ceae9291205af6bb494e347f7a60d280f0afa95b5",
                "dea8a479868492412c837c5944566e2a2bed02cebf56578760b2aee7e0300cd3",
                "226dc7154b2f195dc31a60fa4cabc18d9e140992c0bd197015519065597417bc",
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
                "8c0c18648b9c0f9e57410f34f30adefd31ea2172df33b03d59e0fe040af216cb",
                "7d7abd405e3d72084e95d4f53a2f1d4263f979d16df6a5dce275b865b1d4a245",
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
                "8bf483010e61007075df987e47ba8b82d5d140ed446c8304f5922c88a1c7be7f",
                "cf40c08fc27b1549157dbb6ae526220c09c7701aa632472433c3b63582a13bf7",
                "35a71263b41831823ef51f23acafa5d2c500579da491799ac7991b2de244512c",
                "d0ea9d36d87f72033871432a6015c3763a5adc28ae7201ca330ab530e9ea4f96",
                "5d6d5e131fea66d8a19164a632e09b08657f397720f252fbcb708d38b972c6ec",
                "c9cf9c4be49c350f53389f358f8b3eecc727952f88c14d5be9bb1d428ee8649d",
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
                "6b91261c69535ba560ee5ed6cf14c4f1530c6b5fbe3ad526edf181529bc5c064",
                "f69b78d4b935fdb5ef3125deac04ed788d7d4369943c3179612af69f224a1176",
                "101f46f7fb912c785b6e9d6213d3934277dfb96a049f11dd5dba20f3fe27d9df",
                "6eb1d9239f26987875640d45e77c51fb48d70be8ef9100b7455f3eff409cc6e6",
                "4e9fdd73ed9069059c49594475f2fb1487d9724aabc7a145d2a212d79fc70ab2",
                "c5724182d749385e538c105509138fa040a525872243707087b6b965fd13515e",
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
                "81afad0f947ecc43d9eadca920f5d9ba5d43d3f580eb3c195d3fa27347001245",
                "e82bd3d82c11c75a6a47f6de0835a209d90d16795df600069f1cbdd7c9772204",
                "d07e2acb4739abd75852cadcf46f707e82051e29386acbe9e398070fa869dd55",
                "c35feddae590b0e40559dd3453de6d2c5fbdcacc3541408b6d20ac6306ba033f",
            ),
            english + chinese,
        )
    }

    @Test
    fun `sample pack golden hash remains stable for filesystem backed content`() {
        val hash = captureSamplePackRuntimeHash()

        assertEquals("ead38be9f88cabf8f18134a0dad418ca423cf13ede51ded4c25f0e751cb17a66", hash)
    }

    @Test
    fun `phase4 uiux pr03 item and ground loot golden hashes remain stable for english and chinese`() {
        val english = capturePhase4UiuxPr03Set(GameLocale.EN_US, "phase4-uiux-pr03-items-en")
        val chinese = capturePhase4UiuxPr03Set(GameLocale.ZH_CN, "phase4-uiux-pr03-items-zh")

        assertEquals(
            listOf(
                "c176f8dc30b6aaccf9f86fb88a941e19222ba37c8f5b0dd5b23ec833856b3ae1",
                "5c2733ab01a494aaad6c89ac7de9535fe14293c5afc4dccee2ddcbbc45da9610",
                "f8266325f3044f19525c08a5990e9a66163af0fad64faa89429a38bbdbda8ca5",
                "787a9c05741fc9ae52c3984eeaa3cdf7cc8ea70a2841c0bab88b63f23d1021c0",
                "0a989e888b79a9f3cdca3dc269beb5024b85217b161511334092b1ab9c7f9c70",
                "14facaafa0d9ed6ec36c562d72e4d3413a3a22b2a0c408d2e5114af0f05ba24c",
                "eaa15599490d0745bd8b37741b6b4506d1c3283ad7d1814584c570dfb670797d",
                "afcd24e7388cfa6ba89b5a99753347740fe60e1a03c01f608f4107b3f5b912fc",
                "556b8677cd6eca9d6c391d1077760af827dbad5067d3bbbf788f496463b997a2",
                "62b3631289f56ed533cdea2d4c95d3fafe1c868e2e2b15f66c80b0ea16a29622",
            ),
            english + chinese,
        )
    }

    @Test
    fun `dark uiux pr03 equipment inventory and shop evidence hashes remain stable`() {
        val hashes = captureDarkUiuxPr03GoldenEvidence()

        assertEquals(
            mapOf(
                "dark-uiux-pr03-equipment-slots" to "c711c2cb5d09d3a2a9fe92833d4d22dcc5f4e7e59d8414d642d80a020081d841",
                "dark-uiux-pr03-inventory-empty" to "70c1875a7ef793e8c28e13f7c0f7e976b2fabb56efc4156475c2e3c32fc74a7a",
                "dark-uiux-pr03-inventory-stacked" to "38ccc5400a2042428d5b405525a1d1719f9f5dc7381f94a38eaecb06344b96f1",
                "dark-uiux-pr03-inscription-shop" to "de3c64b9bee81544d4a8bf3e2f77f4efb2d97d7d99e0e8e8e42a38d873354b81",
                "dark-uiux-pr03-shop-full-slot-replace" to "c30bcda437f1e786d367e9250adda74044fb0f23e0c403253f22d3b25bcc8b7e",
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
    fun `dark uiux pr08 director runtime evidence writes canonical artifacts`() {
        val hashes = captureDarkUiuxPr08DirectorEvidence()

        assertEquals(darkUiuxPr08DirectorEvidenceLabels, hashes.keys.toList())
        assertEquals(
            mapOf(
                "dark-uiux-pr08-director-parity-1672x941" to "e7f65d0f2f205e238909ec1cd9f54387a9eaf365912e1da5710c9df9e72ed936",
                "dark-uiux-pr08-director-map-stage-crop" to "84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676",
                "dark-uiux-pr08-director-right-panel-crop" to "201645bf13f9824fd95289063b92ea8adc722f9d0f344aca75902a2e99c26c47",
                "dark-uiux-pr08-director-bottom-deck-crop" to "a416b96d21059212a6a7682f8163f0f065dcac3b545bad28b064814f8120cdfd",
                "dark-uiux-pr08-director-forest-map-stage-crop" to "b5d85c0849478f5b0f355a07aa7a5853824a9da048c48539edb7a7c61e358368",
                "dark-uiux-pr08-director-mine-map-stage-crop" to "7233f0dc91505c77e7cfe4b1cbb8d02ed6d92c71d11b9e1911e33b1ecffbb3b4",
                "dark-uiux-pr08-director-shadow-depths-map-stage-crop" to "674afbdba5bb9e4f8618c01fd7ea75590439bd5c400fb2aa000542fd02886eb5",
                "dark-uiux-pr08-director-telegraph-combat-crop" to "2e13535fea38cef3e7a624d3659685e756588d7df3ae4907e2dd7ab03ac51b57",
            ),
            hashes,
        )
        assertTrue(hashes.values.all { hash -> hash.matches(Regex("[a-f0-9]{64}")) })
        assertTrue(
            hashes["dark-uiux-pr08-director-map-stage-crop"] != hashes["dark-uiux-pr08-director-right-panel-crop"],
            "PR08 director map-stage evidence must not duplicate the right-panel crop.",
        )
        assertTrue(
            hashes["dark-uiux-pr08-director-map-stage-crop"] != hashes["dark-uiux-pr08-director-bottom-deck-crop"],
            "PR08 director map-stage evidence must not duplicate the bottom-deck crop.",
        )
        assertTrue(
            listOf(
                hashes["dark-uiux-pr08-director-map-stage-crop"],
                hashes["dark-uiux-pr08-director-forest-map-stage-crop"],
                hashes["dark-uiux-pr08-director-mine-map-stage-crop"],
                hashes["dark-uiux-pr08-director-shadow-depths-map-stage-crop"],
            ).filterNotNull().toSet().size == 4,
            "PR08 director family evidence must capture independent map-stage crops per accepted room-art family.",
        )
    }

    @Test
    fun `dark uiux pr08 random seed generalization probe writes board artifacts`() {
        val evidence = captureDarkUiuxPr08GeneralizationProbe()
        val hashes = evidence.associate { entry -> entry.probe.label to entry.hash }

        assertEquals(darkUiuxPr08GeneralizationEvidenceLabels, hashes.keys.toList())
        assertTrue(hashes.values.all { hash -> hash.matches(Regex("[a-f0-9]{64}")) })
        assertTrue(
            evidence.all { entry -> entry.topology.tilesetKey == DarkUiMapVisualKeys.RUINS_TILESET },
            "PR08 D8 probe must stay on the tileset.ruins family.",
        )
        assertTrue(
            evidence.all { entry -> entry.topology.visibleMaterialCells > 0 },
            "PR08 D8 probe must capture visible room material for every seed.",
        )
        assertTrue(
            evidence.map { entry -> entry.topology.signature }.toSet().size >= 4,
            "PR08 D8 probe must include varied visible topology signatures, not eight copies of the fixed proof crop.",
        )
        assertTrue(
            Files.isRegularFile(darkUiuxPr08GeneralizationGoldenDir().resolve("dark-uiux-pr08-generalization-board.png")),
            "PR08 D8 probe must write a board artifact for director review.",
        )
    }

    @Test
    fun `dark uiux pr08 topology source mask diagnostic writes owner artifacts`() {
        val hashes = captureDarkUiuxPr08TopologySourceMaskDiagnostic()

        assertEquals(darkUiuxPr08TopologyMaskEvidenceLabels, hashes.keys.toList())
        assertTrue(hashes.values.all { hash -> hash.matches(Regex("[a-f0-9]{64}")) })
        assertTrue(
            Files.isRegularFile(darkUiuxPr08TopologyMaskGoldenDir().resolve("dark-uiux-pr08-topology-source-mask-board.png")),
            "PR08 topology source mask diagnostic must write a repeatable owner board artifact.",
        )
        assertTrue(
            Files.isRegularFile(darkUiuxPr08TopologyMaskGoldenDir().resolve("evidence-index.tsv")),
            "PR08 topology source mask diagnostic must write an evidence index.",
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
                "phase4-uiux-pr05-telegraph-triple-surface" to "f02c27ffcd9259f08b03ac26ad082ce9adf760da79353417d7442ad241bdecb6",
                "phase4-uiux-pr05-combat-action" to "cec3fd50461f55e1748258ed8b0bc09b2589895e101e89a91e4cf6a9c71d565a",
                "phase4-uiux-pr05-combat-method" to "bfe2e831f7e98a935f0f90a23d06bda6382ae8f18225a21c966d2d3a5d801e2a",
                "phase4-uiux-pr05-combat-target" to "e8227182d9f951a919ab1c3c3c186ed91b24fddd5d81c9805364aa3dfcd0ec29",
                "phase4-uiux-pr05-combat-disabled-resource" to "cec3fd50461f55e1748258ed8b0bc09b2589895e101e89a91e4cf6a9c71d565a",
                "phase4-uiux-pr05-combat-illegal-target" to "3807fa67a1475652dc2c37178bcd50fe243b696c4f48f232d6dc63a67e958951",
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
                "phase4-v4-pr05-molten-glass-phase-override-warning" to "32d68de0d46bd37f40d961565f784b10629d214c05c53c51d058c7301ed667bd",
                "phase4-v4-pr05-grey-crown-phase-override-warning" to "32d68de0d46bd37f40d961565f784b10629d214c05c53c51d058c7301ed667bd",
                "phase4-v4-pr05-abyssal-eclipse-phase-override-warning" to "32d68de0d46bd37f40d961565f784b10629d214c05c53c51d058c7301ed667bd",
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

    private fun captureDarkUiuxPr08DirectorEvidence(): Map<String, String> {
        val hashes = linkedMapOf<String, String>()
        hashes +=
            captureDarkUiuxPr08DirectorEvidenceWindow(width = 1672, height = 941) { app, overlaySource ->
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                linkedMapOf(
                    "dark-uiux-pr08-director-parity-1672x941" to
                        captureGoldenArtifact(
                            label = "dark-uiux-pr08-director-parity-1672x941",
                            evidenceDir = darkUiuxPr08DirectorGoldenDir(),
                            flipY = true,
                        ) {
                            repeat(2) { app.render() }
                        },
                )
            }
        hashes +=
            captureDarkUiuxPr08DirectorEvidenceWindow(width = 1280, height = 800) { app, overlaySource ->
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for PR-08 director evidence." }
                val layout = currentTileLayout(session)
                val mapStageCrop = goldenCrop(layout.demoShell.mapStage, layout)
                val rightPanelCrop = goldenCrop(layout.demoShell.rightPanel, layout)
                val bottomDeckCrop = goldenCrop(layout.demoShell.bottomDeck.bounds, layout)
                val windowHashes = linkedMapOf<String, String>()
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                windowHashes["dark-uiux-pr08-director-map-stage-crop"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr08-director-map-stage-crop",
                        evidenceDir = darkUiuxPr08DirectorGoldenDir(),
                        crop = mapStageCrop,
                        flipY = true,
                    ) {
                        repeat(2) { app.render() }
                    }
                windowHashes["dark-uiux-pr08-director-right-panel-crop"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr08-director-right-panel-crop",
                        evidenceDir = darkUiuxPr08DirectorGoldenDir(),
                        crop = rightPanelCrop,
                        flipY = true,
                    ) {
                        repeat(2) { app.render() }
                    }
                windowHashes["dark-uiux-pr08-director-bottom-deck-crop"] =
                    captureGoldenArtifact(
                        label = "dark-uiux-pr08-director-bottom-deck-crop",
                        evidenceDir = darkUiuxPr08DirectorGoldenDir(),
                        crop = bottomDeckCrop,
                        flipY = true,
                    ) {
                        repeat(2) { app.render() }
                    }
                windowHashes
            }
        hashes += captureDarkUiuxPr08DirectorFamilyMapStageCrops()
        hashes += captureDarkUiuxPr08DirectorTelegraphCombatCrop()
        writeDarkUiuxPr08DirectorEvidenceIndex(hashes)
        return hashes
    }

    private fun captureDarkUiuxPr08DirectorEvidenceWindow(
        width: Int,
        height: Int,
        capture: (GameApp, MutableOverlayCommandSource) -> Map<String, String>,
    ): Map<String, String> =
        withLwjgl3Context(width = width, height = height) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr08-director-evidence-$width-$height")),
                    validationSaveManager = SaveManager(tempDir.resolve("dark-uiux-pr08-director-validation-$width-$height")),
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
                requireNotNull(app.activeSessionOrNull()) { "Expected active validation session for PR-08 director evidence." }
                capture(app, overlaySource)
            } finally {
                app.dispose()
            }
        }

    private fun captureDarkUiuxPr08DirectorFamilyMapStageCrops(): Map<String, String> =
        listOf(
            DarkUiuxPr08DirectorFamilyEvidence(
                scenarioId = ValidationScenarioId("dark-uiux-pr08-director-forest-map-stage"),
            ),
            DarkUiuxPr08DirectorFamilyEvidence(
                scenarioId = ValidationScenarioId("dark-uiux-pr08-director-mine-map-stage"),
            ),
            DarkUiuxPr08DirectorFamilyEvidence(
                scenarioId = ValidationScenarioId("dark-uiux-pr08-director-shadow-depths-map-stage"),
            ),
        ).fold(linkedMapOf<String, String>()) { hashes, family ->
            hashes += captureDarkUiuxPr08DirectorFamilyMapStageCrop(family)
            hashes
        }

    private fun captureDarkUiuxPr08DirectorFamilyMapStageCrop(family: DarkUiuxPr08DirectorFamilyEvidence): Map<String, String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("${family.label}-save")),
                    defaultConfig = family.scenario.toSessionOptions(ContentPackSelection.EMPTY).foundationConfig,
                    menuInputSourceFactory = { NoOpInputSource },
                    gameCommandSourceFactory = { overlaySource },
                    outcomeInputSourceFactory = { NoOpInputSource },
                    renderEnabled = true,
                    initialLocale = GameLocale.ZH_CN,
                )

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for ${family.label}." }
                val layout = currentTileLayout(session)
                val mapStageCrop = goldenCrop(layout.demoShell.mapStage, layout)
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                linkedMapOf(
                    family.label to
                        captureGoldenArtifact(
                            label = family.label,
                            evidenceDir = darkUiuxPr08DirectorGoldenDir(),
                            crop = mapStageCrop,
                            flipY = true,
                        ) {
                            repeat(2) { app.render() }
                        },
                )
            } finally {
                app.dispose()
            }
        }

    private data class DarkUiuxPr08DirectorFamilyEvidence(
        val scenarioId: ValidationScenarioId,
    ) {
        val scenario = ValidationScenarioRegistry.require(scenarioId)
        val label =
            scenario.evidence.requiredEvidenceFiles
                .single { evidenceFile -> evidenceFile.endsWith("-map-stage-crop.png") }
                .removePrefix("evidence/")
                .removeSuffix(".png")
    }

    private fun captureDarkUiuxPr08GeneralizationProbe(): List<Pr08GeneralizationProbeEvidence> {
        val evidence =
            darkUiuxPr08GeneralizationProbes.map { probe ->
                captureDarkUiuxPr08GeneralizationProbeCrop(probe)
            }
        writeDarkUiuxPr08GeneralizationEvidenceIndex(evidence)
        writeDarkUiuxPr08GeneralizationBoard(evidence)
        return evidence
    }

    private fun captureDarkUiuxPr08GeneralizationProbeCrop(
        probe: Pr08GeneralizationProbe,
    ): Pr08GeneralizationProbeEvidence =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("${probe.label}-save")),
                    defaultConfig =
                        FoundationGameConfig(
                            seed = probe.seed,
                            zoneId = probe.zoneId,
                            floor = probe.floor,
                            maxFloor = maxOf(2, probe.floor),
                            playerProfessionId = probe.professionId,
                            zoneRoute = listOf(probe.zoneId),
                            routeIndex = 0,
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
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for ${probe.label}." }
                val layout = currentTileLayout(session)
                val mapStageCrop = goldenCrop(layout.demoShell.mapStage, layout)
                val topology = pr08RuinsVisibleTopology(session)
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                val hash =
                    captureGoldenArtifact(
                        label = probe.label,
                        evidenceDir = darkUiuxPr08GeneralizationGoldenDir(),
                        crop = mapStageCrop,
                        flipY = true,
                    ) {
                        repeat(2) { app.render() }
                    }
                Pr08GeneralizationProbeEvidence(
                    probe = probe,
                    hash = hash,
                    topology = topology,
                )
            } finally {
                app.dispose()
        }
    }

    private fun captureDarkUiuxPr08TopologySourceMaskDiagnostic(): Map<String, String> {
        val evidenceDir = darkUiuxPr08TopologyMaskGoldenDir()
        Files.createDirectories(evidenceDir)
        val sourcePath =
            repoRootPath().resolve(
                "client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png",
            )
        require(Files.isRegularFile(sourcePath)) {
            "PR08 topology source diagnostic expected the promoted runtime source PNG."
        }
        val source = ImageIO.read(sourcePath.toFile())
        val board = renderPr08TopologySourceMaskBoard(source)
        val boardPath = evidenceDir.resolve("dark-uiux-pr08-topology-source-mask-board.png")
        ImageIO.write(board, "png", boardPath.toFile())
        val hashes =
            linkedMapOf(
                "dark-uiux-pr08-topology-source-mask-board" to fileHash(boardPath),
            )
        writeDarkUiuxPr08TopologySourceMaskEvidenceIndex(
            hashes = hashes,
            sourceHash = fileHash(sourcePath),
        )
        return hashes
    }

    private fun renderPr08TopologySourceMaskBoard(
        source: java.awt.image.BufferedImage,
    ): java.awt.image.BufferedImage {
        val probes = pr08TopologyMaskProbes()
        val panelWidth = 900
        val panelHeight = 360
        val margin = 24
        val board =
            java.awt.image.BufferedImage(
                panelWidth,
                margin + probes.size * panelHeight + (probes.size - 1) * margin,
                java.awt.image.BufferedImage.TYPE_INT_ARGB,
            )
        val graphics = board.createGraphics()
        try {
            graphics.color = java.awt.Color(3, 10, 10)
            graphics.fillRect(0, 0, board.width, board.height)
            probes.forEachIndexed { index, probe ->
                val panelY = margin + index * (panelHeight + margin)
                drawPr08TopologyMaskProbePanel(
                    graphics = graphics,
                    source = source,
                    probe = probe,
                    x = margin,
                    y = panelY,
                    width = panelWidth - margin * 2,
                    height = panelHeight,
                )
            }
        } finally {
            graphics.dispose()
        }
        return board
    }

    private fun drawPr08TopologyMaskProbePanel(
        graphics: java.awt.Graphics2D,
        source: java.awt.image.BufferedImage,
        probe: Pr08TopologyMaskProbe,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        graphics.color = java.awt.Color(2, 8, 8)
        graphics.fillRect(x, y, width, height)
        graphics.color = java.awt.Color(120, 73, 17)
        graphics.drawRect(x, y, width - 1, height - 1)
        graphics.font = java.awt.Font("SansSerif", java.awt.Font.PLAIN, 16)
        graphics.color = java.awt.Color(223, 215, 196)
        graphics.drawString(probe.label, x + 16, y + 28)

        val cellSize = minOf(46, (width - 120) / probe.columns, (height - 92) / probe.rows)
        val gridWidth = cellSize * probe.columns
        val gridHeight = cellSize * probe.rows
        val gridX = x + (width - gridWidth) / 2
        val gridY = y + 58

        graphics.color = java.awt.Color(1, 6, 6)
        graphics.fillRect(gridX - 10, gridY - 10, gridWidth + 20, gridHeight + 20)
        for (row in 0 until probe.rows) {
            for (column in 0 until probe.columns) {
                val cellX = gridX + column * cellSize
                val cellY = gridY + row * cellSize
                if ((column to row) in probe.visibleCells) {
                    graphics.drawImage(
                        source,
                        cellX,
                        cellY,
                        cellX + cellSize,
                        cellY + cellSize,
                        column * source.width / probe.columns,
                        row * source.height / probe.rows,
                        (column + 1) * source.width / probe.columns,
                        (row + 1) * source.height / probe.rows,
                        null,
                    )
                    graphics.color = java.awt.Color(0, 245, 120)
                } else {
                    graphics.color = java.awt.Color(3, 8, 8)
                    graphics.fillRect(cellX, cellY, cellSize, cellSize)
                    graphics.color = java.awt.Color(255, 34, 34)
                }
                graphics.drawRect(cellX, cellY, cellSize - 1, cellSize - 1)
            }
        }
        graphics.color = java.awt.Color(0, 220, 210)
        probe.cropBands.forEach { band ->
            graphics.drawRect(
                gridX + band.x * cellSize,
                gridY + band.y * cellSize,
                band.width * cellSize - 1,
                band.height * cellSize - 1,
            )
        }
        graphics.font = java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12)
        graphics.color = java.awt.Color(187, 178, 158)
        graphics.drawString("cyan=crop band, green=visible cells, red=hidden bbox cells", x + 16, y + height - 18)
    }

    private fun pr08TopologyMaskProbes(): List<Pr08TopologyMaskProbe> =
        listOf(
            Pr08TopologyMaskProbe(
                label = "L-like visible topology - current dedicated topology source",
                columns = 8,
                rows = 7,
                visibleCells =
                    buildSet {
                        for (row in 0..3) {
                            for (column in 0..2) {
                                add(column to row)
                            }
                        }
                        for (row in 4..6) {
                            for (column in 0..7) {
                                add(column to row)
                            }
                        }
                    },
                cropBands =
                    listOf(
                        Pr08TopologyMaskBand(x = 0, y = 0, width = 3, height = 4),
                        Pr08TopologyMaskBand(x = 0, y = 4, width = 8, height = 3),
                    ),
            ),
            Pr08TopologyMaskProbe(
                label = "corridor-heavy offset topology - current dedicated topology source",
                columns = 13,
                rows = 7,
                visibleCells =
                    buildSet {
                        for (row in 0..2) {
                            for (column in 8..12) {
                                add(column to row)
                            }
                        }
                        for (row in 3..4) {
                            for (column in 0..12) {
                                add(column to row)
                            }
                        }
                        for (row in 5..6) {
                            for (column in 0..3) {
                                add(column to row)
                            }
                        }
                    },
                cropBands =
                    listOf(
                        Pr08TopologyMaskBand(x = 8, y = 0, width = 5, height = 3),
                        Pr08TopologyMaskBand(x = 0, y = 3, width = 13, height = 2),
                        Pr08TopologyMaskBand(x = 0, y = 5, width = 4, height = 2),
                    ),
            ),
            Pr08TopologyMaskProbe(
                label = "tall cross topology - current dedicated topology source",
                columns = 8,
                rows = 10,
                visibleCells =
                    buildSet {
                        for (row in 0..9) {
                            for (column in 3..4) {
                                add(column to row)
                            }
                        }
                        for (row in 4..6) {
                            for (column in 0..7) {
                                add(column to row)
                            }
                        }
                    },
                cropBands =
                    listOf(
                        Pr08TopologyMaskBand(x = 3, y = 0, width = 2, height = 10),
                        Pr08TopologyMaskBand(x = 0, y = 4, width = 8, height = 3),
                    ),
            ),
        )

    private fun pr08RuinsVisibleTopology(session: FoundationGameSession): Pr08VisibleTopology {
        val snapshot = session.renderSnapshot()
        val visibleMaterialCells =
            snapshot.mapCells.filter { cell ->
                cell.visibility == CellVisibilitySnapshot.VISIBLE &&
                    (cell.terrainVisualKey == DarkUiMapVisualKeys.RUINS_GROUND ||
                        cell.terrainVisualKey == DarkUiMapVisualKeys.RUINS_WALL)
            }
        require(visibleMaterialCells.isNotEmpty()) {
            "PR08 generalization probe expected visible ruins floor or wall material."
        }
        val minX = visibleMaterialCells.minOf { cell -> cell.x }
        val maxX = visibleMaterialCells.maxOf { cell -> cell.x }
        val minY = visibleMaterialCells.minOf { cell -> cell.y }
        val maxY = visibleMaterialCells.maxOf { cell -> cell.y }
        val boundsWidth = maxX - minX + 1
        val boundsHeight = maxY - minY + 1
        val fillRatioPermille = (visibleMaterialCells.size * 1000) / (boundsWidth * boundsHeight).coerceAtLeast(1)
        return Pr08VisibleTopology(
            tilesetKey = snapshot.metadata.tilesetKey,
            visibleMaterialCells = visibleMaterialCells.size,
            boundsWidth = boundsWidth,
            boundsHeight = boundsHeight,
            fillRatioPermille = fillRatioPermille,
            playerOffsetX = snapshot.metadata.playerX - minX,
            playerOffsetY = snapshot.metadata.playerY - minY,
        )
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

    private fun captureDarkUiuxPr08DirectorTelegraphCombatCrop(): Map<String, String> =
        withLwjgl3Context(width = 1280, height = 800) {
            val overlaySource = MutableOverlayCommandSource()
            val app =
                GameApp(
                    saveManager = SaveManager(tempDir.resolve("dark-uiux-pr08-director-telegraph-combat")),
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
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected active session for PR-08 director telegraph evidence." }
                val stairsDown = requireNotNull(automationStairPoint(session, StairDirection.DOWN)) {
                    "Expected downstairs entry for PR-08 director telegraph evidence."
                }
                automationMovePlayerTo(session, stairsDown)
                app.render()
                check(session.perform(PlayerCommand.Descend)) { "Failed to descend into the boss floor for PR-08 director telegraph evidence." }
                app.render()

                val bossId = requireNotNull(automationBossEntity(session)) { "Expected a live boss entity for PR-08 director telegraph evidence." }
                val bossPoint = requireNotNull(automationWorld(session).get<Position>(bossId)) {
                    "Expected boss position for PR-08 director telegraph evidence."
                }.toPoint()
                automationMovePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
                prepareBossTelegraphFixture(session, bossId)
                val snapshot = waitForBossTelegraph(session, app)
                assertTrue(snapshot.overlays.any { overlay -> overlay.id.startsWith("telegraph:") })
                val layout = currentTileLayout(session)
                val mapStageCrop = goldenCrop(layout.demoShell.mapStage, layout)
                linkedMapOf(
                    "dark-uiux-pr08-director-telegraph-combat-crop" to
                        captureGoldenArtifact(
                            label = "dark-uiux-pr08-director-telegraph-combat-crop",
                            evidenceDir = darkUiuxPr08DirectorGoldenDir(),
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

    private fun writeDarkUiuxPr08DirectorEvidenceIndex(hashes: Map<String, String>) {
        val evidenceDir = darkUiuxPr08DirectorGoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact")
                hashes.forEach { (label, hash) ->
                    appendLine("$label\t$hash\tclient/build/reports/golden/dark-uiux-pr08-director/$label.png")
                }
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun writeDarkUiuxPr08GeneralizationEvidenceIndex(evidence: List<Pr08GeneralizationProbeEvidence>) {
        val evidenceDir = darkUiuxPr08GeneralizationGoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine(
                    "label\thash\tartifact\tseed\tzone\tfloor\tprofession\tviewport\tprobeIntent\ttileset\t" +
                        "visibleMaterialCells\tboundsWidth\tboundsHeight\tfillRatioPermille\tplayerOffset\ttopologySignature",
                )
                evidence.forEach { entry ->
                    appendLine(
                        listOf(
                            entry.probe.label,
                            entry.hash,
                            "client/build/reports/golden/dark-uiux-pr08-generalization/${entry.probe.label}.png",
                            entry.probe.seed.toString(),
                            entry.probe.zoneId,
                            entry.probe.floor.toString(),
                            entry.probe.professionId,
                            "1280x800",
                            entry.probe.probeIntent,
                            entry.topology.tilesetKey,
                            entry.topology.visibleMaterialCells.toString(),
                            entry.topology.boundsWidth.toString(),
                            entry.topology.boundsHeight.toString(),
                            entry.topology.fillRatioPermille.toString(),
                            "${entry.topology.playerOffsetX}_${entry.topology.playerOffsetY}",
                            entry.topology.signature,
                        ).joinToString(separator = "\t"),
                    )
                }
                appendLine(
                    "dark-uiux-pr08-generalization-board\tN/A\t" +
                        "client/build/reports/golden/dark-uiux-pr08-generalization/dark-uiux-pr08-generalization-board.png" +
                        "\tN/A\tN/A\tN/A\tN/A\t1280x800\t4x2 D8 crop board\tN/A\tN/A\tN/A\tN/A\tN/A\tN/A\tN/A",
                )
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun writeDarkUiuxPr08TopologySourceMaskEvidenceIndex(
        hashes: Map<String, String>,
        sourceHash: String,
    ) {
        val evidenceDir = darkUiuxPr08TopologyMaskGoldenDir()
        Files.createDirectories(evidenceDir)
        val rows =
            buildString {
                appendLine("label\thash\tartifact\tsourceArtifact\tsourceHash\tmaskCount\tcontract")
                hashes.forEach { (label, hash) ->
                    appendLine(
                        listOf(
                            label,
                            hash,
                            "client/build/reports/golden/dark-uiux-pr08-topology-source-mask/$label.png",
                            "client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png",
                            sourceHash,
                            pr08TopologyMaskProbes().size.toString(),
                            "review-only mask diagnostic promoted to repeatable golden owner evidence",
                        ).joinToString(separator = "\t"),
                    )
                }
            }
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), rows)
    }

    private fun writeDarkUiuxPr08GeneralizationBoard(evidence: List<Pr08GeneralizationProbeEvidence>) {
        val evidenceDir = darkUiuxPr08GeneralizationGoldenDir()
        Files.createDirectories(evidenceDir)
        val images =
            evidence.map { entry ->
                ImageIO.read(evidenceDir.resolve("${entry.probe.label}.png").toFile())
            }
        val columns = 4
        val rows = (images.size + columns - 1) / columns
        val cellWidth = images.maxOf { image -> image.width }
        val cellHeight = images.maxOf { image -> image.height }
        val board =
            java.awt.image.BufferedImage(
                cellWidth * columns,
                cellHeight * rows,
                java.awt.image.BufferedImage.TYPE_INT_ARGB,
            )
        val graphics = board.createGraphics()
        try {
            graphics.color = java.awt.Color.BLACK
            graphics.fillRect(0, 0, board.width, board.height)
            images.forEachIndexed { index, image ->
                val column = index % columns
                val row = index / columns
                graphics.drawImage(
                    image,
                    column * cellWidth,
                    row * cellHeight,
                    cellWidth,
                    cellHeight,
                    null,
                )
            }
            ImageIO.write(board, "png", evidenceDir.resolve("dark-uiux-pr08-generalization-board.png").toFile())
        } finally {
            graphics.dispose()
        }
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

    private fun darkUiuxPr08DirectorGoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr08-director")

    private fun darkUiuxPr08GeneralizationGoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr08-generalization")

    private fun darkUiuxPr08TopologyMaskGoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/dark-uiux-pr08-topology-source-mask")

    private fun phase4UiuxPr05GoldenDir(): Path =
        repoRootPath().resolve("client/build/reports/golden/phase4-uiux-pr05")

    private fun repoRootPath(): Path =
        Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()

    private fun currentTileLayout(session: FoundationGameSession): TileLayoutMetrics {
        val snapshot = session.renderSnapshot()
        return TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, cellWidth = 42f, cellHeight = 42f)
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
        val layout = TileRenderer.layoutMetrics(snapshot.metadata.width, snapshot.metadata.height, cellWidth = 42f, cellHeight = 42f)
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

    private fun fileHash(path: Path): String =
        MessageDigest.getInstance("SHA-256")
            .digest(Files.readAllBytes(path))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

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
