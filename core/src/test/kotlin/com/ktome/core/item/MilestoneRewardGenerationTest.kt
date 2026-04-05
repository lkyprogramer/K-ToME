package com.ktome.core.item

import com.ktome.core.loot.LootBudget
import com.ktome.core.loot.LootRollContext
import com.ktome.core.loot.LootRollResult
import com.ktome.core.loot.RarityTier
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTierEligibility
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
        val generator = ItemGenerator(bundle = weaponBundle(), random = TestRandomSource(ints = listOf(0)))

        val reward =
            generator.generateRoll(
                lootRoll = lootRollResult(rarityTier = RarityTier.NORMAL, affixBudget = 6),
                base = rewardWeaponBase(),
                affixContext =
                    AffixSelectionContext(
                        qualityFloor = RarityTier.MAGIC,
                        minAffixCount = 1,
                        buildTags = setOf("fire"),
                    ),
            )

        assertEquals(RarityTier.MAGIC, reward.item.quality)
        assertEquals(listOf("flaming"), reward.item.affixes.map(AffixDef::id))
        assertEquals(6, reward.trace.affixBudgetConsumed)
        assertEquals(0, reward.trace.affixBudgetDeviation)
    }

    @Test
    fun `milestone affix generation honors blacklist families and route bias`() {
        val generator = ItemGenerator(bundle = weaponBundle(), random = TestRandomSource(ints = listOf(0, 0, 0)))

        val reward =
            generator.generateRoll(
                lootRoll = lootRollResult(qLvl = 13, rarityTier = RarityTier.RARE, affixBudget = 15),
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

        assertEquals(RarityTier.RARE, reward.item.quality)
        assertEquals(listOf("shadowed", "swift", "striking"), reward.item.affixes.map(AffixDef::id))
        assertFalse(reward.item.affixes.any { affix -> affix.id == "flaming" })
    }

    @Test
    fun `milestone reward generation fails fast when affix floor is impossible`() {
        val bundle =
            ItemDataBundle(
                baseItems = listOf(rewardWeaponBase()),
                materials = emptyList(),
                affixes =
                    listOf(
                        affix(
                            id = "single_prefix",
                            family = "physical",
                            cost = 6,
                            statModifiers = StatModifier(attack = 2),
                            tags = setOf("weapon", "physical"),
                        ),
                    ),
            )
        val generator = ItemGenerator(bundle = bundle, random = TestRandomSource(ints = listOf(0)))

        assertThrows(IllegalArgumentException::class.java) {
            generator.generate(
                lootRoll = lootRollResult(qLvl = 13, rarityTier = RarityTier.RARE, affixBudget = 6),
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
    fun `affix generator reports impossible budget constrained generation before item generation`() {
        val affixGenerator =
            AffixGenerator(
                pool =
                    AffixPool(
                        listOf(
                            affix(
                                id = "single_prefix",
                                family = "physical",
                                cost = 6,
                                statModifiers = StatModifier(attack = 2),
                                tags = setOf("weapon", "physical"),
                            ),
                            affix(
                                id = "single_suffix",
                                family = "mobility",
                                type = AffixType.SUFFIX,
                                cost = 6,
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
                budget = 6,
                rarityTier = RarityTier.RARE,
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
                            affix(
                                id = "dead_end_prefix",
                                family = "trap",
                                cost = 10,
                                statModifiers = StatModifier(attack = 2),
                                tags = setOf("weapon", "trap"),
                            ),
                            affix(
                                id = "safe_prefix_one",
                                family = "offense",
                                cost = 6,
                                statModifiers = StatModifier(attack = 1),
                                tags = setOf("weapon", "offense"),
                            ),
                            affix(
                                id = "safe_suffix",
                                family = "mobility",
                                type = AffixType.SUFFIX,
                                cost = 3,
                                statModifiers = StatModifier(speed = 4),
                                tags = setOf("weapon", "mobility"),
                                blacklistTags = setOf("trap"),
                            ),
                            affix(
                                id = "safe_prefix_two",
                                family = "precision",
                                cost = 6,
                                statModifiers = StatModifier(attack = 1),
                                tags = setOf("weapon", "precision"),
                            ),
                        ),
                    ),
                random = TestRandomSource(ints = listOf(0, 0, 0)),
            )

        val selection =
            affixGenerator.generate(
                floor = 5,
                budget = 15,
                rarityTier = RarityTier.RARE,
                equipType = AffixEquipType.WEAPON,
                context = AffixSelectionContext(itemTags = setOf("weapon")),
            )

        assertEquals(listOf("safe_prefix_one", "safe_suffix", "safe_prefix_two"), selection.affixes.map(AffixDef::id))
    }

    private fun lootRollResult(
        qLvl: Int = 7,
        rarityTier: RarityTier,
        affixBudget: Int,
    ): LootRollResult =
        LootRollResult(
            context = lootRollContext(sourceLevel = qLvl),
            budget =
                LootBudget(
                    iLvl = qLvl,
                    qLvl = qLvl,
                    rarityTier = rarityTier,
                    rarityScore = 0.0f,
                    affixBudget = affixBudget,
                    specialTierEligibility = SpecialTierEligibility(availableSpecialTiers = emptySet()),
                ),
            rolledRarityTier = rarityTier,
        )

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
                    affix(
                        id = "flaming",
                        family = "fire",
                        cost = 6,
                        statModifiers = StatModifier(attack = 2),
                        tags = setOf("weapon", "fire", "offense"),
                    ),
                    affix(
                        id = "shadowed",
                        family = "shadow",
                        cost = 6,
                        statModifiers = StatModifier(attack = 2),
                        tags = setOf("weapon", "shadow", "offense"),
                    ),
                    affix(
                        id = "striking",
                        family = "physical",
                        cost = 6,
                        statModifiers = StatModifier(attack = 1),
                        tags = setOf("weapon", "physical", "strength"),
                    ),
                    affix(
                        id = "swift",
                        family = "mobility",
                        type = AffixType.SUFFIX,
                        cost = 3,
                        statModifiers = StatModifier(speed = 8),
                        tags = setOf("weapon", "mobility"),
                    ),
                    affix(
                        id = "warded",
                        family = "protection",
                        type = AffixType.SUFFIX,
                        cost = 3,
                        statModifiers = StatModifier(defense = 2),
                        tags = setOf("weapon", "protection"),
                    ),
                ),
        )

    private fun affix(
        id: String,
        family: String,
        cost: Int,
        type: AffixType = AffixType.PREFIX,
        statModifiers: StatModifier,
        tags: Set<String> = emptySet(),
        blacklistTags: Set<String> = emptySet(),
    ): AffixDef =
        AffixDef(
            id = id,
            name = id,
            type = type,
            equipType = AffixEquipType.WEAPON,
            cost = cost,
            affixFamily = family,
            statModifiers = statModifiers,
            tags = tags,
            blacklistTags = blacklistTags,
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
