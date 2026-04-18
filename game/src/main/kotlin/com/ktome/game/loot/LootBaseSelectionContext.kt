package com.ktome.game.loot

import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.ItemBaseDef

private const val NO_BUILD_MATCH_MULTIPLIER_BPS: Int = 10_000
private const val WEAK_BUILD_MATCH_MULTIPLIER_BPS: Int = 11_500
private const val STRONG_BUILD_MATCH_MULTIPLIER_BPS: Int = 13_500
private const val EXACT_PROFESSION_MATCH_MULTIPLIER_BPS: Int = 30_000
private const val EXACT_PROFESSION_CAPSTONE_MATCH_MULTIPLIER_BPS: Int = 250_000
private const val DEFAULT_ANTI_COLLAPSE_MULTIPLIER_BPS: Int = 10_000
private const val DOMINANT_RISK_MISMATCH_MULTIPLIER_BPS: Int = 6_500

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
    val preferredProfessionTag: String? = null,
) {
    init {
        require(buildTags.none(String::isBlank)) { "LootBaseSelectionContext.buildTags must not contain blanks." }
        require(preferredProfessionTag?.isBlank() != true) {
            "LootBaseSelectionContext.preferredProfessionTag must not be blank when present."
        }
    }

    val normalizedBuildTags: Set<String> = buildTags.mapTo(linkedSetOf(), ::normalizeLootBaseSelectionTag)
    val normalizedPreferredProfessionTag: String? = preferredProfessionTag?.let(::normalizeLootBaseSelectionTag)

    val hasSignalContext: Boolean
        get() = normalizedBuildTags.isNotEmpty() || normalizedPreferredProfessionTag != null

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
    val exactProfessionMatch: Boolean,
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
            exactProfessionMatch = false,
            buildTagMatchMultiplierBasisPoints = NO_BUILD_MATCH_MULTIPLIER_BPS,
            antiCollapseMultiplierBasisPoints = DEFAULT_ANTI_COLLAPSE_MULTIPLIER_BPS,
        )
    }
    val matchedBuildTags = semanticTags.intersect(normalizedBuildTags)
    val exactProfessionMatch =
        normalizedPreferredProfessionTag?.let { preferredProfessionTag ->
            preferredProfessionTag in semanticTags
        } ?: false
    val exactProfessionCapstoneMatch =
        exactProfessionMatch &&
            normalizedPreferredProfessionTag?.let { professionId ->
                base.id in foundationProfessionCapstoneBaseIdsByProfessionId[professionId].orEmpty()
            } == true
    val matchStrength =
        when {
            matchedBuildTags.size >= 2 -> LootBaseBuildMatchStrength.STRONG
            matchedBuildTags.size == 1 -> LootBaseBuildMatchStrength.WEAK
            else -> LootBaseBuildMatchStrength.NONE
        }
    val buildTagMatchMultiplierBasisPoints =
        when {
            exactProfessionCapstoneMatch -> EXACT_PROFESSION_CAPSTONE_MATCH_MULTIPLIER_BPS
            exactProfessionMatch -> EXACT_PROFESSION_MATCH_MULTIPLIER_BPS
            matchStrength == LootBaseBuildMatchStrength.STRONG -> STRONG_BUILD_MATCH_MULTIPLIER_BPS
            matchStrength == LootBaseBuildMatchStrength.WEAK -> WEAK_BUILD_MATCH_MULTIPLIER_BPS
            else -> NO_BUILD_MATCH_MULTIPLIER_BPS
        }
    val antiCollapseMultiplierBasisPoints =
        if ("dominant_risk" in rawSemanticTags && normalizedPreferredProfessionTag != null && !exactProfessionMatch) {
            DOMINANT_RISK_MISMATCH_MULTIPLIER_BPS
        } else {
            DEFAULT_ANTI_COLLAPSE_MULTIPLIER_BPS
        }
    return LootBaseSelectionEvaluation(
        semanticTags = semanticTags,
        matchedBuildTags = matchedBuildTags,
        matchStrength = matchStrength,
        exactProfessionMatch = exactProfessionMatch,
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
