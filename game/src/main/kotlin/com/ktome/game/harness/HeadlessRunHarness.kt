package com.ktome.game.harness

import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
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
        val stallDetector = StallDetector(maxRepeats = stallRepeats)
        var turnCount = 0
        var checkpointVerified = false
        var checkpointSeen = false
        var checkpointTurn: Int? = null
        var failureReason: String? = null
        var stuckReason: String? = null
        var observation = RunObservationCapture.capture(session, turnCount)

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
                routeIndex = session.config.routeIndex,
                success = false,
                outcome = session.runOutcome(),
                floorReached = session.currentFloor(),
                turns = turnCount,
                goalReached = goalSatisfied(spec, observation, checkpointTurn),
                failureReason = failureReason,
                stuckReason = stuckReason,
                checkpointRoundTripVerified = checkpointVerified,
                commandStats = commandStats.toMap(),
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
            is ScenarioGoal.ReachFloor,
            is ScenarioGoal.ReachFloorOrTerminal,
            -> !observation.runOutcome.isTerminal

            else -> false
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
}
