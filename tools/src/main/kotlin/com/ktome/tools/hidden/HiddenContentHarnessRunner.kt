package com.ktome.tools.hidden

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.map.Point
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.center
import com.ktome.core.phase.PackId
import com.ktome.core.snapshot.FrontstageActionCategorySnapshot
import com.ktome.core.snapshot.FrontstageActionPrioritySnapshot
import com.ktome.core.world.solvability.NodeAnchorId
import com.ktome.core.world.solvability.SearchActionResult
import com.ktome.core.world.solvability.SearchBindingId
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.data.DataLoader
import com.ktome.game.hidden.HiddenConditionKey
import com.ktome.game.hidden.HiddenEventDef
import com.ktome.game.hidden.HiddenEventRewardKey
import com.ktome.game.hidden.HiddenEventRewardPayload
import com.ktome.game.hidden.HiddenTriggerType
import com.ktome.game.hidden.SecretZoneDef
import com.ktome.game.i18n.GameLocale
import com.ktome.tools.mapgen.phase4HarnessHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class HiddenContentHarnessRun(
    val totalCases: Int,
    val failureCount: Int,
    val summaryPath: Path,
    val eventsPath: Path,
)

data class HiddenContentCaseSpec(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val searchBindingId: String,
    val primerActionId: String,
)

data class HiddenFrontstageActionCueEvidence(
    val category: String,
    val priority: String,
    val stableKey: String,
    val messageKey: String,
)

data class HiddenContentCaseResult(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val searchBindingId: String,
    val primerActionId: String,
    val primerActionUsed: Boolean,
    val entranceBindingId: String,
    val resolvedReturnBridgeNodeId: String,
    val searchActionResult: String,
    val explicitSearchReveal: Boolean,
    val triggerType: String,
    val hiddenEventIds: List<String>,
    val triggerTypes: List<String>,
    val triggerPathClasses: List<String>,
    val optionalOnlyTriggerPathClasses: List<String>,
    val secretZoneId: String?,
    val secretZoneEntered: Boolean,
    val secretRewardNodePresent: Boolean,
    val criticalPathReachable: Boolean,
    val searchFailureKeepsMainlineReachable: Boolean,
    val returnedToMainline: Boolean,
    val returnedRoomNodeId: String?,
    val returnedPoint: String?,
    val expectedReturnPoint: String?,
    val returnBridgeMatchesResolvedNodeId: Boolean,
    val proofSearchActionResult: String?,
    val solvabilityProofMatchesSearchAction: Boolean,
    val solvabilityProofCoversReturnBridge: Boolean,
    val rewardSources: List<String>,
    val rewardBudgetSources: List<String>,
    val expectedRewardBudgetSources: List<String>,
    val threatBudgetSources: List<String>,
    val expectedThreatBudgetSources: List<String>,
    val logKeys: List<String>,
    val frontstageActionCues: List<HiddenFrontstageActionCueEvidence>,
    val failure: String? = null,
) {
    val frontstageActionCueCategories: List<String>
        get() = frontstageActionCues.map(HiddenFrontstageActionCueEvidence::category)

    val frontstageActionCuePriorities: List<String>
        get() = frontstageActionCues.map(HiddenFrontstageActionCueEvidence::priority)

    val frontstageActionCueStableKeys: List<String>
        get() = frontstageActionCues.map(HiddenFrontstageActionCueEvidence::stableKey)

    val frontstageActionCueMessageKeys: List<String>
        get() = frontstageActionCues.map(HiddenFrontstageActionCueEvidence::messageKey)

    val optionalOnlyTriggerPathClassesWithinOptionalOrSecret: Boolean
        get() =
            optionalOnlyTriggerPathClasses.all { pathClass ->
                pathClass == PathClass.OPTIONAL.name || pathClass == PathClass.SECRET.name
            }

    val rewardBridgeBackedByLootBudget: Boolean
        get() = expectedRewardBudgetSources.all(rewardBudgetSources::contains)

    val encounterBridgeBackedByThreatBudget: Boolean
        get() = expectedThreatBudgetSources.all(threatBudgetSources::contains)

    fun gateFailureReasons(): List<String> =
        buildList {
            if (failure != null) {
                add("case.runtime_failure")
            }
            if (resolvedReturnBridgeNodeId.isBlank()) {
                add("case.return_bridge_missing")
            }
            if (!criticalPathReachable) {
                add("case.mainline_path_unreachable")
            }
            if (hiddenEventIds.isNotEmpty() && !optionalOnlyTriggerPathClassesWithinOptionalOrSecret) {
                add("case.optional_only_hidden_event_outside_optional_or_secret")
            }
            if (secretZoneEntered && !secretRewardNodePresent) {
                add("case.secret_reward_node_missing")
            }
            if (!rewardBridgeBackedByLootBudget) {
                add("case.reward_bridge_outside_loot_budget")
            }
            if (!encounterBridgeBackedByThreatBudget) {
                add("case.secret_encounter_outside_threat_budget")
            }
            if (searchActionResult == SearchActionResult.FAILED_CHECK.name && !searchFailureKeepsMainlineReachable) {
                add("case.search_failure_blocks_mainline")
            }
            if (!solvabilityProofMatchesSearchAction) {
                add("case.search_result_proof_mismatch")
            }
            if (!solvabilityProofCoversReturnBridge) {
                add("case.return_bridge_not_reachable_in_proof")
            }
            if (!returnedToMainline) {
                add("case.did_not_return_to_mainline")
            }
        }

    fun toJson(header: HarnessReportHeader): JsonObject =
        buildJsonObject {
            put("buildId", header.buildId)
            put("phaseId", header.phaseId)
            put("locale", header.locale)
            put("contentSchemaVersion", header.contentSchemaVersion)
            put("searchRuleVersion", header.searchRuleVersion)
            put("secretRuleVersion", header.secretRuleVersion)
            put("seed", seed)
            put("zoneId", zoneId)
            put("floorIndex", floorIndex)
            put("searchBindingId", searchBindingId)
            put("primerActionId", primerActionId)
            put("primerActionUsed", primerActionUsed)
            put("entranceBindingId", entranceBindingId)
            put("resolvedReturnBridgeNodeId", resolvedReturnBridgeNodeId)
            put("searchActionResult", searchActionResult)
            put("explicitSearchReveal", explicitSearchReveal)
            put("triggerType", triggerType)
            put("secretZoneId", secretZoneId)
            put("secretZoneEntered", secretZoneEntered)
            put("secretRewardNodePresent", secretRewardNodePresent)
            put("criticalPathReachable", criticalPathReachable)
            put("searchFailureKeepsMainlineReachable", searchFailureKeepsMainlineReachable)
            put("returnedToMainline", returnedToMainline)
            put("returnedRoomNodeId", returnedRoomNodeId)
            put("returnedPoint", returnedPoint)
            put("expectedReturnPoint", expectedReturnPoint)
            put("returnBridgeMatchesResolvedNodeId", returnBridgeMatchesResolvedNodeId)
            put("proofSearchActionResult", proofSearchActionResult)
            put("solvabilityProofMatchesSearchAction", solvabilityProofMatchesSearchAction)
            put("solvabilityProofCoversReturnBridge", solvabilityProofCoversReturnBridge)
            put("triggerPathClassesWithinOptionalOrSecret", optionalOnlyTriggerPathClassesWithinOptionalOrSecret)
            put("optionalOnlyTriggerPathClassesWithinOptionalOrSecret", optionalOnlyTriggerPathClassesWithinOptionalOrSecret)
            put("rewardBridgeBackedByLootBudget", rewardBridgeBackedByLootBudget)
            put("encounterBridgeBackedByThreatBudget", encounterBridgeBackedByThreatBudget)
            putJsonArray("triggerTypes") {
                triggerTypes.forEach { triggerType -> add(JsonPrimitive(triggerType)) }
            }
            putJsonArray("triggerPathClasses") {
                triggerPathClasses.forEach { pathClass -> add(JsonPrimitive(pathClass)) }
            }
            putJsonArray("optionalOnlyTriggerPathClasses") {
                optionalOnlyTriggerPathClasses.forEach { pathClass -> add(JsonPrimitive(pathClass)) }
            }
            putJsonArray("hiddenEventIds") {
                hiddenEventIds.forEach { hiddenEventId -> add(JsonPrimitive(hiddenEventId)) }
            }
            putJsonArray("rewardSources") {
                rewardSources.forEach { rewardSource -> add(JsonPrimitive(rewardSource)) }
            }
            putJsonArray("rewardBudgetSources") {
                rewardBudgetSources.forEach { rewardSource -> add(JsonPrimitive(rewardSource)) }
            }
            putJsonArray("expectedRewardBudgetSources") {
                expectedRewardBudgetSources.forEach { rewardSource -> add(JsonPrimitive(rewardSource)) }
            }
            putJsonArray("threatBudgetSources") {
                threatBudgetSources.forEach { threatSource -> add(JsonPrimitive(threatSource)) }
            }
            putJsonArray("expectedThreatBudgetSources") {
                expectedThreatBudgetSources.forEach { threatSource -> add(JsonPrimitive(threatSource)) }
            }
            putJsonArray("logKeys") {
                logKeys.forEach { logKey -> add(JsonPrimitive(logKey)) }
            }
            putJsonArray("frontstageActionCueCategories") {
                frontstageActionCueCategories.forEach { category -> add(JsonPrimitive(category)) }
            }
            putJsonArray("frontstageActionCuePriorities") {
                frontstageActionCuePriorities.forEach { priority -> add(JsonPrimitive(priority)) }
            }
            putJsonArray("frontstageActionCueStableKeys") {
                frontstageActionCueStableKeys.forEach { stableKey -> add(JsonPrimitive(stableKey)) }
            }
            putJsonArray("frontstageActionCueMessageKeys") {
                frontstageActionCueMessageKeys.forEach { messageKey -> add(JsonPrimitive(messageKey)) }
            }
            putJsonArray("caseFailureReasons") {
                gateFailureReasons().forEach { reason -> add(JsonPrimitive(reason)) }
            }
            failure?.let { put("failure", it) }
        }
}

internal data class HiddenContentKernelRun(
    val header: HarnessReportHeader,
    val results: List<HiddenContentCaseResult>,
)

internal data class HiddenContentZoneMetrics(
    val caseCount: Int,
    val hiddenEventTriggerCount: Int,
    val secretZoneDiscoveryCount: Int,
) {
    val hiddenEventTriggerRate: Double
        get() = if (caseCount == 0) 0.0 else hiddenEventTriggerCount.toDouble() / caseCount.toDouble()

    val secretZoneDiscoveryRate: Double
        get() = if (caseCount == 0) 0.0 else secretZoneDiscoveryCount.toDouble() / caseCount.toDouble()
}

internal data class HiddenContentSummaryMetrics(
    val totalCases: Int,
    val distinctSeedCount: Int,
    val caseFailureCount: Int,
    val aggregateFailureCount: Int,
    val hiddenEventTriggerCount: Int,
    val hiddenEventTriggerRate: Double,
    val secretZoneDiscoveryCount: Int,
    val secretZoneDiscoveryRate: Double,
    val explicitSearchRevealCount: Int,
    val primerActionUsedCount: Int,
    val primerFreeCaseCount: Int,
    val searchFailureCount: Int,
    val zeroHiddenEventZoneCount: Int,
    val zeroSecretZoneZoneCount: Int,
    val criticalPathFailureCount: Int,
    val triggerContextFailureCount: Int,
    val secretRewardNodeMissingCount: Int,
    val rewardBudgetFailureCount: Int,
    val threatBudgetFailureCount: Int,
    val searchFailureBlockingCount: Int,
    val proofMismatchCount: Int,
    val runtimeReturnDestinationMismatchCount: Int,
    val hiddenEventRegistryCount: Int,
    val secretZoneRegistryCount: Int,
    val hiddenTriggerTypeCoverage: Double,
    val hiddenTriggerTypeSet: Set<String>,
    val secretEntranceBindingCoverage: Int,
    val secretEntranceBindingSet: Set<String>,
) {
    val failureCount: Int
        get() = caseFailureCount + aggregateFailureCount
}

internal data class HiddenContentAnalysis(
    val summary: HiddenContentSummaryMetrics,
    val zoneBreakdown: Map<String, HiddenContentZoneMetrics>,
    val aggregateFailures: List<String>,
)

internal data class HiddenContentRegistryMetrics(
    val hiddenTriggerTypeSet: Set<String>,
    val secretEntranceBindingSet: Set<String>,
    val hiddenEventRegistryCount: Int,
    val secretZoneRegistryCount: Int,
) {
    val hiddenTriggerTypeCoverage: Double
        get() = hiddenTriggerTypeSet.size.toDouble() / HiddenTriggerType.entries.size.toDouble()

    val secretEntranceBindingCoverage: Int
        get() = secretEntranceBindingSet.size
}

internal const val MIN_HIDDEN_EVENT_TRIGGER_RATE: Double = 0.30
internal const val MIN_SECRET_ZONE_DISCOVERY_RATE: Double = 0.10
internal const val MIN_HIDDEN_EVENT_REGISTRY_COUNT: Int = 12
internal const val MIN_HIDDEN_TRIGGER_TYPE_COVERAGE: Double = 4.0 / 6.0
internal const val MIN_SECRET_ENTRANCE_BINDING_COVERAGE: Int = 3

internal object HiddenContentRegistrySnapshot {
    fun load(): HiddenContentRegistryMetrics {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        return HiddenContentRegistryMetrics(
            hiddenTriggerTypeSet = catalog.hiddenEvents.mapTo(linkedSetOf()) { hiddenEvent -> hiddenEvent.triggerType.name },
            secretEntranceBindingSet = catalog.secretZones.mapTo(linkedSetOf()) { secretZone -> secretZone.entranceBindingId.value },
            hiddenEventRegistryCount = catalog.hiddenEvents.size,
            secretZoneRegistryCount = catalog.secretZones.size,
        )
    }
}

internal object HiddenContentHarnessKernel {
    private const val FLOOR_INDEX: Int = 1
    private const val SEED_BASE: Long = 20260407070000L
    private const val ZONE_SEED_BLOCK: Long = 1_000L
    private const val SEEDS_PER_ZONE: Int = 125
    private const val CRYSTAL_CACHE_CHEST_ID: String = "crystal_cache_chest"
    private const val TEMPLE_WARD_RELIQUARY_ID: String = "temple_ward_reliquary"
    private val hiddenBindingScenarios: List<HiddenBindingScenario> =
        listOf(
            HiddenBindingScenario(
                zoneId = "greenwood_fringe",
                searchBindingId = SearchBindingId("search.greenwood.hidden_cache"),
                primerAction = HiddenPrimerAction.NONE,
            ),
            HiddenBindingScenario(
                zoneId = "deep_iron_pit",
                searchBindingId = SearchBindingId("search.deep_iron.slag_cache"),
                primerAction = HiddenPrimerAction.FORCE_ELITE_KILL,
            ),
            HiddenBindingScenario(
                zoneId = "deep_iron_pit",
                searchBindingId = SearchBindingId("search.deep_iron.smuggler_stash"),
                primerAction = HiddenPrimerAction.ENTER_HIDDEN_BRANCH_ROOM,
            ),
            HiddenBindingScenario(
                zoneId = "underground_river",
                searchBindingId = SearchBindingId("search.underground_river.crystal_rift"),
                primerAction = HiddenPrimerAction.OPEN_CRYSTAL_CACHE_CHEST,
            ),
            HiddenBindingScenario(
                zoneId = "abyssal_temple",
                searchBindingId = SearchBindingId("search.abyssal_temple.warded_archive"),
                primerAction = HiddenPrimerAction.CLAIM_WARD_RELIQUARY,
            ),
        )

    private data class HiddenBindingScenario(
        val zoneId: String,
        val searchBindingId: SearchBindingId,
        val primerAction: HiddenPrimerAction,
    )

    private enum class HiddenPrimerAction {
        NONE,
        FORCE_ELITE_KILL,
        OPEN_CRYSTAL_CACHE_CHEST,
        CLAIM_WARD_RELIQUARY,
        ENTER_HIDDEN_BRANCH_ROOM,
    }

    private data class HiddenPrimerExecution(
        val triggerPathClass: String? = null,
    )

    fun execute(): HiddenContentKernelRun {
        val cases =
            hiddenBindingScenarios.flatMapIndexed { scenarioOrdinal, scenario ->
                (0 until SEEDS_PER_ZONE).map { seedOrdinal ->
                    HiddenContentCaseSpec(
                        zoneId = scenario.zoneId,
                        floorIndex = FLOOR_INDEX,
                        seed = SEED_BASE + scenarioOrdinal * ZONE_SEED_BLOCK + seedOrdinal,
                        searchBindingId = scenario.searchBindingId.value,
                        primerActionId = scenario.primerAction.name,
                    )
                }
            }
        val header = phase4HarnessHeader(harnessId = HiddenContentHarnessRunner.HARNESS_ID, seedList = cases.map(HiddenContentCaseSpec::seed))
        val results = cases.map(::executeCase)
        return HiddenContentKernelRun(header = header, results = results)
    }

    private fun executeCase(caseSpec: HiddenContentCaseSpec): HiddenContentCaseResult =
        try {
            val bindingId = SearchBindingId(caseSpec.searchBindingId)
            val session =
                GameModule.newFoundationSession(
                    config =
                        FoundationGameConfig(
                            seed = caseSpec.seed,
                            zoneId = caseSpec.zoneId,
                            floor = caseSpec.floorIndex,
                            playerProfessionId = "arcanist",
                        ),
                    saveManager = com.ktome.core.save.SaveManager(reportDir().resolve("tmp").resolve("${caseSpec.zoneId}-${caseSpec.seed}")),
                    locale = GameLocale.EN_US,
                )
            val generatedFloor = session.automationGeneratedFloor()
            val entrance = requireNotNull(generatedFloor.entranceByBinding(bindingId)) {
                "Missing generated entrance for binding '${bindingId.value}'."
            }
            val primer = primeScenario(session = session, caseSpec = caseSpec, generatedFloor = generatedFloor)
            val entranceRoom = requireNotNull(generatedFloor.roomForEntrance(entrance))
            val searchPoint = requireNotNull(session.automationSearchPointForBinding(bindingId)) {
                "Missing search point for binding '${bindingId.value}'."
            }
            val searchStateBefore = session.automationSearchState().firstOrNull { entry -> entry.bindingId == bindingId }?.result
            session.automationMovePlayerTo(searchPoint)
            val searchAccepted = session.perform(PlayerCommand.Search)
            val searchResult =
                session.automationSearchState()
                    .firstOrNull { entry -> entry.bindingId == entrance.bindingId }
                    ?.result
                    ?: SearchActionResult.NO_TARGET
            var secretRewardNodePresent = false
            var secretRewardPathClass = PathClass.SECRET.name

            if (searchResult == SearchActionResult.REVEALED) {
                session.automationHiddenEntrancePointForBinding(bindingId)?.let { propPoint ->
                    session.automationMovePlayerTo(propPoint)
                    session.perform(PlayerCommand.Interact)
                }
                val secretRoom =
                    generatedFloor.roomByAnchor(entrance.targetAnchorId)
                        ?: generatedFloor.rooms.firstOrNull { room -> room.nodeId == entrance.targetNodeId }
                val rewardPoint = session.automationSecretRewardPointForBinding(bindingId)
                secretRewardNodePresent =
                    rewardPoint != null &&
                        session.renderSnapshot().props.any { prop ->
                            prop.propTypeId == "secret_reward" && Point(prop.x, prop.y) == rewardPoint
                        }
                secretRewardPathClass =
                    rewardPoint
                        ?.let { point -> secretRoom?.pathClass?.name ?: generatedFloor.roomAt(point)?.pathClass?.name ?: "UNKNOWN" }
                        ?: "UNKNOWN"
                rewardPoint?.let { point ->
                    session.automationMovePlayerTo(point)
                    session.perform(PlayerCommand.Interact)
                }
                session.automationSecretReturnPointForBinding(bindingId)?.let { point ->
                    session.automationMovePlayerTo(point)
                    session.perform(PlayerCommand.Interact)
                }
            }

            val hiddenEventIds = session.automationConsumedHiddenEventIds().sorted()
            val hiddenEvents = hiddenEventIds.mapNotNull(DataRegistryHolder::hiddenEvent)
            val triggerTypes = hiddenEvents.map(HiddenEventDef::triggerType).map(HiddenTriggerType::name).distinct().sorted()
            val triggerType =
                when (triggerTypes.size) {
                    0 -> "NONE"
                    1 -> triggerTypes.single()
                    else -> "MULTIPLE:${triggerTypes.joinToString(separator = ",")}"
                }
            val triggerPathClasses =
                hiddenEvents.map { hiddenEvent ->
                    triggerPathClass(
                        hiddenEvent = hiddenEvent,
                        generatedFloor = generatedFloor,
                        entranceRoom = entranceRoom,
                        secretRewardPathClass = secretRewardPathClass,
                        primerTriggerPathClass = primer.triggerPathClass,
                    )
                }
            val optionalOnlyTriggerPathClasses =
                hiddenEvents
                    .filter(HiddenEventDef::optionalOnly)
                    .map { hiddenEvent ->
                        triggerPathClass(
                            hiddenEvent = hiddenEvent,
                            generatedFloor = generatedFloor,
                            entranceRoom = entranceRoom,
                            secretRewardPathClass = secretRewardPathClass,
                            primerTriggerPathClass = primer.triggerPathClass,
                        )
                    }
            val secretZoneId = session.automationVisitedSecretZoneIds().firstOrNull()?.id
                ?.takeIf { candidate -> candidate == entrance.targetSecretZoneId.id }
            val proof = session.automationSolvabilityProof()
            val proofSearchActionResult = proof.searchStates.firstOrNull { entry -> entry.bindingId == entrance.bindingId }?.result?.name
            val finalSnapshot = session.renderSnapshot()
            val frontstageActionCues = finalSnapshot.uiState.frontstageReadability.recentActionCues
            val actualPlayerPoint = session.playerPosition()
            val finalRoom = generatedFloor.roomAt(actualPlayerPoint)
            val secretZoneEntered = secretZoneId != null
            val returnedToMainline = !secretZoneEntered || finalRoom?.pathClass != PathClass.SECRET
            val expectedReturnPoint = session.automationResolvedReturnPointForBinding(bindingId)
            val returnedRoomNodeId =
                when {
                    !secretZoneEntered -> finalRoom?.nodeId?.value
                    expectedReturnPoint == actualPlayerPoint -> entrance.resolvedReturnBridgeNodeId.value
                    else -> finalRoom?.nodeId?.value
                }
            val rewardBudgetSources =
                session.automationFloorRewardBudget().rewardDeltas
                    .map(com.ktome.core.loot.RewardDelta::source)
                    .distinct()
                    .sorted()
            val expectedRewardBudgetSources = DataRegistryHolder.expectedRewardBudgetSources(hiddenEvents = hiddenEvents, secretZoneId = secretZoneId)
            val threatBudgetSources = session.automationSecretEncounterThreatSources()
            val expectedThreatBudgetSources = DataRegistryHolder.expectedThreatBudgetSources(hiddenEvents)
            val criticalPathReachable = proof.criticalPathReachable
            val searchFailureKeepsMainlineReachable =
                searchResult != SearchActionResult.FAILED_CHECK ||
                    (criticalPathReachable && !secretZoneEntered && returnedRoomNodeId == entranceRoom.nodeId.value)
            HiddenContentCaseResult(
                zoneId = caseSpec.zoneId,
                floorIndex = caseSpec.floorIndex,
                seed = caseSpec.seed,
                searchBindingId = entrance.bindingId.value,
                primerActionId = caseSpec.primerActionId,
                primerActionUsed = caseSpec.primerActionId != HiddenPrimerAction.NONE.name,
                entranceBindingId = entrance.entranceAnchorId.value,
                resolvedReturnBridgeNodeId = entrance.resolvedReturnBridgeNodeId.value,
                searchActionResult = searchResult.name,
                explicitSearchReveal =
                    searchAccepted &&
                        searchStateBefore == null &&
                        searchResult == SearchActionResult.REVEALED,
                triggerType = triggerType,
                hiddenEventIds = hiddenEventIds,
                triggerTypes = triggerTypes,
                triggerPathClasses = triggerPathClasses,
                optionalOnlyTriggerPathClasses = optionalOnlyTriggerPathClasses,
                secretZoneId = secretZoneId,
                secretZoneEntered = secretZoneEntered,
                secretRewardNodePresent = secretZoneEntered && secretRewardNodePresent,
                criticalPathReachable = criticalPathReachable,
                searchFailureKeepsMainlineReachable = searchFailureKeepsMainlineReachable,
                returnedToMainline = returnedToMainline,
                returnedRoomNodeId = returnedRoomNodeId,
                returnedPoint = actualPlayerPoint.toDebugString(),
                expectedReturnPoint = expectedReturnPoint?.toDebugString(),
                returnBridgeMatchesResolvedNodeId = !secretZoneEntered || actualPlayerPoint == expectedReturnPoint,
                proofSearchActionResult = proofSearchActionResult,
                solvabilityProofMatchesSearchAction = proofSearchActionResult == searchResult.name,
                solvabilityProofCoversReturnBridge =
                    proof.visitedNodes.any { nodeId -> nodeId.value == entrance.resolvedReturnBridgeNodeId.value },
                rewardSources = finalSnapshot.uiState.recentRewards.map { reward -> reward.source.name },
                rewardBudgetSources = rewardBudgetSources,
                expectedRewardBudgetSources = expectedRewardBudgetSources,
                threatBudgetSources = threatBudgetSources,
                expectedThreatBudgetSources = expectedThreatBudgetSources,
                logKeys = finalSnapshot.logEvents.map { event -> event.message.key },
                frontstageActionCues =
                    frontstageActionCues.map { cue ->
                        HiddenFrontstageActionCueEvidence(
                            category = cue.category.name,
                            priority = cue.priority.name,
                            stableKey = cue.stableKey,
                            messageKey = cue.message.key,
                        )
                    },
            )
        } catch (exception: Exception) {
            HiddenContentCaseResult(
                zoneId = caseSpec.zoneId,
                floorIndex = caseSpec.floorIndex,
                seed = caseSpec.seed,
                searchBindingId = caseSpec.searchBindingId,
                primerActionId = caseSpec.primerActionId,
                primerActionUsed = caseSpec.primerActionId != HiddenPrimerAction.NONE.name,
                entranceBindingId = "",
                resolvedReturnBridgeNodeId = "",
                searchActionResult = SearchActionResult.NO_TARGET.name,
                explicitSearchReveal = false,
                triggerType = "NONE",
                hiddenEventIds = emptyList(),
                triggerTypes = emptyList(),
                triggerPathClasses = emptyList(),
                optionalOnlyTriggerPathClasses = emptyList(),
                secretZoneId = null,
                secretZoneEntered = false,
                secretRewardNodePresent = false,
                criticalPathReachable = false,
                searchFailureKeepsMainlineReachable = false,
                returnedToMainline = false,
                returnedRoomNodeId = null,
                returnedPoint = null,
                expectedReturnPoint = null,
                returnBridgeMatchesResolvedNodeId = false,
                proofSearchActionResult = null,
                solvabilityProofMatchesSearchAction = false,
                solvabilityProofCoversReturnBridge = false,
                rewardSources = emptyList(),
                rewardBudgetSources = emptyList(),
                expectedRewardBudgetSources = emptyList(),
                threatBudgetSources = emptyList(),
                expectedThreatBudgetSources = emptyList(),
                logKeys = emptyList(),
                frontstageActionCues = emptyList(),
                failure = exception.message ?: exception::class.simpleName.orEmpty(),
            )
        }

    private fun primeScenario(
        session: com.ktome.game.FoundationGameSession,
        caseSpec: HiddenContentCaseSpec,
        generatedFloor: com.ktome.core.mapgen.GeneratedFloor,
    ): HiddenPrimerExecution {
        val scenario =
            requireNotNull(
                hiddenBindingScenarios.firstOrNull { candidate ->
                    candidate.zoneId == caseSpec.zoneId && candidate.searchBindingId.value == caseSpec.searchBindingId
                },
            ) {
                "Unsupported hidden-content binding scenario '${caseSpec.zoneId}:${caseSpec.searchBindingId}'."
            }
        return when (scenario.primerAction) {
            HiddenPrimerAction.NONE -> HiddenPrimerExecution()
            HiddenPrimerAction.FORCE_ELITE_KILL -> {
                require(session.automationKillFirstExistingEliteMonster()) {
                    "No existing elite monster available for '${caseSpec.searchBindingId}'."
                }
                HiddenPrimerExecution(triggerPathClass = PathClass.CRITICAL_PATH.name)
            }
            HiddenPrimerAction.OPEN_CRYSTAL_CACHE_CHEST -> {
                val point = requireNotNull(session.automationInteractablePoint(CRYSTAL_CACHE_CHEST_ID)) {
                    "Missing interactable '$CRYSTAL_CACHE_CHEST_ID'."
                }
                session.automationMovePlayerTo(point)
                require(session.perform(PlayerCommand.Interact)) {
                    "Failed to open interactable '$CRYSTAL_CACHE_CHEST_ID'."
                }
                HiddenPrimerExecution(
                    triggerPathClass = generatedFloor.roomAt(point)?.pathClass?.name ?: "UNKNOWN",
                )
            }
            HiddenPrimerAction.CLAIM_WARD_RELIQUARY -> {
                val point = requireNotNull(session.automationInteractablePoint(TEMPLE_WARD_RELIQUARY_ID)) {
                    "Missing interactable '$TEMPLE_WARD_RELIQUARY_ID'."
                }
                session.automationMovePlayerTo(point)
                require(session.perform(PlayerCommand.Interact)) {
                    "Failed to claim interactable '$TEMPLE_WARD_RELIQUARY_ID'."
                }
                session.perform(PlayerCommand.CloseShop)
                HiddenPrimerExecution(
                    triggerPathClass = generatedFloor.roomAt(point)?.pathClass?.name ?: "UNKNOWN",
                )
            }
            HiddenPrimerAction.ENTER_HIDDEN_BRANCH_ROOM -> {
                val point =
                    requireNotNull(generatedFloor.roomByAnchor(NodeAnchorId("hidden.branch"))) {
                        "Missing hidden.branch room for '${caseSpec.searchBindingId}'."
                    }.center
                session.automationMovePlayerTo(point)
                HiddenPrimerExecution(
                    triggerPathClass = generatedFloor.roomAt(point)?.pathClass?.name ?: "UNKNOWN",
                )
            }
        }
    }

    private fun triggerPathClass(
        hiddenEvent: HiddenEventDef,
        generatedFloor: com.ktome.core.mapgen.GeneratedFloor,
        entranceRoom: com.ktome.core.mapgen.RoomInstance,
        secretRewardPathClass: String,
        primerTriggerPathClass: String?,
    ): String =
        when (hiddenEvent.triggerType) {
            HiddenTriggerType.PERCEPTION_REVEAL -> entranceRoom.pathClass.name
            HiddenTriggerType.INTERACT_TILE -> secretRewardPathClass
            HiddenTriggerType.ENTER_ROOM ->
                roomPathClassForHiddenEvent(generatedFloor = generatedFloor, hiddenEvent = hiddenEvent)
                    ?: primerTriggerPathClass
                    ?: "UNKNOWN"
            HiddenTriggerType.KILL_ELITE,
            HiddenTriggerType.OPEN_CHEST,
            HiddenTriggerType.QUEST_STEP,
            -> primerTriggerPathClass ?: "UNKNOWN"
        }

    private fun roomPathClassForHiddenEvent(
        generatedFloor: com.ktome.core.mapgen.GeneratedFloor,
        hiddenEvent: HiddenEventDef,
    ): String? {
        val requiredRoomTags =
            hiddenEvent.conditions
                .filter { condition -> condition.key == HiddenConditionKey.ROOM_TAG }
                .map { condition -> condition.expectedValue }
                .toSet()
        if (requiredRoomTags.isEmpty()) {
            return null
        }
        return generatedFloor.rooms
            .firstOrNull { room -> requiredRoomTags.all(room.tags::contains) }
            ?.pathClass
            ?.name
    }

    private object DataRegistryHolder {
        private val hiddenEventsById: Map<String, HiddenEventDef> by lazy {
            val loader = com.ktome.game.data.DataLoader(GameLocale.EN_US)
            val catalog = loader.loadSchemaCatalog()
            catalog.hiddenEvents.associateBy(HiddenEventDef::id)
        }

        private val secretZonesById: Map<String, SecretZoneDef> by lazy {
            val loader = com.ktome.game.data.DataLoader(GameLocale.EN_US)
            val catalog = loader.loadSchemaCatalog()
            catalog.secretZones.associateBy { secretZone -> secretZone.id.id }
        }

        fun hiddenEvent(hiddenEventId: String): HiddenEventDef? = hiddenEventsById[hiddenEventId]

        fun expectedRewardBudgetSources(
            hiddenEvents: List<HiddenEventDef>,
            secretZoneId: String?,
        ): List<String> =
            buildSet {
                hiddenEvents
                    .filter { hiddenEvent -> hiddenEvent.rewards.any { reward -> reward.key == HiddenEventRewardKey.LOOT_PROFILE } }
                    .forEach { hiddenEvent -> add("hiddenEvent:${hiddenEvent.id}") }
                secretZoneId
                    ?.let(secretZonesById::get)
                    ?.let { secretZone -> add("secretZone:${secretZone.id.id}") }
            }.toList().sorted()

        fun expectedThreatBudgetSources(hiddenEvents: List<HiddenEventDef>): List<String> =
            hiddenEvents
                .flatMap { hiddenEvent ->
                    hiddenEvent.rewards.mapNotNull { reward ->
                        when (val payload = reward.payload) {
                            is HiddenEventRewardPayload.TriggerEncounter -> "secretEncounter:${payload.encounterRef.id}"
                            else -> null
                        }
                    }
                }.distinct()
                .sorted()
    }

    private fun Point.toDebugString(): String = "$x,$y"
}

internal object HiddenContentHarnessAnalysis {
    fun analyze(results: List<HiddenContentCaseResult>): HiddenContentAnalysis {
        val registryMetrics = HiddenContentRegistrySnapshot.load()
        val zoneBreakdown =
            results.groupBy(HiddenContentCaseResult::zoneId)
                .mapValues { (_, zoneResults) ->
                    HiddenContentZoneMetrics(
                        caseCount = zoneResults.size,
                        hiddenEventTriggerCount = zoneResults.count { result -> result.hiddenEventIds.isNotEmpty() },
                        secretZoneDiscoveryCount = zoneResults.count(HiddenContentCaseResult::secretZoneEntered),
                    )
                }
        val hiddenEventTriggerCount = results.count { result -> result.hiddenEventIds.isNotEmpty() }
        val secretZoneDiscoveryCount = results.count(HiddenContentCaseResult::secretZoneEntered)
        val aggregateFailures =
            buildList {
                if (hiddenEventTriggerCount.toDouble() / results.size.toDouble() < MIN_HIDDEN_EVENT_TRIGGER_RATE) {
                    add("aggregate.hidden_event_trigger_rate_below_threshold")
                }
                if (secretZoneDiscoveryCount.toDouble() / results.size.toDouble() < MIN_SECRET_ZONE_DISCOVERY_RATE) {
                    add("aggregate.secret_zone_discovery_rate_below_threshold")
                }
                if (zoneBreakdown.values.any { metrics -> metrics.hiddenEventTriggerCount == 0 }) {
                    add("aggregate.zone_hidden_event_coverage_failed")
                }
                if (zoneBreakdown.values.any { metrics -> metrics.secretZoneDiscoveryCount == 0 }) {
                    add("aggregate.zone_secret_zone_coverage_failed")
                }
                if (results.any { result -> !result.criticalPathReachable }) {
                    add("aggregate.hidden_content_blocks_mainline")
                }
                if (results.any { result -> result.hiddenEventIds.isNotEmpty() && !result.optionalOnlyTriggerPathClassesWithinOptionalOrSecret }) {
                    add("aggregate.optional_only_hidden_event_outside_optional_or_secret")
                }
                if (results.any { result -> result.secretZoneEntered && !result.secretRewardNodePresent }) {
                    add("aggregate.secret_reward_node_missing")
                }
                if (results.any { result -> !result.rewardBridgeBackedByLootBudget }) {
                    add("aggregate.reward_bridge_outside_loot_budget")
                }
                if (results.any { result -> !result.encounterBridgeBackedByThreatBudget }) {
                    add("aggregate.secret_encounter_outside_threat_budget")
                }
                if (results.any { result -> result.searchActionResult == SearchActionResult.FAILED_CHECK.name && !result.searchFailureKeepsMainlineReachable }) {
                    add("aggregate.search_failure_blocks_mainline")
                }
                if (
                    results.any { result ->
                        !result.solvabilityProofMatchesSearchAction ||
                            !result.solvabilityProofCoversReturnBridge
                    }
                ) {
                    add("aggregate.return_bridge_or_proof_mismatch")
                }
                if (registryMetrics.hiddenEventRegistryCount < MIN_HIDDEN_EVENT_REGISTRY_COUNT) {
                    add("aggregate.hidden_event_registry_count_below_threshold")
                }
                if (registryMetrics.hiddenTriggerTypeCoverage < MIN_HIDDEN_TRIGGER_TYPE_COVERAGE) {
                    add("aggregate.hidden_trigger_type_coverage_failed")
                }
                if (registryMetrics.secretEntranceBindingCoverage < MIN_SECRET_ENTRANCE_BINDING_COVERAGE) {
                    add("aggregate.secret_entrance_binding_coverage_failed")
                }
            }
        val summary =
            HiddenContentSummaryMetrics(
                totalCases = results.size,
                distinctSeedCount = results.map(HiddenContentCaseResult::seed).distinct().size,
                caseFailureCount = results.count { result -> result.gateFailureReasons().isNotEmpty() },
                aggregateFailureCount = aggregateFailures.size,
                hiddenEventTriggerCount = hiddenEventTriggerCount,
                hiddenEventTriggerRate = hiddenEventTriggerCount.toDouble() / results.size.toDouble(),
                secretZoneDiscoveryCount = secretZoneDiscoveryCount,
                secretZoneDiscoveryRate = secretZoneDiscoveryCount.toDouble() / results.size.toDouble(),
                explicitSearchRevealCount = results.count(HiddenContentCaseResult::explicitSearchReveal),
                primerActionUsedCount = results.count(HiddenContentCaseResult::primerActionUsed),
                primerFreeCaseCount = results.count { result -> !result.primerActionUsed },
                searchFailureCount = results.count { result -> result.searchActionResult == SearchActionResult.FAILED_CHECK.name },
                zeroHiddenEventZoneCount = zoneBreakdown.values.count { metrics -> metrics.hiddenEventTriggerCount == 0 },
                zeroSecretZoneZoneCount = zoneBreakdown.values.count { metrics -> metrics.secretZoneDiscoveryCount == 0 },
                criticalPathFailureCount = results.count { result -> !result.criticalPathReachable },
                triggerContextFailureCount =
                    results.count { result ->
                        result.hiddenEventIds.isNotEmpty() && !result.optionalOnlyTriggerPathClassesWithinOptionalOrSecret
                    },
                secretRewardNodeMissingCount = results.count { result -> result.secretZoneEntered && !result.secretRewardNodePresent },
                rewardBudgetFailureCount = results.count { result -> !result.rewardBridgeBackedByLootBudget },
                threatBudgetFailureCount = results.count { result -> !result.encounterBridgeBackedByThreatBudget },
                searchFailureBlockingCount =
                    results.count { result ->
                        result.searchActionResult == SearchActionResult.FAILED_CHECK.name && !result.searchFailureKeepsMainlineReachable
                    },
                proofMismatchCount =
                    results.count { result ->
                        !result.solvabilityProofMatchesSearchAction ||
                            !result.solvabilityProofCoversReturnBridge
                    },
                runtimeReturnDestinationMismatchCount =
                    results.count { result ->
                        result.secretZoneEntered && !result.returnBridgeMatchesResolvedNodeId
                    },
                hiddenEventRegistryCount = registryMetrics.hiddenEventRegistryCount,
                secretZoneRegistryCount = registryMetrics.secretZoneRegistryCount,
                hiddenTriggerTypeCoverage = registryMetrics.hiddenTriggerTypeCoverage,
                hiddenTriggerTypeSet = registryMetrics.hiddenTriggerTypeSet,
                secretEntranceBindingCoverage = registryMetrics.secretEntranceBindingCoverage,
                secretEntranceBindingSet = registryMetrics.secretEntranceBindingSet,
            )
        return HiddenContentAnalysis(summary = summary, zoneBreakdown = zoneBreakdown, aggregateFailures = aggregateFailures)
    }
}

object HiddenContentHarnessRunner {
    const val HARNESS_ID: String = "hiddenContentHarness"
    private const val SUMMARY_FILE: String = "hidden-content-summary.json"
    private const val EVENTS_FILE: String = "hidden-content-events.jsonl"
    private val json: Json = Json { prettyPrint = true }

    fun run(): HiddenContentHarnessRun {
        val kernelRun = HiddenContentHarnessKernel.execute()
        val analysis = HiddenContentHarnessAnalysis.analyze(kernelRun.results)
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val frontstageMetrics = frontstageCueContractMetrics(results = kernelRun.results, outputDir = outputDir)
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        val eventsPath = outputDir.resolve(EVENTS_FILE)
        Files.writeString(
            summaryPath,
            json.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(kernelRun = kernelRun, analysis = analysis, frontstageMetrics = frontstageMetrics),
            ),
        )
        Files.writeString(
            eventsPath,
            kernelRun.results.joinToString(separator = "\n") { result ->
                Json.encodeToString(JsonElement.serializer(), result.toJson(kernelRun.header))
            } + "\n",
        )
        return HiddenContentHarnessRun(
            totalCases = kernelRun.results.size,
            failureCount = analysis.summary.failureCount,
            summaryPath = summaryPath,
            eventsPath = eventsPath,
        )
    }

    internal fun loadKernelRun(reportDir: Path = reportDir()): HiddenContentKernelRun? {
        val summaryPath = reportDir.resolve(SUMMARY_FILE)
        val eventsPath = reportDir.resolve(EVENTS_FILE)
        if (!Files.isRegularFile(summaryPath) || !Files.isRegularFile(eventsPath)) {
            return null
        }
        val payload = json.parseToJsonElement(Files.readString(summaryPath)).jsonObject
        val header = payload.getValue("header").jsonObject.toHarnessReportHeader()
        val results =
            Files.readAllLines(eventsPath)
                .asSequence()
                .filter(String::isNotBlank)
                .map { line -> json.parseToJsonElement(line).jsonObject.toHiddenContentCaseResult() }
                .toList()
        return HiddenContentKernelRun(
            header = header,
            results = results,
        )
    }

    private fun buildSummaryPayload(
        kernelRun: HiddenContentKernelRun,
        analysis: HiddenContentAnalysis,
        frontstageMetrics: HiddenFrontstageCueContractMetrics,
    ): JsonObject {
        val summary = analysis.summary
        return buildJsonObject {
            put("header", kernelRun.header.toJson())
            putJsonObject("summary") {
                put("scriptedVerification", true)
                put("totalCases", summary.totalCases)
                put("distinctSeedCount", summary.distinctSeedCount)
                put("failureCount", summary.failureCount)
                put("caseFailureCount", summary.caseFailureCount)
                put("aggregateFailureCount", summary.aggregateFailureCount)
                put("hiddenEventTriggerCount", summary.hiddenEventTriggerCount)
                put("hiddenEventTriggerRate", summary.hiddenEventTriggerRate)
                put("secretZoneDiscoveryCount", summary.secretZoneDiscoveryCount)
                put("secretZoneDiscoveryRate", summary.secretZoneDiscoveryRate)
                put("explicitSearchRevealCount", summary.explicitSearchRevealCount)
                put("primerActionUsedCount", summary.primerActionUsedCount)
                put("primerFreeCaseCount", summary.primerFreeCaseCount)
                put("searchFailureCount", summary.searchFailureCount)
                put("zeroTriggerZoneCount", summary.zeroHiddenEventZoneCount)
                put("zeroHiddenEventZoneCount", summary.zeroHiddenEventZoneCount)
                put("zeroSecretZoneZoneCount", summary.zeroSecretZoneZoneCount)
                put("criticalPathFailureCount", summary.criticalPathFailureCount)
                put("triggerContextFailureCount", summary.triggerContextFailureCount)
                put("secretRewardNodeMissingCount", summary.secretRewardNodeMissingCount)
                put("rewardBudgetFailureCount", summary.rewardBudgetFailureCount)
                put("threatBudgetFailureCount", summary.threatBudgetFailureCount)
                put("searchFailureBlockingCount", summary.searchFailureBlockingCount)
                put("proofMismatchCount", summary.proofMismatchCount)
                put("runtimeReturnDestinationMismatchCount", summary.runtimeReturnDestinationMismatchCount)
                put("hiddenEventRegistryCount", summary.hiddenEventRegistryCount)
                put("secretZoneRegistryCount", summary.secretZoneRegistryCount)
                put("hiddenTriggerTypeCoverage", summary.hiddenTriggerTypeCoverage)
                putJsonArray("hiddenTriggerTypeSet") {
                    summary.hiddenTriggerTypeSet.sorted().forEach { triggerType -> add(JsonPrimitive(triggerType)) }
                }
                put("secretEntranceBindingCoverage", summary.secretEntranceBindingCoverage)
                putJsonArray("secretEntranceBindingSet") {
                    summary.secretEntranceBindingSet.sorted().forEach { bindingId -> add(JsonPrimitive(bindingId)) }
                }
                put("frontstageHighPriorityCueRetainedRate", frontstageMetrics.highPriorityCueRetainedRate)
                put("frontstageCueDedupAppliedCount", frontstageMetrics.dedupAppliedCount)
                put("frontstageCueExpiryParity", frontstageMetrics.expiryParity)
                put("frontstageSecretCueVisibilityRate", frontstageMetrics.secretCueVisibilityRate)
                put("frontstageHighPriorityCueRetainedCount", frontstageMetrics.highPriorityCueRetainedCount)
                put("frontstageHighPriorityExpectedCount", frontstageMetrics.highPriorityExpectedCount)
                put("frontstageSecretCueVisibleCount", frontstageMetrics.secretCueVisibleCount)
                put("frontstageSecretCueExpectedCount", frontstageMetrics.secretCueExpectedCount)
                put("frontstageDuplicateNoTargetLogCount", frontstageMetrics.duplicateNoTargetLogCount)
                put("frontstageRemainingNoTargetCueCount", frontstageMetrics.remainingNoTargetCueCount)
                put("frontstageCueExpiryProbePassedCount", frontstageMetrics.expiryProbePassedCount)
                put("frontstageCueExpiryProbeTotalCount", frontstageMetrics.expiryProbeTotalCount)
                putJsonArray("frontstageCueExpiryProbePriorities") {
                    frontstageMetrics.expiryProbePriorities.forEach { priority -> add(JsonPrimitive(priority)) }
                }
            }
            putJsonArray("aggregateFailures") {
                analysis.aggregateFailures.forEach { failure -> add(JsonPrimitive(failure)) }
            }
            putJsonObject("zones") {
                analysis.zoneBreakdown.toSortedMap().forEach { (zoneId, metrics) ->
                    putJsonObject(zoneId) {
                        put("caseCount", metrics.caseCount)
                        put("hiddenEventTriggerCount", metrics.hiddenEventTriggerCount)
                        put("hiddenEventTriggerRate", metrics.hiddenEventTriggerRate)
                        put("secretZoneDiscoveryCount", metrics.secretZoneDiscoveryCount)
                        put("secretZoneDiscoveryRate", metrics.secretZoneDiscoveryRate)
                    }
                }
            }
        }
    }
}

internal fun reportDir(): Path {
    val configured = System.getProperty("ktome.phase4.hidden.reportDir")
    return if (configured.isNullOrBlank()) {
        Path.of("tools", "build", "reports", "phase4", "hidden")
    } else {
        Path.of(configured)
    }
}

internal data class HiddenFrontstageCueContractMetrics(
    val highPriorityCueRetainedRate: Double,
    val dedupAppliedCount: Int,
    val expiryParity: Double,
    val secretCueVisibilityRate: Double,
    val highPriorityCueRetainedCount: Int,
    val highPriorityExpectedCount: Int,
    val secretCueVisibleCount: Int,
    val secretCueExpectedCount: Int,
    val duplicateNoTargetLogCount: Int,
    val remainingNoTargetCueCount: Int,
    val expiryProbePassedCount: Int,
    val expiryProbeTotalCount: Int,
    val expiryProbePriorities: List<String>,
)

internal fun frontstageCueContractMetrics(
    results: List<HiddenContentCaseResult>,
    outputDir: Path,
): HiddenFrontstageCueContractMetrics {
    var highPriorityExpectedCount = 0
    var highPriorityRetainedCount = 0
    var secretCueExpectedCount = 0
    var secretCueVisibleCount = 0
    results.forEach { result ->
        val actualCues = result.observedFrontstageCues()
        val secretExpectations = expectedSecretFrontstageCues(result)
        val highPriorityExpectations = expectedHighPriorityFrontstageCues(result, secretExpectations)
        if (highPriorityExpectations.isNotEmpty()) {
            highPriorityExpectedCount += 1
            if (highPriorityExpectations.any { expectation -> actualCues.any(expectation::matches) }) {
                highPriorityRetainedCount += 1
            }
        }
        if (secretExpectations.isNotEmpty()) {
            secretCueExpectedCount += 1
            if (secretExpectations.any { expectation -> actualCues.any(expectation::matches) }) {
                secretCueVisibleCount += 1
            }
        }
    }
    val probe = runFrontstageCueContractProbe(outputDir)
    return HiddenFrontstageCueContractMetrics(
        highPriorityCueRetainedRate = hiddenRatio(highPriorityRetainedCount, highPriorityExpectedCount),
        dedupAppliedCount = probe.dedupAppliedCount,
        expiryParity = probe.expiryParity,
        secretCueVisibilityRate = hiddenRatio(secretCueVisibleCount, secretCueExpectedCount),
        highPriorityCueRetainedCount = highPriorityRetainedCount,
        highPriorityExpectedCount = highPriorityExpectedCount,
        secretCueVisibleCount = secretCueVisibleCount,
        secretCueExpectedCount = secretCueExpectedCount,
        duplicateNoTargetLogCount = probe.duplicateNoTargetLogCount,
        remainingNoTargetCueCount = probe.remainingNoTargetCueCount,
        expiryProbePassedCount = probe.expiryProbePassedCount,
        expiryProbeTotalCount = probe.expiryProbeTotalCount,
        expiryProbePriorities = probe.expiryProbePriorities,
    )
}

private data class FrontstageCueContractProbeResult(
    val dedupAppliedCount: Int,
    val expiryParity: Double,
    val duplicateNoTargetLogCount: Int,
    val remainingNoTargetCueCount: Int,
    val expiryProbePassedCount: Int,
    val expiryProbeTotalCount: Int,
    val expiryProbePriorities: List<String>,
)

private fun runFrontstageCueContractProbe(outputDir: Path): FrontstageCueContractProbeResult {
    val session =
        GameModule.newFoundationSession(
            config = FoundationGameConfig(seed = 20260416L, zoneId = "greenwood_fringe", playerProfessionId = "rogue"),
            saveManager = com.ktome.core.save.SaveManager(outputDir.resolve("tmp").resolve("frontstage-cue-probe")),
            locale = GameLocale.EN_US,
        )
    session.automationRecordFrontstageActionCueForVerification(
        category = FrontstageActionCategorySnapshot.SEARCH,
        priority = FrontstageActionPrioritySnapshot.MEDIUM,
        stableKey = "search:no_target",
        messageKey = "log.search.no_target",
    )
    session.automationRecordFrontstageActionCueForVerification(
        category = FrontstageActionCategorySnapshot.SEARCH,
        priority = FrontstageActionPrioritySnapshot.MEDIUM,
        stableKey = "search:no_target",
        messageKey = "log.search.no_target",
    )
    val duplicateNoTargetLogCount = session.renderSnapshot().logEvents.count { event -> event.message.key == "log.search.no_target" }
    val dedupedCueCount =
        session.renderSnapshot().uiState.frontstageReadability.recentActionCues.count { cue ->
            cue.stableKey == "search:no_target"
        }
    repeat(3) {
        session.perform(PlayerCommand.Wait)
    }
    val remainingNoTargetCueCount =
        session.renderSnapshot().uiState.frontstageReadability.recentActionCues.count { cue ->
            cue.stableKey == "search:no_target"
        }
    val ttlProbeResults =
        FrontstageActionPrioritySnapshot.entries.map { priority ->
            runFrontstageTtlProbe(outputDir = outputDir, priority = priority)
        }
    val expiryProbePassedCount = ttlProbeResults.count(FrontstageTtlProbeResult::passed)
    return FrontstageCueContractProbeResult(
        dedupAppliedCount = if (duplicateNoTargetLogCount >= 2 && dedupedCueCount == 1) 1 else 0,
        expiryParity = hiddenRatio(expiryProbePassedCount, ttlProbeResults.size),
        duplicateNoTargetLogCount = duplicateNoTargetLogCount,
        remainingNoTargetCueCount = remainingNoTargetCueCount,
        expiryProbePassedCount = expiryProbePassedCount,
        expiryProbeTotalCount = ttlProbeResults.size,
        expiryProbePriorities = ttlProbeResults.map(FrontstageTtlProbeResult::priority),
    )
}

private data class FrontstageTtlProbeResult(
    val priority: String,
    val passed: Boolean,
)

private fun runFrontstageTtlProbe(
    outputDir: Path,
    priority: FrontstageActionPrioritySnapshot,
): FrontstageTtlProbeResult {
    val session =
        GameModule.newFoundationSession(
            config = FoundationGameConfig(seed = 20260416L, zoneId = "greenwood_fringe", playerProfessionId = "rogue"),
            saveManager = com.ktome.core.save.SaveManager(outputDir.resolve("tmp").resolve("frontstage-ttl-${priority.name.lowercase()}")),
            locale = GameLocale.EN_US,
        )
    val stableKey = "ttl:${priority.name.lowercase()}"
    session.automationRecordFrontstageActionCueForVerification(
        category = frontstageProbeCategory(priority),
        priority = priority,
        stableKey = stableKey,
        messageKey = frontstageProbeMessageKey(priority),
    )
    val visibleImmediately = session.hasFrontstageCue(stableKey)
    repeat(frontstageProbeTtlTurns(priority)) {
        session.perform(PlayerCommand.Wait)
    }
    val retainedThroughTtl = session.hasFrontstageCue(stableKey)
    session.perform(PlayerCommand.Wait)
    val expiredAfterBoundary = !session.hasFrontstageCue(stableKey)
    return FrontstageTtlProbeResult(
        priority = priority.name,
        passed = visibleImmediately && retainedThroughTtl && expiredAfterBoundary,
    )
}

private fun FoundationGameSession.hasFrontstageCue(stableKey: String): Boolean =
    renderSnapshot().uiState.frontstageReadability.recentActionCues.any { cue -> cue.stableKey == stableKey }

private fun frontstageProbeCategory(priority: FrontstageActionPrioritySnapshot): FrontstageActionCategorySnapshot =
    when (priority) {
        FrontstageActionPrioritySnapshot.CRITICAL -> FrontstageActionCategorySnapshot.SECRET
        FrontstageActionPrioritySnapshot.HIGH -> FrontstageActionCategorySnapshot.SEARCH
        FrontstageActionPrioritySnapshot.MEDIUM -> FrontstageActionCategorySnapshot.SEARCH
        FrontstageActionPrioritySnapshot.LOW -> FrontstageActionCategorySnapshot.PASSIVE
    }

private fun frontstageProbeMessageKey(priority: FrontstageActionPrioritySnapshot): String =
    when (priority) {
        FrontstageActionPrioritySnapshot.CRITICAL -> "log.hidden.secret_zone.enter"
        FrontstageActionPrioritySnapshot.HIGH -> "log.search.failed_tag"
        FrontstageActionPrioritySnapshot.MEDIUM -> "log.search.no_target"
        FrontstageActionPrioritySnapshot.LOW -> "log.passive.hp_regen"
    }

private fun frontstageProbeTtlTurns(priority: FrontstageActionPrioritySnapshot): Int =
    when (priority) {
        FrontstageActionPrioritySnapshot.CRITICAL,
        FrontstageActionPrioritySnapshot.HIGH,
        -> 3

        FrontstageActionPrioritySnapshot.MEDIUM -> 2
        FrontstageActionPrioritySnapshot.LOW -> 1
    }

private data class ObservedFrontstageCue(
    val category: String,
    val priority: String,
    val stableKey: String,
    val messageKey: String,
)

private data class ExpectedFrontstageCue(
    val category: String,
    val priority: String,
    val stableKey: String? = null,
    val stableKeyPrefix: String? = null,
    val messageKeys: Set<String>,
) {
    fun matches(actual: ObservedFrontstageCue): Boolean {
        val stableKeyMatches =
            when {
                stableKey != null -> actual.stableKey == stableKey
                stableKeyPrefix != null -> actual.stableKey.startsWith(stableKeyPrefix)
                else -> true
            }
        return actual.category == category &&
            actual.priority == priority &&
            actual.messageKey in messageKeys &&
            stableKeyMatches
    }
}

private fun HiddenContentCaseResult.observedFrontstageCues(): List<ObservedFrontstageCue> =
    frontstageActionCues.map { cue ->
        ObservedFrontstageCue(
            category = cue.category,
            priority = cue.priority,
            stableKey = cue.stableKey,
            messageKey = cue.messageKey,
        )
    }

private fun expectedHighPriorityFrontstageCues(
    result: HiddenContentCaseResult,
    secretExpectations: List<ExpectedFrontstageCue> = expectedSecretFrontstageCues(result),
): List<ExpectedFrontstageCue> =
    buildList {
        if (result.explicitSearchReveal && result.searchActionResult == SearchActionResult.REVEALED.name) {
            add(
                ExpectedFrontstageCue(
                    category = FrontstageActionCategorySnapshot.SEARCH.name,
                    priority = FrontstageActionPrioritySnapshot.CRITICAL.name,
                    stableKey = "search:${result.searchBindingId}:revealed",
                    messageKeys = setOf("log.search.revealed", "log.search.revealed_tag"),
                ),
            )
        }
        if (result.searchActionResult == SearchActionResult.FAILED_CHECK.name) {
            add(
                ExpectedFrontstageCue(
                    category = FrontstageActionCategorySnapshot.SEARCH.name,
                    priority = FrontstageActionPrioritySnapshot.HIGH.name,
                    stableKey = "search:${result.searchBindingId}:failed_check",
                    messageKeys = setOf("log.search.failed_check", "log.search.failed_tag"),
                ),
            )
        }
        addAll(secretExpectations)
    }

private fun expectedSecretFrontstageCues(result: HiddenContentCaseResult): List<ExpectedFrontstageCue> =
    buildList {
        val secretZoneId = result.secretZoneId
        if (secretZoneId != null && "log.hidden.secret_zone.revealed" in result.logKeys) {
            add(
                ExpectedFrontstageCue(
                    category = FrontstageActionCategorySnapshot.SECRET.name,
                    priority = FrontstageActionPrioritySnapshot.CRITICAL.name,
                    stableKey = "secret:reveal:$secretZoneId",
                    messageKeys = setOf("log.hidden.secret_zone.revealed"),
                ),
            )
        }
        if (secretZoneId != null && result.secretZoneEntered) {
            add(
                ExpectedFrontstageCue(
                    category = FrontstageActionCategorySnapshot.SECRET.name,
                    priority = FrontstageActionPrioritySnapshot.CRITICAL.name,
                    stableKey = "secret:enter:$secretZoneId",
                    messageKeys = setOf("log.hidden.secret_zone.enter"),
                ),
            )
        }
        if (secretZoneId != null && result.logKeys.any { key -> key == "log.hidden.reward.claimed" || key == "log.hidden.reward.dropped" }) {
            add(
                ExpectedFrontstageCue(
                    category = FrontstageActionCategorySnapshot.SECRET.name,
                    priority = FrontstageActionPrioritySnapshot.CRITICAL.name,
                    stableKey = "secret:reward:$secretZoneId",
                    messageKeys = setOf("log.hidden.reward.claimed", "log.hidden.reward.dropped"),
                ),
            )
        }
        if ("log.hidden.reward.buff" in result.logKeys) {
            add(
                ExpectedFrontstageCue(
                    category = FrontstageActionCategorySnapshot.SECRET.name,
                    priority = FrontstageActionPrioritySnapshot.HIGH.name,
                    stableKeyPrefix = "secret:buff:${secretZoneId ?: ""}",
                    messageKeys = setOf("log.hidden.reward.buff"),
                ),
            )
        }
        if ("log.hidden.reward.encounter" in result.logKeys) {
            add(
                ExpectedFrontstageCue(
                    category = FrontstageActionCategorySnapshot.SECRET.name,
                    priority = FrontstageActionPrioritySnapshot.CRITICAL.name,
                    stableKeyPrefix = "secret:encounter:${secretZoneId ?: "hidden-event"}:",
                    messageKeys = setOf("log.hidden.reward.encounter"),
                ),
            )
        }
        if ("log.hidden.primer.acquired" in result.logKeys) {
            add(
                ExpectedFrontstageCue(
                    category = FrontstageActionCategorySnapshot.SECRET.name,
                    priority = FrontstageActionPrioritySnapshot.HIGH.name,
                    stableKeyPrefix = "secret:primer:",
                    messageKeys = setOf("log.hidden.primer.acquired"),
                ),
            )
        }
    }

private fun hiddenRatio(
    numerator: Int,
    denominator: Int,
): Double =
    if (denominator == 0) {
        0.0
    } else {
        numerator.toDouble() / denominator.toDouble()
    }

private fun JsonObject.toHarnessReportHeader(): HarnessReportHeader =
    HarnessReportHeader(
        harnessId = stringValue("harnessId"),
        phaseId = stringValue("phaseId"),
        buildId = stringValue("buildId"),
        locale = stringValue("locale"),
        contentSchemaVersion = intValue("contentSchemaVersion"),
        topologyFingerprintVersion = intValue("topologyFingerprintVersion"),
        rewardLedgerVersion = intValue("rewardLedgerVersion"),
        lootFormulaVersion = intValue("lootFormulaVersion"),
        specialTierEligibilityVersion = intValue("specialTierEligibilityVersion"),
        searchRuleVersion = intValue("searchRuleVersion"),
        secretRuleVersion = intValue("secretRuleVersion"),
        overlayContractVersion = intValue("overlayContractVersion"),
        activePackIds = getValue("activePackIds").jsonArray.map { packId -> PackId(packId.jsonPrimitive.content) },
        activePackManifestVersions =
            getValue("activePackManifestVersions").jsonObject.entries.associate { (packId, version) ->
                PackId(packId) to version.jsonPrimitive.content
            },
        timestamp = stringValue("timestamp"),
        seedList = getValue("seedList").jsonArray.map { seed -> seed.jsonPrimitive.content.toLong() },
    )

private fun JsonObject.toHiddenContentCaseResult(): HiddenContentCaseResult =
    HiddenContentCaseResult(
        zoneId = stringValue("zoneId"),
        floorIndex = intValue("floorIndex"),
        seed = longValue("seed"),
        searchBindingId = stringValue("searchBindingId"),
        primerActionId = stringValue("primerActionId"),
        primerActionUsed = booleanValue("primerActionUsed"),
        entranceBindingId = stringValue("entranceBindingId"),
        resolvedReturnBridgeNodeId = stringValue("resolvedReturnBridgeNodeId"),
        searchActionResult = stringValue("searchActionResult"),
        explicitSearchReveal = booleanValue("explicitSearchReveal"),
        triggerType = stringValue("triggerType"),
        hiddenEventIds = stringList("hiddenEventIds"),
        triggerTypes = stringList("triggerTypes"),
        triggerPathClasses = stringList("triggerPathClasses"),
        optionalOnlyTriggerPathClasses = stringList("optionalOnlyTriggerPathClasses"),
        secretZoneId = nullableString("secretZoneId"),
        secretZoneEntered = booleanValue("secretZoneEntered"),
        secretRewardNodePresent = booleanValue("secretRewardNodePresent"),
        criticalPathReachable = booleanValue("criticalPathReachable"),
        searchFailureKeepsMainlineReachable = booleanValue("searchFailureKeepsMainlineReachable"),
        returnedToMainline = booleanValue("returnedToMainline"),
        returnedRoomNodeId = nullableString("returnedRoomNodeId"),
        returnedPoint = nullableString("returnedPoint"),
        expectedReturnPoint = nullableString("expectedReturnPoint"),
        returnBridgeMatchesResolvedNodeId = booleanValue("returnBridgeMatchesResolvedNodeId"),
        proofSearchActionResult = nullableString("proofSearchActionResult"),
        solvabilityProofMatchesSearchAction = booleanValue("solvabilityProofMatchesSearchAction"),
        solvabilityProofCoversReturnBridge = booleanValue("solvabilityProofCoversReturnBridge"),
        rewardSources = stringList("rewardSources"),
        rewardBudgetSources = stringList("rewardBudgetSources"),
        expectedRewardBudgetSources = stringList("expectedRewardBudgetSources"),
        threatBudgetSources = stringList("threatBudgetSources"),
        expectedThreatBudgetSources = stringList("expectedThreatBudgetSources"),
        logKeys = stringList("logKeys"),
        frontstageActionCues = frontstageActionCueEvidence(),
        failure = nullableString("failure"),
    )

private fun JsonObject.stringList(key: String): List<String> = getValue(key).jsonArray.map { element -> element.jsonPrimitive.content }

private fun JsonObject.frontstageActionCueEvidence(): List<HiddenFrontstageActionCueEvidence> {
    val categories = stringList("frontstageActionCueCategories")
    val priorities = stringList("frontstageActionCuePriorities")
    val stableKeys = stringList("frontstageActionCueStableKeys")
    val messageKeys = stringList("frontstageActionCueMessageKeys")
    require(
        categories.size == priorities.size &&
            categories.size == stableKeys.size &&
            categories.size == messageKeys.size,
    ) {
        "Frontstage action cue evidence columns must be present and aligned: " +
            "categories=${categories.size}, priorities=${priorities.size}, stableKeys=${stableKeys.size}, messageKeys=${messageKeys.size}."
    }
    return categories.indices.map { index ->
        HiddenFrontstageActionCueEvidence(
            category = categories[index],
            priority = priorities[index],
            stableKey = stableKeys[index],
            messageKey = messageKeys[index],
        )
    }
}

private fun JsonObject.intValue(key: String): Int = getValue(key).jsonPrimitive.content.toInt()

private fun JsonObject.longValue(key: String): Long = getValue(key).jsonPrimitive.content.toLong()

private fun JsonObject.stringValue(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.booleanValue(key: String): Boolean = getValue(key).jsonPrimitive.content.toBooleanStrict()

private fun JsonObject.nullableString(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
