package com.ktome.game.harness

import com.ktome.core.item.EquipSlot
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.loot.RarityTier
import com.ktome.core.profile.MilestoneRewardSummary
import com.ktome.core.run.RunOutcome
import com.ktome.core.save.SaveManager
import com.ktome.game.FOUNDATION_SYNERGY_AFFIX_IDS
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.ZoneMechanicRuntime
import com.ktome.game.data.DataLoader
import com.ktome.game.loot.MilestoneRewardScoreSample
import com.ktome.game.loot.MilestoneRewardSlotFamily
import com.ktome.game.loot.foundationBuildIdentityByProfessionId
import com.ktome.game.loot.foundationProfessionCapstoneBaseIdsByProfessionId
import com.ktome.game.loot.milestoneRewardSlotFamily
import com.ktome.game.validation.ValidationAction
import com.ktome.game.validation.ValidationScenarioActionId
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationScenarioRegistry
import com.ktome.game.validation.ValidationSessionRequest
import java.nio.file.Path
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LongRunLabFullTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    fun `full long run lab separates full route gate from branch inclusive probes`() {
        val fullRouteSpecs =
            LongRunLabSeedBank.pr06FullRouteSpecs(
                corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
                initialTalentPointGrant = PROFESSION_TREE_CHOICE_PROBE_TALENT_POINTS,
            )
        val branchInclusiveSpecs =
            LongRunLabSeedBank.pr06BranchInclusiveSpecs(
                corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
                initialTalentPointGrant = PROFESSION_TREE_CHOICE_PROBE_TALENT_POINTS,
            )
        val routeProbeSpecs = LongRunLabSeedBank.pr06RouteProbeSpecs(corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID)
        val lateRouteProbeSpecs = LongRunLabSeedBank.pr06LateRouteProbeSpecs(corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID)
        val kernelExecution =
            LongRunKernelCache.execute(
                rootDir = tempDir,
                specs = fullRouteSpecs + branchInclusiveSpecs + routeProbeSpecs + lateRouteProbeSpecs,
            )
        val reportsByName = kernelExecution.reports.associateBy(ScenarioReport::name)
        val fullRouteReports = fullRouteSpecs.map { spec -> requireNotNull(reportsByName[spec.name]) }
        val branchInclusiveReports = branchInclusiveSpecs.map { spec -> requireNotNull(reportsByName[spec.name]) }
        val routeProbeReportsBySpec = routeProbeSpecs.map { spec -> requireNotNull(reportsByName[spec.name]) }
        val lateRouteProbeReportsBySpec = lateRouteProbeSpecs.map { spec -> requireNotNull(reportsByName[spec.name]) }
        val reports = fullRouteReports + branchInclusiveReports + routeProbeReportsBySpec + lateRouteProbeReportsBySpec
        val routeProbeReports = reports.filter { report -> report.scenarioType == ScenarioType.ROUTE_PROBE }
        val lateRouteProbeReports = reports.filter { report -> report.scenarioType == ScenarioType.LATE_ROUTE_PROBE }
        val fullRouteNonVictoryReports = fullRouteReports.filter { report -> report.outcome !is RunOutcome.Victory }
        val reachedTempleCount = fullRouteReports.count { report -> zoneDepth(report.finalZoneId) >= zoneDepth("abyssal_temple") }
        val failuresAfterDeepIron =
            fullRouteNonVictoryReports.count { report -> zoneDepth(report.finalZoneId) > zoneDepth("deep_iron_pit") }
        val afterDeepIronRatio =
            if (fullRouteNonVictoryReports.isEmpty()) {
                null
            } else {
                failuresAfterDeepIron.toDouble() / fullRouteNonVictoryReports.size.toDouble()
            }
        val averageTurns = if (reports.isEmpty()) 0.0 else reports.map(ScenarioReport::turns).average()
        val averageHeadlessTurns = if (reports.isEmpty()) 0.0 else reports.map(ScenarioReport::headlessTurnEquivalent).average()
        val branchSampleCount = branchInclusiveReports.size
        val deathDistribution = fullRouteNonVictoryReports.groupingBy(ScenarioReport::finalZoneId).eachCount().toSortedMap()
        val terminalRouteReports =
            reports.filterNot { report ->
                report.scenarioType == ScenarioType.ROUTE_PROBE ||
                    report.scenarioType == ScenarioType.LATE_ROUTE_PROBE
            }
        val routeHashDistribution = terminalRouteReports.groupingBy(ScenarioReport::zoneRouteHash).eachCount().toSortedMap()
        val branchRouteHashDistribution = branchInclusiveReports.groupingBy(ScenarioReport::zoneRouteHash).eachCount().toSortedMap()
        val scenarioTypeDistribution = scenarioTypeDistribution(reports, includeZeroCounts = true)
        val routeDiversity = zoneRouteHashDiversity(reports)
        val routeTokenSample = terminalRouteReports.map(ScenarioReport::routeToken).distinct().sorted().take(8)
        val fullRouteTokenSample = fullRouteReports.map(ScenarioReport::routeToken).distinct().sorted()
        val branchRouteTokenSample = branchInclusiveReports.map(ScenarioReport::routeToken).distinct().sorted()
        val probeRouteHashSample = routeDiversity.probeRouteHashSample
        val milestoneRewards = reports.flatMap(ScenarioReport::milestoneRewards)
        val allScenarioBreakpointMetrics = breakpointMetrics(reports)
        val fullRouteBreakpointMetrics = breakpointMetrics(fullRouteReports)
        val cadenceRewardCount = reports.sumOf(ScenarioReport::cadenceRewardCount)
        val shopRefreshPurchaseCount = reports.sumOf(ScenarioReport::shopRefreshPurchaseCount)
        val lateRunReliquaryPurchaseCount = reports.sumOf(ScenarioReport::lateRunReliquaryPurchaseCount)
        val lateRunReliquaryVisitCount = reports.sumOf(ScenarioReport::lateRunReliquaryVisitCount)
        val lateRunReliquaryRefreshCount = reports.sumOf(ScenarioReport::lateRunReliquaryRefreshCount)
        val lateRunReliquaryItemPurchaseCount = reports.sumOf(ScenarioReport::lateRunReliquaryItemPurchaseCount)
        val lateRunReliquaryNonMandatoryPurchaseCount = reports.sumOf(ScenarioReport::lateRunReliquaryNonMandatoryPurchaseCount)
        val lateRunReliquaryShardSpent = reports.sumOf(ScenarioReport::lateRunReliquaryShardSpent)
        val lateRunReliquaryTagDistribution = aggregateReliquaryTagDistribution(reports)
        val allScenarioAffixSynergyMetrics = affixSynergyMetrics(reports)
        val fullRouteAffixSynergyMetrics = affixSynergyMetrics(fullRouteReports)
        val allScenarioSynergyRewardMetrics = synergyRewardMetrics(reports)
        val fullRouteSynergyRewardMetrics = synergyRewardMetrics(fullRouteReports)
        val terminalWeaponIdentity = terminalWeaponIdentitySummary(fullRouteReports)
        val fullRouteZoneTraversalDiagnostics = aggregateZoneTraversalDiagnostics(fullRouteReports)
        val criticalPathZoneIds = criticalPathZoneIds()
        val criticalPathZoneDesignAudit = criticalPathZoneDesignAudit()
        val milestoneRewardQualityDistribution = milestoneRewards.groupingBy { it.qualityTier.name }.eachCount().toSortedMap()
        val milestoneAffixCountDistribution = milestoneRewards.groupingBy { it.affixIds.size.toString() }.eachCount().toSortedMap()
        val milestoneRewardAdoptionDistribution =
            milestoneRewards
                .groupingBy { reward -> if (reward.adoptedDuringRun) "adopted" else "notAdopted" }
                .eachCount()
                .toSortedMap()
        val milestoneRewardAdoptionDelta =
            milestoneRewardAdoptionDistribution.getOrDefault("adopted", 0) -
                milestoneRewardAdoptionDistribution.getOrDefault("notAdopted", 0)
        val milestoneRewardSlotDistribution = milestoneRewards.groupingBy { it.equipSlot.name }.eachCount().toSortedMap()
        val milestoneRewardSlotBalance = milestoneRewardSlotBalance(milestoneRewards)
        val rewardScoreBreakdownSamples = rewardScoreBreakdownSamples(fullRouteReports)
        assertRewardScoreBreakdownSamplesCoverPr03(rewardScoreBreakdownSamples)
        val routeRewardAffixUsageSummary =
            milestoneRewards
                .filter { reward -> reward.rewardSource == MilestoneRewardSource.ROUTE }
                .flatMap { reward -> reward.affixIds.ifEmpty { listOf("none") } }
                .groupingBy { it }
                .eachCount()
                .toSortedMap()
        val professionCapstoneSummary = professionCapstoneSummary(fullRouteReports)
        val capstoneAdoptionBySlot = capstoneAdoptionBySlot(fullRouteReports)
        val professionTerminalIdentityItemIds = professionTerminalIdentityItemIds(fullRouteReports)
        val wrongProfessionCapstoneAdoptionCount = wrongProfessionCapstoneAdoptionCount(fullRouteReports)
        val professionTreeChoiceMetrics = professionTreeChoiceMetrics(fullRouteReports)
        val inscriptionReplacementProbe = inscriptionReplacementProbe()
        val inscriptionShopMetrics = inscriptionShopMetrics(fullRouteReports, inscriptionReplacementProbe)
        val reportPath = HarnessReportWriter.reportDir().resolve("long-run-full.json")
        val failingReports = reports.filterNot(ScenarioReport::success)

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "long-run-full",
            payload =
                buildJsonObject {
                    put("sliceId", "phase3-pr15-long-run-full-v1")
                    put("buildId", HarnessMetadata.BUILD_ID)
                    put("phaseId", HarnessMetadata.PHASE_ID)
                    put("rulesetVersion", HarnessMetadata.RULESET_VERSION)
                    put("traceSchemaVersion", HarnessMetadata.TRACE_SCHEMA_VERSION)
                    put("corpusId", HarnessMetadata.LONG_RUN_FULL_CORPUS_ID)
                    put("profileId", HarnessMetadata.PROFILE_ID)
                    put("kernelCache", kernelExecution.cacheMetadata(Path.of(System.getProperty("ktome.repo.root", "."))))
                    put("localeId", reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed")
                    put("scenarioCount", reports.size)
                    put("fullRouteCount", fullRouteReports.size)
                    put("branchInclusiveCount", branchInclusiveReports.size)
                    put("routeProbeCount", routeProbeReports.size)
                    put("lateRouteProbeCount", lateRouteProbeReports.size)
                    put("branchSampleCount", branchSampleCount)
                    put("reachedTempleCount", reachedTempleCount)
                    put("nonVictoryCount", fullRouteNonVictoryReports.size)
                    put("failuresAfterDeepIron", failuresAfterDeepIron)
                    if (afterDeepIronRatio == null) {
                        put("afterDeepIronRatio", "N/A")
                    } else {
                        put("afterDeepIronRatio", afterDeepIronRatio)
                    }
                    put("averageTurns", averageTurns)
                    put("averageHeadlessTurns", averageHeadlessTurns)
                    put("cadenceRewardCount", cadenceRewardCount)
                    put("shopRefreshPurchaseCount", shopRefreshPurchaseCount)
                    put("lateRunReliquaryPurchaseCount", lateRunReliquaryPurchaseCount)
                    put("lateRunReliquaryVisitCount", lateRunReliquaryVisitCount)
                    put("lateRunReliquaryRefreshCount", lateRunReliquaryRefreshCount)
                    put("lateRunReliquaryItemPurchaseCount", lateRunReliquaryItemPurchaseCount)
                    put("lateRunReliquaryNonMandatoryPurchaseCount", lateRunReliquaryNonMandatoryPurchaseCount)
                    put("lateRunReliquaryShardSpent", lateRunReliquaryShardSpent)
                    putJsonObject("lateRunReliquaryTagDistribution") {
                        lateRunReliquaryTagDistribution.forEach { (tag, count) -> put(tag, count) }
                    }
                    put("affixSynergyActivationCount", allScenarioAffixSynergyMetrics.activationCount)
                    putJsonObject("affixSynergyActivationDistribution") {
                        allScenarioAffixSynergyMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                    }
                    put("synergyAffixRewardCount", allScenarioSynergyRewardMetrics.rewardCount)
                    put("synergyAffixAdoptionCount", allScenarioSynergyRewardMetrics.adoptionCount)
                    putJsonObject("synergyAffixDistribution") {
                        allScenarioSynergyRewardMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                    }
                    put("breakpointPayoffObservationCount", allScenarioBreakpointMetrics.observationCount)
                    put("breakpointPayoffBuildHashChangeCount", allScenarioBreakpointMetrics.buildHashChangeCount)
                    putJsonObject("breakpointPayoffTalentDistribution") {
                        allScenarioBreakpointMetrics.talentDistribution.forEach { (talentId, count) -> put(talentId, count) }
                    }
                    putJsonObject("breakpointPayoffEffectDistribution") {
                        allScenarioBreakpointMetrics.effectDistribution.forEach { (effectKind, count) -> put(effectKind, count) }
                    }
                    put("fullRouteBreakpointPayoffObservationCount", fullRouteBreakpointMetrics.observationCount)
                    put("fullRouteBreakpointPayoffBuildHashChangeCount", fullRouteBreakpointMetrics.buildHashChangeCount)
                    put("fullRouteAffixSynergyActivationCount", fullRouteAffixSynergyMetrics.activationCount)
                    putJsonObject("fullRouteAffixSynergyActivationDistribution") {
                        fullRouteAffixSynergyMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                    }
                    put("fullRouteSynergyAffixRewardCount", fullRouteSynergyRewardMetrics.rewardCount)
                    put("fullRouteSynergyAffixAdoptionCount", fullRouteSynergyRewardMetrics.adoptionCount)
                    putJsonObject("fullRouteSynergyAffixDistribution") {
                        fullRouteSynergyRewardMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                    }
                    putJsonObject("fullRouteBreakpointPayoffTalentDistribution") {
                        fullRouteBreakpointMetrics.talentDistribution.forEach { (talentId, count) -> put(talentId, count) }
                    }
                    putJsonObject("fullRouteBreakpointPayoffEffectDistribution") {
                        fullRouteBreakpointMetrics.effectDistribution.forEach { (effectKind, count) -> put(effectKind, count) }
                    }
                    put("terminalWeaponBaseDiversity", terminalWeaponIdentity.terminalWeaponBaseDiversity)
                    put("crossProfessionTopWeaponDominance", terminalWeaponIdentity.crossProfessionTopWeaponDominance)
                    put("professionAlignedWeaponAdoptionRate", terminalWeaponIdentity.professionAlignedWeaponAdoptionRate)
                    put("alignedFullRouteSampleCount", terminalWeaponIdentity.alignedFullRouteSampleCount)
                    put("fullRouteSampleCount", terminalWeaponIdentity.fullRouteSampleCount)
                    put("professionCapstoneSeenRate", professionCapstoneSummary.professionCapstoneSeenRate)
                    put("professionCapstoneAdoptionRate", professionCapstoneSummary.professionCapstoneAdoptionRate)
                    put("nonWeaponBuildPayoffRate", professionCapstoneSummary.nonWeaponBuildPayoffRate)
                    put("milestoneRewardAdoptionDelta", milestoneRewardAdoptionDelta)
                    put("milestoneRewardSlotBalance.maxSlotShare", milestoneRewardSlotBalance.maxSlotShare)
                    put("milestoneRewardSlotBalance.WEAPON", milestoneRewardSlotBalance.share(MilestoneRewardSlotFamily.WEAPON))
                    put("milestoneRewardSlotBalance.OFF_HAND", milestoneRewardSlotBalance.share(MilestoneRewardSlotFamily.OFF_HAND))
                    put("milestoneRewardSlotBalance.ARMOR", milestoneRewardSlotBalance.share(MilestoneRewardSlotFamily.ARMOR))
                    put("milestoneRewardSlotBalance.ACCESSORY", milestoneRewardSlotBalance.share(MilestoneRewardSlotFamily.ACCESSORY))
                    put("milestoneRewardSlotBalance.CONSUMABLE_OR_UTILITY", milestoneRewardSlotBalance.share(MilestoneRewardSlotFamily.CONSUMABLE_OR_UTILITY))
                    put("starterProfessionTalentMaxCount", professionTreeChoiceMetrics.starterProfessionTalentMaxCount)
                    put("learnedTalentChoiceEventRate", professionTreeChoiceMetrics.learnedTalentChoiceEventRate)
                    put("multiTreeInvestmentAboveThresholdRate", professionTreeChoiceMetrics.multiTreeInvestmentAboveThresholdRate)
                    put("breakpointChoiceEventRate", professionTreeChoiceMetrics.breakpointChoiceEventRate)
                    putJsonObject("talentTreePrimaryInvestmentDistribution") {
                        professionTreeChoiceMetrics.talentTreePrimaryInvestmentDistribution.forEach { (treeId, count) -> put(treeId, count) }
                    }
                    put("talentReserveSwapCount", professionTreeChoiceMetrics.talentReserveSwapCount)
                    putJsonObject("rankBreakpointAdoptionByTalent") {
                        professionTreeChoiceMetrics.rankBreakpointAdoptionByTalent.forEach { (talentId, count) -> put(talentId, count) }
                    }
                    put("autoLearnedNonStarterTalentCount", professionTreeChoiceMetrics.autoLearnedNonStarterTalentCount)
                    put("starterInscriptionMaxCount", inscriptionShopMetrics.starterInscriptionMaxCount)
                    put("fullSlotInscriptionPurchaseBlockedWithoutReplacementCount", inscriptionShopMetrics.fullSlotInscriptionPurchaseBlockedWithoutReplacementCount)
                    put("fullSlotInscriptionPurchaseReplacementPromptCount", inscriptionShopMetrics.fullSlotInscriptionPurchaseReplacementPromptCount)
                    put("inscriptionInstallOrReplaceRate", inscriptionShopMetrics.inscriptionInstallOrReplaceRate)
                    put("inscriptionReplacementProbeSuccessCount", inscriptionShopMetrics.replacementProbeSuccessCount)
                    putJsonObject("inscriptionReplacementProbe") {
                        put("successCount", inscriptionReplacementProbe.successCount)
                        put("replaceCount", inscriptionReplacementProbe.replaceCount)
                        put("installCount", inscriptionReplacementProbe.installCount)
                        put("selectedHotkey", inscriptionReplacementProbe.selectedHotkey)
                        put("candidateInscriptionId", inscriptionReplacementProbe.candidateInscriptionId)
                        putJsonArray("terminalLoadout") {
                            inscriptionReplacementProbe.terminalLoadout.forEach { inscriptionId -> add(JsonPrimitive(inscriptionId)) }
                        }
                    }
                    put("terminalInscriptionLoadoutDiversity", inscriptionShopMetrics.terminalInscriptionLoadoutDiversity)
                    putJsonObject("inscriptionCategoryCountDistribution") {
                        inscriptionShopMetrics.inscriptionCategoryCountDistribution.forEach { (categorySummary, count) -> put(categorySummary, count) }
                    }
                    put("shopInscriptionOfferConversionRate", inscriptionShopMetrics.shopInscriptionOfferConversionRate)
                    putJsonObject("inscriptionReplaceReasonDistribution") {
                        inscriptionShopMetrics.inscriptionReplaceReasonDistribution.forEach { (reason, count) -> put(reason, count) }
                    }
                    put("inscriptionPurchaseCancelledAfterReplacementPrompt", inscriptionShopMetrics.inscriptionPurchaseCancelledAfterReplacementPrompt)
                    put("shopPurchaseDeniedInsufficientGoldCount", inscriptionShopMetrics.shopPurchaseDeniedInsufficientGoldCount)
                    putJsonArray("includedProfessions") {
                        PROFESSION_TREE_RELEASE_PROFESSIONS.forEach { professionId -> add(JsonPrimitive(professionId)) }
                    }
                    putJsonArray("advancedReportOnlyProfessions") {
                        PROFESSION_TREE_ADVANCED_REPORT_ONLY_PROFESSIONS.forEach { professionId -> add(JsonPrimitive(professionId)) }
                    }
                    putJsonArray("excludedFrozenProfessions") {
                        PROFESSION_TREE_EXCLUDED_FROZEN_PROFESSIONS.forEach { professionId -> add(JsonPrimitive(professionId)) }
                    }
                    terminalWeaponIdentity.topWeaponBaseId?.let { topWeaponBaseId -> put("crossProfessionTopWeaponBaseId", topWeaponBaseId) }
                    put("crossProfessionTopWeaponCount", terminalWeaponIdentity.topWeaponCount)
                    putJsonObject("professionTerminalWeaponDistribution") {
                        terminalWeaponIdentity.professionTerminalWeaponDistribution.forEach { (professionId, distribution) ->
                            putJsonObject(professionId) {
                                distribution.forEach { (weaponBaseId, count) -> put(weaponBaseId, count) }
                            }
                        }
                    }
                    putJsonObject("professionTopWeaponBaseIds") {
                        terminalWeaponIdentity.professionTopWeaponBaseIds.forEach { (professionId, weaponBaseId) ->
                            put(professionId, weaponBaseId)
                        }
                    }
                    putJsonObject("professionTopWeaponSemanticTags") {
                        terminalWeaponIdentity.professionTopWeaponSemanticTags.forEach { (professionId, semanticTags) ->
                            putJsonArray(professionId) {
                                semanticTags.forEach { tag -> add(JsonPrimitive(tag)) }
                            }
                        }
                    }
                    putJsonObject("professionCapstoneBreakdown") {
                        professionCapstoneSummary.professionBreakdown.forEach { (professionId, breakdown) ->
                            putJsonObject(professionId) {
                                put("sampleCount", breakdown.sampleCount)
                                put("seenCount", breakdown.seenCount)
                                put("adoptedCount", breakdown.adoptedCount)
                                put("nonWeaponPayoffCount", breakdown.nonWeaponPayoffCount)
                                putJsonObject("seenItems") {
                                    breakdown.seenItems.forEach { (itemId, count) -> put(itemId, count) }
                                }
                                putJsonObject("adoptedItems") {
                                    breakdown.adoptedItems.forEach { (itemId, count) -> put(itemId, count) }
                                }
                                putJsonObject("nonWeaponPayoffItems") {
                                    breakdown.nonWeaponPayoffItems.forEach { (itemId, count) -> put(itemId, count) }
                                }
                            }
                        }
                    }
                    putJsonObject("capstoneAdoptionBySlot") {
                        capstoneAdoptionBySlot.forEach { (slotId, count) -> put(slotId, count) }
                    }
                    putJsonObject("professionTerminalIdentityItemIds") {
                        professionTerminalIdentityItemIds.forEach { (professionId, itemIds) ->
                            putJsonArray(professionId) {
                                itemIds.forEach { itemId -> add(JsonPrimitive(itemId)) }
                            }
                        }
                    }
                    put("wrongProfessionCapstoneAdoptionCount", wrongProfessionCapstoneAdoptionCount)
                    putJsonObject("fullRouteZoneTraversalDiagnostics") {
                        fullRouteZoneTraversalDiagnostics.forEach { (zoneId, diagnostic) ->
                            putJsonObject(zoneId) {
                                put("sampleCount", diagnostic.sampleCount)
                                put("visitCount", diagnostic.visitCount)
                                put("avgPlayerTurns", diagnostic.avgPlayerTurns)
                                put("avgEnemyTurns", diagnostic.avgEnemyTurns)
                                put("avgEnemyTurnsPerPlayerTurn", diagnostic.avgEnemyTurnsPerPlayerTurn)
                                put("avgVisibleHostileTurnCount", diagnostic.avgVisibleHostileTurnCount)
                                put("avgLiveHostileWindow", diagnostic.avgLiveHostileWindow)
                                put("maxVisibleHostiles", diagnostic.maxVisibleHostiles)
                                put("objectiveAcquireSampleCount", diagnostic.objectiveAcquireSampleCount)
                                diagnostic.avgObjectiveAcquireTurn?.let { put("avgObjectiveAcquireTurn", it) }
                                diagnostic.avgObjectiveAcquireHeadlessTurnEquivalent?.let { put("avgObjectiveAcquireHeadlessTurnEquivalent", it) }
                                putJsonObject("objectiveStateDistribution") {
                                    diagnostic.objectiveStateDistribution.forEach { (state, count) -> put(state, count) }
                                }
                            }
                        }
                    }
                    putJsonArray("criticalPathZoneIds") {
                        criticalPathZoneIds.forEach { zoneId -> add(JsonPrimitive(zoneId)) }
                    }
                    putJsonObject("criticalPathZoneDesignAudit") {
                        criticalPathZoneDesignAudit.forEach { (zoneId, audit) ->
                            putJsonObject(zoneId) {
                                put("floorCount", audit.floorCount)
                                put("mapSize", audit.mapSize)
                                put("worldRole", audit.worldRole)
                                put("monsterPoolCount", audit.monsterPoolCount)
                                put("elitePoolCount", audit.elitePoolCount)
                                audit.bossEncounterId?.let { put("bossEncounterId", it) }
                                audit.objectiveSetId?.let { put("objectiveSetId", it) }
                                audit.objectiveCompletionRule?.let { put("objectiveCompletionRule", it) }
                                putJsonArray("specialMechanics") {
                                    audit.specialMechanics.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
                                }
                                putJsonArray("allMechanicTerms") {
                                    audit.allMechanicTerms.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
                                }
                                putJsonArray("runtimeHookIds") {
                                    audit.runtimeHookIds.forEach { hookId -> add(JsonPrimitive(hookId)) }
                                }
                                putJsonArray("flavorOnlyMechanics") {
                                    audit.flavorOnlyMechanics.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
                                }
                                put("mechanicTermsPartitioned", audit.mechanicTermsPartitioned)
                                putJsonArray("mechanicsWithoutDedicatedRuntimeHook") {
                                    audit.mechanicsWithoutDedicatedRuntimeHook.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
                                }
                                putJsonArray("objectivePlacements") {
                                    audit.objectivePlacements.forEach { placement -> add(JsonPrimitive(placement)) }
                                }
                                putJsonObject("terrainTagWeights") {
                                    audit.terrainTagWeights.forEach { (terrainTag, weight) -> put(terrainTag, weight) }
                                }
                            }
                        }
                    }
                    putJsonObject("scenarioTypeDistribution") {
                        scenarioTypeDistribution.forEach { (scenarioType, count) -> put(scenarioType, count) }
                    }
                    putJsonObject("deathDistribution") {
                        deathDistribution.forEach { (zoneId, count) -> put(zoneId, count) }
                    }
                    putJsonObject("zoneRouteHashDistribution") {
                        routeHashDistribution.forEach { (routeHash, count) -> put(routeHash, count) }
                    }
                    putJsonObject("zoneRouteHashDiversity") {
                        put("totalRuns", routeDiversity.totalRuns)
                        put("distinctHashes", routeDiversity.distinctHashes)
                        put("fullRouteIntentDistinctCount", routeDiversity.fullRouteIntentDistinctCount)
                        put("actualFullRouteHashDistinctCount", routeDiversity.actualFullRouteHashDistinctCount)
                        put("topHash", routeDiversity.topHash)
                        put("topHashCount", routeDiversity.topHashCount)
                        put("topHashShare", routeDiversity.topHashShare)
                        putJsonArray("probeRouteHashSample") {
                            routeDiversity.probeRouteHashSample.forEach { hash -> add(JsonPrimitive(hash)) }
                        }
                    }
                    put("fullRouteIntentDistinctCount", routeDiversity.fullRouteIntentDistinctCount)
                    put("actualFullRouteHashDistinctCount", routeDiversity.actualFullRouteHashDistinctCount)
                    put("topologyCategoryDiversityPerSmokeRun.reportOnly", routeDiversity.distinctHashes.toDouble() / routeDiversity.totalRuns.toDouble())
                    putJsonArray("routeTokenSample") {
                        routeTokenSample.forEach { token -> add(JsonPrimitive(token)) }
                    }
                    putJsonArray("fullRouteTokenSample") {
                        fullRouteTokenSample.forEach { token -> add(JsonPrimitive(token)) }
                    }
                    putJsonArray("branchRouteTokenSample") {
                        branchRouteTokenSample.forEach { token -> add(JsonPrimitive(token)) }
                    }
                    putJsonArray("probeRouteHashSample") {
                        probeRouteHashSample.forEach { hash -> add(JsonPrimitive(hash)) }
                    }
                    putJsonObject("branchRouteHashDistribution") {
                        branchRouteHashDistribution.forEach { (routeHash, count) -> put(routeHash, count) }
                    }
                    putJsonObject("milestoneRewardQualityDistribution") {
                        milestoneRewardQualityDistribution.forEach { (quality, count) -> put(quality, count) }
                    }
                    putJsonObject("milestoneAffixCountDistribution") {
                        milestoneAffixCountDistribution.forEach { (affixCount, count) -> put(affixCount, count) }
                    }
                    putJsonObject("milestoneRewardAdoptionDistribution") {
                        milestoneRewardAdoptionDistribution.forEach { (adoption, count) -> put(adoption, count) }
                    }
                    putJsonObject("milestoneRewardSlotDistribution") {
                        milestoneRewardSlotDistribution.forEach { (slotId, count) -> put(slotId, count) }
                    }
                    putJsonObject("milestoneRewardSlotFamilyDistribution") {
                        milestoneRewardSlotBalance.counts.forEach { (slotFamily, count) -> put(slotFamily.name, count) }
                    }
                    putJsonObject("milestoneRewardSlotFamilyShares") {
                        MilestoneRewardSlotFamily.entries.forEach { slotFamily -> put(slotFamily.name, milestoneRewardSlotBalance.share(slotFamily)) }
                    }
                    putJsonObject("routeRewardAffixUsageSummary") {
                        routeRewardAffixUsageSummary.forEach { (affixId, count) -> put(affixId, count) }
                    }
                    putJsonArray("rewardScoreBreakdownSamples") {
                        rewardScoreBreakdownSamples.forEach { sample ->
                            add(sample.toJson())
                        }
                    }
                    putJsonArray("reports") {
                        reports.forEach { add(it.toJson()) }
                    }
                },
            markdown =
                buildString {
                    appendLine("# Long Run Lab Full")
                    appendLine("- sliceId: phase3-pr15-long-run-full-v1")
                    appendLine("- buildId: ${HarnessMetadata.BUILD_ID}")
                    appendLine("- phaseId: ${HarnessMetadata.PHASE_ID}")
                    appendLine("- rulesetVersion: ${HarnessMetadata.RULESET_VERSION}")
                    appendLine("- traceSchemaVersion: ${HarnessMetadata.TRACE_SCHEMA_VERSION}")
                    appendLine("- corpusId: ${HarnessMetadata.LONG_RUN_FULL_CORPUS_ID}")
                    appendLine("- profileId: ${HarnessMetadata.PROFILE_ID}")
                    appendLine("- localeId: ${reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed"}")
                    appendLine("- fullRouteCount: ${fullRouteReports.size}")
                    appendLine("- branchInclusiveCount: ${branchInclusiveReports.size}")
                    appendLine("- routeProbeCount: ${routeProbeReports.size}")
                    appendLine("- lateRouteProbeCount: ${lateRouteProbeReports.size}")
                    appendLine("- branchSampleCount: $branchSampleCount/${reports.size}")
                    appendLine("- reachedTempleCount: $reachedTempleCount/${fullRouteReports.size}")
                    appendLine("- nonVictoryCount: ${fullRouteNonVictoryReports.size}")
                    appendLine("- failuresAfterDeepIron: $failuresAfterDeepIron")
                    appendLine("- afterDeepIronRatio: ${afterDeepIronRatio ?: "N/A"}")
                    appendLine("- scenarioTypeDistribution: $scenarioTypeDistribution")
                    appendLine("- averageTurns: $averageTurns")
                    appendLine("- averageHeadlessTurns: $averageHeadlessTurns")
                    appendLine("- cadenceRewardCount: $cadenceRewardCount")
                    appendLine("- shopRefreshPurchaseCount: $shopRefreshPurchaseCount")
                    appendLine("- lateRunReliquaryPurchaseCount: $lateRunReliquaryPurchaseCount")
                    appendLine("- lateRunReliquaryVisitCount: $lateRunReliquaryVisitCount")
                    appendLine("- lateRunReliquaryRefreshCount: $lateRunReliquaryRefreshCount")
                    appendLine("- lateRunReliquaryItemPurchaseCount: $lateRunReliquaryItemPurchaseCount")
                    appendLine("- lateRunReliquaryNonMandatoryPurchaseCount: $lateRunReliquaryNonMandatoryPurchaseCount")
                    appendLine("- lateRunReliquaryShardSpent: $lateRunReliquaryShardSpent")
                    appendLine("- lateRunReliquaryTagDistribution: ${if (lateRunReliquaryTagDistribution.isEmpty()) "none" else lateRunReliquaryTagDistribution}")
                    appendLine("- affixSynergyActivationCount: ${allScenarioAffixSynergyMetrics.activationCount}")
                    appendLine("- affixSynergyActivationDistribution: ${if (allScenarioAffixSynergyMetrics.distribution.isEmpty()) "none" else allScenarioAffixSynergyMetrics.distribution}")
                    appendLine("- synergyAffixRewardCount: ${allScenarioSynergyRewardMetrics.rewardCount}")
                    appendLine("- synergyAffixAdoptionCount: ${allScenarioSynergyRewardMetrics.adoptionCount}")
                    appendLine("- synergyAffixDistribution: ${if (allScenarioSynergyRewardMetrics.distribution.isEmpty()) "none" else allScenarioSynergyRewardMetrics.distribution}")
                    appendLine("- breakpointPayoffObservationCount: ${allScenarioBreakpointMetrics.observationCount}")
                    appendLine("- breakpointPayoffBuildHashChangeCount: ${allScenarioBreakpointMetrics.buildHashChangeCount}")
                    appendLine("- breakpointPayoffTalentDistribution: ${if (allScenarioBreakpointMetrics.talentDistribution.isEmpty()) "none" else allScenarioBreakpointMetrics.talentDistribution}")
                    appendLine("- breakpointPayoffEffectDistribution: ${if (allScenarioBreakpointMetrics.effectDistribution.isEmpty()) "none" else allScenarioBreakpointMetrics.effectDistribution}")
                    appendLine("- fullRouteBreakpointPayoffObservationCount: ${fullRouteBreakpointMetrics.observationCount}")
                    appendLine("- fullRouteBreakpointPayoffBuildHashChangeCount: ${fullRouteBreakpointMetrics.buildHashChangeCount}")
                    appendLine("- fullRouteAffixSynergyActivationCount: ${fullRouteAffixSynergyMetrics.activationCount}")
                    appendLine("- fullRouteAffixSynergyActivationDistribution: ${if (fullRouteAffixSynergyMetrics.distribution.isEmpty()) "none" else fullRouteAffixSynergyMetrics.distribution}")
                    appendLine("- fullRouteSynergyAffixRewardCount: ${fullRouteSynergyRewardMetrics.rewardCount}")
                    appendLine("- fullRouteSynergyAffixAdoptionCount: ${fullRouteSynergyRewardMetrics.adoptionCount}")
                    appendLine("- fullRouteSynergyAffixDistribution: ${if (fullRouteSynergyRewardMetrics.distribution.isEmpty()) "none" else fullRouteSynergyRewardMetrics.distribution}")
                    appendLine("- fullRouteBreakpointPayoffTalentDistribution: ${if (fullRouteBreakpointMetrics.talentDistribution.isEmpty()) "none" else fullRouteBreakpointMetrics.talentDistribution}")
                    appendLine("- fullRouteBreakpointPayoffEffectDistribution: ${if (fullRouteBreakpointMetrics.effectDistribution.isEmpty()) "none" else fullRouteBreakpointMetrics.effectDistribution}")
                    appendLine("- terminalWeaponBaseDiversity: ${terminalWeaponIdentity.terminalWeaponBaseDiversity}")
                    appendLine("- crossProfessionTopWeaponDominance: ${terminalWeaponIdentity.crossProfessionTopWeaponDominance}")
                    appendLine("- professionAlignedWeaponAdoptionRate: ${terminalWeaponIdentity.professionAlignedWeaponAdoptionRate}")
                    appendLine("- professionCapstoneSeenRate: ${professionCapstoneSummary.professionCapstoneSeenRate}")
                    appendLine("- professionCapstoneAdoptionRate: ${professionCapstoneSummary.professionCapstoneAdoptionRate}")
                    appendLine("- nonWeaponBuildPayoffRate: ${professionCapstoneSummary.nonWeaponBuildPayoffRate}")
                    appendLine("- milestoneRewardAdoptionDelta: $milestoneRewardAdoptionDelta")
                    appendLine("- milestoneRewardSlotFamilyDistribution: ${milestoneRewardSlotBalance.counts}")
                    appendLine("- milestoneRewardSlotFamilyShares: ${MilestoneRewardSlotFamily.entries.associate { slotFamily -> slotFamily.name to milestoneRewardSlotBalance.share(slotFamily) }}")
                    appendLine("- wrongProfessionCapstoneAdoptionCount: $wrongProfessionCapstoneAdoptionCount")
                    appendLine("- starterProfessionTalentMaxCount: ${professionTreeChoiceMetrics.starterProfessionTalentMaxCount}")
                    appendLine("- learnedTalentChoiceEventRate: ${professionTreeChoiceMetrics.learnedTalentChoiceEventRate}")
                    appendLine("- multiTreeInvestmentAboveThresholdRate: ${professionTreeChoiceMetrics.multiTreeInvestmentAboveThresholdRate}")
                    appendLine("- breakpointChoiceEventRate: ${professionTreeChoiceMetrics.breakpointChoiceEventRate}")
                    appendLine("- talentTreePrimaryInvestmentDistribution: ${if (professionTreeChoiceMetrics.talentTreePrimaryInvestmentDistribution.isEmpty()) "none" else professionTreeChoiceMetrics.talentTreePrimaryInvestmentDistribution}")
                    appendLine("- talentReserveSwapCount: ${professionTreeChoiceMetrics.talentReserveSwapCount}")
                    appendLine("- rankBreakpointAdoptionByTalent: ${if (professionTreeChoiceMetrics.rankBreakpointAdoptionByTalent.isEmpty()) "none" else professionTreeChoiceMetrics.rankBreakpointAdoptionByTalent}")
                    appendLine("- autoLearnedNonStarterTalentCount: ${professionTreeChoiceMetrics.autoLearnedNonStarterTalentCount}")
                    appendLine("- starterInscriptionMaxCount: ${inscriptionShopMetrics.starterInscriptionMaxCount}")
                    appendLine("- fullSlotInscriptionPurchaseBlockedWithoutReplacementCount: ${inscriptionShopMetrics.fullSlotInscriptionPurchaseBlockedWithoutReplacementCount}")
                    appendLine("- fullSlotInscriptionPurchaseReplacementPromptCount: ${inscriptionShopMetrics.fullSlotInscriptionPurchaseReplacementPromptCount}")
                    appendLine("- inscriptionInstallOrReplaceRate: ${inscriptionShopMetrics.inscriptionInstallOrReplaceRate}")
                    appendLine("- inscriptionReplacementProbeSuccessCount: ${inscriptionShopMetrics.replacementProbeSuccessCount}")
                    appendLine("- inscriptionReplacementProbe: hotkey=${inscriptionReplacementProbe.selectedHotkey}, candidate=${inscriptionReplacementProbe.candidateInscriptionId}, replace=${inscriptionReplacementProbe.replaceCount}, install=${inscriptionReplacementProbe.installCount}, terminal=${inscriptionReplacementProbe.terminalLoadout}")
                    appendLine("- terminalInscriptionLoadoutDiversity: ${inscriptionShopMetrics.terminalInscriptionLoadoutDiversity}")
                    appendLine("- inscriptionCategoryCountDistribution: ${if (inscriptionShopMetrics.inscriptionCategoryCountDistribution.isEmpty()) "none" else inscriptionShopMetrics.inscriptionCategoryCountDistribution}")
                    appendLine("- shopInscriptionOfferConversionRate: ${inscriptionShopMetrics.shopInscriptionOfferConversionRate}")
                    appendLine("- inscriptionReplaceReasonDistribution: ${if (inscriptionShopMetrics.inscriptionReplaceReasonDistribution.isEmpty()) "none" else inscriptionShopMetrics.inscriptionReplaceReasonDistribution}")
                    appendLine("- includedProfessions: ${PROFESSION_TREE_RELEASE_PROFESSIONS.joinToString()}")
                    appendLine("- advancedReportOnlyProfessions: ${PROFESSION_TREE_ADVANCED_REPORT_ONLY_PROFESSIONS.joinToString()}")
                    appendLine("- excludedFrozenProfessions: ${PROFESSION_TREE_EXCLUDED_FROZEN_PROFESSIONS.joinToString()}")
                    appendLine("- crossProfessionTopWeaponBaseId: ${terminalWeaponIdentity.topWeaponBaseId ?: "none"}")
                    appendLine("- crossProfessionTopWeaponCount: ${terminalWeaponIdentity.topWeaponCount}/${terminalWeaponIdentity.fullRouteSampleCount}")
                    appendLine("- professionTerminalWeaponDistribution: ${if (terminalWeaponIdentity.professionTerminalWeaponDistribution.isEmpty()) "none" else terminalWeaponIdentity.professionTerminalWeaponDistribution}")
                    appendLine("- professionTopWeaponBaseIds: ${if (terminalWeaponIdentity.professionTopWeaponBaseIds.isEmpty()) "none" else terminalWeaponIdentity.professionTopWeaponBaseIds}")
                    appendLine("- professionTopWeaponSemanticTags: ${if (terminalWeaponIdentity.professionTopWeaponSemanticTags.isEmpty()) "none" else terminalWeaponIdentity.professionTopWeaponSemanticTags}")
                    appendLine("- professionCapstoneBreakdown: ${if (professionCapstoneSummary.professionBreakdown.isEmpty()) "none" else professionCapstoneSummary.professionBreakdown}")
                    appendLine("- capstoneAdoptionBySlot: ${if (capstoneAdoptionBySlot.isEmpty()) "none" else capstoneAdoptionBySlot}")
                    appendLine("- professionTerminalIdentityItemIds: ${if (professionTerminalIdentityItemIds.isEmpty()) "none" else professionTerminalIdentityItemIds}")
                    appendLine("- fullRouteZoneTraversalDiagnostics: ${if (fullRouteZoneTraversalDiagnostics.isEmpty()) "none" else fullRouteZoneTraversalDiagnostics}")
                    appendLine("- criticalPathZoneIds: ${criticalPathZoneIds.joinToString()}")
                    appendLine("- criticalPathZoneDesignAudit: ${if (criticalPathZoneDesignAudit.isEmpty()) "none" else criticalPathZoneDesignAudit}")
                    appendLine("- deathDistribution: ${if (deathDistribution.isEmpty()) "none" else deathDistribution}")
                    appendLine("- zoneRouteHashDistribution: ${if (routeHashDistribution.isEmpty()) "none" else routeHashDistribution}")
                    appendLine("- zoneRouteHashDiversity.topHashShare: ${routeDiversity.topHashShare}")
                    appendLine("- fullRouteIntentDistinctCount: ${routeDiversity.fullRouteIntentDistinctCount}")
                    appendLine("- actualFullRouteHashDistinctCount: ${routeDiversity.actualFullRouteHashDistinctCount}")
                    appendLine("- routeTokenSample: ${if (routeTokenSample.isEmpty()) "none" else routeTokenSample}")
                    appendLine("- fullRouteTokenSample: ${if (fullRouteTokenSample.isEmpty()) "none" else fullRouteTokenSample}")
                    appendLine("- branchRouteTokenSample: ${if (branchRouteTokenSample.isEmpty()) "none" else branchRouteTokenSample}")
                    appendLine("- probeRouteHashSample: ${if (probeRouteHashSample.isEmpty()) "none" else probeRouteHashSample}")
                    appendLine("- branchRouteHashDistribution: ${if (branchRouteHashDistribution.isEmpty()) "none" else branchRouteHashDistribution}")
                    appendLine("- milestoneRewardQualityDistribution: ${if (milestoneRewardQualityDistribution.isEmpty()) "none" else milestoneRewardQualityDistribution}")
                    appendLine("- milestoneAffixCountDistribution: ${if (milestoneAffixCountDistribution.isEmpty()) "none" else milestoneAffixCountDistribution}")
                    appendLine("- milestoneRewardAdoptionDistribution: ${if (milestoneRewardAdoptionDistribution.isEmpty()) "none" else milestoneRewardAdoptionDistribution}")
                    appendLine("- milestoneRewardSlotDistribution: ${if (milestoneRewardSlotDistribution.isEmpty()) "none" else milestoneRewardSlotDistribution}")
                    appendLine("- routeRewardAffixUsageSummary: ${if (routeRewardAffixUsageSummary.isEmpty()) "none" else routeRewardAffixUsageSummary}")
                    appendLine("- rewardScoreBreakdownSamples: ${rewardScoreBreakdownSamples.size}")
                    reports.forEach { report ->
                        val objectiveSummary =
                            report.zoneObjectiveSummaries.joinToString { summary ->
                                "${summary.zoneId}:${summary.state.name}${if (summary.completionFlagGranted) "#flag" else ""}"
                            }
                        val milestoneSummary =
                            report.milestoneRewards.joinToString { reward ->
                                "${reward.rewardSource}:${reward.baseItemId}:${reward.equipSlot.name}:before=${reward.equippedBaseItemIdBeforeReward ?: "empty"}:final=${reward.equippedBaseItemIdAtRunEnd ?: "empty"}:adoptedDuring=${reward.adoptedDuringRun}:adoptedFinal=${reward.adoptedInFinalBuild}:${reward.qualityTier.name}:${if (reward.affixIds.isEmpty()) "none" else reward.affixIds.joinToString("+")}"
                            }
                        val breakpointSummary =
                            report.breakpointPayoffs.joinToString { payoff ->
                                "${payoff.talentId}@${payoff.breakpointRank}:${payoff.unlockedEffectKinds.joinToString("+")}"
                            }
                        val breakpointObservationSummary =
                            report.breakpointPayoffObservations.joinToString { observation ->
                                "${observation.talentId}@${observation.breakpointRank}:${observation.buildHashChanged}:${observation.buildHashBeforeUnlock}->${observation.buildHashAfterUnlock}"
                            }
                        appendLine(
                            "- class=${report.professionId}, race=${report.raceId}, seed=${report.seed}, scenarioType=${report.scenarioType.reportValue}, isFullRoute=${report.isFullRoute}, finalZone=${report.finalZoneId}, turns=${report.turns}, headless=${report.headlessTurnEquivalent}, routeHash=${report.zoneRouteHash}, route=${report.zonePath.joinToString(" -> ")}, objectives=${if (objectiveSummary.isBlank()) "none" else objectiveSummary}, buildHash=${report.buildHash ?: "unknown"}, breakpointPayoffs=${if (breakpointSummary.isBlank()) "none" else breakpointSummary}, breakpointPayoffObservations=${if (breakpointObservationSummary.isBlank()) "none" else breakpointObservationSummary}, affixSynergy=${report.affixSynergyActivationCount}:${if (report.affixSynergyActivationDistribution.isEmpty()) "none" else report.affixSynergyActivationDistribution}, cadence=${report.cadenceRewardCount}, refresh=${report.shopRefreshPurchaseCount}, reliquary={visits=${report.lateRunReliquaryVisitCount}, purchases=${report.lateRunReliquaryPurchaseCount}, items=${report.lateRunReliquaryItemPurchaseCount}, refreshes=${report.lateRunReliquaryRefreshCount}, nonMandatory=${report.lateRunReliquaryNonMandatoryPurchaseCount}, spent=${report.lateRunReliquaryShardSpent}, tags=${if (report.lateRunReliquaryTagDistribution.isEmpty()) "none" else report.lateRunReliquaryTagDistribution}}, milestoneRewards=${if (milestoneSummary.isBlank()) "none" else milestoneSummary}, outcome=${report.outcome}, crashedOrStalled=${report.crashedOrStalled()}",
                        )
                        if (report.headlessTurnEquivalent > 2900 || report.outcome !is RunOutcome.Victory) {
                            val zoneHeadlessSummary =
                                report.zoneHeadlessMilestones.joinToString { milestone ->
                                    "${milestone.zoneId}:${milestone.headlessTurnEquivalent}(+${milestone.deltaHeadlessTurns})"
                                }
                            appendLine("  zoneHeadless=$zoneHeadlessSummary")
                        }
                        if (report.finalZoneId == "shattered_outpost") {
                            val captainTraceSummary =
                                report.captainEncounterTrace.takeLast(6).joinToString { entry ->
                                    "t${entry.turnIndex}/h${entry.headlessTurnEquivalent}/hp${entry.playerHp}/${entry.playerMaxHp}/res${entry.playerResourceCurrent}/${entry.playerResourceMax}/${entry.playerResourceTypeId}/boss${entry.captainHp ?: -1}/${entry.captainMaxHp ?: -1}/d${entry.captainDistance ?: -1}/${entry.command ?: "-"}"
                                }
                            appendLine("  captainTrace=$captainTraceSummary")
                        }
                    }
                },
        )
        val longRunPayload = kotlinx.serialization.json.Json.parseToJsonElement(reportPath.toFile().readText()).jsonObject

        assertTrue(
            reports.none(ScenarioReport::crashedOrStalled),
            reports.filter(ScenarioReport::crashedOrStalled).joinToString(separator = "\n") { report ->
                "${report.professionId}/${report.raceId}/${report.seed}/${report.scenarioType.reportValue}: ${report.failureReason ?: report.stuckReason ?: "unknown"}"
            },
        )
        assertTrue(
            failingReports.isEmpty(),
            failingReports.joinToString(separator = "\n") { report ->
                val tail = (report.assertionFailures + listOfNotNull(report.failureReason, report.stuckReason)).joinToString()
                "${report.professionId}/${report.raceId}/${report.seed}/${report.scenarioType.reportValue}: ${tail.ifBlank { report.outcome.toString() }}"
            },
        )
        assertTrue(
            fullRouteReports.size == 12,
            "Expected full-route matrix to freeze at 12 foundation runs, actual=${fullRouteReports.size}",
        )
        assertTrue(
            branchInclusiveReports.size == 4,
            "Expected branch-inclusive probe matrix to freeze at four route variants, actual=${branchInclusiveReports.size}",
        )
        assertTrue(
            fullRouteReports.all { report ->
                report.isFullRoute &&
                    report.routeIndex == 0
            },
            "Expected every full-route matrix sample to stay explicitly full-route with routeIndex=0.",
        )
        assertTrue(
            fullRouteReports.groupingBy(ScenarioReport::zoneId).eachCount().toSortedMap() ==
                mapOf("deep_iron_pit" to 4, "greenwood_fringe" to 4, "underground_river" to 4),
            "Expected PR06 full-route start-zone distribution to be 4/4/4, actual=${fullRouteReports.groupingBy(ScenarioReport::zoneId).eachCount()}",
        )
        assertTrue(longRunPayload.containsKey("professionCapstoneSeenRate"))
        assertTrue(longRunPayload.containsKey("professionCapstoneAdoptionRate"))
        assertTrue(longRunPayload.containsKey("nonWeaponBuildPayoffRate"))
        assertTrue(longRunPayload.containsKey("starterProfessionTalentMaxCount"))
        assertTrue(longRunPayload.containsKey("learnedTalentChoiceEventRate"))
        assertTrue(longRunPayload.containsKey("multiTreeInvestmentAboveThresholdRate"))
        assertTrue(longRunPayload.containsKey("breakpointChoiceEventRate"))
        assertTrue(longRunPayload.containsKey("talentTreePrimaryInvestmentDistribution"))
        assertTrue(longRunPayload.containsKey("rankBreakpointAdoptionByTalent"))
        assertTrue(longRunPayload.containsKey("autoLearnedNonStarterTalentCount"))
        assertTrue(longRunPayload.containsKey("professionCapstoneBreakdown"))
        assertTrue(
            branchInclusiveReports.all { report ->
                !report.isFullRoute &&
                    report.primarySecretZoneId != null
            },
            "Expected branch-inclusive probes to remain explicitly downgraded from full-route gate and carry a primary secret marker.",
        )
        assertTrue(
            routeProbeReports.size == 2,
            "Expected PR06 full owner evidence to include two route probes, actual=${routeProbeReports.size}.",
        )
        assertTrue(
            lateRouteProbeReports.size == 2,
            "Expected PR06 full owner evidence to include two late-route probes, actual=${lateRouteProbeReports.size}.",
        )
        assertTrue(
            routeDiversity.topHashShare <= 0.40,
            "Expected zoneRouteHashDiversity.topHashShare <= 0.40, actual=${routeDiversity.topHashShare} distribution=$routeHashDistribution",
        )
        assertTrue(
            routeDiversity.fullRouteIntentDistinctCount == 12,
            "Expected fullRouteIntentDistinctCount=12, actual=${routeDiversity.fullRouteIntentDistinctCount}",
        )
        val fullRouteStartZoneCount = fullRouteReports.map(ScenarioReport::zoneId).distinct().size
        assertTrue(
            routeDiversity.actualFullRouteHashDistinctCount >= fullRouteStartZoneCount,
            "Expected actual full-route hash diversity to stay visible at least at start-zone granularity, actual=${routeDiversity.actualFullRouteHashDistinctCount}",
        )
        assertTrue(
            branchInclusiveReports.all { report ->
                report.primarySecretZoneId != null &&
                    report.primarySecretZoneId in report.visitedSecretZoneIds &&
                    "secret:${report.primarySecretZoneId}" in report.routeToken
            },
            "Expected branch-inclusive matrix to prove runtime secret visits, actual=${branchInclusiveReports.map { report -> report.name to report.visitedSecretZoneIds }}",
        )
        assertTrue(
            probeRouteHashSample.size == 4,
            "Expected route and late-route probes to expose four independent probe hashes, actual=$probeRouteHashSample",
        )
        assertTrue(
            reports.all { report -> report.zoneRouteHash.length == 16 && report.seedString == report.seed.toString() },
            "Expected 16-char route hashes and raw seed strings in every PR06 full report.",
        )
        assertTrue(
            fullRouteReports.all { report -> report.headlessTurnEquivalent <= 3000 },
            "Expected all full-route gate runs to stay within headlessTurnEquivalent <= 3000.",
        )
        assertTrue(
            branchSampleCount >= 4,
            "Expected branch-inclusive matrix to include all four optional branches, actual=$branchSampleCount/${reports.size}",
        )
        assertTrue(
            branchRouteHashDistribution.size >= 3,
            "Expected branch-inclusive probe matrix to exercise at least 3 distinct route hashes, actual=$branchRouteHashDistribution",
        )
        assertTrue(
            branchInclusiveReports.any { report ->
                report.zonePath.contains("underground_river") && report.zonePath.contains("crystal_cavern")
            },
            "Expected branch-inclusive matrix to keep at least one routed sample through underground_river -> crystal_cavern.",
        )
        val expectedGuardProfileZoneCoverage =
            setOf(
                "greenwood_fringe",
                "deep_iron_pit",
                "underground_river",
                "crystal_cavern",
                "abyssal_temple",
            )
        expectedGuardProfileZoneCoverage.forEach { zoneId ->
            val zoneReports = reports.filter { report -> zoneId in report.zonePath }
            assertTrue(
                zoneReports.isNotEmpty(),
                "Expected PR06 focus zone '$zoneId' to be covered by long-run owner evidence.",
            )
            assertTrue(
                zoneReports.all { report -> report.success && !report.crashedOrStalled() },
                "Expected PR06 focus zone '$zoneId' to remain stable under long-run coverage, actual=${zoneReports.map { report -> "${report.name}:${report.zonePath}:${report.failureReason ?: report.stuckReason ?: report.outcome}" }}",
            )
        }
        assertTrue(
            reachedTempleCount >= 8,
            "Expected at least 8/12 full-route matrix runs to reach abyssal_temple or deeper, actual=$reachedTempleCount/${fullRouteReports.size}",
        )
        if (afterDeepIronRatio != null && fullRouteNonVictoryReports.size >= 2) {
            assertTrue(
                afterDeepIronRatio >= 0.5,
                "Expected at least 50% of full-route non-victory runs to fail after deep_iron_pit, actual=$afterDeepIronRatio",
            )
        }
        assertTrue(
            fullRouteBreakpointMetrics.observationCount >= 4,
            "Expected full-route matrix to observe at least four breakpoint payoff unlocks, actual=${fullRouteBreakpointMetrics.observationCount}",
        )
        assertTrue(
            fullRouteBreakpointMetrics.buildHashChangeCount >= 4,
            "Expected full-route matrix to record build-hash changes for observed payoff unlocks, actual=${fullRouteBreakpointMetrics.buildHashChangeCount}",
        )
        assertTrue(
            allScenarioSynergyRewardMetrics.rewardCount >= 1,
            "Expected full long-run lab to surface at least one documented synergy affix reward, actual=${allScenarioSynergyRewardMetrics.distribution}",
        )
        assertTrue(
            fullRouteSynergyRewardMetrics.rewardCount >= 1,
            "Expected full-route matrix to surface at least one documented synergy affix reward, actual=${fullRouteSynergyRewardMetrics.distribution}",
        )
        assertTrue(
            fullRouteSynergyRewardMetrics.adoptionCount >= 1,
            "Expected full-route matrix to keep at least one documented synergy affix in the final build, actual=${fullRouteSynergyRewardMetrics.distribution}",
        )
        assertTrue(
            routeRewardAffixUsageSummary.isNotEmpty(),
            "Expected PR06 full-route matrix to retain route reward affix owner evidence.",
        )
        assertTrue(
            milestoneRewardSlotBalance.counts.keys.containsAll(
                setOf(
                    MilestoneRewardSlotFamily.WEAPON,
                    MilestoneRewardSlotFamily.OFF_HAND,
                    MilestoneRewardSlotFamily.ARMOR,
                    MilestoneRewardSlotFamily.ACCESSORY,
                ),
            ),
            "Expected PR06 reward owner evidence to cover build slot families, actual=${milestoneRewardSlotBalance.counts}",
        )
        assertTrue(
            milestoneRewardQualityDistribution.keys.containsAll(setOf("MAGIC", "RARE")),
            "Expected PR06 reward owner evidence to retain multiple reward qualities, actual=$milestoneRewardQualityDistribution",
        )
        assertTrue(
            milestoneAffixCountDistribution.keys.any { affixCount -> affixCount.toInt() >= 2 },
            "Expected PR06 reward owner evidence to retain affixed rewards, actual=$milestoneAffixCountDistribution",
        )
        assertTrue(longRunPayload.containsKey("starterInscriptionMaxCount"))
        assertTrue(longRunPayload.containsKey("fullSlotInscriptionPurchaseBlockedWithoutReplacementCount"))
        assertTrue(longRunPayload.containsKey("fullSlotInscriptionPurchaseReplacementPromptCount"))
        assertTrue(longRunPayload.containsKey("inscriptionInstallOrReplaceRate"))
        assertTrue(longRunPayload.containsKey("inscriptionReplacementProbeSuccessCount"))
        assertTrue(longRunPayload.containsKey("inscriptionReplacementProbe"))
        assertEquals(
            1,
            inscriptionShopMetrics.replacementProbeSuccessCount,
            "Expected longRun owner evidence to prove the full-slot replacement path with SmokeBot.",
        )
        assertTrue(longRunPayload.containsKey("terminalInscriptionLoadoutDiversity"))
        assertTrue(longRunPayload.containsKey("shopInscriptionOfferConversionRate"))
    }

    @Test
    fun `terminal weapon identity summary fails fast when a full-route report is missing terminal weapon contract data`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                terminalWeaponIdentitySummary(
                    listOf(
                        sampleScenarioReport(
                            name = "missing-terminal-weapon",
                            professionId = "vanguard",
                            terminalWeaponBaseId = null,
                        ),
                    ),
                )
            }

        assertTrue(exception.message.orEmpty().contains("missing terminalWeaponBaseId"))
    }

    @Test
    fun `profession aligned terminal weapon rules stay in sync with the current item catalog`() {
        val fullRouteProfessionIds = fullRouteMatrixSpecs().map(ScenarioSpec::professionId).toSet()
        val allKnownItemTags = itemTagsById.values.flatten().toSet()

        assertEquals(fullRouteProfessionIds, PROFESSION_ALIGNED_TERMINAL_WEAPON_RULES.keys)
        PROFESSION_ALIGNED_TERMINAL_WEAPON_RULES.forEach { (professionId, rule) ->
            assertTrue(
                rule.exactWeaponBaseIds.all(itemTagsById::containsKey),
                "Profession '$professionId' references unknown terminal weapon ids ${rule.exactWeaponBaseIds - itemTagsById.keys}.",
            )
            assertTrue(
                rule.requiredItemTags.all(allKnownItemTags::contains),
                "Profession '$professionId' references unknown alignment tags ${rule.requiredItemTags - allKnownItemTags}.",
            )
        }
    }

    @Test
    fun `profession capstone summary tracks seen adopted and non weapon payoff rates`() {
        val summary =
            professionCapstoneSummary(
                listOf(
                    sampleScenarioReport(
                        name = "capstone-arcanist",
                        professionId = "arcanist",
                        terminalWeaponBaseId = "arcane_staff",
                        milestoneRewards =
                            listOf(
                                sampleMilestoneReward(
                                    baseItemId = "unique_deepcurrent_lens",
                                    equipSlot = EquipSlot.OFF_HAND,
                                    adoptedInFinalBuild = true,
                                ),
                            ),
                    ),
                    sampleScenarioReport(
                        name = "capstone-vanguard",
                        professionId = "vanguard",
                        terminalWeaponBaseId = "long_sword",
                        milestoneRewards =
                            listOf(
                                sampleMilestoneReward(
                                    baseItemId = "artifact_forge_oath",
                                    equipSlot = EquipSlot.WEAPON,
                                    adoptedInFinalBuild = true,
                                ),
                            ),
                    ),
                    sampleScenarioReport(
                        name = "capstone-templar",
                        professionId = "templar",
                        terminalWeaponBaseId = "long_sword",
                        milestoneRewards =
                            listOf(
                                sampleMilestoneReward(
                                    baseItemId = "unique_voidlit_seal",
                                    equipSlot = EquipSlot.OFF_HAND,
                                    adoptedInFinalBuild = false,
                                ),
                            ),
                    ),
                ),
            )

        assertEquals(1.0, summary.professionCapstoneSeenRate, 0.0001)
        assertEquals(2.0 / 3.0, summary.professionCapstoneAdoptionRate, 0.0001)
        assertEquals(1.0 / 3.0, summary.nonWeaponBuildPayoffRate, 0.0001)
        assertEquals(1, summary.professionBreakdown.getValue("arcanist").seenCount)
        assertEquals(1, summary.professionBreakdown.getValue("arcanist").adoptedCount)
        assertEquals(1, summary.professionBreakdown.getValue("arcanist").nonWeaponPayoffCount)
        assertEquals(1, summary.professionBreakdown.getValue("templar").seenCount)
        assertEquals(0, summary.professionBreakdown.getValue("templar").adoptedCount)
        assertEquals(0, summary.professionBreakdown.getValue("templar").nonWeaponPayoffCount)
    }

    @Test
    fun `zone traversal diagnostics aggregate enemy turn pressure and objective timing`() {
        val reports =
            listOf(
                sampleScenarioReport(
                    name = "diag-a",
                    professionId = "vanguard",
                    terminalWeaponBaseId = "battle_axe",
                    zoneTraversalDiagnostics =
                        listOf(
                            ZoneTraversalDiagnostic(
                                zoneId = "greenwood_fringe",
                                visitCount = 1,
                                playerTurns = 100,
                                enemyTurns = 220,
                                enemyTurnsPerPlayerTurn = 2.2,
                                visibleHostileTurnCount = 35,
                                liveHostileWindow = 12,
                                maxVisibleHostiles = 3,
                                objectiveAcquireTurn = 8,
                                objectiveAcquireHeadlessTurnEquivalent = 14,
                                objectiveStateAtExit = com.ktome.core.world.ObjectiveState.COMPLETED,
                            ),
                        ),
                ),
                sampleScenarioReport(
                    name = "diag-b",
                    professionId = "templar",
                    terminalWeaponBaseId = "long_sword",
                    zoneTraversalDiagnostics =
                        listOf(
                            ZoneTraversalDiagnostic(
                                zoneId = "greenwood_fringe",
                                visitCount = 1,
                                playerTurns = 80,
                                enemyTurns = 120,
                                enemyTurnsPerPlayerTurn = 1.5,
                                visibleHostileTurnCount = 20,
                                liveHostileWindow = 7,
                                maxVisibleHostiles = 2,
                                objectiveAcquireTurn = 6,
                                objectiveAcquireHeadlessTurnEquivalent = 10,
                                objectiveStateAtExit = com.ktome.core.world.ObjectiveState.IN_PROGRESS,
                            ),
                        ),
                ),
            )

        val aggregate = requireNotNull(aggregateZoneTraversalDiagnostics(reports)["greenwood_fringe"])

        assertEquals(2, aggregate.sampleCount)
        assertEquals(2, aggregate.visitCount)
        assertEquals(90.0, aggregate.avgPlayerTurns)
        assertEquals(170.0, aggregate.avgEnemyTurns)
        assertEquals(1.85, aggregate.avgEnemyTurnsPerPlayerTurn, 0.0001)
        assertEquals(27.5, aggregate.avgVisibleHostileTurnCount)
        assertEquals(9.5, aggregate.avgLiveHostileWindow)
        assertEquals(3, aggregate.maxVisibleHostiles)
        assertEquals(2, aggregate.objectiveAcquireSampleCount)
        assertEquals(7.0, aggregate.avgObjectiveAcquireTurn)
        assertEquals(12.0, aggregate.avgObjectiveAcquireHeadlessTurnEquivalent)
        assertEquals(mapOf("COMPLETED" to 1, "IN_PROGRESS" to 1), aggregate.objectiveStateDistribution)
    }

    @Test
    fun `critical path zone design audit exposes current pacing contracts`() {
        val audit = criticalPathZoneDesignAudit()

        assertEquals(setOf("greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "underground_river", "abyssal_temple"), audit.keys)
        assertTrue(audit.getValue("greenwood_fringe").objectivePlacements.any { placement -> "trail_cache@floor1:stairs_down+1,0" in placement })
        assertTrue(audit.getValue("greenwood_fringe").runtimeHookIds.contains("trail_pressure"))
        assertTrue(!audit.getValue("greenwood_fringe").mechanicsWithoutDedicatedRuntimeHook.contains("trail_pressure"))
        assertTrue(audit.getValue("deep_iron_pit").objectivePlacements.any { placement -> "mine_furnace@floor2:boss_entry+0,0" in placement })
        assertTrue(audit.getValue("deep_iron_pit").runtimeHookIds.contains("slag_alert"))
        assertTrue(!audit.getValue("deep_iron_pit").mechanicsWithoutDedicatedRuntimeHook.contains("slag_alert"))
        assertTrue(audit.getValue("grey_gate_depths").objectivePlacements.any { placement -> "seal_cache@floor1:stairs_down+1,0" in placement })
        assertTrue(audit.getValue("underground_river").objectivePlacements.any { placement -> "crystal_cache_chest@floor1:stairs_down+1,0" in placement })
        assertTrue(audit.getValue("abyssal_temple").specialMechanics.contains("void_pressure"))
        assertTrue(audit.getValue("abyssal_temple").runtimeHookIds.contains("void_pressure"))
        assertTrue(audit.values.all(CriticalPathZoneDesignAuditEntry::mechanicTermsPartitioned))
    }

    private fun fullRouteMatrixSpecs(): List<ScenarioSpec> {
        val professions = listOf("vanguard", "arcanist", "rogue", "templar")
        val races = listOf("human", "elf", "dwarf")
        return professions.flatMap { professionId ->
            races.map { raceId ->
                ScenarioSpec(
                    name = "long-run-full-$professionId-$raceId",
                    seed = LongRunLabSeedBank.fullRouteMatrixSeed(professionId = professionId, raceId = raceId),
                    professionId = professionId,
                    raceId = raceId,
                    zoneId = FOUNDATION_ZONE_ROUTE.first(),
                    zoneRoute = FOUNDATION_ZONE_ROUTE,
                    routeIndex = 0,
                    scenarioType = ScenarioType.FULL_ROUTE,
                    corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
                    maxTurns = 1800,
                    goal = ScenarioGoal.ReachTerminal,
                    initialTalentPointGrant = PROFESSION_TREE_CHOICE_PROBE_TALENT_POINTS,
                    assertions = emptyList(),
                )
            }
        }
    }

    private fun branchProbeMatrixSpecs(): List<ScenarioSpec> =
        listOf(
            branchInclusiveSpec(
                name = "long-run-branch-bandit-rogue-human",
                seed = 20260320L,
                professionId = "rogue",
                raceId = "human",
                zoneRoute = FOUNDATION_BANDIT_ROUTE,
                goalZoneId = "bandit_camp",
            ),
            branchInclusiveSpec(
                name = "long-run-branch-elven-rogue-elf",
                seed = 20260451L,
                professionId = "rogue",
                raceId = "elf",
                zoneRoute = FOUNDATION_ELVEN_ROUTE,
                goalZoneId = "elven_ruins",
            ),
            branchInclusiveSpec(
                name = "long-run-branch-molten-vanguard-dwarf",
                seed = LongRunLabSeedBank.fullRouteMatrixSeed(professionId = "vanguard", raceId = "dwarf"),
                professionId = "vanguard",
                raceId = "dwarf",
                zoneRoute = FOUNDATION_MOLTEN_ROUTE,
                goalZoneId = "molten_core",
            ),
            branchInclusiveSpec(
                name = "long-run-branch-crystal-arcanist-human",
                seed = 20260440L,
                professionId = "arcanist",
                raceId = "human",
                zoneRoute = FOUNDATION_CRYSTAL_ROUTE,
                goalZoneId = "crystal_cavern",
            ),
        )

    private fun branchInclusiveSpec(
        name: String,
        seed: Long,
        professionId: String,
        raceId: String,
        zoneRoute: List<String>,
        goalZoneId: String,
    ): ScenarioSpec =
        ScenarioSpec(
            name = name,
            seed = seed,
            professionId = professionId,
            raceId = raceId,
            zoneId = FOUNDATION_ZONE_ROUTE.first(),
            zoneRoute = zoneRoute,
            routeIndex = 0,
            scenarioType = ScenarioType.BRANCH_INCLUSIVE,
            corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
            maxTurns = 1800,
            goal = ScenarioGoal.ReachZoneAtLeastOrTerminal(goalZoneId),
            assertions =
                listOf(
                    ScenarioAssertion.NoFailure,
                    ScenarioAssertion.NoStall,
                    ScenarioAssertion.VisitedZone(goalZoneId),
                ),
        )

    private fun breakpointMetrics(reports: List<ScenarioReport>): BreakpointMetrics {
        val breakpointPayoffs = reports.flatMap(ScenarioReport::breakpointPayoffs)
        val observations = reports.flatMap(ScenarioReport::breakpointPayoffObservations)
        return BreakpointMetrics(
            observationCount = observations.size,
            buildHashChangeCount = observations.count { observation -> observation.buildHashChanged },
            talentDistribution = breakpointPayoffs.groupingBy { payoff -> payoff.talentId }.eachCount().toSortedMap(),
            effectDistribution =
                breakpointPayoffs
                    .flatMap { payoff -> payoff.unlockedEffectKinds }
                    .groupingBy { effectKind -> effectKind }
                    .eachCount()
                    .toSortedMap(),
        )
    }

    private fun affixSynergyMetrics(reports: List<ScenarioReport>): AffixSynergyMetrics =
        AffixSynergyMetrics(
            activationCount = reports.sumOf(ScenarioReport::affixSynergyActivationCount),
            distribution =
                reports
                    .flatMap { report -> report.affixSynergyActivationDistribution.entries }
                    .groupingBy { (affixId, _) -> affixId }
                    .fold(0) { accumulator, (_, count) -> accumulator + count }
                    .toSortedMap(),
        )

    private fun synergyRewardMetrics(reports: List<ScenarioReport>): SynergyRewardMetrics =
        SynergyRewardMetrics(
            rewardCount =
                reports.sumOf { report ->
                    val synergyAffixIds = FOUNDATION_SYNERGY_AFFIX_IDS[report.professionId].orEmpty()
                    report.milestoneRewards.count { reward -> reward.affixIds.any(synergyAffixIds::contains) }
                },
            adoptionCount =
                reports.sumOf { report ->
                    val synergyAffixIds = FOUNDATION_SYNERGY_AFFIX_IDS[report.professionId].orEmpty()
                    report.milestoneRewards.count { reward -> reward.adoptedInFinalBuild && reward.affixIds.any(synergyAffixIds::contains) }
                },
            distribution =
                reports
                    .flatMap { report ->
                        val synergyAffixIds = FOUNDATION_SYNERGY_AFFIX_IDS[report.professionId].orEmpty()
                        report.milestoneRewards.flatMap { reward -> reward.affixIds.filter(synergyAffixIds::contains) }
                    }.groupingBy { affixId -> affixId }
                    .eachCount()
                    .toSortedMap(),
        )

    private fun aggregateReliquaryTagDistribution(reports: List<ScenarioReport>): Map<String, Int> =
        reports
            .flatMap { report -> report.lateRunReliquaryTagDistribution.entries }
            .groupingBy { entry -> entry.key }
            .fold(0) { accumulator, entry -> accumulator + entry.value }
            .toSortedMap()

    private fun terminalWeaponIdentitySummary(reports: List<ScenarioReport>): TerminalWeaponIdentitySummary {
        val missingTerminalWeaponReports =
            reports.filter { report -> report.terminalWeaponBaseId.isNullOrBlank() }
        require(missingTerminalWeaponReports.isEmpty()) {
            "Full-route reports missing terminalWeaponBaseId: " +
                missingTerminalWeaponReports.joinToString { report -> "${report.name}:${report.professionId}/${report.raceId}/${report.seed}" }
        }
        val terminalWeaponByReport =
            reports.associateWith { report -> requireNotNull(report.terminalWeaponBaseId) }
        val distribution =
            terminalWeaponByReport.entries
                .groupBy { (report, _) -> report.professionId }
                .mapValues { (_, entries) ->
                    entries.groupingBy { (_, weaponBaseId) -> weaponBaseId }.eachCount().toSortedMap()
                }.toSortedMap()
        val terminalWeaponCounts = terminalWeaponByReport.values.groupingBy { weaponBaseId -> weaponBaseId }.eachCount()
        val topWeapon = terminalWeaponCounts.maxByOrNull { (_, count) -> count }
        val professionTopWeaponBaseIds =
            distribution
                .mapValues { (_, professionDistribution) ->
                    professionDistribution.maxByOrNull { (_, count) -> count }?.key
                }.mapValues { (_, weaponBaseId) -> requireNotNull(weaponBaseId) }
        val professionTopWeaponSemanticTags =
            professionTopWeaponBaseIds.mapValues { (_, weaponBaseId) -> itemSemanticTagsById[weaponBaseId].orEmpty() }
        val alignedCount =
            terminalWeaponByReport.count { (report, weaponBaseId) ->
                isProfessionAlignedTerminalWeapon(professionId = report.professionId, weaponBaseId = weaponBaseId)
            }
        return TerminalWeaponIdentitySummary(
            fullRouteSampleCount = reports.size,
            terminalWeaponBaseDiversity = terminalWeaponCounts.keys.size,
            crossProfessionTopWeaponDominance =
                if (reports.isEmpty()) {
                    0.0
                } else {
                    (topWeapon?.value ?: 0).toDouble() / reports.size.toDouble()
                },
            professionAlignedWeaponAdoptionRate =
                if (reports.isEmpty()) {
                    0.0
                } else {
                    alignedCount.toDouble() / reports.size.toDouble()
                },
            alignedFullRouteSampleCount = alignedCount,
            topWeaponBaseId = topWeapon?.key,
            topWeaponCount = topWeapon?.value ?: 0,
            professionTerminalWeaponDistribution = distribution,
            professionTopWeaponBaseIds = professionTopWeaponBaseIds,
            professionTopWeaponSemanticTags = professionTopWeaponSemanticTags,
        )
    }

    private fun aggregateZoneTraversalDiagnostics(
        reports: List<ScenarioReport>,
    ): Map<String, ZoneTraversalDiagnosticAggregate> =
        reports
            .flatMap(ScenarioReport::zoneTraversalDiagnostics)
            .groupBy(ZoneTraversalDiagnostic::zoneId)
            .toSortedMap(compareBy(::zoneDepth).thenBy { it })
            .mapValues { (_, diagnostics) ->
                val objectiveAcquireTurns = diagnostics.mapNotNull(ZoneTraversalDiagnostic::objectiveAcquireTurn)
                val objectiveAcquireHeadlessTurns = diagnostics.mapNotNull(ZoneTraversalDiagnostic::objectiveAcquireHeadlessTurnEquivalent)
                ZoneTraversalDiagnosticAggregate(
                    sampleCount = diagnostics.size,
                    visitCount = diagnostics.sumOf(ZoneTraversalDiagnostic::visitCount),
                    avgPlayerTurns = diagnostics.map(ZoneTraversalDiagnostic::playerTurns).average(),
                    avgEnemyTurns = diagnostics.map(ZoneTraversalDiagnostic::enemyTurns).average(),
                    avgEnemyTurnsPerPlayerTurn = diagnostics.map(ZoneTraversalDiagnostic::enemyTurnsPerPlayerTurn).average(),
                    avgVisibleHostileTurnCount = diagnostics.map(ZoneTraversalDiagnostic::visibleHostileTurnCount).average(),
                    avgLiveHostileWindow = diagnostics.map(ZoneTraversalDiagnostic::liveHostileWindow).average(),
                    maxVisibleHostiles = diagnostics.maxOfOrNull(ZoneTraversalDiagnostic::maxVisibleHostiles) ?: 0,
                    objectiveAcquireSampleCount = objectiveAcquireTurns.size,
                    avgObjectiveAcquireTurn =
                        objectiveAcquireTurns
                            .takeIf(List<Int>::isNotEmpty)
                            ?.average(),
                    avgObjectiveAcquireHeadlessTurnEquivalent =
                        objectiveAcquireHeadlessTurns
                            .takeIf(List<Int>::isNotEmpty)
                            ?.average(),
                    objectiveStateDistribution =
                        diagnostics
                            .mapNotNull(ZoneTraversalDiagnostic::objectiveStateAtExit)
                            .groupingBy { state -> state.name }
                            .eachCount()
                            .toSortedMap(),
                )
            }

    private fun professionCapstoneSummary(reports: List<ScenarioReport>): ProfessionCapstoneSummary {
        val breakdown =
            reports
                .groupBy(ScenarioReport::professionId)
                .toSortedMap()
                .mapValues { (professionId, professionReports) ->
                    val capstoneIds =
                        requireNotNull(PROFESSION_CAPSTONE_ITEM_IDS[professionId]) {
                            "Missing profession capstone configuration for '$professionId'."
                        }
                    val seenReports =
                        professionReports.filter { report ->
                            report.milestoneRewards.any { reward -> reward.baseItemId in capstoneIds }
                        }
                    val adoptedReports =
                        professionReports.filter { report ->
                            report.milestoneRewards.any { reward -> reward.baseItemId in capstoneIds && reward.adoptedInFinalBuild }
                        }
                    val nonWeaponPayoffReports =
                        professionReports.filter { report ->
                            report.milestoneRewards.any { reward ->
                                reward.baseItemId in capstoneIds &&
                                    reward.adoptedInFinalBuild &&
                                    reward.equipSlot != EquipSlot.WEAPON
                            }
                        }
                    ProfessionCapstoneBreakdown(
                        sampleCount = professionReports.size,
                        seenCount = seenReports.size,
                        adoptedCount = adoptedReports.size,
                        nonWeaponPayoffCount = nonWeaponPayoffReports.size,
                        seenItems = countMilestoneItems(seenReports, capstoneIds),
                        adoptedItems = countMilestoneItems(adoptedReports, capstoneIds, adoptedOnly = true),
                        nonWeaponPayoffItems =
                            countMilestoneItems(
                                nonWeaponPayoffReports,
                                capstoneIds,
                                adoptedOnly = true,
                                nonWeaponOnly = true,
                            ),
                    )
                }
        val sampleCount = reports.size
        val seenCount = breakdown.values.sumOf(ProfessionCapstoneBreakdown::seenCount)
        val adoptedCount = breakdown.values.sumOf(ProfessionCapstoneBreakdown::adoptedCount)
        val nonWeaponPayoffCount = breakdown.values.sumOf(ProfessionCapstoneBreakdown::nonWeaponPayoffCount)
        return ProfessionCapstoneSummary(
            professionCapstoneSeenRate = ratio(seenCount, sampleCount),
            professionCapstoneAdoptionRate = ratio(adoptedCount, sampleCount),
            nonWeaponBuildPayoffRate = ratio(nonWeaponPayoffCount, sampleCount),
            professionBreakdown = breakdown,
        )
    }

    private fun milestoneRewardSlotBalance(rewards: List<MilestoneRewardSummary>): MilestoneRewardSlotBalanceSummary {
        val counts =
            rewards
                .map(::milestoneRewardSlotFamily)
                .groupingBy { slotFamily -> slotFamily }
                .eachCount()
                .toSortedMap()
        return MilestoneRewardSlotBalanceSummary(totalCount = rewards.size, counts = counts)
    }

    private fun milestoneRewardSlotFamily(reward: MilestoneRewardSummary): MilestoneRewardSlotFamily {
        val base = itemBaseById[reward.baseItemId]
            ?: return MilestoneRewardSlotFamily.CONSUMABLE_OR_UTILITY
        return milestoneRewardSlotFamily(base) ?: MilestoneRewardSlotFamily.CONSUMABLE_OR_UTILITY
    }

    private fun rewardScoreBreakdownSamples(reports: List<ScenarioReport>): List<MilestoneRewardScoreSample> =
        reports
            .sortedWith(compareBy(ScenarioReport::professionId).thenBy(ScenarioReport::name))
            .flatMap { report ->
                val minimumSampleCount = minimumRewardScoreSamplesForTerminalRun(report)
                require(report.milestoneRewardScoreSamples.size >= minimumSampleCount) {
                    "Terminal run '${report.name}' must expose at least $minimumSampleCount reward score samples, " +
                        "actual=${report.milestoneRewardScoreSamples.size}."
                }
                val samples =
                    report.milestoneRewardScoreSamples.map { sample ->
                        if (sample.scenarioName.isBlank()) {
                            sample.copy(scenarioName = report.name)
                        } else {
                            sample
                        }
                    }
                val sortedSamples =
                    samples.sortedWith(
                        compareByDescending<MilestoneRewardScoreSample>(MilestoneRewardScoreSample::selected)
                            .thenBy(MilestoneRewardScoreSample::sourceId)
                            .thenBy(MilestoneRewardScoreSample::baseItemId),
                    )
                val requiredEvidenceSamples =
                    listOfNotNull(
                        sortedSamples.firstOrNull(MilestoneRewardScoreSample::selected),
                        sortedSamples.firstOrNull { sample -> !sample.legal },
                        sortedSamples.firstOrNull { sample ->
                            !sample.legal &&
                                (
                                    sample.scoreBreakdown.professionCapstoneBonus > 0 ||
                                        sample.scoreBreakdown.nonWeaponPayoffBonus > 0
                                )
                        },
                        sortedSamples.firstOrNull { sample -> sample.scoreBreakdown.wrongProfessionCapstonePenalty > 0 },
                        sortedSamples.firstOrNull { sample -> sample.scoreBreakdown.nonWeaponPayoffBonus > 0 },
                        sortedSamples.firstOrNull {
                            sample -> sample.slotFamily != MilestoneRewardSlotFamily.OFF_HAND && sample.scoreBreakdown.lateCommonPenalty > 0
                        },
                        sortedSamples.firstOrNull { sample ->
                            sample.baseItemId == "basic_shield" &&
                                (
                                    sample.scoreBreakdown.lateCommonPenalty > 0 ||
                                        sample.rejectionReason == "LATE_COMMON_ROGUE_OFF_HAND"
                                )
                        },
                    )
                (requiredEvidenceSamples + sortedSamples)
                    .distinctBy { sample -> rewardScoreSampleKey(sample) }
                    .take(MIN_REWARD_SCORE_SAMPLES_PER_TERMINAL_RUN)
            }

    private fun minimumRewardScoreSamplesForTerminalRun(report: ScenarioReport): Int =
        (report.zonePath.distinct().size * MIN_REWARD_SCORE_SAMPLES_PER_ZONE)
            .coerceIn(
                minimumValue = MIN_REWARD_SCORE_SAMPLES_PER_SHORT_ROUTE,
                maximumValue = MIN_REWARD_SCORE_SAMPLES_PER_TERMINAL_RUN,
            )

    private fun assertRewardScoreBreakdownSamplesCoverPr03(samples: List<MilestoneRewardScoreSample>) {
        assertTrue(
            samples.any { sample -> sample.selected },
            "rewardScoreBreakdownSamples must include selected milestone candidates.",
        )
        assertTrue(
            samples.any { sample -> !sample.legal },
            "rewardScoreBreakdownSamples must include rejected milestone candidates.",
        )
        assertTrue(
            samples.any { sample ->
                !sample.legal &&
                    (
                        sample.scoreBreakdown.professionCapstoneBonus > 0 ||
                            sample.scoreBreakdown.nonWeaponPayoffBonus > 0
                    )
            },
            "rewardScoreBreakdownSamples must include a rejected capstone candidate.",
        )
        assertTrue(
            samples.any { sample -> sample.scoreBreakdown.wrongProfessionCapstonePenalty > 0 },
            "rewardScoreBreakdownSamples must include a wrong-profession capstone penalty sample.",
        )
        assertTrue(
            samples.any { sample -> sample.scoreBreakdown.nonWeaponPayoffBonus > 0 },
            "rewardScoreBreakdownSamples must include a non-weapon payoff bonus sample.",
        )
        assertTrue(
            samples.any { sample -> sample.scoreBreakdown.professionCapstoneBonus > 0 },
            "rewardScoreBreakdownSamples must include a profession capstone bonus sample.",
        )
        assertTrue(
            samples.any { sample -> sample.scoreBreakdown.terminalIdentityBonus > 0 },
            "rewardScoreBreakdownSamples must include a terminal identity build bonus sample.",
        )
        assertTrue(
            samples.any { sample -> sample.scoreBreakdown.slotRotationBonus > 0 },
            "rewardScoreBreakdownSamples must include a slot rotation bonus sample.",
        )
        assertTrue(
            samples.any { sample -> sample.scoreBreakdown.duplicateSlotPenalty > 0 },
            "rewardScoreBreakdownSamples must include a duplicate slot penalty sample.",
        )
        assertTrue(
            samples.mapNotNull(MilestoneRewardScoreSample::slotFamily).toSet().containsAll(
                setOf(
                    MilestoneRewardSlotFamily.WEAPON,
                    MilestoneRewardSlotFamily.OFF_HAND,
                    MilestoneRewardSlotFamily.ARMOR,
                    MilestoneRewardSlotFamily.ACCESSORY,
                ),
            ),
            "rewardScoreBreakdownSamples must retain reward/build slot-family coverage.",
        )
        assertTrue(
            samples.map(MilestoneRewardScoreSample::rewardSource).toSet().containsAll(
                setOf(
                    MilestoneRewardSource.ROUTE,
                    MilestoneRewardSource.SUPPORT,
                    MilestoneRewardSource.CACHE,
                ),
            ),
            "rewardScoreBreakdownSamples must retain route/support/cache owner surfaces.",
        )
    }

    private fun rewardScoreSampleKey(sample: MilestoneRewardScoreSample): String =
        listOf(
            sample.scenarioName,
            sample.professionId,
            sample.sourceId,
            sample.baseItemId,
            sample.selected.toString(),
            sample.legal.toString(),
            sample.rejectionReason.orEmpty(),
            sample.scoreBreakdown.totalScore.toString(),
        ).joinToString("|")

    private fun capstoneAdoptionBySlot(reports: List<ScenarioReport>): Map<String, Int> =
        reports
            .flatMap { report ->
                val capstoneIds = PROFESSION_CAPSTONE_ITEM_IDS[report.professionId].orEmpty()
                report.milestoneRewards.filter { reward -> reward.adoptedInFinalBuild && reward.baseItemId in capstoneIds }
            }.groupingBy { reward -> reward.equipSlot.name }
            .eachCount()
            .toSortedMap()

    private fun professionTerminalIdentityItemIds(reports: List<ScenarioReport>): Map<String, List<String>> =
        reports
            .groupBy(ScenarioReport::professionId)
            .toSortedMap()
            .mapValues { (professionId, professionReports) ->
                val capstoneIds = PROFESSION_CAPSTONE_ITEM_IDS[professionId].orEmpty()
                professionReports
                    .flatMap(ScenarioReport::milestoneRewards)
                    .filter { reward -> reward.adoptedInFinalBuild && reward.baseItemId in capstoneIds }
                    .map(MilestoneRewardSummary::baseItemId)
                    .distinct()
                    .sorted()
            }

    private fun wrongProfessionCapstoneAdoptionCount(reports: List<ScenarioReport>): Int =
        reports.sumOf { report ->
            val capstoneIds = PROFESSION_CAPSTONE_ITEM_IDS[report.professionId].orEmpty()
            report.milestoneRewards.count { reward ->
                reward.adoptedInFinalBuild &&
                    "capstone" in itemSemanticTagsById[reward.baseItemId].orEmpty() &&
                    reward.baseItemId !in capstoneIds
            }
        }

    private fun professionTreeChoiceMetrics(reports: List<ScenarioReport>): ProfessionTreeChoiceMetrics {
        val includedTerminalReports =
            reports
                .filter { report -> report.professionId in PROFESSION_TREE_RELEASE_PROFESSIONS }
                .filter { report -> report.outcome.isTerminal }
        val breakpointPreviewReports = includedTerminalReports.filter(ScenarioReport::breakpointPreviewAvailable)
        return ProfessionTreeChoiceMetrics(
            starterProfessionTalentMaxCount = includedTerminalReports.maxOfOrNull(ScenarioReport::starterProfessionTalentCount) ?: 0,
            learnedTalentChoiceEventRate =
                ratio(
                    includedTerminalReports.count { report -> report.learnedTalentChoiceEventCount > 0 },
                    includedTerminalReports.size,
                ),
            multiTreeInvestmentAboveThresholdRate =
                ratio(
                    includedTerminalReports.count(ScenarioReport::multiTreeInvestmentAboveThreshold),
                    includedTerminalReports.size,
                ),
            breakpointChoiceEventRate =
                ratio(
                    breakpointPreviewReports.count { report -> report.breakpointChoiceEventCount > 0 },
                    breakpointPreviewReports.size,
                ),
            talentTreePrimaryInvestmentDistribution =
                includedTerminalReports
                    .map { report -> report.talentTreePrimaryInvestmentTreeId ?: "none" }
                    .groupingBy { treeId -> treeId }
                    .eachCount()
                    .toSortedMap(),
            talentReserveSwapCount = includedTerminalReports.sumOf(ScenarioReport::talentReserveSwapCount),
            rankBreakpointAdoptionByTalent =
                includedTerminalReports
                    .flatMap { report -> report.rankBreakpointAdoptionByTalent.entries }
                    .groupingBy { (talentId, _) -> talentId }
                    .fold(0) { accumulator, (_, count) -> accumulator + count }
                    .toSortedMap(),
            autoLearnedNonStarterTalentCount = includedTerminalReports.sumOf(ScenarioReport::autoLearnedNonStarterTalentCount),
        )
    }

    private fun inscriptionShopMetrics(
        reports: List<ScenarioReport>,
        replacementProbe: InscriptionReplacementProbe,
    ): InscriptionShopMetrics {
        val terminalReports = reports.filter { report -> report.isFullRoute && report.outcome.isTerminal }
        val installOrReplaceCount = terminalReports.count { report -> report.inscriptionInstallCount + report.inscriptionReplaceCount > 0 }
        val seenOfferCount = terminalReports.sumOf(ScenarioReport::shopInscriptionOfferSeenCount)
        val purchaseCount = terminalReports.sumOf(ScenarioReport::shopInscriptionOfferPurchaseCount)
        return InscriptionShopMetrics(
            starterInscriptionMaxCount = terminalReports.maxOfOrNull(ScenarioReport::startingInscriptionCount) ?: 0,
            fullSlotInscriptionPurchaseBlockedWithoutReplacementCount =
                terminalReports.sumOf(ScenarioReport::fullSlotInscriptionPurchaseBlockedWithoutReplacementCount),
            fullSlotInscriptionPurchaseReplacementPromptCount =
                terminalReports.sumOf(ScenarioReport::fullSlotInscriptionPurchaseReplacementPromptCount),
            inscriptionInstallOrReplaceRate = ratio(installOrReplaceCount, terminalReports.size),
            terminalInscriptionLoadoutDiversity =
                terminalReports
                    .map { report -> report.terminalInscriptionLoadout.joinToString(separator = "|") }
                    .filter(String::isNotBlank)
                    .toSet()
                    .size,
            inscriptionCategoryCountDistribution =
                terminalReports
                    .map { report ->
                        report.terminalInscriptionCategoryCounts.entries.joinToString(separator = "|") { (categoryId, count) -> "$categoryId:$count" }
                    }.filter(String::isNotBlank)
                    .groupingBy { summary -> summary }
                    .eachCount()
                    .toSortedMap(),
            shopInscriptionOfferConversionRate = ratio(purchaseCount, seenOfferCount),
            inscriptionReplaceReasonDistribution =
                terminalReports
                    .flatMap { report -> report.inscriptionReplaceReasonDistribution.entries }
                    .groupBy(Map.Entry<String, Int>::key)
                    .mapValues { (_, entries) -> entries.sumOf(Map.Entry<String, Int>::value) }
                    .toSortedMap(),
            replacementProbeSuccessCount = replacementProbe.successCount,
            inscriptionPurchaseCancelledAfterReplacementPrompt =
                terminalReports.sumOf(ScenarioReport::inscriptionPurchaseCancelledAfterReplacementPrompt),
            shopPurchaseDeniedInsufficientGoldCount =
                terminalReports.sumOf(ScenarioReport::shopPurchaseDeniedInsufficientGoldCount),
        )
    }

    private fun inscriptionReplacementProbe(): InscriptionReplacementProbe {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr02"))
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("phase4-v4-pr02-longrun-replacement-probe")),
                    options = scenario.toSessionOptions(),
                ),
            )
        check(
            session.perform(
                PlayerCommand.Validation(
                    ValidationAction.Phase4V4ScenarioAction(
                        scenarioId = scenario.id,
                        actionId = ValidationScenarioActionId.PREPARE_SECONDARY_SCENE,
                    ),
                ),
            ),
        ) {
            "Phase4 v4 PR-02 replacement probe could not prepare the full-slot validation scene."
        }
        val controlledPhaseOffer =
            requireNotNull(
                session.renderSnapshot().uiState.activeShop?.offers?.firstOrNull { offer ->
                    offer.labelKey == "inscription.controlled_phase.name"
                },
            ) {
                "Phase4 v4 PR-02 replacement probe requires a controlled_phase shop offer."
            }
        check(
            session.perform(
                PlayerCommand.BuyShopOffer(
                    index = controlledPhaseOffer.index,
                    offerFingerprint = controlledPhaseOffer.offerFingerprint,
                ),
            ),
        ) {
            "Phase4 v4 PR-02 replacement probe could not open the replacement prompt."
        }
        val prompt = requireNotNull(session.renderSnapshot().uiState.activeShop?.inscriptionReplacementPrompt) {
            "Phase4 v4 PR-02 replacement probe expected an active replacement prompt."
        }
        check(prompt.currentSlots.size == 4) {
            "Phase4 v4 PR-02 replacement probe must exercise a full-slot loadout, actual=${prompt.currentSlots.size}."
        }
        val botCommand = SmokeBot().decide(RunObservationCapture.capture(session, turnIndex = 0))
        val replacementCommand =
            botCommand as? PlayerCommand.BuyShopOffer
                ?: error("SmokeBot must choose a replacement BuyShopOffer, actual=$botCommand.")
        val selectedHotkey =
            replacementCommand.replacementHotkey
                ?: error("SmokeBot replacement command must include replacementHotkey.")
        check(session.perform(replacementCommand)) {
            "Phase4 v4 PR-02 replacement probe command was rejected: $replacementCommand."
        }
        val summary = session.currentInscriptionRunSummary()
        val expectedTerminalLoadout = listOf("healing_light", "controlled_phase", "iron_shield", "purge")
        val probePassed =
            summary.replaceCount > 0 &&
                selectedHotkey == 6 &&
                prompt.candidate.inscriptionId == "controlled_phase" &&
                summary.terminalLoadout == expectedTerminalLoadout
        return InscriptionReplacementProbe(
            successCount = if (probePassed) 1 else 0,
            installCount = summary.installCount,
            replaceCount = summary.replaceCount,
            selectedHotkey = selectedHotkey,
            candidateInscriptionId = prompt.candidate.inscriptionId,
            terminalLoadout = summary.terminalLoadout,
        )
    }

    private fun countMilestoneItems(
        reports: List<ScenarioReport>,
        capstoneIds: Set<String>,
        adoptedOnly: Boolean = false,
        nonWeaponOnly: Boolean = false,
    ): Map<String, Int> =
        reports
            .flatMap(ScenarioReport::milestoneRewards)
            .filter { reward -> reward.baseItemId in capstoneIds }
            .filter { reward -> !adoptedOnly || reward.adoptedInFinalBuild }
            .filter { reward -> !nonWeaponOnly || reward.equipSlot != EquipSlot.WEAPON }
            .groupingBy(MilestoneRewardSummary::baseItemId)
            .eachCount()
            .toSortedMap()

    private fun ratio(
        numerator: Int,
        denominator: Int,
    ): Double =
        if (denominator == 0) {
            0.0
        } else {
            numerator.toDouble() / denominator.toDouble()
        }

    private fun criticalPathZoneIds(): List<String> =
        FOUNDATION_ZONE_ROUTE.drop(1).dropLast(1)

    private fun criticalPathZoneDesignAudit(): Map<String, CriticalPathZoneDesignAuditEntry> {
        val objectivesById = schemaCatalog.objectiveSets.associateBy { objective -> objective.id }
        val profilesByZone = schemaCatalog.zoneMapgenProfiles.associateBy { profile -> profile.zoneId }
        return criticalPathZoneIds()
            .map { zoneId ->
                val zone = requireNotNull(schemaCatalog.zones.firstOrNull { candidate -> candidate.id == zoneId }) {
                    "Missing critical-path zone '$zoneId' in schema catalog."
                }
                val objective = zone.objectiveSetId?.let(objectivesById::get)
                val mechanicClassification = ZoneMechanicRuntime.classifyMechanics(zone)
                zoneId to
                    CriticalPathZoneDesignAuditEntry(
                        zoneId = zoneId,
                        floorCount = zone.floorCount,
                        mapSize = "${zone.mapSize.width}x${zone.mapSize.height}",
                        worldRole = zone.worldRole,
                        specialMechanics = zone.specialMechanics,
                        allMechanicTerms = mechanicClassification.allMechanicTerms,
                        runtimeHookIds = mechanicClassification.runtimeHookIds,
                        flavorOnlyMechanics = mechanicClassification.flavorOnlyMechanics,
                        mechanicTermsPartitioned = mechanicClassification.termsPartitioned,
                        mechanicsWithoutDedicatedRuntimeHook = mechanicClassification.mechanicsWithoutDedicatedRuntimeHook,
                        monsterPoolCount = zone.monsterPools.size,
                        elitePoolCount = zone.elitePools.size,
                        bossEncounterId = zone.bossEncounterId,
                        objectiveSetId = zone.objectiveSetId,
                        objectiveCompletionRule = objective?.completionRule,
                        objectivePlacements =
                            objective
                                ?.placements
                                ?.sortedWith(compareBy({ it.floor }, { it.interactableId }))
                                ?.map { placement ->
                                    "${placement.interactableId}@floor${placement.floor}:${placement.anchor}+${placement.offset.x},${placement.offset.y}"
                                }.orEmpty(),
                        terrainTagWeights =
                            profilesByZone[zoneId]
                                ?.terrainTagWeights
                                ?.entries
                                ?.sortedBy { entry -> entry.key.name }
                                ?.associate { entry -> entry.key.name to entry.value.toDouble() }
                                .orEmpty(),
                    )
            }.toMap(linkedMapOf())
    }

    private fun isProfessionAlignedTerminalWeapon(
        professionId: String,
        weaponBaseId: String,
    ): Boolean {
        val rule =
            requireNotNull(PROFESSION_ALIGNED_TERMINAL_WEAPON_RULES[professionId]) {
                "Missing profession-aligned terminal weapon rule for '$professionId'."
            }
        val itemTags = itemTagsById[weaponBaseId].orEmpty()
        return rule.matches(weaponBaseId = weaponBaseId, itemTags = itemTags)
    }

    private data class BreakpointMetrics(
        val observationCount: Int,
        val buildHashChangeCount: Int,
        val talentDistribution: Map<String, Int>,
        val effectDistribution: Map<String, Int>,
    )

    private data class AffixSynergyMetrics(
        val activationCount: Int,
        val distribution: Map<String, Int>,
    )

    private data class SynergyRewardMetrics(
        val rewardCount: Int,
        val adoptionCount: Int,
        val distribution: Map<String, Int>,
    )

    private data class TerminalWeaponIdentitySummary(
        val fullRouteSampleCount: Int,
        val terminalWeaponBaseDiversity: Int,
        val crossProfessionTopWeaponDominance: Double,
        val professionAlignedWeaponAdoptionRate: Double,
        val alignedFullRouteSampleCount: Int,
        val topWeaponBaseId: String?,
        val topWeaponCount: Int,
        val professionTerminalWeaponDistribution: Map<String, Map<String, Int>>,
        val professionTopWeaponBaseIds: Map<String, String>,
        val professionTopWeaponSemanticTags: Map<String, Set<String>>,
    )

    private data class ProfessionCapstoneSummary(
        val professionCapstoneSeenRate: Double,
        val professionCapstoneAdoptionRate: Double,
        val nonWeaponBuildPayoffRate: Double,
        val professionBreakdown: Map<String, ProfessionCapstoneBreakdown>,
    )

    private data class ProfessionCapstoneBreakdown(
        val sampleCount: Int,
        val seenCount: Int,
        val adoptedCount: Int,
        val nonWeaponPayoffCount: Int,
        val seenItems: Map<String, Int>,
        val adoptedItems: Map<String, Int>,
        val nonWeaponPayoffItems: Map<String, Int>,
    )

    private data class MilestoneRewardSlotBalanceSummary(
        val totalCount: Int,
        val counts: Map<MilestoneRewardSlotFamily, Int>,
    ) {
        val maxSlotShare: Double = counts.values.maxOfOrNull { count -> share(count) } ?: 0.0

        fun share(slotFamily: MilestoneRewardSlotFamily): Double = share(counts.getOrDefault(slotFamily, 0))

        private fun share(count: Int): Double =
            if (totalCount == 0) {
                0.0
            } else {
                count.toDouble() / totalCount.toDouble()
            }
    }

    private fun MilestoneRewardScoreSample.toJson() =
        buildJsonObject {
            put("scenarioName", scenarioName)
            put("rewardSource", rewardSource.name)
            put("sourceId", sourceId)
            put("professionId", professionId)
            put("zoneId", zoneId)
            put("baseItemId", baseItemId)
            put("selected", selected)
            put("legal", legal)
            rejectionReason?.let { put("rejectionReason", it) }
            slotFamily?.let { put("slotFamily", it.name) }
            putJsonObject("scoreBreakdown") {
                put("baseScore", scoreBreakdown.baseScore)
                put("professionCapstoneBonus", scoreBreakdown.professionCapstoneBonus)
                put("nonWeaponPayoffBonus", scoreBreakdown.nonWeaponPayoffBonus)
                put("wrongProfessionCapstonePenalty", scoreBreakdown.wrongProfessionCapstonePenalty)
                put("slotRotationBonus", scoreBreakdown.slotRotationBonus)
                put("duplicateSlotPenalty", scoreBreakdown.duplicateSlotPenalty)
                put("terminalIdentityBonus", scoreBreakdown.terminalIdentityBonus)
                put("lateCommonPenalty", scoreBreakdown.lateCommonPenalty)
                put("positiveBonusBeforeCap", scoreBreakdown.positiveBonusBeforeCap)
                put("positiveBonusCap", scoreBreakdown.positiveBonusCap)
                put("positiveBonusAfterCap", scoreBreakdown.positiveBonusAfterCap)
                put("totalScore", scoreBreakdown.totalScore)
            }
        }

    private data class ProfessionTreeChoiceMetrics(
        val starterProfessionTalentMaxCount: Int,
        val learnedTalentChoiceEventRate: Double,
        val multiTreeInvestmentAboveThresholdRate: Double,
        val breakpointChoiceEventRate: Double,
        val talentTreePrimaryInvestmentDistribution: Map<String, Int>,
        val talentReserveSwapCount: Int,
        val rankBreakpointAdoptionByTalent: Map<String, Int>,
        val autoLearnedNonStarterTalentCount: Int,
    )

    private data class InscriptionShopMetrics(
        val starterInscriptionMaxCount: Int,
        val fullSlotInscriptionPurchaseBlockedWithoutReplacementCount: Int,
        val fullSlotInscriptionPurchaseReplacementPromptCount: Int,
        val inscriptionInstallOrReplaceRate: Double,
        val replacementProbeSuccessCount: Int,
        val terminalInscriptionLoadoutDiversity: Int,
        val inscriptionCategoryCountDistribution: Map<String, Int>,
        val shopInscriptionOfferConversionRate: Double,
        val inscriptionReplaceReasonDistribution: Map<String, Int>,
        val inscriptionPurchaseCancelledAfterReplacementPrompt: Int,
        val shopPurchaseDeniedInsufficientGoldCount: Int,
    )

    private data class InscriptionReplacementProbe(
        val successCount: Int,
        val installCount: Int,
        val replaceCount: Int,
        val selectedHotkey: Int,
        val candidateInscriptionId: String,
        val terminalLoadout: List<String>,
    )

    private data class ZoneTraversalDiagnosticAggregate(
        val sampleCount: Int,
        val visitCount: Int,
        val avgPlayerTurns: Double,
        val avgEnemyTurns: Double,
        val avgEnemyTurnsPerPlayerTurn: Double,
        val avgVisibleHostileTurnCount: Double,
        val avgLiveHostileWindow: Double,
        val maxVisibleHostiles: Int,
        val objectiveAcquireSampleCount: Int,
        val avgObjectiveAcquireTurn: Double?,
        val avgObjectiveAcquireHeadlessTurnEquivalent: Double?,
        val objectiveStateDistribution: Map<String, Int>,
    )

    private data class CriticalPathZoneDesignAuditEntry(
        val zoneId: String,
        val floorCount: Int,
        val mapSize: String,
        val worldRole: String,
        val specialMechanics: List<String>,
        val allMechanicTerms: List<String>,
        val runtimeHookIds: List<String>,
        val flavorOnlyMechanics: List<String>,
        val mechanicTermsPartitioned: Boolean,
        val mechanicsWithoutDedicatedRuntimeHook: List<String>,
        val monsterPoolCount: Int,
        val elitePoolCount: Int,
        val bossEncounterId: String?,
        val objectiveSetId: String?,
        val objectiveCompletionRule: String?,
        val objectivePlacements: List<String>,
        val terrainTagWeights: Map<String, Double>,
    )

    private data class ProfessionAlignedTerminalWeaponRule(
        val exactWeaponBaseIds: Set<String> = emptySet(),
        val requiredItemTags: Set<String> = emptySet(),
    ) {
        fun matches(
            weaponBaseId: String,
            itemTags: Set<String>,
        ): Boolean = weaponBaseId in exactWeaponBaseIds || itemTags.any(requiredItemTags::contains)
    }

    private companion object {
        private const val PROFESSION_TREE_CHOICE_PROBE_TALENT_POINTS = 6
        private const val MIN_REWARD_SCORE_SAMPLES_PER_TERMINAL_RUN = 20
        private const val MIN_REWARD_SCORE_SAMPLES_PER_SHORT_ROUTE = 8
        private const val MIN_REWARD_SCORE_SAMPLES_PER_ZONE = 4

        private val schemaCatalog = DataLoader().loadSchemaCatalog()
        private val PROFESSION_ALIGNED_TERMINAL_WEAPON_RULES: Map<String, ProfessionAlignedTerminalWeaponRule> =
            mapOf(
                "vanguard" to
                    ProfessionAlignedTerminalWeaponRule(
                        exactWeaponBaseIds = setOf("battle_axe", "long_sword"),
                        requiredItemTags = foundationBuildIdentityByProfessionId.getValue("vanguard").terminalIdentityTags,
                    ),
                "templar" to
                    ProfessionAlignedTerminalWeaponRule(
                        exactWeaponBaseIds = setOf("battle_axe", "long_sword"),
                        requiredItemTags = foundationBuildIdentityByProfessionId.getValue("templar").terminalIdentityTags,
                    ),
                "rogue" to
                    ProfessionAlignedTerminalWeaponRule(
                        exactWeaponBaseIds = setOf("short_sword", "hunter_bow"),
                        requiredItemTags = foundationBuildIdentityByProfessionId.getValue("rogue").terminalIdentityTags,
                    ),
                "arcanist" to
                    ProfessionAlignedTerminalWeaponRule(
                        exactWeaponBaseIds = setOf("arcane_staff"),
                        requiredItemTags = foundationBuildIdentityByProfessionId.getValue("arcanist").terminalIdentityTags,
                    ),
            )
        private val PROFESSION_CAPSTONE_ITEM_IDS: Map<String, Set<String>> = foundationProfessionCapstoneBaseIdsByProfessionId
        private val PROFESSION_TREE_RELEASE_PROFESSIONS: List<String> = listOf("vanguard", "arcanist", "rogue", "templar")
        private val PROFESSION_TREE_ADVANCED_REPORT_ONLY_PROFESSIONS: List<String> = listOf("berserker", "spellblade")
        private val PROFESSION_TREE_EXCLUDED_FROZEN_PROFESSIONS: List<String> = listOf("shadowblade", "warden")
        private val itemTagsById: Map<String, Set<String>> =
            schemaCatalog.itemBundle.items.associate { item -> item.id to item.tags.toSet() }
        private val itemSemanticTagsById: Map<String, Set<String>> =
            DataLoader().loadItemBundle().baseItems.associate { item -> item.id to item.tags.toSet() }
        private val itemBaseById = DataLoader().loadItemBundle().baseItems.associateBy { item -> item.id }
    }

    private fun sampleScenarioReport(
        name: String,
        professionId: String,
        terminalWeaponBaseId: String?,
        zoneTraversalDiagnostics: List<ZoneTraversalDiagnostic> = emptyList(),
        milestoneRewards: List<MilestoneRewardSummary> = emptyList(),
    ): ScenarioReport =
        ScenarioReport(
            name = name,
            seed = 1L,
            professionId = professionId,
            success = true,
            outcome = RunOutcome.Victory(floor = 1),
            floorReached = 1,
            turns = 10,
            goalReached = true,
            terminalWeaponBaseId = terminalWeaponBaseId,
            zoneTraversalDiagnostics = zoneTraversalDiagnostics,
            milestoneRewards = milestoneRewards,
        )

    private fun sampleMilestoneReward(
        baseItemId: String,
        equipSlot: EquipSlot,
        adoptedInFinalBuild: Boolean,
    ): MilestoneRewardSummary =
        MilestoneRewardSummary(
            rewardSource = MilestoneRewardSource.ROUTE,
            sourceId = "test:$baseItemId",
            zoneId = "deep_iron_pit",
            baseItemId = baseItemId,
            equipSlot = equipSlot,
            qualityTier = RarityTier.RARE,
            buildHashAtGrant = "before",
            equippedBaseItemIdBeforeReward = null,
            equippedBaseItemIdAtRunEnd = if (adoptedInFinalBuild) baseItemId else "other:$baseItemId",
            adoptedInFinalBuild = adoptedInFinalBuild,
        )
}
