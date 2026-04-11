package com.ktome.tools.phase4

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
    val experienceMetrics: List<Phase4ExperienceMetric>,
)

private data class Phase4TaskAggregate(
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
                relativeSourcePath = "build/reports/harness/boss-harness.json",
                reader = ::readBossHarness,
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

        val taskReports = taskDescriptors.map { descriptor -> descriptor.read(repoRoot) }
        val experienceMetrics = buildExperienceMetrics(repoRoot = repoRoot, tasks = taskReports)
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
        val boss = requireTask(tasksById, "bossHarness")
        val loot = requireTask(tasksById, "whiteBoxLoot")
        val hidden = requireTask(tasksById, "whiteBoxHiddenContent")
        val terrain = requireTask(tasksById, "terrainInteractionBatch")
        val terrainBaseline = readTerrainBaseline(repoRoot)

        val lootAverageOverlap = loot.metrics.doubleValue("lootProfileAverageBaseItemOverlap")
        val lootMaxOverlap = loot.metrics.doubleValue("lootProfileMaxBaseItemOverlap")
        val lootDistinctBaseItemCount = loot.metrics.intValue("lootProfileDistinctBaseItemCount")
        val affixCount = loot.metrics.intValue("affixCount")
        val uniqueTemplateCount = loot.metrics.intValue("uniqueTemplateCount")
        val artifactTemplateCount = loot.metrics.intValue("artifactTemplateCount")
        val totalCount = loot.metrics.intValue("totalCount")
        val affixCoverage = loot.metrics.doubleValue("affixPassiveCoverage")
        val affixPassiveKinds = loot.metrics.stringList("affixPassiveKinds")
        val hiddenEventRegistryCount = hidden.metrics.intValue("hiddenEventRegistryCount")
        val hiddenTriggerCoverage = hidden.metrics.doubleValue("hiddenTriggerTypeCoverage")
        val hiddenTriggerTypes = hidden.metrics.stringList("hiddenTriggerTypeSet")
        val secretBindingCoverage = hidden.metrics.intValue("secretEntranceBindingCoverage")
        val secretBindingSet = hidden.metrics.stringList("secretEntranceBindingSet")
        val terrainTaggedExposureRate = terrain.metrics.doubleValue("terrainTaggedCombatExposureRate")
        val terrainEncounterRate = terrain.metrics.doubleValue("terrainInteractionEncounterRate")
        val combatCount = terrain.metrics.intValue("combatCount")
        val taggedCombatCount = terrain.metrics.intValue("taggedCombatCount")
        val triggeredInteractionCombatCount = terrain.metrics.intValue("triggeredInteractionCombatCount")
        val terrainMetricDefinitionVersion = terrain.metrics.stringValue("terrainMetricDefinitionVersion")
        require(terrainMetricDefinitionVersion == terrainBaseline.metricDefinitionVersion) {
            "Terrain metric definition drifted: runtime=$terrainMetricDefinitionVersion baseline=${terrainBaseline.metricDefinitionVersion}."
        }
        val terrainTaggedExposureBaseline = terrainBaseline.metric("terrainTaggedCombatExposureRate")
        val terrainEncounterBaseline = terrainBaseline.metric("terrainInteractionEncounterRate")
        val terrainTaggedExposureRelativeIncrease = relativeIncrease(terrainTaggedExposureRate, terrainTaggedExposureBaseline.baselineRate)
        val terrainEncounterRelativeIncrease = relativeIncrease(terrainEncounterRate, terrainEncounterBaseline.baselineRate)
        val uniqueArtifactMeaningfulSwapRate = loot.metrics.doubleValue("uniqueArtifactMeaningfulSwapRate")
        val uniqueArtifactOutcomeCount = loot.metrics.intValue("uniqueArtifactOutcomeCount")
        val meaningfulUniqueArtifactSwapCount = loot.metrics.intValue("meaningfulUniqueArtifactSwapCount")
        return listOf(
            Phase4ExperienceMetric(
                metricId = "eliteMutationDistinctCount",
                sourceTaskId = boss.taskId,
                currentValue = boss.metrics.getValue("eliteMutationDistinctCount"),
                currentValueText = boss.metrics.intValue("eliteMutationDistinctCount").toString(),
                target = ">= 12",
                status = verdictOf(boss.metrics.intValue("eliteMutationDistinctCount") >= 12),
            ),
            Phase4ExperienceMetric(
                metricId = "eliteMutationValidPairCount",
                sourceTaskId = boss.taskId,
                currentValue = boss.metrics.getValue("eliteMutationValidPairCount"),
                currentValueText = boss.metrics.intValue("eliteMutationValidPairCount").toString(),
                target = ">= 40",
                status = verdictOf(boss.metrics.intValue("eliteMutationValidPairCount") >= 40),
            ),
            Phase4ExperienceMetric(
                metricId = "lootProfileBaseItemOverlapMatrix",
                sourceTaskId = loot.taskId,
                currentValue =
                    buildJsonObject {
                        put("averageOverlap", loot.metrics.getValue("lootProfileAverageBaseItemOverlap"))
                        put("maxOverlap", loot.metrics.getValue("lootProfileMaxBaseItemOverlap"))
                        put("matrix", loot.metrics.getValue("lootProfileBaseItemOverlapMatrix"))
                    },
                currentValueText = "average=${formatRatio(lootAverageOverlap)}, max=${formatRatio(lootMaxOverlap)}",
                target = "averageOverlap < 0.30",
                status = verdictOf(lootAverageOverlap < 0.30),
                note = "overlap = |A ∩ B| / min(|A|, |B|)",
            ),
            Phase4ExperienceMetric(
                metricId = "lootProfileMaxBaseItemOverlap",
                sourceTaskId = loot.taskId,
                currentValue = loot.metrics.getValue("lootProfileMaxBaseItemOverlap"),
                currentValueText = formatRatio(lootMaxOverlap),
                target = "< 0.95 (sanity)",
                status = verdictOf(lootMaxOverlap < 0.95),
                note = "soft sanity guard for profile identity; does not affect task verdict",
            ),
            Phase4ExperienceMetric(
                metricId = "lootProfileDistinctBaseItemCount",
                sourceTaskId = loot.taskId,
                currentValue = loot.metrics.getValue("lootProfileDistinctBaseItemCount"),
                currentValueText = lootDistinctBaseItemCount.toString(),
                target = ">= 35",
                status = verdictOf(lootDistinctBaseItemCount >= 35),
            ),
            Phase4ExperienceMetric(
                metricId = "affixCount",
                sourceTaskId = loot.taskId,
                currentValue = loot.metrics.getValue("affixCount"),
                currentValueText = affixCount.toString(),
                target = ">= 75",
                status = verdictOf(affixCount >= 75),
            ),
            Phase4ExperienceMetric(
                metricId = "uniqueTemplateCount",
                sourceTaskId = loot.taskId,
                currentValue = loot.metrics.getValue("uniqueTemplateCount"),
                currentValueText = uniqueTemplateCount.toString(),
                target = ">= 20",
                status = verdictOf(uniqueTemplateCount >= 20),
            ),
            Phase4ExperienceMetric(
                metricId = "artifactTemplateCount",
                sourceTaskId = loot.taskId,
                currentValue = loot.metrics.getValue("artifactTemplateCount"),
                currentValueText = artifactTemplateCount.toString(),
                target = ">= 8",
                status = verdictOf(artifactTemplateCount >= 8),
            ),
            Phase4ExperienceMetric(
                metricId = "totalLootContentCount",
                sourceTaskId = loot.taskId,
                currentValue = loot.metrics.getValue("totalCount"),
                currentValueText = totalCount.toString(),
                target = ">= 103",
                status = verdictOf(totalCount >= 103),
            ),
            Phase4ExperienceMetric(
                metricId = "affixPassiveCoverage",
                sourceTaskId = loot.taskId,
                currentValue =
                    buildJsonObject {
                        put("coverageRatio", loot.metrics.getValue("affixPassiveCoverage"))
                        put("passiveKinds", loot.metrics.getValue("affixPassiveKinds"))
                    },
                currentValueText = "${formatPercent(affixCoverage)} (${affixPassiveKinds.joinToString()})",
                target = ">= 80%",
                status = verdictOf(affixCoverage >= 0.80),
            ),
            Phase4ExperienceMetric(
                metricId = "hiddenEventCount",
                sourceTaskId = hidden.taskId,
                currentValue = hidden.metrics.getValue("hiddenEventRegistryCount"),
                currentValueText = hiddenEventRegistryCount.toString(),
                target = ">= 12",
                status = verdictOf(hiddenEventRegistryCount >= 12),
            ),
            Phase4ExperienceMetric(
                metricId = "hiddenTriggerTypeCoverage",
                sourceTaskId = hidden.taskId,
                currentValue =
                    buildJsonObject {
                        put("coverageRatio", hidden.metrics.getValue("hiddenTriggerTypeCoverage"))
                        put("triggerTypes", hidden.metrics.getValue("hiddenTriggerTypeSet"))
                    },
                currentValueText = "${formatPercent(hiddenTriggerCoverage)} (${hiddenTriggerTypes.joinToString()})",
                target = ">= 4/6",
                status = verdictOf(hiddenTriggerCoverage >= (4.0 / 6.0)),
            ),
            Phase4ExperienceMetric(
                metricId = "secretEntranceBindingCoverage",
                sourceTaskId = hidden.taskId,
                currentValue =
                    buildJsonObject {
                        put("bindingCount", hidden.metrics.getValue("secretEntranceBindingCoverage"))
                        put("bindingSet", hidden.metrics.getValue("secretEntranceBindingSet"))
                    },
                currentValueText = "$secretBindingCoverage (${secretBindingSet.joinToString()})",
                target = ">= 3",
                status = verdictOf(secretBindingCoverage >= 3),
            ),
            Phase4ExperienceMetric(
                metricId = "terrainTaggedCombatExposureRate",
                sourceTaskId = terrain.taskId,
                currentValue =
                    buildJsonObject {
                        put("rate", terrain.metrics.getValue("terrainTaggedCombatExposureRate"))
                        put("combatCount", terrain.metrics.getValue("combatCount"))
                        put("taggedCombatCount", terrain.metrics.getValue("taggedCombatCount"))
                        put("baseline", terrainTaggedExposureBaseline.toJson())
                        put("relativeIncrease", terrainTaggedExposureRelativeIncrease)
                        put("targetRate", terrainTaggedExposureBaseline.targetRate)
                        put("decisionPathByCurrentMetrics", terrain.metrics.getValue("decisionPathByCurrentMetrics"))
                        put("terrainCoverageByZone", terrain.metrics.getValue("terrainCoverageByZone"))
                    },
                currentValueText =
                    "${formatPercent(terrainTaggedExposureRate)} ($taggedCombatCount/$combatCount), " +
                        "delta=${formatSignedPercent(terrainTaggedExposureRelativeIncrease)} vs baseline ${formatPercent(terrainTaggedExposureBaseline.baselineRate)}",
                target =
                    ">= ${formatPercentPrecise(terrainTaggedExposureBaseline.targetRate)} " +
                        "(baseline ${formatPercentPrecise(terrainTaggedExposureBaseline.baselineRate)} +${formatPercentPrecise(terrainTaggedExposureBaseline.targetRelativeIncrease)})",
                status = verdictOf(terrainTaggedExposureRate >= terrainTaggedExposureBaseline.targetRate),
                note =
                    "baseline=${terrainBaseline.baselineId} @ ${terrainBaseline.sourceArtifactPath} " +
                        "(buildId=${terrainBaseline.sourceBuildId ?: "unknown"}, generatedAt=${terrainBaseline.sourceGeneratedAt ?: "unknown"}, metricDefinitionVersion=$terrainMetricDefinitionVersion); " +
                        "decisionPathByCurrentMetrics=${terrain.metrics.stringValue("decisionPathByCurrentMetrics")}",
            ),
            Phase4ExperienceMetric(
                metricId = "terrainInteractionEncounterRate",
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
                        put("terrainCoverageByZone", terrain.metrics.getValue("terrainCoverageByZone"))
                    },
                currentValueText =
                    "${formatPercent(terrainEncounterRate)} ($triggeredInteractionCombatCount/$taggedCombatCount), " +
                        "delta=${formatSignedPercent(terrainEncounterRelativeIncrease)} vs baseline ${formatPercent(terrainEncounterBaseline.baselineRate)}",
                target =
                    ">= ${formatPercentPrecise(terrainEncounterBaseline.targetRate)} " +
                        "(baseline ${formatPercentPrecise(terrainEncounterBaseline.baselineRate)} +${formatPercentPrecise(terrainEncounterBaseline.targetRelativeIncrease)})",
                status = verdictOf(terrainEncounterRate >= terrainEncounterBaseline.targetRate),
                note =
                    "encounterRate = triggeredInteractionCombatCount / taggedCombatCount; " +
                        "baseline=${terrainBaseline.baselineId} normalized by ${terrainEncounterBaseline.normalizedFormula} " +
                        "from ${terrainBaseline.sourceArtifactPath}",
            ),
            Phase4ExperienceMetric(
                metricId = "uniqueArtifactMeaningfulSwapRate",
                sourceTaskId = loot.taskId,
                currentValue =
                    buildJsonObject {
                        put("rate", loot.metrics.getValue("uniqueArtifactMeaningfulSwapRate"))
                        put("uniqueArtifactOutcomeCount", loot.metrics.getValue("uniqueArtifactOutcomeCount"))
                        put("meaningfulUniqueArtifactSwapCount", loot.metrics.getValue("meaningfulUniqueArtifactSwapCount"))
                    },
                currentValueText = "${formatPercent(uniqueArtifactMeaningfulSwapRate)} ($meaningfulUniqueArtifactSwapCount/$uniqueArtifactOutcomeCount)",
                target = ">= 50%",
                status = verdictOf(uniqueArtifactMeaningfulSwapRate >= 0.50),
                note = "meaningful = passive signature set is non-empty and not fully covered by the same-slot rare passive universe",
            ),
        )
    }

    private fun renderMarkdown(report: Phase4AggregateReport): String =
        buildString {
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
            appendLine("## 体验度量基线")
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
