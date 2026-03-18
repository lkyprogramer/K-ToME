package com.ktome.core.save

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import kotlinx.serialization.Serializable

@Serializable
data class SaveSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val saveContractVersion: SaveContractVersion = SaveContractVersion.CURRENT,
    val buildMetadata: String = DEFAULT_BUILD_METADATA,
    val timestampEpochMillis: Long,
    val worldSeed: Long,
    val currentZoneId: String,
    val floorIndex: Int,
    val mapWidth: Int,
    val mapHeight: Int,
    val fovRadius: Int,
    val messageLogSize: Int,
    val playerProfessionId: String,
    val maxFloor: Int,
    val turnCount: Int,
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

    fun validateOrThrow() {
        require(schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Schema version $schemaVersion must equal $CURRENT_SCHEMA_VERSION."
        }
        require(saveContractVersion == SaveContractVersion.CURRENT) {
            "Save contract version must be ${SaveContractVersion.CURRENT}."
        }
        require(buildMetadata.isNotBlank()) { "buildMetadata must not be blank." }
        require(currentZoneId.isNotBlank()) { "currentZoneId must not be blank." }
        require(playerProfessionId.isNotBlank()) { "playerProfessionId must not be blank." }
        require(mapWidth > 0) { "Map width must be positive." }
        require(mapHeight > 0) { "Map height must be positive." }
        require(fovRadius > 0) { "FOV radius must be positive." }
        require(messageLogSize > 0) { "Message log size must be positive." }
        require(floorIndex in 1..maxFloor) { "Floor index $floorIndex must be within 1..$maxFloor." }
        require(floors.isNotEmpty()) { "At least one floor snapshot is required." }
        require(floors.all { floor -> floor.floorIndex in 1..maxFloor }) {
            "All floor snapshots must be within 1..$maxFloor."
        }
        require(floors.distinctBy(FloorSnapshot::floorIndex).size == floors.size) {
            "Floor snapshots must not contain duplicates."
        }
        require(floors.any { floor -> floor.floorIndex == floorIndex }) {
            "Current floor $floorIndex must exist in the snapshot."
        }
        require(floors.all { floor -> floor.map.rows.size == mapHeight }) {
            "All floor snapshots must match top-level mapHeight $mapHeight."
        }
        require(
            floors.all { floor ->
                floor.map.rows.all { row -> row.length == mapWidth }
            },
        ) {
            "All floor snapshots must match top-level mapWidth $mapWidth."
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
        player.validateOrThrow()
        floors.forEach(FloorSnapshot::validateOrThrow)
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
        const val DEFAULT_BUILD_METADATA: String = "phase2-dev"
    }
}

@Serializable
data class PlayerSnapshot(
    val entity: EntitySnapshot,
    val carriedEntities: List<EntitySnapshot> = emptyList(),
) {
    fun validateOrThrow() {
        entity.validateOrThrow()
        require(entity.isPlayerControlled) { "Player snapshot entity must be player controlled." }
        carriedEntities.forEach(EntitySnapshot::validateOrThrow)
    }
}

@Serializable
data class FloorSnapshot(
    val floorIndex: Int,
    val map: MapSnapshot,
    val stairsUp: PointSnapshot? = null,
    val stairsDown: PointSnapshot? = null,
    val exploredTiles: List<PointSnapshot> = emptyList(),
    val entities: List<EntitySnapshot> = emptyList(),
) {
    init {
        validateOrThrow()
    }

    fun validateOrThrow() {
        require(floorIndex > 0) { "Floor numbers must be positive." }
        map.validateOrThrow()
        entities.forEach(EntitySnapshot::validateOrThrow)
    }
}

@Serializable
data class MapSnapshot(
    val rows: List<String>,
    val playerStart: PointSnapshot,
) {
    fun validateOrThrow() {
        GameMap.fromAscii(rows = rows, playerStart = playerStart.toPoint())
    }
}

@Serializable
data class PointSnapshot(
    val x: Int,
    val y: Int,
) {
    fun toPoint(): Point = Point(x, y)

    companion object {
        fun from(point: Point): PointSnapshot = PointSnapshot(x = point.x, y = point.y)
    }
}

@Serializable
data class EntitySnapshot(
    val id: Int,
    val position: PointSnapshot? = null,
    val blocksMovement: Boolean = false,
    val faction: String? = null,
    val stats: StatsSnapshot? = null,
    val combatProfile: CombatProfileSnapshot? = null,
    val healthCurrent: Int? = null,
    val staminaCurrent: Int? = null,
    val energyCurrent: Int? = null,
    val experience: ExperienceSnapshot? = null,
    val experienceReward: Int? = null,
    val aiBehavior: AIBehaviorSnapshot? = null,
    val monsterTemplateId: String? = null,
    val patrolRoute: PatrolRouteSnapshot? = null,
    val inventory: InventorySnapshot? = null,
    val equipment: EquipmentSnapshot? = null,
    val cooldowns: Map<String, Int>? = null,
    val effects: List<ActiveEffectSnapshot>? = null,
    val talentLoadout: TalentLoadoutSnapshot? = null,
    val itemState: ItemSnapshot? = null,
    val isGroundItem: Boolean = false,
    val isPlayerControlled: Boolean = false,
    val stair: StairSnapshot? = null,
) {
    init {
        validateOrThrow()
    }

    fun validateOrThrow() {
        require(id > 0) { "Entity ids must be positive." }
        require(healthCurrent == null || healthCurrent >= 0) { "healthCurrent must not be negative." }
        require(staminaCurrent == null || staminaCurrent >= 0) { "staminaCurrent must not be negative." }
        require(cooldowns?.values?.all { value -> value >= 0 } != false) {
            "Cooldown values must not be negative."
        }
        inventory?.validateOrThrow()
        equipment?.validateOrThrow()
        patrolRoute?.validateOrThrow()
        talentLoadout?.validateOrThrow()
        itemState?.validateOrThrow()
        effects?.forEach(ActiveEffectSnapshot::validateOrThrow)
    }
}

@Serializable
data class StatsSnapshot(
    val str: Int,
    val dex: Int,
    val con: Int,
    val wil: Int,
)

@Serializable
data class CombatProfileSnapshot(
    val baseAttack: Int,
    val baseDefense: Int,
    val baseAccuracy: Int = 10,
    val baseEvasion: Int = 5,
    val baseSpeed: Int = 100,
    val baseHp: Int = 50,
    val baseStamina: Int = 40,
    val baseHpRegen: Double = 1.0,
)

@Serializable
data class ExperienceSnapshot(
    val current: Int = 0,
    val level: Int = 1,
    val unspentStatPoints: Int = 0,
    val unspentTalentPoints: Int = 0,
)

@Serializable
data class AIBehaviorSnapshot(
    val type: String,
    val sightRadius: Int = 8,
    val preferredRangeStart: Int = 1,
    val preferredRangeEnd: Int = 1,
)

@Serializable
data class PatrolRouteSnapshot(
    val waypoints: List<PointSnapshot>,
    val nextWaypointIndex: Int = 0,
) {
    fun validateOrThrow() {
        require(waypoints.isNotEmpty()) { "Patrol routes must define at least one waypoint." }
        require(nextWaypointIndex in waypoints.indices) { "Patrol route index must reference an existing waypoint." }
    }
}

@Serializable
data class InventorySnapshot(
    val capacity: Int = 12,
    val itemIds: List<Int> = emptyList(),
) {
    fun validateOrThrow() {
        require(capacity > 0) { "Inventory capacity must be positive." }
        require(itemIds.all { itemId -> itemId > 0 }) { "Inventory item ids must be positive." }
    }
}

@Serializable
data class EquipmentSnapshot(
    val slots: Map<String, Int> = emptyMap(),
) {
    fun validateOrThrow() {
        require(slots.keys.all(String::isNotBlank)) { "Equipment slot ids must not be blank." }
        require(slots.values.all { itemId -> itemId > 0 }) { "Equipped item ids must be positive." }
    }
}

@Serializable
data class TalentLoadoutSnapshot(
    val slotToTalentId: Map<Int, String> = emptyMap(),
    val talentLevels: Map<String, Int> = emptyMap(),
) {
    fun validateOrThrow() {
        require(slotToTalentId.keys.all { slot -> slot > 0 }) { "Talent slots must be positive." }
        require(slotToTalentId.values.all(String::isNotBlank)) { "Talent ids must not be blank." }
        require(talentLevels.keys.all(String::isNotBlank)) { "Talent ids must not be blank." }
        require(talentLevels.values.all { level -> level > 0 }) { "Talent levels must be positive." }
    }
}

@Serializable
data class StairSnapshot(
    val direction: String,
)

@Serializable
data class ActiveEffectSnapshot(
    val id: String,
    val type: String,
    val remainingTurns: Int,
    val statModifiers: StatModifierSnapshot = StatModifierSnapshot(),
    val skipNextDecay: Boolean = false,
) {
    fun validateOrThrow() {
        require(id.isNotBlank()) { "Effect ids must not be blank." }
        require(type.isNotBlank()) { "Effect types must not be blank." }
        require(remainingTurns >= 0) { "remainingTurns must not be negative." }
    }
}

@Serializable
data class ItemSnapshot(
    val baseId: String,
    val type: String,
    val slot: String? = null,
    val quality: String,
    val materialId: String? = null,
    val affixIds: List<String> = emptyList(),
    val stats: StatModifierSnapshot = StatModifierSnapshot(),
    val effect: String? = null,
    val magnitude: Int = 0,
) {
    fun validateOrThrow() {
        require(baseId.isNotBlank()) { "Item base ids must not be blank." }
        require(type.isNotBlank()) { "Item types must not be blank." }
        require(quality.isNotBlank()) { "Item quality must not be blank." }
        require(affixIds.all(String::isNotBlank)) { "Item affix ids must not be blank." }
    }
}

@Serializable
data class StatModifierSnapshot(
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
    val attackMultiplierBonus: Double = 0.0,
    val defenseMultiplierBonus: Double = 0.0,
)
