package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.resource.ResourceType

data class EquippedPassiveSource(
    val item: ItemInstance,
    val passive: EquipmentPassive,
    val affixId: String? = null,
)

data class PassiveDamageAdjustment(
    val multiplier: Double,
    val sources: List<EquippedPassiveSource>,
)

data class PassiveStatContext(
    val healthFraction: Double,
    val selfStatusIds: Set<String> = emptySet(),
    val terrainTags: Set<TerrainTag> = emptySet(),
)

data class PassiveStatAdjustment(
    val modifier: StatModifier,
    val sources: List<EquippedPassiveSource>,
)

data class PassiveStatusProcTrigger(
    val source: EquippedPassiveSource,
    val statusId: String,
    val chance: Double,
    val duration: Int,
    val magnitude: Double,
)

data class PassiveResourceRestoreTrigger(
    val source: EquippedPassiveSource,
    val resourceType: ResourceType,
    val amount: Int,
)

object PassiveEffectResolver {
    fun equippedPassives(
        world: World,
        entity: EntityId,
    ): List<EquippedPassiveSource> =
        world.get<Equipment>(entity)?.slots?.values
            ?.flatMap { itemId ->
                val item = world.get<ItemInstance>(itemId) ?: return@flatMap emptyList()
                itemPassives(item)
            }
            .orEmpty()

    fun resolveDamageAdjustment(
        passives: List<EquippedPassiveSource>,
        targetTags: Set<String>,
        targetStatusIds: Set<String>,
        damageType: DamageType,
    ): PassiveDamageAdjustment {
        val sources =
            passives.filter { source ->
                when (val passive = source.passive) {
                    is EquipmentPassive.DamageVsTag -> passive.tag in targetTags
                    is EquipmentPassive.DamageVsStatus -> passive.statusId in targetStatusIds
                    is EquipmentPassive.DamageTypeBonus -> passive.type == damageType
                    is EquipmentPassive.OnHitStatusProc,
                    is EquipmentPassive.OnKillResourceRestore,
                    is EquipmentPassive.ConditionalStatBonus,
                    is EquipmentPassive.TerrainAffinityBonus,
                    is EquipmentPassive.HpRegenPerTurn,
                    is EquipmentPassive.ResistanceBonus,
                    -> false
                }
            }
        val totalBonus =
            sources.sumOf { source ->
                when (val passive = source.passive) {
                    is EquipmentPassive.DamageVsTag -> passive.bonusPercent
                    is EquipmentPassive.DamageVsStatus -> passive.bonusPercent
                    is EquipmentPassive.DamageTypeBonus -> passive.bonusPercent
                    is EquipmentPassive.OnHitStatusProc,
                    is EquipmentPassive.OnKillResourceRestore,
                    is EquipmentPassive.ConditionalStatBonus,
                    is EquipmentPassive.TerrainAffinityBonus,
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
                is EquipmentPassive.OnHitStatusProc,
                is EquipmentPassive.OnKillResourceRestore,
                is EquipmentPassive.ConditionalStatBonus,
                is EquipmentPassive.TerrainAffinityBonus,
                is EquipmentPassive.DamageVsStatus,
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

                    is EquipmentPassive.OnHitStatusProc,
                    is EquipmentPassive.OnKillResourceRestore,
                    is EquipmentPassive.ConditionalStatBonus,
                    is EquipmentPassive.TerrainAffinityBonus,
                    is EquipmentPassive.DamageVsStatus,
                    is EquipmentPassive.DamageTypeBonus,
                    is EquipmentPassive.DamageVsTag,
                    is EquipmentPassive.HpRegenPerTurn,
                    -> Unit
                }
            }
        }

    fun resolveStatAdjustment(
        passives: List<EquippedPassiveSource>,
        context: PassiveStatContext,
    ): PassiveStatAdjustment {
        val sources =
            passives.filter { source ->
                when (val passive = source.passive) {
                    is EquipmentPassive.ConditionalStatBonus -> matchesCondition(passive, context)
                    is EquipmentPassive.TerrainAffinityBonus -> passive.terrainTag in context.terrainTags
                    is EquipmentPassive.OnHitStatusProc,
                    is EquipmentPassive.OnKillResourceRestore,
                    is EquipmentPassive.DamageVsStatus,
                    is EquipmentPassive.DamageTypeBonus,
                    is EquipmentPassive.DamageVsTag,
                    is EquipmentPassive.HpRegenPerTurn,
                    is EquipmentPassive.ResistanceBonus,
                    -> false
                }
            }
        val modifier =
            sources.fold(StatModifier.ZERO) { acc, source ->
                val next =
                    when (val passive = source.passive) {
                        is EquipmentPassive.ConditionalStatBonus -> passive.statModifier
                        is EquipmentPassive.TerrainAffinityBonus -> passive.statModifier
                        else -> StatModifier.ZERO
                    }
                acc + next
            }
        return PassiveStatAdjustment(
            modifier = modifier,
            sources = sources,
        )
    }

    fun onHitStatusProcs(passives: List<EquippedPassiveSource>): List<PassiveStatusProcTrigger> =
        passives.mapNotNull { source ->
            val passive = source.passive as? EquipmentPassive.OnHitStatusProc ?: return@mapNotNull null
            PassiveStatusProcTrigger(
                source = source,
                statusId = passive.statusId,
                chance = passive.chance,
                duration = passive.duration,
                magnitude = passive.magnitude,
            )
        }

    fun onKillResourceRestores(passives: List<EquippedPassiveSource>): List<PassiveResourceRestoreTrigger> =
        passives.mapNotNull { source ->
            val passive = source.passive as? EquipmentPassive.OnKillResourceRestore ?: return@mapNotNull null
            PassiveResourceRestoreTrigger(
                source = source,
                resourceType = passive.resourceType,
                amount = passive.amount,
            )
        }

    private fun matchesCondition(
        passive: EquipmentPassive.ConditionalStatBonus,
        context: PassiveStatContext,
    ): Boolean =
        when (passive.condition) {
            PassiveCondition.HP_BELOW_50 -> context.healthFraction < 0.50
            PassiveCondition.HP_BELOW_30 -> context.healthFraction < 0.30
            PassiveCondition.HP_ABOVE_80 -> context.healthFraction > 0.80
            PassiveCondition.SELF_HAS_STATUS -> passive.statusId in context.selfStatusIds
        }

    private fun itemPassives(item: ItemInstance): List<EquippedPassiveSource> =
        buildList {
            item.passive?.let { passive ->
                add(
                    EquippedPassiveSource(
                        item = item,
                        passive = passive,
                    ),
                )
            }
            item.affixes.forEach { affix ->
                affix.passive?.let { passive ->
                    add(
                        EquippedPassiveSource(
                            item = item,
                            passive = passive,
                            affixId = affix.id,
                        ),
                    )
                }
            }
        }
}
