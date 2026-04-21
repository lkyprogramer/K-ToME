package com.ktome.game.hidden

import com.ktome.core.world.solvability.ContentRef
import com.ktome.core.world.solvability.RegistryId
import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecretRewardAuthorityTest {
    private val catalog by lazy(LazyThreadSafetyMode.NONE) { DataLoader().loadSchemaCatalog() }

    @Test
    fun `resolver keeps secret zone reward pinned to secret zone definition and flags mixed payloads`() {
        val secretZone = requireNotNull(catalog.secretZones.firstOrNull { zone -> zone.id.id == "underground_river_crystal_rift" })
        val hiddenEvent = requireNotNull(catalog.hiddenEvents.firstOrNull { event -> event.id == "hidden.event.underground_river.crystal_rift.reward" })
        val resolved = SecretRewardAuthority.resolve(secretZone = secretZone, hiddenEvent = hiddenEvent)
        val mixedPayloadEvent =
            hiddenEvent.copy(
                rewards =
                    hiddenEvent.rewards +
                        HiddenEventReward(
                            key = HiddenEventRewardKey.LOOT_PROFILE,
                            payload =
                                HiddenEventRewardPayload.LootProfile(
                                    lootProfileRef = ContentRef(registry = RegistryId(LOOT_PROFILE_REGISTRY_ID), id = "loot.foundation.common"),
                                ),
                        ),
            )
        val mismatched = SecretRewardAuthority.resolve(secretZone = secretZone, hiddenEvent = mixedPayloadEvent)

        assertEquals(secretZone.rewardProfileId.id, resolved.rewardProfileId)
        assertEquals(SecretRewardAuthorityResolutionSource.SECRET_ZONE_DEF, resolved.source)
        assertNull(resolved.mismatchReason)
        assertEquals(secretZone.rewardProfileId.id, mismatched.rewardProfileId)
        assertEquals(SecretRewardAuthorityResolutionSource.MISMATCH, mismatched.source)
        assertEquals("loot_profile_present", mismatched.mismatchReason)
    }

    @Test
    fun `assertion scanner and reward-structure summary keep secret reward authority contract stable`() {
        val secretZone = requireNotNull(catalog.secretZones.firstOrNull { zone -> zone.id.id == "underground_river_crystal_rift" })
        val rewardStructureKeys = SecretRewardAuthorityAssertions.rewardStructureKeysByProfileId(catalog)
        val missingHiddenEventCatalog =
            catalog.copy(
                secretZones =
                    catalog.secretZones.map { zone ->
                        if (zone.id == secretZone.id) {
                            zone.copy(
                                guaranteedContent =
                                    listOf(ContentRef(registry = RegistryId(HIDDEN_EVENT_REGISTRY_ID), id = "hidden.event.missing.secret.reward")),
                            )
                        } else {
                            zone
                        }
                    },
            )
        val violations = SecretRewardAuthorityAssertions.scanCatalog(missingHiddenEventCatalog)

        assertTrue(rewardStructureKeys.getValue(secretZone.rewardProfileId.id).contains("SECRET_ZONE_REWARD"))
        assertEquals(1, violations.size)
        assertEquals("${secretZone.id.id}:hidden.event.missing.secret.reward", violations.single().culpritId)
        assertEquals("missing_hidden_event", violations.single().reason)
        assertTrue(violations.single().validationMessage().contains("is missing"))
    }
}
