package com.ktome.core.snapshot

import kotlinx.serialization.Serializable

@Serializable
data class RenderSnapshot(
    val metadata: RenderMetadataSnapshot,
    val mapCells: List<MapCellSnapshot>,
    val props: List<PropRenderSnapshot> = emptyList(),
    val actors: List<ActorRenderSnapshot> = emptyList(),
    val overlays: List<OverlayRenderSnapshot> = emptyList(),
    val uiState: RenderUiStateSnapshot,
    val logEvents: List<RenderLogEventSnapshot> = emptyList(),
)

@Serializable
data class RenderMetadataSnapshot(
    val revision: Long,
    val zoneId: String,
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
    val stairDirectionId: String? = null,
    val actorEntityId: Int? = null,
    val items: List<ItemRenderSnapshot> = emptyList(),
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
)

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
)

@Serializable
data class RenderUiStateSnapshot(
    val playerStatus: PlayerStatusSnapshot,
    val equipment: List<EquipmentSlotSnapshot>,
    val talents: List<TalentSlotSnapshot>,
    val inventory: List<InventoryEntrySnapshot>,
    val targetablePositions: List<GridPointSnapshot>,
)

@Serializable
data class PlayerStatusSnapshot(
    val currentHp: Int,
    val maxHp: Int,
    val currentResource: Int,
    val maxResource: Int,
    val resourceLabelKey: String,
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

@Serializable
data class EquipmentSlotSnapshot(
    val slotId: String,
    val item: ItemRenderSnapshot? = null,
)

@Serializable
data class TalentSlotSnapshot(
    val slot: Int,
    val talentId: String,
    val nameKey: String,
    val visualKey: String? = null,
    val iconKey: String? = null,
    val audioProfile: String? = null,
    val level: Int,
    val maxLevel: Int,
    val resourceCost: Int,
    val resourceLabelKey: String,
    val range: Int,
    val minRange: Int,
    val currentCooldown: Int,
    val maxCooldown: Int,
    val requiresTarget: Boolean,
)

@Serializable
data class InventoryEntrySnapshot(
    val index: Int,
    val item: ItemRenderSnapshot,
    val equippedSlotId: String? = null,
)

@Serializable
data class ItemRenderSnapshot(
    val baseItemId: String,
    val nameKey: String,
    val typeId: String,
    val visualKey: String? = null,
    val iconKey: String? = null,
    val audioProfile: String? = null,
    val slotId: String? = null,
    val stats: ItemStatModifierSnapshot = ItemStatModifierSnapshot(),
    val effectTypeId: String? = null,
    val magnitude: Int = 0,
)

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
