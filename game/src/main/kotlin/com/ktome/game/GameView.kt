package com.ktome.game

import com.ktome.core.ecs.EntityId
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemType
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.talent.TalentTreeOwnerType

sealed interface PlayerCommand {
    data class Move(val delta: Point) : PlayerCommand

    data object Wait : PlayerCommand

    data object PickUp : PlayerCommand

    data object Interact : PlayerCommand

    data object Ascend : PlayerCommand

    data object Descend : PlayerCommand

    data object SaveGame : PlayerCommand

    data object CloseShop : PlayerCommand

    data class BuyShopOffer(val index: Int) : PlayerCommand

    data class SellInventoryItem(val index: Int) : PlayerCommand

    data class SelectRoute(val index: Int) : PlayerCommand

    data class ActivateInventoryItem(val index: Int) : PlayerCommand

    data class UseInscription(
        val hotkey: Int,
        val target: Point? = null,
    ) : PlayerCommand

    data class UseTalent(val slot: Int, val target: Point? = null) : PlayerCommand

    data class EquipTalentToSlot(val slot: Int, val talentId: String) : PlayerCommand

    data class AssignStat(val stat: PrimaryStat) : PlayerCommand

    data class AssignTalent(val talentId: String) : PlayerCommand

    data object ConfirmTalentDraft : PlayerCommand

    data object RollbackTalentDraft : PlayerCommand

    data class RespecTalentTree(
        val ownerType: TalentTreeOwnerType,
        val treeOwnerId: String,
    ) : PlayerCommand
}

enum class PrimaryStat {
    STR,
    DEX,
    CON,
    WIL,
}

data class ActorView(
    val entityId: EntityId,
    val position: Point,
    val glyph: Char,
    val colorHex: String,
    val name: String,
    val isPlayer: Boolean,
)

data class PlayerStatus(
    val currentHp: Int,
    val maxHp: Int,
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
)

data class PlayerResourceView(
    val current: Int,
    val max: Int,
    val typeId: String,
    val stableMin: Int? = null,
    val stableMax: Int? = null,
    val secondary: SecondaryPlayerResourceView? = null,
)

data class SecondaryPlayerResourceView(
    val current: Int,
    val max: Int,
    val typeId: String,
    val stableMin: Int? = null,
    val stableMax: Int? = null,
)

data class InscriptionView(
    val hotkey: Int,
    val inscriptionId: String,
    val nameKey: String,
    val descKey: String,
    val iconKey: String,
    val categoryId: String,
    val cooldownRemaining: Int,
    val maxCooldown: Int,
)

data class InventoryItemView(
    val index: Int,
    val name: String,
    val type: ItemType,
    val slot: EquipSlot? = null,
    val equippedSlot: EquipSlot? = null,
    val effect: ConsumableEffect? = null,
    val resourceTypeId: String? = null,
    val magnitude: Int = 0,
)

data class EquipmentSlotView(
    val slot: EquipSlot,
    val itemName: String?,
)

data class TalentSlotView(
    val slot: Int,
    val talentId: String,
    val name: String,
    val descKey: String? = null,
    val level: Int,
    val committedLevel: Int = level,
    val maxLevel: Int,
    val resourceCost: Int,
    val resourceTypeId: String = "STAMINA",
    val range: Int,
    val minRange: Int,
    val currentCooldown: Int,
    val maxCooldown: Int,
    val requiresTarget: Boolean,
    val descriptionModel: com.ktome.core.talent.DescriptionModel? = null,
    val nextBreakpointPreview: com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot? = null,
    val hasPendingAllocation: Boolean = false,
)

data class TalentReserveView(
    val talentId: String,
    val name: String,
    val descKey: String? = null,
    val level: Int,
    val committedLevel: Int = level,
    val maxLevel: Int,
    val resourceCost: Int,
    val resourceTypeId: String = "STAMINA",
    val range: Int,
    val minRange: Int,
    val currentCooldown: Int,
    val maxCooldown: Int,
    val requiresTarget: Boolean,
    val descriptionModel: com.ktome.core.talent.DescriptionModel? = null,
    val nextBreakpointPreview: com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot? = null,
    val hasPendingAllocation: Boolean = false,
)

data class OutcomeSummary(
    val outcome: RunOutcome,
    val floorReached: Int,
    val maxFloor: Int,
    val turns: Int,
    val headlessTurnEquivalent: Int = turns,
    val playerLevel: Int,
    val zoneNameKey: String,
    val zonePath: List<String> = emptyList(),
    val shardBalance: Int = 0,
    val defeatedBossIds: List<String> = emptyList(),
    val outcomeReasonKey: String,
    val killerNameKey: String?,
    val killerTemplateId: String?,
    val finalHpCurrent: Int,
    val finalHpMax: Int,
    val finalResourceTypeId: String,
    val finalResourceLabelKey: String,
    val finalResourceCurrent: Int,
    val finalResourceMax: Int,
    val lastEvents: List<RenderTextTokenSnapshot>,
)

enum class TileVisibility {
    VISIBLE,
    EXPLORED,
    HIDDEN,
}

data class InspectView(
    val point: Point,
    val visibility: TileVisibility,
    val terrainName: String,
    val actor: InspectActorView? = null,
    val items: List<InspectItemView> = emptyList(),
    val stairLabel: String? = null,
    val stairDirectionId: String? = null,
)

data class InspectActorView(
    val name: String,
    val role: String,
    val currentHp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val accuracy: Int,
    val evasion: Int,
    val speed: Int,
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val willpower: Int,
    val statusEffects: List<String> = emptyList(),
)

data class InspectItemView(
    val name: String,
    val typeLabel: String,
    val details: List<String>,
)
