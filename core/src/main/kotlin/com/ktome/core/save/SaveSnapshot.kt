package com.ktome.core.save

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.Faction
import com.ktome.core.ecs.Stats
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemInstance
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.talent.ActiveEffect

data class SaveSnapshot(
    val version: Int = CURRENT_VERSION,
    val timestampEpochMillis: Long,
    val seed: Long,
    val mapWidth: Int,
    val mapHeight: Int,
    val fovRadius: Int,
    val messageLogSize: Int,
    val currentFloor: Int,
    val maxFloor: Int,
    val turnCount: Int,
    val messageLog: List<String>,
    val player: PlayerSnapshot,
    val floors: List<FloorSnapshot>,
    val combatRandomState: Long? = null,
    val sessionRandomState: Long? = null,
    val pendingActionIds: List<Int> = emptyList(),
    val activeTurnActorId: Int? = null,
) {
    init {
        validateOrThrow()
    }

    companion object {
        const val CURRENT_VERSION: Int = 2

        fun isSupportedVersion(version: Int): Boolean = version == CURRENT_VERSION
    }

    fun validateOrThrow() {
        require(mapWidth > 0) { "Map width must be positive." }
        require(mapHeight > 0) { "Map height must be positive." }
        require(fovRadius > 0) { "FOV radius must be positive." }
        require(messageLogSize > 0) { "Message log size must be positive." }
        require(currentFloor in 1..maxFloor) { "Current floor $currentFloor must be within 1..$maxFloor." }
        require(floors.isNotEmpty()) { "At least one floor snapshot is required." }
        require(floors.all { floor -> floor.floor in 1..maxFloor }) {
            "All floor snapshots must be within 1..$maxFloor."
        }
        require(floors.distinctBy(FloorSnapshot::floor).size == floors.size) {
            "Floor snapshots must not contain duplicates."
        }
        require(floors.any { floor -> floor.floor == currentFloor }) {
            "Current floor $currentFloor must exist in the snapshot."
        }
        require(pendingActionIds.all { pendingId -> pendingId > 0 }) {
            "Pending action entity ids must be positive."
        }
        require(activeTurnActorId == null || activeTurnActorId > 0) {
            "Active turn actor id must be positive when present."
        }
        require(activeTurnActorId == null || activeTurnActorId in pendingActionIds) {
            "Active turn actor must exist in the pending action queue."
        }
        floors.forEach(FloorSnapshot::validateOrThrow)
        player.validateOrThrow()
    }
}

data class PlayerSnapshot(
    val entity: EntitySnapshot,
    val carriedEntities: List<EntitySnapshot> = emptyList(),
) {
    fun validateOrThrow() {
        entity.validateOrThrow()
        carriedEntities.forEach(EntitySnapshot::validateOrThrow)
    }
}

data class FloorSnapshot(
    val floor: Int,
    val map: MapSnapshot,
    val stairsUp: Point? = null,
    val stairsDown: Point? = null,
    val exploredTiles: List<Point> = emptyList(),
    val entities: List<EntitySnapshot> = emptyList(),
) {
    init {
        validateOrThrow()
    }

    fun validateOrThrow() {
        require(floor > 0) { "Floor numbers must be positive." }
        map.validateOrThrow()
        entities.forEach(EntitySnapshot::validateOrThrow)
    }
}

data class MapSnapshot(
    val rows: List<String>,
    val playerStart: Point,
) {
    fun validateOrThrow() {
        GameMap.fromAscii(rows = rows, playerStart = playerStart)
    }
}

data class EntitySnapshot(
    val id: Int,
    val position: Point? = null,
    val glyph: Char? = null,
    val colorHex: String? = null,
    val name: String? = null,
    val blocksMovement: Boolean = false,
    val faction: Faction? = null,
    val stats: Stats? = null,
    val combatProfile: CombatProfile? = null,
    val healthCurrent: Int? = null,
    val staminaCurrent: Int? = null,
    val energyCurrent: Int? = null,
    val experience: Experience? = null,
    val experienceReward: Int? = null,
    val aiBehavior: AIBehavior? = null,
    val monsterTemplateId: String? = null,
    val patrolRoute: PatrolRouteSnapshot? = null,
    val inventory: InventorySnapshot? = null,
    val equipment: EquipmentSnapshot? = null,
    val cooldowns: Map<String, Int>? = null,
    val effects: List<ActiveEffect>? = null,
    val talentLoadout: TalentLoadoutSnapshot? = null,
    val itemInstance: ItemInstance? = null,
    val isGroundItem: Boolean = false,
    val isPlayerControlled: Boolean = false,
    val stair: StairSnapshot? = null,
) {
    init {
        validateOrThrow()
    }

    fun validateOrThrow() {
        require(id > 0) { "Entity ids must be positive." }
    }
}

data class PatrolRouteSnapshot(
    val waypoints: List<Point>,
    val nextWaypointIndex: Int = 0,
)

data class InventorySnapshot(
    val capacity: Int = 12,
    val itemIds: List<Int> = emptyList(),
)

data class EquipmentSnapshot(
    val slots: Map<EquipSlot, Int> = emptyMap(),
)

data class TalentLoadoutSnapshot(
    val slotToTalentId: Map<Int, String> = emptyMap(),
    val talentLevels: Map<String, Int> = emptyMap(),
)

data class StairSnapshot(
    val direction: StairDirection,
)
