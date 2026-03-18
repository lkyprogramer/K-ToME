package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.game.FoundationGameSession
import com.ktome.game.PlayerCommand

interface CommandSource {
    fun nextCommand(session: FoundationGameSession): PlayerCommand?

    fun onCommandResult(
        session: FoundationGameSession,
        command: PlayerCommand,
        consumed: Boolean,
    )

    fun overlayState(): OverlayState

    fun isMapMode(): Boolean

    fun shouldReturnToMenu(): Boolean = false
}

class InputHandlerCommandSource(
    private val inputHandler: InputHandler = InputHandler(),
    private val inputSource: InputSource = GdxInputSource,
) : CommandSource {
    override fun nextCommand(session: FoundationGameSession): PlayerCommand? = inputHandler.pollCommand(session)

    override fun onCommandResult(
        session: FoundationGameSession,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        inputHandler.onCommandResult(session, command, consumed)
    }

    override fun overlayState(): OverlayState = inputHandler.overlayState()

    override fun isMapMode(): Boolean = inputHandler.isMapMode()

    override fun shouldReturnToMenu(): Boolean =
        isMapMode() && inputSource.isKeyJustPressed(Keys.ESCAPE)
}
