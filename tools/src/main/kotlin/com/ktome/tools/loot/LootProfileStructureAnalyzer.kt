package com.ktome.tools.loot

import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.data.schema.LootProfileLocalIdentityCategory
import com.ktome.game.data.schema.LootProfileSchemaV3
import com.ktome.game.loot.LootProfileCandidatePoolResolver
import com.ktome.core.item.ItemDataBundle

private const val NEAR_SUBSET_OVERLAP_THRESHOLD: Double = 0.95

data class LootProfileStructureAnalysis(
    val profileCount: Int,
    val pairDiffs: List<LootProfilePairDiff>,
    val culpritPairs: List<LootProfilePairDiff>,
)

data class LootProfilePairDiff(
    val pairId: String,
    val leftProfileId: String,
    val rightProfileId: String,
    val overlap: Double,
    val leftIsSubsetOfRight: Boolean,
    val rightIsSubsetOfLeft: Boolean,
    val pairType: String?,
    val sharedBaseIds: List<String>,
    val leftOnlyBaseIds: List<String>,
    val rightOnlyBaseIds: List<String>,
    val explicitVsTagMatched: LootPairSourceBreakdown,
    val culpritReasons: List<String>,
)

data class LootPairSourceBreakdown(
    val left: LootProfileSourceBreakdown,
    val right: LootProfileSourceBreakdown,
)

data class LootProfileSourceBreakdown(
    val explicitOnlyBaseIds: List<String>,
    val tagMatchedOnlyBaseIds: List<String>,
    val explicitAndTagMatchedBaseIds: List<String>,
)

private data class LootProfileSourceIndex(
    val profileId: String,
    val candidateBaseIds: Set<String>,
    val explicitBaseIds: Set<String>,
    val tagMatchedBaseIds: Set<String>,
    val canonicalZoneId: String?,
    val category: LootProfileLocalIdentityCategory,
) {
    fun toBreakdown(): LootProfileSourceBreakdown =
        LootProfileSourceBreakdown(
            explicitOnlyBaseIds = (explicitBaseIds - tagMatchedBaseIds).toList().sorted(),
            tagMatchedOnlyBaseIds = (tagMatchedBaseIds - explicitBaseIds).toList().sorted(),
            explicitAndTagMatchedBaseIds = explicitBaseIds.intersect(tagMatchedBaseIds).toList().sorted(),
        )
}

object LootProfileStructureAnalyzer {
    fun analyze(
        profiles: List<LootProfileSchemaV3>,
        itemBundle: ItemDataBundle,
    ): LootProfileStructureAnalysis {
        val resolver = LootProfileCandidatePoolResolver(itemBundle)
        val sourceIndices =
            profiles
                .map { profile ->
                    val pool = resolver.resolve(profile)
                    val explicitBaseIds = profile.itemIds.toSet()
                    val localIdentityMetadata = profile.localIdentityMetadata()
                    val tagMatchedBaseIds =
                        if (profile.poolStrategy == LootPoolStrategy.TAG_WEIGHTED) {
                            resolveTagMatchedBaseIds(itemBundle = itemBundle, tags = profile.itemTagFilter)
                        } else {
                            emptySet()
                        }
                    LootProfileSourceIndex(
                        profileId = profile.id,
                        candidateBaseIds = pool.allCandidateBaseIds,
                        explicitBaseIds = explicitBaseIds.intersect(pool.allCandidateBaseIds),
                        tagMatchedBaseIds = tagMatchedBaseIds.intersect(pool.allCandidateBaseIds),
                        canonicalZoneId = localIdentityMetadata.canonicalZoneId,
                        category = localIdentityMetadata.category,
                    )
                }
        val pairDiffs =
            buildList {
                for (leftIndex in sourceIndices.indices) {
                    for (rightIndex in leftIndex + 1 until sourceIndices.size) {
                        add(pairDiff(left = sourceIndices[leftIndex], right = sourceIndices[rightIndex]))
                    }
                }
            }
        val culpritPairs = pairDiffs.filter { pairDiff -> pairDiff.isCulprit() }
        return LootProfileStructureAnalysis(
            profileCount = profiles.size,
            pairDiffs = pairDiffs.sortedBy(LootProfilePairDiff::pairId),
            culpritPairs = culpritPairs.sortedWith(compareByDescending<LootProfilePairDiff> { it.overlap }.thenBy(LootProfilePairDiff::pairId)),
        )
    }

    private fun pairDiff(
        left: LootProfileSourceIndex,
        right: LootProfileSourceIndex,
    ): LootProfilePairDiff {
        val sharedBaseIds = left.candidateBaseIds.intersect(right.candidateBaseIds).toList().sorted()
        val leftOnlyBaseIds = (left.candidateBaseIds - right.candidateBaseIds).toList().sorted()
        val rightOnlyBaseIds = (right.candidateBaseIds - left.candidateBaseIds).toList().sorted()
        val denominator = minOf(left.candidateBaseIds.size, right.candidateBaseIds.size).coerceAtLeast(1)
        val overlap = sharedBaseIds.size.toDouble() / denominator.toDouble()
        val pairType = pairType(left = left, right = right)
        val culpritReasons =
            buildList {
                if (leftOnlyBaseIds.isEmpty()) {
                    add("left_subset_of_right")
                }
                if (rightOnlyBaseIds.isEmpty()) {
                    add("right_subset_of_left")
                }
                if (overlap >= NEAR_SUBSET_OVERLAP_THRESHOLD) {
                    add("near_total_subset_overlap")
                }
                if (exceedsGlobalLocalIdentityGuardrail(pairType = pairType, overlap = overlap)) {
                    add(
                        when (pairType) {
                            SECRET_VS_CADENCE_PAIR_TYPE -> "same_zone_secret_vs_cadence"
                            SECRET_VS_REWARD_PAIR_TYPE -> "same_zone_secret_vs_reward"
                            else -> error("Unexpected same-zone local identity pairType '$pairType'.")
                        },
                    )
                }
            }
        return LootProfilePairDiff(
            pairId = "${left.profileId} <-> ${right.profileId}",
            leftProfileId = left.profileId,
            rightProfileId = right.profileId,
            overlap = overlap,
            leftIsSubsetOfRight = leftOnlyBaseIds.isEmpty(),
            rightIsSubsetOfLeft = rightOnlyBaseIds.isEmpty(),
            pairType = pairType,
            sharedBaseIds = sharedBaseIds,
            leftOnlyBaseIds = leftOnlyBaseIds,
            rightOnlyBaseIds = rightOnlyBaseIds,
            explicitVsTagMatched =
                LootPairSourceBreakdown(
                    left = left.toBreakdown(),
                    right = right.toBreakdown(),
                ),
            culpritReasons = culpritReasons.sorted(),
        )
    }

    private fun pairType(
        left: LootProfileSourceIndex,
        right: LootProfileSourceIndex,
    ): String? {
        if (left.canonicalZoneId == null || right.canonicalZoneId == null || left.canonicalZoneId != right.canonicalZoneId) {
            return null
        }
        return localIdentityPairType(leftCategory = left.category, rightCategory = right.category)
    }

    private fun LootProfilePairDiff.isCulprit(): Boolean = culpritReasons.isNotEmpty()

    private fun resolveTagMatchedBaseIds(
        itemBundle: ItemDataBundle,
        tags: List<String>,
    ): Set<String> {
        if (tags.isEmpty()) {
            return emptySet()
        }
        val normalizedTags = tags.mapTo(linkedSetOf(), ::normalizeLootTag)
        return itemBundle.baseItems
            .asSequence()
            .filter { base -> base.tags.any { tag -> normalizeLootTag(tag) in normalizedTags } }
            .mapTo(linkedSetOf()) { base -> base.id }
    }
}
