package com.ktome.core.ai

import com.ktome.core.ecs.EntityId
import com.ktome.core.map.Point
import com.ktome.core.status.StatusEffectType
import com.ktome.core.talent.EffectTracker

object StealthTauntHandler {
    fun resolveTargetId(
        effectTracker: EffectTracker?,
        fallbackTargetId: EntityId,
        isAlive: (EntityId) -> Boolean,
    ): EntityId =
        effectTracker
            ?.activeEffects()
            ?.firstOrNull { effect -> effect.type == StatusEffectType.TAUNT }
            ?.sourceEntityId
            ?.takeIf(isAlive)
            ?: fallbackTargetId

    fun isTargetVisible(
        effectTracker: EffectTracker?,
        targetPosition: Point?,
        visibleTiles: Set<Point>,
    ): Boolean {
        if (effectTracker?.has(StatusEffectType.STEALTH) == true) {
            return false
        }
        return targetPosition != null && targetPosition in visibleTiles
    }

    fun rememberLastKnownTargetPosition(
        perception: AIPerceptionState?,
        targetVisible: Boolean,
        targetPosition: Point,
    ) {
        if (targetVisible) {
            perception?.lastKnownTargetPosition = targetPosition
        }
    }

    fun consumeLastKnownTargetPosition(
        perception: AIPerceptionState?,
        currentPosition: Point,
        useLastKnownPosition: Boolean,
    ): Point? {
        if (!useLastKnownPosition) {
            return null
        }
        val lastKnownTargetPosition = perception?.lastKnownTargetPosition ?: return null
        if (currentPosition == lastKnownTargetPosition) {
            perception.lastKnownTargetPosition = null
            return null
        }
        return lastKnownTargetPosition
    }
}
