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
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputSource
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.PlayerCommand
import com.ktome.game.i18n.GameLocale
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opentest4j.TestAbortedException

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
                "820a41898093a475e4a32e72925e32b07268d2130ff1af55b63ad2926adeae33",
                "5f1d0a0beefe35591632a36775affbe7888ad7f8388b0f0e26b826744fe1129d",
                "f80ad05054066dc3269b3521adbd9af18d3f4feb3ad61755a3f0695aa2c62e40",
                "7e86ed2a97316e35f70438b676a96ee2faaa9d2e61a6b65c96ac2635a27d56a2",
                "3ef9508e890a818d8d80ee5e2d09486007b647fa1d14413b854548931e8bbdbb",
                "6a0d93bde0b6058c725a9ccb06f2ec3b437669ce69ccc2df80ac40b2ee6c9b67",
                "2cf6ff321b1c3abc1e074943445a1cba2fc5406054ea1958fad5207f7c5ea2bd",
                "470c23e05d96ba48099b0543ab5dbc6002fd321fc58026e4a976bd8589d1feeb",
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
                "ac51ba53b925c41690275ef69fc077d3a6d3f0b570928dd2ddceee7149b53f78",
                "8e4acbf07a3563abf6510557147aa5175cda0359bdf7cff38b6ca4ed69e21f1e",
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
                "7ea2a898a69c4004cd508b557d142c067160a7ce8c51eaf46233a0d5b82644c5",
                "82bb30ac7c565acd07ecbc5ae708ee4a31ebb3ec956916ad020ab6205d947460",
                "086a00c58a1092862105214b197a2c0cdc733ec56d309f72ae91c6ff5875c753",
                "31c00678f72b3937cb08a34954c6bc2c90cead3743e7b8c9e88c797c64f647e2",
                "f0164cdd7d72451810e92ecbfde126db1826fe3b7149442154eb93d14ff62911",
                "1b7bea26db556726694f74d4a5761a8a5ae420364e34ac16ea7d78bea2f2b4e8",
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
                "6f2b6ca634c01f6c86b868148ec92347ec89a4a43c13b40cede0c9bb2a83fc65",
                "c8f83fe0a33039f71c8dbc4e5b3c5162a0667beed8d2e5ceacd1b8a4bb970e37",
                "b8d0501796817d28853fd81302c4e48ecaf3ba50782b26c4064b403c5d9981f3",
                "7f418c9f08973502e7123d485120b602979f18ef0cf36ca550722c35c2e7d38c",
            ),
            english + chinese,
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

    private fun captureGameplaySet(
        locale: GameLocale,
        saveFolderName: String,
        defaultConfig: FoundationGameConfig,
        includeMenu: Boolean = false,
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

                val bossId = requireNotNull(automationEntityByTemplateId(session, "cultist.dungeon_lord")) { "Expected dungeon lord boss for overlay golden capture." }
                val bossPoint = requireNotNull(automationWorld(session).get<Position>(bossId)) { "Expected boss position for overlay golden capture." }.toPoint()
                automationMovePlayerTo(session, bossPoint)
                app.render()

                val snapshot = session.renderSnapshot()
                assertTrue(snapshot.overlays.any { overlay -> overlay.id.startsWith("boss-warning:") })
                assertTrue(snapshot.overlays.any { overlay -> overlay.id.startsWith("telegraph:") })
                assertTrue(snapshot.logEvents.any { event -> event.message.key == "log.warning.boss_presence" })
                assertTrue(snapshot.logEvents.any { event -> event.message.key == "log.warning.telegraph" })
                overlaySource.overlayState = OverlayState(mode = UiMode.MAP)
                return@withLwjgl3Context captureHash { repeat(2) { app.render() } }
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

private fun automationWorld(session: FoundationGameSession): World =
    invokeSessionInternal(session, "automationWorld") as World

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

private fun automationForceDefeatPlayer(session: FoundationGameSession) {
    invokeSessionInternal(session, "automationForceDefeatPlayer")
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
