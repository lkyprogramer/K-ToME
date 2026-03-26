package com.ktome.core.item

import com.ktome.core.random.RandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AffixGeneratorTest {
    @Test
    fun `generator respects equip type and blacklist context`() {
        val generator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            AffixDef(
                                id = "sharp",
                                name = "Sharp",
                                type = AffixType.PREFIX,
                                equipType = AffixEquipType.WEAPON,
                                statModifiers = StatModifier(attack = 3),
                                tags = setOf("weapon", "physical"),
                            ),
                            AffixDef(
                                id = "burning",
                                name = "Burning",
                                type = AffixType.PREFIX,
                                equipType = AffixEquipType.WEAPON,
                                statModifiers = StatModifier(attack = 2),
                                tags = setOf("weapon", "fire"),
                                blacklistTags = setOf("cold"),
                            ),
                            AffixDef(
                                id = "sturdy",
                                name = "Sturdy",
                                type = AffixType.PREFIX,
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
                count = 1,
                equipType = AffixEquipType.WEAPON,
                context = AffixSelectionContext(buildTags = setOf("cold")),
            )

        assertEquals(listOf("sharp"), result.map(AffixDef::id))
    }

    @Test
    fun `tag weighting favors build relevant affixes`() {
        val weighting = AffixTagWeighting()
        val relevant =
            AffixDef(
                id = "stormforged",
                name = "Stormforged",
                type = AffixType.PREFIX,
                equipType = AffixEquipType.WEAPON,
                statModifiers = StatModifier(attack = 3),
                tags = setOf("weapon", "lightning", "arcanist"),
            )
        val irrelevant =
            AffixDef(
                id = "brutal",
                name = "Brutal",
                type = AffixType.PREFIX,
                equipType = AffixEquipType.WEAPON,
                statModifiers = StatModifier(attack = 4),
                tags = setOf("weapon", "vanguard"),
            )

        val context = AffixSelectionContext(buildTags = setOf("arcanist", "lightning"))

        assertTrue(weighting.weight(relevant, context) > weighting.weight(irrelevant, context))
    }

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
