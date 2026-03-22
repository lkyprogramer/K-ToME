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
import com.ktome.game.i18n.Localizer

internal const val menuWidth = 960f
internal const val menuHeight = 540f

internal data class MainMenuTextSnapshot(
    val title: String,
    val subtitle: String,
    val profession: String,
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
    selectedProfessionId: String,
    private val notice: String? = null,
    inputSource: InputSource = GdxInputSource,
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private val viewport = FitViewport(menuWidth, menuHeight)
    private val controller = MainMenuController(input = inputSource, availableProfessionIds = availableProfessionIds, initialProfessionId = selectedProfessionId)
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

        batch.begin()
        font.color = Color.GOLD
        font.draw(batch, text.title, 120f, 420f)
        font.color = Color.LIGHT_GRAY
        font.draw(batch, text.subtitle, 120f, 392f)
        font.color = Color.WHITE
        font.draw(batch, text.profession, 120f, 352f)
        font.color = Color.GRAY
        font.draw(batch, text.professionDescription, 120f, 324f)
        font.draw(batch, text.professionResourceHint, 120f, 296f)

        controller.entries(continueEnabled).forEachIndexed { index, entry ->
            font.color =
                when {
                    !entry.enabled -> Color.DARK_GRAY
                    index == selectedIndex -> Color.CYAN
                    else -> Color.WHITE
                }
            font.draw(batch, text.entries[index], 120f, 248f - index * 32f)
        }

        font.color = Color.GOLD
        font.draw(batch, text.language, 120f, 160f)
        font.color = Color.GRAY
        font.draw(batch, text.controls, 120f, 112f)
        text.notice?.let { message ->
            font.color = Color.SALMON
            font.draw(batch, message, 120f, 76f)
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
                professionDescription = app.text("profession.$professionId.desc"),
                professionResourceHint = professionResourceHint(app.localizer(), professionId),
                entries = controller.entries(continueEnabled).map { entry -> app.text(entry.labelKey) },
                language = app.text("ui.menu.language", "value" to app.localizer().localeLabel()),
                controls = app.text("ui.menu.controls"),
                notice = notice?.takeIf(String::isNotBlank),
            )
        }
}

internal fun professionResourceHint(
    localizer: Localizer,
    professionId: String,
): String =
    localizer.text(
        when (professionId) {
            "vanguard" -> "profession.vanguard.resource_hint"
            "arcanist" -> "profession.arcanist.resource_hint"
            "rogue" -> "profession.rogue.resource_hint"
            "templar" -> "profession.templar.resource_hint"
            else -> error("Unknown profession '$professionId'.")
        },
    )
