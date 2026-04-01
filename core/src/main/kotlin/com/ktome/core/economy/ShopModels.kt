package com.ktome.core.economy

import kotlinx.serialization.Serializable

@Serializable
data class ShopNode(
    val id: String,
    val zoneId: String,
    val nameKey: String,
    val inventory: List<ShopOffer>,
    val refreshInventory: List<ShopOffer> = emptyList(),
    val rescuePolicy: RescueInventoryPolicy,
) {
    init {
        require(id.isNotBlank()) { "ShopNode.id must not be blank." }
        require(zoneId.isNotBlank()) { "ShopNode.zoneId must not be blank." }
        require(nameKey.isNotBlank()) { "ShopNode.nameKey must not be blank." }
        require((inventory + refreshInventory).distinctBy(ShopOffer::id).size == inventory.size + refreshInventory.size) {
            "ShopNode '$id' must not contain duplicate offer ids across inventory and refreshInventory."
        }
    }
}

@Serializable
enum class ShopServiceType {
    REFRESH_STOCK,
}

@Serializable
data class ShopOffer(
    val id: String,
    val itemBaseId: String? = null,
    val inscriptionId: String? = null,
    val serviceType: ShopServiceType? = null,
    val price: Int,
    val tags: Set<String> = emptySet(),
) {
    init {
        require(id.isNotBlank()) { "ShopOffer.id must not be blank." }
        val sourceCount = listOf(itemBaseId, inscriptionId, serviceType).count { source -> source != null }
        require(sourceCount == 1) {
            "ShopOffer '$id' must reference exactly one of itemBaseId, inscriptionId, or serviceType."
        }
        require(itemBaseId?.isNotBlank() != false) { "ShopOffer.itemBaseId must not be blank when present." }
        require(inscriptionId?.isNotBlank() != false) { "ShopOffer.inscriptionId must not be blank when present." }
        require(price >= 0) { "ShopOffer.price must not be negative." }
        require(tags.none(String::isBlank)) { "ShopOffer.tags must not contain blank values." }
    }
}

@Serializable
data class RescueInventoryPolicy(
    val guaranteedTags: Set<String>,
    val affordability: AffordableRescueSlotPolicy,
) {
    init {
        require(guaranteedTags.none(String::isBlank)) { "RescueInventoryPolicy.guaranteedTags must not contain blank values." }
    }
}

@Serializable
data class AffordableRescueSlotPolicy(
    val checkpointId: String,
    val expectedShardBudgetByCheckpoint: Int,
    val mandatoryAffordableItemCount: Int,
    val requiredAffordableTags: Set<String>,
) {
    init {
        require(checkpointId.isNotBlank()) { "AffordableRescueSlotPolicy.checkpointId must not be blank." }
        require(expectedShardBudgetByCheckpoint >= 0) {
            "AffordableRescueSlotPolicy.expectedShardBudgetByCheckpoint must not be negative."
        }
        require(mandatoryAffordableItemCount >= 0) {
            "AffordableRescueSlotPolicy.mandatoryAffordableItemCount must not be negative."
        }
        require(requiredAffordableTags.none(String::isBlank)) {
            "AffordableRescueSlotPolicy.requiredAffordableTags must not contain blank values."
        }
    }
}

@Serializable
data class ShopInventoryState(
    val shopId: String,
    val visitCount: Int = 0,
    val purchasedOfferIds: Set<String> = emptySet(),
    val activeRefreshableOffers: List<ShopOffer> = emptyList(),
) {
    init {
        require(shopId.isNotBlank()) { "ShopInventoryState.shopId must not be blank." }
        require(visitCount >= 0) { "ShopInventoryState.visitCount must not be negative." }
        require(purchasedOfferIds.none(String::isBlank)) { "ShopInventoryState.purchasedOfferIds must not contain blank values." }
        require(activeRefreshableOffers.distinctBy(ShopOffer::id).size == activeRefreshableOffers.size) {
            "ShopInventoryState.activeRefreshableOffers must not contain duplicate offer ids."
        }
    }
}
