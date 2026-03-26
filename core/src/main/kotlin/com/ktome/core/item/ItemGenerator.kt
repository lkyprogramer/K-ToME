package com.ktome.core.item

import com.ktome.core.random.RandomSource
import kotlin.random.Random

class ItemGenerator(
    private val bundle: ItemDataBundle,
    private val random: RandomSource,
    private val affixGenerator: AffixGenerator = AffixGenerator(AffixPool(bundle.affixes), random),
) {
    fun generate(
        floor: Int,
        affixContext: AffixSelectionContext = AffixSelectionContext(),
    ): ItemInstance {
        val base = chooseBaseItem(floor)
        return generate(base, floor, affixContext)
    }

    fun generate(
        base: ItemBaseDef,
        floor: Int,
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
                quality = ItemQuality.COMMON,
                stats = base.baseStats,
                effect = base.effect,
                resourceTypeId = base.resourceTypeId,
                magnitude = base.magnitude,
                passive = base.passive,
            )
        }

        val quality = chooseQuality(floor = floor, qualityFloor = affixContext.qualityFloor)
        val material = chooseMaterial(base, floor)
        val requiredAffixCount = maxOf(quality.affixCount, affixContext.minAffixCount)
        val resolvedAffixContext =
            affixContext.copy(
                itemTags = affixContext.itemTags + base.tags,
                qualityFloor = quality,
                minAffixCount = requiredAffixCount,
            )
        val affixes =
            resolveEquipType(base)?.let { equipType ->
                affixGenerator.generate(
                    floor = floor,
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

    fun generate(
        floor: Int,
        seed: Long,
    ): ItemInstance = ItemGenerator(bundle, RandomSource.from(Random(seed))).generate(floor)

    private fun chooseBaseItem(floor: Int): ItemBaseDef {
        val candidates = bundle.baseItems.filter { floor in it.dropFloors }
        require(candidates.isNotEmpty()) { "No base items are configured for floor $floor." }
        return chooseWeighted(candidates) { it.dropWeight }
    }

    private fun chooseQuality(
        floor: Int,
        qualityFloor: ItemQuality? = null,
    ): ItemQuality {
        val commonWeight = (60 - floor * 5).coerceAtLeast(20)
        val magicWeight = 30 + floor * 3
        val rareWeight = 10 + floor * 2
        val roll = random.nextInt(0, commonWeight + magicWeight + rareWeight)
        val rolled =
            when {
            roll < commonWeight -> ItemQuality.COMMON
            roll < commonWeight + magicWeight -> ItemQuality.MAGIC
            else -> ItemQuality.RARE
        }
        return if (qualityFloor == null || rolled.ordinal >= qualityFloor.ordinal) rolled else qualityFloor
    }

    private fun chooseMaterial(
        base: ItemBaseDef,
        floor: Int,
    ): MaterialDef? {
        if (base.allowedMaterials.isEmpty()) {
            return null
        }

        val candidates = bundle.materials.filter { it.id in base.allowedMaterials && floor >= it.minFloor }
        require(candidates.isNotEmpty()) { "No materials are configured for ${base.id} on floor $floor." }
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
}
