package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputSource
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.game.PlayerCreationSelection
import com.ktome.game.PlayerCreationState

internal sealed interface MainMenuAction {
    data object QuickStart : MainMenuAction

    data object Continue : MainMenuAction

    data object CopyContinueErrorDetail : MainMenuAction

    data object ValidationMode : MainMenuAction

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
    initialContinueAvailability: ContinueAvailability = ContinueAvailability.Absent,
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
    private var focusedAxis: PlayerCreationFocus = PlayerCreationFocus.PROFESSION
    private var selectedProfessionIndex: Int = browseableProfessionOptions.indexOfFirst { option -> option.id == playerCreationState.selection.professionId }.takeIf { it >= 0 } ?: 0
    private var selectedRaceIndex: Int = browseableRaceOptions.indexOfFirst { option -> option.id == playerCreationState.selection.raceId }.takeIf { it >= 0 } ?: 0
    private var selectedIndex: Int = MainMenuFocusPolicy.initialIndex(entries(initialContinueAvailability), initialContinueAvailability)

    fun selectedIndex(): Int = selectedIndex

    fun currentSelection(): PlayerCreationSelection =
        PlayerCreationSelection(
            professionId = currentProfessionOption().id,
            raceId = currentRaceOption().id,
        )

    fun currentFocus(): PlayerCreationFocus = focusedAxis

    fun currentProfessionOption() = professionOptions()[selectedProfessionIndex]

    fun currentRaceOption() = raceOptions()[selectedRaceIndex]

    fun entries(continueAvailability: ContinueAvailability): List<MenuEntry> =
        listOf(
            MenuEntry(
                action = MainMenuAction.QuickStart,
                labelKey = "ui.menu.action.quick-start",
                state = MenuEntryState(enabled = canStartNewGame(), focusable = true),
            ),
            MenuEntry(
                action = MainMenuAction.Continue,
                labelKey = "ui.menu.action.continue",
                state =
                    MenuEntryState(
                        enabled = continueAvailability.isAvailable,
                        focusable = continueAvailability !is ContinueAvailability.Absent,
                        disabledReasonKey = (continueAvailability as? ContinueAvailability.Unavailable)?.reasonKey,
                    ),
            ),
            MenuEntry(
                action = MainMenuAction.ValidationMode,
                labelKey = "ui.menu.action.validation",
                state = MenuEntryState(enabled = true, focusable = true),
            ),
            MenuEntry(
                action = MainMenuAction.ExitGame,
                labelKey = "ui.menu.exit",
                state = MenuEntryState(enabled = true, focusable = true),
            ),
        )

    fun pollAction(continueAvailability: ContinueAvailability): MainMenuPollResult {
        val entries = entries(continueAvailability)
        if (entries.getOrNull(selectedIndex)?.focusable != true) {
            selectedIndex = MainMenuFocusPolicy.initialIndex(entries, continueAvailability)
        }
        if (input.isKeyJustPressed(Keys.L)) {
            return MainMenuPollResult(
                action = MainMenuAction.ToggleLocale,
                selection = currentSelection(),
                focusedAxis = focusedAxis,
                localeToggled = true,
            )
        }
        if (continueAvailability is ContinueAvailability.Unavailable && input.isKeyJustPressed(Keys.C)) {
            return MainMenuPollResult(
                action = MainMenuAction.CopyContinueErrorDetail,
                selection = currentSelection(),
                focusedAxis = focusedAxis,
            )
        }
        var selectionChanged = false
        var professionChanged = false
        var raceChanged = false
        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            selectedIndex = nextFocusableIndex(entries, delta = -1)
            selectionChanged = true
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            selectedIndex = nextFocusableIndex(entries, delta = 1)
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
                action = selected.action,
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

    private fun nextFocusableIndex(
        entries: List<MenuEntry>,
        delta: Int,
    ): Int {
        var candidate = selectedIndex
        repeat(entries.size) {
            candidate = (candidate + delta).floorMod(entries.size)
            if (entries[candidate].focusable) {
                return candidate
            }
        }
        return selectedIndex
    }

    internal data class MenuEntryState(
        val enabled: Boolean,
        val focusable: Boolean,
        val disabledReasonKey: String? = null,
    )

    internal data class MenuEntry(
        val action: MainMenuAction,
        val labelKey: String,
        val state: MenuEntryState,
    ) {
        val enabled: Boolean get() = state.enabled
        val focusable: Boolean get() = state.focusable
        val disabledReasonKey: String? get() = state.disabledReasonKey
    }
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
