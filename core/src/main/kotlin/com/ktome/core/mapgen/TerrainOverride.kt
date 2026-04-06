package com.ktome.core.mapgen

import com.ktome.core.combat.DamageType

data class TerrainOverride(
    val terrainTags: Set<TerrainTag>,
    val sourceRuleId: String,
    var remainingTurns: Int,
    val conductsLightning: Boolean = false,
    val tickDamageType: DamageType? = null,
    val tickDamage: Int = 0,
) {
    init {
        require(sourceRuleId.isNotBlank()) { "TerrainOverride.sourceRuleId must not be blank." }
        require(remainingTurns >= 0) { "TerrainOverride.remainingTurns must not be negative." }
        require(tickDamage >= 0) { "TerrainOverride.tickDamage must not be negative." }
        if (tickDamage > 0) {
            requireNotNull(tickDamageType) { "TerrainOverride.tickDamageType is required when tickDamage > 0." }
        }
    }

    val isExpired: Boolean
        get() = remainingTurns == 0
}
