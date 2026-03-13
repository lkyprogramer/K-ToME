package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.core.map.Point
import com.ktome.game.PrimaryStat
import com.ktome.game.FoundationGameSession
import com.ktome.game.PlayerCommand

enum class UiMode {
    MAP,
    INVENTORY,
    TARGETING,
    STAT_ASSIGN,
    TALENT_ASSIGN,
}

data class OverlayState(
    val mode: UiMode,
    val inventorySelection: Int = 0,
    val targetingSlot: Int? = null,
    val targetingCursor: Point? = null,
)

class InputHandler(
    private val input: InputSource = GdxInputSource,
) {
    private val movementBindings =
        linkedMapOf(
            Keys.Q to Point(-1, -1),
            Keys.W to Point(0, -1),
            Keys.E to Point(1, -1),
            Keys.A to Point(-1, 0),
            Keys.D to Point(1, 0),
            Keys.Z to Point(-1, 1),
            Keys.X to Point(0, 1),
            Keys.C to Point(1, 1),
            Keys.UP to Point(0, -1),
            Keys.DOWN to Point(0, 1),
            Keys.LEFT to Point(-1, 0),
            Keys.RIGHT to Point(1, 0),
            Keys.HOME to Point(-1, -1),
            Keys.PAGE_UP to Point(1, -1),
            Keys.END to Point(-1, 1),
            Keys.PAGE_DOWN to Point(1, 1),
            Keys.NUMPAD_7 to Point(-1, -1),
            Keys.NUMPAD_8 to Point(0, -1),
            Keys.NUMPAD_9 to Point(1, -1),
            Keys.NUMPAD_4 to Point(-1, 0),
            Keys.NUMPAD_6 to Point(1, 0),
            Keys.NUMPAD_1 to Point(-1, 1),
            Keys.NUMPAD_2 to Point(0, 1),
            Keys.NUMPAD_3 to Point(1, 1),
        )

    private val waitBindings = listOf(Keys.S, Keys.PERIOD, Keys.SPACE, Keys.NUMPAD_5)
    private var mode: UiMode = UiMode.MAP
    private var inventorySelection: Int = 0
    private var targetingSlot: Int? = null
    private var targetingCursor: Point? = null

    fun isMapMode(): Boolean = mode == UiMode.MAP

    fun overlayState(): OverlayState =
        OverlayState(
            mode = mode,
            inventorySelection = inventorySelection,
            targetingSlot = targetingSlot,
            targetingCursor = targetingCursor,
        )

    fun pollCommand(session: FoundationGameSession): PlayerCommand? {
        reconcileMode(session)
        return when (mode) {
            UiMode.MAP -> pollMapCommand(session)
            UiMode.INVENTORY -> pollInventoryCommand(session)
            UiMode.TARGETING -> pollTargetingCommand(session)
            UiMode.STAT_ASSIGN -> pollStatAssignCommand(session)
            UiMode.TALENT_ASSIGN -> pollTalentAssignCommand(session)
        }
    }

    fun onCommandResult(
        session: FoundationGameSession,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        when (command) {
            is PlayerCommand.UseTalent -> {
                if (command.target == null) {
                    reconcileMode(session)
                    return
                }

                if (consumed) {
                    clearTargeting()
                } else {
                    mode = UiMode.TARGETING
                    targetingSlot = command.slot
                    targetingCursor = command.target
                }
            }

            is PlayerCommand.AssignStat,
            is PlayerCommand.AssignTalent,
            PlayerCommand.SaveGame,
            PlayerCommand.Ascend,
            PlayerCommand.Descend,
            -> reconcileMode(session)

            else -> Unit
        }
        reconcileMode(session)
    }

    private fun reconcileMode(session: FoundationGameSession) {
        when (mode) {
            UiMode.STAT_ASSIGN -> {
                if (!session.hasPendingStatAllocation()) {
                    mode = UiMode.MAP
                }
            }

            UiMode.TALENT_ASSIGN -> {
                if (!session.hasPendingTalentAllocation()) {
                    mode = UiMode.MAP
                }
            }

            else -> Unit
        }

        if (session.hasPendingStatAllocation()) {
            mode = UiMode.STAT_ASSIGN
        }
    }

    private fun pollMapCommand(session: FoundationGameSession): PlayerCommand? {
        if (isSaveBinding()) {
            return PlayerCommand.SaveGame
        }

        if (isDescendBinding()) {
            return PlayerCommand.Descend
        }

        if (isAscendBinding()) {
            return PlayerCommand.Ascend
        }

        val movement = movementBindings.entries.firstOrNull { (key, _) -> input.isKeyJustPressed(key) }?.value
        if (movement != null) {
            return PlayerCommand.Move(movement)
        }

        if (waitBindings.any(input::isKeyJustPressed)) {
            return PlayerCommand.Wait
        }

        if (input.isKeyJustPressed(Keys.G)) {
            return PlayerCommand.PickUp
        }

        if (input.isKeyJustPressed(Keys.I)) {
            mode = UiMode.INVENTORY
            inventorySelection = inventorySelection.coerceAtMost((session.inventoryItems().size - 1).coerceAtLeast(0))
            return null
        }

        if (input.isKeyJustPressed(Keys.T) && session.hasPendingTalentAllocation()) {
            mode = UiMode.TALENT_ASSIGN
            return null
        }

        hotkeySlot()?.let { slot ->
            val talent = session.talentSlots().firstOrNull { it.slot == slot } ?: return null
            if (!talent.requiresTarget) {
                return PlayerCommand.UseTalent(slot)
            }

            mode = UiMode.TARGETING
            targetingSlot = slot
            targetingCursor = defaultTargetCursor(session)
        }

        return null
    }

    private fun pollInventoryCommand(session: FoundationGameSession): PlayerCommand? {
        val inventorySize = session.inventoryItems().size
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.I)) {
            mode = UiMode.MAP
            return null
        }

        if (inventorySize == 0) {
            return null
        }

        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            inventorySelection = (inventorySelection - 1).coerceAtLeast(0)
            return null
        }

        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.X)) {
            inventorySelection = (inventorySelection + 1).coerceAtMost(inventorySize - 1)
            return null
        }

        if (
            input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.SPACE) ||
            input.isKeyJustPressed(Keys.E)
        ) {
            return PlayerCommand.ActivateInventoryItem(inventorySelection)
        }

        return null
    }

    private fun pollTargetingCommand(session: FoundationGameSession): PlayerCommand? {
        if (input.isKeyJustPressed(Keys.ESCAPE)) {
            clearTargeting()
            return null
        }

        val cursor = targetingCursor ?: session.playerPosition()
        val movement = movementBindings.entries.firstOrNull { (key, _) -> input.isKeyJustPressed(key) }?.value
        if (movement != null) {
            targetingCursor =
                Point(
                    x = (cursor.x + movement.x).coerceIn(0, session.map.width - 1),
                    y = (cursor.y + movement.y).coerceIn(0, session.map.height - 1),
                )
            return null
        }

        if (input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)) {
            return PlayerCommand.UseTalent(requireNotNull(targetingSlot), targetingCursor ?: session.playerPosition())
        }

        return null
    }

    private fun pollStatAssignCommand(session: FoundationGameSession): PlayerCommand? {
        if (!session.hasPendingStatAllocation()) {
            mode = UiMode.MAP
            return null
        }

        return when {
            input.isKeyJustPressed(Keys.NUM_1) -> PlayerCommand.AssignStat(PrimaryStat.STR)
            input.isKeyJustPressed(Keys.NUM_2) -> PlayerCommand.AssignStat(PrimaryStat.DEX)
            input.isKeyJustPressed(Keys.NUM_3) -> PlayerCommand.AssignStat(PrimaryStat.CON)
            input.isKeyJustPressed(Keys.NUM_4) -> PlayerCommand.AssignStat(PrimaryStat.WIL)
            else -> null
        }
    }

    private fun pollTalentAssignCommand(session: FoundationGameSession): PlayerCommand? {
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.T)) {
            mode = UiMode.MAP
            return null
        }
        if (!session.hasPendingTalentAllocation()) {
            mode = UiMode.MAP
            return null
        }

        return hotkeySlot()?.let(PlayerCommand::AssignTalent)
    }

    private fun hotkeySlot(): Int? =
        when {
            input.isKeyJustPressed(Keys.NUM_1) -> 1
            input.isKeyJustPressed(Keys.NUM_2) -> 2
            input.isKeyJustPressed(Keys.NUM_3) -> 3
            input.isKeyJustPressed(Keys.NUM_4) -> 4
            else -> null
        }

    private fun defaultTargetCursor(session: FoundationGameSession): Point =
        session.targetableHostilePositions()
            .firstOrNull()
            ?: session.playerPosition()

    private fun clearTargeting() {
        mode = UiMode.MAP
        targetingSlot = null
        targetingCursor = null
    }

    private fun isSaveBinding(): Boolean = controlPressed() && input.isKeyJustPressed(Keys.S)

    private fun isDescendBinding(): Boolean = shiftPressed() && input.isKeyJustPressed(Keys.PERIOD)

    private fun isAscendBinding(): Boolean = shiftPressed() && input.isKeyJustPressed(Keys.COMMA)

    private fun controlPressed(): Boolean = input.isKeyPressed(Keys.CONTROL_LEFT) || input.isKeyPressed(Keys.CONTROL_RIGHT)

    private fun shiftPressed(): Boolean = input.isKeyPressed(Keys.SHIFT_LEFT) || input.isKeyPressed(Keys.SHIFT_RIGHT)
}
