package com.ktome.client.ui.status

import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.core.snapshot.TerrainOverrideRenderSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusPresentationModelTest {
    @Test
    fun `status badge formatter is shared and keeps turn information`() {
        val stacked =
            StatusPresentationBuilder.build(
                StatusEffectRenderSnapshot(
                    typeId = "guard",
                    remainingTurns = 4,
                    stackCount = 3,
                    stackCap = 5,
                    category = StatusEffectCategorySnapshot.BUFF,
                ),
            )
        val single =
            StatusPresentationBuilder.build(
                StatusEffectRenderSnapshot(
                    typeId = "stun",
                    remainingTurns = 2,
                    category = StatusEffectCategorySnapshot.DEBUFF,
                ),
            )

        assertEquals("3/5 4t", stacked.badgeText)
        assertEquals("2t", single.badgeText)
        assertEquals(
            stacked.badgeText,
            StatusHudRenderer.renderCompact(
                StatusEffectRenderSnapshot(
                    typeId = "guard",
                    remainingTurns = 4,
                    stackCount = 3,
                    stackCap = 5,
                    category = StatusEffectCategorySnapshot.BUFF,
                ),
            ),
        )
    }

    @Test
    fun `status badge formatter caps large stacks and long durations`() {
        val stacked =
            StatusPresentationBuilder.build(
                StatusEffectRenderSnapshot(
                    typeId = "bleed",
                    remainingTurns = 128,
                    stackCount = 184,
                    stackCap = 240,
                    category = StatusEffectCategorySnapshot.DEBUFF,
                ),
            )
        val longDuration =
            StatusPresentationBuilder.build(
                StatusEffectRenderSnapshot(
                    typeId = "ward",
                    remainingTurns = 120,
                    stackCount = 1,
                    category = StatusEffectCategorySnapshot.BUFF,
                ),
            )

        assertEquals("x99+ 99+", stacked.badgeText)
        assertEquals("99+", longDuration.badgeText)
    }

    @Test
    fun `telegraph overlay priority outranks debuff while zone effect keeps lower weight`() {
        val telegraph =
            StatusPresentationBuilder.buildTelegraph(
                TelegraphStatusPresentationRequest(
                    typeId = "boss_slam",
                    nameKey = null,
                    iconKey = "telegraph.boss_slam",
                    dangerLevel = 4,
                    previewTurnsRemaining = 1,
                ),
            )
        val debuff =
            StatusPresentationBuilder.build(
                StatusEffectRenderSnapshot(
                    typeId = "stun",
                    remainingTurns = 1,
                    stackCount = 10,
                    category = StatusEffectCategorySnapshot.DEBUFF,
                ),
            )
        val zone =
            StatusPresentationBuilder.buildZoneEffect(
                TerrainOverrideRenderSnapshot(
                    sourceRuleId = "burning_oil",
                    ruleNameKey = "terrain.rule.burning_oil",
                    remainingTurns = 3,
                    conductsLightning = false,
                    tickDamageTypeId = "FIRE",
                    tickDamage = 2,
                ),
            )

        assertTrue(telegraph.priority > debuff.priority)
        assertTrue(debuff.priority > zone.priority)
    }

    @Test
    fun `same priority uses type id tiebreaker`() {
        val sorted =
            StatusPresentationBuilder.sorted(
                listOf(
                    StatusPresentationBuilder.build(StatusEffectRenderSnapshot(typeId = "zeta", remainingTurns = 4)),
                    StatusPresentationBuilder.build(StatusEffectRenderSnapshot(typeId = "alpha", remainingTurns = 4)),
                ),
            )

        assertEquals(listOf("alpha", "zeta"), sorted.map(StatusPresentationModel::typeId))
    }
}
