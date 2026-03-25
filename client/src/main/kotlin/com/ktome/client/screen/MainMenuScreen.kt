package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
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
import com.ktome.client.ui.creation.ClassSelectPanel
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.game.ProfessionSelectionOption
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
internal const val MAIN_MENU_CLASS_ENTRY_BASE_OFFSET_Y = 104f
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
    val entries: List<String>,
    val language: String,
    val controls: String,
    val notice: String?,
)

class MainMenuScreen(
    private val app: GameApp,
    private val continueEnabled: Boolean,
    private val availableProfessionIds: List<String>,
    private val professionSelections: List<ProfessionSelectionOption> =
        availableProfessionIds.map { professionId ->
            ProfessionSelectionOption(
                id = professionId,
                tier = com.ktome.core.profession.ProfessionTier.BASE,
                unlockState = com.ktome.core.profile.ClassUnlockState.RELEASE_UNLOCKED,
                playabilityState = ClassPlayabilityState.PLAYABLE,
            )
        },
    selectedProfessionId: String,
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
            availableProfessionIds = availableProfessionIds,
            initialProfessionId = selectedProfessionId,
            professionSelections = professionSelections,
        )
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
        if (poll.professionChanged) {
            app.rememberProfessionSelection(poll.selectedProfessionId)
        }
        audioRouter?.onMenuInteraction(
            selectionChanged = poll.selectionChanged,
            localeToggled = poll.localeToggled,
            accepted = poll.action != null && poll.action != MainMenuAction.ToggleLocale,
            rejected = poll.rejected,
        )
        when (poll.action) {
            MainMenuAction.StartNewGame -> {
                app.startNewGame(poll.selectedProfessionId)
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
            MainMenuAction.ToggleLocale -> Unit.also { app.cycleLocale() }
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
        val classSelectPanel =
            ClassSelectPanel.build(
                profession = text.profession,
                professionState = text.professionState,
                professionDescription = text.professionDescription,
                professionResourceHint = text.professionResourceHint,
                entries = entries,
                selectedIndex = selectedIndex,
                localizedEntryLabels = text.entries,
            )

        batch.begin()
        font.color = Color.GOLD
        font.draw(batch, text.title, MAIN_MENU_TEXT_X, MAIN_MENU_TITLE_Y)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, text.subtitle, MAIN_MENU_TEXT_X, MAIN_MENU_SUBTITLE_Y)
        ClassSelectPanel.render(batch, font, classSelectPanel, professionStateColor(controller.currentProfessionId()), MAIN_MENU_TEXT_X, MAIN_MENU_PANEL_TOP_Y)

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
        controller.currentProfessionId().let { professionId ->
            MainMenuTextSnapshot(
                title = app.text("ui.menu.title"),
                subtitle = app.text("ui.menu.subtitle"),
                profession = app.text("ui.menu.profession", "value" to app.text("profession.$professionId.name")),
                professionState = professionStateText(app.localizer(), selectionFor(professionId).playabilityState),
                professionDescription = app.text("profession.$professionId.desc"),
                professionResourceHint = professionResourceHint(app.localizer(), professionId),
                entries = controller.entries(continueEnabled).map { entry -> app.text(entry.labelKey) },
                language = app.text("ui.menu.language", "value" to app.localizer().localeLabel()),
                controls = app.text("ui.menu.controls"),
                notice = notice?.takeIf(String::isNotBlank),
            )
        }

    private fun selectionFor(professionId: String): ProfessionSelectionOption =
        professionSelections.firstOrNull { option -> option.id == professionId }
            ?: ProfessionSelectionOption(
                id = professionId,
                tier = com.ktome.core.profession.ProfessionTier.BASE,
                unlockState = com.ktome.core.profile.ClassUnlockState.RELEASE_UNLOCKED,
                playabilityState = ClassPlayabilityState.PLAYABLE,
            )

    private fun professionStateColor(professionId: String): Color =
        when (selectionFor(professionId).playabilityState) {
            ClassPlayabilityState.PLAYABLE -> Color.CYAN
            ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE -> Color.GOLD
            ClassPlayabilityState.LOCKED -> Color.DARK_GRAY
        }
}

internal fun professionResourceHint(
    localizer: Localizer,
    professionId: String,
): String = localizer.text("profession.$professionId.resource_hint")

internal fun professionStateText(
    localizer: Localizer,
    state: ClassPlayabilityState,
): String =
    localizer.text(
        when (state) {
            ClassPlayabilityState.PLAYABLE -> "ui.menu.profession_state.playable"
            ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE -> "ui.menu.profession_state.unavailable"
            ClassPlayabilityState.LOCKED -> "ui.menu.profession_state.locked"
        },
    )
