package com.ktome.tools.phase4

import com.ktome.tools.loot.formatStrictAwareLocalIdentityCurrentValue
import com.ktome.tools.loot.localIdentityEvaluationDetail
import com.ktome.tools.loot.splitByLocalIdentityPairType
import com.ktome.tools.loot.toLootStrictLocalIdentityViolation
import com.ktome.tools.verification.EvaluationEntry
import com.ktome.tools.verification.EvaluationEntryStatus
import com.ktome.tools.verification.VerificationBaseline
import java.nio.file.Files
import java.nio.file.Path
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
    val metricCatalog: List<Phase4MetricCatalogEntry>,
    val experienceMetrics: List<Phase4ExperienceMetric>,
)

private data class Phase4ExperienceMetric(
    val metricId: String,
    val sourceTaskId: String,
    val currentValue: JsonElement,
    val currentValueText: String,
    val target: String,
    val status: String,
    val note: String? = null,
    val details: JsonObject = JsonObject(emptyMap()),
)

@Deprecated(
    message = "Legacy Phase 4 aggregate kept only for parity/fallback. Canonical authority lives in ReportPhase4Runner.",
)
object Phase4ReportRunner {
    private const val SUMMARY_FILE: String = "phase4-summary.json"
    private const val MARKDOWN_FILE: String = "phase4-summary.md"
    private val json: Json = Json { prettyPrint = true }

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
        Phase4DomainArtifactRegistry.collectTaskAggregates(repoRoot)

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
        val scriptedHiddenBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.scriptedHiddenBaselinePath()))
        val organicHiddenBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.organicHiddenBaselinePath()))
        val lootBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.lootBaselinePath()))
        val terminalBuildBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.terminalBuildBaselinePath()))
        val criticalPathPacingBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.criticalPathPacingBaselinePath()))
        val terrainUnifiedBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.terrainUnifiedBaselinePath()))
        val terrainPerZoneBaseline = VerificationBaseline.read(repoRoot.resolve(Phase4OwnerBaselineRegistry.terrainPerZoneBaselinePath()))
        val terrainBaseline = readTerrainBaseline(repoRoot)
        val scriptedHiddenRange = scriptedHiddenBaseline.requiredMetric("scriptedHiddenVerificationRate")
        val leadDiscoveryRange = organicHiddenBaseline.requiredMetric("leadDiscoveryRate")
        val secretConversionRange = organicHiddenBaseline.requiredMetric("secretConversionRate")
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
        val organicDiscoveryCount = organicHidden.metrics.intValue("leadDiscoveryCount")
        val leadDiscoveryRate = organicHidden.metrics.doubleValue("leadDiscoveryRate")
        val secretConversionCount = organicHidden.metrics.intValue("secretConversionCount")
        val secretConversionRate = organicHidden.metrics.doubleValue("secretConversionRate")
        val organicSearchActionUseRate = organicHidden.metrics.doubleValue("searchActionUseRate")
        val organicSecretZoneEntryRate = organicHidden.metrics.doubleValue("secretZoneEntryRate")
        val sameZoneSecretVsCadenceMaxOverlap = loot.metrics.doubleValue("sameZoneSecretVsCadenceMaxOverlap")
        val sameZoneSecretVsRewardMaxOverlap = loot.metrics.doubleValue("sameZoneSecretVsRewardMaxOverlap")
        val strictLocalIdentityViolations =
            loot.metrics.getValue("strictLocalIdentityViolations").jsonArray.map { violation ->
                violation.jsonObject.toLootStrictLocalIdentityViolation()
            }
        val strictViolationBreakdown = strictLocalIdentityViolations.splitByLocalIdentityPairType()
        val strictCadenceViolations = strictViolationBreakdown.cadenceViolations
        val strictRewardViolations = strictViolationBreakdown.rewardViolations
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
        val localRewardEntriesByMetricId =
            Phase4AggregationInputRunner.localRewardIdentityEvaluation(task = loot, baseline = lootBaseline)
                .entries
                .associateBy(EvaluationEntry::metricId)
        val terminalBuildEntriesByMetricId =
            Phase4AggregationInputRunner.terminalBuildIdentityEvaluation(task = longRun, baseline = terminalBuildBaseline)
                .entries
                .associateBy(EvaluationEntry::metricId)
        val criticalPathPacingEvaluation =
            CriticalPathPacingEvaluator.evaluate(
                longRun.metrics,
                thresholds = CriticalPathPacingThresholds.fromBaseline(criticalPathPacingBaseline),
            )
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
        return buildList {
            add(
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
                    note =
                        "primerCases=${scriptedHidden.metrics.intValue("primerActionUsedCount")}, " +
                            "primerFreeCases=${scriptedHidden.metrics.intValue("primerFreeCaseCount")}",
                ),
            )
            add(
                Phase4ExperienceMetric(
                    metricId = "leadDiscoveryRate",
                    sourceTaskId = organicHidden.taskId,
                    currentValue =
                        buildJsonObject {
                            put("rate", organicHidden.metrics.getValue("leadDiscoveryRate"))
                            put("totalCases", organicHidden.metrics.getValue("totalCases"))
                            put("leadDiscoveryCount", organicHidden.metrics.getValue("leadDiscoveryCount"))
                            put("searchActionUseRate", organicHidden.metrics.getValue("searchActionUseRate"))
                            put("secretConversionRate", organicHidden.metrics.getValue("secretConversionRate"))
                            put("secretZoneEntryRate", organicHidden.metrics.getValue("secretZoneEntryRate"))
                            put("firstHiddenDiscoveryTurnP50", organicHidden.metrics.getValue("firstHiddenDiscoveryTurnP50"))
                            put("firstHiddenDiscoveryTurnP90", organicHidden.metrics.getValue("firstHiddenDiscoveryTurnP90"))
                            put("firstSecretZoneEntryTurnP50", organicHidden.metrics.getValue("firstSecretZoneEntryTurnP50"))
                            put("firstSecretZoneEntryTurnP90", organicHidden.metrics.getValue("firstSecretZoneEntryTurnP90"))
                            put("comboCount", organicHidden.metrics.getValue("comboCount"))
                            put("seedsPerZoneCombo", organicHidden.metrics.getValue("seedsPerZoneCombo"))
                            put("searchPromptRequired", organicHidden.metrics.getValue("searchPromptRequired"))
                            put("reactiveSearchOnly", organicHidden.metrics.getValue("reactiveSearchOnly"))
                            put("zones", organicHidden.metrics.getValue("zones"))
                            put("combinations", organicHidden.metrics.getValue("combinations"))
                            put("zoneDiscoveryDistribution", organicHidden.metrics.getValue("zoneDiscoveryDistribution"))
                            put("secretZoneDiscoveryDistribution", organicHidden.metrics.getValue("secretZoneDiscoveryDistribution"))
                        },
                    currentValueText =
                        "${formatPercent(leadDiscoveryRate)} ($organicDiscoveryCount/$organicTotalCases), " +
                            "searchUse=${formatPercent(organicSearchActionUseRate)}, secretEntry=${formatPercent(organicSecretZoneEntryRate)}",
                    target = Phase4OwnerMetricTargets.targetText("leadDiscoveryRate", leadDiscoveryRange),
                    status = verdictOf(Phase4OwnerMetricTargets.passes(leadDiscoveryRange, leadDiscoveryRate)),
                    note =
                        "probeBot=${organicHidden.metrics.stringValue("probeBotId")}, scripted=false, observationOnly=true, " +
                            "promptRequired=${organicHidden.metrics.booleanValue("searchPromptRequired")}, " +
                            "combos=${organicHidden.metrics.intValue("comboCount")}, seedsPerCombo=${organicHidden.metrics.intValue("seedsPerZoneCombo")}",
                ),
            )
            add(
                Phase4ExperienceMetric(
                    metricId = "secretConversionRate",
                    sourceTaskId = organicHidden.taskId,
                    currentValue =
                        buildJsonObject {
                            put("rate", organicHidden.metrics.getValue("secretConversionRate"))
                            put("leadDiscoveryCount", organicHidden.metrics.getValue("leadDiscoveryCount"))
                            put("secretConversionCount", organicHidden.metrics.getValue("secretConversionCount"))
                            put("secretZoneEntryRate", organicHidden.metrics.getValue("secretZoneEntryRate"))
                            put("perZoneSecretEntryMinRate", organicHidden.metrics.getValue("perZoneSecretEntryMinRate"))
                            put("failingSecretEntryZoneIds", organicHidden.metrics.getValue("failingSecretEntryZoneIds"))
                            put("zones", organicHidden.metrics.getValue("zones"))
                        },
                    currentValueText =
                        "${formatPercent(secretConversionRate)} ($secretConversionCount/$organicDiscoveryCount), " +
                            "secretEntry=${formatPercent(organicSecretZoneEntryRate)}",
                    target = Phase4OwnerMetricTargets.targetText("secretConversionRate", secretConversionRange),
                    status =
                        verdictOf(
                            Phase4OwnerMetricTargets.passes(secretConversionRange, secretConversionRate) &&
                                organicHidden.metrics.stringList("failingSecretEntryZoneIds").isEmpty(),
                        ),
                    note =
                        "perZoneSecretEntryMinRate=${formatPercent(organicHidden.metrics.doubleValue("perZoneSecretEntryMinRate"))}, " +
                            "failingZones=${organicHidden.metrics.stringList("failingSecretEntryZoneIds").joinToString().ifBlank { "none" }}",
                ),
            )
            add(
                Phase4ExperienceMetric(
                    metricId = "sameZoneSecretVsCadenceMaxOverlap",
                    sourceTaskId = loot.taskId,
                    currentValue =
                        buildJsonObject {
                            put("maxOverlap", loot.metrics.getValue("sameZoneSecretVsCadenceMaxOverlap"))
                            put("pairs", loot.metrics.getValue("sameZoneSecretVsCadencePairs"))
                            put("localIdentityFailurePairs", loot.metrics.getValue("localIdentityFailurePairs"))
                            put("strictLocalIdentityViolations", loot.metrics.getValue("strictLocalIdentityViolations"))
                        },
                    currentValueText = formatStrictAwareLocalIdentityCurrentValue(sameZoneSecretVsCadenceMaxOverlap, strictCadenceViolations),
                    target = Phase4OwnerMetricTargets.targetText("sameZoneSecretVsCadenceMaxOverlap", cadenceOverlapRange),
                    status =
                        verdictOf(
                            Phase4OwnerMetricTargets.passes(cadenceOverlapRange, sameZoneSecretVsCadenceMaxOverlap) &&
                                strictCadenceViolations.isEmpty(),
                        ),
                    note =
                        localIdentityEvaluationDetail(
                            pairCount = loot.metrics.getValue("sameZoneSecretVsCadencePairs").jsonArray.size,
                            strictViolationCount = strictCadenceViolations.size,
                            includeOverlapFormula = true,
                        ),
                ),
            )
            add(
                Phase4ExperienceMetric(
                    metricId = "sameZoneSecretVsRewardMaxOverlap",
                    sourceTaskId = loot.taskId,
                    currentValue =
                        buildJsonObject {
                            put("maxOverlap", loot.metrics.getValue("sameZoneSecretVsRewardMaxOverlap"))
                            put("pairs", loot.metrics.getValue("sameZoneSecretVsRewardPairs"))
                            put("localIdentityFailurePairs", loot.metrics.getValue("localIdentityFailurePairs"))
                            put("strictLocalIdentityViolations", loot.metrics.getValue("strictLocalIdentityViolations"))
                        },
                    currentValueText = formatStrictAwareLocalIdentityCurrentValue(sameZoneSecretVsRewardMaxOverlap, strictRewardViolations),
                    target = Phase4OwnerMetricTargets.targetText("sameZoneSecretVsRewardMaxOverlap", rewardOverlapRange),
                    status =
                        verdictOf(
                            Phase4OwnerMetricTargets.passes(rewardOverlapRange, sameZoneSecretVsRewardMaxOverlap) &&
                                strictRewardViolations.isEmpty(),
                        ),
                    note =
                        localIdentityEvaluationDetail(
                            pairCount = loot.metrics.getValue("sameZoneSecretVsRewardPairs").jsonArray.size,
                            failurePairCount = loot.metrics.getValue("localIdentityFailurePairs").jsonArray.size,
                            strictViolationCount = strictRewardViolations.size,
                        ),
                ),
            )
            add(
                localRewardEntriesByMetricId.getValue("dynamicPoolCoverage").toLegacyExperienceMetric(loot.taskId),
            )
            add(
                localRewardEntriesByMetricId.getValue("specialTierPassiveFamilyDuplicateCount").toLegacyExperienceMetric(loot.taskId),
            )
            add(
                localRewardEntriesByMetricId.getValue("secretZoneRewardAuthorityViolations").toLegacyExperienceMetric(loot.taskId),
            )
            add(
                localRewardEntriesByMetricId.getValue("professionCapstoneSourceCoverage.reportOnly").toLegacyExperienceMetric(loot.taskId),
            )
            add(
                Phase4ExperienceMetric(
                    metricId = "terminalWeaponBaseDiversity",
                    sourceTaskId = longRun.taskId,
                    currentValue = longRun.metrics.getValue("terminalWeaponBaseDiversity"),
                    currentValueText = terminalWeaponBaseDiversity.toString(),
                    target = Phase4OwnerMetricTargets.targetText("terminalWeaponBaseDiversity", diversityRange),
                    status = verdictOf(Phase4OwnerMetricTargets.passes(diversityRange, terminalWeaponBaseDiversity.toDouble())),
                    note = "terminalBases=$terminalWeaponBaseNote; topWeaponSemantics=$professionTopWeaponSemanticNote",
                ),
            )
            add(
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
            )
            add(
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
            )
            add(
                terminalBuildEntriesByMetricId.getValue("professionCapstoneSeenRate").toLegacyExperienceMetric(longRun.taskId),
            )
            add(
                terminalBuildEntriesByMetricId.getValue("professionCapstoneAdoptionRate").toLegacyExperienceMetric(longRun.taskId),
            )
            add(
                terminalBuildEntriesByMetricId.getValue("nonWeaponBuildPayoffRate").toLegacyExperienceMetric(longRun.taskId),
            )
            add(
                terminalBuildEntriesByMetricId.getValue("professionCapstoneAdoptionFloor.reportOnly").toLegacyExperienceMetric(longRun.taskId),
            )
            add(
                terminalBuildEntriesByMetricId.getValue("nonWeaponBuildPayoffFloor.reportOnly").toLegacyExperienceMetric(longRun.taskId),
            )
            addAll(
                criticalPathPacingEvaluation.toExperienceMetrics(longRun.taskId).map { metric ->
                    metric.toLegacyExperienceMetric()
                },
            )
            add(
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
            )
            add(
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
    }

    private fun renderMarkdown(report: Phase4AggregateReport): String =
        buildString {
            val metricsById = report.experienceMetrics.associateBy(Phase4ExperienceMetric::metricId)
            val tasksById = report.tasks.associateBy(Phase4TaskAggregate::taskId)
            val lootTask = requireTask(tasksById, "whiteBoxLoot")
            val longRunTask = requireTask(tasksById, "longRunLab")
            val criticalPathMetric = metricsById.getValue("criticalPathCombatFloorSatisfied")
            val criticalPathMetricValue = criticalPathMetric.currentValue.jsonObject
            val criticalPathZoneIds =
                criticalPathMetricValue.getValue("criticalPathZoneIds").jsonArray.map { zoneId ->
                    zoneId.jsonPrimitive.content
                }
            val criticalPathBreakdown = criticalPathMetricValue.getValue("zoneBreakdown").jsonObject
            val criticalPathDesignAudit =
                metricsById.getValue("criticalPathCombatFloorSatisfied").details
                    .getValue("designAudit")
                    .jsonArray
                    .toCriticalPathDesignAuditSnapshots()
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
            appendLine("## Organic Hidden")
            appendLine("- sourceTask.scripted: `${scriptedHiddenTask.taskId}`")
            appendLine("- sourceTask.organic: `${organicHiddenTask.taskId}`")
            appendLine("- `scriptedHiddenVerificationRate`: ${metricsById.getValue("scriptedHiddenVerificationRate").currentValueText} / ${metricsById.getValue("scriptedHiddenVerificationRate").status}")
            appendLine("- `leadDiscoveryRate`: ${metricsById.getValue("leadDiscoveryRate").currentValueText} / ${metricsById.getValue("leadDiscoveryRate").status}")
            appendLine("- `secretConversionRate`: ${metricsById.getValue("secretConversionRate").currentValueText} / ${metricsById.getValue("secretConversionRate").status}")
            appendLine("- `zoneDiscoveryDistribution`: ${organicHiddenTask.metrics.getValue("zoneDiscoveryDistribution").jsonObject.entries.joinToString { (zoneId, rate) -> "$zoneId=${formatPercent(rate.jsonPrimitive.content.toDouble())}" }}")
            appendLine("- `secretZoneDiscoveryDistribution`: ${organicHiddenTask.metrics.getValue("secretZoneDiscoveryDistribution").jsonObject.entries.joinToString { (secretZoneId, rate) -> "$secretZoneId=${formatPercent(rate.jsonPrimitive.content.toDouble())}" }}")
            appendLine("- `failingSecretEntryZoneIds`: `${organicHiddenTask.metrics.stringList("failingSecretEntryZoneIds").joinToString().ifBlank { "none" }}`")
            appendLine("- `searchPromptRequired`: `${organicHiddenTask.metrics.booleanValue("searchPromptRequired")}`")
            appendLine()
            appendLine("## Local Reward Identity")
            appendLine("- sourceTask: `${lootTask.taskId}`")
            appendLine("- `sameZoneSecretVsCadenceMaxOverlap`: ${metricsById.getValue("sameZoneSecretVsCadenceMaxOverlap").currentValueText} / ${metricsById.getValue("sameZoneSecretVsCadenceMaxOverlap").status}")
            appendLine("- `sameZoneSecretVsRewardMaxOverlap`: ${metricsById.getValue("sameZoneSecretVsRewardMaxOverlap").currentValueText} / ${metricsById.getValue("sameZoneSecretVsRewardMaxOverlap").status}")
            appendLine("- `dynamicPoolCoverage`: ${metricsById.getValue("dynamicPoolCoverage").currentValueText} / ${metricsById.getValue("dynamicPoolCoverage").status}")
            appendLine("- `specialTierPassiveFamilyDuplicateCount`: ${metricsById.getValue("specialTierPassiveFamilyDuplicateCount").currentValueText} / ${metricsById.getValue("specialTierPassiveFamilyDuplicateCount").status}")
            appendLine("- `secretZoneRewardAuthorityViolations`: ${metricsById.getValue("secretZoneRewardAuthorityViolations").currentValueText} / ${metricsById.getValue("secretZoneRewardAuthorityViolations").status}")
            appendLine("- `localIdentityFailurePairs`: ${lootTask.metrics.getValue("localIdentityFailurePairs").jsonArray.joinToString { pair -> pair.jsonPrimitive.content }.ifBlank { "none" }}")
            appendLine("- `strictLocalIdentityViolations`: ${lootTask.metrics.getValue("strictLocalIdentityViolations").jsonArray.joinToString { violation -> violation.jsonObject.getValue("pairId").jsonPrimitive.content }.ifBlank { "none" }}")
            appendLine("- `secretZoneRewardAuthorityViolationIds`: ${lootTask.metrics.getValue("secretZoneRewardAuthorityViolations").jsonArray.joinToString { violation -> violation.jsonObject.getValue("violationId").jsonPrimitive.content }.ifBlank { "none" }}")
            appendLine("- `specialTierPassiveFamilyDuplicateSummary.duplicateFamilyCount`: ${lootTask.metrics.getValue("specialTierPassiveFamilyDuplicateSummary").jsonObject.getValue("duplicateFamilyCount").jsonPrimitive.content}")
            appendLine("- `uniqueArtifactMeaningfulSwapRate`: ${formatPercent(lootTask.metrics.doubleValue("uniqueArtifactMeaningfulSwapRate"))}")
            lootTask.metrics["secretProfileIdentitySummaries"]
                ?.jsonArray
                ?.takeIf { summaries -> summaries.isNotEmpty() }
                ?.let { summaries ->
                    appendLine("- secret reward identity summaries:")
                    summaries.forEach { summary ->
                        val payload = summary.jsonObject
                        val profileId = payload.getValue("profileId").jsonPrimitive.content
                        val canonicalZoneId = payload.getValue("canonicalZoneId").jsonPrimitive.content
                        val rewardStructureKeys =
                            payload.getValue("rewardStructureKeys").jsonArray.joinToString { key ->
                                key.jsonPrimitive.content
                            }
                        appendLine("  - `$profileId` (`$canonicalZoneId`)")
                        appendLine("    - rewardStructureKeys: `$rewardStructureKeys`")
                    }
                }
            appendLine()
            appendLine("## Terminal Build Identity")
            appendLine("- sourceTask: `${longRunTask.taskId}`")
            appendLine("- `terminalWeaponBaseDiversity`: ${metricsById.getValue("terminalWeaponBaseDiversity").currentValueText} / ${metricsById.getValue("terminalWeaponBaseDiversity").status}")
            appendLine("- `crossProfessionTopWeaponDominance`: ${metricsById.getValue("crossProfessionTopWeaponDominance").currentValueText} / ${metricsById.getValue("crossProfessionTopWeaponDominance").status}")
            appendLine("- `professionAlignedWeaponAdoptionRate`: ${metricsById.getValue("professionAlignedWeaponAdoptionRate").currentValueText} / ${metricsById.getValue("professionAlignedWeaponAdoptionRate").status}")
            appendLine("- `professionCapstoneSeenRate`: ${metricsById.getValue("professionCapstoneSeenRate").currentValueText} / ${metricsById.getValue("professionCapstoneSeenRate").status}")
            appendLine("- `professionCapstoneAdoptionRate`: ${metricsById.getValue("professionCapstoneAdoptionRate").currentValueText} / ${metricsById.getValue("professionCapstoneAdoptionRate").status}")
            appendLine("- `nonWeaponBuildPayoffRate`: ${metricsById.getValue("nonWeaponBuildPayoffRate").currentValueText} / ${metricsById.getValue("nonWeaponBuildPayoffRate").status}")
            appendLine("- reportOnlyProfessionFloors: `${professionBuildIdentityReportOnlyFloorNote()}`")
            appendLine("- preferredRewardSources: `${professionBuildIdentityPreferredSourceNote()}`")
            val professionTopWeaponSemanticTags = longRunTask.metrics.getValue("professionTopWeaponSemanticTags")
            val professionCapstoneBreakdown = longRunTask.metrics.getValue("professionCapstoneBreakdown")
            appendLine("```json")
            appendLine(professionTerminalWeaponDistribution)
            appendLine("```")
            appendLine("```json")
            appendLine(professionTopWeaponSemanticTags.toString())
            appendLine("```")
            appendLine("```json")
            appendLine(professionCapstoneBreakdown.toString())
            appendLine("```")
            appendLine()
            appendLine("## Critical Path Pacing")
            appendLine("- sourceTask: `${longRunTask.taskId}`")
            appendLine("- `avgObjectiveAcquireTurn`: ${metricsById.getValue("avgObjectiveAcquireTurn").currentValueText} / ${metricsById.getValue("avgObjectiveAcquireTurn").status}")
            appendLine("- `avgVisibleHostileTurnCount`: ${metricsById.getValue("avgVisibleHostileTurnCount").currentValueText} / ${metricsById.getValue("avgVisibleHostileTurnCount").status}")
            appendLine("- `avgEnemyTurns`: ${metricsById.getValue("avgEnemyTurns").currentValueText} / ${metricsById.getValue("avgEnemyTurns").status}")
            appendLine("- `criticalPathCombatFloorSatisfied`: ${criticalPathMetric.currentValueText} / ${criticalPathMetric.status}")
            appendLine("- criticalPathZoneIds: `${criticalPathZoneIds.joinToString()}`")
            appendLine("| zoneId | avgObjectiveAcquireTurn | avgVisibleHostileTurnCount | avgEnemyTurns | satisfied |")
            appendLine("| --- | --- | --- | --- | --- |")
            criticalPathZoneIds.forEach { zoneId ->
                val zoneBreakdown = criticalPathBreakdown.getValue(zoneId).jsonObject
                appendLine(
                    "| `$zoneId` | ${formatNullableScalar(zoneBreakdown["avgObjectiveAcquireTurn"]?.jsonPrimitive?.content?.toDoubleOrNull())} | ${formatScalar(zoneBreakdown.getValue("avgVisibleHostileTurnCount").jsonPrimitive.content.toDouble())} | ${formatScalar(zoneBreakdown.getValue("avgEnemyTurns").jsonPrimitive.content.toDouble())} | `${zoneBreakdown.getValue("satisfied").jsonPrimitive.content}` |",
                )
            }
            appendLine()
            appendLine("### Critical Path Design Audit")
            appendLine("| zoneId | floorCount | mapSize | worldRole | objectiveSetId | objectiveCompletionRule | mechanicsWithoutDedicatedRuntimeHook |")
            appendLine("| --- | --- | --- | --- | --- | --- | --- |")
            criticalPathDesignAudit.forEach { audit ->
                appendLine(
                    "| `${audit.zoneId}` | `${audit.floorCount}` | `${audit.mapSize}` | `${audit.worldRole}` | `${audit.objectiveSetId}` | `${audit.objectiveCompletionRule}` | `${audit.mechanicsWithoutDedicatedRuntimeHook.joinToString().ifBlank { "none" }}` |",
                )
            }
            appendLine()
            appendLine("## Scripted vs Organic Hidden")
            appendLine("- sourceTask.scripted: `${scriptedHiddenTask.taskId}`")
            appendLine("- sourceTask.organic: `${organicHiddenTask.taskId}`")
            appendLine("- `scriptedHiddenVerificationRate`: ${metricsById.getValue("scriptedHiddenVerificationRate").currentValueText} / ${metricsById.getValue("scriptedHiddenVerificationRate").status}")
            appendLine("- `leadDiscoveryRate`: ${metricsById.getValue("leadDiscoveryRate").currentValueText} / ${metricsById.getValue("leadDiscoveryRate").status}")
            appendLine("- `secretConversionRate`: ${metricsById.getValue("secretConversionRate").currentValueText} / ${metricsById.getValue("secretConversionRate").status}")
            appendLine("- scripted primer cases: `${scriptedHiddenTask.metrics.intValue("primerActionUsedCount")}`")
            appendLine("- organic search use rate: `${formatPercent(organicHiddenTask.metrics.doubleValue("searchActionUseRate"))}`")
            appendLine("- organic secret zone entry rate: `${formatPercent(organicHiddenTask.metrics.doubleValue("secretZoneEntryRate"))}`")
            appendLine("- organic failing secret-entry zones: `${organicHiddenTask.metrics.stringList("failingSecretEntryZoneIds").joinToString().ifBlank { "none" }}`")
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
        if (details.isNotEmpty()) {
            put("details", details)
        }
    }

private fun CriticalPathPacingLegacyMetricProjection.toLegacyExperienceMetric(): Phase4ExperienceMetric =
    Phase4ExperienceMetric(
        metricId = metricId,
        sourceTaskId = sourceTaskId,
        currentValue = currentValue,
        currentValueText = currentValueText,
        target = target,
        status = status,
        note = note,
        details = details,
    )

private fun EvaluationEntry.toLegacyExperienceMetric(sourceTaskId: String): Phase4ExperienceMetric =
    Phase4ExperienceMetric(
        metricId = metricId,
        sourceTaskId = sourceTaskId,
        currentValue = currentValue,
        currentValueText = currentValueText,
        target = targetText ?: "n/a",
        status = if (status == EvaluationEntryStatus.UNEXPECTED_REGRESSION) "FAIL" else "PASS",
        note = note,
        details = details,
    )

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

private fun requireTask(
    tasksById: Map<String, Phase4TaskAggregate>,
    taskId: String,
): Phase4TaskAggregate = checkNotNull(tasksById[taskId]) { "Missing phase4 task aggregate for $taskId." }

internal fun countFailedStatuses(statuses: Iterable<String>): Int = statuses.count { status -> status == "FAIL" }

private fun verdictOf(passed: Boolean): String = if (passed) "PASS" else "FAIL"

private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value * 100.0)

private fun formatPercentPrecise(value: Double): String = String.format(Locale.US, "%.2f%%", value * 100.0)

private fun formatScalar(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun formatNullableScalar(value: Double?): String = value?.let(::formatScalar) ?: "n/a"

private fun formatSignedPercent(value: Double): String =
    if (value >= 0.0) {
        "+${formatPercentPrecise(value)}"
    } else {
        "-${formatPercentPrecise(kotlin.math.abs(value))}"
    }

private fun JsonObject.intValue(key: String): Int = getValue(key).jsonPrimitive.content.toInt()

private fun JsonObject.doubleValue(key: String): Double = getValue(key).jsonPrimitive.content.toDouble()

private fun JsonObject.booleanValue(key: String): Boolean = getValue(key).jsonPrimitive.content.toBooleanStrict()

private fun JsonObject.stringValue(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.stringList(key: String): List<String> = getValue(key).jsonArray.map { element -> element.jsonPrimitive.content }
