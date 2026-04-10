package com.ktome.core.item

import com.ktome.core.loot.RarityTier
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.core.random.RandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AffixGeneratorTest {
    @Test
    fun `generator respects equip type blacklist and budget constraints`() {
        val generator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            affix(
                                id = "sharp",
                                family = "physical",
                                cost = 6,
                                statModifiers = StatModifier(attack = 3),
                                tags = setOf("weapon", "physical"),
                            ),
                            affix(
                                id = "burning",
                                family = "fire",
                                cost = 6,
                                statModifiers = StatModifier(attack = 2),
                                tags = setOf("weapon", "fire"),
                                blacklistTags = setOf("cold"),
                            ),
                            affix(
                                id = "sturdy",
                                family = "defense",
                                cost = 6,
                                equipType = AffixEquipType.ARMOR,
                                statModifiers = StatModifier(defense = 2),
                            ),
                        ),
                    ),
                random = FixedRandomSource(0),
            )

        val result =
            generator.generate(
                floor = 3,
                budget = 6,
                rarityTier = RarityTier.MAGIC,
                equipType = AffixEquipType.WEAPON,
                context = AffixSelectionContext(buildTags = setOf("cold")),
            )

        assertEquals(listOf("sharp"), result.affixes.map(AffixDef::id))
        assertEquals(6, result.budgetConsumed)
        assertEquals(6, result.costBreakdown.single().cost)
    }

    @Test
    fun `generator rejects duplicate family and exclusive group combinations during candidate filtering`() {
        val generator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            affix(id = "storm_prefix", family = "lightning", cost = 6, exclusiveGroup = "element", statModifiers = StatModifier(attack = 3)),
                            affix(
                                id = "shock_suffix",
                                family = "lightning_suffix",
                                type = AffixType.SUFFIX,
                                cost = 6,
                                exclusiveGroup = "element",
                                statModifiers = StatModifier(speed = 6),
                            ),
                            affix(
                                id = "warded_suffix",
                                family = "protection",
                                type = AffixType.SUFFIX,
                                cost = 6,
                                statModifiers = StatModifier(defense = 3),
                            ),
                        ),
                    ),
                random = FixedRandomSource(0),
            )

        val result =
            generator.generate(
                floor = 4,
                budget = 12,
                rarityTier = RarityTier.RARE,
                equipType = AffixEquipType.WEAPON,
                context = AffixSelectionContext(itemTags = setOf("weapon")),
            )

        assertEquals(listOf("storm_prefix", "warded_suffix"), result.affixes.map(AffixDef::id))
    }

    @Test
    fun `generator allows at most one trivial affix and reserves cast speed for non trivial bands`() {
        val generator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            affix(
                                id = "swift_trivial",
                                family = "cast_speed",
                                cost = 1,
                                statModifiers = StatModifier(castSpeedRating = 4),
                                tags = setOf("weapon", "arcanist"),
                            ),
                            affix(
                                id = "swift_minor",
                                family = "cast_speed",
                                cost = 3,
                                statModifiers = StatModifier(castSpeedRating = 10),
                                tags = setOf("weapon", "arcanist"),
                            ),
                            affix(
                                id = "light_suffix",
                                family = "utility",
                                type = AffixType.SUFFIX,
                                cost = 1,
                                statModifiers = StatModifier(speed = 2),
                            ),
                            affix(
                                id = "ward_suffix",
                                family = "protection",
                                type = AffixType.SUFFIX,
                                cost = 3,
                                statModifiers = StatModifier(defense = 2),
                            ),
                        ),
                    ),
                random = FixedRandomSource(0),
            )

        val result =
            generator.generate(
                floor = 5,
                budget = 4,
                rarityTier = RarityTier.RARE,
                equipType = AffixEquipType.WEAPON,
                context = AffixSelectionContext(buildTags = setOf("arcanist")),
            )

        assertEquals(listOf("swift_minor", "light_suffix"), result.affixes.map(AffixDef::id))
        assertTrue(result.affixes.count { affix -> affix.cost == 1 } <= 1)
        assertTrue(result.affixes.none { affix -> affix.id == "swift_trivial" })
    }

    @Test
    fun `generator caps budget target to the affix stage envelope and records raw shortfall`() {
        val generator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            affix(id = "sturdy", family = "armor_plate", equipType = AffixEquipType.ARMOR, cost = 6, statModifiers = StatModifier(defense = 2)),
                            affix(
                                id = "warded",
                                family = "armor_guard",
                                equipType = AffixEquipType.ARMOR,
                                type = AffixType.SUFFIX,
                                cost = 6,
                                statModifiers = StatModifier(defense = 2),
                            ),
                        ),
                    ),
                random = FixedRandomSource(0),
            )

        val result =
            generator.generate(
                floor = 2,
                budget = 15,
                rarityTier = RarityTier.RARE,
                equipType = AffixEquipType.ARMOR,
                context = AffixSelectionContext(itemTags = setOf("armor")),
            )

        assertEquals(12, result.budgetTarget)
        assertEquals(12, result.budgetConsumed)
        assertEquals(3, result.rawBudgetShortfall)
        assertEquals(0, result.affixBudgetDeviation)
    }

    @Test
    fun `generator keeps non zero effective deviation when conflicts block the slot ceiling`() {
        val generator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            affix(id = "storm_core", family = "storm_core", cost = 10, exclusiveGroup = "element", statModifiers = StatModifier(attack = 3)),
                            affix(
                                id = "shock_shell",
                                family = "shock_shell",
                                type = AffixType.SUFFIX,
                                cost = 10,
                                exclusiveGroup = "element",
                                statModifiers = StatModifier(speed = 4),
                            ),
                            affix(
                                id = "guard_wrap",
                                family = "guard_wrap",
                                type = AffixType.SUFFIX,
                                cost = 3,
                                statModifiers = StatModifier(defense = 2),
                            ),
                        ),
                    ),
                random = FixedRandomSource(0),
            )

        val result =
            generator.generate(
                floor = 3,
                budget = 20,
                rarityTier = RarityTier.MAGIC,
                equipType = AffixEquipType.WEAPON,
            )

        assertEquals(20, result.budgetTarget)
        assertEquals(13, result.budgetConsumed)
        assertEquals(7, result.affixBudgetDeviation)
        assertEquals(7, result.rawBudgetShortfall)
    }

    @Test
    fun `generator uses random tiebreak among equally optimal affix sets`() {
        val pool =
            AffixPool(
                listOf(
                    affix(id = "ember", family = "weapon_fire", cost = 6, statModifiers = StatModifier(attack = 2)),
                    affix(id = "frost", family = "weapon_cold", cost = 6, statModifiers = StatModifier(attack = 2)),
                    affix(id = "fury", family = "suffix_fury", type = AffixType.SUFFIX, cost = 6, statModifiers = StatModifier(speed = 3)),
                    affix(id = "guard", family = "suffix_guard", type = AffixType.SUFFIX, cost = 6, statModifiers = StatModifier(defense = 2)),
                ),
            )

        val first =
            AffixGenerator(pool = pool, random = FixedRandomSource(0)).generate(
                floor = 3,
                budget = 12,
                rarityTier = RarityTier.MAGIC,
                equipType = AffixEquipType.WEAPON,
            )
        val second =
            AffixGenerator(pool = pool, random = FixedRandomSource(999)).generate(
                floor = 3,
                budget = 12,
                rarityTier = RarityTier.MAGIC,
                equipType = AffixEquipType.WEAPON,
            )

        assertEquals(12, first.budgetConsumed)
        assertEquals(12, second.budgetConsumed)
        assertTrue(first.affixes.map(AffixDef::id) != second.affixes.map(AffixDef::id))
    }

    @Test
    fun `canGenerate does not advance random state during feasibility checks`() {
        val random = SplitMix64RandomSource.fromSeed(42L)
        val generator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            affix(id = "ember", family = "weapon_fire", cost = 6, statModifiers = StatModifier(attack = 2)),
                            affix(id = "fury", family = "suffix_fury", type = AffixType.SUFFIX, cost = 6, statModifiers = StatModifier(speed = 3)),
                        ),
                    ),
                random = random,
            )

        val before = random.snapshotState()
        assertTrue(
            generator.canGenerate(
                floor = 3,
                budget = 12,
                rarityTier = RarityTier.MAGIC,
                equipType = AffixEquipType.WEAPON,
            ),
        )
        assertEquals(before, random.snapshotState())

        assertFalse(
            generator.canGenerate(
                floor = 3,
                budget = 5,
                rarityTier = RarityTier.RARE,
                equipType = AffixEquipType.WEAPON,
            ),
        )
        assertEquals(before, random.snapshotState())
    }

    @Test
    fun `tag weighting favors build relevant affixes`() {
        val weighting = AffixTagWeighting()
        val relevant =
            affix(
                id = "stormforged",
                family = "lightning",
                cost = 6,
                statModifiers = StatModifier(attack = 3),
                tags = setOf("weapon", "lightning", "arcanist"),
            )
        val irrelevant =
            affix(
                id = "brutal",
                family = "physical",
                cost = 6,
                statModifiers = StatModifier(attack = 4),
                tags = setOf("weapon", "vanguard"),
            )

        val context = AffixSelectionContext(buildTags = setOf("arcanist", "lightning"))

        assertTrue(weighting.weight(relevant, context) > weighting.weight(irrelevant, context))
    }

    @Test
    fun `tag weighting uses affix bias without consuming special template bias`() {
        val weighting = AffixTagWeighting()
        val affixPreferred =
            affix(
                id = "tidal",
                family = "water",
                cost = 6,
                statModifiers = StatModifier(attack = 3),
                tags = setOf("weapon", "water"),
            )
        val specialOnly =
            affix(
                id = "radiant",
                family = "holy",
                cost = 6,
                statModifiers = StatModifier(attack = 3),
                tags = setOf("weapon", "holy"),
            )

        val context =
            AffixSelectionContext(
                affixBiasTags = setOf("water"),
                specialTemplateBiasTags = setOf("holy"),
            )

        assertTrue(weighting.weight(affixPreferred, context) > weighting.weight(specialOnly, context))
    }

    private fun affix(
        id: String,
        family: String,
        cost: Int,
        type: AffixType = AffixType.PREFIX,
        equipType: AffixEquipType = AffixEquipType.WEAPON,
        exclusiveGroup: String? = null,
        statModifiers: StatModifier,
        tags: Set<String> = emptySet(),
        blacklistTags: Set<String> = emptySet(),
    ): AffixDef =
        AffixDef(
            id = id,
            name = id,
            type = type,
            equipType = equipType,
            cost = cost,
            affixFamily = family,
            exclusiveGroup = exclusiveGroup,
            statModifiers = statModifiers,
            tags = tags,
            blacklistTags = blacklistTags,
        )

    private class FixedRandomSource(
        private val value: Int,
    ) : RandomSource {
        override fun nextDouble(): Double = 0.0

        override fun nextInt(
            fromInclusive: Int,
            untilExclusive: Int,
        ): Int = fromInclusive + value.coerceIn(0, untilExclusive - fromInclusive - 1)
    }
}
