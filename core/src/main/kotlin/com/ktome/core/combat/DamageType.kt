package com.ktome.core.combat

import kotlinx.serialization.Serializable

@Serializable
enum class DamageType {
    PHYSICAL,
    FIRE,
    COLD,
    LIGHTNING,
    HOLY,
    SHADOW,
    ;

    val isElemental: Boolean
        get() = this != PHYSICAL
}
