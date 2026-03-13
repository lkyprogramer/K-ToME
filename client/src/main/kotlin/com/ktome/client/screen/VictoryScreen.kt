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
import com.ktome.game.RunSummary

class VictoryScreen(
    private val app: GameApp,
    private val summary: RunSummary,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private val viewport = FitViewport(menuWidth, menuHeight)

    override fun show() {
        ensureResources()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            app.showMainMenu(saveCurrent = false)
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
        font.draw(batch, "Victory", 120f, 420f)
        font.color = Color.WHITE
        font.draw(batch, "Floors cleared: ${summary.floorReached}/${summary.maxFloor}", 120f, 340f)
        font.draw(batch, "Turns taken: ${summary.turns}", 120f, 308f)
        font.draw(batch, "Final level: ${summary.playerLevel}", 120f, 276f)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, "Enter or Esc to return to main menu", 120f, 140f)
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
