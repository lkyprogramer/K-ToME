package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.random.RandomSource
import com.ktome.core.support.TestRandomSource
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemGeneratorTest {
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
        val first = ItemGenerator(bundle, RandomSource.from(Random(42))).generate(2)
        val second = ItemGenerator(bundle, RandomSource.from(Random(42))).generate(2)

        assertEquals(first, second)
    }

    @Test
    fun `higher floors unlock stronger materials`() {
        val generator = ItemGenerator(bundle, TestRandomSource(ints = listOf(0, 1, 1)))

        val generated = generator.generate(2)

        assertEquals("STEEL", generated.materialId)
        assertTrue(generated.stats.attack >= 8)
    }

    @Test
    fun `quality controls affix count`() {
        val generator =
            ItemGenerator(
                bundle,
                TestRandomSource(
                    ints = listOf(0, 99, 0, 0, 0),
                    defaultInt = 0,
                    defaultDouble = 0.95,
                ),
            )

        val generated = generator.generate(3)

        assertEquals(ItemQuality.RARE, generated.quality)
        assertEquals(2, generated.affixes.size)
    }

    @Test
    fun `consumables bypass material and affix generation`() {
        val generator = ItemGenerator(bundle, TestRandomSource(ints = listOf(14)))

        val generated = generator.generate(1)

        assertEquals(ItemType.CONSUMABLE, generated.type)
        assertEquals(ItemQuality.COMMON, generated.quality)
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
                TestRandomSource(ints = listOf(50, 1)),
            ).generate(
                base = base,
                floor = 3,
                affixContext = AffixSelectionContext(buildTags = setOf("arcanist")),
            )
        val withoutBuildTags =
            ItemGenerator(
                affixBundle,
                TestRandomSource(ints = listOf(50, 1)),
            ).generate(
                base = base,
                floor = 3,
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
                TestRandomSource(ints = listOf(0, 0, 0)),
            ).generate(
                base = base,
                floor = 4,
                affixContext = AffixSelectionContext(minAffixCount = 1),
            )

        assertEquals("of_smite", generated.affixes.single().id)
        assertTrue(generated.passive is EquipmentPassive.DamageTypeBonus)
        assertTrue(generated.affixes.single().passive is EquipmentPassive.DamageVsStatus)
    }
}
