package com.ktome.core.ecs

interface GameSystem {
    val priority: Int
        get() = 0

    fun update(world: World)
}
