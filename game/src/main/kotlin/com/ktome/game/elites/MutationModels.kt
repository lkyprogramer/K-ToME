package com.ktome.game.elites

import com.ktome.core.combat.DamageType
import com.ktome.core.item.StatModifier
import com.ktome.core.mapgen.TerrainTag

enum class MutationKind {
    STAT_PACKAGE,
    ABILITY_GRANT,
    AURA,
    AI_SHIFT,
    ELEMENT_PACKAGE,
}

enum class MutationTier {
    MINOR,
    MAJOR,
    SIGNATURE,
}

data class StatModifierRef(
    val modifierId: String,
) {
    init {
        require(modifierId.isNotBlank()) { "StatModifierRef.modifierId must not be blank." }
    }
}

data class TalentGrantRef(
    val talentId: String,
) {
    init {
        require(talentId.isNotBlank()) { "TalentGrantRef.talentId must not be blank." }
    }
}

data class MutationRef(
    val mutationId: String,
) {
    init {
        require(mutationId.isNotBlank()) { "MutationRef.mutationId must not be blank." }
    }
}

data class MutationStatModifierDef(
    val id: String,
    val statModifier: StatModifier = StatModifier.ZERO,
    val resistances: Map<DamageType, Int> = emptyMap(),
) {
    init {
        require(id.isNotBlank()) { "MutationStatModifierDef.id must not be blank." }
    }
}

data class EliteMutationDef(
    val id: String,
    val kind: MutationKind,
    val tier: MutationTier,
    val threatCost: Int,
    val nameKey: String,
    val iconKey: String,
    val applyToTags: Set<String>,
    val minFloor: Int,
    val maxFloor: Int?,
    val allowedZones: Set<String>,
    val preferredTerrainTags: List<TerrainTag> = emptyList(),
    val statModifiers: List<StatModifierRef>,
    val grantedTalents: List<TalentGrantRef>,
    val aiProfileOverlay: String?,
    val incompatibleWith: Set<String>,
    val auraStatusId: String? = null,
    val auraRadius: Int = 0,
    val auraDuration: Int = 1,
    val auraMagnitude: Double = 0.0,
) {
    init {
        require(id.isNotBlank()) { "EliteMutationDef.id must not be blank." }
        require(threatCost >= 0) { "EliteMutationDef.threatCost must not be negative." }
        require(nameKey.isNotBlank()) { "EliteMutationDef.nameKey must not be blank." }
        require(iconKey.isNotBlank()) { "EliteMutationDef.iconKey must not be blank." }
        require(minFloor > 0) { "EliteMutationDef.minFloor must be positive." }
        require(maxFloor == null || maxFloor >= minFloor) { "EliteMutationDef.maxFloor must be >= minFloor when present." }
        require(applyToTags.all(String::isNotBlank)) { "EliteMutationDef.applyToTags must not contain blanks." }
        require(allowedZones.all(String::isNotBlank)) { "EliteMutationDef.allowedZones must not contain blanks." }
        require(preferredTerrainTags.size == preferredTerrainTags.distinct().size) {
            "EliteMutationDef.preferredTerrainTags must not contain duplicates."
        }
        require(aiProfileOverlay == null || aiProfileOverlay.isNotBlank()) {
            "EliteMutationDef.aiProfileOverlay must not be blank when present."
        }
        require(incompatibleWith.all(String::isNotBlank)) { "EliteMutationDef.incompatibleWith must not contain blanks." }
        require(auraStatusId == null || auraStatusId.isNotBlank()) { "EliteMutationDef.auraStatusId must not be blank when present." }
        require(auraRadius >= 0) { "EliteMutationDef.auraRadius must not be negative." }
        require(auraDuration > 0) { "EliteMutationDef.auraDuration must be positive." }
    }
}

data class EliteMutationConfig(
    val maxMutationsPerElite: Int = 2,
) {
    init {
        require(maxMutationsPerElite == 2) {
            "EliteMutationConfig.maxMutationsPerElite is frozen to 2 in Phase 4."
        }
    }
}

data class BossVariantDef(
    val id: String,
    val baseEncounterId: String,
    val grantedMutations: List<MutationRef>,
    val threatCost: Int,
    val lootProfileOverride: String?,
    val visualTintKey: String?,
    val actionWeightProfileId: String? = null,
) {
    init {
        require(id.isNotBlank()) { "BossVariantDef.id must not be blank." }
        require(baseEncounterId.isNotBlank()) { "BossVariantDef.baseEncounterId must not be blank." }
        require(threatCost >= 0) { "BossVariantDef.threatCost must not be negative." }
        require(lootProfileOverride == null || lootProfileOverride.isNotBlank()) {
            "BossVariantDef.lootProfileOverride must not be blank when present."
        }
        require(visualTintKey == null || visualTintKey.isNotBlank()) {
            "BossVariantDef.visualTintKey must not be blank when present."
        }
        require(actionWeightProfileId == null || actionWeightProfileId.isNotBlank()) {
            "BossVariantDef.actionWeightProfileId must not be blank when present."
        }
    }
}

data class ActionWeightProfileDef(
    val id: String,
    val actionWeights: Map<String, Double>,
) {
    init {
        require(id.isNotBlank()) { "ActionWeightProfileDef.id must not be blank." }
        require(actionWeights.keys.all(String::isNotBlank)) { "ActionWeightProfileDef.actionWeights keys must not be blank." }
        require(actionWeights.values.all { weight -> weight >= 0.0 }) {
            "ActionWeightProfileDef.actionWeights values must not be negative."
        }
    }
}

data class MutationSelectionContext(
    val zoneId: String,
    val floorIndex: Int,
    val applyToTags: Set<String>,
    val allowDoubleMutation: Boolean = false,
) {
    init {
        require(zoneId.isNotBlank()) { "MutationSelectionContext.zoneId must not be blank." }
        require(floorIndex > 0) { "MutationSelectionContext.floorIndex must be positive." }
        require(applyToTags.all(String::isNotBlank)) { "MutationSelectionContext.applyToTags must not contain blanks." }
    }
}

class EliteMutationRegistry(
    val config: EliteMutationConfig,
    val statModifiersById: Map<String, MutationStatModifierDef>,
    private val definitionsById: Map<String, EliteMutationDef>,
) {
    private companion object {
        val HIGH_PRESSURE_ZONES: Set<String> =
            setOf(
                "molten_core",
                "grey_gate_depths",
                "abyssal_temple",
                "abyssal_heart",
            )
        // Keep terrain-affinity packages slightly above same-tier generic majors so Path B
        // can manifest in real combat, without fully crowding out non-affinity elites.
        const val PREFERRED_TERRAIN_WEIGHT_BONUS: Int = 4
    }

    init {
        definitionsById.values.forEach { definition ->
            definition.statModifiers.forEach { ref ->
                require(statModifiersById.containsKey(ref.modifierId)) {
                    "Elite mutation '${definition.id}' references unknown stat modifier '${ref.modifierId}'."
                }
            }
            definition.incompatibleWith.forEach { incompatibleId ->
                require(definitionsById.containsKey(incompatibleId)) {
                    "Elite mutation '${definition.id}' references unknown incompatible mutation '$incompatibleId'."
                }
            }
        }
    }

    fun all(): List<EliteMutationDef> = definitionsById.values.sortedBy(EliteMutationDef::id)

    fun resolve(id: String): EliteMutationDef? = definitionsById[id]

    fun modifier(id: String): MutationStatModifierDef? = statModifiersById[id]

    fun select(
        context: MutationSelectionContext,
        nextIndex: (Int) -> Int,
    ): List<EliteMutationDef> {
        val selected = mutableListOf<EliteMutationDef>()
        var candidates =
            all().filter { definition ->
                definition.minFloor <= context.floorIndex &&
                    (definition.maxFloor == null || context.floorIndex <= definition.maxFloor) &&
                    (definition.allowedZones.isEmpty() || context.zoneId in definition.allowedZones) &&
                    context.applyToTags.any(definition.applyToTags::contains)
            }
        if (candidates.isEmpty()) {
            return emptyList()
        }
        val desiredCount = if (context.allowDoubleMutation) config.maxMutationsPerElite else 1
        while (selected.size < desiredCount && candidates.isNotEmpty()) {
            val weightedCandidates =
                candidates.map { definition ->
                    definition to (tierWeight(definition.tier, context) + terrainAffinityWeightBonus(definition, context))
                }
            val totalWeight = weightedCandidates.sumOf { (_, weight) -> weight.coerceAtLeast(0) }
            if (totalWeight <= 0) {
                break
            }
            var roll = nextIndex(totalWeight)
            val chosen =
                weightedCandidates.firstOrNull { (_, weight) ->
                    roll -= weight
                    roll < 0
                }?.first ?: weightedCandidates.last().first
            selected += chosen
            candidates =
                candidates.filter { candidate ->
                    candidate.id != chosen.id &&
                        candidate.id !in chosen.incompatibleWith &&
                        chosen.id !in candidate.incompatibleWith &&
                        !(candidate.tier == MutationTier.SIGNATURE && selected.any { mutation -> mutation.tier == MutationTier.SIGNATURE })
                }
        }
        return selected
    }

    private fun tierWeight(
        tier: MutationTier,
        context: MutationSelectionContext,
    ): Int {
        val floorProgress = (context.floorIndex - 1).coerceIn(0, 3)
        val zoneProgress = if (context.zoneId in HIGH_PRESSURE_ZONES) 1 else 0
        val progression = floorProgress + zoneProgress
        return when (tier) {
            MutationTier.MINOR -> (6 - progression).coerceAtLeast(2)
            MutationTier.MAJOR -> 3 + progression
            MutationTier.SIGNATURE ->
                if (context.floorIndex < 4) {
                    0
                } else {
                    1 + progression
                }
        }
    }

    private fun terrainAffinityWeightBonus(
        definition: EliteMutationDef,
        context: MutationSelectionContext,
    ): Int =
        if (definition.preferredTerrainTags.isEmpty()) {
            0
        } else if (definition.allowedZones.isNotEmpty() && context.zoneId !in definition.allowedZones) {
            0
        } else {
            PREFERRED_TERRAIN_WEIGHT_BONUS
        }
}

class BossVariantRegistry(
    private val variantsByBaseEncounterId: Map<String, List<BossVariantDef>>,
    private val variantsById: Map<String, BossVariantDef>,
) {
    fun all(): List<BossVariantDef> = variantsById.values.sortedBy(BossVariantDef::id)

    fun resolve(id: String): BossVariantDef? = variantsById[id]

    fun variantsFor(baseEncounterId: String): List<BossVariantDef> =
        variantsByBaseEncounterId[baseEncounterId].orEmpty().sortedBy(BossVariantDef::id)

    fun select(
        baseEncounterId: String,
        nextIndex: (Int) -> Int,
    ): BossVariantDef? {
        val variants = variantsFor(baseEncounterId)
        if (variants.isEmpty()) {
            return null
        }
        return variants[nextIndex(variants.size)]
    }
}
