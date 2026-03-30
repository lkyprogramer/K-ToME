package com.ktome.core.economy

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ShopNodeTest {
    @Test
    fun `shop node rejects duplicate offers across inventory and refresh inventory`() {
        assertThrows(IllegalArgumentException::class.java) {
            ShopNode(
                id = "greenwood_supply_post",
                zoneId = "greenwood_fringe",
                nameKey = "shop.greenwood_supply_post.name",
                inventory =
                    listOf(
                        ShopOffer(id = "offer.healing_potion", itemBaseId = "healing_potion", price = 18),
                    ),
                refreshInventory = listOf(ShopOffer(id = "offer.healing_potion", itemBaseId = "mana_potion", price = 18)),
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

    @Test
    fun `shop offer requires exactly one item inscription or service source`() {
        assertThrows(IllegalArgumentException::class.java) {
            ShopOffer(
                id = "offer.invalid",
                itemBaseId = "healing_potion",
                inscriptionId = "phase_door",
                price = 10,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShopOffer(
                id = "offer.missing",
                price = 10,
            )
        }
        ShopOffer(
            id = "offer.refresh",
            serviceType = ShopServiceType.REFRESH_STOCK,
            price = 35,
            tags = setOf("SERVICE", "REFRESH_STOCK"),
        )
    }
}
