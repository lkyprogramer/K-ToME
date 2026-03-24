package com.ktome.core.ai

import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar

sealed interface AIPathCommand {
    data class Move(val destination: Point) : AIPathCommand

    data class Attack(val target: EntityId) : AIPathCommand

    data object Wait : AIPathCommand
}

data class AIPathingActorSnapshot(
    val entityId: EntityId,
    val position: Point,
    val behavior: AIBehavior,
    val patrolRoute: PatrolRoute? = null,
)

data class AIPathingTargetSnapshot(
    val entityId: EntityId,
    val position: Point,
)

data class AIPathingContext(
    val map: GameMap,
    val actor: AIPathingActorSnapshot,
    val target: AIPathingTargetSnapshot,
    val occupiedTiles: Set<Point>,
    val targetVisible: Boolean,
)

data class AIPathingResult(
    val command: AIPathCommand,
    val nextPatrolIndex: Int? = null,
)

object AIPathing {
    fun chase(context: AIPathingContext): AIPathingResult {
        if (!context.targetVisible) {
            return AIPathingResult(AIPathCommand.Wait)
        }

        if (context.actor.position.isAdjacentTo(context.target.position)) {
            return AIPathingResult(AIPathCommand.Attack(context.target.entityId))
        }

        return moveToward(context)
    }

    fun kite(context: AIPathingContext): AIPathingResult {
        if (!context.targetVisible) {
            return AIPathingResult(AIPathCommand.Wait)
        }

        val distance = context.actor.position.chebyshevDistanceTo(context.target.position)
        val preferredStart = context.actor.behavior.preferredRangeStart
        val preferredEnd = context.actor.behavior.preferredRangeEnd

        if (distance < preferredStart) {
            val retreat = retreatStep(context)
            return if (retreat != null) {
                AIPathingResult(AIPathCommand.Move(retreat))
            } else if (context.actor.position.isAdjacentTo(context.target.position)) {
                AIPathingResult(AIPathCommand.Attack(context.target.entityId))
            } else {
                AIPathingResult(AIPathCommand.Wait)
            }
        }

        if (distance in preferredStart..preferredEnd) {
            return if (context.actor.position.isAdjacentTo(context.target.position)) {
                AIPathingResult(AIPathCommand.Attack(context.target.entityId))
            } else {
                AIPathingResult(AIPathCommand.Wait)
            }
        }

        return moveToward(context)
    }

    fun patrol(context: AIPathingContext): AIPathingResult {
        if (context.targetVisible) {
            return chase(context)
        }

        val patrolRoute = context.actor.patrolRoute ?: return AIPathingResult(AIPathCommand.Wait)
        val currentWaypoint = patrolRoute.waypoints[patrolRoute.nextWaypointIndex]
        val resolvedIndex = if (context.actor.position == currentWaypoint) {
            (patrolRoute.nextWaypointIndex + 1) % patrolRoute.waypoints.size
        } else {
            patrolRoute.nextWaypointIndex
        }
        val nextWaypoint = patrolRoute.waypoints[resolvedIndex]
        if (context.actor.position == nextWaypoint) {
            return AIPathingResult(AIPathCommand.Wait, nextPatrolIndex = resolvedIndex)
        }

        val path = AStar.findPath(
            map = context.map,
            start = context.actor.position,
            goal = nextWaypoint,
            blocked = context.occupiedTiles - nextWaypoint,
        )
        val nextStep = path.getOrNull(1) ?: return AIPathingResult(AIPathCommand.Wait, nextPatrolIndex = resolvedIndex)

        return AIPathingResult(
            command = AIPathCommand.Move(nextStep),
            nextPatrolIndex = resolvedIndex,
        )
    }

    fun moveToward(context: AIPathingContext): AIPathingResult {
        val path = AStar.findPath(
            map = context.map,
            start = context.actor.position,
            goal = context.target.position,
            blocked = context.occupiedTiles - context.target.position,
        )
        val nextStep = path.getOrNull(1) ?: return AIPathingResult(AIPathCommand.Wait)
        return AIPathingResult(AIPathCommand.Move(nextStep))
    }

    fun retreatStep(context: AIPathingContext): Point? =
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
