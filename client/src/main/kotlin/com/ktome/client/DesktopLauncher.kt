package com.ktome.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.ktome.game.contentpack.ContentPackSelection
import java.nio.file.Path

private const val windowTitle = "K-ToME v0.1.0"
private const val windowWidth = 1280
private const val windowHeight = 800
private const val contentPackRootsProperty: String = "ktome.contentPackRoots"

fun main() {
    val configuration = Lwjgl3ApplicationConfiguration().apply {
        setTitle("$windowTitle | arrows/numpad move, Enter stairs, x inspect, Ctrl+S save")
        setWindowedMode(windowWidth, windowHeight)
        useVsync(true)
        setForegroundFPS(60)
        setResizable(true)
    }

    Lwjgl3Application(
        GameApp(contentPackSelection = contentPackSelectionFromSystemProperty()),
        configuration,
    )
}

private fun contentPackSelectionFromSystemProperty(): ContentPackSelection {
    val roots =
        System.getProperty(contentPackRootsProperty)
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { rawPath -> Path.of(rawPath).toAbsolutePath().normalize() }
    return if (roots.isEmpty()) {
        ContentPackSelection.EMPTY
    } else {
        ContentPackSelection(
            activePackRoots = roots,
            availablePackRoots = roots,
        )
    }
}
