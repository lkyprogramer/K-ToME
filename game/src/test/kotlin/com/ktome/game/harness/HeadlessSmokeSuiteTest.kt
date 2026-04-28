package com.ktome.game.harness

import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.FOUNDATION_ZONE_ID
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.PlayerCommand
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class HeadlessSmokeSuiteTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("headlessSmoke")
    fun `pr headless smoke scenarios are green`() {
        val harness = HeadlessRunHarness(rootDir = tempDir)
        val reports =
            listOf(
                harness.run(
                    ScenarioSpec(
                        name = "vanguard-shattered-outpost",
                        seed = 20260312L,
                        zoneId = FOUNDATION_ZONE_ID,
                        professionId = FOUNDATION_PROFESSION_ID,
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 0,
                        maxTurns = 450,
                        goal = ScenarioGoal.ReachFloor(2),
                        assertions = listOf(ScenarioAssertion.ReachedFloorAtLeast(2), ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                ),
                harness.run(
                    ScenarioSpec(
                        name = "arcanist-shattered-outpost-save-load",
                        seed = 20260313L,
                        zoneId = FOUNDATION_ZONE_ID,
                        professionId = "arcanist",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 0,
                        maxTurns = 450,
                        goal = ScenarioGoal.ReachFloor(2),
                        saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 10),
                        assertions =
                            listOf(
                                ScenarioAssertion.ReachedFloorAtLeast(2),
                                ScenarioAssertion.CheckpointRoundTrip,
                                ScenarioAssertion.NoFailure,
                                ScenarioAssertion.NoStall,
                            ),
                    ),
                ),
                harness.run(
                    ScenarioSpec(
                        name = "rogue-deep-iron-pit",
                        seed = 20260316L,
                        zoneId = "deep_iron_pit",
                        professionId = "rogue",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 2,
                        maxTurns = 600,
                        goal = ScenarioGoal.ReachFloor(2),
                        assertions = listOf(ScenarioAssertion.ReachedFloorAtLeast(2), ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                ),
                harness.run(
                    ScenarioSpec(
                        name = "templar-grey-gate-depths",
                        seed = 20260317L,
                        zoneId = "grey_gate_depths",
                        professionId = "templar",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 3,
                        maxTurns = 600,
                        goal = ScenarioGoal.ReachFloor(2),
                        assertions = listOf(ScenarioAssertion.ReachedFloorAtLeast(2), ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
                    ),
                ),
            )

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "headless-smoke",
            payload =
                buildJsonArray {
                    reports.forEach { report ->
                        add(report.toJson())
                    }
                },
            markdown =
                buildString {
                    appendLine("# Headless Smoke")
                    reports.forEach { report ->
                        appendLine(
                            "- ${report.name}: success=${report.success}, zone=${report.zoneId}, routeIndex=${report.routeIndex}, profession=${report.professionId}, floor=${report.floorReached}, turns=${report.turns}, outcome=${report.outcome}",
                        )
                    }
                },
        )

        assertTrue(reports.all { it.success }, reports.joinToString(separator = "\n") { "${it.name}: ${it.failureReason ?: it.stuckReason ?: it.assertionFailures.joinToString()}" })
    }

    @Test
    fun `harness reports invalid talent commands instead of masking them`() {
        val harness =
            HeadlessRunHarness(
                rootDir = tempDir,
                bot =
                    object : RunBot {
                        override fun decide(observation: RunObservation): PlayerCommand = PlayerCommand.UseTalent(slot = 99)
                    },
            )

        val report =
            harness.run(
                ScenarioSpec(
                    name = "invalid-talent-fallback",
                    seed = 20260312L,
                    zoneId = FOUNDATION_ZONE_ID,
                    professionId = FOUNDATION_PROFESSION_ID,
                    zoneRoute = FOUNDATION_ZONE_ROUTE,
                    routeIndex = 0,
                    maxTurns = 1,
                    goal = ScenarioGoal.SurviveTurns(1),
                    assertions = listOf(ScenarioAssertion.NoStall),
                ),
            )

        assertFalse(report.success, "Expected invalid talent command to fail the harness gate.")
        assertTrue(
            report.failureReason?.startsWith("Command rejected: UseTalent(99)") == true,
            "Expected harness to surface the rejected invalid talent command, actual=${report.failureReason}",
        )
    }
}

internal fun ScenarioReport.toJson() =
    buildJsonObject {
        put("name", name)
        put("seed", seed)
        put("zoneId", zoneId)
        put("professionId", professionId)
        put("raceId", raceId)
        put("routeIndex", routeIndex)
        put("scenarioType", scenarioType.reportValue)
        put("isFullRoute", isFullRoute)
        put("finalZoneId", finalZoneId)
        put("zoneRouteHash", zoneRouteHash)
        put("buildId", buildId)
        put("phaseId", phaseId)
        put("rulesetVersion", rulesetVersion)
        put("traceSchemaVersion", traceSchemaVersion)
        put("corpusId", corpusId)
        put("localeId", localeId)
        put("profileId", profileId)
        buildHash?.let { put("buildHash", it) }
        terminalWeaponBaseId?.let { put("terminalWeaponBaseId", it) }
        putJsonArray("breakpointPayoffs") {
            breakpointPayoffs.forEach { payoff ->
                add(
                    buildJsonObject {
                        put("talentId", payoff.talentId)
                        put("treeId", payoff.treeId)
                        put("achievedRank", payoff.achievedRank)
                        put("breakpointRank", payoff.breakpointRank)
                        putJsonArray("unlockedEffectKinds") {
                            payoff.unlockedEffectKinds.forEach { effectKind -> add(JsonPrimitive(effectKind)) }
                        }
                    },
                )
            }
        }
        putJsonArray("breakpointPayoffObservations") {
            breakpointPayoffObservations.forEach { observation ->
                add(
                    buildJsonObject {
                        put("talentId", observation.talentId)
                        put("treeId", observation.treeId)
                        put("achievedRank", observation.achievedRank)
                        put("breakpointRank", observation.breakpointRank)
                        put("turnIndex", observation.turnIndex)
                        put("headlessTurnEquivalent", observation.headlessTurnEquivalent)
                        put("buildHashBeforeUnlock", observation.buildHashBeforeUnlock)
                        put("buildHashAfterUnlock", observation.buildHashAfterUnlock)
                        put("buildHashChanged", observation.buildHashChanged)
                        putJsonArray("unlockedEffectKinds") {
                            observation.unlockedEffectKinds.forEach { effectKind -> add(JsonPrimitive(effectKind)) }
                        }
                    },
                )
            }
        }
        put("cadenceRewardCount", cadenceRewardCount)
        put("shopRefreshPurchaseCount", shopRefreshPurchaseCount)
        put("lateRunReliquaryPurchaseCount", lateRunReliquaryPurchaseCount)
        put("lateRunReliquaryVisitCount", lateRunReliquaryVisitCount)
        put("lateRunReliquaryRefreshCount", lateRunReliquaryRefreshCount)
        put("lateRunReliquaryItemPurchaseCount", lateRunReliquaryItemPurchaseCount)
        put("lateRunReliquaryNonMandatoryPurchaseCount", lateRunReliquaryNonMandatoryPurchaseCount)
        put("lateRunReliquaryShardSpent", lateRunReliquaryShardSpent)
        putJsonObject("lateRunReliquaryTagDistribution") {
            lateRunReliquaryTagDistribution.forEach { (tag, count) ->
                put(tag, count)
            }
        }
        put("affixSynergyActivationCount", affixSynergyActivationCount)
        putJsonObject("affixSynergyActivationDistribution") {
            affixSynergyActivationDistribution.forEach { (affixId, count) ->
                put(affixId, count)
            }
        }
        put("starterProfessionTalentCount", starterProfessionTalentCount)
        put("learnedTalentChoiceEventCount", learnedTalentChoiceEventCount)
        put("learnableNonStarterTalentCount", learnableNonStarterTalentCount)
        put("breakpointChoiceEventCount", breakpointChoiceEventCount)
        put("breakpointPreviewAvailable", breakpointPreviewAvailable)
        putJsonObject("talentTreeInvestmentByTree") {
            talentTreeInvestmentByTree.forEach { (treeId, points) -> put(treeId, points) }
        }
        talentTreePrimaryInvestmentTreeId?.let { treeId -> put("talentTreePrimaryInvestmentTreeId", treeId) }
        put("talentTreePrimaryInvestmentPoints", talentTreePrimaryInvestmentPoints)
        put("multiTreeInvestmentAboveThreshold", multiTreeInvestmentAboveThreshold)
        put("talentReserveSwapCount", talentReserveSwapCount)
        putJsonObject("rankBreakpointAdoptionByTalent") {
            rankBreakpointAdoptionByTalent.forEach { (talentId, count) -> put(talentId, count) }
        }
        put("autoLearnedNonStarterTalentCount", autoLearnedNonStarterTalentCount)
        put("startingInscriptionCount", startingInscriptionCount)
        put("inscriptionInstallCount", inscriptionInstallCount)
        put("inscriptionReplaceCount", inscriptionReplaceCount)
        put("fullSlotInscriptionPurchaseBlockedWithoutReplacementCount", fullSlotInscriptionPurchaseBlockedWithoutReplacementCount)
        put("inscriptionPurchaseCancelledAfterReplacementPrompt", inscriptionPurchaseCancelledAfterReplacementPrompt)
        put("shopPurchaseDeniedInsufficientGoldCount", shopPurchaseDeniedInsufficientGoldCount)
        put("shopInscriptionOfferSeenCount", shopInscriptionOfferSeenCount)
        put("shopInscriptionOfferPurchaseCount", shopInscriptionOfferPurchaseCount)
        putJsonArray("terminalInscriptionLoadout") {
            terminalInscriptionLoadout.forEach { inscriptionId -> add(JsonPrimitive(inscriptionId)) }
        }
        putJsonObject("terminalInscriptionCategoryCounts") {
            terminalInscriptionCategoryCounts.forEach { (categoryId, count) -> put(categoryId, count) }
        }
        putJsonObject("inscriptionReplaceReasonDistribution") {
            inscriptionReplaceReasonDistribution.forEach { (reason, count) -> put(reason, count) }
        }
        putJsonArray("milestoneRewards") {
            milestoneRewards.forEach { reward ->
                add(
                    buildJsonObject {
                        put("rewardSource", reward.rewardSource.name)
                        put("sourceId", reward.sourceId)
                        put("zoneId", reward.zoneId)
                        put("baseItemId", reward.baseItemId)
                        put("equipSlot", reward.equipSlot.name)
                        put("qualityTier", reward.qualityTier.name)
                        put("buildHashAtGrant", reward.buildHashAtGrant)
                        putJsonArray("affixIds") {
                            reward.affixIds.forEach { affixId -> add(JsonPrimitive(affixId)) }
                        }
                        reward.equippedBaseItemIdBeforeReward?.let { put("equippedBaseItemIdBeforeReward", it) }
                        reward.equippedBaseItemIdAtRunEnd?.let { put("equippedBaseItemIdAtRunEnd", it) }
                        put("adoptedInFinalBuild", reward.adoptedInFinalBuild)
                    },
                )
            }
        }
        put("success", success)
        put("outcome", outcome.toString())
        put("floorReached", floorReached)
        put("turns", turns)
        put("headlessTurnEquivalent", headlessTurnEquivalent)
        put("goalReached", goalReached)
        failureReason?.let { put("failureReason", it) }
        stuckReason?.let { put("stuckReason", it) }
        put("checkpointRoundTripVerified", checkpointRoundTripVerified)
        putJsonArray("zonePath") { zonePath.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("zoneHeadlessMilestones") {
            zoneHeadlessMilestones.forEach { milestone ->
                add(
                    buildJsonObject {
                        put("zoneId", milestone.zoneId)
                        put("turnIndex", milestone.turnIndex)
                        put("headlessTurnEquivalent", milestone.headlessTurnEquivalent)
                        put("deltaTurns", milestone.deltaTurns)
                        put("deltaHeadlessTurns", milestone.deltaHeadlessTurns)
                    },
                )
            }
        }
        putJsonArray("zoneObjectiveSummaries") {
            zoneObjectiveSummaries.forEach { summary ->
                add(
                    buildJsonObject {
                        put("zoneId", summary.zoneId)
                        put("questId", summary.questId)
                        put("objectiveId", summary.objectiveId)
                        put("state", summary.state.name)
                        put("completionFlagGranted", summary.completionFlagGranted)
                    },
                )
            }
        }
        putJsonArray("zoneTraversalDiagnostics") {
            zoneTraversalDiagnostics.forEach { diagnostic ->
                add(
                    buildJsonObject {
                        put("zoneId", diagnostic.zoneId)
                        put("visitCount", diagnostic.visitCount)
                        put("playerTurns", diagnostic.playerTurns)
                        put("enemyTurns", diagnostic.enemyTurns)
                        put("enemyTurnsPerPlayerTurn", diagnostic.enemyTurnsPerPlayerTurn)
                        put("visibleHostileTurnCount", diagnostic.visibleHostileTurnCount)
                        put("liveHostileWindow", diagnostic.liveHostileWindow)
                        put("maxVisibleHostiles", diagnostic.maxVisibleHostiles)
                        diagnostic.objectiveAcquireTurn?.let { put("objectiveAcquireTurn", it) }
                        diagnostic.objectiveAcquireHeadlessTurnEquivalent?.let { put("objectiveAcquireHeadlessTurnEquivalent", it) }
                        diagnostic.objectiveStateAtExit?.let { put("objectiveStateAtExit", it.name) }
                    },
                )
            }
        }
        putJsonArray("captainEncounterTrace") {
            captainEncounterTrace.forEach { entry ->
                add(
                    buildJsonObject {
                        put("turnIndex", entry.turnIndex)
                        put("headlessTurnEquivalent", entry.headlessTurnEquivalent)
                        put("floor", entry.floor)
                        put("playerHp", entry.playerHp)
                        put("playerMaxHp", entry.playerMaxHp)
                        put("playerResourceCurrent", entry.playerResourceCurrent)
                        put("playerResourceMax", entry.playerResourceMax)
                        put("playerResourceTypeId", entry.playerResourceTypeId)
                        entry.captainHp?.let { put("captainHp", it) }
                        entry.captainMaxHp?.let { put("captainMaxHp", it) }
                        entry.captainDistance?.let { put("captainDistance", it) }
                        entry.command?.let { put("command", it) }
                        putJsonArray("recentMessages") { entry.recentMessages.forEach { add(JsonPrimitive(it)) } }
                        putJsonArray("recentEvents") { entry.recentEvents.forEach { add(JsonPrimitive(it)) } }
                    },
                )
            }
        }
        putJsonArray("lastCommands") { lastCommands.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("lastMessages") { lastMessages.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("eventTail") { eventTail.forEach { add(JsonPrimitive(it)) } }
    }
