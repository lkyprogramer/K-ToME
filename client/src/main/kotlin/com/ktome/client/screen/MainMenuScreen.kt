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
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.game.PlayerCreationState
import com.ktome.game.i18n.Localizer

internal const val menuWidth = 960f
internal const val menuHeight = 540f
internal const val MAIN_MENU_TEXT_X = 120f
internal const val MAIN_MENU_TITLE_Y = 420f
internal const val MAIN_MENU_SUBTITLE_Y = 392f
internal const val MAIN_MENU_PANEL_TOP_Y = 352f
internal const val MAIN_MENU_FOOTER_LANGUAGE_Y = 128f
internal const val MAIN_MENU_FOOTER_CONTROLS_Y = 80f
internal const val MAIN_MENU_FOOTER_NOTICE_Y = 44f
internal const val MAIN_MENU_FOOTER_LINE_HEIGHT = 24f
internal const val MAIN_MENU_CLASS_ENTRY_BASE_OFFSET_Y = 132f
internal const val MAIN_MENU_CLASS_ENTRY_STEP_Y = 32f

internal fun mainMenuClassEntryY(index: Int): Float =
    MAIN_MENU_PANEL_TOP_Y - MAIN_MENU_CLASS_ENTRY_BASE_OFFSET_Y - index * MAIN_MENU_CLASS_ENTRY_STEP_Y

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
    val language: String,
    val controls: String,
    val notice: String?,
)

class MainMenuScreen(
    private val app: GameApp,
    private val continueEnabled: Boolean,
    playerCreationState: PlayerCreationState,
    private val notice: String? = null,
    inputSource: InputSource = GdxInputSource,
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private val viewport = FitViewport(menuWidth, menuHeight)
    private val controller =
        MainMenuController(
            input = inputSource,
            playerCreationState = playerCreationState,
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

    override fun show() {
        audioRouter?.onMenuShown()
        if (!renderEnabled) {
            return
        }
        ensureResources()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun render(delta: Float) {
        val poll = controller.pollAction(continueEnabled)
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
            MainMenuAction.StartNewGame -> {
                app.startNewGame(poll.selection)
                return
            }
            MainMenuAction.ContinueGame -> {
                app.continueGame()
                return
            }
            MainMenuAction.ExitGame -> {
                Gdx.app.exit()
                return
            }
            MainMenuAction.ToggleLocale -> {
                app.cycleLocale()
                app.showMainMenu(saveCurrent = false, notice = notice)
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

        ScreenUtils.clear(0.04f, 0.04f, 0.06f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        val selectedIndex = controller.selectedIndex()
        val entries = controller.entries(continueEnabled)
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
        font.color = Color.GOLD
        font.draw(batch, text.title, MAIN_MENU_TEXT_X, MAIN_MENU_TITLE_Y)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, text.subtitle, MAIN_MENU_TEXT_X, MAIN_MENU_SUBTITLE_Y)
        PlayerCreationPanel.render(
            batch = batch,
            font = font,
            model = playerCreationPanel,
            professionStateColor = selectionStateColor(controller.currentProfessionOption().playabilityState),
            raceStateColor = selectionStateColor(controller.currentRaceOption().playabilityState),
            x = MAIN_MENU_TEXT_X,
            topY = MAIN_MENU_PANEL_TOP_Y,
        )

        font.color = Color.GOLD
        font.draw(batch, text.language, MAIN_MENU_TEXT_X, MAIN_MENU_FOOTER_LANGUAGE_Y)
        font.color = Color.GRAY
        font.draw(batch, text.controls, MAIN_MENU_TEXT_X, MAIN_MENU_FOOTER_CONTROLS_Y)
        text.notice?.let { message ->
            font.color = Color.SALMON
            font.draw(batch, message, MAIN_MENU_TEXT_X, MAIN_MENU_FOOTER_NOTICE_Y)
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
    }

    internal fun textSnapshot(): MainMenuTextSnapshot =
        app.localizer().let { localizer ->
            val professionOption = controller.currentProfessionOption()
            val raceOption = controller.currentRaceOption()
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
                entries = controller.entries(continueEnabled).map { entry -> app.text(entry.labelKey) },
                language = app.text("ui.menu.language", "value" to localizer.localeLabel()),
                controls = app.text("ui.menu.controls"),
                notice = notice?.takeIf(String::isNotBlank),
            )
        }

    private fun selectionStateColor(state: ClassPlayabilityState): Color =
        when (state) {
            ClassPlayabilityState.PLAYABLE -> Color.CYAN
            ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE -> Color.GOLD
            ClassPlayabilityState.LOCKED -> Color.DARK_GRAY
        }
}

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
