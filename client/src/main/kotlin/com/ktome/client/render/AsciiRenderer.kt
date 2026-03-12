package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.ktome.client.ui.MessageLog
import com.ktome.core.map.Point
import com.ktome.core.map.TileType
import com.ktome.game.ActorView
import com.ktome.game.FoundationGameSession

class AsciiRenderer(
    private val cellWidth: Float = 16f,
    private val cellHeight: Float = 16f,
) : Disposable {
    private val font = BitmapFont().apply {
        setUseIntegerPositions(true)
        color = Color.WHITE
    }
    private val messageLog = MessageLog(maxLines = messageRows)

    fun render(
        batch: SpriteBatch,
        session: FoundationGameSession,
    ) {
        val map = session.map
        val visible = session.visibleTiles()
        val explored = session.exploredTiles()
        val mapOffsetY = uiRows * cellHeight

        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                val point = Point(x, y)
                val glyphState = when {
                    point in visible -> {
                        val tile = map[point]
                        GlyphState(tile.glyph, visibleColor(tile))
                    }
                    point in explored -> {
                        val tile = map[point]
                        GlyphState(tile.glyph, exploredColor(tile))
                    }
                    else -> null
                }

                glyphState?.let { state ->
                    drawGlyph(batch, x, y, map.height, mapOffsetY, state)
                }
            }
        }

        session.actorViews()
            .sortedBy { if (it.isPlayer) 1 else 0 }
            .forEach { actor ->
                drawActor(batch, actor, map.height, mapOffsetY)
            }

        drawStatus(batch, session)
        drawMessages(batch, session)
    }

    override fun dispose() {
        font.dispose()
    }

    private fun visibleColor(tile: TileType): Color =
        when (tile) {
            TileType.WALL -> Color.LIGHT_GRAY
            TileType.FLOOR -> Color.WHITE
        }

    private fun exploredColor(tile: TileType): Color =
        when (tile) {
            TileType.WALL -> Color.DARK_GRAY
            TileType.FLOOR -> Color.GRAY
        }

    private fun drawActor(
        batch: SpriteBatch,
        actor: ActorView,
        mapHeight: Int,
        mapOffsetY: Float,
    ) {
        drawGlyph(
            batch = batch,
            x = actor.position.x,
            y = actor.position.y,
            mapHeight = mapHeight,
            mapOffsetY = mapOffsetY,
            glyphState = GlyphState(actor.glyph, Color.valueOf(actor.colorHex.removePrefix("#"))),
        )
    }

    private fun drawStatus(
        batch: SpriteBatch,
        session: FoundationGameSession,
    ) {
        val status = session.playerStatus()
        font.color = if (session.isGameOver()) Color.SCARLET else Color.GOLD
        font.draw(
            batch,
            "HP ${status.currentHp}/${status.maxHp}  LV ${status.level}  XP ${status.currentExperience}/${status.nextLevelRequirement}  STAT ${status.statPoints}  TAL ${status.talentPoints}",
            4f,
            messageRows * cellHeight + cellHeight - 4f,
        )
    }

    private fun drawMessages(
        batch: SpriteBatch,
        session: FoundationGameSession,
    ) {
        font.color = Color.WHITE
        messageLog.render(
            batch = batch,
            font = font,
            messages = session.messageLog(),
            x = 4f,
            baselineY = 12f,
            lineHeight = cellHeight,
        )
    }

    private fun drawGlyph(
        batch: SpriteBatch,
        x: Int,
        y: Int,
        mapHeight: Int,
        mapOffsetY: Float,
        glyphState: GlyphState,
    ) {
        font.color = glyphState.color
        font.draw(
            batch,
            glyphState.glyph.toString(),
            x * cellWidth + 2f,
            mapOffsetY + (mapHeight - y) * cellHeight - 4f,
        )
    }

    private data class GlyphState(
        val glyph: Char,
        val color: Color,
    )

    companion object {
        const val messageRows: Int = 6
        const val uiRows: Int = messageRows + 1
    }
}
