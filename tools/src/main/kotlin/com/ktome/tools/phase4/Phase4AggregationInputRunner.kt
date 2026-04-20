package com.ktome.tools.phase4

import com.ktome.game.loot.foundationBuildIdentityByProfessionId
import com.ktome.game.loot.foundationBuildIdentityProfessionIds
import com.ktome.tools.loot.LocalRewardIdentityMetricEvaluationInput
import com.ktome.tools.loot.buildLocalRewardIdentityEvaluation
import com.ktome.tools.loot.splitByLocalIdentityPairType
import com.ktome.tools.loot.toLootStrictLocalIdentityViolation
import com.ktome.tools.verification.BaselineMode
import com.ktome.tools.verification.EvaluationEntry
import com.ktome.tools.verification.EvaluationEntryStatus
import com.ktome.tools.verification.EvaluationResult
import com.ktome.tools.verification.EvaluationVerdict
import com.ktome.tools.verification.KernelResult
import com.ktome.tools.verification.RenderResult
import com.ktome.tools.verification.ReportAggregationInput
import com.ktome.tools.verification.VerificationBaseline
import com.ktome.tools.verification.VerificationBaselineComparator
import com.ktome.tools.verification.VerificationCacheSupport
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

private const val PHASE4_AGGREGATION_INPUT_CONTRACT_VERSION: String = "phase4-aggregation-input-v8"
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

internal data class ProfessionCapstoneSeenFloorViolation(
    val professionId: String,
    val seenCount: Int,
    val requiredSeenCount: Int,
    val sampleCount: Int,
)

internal data class ProfessionCapstoneCountFloorViolation(
    val professionId: String,
    val actualCount: Int,
    val requiredCount: Int,
    val sampleCount: Int,
)

private data class Phase4EvaluationCacheEntry(
    val cachePath: Path,
    val fingerprint: String,
    val cacheRoot: Path,
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

        val taskReports = Phase4DomainArtifactRegistry.collectTaskAggregates(repoRoot)
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
            val evaluationCacheEntry = resolveEvaluationCacheEntry(repoRoot = repoRoot, task = task, currentFingerprints = currentFingerprints)
            val existing = loadExistingInput(inputPath) ?: evaluationCacheEntry?.cachePath?.let(::loadExistingInput)
            val invalidationReason = resolveInvalidationReason(existing?.renderResult?.metadata, currentFingerprints)
            val evaluationStartNanos = System.nanoTime()
            val reused = invalidationReason == null && existing != null
            val input =
                if (reused) {
                    reusedInputCount += 1
                    existing
                } else {
                    val baselinesByPath = Phase4OwnerBaselineRegistry.readOwnerBaselines(repoRoot = repoRoot, taskId = task.taskId)
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
                    artifactReuseSource = evaluationCacheEntry?.let { entry -> VerificationCacheSupport.relativeToRepo(entry.cacheRoot, repoRoot) },
                    evaluationCacheFingerprint = evaluationCacheEntry?.fingerprint,
                    invalidationReason = invalidationReason,
                    evaluationDurationMillis = evaluationMillis,
                )
            Files.writeString(inputPath, phase4AggregationJson.encodeToString(annotatedInput))
            evaluationCacheEntry?.let { entry ->
                Files.createDirectories(entry.cachePath.parent)
                Files.writeString(entry.cachePath, phase4AggregationJson.encodeToString(annotatedInput))
            }
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
            "hiddenContentHarness" -> evaluations += scriptedHiddenEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.scriptedHiddenBaselinePath()))
            "organicHiddenProbe" -> evaluations += organicHiddenEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.organicHiddenBaselinePath()))
            "whiteBoxLoot" ->
                evaluations +=
                    readEmbeddedOwnerEvaluation(repoRoot = repoRoot(), task = task)
                        ?.takeIf { embedded -> embeddedLootOwnerEvaluationMatchesTaskMetrics(task = task, evaluation = embedded) }
                        ?: localRewardIdentityEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.lootBaselinePath()))
            "longRunLab" -> {
                evaluations += terminalBuildIdentityEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.terminalBuildBaselinePath()))
                evaluations += criticalPathPacingEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.criticalPathPacingBaselinePath()))
            }
            "terrainInteractionBatch" -> {
                evaluations += terrainAggregateEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.terrainUnifiedBaselinePath()))
                evaluations += terrainPerZoneLowerBoundEvaluation(task = task, baseline = baselinesByPath.getValue(Phase4OwnerBaselineRegistry.terrainPerZoneBaselinePath()))
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

    private fun readEmbeddedOwnerEvaluation(
        repoRoot: Path,
        task: Phase4TaskAggregate,
    ): EvaluationResult? {
        val sourcePath = repoRoot.resolve(task.sourcePath)
        if (!Files.isRegularFile(sourcePath)) {
            return null
        }
        return runCatching {
            val payload = phase4AggregationJson.parseToJsonElement(sourcePath.readText()).jsonObject
            val ownerEvaluation = payload["ownerEvaluation"] ?: return null
            phase4AggregationJson.decodeFromString<EvaluationResult>(
                phase4AggregationJson.encodeToString(JsonElement.serializer(), ownerEvaluation),
            )
        }.getOrNull()
    }

    private fun resolveEvaluationCacheEntry(
        repoRoot: Path,
        task: Phase4TaskAggregate,
        currentFingerprints: JsonObject,
    ): Phase4EvaluationCacheEntry? {
        if (task.taskId != "longRunLab") {
            return null
        }
        val cacheDirs = VerificationCacheSupport.cacheDirs(domainId = "longrun", repoRoot = repoRoot)
        val fingerprint =
            VerificationCacheSupport.sha256(
                PHASE4_AGGREGATION_INPUT_CONTRACT_VERSION,
                task.taskId,
                phase4AggregationJson.encodeToString(JsonElement.serializer(), currentFingerprints),
            )
        val cacheRoot = VerificationCacheSupport.ensureDirectory(cacheDirs.evaluationDir.resolve("$PHASE4_AGGREGATION_INPUT_CONTRACT_VERSION-$fingerprint"))
        return Phase4EvaluationCacheEntry(
            cachePath = cacheRoot.resolve("${task.taskId}.json"),
            fingerprint = fingerprint,
            cacheRoot = cacheRoot,
        )
    }

    private fun embeddedLootOwnerEvaluationMatchesTaskMetrics(
        task: Phase4TaskAggregate,
        evaluation: EvaluationResult,
    ): Boolean {
        if (task.taskId != "whiteBoxLoot" || evaluation.evaluationId != "loot.localRewardIdentity") {
            return true
        }
        val entriesByMetricId = evaluation.entries.associateBy(EvaluationEntry::metricId)
        val strictViolationBreakdown =
            task.metrics.getValue("strictLocalIdentityViolations").jsonArray
                .map { violation -> violation.jsonObject.toLootStrictLocalIdentityViolation() }
                .splitByLocalIdentityPairType()
        val expectedCadenceStatus =
            if (strictViolationBreakdown.cadenceViolations.isEmpty()) {
                EvaluationEntryStatus.PASS
            } else {
                EvaluationEntryStatus.UNEXPECTED_REGRESSION
            }
        val expectedRewardStatus =
            if (strictViolationBreakdown.rewardViolations.isEmpty()) {
                EvaluationEntryStatus.PASS
            } else {
                EvaluationEntryStatus.UNEXPECTED_REGRESSION
            }
        val expectedDynamicPoolStatus =
            if (task.metrics.doubleValue("dynamicPoolCoverage") >= 1.0) {
                EvaluationEntryStatus.PASS
            } else {
                EvaluationEntryStatus.UNEXPECTED_REGRESSION
            }
        val expectedSpecialTierDuplicateStatus =
            if (task.metrics.intValue("specialTierPassiveFamilyDuplicateCount") == 0) {
                EvaluationEntryStatus.PASS
            } else {
                EvaluationEntryStatus.UNEXPECTED_REGRESSION
            }
        val expectedSourceCoverageStatus =
            if (
                requireRewardRoutingCoverageSummary(task.metrics)
                    .getValue("professionSourceCoverage")
                    .jsonArray
                    .all { coverage -> coverage.jsonObject.getValue("covered").jsonPrimitive.content.toBooleanStrict() }
            ) {
                EvaluationEntryStatus.PASS
            } else {
                EvaluationEntryStatus.UNEXPECTED_REGRESSION
            }
        val expectedUnexpectedRegressionCount =
            listOf(
                expectedCadenceStatus,
                expectedRewardStatus,
                expectedDynamicPoolStatus,
                expectedSpecialTierDuplicateStatus,
                expectedSourceCoverageStatus,
            ).count { status -> status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }
        val expectedVerdict =
            if (expectedUnexpectedRegressionCount > 0) {
                EvaluationVerdict.FAIL
            } else {
                EvaluationVerdict.PASS
            }
        return entriesByMetricId["sameZoneSecretVsCadenceMaxOverlap"]?.status == expectedCadenceStatus &&
            entriesByMetricId["sameZoneSecretVsRewardMaxOverlap"]?.status == expectedRewardStatus &&
            entriesByMetricId["dynamicPoolCoverage"]?.status == expectedDynamicPoolStatus &&
            entriesByMetricId["specialTierPassiveFamilyDuplicateCount"]?.status == expectedSpecialTierDuplicateStatus &&
            entriesByMetricId["professionCapstoneSourceCoverage.reportOnly"]?.status == expectedSourceCoverageStatus &&
            evaluation.unexpectedRegressionCount == expectedUnexpectedRegressionCount &&
            evaluation.verdict == expectedVerdict
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
        val range = baseline.requiredMetric(metricId)
        val result =
            VerificationBaselineComparator.compareBudgetThreshold(
                domainId = "hidden",
                evaluationId = "hidden.scriptedOwner",
                baseline = baseline.copy(expectedMetricRanges = listOf(range)),
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
                    targetText = Phase4OwnerMetricTargets.targetText(metricId, range),
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
        val leadDiscoveryCount = task.metrics.intValue("leadDiscoveryCount")
        val leadDiscoveryRate = task.metrics.doubleValue("leadDiscoveryRate")
        val secretConversionCount = task.metrics.intValue("secretConversionCount")
        val secretConversionRate = task.metrics.doubleValue("secretConversionRate")
        val searchActionUseRate = task.metrics.doubleValue("searchActionUseRate")
        val secretZoneEntryRate = task.metrics.doubleValue("secretZoneEntryRate")
        val leadMetricId = "leadDiscoveryRate"
        val secretConversionMetricId = "secretConversionRate"
        val leadRange = baseline.requiredMetric(leadMetricId)
        val secretConversionRange = baseline.requiredMetric(secretConversionMetricId)
        val perZoneSecretEntryMinRate = task.metrics.doubleValue("perZoneSecretEntryMinRate")
        val failingSecretEntryZoneIds = task.metrics.stringList("failingSecretEntryZoneIds")
        val leadEntry =
            EvaluationEntry(
                metricId = leadMetricId,
                status =
                    if (Phase4OwnerMetricTargets.passes(leadRange, leadDiscoveryRate)) {
                        EvaluationEntryStatus.PASS
                    } else {
                        EvaluationEntryStatus.UNEXPECTED_REGRESSION
                    },
                currentValue =
                    buildJsonObject {
                        put("rate", task.metrics.getValue("leadDiscoveryRate"))
                        put("totalCases", task.metrics.getValue("totalCases"))
                        put("leadDiscoveryCount", task.metrics.getValue("leadDiscoveryCount"))
                        put("searchActionUseRate", task.metrics.getValue("searchActionUseRate"))
                        put("secretZoneEntryRate", task.metrics.getValue("secretZoneEntryRate"))
                        put("secretConversionRate", task.metrics.getValue("secretConversionRate"))
                        put("firstHiddenDiscoveryTurnP50", task.metrics.getValue("firstHiddenDiscoveryTurnP50"))
                        put("firstHiddenDiscoveryTurnP90", task.metrics.getValue("firstHiddenDiscoveryTurnP90"))
                        put("firstSecretZoneEntryTurnP50", task.metrics.getValue("firstSecretZoneEntryTurnP50"))
                        put("firstSecretZoneEntryTurnP90", task.metrics.getValue("firstSecretZoneEntryTurnP90"))
                        put("comboCount", task.metrics.getValue("comboCount"))
                        put("seedsPerZoneCombo", task.metrics.getValue("seedsPerZoneCombo"))
                        put("searchPromptRequired", task.metrics.getValue("searchPromptRequired"))
                        put("reactiveSearchOnly", task.metrics.getValue("reactiveSearchOnly"))
                        put("zones", task.metrics.getValue("zones"))
                        put("combinations", task.metrics.getValue("combinations"))
                        put("zoneDiscoveryDistribution", task.metrics.getValue("zoneDiscoveryDistribution"))
                        put("secretZoneDiscoveryDistribution", task.metrics.getValue("secretZoneDiscoveryDistribution"))
                    },
                currentValueText =
                    "${formatPercent(leadDiscoveryRate)} ($leadDiscoveryCount/$totalCases), " +
                        "searchUse=${formatPercent(searchActionUseRate)}, secretEntry=${formatPercent(secretZoneEntryRate)}",
                targetText = Phase4OwnerMetricTargets.targetText(leadMetricId, leadRange),
                note =
                    "probeBot=${task.metrics.stringValue("probeBotId")}, scripted=false, observationOnly=true, " +
                        "promptRequired=${task.metrics.booleanValue("searchPromptRequired")}, " +
                        "combos=${task.metrics.intValue("comboCount")}, seedsPerCombo=${task.metrics.intValue("seedsPerZoneCombo")}",
                details = task.metrics,
            )
        val secretConversionEntry =
            EvaluationEntry(
                metricId = secretConversionMetricId,
                status =
                    if (Phase4OwnerMetricTargets.passes(secretConversionRange, secretConversionRate) && failingSecretEntryZoneIds.isEmpty()) {
                        EvaluationEntryStatus.PASS
                    } else {
                        EvaluationEntryStatus.UNEXPECTED_REGRESSION
                    },
                currentValue =
                    buildJsonObject {
                        put("rate", task.metrics.getValue("secretConversionRate"))
                        put("leadDiscoveryCount", task.metrics.getValue("leadDiscoveryCount"))
                        put("secretConversionCount", task.metrics.getValue("secretConversionCount"))
                        put("secretZoneEntryRate", task.metrics.getValue("secretZoneEntryRate"))
                        put("perZoneSecretEntryMinRate", task.metrics.getValue("perZoneSecretEntryMinRate"))
                        put("failingSecretEntryZoneIds", task.metrics.getValue("failingSecretEntryZoneIds"))
                        put("zones", task.metrics.getValue("zones"))
                    },
                currentValueText =
                    "${formatPercent(secretConversionRate)} ($secretConversionCount/$leadDiscoveryCount), " +
                        "secretEntry=${formatPercent(secretZoneEntryRate)}",
                targetText = Phase4OwnerMetricTargets.targetText(secretConversionMetricId, secretConversionRange),
                note =
                    "perZoneSecretEntryMinRate=${formatPercent(perZoneSecretEntryMinRate)}, " +
                        "failingZones=${failingSecretEntryZoneIds.joinToString().ifBlank { "none" }}",
                details = task.metrics,
            )
        val entries = listOf(leadEntry, secretConversionEntry)
        val unexpectedRegressionCount = entries.count { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }
        return EvaluationResult(
            evaluationId = "organic-hidden.owner",
            domainId = "organic-hidden",
            mode = baseline.mode,
            verdict = if (unexpectedRegressionCount == 0) EvaluationVerdict.PASS else EvaluationVerdict.FAIL,
            baselineId = baseline.baselineId,
            metricDefinitionVersion = baseline.metricDefinitionVersion,
            passCount = entries.count { entry -> entry.status == EvaluationEntryStatus.PASS },
            approvedDebtCount = 0,
            expectedFailureCount = 0,
            unexpectedRegressionCount = unexpectedRegressionCount,
            improvedDebtCount = 0,
            entries = entries,
        )
    }

    internal fun localRewardIdentityEvaluation(
        task: Phase4TaskAggregate,
        baseline: VerificationBaseline,
    ): EvaluationResult {
        val cadenceMetricId = "sameZoneSecretVsCadenceMaxOverlap"
        val rewardMetricId = "sameZoneSecretVsRewardMaxOverlap"
        val dynamicPoolMetricId = "dynamicPoolCoverage"
        val specialTierDuplicateMetricId = "specialTierPassiveFamilyDuplicateCount"
        val secretZoneRewardAuthorityMetricId = "secretZoneRewardAuthorityViolations"
        val sourceCoverageMetricId = "professionCapstoneSourceCoverage.reportOnly"
        val cadenceOverlap = task.metrics.doubleValue(cadenceMetricId)
        val rewardOverlap = task.metrics.doubleValue(rewardMetricId)
        val dynamicPoolCoverage = task.metrics.doubleValue(dynamicPoolMetricId)
        val dynamicPoolTargetProfiles = task.metrics.getValue("dynamicPoolTargetProfiles")
        val specialTierPassiveFamilyDuplicateSummary = task.metrics.getValue("specialTierPassiveFamilyDuplicateSummary")
        val specialTierPassiveFamilyDuplicateCount =
            specialTierPassiveFamilyDuplicateSummary.jsonObject.getValue("duplicateFamilyCount").jsonPrimitive.content.toInt()
        val rewardRoutingCoverageSummary = requireRewardRoutingCoverageSummary(task.metrics)
        val uniqueArtifactMeaningfulSwapRate = task.metrics.doubleValue("uniqueArtifactMeaningfulSwapRate")
        val dynamicTargetCount =
            dynamicPoolTargetProfiles.jsonArray.count { profile ->
                profile.jsonObject.getValue("dynamic").jsonPrimitive.content.toBooleanStrict()
            }
        val strictViolations =
            task.metrics.getValue("strictLocalIdentityViolations").jsonArray.map { violation ->
                violation.jsonObject.toLootStrictLocalIdentityViolation()
            }
        val localRewardEvaluation =
            buildLocalRewardIdentityEvaluation(
            baseline = baseline,
            strictViolations = strictViolations,
            cadenceInput =
                LocalRewardIdentityMetricEvaluationInput(
                    metricId = cadenceMetricId,
                    overlap = cadenceOverlap,
                    currentValueElement =
                        buildJsonObject {
                            put("maxOverlap", task.metrics.getValue(cadenceMetricId))
                            put("pairs", task.metrics.getValue("sameZoneSecretVsCadencePairs"))
                            put("localIdentityFailurePairs", task.metrics.getValue("localIdentityFailurePairs"))
                            put("strictLocalIdentityViolations", task.metrics.getValue("strictLocalIdentityViolations"))
                        },
                    pairCount = task.metrics.getValue("sameZoneSecretVsCadencePairs").jsonArray.size,
                    includeOverlapFormula = true,
                ),
            rewardInput =
                LocalRewardIdentityMetricEvaluationInput(
                    metricId = rewardMetricId,
                    overlap = rewardOverlap,
                    currentValueElement =
                        buildJsonObject {
                            put("maxOverlap", task.metrics.getValue(rewardMetricId))
                            put("pairs", task.metrics.getValue("sameZoneSecretVsRewardPairs"))
                            put("localIdentityFailurePairs", task.metrics.getValue("localIdentityFailurePairs"))
                            put("strictLocalIdentityViolations", task.metrics.getValue("strictLocalIdentityViolations"))
                        },
                    pairCount = task.metrics.getValue("sameZoneSecretVsRewardPairs").jsonArray.size,
                    failurePairCount = task.metrics.getValue("localIdentityFailurePairs").jsonArray.size,
                ),
            detailsByMetricId =
                mapOf(
                    cadenceMetricId to task.metrics,
                    rewardMetricId to task.metrics,
                ),
        )
        val dynamicPoolRange = baseline.requiredMetric(dynamicPoolMetricId)
        val specialTierDuplicateRange = baseline.requiredMetric(specialTierDuplicateMetricId)
        val secretZoneRewardAuthorityRange = baseline.requiredMetric(secretZoneRewardAuthorityMetricId)
        val dynamicPoolEntry =
            EvaluationEntry(
                metricId = dynamicPoolMetricId,
                status =
                    if (Phase4OwnerMetricTargets.passes(dynamicPoolRange, dynamicPoolCoverage)) {
                        EvaluationEntryStatus.PASS
                    } else {
                        EvaluationEntryStatus.UNEXPECTED_REGRESSION
                    },
                currentValue =
                    buildJsonObject {
                        put("rate", task.metrics.getValue(dynamicPoolMetricId))
                        put("dynamicPoolTargetProfiles", dynamicPoolTargetProfiles)
                        put("specialTierPassiveFamilyDuplicateSummary", specialTierPassiveFamilyDuplicateSummary)
                        put("uniqueArtifactMeaningfulSwapRate", task.metrics.getValue("uniqueArtifactMeaningfulSwapRate"))
                    },
                currentValueText = "${formatPercent(dynamicPoolCoverage)} ($dynamicTargetCount/${dynamicPoolTargetProfiles.jsonArray.size})",
                targetText = Phase4OwnerMetricTargets.targetText(dynamicPoolMetricId, dynamicPoolRange),
                note =
                    "duplicateFamilies=${specialTierPassiveFamilyDuplicateSummary.jsonObject.getValue("duplicateFamilyCount").jsonPrimitive.content}; " +
                        "meaningfulSwap=${formatPercent(uniqueArtifactMeaningfulSwapRate)}",
                details = task.metrics,
            )
        val specialTierDuplicateEntry =
            EvaluationEntry(
                metricId = specialTierDuplicateMetricId,
                status =
                    if (Phase4OwnerMetricTargets.passes(specialTierDuplicateRange, specialTierPassiveFamilyDuplicateCount.toDouble())) {
                        EvaluationEntryStatus.PASS
                    } else {
                        EvaluationEntryStatus.UNEXPECTED_REGRESSION
                    },
                currentValue =
                    buildJsonObject {
                        put("count", specialTierPassiveFamilyDuplicateCount)
                        put("specialTierPassiveFamilyDuplicateSummary", specialTierPassiveFamilyDuplicateSummary)
                        put("uniqueArtifactMeaningfulSwapRate", task.metrics.getValue("uniqueArtifactMeaningfulSwapRate"))
                    },
                currentValueText = specialTierPassiveFamilyDuplicateCount.toString(),
                targetText = Phase4OwnerMetricTargets.targetText(specialTierDuplicateMetricId, specialTierDuplicateRange),
                note =
                    "duplicatedZones=${specialTierPassiveFamilyDuplicateSummary.jsonObject.getValue("duplicatedZoneCount").jsonPrimitive.content}; " +
                        "meaningfulSwap=${formatPercent(uniqueArtifactMeaningfulSwapRate)}",
                details = task.metrics,
            )
        val secretZoneRewardAuthorityViolationCount =
            task.metrics.intValue("secretZoneRewardAuthorityViolationCount")
        val secretZoneRewardAuthorityEntry =
            EvaluationEntry(
                metricId = secretZoneRewardAuthorityMetricId,
                status =
                    if (Phase4OwnerMetricTargets.passes(secretZoneRewardAuthorityRange, secretZoneRewardAuthorityViolationCount.toDouble())) {
                        EvaluationEntryStatus.PASS
                    } else {
                        EvaluationEntryStatus.UNEXPECTED_REGRESSION
                    },
                currentValue =
                    buildJsonObject {
                        put("count", task.metrics.getValue("secretZoneRewardAuthorityViolationCount"))
                        put("violations", task.metrics.getValue("secretZoneRewardAuthorityViolations"))
                    },
                currentValueText = secretZoneRewardAuthorityViolationCount.toString(),
                targetText = Phase4OwnerMetricTargets.targetText(secretZoneRewardAuthorityMetricId, secretZoneRewardAuthorityRange),
                note =
                    if (secretZoneRewardAuthorityViolationCount == 0) {
                        "secretZoneDef.rewardProfileId is the only authority"
                    } else {
                        "violations=" +
                            task.metrics.getValue("secretZoneRewardAuthorityViolations").jsonArray.joinToString { violation ->
                                violation.jsonObject.getValue("violationId").jsonPrimitive.content
                            }
                    },
                details = task.metrics,
            )
        val sourceCoveragePairs = rewardRoutingCoverageSummary.jsonObject.getValue("professionSourceCoverage").jsonArray
        val missingSourceCoverage =
            sourceCoveragePairs.filter { coverage ->
                !coverage.jsonObject.getValue("covered").jsonPrimitive.content.toBooleanStrict()
            }
        val sourceCoverageEntry =
            EvaluationEntry(
                metricId = sourceCoverageMetricId,
                status = if (missingSourceCoverage.isEmpty()) EvaluationEntryStatus.PASS else EvaluationEntryStatus.UNEXPECTED_REGRESSION,
                currentValue = rewardRoutingCoverageSummary,
                currentValueText =
                    "${rewardRoutingCoverageSummary.jsonObject.getValue("coveredSourcePairCount").jsonPrimitive.content}/" +
                        rewardRoutingCoverageSummary.jsonObject.getValue("totalSourcePairCount").jsonPrimitive.content,
                targetText = "all preferred profession/source pairs covered",
                note =
                    if (missingSourceCoverage.isEmpty()) {
                        "allPreferredSourcesCovered"
                    } else {
                        "missingSourceCoverage=" +
                            missingSourceCoverage.joinToString { coverage ->
                                val payload = coverage.jsonObject
                                "${payload.getValue("professionId").jsonPrimitive.content}:${payload.getValue("rewardSource").jsonPrimitive.content}" +
                                    "[${payload.getValue("culpritSourceIds").jsonArray.joinToString { culprit -> culprit.jsonPrimitive.content }.ifBlank { "none" }}]"
                            }
                    },
                details = task.metrics,
            )
        val entries = localRewardEvaluation.entries + dynamicPoolEntry + specialTierDuplicateEntry + secretZoneRewardAuthorityEntry + sourceCoverageEntry
        val unexpectedRegressionCount = entries.count { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }
        return localRewardEvaluation.copy(
            verdict = if (unexpectedRegressionCount > 0) EvaluationVerdict.FAIL else EvaluationVerdict.PASS,
            passCount = entries.count { entry -> entry.status == EvaluationEntryStatus.PASS },
            approvedDebtCount = entries.count { entry -> entry.status == EvaluationEntryStatus.APPROVED_DEBT },
            expectedFailureCount = entries.count { entry -> entry.status == EvaluationEntryStatus.EXPECTED_FAILURE },
            unexpectedRegressionCount = unexpectedRegressionCount,
            improvedDebtCount = entries.count { entry -> entry.status == EvaluationEntryStatus.IMPROVEMENT },
            entries = entries,
        )
    }

    internal fun terminalBuildIdentityEvaluation(
        task: Phase4TaskAggregate,
        baseline: VerificationBaseline,
    ): EvaluationResult {
        val diversityMetricId = "terminalWeaponBaseDiversity"
        val dominanceMetricId = "crossProfessionTopWeaponDominance"
        val adoptionMetricId = "professionAlignedWeaponAdoptionRate"
        val capstoneSeenMetricId = "professionCapstoneSeenRate"
        val capstoneAdoptionMetricId = "professionCapstoneAdoptionRate"
        val nonWeaponMetricId = "nonWeaponBuildPayoffRate"
        val diversity = task.metrics.intValue(diversityMetricId)
        val dominance = task.metrics.doubleValue(dominanceMetricId)
        val adoption = task.metrics.doubleValue(adoptionMetricId)
        val professionCapstoneSeenRate = task.metrics.doubleValue(capstoneSeenMetricId)
        val nonWeaponBuildPayoffRate = task.metrics.doubleValue(nonWeaponMetricId)
        val professionCapstoneAdoptionRate = task.metrics.doubleValue(capstoneAdoptionMetricId)
        val fullRouteCount = task.metrics.intValue("fullRouteCount")
        val alignedFullRouteSampleCount = task.metrics.intValue("alignedFullRouteSampleCount")
        val professionTerminalWeaponDistribution = task.metrics.getValue("professionTerminalWeaponDistribution")
        val professionTopWeaponBaseIds = task.metrics.getValue("professionTopWeaponBaseIds")
        val professionTopWeaponSemanticTags = task.metrics.getValue("professionTopWeaponSemanticTags")
        val professionCapstoneBreakdown = task.metrics.getValue("professionCapstoneBreakdown")
        val professionCapstoneAdoptionCount =
            professionCapstoneBreakdown.jsonObject.values.sumOf { breakdown ->
                breakdown.jsonObject.getValue("adoptedCount").jsonPrimitive.content.toInt()
            }
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
        val diversityRange = baseline.requiredMetric(diversityMetricId)
        val dominanceRange = baseline.requiredMetric(dominanceMetricId)
        val adoptionRange = baseline.requiredMetric(adoptionMetricId)
        val capstoneSeenRange = baseline.requiredMetric(capstoneSeenMetricId)
        val capstoneAdoptionRange = baseline.requiredMetric(capstoneAdoptionMetricId)
        val nonWeaponRange = baseline.requiredMetric(nonWeaponMetricId)
        val professionCapstoneSeenViolations =
            professionCapstoneSeenFloorViolations(
                professionCapstoneBreakdown = professionCapstoneBreakdown.jsonObject,
                range = capstoneSeenRange,
            )
        val professionCapstoneAdoptionViolations =
            professionCapstoneAdoptionFloorViolations(professionCapstoneBreakdown.jsonObject)
        val professionNonWeaponViolations =
            professionNonWeaponFloorViolations(professionCapstoneBreakdown.jsonObject)
        val result =
            VerificationBaselineComparator.compareBudgetThreshold(
                domainId = "longrun",
                evaluationId = "longrun.terminalBuildIdentity",
                baseline =
                    baseline.copy(
                        expectedMetricRanges =
                            listOf(
                                diversityRange,
                                dominanceRange,
                                adoptionRange,
                                capstoneSeenRange,
                                capstoneAdoptionRange,
                                nonWeaponRange,
                            ),
                    ),
                actualMetrics =
                    mapOf(
                        diversityMetricId to diversity.toDouble(),
                        dominanceMetricId to dominance,
                        adoptionMetricId to adoption,
                        capstoneSeenMetricId to professionCapstoneSeenRate,
                        capstoneAdoptionMetricId to professionCapstoneAdoptionRate,
                        nonWeaponMetricId to nonWeaponBuildPayoffRate,
                    ),
                currentValueTexts =
                    mapOf(
                        diversityMetricId to diversity.toString(),
                        dominanceMetricId to
                            "${formatPercent(dominance)} (${task.metrics.intValue("crossProfessionTopWeaponCount")}/$fullRouteCount) " +
                                "top=${task.metrics["crossProfessionTopWeaponBaseId"]?.jsonPrimitive?.content ?: "unknown"}",
                        adoptionMetricId to "${formatPercent(adoption)} ($alignedFullRouteSampleCount/$fullRouteCount)",
                        capstoneSeenMetricId to "${formatPercent(professionCapstoneSeenRate)} adoption=${formatPercent(professionCapstoneAdoptionRate)}",
                        capstoneAdoptionMetricId to "${formatPercent(professionCapstoneAdoptionRate)} ($professionCapstoneAdoptionCount/$fullRouteCount)",
                        nonWeaponMetricId to "${formatPercent(nonWeaponBuildPayoffRate)} adoption=${formatPercent(professionCapstoneAdoptionRate)}",
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
                        capstoneSeenMetricId to
                            buildJsonObject {
                                put("rate", task.metrics.getValue(capstoneSeenMetricId))
                                put("professionCapstoneAdoptionRate", task.metrics.getValue("professionCapstoneAdoptionRate"))
                                put("professionCapstoneBreakdown", professionCapstoneBreakdown)
                                put("fullRouteCount", task.metrics.getValue("fullRouteCount"))
                            },
                        capstoneAdoptionMetricId to
                            buildJsonObject {
                                put("rate", task.metrics.getValue(capstoneAdoptionMetricId))
                                put("professionCapstoneBreakdown", professionCapstoneBreakdown)
                                put("fullRouteCount", task.metrics.getValue("fullRouteCount"))
                            },
                        nonWeaponMetricId to
                            buildJsonObject {
                                put("rate", task.metrics.getValue(nonWeaponMetricId))
                                put("professionCapstoneAdoptionRate", task.metrics.getValue("professionCapstoneAdoptionRate"))
                                put("professionCapstoneBreakdown", professionCapstoneBreakdown)
                                put("fullRouteCount", task.metrics.getValue("fullRouteCount"))
                            },
                    ),
                detailsByMetricId =
                    mapOf(
                        diversityMetricId to task.metrics,
                        dominanceMetricId to task.metrics,
                        adoptionMetricId to task.metrics,
                        capstoneSeenMetricId to task.metrics,
                        capstoneAdoptionMetricId to task.metrics,
                        nonWeaponMetricId to task.metrics,
                    ),
            )
        val statusOverrides =
            if (professionCapstoneSeenViolations.isEmpty()) {
                emptyMap()
            } else {
                mapOf(
                    capstoneSeenMetricId to
                        EvaluationEntryStatusOverride(
                            status = EvaluationEntryStatus.UNEXPECTED_REGRESSION,
                            note = professionCapstoneSeenNote(professionCapstoneAdoptionRate, professionCapstoneBreakdown.jsonObject, professionCapstoneSeenViolations),
                        ),
                )
            }
        val baseResult =
            result
            .withEntryStatusOverrides(statusOverrides)
            .withEntryPresentations(
            mapOf(
                diversityMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(diversityMetricId, diversityRange),
                        note = "terminalBases=$terminalWeaponBaseNote; topWeaponSemantics=$professionTopWeaponSemanticNote",
                    ),
                dominanceMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(dominanceMetricId, dominanceRange),
                        note = "topWeaponBaseId=${task.metrics["crossProfessionTopWeaponBaseId"]?.jsonPrimitive?.content ?: "unknown"}",
                    ),
                adoptionMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(adoptionMetricId, adoptionRange),
                        note = "alignedSamples=$alignedFullRouteSampleCount/$fullRouteCount; topWeaponSemantics=$professionTopWeaponSemanticNote",
                    ),
                capstoneSeenMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(capstoneSeenMetricId, capstoneSeenRange),
                        note = professionCapstoneSeenNote(professionCapstoneAdoptionRate, professionCapstoneBreakdown.jsonObject, professionCapstoneSeenViolations),
                    ),
                capstoneAdoptionMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(capstoneAdoptionMetricId, capstoneAdoptionRange),
                        note =
                            "nonWeaponPayoff=${formatPercent(nonWeaponBuildPayoffRate)}; " +
                                "breakdown=${professionCapstoneBreakdown.jsonObject.keys.sorted().joinToString()}; " +
                                "reportOnlyFloors=${professionBuildIdentityReportOnlyFloorNote()}; " +
                                "preferredSources=${professionBuildIdentityPreferredSourceNote()}",
                    ),
                nonWeaponMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(nonWeaponMetricId, nonWeaponRange),
                        note =
                            "capstoneAdoption=${formatPercent(professionCapstoneAdoptionRate)}; " +
                                "breakdown=${professionCapstoneBreakdown.jsonObject.keys.sorted().joinToString()}; " +
                                "reportOnlyFloors=${professionBuildIdentityReportOnlyFloorNote()}; " +
                                "preferredSources=${professionBuildIdentityPreferredSourceNote()}",
                    ),
            ),
        )
        val reportOnlyEntries =
            listOf(
                EvaluationEntry(
                    metricId = "professionCapstoneAdoptionFloor.reportOnly",
                    status = if (professionCapstoneAdoptionViolations.isEmpty()) EvaluationEntryStatus.PASS else EvaluationEntryStatus.APPROVED_DEBT,
                    currentValue =
                        buildJsonObject {
                            put("professionCapstoneBreakdown", professionCapstoneBreakdown)
                        },
                    currentValueText =
                        "${phase4FoundationProfessionIds.size - professionCapstoneAdoptionViolations.size}/${phase4FoundationProfessionIds.size}",
                    targetText = "report-only per-profession adoption floor from build-identity schema",
                    note = professionCapstoneAdoptionFloorNote(professionCapstoneAdoptionViolations),
                    details = task.metrics,
                ),
                EvaluationEntry(
                    metricId = "nonWeaponBuildPayoffFloor.reportOnly",
                    status = if (professionNonWeaponViolations.isEmpty()) EvaluationEntryStatus.PASS else EvaluationEntryStatus.APPROVED_DEBT,
                    currentValue =
                        buildJsonObject {
                            put("professionCapstoneBreakdown", professionCapstoneBreakdown)
                        },
                    currentValueText = "${phase4FoundationProfessionIds.size - professionNonWeaponViolations.size}/${phase4FoundationProfessionIds.size}",
                    targetText = "report-only per-profession non-weapon floor from build-identity schema",
                    note = professionNonWeaponFloorNote(professionNonWeaponViolations),
                    details = task.metrics,
                ),
            )
        return appendReportOnlyEntries(baseResult, reportOnlyEntries)
    }

    private fun criticalPathPacingEvaluation(
        task: Phase4TaskAggregate,
        baseline: VerificationBaseline,
    ): EvaluationResult {
        val objectiveMetricId = "avgObjectiveAcquireTurn"
        val visibleMetricId = "avgVisibleHostileTurnCount"
        val enemyMetricId = "avgEnemyTurns"
        val satisfiedMetricId = "criticalPathCombatFloorSatisfied"
        val objectiveRange = baseline.requiredMetric(objectiveMetricId)
        val visibleRange = baseline.requiredMetric(visibleMetricId)
        val enemyRange = baseline.requiredMetric(enemyMetricId)
        val satisfiedRange = baseline.requiredMetric(satisfiedMetricId)
        val evaluation =
            CriticalPathPacingEvaluator.evaluate(
                longRunMetrics = task.metrics,
                thresholds = CriticalPathPacingThresholds.fromBaseline(baseline),
            )
        val result =
            evaluation
                .toEvaluationResult(
                    domainId = "longrun",
                    evaluationId = "longrun.criticalPathPacing",
                ).copy(
                    baselineId = baseline.baselineId,
                    metricDefinitionVersion = baseline.metricDefinitionVersion,
                )
        return result.withEntryPresentations(
            mapOf(
                objectiveMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(objectiveMetricId, objectiveRange),
                        note = evaluation.note,
                    ),
                visibleMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(visibleMetricId, visibleRange),
                        note = evaluation.note,
                    ),
                enemyMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(enemyMetricId, enemyRange),
                        note = evaluation.note,
                    ),
                satisfiedMetricId to
                    MetricPresentation(
                        targetText = Phase4OwnerMetricTargets.targetText(satisfiedMetricId, satisfiedRange),
                        note = evaluation.note,
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
            Phase4OwnerMetricTargets.targetText(metricId, range)
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
                    targetText = Phase4OwnerMetricTargets.targetText(metricId, range),
                    note = note,
                ),
        )
    }

    private fun ReportAggregationInput.withMaterializationMetadata(
        currentFingerprints: JsonObject,
        cacheStatus: String,
        artifactReused: Boolean,
        artifactReuseSource: String?,
        evaluationCacheFingerprint: String?,
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
                artifactReuseSource?.let { value -> put("artifactReuseSource", value) }
                evaluationCacheFingerprint?.let { value -> put("evaluationCacheFingerprint", value) }
                invalidationReason?.let { value -> put("invalidationReason", value) }
                put("evaluationDurationMillis", evaluationDurationMillis)
                put("sourceArtifactFingerprint", currentFingerprints.getValue("sourceArtifactFingerprint"))
                put("baselineFingerprints", currentFingerprints.getValue("baselineFingerprints"))
            }
        return copy(renderResult = currentRender.copy(metadata = updatedMetadata))
    }
}

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

private data class EvaluationEntryStatusOverride(
    val status: EvaluationEntryStatus,
    val note: String? = null,
)

private fun EvaluationResult.withEntryStatusOverrides(
    overrides: Map<String, EvaluationEntryStatusOverride>,
): EvaluationResult {
    if (overrides.isEmpty()) {
        return this
    }
    val updatedEntries =
        entries.map { entry ->
            overrides[entry.metricId]?.let { override ->
                entry.copy(status = override.status, note = override.note ?: entry.note)
            } ?: entry
        }
    return copy(
        verdict = if (updatedEntries.any { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }) EvaluationVerdict.FAIL else EvaluationVerdict.PASS,
        passCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.PASS },
        approvedDebtCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.APPROVED_DEBT },
        expectedFailureCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.EXPECTED_FAILURE },
        unexpectedRegressionCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION },
        improvedDebtCount = updatedEntries.count { entry -> entry.status == EvaluationEntryStatus.IMPROVEMENT },
        entries = updatedEntries,
    )
}

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

internal val phase4FoundationProfessionIds: Set<String> = foundationBuildIdentityProfessionIds

internal fun professionBuildIdentityReportOnlyFloorNote(): String =
    foundationBuildIdentityByProfessionId.values
        .sortedBy { identity -> identity.professionId }
        .joinToString(separator = ", ") { identity ->
            val floors = identity.reportOnlyFloors
            "${identity.professionId}(seen>=${floors.seenMinCount},adoption>=${floors.adoptionMinCount},nonWeapon>=${floors.nonWeaponMinCount})"
        }

internal fun professionBuildIdentityPreferredSourceNote(): String =
    foundationBuildIdentityByProfessionId.values
        .sortedBy { identity -> identity.professionId }
        .joinToString(separator = ", ") { identity ->
            "${identity.professionId}=${identity.preferredRewardSources.joinToString(separator = "/") { source -> source.name }}"
        }

internal fun professionCapstoneSeenFloorViolations(
    professionCapstoneBreakdown: JsonObject,
    range: VerificationExpectedMetricRange,
): List<ProfessionCapstoneSeenFloorViolation> {
    val requiredSeenCount = range.metadata["perProfessionSeenMinCount"]?.jsonPrimitive?.content?.toIntOrNull() ?: return emptyList()
    return phase4FoundationProfessionIds.mapNotNull { professionId ->
        val breakdown = professionCapstoneBreakdown[professionId]?.jsonObject
        val seenCount = breakdown?.get("seenCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val sampleCount = breakdown?.get("sampleCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        if (seenCount < requiredSeenCount) {
            ProfessionCapstoneSeenFloorViolation(
                professionId = professionId,
                seenCount = seenCount,
                requiredSeenCount = requiredSeenCount,
                sampleCount = sampleCount,
            )
        } else {
            null
        }
    }
}

internal fun professionCapstoneSeenNote(
    professionCapstoneAdoptionRate: Double,
    professionCapstoneBreakdown: JsonObject,
    violations: List<ProfessionCapstoneSeenFloorViolation>,
): String =
    buildList {
        add("capstoneAdoption=${formatPercent(professionCapstoneAdoptionRate)}")
        add("breakdown=${professionCapstoneBreakdown.keys.sorted().joinToString()}")
        add("reportOnlyFloors=${professionBuildIdentityReportOnlyFloorNote()}")
        add("preferredSources=${professionBuildIdentityPreferredSourceNote()}")
        if (violations.isNotEmpty()) {
            add(
                "missingPerProfessionSeen=" +
                    violations.joinToString { violation ->
                        "${violation.professionId}(${violation.seenCount}/${violation.requiredSeenCount},samples=${violation.sampleCount})"
                    },
            )
        }
    }.joinToString(separator = "; ")

internal fun professionCapstoneAdoptionFloorViolations(
    professionCapstoneBreakdown: JsonObject,
): List<ProfessionCapstoneCountFloorViolation> =
    phase4FoundationProfessionIds.mapNotNull { professionId ->
        val breakdown = professionCapstoneBreakdown[professionId]?.jsonObject
        val actualCount = breakdown?.get("adoptedCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val sampleCount = breakdown?.get("sampleCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val requiredCount = foundationBuildIdentityByProfessionId.getValue(professionId).reportOnlyFloors.adoptionMinCount
        if (actualCount < requiredCount) {
            ProfessionCapstoneCountFloorViolation(
                professionId = professionId,
                actualCount = actualCount,
                requiredCount = requiredCount,
                sampleCount = sampleCount,
            )
        } else {
            null
        }
    }

internal fun professionNonWeaponFloorViolations(
    professionCapstoneBreakdown: JsonObject,
): List<ProfessionCapstoneCountFloorViolation> =
    phase4FoundationProfessionIds.mapNotNull { professionId ->
        val breakdown = professionCapstoneBreakdown[professionId]?.jsonObject
        val actualCount = breakdown?.get("nonWeaponPayoffCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val sampleCount = breakdown?.get("sampleCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val requiredCount = foundationBuildIdentityByProfessionId.getValue(professionId).reportOnlyFloors.nonWeaponMinCount
        if (actualCount < requiredCount) {
            ProfessionCapstoneCountFloorViolation(
                professionId = professionId,
                actualCount = actualCount,
                requiredCount = requiredCount,
                sampleCount = sampleCount,
            )
        } else {
            null
        }
    }

internal fun professionCapstoneAdoptionFloorNote(
    violations: List<ProfessionCapstoneCountFloorViolation>,
): String =
    reportOnlyFloorNote("adoption", violations)

internal fun professionNonWeaponFloorNote(
    violations: List<ProfessionCapstoneCountFloorViolation>,
): String =
    reportOnlyFloorNote("nonWeapon", violations)

private fun reportOnlyFloorNote(
    label: String,
    violations: List<ProfessionCapstoneCountFloorViolation>,
): String =
    buildList {
        add("reportOnlyFloors=${professionBuildIdentityReportOnlyFloorNote()}")
        if (violations.isEmpty()) {
            add("$label=all-professions-covered")
        } else {
            add(
                "$label=" +
                    violations.joinToString { violation ->
                        "${violation.professionId}(${violation.actualCount}/${violation.requiredCount},samples=${violation.sampleCount})"
                    },
            )
        }
    }.joinToString(separator = "; ")

private fun appendReportOnlyEntries(
    evaluation: EvaluationResult,
    extraEntries: List<EvaluationEntry>,
): EvaluationResult {
    if (extraEntries.isEmpty()) {
        return evaluation
    }
    val entries = evaluation.entries + extraEntries
    return evaluation.copy(
        verdict = if (entries.any { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION }) EvaluationVerdict.FAIL else EvaluationVerdict.PASS,
        passCount = entries.count { entry -> entry.status == EvaluationEntryStatus.PASS },
        approvedDebtCount = entries.count { entry -> entry.status == EvaluationEntryStatus.APPROVED_DEBT },
        expectedFailureCount = entries.count { entry -> entry.status == EvaluationEntryStatus.EXPECTED_FAILURE },
        unexpectedRegressionCount = entries.count { entry -> entry.status == EvaluationEntryStatus.UNEXPECTED_REGRESSION },
        improvedDebtCount = entries.count { entry -> entry.status == EvaluationEntryStatus.IMPROVEMENT },
        entries = entries,
    )
}

private fun JsonObject.intValue(key: String): Int = getValue(key).jsonPrimitive.content.toInt()

private fun JsonObject.doubleValue(key: String): Double = getValue(key).jsonPrimitive.content.toDouble()

private fun requireRewardRoutingCoverageSummary(metrics: JsonObject): JsonObject =
    metrics["rewardRoutingCoverageSummary"]?.jsonObject
        ?: error("whiteBoxLoot.rewardRoutingCoverageSummary missing from artifact.")

private fun JsonObject.booleanValue(key: String): Boolean = getValue(key).jsonPrimitive.content.toBooleanStrict()

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

private fun formatDecimal(value: Double?): String =
    value?.let { current -> String.format(Locale.US, "%.1f", current) } ?: "n/a"

private fun formatRequiredDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun formatSignedPercent(value: Double): String =
    if (value >= 0.0) {
        "+${formatPercentPrecise(value)}"
    } else {
        String.format(Locale.US, "%.2f%%", value * 100.0)
    }
