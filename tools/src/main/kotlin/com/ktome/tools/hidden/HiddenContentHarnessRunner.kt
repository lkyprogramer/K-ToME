package com.ktome.tools.hidden

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.map.Point
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.center
import com.ktome.core.mapgen.contains
import com.ktome.core.world.solvability.SearchActionResult
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.data.DataLoader
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
)

data class HiddenContentCaseResult(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val searchBindingId: String,
    val entranceBindingId: String,
    val resolvedReturnBridgeNodeId: String,
    val searchActionResult: String,
    val triggerType: String,
    val hiddenEventIds: List<String>,
    val triggerTypes: List<String>,
    val triggerPathClasses: List<String>,
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
    val failure: String? = null,
) {
    val triggerPathClassesWithinOptionalOrSecret: Boolean
        get() = triggerPathClasses.all { pathClass -> pathClass == PathClass.OPTIONAL.name || pathClass == PathClass.SECRET.name }

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
            if (hiddenEventIds.isNotEmpty() && !triggerPathClassesWithinOptionalOrSecret) {
                add("case.hidden_event_outside_optional_or_secret")
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
            put("entranceBindingId", entranceBindingId)
            put("resolvedReturnBridgeNodeId", resolvedReturnBridgeNodeId)
            put("searchActionResult", searchActionResult)
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
            put("triggerPathClassesWithinOptionalOrSecret", triggerPathClassesWithinOptionalOrSecret)
            put("rewardBridgeBackedByLootBudget", rewardBridgeBackedByLootBudget)
            put("encounterBridgeBackedByThreatBudget", encounterBridgeBackedByThreatBudget)
            putJsonArray("triggerTypes") {
                triggerTypes.forEach { triggerType -> add(JsonPrimitive(triggerType)) }
            }
            putJsonArray("triggerPathClasses") {
                triggerPathClasses.forEach { pathClass -> add(JsonPrimitive(pathClass)) }
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
) {
    val hiddenTriggerTypeCoverage: Double
        get() = hiddenTriggerTypeSet.size.toDouble() / HiddenTriggerType.entries.size.toDouble()

    val secretEntranceBindingCoverage: Int
        get() = secretEntranceBindingSet.size
}

internal const val MIN_HIDDEN_EVENT_TRIGGER_RATE: Double = 0.30
internal const val MIN_SECRET_ZONE_DISCOVERY_RATE: Double = 0.10
internal const val MIN_HIDDEN_TRIGGER_TYPE_COVERAGE: Double = 2.0 / 6.0
internal const val MIN_SECRET_ENTRANCE_BINDING_COVERAGE: Int = 1

internal object HiddenContentRegistrySnapshot {
    fun load(): HiddenContentRegistryMetrics {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        return HiddenContentRegistryMetrics(
            hiddenTriggerTypeSet = catalog.hiddenEvents.mapTo(linkedSetOf()) { hiddenEvent -> hiddenEvent.triggerType.name },
            secretEntranceBindingSet = catalog.secretZones.mapTo(linkedSetOf()) { secretZone -> secretZone.entranceBindingId.value },
        )
    }
}

internal object HiddenContentHarnessKernel {
    private const val FLOOR_INDEX: Int = 1
    private const val SEED_BASE: Long = 20260407070000L
    private const val ZONE_SEED_BLOCK: Long = 1_000L
    private const val SEEDS_PER_ZONE: Int = 125
    private val upgradedZones: List<String> =
        listOf(
            "greenwood_fringe",
            "deep_iron_pit",
            "underground_river",
            "abyssal_temple",
        )

    fun execute(): HiddenContentKernelRun {
        val cases =
            upgradedZones.flatMapIndexed { zoneOrdinal, zoneId ->
                (0 until SEEDS_PER_ZONE).map { seedOrdinal ->
                    HiddenContentCaseSpec(
                        zoneId = zoneId,
                        floorIndex = FLOOR_INDEX,
                        seed = SEED_BASE + zoneOrdinal * ZONE_SEED_BLOCK + seedOrdinal,
                    )
                }
            }
        val header = phase4HarnessHeader(harnessId = HiddenContentHarnessRunner.HARNESS_ID, seedList = cases.map(HiddenContentCaseSpec::seed))
        val results = cases.map(::executeCase)
        return HiddenContentKernelRun(header = header, results = results)
    }

    private fun executeCase(caseSpec: HiddenContentCaseSpec): HiddenContentCaseResult =
        try {
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
            val entrance =
                session.automationGeneratedFloor().entrances
                    .sortedBy { candidate -> candidate.bindingId.value }
                    .first()
            val generatedFloor = session.automationGeneratedFloor()
            val entranceRoom = requireNotNull(generatedFloor.roomForEntrance(entrance))
            val searchPoint = entranceRoom.center
            session.automationMovePlayerTo(searchPoint)
            session.perform(PlayerCommand.Search)
            val searchResult =
                session.automationSearchState()
                    .firstOrNull { entry -> entry.bindingId == entrance.bindingId }
                    ?.result
                    ?: SearchActionResult.NO_TARGET
            var secretRewardNodePresent = false
            var secretRewardPathClass = PathClass.SECRET.name

            if (searchResult == SearchActionResult.REVEALED) {
                session.renderSnapshot().props.firstOrNull { prop -> prop.propTypeId == "hidden_entrance" }?.let { prop ->
                    session.automationMovePlayerTo(Point(prop.x, prop.y))
                    session.perform(PlayerCommand.Interact)
                }
                val rewardProp = session.renderSnapshot().props.firstOrNull { prop -> prop.propTypeId == "secret_reward" }
                secretRewardNodePresent = rewardProp != null
                secretRewardPathClass =
                    rewardProp
                        ?.let { prop ->
                            secretZoneAnchorPathClass(
                                generatedFloor = generatedFloor,
                                entrance = entrance,
                                point = Point(prop.x, prop.y),
                            )
                        }
                        ?: "UNKNOWN"
                rewardProp?.let { prop ->
                    session.automationMovePlayerTo(Point(prop.x, prop.y))
                    session.perform(PlayerCommand.Interact)
                }
                session.renderSnapshot().props.firstOrNull { prop -> prop.propTypeId == "secret_return" }?.let { prop ->
                    session.automationMovePlayerTo(Point(prop.x, prop.y))
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
                    when (hiddenEvent.triggerType) {
                        HiddenTriggerType.PERCEPTION_REVEAL -> entranceRoom.pathClass.name
                        HiddenTriggerType.INTERACT_TILE -> secretRewardPathClass
                        else -> generatedFloor.roomAt(searchPoint)?.pathClass?.name ?: "UNKNOWN"
                    }
                }
            val secretZoneId = session.automationVisitedSecretZoneIds().firstOrNull()?.id
            val proof = session.automationSolvabilityProof()
            val proofSearchActionResult = proof.searchStates.firstOrNull { entry -> entry.bindingId == entrance.bindingId }?.result?.name
            val finalSnapshot = session.renderSnapshot()
            val actualPlayerPoint = session.playerPosition()
            val finalRoom = generatedFloor.roomAt(actualPlayerPoint)
            val secretZoneEntered = secretZoneId != null
            val returnedToMainline = !secretZoneEntered || finalRoom?.pathClass != PathClass.SECRET
            val returnRoom =
                generatedFloor.rooms.firstOrNull { room -> room.nodeId == entrance.resolvedReturnBridgeNodeId }
            val expectedReturnPoint = returnRoom?.let { room -> secretZoneAnchorEntryPoint(generatedFloor = generatedFloor, room = room) }
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
                entranceBindingId = entrance.entranceAnchorId.value,
                resolvedReturnBridgeNodeId = entrance.resolvedReturnBridgeNodeId.value,
                searchActionResult = searchResult.name,
                triggerType = triggerType,
                hiddenEventIds = hiddenEventIds,
                triggerTypes = triggerTypes,
                triggerPathClasses = triggerPathClasses,
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
            )
        } catch (exception: Exception) {
            HiddenContentCaseResult(
                zoneId = caseSpec.zoneId,
                floorIndex = caseSpec.floorIndex,
                seed = caseSpec.seed,
                searchBindingId = "",
                entranceBindingId = "",
                resolvedReturnBridgeNodeId = "",
                searchActionResult = SearchActionResult.NO_TARGET.name,
                triggerType = "NONE",
                hiddenEventIds = emptyList(),
                triggerTypes = emptyList(),
                triggerPathClasses = emptyList(),
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
                failure = exception.message ?: exception::class.simpleName.orEmpty(),
            )
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

    private fun secretZoneAnchorEntryPoint(
        generatedFloor: com.ktome.core.mapgen.GeneratedFloor,
        room: com.ktome.core.mapgen.RoomInstance,
    ): Point =
        roomWalkablePoints(generatedFloor = generatedFloor, room = room).firstOrNull() ?: room.center

    private fun secretZoneAnchorPathClass(
        generatedFloor: com.ktome.core.mapgen.GeneratedFloor,
        entrance: com.ktome.core.mapgen.GeneratedEntrance,
        point: Point,
    ): String {
        val secretRoom =
            generatedFloor.roomByAnchor(entrance.targetAnchorId)
                ?: generatedFloor.rooms.firstOrNull { room -> room.nodeId == entrance.targetNodeId }
        if (secretRoom != null) {
            val anchors = secretZoneAnchorPoints(generatedFloor = generatedFloor, room = secretRoom)
            if (point == anchors.entry || point == anchors.reward || point == anchors.returnBridge) {
                return secretRoom.pathClass.name
            }
        }
        return generatedFloor.roomAt(point)?.pathClass?.name ?: "UNKNOWN"
    }

    private fun secretZoneAnchorPoints(
        generatedFloor: com.ktome.core.mapgen.GeneratedFloor,
        room: com.ktome.core.mapgen.RoomInstance,
    ): SecretZoneAnchorPoints {
        val points = roomWalkablePoints(generatedFloor = generatedFloor, room = room)
        val entry = points.firstOrNull() ?: room.center
        val reward = points.firstOrNull { point -> point != entry } ?: entry
        val returnBridge = points.firstOrNull { point -> point != entry && point != reward } ?: reward
        return SecretZoneAnchorPoints(entry = entry, reward = reward, returnBridge = returnBridge)
    }

    private fun roomWalkablePoints(
        generatedFloor: com.ktome.core.mapgen.GeneratedFloor,
        room: com.ktome.core.mapgen.RoomInstance,
    ): List<Point> =
        buildList {
            for (y in room.y until room.y + room.height) {
                for (x in room.x until room.x + room.width) {
                    val point = Point(x, y)
                    if (generatedFloor.map.isInBounds(x, y) && room.contains(point) && !generatedFloor.map[point].blocksMovement) {
                        add(point)
                    }
                }
            }
        }.sortedWith(
            compareBy<Point> { point -> point.chebyshevDistanceTo(room.center) }
                .thenBy(Point::y)
                .thenBy(Point::x),
        )

    private fun Point.toDebugString(): String = "$x,$y"

    private data class SecretZoneAnchorPoints(
        val entry: Point,
        val reward: Point,
        val returnBridge: Point,
    )
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
                if (results.any { result -> result.hiddenEventIds.isNotEmpty() && !result.triggerPathClassesWithinOptionalOrSecret }) {
                    add("aggregate.hidden_event_outside_optional_or_secret")
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
                explicitSearchRevealCount = results.count { result -> result.searchActionResult == SearchActionResult.REVEALED.name },
                searchFailureCount = results.count { result -> result.searchActionResult == SearchActionResult.FAILED_CHECK.name },
                zeroHiddenEventZoneCount = zoneBreakdown.values.count { metrics -> metrics.hiddenEventTriggerCount == 0 },
                zeroSecretZoneZoneCount = zoneBreakdown.values.count { metrics -> metrics.secretZoneDiscoveryCount == 0 },
                criticalPathFailureCount = results.count { result -> !result.criticalPathReachable },
                triggerContextFailureCount = results.count { result -> result.hiddenEventIds.isNotEmpty() && !result.triggerPathClassesWithinOptionalOrSecret },
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
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        val eventsPath = outputDir.resolve(EVENTS_FILE)
        Files.writeString(
            summaryPath,
            json.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(kernelRun = kernelRun, analysis = analysis),
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

    private fun buildSummaryPayload(
        kernelRun: HiddenContentKernelRun,
        analysis: HiddenContentAnalysis,
    ): JsonObject {
        val summary = analysis.summary
        return buildJsonObject {
            put("header", kernelRun.header.toJson())
            putJsonObject("summary") {
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
                put("hiddenTriggerTypeCoverage", summary.hiddenTriggerTypeCoverage)
                putJsonArray("hiddenTriggerTypeSet") {
                    summary.hiddenTriggerTypeSet.sorted().forEach { triggerType -> add(JsonPrimitive(triggerType)) }
                }
                put("secretEntranceBindingCoverage", summary.secretEntranceBindingCoverage)
                putJsonArray("secretEntranceBindingSet") {
                    summary.secretEntranceBindingSet.sorted().forEach { bindingId -> add(JsonPrimitive(bindingId)) }
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
