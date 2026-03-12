package com.ktome.core.turn

import com.ktome.core.ecs.EntityId

data class TurnActorState(
    val entityId: EntityId,
    val speed: Int,
    val energy: Int,
)

data class TurnTickResult(
    val actionQueue: List<EntityId>,
    val remainingEnergy: Map<EntityId, Int>,
)

class TurnScheduler(
    private val actionThreshold: Int = 100,
) {
    private val entries = linkedMapOf<EntityId, SchedulerEntry>()

    fun addEntity(
        id: EntityId,
        speed: Int,
    ) {
        require(speed > 0) { "speed must be positive." }
        entries[id] = SchedulerEntry(speed = speed, energy = 0)
    }

    fun removeEntity(id: EntityId) {
        entries.remove(id)
    }

    fun updateSpeed(
        id: EntityId,
        speed: Int,
    ) {
        require(speed > 0) { "speed must be positive." }
        val entry = requireNotNull(entries[id]) { "Unknown scheduled entity: $id" }
        entries[id] = entry.copy(speed = speed)
    }

    fun tick(): EntityId {
        require(entries.isNotEmpty()) { "Cannot tick scheduler with no entities." }

        while (true) {
            val ready = tickAll()
            if (ready.isNotEmpty()) {
                return ready.first()
            }
        }
    }

    fun tickAll(): List<EntityId> {
        if (entries.isEmpty()) {
            return emptyList()
        }

        entries.keys.sortedBy(EntityId::value).forEach { entityId ->
            val current = requireNotNull(entries[entityId])
            entries[entityId] = current.copy(energy = current.energy + current.speed)
        }

        return buildList {
            while (true) {
                val dueThisWave = entries.entries
                    .filter { (_, entry) -> entry.energy >= actionThreshold }
                    .sortedBy { (entityId, _) -> entityId.value }

                if (dueThisWave.isEmpty()) {
                    return@buildList
                }

                dueThisWave.forEach { (entityId, entry) ->
                    entries[entityId] = entry.copy(energy = entry.energy - actionThreshold)
                    add(entityId)
                }
            }
        }
    }

    private data class SchedulerEntry(
        val speed: Int,
        val energy: Int,
    )

    companion object {
        fun tick(actorStates: List<TurnActorState>): TurnTickResult {
            val scheduler = TurnScheduler()
            actorStates.forEach { state ->
                scheduler.addEntity(state.entityId, state.speed)
                scheduler.entries[state.entityId] = SchedulerEntry(speed = state.speed, energy = state.energy)
            }

            return TurnTickResult(
                actionQueue = scheduler.tickAll(),
                remainingEnergy = scheduler.entries.mapValues { (_, value) -> value.energy },
            )
        }
    }
}
