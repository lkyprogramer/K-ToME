package com.ktome.game

import com.ktome.core.item.AffixSelectionContext
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.TalentSchemaV2
import com.ktome.game.data.schema.TalentTreeSchemaV2

internal fun professionAffixBuildContext(
    schemaCatalog: SchemaCatalog,
    profession: ProfessionSchemaV2,
    unlockedTalentIds: Set<String> = profession.startingTalents.toSet(),
): AffixSelectionContext =
    AffixSelectionContext(
        buildTags = professionAffixBuildTags(schemaCatalog, profession, unlockedTalentIds),
    )

internal fun professionAffixBuildTags(
    schemaCatalog: SchemaCatalog,
    profession: ProfessionSchemaV2,
    unlockedTalentIds: Set<String> = profession.startingTalents.toSet(),
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
        unlockedTalentIds.forEach { talentId ->
            talentId.semanticTokens().forEach(::addMeaningfulTag)
            talentsById[talentId]?.let { talent ->
                talent.tags.forEach(::addMeaningfulTag)
                talent.damageType?.let(::addTag)
                talent.powerDimension?.let(::addTag)
                talent.keywords.forEach(::addMeaningfulTag)
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

private fun MutableSet<String>.addMeaningfulTag(tag: String) {
    val normalized = tag.trim().lowercase()
    if (normalized.isBlank() || normalized in NON_SIGNAL_BUILD_TAGS) {
        return
    }
    add(normalized)
}

private fun MutableSet<String>.addTag(tag: String) {
    val normalized = tag.trim().lowercase()
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
