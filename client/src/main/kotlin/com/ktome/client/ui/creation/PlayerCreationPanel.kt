package com.ktome.client.ui.creation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ktome.client.screen.MainMenuController
import com.ktome.client.screen.PlayerCreationFocus
import com.ktome.client.ui.token.UiDesignTokens

private const val PLAYER_CREATION_SECTION_OFFSET_X = 380f

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
            topY = topY,
            focused = model.focusedAxis == PlayerCreationFocus.RACE,
        )

        model.entries.forEachIndexed { index, entry ->
            font.color =
                when {
                    !entry.enabled -> disabledColor()
                    entry.selected -> UiDesignTokens.color.menu.selection.focused.color()
                    else -> UiDesignTokens.color.menu.selection.normal.color()
                }
            font.draw(batch, entry.text, x, topY - 132f - index * 32f)
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
        font.draw(batch, model.title, x, topY)
        font.color = stateColor
        font.draw(batch, model.state, x, topY - 28f)
        font.color = UiDesignTokens.color.text.disabled.color()
        font.draw(batch, model.description, x, topY - 56f)
        model.detail?.takeIf(String::isNotBlank)?.let { detail ->
            font.draw(batch, detail, x, topY - 84f)
        }
        model.note?.takeIf(String::isNotBlank)?.let { note ->
            font.draw(batch, note, x, topY - 108f)
        }
    }

    private fun disabledColor(): Color = UiDesignTokens.color.menu.selection.disabled.color()
}
