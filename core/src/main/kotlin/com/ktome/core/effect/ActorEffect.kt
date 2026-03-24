package com.ktome.core.effect

import com.ktome.core.ecs.EntityId
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusInstance

data class ActorEffect(
    override val effects: MutableList<StatusInstance> = mutableListOf(),
    val ownerId: EntityId? = null,
) : PersistentEffect {
    fun has(type: StatusEffectType): Boolean =
        activeEffects().any { effect -> effect.schemaId == type.schemaId }
}
