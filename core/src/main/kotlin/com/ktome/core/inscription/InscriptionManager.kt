package com.ktome.core.inscription

import com.ktome.core.combat.DiminishingReturns

object InscriptionManager {
    fun canEquip(
        loadout: InscriptionLoadout,
        equippedDefinitions: List<InscriptionDef>,
        candidate: InscriptionDef,
    ): Boolean {
        if (loadout.slots.size >= MAX_INSCRIPTION_SLOTS) {
            return false
        }
        val categoryCount = equippedDefinitions.count { definition -> definition.category == candidate.category }
        return categoryCount < MAX_INSCRIPTION_PER_CATEGORY
    }

    fun equip(
        loadout: InscriptionLoadout,
        equippedDefinitions: List<InscriptionDef>,
        candidate: InscriptionDef,
    ): Boolean {
        if (!canEquip(loadout, equippedDefinitions, candidate)) {
            return false
        }
        val nextHotkey = INSCRIPTION_HOTKEY_START + loadout.slots.size
        loadout.slots += InscriptionSlot(hotkey = nextHotkey, inscriptionId = candidate.id)
        return true
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
}
