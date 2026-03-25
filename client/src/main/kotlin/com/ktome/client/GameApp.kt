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
import com.ktome.client.audio.AudioSinkBindingsFactory
import com.ktome.client.audio.AudioRouter
import com.ktome.client.audio.DefaultAudioSinkBindingsFactory
import com.ktome.client.input.AudioRouterAwareCommandSource
import com.ktome.client.input.CommandSource
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputHandlerCommandSource
import com.ktome.client.input.InputSource
import com.ktome.client.screen.FoundationGameScreen
import com.ktome.client.screen.GameOverScreen
import com.ktome.client.screen.MainMenuScreen
import com.ktome.client.screen.VictoryScreen
import com.ktome.core.combat.CombatRuleset
import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.profile.AdvancedClassUnlockRule
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.core.profile.ProfileData
import com.ktome.core.profile.ProfileManager
import com.ktome.core.profile.ProfileProgression
import com.ktome.core.profile.RunSummary as ProfileRunSummary
import com.ktome.core.save.AssetVersionContract
import com.ktome.core.save.AssetVersionGate
import com.ktome.core.save.AssetVersionMismatchException
import com.ktome.core.save.SaveLoadException
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.ProfessionSelectionOption
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.i18n.Localizer
import java.nio.file.Path

class GameApp(
    private val saveManager: SaveManager = SaveManager(defaultSaveDir()),
    private val defaultConfig: FoundationGameConfig = FoundationGameConfig(),
    availableProfessionIdsProvider: (GameLocale) -> List<String> = { locale -> GameModule.availableProfessionIds(locale) },
    private val professionSelectionProvider:
        (GameLocale, ProfileData, AvailabilityContext) -> List<ProfessionSelectionOption> =
            { locale, profile, context -> GameModule.professionSelections(locale, profile, context) },
    private val profileManager: ProfileManager = ProfileManager(defaultSaveDir().resolve("profile")),
    private val menuInputSourceFactory: () -> InputSource = { GdxInputSource },
    private val gameCommandSourceFactory: () -> CommandSource = { InputHandlerCommandSource() },
    private val outcomeInputSourceFactory: () -> InputSource = { GdxInputSource },
    private val renderEnabled: Boolean = true,
    private val assetVersionProvider: () -> AssetVersionContract = AssetVersionResourceLoader::load,
    private val visualManifestProvider: () -> VisualManifest = VisualManifestResourceLoader::load,
    private val audioManifestProvider: () -> AudioManifest = AudioManifestResourceLoader::load,
    private val assetVersionGate: AssetVersionGate = AssetVersionGate(),
    private val audioSinkBindingsFactory: AudioSinkBindingsFactory = DefaultAudioSinkBindingsFactory,
    initialLocale: GameLocale = GameLocale.DEFAULT,
    localizationBundle: LocalizationBundle = LocalizationBundle.load(),
) : Game() {
    private val initialProfileState =
        loadProfilePersistenceState(
            profileManager = profileManager,
            localizer = localizationBundle.translator(initialLocale),
        )
    private val availableProfessionIdsProvider = availableProfessionIdsProvider
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
    private var profileData: ProfileData = initialProfileState.profileData
    private var profilePersistenceEnabled: Boolean = initialProfileState.persistenceEnabled
    private var professionSelections: List<ProfessionSelectionOption> =
        resolveProfessionSelections(locale = initialLocale)
    private var selectedProfessionId: String =
        defaultConfig.playerProfessionId.takeIf(::containsProfession)
            ?: professionSelections.firstOrNull { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }?.id
            ?: professionSelections.first().id
    private var activeSession: FoundationGameSession? = null
    private var pendingMenuNotice: String? = initialProfileState.notice
    private val audioSinks = audioSinkBindingsFactory.create(renderEnabled)

    override fun create() {
        showMainMenu(saveCurrent = false, notice = assetContractNotice())
    }

    fun startNewGame(professionId: String = selectedProfessionId) {
        if (!ensureAssetContracts()) {
            return
        }
        refreshProfessionSelections()
        val selectedOption =
            professionSelections.firstOrNull { option -> option.id == professionId }
                ?: professionSelections.firstOrNull { option -> option.id == selectedProfessionId }
                ?: professionSelections.first()
        if (selectedOption.playabilityState != ClassPlayabilityState.PLAYABLE) {
            selectedProfessionId = selectedOption.id
            showMainMenu(saveCurrent = false, notice = professionSelectionNotice(selectedOption))
            return
        }
        val resolvedProfessionId = selectedOption.id
        selectedProfessionId = resolvedProfessionId
        val session =
            lifecycle.startNewSession {
                GameModule.newFoundationSession(
                    config = defaultConfig.copy(playerProfessionId = resolvedProfessionId),
                    saveManager = saveManager,
                    locale = currentLocale,
                    profile = profileData,
                    availabilityContext = AvailabilityContext.PLAYER_CREATION,
                )
            }
        activeSession = session
        assetContracts.prepareSession(session.renderSnapshot())
        replaceScreen(FoundationGameScreen(this, session, requireClientAssets(), prepareCommandSource(requireClientAssets(), gameCommandSourceFactory()), renderEnabled))
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
        replaceScreen(FoundationGameScreen(this, session, requireClientAssets(), prepareCommandSource(requireClientAssets(), gameCommandSourceFactory()), renderEnabled))
    }

    fun showOutcome(session: FoundationGameSession) {
        val summary = session.runSummary() ?: return
        recordProfileRun(session)
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
        refreshProfessionSelections()
        val continueEnabled = lifecycle.refreshContinueAvailability()
        replaceScreen(
            MainMenuScreen(
                app = this,
                continueEnabled = continueEnabled,
                availableProfessionIds = professionSelections.map(ProfessionSelectionOption::id),
                professionSelections = professionSelections,
                selectedProfessionId = selectedProfessionId,
                notice = notice ?: pendingMenuNotice ?: lifecycle.consumeNotice(),
                inputSource = menuInputSourceFactory(),
                renderEnabled = renderEnabled,
            ),
        )
        pendingMenuNotice = null
    }

    override fun dispose() {
        activeSession?.saveOnExit()
        audioSinks.dispose()
        assetContracts.dispose()
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

    internal fun rememberProfessionSelection(professionId: String) {
        if (containsProfession(professionId)) {
            selectedProfessionId = professionId
        }
    }

    internal fun cycleLocale(): GameLocale {
        currentLocale = currentLocale.cycle()
        currentLocalizer = localizationBundle.translator(currentLocale)
        refreshProfessionSelections()
        return currentLocale
    }

    internal fun text(
        key: String,
        vararg args: Pair<String, Any?>,
    ): String = currentLocalizer.text(key, *args)

    internal fun text(token: RenderTextTokenSnapshot): String =
        currentLocalizer.text(
            token.key,
            *token.arguments.map { argument -> argument.name to renderTextArgument(argument) }.toTypedArray(),
        )

    private fun renderTextArgument(argument: RenderTextArgumentSnapshot): String =
        argument.valueToken?.let(::text)
            ?: argument.valueKey?.let(currentLocalizer::text)
            ?: argument.value.orEmpty()

    internal fun warmSessionAssets(snapshot: com.ktome.core.snapshot.RenderSnapshot) {
        assetContracts.warmCache(snapshot)
    }

    internal fun audioRouterOrNull(): AudioRouter? =
        assetContracts.bundleOrNull()?.let { bundle ->
            AudioRouter(
                bundle.audioResolver,
                audioSinks.cueSink,
                audioSinks.backgroundSink,
            )
        }

    private fun ensureAssetContracts(): Boolean =
        assetContractNotice()?.let { notice ->
            showMainMenu(saveCurrent = false, notice = notice)
            false
        } ?: true

    private fun refreshProfessionSelections() {
        professionSelections = resolveProfessionSelections(locale = currentLocale)
        if (!containsProfession(selectedProfessionId)) {
            selectedProfessionId =
                professionSelections.firstOrNull { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }?.id
                    ?: professionSelections.first().id
        }
    }

    private fun resolveProfessionSelections(locale: GameLocale): List<ProfessionSelectionOption> {
        val baselineIds =
            availableProfessionIdsProvider(locale)
                .distinct()
                .ifEmpty { listOf(defaultConfig.playerProfessionId) }
        val catalogById =
            professionSelectionProvider(locale, profileData, AvailabilityContext.PLAYER_CREATION)
                .associateBy(ProfessionSelectionOption::id)
        return baselineIds.map { professionId ->
            catalogById[professionId]
                ?: ProfessionSelectionOption(
                    id = professionId,
                    tier = com.ktome.core.profession.ProfessionTier.BASE,
                    unlockState = com.ktome.core.profile.ClassUnlockState.RELEASE_UNLOCKED,
                    playabilityState = ClassPlayabilityState.PLAYABLE,
                )
        }
    }

    private fun containsProfession(professionId: String): Boolean =
        professionSelections.any { option -> option.id == professionId }

    private fun professionSelectionNotice(option: ProfessionSelectionOption): String =
        when (option.playabilityState) {
            ClassPlayabilityState.PLAYABLE -> ""
            ClassPlayabilityState.LOCKED -> text("ui.menu.profession_locked", "profession" to text("profession.${option.id}.name"))
            ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE ->
                text("ui.menu.profession_unavailable", "profession" to text("profession.${option.id}.name"))
        }

    private fun recordProfileRun(session: FoundationGameSession) {
        val result =
            appendAndPersistProfileRun(
                profileManager = profileManager,
                profile = profileData,
                persistenceEnabled = profilePersistenceEnabled,
                summary = profileRunSummary(session),
                unlockRules = GameModule.advancedClassUnlockRules(currentLocale),
                localizer = currentLocalizer,
            )
        profileData = result.profileData
        pendingMenuNotice = result.notice
        if (result.persisted) {
            refreshProfessionSelections()
        }
    }

    private fun profileRunSummary(session: FoundationGameSession): ProfileRunSummary =
        ProfileRunSummary(
            seed = session.config.seed,
            finishedAtEpochMillis = System.currentTimeMillis(),
            classId = session.config.playerProfessionId,
            raceId = session.config.playerRaceId,
            finalZoneId = session.config.zoneId,
            turnCount = session.currentTurnCount(),
            headlessTurnEquivalent = session.currentTurnCount(),
            zoneRouteHash = session.config.zoneRoute.joinToString(">"),
            buildHash = PROFILE_BUILD_HASH,
            rulesetVersion = CombatRuleset.RULESET_VERSION,
            victory = session.isVictory(),
            defeatReason = session.runOutcome().takeUnless { outcome -> session.isVictory() }?.toString(),
        )

    private fun requireClientAssets(): ClientAssetBundle =
        requireNotNull(assetContracts.bundleOrNull()) {
            "Client assets must be loaded before entering the game screen."
        }

    private fun prepareCommandSource(
        assets: ClientAssetBundle,
        commandSource: CommandSource,
    ): CommandSource =
        commandSource.also { source ->
            if (source is AudioRouterAwareCommandSource && source.audioRouter == null) {
                source.audioRouter =
                    AudioRouter(
                        assets.audioResolver,
                        audioSinks.cueSink,
                        audioSinks.backgroundSink,
                    )
            }
        }

    companion object {
        private const val PROFILE_BUILD_HASH: String = "ktome-0.1.0"

        private fun defaultSaveDir(): Path = Path.of(System.getProperty("user.home"), ".ktome")
    }
}

internal data class LoadedProfilePersistenceState(
    val profileData: ProfileData,
    val persistenceEnabled: Boolean,
    val notice: String? = null,
)

internal data class PersistedProfileRunResult(
    val profileData: ProfileData,
    val persisted: Boolean,
    val notice: String? = null,
)

internal fun loadProfilePersistenceState(
    profileManager: ProfileManager,
    localizer: Localizer,
): LoadedProfilePersistenceState =
    runCatching { profileManager.load() }
        .fold(
            onSuccess = { profile -> LoadedProfilePersistenceState(profileData = profile, persistenceEnabled = true) },
            onFailure = {
                LoadedProfilePersistenceState(
                    profileData = ProfileData(),
                    persistenceEnabled = false,
                    notice = localizer.text("ui.menu.profile_load_failed"),
                )
            },
        )

internal fun appendAndPersistProfileRun(
    profileManager: ProfileManager,
    profile: ProfileData,
    persistenceEnabled: Boolean,
    summary: ProfileRunSummary,
    unlockRules: Iterable<AdvancedClassUnlockRule>,
    localizer: Localizer,
): PersistedProfileRunResult {
    if (!persistenceEnabled) {
        return PersistedProfileRunResult(
            profileData = profile,
            persisted = false,
            notice = localizer.text("ui.menu.profile_load_failed"),
        )
    }
    val updatedProfile =
        ProfileProgression.appendRun(
            profile = profile,
            summary = summary,
            unlockRules = unlockRules,
        )
    return if (profileManager.save(updatedProfile)) {
        PersistedProfileRunResult(profileData = updatedProfile, persisted = true)
    } else {
        PersistedProfileRunResult(
            profileData = profile,
            persisted = false,
            notice = localizer.text("ui.menu.profile_save_failed"),
        )
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
            dispose()
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
            dispose()
            exception.message
        } catch (exception: AssetVersionLoadException) {
            dispose()
            exception.message
        } catch (exception: ManifestLoadException) {
            dispose()
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

    fun dispose() {
        cachedBundle?.dispose()
        cachedBundle = null
        loadStrategy = null
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
