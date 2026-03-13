package com.ktome.client.replay

import com.ktome.client.input.InputSource

internal class ReplayInputSource : InputSource {
    private var pressedKeys: Set<Int> = emptySet()
    private var justPressedKeys: Set<Int> = emptySet()

    override fun isKeyJustPressed(keycode: Int): Boolean = keycode in justPressedKeys

    override fun isKeyPressed(keycode: Int): Boolean = keycode in pressedKeys

    fun frame(
        justPressed: Set<Int> = emptySet(),
        pressed: Set<Int> = justPressed,
    ) {
        justPressedKeys = justPressed
        pressedKeys = pressed
    }

    fun clear() {
        pressedKeys = emptySet()
        justPressedKeys = emptySet()
    }
}
