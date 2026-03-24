package com.ktome.game

import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.game.data.schema.TalentSchemaV2
import com.ktome.game.data.schema.TalentTreeSchemaV2

data class TalentTreeOwnerRef(
    val ownerType: TalentTreeOwnerType,
    val treeOwnerId: String,
)

class TalentTreeOwnerResolver(
    private val treeSchemasById: Map<String, TalentTreeSchemaV2>,
) {
    fun ownerForTalent(talentSchema: TalentSchemaV2): TalentTreeOwnerRef? =
        treeSchemasById[talentSchema.treeId]?.ownerRef()

    fun ownerForTalents(talentSchemas: Iterable<TalentSchemaV2>): TalentTreeOwnerRef? =
        talentSchemas
            .mapNotNull(::ownerForTalent)
            .distinct()
            .singleOrNull()
}
