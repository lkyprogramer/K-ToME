package com.ktome.game.harness

import com.ktome.game.PlayerCommand

internal object LoadoutPlanner {
    fun preferredLoadoutCommand(observation: RunObservation): PlayerCommand? {
        val desiredOrder = desiredLoadoutOrder(observation)
        if (desiredOrder.isEmpty()) {
            return null
        }
        val unlockedTalentIds =
            linkedSetOf<String>().apply {
                observation.talentSlots.mapTo(this) { slot -> slot.talentId }
                observation.reserveTalents.mapTo(this) { talent -> talent.talentId }
            }
        desiredOrder
            .filter(unlockedTalentIds::contains)
            .take(4)
            .forEachIndexed { index, talentId ->
                val targetSlot = index + 1
                val currentTalentId = observation.talentSlots.firstOrNull { slot -> slot.slot == targetSlot }?.talentId
                if (currentTalentId != talentId) {
                    return PlayerCommand.EquipTalentToSlot(slot = targetSlot, talentId = talentId)
                }
            }
        return null
    }

    private fun desiredLoadoutOrder(observation: RunObservation): List<String> =
        when (observation.playerResource.typeId) {
            "STAMINA" ->
                listOf(
                    "power_strike",
                    "shield_bash",
                    "charge",
                    "war_cry",
                    "guard_stance",
                    "sweeping_strike",
                    "sunder_armor",
                    "intimidation",
                    "unyielding",
                )

            "MANA" ->
                listOf(
                    "fireball",
                    "blink",
                    "arcane_shield",
                    "ice_bolt",
                    "mana_surge",
                    "ice_prison",
                    "frost_nova",
                    "flame_wall",
                )

            "ENERGY" ->
                listOf(
                    "backstab",
                    "poison_blade",
                    "shadowstep",
                    "roll",
                    "stealth",
                    "smoke_bomb",
                    "blade_flurry",
                    "deathblow",
                )

            "POSITIVE_ENERGY" ->
                listOf(
                    "holy_strike",
                    "holy_light",
                    "judgment_hammer",
                    "holy_shield",
                    "devotion",
                    "holy_aura",
                    "divine_intervention",
                    "purify",
                )

            else -> emptyList()
        }
}
