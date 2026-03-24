package com.ktome.core.talent

enum class TalentTreeOwnerType {
    PROFESSION,
    RACE,
}

data class TalentAllocationDraft(
    val ownerType: TalentTreeOwnerType,
    val treeOwnerId: String,
    val pendingRanks: Map<String, Int>,
    val previousPendingRanks: Map<String, Int>? = null,
)

data class TalentAllocationPreview(
    val effectiveRanks: Map<String, Int>,
    val spentPointsDelta: Int,
    val remainingPoints: Int,
)

object TalentAllocationPlanner {
    fun effectiveRanks(
        liveRanks: Map<String, Int>,
        draft: TalentAllocationDraft?,
    ): Map<String, Int> =
        buildMap {
            putAll(liveRanks)
            draft?.pendingRanks?.forEach { (talentId, rank) -> put(talentId, rank) }
        }

    fun preview(
        liveRanks: Map<String, Int>,
        minimumRanks: Map<String, Int>,
        availablePoints: Int,
        draft: TalentAllocationDraft?,
    ): TalentAllocationPreview {
        val effectiveRanks = effectiveRanks(liveRanks = liveRanks, draft = draft)
        val liveSpent = spentPoints(liveRanks = liveRanks, minimumRanks = minimumRanks)
        val previewSpent = spentPoints(liveRanks = effectiveRanks, minimumRanks = minimumRanks)
        val delta = previewSpent - liveSpent
        return TalentAllocationPreview(
            effectiveRanks = effectiveRanks,
            spentPointsDelta = delta,
            remainingPoints = availablePoints - delta,
        )
    }

    fun applyRankIncrease(
        draft: TalentAllocationDraft?,
        ownerType: TalentTreeOwnerType,
        treeOwnerId: String,
        talentId: String,
        nextRank: Int,
    ): TalentAllocationDraft {
        val currentRanks = draft?.pendingRanks.orEmpty().toMutableMap()
        currentRanks[talentId] = nextRank
        return TalentAllocationDraft(
            ownerType = ownerType,
            treeOwnerId = treeOwnerId,
            pendingRanks = currentRanks.toMap(linkedMapOf()),
            previousPendingRanks = draft?.pendingRanks?.toMap(linkedMapOf()) ?: emptyMap(),
        )
    }

    fun hasPendingChanges(
        liveRanks: Map<String, Int>,
        draft: TalentAllocationDraft?,
    ): Boolean = draft != null && effectiveRanks(liveRanks = liveRanks, draft = draft) != liveRanks

    fun normalize(
        liveRanks: Map<String, Int>,
        draft: TalentAllocationDraft?,
    ): TalentAllocationDraft? = draft?.takeIf { hasPendingChanges(liveRanks = liveRanks, draft = it) }

    fun spentPoints(
        liveRanks: Map<String, Int>,
        minimumRanks: Map<String, Int>,
    ): Int =
        liveRanks.entries.sumOf { (talentId, rank) ->
            val minimumRank = minimumRanks[talentId] ?: 0
            (rank - minimumRank).coerceAtLeast(0)
        }
}

class RespecManager {
    fun createDraft(
        ownerType: TalentTreeOwnerType,
        treeOwnerId: String,
        liveRanks: Map<String, Int>,
        minimumRanks: Map<String, Int>,
    ): TalentAllocationDraft =
        TalentAllocationDraft(
            ownerType = ownerType,
            treeOwnerId = treeOwnerId,
            pendingRanks =
                liveRanks.keys.associateWith { talentId ->
                    minimumRanks[talentId] ?: 0
                },
            previousPendingRanks = liveRanks.toMap(linkedMapOf()),
        )
}

object RollbackManager {
    fun rollback(draft: TalentAllocationDraft): TalentAllocationDraft? =
        draft.previousPendingRanks?.let { previousRanks ->
            if (previousRanks.isEmpty()) {
                null
            } else {
                draft.copy(
                    pendingRanks = previousRanks.toMap(linkedMapOf()),
                    previousPendingRanks = null,
                )
            }
        }
}

object TalentPrerequisiteValidator {
    fun missingPrerequisites(
        prerequisites: List<TalentPrerequisite>,
        ranks: Map<String, Int>,
    ): List<TalentPrerequisite> =
        prerequisites.filter { prerequisite ->
            (ranks[prerequisite.talentId] ?: 0) < prerequisite.minRank
        }

    fun isSatisfied(
        prerequisites: List<TalentPrerequisite>,
        ranks: Map<String, Int>,
    ): Boolean = missingPrerequisites(prerequisites = prerequisites, ranks = ranks).isEmpty()
}
