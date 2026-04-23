package com.ktome.client.ui.status

import com.badlogic.gdx.graphics.Color
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.game.i18n.Localizer
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot

internal object StatusHudRenderer {
    fun renderLabel(
        localizer: Localizer,
        effect: StatusEffectRenderSnapshot,
    ): String = renderLabel(localizer, StatusPresentationBuilder.build(effect))

    fun renderLabel(
        localizer: Localizer,
        presentation: StatusPresentationModel,
    ): String {
        val label = presentation.nameKey?.let(localizer::text) ?: presentation.typeId
        return listOf(label, presentation.badgeText.takeIf(String::isNotBlank)).filterNotNull().joinToString(" ")
    }

    fun renderTurns(
        localizer: Localizer,
        effect: StatusEffectRenderSnapshot,
    ): String = renderLabel(localizer, effect)

    fun renderCompact(effect: StatusEffectRenderSnapshot): String =
        StatusPresentationBuilder.build(effect).badgeText

    fun accentColor(category: StatusEffectCategorySnapshot): Color =
        when (category) {
            StatusEffectCategorySnapshot.BUFF -> UiDesignTokens.color.status.buffAccent.color()
            StatusEffectCategorySnapshot.DEBUFF -> UiDesignTokens.color.status.debuffAccent.color()
            StatusEffectCategorySnapshot.NEUTRAL -> UiDesignTokens.color.status.neutralAccent.color()
        }

    fun badgeColor(category: StatusEffectCategorySnapshot): Color =
        when (category) {
            StatusEffectCategorySnapshot.BUFF -> UiDesignTokens.color.status.badge.stack.color()
            StatusEffectCategorySnapshot.DEBUFF -> UiDesignTokens.color.status.badge.turns.color()
            StatusEffectCategorySnapshot.NEUTRAL -> UiDesignTokens.color.status.badge.cap.color()
        }
}
