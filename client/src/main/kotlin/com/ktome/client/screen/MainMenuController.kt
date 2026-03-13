package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputSource

internal sealed interface MainMenuAction {
    data object StartNewGame : MainMenuAction

    data object ContinueGame : MainMenuAction

    data object ExitGame : MainMenuAction
}

internal class MainMenuController(
    private val input: InputSource = GdxInputSource,
) {
    private var selectedIndex: Int = 0

    fun selectedIndex(): Int = selectedIndex

    fun entries(hasSave: Boolean): List<MenuEntry> =
        listOf(
            MenuEntry("New Game", enabled = true),
            MenuEntry("Continue", enabled = hasSave),
            MenuEntry("Exit", enabled = true),
        )

    fun pollAction(hasSave: Boolean): MainMenuAction? {
        val entries = entries(hasSave)
        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            selectedIndex = (selectedIndex - 1).floorMod(entries.size)
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            selectedIndex = (selectedIndex + 1).floorMod(entries.size)
        }
        if (input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)) {
            val selected = entries[selectedIndex]
            if (!selected.enabled) {
                return null
            }
            return when (selectedIndex) {
                0 -> MainMenuAction.StartNewGame
                1 -> MainMenuAction.ContinueGame
                else -> MainMenuAction.ExitGame
            }
        }
        return null
    }

    internal data class MenuEntry(
        val label: String,
        val enabled: Boolean,
    )
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
