package com.ktome.client

import com.ktome.game.elites.BossVariantSelectionMode

internal fun bossVariantModeLabelKey(modeId: String): String =
    when (modeId) {
        BossVariantSelectionMode.DISABLED.name -> "ui.validation.boss_variant_mode.disabled"
        BossVariantSelectionMode.AUTO.name -> "ui.validation.boss_variant_mode.auto"
        BossVariantSelectionMode.FORCE_AVAILABLE.name -> "ui.validation.boss_variant_mode.force_available"
        else -> "ui.validation.none"
    }
