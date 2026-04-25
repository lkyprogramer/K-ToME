package com.ktome.client.input

import com.ktome.client.audio.AudioRouter
import com.ktome.client.ui.combat.CombatDecisionValidationSurface
import com.ktome.client.validation.ValidationScenarioPresentationCatalog
import com.ktome.client.validation.validationScenarioRequiredEvidenceKeys
import com.ktome.core.map.Point
import com.ktome.game.FoundationGameSession
import com.ktome.game.validation.ValidationAction
import com.ktome.game.validation.ValidationOverlaySection
import com.ktome.game.validation.ValidationPhase4Guide
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationScenarioActionId
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationSummarySnapshot
import com.ktome.game.validation.hasMeaningfulNextSeedRestart
import com.ktome.game.validation.validationHiddenBindingForPreset
import com.ktome.game.validation.validationPhase4Guide
import java.util.concurrent.ConcurrentHashMap

enum class ValidationOverlayAvailability {
    DISABLED,
    ENABLED,
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
    val phase4V4FastSelection: Int = 0,
) {
    fun moveSection(
        delta: Int,
        sections: List<ValidationOverlaySection> = ValidationOverlaySection.entries,
    ): ValidationOverlayCursor {
        val currentIndex = sections.indexOf(selectedSection)
        val nextIndex = (currentIndex.coerceAtLeast(0) + delta).coerceIn(0, sections.lastIndex)
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
            ValidationOverlaySection.PHASE4_V4_FAST -> phase4V4FastSelection
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
            ValidationOverlaySection.PHASE4_V4_FAST -> copy(phase4V4FastSelection = index)
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
    val scenarioContext: ValidationOverlayScenarioContext?,
    val sections: List<ValidationOverlaySectionState>,
)

data class ValidationOverlayScenarioContext(
    val titleKey: String,
    val requiredEvidenceKeys: List<String>,
)

data class ValidationOverlaySelection(
    val preset: ValidationPreset,
    val restartNextSeedEnabled: Boolean,
    val scenarioId: ValidationScenarioId? = null,
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
    val scenarioId: ValidationScenarioId? = null,
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

internal data class ValidationOverlayDescriptorPlan(
    val scope: ValidationOverlayDescriptorScope,
    val sections: List<ValidationOverlaySection>,
    val descriptorsBySection: Map<ValidationOverlaySection, List<ValidationOverlayActionDescriptor>>,
) {
    fun descriptors(section: ValidationOverlaySection): List<ValidationOverlayActionDescriptor> =
        descriptorsBySection[section].orEmpty()

    fun actionDescriptor(
        section: ValidationOverlaySection,
        index: Int,
    ): ValidationOverlayActionDescriptor? = descriptors(section).getOrNull(index)

    fun actionCount(section: ValidationOverlaySection): Int = descriptors(section).size

    fun sectionStates(cursor: ValidationOverlayCursor): List<ValidationOverlaySectionState> =
        sections.map { section ->
            val selectedIndex = cursor.actionIndex(section)
            ValidationOverlaySectionState(
                titleKey = section.titleKey,
                selected = cursor.selectedSection == section,
                actions =
                    descriptors(section).mapIndexed { index, descriptor ->
                        ValidationOverlayActionState(
                            labelKey = descriptor.labelKey,
                            selected = cursor.selectedSection == section && selectedIndex == index,
                        )
                    },
            )
        }

    companion object {
        fun build(scope: ValidationOverlayDescriptorScope): ValidationOverlayDescriptorPlan {
            val descriptorsBySection =
                ValidationOverlaySection.entries
                    .mapNotNull { section ->
                        val descriptors = buildValidationOverlayActionDescriptors(scope = scope, section = section)
                        section.takeIf { descriptors.isNotEmpty() }?.let { it to descriptors }
                    }.toMap()
            return ValidationOverlayDescriptorPlan(
                scope = scope,
                sections = descriptorsBySection.keys.toList(),
                descriptorsBySection = descriptorsBySection,
            )
        }
    }
}

internal object ValidationOverlayDescriptorPlanCache {
    private val plans = ConcurrentHashMap<ValidationOverlayDescriptorScope, ValidationOverlayDescriptorPlan>()

    fun plan(scope: ValidationOverlayDescriptorScope): ValidationOverlayDescriptorPlan =
        plans.computeIfAbsent(scope) { key -> ValidationOverlayDescriptorPlan.build(key) }
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
                    validationScenarioId = requireNotNull(session.validationSummarySnapshot()).scenarioId,
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
                phase4Guide = validationPhase4Guide(summary),
                scenarioContext = validationOverlayScenarioContext(summary),
                sections = buildValidationOverlaySections(summary, cursor),
            ),
    )
}

private fun validationOverlayScenarioContext(summary: ValidationSummarySnapshot): ValidationOverlayScenarioContext? {
    val scenarioId = summary.scenarioId ?: return null
    val presentation = ValidationScenarioPresentationCatalog.find(scenarioId) ?: return null
    return ValidationOverlayScenarioContext(
        titleKey = presentation.titleKey,
        requiredEvidenceKeys = validationScenarioRequiredEvidenceKeys(scenarioId),
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
    ValidationOverlayDescriptorPlanCache
        .plan(selection.descriptorScope())
        .actionDescriptor(section = selection.section, index = selection.index)

private fun buildValidationOverlaySections(
    summary: ValidationSummarySnapshot,
    cursor: ValidationOverlayCursor,
): List<ValidationOverlaySectionState> {
    val scope =
        ValidationOverlayDescriptorScope(
            preset = summary.preset,
            restartMode = summary.restartMode(),
            scenarioId = summary.scenarioId,
        )
    return ValidationOverlayDescriptorPlanCache.plan(scope).sectionStates(cursor)
}

internal fun availableValidationOverlaySections(scope: ValidationOverlayDescriptorScope): List<ValidationOverlaySection> =
    ValidationOverlayDescriptorPlanCache.plan(scope).sections

internal fun validationOverlayActionCount(
    scope: ValidationOverlayDescriptorScope,
    section: ValidationOverlaySection,
): Int = ValidationOverlayDescriptorPlanCache.plan(scope).actionCount(section)

private fun buildValidationOverlayActionDescriptors(
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

        ValidationOverlaySection.PHASE4_V4_FAST ->
            scope.scenarioId?.let { scenarioId ->
                ValidationScenarioActionId.entries.map { actionId ->
                    ValidationOverlayActionDescriptor(
                        labelKey = "ui.validation.action.phase4_v4.${actionId.value}",
                        buildAction = {
                            ValidationAction.Phase4V4ScenarioAction(
                                scenarioId = scenarioId,
                                actionId = actionId,
                            )
                        },
                    )
                }
            }.orEmpty()
    }

private fun ValidationOverlaySelection.restartMode(): ValidationOverlayRestartMode =
    if (restartNextSeedEnabled) {
        ValidationOverlayRestartMode.NEXT_SEED_ENABLED
    } else {
        ValidationOverlayRestartMode.SAME_PRESET_ONLY
    }

private fun ValidationOverlaySelection.descriptorScope(): ValidationOverlayDescriptorScope =
    ValidationOverlayDescriptorScope(
        preset = preset,
        restartMode = restartMode(),
        scenarioId = scenarioId,
    )

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
