package com.ktome.core.inscription

import com.ktome.core.combat.DamageType

data class InscriptionDef(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val iconKey: String,
    val category: InscriptionCategory,
    val cooldown: Int,
    val effect: InscriptionEffect,
    val tier: Int,
) {
    init {
        require(id.isNotBlank()) { "Inscription id must not be blank." }
        require(nameKey.isNotBlank()) { "Inscription '$id' nameKey must not be blank." }
        require(descKey.isNotBlank()) { "Inscription '$id' descKey must not be blank." }
        require(cooldown >= 0) { "Inscription '$id' cooldown must not be negative." }
        require(tier in 1..3) { "Inscription '$id' tier must be within 1..3." }
    }
}

enum class InscriptionCategory {
    HEALING,
    MOVEMENT,
    PROTECTION,
    CLEANSING,
    // Reserved for post-PR-05 offensive inscriptions; no shipped data should reference it yet.
    OFFENSE,
}

sealed interface InscriptionEffect {
    data class Heal(
        val amount: Int = 0,
        val percentMax: Double = 0.0,
    ) : InscriptionEffect

    data class Teleport(
        val range: Int,
        val controlled: Boolean = false,
    ) : InscriptionEffect

    data class Shield(
        val amount: Int,
        val duration: Int,
    ) : InscriptionEffect

    data class Cleanse(
        val count: Int,
        val alsoHeal: Int = 0,
    ) : InscriptionEffect

    // Reserved for future offensive inscriptions; PR-05 intentionally ships no concrete users.
    data class DamageBoost(
        val multiplier: Double,
        val duration: Int,
        val damageType: DamageType? = null,
    ) : InscriptionEffect
}
