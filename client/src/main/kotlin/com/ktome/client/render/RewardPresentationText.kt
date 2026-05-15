package com.ktome.client.render

import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.input.UiMode
import com.ktome.core.snapshot.InscriptionReplacementCategoryChangeSnapshot
import com.ktome.core.snapshot.InscriptionReplacementEntrySnapshot
import com.ktome.core.snapshot.InscriptionReplacementPromptSnapshot
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

internal enum class InscriptionReplacementPromptTone {
    TITLE,
    PRIMARY,
    SECONDARY,
    WARNING,
}

internal data class InscriptionReplacementPromptLine(
    val text: String,
    val tone: InscriptionReplacementPromptTone,
    val selected: Boolean = false,
    val iconKey: String? = null,
)

internal fun inscriptionReplacementPromptLines(
    localizer: Localizer,
    prompt: InscriptionReplacementPromptSnapshot,
    selectedHotkey: Int?,
): List<InscriptionReplacementPromptLine> {
    val currentHotkeys = prompt.currentSlots.mapNotNull { slot -> slot.hotkey }
    val effectiveSelectedHotkey = selectedHotkey?.takeIf { hotkey -> hotkey in currentHotkeys } ?: currentHotkeys.firstOrNull()
    val candidate = prompt.candidate
    val lines = mutableListOf<InscriptionReplacementPromptLine>()
    lines += InscriptionReplacementPromptLine(localizer.text("ui.inscription.replace.title"), InscriptionReplacementPromptTone.TITLE)
    lines +=
        InscriptionReplacementPromptLine(
            localizer.text(
                "ui.inscription.replace.candidate_detail",
                "name" to localizer.text(candidate.nameKey),
                "category" to inscriptionCategoryLabel(localizer, candidate),
                "cooldown" to inscriptionCooldownText(candidate),
                "price" to prompt.price,
            ),
            InscriptionReplacementPromptTone.PRIMARY,
        )
    lines += InscriptionReplacementPromptLine(localizer.text("ui.inscription.replace.tags", "tags" to inscriptionTagText(localizer, candidate)), InscriptionReplacementPromptTone.SECONDARY)
    lines += InscriptionReplacementPromptLine(localizer.text("ui.inscription.replace.effect", "effect" to localizer.text(candidate.descKey)), InscriptionReplacementPromptTone.SECONDARY)
    prompt.categoryChanges
        .filter { change -> change.targetHotkey == effectiveSelectedHotkey }
        .forEach { change ->
            lines +=
                InscriptionReplacementPromptLine(
                    localizer.text(
                        "ui.inscription.replace.category_delta",
                        "category" to inscriptionCategoryLabel(localizer, change),
                        "before" to change.beforeCount,
                        "after" to change.afterCount,
                        "limit" to change.limit,
                    ),
                    if (change.afterCount > change.limit) {
                        InscriptionReplacementPromptTone.WARNING
                    } else {
                        InscriptionReplacementPromptTone.SECONDARY
                    },
                )
        }
    prompt.currentSlots.forEach { slot ->
        val selected = effectiveSelectedHotkey != null && effectiveSelectedHotkey == slot.hotkey
        val upgradeTag =
            if (prompt.candidate.upgradeFromInscriptionId == slot.inscriptionId) {
                " · ${localizer.text("ui.inscription.replace.upgrade_tag")}"
            } else {
                ""
            }
        lines +=
            InscriptionReplacementPromptLine(
                localizer.text(
                    "ui.inscription.replace.slot_detail",
                    "hotkey" to (slot.hotkey ?: "-"),
                    "name" to localizer.text(slot.nameKey),
                    "category" to inscriptionCategoryLabel(localizer, slot),
                    "cooldown" to inscriptionCooldownText(slot),
                    "upgrade" to upgradeTag,
                ),
                InscriptionReplacementPromptTone.PRIMARY,
                selected = selected,
                iconKey = DarkUiChromeVisualKeys.SHOP_REPLACEMENT_SLOT_MARKER,
            )
        lines += InscriptionReplacementPromptLine(localizer.text("ui.inscription.replace.tags", "tags" to inscriptionTagText(localizer, slot)), InscriptionReplacementPromptTone.SECONDARY)
        lines += InscriptionReplacementPromptLine(localizer.text("ui.inscription.replace.effect", "effect" to localizer.text(slot.descKey)), InscriptionReplacementPromptTone.SECONDARY)
    }
    prompt.rejectedReasonKey?.let { reasonKey ->
        lines += InscriptionReplacementPromptLine(localizer.text(reasonKey), InscriptionReplacementPromptTone.WARNING)
    }
    lines += InscriptionReplacementPromptLine(localizer.text("ui.controls.inscription_replace"), InscriptionReplacementPromptTone.SECONDARY)
    return lines
}

private fun inscriptionCategoryLabel(
    localizer: Localizer,
    entry: InscriptionReplacementEntrySnapshot,
): String = inscriptionCategoryLabel(localizer, entry.categoryLabelKey, entry.categoryId)

private fun inscriptionCategoryLabel(
    localizer: Localizer,
    change: InscriptionReplacementCategoryChangeSnapshot,
): String = inscriptionCategoryLabel(localizer, change.categoryLabelKey, change.categoryId)

private fun inscriptionCategoryLabel(
    localizer: Localizer,
    categoryLabelKey: String,
    categoryId: String,
): String =
    if (categoryLabelKey.isNotBlank()) {
        localizer.text(categoryLabelKey)
    } else {
        categoryId
    }

private fun inscriptionCooldownText(entry: InscriptionReplacementEntrySnapshot): String =
    "${entry.cooldownRemaining}/${entry.maxCooldown}"

private fun inscriptionTagText(
    localizer: Localizer,
    entry: InscriptionReplacementEntrySnapshot,
): String =
    entry.effectTagLabelKeys
        .takeIf { tags -> tags.isNotEmpty() }
        ?.joinToString(", ") { labelKey -> localizer.text(labelKey) }
        ?: "-"
