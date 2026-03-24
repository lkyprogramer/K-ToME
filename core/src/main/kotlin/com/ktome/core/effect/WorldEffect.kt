package com.ktome.core.effect

import com.ktome.core.ecs.EntityId
import com.ktome.core.status.StatusInstance

data class WorldEffect(
    val effectId: String,
    val affectedActorIds: Set<EntityId> = emptySet(),
    val worldPriority: Int = 300,
    override val effects: MutableList<StatusInstance> = mutableListOf(),
) : PersistentEffect
