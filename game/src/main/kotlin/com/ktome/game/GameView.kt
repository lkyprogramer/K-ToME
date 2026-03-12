package com.ktome.game

import com.ktome.core.ecs.EntityId
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemType
import com.ktome.core.map.Point

sealed interface PlayerCommand {
    data class Move(val delta: Point) : PlayerCommand

    data object Wait : PlayerCommand

    data object PickUp : PlayerCommand

    data class ActivateInventoryItem(val index: Int) : PlayerCommand

    data class UseTalent(val slot: Int, val target: Point? = null) : PlayerCommand
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
    val staminaCost: Int,
    val currentCooldown: Int,
    val maxCooldown: Int,
    val requiresTarget: Boolean,
)
