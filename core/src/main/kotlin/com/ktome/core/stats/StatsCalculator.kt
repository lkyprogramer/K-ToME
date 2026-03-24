package com.ktome.core.stats

import com.ktome.core.combat.PowerSaveFormula
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.item.Equipment
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.StatModifier
import com.ktome.core.resource.StaminaPools
import com.ktome.core.talent.EffectTracker
import kotlin.math.roundToInt

object StatsCalculator {
    fun calculate(
        stats: Stats,
        profile: CombatProfile,
        modifiers: StatModifier = StatModifier(),
        level: Int = 1,
    ): DerivedStats =
        calculateDerived(stats, profile, modifiers, level)

    fun calculate(
        world: World,
        entity: EntityId,
    ): DerivedStats {
        val stats = requireNotNull(world.get<Stats>(entity)) { "Missing Stats for $entity" }
        val profile = requireNotNull(world.get<CombatProfile>(entity)) { "Missing CombatProfile for $entity" }
        val level = world.get<Experience>(entity)?.level ?: 1
        return calculate(stats, profile, collectModifiers(world, entity), level)
    }

    fun effectiveStats(
        world: World,
        entity: EntityId,
    ): Stats {
        val stats = requireNotNull(world.get<Stats>(entity)) { "Missing Stats for $entity" }
        return applyModifiers(stats, collectModifiers(world, entity))
    }

    fun recalculateAndStore(
        world: World,
        entity: EntityId,
    ): DerivedStats {
        val derived = calculate(world, entity)
        world.add(entity, derived)
        world.get<Health>(entity)?.let { health ->
            val delta = derived.maxHp - health.max
            health.max = derived.maxHp
            health.current = (health.current + delta).coerceIn(0, health.max)
        }
        if (StaminaPools.hasPool(world, entity)) {
            StaminaPools.shiftMax(world, entity, derived.maxStamina)
        }
        return derived
    }

    private fun collectModifiers(
        world: World,
        entity: EntityId,
    ): StatModifier {
        val equipmentModifiers =
            world.get<Equipment>(entity)?.slots?.values
                ?.mapNotNull { itemId -> world.get<ItemInstance>(itemId)?.stats }
                ?.fold(StatModifier.ZERO) { acc, modifier -> acc + modifier }
                ?: StatModifier.ZERO
        val effectModifiers =
            world.get<EffectTracker>(entity)?.effects
                ?.filter { it.remainingTurns > 0 }
                ?.map(ActiveEffectAccessor::modifier)
                ?.fold(StatModifier.ZERO) { acc, modifier -> acc + modifier }
                ?: StatModifier.ZERO
        return equipmentModifiers + effectModifiers
    }

    private fun calculateDerived(
        stats: Stats,
        profile: CombatProfile,
        modifiers: StatModifier,
        level: Int,
    ): DerivedStats {
        val effectiveStats = applyModifiers(stats, modifiers)

        val attackBase = profile.baseAttack + effectiveStats.str * 2 + modifiers.attack
        val defenseBase = profile.baseDefense + modifiers.defense
        val accuracyBase = profile.baseAccuracy + effectiveStats.dex + modifiers.accuracy
        val evasionBase = profile.baseEvasion + effectiveStats.dex + modifiers.evasion
        val speedBase = profile.baseSpeed + effectiveStats.dex / 2 + modifiers.speed

        return DerivedStats(
            attack = applyMultiplier(attackBase, modifiers.attackMultiplierBonus),
            defense = applyMultiplier(defenseBase, modifiers.defenseMultiplierBonus),
            accuracy = accuracyBase,
            evasion = evasionBase,
            speed = speedBase,
            critChance = (0.05 + effectiveStats.dex * 0.002 + modifiers.critChance).coerceIn(0.0, 0.50),
            critResistance = 0.0,
            maxHp = profile.baseHp + effectiveStats.con * 8 + modifiers.maxHp,
            maxStamina = profile.baseStamina + effectiveStats.wil * 5 + modifiers.maxStamina,
            hpRegen = profile.baseHpRegen + effectiveStats.con * 0.2 + modifiers.hpRegen,
            staminaRegen = 3.0 + modifiers.staminaRegen,
            talentPower = 1.0 + effectiveStats.wil * 0.01 + modifiers.talentPower,
            powerSave = PowerSaveFormula.calculate(effectiveStats, level),
        )
    }

    private fun applyMultiplier(
        value: Int,
        bonus: Double,
    ): Int = (value * (1.0 + bonus)).roundToInt().coerceAtLeast(0)

    private fun applyModifiers(
        stats: Stats,
        modifiers: StatModifier,
    ): Stats =
        Stats(
            str = stats.str + modifiers.str,
            dex = stats.dex + modifiers.dex,
            con = stats.con + modifiers.con,
            wil = stats.wil + modifiers.wil,
        )

    private object ActiveEffectAccessor {
        fun modifier(effect: com.ktome.core.talent.ActiveEffect): StatModifier = effect.effectiveStatModifier()
    }
}
