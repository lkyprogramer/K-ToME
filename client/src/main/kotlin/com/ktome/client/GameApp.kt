package com.ktome.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.Screen
import com.ktome.client.screen.FoundationGameScreen
import com.ktome.client.screen.GameOverScreen
import com.ktome.client.screen.MainMenuScreen
import com.ktome.client.screen.VictoryScreen
import com.ktome.core.save.AssetVersionContract
import com.ktome.core.save.AssetVersionGate
import com.ktome.core.save.AssetVersionMismatchException
import com.ktome.core.save.SaveLoadException
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import java.nio.file.Path

class GameApp(
    private val saveManager: SaveManager = SaveManager(defaultSaveDir()),
    private val defaultConfig: FoundationGameConfig = FoundationGameConfig(),
    private val assetVersionProvider: () -> AssetVersionContract = AssetVersionResourceLoader::load,
    private val assetVersionGate: AssetVersionGate = AssetVersionGate(),
) : Game() {
    private val lifecycle = LifecycleCoordinator(saveManager)
    private var activeSession: FoundationGameSession? = null

    override fun create() {
        showMainMenu(saveCurrent = false, notice = assetContractNotice())
    }

    fun startNewGame() {
        if (!ensureAssetContracts()) {
            return
        }
        val session =
            lifecycle.startNewSession {
                GameModule.newFoundationSession(config = defaultConfig, saveManager = saveManager)
            }
        activeSession = session
        replaceScreen(FoundationGameScreen(this, session))
    }

    fun continueGame() {
        if (!ensureAssetContracts()) {
            return
        }
        val session =
            lifecycle.continueSession {
                GameModule.loadFoundationSession(saveManager)
            } ?: run {
                showMainMenu(saveCurrent = false, notice = lifecycle.consumeNotice())
                return
            }
        activeSession = session
        replaceScreen(FoundationGameScreen(this, session))
    }

    fun showOutcome(session: FoundationGameSession) {
        val summary = session.runSummary() ?: return
        activeSession = null
        replaceScreen(
            if (session.isVictory()) {
                VictoryScreen(this, summary)
            } else {
                GameOverScreen(this, summary)
            },
        )
    }

    fun showMainMenu(
        saveCurrent: Boolean = true,
        notice: String? = null,
    ) {
        if (saveCurrent) {
            activeSession?.saveOnExit()
        }
        activeSession = null
        val continueEnabled = lifecycle.refreshContinueAvailability()
        replaceScreen(MainMenuScreen(this, continueEnabled, notice ?: lifecycle.consumeNotice()))
    }

    override fun dispose() {
        activeSession?.saveOnExit()
        super.dispose()
    }

    private fun replaceScreen(nextScreen: Screen) {
        val previous = screen
        setScreen(nextScreen)
        previous?.dispose()
    }

    private fun assetContractNotice(): String? =
        try {
            assetVersionGate.requireCompatible(assetVersionProvider())
            null
        } catch (exception: AssetVersionMismatchException) {
            exception.message
        } catch (exception: AssetVersionLoadException) {
            exception.message
        }

    private fun ensureAssetContracts(): Boolean =
        assetContractNotice()?.let { notice ->
            showMainMenu(saveCurrent = false, notice = notice)
            false
        } ?: true

    companion object {
        private fun defaultSaveDir(): Path = Path.of(System.getProperty("user.home"), ".ktome")
    }
}

internal class LifecycleCoordinator(
    private val saveManager: SaveManager,
) {
    private var cachedContinueAvailable: Boolean = false
    private var pendingNotice: String? = null

    fun refreshContinueAvailability(): Boolean {
        pendingNotice = null
        cachedContinueAvailable = snapshotIsLoadable()
        return cachedContinueAvailable
    }

    fun cachedContinueAvailability(): Boolean = cachedContinueAvailable

    fun consumeNotice(): String? =
        pendingNotice.also {
            pendingNotice = null
        }

    fun <T> startNewSession(factory: () -> T): T {
        val session = factory()
        saveManager.deleteSave()
        cachedContinueAvailable = false
        return session
    }

    fun <T> continueSession(loader: () -> T?): T? {
        val session =
            try {
                loader()
            } catch (exception: SaveLoadException) {
                pendingNotice = exception.message
                null
            }
        if (session == null) {
            cachedContinueAvailable = false
        } else {
            cachedContinueAvailable = true
        }
        return session
    }

    private fun snapshotIsLoadable(): Boolean =
        try {
            saveManager.load() != null
        } catch (exception: SaveLoadException) {
            pendingNotice = exception.message
            false
        } catch (_: Exception) {
            false
        }
}
