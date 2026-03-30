package com.ktome.game.harness

import com.ktome.game.PlayerCommand

internal object LoadoutPlanner {
    fun preferredLoadoutCommand(observation: RunObservation): PlayerCommand? {
        val unlockedTalentIds =
            linkedSetOf<String>().apply {
                observation.talentSlots.mapTo(this) { slot -> slot.talentId }
                observation.reserveTalents.mapTo(this) { talent -> talent.talentId }
            }
        val desiredOrder = desiredLoadoutOrder(observation, unlockedTalentIds)
        if (desiredOrder.isEmpty()) {
            return null
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

    private fun desiredLoadoutOrder(
        observation: RunObservation,
        unlockedTalentIds: Set<String>,
    ): List<String> =
        when {
            unlockedTalentIds.any(SPELLBLADE_TALENT_IDS::contains) ->
                listOf(
                    "arcane_edge",
                    "runic_edge",
                    "spell_parry",
                    "blink_strike",
                    "sunder_sigil",
                    "balance_point",
                    "counter_seal",
                    "flux_reversal",
                    "spell_rend",
                    "flux_burst",
                    "mana_lunge",
                    "flux_anchor",
                )

            observation.playerResource.typeId == "HATE" ->
                listOf(
                    "savage_hew",
                    "fault_line",
                    "slaughter_drive",
                    "pursuit_drive",
                    "riven_edge",
                    "aftershock",
                    "kill_frenzy",
                    "pain_fuel",
                    "reckless_slam",
                    "rupture_wave",
                    "last_stand",
                    "blood_rush",
                )

            observation.playerResource.typeId == "STAMINA" ->
                listOf(
                    "power_strike",
                    "shield_bash",
                    "linebreaker",
                    "earthshaker",
                    "charge",
                    "battlefield_command",
                    "war_cry",
                    "guard_stance",
                    "sweeping_strike",
                    "sunder_armor",
                    "intimidation",
                    "unyielding",
                )

            observation.playerResource.typeId == "MANA" ->
                listOf(
                    "fireball",
                    "blink",
                    "arcane_shield",
                    "void_breach",
                    "ice_bolt",
                    "glacial_seal",
                    "inferno_orb",
                    "mana_surge",
                    "ice_prison",
                    "frost_nova",
                    "flame_wall",
                )

            observation.playerResource.typeId == "ENERGY" ->
                listOf(
                    "backstab",
                    "shadow_bind",
                    "roll",
                    "eviscerate",
                    "shadowstep",
                    "stealth",
                    "poison_blade",
                    "ricochet_knives",
                    "smoke_bomb",
                    "deathblow",
                    "blade_flurry",
                    "dusk_shroud",
                )

            observation.playerResource.typeId == "POSITIVE_ENERGY" ->
                listOf(
                    "holy_strike",
                    "holy_light",
                    "consecration",
                    "sanctuary",
                    "judgment_hammer",
                    "ritual_break",
                    "holy_shield",
                    "devotion",
                    "holy_aura",
                    "divine_intervention",
                    "purify",
                )

            else -> emptyList()
        }

    private val SPELLBLADE_TALENT_IDS =
        setOf(
            "arcane_edge",
            "runic_edge",
            "sunder_sigil",
            "balance_point",
            "flux_reversal",
            "spell_rend",
            "flux_anchor",
            "flux_burst",
            "mana_lunge",
            "spell_parry",
            "blink_strike",
            "counter_seal",
        )
}
