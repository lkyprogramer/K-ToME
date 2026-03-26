package com.ktome.core.economy

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AffordableRescueInventoryPolicyTest {
    @Test
    fun `checkpoint rescue policy keeps required tags affordable within budget`() {
        val policy =
            AffordableRescueSlotPolicy(
                checkpointId = "deep_iron_pit",
                expectedShardBudgetByCheckpoint = 80,
                mandatoryAffordableItemCount = 2,
                requiredAffordableTags = setOf("MOVEMENT", "CLEANSING", "PROTECTION"),
            )
        val offers =
            listOf(
                ShopOffer(id = "phase-door", inscriptionId = "phase_door", price = 58, tags = setOf("MOVEMENT")),
                ShopOffer(id = "purge", inscriptionId = "purge", price = 54, tags = setOf("CLEANSING")),
                ShopOffer(id = "chain-mail", itemBaseId = "chain_mail", price = 62, tags = setOf("PROTECTION")),
                ShopOffer(id = "war-maul", itemBaseId = "war_maul", price = 96, tags = setOf("OFFENSE")),
            )

        val affordable =
            ShardEconomy.mandatoryAffordableOffers(
                offers = offers,
                balance = policy.expectedShardBudgetByCheckpoint,
                requiredTags = policy.requiredAffordableTags,
            )

        assertTrue(affordable.size >= policy.mandatoryAffordableItemCount)
        val affordableTags = affordable.flatMapTo(linkedSetOf()) { offer -> offer.tags }
        assertTrue("MOVEMENT" in affordableTags)
        assertTrue("CLEANSING" in affordableTags || "PROTECTION" in affordableTags)
    }
}
