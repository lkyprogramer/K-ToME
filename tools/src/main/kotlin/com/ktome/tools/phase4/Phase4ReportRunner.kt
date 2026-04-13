package com.ktome.tools.phase4

import com.ktome.game.data.DataLoader
import com.ktome.tools.verification.VerificationBaseline
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.Locale
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

data class Phase4ReportRun(
    val taskCount: Int,
    val failedTaskCount: Int,
    val failedExperienceMetricCount: Int,
    val failedGateCount: Int,
    val summaryPath: Path,
    val markdownPath: Path,
)

private val phase4Json: Json = Json { prettyPrint = true }
private const val TERRAIN_BASELINE_RELATIVE_PATH: String =
    "docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline.json"
private val longRunItemSemanticTagsById: Map<String, List<String>> by lazy {
    DataLoader()
        .loadItemBundle()
        .baseItems
        .associate { item -> item.id to item.tags.sorted() }
}

private data class TerrainMetricBaseline(
    val baselineId: String,
    val metricDefinitionVersion: String,
    val sourceArtifactPath: String,
    val sourceBuildId: String?,
    val sourceGeneratedAt: String?,
    val metricsById: Map<String, TerrainMetricBaselineMetric>,
)

private data class TerrainMetricBaselineMetric(
    val metricId: String,
    val baselineRate: Double,
    val targetRelativeIncrease: Double,
    val numerator: Int,
    val denominator: Int,
    val numeratorLabel: String,
    val denominatorLabel: String,
    val sourceValueText: String,
    val normalizedFormula: String,
) {
    val targetRate: Double = baselineRate * (1.0 + targetRelativeIncrease)
}

private data class Phase4AggregateReport(
    val phaseId: String,
    val generatedAt: String,
    val buildId: String? = null,
    val locale: String? = null,
    val taskCount: Int,
    val passedTaskCount: Int,
    val failedTaskCount: Int,
    val failedExperienceMetricCount: Int,
    val failedGateCount: Int,
    val tasks: List<Phase4TaskAggregate>,
    val metricCatalog: List<Phase4MetricCatalogEntry>,
    val experienceMetrics: List<Phase4ExperienceMetric>,
)

internal data class Phase4TaskAggregate(
    val taskId: String,
    val status: String,
    val sourcePath: String,
    val buildId: String? = null,
    val locale: String? = null,
    val metrics: JsonObject,
)

private data class Phase4ExperienceMetric(
    val metricId: String,
    val sourceTaskId: String,
    val currentValue: JsonElement,
    val currentValueText: String,
    val target: String,
    val status: String,
    val note: String? = null,
)

private data class Phase4TaskDescriptor(
    val relativeSourcePath: String,
    val reader: (repoRoot: Path, sourcePath: Path, payload: JsonObject) -> Phase4TaskAggregate,
) {
    fun read(repoRoot: Path): Phase4TaskAggregate {
        val sourcePath = repoRoot.resolve(relativeSourcePath)
        val payload = readPhase4Json(sourcePath)
        return reader(repoRoot, sourcePath, payload)
    }
}

object Phase4ReportRunner {
    private const val SUMMARY_FILE: String = "phase4-summary.json"
    private const val MARKDOWN_FILE: String = "phase4-summary.md"
    private val CONTENT_PACK_ARTIFACT_MAX_SKEW: Duration = Duration.ofMinutes(30)
    private val json: Json = Json { prettyPrint = true }
    private val taskDescriptors: List<Phase4TaskDescriptor> =
        listOf(
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json",
                reader = ::readMapgenSmoke,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/solvability/solvability-summary.json",
                reader = ::readSolvabilityHarness,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/hidden/hidden-content-summary.json",
                reader = ::readHiddenContentHarness,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/hidden/organic-hidden-probe-summary.json",
                reader = ::readOrganicHiddenProbe,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "build/reports/harness/boss-harness.json",
                reader = ::readBossHarness,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "build/reports/harness/long-run-full.json",
                reader = ::readLongRunLabFull,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-summary.json",
                reader = ::readTerrainInteractionBatch,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-summary.json",
                reader = ::readWhiteBoxMapgen,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/solvability/whitebox-solvability-summary.json",
                reader = ::readWhiteBoxSolvability,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/loot/loot-balance-summary.json",
                reader = ::readLootBalanceLab,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json",
                reader = ::readWhiteBoxLoot,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-summary.json",
                reader = ::readWhiteBoxHiddenContent,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/content-pack/whitebox-content-pack-summary.json",
                reader = ::readWhiteBoxContentPack,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/content-pack/content-pack-summary.json",
                reader = ::readContentPackHarness,
            ),
        )

    fun run(): Phase4ReportRun {
        val repoRoot = repoRoot()
        val outputDir = reportDir()
        Files.createDirectories(outputDir)

        val taskReports = collectTaskAggregates(repoRoot)
        val sourcePathByTaskId = taskReports.associate { task -> task.taskId to task.sourcePath }
        val experienceMetrics = buildExperienceMetrics(repoRoot = repoRoot, tasks = taskReports)
        val metricCatalog =
            Phase4MetricCatalog.entries(
                sourcePathByTaskId = sourcePathByTaskId,
                targetTextByMetricId = experienceMetrics.associate { metric -> metric.metricId to metric.target },
            )
        val failedTaskCount = countFailedStatuses(taskReports.map(Phase4TaskAggregate::status))
        val failedExperienceMetricCount = countFailedStatuses(experienceMetrics.map(Phase4ExperienceMetric::status))
        val failedGateCount = failedTaskCount + failedExperienceMetricCount
        val aggregate =
            Phase4AggregateReport(
                phaseId = "P4",
                generatedAt = Instant.now().toString(),
                buildId = taskReports.firstNotNullOfOrNull { task -> task.buildId },
                locale = taskReports.firstNotNullOfOrNull { task -> task.locale },
                taskCount = taskReports.size,
                passedTaskCount = taskReports.count { task -> task.status == "PASS" },
                failedTaskCount = failedTaskCount,
                failedExperienceMetricCount = failedExperienceMetricCount,
                failedGateCount = failedGateCount,
                tasks = taskReports,
                metricCatalog = metricCatalog,
                experienceMetrics = experienceMetrics,
            )
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        val markdownPath = outputDir.resolve(MARKDOWN_FILE)
        Files.writeString(summaryPath, json.encodeToString(JsonElement.serializer(), aggregate.toJson()))
        Files.writeString(markdownPath, renderMarkdown(aggregate))
        return Phase4ReportRun(
            taskCount = aggregate.taskCount,
            failedTaskCount = aggregate.failedTaskCount,
            failedExperienceMetricCount = aggregate.failedExperienceMetricCount,
            failedGateCount = aggregate.failedGateCount,
            summaryPath = summaryPath,
            markdownPath = markdownPath,
        )
    }

    internal fun collectTaskAggregates(repoRoot: Path = repoRoot()): List<Phase4TaskAggregate> =
        taskDescriptors.map { descriptor -> descriptor.read(repoRoot) }

    private fun readMapgenSmoke(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failureCount = summary.intValue("failureCount")
        val emptyMapCount = summary.intValue("emptyMapCount")
        val unreachableCount = summary.intValue("unreachableCount")
        return Phase4TaskAggregate(
            taskId = "mapgenSmoke",
            status = if (failureCount == 0 && emptyMapCount == 0 && unreachableCount == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("totalCases", summary.intValue("totalCases"))
                    put("distinctSeedCount", summary.intValue("distinctSeedCount"))
                    put("failureCount", failureCount)
                    put("emptyMapCount", emptyMapCount)
                    put("unreachableCount", unreachableCount)
                    put("p95GenerationMillis", summary.intValue("p95GenerationMillis"))
                },
        )
    }

    private fun readSolvabilityHarness(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failureCount = summary.intValue("failureCount")
        val criticalPathFailureCount = summary.intValue("criticalPathFailureCount")
        return Phase4TaskAggregate(
            taskId = "solvabilityHarness",
            status = if (failureCount == 0 && criticalPathFailureCount == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("totalCases", summary.intValue("totalCases"))
                    put("distinctSeedCount", summary.intValue("distinctSeedCount"))
                    put("failureCount", failureCount)
                    put("criticalPathFailureCount", criticalPathFailureCount)
                    put("casesWithBacktrackProof", summary.intValue("casesWithBacktrackProof"))
                    put("casesWithSecretReveal", summary.intValue("casesWithSecretReveal"))
                    put("casesWithSearchFailure", summary.intValue("casesWithSearchFailure"))
                    put("providedDiscoveryTagCount", summary.intValue("providedDiscoveryTagCount"))
                    put("providedDiscoveryTags", summary.getValue("providedDiscoveryTags"))
                    put("hiddenAnchorFamilyFailureCount", summary.intValue("hiddenAnchorFamilyFailureCount"))
                    put("requiredHiddenAnchorFamilies", summary.getValue("requiredHiddenAnchorFamilies"))
                    put("observedHiddenAnchorFamilies", summary.getValue("observedHiddenAnchorFamilies"))
                },
        )
    }

    private fun readHiddenContentHarness(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failureCount = summary.intValue("failureCount")
        return Phase4TaskAggregate(
            taskId = "hiddenContentHarness",
            status = if (failureCount == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("scriptedVerification", summary.getValue("scriptedVerification"))
                    put("totalCases", summary.intValue("totalCases"))
                    put("distinctSeedCount", summary.intValue("distinctSeedCount"))
                    put("failureCount", failureCount)
                    put("caseFailureCount", summary.intValue("caseFailureCount"))
                    put("aggregateFailureCount", summary.intValue("aggregateFailureCount"))
                    put("hiddenEventTriggerCount", summary.intValue("hiddenEventTriggerCount"))
                    put("hiddenEventTriggerRate", summary.doubleValue("hiddenEventTriggerRate"))
                    put("secretZoneDiscoveryCount", summary.intValue("secretZoneDiscoveryCount"))
                    put("secretZoneDiscoveryRate", summary.doubleValue("secretZoneDiscoveryRate"))
                    put("explicitSearchRevealCount", summary.intValue("explicitSearchRevealCount"))
                    put("primerActionUsedCount", summary.intValue("primerActionUsedCount"))
                    put("primerFreeCaseCount", summary.intValue("primerFreeCaseCount"))
                    put("searchFailureCount", summary.intValue("searchFailureCount"))
                    put("zeroHiddenEventZoneCount", summary.intValue("zeroHiddenEventZoneCount"))
                    put("zeroSecretZoneZoneCount", summary.intValue("zeroSecretZoneZoneCount"))
                    put("criticalPathFailureCount", summary.intValue("criticalPathFailureCount"))
                    put("triggerContextFailureCount", summary.intValue("triggerContextFailureCount"))
                    put("secretRewardNodeMissingCount", summary.intValue("secretRewardNodeMissingCount"))
                    put("rewardBudgetFailureCount", summary.intValue("rewardBudgetFailureCount"))
                    put("threatBudgetFailureCount", summary.intValue("threatBudgetFailureCount"))
                    put("searchFailureBlockingCount", summary.intValue("searchFailureBlockingCount"))
                    put("proofMismatchCount", summary.intValue("proofMismatchCount"))
                    put("runtimeReturnDestinationMismatchCount", summary.intValue("runtimeReturnDestinationMismatchCount"))
                    put("hiddenEventRegistryCount", summary.intValue("hiddenEventRegistryCount"))
                    put("secretZoneRegistryCount", summary.intValue("secretZoneRegistryCount"))
                    put("hiddenTriggerTypeCoverage", summary.getValue("hiddenTriggerTypeCoverage"))
                    put("hiddenTriggerTypeSet", summary.getValue("hiddenTriggerTypeSet"))
                    put("secretEntranceBindingCoverage", summary.getValue("secretEntranceBindingCoverage"))
                    put("secretEntranceBindingSet", summary.getValue("secretEntranceBindingSet"))
                },
        )
    }

    private fun readOrganicHiddenProbe(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val runtimeFailureCount = summary.intValue("runtimeFailureCount")
        return Phase4TaskAggregate(
            taskId = "organicHiddenProbe",
            status = if (runtimeFailureCount == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("scriptedVerification", summary.getValue("scriptedVerification"))
                    put("primerActionUsedCount", summary.intValue("primerActionUsedCount"))
                    put("totalCases", summary.intValue("totalCases"))
                    put("distinctSeedCount", summary.intValue("distinctSeedCount"))
                    put("runtimeFailureCount", runtimeFailureCount)
                    put("searchAttemptCount", summary.intValue("searchAttemptCount"))
                    put("runsWithSearchActionCount", summary.intValue("runsWithSearchActionCount"))
                    put("searchActionUseCount", summary.intValue("searchActionUseCount"))
                    put("searchActionUseRate", summary.doubleValue("searchActionUseRate"))
                    put("discoveryWithoutPrimerCount", summary.intValue("discoveryWithoutPrimerCount"))
                    put("organicHiddenDiscoveryRate", summary.doubleValue("organicHiddenDiscoveryRate"))
                    put("secretZoneEntryCount", summary.intValue("secretZoneEntryCount"))
                    put("secretZoneEntryRate", summary.doubleValue("secretZoneEntryRate"))
                    put("averageFirstHiddenDiscoveryTurn", summary.getValue("averageFirstHiddenDiscoveryTurn"))
                    put("averageFirstSecretZoneEntryTurn", summary.getValue("averageFirstSecretZoneEntryTurn"))
                    put("probeBotId", summary.getValue("probeBotId"))
                    put("probeTurnBudget", summary.intValue("probeTurnBudget"))
                    put("probeMaxFloor", summary.intValue("probeMaxFloor"))
                    put("zones", payload.getValue("zones"))
                    put("notes", payload.getValue("notes"))
                },
        )
    }

    private fun readBossHarness(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val reports = payload.getValue("reports").jsonArray
        val pairReports = payload["pairReports"]?.jsonArray.orEmpty()
        val whiteBoxSourcePath = repoRoot.resolve("tools/build/reports/phase4/whitebox/boss/whitebox-boss-summary.json")
        val whiteBoxPayload = readPhase4Json(whiteBoxSourcePath)
        val whiteBoxHeader = whiteBoxPayload.getValue("header").jsonObject
        val whiteBoxSummary = whiteBoxPayload.getValue("summary").jsonObject
        val whiteBoxFailedAssertions = whiteBoxSummary.intValue("failedAssertions")
        val whiteBoxFirstFailedJoinKey = whiteBoxPayload["firstFailedJoinKey"]
        val perEncounterMetrics = aggregateMetrics(whiteBoxPayload, "per-encounter")
        val corpusMetrics = aggregateMetrics(whiteBoxPayload, "corpus")
        val failureCount =
            reports.count { element -> !element.jsonObject.getValue("success").jsonPrimitive.content.toBooleanStrict() } +
                pairReports.count { element -> !element.jsonObject.getValue("success").jsonPrimitive.content.toBooleanStrict() }
        val aiTraceCountTotal = reports.sumOf { element -> element.jsonObject.intValue("aiTraceCount") }
        val bossTraceCountTotal = reports.sumOf { element -> element.jsonObject.intValue("bossTraceCount") }
        val distinctTemplateCount = reports.map { element -> element.jsonObject.stringValue("templateId") }.distinct().size
        val variantCount = reports.count { element -> "variantId" in element.jsonObject }
        val phaseGraphStructuralDiffCount = pairReports.sumOf { element -> element.jsonObject.intValue("phaseGraphStructuralDiffCount") }
        return Phase4TaskAggregate(
            taskId = "bossHarness",
            status = if (failureCount == 0 && whiteBoxFailedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = whiteBoxHeader.stringValue("buildId"),
            locale = whiteBoxHeader.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("scriptVersion", payload.getValue("scriptVersion").jsonPrimitive.content)
                    put("reportCount", reports.size)
                    put("pairCount", pairReports.size)
                    put("failureCount", failureCount)
                    put("distinctTemplateCount", distinctTemplateCount)
                    put("variantCount", variantCount)
                    put("aiTraceCountTotal", aiTraceCountTotal)
                    put("bossTraceCountTotal", bossTraceCountTotal)
                    put("phaseGraphStructuralDiffCount", phaseGraphStructuralDiffCount)
                    put("whiteBoxFailedAssertions", whiteBoxFailedAssertions)
                    put("whiteBoxFailedCaseCount", whiteBoxPayload.intValue("failedCaseCount"))
                    put("whiteBoxFailedAggregateCount", whiteBoxPayload.intValue("failedAggregateCount"))
                    put("whiteBoxArtifactCount", whiteBoxSummary.intValue("artifactCount"))
                    put("whiteBoxSummaryPath", relativize(repoRoot, whiteBoxSourcePath))
                    put("perEncounterAggregateMetrics", perEncounterMetrics)
                    put("corpusAggregateMetrics", corpusMetrics)
                    put("perEncounterPairCount", perEncounterMetrics.getValue("pairCount"))
                    put("perEncounterVariantCount", perEncounterMetrics.getValue("variantCount"))
                    put("eliteMutationDistinctCount", corpusMetrics.getValue("eliteMutationDistinctCount"))
                    put("eliteMutationValidPairCount", corpusMetrics.getValue("eliteMutationValidPairCount"))
                    put("bossVariantCount", corpusMetrics.getValue("bossVariantCount"))
                    put("bossVariantMutationPairwiseDistinct", corpusMetrics.getValue("bossVariantMutationPairwiseDistinct"))
                    put("terrainPreferenceVariantCount", corpusMetrics.getValue("terrainPreferenceVariantCount"))
                    put("terrainPreferenceAvailableVariantCount", corpusMetrics.getValue("terrainPreferenceAvailableVariantCount"))
                    put("terrainPreferenceImplementedCount", corpusMetrics.getValue("terrainPreferenceImplementedCount"))
                    put("terrainPreferenceImplementedRate", corpusMetrics.getValue("terrainPreferenceImplementedRate"))
                    put("mutationTierDistribution", corpusMetrics.getValue("mutationTierDistribution"))
                    put("bossVariantMutationSets", corpusMetrics.getValue("bossVariantMutationSets"))
                    put("bossVariantPreferredTerrainTags", corpusMetrics.getValue("bossVariantPreferredTerrainTags"))
                    whiteBoxFirstFailedJoinKey?.let { joinKey -> put("whiteBoxFirstFailedJoinKey", joinKey.toString()) }
                },
        )
    }

    private fun readLongRunLabFull(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val fullRouteCount = payload.intValue("fullRouteCount")
        val professionTerminalWeaponDistribution = payload.getValue("professionTerminalWeaponDistribution")
        val professionTopWeaponBaseIds =
            payload["professionTopWeaponBaseIds"] ?: deriveProfessionTopWeaponBaseIds(professionTerminalWeaponDistribution.jsonObject)
        val professionTopWeaponSemanticTags =
            payload["professionTopWeaponSemanticTags"]
                ?: deriveProfessionTopWeaponSemanticTags(professionTopWeaponBaseIds.jsonObject)
        return Phase4TaskAggregate(
            taskId = "longRunLab",
            status = if (fullRouteCount > 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = payload.stringValue("buildId"),
            locale = payload.stringValue("localeId"),
            metrics =
                buildJsonObject {
                    put("scenarioCount", payload.intValue("scenarioCount"))
                    put("fullRouteCount", fullRouteCount)
                    put("branchInclusiveCount", payload.intValue("branchInclusiveCount"))
                    put("terminalWeaponBaseDiversity", payload.intValue("terminalWeaponBaseDiversity"))
                    put("crossProfessionTopWeaponDominance", payload.doubleValue("crossProfessionTopWeaponDominance"))
                    put("professionAlignedWeaponAdoptionRate", payload.doubleValue("professionAlignedWeaponAdoptionRate"))
                    put("alignedFullRouteSampleCount", payload.intValue("alignedFullRouteSampleCount"))
                    put("crossProfessionTopWeaponCount", payload.intValue("crossProfessionTopWeaponCount"))
                    payload["crossProfessionTopWeaponBaseId"]?.let { topWeaponBaseId -> put("crossProfessionTopWeaponBaseId", topWeaponBaseId) }
                    put("professionTerminalWeaponDistribution", professionTerminalWeaponDistribution)
                    put("professionTopWeaponBaseIds", professionTopWeaponBaseIds)
                    put("professionTopWeaponSemanticTags", professionTopWeaponSemanticTags)
                },
        )
    }

    private fun deriveProfessionTopWeaponBaseIds(distribution: JsonObject): JsonObject =
        buildJsonObject {
            distribution.entries
                .sortedBy(Map.Entry<String, JsonElement>::key)
                .forEach { (professionId, professionDistribution) ->
                    val topWeaponBaseId =
                        professionDistribution
                            .jsonObject
                            .maxByOrNull { (_, count) -> count.jsonPrimitive.content.toInt() }
                            ?.key
                    if (topWeaponBaseId != null) {
                        put(professionId, topWeaponBaseId)
                    }
                }
        }

    private fun deriveProfessionTopWeaponSemanticTags(topWeaponBaseIds: JsonObject): JsonObject =
        buildJsonObject {
            topWeaponBaseIds.entries
                .sortedBy(Map.Entry<String, JsonElement>::key)
                .forEach { (professionId, weaponBaseIdElement) ->
                    val semanticTags = longRunItemSemanticTagsById[weaponBaseIdElement.jsonPrimitive.content].orEmpty()
                    putJsonArray(professionId) {
                        semanticTags.forEach { tag -> add(JsonPrimitive(tag)) }
                    }
                }
        }

    private fun readTerrainInteractionBatch(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val corpusMetrics = aggregateMetrics(payload, "corpus")
        val failedAssertions = summary.intValue("failedAssertions")
        return Phase4TaskAggregate(
            taskId = "terrainInteractionBatch",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                    put("failedCaseCount", payload.intValue("failedCaseCount"))
                    put("failedAggregateCount", payload.intValue("failedAggregateCount"))
                    put("corpusAggregateMetrics", corpusMetrics)
                    put("terrainTaggedCombatExposureRate", corpusMetrics.getValue("terrainTaggedCombatExposureRate"))
                    put("terrainInteractionEncounterRate", corpusMetrics.getValue("terrainInteractionEncounterRate"))
                    put("combatCount", corpusMetrics.getValue("combatCount"))
                    put("taggedCombatCount", corpusMetrics.getValue("taggedCombatCount"))
                    put("triggeredInteractionCombatCount", corpusMetrics.getValue("triggeredInteractionCombatCount"))
                    put("preferredTerrainCaseCount", corpusMetrics.getValue("preferredTerrainCaseCount"))
                    put("preferredTerrainImplementedCount", corpusMetrics.getValue("preferredTerrainImplementedCount"))
                    put("preferredTerrainCaseImplementationRate", corpusMetrics.getValue("preferredTerrainCaseImplementationRate"))
                    put("preferredTerrainCombatCount", corpusMetrics.getValue("preferredTerrainCombatCount"))
                    put("preferredTerrainImplementedCombatCount", corpusMetrics.getValue("preferredTerrainImplementedCombatCount"))
                    put("preferredTerrainCombatImplementationRate", corpusMetrics.getValue("preferredTerrainCombatImplementationRate"))
                    put("terrainMetricDefinitionVersion", corpusMetrics.getValue("terrainMetricDefinitionVersion"))
                    put("terrainTaggedCombatExposureFormula", corpusMetrics.getValue("terrainTaggedCombatExposureFormula"))
                    put("terrainInteractionEncounterFormula", corpusMetrics.getValue("terrainInteractionEncounterFormula"))
                    put("decisionPathByCurrentMetrics", corpusMetrics.getValue("decisionPathByCurrentMetrics"))
                    put("preferredTerrainTagsSeen", corpusMetrics.getValue("preferredTerrainTagsSeen"))
                    put("combatSampledZoneIds", corpusMetrics.getValue("combatSampledZoneIds"))
                    put("combatSampledZoneExclusionNotes", corpusMetrics.getValue("combatSampledZoneExclusionNotes"))
                    put("perZoneEncounterLowerBoundTarget", corpusMetrics.getValue("perZoneEncounterLowerBoundTarget"))
                    put("perZoneEncounterFailures", corpusMetrics.getValue("perZoneEncounterFailures"))
                    put("terrainCoverageByZone", corpusMetrics.getValue("terrainCoverageByZone"))
                    payload["firstFailedJoinKey"]?.let { joinKey -> put("firstFailedJoinKey", joinKey.toString()) }
                },
        )
    }

    private fun readWhiteBoxMapgen(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        val corpusMetrics = aggregateMetrics(payload, "corpus")
        return Phase4TaskAggregate(
            taskId = "whiteBoxMapgen",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                    put("corpusAggregateMetrics", corpusMetrics)
                    put("requiredHiddenAnchorFamilies", corpusMetrics.getValue("requiredHiddenAnchorFamilies"))
                    put("observedHiddenAnchorFamilies", corpusMetrics.getValue("observedHiddenAnchorFamilies"))
                },
        )
    }

    private fun readWhiteBoxSolvability(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        return Phase4TaskAggregate(
            taskId = "whiteBoxSolvability",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                },
        )
    }

    private fun readLootBalanceLab(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val clamp = payload.getValue("magicFindClampComparison").jsonObject
        val failedExpectationCount = summary.intValue("failedExpectationCount")
        return Phase4TaskAggregate(
            taskId = "lootBalanceLab",
            status = if (failedExpectationCount == 0 && clamp.getValue("withinTolerance").jsonPrimitive.content.toBooleanStrict()) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("matrixCount", summary.intValue("matrixCount"))
                    put("totalRolls", summary.intValue("totalRolls"))
                    put("failedExpectationCount", failedExpectationCount)
                    put("affixCount", payload.getValue("specialTemplatePool").jsonObject.intValue("affixCount"))
                    put("uniqueTemplateCount", payload.getValue("specialTemplatePool").jsonObject.intValue("uniqueTemplateCount"))
                    put("artifactTemplateCount", payload.getValue("specialTemplatePool").jsonObject.intValue("artifactTemplateCount"))
                    put("totalCount", payload.getValue("specialTemplatePool").jsonObject.intValue("totalCount"))
                    put("rarePityActivations", summary.intValue("rarePityActivations"))
                    put("uniquePityActivations", summary.intValue("uniquePityActivations"))
                    put("maxMagicRateDrift", summary.doubleValue("maxMagicRateDrift"))
                    put("maxRareRateDrift", summary.doubleValue("maxRareRateDrift"))
                    put("maxUniqueRelativeError", summary.doubleValue("maxUniqueRelativeError"))
                    put("maxArtifactRelativeError", summary.doubleValue("maxArtifactRelativeError"))
                    put("clampWithinTolerance", clamp.getValue("withinTolerance").jsonPrimitive.content.toBooleanStrict())
                    put("clampMaxDistributionDelta", clamp.getValue("maxDistributionDelta").jsonPrimitive.content.toDouble())
                },
        )
    }

    private fun readContentPackHarness(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val whiteBoxSourcePath = repoRoot.resolve("tools/build/reports/phase4/whitebox/content-pack/whitebox-content-pack-summary.json")
        val whiteBoxPayload = readPhase4Json(whiteBoxSourcePath)
        requireFreshnessAligned(
            primaryPath = sourcePath,
            primaryPayload = payload,
            secondaryPath = whiteBoxSourcePath,
            secondaryPayload = whiteBoxPayload,
            maxSkew = CONTENT_PACK_ARTIFACT_MAX_SKEW,
        )
        val whiteBoxSummary = whiteBoxPayload.getValue("summary").jsonObject
        val whiteBoxFailedAssertions = whiteBoxSummary.intValue("failedAssertions")
        val failureCount = summary.intValue("failureCount")
        val whiteBoxCorpusMetrics = aggregateMetrics(whiteBoxPayload, "corpus")
        return Phase4TaskAggregate(
            taskId = "contentPackHarness",
            status = if (failureCount == 0 && whiteBoxFailedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("totalCases", summary.intValue("totalCases"))
                    put("failureCount", failureCount)
                    put("caseFailureCount", summary.intValue("caseFailureCount"))
                    put("aggregateFailureCount", summary.intValue("aggregateFailureCount"))
                    put("successfulRuntimeCaseCount", summary.intValue("successfulRuntimeCaseCount"))
                    put("expectedFailureCaseCount", summary.intValue("expectedFailureCaseCount"))
                    put("diagnosticMismatchCount", summary.intValue("diagnosticMismatchCount"))
                    put("localeResolutionFailureCount", summary.intValue("localeResolutionFailureCount"))
                    put("visualResolutionFailureCount", summary.intValue("visualResolutionFailureCount"))
                    put("audioResolutionFailureCount", summary.intValue("audioResolutionFailureCount"))
                    put("headlessRunFailureCount", summary.intValue("headlessRunFailureCount"))
                    put("fallbackFailureCount", summary.intValue("fallbackFailureCount"))
                    put("precedenceFailureCount", summary.intValue("precedenceFailureCount"))
                    put("resourceContractFailureCount", summary.intValue("resourceContractFailureCount"))
                    put("generatedTemplateFailureCount", summary.intValue("generatedTemplateFailureCount"))
                    put("legacyLootProfileSchemaRejectCount", summary.intValue("legacyLootProfileSchemaRejectCount"))
                    put("legacyLootProfileSchemaRejectSummaries", summary.getValue("legacyLootProfileSchemaRejectSummaries"))
                    put("whiteBoxFailedAssertions", whiteBoxFailedAssertions)
                    put("whiteBoxSummaryPath", relativize(repoRoot, whiteBoxSourcePath))
                    put("whiteBoxCorpusAggregateMetrics", whiteBoxCorpusMetrics)
                    put("contentPackArtifactTimestamp", payload.getValue("header").jsonObject.getValue("timestamp"))
                    put("whiteBoxContentPackArtifactTimestamp", whiteBoxPayload.getValue("header").jsonObject.getValue("timestamp"))
                },
        )
    }

    private fun readWhiteBoxLoot(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        val corpusMetrics = aggregateMetrics(payload, "corpus")
        return Phase4TaskAggregate(
            taskId = "whiteBoxLoot",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                    put("affixCount", corpusMetrics.getValue("affixCount"))
                    put("uniqueTemplateCount", corpusMetrics.getValue("uniqueTemplateCount"))
                    put("artifactTemplateCount", corpusMetrics.getValue("artifactTemplateCount"))
                    put("totalCount", corpusMetrics.getValue("totalCount"))
                    put("corpusAggregateMetrics", corpusMetrics)
                    put("lootProfileAverageBaseItemOverlap", corpusMetrics.getValue("lootProfileAverageBaseItemOverlap"))
                    put("lootProfileMaxBaseItemOverlap", corpusMetrics.getValue("lootProfileMaxBaseItemOverlap"))
                    put("lootProfileDistinctBaseItemCount", corpusMetrics.getValue("lootProfileDistinctBaseItemCount"))
                    put("lootProfileBaseItemOverlapMatrix", corpusMetrics.getValue("lootProfileBaseItemOverlapMatrix"))
                    put("sameZoneSecretVsCadenceMaxOverlap", corpusMetrics.getValue("sameZoneSecretVsCadenceMaxOverlap"))
                    put("sameZoneSecretVsRewardMaxOverlap", corpusMetrics.getValue("sameZoneSecretVsRewardMaxOverlap"))
                    put("sameZoneSecretVsCadencePairs", corpusMetrics.getValue("sameZoneSecretVsCadencePairs"))
                    put("sameZoneSecretVsRewardPairs", corpusMetrics.getValue("sameZoneSecretVsRewardPairs"))
                    put("localIdentityFailurePairs", corpusMetrics.getValue("localIdentityFailurePairs"))
                    put("affixPassiveCoverage", corpusMetrics.getValue("affixPassiveCoverage"))
                    put("affixPassiveKinds", corpusMetrics.getValue("affixPassiveKinds"))
                    put("uniqueArtifactOutcomeCount", corpusMetrics.getValue("uniqueArtifactOutcomeCount"))
                    put("meaningfulUniqueArtifactSwapCount", corpusMetrics.getValue("meaningfulUniqueArtifactSwapCount"))
                    put("uniqueArtifactMeaningfulSwapRate", corpusMetrics.getValue("uniqueArtifactMeaningfulSwapRate"))
                },
        )
    }

    private fun readWhiteBoxContentPack(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        return Phase4TaskAggregate(
            taskId = "whiteBoxContentPack",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                    put("failedCaseCount", payload.intValue("failedCaseCount"))
                    put("failedAggregateCount", payload.intValue("failedAggregateCount"))
                    payload["firstFailedJoinKey"]?.let { joinKey -> put("firstFailedJoinKey", joinKey.toString()) }
                },
        )
    }

    private fun readWhiteBoxHiddenContent(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        val corpusMetrics = aggregateMetrics(payload, "corpus")
        return Phase4TaskAggregate(
            taskId = "whiteBoxHiddenContent",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                    put("failedCaseCount", payload.intValue("failedCaseCount"))
                    put("failedAggregateCount", payload.intValue("failedAggregateCount"))
                    put("corpusAggregateMetrics", corpusMetrics)
                    put("hiddenEventRegistryCount", corpusMetrics.getValue("hiddenEventRegistryCount"))
                    put("secretZoneRegistryCount", corpusMetrics.getValue("secretZoneRegistryCount"))
                    put("hiddenTriggerTypeCoverage", corpusMetrics.getValue("hiddenTriggerTypeCoverage"))
                    put("hiddenTriggerTypeSet", corpusMetrics.getValue("hiddenTriggerTypeSet"))
                    put("secretEntranceBindingCoverage", corpusMetrics.getValue("secretEntranceBindingCoverage"))
                    put("secretEntranceBindingSet", corpusMetrics.getValue("secretEntranceBindingSet"))
                    payload["firstFailedJoinKey"]?.let { joinKey -> put("firstFailedJoinKey", joinKey.toString()) }
                },
        )
    }

    private fun buildExperienceMetrics(
        repoRoot: Path,
        tasks: List<Phase4TaskAggregate>,
    ): List<Phase4ExperienceMetric> {
        val tasksById = tasks.associateBy(Phase4TaskAggregate::taskId)
        val loot = requireTask(tasksById, "whiteBoxLoot")
        val scriptedHidden = requireTask(tasksById, "hiddenContentHarness")
        val organicHidden = requireTask(tasksById, "organicHiddenProbe")
        val longRun = requireTask(tasksById, "longRunLab")
        val terrain = requireTask(tasksById, "terrainInteractionBatch")
        val scriptedHiddenBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.SCRIPTED_HIDDEN_BASELINE_RELATIVE_PATH))
        val organicHiddenBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.ORGANIC_HIDDEN_BASELINE_RELATIVE_PATH))
        val lootBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH))
        val terminalBuildBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.TERMINAL_BUILD_BASELINE_RELATIVE_PATH))
        val terrainUnifiedBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.TERRAIN_UNIFIED_BASELINE_RELATIVE_PATH))
        val terrainPerZoneBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.TERRAIN_PER_ZONE_BASELINE_RELATIVE_PATH))
        val terrainBaseline = readTerrainBaseline(repoRoot)
        val scriptedHiddenRange = scriptedHiddenBaseline.requiredMetric("scriptedHiddenVerificationRate")
        val organicHiddenRange = organicHiddenBaseline.requiredMetric("organicHiddenDiscoveryRate")
        val cadenceOverlapRange = lootBaseline.requiredMetric("sameZoneSecretVsCadenceMaxOverlap")
        val rewardOverlapRange = lootBaseline.requiredMetric("sameZoneSecretVsRewardMaxOverlap")
        val diversityRange = terminalBuildBaseline.requiredMetric("terminalWeaponBaseDiversity")
        val dominanceRange = terminalBuildBaseline.requiredMetric("crossProfessionTopWeaponDominance")
        val adoptionRange = terminalBuildBaseline.requiredMetric("professionAlignedWeaponAdoptionRate")
        val terrainAggregateRange = terrainUnifiedBaseline.requiredMetric("terrainInteractionEncounterRate.aggregate")
        val terrainPerZoneRange = terrainPerZoneBaseline.requiredMetric("terrainInteractionEncounterRate.per_zone_lower_bound")

        val scriptedTotalCases = scriptedHidden.metrics.intValue("totalCases")
        val scriptedFailureCount = scriptedHidden.metrics.intValue("failureCount")
        val scriptedHiddenVerificationRate =
            if (scriptedTotalCases == 0) {
                0.0
            } else {
                (scriptedTotalCases - scriptedFailureCount).toDouble() / scriptedTotalCases.toDouble()
            }
        val organicTotalCases = organicHidden.metrics.intValue("totalCases")
        val organicDiscoveryCount = organicHidden.metrics.intValue("discoveryWithoutPrimerCount")
        val organicHiddenDiscoveryRate = organicHidden.metrics.doubleValue("organicHiddenDiscoveryRate")
        val organicSearchActionUseRate = organicHidden.metrics.doubleValue("searchActionUseRate")
        val organicSecretZoneEntryRate = organicHidden.metrics.doubleValue("secretZoneEntryRate")
        val sameZoneSecretVsCadenceMaxOverlap = loot.metrics.doubleValue("sameZoneSecretVsCadenceMaxOverlap")
        val sameZoneSecretVsRewardMaxOverlap = loot.metrics.doubleValue("sameZoneSecretVsRewardMaxOverlap")
        val terminalWeaponBaseDiversity = longRun.metrics.intValue("terminalWeaponBaseDiversity")
        val crossProfessionTopWeaponDominance = longRun.metrics.doubleValue("crossProfessionTopWeaponDominance")
        val professionAlignedWeaponAdoptionRate = longRun.metrics.doubleValue("professionAlignedWeaponAdoptionRate")
        val alignedFullRouteSampleCount = longRun.metrics.intValue("alignedFullRouteSampleCount")
        val fullRouteCount = longRun.metrics.intValue("fullRouteCount")
        val professionTerminalWeaponDistribution = longRun.metrics.getValue("professionTerminalWeaponDistribution")
        val professionTopWeaponBaseIds = longRun.metrics.getValue("professionTopWeaponBaseIds")
        val professionTopWeaponSemanticTags = longRun.metrics.getValue("professionTopWeaponSemanticTags")
        val terminalWeaponBaseNote =
            professionTerminalWeaponDistribution.jsonObject.values
                .flatMap { distribution -> distribution.jsonObject.keys }
                .distinct()
                .sorted()
                .joinToString()
        val professionTopWeaponSemanticNote =
            professionTopWeaponBaseIds.jsonObject.keys
                .sorted()
                .joinToString(separator = "; ") { professionId ->
                    val weaponBaseId = professionTopWeaponBaseIds.jsonObject.getValue(professionId).jsonPrimitive.content
                    val semanticTags =
                        professionTopWeaponSemanticTags.jsonObject[professionId]
                            ?.jsonArray
                            ?.joinToString { tag -> tag.jsonPrimitive.content }
                            ?: ""
                    "$professionId=$weaponBaseId[$semanticTags]"
                }
        val terrainEncounterRate = terrain.metrics.doubleValue("terrainInteractionEncounterRate")
        val taggedCombatCount = terrain.metrics.intValue("taggedCombatCount")
        val triggeredInteractionCombatCount = terrain.metrics.intValue("triggeredInteractionCombatCount")
        val terrainMetricDefinitionVersion = terrain.metrics.stringValue("terrainMetricDefinitionVersion")
        val perZoneEncounterLowerBoundTarget = terrain.metrics.doubleValue("perZoneEncounterLowerBoundTarget")
        val perZoneEncounterFailures = terrain.metrics.stringList("perZoneEncounterFailures")
        require(terrainMetricDefinitionVersion == terrainBaseline.metricDefinitionVersion) {
            "Terrain metric definition drifted: runtime=$terrainMetricDefinitionVersion baseline=${terrainBaseline.metricDefinitionVersion}."
        }
        val terrainEncounterBaseline = terrainBaseline.metric("terrainInteractionEncounterRate")
        val terrainEncounterRelativeIncrease = relativeIncrease(terrainEncounterRate, terrainEncounterBaseline.baselineRate)
        return listOf(
            Phase4ExperienceMetric(
                metricId = "scriptedHiddenVerificationRate",
                sourceTaskId = scriptedHidden.taskId,
                currentValue =
                    buildJsonObject {
                        put("rate", scriptedHiddenVerificationRate)
                        put("totalCases", scriptedHidden.metrics.getValue("totalCases"))
                        put("failureCount", scriptedHidden.metrics.getValue("failureCount"))
                        put("primerActionUsedCount", scriptedHidden.metrics.getValue("primerActionUsedCount"))
                        put("primerFreeCaseCount", scriptedHidden.metrics.getValue("primerFreeCaseCount"))
                        put("secretZoneDiscoveryRate", scriptedHidden.metrics.getValue("secretZoneDiscoveryRate"))
                    },
                currentValueText =
                    "${formatPercent(scriptedHiddenVerificationRate)} (${scriptedTotalCases - scriptedFailureCount}/$scriptedTotalCases)",
                target = Phase4OwnerMetricTargets.targetText("scriptedHiddenVerificationRate", scriptedHiddenRange),
                status = verdictOf(Phase4OwnerMetricTargets.passes(scriptedHiddenRange, scriptedHiddenVerificationRate)),
                note = "primerCases=${scriptedHidden.metrics.intValue("primerActionUsedCount")}, primerFreeCases=${scriptedHidden.metrics.intValue("primerFreeCaseCount")}",
            ),
            Phase4ExperienceMetric(
                metricId = "organicHiddenDiscoveryRate",
                sourceTaskId = organicHidden.taskId,
                currentValue =
                    buildJsonObject {
                        put("rate", organicHidden.metrics.getValue("organicHiddenDiscoveryRate"))
                        put("totalCases", organicHidden.metrics.getValue("totalCases"))
                        put("discoveryWithoutPrimerCount", organicHidden.metrics.getValue("discoveryWithoutPrimerCount"))
                        put("searchActionUseRate", organicHidden.metrics.getValue("searchActionUseRate"))
                        put("secretZoneEntryRate", organicHidden.metrics.getValue("secretZoneEntryRate"))
                        put("zones", organicHidden.metrics.getValue("zones"))
                    },
                currentValueText =
                    "${formatPercent(organicHiddenDiscoveryRate)} ($organicDiscoveryCount/$organicTotalCases), " +
                        "searchUse=${formatPercent(organicSearchActionUseRate)}, secretEntry=${formatPercent(organicSecretZoneEntryRate)}",
                target = Phase4OwnerMetricTargets.targetText("organicHiddenDiscoveryRate", organicHiddenRange),
                status = verdictOf(Phase4OwnerMetricTargets.passes(organicHiddenRange, organicHiddenDiscoveryRate)),
                note = "probeBot=${organicHidden.metrics.stringValue("probeBotId")}, scripted=false, observationOnly=true",
            ),
            Phase4ExperienceMetric(
                metricId = "sameZoneSecretVsCadenceMaxOverlap",
                sourceTaskId = loot.taskId,
                currentValue =
                    buildJsonObject {
                        put("maxOverlap", loot.metrics.getValue("sameZoneSecretVsCadenceMaxOverlap"))
                        put("pairs", loot.metrics.getValue("sameZoneSecretVsCadencePairs"))
                        put("localIdentityFailurePairs", loot.metrics.getValue("localIdentityFailurePairs"))
                    },
                currentValueText = formatRatio(sameZoneSecretVsCadenceMaxOverlap),
                target = Phase4OwnerMetricTargets.targetText("sameZoneSecretVsCadenceMaxOverlap", cadenceOverlapRange),
                status = verdictOf(Phase4OwnerMetricTargets.passes(cadenceOverlapRange, sameZoneSecretVsCadenceMaxOverlap)),
                note = "pairCount=${loot.metrics.getValue("sameZoneSecretVsCadencePairs").jsonArray.size}, overlap = |A ∩ B| / min(|A|, |B|)",
            ),
            Phase4ExperienceMetric(
                metricId = "sameZoneSecretVsRewardMaxOverlap",
                sourceTaskId = loot.taskId,
                currentValue =
                    buildJsonObject {
                        put("maxOverlap", loot.metrics.getValue("sameZoneSecretVsRewardMaxOverlap"))
                        put("pairs", loot.metrics.getValue("sameZoneSecretVsRewardPairs"))
                        put("localIdentityFailurePairs", loot.metrics.getValue("localIdentityFailurePairs"))
                    },
                currentValueText = formatRatio(sameZoneSecretVsRewardMaxOverlap),
                target = Phase4OwnerMetricTargets.targetText("sameZoneSecretVsRewardMaxOverlap", rewardOverlapRange),
                status = verdictOf(Phase4OwnerMetricTargets.passes(rewardOverlapRange, sameZoneSecretVsRewardMaxOverlap)),
                note = "pairCount=${loot.metrics.getValue("sameZoneSecretVsRewardPairs").jsonArray.size}, failurePairs=${loot.metrics.getValue("localIdentityFailurePairs").jsonArray.size}",
            ),
            Phase4ExperienceMetric(
                metricId = "terminalWeaponBaseDiversity",
                sourceTaskId = longRun.taskId,
                currentValue = longRun.metrics.getValue("terminalWeaponBaseDiversity"),
                currentValueText = terminalWeaponBaseDiversity.toString(),
                target = Phase4OwnerMetricTargets.targetText("terminalWeaponBaseDiversity", diversityRange),
                status = verdictOf(Phase4OwnerMetricTargets.passes(diversityRange, terminalWeaponBaseDiversity.toDouble())),
                note = "terminalBases=$terminalWeaponBaseNote; topWeaponSemantics=$professionTopWeaponSemanticNote",
            ),
            Phase4ExperienceMetric(
                metricId = "crossProfessionTopWeaponDominance",
                sourceTaskId = longRun.taskId,
                currentValue =
                    buildJsonObject {
                        put("rate", longRun.metrics.getValue("crossProfessionTopWeaponDominance"))
                        longRun.metrics["crossProfessionTopWeaponBaseId"]?.let { topWeaponBaseId -> put("topWeaponBaseId", topWeaponBaseId) }
                        put("topWeaponCount", longRun.metrics.getValue("crossProfessionTopWeaponCount"))
                        put("fullRouteCount", longRun.metrics.getValue("fullRouteCount"))
                    },
                currentValueText =
                    "${formatPercent(crossProfessionTopWeaponDominance)} " +
                        "(${longRun.metrics.intValue("crossProfessionTopWeaponCount")}/$fullRouteCount) " +
                        "top=${longRun.metrics["crossProfessionTopWeaponBaseId"]?.jsonPrimitive?.content ?: "unknown"}",
                target = Phase4OwnerMetricTargets.targetText("crossProfessionTopWeaponDominance", dominanceRange),
                status = verdictOf(Phase4OwnerMetricTargets.passes(dominanceRange, crossProfessionTopWeaponDominance)),
                note = "topWeaponBaseId=${longRun.metrics["crossProfessionTopWeaponBaseId"]?.jsonPrimitive?.content ?: "unknown"}",
            ),
            Phase4ExperienceMetric(
                metricId = "professionAlignedWeaponAdoptionRate",
                sourceTaskId = longRun.taskId,
                currentValue =
                    buildJsonObject {
                        put("rate", longRun.metrics.getValue("professionAlignedWeaponAdoptionRate"))
                        put("alignedFullRouteSampleCount", longRun.metrics.getValue("alignedFullRouteSampleCount"))
                        put("fullRouteCount", longRun.metrics.getValue("fullRouteCount"))
                        put("professionTerminalWeaponDistribution", professionTerminalWeaponDistribution)
                        put("professionTopWeaponBaseIds", professionTopWeaponBaseIds)
                        put("professionTopWeaponSemanticTags", professionTopWeaponSemanticTags)
                    },
                currentValueText = "${formatPercent(professionAlignedWeaponAdoptionRate)} ($alignedFullRouteSampleCount/$fullRouteCount)",
                target = Phase4OwnerMetricTargets.targetText("professionAlignedWeaponAdoptionRate", adoptionRange),
                status = verdictOf(Phase4OwnerMetricTargets.passes(adoptionRange, professionAlignedWeaponAdoptionRate)),
                note = "alignedSamples=$alignedFullRouteSampleCount/$fullRouteCount; topWeaponSemantics=$professionTopWeaponSemanticNote",
            ),
            Phase4ExperienceMetric(
                metricId = "terrainInteractionEncounterRate.aggregate",
                sourceTaskId = terrain.taskId,
                currentValue =
                    buildJsonObject {
                        put("rate", terrain.metrics.getValue("terrainInteractionEncounterRate"))
                        put("taggedCombatCount", terrain.metrics.getValue("taggedCombatCount"))
                        put("triggeredInteractionCombatCount", terrain.metrics.getValue("triggeredInteractionCombatCount"))
                        put("baseline", terrainEncounterBaseline.toJson())
                        put("relativeIncrease", terrainEncounterRelativeIncrease)
                        put("targetRate", terrainEncounterBaseline.targetRate)
                        put("decisionPathByCurrentMetrics", terrain.metrics.getValue("decisionPathByCurrentMetrics"))
                        put("combatSampledZoneIds", terrain.metrics.getValue("combatSampledZoneIds"))
                        put("combatSampledZoneExclusionNotes", terrain.metrics.getValue("combatSampledZoneExclusionNotes"))
                        put("terrainCoverageByZone", terrain.metrics.getValue("terrainCoverageByZone"))
                    },
                currentValueText =
                    "${formatPercent(terrainEncounterRate)} ($triggeredInteractionCombatCount/$taggedCombatCount), " +
                        "delta=${formatSignedPercent(terrainEncounterRelativeIncrease)} vs baseline ${formatPercent(terrainEncounterBaseline.baselineRate)}",
                target = Phase4OwnerMetricTargets.targetText("terrainInteractionEncounterRate.aggregate", terrainAggregateRange),
                status = verdictOf(Phase4OwnerMetricTargets.passes(terrainAggregateRange, terrainEncounterRate)),
                note =
                    "baseline=${terrainBaseline.baselineId} @ ${terrainBaseline.sourceArtifactPath} " +
                        "(buildId=${terrainBaseline.sourceBuildId ?: "unknown"}, generatedAt=${terrainBaseline.sourceGeneratedAt ?: "unknown"}, metricDefinitionVersion=$terrainMetricDefinitionVersion); " +
                        "decisionPathByCurrentMetrics=${terrain.metrics.stringValue("decisionPathByCurrentMetrics")}",
            ),
            Phase4ExperienceMetric(
                metricId = "terrainInteractionEncounterRate.per_zone_lower_bound",
                sourceTaskId = terrain.taskId,
                currentValue =
                    buildJsonObject {
                        put("target", terrain.metrics.getValue("perZoneEncounterLowerBoundTarget"))
                        put("failureZones", terrain.metrics.getValue("perZoneEncounterFailures"))
                        put("combatSampledZoneIds", terrain.metrics.getValue("combatSampledZoneIds"))
                        put("combatSampledZoneExclusionNotes", terrain.metrics.getValue("combatSampledZoneExclusionNotes"))
                        put("terrainCoverageByZone", terrain.metrics.getValue("terrainCoverageByZone"))
                    },
                currentValueText =
                    if (perZoneEncounterFailures.isEmpty()) {
                        "all sampled zones >= ${formatPercent(perZoneEncounterLowerBoundTarget)}"
                    } else {
                        "failed=${perZoneEncounterFailures.joinToString()} target=${formatPercent(perZoneEncounterLowerBoundTarget)}"
                    },
                target = Phase4OwnerMetricTargets.targetText("terrainInteractionEncounterRate.per_zone_lower_bound", terrainPerZoneRange),
                status = verdictOf(Phase4OwnerMetricTargets.passes(terrainPerZoneRange, perZoneEncounterFailures.size.toDouble())),
                note =
                    "combatSampledZoneIds=${terrain.metrics.stringList("combatSampledZoneIds").joinToString()}, " +
                        "exclusions=${terrain.metrics.getValue("combatSampledZoneExclusionNotes").jsonArray.joinToString { note -> note.jsonPrimitive.content }}",
            ),
        )
    }

    private fun renderMarkdown(report: Phase4AggregateReport): String =
        buildString {
            val metricsById = report.experienceMetrics.associateBy(Phase4ExperienceMetric::metricId)
            val tasksById = report.tasks.associateBy(Phase4TaskAggregate::taskId)
            val lootTask = requireTask(tasksById, "whiteBoxLoot")
            val longRunTask = requireTask(tasksById, "longRunLab")
            val professionTerminalWeaponDistribution =
                json.encodeToString(
                    JsonObject.serializer(),
                    buildJsonObject {
                        put("professionTerminalWeaponDistribution", longRunTask.metrics.getValue("professionTerminalWeaponDistribution"))
                    },
                )
            val scriptedHiddenTask = requireTask(tasksById, "hiddenContentHarness")
            val organicHiddenTask = requireTask(tasksById, "organicHiddenProbe")
            val terrainTask = requireTask(tasksById, "terrainInteractionBatch")

            appendLine("# Phase 4 Report")
            appendLine()
            appendLine("- generatedAt: `${report.generatedAt}`")
            report.buildId?.let { buildId -> appendLine("- buildId: `${buildId}`") }
            report.locale?.let { locale -> appendLine("- locale: `${locale}`") }
            appendLine("- taskCount: `${report.taskCount}`")
            appendLine("- passedTaskCount: `${report.passedTaskCount}`")
            appendLine("- failedTaskCount: `${report.failedTaskCount}`")
            appendLine("- failedExperienceMetricCount: `${report.failedExperienceMetricCount}`")
            appendLine("- failedGateCount: `${report.failedGateCount}`")
            appendLine()
            appendLine("## 指标 Owner 表")
            appendLine("| metricId | owner | sourcePath | formula | target | fail semantics |")
            appendLine("| --- | --- | --- | --- | --- | --- |")
            report.metricCatalog.forEach { entry ->
                appendLine(
                    "| `${entry.id}` | `${entry.ownerTaskId}` | `${entry.sourcePath}` | `${entry.formula}` | `${entry.targetText}` | ${entry.failSemantics} |",
                )
            }
            appendLine()
            appendLine("## Owner Metrics")
            appendLine("| metricId | current | source | target | status |")
            appendLine("| --- | --- | --- | --- | --- |")
            report.experienceMetrics.forEach { metric ->
                appendLine("| `${metric.metricId}` | ${metric.currentValueText} | `${metric.sourceTaskId}` | `${metric.target}` | ${metric.status} |")
            }
            val notedMetrics = report.experienceMetrics.filter { metric -> metric.note != null }
            if (notedMetrics.isNotEmpty()) {
                appendLine()
                notedMetrics.forEach { metric ->
                    appendLine("- `${metric.metricId}` note: ${metric.note}")
                }
            }
            appendLine()
            appendLine("## Local Reward Identity")
            appendLine("- sourceTask: `${lootTask.taskId}`")
            appendLine("- `sameZoneSecretVsCadenceMaxOverlap`: ${metricsById.getValue("sameZoneSecretVsCadenceMaxOverlap").currentValueText} / ${metricsById.getValue("sameZoneSecretVsCadenceMaxOverlap").status}")
            appendLine("- `sameZoneSecretVsRewardMaxOverlap`: ${metricsById.getValue("sameZoneSecretVsRewardMaxOverlap").currentValueText} / ${metricsById.getValue("sameZoneSecretVsRewardMaxOverlap").status}")
            appendLine("- `localIdentityFailurePairs`: ${lootTask.metrics.getValue("localIdentityFailurePairs").jsonArray.joinToString { pair -> pair.jsonPrimitive.content }.ifBlank { "none" }}")
            appendLine()
            appendLine("## Terminal Build Identity")
            appendLine("- sourceTask: `${longRunTask.taskId}`")
            appendLine("- `terminalWeaponBaseDiversity`: ${metricsById.getValue("terminalWeaponBaseDiversity").currentValueText} / ${metricsById.getValue("terminalWeaponBaseDiversity").status}")
            appendLine("- `crossProfessionTopWeaponDominance`: ${metricsById.getValue("crossProfessionTopWeaponDominance").currentValueText} / ${metricsById.getValue("crossProfessionTopWeaponDominance").status}")
            appendLine("- `professionAlignedWeaponAdoptionRate`: ${metricsById.getValue("professionAlignedWeaponAdoptionRate").currentValueText} / ${metricsById.getValue("professionAlignedWeaponAdoptionRate").status}")
            val professionTopWeaponSemanticTags = longRunTask.metrics.getValue("professionTopWeaponSemanticTags")
            appendLine("```json")
            appendLine(professionTerminalWeaponDistribution)
            appendLine("```")
            appendLine("```json")
            appendLine(professionTopWeaponSemanticTags.toString())
            appendLine("```")
            appendLine()
            appendLine("## Scripted vs Organic Hidden")
            appendLine("- sourceTask.scripted: `${scriptedHiddenTask.taskId}`")
            appendLine("- sourceTask.organic: `${organicHiddenTask.taskId}`")
            appendLine("- `scriptedHiddenVerificationRate`: ${metricsById.getValue("scriptedHiddenVerificationRate").currentValueText} / ${metricsById.getValue("scriptedHiddenVerificationRate").status}")
            appendLine("- `organicHiddenDiscoveryRate`: ${metricsById.getValue("organicHiddenDiscoveryRate").currentValueText} / ${metricsById.getValue("organicHiddenDiscoveryRate").status}")
            appendLine("- scripted primer cases: `${scriptedHiddenTask.metrics.intValue("primerActionUsedCount")}`")
            appendLine("- organic search use rate: `${formatPercent(organicHiddenTask.metrics.doubleValue("searchActionUseRate"))}`")
            appendLine("- organic secret zone entry rate: `${formatPercent(organicHiddenTask.metrics.doubleValue("secretZoneEntryRate"))}`")
            appendLine()
            appendLine("## Terrain Combat Sample Contract")
            appendLine("- sourceTask: `${terrainTask.taskId}`")
            appendLine("- `terrainInteractionEncounterRate.aggregate`: ${metricsById.getValue("terrainInteractionEncounterRate.aggregate").currentValueText} / ${metricsById.getValue("terrainInteractionEncounterRate.aggregate").status}")
            appendLine("- `terrainInteractionEncounterRate.per_zone_lower_bound`: ${metricsById.getValue("terrainInteractionEncounterRate.per_zone_lower_bound").currentValueText} / ${metricsById.getValue("terrainInteractionEncounterRate.per_zone_lower_bound").status}")
            appendLine("- combatSampledZoneIds: `${terrainTask.metrics.stringList("combatSampledZoneIds").joinToString()}`")
            appendLine("- combatSampledZoneExclusionNotes:")
            terrainTask.metrics.getValue("combatSampledZoneExclusionNotes").jsonArray.forEach { note ->
                appendLine("  - ${note.jsonPrimitive.content}")
            }
            appendLine("- perZoneEncounterFailures: `${terrainTask.metrics.stringList("perZoneEncounterFailures").joinToString().ifBlank { "none" }}`")
            appendLine()
            appendLine("## Tasks")
            report.tasks.forEach { task ->
                appendLine("### `${task.taskId}` - ${task.status}")
                appendLine("- sourcePath: `${task.sourcePath}`")
                task.metrics["legacyLootProfileSchemaRejectSummaries"]
                    ?.jsonArray
                    ?.takeIf { summaries -> summaries.isNotEmpty() }
                    ?.let { summaries ->
                        appendLine("- legacyLootProfileSchemaRejectSummaries:")
                        summaries.forEach { summary ->
                            val payload = summary.jsonObject
                            appendLine(
                                "  - `${payload.stringValue("packId")}` -> `${payload.stringValue("targetProfileId")}`: " +
                                    "${payload.stringValue("diagnosticCode")} " +
                                    "(actual=${payload.stringValue("actualSchemaVersion")}, expected=${payload.stringValue("expectedSchemaVersion")})",
                            )
                        }
                    }
                appendLine("```json")
                appendLine(json.encodeToString(JsonObject.serializer(), task.metrics))
                appendLine("```")
            }
        }

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4")
        } else {
            Path.of(configured)
        }
    }

    private fun repoRoot(): Path {
        val configured = System.getProperty("ktome.repo.root")
        return if (configured.isNullOrBlank()) Path.of(".").toAbsolutePath().normalize() else Path.of(configured).toAbsolutePath().normalize()
    }

    private fun relativize(
        repoRoot: Path,
        path: Path,
    ): String = repoRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/')
}

private fun Phase4AggregateReport.toJson(): JsonObject =
    buildJsonObject {
        put("phaseId", phaseId)
        put("generatedAt", generatedAt)
        buildId?.let { value -> put("buildId", value) }
        locale?.let { value -> put("locale", value) }
        put("taskCount", taskCount)
        put("passedTaskCount", passedTaskCount)
        put("failedTaskCount", failedTaskCount)
        put("failedExperienceMetricCount", failedExperienceMetricCount)
        put("failedGateCount", failedGateCount)
        putJsonArray("tasks") {
            tasks.forEach { task -> add(task.toJson()) }
        }
        putJsonArray("metricCatalog") {
            metricCatalog.forEach { entry -> add(entry.toJson()) }
        }
        putJsonArray("experienceMetrics") {
            experienceMetrics.forEach { metric -> add(metric.toJson()) }
        }
    }

private fun Phase4TaskAggregate.toJson(): JsonObject =
    buildJsonObject {
        put("taskId", taskId)
        put("status", status)
        put("sourcePath", sourcePath)
        buildId?.let { value -> put("buildId", value) }
        locale?.let { value -> put("locale", value) }
        putJsonObject("metrics") {
            metrics.forEach { (key, value) -> put(key, value) }
        }
    }

private fun Phase4ExperienceMetric.toJson(): JsonObject =
    buildJsonObject {
        put("metricId", metricId)
        put("sourceTaskId", sourceTaskId)
        put("currentValue", currentValue)
        put("currentValueText", currentValueText)
        put("target", target)
        put("status", status)
        note?.let { value -> put("note", value) }
    }

private fun readTerrainBaseline(repoRoot: Path): TerrainMetricBaseline {
    val baselinePath = repoRoot.resolve(TERRAIN_BASELINE_RELATIVE_PATH)
    val payload = readPhase4Json(baselinePath)
    val metricsById =
        payload.getValue("metrics").jsonArray.associate { element ->
            val metric = element.jsonObject
            val metricId = metric.stringValue("metricId")
            metricId to
                TerrainMetricBaselineMetric(
                    metricId = metricId,
                    baselineRate = metric.doubleValue("baselineRate"),
                    targetRelativeIncrease = metric.doubleValue("targetRelativeIncrease"),
                    numerator = metric.intValue("numerator"),
                    denominator = metric.intValue("denominator"),
                    numeratorLabel = metric.stringValue("numeratorLabel"),
                    denominatorLabel = metric.stringValue("denominatorLabel"),
                    sourceValueText = metric.stringValue("sourceValueText"),
                    normalizedFormula = metric.stringValue("normalizedFormula"),
                )
        }
    return TerrainMetricBaseline(
        baselineId = payload.stringValue("baselineId"),
        metricDefinitionVersion = payload.stringValue("metricDefinitionVersion"),
        sourceArtifactPath = payload.stringValue("sourceArtifactPath"),
        sourceBuildId = payload.stringValue("sourceBuildId"),
        sourceGeneratedAt = payload.stringValue("sourceGeneratedAt"),
        metricsById = metricsById,
    )
}

private fun TerrainMetricBaseline.metric(metricId: String): TerrainMetricBaselineMetric =
    checkNotNull(metricsById[metricId]) { "Missing terrain baseline metric '$metricId' in $baselineId." }

private fun TerrainMetricBaselineMetric.toJson(): JsonObject =
    buildJsonObject {
        put("metricId", metricId)
        put("baselineRate", baselineRate)
        put("targetRelativeIncrease", targetRelativeIncrease)
        put("targetRate", targetRate)
        put("numerator", numerator)
        put("denominator", denominator)
        put("numeratorLabel", numeratorLabel)
        put("denominatorLabel", denominatorLabel)
        put("sourceValueText", sourceValueText)
        put("normalizedFormula", normalizedFormula)
    }

private fun relativeIncrease(
    currentValue: Double,
    baselineValue: Double,
): Double =
    if (baselineValue == 0.0) {
        0.0
    } else {
        (currentValue - baselineValue) / baselineValue
    }

private fun aggregateMetrics(
    payload: JsonObject,
    groupId: String,
): JsonObject =
    payload.getValue("aggregates").jsonArray
        .first { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == groupId }
        .jsonObject
        .getValue("metrics")
        .jsonObject

private fun requireTask(
    tasksById: Map<String, Phase4TaskAggregate>,
    taskId: String,
): Phase4TaskAggregate = checkNotNull(tasksById[taskId]) { "Missing phase4 task aggregate for $taskId." }

private fun requireFreshnessAligned(
    primaryPath: Path,
    primaryPayload: JsonObject,
    secondaryPath: Path,
    secondaryPayload: JsonObject,
    maxSkew: Duration,
) {
    val primaryBuildId = reportBuildId(primaryPayload)
    val secondaryBuildId = reportBuildId(secondaryPayload)
    check(primaryBuildId == secondaryBuildId) {
        "Mismatched content-pack artifact buildIds: $primaryPath ($primaryBuildId) vs $secondaryPath ($secondaryBuildId)."
    }
    val primaryTimestamp = reportTimestamp(primaryPayload)
    val secondaryTimestamp = reportTimestamp(secondaryPayload)
    val skew = Duration.between(primaryTimestamp, secondaryTimestamp).abs()
    check(skew <= maxSkew) {
        "Stale content-pack artifacts: $primaryPath ($primaryBuildId @ $primaryTimestamp) vs $secondaryPath ($secondaryBuildId @ $secondaryTimestamp), skew=${skew.toMinutes()}m exceeds ${maxSkew.toMinutes()}m."
    }
}

internal fun countFailedStatuses(statuses: Iterable<String>): Int = statuses.count { status -> status == "FAIL" }

private fun verdictOf(passed: Boolean): String = if (passed) "PASS" else "FAIL"

private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value * 100.0)

private fun formatPercentPrecise(value: Double): String = String.format(Locale.US, "%.2f%%", value * 100.0)

private fun formatSignedPercent(value: Double): String =
    if (value >= 0.0) {
        "+${formatPercentPrecise(value)}"
    } else {
        "-${formatPercentPrecise(kotlin.math.abs(value))}"
    }

private fun formatRatio(value: Double): String = String.format(Locale.US, "%.3f", value)

private fun readPhase4Json(path: Path): JsonObject {
    check(Files.exists(path)) { "Missing phase4 report source: $path" }
    return phase4Json.parseToJsonElement(Files.readString(path)).jsonObject
}

private fun reportBuildId(payload: JsonObject): String = payload.getValue("header").jsonObject.stringValue("buildId")

private fun reportTimestamp(payload: JsonObject): Instant = Instant.parse(payload.getValue("header").jsonObject.stringValue("timestamp"))

private fun JsonObject.intValue(key: String): Int = getValue(key).jsonPrimitive.content.toInt()

private fun JsonObject.doubleValue(key: String): Double = getValue(key).jsonPrimitive.content.toDouble()

private fun JsonObject.stringValue(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.stringList(key: String): List<String> = getValue(key).jsonArray.map { element -> element.jsonPrimitive.content }
