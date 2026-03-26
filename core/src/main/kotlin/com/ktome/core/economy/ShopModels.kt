package com.ktome.core.economy

import kotlinx.serialization.Serializable

@Serializable
data class ShopNode(
    val id: String,
    val zoneId: String,
    val nameKey: String,
    val inventory: List<ShopOffer>,
    val rescuePolicy: RescueInventoryPolicy,
) {
    init {
        require(id.isNotBlank()) { "ShopNode.id must not be blank." }
        require(zoneId.isNotBlank()) { "ShopNode.zoneId must not be blank." }
        require(nameKey.isNotBlank()) { "ShopNode.nameKey must not be blank." }
        require(inventory.distinctBy(ShopOffer::id).size == inventory.size) {
            "ShopNode '$id' must not contain duplicate offer ids."
        }
    }
}

@Serializable
data class ShopOffer(
    val id: String,
    val itemBaseId: String? = null,
    val inscriptionId: String? = null,
    val price: Int,
    val tags: Set<String> = emptySet(),
) {
    init {
        require(id.isNotBlank()) { "ShopOffer.id must not be blank." }
        require(itemBaseId != null || inscriptionId != null) {
            "ShopOffer '$id' must reference either an itemBaseId or an inscriptionId."
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
    val purchasedOfferIds: Set<String> = emptySet(),
) {
    init {
        require(shopId.isNotBlank()) { "ShopInventoryState.shopId must not be blank." }
        require(purchasedOfferIds.none(String::isBlank)) { "ShopInventoryState.purchasedOfferIds must not contain blank values." }
    }
}
