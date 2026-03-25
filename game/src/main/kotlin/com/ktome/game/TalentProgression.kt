package com.ktome.game

import com.ktome.core.race.RaceDef
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.TalentSchemaV2

internal object TalentProgression {
    fun unlockedTalentIds(
        schemaCatalog: SchemaCatalog,
        profession: ProfessionSchemaV2,
        level: Int,
        race: RaceDef? = null,
    ): List<String> {
        val talentById = schemaCatalog.talents.associateBy(TalentSchemaV2::id)
        val orderedTreeTalents =
            schemaCatalog.talentTrees
                .filter { tree -> tree.id in profession.talentTrees }
                .flatMap { tree -> tree.nodes }
        val orderedRaceTalents =
            race?.let { activeRace ->
                schemaCatalog.talentTrees
                    .filter { tree -> tree.id in activeRace.talentTrees }
                    .flatMap { tree -> tree.nodes }
            }.orEmpty()

        return buildList {
            profession.startingTalents.distinct().forEach { talentId ->
                if (talentById[talentId]?.unlockLevel?.let { unlockLevel -> level >= unlockLevel } != false) {
                    addUnique(talentId)
                }
            }
            race?.startingTalents.orEmpty().distinct().forEach { talentId ->
                if (talentById[talentId]?.unlockLevel?.let { unlockLevel -> level >= unlockLevel } != false) {
                    addUnique(talentId)
                }
            }
            orderedTreeTalents.forEach { talentId ->
                val schema = talentById[talentId] ?: return@forEach
                if (level >= schema.unlockLevel) {
                    addUnique(talentId)
                }
            }
            orderedRaceTalents.forEach { talentId ->
                val schema = talentById[talentId] ?: return@forEach
                if (level >= schema.unlockLevel) {
                    addUnique(talentId)
                }
            }
        }
    }

    private fun MutableList<String>.addUnique(talentId: String) {
        if (talentId !in this) {
            add(talentId)
        }
    }
}
