package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.GameApp
import com.ktome.client.assets.ClientAssetBundle
import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.RenderSnapshotAssetAudit
import com.ktome.client.audio.AudioRouter
import com.ktome.client.input.AudioRouterAwareCommandSource
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputHandlerCommandSource
import com.ktome.client.render.KtomeFonts
import com.ktome.client.render.TileTextStyle
import com.ktome.client.render.TileRenderer
import com.ktome.client.ui.chrome.ChromeSurfaceKind
import com.ktome.client.ui.state.UiErrorState
import com.ktome.client.ui.state.UiLoadingState
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.FoundationGameSession

private const val cellWidth = 42f
private const val cellHeight = 42f
private const val runtimeErrorMessageMaxChars = 200

class FoundationGameScreen(
    private val app: GameApp,
    private val session: FoundationGameSession,
    private val assets: ClientAssetBundle,
    private val commandSource: CommandSource = InputHandlerCommandSource(audioRouter = AudioRouter(assets.audioResolver)),
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var renderer: TileRenderer? = null
    private var systemFont: BitmapFont? = null
    private var chrome: StandaloneScreenChrome? = null
    private var lastAudioSnapshot: RenderSnapshot? = null
    private val assetAudit = RenderSnapshotAssetAudit(assets)
    private var lastAuditedRevision: Long? = null
    private var activeErrorState: UiErrorState? = null
    private val viewport =
        session.renderSnapshot().let { initialSnapshot ->
            FitViewport(
                FoundationViewportSupport.worldWidth(initialSnapshot),
                FoundationViewportSupport.worldHeight(initialSnapshot),
            )
        }

    override fun show() {
        centerCamera()
    }

    override fun render(delta: Float) {
        var snapshot = session.renderSnapshot()
        activeErrorState?.let { errorState ->
            if (handleErrorStateInput(errorState)) {
                return
            }
            if (renderEnabled) {
                renderErrorState(errorState)
            }
            return
        }
        if (session.runOutcome().isTerminal) {
            app.showOutcome(session)
            return
        }

        syncViewport(snapshot)
        commandSource.nextCommand(snapshot)?.let { command ->
            val previousSnapshot = snapshot
            val consumed = session.perform(command)
            snapshot = session.renderSnapshot()
            syncViewport(snapshot)
            commandSource.onCommandResult(previousSnapshot, snapshot, command, consumed)
            session.consumePendingValidationRestartOptions()?.let { restartOptions ->
                app.startValidationSession(restartOptions)
                return
            }
        }

        if (session.runOutcome().isTerminal) {
            app.showOutcome(session)
            return
        }

        if (commandSource.shouldReturnToMenu()) {
            commandSource.onReturnToMenu()
            app.showMainMenu(saveCurrent = true)
            return
        }

        val overlayState = commandSource.overlayState()
        val loadingState = app.loadingStateFor(snapshot)
        try {
            app.warmSessionAssets(snapshot)
        } catch (exception: RuntimeException) {
            recordRuntimeError(snapshot, exception)
            return
        }
        if (loadingState != null) {
            if (renderEnabled) {
                renderLoadingState(loadingState)
            }
            return
        }
        if (lastAudioSnapshot?.metadata?.revision != snapshot.metadata.revision) {
            commandSource.onSnapshotUpdated(lastAudioSnapshot, snapshot)
            lastAudioSnapshot = snapshot
        }
        if (!renderEnabled) {
            try {
                auditSnapshot(snapshot)
                TileRenderer.renderHeadless(session.localizer(), assets.visualResolver, snapshot, overlayState, cellWidth = cellWidth, cellHeight = cellHeight)
            } catch (exception: RuntimeException) {
                recordRuntimeError(snapshot, exception)
            }
            return
        }

        ensureResources()
        try {
            auditSnapshot(snapshot)
        } catch (exception: RuntimeException) {
            recordRuntimeError(snapshot, exception)
            renderErrorState(requireNotNull(activeErrorState))
            return
        }
        val batch = requireNotNull(batch)
        val renderer = requireNotNull(renderer)
        ScreenUtils.clear(0.03f, 0.03f, 0.05f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        try {
            batch.begin()
            renderer.render(batch, snapshot, overlayState)
        } catch (exception: RuntimeException) {
            recordRuntimeError(snapshot, exception)
        } finally {
            if (batch.isDrawing) {
                batch.end()
            }
        }
        activeErrorState?.let(::renderErrorState)
    }

    override fun resize(width: Int, height: Int) {
        if (renderEnabled) {
            viewport.update(width, height, true)
            syncViewport(session.renderSnapshot())
            centerCamera()
        }
    }

    override fun dispose() {
        renderer?.dispose()
        renderer = null
        chrome?.dispose()
        chrome = null
        systemFont?.dispose()
        systemFont = null
        batch?.dispose()
        batch = null
        lastAudioSnapshot = null
    }

    private fun centerCamera() {
        viewport.camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)
        viewport.camera.update()
    }

    private fun ensureResources() {
        if (batch == null) {
            batch = SpriteBatch()
        }
        if (systemFont == null) {
            systemFont = KtomeFonts.createUiFont(size = UiDesignTokens.typography.body)
        }
        if (renderer == null) {
            renderer =
                TileRenderer(
                    localizer = session.localizer(),
                    visualResolver = assets.visualResolver,
                    textureRepository = assets.textureRepository,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                )
        }
        if (chrome == null) {
            chrome = StandaloneScreenChrome(assets.textureRepository)
        }
    }

    private fun renderLoadingState(loadingState: UiLoadingState) {
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(systemFont)
        val layout = DarkStandaloneScreenLayout.runtimeStatus(viewport.worldWidth, viewport.worldHeight)
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
                chromeAssets = runtimeChromeAssets(DarkUiChromeVisualKeys.SCREEN_LOADING_MARKER),
            ),
        )
        font.color = UiDesignTokens.color.text.primary.color()
        val headerContent = layout.header.insetForChromeFrame()
        val footerContent = layout.footerHelp.insetForChromeFrame(ChromeSurfaceKind.FooterHint)
        font.draw(batch, headerContent.fitText(app.text(loadingState.message), TileTextStyle.UI), headerContent.x, headerContent.top - 8f)
        if (loadingState.allowsCancel) {
            font.color = UiDesignTokens.color.text.secondary.color()
            font.draw(batch, footerContent.fitText(app.text(UiLoadingState.cancelLabelToken), TileTextStyle.SMALL), footerContent.x, footerContent.top - 4f)
        }
        batch.end()
    }

    private fun renderErrorState(errorState: UiErrorState) {
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(systemFont)
        val layout = DarkStandaloneScreenLayout.runtimeStatus(viewport.worldWidth, viewport.worldHeight)
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
                chromeAssets = runtimeChromeAssets(DarkUiChromeVisualKeys.SCREEN_ERROR_MARKER),
            ),
        )
        font.color = UiDesignTokens.color.telegraph.high.color()
        val headerContent = layout.header.insetForChromeFrame()
        val bodyContent = layout.primaryActionStack.insetForChromeFrame()
        font.draw(batch, headerContent.fitText(app.text(errorState.heading), TileTextStyle.UI), headerContent.x, headerContent.top - 8f)
        font.color = UiDesignTokens.color.text.primary.color()
        var y = bodyContent.top - 8f
        bodyContent.wrapText(app.text(errorState.detail), TileTextStyle.SMALL, maxLines = 3).forEach { line ->
            font.draw(batch, line, bodyContent.x, y)
            y -= 30f
        }
        y -= 12f
        errorState.actions.forEach { action ->
            val label = uiErrorActionLabel(action, app::text, errorState.copyDetailLabelKey)
            font.draw(batch, bodyContent.fitText(label, TileTextStyle.SMALL), bodyContent.x, y)
            y -= 30f
        }
        batch.end()
    }

    private fun runtimeChromeAssets(screenMarkerKey: String): StandaloneChromeAssets =
        StandaloneChromeAssets.resolve(
            visualResolver = assets.visualResolver,
            screenMarkerKey = screenMarkerKey,
        )

    private fun handleErrorStateInput(errorState: UiErrorState): Boolean {
        if (Gdx.input == null) {
            return false
        }
        return when {
            Gdx.input.isKeyJustPressed(Keys.R) -> {
                activeErrorState = null
                lastAuditedRevision = null
                true
            }

            Gdx.input.isKeyJustPressed(Keys.C) -> {
                copyTextToClipboard(errorState.payload.renderPlainText())
                false
            }

            Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACKSPACE) -> {
                activeErrorState = null
                app.showMainMenu(saveCurrent = false)
                true
            }

            else -> false
        }
    }

    private fun recordRuntimeError(
        snapshot: RenderSnapshot,
        exception: RuntimeException,
    ) {
        if (!renderEnabled) {
            throw exception
        }
        (commandSource as? AudioRouterAwareCommandSource)?.audioRouter?.onCriticalError()
        activeErrorState = runtimeErrorState(snapshot, exception)
    }

    private fun runtimeErrorState(
        snapshot: RenderSnapshot,
        exception: RuntimeException,
    ): UiErrorState =
        UiErrorState.recoverable(
            localizer = session.localizer(),
            heading = RenderTextTokenSnapshot("ui.error.runtime.heading"),
            detail = RenderTextTokenSnapshot("ui.error.runtime.detail"),
            contextKeyValuePairs =
                listOf(
                    "screen" to "FoundationGameScreen",
                    "revision" to snapshot.metadata.revision.toString(),
                    "zoneId" to snapshot.metadata.zoneId,
                    "exception" to exception::class.java.name,
                    "message" to exception.message.orEmpty().take(runtimeErrorMessageMaxChars),
                ),
        )

    private fun auditSnapshot(snapshot: com.ktome.core.snapshot.RenderSnapshot) {
        if (lastAuditedRevision == snapshot.metadata.revision) {
            return
        }
        assetAudit.audit(snapshot)
        lastAuditedRevision = snapshot.metadata.revision
    }

    private fun syncViewport(snapshot: RenderSnapshot) {
        if (FoundationViewportSupport.syncViewport(viewport, snapshot, Gdx.graphics.width, Gdx.graphics.height)) {
            centerCamera()
        }
    }
}

internal object FoundationViewportSupport {
    fun worldWidth(snapshot: RenderSnapshot): Float =
        TileRenderer.worldWidth(snapshot, cellWidth = cellWidth, cellHeight = cellHeight)

    fun worldHeight(snapshot: RenderSnapshot): Float =
        TileRenderer.worldHeight(snapshot, cellWidth = cellWidth, cellHeight = cellHeight)

    fun syncViewport(
        viewport: FitViewport,
        snapshot: RenderSnapshot,
        screenWidth: Int,
        screenHeight: Int,
    ): Boolean {
        val targetWidth = worldWidth(snapshot)
        val targetHeight = worldHeight(snapshot)
        if (viewport.worldWidth == targetWidth && viewport.worldHeight == targetHeight) {
            return false
        }
        viewport.setWorldSize(targetWidth, targetHeight)
        if (Gdx.graphics != null) {
            viewport.update(screenWidth, screenHeight, true)
        }
        return true
    }
}
