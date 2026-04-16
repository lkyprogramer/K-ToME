package com.ktome.client.render

import com.ktome.client.input.UiMode
import com.ktome.core.snapshot.ShopOfferSnapshot
import com.ktome.game.i18n.Localizer

internal val recentRewardPresentationModes: Set<UiMode> =
    setOf(UiMode.MAP, UiMode.SHOP, UiMode.WORLD_MAP, UiMode.INVENTORY)

internal fun recentRewardText(
    sourceLabel: String,
    itemDisplayName: String,
): String = "$sourceLabel: $itemDisplayName"

internal fun recentRewardDetailText(detailText: String): String = "  $detailText"

internal fun shopOfferText(
    localizer: Localizer,
    offer: ShopOfferSnapshot,
): String {
    val localizedTags = offer.tagLabelKeys.map(localizer::text)
    val tagSuffix =
        if (localizedTags.isEmpty()) {
            ""
        } else {
            " [${localizedTags.joinToString(" / ")}]"
        }
    return "${offer.index + 1}. ${localizer.text(offer.labelKey)}$tagSuffix (${offer.price})"
}
