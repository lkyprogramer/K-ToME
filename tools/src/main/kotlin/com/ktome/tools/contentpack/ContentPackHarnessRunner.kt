package com.ktome.tools.contentpack

import com.ktome.client.assets.ClientAssetBundle
import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.client.assets.ManifestLoadException
import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.World
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.loot.RarityTier
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier
import com.ktome.core.phase.PackId
import com.ktome.core.save.SaveManager
import com.ktome.core.map.Point
import com.ktome.core.world.solvability.SearchActionResult
import com.ktome.core.world.solvability.SearchBindingId
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.contentpack.ContentPackFixtureCatalog
import com.ktome.game.contentpack.ContentPackLoadException
import com.ktome.game.contentpack.ContentPackRuntimeResolver
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.contentpack.OverlayOp
import com.ktome.game.contentpack.ResolvedContentPackSelection
import com.ktome.game.contentpack.repoRoot
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.SpecialItemTemplateSchemaV2
import com.ktome.game.hidden.HiddenEventDef
import com.ktome.game.hidden.HiddenEventRewardPayload
import com.ktome.game.hidden.SecretRewardAuthority
import com.ktome.game.hidden.SecretZoneDef
import com.ktome.game.i18n.GameLocale
import com.ktome.tools.mapgen.phase4HarnessHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.yaml.snakeyaml.Yaml

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
    val generatedTemplateSeedList: List<Long>,
    val resolvedOrder: List<String>,
    val overlayOps: List<String>,
    val expectedFailureCodes: List<String>,
    val diagnosticCodes: List<String>,
    val diagnostics: List<String>,
    val diagnosticDetails: List<Map<String, String>>,
    val localeKeysResolved: List<String>,
    val visualKeysResolved: List<String>,
    val audioKeysResolved: List<String>,
    val localeResolutionFailureCount: Int,
    val visualResolutionFailureCount: Int,
    val audioResolutionFailureCount: Int,
    val headlessRunSucceeded: Boolean,
    val fallbackToBaseVerified: Boolean,
    val secretZonePresent: Boolean,
    val secretZoneNameKey: String?,
    val secretZoneVisualKey: String?,
    val secretZoneAudioProfile: String?,
    val hiddenEventPresent: Boolean,
    val secretRewardProfileId: String?,
    val lootProfilePresent: Boolean,
    val lootProfileRewardBudget: Int?,
    val lootProfileItemIds: List<String>,
    val lootProfilePoolStrategy: String?,
    val lootProfileItemTagFilter: List<String>,
    val lootProfileExcludeIds: List<String>,
    val lootProfileTypeWeights: Map<String, Int>,
    val lootProfileSlotBias: Map<String, Int>,
    val lootProfileSpecialTemplateTagPreference: List<String>,
    val lootProfileAffixTagPreference: List<String>,
    val specialTemplateIds: List<String>,
    val generatedSpecialTemplateIds: List<String>,
    val samplePackTouchedContentIds: List<String>,
    val samplePackFixedSeedTouchedContentIds: List<String>,
    val samplePackFixedSeedRunCount: Int,
    val samplePackFixedSeedTouchedRunCount: Int,
    val samplePackAddOnlyMainPath: Boolean,
    val samplePackSecondarySecretSlotUsed: Boolean,
    val samplePackFixedSeedVisibilityCase: Boolean,
    val resourceContractVerified: Boolean,
    val resourceContractDetails: List<String>,
    val precedenceVerified: Boolean,
    val success: Boolean,
    val failureReasons: List<String>,
) {
    fun toFactsJson(): JsonObject =
        buildJsonObject {
            put("fixtureId", fixtureId)
            put("packId", packId)
            putJsonArray("activePackIds") { activePackIds.forEach { packId -> add(JsonPrimitive(packId)) } }
            putJsonObject("activePackManifestVersions") {
                activePackManifestVersions.forEach { (packId, version) -> put(packId, version) }
            }
            putJsonArray("seedList") { seedList.forEach { seed -> add(JsonPrimitive(seed)) } }
            putJsonArray("generatedTemplateSeedList") { generatedTemplateSeedList.forEach { seed -> add(JsonPrimitive(seed)) } }
            putJsonArray("resolvedOrder") { resolvedOrder.forEach { packId -> add(JsonPrimitive(packId)) } }
            putJsonArray("overlayOps") { overlayOps.forEach { op -> add(JsonPrimitive(op)) } }
            putJsonArray("expectedFailureCodes") { expectedFailureCodes.forEach { code -> add(JsonPrimitive(code)) } }
            putJsonArray("diagnosticCodes") { diagnosticCodes.forEach { code -> add(JsonPrimitive(code)) } }
            putJsonArray("diagnostics") { diagnostics.forEach { diagnostic -> add(JsonPrimitive(diagnostic)) } }
            putJsonArray("diagnosticDetails") {
                diagnosticDetails.forEach { details -> add(details.toJson()) }
            }
            putJsonArray("localeKeysResolved") { localeKeysResolved.forEach { key -> add(JsonPrimitive(key)) } }
            putJsonArray("visualKeysResolved") { visualKeysResolved.forEach { key -> add(JsonPrimitive(key)) } }
            putJsonArray("audioKeysResolved") { audioKeysResolved.forEach { key -> add(JsonPrimitive(key)) } }
            put("localeResolutionFailureCount", localeResolutionFailureCount)
            put("visualResolutionFailureCount", visualResolutionFailureCount)
            put("audioResolutionFailureCount", audioResolutionFailureCount)
            put("headlessRunSucceeded", headlessRunSucceeded)
            put("fallbackToBaseVerified", fallbackToBaseVerified)
            put("secretZonePresent", secretZonePresent)
            put("secretZoneNameKey", secretZoneNameKey)
            put("secretZoneVisualKey", secretZoneVisualKey)
            put("secretZoneAudioProfile", secretZoneAudioProfile)
            put("hiddenEventPresent", hiddenEventPresent)
            put("secretRewardProfileId", secretRewardProfileId)
            put("lootProfilePresent", lootProfilePresent)
            put("lootProfileRewardBudget", lootProfileRewardBudget)
            putJsonArray("lootProfileItemIds") { lootProfileItemIds.forEach { itemId -> add(JsonPrimitive(itemId)) } }
            put("lootProfilePoolStrategy", lootProfilePoolStrategy)
            putJsonArray("lootProfileItemTagFilter") {
                lootProfileItemTagFilter.forEach { tag -> add(JsonPrimitive(tag)) }
            }
            putJsonArray("lootProfileExcludeIds") {
                lootProfileExcludeIds.forEach { itemId -> add(JsonPrimitive(itemId)) }
            }
            putJsonObject("lootProfileTypeWeights") {
                lootProfileTypeWeights.forEach { (type, weight) -> put(type, weight) }
            }
            putJsonObject("lootProfileSlotBias") {
                lootProfileSlotBias.forEach { (slot, weight) -> put(slot, weight) }
            }
            putJsonArray("lootProfileSpecialTemplateTagPreference") {
                lootProfileSpecialTemplateTagPreference.forEach { tag -> add(JsonPrimitive(tag)) }
            }
            putJsonArray("lootProfileAffixTagPreference") {
                lootProfileAffixTagPreference.forEach { tag -> add(JsonPrimitive(tag)) }
            }
            putJsonArray("specialTemplateIds") { specialTemplateIds.forEach { templateId -> add(JsonPrimitive(templateId)) } }
            putJsonArray("generatedSpecialTemplateIds") {
                generatedSpecialTemplateIds.forEach { templateId -> add(JsonPrimitive(templateId)) }
            }
            putJsonArray("samplePackTouchedContentIds") {
                samplePackTouchedContentIds.forEach { contentId -> add(JsonPrimitive(contentId)) }
            }
            putJsonArray("samplePackFixedSeedTouchedContentIds") {
                samplePackFixedSeedTouchedContentIds.forEach { contentId -> add(JsonPrimitive(contentId)) }
            }
            put("samplePackFixedSeedRunCount", samplePackFixedSeedRunCount)
            put("samplePackFixedSeedTouchedRunCount", samplePackFixedSeedTouchedRunCount)
            put("samplePackAddOnlyMainPath", samplePackAddOnlyMainPath)
            put("samplePackSecondarySecretSlotUsed", samplePackSecondarySecretSlotUsed)
            put("samplePackFixedSeedVisibilityCase", samplePackFixedSeedVisibilityCase)
            put("resourceContractVerified", resourceContractVerified)
            putJsonArray("resourceContractDetails") {
                resourceContractDetails.forEach { detail -> add(JsonPrimitive(detail)) }
            }
            put("precedenceVerified", precedenceVerified)
            put("success", success)
            putJsonArray("failureReasons") { failureReasons.forEach { reason -> add(JsonPrimitive(reason)) } }
        }

    fun toJson(header: HarnessReportHeader): JsonObject =
        buildJsonObject {
            put("buildId", header.buildId)
            put("phaseId", header.phaseId)
            put("locale", header.locale)
            put("overlayContractVersion", header.overlayContractVersion)
            toFactsJson().forEach { (key, value) -> put(key, value) }
        }
}

internal data class LegacyLootProfileSchemaRejectSummary(
    val fixtureId: String,
    val packId: String,
    val diagnosticCode: String,
    val targetProfileId: String,
    val actualSchemaVersion: String,
    val expectedSchemaVersion: String,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("fixtureId", fixtureId)
            put("packId", packId)
            put("diagnosticCode", diagnosticCode)
            put("targetProfileId", targetProfileId)
            put("actualSchemaVersion", actualSchemaVersion)
            put("expectedSchemaVersion", expectedSchemaVersion)
        }
}

private fun Map<String, String>.toJson(): JsonObject =
    buildJsonObject {
        this@toJson.toSortedMap().forEach { (key, value) -> put(key, value) }
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
    val resourceContractFailureCount: Int,
    val generatedTemplateFailureCount: Int,
    val samplePackContentPlayerVisibilityRate: Double,
    val samplePackTouchedContentIds: List<String>,
    val samplePackAddOnlyMainPath: Boolean,
    val samplePackSecondarySecretSlotUsed: Boolean,
    val samplePackFixedSeedVisibilityCase: Boolean,
    val activeSampleFixedSeedRunCount: Int,
    val touchedSampleRunCount: Int,
    val legacyLootProfileSchemaRejectCount: Int,
    val legacyLootProfileSchemaRejectSummaries: List<LegacyLootProfileSchemaRejectSummary>,
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

internal data class SampleRewardTrace(
    val seed: Long,
    val generatedSpecialTemplateId: String?,
    val touchedContentIds: List<String>,
)

internal data class ResourceContractCheck(
    val verified: Boolean,
    val details: List<String>,
)

object ContentPackHarnessRunner {
    private const val SUMMARY_FILE: String = "content-pack-summary.json"
    private const val RUNS_FILE: String = "content-pack-runs.jsonl"
    private const val sampleSecretZoneId: String = "sample.flooded_relics.secret_zone.flooded_reliquary"
    private const val baseSecretZoneId: String = "underground_river_crystal_rift"
    private const val sampleSecretBindingId: String = "sample.flooded_relics.search.flooded_reliquary"
    private const val crystalCacheChestId: String = "crystal_cache_chest"
    private const val sampleHiddenEventId: String = "sample.flooded_relics.hidden_event.flooded_reliquary.reward"
    private const val baseHiddenEventId: String = "hidden.event.underground_river.crystal_rift.reward"
    private const val sampleLootProfileId: String = "sample.flooded_relics.loot.flooded_reliquary.secret"
    private const val baseLootProfileId: String = "loot.underground_river_crystal_rift.secret"
    private const val fixedSeedVisibilityCaseId: String = "sample_flooded_relics_active_2026042437"
    private const val fixedSeedVisibilitySeed: Long = 2026042437L
    private const val imagePlanPath: String = "assets-src/image/specs/phase4-pr09-gemini-plan.yaml"
    private const val imageGenerationReportPath: String = "assets-src/image/manifests/phase4-pr09-generation-report.jsonl"
    private const val imageProcessingReportPath: String = "assets-src/image/manifests/phase4-pr09-processing-report.jsonl"
    private const val audioPlanPath: String = "assets-src/audio/specs/phase4-pr09-audio-plan.yaml"
    private const val audioProcessingReportPath: String = "assets-src/audio/manifests/phase4-pr09-processing-report.jsonl"
    private val json: Json = Json { prettyPrint = true }
    private val lineJson: Json = Json { prettyPrint = false }
    private val yaml = Yaml()
    private val baseLocale: GameLocale = GameLocale.EN_US
    private val officialSampleVisualKeys: List<String> =
        listOf(
            "sample_flooded_relics.zone.flooded_reliquary.visual",
            "sample_flooded_relics.zone.flooded_reliquary.icon",
            "sample_flooded_relics.prop.reliquary_node.visual",
            "sample_flooded_relics.prop.return_bridge.visual",
            "sample_flooded_relics.item.floodtide_lantern.visual",
            "sample_flooded_relics.item.floodtide_lantern.icon",
            "sample_flooded_relics.item.tideglass_echo.visual",
            "sample_flooded_relics.item.tideglass_echo.icon",
        )
    private val officialSampleAudioKeys: List<String> =
        listOf(
            "sample_flooded_relics.audio.zone.flooded_reliquary",
            "sample_flooded_relics.audio.interactable.reliquary_node",
            "sample_flooded_relics.audio.interactable.return_bridge",
            "sample_flooded_relics.audio.item.floodtide_lantern",
            "sample_flooded_relics.audio.item.tideglass_echo",
        )
    private val officialSampleLocaleKeys: List<String> =
        listOf(
            "sample_flooded_relics.zone.flooded_reliquary.name",
            "sample_flooded_relics.zone.flooded_reliquary.desc",
            "sample_flooded_relics.prop.reliquary_node.name",
            "sample_flooded_relics.prop.reliquary_node.desc",
            "sample_flooded_relics.prop.return_bridge.name",
            "sample_flooded_relics.prop.return_bridge.desc",
            "sample_flooded_relics.item.floodtide_lantern.name",
            "sample_flooded_relics.item.floodtide_lantern.desc",
            "sample_flooded_relics.item.tideglass_echo.name",
            "sample_flooded_relics.item.tideglass_echo.desc",
        )

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
        val harnessSpec = ContentPackFixtureCatalog.harnessSpec(ContentPackFixtureCatalog.samplePackId)
        val precedenceScenario = harnessSpec.dualPackScenarios.single()
        val scenarioCatalog =
            listOf(
                HarnessScenario(
                    fixtureId = "official_sample_pack",
                    reportPackId = ContentPackFixtureCatalog.samplePackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.selection(
                            activePackRoots = listOf(ContentPackFixtureCatalog.samplePackRoot()),
                        )
                    },
                    seedList = harnessSpec.harnessSeeds,
                    expectedResolvedOrder = listOf(ContentPackFixtureCatalog.samplePackId),
                    expectedSecretZoneNameKey = "sample_flooded_relics.zone.flooded_reliquary.name",
                    expectedSecretZoneVisualKey = "sample_flooded_relics.zone.flooded_reliquary.visual",
                    expectedSecretZoneAudioProfile = "sample_flooded_relics.audio.zone.flooded_reliquary",
                    expectedSecretRewardProfileId = sampleLootProfileId,
                    expectedLootProfileId = sampleLootProfileId,
                    expectedLootProfileSpecialTemplateTagPreference = listOf("underground_river"),
                    expectedLootProfileAffixTagPreference = listOf("underground_river"),
                    expectedSpecialTemplateIds =
                        setOf(
                            "sample.flooded_relics.unique.floodtide_lantern",
                            "sample.flooded_relics.artifact.tideglass_echo",
                        ),
                    generatedTemplateSeedList = harnessSpec.generatedTemplateSeeds,
                    localeKeys = officialSampleLocaleKeys,
                    visualKeys = officialSampleVisualKeys,
                    audioKeys = officialSampleAudioKeys,
                    verifyResourceContracts = true,
                    verifyGeneratedTemplates = true,
                ),
                HarnessScenario(
                    fixtureId = "disabled_pack_fallback",
                    reportPackId = ContentPackFixtureCatalog.samplePackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.selection(
                            activePackRoots = emptyList(),
                            availablePackRoots = listOf(ContentPackFixtureCatalog.samplePackRoot()),
                        )
                    },
                    expectedSecretZoneId = baseSecretZoneId,
                    expectedHiddenEventId = baseHiddenEventId,
                    expectedSecretZoneNameKey = "zone.secret.underground_river_crystal_rift.name",
                    expectedSecretZoneVisualKey = "zone.secret.underground_river_crystal_rift.visual",
                    expectedSecretZoneAudioProfile = "audio.secret_zone.underground_river_crystal_rift",
                    expectedSecretRewardProfileId = baseLootProfileId,
                    expectedLootProfileId = baseLootProfileId,
                    localeKeys =
                        listOf(
                            "zone.secret.underground_river_crystal_rift.name",
                            "zone.secret.underground_river_crystal_rift.desc",
                        ),
                    visualKeys =
                        listOf(
                            "zone.secret.underground_river_crystal_rift.visual",
                            "zone.secret.underground_river_crystal_rift.icon",
                        ),
                    audioKeys = listOf("audio.secret_zone.underground_river_crystal_rift"),
                    expectFallbackToBase = true,
                ),
                HarnessScenario(
                    fixtureId = "precedence_fixture",
                    reportPackId = precedenceScenario.fixturePackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.selection(
                            activePackRoots = listOf(ContentPackFixtureCatalog.fixturePackRoot(precedenceScenario.fixturePackId)),
                            availablePackRoots =
                                listOf(
                                    ContentPackFixtureCatalog.samplePackRoot(),
                                    ContentPackFixtureCatalog.fixturePackRoot(precedenceScenario.fixturePackId),
                                ),
                        )
                    },
                    seedList = harnessSpec.harnessSeeds,
                    expectedResolvedOrder = precedenceScenario.expectedOrder,
                    expectedOverlayOps = precedenceScenario.expectedOps,
                    expectedSecretZoneNameKey = "fixture_sample_flooded_relics_override.zone.flooded_reliquary.name",
                    expectedSecretZoneVisualKey = "fixture_sample_flooded_relics_override.zone.flooded_reliquary.visual",
                    expectedSecretZoneAudioProfile = "fixture_sample_flooded_relics_override.audio.zone.flooded_reliquary",
                    expectedSecretRewardProfileId = sampleLootProfileId,
                    expectedLootProfileId = sampleLootProfileId,
                    expectedLootProfileSpecialTemplateTagPreference = listOf("underground_river"),
                    expectedLootProfileAffixTagPreference = listOf("underground_river"),
                    expectedSpecialTemplateIds =
                        setOf(
                            "sample.flooded_relics.unique.floodtide_lantern",
                            "sample.flooded_relics.artifact.tideglass_echo",
                        ),
                    localeKeys =
                        listOf(
                            "fixture_sample_flooded_relics_override.zone.flooded_reliquary.name",
                            "fixture_sample_flooded_relics_override.zone.flooded_reliquary.desc",
                        ),
                    visualKeys =
                        listOf(
                            "fixture_sample_flooded_relics_override.zone.flooded_reliquary.visual",
                            "fixture_sample_flooded_relics_override.zone.flooded_reliquary.icon",
                        ),
                    audioKeys = listOf("fixture_sample_flooded_relics_override.audio.zone.flooded_reliquary"),
                ),
                HarnessScenario(
                    fixtureId = "split_bias_fixture",
                    reportPackId = ContentPackFixtureCatalog.sampleBiasSplitFixturePackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.selection(
                            activePackRoots = listOf(ContentPackFixtureCatalog.fixturePackRoot(ContentPackFixtureCatalog.sampleBiasSplitFixturePackId)),
                            availablePackRoots =
                                listOf(
                                    ContentPackFixtureCatalog.samplePackRoot(),
                                    ContentPackFixtureCatalog.fixturePackRoot(ContentPackFixtureCatalog.sampleBiasSplitFixturePackId),
                                ),
                        )
                    },
                    seedList = harnessSpec.harnessSeeds,
                    expectedResolvedOrder = listOf(ContentPackFixtureCatalog.samplePackId, ContentPackFixtureCatalog.sampleBiasSplitFixturePackId),
                    expectedSecretRewardProfileId = sampleLootProfileId,
                    expectedLootProfileId = sampleLootProfileId,
                    expectedLootProfileSpecialTemplateTagPreference = listOf("underground_river"),
                    expectedLootProfileAffixTagPreference = listOf("water"),
                    expectedSpecialTemplateIds =
                        setOf(
                            "sample.flooded_relics.unique.floodtide_lantern",
                            "sample.flooded_relics.artifact.tideglass_echo",
                        ),
                    generatedTemplateSeedList = harnessSpec.generatedTemplateSeeds,
                    localeKeys = officialSampleLocaleKeys,
                    visualKeys = officialSampleVisualKeys,
                    audioKeys = officialSampleAudioKeys,
                    verifyGeneratedTemplates = true,
                ),
                HarnessScenario(
                    fixtureId = "append_runtime_rejected",
                    reportPackId = ContentPackFixtureCatalog.appendPackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.appendPackId))
                    },
                    expectedFailureCodes = setOf("content-pack.overlay.runtime-op-forbidden"),
                ),
                HarnessScenario(
                    fixtureId = "deny_runtime_rejected",
                    reportPackId = ContentPackFixtureCatalog.denyPackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.denyPackId))
                    },
                    expectedFailureCodes = setOf("content-pack.overlay.runtime-op-forbidden"),
                ),
                HarnessScenario(
                    fixtureId = "missing_dependency",
                    reportPackId = ContentPackFixtureCatalog.missingDependencyPackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.missingDependencyPackId))
                    },
                    expectedFailureCodes = setOf("content-pack.dependency.missing"),
                ),
                HarnessScenario(
                    fixtureId = "dependency_cycle",
                    reportPackId = ContentPackFixtureCatalog.cyclePackAId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.cyclePackAId))
                    },
                    expectedFailureCodes = setOf("content-pack.dependency.cycle"),
                ),
                HarnessScenario(
                    fixtureId = "version_range_conflict",
                    reportPackId = ContentPackFixtureCatalog.versionConflictPackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.versionConflictPackId))
                    },
                    expectedFailureCodes = setOf("content-pack.version-range.conflict"),
                ),
                HarnessScenario(
                    fixtureId = "namespace_collision",
                    reportPackId = ContentPackFixtureCatalog.namespaceCollisionLeftId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.availableSelection(
                            listOf(
                                ContentPackFixtureCatalog.namespaceCollisionLeftId,
                                ContentPackFixtureCatalog.namespaceCollisionRightId,
                            ),
                        )
                    },
                    expectedFailureCodes = setOf("content-pack.namespace.collision"),
                ),
                HarnessScenario(
                    fixtureId = "same_priority_duplicate_target",
                    reportPackId = ContentPackFixtureCatalog.samePriorityLeftId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.availableSelection(
                            listOf(
                                ContentPackFixtureCatalog.samePriorityLeftId,
                                ContentPackFixtureCatalog.samePriorityRightId,
                            ),
                        )
                    },
                    expectedFailureCodes = setOf("content-pack.overlay.same-priority-duplicate-target"),
                ),
                HarnessScenario(
                    fixtureId = "duplicate_without_replace",
                    reportPackId = ContentPackFixtureCatalog.duplicateWithoutReplacePackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.duplicateWithoutReplacePackId))
                    },
                    expectedFailureCodes = setOf("content-pack.overlay.add-conflict"),
                ),
                HarnessScenario(
                    fixtureId = "legacy_v2_loot_profile_rejected",
                    reportPackId = ContentPackFixtureCatalog.legacyV2LootProfilePackId,
                    selectionProvider = {
                        ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.legacyV2LootProfilePackId))
                    },
                    expectedFailureCodes = setOf("content-pack.loot-profile.schema-version-mismatch"),
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
                seedList =
                    results
                        .flatMap { result -> result.seedList + result.generatedTemplateSeedList }
                        .distinct(),
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
        val selection = scenario.selectionProvider()
        var resolvedSelection: ResolvedContentPackSelection? = null
        var resolvedOrder: List<String> = emptyList()
        var overlayOps: List<String> = emptyList()
        return try {
            resolvedSelection =
                if (selection.isEmpty) {
                    ResolvedContentPackSelection.EMPTY
                } else {
                    ContentPackRuntimeResolver.resolve(selection)
                }
            val currentResolvedSelection = requireNotNull(resolvedSelection)
            resolvedOrder = currentResolvedSelection.orderedPacks.map { pack -> pack.id.value }
            overlayOps = currentResolvedSelection.orderedPacks.flatMap { pack -> pack.manifest.overlays.map { overlay -> overlay.op.name } }

            val loader =
                DataLoader(
                    locale = baseLocale,
                    packSelection = selection,
                    preResolvedContentPackSelection = currentResolvedSelection,
                )
            val catalog = loader.loadSchemaCatalog()
            val assetBundle = loadAssetBundle(selection = selection, resolvedSelection = currentResolvedSelection)
            try {
                val secretZone = catalog.secretZones.firstOrNull { zone -> zone.id.id == scenario.expectedSecretZoneId }
                val hiddenEvent = catalog.hiddenEvents.firstOrNull { event -> event.id == scenario.expectedHiddenEventId }
                val lootProfile = scenario.expectedLootProfileId?.let { expectedId -> catalog.lootProfiles.firstOrNull { profile -> profile.id == expectedId } }
                val packSpecialTemplates =
                    (catalog.itemBundle.uniqueTemplates + catalog.itemBundle.artifactTemplates)
                        .filter { template -> template.id in scenario.expectedSpecialTemplateIds }
                val localeChecks = verifyLocaleKeys(localizer = loader.localizer, keys = scenario.localeKeys)
                val visualChecks = verifyVisualKeys(bundle = assetBundle, keys = scenario.visualKeys)
                val audioChecks = verifyAudioKeys(bundle = assetBundle, keys = scenario.audioKeys)
                val sampleRewardTraces =
                    if (scenario.verifyGeneratedTemplates) {
                        generateSampleRewardTraces(
                            selection = selection,
                            seedList = scenario.generatedTemplateSeedList,
                        )
                    } else {
                        emptyList()
                    }
                val generatedSpecialTemplateIds = sampleRewardTraces.mapNotNull(SampleRewardTrace::generatedSpecialTemplateId)
                val samplePackTouchedContentIds =
                    sampleRewardTraces
                        .flatMap(SampleRewardTrace::touchedContentIds)
                        .distinct()
                        .sorted()
                val samplePackFixedSeedTraces =
                    sampleRewardTraces.filter { trace -> trace.seed == fixedSeedVisibilitySeed }
                val samplePackFixedSeedTouchedContentIds =
                    samplePackFixedSeedTraces
                        .flatMap(SampleRewardTrace::touchedContentIds)
                        .distinct()
                        .sorted()
                val samplePackFixedSeedTouchedRunCount =
                    samplePackFixedSeedTraces.count { trace -> hasSampleFixedSeedRuntimeTouch(trace.touchedContentIds) }
                val sampleManifest = currentResolvedSelection.orderedPacks.firstOrNull { pack -> pack.id == ContentPackFixtureCatalog.samplePackId }?.manifest
                val samplePackAddOnlyMainPath = sampleManifest?.overlays?.all { overlay -> overlay.op == OverlayOp.ADD } == true
                val samplePackFixedSeedVisibilityCase =
                    sampleManifest
                        ?.extensions
                        ?.hiddenBranchBindings
                        ?.any { binding -> binding.fixedSeedVisibilityCase == fixedSeedVisibilityCaseId } == true
                val sampleSecretPlanUsed =
                    catalog.zoneMapgenProfiles
                        .firstOrNull { profile -> profile.zoneId == "underground_river" }
                        ?.hiddenEntrancePlans
                        ?.any { plan ->
                            plan.bindingId.value == sampleSecretBindingId &&
                                plan.sourceAnchorId.value == "hidden.critical.adjacent" &&
                                plan.targetSecretZoneId.id == sampleSecretZoneId
                        } == true
                val samplePackSecondarySecretSlotUsed = secretZone?.secretZoneSelector?.secondarySlot == true && sampleSecretPlanUsed
                val headlessRunSucceeded =
                    if (scenario.seedList.isNotEmpty()) {
                        scenario.seedList.all { seed -> runHeadlessCase(selection = selection, seed = seed) }
                    } else {
                        false
                    }
                val fallbackToBaseVerified =
                    scenario.expectFallbackToBase &&
                        secretZone?.nameKey == "zone.secret.underground_river_crystal_rift.name" &&
                        secretRewardProfileId(secretZone, hiddenEvent) == baseLootProfileId
                val resourceContracts =
                    if (scenario.verifyResourceContracts) {
                        verifySamplePackResourceContracts()
                    } else {
                        ResourceContractCheck(verified = true, details = emptyList())
                    }
                val precedenceVerified = precedenceMatches(scenario = scenario, resolvedOrder = resolvedOrder, overlayOps = overlayOps)
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
                        if (scenario.seedList.isNotEmpty() && !headlessRunSucceeded) {
                            add("case.headless_run_failed")
                        }
                        if (scenario.expectFallbackToBase && !fallbackToBaseVerified) {
                            add("case.base_fallback_not_verified")
                        }
                        if (!resourceContracts.verified) {
                            add("case.resource_contract_failed")
                        }
                        if (secretZone == null) {
                            add("case.secret_zone_missing")
                        }
                        if (scenario.expectedSecretZoneNameKey != null && secretZone?.nameKey != scenario.expectedSecretZoneNameKey) {
                            add("case.secret_zone_name_key_mismatch")
                        }
                        if (scenario.expectedSecretZoneVisualKey != null && secretZone?.visualKey != scenario.expectedSecretZoneVisualKey) {
                            add("case.secret_zone_visual_key_mismatch")
                        }
                        if (scenario.expectedSecretZoneAudioProfile != null && secretZone?.audioProfile != scenario.expectedSecretZoneAudioProfile) {
                            add("case.secret_zone_audio_profile_mismatch")
                        }
                        if (hiddenEvent == null && scenario.expectedSecretRewardProfileId != null) {
                            add("case.hidden_event_missing")
                        }
                        if (scenario.expectedSecretRewardProfileId != null &&
                            secretRewardProfileId(secretZone, hiddenEvent) != scenario.expectedSecretRewardProfileId
                        ) {
                            add("case.secret_reward_profile_mismatch")
                        }
                        if (scenario.expectedLootProfileId != null && lootProfile == null) {
                            add("case.loot_profile_missing")
                        }
                        if (scenario.expectedLootProfileId == sampleLootProfileId && lootProfile?.id != sampleLootProfileId) {
                            add("case.loot_profile_registry_mismatch")
                        }
                        if (scenario.expectedLootProfileSpecialTemplateTagPreference.isNotEmpty() &&
                            lootProfile?.specialTemplateTagPreference != scenario.expectedLootProfileSpecialTemplateTagPreference
                        ) {
                            add("case.loot_profile_special_template_preference_mismatch")
                        }
                        if (scenario.expectedLootProfileAffixTagPreference.isNotEmpty() &&
                            lootProfile?.affixTagPreference != scenario.expectedLootProfileAffixTagPreference
                        ) {
                            add("case.loot_profile_affix_preference_mismatch")
                        }
                        if (scenario.expectedSpecialTemplateIds.isNotEmpty() &&
                            packSpecialTemplates.mapTo(linkedSetOf(), SpecialItemTemplateSchemaV2::id) != scenario.expectedSpecialTemplateIds
                        ) {
                            add("case.special_template_registry_mismatch")
                        }
                        if (scenario.verifyGeneratedTemplates &&
                            generatedSpecialTemplateIds.none { templateId -> templateId.startsWith("sample.flooded_relics.") }
                        ) {
                            add("case.generated_special_template_missing")
                        }
                        if (scenario.verifyGeneratedTemplates &&
                            !hasSampleFixedSeedRuntimeTouch(samplePackFixedSeedTouchedContentIds)
                        ) {
                            add("case.sample_pack_runtime_touch_missing")
                        }
                        if (scenario.verifyGeneratedTemplates &&
                            !hasSampleItemTouch(samplePackFixedSeedTouchedContentIds)
                        ) {
                            add("case.sample_pack_item_touch_missing")
                        }
                        if (scenario.verifyGeneratedTemplates && !samplePackSecondarySecretSlotUsed) {
                            add("case.sample_pack_secondary_secret_slot_missing")
                        }
                        if (scenario.verifyGeneratedTemplates && !samplePackFixedSeedVisibilityCase) {
                            add("case.sample_pack_fixed_seed_visibility_missing")
                        }
                    }
                ContentPackCaseResult(
                    fixtureId = scenario.fixtureId,
                    packId = scenario.reportPackId.value,
                    activePackIds = currentResolvedSelection.activePackIds.map(PackId::value),
                    activePackManifestVersions = currentResolvedSelection.activePackManifestVersions.mapKeys { (packId, _) -> packId.value },
                    seedList = scenario.seedList,
                    generatedTemplateSeedList = scenario.generatedTemplateSeedList,
                    resolvedOrder = resolvedOrder,
                    overlayOps = overlayOps,
                    expectedFailureCodes = scenario.expectedFailureCodes.sorted(),
                    diagnosticCodes = emptyList(),
                    diagnostics = emptyList(),
                    diagnosticDetails = emptyList(),
                    localeKeysResolved = localeChecks.resolvedKeys,
                    visualKeysResolved = visualChecks.resolvedKeys,
                    audioKeysResolved = audioChecks.resolvedKeys,
                    localeResolutionFailureCount = localeChecks.failureCount,
                    visualResolutionFailureCount = visualChecks.failureCount,
                    audioResolutionFailureCount = audioChecks.failureCount,
                    headlessRunSucceeded = headlessRunSucceeded,
                    fallbackToBaseVerified = fallbackToBaseVerified,
                    secretZonePresent = secretZone != null,
                    secretZoneNameKey = secretZone?.nameKey,
                    secretZoneVisualKey = secretZone?.visualKey,
                    secretZoneAudioProfile = secretZone?.audioProfile,
                    hiddenEventPresent = hiddenEvent != null,
                    secretRewardProfileId = secretRewardProfileId(secretZone, hiddenEvent),
                    lootProfilePresent = lootProfile != null,
                    lootProfileRewardBudget = lootProfile?.rewardBudget,
                    lootProfileItemIds = lootProfile?.itemIds.orEmpty(),
                    lootProfilePoolStrategy = lootProfile?.poolStrategy?.name,
                    lootProfileItemTagFilter = lootProfile?.itemTagFilter.orEmpty(),
                    lootProfileExcludeIds = lootProfile?.excludeIds.orEmpty(),
                    lootProfileTypeWeights =
                        lootProfile
                            ?.typeWeights
                            ?.mapKeys { (type, _) -> type.name }
                            .orEmpty(),
                    lootProfileSlotBias =
                        lootProfile
                            ?.slotBias
                            ?.mapKeys { (slot, _) -> slot.name }
                            .orEmpty(),
                    lootProfileSpecialTemplateTagPreference = lootProfile?.specialTemplateTagPreference.orEmpty(),
                    lootProfileAffixTagPreference = lootProfile?.affixTagPreference.orEmpty(),
                    specialTemplateIds = packSpecialTemplates.map(SpecialItemTemplateSchemaV2::id).sorted(),
                    generatedSpecialTemplateIds = generatedSpecialTemplateIds,
                    samplePackTouchedContentIds = samplePackTouchedContentIds,
                    samplePackFixedSeedTouchedContentIds = samplePackFixedSeedTouchedContentIds,
                    samplePackFixedSeedRunCount = samplePackFixedSeedTraces.size,
                    samplePackFixedSeedTouchedRunCount = samplePackFixedSeedTouchedRunCount,
                    samplePackAddOnlyMainPath = samplePackAddOnlyMainPath,
                    samplePackSecondarySecretSlotUsed = samplePackSecondarySecretSlotUsed,
                    samplePackFixedSeedVisibilityCase = samplePackFixedSeedVisibilityCase,
                    resourceContractVerified = resourceContracts.verified,
                    resourceContractDetails = resourceContracts.details,
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
            val resolvedActivePackIds = resolvedSelection?.activePackIds?.map(PackId::value).orEmpty()
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
                packId = scenario.reportPackId.value,
                activePackIds = resolvedActivePackIds,
                activePackManifestVersions = resolvedManifestVersions,
                seedList = scenario.seedList,
                generatedTemplateSeedList = scenario.generatedTemplateSeedList,
                resolvedOrder = resolvedOrder,
                overlayOps = overlayOps,
                expectedFailureCodes = scenario.expectedFailureCodes.sorted(),
                diagnosticCodes = actualCodes.toList(),
                diagnostics = failure.diagnostics,
                diagnosticDetails = failure.diagnosticDetails,
                localeKeysResolved = emptyList(),
                visualKeysResolved = emptyList(),
                audioKeysResolved = emptyList(),
                localeResolutionFailureCount = 0,
                visualResolutionFailureCount = 0,
                audioResolutionFailureCount = 0,
                headlessRunSucceeded = false,
                fallbackToBaseVerified = false,
                secretZonePresent = false,
                secretZoneNameKey = null,
                secretZoneVisualKey = null,
                secretZoneAudioProfile = null,
                hiddenEventPresent = false,
                secretRewardProfileId = null,
                lootProfilePresent = false,
                lootProfileRewardBudget = null,
                lootProfileItemIds = emptyList(),
                lootProfilePoolStrategy = null,
                lootProfileItemTagFilter = emptyList(),
                lootProfileExcludeIds = emptyList(),
                lootProfileTypeWeights = emptyMap(),
                lootProfileSlotBias = emptyMap(),
                lootProfileSpecialTemplateTagPreference = emptyList(),
                lootProfileAffixTagPreference = emptyList(),
                specialTemplateIds = emptyList(),
                generatedSpecialTemplateIds = emptyList(),
                samplePackTouchedContentIds = emptyList(),
                samplePackFixedSeedTouchedContentIds = emptyList(),
                samplePackFixedSeedRunCount = 0,
                samplePackFixedSeedTouchedRunCount = 0,
                samplePackAddOnlyMainPath = false,
                samplePackSecondarySecretSlotUsed = false,
                samplePackFixedSeedVisibilityCase = false,
                resourceContractVerified = !scenario.verifyResourceContracts,
                resourceContractDetails = emptyList(),
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
                diagnosticDetails = loadFailure.diagnostics.map { diagnostic -> diagnostic.details },
            )
        }
        causes.filterIsInstance<ManifestLoadException>().firstOrNull()?.let { manifestFailure ->
            return StructuredHarnessFailure(
                diagnosticCodes = listOf(manifestFailureCode(manifestFailure)),
                diagnostics = listOf(requireNotNull(manifestFailure.message)),
                diagnosticDetails = emptyList(),
            )
        }
        causes.firstOrNull(::isLocaleResourceFailure)?.let { localeFailure ->
            return StructuredHarnessFailure(
                diagnosticCodes = listOf("content-pack.resource.locale-invalid"),
                diagnostics = listOf("${localeFailure::class.simpleName}: ${localeFailure.message ?: "Invalid locale bundle."}"),
                diagnosticDetails = emptyList(),
            )
        }
        return unexpectedFailure(exception)
    }

    private fun loadAssetBundle(
        selection: ContentPackSelection,
        resolvedSelection: ResolvedContentPackSelection,
    ): ClientAssetBundle =
        if (selection.isEmpty) {
            ClientAssetBundleLoader.load()
        } else {
            ClientAssetBundleLoader.load(resolvedSelection)
        }

    private fun generateSampleRewardTraces(
        selection: ContentPackSelection,
        seedList: List<Long>,
    ): List<SampleRewardTrace> {
        return seedList.mapNotNull { seed ->
            val saveDir = Files.createTempDirectory("ktome-content-pack-generated-template")
            try {
                val session =
                    GameModule.newFoundationSession(
                        config = FoundationGameConfig(seed = seed, zoneId = "underground_river", playerProfessionId = "arcanist"),
                        saveManager = SaveManager(saveDir),
                        locale = baseLocale,
                        contentPackSelection = selection,
                    )
                generateSampleRewardTrace(session, seed)
            } finally {
                saveDir.toFile().deleteRecursively()
            }
        }
    }

    private fun generateSampleRewardTrace(
        session: FoundationGameSession,
        seed: Long,
    ): SampleRewardTrace {
        clearMonsters(session)
        val bindingId = SearchBindingId(sampleSecretBindingId)
        val crystalChestPoint = requireNotNull(session.automationInteractablePoint(crystalCacheChestId)) {
            "Expected crystal cache chest for sample content-pack harness."
        }
        session.automationMovePlayerTo(crystalChestPoint)
        check(session.perform(PlayerCommand.Interact)) { "Failed to open crystal cache chest for sample content-pack harness." }
        val searchPoint = requireNotNull(session.automationSearchPointForBinding(bindingId)) {
            "Missing search point for sample content-pack harness binding '$sampleSecretBindingId'."
        }
        val bindingSearchResult =
            session.automationSearchState()
                .firstOrNull { entry -> entry.bindingId == bindingId }
                ?.result
        if (bindingSearchResult != SearchActionResult.REVEALED) {
            session.automationMovePlayerTo(searchPoint)
            check(session.perform(PlayerCommand.Search)) { "Failed to reveal sample content-pack entrance for harness verification." }
        }

        val entrancePoint = requireNotNull(session.automationHiddenEntrancePointForBinding(bindingId)) {
            "Expected revealed hidden entrance for sample content-pack harness."
        }
        session.automationMovePlayerTo(entrancePoint)
        check(session.perform(PlayerCommand.Interact)) { "Failed to enter sample content-pack secret zone for harness verification." }
        check(session.automationVisitedSecretZoneIds().any { secretZone -> secretZone.id == sampleSecretZoneId }) {
            "Sample content-pack secret zone '$sampleSecretZoneId' was not visited through the runtime route."
        }

        val rewardPoint = requireNotNull(session.automationSecretRewardPointForBinding(bindingId)) {
            "Missing secret reward anchor for sample content-pack harness."
        }
        check(session.renderSnapshot().props.any { prop -> prop.propTypeId == "secret_reward" && Point(prop.x, prop.y) == rewardPoint }) {
            "Expected secret reward prop for sample content-pack harness."
        }
        session.automationMovePlayerTo(rewardPoint)
        check(session.perform(PlayerCommand.Interact)) { "Failed to claim sample content-pack reward through runtime route." }
        val generatedSpecialTemplateId = session.inventoryItems().firstOrNull { item -> item.specialTemplateId != null }?.specialTemplateId
        return SampleRewardTrace(
            seed = seed,
            generatedSpecialTemplateId = generatedSpecialTemplateId,
            touchedContentIds = session.automationActivePackTouchedContentIds(),
        )
    }

    private fun clearMonsters(session: FoundationGameSession) {
        val world = runtimeWorld(session)
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
    }

    private fun secretRewardProfileId(
        secretZone: SecretZoneDef?,
        hiddenEvent: HiddenEventDef?,
    ): String? {
        val reward = hiddenEvent?.rewards.orEmpty()
        if (reward.none { entry -> entry.payload is HiddenEventRewardPayload.SecretZoneReward } || secretZone == null) {
            return null
        }
        val resolvedReward = SecretRewardAuthority.resolve(secretZone = secretZone, hiddenEvent = hiddenEvent)
        return resolvedReward.rewardProfileId.takeIf { resolvedReward.mismatchReason == null }
    }

    private fun verifySamplePackResourceContracts(): ResourceContractCheck {
        val packRoot = ContentPackFixtureCatalog.samplePackRoot()
        val visualManifestPath = packRoot.resolve("visual/visual-manifest.json")
        val audioManifestPath = packRoot.resolve("audio/audio-manifest.json")
        val visualEntries =
            json.parseToJsonElement(Files.readString(visualManifestPath))
                .jsonObject
                .getValue("entries")
                .jsonArray
        val audioEntries =
            json.parseToJsonElement(Files.readString(audioManifestPath))
                .jsonObject
                .getValue("entries")
                .jsonArray
        val imagePlan = loadYamlMap(repoRoot().resolve(imagePlanPath))
        val audioPlan = loadYamlMap(repoRoot().resolve(audioPlanPath))
        val imagePlanEntries =
            (((imagePlan["phase2AssetGates"] as? Map<*, *>)?.get("P4-PR09")) as? Map<*, *>)
                ?.get("assets") as? List<*>
                ?: emptyList<Any?>()
        val audioPlanEntries = audioPlan["entries"] as? List<*> ?: emptyList<Any?>()

        val expectedVisualKeys =
            imagePlanEntries.map { raw ->
                raw.requiredMap().requiredString("visualKey")
            }.toSet()
        val expectedVisualPaths =
            imagePlanEntries.map { raw ->
                raw.requiredMap().requiredString("outputName")
            }.toSet()
        val actualVisualKeys =
            visualEntries.map { entry ->
                entry.jsonObject.getValue("key").jsonPrimitive.content
            }.toSet()
        val actualVisualPaths =
            visualEntries.map { entry ->
                entry.jsonObject.getValue("rawOutputPath").jsonPrimitive.content
            }.toSet()

        val expectedAudioKeys =
            audioPlanEntries.map { raw ->
                raw.requiredMap().requiredString("key")
            }.toSet()
        val expectedAudioPaths =
            audioPlanEntries.map { raw ->
                raw.requiredMap().requiredString("sourcePath")
            }.toSet()
        val actualAudioKeys =
            audioEntries.map { entry ->
                entry.jsonObject.getValue("key").jsonPrimitive.content
            }.toSet()
        val actualAudioPaths =
            audioEntries.map { entry ->
                entry.jsonObject.getValue("sourcePath").jsonPrimitive.content
            }.toSet()

        val missingVisualFiles =
            actualVisualPaths.filterNot { relativePath -> Files.isRegularFile(packRoot.resolve(relativePath)) }
        val missingAudioFiles =
            actualAudioPaths.filterNot { relativePath -> Files.isRegularFile(packRoot.resolve(relativePath)) }
        val reportPaths =
            listOf(
                repoRoot().resolve(imageGenerationReportPath),
                repoRoot().resolve(imageProcessingReportPath),
                repoRoot().resolve(audioProcessingReportPath),
            )
        val missingReports = reportPaths.filterNot(Files::isRegularFile)
        val details =
            buildList {
                if (expectedVisualKeys != actualVisualKeys) {
                    add("visualKeySetMismatch")
                }
                if (expectedVisualPaths != actualVisualPaths) {
                    add("visualPathSetMismatch")
                }
                if (expectedAudioKeys != actualAudioKeys) {
                    add("audioKeySetMismatch")
                }
                if (expectedAudioPaths != actualAudioPaths) {
                    add("audioPathSetMismatch")
                }
                if (missingVisualFiles.isNotEmpty()) {
                    add("missingVisualFiles=${missingVisualFiles.sorted()}")
                }
                if (missingAudioFiles.isNotEmpty()) {
                    add("missingAudioFiles=${missingAudioFiles.sorted()}")
                }
                if (missingReports.isNotEmpty()) {
                    add("missingReports=${missingReports.map(Path::toString).sorted()}")
                }
            }
        return ResourceContractCheck(
            verified = details.isEmpty(),
            details = details,
        )
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
            diagnosticDetails = emptyList(),
        )

    private fun precedenceMatches(
        scenario: HarnessScenario,
        resolvedOrder: List<String>,
        overlayOps: List<String>,
    ): Boolean =
        (scenario.expectedResolvedOrder.isEmpty() || resolvedOrder == scenario.expectedResolvedOrder.map(PackId::value)) &&
            (scenario.expectedOverlayOps.isEmpty() ||
                overlayOps.filter { op -> op != OverlayOp.ADD.name } == scenario.expectedOverlayOps.map(OverlayOp::name))

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
        bundle: ClientAssetBundle,
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
        bundle: ClientAssetBundle,
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
                    config = FoundationGameConfig(seed = seed, zoneId = "underground_river"),
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

    private fun analyze(results: List<ContentPackCaseResult>): ContentPackAnalysis {
        val legacyLootProfileSchemaRejectSummaries =
            results
                .filter { result ->
                    result.fixtureId == "legacy_v2_loot_profile_rejected" &&
                        result.success &&
                        result.diagnosticCodes.contains("content-pack.loot-profile.schema-version-mismatch")
                }.flatMap { result ->
                    result.diagnosticCodes.mapIndexedNotNull { index, diagnosticCode ->
                        if (diagnosticCode != "content-pack.loot-profile.schema-version-mismatch") {
                            return@mapIndexedNotNull null
                        }
                        val details = result.diagnosticDetails.getOrNull(index).orEmpty()
                        LegacyLootProfileSchemaRejectSummary(
                            fixtureId = result.fixtureId,
                            packId = details["packId"] ?: (result.packId ?: "unknown"),
                            diagnosticCode = diagnosticCode,
                            targetProfileId = details["targetProfileId"] ?: "unknown",
                            actualSchemaVersion = details["actualSchemaVersion"] ?: "unknown",
                            expectedSchemaVersion = details["expectedSchemaVersion"] ?: "unknown",
                        )
                    }
                }
        val aggregateFailures =
            buildList {
                if (results.any { result -> "case.diagnostic_code_mismatch" in result.failureReasons }) add("aggregate.diagnostic_code_mismatch")
                if (results.any { result -> result.localeResolutionFailureCount > 0 }) add("aggregate.locale_key_unresolved")
                if (results.any { result -> result.visualResolutionFailureCount > 0 }) add("aggregate.visual_key_unresolved")
                if (results.any { result -> result.audioResolutionFailureCount > 0 }) add("aggregate.audio_key_unresolved")
                if (results.any { result -> result.seedList.isNotEmpty() && !result.headlessRunSucceeded }) add("aggregate.headless_run_failure")
                if (results.any { result -> "case.base_fallback_not_verified" in result.failureReasons }) add("aggregate.base_fallback_failure")
                if (results.any { result -> !result.precedenceVerified }) add("aggregate.precedence_failure")
                if (results.any { result -> "case.resource_contract_failed" in result.failureReasons }) add("aggregate.resource_contract_failure")
                if (results.any { result -> "case.generated_special_template_missing" in result.failureReasons }) add("aggregate.generated_special_template_missing")
                if (results.any { result -> "case.sample_pack_runtime_touch_missing" in result.failureReasons }) add("aggregate.sample_pack_runtime_touch_missing")
                if (results.any { result -> "case.sample_pack_item_touch_missing" in result.failureReasons }) add("aggregate.sample_pack_item_touch_missing")
                if (results.any { result -> "case.sample_pack_secondary_secret_slot_missing" in result.failureReasons }) add("aggregate.sample_pack_secondary_secret_slot_missing")
                if (results.any { result -> "case.sample_pack_fixed_seed_visibility_missing" in result.failureReasons }) add("aggregate.sample_pack_fixed_seed_visibility_missing")
                if (results.any { result -> "legacy_v2_loot_profile_rejected" == result.fixtureId && !result.success }) {
                    add("aggregate.legacy_loot_profile_schema_reject_failure")
                }
            }
        val officialSampleResults = results.filter { result -> result.fixtureId == "official_sample_pack" }
        val touchedSampleRunCount =
            officialSampleResults.sumOf(ContentPackCaseResult::samplePackFixedSeedTouchedRunCount)
        val activeSampleFixedSeedRunCount =
            officialSampleResults.sumOf(ContentPackCaseResult::samplePackFixedSeedRunCount)
        val samplePackTouchedContentIds =
            officialSampleResults
                .flatMap(ContentPackCaseResult::samplePackFixedSeedTouchedContentIds)
                .distinct()
                .sorted()
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
                headlessRunFailureCount = results.count { result -> result.seedList.isNotEmpty() && !result.headlessRunSucceeded },
                fallbackFailureCount = results.count { result -> "case.base_fallback_not_verified" in result.failureReasons },
                precedenceFailureCount = results.count { result -> !result.precedenceVerified },
                resourceContractFailureCount = results.count { result -> "case.resource_contract_failed" in result.failureReasons },
                generatedTemplateFailureCount = results.count { result -> "case.generated_special_template_missing" in result.failureReasons },
                samplePackContentPlayerVisibilityRate =
                    if (activeSampleFixedSeedRunCount == 0) 0.0 else touchedSampleRunCount.toDouble() / activeSampleFixedSeedRunCount.toDouble(),
                samplePackTouchedContentIds = samplePackTouchedContentIds,
                samplePackAddOnlyMainPath = officialSampleResults.singleOrNull()?.samplePackAddOnlyMainPath == true,
                samplePackSecondarySecretSlotUsed = officialSampleResults.singleOrNull()?.samplePackSecondarySecretSlotUsed == true,
                samplePackFixedSeedVisibilityCase = officialSampleResults.singleOrNull()?.samplePackFixedSeedVisibilityCase == true,
                activeSampleFixedSeedRunCount = activeSampleFixedSeedRunCount,
                touchedSampleRunCount = touchedSampleRunCount,
                legacyLootProfileSchemaRejectCount = legacyLootProfileSchemaRejectSummaries.size,
                legacyLootProfileSchemaRejectSummaries = legacyLootProfileSchemaRejectSummaries,
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
                    put("resourceContractFailureCount", analysis.summary.resourceContractFailureCount)
                    put("generatedTemplateFailureCount", analysis.summary.generatedTemplateFailureCount)
                    put("samplePackContentPlayerVisibilityRate.reportOnly", analysis.summary.samplePackContentPlayerVisibilityRate)
                    putJsonArray("samplePackTouchedContentIds") {
                        analysis.summary.samplePackTouchedContentIds.forEach { contentId -> add(JsonPrimitive(contentId)) }
                    }
                    put("samplePackAddOnlyMainPath", analysis.summary.samplePackAddOnlyMainPath)
                    put("samplePackSecondarySecretSlotUsed", analysis.summary.samplePackSecondarySecretSlotUsed)
                    put("samplePackFixedSeedVisibilityCase", analysis.summary.samplePackFixedSeedVisibilityCase)
                    put("activeSampleFixedSeedRunCount", analysis.summary.activeSampleFixedSeedRunCount)
                    put("touchedSampleRunCount", analysis.summary.touchedSampleRunCount)
                    put("legacyLootProfileSchemaRejectCount", analysis.summary.legacyLootProfileSchemaRejectCount)
                    putJsonArray("legacyLootProfileSchemaRejectSummaries") {
                        analysis.summary.legacyLootProfileSchemaRejectSummaries.forEach { summary ->
                            add(summary.toJson())
                        }
                    }
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

    private fun loadYamlMap(path: Path): Map<String, Any?> =
        Files.newBufferedReader(path).use { reader ->
            @Suppress("UNCHECKED_CAST")
            (yaml.load<Map<String, Any?>>(reader) ?: emptyMap())
        }

    private fun zoneRewardBiasTags(zone: com.ktome.game.data.schema.ZoneSchemaV2): Set<String> =
        linkedSetOf<String>().apply {
            zone.uniqueContentTag
                ?.split('.', '_', '-')
                ?.map(String::trim)
                ?.filter(String::isNotBlank)
                ?.map(String::lowercase)
                ?.forEach(::add)
            zone.specialMechanics.map(String::lowercase).forEach(::add)
            zone.environmentTheme.takeIf(String::isNotBlank)?.lowercase()?.let(::add)
        }

    private fun hasSampleFixedSeedRuntimeTouch(touchedContentIds: List<String>): Boolean =
        sampleSecretZoneId in touchedContentIds &&
            sampleHiddenEventId in touchedContentIds &&
            sampleLootProfileId in touchedContentIds

    private fun hasSampleItemTouch(touchedContentIds: List<String>): Boolean =
        touchedContentIds.any { contentId ->
            contentId.startsWith("sample.flooded_relics.") &&
                contentId !in setOf(sampleSecretZoneId, sampleHiddenEventId, sampleLootProfileId)
        }

    private data class HarnessScenario(
        val fixtureId: String,
        val reportPackId: PackId,
        val selectionProvider: () -> ContentPackSelection,
        val seedList: List<Long> = emptyList(),
        val generatedTemplateSeedList: List<Long> = seedList,
        val expectedFailureCodes: Set<String> = emptySet(),
        val expectedResolvedOrder: List<PackId> = emptyList(),
        val expectedOverlayOps: List<OverlayOp> = emptyList(),
        val expectedSecretZoneId: String = sampleSecretZoneId,
        val expectedHiddenEventId: String = sampleHiddenEventId,
        val expectedSecretZoneNameKey: String? = null,
        val expectedSecretZoneVisualKey: String? = null,
        val expectedSecretZoneAudioProfile: String? = null,
        val expectedSecretRewardProfileId: String? = null,
        val expectedLootProfileId: String? = null,
        val expectedLootProfileSpecialTemplateTagPreference: List<String> = emptyList(),
        val expectedLootProfileAffixTagPreference: List<String> = emptyList(),
        val expectedSpecialTemplateIds: Set<String> = emptySet(),
        val localeKeys: List<String> = emptyList(),
        val visualKeys: List<String> = emptyList(),
        val audioKeys: List<String> = emptyList(),
        val expectFallbackToBase: Boolean = false,
        val verifyResourceContracts: Boolean = false,
        val verifyGeneratedTemplates: Boolean = false,
    )

    private data class ResolutionCheck(
        val resolvedKeys: List<String>,
        val failureCount: Int,
    )

    internal data class StructuredHarnessFailure(
        val diagnosticCodes: List<String>,
        val diagnostics: List<String>,
        val diagnosticDetails: List<Map<String, String>>,
    )
}

private fun runtimeWorld(session: FoundationGameSession): World =
    session.javaClass.getDeclaredField("world").let { field ->
        field.isAccessible = true
        field.get(session) as World
    }

private fun Any?.requiredMap(): Map<*, *> =
    this as? Map<*, *> ?: error("Entry must be a map.")

private fun Map<*, *>.requiredString(key: String): String =
    this[key]?.toString()?.trim()?.takeIf(String::isNotBlank)
        ?: error("Missing required string '$key'.")
