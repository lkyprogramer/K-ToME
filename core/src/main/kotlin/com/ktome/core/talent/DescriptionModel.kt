package com.ktome.core.talent

import kotlin.math.roundToInt

sealed interface DescriptionValue {
    data class IntValue(val value: Int) : DescriptionValue

    data class DecimalValue(val value: Double) : DescriptionValue

    data class BooleanValue(val value: Boolean) : DescriptionValue

    data class TextValue(val value: String) : DescriptionValue
}

data class DescriptionModel(
    val templateKey: String,
    val placeholders: Map<String, DescriptionValue>,
    val keywords: List<String>,
)

data class DescriptionContext(
    val currentRank: Int,
    val previewRank: Int = currentRank,
)

data class TalentBreakpointPreview(
    val atRank: Int,
    val descriptionAddendumKey: String? = null,
    val model: DescriptionModel,
)

object DynamicDescriptionResolver {
    val BREAKPOINT_TEMPLATE_KEYS: Set<String> =
        setOf(
            "talent.breakpoint.apply_status",
            "talent.breakpoint.displacement",
            "talent.breakpoint.resource_restore",
            "talent.breakpoint.heal",
            "talent.breakpoint.stat_modifier",
            "talent.breakpoint.damage",
            "talent.breakpoint.rank",
        )

    fun resolve(
        talent: TalentDef,
        context: DescriptionContext,
    ): DescriptionModel {
        val effect = talent.levelEffect(context.previewRank)
        val placeholders = linkedMapOf<String, DescriptionValue>()

        placeholders["rank"] = DescriptionValue.IntValue(context.previewRank)
        placeholders["maxRank"] = DescriptionValue.IntValue(talent.maxRank)
        placeholders["tier"] = DescriptionValue.IntValue(talent.tier)
        placeholders["cooldown"] = DescriptionValue.IntValue(talent.cooldown)
        placeholders["range"] = DescriptionValue.IntValue(talent.targetingDef.range + effect.rangeBonus)
        placeholders["minRange"] = DescriptionValue.IntValue(talent.targetingDef.minRange)
        placeholders["radius"] = DescriptionValue.IntValue(talent.targetingDef.areaRadius)
        placeholders["requiresTarget"] = DescriptionValue.BooleanValue(talent.targetingDef.type != TalentTargetingType.SELF)

        talent.resolvedResourceCosts().forEach { (resourceType, amount) ->
            placeholders["cost${resourceType.name}"] = DescriptionValue.IntValue(amount)
        }
        talent.resolvedResourceCosts().entries.firstOrNull()?.let { (resourceType, amount) ->
            placeholders["resourceType"] = DescriptionValue.TextValue(resourceType.name)
            placeholders["resourceCost"] = DescriptionValue.IntValue(amount)
        }

        appendEffectPlaceholders(placeholders, effect)

        talent.nextBreakpoint(context.previewRank)?.let { breakpoint ->
            placeholders["nextBreakpointRank"] = DescriptionValue.IntValue(breakpoint.atRank)
        }

        return DescriptionModel(
            templateKey = talent.descriptionTemplateKey,
            placeholders = placeholders,
            keywords = talent.keywords.distinct(),
        )
    }

    fun nextBreakpointPreview(
        talent: TalentDef,
        currentRank: Int,
    ): TalentBreakpointPreview? {
        val breakpoint = talent.nextBreakpoint(currentRank) ?: return null
        val placeholders = linkedMapOf<String, DescriptionValue>()
        placeholders["rank"] = DescriptionValue.IntValue(breakpoint.atRank)
        appendEffectPlaceholders(placeholders, TalentLevelEffect(effectOps = breakpoint.unlockedEffects))
        return TalentBreakpointPreview(
            atRank = breakpoint.atRank,
            descriptionAddendumKey = breakpoint.descriptionAddendumKey,
            model =
                DescriptionModel(
                    templateKey = breakpointTemplateKey(breakpoint.unlockedEffects),
                    placeholders = placeholders,
                    keywords = talent.keywords.distinct(),
                ),
        )
    }

    private fun breakpointTemplateKey(unlockedEffects: List<EffectOp>): String =
        when (unlockedEffects.firstOrNull()) {
            is EffectOp.ApplyStatus -> "talent.breakpoint.apply_status"
            is EffectOp.Displacement -> "talent.breakpoint.displacement"
            is EffectOp.ResourceRestore -> "talent.breakpoint.resource_restore"
            is EffectOp.Heal -> "talent.breakpoint.heal"
            is EffectOp.StatModifier -> "talent.breakpoint.stat_modifier"
            is EffectOp.Damage -> "talent.breakpoint.damage"
            null -> "talent.breakpoint.rank"
        }

    private fun appendEffectPlaceholders(
        placeholders: MutableMap<String, DescriptionValue>,
        effect: TalentLevelEffect,
    ) {
        placeholders["damageMultiplier"] = DescriptionValue.IntValue((effect.damageMultiplier * 100.0).roundToInt())
        if (effect.knockback > 0) {
            placeholders["knockback"] = DescriptionValue.IntValue(effect.knockback)
        }
        if (effect.healFraction > 0.0) {
            placeholders["healPercent"] = DescriptionValue.IntValue((effect.healFraction * 100.0).roundToInt())
        }
        if (effect.resourceRestoreFraction > 0.0) {
            placeholders["resourceRestorePercent"] = DescriptionValue.IntValue((effect.resourceRestoreFraction * 100.0).roundToInt())
        }

        effect.effectOps.filterIsInstance<EffectOp.Damage>().firstOrNull()?.let { damage ->
            if (damage.scaling.attackMultiplier > 0.0) {
                placeholders["damagePercent"] =
                    DescriptionValue.IntValue((damage.scaling.attackMultiplier * 100.0).roundToInt())
            }
            damage.damageType?.let { damageType ->
                placeholders["damageType"] = DescriptionValue.TextValue(damageType.name)
            }
        }

        effect.effectOps.filterIsInstance<EffectOp.Heal>().firstOrNull()?.let { heal ->
            if (heal.maxHpFraction > 0.0) {
                placeholders["healPercent"] = DescriptionValue.IntValue((heal.maxHpFraction * 100.0).roundToInt())
            }
        }

        effect.effectOps.filterIsInstance<EffectOp.ResourceRestore>().firstOrNull()?.let { restore ->
            if (restore.amount > 0) {
                placeholders["resourceRestoreAmount"] = DescriptionValue.IntValue(restore.amount)
            }
            if (restore.fraction > 0.0) {
                placeholders["resourceRestorePercent"] =
                    DescriptionValue.IntValue((restore.fraction * 100.0).roundToInt())
            }
        }

        effect.effectOps.filterIsInstance<EffectOp.ApplyStatus>().firstOrNull()?.let { applyStatus ->
            placeholders["statusDuration"] = DescriptionValue.IntValue(applyStatus.duration)
            placeholders["statusId"] = DescriptionValue.TextValue(applyStatus.statusId)
            if (applyStatus.magnitude > 0.0) {
                placeholders["statusMagnitude"] =
                    DescriptionValue.IntValue((applyStatus.magnitude * 100.0).roundToInt())
            }
        }

        effect.effectOps.filterIsInstance<EffectOp.Displacement>().firstOrNull()?.let { displacement ->
            placeholders["displacementDistance"] = DescriptionValue.IntValue(displacement.distance)
        }
    }
}
