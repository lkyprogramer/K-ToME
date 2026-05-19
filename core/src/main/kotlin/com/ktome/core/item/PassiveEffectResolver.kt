package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.resource.ResourceType

data class PassiveDamageAdjustment(
    val multiplier: Double,
    val sources: List<PassiveSource>,
)

data class PassiveStatContext(
    val healthFraction: Double,
    val selfStatusIds: Set<String> = emptySet(),
    val terrainTags: Set<TerrainTag> = emptySet(),
)

data class PassiveStatAdjustment(
    val modifier: StatModifier,
    val sources: List<PassiveSource>,
)

data class PassiveStatusProcTrigger(
    val source: PassiveSource,
    val statusId: String,
    val chance: Double,
    val duration: Int,
    val magnitude: Double,
)

data class PassiveResourceRestoreTrigger(
    val source: PassiveSource,
    val resourceType: ResourceType,
    val amount: Int,
)

object PassiveEffectResolver {
    fun equipmentPassiveSources(
        world: World,
        entity: EntityId,
    ): List<PassiveSource> =
        world.get<Equipment>(entity)?.slots?.values
            ?.flatMap { itemId ->
                val item = world.get<ItemInstance>(itemId) ?: return@flatMap emptyList()
                itemPassives(itemId = itemId, item = item)
            }
            .orEmpty()

    fun resolveDamageAdjustment(
        passives: List<PassiveSource>,
        targetTags: Set<String>,
        targetStatusIds: Set<String>,
        damageType: DamageType,
    ): PassiveDamageAdjustment {
        val sources =
            passives.filter { source ->
                when (val passive = source.passive) {
                    is PassiveEffect.DamageVsTag -> passive.tag in targetTags
                    is PassiveEffect.DamageVsStatus -> passive.statusId in targetStatusIds
                    is PassiveEffect.DamageTypeBonus -> passive.type == damageType
                    is PassiveEffect.OnHitStatusProc,
                    is PassiveEffect.OnKillResourceRestore,
                    is PassiveEffect.ConditionalStatBonus,
                    is PassiveEffect.TerrainAffinityBonus,
                    is PassiveEffect.StatModifierEffect,
                    is PassiveEffect.HpRegenPerTurn,
                    is PassiveEffect.ResistanceBonus,
                    -> false
                }
            }
        val totalBonus =
            sources.sumOf { source ->
                when (val passive = source.passive) {
                    is PassiveEffect.DamageVsTag -> passive.bonusPercent
                    is PassiveEffect.DamageVsStatus -> passive.bonusPercent
                    is PassiveEffect.DamageTypeBonus -> passive.bonusPercent
                    is PassiveEffect.OnHitStatusProc,
                    is PassiveEffect.OnKillResourceRestore,
                    is PassiveEffect.ConditionalStatBonus,
                    is PassiveEffect.TerrainAffinityBonus,
                    is PassiveEffect.StatModifierEffect,
                    is PassiveEffect.HpRegenPerTurn,
                    is PassiveEffect.ResistanceBonus,
                    -> 0.0
                }
            }
        return PassiveDamageAdjustment(
            multiplier = 1.0 + totalBonus,
            sources = sources,
        )
    }

    fun hpRegenPerTurn(passives: List<PassiveSource>): Int =
        passives.sumOf { source ->
            when (val passive = source.passive) {
                is PassiveEffect.HpRegenPerTurn -> passive.amount
                is PassiveEffect.OnHitStatusProc,
                is PassiveEffect.OnKillResourceRestore,
                is PassiveEffect.ConditionalStatBonus,
                is PassiveEffect.TerrainAffinityBonus,
                is PassiveEffect.StatModifierEffect,
                is PassiveEffect.DamageVsStatus,
                is PassiveEffect.DamageTypeBonus,
                is PassiveEffect.DamageVsTag,
                is PassiveEffect.ResistanceBonus,
                -> 0
            }
        }

    fun resistanceBonuses(passives: List<PassiveSource>): Map<DamageType, Int> =
        buildMap {
            passives.forEach { source ->
                when (val passive = source.passive) {
                    is PassiveEffect.ResistanceBonus -> {
                        put(passive.damageType, (get(passive.damageType) ?: 0) + passive.amount)
                    }

                    is PassiveEffect.OnHitStatusProc,
                    is PassiveEffect.OnKillResourceRestore,
                    is PassiveEffect.ConditionalStatBonus,
                    is PassiveEffect.TerrainAffinityBonus,
                    is PassiveEffect.StatModifierEffect,
                    is PassiveEffect.DamageVsStatus,
                    is PassiveEffect.DamageTypeBonus,
                    is PassiveEffect.DamageVsTag,
                    is PassiveEffect.HpRegenPerTurn,
                    -> Unit
                }
            }
        }

    fun resolveStatAdjustment(
        passives: List<PassiveSource>,
        context: PassiveStatContext,
    ): PassiveStatAdjustment {
        val sources =
            passives.filter { source ->
                when (val passive = source.passive) {
                    is PassiveEffect.ConditionalStatBonus -> matchesCondition(passive, context)
                    is PassiveEffect.TerrainAffinityBonus -> passive.terrainTag in context.terrainTags
                    is PassiveEffect.StatModifierEffect -> true
                    is PassiveEffect.HpRegenPerTurn -> true
                    is PassiveEffect.OnHitStatusProc,
                    is PassiveEffect.OnKillResourceRestore,
                    is PassiveEffect.DamageVsStatus,
                    is PassiveEffect.DamageTypeBonus,
                    is PassiveEffect.DamageVsTag,
                    is PassiveEffect.ResistanceBonus,
                    -> false
                }
            }
        val modifier =
            sources.fold(StatModifier.ZERO) { acc, source ->
                val next =
                    when (val passive = source.passive) {
                        is PassiveEffect.ConditionalStatBonus -> passive.statModifier
                        is PassiveEffect.TerrainAffinityBonus -> passive.statModifier
                        is PassiveEffect.StatModifierEffect -> passive.statModifier
                        is PassiveEffect.HpRegenPerTurn -> StatModifier(hpRegen = passive.amount.toDouble())
                        else -> StatModifier.ZERO
                    }
                acc + next
            }
        return PassiveStatAdjustment(
            modifier = modifier,
            sources = sources,
        )
    }

    fun onHitStatusProcs(passives: List<PassiveSource>): List<PassiveStatusProcTrigger> =
        passives.mapNotNull { source ->
            val passive = source.passive as? PassiveEffect.OnHitStatusProc ?: return@mapNotNull null
            PassiveStatusProcTrigger(
                source = source,
                statusId = passive.statusId,
                chance = passive.chance,
                duration = passive.duration,
                magnitude = passive.magnitude,
            )
        }

    fun onKillResourceRestores(passives: List<PassiveSource>): List<PassiveResourceRestoreTrigger> =
        passives.mapNotNull { source ->
            val passive = source.passive as? PassiveEffect.OnKillResourceRestore ?: return@mapNotNull null
            PassiveResourceRestoreTrigger(
                source = source,
                resourceType = passive.resourceType,
                amount = passive.amount,
            )
        }

    private fun matchesCondition(
        passive: PassiveEffect.ConditionalStatBonus,
        context: PassiveStatContext,
    ): Boolean =
        when (passive.condition) {
            PassiveCondition.HP_BELOW_50 -> context.healthFraction < 0.50
            PassiveCondition.HP_BELOW_30 -> context.healthFraction < 0.30
            PassiveCondition.HP_ABOVE_80 -> context.healthFraction > 0.80
            PassiveCondition.SELF_HAS_STATUS -> passive.statusId in context.selfStatusIds
        }

    private fun itemPassives(
        itemId: EntityId,
        item: ItemInstance,
    ): List<PassiveSource> =
        buildList {
            item.passive?.let { passive ->
                add(
                    PassiveSource(
                        kind = PassiveSourceKind.EQUIPMENT,
                        sourceId = item.basePassiveSourceId(itemId),
                        sourceTemplateId = item.baseId,
                        itemEntityId = itemId,
                        sourceSpecialTemplateId = item.specialTemplateId,
                        passive = passive,
                    ),
                )
            }
            item.affixes.forEach { affix ->
                affix.passive?.let { passive ->
                    add(
                        PassiveSource(
                            kind = PassiveSourceKind.EQUIPMENT,
                            sourceId = item.affixPassiveSourceId(itemId = itemId, affixId = affix.id),
                            sourceTemplateId = item.baseId,
                            itemEntityId = itemId,
                            passive = passive,
                            affixId = affix.id,
                            sourceSpecialTemplateId = item.specialTemplateId,
                        ),
                    )
                }
            }
        }

    private fun ItemInstance.basePassiveSourceId(itemId: EntityId): String =
        specialTemplateId?.let { templateId ->
            "equipment:${itemId.value}:$baseId:special:$templateId"
        } ?: "equipment:${itemId.value}:$baseId"

    private fun ItemInstance.affixPassiveSourceId(
        itemId: EntityId,
        affixId: String,
    ): String =
        specialTemplateId?.let { templateId ->
            "equipment:${itemId.value}:$baseId:special:$templateId:affix:$affixId"
        } ?: "equipment:${itemId.value}:$baseId:affix:$affixId"
}
