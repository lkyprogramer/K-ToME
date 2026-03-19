package com.ktome.client.golden

import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.render.AsciiRenderer
import com.ktome.client.render.AsciiTextTone
import com.ktome.client.screen.MainMenuTextSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.Localizer
import com.ktome.game.i18n.LocalizationBundle
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.math.max
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@Tag("goldenScreenshot")
class GoldenScreenshotHarnessTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `golden screenshot hashes remain stable`() {
        val scenario = GoldenBaselineScenario()
        val localizer = LocalizationBundle.load().translator(scenario.locale)
        val renderer = SoftwareGoldenRenderer(scenario, localizer)
        val snapshot = liveGameplaySnapshot(scenario)

        val menuHash = renderer.pixelHash(renderer.renderMainMenu(productionMenuSnapshot(localizer)))
        val hudHash = renderer.pixelHash(renderer.renderGame(snapshot, UiMode.MAP))
        val inventoryHash = renderer.pixelHash(renderer.renderGame(snapshot, UiMode.INVENTORY))
        val inspectHash = renderer.pixelHash(renderer.renderGame(snapshot, UiMode.INSPECT))

        assertEquals("ded54864652335b5560e7cd8a600f43d659043411504a8b95c76bcb9e0d709e8", menuHash)
        assertEquals("ab00cc351c050bc357beba08d759e91fd5245a9a16629a8ec34d0297a05c357d", hudHash)
        assertEquals("007de4b64bce5812149d3577c654fa87cbc72484112713a5d829459dea583745", inventoryHash)
        assertEquals("9c08ab022b085dd1ae17fb6d476eed0ded56ef9264037496fbc1378e6b13556c", inspectHash)
    }

    private fun productionMenuSnapshot(localizer: Localizer): MainMenuTextSnapshot =
        MainMenuTextSnapshot(
            title = localizer.text("ui.menu.title"),
            subtitle = localizer.text("ui.menu.subtitle"),
            entries =
                listOf(
                    localizer.text("ui.menu.new_game"),
                    localizer.text("ui.menu.continue"),
                    localizer.text("ui.menu.exit"),
                ),
            language = localizer.text("ui.menu.language", "value" to localizer.localeLabel()),
            controls = localizer.text("ui.menu.controls"),
            notice = "Asset contract ready",
        )

    private fun liveGameplaySnapshot(scenario: GoldenBaselineScenario): RenderSnapshot =
        GameModule.newFoundationSession(
            config =
                FoundationGameConfig(
                    seed = scenario.seed,
                    zoneId = "shattered_outpost",
                    playerProfessionId = "vanguard",
                ),
            saveManager = SaveManager(tempDir.resolve("golden-save-${scenario.seed}")),
            locale = scenario.locale,
        ).renderSnapshot()

    private data class GoldenBaselineScenario(
        val seed: Long = 20260318L,
        val locale: GameLocale = GameLocale.EN_US,
        val menuBaseWidthPx: Int = 320,
        val menuBaseHeightPx: Int = 180,
        val gameBaseWidthPx: Int = 360,
        val gameBaseHeightPx: Int = 220,
        val cellBasePx: Int = 18,
        val uiScale: Int = 1,
        val fontId: String = "software-block-ui-v1",
    ) {
        val menuWidthPx: Int = menuBaseWidthPx * uiScale
        val menuHeightPx: Int = menuBaseHeightPx * uiScale
        val gameWidthPx: Int = gameBaseWidthPx * uiScale
        val gameHeightPx: Int = gameBaseHeightPx * uiScale
        val cellPx: Int = cellBasePx * uiScale
    }

    private class SoftwareGoldenRenderer(
        private val scenario: GoldenBaselineScenario,
        private val localizer: Localizer,
    ) {
        private val clientAssets = ClientAssetBundleLoader.load()

        init {
            require(scenario.fontId.isNotBlank()) { "Golden screenshot fontId must not be blank." }
        }

        fun renderMainMenu(snapshot: MainMenuTextSnapshot): BufferedImage {
            val scale = scenario.uiScale
            val image = BufferedImage(scenario.menuWidthPx, scenario.menuHeightPx, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            base(graphics, image.width, image.height)
            graphics.color = Color(0xCC, 0xAA, 0x33)
            graphics.fillRect(24 * scale, 18 * scale, snapshot.title.length * 12 * scale, 14 * scale)
            graphics.color = Color(0x88, 0x88, 0x99)
            graphics.fillRect(24 * scale, 40 * scale, snapshot.subtitle.length * 7 * scale, 8 * scale)
            snapshot.entries.forEachIndexed { index, entry ->
                graphics.color = if (index == 0) Color(0x33, 0xCC, 0xDD) else Color(0xDD, 0xDD, 0xDD)
                graphics.fillRect(24 * scale, (72 + index * 18) * scale, entry.length * 7 * scale, 10 * scale)
            }
            graphics.color = Color(0xDD, 0x99, 0x33)
            graphics.fillRect(24 * scale, 136 * scale, snapshot.language.length * 5 * scale, 8 * scale)
            graphics.color = Color(0x77, 0x77, 0x77)
            graphics.fillRect(24 * scale, 152 * scale, snapshot.controls.length * 4 * scale, 6 * scale)
            snapshot.notice?.let { notice ->
                graphics.color = Color(0xE7, 0x84, 0x7D)
                graphics.fillRect(24 * scale, 164 * scale, notice.length * 4 * scale, 6 * scale)
            }
            graphics.dispose()
            return image
        }

        fun renderGame(
            snapshot: RenderSnapshot,
            mode: UiMode,
        ): BufferedImage {
            val overlayState =
                when (mode) {
                    UiMode.INVENTORY -> OverlayState(mode = mode, inventorySelection = 0)
                    else -> OverlayState(mode = mode)
                }
            val model = AsciiRenderer.buildRenderModel(localizer, clientAssets.visualResolver, snapshot, overlayState)
            val cell = scenario.cellPx
            val scale = scenario.uiScale
            val image = BufferedImage(scenario.gameWidthPx, scenario.gameHeightPx, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            base(graphics, image.width, image.height)

            model.terrainGlyphs.forEach { glyph ->
                graphics.color = Color.decode(glyph.colorHex)
                graphics.fillRect(16 * scale + glyph.x * cell, 24 * scale + glyph.y * cell, cell - 2 * scale, cell - 2 * scale)
            }
            model.propGlyphs.forEach { glyph ->
                graphics.color = Color.decode(glyph.colorHex)
                graphics.fillRect(20 * scale + glyph.x * cell, 28 * scale + glyph.y * cell, cell - 10 * scale, cell - 10 * scale)
            }
            model.actorGlyphs.forEach { glyph ->
                graphics.color = Color.decode(glyph.colorHex)
                graphics.fillOval(19 * scale + glyph.x * cell, 27 * scale + glyph.y * cell, cell - 8 * scale, cell - 8 * scale)
            }
            model.targetCursor?.let { cursor ->
                graphics.color = Color(0xFF, 0x24, 0x00)
                graphics.stroke = BasicStroke(2f * scale)
                graphics.drawRect(16 * scale + cursor.x * cell, 24 * scale + cursor.y * cell, cell - 2 * scale, cell - 2 * scale)
            }
            model.inspectCursor?.let { cursor ->
                graphics.color = Color(0x00, 0xFF, 0xFF)
                graphics.stroke = BasicStroke(2f * scale)
                graphics.drawRect(19 * scale + cursor.x * cell, 27 * scale + cursor.y * cell, cell - 8 * scale, cell - 8 * scale)
            }

            graphics.color = Color(0xCC, 0xAA, 0x33)
            graphics.fillRect(16 * scale, 8 * scale, max(120 * scale, model.hudText.length * 2 * scale), 8 * scale)

            model.messageLines.takeLast(AsciiRenderer.messageRows).forEachIndexed { index, text ->
                graphics.color = Color(0xDD, 0xDD, 0xDD)
                graphics.fillRect(16 * scale, 206 * scale - index * 10 * scale, max(20 * scale, text.length * 3 * scale), 6 * scale)
            }

            model.sidebarLines.forEachIndexed { index, line ->
                if (line.text.isBlank()) {
                    return@forEachIndexed
                }
                graphics.color = tone(line.tone)
                graphics.fillRect(246 * scale, 24 * scale + index * 10 * scale, max(12 * scale, line.text.length * 3 * scale), 6 * scale)
            }

            graphics.dispose()
            return image
        }

        fun pixelHash(image: BufferedImage): String {
            val argb = IntArray(image.width * image.height)
            image.getRGB(0, 0, image.width, image.height, argb, 0, image.width)
            val bytes = ByteBuffer.allocate(argb.size * Int.SIZE_BYTES).apply { argb.forEach(::putInt) }.array()
            return MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
        }

        private fun tone(tone: AsciiTextTone): Color =
            when (tone) {
                AsciiTextTone.GOLD -> Color(0xCC, 0xAA, 0x33)
                AsciiTextTone.WHITE -> Color(0xDD, 0xDD, 0xDD)
                AsciiTextTone.LIGHT_GRAY -> Color(0xAA, 0xAA, 0xAA)
                AsciiTextTone.CYAN -> Color(0x33, 0xCC, 0xDD)
                AsciiTextTone.GRAY -> Color(0x77, 0x77, 0x77)
            }

        private fun base(
            graphics: Graphics2D,
            width: Int,
            height: Int,
        ) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            graphics.color = Color(0x0A, 0x0A, 0x14)
            graphics.fillRect(0, 0, width, height)
        }
    }
}
