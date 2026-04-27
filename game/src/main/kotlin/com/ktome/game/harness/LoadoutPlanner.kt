package com.ktome.game.harness

import com.ktome.game.PlayerCommand

internal object LoadoutPlanner {
    fun preferredLoadoutCommand(observation: RunObservation): PlayerCommand? {
        val learnedTalentIds =
            linkedSetOf<String>().apply {
                observation.talentSlots.mapTo(this) { slot -> slot.talentId }
                observation.reserveTalents.mapTo(this) { talent -> talent.talentId }
            }
        val desiredOrder = desiredLoadoutOrder(observation, learnedTalentIds)
        if (desiredOrder.isEmpty()) {
            return null
        }
        desiredOrder
            .filter(learnedTalentIds::contains)
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
        learnedTalentIds: Set<String>,
    ): List<String> =
        when {
            learnedTalentIds.any(SPELLBLADE_TALENT_IDS::contains) ->
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
                    "guard_stance",
                    "linebreaker",
                    "taunt",
                    "charge",
                    "earthshaker",
                    "battlefield_command",
                    "war_cry",
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
                    "shadowstep",
                    "eviscerate",
                    "roll",
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
                    "holy_mark",
                    "consecration",
                    "judgment_hammer",
                    "sanctuary",
                    "ritual_break",
                    "holy_shield",
                    "purify",
                    "devotion",
                    "holy_aura",
                    "divine_intervention",
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
