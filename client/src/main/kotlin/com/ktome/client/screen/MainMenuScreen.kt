package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.GameApp
import com.ktome.client.audio.AudioRouter
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputSource
import com.ktome.client.render.KtomeFonts
import com.ktome.client.text.LocalizedTextSeparator
import com.ktome.client.text.joinLocalizedKeys
import com.ktome.client.ui.creation.PlayerCreationPanel
import com.ktome.client.ui.creation.PlayerCreationSectionModel
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.game.PlayerCreationState
import com.ktome.game.i18n.Localizer

internal const val menuWidth = 960f
internal const val menuHeight = 540f
internal const val MAIN_MENU_TEXT_X = 80f
internal const val MAIN_MENU_TITLE_Y = 472f
internal const val MAIN_MENU_SUBTITLE_Y = 438f
internal const val MAIN_MENU_PANEL_TOP_Y = 370f
internal const val MAIN_MENU_BUILD_SUMMARY_X = 438f
internal const val MAIN_MENU_FOOTER_LINE_HEIGHT = 24f
internal const val MAIN_MENU_CLASS_ENTRY_BASE_OFFSET_Y = 132f
internal const val MAIN_MENU_CLASS_ENTRY_STEP_Y = 32f
internal const val MAIN_MENU_PLAYER_CREATION_SECTION_BOTTOM_Y = MAIN_MENU_PANEL_TOP_Y - 108f
internal const val MAIN_MENU_HELP_TOP_Y = MAIN_MENU_PANEL_TOP_Y - MAIN_MENU_CLASS_ENTRY_BASE_OFFSET_Y - 12f
internal const val MAIN_MENU_HELP_BLOCK_STEP_Y = MAIN_MENU_FOOTER_LINE_HEIGHT + 4f
internal const val MAIN_MENU_HELP_MAX_WIDTH = menuWidth - MAIN_MENU_BUILD_SUMMARY_X - 40f
internal const val MAIN_MENU_FOOTER_LANGUAGE_DEFAULT_Y = 100f
internal const val MAIN_MENU_FOOTER_CONTROLS_DEFAULT_Y = 72f
internal const val MAIN_MENU_FOOTER_NOTICE_DEFAULT_Y = 42f
internal const val MAIN_MENU_ENTRY_TO_FOOTER_CLEARANCE_Y = MAIN_MENU_FOOTER_LINE_HEIGHT + 4f
internal const val MAIN_MENU_FOOTER_STACK_CLEARANCE_Y = MAIN_MENU_FOOTER_LINE_HEIGHT + 4f

internal fun mainMenuClassEntryY(index: Int): Float =
    MAIN_MENU_PANEL_TOP_Y - MAIN_MENU_CLASS_ENTRY_BASE_OFFSET_Y - index * MAIN_MENU_CLASS_ENTRY_STEP_Y

internal fun mainMenuLastEntryY(entryCount: Int): Float = mainMenuClassEntryY((entryCount - 1).coerceAtLeast(0))

internal fun mainMenuHelpLineY(index: Int): Float = MAIN_MENU_HELP_TOP_Y - index * MAIN_MENU_HELP_BLOCK_STEP_Y

internal fun mainMenuFooterLanguageY(entryCount: Int): Float =
    minOf(
        MAIN_MENU_FOOTER_LANGUAGE_DEFAULT_Y,
        mainMenuLastEntryY(entryCount) - MAIN_MENU_ENTRY_TO_FOOTER_CLEARANCE_Y,
    )

internal fun mainMenuFooterControlsY(entryCount: Int): Float =
    minOf(
        MAIN_MENU_FOOTER_CONTROLS_DEFAULT_Y,
        mainMenuFooterLanguageY(entryCount) - MAIN_MENU_FOOTER_STACK_CLEARANCE_Y,
    )

internal fun mainMenuFooterNoticeY(entryCount: Int): Float =
    minOf(
        MAIN_MENU_FOOTER_NOTICE_DEFAULT_Y,
        mainMenuFooterControlsY(entryCount) - MAIN_MENU_FOOTER_STACK_CLEARANCE_Y,
    )

internal data class MainMenuTextSnapshot(
    val title: String,
    val subtitle: String,
    val profession: String,
    val professionState: String,
    val professionDescription: String,
    val professionResourceHint: String,
    val professionNote: String? = null,
    val race: String,
    val raceState: String,
    val raceDescription: String,
    val raceNote: String? = null,
    val entries: List<String>,
    val buildSummary: List<MainMenuBuildSummaryTextLine>,
    val helpLines: List<String>,
    val continueDisabledReason: String? = null,
    val continueDisabledDetail: String? = null,
    val language: String,
    val controls: String,
    val notice: String?,
) {
    val footerNotice: String?
        get() = notice ?: continueDisabledReason ?: continueDisabledDetail
}

internal class MainMenuScreen(
    private val app: GameApp,
    private val continueAvailability: ContinueAvailability,
    playerCreationState: PlayerCreationState,
    notice: String? = null,
    inputSource: InputSource = GdxInputSource,
    private val renderEnabled: Boolean = true,
    private val copyToClipboard: (String) -> Boolean = ::copyTextToClipboard,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private val viewport = FitViewport(menuWidth, menuHeight)
    private val controller =
        MainMenuController(
            input = inputSource,
            playerCreationState = playerCreationState,
            initialContinueAvailability = continueAvailability,
        )
    private val playerCreationState = playerCreationState
    private val professionUnavailableNameKeys =
        playerCreationState.professionOptions
            .filter { option -> option.playabilityState == ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE }
            .map { option -> option.displayNameKey }
    private val raceUnavailableNameKeys =
        playerCreationState.raceOptions
            .filter { option -> option.playabilityState == ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE }
            .map { option -> option.displayNameKey }
    private val audioRouter: AudioRouter? = app.audioRouterOrNull()
    private var menuNotice: String? = notice
    private var chrome: StandaloneScreenChrome? = null

    override fun show() {
        audioRouter?.onMenuShown()
        if (!renderEnabled) {
            return
        }
        ensureResources()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun render(delta: Float) {
        val poll = controller.pollAction(continueAvailability)
        if (poll.professionChanged || poll.raceChanged) {
            app.rememberPlayerCreationSelection(poll.selection)
        }
        audioRouter?.onMenuInteraction(
            selectionChanged = poll.selectionChanged,
            localeToggled = poll.localeToggled,
            accepted = poll.action != null && poll.action != MainMenuAction.ToggleLocale,
            rejected = poll.rejected,
        )
        when (poll.action) {
            MainMenuAction.QuickStart -> {
                app.startNewGame(poll.selection)
                return
            }
            MainMenuAction.Continue -> {
                app.continueGame()
                return
            }
            MainMenuAction.CopyContinueErrorDetail -> {
                copyContinueErrorDetail()
            }
            MainMenuAction.ValidationMode -> {
                app.showValidationSetup()
                return
            }
            MainMenuAction.ExitGame -> {
                Gdx.app.exit()
                return
            }
            MainMenuAction.ToggleLocale -> {
                app.cycleLocale()
                app.showMainMenu(saveCurrent = false, notice = menuNotice)
                return
            }
            null -> Unit
        }
        if (!renderEnabled) {
            return
        }
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(font)
        val text = textSnapshot()
        val layout = DarkStandaloneScreenLayout.mainMenu()

        val surfaceBase = UiDesignTokens.color.surface.base.color()
        ScreenUtils.clear(surfaceBase.r, surfaceBase.g, surfaceBase.b, surfaceBase.a)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        val selectedIndex = controller.selectedIndex()
        val entries = controller.entries(continueAvailability)
        val footerLanguageY = mainMenuFooterLanguageY(entries.size)
        val footerControlsY = mainMenuFooterControlsY(entries.size)
        val footerNoticeY = mainMenuFooterNoticeY(entries.size)
        val playerCreationPanel =
            PlayerCreationPanel.build(
                professionSection =
                    PlayerCreationSectionModel(
                        title = text.profession,
                        state = text.professionState,
                        description = text.professionDescription,
                        detail = text.professionResourceHint,
                        note = text.professionNote,
                    ),
                raceSection =
                    PlayerCreationSectionModel(
                        title = text.race,
                        state = text.raceState,
                        description = text.raceDescription,
                        note = text.raceNote,
                    ),
                focusedAxis = controller.currentFocus(),
                entries = entries,
                selectedIndex = selectedIndex,
                localizedEntryLabels = text.entries,
            )

        batch.begin()
        requireNotNull(chrome).draw(
            batch,
            StandaloneChromeRequest(
                layout = layout,
                detailAreaMode =
                    if (text.footerNotice == null) {
                        StandaloneDetailAreaMode.HIDDEN
                    } else {
                        StandaloneDetailAreaMode.VISIBLE
                    },
            ),
        )
        font.color = UiDesignTokens.color.quality.rare.color()
        font.draw(batch, text.title, MAIN_MENU_TEXT_X, MAIN_MENU_TITLE_Y)
        font.color = UiDesignTokens.color.text.secondary.color()
        font.draw(batch, text.subtitle, MAIN_MENU_TEXT_X, MAIN_MENU_SUBTITLE_Y)
        text.buildSummary.forEachIndexed { index, line ->
            font.color =
                if (line.disabled) {
                    UiDesignTokens.color.text.disabled.color()
                } else {
                    UiDesignTokens.color.text.secondary.color()
                }
            font.draw(batch, line.text, MAIN_MENU_BUILD_SUMMARY_X, MAIN_MENU_TITLE_Y - index * MAIN_MENU_FOOTER_LINE_HEIGHT)
        }
        PlayerCreationPanel.render(
            batch = batch,
            font = font,
            model = playerCreationPanel,
            professionStateColor = selectionStateColor(controller.currentProfessionOption().playabilityState),
            raceStateColor = selectionStateColor(controller.currentRaceOption().playabilityState),
            x = MAIN_MENU_TEXT_X,
            topY = MAIN_MENU_PANEL_TOP_Y,
        )

        text.helpLines.forEachIndexed { index, line ->
            font.color = UiDesignTokens.color.text.secondary.color()
            font.draw(batch, line, MAIN_MENU_BUILD_SUMMARY_X, mainMenuHelpLineY(index))
        }

        font.color = UiDesignTokens.color.quality.rare.color()
        font.draw(batch, text.language, MAIN_MENU_TEXT_X, footerLanguageY)
        font.color = UiDesignTokens.color.text.disabled.color()
        font.draw(batch, text.controls, MAIN_MENU_TEXT_X, footerControlsY)
        text.footerNotice?.let { message ->
            font.color = UiDesignTokens.color.status.badge.turns.color()
            font.draw(batch, message, MAIN_MENU_TEXT_X, footerNoticeY)
        }
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        if (renderEnabled) {
            viewport.update(width, height, true)
        }
    }

    override fun dispose() {
        font?.dispose()
        font = null
        chrome?.dispose()
        chrome = null
        batch?.dispose()
        batch = null
    }

    private fun ensureResources() {
        if (batch == null) {
            batch = SpriteBatch()
        }
        if (font == null) {
            font = KtomeFonts.createUiFont(size = 24)
        }
        if (chrome == null) {
            chrome = StandaloneScreenChrome()
        }
    }

    internal fun textSnapshot(): MainMenuTextSnapshot =
        app.localizer().let { localizer ->
            val professionOption = controller.currentProfessionOption()
            val raceOption = controller.currentRaceOption()
            val unavailable = continueAvailability as? ContinueAvailability.Unavailable
            val entries = controller.entries(continueAvailability)
            val summary = summaryModel(localizer)
            MainMenuTextSnapshot(
                title = app.text("ui.menu.title"),
                subtitle = app.text("ui.menu.subtitle"),
                profession = selectionLabel(localizer, "ui.menu.profession", professionOption.displayNameKey),
                professionState = selectionStateText(localizer, professionOption.playabilityState),
                professionDescription = app.text(professionOption.descriptionKey),
                professionResourceHint = resourceHintText(localizer, professionOption.resourceHintKey),
                professionNote =
                    discoveredUnavailableNoteText(
                        localizer = localizer,
                        noteKey = "ui.menu.profession_discovered_unavailable",
                        optionNameKeys = professionUnavailableNameKeys,
                    ),
                race = selectionLabel(localizer, "ui.menu.race", raceOption.displayNameKey),
                raceState = selectionStateText(localizer, raceOption.playabilityState),
                raceDescription = app.text(raceOption.descriptionKey),
                raceNote =
                    discoveredUnavailableNoteText(
                        localizer = localizer,
                        noteKey = "ui.menu.race_discovered_unavailable",
                        optionNameKeys = raceUnavailableNameKeys,
                    ),
                entries = entries.map { entry -> app.text(entry.labelKey) },
                buildSummary = summary.localizedBuildSummary(localizer),
                helpLines = summary.helpLines,
                continueDisabledReason = unavailable?.let { reason -> app.text(reason.reasonKey) },
                continueDisabledDetail =
                    unavailable
                        ?.takeIf { reason -> reason.reasonCode == ContinueUnavailableReasonCode.CORRUPTED }
                        ?.let { app.text("ui.menu.continue.corrupted.detail") },
                language = app.text("ui.menu.language", "value" to localizer.localeLabel()),
                controls = controlsText(unavailable),
                notice = menuNotice?.takeIf(String::isNotBlank),
            )
        }

    private fun copyContinueErrorDetail() {
        val payload =
            (continueAvailability as? ContinueAvailability.Unavailable)
                ?.let { unavailable -> ContinueUnavailablePayloadFormatter.format(app.localizer(), unavailable).renderPlainText() }
                ?: return
        val copied = copyToClipboard(payload)
        menuNotice =
            app.text(
                if (copied) {
                    "ui.menu.continue.error.copied"
                } else {
                    "ui.menu.continue.error.copy-failed"
                },
            )
    }

    private fun controlsText(unavailable: ContinueAvailability.Unavailable?): String =
        if (unavailable == null) {
            app.text("ui.menu.controls")
        } else {
            app.text("ui.menu.controls") + "  " + app.text("ui.menu.continue.copy-hint")
        }

    private fun selectionStateColor(state: ClassPlayabilityState): Color =
        when (state) {
            ClassPlayabilityState.PLAYABLE -> UiDesignTokens.color.focus.ring.color()
            ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE -> UiDesignTokens.color.quality.rare.color()
            ClassPlayabilityState.LOCKED -> UiDesignTokens.color.text.disabled.color()
        }

    private fun summaryModel(localizer: Localizer): MainMenuSummaryModel =
        MainMenuSummaryModel(
            primaryAction =
                MainMenuFocusPolicy.primaryAction(
                    entries = controller.entries(continueAvailability),
                    continueAvailability = continueAvailability,
                ),
            continueAvailability = continueAvailability,
            buildSummary =
                listOf(
                    BuildCapabilityLine("ui.menu.build.class-roster.label", "ui.menu.build.class-roster.value"),
                    BuildCapabilityLine("ui.menu.build.race-roster.label", "ui.menu.build.race-roster.value"),
                    BuildCapabilityLine("ui.menu.build.phase4.label", "ui.menu.build.phase4.value"),
                ),
            helpLines =
                listOf(
                    localizer.text("ui.menu.help.primary-keys"),
                    localizer.text("ui.menu.help.safe-start"),
                ),
            localeLabel = localizer.localeLabel(),
        )
}

internal fun copyTextToClipboard(payload: String): Boolean =
    runCatching {
        val clipboard = Gdx.app?.clipboard
        if (clipboard == null) {
            false
        } else {
            clipboard.contents = payload
            true
        }
    }.getOrDefault(false)

internal fun resourceHintText(
    localizer: Localizer,
    resourceHintKey: String,
): String = localizer.text(resourceHintKey)

internal fun selectionLabel(
    localizer: Localizer,
    labelKey: String,
    valueKey: String,
): String = localizer.text(labelKey, "value" to localizer.text(valueKey))

internal fun selectionStateText(
    localizer: Localizer,
    state: ClassPlayabilityState,
): String =
    localizer.text(
        when (state) {
            ClassPlayabilityState.PLAYABLE -> "ui.menu.selection_state.playable"
            ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE -> "ui.menu.selection_state.unavailable"
            ClassPlayabilityState.LOCKED -> "ui.menu.selection_state.locked"
        },
    )

internal fun discoveredUnavailableNoteText(
    localizer: Localizer,
    noteKey: String,
    optionNameKeys: List<String>,
): String? =
    optionNameKeys
        .distinct()
        .takeIf(List<String>::isNotEmpty)
        ?.let { nameKeys -> localizer.joinLocalizedKeys(LocalizedTextSeparator.LIST, nameKeys) }
        ?.let { items -> localizer.text(noteKey, "items" to items) }
