package com.ktome.game.loot

import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemType
import com.ktome.core.item.MilestoneRewardSource

class MilestoneRewardSelector(
    private val itemBundle: ItemDataBundle,
) {
    private val baseItemsById: Map<String, ItemBaseDef> = itemBundle.baseItems.associateBy(ItemBaseDef::id)

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
        val preferenceIndices = request.rewardPreferenceOrder.withIndex().associate { (index, baseId) -> baseId to index }
        return request.candidateBaseIds
            .distinct()
            .map { baseItemId ->
                evaluateCandidate(
                    baseItemId = baseItemId,
                    request = request,
                    replacementSlot = replacementSlot,
                    professionSuitability = professionSuitability,
                    canSatisfyAffixes = canSatisfyAffixes,
                )
            }.sortedWith(
                compareByDescending<MilestoneRewardCandidate> { candidate -> candidate.legal }
                    .thenByDescending { candidate -> if (candidate.legal) candidate.score else Int.MIN_VALUE }
                    .thenBy { candidate -> preferenceIndices[candidate.baseItemId] ?: Int.MAX_VALUE }
                    .thenBy(MilestoneRewardCandidate::baseItemId),
            )
    }

    private fun evaluateCandidate(
        baseItemId: String,
        request: MilestoneRewardSelectionRequest,
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
        val exactProfessionCapstone = evaluation.exactProfessionMatch && "capstone" in base.tags
        val nonWeaponCapstone = exactProfessionCapstone && "non_weapon_capstone" in base.tags && base.slot != EquipSlot.WEAPON
        val scoreBreakdown =
            MilestoneRewardScoreBreakdown(
                poolWeightScore = (request.poolWeightByBaseId[base.id] ?: base.dropWeight.coerceAtLeast(1)) * 10,
                freshBonus =
                    if (base.id !in request.currentOwnedBaseIds && evaluation.matchStrength != LootBaseBuildMatchStrength.NONE) {
                        40
                    } else {
                        0
                    },
                buildMatchScore =
                    when (evaluation.matchStrength) {
                        LootBaseBuildMatchStrength.NONE -> 0
                        LootBaseBuildMatchStrength.WEAK -> 30
                        LootBaseBuildMatchStrength.STRONG -> 70
                    },
                exactProfessionScore = if (evaluation.exactProfessionMatch) 35 else 0,
                professionCapstoneScore = if (exactProfessionCapstone) 55 else 0,
                nonWeaponAnchorScore = if (nonWeaponCapstone) 40 else 0,
                preferredRewardSourceScore =
                    if (exactProfessionCapstone && request.selectorContext.rewardSource in request.preferredRewardSources) {
                        PREFERRED_REWARD_SOURCE_SCORE
                    } else {
                        0
                    },
                routeBiasScore = rewardBaseTags(base).count(request.selectorContext.routeBiasTags::contains) * 12,
                rewardBiasScore = rewardBaseTags(base).count(milestoneRewardSourceBiasTags(request.selectorContext.rewardSource)::contains) * 8,
                antiCollapsePenalty =
                    if (evaluation.antiCollapseMultiplierBasisPoints < 10_000) {
                        30
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
        )
    }

    private fun candidateRejectionReason(
        base: ItemBaseDef,
        specialTemplatePresent: Boolean,
        request: MilestoneRewardSelectionRequest,
        replacementSlot: EquipSlot?,
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
        if (slot in request.selectorContext.occupiedSlots && slot != replacementSlot) {
            return MilestoneRewardRejectionReason.OCCUPIED_SLOT_REQUIRES_REPLACEMENT
        }
        return null
    }

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
}

private const val PREFERRED_REWARD_SOURCE_SCORE: Int = 50
