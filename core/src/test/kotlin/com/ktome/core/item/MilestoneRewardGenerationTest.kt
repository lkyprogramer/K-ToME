package com.ktome.core.item

import com.ktome.core.loot.LootRollContext
import com.ktome.core.loot.RarityTier
import com.ktome.core.loot.SourceTier
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.support.TestRandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MilestoneRewardGenerationTest {
    private val zoneRewardProfile =
        ZoneRewardProfile(
            id = "zone.reward.test",
            zoneId = "zone.reward.test",
            rarityBonus = 0.0f,
            qualityBonus = 0,
            baseRewardBudget = 0,
        )

    @Test
    fun `milestone quality floor upgrades common rolls to magic`() {
        val generator = ItemGenerator(bundle = weaponBundle(), random = TestRandomSource(ints = listOf(0, 0, 0, 0, 0)))

        val reward =
            generator.generate(
                context = lootRollContext(sourceLevel = 7),
                zoneRewardProfile = zoneRewardProfile,
                base = rewardWeaponBase(),
                affixContext =
                    AffixSelectionContext(
                        qualityFloor = RarityTier.MAGIC,
                        minAffixCount = 1,
                        buildTags = setOf("fire"),
                    ),
            )

        assertEquals(RarityTier.MAGIC, reward.quality)
        assertEquals(1, reward.affixes.size)
        assertEquals(listOf("flaming"), reward.affixes.map(AffixDef::id))
    }

    @Test
    fun `milestone affix generation honors blacklist families and route bias`() {
        val generator = ItemGenerator(bundle = weaponBundle(), random = TestRandomSource(ints = listOf(0, 0, 0, 0, 0, 0, 0)))

        val reward =
            generator.generate(
                context = lootRollContext(sourceLevel = 13),
                zoneRewardProfile = zoneRewardProfile,
                base = rewardWeaponBase(),
                affixContext =
                    AffixSelectionContext(
                        qualityFloor = RarityTier.RARE,
                        minAffixCount = 3,
                        buildTags = setOf("shadow"),
                        routeBiasTags = setOf("mobility"),
                        blacklistFamilies = setOf("fire"),
                    ),
            )

        assertEquals(RarityTier.RARE, reward.quality)
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
                context = lootRollContext(sourceLevel = 13),
                zoneRewardProfile = zoneRewardProfile,
                base = rewardWeaponBase(),
                affixContext =
                    AffixSelectionContext(
                        qualityFloor = RarityTier.RARE,
                        minAffixCount = 2,
                    ),
            )
        }
    }

    @Test
    fun `affix generator reports impossible affix counts before item generation`() {
        val affixGenerator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            AffixDef(
                                id = "single_prefix",
                                name = "Single Prefix",
                                type = AffixType.PREFIX,
                                equipType = AffixEquipType.WEAPON,
                                statModifiers = StatModifier(attack = 2),
                                tags = setOf("weapon", "physical"),
                            ),
                            AffixDef(
                                id = "single_suffix",
                                name = "Single Suffix",
                                type = AffixType.SUFFIX,
                                equipType = AffixEquipType.WEAPON,
                                statModifiers = StatModifier(speed = 4),
                                tags = setOf("weapon", "mobility"),
                            ),
                        ),
                    ),
                random = TestRandomSource(ints = listOf(0)),
            )

        assertFalse(
            affixGenerator.canGenerate(
                floor = 5,
                count = 3,
                equipType = AffixEquipType.WEAPON,
                context = AffixSelectionContext(itemTags = setOf("weapon")),
            ),
        )
    }

    @Test
    fun `affix generator avoids dead end random picks when a valid chain exists`() {
        val affixGenerator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            AffixDef(
                                id = "dead_end_prefix",
                                name = "Dead End",
                                type = AffixType.PREFIX,
                                equipType = AffixEquipType.WEAPON,
                                statModifiers = StatModifier(attack = 2),
                                tags = setOf("weapon", "trap"),
                            ),
                            AffixDef(
                                id = "safe_prefix_one",
                                name = "Safe One",
                                type = AffixType.PREFIX,
                                equipType = AffixEquipType.WEAPON,
                                statModifiers = StatModifier(attack = 1),
                                tags = setOf("weapon", "offense"),
                            ),
                            AffixDef(
                                id = "safe_suffix",
                                name = "Safe Suffix",
                                type = AffixType.SUFFIX,
                                equipType = AffixEquipType.WEAPON,
                                statModifiers = StatModifier(speed = 4),
                                tags = setOf("weapon", "mobility"),
                                blacklistTags = setOf("trap"),
                            ),
                            AffixDef(
                                id = "safe_prefix_two",
                                name = "Safe Two",
                                type = AffixType.PREFIX,
                                equipType = AffixEquipType.WEAPON,
                                statModifiers = StatModifier(attack = 1),
                                tags = setOf("weapon", "precision"),
                            ),
                        ),
                    ),
                random = TestRandomSource(ints = listOf(0, 0, 0)),
            )

        val affixes =
            affixGenerator.generate(
                floor = 5,
                count = 3,
                equipType = AffixEquipType.WEAPON,
                context = AffixSelectionContext(itemTags = setOf("weapon")),
            )

        assertEquals(3, affixes.size)
        assertEquals("safe_prefix_one", affixes.first().id)
        assertEquals("safe_suffix", affixes[1].id)
    }

    private fun lootRollContext(
        sourceLevel: Int,
        playerLevel: Int = sourceLevel,
        seed: Long = 42L,
    ): LootRollContext =
        LootRollContext(
            sourceLevel = sourceLevel,
            sourceTier = SourceTier.CHEST,
            zoneId = zoneRewardProfile.zoneId,
            playerLevel = playerLevel,
            magicFindBonus = 0.0f,
            seed = seed,
        )

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
