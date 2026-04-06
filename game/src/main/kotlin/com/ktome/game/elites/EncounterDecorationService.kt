package com.ktome.game.elites

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.AiProfileOverride
import com.ktome.core.ecs.BossVariantRuntime
import com.ktome.core.ecs.EliteMutationLoadout
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.ResistanceProfile
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.item.StatModifier
import com.ktome.core.resource.ResourcePool
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.resource.StaminaPools
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusEffectDef
import com.ktome.core.status.StatusLifecycle
import com.ktome.core.status.StackingRule
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.TalentLoadout
import com.ktome.game.GameContent
import com.ktome.game.model.MonsterTemplate

enum class BossVariantSelectionMode {
    DISABLED,
    AUTO,
    FORCE_AVAILABLE,
}

internal data class SpawnDecorationRequest(
    val zoneId: String,
    val floorIndex: Int,
    val template: MonsterTemplate,
    val allowDoubleMutation: Boolean = false,
    val bossEncounterId: String? = null,
    val preferredBossVariantId: String? = null,
    val bossVariantSelectionMode: BossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
)

internal data class EncounterDecoration(
    val mutations: List<EliteMutationDef> = emptyList(),
    val bossVariant: BossVariantDef? = null,
)

internal class EncounterDecorationService(
    private val content: GameContent,
) {
    fun selectDecoration(
        request: SpawnDecorationRequest,
        nextIndex: (Int) -> Int,
    ): EncounterDecoration {
        val bossVariant =
            resolveBossVariant(
                request = request,
                nextIndex = nextIndex,
            )
        val mutations =
            if (bossVariant != null) {
                bossVariant.grantedMutations.mapNotNull { mutationRef -> content.eliteMutationRegistry.resolve(mutationRef.mutationId) }
            } else if (isEliteTemplate(request.template)) {
                content.eliteMutationRegistry.select(
                    context =
                        MutationSelectionContext(
                            zoneId = request.zoneId,
                            floorIndex = request.floorIndex,
                            applyToTags = request.template.tags.toSet(),
                            allowDoubleMutation = request.allowDoubleMutation,
                        ),
                    nextIndex = nextIndex,
                )
            } else {
                emptyList()
            }
        return EncounterDecoration(
            mutations = mutations,
            bossVariant = bossVariant,
        )
    }

    fun applyDecoration(
        world: World,
        entityId: EntityId,
        decoration: EncounterDecoration,
    ) {
        if (decoration.mutations.isEmpty() && decoration.bossVariant == null) {
            return
        }
        val loadout = world.get<EliteMutationLoadout>(entityId) ?: EliteMutationLoadout().also { mutationLoadout -> world.add(entityId, mutationLoadout) }
        decoration.mutations.forEach { mutation ->
            if (mutation.id !in loadout.mutationIds) {
                loadout.mutationIds += mutation.id
            }
            applyMutation(world, entityId, mutation)
        }
        if (decoration.mutations.isNotEmpty()) {
            applyMutationName(world, entityId, decoration.mutations)
        }
        decoration.bossVariant?.let { variant ->
            world.add(
                entityId,
                BossVariantRuntime(
                    variantId = variant.id,
                    baseEncounterId = variant.baseEncounterId,
                    threatCost = variant.threatCost,
                    lootProfileOverride = variant.lootProfileOverride,
                    visualTintKey = variant.visualTintKey,
                    actionWeightProfileId = variant.actionWeightProfileId,
                ),
            )
        }
    }

    private fun resolveBossVariant(
        request: SpawnDecorationRequest,
        nextIndex: (Int) -> Int,
    ): BossVariantDef? {
        val baseEncounterId = request.bossEncounterId ?: return null
        if (request.bossVariantSelectionMode == BossVariantSelectionMode.DISABLED) {
            return null
        }
        request.preferredBossVariantId?.let { preferredVariantId ->
            return requireNotNull(content.bossVariantRegistry.resolve(preferredVariantId)) {
                "Preferred boss variant '$preferredVariantId' is not registered."
            }
        }
        val variants = content.bossVariantRegistry.variantsFor(baseEncounterId)
        if (variants.isEmpty()) {
            return null
        }
        if (request.bossVariantSelectionMode == BossVariantSelectionMode.AUTO && nextIndex(2) == 0) {
            return null
        }
        return content.bossVariantRegistry.select(baseEncounterId, nextIndex)
    }

    private fun isEliteTemplate(template: MonsterTemplate): Boolean =
        "elite" in template.tags || template.lootProfileId.endsWith(".elite")

    private fun applyMutation(
        world: World,
        entityId: EntityId,
        mutation: EliteMutationDef,
    ) {
        mutation.statModifiers.forEach { modifierRef ->
            val modifier = requireNotNull(content.eliteMutationRegistry.modifier(modifierRef.modifierId)) {
                "Missing mutation stat modifier '${modifierRef.modifierId}'."
            }
            applyPermanentMutationModifier(world, entityId, mutation, modifier.statModifier)
            if (modifier.resistances.isNotEmpty()) {
                val resistanceProfile = world.get<ResistanceProfile>(entityId) ?: ResistanceProfile().also { profile -> world.add(entityId, profile) }
                modifier.resistances.forEach { (damageType, amount) ->
                    resistanceProfile.values[damageType] = (resistanceProfile.values[damageType] ?: 0) + amount
                }
            }
        }
        mutation.grantedTalents.forEach { grantRef ->
            grantTalent(world, entityId, grantRef.talentId)
        }
        mutation.aiProfileOverlay?.let { profileId ->
            world.add(entityId, AiProfileOverride(profileId))
        }
    }

    private fun applyPermanentMutationModifier(
        world: World,
        entityId: EntityId,
        mutation: EliteMutationDef,
        statModifier: StatModifier,
    ) {
        val tracker = world.get<EffectTracker>(entityId) ?: EffectTracker(ownerId = entityId).also { effectTracker -> world.add(entityId, effectTracker) }
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                definition =
                    StatusEffectDef(
                        id = "mutation:${mutation.id}",
                        type = StatusEffectType.CUSTOM,
                        nameKey = mutation.nameKey,
                        iconKey = mutation.iconKey,
                        stackingRule = StackingRule.REFRESH_DURATION,
                        dispellable = false,
                        statModifier = statModifier,
                    ),
                effectId = "mutation:${mutation.id}",
                duration = 9999,
            ),
        )
    }

    private fun grantTalent(
        world: World,
        entityId: EntityId,
        talentId: String,
    ) {
        val loadout = world.get<TalentLoadout>(entityId) ?: TalentLoadout().also { talentLoadout -> world.add(entityId, talentLoadout) }
        if (talentId !in loadout.talentLevels) {
            val nextSlot = ((loadout.slotToTalentId.keys.maxOrNull() ?: 0) + 1).coerceAtLeast(1)
            loadout.slotToTalentId[nextSlot] = talentId
            loadout.talentLevels[talentId] = 1
        }
        if (world.get<CooldownState>(entityId) == null) {
            world.add(entityId, CooldownState())
        }
        ensureMutationTalentPools(world, entityId)
    }

    private fun ensureMutationTalentPools(
        world: World,
        entityId: EntityId,
    ) {
        val pools = world.get<ResourcePools>(entityId) ?: ResourcePools().also { resourcePools -> world.add(entityId, resourcePools) }
        val staminaMax = world.get<com.ktome.core.ecs.DerivedStats>(entityId)?.maxStamina ?: 40
        StaminaPools.ensurePool(world, entityId, current = staminaMax, max = staminaMax)
        linkedMapOf(
            ResourceType.STAMINA to staminaMax,
            ResourceType.MANA to 120,
            ResourceType.ENERGY to 100,
            ResourceType.POSITIVE_ENERGY to 100,
            ResourceType.HATE to 100,
            ResourceType.EQUILIBRIUM to 100,
        ).forEach { (type, max) ->
            if (pools.pool(type) == null) {
                pools.entries[type] =
                    ResourcePool(
                        type = type,
                        current = if (type == ResourceType.EQUILIBRIUM) max / 2 else max,
                        max = max,
                    )
            }
        }
    }

    private fun applyMutationName(
        world: World,
        entityId: EntityId,
        mutations: List<EliteMutationDef>,
    ) {
        val name = world.get<Name>(entityId) ?: return
        val mutationPrefix =
            mutations
                .map { mutation -> content.localizer.text(mutation.nameKey) }
                .distinct()
                .joinToString(separator = " ")
        world.add(entityId, name.copy(value = "$mutationPrefix ${name.value}".trim()))
    }
}
