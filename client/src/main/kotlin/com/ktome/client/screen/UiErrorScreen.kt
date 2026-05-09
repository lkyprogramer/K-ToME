package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.GameApp
import com.ktome.client.render.KtomeFonts
import com.ktome.client.ui.card.ModalCardAction
import com.ktome.client.ui.state.UiErrorState
import com.ktome.client.ui.token.UiDesignTokens

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
    private var chrome: StandaloneScreenChrome? = null
    private val viewport = FitViewport(UiDesignTokens.fixed.standaloneWidth, UiDesignTokens.fixed.standaloneHeight)

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
        val layout = DarkStandaloneScreenLayout.outcome()
        val surfaceBase = UiDesignTokens.color.surface.base.color()
        ScreenUtils.clear(surfaceBase.r, surfaceBase.g, surfaceBase.b, surfaceBase.a)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        requireNotNull(chrome).draw(batch, StandaloneChromeRequest(layout = layout, detailAreaMode = StandaloneDetailAreaMode.HIDDEN))
        font.color = UiDesignTokens.color.telegraph.high.color()
        font.draw(batch, app.text(errorState.heading), layout.header.x, layout.header.top - 22f)
        font.color = UiDesignTokens.color.text.primary.color()
        val bodyLines =
            listOf(DarkStandaloneScreenLayout.truncate(app.text(errorState.detail), 82)) +
                errorState.actions.map { action ->
                    DarkStandaloneScreenLayout.truncate(uiErrorActionLabel(action, app::text, errorState.copyDetailLabelKey), 82)
                }
        bodyLines.zip(DarkStandaloneScreenLayout.outcomeBodyLineBaselines(bodyLines.size)).forEach { (line, y) ->
            font.draw(batch, line, layout.primaryActionStack.x, y)
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
        chrome?.dispose()
        chrome = null
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
        if (chrome == null) {
            chrome = StandaloneScreenChrome()
        }
    }
}

internal fun uiErrorActionLabel(
    action: ModalCardAction,
    text: (String) -> String,
    copyDetailLabelKey: String = ModalCardAction.COPY_ERROR_DETAIL.labelKey,
): String =
    when (action) {
        ModalCardAction.RETRY -> "R - ${text(action.labelKey)}"
        ModalCardAction.BACK_TO_MENU -> "Esc - ${text(action.labelKey)}"
        ModalCardAction.COPY_ERROR_DETAIL -> "C - ${text(copyDetailLabelKey)}"
        else -> text(action.labelKey)
    }
