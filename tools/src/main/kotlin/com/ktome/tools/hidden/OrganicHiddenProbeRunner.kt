package com.ktome.tools.hidden

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.mapgen.GeneratedFloor
import com.ktome.core.mapgen.PathClass
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.core.save.SaveManager
import com.ktome.core.world.solvability.SearchActionResult
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.harness.RunObservation
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import com.ktome.game.harness.commandName
import com.ktome.game.harness.consumesTurn
import com.ktome.game.i18n.GameLocale
import com.ktome.tools.mapgen.phase4HarnessHeader
import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

object OrganicHiddenProbeRunner {
    const val HARNESS_ID: String = "organicHiddenProbe"

    private const val PROBE_BOT_ID: String = "organic-hidden-probe-bot-v5"
    private const val ORGANIC_HIDDEN_KERNEL_CACHE_VERSION: String = "uvr-pr05-organic-hidden-kernel-v4"
    private const val FLOOR_INDEX: Int = 1
    private const val SEED_BASE: Long = 20260411010000L
    private const val ZONE_SEED_BLOCK: Long = 10_000L
    private const val COMBO_SEED_BLOCK: Long = 100L
    private const val SEEDS_PER_ZONE_COMBO: Int = 11
    private const val TURN_BUDGET: Int = 52
    private const val MAX_FLOOR: Int = 1
    private const val PER_ZONE_SECRET_ENTRY_MIN_RATE: Double = 0.05
    private val json: Json = Json { prettyPrint = true }
    private val zoneIds: List<String> =
        listOf(
            "greenwood_fringe",
            "deep_iron_pit",
            "underground_river",
            "abyssal_temple",
        )
    private val releasedPlayerCreationState by lazy { GameModule.playerCreationState(locale = GameLocale.EN_US) }
    private val releasedProfessionIds: List<String> by lazy { playableProfessionIds() }
    private val releasedRaceIds: List<String> by lazy { playableRaceIds() }

    internal fun turnBudget(): Int = TURN_BUDGET

    internal fun maxFloor(): Int = MAX_FLOOR

    fun run(): OrganicHiddenProbeRun {
        val repoRoot = VerificationCacheSupport.repoRoot()
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val tempSaveRoot = outputDir.resolve("tmp")
        Files.createDirectories(tempSaveRoot)
        val cases =
            zoneIds.flatMapIndexed { zoneOrdinal, zoneId ->
                releasedProfessionIds.flatMapIndexed { professionOrdinal, professionId ->
                    releasedRaceIds.flatMapIndexed { raceOrdinal, raceId ->
                        val comboOrdinal = professionOrdinal * releasedRaceIds.size + raceOrdinal
                        (0 until SEEDS_PER_ZONE_COMBO).map { seedOrdinal ->
                            OrganicHiddenProbeCaseSpec(
                                zoneId = zoneId,
                                floorIndex = FLOOR_INDEX,
                                professionId = professionId,
                                raceId = raceId,
                                seed = seedFor(zoneOrdinal = zoneOrdinal, comboOrdinal = comboOrdinal, seedOrdinal = seedOrdinal),
                            )
                        }
                    }
                }
            }
        val header = phase4HarnessHeader(harnessId = HARNESS_ID, seedList = cases.map(OrganicHiddenProbeCaseSpec::seed))
        val kernelCache = VerificationCacheSupport.cacheDirs(domainId = "organic-hidden", repoRoot = repoRoot)
        val inputFingerprint = VerificationCacheSupport.sha256Files(organicHiddenFingerprintInputs(repoRoot))
        val kernelRoot = VerificationCacheSupport.ensureDirectory(kernelCache.kernelDir.resolve(inputFingerprint))
        var reusedShardCount = 0
        val shardSpecs = buildShardSpecs(cases)
        val shardEventPaths = mutableListOf<Path>()
        val results =
            shardSpecs
                .flatMap { shardSpec ->
                    val shardDir = VerificationCacheSupport.ensureDirectory(kernelRoot.resolve(shardSpec.shardId))
                    val shardSummaryPath = shardDir.resolve("summary.json")
                    val shardEventsPath = shardDir.resolve("events.jsonl")
                    shardEventPaths.add(shardEventsPath)
                    if (Files.isRegularFile(shardSummaryPath) && Files.isRegularFile(shardEventsPath)) {
                        reusedShardCount += 1
                        readShardResults(shardEventsPath)
                    } else {
                        val shardResults =
                            shardSpec.cases.map { caseSpec -> executeCase(caseSpec = caseSpec, tempSaveRoot = tempSaveRoot) }
                        writeShardResults(shardSummaryPath = shardSummaryPath, shardEventsPath = shardEventsPath, header = header, shardSpec = shardSpec, results = shardResults)
                        shardResults
                    }
                }.sortedWith(
                    compareBy(
                        OrganicHiddenProbeCaseResult::zoneId,
                        OrganicHiddenProbeCaseResult::professionId,
                        OrganicHiddenProbeCaseResult::raceId,
                        OrganicHiddenProbeCaseResult::seed,
                    ),
                )
        val summary =
            OrganicHiddenProbeSummaryAggregator.summarize(
                OrganicHiddenProbeSummaryRequest(
                    results = results,
                    professionIds = releasedProfessionIds,
                    raceIds = releasedRaceIds,
                    seedsPerZoneCombo = SEEDS_PER_ZONE_COMBO,
                    perZoneSecretEntryMinRate = PER_ZONE_SECRET_ENTRY_MIN_RATE,
                ),
            )
        val artifactPaths =
            OrganicHiddenProbeArtifactWriter.writeSummaryArtifacts(
                OrganicHiddenProbeArtifactWriteRequest(
                    outputDir = outputDir,
                    header = header,
                    summary = summary,
                    kernelCacheMetadata =
                        buildJsonObject {
                            put("contractVersion", ORGANIC_HIDDEN_KERNEL_CACHE_VERSION)
                            put("inputFingerprint", inputFingerprint)
                            put("cacheStatus", if (reusedShardCount == shardSpecs.size) "HIT" else "MISS")
                            put("reusedShardCount", reusedShardCount)
                            put("shardCount", shardSpecs.size)
                            putJsonArray("shardEventPaths") {
                                shardEventPaths.forEach { shardEventPath ->
                                    add(JsonPrimitive(VerificationCacheSupport.relativeToRepo(shardEventPath, repoRoot)))
                                }
                            }
                        },
                    shardEventPaths = shardEventPaths,
                    probeBotId = PROBE_BOT_ID,
                    probeTurnBudget = TURN_BUDGET,
                    probeMaxFloor = MAX_FLOOR,
                    json = json,
                ),
            )
        return OrganicHiddenProbeRun(
            totalCases = results.size,
            runtimeFailureCount = summary.runtimeFailureCount,
            summaryPath = artifactPaths.summaryPath,
            eventsPath = artifactPaths.eventsPath,
            markdownPath = artifactPaths.markdownPath,
        )
    }

    private fun executeCase(
        caseSpec: OrganicHiddenProbeCaseSpec,
        tempSaveRoot: Path,
    ): OrganicHiddenProbeCaseResult {
        val bot = OrganicHiddenProbeBotPolicy(delegate = SmokeBot())
        val commandTail = ArrayDeque<String>()
        return try {
            val session =
                GameModule.newFoundationSession(
                    config =
                        FoundationGameConfig(
                            seed = caseSpec.seed,
                            zoneId = caseSpec.zoneId,
                            floor = caseSpec.floorIndex,
                            playerProfessionId = caseSpec.professionId,
                            playerRaceId = caseSpec.raceId,
                        ),
                    saveManager = SaveManager(tempSaveRoot.resolve("organic-${caseSpec.zoneId}-${caseSpec.professionId}-${caseSpec.raceId}-${caseSpec.seed}")),
                    locale = GameLocale.EN_US,
                )
            val stallDetector = ProbeStallDetector(maxRepeats = 12)
            var turnCount = 0
            var observation = RunObservationCapture.capture(session, turnCount)
            var firstHiddenDiscoveryTurn: Int? = null
            var firstSecretZoneEntryTurn: Int? = null
            var issuedSearchAttemptCount = 0
            var acceptedSearchActionCount = 0
            var searchPromptVisibleCount = if (observation.searchPromptAvailable) 1 else 0
            var latestProbeState = OrganicProbeState(revealedBindingIds = emptySet(), secretZoneIds = emptySet())
            val slagCueMetrics = slagCueMetricsFor(caseSpec = caseSpec, generatedFloor = session.automationGeneratedFloor())

            while (turnCount < TURN_BUDGET && !observation.runOutcome.isTerminal && session.currentFloor() <= MAX_FLOOR) {
                val command = bot.decide(observation) ?: PlayerCommand.Wait
                if (command == PlayerCommand.Search) {
                    issuedSearchAttemptCount += 1
                }
                val accepted = session.perform(command)
                if (!accepted) {
                    if (command == PlayerCommand.Search) {
                        bot.recordRejectedSearch(observation)
                        continue
                    }
                    return OrganicHiddenProbeCaseResult(
                        zoneId = caseSpec.zoneId,
                        floorIndex = caseSpec.floorIndex,
                        professionId = caseSpec.professionId,
                        raceId = caseSpec.raceId,
                        seed = caseSpec.seed,
                        turnCount = turnCount,
                        searchAttemptCount = issuedSearchAttemptCount,
                        searchActionUseCount = acceptedSearchActionCount,
                        searchPromptVisibleCount = searchPromptVisibleCount,
                        searchRevealCount = latestProbeState.revealedBindingIds.size,
                        slagCueEligibleRoomCount = slagCueMetrics.eligibleRoomCount,
                        slagCueCandidateCount = slagCueMetrics.cueCandidateCount,
                        hiddenEventIds = currentConsumedHiddenEventIds(session).toList().sorted(),
                        secretZoneIds = latestProbeState.secretZoneIds.toList().sorted(),
                        firstHiddenDiscoveryTurn = firstHiddenDiscoveryTurn,
                        firstSecretZoneEntryTurn = firstSecretZoneEntryTurn,
                        lastCommands = commandTail.toList(),
                        finalZoneId = session.config.zoneId,
                        finalFloor = session.currentFloor(),
                        runtimeFailure = "Command rejected: ${command.commandName()}",
                    )
                }
                if (command == PlayerCommand.Search) {
                    acceptedSearchActionCount += 1
                    bot.recordAcceptedSearch(observation)
                }
                if (command.consumesTurn()) {
                    turnCount += 1
                }
                commandTail.addLast(command.commandName())
                while (commandTail.size > 12) {
                    commandTail.removeFirst()
                }
                observation = RunObservationCapture.capture(session, turnCount)
                if (observation.searchPromptAvailable) {
                    searchPromptVisibleCount += 1
                }
                latestProbeState = currentProbeState(session)
                val leadDiscovered = latestProbeState.revealedBindingIds.isNotEmpty() || latestProbeState.secretZoneIds.isNotEmpty()
                if (firstHiddenDiscoveryTurn == null && leadDiscovered) {
                    firstHiddenDiscoveryTurn = turnCount
                }
                if (firstSecretZoneEntryTurn == null && latestProbeState.secretZoneIds.isNotEmpty()) {
                    firstSecretZoneEntryTurn = turnCount
                }
                if (latestProbeState.secretZoneIds.isNotEmpty()) {
                    break
                }
                if (stallDetector.observe(observation.signature()) != null) {
                    break
                }
            }

            OrganicHiddenProbeCaseResult(
                zoneId = caseSpec.zoneId,
                floorIndex = caseSpec.floorIndex,
                professionId = caseSpec.professionId,
                raceId = caseSpec.raceId,
                seed = caseSpec.seed,
                turnCount = turnCount,
                searchAttemptCount = issuedSearchAttemptCount,
                searchActionUseCount = acceptedSearchActionCount,
                searchPromptVisibleCount = searchPromptVisibleCount,
                searchRevealCount = latestProbeState.revealedBindingIds.size,
                slagCueEligibleRoomCount = slagCueMetrics.eligibleRoomCount,
                slagCueCandidateCount = slagCueMetrics.cueCandidateCount,
                hiddenEventIds = currentConsumedHiddenEventIds(session).toList().sorted(),
                secretZoneIds = latestProbeState.secretZoneIds.toList().sorted(),
                firstHiddenDiscoveryTurn = firstHiddenDiscoveryTurn,
                firstSecretZoneEntryTurn = firstSecretZoneEntryTurn,
                lastCommands = commandTail.toList(),
                finalZoneId = session.config.zoneId,
                finalFloor = session.currentFloor(),
            )
        } catch (exception: Exception) {
            OrganicHiddenProbeCaseResult(
                zoneId = caseSpec.zoneId,
                floorIndex = caseSpec.floorIndex,
                professionId = caseSpec.professionId,
                raceId = caseSpec.raceId,
                seed = caseSpec.seed,
                turnCount = 0,
                searchAttemptCount = 0,
                searchActionUseCount = 0,
                searchPromptVisibleCount = 0,
                searchRevealCount = 0,
                slagCueEligibleRoomCount = 0,
                slagCueCandidateCount = 0,
                hiddenEventIds = emptyList(),
                secretZoneIds = emptyList(),
                firstHiddenDiscoveryTurn = null,
                firstSecretZoneEntryTurn = null,
                lastCommands = commandTail.toList(),
                finalZoneId = caseSpec.zoneId,
                finalFloor = caseSpec.floorIndex,
                runtimeFailure = exception.message ?: exception::class.simpleName.orEmpty(),
            )
        }
    }

    private fun seedFor(
        zoneOrdinal: Int,
        comboOrdinal: Int,
        seedOrdinal: Int,
    ): Long = SEED_BASE + zoneOrdinal * ZONE_SEED_BLOCK + comboOrdinal * COMBO_SEED_BLOCK + seedOrdinal

    private fun playableProfessionIds(): List<String> =
        releasedPlayerCreationState.professionOptions
            .filter { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }
            .map { option -> option.id }

    private fun playableRaceIds(): List<String> =
        releasedPlayerCreationState.raceOptions
            .filter { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }
            .map { option -> option.id }

    private fun buildShardSpecs(cases: List<OrganicHiddenProbeCaseSpec>): List<OrganicHiddenProbeShardSpec> =
        cases.groupBy { case -> Triple(case.zoneId, case.professionId, case.raceId) }
            .toSortedMap(compareBy<Triple<String, String, String>>({ it.first }, { it.second }, { it.third }))
            .map { (shardKey, shardCases) ->
                OrganicHiddenProbeShardSpec(
                    zoneId = shardKey.first,
                    floorIndex = FLOOR_INDEX,
                    professionId = shardKey.second,
                    raceId = shardKey.third,
                    cases = shardCases.sortedBy(OrganicHiddenProbeCaseSpec::seed),
                )
            }

    private fun organicHiddenFingerprintInputs(repoRoot: Path): List<Path> =
        listOf(
            repoRoot.resolve("core/src/main/kotlin/com/ktome/core"),
            repoRoot.resolve("tools/src/main/kotlin/com/ktome/tools/hidden"),
            repoRoot.resolve("game/src/main/kotlin/com/ktome/game"),
            repoRoot.resolve("game/src/main/resources/data/events"),
            repoRoot.resolve("game/src/main/resources/data/loot"),
            repoRoot.resolve("game/src/main/resources/data/professions"),
            repoRoot.resolve("game/src/main/resources/data/races"),
            repoRoot.resolve("game/src/main/resources/data/secret-zones"),
            repoRoot.resolve("game/src/main/resources/data/mapgen"),
        )

    private fun writeShardResults(
        shardSummaryPath: Path,
        shardEventsPath: Path,
        header: HarnessReportHeader,
        shardSpec: OrganicHiddenProbeShardSpec,
        results: List<OrganicHiddenProbeCaseResult>,
    ) {
        Files.createDirectories(shardSummaryPath.parent)
        Files.writeString(
            shardSummaryPath,
            json.encodeToString(
                JsonElement.serializer(),
                buildJsonObject {
                    put("header", header.toJson())
                    put("shardId", shardSpec.shardId)
                    put("zoneId", shardSpec.zoneId)
                    put("floorIndex", shardSpec.floorIndex)
                    put("professionId", shardSpec.professionId)
                    put("raceId", shardSpec.raceId)
                    put("caseCount", results.size)
                },
            ),
        )
        Files.writeString(
            shardEventsPath,
            results.joinToString(separator = "\n") { result -> Json.encodeToString(JsonElement.serializer(), result.toJson(header)) } + "\n",
        )
    }

    private fun readShardResults(shardEventsPath: Path): List<OrganicHiddenProbeCaseResult> =
        Files.readAllLines(shardEventsPath)
            .filter(String::isNotBlank)
            .map { line -> json.parseToJsonElement(line).jsonObject.toOrganicHiddenProbeCaseResult() }
}

private data class OrganicProbeState(
    val revealedBindingIds: Set<String>,
    val secretZoneIds: Set<String>,
)

private fun currentProbeState(session: com.ktome.game.FoundationGameSession): OrganicProbeState =
    OrganicProbeState(
        revealedBindingIds =
            session.automationSearchState()
                .asSequence()
                .filter { entry -> entry.result == SearchActionResult.REVEALED }
                .map { entry -> entry.bindingId.value }
                .toCollection(linkedSetOf()),
        secretZoneIds = session.automationVisitedSecretZoneIds().mapTo(linkedSetOf()) { secretZoneId -> secretZoneId.id },
    )

private fun currentConsumedHiddenEventIds(session: com.ktome.game.FoundationGameSession): Set<String> =
    session.automationConsumedHiddenEventIds().toCollection(linkedSetOf())

private data class OrganicSlagCueMetrics(
    val eligibleRoomCount: Int,
    val cueCandidateCount: Int,
)

private fun slagCueMetricsFor(
    caseSpec: OrganicHiddenProbeCaseSpec,
    generatedFloor: GeneratedFloor,
): OrganicSlagCueMetrics {
    if (caseSpec.zoneId != "deep_iron_pit") {
        return OrganicSlagCueMetrics(eligibleRoomCount = 0, cueCandidateCount = 0)
    }
    val eligibleRooms = generatedFloor.rooms.filter { room -> room.pathClass != PathClass.SECRET }
    val cueCandidateNodeIds =
        generatedFloor.vaultPlacements
            .asSequence()
            .filter { placement -> placement.vaultId.hasSlagCueKeyword() || placement.roomDefId.hasSlagCueKeyword() }
            .map { placement -> placement.nodeId }
            .toSet()
    val cueCandidateCount =
        eligibleRooms.count { room ->
            room.nodeId in cueCandidateNodeIds ||
                room.roomDefId.hasSlagCueKeyword() ||
                room.patternId?.hasSlagCueKeyword() == true ||
                room.biomeFamilyId?.hasSlagCueKeyword() == true ||
                room.tags.any(String::hasSlagCueKeyword)
        }
    return OrganicSlagCueMetrics(
        eligibleRoomCount = eligibleRooms.size,
        cueCandidateCount = cueCandidateCount,
    )
}

private fun String.hasSlagCueKeyword(): Boolean {
    val normalized = lowercase()
    return normalized.contains("slag") ||
        normalized.contains("ore") ||
        normalized.contains("mine") ||
        normalized.contains("forge")
}

private class ProbeStallDetector(
    private val maxRepeats: Int,
) {
    private var lastSignature: String? = null
    private var repeatCount: Int = 0

    fun observe(signature: String): String? {
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
}

private fun RunObservation.signature(): String =
    buildString {
        append(zoneId)
        append('|')
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
        append(visibleHostilePositions.size)
        append('|')
        append(visibleBossPositions.size)
        append('|')
        append(searchableInteractableSignature())
    }

private fun RunObservation.searchableInteractableSignature(): String =
    visibleInteractables
        .sortedWith(compareBy({ it.position.y }, { it.position.x }, { it.id }))
        .joinToString(separator = ";") { interactable -> "${interactable.id}@${interactable.position.x},${interactable.position.y}" }
