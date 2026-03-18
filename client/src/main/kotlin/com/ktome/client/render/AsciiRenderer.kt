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
import com.ktome.game.TileVisibility
import com.ktome.game.i18n.Localizer

class AsciiRenderer(
    private val localizer: Localizer,
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
        drawInspectCursor(batch, overlayState, map.height, mapOffsetY)
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
        font.color =
            when {
                session.isVictory() -> Color.FOREST
                session.isGameOver() -> Color.SCARLET
                else -> Color.GOLD
            }
        font.draw(
            batch,
            hudText(localizer, session),
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
        font.draw(batch, tr("ui.sidebar.equipment"), sidebarX, line)
        line -= cellHeight
        font.color = Color.WHITE
        session.equipmentSlots().forEach { equipment ->
            val label = slotLabel(equipment.slot)
            font.draw(batch, "$label: ${equipment.itemName ?: "-"}", sidebarX, line)
            line -= cellHeight
        }

        line -= cellHeight / 2f
        font.color = Color.GOLD
        font.draw(batch, tr("ui.sidebar.talents"), sidebarX, line)
        line -= cellHeight
        font.color = Color.WHITE
        session.talentSlots().forEach { talent ->
            val state =
                if (talent.currentCooldown > 0) {
                    "${tr("ui.sidebar.cooldown.short")}:${talent.currentCooldown}"
                } else {
                    tr("ui.sidebar.ready")
                }
            font.draw(batch, "${talent.slot}.${talent.name} L${talent.level}/${talent.maxLevel} [$state]", sidebarX, line)
            line -= cellHeight
            font.draw(batch, "   ${talent.staminaCost} ${tr("ui.hud.stamina.short")}", sidebarX, line)
            line -= cellHeight
        }

        line -= cellHeight / 2f
        font.color = Color.GOLD
        font.draw(batch, tr("ui.sidebar.ground"), sidebarX, line)
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
                font.draw(batch, tr("ui.controls.map.pick_up"), sidebarX, line)
                line -= cellHeight
                font.draw(batch, tr("ui.controls.map.inventory"), sidebarX, line)
                line -= cellHeight
                font.draw(batch, tr("ui.controls.map.save"), sidebarX, line)
                line -= cellHeight
                if (session.canDescend()) {
                    font.draw(batch, tr("ui.controls.map.descend"), sidebarX, line)
                    line -= cellHeight
                }
                if (session.canAscend()) {
                    font.draw(batch, tr("ui.controls.map.ascend"), sidebarX, line)
                    line -= cellHeight
                }
                if (session.hasPendingTalentAllocation()) {
                    font.draw(batch, tr("ui.controls.map.spend_talent"), sidebarX, line)
                    line -= cellHeight
                }
                if (session.hasPendingStatAllocation()) {
                    font.draw(batch, tr("ui.controls.map.spend_stat"), sidebarX, line)
                }
            }

            UiMode.INVENTORY -> {
                line -= cellHeight / 2f
                font.color = Color.GOLD
                font.draw(batch, sidebarTitle(localizer, UiMode.INVENTORY), sidebarX, line)
                line -= cellHeight
                session.inventoryItems().forEach { item ->
                    font.color = if (item.index == overlayState.inventorySelection) Color.CYAN else Color.WHITE
                    val equipped = item.equippedSlot?.let { slot -> " [${slotLabel(slot)}]" } ?: ""
                    font.draw(batch, "${item.index + 1}. ${item.name}$equipped", sidebarX, line)
                    line -= cellHeight
                }
                if (session.inventoryItems().isEmpty()) {
                    font.color = Color.GRAY
                    font.draw(batch, tr("ui.sidebar.empty"), sidebarX, line)
                    line -= cellHeight
                }
                font.color = Color.LIGHT_GRAY
                font.draw(batch, tr("ui.controls.inventory"), sidebarX, line)
            }

            UiMode.TARGETING -> {
                line -= cellHeight / 2f
                font.color = Color.GOLD
                font.draw(batch, sidebarTitle(localizer, UiMode.TARGETING), sidebarX, line)
                line -= cellHeight
                font.color = Color.WHITE
                font.draw(batch, tr("ui.targeting.slot", "slot" to overlayState.targetingSlot), sidebarX, line)
                line -= cellHeight
                val cursor = overlayState.targetingCursor
                font.draw(
                    batch,
                    tr("ui.targeting.cursor", "x" to (cursor?.x ?: "-"), "y" to (cursor?.y ?: "-")),
                    sidebarX,
                    line,
                )
                line -= cellHeight
                font.color = Color.LIGHT_GRAY
                font.draw(batch, tr("ui.controls.targeting"), sidebarX, line)
            }

            UiMode.INSPECT -> {
                line -= cellHeight / 2f
                val cursor = overlayState.inspectCursor ?: session.playerPosition()
                val inspect = session.inspectAt(cursor)

                font.color = Color.GOLD
                font.draw(batch, sidebarTitle(localizer, UiMode.INSPECT), sidebarX, line)
                line -= cellHeight
                font.color = Color.WHITE
                font.draw(batch, tr("ui.inspect.cursor", "x" to cursor.x, "y" to cursor.y), sidebarX, line)
                line -= cellHeight
                font.draw(batch, "${visibilityLabel(inspect.visibility)} ${inspect.terrainName}", sidebarX, line)
                line -= cellHeight

                inspect.actor?.let { actor ->
                    font.color = Color.GOLD
                    font.draw(batch, actor.name, sidebarX, line)
                    line -= cellHeight
                    font.color = Color.WHITE
                    font.draw(batch, actor.role, sidebarX, line)
                    line -= cellHeight
                    font.draw(batch, "${tr("ui.hud.hp.short")} ${actor.currentHp}/${actor.maxHp}", sidebarX, line)
                    line -= cellHeight
                    font.draw(batch, "${tr("ui.hud.attack.short")} ${actor.attack}  ${tr("ui.hud.defense.short")} ${actor.defense}", sidebarX, line)
                    line -= cellHeight
                    font.draw(batch, "${tr("ui.hud.accuracy.short")} ${actor.accuracy}  ${tr("ui.hud.evasion.short")} ${actor.evasion}", sidebarX, line)
                    line -= cellHeight
                    font.draw(batch, "${tr("ui.stat.str")} ${actor.strength}  ${tr("ui.stat.dex")} ${actor.dexterity}", sidebarX, line)
                    line -= cellHeight
                    font.draw(batch, "${tr("ui.stat.con")} ${actor.constitution}  ${tr("ui.stat.wil")} ${actor.willpower}", sidebarX, line)
                    line -= cellHeight
                    font.draw(batch, "${tr("ui.hud.speed.short")} ${actor.speed}", sidebarX, line)
                    line -= cellHeight
                    if (actor.statusEffects.isNotEmpty()) {
                        font.color = Color.LIGHT_GRAY
                        actor.statusEffects.forEach { effect ->
                            font.draw(batch, effect, sidebarX, line)
                            line -= cellHeight
                        }
                    }
                }

                if (inspect.items.isNotEmpty()) {
                    line -= cellHeight / 2f
                    font.color = Color.GOLD
                    font.draw(batch, tr("ui.sidebar.items"), sidebarX, line)
                    line -= cellHeight
                    inspect.items.forEach { item ->
                        font.color = Color.WHITE
                        font.draw(batch, item.name, sidebarX, line)
                        line -= cellHeight
                        font.color = Color.LIGHT_GRAY
                        font.draw(batch, item.typeLabel, sidebarX, line)
                        line -= cellHeight
                        item.details.forEach { detail ->
                            font.draw(batch, detail, sidebarX, line)
                            line -= cellHeight
                        }
                    }
                }

                inspect.stairLabel?.let { stairLabel ->
                    line -= cellHeight / 2f
                    font.color = Color.GOLD
                    font.draw(batch, stairLabel, sidebarX, line)
                    line -= cellHeight
                }

                if (inspect.actor == null && inspect.items.isEmpty() && inspect.stairLabel == null) {
                    font.color = Color.GRAY
                    val message =
                        when (inspect.visibility) {
                            TileVisibility.VISIBLE -> tr("ui.inspect.no_visible_target")
                            TileVisibility.EXPLORED -> tr("ui.inspect.explored_not_visible")
                            TileVisibility.HIDDEN -> tr("ui.inspect.unknown_tile")
                        }
                    font.draw(batch, message, sidebarX, line)
                    line -= cellHeight
                }

                font.color = Color.LIGHT_GRAY
                font.draw(batch, tr("ui.controls.inspect"), sidebarX, line)
            }

            UiMode.STAT_ASSIGN -> {
                line -= cellHeight / 2f
                font.color = Color.GOLD
                font.draw(batch, sidebarTitle(localizer, UiMode.STAT_ASSIGN), sidebarX, line)
                line -= cellHeight
                font.color = Color.WHITE
                font.draw(batch, tr("ui.sidebar.points", "value" to session.playerStatus().statPoints), sidebarX, line)
                line -= cellHeight
                font.draw(batch, "1. ${tr("ui.stat.str")}", sidebarX, line)
                line -= cellHeight
                font.draw(batch, "2. ${tr("ui.stat.dex")}", sidebarX, line)
                line -= cellHeight
                font.draw(batch, "3. ${tr("ui.stat.con")}", sidebarX, line)
                line -= cellHeight
                font.draw(batch, "4. ${tr("ui.stat.wil")}", sidebarX, line)
            }

            UiMode.TALENT_ASSIGN -> {
                line -= cellHeight / 2f
                font.color = Color.GOLD
                font.draw(batch, sidebarTitle(localizer, UiMode.TALENT_ASSIGN), sidebarX, line)
                line -= cellHeight
                font.color = Color.WHITE
                font.draw(batch, tr("ui.sidebar.points", "value" to session.playerStatus().talentPoints), sidebarX, line)
                line -= cellHeight
                session.talentSlots().forEach { talent ->
                    font.draw(batch, "${talent.slot}. ${talent.name} L${talent.level}/${talent.maxLevel}", sidebarX, line)
                    line -= cellHeight
                }
                font.color = Color.LIGHT_GRAY
                font.draw(batch, tr("ui.controls.talent_assign"), sidebarX, line)
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

    private fun drawInspectCursor(
        batch: SpriteBatch,
        overlayState: OverlayState,
        mapHeight: Int,
        mapOffsetY: Float,
    ) {
        val cursor = overlayState.inspectCursor ?: return
        if (overlayState.mode != UiMode.INSPECT) {
            return
        }

        font.color = Color.CYAN
        font.draw(
            batch,
            "+",
            cursor.x * cellWidth + 2f,
            mapOffsetY + (mapHeight - cursor.y) * cellHeight - 4f,
        )
    }

    private data class GlyphState(
        val glyph: Char,
        val color: Color,
    )

    private fun tr(
        key: String,
        vararg args: Pair<String, Any?>,
    ): String = localizer.text(key, *args)

    private fun slotLabel(slot: EquipSlot): String =
        when (slot) {
            EquipSlot.WEAPON -> tr("ui.sidebar.weapon")
            EquipSlot.ARMOR -> tr("ui.sidebar.armor")
        }

    private fun visibilityLabel(visibility: TileVisibility): String =
        when (visibility) {
            TileVisibility.VISIBLE -> tr("ui.inspect.visible")
            TileVisibility.EXPLORED -> tr("ui.inspect.explored")
            TileVisibility.HIDDEN -> tr("ui.inspect.hidden")
        }

    companion object {
        const val messageRows: Int = 6
        const val uiRows: Int = messageRows + 1
        const val sidebarColumns: Int = 28

        internal fun hudText(
            localizer: Localizer,
            session: FoundationGameSession,
        ): String {
            val status = session.playerStatus()
            fun tr(
                key: String,
                vararg args: Pair<String, Any?>,
            ): String = localizer.text(key, *args)

            return "${tr("ui.hud.floor.short")} ${session.currentFloor()}/${session.maxFloor()}  " +
                "${tr("ui.hud.hp.short")} ${status.currentHp}/${status.maxHp}  " +
                "${tr("ui.hud.stamina.short")} ${status.currentStamina}/${status.maxStamina}  " +
                "${tr("ui.hud.attack.short")} ${status.attack}  " +
                "${tr("ui.hud.defense.short")} ${status.defense}  " +
                "${tr("ui.hud.level.short")} ${status.level}  " +
                "${tr("ui.hud.xp.short")} ${status.currentExperience}/${status.nextLevelRequirement}  " +
                "${tr("ui.hud.stat.short")} ${status.statPoints}  " +
                "${tr("ui.hud.talent.short")} ${status.talentPoints}"
        }

        internal fun sidebarTitle(
            localizer: Localizer,
            mode: UiMode,
        ): String =
            when (mode) {
                UiMode.MAP -> localizer.text("ui.sidebar.ground")
                UiMode.INVENTORY -> localizer.text("ui.sidebar.inventory")
                UiMode.TARGETING -> localizer.text("ui.sidebar.targeting")
                UiMode.INSPECT -> localizer.text("ui.sidebar.inspect")
                UiMode.STAT_ASSIGN -> localizer.text("ui.sidebar.assign_stats")
                UiMode.TALENT_ASSIGN -> localizer.text("ui.sidebar.improve_talents")
            }
    }
}
