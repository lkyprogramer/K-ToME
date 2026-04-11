package com.ktome.game.harness

import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.save.SaveManager
import com.ktome.core.world.ObjectiveState
import com.ktome.game.AbyssalRuntimeKeys
import com.ktome.game.BreakpointPayoffObservation
import com.ktome.game.BreakpointPayoffSummary
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.data.DataLoader
import com.ktome.game.zoneRouteHash
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque

class HeadlessRunHarness(
    private val rootDir: Path = Files.createTempDirectory("ktome-headless-harness"),
    private val bot: RunBot = SmokeBot(),
    private val stallRepeats: Int = 20,
) {
    fun run(spec: ScenarioSpec): ScenarioReport {
        val runDir = rootDir.resolve("${spec.name}-seed-${spec.seed}")
        Files.createDirectories(runDir)
        val saveManager = SaveManager(runDir.resolve("save"))
        saveManager.deleteSave()

        var session = newSession(spec, saveManager)
        val commandStats = linkedMapOf<String, Int>()
        val commandTail = ArrayDeque<String>()
        val zoneHeadlessMilestones = mutableListOf<ZoneHeadlessMilestone>()
        val visitedZonePath = mutableListOf<String>()
        val captainEncounterTrace = ArrayDeque<CaptainEncounterTraceEntry>()
        val breakpointPayoffObservations = mutableListOf<BreakpointPayoffObservation>()
        val seenBreakpointPayoffKeys = linkedSetOf<String>()
        val affixSynergyActivationTotals = linkedMapOf<String, Int>()
        var previousAffixSynergySnapshot = emptyMap<String, Int>()
        val stallDetector = StallDetector(maxRepeats = stallRepeats)
        var turnCount = 0
        var checkpointVerified = false
        var checkpointSeen = false
        var checkpointTurn: Int? = null
        var failureReason: String? = null
        var stuckReason: String? = null
        var observation = RunObservationCapture.capture(session, turnCount)
        var bossCombatLock = updateBossCombatLock(session, observation, currentLock = null)
        var previousBuildHash = session.currentCommittedBuildHash()
        appendVisitedZone(visitedZonePath, session.config.zoneId)
        appendZoneMilestone(
            milestones = zoneHeadlessMilestones,
            zoneId = session.config.zoneId,
            turnIndex = turnCount,
            headlessTurnEquivalent = session.currentHeadlessTurnEquivalent(),
        )
        appendCaptainEncounterTrace(
            trace = captainEncounterTrace,
            session = session,
            observation = observation,
            commandName = null,
        )
        recordNewBreakpointPayoffObservations(
            session = session,
            observations = breakpointPayoffObservations,
            seenKeys = seenBreakpointPayoffKeys,
            buildHashBefore = previousBuildHash,
            turnIndex = turnCount,
        )
        previousAffixSynergySnapshot =
            accumulateAffixSynergyActivationDelta(
                current = session.currentAffixSynergyActivationDistribution(),
                previous = previousAffixSynergySnapshot,
                totals = affixSynergyActivationTotals,
            )
        refreshDeferredBreakpointPayoffBuildHashChanges(
            session = session,
            observations = breakpointPayoffObservations,
        )

        while (turnCount < spec.maxTurns && !observation.runOutcome.isTerminal && !goalSatisfied(spec, observation, checkpointTurn)) {
            val checkpoint = spec.saveLoadCheckpoint
            if (checkpoint != null && !checkpointSeen && observation.floor >= checkpoint.floor) {
                val roundTrip = roundTripCheckpoint(session, saveManager)
                checkpointSeen = true
                checkpointTurn = turnCount
                checkpointVerified = roundTrip.verified
                if (!roundTrip.verified) {
                    failureReason = roundTrip.failureReason
                    break
                }
                session = roundTrip.loadedSession ?: session
                observation = RunObservationCapture.capture(session, turnCount)
                appendVisitedZone(visitedZonePath, session.config.zoneId)
                appendZoneMilestone(
                    milestones = zoneHeadlessMilestones,
                    zoneId = session.config.zoneId,
                    turnIndex = turnCount,
                    headlessTurnEquivalent = session.currentHeadlessTurnEquivalent(),
                )
                appendCaptainEncounterTrace(
                    trace = captainEncounterTrace,
                    session = session,
                    observation = observation,
                    commandName = "CheckpointReload",
                )
                previousBuildHash = session.currentCommittedBuildHash()
                recordNewBreakpointPayoffObservations(
                    session = session,
                    observations = breakpointPayoffObservations,
                    seenKeys = seenBreakpointPayoffKeys,
                    buildHashBefore = previousBuildHash,
                    turnIndex = turnCount,
                )
                previousAffixSynergySnapshot = emptyMap()
                previousAffixSynergySnapshot =
                    accumulateAffixSynergyActivationDelta(
                        current = session.currentAffixSynergyActivationDistribution(),
                        previous = previousAffixSynergySnapshot,
                        totals = affixSynergyActivationTotals,
                    )
                refreshDeferredBreakpointPayoffBuildHashChanges(
                    session = session,
                    observations = breakpointPayoffObservations,
                )
                stallDetector.reset()
            }

            val command =
                routeProgressCommand(session, observation)
                    .takeIf { shouldPrioritizeRouteProgress(spec, observation, bossCombatLock != null) }
                    ?: bot.decide(observation)
                    ?: run {
                    failureReason = "Bot returned no command."
                    break
                }
            val renderedCommand = renderCommand(command)
            commandStats[command.commandName()] = (commandStats[command.commandName()] ?: 0) + 1
            commandTail.addLast(renderedCommand)
            while (commandTail.size > 12) {
                commandTail.removeFirst()
            }
            val buildHashBeforeCommand = session.currentCommittedBuildHash()

            val accepted = session.perform(command)
            if (!accepted) {
                failureReason =
                    buildString {
                        append("Command rejected: ")
                        append(renderedCommand)
                        if (command is PlayerCommand.UseTalent) {
                            session.automationTalentFailureReason(slot = command.slot, target = command.target)?.let { reason ->
                                append(" (")
                                append(reason)
                                append(')')
                            }
                        }
                    }
                break
            }

            if (command.consumesTurn()) {
                turnCount += 1
            }

            observation = RunObservationCapture.capture(session, turnCount)
            bossCombatLock = updateBossCombatLock(session, observation, bossCombatLock)
            appendVisitedZone(visitedZonePath, session.config.zoneId)
            appendZoneMilestone(
                milestones = zoneHeadlessMilestones,
                zoneId = session.config.zoneId,
                turnIndex = turnCount,
                headlessTurnEquivalent = session.currentHeadlessTurnEquivalent(),
            )
            appendCaptainEncounterTrace(
                trace = captainEncounterTrace,
                session = session,
                observation = observation,
                commandName = renderedCommand,
            )
            recordNewBreakpointPayoffObservations(
                session = session,
                observations = breakpointPayoffObservations,
                seenKeys = seenBreakpointPayoffKeys,
                buildHashBefore = buildHashBeforeCommand,
                turnIndex = turnCount,
            )
            previousAffixSynergySnapshot =
                accumulateAffixSynergyActivationDelta(
                    current = session.currentAffixSynergyActivationDistribution(),
                    previous = previousAffixSynergySnapshot,
                    totals = affixSynergyActivationTotals,
                )
            refreshDeferredBreakpointPayoffBuildHashChanges(
                session = session,
                observations = breakpointPayoffObservations,
            )
            stallDetector.observe(observation)?.let { reason ->
                stuckReason = reason
                break
            }
        }

        if (failureReason == null && stuckReason == null && turnCount >= spec.maxTurns && !goalSatisfied(spec, observation, checkpointTurn)) {
            failureReason = "Turn budget exhausted."
        }

        val provisional =
            ScenarioReport(
                name = spec.name,
                seed = spec.seed,
                zoneId = spec.zoneId,
                professionId = spec.professionId,
                raceId = spec.raceId,
                routeIndex = spec.routeIndex,
                finalZoneId = session.config.zoneId,
                zoneRouteHash = zoneRouteHash(visitedZonePath),
                zonePath = visitedZonePath.toList(),
                scenarioType = spec.scenarioType,
                success = false,
                outcome = session.runOutcome(),
                floorReached = session.currentFloor(),
                turns = turnCount,
                headlessTurnEquivalent = session.currentHeadlessTurnEquivalent(),
                buildId = HarnessMetadata.BUILD_ID,
                phaseId = HarnessMetadata.PHASE_ID,
                rulesetVersion = HarnessMetadata.RULESET_VERSION,
                traceSchemaVersion = HarnessMetadata.TRACE_SCHEMA_VERSION,
                corpusId = spec.corpusId,
                localeId = session.localizer().locale.id,
                profileId = HarnessMetadata.PROFILE_ID,
                buildHash = session.currentBuildHash(),
                breakpointPayoffs = session.currentBreakpointPayoffSummaries(),
                breakpointPayoffObservations = breakpointPayoffObservations.toList(),
                milestoneRewards = session.milestoneRewardSummaries(),
                cadenceRewardCount = session.currentCadenceRewardCount(),
                shopRefreshPurchaseCount = session.currentShopRefreshPurchaseCount(),
                lateRunReliquaryPurchaseCount = session.currentLateRunReliquaryPurchaseCount(),
                lateRunReliquaryVisitCount = session.currentLateRunReliquaryVisitCount(),
                lateRunReliquaryRefreshCount = session.currentLateRunReliquaryRefreshCount(),
                lateRunReliquaryItemPurchaseCount = session.currentLateRunReliquaryItemPurchaseCount(),
                lateRunReliquaryNonMandatoryPurchaseCount = session.currentLateRunReliquaryNonMandatoryPurchaseCount(),
                lateRunReliquaryShardSpent = session.currentLateRunReliquaryShardSpent(),
                lateRunReliquaryTagDistribution = session.currentLateRunReliquaryPurchaseTagDistribution(),
                affixSynergyActivationCount = affixSynergyActivationTotals.values.sum(),
                affixSynergyActivationDistribution = affixSynergyActivationTotals.toMap(linkedMapOf()),
                goalReached = goalSatisfied(spec, observation, checkpointTurn),
                failureReason = failureReason,
                stuckReason = stuckReason,
                checkpointRoundTripVerified = checkpointVerified,
                commandStats = commandStats.toMap(),
                zoneHeadlessMilestones = zoneHeadlessMilestones.toList(),
                zoneObjectiveSummaries = buildZoneObjectiveSummaries(session, visitedZonePath),
                captainEncounterTrace = captainEncounterTrace.toList(),
                lastCommands = commandTail.toList(),
                lastMessages = observation.messageLogTail,
                eventTail = observation.eventTail,
            )

        val assertionFailures = spec.assertions.mapNotNull { it.verify(provisional) }
        val success = provisional.failureReason == null && provisional.stuckReason == null && provisional.goalReached && assertionFailures.isEmpty()

        return provisional.copy(
            success = success,
            assertionFailures = assertionFailures,
        )
    }

    private fun recordNewBreakpointPayoffObservations(
        session: FoundationGameSession,
        observations: MutableList<BreakpointPayoffObservation>,
        seenKeys: MutableSet<String>,
        buildHashBefore: String,
        turnIndex: Int,
    ) {
        val buildHashAfter = session.currentCommittedBuildHash()
        session.currentBreakpointPayoffSummaries().forEach { summary ->
            if (!seenKeys.add(summary.observationKey())) {
                return@forEach
            }
            observations +=
                BreakpointPayoffObservation(
                    talentId = summary.talentId,
                    treeId = summary.treeId,
                    achievedRank = summary.achievedRank,
                    breakpointRank = summary.breakpointRank,
                    unlockedEffectKinds = summary.unlockedEffectKinds,
                    turnIndex = turnIndex,
                    headlessTurnEquivalent = session.currentHeadlessTurnEquivalent(),
                    buildHashBeforeUnlock = buildHashBefore,
                    buildHashAfterUnlock = buildHashAfter,
                    buildHashChanged = buildHashBefore != buildHashAfter,
                )
        }
    }

    private fun refreshDeferredBreakpointPayoffBuildHashChanges(
        session: FoundationGameSession,
        observations: MutableList<BreakpointPayoffObservation>,
    ) {
        val currentBuildHash = session.currentCommittedBuildHash()
        val activeTalentIds = session.talentSlots().mapTo(linkedSetOf()) { slot -> slot.talentId }
        observations.indices.forEach { index ->
            val observation = observations[index]
            if (observation.buildHashChanged || observation.talentId !in activeTalentIds) {
                return@forEach
            }
            if (currentBuildHash == observation.buildHashBeforeUnlock) {
                return@forEach
            }
            observations[index] =
                observation.copy(
                    buildHashAfterUnlock = currentBuildHash,
                    buildHashChanged = true,
                )
        }
    }

    private fun accumulateAffixSynergyActivationDelta(
        current: Map<String, Int>,
        previous: Map<String, Int>,
        totals: MutableMap<String, Int>,
    ): Map<String, Int> {
        current.forEach { (affixId, count) ->
            val delta = count - (previous[affixId] ?: 0)
            if (delta > 0) {
                totals[affixId] = (totals[affixId] ?: 0) + delta
            }
        }
        return current.toMap(linkedMapOf())
    }

    private fun BreakpointPayoffSummary.observationKey(): String =
        "$talentId@$breakpointRank:${unlockedEffectKinds.sorted().joinToString(separator = "+")}"

    private fun goalSatisfied(
        spec: ScenarioSpec,
        observation: RunObservation,
        checkpointTurn: Int?,
    ): Boolean {
        if (!spec.goal.isSatisfied(observation)) {
            return false
        }
        val checkpoint = spec.saveLoadCheckpoint ?: return true
        val observedCheckpointTurn = checkpointTurn ?: return false
        return observation.turnIndex - observedCheckpointTurn >= checkpoint.continueTurns
    }

    private fun shouldPrioritizeRouteProgress(
        spec: ScenarioSpec,
        observation: RunObservation,
        bossCombatLocked: Boolean,
    ): Boolean =
        when {
            observation.activeShopId != null -> false
            bossCombatLocked -> false

            else ->
                when (spec.goal) {
                    ScenarioGoal.ReachTerminal,
                    ScenarioGoal.Victory,
                    -> true

                    is ScenarioGoal.ReachFloor,
                    is ScenarioGoal.ReachFloorOrTerminal,
                    is ScenarioGoal.ReachZoneAtLeastOrTerminal,
                    -> !observation.runOutcome.isTerminal

                    is ScenarioGoal.SurviveTurns ->
                        spec.assertions
                            .filterIsInstance<ScenarioAssertion.FinalZoneAtLeast>()
                            .any { assertion -> zoneDepth(observation.zoneId) < zoneDepth(assertion.zoneId) }
                }
        }

    private fun updateBossCombatLock(
        session: FoundationGameSession,
        observation: RunObservation,
        currentLock: BossCombatLock?,
    ): BossCombatLock? {
        if (observation.zoneId != "deep_iron_pit") {
            return null
        }
        if (observation.visibleBossPositions.isNotEmpty()) {
            return BossCombatLock(
                zoneId = observation.zoneId,
                floor = observation.floor,
            )
        }
        val activeLock = currentLock ?: return null
        if (activeLock.zoneId != observation.zoneId || activeLock.floor != observation.floor) {
            return null
        }
        return if (session.automationBossPoint() == null) null else activeLock
    }

    private data class BossCombatLock(
        val zoneId: String,
        val floor: Int,
    )

    private fun newSession(
        spec: ScenarioSpec,
        saveManager: SaveManager,
    ): FoundationGameSession =
        GameModule.newFoundationSession(
            config =
                FoundationGameConfig(
                    seed = spec.seed,
                    zoneId = spec.zoneId,
                    playerProfessionId = spec.professionId,
                    playerRaceId = spec.raceId,
                    zoneRoute = spec.zoneRoute,
                    routeIndex = spec.routeIndex,
                ),
            saveManager = saveManager,
            availabilityContext = AvailabilityContext.DEV_LAB,
        )

    private fun roundTripCheckpoint(
        session: FoundationGameSession,
        saveManager: SaveManager,
    ): CheckpointRoundTripResult {
        val configBefore = session.config
        val floorBefore = session.currentFloor()
        val inventoryBefore = session.inventoryItems().map { it.name }
        if (!session.perform(PlayerCommand.SaveGame)) {
            return CheckpointRoundTripResult(failureReason = "Checkpoint save was rejected.")
        }
        val loaded = GameModule.loadFoundationSession(saveManager)
            ?: return CheckpointRoundTripResult(failureReason = "Checkpoint reload returned null.")

        val inventoryAfter = loaded.inventoryItems().map { it.name }
        if (loaded.currentFloor() != floorBefore) {
            return CheckpointRoundTripResult(failureReason = "Checkpoint reload changed floor from $floorBefore to ${loaded.currentFloor()}.")
        }
        if (loaded.config.zoneId != configBefore.zoneId) {
            return CheckpointRoundTripResult(
                failureReason = "Checkpoint reload changed zone from ${configBefore.zoneId} to ${loaded.config.zoneId}.",
            )
        }
        if (loaded.config.playerProfessionId != configBefore.playerProfessionId) {
            return CheckpointRoundTripResult(
                failureReason =
                    "Checkpoint reload changed profession from ${configBefore.playerProfessionId} to ${loaded.config.playerProfessionId}.",
            )
        }
        if (loaded.config.routeIndex != configBefore.routeIndex) {
            return CheckpointRoundTripResult(
                failureReason = "Checkpoint reload changed routeIndex from ${configBefore.routeIndex} to ${loaded.config.routeIndex}.",
            )
        }
        if (loaded.config.zoneRoute != configBefore.zoneRoute) {
            return CheckpointRoundTripResult(
                failureReason = "Checkpoint reload changed zoneRoute from ${configBefore.zoneRoute} to ${loaded.config.zoneRoute}.",
            )
        }
        if (loaded.worldProgress() != session.worldProgress()) {
            return CheckpointRoundTripResult(failureReason = "Checkpoint reload changed worldProgress state.")
        }
        if (loaded.shopStates() != session.shopStates()) {
            return CheckpointRoundTripResult(failureReason = "Checkpoint reload changed shopStates.")
        }
        if (loaded.currentShardBalance() != session.currentShardBalance()) {
            return CheckpointRoundTripResult(
                failureReason = "Checkpoint reload changed shardBalance from ${session.currentShardBalance()} to ${loaded.currentShardBalance()}.",
            )
        }
        if (loaded.currentHeadlessTurnEquivalent() != session.currentHeadlessTurnEquivalent()) {
            return CheckpointRoundTripResult(
                failureReason =
                    "Checkpoint reload changed headlessTurnEquivalent from ${session.currentHeadlessTurnEquivalent()} to ${loaded.currentHeadlessTurnEquivalent()}.",
            )
        }
        if (loaded.milestoneRewardSummaries() != session.milestoneRewardSummaries()) {
            return CheckpointRoundTripResult(failureReason = "Checkpoint reload changed milestoneRewardSummaries.")
        }
        if (inventoryAfter != inventoryBefore) {
            return CheckpointRoundTripResult(failureReason = "Checkpoint reload changed inventory order/content.")
        }
        return CheckpointRoundTripResult(loadedSession = loaded, verified = true)
    }

    private data class CheckpointRoundTripResult(
        val loadedSession: FoundationGameSession? = null,
        val verified: Boolean = false,
        val failureReason: String? = null,
    )

    private fun appendZoneMilestone(
        milestones: MutableList<ZoneHeadlessMilestone>,
        zoneId: String,
        turnIndex: Int,
        headlessTurnEquivalent: Int,
    ) {
        if (milestones.lastOrNull()?.zoneId == zoneId) {
            return
        }
        val previous = milestones.lastOrNull()
        milestones +=
            ZoneHeadlessMilestone(
                zoneId = zoneId,
                turnIndex = turnIndex,
                headlessTurnEquivalent = headlessTurnEquivalent,
                deltaTurns = if (previous == null) 0 else turnIndex - previous.turnIndex,
                deltaHeadlessTurns = if (previous == null) 0 else headlessTurnEquivalent - previous.headlessTurnEquivalent,
            )
    }

    private fun appendCaptainEncounterTrace(
        trace: ArrayDeque<CaptainEncounterTraceEntry>,
        session: FoundationGameSession,
        observation: RunObservation,
        commandName: String?,
    ) {
        if (session.config.zoneId != "shattered_outpost" || session.currentFloor() < 2) {
            return
        }
        val captainId = session.automationEntityByTemplateId("bandit.captain")
        val captainHealth = captainId?.let { entityId -> session.automationWorld().get<Health>(entityId) }
        val captainPosition = captainId?.let { entityId -> session.automationWorld().get<Position>(entityId)?.toPoint() }
        val entry =
            CaptainEncounterTraceEntry(
                turnIndex = observation.turnIndex,
                headlessTurnEquivalent = session.currentHeadlessTurnEquivalent(),
                floor = session.currentFloor(),
                playerHp = observation.playerStatus.currentHp,
                playerMaxHp = observation.playerStatus.maxHp,
                playerResourceCurrent = observation.playerResource.current,
                playerResourceMax = observation.playerResource.max,
                playerResourceTypeId = observation.playerResource.typeId,
                captainHp = captainHealth?.current,
                captainMaxHp = captainHealth?.max,
                captainDistance = captainPosition?.chebyshevDistanceTo(observation.playerPosition),
                command = commandName,
                recentMessages = observation.messageLogTail.takeLast(CAPTAIN_TRACE_MESSAGE_WINDOW),
                recentEvents = observation.eventTail.takeLast(CAPTAIN_TRACE_EVENT_WINDOW),
            )
        if (trace.lastOrNull() == entry) {
            return
        }
        trace.addLast(entry)
        while (trace.size > CAPTAIN_TRACE_LIMIT) {
            trace.removeFirst()
        }
    }

    private fun buildZoneObjectiveSummaries(
        session: FoundationGameSession,
        visitedZonePath: List<String>,
    ): List<ZoneObjectiveSummary> {
        val catalog = DataLoader(session.localizer().locale).loadSchemaCatalog()
        val zonesById = catalog.zones.associateBy { zone -> zone.id }
        val objectivesById = catalog.objectiveSets.associateBy { objective -> objective.id }
        val worldProgress = session.worldProgress()
        return visitedZonePath
            .distinct()
            .mapNotNull { zoneId ->
                val zone = zonesById[zoneId] ?: return@mapNotNull null
                val objective = zone.objectiveSetId?.let(objectivesById::get) ?: return@mapNotNull null
                val questId = objective.linkedQuestId ?: return@mapNotNull null
                val objectiveId = objective.questObjectiveId ?: return@mapNotNull null
                val quest = worldProgress.questStates[questId] ?: return@mapNotNull null
                val state = quest.objectiveStates[objectiveId] ?: ObjectiveState.LOCKED
                ZoneObjectiveSummary(
                    zoneId = zoneId,
                    questId = questId,
                    objectiveId = objectiveId,
                    state = state,
                    completionFlagGranted = quest.completionFlags.any(worldProgress.worldFlags::contains),
                )
            }
    }

    private fun appendVisitedZone(
        visitedZonePath: MutableList<String>,
        zoneId: String,
    ) {
        if (visitedZonePath.lastOrNull() != zoneId) {
            visitedZonePath += zoneId
        }
    }

    private companion object {
        const val CAPTAIN_TRACE_LIMIT: Int = 80
        const val CAPTAIN_TRACE_MESSAGE_WINDOW: Int = 4
        const val CAPTAIN_TRACE_EVENT_WINDOW: Int = 4
    }
}
