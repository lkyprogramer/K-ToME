package com.ktome.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.ktome.client.screen.FoundationGameScreen

private const val windowTitle = "K-ToME Foundation 1-1.0"
private const val windowWidth = 1280
private const val windowHeight = 800

fun main() {
    val configuration = Lwjgl3ApplicationConfiguration().apply {
        setTitle("$windowTitle | Move with QWEASDZC, arrows, Home/End/PgUp/PgDn or numpad")
        setWindowedMode(windowWidth, windowHeight)
        useVsync(true)
        setForegroundFPS(60)
        setResizable(true)
    }

    Lwjgl3Application(KtomeClientApp(), configuration)
}

private class KtomeClientApp : Game() {
    override fun create() {
        setScreen(FoundationGameScreen())
    }
}
