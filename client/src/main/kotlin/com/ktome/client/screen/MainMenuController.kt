package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputSource
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.game.ProfessionSelectionOption

internal sealed interface MainMenuAction {
    data object StartNewGame : MainMenuAction

    data object ContinueGame : MainMenuAction

    data object ExitGame : MainMenuAction

    data object ToggleLocale : MainMenuAction
}

internal data class MainMenuPollResult(
    val action: MainMenuAction? = null,
    val selectedProfessionId: String,
    val selectedRaceId: String,
    val selectionChanged: Boolean = false,
    val professionChanged: Boolean = false,
    val raceChanged: Boolean = false,
    val localeToggled: Boolean = false,
    val rejected: Boolean = false,
)

internal class MainMenuController(
    private val input: InputSource = GdxInputSource,
    availableProfessionIds: List<String>,
    availableRaceIds: List<String>,
    initialProfessionId: String,
    initialRaceId: String,
    professionSelections: List<ProfessionSelectionOption> = emptyList(),
) {
    private val professionIds: List<String> = availableProfessionIds.distinct().also { ids ->
        require(ids.isNotEmpty()) { "Main menu requires at least one available profession." }
    }
    private val raceIds: List<String> = availableRaceIds.distinct().also { ids ->
        require(ids.isNotEmpty()) { "Main menu requires at least one available race." }
    }
    private val professionSelectionStates: Map<String, ClassPlayabilityState> =
        professionSelections
            .distinctBy(ProfessionSelectionOption::id)
            .associate { selection -> selection.id to selection.playabilityState }
    private var selectedIndex: Int = 0
    private var selectedProfessionIndex: Int = professionIds.indexOf(initialProfessionId).takeIf { it >= 0 } ?: 0
    private var selectedRaceIndex: Int = raceIds.indexOf(initialRaceId).takeIf { it >= 0 } ?: 0

    fun selectedIndex(): Int = selectedIndex

    fun currentProfessionId(): String = professionIds[selectedProfessionIndex]

    fun currentRaceId(): String = raceIds[selectedRaceIndex]

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
                selectedProfessionId = currentProfessionId(),
                selectedRaceId = currentRaceId(),
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
            selectedProfessionIndex = (selectedProfessionIndex - 1).floorMod(professionIds.size)
            professionChanged = true
        }
        if (input.isKeyJustPressed(Keys.RIGHT) || input.isKeyJustPressed(Keys.D)) {
            selectedProfessionIndex = (selectedProfessionIndex + 1).floorMod(professionIds.size)
            professionChanged = true
        }
        if (input.isKeyJustPressed(Keys.Q)) {
            selectedRaceIndex = (selectedRaceIndex - 1).floorMod(raceIds.size)
            raceChanged = true
        }
        if (input.isKeyJustPressed(Keys.E)) {
            selectedRaceIndex = (selectedRaceIndex + 1).floorMod(raceIds.size)
            raceChanged = true
        }
        if (input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)) {
            val selected = entries[selectedIndex]
            if (!selected.enabled) {
                return MainMenuPollResult(
                    selectedProfessionId = currentProfessionId(),
                    selectedRaceId = currentRaceId(),
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
                selectedProfessionId = currentProfessionId(),
                selectedRaceId = currentRaceId(),
                selectionChanged = selectionChanged || professionChanged || raceChanged,
                professionChanged = professionChanged,
                raceChanged = raceChanged,
            )
        }
        return MainMenuPollResult(
            selectedProfessionId = currentProfessionId(),
            selectedRaceId = currentRaceId(),
            selectionChanged = selectionChanged || professionChanged || raceChanged,
            professionChanged = professionChanged,
            raceChanged = raceChanged,
        )
    }

    private fun canStartNewGame(): Boolean = selectedProfessionState() == ClassPlayabilityState.PLAYABLE

    private fun selectedProfessionState(): ClassPlayabilityState =
        professionSelectionStates[currentProfessionId()] ?: ClassPlayabilityState.PLAYABLE

    internal data class MenuEntry(
        val labelKey: String,
        val enabled: Boolean,
    )
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
