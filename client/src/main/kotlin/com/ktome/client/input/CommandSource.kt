package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.client.audio.AudioRouter
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

    fun onReturnToMenu() {}

    fun onSnapshotUpdated(
        previous: RenderSnapshot?,
        current: RenderSnapshot,
    ) {}
}

interface AudioRouterAwareCommandSource {
    var audioRouter: AudioRouter?
}

class InputHandlerCommandSource(
    private val inputHandler: InputHandler = InputHandler(),
    private val inputSource: InputSource = GdxInputSource,
    override var audioRouter: AudioRouter? = null,
) : CommandSource, AudioRouterAwareCommandSource {
    override fun nextCommand(snapshot: RenderSnapshot): PlayerCommand? {
        val previous = inputHandler.overlayState()
        val command = inputHandler.pollCommand(snapshot)
        audioRouter?.onOverlayStateChanged(previous, inputHandler.overlayState())
        return command
    }

    override fun onCommandResult(
        snapshot: RenderSnapshot,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        inputHandler.onCommandResult(snapshot, command, consumed)
        audioRouter?.onCommandResolved(snapshot, command, consumed)
    }

    override fun overlayState(): OverlayState = inputHandler.overlayState()

    override fun isMapMode(): Boolean = inputHandler.isMapMode()

    override fun shouldReturnToMenu(): Boolean =
        isMapMode() && inputSource.isKeyJustPressed(Keys.ESCAPE)

    override fun onReturnToMenu() {
        audioRouter?.onReturnToMenu()
    }

    override fun onSnapshotUpdated(
        previous: RenderSnapshot?,
        current: RenderSnapshot,
    ) {
        audioRouter?.onSnapshotUpdated(previous, current)
    }
}
