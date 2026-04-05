package com.ktome.core.item

import com.ktome.core.loot.LootBudgetResolver
import com.ktome.core.loot.LootRollContext
import com.ktome.core.loot.LootRollResult
import com.ktome.core.loot.PityTracker
import com.ktome.core.loot.RarityTier
import com.ktome.core.random.RandomSource
import com.ktome.core.mapgen.ZoneRewardProfile

data class GeneratedItemRoll(
    val item: ItemInstance,
    val rollResult: LootRollResult,
)

class ItemGenerator(
    private val bundle: ItemDataBundle,
    private val random: RandomSource,
    private val affixGenerator: AffixGenerator = AffixGenerator(AffixPool(bundle.affixes), random),
    private val lootBudgetResolver: LootBudgetResolver = LootBudgetResolver(random),
) {
    fun generate(
        lootRoll: LootRollResult,
        base: ItemBaseDef,
        affixContext: AffixSelectionContext = AffixSelectionContext(),
    ): ItemInstance {
        if (base.type == ItemType.CONSUMABLE) {
            return ItemInstance(
                baseId = base.id,
                name = base.name,
                type = base.type,
                slot = null,
                glyph = base.glyph,
                colorHex = base.colorHex,
                quality = RarityTier.NORMAL,
                stats = base.baseStats,
                effect = base.effect,
                resourceTypeId = base.resourceTypeId,
                magnitude = base.magnitude,
                passive = base.passive,
            )
        }

        val quality = affixContext.qualityFloor?.takeIf { minimum -> minimum.ordinal > lootRoll.resolvedRarityTier.ordinal } ?: lootRoll.resolvedRarityTier
        val material = chooseMaterial(base, lootRoll.budget.qLvl)
        val requiredAffixCount = maxOf(quality.defaultAffixCount(), affixContext.minAffixCount)
        val resolvedAffixContext =
            affixContext.copy(
                itemTags = affixContext.itemTags + base.tags,
                qualityFloor = quality,
                minAffixCount = requiredAffixCount,
            )
        val affixes =
            resolveEquipType(base)?.let { equipType ->
                affixGenerator.generate(
                    floor = legacyFloorBandForItemLevel(lootRoll.budget.qLvl),
                    count = requiredAffixCount,
                    equipType = equipType,
                    context = resolvedAffixContext,
                )
            }.orEmpty()
        require(affixes.size >= resolvedAffixContext.minAffixCount) {
            "Reward item '${base.id}' failed to satisfy minAffixCount=${resolvedAffixContext.minAffixCount}."
        }
        val stats = listOf(base.baseStats, material?.statModifiers ?: StatModifier.ZERO)
            .plus(affixes.map(AffixDef::statModifiers))
            .fold(StatModifier.ZERO) { acc, modifier -> acc + modifier }

        return ItemInstance(
            baseId = base.id,
            name = buildName(base, material, affixes),
            type = base.type,
            slot = base.slot,
            glyph = base.glyph,
            colorHex = base.colorHex,
            quality = quality,
            materialId = material?.id,
            materialName = material?.name,
            affixes = affixes,
            stats = stats,
            effect = base.effect,
            resourceTypeId = base.resourceTypeId,
            magnitude = base.magnitude,
            passive = base.passive,
        )
    }

    fun rollAndGenerate(
        context: LootRollContext,
        zoneRewardProfile: ZoneRewardProfile,
        affixContext: AffixSelectionContext = AffixSelectionContext(),
        pityTracker: PityTracker = PityTracker(),
        base: ItemBaseDef? = null,
    ): GeneratedItemRoll {
        val rollResult =
            lootBudgetResolver.roll(
                context = context,
                zoneRewardProfile = zoneRewardProfile,
                pityTracker = pityTracker,
                minimumRarityTier = affixContext.qualityFloor,
            )
        val resolvedBase = base ?: chooseBaseItem(rollResult.budget.iLvl)
        return GeneratedItemRoll(
            item = generate(lootRoll = rollResult, base = resolvedBase, affixContext = affixContext),
            rollResult = rollResult,
        )
    }

    fun generate(
        context: LootRollContext,
        zoneRewardProfile: ZoneRewardProfile,
        affixContext: AffixSelectionContext = AffixSelectionContext(),
        pityTracker: PityTracker = PityTracker(),
        base: ItemBaseDef? = null,
    ): ItemInstance =
        rollAndGenerate(
            context = context,
            zoneRewardProfile = zoneRewardProfile,
            affixContext = affixContext,
            pityTracker = pityTracker,
            base = base,
        ).item

    private fun chooseBaseItem(itemLevel: Int): ItemBaseDef {
        val floorBand = legacyFloorBandForItemLevel(itemLevel)
        val candidates = bundle.baseItems.filter { floorBand in it.dropFloors }
        require(candidates.isNotEmpty()) { "No base items are configured for floor band $floorBand (itemLevel=$itemLevel)." }
        return chooseWeighted(candidates) { it.dropWeight }
    }

    private fun chooseMaterial(
        base: ItemBaseDef,
        itemLevel: Int,
    ): MaterialDef? {
        if (base.allowedMaterials.isEmpty()) {
            return null
        }

        val floorBand = legacyFloorBandForItemLevel(itemLevel)
        val candidates = bundle.materials.filter { it.id in base.allowedMaterials && floorBand >= it.minFloor }
        require(candidates.isNotEmpty()) { "No materials are configured for ${base.id} on floor band $floorBand (itemLevel=$itemLevel)." }
        return candidates[random.nextInt(0, candidates.size)]
    }

    private fun resolveEquipType(base: ItemBaseDef): AffixEquipType? =
        when (base.type) {
            ItemType.WEAPON -> AffixEquipType.WEAPON
            ItemType.ARMOR -> AffixEquipType.ARMOR
            ItemType.CONSUMABLE -> null
        }

    private fun buildName(
        base: ItemBaseDef,
        material: MaterialDef?,
        affixes: List<AffixDef>,
    ): String {
        val prefixes = affixes.filter { it.type == AffixType.PREFIX }.joinToString("") { it.name }
        val suffixes = affixes.filter { it.type == AffixType.SUFFIX }.joinToString(" ") { it.name }
        return buildString {
            if (prefixes.isNotBlank()) {
                append(prefixes)
                append(' ')
            }
            material?.name?.let {
                append(it)
                append(' ')
            }
            append(base.name)
            if (suffixes.isNotBlank()) {
                append(' ')
                append(suffixes)
            }
        }
    }

    private fun <T> chooseWeighted(
        values: List<T>,
        weightOf: (T) -> Int,
    ): T {
        val totalWeight = values.sumOf(weightOf)
        require(totalWeight > 0) { "Weighted selection requires a positive total weight." }
        var roll = random.nextInt(0, totalWeight)
        values.forEach { value ->
            roll -= weightOf(value)
            if (roll < 0) {
                return value
            }
        }
        return values.last()
    }

    private fun legacyFloorBandForItemLevel(itemLevel: Int): Int =
        ((itemLevel - 1) / 3 + 1).coerceIn(1, 5)
}
