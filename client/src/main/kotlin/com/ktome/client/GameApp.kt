package com.ktome.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.Screen
import com.ktome.client.assets.AudioManifest
import com.ktome.client.assets.AudioManifestResourceLoader
import com.ktome.client.assets.ClientAssetBundle
import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.client.assets.ClientAssetLoadStrategy
import com.ktome.client.assets.ManifestLoadException
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestResourceLoader
import com.ktome.client.input.CommandSource
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputHandlerCommandSource
import com.ktome.client.input.InputSource
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
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.i18n.Localizer
import java.nio.file.Path

class GameApp(
    private val saveManager: SaveManager = SaveManager(defaultSaveDir()),
    private val defaultConfig: FoundationGameConfig = FoundationGameConfig(),
    private val menuInputSourceFactory: () -> InputSource = { GdxInputSource },
    private val gameCommandSourceFactory: () -> CommandSource = { InputHandlerCommandSource() },
    private val outcomeInputSourceFactory: () -> InputSource = { GdxInputSource },
    private val renderEnabled: Boolean = true,
    private val assetVersionProvider: () -> AssetVersionContract = AssetVersionResourceLoader::load,
    private val visualManifestProvider: () -> VisualManifest = VisualManifestResourceLoader::load,
    private val audioManifestProvider: () -> AudioManifest = AudioManifestResourceLoader::load,
    private val assetVersionGate: AssetVersionGate = AssetVersionGate(),
    initialLocale: GameLocale = GameLocale.EN_US,
    localizationBundle: LocalizationBundle = LocalizationBundle.load(),
) : Game() {
    private val lifecycle = LifecycleCoordinator(saveManager)
    private val assetContracts =
        AssetContractCoordinator(
            assetVersionProvider = assetVersionProvider,
            visualManifestProvider = visualManifestProvider,
            audioManifestProvider = audioManifestProvider,
            assetVersionGate = assetVersionGate,
        )
    private val localizationBundle = localizationBundle
    private var currentLocale: GameLocale = initialLocale
    private var currentLocalizer: Localizer = localizationBundle.translator(initialLocale)
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
                GameModule.newFoundationSession(config = defaultConfig, saveManager = saveManager, locale = currentLocale)
            }
        activeSession = session
        assetContracts.prepareSession(session.renderSnapshot())
        replaceScreen(FoundationGameScreen(this, session, requireClientAssets(), gameCommandSourceFactory(), renderEnabled))
    }

    fun continueGame() {
        if (!ensureAssetContracts()) {
            return
        }
        val session =
            lifecycle.continueSession {
                GameModule.loadFoundationSession(saveManager, locale = currentLocale)
            } ?: run {
                showMainMenu(saveCurrent = false, notice = lifecycle.consumeNotice())
                return
            }
        activeSession = session
        assetContracts.prepareSession(session.renderSnapshot())
        replaceScreen(FoundationGameScreen(this, session, requireClientAssets(), gameCommandSourceFactory(), renderEnabled))
    }

    fun showOutcome(session: FoundationGameSession) {
        val summary = session.runSummary() ?: return
        activeSession = null
        replaceScreen(
            if (session.isVictory()) {
                VictoryScreen(this, summary, outcomeInputSourceFactory(), renderEnabled)
            } else {
                GameOverScreen(this, summary, outcomeInputSourceFactory(), renderEnabled)
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
        replaceScreen(MainMenuScreen(this, continueEnabled, notice ?: lifecycle.consumeNotice(), menuInputSourceFactory(), renderEnabled))
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
        assetContracts.noticeOrNull()

    internal fun activeSessionOrNull(): FoundationGameSession? = activeSession

    internal fun currentLocale(): GameLocale = currentLocale

    internal fun localizer(): Localizer = currentLocalizer

    internal fun cycleLocale(): GameLocale {
        currentLocale = currentLocale.cycle()
        currentLocalizer = localizationBundle.translator(currentLocale)
        return currentLocale
    }

    internal fun text(
        key: String,
        vararg args: Pair<String, Any?>,
    ): String = currentLocalizer.text(key, *args)

    internal fun warmSessionAssets(snapshot: com.ktome.core.snapshot.RenderSnapshot) {
        assetContracts.warmCache(snapshot)
    }

    private fun ensureAssetContracts(): Boolean =
        assetContractNotice()?.let { notice ->
            showMainMenu(saveCurrent = false, notice = notice)
            false
        } ?: true

    private fun requireClientAssets(): ClientAssetBundle =
        requireNotNull(assetContracts.bundleOrNull()) {
            "Client assets must be loaded before entering the game screen."
        }

    companion object {
        private fun defaultSaveDir(): Path = Path.of(System.getProperty("user.home"), ".ktome")
    }
}

internal class AssetContractCoordinator(
    private val assetVersionProvider: () -> AssetVersionContract,
    private val visualManifestProvider: () -> VisualManifest,
    private val audioManifestProvider: () -> AudioManifest,
    private val assetVersionGate: AssetVersionGate = AssetVersionGate(),
) {
    private var cachedBundle: ClientAssetBundle? = null
    private var loadStrategy: ClientAssetLoadStrategy? = null

    fun noticeOrNull(): String? =
        try {
            assetVersionGate.requireCompatible(assetVersionProvider())
            cachedBundle =
                ClientAssetBundleLoader.load(
                    visualManifestProvider = visualManifestProvider,
                    audioManifestProvider = audioManifestProvider,
                )
            loadStrategy = requireNotNull(cachedBundle).let { bundle ->
                ClientAssetLoadStrategy(bundle).also(ClientAssetLoadStrategy::bootstrapLoad)
            }
            null
        } catch (exception: AssetVersionMismatchException) {
            cachedBundle = null
            loadStrategy = null
            exception.message
        } catch (exception: AssetVersionLoadException) {
            cachedBundle = null
            loadStrategy = null
            exception.message
        } catch (exception: ManifestLoadException) {
            cachedBundle = null
            loadStrategy = null
            exception.message
        }

    fun bundleOrNull(): ClientAssetBundle? = cachedBundle

    fun prepareSession(snapshot: com.ktome.core.snapshot.RenderSnapshot) {
        requireNotNull(loadStrategy) {
            "Bootstrap asset load must complete before preparing a gameplay session."
        }.sessionLoad(snapshot)
    }

    fun warmCache(snapshot: com.ktome.core.snapshot.RenderSnapshot) {
        loadStrategy?.warmCache(snapshot)
    }

    internal fun loadStateOrNull(): com.ktome.client.assets.AssetLoadStateSnapshot? = loadStrategy?.stateSnapshot()
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
