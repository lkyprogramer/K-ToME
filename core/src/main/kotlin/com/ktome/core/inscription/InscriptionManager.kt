package com.ktome.core.inscription

import com.ktome.core.combat.DiminishingReturns
import com.ktome.core.event.InscriptionReplacedEvent
import kotlin.math.ceil

enum class InscriptionEquipFailure {
    FULL_REQUIRES_REPLACEMENT,
    CATEGORY_LIMIT,
    TARGET_SLOT_MISSING,
    SAME_INSCRIPTION,
}

sealed interface InscriptionEquipCheck {
    data object Allowed : InscriptionEquipCheck

    data class Rejected(
        val reason: InscriptionEquipFailure,
    ) : InscriptionEquipCheck
}

sealed interface InscriptionReplaceOutcome {
    data class Applied(
        val newLoadout: InscriptionLoadout,
        val newCooldownState: InscriptionCooldownState,
        val event: InscriptionReplacedEvent,
    ) : InscriptionReplaceOutcome

    data class Rejected(
        val reason: InscriptionEquipFailure,
    ) : InscriptionReplaceOutcome
}

data class InscriptionReplaceRequest(
    val loadout: InscriptionLoadout,
    val cooldowns: InscriptionCooldownState,
    val equippedDefinitions: List<InscriptionDef>,
    val candidate: InscriptionDef,
    val targetHotkey: Int,
)

object InscriptionManager {
    fun canEquip(
        loadout: InscriptionLoadout,
        equippedDefinitions: List<InscriptionDef>,
        candidate: InscriptionDef,
    ): InscriptionEquipCheck {
        if (loadout.slots.size >= MAX_INSCRIPTION_SLOTS) {
            return InscriptionEquipCheck.Rejected(InscriptionEquipFailure.FULL_REQUIRES_REPLACEMENT)
        }
        val categoryCount = equippedDefinitions.count { definition -> definition.category == candidate.category }
        return if (categoryCount < MAX_INSCRIPTION_PER_CATEGORY) {
            InscriptionEquipCheck.Allowed
        } else {
            InscriptionEquipCheck.Rejected(InscriptionEquipFailure.CATEGORY_LIMIT)
        }
    }

    fun equip(
        loadout: InscriptionLoadout,
        equippedDefinitions: List<InscriptionDef>,
        candidate: InscriptionDef,
    ): Boolean {
        if (canEquip(loadout, equippedDefinitions, candidate) !is InscriptionEquipCheck.Allowed) {
            return false
        }
        val nextHotkey = INSCRIPTION_HOTKEY_START + loadout.slots.size
        loadout.slots += InscriptionSlot(hotkey = nextHotkey, inscriptionId = candidate.id)
        return true
    }

    fun canReplace(
        loadout: InscriptionLoadout,
        equippedDefinitions: List<InscriptionDef>,
        candidate: InscriptionDef,
        targetHotkey: Int,
    ): InscriptionEquipCheck {
        val targetSlot = loadout.slots.firstOrNull { slot -> slot.hotkey == targetHotkey }
            ?: return InscriptionEquipCheck.Rejected(InscriptionEquipFailure.TARGET_SLOT_MISSING)
        if (targetSlot.inscriptionId == candidate.id) {
            return InscriptionEquipCheck.Rejected(InscriptionEquipFailure.SAME_INSCRIPTION)
        }
        if (loadout.slots.any { slot -> slot.hotkey != targetHotkey && slot.inscriptionId == candidate.id }) {
            return InscriptionEquipCheck.Rejected(InscriptionEquipFailure.SAME_INSCRIPTION)
        }
        val targetDefinition =
            requireNotNull(equippedDefinitions.firstOrNull { definition -> definition.id == targetSlot.inscriptionId }) {
                "Inscription replacement target '${targetSlot.inscriptionId}' is missing from equipped definitions."
            }
        val currentCandidateCategoryCount = equippedDefinitions.count { definition -> definition.category == candidate.category }
        val postReplaceCategoryCount =
            currentCandidateCategoryCount -
                (if (targetDefinition.category == candidate.category) 1 else 0) +
                1
        return if (postReplaceCategoryCount <= MAX_INSCRIPTION_PER_CATEGORY) {
            InscriptionEquipCheck.Allowed
        } else {
            InscriptionEquipCheck.Rejected(InscriptionEquipFailure.CATEGORY_LIMIT)
        }
    }

    fun replace(request: InscriptionReplaceRequest): InscriptionReplaceOutcome {
        val check = canReplace(request.loadout, request.equippedDefinitions, request.candidate, request.targetHotkey)
        if (check is InscriptionEquipCheck.Rejected) {
            return InscriptionReplaceOutcome.Rejected(check.reason)
        }
        val targetSlot = requireNotNull(request.loadout.slots.firstOrNull { slot -> slot.hotkey == request.targetHotkey })
        val nextLoadout =
            InscriptionLoadout(
                slots =
                    request.loadout.slots.map { slot ->
                        if (slot.hotkey == request.targetHotkey) {
                            slot.copy(inscriptionId = request.candidate.id)
                        } else {
                            slot
                        }
                    }.toMutableList(),
            )
        val nextCooldowns =
            InscriptionCooldownState(
                request.cooldowns.remainingByInscriptionId
                    .filterKeys { inscriptionId -> inscriptionId != targetSlot.inscriptionId }
                    .toMutableMap(),
            )
        nextCooldowns.remainingByInscriptionId[request.candidate.id] = replacementInitialCooldown(request.candidate)
        return InscriptionReplaceOutcome.Applied(
            newLoadout = nextLoadout,
            newCooldownState = nextCooldowns,
            event =
                InscriptionReplacedEvent(
                    hotkey = request.targetHotkey,
                    oldInscriptionId = targetSlot.inscriptionId,
                    newInscriptionId = request.candidate.id,
                ),
        )
    }

    fun startCooldown(
        cooldowns: InscriptionCooldownState,
        inscription: InscriptionDef,
        effectiveCastSpeed: Double = 0.0,
    ) {
        cooldowns.remainingByInscriptionId[inscription.id] =
            DiminishingReturns.adjustedCooldownTurns(
                baseCooldown = inscription.cooldown,
                effectiveCastSpeed = effectiveCastSpeed,
            )
    }

    fun isOnCooldown(
        cooldowns: InscriptionCooldownState,
        inscriptionId: String,
    ): Boolean = (cooldowns.remainingByInscriptionId[inscriptionId] ?: 0) > 0

    fun tickCooldowns(cooldowns: InscriptionCooldownState) {
        cooldowns.remainingByInscriptionId.keys.toList().forEach { inscriptionId ->
            val remaining = (cooldowns.remainingByInscriptionId[inscriptionId] ?: 0) - 1
            if (remaining <= 0) {
                cooldowns.remainingByInscriptionId.remove(inscriptionId)
            } else {
                cooldowns.remainingByInscriptionId[inscriptionId] = remaining
            }
        }
    }

    private fun replacementInitialCooldown(candidate: InscriptionDef): Int =
        ceil(candidate.cooldown * 0.5).toInt().coerceAtLeast(1)
}
