package com.ktome.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.Screen
import com.ktome.client.screen.FoundationGameScreen
import com.ktome.client.screen.GameOverScreen
import com.ktome.client.screen.MainMenuScreen
import com.ktome.client.screen.VictoryScreen
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import java.nio.file.Path

class GameApp(
    private val saveManager: SaveManager = SaveManager(defaultSaveDir()),
    private val defaultConfig: FoundationGameConfig = FoundationGameConfig(),
) : Game() {
    private val lifecycle = LifecycleCoordinator(saveManager)
    private var activeSession: FoundationGameSession? = null

    override fun create() {
        showMainMenu(saveCurrent = false)
    }

    fun startNewGame() {
        val session =
            lifecycle.startNewSession {
                GameModule.newFoundationSession(config = defaultConfig, saveManager = saveManager)
            }
        activeSession = session
        replaceScreen(FoundationGameScreen(this, session))
    }

    fun continueGame() {
        val session =
            lifecycle.continueSession {
                GameModule.loadFoundationSession(saveManager)
            } ?: run {
                showMainMenu(saveCurrent = false)
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

    fun showMainMenu(saveCurrent: Boolean = true) {
        if (saveCurrent) {
            activeSession?.saveOnExit()
        }
        activeSession = null
        val continueEnabled = lifecycle.refreshContinueAvailability()
        replaceScreen(MainMenuScreen(this, continueEnabled))
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

    companion object {
        private fun defaultSaveDir(): Path = Path.of(System.getProperty("user.home"), ".ktome")
    }
}

internal class LifecycleCoordinator(
    private val saveManager: SaveManager,
) {
    private var cachedContinueAvailable: Boolean = false

    fun refreshContinueAvailability(): Boolean {
        cachedContinueAvailable = snapshotIsLoadable()
        return cachedContinueAvailable
    }

    fun cachedContinueAvailability(): Boolean = cachedContinueAvailable

    fun <T> startNewSession(factory: () -> T): T {
        val session = factory()
        saveManager.deleteSave()
        cachedContinueAvailable = false
        return session
    }

    fun <T> continueSession(loader: () -> T?): T? {
        val session = loader()
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
        } catch (_: Exception) {
            false
        }
}
