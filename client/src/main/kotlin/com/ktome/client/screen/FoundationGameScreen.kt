package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.GameApp
import com.ktome.client.input.InputHandler
import com.ktome.client.render.AsciiRenderer
import com.ktome.game.FoundationGameSession

private const val cellWidth = 16f
private const val cellHeight = 16f

class FoundationGameScreen(
    private val app: GameApp,
    private val session: FoundationGameSession,
) : ScreenAdapter() {
    private val batch = SpriteBatch()
    private val renderer = AsciiRenderer(cellWidth = cellWidth, cellHeight = cellHeight)
    private val inputHandler = InputHandler()
    private val viewport = FitViewport(
        (session.map.width + AsciiRenderer.sidebarColumns) * cellWidth,
        (session.map.height + AsciiRenderer.uiRows) * cellHeight,
    )

    override fun show() {
        centerCamera()
    }

    override fun render(delta: Float) {
        if (session.runOutcome().isTerminal) {
            app.showOutcome(session)
            return
        }

        inputHandler.pollCommand(session)?.let { command ->
            val consumed = session.perform(command)
            inputHandler.onCommandResult(session, command, consumed)
        }

        if (session.runOutcome().isTerminal) {
            app.showOutcome(session)
            return
        }

        if (inputHandler.isMapMode() && Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            app.showMainMenu(saveCurrent = true)
            return
        }

        ScreenUtils.clear(0.03f, 0.03f, 0.05f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        batch.begin()
        renderer.render(batch, session, inputHandler.overlayState())
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        centerCamera()
    }

    override fun dispose() {
        renderer.dispose()
        batch.dispose()
    }

    private fun centerCamera() {
        viewport.camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)
        viewport.camera.update()
    }
}
