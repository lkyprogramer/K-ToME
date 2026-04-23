package com.ktome.client.telegraph

import com.ktome.client.render.AsciiTextLine
import com.ktome.client.render.AsciiTextTone
import com.ktome.client.render.TileTextRow
import com.ktome.client.render.TileTextTone
import com.ktome.client.ui.settings.AccessibilityToggle
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.i18n.Localizer

internal object TelegraphRenderer {
    fun tileRows(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): List<TileTextRow> = tileRows(localizer, snapshot.overlays)

    fun tileRows(
        localizer: Localizer,
        overlays: List<OverlayRenderSnapshot>,
    ): List<TileTextRow> = tileRows(localizer, overlays, AccessibilityToggle.fromSystemProperties())

    fun tileRows(
        localizer: Localizer,
        overlays: List<OverlayRenderSnapshot>,
        accessibility: AccessibilityToggle,
    ): List<TileTextRow> =
        TelegraphPresentationModel
            .sorted(overlays)
            .map { (overlay, presentation) ->
                TileTextRow(
                    text = rowText(localizer, overlay, presentation, accessibility),
                    tone = tileTone(overlay.dangerLevel),
                )
            }

    fun asciiLines(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): List<AsciiTextLine> {
        val accessibility = AccessibilityToggle.fromSystemProperties()
        return TelegraphPresentationModel
            .sorted(snapshot.overlays)
            .map { (overlay, presentation) ->
                AsciiTextLine(
                    text = rowText(localizer, overlay, presentation, accessibility),
                    tone = asciiTone(overlay.dangerLevel),
                )
            }
    }

    fun alpha(dangerLevel: Int): Float =
        alpha(dangerLevel = dangerLevel, accessibility = AccessibilityToggle.fromSystemProperties())

    fun alpha(
        dangerLevel: Int,
        accessibility: AccessibilityToggle,
    ): Float =
        accessibility.overlayAlpha(
            baseAlpha =
        when {
            dangerLevel >= 4 -> 0.92f
            dangerLevel == 3 -> 0.82f
            dangerLevel == 2 -> 0.65f
            else -> 0.50f
        },
        )

    fun fallbackColorHex(dangerLevel: Int): String = UiDesignTokens.color.telegraph.forDangerLevel(dangerLevel).hexString()

    fun targetCardRows(
        localizer: Localizer,
        overlay: OverlayRenderSnapshot,
    ): List<String> {
        val model = TelegraphPresentationModel.fromOverlay(overlay)
        return listOf(
            localizer.text(
                "ui.telegraph.target.summary",
                "danger" to dangerLabel(localizer, model.dangerLevel),
                "turns" to model.previewTurnsRemaining,
                "cells" to model.affectedCellCount,
                "ability" to model.typeId,
            ),
            warningText(localizer, overlay),
        )
    }

    fun logPrefix(
        localizer: Localizer,
        presentation: TelegraphPresentationModel,
    ): String =
        listOf(
            presentation.badgeText.takeIf(String::isNotBlank),
            dangerLabel(localizer, presentation.dangerLevel),
        ).filterNotNull().joinToString(separator = " ", prefix = "[", postfix = "]")

    private fun rowText(
        localizer: Localizer,
        overlay: OverlayRenderSnapshot,
        presentation: TelegraphPresentationModel,
        accessibility: AccessibilityToggle,
    ): String =
        listOfNotNull(
            presentation.badgeText.takeIf(String::isNotBlank),
            accessibility.riskCueBadge(overlay.dangerLevel),
            dangerLabel(localizer, overlay.dangerLevel),
            warningText(localizer, overlay),
        ).joinToString(" ")

    private fun warningText(
        localizer: Localizer,
        overlay: OverlayRenderSnapshot,
    ): String = overlay.warningMessage?.let { token -> renderTextToken(localizer, token) } ?: overlay.sourceAbilityId

    private fun dangerLabel(
        localizer: Localizer,
        dangerLevel: Int,
    ): String =
        when {
            dangerLevel >= 4 -> localizer.text("ui.warning.lethal")
            dangerLevel == 3 -> localizer.text("ui.warning.high")
            dangerLevel == 2 -> localizer.text("ui.warning.moderate")
            else -> localizer.text("ui.warning.low")
        }

    private fun renderTextToken(
        localizer: Localizer,
        token: RenderTextTokenSnapshot,
    ): String =
        localizer.text(
            token.key,
            *token.arguments.map { argument -> argument.name to resolveArgument(localizer, argument) }.toTypedArray(),
        )

    private fun resolveArgument(
        localizer: Localizer,
        argument: RenderTextArgumentSnapshot,
    ): String =
        argument.value
            ?: argument.valueKey?.let(localizer::text)
            ?: argument.valueToken?.let { token -> renderTextToken(localizer, token) }
            ?: ""

    fun tileTone(dangerLevel: Int): TileTextTone =
        when {
            dangerLevel >= 4 -> TileTextTone.MAGENTA
            dangerLevel == 3 -> TileTextTone.RED
            dangerLevel == 2 -> TileTextTone.GOLD
            else -> TileTextTone.CYAN
        }

    private fun asciiTone(dangerLevel: Int): AsciiTextTone =
        when {
            dangerLevel >= 4 -> AsciiTextTone.MAGENTA
            dangerLevel == 3 -> AsciiTextTone.RED
            dangerLevel == 2 -> AsciiTextTone.GOLD
            else -> AsciiTextTone.CYAN
        }
}
