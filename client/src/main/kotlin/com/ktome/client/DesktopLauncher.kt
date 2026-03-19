package com.ktome.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

private const val windowTitle = "K-ToME v0.1.0"
private const val windowWidth = 1280
private const val windowHeight = 800

fun main() {
    val configuration = Lwjgl3ApplicationConfiguration().apply {
        setTitle("$windowTitle | arrows/numpad move, Enter stairs, x inspect, Ctrl+S save")
        setWindowedMode(windowWidth, windowHeight)
        useVsync(true)
        setForegroundFPS(60)
        setResizable(true)
    }

    Lwjgl3Application(GameApp(), configuration)
}
