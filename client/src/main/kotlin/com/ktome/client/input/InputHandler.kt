package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.core.map.Point
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.PrimaryStat
import com.ktome.game.PlayerCommand

enum class UiMode {
    MAP,
    INVENTORY,
    TARGETING,
    INSPECT,
    STAT_ASSIGN,
    TALENT_ASSIGN,
}

data class OverlayState(
    val mode: UiMode,
    val inventorySelection: Int = 0,
    val targetingSlot: Int? = null,
    val targetingCursor: Point? = null,
    val inspectCursor: Point? = null,
)

class InputHandler(
    private val input: InputSource = GdxInputSource,
) {
    private val repeatInitialDelayFrames = 12
    private val repeatIntervalFrames = 3
    private val movementBindings =
        linkedMapOf(
            Keys.Q to Point(-1, -1),
            Keys.W to Point(0, -1),
            Keys.E to Point(1, -1),
            Keys.A to Point(-1, 0),
            Keys.S to Point(0, 1),
            Keys.D to Point(1, 0),
            Keys.Z to Point(-1, 1),
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

    private val waitBindings = listOf(Keys.PERIOD, Keys.SPACE, Keys.NUMPAD_5)
    private var mode: UiMode = UiMode.MAP
    private var inventorySelection: Int = 0
    private var targetingSlot: Int? = null
    private var targetingCursor: Point? = null
    private var inspectCursor: Point? = null
    private var heldMovementKey: Int? = null
    private var movementRepeatCountdown: Int = repeatInitialDelayFrames

    fun isMapMode(): Boolean = mode == UiMode.MAP

    fun overlayState(): OverlayState =
        OverlayState(
            mode = mode,
            inventorySelection = inventorySelection,
            targetingSlot = targetingSlot,
            targetingCursor = targetingCursor,
            inspectCursor = inspectCursor,
        )

    fun pollCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (mode == UiMode.MAP && input.isKeyJustPressed(Keys.X)) {
            mode = UiMode.INSPECT
            inspectCursor = defaultInspectCursor(snapshot)
            return null
        }

        reconcileMode(snapshot)
        if (mode != UiMode.MAP) {
            resetMovementRepeat()
        }
        return when (mode) {
            UiMode.MAP -> pollMapCommand(snapshot)
            UiMode.INVENTORY -> pollInventoryCommand(snapshot)
            UiMode.TARGETING -> pollTargetingCommand(snapshot)
            UiMode.INSPECT -> pollInspectCommand(snapshot)
            UiMode.STAT_ASSIGN -> pollStatAssignCommand(snapshot)
            UiMode.TALENT_ASSIGN -> pollTalentAssignCommand(snapshot)
        }
    }

    fun onCommandResult(
        snapshot: RenderSnapshot,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        when (command) {
            is PlayerCommand.UseTalent -> {
                if (command.target == null) {
                    reconcileMode(snapshot)
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
            -> reconcileMode(snapshot)

            else -> Unit
        }
        reconcileMode(snapshot)
    }

    private fun reconcileMode(snapshot: RenderSnapshot) {
        when (mode) {
            UiMode.STAT_ASSIGN -> {
                if (!hasPendingStatAllocation(snapshot)) {
                    mode = UiMode.MAP
                }
            }

            UiMode.TALENT_ASSIGN -> {
                if (!hasPendingTalentAllocation(snapshot)) {
                    mode = UiMode.MAP
                }
            }

            else -> Unit
        }

        if (hasPendingStatAllocation(snapshot) && mode == UiMode.MAP) {
            mode = UiMode.STAT_ASSIGN
        }
    }

    private fun pollMapCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (isSaveBinding()) {
            return PlayerCommand.SaveGame
        }

        stairCommandAtPlayer(snapshot)?.let { command ->
            if (isInteractBinding()) {
                return command
            }
        }

        if (isDescendBinding()) {
            return PlayerCommand.Descend
        }

        if (isAscendBinding()) {
            return PlayerCommand.Ascend
        }

        val movement = pollMovementCommand()
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
            inventorySelection = inventorySelection.coerceAtMost((snapshot.uiState.inventory.size - 1).coerceAtLeast(0))
            resetMovementRepeat()
            return null
        }

        if (input.isKeyJustPressed(Keys.T) && hasPendingTalentAllocation(snapshot)) {
            mode = UiMode.TALENT_ASSIGN
            resetMovementRepeat()
            return null
        }

        hotkeySlot()?.let { slot ->
            val talent = snapshot.uiState.talents.firstOrNull { it.slot == slot } ?: return null
            if (!talent.requiresTarget) {
                return PlayerCommand.UseTalent(slot)
            }

            mode = UiMode.TARGETING
            targetingSlot = slot
            targetingCursor = defaultTargetCursor(snapshot)
            resetMovementRepeat()
        }

        return null
    }

    private fun pollInspectCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.X)) {
            clearInspect()
            return null
        }

        val cursor = inspectCursor ?: defaultInspectCursor(snapshot)
        val movement = movementBindings.entries.firstOrNull { (key, _) -> input.isKeyJustPressed(key) }?.value
        if (movement != null) {
            inspectCursor =
                Point(
                    x = (cursor.x + movement.x).coerceIn(0, snapshot.metadata.width - 1),
                    y = (cursor.y + movement.y).coerceIn(0, snapshot.metadata.height - 1),
                )
        }
        return null
    }

    private fun pollInventoryCommand(snapshot: RenderSnapshot): PlayerCommand? {
        val inventorySize = snapshot.uiState.inventory.size
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.I)) {
            mode = UiMode.MAP
            resetMovementRepeat()
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

    private fun pollTargetingCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (input.isKeyJustPressed(Keys.ESCAPE)) {
            clearTargeting()
            return null
        }

        val cursor = targetingCursor ?: playerPosition(snapshot)
        val movement = movementBindings.entries.firstOrNull { (key, _) -> input.isKeyJustPressed(key) }?.value
        if (movement != null) {
            targetingCursor =
                Point(
                    x = (cursor.x + movement.x).coerceIn(0, snapshot.metadata.width - 1),
                    y = (cursor.y + movement.y).coerceIn(0, snapshot.metadata.height - 1),
                )
            return null
        }

        if (input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)) {
            return PlayerCommand.UseTalent(requireNotNull(targetingSlot), targetingCursor ?: playerPosition(snapshot))
        }

        return null
    }

    private fun pollStatAssignCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (!hasPendingStatAllocation(snapshot)) {
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

    private fun pollTalentAssignCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.T)) {
            mode = UiMode.MAP
            return null
        }
        if (!hasPendingTalentAllocation(snapshot)) {
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

    private fun defaultTargetCursor(snapshot: RenderSnapshot): Point =
        snapshot.uiState.targetablePositions
            .firstOrNull()
            ?.toPoint()
            ?: playerPosition(snapshot)

    private fun defaultInspectCursor(snapshot: RenderSnapshot): Point = playerPosition(snapshot)

    private fun clearTargeting() {
        mode = UiMode.MAP
        targetingSlot = null
        targetingCursor = null
        resetMovementRepeat()
    }

    private fun clearInspect() {
        mode = UiMode.MAP
        inspectCursor = null
        resetMovementRepeat()
    }

    private fun isSaveBinding(): Boolean = controlPressed() && input.isKeyJustPressed(Keys.S)

    private fun isInteractBinding(): Boolean =
        input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.NUMPAD_ENTER)

    private fun isDescendBinding(): Boolean = shiftPressed() && input.isKeyJustPressed(Keys.PERIOD)

    private fun isAscendBinding(): Boolean = shiftPressed() && input.isKeyJustPressed(Keys.COMMA)

    private fun controlPressed(): Boolean = input.isKeyPressed(Keys.CONTROL_LEFT) || input.isKeyPressed(Keys.CONTROL_RIGHT)

    private fun shiftPressed(): Boolean = input.isKeyPressed(Keys.SHIFT_LEFT) || input.isKeyPressed(Keys.SHIFT_RIGHT)

    private fun hasPendingStatAllocation(snapshot: RenderSnapshot): Boolean = snapshot.uiState.playerStatus.statPoints > 0

    private fun hasPendingTalentAllocation(snapshot: RenderSnapshot): Boolean = snapshot.uiState.playerStatus.talentPoints > 0

    private fun playerPosition(snapshot: RenderSnapshot): Point = Point(snapshot.metadata.playerX, snapshot.metadata.playerY)

    private fun pollMovementCommand(): Point? {
        movementBindings.entries.firstOrNull { (key, _) -> input.isKeyJustPressed(key) }?.let { (key, delta) ->
            heldMovementKey = key
            movementRepeatCountdown = repeatInitialDelayFrames
            return delta
        }

        val key = heldMovementKey ?: return null
        if (!input.isKeyPressed(key)) {
            resetMovementRepeat()
            return null
        }

        movementRepeatCountdown -= 1
        if (movementRepeatCountdown > 0) {
            return null
        }

        movementRepeatCountdown = repeatIntervalFrames
        return movementBindings.getValue(key)
    }

    private fun resetMovementRepeat() {
        heldMovementKey = null
        movementRepeatCountdown = repeatInitialDelayFrames
    }

    private fun stairCommandAtPlayer(snapshot: RenderSnapshot): PlayerCommand? {
        val playerPosition = playerPosition(snapshot)
        val direction =
            snapshot.mapCells
                .firstOrNull { cell -> cell.x == playerPosition.x && cell.y == playerPosition.y }
                ?.stairDirectionId
                ?: snapshot.props
                    .firstOrNull { prop -> prop.isStairAt(playerPosition) }
                    ?.stairDirectionId

        return when (direction) {
            "UP" -> PlayerCommand.Ascend
            "DOWN" -> PlayerCommand.Descend
            else -> null
        }
    }

    private fun PropRenderSnapshot.isStairAt(point: Point): Boolean =
        propTypeId == "stairs" && x == point.x && y == point.y

    private fun GridPointSnapshot.toPoint(): Point = Point(x, y)
}
