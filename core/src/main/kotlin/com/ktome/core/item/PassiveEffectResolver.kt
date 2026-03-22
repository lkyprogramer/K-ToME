package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get

data class EquippedPassiveSource(
    val item: ItemInstance,
    val passive: EquipmentPassive,
)

data class PassiveDamageAdjustment(
    val multiplier: Double,
    val sources: List<EquippedPassiveSource>,
)

object PassiveEffectResolver {
    fun equippedPassives(
        world: World,
        entity: EntityId,
    ): List<EquippedPassiveSource> =
        world.get<Equipment>(entity)?.slots?.values
            ?.mapNotNull { itemId ->
                val item = world.get<ItemInstance>(itemId) ?: return@mapNotNull null
                item.passive?.let { passive ->
                    EquippedPassiveSource(
                        item = item,
                        passive = passive,
                    )
                }
            }
            .orEmpty()

    fun resolveDamageAdjustment(
        passives: List<EquippedPassiveSource>,
        targetTags: Set<String>,
        damageType: DamageType,
    ): PassiveDamageAdjustment {
        val sources =
            passives.filter { source ->
                when (val passive = source.passive) {
                    is EquipmentPassive.DamageVsTag -> passive.tag in targetTags
                    is EquipmentPassive.DamageTypeBonus -> passive.type == damageType
                    is EquipmentPassive.HpRegenPerTurn,
                    is EquipmentPassive.ResistanceBonus,
                    -> false
                }
            }
        val totalBonus =
            sources.sumOf { source ->
                when (val passive = source.passive) {
                    is EquipmentPassive.DamageVsTag -> passive.bonusPercent
                    is EquipmentPassive.DamageTypeBonus -> passive.bonusPercent
                    is EquipmentPassive.HpRegenPerTurn,
                    is EquipmentPassive.ResistanceBonus,
                    -> 0.0
                }
            }
        return PassiveDamageAdjustment(
            multiplier = 1.0 + totalBonus,
            sources = sources,
        )
    }

    fun hpRegenPerTurn(passives: List<EquippedPassiveSource>): Int =
        passives.sumOf { source ->
            when (val passive = source.passive) {
                is EquipmentPassive.HpRegenPerTurn -> passive.amount
                is EquipmentPassive.DamageTypeBonus,
                is EquipmentPassive.DamageVsTag,
                is EquipmentPassive.ResistanceBonus,
                -> 0
            }
        }

    fun resistanceBonuses(passives: List<EquippedPassiveSource>): Map<DamageType, Int> =
        buildMap {
            passives.forEach { source ->
                when (val passive = source.passive) {
                    is EquipmentPassive.ResistanceBonus -> {
                        put(passive.damageType, (get(passive.damageType) ?: 0) + passive.amount)
                    }

                    is EquipmentPassive.DamageTypeBonus,
                    is EquipmentPassive.DamageVsTag,
                    is EquipmentPassive.HpRegenPerTurn,
                    -> Unit
                }
            }
        }
}
