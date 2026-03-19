package com.ktome.core.resource

import kotlinx.serialization.Serializable

enum class ResourceType {
    STAMINA,
    MANA,
    ENERGY,
    POSITIVE_ENERGY,
    HATE,
    EQUILIBRIUM,
    ;

    companion object {
        fun fromId(id: String): ResourceType =
            entries.firstOrNull { type -> type.name == id }
                ?: throw IllegalArgumentException("Unknown resource type '$id'.")
    }
}

data class ResourcePool(
    val type: ResourceType,
    var current: Int,
    var max: Int,
) {
    fun restore(amount: Int) {
        current = (current + amount).coerceIn(0, max)
    }

    fun spend(amount: Int) {
        current = (current - amount).coerceAtLeast(0)
    }

    fun syncTo(
        nextCurrent: Int,
        nextMax: Int,
    ) {
        max = nextMax.coerceAtLeast(0)
        current = nextCurrent.coerceIn(0, max)
    }
}

data class ResourcePools(
    val entries: MutableMap<ResourceType, ResourcePool> = linkedMapOf(),
) {
    fun pool(type: ResourceType): ResourcePool? = entries[type]

    fun getOrCreate(
        type: ResourceType,
        current: Int,
        max: Int,
    ): ResourcePool =
        entries.getOrPut(type) {
            ResourcePool(
                type = type,
                current = current.coerceIn(0, max.coerceAtLeast(0)),
                max = max.coerceAtLeast(0),
            )
        }
}

@Serializable
data class ResourcePoolSnapshot(
    val type: String,
    val current: Int,
    val max: Int,
) {
    init {
        require(type.isNotBlank()) { "Resource pool type must not be blank." }
        require(current >= 0) { "Resource pool current must not be negative." }
        require(max >= 0) { "Resource pool max must not be negative." }
    }
}
