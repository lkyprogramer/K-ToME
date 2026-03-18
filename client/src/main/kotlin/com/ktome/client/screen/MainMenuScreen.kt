package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.GameApp
import com.ktome.client.input.InputSource
import com.ktome.client.input.GdxInputSource

internal const val menuWidth = 960f
internal const val menuHeight = 540f

internal data class MainMenuTextSnapshot(
    val title: String,
    val subtitle: String,
    val entries: List<String>,
    val language: String,
    val controls: String,
    val notice: String?,
)

class MainMenuScreen(
    private val app: GameApp,
    private val continueEnabled: Boolean,
    private val notice: String? = null,
    inputSource: InputSource = GdxInputSource,
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private val viewport = FitViewport(menuWidth, menuHeight)
    private val controller = MainMenuController(inputSource)

    override fun show() {
        if (!renderEnabled) {
            return
        }
        ensureResources()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun render(delta: Float) {
        when (controller.pollAction(continueEnabled)) {
            MainMenuAction.StartNewGame -> {
                app.startNewGame()
                return
            }
            MainMenuAction.ContinueGame -> {
                app.continueGame()
                return
            }
            MainMenuAction.ExitGame -> {
                Gdx.app.exit()
                return
            }
            MainMenuAction.ToggleLocale -> Unit.also { app.cycleLocale() }
            null -> Unit
        }
        if (!renderEnabled) {
            return
        }
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(font)
        val text = textSnapshot()

        ScreenUtils.clear(0.04f, 0.04f, 0.06f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        val selectedIndex = controller.selectedIndex()

        batch.begin()
        font.color = Color.GOLD
        font.draw(batch, text.title, 120f, 420f)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, text.subtitle, 120f, 392f)

        controller.entries(continueEnabled).forEachIndexed { index, entry ->
            font.color =
                when {
                    !entry.enabled -> Color.DARK_GRAY
                    index == selectedIndex -> Color.CYAN
                    else -> Color.WHITE
                }
            font.draw(batch, text.entries[index], 120f, 320f - index * 32f)
        }

        font.color = Color.GOLD
        font.draw(batch, text.language, 120f, 188f)
        font.color = Color.GRAY
        font.draw(batch, text.controls, 120f, 140f)
        text.notice?.let { message ->
            font.color = Color.SALMON
            font.draw(batch, message, 120f, 100f)
        }
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        if (renderEnabled) {
            viewport.update(width, height, true)
        }
    }

    override fun dispose() {
        font?.dispose()
        font = null
        batch?.dispose()
        batch = null
    }

    private fun ensureResources() {
        if (batch == null) {
            batch = SpriteBatch()
        }
        if (font == null) {
            font =
                BitmapFont().apply {
                    setUseIntegerPositions(true)
                }
        }
    }

    internal fun textSnapshot(): MainMenuTextSnapshot =
        MainMenuTextSnapshot(
            title = app.text("ui.menu.title"),
            subtitle = app.text("ui.menu.subtitle"),
            entries = controller.entries(continueEnabled).map { entry -> app.text(entry.labelKey) },
            language = app.text("ui.menu.language", "value" to app.localizer().localeLabel()),
            controls = app.text("ui.menu.controls"),
            notice = notice?.takeIf(String::isNotBlank),
        )
}
