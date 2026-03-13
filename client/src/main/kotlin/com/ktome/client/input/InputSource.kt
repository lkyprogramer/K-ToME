package com.ktome.client.input

import com.badlogic.gdx.Gdx

interface InputSource {
    fun isKeyJustPressed(keycode: Int): Boolean

    fun isKeyPressed(keycode: Int): Boolean
}

object GdxInputSource : InputSource {
    override fun isKeyJustPressed(keycode: Int): Boolean = Gdx.input.isKeyJustPressed(keycode)

    override fun isKeyPressed(keycode: Int): Boolean = Gdx.input.isKeyPressed(keycode)
}
