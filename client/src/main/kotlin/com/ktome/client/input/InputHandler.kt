package com.ktome.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.ktome.core.map.Point
import com.ktome.game.FoundationGameSession
import com.ktome.game.PlayerCommand

enum class UiMode {
    MAP,
    INVENTORY,
    TARGETING,
}

data class OverlayState(
    val mode: UiMode,
    val inventorySelection: Int = 0,
    val targetingSlot: Int? = null,
    val targetingCursor: Point? = null,
)

class InputHandler {
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

    fun overlayState(): OverlayState =
        OverlayState(
            mode = mode,
            inventorySelection = inventorySelection,
            targetingSlot = targetingSlot,
            targetingCursor = targetingCursor,
        )

    fun pollCommand(session: FoundationGameSession): PlayerCommand? =
        when (mode) {
            UiMode.MAP -> pollMapCommand(session)
            UiMode.INVENTORY -> pollInventoryCommand(session)
            UiMode.TARGETING -> pollTargetingCommand(session)
        }

    fun onCommandResult(
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        when (command) {
            is PlayerCommand.UseTalent -> {
                if (command.target == null) {
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

            else -> Unit
        }
    }

    private fun pollMapCommand(session: FoundationGameSession): PlayerCommand? {
        val movement = movementBindings.entries.firstOrNull { (key, _) -> Gdx.input.isKeyJustPressed(key) }?.value
        if (movement != null) {
            return PlayerCommand.Move(movement)
        }

        if (waitBindings.any(Gdx.input::isKeyJustPressed)) {
            return PlayerCommand.Wait
        }

        if (Gdx.input.isKeyJustPressed(Keys.G)) {
            return PlayerCommand.PickUp
        }

        if (Gdx.input.isKeyJustPressed(Keys.I)) {
            mode = UiMode.INVENTORY
            inventorySelection = inventorySelection.coerceAtMost((session.inventoryItems().size - 1).coerceAtLeast(0))
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
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.I)) {
            mode = UiMode.MAP
            return null
        }

        if (inventorySize == 0) {
            return null
        }

        if (Gdx.input.isKeyJustPressed(Keys.UP) || Gdx.input.isKeyJustPressed(Keys.W)) {
            inventorySelection = (inventorySelection - 1).coerceAtLeast(0)
            return null
        }

        if (Gdx.input.isKeyJustPressed(Keys.DOWN) || Gdx.input.isKeyJustPressed(Keys.X)) {
            inventorySelection = (inventorySelection + 1).coerceAtMost(inventorySize - 1)
            return null
        }

        if (
            Gdx.input.isKeyJustPressed(Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Keys.E)
        ) {
            return PlayerCommand.ActivateInventoryItem(inventorySelection)
        }

        return null
    }

    private fun pollTargetingCommand(session: FoundationGameSession): PlayerCommand? {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            clearTargeting()
            return null
        }

        val cursor = targetingCursor ?: session.playerPosition()
        val movement = movementBindings.entries.firstOrNull { (key, _) -> Gdx.input.isKeyJustPressed(key) }?.value
        if (movement != null) {
            targetingCursor =
                Point(
                    x = (cursor.x + movement.x).coerceIn(0, session.map.width - 1),
                    y = (cursor.y + movement.y).coerceIn(0, session.map.height - 1),
                )
            return null
        }

        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            return PlayerCommand.UseTalent(requireNotNull(targetingSlot), targetingCursor ?: session.playerPosition())
        }

        return null
    }

    private fun hotkeySlot(): Int? =
        when {
            Gdx.input.isKeyJustPressed(Keys.NUM_1) -> 1
            Gdx.input.isKeyJustPressed(Keys.NUM_2) -> 2
            Gdx.input.isKeyJustPressed(Keys.NUM_3) -> 3
            Gdx.input.isKeyJustPressed(Keys.NUM_4) -> 4
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
}
