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

enum class ResourceAxis {
    HP,
    STAMINA,
    MANA,
    ENERGY,
    POSITIVE_ENERGY,
    HATE,
    EQUILIBRIUM,
    ;

    fun asResourceTypeOrNull(): ResourceType? =
        when (this) {
            HP -> null
            STAMINA -> ResourceType.STAMINA
            MANA -> ResourceType.MANA
            ENERGY -> ResourceType.ENERGY
            POSITIVE_ENERGY -> ResourceType.POSITIVE_ENERGY
            HATE -> ResourceType.HATE
            EQUILIBRIUM -> ResourceType.EQUILIBRIUM
        }

    companion object {
        fun fromId(id: String): ResourceAxis =
            entries.firstOrNull { axis -> axis.name == id }
                ?: throw IllegalArgumentException("Unknown resource axis '$id'.")
    }
}

@Serializable
enum class EquilibriumAffinity {
    PHYSICAL,
    ARCANE,
    NEUTRAL,
}

@Serializable
data class DecayPolicy(
    val amountPerTurn: Int,
    val outOfCombatOnly: Boolean = true,
) {
    init {
        require(amountPerTurn >= 0) { "Decay amount must not be negative." }
    }
}

@Serializable
sealed interface ResourceRegenProfile {
    @Serializable
    data class PerTurn(val amount: Int) : ResourceRegenProfile {
        init {
            require(amount >= 0) { "Per-turn regen must not be negative." }
        }
    }

    @Serializable
    data class OnHit(val amount: Int) : ResourceRegenProfile {
        init {
            require(amount >= 0) { "On-hit regen must not be negative." }
        }
    }

    @Serializable
    data class OnDamageTaken(val percent: Double) : ResourceRegenProfile {
        init {
            require(percent >= 0.0) { "On-damage regen percent must not be negative." }
        }
    }

    @Serializable
    data class OnKill(val amount: Int) : ResourceRegenProfile {
        init {
            require(amount >= 0) { "On-kill regen must not be negative." }
        }
    }

    @Serializable
    data class Decay(val policy: DecayPolicy) : ResourceRegenProfile

    @Serializable
    data class Composite(val entries: List<ResourceRegenProfile>) : ResourceRegenProfile {
        init {
            require(entries.isNotEmpty()) { "Composite regen profile must not be empty." }
        }
    }

    @Serializable
    data object None : ResourceRegenProfile
}

@Serializable
data class ResourceProfileRef(
    val axis: ResourceAxis,
    val initialCurrent: Int,
    val max: Int,
    val regenProfile: ResourceRegenProfile = ResourceRegenProfile.None,
    val stableMin: Int? = null,
    val stableMax: Int? = null,
) {
    init {
        require(initialCurrent >= 0) { "Initial resource current must not be negative." }
        require(max >= 0) { "Resource max must not be negative." }
        require(initialCurrent <= max) { "Initial resource current must not exceed max." }
        if (stableMin != null || stableMax != null) {
            require(stableMin != null && stableMax != null) { "Stable range must define both min and max." }
            require(stableMin in 0..stableMax) { "Stable min must be within 0..stableMax." }
            require(stableMax <= max) { "Stable max must not exceed resource max." }
        }
    }

    val resourceType: ResourceType?
        get() = axis.asResourceTypeOrNull()
}

data class EquilibriumState(
    var lastResolvedAffinity: EquilibriumAffinity = EquilibriumAffinity.NEUTRAL,
)

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
