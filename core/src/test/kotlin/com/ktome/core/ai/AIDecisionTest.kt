package com.ktome.core.ai

import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AIDecisionTest {
    private val map = GameMap.fromAscii(
        rows = listOf(
            "########",
            "#......#",
            "#......#",
            "#......#",
            "########",
        ),
        playerStart = Point(1, 1),
    )

    @Test
    fun chaseMovesTowardTarget() {
        val decision = AIDecision.decide(
            context = context(
                behavior = AIBehavior(AIType.CHASE),
                self = Point(5, 2),
                target = Point(2, 2),
                visible = true,
            ),
        )

        assertEquals(AIDecisionResult(AIAction.Move(Point(4, 1))), decision)
    }

    @Test
    fun chaseAttacksWhenAdjacent() {
        val decision = AIDecision.decide(
            context = context(
                behavior = AIBehavior(AIType.CHASE),
                self = Point(3, 2),
                target = Point(2, 2),
                visible = true,
            ),
        )

        assertEquals(AIDecisionResult(AIAction.Attack(EntityId(1))), decision)
    }

    @Test
    fun chaseWaitsWhenTargetIsNotVisible() {
        val decision = AIDecision.decide(
            context = context(
                behavior = AIBehavior(AIType.CHASE),
                self = Point(3, 2),
                target = Point(2, 2),
                visible = false,
            ),
        )

        assertEquals(AIDecisionResult(AIAction.Wait), decision)
    }

    @Test
    fun kiteRetreatsTooClose() {
        val decision = AIDecision.decide(
            context = context(
                behavior = AIBehavior(AIType.KITE, preferredRangeStart = 2, preferredRangeEnd = 3),
                self = Point(3, 2),
                target = Point(2, 2),
                visible = true,
            ),
        )

        assertEquals(AIDecisionResult(AIAction.Move(Point(4, 3))), decision)
    }

    @Test
    fun kiteAttacksInRange() {
        val decision = AIDecision.decide(
            context = context(
                behavior = AIBehavior(AIType.KITE, preferredRangeStart = 2, preferredRangeEnd = 3),
                self = Point(5, 2),
                target = Point(2, 2),
                visible = true,
            ),
        )

        assertEquals(AIDecisionResult(AIAction.Attack(EntityId(1))), decision)
    }

    @Test
    fun patrolFollowsWaypoints() {
        val decision = AIDecision.decide(
            context = context(
                behavior = AIBehavior(AIType.PATROL),
                self = Point(2, 2),
                target = Point(6, 2),
                visible = false,
                patrolWaypoints = listOf(Point(2, 2), Point(5, 2)),
                patrolIndex = 0,
            ),
        )

        assertEquals(
            AIDecisionResult(
                action = AIAction.Move(Point(3, 1)),
                nextPatrolIndex = 1,
            ),
            decision,
        )
    }

    @Test
    fun patrolSwitchesToChase() {
        val decision = AIDecision.decide(
            context = context(
                behavior = AIBehavior(AIType.PATROL),
                self = Point(5, 2),
                target = Point(2, 2),
                visible = true,
                patrolWaypoints = listOf(Point(5, 2), Point(5, 3)),
                patrolIndex = 0,
            ),
        )

        assertEquals(AIDecisionResult(AIAction.Move(Point(4, 1))), decision)
    }

    private fun context(
        behavior: AIBehavior,
        self: Point,
        target: Point,
        visible: Boolean,
        patrolWaypoints: List<Point> = emptyList(),
        patrolIndex: Int = 0,
    ): AIDecisionContext =
        AIDecisionContext(
            map = map,
            actor = AIActorSnapshot(
                entityId = EntityId(2),
                position = self,
                behavior = behavior,
                patrolRoute = patrolWaypoints.takeIf { it.isNotEmpty() }?.let { PatrolRoute(it, patrolIndex) },
            ),
            target = AITargetSnapshot(
                entityId = EntityId(1),
                position = target,
            ),
            occupiedTiles = emptySet(),
            targetVisible = visible,
        )
}
