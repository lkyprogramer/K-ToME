package com.ktome.client.ui.status

import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.core.snapshot.TerrainOverrideRenderSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
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

    @Test
    fun `status hud folds overflow into fixed final slot`() {
        val localizer =
            LocalizationBundle
                .fromMaps(
                    mapOf(
                        GameLocale.EN_US to
                            mapOf(
                                "ui.status.fold.summary" to "+{count} more",
                                "ui.status.fold.non_interactive_hint" to "Inspect target for hidden status details.",
                                "ui.status.fold.detail.title" to "{count} folded statuses",
                                "ui.status.fold.detail.body" to "Folded statuses: {statuses}",
                            ),
                        GameLocale.ZH_CN to
                            mapOf(
                                "ui.status.fold.summary" to "+{count} more",
                                "ui.status.fold.non_interactive_hint" to "通过检查目标查看隐藏状态详情。",
                                "ui.status.fold.detail.title" to "{count} 个折叠状态",
                                "ui.status.fold.detail.body" to "已折叠状态：{statuses}",
                            ),
                    ),
                )
                .translator(GameLocale.EN_US)
        val icons =
            (0 until 12).map { index ->
                StatusHudIconModel(
                    asset = testAsset("icon.status.$index"),
                    presentation =
                        StatusPresentationBuilder.build(
                            StatusEffectRenderSnapshot(
                                typeId = "status_$index",
                                iconKey = "icon.status.$index",
                                remainingTurns = index + 1,
                                category = StatusEffectCategorySnapshot.DEBUFF,
                            ),
                        ),
                )
            }

        val presented = StatusHudPresenter.present(localizer, icons)

        assertEquals(StatusHudPresenter.MAX_VISIBLE_STATUS_ICONS, presented.size)
        assertEquals("status_4", presented[4].presentation.typeId)
        assertTrue(presented.last().isFoldBadge)
        assertEquals("+7 more", presented.last().badgeText)
        assertEquals(
            listOf("status_5", "status_6", "status_7", "status_8", "status_9", "status_10", "status_11"),
            presented.last().hiddenPresentations.map { it.typeId },
        )
        assertEquals(false, presented.last().foldInteraction?.interactive)
        assertEquals("Inspect target for hidden status details.", presented.last().foldInteraction?.hint)
        assertEquals("7 folded statuses", presented.last().foldInteraction?.detailTitle)
        assertEquals(
            "Folded statuses: status_5, status_6, status_7, status_8, status_9, status_10, status_11",
            presented.last().foldInteraction?.detailBody,
        )
    }

    private fun testAsset(key: String): ResolvedVisualAsset =
        ResolvedVisualAsset(
            requestedKey = key,
            resolvedKey = key,
            matchedByPrefix = false,
            fallbackUsed = false,
            entry =
                VisualManifestEntry(
                    key = key,
                    category = "icon_status",
                    rawOutputPath = "dark-v1/icons/${key.replace('.', '_')}.png",
                    footprint = "ui",
                ),
        )
}
