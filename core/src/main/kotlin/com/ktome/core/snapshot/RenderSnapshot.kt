package com.ktome.core.snapshot

import com.ktome.core.talent.TalentCategory
import com.ktome.core.talent.TalentTreeOwnerType
import kotlinx.serialization.Serializable

const val COMBAT_FEEDBACK_EVENT_LIMIT: Int = 12

@Serializable
data class RenderSnapshot(
    val metadata: RenderMetadataSnapshot,
    val mapCells: List<MapCellSnapshot>,
    val props: List<PropRenderSnapshot> = emptyList(),
    val actors: List<ActorRenderSnapshot> = emptyList(),
    val overlays: List<OverlayRenderSnapshot> = emptyList(),
    val uiState: RenderUiStateSnapshot,
    val logEvents: List<RenderLogEventSnapshot> = emptyList(),
    val combatFeedbackEvents: List<CombatFeedbackSnapshot> = emptyList(),
)

@Serializable
data class RenderMetadataSnapshot(
    val revision: Long,
    val zoneId: String,
    val zoneNameKey: String,
    val zoneDescKey: String? = null,
    val currentFloor: Int,
    val maxFloor: Int,
    val width: Int,
    val height: Int,
    val playerX: Int,
    val playerY: Int,
    val zoneVisualKey: String,
    val zoneAudioProfile: String,
    val tilesetKey: String,
    val ambientProfile: String,
)

@Serializable
enum class CellVisibilitySnapshot {
    VISIBLE,
    EXPLORED,
    HIDDEN,
}

@Serializable
data class MapCellSnapshot(
    val x: Int,
    val y: Int,
    val visibility: CellVisibilitySnapshot,
    val terrainTypeId: String,
    val terrainVisualKey: String,
    val terrainTags: List<String> = emptyList(),
    val terrainAudioProfile: String? = null,
    val terrainOverride: TerrainOverrideRenderSnapshot? = null,
    val stairDirectionId: String? = null,
    val actorEntityId: Int? = null,
    val items: List<ItemRenderSnapshot> = emptyList(),
)

@Serializable
data class TerrainOverrideRenderSnapshot(
    val sourceRuleId: String,
    val ruleNameKey: String,
    val remainingTurns: Int,
    val conductsLightning: Boolean,
    val tickDamageTypeId: String? = null,
    val tickDamage: Int = 0,
)

@Serializable
data class PropRenderSnapshot(
    val id: String,
    val x: Int,
    val y: Int,
    val propTypeId: String,
    val stairDirectionId: String? = null,
    val visualKey: String,
    val audioProfile: String? = null,
    val nameKey: String? = null,
    val descKey: String? = null,
    val stateLabelKey: String? = null,
)

@Serializable
enum class ActorRoleKindSnapshot {
    PLAYER,
    BOSS,
    MONSTER,
    GENERIC,
}

@Serializable
data class StatusEffectRenderSnapshot(
    val typeId: String,
    val remainingTurns: Int,
    val nameKey: String? = null,
    val iconKey: String? = null,
    val stackCount: Int = 1,
    val stackCap: Int? = null,
    val category: StatusEffectCategorySnapshot = StatusEffectCategorySnapshot.DEBUFF,
)

@Serializable
enum class StatusEffectCategorySnapshot {
    BUFF,
    DEBUFF,
    NEUTRAL,
}

@Serializable
data class ActorRenderSnapshot(
    val entityId: Int,
    val x: Int,
    val y: Int,
    val visualKey: String,
    val audioProfile: String? = null,
    val nameKey: String,
    val isPlayer: Boolean,
    val roleKind: ActorRoleKindSnapshot? = null,
    val aiTypeId: String? = null,
    val displayTintColorHex: String? = null,
    val mutations: List<ActorMutationRenderSnapshot> = emptyList(),
    val bossVariant: BossVariantRenderSnapshot? = null,
    val currentHp: Int? = null,
    val maxHp: Int? = null,
    val attack: Int? = null,
    val defense: Int? = null,
    val accuracy: Int? = null,
    val evasion: Int? = null,
    val speed: Int? = null,
    val strength: Int? = null,
    val dexterity: Int? = null,
    val constitution: Int? = null,
    val willpower: Int? = null,
    val statusEffects: List<StatusEffectRenderSnapshot> = emptyList(),
)

@Serializable
data class ActorMutationRenderSnapshot(
    val mutationId: String,
    val nameKey: String,
    val iconKey: String,
    val audioProfile: String? = null,
    val kindId: String,
    val tierId: String,
    val summary: RenderTextTokenSnapshot? = null,
)

@Serializable
data class BossVariantRenderSnapshot(
    val variantId: String,
    val nameKey: String,
    val visualTintKey: String? = null,
    val audioProfile: String? = null,
)

@Serializable
enum class OverlayShapeSnapshot {
    SINGLE_TILE,
    LINE,
    CONE,
    RING,
    CUSTOM,
}

@Serializable
data class OverlayRenderSnapshot(
    val id: String,
    val visualKey: String,
    val audioProfile: String? = null,
    val previewTurns: Int,
    val dangerLevel: Int,
    val shape: OverlayShapeSnapshot,
    val sourceAbilityId: String,
    val cells: List<GridPointSnapshot>,
    val warningMessage: RenderTextTokenSnapshot? = null,
)

@Serializable
enum class CombatFeedbackTypeSnapshot {
    DAMAGE,
    HEAL,
    MISS,
    STATUS_APPLIED,
    STATUS_REMOVED,
}

@Serializable
data class CombatFeedbackSnapshot(
    val targetEntityId: Int?,
    val sourceEntityId: Int? = null,
    val x: Int,
    val y: Int,
    val type: CombatFeedbackTypeSnapshot,
    val amount: Int? = null,
    val damageTypeId: String? = null,
    val statusNameKey: String? = null,
    val critical: Boolean = false,
)

@Serializable
data class RenderUiStateSnapshot(
    val playerStatus: PlayerStatusSnapshot,
    val equipment: List<EquipmentSlotSnapshot>,
    val talents: List<TalentSlotSnapshot>,
    val reserveTalents: List<TalentReserveSnapshot> = emptyList(),
    val talentTrees: List<TalentTreeSnapshot> = emptyList(),
    val inscriptions: List<InscriptionSlotSnapshot> = emptyList(),
    val inventory: List<InventoryEntrySnapshot>,
    val recentRewards: List<RewardPresentationEntrySnapshot> = emptyList(),
    val frontstageReadability: FrontstageReadabilitySnapshot = FrontstageReadabilitySnapshot(),
    val targetablePositions: List<GridPointSnapshot>,
    val searchPromptLabelKey: String? = null,
    val shardBalance: Int = 0,
    val activeShop: ShopPanelSnapshot? = null,
    val activeRouteSelection: RouteSelectionSnapshot? = null,
)

@Serializable
sealed interface DescriptionValueSnapshot {
    @Serializable
    data class IntValue(val value: Int) : DescriptionValueSnapshot

    @Serializable
    data class DecimalValue(val value: Double) : DescriptionValueSnapshot

    @Serializable
    data class BooleanValue(val value: Boolean) : DescriptionValueSnapshot

    @Serializable
    data class TextValue(val value: String) : DescriptionValueSnapshot

    @Serializable
    data class StatusValue(
        val statusId: String,
        val nameKey: String,
    ) : DescriptionValueSnapshot
}

@Serializable
data class DescriptionModelSnapshot(
    val templateKey: String,
    val placeholders: Map<String, DescriptionValueSnapshot> = emptyMap(),
    val keywords: List<String> = emptyList(),
)

@Serializable
data class TalentBreakpointPreviewSnapshot(
    val atRank: Int,
    val descriptionAddendumKey: String? = null,
    val model: DescriptionModelSnapshot,
)

@Serializable
data class PlayerStatusSnapshot(
    val currentHp: Int,
    val maxHp: Int,
    val currentResource: Int,
    val maxResource: Int,
    val resourceLabelKey: String,
    val resourceTypeId: String = "STAMINA",
    val resourceStableMin: Int? = null,
    val resourceStableMax: Int? = null,
    val secondaryResourceCurrent: Int? = null,
    val secondaryResourceMax: Int? = null,
    val secondaryResourceLabelKey: String? = null,
    val secondaryResourceTypeId: String? = null,
    val secondaryResourceStableMin: Int? = null,
    val secondaryResourceStableMax: Int? = null,
    val level: Int,
    val currentExperience: Int,
    val nextLevelRequirement: Int,
    val statPoints: Int,
    val talentPoints: Int,
    val raceTalentPoints: Int = 0,
    val attack: Int,
    val defense: Int,
    val accuracy: Int,
    val evasion: Int,
    val speed: Int,
    val castSpeedRating: Int = 0,
    val effectiveCastSpeed: Double = 0.0,
)

@Serializable
data class InscriptionSlotSnapshot(
    val hotkey: Int,
    val inscriptionId: String,
    val nameKey: String,
    val descKey: String,
    val iconKey: String,
    val categoryId: String,
    val cooldownRemaining: Int,
    val maxCooldown: Int,
    val requiresTarget: Boolean = false,
)

@Serializable
data class EquipmentSlotSnapshot(
    val slotId: String,
    val item: ItemRenderSnapshot? = null,
)

@Serializable
data class TalentSlotSnapshot(
    val slot: Int,
    val talentId: String,
    val ownerType: String = TalentTreeOwnerType.PROFESSION.name,
    val treeOwnerId: String = "",
    val nameKey: String,
    val visualKey: String? = null,
    val iconKey: String? = null,
    val damageTypeIconKey: String? = null,
    val audioProfile: String? = null,
    val level: Int,
    val maxLevel: Int,
    val resourceCost: Int,
    val resourceLabelKey: String,
    val resourceTypeId: String = "STAMINA",
    val range: Int,
    val minRange: Int,
    val currentCooldown: Int,
    val maxCooldown: Int,
    val requiresTarget: Boolean,
    val descKey: String? = null,
    val committedLevel: Int = level,
    val descriptionModel: DescriptionModelSnapshot? = null,
    val nextBreakpointPreview: TalentBreakpointPreviewSnapshot? = null,
    val isMaxRank: Boolean = false,
    val hasPendingAllocation: Boolean = false,
)

@Serializable
data class TalentReserveSnapshot(
    val talentId: String,
    val ownerType: String = TalentTreeOwnerType.PROFESSION.name,
    val treeOwnerId: String = "",
    val nameKey: String,
    val visualKey: String? = null,
    val iconKey: String? = null,
    val damageTypeIconKey: String? = null,
    val audioProfile: String? = null,
    val level: Int,
    val maxLevel: Int,
    val resourceCost: Int,
    val resourceLabelKey: String,
    val resourceTypeId: String = "STAMINA",
    val range: Int,
    val minRange: Int,
    val currentCooldown: Int,
    val maxCooldown: Int,
    val requiresTarget: Boolean,
    val descKey: String? = null,
    val committedLevel: Int = level,
    val descriptionModel: DescriptionModelSnapshot? = null,
    val nextBreakpointPreview: TalentBreakpointPreviewSnapshot? = null,
    val isMaxRank: Boolean = false,
    val hasPendingAllocation: Boolean = false,
)

@Serializable
enum class TalentNodeStateSnapshot {
    LOCKED,
    LEARNABLE,
    LEARNED_RESERVE,
    LEARNED_ACTIVE,
}

@Serializable
enum class TalentNodeLockReasonTypeSnapshot {
    LEVEL,
    PREREQUISITE_RANK,
    TREE_INVESTMENT,
    CROSS_TREE_INVESTMENT,
}

@Serializable
data class TalentNodeLockReasonSnapshot(
    val type: TalentNodeLockReasonTypeSnapshot,
    val messageKey: String,
    val talentId: String? = null,
    val talentNameKey: String? = null,
    val treeId: String? = null,
    val treeNameKey: String? = null,
    val requiredLevel: Int? = null,
    val currentLevel: Int? = null,
    val requiredRank: Int? = null,
    val currentRank: Int? = null,
    val requiredPoints: Int? = null,
    val currentPoints: Int? = null,
)

@Serializable
data class TalentTreeSnapshot(
    val treeId: String,
    val ownerType: String = TalentTreeOwnerType.PROFESSION.name,
    val treeOwnerId: String = "",
    val nameKey: String,
    val descKey: String,
    val visualKey: String? = null,
    val iconKey: String? = null,
    val audioProfile: String? = null,
    val nodes: List<TalentTreeNodeSnapshot> = emptyList(),
)

@Serializable
data class TalentTreeNodeSnapshot(
    val talentId: String,
    val treeId: String,
    val ownerType: String = TalentTreeOwnerType.PROFESSION.name,
    val treeOwnerId: String = "",
    val nameKey: String,
    val descKey: String? = null,
    val visualKey: String? = null,
    val iconKey: String? = null,
    val damageTypeIconKey: String? = null,
    val audioProfile: String? = null,
    val category: TalentCategory = TalentCategory.ACTIVE,
    val state: TalentNodeStateSnapshot,
    val rank: Int,
    val committedRank: Int = rank,
    val maxRank: Int,
    val unlockLevel: Int,
    val resourceCost: Int,
    val resourceLabelKey: String,
    val resourceTypeId: String = "STAMINA",
    val range: Int,
    val minRange: Int,
    val currentCooldown: Int,
    val maxCooldown: Int,
    val requiresTarget: Boolean,
    val descriptionModel: DescriptionModelSnapshot? = null,
    val nextBreakpointPreview: TalentBreakpointPreviewSnapshot? = null,
    val lockReasons: List<TalentNodeLockReasonSnapshot> = emptyList(),
    val isMaxRank: Boolean = false,
    val hasPendingAllocation: Boolean = false,
)

@Serializable
data class InventoryEntrySnapshot(
    val index: Int,
    val item: ItemRenderSnapshot,
    val equippedSlotId: String? = null,
)

@Serializable
enum class RewardPresentationSourceSnapshot {
    CADENCE,
    ROUTE,
    BOSS,
    CACHE,
    SUPPORT,
    HIDDEN_EVENT,
    SECRET_ZONE,
}

@Serializable
data class RewardPresentationEntrySnapshot(
    val source: RewardPresentationSourceSnapshot,
    val sourceLabelKey: String,
    val itemDisplayName: RenderTextTokenSnapshot,
    val detailText: RenderTextTokenSnapshot? = null,
)

@Serializable
enum class FrontstageActionCategorySnapshot {
    SEARCH,
    SECRET,
    PASSIVE,
}

@Serializable
enum class FrontstageActionPrioritySnapshot {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
}

@Serializable
data class FrontstageActionCueSnapshot(
    val category: FrontstageActionCategorySnapshot,
    val priority: FrontstageActionPrioritySnapshot,
    val stableKey: String,
    val message: RenderTextTokenSnapshot,
)

@Serializable
data class FrontstageReadabilitySnapshot(
    val mutationHighlights: List<RenderTextTokenSnapshot> = emptyList(),
    val terrainHighlights: List<RenderTextTokenSnapshot> = emptyList(),
    val recentActionCues: List<FrontstageActionCueSnapshot> = emptyList(),
)

@Serializable
data class ShopPanelSnapshot(
    val shopId: String,
    val shopNameKey: String,
    val hintLabelKeys: List<String> = emptyList(),
    val offers: List<ShopOfferSnapshot>,
    val sellEntries: List<ShopSellEntrySnapshot> = emptyList(),
    val inscriptionReplacementPrompt: InscriptionReplacementPromptSnapshot? = null,
)

@Serializable
data class ShopOfferSnapshot(
    val index: Int,
    val labelKey: String,
    val price: Int,
    val offerFingerprint: String,
    val tags: List<String> = emptyList(),
    val tagLabelKeys: List<String> = emptyList(),
)

@Serializable
data class InscriptionReplacementPromptSnapshot(
    val offerIndex: Int,
    val offerFingerprint: String,
    val candidate: InscriptionReplacementEntrySnapshot,
    val currentSlots: List<InscriptionReplacementEntrySnapshot>,
    val categoryChanges: List<InscriptionReplacementCategoryChangeSnapshot>,
    val price: Int,
    val rejectedReasonKey: String? = null,
)

@Serializable
data class InscriptionReplacementEntrySnapshot(
    val hotkey: Int? = null,
    val inscriptionId: String,
    val nameKey: String,
    val descKey: String,
    val iconKey: String,
    val categoryId: String,
    val categoryLabelKey: String = "",
    val effectTagLabelKeys: List<String> = emptyList(),
    val cooldownRemaining: Int = 0,
    val maxCooldown: Int = 0,
    val upgradeFromInscriptionId: String? = null,
)

@Serializable
data class InscriptionReplacementCategoryChangeSnapshot(
    val targetHotkey: Int? = null,
    val categoryId: String,
    val categoryLabelKey: String = "",
    val beforeCount: Int,
    val afterCount: Int,
    val limit: Int,
)

@Serializable
data class ShopSellEntrySnapshot(
    val inventoryIndex: Int,
    val price: Int,
)

@Serializable
data class RouteSelectionSnapshot(
    val currentZoneNameKey: String,
    val options: List<RouteOptionSnapshot>,
)

@Serializable
data class RouteOptionSnapshot(
    val index: Int,
    val routeId: String,
    val destinationZoneId: String,
    val destinationZoneNameKey: String,
    val destinationZoneDescKey: String? = null,
    val recommendedLevelMin: Int,
    val recommendedLevelMax: Int,
    val shardReward: Int,
    val guaranteedRewardItemNameKeys: List<String> = emptyList(),
    val milestoneRewardLabelKey: String? = null,
    val rescueHintLabelKeys: List<String> = emptyList(),
    val mechanicHintKey: String? = null,
    val isReturnPath: Boolean = false,
)

@Serializable
data class ItemRenderSnapshot(
    val baseItemId: String,
    val specialTemplateId: String? = null,
    val specialTierId: String? = null,
    val nameKey: String,
    val displayName: RenderTextTokenSnapshot? = null,
    val descKey: String? = null,
    val typeId: String,
    val visualKey: String? = null,
    val iconKey: String? = null,
    val audioProfile: String? = null,
    val slotId: String? = null,
    val qualityTierId: String = "NORMAL",
    val qualityNameKey: String? = null,
    val materialNameKey: String? = null,
    val affixIds: List<String> = emptyList(),
    val affixNameKeys: List<String> = emptyList(),
    val passiveDescriptions: List<RenderTextTokenSnapshot> = emptyList(),
    val stats: ItemStatModifierSnapshot = ItemStatModifierSnapshot(),
    val effectTypeId: String? = null,
    val resourceTypeId: String? = null,
    val magnitude: Int = 0,
) {
    init {
        val validSpecialTiers = setOf("UNIQUE", "ARTIFACT")
        require(specialTierId == null || specialTierId in validSpecialTiers) {
            "ItemRenderSnapshot.specialTierId must be UNIQUE or ARTIFACT when present."
        }
        require((specialTemplateId == null) == (specialTierId == null)) {
            "ItemRenderSnapshot requires specialTemplateId and specialTierId to be present together."
        }
    }
}

@Serializable
data class ItemStatModifierSnapshot(
    val str: Int = 0,
    val dex: Int = 0,
    val con: Int = 0,
    val wil: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val accuracy: Int = 0,
    val evasion: Int = 0,
    val speed: Int = 0,
    val castSpeedRating: Int = 0,
    val maxHp: Int = 0,
    val maxStamina: Int = 0,
    val hpRegen: Double = 0.0,
    val staminaRegen: Double = 0.0,
    val critChance: Double = 0.0,
    val talentPower: Double = 0.0,
)

@Serializable
data class RenderTextArgumentSnapshot(
    val name: String,
    val value: String? = null,
    val valueKey: String? = null,
    val valueToken: RenderTextTokenSnapshot? = null,
)

@Serializable
data class RenderTextTokenSnapshot(
    val key: String,
    val arguments: List<RenderTextArgumentSnapshot> = emptyList(),
)

@Serializable
data class RenderLogEventSnapshot(
    val message: RenderTextTokenSnapshot,
)

@Serializable
data class GridPointSnapshot(
    val x: Int,
    val y: Int,
)
