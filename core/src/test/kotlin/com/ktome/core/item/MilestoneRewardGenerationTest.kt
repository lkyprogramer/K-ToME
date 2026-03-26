package com.ktome.core.item

import com.ktome.core.support.TestRandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MilestoneRewardGenerationTest {
    @Test
    fun `milestone quality floor upgrades common rolls to magic`() {
        val generator = ItemGenerator(bundle = weaponBundle(), random = TestRandomSource(ints = listOf(0, 0, 0, 0, 0)))

        val reward =
            generator.generate(
                base = rewardWeaponBase(),
                floor = 3,
                affixContext =
                    AffixSelectionContext(
                        qualityFloor = ItemQuality.MAGIC,
                        minAffixCount = 1,
                        buildTags = setOf("fire"),
                    ),
            )

        assertEquals(ItemQuality.MAGIC, reward.quality)
        assertEquals(1, reward.affixes.size)
        assertEquals(listOf("flaming"), reward.affixes.map(AffixDef::id))
    }

    @Test
    fun `milestone affix generation honors blacklist families and route bias`() {
        val generator = ItemGenerator(bundle = weaponBundle(), random = TestRandomSource(ints = listOf(0, 0, 0, 0, 0, 0, 0)))

        val reward =
            generator.generate(
                base = rewardWeaponBase(),
                floor = 5,
                affixContext =
                    AffixSelectionContext(
                        qualityFloor = ItemQuality.RARE,
                        minAffixCount = 3,
                        buildTags = setOf("shadow"),
                        routeBiasTags = setOf("mobility"),
                        blacklistFamilies = setOf("fire"),
                    ),
            )

        assertEquals(ItemQuality.RARE, reward.quality)
        assertEquals(3, reward.affixes.size)
        assertFalse(reward.affixes.any { affix -> affix.id == "flaming" })
        assertEquals(listOf("shadowed", "swift", "striking"), reward.affixes.map(AffixDef::id))
    }

    @Test
    fun `milestone reward generation fails fast when affix floor is impossible`() {
        val bundle =
            ItemDataBundle(
                baseItems = listOf(rewardWeaponBase()),
                materials = emptyList(),
                affixes =
                    listOf(
                        AffixDef(
                            id = "single_prefix",
                            name = "Single Prefix",
                            type = AffixType.PREFIX,
                            equipType = AffixEquipType.WEAPON,
                            statModifiers = StatModifier(attack = 2),
                            tags = setOf("weapon", "physical"),
                        ),
                    ),
            )
        val generator = ItemGenerator(bundle = bundle, random = TestRandomSource(ints = listOf(0, 0, 0)))

        assertThrows(IllegalArgumentException::class.java) {
            generator.generate(
                base = rewardWeaponBase(),
                floor = 5,
                affixContext =
                    AffixSelectionContext(
                        qualityFloor = ItemQuality.RARE,
                        minAffixCount = 2,
                    ),
            )
        }
    }

    private fun weaponBundle(): ItemDataBundle =
        ItemDataBundle(
            baseItems = listOf(rewardWeaponBase()),
            materials = emptyList(),
            affixes =
                listOf(
                    AffixDef(
                        id = "flaming",
                        name = "Flaming",
                        type = AffixType.PREFIX,
                        equipType = AffixEquipType.WEAPON,
                        statModifiers = StatModifier(attack = 2),
                        tags = setOf("weapon", "fire", "offense"),
                    ),
                    AffixDef(
                        id = "shadowed",
                        name = "Shadowed",
                        type = AffixType.PREFIX,
                        equipType = AffixEquipType.WEAPON,
                        statModifiers = StatModifier(attack = 2),
                        tags = setOf("weapon", "shadow", "offense"),
                    ),
                    AffixDef(
                        id = "striking",
                        name = "Striking",
                        type = AffixType.PREFIX,
                        equipType = AffixEquipType.WEAPON,
                        statModifiers = StatModifier(attack = 1),
                        tags = setOf("weapon", "physical", "strength"),
                    ),
                    AffixDef(
                        id = "swift",
                        name = "Swift",
                        type = AffixType.SUFFIX,
                        equipType = AffixEquipType.WEAPON,
                        statModifiers = StatModifier(speed = 8),
                        tags = setOf("weapon", "mobility"),
                    ),
                    AffixDef(
                        id = "warded",
                        name = "Warded",
                        type = AffixType.SUFFIX,
                        equipType = AffixEquipType.WEAPON,
                        statModifiers = StatModifier(defense = 2),
                        tags = setOf("weapon", "protection"),
                    ),
                ),
        )

    private fun rewardWeaponBase(): ItemBaseDef =
        ItemBaseDef(
            id = "reward_sword",
            name = "Reward Sword",
            type = ItemType.WEAPON,
            slot = EquipSlot.WEAPON,
            tags = setOf("item", "weapon", "reward"),
            glyph = ')',
            colorHex = "#FFFFFF",
            baseStats = StatModifier(attack = 6),
            dropFloors = listOf(3, 4, 5),
        )
}
