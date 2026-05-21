package com.ktome.core.snapshot

fun ItemRenderSnapshot.inventoryStackIdentity(): String? {
    val normalizedTypeId = typeId.uppercase()
    if (normalizedTypeId !in stackableRenderItemTypeIds || slotId != null || specialTemplateId != null || specialTierId != null) {
        return null
    }
    if (affixIds.isNotEmpty() || affixNameKeys.isNotEmpty() || stats != ItemStatModifierSnapshot()) {
        return null
    }
    return listOf(
        baseItemId,
        normalizedTypeId,
        qualityTierId,
        effectTypeId.orEmpty(),
        resourceTypeId.orEmpty(),
        materialNameKey.orEmpty(),
        magnitude.toString(),
    ).joinToString(separator = "|")
}

internal val stackableRenderItemTypeIds = setOf("CONSUMABLE", "MATERIAL", "CURRENCY")
