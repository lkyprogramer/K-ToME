package com.ktome.game.loot

import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.loot.SourceTier
import com.ktome.core.resource.ResourceType
import com.ktome.game.data.schema.ItemSchemaV2
import com.ktome.game.data.schema.ProfessionSchemaV2

val FOUNDATION_DETERMINISTIC_RESCUE_UTILITY_BASE_IDS: Set<String> =
    setOf(
        "healing_potion",
        "scroll_teleport",
        "mana_potion",
        "stamina_draught",
        "energy_tonic",
        "consecrated_oil",
    )

fun foundationMilestoneRewardSourceTier(source: MilestoneRewardSource): SourceTier =
    when (source) {
        MilestoneRewardSource.ROUTE,
        MilestoneRewardSource.CACHE,
        MilestoneRewardSource.SUPPORT,
        -> SourceTier.CHEST
        MilestoneRewardSource.BOSS -> SourceTier.BOSS
    }

fun isMilestoneRewardSuitableForProfession(
    base: ItemBaseDef,
    profession: ProfessionSchemaV2?,
    itemSchemaByBaseId: (String) -> ItemSchemaV2?,
): Boolean {
    if (profession == null) {
        return true
    }
    if (base.resourceTypeId != null && base.resourceTypeId != profession.resourceType) {
        return false
    }
    val itemSchema = itemSchemaByBaseId(base.id)
    if (profession.resourceType != ResourceType.MANA.name && itemSchema?.tags?.contains("arcane") == true) {
        return false
    }
    return true
}

fun foundationStandardLootWeightByBaseIdFromPools(
    baseItemById: (String) -> ItemBaseDef?,
    pools: List<LootProfileCandidatePool>,
    selectionContext: LootBaseSelectionContext,
): Map<String, Int> {
    return foundationLootWeightByBaseIdFromPools(
        baseItemById = baseItemById,
        pools = pools,
        selectionContext = selectionContext,
    ) { pool -> pool.standardCandidateBaseIds }
}

fun foundationMilestoneLootWeightByBaseIdFromPools(
    baseItemById: (String) -> ItemBaseDef?,
    pools: List<LootProfileCandidatePool>,
    selectionContext: LootBaseSelectionContext,
): Map<String, Int> {
    return foundationLootWeightByBaseIdFromPools(
        baseItemById = baseItemById,
        pools = pools,
        selectionContext = selectionContext,
    ) { pool -> pool.allCandidateBaseIds }
}

private fun foundationLootWeightByBaseIdFromPools(
    baseItemById: (String) -> ItemBaseDef?,
    pools: List<LootProfileCandidatePool>,
    selectionContext: LootBaseSelectionContext,
    candidateBaseIdsForPool: (LootProfileCandidatePool) -> Iterable<String>,
): Map<String, Int> {
    val weightsByBaseId = linkedMapOf<String, Int>()
    pools.forEach { pool ->
        val candidateBaseIds = candidateBaseIdsForPool(pool)
        candidateBaseIds.forEach { baseId ->
            val base = requireNotNull(baseItemById(baseId)) {
                "Loot profile '${pool.profileId}' resolved unknown candidate base '$baseId'."
            }
            val weightedDrop = pool.weightFor(base, selectionContext)
            weightsByBaseId[baseId] = weightsByBaseId.getOrDefault(baseId, 0) + weightedDrop
        }
    }
    return weightsByBaseId
}
