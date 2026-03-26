package com.ktome.core.economy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShardEconomyTest {
    @Test
    fun `mandatory affordable offers respect budget and rescue tags`() {
        val offers =
            listOf(
                ShopOffer(id = "recovery", itemBaseId = "healing_potion", price = 18, tags = setOf("RECOVERY")),
                ShopOffer(id = "luxury", itemBaseId = "war_maul", price = 80, tags = setOf("OFFENSE")),
                ShopOffer(id = "movement", inscriptionId = "phase_door", price = 42, tags = setOf("MOVEMENT")),
            )

        val affordable = ShardEconomy.mandatoryAffordableOffers(offers, balance = 45, requiredTags = setOf("RECOVERY", "MOVEMENT"))

        assertEquals(listOf("recovery", "movement"), affordable.map(ShopOffer::id))
        assertTrue(ShardEconomy.canAfford(balance = 45, price = 42))
        assertFalse(ShardEconomy.canAfford(balance = 45, price = 46))
    }

    @Test
    fun `sell value has a non zero floor`() {
        assertEquals(1, ShardEconomy.sellValue(0))
        assertEquals(9, ShardEconomy.sellValue(18))
    }
}
