package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.client.replay.ReplayInputSource
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class InputHandlerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `x enters inspect mode and cursor movement stays UI only`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val session = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("inspect-mode-save")))
        val playerStart = session.playerPosition()

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        assertEquals(playerStart, handler.overlayState().inspectCursor)
        input.clear()

        val start = requireNotNull(handler.overlayState().inspectCursor)
        input.frame(justPressed = setOf(Keys.S))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(
            Point(start.x, (start.y + 1).coerceAtMost(session.map.height - 1)),
            handler.overlayState().inspectCursor,
        )
        assertEquals(playerStart, session.playerPosition())
        input.clear()

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
    }

    @Test
    fun `inspect mode remains available while stat points are pending`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val session = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("pending-stat-save")))
        requireNotNull(runtimeWorld(session).get<Experience>(session.playerId)).unspentStatPoints = 1

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        input.clear()

        input.frame()
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.MAP, handler.overlayState().mode)

        input.frame()
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.STAT_ASSIGN, handler.overlayState().mode)
    }

    @Test
    fun `map mode keeps south movement on s while control s remains save`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val session = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("movement-save")))

        input.frame(justPressed = setOf(Keys.S))
        assertEquals(PlayerCommand.Move(Point(0, 1)), handler.pollCommand(session.renderSnapshot()))
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S))
        assertEquals(PlayerCommand.SaveGame, handler.pollCommand(session.renderSnapshot()))
    }

    private fun runtimeWorld(session: com.ktome.game.FoundationGameSession): World {
        val field = com.ktome.game.FoundationGameSession::class.java.getDeclaredField("world")
        field.isAccessible = true
        return field.get(session) as World
    }
}
