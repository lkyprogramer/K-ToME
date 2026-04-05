package com.ktome.core.item

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

class AffixTagWeighting {
    fun weight(
        affix: AffixDef,
        context: AffixSelectionContext,
    ): Int {
        val itemMatches = affix.tags.count(context.itemTags::contains)
        val buildMatches = affix.tags.count(context.buildTags::contains)
        val routeMatches = affix.tags.count(context.routeBiasTags::contains)
        val sourceMatches = affix.tags.count(rewardSourceBiasTags(context.rewardSource)::contains)
        return (1 + itemMatches + buildMatches * 4 + routeMatches * 2 + sourceMatches).coerceAtLeast(1)
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
        val selectedFamilies = selected.mapTo(linkedSetOf(), AffixDef::familyId)
        if (affix.familyId() in selectedFamilies || affix.familyId() in context.blacklistFamilies) {
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
        count: Int,
        equipType: AffixEquipType,
        context: AffixSelectionContext = AffixSelectionContext(),
    ): Boolean {
        if (count <= 0) {
            return true
        }
        return canFillSlots(
            slotOrder = slotOrderFor(count),
            slotIndex = 0,
            floor = floor,
            equipType = equipType,
            context = context,
            selected = emptyList(),
            selectedIds = emptySet(),
        )
    }

    fun generate(
        floor: Int,
        count: Int,
        equipType: AffixEquipType,
        context: AffixSelectionContext = AffixSelectionContext(),
    ): List<AffixDef> {
        if (count <= 0) {
            return emptyList()
        }
        val slotOrder = slotOrderFor(count)
        return selectAffixes(
            slotOrder = slotOrder,
            slotIndex = 0,
            floor = floor,
            equipType = equipType,
            context = context,
            selected = emptyList(),
            selectedIds = emptySet(),
        ).orEmpty()
    }

    private fun canFillSlots(
        slotOrder: List<AffixType>,
        slotIndex: Int,
        floor: Int,
        equipType: AffixEquipType,
        context: AffixSelectionContext,
        selected: List<AffixDef>,
        selectedIds: Set<String>,
    ): Boolean {
        if (slotIndex >= slotOrder.size) {
            return true
        }
        val eligible =
            pool.candidates(floor = floor, equipType = equipType, slotType = slotOrder[slotIndex])
                .filter { candidate -> isEligibleCandidate(candidate, selectedIds, selected, context) }
        if (eligible.isEmpty()) {
            return false
        }
        return eligible.any { candidate ->
            canFillSlots(
                slotOrder = slotOrder,
                slotIndex = slotIndex + 1,
                floor = floor,
                equipType = equipType,
                context = context,
                selected = selected + candidate,
                selectedIds = selectedIds + candidate.id,
            )
        }
    }

    private fun chooseSingle(
        candidates: List<AffixDef>,
        selected: List<AffixDef>,
        selectedIds: Set<String>,
        context: AffixSelectionContext,
    ): AffixDef? {
        val eligible =
            candidates.filter { candidate -> isEligibleCandidate(candidate, selectedIds, selected, context) }
        if (eligible.isEmpty()) {
            return null
        }
        val weights = eligible.map { affix -> weighting.weight(affix, context) }
        var roll = random.nextInt(0, weights.sum())
        eligible.forEachIndexed { index, affix ->
            roll -= weights[index]
            if (roll < 0) {
                return affix
            }
        }
        return eligible.last()
    }

    private fun selectAffixes(
        slotOrder: List<AffixType>,
        slotIndex: Int,
        floor: Int,
        equipType: AffixEquipType,
        context: AffixSelectionContext,
        selected: List<AffixDef>,
        selectedIds: Set<String>,
    ): List<AffixDef>? {
        if (slotIndex >= slotOrder.size) {
            return selected
        }
        val candidates = pool.candidates(floor = floor, equipType = equipType, slotType = slotOrder[slotIndex])
        val eligible =
            candidates.filter { candidate -> isEligibleCandidate(candidate, selectedIds, selected, context) }
        val viable =
            eligible.filter { candidate ->
                canFillSlots(
                    slotOrder = slotOrder,
                    slotIndex = slotIndex + 1,
                    floor = floor,
                    equipType = equipType,
                    context = context,
                    selected = selected + candidate,
                    selectedIds = selectedIds + candidate.id,
                )
            }
        if (viable.isEmpty()) {
            return null
        }
        val next =
            chooseSingle(
                candidates = viable,
                selected = selected,
                selectedIds = selectedIds,
                context = context,
            ) ?: return null
        return selectAffixes(
            slotOrder = slotOrder,
            slotIndex = slotIndex + 1,
            floor = floor,
            equipType = equipType,
            context = context,
            selected = selected + next,
            selectedIds = selectedIds + next.id,
        )
    }

    private fun slotOrderFor(count: Int): List<AffixType> =
        buildList {
            if (count >= 1) add(AffixType.PREFIX)
            if (count >= 2) add(AffixType.SUFFIX)
            if (count >= 3) add(AffixType.PREFIX)
            if (count >= 4) add(AffixType.SUFFIX)
        }

    private fun isEligibleCandidate(
        candidate: AffixDef,
        selectedIds: Set<String>,
        selected: List<AffixDef>,
        context: AffixSelectionContext,
    ): Boolean = candidate.id !in selectedIds && !blacklist.rejects(candidate, selected, context)
}

private fun rewardSourceBiasTags(source: MilestoneRewardSource?): Set<String> =
    when (source) {
        MilestoneRewardSource.ROUTE -> setOf("reward", "route")
        MilestoneRewardSource.BOSS -> setOf("boss", "elite", "reward")
        MilestoneRewardSource.CACHE -> setOf("cache", "reward")
        MilestoneRewardSource.SUPPORT -> setOf("support", "cache", "reward")
        null -> emptySet()
    }

private fun AffixDef.familyId(): String =
    tags.firstOrNull(KNOWN_AFFIX_FAMILY_TAGS::contains) ?: id

private val KNOWN_AFFIX_FAMILY_TAGS: Set<String> =
    linkedSetOf(
        "physical",
        "fire",
        "cold",
        "lightning",
        "holy",
        "shadow",
        "offense",
        "defense",
        "protection",
        "mobility",
        "control",
        "resistance",
        "rescue",
        "cleansing",
        "life",
        "sustain",
        "accuracy",
        "strength",
        "spell",
        "willpower",
        "armor_break",
        "precision",
        "brutal",
    )
