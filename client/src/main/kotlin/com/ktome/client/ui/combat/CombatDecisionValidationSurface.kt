package com.ktome.client.ui.combat

import com.ktome.core.map.Point
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.i18n.Localizer

enum class CombatDecisionValidationSurface {
    METHOD,
    DISABLED_RESOURCE,
    NO_LEGAL_TARGET,
    ILLEGAL_TARGET,
    MISSING_FACT,
}

internal data class CombatDecisionValidationPanelRequest(
    val localizer: Localizer,
    val snapshot: RenderSnapshot,
    val surface: CombatDecisionValidationSurface,
    val state: CombatDecisionFrameState,
    val focusIndex: Int,
)

internal object CombatDecisionValidationFixtures {
    const val ACTION_ID: String = "validation:pr05-combat-decision"
    const val METHOD_ID: String = "validation:method"

    fun initialState(surface: CombatDecisionValidationSurface): CombatDecisionFrameState =
        when (surface) {
            CombatDecisionValidationSurface.METHOD ->
                CombatDecisionFrameState(
                    phase = CombatDecisionPhase.METHOD,
                    selectedActionId = ACTION_ID,
                    selectedMethodId = null,
                    skippedMethod = false,
                )

            CombatDecisionValidationSurface.NO_LEGAL_TARGET,
            CombatDecisionValidationSurface.ILLEGAL_TARGET,
            ->
                CombatDecisionFrameState(
                    phase = CombatDecisionPhase.TARGET,
                    selectedActionId = ACTION_ID,
                    selectedMethodId = METHOD_ID,
                    skippedMethod = false,
                )

            CombatDecisionValidationSurface.DISABLED_RESOURCE,
            CombatDecisionValidationSurface.MISSING_FACT,
            ->
                CombatDecisionFrameState(
                    phase = CombatDecisionPhase.ACTION,
                    selectedActionId = null,
                    selectedMethodId = null,
                    skippedMethod = false,
                )
        }

    fun initialCursor(
        snapshot: RenderSnapshot,
        surface: CombatDecisionValidationSurface,
    ): Point =
        when (surface) {
            CombatDecisionValidationSurface.ILLEGAL_TARGET -> illegalTargetPoint(snapshot)
            else -> legalTargetPoint(snapshot)
        }

    fun focusCount(
        surface: CombatDecisionValidationSurface,
        state: CombatDecisionFrameState,
    ): Int =
        when (state.phase) {
            CombatDecisionPhase.ACTION -> 1
            CombatDecisionPhase.METHOD -> if (surface == CombatDecisionValidationSurface.METHOD) 3 else 1
            CombatDecisionPhase.TARGET -> if (surface == CombatDecisionValidationSurface.NO_LEGAL_TARGET) 0 else 1
        }

    fun panel(request: CombatDecisionValidationPanelRequest): CombatDecisionPanelModel =
        when (request.state.phase) {
            CombatDecisionPhase.ACTION -> actionPanel(request.localizer, request.surface, request.focusIndex)
            CombatDecisionPhase.METHOD -> methodPanel(request.localizer, request.focusIndex)
            CombatDecisionPhase.TARGET ->
                targetPanel(
                    request.localizer,
                    request.snapshot,
                    request.surface,
                    request.focusIndex,
                )
        }

    fun disabledMessage(surface: CombatDecisionValidationSurface): String? =
        when (surface) {
            CombatDecisionValidationSurface.DISABLED_RESOURCE -> CombatDecisionFeedbackKeys.DISABLED_RESOURCE
            CombatDecisionValidationSurface.MISSING_FACT -> "ui.combat.fact.missing"
            else -> null
        }

    fun legalTargetPoint(snapshot: RenderSnapshot): Point =
        Point(
            x = (snapshot.metadata.playerX + 1).coerceAtMost(snapshot.metadata.width - 1),
            y = snapshot.metadata.playerY.coerceIn(0, snapshot.metadata.height - 1),
        )

    private fun illegalTargetPoint(snapshot: RenderSnapshot): Point =
        Point(
            x = (snapshot.metadata.playerX + 2).coerceAtMost(snapshot.metadata.width - 1),
            y = snapshot.metadata.playerY.coerceIn(0, snapshot.metadata.height - 1),
        )

    private fun actionPanel(
        localizer: Localizer,
        surface: CombatDecisionValidationSurface,
        focusIndex: Int,
    ): CombatDecisionPanelModel {
        val disabledReason = disabledMessage(surface)
        val labelKey =
            when (surface) {
                CombatDecisionValidationSurface.DISABLED_RESOURCE -> "ui.validation.pr05.combat.action.expensive"
                CombatDecisionValidationSurface.MISSING_FACT -> "ui.validation.pr05.combat.action.missing_fact"
                else -> "ui.validation.pr05.combat.action.multi_method"
            }
        val suffix =
            disabledReason?.let { reasonKey -> " · ${localizer.text(reasonKey)}" }.orEmpty()
        return CombatDecisionPanelModel(
            title = localizer.text("ui.combat.phase.action"),
            phaseIconKey = CombatAffordanceResourceKeys.ACTION_ICON,
            confirmAudioCueKey = CombatAffordanceResourceKeys.ACTION_CONFIRM_AUDIO,
            rows =
                listOf(
                    CombatDecisionPanelRow(
                        text = "1. ${localizer.text(labelKey)}$suffix",
                        iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                        selected = focusIndex == 0,
                        enabled = disabledReason == null,
                        danger = surface == CombatDecisionValidationSurface.METHOD,
                    ),
                ),
        )
    }

    private fun methodPanel(
        localizer: Localizer,
        focusIndex: Int,
    ): CombatDecisionPanelModel =
        CombatDecisionPanelModel(
            title = localizer.text("ui.combat.phase.method"),
            phaseIconKey = CombatAffordanceResourceKeys.METHOD_ICON,
            confirmAudioCueKey = CombatAffordanceResourceKeys.METHOD_CONFIRM_AUDIO,
            rows =
                listOf(
                    "ui.combat.method.default",
                    "ui.validation.pr05.combat.method.safe_step",
                    "ui.validation.pr05.combat.method.burst_commit",
                ).mapIndexed { index, labelKey ->
                    CombatDecisionPanelRow(
                        text = "${index + 1}. ${localizer.text(labelKey)}",
                        iconKey = CombatAffordanceResourceKeys.METHOD_ICON,
                        selected = index == focusIndex.coerceIn(0, 2),
                    )
                },
        )

    private fun targetPanel(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        surface: CombatDecisionValidationSurface,
        focusIndex: Int,
    ): CombatDecisionPanelModel =
        CombatDecisionPanelModel(
            title = localizer.text("ui.combat.phase.target"),
            phaseIconKey = CombatAffordanceResourceKeys.TARGET_ICON,
            confirmAudioCueKey = CombatAffordanceResourceKeys.TARGET_CONFIRM_AUDIO,
            rows =
                if (surface == CombatDecisionValidationSurface.NO_LEGAL_TARGET) {
                    listOf(
                        CombatDecisionPanelRow(
                            text = localizer.text(CombatDecisionFeedbackKeys.NO_LEGAL_TARGET),
                            iconKey = CombatAffordanceResourceKeys.INVALID_ICON,
                            enabled = false,
                        ),
                    )
                } else {
                    val target = legalTargetPoint(snapshot)
                    listOf(
                        CombatDecisionPanelRow(
                            text =
                                localizer.text(
                                    "ui.combat.target.tile",
                                    "index" to 1,
                                    "x" to target.x,
                                    "y" to target.y,
                                ),
                            iconKey = CombatAffordanceResourceKeys.TARGET_ICON,
                            selected = focusIndex == 0,
                        ),
                    )
                },
        )
}
