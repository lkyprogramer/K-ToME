package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.ui.MessageLog
import com.ktome.core.item.EquipSlot
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
        overlayState: OverlayState,
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

        drawTargetCursor(batch, overlayState, map.height, mapOffsetY)
        drawStatus(batch, session)
        drawMessages(batch, session)
        drawSidebar(batch, session, overlayState, map.height, mapOffsetY)
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
            "HP ${status.currentHp}/${status.maxHp}  STA ${status.currentStamina}/${status.maxStamina}  ATK ${status.attack}  DEF ${status.defense}  LV ${status.level}  XP ${status.currentExperience}/${status.nextLevelRequirement}  STAT ${status.statPoints}  TAL ${status.talentPoints}",
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

    private fun drawSidebar(
        batch: SpriteBatch,
        session: FoundationGameSession,
        overlayState: OverlayState,
        mapHeight: Int,
        mapOffsetY: Float,
    ) {
        val sidebarX = (session.map.width + 1) * cellWidth
        var line = mapOffsetY + mapHeight * cellHeight - 4f

        font.color = Color.GOLD
        font.draw(batch, "Equipment", sidebarX, line)
        line -= cellHeight
        font.color = Color.WHITE
        session.equipmentSlots().forEach { equipment ->
            val label =
                when (equipment.slot) {
                    EquipSlot.WEAPON -> "Weapon"
                    EquipSlot.ARMOR -> "Armor"
                }
            font.draw(batch, "$label: ${equipment.itemName ?: "-"}", sidebarX, line)
            line -= cellHeight
        }

        line -= cellHeight / 2f
        font.color = Color.GOLD
        font.draw(batch, "Talents", sidebarX, line)
        line -= cellHeight
        font.color = Color.WHITE
        session.talentSlots().forEach { talent ->
            val state =
                if (talent.currentCooldown > 0) {
                    "CD:${talent.currentCooldown}"
                } else {
                    "Ready"
                }
            font.draw(batch, "${talent.slot}.${talent.name} [$state] ${talent.staminaCost} STA", sidebarX, line)
            line -= cellHeight
        }

        line -= cellHeight / 2f
        font.color = Color.GOLD
        font.draw(batch, "Ground", sidebarX, line)
        line -= cellHeight
        font.color = Color.WHITE
        val groundItems = session.itemNamesAtPlayerPosition()
        if (groundItems.isEmpty()) {
            font.draw(batch, "-", sidebarX, line)
            line -= cellHeight
        } else {
            groundItems.forEach { name ->
                font.draw(batch, name, sidebarX, line)
                line -= cellHeight
            }
        }

        when (overlayState.mode) {
            UiMode.MAP -> {
                line -= cellHeight / 2f
                font.color = Color.LIGHT_GRAY
                font.draw(batch, "g pick up", sidebarX, line)
                line -= cellHeight
                font.draw(batch, "i inventory", sidebarX, line)
            }

            UiMode.INVENTORY -> {
                line -= cellHeight / 2f
                font.color = Color.GOLD
                font.draw(batch, "Inventory", sidebarX, line)
                line -= cellHeight
                session.inventoryItems().forEach { item ->
                    font.color = if (item.index == overlayState.inventorySelection) Color.CYAN else Color.WHITE
                    val equipped = item.equippedSlot?.let { slot -> " [$slot]" } ?: ""
                    font.draw(batch, "${item.index + 1}. ${item.name}$equipped", sidebarX, line)
                    line -= cellHeight
                }
                if (session.inventoryItems().isEmpty()) {
                    font.color = Color.GRAY
                    font.draw(batch, "(empty)", sidebarX, line)
                    line -= cellHeight
                }
                font.color = Color.LIGHT_GRAY
                font.draw(batch, "W/X move  E use/equip  Esc close", sidebarX, line)
            }

            UiMode.TARGETING -> {
                line -= cellHeight / 2f
                font.color = Color.GOLD
                font.draw(batch, "Targeting", sidebarX, line)
                line -= cellHeight
                font.color = Color.WHITE
                font.draw(batch, "Slot ${overlayState.targetingSlot}", sidebarX, line)
                line -= cellHeight
                val cursor = overlayState.targetingCursor
                font.draw(batch, "Cursor ${cursor?.x ?: "-"},${cursor?.y ?: "-"}", sidebarX, line)
                line -= cellHeight
                font.color = Color.LIGHT_GRAY
                font.draw(batch, "Move cursor  Enter confirm  Esc cancel", sidebarX, line)
            }
        }
    }

    private fun drawTargetCursor(
        batch: SpriteBatch,
        overlayState: OverlayState,
        mapHeight: Int,
        mapOffsetY: Float,
    ) {
        val cursor = overlayState.targetingCursor ?: return
        if (overlayState.mode != UiMode.TARGETING) {
            return
        }

        font.color = Color.SCARLET
        font.draw(
            batch,
            "X",
            cursor.x * cellWidth + 2f,
            mapOffsetY + (mapHeight - cursor.y) * cellHeight - 4f,
        )
    }

    private data class GlyphState(
        val glyph: Char,
        val color: Color,
    )

    companion object {
        const val messageRows: Int = 6
        const val uiRows: Int = messageRows + 1
        const val sidebarColumns: Int = 28
    }
}
