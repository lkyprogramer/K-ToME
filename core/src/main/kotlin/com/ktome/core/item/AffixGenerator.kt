package com.ktome.core.item

import com.ktome.core.random.RandomSource

data class AffixSelectionContext(
    val itemTags: Set<String> = emptySet(),
    val buildTags: Set<String> = emptySet(),
)

class AffixTagWeighting {
    fun weight(
        affix: AffixDef,
        context: AffixSelectionContext,
    ): Int {
        val relevantTags = context.itemTags + context.buildTags
        val matchingTags = affix.tags.count(relevantTags::contains)
        return (1 + matchingTags * 3).coerceAtLeast(1)
    }
}

class AffixBlacklist {
    fun rejects(
        affix: AffixDef,
        selected: List<AffixDef>,
        context: AffixSelectionContext,
    ): Boolean {
        val selectedTags = selected.flatMapTo(linkedSetOf()) { chosen -> chosen.tags }
        if (affix.blacklistTags.any(selectedTags::contains)) {
            return true
        }
        val relevantTags = context.itemTags + context.buildTags
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
    fun generate(
        floor: Int,
        count: Int,
        equipType: AffixEquipType,
        context: AffixSelectionContext = AffixSelectionContext(),
    ): List<AffixDef> {
        if (count <= 0) {
            return emptyList()
        }
        val selected = mutableListOf<AffixDef>()
        val slotOrder =
            buildList {
                if (count >= 1) add(AffixType.PREFIX)
                if (count >= 2) add(AffixType.SUFFIX)
                if (count >= 3) add(AffixType.PREFIX)
                if (count >= 4) add(AffixType.SUFFIX)
            }
        slotOrder.forEach { slotType ->
            chooseSingle(
                candidates = pool.candidates(floor = floor, equipType = equipType, slotType = slotType),
                selected = selected,
                context = context,
            )?.let(selected::add)
        }
        return selected
    }

    private fun chooseSingle(
        candidates: List<AffixDef>,
        selected: List<AffixDef>,
        context: AffixSelectionContext,
    ): AffixDef? {
        val eligible =
            candidates.filterNot { candidate ->
                candidate.id in selected.map(AffixDef::id) || blacklist.rejects(candidate, selected, context)
            }
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
}
