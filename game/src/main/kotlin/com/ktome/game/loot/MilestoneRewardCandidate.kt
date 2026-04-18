package com.ktome.game.loot

import com.ktome.core.item.EquipSlot
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.loot.SourceTier

data class MilestoneRewardCandidate(
    val baseItemId: String,
    val legal: Boolean,
    val rejectionReason: MilestoneRewardRejectionReason?,
    val scoreBreakdown: MilestoneRewardScoreBreakdown,
    val specialLinkedBase: Boolean,
    val exactProfessionCapstone: Boolean,
    val nonWeaponCapstone: Boolean,
) {
    init {
        require(legal == (rejectionReason == null)) {
            "MilestoneRewardCandidate legality and rejectionReason must agree for '$baseItemId'."
        }
    }

    val score: Int
        get() = scoreBreakdown.totalScore
}

data class MilestoneRewardSelectorContext(
    val rewardSource: MilestoneRewardSource,
    val zoneId: String,
    val sourceTier: SourceTier,
    val effectiveFloorBand: Int,
    val routeBiasTags: Set<String> = emptySet(),
    val reservedSlots: Set<EquipSlot> = emptySet(),
    val occupiedSlots: Set<EquipSlot> = emptySet(),
)

data class MilestoneRewardSelectionRequest(
    val candidateBaseIds: List<String>,
    val selectorContext: MilestoneRewardSelectorContext,
    val poolWeightByBaseId: Map<String, Int> = emptyMap(),
    val selectionContext: LootBaseSelectionContext = LootBaseSelectionContext.EMPTY,
    val currentOwnedBaseIds: Set<String> = emptySet(),
    val preferredRewardSources: Set<MilestoneRewardSource> = emptySet(),
    val rewardPreferenceOrder: List<String> = emptyList(),
    val replacementSlotPriority: List<EquipSlot> = emptyList(),
    val forbiddenBaseIds: Set<String> = emptySet(),
    val forcedReplacementSlot: EquipSlot? = null,
)

data class MilestoneRewardSelectionResult(
    val selectedBaseId: String?,
    val replacementSlot: EquipSlot?,
    val rankedCandidates: List<MilestoneRewardCandidate>,
)
