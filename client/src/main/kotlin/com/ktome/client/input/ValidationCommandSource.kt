package com.ktome.client.input

import com.ktome.client.audio.AudioRouter
import com.ktome.client.ui.combat.CombatDecisionValidationSurface
import com.ktome.core.map.Point
import com.ktome.game.FoundationGameSession
import com.ktome.game.validation.ValidationAction
import com.ktome.game.validation.ValidationPhase4Guide
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationSummarySnapshot
import com.ktome.game.validation.hasMeaningfulNextSeedRestart
import com.ktome.game.validation.validationHiddenBindingForPreset
import com.ktome.game.validation.validationPhase4Guide

enum class ValidationOverlayAvailability {
    DISABLED,
    ENABLED,
}

enum class ValidationOverlaySection(
    val titleKey: String,
) {
    RESTART(titleKey = "ui.validation.section.restart"),
    PR05_COMBAT(titleKey = "ui.validation.section.pr05_combat"),
    TRAVEL(titleKey = "ui.validation.section.travel"),
    RECOVERY(titleKey = "ui.validation.section.recovery"),
    ENCOUNTER(titleKey = "ui.validation.section.encounter"),
    TERRAIN(titleKey = "ui.validation.section.terrain"),
    REWARD_AND_ITEM(titleKey = "ui.validation.section.reward_and_item"),
    DISCOVERY(titleKey = "ui.validation.section.discovery"),
}

data class ValidationOverlayCursor(
    val selectedSection: ValidationOverlaySection = ValidationOverlaySection.RESTART,
    val restartSelection: Int = 0,
    val travelSelection: Int = 0,
    val recoverySelection: Int = 0,
    val encounterSelection: Int = 0,
    val terrainSelection: Int = 0,
    val rewardAndItemSelection: Int = 0,
    val discoverySelection: Int = 0,
    val pr05CombatSelection: Int = 0,
) {
    fun moveSection(delta: Int): ValidationOverlayCursor {
        val sections = ValidationOverlaySection.entries
        val currentIndex = sections.indexOf(selectedSection)
        val nextIndex = (currentIndex + delta).coerceIn(0, sections.lastIndex)
        return copy(selectedSection = sections[nextIndex])
    }

    fun moveAction(
        delta: Int,
        actionCount: Int,
    ): ValidationOverlayCursor {
        val nextIndex = (actionIndex(selectedSection) + delta).coerceIn(0, actionCount - 1)
        return withActionIndex(selectedSection, nextIndex)
    }

    fun actionIndex(section: ValidationOverlaySection): Int =
        when (section) {
            ValidationOverlaySection.RESTART -> restartSelection
            ValidationOverlaySection.TRAVEL -> travelSelection
            ValidationOverlaySection.RECOVERY -> recoverySelection
            ValidationOverlaySection.ENCOUNTER -> encounterSelection
            ValidationOverlaySection.TERRAIN -> terrainSelection
            ValidationOverlaySection.REWARD_AND_ITEM -> rewardAndItemSelection
            ValidationOverlaySection.DISCOVERY -> discoverySelection
            ValidationOverlaySection.PR05_COMBAT -> pr05CombatSelection
        }

    private fun withActionIndex(
        section: ValidationOverlaySection,
        index: Int,
    ): ValidationOverlayCursor =
        when (section) {
            ValidationOverlaySection.RESTART -> copy(restartSelection = index)
            ValidationOverlaySection.TRAVEL -> copy(travelSelection = index)
            ValidationOverlaySection.RECOVERY -> copy(recoverySelection = index)
            ValidationOverlaySection.ENCOUNTER -> copy(encounterSelection = index)
            ValidationOverlaySection.TERRAIN -> copy(terrainSelection = index)
            ValidationOverlaySection.REWARD_AND_ITEM -> copy(rewardAndItemSelection = index)
            ValidationOverlaySection.DISCOVERY -> copy(discoverySelection = index)
            ValidationOverlaySection.PR05_COMBAT -> copy(pr05CombatSelection = index)
        }
}

data class ValidationOverlayActionState(
    val labelKey: String,
    val selected: Boolean,
)

data class ValidationOverlaySectionState(
    val titleKey: String,
    val selected: Boolean,
    val actions: List<ValidationOverlayActionState>,
)

data class ValidationOverlayPanelState(
    val summary: ValidationSummarySnapshot,
    val zoneNameKey: String,
    val inspectCursor: Point,
    val phase4Guide: ValidationPhase4Guide,
    val sections: List<ValidationOverlaySectionState>,
)

data class ValidationOverlaySelection(
    val preset: ValidationPreset,
    val restartNextSeedEnabled: Boolean,
    val section: ValidationOverlaySection,
    val index: Int,
    val inspectCursor: Point,
)

internal enum class ValidationOverlayRestartMode {
    SAME_PRESET_ONLY,
    NEXT_SEED_ENABLED,
}

internal data class ValidationOverlayDescriptorScope(
    val preset: ValidationPreset,
    val restartMode: ValidationOverlayRestartMode,
)

internal data class ValidationOverlayActionDescriptor(
    val labelKey: String,
    val buildAction: ((Point) -> ValidationAction)? = null,
    val combatDecisionSurface: CombatDecisionValidationSurface? = null,
) {
    fun requireGameAction(inspectCursor: Point): ValidationAction =
        buildAction?.invoke(inspectCursor)
            ?: error("Validation action $labelKey is client-only and cannot be dispatched to the game session.")
}

class ValidationCommandSource(
    private val session: FoundationGameSession,
    private val delegate: CommandSource =
        InputHandlerCommandSource(
            inputHandler =
                InputHandler(
                    validationOverlayAvailability = ValidationOverlayAvailability.ENABLED,
                    validationPreset = requireNotNull(session.validationSummarySnapshot()).preset,
                    validationRestartNextSeedEnabled =
                        requireNotNull(session.validationSummarySnapshot()).hasMeaningfulNextSeedRestart(),
                ),
        ),
) : CommandSource by delegate, AudioRouterAwareCommandSource {
    override var audioRouter: AudioRouter?
        get() = (delegate as? AudioRouterAwareCommandSource)?.audioRouter
        set(value) {
            (delegate as? AudioRouterAwareCommandSource)?.audioRouter = value
        }

    override fun overlayState(): OverlayState = enrichValidationOverlayState(session, delegate.overlayState())
}

internal fun enrichValidationOverlayState(
    session: FoundationGameSession,
    overlayState: OverlayState,
): OverlayState {
    if (overlayState.mode != UiMode.VALIDATION) {
        return overlayState
    }
    val summary = session.validationSummarySnapshot() ?: return overlayState
    val snapshot = session.renderSnapshot()
    val inspectCursor =
        overlayState.inspectCursor
            ?: Point(snapshot.metadata.playerX, snapshot.metadata.playerY)
    val cursor = overlayState.validationCursor ?: ValidationOverlayCursor()
    return overlayState.copy(
        validationPanel =
            ValidationOverlayPanelState(
                summary = summary,
                zoneNameKey = snapshot.metadata.zoneNameKey,
                inspectCursor = inspectCursor,
                phase4Guide = validationPhase4Guide(summary.preset),
                sections = buildValidationOverlaySections(summary, cursor),
            ),
    )
}

internal fun validationOverlayAction(
    selection: ValidationOverlaySelection,
): ValidationAction =
    validationOverlayActionDescriptor(selection)?.requireGameAction(selection.inspectCursor)
        ?: error("Unsupported ${selection.section} action index ${selection.index}.")

internal fun validationOverlayActionDescriptor(
    selection: ValidationOverlaySelection,
): ValidationOverlayActionDescriptor? =
    validationOverlayActionDescriptors(
        scope =
            ValidationOverlayDescriptorScope(
                preset = selection.preset,
                restartMode = selection.restartMode(),
            ),
        section = selection.section,
    ).getOrNull(selection.index)

private fun buildValidationOverlaySections(
    summary: ValidationSummarySnapshot,
    cursor: ValidationOverlayCursor,
): List<ValidationOverlaySectionState> =
    ValidationOverlaySection.entries.map { section ->
        val selectedIndex = cursor.actionIndex(section)
        val actionDescriptors =
            validationOverlayActionDescriptors(
                scope =
                    ValidationOverlayDescriptorScope(
                        preset = summary.preset,
                        restartMode = summary.restartMode(),
                    ),
                section = section,
            )
        ValidationOverlaySectionState(
            titleKey = section.titleKey,
            selected = cursor.selectedSection == section,
            actions =
                actionDescriptors.mapIndexed { index, descriptor ->
                    ValidationOverlayActionState(
                        labelKey = descriptor.labelKey,
                        selected = cursor.selectedSection == section && selectedIndex == index,
                    )
                },
        )
    }

internal fun validationOverlayActionDescriptors(
    scope: ValidationOverlayDescriptorScope,
    section: ValidationOverlaySection,
): List<ValidationOverlayActionDescriptor> =
    when (section) {
        ValidationOverlaySection.RESTART ->
            buildList {
                add(
                    ValidationOverlayActionDescriptor(
                        labelKey = "ui.validation.action.restart.same_preset",
                        buildAction = { ValidationAction.RestartSamePreset },
                    ),
                )
                if (scope.restartMode == ValidationOverlayRestartMode.NEXT_SEED_ENABLED) {
                    add(
                        ValidationOverlayActionDescriptor(
                            labelKey = "ui.validation.action.restart.next_seed",
                            buildAction = { ValidationAction.RestartNextSeed },
                        ),
                    )
                }
            }

        ValidationOverlaySection.TRAVEL -> validationTravelActions(scope.preset)

        ValidationOverlaySection.RECOVERY ->
            listOf(
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.recovery.full_heal",
                    buildAction = { ValidationAction.FullHeal },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.recovery.restore_resources",
                    buildAction = { ValidationAction.RestoreResources },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.recovery.reset_cooldowns",
                    buildAction = { ValidationAction.ResetCooldowns },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.recovery.grant_shards",
                    buildAction = { ValidationAction.GrantShards(amount = 50) },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.recovery.grant_stat",
                    buildAction = { ValidationAction.GrantStatPoints(amount = 1) },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.recovery.grant_talent",
                    buildAction = { ValidationAction.GrantTalentPoints(amount = 1) },
                ),
            )

        ValidationOverlaySection.ENCOUNTER ->
            listOf(
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.encounter.spawn_elite",
                    buildAction = { ValidationAction.SpawnEliteNearPlayer },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.encounter.trigger_boss_telegraph",
                    buildAction = { ValidationAction.TriggerBossTelegraph },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.encounter.kill_hostile",
                    buildAction = { ValidationAction.KillNearestHostile },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.encounter.kill_elite",
                    buildAction = { ValidationAction.KillNearestElite },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.encounter.kill_boss",
                    buildAction = { ValidationAction.KillActiveBoss },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.encounter.force_defeat",
                    buildAction = { ValidationAction.ForcePlayerDefeat },
                ),
            )

        ValidationOverlaySection.TERRAIN ->
            listOf(
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.terrain.set_water",
                    buildAction = { inspectCursor ->
                        ValidationAction.SetTerrainOverride(
                            point = inspectCursor,
                            terrainOverride =
                                com.ktome.core.mapgen.TerrainOverride(
                                    terrainTags = setOf(com.ktome.core.mapgen.TerrainTag.WATER),
                                    sourceRuleId = "validation.overlay.water",
                                    remainingTurns = 3,
                                ),
                        )
                    },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.terrain.clear_cursor",
                    buildAction = { inspectCursor -> ValidationAction.ClearTerrainOverride(point = inspectCursor) },
                ),
            )

        ValidationOverlaySection.REWARD_AND_ITEM ->
            listOf(
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.reward.present",
                    buildAction = {
                        ValidationAction.PresentReward(
                            profileIds = emptyList(),
                            fallbackBaseId = "healing_potion",
                            sourceId = "validation.overlay.reward",
                        )
                    },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.reward.spawn_potion",
                    buildAction = { ValidationAction.SpawnItem(baseItemId = "healing_potion") },
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.reward.pr03_showcase",
                    buildAction = { ValidationAction.SpawnPr03ItemShowcase },
                ),
            )

        ValidationOverlaySection.DISCOVERY ->
            listOf(
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.discovery.execute_search",
                    buildAction = { ValidationAction.ExecuteSearch },
                ),
            )

        ValidationOverlaySection.PR05_COMBAT ->
            listOf(
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.pr05_combat.method",
                    combatDecisionSurface = CombatDecisionValidationSurface.METHOD,
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.pr05_combat.disabled_resource",
                    combatDecisionSurface = CombatDecisionValidationSurface.DISABLED_RESOURCE,
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.pr05_combat.no_legal_target",
                    combatDecisionSurface = CombatDecisionValidationSurface.NO_LEGAL_TARGET,
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.pr05_combat.illegal_target",
                    combatDecisionSurface = CombatDecisionValidationSurface.ILLEGAL_TARGET,
                ),
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.pr05_combat.missing_fact",
                    combatDecisionSurface = CombatDecisionValidationSurface.MISSING_FACT,
                ),
            )
    }

private fun ValidationOverlaySelection.restartMode(): ValidationOverlayRestartMode =
    if (restartNextSeedEnabled) {
        ValidationOverlayRestartMode.NEXT_SEED_ENABLED
    } else {
        ValidationOverlayRestartMode.SAME_PRESET_ONLY
    }

private fun ValidationSummarySnapshot.restartMode(): ValidationOverlayRestartMode =
    if (hasMeaningfulNextSeedRestart()) {
        ValidationOverlayRestartMode.NEXT_SEED_ENABLED
    } else {
        ValidationOverlayRestartMode.SAME_PRESET_ONLY
    }

private fun validationTravelActions(preset: ValidationPreset): List<ValidationOverlayActionDescriptor> =
    if (validationHiddenBindingForPreset(preset) != null) {
        val hiddenBindingId = requireNotNull(validationHiddenBindingForPreset(preset))
        listOf(
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.search_anchor",
                buildAction = { ValidationAction.TravelToSearchBinding(hiddenBindingId) },
            ),
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.hidden_entrance",
                buildAction = { ValidationAction.TravelToHiddenEntrance(hiddenBindingId) },
            ),
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.secret_reward",
                buildAction = { ValidationAction.TravelToSecretReward(hiddenBindingId) },
            ),
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.secret_return",
                buildAction = { ValidationAction.TravelToSecretReturn(hiddenBindingId) },
            ),
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.inspect_cursor",
                buildAction = { inspectCursor -> ValidationAction.TravelToPoint(inspectCursor) },
            ),
        )
    } else {
        listOf(
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.stair_down",
                buildAction = { ValidationAction.TravelToStair(com.ktome.core.dungeon.StairDirection.DOWN) },
            ),
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.stair_up",
                buildAction = { ValidationAction.TravelToStair(com.ktome.core.dungeon.StairDirection.UP) },
            ),
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.boss",
                buildAction = { ValidationAction.TravelToBoss },
            ),
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.pending_objective",
                buildAction = { ValidationAction.TravelToPendingObjective },
            ),
            if (preset == ValidationPreset.LOOT_LAB) {
                ValidationOverlayActionDescriptor(
                    labelKey = "ui.validation.action.travel.merchant_stall",
                    buildAction = { ValidationAction.TravelToInteractable("merchant_stall") },
                )
            } else {
                null
            },
            ValidationOverlayActionDescriptor(
                labelKey = "ui.validation.action.travel.inspect_cursor",
                buildAction = { inspectCursor -> ValidationAction.TravelToPoint(inspectCursor) },
            ),
        ).filterNotNull()
    }
