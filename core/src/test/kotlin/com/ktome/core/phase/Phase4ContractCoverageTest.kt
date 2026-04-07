package com.ktome.core.phase

import com.ktome.core.combat.DamageType
import com.ktome.core.combat.ElementInteractionRegistry
import com.ktome.core.ecs.BossVariantRuntime
import com.ktome.core.ecs.EliteMutationLoadout
import com.ktome.core.mapgen.TerrainOverride
import com.ktome.core.mapgen.TerrainTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Phase4ContractCoverageTest {
    @Test
    fun `phase four value contracts expose frozen versions and validate pack ids`() {
        assertEquals("P4", Phase4ContractVersions.PHASE_ID)
        assertEquals(1, Phase4ContractVersions.ELEMENT_INTERACTION_REGISTRY_VERSION)
        assertEquals(1, Phase4ContractVersions.TERRAIN_OVERRIDE_VERSION)
        assertEquals(1, Phase4ContractVersions.BOSS_VARIANT_OVERLAY_VERSION)
        assertEquals("official.pack", PackId("official.pack").toString())

        assertThrows(IllegalArgumentException::class.java) {
            PackId(" ")
        }
    }

    @Test
    fun `terrain override validates turn and damage invariants`() {
        val expired =
            TerrainOverride(
                terrainTags = setOf(TerrainTag.WATER),
                sourceRuleId = ElementInteractionRegistry.TERRAIN_COLD_WATER_FREEZE,
                remainingTurns = 0,
            )
        val active =
            TerrainOverride(
                terrainTags = setOf(TerrainTag.ICE),
                sourceRuleId = ElementInteractionRegistry.TERRAIN_FIRE_OIL_IGNITE,
                remainingTurns = 3,
                conductsLightning = true,
                tickDamageType = DamageType.FIRE,
                tickDamage = 4,
            )

        assertTrue(expired.isExpired)
        assertFalse(active.isExpired)
        assertTrue(active.conductsLightning)
        assertEquals(DamageType.FIRE, active.tickDamageType)
        assertEquals(4, active.tickDamage)

        assertThrows(IllegalArgumentException::class.java) {
            TerrainOverride(
                terrainTags = emptySet(),
                sourceRuleId = "",
                remainingTurns = 1,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TerrainOverride(
                terrainTags = setOf(TerrainTag.OIL),
                sourceRuleId = "rule",
                remainingTurns = -1,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TerrainOverride(
                terrainTags = setOf(TerrainTag.OIL),
                sourceRuleId = "rule",
                remainingTurns = 1,
                tickDamage = -1,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TerrainOverride(
                terrainTags = setOf(TerrainTag.OIL),
                sourceRuleId = "rule",
                remainingTurns = 1,
                tickDamage = 1,
            )
        }
    }

    @Test
    fun `phase four runtime components fail fast on invalid mutation and boss variant state`() {
        val loadout = EliteMutationLoadout(mutableListOf("elite.stonehide"))
        val runtime =
            BossVariantRuntime(
                variantId = "boss.variant.grey_crown",
                baseEncounterId = "dungeon_lord_encounter",
                threatCost = 3,
                lootProfileOverride = "loot.grey_gate_depths.reward",
                visualTintKey = "vfx.boss.variant.grey_crown",
                actionWeightProfileId = "boss.variant.weight.grey_crown",
            )

        assertEquals(listOf("elite.stonehide"), loadout.mutationIds)
        assertEquals("boss.variant.grey_crown", runtime.variantId)
        assertEquals("dungeon_lord_encounter", runtime.baseEncounterId)

        assertThrows(IllegalArgumentException::class.java) {
            EliteMutationLoadout(mutableListOf(""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BossVariantRuntime(
                variantId = "",
                baseEncounterId = "encounter",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            BossVariantRuntime(
                variantId = "boss.variant",
                baseEncounterId = "",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            BossVariantRuntime(
                variantId = "boss.variant",
                baseEncounterId = "encounter",
                threatCost = -1,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            BossVariantRuntime(
                variantId = "boss.variant",
                baseEncounterId = "encounter",
                lootProfileOverride = " ",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            BossVariantRuntime(
                variantId = "boss.variant",
                baseEncounterId = "encounter",
                visualTintKey = " ",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            BossVariantRuntime(
                variantId = "boss.variant",
                baseEncounterId = "encounter",
                actionWeightProfileId = " ",
            )
        }
    }
}
