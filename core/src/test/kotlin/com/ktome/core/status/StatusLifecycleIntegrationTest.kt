package com.ktome.core.status

import com.ktome.core.ecs.EntityId
import com.ktome.core.item.StatModifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatusLifecycleIntegrationTest {
    @Test
    fun `unique status rule is driven by generic fields instead of name level special cases`() {
        val tracker = StatusTracker()

        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                definition = warCryEmpowerDefinition(),
                effectId = "war_cry_first",
                duration = 4,
                magnitude = 0.20,
                sourceEntityId = EntityId(1),
            ),
        )
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                definition = warCryEmpowerDefinition(),
                effectId = "war_cry_refresh",
                duration = 6,
                magnitude = 0.35,
                sourceEntityId = EntityId(1),
            ),
        )
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                definition = warCryEmpowerDefinition(),
                effectId = "war_cry_other_source",
                duration = 5,
                magnitude = 0.25,
                sourceEntityId = EntityId(2),
            ),
        )

        val warCryEffects = tracker.activeEffects().filter { effect -> effect.schemaId == "war_cry_empower" }.sortedBy { effect -> effect.sourceEntityId?.value }

        assertEquals(2, warCryEffects.size)
        assertEquals(6, warCryEffects.first().remainingTurns)
        assertEquals(0.35, warCryEffects.first().magnitude, 0.0001)
        assertEquals(5, warCryEffects.last().remainingTurns)
        assertEquals(0.25, warCryEffects.last().magnitude, 0.0001)
    }

    private fun warCryEmpowerDefinition(): StatusEffectDef =
        StatusEffectDef(
            id = "war_cry_empower",
            type = StatusEffectType.CUSTOM,
            category = EffectCategory.BUFF,
            nameKey = "status.war_cry_buff",
            stackingRule = StackingRule.UNIQUE,
            replacePolicy = ReplacePolicy.KEEP_STRONGEST,
            uniquenessKey = "war_cry_empower",
            sourceScopedUnique = true,
            statModifier = StatModifier(attackMultiplierBonus = 1.0),
        )
}
