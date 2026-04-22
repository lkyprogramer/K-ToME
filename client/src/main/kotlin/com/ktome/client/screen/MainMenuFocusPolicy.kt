package com.ktome.client.screen

internal object MainMenuFocusPolicy {
    fun primaryAction(
        entries: List<MainMenuController.MenuEntry>,
        continueAvailability: ContinueAvailability,
    ): MainMenuPrimaryAction {
        val index = initialIndex(entries, continueAvailability)
        return entries.getOrNull(index)?.action?.toPrimaryAction() ?: MainMenuPrimaryAction.QUICK_START
    }

    fun initialIndex(
        entries: List<MainMenuController.MenuEntry>,
        continueAvailability: ContinueAvailability,
    ): Int {
        val preferredAction = preferredMenuAction(continueAvailability)
        return entries
            .indexOfFirst { entry -> entry.action == preferredAction && entry.enabled && entry.focusable }
            .takeIf { index -> index >= 0 }
            ?: entries.indexOfFirst { entry -> entry.enabled && entry.focusable }.takeIf { index -> index >= 0 }
            ?: entries.indexOfFirst { entry -> entry.focusable }.takeIf { index -> index >= 0 }
            ?: 0
    }

    private fun preferredMenuAction(continueAvailability: ContinueAvailability): MainMenuAction =
        if (continueAvailability.isAvailable) {
            MainMenuAction.Continue
        } else {
            MainMenuAction.QuickStart
        }

    private fun MainMenuAction.toPrimaryAction(): MainMenuPrimaryAction =
        when (this) {
            MainMenuAction.QuickStart -> MainMenuPrimaryAction.QUICK_START
            MainMenuAction.Continue -> MainMenuPrimaryAction.CONTINUE
            MainMenuAction.ValidationMode -> MainMenuPrimaryAction.VALIDATION_MODE
            MainMenuAction.ExitGame -> MainMenuPrimaryAction.EXIT_GAME
            MainMenuAction.CopyContinueErrorDetail,
            MainMenuAction.ToggleLocale,
            -> error("Non-entry action cannot be the main menu primary action")
        }
}
