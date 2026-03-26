package com.ktome.core.economy

object ShardEconomy {
    const val CURRENCY_ID: String = "shard"

    fun canAfford(
        balance: Int,
        price: Int,
    ): Boolean = balance >= price

    fun sellValue(price: Int): Int = (price / 2).coerceAtLeast(1)

    fun mandatoryAffordableOffers(
        offers: List<ShopOffer>,
        balance: Int,
        requiredTags: Set<String>,
    ): List<ShopOffer> =
        offers.filter { offer ->
            canAfford(balance, offer.price) && offer.tags.any(requiredTags::contains)
        }
}
