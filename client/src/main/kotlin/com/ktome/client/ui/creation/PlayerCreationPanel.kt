package com.ktome.client.ui.creation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ktome.client.screen.MainMenuController
import com.ktome.client.screen.PlayerCreationFocus
import com.ktome.client.ui.token.UiDesignTokens

internal const val PLAYER_CREATION_SECTION_OFFSET_X = 380f
internal const val PLAYER_CREATION_SECTION_MAX_CHARS = 34
internal const val PLAYER_CREATION_ACTION_MAX_CHARS = 28
private const val PLAYER_CREATION_SECTION_LINE_STEP_Y = 22f
private const val PLAYER_CREATION_RACE_SECTION_OFFSET_Y = 70f
private const val PLAYER_CREATION_ACTION_BASE_OFFSET_Y = 120f
private const val PLAYER_CREATION_ACTION_STEP_Y = 22f

internal data class PlayerCreationPanelEntryModel(
    val text: String,
    val enabled: Boolean,
    val selected: Boolean,
)

internal data class PlayerCreationSectionModel(
    val title: String,
    val state: String,
    val description: String,
    val detail: String? = null,
    val note: String? = null,
)

internal data class PlayerCreationPanelModel(
    val professionSection: PlayerCreationSectionModel,
    val raceSection: PlayerCreationSectionModel,
    val focusedAxis: PlayerCreationFocus,
    val entries: List<PlayerCreationPanelEntryModel>,
)

internal object PlayerCreationPanel {
    fun build(
        professionSection: PlayerCreationSectionModel,
        raceSection: PlayerCreationSectionModel,
        focusedAxis: PlayerCreationFocus,
        entries: List<MainMenuController.MenuEntry>,
        selectedIndex: Int,
        localizedEntryLabels: List<String>,
    ): PlayerCreationPanelModel =
        PlayerCreationPanelModel(
            professionSection = professionSection,
            raceSection = raceSection,
            focusedAxis = focusedAxis,
            entries =
                entries.mapIndexed { index, entry ->
                    PlayerCreationPanelEntryModel(
                        text = localizedEntryLabels[index],
                        enabled = entry.enabled,
                        selected = index == selectedIndex,
                    )
                },
        )

    fun render(
        batch: SpriteBatch,
        font: BitmapFont,
        model: PlayerCreationPanelModel,
        professionStateColor: Color,
        raceStateColor: Color,
        x: Float,
        topY: Float,
    ) {
        renderSection(
            batch = batch,
            font = font,
            model = model.professionSection,
            stateColor = professionStateColor,
            x = x,
            topY = topY,
            focused = model.focusedAxis == PlayerCreationFocus.PROFESSION,
        )
        renderSection(
            batch = batch,
            font = font,
            model = model.raceSection,
            stateColor = raceStateColor,
            x = x + PLAYER_CREATION_SECTION_OFFSET_X,
            topY = topY - PLAYER_CREATION_RACE_SECTION_OFFSET_Y,
            focused = model.focusedAxis == PlayerCreationFocus.RACE,
        )

        model.entries.forEachIndexed { index, entry ->
            font.color =
                when {
                    !entry.enabled -> disabledColor()
                    entry.selected -> UiDesignTokens.color.menu.selection.focused.color()
                    else -> UiDesignTokens.color.menu.selection.normal.color()
                }
            font.draw(
                batch,
                fitText(entry.text, PLAYER_CREATION_ACTION_MAX_CHARS),
                x,
                topY - PLAYER_CREATION_ACTION_BASE_OFFSET_Y - index * PLAYER_CREATION_ACTION_STEP_Y,
            )
        }
    }

    private fun renderSection(
        batch: SpriteBatch,
        font: BitmapFont,
        model: PlayerCreationSectionModel,
        stateColor: Color,
        x: Float,
        topY: Float,
        focused: Boolean,
    ) {
        font.color = if (focused) UiDesignTokens.color.focus.ring.color() else UiDesignTokens.color.text.primary.color()
        font.draw(batch, fitText(model.title, PLAYER_CREATION_SECTION_MAX_CHARS), x, topY)
        font.color = stateColor
        font.draw(batch, fitText(model.state, PLAYER_CREATION_SECTION_MAX_CHARS), x, topY - PLAYER_CREATION_SECTION_LINE_STEP_Y)
        font.color = UiDesignTokens.color.text.disabled.color()
        font.draw(batch, fitText(model.description, PLAYER_CREATION_SECTION_MAX_CHARS), x, topY - PLAYER_CREATION_SECTION_LINE_STEP_Y * 2f)
        model.detail?.takeIf(String::isNotBlank)?.let { detail ->
            font.draw(batch, fitText(detail, PLAYER_CREATION_SECTION_MAX_CHARS), x, topY - PLAYER_CREATION_SECTION_LINE_STEP_Y * 3f)
        }
        model.note?.takeIf(String::isNotBlank)?.let { note ->
            font.draw(batch, fitText(note, PLAYER_CREATION_SECTION_MAX_CHARS), x, topY - PLAYER_CREATION_SECTION_LINE_STEP_Y * 4f)
        }
    }

    private fun disabledColor(): Color = UiDesignTokens.color.menu.selection.disabled.color()

    internal fun fitText(
        text: String,
        maxChars: Int,
    ): String {
        if (maxChars <= 0) {
            return ""
        }
        if (text.length <= maxChars) {
            return text
        }
        if (maxChars == 1) {
            return "…"
        }
        return text.take(maxChars - 1) + "…"
    }
}
