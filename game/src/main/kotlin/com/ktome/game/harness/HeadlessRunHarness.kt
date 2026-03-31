package com.ktome.game.harness

import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.save.SaveManager
import com.ktome.core.world.ObjectiveState
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
        val stallDetector = StallDetector(maxRepeats = stallRepeats)
        var turnCount = 0
        var checkpointVerified = false
        var checkpointSeen = false
        var checkpointTurn: Int? = null
        var failureReason: String? = null
        var stuckReason: String? = null
        var observation = RunObservationCapture.capture(session, turnCount)
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
                stallDetector.reset()
            }

            val command =
                routeProgressCommand(session, observation)
                    .takeIf { shouldPrioritizeRouteProgress(spec, observation) }
                    ?: bot.decide(observation)
                    ?: run {
                    failureReason = "Bot returned no command."
                    break
                }
            commandStats[command.commandName()] = (commandStats[command.commandName()] ?: 0) + 1
            commandTail.addLast(command.commandName())
            while (commandTail.size > 12) {
                commandTail.removeFirst()
            }

            val accepted = session.perform(command)
            if (!accepted) {
                failureReason = "Command rejected: ${command.commandName()}"
                break
            }

            if (command.consumesTurn()) {
                turnCount += 1
            }

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
                commandName = command.commandName(),
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
                milestoneRewards = session.milestoneRewardSummaries(),
                cadenceRewardCount = session.currentCadenceRewardCount(),
                shopRefreshPurchaseCount = session.currentShopRefreshPurchaseCount(),
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
    ): Boolean =
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
