package com.ktome.game

import com.ktome.core.race.RaceDef
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.TalentPrerequisiteSchemaV2
import com.ktome.game.data.schema.TalentSchemaV2
import com.ktome.game.data.schema.TalentTreeSchemaV2

internal enum class TalentLockReasonType {
    LEVEL,
    PREREQUISITE_RANK,
    TREE_INVESTMENT,
    CROSS_TREE_INVESTMENT,
}

internal data class TalentLockReason(
    val type: TalentLockReasonType,
    val messageKey: String,
    val talentId: String? = null,
    val treeId: String? = null,
    val requiredLevel: Int? = null,
    val currentLevel: Int? = null,
    val requiredRank: Int? = null,
    val currentRank: Int? = null,
    val requiredPoints: Int? = null,
    val currentPoints: Int? = null,
)

internal data class TalentProgressionRequest(
    val schemaCatalog: SchemaCatalog,
    val profession: ProfessionSchemaV2,
    val level: Int,
    val learnedRanks: Map<String, Int>,
    val race: RaceDef? = null,
)

internal data class TalentProgressionEvaluationContext(
    val talentById: Map<String, TalentSchemaV2>,
    val treeById: Map<String, TalentTreeSchemaV2>,
    val ownerTreeIds: Set<String>,
    val treeInvestments: Map<String, Int>,
)

internal object TalentProgression {
    fun startingTalentIds(
        profession: ProfessionSchemaV2,
        race: RaceDef? = null,
    ): List<String> =
        buildList {
            profession.startingTalents.distinct().forEach { talentId -> addUnique(talentId) }
            race?.startingTalents.orEmpty().distinct().forEach { talentId -> addUnique(talentId) }
        }

    fun learnableTalentIds(request: TalentProgressionRequest): List<String> {
        val context = evaluationContext(request)
        return orderedTalentTrees(request.schemaCatalog, request.profession, request.race)
            .flatMap(TalentTreeSchemaV2::nodes)
            .distinct()
            .filterNot { talentId -> (request.learnedRanks[talentId] ?: 0) > 0 }
            .filter { talentId ->
                talentLockReasons(request, talentId, context).isEmpty()
            }
    }

    fun evaluationContext(request: TalentProgressionRequest): TalentProgressionEvaluationContext =
        TalentProgressionEvaluationContext(
            talentById = request.schemaCatalog.talents.associateBy(TalentSchemaV2::id),
            treeById = request.schemaCatalog.talentTrees.associateBy(TalentTreeSchemaV2::id),
            ownerTreeIds =
                orderedTalentTrees(request.schemaCatalog, request.profession, request.race)
                    .mapTo(linkedSetOf(), TalentTreeSchemaV2::id),
            treeInvestments = treeInvestmentByTree(request.schemaCatalog, request.learnedRanks),
        )

    fun talentLockReasons(
        request: TalentProgressionRequest,
        talentId: String,
    ): List<TalentLockReason> = talentLockReasons(request, talentId, evaluationContext(request))

    fun talentLockReasons(
        request: TalentProgressionRequest,
        talentId: String,
        context: TalentProgressionEvaluationContext,
    ): List<TalentLockReason> {
        val talent = context.talentById[talentId] ?: return listOf(unknownTalentReason(talentId))
        val tree = context.treeById[talent.treeId] ?: return listOf(unknownTreeReason(talent.treeId))
        if (tree.id !in context.ownerTreeIds) {
            return listOf(unknownTreeReason(tree.id))
        }

        val nodeTier = talentNodeTier(tree, talentId)
        val requiredUnlockLevel = maxOf(talent.unlockLevel, tierUnlockLevelRequirement(nodeTier))
        val reasons = mutableListOf<TalentLockReason>()
        if (request.level < requiredUnlockLevel) {
            reasons +=
                TalentLockReason(
                    type = TalentLockReasonType.LEVEL,
                    messageKey = "ui.talent.tree.lock.level",
                    requiredLevel = requiredUnlockLevel,
                    currentLevel = request.level,
                )
        }
        reasons += missingPrerequisiteReasons(talent.requirements.talentPrereqs, request.learnedRanks)
        val sameTreeRequired = sameTreeInvestmentRequirement(nodeTier)
        if (sameTreeRequired > 0) {
            val currentPoints = context.treeInvestments[tree.id] ?: 0
            if (currentPoints < sameTreeRequired) {
                reasons +=
                    TalentLockReason(
                        type = TalentLockReasonType.TREE_INVESTMENT,
                        messageKey = "ui.talent.tree.lock.tree_investment",
                        treeId = tree.id,
                        requiredPoints = sameTreeRequired,
                        currentPoints = currentPoints,
                    )
            }
        }
        val otherTreeRequired = otherTreeInvestmentRequirement(nodeTier)
        if (otherTreeRequired > 0 && tree.ownerRef().ownerType == com.ktome.core.talent.TalentTreeOwnerType.PROFESSION) {
            val investedOtherTrees =
                context.ownerTreeIds
                    .filterNot { ownerTreeId -> ownerTreeId == tree.id }
                    .count { ownerTreeId -> (context.treeInvestments[ownerTreeId] ?: 0) > 0 }
            if (investedOtherTrees < otherTreeRequired) {
                reasons +=
                    TalentLockReason(
                        type = TalentLockReasonType.CROSS_TREE_INVESTMENT,
                        messageKey = "ui.talent.tree.lock.cross_tree_investment",
                        treeId = tree.id,
                        requiredPoints = otherTreeRequired,
                        currentPoints = investedOtherTrees,
                    )
            }
        }
        return reasons
    }

    fun talentNodeTier(
        tree: TalentTreeSchemaV2,
        talentId: String,
    ): Int {
        val index = tree.nodes.indexOf(talentId)
        if (tree.nodes.size <= 4) {
            return when {
                index < 0 -> 1
                index <= 1 -> 1
                index == 2 -> 2
                else -> 3
            }
        }
        return when {
            index < 0 -> 1
            index <= 1 -> 1
            index <= 3 -> 2
            else -> 3
        }
    }

    fun treeInvestmentByTree(
        schemaCatalog: SchemaCatalog,
        learnedRanks: Map<String, Int>,
    ): Map<String, Int> {
        val talentTreeById =
            schemaCatalog.talentTrees
                .flatMap { tree -> tree.nodes.map { talentId -> talentId to tree.id } }
                .toMap()
        return learnedRanks.entries
            .filter { (_, rank) -> rank > 0 }
            .groupingBy { (talentId, _) -> talentTreeById[talentId] ?: "" }
            .fold(0) { total, (_, rank) -> total + rank }
            .filterKeys(String::isNotBlank)
            .toSortedMap()
    }

    private fun orderedTalentTrees(
        schemaCatalog: SchemaCatalog,
        profession: ProfessionSchemaV2,
        race: RaceDef?,
    ): List<TalentTreeSchemaV2> {
        val treesById = schemaCatalog.talentTrees.associateBy(TalentTreeSchemaV2::id)
        return buildList {
            profession.talentTrees.mapNotNull(treesById::get).forEach(::add)
            race?.talentTrees.orEmpty().mapNotNull(treesById::get).forEach(::add)
        }
    }

    private fun missingPrerequisiteReasons(
        prerequisites: List<TalentPrerequisiteSchemaV2>,
        ranks: Map<String, Int>,
    ): List<TalentLockReason> =
        prerequisites.mapNotNull { prerequisite ->
            val currentRank = ranks[prerequisite.talentId] ?: 0
            if (currentRank >= prerequisite.minRank) {
                null
            } else {
                TalentLockReason(
                    type = TalentLockReasonType.PREREQUISITE_RANK,
                    messageKey = "ui.talent.tree.lock.prerequisite_rank",
                    talentId = prerequisite.talentId,
                    requiredRank = prerequisite.minRank,
                    currentRank = currentRank,
                )
            }
        }

    private fun sameTreeInvestmentRequirement(nodeTier: Int): Int =
        when {
            nodeTier >= 3 -> 5
            nodeTier == 2 -> 2
            else -> 0
        }

    private fun tierUnlockLevelRequirement(nodeTier: Int): Int =
        when {
            nodeTier >= 3 -> 5
            nodeTier == 2 -> 3
            else -> 1
        }

    private fun otherTreeInvestmentRequirement(nodeTier: Int): Int =
        when {
            nodeTier == 2 -> 1
            else -> 0
        }

    private fun unknownTalentReason(talentId: String): TalentLockReason =
        TalentLockReason(
            type = TalentLockReasonType.PREREQUISITE_RANK,
            messageKey = "ui.talent.tree.lock.unknown_talent",
            talentId = talentId,
        )

    private fun unknownTreeReason(treeId: String): TalentLockReason =
        TalentLockReason(
            type = TalentLockReasonType.TREE_INVESTMENT,
            messageKey = "ui.talent.tree.lock.unknown_tree",
            treeId = treeId,
        )

    private fun MutableList<String>.addUnique(talentId: String) {
        if (talentId !in this) {
            add(talentId)
        }
    }
}
