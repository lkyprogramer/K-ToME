package com.ktome.client.render

import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.ui.UiCompanionVisualKeys
import com.ktome.core.snapshot.ItemRenderSnapshot

internal fun resolveItemIconVisual(
    visualResolver: VisualManifestResolver,
    item: ItemRenderSnapshot,
): ResolvedVisualAsset {
    for (requestedKey in listOfNotNull(item.iconKey, item.visualKey)) {
        val resolved = visualResolver.resolve(requestedKey)
        if (!resolved.fallbackUsed && !resolved.matchedByPrefix) {
            return resolved
        }
    }
    val fallback = visualResolver.resolve(UiCompanionVisualKeys.EMPTY_INVENTORY)
    require(!fallback.fallbackUsed && !fallback.matchedByPrefix) {
        "Item icon fallback requires exact visual key '${UiCompanionVisualKeys.EMPTY_INVENTORY}'."
    }
    return fallback
}
