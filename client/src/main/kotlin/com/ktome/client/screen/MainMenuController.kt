package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputSource

internal sealed interface MainMenuAction {
    data object StartNewGame : MainMenuAction

    data object ContinueGame : MainMenuAction

    data object ExitGame : MainMenuAction

    data object ToggleLocale : MainMenuAction
}

internal data class MainMenuPollResult(
    val action: MainMenuAction? = null,
    val selectedProfessionId: String,
    val selectionChanged: Boolean = false,
    val professionChanged: Boolean = false,
    val localeToggled: Boolean = false,
    val rejected: Boolean = false,
)

internal class MainMenuController(
    private val input: InputSource = GdxInputSource,
    availableProfessionIds: List<String>,
    initialProfessionId: String,
) {
    private val professionIds: List<String> = availableProfessionIds.distinct().also { ids ->
        require(ids.isNotEmpty()) { "Main menu requires at least one available profession." }
    }
    private var selectedIndex: Int = 0
    private var selectedProfessionIndex: Int = professionIds.indexOf(initialProfessionId).takeIf { it >= 0 } ?: 0

    fun selectedIndex(): Int = selectedIndex

    fun currentProfessionId(): String = professionIds[selectedProfessionIndex]

    fun entries(hasSave: Boolean): List<MenuEntry> =
        listOf(
            MenuEntry("ui.menu.new_game", enabled = true),
            MenuEntry("ui.menu.continue", enabled = hasSave),
            MenuEntry("ui.menu.exit", enabled = true),
        )

    fun pollAction(hasSave: Boolean): MainMenuPollResult {
        val entries = entries(hasSave)
        if (input.isKeyJustPressed(Keys.L)) {
            return MainMenuPollResult(
                action = MainMenuAction.ToggleLocale,
                selectedProfessionId = currentProfessionId(),
                localeToggled = true,
            )
        }
        var selectionChanged = false
        var professionChanged = false
        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            selectedIndex = (selectedIndex - 1).floorMod(entries.size)
            selectionChanged = true
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            selectedIndex = (selectedIndex + 1).floorMod(entries.size)
            selectionChanged = true
        }
        if (input.isKeyJustPressed(Keys.LEFT) || input.isKeyJustPressed(Keys.A)) {
            selectedProfessionIndex = (selectedProfessionIndex - 1).floorMod(professionIds.size)
            professionChanged = true
        }
        if (input.isKeyJustPressed(Keys.RIGHT) || input.isKeyJustPressed(Keys.D)) {
            selectedProfessionIndex = (selectedProfessionIndex + 1).floorMod(professionIds.size)
            professionChanged = true
        }
        if (input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)) {
            val selected = entries[selectedIndex]
            if (!selected.enabled) {
                return MainMenuPollResult(
                    selectedProfessionId = currentProfessionId(),
                    selectionChanged = selectionChanged || professionChanged,
                    professionChanged = professionChanged,
                    rejected = true,
                )
            }
            return MainMenuPollResult(
                action =
                    when (selectedIndex) {
                        0 -> MainMenuAction.StartNewGame
                        1 -> MainMenuAction.ContinueGame
                        else -> MainMenuAction.ExitGame
                    },
                selectedProfessionId = currentProfessionId(),
                selectionChanged = selectionChanged || professionChanged,
                professionChanged = professionChanged,
            )
        }
        return MainMenuPollResult(
            selectedProfessionId = currentProfessionId(),
            selectionChanged = selectionChanged || professionChanged,
            professionChanged = professionChanged,
        )
    }

    internal data class MenuEntry(
        val labelKey: String,
        val enabled: Boolean,
    )
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
