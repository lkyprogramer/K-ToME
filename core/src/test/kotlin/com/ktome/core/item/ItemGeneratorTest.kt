package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.combat.DiminishingReturns
import com.ktome.core.loot.LootBudget
import com.ktome.core.loot.LootRollContext
import com.ktome.core.loot.LootRollResult
import com.ktome.core.loot.PityTracker
import com.ktome.core.loot.RarityTier
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier
import com.ktome.core.loot.SpecialTierEligibility
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.random.RandomSource
import com.ktome.core.support.TestRandomSource
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
                        id = "sun_blade_unique",
                        name = "日耀圣刃",
                        type = ItemType.WEAPON,
                        slot = EquipSlot.WEAPON,
                        glyph = ')',
                        colorHex = "#FFF4AA",
                        baseStats = StatModifier(attack = 7),
                        allowedMaterials = listOf("STEEL"),
                        dropFloors = listOf(2, 3, 4),
                        dropWeight = 0,
                        passive = EquipmentPassive.DamageTypeBonus(type = DamageType.HOLY, bonusPercent = 0.12),
                    ),
                    ItemBaseDef(
                        id = "river_echo_artifact",
                        name = "河音回响",
                        type = ItemType.ARMOR,
                        slot = EquipSlot.OFF_HAND,
                        glyph = ']',
                        colorHex = "#7FD8FF",
                        baseStats = StatModifier(defense = 2, castSpeedRating = 8),
                        allowedMaterials = listOf("STEEL"),
                        dropFloors = listOf(2, 3, 4),
                        dropWeight = 0,
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
                    affix(id = "sharp", family = "physical", cost = 6, statModifiers = StatModifier(attack = 3)),
                    affix(id = "swift", family = "mobility", type = AffixType.SUFFIX, cost = 6, statModifiers = StatModifier(speed = 10), minFloor = 2),
                    affix(id = "holy_edge", family = "holy", cost = 10, statModifiers = StatModifier(attack = 4)),
                    affix(id = "of_haste", family = "cast_speed", type = AffixType.SUFFIX, cost = 3, statModifiers = StatModifier(castSpeedRating = 12)),
                ),
            specialTemplates =
                listOf(
                    SpecialItemTemplate(
                        id = "unique.sun_blade",
                        itemId = "sun_blade_unique",
                        specialTier = SpecialTier.UNIQUE,
                        nameKey = "item.unique.sun_blade.name",
                        descKey = "item.unique.sun_blade.desc",
                        visualKey = "item.unique.sun_blade",
                        iconKey = "icon.item.unique.sun_blade",
                        audioProfile = "audio.item.unique.sun_blade",
                        schemaVersion = 2,
                        tags = setOf("holy", "templar"),
                        allowedSourceTiers = setOf(SourceTier.ELITE, SourceTier.BOSS),
                        allowedZones = setOf(zoneRewardProfile.zoneId),
                        fixedAffixIds = listOf("holy_edge", "of_haste"),
                        fixedMaterialId = "STEEL",
                    ),
                    SpecialItemTemplate(
                        id = "artifact.river_echo",
                        itemId = "river_echo_artifact",
                        specialTier = SpecialTier.ARTIFACT,
                        nameKey = "item.artifact.river_echo.name",
                        descKey = "item.artifact.river_echo.desc",
                        visualKey = "item.artifact.river_echo",
                        iconKey = "icon.item.artifact.river_echo",
                        audioProfile = "audio.item.artifact.river_echo",
                        schemaVersion = 2,
                        tags = setOf("secret_zone", "arcanist"),
                        allowedSourceTiers = setOf(SourceTier.CHEST, SourceTier.SECRET_ZONE),
                        allowedZones = setOf(zoneRewardProfile.zoneId),
                        fixedAffixIds = listOf("of_haste"),
                        fixedMaterialId = "STEEL",
                    ),
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
        val generator = ItemGenerator(bundle, TestRandomSource(ints = listOf(1)))

        val generated =
            generator.generate(
                lootRoll = lootRollResult(qLvl = 7, rarityTier = RarityTier.NORMAL, affixBudget = 0),
                base = shortSwordBase(),
            )

        assertEquals("STEEL", generated.materialId)
        assertTrue(generated.stats.attack >= 8)
    }

    @Test
    fun `roll and generate enriches special tier eligibility from template registry`() {
        val generated =
            ItemGenerator(bundle, TestRandomSource(ints = listOf(0, 0, 0))).rollAndGenerate(
                context = lootRollContext(sourceLevel = 8, sourceTier = SourceTier.ELITE),
                zoneRewardProfile = zoneRewardProfile,
                base = shortSwordBase(),
            )

        assertEquals(setOf("unique.sun_blade"), generated.rollResult.budget.specialTierEligibility.availableTemplateIds)
        assertEquals(SpecialTier.UNIQUE, generated.trace.specialTier)
        assertEquals("unique.sun_blade", generated.trace.specialTemplateId)
        assertEquals("sun_blade_unique", generated.item.baseId)
    }

    @Test
    fun `secret zone source tier resolves artifact templates through the same registry path`() {
        val generated =
            ItemGenerator(bundle, TestRandomSource(ints = listOf(0, 0, 0))).rollAndGenerate(
                context = lootRollContext(sourceLevel = 8, sourceTier = SourceTier.SECRET_ZONE),
                zoneRewardProfile = zoneRewardProfile,
                specialTierEligibility = SpecialTierEligibility(availableSpecialTiers = setOf(SpecialTier.UNIQUE, SpecialTier.ARTIFACT)),
            )

        assertEquals(setOf("artifact.river_echo"), generated.rollResult.budget.specialTierEligibility.availableTemplateIds)
        assertEquals(SpecialTier.ARTIFACT, generated.trace.specialTier)
        assertEquals("artifact.river_echo", generated.trace.specialTemplateId)
        assertEquals("river_echo_artifact", generated.item.baseId)
    }

    @Test
    fun `chest source tier keeps river echo artifact reachable in live reward paths`() {
        val generated =
            ItemGenerator(bundle, TestRandomSource(ints = listOf(0, 0, 0))).rollAndGenerate(
                context = lootRollContext(sourceLevel = 8, sourceTier = SourceTier.CHEST),
                zoneRewardProfile = zoneRewardProfile,
                specialTierEligibility = SpecialTierEligibility(availableSpecialTiers = setOf(SpecialTier.UNIQUE, SpecialTier.ARTIFACT)),
            )

        assertEquals(setOf("artifact.river_echo"), generated.rollResult.budget.specialTierEligibility.availableTemplateIds)
        assertEquals(SpecialTier.ARTIFACT, generated.trace.specialTier)
        assertEquals("artifact.river_echo", generated.trace.specialTemplateId)
        assertEquals("river_echo_artifact", generated.item.baseId)
    }

    @Test
    fun `special template selection honors special template bias independently from affix bias`() {
        val biasBundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        ItemBaseDef(
                            id = "holy_unique_base",
                            name = "Holy Base",
                            type = ItemType.WEAPON,
                            slot = EquipSlot.WEAPON,
                            glyph = ')',
                            colorHex = "#FFF8C4",
                            dropFloors = listOf(1),
                        ),
                        ItemBaseDef(
                            id = "shadow_unique_base",
                            name = "Shadow Base",
                            type = ItemType.WEAPON,
                            slot = EquipSlot.WEAPON,
                            glyph = ')',
                            colorHex = "#8866CC",
                            dropFloors = listOf(1),
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
                specialTemplates =
                    listOf(
                        SpecialItemTemplate(
                            id = "unique.holy_preference",
                            itemId = "holy_unique_base",
                            specialTier = SpecialTier.UNIQUE,
                            nameKey = "item.unique.holy_preference.name",
                            descKey = "item.unique.holy_preference.desc",
                            visualKey = "item.unique.holy_preference",
                            iconKey = "icon.item.unique.holy_preference",
                            audioProfile = "audio.item.unique.holy_preference",
                            schemaVersion = 2,
                            tags = setOf("holy"),
                            allowedSourceTiers = setOf(SourceTier.CHEST),
                            allowedZones = setOf(zoneRewardProfile.zoneId),
                        ),
                        SpecialItemTemplate(
                            id = "unique.shadow_preference",
                            itemId = "shadow_unique_base",
                            specialTier = SpecialTier.UNIQUE,
                            nameKey = "item.unique.shadow_preference.name",
                            descKey = "item.unique.shadow_preference.desc",
                            visualKey = "item.unique.shadow_preference",
                            iconKey = "icon.item.unique.shadow_preference",
                            audioProfile = "audio.item.unique.shadow_preference",
                            schemaVersion = 2,
                            tags = setOf("shadow"),
                            allowedSourceTiers = setOf(SourceTier.CHEST),
                            allowedZones = setOf(zoneRewardProfile.zoneId),
                        ),
                    ),
            )

        val generated =
            ItemGenerator(biasBundle, TestRandomSource(ints = listOf(1))).generateRoll(
                lootRoll =
                    lootRollResult(
                        rarityTier = RarityTier.RARE,
                        affixBudget = 0,
                        upgradedSpecialTier = SpecialTier.UNIQUE,
                        specialTierEligibility =
                            SpecialTierEligibility(
                                availableSpecialTiers = setOf(SpecialTier.UNIQUE),
                                availableTemplateIds = setOf("unique.holy_preference", "unique.shadow_preference"),
                            ),
                    ),
                affixContext =
                    AffixSelectionContext(
                        affixBiasTags = setOf("holy"),
                        specialTemplateBiasTags = setOf("shadow"),
                    ),
            )

        assertEquals("unique.shadow_preference", generated.trace.specialTemplateId)
        assertEquals("shadow_unique_base", generated.item.baseId)
    }

    @Test
    fun `special tier rolls select template and emit cast speed trace`() {
        val generated =
            ItemGenerator(bundle, TestRandomSource(ints = listOf(0))).generateRoll(
                lootRoll =
                    lootRollResult(
                        rarityTier = RarityTier.RARE,
                        affixBudget = 24,
                        upgradedSpecialTier = SpecialTier.UNIQUE,
                        specialTierEligibility =
                            SpecialTierEligibility(
                                availableSpecialTiers = setOf(SpecialTier.UNIQUE),
                                availableTemplateIds = setOf("unique.sun_blade"),
                            ),
                        resultingPityTracker = PityTracker(rollsSinceLastRare = 0, eligibleSpecialRollsSinceLastUnique = 0),
                    ),
                previousPityTracker = PityTracker(rollsSinceLastRare = 6, eligibleSpecialRollsSinceLastUnique = 51),
                base = shortSwordBase(),
            )

        assertEquals("sun_blade_unique", generated.item.baseId)
        assertEquals(RarityTier.RARE, generated.item.quality)
        assertEquals("unique.sun_blade", generated.trace.specialTemplateId)
        assertEquals(SpecialTier.UNIQUE, generated.trace.specialTier)
        assertEquals(12, generated.trace.rawCastSpeedRating)
        assertEquals(DiminishingReturns.effectiveCastSpeed(12), generated.trace.effectiveCastSpeed, 1e-6)
        assertEquals(0, generated.trace.affixBudgetDeviation)
        assertEquals(51, generated.trace.previousPityTracker.eligibleSpecialRollsSinceLastUnique)
    }

    @Test
    fun `direct special template generation reuses fixed material and affix rules`() {
        val generated =
            ItemGenerator(bundle, TestRandomSource()).generateSpecialTemplate(
                templateId = "artifact.river_echo",
                itemLevel = 7,
            )

        assertEquals("river_echo_artifact", generated.baseId)
        assertEquals("artifact.river_echo", generated.specialTemplateId)
        assertEquals("STEEL", generated.materialId)
        assertEquals(listOf("of_haste"), generated.affixes.map(AffixDef::id))
        assertEquals(RarityTier.RARE, generated.quality)
        assertEquals(20, generated.stats.castSpeedRating)
    }

    @Test
    fun `special template generation fails fast when fixed affix payload is incomplete`() {
        val brokenBundle =
            bundle.copy(
                specialTemplates =
                    listOf(
                        SpecialItemTemplate(
                            id = "unique.broken_blade",
                            itemId = "sun_blade_unique",
                            specialTier = SpecialTier.UNIQUE,
                            nameKey = "item.unique.broken_blade.name",
                            descKey = "item.unique.broken_blade.desc",
                            visualKey = "item.unique.broken_blade",
                            iconKey = "icon.item.unique.broken_blade",
                            audioProfile = "audio.item.unique.broken_blade",
                            schemaVersion = 2,
                            tags = setOf("holy"),
                            allowedSourceTiers = setOf(SourceTier.ELITE),
                            allowedZones = setOf(zoneRewardProfile.zoneId),
                            fixedAffixIds = listOf("missing_affix"),
                            fixedMaterialId = "STEEL",
                        ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            ItemGenerator(brokenBundle, TestRandomSource(ints = listOf(0))).generateRoll(
                lootRoll =
                    lootRollResult(
                        rarityTier = RarityTier.RARE,
                        affixBudget = 24,
                        upgradedSpecialTier = SpecialTier.UNIQUE,
                        specialTierEligibility =
                            SpecialTierEligibility(
                                availableSpecialTiers = setOf(SpecialTier.UNIQUE),
                                availableTemplateIds = setOf("unique.broken_blade"),
                            ),
                    ),
                base = shortSwordBase(),
            )
        }
    }

    @Test
    fun `consumables bypass material and affix generation`() {
        val generator = ItemGenerator(bundle, TestRandomSource(ints = listOf(0, 0)))

        val generated =
            generator.generate(
                lootRoll = lootRollResult(rarityTier = RarityTier.RARE, affixBudget = 40),
                base = healingPotionBase(),
            )

        assertEquals(ItemType.CONSUMABLE, generated.type)
        assertEquals(RarityTier.NORMAL, generated.quality)
        assertTrue(generated.affixes.isEmpty())
        assertEquals(null, generated.materialId)
    }

    @Test
    fun `formal random loot path excludes consumables from base selection`() {
        val generated =
            ItemGenerator(bundle, TestRandomSource(ints = listOf(0, 14, 0))).rollAndGenerate(
                context = lootRollContext(sourceLevel = 4, seed = 99L),
                zoneRewardProfile = zoneRewardProfile,
            )

        assertEquals("short_sword", generated.item.baseId)
        assertEquals(ItemType.WEAPON, generated.item.type)
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
                        affix(
                            id = "stormforged",
                            family = "lightning",
                            cost = 6,
                            statModifiers = StatModifier(attack = 2),
                            tags = setOf("arcanist"),
                        ),
                        affix(
                            id = "brutal",
                            family = "physical",
                            cost = 6,
                            statModifiers = StatModifier(attack = 2),
                            tags = setOf("vanguard"),
                        ),
                    ),
            )

        val withBuildTags =
            ItemGenerator(
                affixBundle,
                TestRandomSource(ints = listOf(7)),
            ).generate(
                lootRoll = lootRollResult(rarityTier = RarityTier.MAGIC, affixBudget = 6),
                base = base,
                affixContext = AffixSelectionContext(buildTags = setOf("arcanist")),
            )
        val withoutBuildTags =
            ItemGenerator(
                affixBundle,
                TestRandomSource(ints = listOf(7)),
            ).generate(
                lootRoll = lootRollResult(rarityTier = RarityTier.MAGIC, affixBudget = 6),
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
                            cost = 10,
                            affixFamily = "holy_smite",
                            statModifiers = StatModifier(attack = 2),
                            minFloor = 4,
                            passive = EquipmentPassive.DamageVsStatus(statusId = "BANE", bonusPercent = 0.12),
                        ),
                    ),
            )

        val generated =
            ItemGenerator(
                affixBundle,
                TestRandomSource(ints = listOf(0)),
            ).generate(
                lootRoll = lootRollResult(qLvl = 10, rarityTier = RarityTier.MAGIC, affixBudget = 10),
                base = base,
                affixContext = AffixSelectionContext(minAffixCount = 1),
            )

        assertEquals("of_smite", generated.affixes.single().id)
        assertTrue(generated.passive is EquipmentPassive.DamageTypeBonus)
        assertTrue(generated.affixes.single().passive is EquipmentPassive.DamageVsStatus)
    }

    private fun affix(
        id: String,
        family: String,
        cost: Int,
        type: AffixType = AffixType.PREFIX,
        statModifiers: StatModifier,
        tags: Set<String> = emptySet(),
        minFloor: Int = 1,
    ): AffixDef =
        AffixDef(
            id = id,
            name = id,
            type = type,
            cost = cost,
            affixFamily = family,
            statModifiers = statModifiers,
            tags = tags,
            minFloor = minFloor,
        )

    private fun shortSwordBase(): ItemBaseDef = requireNotNull(bundle.baseItems.firstOrNull { it.id == "short_sword" })

    private fun healingPotionBase(): ItemBaseDef = requireNotNull(bundle.baseItems.firstOrNull { it.id == "healing_potion" })

    private fun lootRollContext(
        sourceLevel: Int,
        playerLevel: Int = sourceLevel,
        seed: Long = 42L,
        sourceTier: SourceTier = SourceTier.NORMAL,
    ): LootRollContext =
        LootRollContext(
            sourceLevel = sourceLevel,
            sourceTier = sourceTier,
            zoneId = zoneRewardProfile.zoneId,
            playerLevel = playerLevel,
            magicFindBonus = 0.0f,
            seed = seed,
        )

    private fun lootRollResult(
        qLvl: Int = 7,
        rarityTier: RarityTier,
        affixBudget: Int,
        upgradedSpecialTier: SpecialTier? = null,
        specialTierEligibility: SpecialTierEligibility = SpecialTierEligibility(availableSpecialTiers = emptySet()),
        resultingPityTracker: PityTracker = PityTracker(),
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
                    specialTierEligibility = specialTierEligibility,
                ),
            rolledRarityTier = rarityTier,
            upgradedSpecialTier = upgradedSpecialTier,
            specialUpgradeAttempted = upgradedSpecialTier != null,
            resultingPityTracker = resultingPityTracker,
        )
}
