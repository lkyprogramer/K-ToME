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
import com.ktome.client.input.GdxInputSource

internal const val menuWidth = 960f
internal const val menuHeight = 540f

class MainMenuScreen(
    private val app: GameApp,
    private val continueEnabled: Boolean,
    private val notice: String? = null,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private val viewport = FitViewport(menuWidth, menuHeight)
    private val controller = MainMenuController(GdxInputSource)

    override fun show() {
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
            null -> Unit
        }
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(font)

        ScreenUtils.clear(0.04f, 0.04f, 0.06f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        val entries = controller.entries(continueEnabled)
        val selectedIndex = controller.selectedIndex()

        batch.begin()
        font.color = Color.GOLD
        font.draw(batch, "K-ToME", 120f, 420f)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, "Main Menu", 120f, 392f)

        entries.forEachIndexed { index, entry ->
            font.color =
                when {
                    !entry.enabled -> Color.DARK_GRAY
                    index == selectedIndex -> Color.CYAN
                    else -> Color.WHITE
                }
            font.draw(batch, entry.label, 120f, 320f - index * 32f)
        }

        font.color = Color.GRAY
        font.draw(batch, "Up/Down select  Enter confirm", 120f, 140f)
        notice?.takeIf(String::isNotBlank)?.let { message ->
            font.color = Color.SALMON
            font.draw(batch, message, 120f, 100f)
        }
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
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
}
