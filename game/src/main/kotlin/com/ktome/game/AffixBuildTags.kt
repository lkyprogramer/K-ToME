package com.ktome.game

import com.ktome.core.item.AffixSelectionContext
import com.ktome.core.status.StatusEffectType
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.TalentLevelEffectSchemaV2
import com.ktome.game.data.schema.TalentSchemaV2
import com.ktome.game.data.schema.TalentTreeSchemaV2

internal fun professionAffixBuildContext(
    schemaCatalog: SchemaCatalog,
    profession: ProfessionSchemaV2,
    talentRanks: Map<String, Int> = profession.startingTalents.associateWith { 1 },
): AffixSelectionContext =
    AffixSelectionContext(
        buildTags = professionAffixBuildTags(schemaCatalog, profession, talentRanks),
    )

internal fun routeRewardBiasTags(rescueTags: Set<String>): Set<String> =
    linkedSetOf<String>().apply {
        rescueTags.forEach { tag ->
            val normalized = tag.trim().lowercase()
            if (normalized.isBlank()) {
                return@forEach
            }
            add(normalized)
            when (normalized) {
                "movement" -> addAll(listOf("mobility", "rescue"))
                "recovery" -> addAll(listOf("sustain", "life", "regeneration", "rescue"))
                "protection" -> addAll(listOf("protection", "defense"))
                "cleansing" -> addAll(listOf("cleansing", "rescue"))
                "arcane" -> addAll(listOf("arcane", "spell"))
                "fire" -> addAll(listOf("fire", "offense"))
            }
        }
    }

internal fun professionAffixBuildTags(
    schemaCatalog: SchemaCatalog,
    profession: ProfessionSchemaV2,
    talentRanks: Map<String, Int> = profession.startingTalents.associateWith { 1 },
): Set<String> {
    val talentTreesById = schemaCatalog.talentTrees.associateBy(TalentTreeSchemaV2::id)
    val talentsById = schemaCatalog.talents.associateBy(TalentSchemaV2::id)
    return linkedSetOf<String>().apply {
        addTag(profession.id)
        profession.tags.forEach(::addMeaningfulTag)
        addTag(profession.primarySpendAxis.name)
        addTag(profession.resourceType)
        profession.stateAxis?.let { axis ->
            addTag(axis.name)
            axis.asResourceTypeOrNull()?.name?.let(::addTag)
        }
        profession.talentTrees.forEach { treeId ->
            treeId.semanticTokens().forEach(::addMeaningfulTag)
            talentTreesById[treeId]?.let { tree ->
                tree.tags.forEach(::addMeaningfulTag)
            }
        }
        talentRanks.forEach { (talentId, currentRank) ->
            talentId.semanticTokens().forEach(::addMeaningfulTag)
            talentsById[talentId]?.let { talent ->
                talent.tags.forEach(::addMeaningfulTag)
                talent.damageType?.let(::addTag)
                talent.powerDimension?.let(::addTag)
                talent.keywords.forEach(::addMeaningfulTag)
                talent.levelEffects[currentRank.coerceIn(1, talent.maxPoints)]?.let(::addEffectSemanticTags)
            }
        }
        profession.soloContract.offenseTags.forEach(::addMeaningfulTag)
        profession.soloContract.defenseTags.forEach(::addMeaningfulTag)
        profession.soloContract.mobilityTags.forEach(::addMeaningfulTag)
        profession.soloContract.aoeAnswerTags.forEach(::addMeaningfulTag)
        profession.soloContract.bossAnswerTags.forEach(::addMeaningfulTag)
        profession.soloContract.panicAnswerTags.forEach(::addMeaningfulTag)
    }
}

private fun MutableSet<String>.addEffectSemanticTags(effect: TalentLevelEffectSchemaV2) {
    effect.associatedEffects.forEach { associatedEffect ->
        statusSemanticTags(associatedEffect.effectType).forEach(::addMeaningfulTag)
    }
    effect.cleanseEffect?.let {
        addMeaningfulTag("cleanse")
        addMeaningfulTag("sustain")
    }
    if (effect.resourceRestoreFraction > 0.0) {
        addMeaningfulTag("resource")
        addMeaningfulTag("tempo")
    }
    if (effect.healFraction > 0.0) {
        addMeaningfulTag("heal")
        addMeaningfulTag("sustain")
    }
}

private fun MutableSet<String>.addMeaningfulTag(tag: String) {
    val normalized = normalizeBuildTag(tag)
    if (normalized.isBlank() || normalized in NON_SIGNAL_BUILD_TAGS) {
        return
    }
    add(normalized)
}

private fun MutableSet<String>.addTag(tag: String) {
    val normalized = normalizeBuildTag(tag)
    if (normalized.isNotBlank()) {
        add(normalized)
    }
}

private val NON_SIGNAL_BUILD_TAGS =
    setOf(
        "profession",
        "foundation",
        "advanced",
        "playable",
        "frozen",
        "tree",
        "talent",
        "race",
    )

private fun String.semanticTokens(): List<String> =
    split('_', '-')
        .map(String::trim)
        .filter(String::isNotBlank)

private fun normalizeBuildTag(tag: String): String =
    when (val normalized = tag.trim().lowercase()) {
        "mark" -> "marked"
        "cleansing" -> "cleanse"
        else -> normalized
    }

private fun statusSemanticTags(effectType: String): Set<String> {
    val canonical = StatusEffectType.fromSchemaId(effectType).schemaId.lowercase()
    return linkedSetOf<String>().apply {
        add(canonical)
        when (canonical) {
            "armor_break" -> add("guard")
            "bane" -> add("holy")
            "burn" -> add("fire")
            "freeze" -> add("cold")
            "guard",
            "guard_stance_buff",
            -> addAll(listOf("guard", "hold_line"))
            "holy_shield_buff" -> addAll(listOf("holy_shield", "sustain"))
            "mana_surge_buff" -> addAll(listOf("mana_tempo", "teleport"))
            "marked" -> addAll(listOf("marked", "crit", "execute"))
            "stealth" -> addAll(listOf("stealth", "crit"))
        }
    }
}
