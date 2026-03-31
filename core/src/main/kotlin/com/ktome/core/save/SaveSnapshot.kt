package com.ktome.core.save

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.economy.ShopInventoryState
import com.ktome.core.profile.MilestoneRewardSummary
import com.ktome.core.resource.ResourcePoolSnapshot
import com.ktome.core.world.WorldProgressDef
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class SaveSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val saveContractVersion: SaveContractVersion = SaveContractVersion.CURRENT,
    val buildMetadata: String = DEFAULT_BUILD_METADATA,
    val timestampEpochMillis: Long,
    val worldSeed: Long,
    val currentZoneId: String,
    val zoneRoute: List<String> = listOf(currentZoneId),
    val routeIndex: Int = 0,
    val worldProgress: WorldProgressDef = WorldProgressDef(),
    val shardBalance: Int = 0,
    val shopStates: List<ShopInventoryState> = emptyList(),
    val cadenceRewardCount: Int = 0,
    val currentFloorRewardState: FloorRewardStateSnapshot = FloorRewardStateSnapshot(),
    val floorIndex: Int,
    val mapWidth: Int,
    val mapHeight: Int,
    val fovRadius: Int,
    val messageLogSize: Int,
    val playerProfessionId: String,
    val playerRaceId: String,
    val maxFloor: Int,
    val turnCount: Int,
    val headlessTurnEquivalent: Int = turnCount,
    val player: PlayerSnapshot,
    val floors: List<FloorSnapshot>,
    val combatRandomState: Long? = null,
    val sessionRandomState: Long? = null,
    val milestoneRewards: List<MilestoneRewardSummary> = emptyList(),
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
        require(zoneRoute.isNotEmpty()) { "zoneRoute must not be empty." }
        require(zoneRoute.all(String::isNotBlank)) { "zoneRoute must not contain blank zone ids." }
        require(routeIndex in zoneRoute.indices) { "routeIndex $routeIndex must be within zoneRoute indices." }
        require(zoneRoute[routeIndex] == currentZoneId) {
            "currentZoneId '$currentZoneId' must match zoneRoute[$routeIndex]='${zoneRoute[routeIndex]}'."
        }
        require(shardBalance >= 0) { "shardBalance must not be negative." }
        require(shopStates.distinctBy(ShopInventoryState::shopId).size == shopStates.size) {
            "shopStates must not contain duplicate shop ids."
        }
        require(cadenceRewardCount >= 0) { "cadenceRewardCount must not be negative." }
        require(playerProfessionId.isNotBlank()) { "playerProfessionId must not be blank." }
        require(playerRaceId.isNotBlank()) { "playerRaceId must not be blank." }
        require(mapWidth > 0) { "Map width must be positive." }
        require(mapHeight > 0) { "Map height must be positive." }
        require(fovRadius > 0) { "FOV radius must be positive." }
        require(messageLogSize > 0) { "Message log size must be positive." }
        require(floorIndex in 1..maxFloor) { "Floor index $floorIndex must be within 1..$maxFloor." }
        require(turnCount >= 0) { "turnCount must not be negative." }
        require(headlessTurnEquivalent >= 0) { "headlessTurnEquivalent must not be negative." }
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
        require(milestoneRewards.distinctBy { reward -> "${reward.rewardSource}:${reward.sourceId}" }.size == milestoneRewards.size) {
            "Milestone rewards must not contain duplicate source entries."
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
        const val CURRENT_SCHEMA_VERSION: Int = 7
        const val DEFAULT_BUILD_METADATA: String = "phase3-pr14-dev"
    }
}

@Serializable
data class FloorRewardStateSnapshot(
    val meaningfulRewardSeenThisFloor: Boolean = false,
    val cadenceRewardGrantedThisFloor: Boolean = false,
)

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
    val rewardState: FloorRewardStateSnapshot = FloorRewardStateSnapshot(),
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
    val areaEffectEmitter: AreaEffectEmitterSnapshot? = null,
    val worldEffect: WorldEffectSnapshot? = null,
    val aiTriggerTracker: AiTriggerTrackerSnapshot? = null,
    val aiPerception: AIPerceptionSnapshot? = null,
    val pendingTelegraph: PendingTelegraphSnapshot? = null,
    val bossEncounterState: BossEncounterStateSnapshot? = null,
    val patrolPressureState: PatrolPressureStateSnapshot? = null,
    val ambushLaneTrigger: AmbushLaneTriggerSnapshot? = null,
    val furnacePressureState: FurnacePressureStateSnapshot? = null,
    val riverCurrentState: RiverCurrentStateSnapshot? = null,
    val crystalShardPressureState: CrystalShardPressureStateSnapshot? = null,
    val resourcePools: List<ResourcePoolSnapshot> = emptyList(),
    val equilibriumLastAffinity: String? = null,
    val raceTalentPoints: Int? = null,
    val inscriptionLoadout: InscriptionLoadoutSnapshot? = null,
    val inscriptionCooldowns: Map<String, Int>? = null,
    val talentLoadout: TalentLoadoutSnapshot? = null,
    val talentAllocationDraft: TalentAllocationDraftSnapshot? = null,
    val itemState: ItemSnapshot? = null,
    val isGroundItem: Boolean = false,
    val isPlayerControlled: Boolean = false,
    val interactableId: String? = null,
    val stair: StairSnapshot? = null,
) {
    init {
        validateOrThrow()
    }

    fun validateOrThrow() {
        require(id > 0) { "Entity ids must be positive." }
        require(healthCurrent == null || healthCurrent >= 0) { "healthCurrent must not be negative." }
        require(cooldowns?.values?.all { value -> value >= 0 } != false) {
            "Cooldown values must not be negative."
        }
        require(resourcePools.distinctBy(ResourcePoolSnapshot::type).size == resourcePools.size) {
            "Resource pools must not contain duplicate types."
        }
        require(raceTalentPoints == null || raceTalentPoints >= 0) {
            "raceTalentPoints must not be negative."
        }
        require(inscriptionCooldowns?.keys?.all(String::isNotBlank) != false) {
            "Inscription cooldown ids must not be blank."
        }
        require(inscriptionCooldowns?.values?.all { value -> value >= 0 } != false) {
            "Inscription cooldowns must not be negative."
        }
        require(interactableId == null || interactableId.isNotBlank()) { "Interactable ids must not be blank." }
        inventory?.validateOrThrow()
        equipment?.validateOrThrow()
        inscriptionLoadout?.validateOrThrow()
        patrolRoute?.validateOrThrow()
        talentLoadout?.validateOrThrow()
        talentAllocationDraft?.validateOrThrow()
        itemState?.validateOrThrow()
        aiTriggerTracker?.validateOrThrow()
        aiPerception?.validateOrThrow()
        pendingTelegraph?.validateOrThrow()
        bossEncounterState?.validateOrThrow()
        patrolPressureState?.validateOrThrow()
        ambushLaneTrigger?.validateOrThrow()
        furnacePressureState?.validateOrThrow()
        riverCurrentState?.validateOrThrow()
        crystalShardPressureState?.validateOrThrow()
        effects?.forEach(ActiveEffectSnapshot::validateOrThrow)
        areaEffectEmitter?.validateOrThrow()
        worldEffect?.validateOrThrow()
        resourcePools.forEach(ResourcePoolSnapshot::validate)
    }
}

@Serializable
data class AiTriggerTrackerSnapshot(
    val consumedTriggerIds: List<String> = emptyList(),
    val pendingCombatStartTriggerIds: List<String> = emptyList(),
    val engagedInCombat: Boolean = false,
) {
    fun validateOrThrow() {
        require(consumedTriggerIds.all(String::isNotBlank)) { "Consumed trigger ids must not be blank." }
        require(consumedTriggerIds.distinct().size == consumedTriggerIds.size) {
            "Consumed trigger ids must not contain duplicates."
        }
        require(pendingCombatStartTriggerIds.all(String::isNotBlank)) { "Pending combat-start trigger ids must not be blank." }
        require(pendingCombatStartTriggerIds.distinct().size == pendingCombatStartTriggerIds.size) {
            "Pending combat-start trigger ids must not contain duplicates."
        }
    }
}

@Serializable
data class AIPerceptionSnapshot(
    val lastKnownTargetPosition: PointSnapshot? = null,
) {
    fun validateOrThrow() {
        // No-op; PointSnapshot is already structurally validated by construction.
    }
}

@Serializable
data class PendingTelegraphSnapshot(
    val telegraphSpecId: String,
    val sourceAbilityId: String,
    val remainingTurns: Int,
    val targetPoint: PointSnapshot,
    val queuedAbilityId: String? = null,
    val dangerLevel: String,
) {
    fun validateOrThrow() {
        require(telegraphSpecId.isNotBlank()) { "Telegraph spec id must not be blank." }
        require(sourceAbilityId.isNotBlank()) { "Telegraph source ability id must not be blank." }
        require(remainingTurns > 0) { "Telegraph remaining turns must stay positive." }
        require(dangerLevel.isNotBlank()) { "Telegraph danger level must not be blank." }
    }
}

@Serializable
data class BossEncounterStateSnapshot(
    val encounterId: String,
    val currentPhaseId: String? = null,
    val encounterTurnCount: Int = 0,
    val phaseTurnCount: Int = 0,
) {
    fun validateOrThrow() {
        require(encounterId.isNotBlank()) { "Boss encounter id must not be blank." }
        require(encounterTurnCount >= 0) { "Boss encounter turn count must not be negative." }
        require(phaseTurnCount >= 0) { "Boss phase turn count must not be negative." }
    }
}

@Serializable
data class PatrolPressureStateSnapshot(
    val spawnTemplateIds: List<String> = emptyList(),
    val maxHostiles: Int,
    val waveLimit: Int,
    val checkIntervalTurns: Int,
    val spawnSeed: Long,
    val nextCheckHeadlessTurn: Int,
    val wavesSpawned: Int = 0,
    val floorHintShown: Boolean = false,
) {
    fun validateOrThrow() {
        require(spawnTemplateIds.all(String::isNotBlank)) { "Patrol pressure spawn templates must not be blank." }
        require(spawnTemplateIds.isNotEmpty()) { "Patrol pressure runtime must declare at least one spawn template." }
        require(maxHostiles > 0) { "Patrol pressure maxHostiles must be positive." }
        require(waveLimit > 0) { "Patrol pressure waveLimit must be positive." }
        require(checkIntervalTurns > 0) { "Patrol pressure checkIntervalTurns must be positive." }
        require(nextCheckHeadlessTurn >= 0) { "Patrol pressure nextCheckHeadlessTurn must not be negative." }
        require(wavesSpawned >= 0) { "Patrol pressure wavesSpawned must not be negative." }
    }
}

@Serializable
data class AmbushLaneTriggerSnapshot(
    val triggerId: String,
    val spawnTemplateIds: List<String> = emptyList(),
    val spawnPoints: List<PointSnapshot> = emptyList(),
) {
    fun validateOrThrow() {
        require(triggerId.isNotBlank()) { "Ambush trigger id must not be blank." }
        require(spawnTemplateIds.all(String::isNotBlank)) { "Ambush trigger spawn templates must not be blank." }
        require(spawnTemplateIds.isNotEmpty()) { "Ambush trigger must declare at least one spawn template." }
        require(spawnPoints.isNotEmpty()) { "Ambush trigger must declare at least one spawn point." }
    }
}

@Serializable
data class FurnacePressureStateSnapshot(
    val hazardCells: List<PointSnapshot> = emptyList(),
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
    val nextCycleTurn: Int,
    val phase: String,
    val phaseTurnsRemaining: Int = 0,
) {
    fun validateOrThrow() {
        require(hazardCells.isNotEmpty()) { "Furnace pressure hazardCells must not be empty." }
        require(cycleIntervalTurns > 0) { "Furnace pressure cycleIntervalTurns must be positive." }
        require(telegraphTurns > 0) { "Furnace pressure telegraphTurns must be positive." }
        require(activeTurns > 0) { "Furnace pressure activeTurns must be positive." }
        require(damagePerTick > 0) { "Furnace pressure damagePerTick must be positive." }
        require(nextCycleTurn >= 0) { "Furnace pressure nextCycleTurn must not be negative." }
        require(phase.isNotBlank()) { "Furnace pressure phase must not be blank." }
        require(phaseTurnsRemaining >= 0) { "Furnace pressure phaseTurnsRemaining must not be negative." }
    }
}

@Serializable
data class RiverCurrentStateSnapshot(
    val laneCells: List<PointSnapshot> = emptyList(),
    val approachCells: List<PointSnapshot> = emptyList(),
    val safeCells: List<PointSnapshot> = emptyList(),
    val pushDx: Int,
    val pushDy: Int,
) {
    fun validateOrThrow() {
        require(laneCells.isNotEmpty()) { "River current laneCells must not be empty." }
        require(approachCells.isNotEmpty()) { "River current approachCells must not be empty." }
        require(abs(pushDx) + abs(pushDy) == 1) { "River current push vector must be cardinal." }
    }
}

@Serializable
data class CrystalShardPressureStateSnapshot(
    val hazardCells: List<PointSnapshot> = emptyList(),
    val cycleIntervalTurns: Int,
    val telegraphTurns: Int,
    val activeTurns: Int,
    val damagePerTick: Int,
    val nextCycleTurn: Int,
    val phase: String,
    val phaseTurnsRemaining: Int = 0,
) {
    fun validateOrThrow() {
        require(hazardCells.isNotEmpty()) { "Crystal shard hazardCells must not be empty." }
        require(cycleIntervalTurns > 0) { "Crystal shard cycleIntervalTurns must be positive." }
        require(telegraphTurns > 0) { "Crystal shard telegraphTurns must be positive." }
        require(activeTurns > 0) { "Crystal shard activeTurns must be positive." }
        require(damagePerTick > 0) { "Crystal shard damagePerTick must be positive." }
        require(nextCycleTurn >= 0) { "Crystal shard nextCycleTurn must not be negative." }
        require(phase.isNotBlank()) { "Crystal shard phase must not be blank." }
        require(phaseTurnsRemaining >= 0) { "Crystal shard phaseTurnsRemaining must not be negative." }
    }
}

private fun ResourcePoolSnapshot.validate() {
    require(current <= max) { "Resource pool current must not exceed max for type '$type'." }
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
data class InscriptionLoadoutSnapshot(
    val slots: List<InscriptionSlotSaveSnapshot> = emptyList(),
) {
    fun validateOrThrow() {
        require(slots.distinctBy(InscriptionSlotSaveSnapshot::hotkey).size == slots.size) {
            "Inscription hotkeys must stay unique."
        }
        slots.forEach(InscriptionSlotSaveSnapshot::validateOrThrow)
    }
}

@Serializable
data class InscriptionSlotSaveSnapshot(
    val hotkey: Int,
    val inscriptionId: String,
) {
    fun validateOrThrow() {
        require(hotkey in 5..8) { "Inscription hotkey must stay within 5..8." }
        require(inscriptionId.isNotBlank()) { "Inscription ids must not be blank." }
    }
}

@Serializable
data class TalentAllocationDraftSnapshot(
    val ownerType: String,
    val treeOwnerId: String,
    val pendingRanks: Map<String, Int> = emptyMap(),
    val previousPendingRanks: Map<String, Int>? = null,
) {
    fun validateOrThrow() {
        require(ownerType.isNotBlank()) { "Talent draft ownerType must not be blank." }
        require(treeOwnerId.isNotBlank()) { "Talent draft treeOwnerId must not be blank." }
        require(pendingRanks.keys.all(String::isNotBlank)) { "Talent draft ids must not be blank." }
        require(pendingRanks.values.all { rank -> rank >= 0 }) { "Talent draft ranks must not be negative." }
        require(previousPendingRanks?.keys?.all(String::isNotBlank) != false) {
            "Talent draft rollback ids must not be blank."
        }
        require(previousPendingRanks?.values?.all { rank -> rank >= 0 } != false) {
            "Talent draft rollback ranks must not be negative."
        }
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
    val stackCount: Int = 1,
    val appliedTurn: Int = 0,
    val sourceEntityId: Int? = null,
    val magnitude: Double = 0.0,
) {
    fun validateOrThrow() {
        require(id.isNotBlank()) { "Effect ids must not be blank." }
        require(type.isNotBlank()) { "Effect types must not be blank." }
        require(remainingTurns >= 0) { "remainingTurns must not be negative." }
        require(stackCount > 0) { "stackCount must be positive." }
    }
}

@Serializable
data class AreaEffectEmitterSnapshot(
    val emitterId: String,
    val sourceEntityId: Int? = null,
    val affectedActorIds: List<Int> = emptyList(),
    val emitterPriority: Int = 200,
    val effects: List<ActiveEffectSnapshot> = emptyList(),
) {
    fun validateOrThrow() {
        require(emitterId.isNotBlank()) { "Area effect emitter ids must not be blank." }
        require(emitterPriority >= 0) { "Area effect emitter priority must not be negative." }
        require(affectedActorIds.all { id -> id > 0 }) { "Affected actor ids must be positive." }
        effects.forEach(ActiveEffectSnapshot::validateOrThrow)
    }
}

@Serializable
data class WorldEffectSnapshot(
    val effectId: String,
    val affectedActorIds: List<Int> = emptyList(),
    val worldPriority: Int = 300,
    val effects: List<ActiveEffectSnapshot> = emptyList(),
) {
    fun validateOrThrow() {
        require(effectId.isNotBlank()) { "World effect ids must not be blank." }
        require(worldPriority >= 0) { "World effect priority must not be negative." }
        require(affectedActorIds.all { id -> id > 0 }) { "Affected actor ids must be positive." }
        effects.forEach(ActiveEffectSnapshot::validateOrThrow)
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
