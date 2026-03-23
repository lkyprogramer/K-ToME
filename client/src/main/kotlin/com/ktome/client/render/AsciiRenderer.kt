package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.i18n.Localizer

class AsciiRenderer(
    private val localizer: Localizer,
    private val visualResolver: VisualManifestResolver,
    private val cellWidth: Float = 16f,
    private val cellHeight: Float = 16f,
) : Disposable {
    private val mapFont = BitmapFont().apply {
        setUseIntegerPositions(true)
        color = Color.WHITE
    }
    private val uiFont = KtomeFonts.createUiFont(size = 18)

    fun render(
        batch: SpriteBatch,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ) {
        val model = buildRenderModel(localizer, visualResolver, snapshot, overlayState)
        val mapHeight = snapshot.metadata.height
        val mapOffsetY = uiRows * cellHeight

        model.terrainGlyphs.forEach { glyph ->
            drawGlyph(batch, glyph.x, glyph.y, mapHeight, mapOffsetY, glyph.glyph, glyph.colorHex)
        }
        model.propGlyphs.forEach { glyph ->
            drawGlyph(batch, glyph.x, glyph.y, mapHeight, mapOffsetY, glyph.glyph, glyph.colorHex)
        }
        model.overlayGlyphs.forEach { glyph ->
            drawGlyph(batch, glyph.x, glyph.y, mapHeight, mapOffsetY, glyph.glyph, glyph.colorHex)
        }
        model.actorGlyphs.forEach { glyph ->
            drawGlyph(batch, glyph.x, glyph.y, mapHeight, mapOffsetY, glyph.glyph, glyph.colorHex)
        }

        model.targetCursor?.let { cursor ->
            drawGlyph(batch, cursor.x, cursor.y, mapHeight, mapOffsetY, '*', "#FF2400")
        }
        model.inspectCursor?.let { cursor ->
            drawGlyph(batch, cursor.x, cursor.y, mapHeight, mapOffsetY, '+', "#00FFFF")
        }

        uiFont.color = Color.GOLD
        uiFont.draw(
            batch,
            model.hudText,
            4f,
            messageRows * cellHeight + cellHeight - 4f,
        )

        model.messageLines.takeLast(messageRows).forEachIndexed { index, entry ->
            uiFont.color = tone(entry.tone)
            uiFont.draw(batch, entry.text, 4f, 12f + index * cellHeight)
        }

        val sidebarX = (snapshot.metadata.width + 1) * cellWidth
        var line = mapOffsetY + mapHeight * cellHeight - 4f
        model.sidebarLines.forEach { entry ->
            uiFont.color = tone(entry.tone)
            if (entry.text.isNotEmpty()) {
                uiFont.draw(batch, entry.text, sidebarX, line)
            }
            line -= cellHeight
        }
    }

    override fun dispose() {
        mapFont.dispose()
        uiFont.dispose()
    }

    private fun drawGlyph(
        batch: SpriteBatch,
        x: Int,
        y: Int,
        mapHeight: Int,
        mapOffsetY: Float,
        glyph: Char,
        colorHex: String,
    ) {
        mapFont.color = Color.valueOf(colorHex.removePrefix("#"))
        mapFont.draw(
            batch,
            glyph.toString(),
            x * cellWidth + 2f,
            mapOffsetY + (mapHeight - y) * cellHeight - 4f,
        )
    }

    private fun tone(tone: AsciiTextTone): Color =
        when (tone) {
            AsciiTextTone.GOLD -> Color.GOLD
            AsciiTextTone.WHITE -> Color.WHITE
            AsciiTextTone.LIGHT_GRAY -> Color.LIGHT_GRAY
            AsciiTextTone.CYAN -> Color.CYAN
            AsciiTextTone.GRAY -> Color.GRAY
            AsciiTextTone.GREEN -> Color.valueOf("59C173")
            AsciiTextTone.RED -> Color.valueOf("D95959")
        }

    companion object {
        const val messageRows: Int = 6
        const val uiRows: Int = messageRows + 1
        const val sidebarColumns: Int = 28

        internal fun buildRenderModel(
            localizer: Localizer,
            visualResolver: VisualManifestResolver,
            snapshot: RenderSnapshot,
            overlayState: OverlayState,
        ): AsciiRenderModel = AsciiRenderModelBuilder.build(localizer, visualResolver, snapshot, overlayState)

        internal fun hudText(
            localizer: Localizer,
            snapshot: RenderSnapshot,
        ): String {
            val status = snapshot.uiState.playerStatus
            return "${localizer.text("ui.hud.floor.short")} ${snapshot.metadata.currentFloor}/${snapshot.metadata.maxFloor}  " +
                "${localizer.text("ui.hud.hp.short")} ${status.currentHp}/${status.maxHp}  " +
                "${localizer.text(status.resourceLabelKey)} ${status.currentResource}/${status.maxResource}  " +
                "${localizer.text("ui.hud.attack.short")} ${status.attack}  " +
                "${localizer.text("ui.hud.defense.short")} ${status.defense}  " +
                "${localizer.text("ui.hud.level.short")} ${status.level}  " +
                "${localizer.text("ui.hud.xp.short")} ${status.currentExperience}/${status.nextLevelRequirement}  " +
                "${localizer.text("ui.hud.stat.short")} ${status.statPoints}  " +
                "${localizer.text("ui.hud.talent.short")} ${status.talentPoints}"
        }

        internal fun sidebarTitle(
            localizer: Localizer,
            mode: UiMode,
        ): String =
            when (mode) {
                UiMode.MAP -> localizer.text("ui.sidebar.ground")
                UiMode.INVENTORY -> localizer.text("ui.sidebar.inventory")
                UiMode.LOADOUT_EDIT -> localizer.text("ui.sidebar.loadout")
                UiMode.TARGETING -> localizer.text("ui.sidebar.targeting")
                UiMode.INSPECT -> localizer.text("ui.sidebar.inspect")
                UiMode.STAT_ASSIGN -> localizer.text("ui.sidebar.assign_stats")
                UiMode.TALENT_ASSIGN -> localizer.text("ui.sidebar.improve_talents")
            }
    }
}
