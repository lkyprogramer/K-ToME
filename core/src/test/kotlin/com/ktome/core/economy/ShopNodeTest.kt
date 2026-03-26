package com.ktome.core.economy

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ShopNodeTest {
    @Test
    fun `shop node rejects duplicate offers`() {
        assertThrows(IllegalArgumentException::class.java) {
            ShopNode(
                id = "greenwood_supply_post",
                zoneId = "greenwood_fringe",
                nameKey = "shop.greenwood_supply_post.name",
                inventory =
                    listOf(
                        ShopOffer(id = "offer.healing_potion", itemBaseId = "healing_potion", price = 18),
                        ShopOffer(id = "offer.healing_potion", itemBaseId = "mana_potion", price = 18),
                    ),
                rescuePolicy =
                    RescueInventoryPolicy(
                        guaranteedTags = setOf("RECOVERY"),
                        affordability =
                            AffordableRescueSlotPolicy(
                                checkpointId = "greenwood_fringe",
                                expectedShardBudgetByCheckpoint = 45,
                                mandatoryAffordableItemCount = 1,
                                requiredAffordableTags = setOf("RECOVERY"),
                            ),
                    ),
            )
        }
    }
}
