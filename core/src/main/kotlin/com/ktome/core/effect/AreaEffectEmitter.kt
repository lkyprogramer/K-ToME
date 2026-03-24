package com.ktome.core.effect

import com.ktome.core.ecs.EntityId
import com.ktome.core.status.StatusInstance

data class AreaEffectEmitter(
    val emitterId: String,
    val sourceEntityId: EntityId? = null,
    val affectedActorIds: Set<EntityId> = emptySet(),
    val emitterPriority: Int = 200,
    override val effects: MutableList<StatusInstance> = mutableListOf(),
) : PersistentEffect
