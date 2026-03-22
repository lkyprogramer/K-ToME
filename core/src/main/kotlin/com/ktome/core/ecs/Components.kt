package com.ktome.core.ecs

import com.ktome.core.combat.DamageType
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.map.Point

data class Position(var x: Int, var y: Int) {
    fun toPoint(): Point = Point(x, y)

    fun moveTo(point: Point) {
        x = point.x
        y = point.y
    }
}

data class Glyph(val value: Char)

data object PlayerControlled

data object MonsterControlled

data class Name(val value: String)

data class DisplayColor(val hex: String)

data class BlocksMovement(val value: Boolean = true)

enum class Faction {
    PLAYER,
    MONSTER,
}

data class FactionTag(val value: Faction)

data class Stats(
    var str: Int,
    var dex: Int,
    var con: Int,
    var wil: Int,
)

data class CombatProfile(
    val baseAttack: Int,
    val baseDefense: Int,
    val baseAccuracy: Int = 10,
    val baseEvasion: Int = 5,
    val baseSpeed: Int = 100,
    val baseHp: Int = 50,
    val baseStamina: Int = 40,
    val baseHpRegen: Double = 1.0,
)

data class ResistanceProfile(
    val values: MutableMap<DamageType, Int> = linkedMapOf(),
) {
    fun valueFor(type: DamageType): Int = values[type] ?: 0
}

data class DerivedStats(
    val attack: Int,
    val defense: Int,
    val accuracy: Int,
    val evasion: Int,
    val speed: Int,
    val critChance: Double,
    val maxHp: Int,
    val maxStamina: Int,
    val hpRegen: Double,
    val staminaRegen: Double,
    val talentPower: Double,
)

data class Health(
    var current: Int,
    var max: Int,
)

data class VisionRadius(val value: Int = 8)

data class AttackProfile(val range: Int = 1)

data class Energy(var current: Int = 0)

data class Experience(
    var current: Int = 0,
    var level: Int = 1,
    var unspentStatPoints: Int = 0,
    var unspentTalentPoints: Int = 0,
)

data class ExperienceReward(val value: Int)

enum class AIType {
    CHASE,
    KITE,
    PATROL,
}

data class AIBehavior(
    val type: AIType,
    val sightRadius: Int = 8,
    val preferredRangeStart: Int = 1,
    val preferredRangeEnd: Int = 1,
) {
    val preferredRange: IntRange
        get() = preferredRangeStart..preferredRangeEnd
}

data class AiTriggerTracker(
    val consumedTriggerIds: MutableSet<String> = linkedSetOf(),
    val pendingCombatStartTriggerIds: MutableSet<String> = linkedSetOf(),
    var engagedInCombat: Boolean = false,
)

data class MonsterTemplateId(val value: String)

data class PatrolRoute(
    val waypoints: List<Point>,
    var nextWaypointIndex: Int = 0,
) {
    init {
        require(waypoints.isNotEmpty()) { "Patrol routes must define at least one waypoint." }
        require(nextWaypointIndex in waypoints.indices) { "Patrol index must point to an existing waypoint." }
    }
}

data class Stair(val direction: StairDirection)

data class Interactable(val id: String)
