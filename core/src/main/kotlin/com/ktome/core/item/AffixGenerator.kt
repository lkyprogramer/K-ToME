package com.ktome.core.item

import com.ktome.core.loot.AffixCost
import com.ktome.core.loot.AffixCostBand
import com.ktome.core.loot.RarityTier
import com.ktome.core.random.RandomSource

data class AffixSelectionContext(
    val itemTags: Set<String> = emptySet(),
    val buildTags: Set<String> = emptySet(),
    val routeBiasTags: Set<String> = emptySet(),
    val rewardSource: MilestoneRewardSource? = null,
    val qualityFloor: RarityTier? = null,
    val minAffixCount: Int = 0,
    val blacklistFamilies: Set<String> = emptySet(),
) {
    init {
        require(minAffixCount >= 0) { "AffixSelectionContext.minAffixCount must not be negative." }
    }
}

data class AffixSelectionResult(
    val affixes: List<AffixDef>,
    val costBreakdown: List<AffixCost>,
    val budgetConsumed: Int,
    val budgetTarget: Int,
    val rawBudgetShortfall: Int,
) {
    init {
        require(budgetTarget >= budgetConsumed) {
            "AffixSelectionResult.budgetTarget must be greater than or equal to budgetConsumed."
        }
    }

    val affixBudgetDeviation: Int
        get() = (budgetTarget - budgetConsumed).coerceAtLeast(0)
}

class AffixTagWeighting {
    fun weight(
        affix: AffixDef,
        context: AffixSelectionContext,
    ): Int {
        val itemMatches = affix.tags.count(context.itemTags::contains)
        val buildMatches = affix.tags.count(context.buildTags::contains)
        val routeMatches = affix.tags.count(context.routeBiasTags::contains)
        val sourceMatches = affix.tags.count(rewardSourceBiasTags(context.rewardSource)::contains)
        val costBias =
            when (affix.cost) {
                AffixCostBand.SIGNATURE.cost -> 6
                AffixCostBand.MAJOR.cost -> 5
                AffixCostBand.MEDIUM.cost -> 4
                AffixCostBand.MINOR.cost -> 2
                else -> 1
            }
        return (1 + itemMatches + buildMatches * 4 + routeMatches * 2 + sourceMatches + costBias).coerceAtLeast(1)
    }
}

class AffixBlacklist {
    fun rejects(
        affix: AffixDef,
        selected: List<AffixDef>,
        context: AffixSelectionContext,
    ): Boolean {
        if (affix.passive != null && selected.any { chosen -> chosen.passive != null }) {
            return true
        }
        val selectedFamilies = selected.mapTo(linkedSetOf(), AffixDef::affixFamily)
        if (affix.affixFamily in selectedFamilies || affix.affixFamily in context.blacklistFamilies) {
            return true
        }
        val selectedExclusiveGroups = selected.mapNotNullTo(linkedSetOf(), AffixDef::exclusiveGroup)
        if (affix.exclusiveGroup != null && affix.exclusiveGroup in selectedExclusiveGroups) {
            return true
        }
        val selectedTags = selected.flatMapTo(linkedSetOf()) { chosen -> chosen.tags }
        if (affix.blacklistTags.any(selectedTags::contains)) {
            return true
        }
        val relevantTags = context.itemTags + context.buildTags + context.routeBiasTags
        return affix.blacklistTags.any(relevantTags::contains)
    }
}

class AffixPool(
    private val affixes: List<AffixDef>,
) {
    fun candidates(
        floor: Int,
        equipType: AffixEquipType,
        slotType: AffixType,
    ): List<AffixDef> =
        affixes.filter { affix ->
            floor >= affix.minFloor &&
                affix.equipType == equipType &&
                affix.type == slotType
        }
}

class AffixGenerator(
    private val pool: AffixPool,
    private val random: RandomSource,
    private val weighting: AffixTagWeighting = AffixTagWeighting(),
    private val blacklist: AffixBlacklist = AffixBlacklist(),
) {
    fun canGenerate(
        floor: Int,
        budget: Int,
        rarityTier: RarityTier,
        equipType: AffixEquipType,
        context: AffixSelectionContext = AffixSelectionContext(),
    ): Boolean =
        findFeasibleSelection(
            floor = floor,
            budget = budget,
            rarityTier = rarityTier,
            equipType = equipType,
            context = context,
        ) != null

    fun generate(
        floor: Int,
        budget: Int,
        rarityTier: RarityTier,
        equipType: AffixEquipType,
        context: AffixSelectionContext = AffixSelectionContext(),
    ): AffixSelectionResult {
        if (budget <= 0 || rarityTier == RarityTier.NORMAL) {
            return AffixSelectionResult(
                affixes = emptyList(),
                costBreakdown = emptyList(),
                budgetConsumed = 0,
                budgetTarget = 0,
                rawBudgetShortfall = 0,
            )
        }

        val minimumCount = requiredAffixCount(rarityTier = rarityTier, context = context)
        val maximumCount = rarityTier.maximumAffixCount().coerceAtLeast(minimumCount)
        val effectiveBudgetTarget =
            effectiveBudgetTarget(
                floor = floor,
                budget = budget,
                rarityTier = rarityTier,
                equipType = equipType,
                context = context,
            )
        val bestSelections =
            findBestSelections(
                floor = floor,
                budget = budget,
                rarityTier = rarityTier,
                equipType = equipType,
                context = context,
            )
        chooseSelection(bestSelections)?.let { selection ->
            val costBreakdown = selection.affixes.map(AffixDef::toAffixCost)
            val budgetConsumed = costBreakdown.sumOf(AffixCost::cost)
            return AffixSelectionResult(
                affixes = selection.affixes,
                costBreakdown = costBreakdown,
                budgetConsumed = budgetConsumed,
                budgetTarget = effectiveBudgetTarget,
                rawBudgetShortfall = (budget - budgetConsumed).coerceAtLeast(0),
            )
        }

        return AffixSelectionResult(
            affixes = emptyList(),
            costBreakdown = emptyList(),
            budgetConsumed = 0,
            budgetTarget = effectiveBudgetTarget,
            rawBudgetShortfall = budget,
        )
    }

    private fun requiredAffixCount(
        rarityTier: RarityTier,
        context: AffixSelectionContext,
    ): Int = maxOf(rarityTier.minimumAffixCount(), context.minAffixCount)

    private fun effectiveBudgetTarget(
        floor: Int,
        budget: Int,
        rarityTier: RarityTier,
        equipType: AffixEquipType,
        context: AffixSelectionContext,
    ): Int {
        // PR-05 reports two related numbers:
        // - budgetTarget/affixBudgetDeviation: the best budget the affix stage can realistically satisfy
        // - rawBudgetShortfall: the gap against the full loot budget request
        // This keeps high-qLvl MAGIC rolls from being treated as impossible failures while preserving
        // visibility into the upstream budget that the rarity pipeline requested.
        val slotOrder = slotOrderFor(rarityTier.maximumAffixCount().coerceAtLeast(requiredAffixCount(rarityTier = rarityTier, context = context)))
        val availableCostsByType =
            slotOrder
                .distinct()
                .associateWith { slotType ->
                    pool.candidates(floor = floor, equipType = equipType, slotType = slotType)
                        .filter { candidate -> isBudgetTargetCandidate(candidate = candidate, context = context) }
                        .map(AffixDef::cost)
                        .sortedDescending()
                        .toMutableList()
                }
                .toMutableMap()
        var total = 0
        var trivialCount = 0
        slotOrder.forEach { slotType ->
            val candidates = availableCostsByType.getValue(slotType)
            val nextIndex =
                candidates.indexOfFirst { cost ->
                    cost != AffixCostBand.TRIVIAL.cost || trivialCount < 1
                }
            if (nextIndex < 0) {
                return@forEach
            }
            val cost = candidates.removeAt(nextIndex)
            if (cost == AffixCostBand.TRIVIAL.cost) {
                trivialCount += 1
            }
            total += cost
        }
        return minOf(budget, total)
    }

    private fun isBudgetTargetCandidate(
        candidate: AffixDef,
        context: AffixSelectionContext,
    ): Boolean {
        if (candidate.affixFamily in context.blacklistFamilies) {
            return false
        }
        if (candidate.cost == AffixCostBand.TRIVIAL.cost && candidate.statModifiers.castSpeedRating > 0) {
            return false
        }
        return true
    }

    private fun findFeasibleSelection(
        floor: Int,
        budget: Int,
        rarityTier: RarityTier,
        equipType: AffixEquipType,
        context: AffixSelectionContext,
    ): List<AffixDef>? {
        if (budget <= 0 || rarityTier == RarityTier.NORMAL) {
            return if (requiredAffixCount(rarityTier = rarityTier, context = context) == 0) emptyList() else null
        }
        val minimumCount = requiredAffixCount(rarityTier = rarityTier, context = context)
        val maximumCount = rarityTier.maximumAffixCount().coerceAtLeast(minimumCount)
        for (count in minimumCount..maximumCount) {
            val selection =
                findFeasibleSelection(
                    slotOrder = slotOrderFor(count),
                    slotIndex = 0,
                    floor = floor,
                    budget = budget,
                    equipType = equipType,
                    context = context,
                    selected = emptyList(),
                    selectedIds = emptySet(),
                    trivialCount = 0,
                )
            if (selection != null) {
                return selection
            }
        }
        return null
    }

    private fun findFeasibleSelection(
        slotOrder: List<AffixType>,
        slotIndex: Int,
        floor: Int,
        budget: Int,
        equipType: AffixEquipType,
        context: AffixSelectionContext,
        selected: List<AffixDef>,
        selectedIds: Set<String>,
        trivialCount: Int,
    ): List<AffixDef>? {
        if (slotIndex >= slotOrder.size) {
            return selected
        }
        val remainingBudget = budget - selected.sumOf(AffixDef::cost)
        if (remainingBudget <= 0) {
            return null
        }
        val eligible =
            eligibleCandidates(
                slotOrder = slotOrder,
                slotIndex = slotIndex,
                floor = floor,
                equipType = equipType,
                context = context,
                selected = selected,
                selectedIds = selectedIds,
                remainingBudget = remainingBudget,
                trivialCount = trivialCount,
            )
        eligible.forEach { candidate ->
            val resolved =
                findFeasibleSelection(
                    slotOrder = slotOrder,
                    slotIndex = slotIndex + 1,
                    floor = floor,
                    budget = budget,
                    equipType = equipType,
                    context = context,
                    selected = selected + candidate,
                    selectedIds = selectedIds + candidate.id,
                    trivialCount = trivialCount + trivialIncrement(candidate),
                )
            if (resolved != null) {
                return resolved
            }
        }
        return null
    }

    private fun findBestSelections(
        floor: Int,
        budget: Int,
        rarityTier: RarityTier,
        equipType: AffixEquipType,
        context: AffixSelectionContext,
    ): List<SelectionCandidate> {
        val minimumCount = requiredAffixCount(rarityTier = rarityTier, context = context)
        val maximumCount = rarityTier.maximumAffixCount().coerceAtLeast(minimumCount)
        val bestSelections = mutableListOf<SelectionCandidate>()
        for (count in minimumCount..maximumCount) {
            val slotOrder = slotOrderFor(count)
            val selection =
                selectBestAffixes(
                    slotOrder = slotOrder,
                    slotIndex = 0,
                    floor = floor,
                    budget = budget,
                    equipType = equipType,
                    context = context,
                    selected = emptyList(),
                    selectedIds = emptySet(),
                    trivialCount = 0,
                )
            selection?.let { candidate ->
                collectBestSelection(bestSelections, candidate)
            }
        }
        return bestSelections
    }

    private fun selectBestAffixes(
        slotOrder: List<AffixType>,
        slotIndex: Int,
        floor: Int,
        budget: Int,
        equipType: AffixEquipType,
        context: AffixSelectionContext,
        selected: List<AffixDef>,
        selectedIds: Set<String>,
        trivialCount: Int,
    ): SelectionCandidate? {
        if (slotIndex >= slotOrder.size) {
            val totalWeight = selected.sumOf { affix -> weighting.weight(affix, context) }
            val totalCost = selected.sumOf(AffixDef::cost)
            return SelectionCandidate(
                affixes = selected,
                totalWeight = totalWeight,
                trivialCount = trivialCount,
                totalCost = totalCost,
                deviation = (budget - totalCost).coerceAtLeast(0),
            )
        }
        val remainingBudget = budget - selected.sumOf(AffixDef::cost)
        if (remainingBudget <= 0) {
            return null
        }

        val eligible =
            eligibleCandidates(
                slotOrder = slotOrder,
                slotIndex = slotIndex,
                floor = floor,
                equipType = equipType,
                context = context,
                selected = selected,
                selectedIds = selectedIds,
                remainingBudget = remainingBudget,
                trivialCount = trivialCount,
            )
        if (eligible.isEmpty()) {
            return null
        }

        val bestSelections = mutableListOf<SelectionCandidate>()
        eligible.forEach { candidate ->
            val selection =
                selectBestAffixes(
                    slotOrder = slotOrder,
                    slotIndex = slotIndex + 1,
                    floor = floor,
                budget = budget,
                    equipType = equipType,
                    context = context,
                    selected = selected + candidate,
                    selectedIds = selectedIds + candidate.id,
                    trivialCount = trivialCount + trivialIncrement(candidate),
                )
            selection?.let { resolved ->
                collectBestSelection(bestSelections, resolved)
            }
        }
        return chooseSelection(bestSelections)
    }

    private fun eligibleCandidates(
        slotOrder: List<AffixType>,
        slotIndex: Int,
        floor: Int,
        equipType: AffixEquipType,
        context: AffixSelectionContext,
        selected: List<AffixDef>,
        selectedIds: Set<String>,
        remainingBudget: Int,
        trivialCount: Int,
    ): List<AffixDef> {
        val rawCandidates = pool.candidates(floor = floor, equipType = equipType, slotType = slotOrder[slotIndex])
        val hasHighValuePrimary =
            slotIndex == 0 &&
                rawCandidates.any { raw ->
                    raw.cost >= AffixCostBand.MEDIUM.cost &&
                        isEligibleCandidate(
                            candidate = raw,
                            selectedIds = selectedIds,
                            selected = selected,
                            context = context,
                            remainingBudget = remainingBudget,
                            trivialCount = trivialCount,
                            isPrimarySlot = false,
                            hasHighValuePrimary = false,
                        )
                }
        return rawCandidates.filter { candidate ->
            isEligibleCandidate(
                candidate = candidate,
                selectedIds = selectedIds,
                selected = selected,
                context = context,
                remainingBudget = remainingBudget,
                trivialCount = trivialCount,
                isPrimarySlot = slotIndex == 0,
                hasHighValuePrimary = hasHighValuePrimary,
            )
        }
    }

    private fun isEligibleCandidate(
        candidate: AffixDef,
        selectedIds: Set<String>,
        selected: List<AffixDef>,
        context: AffixSelectionContext,
        remainingBudget: Int,
        trivialCount: Int,
        isPrimarySlot: Boolean,
        hasHighValuePrimary: Boolean,
    ): Boolean {
        if (candidate.id in selectedIds) {
            return false
        }
        if (candidate.cost > remainingBudget) {
            return false
        }
        if (candidate.cost == AffixCostBand.TRIVIAL.cost && trivialCount >= 1) {
            return false
        }
        if (candidate.cost == AffixCostBand.TRIVIAL.cost && candidate.statModifiers.castSpeedRating > 0) {
            return false
        }
        if (isPrimarySlot && hasHighValuePrimary && candidate.cost < AffixCostBand.MEDIUM.cost) {
            return false
        }
        return !blacklist.rejects(candidate, selected, context)
    }

    private fun slotOrderFor(count: Int): List<AffixType> =
        buildList {
            if (count >= 1) add(AffixType.PREFIX)
            if (count >= 2) add(AffixType.SUFFIX)
            if (count >= 3) add(AffixType.PREFIX)
            if (count >= 4) add(AffixType.SUFFIX)
        }

    private fun trivialIncrement(candidate: AffixDef): Int =
        if (candidate.cost == AffixCostBand.TRIVIAL.cost) {
            1
        } else {
            0
        }

    private fun collectBestSelection(
        selections: MutableList<SelectionCandidate>,
        candidate: SelectionCandidate,
    ) {
        if (selections.isEmpty()) {
            selections += candidate
            return
        }
        val comparison = compareSelections(candidate, selections.first())
        when {
            comparison < 0 -> {
                selections.clear()
                selections += candidate
            }
            comparison == 0 -> selections += candidate
        }
    }

    private fun chooseSelection(candidates: List<SelectionCandidate>): SelectionCandidate? {
        if (candidates.isEmpty()) {
            return null
        }
        val totalWeight = candidates.sumOf { candidate -> candidate.totalWeight.coerceAtLeast(1) }
        var roll = random.nextInt(0, Int.MAX_VALUE) % totalWeight
        candidates.forEach { candidate ->
            roll -= candidate.totalWeight.coerceAtLeast(1)
            if (roll < 0) {
                return candidate
            }
        }
        return candidates.last()
    }

    private fun compareSelections(
        left: SelectionCandidate,
        right: SelectionCandidate,
    ): Int =
        when {
            left.deviation != right.deviation -> left.deviation.compareTo(right.deviation)
            left.totalCost != right.totalCost -> right.totalCost.compareTo(left.totalCost)
            left.trivialCount != right.trivialCount -> left.trivialCount.compareTo(right.trivialCount)
            left.highValueCount != right.highValueCount -> right.highValueCount.compareTo(left.highValueCount)
            left.totalWeight != right.totalWeight -> right.totalWeight.compareTo(left.totalWeight)
            left.affixes.size != right.affixes.size -> left.affixes.size.compareTo(right.affixes.size)
            else -> 0
        }

    private data class SelectionCandidate(
        val affixes: List<AffixDef>,
        val totalWeight: Int,
        val trivialCount: Int,
        val totalCost: Int,
        val deviation: Int,
    ) {
        val highValueCount: Int
            get() = affixes.count { affix -> affix.cost >= AffixCostBand.MEDIUM.cost }
    }
}

private fun rewardSourceBiasTags(source: MilestoneRewardSource?): Set<String> =
    when (source) {
        MilestoneRewardSource.ROUTE -> setOf("reward", "route")
        MilestoneRewardSource.BOSS -> setOf("boss", "elite", "reward")
        MilestoneRewardSource.CACHE -> setOf("cache", "reward")
        MilestoneRewardSource.SUPPORT -> setOf("support", "cache", "reward")
        null -> emptySet()
    }
