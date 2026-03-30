package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputSource
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.game.PlayerCreationSelection
import com.ktome.game.PlayerCreationState

internal sealed interface MainMenuAction {
    data object StartNewGame : MainMenuAction

    data object ContinueGame : MainMenuAction

    data object ExitGame : MainMenuAction

    data object ToggleLocale : MainMenuAction
}

internal enum class PlayerCreationFocus {
    PROFESSION,
    RACE,
}

internal data class MainMenuPollResult(
    val action: MainMenuAction? = null,
    val selection: PlayerCreationSelection,
    val focusedAxis: PlayerCreationFocus,
    val selectionChanged: Boolean = false,
    val professionChanged: Boolean = false,
    val raceChanged: Boolean = false,
    val localeToggled: Boolean = false,
    val rejected: Boolean = false,
)

internal class MainMenuController(
    private val input: InputSource = GdxInputSource,
    playerCreationState: PlayerCreationState,
) {
    private val playerCreationState = playerCreationState
    private val browseableProfessionOptions =
        playerCreationState.professionOptions
            .filter { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }
            .ifEmpty { playerCreationState.professionOptions }
    private val browseableRaceOptions =
        playerCreationState.raceOptions
            .filter { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }
            .ifEmpty { playerCreationState.raceOptions }
    private var selectedIndex: Int = 0
    private var focusedAxis: PlayerCreationFocus = PlayerCreationFocus.PROFESSION
    private var selectedProfessionIndex: Int = browseableProfessionOptions.indexOfFirst { option -> option.id == playerCreationState.selection.professionId }.takeIf { it >= 0 } ?: 0
    private var selectedRaceIndex: Int = browseableRaceOptions.indexOfFirst { option -> option.id == playerCreationState.selection.raceId }.takeIf { it >= 0 } ?: 0

    fun selectedIndex(): Int = selectedIndex

    fun currentSelection(): PlayerCreationSelection =
        PlayerCreationSelection(
            professionId = currentProfessionOption().id,
            raceId = currentRaceOption().id,
        )

    fun currentFocus(): PlayerCreationFocus = focusedAxis

    fun currentProfessionOption() = professionOptions()[selectedProfessionIndex]

    fun currentRaceOption() = raceOptions()[selectedRaceIndex]

    fun entries(hasSave: Boolean): List<MenuEntry> =
        listOf(
            MenuEntry("ui.menu.new_game", enabled = canStartNewGame()),
            MenuEntry("ui.menu.continue", enabled = hasSave),
            MenuEntry("ui.menu.exit", enabled = true),
        )

    fun pollAction(hasSave: Boolean): MainMenuPollResult {
        val entries = entries(hasSave)
        if (input.isKeyJustPressed(Keys.L)) {
            return MainMenuPollResult(
                action = MainMenuAction.ToggleLocale,
                selection = currentSelection(),
                focusedAxis = focusedAxis,
                localeToggled = true,
            )
        }
        var selectionChanged = false
        var professionChanged = false
        var raceChanged = false
        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            selectedIndex = (selectedIndex - 1).floorMod(entries.size)
            selectionChanged = true
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            selectedIndex = (selectedIndex + 1).floorMod(entries.size)
            selectionChanged = true
        }
        if (input.isKeyJustPressed(Keys.LEFT) || input.isKeyJustPressed(Keys.A)) {
            selectedProfessionIndex = (selectedProfessionIndex - 1).floorMod(professionOptions().size)
            focusedAxis = PlayerCreationFocus.PROFESSION
            professionChanged = true
        }
        if (input.isKeyJustPressed(Keys.RIGHT) || input.isKeyJustPressed(Keys.D)) {
            selectedProfessionIndex = (selectedProfessionIndex + 1).floorMod(professionOptions().size)
            focusedAxis = PlayerCreationFocus.PROFESSION
            professionChanged = true
        }
        if (input.isKeyJustPressed(Keys.Q)) {
            selectedRaceIndex = (selectedRaceIndex - 1).floorMod(raceOptions().size)
            focusedAxis = PlayerCreationFocus.RACE
            raceChanged = true
        }
        if (input.isKeyJustPressed(Keys.E)) {
            selectedRaceIndex = (selectedRaceIndex + 1).floorMod(raceOptions().size)
            focusedAxis = PlayerCreationFocus.RACE
            raceChanged = true
        }
        if (input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)) {
            val selected = entries[selectedIndex]
            if (!selected.enabled) {
                return MainMenuPollResult(
                    selection = currentSelection(),
                    focusedAxis = focusedAxis,
                    selectionChanged = selectionChanged || professionChanged || raceChanged,
                    professionChanged = professionChanged,
                    raceChanged = raceChanged,
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
                selection = currentSelection(),
                focusedAxis = focusedAxis,
                selectionChanged = selectionChanged || professionChanged || raceChanged,
                professionChanged = professionChanged,
                raceChanged = raceChanged,
            )
        }
        return MainMenuPollResult(
            selection = currentSelection(),
            focusedAxis = focusedAxis,
            selectionChanged = selectionChanged || professionChanged || raceChanged,
            professionChanged = professionChanged,
            raceChanged = raceChanged,
        )
    }

    private fun canStartNewGame(): Boolean =
        currentProfessionOption().playabilityState == ClassPlayabilityState.PLAYABLE &&
            currentRaceOption().playabilityState == ClassPlayabilityState.PLAYABLE

    private fun professionOptions() = browseableProfessionOptions

    private fun raceOptions() = browseableRaceOptions

    internal data class MenuEntry(
        val labelKey: String,
        val enabled: Boolean,
    )
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
