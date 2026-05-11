package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.GameApp
import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputSource
import com.ktome.client.render.KtomeFonts
import com.ktome.client.ui.chrome.ChromeSurfaceKind
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.game.OutcomeSummary

class VictoryScreen(
    private val app: GameApp,
    private val summary: OutcomeSummary,
    private val inputSource: InputSource = GdxInputSource,
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private val bodyLines: List<String>
        get() = OutcomeSummaryPresenter.bodyLines(app, summary, isVictory = true)
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private var chrome: StandaloneScreenChrome? = null
    private val viewport = FitViewport(menuWidth, menuHeight)

    override fun show() {
        app.audioRouterOrNull()?.onMenuShown()
        if (!renderEnabled) {
            return
        }
        ensureResources()
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    override fun render(delta: Float) {
        if (inputSource.isKeyJustPressed(Keys.ENTER) || inputSource.isKeyJustPressed(Keys.ESCAPE)) {
            app.showMainMenu(saveCurrent = false)
            return
        }
        if (!renderEnabled) {
            return
        }
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(font)
        val layout = DarkStandaloneScreenLayout.outcome()

        val surfaceBase = UiDesignTokens.color.surface.base.color()
        ScreenUtils.clear(surfaceBase.r, surfaceBase.g, surfaceBase.b, surfaceBase.a)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        batch.begin()
        requireNotNull(chrome).draw(
            batch,
            StandaloneChromeRequest(
                layout = layout,
                detailAreaMode = StandaloneDetailAreaMode.HIDDEN,
                chromeAssets = app.standaloneChromeAssetsOrNull(DarkUiChromeVisualKeys.SCREEN_OUTCOME_VICTORY_MARKER),
            ),
        )
        val headerContent = layout.header.insetForChromeFrame()
        val bodyContent = layout.primaryActionStack.insetForChromeFrame()
        val footerContent = layout.footerHelp.insetForChromeFrame(ChromeSurfaceKind.FooterHint)
        font.color = UiDesignTokens.color.status.badge.stack.color()
        font.draw(batch, DarkStandaloneScreenLayout.truncate(app.text("ui.victory.title"), headerContent.maxChars()), headerContent.x, headerContent.top - 4f)
        font.color = UiDesignTokens.color.text.primary.color()
        bodyLines.zip(DarkStandaloneScreenLayout.outcomeBodyLineBaselines(bodyLines.size)).forEach { (line, y) ->
            font.draw(batch, DarkStandaloneScreenLayout.truncate(line, bodyContent.maxChars()), bodyContent.x, y.coerceAtLeast(bodyContent.y + 18f))
        }
        font.color = UiDesignTokens.color.text.disabled.color()
        font.draw(batch, DarkStandaloneScreenLayout.truncate(app.text("ui.screen.return_to_menu"), footerContent.maxChars()), footerContent.x, footerContent.top - 4f)
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
            font = KtomeFonts.createUiFont(size = UiDesignTokens.typography.body)
        }
        if (chrome == null) {
            chrome = StandaloneScreenChrome(app.standaloneChromeTextureRepositoryOrNull())
        }
    }
}
