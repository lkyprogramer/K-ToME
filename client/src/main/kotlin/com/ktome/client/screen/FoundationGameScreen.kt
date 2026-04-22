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
import com.ktome.client.assets.ClientAssetBundle
import com.ktome.client.assets.RenderSnapshotAssetAudit
import com.ktome.client.audio.AudioRouter
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputHandlerCommandSource
import com.ktome.client.render.KtomeFonts
import com.ktome.client.render.TileRenderer
import com.ktome.client.ui.state.UiErrorState
import com.ktome.client.ui.state.UiLoadingState
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.FoundationGameSession
import kotlin.math.max

private const val cellWidth = 32f
private const val cellHeight = 32f
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
            activeErrorState = runtimeErrorState(snapshot, exception)
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
                activeErrorState = runtimeErrorState(snapshot, exception)
            }
            return
        }

        ensureResources()
        try {
            auditSnapshot(snapshot)
        } catch (exception: RuntimeException) {
            activeErrorState = runtimeErrorState(snapshot, exception)
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
            activeErrorState = runtimeErrorState(snapshot, exception)
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
            systemFont = KtomeFonts.createUiFont(size = 24)
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
    }

    private fun renderLoadingState(loadingState: UiLoadingState) {
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(systemFont)
        ScreenUtils.clear(0.03f, 0.03f, 0.05f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, app.text(loadingState.message), 32f, max(64f, viewport.worldHeight - 48f))
        if (loadingState.allowsCancel) {
            font.draw(batch, app.text(UiLoadingState.cancelLabelToken), 32f, max(32f, viewport.worldHeight - 84f))
        }
        batch.end()
    }

    private fun renderErrorState(errorState: UiErrorState) {
        ensureResources()
        val batch = requireNotNull(batch)
        val font = requireNotNull(systemFont)
        ScreenUtils.clear(0.06f, 0.02f, 0.02f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        font.color = Color.WHITE
        var y = max(120f, viewport.worldHeight - 48f)
        font.draw(batch, app.text(errorState.heading), 32f, y)
        y -= 36f
        font.draw(batch, app.text(errorState.detail), 32f, y)
        y -= 54f
        errorState.actions.forEach { action ->
            font.draw(batch, uiErrorActionLabel(action, app::text), 32f, y)
            y -= 30f
        }
        batch.end()
    }

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
