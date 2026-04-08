package com.ktome.core.item

import com.ktome.core.combat.DiminishingReturns
import com.ktome.core.loot.AffixCost
import com.ktome.core.loot.LootBudgetResolver
import com.ktome.core.loot.LootRollContext
import com.ktome.core.loot.LootRollResult
import com.ktome.core.loot.PityTracker
import com.ktome.core.loot.RarityTier
import com.ktome.core.loot.SpecialTier
import com.ktome.core.loot.SpecialTierEligibility
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.random.RandomSource

data class ItemGenerationTrace(
    val generatedBaseItemId: String,
    val specialTier: SpecialTier? = null,
    val specialTemplateId: String? = null,
    val affixCostBreakdown: List<AffixCost> = emptyList(),
    val affixBudgetTarget: Int = 0,
    val affixBudgetConsumed: Int = 0,
    val affixBudgetDeviation: Int = 0,
    val rawAffixBudgetShortfall: Int = 0,
    val previousPityTracker: PityTracker = PityTracker(),
    val resultingPityTracker: PityTracker = PityTracker(),
    val rawCastSpeedRating: Int = 0,
    val effectiveCastSpeed: Double = 0.0,
) {
    init {
        require(generatedBaseItemId.isNotBlank()) { "ItemGenerationTrace.generatedBaseItemId must not be blank." }
    }
}

data class GeneratedItemRoll(
    val item: ItemInstance,
    val rollResult: LootRollResult,
    val trace: ItemGenerationTrace,
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
    ): ItemInstance =
        generateRoll(
            lootRoll = lootRoll,
            affixContext = affixContext,
            previousPityTracker = PityTracker(),
            base = base,
        ).item

    fun generateRoll(
        lootRoll: LootRollResult,
        affixContext: AffixSelectionContext = AffixSelectionContext(),
        previousPityTracker: PityTracker = PityTracker(),
        base: ItemBaseDef? = null,
    ): GeneratedItemRoll {
        val specialTemplate =
            lootRoll.upgradedSpecialTier?.let { specialTier ->
                selectSpecialTemplate(
                    lootRoll = lootRoll,
                    specialTier = specialTier,
                    context = affixContext,
                )
            }
        return if (specialTemplate != null) {
            generateSpecialItem(
                lootRoll = lootRoll,
                previousPityTracker = previousPityTracker,
                template = specialTemplate,
            )
        } else {
            val resolvedBase = base ?: chooseBaseItem(lootRoll.budget.iLvl)
            generateStandardItem(
                lootRoll = lootRoll,
                previousPityTracker = previousPityTracker,
                base = resolvedBase,
                affixContext = affixContext,
            )
        }
    }

    fun rollAndGenerate(
        context: LootRollContext,
        zoneRewardProfile: ZoneRewardProfile,
        affixContext: AffixSelectionContext = AffixSelectionContext(),
        pityTracker: PityTracker = PityTracker(),
        base: ItemBaseDef? = null,
        specialTierEligibility: SpecialTierEligibility? = null,
    ): GeneratedItemRoll {
        val resolvedEligibility = resolveSpecialTierEligibility(context = context, override = specialTierEligibility)
        val rollResult =
            lootBudgetResolver.roll(
                context = context,
                zoneRewardProfile = zoneRewardProfile,
                specialTierEligibility = resolvedEligibility,
                pityTracker = pityTracker,
                minimumRarityTier = affixContext.qualityFloor,
            )
        return generateRoll(
            lootRoll = rollResult,
            affixContext = affixContext,
            previousPityTracker = pityTracker,
            base = base,
        )
    }

    fun generate(
        context: LootRollContext,
        zoneRewardProfile: ZoneRewardProfile,
        affixContext: AffixSelectionContext = AffixSelectionContext(),
        pityTracker: PityTracker = PityTracker(),
        base: ItemBaseDef? = null,
        specialTierEligibility: SpecialTierEligibility? = null,
    ): ItemInstance =
        rollAndGenerate(
            context = context,
            zoneRewardProfile = zoneRewardProfile,
            affixContext = affixContext,
            pityTracker = pityTracker,
            base = base,
            specialTierEligibility = specialTierEligibility,
        ).item

    private fun generateStandardItem(
        lootRoll: LootRollResult,
        previousPityTracker: PityTracker,
        base: ItemBaseDef,
        affixContext: AffixSelectionContext,
    ): GeneratedItemRoll {
        if (base.type == ItemType.CONSUMABLE) {
            val item =
                ItemInstance(
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
            return GeneratedItemRoll(
                item = item,
                rollResult = lootRoll,
                trace = traceFor(item = item, lootRoll = lootRoll, previousPityTracker = previousPityTracker),
            )
        }

        val quality =
            affixContext.qualityFloor?.takeIf { minimum -> minimum.ordinal > lootRoll.resolvedRarityTier.ordinal }
                ?: lootRoll.resolvedRarityTier
        val material = chooseMaterial(base = base, itemLevel = lootRoll.budget.qLvl)
        val requiredAffixCount = maxOf(quality.minimumAffixCount(), affixContext.minAffixCount)
        val resolvedAffixContext =
            affixContext.copy(
                itemTags = affixContext.itemTags + base.tags,
                qualityFloor = quality,
                minAffixCount = requiredAffixCount,
            )
        val affixSelection =
            resolveEquipType(base)?.let { equipType ->
                affixGenerator.generate(
                    floor = affixFloorBandForQualityLevel(lootRoll.budget.qLvl),
                    budget = lootRoll.budget.affixBudget,
                    rarityTier = quality,
                    equipType = equipType,
                    context = resolvedAffixContext,
                )
            } ?:
                AffixSelectionResult(
                    affixes = emptyList(),
                    costBreakdown = emptyList(),
                    budgetConsumed = 0,
                    budgetTarget = 0,
                    rawBudgetShortfall = 0,
                )
        require(affixSelection.affixes.size >= requiredAffixCount) {
            "Reward item '${base.id}' failed to satisfy minAffixCount=$requiredAffixCount."
        }

        val item =
            buildItemInstance(
                base = base,
                material = material,
                affixes = affixSelection.affixes,
                quality = quality,
                name = buildName(base = base, material = material, affixes = affixSelection.affixes),
            )
        return GeneratedItemRoll(
            item = item,
            rollResult = lootRoll,
            trace =
                traceFor(
                    item = item,
                    lootRoll = lootRoll,
                    previousPityTracker = previousPityTracker,
                    affixCostBreakdown = affixSelection.costBreakdown,
                    affixBudgetTarget = affixSelection.budgetTarget,
                    affixBudgetConsumed = affixSelection.budgetConsumed,
                    affixBudgetDeviation = affixSelection.affixBudgetDeviation,
                    rawAffixBudgetShortfall = affixSelection.rawBudgetShortfall,
                ),
        )
    }

    private fun generateSpecialItem(
        lootRoll: LootRollResult,
        previousPityTracker: PityTracker,
        template: SpecialItemTemplate,
    ): GeneratedItemRoll {
        val base =
            requireNotNull(bundle.baseItems.firstOrNull { item -> item.id == template.itemId }) {
                "Special template '${template.id}' references unknown item '${template.itemId}'."
            }
        val material = template.fixedMaterialId?.let { materialId -> chooseMaterial(base = base, itemLevel = lootRoll.budget.qLvl, fixedMaterialId = materialId) }
        val affixes =
            template.fixedAffixIds.map { affixId ->
                requireNotNull(bundle.affixes.firstOrNull { affix -> affix.id == affixId }) {
                    "Special template '${template.id}' references unknown affix '$affixId'."
                }
            }
        val item =
            buildItemInstance(
                base = base,
                material = material,
                affixes = affixes,
                quality = RarityTier.RARE,
                name = base.name,
                specialTemplateId = template.id,
            )
        return GeneratedItemRoll(
            item = item,
            rollResult = lootRoll,
            trace =
                traceFor(
                    item = item,
                    lootRoll = lootRoll,
                    previousPityTracker = previousPityTracker,
                    specialTier = template.specialTier,
                    specialTemplateId = template.id,
                    affixCostBreakdown = affixes.map(AffixDef::toAffixCost),
                    affixBudgetTarget = 0,
                    affixBudgetConsumed = 0,
                    affixBudgetDeviation = 0,
                    rawAffixBudgetShortfall = 0,
                ),
        )
    }

    private fun traceFor(
        item: ItemInstance,
        lootRoll: LootRollResult,
        previousPityTracker: PityTracker,
        specialTier: SpecialTier? = lootRoll.upgradedSpecialTier,
        specialTemplateId: String? = null,
        affixCostBreakdown: List<AffixCost> = emptyList(),
        affixBudgetTarget: Int = 0,
        affixBudgetConsumed: Int = 0,
        affixBudgetDeviation: Int = 0,
        rawAffixBudgetShortfall: Int = 0,
    ): ItemGenerationTrace =
        ItemGenerationTrace(
            generatedBaseItemId = item.baseId,
            specialTier = specialTier,
            specialTemplateId = specialTemplateId,
            affixCostBreakdown = affixCostBreakdown,
            affixBudgetTarget = affixBudgetTarget,
            affixBudgetConsumed = affixBudgetConsumed,
            affixBudgetDeviation = affixBudgetDeviation,
            rawAffixBudgetShortfall = rawAffixBudgetShortfall,
            previousPityTracker = previousPityTracker,
            resultingPityTracker = lootRoll.resultingPityTracker,
            rawCastSpeedRating = item.stats.castSpeedRating,
            effectiveCastSpeed = DiminishingReturns.effectiveCastSpeed(item.stats.castSpeedRating),
        )

    private fun buildItemInstance(
        base: ItemBaseDef,
        material: MaterialDef?,
        affixes: List<AffixDef>,
        quality: RarityTier,
        name: String,
        specialTemplateId: String? = null,
    ): ItemInstance {
        val stats =
            listOf(base.baseStats, material?.statModifiers ?: StatModifier.ZERO)
                .plus(affixes.map(AffixDef::statModifiers))
                .fold(StatModifier.ZERO) { acc, modifier -> acc + modifier }
        return ItemInstance(
            baseId = base.id,
            name = name,
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
            specialTemplateId = specialTemplateId,
        )
    }

    private fun resolveSpecialTierEligibility(
        context: LootRollContext,
        override: SpecialTierEligibility?,
    ): SpecialTierEligibility {
        val requested = override ?: LootBudgetResolver.defaultSpecialTierEligibility(context.sourceTier)
        val runtimeTemplateIds =
            bundle.eligibleSpecialTemplateIds(
                zoneId = context.zoneId,
                sourceTier = context.sourceTier,
                allowedSpecialTiers = requested.availableSpecialTiers,
            )
        val templateIds =
            if (requested.availableTemplateIds.isEmpty()) {
                runtimeTemplateIds
            } else {
                runtimeTemplateIds.intersect(requested.availableTemplateIds)
            }
        val availableSpecialTiers =
            requested.availableSpecialTiers.filterTo(linkedSetOf()) { tier ->
                templateIds.any { templateId -> bundle.specialTemplate(templateId)?.specialTier == tier }
            }
        return SpecialTierEligibility(
            availableSpecialTiers = availableSpecialTiers,
            availableTemplateIds = templateIds,
        )
    }

    private fun chooseBaseItem(itemLevel: Int): ItemBaseDef {
        val floorBand = legacyFloorBandForItemLevel(itemLevel)
        val candidates =
            bundle.baseItems.filter { item ->
                item.type != ItemType.CONSUMABLE &&
                    bundle.specialTemplateForItemId(item.id) == null &&
                    floorBand in item.dropFloors
            }
        require(candidates.isNotEmpty()) { "No normal base items are configured for floor band $floorBand (itemLevel=$itemLevel)." }
        return chooseWeighted(candidates) { it.dropWeight }
    }

    private fun chooseMaterial(
        base: ItemBaseDef,
        itemLevel: Int,
        fixedMaterialId: String? = null,
    ): MaterialDef? {
        if (fixedMaterialId != null) {
            val material =
                requireNotNull(bundle.materials.firstOrNull { candidate -> candidate.id == fixedMaterialId }) {
                    "Missing fixed material '$fixedMaterialId' for item '${base.id}'."
                }
            require(base.allowedMaterials.isEmpty() || fixedMaterialId in base.allowedMaterials) {
                "Item '${base.id}' does not allow fixed material '$fixedMaterialId'."
            }
            val floorBand = legacyFloorBandForItemLevel(itemLevel)
            require(floorBand >= material.minFloor) {
                "Fixed material '$fixedMaterialId' is not available for floor band $floorBand."
            }
            return material
        }
        if (base.allowedMaterials.isEmpty()) {
            return null
        }

        val floorBand = legacyFloorBandForItemLevel(itemLevel)
        val candidates = bundle.materials.filter { it.id in base.allowedMaterials && floorBand >= it.minFloor }
        require(candidates.isNotEmpty()) { "No materials are configured for ${base.id} on floor band $floorBand (itemLevel=$itemLevel)." }
        return candidates[random.nextInt(0, candidates.size)]
    }

    private fun selectSpecialTemplate(
        lootRoll: LootRollResult,
        specialTier: SpecialTier,
        context: AffixSelectionContext,
    ): SpecialItemTemplate {
        val candidates =
            lootRoll.budget.specialTierEligibility.availableTemplateIds
                .asSequence()
                .map { templateId ->
                    requireNotNull(bundle.specialTemplate(templateId)) {
                        "Loot roll references unknown special template '$templateId'."
                    }
                }
                .filter { template -> template.specialTier == specialTier }
                .toList()
        require(candidates.isNotEmpty()) {
            "Loot roll upgraded to $specialTier but no matching template is available."
        }
        return chooseWeighted(candidates) { template -> specialTemplateWeight(template, context) }
    }

    private fun specialTemplateWeight(
        template: SpecialItemTemplate,
        context: AffixSelectionContext,
    ): Int {
        val buildMatches = template.tags.count(context.buildTags::contains)
        val routeMatches = template.tags.count(context.routeBiasTags::contains)
        val sourceMatches = template.tags.count(templateRewardSourceBiasTags(context.rewardSource)::contains)
        return (1 + buildMatches * 4 + routeMatches * 2 + sourceMatches).coerceAtLeast(1)
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

    private fun affixFloorBandForQualityLevel(qualityLevel: Int): Int =
        ((qualityLevel + 2) / 2).coerceIn(1, 5)

    private fun templateRewardSourceBiasTags(source: MilestoneRewardSource?): Set<String> =
        when (source) {
            MilestoneRewardSource.ROUTE -> setOf("reward", "route")
            MilestoneRewardSource.BOSS -> setOf("boss", "elite", "reward")
            MilestoneRewardSource.CACHE -> setOf("cache", "reward")
            MilestoneRewardSource.SUPPORT -> setOf("support", "cache", "reward")
            null -> emptySet()
        }
}
