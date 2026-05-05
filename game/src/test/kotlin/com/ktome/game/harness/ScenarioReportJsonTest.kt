package com.ktome.game.harness

import com.ktome.core.loot.RarityTier
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.item.EquipSlot
import com.ktome.core.profile.MilestoneRewardSummary
import com.ktome.core.run.RunOutcome
import com.ktome.game.BreakpointPayoffObservation
import com.ktome.game.BreakpointPayoffSummary
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScenarioReportJsonTest {
    @Test
    fun `scenario report json includes zone headless milestones and captain trace`() {
        val json =
            ScenarioReport(
                name = "long-run-full-templar-human",
                seed = 20260360L,
                professionId = "templar",
                raceId = "human",
                scenarioType = ScenarioType.FULL_ROUTE,
                success = false,
                outcome = RunOutcome.Defeat(floor = 2),
                floorReached = 2,
                turns = 153,
                headlessTurnEquivalent = 371,
                corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
                localeId = "en-US",
                buildHash = "templar#human#build",
                terminalWeaponBaseId = "forgebreaker_pick",
                breakpointPayoffs =
                    listOf(
                        BreakpointPayoffSummary(
                            talentId = "holy_mark",
                            treeId = "templar_smite",
                            achievedRank = 4,
                            breakpointRank = 4,
                            unlockedEffectKinds = listOf("apply_status:bane"),
                        ),
                    ),
                breakpointPayoffObservations =
                    listOf(
                        BreakpointPayoffObservation(
                            talentId = "holy_mark",
                            treeId = "templar_smite",
                            achievedRank = 4,
                            breakpointRank = 4,
                            unlockedEffectKinds = listOf("apply_status:bane"),
                            turnIndex = 88,
                            headlessTurnEquivalent = 190,
                            buildHashBeforeUnlock = "templar#human#pre-payoff",
                            buildHashAfterUnlock = "templar#human#build",
                            buildHashChanged = true,
                        ),
                    ),
                milestoneRewards =
                    listOf(
                        MilestoneRewardSummary(
                            rewardSource = MilestoneRewardSource.ROUTE,
                            sourceId = "route.greenwood_fringe.deep_iron_pit",
                            zoneId = "greenwood_fringe",
                            baseItemId = "forgebreaker_pick",
                            equipSlot = EquipSlot.WEAPON,
                            qualityTier = RarityTier.MAGIC,
                            buildHashAtGrant = "templar#human#grant",
                            affixIds = listOf("flaming", "fortified"),
                            equippedBaseItemIdBeforeReward = "long_sword",
                            equippedBaseItemIdAtRunEnd = "forgebreaker_pick",
                            adoptedInFinalBuild = true,
                        ),
                    ),
                affixSynergyActivationCount = 3,
                affixSynergyActivationDistribution =
                    linkedMapOf(
                        "of_smite" to 2,
                        "of_cleansing" to 1,
                    ),
                learnableNonStarterTalentCount = 2,
                goalReached = true,
                zonePath = listOf("shattered_outpost"),
                zoneHeadlessMilestones =
                    listOf(
                        ZoneHeadlessMilestone(
                            zoneId = "shattered_outpost",
                            turnIndex = 0,
                            headlessTurnEquivalent = 0,
                            deltaTurns = 0,
                            deltaHeadlessTurns = 0,
                        ),
                    ),
                zoneObjectiveSummaries =
                    listOf(
                        ZoneObjectiveSummary(
                            zoneId = "shattered_outpost",
                            questId = "quest.shattered_outpost",
                            objectiveId = "breach",
                            state = com.ktome.core.world.ObjectiveState.COMPLETED,
                            completionFlagGranted = true,
                        ),
                    ),
                zoneTraversalDiagnostics =
                    listOf(
                        ZoneTraversalDiagnostic(
                            zoneId = "shattered_outpost",
                            visitCount = 1,
                            playerTurns = 24,
                            enemyTurns = 31,
                            enemyTurnsPerPlayerTurn = 31.0 / 24.0,
                            visibleHostileTurnCount = 9,
                            liveHostileWindow = 4,
                            maxVisibleHostiles = 2,
                            objectiveAcquireTurn = 3,
                            objectiveAcquireHeadlessTurnEquivalent = 5,
                            objectiveStateAtExit = com.ktome.core.world.ObjectiveState.COMPLETED,
                        ),
                    ),
                captainEncounterTrace =
                    listOf(
                        CaptainEncounterTraceEntry(
                            turnIndex = 153,
                            headlessTurnEquivalent = 371,
                            floor = 2,
                            playerHp = 0,
                            playerMaxHp = 60,
                            playerResourceCurrent = 5,
                            playerResourceMax = 20,
                            playerResourceTypeId = "POSITIVE_ENERGY",
                            captainHp = 18,
                            captainMaxHp = 38,
                            captainDistance = 1,
                            command = "Move",
                            recentMessages = listOf("强盗队长 命中 英雄，造成 30 点伤害。"),
                            recentEvents = listOf("damage:12002->1:30"),
                        ),
                    ),
            ).toJson().jsonObject

        val milestones = json.requiredArray("zoneHeadlessMilestones")
        assertEquals(1, milestones.size)
        assertEquals("shattered_outpost", milestones.single().jsonObject.requiredString("zoneId"))
        assertEquals(HarnessMetadata.BUILD_ID, json.requiredString("buildId"))
        assertEquals(HarnessMetadata.PHASE_ID, json.requiredString("phaseId"))
        assertEquals(HarnessMetadata.LONG_RUN_FULL_CORPUS_ID, json.requiredString("corpusId"))
        assertEquals("templar#human#build", json.requiredString("buildHash"))
        assertEquals("forgebreaker_pick", json.requiredString("terminalWeaponBaseId"))
        assertEquals("full_route", json.requiredString("scenarioType"))
        assertEquals("true", json.requiredString("isFullRoute"))
        assertEquals("20260360", json.requiredString("seedString"))
        assertEquals("shattered_outpost", json.requiredString("routeToken"))
        assertEquals("shattered_outpost", json.requiredArray("routeIntent").single().jsonPrimitive.content)
        assertTrue(json.requiredArray("visitedSecretZoneIds").isEmpty())
        assertEquals("3", json.requiredString("affixSynergyActivationCount"))
        assertEquals("2", json.requiredString("learnableNonStarterTalentCount"))
        assertEquals("0", json.requiredString("lateRunReliquaryPurchaseCount"))
        assertEquals("0", json.requiredString("lateRunReliquaryVisitCount"))
        assertEquals("0", json.requiredString("lateRunReliquaryRefreshCount"))
        assertEquals("0", json.requiredString("lateRunReliquaryItemPurchaseCount"))
        assertEquals("0", json.requiredString("lateRunReliquaryNonMandatoryPurchaseCount"))
        assertEquals("0", json.requiredString("lateRunReliquaryShardSpent"))
        assertTrue(json.requiredObject("lateRunReliquaryTagDistribution").isEmpty())
        val affixActivations = json.requiredObject("affixSynergyActivationDistribution")
        assertEquals("2", affixActivations.requiredString("of_smite"))
        assertEquals("1", affixActivations.requiredString("of_cleansing"))
        val breakpointPayoffs = json.requiredArray("breakpointPayoffs")
        assertEquals(1, breakpointPayoffs.size)
        assertEquals("holy_mark", breakpointPayoffs.single().jsonObject.requiredString("talentId"))
        assertEquals("apply_status:bane", breakpointPayoffs.single().jsonObject.requiredArray("unlockedEffectKinds").single().jsonPrimitive.content)
        val breakpointObservations = json.requiredArray("breakpointPayoffObservations")
        assertEquals(1, breakpointObservations.size)
        val breakpointObservation = breakpointObservations.single().jsonObject
        assertEquals("holy_mark", breakpointObservation.requiredString("talentId"))
        assertEquals("templar#human#pre-payoff", breakpointObservation.requiredString("buildHashBeforeUnlock"))
        assertEquals("templar#human#build", breakpointObservation.requiredString("buildHashAfterUnlock"))
        assertEquals("true", breakpointObservation.requiredString("buildHashChanged"))
        val milestoneRewards = json.requiredArray("milestoneRewards")
        assertEquals(1, milestoneRewards.size)
        assertEquals("ROUTE", milestoneRewards.single().jsonObject.requiredString("rewardSource"))
        assertEquals("forgebreaker_pick", milestoneRewards.single().jsonObject.requiredString("baseItemId"))
        assertEquals("WEAPON", milestoneRewards.single().jsonObject.requiredString("equipSlot"))
        assertEquals("templar#human#grant", milestoneRewards.single().jsonObject.requiredString("buildHashAtGrant"))
        assertEquals("long_sword", milestoneRewards.single().jsonObject.requiredString("equippedBaseItemIdBeforeReward"))
        assertEquals("forgebreaker_pick", milestoneRewards.single().jsonObject.requiredString("equippedBaseItemIdAtRunEnd"))
        assertEquals("true", milestoneRewards.single().jsonObject.requiredString("adoptedInFinalBuild"))
        val objectives = json.requiredArray("zoneObjectiveSummaries")
        assertEquals(1, objectives.size)
        assertEquals("quest.shattered_outpost", objectives.single().jsonObject.requiredString("questId"))
        assertEquals("COMPLETED", objectives.single().jsonObject.requiredString("state"))
        val traversalDiagnostics = json.requiredArray("zoneTraversalDiagnostics")
        assertEquals(1, traversalDiagnostics.size)
        val traversal = traversalDiagnostics.single().jsonObject
        assertEquals("shattered_outpost", traversal.requiredString("zoneId"))
        assertEquals("24", traversal.requiredString("playerTurns"))
        assertEquals("31", traversal.requiredString("enemyTurns"))
        assertEquals("9", traversal.requiredString("visibleHostileTurnCount"))
        assertEquals("4", traversal.requiredString("liveHostileWindow"))
        assertEquals("2", traversal.requiredString("maxVisibleHostiles"))
        assertEquals("3", traversal.requiredString("objectiveAcquireTurn"))
        assertEquals("5", traversal.requiredString("objectiveAcquireHeadlessTurnEquivalent"))
        assertEquals("COMPLETED", traversal.requiredString("objectiveStateAtExit"))

        val captainTrace = json.requiredArray("captainEncounterTrace")
        assertEquals(1, captainTrace.size)
        val traceEntry = captainTrace.single().jsonObject
        assertEquals("POSITIVE_ENERGY", traceEntry.requiredString("playerResourceTypeId"))
        assertEquals("Move", traceEntry.requiredString("command"))
        assertTrue(traceEntry.requiredArray("recentMessages").isNotEmpty())
        assertTrue(traceEntry.requiredArray("recentEvents").isNotEmpty())
    }

    private fun JsonObject.requiredString(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.requiredArray(key: String): JsonArray = getValue(key).jsonArray

    private fun JsonObject.requiredObject(key: String): JsonObject = getValue(key).jsonObject
}
