package com.ktome.client.render

import com.ktome.game.i18n.Localizer

internal fun equipmentSlotLabel(
    localizer: Localizer,
    slotId: String,
): String =
    equipmentSlotLabelKey(slotId)
        .takeIf { key -> key != slotId }
        ?.let(localizer::text)
        ?: slotId

internal fun equipmentSlotLabelKey(slotId: String): String =
    when (slotId) {
        "WEAPON" -> "ui.sidebar.weapon"
        "OFF_HAND" -> "ui.sidebar.off_hand"
        "ARMOR" -> "ui.sidebar.armor"
        "ACCESSORY" -> "ui.reward.slot.accessory"
        else -> slotId
    }
