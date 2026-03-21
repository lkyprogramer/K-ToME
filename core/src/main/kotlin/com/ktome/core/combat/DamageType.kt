package com.ktome.core.combat

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
