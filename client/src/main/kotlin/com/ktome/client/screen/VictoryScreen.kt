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
import com.ktome.client.input.InputSource
import com.ktome.client.render.KtomeFonts
import com.ktome.game.OutcomeSummary

class VictoryScreen(
    private val app: GameApp,
    private val summary: OutcomeSummary,
    private val inputSource: InputSource = GdxInputSource,
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private val bodyLines: List<String>
        get() = OutcomeSummaryPresenter.bodyLines(app, summary, isVictory = true)
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private val viewport = FitViewport(menuWidth, menuHeight)

    override fun show() {
        app.audioRouterOrNull()?.onMenuShown()
        if (!renderEnabled) {
            return
        }
        ensureResources()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun render(delta: Float) {
        if (inputSource.isKeyJustPressed(Keys.ENTER) || inputSource.isKeyJustPressed(Keys.ESCAPE)) {
            app.showMainMenu(saveCurrent = false)
            return
        }
        if (!renderEnabled) {
            return
        }
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(font)

        ScreenUtils.clear(0.02f, 0.06f, 0.03f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        batch.begin()
        font.color = Color.FOREST
        font.draw(batch, app.text("ui.victory.title"), 120f, 470f)
        font.color = Color.WHITE
        var y = 418f
        bodyLines.forEach { line ->
            font.draw(batch, line, 120f, y)
            y -= 26f
        }
        font.color = Color.LIGHT_GRAY
        font.draw(batch, app.text("ui.screen.return_to_menu"), 120f, 40f)
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
            font = KtomeFonts.createUiFont(size = 20)
        }
    }
}
