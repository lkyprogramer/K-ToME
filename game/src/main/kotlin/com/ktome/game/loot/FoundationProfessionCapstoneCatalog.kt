package com.ktome.game.loot

import com.ktome.core.item.EquipSlot
import com.ktome.core.profession.ProfessionTier
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.ProfessionSchemaV2

private data class FoundationProfessionCapstoneCatalog(
    val baseIdsByProfessionId: Map<String, Set<String>>,
    val baseIdsByResourceType: Map<String, Set<String>>,
    val nonWeaponBaseIdsByResourceType: Map<String, Set<String>>,
)

private val foundationProfessionCapstoneCatalog: FoundationProfessionCapstoneCatalog by lazy(::buildFoundationProfessionCapstoneCatalog)

internal val foundationProfessionCapstoneBaseIdsByProfessionId: Map<String, Set<String>>
    get() = foundationProfessionCapstoneCatalog.baseIdsByProfessionId

internal fun foundationProfessionCapstonePreferenceScore(
    resourceTypeId: String,
    baseItemId: String,
    slot: EquipSlot?,
): Int {
    val capstoneIds = foundationProfessionCapstoneCatalog.baseIdsByResourceType[resourceTypeId].orEmpty()
    if (baseItemId !in capstoneIds) {
        return 0
    }
    val nonWeaponCapstoneIds = foundationProfessionCapstoneCatalog.nonWeaponBaseIdsByResourceType[resourceTypeId].orEmpty()
    val nonWeaponCapstone = baseItemId in nonWeaponCapstoneIds
    return when (slot) {
        EquipSlot.OFF_HAND -> if (nonWeaponCapstone) 150 else 120
        EquipSlot.ARMOR -> if (nonWeaponCapstone) 140 else 120
        EquipSlot.WEAPON -> 120
        null -> if (nonWeaponCapstone) 135 else 120
    }
}

private fun buildFoundationProfessionCapstoneCatalog(): FoundationProfessionCapstoneCatalog {
    val loader = DataLoader()
    val schemaCatalog = loader.loadSchemaCatalog()
    val itemBundle = loader.loadItemBundle()
    val foundationProfessions =
        schemaCatalog.professions.filter { profession ->
            profession.tier == ProfessionTier.BASE &&
                profession.tags.any { tag -> normalizeLootBaseSelectionTag(tag) == "foundation" }
        }
    val normalizedTagsByBaseId =
        itemBundle.baseItems.associate { base ->
            base.id to base.tags.mapTo(linkedSetOf(), ::normalizeLootBaseSelectionTag)
        }
    val baseIdsByProfessionId =
        foundationProfessions.associate { profession ->
            val capstoneBaseIds =
                itemBundle.baseItems
                    .asSequence()
                    .filter { base ->
                        val tags = normalizedTagsByBaseId.getValue(base.id)
                        "capstone" in tags && profession.id in tags
                    }.map { base -> base.id }
                    .toCollection(linkedSetOf())
            check(capstoneBaseIds.isNotEmpty()) {
                "Foundation profession '${profession.id}' must declare at least one tagged capstone base item."
            }
            profession.id to capstoneBaseIds
        }
    val baseIdsByResourceType = foundationProfessions.capstoneIdsByResourceType(baseIdsByProfessionId)
    return FoundationProfessionCapstoneCatalog(
        baseIdsByProfessionId = baseIdsByProfessionId,
        baseIdsByResourceType = baseIdsByResourceType,
        nonWeaponBaseIdsByResourceType =
            foundationProfessions.capstoneIdsByResourceType(baseIdsByProfessionId) { baseItemId ->
                "non_weapon_capstone" in normalizedTagsByBaseId.getValue(baseItemId)
            },
    )
}

private fun List<ProfessionSchemaV2>.capstoneIdsByResourceType(
    baseIdsByProfessionId: Map<String, Set<String>>,
    filter: (String) -> Boolean = { true },
): Map<String, Set<String>> =
    groupBy(ProfessionSchemaV2::resourceType)
        .mapValues { (_, professions) ->
            professions
                .flatMap { profession -> baseIdsByProfessionId.getValue(profession.id) }
                .filter(filter)
                .toCollection(linkedSetOf())
        }
