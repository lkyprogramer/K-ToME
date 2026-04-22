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
import com.ktome.client.render.KtomeFonts
import com.ktome.client.ui.card.ModalCardAction
import com.ktome.client.ui.state.UiErrorState
import kotlin.math.max

private const val errorScreenWidth = 960f
private const val errorScreenHeight = 540f

internal class UiErrorScreen(
    private val app: GameApp,
    private val errorState: UiErrorState,
    private val retry: () -> Unit,
    private val backToMenu: () -> Unit,
    private val renderEnabled: Boolean = true,
    private val copyToClipboard: (String) -> Boolean = ::copyTextToClipboard,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private val viewport = FitViewport(errorScreenWidth, errorScreenHeight)

    override fun show() {
        if (!renderEnabled) {
            return
        }
        ensureResources()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun render(delta: Float) {
        if (handleInput()) {
            return
        }
        if (!renderEnabled) {
            return
        }
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(font)
        ScreenUtils.clear(0.06f, 0.02f, 0.02f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        font.color = Color.WHITE
        var y = max(120f, viewport.worldHeight - 48f)
        font.draw(batch, app.text(errorState.heading), 40f, y)
        y -= 40f
        font.draw(batch, app.text(errorState.detail), 40f, y)
        y -= 60f
        errorState.actions.forEach { action ->
            font.draw(batch, uiErrorActionLabel(action, app::text), 40f, y)
            y -= 32f
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

    private fun handleInput(): Boolean {
        if (Gdx.input == null) {
            return false
        }
        return when {
            Gdx.input.isKeyJustPressed(Keys.R) -> {
                retry()
                true
            }
            Gdx.input.isKeyJustPressed(Keys.C) -> {
                copyToClipboard(errorState.payload.renderPlainText())
                false
            }
            Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACKSPACE) -> {
                backToMenu()
                true
            }
            else -> false
        }
    }

    private fun ensureResources() {
        if (batch == null) {
            batch = SpriteBatch()
        }
        if (font == null) {
            font = KtomeFonts.createUiFont(size = 24)
        }
    }
}

internal fun uiErrorActionLabel(
    action: ModalCardAction,
    text: (String) -> String,
): String =
    when (action) {
        ModalCardAction.RETRY -> "R - ${text(action.labelKey)}"
        ModalCardAction.BACK_TO_MENU -> "Esc - ${text(action.labelKey)}"
        ModalCardAction.COPY_ERROR_DETAIL -> "C - ${text(action.labelKey)}"
        else -> text(action.labelKey)
    }
