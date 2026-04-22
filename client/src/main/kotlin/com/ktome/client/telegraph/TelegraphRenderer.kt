package com.ktome.client.telegraph

import com.ktome.client.render.AsciiTextLine
import com.ktome.client.render.AsciiTextTone
import com.ktome.client.render.TileTextRow
import com.ktome.client.render.TileTextTone
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
    ): List<TileTextRow> =
        snapshot.overlays
            .sortedWith(compareByDescending<OverlayRenderSnapshot> { it.dangerLevel }.thenBy(OverlayRenderSnapshot::id))
            .map { overlay ->
                TileTextRow(
                    text = "T-${overlay.previewTurns} ${dangerLabel(localizer, overlay.dangerLevel)} ${warningText(localizer, overlay)}",
                    tone = tileTone(overlay.dangerLevel),
                )
            }

    fun asciiLines(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): List<AsciiTextLine> =
        snapshot.overlays
            .sortedWith(compareByDescending<OverlayRenderSnapshot> { it.dangerLevel }.thenBy(OverlayRenderSnapshot::id))
            .map { overlay ->
                AsciiTextLine(
                    text = "T-${overlay.previewTurns} ${dangerLabel(localizer, overlay.dangerLevel)} ${warningText(localizer, overlay)}",
                    tone = asciiTone(overlay.dangerLevel),
                )
            }

    fun alpha(dangerLevel: Int): Float =
        when {
            dangerLevel >= 4 -> 0.92f
            dangerLevel == 3 -> 0.82f
            dangerLevel == 2 -> 0.65f
            else -> 0.50f
        }

    fun fallbackColorHex(dangerLevel: Int): String = UiDesignTokens.color.telegraph.forDangerLevel(dangerLevel).hexString()

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

    private fun tileTone(dangerLevel: Int): TileTextTone =
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
