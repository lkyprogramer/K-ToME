package com.ktome.client.ui.talent

import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.talent.DescriptionContext
import com.ktome.core.talent.DescriptionModel
import com.ktome.core.talent.DescriptionValue
import com.ktome.core.talent.DynamicDescriptionResolver
import com.ktome.core.talent.EffectOp
import com.ktome.core.talent.TalentBreakpointPreview
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentTargetingType
import com.ktome.game.FOUNDATION_BREAKPOINT_PAYOFF_CONTRACTS
import com.ktome.game.FoundationBreakpointPayoffContract
import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class DescriptionPresenterTest {
    @Test
    fun `presenter renders localized description keyword tooltip and breakpoint preview`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val lines =
            DescriptionPresenter.presentForReserveTalent(
                localizer = localizer,
                talent =
                    TalentReserveSnapshot(
                        talentId = "charge",
                        nameKey = "talent.vanguard.charge.name",
                        level = 2,
                        committedLevel = 1,
                        maxLevel = 5,
                        resourceCost = 10,
                        resourceLabelKey = "ui.hud.stamina.short",
                        range = 5,
                        minRange = 1,
                        currentCooldown = 0,
                        maxCooldown = 6,
                        requiresTarget = true,
                        descriptionModel =
                            DescriptionModelSnapshot(
                                templateKey = "talent.vanguard.charge.desc",
                                placeholders =
                                    mapOf(
                                        "minRange" to DescriptionValueSnapshot.IntValue(1),
                                        "range" to DescriptionValueSnapshot.IntValue(5),
                                        "damagePercent" to DescriptionValueSnapshot.IntValue(130),
                                    ),
                                keywords = listOf("damage", "stun"),
                            ),
                        nextBreakpointPreview =
                            TalentBreakpointPreviewSnapshot(
                                atRank = 5,
                                descriptionAddendumKey = "talent.breakpoint.vanguard.guard_stance.hold_line",
                                model =
                                    DescriptionModelSnapshot(
                                        templateKey = "talent.breakpoint.apply_status",
                                        placeholders =
                                            mapOf(
                                                "statusDuration" to DescriptionValueSnapshot.IntValue(2),
                                                "statusId" to
                                                    DescriptionValueSnapshot.StatusValue(
                                                        statusId = "STUN",
                                                        nameKey = "status.stun",
                                                    ),
                                            ),
                                    ),
                            ),
                        hasPendingAllocation = true,
                    ),
            )

        assertTrue(lines.any { line -> line.contains("Rush a foe from 1 to 5 tiles away for 130% Damage.") })
        assertTrue(lines.any { line -> line.contains("Damage:") })
        assertTrue(lines.any { line -> line.contains("Preview rank 2 (live 1).") })
        assertTrue(lines.any { line -> line.contains("Next breakpoint: rank 5.") })
        assertTrue(lines.any { line -> line.contains("hold-the-line payoff") })
        assertTrue(lines.any { line -> line.contains("new status effect for 2 turns") })
    }

    @Test
    fun `presenter marks breakpoint preview lines as secondary content`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val lines =
            DescriptionPresenter.presentReserveTalentLines(
                localizer = localizer,
                talent =
                    TalentReserveSnapshot(
                        talentId = "charge",
                        nameKey = "talent.vanguard.charge.name",
                        level = 2,
                        committedLevel = 1,
                        maxLevel = 5,
                        resourceCost = 10,
                        resourceLabelKey = "ui.hud.stamina.short",
                        range = 5,
                        minRange = 1,
                        currentCooldown = 0,
                        maxCooldown = 6,
                        requiresTarget = true,
                        descriptionModel =
                            DescriptionModelSnapshot(
                                templateKey = "talent.vanguard.charge.desc",
                                placeholders =
                                    mapOf(
                                        "minRange" to DescriptionValueSnapshot.IntValue(1),
                                        "range" to DescriptionValueSnapshot.IntValue(5),
                                        "damagePercent" to DescriptionValueSnapshot.IntValue(130),
                                    ),
                            ),
                        nextBreakpointPreview =
                            TalentBreakpointPreviewSnapshot(
                                atRank = 5,
                                model =
                                    DescriptionModelSnapshot(
                                        templateKey = "talent.breakpoint.apply_status",
                                        placeholders =
                                            mapOf(
                                                "statusDuration" to DescriptionValueSnapshot.IntValue(2),
                                                "statusId" to
                                                    DescriptionValueSnapshot.StatusValue(
                                                        statusId = "STUN",
                                                        nameKey = "status.stun",
                                                    ),
                                            ),
                                    ),
                            ),
                    ),
            )

        val breakpointHeader = lines.first { line -> line.text == "Next breakpoint: rank 5." }
        val breakpointEffect = lines.first { line -> line.text.contains("new status effect for 2 turns") }

        assertEquals(DescriptionLineKind.SECONDARY, breakpointHeader.kind)
        assertEquals(DescriptionLineKind.SECONDARY, breakpointEffect.kind)
    }

    @Test
    fun `presenter renders resource restore breakpoint preview for blink payoff`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        val lines =
            DescriptionPresenter.presentForReserveTalent(
                localizer = localizer,
                talent =
                    TalentReserveSnapshot(
                        talentId = "blink",
                        nameKey = "talent.arcanist.blink.name",
                        level = 1,
                        committedLevel = 1,
                        maxLevel = 5,
                        resourceCost = 14,
                        resourceLabelKey = "ui.hud.mana.short",
                        range = 5,
                        minRange = 2,
                        currentCooldown = 0,
                        maxCooldown = 6,
                        requiresTarget = true,
                        descriptionModel =
                            DescriptionModelSnapshot(
                                templateKey = "talent.arcanist.blink.desc",
                                placeholders =
                                    mapOf(
                                        "range" to DescriptionValueSnapshot.IntValue(5),
                                    ),
                            ),
                        nextBreakpointPreview =
                            TalentBreakpointPreviewSnapshot(
                                atRank = 4,
                                model =
                                    DescriptionModelSnapshot(
                                        templateKey = "talent.breakpoint.resource_restore",
                                        placeholders =
                                            mapOf(
                                                "resourceRestorePercent" to DescriptionValueSnapshot.IntValue(10),
                                            ),
                                    ),
                            ),
                    ),
            )

        assertTrue(lines.any { line -> line.contains("restores mana") })
        assertTrue(lines.any { line -> line.contains("Next breakpoint: rank 4.") })
        assertTrue(lines.any { line -> line.contains("10% resource recovery") })
    }

    @Test
    fun `presenter renders generic custom status names from snapshot instead of hardcoded ids`() {
        val localizer =
            LocalizationBundle.load { path ->
                when (path) {
                    "/i18n/en-US.json" ->
                        """
                        {
                          "ui.locale.en-US": "English",
                          "ui.locale.zh-CN": "Chinese",
                          "talent.test.status_preview": "Applies {statusId}.",
                          "status.war_cry_buff": "War Cry"
                        }
                        """.trimIndent()

                    "/i18n/zh-CN.json" ->
                        """
                        {
                          "ui.locale.en-US": "英文",
                          "ui.locale.zh-CN": "中文",
                          "talent.test.status_preview": "施加 {statusId}。",
                          "status.war_cry_buff": "战吼"
                        }
                        """.trimIndent()

                    else -> error("Unexpected resource $path")
                }
            }.translator(GameLocale.EN_US)

        val lines =
            DescriptionPresenter.presentForReserveTalent(
                localizer = localizer,
                talent =
                    TalentReserveSnapshot(
                        talentId = "war_cry",
                        nameKey = "talent.vanguard.war_cry.name",
                        level = 1,
                        committedLevel = 1,
                        maxLevel = 5,
                        resourceCost = 10,
                        resourceLabelKey = "ui.hud.stamina.short",
                        range = 0,
                        minRange = 0,
                        currentCooldown = 0,
                        maxCooldown = 6,
                        requiresTarget = false,
                        descriptionModel =
                            DescriptionModelSnapshot(
                                templateKey = "talent.test.status_preview",
                                placeholders =
                                    mapOf(
                                        "statusId" to
                                            DescriptionValueSnapshot.StatusValue(
                                                statusId = "war_cry_empower",
                                                nameKey = "status.war_cry_buff",
                                            ),
                                    ),
                            ),
                    ),
            )

        assertTrue(lines.any { line -> line == "Applies War Cry." })
        assertTrue(lines.none { line -> line.contains("war_cry_empower") })
    }

    @Test
    fun `presenter rejects unknown keyword ids instead of silently dropping them`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        assertThrows<IllegalArgumentException> {
            DescriptionPresenter.presentForReserveTalent(
                localizer = localizer,
                talent =
                    TalentReserveSnapshot(
                        talentId = "charge",
                        nameKey = "talent.vanguard.charge.name",
                        level = 1,
                        committedLevel = 1,
                        maxLevel = 5,
                        resourceCost = 10,
                        resourceLabelKey = "ui.hud.stamina.short",
                        range = 5,
                        minRange = 1,
                        currentCooldown = 0,
                        maxCooldown = 6,
                        requiresTarget = true,
                        descriptionModel =
                            DescriptionModelSnapshot(
                                templateKey = "talent.vanguard.charge.desc",
                                placeholders =
                                    mapOf(
                                        "minRange" to DescriptionValueSnapshot.IntValue(1),
                                        "range" to DescriptionValueSnapshot.IntValue(5),
                                        "damagePercent" to DescriptionValueSnapshot.IntValue(130),
                                    ),
                                keywords = listOf("missing_keyword"),
                            ),
                    ),
            )
        }
    }

    @Test
    fun `presenter renders all documented base class breakpoint payoff previews from live talent definitions`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val talentsById = DataLoader(GameLocale.EN_US).loadTalentDefinitions().associateBy(TalentDef::id)

        FOUNDATION_BREAKPOINT_PAYOFF_CONTRACTS.forEach { contract ->
            val talent = requireNotNull(talentsById[contract.talentId])
            val preview = requireNotNull(DynamicDescriptionResolver.nextBreakpointPreview(talent, contract.previewRank))
            val lines =
                DescriptionPresenter.presentReserveTalentLines(
                    localizer = localizer,
                    talent = buildReserveTalentSnapshot(talent, contract.previewRank, preview),
                )

            assertEquals(contract.breakpointRank, preview.atRank)
            assertEquals(contract.descriptionAddendumKey, preview.descriptionAddendumKey)
            assertTrue(
                lines.any { line ->
                    line.kind == DescriptionLineKind.SECONDARY &&
                        line.text == localizer.text("ui.talent.next_breakpoint", "rank" to contract.breakpointRank)
                },
                "Expected ${contract.talentId} to render the live next-breakpoint header.",
            )
            assertTrue(
                lines.any { line -> line.kind == DescriptionLineKind.SECONDARY && line.text == localizer.text(contract.descriptionAddendumKey) },
                "Expected ${contract.talentId} to render ${contract.descriptionAddendumKey}.",
            )
            assertTrue(
                lines.count { line -> line.kind == DescriptionLineKind.SECONDARY } >= 3,
                "Expected ${contract.talentId} preview to render header + addendum + effect lines, actual=$lines",
            )
            assertTrue(
                lines.none { line -> "!!" in line.text },
                "Expected ${contract.talentId} preview rendering to resolve all localization keys, actual=$lines",
            )
        }
    }

    @Test
    fun `live breakpoint preview semantics stay aligned with unlocked breakpoint effects`() {
        val talentsById = DataLoader(GameLocale.EN_US).loadTalentDefinitions().associateBy(TalentDef::id)

        FOUNDATION_BREAKPOINT_PAYOFF_CONTRACTS.forEach { contract ->
            val talent = requireNotNull(talentsById[contract.talentId])
            val breakpoint = talent.breakpoints.single { documented -> documented.atRank == contract.breakpointRank }
            val preview = requireNotNull(DynamicDescriptionResolver.nextBreakpointPreview(talent, contract.previewRank))
            val primaryEffect = breakpoint.unlockedEffects.first()
            val documentedEffect = breakpoint.unlockedEffects.single { effect -> contract.matchesDocumentedEffect(effect) }

            assertEquals(contract.descriptionAddendumKey, preview.descriptionAddendumKey)
            assertEquals(previewTemplateKey(primaryEffect), preview.model.templateKey)
            assertEquals(contract.breakpointRank, (preview.model.placeholders.getValue("rank") as DescriptionValue.IntValue).value)

            when (primaryEffect) {
                is EffectOp.Damage -> {
                    assertEquals(
                        (primaryEffect.scaling.attackMultiplier * 100.0).toInt(),
                        (preview.model.placeholders.getValue("damagePercent") as DescriptionValue.IntValue).value,
                    )
                }

                is EffectOp.ApplyStatus -> {
                    assertEquals(primaryEffect.statusId, (preview.model.placeholders.getValue("statusId") as DescriptionValue.TextValue).value)
                    assertEquals(primaryEffect.duration, (preview.model.placeholders.getValue("statusDuration") as DescriptionValue.IntValue).value)
                }

                is EffectOp.ResourceRestore -> {
                    assertEquals(
                        (primaryEffect.fraction * 100.0).toInt(),
                        (preview.model.placeholders.getValue("resourceRestorePercent") as DescriptionValue.IntValue).value,
                    )
                }

                is EffectOp.Heal -> {
                    assertEquals(
                        (primaryEffect.maxHpFraction * 100.0).toInt(),
                        (preview.model.placeholders.getValue("healPercent") as DescriptionValue.IntValue).value,
                    )
                }

                is EffectOp.StatModifier,
                is EffectOp.Displacement,
                -> Unit
            }

            when (documentedEffect) {
                is EffectOp.ApplyStatus -> {
                    assertTrue(
                        breakpoint.unlockedEffects.filterIsInstance<EffectOp.ApplyStatus>().any { effect -> effect.statusId == documentedEffect.statusId },
                        "Expected ${contract.talentId} breakpoint to retain documented status payoff ${documentedEffect.statusId}.",
                    )
                }

                is EffectOp.ResourceRestore -> {
                    assertTrue(
                        breakpoint.unlockedEffects.filterIsInstance<EffectOp.ResourceRestore>().any { effect ->
                            effect.type.name == documentedEffect.type.name && effect.fraction == documentedEffect.fraction
                        },
                        "Expected ${contract.talentId} breakpoint to retain documented resource restore payoff.",
                    )
                }

                else -> error("Unsupported documented breakpoint effect $documentedEffect for ${contract.talentId}")
            }
        }
    }

    private fun buildReserveTalentSnapshot(
        talent: TalentDef,
        previewRank: Int,
        preview: TalentBreakpointPreview,
    ): TalentReserveSnapshot {
        val model = DynamicDescriptionResolver.resolve(talent, DescriptionContext(currentRank = previewRank))
        val resourceCost = talent.resolvedResourceCosts().values.firstOrNull() ?: 0
        return TalentReserveSnapshot(
            talentId = talent.id,
            nameKey = talent.nameKey,
            level = previewRank,
            committedLevel = previewRank,
            maxLevel = talent.maxRank,
            resourceCost = resourceCost,
            resourceLabelKey = "ui.hud.stamina.short",
            range = talent.targetingDef.range,
            minRange = talent.targetingDef.minRange,
            currentCooldown = 0,
            maxCooldown = talent.cooldown,
            requiresTarget = talent.targetingDef.type != TalentTargetingType.SELF,
            descriptionModel = model.toSnapshot(),
            nextBreakpointPreview = preview.toSnapshot(),
        )
    }

    private fun DescriptionModel.toSnapshot(): DescriptionModelSnapshot =
        DescriptionModelSnapshot(
            templateKey = templateKey,
            placeholders = placeholders.mapValues { (_, value) -> value.toSnapshot() },
            keywords = keywords,
        )

    private fun DescriptionValue.toSnapshot(): DescriptionValueSnapshot =
        when (this) {
            is DescriptionValue.BooleanValue -> DescriptionValueSnapshot.BooleanValue(value)
            is DescriptionValue.DecimalValue -> DescriptionValueSnapshot.DecimalValue(value)
            is DescriptionValue.IntValue -> DescriptionValueSnapshot.IntValue(value)
            is DescriptionValue.TextValue -> DescriptionValueSnapshot.TextValue(value)
        }

    private fun TalentBreakpointPreview.toSnapshot(): TalentBreakpointPreviewSnapshot =
        TalentBreakpointPreviewSnapshot(
            atRank = atRank,
            descriptionAddendumKey = descriptionAddendumKey,
            model = model.toSnapshot(),
        )

    private fun FoundationBreakpointPayoffContract.matchesDocumentedEffect(effect: EffectOp): Boolean =
        when {
            payoffStatusId != null ->
                effect is EffectOp.ApplyStatus && effect.statusId.equals(payoffStatusId, ignoreCase = true)
            payoffResourceTypeId != null ->
                effect is EffectOp.ResourceRestore && effect.type.name == payoffResourceTypeId
            else -> false
        }

    private fun previewTemplateKey(effect: EffectOp): String =
        when (effect) {
            is EffectOp.ApplyStatus -> "talent.breakpoint.apply_status"
            is EffectOp.Displacement -> "talent.breakpoint.displacement"
            is EffectOp.ResourceRestore -> "talent.breakpoint.resource_restore"
            is EffectOp.Heal -> "talent.breakpoint.heal"
            is EffectOp.StatModifier -> "talent.breakpoint.stat_modifier"
            is EffectOp.Damage -> "talent.breakpoint.damage"
        }
}
