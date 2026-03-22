package com.ktome.core.resource

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get

object StaminaPools {
    fun hasPool(
        world: World,
        entityId: EntityId,
    ): Boolean = world.get<ResourcePools>(entityId)?.pool(ResourceType.STAMINA) != null

    fun ensurePool(
        world: World,
        entityId: EntityId,
        current: Int,
        max: Int,
    ): ResourcePool {
        val pools = world.get<ResourcePools>(entityId) ?: ResourcePools().also { resourcePools -> world.add(entityId, resourcePools) }
        return pools.pool(ResourceType.STAMINA)
            ?: ResourcePool(
                type = ResourceType.STAMINA,
                current = current.coerceIn(0, max.coerceAtLeast(0)),
                max = max.coerceAtLeast(0),
            ).also { pool ->
                pools.entries[ResourceType.STAMINA] = pool
            }
    }

    fun pool(
        world: World,
        entityId: EntityId,
    ): ResourcePool =
        requireNotNull(world.get<ResourcePools>(entityId)?.pool(ResourceType.STAMINA)) {
            "Missing STAMINA ResourcePool for $entityId"
        }

    fun current(
        world: World,
        entityId: EntityId,
    ): Int = pool(world, entityId).current

    fun max(
        world: World,
        entityId: EntityId,
    ): Int = pool(world, entityId).max

    fun spend(
        world: World,
        entityId: EntityId,
        amount: Int,
    ): Int {
        val pool = pool(world, entityId)
        val before = pool.current
        pool.spend(amount)
        return before - pool.current
    }

    fun restore(
        world: World,
        entityId: EntityId,
        amount: Int,
    ): Int {
        val pool = pool(world, entityId)
        val before = pool.current
        pool.restore(amount)
        return pool.current - before
    }

    fun syncTo(
        world: World,
        entityId: EntityId,
        nextCurrent: Int,
        nextMax: Int,
    ): ResourcePool {
        val pool = pool(world, entityId)
        pool.syncTo(nextCurrent = nextCurrent, nextMax = nextMax)
        return pool
    }

    fun shiftMax(
        world: World,
        entityId: EntityId,
        nextMax: Int,
    ): ResourcePool {
        val pool = pool(world, entityId)
        val delta = nextMax - pool.max
        return syncTo(
            world = world,
            entityId = entityId,
            nextCurrent = pool.current + delta,
            nextMax = nextMax,
        )
    }

    fun restoreToMax(
        world: World,
        entityId: EntityId,
    ): ResourcePool {
        val pool = pool(world, entityId)
        return syncTo(
            world = world,
            entityId = entityId,
            nextCurrent = pool.max,
            nextMax = pool.max,
        )
    }
}
