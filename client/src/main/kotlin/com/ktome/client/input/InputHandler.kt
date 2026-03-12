package com.ktome.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.ktome.core.map.Point

class InputHandler {
    private val movementBindings = linkedMapOf(
        Keys.Q to Point(-1, -1),
        Keys.W to Point(0, -1),
        Keys.E to Point(1, -1),
        Keys.A to Point(-1, 0),
        Keys.D to Point(1, 0),
        Keys.Z to Point(-1, 1),
        Keys.X to Point(0, 1),
        Keys.C to Point(1, 1),
        Keys.UP to Point(0, -1),
        Keys.DOWN to Point(0, 1),
        Keys.LEFT to Point(-1, 0),
        Keys.RIGHT to Point(1, 0),
        Keys.HOME to Point(-1, -1),
        Keys.PAGE_UP to Point(1, -1),
        Keys.END to Point(-1, 1),
        Keys.PAGE_DOWN to Point(1, 1),
        Keys.NUMPAD_7 to Point(-1, -1),
        Keys.NUMPAD_8 to Point(0, -1),
        Keys.NUMPAD_9 to Point(1, -1),
        Keys.NUMPAD_4 to Point(-1, 0),
        Keys.NUMPAD_6 to Point(1, 0),
        Keys.NUMPAD_1 to Point(-1, 1),
        Keys.NUMPAD_2 to Point(0, 1),
        Keys.NUMPAD_3 to Point(1, 1),
    )

    fun pollMovement(): Point? =
        movementBindings.entries.firstOrNull { (key, _) -> Gdx.input.isKeyJustPressed(key) }?.value
}
