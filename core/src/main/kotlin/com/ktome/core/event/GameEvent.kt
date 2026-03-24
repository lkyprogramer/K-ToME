package com.ktome.core.event

import com.ktome.core.ecs.EntityId
import com.ktome.core.movement.MoveBlockReason
import com.ktome.core.status.EffectCarrierKind
import com.ktome.core.status.StatusEffectType

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

sealed interface StatusEvent : GameEvent {
    val target: EntityId
    val statusType: StatusEffectType
    val statusId: String
}

data class StatusAppliedEvent(
    override val target: EntityId,
    override val statusType: StatusEffectType,
    override val statusId: String = statusType.schemaId,
    val source: EntityId? = null,
    val remainingTurns: Int,
) : StatusEvent

data class StatusRemovedEvent(
    override val target: EntityId,
    override val statusType: StatusEffectType,
    override val statusId: String = statusType.schemaId,
    val reason: String,
) : StatusEvent

data class StatusCleanseEvent(
    override val target: EntityId,
    override val statusType: StatusEffectType,
    override val statusId: String = statusType.schemaId,
    val reason: String = "CLEANSE",
) : StatusEvent

data class StatusTickEvent(
    override val target: EntityId,
    override val statusType: StatusEffectType,
    override val statusId: String = statusType.schemaId,
    val damage: Int,
    val carrierKind: EffectCarrierKind,
) : StatusEvent

data class TauntOverrideEvent(
    override val target: EntityId,
    override val statusType: StatusEffectType = StatusEffectType.TAUNT,
    override val statusId: String = statusType.schemaId,
    val previousSource: EntityId?,
    val newSource: EntityId?,
) : StatusEvent

data class StealthBrokenEvent(
    override val target: EntityId,
    override val statusType: StatusEffectType = StatusEffectType.STEALTH,
    override val statusId: String = statusType.schemaId,
    val damage: Int,
) : StatusEvent

data class StatusInteractionEvent(
    override val target: EntityId,
    override val statusType: StatusEffectType,
    override val statusId: String = statusType.schemaId,
    val interactionId: String,
) : StatusEvent
