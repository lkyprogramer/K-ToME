package com.ktome.game.loot

import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemType
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier

class MilestoneRewardSelector(
    private val itemBundle: ItemDataBundle,
) {
    private val baseItemsById: Map<String, ItemBaseDef> = itemBundle.baseItems.associateBy(ItemBaseDef::id)
    private val supportDuplicateGuardBaseIds: Set<String> = setOf("basic_shield")

    fun select(
        request: MilestoneRewardSelectionRequest,
        professionSuitability: (ItemBaseDef) -> Boolean,
        canSatisfyAffixes: (ItemBaseDef) -> Boolean,
    ): MilestoneRewardSelectionResult {
        val candidatesByReplacementSlot = linkedMapOf<EquipSlot?, List<MilestoneRewardCandidate>>()
        fun rankedCandidatesFor(replacementSlot: EquipSlot?): List<MilestoneRewardCandidate> =
            candidatesByReplacementSlot.getOrPut(replacementSlot) {
                evaluateCandidates(
                    request = request,
                    replacementSlot = replacementSlot,
                    professionSuitability = professionSuitability,
                    canSatisfyAffixes = canSatisfyAffixes,
                )
            }
        val strictCandidates =
            rankedCandidatesFor(request.forcedReplacementSlot)
        if (request.forcedReplacementSlot != null) {
            return MilestoneRewardSelectionResult(
                selectedBaseId = strictCandidates.firstOrNull(MilestoneRewardCandidate::legal)?.baseItemId,
                replacementSlot = request.forcedReplacementSlot,
                rankedCandidates = strictCandidates,
            )
        }
        val replacementDecision =
            selectReplacementSlot(
                request = request,
                rankedCandidatesFor = ::rankedCandidatesFor,
            )
        if (strictCandidates.any(MilestoneRewardCandidate::legal) && replacementDecision?.exactProfessionCapstone != true) {
            return MilestoneRewardSelectionResult(
                selectedBaseId = strictCandidates.firstOrNull(MilestoneRewardCandidate::legal)?.baseItemId,
                replacementSlot = null,
                rankedCandidates = strictCandidates,
            )
        }
        val replacementSlot = replacementDecision?.slot
        val rankedCandidates =
            if (replacementSlot == null) {
                strictCandidates
            } else {
                rankedCandidatesFor(replacementSlot)
            }
        return MilestoneRewardSelectionResult(
            selectedBaseId = rankedCandidates.firstOrNull(MilestoneRewardCandidate::legal)?.baseItemId,
            replacementSlot = replacementSlot,
            rankedCandidates = rankedCandidates,
        )
    }

    private fun selectReplacementSlot(
        request: MilestoneRewardSelectionRequest,
        rankedCandidatesFor: (EquipSlot?) -> List<MilestoneRewardCandidate>,
    ): ReplacementSlotCandidate? =
        request.replacementSlotPriority
            .mapIndexedNotNull { priorityIndex, candidateSlot ->
                if (candidateSlot !in request.selectorContext.occupiedSlots || candidateSlot in request.selectorContext.reservedSlots) {
                    return@mapIndexedNotNull null
                }
                val rankedCandidate = rankedCandidatesFor(candidateSlot).firstOrNull(MilestoneRewardCandidate::legal) ?: return@mapIndexedNotNull null
                ReplacementSlotCandidate(
                    slot = candidateSlot,
                    priorityIndex = priorityIndex,
                    score = rankedCandidate.score,
                    exactProfessionCapstone = rankedCandidate.exactProfessionCapstone,
                    nonWeaponCapstone = rankedCandidate.nonWeaponCapstone,
                )
            }.let { replacementCandidates ->
                replacementCandidates
                    .filter(ReplacementSlotCandidate::exactProfessionCapstone)
                    .sortedWith(
                        compareByDescending<ReplacementSlotCandidate> { it.nonWeaponCapstone }
                            .thenByDescending(ReplacementSlotCandidate::score)
                            .thenBy(ReplacementSlotCandidate::priorityIndex),
                    ).firstOrNull()
                    ?: replacementCandidates.minByOrNull(ReplacementSlotCandidate::priorityIndex)
            }

    private fun evaluateCandidates(
        request: MilestoneRewardSelectionRequest,
        replacementSlot: EquipSlot?,
        professionSuitability: (ItemBaseDef) -> Boolean,
        canSatisfyAffixes: (ItemBaseDef) -> Boolean,
    ): List<MilestoneRewardCandidate> {
        val evaluationContext = candidateEvaluationContext(request)
        return request.candidateBaseIds
            .distinct()
            .map { baseItemId ->
                evaluateCandidate(
                    baseItemId = baseItemId,
                    request = request,
                    evaluationContext = evaluationContext,
                    replacementSlot = replacementSlot,
                    professionSuitability = professionSuitability,
                    canSatisfyAffixes = canSatisfyAffixes,
                )
            }.sortedWith(
                compareByDescending<MilestoneRewardCandidate> { candidate -> candidate.legal }
                    .thenByDescending { candidate -> if (candidate.legal) candidate.score else Int.MIN_VALUE }
                    .thenByDescending(MilestoneRewardCandidate::exactProfessionCapstone)
                    .thenByDescending(MilestoneRewardCandidate::nonWeaponCapstone)
                    .thenByDescending(MilestoneRewardCandidate::rarityRank)
                    .thenByDescending { candidate -> candidate.slotFamily !in evaluationContext.recentSlotFamiliesTail }
                    .thenBy(MilestoneRewardCandidate::baseItemId),
            )
    }

    private fun candidateEvaluationContext(request: MilestoneRewardSelectionRequest): CandidateEvaluationContext {
        val identity = request.selectorContext.professionId?.let(foundationBuildIdentityByProfessionId::get)
        val currentProfessionWeaponCapstoneBaseIds = identity?.capstoneBaseIds.orEmpty()
        val currentProfessionNonWeaponPayoffBaseIds = identity?.nonWeaponCapstoneBaseIds.orEmpty()
        return CandidateEvaluationContext(
            currentProfessionIdentityBaseIds = currentProfessionWeaponCapstoneBaseIds + currentProfessionNonWeaponPayoffBaseIds,
            currentProfessionNonWeaponPayoffBaseIds = currentProfessionNonWeaponPayoffBaseIds,
            currentProfessionNonWeaponOnlyBaseIds = currentProfessionNonWeaponPayoffBaseIds - currentProfessionWeaponCapstoneBaseIds,
            knownProfessionIdentityBaseIds =
                foundationBuildIdentityByProfessionId.values.flatMapTo(linkedSetOf()) { buildIdentity ->
                    buildIdentity.capstoneBaseIds + buildIdentity.nonWeaponCapstoneBaseIds
                },
            recentSlotFamiliesTail = request.selectorContext.recentSlotFamilies.takeLast(3),
            lastRecentSlotFamily = request.selectorContext.recentSlotFamilies.lastOrNull(),
        )
    }

    private fun evaluateCandidate(
        baseItemId: String,
        request: MilestoneRewardSelectionRequest,
        evaluationContext: CandidateEvaluationContext,
        replacementSlot: EquipSlot?,
        professionSuitability: (ItemBaseDef) -> Boolean,
        canSatisfyAffixes: (ItemBaseDef) -> Boolean,
    ): MilestoneRewardCandidate {
        val base = baseItemsById[baseItemId]
        if (base == null) {
            return rejectedCandidate(
                baseItemId = baseItemId,
                reason = MilestoneRewardRejectionReason.UNKNOWN_BASE,
            )
        }
        val specialTemplate = itemBundle.specialTemplateForItemId(base.id)
        val evaluation = request.selectionContext.evaluate(base)
        val catalogBackedIdentityCandidate = base.id in evaluationContext.currentProfessionIdentityBaseIds
        val syntheticExactProfessionCapstoneCandidate =
            base.id !in evaluationContext.knownProfessionIdentityBaseIds &&
                evaluation.exactProfessionMatch &&
                "capstone" in base.tags
        val currentProfessionIdentityCandidate =
            catalogBackedIdentityCandidate || syntheticExactProfessionCapstoneCandidate
        val professionCapstoneTags = specialTemplate?.tags ?: base.tags
        val exactProfessionCapstone =
            currentProfessionIdentityCandidate &&
                "capstone" in professionCapstoneTags &&
                base.id !in evaluationContext.currentProfessionNonWeaponOnlyBaseIds
        val nonWeaponCapstone =
            base.slot != EquipSlot.WEAPON &&
                (
                    base.id in evaluationContext.currentProfessionNonWeaponPayoffBaseIds ||
                        (exactProfessionCapstone && "non_weapon_capstone" in base.tags)
                )
        val wrongProfessionCapstone =
            (base.id in evaluationContext.knownProfessionIdentityBaseIds && !currentProfessionIdentityCandidate) ||
                ("capstone" in base.tags && !currentProfessionIdentityCandidate)
        val slotFamily = milestoneRewardSlotFamily(base)
        val rarityRank = rarityRank(base, specialTemplate?.specialTier)
        val baseScore = baseScore(base = base, request = request, specialTier = specialTemplate?.specialTier)
        val lateCommonMilestoneCandidate =
            isLateCommonMilestoneCandidate(base = base, specialTemplatePresent = specialTemplate != null, request = request)
        val nonWeaponPayoffBonus =
            if (nonWeaponCapstone) {
                ceilRatio(baseScore, 45, 100)
            } else {
                0
            }
        val slotRotationBonus =
            if (slotFamily != null && slotFamily !in evaluationContext.recentSlotFamiliesTail) {
                minOf(
                    ceilRatio(baseScore, 20, 100),
                    nonWeaponPayoffBonus / 2,
                )
            } else {
                0
            }
        val scoreBreakdown =
            MilestoneRewardScoreBreakdown(
                baseScore = baseScore,
                professionCapstoneBonus = if (exactProfessionCapstone) ceilRatio(baseScore, 60, 100) else 0,
                nonWeaponPayoffBonus = nonWeaponPayoffBonus,
                wrongProfessionCapstonePenalty = if (wrongProfessionCapstone) ceilRatio(baseScore, 80, 100) else 0,
                slotRotationBonus = slotRotationBonus,
                duplicateSlotPenalty =
                    if (slotFamily != null && evaluationContext.lastRecentSlotFamily == slotFamily) {
                        ceilRatio(baseScore, 25, 100)
                    } else {
                        0
                    },
                terminalIdentityBonus =
                    if (!request.selectorContext.terminalIdentitySatisfied && (exactProfessionCapstone || nonWeaponCapstone)) {
                        ceilRatio(baseScore, 35, 100)
                    } else {
                        0
                    },
                lateCommonPenalty =
                    if (lateCommonMilestoneCandidate) {
                        ceilRatio(baseScore, 35, 100)
                    } else {
                        0
                    },
            )
        val rejectionReason =
            candidateRejectionReason(
                base = base,
                specialTemplatePresent = specialTemplate != null,
                request = request,
                replacementSlot = replacementSlot,
                currentProfessionIdentityCandidate = currentProfessionIdentityCandidate,
                lateCommonMilestoneCandidate = lateCommonMilestoneCandidate,
                professionSuitability = professionSuitability,
                canSatisfyAffixes = canSatisfyAffixes,
            )
        return MilestoneRewardCandidate(
            baseItemId = base.id,
            legal = rejectionReason == null,
            rejectionReason = rejectionReason,
            scoreBreakdown = scoreBreakdown,
            specialLinkedBase = specialTemplate != null,
            exactProfessionCapstone = exactProfessionCapstone,
            nonWeaponCapstone = nonWeaponCapstone,
            slotFamily = slotFamily,
            rarityRank = rarityRank,
        )
    }

    private fun baseScore(
        base: ItemBaseDef,
        request: MilestoneRewardSelectionRequest,
        specialTier: SpecialTier?,
    ): Int {
        val sourceTierScore =
            when (request.selectorContext.sourceTier) {
                SourceTier.NORMAL -> 0
                SourceTier.ELITE -> 15
                SourceTier.CHEST -> 25
                SourceTier.BOSS -> 40
                SourceTier.SECRET_ZONE -> 45
            }
        val rarityScore =
            when {
                specialTier == SpecialTier.ARTIFACT || "artifact" in base.tags -> 160
                specialTier == SpecialTier.UNIQUE || "unique" in base.tags -> 120
                "rare" in base.tags -> 80
                else -> 0
            }
        val slotBaseValue =
            when (milestoneRewardSlotFamily(base)) {
                MilestoneRewardSlotFamily.WEAPON -> 110
                MilestoneRewardSlotFamily.OFF_HAND -> 180
                MilestoneRewardSlotFamily.ARMOR -> 120
                MilestoneRewardSlotFamily.ACCESSORY -> 90
                MilestoneRewardSlotFamily.CONSUMABLE_OR_UTILITY -> 100
                null -> 0
            }
        return (
            statBudgetScore(base) +
                rarityScore +
                slotBaseValue +
                sourceTierScore
        ).coerceAtLeast(1)
    }

    private fun statBudgetScore(base: ItemBaseDef): Int =
        with(base.baseStats) {
            attack * 2 +
                defense * 2 +
                accuracy +
                evasion +
                speed +
                castSpeedRating / 2 +
                maxHp / 4 +
                str * 4 +
                dex * 4 +
                con * 4 +
                wil * 4 +
                (talentPower * 100).toInt()
        }.coerceAtLeast(0)

    private fun rarityRank(
        base: ItemBaseDef,
        specialTier: SpecialTier?,
    ): Int =
        when {
            specialTier == SpecialTier.ARTIFACT || "artifact" in base.tags -> 3
            specialTier == SpecialTier.UNIQUE || "unique" in base.tags -> 2
            "rare" in base.tags -> 1
            else -> 0
        }

    private fun candidateRejectionReason(
        base: ItemBaseDef,
        specialTemplatePresent: Boolean,
        request: MilestoneRewardSelectionRequest,
        replacementSlot: EquipSlot?,
        currentProfessionIdentityCandidate: Boolean,
        lateCommonMilestoneCandidate: Boolean,
        professionSuitability: (ItemBaseDef) -> Boolean,
        canSatisfyAffixes: (ItemBaseDef) -> Boolean,
    ): MilestoneRewardRejectionReason? {
        val slot = base.slot ?: return MilestoneRewardRejectionReason.NON_EQUIPMENT_REWARD
        if (base.type == ItemType.CONSUMABLE) {
            return MilestoneRewardRejectionReason.NON_EQUIPMENT_REWARD
        }
        if (base.id in request.forbiddenBaseIds) {
            return MilestoneRewardRejectionReason.FORBIDDEN_BASE
        }
        if (specialTemplatePresent) {
            val template = requireNotNull(itemBundle.specialTemplateForItemId(base.id)) {
                "Special-linked milestone candidate '${base.id}' is missing its template."
            }
            if (request.selectorContext.sourceTier !in template.allowedSourceTiers) {
                return MilestoneRewardRejectionReason.SPECIAL_TEMPLATE_SOURCE_TIER_MISMATCH
            }
            if (request.selectorContext.zoneId !in template.allowedZones) {
                return MilestoneRewardRejectionReason.SPECIAL_TEMPLATE_ZONE_MISMATCH
            }
        } else if (request.selectorContext.effectiveFloorBand !in base.dropFloors) {
            return MilestoneRewardRejectionReason.DROP_FLOOR_MISMATCH
        }
        if (!professionSuitability(base)) {
            return MilestoneRewardRejectionReason.PROFESSION_MISMATCH
        }
        val hasUsableMaterial =
            base.allowedMaterials.isEmpty() ||
                itemBundle.materials.any { material ->
                    material.id in base.allowedMaterials && request.selectorContext.effectiveFloorBand >= material.minFloor
                }
        if (!hasUsableMaterial) {
            return MilestoneRewardRejectionReason.NO_USABLE_MATERIAL
        }
        if (!canSatisfyAffixes(base)) {
            return MilestoneRewardRejectionReason.AFFIX_INFEASIBLE
        }
        if (slot in request.selectorContext.reservedSlots) {
            return MilestoneRewardRejectionReason.RESERVED_SLOT
        }
        if (
            base.id in request.currentOwnedBaseIds &&
            (
                request.selectorContext.rewardSource == MilestoneRewardSource.ROUTE ||
                    request.selectorContext.rewardSource == MilestoneRewardSource.BOSS ||
                    (!currentProfessionIdentityCandidate && base.id in supportDuplicateGuardBaseIds)
            )
        ) {
            return MilestoneRewardRejectionReason.OWNED_BASE_DUPLICATE
        }
        if (replacementSlot != null && slot != replacementSlot) {
            return MilestoneRewardRejectionReason.REPLACEMENT_SLOT_MISMATCH
        }
        if (slot in request.selectorContext.occupiedSlots && slot != replacementSlot) {
            return MilestoneRewardRejectionReason.OCCUPIED_SLOT_REQUIRES_REPLACEMENT
        }
        if (
            lateCommonMilestoneCandidate &&
            request.selectorContext.professionId == "rogue" &&
            slot == EquipSlot.OFF_HAND
        ) {
            return MilestoneRewardRejectionReason.LATE_COMMON_ROGUE_OFF_HAND
        }
        return null
    }

    private fun isLateCommonMilestoneCandidate(
        base: ItemBaseDef,
        specialTemplatePresent: Boolean,
        request: MilestoneRewardSelectionRequest,
    ): Boolean =
        request.selectorContext.playerLevel >= 5 &&
            !specialTemplatePresent &&
            "unique" !in base.tags &&
            "artifact" !in base.tags

    private fun rejectedCandidate(
        baseItemId: String,
        reason: MilestoneRewardRejectionReason,
    ): MilestoneRewardCandidate =
        MilestoneRewardCandidate(
            baseItemId = baseItemId,
            legal = false,
            rejectionReason = reason,
            scoreBreakdown = MilestoneRewardScoreBreakdown(),
            specialLinkedBase = false,
            exactProfessionCapstone = false,
            nonWeaponCapstone = false,
            slotFamily = null,
            rarityRank = 0,
        )

    private fun rewardBaseTags(base: ItemBaseDef): Set<String> =
        linkedSetOf<String>().apply {
            addAll(lootBaseSemanticTags(base))
            add(base.type.name.lowercase())
            base.slot?.name?.lowercase()?.let(::add)
        }

    private fun milestoneRewardSourceBiasTags(source: MilestoneRewardSource): Set<String> =
        when (source) {
            MilestoneRewardSource.ROUTE -> setOf("reward", "route")
            MilestoneRewardSource.BOSS -> setOf("reward", "boss", "elite")
            MilestoneRewardSource.CACHE -> setOf("reward", "cache")
            MilestoneRewardSource.SUPPORT -> setOf("reward", "cache", "support")
        }

    private data class ReplacementSlotCandidate(
        val slot: EquipSlot,
        val priorityIndex: Int,
        val score: Int,
        val exactProfessionCapstone: Boolean,
        val nonWeaponCapstone: Boolean,
    )

    private data class CandidateEvaluationContext(
        val currentProfessionIdentityBaseIds: Set<String>,
        val currentProfessionNonWeaponPayoffBaseIds: Set<String>,
        val currentProfessionNonWeaponOnlyBaseIds: Set<String>,
        val knownProfessionIdentityBaseIds: Set<String>,
        val recentSlotFamiliesTail: List<MilestoneRewardSlotFamily>,
        val lastRecentSlotFamily: MilestoneRewardSlotFamily?,
    )
}
