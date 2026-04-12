package com.ktome.game.loot

import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemType
import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.data.schema.LootProfileSchemaV3

data class LootProfileCandidatePool(
    val profileId: String,
    val allCandidateBaseIds: Set<String>,
    val standardCandidateBaseIds: Set<String>,
    val specialLinkedBaseIds: Set<String>,
    val preferredSpecialTemplateIds: Set<String>,
    val typeWeights: Map<ItemType, Int>,
    val slotBias: Map<EquipSlot, Int>,
    val affixBiasTags: Set<String>,
    val specialTemplateBiasTags: Set<String>,
) {
    init {
        require(profileId.isNotBlank()) { "LootProfileCandidatePool.profileId must not be blank." }
        require(allCandidateBaseIds.isNotEmpty()) { "LootProfileCandidatePool.allCandidateBaseIds must not be empty." }
        require(standardCandidateBaseIds.isNotEmpty()) { "LootProfileCandidatePool.standardCandidateBaseIds must not be empty." }
        require(allCandidateBaseIds.containsAll(standardCandidateBaseIds)) {
            "LootProfileCandidatePool.standardCandidateBaseIds must stay within allCandidateBaseIds."
        }
        require(allCandidateBaseIds.containsAll(specialLinkedBaseIds)) {
            "LootProfileCandidatePool.specialLinkedBaseIds must stay within allCandidateBaseIds."
        }
        require(preferredSpecialTemplateIds.none(String::isBlank)) {
            "LootProfileCandidatePool.preferredSpecialTemplateIds must not contain blanks."
        }
        require(typeWeights.values.all { weight -> weight > 0 }) {
            "LootProfileCandidatePool.typeWeights must be positive."
        }
        require(slotBias.values.all { weight -> weight > 0 }) {
            "LootProfileCandidatePool.slotBias must be positive."
        }
        require(affixBiasTags.none(String::isBlank)) {
            "LootProfileCandidatePool.affixBiasTags must not contain blanks."
        }
        require(specialTemplateBiasTags.none(String::isBlank)) {
            "LootProfileCandidatePool.specialTemplateBiasTags must not contain blanks."
        }
    }

    fun weightFor(
        base: ItemBaseDef,
        selectionContext: LootBaseSelectionContext = LootBaseSelectionContext.EMPTY,
    ): Int {
        val typeWeight = typeWeights[base.type] ?: 1
        val slotWeight = base.slot?.let { slot -> slotBias[slot] ?: 1 } ?: 1
        val evaluation = selectionContext.evaluate(base)
        val weighted =
            base.dropWeight.coerceAtLeast(1).toLong() *
                typeWeight.toLong() *
                slotWeight.toLong() *
                evaluation.buildTagMatchMultiplierBasisPoints.toLong() *
                evaluation.antiCollapseMultiplierBasisPoints.toLong()
        return (weighted / 10_000L / 10_000L).coerceAtLeast(1L).toInt()
    }
}

class LootProfileCandidatePoolResolver(
    private val itemBundle: ItemDataBundle,
) {
    private val baseItemsById: Map<String, ItemBaseDef> = itemBundle.baseItems.associateBy(ItemBaseDef::id)

    fun resolve(profile: LootProfileSchemaV3): LootProfileCandidatePool {
        val explicitBaseIds = resolveExplicitIds(profile.itemIds, fieldName = "itemIds", profileId = profile.id)
        val allCandidateBaseIds =
            linkedSetOf<String>().apply {
                addAll(explicitBaseIds)
                if (profile.poolStrategy == LootPoolStrategy.TAG_WEIGHTED) {
                    addAll(resolveTagMatchedIds(profile.itemTagFilter))
                }
            }
        val excludedIds = resolveExplicitIds(profile.excludeIds, fieldName = "excludeIds", profileId = profile.id)
        allCandidateBaseIds.removeAll(excludedIds)
        val survivingExplicitBaseIds = explicitBaseIds - excludedIds

        require(allCandidateBaseIds.isNotEmpty()) {
            "Loot profile '${profile.id}' resolved to an empty candidate pool."
        }

        val specialLinkedBaseIds =
            allCandidateBaseIds
                .filterTo(linkedSetOf()) { baseId -> itemBundle.specialTemplateForItemId(baseId) != null }
        val explicitPreferredSpecialTemplateIds =
            survivingExplicitBaseIds
                .mapNotNullTo(linkedSetOf()) { baseId -> itemBundle.specialTemplateForItemId(baseId)?.id }
        val preferredSpecialTemplateIds =
            explicitPreferredSpecialTemplateIds.ifEmpty {
                resolveTemplateIdsByTags(
                    tags = profile.specialTemplateTagPreference,
                    allowedBaseIds = allCandidateBaseIds,
                )
            }
        val standardCandidateBaseIds = allCandidateBaseIds - specialLinkedBaseIds
        require(standardCandidateBaseIds.isNotEmpty()) {
            "Loot profile '${profile.id}' resolved to only special-linked bases. Standard rewards must keep at least one non-special candidate."
        }

        return LootProfileCandidatePool(
            profileId = profile.id,
            allCandidateBaseIds = allCandidateBaseIds,
            standardCandidateBaseIds = standardCandidateBaseIds,
            specialLinkedBaseIds = specialLinkedBaseIds,
            preferredSpecialTemplateIds = preferredSpecialTemplateIds,
            typeWeights = profile.typeWeights,
            slotBias = profile.slotBias,
            affixBiasTags = profile.affixTagPreference.mapTo(linkedSetOf(), ::normalizeTag),
            specialTemplateBiasTags = profile.specialTemplateTagPreference.mapTo(linkedSetOf(), ::normalizeTag),
        )
    }

    private fun resolveExplicitIds(
        ids: List<String>,
        fieldName: String,
        profileId: String,
    ): Set<String> =
        ids.mapTo(linkedSetOf()) { baseId ->
            require(baseItemsById.containsKey(baseId)) {
                "Loot profile '$profileId' references unknown base item '$baseId' in $fieldName."
            }
            baseId
        }

    private fun resolveTagMatchedIds(tags: List<String>): Set<String> {
        if (tags.isEmpty()) {
            return emptySet()
        }
        val normalizedTags = tags.mapTo(linkedSetOf(), ::normalizeTag)
        return baseItemsById.values
            .asSequence()
            .filter { base -> base.tags.any { tag -> normalizeTag(tag) in normalizedTags } }
            .mapTo(linkedSetOf(), ItemBaseDef::id)
    }

    private fun resolveTemplateIdsByTags(
        tags: List<String>,
        allowedBaseIds: Set<String>,
    ): Set<String> {
        if (tags.isEmpty()) {
            return emptySet()
        }
        val normalizedTags = tags.mapTo(linkedSetOf(), ::normalizeTag)
        return itemBundle.specialTemplates
            .asSequence()
            .filter { template -> template.itemId in allowedBaseIds }
            .filter { template -> template.tags.any { tag -> normalizeTag(tag) in normalizedTags } }
            .mapTo(linkedSetOf()) { template -> template.id }
    }

    private fun normalizeTag(tag: String): String = tag.trim().lowercase()
}
