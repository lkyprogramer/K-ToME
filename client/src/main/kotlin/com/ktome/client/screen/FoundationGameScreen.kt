package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.GameApp
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputHandlerCommandSource
import com.ktome.client.render.AsciiRenderer
import com.ktome.game.FoundationGameSession

private const val cellWidth = 16f
private const val cellHeight = 16f

class FoundationGameScreen(
    private val app: GameApp,
    private val session: FoundationGameSession,
    private val commandSource: CommandSource = InputHandlerCommandSource(),
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var renderer: AsciiRenderer? = null
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

        commandSource.nextCommand(session)?.let { command ->
            val consumed = session.perform(command)
            commandSource.onCommandResult(session, command, consumed)
        }

        if (session.runOutcome().isTerminal) {
            app.showOutcome(session)
            return
        }

        if (commandSource.shouldReturnToMenu()) {
            app.showMainMenu(saveCurrent = true)
            return
        }

        if (!renderEnabled) {
            return
        }

        ensureResources()
        val batch = requireNotNull(batch)
        val renderer = requireNotNull(renderer)
        ScreenUtils.clear(0.03f, 0.03f, 0.05f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        batch.begin()
        renderer.render(batch, session, commandSource.overlayState())
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        if (renderEnabled) {
            viewport.update(width, height, true)
            centerCamera()
        }
    }

    override fun dispose() {
        renderer?.dispose()
        renderer = null
        batch?.dispose()
        batch = null
    }

    private fun centerCamera() {
        viewport.camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)
        viewport.camera.update()
    }

    private fun ensureResources() {
        if (batch == null) {
            batch = SpriteBatch()
        }
        if (renderer == null) {
            renderer = AsciiRenderer(cellWidth = cellWidth, cellHeight = cellHeight)
        }
    }
}
