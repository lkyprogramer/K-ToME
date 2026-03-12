package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.ktome.core.map.Point
import com.ktome.core.map.TileType
import com.ktome.game.FoundationGameSession

class AsciiRenderer(
    private val cellWidth: Float = 16f,
    private val cellHeight: Float = 16f,
) : Disposable {
    private val font = BitmapFont().apply {
        setUseIntegerPositions(true)
        color = Color.WHITE
    }

    fun render(
        batch: SpriteBatch,
        session: FoundationGameSession,
    ) {
        val map = session.map
        val playerPosition = session.playerPosition()
        val visible = session.visibleTiles()
        val explored = session.exploredTiles()

        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                val point = Point(x, y)
                val glyphState = when {
                    point == playerPosition && point in visible -> GlyphState(session.playerGlyph(), Color.GOLD)
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
                    drawGlyph(batch, x, y, map.height, state)
                }
            }
        }
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

    private fun drawGlyph(
        batch: SpriteBatch,
        x: Int,
        y: Int,
        mapHeight: Int,
        glyphState: GlyphState,
    ) {
        font.color = glyphState.color
        font.draw(
            batch,
            glyphState.glyph.toString(),
            x * cellWidth + 2f,
            (mapHeight - y) * cellHeight - 4f,
        )
    }

    private data class GlyphState(
        val glyph: Char,
        val color: Color,
    )
}
