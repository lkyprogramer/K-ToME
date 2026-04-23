package com.ktome.client.ui.combat

import com.ktome.core.map.Point
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.InscriptionSlotSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.game.PlayerCommand

enum class CombatDecisionPhase {
    ACTION,
    METHOD,
    TARGET,
}

data class CombatDecisionFrameState(
    val phase: CombatDecisionPhase,
    val selectedActionId: String?,
    val selectedMethodId: String?,
    val skippedMethod: Boolean = false,
)

internal enum class CombatActionKind {
    TALENT,
    INSCRIPTION,
}

internal data class CombatMethodOption(
    val id: String,
    val labelKey: String,
    val iconKey: String = CombatAffordanceResourceKeys.METHOD_ICON,
    val audioCueKey: String = CombatAffordanceResourceKeys.METHOD_CONFIRM_AUDIO,
)

internal data class CombatActionOption(
    val id: String,
    val kind: CombatActionKind,
    val commandIndex: Int,
    val sourceAbilityId: String,
    val labelKey: String,
    val iconKey: String?,
    val audioCueKey: String?,
    val resourceCost: Int?,
    val resourceLabelKey: String?,
    val cooldownTurns: Int?,
    val range: Int?,
    val minRange: Int?,
    val requiresTarget: Boolean,
    val methodOptions: List<CombatMethodOption> = listOf(CombatMethodOption(id = "default", labelKey = "ui.combat.method.default")),
) {
    fun usesFreeCursorTargeting(): Boolean = kind == CombatActionKind.INSCRIPTION && requiresTarget

    fun command(target: Point?): PlayerCommand =
        when (kind) {
            CombatActionKind.TALENT -> PlayerCommand.UseTalent(slot = commandIndex, target = target.takeIf { requiresTarget })
            CombatActionKind.INSCRIPTION -> PlayerCommand.UseInscription(hotkey = commandIndex, target = target.takeIf { requiresTarget })
        }
}

internal data class CombatTargetOption(
    val id: String,
    val point: Point,
    val labelKey: String = "ui.combat.target.tile",
    val iconKey: String = CombatAffordanceResourceKeys.TARGET_ICON,
    val lockIconKey: String = CombatAffordanceResourceKeys.LOCK_ICON,
    val lockAudioCueKey: String = CombatAffordanceResourceKeys.TARGET_LOCK_AUDIO,
)

internal object CombatDecisionFrame {
    val initialState: CombatDecisionFrameState =
        CombatDecisionFrameState(
            phase = CombatDecisionPhase.ACTION,
            selectedActionId = null,
            selectedMethodId = null,
        )

    fun availableActions(snapshot: RenderSnapshot): List<CombatActionOption> =
        buildList {
            snapshot.uiState.talents.forEach { talent -> add(talent.toCombatAction()) }
            snapshot.uiState.inscriptions.forEach { inscription -> add(inscription.toCombatAction()) }
        }

    fun selectedAction(
        snapshot: RenderSnapshot,
        state: CombatDecisionFrameState,
    ): CombatActionOption? =
        state.selectedActionId?.let { selectedId ->
            availableActions(snapshot).firstOrNull { action -> action.id == selectedId }
        }

    fun legalTargets(
        snapshot: RenderSnapshot,
        action: CombatActionOption,
    ): List<CombatTargetOption> {
        if (!action.requiresTarget) {
            return listOf(
                CombatTargetOption(
                    id = "self",
                    point = Point(snapshot.metadata.playerX, snapshot.metadata.playerY),
                    labelKey = "ui.combat.target.self",
                ),
            )
        }
        if (action.usesFreeCursorTargeting()) {
            return emptyList()
        }
        return snapshot.uiState.targetablePositions.mapIndexed { index, point ->
            CombatTargetOption(
                id = "target-${index + 1}",
                point = point.toPoint(),
            )
        }
    }

    fun isActionDisabled(
        snapshot: RenderSnapshot,
        action: CombatActionOption,
    ): Boolean =
        disabledReasonKey(snapshot, action) != null

    fun disabledReasonKey(
        snapshot: RenderSnapshot,
        action: CombatActionOption,
    ): String? =
        when {
            action.methodOptions.isEmpty() -> CombatDecisionFeedbackKeys.DISABLED_NO_METHOD
            action.cooldownTurns != null && action.cooldownTurns > 0 -> CombatDecisionFeedbackKeys.DISABLED_COOLDOWN
            action.resourceCost != null && action.resourceCost > snapshot.uiState.playerStatus.currentResource ->
                CombatDecisionFeedbackKeys.DISABLED_RESOURCE
            else -> null
        }

    private fun TalentSlotSnapshot.toCombatAction(): CombatActionOption =
        CombatActionOption(
            id = "talent:$slot",
            kind = CombatActionKind.TALENT,
            commandIndex = slot,
            sourceAbilityId = talentId,
            labelKey = nameKey,
            iconKey = iconKey,
            audioCueKey = audioProfile,
            resourceCost = resourceCost,
            resourceLabelKey = resourceLabelKey,
            cooldownTurns = currentCooldown,
            range = range,
            minRange = minRange,
            requiresTarget = requiresTarget,
        )

    private fun InscriptionSlotSnapshot.toCombatAction(): CombatActionOption =
        CombatActionOption(
            id = "inscription:$hotkey",
            kind = CombatActionKind.INSCRIPTION,
            commandIndex = hotkey,
            sourceAbilityId = inscriptionId,
            labelKey = nameKey,
            iconKey = iconKey,
            audioCueKey = null,
            resourceCost = null,
            resourceLabelKey = null,
            cooldownTurns = cooldownRemaining,
            range = null,
            minRange = null,
            requiresTarget = requiresTarget,
        )

    private fun GridPointSnapshot.toPoint(): Point = Point(x, y)
}
