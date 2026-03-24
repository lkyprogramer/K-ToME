package com.ktome.client.ui.talent

import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
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
}
