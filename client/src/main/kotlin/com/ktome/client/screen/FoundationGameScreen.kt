package com.ktome.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.GameApp
import com.ktome.client.assets.ClientAssetBundle
import com.ktome.client.assets.RenderSnapshotAssetAudit
import com.ktome.client.audio.AudioRouter
import com.ktome.client.input.CommandSource
import com.ktome.client.input.InputHandlerCommandSource
import com.ktome.client.render.TileRenderer
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.FoundationGameSession

private const val cellWidth = 32f
private const val cellHeight = 32f

class FoundationGameScreen(
    private val app: GameApp,
    private val session: FoundationGameSession,
    private val assets: ClientAssetBundle,
    private val commandSource: CommandSource = InputHandlerCommandSource(audioRouter = AudioRouter(assets.audioResolver)),
    private val renderEnabled: Boolean = true,
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var renderer: TileRenderer? = null
    private var lastAudioSnapshot: RenderSnapshot? = null
    private val assetAudit = RenderSnapshotAssetAudit(assets)
    private var lastAuditedRevision: Long? = null
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
        if (session.runOutcome().isTerminal) {
            app.showOutcome(session)
            return
        }

        syncViewport(snapshot)
        commandSource.nextCommand(snapshot)?.let { command ->
            val consumed = session.perform(command)
            snapshot = session.renderSnapshot()
            syncViewport(snapshot)
            commandSource.onCommandResult(snapshot, command, consumed)
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
        app.warmSessionAssets(snapshot)
        if (lastAudioSnapshot?.metadata?.revision != snapshot.metadata.revision) {
            commandSource.onSnapshotUpdated(lastAudioSnapshot, snapshot)
            lastAudioSnapshot = snapshot
        }
        if (!renderEnabled) {
            auditSnapshot(snapshot)
            TileRenderer.renderHeadless(session.localizer(), assets.visualResolver, snapshot, overlayState, cellWidth = cellWidth, cellHeight = cellHeight)
            return
        }

        ensureResources()
        auditSnapshot(snapshot)
        val batch = requireNotNull(batch)
        val renderer = requireNotNull(renderer)
        ScreenUtils.clear(0.03f, 0.03f, 0.05f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        batch.begin()
        renderer.render(batch, snapshot, overlayState)
        batch.end()
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
