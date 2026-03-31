package com.ktome.game

import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.map.Point

internal fun interactablePoint(
    session: FoundationGameSession,
    interactableId: String,
): Point {
    val world = session.automationWorld()
    val entityId =
        requireNotNull(
            world.entitiesWith(Position::class, Interactable::class)
                .firstOrNull { candidate -> world.get<Interactable>(candidate)?.id == interactableId },
        ) {
            "Expected interactable '$interactableId'."
        }
    return requireNotNull(world.get<Position>(entityId)).toPoint()
}
