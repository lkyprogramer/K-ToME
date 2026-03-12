package com.ktome.client.screen

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.input.InputHandler
import com.ktome.client.render.AsciiRenderer
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule

private const val cellWidth = 16f
private const val cellHeight = 16f

class FoundationGameScreen(
    private val session: FoundationGameSession = GameModule.newFoundationSession(
        FoundationGameConfig(),
    ),
) : ScreenAdapter() {
    private val batch = SpriteBatch()
    private val renderer = AsciiRenderer(cellWidth = cellWidth, cellHeight = cellHeight)
    private val inputHandler = InputHandler()
    private val viewport = FitViewport(
        session.map.width * cellWidth,
        (session.map.height + AsciiRenderer.uiRows) * cellHeight,
    )

    override fun show() {
        centerCamera()
    }

    override fun render(delta: Float) {
        inputHandler.pollCommand()?.let(session::perform)

        ScreenUtils.clear(0.03f, 0.03f, 0.05f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        batch.begin()
        renderer.render(batch, session)
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
