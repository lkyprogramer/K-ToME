package com.ktome.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.ktome.client.ui.token.UiDesignTokens
import kotlin.math.min
import kotlin.math.roundToInt

interface InputSource {
    fun isKeyJustPressed(keycode: Int): Boolean

    fun isKeyPressed(keycode: Int): Boolean

    fun isButtonJustPressed(button: Int): Boolean = false

    fun pointerX(): Int = 0

    fun pointerY(): Int = 0

    fun viewportWidth(): Int = UiDesignTokens.fixed.shellPreferredWorldWidth.roundToInt()

    fun viewportHeight(): Int = UiDesignTokens.fixed.shellPreferredWorldHeight.roundToInt()
}

object GdxInputSource : InputSource {
    override fun isKeyJustPressed(keycode: Int): Boolean = Gdx.input.isKeyJustPressed(keycode)

    override fun isKeyPressed(keycode: Int): Boolean = Gdx.input.isKeyPressed(keycode)

    override fun isButtonJustPressed(button: Int): Boolean =
        button == Buttons.LEFT && Gdx.input.isButtonJustPressed(Buttons.LEFT)

    override fun pointerX(): Int = pointerWorldCoordinates().first

    override fun pointerY(): Int = pointerWorldCoordinates().second

    override fun viewportWidth(): Int = UiDesignTokens.fixed.shellPreferredWorldWidth.roundToInt()

    override fun viewportHeight(): Int = UiDesignTokens.fixed.shellPreferredWorldHeight.roundToInt()

    private fun pointerWorldCoordinates(): Pair<Int, Int> {
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        val worldWidth = UiDesignTokens.fixed.shellPreferredWorldWidth
        val worldHeight = UiDesignTokens.fixed.shellPreferredWorldHeight
        val scale = min(screenWidth / worldWidth, screenHeight / worldHeight).coerceAtLeast(0.0001f)
        val viewportPixelWidth = worldWidth * scale
        val viewportPixelHeight = worldHeight * scale
        val viewportPixelX = (screenWidth - viewportPixelWidth) / 2f
        val viewportPixelY = (screenHeight - viewportPixelHeight) / 2f
        val worldX = ((Gdx.input.x - viewportPixelX) / scale).roundToInt()
        val worldY = ((screenHeight - Gdx.input.y - viewportPixelY) / scale).roundToInt()
        return worldX to worldY
    }
}
