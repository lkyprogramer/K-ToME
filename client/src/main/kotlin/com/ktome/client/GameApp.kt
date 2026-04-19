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
import com.ktome.client.input.ValidationCommandSource
import com.ktome.client.screen.FoundationGameScreen
import com.ktome.client.screen.GameOverScreen
import com.ktome.client.screen.MainMenuScreen
import com.ktome.client.screen.ValidationSetupContext
import com.ktome.client.screen.ValidationSetupScreen
import com.ktome.client.screen.ValidationZoneOption
import com.ktome.client.screen.VictoryScreen
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
import com.ktome.game.PlayerCreationSelection
import com.ktome.game.PlayerCreationState
import com.ktome.game.contentpack.ContentPackLoadException
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.i18n.Localizer
import com.ktome.game.validation.ProfileRunPersistenceMode
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationPaths
import com.ktome.game.validation.ValidationSessionRequest
import com.ktome.game.validation.ValidationSessionOptions
import com.ktome.game.validation.loadPersistedValidationSessionOptions
import com.ktome.game.validation.validationSessionOptionsForPreset
import java.nio.file.Files
import java.nio.file.Path

class GameApp(
    private val saveManager: SaveManager = SaveManager(defaultSaveDir()),
    private val validationSaveManager: SaveManager = SaveManager(ValidationPaths.saveDir()),
    private val defaultConfig: FoundationGameConfig = FoundationGameConfig(),
    private val contentPackSelection: ContentPackSelection = ContentPackSelection.EMPTY,
    private val playerCreationStateProvider:
        (GameLocale, ProfileData, PlayerCreationSelection?) -> PlayerCreationState =
            { locale, profile, previousSelection ->
                GameModule.playerCreationState(
                    locale = locale,
                    profile = profile,
                    previousSelection = previousSelection,
                    context = AvailabilityContext.PLAYER_CREATION,
                )
            },
    private val profileManager: ProfileManager = ProfileManager(defaultSaveDir().resolve("profile")),
    private val validationProfileManager: ProfileManager = ProfileManager(ValidationPaths.profileDir()),
    private val menuInputSourceFactory: () -> InputSource = { GdxInputSource },
    private val gameCommandSourceFactory: () -> CommandSource = { InputHandlerCommandSource() },
    private val outcomeInputSourceFactory: () -> InputSource = { GdxInputSource },
    private val renderEnabled: Boolean = true,
    private val assetVersionProvider: () -> AssetVersionContract = AssetVersionResourceLoader::load,
    private val visualManifestProvider: () -> VisualManifest = VisualManifestResourceLoader::load,
    private val audioManifestProvider: () -> AudioManifest = AudioManifestResourceLoader::load,
    private val assetVersionGate: AssetVersionGate = AssetVersionGate(),
    private val audioSinkBindingsFactory: AudioSinkBindingsFactory = DefaultAudioSinkBindingsFactory,
    private val validationSamplePackSelectionProvider: () -> ContentPackSelection = { defaultValidationSamplePackSelection() },
    initialLocale: GameLocale = GameLocale.DEFAULT,
    localizationBundle: LocalizationBundle = LocalizationBundle.load(),
) : Game() {
    private val initialProfileState =
        loadProfilePersistenceState(
            profileManager = profileManager,
            localizer = localizationBundle.translator(initialLocale),
        )
    private val initialValidationProfileState =
        loadProfilePersistenceState(
            profileManager = validationProfileManager,
            localizer = localizationBundle.translator(initialLocale),
        )
    private val defaultPlayerCreationSelection =
        PlayerCreationSelection(
            professionId = defaultConfig.playerProfessionId,
            raceId = defaultConfig.playerRaceId,
        )
    private var activeContentPackSelection: ContentPackSelection = contentPackSelection
    private val lifecycle = LifecycleCoordinator(saveManager)
    private val validationLifecycle =
        LifecycleCoordinator(validationSaveManager) {
            loadPersistedValidationSessionOptions(validationSaveManager) != null
        }
    private val assetContracts =
        AssetContractCoordinator(
            assetVersionProvider = assetVersionProvider,
            visualManifestProvider = visualManifestProvider,
            audioManifestProvider = audioManifestProvider,
            clientAssetBundleProvider = {
                if (activeContentPackSelection.isEmpty) {
                    ClientAssetBundleLoader.load(
                        visualManifestProvider = visualManifestProvider,
                        audioManifestProvider = audioManifestProvider,
                    )
                } else {
                    ClientAssetBundleLoader.load(contentPackSelection = activeContentPackSelection)
                }
            },
            assetVersionGate = assetVersionGate,
        )
    private val localizationBundle = localizationBundle
    private var currentLocale: GameLocale = initialLocale
    private var currentLocalizer: Localizer = localizationBundle.translator(initialLocale)
    private var profileData: ProfileData = initialProfileState.profileData
    private var profilePersistenceEnabled: Boolean = initialProfileState.persistenceEnabled
    private var validationProfileData: ProfileData = initialValidationProfileState.profileData
    private var validationProfilePersistenceEnabled: Boolean = initialValidationProfileState.persistenceEnabled
    private var validationSetupOptions: ValidationSessionOptions =
        validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF)
    private var validationSetupNotice: String? = null
    private var playerCreationState: PlayerCreationState =
        resolvePlayerCreationState(
            locale = initialLocale,
            previousSelection = defaultPlayerCreationSelection,
        )
    private var activeSession: FoundationGameSession? = null
    private var activeSessionPath: SessionPath = SessionPath.STANDARD
    private var pendingMenuNotice: String? = initialProfileState.notice
    private val audioSinks = audioSinkBindingsFactory.create(renderEnabled)

    override fun create() {
        showMainMenu(saveCurrent = false, notice = assetContractNotice())
    }

    fun startNewGame(selection: PlayerCreationSelection = playerCreationState.selection) {
        activeContentPackSelection = contentPackSelection
        if (!ensureAssetContracts()) {
            return
        }
        val refreshedState = refreshPlayerCreationState(selection)
        if (!refreshedState.canStartNewGame()) {
            showMainMenu(saveCurrent = false, notice = playerCreationSelectionNotice(refreshedState))
            return
        }
        val session =
            lifecycle.startNewSession {
                GameModule.newFoundationSession(
                    config = playerCreationConfig(refreshedState.selection),
                    saveManager = saveManager,
                    locale = currentLocale,
                    contentPackSelection = contentPackSelection,
                    profile = profileData,
                    availabilityContext = AvailabilityContext.PLAYER_CREATION,
                )
            }
        activeSessionPath = SessionPath.STANDARD
        activeSession = session
        assetContracts.prepareSession(session.renderSnapshot())
        replaceScreen(FoundationGameScreen(this, session, requireClientAssets(), prepareCommandSource(requireClientAssets(), session, gameCommandSourceFactory()), renderEnabled))
    }

    fun continueGame() {
        activeContentPackSelection = contentPackSelection
        if (!ensureAssetContracts()) {
            return
        }
        val session =
            lifecycle.continueSession {
                GameModule.loadFoundationSession(
                    saveManager,
                    locale = currentLocale,
                    contentPackSelection = contentPackSelection,
                )
            } ?: run {
                showMainMenu(saveCurrent = false, notice = lifecycle.consumeNotice())
                return
            }
        activeSessionPath = SessionPath.STANDARD
        activeSession = session
        assetContracts.prepareSession(session.renderSnapshot())
        replaceScreen(FoundationGameScreen(this, session, requireClientAssets(), prepareCommandSource(requireClientAssets(), session, gameCommandSourceFactory()), renderEnabled))
    }

    internal fun showValidationSetup(
        options: ValidationSessionOptions = validationSetupOptions,
        notice: String? = validationSetupNotice,
    ) {
        validationSetupOptions = options
        replaceScreen(
            ValidationSetupScreen(
                app = this,
                context = validationSetupContext(options, notice),
                inputSource = menuInputSourceFactory(),
                renderEnabled = renderEnabled,
            ),
        )
        validationSetupNotice = null
    }

    internal fun startValidationSession(config: FoundationGameConfig = defaultConfig) {
        startValidationSession(
            validationSetupOptions.copy(
                foundationConfig = config,
                contentPackSelection = contentPackSelection,
            ),
        )
    }

    internal fun startValidationSession(options: ValidationSessionOptions) {
        validationSetupOptions = options
        activeContentPackSelection = options.contentPackSelection
        if (!ensureValidationAssetContracts(options)) {
            return
        }
        val session =
            try {
                validationLifecycle.startNewSession {
                    GameModule.newValidationSession(
                        ValidationSessionRequest(
                            saveManager = validationSaveManager,
                            locale = currentLocale,
                            profile = validationProfileData,
                            options = options,
                        ),
                    )
                }
            } catch (exception: IllegalArgumentException) {
                showValidationSetupFailure(options, exception)
                return
            } catch (exception: ContentPackLoadException) {
                showValidationSetupFailure(options, exception)
                return
            }
        activeSessionPath = SessionPath.VALIDATION
        activeSession = session
        try {
            assetContracts.prepareSession(session.renderSnapshot())
        } catch (exception: ManifestLoadException) {
            showValidationSetupFailure(options, exception)
            return
        } catch (exception: ContentPackLoadException) {
            showValidationSetupFailure(options, exception)
            return
        }
        replaceScreen(FoundationGameScreen(this, session, requireClientAssets(), prepareCommandSource(requireClientAssets(), session, gameCommandSourceFactory()), renderEnabled))
    }

    internal fun continueValidationSession() {
        continueValidationSession(validationSetupOptions)
    }

    internal fun continueValidationSession(options: ValidationSessionOptions) {
        val resolvedOptions =
            try {
                loadPersistedValidationSessionOptions(validationSaveManager)
            } catch (exception: SaveLoadException) {
                pendingMenuNotice = exception.message
                showMainMenu(saveCurrent = false, notice = pendingMenuNotice)
                return
            }
                ?: run {
                    showMainMenu(saveCurrent = false)
                    return
                }
        validationSetupOptions = resolvedOptions
        activeContentPackSelection = resolvedOptions.contentPackSelection
        if (!ensureValidationAssetContracts(resolvedOptions)) {
            return
        }
        val session =
            validationLifecycle.continueSession {
                GameModule.loadValidationSessionResolved(
                    saveManager = validationSaveManager,
                    locale = currentLocale,
                    resolvedOptions = resolvedOptions,
                    contentPackSelection = resolvedOptions.contentPackSelection,
                )
            } ?: run {
                pendingMenuNotice = validationLifecycle.consumeNotice()
                showMainMenu(saveCurrent = false, notice = pendingMenuNotice)
                return
            }
        activeSessionPath = SessionPath.VALIDATION
        activeSession = session
        assetContracts.prepareSession(session.renderSnapshot())
        replaceScreen(FoundationGameScreen(this, session, requireClientAssets(), prepareCommandSource(requireClientAssets(), session, gameCommandSourceFactory()), renderEnabled))
    }

    fun showOutcome(session: FoundationGameSession) {
        val summary = session.outcomeSummary() ?: return
        if (session.profileRunPersistenceMode() == ProfileRunPersistenceMode.WRITE_PROFILE_SUMMARY) {
            recordProfileRun(session)
        }
        activeSession = null
        activeSessionPath = SessionPath.STANDARD
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
        activeSessionPath = SessionPath.STANDARD
        activeContentPackSelection = contentPackSelection
        refreshPlayerCreationState()
        val continueEnabled = lifecycle.refreshContinueAvailability()
        replaceScreen(
            MainMenuScreen(
                app = this,
                continueEnabled = continueEnabled,
                playerCreationState = playerCreationState,
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

    internal fun standardContinueAvailableForTest(): Boolean = lifecycle.refreshContinueAvailability()

    internal fun validationContinueAvailableForTest(): Boolean = validationLifecycle.refreshContinueAvailability()

    internal fun currentLocale(): GameLocale = currentLocale

    internal fun localizer(): Localizer = currentLocalizer

    internal fun rememberValidationSetupOptions(options: ValidationSessionOptions) {
        validationSetupOptions = options
    }

    internal fun rememberPlayerCreationSelection(selection: PlayerCreationSelection) {
        playerCreationState =
            playerCreationState.copy(
                selection = PlayerCreationSelection(
                    professionId =
                        selection.professionId
                            .takeIf { optionId -> playerCreationState.professionOptions.any { option -> option.id == optionId } }
                            ?: playerCreationState.selection.professionId,
                    raceId =
                        selection.raceId
                            .takeIf { optionId -> playerCreationState.raceOptions.any { option -> option.id == optionId } }
                            ?: playerCreationState.selection.raceId,
                ),
            )
    }

    internal fun cycleLocale(): GameLocale {
        currentLocale = currentLocale.cycle()
        currentLocalizer = localizationBundle.translator(currentLocale)
        refreshPlayerCreationState()
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

    private fun ensureValidationAssetContracts(options: ValidationSessionOptions): Boolean =
        assetContractNotice()?.let { notice ->
            validationSetupNotice = notice
            showValidationSetup(options, notice)
            false
        } ?: true

    private fun refreshPlayerCreationState(
        previousSelection: PlayerCreationSelection = playerCreationState.selection,
    ): PlayerCreationState =
        resolvePlayerCreationState(currentLocale, previousSelection).also { refreshed ->
            playerCreationState = refreshed
        }

    private fun resolvePlayerCreationState(
        locale: GameLocale,
        previousSelection: PlayerCreationSelection? = null,
    ): PlayerCreationState =
        playerCreationStateProvider(locale, profileData, previousSelection)

    private fun validationSetupContext(
        options: ValidationSessionOptions,
        notice: String? = null,
    ): ValidationSetupContext {
        val schemaCatalog = DataLoader(locale = currentLocale).loadSchemaCatalog()
        return ValidationSetupContext(
            initialOptions = options,
            playerCreationState =
                GameModule.playerCreationState(
                    locale = currentLocale,
                    profile = validationProfileData,
                    previousSelection =
                        PlayerCreationSelection(
                            professionId = options.foundationConfig.playerProfessionId,
                            raceId = options.foundationConfig.playerRaceId,
                        ),
                    context = AvailabilityContext.WHITE_BOX,
                ),
            zones =
                schemaCatalog.zones.map { zone ->
                    ValidationZoneOption(
                        id = zone.id,
                        displayNameKey = zone.nameKey,
                        floorCount = zone.floorCount,
                    )
                },
            bossVariantIds = schemaCatalog.bossVariants.map { variant -> variant.id }.sorted(),
            samplePackSelection = validationSamplePackSelectionProvider(),
            continueEnabled = validationLifecycle.refreshContinueAvailability(),
            notice = notice,
        )
    }

    private fun showValidationSetupFailure(
        options: ValidationSessionOptions,
        exception: Exception,
    ) {
        validationSetupNotice = exception.message ?: exception::class.simpleName ?: "Validation setup failed."
        activeSession = null
        activeSessionPath = SessionPath.STANDARD
        activeContentPackSelection = contentPackSelection
        showValidationSetup(options, validationSetupNotice)
    }

    private fun playerCreationSelectionNotice(state: PlayerCreationState): String {
        val professionOption = state.selectedProfessionOption()
        if (professionOption.playabilityState != ClassPlayabilityState.PLAYABLE) {
            return when (professionOption.playabilityState) {
                ClassPlayabilityState.PLAYABLE -> ""
                ClassPlayabilityState.LOCKED ->
                    text("ui.menu.profession_locked", "profession" to text(professionOption.displayNameKey))

                ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE ->
                    text("ui.menu.profession_unavailable", "profession" to text(professionOption.displayNameKey))
            }
        }
        val raceOption = state.selectedRaceOption()
        return when (raceOption.playabilityState) {
            ClassPlayabilityState.PLAYABLE -> ""
            ClassPlayabilityState.LOCKED -> text("ui.menu.race_locked", "race" to text(raceOption.displayNameKey))
            ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE ->
                text("ui.menu.race_unavailable", "race" to text(raceOption.displayNameKey))
        }
    }

    private fun playerCreationConfig(selection: PlayerCreationSelection): FoundationGameConfig =
        newGameConfig(
            defaultConfig = defaultConfig,
            professionId = selection.professionId,
            raceId = selection.raceId,
        )

    private fun recordProfileRun(session: FoundationGameSession) {
        val target =
            when (activeSessionPath) {
                SessionPath.STANDARD ->
                    ActiveProfileTarget(
                        profileManager = profileManager,
                        profile = profileData,
                        persistenceEnabled = profilePersistenceEnabled,
                    )

                SessionPath.VALIDATION ->
                    ActiveProfileTarget(
                        profileManager = validationProfileManager,
                        profile = validationProfileData,
                        persistenceEnabled = validationProfilePersistenceEnabled,
                    )
            }
        val result =
            appendAndPersistProfileRun(
                profileManager = target.profileManager,
                profile = target.profile,
                persistenceEnabled = target.persistenceEnabled,
                summary = requireNotNull(session.profileRunSummary(System.currentTimeMillis())),
                unlockRules = GameModule.advancedClassUnlockRules(currentLocale),
                localizer = currentLocalizer,
            )
        when (activeSessionPath) {
            SessionPath.STANDARD -> {
                profileData = result.profileData
                pendingMenuNotice = result.notice
                if (result.persisted) {
                    refreshPlayerCreationState()
                }
            }

            SessionPath.VALIDATION -> {
                validationProfileData = result.profileData
            }
        }
    }

    private fun requireClientAssets(): ClientAssetBundle =
        requireNotNull(assetContracts.bundleOrNull()) {
            "Client assets must be loaded before entering the game screen."
        }

    private fun prepareCommandSource(
        assets: ClientAssetBundle,
        session: FoundationGameSession,
        commandSource: CommandSource,
    ): CommandSource {
        val effectiveSource =
            if (!session.isValidationSession()) {
                commandSource
            } else if (commandSource is InputHandlerCommandSource) {
                ValidationCommandSource(session)
            } else {
                ValidationCommandSource(session, commandSource)
            }
        return effectiveSource.also { source ->
            if (source is AudioRouterAwareCommandSource && source.audioRouter == null) {
                source.audioRouter =
                    AudioRouter(
                        assets.audioResolver,
                        audioSinks.cueSink,
                        audioSinks.backgroundSink,
                    )
            }
        }
    }

    companion object {
        private fun defaultSaveDir(): Path = Path.of(System.getProperty("user.home"), ".ktome")

        private fun defaultValidationSamplePackSelection(): ContentPackSelection {
            return resolveValidationSamplePackSelection()
        }
    }
}

private const val validationSamplePackRelativePath: String = "examples/content-packs/sample.flooded_relics"
private const val bundledValidationSamplePackRelativePath: String = "content-packs/sample.flooded_relics"

internal fun resolveValidationSamplePackSelection(
    candidateRoots: List<Path> = validationSamplePackCandidateRoots(),
): ContentPackSelection {
    val packRoot =
        candidateRoots.firstOrNull { candidate ->
            Files.isRegularFile(candidate.resolve("manifest.yaml"))
        } ?: return ContentPackSelection.EMPTY
    return ContentPackSelection.of(packRoot)
}

internal fun validationSamplePackCandidateRoots(
    overrideRoot: String? = System.getProperty("ktome.validationSamplePackRoot"),
    repoRoot: String? = System.getProperty("ktome.repo.root"),
    runtimeAppDir: Path? = runtimeApplicationDir(),
    workingDir: Path = Path.of(".").toAbsolutePath().normalize(),
): List<Path> {
    val candidates = linkedSetOf<Path>()
    parseNormalizedPath(overrideRoot)?.let(candidates::add)
    parseNormalizedPath(repoRoot)?.resolve(validationSamplePackRelativePath)?.normalize()?.let(candidates::add)
    runtimeAppDir?.let { appDir ->
        candidates.add(appDir.resolve(bundledValidationSamplePackRelativePath).normalize())
        appDir.parent?.resolve(bundledValidationSamplePackRelativePath)?.normalize()?.let(candidates::add)
    }
    candidates.add(workingDir.resolve(validationSamplePackRelativePath).normalize())
    return candidates.toList()
}

private fun runtimeApplicationDir(): Path? =
    runCatching {
        Path.of(GameApp::class.java.protectionDomain.codeSource.location.toURI())
    }.getOrNull()?.let { codeSourcePath ->
        if (Files.isRegularFile(codeSourcePath)) {
            codeSourcePath.parent
        } else {
            codeSourcePath
        }
    }

private fun parseNormalizedPath(rawPath: String?): Path? =
    rawPath
        ?.takeIf(String::isNotBlank)
        ?.let { value ->
            runCatching {
                Path.of(value).toAbsolutePath().normalize()
            }.getOrNull()
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

private data class ActiveProfileTarget(
    val profileManager: ProfileManager,
    val profile: ProfileData,
    val persistenceEnabled: Boolean,
)

private enum class SessionPath {
    STANDARD,
    VALIDATION,
}

internal fun newGameConfig(
    defaultConfig: FoundationGameConfig,
    professionId: String,
    raceId: String,
): FoundationGameConfig =
    defaultConfig.copy(
        playerProfessionId = professionId,
        playerRaceId = raceId,
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
    private val clientAssetBundleProvider: (() -> ClientAssetBundle)? = null,
    private val assetVersionGate: AssetVersionGate = AssetVersionGate(),
) {
    private var cachedBundle: ClientAssetBundle? = null
    private var loadStrategy: ClientAssetLoadStrategy? = null

    fun noticeOrNull(): String? =
        try {
            dispose()
            assetVersionGate.requireCompatible(assetVersionProvider())
            cachedBundle =
                clientAssetBundleProvider?.invoke()
                    ?: ClientAssetBundleLoader.load(
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
        } catch (exception: ContentPackLoadException) {
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
    private val continueAvailabilityProbe: () -> Boolean = { saveManager.load() != null },
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
            continueAvailabilityProbe()
        } catch (exception: SaveLoadException) {
            pendingNotice = exception.message
            false
        } catch (_: Exception) {
            false
        }
}
