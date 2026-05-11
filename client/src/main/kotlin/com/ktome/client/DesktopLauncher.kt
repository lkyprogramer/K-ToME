package com.ktome.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.ktome.client.build.BuildInfo
import com.ktome.client.screen.DesktopLauncherTitleFormatter
import com.ktome.game.contentpack.ContentPackSelection
import java.nio.file.Path

private const val windowWidth = 1280
private const val windowHeight = 800
private const val windowX = 80
private const val windowY = 60
private const val contentPackRootsProperty: String = "ktome.contentPackRoots"

fun main() {
    BuildInfo.initialize()
    val configuration = Lwjgl3ApplicationConfiguration().apply {
        setTitle(DesktopLauncherTitleFormatter.format())
        setWindowedMode(windowWidth, windowHeight)
        setWindowPosition(windowX, windowY)
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
