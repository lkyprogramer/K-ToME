package com.ktome.client.screen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainMenuSummaryModelTest {
    @Test
    fun `primary action follows continue availability without promoting validation mode`() {
        assertEquals(
            MainMenuPrimaryAction.QUICK_START,
            MainMenuFocusPolicy.primaryAction(entries(ContinueAvailability.Absent), ContinueAvailability.Absent),
        )
        assertEquals(
            MainMenuPrimaryAction.CONTINUE,
            MainMenuFocusPolicy.primaryAction(entries(ContinueAvailability.Available), ContinueAvailability.Available),
        )
        val unavailable =
            ContinueAvailability.Unavailable(
                reasonCode = ContinueUnavailableReasonCode.CORRUPTED,
                savePath = "/tmp/run-save.json",
            )
        assertEquals(
            MainMenuPrimaryAction.QUICK_START,
            MainMenuFocusPolicy.primaryAction(entries(unavailable), unavailable),
        )
    }

    @Test
    fun `primary action mirrors actual validation fallback`() {
        val entries =
            listOf(
                MainMenuController.MenuEntry(
                    action = MainMenuAction.QuickStart,
                    labelKey = "ui.menu.action.quick-start",
                    state = MainMenuController.MenuEntryState(enabled = false, focusable = true),
                ),
                MainMenuController.MenuEntry(
                    action = MainMenuAction.ValidationMode,
                    labelKey = "ui.menu.action.validation",
                    state = MainMenuController.MenuEntryState(enabled = true, focusable = true),
                ),
            )

        assertEquals(
            MainMenuPrimaryAction.VALIDATION_MODE,
            MainMenuFocusPolicy.primaryAction(entries, ContinueAvailability.Absent),
        )
    }

    private fun entries(continueAvailability: ContinueAvailability): List<MainMenuController.MenuEntry> =
        listOf(
            MainMenuController.MenuEntry(
                action = MainMenuAction.QuickStart,
                labelKey = "ui.menu.action.quick-start",
                state = MainMenuController.MenuEntryState(enabled = true, focusable = true),
            ),
            MainMenuController.MenuEntry(
                action = MainMenuAction.Continue,
                labelKey = "ui.menu.action.continue",
                state =
                    MainMenuController.MenuEntryState(
                        enabled = continueAvailability.isAvailable,
                        focusable = continueAvailability !is ContinueAvailability.Absent,
                    ),
            ),
            MainMenuController.MenuEntry(
                action = MainMenuAction.ValidationMode,
                labelKey = "ui.menu.action.validation",
                state = MainMenuController.MenuEntryState(enabled = true, focusable = true),
            ),
        )
}
