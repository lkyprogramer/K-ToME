package com.ktome.game.loot

import com.ktome.core.item.EquipSlot
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.profession.ProfessionTier
import com.ktome.game.data.DataLoader

data class FoundationBuildIdentityReportOnlyFloors(
    val seenMinCount: Int,
    val adoptionMinCount: Int,
    val nonWeaponMinCount: Int,
)

data class FoundationProfessionBuildIdentity(
    val professionId: String,
    val resourceTypeId: String,
    val capstoneBaseIds: Set<String>,
    val nonWeaponCapstoneBaseIds: Set<String>,
    val preferredRewardSources: Set<MilestoneRewardSource>,
    val preferredReplacementSlots: Set<EquipSlot>,
    val terminalIdentityTags: Set<String>,
    val reportOnlyFloors: FoundationBuildIdentityReportOnlyFloors,
)

private data class FoundationBuildIdentityCatalog(
    val identitiesByProfessionId: Map<String, FoundationProfessionBuildIdentity>,
    val identitiesByResourceTypeId: Map<String, FoundationProfessionBuildIdentity>,
)

private val foundationBuildIdentityCatalog: FoundationBuildIdentityCatalog by lazy(::buildFoundationBuildIdentityCatalog)

val foundationBuildIdentityByProfessionId: Map<String, FoundationProfessionBuildIdentity>
    get() = foundationBuildIdentityCatalog.identitiesByProfessionId

val foundationBuildIdentityProfessionIds: Set<String>
    get() = foundationBuildIdentityCatalog.identitiesByProfessionId.keys

val foundationProfessionCapstoneBaseIdsByProfessionId: Map<String, Set<String>>
    get() =
        foundationBuildIdentityCatalog.identitiesByProfessionId.mapValues { (_, identity) ->
            identity.capstoneBaseIds
        }

fun foundationBuildIdentityForResourceType(resourceTypeId: String): FoundationProfessionBuildIdentity? =
    foundationBuildIdentityCatalog.identitiesByResourceTypeId[resourceTypeId]

fun foundationProfessionCapstonePreferenceScore(
    resourceTypeId: String,
    baseItemId: String,
    slot: EquipSlot?,
): Int {
    val identity = foundationBuildIdentityForResourceType(resourceTypeId) ?: return 0
    if (baseItemId !in identity.capstoneBaseIds) {
        return 0
    }
    val nonWeaponCapstone = baseItemId in identity.nonWeaponCapstoneBaseIds
    return when (slot) {
        EquipSlot.OFF_HAND -> if (nonWeaponCapstone) 150 else 120
        EquipSlot.ARMOR -> if (nonWeaponCapstone) 140 else 120
        EquipSlot.WEAPON -> 120
        null -> if (nonWeaponCapstone) 135 else 120
    }
}

private fun buildFoundationBuildIdentityCatalog(): FoundationBuildIdentityCatalog {
    val schemaCatalog = DataLoader().loadSchemaCatalog()
    val foundationProfessionsById =
        schemaCatalog.professions
            .filter { profession ->
                profession.tier == ProfessionTier.BASE &&
                    profession.tags.any { tag -> normalizeLootBaseSelectionTag(tag) == "foundation" }
            }.associateBy { profession -> profession.id }
    val identitiesByProfessionId =
        schemaCatalog.buildIdentities.associate { identity ->
            val profession = requireNotNull(foundationProfessionsById[identity.professionId]) {
                "Missing foundation profession '${identity.professionId}' for build identity catalog."
            }
            identity.professionId to
                FoundationProfessionBuildIdentity(
                    professionId = identity.professionId,
                    resourceTypeId = profession.resourceType,
                    capstoneBaseIds = identity.capstoneBaseIds.toCollection(linkedSetOf()),
                    nonWeaponCapstoneBaseIds = identity.nonWeaponCapstoneBaseIds.toCollection(linkedSetOf()),
                    preferredRewardSources = identity.preferredRewardSources.toCollection(linkedSetOf()),
                    preferredReplacementSlots = identity.preferredReplacementSlots.toCollection(linkedSetOf()),
                    terminalIdentityTags = identity.terminalIdentityTags.mapTo(linkedSetOf(), ::normalizeLootBaseSelectionTag),
                    reportOnlyFloors =
                        FoundationBuildIdentityReportOnlyFloors(
                            seenMinCount = identity.reportOnlyFloors.seenMinCount,
                            adoptionMinCount = identity.reportOnlyFloors.adoptionMinCount,
                            nonWeaponMinCount = identity.reportOnlyFloors.nonWeaponMinCount,
                        ),
                )
        }
    val identitiesByResourceTypeId =
        identitiesByProfessionId.values.associateBy { identity -> identity.resourceTypeId }
    require(identitiesByResourceTypeId.size == identitiesByProfessionId.size) {
        "Foundation build identity catalog must keep a unique resourceTypeId per foundation profession."
    }
    return FoundationBuildIdentityCatalog(
        identitiesByProfessionId = identitiesByProfessionId,
        identitiesByResourceTypeId = identitiesByResourceTypeId,
    )
}
