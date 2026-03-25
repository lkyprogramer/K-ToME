package com.ktome.core.ai

import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AIPathingTest {
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
    fun `chase moves toward target`() {
        val result = AIPathing.chase(context(AIBehavior(AIType.CHASE), self = Point(5, 2), target = Point(2, 2), visible = true))

        assertEquals(AIPathingResult(AIPathCommand.Move(Point(4, 1))), result)
    }

    @Test
    fun `chase attacks when adjacent`() {
        val result = AIPathing.chase(context(AIBehavior(AIType.CHASE), self = Point(3, 2), target = Point(2, 2), visible = true))

        assertEquals(AIPathingResult(AIPathCommand.Attack(EntityId(1))), result)
    }

    @Test
    fun `chase waits when target is not visible`() {
        val result = AIPathing.chase(context(AIBehavior(AIType.CHASE), self = Point(5, 2), target = Point(2, 2), visible = false))

        assertEquals(AIPathingResult(AIPathCommand.Wait), result)
    }

    @Test
    fun `kite retreats when the player is too close`() {
        val result =
            AIPathing.kite(
                context(
                    behavior = AIBehavior(AIType.KITE, preferredRangeStart = 2, preferredRangeEnd = 3),
                    self = Point(3, 2),
                    target = Point(2, 2),
                    visible = true,
                ),
            )

        assertEquals(AIPathingResult(AIPathCommand.Move(Point(4, 3))), result)
    }

    @Test
    fun `kite holds position at preferred range when no ranged attack contract exists`() {
        val result =
            AIPathing.kite(
                context(
                    behavior = AIBehavior(AIType.KITE, preferredRangeStart = 2, preferredRangeEnd = 3),
                    self = Point(5, 2),
                    target = Point(2, 2),
                    visible = true,
                ),
            )

        assertEquals(AIPathingResult(AIPathCommand.Wait), result)
    }

    @Test
    fun `kite falls back to melee when retreat has no open tile`() {
        val result =
            AIPathing.kite(
                context(
                    behavior = AIBehavior(AIType.KITE, preferredRangeStart = 2, preferredRangeEnd = 3),
                    self = Point(3, 2),
                    target = Point(2, 2),
                    visible = true,
                    occupiedTiles = setOf(Point(2, 1), Point(3, 1), Point(4, 1), Point(4, 2), Point(4, 3), Point(3, 3), Point(2, 3), Point(2, 2)),
                ),
            )

        assertEquals(AIPathingResult(AIPathCommand.Attack(EntityId(1))), result)
    }

    @Test
    fun `patrol follows waypoints until the player is visible`() {
        val result =
            AIPathing.patrol(
                context(
                    behavior = AIBehavior(AIType.PATROL),
                    self = Point(2, 2),
                    target = Point(6, 2),
                    visible = false,
                    patrolWaypoints = listOf(Point(2, 2), Point(5, 2)),
                    patrolIndex = 0,
                ),
            )

        assertEquals(
            AIPathingResult(
                command = AIPathCommand.Move(Point(3, 1)),
                nextPatrolIndex = 1,
            ),
            result,
        )
    }

    @Test
    fun `patrol switches to chase when target becomes visible`() {
        val result =
            AIPathing.patrol(
                context(
                    behavior = AIBehavior(AIType.PATROL),
                    self = Point(3, 2),
                    target = Point(2, 2),
                    visible = true,
                    patrolWaypoints = listOf(Point(5, 2), Point(6, 2)),
                ),
            )

        assertEquals(AIPathingResult(AIPathCommand.Attack(EntityId(1))), result)
    }

    @Test
    fun `patrol waits when no patrol route is configured`() {
        val result =
            AIPathing.patrol(
                context(
                    behavior = AIBehavior(AIType.PATROL),
                    self = Point(3, 2),
                    target = Point(6, 2),
                    visible = false,
                ),
            )

        assertEquals(AIPathingResult(AIPathCommand.Wait), result)
    }

    @Test
    fun `move toward waits when no path exists`() {
        val blockedMap =
            GameMap.fromAscii(
                rows = listOf("#####", "#...#", "#####"),
                playerStart = Point(1, 1),
            )

        val result =
            AIPathing.moveToward(
                context(
                    map = blockedMap,
                    behavior = AIBehavior(AIType.CHASE),
                    self = Point(1, 1),
                    target = Point(3, 1),
                    visible = true,
                    occupiedTiles = setOf(Point(2, 1)),
                ),
            )

        assertEquals(AIPathingResult(AIPathCommand.Wait), result)
    }

    private fun context(
        behavior: AIBehavior,
        self: Point,
        target: Point,
        visible: Boolean,
        patrolWaypoints: List<Point> = emptyList(),
        patrolIndex: Int = 0,
        occupiedTiles: Set<Point> = emptySet(),
        map: GameMap = this.map,
    ): AIPathingContext =
        AIPathingContext(
            map = map,
            actor =
                AIPathingActorSnapshot(
                    entityId = EntityId(2),
                    position = self,
                    behavior = behavior,
                    patrolRoute = patrolWaypoints.takeIf { it.isNotEmpty() }?.let { PatrolRoute(it, patrolIndex) },
                ),
            target = AIPathingTargetSnapshot(entityId = EntityId(1), position = target),
            occupiedTiles = occupiedTiles,
            targetVisible = visible,
        )
}
