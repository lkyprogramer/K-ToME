package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.loot.LootRollContext
import com.ktome.core.loot.RarityTier
import com.ktome.core.loot.SourceTier
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.random.RandomSource
import com.ktome.core.support.TestRandomSource
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemGeneratorTest {
    private val zoneRewardProfile =
        ZoneRewardProfile(
            id = "zone.reward.test",
            zoneId = "zone.reward.test",
            rarityBonus = 0.0f,
            qualityBonus = 0,
            baseRewardBudget = 0,
        )

    private val bundle =
        ItemDataBundle(
            baseItems =
                listOf(
                    ItemBaseDef(
                        id = "short_sword",
                        name = "短剑",
                        type = ItemType.WEAPON,
                        slot = EquipSlot.WEAPON,
                        glyph = ')',
                        colorHex = "#FFFFFF",
                        baseStats = StatModifier(attack = 5),
                        allowedMaterials = listOf("IRON", "STEEL"),
                        dropFloors = listOf(1, 2, 3),
                        dropWeight = 10,
                    ),
                    ItemBaseDef(
                        id = "healing_potion",
                        name = "治疗药水",
                        type = ItemType.CONSUMABLE,
                        glyph = '!',
                        colorHex = "#FF0000",
                        dropFloors = listOf(1, 2, 3),
                        dropWeight = 5,
                        effect = ConsumableEffect.HEAL,
                        magnitude = 30,
                    ),
                ),
            materials =
                listOf(
                    MaterialDef(id = "IRON", name = "铁制", minFloor = 1, statModifiers = StatModifier(attack = 1)),
                    MaterialDef(id = "STEEL", name = "钢制", minFloor = 2, statModifiers = StatModifier(attack = 3)),
                ),
            affixes =
                listOf(
                    AffixDef(id = "sharp", name = "锐利的", type = AffixType.PREFIX, statModifiers = StatModifier(attack = 3)),
                    AffixDef(id = "swift", name = "速度之", type = AffixType.SUFFIX, statModifiers = StatModifier(speed = 10), minFloor = 2),
                ),
        )

    @Test
    fun `seed determinism yields identical items`() {
        val first =
            ItemGenerator(bundle, RandomSource.from(Random(42))).generate(
                context = lootRollContext(sourceLevel = 4, seed = 42L),
                zoneRewardProfile = zoneRewardProfile,
                base = shortSwordBase(),
            )
        val second =
            ItemGenerator(bundle, RandomSource.from(Random(42))).generate(
                context = lootRollContext(sourceLevel = 4, seed = 42L),
                zoneRewardProfile = zoneRewardProfile,
                base = shortSwordBase(),
            )

        assertEquals(first, second)
    }

    @Test
    fun `higher floors unlock stronger materials`() {
        val generator = ItemGenerator(bundle, TestRandomSource(ints = listOf(0, 0, 1)))

        val generated =
            generator.generate(
                context = lootRollContext(sourceLevel = 4),
                zoneRewardProfile = zoneRewardProfile,
                base = shortSwordBase(),
            )

        assertEquals("STEEL", generated.materialId)
        assertTrue(generated.stats.attack >= 8)
    }

    @Test
    fun `quality controls affix count`() {
        val generator =
            ItemGenerator(
                bundle,
                TestRandomSource(
                    ints = listOf(0, 980, 0, 0, 0),
                    defaultInt = 0,
                    defaultDouble = 0.95,
                ),
            )

        val generated =
            generator.generate(
                context = lootRollContext(sourceLevel = 7),
                zoneRewardProfile = zoneRewardProfile,
                base = shortSwordBase(),
            )

        assertEquals(RarityTier.RARE, generated.quality)
        assertEquals(2, generated.affixes.size)
    }

    @Test
    fun `consumables bypass material and affix generation`() {
        val generator = ItemGenerator(bundle, TestRandomSource(ints = listOf(0, 0)))

        val generated =
            generator.generate(
                context = lootRollContext(sourceLevel = 1),
                zoneRewardProfile = zoneRewardProfile,
                base = healingPotionBase(),
            )

        assertEquals(ItemType.CONSUMABLE, generated.type)
        assertEquals(RarityTier.NORMAL, generated.quality)
        assertTrue(generated.affixes.isEmpty())
        assertEquals(null, generated.materialId)
    }

    @Test
    fun `caller provided build tags influence affix generation`() {
        val base =
            ItemBaseDef(
                id = "wand",
                name = "法杖",
                type = ItemType.WEAPON,
                slot = EquipSlot.WEAPON,
                glyph = ')',
                colorHex = "#FFFFFF",
                dropFloors = listOf(3),
            )
        val affixBundle =
            ItemDataBundle(
                baseItems = listOf(base),
                materials = emptyList(),
                affixes =
                    listOf(
                        AffixDef(
                            id = "stormforged",
                            name = "风暴铸造",
                            type = AffixType.PREFIX,
                            statModifiers = StatModifier(attack = 2),
                            tags = setOf("arcanist"),
                        ),
                        AffixDef(
                            id = "brutal",
                            name = "残暴",
                            type = AffixType.PREFIX,
                            statModifiers = StatModifier(attack = 2),
                            tags = setOf("vanguard"),
                        ),
                    ),
            )

        val withBuildTags =
            ItemGenerator(
                affixBundle,
                TestRandomSource(ints = listOf(0, 800, 1)),
            ).generate(
                context = lootRollContext(sourceLevel = 7),
                zoneRewardProfile = zoneRewardProfile,
                base = base,
                affixContext = AffixSelectionContext(buildTags = setOf("arcanist")),
            )
        val withoutBuildTags =
            ItemGenerator(
                affixBundle,
                TestRandomSource(ints = listOf(0, 800, 1)),
            ).generate(
                context = lootRollContext(sourceLevel = 7),
                zoneRewardProfile = zoneRewardProfile,
                base = base,
            )

        assertEquals(listOf("stormforged"), withBuildTags.affixes.map(AffixDef::id))
        assertEquals(listOf("brutal"), withoutBuildTags.affixes.map(AffixDef::id))
    }

    @Test
    fun `generator keeps affix passives on selected affixes without overwriting base passive`() {
        val base =
            ItemBaseDef(
                id = "templar_blade",
                name = "Templar Blade",
                type = ItemType.WEAPON,
                slot = EquipSlot.WEAPON,
                glyph = ')',
                colorHex = "#FFFFFF",
                dropFloors = listOf(4),
                passive = EquipmentPassive.DamageTypeBonus(type = DamageType.HOLY, bonusPercent = 0.10),
            )
        val affixBundle =
            ItemDataBundle(
                baseItems = listOf(base),
                materials = emptyList(),
                affixes =
                    listOf(
                        AffixDef(
                            id = "of_smite",
                            name = "of Smite",
                            type = AffixType.PREFIX,
                            statModifiers = StatModifier(attack = 2),
                            minFloor = 4,
                            passive = EquipmentPassive.DamageVsStatus(statusId = "BANE", bonusPercent = 0.12),
                        ),
                    ),
            )

        val generated =
            ItemGenerator(
                affixBundle,
                TestRandomSource(ints = listOf(0, 800, 0)),
            ).generate(
                context = lootRollContext(sourceLevel = 10),
                zoneRewardProfile = zoneRewardProfile,
                base = base,
                affixContext = AffixSelectionContext(minAffixCount = 1),
            )

        assertEquals("of_smite", generated.affixes.single().id)
        assertTrue(generated.passive is EquipmentPassive.DamageTypeBonus)
        assertTrue(generated.affixes.single().passive is EquipmentPassive.DamageVsStatus)
    }

    private fun shortSwordBase(): ItemBaseDef = requireNotNull(bundle.baseItems.firstOrNull { it.id == "short_sword" })

    private fun healingPotionBase(): ItemBaseDef = requireNotNull(bundle.baseItems.firstOrNull { it.id == "healing_potion" })

    private fun lootRollContext(
        sourceLevel: Int,
        playerLevel: Int = sourceLevel,
        seed: Long = 42L,
    ): LootRollContext =
        LootRollContext(
            sourceLevel = sourceLevel,
            sourceTier = SourceTier.NORMAL,
            zoneId = zoneRewardProfile.zoneId,
            playerLevel = playerLevel,
            magicFindBonus = 0.0f,
            seed = seed,
        )
}
