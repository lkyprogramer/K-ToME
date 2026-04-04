package com.ktome.tools.lint

import com.ktome.core.mapgen.MapgenTemplateCatalog
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.TerrainTag
import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("contractLint")
class Phase4MapgenContractLintTest {
    @Test
    fun `phase4 mapgen content keeps cross references and frozen reward values aligned`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val roomIds = catalog.roomDefs.map { room -> room.id }.toSet()
        val patternTemplateIds = catalog.patternTemplates.map { template -> template.id }.toSet()
        val vaultTemplateIds = catalog.vaultTemplates.map { template -> template.id }.toSet()
        val biomeFamilyIds = catalog.biomeFamilies.map { family -> family.id }.toSet()
        val mapgenProfilesById = catalog.zoneMapgenProfiles.associateBy { profile -> profile.id }
        val rewardProfilesById = catalog.zoneRewardProfiles.associateBy { profile -> profile.id }
        val zonesById = catalog.zones.associateBy { zone -> zone.id }

        catalog.patternRooms.forEach { patternRoom ->
            assertTrue(patternRoom.baseRoomId in roomIds, "Pattern room ${patternRoom.id} references unknown base room ${patternRoom.baseRoomId}.")
            assertTrue(patternRoom.patternId in patternTemplateIds, "Pattern room ${patternRoom.id} references unknown pattern template ${patternRoom.patternId}.")
            assertTrue(patternRoom.patternId in MapgenTemplateCatalog.supportedPatternIds, "Pattern room ${patternRoom.id} uses unsupported runtime pattern ${patternRoom.patternId}.")
        }
        val patternTemplatesById = catalog.patternTemplates.associateBy { template -> template.id }
        assertTrue(patternTemplatesById.getValue("boardwalk_lane").rows.any { row -> '~' in row })
        assertTrue(patternTemplatesById.getValue("shallow_channels").rows.any { row -> '~' in row })
        assertTrue(patternTemplatesById.getValue("forge_aisles").rows.any { row -> 'o' in row })
        assertTrue(patternTemplatesById.getValue("ring_walk").rows.any { row -> '*' in row })
        assertTrue(patternTemplatesById.getValue("sanctum_ring").rows.any { row -> 'o' in row })

        catalog.vaults.forEach { vault ->
            assertTrue(vault.templateId in vaultTemplateIds, "Vault ${vault.id} references unknown vault template ${vault.templateId}.")
            assertTrue(vault.templateId in MapgenTemplateCatalog.supportedVaultTemplateIds, "Vault ${vault.id} uses unsupported runtime vault template ${vault.templateId}.")
            assertTrue(vault.allowOnBiomeFamilies.isNotEmpty(), "Vault ${vault.id} must allow at least one biome family.")
            assertTrue(vault.allowOnBiomeFamilies.all(biomeFamilyIds::contains), "Vault ${vault.id} references unknown biome family.")
            assertTrue(vault.requiredTerrainTags.all { tag -> tag in setOf(TerrainTag.WATER, TerrainTag.OIL, TerrainTag.ICE) })
            assertTrue(vault.rewardBudget >= 0, "Vault ${vault.id} must not have a negative reward budget.")
            if (vault.pathClass == PathClass.CRITICAL_PATH) {
                assertEquals(0, vault.rewardBudget, "Critical-path vault ${vault.id} must not inject optional reward budget.")
            }
        }

        catalog.zoneMapgenProfiles.forEach { profile ->
            assertTrue(profile.allowedBiomeFamilies.isNotEmpty(), "Zone mapgen profile ${profile.id} must declare at least one biome family.")
            assertTrue(profile.allowedBiomeFamilies.all(biomeFamilyIds::contains), "Zone mapgen profile ${profile.id} references unknown biome family.")
            assertTrue(profile.vaultPool.all { vaultId -> catalog.vaults.any { vault -> vault.id == vaultId } }, "Zone mapgen profile ${profile.id} references unknown vault id.")
        }

        assertFrozenZoneProfile(
            zone = requireNotNull(zonesById["greenwood_fringe"]),
            expectedMapgenProfileId = "zone_mapgen.greenwood_fringe.phase4",
            expectedRewardProfileId = "zone_reward.greenwood_fringe.phase4",
            rewardProfilesById = rewardProfilesById,
            mapgenProfilesById = mapgenProfilesById,
            expectedRarityBonus = 0.00f,
            expectedQualityBonus = 0,
        )
        assertFrozenZoneProfile(
            zone = requireNotNull(zonesById["deep_iron_pit"]),
            expectedMapgenProfileId = "zone_mapgen.deep_iron_pit.phase4",
            expectedRewardProfileId = "zone_reward.deep_iron_pit.phase4",
            rewardProfilesById = rewardProfilesById,
            mapgenProfilesById = mapgenProfilesById,
            expectedRarityBonus = 0.05f,
            expectedQualityBonus = 1,
        )
        assertFrozenZoneProfile(
            zone = requireNotNull(zonesById["underground_river"]),
            expectedMapgenProfileId = "zone_mapgen.underground_river.phase4",
            expectedRewardProfileId = "zone_reward.underground_river.phase4",
            rewardProfilesById = rewardProfilesById,
            mapgenProfilesById = mapgenProfilesById,
            expectedRarityBonus = 0.08f,
            expectedQualityBonus = 1,
        )
        assertFrozenZoneProfile(
            zone = requireNotNull(zonesById["abyssal_temple"]),
            expectedMapgenProfileId = "zone_mapgen.abyssal_temple.phase4",
            expectedRewardProfileId = "zone_reward.abyssal_temple.phase4",
            rewardProfilesById = rewardProfilesById,
            mapgenProfilesById = mapgenProfilesById,
            expectedRarityBonus = 0.12f,
            expectedQualityBonus = 2,
        )
    }

    private fun assertFrozenZoneProfile(
        zone: com.ktome.game.data.schema.ZoneSchemaV2,
        expectedMapgenProfileId: String,
        expectedRewardProfileId: String,
        rewardProfilesById: Map<String, com.ktome.core.mapgen.ZoneRewardProfile>,
        mapgenProfilesById: Map<String, com.ktome.core.mapgen.ZoneMapgenProfile>,
        expectedRarityBonus: Float,
        expectedQualityBonus: Int,
    ) {
        assertEquals(expectedMapgenProfileId, zone.mapgenProfileId, "Zone ${zone.id} must stay pinned to the PR-02 mapgen profile id.")
        assertEquals(expectedRewardProfileId, zone.rewardProfileId, "Zone ${zone.id} must stay pinned to the PR-02 reward profile id.")
        assertTrue(expectedMapgenProfileId in mapgenProfilesById, "Zone ${zone.id} references an unknown mapgen profile.")
        val rewardProfile = requireNotNull(rewardProfilesById[expectedRewardProfileId]) { "Zone ${zone.id} reward profile missing." }
        assertEquals(expectedRarityBonus, rewardProfile.rarityBonus, "Zone ${zone.id} rarity bonus drifted from PR-02 freeze table.")
        assertEquals(expectedQualityBonus, rewardProfile.qualityBonus, "Zone ${zone.id} quality bonus drifted from PR-02 freeze table.")
    }
}
