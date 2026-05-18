package com.ktome.client.ui.layout

import com.ktome.client.ui.combat.CombatDecisionFrameState
import com.ktome.client.ui.talent.TalentTreeSelectionIdentity
import com.ktome.core.map.Point

const val MODAL_STACK_MAX_DEPTH = 3

enum class ModalFrameKind {
    INVENTORY,
    LOADOUT_EDIT,
    TALENT_ASSIGN,
    ACTIVE_TALENT_SLOT_CHOICE,
    INSPECT,
    TARGETING,
    ITEM_DETAIL,
    ITEM_COMPARE,
    COMBAT_DECISION,
}

data class ModalFrameLocalState(
    val focusIndex: Int = 0,
    val inventorySelection: Int = 0,
    val loadoutSlotSelection: Int = 1,
    val loadoutReserveSelection: Int = 0,
    val talentTreeSelection: Int = 0,
    val talentTreeSelectionIdentity: TalentTreeSelectionIdentity? = null,
    val talentTreePreviewExpanded: Boolean = true,
    val targetingSlot: Int? = null,
    val targetingInscriptionHotkey: Int? = null,
    val targetingCursor: Point? = null,
    val inspectCursor: Point? = null,
    val explainPaneOpen: Boolean = false,
    val combatDecisionState: CombatDecisionFrameState? = null,
)

data class ModalFrame(
    val kind: ModalFrameKind,
    val localState: ModalFrameLocalState = ModalFrameLocalState(),
)

internal class ModalStack(
    private val maxDepth: Int = MODAL_STACK_MAX_DEPTH,
) {
    private val frames = mutableListOf<ModalFrame>()

    val depth: Int
        get() = frames.size

    val isEmpty: Boolean
        get() = frames.isEmpty()

    val isNotEmpty: Boolean
        get() = frames.isNotEmpty()

    fun top(): ModalFrame? = frames.lastOrNull()

    fun frames(): List<ModalFrame> = frames.toList()

    fun canPush(): Boolean = frames.size < maxDepth

    fun push(frame: ModalFrame) {
        check(canPush()) { "ModalStack depth $maxDepth exceeded by ${frame.kind}." }
        frames += frame
    }

    fun pop(): ModalFrame? =
        if (frames.isEmpty()) {
            null
        } else {
            frames.removeAt(frames.lastIndex)
        }

    fun replaceTop(transform: (ModalFrame) -> ModalFrame) {
        val index = frames.lastIndex
        check(index >= 0) { "Cannot replace top frame on an empty ModalStack." }
        frames[index] = transform(frames[index])
    }

    fun clear() {
        frames.clear()
    }
}
