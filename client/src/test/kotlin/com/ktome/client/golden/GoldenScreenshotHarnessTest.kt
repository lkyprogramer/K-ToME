package com.ktome.client.golden

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.utils.ScreenUtils
import com.ktome.client.GameApp
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputSource
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.FoundationGameConfig
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import com.ktome.game.i18n.GameLocale
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@Tag("goldenScreenshot")
class GoldenScreenshotHarnessTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `golden screenshot hashes remain stable for english and chinese formal screens`() {
        val english = captureGoldenSet(GameLocale.EN_US, "en-us")
        val chinese = captureGoldenSet(GameLocale.ZH_CN, "zh-cn")

        assertEquals(
            listOf(
                "cbd1cf6fd0c151be171c66530fe6d893b271bc8bbe6cfc1c8e859f7df89f3a8c",
                "d526fc5500537001e5bca0a5215222e786f543e4b2b7d3595bab7eaa063df13f",
                "934cffe81fbdf0c530011414be92df59f5a56ffdefc52f8864c10588ad06f05f",
                "b426031140d27f0eac2efb98906fa9eb668a2d033d9328235a4c0e49954361f2",
                "919c7b610f3fee94eeecb68375faadb8b2a05f55075e4b876c0513ade396acf7",
                "239bc9cfd5a916b6699d082acab8d34f8a292027bf1c1b39b0f77260fa2d3b28",
                "32e29c9eb24dc604bfdf2819c7c1100a7f24ffd9db1c5d745f220ba51d062a40",
                "9bc0dc7aaa2f486d15b0c2aa53984512c9d23af9e4cc24516654c27ec124e065",
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
                "3766bdbf75b14464012da759b452a21f6cbd76edfd08549c66dd0c6bcf2cfcb0",
                "7ca06c76c2cabcda1b8663468e02f74d02f19c770a1e67ea1be503a476eab778",
            ),
            listOf(english, chinese),
        )
    }

    private fun captureGoldenSet(
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
                val menuHash = captureHash { repeat(2) { app.render() } }

                app.startNewGame()
                val hudHash =
                    captureHash {
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        repeat(2) { app.render() }
                    }
                val inventoryHash =
                    captureHash {
                        overlaySource.overlayState = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 0)
                        repeat(2) { app.render() }
                    }
                val inspectHash =
                    captureHash {
                        overlaySource.overlayState = OverlayState(mode = UiMode.INSPECT)
                        repeat(2) { app.render() }
                    }
                listOf(menuHash, hudHash, inventoryHash, inspectHash)
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
            val bot = SmokeBot()

            try {
                app.create()
                app.startNewGame()
                val session = requireNotNull(app.activeSessionOrNull()) { "Expected an active session for overlay golden capture." }

                for (step in 0 until 800) {
                    val snapshot = session.renderSnapshot()
                    val hasBossWarning = snapshot.overlays.any { overlay -> overlay.id.startsWith("boss-warning:") }
                    val hasTelegraph = snapshot.overlays.any { overlay -> overlay.id.startsWith("telegraph:") }
                    if (hasBossWarning && hasTelegraph) {
                        assertTrue(snapshot.logEvents.any { event -> event.message.key == "log.warning.boss_presence" })
                        assertTrue(snapshot.logEvents.any { event -> event.message.key == "log.warning.telegraph" })
                        overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                        return@withLwjgl3Context captureHash { repeat(2) { app.render() } }
                    }

                    val command = bot.decide(RunObservationCapture.capture(session, step))
                    check(session.perform(command)) { "Command rejected while driving overlay golden: $command" }
                    app.render()
                }

                error("Failed to reach a visible boss warning overlay for locale ${locale.id}.")
            } finally {
                app.dispose()
            }
        }

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

        Lwjgl3Application(
            object : ApplicationAdapter() {
                override fun create() {
                    result = runCatching(block)
                    Gdx.app.exit()
                }
            },
            configuration,
        )

        return requireNotNull(result) {
            "LWJGL3 golden capture did not produce a result."
        }.getOrThrow()
    }
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
