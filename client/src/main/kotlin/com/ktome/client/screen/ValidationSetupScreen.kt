package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.GameApp
import com.ktome.client.bossVariantModeLabelKey
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputSource
import com.ktome.client.render.KtomeFonts
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.client.validation.ValidationScenarioPresentationCatalog
import com.ktome.game.elites.BossVariantSelectionMode
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationSessionOptions

internal data class ValidationSetupTextSnapshot(
    val title: String,
    val subtitle: String,
    val presetSummary: String,
    val activePackSummary: String,
    val notice: String?,
    val entries: List<String>,
)

internal class ValidationSetupScreen(
    private val app: GameApp,
    context: ValidationSetupContext,
    inputSource: InputSource = GdxInputSource,
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private val viewport = FitViewport(menuWidth, menuHeight)
    private val zoneNameById = context.zones.associate { zone -> zone.id to app.text(zone.displayNameKey) }
    private val notice: String? = context.notice
    private val controller = ValidationSetupController(input = inputSource, context = context)
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private var chrome: StandaloneScreenChrome? = null

    override fun show() {
        app.audioRouterOrNull()?.onMenuShown()
        if (!renderEnabled) {
            return
        }
        ensureResources()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun render(delta: Float) {
        val poll = controller.pollAction()
        if (poll.selectionChanged) {
            app.rememberValidationSetupOptions(poll.options)
        }
        when (val action = poll.action) {
            is ValidationSetupAction.StartSession -> {
                app.startValidationSession(action.options)
                return
            }

            is ValidationSetupAction.ContinueSession -> {
                app.continueValidationSession(action.options)
                return
            }

            ValidationSetupAction.Back -> {
                app.showMainMenu(saveCurrent = false)
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
        val text = textSnapshot(poll.options)
        val layout = DarkStandaloneScreenLayout.validationSetup()

        val surfaceBase = UiDesignTokens.color.surface.base.color()
        ScreenUtils.clear(surfaceBase.r, surfaceBase.g, surfaceBase.b, surfaceBase.a)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        batch.begin()
        requireNotNull(chrome).draw(
            batch,
            StandaloneChromeRequest(
                layout = layout,
                detailAreaMode =
                    if (text.notice == null) {
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
        font.color = UiDesignTokens.color.text.secondary.color()
        font.draw(batch, DarkStandaloneScreenLayout.truncate(text.presetSummary, 78), layout.secondaryPanel.x, layout.secondaryPanel.top - 12f)
        font.draw(batch, DarkStandaloneScreenLayout.truncate(text.activePackSummary, 78), layout.secondaryPanel.x, layout.secondaryPanel.top - 36f)
        text.notice?.let { message ->
            font.color = UiDesignTokens.color.status.badge.turns.color()
            font.draw(batch, DarkStandaloneScreenLayout.truncate(message, 78), layout.disabledDetailArea.x, layout.disabledDetailArea.top)
        }

        val entryPlacements = DarkStandaloneScreenLayout.validationEntryPlacements(text.entries.size)
        text.entries.forEachIndexed { index, line ->
            val entryPlacement = entryPlacements[index]
            font.color =
                if (controller.selectedEntry() == ValidationSetupEntryId.entries[index]) {
                    UiDesignTokens.color.focus.ring.color()
                } else {
                    UiDesignTokens.color.text.primary.color()
            }
            font.draw(batch, DarkStandaloneScreenLayout.truncate(line, entryPlacement.maxChars), entryPlacement.x, entryPlacement.baselineY)
        }

        font.color = UiDesignTokens.color.text.disabled.color()
        font.draw(batch, app.text("ui.validation.controls"), layout.footerHelp.x, DarkStandaloneScreenLayout.validationFooterControlsBaselineY)
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

    internal fun textSnapshot(options: ValidationSessionOptions = controller.currentOptions()): ValidationSetupTextSnapshot {
        val localizer = app.localizer()
        val entries =
            listOf(
                app.text("ui.validation.entry.preset", "value" to localizer.text(options.preset.titleKey)),
                app.text(
                    "ui.validation.entry.scenario",
                    "value" to scenarioLabel(options.scenarioId),
                ),
                app.text(
                    "ui.validation.entry.profession",
                    "value" to app.text("profession.${options.foundationConfig.playerProfessionId}.name"),
                ),
                app.text("ui.validation.entry.race", "value" to app.text("race.${options.foundationConfig.playerRaceId}.name")),
                app.text("ui.validation.entry.seed", "value" to options.foundationConfig.seed.toString()),
                app.text(
                    "ui.validation.entry.zone",
                    "value" to zoneNameById[options.foundationConfig.zoneId].orEmpty(),
                ),
                app.text("ui.validation.entry.floor", "value" to options.foundationConfig.floor.toString()),
                app.text(
                    "ui.validation.entry.route",
                    "value" to routeLabel(options),
                ),
                app.text("ui.validation.entry.route_index", "value" to options.foundationConfig.routeIndex.toString()),
                app.text(
                    "ui.validation.entry.boss_variant_mode",
                    "value" to bossVariantModeLabel(options.foundationConfig.bossVariantSelectionMode),
                ),
                app.text(
                    "ui.validation.entry.preferred_variant",
                    "value" to (options.foundationConfig.preferredBossVariantId ?: app.text("ui.validation.none")),
                ),
                app.text(
                    "ui.validation.entry.sample_pack",
                    "value" to packToggleLabel(options),
                ),
                app.text("ui.validation.action.start"),
                app.text("ui.validation.action.continue"),
                app.text("ui.validation.action.back"),
            )
        return ValidationSetupTextSnapshot(
            title = app.text("ui.validation.title"),
            subtitle = app.text("ui.validation.subtitle"),
            presetSummary = app.text(options.preset.summaryKey),
            activePackSummary = app.text("ui.validation.active_packs", "value" to packSummary(options)),
            notice = notice,
            entries = entries,
        )
    }

    private fun routeLabel(options: ValidationSessionOptions): String =
        if (options.foundationConfig.zoneRoute.size == 1) {
            app.text("ui.validation.route.current_zone")
        } else {
            app.text("ui.validation.route.foundation")
        }

    private fun scenarioLabel(scenarioId: ValidationScenarioId?): String {
        scenarioId ?: return app.text("ui.validation.none")
        val titleKey = ValidationScenarioPresentationCatalog.find(scenarioId)?.titleKey
        return titleKey?.let(app::text) ?: scenarioId.value
    }

    private fun bossVariantModeLabel(mode: BossVariantSelectionMode): String =
        app.text(bossVariantModeLabelKey(mode.name))

    private fun packToggleLabel(options: ValidationSessionOptions): String =
        if (options.contentPackSelection.activePackRoots.isEmpty()) {
            app.text("ui.validation.toggle.off")
        } else {
            app.text("ui.validation.toggle.on")
        }

    private fun packSummary(options: ValidationSessionOptions): String =
        options.contentPackSelection.activePackRoots
            .map { root -> root.fileName.toString() }
            .ifEmpty { listOf(app.text("ui.validation.not_available")) }
            .joinToString(", ")

    private fun ensureResources() {
        if (batch == null) {
            batch = SpriteBatch()
        }
        if (font == null) {
            font = KtomeFonts.createUiFont(size = 20)
        }
        if (chrome == null) {
            chrome = StandaloneScreenChrome()
        }
    }
}
