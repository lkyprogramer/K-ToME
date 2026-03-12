package com.ktome.core.ai

import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar

sealed interface AIAction {
    data class Move(val destination: Point) : AIAction

    data class Attack(val target: EntityId) : AIAction

    data object Wait : AIAction
}

data class AIActorSnapshot(
    val entityId: EntityId,
    val position: Point,
    val behavior: AIBehavior,
    val patrolRoute: PatrolRoute? = null,
)

data class AITargetSnapshot(
    val entityId: EntityId,
    val position: Point,
)

data class AIDecisionContext(
    val map: GameMap,
    val actor: AIActorSnapshot,
    val target: AITargetSnapshot,
    val occupiedTiles: Set<Point>,
    val targetVisible: Boolean,
)

data class AIDecisionResult(
    val action: AIAction,
    val nextPatrolIndex: Int? = null,
)

object AIDecision {
    fun decide(context: AIDecisionContext): AIDecisionResult =
        when (context.actor.behavior.type) {
            AIType.CHASE -> chase(context)
            AIType.KITE -> kite(context)
            AIType.PATROL -> patrol(context)
        }

    private fun chase(context: AIDecisionContext): AIDecisionResult {
        if (!context.targetVisible) {
            return AIDecisionResult(AIAction.Wait)
        }

        if (context.actor.position.isAdjacentTo(context.target.position)) {
            return AIDecisionResult(AIAction.Attack(context.target.entityId))
        }

        return moveToward(context)
    }

    private fun kite(context: AIDecisionContext): AIDecisionResult {
        if (!context.targetVisible) {
            return AIDecisionResult(AIAction.Wait)
        }

        val distance = context.actor.position.chebyshevDistanceTo(context.target.position)
        val preferredStart = context.actor.behavior.preferredRangeStart
        val preferredEnd = context.actor.behavior.preferredRangeEnd

        if (distance < preferredStart) {
            val retreat = retreatStep(context)
            return if (retreat != null) {
                AIDecisionResult(AIAction.Move(retreat))
            } else if (context.actor.position.isAdjacentTo(context.target.position)) {
                AIDecisionResult(AIAction.Attack(context.target.entityId))
            } else {
                AIDecisionResult(AIAction.Wait)
            }
        }

        if (distance in preferredStart..preferredEnd) {
            return AIDecisionResult(AIAction.Attack(context.target.entityId))
        }

        return moveToward(context)
    }

    private fun patrol(context: AIDecisionContext): AIDecisionResult {
        if (context.targetVisible) {
            return chase(context)
        }

        val patrolRoute = context.actor.patrolRoute ?: return AIDecisionResult(AIAction.Wait)
        val currentWaypoint = patrolRoute.waypoints[patrolRoute.nextWaypointIndex]
        val resolvedIndex = if (context.actor.position == currentWaypoint) {
            (patrolRoute.nextWaypointIndex + 1) % patrolRoute.waypoints.size
        } else {
            patrolRoute.nextWaypointIndex
        }
        val nextWaypoint = patrolRoute.waypoints[resolvedIndex]
        if (context.actor.position == nextWaypoint) {
            return AIDecisionResult(AIAction.Wait, nextPatrolIndex = resolvedIndex)
        }

        val path = AStar.findPath(
            map = context.map,
            start = context.actor.position,
            goal = nextWaypoint,
            blocked = context.occupiedTiles - nextWaypoint,
        )
        val nextStep = path.getOrNull(1) ?: return AIDecisionResult(AIAction.Wait, nextPatrolIndex = resolvedIndex)

        return AIDecisionResult(
            action = AIAction.Move(nextStep),
            nextPatrolIndex = resolvedIndex,
        )
    }

    private fun moveToward(context: AIDecisionContext): AIDecisionResult {
        val path = AStar.findPath(
            map = context.map,
            start = context.actor.position,
            goal = context.target.position,
            blocked = context.occupiedTiles - context.target.position,
        )
        val nextStep = path.getOrNull(1) ?: return AIDecisionResult(AIAction.Wait)
        return AIDecisionResult(AIAction.Move(nextStep))
    }

    private fun retreatStep(context: AIDecisionContext): Point? =
        Point.ALL_DIRECTIONS
            .map { context.actor.position + it }
            .filter { destination ->
                context.map.isInBounds(destination.x, destination.y) &&
                    !context.map.blocksMovement(destination.x, destination.y) &&
                    destination !in context.occupiedTiles
            }
            .maxWithOrNull(
                compareBy<Point> { it.chebyshevDistanceTo(context.target.position) }
                    .thenBy(Point::y)
                    .thenBy(Point::x),
            )
}
