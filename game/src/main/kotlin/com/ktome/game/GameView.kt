package com.ktome.game

import com.ktome.core.ecs.EntityId
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemType
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome

sealed interface PlayerCommand {
    data class Move(val delta: Point) : PlayerCommand

    data object Wait : PlayerCommand

    data object PickUp : PlayerCommand

    data object Ascend : PlayerCommand

    data object Descend : PlayerCommand

    data object SaveGame : PlayerCommand

    data class ActivateInventoryItem(val index: Int) : PlayerCommand

    data class UseTalent(val slot: Int, val target: Point? = null) : PlayerCommand

    data class AssignStat(val stat: PrimaryStat) : PlayerCommand

    data class AssignTalent(val slot: Int) : PlayerCommand
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
    val currentStamina: Int,
    val maxStamina: Int,
    val level: Int,
    val currentExperience: Int,
    val nextLevelRequirement: Int,
    val statPoints: Int,
    val talentPoints: Int,
    val attack: Int,
    val defense: Int,
    val accuracy: Int,
    val evasion: Int,
    val speed: Int,
)

data class InventoryItemView(
    val index: Int,
    val name: String,
    val type: ItemType,
    val equippedSlot: EquipSlot? = null,
)

data class EquipmentSlotView(
    val slot: EquipSlot,
    val itemName: String?,
)

data class TalentSlotView(
    val slot: Int,
    val talentId: String,
    val name: String,
    val level: Int,
    val maxLevel: Int,
    val staminaCost: Int,
    val currentCooldown: Int,
    val maxCooldown: Int,
    val requiresTarget: Boolean,
)

data class RunSummary(
    val outcome: RunOutcome,
    val floorReached: Int,
    val maxFloor: Int,
    val turns: Int,
    val playerLevel: Int,
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
