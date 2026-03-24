package com.ktome.client.ui.status

import com.ktome.game.i18n.Localizer
import com.ktome.core.snapshot.StatusEffectRenderSnapshot

internal object StatusHudRenderer {
    fun renderLabel(
        localizer: Localizer,
        effect: StatusEffectRenderSnapshot,
    ): String = (effect.nameKey?.let(localizer::text) ?: effect.typeId) + stackSuffix(effect)

    fun renderTurns(
        localizer: Localizer,
        effect: StatusEffectRenderSnapshot,
    ): String =
        localizer.text(
            "ui.inspect.effect.turns",
            "name" to renderLabel(localizer, effect),
            "turns" to effect.remainingTurns,
        )

    fun renderCompact(effect: StatusEffectRenderSnapshot): String =
        buildString {
            when {
                effect.stackCount > 1 && effect.stackCap != null -> append("${effect.stackCount}/${effect.stackCap}")
                effect.stackCount > 1 -> append("${effect.stackCount}x")
            }
            if (isNotEmpty()) {
                append(" ")
            }
            append("${effect.remainingTurns}t")
        }

    private fun stackSuffix(effect: StatusEffectRenderSnapshot): String =
        when {
            effect.stackCount <= 1 -> ""
            effect.stackCap != null -> " x${effect.stackCount}/${effect.stackCap}"
            else -> " x${effect.stackCount}"
        }
}
