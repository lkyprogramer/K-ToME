package com.ktome.core.event

import com.ktome.core.ecs.EntityId
import com.ktome.core.movement.MoveBlockReason

sealed interface GameEvent

data class DamageDealtEvent(
    val attacker: EntityId,
    val target: EntityId,
    val damage: Int,
    val crit: Boolean,
    val ranged: Boolean = false,
) : GameEvent

data class MissEvent(
    val attacker: EntityId,
    val target: EntityId,
    val ranged: Boolean = false,
) : GameEvent

data class EntityDeathEvent(
    val entity: EntityId,
    val killer: EntityId?,
) : GameEvent

data class ExperienceGainedEvent(
    val entity: EntityId,
    val amount: Int,
) : GameEvent

data class LevelUpEvent(
    val entity: EntityId,
    val newLevel: Int,
    val unspentStatPoints: Int,
    val unspentTalentPoints: Int,
) : GameEvent

data class MovementBlockedEvent(
    val entity: EntityId,
    val reason: MoveBlockReason,
) : GameEvent
