package com.ktome.game.data

import com.ktome.game.data.schema.RewardRoutingGrantMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RewardRoutingContractTest {
    @Test
    fun `reward routing authority covers all runtime interactable reward lookups without duplicates`() {
        val entries = DataLoader().loadSchemaCatalog().rewardRoutingEntries
        val entriesByKey =
            entries.associateBy { entry ->
                RewardRoutingKey(
                    zoneId = entry.zoneId,
                    interactableId = entry.interactableId,
                    grantMode = entry.grantMode,
                )
            }

        assertEquals(entries.size, entriesByKey.size, "Reward routing authority must not contain duplicate keys.")
        assertEquals(expectedRoutingKeys(), entriesByKey.keys)
        assertEquals(
            listOf("loot.underground_river.reward", "loot.foundation.elite"),
            entriesByKey.getValue(
                RewardRoutingKey(
                    zoneId = "underground_river",
                    interactableId = "river_ferry_anchor",
                    grantMode = RewardRoutingGrantMode.SUPPORT_GRANT,
                ),
            ).profileIds,
        )
        assertEquals(
            listOf("loot.abyssal_heart.reward", "loot.foundation.boss"),
            entriesByKey.getValue(
                RewardRoutingKey(
                    zoneId = "abyssal_heart",
                    interactableId = "heart_ward_focus",
                    grantMode = RewardRoutingGrantMode.SUPPORT_GRANT,
                ),
            ).profileIds,
        )
    }

    private fun expectedRoutingKeys(): Set<RewardRoutingKey> =
        setOf(
            RewardRoutingKey("shattered_outpost", "supply_crate", RewardRoutingGrantMode.GROUND_CACHE),
            RewardRoutingKey("greenwood_fringe", "trail_cache", RewardRoutingGrantMode.GROUND_CACHE),
            RewardRoutingKey("deep_iron_pit", "ore_stash", RewardRoutingGrantMode.GROUND_CACHE),
            RewardRoutingKey("grey_gate_depths", "seal_cache", RewardRoutingGrantMode.GROUND_CACHE),
            RewardRoutingKey("bandit_camp", "bandit_cache", RewardRoutingGrantMode.GROUND_CACHE),
            RewardRoutingKey("underground_river", "crystal_cache_chest", RewardRoutingGrantMode.GROUND_CACHE),
            RewardRoutingKey("shattered_outpost", "armory_gate", RewardRoutingGrantMode.SUPPORT_GRANT),
            RewardRoutingKey("greenwood_fringe", "hunter_snare", RewardRoutingGrantMode.SUPPORT_GRANT),
            RewardRoutingKey("deep_iron_pit", "mine_furnace", RewardRoutingGrantMode.SUPPORT_GRANT),
            RewardRoutingKey("grey_gate_depths", "ritual_altar", RewardRoutingGrantMode.SUPPORT_GRANT),
            RewardRoutingKey("elven_ruins", "elven_wardstone", RewardRoutingGrantMode.SUPPORT_GRANT),
            RewardRoutingKey("molten_core", "molten_pressure_valve", RewardRoutingGrantMode.SUPPORT_GRANT),
            RewardRoutingKey("crystal_cavern", "crystal_resonance_node", RewardRoutingGrantMode.SUPPORT_GRANT),
            RewardRoutingKey("underground_river", "river_ferry_anchor", RewardRoutingGrantMode.SUPPORT_GRANT),
            RewardRoutingKey("abyssal_temple", "temple_ward_reliquary", RewardRoutingGrantMode.SUPPORT_GRANT),
            RewardRoutingKey("abyssal_heart", "heart_ward_focus", RewardRoutingGrantMode.SUPPORT_GRANT),
        )

    private data class RewardRoutingKey(
        val zoneId: String,
        val interactableId: String,
        val grantMode: RewardRoutingGrantMode,
    )
}
