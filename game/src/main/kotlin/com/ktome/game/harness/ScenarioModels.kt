package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.profile.MilestoneRewardSummary
import com.ktome.core.run.RunOutcome
import com.ktome.core.snapshot.RouteSelectionSnapshot
import com.ktome.core.world.ObjectiveState
import com.ktome.game.BreakpointPayoffObservation
import com.ktome.game.BreakpointPayoffSummary
import com.ktome.game.FOUNDATION_ZONE_ID
import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.FOUNDATION_RACE_ID
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.InventoryItemView
import com.ktome.game.PlayerResourceView
import com.ktome.game.PlayerStatus
import com.ktome.game.TalentReserveView
import com.ktome.game.TalentSlotView
import com.ktome.game.TalentTreeView
import com.ktome.game.loot.MilestoneRewardScoreSample
import com.ktome.game.routeToken
import com.ktome.game.secretRouteMarker
import com.ktome.game.validateRouteTokenPart
import com.ktome.game.validateSecretRouteMarkerId

enum class ScenarioType(
    val reportValue: String,
) {
    FULL_ROUTE("full_route"),
    BRANCH_INCLUSIVE("branch_inclusive"),
    ROUTE_PROBE("route_probe"),
    LATE_ROUTE_PROBE("late_route_probe"),
    ;

    val isFullRoute: Boolean
        get() = this == FULL_ROUTE
}

data class ScenarioSpec(
    val name: String,
    val seed: Long,
    val zoneId: String = FOUNDATION_ZONE_ID,
    val professionId: String = FOUNDATION_PROFESSION_ID,
    val raceId: String = FOUNDATION_RACE_ID,
    val zoneRoute: List<String> = listOf(zoneId),
    val routeIndex: Int = 0,
    val routeIntent: List<String> = zoneRoute,
    val routeTokenParts: List<String> = zoneRoute,
    val primarySecretZoneId: String? = null,
    val probeRoute: List<String> = emptyList(),
    val scenarioType: ScenarioType = inferScenarioType(zoneId = zoneId, zoneRoute = zoneRoute, routeIndex = routeIndex),
    val corpusId: String = HarnessMetadata.DEFAULT_CORPUS_ID,
    val maxTurns: Int,
    val goal: ScenarioGoal,
    val initialTalentPointGrant: Int = 0,
    val saveLoadCheckpoint: SaveLoadCheckpoint? = null,
    val assertions: List<ScenarioAssertion> = emptyList(),
) {
    init {
        require(
            isCompatibleScenarioType(
                scenarioType = scenarioType,
                zoneId = zoneId,
                zoneRoute = zoneRoute,
                routeIndex = routeIndex,
                primarySecretZoneId = primarySecretZoneId,
            ),
        ) {
            "Scenario '$name' uses incompatible scenarioType=${scenarioType.reportValue} for zoneId=$zoneId, routeIndex=$routeIndex, zoneRoute=$zoneRoute."
        }
        validateScenarioRouteParts(scenarioName = name, fieldName = "routeIntent", parts = routeIntent, allowEmpty = false)
        validateScenarioRouteParts(scenarioName = name, fieldName = "routeTokenParts", parts = routeTokenParts, allowEmpty = false)
        primarySecretZoneId?.let(::validateSecretRouteMarkerId)
        require(primarySecretZoneId == null || secretRouteMarker(primarySecretZoneId) in routeTokenParts) {
            "Scenario '$name' primarySecretZoneId=$primarySecretZoneId must appear as a secret route marker in routeTokenParts=$routeTokenParts."
        }
        validateScenarioRouteParts(scenarioName = name, fieldName = "probeRoute", parts = probeRoute, allowEmpty = true)
        require(
            corpusId != HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID ||
                zoneId !in FORBIDDEN_ROUTE_DIVERSITY_START_ZONE_IDS,
        ) {
            "Scenario '$name' uses forbidden route diversity start zone '$zoneId'."
        }
        require(initialTalentPointGrant >= 0) {
            "Scenario '$name' initialTalentPointGrant must be non-negative, got $initialTalentPointGrant."
        }
    }
}

private fun validateScenarioRouteParts(
    scenarioName: String,
    fieldName: String,
    parts: List<String>,
    allowEmpty: Boolean,
) {
    require(allowEmpty || parts.isNotEmpty()) {
        "Scenario '$scenarioName' $fieldName must not be empty."
    }
    parts.forEach { part ->
        try {
            validateRouteTokenPart(part)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Scenario '$scenarioName' $fieldName contains invalid route token part '$part': ${error.message}",
                error,
            )
        }
    }
}

sealed interface ScenarioGoal {
    fun isSatisfied(observation: RunObservation): Boolean

    data class ReachFloor(
        val floor: Int,
    ) : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean = observation.floor >= floor
    }

    data object ReachTerminal : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean = observation.runOutcome.isTerminal
    }

    data object Victory : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean = observation.runOutcome is RunOutcome.Victory
    }

    data class ReachFloorOrTerminal(
        val floor: Int,
    ) : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean =
            observation.floor >= floor ||
                (observation.runOutcome.isTerminal && observation.runOutcome !is RunOutcome.Defeat)
    }

    data class ReachZoneAtLeastOrTerminal(
        val zoneId: String,
    ) : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean =
            zoneDepth(observation.zoneId) >= zoneDepth(zoneId) ||
                (observation.runOutcome.isTerminal && observation.runOutcome !is RunOutcome.Defeat)
    }

    data class SurviveTurns(
        val turns: Int,
    ) : ScenarioGoal {
        override fun isSatisfied(observation: RunObservation): Boolean = observation.turnIndex >= turns
    }
}

data class SaveLoadCheckpoint(
    val floor: Int,
    val continueTurns: Int,
)

data class ZoneHeadlessMilestone(
    val zoneId: String,
    val turnIndex: Int,
    val headlessTurnEquivalent: Int,
    val deltaTurns: Int,
    val deltaHeadlessTurns: Int,
)

data class CaptainEncounterTraceEntry(
    val turnIndex: Int,
    val headlessTurnEquivalent: Int,
    val floor: Int,
    val playerHp: Int,
    val playerMaxHp: Int,
    val playerResourceCurrent: Int,
    val playerResourceMax: Int,
    val playerResourceTypeId: String,
    val captainHp: Int?,
    val captainMaxHp: Int?,
    val captainDistance: Int?,
    val command: String?,
    val recentMessages: List<String>,
    val recentEvents: List<String>,
)

data class ZoneObjectiveSummary(
    val zoneId: String,
    val questId: String,
    val objectiveId: String,
    val state: ObjectiveState,
    val completionFlagGranted: Boolean,
)

data class ZoneTraversalDiagnostic(
    val zoneId: String,
    val visitCount: Int,
    val playerTurns: Int,
    val enemyTurns: Int,
    val enemyTurnsPerPlayerTurn: Double,
    val visibleHostileTurnCount: Int,
    val liveHostileWindow: Int,
    val maxVisibleHostiles: Int,
    val objectiveAcquireTurn: Int? = null,
    val objectiveAcquireHeadlessTurnEquivalent: Int? = null,
    val objectiveStateAtExit: ObjectiveState? = null,
)

data class ScenarioReport(
    val name: String,
    val seed: Long,
    val zoneId: String = FOUNDATION_ZONE_ID,
    val professionId: String,
    val raceId: String = FOUNDATION_RACE_ID,
    val routeIndex: Int = 0,
    val finalZoneId: String = zoneId,
    val zoneRouteHash: String = zoneId,
    val zonePath: List<String> = listOf(zoneId),
    val routeToken: String = routeToken(zonePath),
    val routeIntent: List<String> = zonePath,
    val primarySecretZoneId: String? = null,
    val visitedSecretZoneIds: Set<String> = emptySet(),
    val probeRouteHash: String? = null,
    val seedString: String = seed.toString(),
    val scenarioType: ScenarioType = inferScenarioType(zoneId = zoneId, zoneRoute = zonePath, routeIndex = routeIndex),
    val success: Boolean,
    val outcome: RunOutcome,
    val floorReached: Int,
    val turns: Int,
    val headlessTurnEquivalent: Int = turns,
    val buildId: String = HarnessMetadata.BUILD_ID,
    val phaseId: String = HarnessMetadata.PHASE_ID,
    val rulesetVersion: String = HarnessMetadata.RULESET_VERSION,
    val traceSchemaVersion: String = HarnessMetadata.TRACE_SCHEMA_VERSION,
    val corpusId: String = HarnessMetadata.DEFAULT_CORPUS_ID,
    val localeId: String = "headless",
    val profileId: String = HarnessMetadata.PROFILE_ID,
    val buildHash: String? = null,
    val terminalWeaponBaseId: String? = null,
    val breakpointPayoffs: List<BreakpointPayoffSummary> = emptyList(),
    val breakpointPayoffObservations: List<BreakpointPayoffObservation> = emptyList(),
    val milestoneRewards: List<MilestoneRewardSummary> = emptyList(),
    val milestoneRewardScoreSamples: List<MilestoneRewardScoreSample> = emptyList(),
    val cadenceRewardCount: Int = 0,
    val shopRefreshPurchaseCount: Int = 0,
    val lateRunReliquaryPurchaseCount: Int = 0,
    val lateRunReliquaryVisitCount: Int = 0,
    val lateRunReliquaryRefreshCount: Int = 0,
    val lateRunReliquaryItemPurchaseCount: Int = 0,
    val lateRunReliquaryNonMandatoryPurchaseCount: Int = 0,
    val lateRunReliquaryShardSpent: Int = 0,
    val lateRunReliquaryTagDistribution: Map<String, Int> = emptyMap(),
    val affixSynergyActivationCount: Int = 0,
    val affixSynergyActivationDistribution: Map<String, Int> = emptyMap(),
    val starterProfessionTalentCount: Int = 0,
    val learnedTalentChoiceEventCount: Int = 0,
    val learnableNonStarterTalentCount: Int = 0,
    val breakpointChoiceEventCount: Int = 0,
    val breakpointPreviewAvailable: Boolean = false,
    val talentTreeInvestmentByTree: Map<String, Int> = emptyMap(),
    val talentTreePrimaryInvestmentTreeId: String? = null,
    val talentTreePrimaryInvestmentPoints: Int = 0,
    val multiTreeInvestmentAboveThreshold: Boolean = false,
    val talentReserveSwapCount: Int = 0,
    val rankBreakpointAdoptionByTalent: Map<String, Int> = emptyMap(),
    val autoLearnedNonStarterTalentCount: Int = 0,
    val startingInscriptionCount: Int = 0,
    val inscriptionInstallCount: Int = 0,
    val inscriptionReplaceCount: Int = 0,
    val fullSlotInscriptionPurchaseBlockedWithoutReplacementCount: Int = 0,
    val fullSlotInscriptionPurchaseReplacementPromptCount: Int = 0,
    val inscriptionPurchaseCancelledAfterReplacementPrompt: Int = 0,
    val shopPurchaseDeniedInsufficientGoldCount: Int = 0,
    val shopInscriptionOfferSeenCount: Int = 0,
    val shopInscriptionOfferPurchaseCount: Int = 0,
    val terminalInscriptionLoadout: List<String> = emptyList(),
    val terminalInscriptionCategoryCounts: Map<String, Int> = emptyMap(),
    val inscriptionReplaceReasonDistribution: Map<String, Int> = emptyMap(),
    val goalReached: Boolean,
    val failureReason: String? = null,
    val stuckReason: String? = null,
    val assertionFailures: List<String> = emptyList(),
    val checkpointRoundTripVerified: Boolean = false,
    val commandStats: Map<String, Int> = emptyMap(),
    val zoneHeadlessMilestones: List<ZoneHeadlessMilestone> = emptyList(),
    val zoneObjectiveSummaries: List<ZoneObjectiveSummary> = emptyList(),
    val zoneTraversalDiagnostics: List<ZoneTraversalDiagnostic> = emptyList(),
    val captainEncounterTrace: List<CaptainEncounterTraceEntry> = emptyList(),
    val lastCommands: List<String> = emptyList(),
    val lastMessages: List<String> = emptyList(),
    val eventTail: List<String> = emptyList(),
) {
    val isFullRoute: Boolean
        get() = scenarioType.isFullRoute

    /**
     * Normal defeats should not be treated as harness crashes/stalls by acceptance labs that
     * separately track progression thresholds.
     */
    fun crashedOrStalled(): Boolean =
        stuckReason != null ||
            (failureReason != null && failureReason != "Turn budget exhausted.")
}

data class ZoneRouteHashDiversity(
    val totalRuns: Int,
    val distinctHashes: Int,
    val fullRouteIntentDistinctCount: Int,
    val actualFullRouteHashDistinctCount: Int,
    val topHash: String,
    val topHashCount: Int,
    val topHashShare: Double,
    val probeRouteHashSample: List<String>,
)

sealed interface ScenarioAssertion {
    fun verify(report: ScenarioReport): String?

    data class ReachedFloorAtLeast(
        val floor: Int,
    ) : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.floorReached >= floor) null else "Expected floor >= $floor but was ${report.floorReached}."
    }

    data object NoStall : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.stuckReason == null) null else "Run stalled: ${report.stuckReason}"
    }

    data object NoFailure : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.failureReason == null) null else "Run failed: ${report.failureReason}"
    }

    data object CheckpointRoundTrip : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.checkpointRoundTripVerified) null else "Checkpoint round-trip was not verified."
    }

    data object Victory : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.outcome is RunOutcome.Victory) null else "Expected victory but got ${report.outcome}."
    }

    data class FinalZoneAtLeast(
        val zoneId: String,
    ) : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (zoneDepth(report.finalZoneId) >= zoneDepth(zoneId)) {
                null
            } else {
                "Expected final zone >= $zoneId but was ${report.finalZoneId}."
            }
    }

    data class VisitedZone(
        val zoneId: String,
    ) : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? =
            if (report.zonePath.contains(zoneId)) {
                null
            } else {
                "Expected route to visit $zoneId but path was ${report.zonePath}."
            }
    }

    data object VisitedPrimarySecretZone : ScenarioAssertion {
        override fun verify(report: ScenarioReport): String? {
            val primarySecretZoneId =
                report.primarySecretZoneId
                    ?: return "Expected primarySecretZoneId for secret branch assertion."
            return when {
                primarySecretZoneId !in report.visitedSecretZoneIds ->
                    "Expected route to visit primary secret zone $primarySecretZoneId but visited secret zones were ${report.visitedSecretZoneIds}."
                secretRouteMarker(primarySecretZoneId) !in report.routeToken ->
                    "Expected routeToken to contain runtime secret marker ${secretRouteMarker(primarySecretZoneId)} but was ${report.routeToken}."
                else -> null
            }
        }
    }
}

internal fun zoneDepth(zoneId: String): Int =
    when (zoneId) {
        "shattered_outpost" -> 0
        "greenwood_fringe" -> 1
        "bandit_camp" -> 2
        "elven_ruins" -> 3
        "deep_iron_pit" -> 4
        "molten_core" -> 5
        "grey_gate_depths" -> 6
        "underground_river" -> 7
        "crystal_cavern" -> 8
        "abyssal_temple" -> 9
        "abyssal_heart" -> 10
        else -> -1
    }

internal val OPTIONAL_ROUTE_ZONE_IDS: Set<String> =
    setOf(
        "bandit_camp",
        "elven_ruins",
        "molten_core",
        "crystal_cavern",
    )

internal val FORBIDDEN_ROUTE_DIVERSITY_START_ZONE_IDS: Set<String> =
    setOf(
        "grey_gate_depths",
        "abyssal_temple",
    )

internal val FOUNDATION_BANDIT_ROUTE: List<String> =
    listOf(
        "shattered_outpost",
        "greenwood_fringe",
        "bandit_camp",
        "greenwood_fringe",
        "deep_iron_pit",
        "grey_gate_depths",
        "underground_river",
        "abyssal_temple",
        "abyssal_heart",
    )

internal val FOUNDATION_ELVEN_ROUTE: List<String> =
    listOf(
        "shattered_outpost",
        "greenwood_fringe",
        "elven_ruins",
        "greenwood_fringe",
        "deep_iron_pit",
        "grey_gate_depths",
        "underground_river",
        "abyssal_temple",
        "abyssal_heart",
    )

internal val FOUNDATION_MOLTEN_ROUTE: List<String> =
    listOf(
        "shattered_outpost",
        "greenwood_fringe",
        "deep_iron_pit",
        "molten_core",
        "deep_iron_pit",
        "grey_gate_depths",
        "underground_river",
        "abyssal_temple",
        "abyssal_heart",
    )

internal val FOUNDATION_CRYSTAL_ROUTE: List<String> =
    listOf(
        "shattered_outpost",
        "greenwood_fringe",
        "deep_iron_pit",
        "grey_gate_depths",
        "underground_river",
        "crystal_cavern",
        "underground_river",
        "abyssal_temple",
        "abyssal_heart",
    )

internal fun scenarioTypeDistribution(
    reports: List<ScenarioReport>,
    includeZeroCounts: Boolean = false,
): Map<String, Int> =
    buildMap {
        ScenarioType.entries.forEach { scenarioType ->
            val count = reports.count { report -> report.scenarioType == scenarioType }
            if (includeZeroCounts || count > 0) {
                put(scenarioType.reportValue, count)
            }
        }
    }

internal fun zoneRouteHashDiversity(reports: List<ScenarioReport>): ZoneRouteHashDiversity {
    val terminalReports =
        reports.filterNot { report ->
            report.scenarioType == ScenarioType.ROUTE_PROBE ||
                report.scenarioType == ScenarioType.LATE_ROUTE_PROBE
        }
    val distribution = terminalReports.groupingBy(ScenarioReport::zoneRouteHash).eachCount()
    val top = distribution.maxWithOrNull(compareBy<Map.Entry<String, Int>> { entry -> entry.value }.thenBy { entry -> entry.key })
    val totalRuns = terminalReports.size
    return ZoneRouteHashDiversity(
        totalRuns = totalRuns,
        distinctHashes = distribution.size,
        fullRouteIntentDistinctCount =
            terminalReports
                .filter { report -> report.scenarioType == ScenarioType.FULL_ROUTE }
                .map { report -> routeToken(report.routeIntent) }
                .distinct()
                .size,
        actualFullRouteHashDistinctCount =
            terminalReports
                .filter { report -> report.scenarioType == ScenarioType.FULL_ROUTE }
                .map(ScenarioReport::zoneRouteHash)
                .distinct()
                .size,
        topHash = top?.key.orEmpty(),
        topHashCount = top?.value ?: 0,
        topHashShare =
            if (totalRuns == 0) {
                0.0
            } else {
                (top?.value ?: 0).toDouble() / totalRuns.toDouble()
            },
        probeRouteHashSample =
            reports
                .mapNotNull(ScenarioReport::probeRouteHash)
                .distinct()
                .sorted(),
    )
}

internal fun inferScenarioType(
    zoneId: String,
    zoneRoute: List<String>,
    routeIndex: Int,
): ScenarioType =
    when {
        isFormalFullRouteStart(zoneId = zoneId, zoneRoute = zoneRoute, routeIndex = routeIndex) -> ScenarioType.FULL_ROUTE
        isBranchInclusiveStart(zoneId = zoneId, zoneRoute = zoneRoute, routeIndex = routeIndex) -> ScenarioType.BRANCH_INCLUSIVE
        zoneDepth(zoneId) >= zoneDepth("abyssal_temple") -> ScenarioType.LATE_ROUTE_PROBE
        else -> ScenarioType.ROUTE_PROBE
    }

private fun isCompatibleScenarioType(
    scenarioType: ScenarioType,
    zoneId: String,
    zoneRoute: List<String>,
    routeIndex: Int,
    primarySecretZoneId: String?,
): Boolean =
    when (scenarioType) {
        ScenarioType.FULL_ROUTE -> isFormalFullRouteStart(zoneId = zoneId, zoneRoute = zoneRoute, routeIndex = routeIndex)
        ScenarioType.BRANCH_INCLUSIVE ->
            primarySecretZoneId != null ||
                isBranchInclusiveStart(zoneId = zoneId, zoneRoute = zoneRoute, routeIndex = routeIndex)
        ScenarioType.ROUTE_PROBE -> true
        ScenarioType.LATE_ROUTE_PROBE -> true
    }

private fun isFormalFullRouteStart(
    zoneId: String,
    zoneRoute: List<String>,
    routeIndex: Int,
): Boolean =
    (
        zoneId == FOUNDATION_ZONE_ID &&
            routeIndex == 0 &&
            zoneRoute == FOUNDATION_ZONE_ROUTE
    ) ||
        (
            zoneId in setOf("greenwood_fringe", "deep_iron_pit", "underground_river") &&
                routeIndex == 0 &&
                zoneRoute.firstOrNull() == zoneId &&
                "abyssal_temple" in zoneRoute
        )

private fun isBranchInclusiveStart(
    zoneId: String,
    zoneRoute: List<String>,
    routeIndex: Int,
): Boolean =
    zoneId == FOUNDATION_ZONE_ID &&
        routeIndex == 0 &&
        zoneRoute.firstOrNull() == FOUNDATION_ZONE_ID &&
        zoneRoute.any(OPTIONAL_ROUTE_ZONE_IDS::contains)

data class RunObservation(
    val zoneId: String = FOUNDATION_ZONE_ID,
    val floor: Int,
    val turnIndex: Int,
    val playerStatus: PlayerStatus,
    val playerResource: PlayerResourceView = PlayerResourceView(current = 0, max = 0, typeId = "STAMINA"),
    val shardBalance: Int = 0,
    val playerPosition: Point,
    val map: GameMap,
    val visibleTiles: Set<Point>,
    val exploredTiles: Set<Point>,
    val visibleHostilePositions: List<Point>,
    val visibleBossPositions: List<Point> = emptyList(),
    val visibleBlockingPositions: Set<Point>,
    val visibleGroundItemPositions: List<Point>,
    val visibleInteractables: List<ObservedInteractable>,
    val knownDownstairsPositions: List<Point>,
    val searchPromptAvailable: Boolean = false,
    val playerStatusTypeIds: Set<String> = emptySet(),
    val activeRouteSelection: RouteSelectionSnapshot? = null,
    val activeShopId: String? = null,
    val activeShopOffers: List<ObservedShopOffer> = emptyList(),
    val activeInscriptionReplacementPrompt: ObservedInscriptionReplacementPrompt? = null,
    val inventoryItems: List<InventoryItemView>,
    val inscriptions: List<ObservedInscription> = emptyList(),
    val talentSlots: List<TalentSlotView>,
    val reserveTalents: List<TalentReserveView> = emptyList(),
    val talentTrees: List<TalentTreeView> = emptyList(),
    val canAscend: Boolean,
    val canDescend: Boolean,
    val runOutcome: RunOutcome,
    val messageLogTail: List<String>,
    val eventTail: List<String>,
)

data class ObservedInteractable(
    val id: String,
    val position: Point,
    val interactionTags: Set<String> = emptySet(),
)

data class ObservedShopOffer(
    val index: Int,
    val offerFingerprint: String,
    val price: Int,
    val tags: Set<String> = emptySet(),
    val purchasable: Boolean = true,
)

data class ObservedInscriptionReplacementPrompt(
    val offerIndex: Int,
    val offerFingerprint: String,
    val candidateInscriptionId: String,
    val candidateCategoryId: String,
    val upgradeFromInscriptionId: String? = null,
    val categoryLimit: Int = Int.MAX_VALUE,
    val currentSlots: List<ObservedInscriptionReplacementSlot> = emptyList(),
)

data class ObservedInscriptionReplacementSlot(
    val hotkey: Int,
    val inscriptionId: String,
    val categoryId: String,
)

data class ObservedInscription(
    val hotkey: Int,
    val inscriptionId: String,
    val cooldownRemaining: Int,
    val requiresTarget: Boolean = false,
)
