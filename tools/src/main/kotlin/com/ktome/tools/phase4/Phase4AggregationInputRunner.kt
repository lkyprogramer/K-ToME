package com.ktome.tools.phase4

import com.ktome.tools.verification.BaselineMode
import com.ktome.tools.verification.EvaluationEntry
import com.ktome.tools.verification.EvaluationResult
import com.ktome.tools.verification.EvaluationVerdict
import com.ktome.tools.verification.KernelResult
import com.ktome.tools.verification.RenderResult
import com.ktome.tools.verification.ReportAggregationInput
import com.ktome.tools.verification.VerificationBaseline
import com.ktome.tools.verification.VerificationBaselineComparator
import com.ktome.tools.verification.VerificationExpectedMetricRange
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.io.path.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val PHASE4_AGGREGATION_INPUT_CONTRACT_VERSION: String = "phase4-aggregation-input-v3"
private const val AGGREGATION_INPUT_DIRECTORY_NAME: String = "inputs"
private const val AGGREGATION_INPUT_SUMMARY_FILE: String = "aggregation-input-summary.json"

private val phase4AggregationJson: Json =
    Json {
        prettyPrint = true
        explicitNulls = false
    }

@Serializable
internal data class Phase4AggregationInputSummary(
    val contractVersion: String,
    val generatedAt: String,
    val inputCount: Int,
    val reusedInputCount: Int,
    val regeneratedInputCount: Int,
    val domainCacheHitRate: Double,
    val artifactReuseRate: Double,
    val slowestDomain: String? = null,
    val slowestEvaluationMillis: Long? = null,
    val topInvalidationReasons: List<String> = emptyList(),
)

internal data class Phase4AggregationInputMaterialization(
    val inputDir: Path,
    val summaryPath: Path,
    val inputs: List<ReportAggregationInput>,
    val summary: Phase4AggregationInputSummary,
)

private data class MetricPresentation(
    val targetText: String,
    val note: String? = null,
)

private data class TerrainUnifiedBaselineMetric(
    val metricId: String,
    val baselineValue: Double,
    val targetRelativeIncrease: Double,
    val numerator: Int? = null,
    val denominator: Int? = null,
    val numeratorLabel: String? = null,
    val denominatorLabel: String? = null,
    val sourceValueText: String? = null,
    val normalizedFormula: String? = null,
) {
    val targetRate: Double = baselineValue * (1.0 + targetRelativeIncrease)
}

internal object Phase4AggregationInputRunner {
    fun materialize(): Phase4AggregationInputMaterialization {
        val repoRoot = repoRoot()
        val reportDir = aggregateReportDir()
        val inputDir = reportDir.resolve(AGGREGATION_INPUT_DIRECTORY_NAME)
        Files.createDirectories(inputDir)

        val taskReports = Phase4ReportRunner.collectTaskAggregates(repoRoot)
        val inputs = mutableListOf<ReportAggregationInput>()
        val invalidationReasonCounts = linkedMapOf<String, Int>()
        val baselineFingerprintCache = mutableMapOf<Path, String>()
        var reusedInputCount = 0
        var slowestDomain: String? = null
        var slowestEvaluationMillis: Long? = null

        taskReports.forEach { task ->
            val inputPath = inputDir.resolve("${task.taskId}.json")
            val baselinePaths = Phase4OwnerBaselineRegistry.ownerBaselinePaths(task.taskId)
            val currentFingerprints =
                buildFingerprints(
                    repoRoot = repoRoot,
                    task = task,
                    baselinePaths = baselinePaths,
                    baselineFingerprintCache = baselineFingerprintCache,
                )
            val existing = loadExistingInput(inputPath)
            val invalidationReason = resolveInvalidationReason(existing?.renderResult?.metadata, currentFingerprints)
            val evaluationStartNanos = System.nanoTime()
            val reused = invalidationReason == null && existing != null
            val baselinesByPath = Phase4OwnerBaselineRegistry.readOwnerBaselines(repoRoot = repoRoot, taskId = task.taskId)
            val input =
                if (reused) {
                    reusedInputCount += 1
                    existing
                } else {
                    invalidationReasonCounts.merge(invalidationReason ?: "missing-input-artifact", 1, Int::plus)
                    buildAggregationInput(task = task, baselinesByPath = baselinesByPath)
                }
            val evaluationMillis = Duration.ofNanos(System.nanoTime() - evaluationStartNanos).toMillis()
            if (slowestEvaluationMillis == null || evaluationMillis > slowestEvaluationMillis!!) {
                slowestEvaluationMillis = evaluationMillis
                slowestDomain = task.taskId
            }
            val annotatedInput =
                input.withMaterializationMetadata(
                    currentFingerprints = currentFingerprints,
                    cacheStatus = if (reused) "HIT" else "MISS",
                    artifactReused = reused,
                    invalidationReason = invalidationReason,
                    evaluationDurationMillis = evaluationMillis,
                )
            Files.writeString(inputPath, phase4AggregationJson.encodeToString(annotatedInput))
            inputs += annotatedInput
        }

        val inputCount = inputs.size
        val regeneratedInputCount = inputCount - reusedInputCount
        val cacheHitRate = ratio(reusedInputCount, inputCount)
        val summary =
            Phase4AggregationInputSummary(
                contractVersion = PHASE4_AGGREGATION_INPUT_CONTRACT_VERSION,
                generatedAt = Instant.now().toString(),
                inputCount = inputCount,
                reusedInputCount = reusedInputCount,
                regeneratedInputCount = regeneratedInputCount,
                domainCacheHitRate = cacheHitRate,
                artifactReuseRate = cacheHitRate,
                slowestDomain = slowestDomain,
                slowestEvaluationMillis = slowestEvaluationMillis,
                topInvalidationReasons =
                    invalidationReasonCounts.entries
                        .sortedByDescending(Map.Entry<String, Int>::value)
                        .map { (reason, count) -> "$reason:$count" },
            )
        val summaryPath = inputDir.resolve(AGGREGATION_INPUT_SUMMARY_FILE)
        Files.writeString(summaryPath, phase4AggregationJson.encodeToString(summary))
        return Phase4AggregationInputMaterialization(
            inputDir = inputDir,
            summaryPath = summaryPath,
            inputs = inputs,
            summary = summary,
        )
    }

    private fun loadExistingInput(path: Path): ReportAggregationInput? {
        if (!Files.exists(path)) {
            return null
        }
        return try {
            phase4AggregationJson.decodeFromString<ReportAggregationInput>(path.readText())
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveInvalidationReason(
        metadata: JsonObject?,
        currentFingerprints: JsonObject,
    ): String? {
        metadata ?: return "missing-render-metadata"
        if (metadata.stringValue("contractVersion") != PHASE4_AGGREGATION_INPUT_CONTRACT_VERSION) {
            return "evaluation-contract-changed"
        }
        if (metadata.getValue("sourceArtifactFingerprint") != currentFingerprints.getValue("sourceArtifactFingerprint")) {
            return "source-artifact-changed"
        }
        if (metadata.getValue("baselineFingerprints") != currentFingerprints.getValue("baselineFingerprints")) {
            return "baseline-changed"
        }
        return null
    }

    private fun buildFingerprints(
        repoRoot: Path,
        task: Phase4TaskAggregate,
        baselinePaths: List<String>,
        baselineFingerprintCache: MutableMap<Path, String>,
    ): JsonObject {
        val sourceArtifactPath = repoRoot.resolve(task.sourcePath)
        return buildJsonObject {
            put("sourceArtifactFingerprint", sha256(sourceArtifactPath))
            putJsonObject("baselineFingerprints") {
                baselinePaths.sorted().forEach { baselinePath ->
                    val absolutePath = repoRoot.resolve(baselinePath)
                    val digest = baselineFingerprintCache.getOrPut(absolutePath) { sha256(absolutePath) }
                    put(baselinePath, digest)
                }
            }
        }
    }

    private fun buildAggregationInput(
        task: Phase4TaskAggregate,
        baselinesByPath: Map<String, VerificationBaseline>,
    ): ReportAggregationInput {
        val evaluations = mutableListOf(taskStatusEvaluation(task))
        when (task.taskId) {
            "hiddenContentHarness" -> evaluations += scriptedHiddenEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.SCRIPTED_HIDDEN_BASELINE_RELATIVE_PATH))
            "organicHiddenProbe" -> evaluations += organicHiddenEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.ORGANIC_HIDDEN_BASELINE_RELATIVE_PATH))
            "whiteBoxLoot" -> evaluations += localRewardIdentityEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH))
            "longRunLab" -> evaluations += terminalBuildIdentityEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.TERMINAL_BUILD_BASELINE_RELATIVE_PATH))
            "terrainInteractionBatch" -> {
                evaluations += terrainAggregateEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.TERRAIN_UNIFIED_BASELINE_RELATIVE_PATH))
                evaluations += terrainPerZoneLowerBoundEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.TERRAIN_PER_ZONE_BASELINE_RELATIVE_PATH))
            }
        }
        val baselinePaths = baselinesByPath.keys.toList().sorted()
        return ReportAggregationInput(
            domainId = task.taskId,
            sourceTaskId = task.taskId,
            kernelResult =
                KernelResult(
                    domainId = task.taskId,
                    sourceTaskId = task.taskId,
                    sourcePath = task.sourcePath,
                    status = task.status,
                    buildId = task.buildId,
                    locale = task.locale,
                    metrics = task.metrics,
                ),
            evaluationResults = evaluations,
            renderResult =
                RenderResult(
                    renderId = "${task.taskId}.sourceSummary",
                    domainId = task.taskId,
                    sourceTaskId = task.taskId,
                    reportOnly = true,
                    summaryPath = task.sourcePath,
                    artifactInputs = listOf(task.sourcePath) + baselinePaths,
                ),
        )
    }

    private fun taskStatusEvaluation(task: Phase4TaskAggregate): EvaluationResult =
        VerificationBaselineComparator.compareStrictZeroFailure(
            domainId = task.taskId,
            evaluationId = "${task.taskId}.taskStatus",
            metricId = "task:${task.taskId}",
            observedFailureCount = if (task.status == "PASS") 0 else 1,
            currentValue = JsonPrimitive(task.status),
            currentValueText = task.status,
            targetText = "PASS",
            note = task.sourcePath,
            details = task.metrics,
        )

    private fun scriptedHiddenEvaluation(
        task: Phase4TaskAggregate,
        baseline: VerificationBaseline,
    ): EvaluationResult {
        val totalCases = task.metrics.intValue("totalCases")
        val failureCount = task.metrics.intValue("failureCount")
        val verificationRate = if (totalCases == 0) 0.0 else (totalCases - failureCount).toDouble() / totalCases.toDouble()
        val metricId = "scriptedHiddenVerificationRate"
        val result =
            VerificationBaselineComparator.compareBudgetThreshold(
                domainId = "hidden",
                evaluationId = "hidden.scriptedOwner",
                baseline = baseline,
                actualMetrics = mapOf(metricId to verificationRate),
                currentValueTexts = mapOf(metricId to "${formatPercent(verificationRate)} (${totalCases - failureCount}/$totalCases)"),
                currentValueElements =
                    mapOf(
                        metricId to
                            buildJsonObject {
                                put("rate", verificationRate)
                                put("totalCases", task.metrics.getValue("totalCases"))
                                put("failureCount", task.metrics.getValue("failureCount"))
                                put("primerActionUsedCount", task.metrics.getValue("primerActionUsedCount"))
                                put("primerFreeCaseCount", task.metrics.getValue("primerFreeCaseCount"))
                                put("secretZoneDiscoveryRate", task.metrics.getValue("secretZoneDiscoveryRate"))
                            },
                    ),
                detailsByMetricId = mapOf(metricId to task.metrics),
            )
        return result.withEntryPresentation(
            metricId = metricId,
            presentation =
                MetricPresentation(
                    targetText = Phase4MetricCatalog.requireSpec(metricId).targetText,
                    note =
                        "primerCases=${task.metrics.intValue("primerActionUsedCount")}, " +
                            "primerFreeCases=${task.metrics.intValue("primerFreeCaseCount")}",
                ),
        )
    }

    private fun organicHiddenEvaluation(
        task: Phase4TaskAggregate,
        baseline: VerificationBaseline,
    ): EvaluationResult {
        val totalCases = task.metrics.intValue("totalCases")
        val discoveryCount = task.metrics.intValue("discoveryWithoutPrimerCount")
        val discoveryRate = task.metrics.doubleValue("organicHiddenDiscoveryRate")
        val searchActionUseRate = task.metrics.doubleValue("searchActionUseRate")
        val secretZoneEntryRate = task.metrics.doubleValue("secretZoneEntryRate")
        val metricId = "organicHiddenDiscoveryRate"
        val result =
            VerificationBaselineComparator.compareBudgetThreshold(
                domainId = "organic-hidden",
                evaluationId = "organic-hidden.owner",
                baseline = baseline,
                actualMetrics = mapOf(metricId to discoveryRate),
                currentValueTexts =
                    mapOf(
                        metricId to
                            "${formatPercent(discoveryRate)} ($discoveryCount/$totalCases), " +
                                "searchUse=${formatPercent(searchActionUseRate)}, secretEntry=${formatPercent(secretZoneEntryRate)}",
                    ),
                currentValueElements =
                    mapOf(
                        metricId to
                            buildJsonObject {
                                put("rate", task.metrics.getValue("organicHiddenDiscoveryRate"))
                                put("totalCases", task.metrics.getValue("totalCases"))
                                put("discoveryWithoutPrimerCount", task.metrics.getValue("discoveryWithoutPrimerCount"))
                                put("searchActionUseRate", task.metrics.getValue("searchActionUseRate"))
                                put("secretZoneEntryRate", task.metrics.getValue("secretZoneEntryRate"))
                                put("zones", task.metrics.getValue("zones"))
                            },
                    ),
                detailsByMetricId = mapOf(metricId to task.metrics),
            )
        return result.withEntryPresentation(
            metricId = metricId,
            presentation =
                MetricPresentation(
                    targetText = Phase4MetricCatalog.requireSpec(metricId).targetText,
                    note = "probeBot=${task.metrics.stringValue("probeBotId")}, scripted=false, observationOnly=true",
                ),
        )
    }

    private fun localRewardIdentityEvaluation(
        task: Phase4TaskAggregate,
        baseline: VerificationBaseline,
    ): EvaluationResult {
        val cadenceMetricId = "sameZoneSecretVsCadenceMaxOverlap"
        val rewardMetricId = "sameZoneSecretVsRewardMaxOverlap"
        val cadenceOverlap = task.metrics.doubleValue(cadenceMetricId)
        val rewardOverlap = task.metrics.doubleValue(rewardMetricId)
        val result =
            VerificationBaselineComparator.compareBudgetThreshold(
                domainId = "loot",
                evaluationId = "loot.localRewardIdentity",
                baseline = baseline,
                actualMetrics =
                    mapOf(
                        cadenceMetricId to cadenceOverlap,
                        rewardMetricId to rewardOverlap,
                    ),
                currentValueTexts =
                    mapOf(
                        cadenceMetricId to formatRatio(cadenceOverlap),
                        rewardMetricId to formatRatio(rewardOverlap),
                    ),
                currentValueElements =
                    mapOf(
                        cadenceMetricId to
                            buildJsonObject {
                                put("maxOverlap", task.metrics.getValue(cadenceMetricId))
                                put("pairs", task.metrics.getValue("sameZoneSecretVsCadencePairs"))
                                put("localIdentityFailurePairs", task.metrics.getValue("localIdentityFailurePairs"))
                            },
                        rewardMetricId to
                            buildJsonObject {
                                put("maxOverlap", task.metrics.getValue(rewardMetricId))
                                put("pairs", task.metrics.getValue("sameZoneSecretVsRewardPairs"))
                                put("localIdentityFailurePairs", task.metrics.getValue("localIdentityFailurePairs"))
                            },
                    ),
                detailsByMetricId =
                    mapOf(
                        cadenceMetricId to task.metrics,
                        rewardMetricId to task.metrics,
                    ),
            )
        return result.withEntryPresentations(
            mapOf(
                cadenceMetricId to
                    MetricPresentation(
                        targetText = Phase4MetricCatalog.requireSpec(cadenceMetricId).targetText,
                        note =
                            "pairCount=${task.metrics.getValue("sameZoneSecretVsCadencePairs").jsonArray.size}, " +
                                "overlap = |A ∩ B| / min(|A|, |B|)",
                    ),
                rewardMetricId to
                    MetricPresentation(
                        targetText = Phase4MetricCatalog.requireSpec(rewardMetricId).targetText,
                        note =
                            "pairCount=${task.metrics.getValue("sameZoneSecretVsRewardPairs").jsonArray.size}, " +
                                "failurePairs=${task.metrics.getValue("localIdentityFailurePairs").jsonArray.size}",
                    ),
            ),
        )
    }

    private fun terminalBuildIdentityEvaluation(
        task: Phase4TaskAggregate,
        baseline: VerificationBaseline,
    ): EvaluationResult {
        val diversityMetricId = "terminalWeaponBaseDiversity"
        val dominanceMetricId = "crossProfessionTopWeaponDominance"
        val adoptionMetricId = "professionAlignedWeaponAdoptionRate"
        val diversity = task.metrics.intValue(diversityMetricId)
        val dominance = task.metrics.doubleValue(dominanceMetricId)
        val adoption = task.metrics.doubleValue(adoptionMetricId)
        val fullRouteCount = task.metrics.intValue("fullRouteCount")
        val alignedFullRouteSampleCount = task.metrics.intValue("alignedFullRouteSampleCount")
        val professionTerminalWeaponDistribution = task.metrics.getValue("professionTerminalWeaponDistribution")
        val professionTopWeaponBaseIds = task.metrics.getValue("professionTopWeaponBaseIds")
        val professionTopWeaponSemanticTags = task.metrics.getValue("professionTopWeaponSemanticTags")
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
        val result =
            VerificationBaselineComparator.compareBudgetThreshold(
                domainId = "longrun",
                evaluationId = "longrun.terminalBuildIdentity",
                baseline = baseline,
                actualMetrics =
                    mapOf(
                        diversityMetricId to diversity.toDouble(),
                        dominanceMetricId to dominance,
                        adoptionMetricId to adoption,
                    ),
                currentValueTexts =
                    mapOf(
                        diversityMetricId to diversity.toString(),
                        dominanceMetricId to
                            "${formatPercent(dominance)} (${task.metrics.intValue("crossProfessionTopWeaponCount")}/$fullRouteCount) " +
                                "top=${task.metrics["crossProfessionTopWeaponBaseId"]?.jsonPrimitive?.content ?: "unknown"}",
                        adoptionMetricId to "${formatPercent(adoption)} ($alignedFullRouteSampleCount/$fullRouteCount)",
                    ),
                currentValueElements =
                    mapOf(
                        diversityMetricId to task.metrics.getValue(diversityMetricId),
                        dominanceMetricId to
                            buildJsonObject {
                                put("rate", task.metrics.getValue(dominanceMetricId))
                                task.metrics["crossProfessionTopWeaponBaseId"]?.let { topWeaponBaseId -> put("topWeaponBaseId", topWeaponBaseId) }
                                put("topWeaponCount", task.metrics.getValue("crossProfessionTopWeaponCount"))
                                put("fullRouteCount", task.metrics.getValue("fullRouteCount"))
                            },
                        adoptionMetricId to
                            buildJsonObject {
                                put("rate", task.metrics.getValue(adoptionMetricId))
                                put("alignedFullRouteSampleCount", task.metrics.getValue("alignedFullRouteSampleCount"))
                                put("fullRouteCount", task.metrics.getValue("fullRouteCount"))
                                put("professionTerminalWeaponDistribution", professionTerminalWeaponDistribution)
                                put("professionTopWeaponBaseIds", professionTopWeaponBaseIds)
                                put("professionTopWeaponSemanticTags", professionTopWeaponSemanticTags)
                            },
                    ),
                detailsByMetricId =
                    mapOf(
                        diversityMetricId to task.metrics,
                        dominanceMetricId to task.metrics,
                        adoptionMetricId to task.metrics,
                    ),
            )
        return result.withEntryPresentations(
            mapOf(
                diversityMetricId to
                    MetricPresentation(
                        targetText = Phase4MetricCatalog.requireSpec(diversityMetricId).targetText,
                        note = "terminalBases=$terminalWeaponBaseNote; topWeaponSemantics=$professionTopWeaponSemanticNote",
                    ),
                dominanceMetricId to
                    MetricPresentation(
                        targetText = Phase4MetricCatalog.requireSpec(dominanceMetricId).targetText,
                        note = "topWeaponBaseId=${task.metrics["crossProfessionTopWeaponBaseId"]?.jsonPrimitive?.content ?: "unknown"}",
                    ),
                adoptionMetricId to
                    MetricPresentation(
                        targetText = Phase4MetricCatalog.requireSpec(adoptionMetricId).targetText,
                        note = "alignedSamples=$alignedFullRouteSampleCount/$fullRouteCount; topWeaponSemantics=$professionTopWeaponSemanticNote",
                    ),
            ),
        )
    }

    private fun terrainAggregateEvaluation(
        task: Phase4TaskAggregate,
        baseline: VerificationBaseline,
    ): EvaluationResult {
        val metricId = "terrainInteractionEncounterRate.aggregate"
        val range = baseline.requiredMetric(metricId)
        val baselineMetric = range.toTerrainUnifiedBaselineMetric()
        val terrainEncounterRate = task.metrics.doubleValue("terrainInteractionEncounterRate")
        val taggedCombatCount = task.metrics.intValue("taggedCombatCount")
        val triggeredInteractionCombatCount = task.metrics.intValue("triggeredInteractionCombatCount")
        val metricDefinitionVersion = task.metrics.stringValue("terrainMetricDefinitionVersion")
        require(metricDefinitionVersion == baseline.metricDefinitionVersion) {
            "Terrain metric definition drifted: runtime=$metricDefinitionVersion baseline=${baseline.metricDefinitionVersion}."
        }
        val relativeIncrease = relativeIncrease(terrainEncounterRate, baselineMetric.baselineValue)
        val result =
            VerificationBaselineComparator.compareRelativeBaseline(
                domainId = "terrain",
                evaluationId = "terrain.aggregateRelativeBaseline",
                baseline = baseline.copy(expectedMetricRanges = listOf(range)),
                actualMetrics = mapOf(metricId to terrainEncounterRate),
                currentValueTexts =
                    mapOf(
                        metricId to
                            "${formatPercent(terrainEncounterRate)} ($triggeredInteractionCombatCount/$taggedCombatCount), " +
                                "delta=${formatSignedPercent(relativeIncrease)} vs baseline ${formatPercent(baselineMetric.baselineValue)}",
                    ),
                currentValueElements =
                    mapOf(
                        metricId to
                            buildJsonObject {
                                put("rate", task.metrics.getValue("terrainInteractionEncounterRate"))
                                put("taggedCombatCount", task.metrics.getValue("taggedCombatCount"))
                                put("triggeredInteractionCombatCount", task.metrics.getValue("triggeredInteractionCombatCount"))
                                put("baseline", baselineMetric.toJson())
                                put("relativeIncrease", relativeIncrease)
                                put("targetRate", baselineMetric.targetRate)
                                put("decisionPathByCurrentMetrics", task.metrics.getValue("decisionPathByCurrentMetrics"))
                                put("combatSampledZoneIds", task.metrics.getValue("combatSampledZoneIds"))
                                put("combatSampledZoneExclusionNotes", task.metrics.getValue("combatSampledZoneExclusionNotes"))
                                put("terrainCoverageByZone", task.metrics.getValue("terrainCoverageByZone"))
                            },
                    ),
                detailsByMetricId = mapOf(metricId to task.metrics),
            )
        val targetText =
            ">= ${formatPercentPrecise(baselineMetric.targetRate)} " +
                "(baseline ${formatPercentPrecise(baselineMetric.baselineValue)} +${formatPercentPrecise(baselineMetric.targetRelativeIncrease)})"
        val note =
            "baseline=${baseline.baselineId} @ ${baseline.sourceArtifactPath ?: "unknown"} " +
                "(buildId=${baseline.sourceBuildId ?: "unknown"}, generatedAt=${baseline.sourceGeneratedAt ?: "unknown"}, " +
                "metricDefinitionVersion=$metricDefinitionVersion); " +
                "decisionPathByCurrentMetrics=${task.metrics.stringValue("decisionPathByCurrentMetrics")}"
        return result.withEntryPresentation(
            metricId = metricId,
            presentation = MetricPresentation(targetText = targetText, note = note),
        )
    }

    private fun terrainPerZoneLowerBoundEvaluation(
        task: Phase4TaskAggregate,
        baseline: VerificationBaseline,
    ): EvaluationResult {
        val metricId = "terrainInteractionEncounterRate.per_zone_lower_bound"
        val range = baseline.requiredMetric(metricId)
        val failureZones = task.metrics.getValue("perZoneEncounterFailures").jsonArray
        val failureCount = failureZones.size.toDouble()
        val lowerBoundTarget = range.requireMetadataDouble("perZoneEncounterLowerBoundTarget")
        val runtimeLowerBoundTarget = task.metrics.doubleValue("perZoneEncounterLowerBoundTarget")
        require(abs(runtimeLowerBoundTarget - lowerBoundTarget) < 1.0e-9) {
            "Terrain per-zone lower bound drifted: runtime=$runtimeLowerBoundTarget baseline=$lowerBoundTarget."
        }
        val result =
            VerificationBaselineComparator.compareBudgetThreshold(
                domainId = "terrain",
                evaluationId = "terrain.perZoneLowerBound",
                baseline = baseline,
                actualMetrics = mapOf(metricId to failureCount),
                currentValueTexts =
                    mapOf(
                        metricId to
                            if (failureZones.isEmpty()) {
                                "all sampled zones >= ${formatPercent(lowerBoundTarget)}"
                            } else {
                                "failed=${failureZones.joinToString { zone -> zone.jsonPrimitive.content }} target=${formatPercent(lowerBoundTarget)}"
                            },
                    ),
                currentValueElements =
                    mapOf(
                        metricId to
                            buildJsonObject {
                                put("target", JsonPrimitive(lowerBoundTarget))
                                put("failureZones", task.metrics.getValue("perZoneEncounterFailures"))
                                put("combatSampledZoneIds", task.metrics.getValue("combatSampledZoneIds"))
                                put("combatSampledZoneExclusionNotes", task.metrics.getValue("combatSampledZoneExclusionNotes"))
                                put("terrainCoverageByZone", task.metrics.getValue("terrainCoverageByZone"))
                            },
                    ),
                detailsByMetricId = mapOf(metricId to task.metrics),
            )
        val note =
            "baseline=${baseline.baselineId}, lowerBoundTarget=${formatPercent(lowerBoundTarget)}; " +
                "combatSampledZoneIds=${task.metrics.stringList("combatSampledZoneIds").joinToString()}, " +
                "exclusions=${task.metrics.getValue("combatSampledZoneExclusionNotes").jsonArray.joinToString { zone -> zone.jsonPrimitive.content }}"
        return result.withEntryPresentation(
            metricId = metricId,
            presentation =
                MetricPresentation(
                    targetText = Phase4MetricCatalog.requireSpec(metricId).targetText,
                    note = note,
                ),
        )
    }

    private fun ReportAggregationInput.withMaterializationMetadata(
        currentFingerprints: JsonObject,
        cacheStatus: String,
        artifactReused: Boolean,
        invalidationReason: String?,
        evaluationDurationMillis: Long,
    ): ReportAggregationInput {
        val currentRender = checkNotNull(renderResult) { "Phase4 aggregation input for $sourceTaskId must include renderResult." }
        val updatedMetadata =
            buildJsonObject {
                currentRender.metadata.forEach { (key, value) -> put(key, value) }
                put("contractVersion", PHASE4_AGGREGATION_INPUT_CONTRACT_VERSION)
                put("cacheStatus", cacheStatus)
                put("artifactReused", artifactReused)
                invalidationReason?.let { value -> put("invalidationReason", value) }
                put("evaluationDurationMillis", evaluationDurationMillis)
                put("sourceArtifactFingerprint", currentFingerprints.getValue("sourceArtifactFingerprint"))
                put("baselineFingerprints", currentFingerprints.getValue("baselineFingerprints"))
            }
        return copy(renderResult = currentRender.copy(metadata = updatedMetadata))
    }
}

private fun VerificationBaseline.requiredMetric(metricId: String): VerificationExpectedMetricRange =
    checkNotNull(expectedMetricRange(metricId)) { "Missing baseline metric '$metricId' in ${baselineId}." }

private fun VerificationExpectedMetricRange.toTerrainUnifiedBaselineMetric(): TerrainUnifiedBaselineMetric =
    TerrainUnifiedBaselineMetric(
        metricId = metricId,
        baselineValue =
            checkNotNull(baselineValue) {
                "Terrain unified baseline metric '$metricId' must declare baselineValue."
            },
        targetRelativeIncrease =
            checkNotNull(targetRelativeIncrease) {
                "Terrain unified baseline metric '$metricId' must declare targetRelativeIncrease."
            },
        numerator = numerator,
        denominator = denominator,
        numeratorLabel = numeratorLabel,
        denominatorLabel = denominatorLabel,
        sourceValueText = sourceValueText,
        normalizedFormula = normalizedFormula,
    )

private fun VerificationExpectedMetricRange.requireMetadataDouble(key: String): Double =
    metadata[key]?.jsonPrimitive?.content?.toDouble()
        ?: error("VerificationExpectedMetricRange($metricId) metadata must declare numeric '$key'.")

private fun TerrainUnifiedBaselineMetric.toJson(): JsonObject =
    buildJsonObject {
        put(
            "metricId",
            if (metricId == "terrainInteractionEncounterRate.aggregate") {
                "terrainInteractionEncounterRate"
            } else {
                metricId
            },
        )
        put("baselineRate", baselineValue)
        put("targetRelativeIncrease", targetRelativeIncrease)
        put("targetRate", targetRate)
        numerator?.let { value -> put("numerator", value) }
        denominator?.let { value -> put("denominator", value) }
        numeratorLabel?.let { value -> put("numeratorLabel", value) }
        denominatorLabel?.let { value -> put("denominatorLabel", value) }
        sourceValueText?.let { value -> put("sourceValueText", value) }
        normalizedFormula?.let { value -> put("normalizedFormula", value) }
    }

private fun EvaluationResult.withEntryPresentation(
    metricId: String,
    presentation: MetricPresentation,
): EvaluationResult = withEntryPresentations(mapOf(metricId to presentation))

private fun EvaluationResult.withEntryPresentations(
    presentations: Map<String, MetricPresentation>,
): EvaluationResult =
    copy(
        entries =
            entries.map { entry ->
                presentations[entry.metricId]?.let { presentation ->
                    entry.copy(
                        targetText = presentation.targetText,
                        note = presentation.note ?: entry.note,
                    )
                } ?: entry
            },
    )

private fun repoRoot(): Path {
    val configured = System.getProperty("ktome.repo.root")
    return if (configured.isNullOrBlank()) Path.of(".").toAbsolutePath().normalize() else Path.of(configured).toAbsolutePath().normalize()
}

private fun aggregateReportDir(): Path {
    val configured = System.getProperty("ktome.phase4.aggregate.reportDir")
    return if (configured.isNullOrBlank()) {
        Path.of("tools", "build", "reports", "verification", "phase4")
    } else {
        Path.of(configured)
    }
}

private fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(DEFAULT_SHA256_BUFFER_SIZE)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead < 0) {
                break
            }
            if (bytesRead > 0) {
                digest.update(buffer, 0, bytesRead)
            }
        }
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private const val DEFAULT_SHA256_BUFFER_SIZE: Int = 8 * 1024

private fun JsonObject.intValue(key: String): Int = getValue(key).jsonPrimitive.content.toInt()

private fun JsonObject.doubleValue(key: String): Double = getValue(key).jsonPrimitive.content.toDouble()

private fun JsonObject.stringValue(key: String): String? = get(key)?.jsonPrimitive?.content

private fun JsonObject.stringList(key: String): List<String> =
    getValue(key).jsonArray.map { value -> value.jsonPrimitive.content }

private fun ratio(
    numerator: Int,
    denominator: Int,
): Double =
    if (denominator == 0) {
        0.0
    } else {
        numerator.toDouble() / denominator.toDouble()
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

private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value * 100.0)

private fun formatPercentPrecise(value: Double): String = String.format(Locale.US, "%.2f%%", value * 100.0)

private fun formatSignedPercent(value: Double): String =
    if (value >= 0.0) {
        "+${formatPercentPrecise(value)}"
    } else {
        String.format(Locale.US, "%.2f%%", value * 100.0)
    }

private fun formatRatio(value: Double): String = String.format(Locale.US, "%.3f", value)
