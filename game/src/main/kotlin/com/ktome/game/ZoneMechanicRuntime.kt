package com.ktome.game

import com.ktome.game.data.schema.ZoneSchemaV2

internal object ZoneMechanicRuntime {
    fun introHintKey(zone: ZoneSchemaV2): String? =
        when {
            "ambush_lane" in zone.specialMechanics -> "zone.mechanic_hint.ambush_lane"
            "lore_cache" in zone.specialMechanics -> "zone.mechanic_hint.lore_cache"
            "ore_cart" in zone.specialMechanics -> "zone.mechanic_hint.ore_cart"
            "lava_pockets" in zone.specialMechanics -> "zone.mechanic_hint.lava_pockets"
            "sealed_gate" in zone.specialMechanics -> "zone.mechanic_hint.sealed_gate"
            "currents" in zone.specialMechanics -> "zone.mechanic_hint.currents"
            "crystal_shards" in zone.specialMechanics -> "zone.mechanic_hint.crystal_shards"
            "abyssal_ward" in zone.specialMechanics -> "zone.mechanic_hint.abyssal_ward"
            "finale" in zone.specialMechanics -> "zone.mechanic_hint.finale"
            zone.environmentTheme == "tutorial_ruins" -> "zone.mechanic_hint.tutorial_ruins"
            zone.environmentTheme == "forest_patrol" -> "zone.mechanic_hint.forest_patrol"
            zone.environmentTheme == "mine_forge" -> "zone.mechanic_hint.mine_forge"
            zone.environmentTheme == "sealed_depths" -> "zone.mechanic_hint.sealed_depths"
            else -> null
        }

    fun uniqueContentRewardProfiles(uniqueContentTag: String?): List<String> =
        when (uniqueContentTag) {
            "optional.bandit_camp.cache" -> listOf("loot.greenwood_fringe.reward", "loot.foundation.elite")
            "optional.elven_ruins.relic" -> listOf("loot.grey_gate_depths.reward", "loot.foundation.boss")
            "optional.molten_core.relic" -> listOf("loot.deep_iron_pit.reward", "loot.foundation.elite")
            "optional.crystal_cavern.node" -> listOf("loot.grey_gate_depths.reward", "loot.foundation.elite")
            else -> emptyList()
        }

    fun uniqueContentFallbackBaseId(uniqueContentTag: String?): String =
        when (uniqueContentTag) {
            "optional.bandit_camp.cache" -> "bandit_trophy"
            "optional.elven_ruins.relic" -> "seal_reliquary"
            "optional.molten_core.relic" -> "forgebreaker_pick"
            "optional.crystal_cavern.node" -> "emerald_charm"
            else -> "healing_potion"
        }
}
