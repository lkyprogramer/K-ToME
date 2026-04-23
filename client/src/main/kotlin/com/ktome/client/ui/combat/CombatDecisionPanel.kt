package com.ktome.client.ui.combat

import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.i18n.Localizer

internal data class CombatDecisionPanelRow(
    val text: String,
    val iconKey: String?,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val danger: Boolean = false,
)

internal data class CombatDecisionPanelModel(
    val title: String,
    val phaseIconKey: String,
    val confirmAudioCueKey: String,
    val rows: List<CombatDecisionPanelRow>,
)

internal data class CombatDecisionPanelRequest(
    val localizer: Localizer,
    val snapshot: RenderSnapshot,
    val state: CombatDecisionFrameState,
    val focusIndex: Int,
    val renderText: (RenderTextTokenSnapshot) -> String,
)

internal object CombatDecisionPanel {
    fun build(request: CombatDecisionPanelRequest): CombatDecisionPanelModel {
        val localizer = request.localizer
        val snapshot = request.snapshot
        val state = request.state
        val focusIndex = request.focusIndex
        val renderText = request.renderText
        val actions = CombatDecisionFrame.availableActions(snapshot)
        return when (state.phase) {
            CombatDecisionPhase.ACTION ->
                CombatDecisionPanelModel(
                    title = localizer.text("ui.combat.phase.action"),
                    phaseIconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                    confirmAudioCueKey = CombatAffordanceResourceKeys.ACTION_CONFIRM_AUDIO,
                    rows =
                        actions.mapIndexed { index, action ->
                            val hint = ActionHintModelBuilder.build(snapshot, action)
                            val missingTexts =
                                listOfNotNull(
                                    hint.legalTargetSummary.missingReason,
                                    hint.missingFactReason,
                                ).map(renderText)
                                    .distinct()
                            CombatDecisionPanelRow(
                                text =
                                    buildString {
                                        append("${index + 1}. ${localizer.text(action.labelKey)}")
                                        hint.rangeSummary?.let { rangeSummary ->
                                            append(" · ")
                                            append(renderText(rangeSummary))
                                        }
                                        hint.disabledReason?.let { reason -> append(" · ${renderText(reason)}") }
                                        missingTexts.forEach { missingText -> append(" · $missingText") }
                                    },
                                iconKey = action.iconKey ?: CombatAffordanceResourceKeys.ACTION_ICON,
                                selected = index == focusIndex.coerceIn(0, (actions.size - 1).coerceAtLeast(0)),
                                enabled = hint.disabledReason == null,
                                danger = hint.telegraphLinkage != null,
                            )
                        }.ifEmpty {
                            listOf(
                                CombatDecisionPanelRow(
                                    text = localizer.text(CombatDecisionFeedbackKeys.NO_AVAILABLE_ACTION),
                                    iconKey = CombatAffordanceResourceKeys.INVALID_ICON,
                                    enabled = false,
                                ),
                            )
                        },
                )

            CombatDecisionPhase.METHOD -> {
                val action = CombatDecisionFrame.selectedAction(snapshot, state)
                val methods = action?.methodOptions.orEmpty()
                CombatDecisionPanelModel(
                    title = localizer.text("ui.combat.phase.method"),
                    phaseIconKey = CombatAffordanceResourceKeys.METHOD_ICON,
                    confirmAudioCueKey = CombatAffordanceResourceKeys.METHOD_CONFIRM_AUDIO,
                    rows =
                        methods.mapIndexed { index, method ->
                            CombatDecisionPanelRow(
                                text = "${index + 1}. ${localizer.text(method.labelKey)}",
                                iconKey = method.iconKey,
                                selected = index == focusIndex.coerceIn(0, (methods.size - 1).coerceAtLeast(0)),
                            )
                        }.ifEmpty {
                            listOf(
                                CombatDecisionPanelRow(
                                    text = localizer.text(CombatDecisionFeedbackKeys.NO_AVAILABLE_ACTION),
                                    iconKey = CombatAffordanceResourceKeys.INVALID_ICON,
                                    enabled = false,
                                ),
                            )
                        },
                )
            }

            CombatDecisionPhase.TARGET -> {
                val action = CombatDecisionFrame.selectedAction(snapshot, state)
                val targets = action?.let { CombatDecisionFrame.legalTargets(snapshot, it) }.orEmpty()
                CombatDecisionPanelModel(
                    title = localizer.text("ui.combat.phase.target"),
                    phaseIconKey = CombatAffordanceResourceKeys.TARGET_ICON,
                    confirmAudioCueKey = CombatAffordanceResourceKeys.TARGET_CONFIRM_AUDIO,
                    rows =
                        if (targets.isEmpty()) {
                            listOf(
                                CombatDecisionPanelRow(
                                    text = localizer.text(CombatDecisionFeedbackKeys.NO_LEGAL_TARGET),
                                    iconKey = CombatAffordanceResourceKeys.INVALID_ICON,
                                    enabled = false,
                                ),
                            )
                        } else {
                            val safeFocusIndex = focusIndex.coerceIn(0, targets.size - 1)
                            targets.mapIndexed { index, target ->
                                CombatDecisionPanelRow(
                                    text =
                                        localizer.text(
                                            target.labelKey,
                                            "index" to (index + 1),
                                            "x" to target.point.x,
                                            "y" to target.point.y,
                                        ),
                                    iconKey = if (index == safeFocusIndex) target.lockIconKey else target.iconKey,
                                    selected = index == safeFocusIndex,
                                )
                            }
                        },
                )
            }
        }
    }
}
