package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.game.PlayerStatus
import com.ktome.game.routeToken
import com.ktome.game.secretRouteMarker
import com.ktome.game.zoneRouteHash
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class ScenarioModelsTest {
    @Test
    fun `pr06 route diversity corpus matches fixed scenario distribution`() {
        val specs = Phase4V4Pr06RouteDiversityCorpus.smokeSpecs(HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID)
        val summary = Phase4V4Pr06RouteDiversitySummary.from(specs)

        assertEquals(20, specs.size)
        assertEquals(12, summary.scenarioTypeDistribution.getValue("full_route"))
        assertEquals(4, summary.scenarioTypeDistribution.getValue("branch_inclusive"))
        assertEquals(2, summary.scenarioTypeDistribution.getValue("route_probe"))
        assertEquals(2, summary.scenarioTypeDistribution.getValue("late_route_probe"))
        assertEquals(12, summary.fullRouteIntentDistinctCount)
        assertEquals(
            specs
                .filter { spec -> spec.scenarioType == ScenarioType.FULL_ROUTE }
                .map(ScenarioSpec::zoneId)
                .distinct()
                .size,
            summary.actualFullRouteHashDistinctCount,
        )
        assertEquals(3, summary.fullRouteTokenSample.size)
        assertTrue(summary.topHashShare <= 0.4, "summary=$summary")
        assertEquals(4, summary.probeRouteHashSample.size)
        assertTrue(specs.none { spec -> spec.zoneId in FORBIDDEN_ROUTE_DIVERSITY_START_ZONE_IDS })
        assertEquals(4, summary.branchRouteTokens.count { token -> "secret:" in token })
        assertTrue(summary.routeTokenSample.any { token -> "secret:" in token })
        assertTrue(summary.zoneRouteHashDistribution.keys.all { hash -> hash.length == 16 })
    }

    @Test
    fun `route hash uses sixteen character sha of route token with secret markers`() {
        val routeParts = listOf("greenwood_fringe", secretRouteMarker("greenwood_hidden_cache"), "deep_iron_pit")

        assertEquals("c7413ee4001c8cf9", zoneRouteHash(routeParts))
    }

    @Test
    fun `route token rejects reserved delimiters outside generated secret markers`() {
        listOf(
            listOf("greenwood_fringe", "deep>iron"),
            listOf("greenwood_fringe", "deep|iron"),
            listOf("greenwood_fringe", "deep:iron"),
            listOf("greenwood_fringe", "secret:bad:marker"),
        ).forEach { routeParts ->
            assertThrows<IllegalArgumentException> {
                routeToken(routeParts)
            }
        }
    }

    @Test
    fun `scenario spec validates route token parts and probe route delimiters`() {
        assertThrows<IllegalArgumentException> {
            ScenarioSpec(
                name = "invalid-route-token-part",
                seed = 1L,
                routeTokenParts = listOf("greenwood_fringe", "deep>iron"),
                maxTurns = 1,
                goal = ScenarioGoal.ReachFloor(1),
            )
        }

        assertThrows<IllegalArgumentException> {
            ScenarioSpec(
                name = "invalid-probe-route",
                seed = 1L,
                probeRoute = listOf("greenwood:fringe"),
                maxTurns = 1,
                goal = ScenarioGoal.ReachFloor(1),
            )
        }
    }

    @Test
    fun `scenario spec infers full route only for canonical shattered outpost mainline start`() {
        val spec =
            ScenarioSpec(
                name = "full-route",
                seed = 1L,
                zoneId = "shattered_outpost",
                zoneRoute = listOf(
                    "shattered_outpost",
                    "greenwood_fringe",
                    "deep_iron_pit",
                    "grey_gate_depths",
                    "underground_river",
                    "abyssal_temple",
                    "abyssal_heart",
                ),
                routeIndex = 0,
                maxTurns = 1,
                goal = ScenarioGoal.ReachFloor(1),
            )

        assertTrue(spec.scenarioType == ScenarioType.FULL_ROUTE)
    }

    @Test
    fun `scenario spec infers branch inclusive when route starts at shattered outpost and enters optional zone`() {
        val spec =
            ScenarioSpec(
                name = "branch-inclusive",
                seed = 1L,
                zoneId = "shattered_outpost",
                zoneRoute = listOf(
                    "shattered_outpost",
                    "greenwood_fringe",
                    "bandit_camp",
                    "greenwood_fringe",
                    "deep_iron_pit",
                    "grey_gate_depths",
                    "underground_river",
                    "abyssal_temple",
                    "abyssal_heart",
                ),
                routeIndex = 0,
                maxTurns = 1,
                goal = ScenarioGoal.ReachFloor(1),
            )

        assertTrue(spec.scenarioType == ScenarioType.BRANCH_INCLUSIVE)
    }

    @Test
    fun `scenario spec infers late route probe for abyssal temple starts`() {
        val spec =
            ScenarioSpec(
                name = "late-route",
                seed = 1L,
                zoneId = "abyssal_temple",
                zoneRoute = listOf("abyssal_temple", "abyssal_heart"),
                routeIndex = 0,
                maxTurns = 1,
                goal = ScenarioGoal.ReachFloor(1),
            )

        assertTrue(spec.scenarioType == ScenarioType.LATE_ROUTE_PROBE)
    }

    @Test
    fun `scenario spec rejects mismatched full route declaration`() {
        assertThrows<IllegalArgumentException> {
            ScenarioSpec(
                name = "invalid-full-route",
                seed = 1L,
                zoneId = "deep_iron_pit",
                zoneRoute = listOf("deep_iron_pit", "grey_gate_depths"),
                routeIndex = 2,
                scenarioType = ScenarioType.FULL_ROUTE,
                maxTurns = 1,
                goal = ScenarioGoal.ReachFloor(1),
            )
        }
    }

    @Test
    fun `reach floor or terminal accepts victory before target floor`() {
        val goal = ScenarioGoal.ReachFloorOrTerminal(5)

        assertTrue(goal.isSatisfied(observation(floor = 1, runOutcome = RunOutcome.Victory(floor = 1))))
    }

    @Test
    fun `reach floor or terminal rejects defeat before target floor`() {
        val goal = ScenarioGoal.ReachFloorOrTerminal(5)

        assertFalse(goal.isSatisfied(observation(floor = 1, runOutcome = RunOutcome.Defeat(floor = 1))))
    }

    @Test
    fun `reach floor or terminal still accepts reaching target floor while run is in progress`() {
        val goal = ScenarioGoal.ReachFloorOrTerminal(5)

        assertTrue(goal.isSatisfied(observation(floor = 5, runOutcome = RunOutcome.InProgress)))
    }

    @Test
    fun `reach zone or terminal accepts reaching target zone while run is in progress`() {
        val goal = ScenarioGoal.ReachZoneAtLeastOrTerminal("abyssal_heart")

        assertTrue(goal.isSatisfied(observation(zoneId = "abyssal_heart", floor = 1, runOutcome = RunOutcome.InProgress)))
    }

    @Test
    fun `reach zone or terminal accepts victory before target zone`() {
        val goal = ScenarioGoal.ReachZoneAtLeastOrTerminal("abyssal_heart")

        assertTrue(goal.isSatisfied(observation(zoneId = "abyssal_temple", floor = 1, runOutcome = RunOutcome.Victory(floor = 1))))
    }

    @Test
    fun `reach zone or terminal rejects defeat before target zone`() {
        val goal = ScenarioGoal.ReachZoneAtLeastOrTerminal("abyssal_heart")

        assertFalse(goal.isSatisfied(observation(zoneId = "abyssal_temple", floor = 1, runOutcome = RunOutcome.Defeat(floor = 1))))
    }

    @Test
    fun `scenario report does not treat normal defeat without harness errors as crash or stall`() {
        val report =
            report(
                success = false,
                goalReached = false,
                outcome = RunOutcome.Defeat(floor = 2),
            )

        assertFalse(report.crashedOrStalled())
    }

    @Test
    fun `scenario report treats harness failure as crash or stall`() {
        val report =
            report(
                success = false,
                goalReached = false,
                outcome = RunOutcome.Defeat(floor = 2),
                failureReason = "Command rejected: Move",
            )

        assertTrue(report.crashedOrStalled())
    }

    @Test
    fun `visited zone assertion requires exact branch visit instead of depth only`() {
        val report =
            report(
                success = true,
                goalReached = true,
                outcome = RunOutcome.InProgress,
            ).copy(
                zonePath = listOf("shattered_outpost", "greenwood_fringe", "elven_ruins"),
                finalZoneId = "elven_ruins",
            )

        val error = ScenarioAssertion.VisitedZone("bandit_camp").verify(report)

        assertTrue(error != null)
    }

    @Test
    fun `primary secret assertion requires runtime secret visit and route marker`() {
        val baseReport =
            report(
                success = true,
                goalReached = true,
                outcome = RunOutcome.InProgress,
            ).copy(
                primarySecretZoneId = "greenwood_hidden_cache",
                routeToken = routeToken(listOf("greenwood_fringe")),
                zonePath = listOf("greenwood_fringe"),
            )

        assertTrue(ScenarioAssertion.VisitedPrimarySecretZone.verify(baseReport) != null)

        val markedWithoutVisit =
            baseReport.copy(
                routeToken = routeToken(listOf("greenwood_fringe", secretRouteMarker("greenwood_hidden_cache"))),
            )
        assertTrue(ScenarioAssertion.VisitedPrimarySecretZone.verify(markedWithoutVisit) != null)

        val proven =
            markedWithoutVisit.copy(visitedSecretZoneIds = setOf("greenwood_hidden_cache"))
        assertEquals(null, ScenarioAssertion.VisitedPrimarySecretZone.verify(proven))
    }

    private fun observation(
        zoneId: String = "shattered_outpost",
        floor: Int,
        runOutcome: RunOutcome,
    ): RunObservation =
        RunObservation(
            zoneId = zoneId,
            floor = floor,
            turnIndex = 0,
            playerStatus =
                PlayerStatus(
                    currentHp = 10,
                    maxHp = 10,
                    level = 1,
                    currentExperience = 0,
                    nextLevelRequirement = 10,
                    statPoints = 0,
                    talentPoints = 0,
                    attack = 1,
                    defense = 1,
                    accuracy = 1,
                    evasion = 1,
                    speed = 100,
                ),
            playerPosition = Point.ZERO,
            map = GameMap.fromAscii(listOf("@")),
            visibleTiles = setOf(Point.ZERO),
            exploredTiles = setOf(Point.ZERO),
            visibleHostilePositions = emptyList(),
            visibleBlockingPositions = emptySet(),
            visibleGroundItemPositions = emptyList(),
            visibleInteractables = emptyList(),
            knownDownstairsPositions = emptyList(),
            inventoryItems = emptyList(),
            talentSlots = emptyList(),
            canAscend = false,
            canDescend = false,
            runOutcome = runOutcome,
            messageLogTail = emptyList(),
            eventTail = emptyList(),
        )

    private fun report(
        success: Boolean,
        goalReached: Boolean,
        outcome: RunOutcome,
        failureReason: String? = null,
        stuckReason: String? = null,
    ): ScenarioReport =
        ScenarioReport(
            name = "scenario",
            seed = 1L,
            professionId = "profession",
            success = success,
            outcome = outcome,
            floorReached = 2,
            turns = 42,
            goalReached = goalReached,
            failureReason = failureReason,
            stuckReason = stuckReason,
        )
}
