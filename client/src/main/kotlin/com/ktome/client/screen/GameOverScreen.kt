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
import com.ktome.game.RunSummary

class GameOverScreen(
    private val app: GameApp,
    private val summary: RunSummary,
    private val inputSource: InputSource = GdxInputSource,
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private val viewport = FitViewport(menuWidth, menuHeight)

    override fun show() {
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

        ScreenUtils.clear(0.08f, 0.02f, 0.02f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        batch.begin()
        font.color = Color.SCARLET
        font.draw(batch, app.text("ui.game_over.title"), 120f, 420f)
        font.color = Color.WHITE
        font.draw(batch, app.text("ui.game_over.floor_reached", "current" to summary.floorReached, "max" to summary.maxFloor), 120f, 340f)
        font.draw(batch, app.text("ui.summary.turns_taken", "turns" to summary.turns), 120f, 308f)
        font.draw(batch, app.text("ui.summary.final_level", "level" to summary.playerLevel), 120f, 276f)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, app.text("ui.screen.return_to_menu"), 120f, 140f)
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
}
