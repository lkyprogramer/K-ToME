package com.ktome.game

import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.resource.ResourcePool
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.resource.StaminaPools
import com.ktome.core.stats.StatsCalculator
import com.ktome.game.data.schema.ProfessionSchemaV2
import kotlin.math.roundToInt

internal object PlayerResourcePools {
    fun ensureInitialized(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
    ): ResourcePools {
        val pools = world.get<ResourcePools>(playerId) ?: ResourcePools().also { resourcePools -> world.add(playerId, resourcePools) }
        StaminaPools.pool(world, playerId)

        val mainType = mainResourceType(profession)
        if (mainType == ResourceType.STAMINA) {
            StaminaPools.syncComponentFromPool(world, playerId)
            return pools
        }

        if (pools.pool(mainType) == null) {
            pools.entries[mainType] =
                ResourcePool(
                    type = mainType,
                    current = startingCurrent(profession, mainType),
                    max = maxFor(world, playerId, profession, mainType),
                )
        }
        syncExisting(world, playerId, profession, pools)
        return pools
    }

    fun sync(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
    ): ResourcePools {
        val pools = ensureInitialized(world, playerId, profession)
        return syncExisting(world, playerId, profession, pools)
    }

    fun current(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
    ): ResourcePools = sync(world, playerId, profession)

    private fun syncExisting(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        pools: ResourcePools,
    ): ResourcePools {
        StaminaPools.syncTo(
            world = world,
            entityId = playerId,
            nextCurrent = StaminaPools.current(world, playerId),
            nextMax = maxFor(world, playerId, profession, ResourceType.STAMINA),
        )

        val mainType = mainResourceType(profession)
        if (mainType == ResourceType.STAMINA) {
            return pools
        }

        val pool = pools.getOrCreate(mainType, current = startingCurrent(profession, mainType), max = maxFor(world, playerId, profession, mainType))
        val nextMax = maxFor(world, playerId, profession, mainType)
        val delta = nextMax - pool.max
        pool.syncTo(nextCurrent = pool.current + delta, nextMax = nextMax)
        return pools
    }

    fun onTurnStart(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        inCombat: Boolean,
    ) {
        val pools = sync(world, playerId, profession)
        when (mainResourceType(profession)) {
            ResourceType.MANA -> pools.pool(ResourceType.MANA)?.restore(2)
            ResourceType.ENERGY -> pools.pool(ResourceType.ENERGY)?.restore(5)
            ResourceType.POSITIVE_ENERGY -> {
                if (!inCombat) {
                    pools.pool(ResourceType.POSITIVE_ENERGY)?.spend(5)
                }
            }
            else -> Unit
        }
    }

    fun onSuccessfulHit(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
    ) {
        val pools = sync(world, playerId, profession)
        when (mainResourceType(profession)) {
            ResourceType.ENERGY -> pools.pool(ResourceType.ENERGY)?.restore(8)
            ResourceType.POSITIVE_ENERGY -> pools.pool(ResourceType.POSITIVE_ENERGY)?.restore(3)
            else -> Unit
        }
    }

    fun onDamageTaken(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        damage: Int,
    ) {
        if (mainResourceType(profession) != ResourceType.POSITIVE_ENERGY) {
            return
        }
        val pools = sync(world, playerId, profession)
        val restored = (damage * 0.15f).roundToInt().coerceAtLeast(1)
        pools.pool(ResourceType.POSITIVE_ENERGY)?.restore(restored)
    }

    internal fun syncStaminaPoolFromComponent(
        world: World,
        playerId: EntityId,
        pools: ResourcePools = world.get<ResourcePools>(playerId) ?: ResourcePools().also { resourcePools -> world.add(playerId, resourcePools) },
    ): ResourcePool {
        val stamina = requireNotNull(world.get<Stamina>(playerId)) { "Missing Stamina for $playerId." }
        val derived = requireNotNull(world.get<DerivedStats>(playerId)) { "Missing DerivedStats for $playerId." }
        val pool = pools.getOrCreate(ResourceType.STAMINA, current = stamina.current, max = derived.maxStamina)
        return StaminaPools.syncTo(
            world = world,
            entityId = playerId,
            nextCurrent = pool.current,
            nextMax = derived.maxStamina,
        )
    }

    private fun mainResourceType(profession: ProfessionSchemaV2): ResourceType =
        ResourceType.fromId(profession.resourceType)

    private fun startingCurrent(
        profession: ProfessionSchemaV2,
        type: ResourceType,
    ): Int = profession.startingResources[type.name] ?: 0

    private fun maxFor(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        type: ResourceType,
    ): Int {
        val derived = requireNotNull(world.get<DerivedStats>(playerId)) { "Missing DerivedStats for $playerId." }
        return when (type) {
            ResourceType.STAMINA -> derived.maxStamina
            ResourceType.MANA -> 50 + StatsCalculator.effectiveStats(world, playerId).wil * 6
            ResourceType.ENERGY,
            ResourceType.POSITIVE_ENERGY,
            ResourceType.HATE,
            ResourceType.EQUILIBRIUM,
            -> profession.resourceCaps[type.name] ?: 100
        }
    }
}
