package com.ktome.game.loot

import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemType
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.loot.SourceTier

enum class MilestoneRewardSlotFamily {
    WEAPON,
    OFF_HAND,
    ARMOR,
    ACCESSORY,
    CONSUMABLE_OR_UTILITY,
}

data class MilestoneRewardCandidate(
    val baseItemId: String,
    val legal: Boolean,
    val rejectionReason: MilestoneRewardRejectionReason?,
    val scoreBreakdown: MilestoneRewardScoreBreakdown,
    val specialLinkedBase: Boolean,
    val exactProfessionCapstone: Boolean,
    val nonWeaponCapstone: Boolean,
    val slotFamily: MilestoneRewardSlotFamily?,
    val rarityRank: Int,
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
    val professionId: String? = null,
    val playerLevel: Int = 1,
    val routeBiasTags: Set<String> = emptySet(),
    val reservedSlots: Set<EquipSlot> = emptySet(),
    val occupiedSlots: Set<EquipSlot> = emptySet(),
    val recentSlotFamilies: List<MilestoneRewardSlotFamily> = emptyList(),
    val terminalIdentitySatisfied: Boolean = false,
)

data class MilestoneRewardSelectionRequest(
    val candidateBaseIds: List<String>,
    val selectorContext: MilestoneRewardSelectorContext,
    val poolWeightByBaseId: Map<String, Int> = emptyMap(),
    val selectionContext: LootBaseSelectionContext = LootBaseSelectionContext.EMPTY,
    val currentOwnedBaseIds: Set<String> = emptySet(),
    val replacementSlotPriority: List<EquipSlot> = emptyList(),
    val forbiddenBaseIds: Set<String> = emptySet(),
    val forcedReplacementSlot: EquipSlot? = null,
)

data class MilestoneRewardSelectionResult(
    val selectedBaseId: String?,
    val replacementSlot: EquipSlot?,
    val rankedCandidates: List<MilestoneRewardCandidate>,
)

data class MilestoneRewardScoreSample(
    val rewardSource: MilestoneRewardSource,
    val sourceId: String,
    val professionId: String,
    val zoneId: String,
    val baseItemId: String,
    val selected: Boolean,
    val legal: Boolean,
    val rejectionReason: String?,
    val slotFamily: MilestoneRewardSlotFamily?,
    val scoreBreakdown: MilestoneRewardScoreBreakdown,
    val scenarioName: String = "",
)

fun milestoneRewardSlotFamily(base: ItemBaseDef): MilestoneRewardSlotFamily? =
    when (base.type) {
        ItemType.WEAPON -> MilestoneRewardSlotFamily.WEAPON
        ItemType.CONSUMABLE -> MilestoneRewardSlotFamily.CONSUMABLE_OR_UTILITY
        ItemType.ARMOR ->
            when (base.slot) {
                EquipSlot.WEAPON -> MilestoneRewardSlotFamily.WEAPON
                EquipSlot.ARMOR -> MilestoneRewardSlotFamily.ARMOR
                EquipSlot.OFF_HAND ->
                    when {
                        "non_weapon_capstone" in base.tags -> MilestoneRewardSlotFamily.OFF_HAND
                        "seal" in base.tags -> MilestoneRewardSlotFamily.CONSUMABLE_OR_UTILITY
                        base.tags.any { tag -> tag in OFF_HAND_SLOT_TAGS } -> MilestoneRewardSlotFamily.OFF_HAND
                        base.tags.any { tag -> tag in UTILITY_SLOT_TAGS } -> MilestoneRewardSlotFamily.CONSUMABLE_OR_UTILITY
                        "accessory" in base.tags -> MilestoneRewardSlotFamily.ACCESSORY
                        else -> MilestoneRewardSlotFamily.OFF_HAND
                    }

                null -> MilestoneRewardSlotFamily.CONSUMABLE_OR_UTILITY
            }
    }

private val UTILITY_SLOT_TAGS: Set<String> =
    setOf("utility", "support", "sustain", "cleansing", "cleanse", "rescue")

private val OFF_HAND_SLOT_TAGS: Set<String> = setOf("shield", "guard", "precision", "spell", "sustain")
