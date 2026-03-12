package com.ktome.client.ui

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class MessageLog(
    private val maxLines: Int,
) {
    fun render(
        batch: SpriteBatch,
        font: BitmapFont,
        messages: List<String>,
        x: Float,
        baselineY: Float,
        lineHeight: Float,
    ) {
        messages.takeLast(maxLines).forEachIndexed { index, line ->
            font.draw(batch, line, x, baselineY + index * lineHeight)
        }
    }
}
