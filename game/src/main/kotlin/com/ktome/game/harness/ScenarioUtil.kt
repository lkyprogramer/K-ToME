package com.ktome.game.harness

import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.game.FoundationGameSession
import com.ktome.game.PlayerCommand
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal class StallDetector(
    private val maxRepeats: Int = 20,
) {
    private var lastSignature: String? = null
    private var repeatCount: Int = 0

    fun observe(observation: RunObservation): String? {
        val signature = observation.signature()
        if (signature == lastSignature) {
            repeatCount += 1
        } else {
            lastSignature = signature
            repeatCount = 1
        }
        return if (repeatCount >= maxRepeats) {
            "Repeated state $signature for $repeatCount observations."
        } else {
            null
        }
    }

    fun reset() {
        lastSignature = null
        repeatCount = 0
    }
}

object RunObservationCapture {
    fun capture(
        session: FoundationGameSession,
        turnIndex: Int,
    ): RunObservation {
        val visibleTiles = session.visibleTiles()
        val exploredTiles = session.exploredTiles()
        val knownDownstairsPositions =
            exploredTiles
                .mapNotNull { point ->
                    session.inspectAt(point)
                        .takeIf { it.stairDirectionId == "DOWN" }
                        ?.point
                }
                .distinct()
                .sortedWith(compareBy<Point> { it.y }.thenBy { it.x })

        val visibleGroundItemPositions =
            visibleTiles
                .mapNotNull { point ->
                    session.inspectAt(point)
                        .takeIf { it.items.isNotEmpty() }
                        ?.point
                }
                .distinct()
                .sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
        val visibleInteractables =
            session.renderSnapshot().props
                .asSequence()
                .filter { prop -> prop.propTypeId != "stairs" }
                .map { prop ->
                    ObservedInteractable(
                        id = prop.propTypeId,
                        position = Point(prop.x, prop.y),
                        interactionTags = session.automationInteractableTags(prop.propTypeId),
                    )
                }
                .distinctBy { interactable -> interactable.id to interactable.position }
                .sortedWith(compareBy<ObservedInteractable> { it.position.y }.thenBy { it.position.x })
                .toList()

        val visibleBlockingPositions =
            session.actorViews()
                .filterNot { it.isPlayer }
                .map { it.position }
                .toSet()
        val visibleBossPositions =
            session.renderSnapshot().actors
                .asSequence()
                .filter { actor -> !actor.isPlayer && actor.roleKind == ActorRoleKindSnapshot.BOSS }
                .map { actor -> Point(actor.x, actor.y) }
                .distinct()
                .sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
                .toList()

        return RunObservation(
            floor = session.currentFloor(),
            turnIndex = turnIndex,
            playerStatus = session.playerStatus(),
            playerResource = session.playerResourceView(),
            playerPosition = session.playerPosition(),
            map = session.map,
            visibleTiles = visibleTiles,
            exploredTiles = exploredTiles,
            visibleHostilePositions = session.targetableHostilePositions(),
            visibleBossPositions = visibleBossPositions,
            visibleBlockingPositions = visibleBlockingPositions,
            visibleGroundItemPositions = visibleGroundItemPositions,
            visibleInteractables = visibleInteractables,
            knownDownstairsPositions = knownDownstairsPositions,
            inventoryItems = session.inventoryItems(),
            talentSlots = session.talentSlots(),
            reserveTalents = session.reserveTalentSlots(),
            canAscend = session.canAscend(),
            canDescend = session.canDescend(),
            runOutcome = session.runOutcome(),
            messageLogTail = session.messageLog().takeLast(12),
            eventTail = session.recentEventLog(12),
        )
    }
}

fun RunObservation.signature(): String =
    buildString {
        append(floor)
        append('|')
        append(playerPosition.x)
        append(',')
        append(playerPosition.y)
        append('|')
        append(playerStatus.currentHp)
        append('/')
        append(playerStatus.maxHp)
        append('|')
        append(playerResource.typeId)
        append(':')
        append(playerResource.current)
        append('/')
        append(playerResource.max)
        append('|')
        append(playerStatus.level)
        append('|')
        append(playerStatus.attack)
        append('/')
        append(playerStatus.defense)
        append('/')
        append(playerStatus.speed)
        append('|')
        append(
            talentSlots.joinToString(separator = ",") { slot ->
                "${slot.slot}:${slot.talentId}:${slot.level}:${slot.committedLevel}:${slot.hasPendingAllocation}:${slot.currentCooldown}"
            },
        )
        append('|')
        append(
            reserveTalents.joinToString(separator = ",") { talent ->
                "${talent.talentId}:${talent.level}:${talent.committedLevel}:${talent.hasPendingAllocation}:${talent.currentCooldown}"
            },
        )
        append('|')
        append(inventoryItems.size)
        append('|')
        append(visibleHostilePositions.size)
    }

fun stepToward(
    observation: RunObservation,
    target: Point,
): Point? =
    AStar.findPath(
        map = observation.map,
        start = observation.playerPosition,
        goal = target,
        blocked = observation.visibleBlockingPositions - target,
    ).getOrNull(1)

fun Point.deltaFrom(origin: Point): Point =
    Point(
        x = (x - origin.x).coerceIn(-1, 1),
        y = (y - origin.y).coerceIn(-1, 1),
    )

fun PlayerCommand.commandName(): String = this::class.simpleName ?: "UnknownCommand"

fun PlayerCommand.consumesTurn(): Boolean =
    when (this) {
        PlayerCommand.Wait,
        is PlayerCommand.Move,
        PlayerCommand.Interact,
        PlayerCommand.PickUp,
        PlayerCommand.Ascend,
        PlayerCommand.Descend,
        is PlayerCommand.UseTalent,
        is PlayerCommand.ActivateInventoryItem,
        -> true

        is PlayerCommand.EquipTalentToSlot,
        is PlayerCommand.AssignStat,
        is PlayerCommand.AssignTalent,
        is PlayerCommand.RespecTalentTree,
        PlayerCommand.ConfirmTalentDraft,
        PlayerCommand.RollbackTalentDraft,
        PlayerCommand.SaveGame,
        -> false
    }

object HarnessReportWriter {
    fun reportDir(): Path {
        val configured = System.getProperty("ktome.harness.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("build", "reports", "harness")
        } else {
            Path.of(configured)
        }
    }

    fun writeJsonAndMarkdown(
        fileStem: String,
        payload: JsonElement,
        markdown: String,
    ) {
        val dir = reportDir()
        val jsonPath = dir.resolve("$fileStem.json")
        val markdownPath = dir.resolve("$fileStem.md")
        Files.createDirectories(jsonPath.parent)
        Files.createDirectories(markdownPath.parent)
        Files.writeString(jsonPath, Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), payload))
        Files.writeString(markdownPath, markdown)
    }
}
