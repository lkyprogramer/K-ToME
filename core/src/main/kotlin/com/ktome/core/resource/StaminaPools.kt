package com.ktome.core.resource

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get

object StaminaPools {
    fun pool(
        world: World,
        entityId: EntityId,
    ): ResourcePool {
        val pools = world.get<ResourcePools>(entityId) ?: ResourcePools().also { resourcePools -> world.add(entityId, resourcePools) }
        return pools.pool(ResourceType.STAMINA) ?: seedFromComponent(world, entityId, pools)
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
        syncComponentFromPool(world, entityId, pool)
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
        syncComponentFromPool(world, entityId, pool)
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
        return syncComponentFromPool(world, entityId, pool)
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

    fun syncComponentFromPool(
        world: World,
        entityId: EntityId,
    ): ResourcePool = syncComponentFromPool(world, entityId, pool(world, entityId))

    private fun seedFromComponent(
        world: World,
        entityId: EntityId,
        pools: ResourcePools,
    ): ResourcePool {
        val stamina = requireNotNull(world.get<Stamina>(entityId)) { "Missing Stamina for $entityId" }
        val pool = pools.getOrCreate(ResourceType.STAMINA, current = stamina.current, max = stamina.max)
        return syncComponentFromPool(world, entityId, pool)
    }

    private fun syncComponentFromPool(
        world: World,
        entityId: EntityId,
        pool: ResourcePool,
    ): ResourcePool {
        val stamina = world.get<Stamina>(entityId)
        if (stamina == null) {
            world.add(entityId, Stamina(current = pool.current, max = pool.max))
            return pool
        }

        stamina.max = pool.max
        stamina.current = pool.current.coerceIn(0, pool.max)
        return pool
    }
}
