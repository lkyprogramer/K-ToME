package com.ktome.game

import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.resource.DecayPolicy
import com.ktome.core.resource.EquilibriumAffinity
import com.ktome.core.resource.EquilibriumState
import com.ktome.core.resource.ResourceAxis
import com.ktome.core.resource.ResourcePool
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceProfileRef
import com.ktome.core.resource.ResourceRegenProfile
import com.ktome.core.resource.ResourceType
import com.ktome.core.resource.StaminaPools
import com.ktome.core.stats.StatsCalculator
import com.ktome.game.data.schema.ProfessionSchemaV2
import kotlin.math.roundToInt

internal object PlayerResourceService {
    private const val EQUILIBRIUM_SHIFT_PER_TURN: Int = 10

    fun ensureInitialized(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
    ): ResourcePools {
        val pools = world.get<ResourcePools>(playerId) ?: ResourcePools().also { resourcePools -> world.add(playerId, resourcePools) }
        profession.resourceProfiles.forEach { profile ->
            ensurePool(world, playerId, profession, pools, profile)
        }
        if (profession.stateAxis == ResourceAxis.EQUILIBRIUM && world.get<EquilibriumState>(playerId) == null) {
            world.add(playerId, EquilibriumState())
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

    fun onTurnStart(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        inCombat: Boolean,
    ) {
        val pools = sync(world, playerId, profession)
        profession.resourceProfiles.forEach { profile ->
            applyTurnStartProfile(world, playerId, pools, profile, inCombat)
        }
        applyEquilibriumShift(world, playerId, profession, pools)
    }

    fun onSuccessfulHit(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
    ) {
        val pools = sync(world, playerId, profession)
        profession.resourceProfiles.forEach { profile ->
            applyOnHit(profile, pools)
        }
    }

    fun onDamageTaken(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        damage: Int,
    ) {
        if (damage <= 0) {
            return
        }
        val pools = sync(world, playerId, profession)
        profession.resourceProfiles.forEach { profile ->
            applyOnDamageTaken(profile, pools, damage)
        }
    }

    fun onKill(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
    ) {
        val pools = sync(world, playerId, profession)
        profession.resourceProfiles.forEach { profile ->
            applyOnKill(profile, pools)
        }
    }

    fun recordSuccessfulAffinity(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        affinity: EquilibriumAffinity,
    ) {
        if (affinity == EquilibriumAffinity.NEUTRAL || profession.stateAxis != ResourceAxis.EQUILIBRIUM) {
            return
        }
        val state = world.get<EquilibriumState>(playerId) ?: EquilibriumState().also { world.add(playerId, it) }
        state.lastResolvedAffinity = affinity
    }

    private fun syncExisting(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        pools: ResourcePools,
    ): ResourcePools {
        profession.resourceProfiles.forEach { profile ->
            val type = profile.resourceType ?: return@forEach
            val nextMax = maxFor(world, playerId, profession, type)
            if (type == ResourceType.STAMINA && !StaminaPools.hasPool(world, playerId)) {
                StaminaPools.ensurePool(world, playerId, current = profile.initialCurrent.coerceAtMost(nextMax), max = nextMax)
            }
            val pool =
                pools.getOrCreate(
                    type = type,
                    current = profile.initialCurrent,
                    max = nextMax,
                )
            val delta = nextMax - pool.max
            val nextCurrent =
                if (pool.max == 0 && pool.current == 0 && type == ResourceType.EQUILIBRIUM) {
                    profile.initialCurrent
                } else {
                    pool.current + delta
                }
            pool.syncTo(nextCurrent = nextCurrent, nextMax = nextMax)
        }
        return pools
    }

    private fun ensurePool(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        pools: ResourcePools,
        profile: ResourceProfileRef,
    ) {
        val type = profile.resourceType ?: return
        val max = maxFor(world, playerId, profession, type)
        if (type == ResourceType.STAMINA && !StaminaPools.hasPool(world, playerId)) {
            StaminaPools.ensurePool(world, playerId, current = profile.initialCurrent.coerceAtMost(max), max = max)
            return
        }
        if (pools.pool(type) == null) {
            pools.entries[type] =
                ResourcePool(
                    type = type,
                    current = profile.initialCurrent.coerceIn(0, max),
                    max = max,
                )
        }
    }

    private fun applyTurnStartProfile(
        world: World,
        playerId: EntityId,
        pools: ResourcePools,
        profile: ResourceProfileRef,
        inCombat: Boolean,
    ) {
        when (val regen = profile.regenProfile) {
            is ResourceRegenProfile.PerTurn -> applyPerTurn(profile, pools, regen.amount)
            is ResourceRegenProfile.Composite -> regen.entries.forEach { entry -> applyTurnStartEntry(world, playerId, pools, profile, entry, inCombat) }
            is ResourceRegenProfile.Decay -> applyDecay(profile, pools, regen.policy, inCombat)
            else -> Unit
        }
    }

    private fun applyTurnStartEntry(
        world: World,
        playerId: EntityId,
        pools: ResourcePools,
        profile: ResourceProfileRef,
        entry: ResourceRegenProfile,
        inCombat: Boolean,
    ) {
        when (entry) {
            is ResourceRegenProfile.PerTurn -> applyPerTurn(profile, pools, entry.amount)
            is ResourceRegenProfile.Decay -> applyDecay(profile, pools, entry.policy, inCombat)
            is ResourceRegenProfile.Composite -> entry.entries.forEach { nested -> applyTurnStartEntry(world, playerId, pools, profile, nested, inCombat) }
            is ResourceRegenProfile.None,
            is ResourceRegenProfile.OnDamageTaken,
            is ResourceRegenProfile.OnHit,
            is ResourceRegenProfile.OnKill,
            -> Unit
        }
    }

    private fun applyOnHit(
        profile: ResourceProfileRef,
        pools: ResourcePools,
    ) {
        when (val regen = profile.regenProfile) {
            is ResourceRegenProfile.OnHit -> restore(profile, pools, regen.amount)
            is ResourceRegenProfile.Composite -> regen.entries.forEach { entry -> applyOnHitEntry(profile, pools, entry) }
            else -> Unit
        }
    }

    private fun applyOnHitEntry(
        profile: ResourceProfileRef,
        pools: ResourcePools,
        entry: ResourceRegenProfile,
    ) {
        when (entry) {
            is ResourceRegenProfile.OnHit -> restore(profile, pools, entry.amount)
            is ResourceRegenProfile.Composite -> entry.entries.forEach { nested -> applyOnHitEntry(profile, pools, nested) }
            else -> Unit
        }
    }

    private fun applyOnDamageTaken(
        profile: ResourceProfileRef,
        pools: ResourcePools,
        damage: Int,
    ) {
        when (val regen = profile.regenProfile) {
            is ResourceRegenProfile.OnDamageTaken -> restorePercent(profile, pools, damage, regen.percent)
            is ResourceRegenProfile.Composite -> regen.entries.forEach { entry -> applyOnDamageTakenEntry(profile, pools, damage, entry) }
            else -> Unit
        }
    }

    private fun applyOnDamageTakenEntry(
        profile: ResourceProfileRef,
        pools: ResourcePools,
        damage: Int,
        entry: ResourceRegenProfile,
    ) {
        when (entry) {
            is ResourceRegenProfile.OnDamageTaken -> restorePercent(profile, pools, damage, entry.percent)
            is ResourceRegenProfile.Composite -> entry.entries.forEach { nested -> applyOnDamageTakenEntry(profile, pools, damage, nested) }
            else -> Unit
        }
    }

    private fun applyOnKill(
        profile: ResourceProfileRef,
        pools: ResourcePools,
    ) {
        when (val regen = profile.regenProfile) {
            is ResourceRegenProfile.OnKill -> restore(profile, pools, regen.amount)
            is ResourceRegenProfile.Composite -> regen.entries.forEach { entry -> applyOnKillEntry(profile, pools, entry) }
            else -> Unit
        }
    }

    private fun applyOnKillEntry(
        profile: ResourceProfileRef,
        pools: ResourcePools,
        entry: ResourceRegenProfile,
    ) {
        when (entry) {
            is ResourceRegenProfile.OnKill -> restore(profile, pools, entry.amount)
            is ResourceRegenProfile.Composite -> entry.entries.forEach { nested -> applyOnKillEntry(profile, pools, nested) }
            else -> Unit
        }
    }

    private fun applyPerTurn(
        profile: ResourceProfileRef,
        pools: ResourcePools,
        amount: Int,
    ) {
        if (profile.resourceType == ResourceType.STAMINA) {
            return
        }
        restore(profile, pools, amount)
    }

    private fun applyDecay(
        profile: ResourceProfileRef,
        pools: ResourcePools,
        policy: DecayPolicy,
        inCombat: Boolean,
    ) {
        if (policy.outOfCombatOnly && inCombat) {
            return
        }
        val type = profile.resourceType ?: return
        pools.pool(type)?.spend(policy.amountPerTurn)
    }

    private fun restore(
        profile: ResourceProfileRef,
        pools: ResourcePools,
        amount: Int,
    ) {
        if (amount <= 0) {
            return
        }
        val type = profile.resourceType ?: return
        pools.pool(type)?.restore(amount)
    }

    private fun restorePercent(
        profile: ResourceProfileRef,
        pools: ResourcePools,
        damage: Int,
        percent: Double,
    ) {
        if (damage <= 0 || percent <= 0.0) {
            return
        }
        restore(profile, pools, (damage * percent).roundToInt().coerceAtLeast(1))
    }

    private fun applyEquilibriumShift(
        world: World,
        playerId: EntityId,
        profession: ProfessionSchemaV2,
        pools: ResourcePools,
    ) {
        if (profession.stateAxis != ResourceAxis.EQUILIBRIUM) {
            return
        }
        val pool = pools.pool(ResourceType.EQUILIBRIUM) ?: return
        val state = world.get<EquilibriumState>(playerId) ?: EquilibriumState().also { world.add(playerId, it) }
        when (state.lastResolvedAffinity) {
            EquilibriumAffinity.PHYSICAL -> pool.spend(EQUILIBRIUM_SHIFT_PER_TURN)
            EquilibriumAffinity.ARCANE -> pool.restore(EQUILIBRIUM_SHIFT_PER_TURN)
            EquilibriumAffinity.NEUTRAL -> Unit
        }
        state.lastResolvedAffinity = EquilibriumAffinity.NEUTRAL
    }

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
