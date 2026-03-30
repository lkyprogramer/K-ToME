package com.ktome.client.render

import com.ktome.game.i18n.Localizer

internal fun equipmentSlotLabel(
    localizer: Localizer,
    slotId: String,
): String =
    when (slotId) {
        "WEAPON" -> localizer.text("ui.sidebar.weapon")
        "OFF_HAND" -> localizer.text("ui.sidebar.off_hand")
        "ARMOR" -> localizer.text("ui.sidebar.armor")
        else -> slotId
    }
