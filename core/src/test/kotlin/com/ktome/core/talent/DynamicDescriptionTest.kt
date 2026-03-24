package com.ktome.core.talent

import com.ktome.core.combat.ApplicationPolicy
import com.ktome.core.combat.DamageType
import com.ktome.core.combat.SaveDimension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DynamicDescriptionTest {
    private val powerStrike =
        TalentDef(
            id = "power_strike",
            nameKey = "talent.vanguard.power_strike.name",
            descriptionTemplateKey = "talent.vanguard.power_strike.desc",
            cooldown = 3,
            targetingDef = TalentTargeting(type = TalentTargetingType.SINGLE_TARGET, range = 1),
            levelEffects =
                mapOf(
                    1 to
                        TalentLevelEffect(
                            effectOps =
                                listOf(
                                    EffectOp.Damage(
                                        damageType = DamageType.PHYSICAL,
                                        scaling = ScalingDef(attackMultiplier = 1.5),
                                    ),
                                ),
                        ),
                    5 to
                        TalentLevelEffect(
                            effectOps =
                                listOf(
                                    EffectOp.Damage(
                                        damageType = DamageType.PHYSICAL,
                                        scaling = ScalingDef(attackMultiplier = 2.5),
                                    ),
                                    EffectOp.Displacement(type = DisplacementType.PUSH, distance = 1),
                                    EffectOp.ApplyStatus(
                                        statusId = "ARMOR_BREAK",
                                        duration = 3,
                                        applicationPolicy = ApplicationPolicy.HOSTILE_HIT_THEN_SAVE,
                                        trigger = EffectTrigger.ON_HIT,
                                        targetScope = EffectTargetScope.PRIMARY_TARGET,
                                        saveDimension = SaveDimension.PHYSICAL,
                                    ),
                                ),
                        ),
                ),
            breakpoints =
                listOf(
                    TalentBreakpoint(
                        atRank = 5,
                        unlockedEffects =
                            listOf(
                                EffectOp.Displacement(type = DisplacementType.PUSH, distance = 1),
                                EffectOp.ApplyStatus(
                                    statusId = "ARMOR_BREAK",
                                    duration = 3,
                                    applicationPolicy = ApplicationPolicy.HOSTILE_HIT_THEN_SAVE,
                                    trigger = EffectTrigger.ON_HIT,
                                    targetScope = EffectTargetScope.PRIMARY_TARGET,
                                    saveDimension = SaveDimension.PHYSICAL,
                                ),
                            ),
                    ),
                ),
            keywords = listOf("damage", "armor_break"),
        )

    @Test
    fun `resolve keeps typed placeholders and keyword references`() {
        val model = DynamicDescriptionResolver.resolve(powerStrike, DescriptionContext(currentRank = 1, previewRank = 1))

        assertEquals("talent.vanguard.power_strike.desc", model.templateKey)
        assertEquals(1, (model.placeholders.getValue("rank") as DescriptionValue.IntValue).value)
        assertEquals(150, (model.placeholders.getValue("damagePercent") as DescriptionValue.IntValue).value)
        assertEquals("PHYSICAL", (model.placeholders.getValue("damageType") as DescriptionValue.TextValue).value)
        assertEquals(listOf("damage", "armor_break"), model.keywords)
    }

    @Test
    fun `next breakpoint preview emits semantic template instead of reusing talent body`() {
        val preview = requireNotNull(DynamicDescriptionResolver.nextBreakpointPreview(powerStrike, currentRank = 1))

        assertEquals(5, preview.atRank)
        assertEquals("talent.breakpoint.displacement", preview.model.templateKey)
        assertEquals(1, (preview.model.placeholders.getValue("displacementDistance") as DescriptionValue.IntValue).value)
        assertTrue(preview.model.placeholders.containsKey("statusDuration"))
    }
}
