package com.ktome.client.ui.creation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ktome.client.screen.MainMenuController

internal data class ClassSelectPanelEntryModel(
    val text: String,
    val enabled: Boolean,
    val selected: Boolean,
)

internal data class ClassSelectPanelModel(
    val profession: String,
    val professionState: String,
    val professionDescription: String,
    val professionResourceHint: String,
    val entries: List<ClassSelectPanelEntryModel>,
)

internal object ClassSelectPanel {
    fun build(
        profession: String,
        professionState: String,
        professionDescription: String,
        professionResourceHint: String,
        entries: List<MainMenuController.MenuEntry>,
        selectedIndex: Int,
        localizedEntryLabels: List<String>,
    ): ClassSelectPanelModel =
        ClassSelectPanelModel(
            profession = profession,
            professionState = professionState,
            professionDescription = professionDescription,
            professionResourceHint = professionResourceHint,
            entries =
                entries.mapIndexed { index, entry ->
                    ClassSelectPanelEntryModel(
                        text = localizedEntryLabels[index],
                        enabled = entry.enabled,
                        selected = index == selectedIndex,
                    )
                },
        )

    fun render(
        batch: SpriteBatch,
        font: BitmapFont,
        model: ClassSelectPanelModel,
        stateColor: Color,
        x: Float,
        topY: Float,
    ) {
        font.color = Color.WHITE
        font.draw(batch, model.profession, x, topY)
        font.color = stateColor
        font.draw(batch, model.professionState, x, topY - 28f)
        font.color = Color.GRAY
        font.draw(batch, model.professionDescription, x, topY - 56f)
        font.draw(batch, model.professionResourceHint, x, topY - 84f)

        model.entries.forEachIndexed { index, entry ->
            font.color =
                when {
                    !entry.enabled -> Color.DARK_GRAY
                    entry.selected -> Color.CYAN
                    else -> Color.WHITE
                }
            font.draw(batch, entry.text, x, topY - 104f - index * 32f)
        }
    }
}
