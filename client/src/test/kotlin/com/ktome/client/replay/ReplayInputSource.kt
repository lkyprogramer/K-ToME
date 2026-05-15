package com.ktome.client.replay

import com.badlogic.gdx.Input.Buttons
import com.ktome.client.input.InputSource

internal class ReplayInputSource : InputSource {
    private var pressedKeys: Set<Int> = emptySet()
    private var justPressedKeys: Set<Int> = emptySet()
    private var justPressedButtons: Set<Int> = emptySet()
    private var pointerX: Int = 0
    private var pointerY: Int = 0
    private var viewportWidth: Int = 1280
    private var viewportHeight: Int = 800

    override fun isKeyJustPressed(keycode: Int): Boolean = keycode in justPressedKeys

    override fun isKeyPressed(keycode: Int): Boolean = keycode in pressedKeys

    override fun isButtonJustPressed(button: Int): Boolean = button in justPressedButtons

    override fun pointerX(): Int = pointerX

    override fun pointerY(): Int = pointerY

    override fun viewportWidth(): Int = viewportWidth

    override fun viewportHeight(): Int = viewportHeight

    fun frame(
        justPressed: Set<Int> = emptySet(),
        pressed: Set<Int> = justPressed,
        justPressedButtons: Set<Int> = emptySet(),
        pointerX: Int = this.pointerX,
        pointerY: Int = this.pointerY,
        viewportWidth: Int = this.viewportWidth,
        viewportHeight: Int = this.viewportHeight,
    ) {
        justPressedKeys = justPressed
        pressedKeys = pressed
        this.justPressedButtons = justPressedButtons
        this.pointerX = pointerX
        this.pointerY = pointerY
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
    }

    fun click(
        x: Int,
        y: Int,
        button: Int = Buttons.LEFT,
    ) {
        frame(justPressedButtons = setOf(button), pointerX = x, pointerY = y)
    }

    fun clear() {
        pressedKeys = emptySet()
        justPressedKeys = emptySet()
        justPressedButtons = emptySet()
    }
}
