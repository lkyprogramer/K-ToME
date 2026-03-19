package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.PlayerCommand

interface CommandSource {
    fun nextCommand(snapshot: RenderSnapshot): PlayerCommand? = null

    fun onCommandResult(
        snapshot: RenderSnapshot,
        command: PlayerCommand,
        consumed: Boolean,
    ) {}

    fun overlayState(): OverlayState

    fun isMapMode(): Boolean

    fun shouldReturnToMenu(): Boolean = false
}

class InputHandlerCommandSource(
    private val inputHandler: InputHandler = InputHandler(),
    private val inputSource: InputSource = GdxInputSource,
) : CommandSource {
    override fun nextCommand(snapshot: RenderSnapshot): PlayerCommand? = inputHandler.pollCommand(snapshot)

    override fun onCommandResult(
        snapshot: RenderSnapshot,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        inputHandler.onCommandResult(snapshot, command, consumed)
    }

    override fun overlayState(): OverlayState = inputHandler.overlayState()

    override fun isMapMode(): Boolean = inputHandler.isMapMode()

    override fun shouldReturnToMenu(): Boolean =
        isMapMode() && inputSource.isKeyJustPressed(Keys.ESCAPE)
}
