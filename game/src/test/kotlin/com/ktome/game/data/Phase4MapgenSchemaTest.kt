package com.ktome.game.data

import com.ktome.core.mapgen.PathClass

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Phase4MapgenSchemaTest {
    private val catalog = DataLoader().loadSchemaCatalog()

    @Test
    fun `phase4 upgraded zones bind documented mapgen and reward profiles`() {
        val expectedFamiliesByZone =
            mapOf(
                "greenwood_fringe" to listOf("forest", "bog"),
                "deep_iron_pit" to listOf("mine", "forge"),
                "underground_river" to listOf("flooded_cavern", "crystal_bank"),
                "abyssal_temple" to listOf("ruin", "oil_catacomb"),
            )
        val expectedRewardHints =
            mapOf(
                "greenwood_fringe" to Triple(0.00f, 0, 6),
                "deep_iron_pit" to Triple(0.05f, 1, 8),
                "underground_river" to Triple(0.08f, 1, 10),
                "abyssal_temple" to Triple(0.12f, 2, 12),
            )
        val profilesById = catalog.zoneMapgenProfiles.associateBy { profile -> profile.id }
        val rewardsById = catalog.zoneRewardProfiles.associateBy { profile -> profile.id }

        expectedFamiliesByZone.forEach { (zoneId, expectedFamilies) ->
            val zone = catalog.zones.first { candidate -> candidate.id == zoneId }
            val mapgenProfile = requireNotNull(zone.mapgenProfileId?.let(profilesById::get)) { "Missing mapgen profile for $zoneId." }
            val rewardProfile = requireNotNull(zone.rewardProfileId?.let(rewardsById::get)) { "Missing reward profile for $zoneId." }

            assertEquals(expectedFamilies, mapgenProfile.allowedBiomeFamilies.toList())
            assertEquals(zoneId, mapgenProfile.zoneId)
            assertEquals(zoneId, rewardProfile.zoneId)
            assertEquals(expectedRewardHints.getValue(zoneId).first, rewardProfile.rarityBonus)
            assertEquals(expectedRewardHints.getValue(zoneId).second, rewardProfile.qualityBonus)
            assertEquals(expectedRewardHints.getValue(zoneId).third, rewardProfile.baseRewardBudget)
        }
    }

    @Test
    fun `phase4 mapgen data keeps template and vault lint invariants`() {
        val roomIds = catalog.roomDefs.map { room -> room.id }.toSet()
        val patternTemplateIds = catalog.patternTemplates.map { template -> template.id }.toSet()
        val vaultTemplateIds = catalog.vaultTemplates.map { template -> template.id }.toSet()
        val biomeFamilyIds = catalog.biomeFamilies.map { family -> family.id }.toSet()
        val targetProfileIds = catalog.zoneMapgenProfiles.map { profile -> profile.id }.toSet()
        val rewardProfileIds = catalog.zoneRewardProfiles.map { profile -> profile.id }.toSet()

        catalog.patternRooms.forEach { pattern ->
            assertTrue(pattern.baseRoomId in roomIds, "Pattern '${pattern.id}' references unknown room '${pattern.baseRoomId}'.")
            assertTrue(pattern.patternId in patternTemplateIds, "Pattern '${pattern.id}' references unknown template '${pattern.patternId}'.")
        }
        catalog.vaults.forEach { vault ->
            assertTrue(vault.templateId in vaultTemplateIds, "Vault '${vault.id}' references unknown template '${vault.templateId}'.")
            assertTrue(vault.allowOnBiomeFamilies.isNotEmpty(), "Vault '${vault.id}' must target at least one biome family.")
            assertTrue(vault.allowOnBiomeFamilies.all(biomeFamilyIds::contains), "Vault '${vault.id}' references unknown biome family.")
            assertTrue(vault.pathClass != PathClass.CRITICAL_PATH || vault.rewardBudget == 0, "Critical-path vault '${vault.id}' must not carry reward budget.")
        }
        catalog.zoneMapgenProfiles.forEach { profile ->
            assertTrue(profile.allowedBiomeFamilies.size <= 2, "Mapgen profile '${profile.id}' must not mix more than two biome families.")
            assertTrue(profile.allowedBiomeFamilies.all(biomeFamilyIds::contains), "Mapgen profile '${profile.id}' references unknown biome family.")
            assertTrue(profile.vaultPool.all { vaultId -> catalog.vaults.any { vault -> vault.id == vaultId } }, "Mapgen profile '${profile.id}' references unknown vault.")
        }
        listOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple").forEach { zoneId ->
            val zone = catalog.zones.first { candidate -> candidate.id == zoneId }
            assertNotNull(zone.mapgenProfileId)
            assertNotNull(zone.rewardProfileId)
            assertTrue(zone.mapgenProfileId in targetProfileIds, "Zone '$zoneId' should bind an explicit mapgen profile.")
            assertTrue(zone.rewardProfileId in rewardProfileIds, "Zone '$zoneId' should bind an explicit reward profile.")
        }
    }
}
