package com.ktome.tools.phase4

import com.ktome.game.data.DataLoader
import com.ktome.tools.mapgen.WhiteBoxSolvabilityFailLane
import com.ktome.tools.mapgen.WhiteBoxSolvabilitySuccessLane
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.json.JsonArray
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

internal data class Phase4TaskAggregate(
    val taskId: String,
    val status: String,
    val sourcePath: String,
    val buildId: String? = null,
    val locale: String? = null,
    val metrics: JsonObject,
)

private val longRunItemSemanticTagsById: Map<String, List<String>> by lazy {
    DataLoader()
        .loadItemBundle()
        .baseItems
        .associate { item -> item.id to item.tags.sorted() }
}

private val contentPackArtifactFreshnessTolerance: Duration = Duration.ofMinutes(1)
private const val WHITEBOX_SOLVABILITY_SUCCESS_CORPUS_GROUP_ID: String = "${WhiteBoxSolvabilitySuccessLane.LANE_ID}:corpus"
private const val WHITEBOX_SOLVABILITY_FAIL_CORPUS_GROUP_ID: String = "${WhiteBoxSolvabilityFailLane.LANE_ID}:corpus"

internal object Phase4DomainArtifactRegistry {
    private val taskReadersById: Map<String, (repoRoot: Path, sourcePath: Path, payload: JsonObject) -> Phase4TaskAggregate> =
        linkedMapOf(
            "mapgenSmoke" to ::readMapgenSmoke,
            "solvabilityHarness" to ::readSolvabilityHarness,
            "hiddenContentHarness" to ::readHiddenContentHarness,
            "organicHiddenProbe" to ::readOrganicHiddenProbe,
            "contentPackHarness" to ::readContentPackHarness,
            "bossHarness" to ::readBossHarness,
            "longRunLab" to ::readLongRunLabFull,
            "terrainInteractionBatch" to ::readTerrainInteractionBatch,
            "whiteBoxMapgen" to ::readWhiteBoxMapgen,
            "whiteBoxSolvability" to ::readWhiteBoxSolvability,
            "lootBalanceLab" to ::readLootBalanceLab,
            "whiteBoxLoot" to ::readWhiteBoxLoot,
            "whiteBoxHiddenContent" to ::readWhiteBoxHiddenContent,
            "whiteBoxContentPack" to ::readWhiteBoxContentPack,
        )

    fun collectTaskAggregates(repoRoot: Path = repoRoot()): List<Phase4TaskAggregate> =
        Phase4AggregationManifestRuntime.tasks().map { task ->
            val reader =
                checkNotNull(taskReadersById[task.taskId]) {
                    "Missing Phase 4 artifact reader for ${task.taskId}."
                }
            val sourcePath = repoRoot.resolve(task.artifactRelativePath)
            val payload = readPhase4Json(sourcePath)
            val aggregate = reader(repoRoot, sourcePath, payload)
            check(aggregate.taskId == task.taskId) {
                "Phase 4 manifest entry ${task.taskId} returned ${aggregate.taskId} from ${task.artifactRelativePath}."
            }
            aggregate
        }

    fun registeredTaskIds(): Set<String> = Phase4AggregationManifestRuntime.taskIdsInOrder().toCollection(linkedSetOf())

    fun aggregationOnlyTaskIds(): Set<String> = Phase4AggregationManifestRuntime.aggregationOnlyTaskIds()

    fun readerTaskIds(): Set<String> = taskReadersById.keys

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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
                    put("frontstageHighPriorityCueRetainedRate", summary.getValue("frontstageHighPriorityCueRetainedRate"))
                    put("frontstageCueDedupAppliedCount", summary.getValue("frontstageCueDedupAppliedCount"))
                    put("frontstageCueExpiryParity", summary.getValue("frontstageCueExpiryParity"))
                    put("frontstageSecretCueVisibilityRate", summary.getValue("frontstageSecretCueVisibilityRate"))
                    put("frontstageHighPriorityCueRetainedCount", summary.getValue("frontstageHighPriorityCueRetainedCount"))
                    put("frontstageHighPriorityExpectedCount", summary.getValue("frontstageHighPriorityExpectedCount"))
                    put("frontstageSecretCueVisibleCount", summary.getValue("frontstageSecretCueVisibleCount"))
                    put("frontstageSecretCueExpectedCount", summary.getValue("frontstageSecretCueExpectedCount"))
                    put("frontstageDuplicateNoTargetLogCount", summary.getValue("frontstageDuplicateNoTargetLogCount"))
                    put("frontstageRemainingNoTargetCueCount", summary.getValue("frontstageRemainingNoTargetCueCount"))
                    put("frontstageCueExpiryProbePassedCount", summary.getValue("frontstageCueExpiryProbePassedCount"))
                    put("frontstageCueExpiryProbeTotalCount", summary.getValue("frontstageCueExpiryProbeTotalCount"))
                    put("frontstageCueExpiryProbePriorities", summary.getValue("frontstageCueExpiryProbePriorities"))
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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
                    put("leadDiscoveryCount", summary.intValue("leadDiscoveryCount"))
                    put("leadDiscoveryRate", summary.doubleValue("leadDiscoveryRate"))
                    put("secretConversionCount", summary.intValue("secretConversionCount"))
                    put("secretConversionRate", summary.doubleValue("secretConversionRate"))
                    put("secretZoneEntryCount", summary.intValue("secretZoneEntryCount"))
                    put("secretZoneEntryRate", summary.doubleValue("secretZoneEntryRate"))
                    put("averageFirstHiddenDiscoveryTurn", summary.getValue("averageFirstHiddenDiscoveryTurn"))
                    put("averageFirstSecretZoneEntryTurn", summary.getValue("averageFirstSecretZoneEntryTurn"))
                    put("firstHiddenDiscoveryTurnP50", summary.getValue("firstHiddenDiscoveryTurnP50"))
                    put("firstHiddenDiscoveryTurnP90", summary.getValue("firstHiddenDiscoveryTurnP90"))
                    put("firstSecretZoneEntryTurnP50", summary.getValue("firstSecretZoneEntryTurnP50"))
                    put("firstSecretZoneEntryTurnP90", summary.getValue("firstSecretZoneEntryTurnP90"))
                    put("professionIds", summary.getValue("professionIds"))
                    put("raceIds", summary.getValue("raceIds"))
                    put("comboCount", summary.intValue("comboCount"))
                    put("seedsPerZoneCombo", summary.intValue("seedsPerZoneCombo"))
                    put("searchPromptRequired", summary.getValue("searchPromptRequired"))
                    put("reactiveSearchOnly", summary.getValue("reactiveSearchOnly"))
                    put("perZoneSecretEntryMinRate", summary.getValue("perZoneSecretEntryMinRate"))
                    put("failingSecretEntryZoneIds", summary.getValue("failingSecretEntryZoneIds"))
                    put("probeBotId", summary.getValue("probeBotId"))
                    put("probeTurnBudget", summary.intValue("probeTurnBudget"))
                    put("probeMaxFloor", summary.intValue("probeMaxFloor"))
                    put("zones", payload.getValue("zones"))
                    put("combinations", payload.getValue("combinations"))
                    put("zoneDiscoveryDistribution", payload.getValue("zoneDiscoveryDistribution"))
                    put("secretZoneDiscoveryDistribution", payload.getValue("secretZoneDiscoveryDistribution"))
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
        val distinctTemplateCount = reports.mapNotNull { element -> element.jsonObject["templateId"]?.jsonPrimitive?.content }.distinct().size
        val variantCount = reports.count { element -> "variantId" in element.jsonObject }
        val phaseGraphStructuralDiffCount = pairReports.sumOf { element -> element.jsonObject.intValue("phaseGraphStructuralDiffCount") }
        return Phase4TaskAggregate(
            taskId = "bossHarness",
            status = if (failureCount == 0 && whiteBoxFailedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = whiteBoxHeader.optionalStringValue("buildId"),
            locale = whiteBoxHeader.optionalStringValue("locale"),
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
                    put("phaseTransitionObservedRatio", corpusMetrics.getValue("phaseTransitionObservedRatio"))
                    put("variantTraceDivergenceRatio", corpusMetrics.getValue("variantTraceDivergenceRatio"))
                    put("minVariantActionTraceDivergenceScore", corpusMetrics.getValue("minVariantActionTraceDivergenceScore"))
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
                    put("bossVariantBasePhaseCountMin", corpusMetrics.getValue("bossVariantBasePhaseCountMin"))
                    put("terrainPreferenceVariantCount", corpusMetrics.getValue("terrainPreferenceVariantCount"))
                    put("terrainPreferenceAvailableVariantCount", corpusMetrics.getValue("terrainPreferenceAvailableVariantCount"))
                    put("terrainPreferenceImplementedCount", corpusMetrics.getValue("terrainPreferenceImplementedCount"))
                    put("terrainPreferenceImplementedRate", corpusMetrics.getValue("terrainPreferenceImplementedRate"))
                    put("mutationTierDistribution", corpusMetrics.getValue("mutationTierDistribution"))
                    put("bossVariantMutationSets", corpusMetrics.getValue("bossVariantMutationSets"))
                    put("bossVariantPreferredTerrainTags", corpusMetrics.getValue("bossVariantPreferredTerrainTags"))
                    put("bossVariantBasePhaseCounts", corpusMetrics.getValue("bossVariantBasePhaseCounts"))
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
        val professionCapstoneSeenRate = payload.requireArtifactMetric("longRunLab", "professionCapstoneSeenRate")
        val professionCapstoneAdoptionRate = payload.requireArtifactMetric("longRunLab", "professionCapstoneAdoptionRate")
        val nonWeaponBuildPayoffRate = payload.requireArtifactMetric("longRunLab", "nonWeaponBuildPayoffRate")
        val professionCapstoneBreakdown = payload.requireArtifactMetric("longRunLab", "professionCapstoneBreakdown")
        val starterProfessionTalentMaxCount = payload.requireArtifactMetric("longRunLab", "starterProfessionTalentMaxCount")
        val learnedTalentChoiceEventRate = payload.requireArtifactMetric("longRunLab", "learnedTalentChoiceEventRate")
        val multiTreeInvestmentAboveThresholdRate = payload.requireArtifactMetric("longRunLab", "multiTreeInvestmentAboveThresholdRate")
        val breakpointChoiceEventRate = payload.requireArtifactMetric("longRunLab", "breakpointChoiceEventRate")
        val starterInscriptionMaxCount = payload.requireArtifactMetric("longRunLab", "starterInscriptionMaxCount")
        val fullSlotInscriptionPurchaseBlockedWithoutReplacementCount =
            payload.requireArtifactMetric("longRunLab", "fullSlotInscriptionPurchaseBlockedWithoutReplacementCount")
        val inscriptionInstallOrReplaceRate = payload.requireArtifactMetric("longRunLab", "inscriptionInstallOrReplaceRate")
        val inscriptionReplacementProbeSuccessCount = payload.requireArtifactMetric("longRunLab", "inscriptionReplacementProbeSuccessCount")
        val inscriptionReplacementProbe = payload.requireArtifactMetric("longRunLab", "inscriptionReplacementProbe")
        val terminalInscriptionLoadoutDiversity = payload.requireArtifactMetric("longRunLab", "terminalInscriptionLoadoutDiversity")
        val inscriptionCategoryCountDistribution = payload.requireArtifactMetric("longRunLab", "inscriptionCategoryCountDistribution")
        val shopInscriptionOfferConversionRate = payload.requireArtifactMetric("longRunLab", "shopInscriptionOfferConversionRate")
        val inscriptionReplaceReasonDistribution = payload.requireArtifactMetric("longRunLab", "inscriptionReplaceReasonDistribution")
        val professionTopWeaponBaseIds =
            payload["professionTopWeaponBaseIds"] ?: deriveProfessionTopWeaponBaseIds(professionTerminalWeaponDistribution.jsonObject)
        val professionTopWeaponSemanticTags =
            payload["professionTopWeaponSemanticTags"]
                ?: deriveProfessionTopWeaponSemanticTags(professionTopWeaponBaseIds.jsonObject)
        val fullRouteZoneTraversalDiagnostics = payload.getValue("fullRouteZoneTraversalDiagnostics")
        val criticalPathZoneIds = payload.getValue("criticalPathZoneIds")
        val criticalPathZoneDesignAudit = payload.getValue("criticalPathZoneDesignAudit")
        return Phase4TaskAggregate(
            taskId = "longRunLab",
            status = if (fullRouteCount > 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = payload["buildId"]?.jsonPrimitive?.content,
            locale = payload["localeId"]?.jsonPrimitive?.content,
            metrics =
                buildJsonObject {
                    put("scenarioCount", payload.intValue("scenarioCount"))
                    put("fullRouteCount", fullRouteCount)
                    put("branchInclusiveCount", payload.intValue("branchInclusiveCount"))
                    put("terminalWeaponBaseDiversity", payload.intValue("terminalWeaponBaseDiversity"))
                    put("crossProfessionTopWeaponDominance", payload.doubleValue("crossProfessionTopWeaponDominance"))
                    put("professionAlignedWeaponAdoptionRate", payload.doubleValue("professionAlignedWeaponAdoptionRate"))
                    put("professionCapstoneSeenRate", professionCapstoneSeenRate)
                    put("professionCapstoneAdoptionRate", professionCapstoneAdoptionRate)
                    put("nonWeaponBuildPayoffRate", nonWeaponBuildPayoffRate)
                    put("starterProfessionTalentMaxCount", starterProfessionTalentMaxCount)
                    put("learnedTalentChoiceEventRate", learnedTalentChoiceEventRate)
                    put("multiTreeInvestmentAboveThresholdRate", multiTreeInvestmentAboveThresholdRate)
                    put("breakpointChoiceEventRate", breakpointChoiceEventRate)
                    put("talentTreePrimaryInvestmentDistribution", payload.getValue("talentTreePrimaryInvestmentDistribution"))
                    put("talentReserveSwapCount", payload.intValue("talentReserveSwapCount"))
                    put("rankBreakpointAdoptionByTalent", payload.getValue("rankBreakpointAdoptionByTalent"))
                    put("autoLearnedNonStarterTalentCount", payload.intValue("autoLearnedNonStarterTalentCount"))
                    put("starterInscriptionMaxCount", starterInscriptionMaxCount)
                    put("fullSlotInscriptionPurchaseBlockedWithoutReplacementCount", fullSlotInscriptionPurchaseBlockedWithoutReplacementCount)
                    put("inscriptionInstallOrReplaceRate", inscriptionInstallOrReplaceRate)
                    put("inscriptionReplacementProbeSuccessCount", inscriptionReplacementProbeSuccessCount)
                    put("inscriptionReplacementProbe", inscriptionReplacementProbe)
                    put("terminalInscriptionLoadoutDiversity", terminalInscriptionLoadoutDiversity)
                    put("inscriptionCategoryCountDistribution", inscriptionCategoryCountDistribution)
                    put("shopInscriptionOfferConversionRate", shopInscriptionOfferConversionRate)
                    put("inscriptionReplaceReasonDistribution", inscriptionReplaceReasonDistribution)
                    put("inscriptionPurchaseCancelledAfterReplacementPrompt", payload.intValue("inscriptionPurchaseCancelledAfterReplacementPrompt"))
                    put("shopPurchaseDeniedInsufficientGoldCount", payload.intValue("shopPurchaseDeniedInsufficientGoldCount"))
                    put("includedProfessions", payload.getValue("includedProfessions"))
                    put("advancedReportOnlyProfessions", payload.getValue("advancedReportOnlyProfessions"))
                    put("excludedFrozenProfessions", payload.getValue("excludedFrozenProfessions"))
                    put("alignedFullRouteSampleCount", payload.intValue("alignedFullRouteSampleCount"))
                    put("crossProfessionTopWeaponCount", payload.intValue("crossProfessionTopWeaponCount"))
                    payload["crossProfessionTopWeaponBaseId"]?.let { topWeaponBaseId -> put("crossProfessionTopWeaponBaseId", topWeaponBaseId) }
                    put("professionTerminalWeaponDistribution", professionTerminalWeaponDistribution)
                    put("professionTopWeaponBaseIds", professionTopWeaponBaseIds)
                    put("professionTopWeaponSemanticTags", professionTopWeaponSemanticTags)
                    put("professionCapstoneBreakdown", professionCapstoneBreakdown)
                    put("fullRouteZoneTraversalDiagnostics", fullRouteZoneTraversalDiagnostics)
                    put("criticalPathZoneIds", criticalPathZoneIds)
                    put("criticalPathZoneDesignAudit", criticalPathZoneDesignAudit)
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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
        val aggregates = payload.getValue("aggregates").jsonArray.map { aggregate -> aggregate.jsonObject }
        val failedAssertions = summary.intValue("failedAssertions")
        val successLane =
            checkNotNull(aggregates.firstOrNull { aggregate -> aggregate.getValue("groupId").jsonPrimitive.content == WHITEBOX_SOLVABILITY_SUCCESS_CORPUS_GROUP_ID }) {
                "whiteBoxSolvability summary must include $WHITEBOX_SOLVABILITY_SUCCESS_CORPUS_GROUP_ID aggregate."
            }
        val failLane =
            checkNotNull(aggregates.firstOrNull { aggregate -> aggregate.getValue("groupId").jsonPrimitive.content == WHITEBOX_SOLVABILITY_FAIL_CORPUS_GROUP_ID }) {
                "whiteBoxSolvability summary must include $WHITEBOX_SOLVABILITY_FAIL_CORPUS_GROUP_ID aggregate."
            }
        return Phase4TaskAggregate(
            taskId = "whiteBoxSolvability",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                    val successMetrics = successLane.getValue("metrics").jsonObject
                    put("revealSuccessCaseCount", successLane.intValue("sampleCount"))
                    put("revealSuccessCasesWithReveal", successMetrics.intValue("casesWithReveal"))
                    put("revealSuccessCasesWithBacktrackProof", successMetrics.intValue("casesWithBacktrackProof"))
                    val failMetrics = failLane.getValue("metrics").jsonObject
                    put("revealFailCaseCount", failLane.intValue("sampleCount"))
                    put("revealFailCasesWithFail", failMetrics.intValue("casesWithFail"))
                    put("revealFailTaxonomy", failMetrics.getValue("failStateTaxonomy"))
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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
        requireContentPackArtifactsSemanticallyAligned(
            primaryPath = sourcePath,
            primaryPayload = payload,
            secondaryPath = whiteBoxSourcePath,
            secondaryPayload = whiteBoxPayload,
        )
        val whiteBoxSummary = whiteBoxPayload.getValue("summary").jsonObject
        val whiteBoxFailedAssertions = whiteBoxSummary.intValue("failedAssertions")
        val failureCount = summary.intValue("failureCount")
        val whiteBoxCorpusMetrics = aggregateMetrics(whiteBoxPayload, "corpus")
        return Phase4TaskAggregate(
            taskId = "contentPackHarness",
            status = if (failureCount == 0 && whiteBoxFailedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
                    put("strictLocalIdentityViolationCount", corpusMetrics.getValue("strictLocalIdentityViolationCount"))
                    put("strictLocalIdentityViolations", corpusMetrics.getValue("strictLocalIdentityViolations"))
                    put("secretZoneRewardAuthorityViolationCount", corpusMetrics.getValue("secretZoneRewardAuthorityViolationCount"))
                    put("secretZoneRewardAuthorityViolations", corpusMetrics.getValue("secretZoneRewardAuthorityViolations"))
                    put("secretProfileIdentitySummaries", validatedSecretProfileIdentitySummaries(corpusMetrics.getValue("secretProfileIdentitySummaries").jsonArray))
                    put("dynamicPoolCoverage", corpusMetrics.requireArtifactMetric("whiteBoxLoot", "dynamicPoolCoverage"))
                    putJsonArray("dynamicPoolTargetProfiles") {
                        corpusMetrics.requireArtifactMetric("whiteBoxLoot", "dynamicPoolTargetProfiles").jsonArray.forEach { targetProfile ->
                            add(targetProfile)
                        }
                    }
                    put("affixPassiveCoverage", corpusMetrics.getValue("affixPassiveCoverage"))
                    put("affixPassiveKinds", corpusMetrics.getValue("affixPassiveKinds"))
                    put("uniqueArtifactOutcomeCount", corpusMetrics.getValue("uniqueArtifactOutcomeCount"))
                    put("meaningfulUniqueArtifactSwapCount", corpusMetrics.getValue("meaningfulUniqueArtifactSwapCount"))
                    put("uniqueArtifactMeaningfulSwapRate", corpusMetrics.getValue("uniqueArtifactMeaningfulSwapRate"))
                    put("specialTierPassiveFamilyDuplicateSummary", corpusMetrics.requireArtifactMetric("whiteBoxLoot", "specialTierPassiveFamilyDuplicateSummary"))
                    put("specialTierPassiveFamilyDuplicateCount", corpusMetrics.requireArtifactMetric("whiteBoxLoot", "specialTierPassiveFamilyDuplicateCount"))
                    put("rewardRoutingCoverageSummary", corpusMetrics.requireArtifactMetric("whiteBoxLoot", "rewardRoutingCoverageSummary"))
                },
        )
    }

    private fun validatedSecretProfileIdentitySummaries(summaries: JsonArray): JsonArray {
        summaries.forEach { element ->
            val summary = element.jsonObject
            check(summary.containsKey("canonicalZoneId")) {
                "whiteBoxLoot.secretProfileIdentitySummaries must expose canonicalZoneId on the canonical aggregate path."
            }
            check(!summary.containsKey("zoneId")) {
                "whiteBoxLoot.secretProfileIdentitySummaries must not retain legacy zoneId on the canonical aggregate path."
            }
        }
        return summaries
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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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
            buildId = header.optionalStringValue("buildId"),
            locale = header.optionalStringValue("locale"),
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

private fun aggregateMetrics(
    payload: JsonObject,
    groupId: String,
): JsonObject =
    payload.getValue("aggregates").jsonArray
        .first { aggregate -> aggregate.jsonObject.getValue("groupId").jsonPrimitive.content == groupId }
        .jsonObject
        .getValue("metrics")
        .jsonObject

private fun JsonObject.requireArtifactMetric(
    taskId: String,
    key: String,
): JsonElement =
    get(key)
        ?: error("Phase4 canonical aggregate requires $taskId.$key in the producer artifact; fix the source report instead of recomputing it locally.")

internal fun requireContentPackArtifactsSemanticallyAligned(
    primaryPath: Path,
    primaryPayload: JsonObject,
    secondaryPath: Path,
    secondaryPayload: JsonObject,
) {
    val primaryBuildId = reportBuildId(primaryPayload)
    val secondaryBuildId = reportBuildId(secondaryPayload)
    check(primaryBuildId == secondaryBuildId) {
        "Mismatched content-pack artifact buildIds: $primaryPath ($primaryBuildId) vs $secondaryPath ($secondaryBuildId)."
    }
    val primaryTimestamp = Instant.parse(reportTimestamp(primaryPayload))
    val secondaryTimestamp = Instant.parse(reportTimestamp(secondaryPayload))
    val freshnessDelta = Duration.between(primaryTimestamp, secondaryTimestamp).abs()
    check(freshnessDelta <= contentPackArtifactFreshnessTolerance) {
        "Mismatched content-pack artifact freshness: $primaryPath ($primaryTimestamp) vs $secondaryPath ($secondaryTimestamp), " +
            "delta=${freshnessDelta.seconds}s exceeds ${contentPackArtifactFreshnessTolerance.seconds}s. " +
            "Fix the producer pair instead of mixing stale and fresh content-pack artifacts."
    }
    val primarySignature = contentPackArtifactSemanticSignature(primaryPayload)
    val secondarySignature = contentPackArtifactSemanticSignature(secondaryPayload)
    check(primarySignature == secondarySignature) {
        "Misaligned content-pack artifacts: $primaryPath ($primaryBuildId) vs $secondaryPath ($secondaryBuildId). " +
            "primarySignature=$primarySignature secondarySignature=$secondarySignature"
    }
}

private fun reportBuildId(payload: JsonObject): String = payload.getValue("header").jsonObject.stringValue("buildId")

private fun reportTimestamp(payload: JsonObject): String = payload.getValue("header").jsonObject.stringValue("timestamp")

internal fun contentPackArtifactSemanticSignature(payload: JsonObject): JsonObject {
    val header = payload.getValue("header").jsonObject
    return buildJsonObject {
        put("phaseId", JsonPrimitive(header.stringValue("phaseId")))
        header.optionalStringValue("locale")?.let { locale -> put("locale", JsonPrimitive(locale)) }
        putJsonArray("activePackIds") {
            header.getValue("activePackIds").jsonArray
                .map { value -> value.jsonPrimitive.content }
                .sorted()
                .forEach { packId -> add(JsonPrimitive(packId)) }
        }
        putJsonObject("activePackManifestVersions") {
            header.getValue("activePackManifestVersions").jsonObject.entries
                .sortedBy(Map.Entry<String, JsonElement>::key)
                .forEach { (packId, manifestVersion) -> put(packId, JsonPrimitive(manifestVersion.jsonPrimitive.content)) }
        }
        putJsonArray("seedList") {
            header.getValue("seedList").jsonArray
                .map { value -> value.jsonPrimitive.content }
                .sorted()
                .forEach { seed -> add(JsonPrimitive(seed)) }
        }
        put("contractVersions", contentPackContractVersionSignature(header))
    }
}

internal fun contentPackContractVersionSignature(header: JsonObject): JsonObject =
    if (header.containsKey("contractVersions")) {
        buildJsonObject {
            header.getValue("contractVersions").jsonArray
                .map { entry ->
                    entry.jsonObject.getValue("contractId").jsonPrimitive.content to
                        entry.jsonObject.getValue("version").jsonPrimitive.content
                }.sortedBy(Pair<String, String>::first)
                .forEach { (contractId, version) -> put(contractId, JsonPrimitive(version)) }
        }
    } else {
        buildJsonObject {
            put("contentSchema", JsonPrimitive(header.stringValue("contentSchemaVersion")))
            put("overlayContract", JsonPrimitive(header.stringValue("overlayContractVersion")))
            put("lootFormula", JsonPrimitive(header.stringValue("lootFormulaVersion")))
            put("rewardLedger", JsonPrimitive(header.stringValue("rewardLedgerVersion")))
            put("searchRule", JsonPrimitive(header.stringValue("searchRuleVersion")))
            put("secretRule", JsonPrimitive(header.stringValue("secretRuleVersion")))
            put("specialTierEligibility", JsonPrimitive(header.stringValue("specialTierEligibilityVersion")))
            put("topologyFingerprint", JsonPrimitive(header.stringValue("topologyFingerprintVersion")))
        }
    }

private fun relativize(
    repoRoot: Path,
    path: Path,
): String = repoRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/')

private fun repoRoot(): Path =
    System.getProperty("ktome.repo.root")
        ?.let(Path::of)
        ?: Path.of("").toAbsolutePath().normalize()

private fun JsonObject.intValue(key: String): Int = getValue(key).jsonPrimitive.content.toInt()

private fun JsonObject.doubleValue(key: String): Double = getValue(key).jsonPrimitive.content.toDouble()

private fun JsonObject.stringValue(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.optionalStringValue(key: String): String? = get(key)?.jsonPrimitive?.content
