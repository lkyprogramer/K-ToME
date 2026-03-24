package com.ktome.core.effect

import com.ktome.core.status.StatusInstance

sealed interface PersistentEffect {
    val effects: MutableList<StatusInstance>

    fun activeEffects(): List<StatusInstance> = effects.filter(StatusInstance::isActive)
}
