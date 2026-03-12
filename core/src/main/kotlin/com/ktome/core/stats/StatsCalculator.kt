package com.ktome.core.stats

import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.Stats

object StatsCalculator {
    fun calculate(
        stats: Stats,
        profile: CombatProfile,
    ): DerivedStats =
        DerivedStats(
            attack = profile.baseAttack + stats.str * 2,
            defense = profile.baseDefense,
            accuracy = profile.baseAccuracy + stats.dex,
            evasion = profile.baseEvasion + stats.dex,
            speed = profile.baseSpeed + stats.dex / 2,
            critChance = (0.05 + stats.dex * 0.002).coerceIn(0.0, 0.50),
            maxHp = profile.baseHp + stats.con * 8,
            maxStamina = profile.baseStamina + stats.wil * 5,
            hpRegen = profile.baseHpRegen + stats.con * 0.2,
            staminaRegen = 3.0,
            talentPower = 1.0 + stats.wil * 0.01,
        )
}
