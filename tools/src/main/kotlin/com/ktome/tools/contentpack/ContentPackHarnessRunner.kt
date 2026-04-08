package com.ktome.tools.contentpack

import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.client.assets.ManifestLoadException
import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.phase.PackId
import com.ktome.core.world.solvability.ContentRef
import com.ktome.core.world.solvability.RegistryId
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.contentpack.ContentPackFixtureCatalog
import com.ktome.game.contentpack.ContentPackLoadException
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.contentpack.OverlayOp
import com.ktome.game.contentpack.repoRoot
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.MonsterSchemaV2
import com.ktome.game.i18n.GameLocale
import com.ktome.tools.mapgen.phase4HarnessHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.SerializationException

data class ContentPackHarnessRun(
    val totalCases: Int,
    val failureCount: Int,
    val summaryPath: Path,
    val runsPath: Path,
)

data class ContentPackCaseResult(
    val fixtureId: String,
    val packId: String?,
    val activePackIds: List<String>,
    val activePackManifestVersions: Map<String, String>,
    val seedList: List<Long>,
    val resolvedOrder: List<String>,
    val overlayOps: List<String>,
    val expectedFailureCodes: List<String>,
    val diagnosticCodes: List<String>,
    val diagnostics: List<String>,
    val localeKeysResolved: List<String>,
    val visualKeysResolved: List<String>,
    val audioKeysResolved: List<String>,
    val localeResolutionFailureCount: Int,
    val visualResolutionFailureCount: Int,
    val audioResolutionFailureCount: Int,
    val headlessRunSucceeded: Boolean,
    val fallbackToBaseVerified: Boolean,
    val registryEntryPresent: Boolean,
    val registryEntryNameKey: String?,
    val registryEntryVisualKey: String?,
    val registryEntryAudioProfile: String?,
    val precedenceVerified: Boolean,
    val success: Boolean,
    val failureReasons: List<String>,
) {
    fun toJson(header: HarnessReportHeader): JsonObject =
        buildJsonObject {
            put("buildId", header.buildId)
            put("phaseId", header.phaseId)
            put("locale", header.locale)
            put("overlayContractVersion", header.overlayContractVersion)
            put("fixtureId", fixtureId)
            put("packId", packId)
            putJsonArray("activePackIds") { activePackIds.forEach { packId -> add(JsonPrimitive(packId)) } }
            putJsonObject("activePackManifestVersions") {
                activePackManifestVersions.forEach { (packId, version) -> put(packId, version) }
            }
            putJsonArray("seedList") { seedList.forEach { seed -> add(JsonPrimitive(seed)) } }
            putJsonArray("resolvedOrder") { resolvedOrder.forEach { packId -> add(JsonPrimitive(packId)) } }
            putJsonArray("overlayOps") { overlayOps.forEach { op -> add(JsonPrimitive(op)) } }
            putJsonArray("expectedFailureCodes") { expectedFailureCodes.forEach { code -> add(JsonPrimitive(code)) } }
            putJsonArray("diagnosticCodes") { diagnosticCodes.forEach { code -> add(JsonPrimitive(code)) } }
            putJsonArray("diagnostics") { diagnostics.forEach { diagnostic -> add(JsonPrimitive(diagnostic)) } }
            putJsonArray("localeKeysResolved") { localeKeysResolved.forEach { key -> add(JsonPrimitive(key)) } }
            putJsonArray("visualKeysResolved") { visualKeysResolved.forEach { key -> add(JsonPrimitive(key)) } }
            putJsonArray("audioKeysResolved") { audioKeysResolved.forEach { key -> add(JsonPrimitive(key)) } }
            put("localeResolutionFailureCount", localeResolutionFailureCount)
            put("visualResolutionFailureCount", visualResolutionFailureCount)
            put("audioResolutionFailureCount", audioResolutionFailureCount)
            put("headlessRunSucceeded", headlessRunSucceeded)
            put("fallbackToBaseVerified", fallbackToBaseVerified)
            put("registryEntryPresent", registryEntryPresent)
            put("registryEntryNameKey", registryEntryNameKey)
            put("registryEntryVisualKey", registryEntryVisualKey)
            put("registryEntryAudioProfile", registryEntryAudioProfile)
            put("precedenceVerified", precedenceVerified)
            put("success", success)
            putJsonArray("failureReasons") { failureReasons.forEach { reason -> add(JsonPrimitive(reason)) } }
        }
}

internal data class ContentPackSummaryMetrics(
    val totalCases: Int,
    val caseFailureCount: Int,
    val aggregateFailureCount: Int,
    val successfulRuntimeCaseCount: Int,
    val expectedFailureCaseCount: Int,
    val diagnosticMismatchCount: Int,
    val localeResolutionFailureCount: Int,
    val visualResolutionFailureCount: Int,
    val audioResolutionFailureCount: Int,
    val headlessRunFailureCount: Int,
    val fallbackFailureCount: Int,
    val precedenceFailureCount: Int,
) {
    val failureCount: Int
        get() = caseFailureCount + aggregateFailureCount
}

internal data class ContentPackAnalysis(
    val summary: ContentPackSummaryMetrics,
    val aggregateFailures: List<String>,
)

internal data class ContentPackKernelRun(
    val header: HarnessReportHeader,
    val results: List<ContentPackCaseResult>,
    val analysis: ContentPackAnalysis,
)

object ContentPackHarnessRunner {
    private const val SUMMARY_FILE: String = "content-pack-summary.json"
    private const val RUNS_FILE: String = "content-pack-runs.jsonl"
    private val json: Json = Json { prettyPrint = true }
    private val lineJson: Json = Json { prettyPrint = false }
    private val targetMonsterRef: ContentRef = ContentRef(registry = RegistryId("monster"), id = "fixture.add_monster.flooded_rat")
    private val baseLocale: GameLocale = GameLocale.EN_US

    fun run(): ContentPackHarnessRun {
        val kernelRun = executeKernel()
        val reportDir = reportDir()
        Files.createDirectories(reportDir)
        val summaryPath = reportDir.resolve(SUMMARY_FILE)
        val runsPath = reportDir.resolve(RUNS_FILE)
        Files.writeString(
            summaryPath,
            json.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(header = kernelRun.header, results = kernelRun.results, analysis = kernelRun.analysis),
            ),
        )
        Files.writeString(
            runsPath,
            kernelRun.results.joinToString(separator = "\n") { result ->
                lineJson.encodeToString(JsonElement.serializer(), result.toJson(kernelRun.header))
            } + "\n",
        )
        return ContentPackHarnessRun(
            totalCases = kernelRun.results.size,
            failureCount = kernelRun.analysis.summary.failureCount,
            summaryPath = summaryPath,
            runsPath = runsPath,
        )
    }

    internal fun executeKernel(): ContentPackKernelRun {
        val harnessSpec = ContentPackSidecarCatalog.loadHarnessSpec(ContentPackFixtureCatalog.addPackId)
        val scenarioCatalog =
            listOf(
                HarnessScenario(
                    fixtureId = "valid_add_pack",
                    activePackIds = listOf(ContentPackFixtureCatalog.addPackId),
                    seedList = harnessSpec.harnessSeeds,
                    expectedResolvedOrder = listOf(ContentPackFixtureCatalog.addPackId),
                    targetRef = targetMonsterRef,
                    expectedMonsterNameKey = "fixture_add_monster.monster.flooded_rat.name",
                    expectedMonsterVisualKey = "fixture_add_monster.actor.flooded_rat",
                    expectedMonsterAudioProfile = "fixture_add_monster.audio.flooded_rat",
                ),
                HarnessScenario(
                    fixtureId = "disabled_pack_fallback",
                    activePackIds = emptyList(),
                    targetRef = targetMonsterRef,
                    expectRegistryEntryPresent = false,
                    expectFallbackToBase = true,
                ),
                HarnessScenario(
                    fixtureId = "replace_precedence",
                    activePackIds = listOf(ContentPackFixtureCatalog.replacePackId),
                    seedList = harnessSpec.harnessSeeds,
                    expectedResolvedOrder = harnessSpec.dualPackScenarios.single().expectedOrder,
                    expectedOverlayOps = harnessSpec.dualPackScenarios.single().expectedOps,
                    targetRef = targetMonsterRef,
                    expectedMonsterNameKey = "fixture_replace_monster.monster.flooded_rat.name",
                    expectedMonsterVisualKey = "fixture_replace_monster.actor.flooded_rat",
                    expectedMonsterAudioProfile = "fixture_replace_monster.audio.flooded_rat",
                ),
                HarnessScenario(
                    fixtureId = "append_runtime_rejected",
                    activePackIds = listOf(ContentPackFixtureCatalog.appendPackId),
                    expectedFailureCodes = setOf("content-pack.overlay.runtime-op-forbidden"),
                ),
                HarnessScenario(
                    fixtureId = "deny_runtime_rejected",
                    activePackIds = listOf(ContentPackFixtureCatalog.denyPackId),
                    expectedFailureCodes = setOf("content-pack.overlay.runtime-op-forbidden"),
                ),
                HarnessScenario(
                    fixtureId = "missing_dependency",
                    activePackIds = listOf(ContentPackFixtureCatalog.missingDependencyPackId),
                    expectedFailureCodes = setOf("content-pack.dependency.missing"),
                ),
                HarnessScenario(
                    fixtureId = "dependency_cycle",
                    activePackIds = listOf(ContentPackFixtureCatalog.cyclePackAId),
                    expectedFailureCodes = setOf("content-pack.dependency.cycle"),
                ),
                HarnessScenario(
                    fixtureId = "version_range_conflict",
                    activePackIds = listOf(ContentPackFixtureCatalog.versionConflictPackId),
                    expectedFailureCodes = setOf("content-pack.version-range.conflict"),
                ),
                HarnessScenario(
                    fixtureId = "namespace_collision",
                    activePackIds =
                        listOf(
                            ContentPackFixtureCatalog.namespaceCollisionLeftId,
                            ContentPackFixtureCatalog.namespaceCollisionRightId,
                        ),
                    expectedFailureCodes = setOf("content-pack.namespace.collision"),
                ),
                HarnessScenario(
                    fixtureId = "same_priority_duplicate_target",
                    activePackIds =
                        listOf(
                            ContentPackFixtureCatalog.samePriorityLeftId,
                            ContentPackFixtureCatalog.samePriorityRightId,
                        ),
                    expectedFailureCodes = setOf("content-pack.overlay.same-priority-duplicate-target"),
                ),
                HarnessScenario(
                    fixtureId = "duplicate_without_replace",
                    activePackIds = listOf(ContentPackFixtureCatalog.duplicateWithoutReplacePackId),
                    expectedFailureCodes = setOf("content-pack.overlay.add-conflict"),
                ),
            ).associateBy(HarnessScenario::fixtureId)
        val fixtureOrder = harnessSpec.fixtureOrder.ifEmpty { scenarioCatalog.keys.toList() }
        require(fixtureOrder.toSet() == scenarioCatalog.keys) {
            "Content-pack harness fixtureOrder must match the scenario catalog exactly."
        }
        val results = fixtureOrder.map { fixtureId -> executeScenario(requireNotNull(scenarioCatalog[fixtureId])) }
        val analysis = analyze(results)
        val resolvedCases = results.filter { result -> result.success && result.activePackIds.isNotEmpty() }
        val header =
            phase4HarnessHeader(
                harnessId = "contentPackHarness",
                seedList = results.flatMap(ContentPackCaseResult::seedList).distinct(),
                locale = baseLocale.id,
                activePackIds = resolvedCases.flatMap { result -> result.activePackIds.map(::PackId) }.distinct(),
                activePackManifestVersions =
                    resolvedCases
                        .flatMap { result -> result.activePackManifestVersions.entries }
                        .associate { (packId, version) -> PackId(packId) to version },
            )
        return ContentPackKernelRun(
            header = header,
            results = results,
            analysis = analysis,
        )
    }

    private fun executeScenario(scenario: HarnessScenario): ContentPackCaseResult {
        val selection = ContentPackFixtureCatalog.availableSelection(scenario.activePackIds)
        var resolvedSelection: com.ktome.game.contentpack.ResolvedContentPackSelection? = null
        var resolvedOrder: List<String> = emptyList()
        var overlayOps: List<String> = emptyList()
        return try {
            val resolverResult = com.ktome.game.contentpack.ContentPackRuntimeResolver.resolve(selection)
            resolvedSelection = resolverResult
            resolvedOrder = resolverResult.orderedPacks.map { pack -> pack.id.value }
            overlayOps = resolverResult.orderedPacks.flatMap { pack -> pack.manifest.overlays.map { overlay -> overlay.op.name } }
            val loader = DataLoader(locale = baseLocale, packSelection = selection, preResolvedContentPackSelection = resolverResult)
            loader.localizer
            val catalog = loader.loadSchemaCatalog()
            val assetBundle = ClientAssetBundleLoader.load(resolverResult)
            try {
                val monster = scenario.targetRef?.let { targetRef -> catalog.monsters.firstOrNull { entry -> entry.id == targetRef.id } }
                val localeChecks =
                    monster?.let { entry ->
                        verifyLocaleKeys(localizer = loader.localizer, keys = listOf(entry.nameKey, entry.descKey))
                    } ?: ResolutionCheck.EMPTY
                val visualChecks =
                    monster?.let { entry ->
                        verifyVisualKeys(bundle = assetBundle, keys = listOf(entry.visualKey, entry.iconKey))
                    } ?: ResolutionCheck.EMPTY
                val audioChecks =
                    monster?.let { entry ->
                        verifyAudioKeys(bundle = assetBundle, keys = listOf(entry.audioProfile))
                    } ?: ResolutionCheck.EMPTY
                val headlessRunSucceeded =
                    if (scenario.seedList.isNotEmpty()) {
                        scenario.seedList.all { seed -> runHeadlessCase(selection = selection, seed = seed) }
                    } else {
                        false
                    }
                val fallbackToBaseVerified =
                    if (scenario.expectFallbackToBase) {
                        verifyBaseFallback()
                    } else {
                        false
                    }
                val precedenceVerified = precedenceMatches(scenario = scenario, resolvedOrder = resolvedOrder, overlayOps = overlayOps)
                val registryEntryPresent = monster != null
                val failureReasons =
                    buildList {
                        if (scenario.expectedFailureCodes.isNotEmpty()) {
                            add("case.expected_failure_but_runtime_succeeded")
                        }
                        if (!precedenceVerified) {
                            add("case.precedence_mismatch")
                        }
                        if (localeChecks.failureCount > 0) {
                            add("case.locale_key_unresolved")
                        }
                        if (visualChecks.failureCount > 0) {
                            add("case.visual_key_unresolved")
                        }
                        if (audioChecks.failureCount > 0) {
                            add("case.audio_key_unresolved")
                        }
                        scenario.targetRef?.let {
                            if (registryEntryPresent != scenario.expectRegistryEntryPresent) {
                                add("case.registry_entry_presence_mismatch")
                            }
                        }
                        scenario.expectedMonsterNameKey?.let { expected ->
                            if (monster?.nameKey != expected) add("case.registry_name_key_mismatch")
                        }
                        scenario.expectedMonsterVisualKey?.let { expected ->
                            if (monster?.visualKey != expected) add("case.registry_visual_key_mismatch")
                        }
                        scenario.expectedMonsterAudioProfile?.let { expected ->
                            if (monster?.audioProfile != expected) add("case.registry_audio_key_mismatch")
                        }
                        if (scenario.seedList.isNotEmpty() && !headlessRunSucceeded) {
                            add("case.headless_run_failed")
                        }
                        if (scenario.expectFallbackToBase && !fallbackToBaseVerified) {
                            add("case.base_fallback_not_verified")
                        }
                    }
                ContentPackCaseResult(
                    fixtureId = scenario.fixtureId,
                    packId = scenario.activePackIds.firstOrNull()?.value,
                    activePackIds = resolverResult.activePackIds.map(PackId::value),
                    activePackManifestVersions = resolverResult.activePackManifestVersions.mapKeys { (packId, _) -> packId.value },
                    seedList = scenario.seedList,
                    resolvedOrder = resolvedOrder,
                    overlayOps = overlayOps,
                    expectedFailureCodes = scenario.expectedFailureCodes.sorted(),
                    diagnosticCodes = emptyList(),
                    diagnostics = emptyList(),
                    localeKeysResolved = localeChecks.resolvedKeys,
                    visualKeysResolved = visualChecks.resolvedKeys,
                    audioKeysResolved = audioChecks.resolvedKeys,
                    localeResolutionFailureCount = localeChecks.failureCount,
                    visualResolutionFailureCount = visualChecks.failureCount,
                    audioResolutionFailureCount = audioChecks.failureCount,
                    headlessRunSucceeded = headlessRunSucceeded,
                    fallbackToBaseVerified = fallbackToBaseVerified,
                    registryEntryPresent = registryEntryPresent,
                    registryEntryNameKey = monster?.nameKey,
                    registryEntryVisualKey = monster?.visualKey,
                    registryEntryAudioProfile = monster?.audioProfile,
                    precedenceVerified = precedenceVerified,
                    success = failureReasons.isEmpty(),
                    failureReasons = failureReasons,
                )
            } finally {
                assetBundle.dispose()
            }
        } catch (exception: Exception) {
            val failure = classifyFailure(exception)
            val actualCodes = failure.diagnosticCodes.toSortedSet()
            val expectedCodes = scenario.expectedFailureCodes.toSortedSet()
            val resolvedActivePackIds = resolvedSelection?.activePackIds?.map(PackId::value) ?: scenario.activePackIds.map(PackId::value)
            val resolvedManifestVersions =
                resolvedSelection?.activePackManifestVersions?.mapKeys { (packId, _) -> packId.value }.orEmpty()
            val failureReasons =
                buildList {
                    if (expectedCodes.isEmpty()) {
                        add("case.runtime_failure")
                    }
                    if (actualCodes != expectedCodes) {
                        add("case.diagnostic_code_mismatch")
                    }
                }
            ContentPackCaseResult(
                fixtureId = scenario.fixtureId,
                packId = scenario.activePackIds.firstOrNull()?.value,
                activePackIds = resolvedActivePackIds,
                activePackManifestVersions = resolvedManifestVersions,
                seedList = scenario.seedList,
                resolvedOrder = resolvedOrder,
                overlayOps = overlayOps,
                expectedFailureCodes = scenario.expectedFailureCodes.sorted(),
                diagnosticCodes = actualCodes.toList(),
                diagnostics = failure.diagnostics,
                localeKeysResolved = emptyList(),
                visualKeysResolved = emptyList(),
                audioKeysResolved = emptyList(),
                localeResolutionFailureCount = 0,
                visualResolutionFailureCount = 0,
                audioResolutionFailureCount = 0,
                headlessRunSucceeded = false,
                fallbackToBaseVerified = false,
                registryEntryPresent = false,
                registryEntryNameKey = null,
                registryEntryVisualKey = null,
                registryEntryAudioProfile = null,
                precedenceVerified = precedenceMatches(scenario = scenario, resolvedOrder = resolvedOrder, overlayOps = overlayOps),
                success = failureReasons.isEmpty(),
                failureReasons = failureReasons,
            )
        }
    }

    internal fun classifyFailure(exception: Throwable): StructuredHarnessFailure {
        val causes = exception.causeSequence().toList()
        causes.filterIsInstance<ContentPackLoadException>().firstOrNull()?.let { loadFailure ->
            return StructuredHarnessFailure(
                diagnosticCodes = loadFailure.diagnostics.map { diagnostic -> diagnostic.code },
                diagnostics = loadFailure.diagnostics.map { diagnostic -> diagnostic.message },
            )
        }
        causes.filterIsInstance<ManifestLoadException>().firstOrNull()?.let { manifestFailure ->
            return StructuredHarnessFailure(
                diagnosticCodes = listOf(manifestFailureCode(manifestFailure)),
                diagnostics = listOf(requireNotNull(manifestFailure.message)),
            )
        }
        causes.firstOrNull(::isLocaleResourceFailure)?.let { localeFailure ->
            return StructuredHarnessFailure(
                diagnosticCodes = listOf("content-pack.resource.locale-invalid"),
                diagnostics = listOf("${localeFailure::class.simpleName}: ${localeFailure.message ?: "Invalid locale bundle."}"),
            )
        }
        return unexpectedFailure(exception)
    }

    private fun manifestFailureCode(exception: ManifestLoadException): String =
        when {
            exception.message?.contains("Visual manifest is invalid") == true -> "content-pack.resource.visual-manifest-invalid"
            exception.message?.contains("Audio manifest is invalid") == true -> "content-pack.resource.audio-manifest-invalid"
            exception.message?.contains("Client asset bundle is invalid") == true -> "content-pack.resource.manifest-merge-invalid"
            else -> "content-pack.resource.manifest-invalid"
        }

    private fun isLocaleResourceFailure(exception: Throwable): Boolean =
        (exception is SerializationException ||
            exception is IllegalStateException ||
            exception is IllegalArgumentException) &&
            exception.stackTrace.any { element ->
                element.className.contains("ContentPackResources") ||
                    element.className.contains("Localization")
            }

    private fun unexpectedFailure(exception: Throwable): StructuredHarnessFailure =
        StructuredHarnessFailure(
            diagnosticCodes = listOf("content-pack.harness.unexpected-exception"),
            diagnostics = listOf("${exception::class.simpleName}: ${exception.message ?: "Unexpected exception."}"),
        )

    private fun precedenceMatches(
        scenario: HarnessScenario,
        resolvedOrder: List<String>,
        overlayOps: List<String>,
    ): Boolean =
        (scenario.expectedResolvedOrder.isEmpty() || resolvedOrder == scenario.expectedResolvedOrder.map(PackId::value)) &&
            (scenario.expectedOverlayOps.isEmpty() || overlayOps.filter { op -> op != OverlayOp.ADD.name } == scenario.expectedOverlayOps.map(OverlayOp::name))

    private fun Throwable.causeSequence(): Sequence<Throwable> = generateSequence(this) { current -> current.cause }

    private fun verifyLocaleKeys(
        localizer: com.ktome.game.i18n.Localizer,
        keys: List<String>,
    ): ResolutionCheck {
        val resolvedKeys = linkedSetOf<String>()
        var failureCount = 0
        keys.forEach { key ->
            val resolved = localizer.text(key)
            if (resolved.startsWith("!!") && resolved.endsWith("!!")) {
                failureCount += 1
            } else {
                resolvedKeys += key
            }
        }
        return ResolutionCheck(resolvedKeys = resolvedKeys.toList(), failureCount = failureCount)
    }

    private fun verifyVisualKeys(
        bundle: com.ktome.client.assets.ClientAssetBundle,
        keys: List<String>,
    ): ResolutionCheck {
        val resolvedKeys = linkedSetOf<String>()
        var failureCount = 0
        keys.forEach { key ->
            val resolved = bundle.visualResolver.resolve(key)
            if (resolved.fallbackUsed || resolved.matchedByPrefix) {
                failureCount += 1
            } else {
                resolvedKeys += key
            }
        }
        return ResolutionCheck(resolvedKeys = resolvedKeys.toList(), failureCount = failureCount)
    }

    private fun verifyAudioKeys(
        bundle: com.ktome.client.assets.ClientAssetBundle,
        keys: List<String>,
    ): ResolutionCheck {
        val resolvedKeys = linkedSetOf<String>()
        var failureCount = 0
        keys.forEach { key ->
            val resolved = bundle.audioResolver.resolve(key)
            if (resolved.fallbackUsed || resolved.matchedByPrefix) {
                failureCount += 1
            } else {
                resolvedKeys += key
            }
        }
        return ResolutionCheck(resolvedKeys = resolvedKeys.toList(), failureCount = failureCount)
    }

    private fun runHeadlessCase(
        selection: ContentPackSelection,
        seed: Long,
    ): Boolean {
        val saveDir = Files.createTempDirectory("ktome-content-pack-harness")
        val saveManager = com.ktome.core.save.SaveManager(saveDir)
        return try {
            val session =
                GameModule.newFoundationSession(
                    config = FoundationGameConfig(seed = seed),
                    saveManager = saveManager,
                    locale = baseLocale,
                    contentPackSelection = selection,
                )
            val saved = session.perform(PlayerCommand.SaveGame)
            val loaded = GameModule.loadFoundationSession(saveManager, locale = baseLocale, contentPackSelection = selection)
            saved && loaded != null
        } finally {
            saveDir.toFile().deleteRecursively()
        }
    }

    private fun verifyBaseFallback(): Boolean {
        val bundle = ClientAssetBundleLoader.load()
        return try {
            val visualFallback = bundle.visualResolver.resolve("fixture_add_monster.actor.flooded_rat").fallbackUsed
            val audioFallback = bundle.audioResolver.resolve("fixture_add_monster.audio.flooded_rat").fallbackUsed
            visualFallback && audioFallback
        } finally {
            bundle.dispose()
        }
    }

    private fun analyze(results: List<ContentPackCaseResult>): ContentPackAnalysis {
        val aggregateFailures =
            buildList {
                if (results.any { result -> "case.diagnostic_code_mismatch" in result.failureReasons }) add("aggregate.diagnostic_code_mismatch")
                if (results.any { result -> result.localeResolutionFailureCount > 0 }) add("aggregate.locale_key_unresolved")
                if (results.any { result -> result.visualResolutionFailureCount > 0 }) add("aggregate.visual_key_unresolved")
                if (results.any { result -> result.audioResolutionFailureCount > 0 }) add("aggregate.audio_key_unresolved")
                if (results.any { result -> result.seedList.isNotEmpty() && !result.headlessRunSucceeded }) add("aggregate.headless_run_failure")
                if (results.any { result -> "case.base_fallback_not_verified" in result.failureReasons }) add("aggregate.base_fallback_failure")
                if (results.any { result -> !result.precedenceVerified }) add("aggregate.precedence_failure")
            }
        val summary =
            ContentPackSummaryMetrics(
                totalCases = results.size,
                caseFailureCount = results.count { result -> !result.success },
                aggregateFailureCount = aggregateFailures.size,
                successfulRuntimeCaseCount = results.count { result -> result.success && result.expectedFailureCodes.isEmpty() },
                expectedFailureCaseCount = results.count { result -> result.success && result.expectedFailureCodes.isNotEmpty() },
                diagnosticMismatchCount = results.count { result -> "case.diagnostic_code_mismatch" in result.failureReasons },
                localeResolutionFailureCount = results.sumOf(ContentPackCaseResult::localeResolutionFailureCount),
                visualResolutionFailureCount = results.sumOf(ContentPackCaseResult::visualResolutionFailureCount),
                audioResolutionFailureCount = results.sumOf(ContentPackCaseResult::audioResolutionFailureCount),
                headlessRunFailureCount =
                    results.count { result ->
                        result.seedList.isNotEmpty() && !result.headlessRunSucceeded
                    },
                fallbackFailureCount = results.count { result -> "case.base_fallback_not_verified" in result.failureReasons },
                precedenceFailureCount = results.count { result -> !result.precedenceVerified },
            )
        return ContentPackAnalysis(
            summary = summary,
            aggregateFailures = aggregateFailures,
        )
    }

    private fun buildSummaryPayload(
        header: HarnessReportHeader,
        results: List<ContentPackCaseResult>,
        analysis: ContentPackAnalysis,
    ): JsonObject =
        buildJsonObject {
            put("header", header.toJson())
            put(
                "summary",
                buildJsonObject {
                    put("verdict", if (analysis.summary.failureCount == 0) "PASS" else "FAIL")
                    put("totalCases", analysis.summary.totalCases)
                    put("failureCount", analysis.summary.failureCount)
                    put("caseFailureCount", analysis.summary.caseFailureCount)
                    put("aggregateFailureCount", analysis.summary.aggregateFailureCount)
                    put("successfulRuntimeCaseCount", analysis.summary.successfulRuntimeCaseCount)
                    put("expectedFailureCaseCount", analysis.summary.expectedFailureCaseCount)
                    put("diagnosticMismatchCount", analysis.summary.diagnosticMismatchCount)
                    put("localeResolutionFailureCount", analysis.summary.localeResolutionFailureCount)
                    put("visualResolutionFailureCount", analysis.summary.visualResolutionFailureCount)
                    put("audioResolutionFailureCount", analysis.summary.audioResolutionFailureCount)
                    put("headlessRunFailureCount", analysis.summary.headlessRunFailureCount)
                    put("fallbackFailureCount", analysis.summary.fallbackFailureCount)
                    put("precedenceFailureCount", analysis.summary.precedenceFailureCount)
                },
            )
            putJsonArray("aggregateFailures") {
                analysis.aggregateFailures.forEach { failure -> add(JsonPrimitive(failure)) }
            }
            putJsonArray("cases") {
                results.forEach { result -> add(result.toJson(header)) }
            }
        }

    private fun reportDir(): Path =
        Path.of(
            System.getProperty("ktome.phase4.contentPack.reportDir")
                ?: repoRoot().resolve("tools/build/reports/phase4/content-pack").toString(),
        )

    private data class HarnessScenario(
        val fixtureId: String,
        val activePackIds: List<PackId>,
        val seedList: List<Long> = emptyList(),
        val expectedFailureCodes: Set<String> = emptySet(),
        val expectedResolvedOrder: List<PackId> = emptyList(),
        val expectedOverlayOps: List<OverlayOp> = emptyList(),
        val targetRef: ContentRef? = null,
        val expectRegistryEntryPresent: Boolean = true,
        val expectedMonsterNameKey: String? = null,
        val expectedMonsterVisualKey: String? = null,
        val expectedMonsterAudioProfile: String? = null,
        val expectFallbackToBase: Boolean = false,
    )

    private data class ResolutionCheck(
        val resolvedKeys: List<String>,
        val failureCount: Int,
    ) {
        companion object {
            val EMPTY: ResolutionCheck = ResolutionCheck(resolvedKeys = emptyList(), failureCount = 0)
        }
    }

    internal data class StructuredHarnessFailure(
        val diagnosticCodes: List<String>,
        val diagnostics: List<String>,
    )
}
