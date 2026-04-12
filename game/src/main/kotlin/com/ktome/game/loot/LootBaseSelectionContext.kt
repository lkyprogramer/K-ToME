package com.ktome.game.loot

import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.ItemBaseDef

private const val NO_BUILD_MATCH_MULTIPLIER_BPS: Int = 10_000
private const val WEAK_BUILD_MATCH_MULTIPLIER_BPS: Int = 11_500
private const val STRONG_BUILD_MATCH_MULTIPLIER_BPS: Int = 13_500
private const val DEFAULT_ANTI_COLLAPSE_MULTIPLIER_BPS: Int = 10_000
private const val DOMINANT_RISK_MISMATCH_MULTIPLIER_BPS: Int = 7_500

private val NON_SIGNAL_BASE_SELECTION_TAGS: Set<String> =
    setOf(
        "item",
        "weapon",
        "armor",
        "consumable",
        "unique",
        "artifact",
        "reward",
        "optional",
        "late",
        "dominant_risk",
    )

data class LootBaseSelectionContext(
    val buildTags: Set<String> = emptySet(),
) {
    init {
        require(buildTags.none(String::isBlank)) { "LootBaseSelectionContext.buildTags must not contain blanks." }
    }

    val normalizedBuildTags: Set<String> = buildTags.mapTo(linkedSetOf(), ::normalizeLootBaseSelectionTag)

    val hasSignalContext: Boolean
        get() = normalizedBuildTags.isNotEmpty()

    companion object {
        val EMPTY: LootBaseSelectionContext = LootBaseSelectionContext()
    }
}

internal enum class LootBaseBuildMatchStrength {
    NONE,
    WEAK,
    STRONG,
}

internal data class LootBaseSelectionEvaluation(
    val semanticTags: Set<String>,
    val matchedBuildTags: Set<String>,
    val matchStrength: LootBaseBuildMatchStrength,
    val buildTagMatchMultiplierBasisPoints: Int,
    val antiCollapseMultiplierBasisPoints: Int,
)

internal fun LootBaseSelectionContext.evaluate(base: ItemBaseDef): LootBaseSelectionEvaluation {
    val rawSemanticTags = lootBaseSemanticTags(base)
    val semanticTags =
        rawSemanticTags
            .filterNot { tag -> tag in NON_SIGNAL_BASE_SELECTION_TAGS }
            .toCollection(linkedSetOf())
    if (!hasSignalContext || semanticTags.isEmpty()) {
        return LootBaseSelectionEvaluation(
            semanticTags = semanticTags,
            matchedBuildTags = emptySet(),
            matchStrength = LootBaseBuildMatchStrength.NONE,
            buildTagMatchMultiplierBasisPoints = NO_BUILD_MATCH_MULTIPLIER_BPS,
            antiCollapseMultiplierBasisPoints = DEFAULT_ANTI_COLLAPSE_MULTIPLIER_BPS,
        )
    }
    val matchedBuildTags = semanticTags.intersect(normalizedBuildTags)
    val matchStrength =
        when {
            matchedBuildTags.size >= 2 -> LootBaseBuildMatchStrength.STRONG
            matchedBuildTags.size == 1 -> LootBaseBuildMatchStrength.WEAK
            else -> LootBaseBuildMatchStrength.NONE
        }
    val buildTagMatchMultiplierBasisPoints =
        when (matchStrength) {
            LootBaseBuildMatchStrength.NONE -> NO_BUILD_MATCH_MULTIPLIER_BPS
            LootBaseBuildMatchStrength.WEAK -> WEAK_BUILD_MATCH_MULTIPLIER_BPS
            LootBaseBuildMatchStrength.STRONG -> STRONG_BUILD_MATCH_MULTIPLIER_BPS
        }
    val antiCollapseMultiplierBasisPoints =
        if ("dominant_risk" in rawSemanticTags && matchStrength == LootBaseBuildMatchStrength.NONE) {
            DOMINANT_RISK_MISMATCH_MULTIPLIER_BPS
        } else {
            DEFAULT_ANTI_COLLAPSE_MULTIPLIER_BPS
        }
    return LootBaseSelectionEvaluation(
        semanticTags = semanticTags,
        matchedBuildTags = matchedBuildTags,
        matchStrength = matchStrength,
        buildTagMatchMultiplierBasisPoints = buildTagMatchMultiplierBasisPoints,
        antiCollapseMultiplierBasisPoints = antiCollapseMultiplierBasisPoints,
    )
}

internal fun lootBaseSemanticTags(base: ItemBaseDef): Set<String> =
    linkedSetOf<String>().apply {
        addAll(base.tags.map(::normalizeLootBaseSelectionTag))
        base.resourceTypeId?.let { resourceTypeId ->
            add(normalizeLootBaseSelectionTag(resourceTypeId))
        }
        if (base.baseStats.attack > 0) {
            add("offense")
        }
        if (base.baseStats.defense > 0 || base.baseStats.maxHp > 0) {
            addAll(listOf("protection", "defense"))
        }
        if (base.baseStats.evasion > 0 || base.baseStats.speed > 0) {
            add("mobility")
        }
        if (base.baseStats.talentPower > 0.0 || base.baseStats.wil > 0) {
            add("spell")
        }
        when (val passive = base.passive) {
            is EquipmentPassive.OnHitStatusProc -> {
                add(normalizeLootBaseSelectionTag(passive.statusId))
                add("offense")
            }
            is EquipmentPassive.OnKillResourceRestore -> {
                add(normalizeLootBaseSelectionTag(passive.resourceType.name))
                add("sustain")
            }
            is EquipmentPassive.ConditionalStatBonus -> add("conditional")
            is EquipmentPassive.TerrainAffinityBonus -> {
                add(normalizeLootBaseSelectionTag(passive.terrainTag.name))
                add("terrain")
            }
            is EquipmentPassive.DamageVsTag -> {
                add(normalizeLootBaseSelectionTag(passive.tag))
                add("offense")
            }
            is EquipmentPassive.DamageVsStatus -> {
                add(normalizeLootBaseSelectionTag(passive.statusId))
                add("offense")
            }
            is EquipmentPassive.DamageTypeBonus -> {
                add(normalizeLootBaseSelectionTag(passive.type.name))
                add("offense")
            }
            is EquipmentPassive.ResistanceBonus -> {
                add(normalizeLootBaseSelectionTag(passive.damageType.name))
                addAll(listOf("protection", "resistance"))
            }
            is EquipmentPassive.HpRegenPerTurn -> addAll(listOf("sustain", "life", "regeneration"))
            null -> Unit
        }
    }

internal fun normalizeLootBaseSelectionTag(tag: String): String = tag.trim().lowercase()
